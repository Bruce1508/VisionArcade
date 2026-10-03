package org.example.visionarcade.camera;

import org.bytedeco.opencv.opencv_core.Mat;

/** Immutable handoff of one captured frame; exactly one owner at a time, released via close(). */
public record FrameSnapshot(long frameId, long captureTimeNanos, Mat mat) implements AutoCloseable {
    @Override
    public void close() {
        if (mat != null && !mat.isNull()) {
            mat.release();
        }
    }
}
