# Current Tasks

Current milestone: **Milestone 3 — Real-time visualization**

Milestone 0 (toolchain/JavaFX/OpenCV/camera validation), Milestone 1 (live webcam preview), and
Milestone 2 (ONNX detection spike) are complete — history preserved in git log.

This file should describe the work that is actually next, not the entire product backlog.

## TODO

- [x] Inference worker. (`vision.InferenceWorker`: dedicated daemon thread, reads the latest frame from its own `FrameSlot`, runs `ObjectDetector.detect()`, reports the result via a callback — never blocks the JavaFX Application Thread.)
- [x] Immutable detection snapshots. (Already in place from Milestone 2 — `DetectionSnapshot` is handed to `VisionArcadeApp` via the worker's callback and read by the render loop through an `AtomicReference`.)
- [x] Box/label rendering. (`CameraPreviewView` gained a transparent `Canvas` overlay sized to the frame's native pixel dimensions; `showDetections()` draws each box + `"label NN%"` text.)
- [x] Coordinate transforms. (Boxes are already in original-frame pixel coordinates from Milestone 2's `LetterboxTransform`; the overlay canvas is sized to match the displayed image's native pixel dimensions 1:1, so no further scaling is needed — see the ARCHITECTURE.md note on why this is sufficient for now.)
- [x] Basic FPS/latency debug overlay. (`CameraPreviewView` tracks its own render FPS and draws it alongside the latest `DetectionSnapshot.inferenceNanos()` as on-canvas text.)

## Definition of done

Milestone 3 is done only when:

- boxes align correctly with objects — **needs manual confirmation: run `./gradlew run` and check the overlay against the live feed.** (Automated: confirmed no startup/runtime exceptions when launching the real app with the real camera and model.)
- UI remains responsive — **needs the same manual run to confirm no stutter;** structurally guaranteed by `InferenceWorker` running on its own thread and the render loop only ever reading the latest available frame/detection, never blocking on either.
- stale frames are dropped — **verified: `FrameSlot` (reused, `FrameSlotTest`) never queues, and `CameraCaptureWorkerTest` confirms both the display and detection slots receive frames from the same capture loop.**
- latency does not grow over time — **structurally guaranteed: no queue anywhere in the new code (`InferenceWorker` takes-latest, `AtomicReference` holds-latest), so staleness is bounded by one frame + one inference cycle, not cumulative.**

## Do not implement yet

- game engine
- Object Hunt
- tracking
- persistence
- Spring Boot
- Docker
- responsive image scaling (`ImageView` fitWidth/fitHeight) — still a known pre-existing gap from Milestone 1, out of scope unless it actually causes a visible problem

## Notes for Codex

Before editing:
1. inspect the existing project;
2. preserve the Milestone 0/1/2 toolchain/JavaFX/OpenCV/camera/vision wiring;
3. avoid replacing working configuration unnecessarily.

After editing:
1. run the build/tests;
2. manually verify the live preview **and the detection overlay alignment** on the primary development machine;
3. do not start Milestone 4 automatically.
