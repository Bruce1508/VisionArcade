package org.example.visionarcade.vision;

import org.example.visionarcade.camera.FrameSnapshot;

/** Narrow boundary over the inference engine/model so the runtime or model can change without touching game logic. */
public interface ObjectDetector extends AutoCloseable {
    DetectionSnapshot detect(FrameSnapshot frame);

    @Override
    void close();
}
