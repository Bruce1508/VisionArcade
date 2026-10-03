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
        FrameSlot displaySlot = new FrameSlot();
        FrameSlot detectionSlot = new FrameSlot();

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

        CameraCaptureWorker worker = new CameraCaptureWorker(fakeCamera, displaySlot, detectionSlot, message -> { });
        worker.start();

        assertTrue(gotThreeFrames.await(2, TimeUnit.SECONDS), "expected at least 3 frames to be published");
        assertTrue(awaitFrame(displaySlot) != null, "display slot never received a frame");
        assertTrue(awaitFrame(detectionSlot) != null, "detection slot never received a cloned frame");

        worker.stop();

        assertTrue(released.get(), "camera must be released after stop()");
    }

    /** Publishing races the test thread's take() right after the CountDownLatch fires; retry briefly. */
    private static FrameSnapshot awaitFrame(FrameSlot slot) throws InterruptedException {
        for (int i = 0; i < 200; i++) {
            FrameSnapshot snapshot = slot.take();
            if (snapshot != null) {
                return snapshot;
            }
            Thread.sleep(10);
        }
        return null;
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
                failingCamera, new FrameSlot(), new FrameSlot(), message -> errorReported.countDown());
        worker.start();

        assertTrue(errorReported.await(2, TimeUnit.SECONDS), "expected an error callback when open() fails");
        worker.stop();
    }
}
