"""Fine-tune yolo26n.pt on the 11-class VisionArcade subset (Milestone 8)."""
from pathlib import Path
from ultralytics import YOLO

DATA_YAML = Path(__file__).resolve().parent.parent / "data" / "data.yaml"

if __name__ == "__main__":
    model = YOLO("yolo26n.pt")
    model.train(
        data=str(DATA_YAML),
        epochs=40,
        patience=10,
        imgsz=640,
        device="mps",
        project=str(Path(__file__).resolve().parent.parent / "runs"),
        name="finetune11",
        exist_ok=True,
    )
