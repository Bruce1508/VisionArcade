package org.example.visionarcade.vision;

/** One detected object: its class label, model confidence, and box in frame pixel coordinates. */
public record Detection(String label, double confidence, BoundingBox box) {
}
