package org.example.visionarcade.vision;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import org.bytedeco.javacpp.FloatPointer;
import org.bytedeco.opencv.global.opencv_dnn;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_core.Size;
import org.example.visionarcade.camera.FrameSnapshot;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.bytedeco.opencv.global.opencv_core.BORDER_CONSTANT;
import static org.bytedeco.opencv.global.opencv_core.CV_32F;
import static org.bytedeco.opencv.global.opencv_core.copyMakeBorder;

/**
 * {@link PoseEstimator} backed by the pretrained YOLO26n-pose model (COCO 17-keypoint, single
 * "person" class) exported to ONNX and run through ONNX Runtime's CPU execution provider. No
 * custom training — see VisionArcade_docs/DEVELOPMENT.md for the pinned model's provenance.
 *
 * Preprocessing/inference plumbing mirrors {@link Yolo26nObjectDetector} rather than sharing it —
 * same precedent as {@code GameEngine}/{@code PongEngine} staying separate concrete classes.
 */
public final class Yolo26nPoseEstimator implements PoseEstimator {

    public static final Path DEFAULT_MODEL_PATH = Path.of("models", "yolo26n-pose.onnx");

    private static final int INPUT_SIZE = 640;
    private static final double DEFAULT_CONFIDENCE_THRESHOLD = 0.5;

    private final OrtEnvironment environment;
    private final OrtSession session;
    private final String inputName;
    private final PoseDecoder decoder;

    public Yolo26nPoseEstimator() throws OrtException, IOException {
        this(DEFAULT_MODEL_PATH, DEFAULT_CONFIDENCE_THRESHOLD);
    }

    public Yolo26nPoseEstimator(Path modelPath, double confidenceThreshold) throws OrtException, IOException {
        if (!Files.isRegularFile(modelPath)) {
            throw new IOException("ONNX model not found at " + modelPath.toAbsolutePath()
                    + " — export it first (see VisionArcade_docs/DEVELOPMENT.md).");
        }
        this.environment = OrtEnvironment.getEnvironment();
        this.session = environment.createSession(modelPath.toString(), new OrtSession.SessionOptions());
        this.inputName = session.getInputNames().iterator().next();
        this.decoder = new PoseDecoder(confidenceThreshold);
    }

    @Override
    public PoseSnapshot detect(FrameSnapshot frame) {
        Mat original = frame.mat();
        LetterboxTransform transform = LetterboxTransform.of(original.cols(), original.rows(), INPUT_SIZE);
        Mat letterboxed = letterbox(original, transform);
        try {
            long start = System.nanoTime();
            float[][] rawOutput = runInference(letterboxed);
            long inferenceNanos = System.nanoTime() - start;

            Pose pose = decoder.decode(rawOutput, transform);
            return new PoseSnapshot(frame.frameId(), frame.captureTimeNanos(), pose, inferenceNanos);
        } catch (OrtException e) {
            throw new IllegalStateException("ONNX inference failed", e);
        } finally {
            letterboxed.release();
        }
    }

    private Mat letterbox(Mat original, LetterboxTransform transform) {
        Mat resized = new Mat();
        opencv_imgproc.resize(original, resized, new Size(transform.resizedWidth(), transform.resizedHeight()));
        Mat padded = new Mat();
        copyMakeBorder(resized, padded, transform.padTop(), transform.padBottom(),
                transform.padLeft(), transform.padRight(), BORDER_CONSTANT, new Scalar(114, 114, 114, 0));
        resized.release();
        return padded;
    }

    private float[][] runInference(Mat letterboxed) throws OrtException {
        Mat blob = opencv_dnn.blobFromImage(letterboxed, 1.0 / 255.0, new Size(INPUT_SIZE, INPUT_SIZE),
                new Scalar(0, 0, 0, 0), true, false, CV_32F);
        try {
            FloatBuffer buffer = new FloatPointer(blob.ptr()).capacity(blob.total()).asBuffer();
            try (OnnxTensor inputTensor = OnnxTensor.createTensor(environment, buffer, new long[]{1, 3, INPUT_SIZE, INPUT_SIZE});
                 OrtSession.Result result = session.run(Map.of(inputName, inputTensor))) {
                float[][][] output = (float[][][]) result.get(0).getValue();
                return output[0];
            }
        } finally {
            blob.release();
        }
    }

    @Override
    public void close() {
        try {
            session.close();
        } catch (OrtException e) {
            throw new IllegalStateException("Failed to close ONNX session", e);
        }
    }
}
