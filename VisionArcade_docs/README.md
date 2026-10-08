# VisionArcade

VisionArcade is a Java desktop project that turns webcam-based computer vision into game input.

Instead of being a bounding-box demo, the application uses detected objects or body movement to drive interactive mini-games.

**Core flow**

`real world -> webcam -> computer vision -> detections -> game logic -> JavaFX`

## Three playable modes

**Object Hunt** — the game asks for an object (a bottle, a cup, ...), the player shows it to the
webcam, the detector confirms it with a confidence threshold and short stability window, and the
game awards points and starts the next round.

**Vision Pong** — the player's own body (the `"person"` detection) controls a paddle by
standing/crouching; a ball bounces around the field off the walls and the paddle, with score and
automatic restart on a miss.

**Pose Match** — a pretrained pose model tracks the player's 17-keypoint skeleton; the game shows
a target pose (Arms Up, T-Pose, One Leg Up) and the player must hold a matching real-world pose
for a short stability window to score a point before the round timer runs out.

Only one game runs at a time; there is no in-app mode selector yet — `VisionArcadeApp` currently
runs Pose Match (Milestone 9).

## Progress

1. Project + native dependency validation — done
2. JavaFX + webcam — done
3. ONNX object detection — done
4. Bounding-box visualization — done
5. Object Hunt MVP — done
6. Tracking/smoothing — done
7. Second game (Vision Pong) — done
8. Testing, benchmarking, packaging, polish — done (Milestone 7)
9. Custom fine-tune on the app's actual class set — done, reference artifact only (Milestone 8)
10. Pose estimation + third game (Pose Match) — code complete, pending manual live verification (Milestone 9)

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
- TensorFlow in the Java runtime
- production-grade packaging

## Commands

```bash
./gradlew run
./gradlew test
./gradlew build
```

Camera-hardware tests are gated and skipped by default: `./gradlew test -Dvisionarcade.hardwareTests=true`.

## Performance targets

Targets are not claims. Actual values must be measured and recorded in `BENCHMARKS.md`.

- Camera capture FPS
- Inference FPS
- p50 / average inference latency
- p95 inference latency
- end-to-end frame-to-display latency
- CPU usage
- memory usage
