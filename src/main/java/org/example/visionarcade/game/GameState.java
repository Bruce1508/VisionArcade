package org.example.visionarcade.game;

/** Immutable snapshot of one {@link GameEngine} tick: current target, time left, score, and round outcome. */
public record GameState(String targetLabel, double secondsRemaining, int score, Result result) {

    public enum Result { IN_PROGRESS, FOUND, TIMEOUT }
}
