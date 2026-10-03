package org.example.visionarcade.app;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.visionarcade.camera.CameraCaptureWorker;
import org.example.visionarcade.camera.FrameSlot;
import org.example.visionarcade.camera.FrameSnapshot;
import org.example.visionarcade.camera.OpenCvCameraSource;
import org.example.visionarcade.game.GameEngine;
import org.example.visionarcade.ui.CameraPreviewView;
import org.example.visionarcade.vision.DetectionSnapshot;
import org.example.visionarcade.vision.DetectionTracker;
import org.example.visionarcade.vision.InferenceWorker;
import org.example.visionarcade.vision.ObjectDetector;
import org.example.visionarcade.vision.Yolo26nObjectDetector;

import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

public class VisionArcadeApp extends Application {

    static final String TITLE = "VisionArcade";

    private final FrameSlot displaySlot = new FrameSlot();
    private final FrameSlot detectionSlot = new FrameSlot();
    private final AtomicReference<DetectionSnapshot> latestDetection = new AtomicReference<>();

    private CameraCaptureWorker cameraWorker;
    private InferenceWorker inferenceWorker;
    private ObjectDetector detector;
    private GameEngine gameEngine;
    private AnimationTimer renderLoop;

    @Override
    public void start(Stage stage) {
        CameraPreviewView previewView = new CameraPreviewView();

        cameraWorker = new CameraCaptureWorker(
                new OpenCvCameraSource(0),
                displaySlot,
                detectionSlot,
                message -> Platform.runLater(() -> previewView.showError(message)));
        cameraWorker.start();

        try {
            detector = new Yolo26nObjectDetector();
            DetectionTracker tracker = new DetectionTracker();
            inferenceWorker = new InferenceWorker(detector, detectionSlot,
                    snapshot -> latestDetection.set(tracker.update(snapshot.captureTimeNanos(), snapshot)));
            inferenceWorker.start();
        } catch (Exception e) {
            // Milestone 3 scope is wiring detection into the live view; a missing/bad model
            // degrades to a plain camera feed with no boxes rather than crashing the app
            // (ARCHITECTURE.md §10: "model missing" is a user-visible failure, not a silent one).
            Platform.runLater(() -> previewView.showError("Object detection unavailable: " + e.getMessage()));
        }

        gameEngine = new GameEngine(new Random(), System.nanoTime());

        renderLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                FrameSnapshot snapshot = displaySlot.take();
                if (snapshot == null) {
                    return;
                }
                try {
                    DetectionSnapshot detection = latestDetection.get();
                    previewView.showFrame(snapshot.mat());
                    previewView.showDetections(detection);
                    previewView.showGame(gameEngine.tick(now, detection));
                } finally {
                    snapshot.close();
                }
            }
        };
        renderLoop.start();

        stage.setTitle(TITLE);
        stage.setScene(new Scene(previewView, 640, 480));
        stage.show();
    }

    @Override
    public void stop() {
        if (renderLoop != null) {
            renderLoop.stop();
        }
        if (inferenceWorker != null) {
            inferenceWorker.stop();
        }
        if (detector != null) {
            detector.close();
        }
        if (cameraWorker != null) {
            cameraWorker.stop();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
