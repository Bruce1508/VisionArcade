package org.example.visionarcade.vision;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PoseDecoderTest {

    @Test
    void decodesTheBestAnchorsBoxAndKeypoints() {
        float[][] raw = new float[56][1];
        raw[0][0] = 100; // cx
        raw[1][0] = 100; // cy
        raw[2][0] = 40;  // w
        raw[3][0] = 60;  // h
        raw[4][0] = 0.9f; // person confidence
        int noseBase = 5 + 3 * Pose.NOSE;
        raw[noseBase][0] = 100;
        raw[noseBase + 1][0] = 90;
        raw[noseBase + 2][0] = 0.8f;
        int shoulderBase = 5 + 3 * Pose.LEFT_SHOULDER;
        raw[shoulderBase][0] = 90;
        raw[shoulderBase + 1][0] = 130;
        raw[shoulderBase + 2][0] = 0.7f;

        LetterboxTransform identity = LetterboxTransform.of(640, 640, 640);
        PoseDecoder decoder = new PoseDecoder(0.5);

        Pose pose = decoder.decode(raw, identity);

        assertEquals(0.9, pose.confidence(), 1e-6);
        assertEquals(new BoundingBox(80, 70, 120, 130), pose.box());
        assertEquals(100, pose.keypoint(Pose.NOSE).x(), 1e-6);
        assertEquals(90, pose.keypoint(Pose.NOSE).y(), 1e-6);
        assertEquals(0.8, pose.keypoint(Pose.NOSE).confidence(), 1e-6);
        assertEquals(90, pose.keypoint(Pose.LEFT_SHOULDER).x(), 1e-6);
        assertEquals(130, pose.keypoint(Pose.LEFT_SHOULDER).y(), 1e-6);
    }

    @Test
    void returnsNullWhenNothingMeetsThreshold() {
        float[][] raw = new float[56][1];
        raw[4][0] = 0.3f;

        LetterboxTransform identity = LetterboxTransform.of(640, 640, 640);
        PoseDecoder decoder = new PoseDecoder(0.5);

        assertNull(decoder.decode(raw, identity));
    }
}
