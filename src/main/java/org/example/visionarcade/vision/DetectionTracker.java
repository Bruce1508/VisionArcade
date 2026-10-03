package org.example.visionarcade.vision;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Smooths detections frame-to-frame and bridges brief misses, so a downstream consumer (e.g. a
 * future game mapping object position to on-screen control) sees stable, continuous positions
 * instead of raw per-frame jitter/dropouts. Not thread-safe — one instance per inference stream,
 * {@link #update} called sequentially from that stream's own thread.
 *
 * <p>Each new detection is matched to the nearest same-label track from the previous update (by
 * box-center distance); matched tracks have their box exponentially smoothed toward the new
 * detection, unmatched detections start a new track, and tracks not seen recently enough are
 * dropped.
 */
public final class DetectionTracker {

    private static final double SMOOTHING_ALPHA = 0.35; // weight given to the newest raw detection
    private static final long LOST_TIMEOUT_NANOS = TimeUnit.MILLISECONDS.toNanos(400);
    private static final double MAX_MATCH_DISTANCE = 150; // pixels; beyond this, treat as a new object

    private static final class Track {
        String label;
        BoundingBox box;
        double confidence;
        long lastSeenNanos;
    }

    private final List<Track> tracks = new ArrayList<>();

    /** Advances tracking to {@code nowNanos} with this cycle's raw detections, returning the smoothed snapshot. */
    public DetectionSnapshot update(long nowNanos, DetectionSnapshot raw) {
        // Snapshot the pre-update track count: new tracks appended below (for an earlier detection
        // in this same cycle) must never be matched against a later detection in the same cycle —
        // matching is only against tracks that survived from the previous update.
        int existingTrackCount = tracks.size();
        boolean[] matched = new boolean[existingTrackCount];

        for (Detection detection : raw.detections()) {
            int bestIndex = -1;
            double bestDistance = MAX_MATCH_DISTANCE;
            for (int i = 0; i < existingTrackCount; i++) {
                Track track = tracks.get(i);
                if (matched[i] || !track.label.equals(detection.label())) {
                    continue;
                }
                double distance = centerDistance(track.box, detection.box());
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestIndex = i;
                }
            }

            Track track;
            if (bestIndex == -1) {
                track = new Track();
                track.label = detection.label();
                track.box = detection.box();
                tracks.add(track);
            } else {
                track = tracks.get(bestIndex);
                track.box = smooth(track.box, detection.box());
                matched[bestIndex] = true;
            }
            track.confidence = detection.confidence();
            track.lastSeenNanos = nowNanos;
        }

        tracks.removeIf(track -> nowNanos - track.lastSeenNanos > LOST_TIMEOUT_NANOS);

        List<Detection> smoothed = new ArrayList<>(tracks.size());
        for (Track track : tracks) {
            smoothed.add(new Detection(track.label, track.confidence, track.box));
        }
        return new DetectionSnapshot(raw.frameId(), raw.captureTimeNanos(), smoothed, raw.inferenceNanos());
    }

    private static BoundingBox smooth(BoundingBox previous, BoundingBox latest) {
        return new BoundingBox(
                lerp(previous.x1(), latest.x1()),
                lerp(previous.y1(), latest.y1()),
                lerp(previous.x2(), latest.x2()),
                lerp(previous.y2(), latest.y2()));
    }

    private static double lerp(double previous, double latest) {
        return previous + (latest - previous) * SMOOTHING_ALPHA;
    }

    private static double centerDistance(BoundingBox a, BoundingBox b) {
        double ax = (a.x1() + a.x2()) / 2;
        double ay = (a.y1() + a.y2()) / 2;
        double bx = (b.x1() + b.x2()) / 2;
        double by = (b.y1() + b.y2()) / 2;
        return Math.hypot(ax - bx, ay - by);
    }
}
