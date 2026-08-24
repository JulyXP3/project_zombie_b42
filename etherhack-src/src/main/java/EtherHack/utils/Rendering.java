/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.debug.LineDrawer
 *  zombie.ui.TextManager
 *  zombie.ui.UIFont
 */
package EtherHack.utils;

import zombie.debug.LineDrawer;
import zombie.ui.TextManager;
import zombie.ui.UIFont;

public class Rendering {
    public static void drawText(String var0, UIFont var1, float var2, float var3, float var4, float var5, float var6, float var7) {
        TextManager.instance.DrawString(var1, (double)var2, (double)var3, var0, (double)var4, (double)var5, (double)var6, (double)var7);
    }

    public static void drawTextCenterWithShadow(String var0, UIFont var1, float var2, float var3, float var4, float var5, float var6, float var7) {
        Rendering.drawTextCenterWithShadow(var0, var1, var2, var3, var4, var5, var6, var7, 1.0f);
    }

    public static void drawTextCenterWithShadow(String var0, UIFont var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8) {
        TextManager.instance.DrawStringCentre(var1, (double)(var2 + var8), (double)(var3 + var8), var0, 0.0, 0.0, 0.0, (double)var7);
        TextManager.instance.DrawStringCentre(var1, (double)var2, (double)var3, var0, (double)var4, (double)var5, (double)var6, (double)var7);
    }

    public static void drawTextCenter(String var0, UIFont var1, float var2, float var3, float var4, float var5, float var6, float var7) {
        TextManager.instance.DrawStringCentre(var1, (double)var2, (double)var3, var0, (double)var4, (double)var5, (double)var6, (double)var7);
    }

    public static void drawLine(int var0, int var1, int var2, int var3, float var4, float var5, float var6, float var7, int var8) {
        LineDrawer.drawLine((float)var0, (float)var1, (float)var2, (float)var3, (float)var4, (float)var5, (float)var6, (float)var7, (int)var8);
    }

    public static void drawCircle(float var0, float var1, float var2, int var3, float var4, float var5, float var6) {
        LineDrawer.drawCircle((float)var0, (float)var1, (float)var2, (int)var3, (float)var4, (float)var5, (float)var6);
    }

    public static void drawArc(float var0, float var1, float var2, float var3, float var4, float var5, int var6, float var7, float var8, float var9, float var10) {
        LineDrawer.drawArc((float)var0, (float)var1, (float)var2, (float)var3, (float)var4, (float)var5, (int)var6, (float)var7, (float)var8, (float)var9, (float)var10);
    }

    public static void drawRect(float var0, float var1, float var2, float var3, float var4, float var5, float var6, float var7, int var8) {
        LineDrawer.drawRect((float)var0, (float)var1, (float)var2, (float)var3, (float)var4, (float)var5, (float)var6, (float)var7, (int)var8);
    }

    public static int getLineH(UIFont var0) {
        return TextManager.instance.getFontHeight(var0);
    }

    // 细线 (0.5px 基/顶厚度, 走 LineDrawer 的 float 粗细重载): int 厚度的 1 在
    // 高分辨率下视觉偏粗, 用户反馈雷达线条太粗。
    public static void drawThinLine(float var0, float var1, float var2, float var3, float var4, float var5, float var6, float var7) {
        LineDrawer.drawLine(var0, var1, var2, var3, var4, var5, var6, var7, 0.5f, 0.5f);
    }

    // ESP 文字缩放 (与 UI 信息页 EtherTheme.hintScale 同一规则): B42 枚举里没有
    // 比 Small 更小的中文字体档 (fonts.txt: CN 的 NewSmall == Small), 缩小走
    // vanilla 的整串缩放绘制 DrawString(font, x, y, scale, str, ...) —— s_scale
    // 同乘字形与步进, 中/英/俄通用。0.70 约小 3 号; 缩后行高不足 13px 时抬高
    // 到 13px (vanilla 最小档行高, 再小汉字糊)。
    public static float espTextScale() {
        float var0 = (float)TextManager.instance.getFontHeight(UIFont.Small);
        return Math.max(0.70f, 13.0f / var0);
    }

    public static int getEspLineH() {
        return (int)((float)Rendering.getLineH(UIFont.Small) * Rendering.espTextScale()) + 2;
    }

    // 居中版缩放绘制: DrawString 无 Centre 变体, 用 MeasureStringX*scale 折算左上角。
    public static void drawEspTextCenter(String var0, float var1, float var2, float var3, float var4, float var5, float var6, float var7) {
        float var8 = (float)TextManager.instance.MeasureStringX(UIFont.Small, var0) * var1;
        float var9 = var2 - var8 / 2.0f;
        TextManager.instance.DrawString(UIFont.Small, (double)(var9 + 1.0f), (double)(var3 + 1.0f), (double)var1, var0, 0.0, 0.0, 0.0, (double)var7);
        TextManager.instance.DrawString(UIFont.Small, (double)var9, (double)var3, (double)var1, var0, (double)var4, (double)var5, (double)var6, (double)var7);
    }
}
