"""Export the pretrained YOLO26n-pose checkpoint to ONNX (Milestone 9). No training — this is the
stock COCO-keypoint model, exported the same way the Milestone 2 detector spike was."""
from pathlib import Path
from ultralytics import YOLO

DEST = Path(__file__).resolve().parent.parent.parent / "models" / "yolo26n-pose.onnx"

if __name__ == "__main__":
    model = YOLO("yolo26n-pose.pt")  # auto-downloads from ultralytics/assets if not cached
    exported = model.export(format="onnx", imgsz=640, opset=18)
    Path(exported).rename(DEST)
    print("exported to", DEST)
