# Current Tasks

Milestones 0-9 (toolchain/JavaFX/OpenCV/camera validation, live webcam preview, ONNX detection
spike, real-time visualization, Object Hunt MVP, tracking/smoothing, Vision Pong, quality/
portfolio polish, custom fine-tune, pose estimation/Pose Match) are complete — history preserved
in git log.

No milestone is currently in progress. See ROADMAP.md for possible next directions (none chosen
yet) before starting new work.

This file should describe the work that is actually next, not the entire product backlog.

## TODO

- [x] Export a pretrained COCO-keypoint pose model (no training) to ONNX via the existing `ml/`
  Python workspace.
- [x] Inspect the exported model's input/output metadata (one-time spike). Confirmed `[1, 56, 8400]`: box + person confidence + 17 keypoints x (x,y,conf).
- [x] `vision.Pose`/`Keypoint` immutable value objects.
- [x] `vision.PoseEstimator` interface + `Yolo26nPoseEstimator` implementation: preprocess, infer,
  decode one person box + 17 keypoints + confidences via `PoseDecoder`; keeps only the
  highest-confidence anchor (one person). `Yolo26nPoseEstimatorIntegrationTest` confirms a real
  person's pose decodes correctly from the pretrained model.
- [x] `game.PoseMatchEngine`: pure state machine, 3-pose target pool (`TargetPose`: Arms Up,
  T-Pose, One Leg Up) scored with torso-scaled geometric heuristics, 600ms continuous-match
  threshold, 20s round timer, 1.5s result display, restart. Unit tested in
  `PoseMatchEngineTest` (7 tests, no camera) mirroring `GameEngineTest`/`PongEngineTest`.
- [x] `CameraPreviewView.showPoseMatch()` + `showPose()`: skeleton overlay (joint lines/dots) and
  target pose name/match %/score/timer HUD, same Canvas overlay pattern as the existing games.
- [x] Wired `VisionArcadeApp` to run `PoseInferenceWorker`/`Yolo26nPoseEstimator` and tick
  `PoseMatchEngine` instead of the Pong wiring — `GameEngine`/`PongEngine`/`InferenceWorker`/
  `Yolo26nObjectDetector` untouched and still tested, same convention as the M4→M6 swap.
- [x] Manually verified Pose Match end-to-end on the primary dev machine — user confirmed it
  looks correct (skeleton overlay, match %, round/result/restart cycle).

## Do not implement yet

- a COCO-80-index-space re-evaluation to get a fair stock-model accuracy baseline for Milestone 8
  — not attempted that round; still open if the user wants a real accuracy before/after
- training or fine-tuning the pose model — use its COCO-pretrained weights as-is (ROADMAP.md
  Milestone 9)
- handling more than one tracked person in Pose Match — take the most confident detection only
- persistence, more games beyond Pose Match — still only to be *considered* per ROADMAP.md
- a mode selector between Object Hunt, Vision Pong, and Pose Match

## Notes for Codex

Before editing:
1. inspect the existing project;
2. preserve the Milestone 0–9 toolchain/JavaFX/OpenCV/camera/vision/game wiring;
3. avoid replacing working configuration unnecessarily.

After editing:
1. run the build/tests;
2. manually verify the live preview and whatever game is currently wired into
   `VisionArcadeApp` plays end-to-end on the primary development machine;
3. do not start a new milestone automatically.
