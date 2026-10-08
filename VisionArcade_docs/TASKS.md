# Current Tasks

Milestones 0-10 (toolchain/JavaFX/OpenCV/camera validation, live webcam preview, ONNX detection
spike, real-time visualization, Object Hunt MVP, tracking/smoothing, Vision Pong, quality/
portfolio polish, custom fine-tune, pose estimation/Pose Match, mode selector) are complete —
history preserved in git log.

No milestone is currently in progress. See ROADMAP.md for possible next directions (none chosen
yet) before starting new work.

This file should describe the work that is actually next, not the entire product backlog.

## TODO

- [x] `app.GameMode` enum (OBJECT_HUNT, VISION_PONG, POSE_MATCH) with a display name.
- [x] `ui.ModeSelectView`: title + one button per `GameMode`, overlaid on the live camera feed.
- [x] `CameraPreviewView.clearOverlay()` so returning to the menu doesn't leave stale HUD drawings.
- [x] Restructured `VisionArcadeApp` around two lazily-started pipelines (object-detection,
  shared by Object Hunt/Vision Pong; pose, for Pose Match) instead of one fixed pipeline.
  Camera worker stays running continuously across mode switches; switching Hunt↔Pong never
  reloads the detector since both share one `activePipeline == OBJECT` check.
- [x] Selecting a mode (from the menu or switching directly between games) always creates a
  fresh engine instance; Escape returns to the menu from any game.
- [x] Manually verified on the primary dev machine — user confirmed it looks correct.

## Do not implement yet

- a COCO-80-index-space re-evaluation to get a fair stock-model accuracy baseline for Milestone 8
  — not attempted that round; still open if the user wants a real accuracy before/after
- training or fine-tuning the pose model — use its COCO-pretrained weights as-is (ROADMAP.md
  Milestone 9)
- handling more than one tracked person in Pose Match — take the most confident detection only
- persistence, more games beyond the existing three — still only to be *considered* per ROADMAP.md
- preloading all pipelines up front — lazy-start only the pipeline the chosen mode needs

## Notes for Codex

Before editing:
1. inspect the existing project;
2. preserve the Milestone 0–9 toolchain/JavaFX/OpenCV/camera/vision/game wiring;
3. avoid replacing working configuration unnecessarily.

After editing:
1. run the build/tests;
2. manually verify the live preview, that the menu is reachable and all three games are playable
   from it, and that Escape returns to the menu from any game, on the primary development machine;
3. do not start a new milestone automatically.
