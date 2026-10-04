package org.example.visionarcade.game;

/** Immutable snapshot of one {@link PongEngine} tick: paddle/ball position (normalized [0,1] field), score, and round outcome. */
public record PongState(double paddlePosition, double ballX, double ballY, int score, Result result) {

    public enum Result { IN_PROGRESS, MISSED }
}
