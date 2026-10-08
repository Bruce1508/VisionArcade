package org.example.visionarcade.ui;

import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import org.bytedeco.opencv.opencv_core.Mat;
import org.example.visionarcade.game.GameState;
import org.example.visionarcade.game.PongState;
import org.example.visionarcade.game.PoseMatchState;
import org.example.visionarcade.vision.BoundingBox;
import org.example.visionarcade.vision.DetectionSnapshot;
import org.example.visionarcade.vision.Keypoint;
import org.example.visionarcade.vision.Pose;
import org.example.visionarcade.vision.PoseSnapshot;

public final class CameraPreviewView extends StackPane {

    private static final double KEYPOINT_VISIBLE_THRESHOLD = 0.3;
    private static final int[][] SKELETON_EDGES = {
            {Pose.LEFT_SHOULDER, Pose.RIGHT_SHOULDER},
            {Pose.LEFT_SHOULDER, Pose.LEFT_ELBOW}, {Pose.LEFT_ELBOW, Pose.LEFT_WRIST},
            {Pose.RIGHT_SHOULDER, Pose.RIGHT_ELBOW}, {Pose.RIGHT_ELBOW, Pose.RIGHT_WRIST},
            {Pose.LEFT_SHOULDER, Pose.LEFT_HIP}, {Pose.RIGHT_SHOULDER, Pose.RIGHT_HIP},
            {Pose.LEFT_HIP, Pose.RIGHT_HIP},
            {Pose.LEFT_HIP, Pose.LEFT_KNEE}, {Pose.LEFT_KNEE, Pose.LEFT_ANKLE},
            {Pose.RIGHT_HIP, Pose.RIGHT_KNEE}, {Pose.RIGHT_KNEE, Pose.RIGHT_ANKLE},
            {Pose.NOSE, Pose.LEFT_SHOULDER}, {Pose.NOSE, Pose.RIGHT_SHOULDER},
    };

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
        drawHudText(gc, "render: %d fps   inference: %.1f ms".formatted(renderFps, inferenceMs), 8, 16);
    }

    /** Draws the pose skeleton (joint lines + dots) and a small FPS/latency debug line; {@code snapshot} or its pose may be null. */
    public void showPose(PoseSnapshot snapshot) {
        GraphicsContext gc = overlay.getGraphicsContext2D();
        gc.clearRect(0, 0, overlay.getWidth(), overlay.getHeight());
        if (frameWidth == 0 || frameHeight == 0) {
            return;
        }

        double scale = Math.min(overlay.getWidth() / frameWidth, overlay.getHeight() / frameHeight);
        double offsetX = (overlay.getWidth() - frameWidth * scale) / 2;
        double offsetY = (overlay.getHeight() - frameHeight * scale) / 2;

        Pose pose = snapshot == null ? null : snapshot.pose();
        if (pose != null) {
            gc.setStroke(Color.LIME);
            gc.setLineWidth(2);
            for (int[] edge : SKELETON_EDGES) {
                drawSkeletonEdge(gc, pose, edge[0], edge[1], offsetX, offsetY, scale);
            }
            gc.setFill(Color.LIME);
            for (Keypoint keypoint : pose.keypoints()) {
                if (keypoint.confidence() >= KEYPOINT_VISIBLE_THRESHOLD) {
                    double x = offsetX + keypoint.x() * scale;
                    double y = offsetY + keypoint.y() * scale;
                    gc.fillOval(x - 3, y - 3, 6, 6);
                }
            }
        }

        double inferenceMs = snapshot == null ? 0 : snapshot.inferenceNanos() / 1_000_000.0;
        drawHudText(gc, "render: %d fps   inference: %.1f ms".formatted(renderFps, inferenceMs), 8, 16);
    }

    private void drawSkeletonEdge(GraphicsContext gc, Pose pose, int a, int b, double offsetX, double offsetY, double scale) {
        Keypoint ka = pose.keypoint(a);
        Keypoint kb = pose.keypoint(b);
        if (ka.confidence() < KEYPOINT_VISIBLE_THRESHOLD || kb.confidence() < KEYPOINT_VISIBLE_THRESHOLD) {
            return;
        }
        gc.strokeLine(offsetX + ka.x() * scale, offsetY + ka.y() * scale, offsetX + kb.x() * scale, offsetY + kb.y() * scale);
    }

    /** Draws text on a translucent dark backing box so it stays readable over both the light letterbox bars and a bright video frame. */
    private void drawHudText(GraphicsContext gc, String text, double x, double y) {
        Text measure = new Text(text);
        measure.setFont(gc.getFont());
        double width = measure.getLayoutBounds().getWidth();
        double height = measure.getLayoutBounds().getHeight();

        gc.setFill(Color.rgb(0, 0, 0, 0.55));
        gc.fillRect(x - 4, y - height + 3, width + 8, height);
        gc.setFill(Color.YELLOW);
        gc.fillText(text, x, y);
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

    /** Draws the Object Hunt HUD (target, timer, score) and a large transient result message; drawn after {@link #showDetections}. */
    public void showGame(GameState state) {
        GraphicsContext gc = overlay.getGraphicsContext2D();
        drawHudText(gc, "find: %s   score: %d   %.0fs".formatted(state.targetLabel(), state.score(), state.secondsRemaining()),
                8, 34);

        if (state.result() != GameState.Result.IN_PROGRESS) {
            String message = state.result() == GameState.Result.FOUND ? "FOUND IT!" : "TIME'S UP!";
            gc.setFont(Font.font(28));
            gc.setFill(state.result() == GameState.Result.FOUND ? Color.LIME : Color.ORANGE);
            gc.fillText(message, overlay.getWidth() / 2 - 70, overlay.getHeight() / 2);
            gc.setFont(Font.getDefault());
        }
    }

    /** Draws the Vision Pong field (ball, paddle, score) over the full overlay canvas, independent of the video's letterbox area. */
    public void showPong(PongState state) {
        GraphicsContext gc = overlay.getGraphicsContext2D();
        double width = overlay.getWidth();
        double height = overlay.getHeight();

        gc.setStroke(Color.WHITE);
        gc.setLineWidth(1);
        gc.strokeRect(0, 0, width, height);

        double paddleHeight = height * 0.2;
        double paddleY = state.paddlePosition() * height - paddleHeight / 2;
        gc.setFill(Color.CYAN);
        gc.fillRect(width - 10, paddleY, 6, paddleHeight);

        gc.setFill(Color.LIME);
        gc.fillOval(state.ballX() * width - 6, state.ballY() * height - 6, 12, 12);

        drawHudText(gc, "score: %d".formatted(state.score()), 8, 34);

        if (state.result() == PongState.Result.MISSED) {
            gc.setFont(Font.font(28));
            gc.setFill(Color.ORANGE);
            gc.fillText("MISSED!", width / 2 - 70, height / 2);
            gc.setFont(Font.getDefault());
        }
    }

    /** Draws the Pose Match HUD (target pose, live match %, timer, score) and a large transient result message; drawn after {@link #showPose}. */
    public void showPoseMatch(PoseMatchState state) {
        GraphicsContext gc = overlay.getGraphicsContext2D();
        drawHudText(gc, "pose: %s   match: %.0f%%   score: %d   %.0fs".formatted(
                state.targetPoseName(), state.matchFraction() * 100, state.score(), state.secondsRemaining()), 8, 34);

        if (state.result() != PoseMatchState.Result.IN_PROGRESS) {
            String message = state.result() == PoseMatchState.Result.MATCHED ? "MATCHED!" : "TIME'S UP!";
            gc.setFont(Font.font(28));
            gc.setFill(state.result() == PoseMatchState.Result.MATCHED ? Color.LIME : Color.ORANGE);
            gc.fillText(message, overlay.getWidth() / 2 - 70, overlay.getHeight() / 2);
            gc.setFont(Font.getDefault());
        }
    }

    /** Clears the overlay canvas; used when returning to the mode-select menu so no stale game HUD lingers. */
    public void clearOverlay() {
        GraphicsContext gc = overlay.getGraphicsContext2D();
        gc.clearRect(0, 0, overlay.getWidth(), overlay.getHeight());
    }

    public void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}
