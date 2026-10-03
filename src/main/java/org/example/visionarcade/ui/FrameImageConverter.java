package org.example.visionarcade.ui;

import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.opencv.opencv_core.Mat;

import java.nio.ByteBuffer;

import static org.bytedeco.opencv.global.opencv_imgproc.COLOR_BGR2RGB;
import static org.bytedeco.opencv.global.opencv_imgproc.cvtColor;

/**
 * Converts a BGR OpenCV frame into a JavaFX image. JavaFX has no 3-byte BGR
 * PixelFormat, only RGB, so the channel order is swapped via OpenCV first.
 */
public final class FrameImageConverter {

    private FrameImageConverter() {
    }

    public static WritableImage toImage(Mat bgrFrame, WritableImage reuse) {
        int width = bgrFrame.cols();
        int height = bgrFrame.rows();
        WritableImage image = (reuse != null && (int) reuse.getWidth() == width && (int) reuse.getHeight() == height)
                ? reuse
                : new WritableImage(width, height);

        try (Mat rgbFrame = new Mat()) {
            cvtColor(bgrFrame, rgbFrame, COLOR_BGR2RGB);

            long rowStride = rgbFrame.step();
            BytePointer pointer = new BytePointer(rgbFrame.data()).capacity(rowStride * height);
            ByteBuffer buffer = pointer.asByteBuffer();

            image.getPixelWriter().setPixels(0, 0, width, height,
                    PixelFormat.getByteRgbInstance(), buffer, (int) rowStride);
        }
        return image;
    }
}
