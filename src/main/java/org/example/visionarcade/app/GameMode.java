package org.example.visionarcade.app;

/** Which of VisionArcade's three games is active; picked from {@code ui.ModeSelectView}. */
public enum GameMode {
    OBJECT_HUNT("Object Hunt"),
    VISION_PONG("Vision Pong"),
    POSE_MATCH("Pose Match");

    private final String displayName;

    GameMode(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
