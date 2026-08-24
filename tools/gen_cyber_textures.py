# -*- coding: utf-8 -*-
"""
赛博朋克 UI 纹理产线 (赛博朋克UI设计方案.md §6 / §12 步骤1)。

用 Pillow 生成青霓虹 (#40E0D0) 主题的 PNG 资产, 覆盖到:
    etherhack-src/src/main/resources/EtherHack/media/ui/

设计要点 (对齐方案 §2/§3/§6):
  - "A-clean 细线" 风: 近白高光核 + 青色外辉光, 单描边;
  - 图标 32x32, 高倍作画 + LANCZOS 缩放, 保证平滑;
  - 复选框语义 (见 UICheckbox.lua:31-33):
        unchecked 以白色 (1,1,1) 绘制 -> 直接烘焙青色;
        checked   以 accentColor 相乘   -> 只作浅色, 由青 accent 上色, 避免青x青双重变暗;
  - close_re 由 ISButton:setImage 原样绘制 -> 直接烘焙青色;
  - 边框角/扫描线为新资产, 步骤2 的 drawFrame/drawScanlines 消费。

产物不可视验证: 另存一张对照预览到 temp/cyber_preview.png 供人工/游戏内核对。
本脚本纯生成资产, 不触碰任何 Lua 逻辑与作弊契约。
"""
import os
import math
from PIL import Image, ImageDraw, ImageFilter

try:
    LANCZOS = Image.Resampling.LANCZOS
    NEAREST = Image.Resampling.NEAREST
except AttributeError:  # 老版本 Pillow 兜底
    LANCZOS = Image.LANCZOS
    NEAREST = Image.NEAREST

# ---- 路径 ----
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "etherhack-src", "src", "main", "resources",
                   "EtherHack", "media", "ui")
PREVIEW = os.path.join(ROOT, "temp", "cyber_preview.png")

# ---- 调色板 ----
CYAN = (72, 216, 168, 255)      # #48D8A8 薄荷青 (辉光/描边, 参考图实测)
CORE = (222, 255, 251, 255)     # 近白高光核 (线条内芯)
BG_PREVIEW = (6, 12, 14, 255)   # 预览底色 (近黑带青, 呼应 glassBG)

# ---- 作画参数 ----
S = 8                # 超采样倍率
GLOW_W = 3.0         # 辉光笔宽 (目标像素)
CORE_W = 1.5         # 内芯笔宽 (目标像素)
GLOW_BLUR = 2.2      # 辉光高斯半径 (目标像素)


def canvas(size):
    return Image.new("RGBA", (size * S, size * S), (0, 0, 0, 0))


def wpx(w):
    return max(1, int(round(w * S)))


def _pts(pts):
    return [(x * S, y * S) for (x, y) in pts]


def line(d, col, w, pts):
    d.line(_pts(pts), fill=col, width=wpx(w), joint="curve")


def ellipse(d, col, w, box, fill=None):
    d.ellipse([box[0] * S, box[1] * S, box[2] * S, box[3] * S],
              outline=col, width=wpx(w), fill=fill)


def arc(d, col, w, box, a, b):
    d.arc([box[0] * S, box[1] * S, box[2] * S, box[3] * S], a, b,
          fill=col, width=wpx(w))


def dot(d, col, cx, cy, r):
    d.ellipse([(cx - r) * S, (cy - r) * S, (cx + r) * S, (cy + r) * S], fill=col)


def neon(size, drawfn, glow_w=GLOW_W, core_w=CORE_W, blur=GLOW_BLUR):
    """两遍合成霓虹: 青辉光(模糊) 底 + 近白内芯 面, 再 LANCZOS 缩放到目标尺寸。"""
    glow = canvas(size)
    drawfn(ImageDraw.Draw(glow), CYAN, glow_w)
    glow = glow.filter(ImageFilter.GaussianBlur(blur * S))
    core = canvas(size)
    drawfn(ImageDraw.Draw(core), CORE, core_w)
    out = Image.alpha_composite(glow, core)
    return out.resize((size, size), LANCZOS)


# =====================================================================
# 10 个选项卡图标 (drawfn: (d, col, w) -> 在 32x32 目标坐标系作画)
# =====================================================================
def ic_info(d, col, w):
    ellipse(d, col, w, (5, 5, 27, 27))
    dot(d, col, 16, 10, 1.6)
    line(d, col, w * 1.2, [(16, 14), (16, 22)])


def ic_character(d, col, w):
    ellipse(d, col, w, (11, 5, 21, 15))          # 头
    arc(d, col, w, (6, 17, 26, 37), 180, 360)     # 肩


def ic_itemCreator(d, col, w):
    line(d, col, w, [(8, 9), (24, 9), (24, 25), (8, 25), (8, 9)])  # 箱
    line(d, col, w, [(16, 12), (16, 22)])                          # +
    line(d, col, w, [(11, 17), (21, 17)])


def ic_trap(d, col, w):
    line(d, col, w, [(5, 25), (27, 25)])          # 基线
    arc(d, col, w, (7, 15, 25, 35), 180, 360)     # 雷体穹顶
    line(d, col, w, [(16, 15), (16, 8)])          # 触角
    dot(d, col, 16, 7, 1.5)                       # 触点


def ic_playerEditor(d, col, w):
    ellipse(d, col, w, (8, 6, 16, 14))            # 头 (偏左)
    arc(d, col, w, (5, 16, 19, 34), 180, 360)     # 肩
    line(d, col, w * 1.1, [(19, 18), (27, 8)])    # 铅笔身
    line(d, col, w, [(17, 19), (20, 17)])         # 铅笔尖


def ic_visuals(d, col, w):
    ellipse(d, col, w, (4, 9, 28, 23))            # 眼眶
    ellipse(d, col, w, (12, 11, 20, 21))          # 虹膜
    dot(d, col, 16, 16, 1.5)                      # 瞳


def ic_teleport(d, col, w):
    ellipse(d, col, w, (10, 5, 22, 17))           # 定位头
    dot(d, col, 16, 11, 1.6)
    line(d, col, w, [(11.5, 15), (16, 27)])       # 针尖
    line(d, col, w, [(20.5, 15), (16, 27)])


def ic_exploit(d, col, w):
    line(d, col, w, [(18, 4), (9, 18), (15, 18), (13, 28),
                     (23, 12), (17, 12), (18, 4)])  # 闪电


def ic_loot(d, col, w):
    line(d, col, w, [(7, 8), (25, 8), (25, 26), (7, 26), (7, 8)])  # 骰子
    for (cx, cy) in [(11, 12), (21, 12), (16, 17), (11, 22), (21, 22)]:
        dot(d, col, cx, cy, 1.3)


def ic_settings(d, col, w):
    ellipse(d, col, w, (8, 8, 24, 24))            # 齿轮外圈
    ellipse(d, col, w, (13, 13, 19, 19))          # 轴孔
    for k in range(8):                            # 8 齿
        a = math.radians(k * 45)
        c, s = math.cos(a), math.sin(a)
        line(d, col, w * 1.1,
             [(16 + 8 * c, 16 + 8 * s), (16 + 11 * c, 16 + 11 * s)])


def ic_vehicle(d, col, w):
    line(d, col, w, [(4, 17), (4, 14.5), (8, 13.5), (11, 9), (18.5, 9),
                     (22, 13.5), (27, 14.5), (27, 17)])  # 车身侧影
    line(d, col, w, [(14.8, 9.5), (14.8, 13)])           # 前后窗分隔
    line(d, col, w, [(4, 17), (7, 17)])                  # 底边-前轮拱
    line(d, col, w, [(13, 17), (19, 17)])                # 底边-中段
    line(d, col, w, [(25, 17), (27, 17)])                # 底边-后轮拱
    ellipse(d, col, w, (7, 17, 13, 23))                  # 前轮
    ellipse(d, col, w, (19, 17, 25, 23))                 # 后轮


ICONS = {
    "info": ic_info,
    "character": ic_character,
    "itemCreator": ic_itemCreator,
    "trap": ic_trap,
    "playerEditor": ic_playerEditor,
    "visuals": ic_visuals,
    "teleport": ic_teleport,
    "exploit": ic_exploit,
    "loot": ic_loot,
    "settings": ic_settings,
    "vehicle": ic_vehicle,
}


# =====================================================================
# 复选框 (64x32, 绘制时缩放到 32x16)
# =====================================================================
def make_checkbox(checked):
    # 方形复选框 (对齐 cyber-A-refined): 青霓虹圆角方框; 勾选时内含青对勾。
    # 烘焙青色, UICheckbox 以 (1,1,1) 绘制, 不受 accent 影响, 始终青。
    n = 32
    img = Image.new("RGBA", (n * S, n * S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    r = 6 * S
    box = [3 * S, 3 * S, (n - 3) * S, (n - 3) * S]
    fill = (14, 40, 38, 190) if checked else (10, 24, 26, 140)
    d.rounded_rectangle(box, radius=r, fill=fill, outline=CYAN, width=wpx(2))
    if checked:
        # 青霓虹对勾
        d.line([(10 * S, 16 * S), (14.5 * S, 21 * S), (23 * S, 10 * S)],
               fill=CORE, width=wpx(3), joint="curve")
    glow = img.filter(ImageFilter.GaussianBlur(GLOW_BLUR * S))
    out = Image.alpha_composite(glow, img)
    return out.resize((n, n), LANCZOS)


# =====================================================================
# 关闭键 (16x16, 青色 X)
# =====================================================================
def ic_close(d, col, w):
    line(d, col, w, [(4.5, 4.5), (11.5, 11.5)])
    line(d, col, w, [(11.5, 4.5), (4.5, 11.5)])


# =====================================================================
# 切角边框 - 左上角 (32x32), 其余三角由翻转得到 (§3 drawFrame 9宫格)
# =====================================================================
def corner_tl(d, col, w):
    # 顶边内收 -> 45度切角 -> 左边内收; 4px 留白给辉光
    line(d, col, w, [(32, 4), (12, 4), (4, 12), (4, 32)])


def make_frame_corners():
    tl = neon(32, corner_tl, glow_w=3.2, core_w=1.6, blur=2.0)
    return {
        "frame_corner_tl": tl,
        "frame_corner_tr": tl.transpose(Image.FLIP_LEFT_RIGHT),
        "frame_corner_bl": tl.transpose(Image.FLIP_TOP_BOTTOM),
        "frame_corner_br": tl.transpose(Image.ROTATE_180),
    }


# =====================================================================
# 磁贴切角 (12x12, 细净单线, 无辉光) — 导航磁贴/功能行盒子专用
#
# 与 frame_corner_* 的区别 (这是概念图对齐的关键):
#   frame_corner_* = 整窗大外框, 烘焙"白核 + 青辉光", 32px, 角上有明显光斑;
#   tile_corner_*  = 磁贴/行盒切角, 纯白细线 + 无模糊, 由 Lua 端以 (r,g,b,a)
#                    相乘染成亮青(激活)或暗青(未激活)。
# 概念图 cyber-A-refined 的导航磁贴是"细净等宽描边的八边形, 且切角明显偏大",
# 因此这里画成"整块纹理即一条 45° 斜线"(纯切角), 而不是小缺口 + 直边,
# 否则角看起来近乎方角, 与概念图不像。
#
# 纹理即切角本体: 斜线从 (CS, 0.5) 连到 (0.5, CS);
# Lua 端 4 条直边接在 x+cs / y+cs 处, 与斜线端点严丝合缝。
# 线宽给 1.5px: 行盒会把 12px 纹理缩到 8px 绘制, 缩放后仍保有约 1px 实线。
# =====================================================================
TILE_CS = 12


def tile_corner_tl(d, col, w):
    line(d, col, w, [(TILE_CS, 0.5), (0.5, TILE_CS)])


def make_tile_corners():
    # 纯白细线, 不做辉光合成 (neon 会加模糊 -> 小尺寸糊成光斑)
    img = canvas(TILE_CS)
    tile_corner_tl(ImageDraw.Draw(img), CORE, 1.5)
    tl = img.resize((TILE_CS, TILE_CS), LANCZOS)
    return {
        "tile_corner_tl": tl,
        "tile_corner_tr": tl.transpose(Image.FLIP_LEFT_RIGHT),
        "tile_corner_bl": tl.transpose(Image.FLIP_TOP_BOTTOM),
        "tile_corner_br": tl.transpose(Image.ROTATE_180),
    }


# =====================================================================
# 磁贴切角楔形 (与 tile_corner_* 配套的实心填充块)
#
# 为什么需要它: 切角盒的底色若用矩形近似 (上下两段内缩 cs + 中段满宽),
# 在 y=cs 处会出现"从内缩跳到满宽"的台阶, 视觉上底色呈十字外凸,
# 与八边形描边不吻合 (实测缺陷)。这里把切角内侧那半三角烘焙成纹理,
# 由 EtherTheme.fillChamfer 以 4 次贴图补齐四角, 几何精确且开销恒定。
#
# tl 角: 斜边 (cs,0)-(0,cs), 内侧 = 右下三角 (cs,0)-(cs,cs)-(0,cs)
# =====================================================================
def make_tile_wedges():
    n = TILE_CS
    img = Image.new("RGBA", (n * S, n * S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.polygon([(n * S, 0), (n * S, n * S), (0, n * S)], fill=(255, 255, 255, 255))
    tl = img.resize((n, n), LANCZOS)
    return {
        "tile_wedge_tl": tl,
        "tile_wedge_tr": tl.transpose(Image.FLIP_LEFT_RIGHT),
        "tile_wedge_bl": tl.transpose(Image.FLIP_TOP_BOTTOM),
        "tile_wedge_br": tl.transpose(Image.ROTATE_180),
    }


# =====================================================================
# 切角图集 chamfer_atlas.png (144x196)
#
# 全部皮肤小纹理(角线/楔形/外框角/扫描线)合成一张, 消除每盒 8 次纹理切换:
# EtherTheme.lua 用 vanilla drawSubTexture 画图集子区域, 整个皮肤共享一次绑定。
# 布局与 EtherTheme.lua 的 atlas* 常量一一对应, 改任一侧必须同步另一侧:
#   y=0   tile_corner tl/tr/bl/br  (x=0/16/32/48,  内容 12px)
#   y=16  tile_wedge  tl/tr/bl/br  (x=0/16/32/48,  内容 12px)
#   y=32  frame_corner tl/tr/bl/br (x=0/36/72/108, 内容 32px)
#   y=68  scanlines 128x128        (x=0)
# =====================================================================
ATLAS_W, ATLAS_H = 144, 196


def _atlas_cells():
    keys = ("tl", "tr", "bl", "br")
    return (
        [("tile_corner_" + k, i * 16, 0) for i, k in enumerate(keys)] +
        [("tile_wedge_" + k, i * 16, 16) for i, k in enumerate(keys)] +
        [("frame_corner_" + k, i * 36, 32) for i, k in enumerate(keys)] +
        [("scanlines", 0, 68)]
    )


def compose_chamfer_atlas(get_img):
    """get_img(name) -> PIL Image; 拼贴图集。"""
    atlas = Image.new("RGBA", (ATLAS_W, ATLAS_H), (0, 0, 0, 0))
    for name, x, y in _atlas_cells():
        atlas.paste(get_img(name), (x, y))
    return atlas


def load_asset_png(name):
    return Image.open(os.path.join(OUT, name + ".png")).convert("RGBA")


# =====================================================================
# 扫描线平铺 (128x128, 静态叠层; drawScanlines 消费; 已并入图集)
# =====================================================================
def make_scanlines():
    n = 128
    img = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    for y in range(0, n, 3):
        d.line([(0, y), (n, y)], fill=(0, 0, 0, 60), width=1)        # 暗线
        d.line([(0, y + 1), (n, y + 1)], fill=(64, 224, 208, 10), width=1)  # 极淡青
    return img


# =====================================================================
# 生成 + 预览
# =====================================================================
def build_all():
    assets = {}
    for name, fn in ICONS.items():
        assets[name] = neon(32, fn)
    assets["checkbox_unchecked"] = make_checkbox(False)
    assets["checkbox_checked"] = make_checkbox(True)
    assets["close_re"] = neon(16, ic_close, glow_w=2.6, core_w=1.4, blur=1.6)
    assets.update(make_frame_corners())
    assets.update(make_tile_corners())
    assets.update(make_tile_wedges())
    assets["scanlines"] = make_scanlines()
    # 图集由上面各单元拼贴 (全量重跑时保证图集与单元同步)
    assets["chamfer_atlas"] = compose_chamfer_atlas(assets.get)
    return assets


def save_all(assets):
    os.makedirs(OUT, exist_ok=True)
    for name, im in assets.items():
        p = os.path.join(OUT, name + ".png")
        im.save(p)
        a = im.split()[3]
        cover = sum(1 for px in a.getdata() if px > 20)
        total = im.size[0] * im.size[1]
        print("  %-22s %-9s alpha=%5.1f%%" %
              (name + ".png", "%dx%d" % im.size, 100.0 * cover / total))


def save_preview(assets):
    order = list(ICONS.keys()) + ["checkbox_unchecked", "checkbox_checked",
                                  "close_re", "chamfer_atlas"]
    scale = 5
    cell_w, cell_h = 64 * scale + 20, 64 * scale + 34
    cols = 6
    rows = (len(order) + cols - 1) // cols
    sheet = Image.new("RGBA", (cols * cell_w, rows * cell_h), BG_PREVIEW)
    d = ImageDraw.Draw(sheet)
    for i, name in enumerate(order):
        im = assets[name]
        big = im.resize((im.size[0] * scale, im.size[1] * scale), NEAREST)
        cx = (i % cols) * cell_w
        cy = (i // cols) * cell_h
        ox = cx + (cell_w - big.size[0]) // 2
        oy = cy + 24 + (64 * scale - big.size[1]) // 2
        # 单元底衬 (略亮) 便于看透明区
        d.rectangle([cx + 6, cy + 22, cx + cell_w - 6, cy + cell_h - 6],
                    fill=(14, 24, 27, 255))
        sheet.alpha_composite(big, (ox, oy))
        d.text((cx + 10, cy + 6), name, fill=(180, 240, 236, 255))
    os.makedirs(os.path.dirname(PREVIEW), exist_ok=True)
    sheet.save(PREVIEW)
    print("preview -> %s (%dx%d)" % (PREVIEW, sheet.size[0], sheet.size[1]))


if __name__ == "__main__":
    import sys
    # 可选前缀过滤: 只写出匹配的资产 (例: python gen_cyber_textures.py tile_corner)
    # 用途: 新增资产时避免用当前 Pillow 版本重写既有 PNG (缩放/模糊实现差异会造成无谓 diff)。
    # 特例 "atlas": 只重拼 chamfer_atlas.png, 且拼贴来源是**已入库的单张 PNG**
    # (而非内存重生成), 保证与现行游戏内视觉逐像素一致。
    args = [a for a in sys.argv[1:] if not a.startswith("-")]
    print("[gen] 输出目录:", OUT)
    if args == ["atlas"]:
        atlas = compose_chamfer_atlas(load_asset_png)
        p = os.path.join(OUT, "chamfer_atlas.png")
        atlas.save(p)
        print("[gen] atlas -> %s (%dx%d, 拼自入库 PNG)" % (p, ATLAS_W, ATLAS_H))
    else:
        prefixes = args
        a = build_all()
        if prefixes:
            a = {k: v for k, v in a.items() if any(k.startswith(p) for p in prefixes)}
            print("[gen] 仅写出前缀匹配: %s -> %d 个" % (", ".join(prefixes), len(a)))
            save_all(a)
            print("[gen] 完成 (已跳过预览图, 避免覆盖全量对照)")
        else:
            save_all(a)
            save_preview(a)
            print("[gen] 完成: %d 个纹理" % len(a))
