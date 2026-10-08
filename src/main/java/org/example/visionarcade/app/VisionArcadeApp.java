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
import org.example.visionarcade.game.PoseMatchEngine;
import org.example.visionarcade.ui.CameraPreviewView;
import org.example.visionarcade.vision.PoseEstimator;
import org.example.visionarcade.vision.PoseInferenceWorker;
import org.example.visionarcade.vision.PoseSnapshot;
import org.example.visionarcade.vision.Yolo26nPoseEstimator;

import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

public class VisionArcadeApp extends Application {

    static final String TITLE = "VisionArcade";

    private final FrameSlot displaySlot = new FrameSlot();
    private final FrameSlot detectionSlot = new FrameSlot();
    private final AtomicReference<PoseSnapshot> latestPose = new AtomicReference<>();

    private CameraCaptureWorker cameraWorker;
    private PoseInferenceWorker poseWorker;
    private PoseEstimator poseEstimator;
    private PoseMatchEngine poseMatchEngine;
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
            poseEstimator = new Yolo26nPoseEstimator();
            poseWorker = new PoseInferenceWorker(poseEstimator, detectionSlot,
                    latestPose::set,
                    message -> Platform.runLater(() -> previewView.showError(message)));
            poseWorker.start();
        } catch (Exception e) {
            // Same degrade-to-plain-camera-feed convention as Milestone 3 (ARCHITECTURE.md §10):
            // a missing/bad pose model is a user-visible failure, not a silent one.
            Platform.runLater(() -> previewView.showError("Pose estimation unavailable: " + e.getMessage()));
        }

        poseMatchEngine = new PoseMatchEngine(new Random(), System.nanoTime());

        renderLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                FrameSnapshot snapshot = displaySlot.take();
                if (snapshot == null) {
                    return;
                }
                try {
                    PoseSnapshot pose = latestPose.get();
                    previewView.showFrame(snapshot.mat());
                    previewView.showPose(pose);
                    previewView.showPoseMatch(poseMatchEngine.tick(now, pose));
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
        if (poseWorker != null) {
            poseWorker.stop();
        }
        if (poseEstimator != null) {
            poseEstimator.close();
        }
        if (cameraWorker != null) {
            cameraWorker.stop();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
