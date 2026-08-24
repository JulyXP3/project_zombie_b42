# -*- coding: utf-8 -*-
"""预览 EtherTheme.drawFrame 的真实观感: 用已入库的 frame_corner_*.png 四角
+ 青色直边, 复刻 Lua drawFrame 逻辑, 画在深底上, 供肉眼核对切角外框是否成形。"""
import os
from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
UI = os.path.join(ROOT, "etherhack-src", "src", "main", "resources", "EtherHack", "media", "ui")
OUT = os.path.join(ROOT, "temp", "frame_preview.png")

W, H = 520, 360
CYAN = (64, 224, 208, 255)
img = Image.new("RGBA", (W, H), (7, 13, 15, 255))

cs = 18          # 屏上角尺寸 (与 drawFrame 一致)
inset = int(round(4 * cs / 32))

def load(n):
    return Image.open(os.path.join(UI, n)).convert("RGBA").resize((cs, cs), Image.LANCZOS)

tl = load("frame_corner_tl.png"); tr = load("frame_corner_tr.png")
bl = load("frame_corner_bl.png"); br = load("frame_corner_br.png")
img.alpha_composite(tl, (0, 0))
img.alpha_composite(tr, (W - cs, 0))
img.alpha_composite(bl, (0, H - cs))
img.alpha_composite(br, (W - cs, H - cs))

d = ImageDraw.Draw(img)
# 四条直边 (与 drawFrame 一致: 在角之间, 对齐角纹理内线)
d.line([(cs, inset), (W - cs, inset)], fill=CYAN, width=1)
d.line([(cs, H - inset - 1), (W - cs, H - inset - 1)], fill=CYAN, width=1)
d.line([(inset, cs), (inset, H - cs)], fill=CYAN, width=1)
d.line([(W - inset - 1, cs), (W - inset - 1, H - cs)], fill=CYAN, width=1)

os.makedirs(os.path.dirname(OUT), exist_ok=True)
img.convert("RGB").save(OUT)
print("frame preview ->", OUT)
