"""Prints a small contact sheet of the gallery screenshots as base64 into the CI log
(lets the screenshots be reviewed from the log when artifacts cannot be downloaded)."""
import base64
import io
import pathlib
import sys

from PIL import Image

shots = sorted((p for p in pathlib.Path(sys.argv[1]).rglob("*gallery_*.png") if "screenshots" in p.parts),
               key=lambda p: int(p.stem[p.stem.index("gallery_"):].split("_")[1]))
if not shots:
    sys.exit("no gallery screenshots found")
thumb_w, thumb_h, columns = 480, 270, 2
rows = (len(shots) + columns - 1) // columns
sheet = Image.new("RGB", (thumb_w * columns, thumb_h * rows), (0, 0, 0))
for i, shot in enumerate(shots):
    image = Image.open(shot).convert("RGB").resize((thumb_w, thumb_h), Image.LANCZOS)
    sheet.paste(image, ((i % columns) * thumb_w, (i // columns) * thumb_h))
buffer = io.BytesIO()
sheet.save(buffer, "JPEG", quality=68)
data = base64.b64encode(buffer.getvalue()).decode()
for i in range(0, len(data), 4000):
    print("DTSHEET " + data[i:i + 4000])
