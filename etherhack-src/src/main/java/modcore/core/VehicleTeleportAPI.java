package modcore.core;

import org.joml.Quaternionf;
import se.krka.kahlua.integration.annotations.LuaMethod;
import zombie.characters.IsoPlayer;
import zombie.core.physics.Bullet;
import zombie.core.physics.WorldSimulation;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehicleManager;

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
 * 本类只提供原语, 不含状态机 (分步推进/等待加载/超时/取消在 Lua 侧 UIMap)。
 */
public final class VehicleTeleportAPI {

    /** 1 楼层对应的物理单位 (vanilla 常量, 见 BaseVehicle.java:1831 / :3202 / :2670)。 */
    private static final float PHYSICS_UNITS_PER_LEVEL = 2.44949f;

    private static final float[] PHYSICS = new float[27];
    /** 上一跳前的物理数组存档 (回滚用, 只保一份: 同时只有一辆车在跳)。 */
    private static final float[] SAVED = new float[27];
    private static boolean savedValid = false;
    private static int savedVehicleId = -1;
    /** 扶正用四元数 (复用, 免每次分配)。 */
    private static final Quaternionf UPRIGHT_QUAT = new Quaternionf();

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
        float px = Float.NaN, py = Float.NaN, pz = Float.NaN;
        try {
            if (Bullet.getOwnVehiclePhysics(vehicle.vehicleId, PHYSICS) == 0) {
                px = PHYSICS[0];
                py = PHYSICS[1];
                pz = PHYSICS[2];
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
        return String.format(
            "logic=(%.2f,%.2f,%.2f) phys=(%.2f,%.2f,%.2f) jni=(%.2f,%.2f,%.2f) ctrl=%s lvl=%d %s %s %s",
            vehicle.getX(), vehicle.getY(), vehicle.getZ(),
            px, py, pz,
            vehicle.jniTransform.origin.x + offsetX,
            vehicle.jniTransform.origin.z + offsetY,
            vehicle.jniTransform.origin.y,
            ctrl, levelFor(vehicle), physLvlPart, playerPart, chunkPart);
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
     * @return "ok" / "blocked-no-chunk" / "blocked-no-square@lvl&lt;层&gt;" / "blocked-*" /
     *         "no physics" / "error" / "no vehicle"
     */
    @LuaMethod(name = "vehicleNativeHop", global = true)
    public static String nativeHop(BaseVehicle vehicle, float dx, float dy, float raiseZ) {
        if (vehicle == null) {
            return "no vehicle";
        }
        try {
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
            // 跳前存档 (供 verify 判下坠基准 + rollback 回退); 存的是抬高前的起跳位姿
            System.arraycopy(PHYSICS, 0, SAVED, 0, PHYSICS.length);
            savedValid = true;
            savedVehicleId = vehicle.vehicleId;

            PHYSICS[0] = x;
            PHYSICS[1] = y;
            if (raiseZ > 0.0f) {
                // 一百零九 抬高基准 = **逻辑层地面** (不是沉底的物理 z): 在水面以下起跳时若以 zOrig 为
                // 基准, 抬高 1.5 楼层仍可能落在陆地地面**之下** → 又变回"嵌进地形"。陆地/上层两种写法等价。
                float base = Math.max(zOrig, lvl * PHYSICS_UNITS_PER_LEVEL);
                PHYSICS[2] = base + raiseZ;            // 方案A: 抬高落下 (SAVED 已存, 回滚不受影响)
            }
            Bullet.setOwnVehiclePhysics(vehicle.vehicleId, PHYSICS, false);
            vehicle.jniTransform.origin.x = x - WorldSimulation.instance.offsetX;
            vehicle.jniTransform.origin.z = y - WorldSimulation.instance.offsetY;
            if (raiseZ > 0.0f) {
                vehicle.jniTransform.origin.y = PHYSICS[2];   // 同帧对齐三源 (物理步下一帧自会覆盖)
            }
            // clientUpdateVehiclePos 内部 setZ(0), z 参数仅透传; sq 按逻辑层查 (见上注)
            VehicleManager.instance.clientUpdateVehiclePos(vehicle, x, y, PHYSICS[2], sq);
            return "ok";
        }
        catch (Throwable t) {
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
            boolean flipped;
            try {
                flipped = vehicle.getUpVectorDot() < BaseVehicle.MINIMUM_DOT_UPRIGHT;
            }
            catch (Throwable t) {
                return "ok";                       // 姿态读数不可用 → 不动 (宁可不动, 不误扶)
            }
            if (!flipped) {
                return "ok";
            }
            if (Bullet.getOwnVehiclePhysics(vehicle.vehicleId, PHYSICS) != 0) {
                return "no physics";
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
            Bullet.setOwnVehiclePhysics(vehicle.vehicleId, PHYSICS, false);
            vehicle.jniTransform.origin.x = PHYSICS[0] - WorldSimulation.instance.offsetX;
            vehicle.jniTransform.origin.y = PHYSICS[2];
            vehicle.jniTransform.origin.z = PHYSICS[1] - WorldSimulation.instance.offsetY;
            UPRIGHT_QUAT.set(0.0f, PHYSICS[4], 0.0f, PHYSICS[6]);
            vehicle.jniTransform.setRotation(UPRIGHT_QUAT);
            return "fixed";
        }
        catch (Throwable t) {
            return "error";
        }
    }

    /**
     * 落位校验 (跳后观察窗口结束时调): 逻辑与原生都贴近目标点、且相对跳前没有下坠 → "ok"。
     * 否则返回原因串 (sinking / logic-drift / phys-drift), 由 Lua 侧决定回滚。
     *
     * @param maxSink 允许的原生高度下降量 (世界单位; 平地正常为 0)
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
            if (savedValid && savedVehicleId == vehicle.vehicleId) {
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
            return "ok";
        }
        catch (Throwable t) {
            return "error";
        }
    }

    /**
     * 回滚到上一跳前的存档位姿 (落位校验失败时用): 物理体 + jniTransform 一起写回,
     * 目的地格存在时同步逻辑簿记。返回 "ok" / "no save" / "no vehicle" / "error"。
     *
     * 一百零九: 逻辑格同样按**载具逻辑层**查 (旧实现用存档里的物理 z, 车在水面以下时必然查不到格 →
     * "物理回来了但簿记没回"), 取不到再退一步用存档 z 反推层 (老行为), 都没有就只回物理体。
     */
    @LuaMethod(name = "vehicleNativeRollback", global = true)
    public static String nativeRollback(BaseVehicle vehicle) {
        if (vehicle == null) {
            return "no vehicle";
        }
        if (!savedValid || savedVehicleId != vehicle.vehicleId) {
            return "no save";
        }
        try {
            Bullet.setOwnVehiclePhysics(vehicle.vehicleId, SAVED, false);
            vehicle.jniTransform.origin.x = SAVED[0] - WorldSimulation.instance.offsetX;
            vehicle.jniTransform.origin.z = SAVED[1] - WorldSimulation.instance.offsetY;
            int lvl = levelFor(vehicle);
            IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare((double)SAVED[0], (double)SAVED[1], (double)lvl);
            if (sq == null) {
                lvl = (int)Math.floor(SAVED[2] / PHYSICS_UNITS_PER_LEVEL + 0.05f);
                sq = IsoWorld.instance.currentCell.getGridSquare((double)SAVED[0], (double)SAVED[1], (double)lvl);
            }
            if (sq != null) {
                VehicleManager.instance.clientUpdateVehiclePos(vehicle, SAVED[0], SAVED[1], SAVED[2], sq);
            }
            savedValid = false;
            return "ok";
        }
        catch (Throwable t) {
            return "error";
        }
    }
}
