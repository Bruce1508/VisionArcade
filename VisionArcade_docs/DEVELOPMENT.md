# VisionArcade Development Guide

Last reviewed: 2026-10-03

## Baseline environment

Recommended for this new project:

- **JDK:** 25 LTS
- **Gradle:** wrapper on current stable 9.x; 9.8.0 is current at review time
- **JavaFX:** 25.0.3 stable line
- **JUnit:** 6.0.3 or newer stable 6.0.x when the project is initialized
- **OpenCV:** 4.14.x
- **ONNX Runtime Java:** 1.30.0 (pinned in Milestone 2; latest stable per Maven Central metadata, verified on macOS arm64)
- **Model:** YOLO26n exported to ONNX (pinned in Milestone 2; see "Pinned model" below)
- **Primary dev machine:** macOS Apple Silicon / aarch64

Use stable releases, not EA/RC/nightly builds.

## Why Java 25 instead of Java 21

Java 25 is the current LTS line in late 2026. This is a fresh project, so starting on the current LTS avoids immediately creating an upgrade task.

If a native dependency proves incompatible with Java 25 during Milestone 0, document the evidence before temporarily falling back.

## Build policy

Always use the Gradle wrapper:

```bash
./gradlew build
./gradlew test
./gradlew run
```

Do not depend on a globally installed Gradle version.

## Dependency policy

- Pin dependency versions.
- Prefer Maven Central / official distributions.
- Avoid dynamic versions such as `1.+`.
- Avoid adding two libraries that solve the same problem.
- Do not upgrade dependencies during unrelated feature work.
- Record meaningful runtime/model changes in an ADR.

## OpenCV native strategy

The first technical spike must validate native loading on Apple Silicon.

Current options include:
- packaged OpenCV/JavaCPP artifacts from Maven Central;
- standard OpenCV Java bindings plus native library setup.

Choose the approach with the least manual machine-specific setup while keeping camera code behind a narrow boundary.

OpenCV 5.0.0 was released in 2026, but this project starts on the mature **4.14.x** line because it is current and reduces major-version integration risk.

## ONNX Runtime

Use direct ONNX Runtime Java APIs first.

Do not add DJL simply to wrap ONNX Runtime. Re-evaluate DJL only if the project later needs multiple inference engines or its higher-level model tooling provides a concrete benefit.

During Milestone 2:
1. verify the current stable Maven artifact;
2. verify native loading on Apple Silicon;
3. record the pinned version;
4. add a model-loading test or integration smoke test.

## Model files

Do not commit large model binaries by default.

Suggested local path:

```text
models/
  detector.onnx
```

`models/*.onnx` should be ignored until a deliberate distribution/licensing decision is made.

Model metadata that should be recorded:
- model family/version
- source
- license
- input dimensions
- class list/dataset
- export command/options
- SHA-256 checksum when practical

### Pinned model (Milestone 2)

- **Family/version:** YOLO26n (Ultralytics 8.4.172)
- **Source:** auto-downloaded by the `ultralytics` PyPI package (`yolo26n.pt`, from Ultralytics' own
  release assets), then exported locally — not committed anywhere upstream as an .onnx
- **License:** AGPL-3.0 (Ultralytics) — fine for local MVP development; revisit before any distribution
- **Input:** `images`, float32, `[1, 3, 640, 640]`, values in `0..1`
- **Output:** `output0`, float32, `[1, 84, 8400]` — rows 0-3 are box `cx,cy,w,h` in 640-space pixels,
  rows 4-83 are per-class sigmoid scores (80 COCO classes, no separate objectness score)
- **Class list/dataset:** COCO 80 classes (standard Ultralytics COCO class order)
- **Export command:** `yolo export model=yolo26n.pt format=onnx imgsz=640 opset=18` (run once in a
  throwaway local venv with `ultralytics`, `onnx`, `onnxruntime` installed — this is dev-time tooling
  per the Python policy above, not a project dependency)
- **SHA-256:** `ce9e45055cc002ee8b431d4b1f055d96867f4199a05d024ee287755042c1118f`
- **Local path:** `models/yolo26n.onnx` (gitignored; re-run the export command above to regenerate it)

## Python/ML workspace

Do not create the Python training stack until custom model work begins.

When needed:

```text
ml/
├── README.md
├── pyproject.toml
├── training/
├── evaluation/
└── export/
```

Use Python for:
- PyTorch experimentation
- fine-tuning
- evaluation
- ONNX export

Do not run a Python inference server for the desktop MVP.

## Logging

Start with JDK logging or a single lightweight logging library only if needed.

Do not add a logging facade + multiple implementations without a reason.

Useful runtime logs:
- camera opened/closed
- selected camera/device
- model loaded
- model input/output shape
- inference initialization failure
- worker shutdown
- unexpected dropped/resource errors

Avoid logging every frame.

## Formatting/style

- 4-space Java indentation
- one public top-level class per file
- prefer `record` for immutable value carriers where appropriate
- `final` where it clarifies ownership, not mechanically everywhere
- small methods with domain names
- no wildcard imports
- no generic `Utils` dumping ground

## Version references checked during planning

- Oracle Java SE roadmap: https://www.oracle.com/java/technologies/java-se-support-roadmap.html
- Gradle releases: https://gradle.org/releases/
- Gradle Java compatibility: https://docs.gradle.org/current/userguide/compatibility.html
- JavaFX roadmap/releases: https://openjfx.io/ and Maven Central
- OpenCV releases: https://opencv.org/releases/
- ONNX Runtime Java: https://onnxruntime.ai/docs/get-started/with-java.html
- Ultralytics model/export docs: https://docs.ultralytics.com/models/ and https://docs.ultralytics.com/modes/export/
- JUnit: https://docs.junit.org/
