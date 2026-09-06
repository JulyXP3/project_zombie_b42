/*
 * 伪·自动驾驶运行期钩子集合 (与安装期注入器 BulletNoClipPatch 分离, 同
 * FullbrightHook 房规: 运行期类零 ASM 引用 — 安装器只解包 EtherHack 前缀条目,
 * ASM 库从不落地游戏目录, 运行期类一旦引用 ASM 类型即 NoClassDefFoundError,
 * 实测 AutoDriveController.refreshWorldNoClip → isActive → UI render 每帧报错)。
 *
 * 三组钩子 (全部 isWorldNoClip 激活态门控, 手动驾驶完全原版):
 * ① 世界碰撞豁免: onCalcPhysics — IsoChunk.calcPhysics 头部钩子, 激活期只保留
 *    Floor 形状; refreshChunks — 状态切换时重传已加载 chunk 物理 (即时生效)。
 * ② 战损豁免: onVehicleHitChrDamage — 车辆撞击角色自伤钳 0 (伤害报告不发出,
 *    服务端零痕迹); onZombieVehicleDamage — 僵尸被撞伤害 500 (碰着即死, 原版
 *    击杀/计数/布娃娃/同步全保留; instanceof IsoZombie 门控, 玩家被撞不受影响)。
 * ③ 冲量豁免: onHitPedestrianImpulse — 撞击减速冲量关 (尸群顶不停车, 优先到达)。
 *
 * B42 车辆物理为原生库 (PZBullet), Java 侧无碰撞掩码 API; 世界静态碰撞体的唯一
 * 装配点 = IsoChunk.calcPhysics (每格产出形状清单, 经 updatePhysicsForLevel →
 * Bullet.updateChunk 上行原生)。动态物体 (僵尸/其他车辆) 不在 chunk 形状里,
 * 照常碰撞; 步行/僵尸寻路走 MapCollisionData 不读这套原生形状, 不受影响。
 *
 * Floor 序号 6 = IsoChunk.PhysicsShapes 枚举声明序 (Solid0 WallN1 WallW2 WallS3
 * WallE4 Tree5 Floor6 ..., private 枚举不可引用, javap 亲验)。
 */
package EtherHack.drive;

import EtherHack.utils.Logger;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoZombie;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.vehicles.BaseVehicle;

public final class BulletNoClipHook {

    private static final int PHYSICS_SHAPE_FLOOR = 6; // PhysicsShapes.Floor 序号 (javap 亲验)
    private static final int MAX_LEVELS = 8;
    private static final float ZOMBIE_HIT_DAMAGE = 500.0f; // 僵尸被撞伤害 (血量个位数, 首触即死)

    private BulletNoClipHook() {
    }

    // ============ ① 世界碰撞豁免 ============

    /** calcPhysics 头部钩子。返回 true = 已替换装配 (跳过原版), false = 放行原版。 */
    public static boolean onCalcPhysics(IsoChunk chunk, int x, int y, int z, int[] shapes) {
        if (!AutoDriveController.isWorldNoClip()) {
            return false;
        }
        for (int i = 0; i < 4 && i < shapes.length; ++i) {
            shapes[i] = -1;
        }
        IsoGridSquare sq = chunk.getGridSquare(x, y, z);
        if (sq != null && sq.getProperties().has(IsoFlagType.solidfloor)) {
            shapes[0] = PHYSICS_SHAPE_FLOOR;
        }
        return true;
    }

    /** 状态切换时重传已加载 chunk 的物理 (激活=过滤版 / 退出=原版), 即时生效。 */
    public static void refreshChunks() {
        try {
            IsoCell cell = IsoWorld.instance.currentCell;
            if (cell == null) {
                return;
            }
            IsoChunkMap chunkMap = cell.getChunkMap(0);
            if (chunkMap == null) {
                return;
            }
            int width = IsoChunkMap.chunkGridWidth;
            int refreshed = 0;
            for (int cy = 0; cy < width; ++cy) {
                for (int cx = 0; cx < width; ++cx) {
                    IsoChunk chunk = chunkMap.getChunk(cx, cy);
                    if (chunk == null) continue;
                    for (int z = 0; z < MAX_LEVELS; ++z) {
                        if (chunk.getLevelData(z) == null) continue;
                        chunk.updatePhysicsForLevel(z);
                        ++refreshed;
                    }
                }
            }
            Logger.printLog("[AutoDrive] world no-clip refresh: " + refreshed + " chunk levels re-uploaded");
        } catch (Throwable t) {
            Logger.error("[AutoDrive] no-clip chunk refresh failed", t);
        }
    }

    // ============ ② 战损豁免 ============

    /**
     * calculateDamageWithCharacter 头部钩子 (车辆撞击角色的自伤计算)。
     * 返回 0 = 激活期零自伤 (总计为 0 时原版连 damageFromHitChr 命令都不发 →
     * 服务端零痕迹); 返回 Integer.MIN_VALUE = 放行原版 (注入体的哨兵值)。
     */
    public static int onVehicleHitChrDamage(BaseVehicle vehicle, IsoGameCharacter chr) {
        return AutoDriveController.isWorldNoClip() ? 0 : Integer.MIN_VALUE;
    }

    /**
     * calculateDamageFromVehicleImpact / RunOver 共用头部钩子 (角色被撞伤害)。
     * 激活期且目标是僵尸 → 500 (首触即死); 其余 (玩家/动物/未激活) → NaN =
     * 放行原版 (注入体的哨兵值)。
     */
    public static float onZombieVehicleDamage(IsoGameCharacter chr, float impactSpeed) {
        if (AutoDriveController.isWorldNoClip() && chr instanceof IsoZombie) {
            return ZOMBIE_HIT_DAMAGE;
        }
        return Float.NaN;
    }

    // ============ ③ 冲量豁免 ============

    /**
     * applyImpulseFromHitPedestrian 头部钩子 (撞击减速冲量)。
     * 返回 true = 激活期跳过冲量 (尸群顶不停车); false = 放行原版。
     */
    public static boolean onHitPedestrianImpulse(BaseVehicle vehicle, IsoGameCharacter chr) {
        return AutoDriveController.isWorldNoClip();
    }
}
