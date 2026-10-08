package org.example.visionarcade.vision;

/**
 * Aspect-ratio-preserving resize + padding ("letterbox") used to fit an arbitrary-sized frame
 * into a fixed square model input. Captures enough state to map a decoded box in
 * {@code targetSize x targetSize} model-input pixel space back to the original frame.
 */
public record LetterboxTransform(double scale, int padLeft, int padTop, int resizedWidth, int resizedHeight, int targetSize) {

    public static LetterboxTransform of(int originalWidth, int originalHeight, int targetSize) {
        double scale = Math.min((double) targetSize / originalWidth, (double) targetSize / originalHeight);
        int resizedWidth = (int) Math.round(originalWidth * scale);
        int resizedHeight = (int) Math.round(originalHeight * scale);
        int padLeft = (targetSize - resizedWidth) / 2;
        int padTop = (targetSize - resizedHeight) / 2;
        return new LetterboxTransform(scale, padLeft, padTop, resizedWidth, resizedHeight, targetSize);
    }

    public int padRight() {
        return targetSize - resizedWidth - padLeft;
    }

    public int padBottom() {
        return targetSize - resizedHeight - padTop;
    }

    /** Maps a box decoded in letterboxed model-input pixel space back to original frame pixels. */
    public BoundingBox toOriginalCoordinates(double x1, double y1, double x2, double y2) {
        return new BoundingBox(toOriginalX(x1), toOriginalY(y1), toOriginalX(x2), toOriginalY(y2));
    }

    /** Maps a single x coordinate decoded in letterboxed model-input pixel space back to original frame pixels. */
    public double toOriginalX(double x) {
        return (x - padLeft) / scale;
    }

    /** Maps a single y coordinate decoded in letterboxed model-input pixel space back to original frame pixels. */
    public double toOriginalY(double y) {
        return (y - padTop) / scale;
    }
}
