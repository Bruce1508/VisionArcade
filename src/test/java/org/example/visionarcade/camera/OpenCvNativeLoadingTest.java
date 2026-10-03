package org.example.visionarcade.camera;

import org.bytedeco.opencv.opencv_core.Mat;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OpenCvNativeLoadingTest {

    @Test
    void opencvNativeLibraryLoadsAndCreatesAMat() {
        try (Mat mat = new Mat(2, 2, org.bytedeco.opencv.global.opencv_core.CV_8UC1)) {
            assertEquals(2, mat.rows());
        }
    }
}
