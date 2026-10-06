"""Export the Milestone 8 fine-tuned checkpoint to ONNX, same pattern as the pinned model."""
from pathlib import Path
from ultralytics import YOLO

BEST_PT = Path(__file__).resolve().parent.parent / "runs" / "finetune11" / "weights" / "best.pt"
DEST = Path(__file__).resolve().parent.parent.parent / "models" / "yolo26n-finetune11.onnx"

if __name__ == "__main__":
    model = YOLO(str(BEST_PT))
    exported = model.export(format="onnx", imgsz=640, opset=18)
    Path(exported).rename(DEST)
    print("exported to", DEST)
