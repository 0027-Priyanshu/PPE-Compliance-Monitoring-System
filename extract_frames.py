import cv2
import os
import sys


def extract_frames(video_path, output_dir, num_frames=6):
    cap = cv2.VideoCapture(video_path)

    if not cap.isOpened():
        print("ERROR: Cannot open video")
        sys.exit(1)

    total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))

    if total_frames <= 0:
        print("ERROR: Invalid frame count")
        cap.release()
        sys.exit(1)

    os.makedirs(output_dir, exist_ok=True)

    positions = []
    for i in range(num_frames):
        pos = int((i + 1) * total_frames / (num_frames + 1))
        positions.append(pos)

    saved = []

    for idx, pos in enumerate(positions):
        cap.set(cv2.CAP_PROP_POS_FRAMES, pos)
        ret, frame = cap.read()

        if ret:
            frame_path = os.path.join(output_dir, f"frame_{idx}.jpg")
            cv2.imwrite(frame_path, frame)
            saved.append(os.path.abspath(frame_path))

    cap.release()

    if not saved:
        print("ERROR: No frames extracted")
        sys.exit(1)

    for path in saved:
        print("FRAME:" + path)


if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("ERROR: Usage: python extract_frames.py <input_video> <output_dir>")
        sys.exit(1)

    extract_frames(sys.argv[1], sys.argv[2])