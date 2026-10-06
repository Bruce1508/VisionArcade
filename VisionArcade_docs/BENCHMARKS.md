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

## Benchmark B — Detector on fixed images (Milestone 8: stock vs. fine-tuned)

Same fixed test image (`src/test/resources/vision/bus.jpg`), same machine (see 2026-10-04 test
environment above, same commit lineage), run through `Yolo26nBenchmarkComparisonTest` — 10
warm-up runs discarded, then 50 measured runs per model, both on ONNX Runtime's CPU execution
provider.

| Metric | Stock (80-class `yolo26n.onnx`) | Fine-tuned (11-class `yolo26n-finetune11.onnx`) |
|---|---:|---:|
| Warm-up runs | 10 | 10 |
| Measured runs | 50 | 50 |
| Mean latency | 12.45 ms | 12.91 ms |
| p50 latency | 12.30 ms | 12.09 ms |
| p95 latency | 13.96 ms | 16.41 ms |
| p99 latency | 15.80 ms | 34.12 ms |

**Finding: fewer classes did not make inference meaningfully faster.** The hypothesis going in
was that dropping from 80 to 11 classes would speed up inference since the model has less to
classify. It doesn't, materially — mean/p50 are within noise of each other. The output tensor
shrinks from `[1, 84, 8400]` to `[1, 15, 8400]` (4 box coords + N classes, same 8400 anchors), but
that's a tiny fraction of the model's total compute; YOLO26n's backbone and feature-extraction
layers process the same 640x640 input regardless of how many classes the head predicts, and the
backbone dominates runtime. The fine-tuned model's single run of the 50 showed a p99 latency
spike (34ms) not seen in the stock model's p99 (15.8ms) — plausibly OS/thermal scheduling noise on
one run rather than a model-architecture effect, since mean/p50 don't show the same gap; not
re-run multiple sessions to confirm, so treat the p99 figure as noisy.

This benchmark does **not** include a before/after accuracy comparison. The fine-tuned model's
own measured mAP is below, but there's no fair "before" number to put next to it here: the stock
model's head outputs 80 classes in COCO's own order, and evaluating it fairly against the same 11
classes would need a second label set built in COCO's 80-class index space (not attempted this
round) — scoring the stock model against only-11-classes-labeled ground truth would be comparing
different things, not a real baseline.

### Milestone 8 fine-tuned model — measured accuracy

From Ultralytics' own validation of `best.pt` against the held-out val split (546 images, 3196
instances across the 11 classes) after training — not estimated:

| Class | Images | Instances | P | R | mAP50 | mAP50-95 |
|---|---:|---:|---:|---:|---:|---:|
| all | 546 | 3196 | 0.548 | 0.425 | 0.438 | 0.287 |
| cup | 119 | 278 | 0.475 | 0.335 | 0.341 | 0.232 |
| bottle | 109 | 293 | 0.537 | 0.437 | 0.449 | 0.280 |
| cell phone | 101 | 124 | 0.567 | 0.323 | 0.348 | 0.203 |
| book | 115 | 569 | 0.364 | 0.169 | 0.188 | 0.083 |
| scissors | 28 | 36 | 0.506 | 0.389 | 0.371 | 0.230 |
| clock | 81 | 108 | 0.665 | 0.528 | 0.534 | 0.373 |
| backpack | 76 | 117 | 0.296 | 0.154 | 0.152 | 0.071 |
| mouse | 75 | 91 | 0.759 | 0.703 | 0.701 | 0.516 |
| keyboard | 87 | 130 | 0.627 | 0.638 | 0.664 | 0.491 |
| remote | 77 | 155 | 0.497 | 0.323 | 0.327 | 0.186 |
| person | 324 | 1295 | 0.738 | 0.682 | 0.738 | 0.487 |

`book` and `backpack` are the weak classes (small/cluttered instances, per COCO's own known
difficulty for these categories); `mouse`, `keyboard`, and `person` are the strongest. See
`DEVELOPMENT.md`'s "Fine-tuned model (Milestone 8)" section for the dataset/training details
behind this run.

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
