package org.example.visionarcade.app;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class VisionArcadeApp extends Application {

    static final String TITLE = "VisionArcade";

    @Override
    public void start(Stage stage) {
        stage.setTitle(TITLE);
        stage.setScene(new Scene(new StackPane(), 640, 480));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
