package EtherHack.Ether;

import se.krka.kahlua.integration.annotations.LuaMethod;
import EtherHack.utils.Rendering;

/**
 * 物品雷达世界标记 (Lua OnPostUIDraw 逐帧绘制) 的绘制原语暴露。
 *
 * Rendering 的静态绘制方法没有 @LuaMethod, Lua 无法直接调用; 而走
 * SafeEtherLuaMethods 的保护代理 (protectionManager.invokeFunction) 每次调用
 * 要做保护名单线性扫描 + UUID 校验, 不适合逐帧逐标记的频率 —— 因此按
 * TrapSpawnAPI 同款模式直接全局暴露 (SafeExposer.exposeRenderingAPI)。
 */
public class RenderingAPI {
    /** 0.5px 细线: 与载具/僵尸雷达 (updateZombiesVisuals 等) 同一绘制入口 */
    @LuaMethod(name = "etherDrawThinLine", global = true)
    public static void drawThinLine(double x1, double y1, double x2, double y2,
                                    double r, double g, double b, double a) {
        Rendering.drawThinLine((float) x1, (float) y1, (float) x2, (float) y2,
                (float) r, (float) g, (float) b, (float) a);
    }
}
