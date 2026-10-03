# VisionArcade Testing Strategy

Testing should grow with the product. Do not build a large test harness before there is behavior to test.

## 1. Unit tests

Best targets:
- bounding-box math
- coordinate mapping
- confidence filtering
- stability-window logic
- scoring
- timers through an injected/test clock if needed
- game state transitions
- simple smoothing/tracking math

These tests should not require a webcam or native model.

## 2. Integration smoke tests

Use sparingly for:
- OpenCV/native library loading
- opening/closing a camera when hardware tests are explicitly enabled
- ONNX Runtime initialization
- loading the selected model
- running inference on one known image

Hardware-dependent tests should not make the normal unit-test suite flaky.

## 3. UI testing

Do not add a JavaFX UI automation framework during early milestones.

Prefer:
- unit tests for game/state logic;
- manual verification checklist for camera/render behavior.

Add UI automation only if the UI becomes complex enough to justify maintenance cost.

## 4. Performance tests

Performance numbers are benchmark results, not pass/fail unit tests at first.

Measure:
- capture FPS
- inference latency
- inference FPS
- end-to-end latency
- memory trend during sustained use

Do not create arbitrary performance thresholds before collecting a baseline.

## 5. Test philosophy

- test behavior, not private implementation details;
- avoid mocks when a simple fake is clearer;
- add Mockito only when it reduces real test complexity;
- use deterministic fake detections for game tests;
- reproduce bugs with a test when practical.
