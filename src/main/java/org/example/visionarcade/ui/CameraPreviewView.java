package org.example.visionarcade.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import org.bytedeco.opencv.opencv_core.Mat;

public final class CameraPreviewView extends StackPane {

    private final ImageView imageView = new ImageView();
    private final Label errorLabel = new Label();
    private WritableImage buffer;

    public CameraPreviewView() {
        imageView.setPreserveRatio(true);
        errorLabel.setTextFill(Color.WHITE);
        errorLabel.setWrapText(true);
        errorLabel.setVisible(false);
        setAlignment(Pos.CENTER);
        getChildren().addAll(imageView, errorLabel);
    }

    public void showFrame(Mat bgrFrame) {
        errorLabel.setVisible(false);
        buffer = FrameImageConverter.toImage(bgrFrame, buffer);
        imageView.setImage(buffer);
    }

    public void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}
