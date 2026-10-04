# Current Tasks

Current milestone: **Milestone 7 — Quality and portfolio polish**

Milestones 0-6 (toolchain/JavaFX/OpenCV/camera validation, live webcam preview, ONNX detection
spike, real-time visualization, Object Hunt MVP, tracking/smoothing, Vision Pong) are complete —
history preserved in git log.

This file should describe the work that is actually next, not the entire product backlog.

## TODO

- [x] Test failure paths. `InferenceWorker` previously let any detector exception kill its thread
  silently, freezing detections forever with no visible error — added an `onError` callback
  (matching `CameraCaptureWorker`'s existing one), wired to `previewView.showError`, covered by a
  new `InferenceWorkerTest` case. Added a `CameraCaptureWorkerTest` case for the pre-existing
  "lost camera feed" path, which was implemented but untested.
- [x] Tighten resource cleanup. `OpenCvCameraSource.open()` left a half-initialized native
  `VideoCapture` handle unreleased when `isOpened()` returned false; now releases and nulls it.
- [x] Code cleanup pass. Audited camera/vision/game/ui/app for dead code, TODOs, and duplication —
  found none worth changing; the codebase was already disciplined from prior milestones' review
  passes.
- [x] Packaging experiment. `./gradlew installDist` works with no build.gradle changes, but
  produces a ~473MB distribution (all-OS/arch native libs from the `*-platform` uber-artifacts).
  Documented in `DEVELOPMENT.md`; not trimmed per the user's call (document, don't build it).
- [x] README/demo media. Updated `VisionArcade_docs/README.md`'s progress list and commands
  section to match reality (M1-M6 done, M7 in progress). Demo media (screenshot/GIF) not added —
  needs a human to capture the running app.
- [x] Optional CI. Added `.github/workflows/build.yml` — `./gradlew build` on push/PR to `main`.
  Hardware tests default off (`visionarcade.hardwareTests=false`); model-gated integration test
  auto-skips since `models/*.onnx` isn't committed — CI should be green with no extra config.
- [x] Benchmark repeatable scenarios. User ran `./gradlew run` and reported the live HUD/Activity
  Monitor readings (28-30 fps render, ~15ms inference, ~30% CPU on an Apple M4 Pro); recorded in
  `BENCHMARKS.md`'s Benchmark C with the full test-environment header. Benchmarks A/B (camera-only,
  fixed-image-set) and percentile/memory figures remain TBD — would need actual instrumentation,
  not just the live HUD.

## Do not implement yet

- persistence, custom training, pose detection, more games — ROADMAP.md says these are only to be
  *considered* once M7 is otherwise done, not built as part of it
- a mode selector between Object Hunt and Vision Pong — unchanged from Milestone 6
- trimming the packaging distribution or attempting `jpackage` — experiment documented, not
  pursued further per the user's call this session

## Notes for Codex

Before editing:
1. inspect the existing project;
2. preserve the Milestone 0–5 toolchain/JavaFX/OpenCV/camera/vision/game wiring;
3. avoid replacing working configuration unnecessarily.

After editing:
1. run the build/tests;
2. manually verify the live preview, detection overlay, **and that standing/crouching in front of
   the camera moves the paddle predictably and the ball/score/miss/restart cycle works** on the
   primary development machine;
3. do not start a new milestone automatically.
