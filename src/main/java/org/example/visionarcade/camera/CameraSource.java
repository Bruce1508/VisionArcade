package org.example.visionarcade.camera;

import org.bytedeco.opencv.opencv_core.Mat;

/** Narrow boundary over the actual camera hardware/library so it can be faked in tests. */
public interface CameraSource {
    boolean open();

    boolean read(Mat destination);

    void release();
}
