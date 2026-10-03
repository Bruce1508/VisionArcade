package org.example.visionarcade.vision;

import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.opencv_core.Mat;
import org.example.visionarcade.camera.FrameSnapshot;
import org.example.visionarcade.camera.OpenCvCameraSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration smoke tests against the real exported model (see TESTING.md §2). Skipped — not
 * failed — when the model hasn't been exported yet; see VisionArcade_docs/DEVELOPMENT.md.
 */
@EnabledIf("modelFileExists")
class Yolo26nObjectDetectorIntegrationTest {

    private Yolo26nObjectDetector detector;

    static boolean modelFileExists() {
        return Files.isRegularFile(Yolo26nObjectDetector.DEFAULT_MODEL_PATH);
    }

    @AfterEach
    void closeDetector() {
        if (detector != null) {
            detector.close();
        }
    }

    @Test
    void detectsKnownObjectsInTestImage() throws Exception {
        detector = new Yolo26nObjectDetector();

        Path imagePath = Paths.get(getClass().getResource("/vision/bus.jpg").toURI());
        Mat image = opencv_imgcodecs.imread(imagePath.toString());
        assertTrue(!image.empty(), "test image failed to load: " + imagePath);

        try (FrameSnapshot frame = new FrameSnapshot(1, System.nanoTime(), image)) {
            DetectionSnapshot snapshot = detector.detect(frame);

            List<String> labels = snapshot.detections().stream().map(Detection::label).toList();
            assertTrue(labels.contains("bus"), "expected a bus detection, got: " + labels);
            assertTrue(labels.contains("person"), "expected a person detection, got: " + labels);
            assertTrue(snapshot.inferenceNanos() > 0, "inference timing should be recorded");
        }
    }

    @Test
    @EnabledIfSystemProperty(named = "visionarcade.hardwareTests", matches = "true")
    void detectsOnARealWebcamFrame() throws Exception {
        detector = new Yolo26nObjectDetector();
        OpenCvCameraSource camera = new OpenCvCameraSource(0);
        try {
            assertTrue(camera.open(), "default camera did not open");
            Mat raw = new Mat();
            assertTrue(camera.read(raw), "camera did not return a frame");

            try (FrameSnapshot frame = new FrameSnapshot(1, System.nanoTime(), raw)) {
                DetectionSnapshot snapshot = detector.detect(frame);
                assertTrue(snapshot.inferenceNanos() > 0, "inference timing should be recorded");
            }
        } finally {
            camera.release();
        }
    }
}
