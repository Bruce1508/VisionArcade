package org.example.visionarcade.game;

import org.example.visionarcade.vision.Detection;
import org.example.visionarcade.vision.DetectionSnapshot;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Pure "Object Hunt" state machine: picks a target object and advances purely from
 * {@code (now, latest detections)} ticks supplied by the caller — no camera/JavaFX
 * dependency, so it can be driven with fake clocks and fake detections in tests.
 */
public final class GameEngine {

    private static final List<String> TARGET_POOL = List.of(
            "cup", "bottle", "cell phone", "book", "scissors", "clock", "backpack", "mouse", "keyboard", "remote");
    private static final double CONFIDENCE_THRESHOLD = 0.5;
    private static final long ROUND_DURATION_NANOS = TimeUnit.SECONDS.toNanos(20);
    private static final long STABILITY_WINDOW_NANOS = TimeUnit.MILLISECONDS.toNanos(600);
    private static final long RESULT_DISPLAY_NANOS = TimeUnit.MILLISECONDS.toNanos(1500);
    private static final long NOT_TRACKING = -1;

    private enum Phase { HUNTING, ROUND_RESULT }

    private final Random random;

    private Phase phase;
    private String currentTarget;
    private String previousTarget;
    private long phaseEndNanos;
    private long stableStartNanos = NOT_TRACKING;
    private int score;
    private GameState.Result lastResult = GameState.Result.IN_PROGRESS;

    public GameEngine(Random random, long nowNanos) {
        this.random = random;
        startRound(nowNanos);
    }

    /** Advances the state machine to {@code nowNanos} given the latest known detections, and returns the resulting state. */
    public GameState tick(long nowNanos, DetectionSnapshot snapshot) {
        if (phase == Phase.ROUND_RESULT) {
            if (nowNanos >= phaseEndNanos) {
                startRound(nowNanos);
            }
            return state(nowNanos);
        }

        if (isTargetVisible(snapshot)) {
            if (stableStartNanos == NOT_TRACKING) {
                stableStartNanos = nowNanos;
            } else if (nowNanos - stableStartNanos >= STABILITY_WINDOW_NANOS) {
                score++;
                endRound(nowNanos, GameState.Result.FOUND);
                return state(nowNanos);
            }
        } else {
            stableStartNanos = NOT_TRACKING;
        }

        if (nowNanos >= phaseEndNanos) {
            endRound(nowNanos, GameState.Result.TIMEOUT);
        }
        return state(nowNanos);
    }

    private boolean isTargetVisible(DetectionSnapshot snapshot) {
        if (snapshot == null) {
            return false;
        }
        for (Detection detection : snapshot.detections()) {
            if (detection.label().equals(currentTarget) && detection.confidence() >= CONFIDENCE_THRESHOLD) {
                return true;
            }
        }
        return false;
    }

    private void endRound(long nowNanos, GameState.Result result) {
        lastResult = result;
        phase = Phase.ROUND_RESULT;
        phaseEndNanos = nowNanos + RESULT_DISPLAY_NANOS;
        stableStartNanos = NOT_TRACKING;
    }

    private void startRound(long nowNanos) {
        previousTarget = currentTarget;
        currentTarget = pickTarget();
        phase = Phase.HUNTING;
        phaseEndNanos = nowNanos + ROUND_DURATION_NANOS;
        stableStartNanos = NOT_TRACKING;
        lastResult = GameState.Result.IN_PROGRESS;
    }

    private String pickTarget() {
        if (TARGET_POOL.size() == 1) {
            return TARGET_POOL.get(0);
        }
        String candidate;
        do {
            candidate = TARGET_POOL.get(random.nextInt(TARGET_POOL.size()));
        } while (candidate.equals(previousTarget));
        return candidate;
    }

    private GameState state(long nowNanos) {
        double secondsRemaining = phase == Phase.HUNTING
                ? Math.max(0, (phaseEndNanos - nowNanos) / 1_000_000_000.0)
                : 0;
        GameState.Result result = phase == Phase.ROUND_RESULT ? lastResult : GameState.Result.IN_PROGRESS;
        return new GameState(currentTarget, secondsRemaining, score, result);
    }
}
