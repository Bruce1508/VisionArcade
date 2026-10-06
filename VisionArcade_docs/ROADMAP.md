# VisionArcade Roadmap

The roadmap is sequential. Do not start a later milestone while the current milestone is unstable.

## Milestone 0 — Project and technical validation

**Goal:** prove the toolchain and native dependencies work on the development machine.

Tasks:
- Java 25 LTS toolchain
- Gradle wrapper
- minimal JavaFX app
- one JUnit test
- validate OpenCV native loading on macOS Apple Silicon
- validate camera permission/capture with a tiny spike

Definition of done:
- `build`, `test`, and `run` work
- JavaFX window opens
- a camera frame can be captured
- shutdown is clean

Do not add ONNX yet.

---

## Milestone 1 — Live webcam view

**Goal:** stable live camera preview in JavaFX.

Tasks:
- camera lifecycle
- background capture worker
- latest-frame strategy
- frame-to-JavaFX image conversion
- start/stop handling
- camera error state

Definition of done:
- live preview is responsive
- no growing frame queue
- closing app releases camera
- UI remains responsive for a sustained manual run

---

## Milestone 2 — ONNX detection spike

**Goal:** load one model and detect objects from frames.

Tasks:
- pin ONNX Runtime Java dependency
- load model once
- inspect model input/output metadata
- preprocessing
- inference
- postprocessing
- confidence filtering

Definition of done:
- known test image produces sensible detections
- webcam frame can be passed through the same detector
- inference timing is recorded

---

## Milestone 3 — Real-time visualization

**Goal:** combine live camera and detections.

Tasks:
- inference worker
- immutable detection snapshots
- box/label rendering
- coordinate transforms
- basic FPS/latency debug overlay

Definition of done:
- boxes align correctly with objects
- UI remains responsive
- stale frames are dropped
- latency does not grow over time

---

## Milestone 4 — Object Hunt MVP

**Goal:** first playable game.

Tasks:
- target class pool
- round timer
- score
- confidence threshold
- stability window
- round success/failure
- restart

Definition of done:
- several rounds can be played end-to-end
- game rules are unit tested without a camera

---

## Milestone 5 — Tracking and smoothing

**Goal:** make positional control stable enough for games.

Only implement what the second game needs.

Possible tasks:
- box center smoothing
- simple nearest-match tracking
- lost-object timeout
- jitter metrics

Do not introduce a heavy tracking framework unless simple methods fail.

---

## Milestone 6 — Second game: Vision Pong

**Goal:** use a real object as a physical controller.

Tasks:
- choose one detected class/controller
- map object position to paddle position
- clamp/smooth control
- simple Pong physics
- score/restart

Definition of done:
- physical movement feels predictable and playable

---

## Milestone 7 — Quality and portfolio polish

Tasks:
- benchmark repeatable scenarios
- test failure paths
- tighten resource cleanup
- improve README/demo media
- packaging experiment
- optional CI for build/test
- code cleanup based on actual complexity

Only here consider:
- persistence
- custom training
- pose detection
- more games

---

## Milestone 8 — Custom fine-tune: app's actual class set

**Goal:** fine-tune the detector on only the classes the app uses, and measure the real
speed/accuracy difference against the stock 80-class model — a concrete "custom training" result
(ROADMAP's M7 "only here consider" list), not a from-scratch model.

Tasks:
- fine-tune YOLO26n (starting from its COCO-pretrained weights, not from scratch) on the 11
  classes the app actually detects: `cup, bottle, cell phone, book, scissors, clock, backpack,
  mouse, keyboard, remote` (Object Hunt's pool) + `person` (Vision Pong's controller)
- build a small COCO-subset dataset for those 11 classes (images + remapped YOLO-format labels),
  not a full 80-class COCO download
- export the fine-tuned checkpoint to ONNX, same pattern as the pinned model in `DEVELOPMENT.md`
- extend `Yolo26nObjectDetector` to accept its class list as a parameter instead of the hardcoded
  80-class `COCO_CLASSES` array, so both models can be loaded and compared
- benchmark inference latency of the stock model vs. the fine-tuned model on the same fixed image
  set (fills in `BENCHMARKS.md`'s still-TBD Benchmark B properly, instead of a manual HUD read)
- record the fine-tuned model's held-out validation mAP (measured, not estimated)

Definition of done:
- fine-tuned ONNX model loads and runs through the existing `ObjectDetector` interface with no
  Java-side architecture change beyond the class-list parameter
- `BENCHMARKS.md` has a real before/after latency comparison plus the new model's measured mAP
- `DEVELOPMENT.md` records the fine-tuned model's provenance the same way the pinned model is
  documented (dataset size, class list, training command, checksum)

Do not swap the live app over to the fine-tuned model without the user confirming it after seeing
the before/after numbers.
