package modcore.core;

import se.krka.kahlua.integration.annotations.LuaMethod;
import se.krka.kahlua.vm.KahluaTable;
import zombie.core.Core;
import zombie.iso.IsoCamera;
import zombie.iso.IsoUtils;
import modcore.utils.Logger;
import modcore.utils.Rendering;

/**
 * 物品雷达世界标记 (Lua OnPostUIDraw 逐帧绘制) 的绘制原语暴露。
 *
 * Rendering 的静态绘制方法没有 @LuaMethod, Lua 无法直接调用; 而走
 * SafeEtherLuaMethods 的保护代理 (protectionManager.invokeFunction) 每次调用
 * 要做保护名单线性扫描 + UUID 校验, 不适合逐帧逐标记的频率 —— 因此按
 * FishingSpawnAPI/TrapSpawnAPI 同款模式直接全局暴露 (SafeExposer.exposeRenderingAPI)。
 *
 * 2026-10-18 批量原语 (探鱼雷达性能设计, 详见 FishRadarAPI 类头与方案文档):
 *   Lua 逐帧把世界图元一次性交给 Java, 避免逐图元跨界调用 + 逐图元世界→屏幕
 *   投影。投影公式与 EtherItemSearch.lua worldToScreen (实机验证款) 逐字一致:
 *   (XToScreen/YToScreen - 相机偏移) / zoom, 玩家索引恒 0 (与 Lua 侧同注释的
 *   原因: Kahlua 不暴露 IsoCamera.frameState 字段)。
 */
public class RenderingAPI {

    private static final int CIRCLE_SEGMENTS = 16;   // 与 game LineDrawer.DrawIsoCircle 同段数
    private static boolean loggedError;

    /** 0.5px 细线: 与载具/僵尸雷达 (updateZombiesVisuals 等) 同一绘制入口 */
    @LuaMethod(name = "etherDrawThinLine", global = true)
    public static void drawThinLine(double x1, double y1, double x2, double y2,
                                    double r, double g, double b, double a) {
        Rendering.drawThinLine((float) x1, (float) y1, (float) x2, (float) y2,
                (float) r, (float) g, (float) b, (float) a);
    }

    /**
     * 批量贴地世界圆 (探鱼雷达鱼点圈/打窝圈)。平铺 7 数/圈:
     * {worldX, worldY, radius, r, g, b, a}; 竖直锚 z=0 贴地 (物品雷达同款 wallOffset=0)。
     * 形状语义与 game LineDrawer.DrawIsoCircle 一致: 16 段, 角度 i*360/16,
     * 顶点 = (wx + R*cosA, wy + R*sinA) (double 运算后转 float, 与官方同)。
     */
    @LuaMethod(name = "etherDrawIsoCircles", global = true)
    public static void drawIsoCircles(KahluaTable data, int count) {
        try {
            if (data == null || count <= 0) {
                return;
            }
            if (count > 1024) {
                count = 1024;
            }
            float zoom = Core.getInstance().getZoom(0);
            if (zoom <= 0.0f) {
                return;
            }
            int idx = 1;
            for (int c = 0; c < count; ++c) {
                float wx = (float) num(data.rawget(idx++));
                float wy = (float) num(data.rawget(idx++));
                float radius = (float) num(data.rawget(idx++));
                float r = (float) num(data.rawget(idx++));
                float g = (float) num(data.rawget(idx++));
                float b = (float) num(data.rawget(idx++));
                float a = (float) num(data.rawget(idx++));
                float prevX = 0.0f;
                float prevY = 0.0f;
                for (int seg = 0; seg <= CIRCLE_SEGMENTS; ++seg) {
                    double ang = Math.toRadians((double) seg * 360.0 / (double) CIRCLE_SEGMENTS);
                    float px = (float) ((double) wx + (double) radius * Math.cos(ang));
                    float py = (float) ((double) wy + (double) radius * Math.sin(ang));
                    float sx = screenX(px, py, zoom);
                    float sy = screenY(px, py, zoom);
                    if (seg > 0) {
                        Rendering.drawThinLine(prevX, prevY, sx, sy, r, g, b, a);
                    }
                    prevX = sx;
                    prevY = sy;
                }
            }
        }
        catch (Throwable t) {
            logError(t);
        }
    }

    /**
     * 批量贴地格轮廓 (探鱼雷达热力格)。平铺 6 数/格:
     * {worldX, worldY, r, g, b, a}; 格角 (x,y)-(x+1,y)-(x+1,y+1)-(x,y+1), z=0。
     */
    @LuaMethod(name = "etherDrawIsoCells", global = true)
    public static void drawIsoCells(KahluaTable data, int count) {
        try {
            if (data == null || count <= 0) {
                return;
            }
            if (count > 2048) {
                count = 2048;
            }
            float zoom = Core.getInstance().getZoom(0);
            if (zoom <= 0.0f) {
                return;
            }
            int idx = 1;
            for (int c = 0; c < count; ++c) {
                float wx = (float) num(data.rawget(idx++));
                float wy = (float) num(data.rawget(idx++));
                float r = (float) num(data.rawget(idx++));
                float g = (float) num(data.rawget(idx++));
                float b = (float) num(data.rawget(idx++));
                float a = (float) num(data.rawget(idx++));
                float x0 = screenX(wx, wy, zoom);
                float y0 = screenY(wx, wy, zoom);
                float x1 = screenX(wx + 1.0f, wy, zoom);
                float y1 = screenY(wx + 1.0f, wy, zoom);
                float x2 = screenX(wx + 1.0f, wy + 1.0f, zoom);
                float y2 = screenY(wx + 1.0f, wy + 1.0f, zoom);
                float x3 = screenX(wx, wy + 1.0f, zoom);
                float y3 = screenY(wx, wy + 1.0f, zoom);
                Rendering.drawThinLine(x0, y0, x1, y1, r, g, b, a);
                Rendering.drawThinLine(x1, y1, x2, y2, r, g, b, a);
                Rendering.drawThinLine(x2, y2, x3, y3, r, g, b, a);
                Rendering.drawThinLine(x3, y3, x0, y0, r, g, b, a);
            }
        }
        catch (Throwable t) {
            logError(t);
        }
    }

    private static float screenX(float wx, float wy, float zoom) {
        return (IsoUtils.XToScreen(wx, wy, 0.0f, 0) - IsoCamera.getOffX()) / zoom;
    }

    private static float screenY(float wx, float wy, float zoom) {
        return (IsoUtils.YToScreen(wx, wy, 0.0f, 0) - IsoCamera.getOffY()) / zoom;
    }

    private static double num(Object o) {
        return o instanceof Number ? ((Number) o).doubleValue() : 0.0;
    }

    private static void logError(Throwable t) {
        if (loggedError) {
            return;
        }
        loggedError = true;
        Logger.printLog("[FishRadar] draw error: " + t);
    }
}
