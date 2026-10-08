package org.example.visionarcade.vision;

/**
 * Immutable result of running the pose estimator once: the most-confident person's pose (or
 * {@code null} if no one was detected above threshold) and how long inference took.
 */
public record PoseSnapshot(long frameId, long captureTimeNanos, Pose pose, long inferenceNanos) {
}
