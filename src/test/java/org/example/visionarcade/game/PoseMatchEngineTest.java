package org.example.visionarcade.game;

import org.example.visionarcade.vision.BoundingBox;
import org.example.visionarcade.vision.Keypoint;
import org.example.visionarcade.vision.Pose;
import org.example.visionarcade.vision.PoseSnapshot;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PoseMatchEngineTest {

    private static final Set<String> TARGET_POSES = Set.of("Arms Up", "T-Pose", "One Leg Up");

    private static Pose poseFrom(Map<Integer, Keypoint> overrides) {
        List<Keypoint> keypoints = new ArrayList<>();
        for (int i = 0; i < Pose.KEYPOINT_COUNT; i++) {
            keypoints.add(overrides.getOrDefault(i, new Keypoint(0, 0, 0)));
        }
        return new Pose(0.9, new BoundingBox(0, 0, 200, 400), keypoints);
    }

    private static Map<Integer, Keypoint> torso() {
        Map<Integer, Keypoint> map = new HashMap<>();
        map.put(Pose.LEFT_SHOULDER, new Keypoint(100, 200, 0.9));
        map.put(Pose.RIGHT_SHOULDER, new Keypoint(140, 200, 0.9));
        map.put(Pose.LEFT_HIP, new Keypoint(100, 300, 0.9));
        map.put(Pose.RIGHT_HIP, new Keypoint(140, 300, 0.9));
        return map;
    }

    private static Pose armsUpPose() {
        Map<Integer, Keypoint> map = torso();
        map.put(Pose.LEFT_WRIST, new Keypoint(100, 100, 0.9));
        map.put(Pose.RIGHT_WRIST, new Keypoint(140, 100, 0.9));
        return poseFrom(map);
    }

    private static Pose tPose() {
        Map<Integer, Keypoint> map = torso();
        map.put(Pose.LEFT_WRIST, new Keypoint(30, 200, 0.9));
        map.put(Pose.RIGHT_WRIST, new Keypoint(210, 200, 0.9));
        return poseFrom(map);
    }

    private static Pose oneLegUpPose() {
        Map<Integer, Keypoint> map = torso();
        map.put(Pose.LEFT_ANKLE, new Keypoint(100, 300, 0.9));
        map.put(Pose.RIGHT_ANKLE, new Keypoint(140, 400, 0.9));
        return poseFrom(map);
    }

    private static Pose neutralPose() {
        Map<Integer, Keypoint> map = torso();
        map.put(Pose.LEFT_WRIST, new Keypoint(100, 300, 0.9));
        map.put(Pose.RIGHT_WRIST, new Keypoint(140, 300, 0.9));
        map.put(Pose.LEFT_ANKLE, new Keypoint(100, 400, 0.9));
        map.put(Pose.RIGHT_ANKLE, new Keypoint(140, 400, 0.9));
        return poseFrom(map);
    }

    private static Pose poseMatching(String targetName) {
        return switch (targetName) {
            case "Arms Up" -> armsUpPose();
            case "T-Pose" -> tPose();
            case "One Leg Up" -> oneLegUpPose();
            default -> throw new IllegalStateException("unknown target: " + targetName);
        };
    }

    private static PoseSnapshot snapshotOf(Pose pose) {
        return new PoseSnapshot(1, 0, pose, 0);
    }

    @Test
    void startsInProgressWithATargetFromThePool() {
        PoseMatchEngine engine = new PoseMatchEngine(new Random(1), 0);

        PoseMatchState state = engine.tick(0, null);

        assertEquals(PoseMatchState.Result.IN_PROGRESS, state.result());
        assertTrue(TARGET_POSES.contains(state.targetPoseName()));
        assertEquals(0, state.score());
    }

    @Test
    void sustainedMatchWinsTheRound() {
        PoseMatchEngine engine = new PoseMatchEngine(new Random(1), 0);
        String target = engine.tick(0, null).targetPoseName();
        Pose matching = poseMatching(target);

        engine.tick(TimeUnit.MILLISECONDS.toNanos(100), snapshotOf(matching)); // match starts
        PoseMatchState state = engine.tick(TimeUnit.MILLISECONDS.toNanos(750), snapshotOf(matching)); // 650ms later

        assertEquals(PoseMatchState.Result.MATCHED, state.result());
        assertEquals(1, state.score());
    }

    @Test
    void aNeutralPoseDoesNotCount() {
        PoseMatchEngine engine = new PoseMatchEngine(new Random(1), 0);
        engine.tick(0, null);

        PoseMatchState state = engine.tick(TimeUnit.MILLISECONDS.toNanos(700), snapshotOf(neutralPose()));

        assertEquals(PoseMatchState.Result.IN_PROGRESS, state.result());
    }

    @Test
    void mustBeHeldContinuouslyNotJustOnce() {
        PoseMatchEngine engine = new PoseMatchEngine(new Random(1), 0);
        String target = engine.tick(0, null).targetPoseName();
        Pose matching = poseMatching(target);

        engine.tick(TimeUnit.MILLISECONDS.toNanos(100), snapshotOf(matching));
        engine.tick(TimeUnit.MILLISECONDS.toNanos(300), snapshotOf(neutralPose())); // pose breaks, stability resets
        PoseMatchState state = engine.tick(TimeUnit.MILLISECONDS.toNanos(900), snapshotOf(matching)); // only 600ms since re-matching

        assertEquals(PoseMatchState.Result.IN_PROGRESS, state.result());
    }

    @Test
    void roundTimesOutWithoutAMatch() {
        PoseMatchEngine engine = new PoseMatchEngine(new Random(1), 0);

        PoseMatchState state = engine.tick(TimeUnit.SECONDS.toNanos(21), null);

        assertEquals(PoseMatchState.Result.TIMEOUT, state.result());
        assertEquals(0, state.score());
    }

    @Test
    void newRoundStartsAfterTheResultIsShown() {
        PoseMatchEngine engine = new PoseMatchEngine(new Random(1), 0);
        engine.tick(TimeUnit.SECONDS.toNanos(21), null); // timeout

        PoseMatchState state = engine.tick(TimeUnit.SECONDS.toNanos(23), null); // past result display window

        assertEquals(PoseMatchState.Result.IN_PROGRESS, state.result());
        assertTrue(state.secondsRemaining() > 15);
    }

    @Test
    void consecutiveRoundsNeverRepeatTheSameTarget() {
        PoseMatchEngine engine = new PoseMatchEngine(new Random(7), 0);
        String previous = engine.tick(0, null).targetPoseName();

        long now = 0;
        for (int i = 0; i < 25; i++) {
            now += TimeUnit.SECONDS.toNanos(21); // force timeout
            engine.tick(now, null);
            now += TimeUnit.SECONDS.toNanos(2); // past result display
            String next = engine.tick(now, null).targetPoseName();
            assertNotEquals(previous, next);
            previous = next;
        }
    }
}
