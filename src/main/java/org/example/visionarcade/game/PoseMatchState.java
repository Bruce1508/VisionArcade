package org.example.visionarcade.game;

/** Immutable snapshot of one {@link PoseMatchEngine} tick: current target pose name, live match fraction [0,1], time left, score, and round outcome. */
public record PoseMatchState(String targetPoseName, double matchFraction, double secondsRemaining, int score, Result result) {

    public enum Result { IN_PROGRESS, MATCHED, TIMEOUT }
}
