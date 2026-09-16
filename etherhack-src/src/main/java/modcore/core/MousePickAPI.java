package modcore.core;

import se.krka.kahlua.integration.annotations.LuaMethod;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoObjectPicker;

/**
 * E2 (2026-09-14 八十八, 见 analysis/DLL分析/E-信息层-设计方案.md §E2): 渲染层拾取。
 *
 * 取证结论 (全部落在原版代码上, **不需要任何渲染补丁**):
 * ① 原版每帧已经算好"鼠标下的物件" —— zombie/ui/UIManager.java:567-571 调
 *    `IsoObjectPicker.Instance.ContextPick(mx,my)` 并缓存, 1225-1232 行把结果写进
 *    `UIManager.setLastPicked(IsoObject)` (getLastPicked 是 public static 且 UIManager
 *    在 Lua 暴露表内 → Lua 直接可读, 零成本);
 * ② 但 getLastPicked() 只在"鼠标不在任何 UI 元素上 + 拾取非空 + tooltip 非空"时才赋值
 *    (UIManager.java:1219/1232 否则写 null) —— 我方面板打开时鼠标必在面板上, 所以面板内
 *    取物件必须走本类兜底;
 * ③ ContextPick/各 Pick* 返回的 IsoObjectPicker.ClickObject **不在 Lua 暴露白名单**
 *    (LuaManager.java:2676 shouldExpose = 白名单成员), 而 Kahlua 只挂方法/静态字段,
 *    实例字段 (ClickObject.tile 等) 在 Lua 里读不到 → 需要这一个薄包装把结果解包成
 *    IsoObject (Lua 可用);
 * ④ 坐标契约: ContextPick 内部会再乘一次 zoom (IsoObjectPicker.java:147-148;
 *    FBORenderObjectPicker.java:58-60) → **必须传 raw 坐标** (getMouseX/getMouseY);
 * ⑤ 只读 getter, 不改任何字段、不注入、不发包 (零网络 → 无服务端可见状态, 无留痕)。
 */
public final class MousePickAPI {

    private MousePickAPI() {
    }

    /**
     * 通用拾取 (原版同一套渲染矩形 + sprite 遮罩命中 + 打分排序)。
     *
     * @param screenX raw 屏幕 X (Lua 侧 getMouseX())
     * @param screenY raw 屏幕 Y (Lua 侧 getMouseY())
     * @return 鼠标下的 IsoObject (原版拾取器打分最高的那个); 未命中/异常 → null
     */
    @LuaMethod(name = "pickObjectAt", global = true)
    public static IsoObject pickObjectAt(int screenX, int screenY) {
        try {
            IsoObjectPicker.ClickObject picked = IsoObjectPicker.Instance.ContextPick(screenX, screenY);
            return picked == null ? null : picked.tile;
        }
        catch (Throwable t) {
            return null;
        }
    }

    /**
     * 拾取信息 (供信息层显示; 中性串, 不做本地化 —— 文案在 Lua 侧拼)。
     *
     * @return "name|type|sprite|x|y|z|score"; 未命中/异常 → "" (空串)
     */
    @LuaMethod(name = "pickObjectInfoAt", global = true)
    public static String pickObjectInfoAt(int screenX, int screenY) {
        try {
            IsoObjectPicker.ClickObject picked = IsoObjectPicker.Instance.ContextPick(screenX, screenY);
            if (picked == null || picked.tile == null) {
                return "";
            }
            IsoObject object = picked.tile;
            IsoGridSquare square = picked.square;
            String sprite = object.getSprite() == null ? "" : safe(object.getSprite().getName());
            String name = safe(object.getObjectName());
            if (name.isEmpty()) {
                name = sprite;
            }
            if (name.isEmpty()) {
                name = object.getClass().getSimpleName();
            }
            int x = square == null ? 0 : square.getX();
            int y = square == null ? 0 : square.getY();
            int z = square == null ? 0 : square.getZ();
            return name + "|" + object.getClass().getSimpleName() + "|" + sprite
                    + "|" + x + "|" + y + "|" + z + "|" + picked.score;
        }
        catch (Throwable t) {
            return "";
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
