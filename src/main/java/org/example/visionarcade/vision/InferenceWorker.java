package org.example.visionarcade.vision;

import org.example.visionarcade.camera.FrameSlot;
import org.example.visionarcade.camera.FrameSnapshot;

import java.util.function.Consumer;

/**
 * Owns the {@link ObjectDetector} on a dedicated background thread; never blocks the JavaFX
 * Application Thread. Always reads the latest available frame from its {@link FrameSlot} — if
 * inference is slower than capture, older frames are dropped (ARCHITECTURE.md §3), never queued.
 */
public final class InferenceWorker {

    private final ObjectDetector detector;
    private final FrameSlot frameSlot;
    private final Consumer<DetectionSnapshot> onDetection;
    private final Consumer<String> onError;

    private volatile boolean running;
    private Thread thread;

    public InferenceWorker(ObjectDetector detector, FrameSlot frameSlot, Consumer<DetectionSnapshot> onDetection) {
        this(detector, frameSlot, onDetection, message -> { });
    }

    public InferenceWorker(ObjectDetector detector, FrameSlot frameSlot, Consumer<DetectionSnapshot> onDetection,
                            Consumer<String> onError) {
        this.detector = detector;
        this.frameSlot = frameSlot;
        this.onDetection = onDetection;
        this.onError = onError;
    }

    public void start() {
        running = true;
        thread = new Thread(this::runLoop, "inference-worker");
        thread.setDaemon(true);
        thread.start();
    }

    private void runLoop() {
        while (running) {
            FrameSnapshot frame = frameSlot.take();
            if (frame == null) {
                // ponytail: fixed 1ms poll backoff; a notify-on-publish signal would avoid the
                // wakeup if FrameSlot's polling cost ever shows up in a profile.
                parkBriefly();
                continue;
            }
            try {
                onDetection.accept(detector.detect(frame));
            } catch (RuntimeException e) {
                onError.accept("Detection failed on one frame: " + e.getMessage());
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
