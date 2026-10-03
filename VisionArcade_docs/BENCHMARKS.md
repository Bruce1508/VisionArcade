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
| Camera FPS | TBD |
| Inference FPS | TBD |
| Mean inference latency | TBD |
| p95 inference latency | TBD |
| Approx. end-to-end latency | TBD |
| CPU usage | TBD |
| Peak memory | TBD |

## Benchmark rules

- warm up the JVM/model before measuring;
- record sample count;
- avoid comparing runs with different model input sizes without saying so;
- do not claim accuracy from casual visual testing;
- keep raw benchmark methodology reproducible;
- never put invented numbers on a resume.
