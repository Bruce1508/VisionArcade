package org.example.visionarcade.camera;

import org.bytedeco.opencv.opencv_core.Mat;

import java.util.function.Consumer;

/**
 * Owns the camera on a dedicated background thread; never blocks the JavaFX Application Thread.
 *
 * <p>Publishes each frame to two independent single-slot consumers: {@code displaySlot} gets the
 * frame as captured, {@code detectionSlot} gets a {@link Mat#clone()}. Two slots (each with its
 * own owned Mat) rather than one shared frame, because {@link FrameSlot#take()} hands out
 * exclusive ownership to a single taker and native Mats must never be read by one thread while
 * another may release them (ARCHITECTURE.md §9).
 */
public final class CameraCaptureWorker {

    private final CameraSource camera;
    private final FrameSlot displaySlot;
    private final FrameSlot detectionSlot;
    private final Consumer<String> onError;

    private volatile boolean running;
    private Thread thread;

    public CameraCaptureWorker(CameraSource camera, FrameSlot displaySlot, FrameSlot detectionSlot, Consumer<String> onError) {
        this.camera = camera;
        this.displaySlot = displaySlot;
        this.detectionSlot = detectionSlot;
        this.onError = onError;
    }

    public void start() {
        running = true;
        thread = new Thread(this::runLoop, "camera-capture");
        thread.setDaemon(true);
        thread.start();
    }

    private void runLoop() {
        if (!camera.open()) {
            onError.accept("Could not open the camera. Check permissions and that no other app is using it.");
            return;
        }
        try {
            long frameId = 0;
            while (running) {
                Mat frame = new Mat();
                if (!camera.read(frame)) {
                    frame.release();
                    onError.accept("Lost the camera feed.");
                    break;
                }
                long id = frameId++;
                long capturedAt = System.nanoTime();
                displaySlot.publish(new FrameSnapshot(id, capturedAt, frame));
                detectionSlot.publish(new FrameSnapshot(id, capturedAt, frame.clone()));
            }
        } finally {
            camera.release();
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
        displaySlot.clear();
        detectionSlot.clear();
    }
}
