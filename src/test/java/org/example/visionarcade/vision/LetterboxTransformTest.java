package org.example.visionarcade.vision;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LetterboxTransformTest {

    @Test
    void padsLandscapeImageOnTopAndBottom() {
        LetterboxTransform transform = LetterboxTransform.of(1920, 1080, 640);

        assertEquals(640.0 / 1920, transform.scale(), 1e-9);
        assertEquals(0, transform.padLeft());
        assertEquals(640, transform.resizedWidth());
        assertEquals((640 - transform.resizedHeight()) / 2, transform.padTop());
    }

    @Test
    void padsPortraitImageOnLeftAndRight() {
        LetterboxTransform transform = LetterboxTransform.of(810, 1080, 640);

        assertEquals(640.0 / 1080, transform.scale(), 1e-9);
        assertEquals(0, transform.padTop());
        assertEquals(640, transform.resizedHeight());
        assertEquals((640 - transform.resizedWidth()) / 2, transform.padLeft());
    }

    @Test
    void mapsLetterboxedCornerBackToOriginalOrigin() {
        LetterboxTransform transform = LetterboxTransform.of(1000, 500, 640);

        BoundingBox box = transform.toOriginalCoordinates(
                transform.padLeft(), transform.padTop(), transform.padLeft() + 10, transform.padTop() + 10);

        assertEquals(0.0, box.x1(), 1e-9);
        assertEquals(0.0, box.y1(), 1e-9);
        assertEquals(10.0 / transform.scale(), box.x2(), 1e-9);
        assertEquals(10.0 / transform.scale(), box.y2(), 1e-9);
    }
}
