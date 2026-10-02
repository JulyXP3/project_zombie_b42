/*
 * MaxWeightPatch 的运行时被调用方 (ASM-free — 工程纪律第 8 条)。
 *
 * 沿革: 一百二十一 首建 (当时为查"掉血"误判) → 一百二十二 随掉血误判回退一并删除
 * (掉血与 carry 代码无关, 该案已结案冻结) → 一百九十一 因新症状**重建**:
 * 4a0e9546ec 起服务端经 PlayerDamagePacket (纯下行) 周期把 vanilla 计算的 maxWeight
 * 推回客户端无条件覆盖字段 (PlayerDamagePacket.parse:46), 叠加 vanilla BodyDamage
 * 自重算 (BodyDamage:1779-1785, 含 maxWeightDelta 复合), OnRenderTick 的字段踩值
 * 在竞速中存在可见回退窗口 (用户实测"时不时被服务端回退") —— 与耐力"图标闪一下"
 * 同构, 按项目原则"踩值被覆盖一律改读取点"根治。
 *
 * 语义: 「无限负重」开启时, **本地玩家**的 getMaxWeight() 恒返回 10000 ——
 * 与字段被谁写入 (服务端推回/BodyDamage 重算) 完全无关; 容量显示与放置判定零竞态。
 * 僵尸/远程玩家走原版 (身份判等不命中)。
 */
package modcore.core;

public final class MaxWeightRuntime {

    private MaxWeightRuntime() {
    }

    /** true = 无限负重开启 + 对象是本地玩家 → 读取点返回 10000。 */
    public static boolean override(Object chr) {
        try {
            CoreMain main = CoreMain.getInstance();
            if (main == null || main.CoreAPI == null || !main.CoreAPI.isUnlimitedCarry) {
                return false;
            }
            return chr != null && chr == zombie.characters.IsoPlayer.getInstance();
        }
        catch (Throwable t) {
            return false;   // 取不到身份/任何异常都不改变原版行为 (失败开放)
        }
    }
}
