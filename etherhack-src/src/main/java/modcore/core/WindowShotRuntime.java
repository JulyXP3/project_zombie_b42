/*
 * WindowShotPatch 的运行时被调用方 (ASM-free — 工程纪律第 8 条)。
 *
 * 语义 (二百一十七): 「关窗射击」开启且本机玩家手持远程武器 (HandWeapon.isRanged) 时,
 * IsoPlayer.updateWhileInVehicle() 内的车窗门控调用点读到 false = 「窗不可命中」→
 * canAttack = true → 关着完好车窗也能进车内攻击分支 (原版该分支只对"开窗/已碎"开路)。
 *
 * 作用域与边界:
 *   · 只改这一个调用点 (补丁只 redirect updateWhileInVehicle 里唯一一处 isHittable 调用),
 *     isHittable 本体与其余 5 个调用点 (僵尸砸窗/淋雨/车辆受击/ damage 自守卫) 零改动。
 *   · 限枪械 (isRanged): 不放开车内近战/推搡穿关窗 (本功能验收目标 = 用枪打僵尸)。
 *   · 其余一切情形 (开关关 / 未持枪 / 取不到身份 / 任何异常) 返回 window.isHittable() 原版值 =
 *     行为与原版逐位一致 (失败开放)。
 *   · MP: 该门控纯客户端行为; 是否放行弹道与是否伤自家车由原生弹道决定 (实测定论,
 *     研判 §4), 本钩子不产生任何网络包。
 */
package modcore.core;

import zombie.characters.IsoPlayer;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.HandWeapon;
import zombie.vehicles.VehicleWindow;

public final class WindowShotRuntime {

    private WindowShotRuntime() {
    }

    /** true/false = 替代 VehicleWindow.isHittable() 的读取值; false = 窗不可命中 → 可攻击。 */
    public static boolean overrideHittable(VehicleWindow window) {
        try {
            CoreMain main = CoreMain.getInstance();
            if (main != null && main.CoreAPI != null && main.CoreAPI.isShootThroughWindow
                    && holdsRangedWeapon(IsoPlayer.getInstance())) {
                return false;
            }
        }
        catch (Throwable t) {
            // 失败开放: 取不到身份/任何异常都不改变原版行为 (回退 isHittable 原值)
        }
        return window.isHittable();
    }

    private static boolean holdsRangedWeapon(IsoPlayer self) {
        if (self == null) {
            return false;
        }
        InventoryItem primary = self.getPrimaryHandItem();
        return primary instanceof HandWeapon && ((HandWeapon) primary).isRanged();
    }
}
