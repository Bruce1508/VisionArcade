# ml/ — training/eval/export workspace (Milestone 8)

Python, PyTorch, and Ultralytics, used only for training/evaluation/export — never a Python
inference server for the desktop app (see repo `DEVELOPMENT.md`). The Java app only ever loads
the resulting `.onnx` file from `models/`.

## Setup

```bash
cd ml
python3.13 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

## Pipeline (Milestone 8: fine-tune on VisionArcade's actual 11 classes)

1. Download COCO 2017 annotations only (not the 19GB image set) into `ml/data/annotations/`:
   `http://images.cocodataset.org/annotations/annotations_trainval2017.zip`
2. `python training/prepare_dataset.py` — filters to the 11 classes the app uses (`cup, bottle,
   cell phone, book, scissors, clock, backpack, mouse, keyboard, remote, person`), caps per-class
   image counts, downloads only those images, writes YOLO-format labels + `ml/data/data.yaml`.
3. `python training/finetune.py` — fine-tunes `yolo26n.pt` (COCO-pretrained) on the subset.
4. `python export/export_onnx.py` — exports the best checkpoint to
   `models/yolo26n-finetune11.onnx`.

See `VisionArcade_docs/DEVELOPMENT.md`'s "Fine-tuned model (Milestone 8)" section for the exact
dataset size, hyperparameters, and measured accuracy from the run that produced the committed
model, and `VisionArcade_docs/BENCHMARKS.md` for the before/after speed comparison.

`ml/data/` and `ml/runs/` are large and gitignored — regenerate by re-running the pipeline above.
