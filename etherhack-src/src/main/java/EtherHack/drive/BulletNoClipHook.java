/*
 * 伪·自动驾驶运行期钩子集合 (与安装期注入器 BulletNoClipPatch 分离, 同
 * FullbrightHook 房规: 运行期类零 ASM 引用 — 安装器只解包 EtherHack 前缀条目,
 * ASM 库从不落地游戏目录, 运行期类一旦引用 ASM 类型即 NoClassDefFoundError,
 * 实测 AutoDriveController.refreshWorldNoClip → isActive → UI render 每帧报错)。
 *
 * 钩子 (手动驾驶完全原版; 激活期/宽限期语义见各方法):
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
import zombie.core.physics.Bullet;
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

    /** calcPhysics 头部钩子。返回 true = 已替换装配 (跳过原版), false = 放行原版。
     *  门控 = isPhysicsNoClip (激活期 + 退出接管后的碰撞恢复宽限期)。 */
    public static boolean onCalcPhysics(IsoChunk chunk, int x, int y, int z, int[] shapes) {
        if (!AutoDriveController.isPhysicsNoClip()) {
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

    // ============ ⑦ 植物碰撞 (树/灌木/草丛) ============

    /**
     * checkCollisionWithPlant 头部注入。返回 true = 整段跳过。
     * 原版: 车扫周围格子的树 (IsoTree)/灌木 (isBush)/草丛 tileset (d_generic_1/d_plants_1)
     * → applyImpulseFromHitPlant = 每帧反向冲量 -mul·质量·速度 (高速正面撞树 mul=0.1)。
     * 这是**非 Bullet 的逐格客户端逻辑** — 我们的 chunk 形状过滤 (①) 只管 Bullet 静态
     * 几何, 植物冲量照跑 → no-clip 穿树"像被黏住" (实测卅九); 建筑无此逻辑所以全速。
     * 跳过整段: 冲量与 scrape 音效全免, 与"静态物可穿"语义一致。
     */
    public static boolean onCheckCollisionWithPlant(BaseVehicle vehicle) {
        return AutoDriveController.isPhysicsNoClip();
    }

    // ============ ② 战损豁免 ============

    /**
     * calculateDamageWithCharacter 头部钩子 (车辆撞击角色的自伤计算)。
     * 返回 0 = 激活期零自伤 (总计为 0 时原版连 damageFromHitChr 命令都不发 →
     * 服务端零痕迹); 返回 Integer.MIN_VALUE = 放行原版 (注入体的哨兵值)。
     */
    public static int onVehicleHitChrDamage(BaseVehicle vehicle, IsoGameCharacter chr) {
        // 僵尸战损: 导航/秒杀开关/蠕动激活 期间全免 (蠕动防砸窗是本意, 卅六);
        // 其他角色 (玩家撞击等) 仅导航期间豁免。
        if (AutoDriveController.isZombieKillActive() && chr instanceof IsoZombie) {
            return 0;
        }
        return AutoDriveController.isWorldNoClip() ? 0 : Integer.MIN_VALUE;
    }

    /**
     * calculateDamageFromVehicleImpact / RunOver 共用头部钩子 (角色被撞伤害)。
     * 激活期且目标是僵尸 → 500 (首触即死); 其余 (玩家/动物/未激活) → NaN =
     * 放行原版 (注入体的哨兵值)。
     */
    public static float onZombieVehicleDamage(IsoGameCharacter chr, float impactSpeed) {
        if (AutoDriveController.isZombieKillActive() && chr instanceof IsoZombie) {
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
        return AutoDriveController.isZombieKillActive();
    }

    // ============ ⑥ 速度上限解锁 (CarSlowFactor 物体: 树/路灯/垃圾桶) ============

    /**
     * updateVelocityMultiplier 头部钩子。
     * 原版: 车辆碰撞带 CarSlowFactor 属性的物体 (树/路灯/垃圾桶/信箱/栅栏, breakingObjects
     * 检测) → breakingSlowFactor > 0 → updateVelocityMultiplier 把上限钳到
     * (34 − breakingSlowFactor) m/s (~36km/h), 穿这些物体"非常慢" (实测卌五)。
     * 注入器 (BulletNoClipPatch#installVelocityUnlock) 已埋入对本方法的调用, 但本方法长期缺失
     * → NoSuchMethodError 被 patch 的 try-catch 吞掉 → 原版限速照跑 (实测卌五)。
     *
     * 卌七: 原版末尾调 native `Bullet.setVehicleVelocityMultiplier(id, maxSpeed, multiplier)`
     * 且**每帧调用** — 原生侧 multiplier 是粘性状态。钩子若只 return true 跳过 = 停报,
     * 原生残留最后一次被钳值 → 限速照旧。必须**主动上报解锁值** (100000, 1.0 = 无上限)。
     * 返回 true = 已替换; false = 放行原版。
     */
    public static boolean onVelocityMultiplier(BaseVehicle vehicle) {
        if (!AutoDriveController.isPhysicsNoClip()) {
            return false;
        }
        Bullet.setVehicleVelocityMultiplier(vehicle.vehicleId, 100000.0f, 1.0f);
        return true;
    }

    // ============ ⑧ 物体碰撞冲量/急停 (CarSlowFactor + HitByCar) ============

    /**
     * IsoObject.Collision 头部钩子 (卌七补漏)。
     * 原版两分支都会拖慢穿行:
     * ① CarSlowFactor 分支 (路牌/垃圾桶/信箱/栅栏): applyImpulseFromHitObject =
     *    每触帧反向冲量 (mass·speed·factor), 不走速度上限, 限速解锁管不到;
     * ② HitByCar 分支: 低速擦碰时 `vehicle.setSpeedKmHour(0)` **直接急停**。
     * 返回 true = no-clip 期整段跳过 (碰撞报告/破损/冲量/急停全免); false = 放行原版。
     */
    public static boolean onObjectCollision(zombie.iso.IsoObject self, zombie.iso.IsoObject vehicle) {
        return AutoDriveController.isPhysicsNoClip();
    }

    // ============ 车辆重置 ============

    /**
     * 车辆重置 (「车辆重置」按钮): 把本车变换抬回正常行驶高度。
     * 嵌墙车在宽限到期后被原版贴地逻辑/静态碰撞挤进地里 (实测), 按钮兜底 —
     * x/z 不动, 仅重建 y (原生高度轴): 地面 z=0 → 原生 y≈0, 朝向保留。
     * 返回 true = 已重置 (Lua 提示成功)。
     */
    public static boolean resetVehiclePose(BaseVehicle self) {
        if (self == null || self.isRemovedFromWorld()) {
            return false;
        }
        zombie.core.physics.Transform t = BaseVehicle.allocTransform();
        self.getWorldTransform(t);
        t.origin.y = 0.0f;   // 地面行驶层 (update() 的 zi 换算: y/2.44949 ≈ 层 0)
        self.setWorldTransform(t);
        BaseVehicle.releaseTransform(t);
        return true;
    }
}
