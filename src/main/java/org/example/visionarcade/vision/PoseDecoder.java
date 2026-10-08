package org.example.visionarcade.vision;

import java.util.ArrayList;
import java.util.List;

/**
 * Decodes a YOLO-pose single-person-class output ({@code [56][numAnchors]}: box + person
 * confidence + 17 keypoints each x/y/confidence) into the single most-confident person, mapped
 * into the original frame. No NMS needed: Pose Match tracks only one controller (Milestone 9),
 * same single-controller convention as Vision Pong, so only the best-scoring anchor is kept.
 */
final class PoseDecoder {

    private final double confidenceThreshold;

    PoseDecoder(double confidenceThreshold) {
        this.confidenceThreshold = confidenceThreshold;
    }

    Pose decode(float[][] rawOutput, LetterboxTransform transform) {
        int numAnchors = rawOutput[0].length;
        int bestAnchor = -1;
        double bestConfidence = confidenceThreshold;
        for (int a = 0; a < numAnchors; a++) {
            double confidence = rawOutput[4][a];
            if (confidence > bestConfidence) {
                bestConfidence = confidence;
                bestAnchor = a;
            }
        }
        if (bestAnchor < 0) {
            return null;
        }

        double cx = rawOutput[0][bestAnchor];
        double cy = rawOutput[1][bestAnchor];
        double w = rawOutput[2][bestAnchor];
        double h = rawOutput[3][bestAnchor];
        BoundingBox box = transform.toOriginalCoordinates(cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2);

        List<Keypoint> keypoints = new ArrayList<>(Pose.KEYPOINT_COUNT);
        for (int k = 0; k < Pose.KEYPOINT_COUNT; k++) {
            int base = 5 + 3 * k;
            double x = transform.toOriginalX(rawOutput[base][bestAnchor]);
            double y = transform.toOriginalY(rawOutput[base + 1][bestAnchor]);
            double confidence = rawOutput[base + 2][bestAnchor];
            keypoints.add(new Keypoint(x, y, confidence));
        }

        return new Pose(bestConfidence, box, keypoints);
    }
}
