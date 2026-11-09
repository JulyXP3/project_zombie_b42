package modcore;

import modcore.core.WornSpawnAPI;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 穿戴生成: 食物临时压重的刻度换算。
 *
 * 服务端 hasRoomFor 只看背包重量是否超过容量, 所以压重必须落在
 * Food.getActualWeight() 的公式上 (b42.22 Food.java:777-820):
 *   usedDelta = 饥饿变化 * 100 / 脚本饥饿变化
 *   普通食物: 脚本重量 * usedDelta
 *   带 ReplaceOnUse: (脚本重量 - 空容器重量) * usedDelta + 空容器重量
 * 这里验证 initialChangeForTarget 给出的初值反算回去正好是目标重量。
 */
class WornSpawnStateTest {

    @Test
    void plainFoodDeltaReproducesTargetWeight() {
        float change = WornSpawnAPI.initialChangeForTarget(-16.0f, 0.2f, -1.0f, 60.0f);
        float usedDelta = change * 100.0f / -16.0f;
        assertEquals(60.0f, 0.2f * usedDelta, 0.001f);
    }

    @Test
    void replaceOnUseFoodDeltaAccountsForEmptyContainer() {
        float change = WornSpawnAPI.initialChangeForTarget(-20.0f, 0.8f, 0.1f, 60.0f);
        float usedDelta = change * 100.0f / -20.0f;
        assertEquals(60.0f, (0.8f - 0.1f) * usedDelta + 0.1f, 0.001f);
    }

    @Test
    void degenerateScriptValuesFallBackToSafeDelta() {
        // 脚本没有饥饿/口渴变化, 或脚本重量为 0: 给 -10 的安全初值, 后续靠实测放大。
        assertEquals(-10.0f, WornSpawnAPI.initialChangeForTarget(0.0f, 0.5f, -1.0f, 60.0f), 0.0001f);
        assertEquals(-10.0f, WornSpawnAPI.initialChangeForTarget(-10.0f, 0.0f, -1.0f, 60.0f), 0.0001f);
    }
}
