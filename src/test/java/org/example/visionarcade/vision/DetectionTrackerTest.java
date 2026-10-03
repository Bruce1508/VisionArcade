package org.example.visionarcade.vision;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DetectionTrackerTest {

    private static DetectionSnapshot snapshotOf(List<Detection> detections) {
        return new DetectionSnapshot(1, 0, detections, 0);
    }

    private static Detection cupAt(double x1, double y1, double x2, double y2) {
        return new Detection("cup", 0.9, new BoundingBox(x1, y1, x2, y2));
    }

    @Test
    void multipleNewObjectsInTheFirstFrameDoNotCrash() {
        DetectionTracker tracker = new DetectionTracker();

        DetectionSnapshot result = tracker.update(0, snapshotOf(List.of(
                cupAt(0, 0, 10, 10),
                new Detection("chair", 0.8, new BoundingBox(500, 500, 600, 600)))));

        assertEquals(2, result.detections().size());
    }

    @Test
    void firstSightingPassesThroughUnchanged() {
        DetectionTracker tracker = new DetectionTracker();

        DetectionSnapshot result = tracker.update(0, snapshotOf(List.of(cupAt(0, 0, 10, 10))));

        assertEquals(1, result.detections().size());
        assertEquals(new BoundingBox(0, 0, 10, 10), result.detections().get(0).box());
    }

    @Test
    void sameObjectJumpingPositionIsSmoothedTowardTheNewPosition() {
        DetectionTracker tracker = new DetectionTracker();
        tracker.update(0, snapshotOf(List.of(cupAt(0, 0, 10, 10))));

        DetectionSnapshot result = tracker.update(
                TimeUnit.MILLISECONDS.toNanos(33), snapshotOf(List.of(cupAt(100, 0, 110, 10))));

        double smoothedX1 = result.detections().get(0).box().x1();
        assertTrue(smoothedX1 > 0 && smoothedX1 < 100,
                "expected smoothed x1 strictly between old and new position, was " + smoothedX1);
    }

    @Test
    void briefMissKeepsTheLastKnownPosition() {
        DetectionTracker tracker = new DetectionTracker();
        tracker.update(0, snapshotOf(List.of(cupAt(0, 0, 10, 10))));

        DetectionSnapshot result = tracker.update(TimeUnit.MILLISECONDS.toNanos(100), snapshotOf(List.of()));

        assertEquals(1, result.detections().size());
        assertEquals(new BoundingBox(0, 0, 10, 10), result.detections().get(0).box());
    }

    @Test
    void sustainedMissDropsTheTrack() {
        DetectionTracker tracker = new DetectionTracker();
        tracker.update(0, snapshotOf(List.of(cupAt(0, 0, 10, 10))));

        DetectionSnapshot result = tracker.update(TimeUnit.MILLISECONDS.toNanos(500), snapshotOf(List.of()));

        assertTrue(result.detections().isEmpty());
    }

    @Test
    void farAwayDetectionOfTheSameLabelStartsANewTrackInsteadOfJumping() {
        DetectionTracker tracker = new DetectionTracker();
        tracker.update(0, snapshotOf(List.of(cupAt(0, 0, 10, 10))));

        // The original track isn't dropped on one miss (lost-object grace period), so both the
        // original (unmatched, still within its grace period) and the new far-away track show up.
        DetectionSnapshot result = tracker.update(
                TimeUnit.MILLISECONDS.toNanos(33), snapshotOf(List.of(cupAt(1000, 1000, 1010, 1010))));

        List<BoundingBox> boxes = result.detections().stream().map(Detection::box).toList();
        assertEquals(2, boxes.size());
        assertTrue(boxes.contains(new BoundingBox(0, 0, 10, 10)));
        assertTrue(boxes.contains(new BoundingBox(1000, 1000, 1010, 1010)));
    }
}
