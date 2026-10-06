# Current Tasks

Current milestone: **Milestone 8 — Custom fine-tune: app's actual class set**

Milestones 0-7 (toolchain/JavaFX/OpenCV/camera validation, live webcam preview, ONNX detection
spike, real-time visualization, Object Hunt MVP, tracking/smoothing, Vision Pong, quality/
portfolio polish) are complete — history preserved in git log.

This file should describe the work that is actually next, not the entire product backlog.

## TODO

- [x] Build an 11-class COCO subset dataset. `ml/training/prepare_dataset.py` — 3184 train / 546
  val images for `cup, bottle, cell phone, book, scissors, clock, backpack, mouse, keyboard,
  remote, person`. Not full COCO (19GB); annotations-only download (~241MB) filtered/capped
  per-class, images downloaded individually.
- [x] Fine-tune YOLO26n on the subset, starting from its COCO-pretrained weights, not from
  scratch. `ml/training/finetune.py`, 40 epochs (patience=10 never triggered — kept improving
  through epoch 38), MPS device on Apple M4 Pro, 1.448 hours. mAP50 0.438 / mAP50-95 0.287 on the
  held-out val split — see `DEVELOPMENT.md`'s "Fine-tuned model (Milestone 8)" and
  `BENCHMARKS.md`'s per-class table.
- [x] Export to ONNX. `models/yolo26n-finetune11.onnx`, output shape `[1, 15, 8400]` (confirmed
  vs. the stock model's `[1, 84, 8400]`).
- [x] Extend `Yolo26nObjectDetector` to accept a class list, not just the hardcoded 80-class
  array — backward compatible, both existing call sites (`VisionArcadeApp`, the integration test)
  unchanged.
- [x] Benchmark stock vs. fine-tuned inference latency on the same fixed image, same machine.
  `Yolo26nBenchmarkComparisonTest` (gated on both model files existing) — fills in `BENCHMARKS.md`'s
  Benchmark B. **Finding: no meaningful speed difference** (mean 12.45ms stock vs. 12.91ms
  fine-tuned) — the output head shrinking from 84 to 15 channels is a small fraction of total
  compute; the backbone dominates runtime regardless of class count. The hypothesis that fewer
  classes = faster inference did not hold up.
- [x] Record the fine-tuned model's held-out validation mAP. Done above — no comparable "before"
  accuracy number was produced (would need a second label set in COCO's 80-class index space to
  fairly score the stock model; not attempted, noted honestly in `BENCHMARKS.md` instead of
  fabricating a comparison).

## Do not implement yet

- swapping the live app (`VisionArcadeApp`) over to the fine-tuned model — ROADMAP.md Milestone 8
  explicitly says not to, without the user confirming after seeing the numbers above (speed is a
  wash; the user hasn't yet decided if the accuracy tradeoff/narrower class set is worth it)
- a COCO-80-index-space re-evaluation to get a fair stock-model accuracy baseline — not attempted
  this round; would be the natural next step if the user wants a real accuracy before/after
- persistence, pose detection, more games — still only to be *considered* per ROADMAP.md, not
  built
- a mode selector between Object Hunt and Vision Pong — unchanged from Milestone 6

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
