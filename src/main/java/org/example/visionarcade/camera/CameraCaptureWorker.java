package org.example.visionarcade.camera;

import org.bytedeco.opencv.opencv_core.Mat;

import java.util.function.Consumer;

/** Owns the camera on a dedicated background thread; never blocks the JavaFX Application Thread. */
public final class CameraCaptureWorker {

    private final CameraSource camera;
    private final FrameSlot frameSlot;
    private final Consumer<String> onError;

    private volatile boolean running;
    private Thread thread;

    public CameraCaptureWorker(CameraSource camera, FrameSlot frameSlot, Consumer<String> onError) {
        this.camera = camera;
        this.frameSlot = frameSlot;
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
                frameSlot.publish(new FrameSnapshot(frameId++, System.nanoTime(), frame));
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
        frameSlot.clear();
    }
}
