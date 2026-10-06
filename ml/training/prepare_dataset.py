"""Build a small 11-class COCO subset (images + YOLO labels) for fine-tuning yolo26n.

Usage: python prepare_dataset.py
Reads ml/data/annotations/instances_{train,val}2017.json (already downloaded).
Writes ml/data/{train,val}/images/*.jpg, ml/data/{train,val}/labels/*.txt, ml/data/data.yaml.
"""
import json
import random
import concurrent.futures as cf
from pathlib import Path
from urllib.request import urlretrieve

CLASSES = ["cup", "bottle", "cell phone", "book", "scissors", "clock",
           "backpack", "mouse", "keyboard", "remote", "person"]
CLASS_INDEX = {name: i for i, name in enumerate(CLASSES)}

DATA_DIR = Path(__file__).resolve().parent.parent / "data"
PER_CLASS_CAP = {"train": 300, "val": 60}
random.seed(0)


def build_split(split: str, ann_file: Path):
    coco = json.loads(ann_file.read_text())
    name_to_catid = {c["name"]: c["id"] for c in coco["categories"] if c["name"] in CLASS_INDEX}
    missing = set(CLASS_INDEX) - set(name_to_catid)
    if missing:
        raise RuntimeError(f"classes not found in COCO categories: {missing}")
    catid_to_index = {name_to_catid[name]: idx for name, idx in CLASS_INDEX.items()}

    images_by_id = {img["id"]: img for img in coco["images"]}
    anns_by_image: dict[int, list] = {}
    for ann in coco["annotations"]:
        if ann["category_id"] in catid_to_index:
            anns_by_image.setdefault(ann["image_id"], []).append(ann)

    # Cap per class: walk classes, pick up to PER_CLASS_CAP[split] images per class,
    # allowing overlap (an image with multiple target classes counts toward each).
    cap = PER_CLASS_CAP[split]
    chosen_image_ids: set[int] = set()
    per_class_count = {idx: 0 for idx in catid_to_index.values()}
    image_ids_by_class: dict[int, list[int]] = {idx: [] for idx in catid_to_index.values()}
    for image_id, anns in anns_by_image.items():
        present = {catid_to_index[a["category_id"]] for a in anns}
        for idx in present:
            image_ids_by_class[idx].append(image_id)

    for idx, ids in image_ids_by_class.items():
        random.shuffle(ids)
        for image_id in ids:
            if per_class_count[idx] >= cap:
                break
            chosen_image_ids.add(image_id)
            per_class_count[idx] += 1

    img_dir = DATA_DIR / split / "images"
    lbl_dir = DATA_DIR / split / "labels"
    img_dir.mkdir(parents=True, exist_ok=True)
    lbl_dir.mkdir(parents=True, exist_ok=True)

    def download_one(image_id: int):
        info = images_by_id[image_id]
        fname = info["file_name"]
        dest = img_dir / fname
        if not dest.exists():
            urlretrieve(info["coco_url"], dest)
        w, h = info["width"], info["height"]
        lines = []
        for ann in anns_by_image[image_id]:
            idx = catid_to_index[ann["category_id"]]
            x, y, bw, bh = ann["bbox"]  # COCO: top-left x,y,w,h in pixels
            cx, cy = (x + bw / 2) / w, (y + bh / 2) / h
            nw, nh = bw / w, bh / h
            lines.append(f"{idx} {cx:.6f} {cy:.6f} {nw:.6f} {nh:.6f}")
        (lbl_dir / (Path(fname).stem + ".txt")).write_text("\n".join(lines))

    ids = list(chosen_image_ids)
    print(f"[{split}] downloading {len(ids)} images (per-class counts: {per_class_count})")
    with cf.ThreadPoolExecutor(max_workers=16) as pool:
        list(pool.map(download_one, ids))
    print(f"[{split}] done: {len(ids)} images")


def main():
    build_split("train", DATA_DIR / "annotations" / "instances_train2017.json")
    build_split("val", DATA_DIR / "annotations" / "instances_val2017.json")

    yaml_text = f"""path: {DATA_DIR}
train: train/images
val: val/images
names:
{chr(10).join(f'  {i}: {name}' for i, name in enumerate(CLASSES))}
"""
    (DATA_DIR / "data.yaml").write_text(yaml_text)
    print("wrote", DATA_DIR / "data.yaml")


if __name__ == "__main__":
    main()
