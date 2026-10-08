package org.example.visionarcade.game;

import org.example.visionarcade.vision.Pose;
import org.example.visionarcade.vision.PoseSnapshot;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Pure "Pose Match" state machine: picks a target pose and advances purely from
 * {@code (now, latest pose)} ticks supplied by the caller — no camera/JavaFX dependency, so it
 * can be driven with fake clocks and fake poses in tests. Same shape as {@link GameEngine}.
 */
public final class PoseMatchEngine {

    private static final double MATCH_THRESHOLD = 0.8;
    private static final long ROUND_DURATION_NANOS = TimeUnit.SECONDS.toNanos(20);
    private static final long STABILITY_WINDOW_NANOS = TimeUnit.MILLISECONDS.toNanos(600);
    private static final long RESULT_DISPLAY_NANOS = TimeUnit.MILLISECONDS.toNanos(1500);
    private static final long NOT_TRACKING = -1;

    private enum Phase { MATCHING, ROUND_RESULT }

    private final Random random;

    private Phase phase;
    private TargetPose currentTarget;
    private TargetPose previousTarget;
    private long phaseEndNanos;
    private long stableStartNanos = NOT_TRACKING;
    private double lastMatchFraction;
    private int score;
    private PoseMatchState.Result lastResult = PoseMatchState.Result.IN_PROGRESS;

    public PoseMatchEngine(Random random, long nowNanos) {
        this.random = random;
        startRound(nowNanos);
    }

    /** Advances the state machine to {@code nowNanos} given the latest known pose, and returns the resulting state. */
    public PoseMatchState tick(long nowNanos, PoseSnapshot snapshot) {
        Pose pose = snapshot == null ? null : snapshot.pose();
        lastMatchFraction = pose == null ? 0 : currentTarget.matchScore(pose);

        if (phase == Phase.ROUND_RESULT) {
            if (nowNanos >= phaseEndNanos) {
                startRound(nowNanos);
            }
            return state(nowNanos);
        }

        if (lastMatchFraction >= MATCH_THRESHOLD) {
            if (stableStartNanos == NOT_TRACKING) {
                stableStartNanos = nowNanos;
            } else if (nowNanos - stableStartNanos >= STABILITY_WINDOW_NANOS) {
                score++;
                endRound(nowNanos, PoseMatchState.Result.MATCHED);
                return state(nowNanos);
            }
        } else {
            stableStartNanos = NOT_TRACKING;
        }

        if (nowNanos >= phaseEndNanos) {
            endRound(nowNanos, PoseMatchState.Result.TIMEOUT);
        }
        return state(nowNanos);
    }

    private void endRound(long nowNanos, PoseMatchState.Result result) {
        lastResult = result;
        phase = Phase.ROUND_RESULT;
        phaseEndNanos = nowNanos + RESULT_DISPLAY_NANOS;
        stableStartNanos = NOT_TRACKING;
    }

    private void startRound(long nowNanos) {
        previousTarget = currentTarget;
        currentTarget = pickTarget();
        phase = Phase.MATCHING;
        phaseEndNanos = nowNanos + ROUND_DURATION_NANOS;
        stableStartNanos = NOT_TRACKING;
        lastResult = PoseMatchState.Result.IN_PROGRESS;
        lastMatchFraction = 0;
    }

    private TargetPose pickTarget() {
        TargetPose[] pool = TargetPose.values();
        if (pool.length == 1) {
            return pool[0];
        }
        TargetPose candidate;
        do {
            candidate = pool[random.nextInt(pool.length)];
        } while (candidate == previousTarget);
        return candidate;
    }

    private PoseMatchState state(long nowNanos) {
        double secondsRemaining = phase == Phase.MATCHING
                ? Math.max(0, (phaseEndNanos - nowNanos) / 1_000_000_000.0)
                : 0;
        PoseMatchState.Result result = phase == Phase.ROUND_RESULT ? lastResult : PoseMatchState.Result.IN_PROGRESS;
        return new PoseMatchState(currentTarget.displayName(), lastMatchFraction, secondsRemaining, score, result);
    }
}
