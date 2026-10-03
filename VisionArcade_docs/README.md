# VisionArcade

VisionArcade is a Java desktop project that turns webcam-based computer vision into game input.

Instead of being a bounding-box demo, the application uses detected objects or body movement to drive interactive mini-games.

**Core flow**

`real world -> webcam -> computer vision -> detections -> game logic -> JavaFX`

## MVP

The first playable mode is **Object Hunt**:

1. The game asks for an object, such as a bottle or cup.
2. The player shows it to the webcam.
3. The detector confirms the object with a confidence threshold and short stability window.
4. The game awards points and starts the next round.

## Planned progression

1. Project + native dependency validation
2. JavaFX + webcam
3. ONNX object detection
4. Bounding-box visualization
5. Object Hunt MVP
6. Tracking/smoothing
7. Second game, likely Vision Pong
8. Testing, benchmarking, packaging, polish

## Baseline stack

Reviewed: **2026-10-03**

- Java 25 LTS
- Gradle 9.x wrapper
- JavaFX 25
- OpenCV 4.14.x for camera/image operations
- ONNX Runtime Java for inference
- YOLO-family ONNX model for the first detector
- JUnit 6
- Python/PyTorch later for optional training, evaluation, and ONNX export

See `ARCHITECTURE.md` for the design and `TASKS.md` for the current work.

## Non-goals for the MVP

- cloud backend
- multiplayer
- accounts/authentication
- Spring Boot
- database persistence
- custom model training
- TensorFlow in the Java runtime
- pose estimation
- production-grade packaging

## Commands

Commands will be finalized after the Gradle scaffold is applied.

Expected shape:

```bash
./gradlew run
./gradlew test
./gradlew build
```

## Performance targets

Targets are not claims. Actual values must be measured and recorded in `BENCHMARKS.md`.

- Camera capture FPS
- Inference FPS
- p50 / average inference latency
- p95 inference latency
- end-to-end frame-to-display latency
- CPU usage
- memory usage
