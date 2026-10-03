# VisionArcade Agent Guide

This repository is intentionally milestone-driven. Do not expand scope just because a feature is technically possible.

## Read before changing code
1. `PRD.md`
2. `ARCHITECTURE.md`
3. `ROADMAP.md`
4. `TASKS.md`
5. `DEVELOPMENT.md`

## Working rules
- Implement only the current milestone in `TASKS.md`.
- Prefer the smallest design that keeps the next 1-2 milestones possible.
- Do not add frameworks, services, persistence layers, or abstractions without a current requirement.
- Keep Java as the production runtime.
- Python is reserved for future model training/evaluation/export work.
- Keep computer-vision/model code behind narrow interfaces so the model/runtime can change later.
- Never queue unbounded camera frames. Real-time latency matters more than processing every frame.
- Prefer immutable value objects across thread boundaries.
- Keep JavaFX work on the JavaFX Application Thread.
- Do not block the JavaFX Application Thread with camera capture or ML inference.
- Do not add Spring Boot, Docker, cloud infrastructure, user accounts, multiplayer, or a database during the MVP.
- Do not add TensorFlow, DJL, or another ML framework unless a concrete requirement justifies it.
- Do not commit large model binaries unless the repository explicitly changes that policy.

## Before declaring a task complete
- Build the project.
- Run automated tests.
- Manually verify the milestone behavior when hardware/UI is involved.
- Update `TASKS.md` to reflect reality.
- Update architecture/docs only if the implementation actually changed.
- Record a new ADR only for a meaningful, hard-to-reverse technical decision.

## Code quality
- Favor clear names over comments that restate code.
- Prefer composition over inheritance.
- Avoid premature design patterns.
- Avoid interfaces with only one implementation unless they isolate a volatile boundary (camera, detector, clock, persistence).
- Keep classes focused; split only when responsibilities genuinely diverge.
- Measure before optimizing.
