package org.example.visionarcade.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import org.example.visionarcade.app.GameMode;

import java.util.function.Consumer;

/** Start/pause menu letting the player pick which of VisionArcade's three games to play. */
public final class ModeSelectView extends StackPane {

    public ModeSelectView(Consumer<GameMode> onSelect) {
        setStyle("-fx-background-color: rgba(0,0,0,0.75);");

        Label title = new Label("VisionArcade");
        title.setTextFill(Color.WHITE);
        title.setFont(Font.font(32));

        VBox buttons = new VBox(12);
        buttons.setAlignment(Pos.CENTER);
        for (GameMode mode : GameMode.values()) {
            Button button = new Button(mode.displayName());
            button.setPrefWidth(220);
            button.setOnAction(event -> onSelect.accept(mode));
            buttons.getChildren().add(button);
        }

        VBox layout = new VBox(24, title, buttons);
        layout.setAlignment(Pos.CENTER);
        getChildren().add(layout);
        setAlignment(Pos.CENTER);
    }
}
