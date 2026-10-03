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
import org.example.visionarcade.ui.CameraPreviewView;

public class VisionArcadeApp extends Application {

    static final String TITLE = "VisionArcade";

    private final FrameSlot frameSlot = new FrameSlot();
    private CameraCaptureWorker cameraWorker;
    private AnimationTimer renderLoop;

    @Override
    public void start(Stage stage) {
        CameraPreviewView previewView = new CameraPreviewView();

        cameraWorker = new CameraCaptureWorker(
                new OpenCvCameraSource(0),
                frameSlot,
                message -> Platform.runLater(() -> previewView.showError(message)));
        cameraWorker.start();

        renderLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                FrameSnapshot snapshot = frameSlot.take();
                if (snapshot == null) {
                    return;
                }
                try {
                    previewView.showFrame(snapshot.mat());
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
        if (cameraWorker != null) {
            cameraWorker.stop();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
