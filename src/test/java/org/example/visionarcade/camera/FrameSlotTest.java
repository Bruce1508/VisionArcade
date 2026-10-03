package org.example.visionarcade.camera;

import org.bytedeco.opencv.opencv_core.Mat;
import org.junit.jupiter.api.Test;

import static org.bytedeco.opencv.global.opencv_core.CV_8UC3;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FrameSlotTest {

    @Test
    void publishingANewerFrameReleasesTheSupersededOne() {
        FrameSlot frameSlot = new FrameSlot();
        Mat firstMat = new Mat(2, 2, CV_8UC3);
        Mat secondMat = new Mat(2, 2, CV_8UC3);

        frameSlot.publish(new FrameSnapshot(1, 0, firstMat));
        frameSlot.publish(new FrameSnapshot(2, 0, secondMat));

        assertTrue(firstMat.empty(), "superseded frame's native data should be released");

        FrameSnapshot taken = frameSlot.take();
        assertEquals(2, taken.frameId());
        taken.close();
    }

    @Test
    void takeReturnsNullWhenNothingNewIsWaiting() {
        FrameSlot frameSlot = new FrameSlot();
        assertEquals(null, frameSlot.take());
    }
}
