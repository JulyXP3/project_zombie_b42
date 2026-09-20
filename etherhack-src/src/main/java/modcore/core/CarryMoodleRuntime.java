/*
 * CarryMoodlePatch 的运行时被调用方 (ASM-free — 工程纪律第 8 条)。
 *
 * 语义 (一百三十三): 无限负重开启时, **本地玩家**的 HEAVY_LOAD 档位读取恒 0 ——
 * 图标不再出现, 且 `BodyDamage.Update:1970` 的背负过重扣血分支 (档≥3 才进) 永不成立。
 * 与 CarryWeightRuntime (分子清零) 构成双重保险; 详见 CarryMoodlePatch 文件头。
 */
package modcore.core;

public final class CarryMoodleRuntime {

    private CarryMoodleRuntime() {
    }

    /** true = 开关开 + 本地玩家的 Moodles 实例 + 类型是 HEAVY_LOAD → 读取点返回 0。 */
    public static boolean zero(Object moodles, Object type) {
        try {
            CoreMain main = CoreMain.getInstance();
            if (main == null || main.CoreAPI == null || !main.CoreAPI.isUnlimitedCarry) {
                return false;
            }
            if (type != zombie.scripting.objects.MoodleType.HEAVY_LOAD) {
                return false;
            }
            zombie.characters.IsoPlayer me = zombie.characters.IsoPlayer.getInstance();
            return me != null && moodles != null && moodles == me.getMoodles();
        }
        catch (Throwable t) {
            return false;   // 身份取不到/任何异常都不改变原版行为 (失败开放)
        }
    }
}
