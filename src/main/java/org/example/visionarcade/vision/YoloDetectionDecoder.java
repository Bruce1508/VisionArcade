package org.example.visionarcade.vision;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Decodes a YOLO-style single-head detection output ({@code [4 + numClasses][numAnchors]}:
 * box center/size followed by per-class sigmoid scores, no separate objectness score) into
 * confidence-filtered, non-max-suppressed detections mapped into the original frame.
 */
final class YoloDetectionDecoder {

    private final String[] classNames;
    private final double confidenceThreshold;
    private final double iouThreshold;

    YoloDetectionDecoder(String[] classNames, double confidenceThreshold, double iouThreshold) {
        this.classNames = classNames;
        this.confidenceThreshold = confidenceThreshold;
        this.iouThreshold = iouThreshold;
    }

    List<Detection> decode(float[][] rawOutput, LetterboxTransform transform) {
        int numAnchors = rawOutput[0].length;
        List<Detection> candidates = new ArrayList<>();

        for (int a = 0; a < numAnchors; a++) {
            int bestClass = -1;
            double bestScore = confidenceThreshold;
            for (int c = 0; c < classNames.length; c++) {
                double score = rawOutput[4 + c][a];
                if (score > bestScore) {
                    bestScore = score;
                    bestClass = c;
                }
            }
            if (bestClass < 0) {
                continue;
            }
            double cx = rawOutput[0][a];
            double cy = rawOutput[1][a];
            double w = rawOutput[2][a];
            double h = rawOutput[3][a];
            BoundingBox box = transform.toOriginalCoordinates(cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2);
            candidates.add(new Detection(classNames[bestClass], bestScore, box));
        }

        return nonMaxSuppress(candidates);
    }

    // ponytail: O(n^2) greedy NMS per class; fine for the low hundreds of post-threshold
    // candidates one frame produces, revisit if anchor/class counts grow a lot.
    private List<Detection> nonMaxSuppress(List<Detection> candidates) {
        List<Detection> kept = new ArrayList<>();
        var byClass = candidates.stream().collect(Collectors.groupingBy(Detection::label));
        for (List<Detection> group : byClass.values()) {
            List<Detection> sorted = new ArrayList<>(group);
            sorted.sort((a, b) -> Double.compare(b.confidence(), a.confidence()));
            boolean[] suppressed = new boolean[sorted.size()];
            for (int i = 0; i < sorted.size(); i++) {
                if (suppressed[i]) continue;
                Detection current = sorted.get(i);
                kept.add(current);
                for (int j = i + 1; j < sorted.size(); j++) {
                    if (!suppressed[j] && iou(current.box(), sorted.get(j).box()) > iouThreshold) {
                        suppressed[j] = true;
                    }
                }
            }
        }
        return kept;
    }

    private static double iou(BoundingBox a, BoundingBox b) {
        double interX1 = Math.max(a.x1(), b.x1());
        double interY1 = Math.max(a.y1(), b.y1());
        double interX2 = Math.min(a.x2(), b.x2());
        double interY2 = Math.min(a.y2(), b.y2());
        double interArea = Math.max(0, interX2 - interX1) * Math.max(0, interY2 - interY1);
        double areaA = (a.x2() - a.x1()) * (a.y2() - a.y1());
        double areaB = (b.x2() - b.x1()) * (b.y2() - b.y1());
        double union = areaA + areaB - interArea;
        return union <= 0 ? 0 : interArea / union;
    }
}
