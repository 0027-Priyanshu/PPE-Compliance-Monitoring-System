import os
import sys
import cv2
from ultralytics import YOLO

BASE_DIR = os.path.dirname(os.path.abspath(__file__))

MODEL_PATH = os.path.join(BASE_DIR, "models", "ppe_best.pt")
OUTPUT_DIR = os.path.join(BASE_DIR, "outputs")

os.makedirs(OUTPUT_DIR, exist_ok=True)

model = YOLO(MODEL_PATH)


def box_center(box):
    x1, y1, x2, y2 = box
    return ((x1 + x2) / 2, (y1 + y2) / 2)


def is_inside_region(point, region):
    px, py = point
    x1, y1, x2, y2 = region
    return x1 <= px <= x2 and y1 <= py <= y2


def draw_summary_panel(img, person_found, helmet, vest, status):
    overlay = img.copy()
    cv2.rectangle(overlay, (10, 10), (320, 140), (0, 0, 0), -1)
    cv2.addWeighted(overlay, 0.45, img, 0.55, 0, img)

    def get_color(flag):
        return (0, 255, 0) if flag else (0, 0, 255)

    if status == "SAFE":
        result_color = (0, 255, 0)
    elif status == "WARNING":
        result_color = (0, 165, 255)
    elif status == "UNSAFE":
        result_color = (0, 0, 255)
    else:
        result_color = (255, 255, 255)

    cv2.putText(img, f"Person: {'Detected' if person_found else 'Not Found'}",
                (20, 35), cv2.FONT_HERSHEY_SIMPLEX, 0.65, get_color(person_found), 2)

    cv2.putText(img, f"Helmet: {'Present' if helmet else 'Missing'}",
                (20, 70), cv2.FONT_HERSHEY_SIMPLEX, 0.65, get_color(helmet), 2)

    cv2.putText(img, f"Vest: {'Present' if vest else 'Missing'}",
                (20, 100), cv2.FONT_HERSHEY_SIMPLEX, 0.65, get_color(vest), 2)

    cv2.putText(img, f"Result: {status}",
                (20, 130), cv2.FONT_HERSHEY_SIMPLEX, 0.72, result_color, 2)


def detect_ppe(image_path):
    img = cv2.imread(image_path)

    if img is None:
        print("RESULT:ERROR")
        return

    results = model(img)

    person_boxes = []
    helmet_boxes = []
    vest_boxes = []

    # collect detections
    for r in results:
        for box in r.boxes:
            label = model.names[int(box.cls)].lower().strip()
            x1, y1, x2, y2 = map(int, box.xyxy[0])

            print("Detected:", label)

            if label in ["person", "worker", "man", "woman"]:
                person_boxes.append((x1, y1, x2, y2))

            elif label in ["helmet", "hardhat"]:
                helmet_boxes.append((x1, y1, x2, y2))

            elif label in ["vest", "safety vest", "safety_vest"]:
                vest_boxes.append((x1, y1, x2, y2))

    if not person_boxes:
        # if no person at all, still save image with summary
        status = "NO PERSON"
        draw_summary_panel(img, False, False, False, status)

        image_name = os.path.basename(image_path)
        name, ext = os.path.splitext(image_name)
        output_path = os.path.join(OUTPUT_DIR, f"{name}_result.jpg")
        cv2.imwrite(output_path, img)

        print(f"RESULT:{status}")
        print(f"OUTPUT:{output_path}")
        return

    # for simplicity, check the largest person only
    person_boxes.sort(key=lambda b: (b[2] - b[0]) * (b[3] - b[1]), reverse=True)
    person_box = person_boxes[0]
    px1, py1, px2, py2 = person_box
    pw = px2 - px1
    ph = py2 - py1

    # define logical regions inside person box
    head_region = (
        px1 + int(0.15 * pw),
        py1,
        px2 - int(0.15 * pw),
        py1 + int(0.28 * ph)
    )

    torso_region = (
        px1 + int(0.10 * pw),
        py1 + int(0.28 * ph),
        px2 - int(0.10 * pw),
        py1 + int(0.72 * ph)
    )

    helmet = False
    vest = False

    valid_helmet_box = None
    valid_vest_box = None

    # helmet is valid only if its center lies in head region
    for hb in helmet_boxes:
        hc = box_center(hb)
        if is_inside_region(hc, head_region):
            helmet = True
            valid_helmet_box = hb
            break

    # vest is valid only if its center lies in torso region
    for vb in vest_boxes:
        vc = box_center(vb)
        if is_inside_region(vc, torso_region):
            vest = True
            valid_vest_box = vb
            break

    # result logic
    if helmet and vest:
        status = "SAFE"
    elif helmet or vest:
        status = "WARNING"
    else:
        status = "UNSAFE"

    # draw largest person
    cv2.rectangle(img, (px1, py1), (px2, py2), (255, 0, 0), 2)
    cv2.putText(img, "person", (px1, max(py1 - 10, 20)),
                cv2.FONT_HERSHEY_SIMPLEX, 0.6, (255, 0, 0), 2)

    # draw head/torso guide regions
    hx1, hy1, hx2, hy2 = head_region
    tx1, ty1, tx2, ty2 = torso_region

    cv2.rectangle(img, (hx1, hy1), (hx2, hy2), (0, 255, 255), 1)
    cv2.putText(img, "head region", (hx1, max(hy1 - 5, 20)),
                cv2.FONT_HERSHEY_SIMPLEX, 0.45, (0, 255, 255), 1)

    cv2.rectangle(img, (tx1, ty1), (tx2, ty2), (255, 255, 0), 1)
    cv2.putText(img, "torso region", (tx1, max(ty1 - 5, 20)),
                cv2.FONT_HERSHEY_SIMPLEX, 0.45, (255, 255, 0), 1)

    # draw only valid PPE boxes
    if valid_helmet_box:
        x1, y1, x2, y2 = valid_helmet_box
        cv2.rectangle(img, (x1, y1), (x2, y2), (0, 255, 0), 2)
        cv2.putText(img, "helmet", (x1, max(y1 - 10, 20)),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.6, (0, 255, 0), 2)

    if valid_vest_box:
        x1, y1, x2, y2 = valid_vest_box
        cv2.rectangle(img, (x1, y1), (x2, y2), (0, 165, 255), 2)
        cv2.putText(img, "vest", (x1, max(y1 - 10, 20)),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.6, (0, 165, 255), 2)

    draw_summary_panel(img, True, helmet, vest, status)

    image_name = os.path.basename(image_path)
    name, ext = os.path.splitext(image_name)
    output_path = os.path.join(OUTPUT_DIR, f"{name}_result.jpg")

    cv2.imwrite(output_path, img)

    print(f"RESULT:{status}")
    print(f"OUTPUT:{output_path}")


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("RESULT:ERROR")
    else:
        detect_ppe(sys.argv[1])