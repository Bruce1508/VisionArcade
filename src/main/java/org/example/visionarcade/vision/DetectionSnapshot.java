package org.example.visionarcade.vision;

import java.util.List;

/** Immutable result of running the detector once: the detections found and how long inference took. */
public record DetectionSnapshot(long frameId, long captureTimeNanos, List<Detection> detections, long inferenceNanos) {
    public DetectionSnapshot {
        detections = List.copyOf(detections);
    }
}
