package org.example.visionarcade.vision;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YoloDetectionDecoderTest {

    private static final String[] CLASSES = {"cat", "dog", "bird"};

    @Test
    void filtersLowConfidenceAndSuppressesOverlappingBoxesPerClass() {
        // rows: cx, cy, w, h, cat, dog, bird — one column per anchor (identity letterbox, so
        // decoded coordinates equal these raw values directly).
        float[][] raw = {
                {100, 102, 400, 200},
                {100, 102, 400, 200},
                {40, 40, 40, 40},
                {40, 40, 40, 40},
                {0.9f, 0.8f, 0.0f, 0.1f},
                {0.0f, 0.0f, 0.5f, 0.1f},
                {0.0f, 0.0f, 0.0f, 0.1f},
        };
        LetterboxTransform identity = LetterboxTransform.of(640, 640, 640);
        YoloDetectionDecoder decoder = new YoloDetectionDecoder(CLASSES, 0.25, 0.45);

        List<Detection> detections = decoder.decode(raw, identity);

        assertEquals(2, detections.size());
        Detection cat = detections.stream().filter(d -> d.label().equals("cat")).findFirst().orElseThrow();
        assertEquals(0.9, cat.confidence(), 1e-6);
        assertEquals(new BoundingBox(80, 80, 120, 120), cat.box());

        Detection dog = detections.stream().filter(d -> d.label().equals("dog")).findFirst().orElseThrow();
        assertEquals(0.5, dog.confidence(), 1e-6);
    }

    @Test
    void returnsEmptyListWhenNothingMeetsThreshold() {
        float[][] raw = {
                {100}, {100}, {40}, {40},
                {0.1f}, {0.1f}, {0.1f},
        };
        LetterboxTransform identity = LetterboxTransform.of(640, 640, 640);
        YoloDetectionDecoder decoder = new YoloDetectionDecoder(CLASSES, 0.25, 0.45);

        assertTrue(decoder.decode(raw, identity).isEmpty());
    }
}
