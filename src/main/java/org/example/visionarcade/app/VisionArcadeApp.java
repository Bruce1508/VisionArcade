package org.example.visionarcade.app;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.example.visionarcade.camera.CameraCaptureWorker;
import org.example.visionarcade.camera.FrameSlot;
import org.example.visionarcade.camera.FrameSnapshot;
import org.example.visionarcade.camera.OpenCvCameraSource;
import org.example.visionarcade.game.GameEngine;
import org.example.visionarcade.game.PongEngine;
import org.example.visionarcade.game.PoseMatchEngine;
import org.example.visionarcade.ui.CameraPreviewView;
import org.example.visionarcade.ui.ModeSelectView;
import org.example.visionarcade.vision.DetectionSnapshot;
import org.example.visionarcade.vision.DetectionTracker;
import org.example.visionarcade.vision.InferenceWorker;
import org.example.visionarcade.vision.ObjectDetector;
import org.example.visionarcade.vision.PoseEstimator;
import org.example.visionarcade.vision.PoseInferenceWorker;
import org.example.visionarcade.vision.PoseSnapshot;
import org.example.visionarcade.vision.Yolo26nObjectDetector;
import org.example.visionarcade.vision.Yolo26nPoseEstimator;

import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Launches to {@link ModeSelectView}'s menu rather than straight into a game (Milestone 10).
 * Object Hunt and Vision Pong share one object-detection pipeline (switching between them never
 * reloads the model); Pose Match uses a separate pose pipeline. Only the pipeline the current
 * mode needs is running — selecting a mode lazily starts it and stops the other one. The camera
 * worker itself runs continuously across mode switches.
 */
public class VisionArcadeApp extends Application {

    static final String TITLE = "VisionArcade";

    private enum Pipeline { NONE, OBJECT, POSE }

    private final FrameSlot displaySlot = new FrameSlot();
    private final FrameSlot detectionSlot = new FrameSlot();
    private final AtomicReference<DetectionSnapshot> latestDetection = new AtomicReference<>();
    private final AtomicReference<PoseSnapshot> latestPose = new AtomicReference<>();

    private CameraCaptureWorker cameraWorker;
    private CameraPreviewView previewView;
    private ModeSelectView modeSelectView;
    private AnimationTimer renderLoop;

    private Pipeline activePipeline = Pipeline.NONE;
    private ObjectDetector detector;
    private InferenceWorker inferenceWorker;
    private DetectionTracker tracker;
    private PoseEstimator poseEstimator;
    private PoseInferenceWorker poseWorker;

    private GameMode currentMode;
    private GameEngine gameEngine;
    private PongEngine pongEngine;
    private PoseMatchEngine poseMatchEngine;

    @Override
    public void start(Stage stage) {
        previewView = new CameraPreviewView();
        modeSelectView = new ModeSelectView(this::selectMode);
        StackPane root = new StackPane(previewView, modeSelectView);

        cameraWorker = new CameraCaptureWorker(
                new OpenCvCameraSource(0),
                displaySlot,
                detectionSlot,
                message -> Platform.runLater(() -> previewView.showError(message)));
        cameraWorker.start();

        renderLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                FrameSnapshot snapshot = displaySlot.take();
                if (snapshot == null) {
                    return;
                }
                try {
                    previewView.showFrame(snapshot.mat());
                    renderCurrentMode(now, snapshot);
                } finally {
                    snapshot.close();
                }
            }
        };
        renderLoop.start();

        stage.setTitle(TITLE);
        Scene scene = new Scene(root, 640, 480);
        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                showMenu();
            }
        });
        stage.setScene(scene);
        stage.show();
    }

    private void renderCurrentMode(long now, FrameSnapshot snapshot) {
        if (currentMode == null) {
            return;
        }
        switch (currentMode) {
            case OBJECT_HUNT -> {
                DetectionSnapshot detection = latestDetection.get();
                previewView.showDetections(detection);
                previewView.showGame(gameEngine.tick(now, detection));
            }
            case VISION_PONG -> {
                DetectionSnapshot detection = latestDetection.get();
                previewView.showDetections(detection);
                previewView.showPong(pongEngine.tick(now, detection, snapshot.mat().rows()));
            }
            case POSE_MATCH -> {
                PoseSnapshot pose = latestPose.get();
                previewView.showPose(pose);
                previewView.showPoseMatch(poseMatchEngine.tick(now, pose));
            }
        }
    }

    private void selectMode(GameMode mode) {
        try {
            switch (mode) {
                case OBJECT_HUNT -> {
                    ensureObjectPipeline();
                    gameEngine = new GameEngine(new Random(), System.nanoTime());
                }
                case VISION_PONG -> {
                    ensureObjectPipeline();
                    pongEngine = new PongEngine(System.nanoTime());
                }
                case POSE_MATCH -> {
                    ensurePosePipeline();
                    poseMatchEngine = new PoseMatchEngine(new Random(), System.nanoTime());
                }
            }
        } catch (Exception e) {
            // Same degrade-to-visible-error convention as Milestone 3 (ARCHITECTURE.md §10): a
            // missing/bad model is a user-visible failure, not a silent one. Stay on the menu.
            previewView.showError("%s unavailable: %s".formatted(mode.displayName(), e.getMessage()));
            return;
        }
        currentMode = mode;
        modeSelectView.setVisible(false);
    }

    private void showMenu() {
        currentMode = null;
        previewView.clearOverlay();
        modeSelectView.setVisible(true);
    }

    private void ensureObjectPipeline() throws Exception {
        if (activePipeline == Pipeline.OBJECT) {
            return;
        }
        stopPosePipeline();
        if (detector == null) {
            detector = new Yolo26nObjectDetector();
            tracker = new DetectionTracker();
            inferenceWorker = new InferenceWorker(detector, detectionSlot,
                    snapshot -> latestDetection.set(tracker.update(snapshot.captureTimeNanos(), snapshot)),
                    message -> Platform.runLater(() -> previewView.showError(message)));
            inferenceWorker.start();
        }
        activePipeline = Pipeline.OBJECT;
    }

    private void ensurePosePipeline() throws Exception {
        if (activePipeline == Pipeline.POSE) {
            return;
        }
        stopObjectPipeline();
        if (poseEstimator == null) {
            poseEstimator = new Yolo26nPoseEstimator();
            poseWorker = new PoseInferenceWorker(poseEstimator, detectionSlot,
                    latestPose::set,
                    message -> Platform.runLater(() -> previewView.showError(message)));
            poseWorker.start();
        }
        activePipeline = Pipeline.POSE;
    }

    private void stopObjectPipeline() {
        if (inferenceWorker != null) {
            inferenceWorker.stop();
            inferenceWorker = null;
        }
        if (detector != null) {
            detector.close();
            detector = null;
        }
        tracker = null;
    }

    private void stopPosePipeline() {
        if (poseWorker != null) {
            poseWorker.stop();
            poseWorker = null;
        }
        if (poseEstimator != null) {
            poseEstimator.close();
            poseEstimator = null;
        }
    }

    @Override
    public void stop() {
        if (renderLoop != null) {
            renderLoop.stop();
        }
        stopObjectPipeline();
        stopPosePipeline();
        if (cameraWorker != null) {
            cameraWorker.stop();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
