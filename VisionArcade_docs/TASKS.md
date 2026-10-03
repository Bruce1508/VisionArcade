# Current Tasks

Current milestone: **Milestone 5 — Tracking and smoothing**

Milestone 0 (toolchain/JavaFX/OpenCV/camera validation), Milestone 1 (live webcam preview),
Milestone 2 (ONNX detection spike), Milestone 3 (real-time visualization), and Milestone 4
(Object Hunt MVP) are complete — history preserved in git log.

This file should describe the work that is actually next, not the entire product backlog.

## TODO

- [x] Box center smoothing. (`vision.DetectionTracker`: exponentially smooths each tracked box
  toward its newest raw detection — `SMOOTHING_ALPHA = 0.35` — instead of snapping frame to frame.)
- [x] Simple nearest-match tracking. (Each raw detection is matched to the nearest same-label track
  from the previous update, within `MAX_MATCH_DISTANCE` pixels; unmatched detections start a new track.)
- [x] Lost-object timeout. (A track not matched for `LOST_TIMEOUT_NANOS` (400ms) is dropped; a brief
  miss just keeps the last known smoothed position instead of vanishing for one frame.)
- [ ] Jitter metrics — deliberately deferred: no concrete consumer needs a number yet (Milestone 6 isn't
  built), and `TESTING.md` says not to invent performance thresholds before a baseline exists. Revisit
  once Vision Pong (M6) needs to know whether smoothing is actually enough.

## Definition of done

Milestone 5 is done only when:

- `DetectionTracker` is unit tested without a camera or model (`DetectionTrackerTest`: pass-through on
  first sighting, smoothing toward a new position, surviving a brief miss, dropping after a sustained
  miss, and not conflating two far-apart same-label objects).
- the live app still runs with no startup/runtime exceptions with the tracker wired between
  `InferenceWorker` and the render loop — **needs manual confirmation: run `./gradlew run`.**
- positions read as noticeably steadier than the raw per-frame boxes during a live run — **needs the
  same manual run to judge subjectively; there is no numeric jitter threshold yet (see "Jitter metrics"
  above).**

## Do not implement yet

- a heavy/general multi-object tracking framework (Kalman filters, Hungarian algorithm, ID re-assignment
  across occlusion) — nearest-match + EMA is the roadmap's explicit "only if simple methods fail" floor
- Milestone 6 (Vision Pong) itself — only build what M5's own definition of done needs
- persistence, additional game modes, Spring Boot, Docker (unchanged from Milestone 4)

## Notes for Codex

Before editing:
1. inspect the existing project;
2. preserve the Milestone 0–4 toolchain/JavaFX/OpenCV/camera/vision/game wiring;
3. avoid replacing working configuration unnecessarily.

After editing:
1. run the build/tests;
2. manually verify the live preview, detection overlay, Object Hunt HUD, **and that tracked boxes look
   steadier than before** on the primary development machine;
3. do not start a new milestone automatically.
