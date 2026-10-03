package org.example.visionarcade.camera;

import org.bytedeco.opencv.opencv_core.Mat;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CameraCaptureWorkerTest {

    @Test
    void publishesFramesThenReleasesCameraOnStop() throws InterruptedException {
        CountDownLatch gotThreeFrames = new CountDownLatch(3);
        AtomicBoolean released = new AtomicBoolean(false);
        FrameSlot frameSlot = new FrameSlot();

        CameraSource fakeCamera = new CameraSource() {
            @Override
            public boolean open() {
                return true;
            }

            @Override
            public boolean read(Mat destination) {
                gotThreeFrames.countDown();
                return true;
            }

            @Override
            public void release() {
                released.set(true);
            }
        };

        CameraCaptureWorker worker = new CameraCaptureWorker(fakeCamera, frameSlot, message -> { });
        worker.start();

        assertTrue(gotThreeFrames.await(2, TimeUnit.SECONDS), "expected at least 3 frames to be published");
        worker.stop();

        assertTrue(released.get(), "camera must be released after stop()");
    }

    @Test
    void reportsErrorWhenCameraFailsToOpen() throws InterruptedException {
        CountDownLatch errorReported = new CountDownLatch(1);
        CameraSource failingCamera = new CameraSource() {
            @Override
            public boolean open() {
                return false;
            }

            @Override
            public boolean read(Mat destination) {
                throw new AssertionError("should never be called when open() fails");
            }

            @Override
            public void release() {
            }
        };

        CameraCaptureWorker worker = new CameraCaptureWorker(
                failingCamera, new FrameSlot(), message -> errorReported.countDown());
        worker.start();

        assertTrue(errorReported.await(2, TimeUnit.SECONDS), "expected an error callback when open() fails");
        worker.stop();
    }
}
