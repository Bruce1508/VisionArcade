package org.example.visionarcade.vision;

import org.example.visionarcade.camera.FrameSlot;
import org.example.visionarcade.camera.FrameSnapshot;

import java.util.function.Consumer;

/**
 * Owns the {@link PoseEstimator} on a dedicated background thread; never blocks the JavaFX
 * Application Thread. Mirrors {@link InferenceWorker}'s shape exactly (same precedent as
 * {@code GameEngine}/{@code PongEngine} staying separate concrete classes rather than a shared
 * generic worker).
 */
public final class PoseInferenceWorker {

    private final PoseEstimator estimator;
    private final FrameSlot frameSlot;
    private final Consumer<PoseSnapshot> onPose;
    private final Consumer<String> onError;

    private volatile boolean running;
    private Thread thread;

    public PoseInferenceWorker(PoseEstimator estimator, FrameSlot frameSlot, Consumer<PoseSnapshot> onPose) {
        this(estimator, frameSlot, onPose, message -> { });
    }

    public PoseInferenceWorker(PoseEstimator estimator, FrameSlot frameSlot, Consumer<PoseSnapshot> onPose,
                                Consumer<String> onError) {
        this.estimator = estimator;
        this.frameSlot = frameSlot;
        this.onPose = onPose;
        this.onError = onError;
    }

    public void start() {
        running = true;
        thread = new Thread(this::runLoop, "pose-inference-worker");
        thread.setDaemon(true);
        thread.start();
    }

    private void runLoop() {
        while (running) {
            FrameSnapshot frame = frameSlot.take();
            if (frame == null) {
                parkBriefly();
                continue;
            }
            try {
                onPose.accept(estimator.detect(frame));
            } catch (RuntimeException e) {
                onError.accept("Pose estimation failed on one frame: " + e.getMessage());
            } finally {
                frame.close();
            }
        }
    }

    private static void parkBriefly() {
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void stop() {
        running = false;
        if (thread != null) {
            try {
                thread.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        frameSlot.clear();
    }
}
