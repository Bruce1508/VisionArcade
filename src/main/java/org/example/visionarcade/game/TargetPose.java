package org.example.visionarcade.game;

import org.example.visionarcade.vision.Pose;

/**
 * Small fixed pool of target poses for Pose Match, each scored against a live {@link Pose} with
 * simple geometric heuristics (relative joint positions scaled by torso length) rather than exact
 * silhouette matching (ROADMAP.md Milestone 9).
 */
enum TargetPose {

    ARMS_UP("Arms Up") {
        @Override
        double matchScore(Pose pose) {
            if (!torsoVisible(pose) || !visible(pose, Pose.LEFT_WRIST) || !visible(pose, Pose.RIGHT_WRIST)) {
                return 0;
            }
            double torso = torsoLength(pose);
            double leftRaise = raiseScore(pose, Pose.LEFT_SHOULDER, Pose.LEFT_WRIST, torso);
            double rightRaise = raiseScore(pose, Pose.RIGHT_SHOULDER, Pose.RIGHT_WRIST, torso);
            return (leftRaise + rightRaise) / 2;
        }
    },

    T_POSE("T-Pose") {
        @Override
        double matchScore(Pose pose) {
            if (!torsoVisible(pose) || !visible(pose, Pose.LEFT_WRIST) || !visible(pose, Pose.RIGHT_WRIST)) {
                return 0;
            }
            double torso = torsoLength(pose);
            double leftScore = extendScore(pose, Pose.LEFT_SHOULDER, Pose.LEFT_WRIST, torso);
            double rightScore = extendScore(pose, Pose.RIGHT_SHOULDER, Pose.RIGHT_WRIST, torso);
            return (leftScore + rightScore) / 2;
        }
    },

    ONE_LEG_UP("One Leg Up") {
        @Override
        double matchScore(Pose pose) {
            if (!torsoVisible(pose) || !visible(pose, Pose.LEFT_ANKLE) || !visible(pose, Pose.RIGHT_ANKLE)) {
                return 0;
            }
            double torso = torsoLength(pose);
            double leftRaised = clamp01((pose.keypoint(Pose.RIGHT_ANKLE).y() - pose.keypoint(Pose.LEFT_ANKLE).y()) / (0.25 * torso));
            double rightRaised = clamp01((pose.keypoint(Pose.LEFT_ANKLE).y() - pose.keypoint(Pose.RIGHT_ANKLE).y()) / (0.25 * torso));
            return Math.max(leftRaised, rightRaised);
        }
    };

    private static final double KEYPOINT_CONFIDENCE_THRESHOLD = 0.3;

    private final String displayName;

    TargetPose(String displayName) {
        this.displayName = displayName;
    }

    String displayName() {
        return displayName;
    }

    abstract double matchScore(Pose pose);

    private static boolean visible(Pose pose, int index) {
        return pose.keypoint(index).confidence() >= KEYPOINT_CONFIDENCE_THRESHOLD;
    }

    private static boolean torsoVisible(Pose pose) {
        return visible(pose, Pose.LEFT_SHOULDER) && visible(pose, Pose.RIGHT_SHOULDER)
                && visible(pose, Pose.LEFT_HIP) && visible(pose, Pose.RIGHT_HIP);
    }

    private static double torsoLength(Pose pose) {
        double shoulderMidX = (pose.keypoint(Pose.LEFT_SHOULDER).x() + pose.keypoint(Pose.RIGHT_SHOULDER).x()) / 2;
        double shoulderMidY = (pose.keypoint(Pose.LEFT_SHOULDER).y() + pose.keypoint(Pose.RIGHT_SHOULDER).y()) / 2;
        double hipMidX = (pose.keypoint(Pose.LEFT_HIP).x() + pose.keypoint(Pose.RIGHT_HIP).x()) / 2;
        double hipMidY = (pose.keypoint(Pose.LEFT_HIP).y() + pose.keypoint(Pose.RIGHT_HIP).y()) / 2;
        return Math.hypot(hipMidX - shoulderMidX, hipMidY - shoulderMidY);
    }

    private static double raiseScore(Pose pose, int shoulderIndex, int wristIndex, double torso) {
        double raise = pose.keypoint(shoulderIndex).y() - pose.keypoint(wristIndex).y();
        return clamp01(raise / (0.3 * torso));
    }

    private static double extendScore(Pose pose, int shoulderIndex, int wristIndex, double torso) {
        double verticalDiff = Math.abs(pose.keypoint(wristIndex).y() - pose.keypoint(shoulderIndex).y());
        double horizontalDiff = Math.abs(pose.keypoint(wristIndex).x() - pose.keypoint(shoulderIndex).x());
        double verticalAlign = clamp01(1 - verticalDiff / (0.3 * torso));
        double horizontalExtend = clamp01(horizontalDiff / (0.6 * torso));
        return verticalAlign * horizontalExtend;
    }

    private static double clamp01(double value) {
        return Math.max(0, Math.min(1, value));
    }
}
