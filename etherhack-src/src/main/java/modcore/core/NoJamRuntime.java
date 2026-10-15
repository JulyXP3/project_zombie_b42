/*
 * NoJamPatch 的运行时被调用方 (ASM-free — 工程纪律第 8 条)。
 *
 * 语义 (二百一十六, 用户拍板"做读取点补丁"): 「无卡壳」开启时, **本地玩家手持**的枪
 * 两个读点改值 —— getJamGunChance() 读到 0f、isJammed() 读到 false; 其余物品 (背包里
 * 未持的枪/他人武器) 读点走原版, 卡壳显示与解卡菜单不受影响。
 *
 * 为什么是读点而不是每帧踩值 (沿革见 NoJamPatch 文件头):
 * ①踩值只改客户端副本的字段, 而服务端**每次开枪**都会在自己副本上掷一次卡壳
 *   (命中包路径触发 OnWeaponSwingHitPoint → ISReloadWeaponAction.onShoot), 并立刻经
 *   syncHandWeaponFields (仅服务端能发, LuaManager:9687) 把 isJammed 广播回来;
 *   客户端收到后要等下一帧踩值才清掉 —— 存在 ≤1 帧窗口, 撞在窗口里的开枪会被
 *   canShoot 的 `weapon:isJammed()` 门吞掉 (偶发单发失效)。读点从根上吞掉一切来源的
 *   "已卡壳"态, 与字段被谁写入无关。
 * ②踩值还带每帧成本与"记账/还原"逻辑; 读点方案在字段写入路径上零改动 (不写任何字段),
 *   无需还原 (工程纪律第 4 条的关注点自然消失)。
 */
package modcore.core;

import zombie.characters.IsoPlayer;
import zombie.inventory.types.HandWeapon;

public final class NoJamRuntime {

    private NoJamRuntime() {
    }

    /** true = getJamGunChance() 读点改回 0f (原版两处掷骰门 getJamGunChance() > 0 随即短路)。 */
    public static boolean overrideChance(HandWeapon weapon) {
        return applies(weapon);
    }

    /** true = isJammed() 读点改回 false (开枪门/tooltip/解卡菜单/拉栓逻辑全覆盖)。 */
    public static boolean overrideJammed(HandWeapon weapon) {
        return applies(weapon);
    }

    private static boolean applies(HandWeapon weapon) {
        try {
            CoreMain main = CoreMain.getInstance();
            if (main == null || main.CoreAPI == null || !main.CoreAPI.isNoJam) {
                return false;
            }
            IsoPlayer self = IsoPlayer.getInstance();
            return self != null
                    && (weapon == self.getPrimaryHandItem() || weapon == self.getSecondaryHandItem());
        }
        catch (Throwable t) {
            return false;   // 取不到身份/任何异常都不改变原版行为 (失败开放)
        }
    }
}
