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
    // Sized to match the displayed image's native pixel dimensions exactly (imageView has no
    // fitWidth/fitHeight scaling — see ARCHITECTURE.md note), so a box drawn at a frame's pixel
    // coordinates lines up with that same pixel in the image without a separate scale transform.
    private final Canvas overlay = new Canvas();
    private final Label errorLabel = new Label();
    private WritableImage buffer;

    private long fpsWindowStartNanos = System.nanoTime();
    private int framesSinceWindow;
    private int renderFps;

    public CameraPreviewView() {
        imageView.setPreserveRatio(true);
        overlay.setMouseTransparent(true);
        errorLabel.setTextFill(Color.WHITE);
        errorLabel.setWrapText(true);
        errorLabel.setVisible(false);
        setAlignment(Pos.CENTER);
        getChildren().addAll(imageView, overlay, errorLabel);
    }

    public void showFrame(Mat bgrFrame) {
        errorLabel.setVisible(false);
        buffer = FrameImageConverter.toImage(bgrFrame, buffer);
        imageView.setImage(buffer);
        if (overlay.getWidth() != bgrFrame.cols() || overlay.getHeight() != bgrFrame.rows()) {
            overlay.setWidth(bgrFrame.cols());
            overlay.setHeight(bgrFrame.rows());
        }
        trackRenderFps();
    }

    /** Draws detection boxes/labels and a small FPS/latency debug line; {@code snapshot} may be null before the first detection arrives. */
    public void showDetections(DetectionSnapshot snapshot) {
        GraphicsContext gc = overlay.getGraphicsContext2D();
        gc.clearRect(0, 0, overlay.getWidth(), overlay.getHeight());

        if (snapshot != null) {
            gc.setStroke(Color.LIME);
            gc.setLineWidth(2);
            gc.setFill(Color.LIME);
            for (var detection : snapshot.detections()) {
                BoundingBox box = detection.box();
                double width = box.x2() - box.x1();
                double height = box.y2() - box.y1();
                gc.strokeRect(box.x1(), box.y1(), width, height);
                gc.fillText("%s %.0f%%".formatted(detection.label(), detection.confidence() * 100),
                        box.x1() + 2, Math.max(12, box.y1() - 4));
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
