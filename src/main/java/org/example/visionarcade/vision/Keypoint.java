package org.example.visionarcade.vision;

/** One pose joint: its position in the source frame's pixel coordinates and the model's confidence for it. */
public record Keypoint(double x, double y, double confidence) {
}
