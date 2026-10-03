package org.example.visionarcade.camera;

import org.bytedeco.opencv.opencv_videoio.VideoCapture;
import org.bytedeco.opencv.opencv_core.Mat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Manual hardware spike for Milestone 0. Disabled by default so the normal
 * suite stays non-flaky on machines/CI without a webcam (see TESTING.md).
 * Run explicitly on the dev machine with:
 *   ./gradlew test -Dvisionarcade.hardwareTests=true --tests CameraCaptureSpikeTest
 * macOS will prompt for camera permission on first run.
 */
@EnabledIfSystemProperty(named = "visionarcade.hardwareTests", matches = "true")
class CameraCaptureSpikeTest {

    @Test
    void opensDefaultCameraAndCapturesOneFrame() {
        VideoCapture camera = new VideoCapture(0);
        try {
            assertTrue(camera.isOpened(), "default camera did not open");

            Mat frame = new Mat();
            boolean grabbed = camera.read(frame);

            assertTrue(grabbed, "camera did not return a frame");
            assertFalse(frame.empty(), "captured frame was empty");
        } finally {
            camera.release();
        }
    }
}
