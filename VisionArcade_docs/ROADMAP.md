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

---

## Milestone 9 — Pose estimation: Pose Match game

**Goal:** integrate a pretrained human-pose model (COCO 17-keypoint) behind a narrow
`PoseEstimator` boundary (mirroring `ObjectDetector`), and build a third game — Pose Match — where
the player matches a target pose shown on screen using real body position. No custom training:
standard pose models ship pretrained on COCO keypoints, so this is spike-sized work like
Milestone 2, not pipeline-sized work like Milestone 8.

Tasks:
- export a pretrained COCO-keypoint pose model to ONNX via the existing `ml/` Python workspace (no
  training)
- inspect its input/output metadata (one-time spike, same shape as Milestone 2's detector spike)
- `vision.PoseEstimator` interface + implementation: preprocessing, inference, decode one person
  box + 17 keypoints + confidences; take only the highest-confidence person in multi-person frames,
  consistent with Vision Pong's single-controller convention
- `vision.Pose`/`Keypoint` immutable value objects
- `game.PoseMatchEngine`: pure state machine (`tick(nowNanos, Pose)`, no camera/JavaFX dependency)
  — pick a target pose from a small fixed pool (e.g. "Arms Up", "T-Pose", "One Leg Up"), score the
  live pose against the target with simple geometric heuristics (relative joint positions, not
  exact silhouette distance), require a continuous match for ~600ms (same threshold Object Hunt
  uses), round timer, result display, restart — same shape as `GameEngine`/`PongEngine`
- `CameraPreviewView.showPoseMatch()`: skeleton overlay (joint lines) + target pose name + match %
  + result message, same Canvas overlay pattern as the existing games
- `VisionArcadeApp` ticks `PoseMatchEngine` instead of `PongEngine` — same "one game at a time, no
  mode selector" convention as the Milestone 4 → Milestone 6 swap

Definition of done:
- pretrained pose ONNX model loads and decodes to sensible keypoints on a webcam frame
- several Pose Match rounds can be played end-to-end, feeling responsive on the primary dev machine
- `PoseMatchEngine` state-machine rules unit tested without a camera, mirroring
  `GameEngineTest`/`PongEngineTest`
- `GameEngine`/`PongEngine` untouched and still tested

Do not:
- train or fine-tune a pose model — use its COCO-pretrained weights as-is
- handle more than one tracked person — take the most confident detection only
- build a mode selector

---

## Milestone 10 — Mode selector

**Goal:** let the player pick Object Hunt, Vision Pong, or Pose Match from an in-app menu instead
of needing a code change. Reverses the "no mode selector" scope limit every milestone up to now
deliberately deferred — now that all three games exist, this is the natural next step, not scope
creep.

Tasks:
- `app.GameMode` enum (OBJECT_HUNT, VISION_PONG, POSE_MATCH) with a display name
- `ui.ModeSelectView`: a simple overlay with a title and one button per `GameMode`, shown over the
  live camera feed
- `VisionArcadeApp` restructured around two lazily-started vision pipelines instead of one fixed
  one: the object-detection pipeline (`ObjectDetector`/`InferenceWorker`/`DetectionTracker`, shared
  by Object Hunt and Vision Pong — switching between those two never reloads the model) and the
  pose pipeline (`PoseEstimator`/`PoseInferenceWorker`). Selecting a mode starts whichever pipeline
  it needs and stops the other if it was running; the camera worker itself stays running
  continuously across mode switches
- picking a mode always creates a fresh engine instance (`GameEngine`/`PongEngine`/
  `PoseMatchEngine`) — score/round state never carries over between menu visits
- Escape returns to the menu from any game; the render loop keeps showing the live camera feed
  behind the menu, just skips ticking/drawing a game overlay while no mode is selected
- `CameraPreviewView` gains a small `clearOverlay()` so stale HUD drawings don't linger when
  returning to the menu

Definition of done:
- app launches to the menu, not straight into a game
- all three games are reachable and playable from the menu in the same app session, including
  switching directly between them without restarting the app
- switching between Object Hunt and Vision Pong does not visibly reload the detector (no pause for
  model loading)
- `GameEngine`/`PongEngine`/`PoseMatchEngine` and their existing tests are untouched

Do not:
- preload all pipelines up front — lazy-start only the one the chosen mode needs
- add persistence/high-scores — out of scope for this milestone (ROADMAP's own "only here
  consider" list, still just under consideration)
