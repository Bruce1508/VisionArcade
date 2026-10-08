package org.example.visionarcade.vision;

import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.opencv_core.Mat;
import org.example.visionarcade.camera.FrameSnapshot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration smoke test against the real exported pretrained pose model. Skipped — not failed —
 * when the model hasn't been exported yet; see VisionArcade_docs/DEVELOPMENT.md.
 */
@EnabledIf("modelFileExists")
class Yolo26nPoseEstimatorIntegrationTest {

    private Yolo26nPoseEstimator estimator;

    static boolean modelFileExists() {
        return Files.isRegularFile(Yolo26nPoseEstimator.DEFAULT_MODEL_PATH);
    }

    @AfterEach
    void closeEstimator() {
        if (estimator != null) {
            estimator.close();
        }
    }

    @Test
    void detectsAPersonsPoseInTestImage() throws Exception {
        estimator = new Yolo26nPoseEstimator();

        Path imagePath = Paths.get(getClass().getResource("/vision/bus.jpg").toURI());
        Mat image = opencv_imgcodecs.imread(imagePath.toString());
        assertTrue(!image.empty(), "test image failed to load: " + imagePath);

        try (FrameSnapshot frame = new FrameSnapshot(1, System.nanoTime(), image)) {
            PoseSnapshot snapshot = estimator.detect(frame);

            assertNotNull(snapshot.pose(), "expected a person's pose to be detected");
            assertEquals(Pose.KEYPOINT_COUNT, snapshot.pose().keypoints().size());
            assertTrue(snapshot.inferenceNanos() > 0, "inference timing should be recorded");
        }
    }
}
