# Current Tasks

Current milestone: **Milestone 1 — Live webcam view**

Milestone 0 is complete (toolchain, JavaFX window, OpenCV native loading, camera
permission/capture all verified — history preserved in git log).

This file should describe the work that is actually next, not the entire product backlog.

## TODO

- [x] Camera lifecycle: open on app start, release cleanly on shutdown. (`OpenCvCameraSource` + `CameraCaptureWorker`.)
- [x] Background capture worker on a dedicated thread, never blocking the JavaFX Application Thread. (`CameraCaptureWorker` runs on its own daemon thread.)
- [x] Latest-frame strategy — never queue frames. (`FrameSlot`: single-slot `AtomicReference` handoff per ADR 0003; superseded frames are released, not queued — covered by `FrameSlotTest`.)
- [x] Frame-to-JavaFX image conversion. (`FrameImageConverter`: OpenCV BGR→RGB via `cvtColor`, then `PixelWriter` + `PixelFormat.getByteRgbInstance()` — JavaFX has no 3-byte BGR format.)
- [x] Start/stop handling tied to the JavaFX Application lifecycle. (`VisionArcadeApp.start()`/`stop()` start/stop the capture worker and render `AnimationTimer` together.)
- [x] Camera error state: user-visible message when the camera can't be opened or the feed is lost. (`CameraPreviewView.showError()`, driven by `CameraCaptureWorker`'s error callback via `Platform.runLater`.)

## Definition of done

Milestone 1 is done only when:

- live preview is responsive — **confirmed manually: live feed renders smoothly.**
- there is no growing frame queue — single-slot `FrameSlot` design, verified by `FrameSlotTest`.
- closing the app releases the camera — `Application.stop()` calls `CameraCaptureWorker.stop()`, which releases the `VideoCapture`.
- the UI remains responsive for a sustained manual run — **confirmed manually.**

## Do not implement yet

- ONNX Runtime
- YOLO
- object detection
- bounding boxes
- game engine
- Object Hunt
- tracking
- persistence
- Spring Boot
- Docker
- Python/PyTorch environment

## Notes for Codex

Before editing:
1. inspect the existing project;
2. preserve the Milestone 0 toolchain/JavaFX/OpenCV wiring;
3. avoid replacing working configuration unnecessarily.

After editing:
1. run the build/tests;
2. manually verify the live preview on the primary development machine;
3. do not start Milestone 2 automatically.
