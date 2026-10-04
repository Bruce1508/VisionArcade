# Current Tasks

Current milestone: **Milestone 6 — Second game: Vision Pong**

Milestone 0 (toolchain/JavaFX/OpenCV/camera validation), Milestone 1 (live webcam preview),
Milestone 2 (ONNX detection spike), Milestone 3 (real-time visualization), Milestone 4 (Object
Hunt MVP), and Milestone 5 (tracking and smoothing) are complete — history preserved in git log.

This file should describe the work that is actually next, not the entire product backlog.

## TODO

- [x] Choose one detected class/controller. (`game.PongEngine`: `"person"` — the one class
  guaranteed to be continuously visible, so the player's own body position drives the paddle.)
- [x] Map object position to paddle position. (Controller box's vertical center, normalized by
  frame height, becomes the paddle's target position in a [0,1] field.)
- [x] Clamp/smooth control. (Target position clamped to [0,1]; paddle eases toward it each tick
  via `PADDLE_SMOOTHING_ALPHA = 0.25`; holds its last position for ticks where the controller
  isn't detected, rather than snapping to center.)
- [x] Simple Pong physics. (Ball bounces off top/bottom/left walls and the paddle on the right
  wall; constant velocity, no difficulty ramp — kept to the roadmap's "simple" scope.)
- [x] Score/restart. (Score increments per successful paddle return; a miss shows a result message
  for 1.5s then restarts, same round/result/restart shape as `GameEngine`.)

## Definition of done

Milestone 6 is done:

- [x] `PongEngine` is unit tested without a camera or model (`PongEngineTest`: initial state, paddle
  tracks the controller, paddle holds position when the controller disappears, paddle position
  stays in [0,1], ball stays in the field while playing, and a round ends in a miss and restarts).
- [x] the live app runs `PongEngine` (not `GameEngine`) with no startup/runtime exceptions —
  confirmed by the user running `./gradlew run`.
- [x] physical movement (standing/crouching in front of the camera) feels predictable and playable —
  confirmed by the user live ("có vẻ mọi thứ work tốt").

## Do not implement yet

- a mode selector between Object Hunt and Vision Pong — `GameEngine`/`GameState`/
  `CameraPreviewView.showGame()` are untouched and still tested, but the live app currently runs
  exactly one game (`PongEngine`) at a time, same as every milestone before it; add a selector only
  if actually asked for
- ball speed-up/difficulty ramp, two-player paddle, AI opponent — none of these are in the
  roadmap's "simple Pong physics" scope
- persistence, additional game modes, Spring Boot, Docker (unchanged from Milestone 4)

## Notes for Codex

Before editing:
1. inspect the existing project;
2. preserve the Milestone 0–5 toolchain/JavaFX/OpenCV/camera/vision/game wiring;
3. avoid replacing working configuration unnecessarily.

After editing:
1. run the build/tests;
2. manually verify the live preview, detection overlay, **and that standing/crouching in front of
   the camera moves the paddle predictably and the ball/score/miss/restart cycle works** on the
   primary development machine;
3. do not start a new milestone automatically.
