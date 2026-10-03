# Current Tasks

Current milestone: **Milestone 2 — ONNX detection spike**

Milestone 0 (toolchain/JavaFX/OpenCV/camera validation) and Milestone 1 (live webcam preview) are
complete — history preserved in git log.

This file should describe the work that is actually next, not the entire product backlog.

## TODO

- [x] Pin ONNX Runtime Java dependency. (`com.microsoft.onnxruntime:onnxruntime:1.30.0`, latest stable per Maven Central metadata, verified running on macOS arm64.)
- [x] Load the model once. (`Yolo26nObjectDetector` loads `models/yolo26n.onnx` via `OrtEnvironment`/`OrtSession` in its constructor.)
- [x] Inspect model input/output metadata. (Input `images` float32 `[1,3,640,640]`; output `output0` float32 `[1,84,8400]` — 4 box coords + 80 COCO class scores, no separate objectness. Input/output tensor names are read from the session at runtime, not hardcoded.)
- [x] Preprocessing. (`Yolo26nObjectDetector` letterbox-resizes via OpenCV `resize`+`copyMakeBorder`, then `opencv_dnn.blobFromImage` for BGR→RGB/normalize/NCHW.)
- [x] Inference. (`OrtSession.run` on the CPU execution provider.)
- [x] Postprocessing. (`YoloDetectionDecoder` decodes boxes, maps letterboxed coordinates back to the original frame via `LetterboxTransform`.)
- [x] Confidence filtering. (`YoloDetectionDecoder` applies a configurable confidence threshold plus per-class NMS, default conf=0.25/IoU=0.45 matching Ultralytics defaults.)

## Definition of done

Milestone 2 is done only when:

- a known test image produces sensible detections — **verified: `Yolo26nObjectDetectorIntegrationTest.detectsKnownObjectsInTestImage()` runs the real model against `src/test/resources/vision/bus.jpg` and asserts both "bus" and "person" are detected.**
- a webcam frame can be passed through the same detector — **verified manually: `Yolo26nObjectDetectorIntegrationTest.detectsOnARealWebcamFrame()` (gated behind `-Dvisionarcade.hardwareTests=true`) opens the real camera, grabs one frame, and runs it through `Yolo26nObjectDetector.detect()` successfully.**
- inference timing is recorded — **verified: both integration tests assert `DetectionSnapshot.inferenceNanos() > 0`.**

## Do not implement yet

- game engine
- Object Hunt
- tracking
- persistence
- Spring Boot
- Docker
- Python/PyTorch environment (the export step used a throwaway local venv, not a project dependency — see `DEVELOPMENT.md`)
- wiring the detector into `VisionArcadeApp`'s render loop, a dedicated inference worker thread, or box/label rendering — that is Milestone 3

## Notes for Codex

Before editing:
1. inspect the existing project;
2. preserve the Milestone 0/1 toolchain/JavaFX/OpenCV/camera wiring;
3. avoid replacing working configuration unnecessarily.

After editing:
1. run the build/tests;
2. manually verify the live preview on the primary development machine;
3. do not start Milestone 3 automatically.
