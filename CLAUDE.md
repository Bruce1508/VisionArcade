# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project state

VisionArcade is a brand-new Java/Gradle project. `src/main/java/org/example/Main.java` is still the
unmodified IntelliJ "Hello and welcome!" template, and `build.gradle` only has the `java` plugin plus
JUnit — no JavaFX, OpenCV, ONNX Runtime, or `application` plugin/`mainClass` configured yet. The real
design lives entirely in `VisionArcade_docs/`; the codebase has not caught up to the docs.

Current milestone: **Milestone 0 — Project and technical validation** (see `VisionArcade_docs/TASKS.md`
for the live checklist — don't duplicate it here, it changes often).

## Documentation map

This repo is doc-driven. Read in this order before changing code (same order `VisionArcade_docs/AGENTS.md`
and `FILE_INDEX.md` specify):

1. `VisionArcade_docs/PRD.md` — product scope, MVP definition, non-goals
2. `VisionArcade_docs/ARCHITECTURE.md` — design, threading model, package shape
3. `VisionArcade_docs/ROADMAP.md` — sequential milestones (do not jump ahead)
4. `VisionArcade_docs/TASKS.md` — what's actually next, right now
5. `VisionArcade_docs/DEVELOPMENT.md` — toolchain/dependency versions and policy
6. `VisionArcade_docs/TESTING.md`, `VisionArcade_docs/BENCHMARKS.md`
7. `VisionArcade_docs/docs/ADR/` — accepted architectural decisions (0001 Java/UI baseline, 0002
   vision/inference boundaries, 0003 latest-frame backpressure)

## Commands

Always use the wrapper, never a globally installed Gradle:

```bash
./gradlew build
./gradlew test
```

Single test class: `./gradlew test --tests "org.example.SomeTest"`

`./gradlew run` is **not yet wired up** — there's no `application` plugin or `mainClass` set in
`build.gradle`. Don't assume it works until that's configured (Milestone 0 work).

## Working rules (from `VisionArcade_docs/AGENTS.md`)

- Implement only the current milestone in `TASKS.md`. Don't build ahead because it's technically possible.
- Baseline stack per `DEVELOPMENT.md`: Java 25 LTS, Gradle 9.x wrapper, JavaFX 25, OpenCV 4.14.x, ONNX
  Runtime Java (direct bindings, no DJL), JUnit 6. Pin versions; no dynamic versions.
- Keep Java as the production runtime. Python is reserved for future model training/eval/export only —
  never a Python inference server for the desktop app.
- Never queue unbounded camera frames — latest-frame/single-slot handoff only (ADR 0003). Real-time
  latency matters more than processing every frame.
- Prefer immutable value objects across thread boundaries.
- Keep JavaFX work on the JavaFX Application Thread; never block it with camera capture or ML inference.
- Keep computer-vision/model code behind narrow interfaces (e.g. `ObjectDetector`) so the model/runtime
  can change without touching game logic.
- Do not add Spring Boot, Docker, cloud infra, user accounts, multiplayer, a database, TensorFlow, or DJL
  without a concrete current requirement.
- Do not commit large model binaries; `models/*.onnx` is meant to be gitignored.
- Avoid interfaces with only one implementation unless they isolate a volatile boundary (camera, detector,
  clock, persistence). Prefer composition over inheritance. No generic `Utils` dumping ground.
- Record a new ADR only for a meaningful, hard-to-reverse technical decision — not routine implementation.

## Planned architecture (not yet implemented)

Target data flow once Milestones 0-3 land (`ARCHITECTURE.md` §2):

```
Webcam -> CameraCapture -> FrameSlot (latest-frame only) -> ObjectDetector (ONNX Runtime)
       -> DetectionState (immutable snapshot) -> GameEngine -> JavaFX Renderer
```

Three execution contexts only for the MVP — do not add a separate game thread until profiling proves it
necessary:
- **Camera worker**: opens camera, captures/timestamps frames, publishes latest safe frame, releases on shutdown.
- **Inference worker**: reads latest frame, preprocesses, runs ONNX inference, publishes latest immutable
  detection snapshot. Drops stale frames if inference is slower than capture.
- **JavaFX Application Thread**: UI, game update/timing (`AnimationTimer`), rendering, reads latest detection
  snapshot.

Planned package shape (`ARCHITECTURE.md` §6) — don't add `service`/`manager`/`repository`/`domain`/
`infrastructure` layers without a real need:

```
...visionarcade
├── app     — application lifecycle/bootstrap
├── camera  — camera capture + frame ownership
├── vision  — preprocessing, detector, detections, postprocessing
├── game    — game state + Object Hunt rules
└── ui      — JavaFX rendering/controllers
```

Native resource ownership rules (`ARCHITECTURE.md` §9): the component that creates a native resource owns
its release unless ownership is explicitly transferred; no multi-threaded mutation of the same native image
object; stale frames replaced in the latest-frame slot must become eligible for cleanup.

## Before declaring a task complete

- Build the project and run automated tests.
- Manually verify milestone behavior when hardware/UI is involved (camera, JavaFX window).
- Update `VisionArcade_docs/TASKS.md` to reflect reality.
- Update architecture docs only if the implementation actually changed.
