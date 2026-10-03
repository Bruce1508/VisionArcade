# VisionArcade Architecture

Last reviewed: 2026-10-03

## 1. Design goals

- Low perceived latency
- Responsive JavaFX UI
- Simple, testable game logic
- Replaceable model/runtime boundary
- Clear native-resource ownership
- Minimal architecture for the MVP

## 2. High-level flow

```text
Webcam
  |
  v
CameraCapture
  |
  | latest frame only
  v
FrameSlot
  |
  v
ObjectDetector (ONNX Runtime)
  |
  | immutable detection snapshot
  v
DetectionState
  |
  v
GameEngine
  |
  v
JavaFX Renderer
```

The production app remains Java. Python is a development-time tool only when training/evaluating/exporting models later.

## 3. MVP threading model

Keep the first implementation to three execution contexts.

### Camera worker
Responsibilities:
- open camera
- capture frames
- timestamp frames
- publish the latest safe frame
- release camera on shutdown

It must not build a backlog.

### Inference worker
Responsibilities:
- read the latest available frame
- preprocess it
- run ONNX inference
- postprocess model output
- publish the latest immutable detection snapshot

If inference is slower than capture, old frames are discarded.

### JavaFX Application Thread
Responsibilities:
- JavaFX controls
- game update/timing via `AnimationTimer` or equivalent
- render camera image/overlay/game HUD
- read the most recent detection snapshot

For the MVP, do **not** add a separate game thread. Add one only if profiling later proves it necessary.

## 4. Backpressure strategy

Real-time interaction values freshness over completeness.

Use a single-slot/latest-value design conceptually:

```text
camera produces F1 F2 F3 F4 F5
                    ^
inference finishes and takes the newest available frame
```

Avoid:

```text
F1 -> F2 -> F3 -> F4 -> F5 -> ... unbounded queue
```

An unbounded queue increases latency until the game reacts to the past.

## 5. Data boundaries

Suggested value objects:

### FrameSnapshot
- frame id
- capture timestamp
- image/frame data reference with explicit ownership

### Detection
- label
- confidence
- bounding box

### DetectionSnapshot
- source frame id/timestamp
- immutable list of detections
- inference timing metadata

### BoundingBox
Prefer normalized or clearly documented pixel coordinates. Do not mix coordinate systems implicitly.

## 6. Package shape

Start small:

```text
...visionarcade
├── app
│   └── application lifecycle/bootstrap
├── camera
│   └── camera capture + frame ownership
├── vision
│   └── preprocessing, detector, detections, postprocessing
├── game
│   └── game state + Object Hunt rules
└── ui
    └── JavaFX rendering/controllers
```

Do not create separate `service`, `manager`, `util`, `repository`, `domain`, or `infrastructure` layers without a real need.

## 7. Important interfaces

Create interfaces only at volatile or hardware-dependent boundaries.

Likely useful:

```java
interface ObjectDetector {
    DetectionSnapshot detect(FrameSnapshot frame);
}
```

Potentially useful for tests:

```java
interface CameraSource { ... }
```

Do not create an interface for every class.

## 8. Technology decisions

### Java
Use **Java 25 LTS** for a new project in late 2026.

### JavaFX
Use the Java 25-aligned stable JavaFX line. Keep the first UI programmatic/simple; do not introduce FXML unless screens become complex enough to justify it.

### OpenCV
Target **OpenCV 4.14.x** for the MVP.

OpenCV 5.0.0 exists, but it is a recent major release. The project does not need 5.x-specific features, so 4.14.x reduces migration/churn risk while remaining current.

Native packaging/loading must be validated on the primary development machine during Milestone 0. Prefer the dependency approach that works cleanly on macOS Apple Silicon without manual per-run setup.

### ONNX Runtime
Use the direct Java binding. Avoid DJL initially; it would add an abstraction layer before the project needs multiple ML engines.

Pinned in Milestone 2: `com.microsoft.onnxruntime:onnxruntime:1.30.0`, verified on macOS arm64.

### Model
First candidate: a small current Ultralytics detection model exported to ONNX, such as **YOLO26n**.

Keep the detector behind `ObjectDetector` because model output formats can change. If YOLO26 export/postprocessing causes unnecessary friction, using another small ONNX detector or YOLO11n is acceptable without changing game code.

Do not train a custom model for the MVP.

## 9. Native resource ownership

Native computer-vision objects can leak memory if ownership is unclear.

Rules:
- the component creating a native resource owns its release unless ownership is explicitly transferred;
- do not allow multiple threads to mutate the same native image object;
- stale frames replaced in the latest-frame slot must become eligible for cleanup/release;
- shutdown must stop workers before releasing shared native runtime resources.

## 10. Error handling

User-visible failures:
- camera unavailable
- camera permission denied
- model missing
- model incompatible
- inference initialization failure

Do not hide these behind generic `Exception` messages. Log technical detail and show a concise user-facing message.

## 11. What we deliberately do not have

For the MVP there is:
- no Spring container
- no event bus
- no database
- no networking
- no microservices
- no custom dependency-injection framework
- no generalized ECS/game framework
- no separate game thread
- no deep inheritance tree

Add complexity only after measurement or a concrete product requirement.
