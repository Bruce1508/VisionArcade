package org.example.visionarcade.camera;

import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_videoio.VideoCapture;

public final class OpenCvCameraSource implements CameraSource {

    private final int deviceIndex;
    private VideoCapture capture;

    public OpenCvCameraSource(int deviceIndex) {
        this.deviceIndex = deviceIndex;
    }

    @Override
    public boolean open() {
        capture = new VideoCapture(deviceIndex);
        return capture.isOpened();
    }

    @Override
    public boolean read(Mat destination) {
        return capture != null && capture.read(destination);
    }

    @Override
    public void release() {
        if (capture != null) {
            capture.release();
            capture = null;
        }
    }
}
