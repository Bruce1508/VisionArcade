package org.example.visionarcade.ui;

import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import org.bytedeco.opencv.opencv_core.Mat;
import org.example.visionarcade.vision.BoundingBox;
import org.example.visionarcade.vision.DetectionSnapshot;

public final class CameraPreviewView extends StackPane {

    private final ImageView imageView = new ImageView();
    private final Canvas overlay = new Canvas();
    private final Label errorLabel = new Label();
    private WritableImage buffer;

    private int frameWidth;
    private int frameHeight;

    private long fpsWindowStartNanos = System.nanoTime();
    private int framesSinceWindow;
    private int renderFps;

    public CameraPreviewView() {
        imageView.setPreserveRatio(true);
        // Scale the image to fit however big the window actually is (user-resizable), rather
        // than always rendering at the camera's native pixel size. The overlay binds to the same
        // size and showDetections() replicates preserveRatio's own letterbox math so boxes track
        // the scaled image instead of assuming 1:1 native pixels.
        imageView.fitWidthProperty().bind(widthProperty());
        imageView.fitHeightProperty().bind(heightProperty());
        overlay.widthProperty().bind(widthProperty());
        overlay.heightProperty().bind(heightProperty());
        overlay.setMouseTransparent(true);
        errorLabel.setTextFill(Color.WHITE);
        errorLabel.setWrapText(true);
        errorLabel.setVisible(false);
        setAlignment(Pos.CENTER);
        getChildren().addAll(imageView, overlay, errorLabel);
    }

    public void showFrame(Mat bgrFrame) {
        errorLabel.setVisible(false);
        frameWidth = bgrFrame.cols();
        frameHeight = bgrFrame.rows();
        buffer = FrameImageConverter.toImage(bgrFrame, buffer);
        imageView.setImage(buffer);
        trackRenderFps();
    }

    /** Draws detection boxes/labels and a small FPS/latency debug line; {@code snapshot} may be null before the first detection arrives. */
    public void showDetections(DetectionSnapshot snapshot) {
        GraphicsContext gc = overlay.getGraphicsContext2D();
        gc.clearRect(0, 0, overlay.getWidth(), overlay.getHeight());
        if (frameWidth == 0 || frameHeight == 0) {
            return;
        }

        // Same fit-inside-a-box math as imageView's preserveRatio, so a detection box drawn here
        // lands on the same screen pixel as the object it was detected on.
        double scale = Math.min(overlay.getWidth() / frameWidth, overlay.getHeight() / frameHeight);
        double offsetX = (overlay.getWidth() - frameWidth * scale) / 2;
        double offsetY = (overlay.getHeight() - frameHeight * scale) / 2;

        if (snapshot != null) {
            gc.setStroke(Color.LIME);
            gc.setLineWidth(2);
            gc.setFill(Color.LIME);
            for (var detection : snapshot.detections()) {
                BoundingBox box = detection.box();
                double x = offsetX + box.x1() * scale;
                double y = offsetY + box.y1() * scale;
                double width = (box.x2() - box.x1()) * scale;
                double height = (box.y2() - box.y1()) * scale;
                gc.strokeRect(x, y, width, height);
                gc.fillText("%s %.0f%%".formatted(detection.label(), detection.confidence() * 100),
                        x + 2, Math.max(12, y - 4));
            }
        }

        double inferenceMs = snapshot == null ? 0 : snapshot.inferenceNanos() / 1_000_000.0;
        gc.setFill(Color.YELLOW);
        gc.fillText("render: %d fps   inference: %.1f ms".formatted(renderFps, inferenceMs), 8, 16);
    }

    private void trackRenderFps() {
        framesSinceWindow++;
        long now = System.nanoTime();
        if (now - fpsWindowStartNanos >= 1_000_000_000L) {
            renderFps = framesSinceWindow;
            framesSinceWindow = 0;
            fpsWindowStartNanos = now;
        }
    }

    public void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}
