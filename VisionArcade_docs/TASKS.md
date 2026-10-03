# Current Tasks

Current milestone: **Milestone 0 — Project and technical validation**

This file should describe the work that is actually next, not the entire product backlog.

## TODO

- [x] Switch/configure project toolchain to **JDK 25 LTS**. (Gradle toolchain `languageVersion = 25`, auto-provisioned via `foojay-resolver-convention` 1.0.0 → Temurin 25.0.4.1 aarch64.)
- [x] Ensure Gradle wrapper is on a Java-25-compatible stable Gradle 9.x release. (Gradle 9.8.0.)
- [x] Create the minimal JavaFX application: one window titled `VisionArcade`. (`org.example.visionarcade.app.VisionArcadeApp`, JavaFX 25.0.4, `javafx.controls` module only.)
- [x] Configure JUnit 6 and add one tiny passing test. (`junit-bom:6.1.3`; `OpenCvNativeLoadingTest` doubles as the native-loading check.)
- [x] Add a minimal OpenCV dependency/native-loading spike appropriate for macOS Apple Silicon. (`org.bytedeco:opencv-platform:4.14.0-1.5.14` — bundles native libs per-platform including macosx-arm64, no manual setup.)
- [x] Verify the app can request camera permission and capture at least one frame. (`./gradlew test -Dvisionarcade.hardwareTests=true --tests CameraCaptureSpikeTest` passes — camera opened, frame grabbed and non-empty. Note: camera permission on this machine was already granted to the terminal process; no new OS dialog appeared.)
- [x] Verify camera/native resources are released after the spike exits. (`camera.release()` in the test's `finally` block; test passes cleanly.)
- [x] Confirm `./gradlew build`, `./gradlew test`, and `./gradlew run` work. (All three ran successfully; `run` opens the JavaFX window and exits cleanly on kill.)
- [x] Update this file with the actual dependency versions selected. (Java 25 LTS toolchain via foojay 1.0.0 → Temurin 25.0.4.1; Gradle 9.8.0; JavaFX 25.0.4 via `org.openjfx.javafxplugin` 0.1.0; OpenCV 4.14.0-1.5.14 via bytedeco `opencv-platform`; JUnit 6.1.3.)

## Definition of done

Milestone 0 is done only when:

- the project builds from the command line using the Gradle wrapper;
- tests pass;
- JavaFX launches;
- OpenCV loads without manual IDE-only configuration;
- the webcam can capture a frame on the primary development machine;
- the process exits cleanly.

## Do not implement yet

- ONNX Runtime
- YOLO
- object detection
- bounding boxes
- game engine
- Object Hunt
- tracking
- persistence
- Spring Boot
- Docker
- Python/PyTorch environment

## Notes for Codex

Before editing:
1. inspect the existing project;
2. preserve useful generated Gradle files;
3. avoid replacing working configuration unnecessarily.

After editing:
1. run the build/tests;
2. report any manual macOS camera-permission step;
3. do not start Milestone 1 automatically.
