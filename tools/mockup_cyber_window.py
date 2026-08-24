# -*- coding: utf-8 -*-
"""
赛博朋克窗口 视觉目标稿 (mockup) —— 因原 cyber-A-refined.png 已随 temp/ 清空丢失,
本脚本用 Pillow 重建"近黑底 + 青霓虹细线 + 切角边框 + 辉光 + 扫描线"的目标观感,
作为实现 Lua UI 时对齐的具体参照 (产物在 temp/, 不入库)。

要点 (方案 §2/§6):
  - 近黑带青底; 主色青霓虹 #40E0D0; 细单描边 + 轻辉光; 静态扫描线;
  - 深色页眉(非亮青实心条) + 青霓虹标题文字; 切角八边形外框;
  - 列表/输入/按钮 = 深底 + 青霓虹切角描边; 多列密集。
"""
import os
from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "temp", "cyber_mockup.png")

W, H = 1044, 660
SS = 2                      # 超采样
CW, CH = W * SS, H * SS

# 调色板
BG        = (7, 13, 15)
PANEL     = (10, 18, 20)
CYAN      = (64, 224, 208)
CORE      = (185, 255, 248)   # 霓虹高光核
DIM       = (95, 165, 160)    # 次要青
TEXT      = (200, 240, 238)
WARN      = (255, 90, 90)


def F(sz, bold=True):
    names = (["consolab.ttf", "seguisb.ttf", "segoeuib.ttf", "arialbd.ttf"]
             if bold else ["consola.ttf", "segoeui.ttf", "arial.ttf"])
    for n in names:
        p = "C:/Windows/Fonts/" + n
        if os.path.exists(p):
            try:
                return ImageFont.truetype(p, sz * SS)
            except Exception:
                pass
    return ImageFont.load_default()


def chamfer_pts(x0, y0, x1, y1, c):
    """切角八边形顶点 (矩形四角各切 45 度, 切角边长 c)。"""
    return [
        (x0 + c, y0), (x1 - c, y0), (x1, y0 + c), (x1, y1 - c),
        (x1 - c, y1), (x0 + c, y1), (x0, y1 - c), (x0, y0 + c),
    ]


def sc(v):
    return v * SS


def main():
    base = Image.new("RGBA", (CW, CH), BG + (255,))
    glow = Image.new("RGBA", (CW, CH), (0, 0, 0, 0))
    core = Image.new("RGBA", (CW, CH), (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow)
    cd = ImageDraw.Draw(core)
    bd = ImageDraw.Draw(base)

    def neon_poly(pts, gw, cw):
        p = [(x, y) for (x, y) in pts]
        gd.line(p + [p[0]], fill=CYAN + (255,), width=gw, joint="curve")
        cd.line(p + [p[0]], fill=CORE + (255,), width=cw, joint="curve")

    def neon_rect(x0, y0, x1, y1, c=10, gw=6, cw=2, fill=None):
        pts = chamfer_pts(x0, y0, x1, y1, sc(c))
        if fill:
            bd.polygon(pts, fill=fill + (255,))
        neon_poly(pts, gw, cw)

    def neon_hline(x0, x1, y, gw=6, cw=2):
        gd.line([(x0, y), (x1, y)], fill=CYAN + (255,), width=gw)
        cd.line([(x0, y), (x1, y)], fill=CORE + (255,), width=cw)

    # ---- 外框: 切角八边形 + 辉光 ----
    neon_rect(sc(6), sc(6), CW - sc(6), CH - sc(6), c=16, gw=7, cw=2)

    # ---- 页眉 (深色, 非亮青实心) + 青霓虹标题 ----
    hh = sc(54)
    bd.rectangle([sc(8), sc(8), CW - sc(8), hh], fill=PANEL + (255,))
    neon_hline(sc(20), CW - sc(20), hh, gw=6, cw=2)          # 页眉下缘霓虹线
    # 标题 (左, 青霓虹, 带辉光)
    ft = F(21)
    title = "E T H E R   H A C K   //   B42"
    gd.text((sc(22), sc(14)), title, font=ft, fill=CYAN + (255,))
    cd.text((sc(22), sc(14)), title, font=ft, fill=CORE + (255,))
    # 署名/声明 (右, 次要青, 两行)
    fs = F(13, bold=False)
    cd.text((CW - sc(360), sc(12)), "Author: Quzile & dei0 & JulyXP3", font=fs, fill=DIM + (255,))
    cd.text((CW - sc(360), sc(32)), "Free & Open Source · If you paid, you got scammed",
            font=fs, fill=DIM + (255,))

    # ---- 左侧导航条 (深底 + 图标 + 激活态) ----
    navx0, navx1 = sc(8), sc(64)
    for i in range(9):
        cy = hh + sc(20) + i * sc(52)
        active = (i == 1)
        if active:
            # 激活: 左青竖条 + 轻底
            bd.rectangle([navx0, cy - sc(12), navx1, cy + sc(28)], fill=(14, 30, 32, 255))
            gd.line([(navx0 + sc(1), cy - sc(12)), (navx0 + sc(1), cy + sc(28))],
                    fill=CYAN + (255,), width=sc(3))
        col = CORE if active else DIM
        # 简易图标: 圆环
        r = sc(11)
        icx = (navx0 + navx1) / 2
        cd.ellipse([icx - r, cy - sc(2) + sc(6) - r, icx + r, cy - sc(2) + sc(6) + r],
                   outline=col + (255,), width=sc(2))

    # ---- 内容区 ----
    cx0 = navx1 + sc(20)
    cx1 = CW - sc(24)
    cyc = hh + sc(18)
    fl = F(16)
    fsm = F(14, bold=False)

    cd.text((cx0, cyc), "Configs list:", font=fl, fill=TEXT + (255,))
    cyc += sc(30)

    # 列表框 (切角霓虹描边 + 深底)
    lb_y1 = cyc + sc(150)
    neon_rect(cx0, cyc, cx1, lb_y1, c=10, gw=5, cw=1, fill=(9, 16, 18))
    cd.text((cx0 + sc(16), cyc + sc(10)), "Name", font=fsm, fill=TEXT + (255,))
    neon_hline(cx0 + sc(6), cx1 - sc(6), cyc + sc(34), gw=3, cw=1)
    cd.text((cx0 + sc(16), cyc + sc(44)), "language", font=fsm, fill=TEXT + (255,))
    cd.text((cx0 + sc(16), cyc + sc(70)), "startup", font=fsm, fill=CORE + (255,))

    # 输入框 + 按钮行 (切角霓虹)
    ry = lb_y1 + sc(18)
    neon_rect(cx0, ry, cx0 + sc(240), ry + sc(38), c=8, gw=4, cw=1, fill=(9, 16, 18))
    cd.text((cx0 + sc(12), ry + sc(9)), "EtherConfig-3", font=fsm, fill=TEXT + (255,))
    for k, lab in enumerate(["Save", "Load", "Delete"]):
        bx0 = cx0 + sc(260) + k * sc(150)
        fill = (12, 40, 40) if k == 0 else (9, 16, 18)
        neon_rect(bx0, ry, bx0 + sc(130), ry + sc(38), c=8, gw=4, cw=1, fill=fill)
        col = CORE if k == 0 else DIM
        cd.text((bx0 + sc(38), ry + sc(9)), lab, font=fsm, fill=col + (255,))

    # 颜色行 (标签 + 切角色块) —— 演示多列密集: 两列
    ry2 = ry + sc(64)
    rows = [("Accent color", CYAN), ("Players UI color", (255, 60, 100)),
            ("Vehicles UI color", (150, 150, 210)), ("Zombies UI color", (255, 150, 90))]
    colw = (cx1 - cx0) / 2
    for k, (lab, sw) in enumerate(rows):
        rx = cx0 + (k % 2) * colw
        rry = ry2 + (k // 2) * sc(44)
        cd.text((rx, rry + sc(6)), lab, font=fsm, fill=TEXT + (255,))
        neon_rect(rx + colw - sc(60), rry, rx + colw - sc(24), rry + sc(30), c=6, gw=3, cw=1, fill=sw)

    # ---- 合成: 辉光模糊 + 叠加 ----
    glow = glow.filter(ImageFilter.GaussianBlur(sc(3)))
    out = Image.alpha_composite(base, glow)
    out = Image.alpha_composite(out, core)

    # ---- 静态扫描线 (极淡) ----
    sl = Image.new("RGBA", (CW, CH), (0, 0, 0, 0))
    sld = ImageDraw.Draw(sl)
    for y in range(0, CH, sc(3)):
        sld.line([(0, y), (CW, y)], fill=(0, 0, 0, 46), width=1)
    out = Image.alpha_composite(out, sl)

    out = out.convert("RGB").resize((W, H), Image.LANCZOS)
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    out.save(OUT)
    print("mockup ->", OUT, out.size)


if __name__ == "__main__":
    main()
