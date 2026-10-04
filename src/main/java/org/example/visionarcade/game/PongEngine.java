package org.example.visionarcade.game;

import org.example.visionarcade.vision.Detection;
import org.example.visionarcade.vision.DetectionSnapshot;

import java.util.concurrent.TimeUnit;

/**
 * Pure "Vision Pong" state machine: a detected {@code "person"} controls a paddle's vertical
 * position; a ball bounces around a normalized [0,1]x[0,1] field off the top/bottom/left walls
 * and the paddle on the right wall. Advances purely from {@code (now, frameHeight, detections)}
 * ticks supplied by the caller — no camera/JavaFX dependency — so it is testable with fake
 * clocks and fake detections, same pattern as {@link GameEngine}.
 */
public final class PongEngine {

    private static final String CONTROLLER_LABEL = "person";
    private static final double CONFIDENCE_THRESHOLD = 0.5;
    private static final double PADDLE_HALF_HEIGHT = 0.1;
    private static final double PADDLE_SMOOTHING_ALPHA = 0.25;
    private static final double BALL_SPEED_X = 0.5;
    private static final double BALL_SPEED_Y = 0.3;
    private static final long RESULT_DISPLAY_NANOS = TimeUnit.MILLISECONDS.toNanos(1500);

    private enum Phase { PLAYING, ROUND_RESULT }

    private Phase phase;
    private double paddlePosition = 0.5;
    private double ballX;
    private double ballY;
    private double ballVx;
    private double ballVy;
    private long lastTickNanos;
    private long phaseEndNanos;
    private int score;
    private PongState.Result lastResult = PongState.Result.IN_PROGRESS;

    public PongEngine(long nowNanos) {
        startRound(nowNanos);
    }

    /** Advances the state machine to {@code nowNanos} given the latest known detections and current frame height, and returns the resulting state. */
    public PongState tick(long nowNanos, DetectionSnapshot snapshot, int frameHeight) {
        updatePaddle(snapshot, frameHeight);

        if (phase == Phase.ROUND_RESULT) {
            if (nowNanos >= phaseEndNanos) {
                startRound(nowNanos);
            }
            return state();
        }

        // ponytail: clamp dt instead of a fixed-timestep accumulator — fine for a per-render-frame
        // tick cadence; revisit only if a very long pause (debugger stop, OS sleep) causes a visible jump.
        double deltaSeconds = Math.min((nowNanos - lastTickNanos) / 1_000_000_000.0, 0.1);
        lastTickNanos = nowNanos;
        advanceBall(deltaSeconds, nowNanos);

        return state();
    }

    private void updatePaddle(DetectionSnapshot snapshot, int frameHeight) {
        Double targetPosition = controllerPosition(snapshot, frameHeight);
        if (targetPosition != null) {
            paddlePosition += (targetPosition - paddlePosition) * PADDLE_SMOOTHING_ALPHA;
        }
        // else: controller not visible this tick — hold the last paddle position rather than snapping to center.
    }

    private Double controllerPosition(DetectionSnapshot snapshot, int frameHeight) {
        if (snapshot == null || frameHeight <= 0) {
            return null;
        }
        for (Detection detection : snapshot.detections()) {
            if (detection.label().equals(CONTROLLER_LABEL) && detection.confidence() >= CONFIDENCE_THRESHOLD) {
                double centerY = (detection.box().y1() + detection.box().y2()) / 2.0;
                return clamp(centerY / frameHeight, 0, 1);
            }
        }
        return null;
    }

    private void advanceBall(double deltaSeconds, long nowNanos) {
        ballX += ballVx * deltaSeconds;
        ballY += ballVy * deltaSeconds;

        if (ballY <= 0) {
            ballY = 0;
            ballVy = -ballVy;
        } else if (ballY >= 1) {
            ballY = 1;
            ballVy = -ballVy;
        }

        if (ballX <= 0) {
            ballX = 0;
            ballVx = -ballVx;
        } else if (ballX >= 1) {
            ballX = 1;
            if (Math.abs(ballY - paddlePosition) <= PADDLE_HALF_HEIGHT) {
                ballVx = -ballVx;
                score++;
            } else {
                endRound(nowNanos);
            }
        }
    }

    private void endRound(long nowNanos) {
        lastResult = PongState.Result.MISSED;
        phase = Phase.ROUND_RESULT;
        phaseEndNanos = nowNanos + RESULT_DISPLAY_NANOS;
    }

    private void startRound(long nowNanos) {
        phase = Phase.PLAYING;
        ballX = 0.1;
        ballY = 0.5;
        ballVx = BALL_SPEED_X;
        ballVy = BALL_SPEED_Y;
        lastTickNanos = nowNanos;
        lastResult = PongState.Result.IN_PROGRESS;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private PongState state() {
        PongState.Result result = phase == Phase.ROUND_RESULT ? lastResult : PongState.Result.IN_PROGRESS;
        return new PongState(paddlePosition, ballX, ballY, score, result);
    }
}
