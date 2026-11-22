package modcore.core;

/**
 * 万象天引落点计算 (纯函数, 不依赖任何游戏类, 便于离线验证)。
 *
 * 语义: 玩家面朝方向的正前方固定距离处的那一格。所有被吸的僵尸都去同一格,
 * 之后由游戏自身的体积碰撞把它们挤开 —— 用户明确要求"同格"而不是环形分散,
 * 所以这里不做避碰、不做间距分配。
 *
 * 方向为零长度时退回落点 = 玩家所在格, 避免除零产生 NaN 把僵尸丢到地图外。
 */
public final class ZombieGatherTarget {

    private ZombieGatherTarget() {
    }

    /**
     * @param px       玩家 X (浮点格坐标)
     * @param py       玩家 Y
     * @param dirX     面朝方向 X (无需预先归一化)
     * @param dirY     面朝方向 Y
     * @param distance 前方距离 (格), &gt;=0
     * @return {格X, 格Y}, 已按 floor 取整 (负数向负无穷取整)
     */
    public static int[] gatherTile(float px, float py, float dirX, float dirY, float distance) {
        float len = (float) Math.sqrt((double) (dirX * dirX + dirY * dirY));
        float tx = px;
        float ty = py;
        if (len > 1.0E-4f) {
            tx = px + dirX / len * distance;
            ty = py + dirY / len * distance;
        }
        return new int[]{(int) Math.floor((double) tx), (int) Math.floor((double) ty)};
    }
}
