# VisionArcade Benchmarks

No benchmark numbers belong here until they are measured on a real build.

## Test environment

Record for every benchmark set:

- date
- git commit
- OS/version
- CPU / Apple Silicon model
- RAM
- JDK version
- OpenCV version
- ONNX Runtime version
- model name/version
- model input size
- execution provider
- camera resolution / requested FPS

### 2026-10-04 (Benchmark C only)

- date: 2026-10-04
- git commit: `883ade2` (M7 working tree, not yet committed at measurement time)
- OS/version: macOS 26.6.2
- CPU: Apple M4 Pro
- RAM: 24 GB
- JDK version: 25 LTS (Gradle toolchain-provisioned, per `DEVELOPMENT.md`)
- OpenCV version: 4.14.0 (bytedeco `opencv-platform:4.14.0-1.5.14`)
- ONNX Runtime version: 1.30.0
- model name/version: YOLO26n, ONNX export (see `DEVELOPMENT.md` "Pinned model")
- model input size: 640x640
- execution provider: CPU (default `OrtSession.SessionOptions`, no GPU EP configured)
- camera resolution / requested FPS: not explicitly requested by the app (`OpenCvCameraSource`
  doesn't set `CAP_PROP_FRAME_WIDTH/HEIGHT/FPS`) — whatever the device's own default is

## Benchmark A — Camera only

| Metric | Value |
|---|---:|
| Capture FPS | TBD |
| Display FPS | TBD |
| CPU usage | TBD |
| Memory after 1 min | TBD |
| Memory after 10 min | TBD |

## Benchmark B — Detector on fixed images

Use a repeatable local image set.

| Metric | Value |
|---|---:|
| Warm-up runs | TBD |
| Measured runs | TBD |
| Mean latency | TBD |
| p50 latency | TBD |
| p95 latency | TBD |
| p99 latency | TBD |

## Benchmark C — Live end-to-end

Define end-to-end latency as clearly as possible, for example:

`frame capture timestamp -> detections available/rendered`

| Metric | Value |
|---|---:|
| Render FPS (HUD "render" counter) | 28-30 fps |
| Inference FPS | TBD — not directly counted; inference is faster than capture so it isn't the bottleneck |
| Mean inference latency | ~15 ms (single stabilized HUD reading, not an averaged sample set) |
| p95 inference latency | TBD — HUD shows an instantaneous reading, not a percentile |
| Approx. end-to-end latency | TBD — not instrumented |
| CPU usage | ~30% (Activity Monitor, `java` process) |
| Peak memory | TBD |

Measured 2026-10-04 by watching the live HUD line (`render: N fps   inference: X.X ms`) for
30-60s with a person continuously in frame, plus Activity Monitor for CPU — see "Test
environment" above for the exact build/hardware. One reading, not a multi-run statistical sample;
re-measure with actual logging before trusting percentiles.

## Benchmark rules

- warm up the JVM/model before measuring;
- record sample count;
- avoid comparing runs with different model input sizes without saying so;
- do not claim accuracy from casual visual testing;
- keep raw benchmark methodology reproducible;
- never put invented numbers on a resume.
