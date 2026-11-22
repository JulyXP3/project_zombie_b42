package modcore.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * 万象天引落点: 玩家面朝方向前方 4 格的同一格 (纯计算, 不建僵尸/不碰世界)。
 *
 * 落点算错的表现是"僵尸被吸到侧后方 / 脚下 / 远处", 且所有僵尸共用同一格
 * 是本次用户明确要求的行为, 所以方向、归一化、退化输入与负坐标分别锁死。
 */
class ZombieGatherTargetTest {

    @Test
    void fourTilesAheadAlongTheFacingDirection() {
        assertArrayEquals(new int[]{104, 200},
                ZombieGatherTarget.gatherTile(100.5f, 200.5f, 1.0f, 0.0f, 4.0f));
    }

    @Test
    void facingNorthMovesTowardNegativeY() {
        assertArrayEquals(new int[]{100, 196},
                ZombieGatherTarget.gatherTile(100.5f, 200.5f, 0.0f, -1.0f, 4.0f));
    }

    @Test
    void diagonalDirectionIsNormalizedBeforeStepping() {
        float c = 0.70710677f;
        assertArrayEquals(new int[]{102, 202},
                ZombieGatherTarget.gatherTile(100.0f, 200.0f, c, c, 4.0f));
    }

    @Test
    void unnormalizedDirectionStillStepsExactlyFourTiles() {
        assertArrayEquals(new int[]{100, 204},
                ZombieGatherTarget.gatherTile(100.5f, 200.5f, 0.0f, 2.0f, 4.0f));
    }

    @Test
    void zeroLengthFacingFallsBackToThePlayerTileInsteadOfNaN() {
        assertArrayEquals(new int[]{100, 200},
                ZombieGatherTarget.gatherTile(100.5f, 200.5f, 0.0f, 0.0f, 4.0f));
    }

    @Test
    void negativeCoordinatesFloorInsteadOfTruncate() {
        assertArrayEquals(new int[]{-5, 0},
                ZombieGatherTarget.gatherTile(-0.5f, 0.5f, -1.0f, 0.0f, 4.0f));
    }
}
