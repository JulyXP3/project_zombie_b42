package modcore.core;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import se.krka.kahlua.integration.annotations.LuaMethod;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.physics.Bullet;
import zombie.core.physics.WorldSimulation;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehicleManager;
import modcore.utils.Logger;

/**
 * C3 传送原语 (2026-09-14 九十一 探针, 九十四 v3 加固; 见 analysis/DLL分析/C-传送与载具-设计方案(C1-C3已实施).md §C3):
 * 载具原生体随动 —— 探针读数 / 目的地判定 / 跳步 / 落位校验 / 回滚。
 *
 * 依据 (取证):
 *  - 客户端权威: 载具位置由司机客户端算并上行 (BaseVehicle.java:3109-3111, 4231-4246),
 *    服务端 VehiclePhysicsPacket.processServer:148-175 盲采、零坐标校验。
 *  - 挪车唯一正当通道 = 改本地物理态让常规 VehiclePhysics 流自动上报; 只改 setX/setY
 *    会被物理步每帧覆盖 (BaseVehicle.java:3199-3201), 必须写原生物理体:
 *    v1 的 setWorldTransform→Bullet.teleportVehicle 实测只维持不到 1 秒 (被打回),
 *    v2 = Bullet.setOwnVehiclePhysics (游戏"强制落位"用的原语) + 同帧对齐 jniTransform
 *    + 目的地格已加载时 VehicleManager.clientUpdateVehiclePos 做逻辑/格/区块簿记。
 *  - 原生体是否真随动: 物理步会从原生读回并覆盖 jniTransform
 *    (WorldSimulation.updateVehiclePhysics:187), 因此三源对照 (逻辑 / Bullet.getOwnVehiclePhysics
 *    / jniTransform.origin) 交叉验证。数组布局见 VehiclePhysicsPacket.set:40-58
 *    ([0..2] = 世界 x/y/z, [3..6] = 四元数, ...)。
 *
 * v3 加固 (2026-09-14 九十四, 用户 v2 实测日志驱动):
 *  ① **先验目的地再动手**: v2 实测中"目的地未加载"那一跳 (ok-no-square) 是把车**先扔出去、
 *     后才发现没格** → 原生体下面没有地面几何, 整车自由落体 (z 一路 -6 → -551, 约 20 秒),
 *     只有手动「车辆重置」才拉回来。现改为: 目的地区块 + 目的地格都通过判定后才写任何状态,
 *     否则原样返回 blocked-* (车一动不动)。
 *  ② **跳前存档 + 落位校验 + 回滚**: 每跳前把整个物理数组存档, 落位后由 Lua 侧观察一个
 *     短窗口再调 verify (逻辑漂移 / 原生漂移 / 下坠), 不合格立即 rollback 回存档位姿。
 *  ③ **可观测性**: 读数串追加玩家逻辑坐标与区块图中心 (排查"目的地没加载"到底是玩家没跟随,
 *     还是流式加载没跟上 —— 区块图以玩家为中心, 只有玩家真的跟过去才会加载新格)。
 *
 * 一百零九 层号修正 (2026-09-15, 用户实测: "汽车在水上动不了, 传送不出去"):
 *   用户日志五条全是 `aborted: dest-unloaded - no-square` / `aborted: input`, **零条 hop** ⇒ 卡在
 *   目的地就绪门 (阶梯全档位不就绪), `nativeHop` 从未被调用。真因 = 目的地判定把**原生物理 z**
 *   当**楼层**传给了 `getGridSquare(x, y, z)`:
 *   ① **单位错** — 该参数是楼层 (内部 `PZMath.fastfloor`), 而 `PHYSICS[2]` 是物理单位 (1 楼层 =
 *      2.44949; vanilla 自己也要 `origin.y / 2.44949f` 才得到楼层, BaseVehicle.java:3202)。
 *   ② **语义错 (本次真凶)** — 车落在水面以下时物理 z 为**负** → `fastfloor` = **-1**, 而陆地/水面
 *      区块 `minLevel = 0` (只有含地下室的区块才是负层), `IsoChunk.getGridSquare:2909` 的
 *      `worldSquareZ < this.minLevel` 让**任何**目的地在**任何**档位都返回 `no-square`
 *      ⇒ 车永远传送不出水域 (且重试多少次都一样)。
 *   权威来源是 vanilla 自己维护的**逻辑层** `vehicle.getZ()` (`BaseVehicle.update:3199-3207`:
 *   `zi = fastfloor(physZ / 2.44949 + 0.05)`, 且**仅当**该层或其下一层有 floor 时才 `setZ(zi)`,
 *   否则留在 `0.0f` ⇒ 水里的车逻辑层正是水面层 0)。现统一: 目的地判定 / 逻辑格查找一律用
 *   `levelFor()`, **抬高基准**改为 `max(zOrig, lvl * 2.44949)` —— 从水里抬高若仍以沉底的 z 为基准,
 *   落点会低于陆地地面 (又变回"嵌进地形"的老毛病), 以逻辑层为基准才保证"始终从目的地地面
 *   +1.5 楼层落下"。陆地/建筑上层上两种写法等价 (`zOrig ≈ lvl * 2.44949`), 属零行为变化;
 *   附带修掉"层 ≥1 的车查错层"的旧隐患 (旧写法 `fastfloor(2.44949)` = 2 ≠ 楼层 1)。
 *
 * 本类提供位姿原语与传送/重置共用落地观察；路线分步、加载等待、位移预算仍由 Lua UIMap 管理。
 */
public final class VehicleTeleportAPI {

    /** 1 楼层对应的物理单位 (vanilla 常量, 见 BaseVehicle.java:1831 / :3202 / :2670)。 */
    private static final float PHYSICS_UNITS_PER_LEVEL = 2.44949f;

    private static final float[] PHYSICS = new float[27];
    /** 写体后的回读缓冲 (不覆盖 PHYSICS, 否则 jni 对齐会用到被回读覆盖的值)。 */
    private static final float[] CHECK = new float[27];
    /** 上一跳前的物理数组存档 (回滚用, 只保一份: 同时只有一辆车在跳)。 */
    private static final float[] SAVED = new float[27];
    private static boolean savedValid = false;
    private static BaseVehicle savedVehicle;
    private static int savedVehicleId = -1;
    private static int savedLevel;
    private static boolean savedPhysicsActive;
    private static boolean restorePending;
    private static boolean restoreWriteAccepted;
    private static boolean restoreNativeConfirmed;
    private static long restoreDeadlineMs;
    private static int restoreFrame;
    private static String restoreResult = "pending-restore";
    private static final long RESTORE_CONFIRM_MS = 1000L;
    /** 本地驾驶车辆独占观察窗；传送与重置不并行占用。 */
    private static final VehicleLandingState LANDING = new VehicleLandingState();
    private static BaseVehicle landingVehicle;
    private static int landingFrame;
    private static String landingResult = "settling";
    private static final float POSE_POSITION_EPS = 0.15f;
    private static final float POSE_PLANE_EPS = 0.5f;
    /** 位姿与乘车坐标共用缓冲，不在每帧分配。 */
    private static final Quaternionf POSE_QUAT = new Quaternionf();
    private static final Quaternionf READ_QUAT = new Quaternionf();
    private static final Vector3f PASSENGER_POS = new Vector3f();

    private VehicleTeleportAPI() {
    }

    /**
     * 载具**逻辑层** (楼层数) —— 目的地判定与抬高基准的唯一权威来源 (一百零九)。
     *
     * vanilla 在 `BaseVehicle.update:3199-3207` 每物理帧维护它: `setZ(0.0f)` → 由物理 z 反推
     * `zi = fastfloor(physZ / 2.44949 + 0.05)` → **仅当**该层或其下一层有 floor 时才 `setZ(zi)`,
     * 否则留在 `0.0f`。所以"车在水面以下 (水面对物理是空的, IsoChunk.calcPhysics 不为水格生成形状)"
     * 这类情况下逻辑层 = 水面层 (通常 0), 而原生物理 z 是负的 —— 两者不可混用。
     * 读不到 (NaN/异常) 时退回 0。
     */
    private static int levelFor(BaseVehicle vehicle) {
        try {
            float z = vehicle.getZ();
            return Float.isNaN(z) ? 0 : (int)Math.floor(z);
        }
        catch (Throwable t) {
            return 0;
        }
    }

    /** 不改变授权；只接受仍在本地驾驶位、实际由本机模拟的车辆。 */
    public static String localSimulationState(BaseVehicle vehicle) {
        if (vehicle == null || vehicle.isRemovedFromWorld() || IsoWorld.instance == null
            || IsoWorld.instance.currentCell == null || vehicle.getCell() != IsoWorld.instance.currentCell) {
            return "no vehicle";
        }
        IsoPlayer player = IsoPlayer.getInstance();
        if (player == null || player.isDead() || player.getVehicle() != vehicle || !vehicle.isDriver(player)) {
            return "no-driver";
        }
        if (!vehicle.isLocalPhysicSim()) {
            return "not-local";
        }
        if (vehicle.getVehicleTowing() != null || vehicle.getVehicleTowedBy() != null) {
            return "towing-linked";
        }
        if (vehicle.isWaitingToReattachTowingPartner()) {
            return "waiting-towing";
        }
        return "ok";
    }

    /** 传送前置状态：未获本机模拟且引擎未转时，明确指向“先启动车辆”。 */
    public static String teleportSimulationState(BaseVehicle vehicle) {
        String state = localSimulationState(vehicle);
        if ("not-local".equals(state)) {
            try {
                if (!vehicle.isEngineRunning()) {
                    return "engine-off";
                }
            }
            catch (Throwable t) {
                return state;
            }
        }
        return state;
    }

    /** 原版区块检查清掉过时禁用标记，再有界尝试开启；不在观察中持续强开。 */
    public static String activateLocalPhysics(BaseVehicle vehicle) {
        String state = localSimulationState(vehicle);
        if (!"ok".equals(state)) {
            return state;
        }
        if (!chunkRingLoaded(IsoWorld.instance, vehicle.getX(), vehicle.getY())) {
            return "chunk-ring-not-loaded";
        }
        vehicle.checkSurroundingChunks();
        vehicle.setPhysicsActive(true);
        if (vehicle.isPhysicsActive() && (!vehicle.isActive || vehicle.isStatic)) {
            vehicle.setPhysicsActive(false);
            vehicle.setPhysicsActive(true);
        }
        return vehicle.isPhysicsActive() && vehicle.isActive && !vehicle.isStatic ? "ok" : "physics-off";
    }

    @LuaMethod(name = "vehicleNativeLandingBegin", global = true)
    public static boolean nativeLandingBegin(BaseVehicle vehicle) {
        try {
            if (!"ok".equals(localSimulationState(vehicle)) || WorldSimulation.instance == null
                || landingVehicle != null && landingVehicle != vehicle) {
                return false;
            }
            int frame = WorldSimulation.instance.getBulletFrameNo();
            LANDING.begin(System.nanoTime() / 1_000_000L);
            landingVehicle = vehicle;
            landingFrame = frame;
            landingResult = "settling";
            return true;
        }
        catch (Throwable t) {
            return false;
        }
    }

    @LuaMethod(name = "vehicleNativeLandingEnd", global = true)
    public static void nativeLandingEnd(BaseVehicle vehicle) {
        if (landingVehicle == vehicle) {
            landingVehicle = null;
        }
    }

    public static boolean landingInProgress() {
        return landingVehicle != null;
    }

    /** 已失败跳步的保存态只允许同一辆车重试恢复，不用新跳覆盖。 */
    public static boolean hasSavedPose(BaseVehicle vehicle) {
        return savedValid && savedVehicle == vehicle && savedVehicleId == vehicle.vehicleId;
    }

    /** 世界切换释放旧引用；失败回滚只在有时限的新物理帧上只读确认。 */
    public static void discardRemovedVehicleState() {
        if (savedVehicle != null && (savedVehicle.isRemovedFromWorld()
            || IsoWorld.instance == null || savedVehicle.getCell() != IsoWorld.instance.currentCell)) {
            clearSaved(savedVehicle);
        }
        if (landingVehicle != null && (landingVehicle.isRemovedFromWorld()
            || IsoWorld.instance == null || landingVehicle.getCell() != IsoWorld.instance.currentCell)) {
            landingVehicle = null;
        }
        if (savedValid && restorePending) {
            long now = System.nanoTime() / 1_000_000L;
            if (now >= restoreDeadlineMs) {
                restoreResult = "restore-timeout";
            } else {
                confirmPendingRestore(savedVehicle, true);
            }
        }
    }

    /** 原生高度 + 垂直速度 + 支撑读数 + 三源一致性，连续观察后才返回 ok。 */
    @LuaMethod(name = "vehicleNativeLandingPoll", global = true)
    public static String nativeLandingPoll(BaseVehicle vehicle) {
        if (landingVehicle != vehicle || vehicle == null) {
            return "no-observation";
        }
        long now = System.nanoTime() / 1_000_000L;
        try {
            String state = localSimulationState(vehicle);
            if (!"ok".equals(state)) {
                return landingResult = LANDING.waiting(now, state);
            }
            if (Bullet.getOwnVehiclePhysics(vehicle.vehicleId, PHYSICS) != 0) {
                return landingResult = LANDING.waiting(now, "no physics");
            }
            if (!chunkRingLoaded(IsoWorld.instance, PHYSICS[0], PHYSICS[1])) {
                return landingResult = LANDING.waiting(now, "chunk-ring-not-loaded");
            }
            if (!vehicle.isPhysicsActive() || !vehicle.isActive || vehicle.isStatic) {
                return landingResult = LANDING.waiting(now, "physics-off");
            }
            int frame = WorldSimulation.instance.getBulletFrameNo();
            if (frame == landingFrame) {
                return landingResult = LANDING.awaitingSample(now);
            }
            landingFrame = frame;
            float up = vehicle.getUpVectorDot();
            if (!Float.isFinite(up)) {
                return landingResult = LANDING.waiting(now, "no-pose");
            }
            boolean flipped = up < BaseVehicle.MINIMUM_DOT_UPRIGHT;
            // 车身碰撞只可触发翻车扶正，不能单独证明正常姿态已接地。
            String result = LANDING.sample(now, PHYSICS[2], PHYSICS[8],
                hasSupport(vehicle, PHYSICS) || flipped && vehicle.jniIsCollide,
                "ok".equals(sourceState(vehicle, PHYSICS)));
            landingResult = "ok".equals(result) && flipped ? "flipped" : result;
            return landingResult;
        }
        catch (Throwable t) {
            return landingResult = LANDING.waiting(now, "error");
        }
    }

    /** 无可读接地布尔字段；悬架压缩及原版近地判据必须与新物理帧/收敛一起使用。 */
    private static boolean hasSupport(BaseVehicle vehicle, float[] pose) {
        int wheels = Math.min(Math.max(0, wheelCount(pose[10])), vehicle.wheelInfo.length);
        float rest = vehicle.getScript().getSuspensionRestLength();
        if (wheels > 0 && Float.isFinite(rest) && rest > 0.0f) {
            for (int i = 0; i < wheels; ++i) {
                float length = pose[14 + i * 4];
                float displayed = vehicle.wheelInfo[i].suspensionLength;
                if (Float.isFinite(length) && Float.isFinite(displayed)
                    && Math.abs(length - displayed) <= 0.05f && length > 0.0f && length < rest - 0.01f) {
                    return true;
                }
            }
        }
        IsoGridSquare sq = vehicle.getSquare();
        return sq != null && sq.hasFloor()
            && Math.abs(pose[2] - sq.z * PHYSICS_UNITS_PER_LEVEL) <= 0.2f * PHYSICS_UNITS_PER_LEVEL;
    }

    private static boolean rotationMatches(float[] pose, Quaternionf rotation) {
        float norm = pose[3] * pose[3] + pose[4] * pose[4] + pose[5] * pose[5] + pose[6] * pose[6];
        float otherNorm = rotation.lengthSquared();
        if (!Float.isFinite(norm) || !Float.isFinite(otherNorm) || norm < 0.5f || otherNorm < 0.5f) {
            return false;
        }
        float dot = pose[3] * rotation.x + pose[4] * rotation.y + pose[5] * rotation.z + pose[6] * rotation.w;
        return Math.abs(dot) / (float)Math.sqrt(norm * otherNorm) >= 0.999f;
    }

    /** 原生回读为 N+0.1，原版写体/序列化按截断取轮数，不要求小数部分为零。 */
    private static int wheelCount(float encoded) {
        return Float.isFinite(encoded) && encoded >= 0.0f && encoded < 5.0f ? (int)encoded : -1;
    }

    private static boolean poseMatches(float[] expected, float[] actual) {
        for (int i = 0; i < 3; ++i) {
            if (!Float.isFinite(actual[i]) || !Float.isFinite(expected[i])
                || Math.abs(actual[i] - expected[i]) > POSE_POSITION_EPS) {
                return false;
            }
        }
        READ_QUAT.set(actual[3], actual[4], actual[5], actual[6]);
        int wheels = wheelCount(expected[10]);
        if (!rotationMatches(expected, READ_QUAT) || wheels < 0 || wheelCount(actual[10]) != wheels) {
            return false;
        }
        for (int i = 7; i < 10; ++i) {
            if (!Float.isFinite(actual[i]) || !Float.isFinite(expected[i])
                || Math.abs(actual[i] - expected[i]) > 0.1f) {
                return false;
            }
        }
        return true;
    }

    /** 悬架是接触派生值，写体回读可能立即重算；恢复姿态但使用最新轮系读数。 */
    private static void copyWheelState(float[] target, float[] source) {
        int wheels = Math.max(0, wheelCount(source[10]));
        System.arraycopy(source, 11, target, 11, wheels * 4);
    }

    private static String sourceState(BaseVehicle vehicle, float[] pose) {
        float x = vehicle.jniTransform.origin.x + WorldSimulation.instance.offsetX;
        float y = vehicle.jniTransform.origin.z + WorldSimulation.instance.offsetY;
        float z = vehicle.jniTransform.origin.y;
        if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)
            || !Float.isFinite(pose[0]) || !Float.isFinite(pose[1]) || !Float.isFinite(pose[2])
            || Math.abs(x - pose[0]) > POSE_POSITION_EPS || Math.abs(y - pose[1]) > POSE_POSITION_EPS
            || Math.abs(z - pose[2]) > POSE_POSITION_EPS
            || !rotationMatches(pose, vehicle.jniTransform.getRotation(READ_QUAT))) {
            return "jni-drift";
        }
        if (!Float.isFinite(vehicle.getX()) || !Float.isFinite(vehicle.getY())
            || Math.abs(vehicle.getX() - pose[0]) > POSE_PLANE_EPS
            || Math.abs(vehicle.getY() - pose[1]) > POSE_PLANE_EPS) {
            return "logic-drift";
        }
        IsoGridSquare sq = vehicle.getSquare();
        if (sq == null || sq.chunk == null || vehicle.getCurrentSquare() != sq || vehicle.chunk != sq.chunk
            || !vehicle.chunk.vehicles.contains(vehicle)) {
            return "bookkeeping-drift";
        }
        for (int seat = 0; seat < vehicle.getMaxPassengers(); ++seat) {
            IsoGameCharacter chr = vehicle.getCharacter(seat);
            if (!(chr instanceof IsoPlayer player) || !player.isLocalPlayer() || player.getVehicle() != vehicle) {
                continue;
            }
            Vector3f pos = vehicle.getPassengerWorldPos(seat, PASSENGER_POS);
            if (pos == null || !Float.isFinite(pos.x) || !Float.isFinite(pos.y) || !Float.isFinite(pos.z)
                || !Float.isFinite(player.getX()) || !Float.isFinite(player.getY()) || !Float.isFinite(player.getZ())
                || Math.abs(player.getX() - pos.x) > POSE_PLANE_EPS
                || Math.abs(player.getY() - pos.y) > POSE_PLANE_EPS || Math.abs(player.getZ() - pos.z) > 0.1f) {
                return "passenger-drift";
            }
        }
        return "ok";
    }

    /** 顺序与原版 vehiclePosUpdate / BaseVehicle.update 乘车簿记一致，只更新本机乘员。 */
    private static void applyPose(BaseVehicle vehicle, float[] pose, IsoGridSquare sq, int level) {
        vehicle.jniTransform.origin.set(pose[0] - WorldSimulation.instance.offsetX, pose[2],
            pose[1] - WorldSimulation.instance.offsetY);
        POSE_QUAT.set(pose[3], pose[4], pose[5], pose[6]).normalize();
        vehicle.jniTransform.setRotation(POSE_QUAT);
        vehicle.savedRot.set(POSE_QUAT);
        vehicle.jniLinearVelocity.set(pose[7], pose[8], pose[9]);
        vehicle.jniIsCollide = false;
        int wheels = Math.min(Math.max(0, wheelCount(pose[10])), vehicle.wheelInfo.length);
        for (int i = 0; i < wheels; ++i) {
            vehicle.wheelInfo[i].steering = pose[11 + i * 4];
            vehicle.wheelInfo[i].rotation = pose[12 + i * 4];
            vehicle.wheelInfo[i].skidInfo = pose[13 + i * 4];
            vehicle.wheelInfo[i].suspensionLength = pose[14 + i * 4];
        }
        VehicleManager.instance.clientUpdateVehiclePos(vehicle, pose[0], pose[1], pose[2], sq);
        vehicle.setZ(level);
        for (int seat = 0; seat < vehicle.getMaxPassengers(); ++seat) {
            IsoGameCharacter chr = vehicle.getCharacter(seat);
            if (!(chr instanceof IsoPlayer player) || !player.isLocalPlayer() || player.getVehicle() != vehicle) {
                continue;
            }
            Vector3f pos = vehicle.getPassengerWorldPos(seat, PASSENGER_POS);
            if (pos != null) {
                player.setX(pos.x);
                player.setY(pos.y);
                player.setZ(pos.z);
                player.setCurrentSquareFromPosition();
            }
        }
        vehicle.polyDirty = true;
    }

    private static void clearSaved(BaseVehicle vehicle) {
        if (savedVehicle == vehicle) {
            savedValid = false;
            savedVehicle = null;
            savedVehicleId = -1;
            restorePending = false;
            restoreWriteAccepted = false;
            restoreNativeConfirmed = false;
            restoreResult = "pending-restore";
        }
    }

    /** 不重写原生体；先见到旧位姿，才允许以随后新帧的完整一致状态结束恢复事务。 */
    private static String confirmPendingRestore(BaseVehicle vehicle, boolean freshFrameOnly) {
        if (!hasSavedPose(vehicle) || !restorePending) {
            return "pending-restore";
        }
        long now = System.nanoTime() / 1_000_000L;
        String pending = now >= restoreDeadlineMs ? "restore-timeout" : "pending-restore";
        try {
            if (!"ok".equals(localSimulationState(vehicle)) || WorldSimulation.instance == null) {
                return restoreResult = pending;
            }
            int frame = WorldSimulation.instance.getBulletFrameNo();
            boolean newFrame = frame != restoreFrame;
            if (freshFrameOnly && !newFrame) {
                return restoreResult = pending;
            }
            restoreFrame = frame;
            if (Bullet.getOwnVehiclePhysics(vehicle.vehicleId, CHECK) != 0) {
                return restoreResult = pending;
            }
            boolean atStart = poseMatches(SAVED, CHECK);
            if (atStart) {
                restoreNativeConfirmed = true;
            }
            if (!restoreNativeConfirmed || !atStart && (!restoreWriteAccepted || !newFrame)
                || wheelCount(CHECK[10]) != wheelCount(SAVED[10])) {
                return restoreResult = pending;
            }
            for (int i = 7; i < 10; ++i) {
                if (!Float.isFinite(CHECK[i])) {
                    return restoreResult = pending;
                }
            }
            if (!"ok".equals(sourceState(vehicle, CHECK))
                || atStart && vehicle.getZ() != savedLevel) {
                return restoreResult = pending;
            }
            clearSaved(vehicle);
            Logger.printLog("[VehTeleport] rollback confirmed, saved pose released");
            return "ok";
        }
        catch (Throwable t) {
            return restoreResult = pending;
        }
    }

    @LuaMethod(name = "vehicleNativeCommit", global = true)
    public static boolean nativeCommit(BaseVehicle vehicle) {
        if (vehicle == null || landingVehicle != vehicle || !"ok".equals(landingResult)) {
            return false;
        }
        clearSaved(vehicle);
        nativeLandingEnd(vehicle);
        return true;
    }

    /**
     * 读三源坐标 (世界坐标): 逻辑 x/y/z、原生物理体读数、jniTransform.origin (换算回世界),
     * 附控制器物理开关、玩家逻辑坐标、区块图中心与尺寸。纯读取, 无副作用。
     */
    @LuaMethod(name = "vehicleNativeProbeRead", global = true)
    public static String nativeProbeRead(BaseVehicle vehicle) {
        if (vehicle == null) {
            return "no vehicle";
        }
        float offsetX = WorldSimulation.instance.offsetX;
        float offsetY = WorldSimulation.instance.offsetY;
        float px = Float.NaN, py = Float.NaN, pz = Float.NaN, vy = Float.NaN;
        try {
            if (Bullet.getOwnVehiclePhysics(vehicle.vehicleId, PHYSICS) == 0) {
                px = PHYSICS[0];
                py = PHYSICS[1];
                pz = PHYSICS[2];
                vy = PHYSICS[8];
            }
        }
        catch (Throwable t) {
            // 原生体未就绪 → 保持 NaN (探针只打印, 不上报)
        }
        String ctrl;
        try {
            ctrl = vehicle.getController() != null && vehicle.getController().isEnable ? "on" : "off";
        }
        catch (Throwable t) {
            ctrl = "?";
        }
        // 玩家逻辑坐标 + 区块图中心 (九十四): 区块流式加载以玩家为中心, 目的地没加载时
        // 先看这两项 —— 玩家没跟过去 (中心没动) 与加载没跟上 (中心动了但格还没来) 处理方式不同。
        String playerPart = "player=n/a";
        String chunkPart = "chunk=n/a";
        try {
            IsoPlayer p = IsoPlayer.getInstance();
            if (p != null) {
                playerPart = String.format("player=(%.2f,%.2f)", p.getX(), p.getY());
            }
            IsoChunkMap cm = IsoWorld.instance.currentCell.getChunkMap(0);
            if (cm != null) {
                chunkPart = String.format("chunk=(%d,%d,w%d)", cm.worldX, cm.worldY, IsoChunkMap.chunkGridWidth);
            }
        }
        catch (Throwable t) {
            // 探针容错: 任何一项读不到都不影响其余读数
        }
        // 一百零九: 两个"层"并排打 —— 逻辑层 (目的地判定/抬高基准用它) 与"物理 z 反推层"
        // (旧写法用它, 车在水面以下时 = -1)。二者不一致就是"传送卡在 no-square"的现场。
        String physLvlPart = Float.isNaN(pz) ? "physLvl=n/a"
            : String.format("physLvl=%d", (int)Math.floor(pz / PHYSICS_UNITS_PER_LEVEL + 0.05f));
        String simulationPart;
        try {
            simulationPart = String.format("auth=%s local=%s ring=%s towingWait=%s vy=%.3f support=%s",
                vehicle.getAuthorizationDescription(), vehicle.isLocalPhysicSim(),
                chunkRingLoaded(IsoWorld.instance, Float.isFinite(px) ? px : vehicle.getX(),
                    Float.isFinite(py) ? py : vehicle.getY()), vehicle.isWaitingToReattachTowingPartner(), vy,
                Float.isFinite(pz) && hasSupport(vehicle, PHYSICS));
        }
        catch (Throwable t) {
            simulationPart = "simulation=n/a";
        }
        return String.format(
            "logic=(%.2f,%.2f,%.2f) phys=(%.2f,%.2f,%.2f) jni=(%.2f,%.2f,%.2f) ctrl=%s lvl=%d %s %s %s %s",
            vehicle.getX(), vehicle.getY(), vehicle.getZ(),
            px, py, pz,
            vehicle.jniTransform.origin.x + offsetX,
            vehicle.jniTransform.origin.z + offsetY,
            vehicle.jniTransform.origin.y,
            ctrl, levelFor(vehicle), physLvlPart, playerPart, chunkPart, simulationPart);
    }

    /**
     * 目的地就绪判定 (无副作用, 不写任何状态): 给定相对位移后的目标点是否可安全落位。
     * 返回值: "ok" / "no-chunk" (目标区块不在当前区块图内) / "chunk-not-loaded" (区块对象在图上
     * 但地形数据还没流出 —— 一百 追加, 见 destState 内注释) / "no-square@lvl&lt;层&gt;" /
     * "no-cell" / "no-chunk-map" / "no physics" / "no vehicle"。
     *
     * 一百零九: **层号取载具逻辑层** (`levelFor`) —— 旧实现传原生物理 z (物理单位, 且车在水面
     * 以下时为负) 会被 `getGridSquare` 当楼层用 ⇒ 一律 no-square, 车传送不出水域。
     */
    @LuaMethod(name = "vehicleNativeDestReady", global = true)
    public static String nativeDestReady(BaseVehicle vehicle, float dx, float dy) {
        if (vehicle == null) {
            return "no vehicle";
        }
        try {
            String state = teleportSimulationState(vehicle);
            if (!"ok".equals(state)) {
                return state;
            }
            if (Bullet.getOwnVehiclePhysics(vehicle.vehicleId, PHYSICS) != 0) {
                return "no physics";
            }
            return destState(PHYSICS[0] + dx, PHYSICS[1] + dy, levelFor(vehicle));
        }
        catch (Throwable t) {
            return "error";
        }
    }

    /**
     * 轻量高度读数 (一百): 只返回原生物理体的世界 z, 零副作用、零分配。
     * 落位判定需要**逐帧**看高度变化才能区分"真下坠"与"正常落位后回稳"
     * (probeRead 返回的是格式化串, 每帧解析既贵又脆)。读不到时返回 NaN。
     */
    @LuaMethod(name = "vehicleNativePhysicsZ", global = true)
    public static float nativePhysicsZ(BaseVehicle vehicle) {
        if (vehicle == null) {
            return Float.NaN;
        }
        try {
            if (Bullet.getOwnVehiclePhysics(vehicle.vehicleId, PHYSICS) != 0) {
                return Float.NaN;
            }
            return PHYSICS[2];
        }
        catch (Throwable t) {
            return Float.NaN;
        }
    }

    /**
     * 物理模拟是否在跑 (三百三十)。`setPhysicsActive` 在 3×3 区块环不齐时会被
     * `disableSimulationDueToLackOfSurroundingChunks` 强制降为 false, 车这时不会下落 ——
     * Lua 侧据此把"高度从未变化"判为冻结而不是落地。
     */
    @LuaMethod(name = "vehicleNativePhysicsActive", global = true)
    public static boolean nativePhysicsActive(BaseVehicle vehicle) {
        if (vehicle == null) {
            return false;
        }
        try {
            return vehicle.isPhysicsActive();
        }
        catch (Throwable t) {
            return false;
        }
    }

    /**
     * 目的地 3×3 区块环是否全部就绪 —— 复刻 vanilla `BaseVehicle.hasChunksAllAround:4294-4307` 的语义。
     *
     * **为什么单块就绪不够 (2026-11-06 三百三十)**: `BaseVehicle.checkSurroundingChunks:4279-4292`
     * 在 3×3 环未全部加载时置 `disableSimulationDueToLackOfSurroundingChunks = true`,
     * `setPhysicsActive` 会被强制降为 false —— 车停在抬高点上不会下落 (悬空), 等环加载完成、
     * 物理恢复时 `WorldSimulation.updateInternal:184-195` 又用原生物理体覆盖 jniTransform
     * (物理体没真正移动) → 车回到传送前位置。所以起跳前必须要求**目的地**的环也齐。
     */
    private static boolean chunkRingLoaded(IsoWorld world, float x, float y) {
        if (world == null || world.currentCell == null) {
            return false;
        }
        int wx = (int)Math.floor(x) / 8;
        int wy = (int)Math.floor(y) / 8;
        for (int dy = -1; dy <= 1; ++dy) {
            for (int dx = -1; dx <= 1; ++dx) {
                int cx = wx + dx;
                int cy = wy + dy;
                if (!world.getMetaGrid().isValidChunk(cx, cy)) {
                    continue;                      // 地图边界外的区块: 原版同样放行
                }
                IsoChunk chunk = world.currentCell.getChunk(cx, cy);
                if (chunk != null && chunk.loaded) {
                    continue;
                }
                return false;
            }
        }
        return true;
    }

    /** 目标点判定 (区块 → 格)。区块不在区块图内时连格都取不到, 分两步报便于排障。
     *  @param level **楼层** (不是物理 z) —— 一百零九 起一律由 {@link #levelFor} 提供。 */
    private static String destState(float x, float y, int level) {
        IsoWorld world = IsoWorld.instance;
        if (world == null || world.currentCell == null) {
            return "no-cell";
        }
        IsoChunkMap chunkMap = world.currentCell.getChunkMap(0);
        if (chunkMap == null) {
            return "no-chunk-map";
        }
        IsoChunk chunk = chunkMap.getChunkForGridSquare((int)Math.floor(x), (int)Math.floor(y));
        if (chunk == null) {
            return "no-chunk";
        }
        // 一百 追加: 区块**对象**在区块图里 != 地形**数据**已经流出 —— IsoChunk.loaded 由流式线程
        // 在 LoadOrCreate 成功后置位 (IsoChunk.java:224 / :2180)。旧判定只看对象非 null, 于是
        // "对象在、地形没到"的槽位一律放行 → 下面没有地面几何 → 整车自由落体 (用户实测: 每跳都可能下坠)。
        if (!chunk.loaded) {
            return "chunk-not-loaded";
        }
        if (world.currentCell.getGridSquare((double)x, (double)y, (double)level) == null) {
            // 一百零九: 带层号 —— "层不合法" (车不在目的地地面层) 与"格不存在"是两回事, 排障必须能分
            return "no-square@lvl" + level;
        }
        // 三百三十: 3×3 环不齐 → 物理会被 checkSurroundingChunks 强制停用, 车会悬在半空。
        if (!chunkRingLoaded(world, x, y)) {
            return "chunk-ring-not-loaded";
        }
        return "ok";
    }

    /**
     * 把载具沿世界 X/Z 平移 (dx/dy = 世界格数), 并按 raiseZ 抬高物理体 (一百零五 方案A "抬高落下"):
     * 车从目标上空落下, 落位高度由物理引擎解算 —— 消除"旧 z 嵌进更高地形"的穿地下坠; 落点在水面时
     * 车沉到河床停稳 (水面对物理是空的: IsoChunk.calcPhysics 只为 tree/stairs/solid/floor/wall 生成
     * 形状, 水格 FloorMaterial="Water" 不生成任何形状, 支撑来自河床地形 mesh), 落位合法性由 Lua 侧
     * "停稳判定"接管, 不按下沉量回滚。抬高 = 垂直升降, 不进反作弊 (SpeedChecker.set 只收 x/y 二维:
     * NetworkCharacterAI.java:350-362)。
     *
     * **v3 顺序修正 (2026-09-14 九十四)**: 目的地判定提到最前 —— 不通过就**不写任何状态**
     * 并返回 blocked-* (v2 是先写物理体再查格, 结果把车扔进未加载区自由落体)。
     * 通过则: 存档位姿 (抬高**前**, 回滚回起跳点) → 写原生物理体 → 同帧对齐 jniTransform →
     * clientUpdateVehiclePos 簿记。**逻辑格一律按载具逻辑层 (`levelFor`) 查** —— 一百零九 修正:
     * 旧实现传未抬高的**物理 z**, 而该参数是**楼层**; 平地恰好都在 0 附近所以没暴露, 车落到水面
     * 以下 (物理 z 为负 → fastfloor = -1) 时目标区块 `minLevel = 0` ⇒ 永远 no-square, 传送死锁。
     * 抬高后的物理 z 同样不能拿来查格 (3.7 经 fastfloor = 3, 一样是不存在的层, 且 null 格会让
     * clientUpdateVehiclePos 把车 removeFromWorld)。
     *
     * @param raiseZ 起跳抬升量 (物理单位, 1 楼层 ≈ 2.44949; ≤0 = 不抬高, 旧行为)
     * @return "ok" / "blocked-no-chunk" / "blocked-no-square@lvl&lt;层&gt;" /
     *         "blocked-chunk-ring-not-loaded" / "blocked-physics-off" / "physics-rejected(rc=&lt;负返回码&gt;)" /
     *         "no physics" / "error" / "no vehicle"
     */
    @LuaMethod(name = "vehicleNativeHop", global = true)
    public static String nativeHop(BaseVehicle vehicle, float dx, float dy, float raiseZ) {
        if (vehicle == null) {
            return "no vehicle";
        }
        try {
            String simulation = teleportSimulationState(vehicle);
            if (!"ok".equals(simulation)) {
                return "blocked-" + simulation;
            }
            if (landingVehicle != null) {
                return "blocked-pending-restore";
            }
            if (savedValid && (savedVehicle != vehicle
                || !"ok".equals(confirmPendingRestore(vehicle, false)))) {
                return "blocked-" + restoreResult;
            }
            if (Bullet.getOwnVehiclePhysics(vehicle.vehicleId, PHYSICS) != 0) {
                return "no physics";
            }
            float zOrig = PHYSICS[2];
            // 一百零九: 一律先算好再判 —— 下面所有 return 都发生在写状态之前
            int lvl = levelFor(vehicle);
            float x = PHYSICS[0] + dx;
            float y = PHYSICS[1] + dy;
            String state = destState(x, y, lvl);
            if (!"ok".equals(state)) {
                return "blocked-" + state;
            }
            IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare((double)x, (double)y, (double)lvl);
            if (sq == null) {
                return "blocked-no-square@lvl" + lvl;     // 与 destState 同参必然一致; 兜底防版本差异
            }
            // 开启前先让原版刷新旧区块禁用标记；失败不占用保存态。
            boolean wasActive = vehicle.isPhysicsActive();
            String activated = activateLocalPhysics(vehicle);
            if (!"ok".equals(activated)) {
                return "blocked-" + activated;
            }
            System.arraycopy(PHYSICS, 0, SAVED, 0, PHYSICS.length);
            savedValid = true;
            savedVehicle = vehicle;
            savedVehicleId = vehicle.vehicleId;
            savedLevel = lvl;
            savedPhysicsActive = wasActive;
            restorePending = false;
            restoreWriteAccepted = false;
            restoreNativeConfirmed = false;
            restoreResult = "pending-restore";

            PHYSICS[0] = x;
            PHYSICS[1] = y;
            if (raiseZ > 0.0f) {
                // 一百零九 抬高基准 = **逻辑层地面** (不是沉底的物理 z): 在水面以下起跳时若以 zOrig 为
                // 基准, 抬高 1.5 楼层仍可能落在陆地地面**之下** → 又变回"嵌进地形"。陆地/上层两种写法等价。
                float base = Math.max(zOrig, lvl * PHYSICS_UNITS_PER_LEVEL);
                PHYSICS[2] = base + raiseZ;            // 方案A: 抬高落下 (SAVED 已存, 回滚不受影响)
            }
            // 三百三十: 清零线速度 —— 残速会在物理恢复的第一帧把车带飞/带翻。
            PHYSICS[7] = 0.0f;
            PHYSICS[8] = 0.0f;
            PHYSICS[9] = 0.0f;

            int written = Bullet.setOwnVehiclePhysics(vehicle.vehicleId, PHYSICS, false);
            // 三百四十七 (2026-11-08): 判据由 "!= 0" 改为 "< 0"。反汇编 PZBullet64.dll 的导出
            // Java_zombie_core_physics_Bullet_setOwnVehiclePhysics 得到的确切语义:
            //   失败只有负值 —— -1 (WorldSimulation::instance 为空, 同时抛 Java 异常) /
            //   -2 (GetFloatArrayElements 返回空指针) / -3 (单例二次判空) / -4 (车辆 ID 不在物理表) /
            //   -5 (数组里的车轮数与车体不符);
            //   成功路径结尾是 `mov eax,[WorldSimulation+0xa8]` 直接返回该字段, **不保证为 0**。
            // 旧判据把"成功"当失败: 每次起跳都立刻写回存档 → 车一动没动, 控制台只剩 physics-rejected。
            // 同一族 JNI 的返回码并不统一 (setVehicleMass/setVehicleParams 成功=0, setVehicleStatic 成功=1),
            // 因此不能按"0 才是成功"套。
            if (written < 0) {
                String rolled = nativeRollback(vehicle);
                return "physics-rejected(rc=" + written + ",rollback=" + rolled + ")";
            }
            // 回读只重试一次；负返回码仍须报告，未确认写入由落地观察拒绝假成功。
            int readBack = Bullet.getOwnVehiclePhysics(vehicle.vehicleId, CHECK);
            if (readBack != 0 || !poseMatches(PHYSICS, CHECK)) {
                int retried = Bullet.setOwnVehiclePhysics(vehicle.vehicleId, PHYSICS, true);
                if (retried < 0) {
                    String rolled = nativeRollback(vehicle);
                    return "physics-rejected(rc=" + retried + ",rollback=" + rolled + ")";
                }
                readBack = Bullet.getOwnVehiclePhysics(vehicle.vehicleId, CHECK);
                if (readBack != 0 || !poseMatches(PHYSICS, CHECK)) {
                    Logger.printLog(String.format(
                        "[VehTeleport] hop write not confirmed (rc=%d read=%d at=%.2f,%.2f,%.2f want=%.2f,%.2f,%.2f)",
                        written, readBack, CHECK[0], CHECK[1], CHECK[2], x, y, PHYSICS[2]));
                }
            }
            if (readBack == 0) {
                copyWheelState(PHYSICS, CHECK);
            }
            applyPose(vehicle, PHYSICS, sq, lvl);
            return "ok";
        }
        catch (Throwable t) {
            String rolled = savedValid && savedVehicle == vehicle ? nativeRollback(vehicle) : "no save";
            Logger.error("[VehTeleport] hop failed (rollback " + rolled + ")", t);
            return "error";
        }
    }

    /**
     * 落位扶正 (一百零八, 用户实测: "1.5 层楼传送过去有可能导致落下的途中翻车")。
     * 车从抬高点落体触地时冲击大, 在斜坡/不平地面会翻滚 —— 传送不该把车留成四轮朝天/侧躺。
     *
     * 做法: vanilla `BaseVehicle.getUpVectorDot()` (:4011, 阈值常量 `MINIMUM_DOT_UPRIGHT = 0.8` :266)
     * 判是否翻覆 → 翻则读物理数组, 由当前四元数取水平朝向 (绕物理 +y 的 yaw), 用"仅 yaw"的竖直
     * 四元数覆盖 `PHYSICS[3..6]`, 并清零线速度 `PHYSICS[7..9]` (落体/翻滚残速会把车再带翻),
     * 经 `Bullet.setOwnVehiclePhysics` 写回 + 同帧对齐 `jniTransform`。
     *
     * **为什么走物理数组而不是 vanilla `flipUpright()`** (:3800, 内部 `setWorldTransform` →
     * `Bullet.teleportVehicle`): 位置/姿态写在 `setOwnVehiclePhysics` 通道已被 C3 多轮实测证明能持久
     * 生效, 而 v1 的 `setWorldTransform` 路线实测维持不到 1 秒就被物理步读回覆盖 (C 设计方案 §C3 v1);
     * 且自行计算可**保留车头朝向**, vanilla `flipUpright` 把旋转直接归零 (朝向丢失)。
     * 代价如实: 翻覆姿态下 yaw 提取是**近似**的 (极端翻滚可能朝向有偏), 目标是"车能开"而非朝向精确。
     *
     * @return "ok" (未翻, 零动作) / "fixed" (已扶正) / "no physics" / "error" / "no vehicle"
     */
    @LuaMethod(name = "vehicleNativeUpright", global = true)
    public static String nativeUpright(BaseVehicle vehicle) {
        return uprightIfFlipped(vehicle);
    }

    /** Java 侧共用入口 (载具页「车辆重置」也走这里, 免重复四元数运算)。 */
    public static String uprightIfFlipped(BaseVehicle vehicle) {
        if (vehicle == null) {
            return "no vehicle";
        }
        try {
            String state = localSimulationState(vehicle);
            if (!"ok".equals(state)) {
                return state;
            }
            boolean flipped;
            try {
                flipped = vehicle.getUpVectorDot() < BaseVehicle.MINIMUM_DOT_UPRIGHT;
            }
            catch (Throwable t) {
                return "no-pose";
            }
            if (!flipped) {
                return "ok";
            }
            String activated = activateLocalPhysics(vehicle);
            if (!"ok".equals(activated)) {
                return activated;
            }
            if (Bullet.getOwnVehiclePhysics(vehicle.vehicleId, PHYSICS) != 0) {
                return "no physics";
            }
            int level = levelFor(vehicle);
            IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(
                (double)PHYSICS[0], (double)PHYSICS[1], (double)level);
            if (sq == null) {
                return "no-square";
            }
            float qx = PHYSICS[3];
            float qy = PHYSICS[4];
            float qz = PHYSICS[5];
            float qw = PHYSICS[6];
            // 水平朝向: 绕物理 +y 的 yaw (标准 Y-up 四元数提取; 翻覆时近似, 见方法注释)
            float yaw = (float)Math.atan2(2.0 * (qw * qy + qx * qz), 1.0 - 2.0 * (qy * qy + qz * qz));
            float half = yaw * 0.5f;
            PHYSICS[3] = 0.0f;
            PHYSICS[4] = (float)Math.sin(half);
            PHYSICS[5] = 0.0f;
            PHYSICS[6] = (float)Math.cos(half);
            PHYSICS[7] = 0.0f;                     // 线速度清零 (vx/vy/vz): 残速会把车再带翻
            PHYSICS[8] = 0.0f;
            PHYSICS[9] = 0.0f;
            int written = Bullet.setOwnVehiclePhysics(vehicle.vehicleId, PHYSICS, false);
            if (written < 0) {
                return "physics-rejected(rc=" + written + ")";
            }
            if (Bullet.getOwnVehiclePhysics(vehicle.vehicleId, CHECK) != 0 || !poseMatches(PHYSICS, CHECK)) {
                return "write-unconfirmed";
            }
            copyWheelState(PHYSICS, CHECK);
            applyPose(vehicle, PHYSICS, sq, level);
            return "fixed";
        }
        catch (Throwable t) {
            return "error";
        }
    }

    /**
     * 落位校验：先要求共用观察窗确认落地，再检查目标平面漂移与原有极端下沉线。
     * @param maxSink 允许的总下沉量 (保留原有落水/落谷政策)
     */
    @LuaMethod(name = "vehicleNativeVerify", global = true)
    public static String nativeVerify(BaseVehicle vehicle, float expectX, float expectY, float maxSink) {
        if (vehicle == null) {
            return "no vehicle";
        }
        try {
            if (Bullet.getOwnVehiclePhysics(vehicle.vehicleId, PHYSICS) != 0) {
                return "no physics";
            }
            String simulation = localSimulationState(vehicle);
            if (!"ok".equals(simulation)) {
                return simulation;
            }
            if (landingVehicle != vehicle || !"ok".equals(landingResult)
                || WorldSimulation.instance.getBulletFrameNo() != landingFrame
                || !Float.isFinite(PHYSICS[8]) || Math.abs(PHYSICS[8]) > 0.1f
                || !vehicle.isPhysicsActive() || !vehicle.isActive || vehicle.isStatic
                || !chunkRingLoaded(IsoWorld.instance, PHYSICS[0], PHYSICS[1]) || !hasSupport(vehicle, PHYSICS)) {
                return "not-landed";
            }
            if (savedValid && savedVehicle == vehicle && savedVehicleId == vehicle.vehicleId) {
                float sink = PHYSICS[2] - SAVED[2];
                if (sink < -maxSink) {
                    return String.format("sinking(dz=%.2f)", sink);
                }
            }
            float lx = vehicle.getX() - expectX;
            float ly = vehicle.getY() - expectY;
            if (Math.abs(lx) > 1.0f || Math.abs(ly) > 1.0f) {
                return String.format("logic-drift(d=%.2f,%.2f)", lx, ly);
            }
            float px = PHYSICS[0] - expectX;
            float py = PHYSICS[1] - expectY;
            if (Math.abs(px) > 2.0f || Math.abs(py) > 2.0f) {
                return String.format("phys-drift(d=%.2f,%.2f)", px, py);
            }
            return sourceState(vehicle, PHYSICS);
        }
        catch (Throwable t) {
            return "error";
        }
    }

    /** 嵌地重置只抬到原有逻辑层，走同一个原生数组通道，不把坡上车辆压到零高度。 */
    public static String liftToLogicalGround(BaseVehicle vehicle) {
        try {
            String state = localSimulationState(vehicle);
            if (!"ok".equals(state)) {
                return state;
            }
            if (Bullet.getOwnVehiclePhysics(vehicle.vehicleId, PHYSICS) != 0) {
                return "no physics";
            }
            int level = levelFor(vehicle);
            float ground = level * PHYSICS_UNITS_PER_LEVEL;
            if (PHYSICS[2] >= ground - 0.5f) {
                return "ok";
            }
            IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(
                (double)PHYSICS[0], (double)PHYSICS[1], (double)level);
            if (sq == null) {
                return "no-square";
            }
            PHYSICS[2] = ground;
            PHYSICS[7] = PHYSICS[8] = PHYSICS[9] = 0.0f;
            int written = Bullet.setOwnVehiclePhysics(vehicle.vehicleId, PHYSICS, false);
            if (written < 0) {
                return "physics-rejected(rc=" + written + ")";
            }
            if (Bullet.getOwnVehiclePhysics(vehicle.vehicleId, CHECK) != 0 || !poseMatches(PHYSICS, CHECK)) {
                return "write-unconfirmed";
            }
            copyWheelState(PHYSICS, CHECK);
            applyPose(vehicle, PHYSICS, sq, level);
            return "lifted";
        }
        catch (Throwable t) {
            return "error";
        }
    }

    /**
     * 回滚到上一跳前的完整原生位姿；未确认写入、显示和簿记前不丢保存态。
     * 原生回读失败返回明确原因，不把未恢复的画面或乘员坐标当作 ok。
     */
    @LuaMethod(name = "vehicleNativeRollback", global = true)
    public static String nativeRollback(BaseVehicle vehicle) {
        if (vehicle == null) {
            return "no vehicle";
        }
        if (!savedValid || savedVehicle != vehicle || savedVehicleId != vehicle.vehicleId) {
            return "no save";
        }
        try {
            String state = localSimulationState(vehicle);
            if (!"ok".equals(state)) {
                return state;
            }
            if (restorePending && restoreWriteAccepted) {
                return confirmPendingRestore(vehicle, false);
            }
            IsoWorld world = IsoWorld.instance;
            if (world == null || world.currentCell == null || VehicleManager.instance == null) {
                return "no-cell";
            }
            IsoGridSquare sq = world.currentCell.getGridSquare(
                (double)SAVED[0], (double)SAVED[1], (double)savedLevel);
            if (sq == null || sq.chunk == null || !sq.chunk.loaded) {
                return "restore-square-unloaded";
            }
            restorePending = true;
            restoreWriteAccepted = false;
            restoreNativeConfirmed = false;
            restoreDeadlineMs = System.nanoTime() / 1_000_000L + RESTORE_CONFIRM_MS;
            restoreFrame = WorldSimulation.instance.getBulletFrameNo();
            restoreResult = "pending-restore";
            int written = Bullet.setOwnVehiclePhysics(vehicle.vehicleId, SAVED, false);
            if (written < 0) {
                return "physics-rejected(rc=" + written + ")";
            }
            restoreWriteAccepted = true;
            int read = Bullet.getOwnVehiclePhysics(vehicle.vehicleId, CHECK);
            if (read != 0 || !poseMatches(SAVED, CHECK)) {
                written = Bullet.setOwnVehiclePhysics(vehicle.vehicleId, SAVED, true);
                if (written < 0) {
                    return "physics-rejected(rc=" + written + ")";
                }
                read = Bullet.getOwnVehiclePhysics(vehicle.vehicleId, CHECK);
                if (read != 0 || !poseMatches(SAVED, CHECK)) {
                    return "pending-restore";
                }
            }
            restoreNativeConfirmed = true;
            // 保存数组保持不变；轮系接触值使用原生恢复后的新读数。
            applyPose(vehicle, CHECK, sq, savedLevel);
            vehicle.checkSurroundingChunks();
            vehicle.setPhysicsActive(savedPhysicsActive);
            String restored = sourceState(vehicle, SAVED);
            if (!"ok".equals(restored)) {
                return "pending-restore";
            }
            int confirmed = Bullet.getOwnVehiclePhysics(vehicle.vehicleId, CHECK);
            if (confirmed != 0 || !poseMatches(SAVED, CHECK)) {
                return "pending-restore";
            }
            if (vehicle.getZ() != savedLevel) {
                return "pending-restore";
            }
            clearSaved(vehicle);
            return "ok";
        }
        catch (Throwable t) {
            Logger.error("[VehTeleport] rollback failed", t);
            return "error";
        }
    }
}
