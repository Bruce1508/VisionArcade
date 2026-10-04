# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project state

VisionArcade is a brand-new Java/Gradle project. Milestone 0 (toolchain, JavaFX window, OpenCV native
loading, camera permission/capture) is done. Milestone 1 (live webcam preview) and Milestone 2 (ONNX
detection spike, `vision.Yolo26nObjectDetector`) are done. Milestone 3 (real-time visualization) is
implemented: `CameraCaptureWorker` now fans each frame out to two single-slot `FrameSlot`s (display +
detection, the detection one a cloned `Mat`, since `FrameSlot.take()` hands exclusive ownership to one
consumer); `vision.InferenceWorker` runs the detector on its own thread against the detection slot;
`VisionArcadeApp` reads the latest `DetectionSnapshot` via an `AtomicReference` each render tick and calls
`CameraPreviewView.showDetections()`, which draws boxes/labels and a render-FPS/inference-latency line on
a `Canvas` overlay sized to the frame's native pixel dimensions. Milestone 4 (Object Hunt MVP) is
implemented: `game.GameEngine` is a pure state machine (`tick(nowNanos, DetectionSnapshot)`, no camera/
JavaFX dependency) that picks a target from a 10-class household-object pool, requires continuous
confidence ≥0.5 detection for ≥600ms to count as "found", runs a 20s round timer, and auto-restarts after
a 1.5s result display. `VisionArcadeApp` ticks one `GameEngine` per render frame using the same `now` the
render loop already has; `CameraPreviewView.showGame()` draws the target/score/timer HUD and a transient
"FOUND IT!"/"TIME'S UP!" message on the existing overlay `Canvas`. Milestone 5 (tracking and smoothing)
is implemented: `vision.DetectionTracker` sits between `InferenceWorker`'s raw per-cycle
`DetectionSnapshot` and `VisionArcadeApp`'s `AtomicReference` — it matches each new detection to the
nearest same-label track from the previous update, exponentially smooths the box toward it, and keeps a
track alive through a brief miss (400ms grace period) instead of vanishing for one frame. Milestone 6
(second game: Vision Pong) is implemented: `game.PongEngine` is a pure state machine
(`tick(nowNanos, DetectionSnapshot, frameHeight)`, no camera/JavaFX dependency) where a detected
`"person"` controls a paddle — the controller box's vertical center, normalized by frame height and
clamped to [0,1], is the paddle's target position, eased toward each tick and held steady when the
controller isn't detected. A ball bounces around a normalized [0,1]x[0,1] field off the top/bottom/left
walls and the paddle on the right wall; a miss shows a result message for 1.5s then restarts, mirroring
`GameEngine`'s round/result/restart shape. `VisionArcadeApp` now ticks `PongEngine` instead of
`GameEngine` per render frame; `CameraPreviewView.showPong()` draws the field/paddle/ball/score on the
existing overlay `Canvas`. `GameEngine`/`GameState`/`CameraPreviewView.showGame()` (Object Hunt) are
untouched and still tested — the app runs one game at a time, same as every milestone before it; there
is no mode selector.

Current milestone: **Milestone 7 — Quality and portfolio polish** (see `VisionArcade_docs/TASKS.md` for the
live checklist — don't duplicate it here, it changes often).

## Documentation map

This repo is doc-driven. Read in this order before changing code (same order `VisionArcade_docs/AGENTS.md`
and `FILE_INDEX.md` specify):

1. `VisionArcade_docs/PRD.md` — product scope, MVP definition, non-goals
2. `VisionArcade_docs/ARCHITECTURE.md` — design, threading model, package shape
3. `VisionArcade_docs/ROADMAP.md` — sequential milestones (do not jump ahead)
4. `VisionArcade_docs/TASKS.md` — what's actually next, right now
5. `VisionArcade_docs/DEVELOPMENT.md` — toolchain/dependency versions and policy
6. `VisionArcade_docs/TESTING.md`, `VisionArcade_docs/BENCHMARKS.md`
7. `VisionArcade_docs/docs/ADR/` — accepted architectural decisions (0001 Java/UI baseline, 0002
   vision/inference boundaries, 0003 latest-frame backpressure)

## Commands

Always use the wrapper, never a globally installed Gradle:

```bash
./gradlew build
./gradlew test
./gradlew run
```

Single test class: `./gradlew test --tests "org.example.visionarcade.camera.OpenCvNativeLoadingTest"`

Camera-hardware tests are gated and skipped by default (they'd prompt for macOS camera permission and
aren't safe in CI): `./gradlew test -Dvisionarcade.hardwareTests=true --tests CameraCaptureSpikeTest`

## Working rules (from `VisionArcade_docs/AGENTS.md`)

- Implement only the current milestone in `TASKS.md`. Don't build ahead because it's technically possible.
- Baseline stack per `DEVELOPMENT.md`: Java 25 LTS, Gradle 9.x wrapper, JavaFX 25, OpenCV 4.14.x, ONNX
  Runtime Java (direct bindings, no DJL), JUnit 6. Pin versions; no dynamic versions.
- Keep Java as the production runtime. Python is reserved for future model training/eval/export only —
  never a Python inference server for the desktop app.
- Never queue unbounded camera frames — latest-frame/single-slot handoff only (ADR 0003). Real-time
  latency matters more than processing every frame.
- Prefer immutable value objects across thread boundaries.
- Keep JavaFX work on the JavaFX Application Thread; never block it with camera capture or ML inference.
- Keep computer-vision/model code behind narrow interfaces (e.g. `ObjectDetector`) so the model/runtime
  can change without touching game logic.
- Do not add Spring Boot, Docker, cloud infra, user accounts, multiplayer, a database, TensorFlow, or DJL
  without a concrete current requirement.
- Do not commit large model binaries; `models/*.onnx` is meant to be gitignored.
- Avoid interfaces with only one implementation unless they isolate a volatile boundary (camera, detector,
  clock, persistence). Prefer composition over inheritance. No generic `Utils` dumping ground.
- Record a new ADR only for a meaningful, hard-to-reverse technical decision — not routine implementation.

## Architecture

Target data flow (`ARCHITECTURE.md` §2); the `CameraCapture -> FrameSlot` half is implemented, the rest
(`ObjectDetector` onward) is Milestone 2+:

```
Webcam -> CameraCapture -> FrameSlot (latest-frame only) -> ObjectDetector (ONNX Runtime)
       -> DetectionState (immutable snapshot) -> GameEngine -> JavaFX Renderer
```

Three execution contexts only for the MVP — do not add a separate game thread until profiling proves it
necessary:
- **Camera worker** (`camera.CameraCaptureWorker`, implemented): opens camera, captures/timestamps
  frames, publishes latest safe frame via `camera.FrameSlot`, releases on shutdown.
- **Inference worker** (Milestone 2+, not implemented): reads latest frame, preprocesses, runs ONNX
  inference, publishes latest immutable detection snapshot. Drops stale frames if inference is slower
  than capture.
- **JavaFX Application Thread** (`app.VisionArcadeApp`, implemented for the camera feed): UI, update/
  timing via `AnimationTimer`, rendering (`ui.FrameImageConverter` + `ui.CameraPreviewView`). Will also
  read the latest detection snapshot once Milestone 2 lands.

Package shape (`ARCHITECTURE.md` §6) — don't add `service`/`manager`/`repository`/`domain`/
`infrastructure` layers without a real need:

```
...visionarcade
├── app     — application lifecycle/bootstrap (VisionArcadeApp)
├── camera  — camera capture + frame ownership (CameraSource, FrameSnapshot, FrameSlot, CameraCaptureWorker)
├── vision  — preprocessing, detector, detections, postprocessing (Milestone 2+, not yet created)
├── game    — game state + Object Hunt rules (GameEngine, GameState)
└── ui      — JavaFX rendering/controllers (FrameImageConverter, CameraPreviewView)
```

Native resource ownership rules (`ARCHITECTURE.md` §9), as implemented: `FrameSnapshot` owns its `Mat`
and releases it in `close()`; `FrameSlot.publish()` releases whatever snapshot it supersedes rather than
queuing it; the consumer (`VisionArcadeApp`'s `AnimationTimer`) takes exclusive ownership via
`FrameSlot.take()` and must close what it takes.

Note: JavaFX's `PixelFormat` has no 3-byte BGR variant (only RGB/BGRA), so `FrameImageConverter` converts
BGR→RGB via OpenCV's `cvtColor` before writing — don't "fix" this back to a nonexistent BGR format.

## Before declaring a task complete

- Build the project and run automated tests.
- Manually verify milestone behavior when hardware/UI is involved (camera, JavaFX window).
- Update `VisionArcade_docs/TASKS.md` to reflect reality.
- Update architecture docs only if the implementation actually changed.
