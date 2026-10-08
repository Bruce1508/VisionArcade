package org.example.visionarcade.vision;

import java.util.List;

/**
 * One detected person's pose: overall box/confidence plus all 17 COCO-order keypoints (some may
 * have low confidence if occluded or out of frame — callers decide their own visibility
 * threshold). Keypoint order: nose, left/right eye, left/right ear, left/right shoulder,
 * left/right elbow, left/right wrist, left/right hip, left/right knee, left/right ankle.
 */
public record Pose(double confidence, BoundingBox box, List<Keypoint> keypoints) {

    public static final int NOSE = 0;
    public static final int LEFT_EYE = 1;
    public static final int RIGHT_EYE = 2;
    public static final int LEFT_EAR = 3;
    public static final int RIGHT_EAR = 4;
    public static final int LEFT_SHOULDER = 5;
    public static final int RIGHT_SHOULDER = 6;
    public static final int LEFT_ELBOW = 7;
    public static final int RIGHT_ELBOW = 8;
    public static final int LEFT_WRIST = 9;
    public static final int RIGHT_WRIST = 10;
    public static final int LEFT_HIP = 11;
    public static final int RIGHT_HIP = 12;
    public static final int LEFT_KNEE = 13;
    public static final int RIGHT_KNEE = 14;
    public static final int LEFT_ANKLE = 15;
    public static final int RIGHT_ANKLE = 16;
    public static final int KEYPOINT_COUNT = 17;

    public Pose {
        keypoints = List.copyOf(keypoints);
    }

    public Keypoint keypoint(int index) {
        return keypoints.get(index);
    }
}
