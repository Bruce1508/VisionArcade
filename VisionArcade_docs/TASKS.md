# Current Tasks

Current milestone: **Milestone 4 — Object Hunt MVP**

Milestone 0 (toolchain/JavaFX/OpenCV/camera validation), Milestone 1 (live webcam preview),
Milestone 2 (ONNX detection spike), and Milestone 3 (real-time visualization) are complete —
history preserved in git log.

This file should describe the work that is actually next, not the entire product backlog.

## TODO

- [x] Game engine. (`game.GameEngine`: pure state machine advanced by `tick(nowNanos, DetectionSnapshot)` —
  no camera/JavaFX dependency, unit tested with fake detections and an injected `Random`/clock per
  `TESTING.md`.)
- [x] Target pool. (10 household COCO classes, excluding "person": cup, bottle, cell phone, book,
  scissors, clock, backpack, mouse, keyboard, remote. A round never repeats the previous round's target.)
- [x] Confidence + stability gating. (Counts as "found" only at confidence ≥ 0.5, continuously for ≥ 600ms —
  avoids single-frame noise triggering a win.)
- [x] Round timer + auto-restart. (20s round timeout; on found/timeout shows the result for 1.5s, then
  starts a new round automatically — driven by the existing `AnimationTimer`'s `now`, no new thread.)
- [x] HUD. (`CameraPreviewView.showGame()` draws target/score/time-remaining on the existing overlay
  `Canvas`, plus a large transient "FOUND IT!"/"TIME'S UP!" message.)
- [x] Wiring. (`VisionArcadeApp` owns one `GameEngine`, ticks it once per render frame with the same
  `now` the render loop already has, right after `showDetections()`.)

## Definition of done

Milestone 4 is done only when:

- a round can be won by holding the target object up to the camera — **needs manual confirmation:
  run `./gradlew run`, show the named target object, and confirm "FOUND IT!" appears and score increments.**
- a round can time out and a new target is assigned automatically — **needs the same manual run.**
- the game never blocks or stutters the render loop — structurally guaranteed: `GameEngine.tick()` is a
  pure, allocation-light function with no I/O, called directly on the JavaFX Application Thread per frame.
- `./gradlew test` passes, including `GameEngineTest` (stability window, confidence gating, timeout,
  auto-restart, no-repeat-target invariant — all via fake clocks/detections, no camera/model needed).

## Do not implement yet

- object tracking across frames (e.g. re-identifying the same physical object)
- persistence (high scores, settings)
- additional game modes beyond Object Hunt
- Spring Boot
- Docker
- a settings/config screen for round duration, thresholds, or the target pool (fixed values are enough
  until there's a concrete reason to make them adjustable)

## Notes for Codex

Before editing:
1. inspect the existing project;
2. preserve the Milestone 0–3 toolchain/JavaFX/OpenCV/camera/vision wiring;
3. avoid replacing working configuration unnecessarily.

After editing:
1. run the build/tests;
2. manually verify the live preview, detection overlay, **and the Object Hunt HUD/round transitions**
   on the primary development machine;
3. do not start a new milestone automatically.
