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


def analyze_person_ppe(person_boxes, helmet_boxes, vest_boxes):
    if not person_boxes:
        return False, False, False, None, None, None

    # Select largest person
    person_boxes.sort(key=lambda b: (b[2] - b[0]) * (b[3] - b[1]), reverse=True)
    person_box = person_boxes[0]
    px1, py1, px2, py2 = person_box
    pw = px2 - px1
    ph = py2 - py1

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

    for hb in helmet_boxes:
        hc = box_center(hb)
        if is_inside_region(hc, head_region):
            helmet = True
            valid_helmet_box = hb
            break

    for vb in vest_boxes:
        vc = box_center(vb)
        if is_inside_region(vc, torso_region):
            vest = True
            valid_vest_box = vb
            break

    return True, helmet, vest, person_box, valid_helmet_box, valid_vest_box


def draw_summary(frame, person_found, helmet, vest, status):
    overlay = frame.copy()
    cv2.rectangle(overlay, (10, 10), (340, 140), (0, 0, 0), -1)
    cv2.addWeighted(overlay, 0.45, frame, 0.55, 0, frame)

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

    cv2.putText(frame, f"Person: {'Detected' if person_found else 'Not Found'}",
                (20, 35), cv2.FONT_HERSHEY_SIMPLEX, 0.65, get_color(person_found), 2)

    cv2.putText(frame, f"Helmet: {'Present' if helmet else 'Missing'}",
                (20, 70), cv2.FONT_HERSHEY_SIMPLEX, 0.65, get_color(helmet), 2)

    cv2.putText(frame, f"Vest: {'Present' if vest else 'Missing'}",
                (20, 100), cv2.FONT_HERSHEY_SIMPLEX, 0.65, get_color(vest), 2)

    cv2.putText(frame, f"Result: {status}",
                (20, 130), cv2.FONT_HERSHEY_SIMPLEX, 0.72, result_color, 2)


def detect_video(video_path):
    cap = cv2.VideoCapture(video_path)

    if not cap.isOpened():
        print("RESULT:ERROR")
        return

    width = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
    height = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
    fps = cap.get(cv2.CAP_PROP_FPS)

    if fps <= 0:
        fps = 25.0

    video_name = os.path.basename(video_path)
    name, ext = os.path.splitext(video_name)
    output_video_path = os.path.join(OUTPUT_DIR, f"{name}_result.mp4")

    fourcc = cv2.VideoWriter_fourcc(*"mp4v")
    out = cv2.VideoWriter(output_video_path, fourcc, fps, (width, height))

    safe_count = 0
    warning_count = 0
    unsafe_count = 0

    while True:
        ret, frame = cap.read()
        if not ret:
            break

        results = model(frame)

        person_boxes = []
        helmet_boxes = []
        vest_boxes = []

        for r in results:
            for box in r.boxes:
                label = model.names[int(box.cls)].lower().strip()
                x1, y1, x2, y2 = map(int, box.xyxy[0])

                if label in ["person", "worker", "man", "woman"]:
                    person_boxes.append((x1, y1, x2, y2))
                elif label in ["helmet", "hardhat"]:
                    helmet_boxes.append((x1, y1, x2, y2))
                elif label in ["vest", "safety vest", "safety_vest"]:
                    vest_boxes.append((x1, y1, x2, y2))

        person_found, helmet, vest, person_box, valid_helmet_box, valid_vest_box = analyze_person_ppe(
            person_boxes, helmet_boxes, vest_boxes
        )

        if not person_found:
            frame_status = "NO PERSON"
        elif helmet and vest:
            frame_status = "SAFE"
            safe_count += 1
        elif helmet or vest:
            frame_status = "WARNING"
            warning_count += 1
        else:
            frame_status = "UNSAFE"
            unsafe_count += 1

        if person_box:
            px1, py1, px2, py2 = person_box
            cv2.rectangle(frame, (px1, py1), (px2, py2), (255, 0, 0), 2)
            cv2.putText(frame, "person", (px1, max(py1 - 10, 20)),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.6, (255, 0, 0), 2)

        if valid_helmet_box:
            x1, y1, x2, y2 = valid_helmet_box
            cv2.rectangle(frame, (x1, y1), (x2, y2), (0, 255, 0), 2)
            cv2.putText(frame, "helmet", (x1, max(y1 - 10, 20)),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.6, (0, 255, 0), 2)

        if valid_vest_box:
            x1, y1, x2, y2 = valid_vest_box
            cv2.rectangle(frame, (x1, y1), (x2, y2), (0, 165, 255), 2)
            cv2.putText(frame, "vest", (x1, max(y1 - 10, 20)),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.6, (0, 165, 255), 2)

        draw_summary(frame, person_found, helmet, vest, frame_status)
        out.write(frame)

    cap.release()
    out.release()

    if not os.path.exists(output_video_path):
        print("RESULT:ERROR")
        return

    total_detected_frames = safe_count + warning_count + unsafe_count

    if total_detected_frames == 0:
        final_status = "NO PERSON"
        confidence = 0.00
    else:
        counts = {
            "SAFE": safe_count,
            "WARNING": warning_count,
            "UNSAFE": unsafe_count
        }
        final_status = max(counts, key=counts.get)
        confidence = (counts[final_status] / total_detected_frames) * 100

    print(f"RESULT:{final_status}")
    print(f"CONFIDENCE:{confidence:.2f}")
    print(f"OUTPUT_VIDEO:{os.path.abspath(output_video_path)}")


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("RESULT:ERROR")
    else:
        detect_video(sys.argv[1])