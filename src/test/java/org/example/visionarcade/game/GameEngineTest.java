package org.example.visionarcade.game;

import org.example.visionarcade.vision.BoundingBox;
import org.example.visionarcade.vision.Detection;
import org.example.visionarcade.vision.DetectionSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameEngineTest {

    private static final Set<String> HOUSEHOLD_TARGETS = Set.of(
            "cup", "bottle", "cell phone", "book", "scissors", "clock", "backpack", "mouse", "keyboard", "remote");

    private static DetectionSnapshot detecting(String label, double confidence) {
        Detection detection = new Detection(label, confidence, new BoundingBox(0, 0, 10, 10));
        return new DetectionSnapshot(1, 0, List.of(detection), 0);
    }

    @Test
    void startsInProgressWithATargetFromTheHouseholdPool() {
        GameEngine engine = new GameEngine(new Random(1), 0);

        GameState state = engine.tick(0, null);

        assertEquals(GameState.Result.IN_PROGRESS, state.result());
        assertTrue(HOUSEHOLD_TARGETS.contains(state.targetLabel()));
        assertEquals(0, state.score());
    }

    @Test
    void sustainedHighConfidenceDetectionWinsTheRound() {
        GameEngine engine = new GameEngine(new Random(1), 0);
        String target = engine.tick(0, null).targetLabel();

        engine.tick(TimeUnit.MILLISECONDS.toNanos(100), detecting(target, 0.9)); // visibility starts
        GameState state = engine.tick(TimeUnit.MILLISECONDS.toNanos(750), detecting(target, 0.9)); // 650ms later

        assertEquals(GameState.Result.FOUND, state.result());
        assertEquals(1, state.score());
    }

    @Test
    void lowConfidenceDetectionDoesNotCount() {
        GameEngine engine = new GameEngine(new Random(1), 0);
        String target = engine.tick(0, null).targetLabel();

        GameState state = engine.tick(TimeUnit.MILLISECONDS.toNanos(700), detecting(target, 0.2));

        assertEquals(GameState.Result.IN_PROGRESS, state.result());
    }

    @Test
    void mustBeSeenContinuouslyNotJustOnce() {
        GameEngine engine = new GameEngine(new Random(1), 0);
        String target = engine.tick(0, null).targetLabel();

        engine.tick(TimeUnit.MILLISECONDS.toNanos(100), detecting(target, 0.9));
        engine.tick(TimeUnit.MILLISECONDS.toNanos(300), null); // target disappears, stability resets
        GameState state = engine.tick(TimeUnit.MILLISECONDS.toNanos(900), detecting(target, 0.9)); // only 600ms since reappearing

        assertEquals(GameState.Result.IN_PROGRESS, state.result());
    }

    @Test
    void roundTimesOutWithoutFindingTheTarget() {
        GameEngine engine = new GameEngine(new Random(1), 0);

        GameState state = engine.tick(TimeUnit.SECONDS.toNanos(21), null);

        assertEquals(GameState.Result.TIMEOUT, state.result());
        assertEquals(0, state.score());
    }

    @Test
    void newRoundStartsAfterTheResultIsShown() {
        GameEngine engine = new GameEngine(new Random(1), 0);
        engine.tick(TimeUnit.SECONDS.toNanos(21), null); // timeout

        GameState state = engine.tick(TimeUnit.SECONDS.toNanos(23), null); // past result display window

        assertEquals(GameState.Result.IN_PROGRESS, state.result());
        assertTrue(state.secondsRemaining() > 15);
    }

    @Test
    void consecutiveRoundsNeverRepeatTheSameTarget() {
        GameEngine engine = new GameEngine(new Random(7), 0);
        String previous = engine.tick(0, null).targetLabel();

        long now = 0;
        for (int i = 0; i < 25; i++) {
            now += TimeUnit.SECONDS.toNanos(21); // force timeout
            engine.tick(now, null);
            now += TimeUnit.SECONDS.toNanos(2); // past result display
            String next = engine.tick(now, null).targetLabel();
            assertNotEquals(previous, next);
            previous = next;
        }
    }
}
