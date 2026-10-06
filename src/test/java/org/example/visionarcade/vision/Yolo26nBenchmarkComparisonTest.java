package org.example.visionarcade.vision;

import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.opencv_core.Mat;
import org.example.visionarcade.camera.FrameSnapshot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Repeatable speed comparison between the stock 80-class model and the Milestone 8 fine-tuned
 * 11-class model, on the same fixed image, printed as mean/p50/p95/p99 latency (BENCHMARKS.md
 * Benchmark B). Not part of the default suite's pass/fail signal (TESTING.md: "do not create
 * arbitrary performance thresholds") — run manually and copy the printed numbers into the docs.
 * Skipped — not failed — when either model file is missing.
 */
@EnabledIf("bothModelsExist")
class Yolo26nBenchmarkComparisonTest {

    private static final int WARMUP_RUNS = 10;
    private static final int MEASURED_RUNS = 50;

    static boolean bothModelsExist() {
        return Files.isRegularFile(Yolo26nObjectDetector.DEFAULT_MODEL_PATH)
                && Files.isRegularFile(Yolo26nObjectDetector.FINE_TUNE_MODEL_PATH);
    }

    @Test
    void comparesStockAndFineTunedLatency() throws Exception {
        Mat image = opencv_imgcodecs.imread(
                Paths.get(getClass().getResource("/vision/bus.jpg").toURI()).toString());
        assertTrue(!image.empty(), "test image failed to load");

        System.out.println("=== Stock (80-class) ===");
        report(benchmark(new Yolo26nObjectDetector(), image));

        System.out.println("=== Fine-tuned (11-class, Milestone 8) ===");
        report(benchmark(
                new Yolo26nObjectDetector(Yolo26nObjectDetector.FINE_TUNE_MODEL_PATH,
                        Yolo26nObjectDetector.FINE_TUNE_CLASSES, 0.4, 0.45),
                image));
    }

    private long[] benchmark(Yolo26nObjectDetector detector, Mat image) {
        try {
            for (int i = 0; i < WARMUP_RUNS; i++) {
                detectOnce(detector, image);
            }
            long[] samplesNanos = new long[MEASURED_RUNS];
            for (int i = 0; i < MEASURED_RUNS; i++) {
                samplesNanos[i] = detectOnce(detector, image).inferenceNanos();
            }
            return samplesNanos;
        } finally {
            detector.close();
        }
    }

    private DetectionSnapshot detectOnce(Yolo26nObjectDetector detector, Mat image) {
        try (FrameSnapshot frame = new FrameSnapshot(1, System.nanoTime(), image.clone())) {
            return detector.detect(frame);
        }
    }

    private void report(long[] samplesNanos) {
        long[] sorted = samplesNanos.clone();
        Arrays.sort(sorted);
        double meanMs = Arrays.stream(sorted).average().orElse(0) / 1_000_000.0;
        System.out.printf("  samples=%d mean=%.2fms p50=%.2fms p95=%.2fms p99=%.2fms%n",
                sorted.length, meanMs, percentileMs(sorted, 50), percentileMs(sorted, 95), percentileMs(sorted, 99));
    }

    private double percentileMs(long[] sortedNanos, int percentile) {
        int index = Math.min(sortedNanos.length - 1, (int) Math.ceil(percentile / 100.0 * sortedNanos.length) - 1);
        return sortedNanos[Math.max(0, index)] / 1_000_000.0;
    }
}
