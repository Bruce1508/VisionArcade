package org.example.visionarcade.vision;

/**
 * Axis-aligned box in the source frame's pixel coordinate space (not normalized),
 * top-left ({@code x1}, {@code y1}) to bottom-right ({@code x2}, {@code y2}).
 */
public record BoundingBox(double x1, double y1, double x2, double y2) {
}
