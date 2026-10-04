package org.example.visionarcade.game;

import org.example.visionarcade.vision.BoundingBox;
import org.example.visionarcade.vision.Detection;
import org.example.visionarcade.vision.DetectionSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PongEngineTest {

    private static final int FRAME_HEIGHT = 480;

    private static DetectionSnapshot personAt(double centerY) {
        Detection detection = new Detection("person", 0.9, new BoundingBox(0, centerY - 5, 10, centerY + 5));
        return new DetectionSnapshot(1, 0, List.of(detection), 0);
    }

    @Test
    void startsInProgressWithTheBallAndPaddleCentered() {
        PongEngine engine = new PongEngine(0);

        PongState state = engine.tick(0, null, FRAME_HEIGHT);

        assertEquals(PongState.Result.IN_PROGRESS, state.result());
        assertEquals(0.5, state.paddlePosition());
        assertEquals(0, state.score());
    }

    @Test
    void paddleFollowsTheControllerTowardTheTopOfTheFrame() {
        PongEngine engine = new PongEngine(0);

        double paddle = 0.5;
        for (int i = 1; i <= 10; i++) {
            paddle = engine.tick(i, personAt(10), FRAME_HEIGHT).paddlePosition();
        }

        assertTrue(paddle < 0.3, "expected paddle to move toward the top, was " + paddle);
    }

    @Test
    void paddleHoldsItsPositionWhenTheControllerDisappears() {
        PongEngine engine = new PongEngine(0);
        for (int i = 1; i <= 10; i++) {
            engine.tick(i, personAt(10), FRAME_HEIGHT);
        }
        double paddleBeforeLoss = engine.tick(11, personAt(10), FRAME_HEIGHT).paddlePosition();

        PongState state = engine.tick(12, null, FRAME_HEIGHT);

        assertEquals(paddleBeforeLoss, state.paddlePosition());
    }

    @Test
    void paddlePositionNeverLeavesTheZeroToOneRange() {
        PongEngine engine = new PongEngine(0);

        PongState state = null;
        for (int i = 1; i <= 20; i++) {
            state = engine.tick(i, personAt(-500), FRAME_HEIGHT); // detection box reported above the frame
        }

        assertTrue(state.paddlePosition() >= 0 && state.paddlePosition() <= 1);
    }

    @Test
    void ballNeverLeavesTheFieldWhilePlaying() {
        PongEngine engine = new PongEngine(0);

        long now = 0;
        for (int i = 0; i < 500; i++) {
            now += TimeUnit.MILLISECONDS.toNanos(16);
            PongState state = engine.tick(now, null, FRAME_HEIGHT);
            assertTrue(state.ballX() >= 0 && state.ballX() <= 1, "ballX out of range: " + state.ballX());
            assertTrue(state.ballY() >= 0 && state.ballY() <= 1, "ballY out of range: " + state.ballY());
        }
    }

    @Test
    void roundEndsInAMissAndRestartsAfterTheResultIsShown() {
        // No detections at all: paddle stays locked at its default center (0.5) for the whole
        // test, so whether the ball is returned is fully determined by the engine's own physics —
        // deterministic and reproducible without hand-predicting the bounce trajectory.
        PongEngine engine = new PongEngine(0);

        long now = 0;
        PongState state = null;
        for (int i = 0; i < 1000; i++) {
            now += TimeUnit.MILLISECONDS.toNanos(16);
            state = engine.tick(now, null, FRAME_HEIGHT);
            if (state.result() == PongState.Result.MISSED) {
                break;
            }
        }

        assertEquals(PongState.Result.MISSED, state.result());

        now += TimeUnit.MILLISECONDS.toNanos(1600); // past the 1.5s result display window
        PongState restarted = engine.tick(now, null, FRAME_HEIGHT);

        assertEquals(PongState.Result.IN_PROGRESS, restarted.result());
    }
}
