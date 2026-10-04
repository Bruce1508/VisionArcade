package org.example.visionarcade.vision;

import org.bytedeco.opencv.opencv_core.Mat;
import org.example.visionarcade.camera.FrameSlot;
import org.example.visionarcade.camera.FrameSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertTrue;

class InferenceWorkerTest {

    @Test
    void runsDetectorOnEachAvailableFrameAndReportsResults() throws InterruptedException {
        FrameSlot frameSlot = new FrameSlot();
        CountDownLatch gotTwoDetections = new CountDownLatch(2);
        AtomicInteger detectCalls = new AtomicInteger();

        ObjectDetector fakeDetector = new ObjectDetector() {
            @Override
            public DetectionSnapshot detect(FrameSnapshot frame) {
                detectCalls.incrementAndGet();
                return new DetectionSnapshot(frame.frameId(), frame.captureTimeNanos(), List.of(), 123);
            }

            @Override
            public void close() {
            }
        };

        InferenceWorker worker = new InferenceWorker(fakeDetector, frameSlot, snapshot -> gotTwoDetections.countDown());
        worker.start();

        // FrameSlot only ever keeps the latest unread frame (ADR 0003): publishing both
        // immediately could legitimately collapse into a single detection before the worker
        // thread wakes up. Space them out like a real camera would.
        frameSlot.publish(new FrameSnapshot(1, System.nanoTime(), new Mat()));
        Thread.sleep(50);
        frameSlot.publish(new FrameSnapshot(2, System.nanoTime(), new Mat()));

        assertTrue(gotTwoDetections.await(2, TimeUnit.SECONDS), "expected two detection callbacks");
        worker.stop();

        assertTrue(detectCalls.get() >= 2, "detector should have been invoked at least twice");
    }

    @Test
    void reportsErrorAndKeepsRunningWhenDetectorThrows() throws InterruptedException {
        FrameSlot frameSlot = new FrameSlot();
        CountDownLatch gotError = new CountDownLatch(1);
        CountDownLatch gotDetectionAfterError = new CountDownLatch(1);
        AtomicInteger detectCalls = new AtomicInteger();

        ObjectDetector flakyDetector = new ObjectDetector() {
            @Override
            public DetectionSnapshot detect(FrameSnapshot frame) {
                if (detectCalls.incrementAndGet() == 1) {
                    throw new IllegalStateException("boom");
                }
                return new DetectionSnapshot(frame.frameId(), frame.captureTimeNanos(), List.of(), 123);
            }

            @Override
            public void close() {
            }
        };

        InferenceWorker worker = new InferenceWorker(flakyDetector, frameSlot,
                snapshot -> gotDetectionAfterError.countDown(),
                message -> gotError.countDown());
        worker.start();

        frameSlot.publish(new FrameSnapshot(1, System.nanoTime(), new Mat()));
        assertTrue(gotError.await(2, TimeUnit.SECONDS), "expected an error callback for the failing frame");

        Thread.sleep(50);
        frameSlot.publish(new FrameSnapshot(2, System.nanoTime(), new Mat()));
        assertTrue(gotDetectionAfterError.await(2, TimeUnit.SECONDS),
                "worker thread must survive a detector exception and keep processing frames");

        worker.stop();
    }
}
