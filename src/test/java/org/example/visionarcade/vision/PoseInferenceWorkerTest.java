package org.example.visionarcade.vision;

import org.bytedeco.opencv.opencv_core.Mat;
import org.example.visionarcade.camera.FrameSlot;
import org.example.visionarcade.camera.FrameSnapshot;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PoseInferenceWorkerTest {

    @Test
    void runsEstimatorOnEachAvailableFrameAndReportsResults() throws InterruptedException {
        FrameSlot frameSlot = new FrameSlot();
        CountDownLatch gotTwoPoses = new CountDownLatch(2);
        AtomicInteger detectCalls = new AtomicInteger();

        PoseEstimator fakeEstimator = new PoseEstimator() {
            @Override
            public PoseSnapshot detect(FrameSnapshot frame) {
                detectCalls.incrementAndGet();
                return new PoseSnapshot(frame.frameId(), frame.captureTimeNanos(), null, 123);
            }

            @Override
            public void close() {
            }
        };

        PoseInferenceWorker worker = new PoseInferenceWorker(fakeEstimator, frameSlot, snapshot -> gotTwoPoses.countDown());
        worker.start();

        frameSlot.publish(new FrameSnapshot(1, System.nanoTime(), new Mat()));
        Thread.sleep(50);
        frameSlot.publish(new FrameSnapshot(2, System.nanoTime(), new Mat()));

        assertTrue(gotTwoPoses.await(2, TimeUnit.SECONDS), "expected two pose callbacks");
        worker.stop();

        assertTrue(detectCalls.get() >= 2, "estimator should have been invoked at least twice");
    }

    @Test
    void reportsErrorAndKeepsRunningWhenEstimatorThrows() throws InterruptedException {
        FrameSlot frameSlot = new FrameSlot();
        CountDownLatch gotError = new CountDownLatch(1);
        CountDownLatch gotPoseAfterError = new CountDownLatch(1);
        AtomicInteger detectCalls = new AtomicInteger();

        PoseEstimator flakyEstimator = new PoseEstimator() {
            @Override
            public PoseSnapshot detect(FrameSnapshot frame) {
                if (detectCalls.incrementAndGet() == 1) {
                    throw new IllegalStateException("boom");
                }
                return new PoseSnapshot(frame.frameId(), frame.captureTimeNanos(), null, 123);
            }

            @Override
            public void close() {
            }
        };

        PoseInferenceWorker worker = new PoseInferenceWorker(flakyEstimator, frameSlot,
                snapshot -> gotPoseAfterError.countDown(),
                message -> gotError.countDown());
        worker.start();

        frameSlot.publish(new FrameSnapshot(1, System.nanoTime(), new Mat()));
        assertTrue(gotError.await(2, TimeUnit.SECONDS), "expected an error callback for the failing frame");

        Thread.sleep(50);
        frameSlot.publish(new FrameSnapshot(2, System.nanoTime(), new Mat()));
        assertTrue(gotPoseAfterError.await(2, TimeUnit.SECONDS),
                "worker thread must survive an estimator exception and keep processing frames");

        worker.stop();
    }
}
