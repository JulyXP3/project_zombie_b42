/*
 * EnduranceStatPatch 的运行时被调用方 (ASM-free — 工程纪律第 8 条)。
 *
 * 语义 (一百三十九): 「无限耐力」开启时, **本机玩家**的 ENDURANCE 读取恒 1.0 ——
 * 无论游戏何时扣减、扣减多少, moodle/图标/冲刺判定/UI 条一律看到满耐力 (不再出现"图标闪一下")。
 * 保留 onTickUpdate 的踩值作兜底 (存值层面)。
 */
package modcore.core;

public final class EnduranceStatRuntime {

    private EnduranceStatRuntime() {
    }

    /** true = 开关开 + 本机玩家的 Stats + 类型 == ENDURANCE → 读取点返回 1.0f。 */
    public static boolean override(Object stats, Object stat) {
        try {
            CoreMain main = CoreMain.getInstance();
            if (main == null || main.CoreAPI == null) {
                return false;
            }
            if (!main.CoreAPI.isUnlimitedEndurance && !main.CoreAPI.isUnlimitedCondition) {
                return false;
            }
            if (stat != zombie.characters.CharacterStat.ENDURANCE) {
                return false;
            }
            zombie.characters.IsoPlayer me = zombie.characters.IsoPlayer.getInstance();
            return me != null && stats != null && stats == me.getStats();
        }
        catch (Throwable t) {
            return false;   // 取不到身份/任何异常都不改变原版行为 (失败开放)
        }
    }
}
