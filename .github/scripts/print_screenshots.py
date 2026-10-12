"""Prints downscaled game test screenshots as base64 into the CI log, so they can be read without artifact access."""
import base64
import io
import pathlib
import sys

from PIL import Image

names = sys.argv[1:]
shots = {p.stem: p for p in pathlib.Path(".").rglob("screenshots/*.png")}
for name in names:
    path = next((p for stem, p in shots.items() if stem.startswith(name)), None)
    if path is None:
        print(f"DTIMG-MISSING {name}")
        continue
    image = Image.open(path).convert("RGB")
    image.thumbnail((640, 640))
    buffer = io.BytesIO()
    image.save(buffer, "JPEG", quality=72)
    data = base64.b64encode(buffer.getvalue()).decode()
    for i in range(0, len(data), 4000):
        print(f"DTIMG {name} {data[i:i + 4000]}")
