package org.example.visionarcade.vision;

import org.example.visionarcade.camera.FrameSnapshot;

/** Narrow boundary over the pose inference engine/model, mirroring {@link ObjectDetector}. */
public interface PoseEstimator extends AutoCloseable {
    PoseSnapshot detect(FrameSnapshot frame);

    @Override
    void close();
}
