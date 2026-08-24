# -*- coding: utf-8 -*-
"""
示例图: 参考图的"拉条行"设计(拉条在左+数值紧随+标签靠右)与功能行间距节奏,
套用到 EtherHack 现有青霓虹皮肤(左磁贴导航 + 888x888 + 图集切角)上的静态 mockup。

资产直接取自 etherhack-src (chamfer_atlas.png / noise.png / 图标), 保证"示例即所得"。
产物: temp/slider_row_mockup.png
"""
import os
from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
UI = os.path.join(ROOT, "etherhack-src", "src", "main", "resources", "EtherHack", "media", "ui")
OUT = os.path.join(ROOT, "temp", "slider_row_mockup.png")

W, H = 888, 888
CYAN = (72, 216, 168)      # 薄荷青 #48D8A8 (参考图实测)
CYAN_DIM = (36, 106, 84)
SKY = (99, 199, 248)      # 天蓝 #63C7F8 (数值)
TEXT = (230, 250, 247)
TEXT_DIM = (153, 204, 204)
BG = (14, 18, 19)       # 近黑青 #0E1213 (参考图实测)
RAIL_BG = (19, 29, 29)
HEADER_H = 24
RAIL_W = 170

# ---- 资产 ----
atlas = Image.open(os.path.join(UI, "chamfer_atlas.png")).convert("RGBA")
noise = Image.open(os.path.join(UI, "noise.png")).convert("RGBA")

def corner(kind, which):
    """kind: 'tile'|'wedge'|'frame' -> (img, 尺寸)"""
    xs = {"tile": ({("tl",0),("tr",16),("bl",32),("br",48)}, 0, 12),
          "wedge": ({("tl",0),("tr",16),("bl",32),("br",48)}, 16, 12),
          "frame": ({("tl",0),("tr",36),("bl",72),("br",108)}, 32, 32)}
    m, y, n = xs[kind]
    x = dict(m)[which]
    return atlas.crop((x, y, x + n, y + n)), n

def font(sz, bold=False):
    for name in (["msyhbd.ttc"] if bold else []) + ["msyh.ttc", "simhei.ttf"]:
        p = os.path.join("C:\\Windows\\Fonts", name)
        if os.path.exists(p):
            return ImageFont.truetype(p, sz)
    return ImageFont.load_default()

img = Image.new("RGBA", (W, H), BG + (255,))

def chamfer_box(d, x, y, w, h, cs=12, outline=CYAN, width=1, alpha=255):
    """细净切角八边形描边 (贴图集角线 + 四条直边)。"""
    if w < cs * 2 + 2 or h < cs * 2 + 2:
        d.rectangle([x, y, x + w - 1, y + h - 1], outline=outline + (alpha,), width=width)
        return
    for which, (cx, cy) in {"tl": (x, y), "tr": (x + w - cs, y),
                            "bl": (x, y + h - cs), "br": (x + w - cs, y + h - cs)}.items():
        c, n = corner("tile", which)
        c = c.resize((cs, cs))
        tint = Image.new("RGBA", c.size, outline + (0,))
        c = Image.composite(c, Image.new("RGBA", c.size, (0, 0, 0, 0)),
                            c.split()[3])
        # 用颜色调制: 保留 alpha, 乘上 outline 色
        r, g, b, a = c.split()
        from PIL import Image as _I
        mul = _I.merge("RGBA", (r.point(lambda v: outline[0] * v // 255),
                                g.point(lambda v: outline[1] * v // 255),
                                b.point(lambda v: outline[2] * v // 255),
                                a.point(lambda v: v * alpha // 255)))
        img.paste(mul, (cx, cy), mul)
    d.line([x + cs, y, x + w - cs, y], fill=outline + (alpha,), width=width)
    d.line([x + cs, y + h - 1, x + w - cs, y + h - 1], fill=outline + (alpha,), width=width)
    d.line([x, y + cs, x, y + h - cs], fill=outline + (alpha,), width=width)
    d.line([x + w - 1, y + cs, x + w - 1, y + h - cs], fill=outline + (alpha,), width=width)

# ---- 玻璃底: 噪点 ----
img.paste(noise, (0, 0), noise.point(lambda v: int(v * 0.12)) if False else None) \
    if False else None
noise_low = noise.copy()
noise_low.putalpha(noise_low.split()[3].point(lambda v: v * 12 // 100))
img.alpha_composite(noise_low)

d = ImageDraw.Draw(img)

# ---- 页眉 ----
d.rectangle([0, 0, W, HEADER_H], fill=RAIL_BG + (245,))
d.line([0, HEADER_H - 1, W, HEADER_H - 1], fill=CYAN + (255,), width=1)
tf = font(13)
tw = d.textlength("ETHER HACK // B42", font=tf)
d.text(((W - tw) / 2, (HEADER_H - 17) / 2), "ETHER HACK // B42", font=tf, fill=CYAN + (255,))

# ---- 左侧导航磁贴 ----
d.rectangle([6, HEADER_H, 6 + RAIL_W, H - 7], fill=RAIL_BG + (235,))
tabs = [("info", "信息"), ("character", "角色"), ("itemCreator", "物品"), ("trap", "陷阱"),
        ("playerEditor", "编辑"), ("visuals", "视觉"), ("teleport", "传送"),
        ("exploit", "漏洞"), ("loot", "战利品"), ("settings", "设置")]
lf = font(12)
TILE_H, BOX_M, ICON, PAD_X, GAP = 44, 4, 22, 12, 10
for i, (icon, label) in enumerate(tabs):
    ty = HEADER_H + 8 + i * TILE_H
    active = (icon == "visuals")
    bx, bw = 6 + BOX_M, RAIL_W - BOX_M * 2
    if active:
        # 轻底
        d.rectangle([bx + 6, ty + BOX_M, bx + bw - 6, ty + TILE_H - BOX_M], fill=(16, 44, 44, 120))
    chamfer_box(d, bx, ty + BOX_M, bw, TILE_H - BOX_M * 2, cs=12,
                outline=CYAN if active else CYAN_DIM, alpha=255 if active else 110)
    if active:
        d.rectangle([bx + 1, ty + BOX_M + 12, bx + 5, ty + TILE_H - BOX_M - 12], fill=CYAN + (255,))
    ic = Image.open(os.path.join(UI, icon + ".png")).convert("RGBA").resize((ICON, ICON))
    if not active:
        r, g, b, a = ic.split()
        from PIL import Image as _I
        ic = _I.merge("RGBA", (r.point(lambda v: 230), g.point(lambda v: 240),
                               b.point(lambda v: 238), a.point(lambda v: v * 90 // 100)))
    img.paste(ic, (bx + PAD_X, ty + (TILE_H - ICON) // 2), ic)
    d.text((bx + PAD_X + ICON + GAP, ty + (TILE_H - 16) / 2), label, font=lf,
           fill=CYAN + (255,) if active else TEXT_DIM + (255,))

# ---- 内容区 (视觉页示例) ----
CX0, CX1 = 6 + RAIL_W + 6, W - 7
CW = CX1 - CX0
PAD = 16
sf = font(13, bold=True)
nf = font(12)

def section(y, title):
    """分区标题: 标题 + 两侧分割线, 分区之间大留白 (参考图的区块节奏)。"""
    tlen = d.textlength(title, font=sf)
    d.text((CX0 + PAD, y), title, font=sf, fill=CYAN + (255,))
    lx = CX0 + PAD + tlen + 12
    d.line([lx, y + 8, CX1 - PAD, y + 8], fill=CYAN_DIM + (150,), width=1)
    return y + 26

def slider_row(y, label, frac, val):
    """参考图的拉条行: 拉条在左 (~58%) + 数值紧随其后 + 标签靠右。行高 30, 行距 48 (1.6x)。"""
    RH = 30
    track_w = int(CW * 0.58)
    tx, ty2 = CX0 + PAD, y + 8
    # 轨道: 深槽 + 细青线
    d.rounded_rectangle([tx, ty2 + 12, tx + track_w, ty2 + 16], radius=2, fill=(20, 38, 40, 255))
    d.line([tx + 2, ty2 + 14, tx + track_w - 2, ty2 + 14], fill=CYAN_DIM + (220,), width=1)
    # 已填充段 + 滑块拇指 (亮青小切角块)
    px = tx + 4 + int((track_w - 8) * frac)
    d.line([tx + 4, ty2 + 14, px, ty2 + 14], fill=CYAN + (255,), width=2)
    chamfer_box(d, px - 5, ty2 + 6, 11, 11, cs=4, outline=CYAN, alpha=255)
    # 数值 (紧随拉条右侧)
    d.text((tx + track_w + 14, y + 7), val, font=nf, fill=SKY + (255,))
    # 标签靠右
    tl = d.textlength(label, font=nf)
    d.text((CX1 - PAD - tl, y + 7), label, font=nf, fill=TEXT + (255,))
    return y + 48

def check_row(y, label, checked):
    RH = 26
    bx, by = CX0 + PAD, y + 5
    d.rectangle([bx, by, bx + 15, by + 15], outline=CYAN_DIM + (200,), width=1)
    if checked:
        d.rectangle([bx + 3, by + 3, bx + 12, by + 12], fill=CYAN + (255,))
    d.text((bx + 24, y + 4), label, font=nf, fill=TEXT + (255,))
    return y + 34

y = HEADER_H + 16
y = section(y, "环境与视野")
y = slider_row(y, "夜视亮度", 0.65, "0.65")
y = slider_row(y, "灯泡半径", 0.72, "18.0")
y = slider_row(y, "雾气密度", 0.30, "0.30")
y += 14
y = section(y, "显示效果")
y = check_row(y, "隐身模式 (Ghost)", True)
y = check_row(y, "穿墙模式 (NoClip)", False)
y = check_row(y, "高亮容器轮廓", True)
y += 14
y = section(y, "实用操作")
# 两枚按钮并排 (沿用行宽节奏)
bw2, bh2 = 150, 28
gap = 12
chamfer_box(d, CX0 + PAD, y, bw2, bh2, cs=6, outline=CYAN, alpha=190)
chamfer_box(d, CX0 + PAD + bw2 + gap, y, bw2, bh2, cs=6, outline=CYAN_DIM, alpha=140)
bt = "全图标记"; bt2 = "清除标记"
b1 = d.textlength(bt, font=nf); b2 = d.textlength(bt2, font=nf)
d.text((CX0 + PAD + (bw2 - b1) / 2, y + 6), bt, font=nf, fill=TEXT + (255,))
d.text((CX0 + PAD + bw2 + gap + (bw2 - b2) / 2, y + 6), bt2, font=nf, fill=TEXT_DIM + (255,))

# ---- 扫描线 + 外框 ----
scan = atlas.crop((0, 68, 128, 196))
scan_low = scan.copy()
scan_low.putalpha(scan_low.split()[3].point(lambda v: v * 50 // 100))
for sy in range(0, H, 128):
    for sx in range(0, W, 128):
        img.alpha_composite(scan_low.resize((min(128, W - sx), min(128, H - sy))), (sx, sy))

FCS = 18
for which, (fx, fy) in {"tl": (0, 0), "tr": (W - FCS, 0), "bl": (0, H - FCS), "br": (W - FCS, H - FCS)}.items():
    c, _ = corner("frame", which)
    img.paste(c.resize((FCS, FCS)), (fx, fy), c.resize((FCS, FCS)))
inset = 4 * FCS // 32
d.line([FCS, inset, W - FCS, inset], fill=CYAN + (255,), width=1)
d.line([FCS, H - inset - 1, W - FCS, H - inset - 1], fill=CYAN + (255,), width=1)
d.line([inset, FCS, inset, H - FCS], fill=CYAN + (255,), width=1)
d.line([W - inset - 1, FCS, W - inset - 1, H - FCS], fill=CYAN + (255,), width=1)

os.makedirs(os.path.dirname(OUT), exist_ok=True)
img.convert("RGB").save(OUT)
print("mockup ->", OUT, img.size)
