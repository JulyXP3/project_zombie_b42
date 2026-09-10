/*
 * 自动驾驶核心状态机 (研判文档 §八: 状态机即抽离边界, 自洽于 Java 侧, 不含任何 UI)。
 *
 * 驱动方式: 唯一注入点 CarController.updateControls 头部 (CarControllerPatch) —
 * 本地司机每帧进入 onUpdateControls(vehicle): 激活时接管 clientControls 并 return true
 * (跳过键位读取), 未激活 return false 放行原版。接管判定读原始键态, 按键当帧取消并
 * 放行 — 无死帧、无交接延迟、无状态残留 (研判 §五)。
 *
 * 下游 100% 原版白嫖: 油门斜率/换挡/转向积分+车速clamp/未加载区块自动刹车/点火,
 * 上行仍是正常 VehiclePhysicsPacket (150/300ms) — 零新网络面 (研判 §一/§二)。
 *
 * 状态机: IDLE → DRIVING ↔ (BRAKE_TO_BOUNDARY → WAIT_LOAD) → ARRIVED/CANCELLED(回IDLE)。
 * 三层速度体系 (§五): 硬顶 = min(服务端 speedLimit×0.85, 车辆脚本极速) 全自动;
 * 巡航目标 = 用户旋钮 (0=自适应 ~55); 场景瞬时上限全自动。
 * 伪·自动驾驶 (2026-09-06 用户拍板"由繁化简": 锚定后直接按道路往终点走, 优先到达):
 * ① 世界碰撞豁免 (BulletNoClipHook/Patch): 激活期 IsoChunk.calcPhysics 只保留
 *    Floor 形状 — 车/栅栏/树/灯柱互相可穿, 开关切换经 refreshChunks 重传已加载
 *    chunk (原版 updatePhysicsForLevel 路径), 新载 chunk 天然跟随; 静态障碍软化 =
 *    近障兜底低速碾过, 不再停-倒-绕舞蹈。
 * ② 战损豁免 (同 Hook): 撞角色自伤 → 0 (伤害报告不发出, 服务端零痕迹); 僵尸被撞
 *    伤害 → 500 (碰着即死, 原版击杀/计数/布娃娃/同步全保留); 撞击减速冲量 → 关
 *    (尸群顶不停车)。均 instanceof/激活态双门控, 手动驾驶与玩家被撞完全原版。
 * ③ 路线 (planRoute): 一次性路线语义 (2026-09-06 用户拍板) — 锚定一次 → 规划一条
 *    路线并画在地图上, 车沿这条线一直走到终点; 路线平时冻结, 只有两个事件改动它:
 *    向前续段 (路线尽头/脱困后) 与受阻替换尾段 (被其他载具硬堵 — 穿墙只豁免
 *    静态物); 周期重规划已删除。路线来源 = 大地图同源道路矢量 (RoadNetwork:
 *    worldmap.xml.bin 的 highway 路网, 与大地图渲染同一份数据) — 起终点各吸附
 *    最近路网节点 Dijkstra 中心折线, 整图一次可得, 无流式加载限制, 首段即全程;
 *    路网不可用/断裂退化直线直奔 (routeKind="direct", 穿墙保证可达)。路线读数
 *    (点数/坐标/进度/类型) 经 AutoDriveAPI 暴露, AutoDriveMap.lua 逐帧重画
 *    同一条固定数据 (蓝色实心线, 兜底直线橙色)。
 * 感知节拍 150ms 自适应 (按时长非 tick 数, 卡顿降帧走廊随 dt 等比拉长), 扫描三层:
 * 动态层按列表过滤 O(目标数), 静态层沿前向中心线 + 两侧车道探未加载边界与实体
 * 障碍 (判定与寻路同源, 且对照 IsoChunk 车辆碰撞装配补齐 blocksight/柱体族真值;
 * 中心线进安全速, 侧车道仅转向朝它时压爬行), 量级 <0.1ms/次。路线事件通道:
 * 尽头续段 / 受阻 1.5s 尾段替换 (8s 收场) / 卡死 6s 续段 (连续 2 次无效升级倒车,
 * 共用 2s 冷却; 冷却跳过不计入受阻清零)。脱困双通道:
 * 卡死倒车 (油门推不动) + 停驻死锁倒车 (带刹怼住无油门, 卡死检测够不着的那条路)。
 * 速度上限族: 弯道角误差线性 + 纯跟踪几何物理限速 (侧向加速度 ~3m/s²) + 偏航
 * (离路径中线越远越慢) + 避障/边界制动距离换算。纵向速度带: 停驶带 (≤2 或障碍
 * 压进爬行档以下 — 停车等绕行, 绝不顶着障碍碾; 绕行/大转向脱困例外) / 爬行带
 * (仅用户设定低速巡航, 油门常给) / 常速带阈值油门-滑行-点刹。
 *
 * 仅依赖共享设施, 禁止 import 任何其他功能域 (drive 域依赖纪律, §八)。
 */
package modcore.drive;

import modcore.utils.Logger;
import org.joml.Vector3f;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.core.physics.CarController;
import zombie.input.GameKeyboard;
import zombie.input.JoypadManager;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.network.ServerOptions;
import zombie.vehicles.BaseVehicle;

import java.util.ArrayList;

public final class AutoDriveController {

    // ===== 状态机 (AutoDriveAPI.autoDriveGetStateId 对外) =====
    public static final int STATE_IDLE = 0;
    public static final int STATE_DRIVING = 1;
    public static final int STATE_BRAKE_TO_BOUNDARY = 2;
    public static final int STATE_WAIT_LOAD = 3;
    public static final int STATE_ARRIVED = 4;

    // ===== 常量 (量级估算, 实测校准, 见研判 §七 验证清单) =====
    private static final long SCAN_PERIOD_MS = 150;      // 感知间隔 (自适应 ≈10 tick)
    private static final float BRAKE_DECEL = 6.0f;       // 舒适制动减速度 m/s² (制动距离估算)
    private static final float ARRIVE_RADIUS = 4.0f;     // 到达半径 (格, §五)
    private static final float CRUISE_ADAPTIVE = 55.0f;  // 自适应巡航档 (从容, 观感像人)
    private static final float SPEED_LIMIT_MARGIN = 0.85f; // 硬顶余量 (永不违规, §四)
    /** Stanley 增益: 横向偏差项的收敛快慢 (e=1 格时低速 ~60°/高速 ~6°)。 */
    private static final float K_STANLEY = 1.5f;

    // ===== 纵向层 (速度剖面 + IDM) 参数 — 单位制: 内部 m/s, 对外 km/h =====
    /** 弯道横向加速度上限 (m/s²): v = √(a·R) 的 a。 */
    private static final float A_LAT_MAX = 1.8f;   // 旧 2.5 高估 PZ 轮胎抓地 → 弯中甩出 (实测); 1.8 = 保守弯速
    /** 剖面规划舒适减速度 (m/s², 后向传递用; 急刹由 BRAKE_DECEL 负责)。 */
    private static final float A_PLAN_BRAKE = 4.0f;
    /** IDM: 最大加速度 (m/s²)。 */
    private static final float IDM_A_MAX = 2.0f;
    /** IDM: 舒适减速度 (m/s²)。 */
    private static final float IDM_B = 3.0f;
    /** IDM: 期望车头时距 (s)。 */
    private static final float IDM_T = 1.2f;
    /** IDM: 最小停车间距 (m, 格)。 */
    private static final float IDM_S0 = 3.0f;
    /** IDM 虚拟前车兜底 (卅八): 走廊内障碍 = 静止前车, gap 项保命 (s→s0 ⇒ 必刹)。 */
    // gap 走廊必须 << 检测走廊 (3.5): 绕行中障碍 lat 常在 0.5~3.5 徘徊, 走廊重叠
    // 则 gap 深刹与绕行转向互相打架 → 开一下刹一下 (实测卅九)。1.5 = 死线正前
    // (车半宽 1.5), 只有真的正对障碍才触发保命刹; 横向绕开即平滑退出。
    private static final float IDM_GAP_CORRIDOR = 1.5f;
    private static final float IDM_GAP_CLEAR = 3.5f;      // 间隙 = lon - (自车半长1.5 + 障碍包围2.0)
    private static final float IDM_ACCEL_MIN = -8.0f;     // 深刹下限 (物理可达)
    /** 剖面采样步长 (格)。 */
    private static final float PROFILE_DS = 2.0f;
    private static final long BOUNDARY_TIMEOUT_MS = 45000; // 边界等待超时
    private static final long REPLAN_COOLDOWN_MS = 2000; // 路线事件 (续段/尾段替换) 冷却闸门
    private static final long ENGINE_DEAD_MS = 5000;     // 引擎持续非运行 → 取消

    // ===== 单例 =====
    private static final AutoDriveController INSTANCE = new AutoDriveController();

    public static AutoDriveController getInstance() {
        return INSTANCE;
    }

    /** 注入入口 (CarControllerPatch 唯一调用点)。返回 true = 本帧已接管。 */
    public static boolean onUpdateControls(BaseVehicle vehicle) {
        return INSTANCE.handleControls(vehicle);
    }

    // ===== 运行态 =====
    private int state = STATE_IDLE;
    private String messageKey = "";
    private float targetX;
    private float targetY;
    private ArrayList<float[]> path;        // 世界坐标路点 {x, y}
    private int pathIdx;
    private String routeKind = "";          // 当前路线类型 ("road"/"fallback"/"")

    // 持久配置 (AutoDriveAPI.loadConfig/saveConfig 落 modcore/config/drive.properties)
    private float cruiseSpeed = 0.0f;       // 用户巡航目标速, 0 = 自适应
    public boolean verbose = false;         // 置 true 输出状态机转换 verbose 日志 (排障用)

    // 控制 (每 tick 算出, writeControls 落盘)
    private float ctlSteer;
    private boolean ctlForward;
    private boolean ctlBackward;
    private boolean ctlBrake;

    // 感知缓存 (150ms)
    private float boundaryDist = Float.MAX_VALUE;
    private long lastScanMs;
    private final Vector3f fwdVec = new Vector3f();

    // 计时/脱困
    private long lastTickMs;
    private long stateSinceMs;
    private boolean arrivalBrake;
    private long engineDeadMs;
    private long replanCooldownMs;
    private long lastHandleMs;             // 补丁通道最近一次进 handleControls 的时刻 (看门狗用)
    private boolean worldNoClipApplied;    // chunk 重传去重 (与目标态一致则跳过 refresh)

    // ===== 动态避障层 (车辆/残骸 waypoint detour, 2026-09-07 卅一) =====
    // MP 下车-车碰撞服务端权威 (ghost 下沉治不了, 廿六/廿七实测), 抬高自身车也不
    // 成立 (raycast 轮不把车顶当接地物, 悬空=驱动失效, 卅零评估) — 唯一真防线 =
    // 物理上别碰: 感知前方走廊最近挡路车 → 生成绕行目标点 (障碍侧后方空地, 静态物
    // 反正穿墙, 目标点只需避开车辆) → 车朝目标点直插绕过 → 障碍过车尾回线。
    // 两侧都无净空 (整条路被车辆堵死) → 前后蠕动防僵尸砸窗 (用户要求)。
    private static final float AVOID_LOOKAHEAD = 18.0f;   // 障碍前瞻 (格, ≈1.1s@55km/h)
    private static final float AVOID_CLEAR_DIST = 8.0f;   // 障碍过车尾余量 (格, 过了才回线)
    // 侧向偏移 8 格 (仿真全绿参数): 走廊需求 3.5 + 收敛余量 4.5; 横向绝对锚定障碍
    // (卅八) 后净空 = 8 - 3.5 = 4.5 格 (旧值 4 穿走廊 / 6 净空仅 1.1, 仿真实测)。
    private static final float AVOID_LAT_OFFSET = 8.0f;
    private static final float AVOID_MIN_AHEAD = 6.0f;    // 胡萝卜点最小前视 (障碍贴近后保持平行偏移线)
    private static final float AVOID_SPEED_CAP = 10.0f;   // 绕行/并入恒速 km/h (卌三, 用户拍板: 慢爬通过;
                                                          // 旧 25 随模式 50↔25 摆动 = 一顿一顿主源之一)
    private static final int AVOID_NONE = 0;              // 无绕行
    private static final int AVOID_DETOUR = 1;            // 目标点绕行中
    private static final int AVOID_BLOCKED = 2;           // 双侧堵死 (蠕动)
    private static final int AVOID_RETURN = 3;            // 并入段: 纯追踪回线 (卌一)
    // RETURN 并入参数 (仿真 S1-S6 全绿): 截获角 ≤ atan(e/LOOKAHEAD) 有界 → 平滑收敛;
    // |cte| < CTE_EXIT 交回 Stanley (近线后 Stanley 良态)。
    private static final float AVOID_RETURN_LOOKAHEAD = 12.0f;
    private static final float AVOID_RETURN_CTE_EXIT = 1.5f;
    private int avoidMode = AVOID_NONE;
    private float avoidTargetX;                           // 绕行目标点 (世界坐标)
    private float avoidTargetY;
    private float avoidObstacleX = Float.NaN;             // 当前挡路障碍 (感知)
    private float avoidObstacleY = Float.NaN;
    private boolean avoidDiagHit;          // AvoidDiag 节流 (状态变化才打)
    private float avoidSide;               // 锁定的绕行侧 (+1/-1): 进入 DETOUR 时定, 过障碍前不变 (卅七)
    private int avoidDiagMode = -1;        // AvoidDiag 模式节流 (模式变化才打)
    private float detourDirX, detourDirY;  // 进入 DETOUR 时锁定的行进方向 (固定参考系, 卌)
    private float avoidWpBX, avoidWpBY;    // 障碍后方 waypoint (固定几何, 卌)
    private float obsLon = Float.NaN;      // 障碍纵向投影 (IDM gap 用, NaN = 走廊内无障碍)
    private float obsLat = Float.NaN;
    private float minDistToTarget = Float.MAX_VALUE;   // 终点最近点追踪 (绕行残留偏移>到达半径时防冲过, 仿真 S5)
    // 碰撞恢复宽限: 退出接管时车可能正在墙里/树下, 立即恢复静态碰撞会被 Bullet
    // 把嵌着的车往地下挤 (实测: 概率黑屏 + 下车发现车埋地下)。宽限期内保持可穿,
    // 玩家驶离后再恢复。伤害/冲量豁免不受宽限 (isWorldNoClip 仍按状态严格门控)。
    private long pendingNoClipOffMs;
    private static final long NOCLIP_GRACE_MS = 15000;

    private AutoDriveController() {
    }

    /**
     * 伪·自动驾驶世界碰撞豁免开关 (BulletNoClipHook 伤害/冲量钩子读): 状态非
     * IDLE 即生效。宽限期内 (退出接管后 NOCLIP_GRACE_MS 毫秒) 返回 false — 手动
     * 驾驶立刻恢复原版战损/冲量, 不留作弊窗口。
     */
    public static boolean isWorldNoClip() {
        return INSTANCE.state != STATE_IDLE;
    }

    /**
     * 静态碰撞豁免 (BulletNoClipHook.onCalcPhysics 读): 激活期或宽限期内生效。
     * 宽限期 = 退出接管后短暂保持可穿, 让玩家把车开出墙体再恢复碰撞。
     */
    public static boolean isPhysicsNoClip() {
        return INSTANCE.state != STATE_IDLE
                || System.currentTimeMillis() < INSTANCE.pendingNoClipOffMs
                || combatNoClipManual;
    }

    // ================================================================
    // 战斗攻击三开关 (载具页「战斗攻击」模块, 2026-09-08):
    // 自动导航期间三项恒定开启 (现有导航行为不变); 开关只控制手动驾驶时的可用性。
    // ================================================================
    private static boolean combatWiggleManual;      // 蠕动秒杀: 手动驾驶驻车被围时前后蠕动
    private static boolean combatZombieKillManual;  // 汽车秒杀: 手动驾驶撞僵尸即死 + 不减速
    private static boolean combatNoClipManual;      // 汽车穿墙: 手动驾驶静态碰撞豁免

    // 蠕动锚定限位 (卅六): 时间交替的前后动力不对称 (实测净后移), 只靠节拍不可控 →
    // 进入蠕动时记锚点, 纵向偏移超 ±WIGGLE_RANGE 立即强制换向, 位置限位是唯一硬保证。
    private boolean wiggleAnchored;
    private float wiggleAnchorX, wiggleAnchorY;
    private boolean manualWiggleOn;                 // 本帧手动蠕动接管中 (秒杀门控)
    private static final float WIGGLE_RANGE = 2.0f; // 蠕动前后范围 (锚点±, 格; 6km/h 每相位移~1.7格, 仿真 S3)

    /** 汽车秒杀总门控 (BulletNoClipHook 僵尸伤害/冲量钩子):
     * 导航中 / 手动秒杀开关 / **蠕动秒杀激活** (蠕动含秒杀, 用户卅六需求)。 */
    public static boolean isZombieKillActive() {
        return INSTANCE.state != STATE_IDLE || combatZombieKillManual || INSTANCE.manualWiggleOn;
    }

    /** 蠕动方向决策 (导航 BLOCKED 与手动共用): 锚点限位优先, 范围内按 1s 节拍。 */
    private boolean wiggleForward(BaseVehicle vehicle, long now) {
        if (!wiggleAnchored) {
            wiggleAnchorX = vehicle.getX();
            wiggleAnchorY = vehicle.getY();
            wiggleAnchored = true;
        }
        Vector3f fwd = vehicle.getForwardVector(fwdVec);
        float lon = (vehicle.getX() - wiggleAnchorX) * fwd.x
                + (vehicle.getY() - wiggleAnchorY) * fwd.z;
        if (lon <= -WIGGLE_RANGE) {
            return true;    // 顶到后边界 → 前进
        }
        if (lon >= WIGGLE_RANGE) {
            return false;   // 顶到前边界 → 后退
        }
        return ((now / 1000L) & 1L) == 0L;   // 范围内: 时间节拍
    }

    public static boolean isCombatWiggle() { return combatWiggleManual; }
    public static boolean isCombatZombieKill() { return combatZombieKillManual; }
    public static boolean isCombatNoClip() { return combatNoClipManual; }

    public static void setCombatWiggle(boolean v) { combatWiggleManual = v; }
    public static void setCombatZombieKill(boolean v) { combatZombieKillManual = v; }

    /** 汽车穿墙开关: 切换即按目标态重传已加载 chunk 物理层 (开=过滤版/关=原版)。 */
    public static void setCombatNoClip(boolean v) {
        combatNoClipManual = v;
        INSTANCE.refreshWorldNoClip();
    }

    // ================================================================
    // 注入入口
    // ================================================================

    private boolean handleControls(BaseVehicle vehicle) {
        try {
            // 宽限期到期收尾: 恢复原版静态碰撞 (手动驾驶每帧仍进这里, on-foot 由
            // isActive 轮询兜底; 宽限期内玩家应已驶离墙体)
            if (state == STATE_IDLE && worldNoClipApplied
                    && System.currentTimeMillis() >= pendingNoClipOffMs) {
                refreshWorldNoClip();
            }
            if (state == STATE_IDLE) {
                return manualWiggle(vehicle);   // 战斗攻击: 手动蠕动 (让位原版则 false)
            }
            if (vehicle == null) {
                deactivate();
                return false;
            }
            CarController cc = vehicle.getController();
            if (cc == null) {
                deactivate();
                return false;
            }
            // 授权司机判定: 非本地玩家驾驶 (下车/换座) → 天然失效 (§五 自动取消)
            IsoGameCharacter driver = vehicle.getDriver();
            if (!(driver instanceof IsoPlayer) || !((IsoPlayer) driver).isLocalPlayer()) {
                deactivate();
                return false;
            }
            // 接管判定: 原始键态, 按键当帧取消并放行原版键位读取 (§五)
            if (takeoverRequested(vehicle)) {
                cancel("UI_DrivePanel_MsgTakeover");
                return false;
            }

            long now = System.currentTimeMillis();
            lastHandleMs = now;
            float dtMs = lastTickMs <= 0 ? 50f : Math.min(500f, now - lastTickMs);
            lastTickMs = now;
            tick(vehicle, dtMs, now);
            if (state == STATE_IDLE) {
                // 刚取消: 本帧显式清零 clientControls (无残留油门), 下帧起原版路径
                CarController.ClientControls c = cc.clientControls;
                c.steering = 0.0f;
                c.forward = false;
                c.backward = false;
                c.brake = false;
                c.shift = false;
                return true;
            }
            writeControls(cc);
            return true;
        } catch (Throwable t) {
            // 任何异常都不允许堵死驾驶: 退出接管, 原版路径兜底
            Logger.error("[AutoDrive] tick failed, deactivating", t);
            deactivate();
            return false;
        }
    }

    /** 接管: 任意驾驶键 (前进/后退/左右/手刹) 抢回操控 (键盘与手柄双路径)。 */
    /** 战斗攻击 (手动蠕动): 开关开 + 玩家无驾驶输入 + 车近乎静止 → 前后蠕动
     * (锚点限位 ±1.5 格 + 1s 节拍, 8km/h 上限), 蠕动激活期间僵尸碰车即死
     * (isZombieKillActive 含 manualWiggleOn, 蠕动秒杀内含汽车秒杀);
     * 玩家踩任何驾驶键 = 立即让位原版 (takeoverRequested)。 */
    private boolean manualWiggle(BaseVehicle vehicle) {
        if (!combatWiggleManual || vehicle == null) {
            manualWiggleOn = false;
            return false;
        }
        CarController cc = vehicle.getController();
        if (cc == null || takeoverRequested(vehicle)) {
            manualWiggleOn = false;
            return false;
        }
        if (Math.abs(vehicle.getCurrentSpeedKmHour()) > 8f) {
            manualWiggleOn = false;
            return false;
        }
        if (!manualWiggleOn) {
            wiggleAnchored = false;     // 本次蠕动刚接手 → 重新锚定
        }
        manualWiggleOn = true;
        boolean goFwd = wiggleForward(vehicle, System.currentTimeMillis());
        CarController.ClientControls c = cc.clientControls;
        c.steering = 0.0f;
        c.forward = goFwd;
        c.backward = !goFwd;
        c.brake = false;
        c.shift = false;
        return true;
    }

    private boolean takeoverRequested(BaseVehicle vehicle) {
        if (vehicle.isKeyboardControlled()) {
            return GameKeyboard.isKeyDown("Forward") || GameKeyboard.isKeyDown("Backward")
                    || GameKeyboard.isKeyDown("Left") || GameKeyboard.isKeyDown("Right")
                    || GameKeyboard.isKeyDown("Brake");
        }
        int joypad = vehicle.getJoypad();
        if (joypad != -1) {
            return JoypadManager.instance.isRTPressed(joypad) || JoypadManager.instance.isLTPressed(joypad)
                    || JoypadManager.instance.isBPressed(joypad)
                    || Math.abs(JoypadManager.instance.getMovementAxisX(joypad)) > 0.5f;
        }
        return true; // 既非键盘也非手柄 (AI 载具等) → 直接取消
    }

    private void writeControls(CarController cc) {
        CarController.ClientControls c = cc.clientControls;
        c.steering = ctlSteer;
        c.forward = ctlForward;
        c.backward = ctlBackward;
        c.brake = ctlBrake;
        c.shift = false; // 原版定速巡航不混入 (自主油门闭环, 研判 §二 纵向控制档 1)
    }

    // ================================================================
    // 状态机 tick
    // ================================================================

    private void tick(BaseVehicle vehicle, float dtMs, long now) {
        float speed = vehicle.getCurrentSpeedKmHour();
        float absSpeed = Math.abs(speed);

        // 感知节拍: 按时长门控, 全状态共用 (等待期扫描照跑 → 检测边界前推自动续驶)
        if (now - lastScanMs >= SCAN_PERIOD_MS) {
            scan(vehicle, absSpeed);
            scanObstacle(vehicle, absSpeed);   // 避障感知: 前方走廊挡路车 (廿八)
            lastScanMs = now;
        }

        if (state == STATE_WAIT_LOAD) {
            waitTick(vehicle, dtMs, absSpeed, now);
            return;
        }
        if (state == STATE_BRAKE_TO_BOUNDARY) {
            ctlForward = false;
            ctlBackward = false;
            ctlBrake = absSpeed > 0.3f;
            ctlSteer = 0;
            if (absSpeed <= 0.3f) {
                setState(STATE_WAIT_LOAD, "UI_DrivePanel_StatusWaitLoad");
            }
            return;
        }
        driveTick(vehicle, dtMs, absSpeed, now);
    }

    // ---------------- DRIVING ----------------

    private void driveTick(BaseVehicle vehicle, float dtMs, float absSpeed, long now) {
        // 引擎持续熄火 (油尽/报废) → 取消; 点火瞬间 (Idle) 由 5s 窗口容忍
        if (vehicle.getEngineState() != BaseVehicle.engineStateTypes.Running) {
            engineDeadMs += dtMs;
            if (engineDeadMs > ENGINE_DEAD_MS) {
                cancel("UI_DrivePanel_MsgEngineDead");
                return;
            }
        } else {
            engineDeadMs = 0;
        }

        // 到达: 距目标 ≤ ARRIVE_RADIUS **或已越过最近点** 判到达, 刹停即结束 (§五)。
        // 最近点追踪: 绕行残留横向偏移 (~8格) > 到达半径 (4格) 时距离判定永不满足,
        // 车冲过终点继续狂奔 (仿真 S5 实测冲过 300 格) — 过最近点 (minDist<12 且
        // 开始远离) 即触发刹停。
        float distToTarget = dist(vehicle.getX(), vehicle.getY(), targetX, targetY);
        if (distToTarget < minDistToTarget) {
            minDistToTarget = distToTarget;
        }
        if (!arrivalBrake && (distToTarget <= ARRIVE_RADIUS
                || (minDistToTarget < 12.0f && distToTarget > minDistToTarget + 2.0f))) {
            arrivalBrake = true;
            log("arrival brake engaged (min dist=" + (int) minDistToTarget + ")");
        }
        if (arrivalBrake) {
            ctlForward = false;
            ctlBackward = false;
            ctlBrake = absSpeed > 0.4f;
            ctlSteer = stanleySteer(vehicle, absSpeed);   // 边刹边回线 (绕行残留偏移, 仿真 S5)
            if (absSpeed <= 0.4f) {
                setState(STATE_ARRIVED, "UI_DrivePanel_MsgArrived");
                deactivate();
            }
            return;
        }

        // 路点推进 + 路线尽头 → 向前续段 (一次性路线语义: 线只向前生长, 不重规划)
        advancePath(vehicle);
        if (path == null || pathIdx >= path.size()) {
            planRoute(vehicle, now, true, false);
            if (path == null || pathIdx >= path.size()) {
                // 暂无可延长路径 (等加载/被围): 减速滑行等待下次冷却
                ctlForward = false;
                ctlBrake = absSpeed > 5f;
                ctlSteer = 0;
                return;
            }
        }

        // ===== 动态避障 (车辆/残骸 waypoint detour, 卅一) =====
        // MP 下车-车碰撞服务端权威 → 唯一真防线 = 别碰上: 感知在 scanObstacle,
        // 此处每帧决策: 生成绕行目标点 (障碍侧后方空地) 朝其直插, 过车尾回线。
        float avoidCap = updateAvoidance(vehicle, now);
        if (avoidMode == AVOID_BLOCKED) {
            // 双向堵死: 接近段 (>10km/h) 先 IDM gap 刹停 — 锚定设在降速后 (检出点
            // 锚定会被 50km/h 惯性滑行冲穿, 仿真 S3 drift 20+ 格教训); 降速后蠕动
            // (锚定 ±2 格 + 1s 节拍, 6km/h 低速, 车不动但驱动僵尸远离门窗)。
            // 阈值 10 > 蠕动峰值 9km/h: 蠕动自身加速不触发清锚 (否则锚点反复重置
            // 净倒车漂移, 仿真 S3 二轮教训)。
            if (absSpeed > 10f) {
                wiggleAnchored = false;
                float vmsB = absSpeed / 3.6f;
                float sGap = Float.isNaN(obsLon) ? 5.0f
                        : Math.max(obsLon - IDM_GAP_CLEAR, 0.5f);
                float sStarB = IDM_S0 + vmsB * IDM_T
                        + vmsB * vmsB / (2.0f * (float) Math.sqrt(IDM_A_MAX * IDM_B));
                float accelB = Math.max(-IDM_A_MAX * (sStarB / sGap) * (sStarB / sGap),
                        IDM_ACCEL_MIN);
                updateThrottle(vmsB, 6.0f / 3.6f, accelB, dtMs);
                ctlSteer = 0.0f;
                return;
            }
            ctlForward = wiggleForward(vehicle, now);
            ctlBackward = !ctlForward;
            ctlBrake = absSpeed > 6f;
            ctlSteer = 0.0f;
            return;
        }
        wiggleAnchored = false;     // 非蠕动态: 锚点失效

        // ===== 纵向: 弯道剖面 + 偏航安全网 + 边界 + 绕行限速 =====
        // 静态物穿墙, 僵尸碾杀 (战损钩子), 车辆/残骸绕行 (本层)。
        float vehicleMax = Math.max(vehicle.getMaxSpeed(), 20.0f);
        float cruise = cruiseSpeed > 0 ? Math.min(cruiseSpeed, vehicleMax)
                : Math.min(Math.min((float) ServerOptions.instance.speedLimit.getValue()
                        * SPEED_LIMIT_MARGIN, vehicleMax), CRUISE_ADAPTIVE);
        float v0 = Math.min(cruise, cornerRefSpeed(vehicle, cruise));
        v0 = Math.min(v0, avoidCap);   // 绕行限速 (转向物理余量)

        // 偏航安全网 (Stanley 收敛失败时兜底, 阈值放宽)
        float cte = crossTrackError(vehicle);
        if (cte > 4.0f) {
            v0 = Math.min(v0, 10.0f);
        } else if (cte > 2.5f) {
            v0 = Math.min(v0, 18.0f);
        }

        // 未加载边界刹停线: 制动距离 + 4 格富余 (停于 3×3 物理守卫带内)
        float brakeDist = brakingDistance(absSpeed);
        if (boundaryDist <= brakeDist + 4.0f) {
            setState(STATE_BRAKE_TO_BOUNDARY, "UI_DrivePanel_StatusBrakeToBoundary");
            return;
        }

        // ===== 执行器: 速度死区 bang-bang + 刹车闩锁 =====
        // accel = a_max·[1-(v/v0)^4 - (s*/s)²]: 自由流项 + **虚拟前车 gap 项** (卅八)。
        // gap: 走廊内 (|lat|<3.5) 障碍 = 静止前车, s = lon-3.5, Δv = v;
        // s→s0 ⇒ accel 深负必刹 — 距离收敛到 0 ⇔ 速度收敛到 0, 与横向绕行层
        // 是否正常无关 (ACC/AEB 层叠思路: 横向负责绕, 纵向保命)。
        // 绕行横向拉开 (|lat|≥3.5) gap 自动退出, 不拖累绕过后提速。
        // 卌四: 绕行/并入期交给游戏巡航 (regulator) — 油门缓升缓降、从不刹车 = 丝滑;
        // 旧 bang-bang 在 10km/h 目标下加速→超调→刹车→欠调→加速 = 一顿一顿 (实测)。
        boolean gapActive = !Float.isNaN(obsLon) && Math.abs(obsLat) < IDM_GAP_CORRIDOR;
        if (avoidMode == AVOID_DETOUR || avoidMode == AVOID_RETURN) {
            vehicle.setRegulator(true);
            vehicle.setRegulatorSpeed(AVOID_SPEED_CAP);
            ctlForward = false;
            ctlBrake = false;
        } else if (v0 < cruise - 2.0f && !gapActive) {
            // 卌九: 弯道剖面/偏航兜底把目标压到低速时, bang-bang 死区相对占比暴涨 →
            // 走-刹振荡 (实测)。同避障卌四款: 游戏巡航恒速丝滑; min(10, v0) 尊重极锐
            // 弯剖面。gap 激活时不切 — 保命层需 IDM 刹停 (车-车服务端权威), 绕开自动回归。
            vehicle.setRegulator(true);
            vehicle.setRegulatorSpeed(Math.min(AVOID_SPEED_CAP, v0));
            ctlForward = false;
            ctlBrake = false;
        } else {
            if (vehicle.isRegulator()) vehicle.setRegulator(false);
            float vms = absSpeed / 3.6f;
            float v0ms = Math.max(v0 / 3.6f, 0.5f);
            float accel = IDM_A_MAX * (1.0f - (float) Math.pow(vms / v0ms, 4.0f));
            if (!Float.isNaN(obsLon) && Math.abs(obsLat) < IDM_GAP_CORRIDOR) {
                float sGap = Math.max(obsLon - IDM_GAP_CLEAR, 0.5f);
                float sStar = IDM_S0 + vms * IDM_T
                        + vms * vms / (2.0f * (float) Math.sqrt(IDM_A_MAX * IDM_B));
                accel -= IDM_A_MAX * (sStar / sGap) * (sStar / sGap);
            }
            updateThrottle(vms, v0ms, accel, dtMs);
        }

        // ===== 横向: 绕行模式 = 朝目标点直插 / 正常 = Stanley 跟线 =====
        // waypoint detour: 绕行中不跟路径, 直接朝绕行目标点 (障碍侧后方空地) 开,
        // 静态物穿墙所以目标点只需避开车辆; 障碍过车尾后回路径 (updateAvoidance 切换)。
        if (avoidMode == AVOID_DETOUR && !Float.isNaN(avoidTargetX)) {
            ctlSteer = steerToPoint(vehicle, absSpeed, avoidTargetX, avoidTargetY);
        } else if (avoidMode == AVOID_RETURN) {
            // 卌五: RETURN 改用 Stanley (和 NONE 期同) — 旧纯追踪增益 2.0 饱和满舵,
            // 大横向偏差时打满方向 → 过冲 → 开回头 → 重新接敌 (实测 f:3600 RETURN → f:3840 DETOUR)。
            // Stanley 兼顾横向偏差 + 航向角, 近线后自然收敛不振荡。
            ctlSteer = stanleySteer(vehicle, absSpeed);
            if (Math.abs(crossTrackError(vehicle)) < AVOID_RETURN_CTE_EXIT) {
                avoidMode = AVOID_NONE;
            }
        } else {
            ctlSteer = stanleySteer(vehicle, absSpeed);
        }
    }

    /** 朝目标点直插 (waypoint detour 转向, 单点制导)。 */
    private float steerToPoint(BaseVehicle vehicle, float absSpeedKmh, float tx, float ty) {
        float err = wrapPi((float) Math.atan2(ty - vehicle.getY(), tx - vehicle.getX()) - heading(vehicle));
        return clamp(err * 2.0f, -1.0f, 1.0f);
    }

    /**
     * 弯道参考速 (速度剖面, velocity profiling 标准法): 沿路径自 pathIdx 前向
     * 采样 (步长 PROFILE_DS), 每采样点用三点外接圆估曲率 → v = √(A_LAT_MAX·R);
     * 后向传递 v_j = min(v_j, √(v_{j+1}² + 2·A_PLAN_BRAKE·Δs)) 保证"到弯前能刹
     * 到弯速"。返回当前位置的参考速 (km/h)。目标速由前方几何决定: 距离连续、
     * 平滑、有预见 — 这是替代"看当前角误差限速"的根治。
     */
    private float cornerRefSpeed(BaseVehicle vehicle, float capKmh) {
        if (path == null || pathIdx >= path.size()) return capKmh;
        // 采样: 从 pathIdx 对应的路点起 (不是车辆实际位置 — 穿墙/偏离时"车→路点"
        // 对角线段会造出假曲率尖峰 → v0 被砸到地板, 实测"穿墙非常慢"根因之一)
        ArrayList<float[]> pts = new ArrayList<float[]>(24);
        float acc = 0;
        float[] prev = path.get(pathIdx);
        outer:
        for (int i = pathIdx; i < path.size(); i++) {
            float[] wp = path.get(i);
            float seg = dist(prev[0], prev[1], wp[0], wp[1]);
            // 长段内插补采样点
            float t0 = 0;
            while (acc + (seg - t0) >= PROFILE_DS) {
                float need = PROFILE_DS - acc;
                float f = (t0 + need) / Math.max(seg, 0.001f);
                pts.add(new float[]{prev[0] + (wp[0] - prev[0]) * f,
                        prev[1] + (wp[1] - prev[1]) * f});
                t0 += need;
                acc = 0;
                if (pts.size() > 64) break outer;
            }
            acc += seg - t0;
            prev = wp;
        }
        int n = pts.size();
        if (n < 3) return capKmh;
        // 曲率 → 限速 (m/s), 外接圆 R = abc/(4A)
        float[] vms = new float[n];
        for (int j = 1; j < n - 1; j++) {
            float[] p0 = pts.get(j - 1), p1 = pts.get(j), p2 = pts.get(j + 1);
            float a = dist(p0[0], p0[1], p1[0], p1[1]);
            float b = dist(p1[0], p1[1], p2[0], p2[1]);
            float c = dist(p0[0], p0[1], p2[0], p2[1]);
            float area = Math.abs((p1[0] - p0[0]) * (p2[1] - p0[1])
                    - (p2[0] - p0[0]) * (p1[1] - p0[1])) * 0.5f;
            if (area < 1e-4f) {
                vms[j] = Float.MAX_VALUE;
                continue;
            }
            float r = (a * b * c) / (4.0f * area);
            // 剖面种子 = 物理弯速 - 死区带宽 (给跟踪滞后留余量: 实际到弯速 ≈ √(a·R))
            vms[j] = (float) Math.max(0.0, Math.sqrt(A_LAT_MAX * r) - THROTTLE_BAND_MS);
        }
        vms[0] = Float.MAX_VALUE;
        vms[n - 1] = Float.MAX_VALUE;
        // 后向传递 (从远到近): 保证物理上刹得到
        for (int j = n - 2; j >= 0; j--) {
            vms[j] = Math.min(vms[j],
                    (float) Math.sqrt(vms[j + 1] * vms[j + 1] + 2.0f * A_PLAN_BRAKE * PROFILE_DS));
        }
        // 当前参考速 = 样本 0 (车旁), 换 km/h 并封顶
        return Math.min(vms[0] == Float.MAX_VALUE ? capKmh : vms[0] * 3.6f, capKmh);
    }

    // ===== 执行器状态机 (速度死区 bang-bang, 数值仿真 temp/longitudinal_sim.ps1 同构) =====

    private static final int THROTTLE_FWD = 0;
    private static final int THROTTLE_COAST = 1;
    private static final int THROTTLE_BRAKE = 2;
    /** 死区半宽 (m/s): ±2 km/h — 巡航 20 时 22 收油滑行, 18 重新给油, 不点刹 (用户实测: 旧 ±5 刹到 15 重启 = 刹停感)。 */
    private static final float THROTTLE_BAND_MS = 2.0f / 3.6f;
    /** v 超剖面 (2×BAND) m/s → 刹车 (真超速, 滑行追不回的陡降); 常规超速由滑行自然回落。 */
    private static final float THROTTLE_BRAKE_GAP_MS = 4.0f / 3.6f;
    private static final long THROTTLE_MIN_DWELL_MS = 800;   // FWD↔COAST 防颤振
    private static final long THROTTLE_BRAKE_DWELL_MS = 400; // 刹车出口驻留 (短)

    private int throttleState = THROTTLE_COAST;
    private long throttleDwellMs;

    /** 油门/刹车执行器状态机 (v, vCmd, IDM acc 均 m/s 制)。 */
    private void updateThrottle(float v, float vCmd, float accel, float dtMs) {
        boolean brakeWant = accel < -1.5f || v > vCmd + THROTTLE_BAND_MS + THROTTLE_BRAKE_GAP_MS;
        int next = throttleState;
        // 刹车进入即时 (安全态不受驻留节流)
        if (brakeWant && throttleState != THROTTLE_BRAKE) {
            next = THROTTLE_BRAKE;
        } else {
            switch (throttleState) {
                case THROTTLE_FWD:
                    if (throttleDwellMs >= THROTTLE_MIN_DWELL_MS && v >= vCmd + THROTTLE_BAND_MS) {
                        next = THROTTLE_COAST;
                    }
                    break;
                case THROTTLE_BRAKE:
                    if (throttleDwellMs >= THROTTLE_BRAKE_DWELL_MS && v <= vCmd + 0.3f) {
                        next = THROTTLE_COAST;   // 紧出口: 末端滑行兜得住
                    }
                    break;
                default:    // COAST
                    if (throttleDwellMs >= THROTTLE_MIN_DWELL_MS && v < vCmd - THROTTLE_BAND_MS) {
                        next = THROTTLE_FWD;
                    }
                    break;
            }
        }
        if (next != throttleState) {
            throttleState = next;
            throttleDwellMs = 0;
        } else {
            throttleDwellMs += (long) dtMs;
        }
        ctlForward = throttleState == THROTTLE_FWD;
        ctlBackward = false;
        ctlBrake = throttleState == THROTTLE_BRAKE;
    }

    /**
     * Stanley 横向控制 (Hoffmann et al.,斯坦福 DARPA 挑战赛):
     * δ = φ + atan2(k·e, v), 返回归一化舵量 [-1,1]。
     * 投影窗口自适应延伸 (起点 pathIdx-2, 加倍延伸上限 64 段)。
     * 绕行 (waypoint detour) 时不走本方法 — 由 steerToPoint 直插目标点 (卅一)。
     */
    private float stanleySteer(BaseVehicle vehicle, float absSpeedKmh) {
        if (path == null || path.isEmpty()) return 0.0f;
        int last = path.size() - 1;
        if (last == 0) {
            // 单点路径: 切向未定义 → 朝点转
            float[] p = path.get(0);
            float err = wrapPi((float) Math.atan2(p[1] - vehicle.getY(), p[0] - vehicle.getX()) - heading(vehicle));
            return clamp(err * 2.0f, -1.0f, 1.0f);
        }
        float x = vehicle.getX();
        float y = vehicle.getY();
        // 投影窗口自适应延伸: 掉头出口/进度滞后时窗口可能
        // 整体在车后方, 固定 +8 段不够 → 加倍延伸, 上限 64 段
        int segStart = Math.max(0, pathIdx - 2);
        int segEnd = Math.min(last - 1, segStart + 16);
        int bestSeg = -1;
        float bestT = 0f;
        float bestD2 = Float.MAX_VALUE;
        while (true) {
            for (int s = segStart; s <= segEnd; s++) {
                float[] a = path.get(s);
                float[] b = path.get(s + 1);
                float dx = b[0] - a[0], dy = b[1] - a[1];
                float l2 = dx * dx + dy * dy;
                float t = l2 > 0.0f ? ((x - a[0]) * dx + (y - a[1]) * dy) / l2 : 0.0f;
                t = t < 0.0f ? 0.0f : (t > 1.0f ? 1.0f : t);
                float px = a[0] + dx * t, py = a[1] + dy * t;
                float ex = x - px, ey = y - py;
                float d2 = ex * ex + ey * ey;
                // 平局偏后段 (<=): 直角弯处入段/出段到车距离打平时 (车正过弯点),
                // 旧严格 < 永远选入段 → 切向不切换 → 转弯迟滞 10+ 格外甩 (数值仿真
                // 抓出)。取后段 = 弯点即切, 转弯在弯点当帧启动。
                if (d2 <= bestD2) {
                    bestD2 = d2;
                    bestSeg = s;
                    bestT = t;
                }
            }
            // 窗口覆盖判定: 最近投影不在窗口末端 (或路径尾/达上限) → 收
            if (bestSeg < segEnd - 1 || segEnd >= last - 1 || segEnd - segStart >= 64) {
                break;
            }
            segEnd = Math.min(last - 1, segEnd + Math.max(8, segEnd - segStart));
        }
        if (bestSeg < 0) return 0.0f;
        float[] a = path.get(bestSeg);
        float[] b = path.get(bestSeg + 1);
        // 切向角 → 航向误差 φ
        float tangentAngle = (float) Math.atan2(b[1] - a[1], b[0] - a[0]);
        float phi = wrapPi(tangentAngle - heading(vehicle));
        // 有符号横向偏差 e = cross(切向, 车→投影点): 投影点在切向 +90° 侧 (atan2
        // 正角方向) 为正 → 需正舵回线。与 steer/heading 的角度符号空间一致。
        float px = a[0] + (b[0] - a[0]) * bestT;
        float py = a[1] + (b[1] - a[1]) * bestT;
        float tx = b[0] - a[0], ty = b[1] - a[1];
        float tl = (float) Math.sqrt(tx * tx + ty * ty);
        if (tl < 1e-4f) return clamp(phi * 2.0f, -1.0f, 1.0f);
        tx /= tl;
        ty /= tl;
        float e = tx * (py - y) - ty * (px - x);
        // 速度 m/s (世界格即米), 下限 0.8 防静止除零发散
        float v = Math.max(absSpeedKmh / 3.6f, 0.8f);
        // Stanley 误差项限幅 (原论文实践): 低速大偏差时 atan2 项可 >60°, 舵量饱和
        // → 车头来回过冲甩尾 (实测弯道摆头)。限 ±0.4rad 让航向项主导, 误差项只微调。
        float eTerm = (float) Math.atan2(K_STANLEY * e, v);
        eTerm = Math.max(-0.4f, Math.min(0.4f, eTerm));
        float delta = phi + eTerm;
        return clamp(delta * 2.0f, -1.0f, 1.0f);
    }

    // ---------------- WAIT_LOAD (黑边等待 + 自动续驶, §六) ----------------

    private void waitTick(BaseVehicle vehicle, float dtMs, float absSpeed, long now) {
        // 停在边界前 (物理活跃, 普通停车); 扫描照跑 (tick 头部已做)
        ctlForward = false;
        ctlSteer = 0;

        ctlBackward = false;
        ctlBrake = absSpeed > 0.3f;

        // 自动续驶: 边界前推 (新区块落地, 可见距离超过起步阈值) → 向前续段 → 起步
        if (boundaryDist > brakingDistance(30.0f) + 10.0f && planRoute(vehicle, now, true, false)) {
            setState(STATE_DRIVING, "UI_DrivePanel_StatusDriving");
            return;
        }

        // 服务器迟迟不给块 → 超时取消并提示
        if (now - stateSinceMs > BOUNDARY_TIMEOUT_MS) {
            cancel("UI_DrivePanel_MsgBoundaryTimeout");
        }
    }

    // ================================================================
    // 感知 (两层扫描, 150ms, §三补注)
    // ================================================================

    /**
     * 感知 (无脑跟线版, 2026-09-07 用户拍板): 只扫**未加载边界** — 前向第一个
     * getGridSquare 空洞 (客户端免费数据)。那是真悬崖 (没地面, 会掉出世界),
     * 不是障碍。僵尸/车辆/静态物一概不扫: 僵尸触之即死 (战损钩子), 静态物可穿
     * (物理豁免), 其他车辆顶开 (自伤为零)。
     */
    private void scan(BaseVehicle vehicle, float absSpeed) {
        boundaryDist = Float.MAX_VALUE;
        IsoCell cell = IsoWorld.instance.currentCell;
        if (cell == null) return;
        Vector3f fwd = vehicle.getForwardVector(fwdVec);
        float fx = fwd.x;
        float fy = fwd.z; // PZ: Vector3f.z 对应世界 y
        float vx = vehicle.getX();
        float vy = vehicle.getY();
        int zFloor = (int) Math.floor(vehicle.getZ());
        float corridor = Math.max(brakingDistance(absSpeed) * 1.5f + 4.0f, state == STATE_WAIT_LOAD ? 40.0f : 12.0f);
        int steps = (int) corridor;
        for (int i = 0; i <= steps; i++) {
            float px = vx + fx * i;
            float py = vy + fy * i;
            IsoGridSquare sq = cell.getGridSquare((int) Math.floor(px), (int) Math.floor(py), zFloor);
            if (sq == null) {
                boundaryDist = i;
                break;
            }
        }
    }

    // ================================================================
    // 动态避障 (车辆/残骸, lateral offset avoidance — 量产 ACC 横向避让标准)
    // ================================================================

    /**
     * 障碍感知 + 绕行决策 (每 150ms 感知节拍随 scan 调用):
     * 扫描前方 AVOID_LOOKAHEAD 内其他车辆 → 取路径走廊内最近一辆为挡路障碍。
     * 挡路判定 = 障碍中心到路径投影段距离 < 车半宽和 (≈2 格走廊)。静态物不判
     * (照穿), 僵尸不判 (照碾) — 只处理车辆/残骸 (MP 碰撞服务端权威, 客户端唯一
     * 真防线 = 别碰上)。
     * 侧向选择 (卅七): 进入绕行时按障碍偏移符号选最小偏航侧并**锁定** (绕行中自车
     * 偏移会让障碍相对符号翻转, 不锁定则目标点来回跳 → 摆动撞车); 瞄准点纵向锚定
     * 障碍自身位置 (AVOID_MIN_AHEAD 钳底), 侧向偏移 AVOID_LAT_OFFSET; 障碍过车尾
     * AVOID_CLEAR_DIST 才回线 (无"目标点到达"早退)。
     */
    private void scanObstacle(BaseVehicle vehicle, float absSpeed) {
        float vx = vehicle.getX();
        float vy = vehicle.getY();
        float selfZ = vehicle.getZ();
        // 前瞻距离随速扩展 (高速要更早看到)
        float look = AVOID_LOOKAHEAD + brakingDistance(absSpeed);
        BaseVehicle best = null;
        float bestDist = Float.MAX_VALUE;
        int cx = (int) (vx / 8.0f);
        int cy = (int) (vy / 8.0f);
        for (int dy = -4; dy <= 4; ++dy) {
            for (int dx = -4; dx <= 4; ++dx) {
                IsoChunk chunk = IsoWorld.instance.currentCell.getChunk(cx + dx, cy + dy);
                if (chunk == null) continue;
                best = scanObstacleChunk(chunk.vehicles, vehicle, vx, vy, selfZ, look, best, bestDist == Float.MAX_VALUE ? Float.MAX_VALUE : bestDist);
                if (best != null) {
                    bestDist = dist(vx, vy, best.getX(), best.getY());
                }
            }
        }
        // cell 全量兜底 (生成车不一定在 chunk 登记 — 廿四实测)
        java.util.Set<BaseVehicle> cellVehicles = IsoWorld.instance.currentCell.getVehicles();
        for (BaseVehicle v : cellVehicles) {
            if (v == vehicle) continue;
            if (Math.abs(v.getZ() - selfZ) > 1.0f) continue;
            float d = dist(vx, vy, v.getX(), v.getY());
            if (d > look || d >= bestDist) continue;
            best = v;
            bestDist = d;
        }
        if (best != null) {
            // 路线横向门 (卌三): 障碍须贴近路线才值得绕 — 转向中航向扫过走廊
            // 误锁路外车 (实测 SmallCar02 lat=15) 会引发无谓绕行
            float[] proj = pathProject(best.getX(), best.getY());
            if (proj != null && proj[2] > 5.5f) best = null;
        }
        if (best != null) {
            avoidObstacleX = best.getX();
            avoidObstacleY = best.getY();
            if (!avoidDiagHit) {
                Logger.printLog("[AvoidDiag] obstacle: " + best.getScriptName()
                        + " at " + (int) best.getX() + "," + (int) best.getY()
                        + " dist=" + (int) dist(vehicle.getX(), vehicle.getY(), best.getX(), best.getY())
                        + " look=" + (int) look);
                avoidDiagHit = true;
            }
        } else {
            // 障碍锁定 (hysteresis): 绕行期间扫描丢失 (z 波动/chunk 边界/车已绕到
            // 障碍侧面横向偏出走廊) 不清状态 — 保持旧坐标继续绕, 清除只由
            // updateAvoidance 的"过车尾"判定负责 (实测: 找到→丢失→找到 抖动,
            // 绕行刚激活即被打断 → 直行撞上)。
            if (avoidDiagHit) {
                Logger.printLog("[AvoidDiag] obstacle lost");
                avoidDiagHit = false;
            }
        }
    }

    /** chunk 车辆列表内的挡路筛选 (返回更近者或原 best)。 */
    private BaseVehicle scanObstacleChunk(java.util.ArrayList<BaseVehicle> list, BaseVehicle self,
            float vx, float vy, float selfZ, float look, BaseVehicle best, float bestDist) {
        for (int i = 0; i < list.size(); ++i) {
            BaseVehicle v = list.get(i);
            if (v == self) continue;
            if (Math.abs(v.getZ() - selfZ) > 1.0f) continue;
            float d = dist(vx, vy, v.getX(), v.getY());
            if (d > look || d >= bestDist) continue;
            if (!isInCorridor(v, self)) continue;
            best = v;
            bestDist = d;
        }
        return best;
    }

    /** 障碍挡路判定: 自车前进走廊与障碍包围圆相交 (圆半径=车长半 ≈2 格, 覆盖横向车)。 */
    private boolean isInCorridor(BaseVehicle obs, BaseVehicle self) {
        float ox = obs.getX() - self.getX();
        float oy = obs.getY() - self.getY();
        // 纵向: 自车朝向分量 (前向才算挡路)
        Vector3f fwd = self.getForwardVector(fwdVec);
        float lon = ox * fwd.x + oy * fwd.z;
        if (lon < -2.0f) {   // 纵向上限不设: scanObstacle 的欧氏 d>look 已过滤 (lon<=d), 双重过滤曾致边界抖动
            return false;
        }
        // 横向: 障碍圆与自车走廊相交判定 — 走廊半宽 = 自车半宽(1.5) + 障碍包围半径(2.0)。
        // 旧实现 corridorHalf=2.2 只看中心点, 横向车/残骸中心偏出即漏判 → 直接撞 (实测)。
        float lat = ox * fwd.z - oy * fwd.x;
        float corridorHalf = 3.5f;   // 1.5 自车半宽 + 2.0 障碍包围半径 (车长约 4 格)
        return Math.abs(lat) < corridorHalf;
    }

    /**
     * 检查目标点是否被其他车辆/残骸占用 (静态物不查 — 反正穿墙)。半径 3 格。
     */
    private boolean pointClearOfVehicles(float px, float py, BaseVehicle self) {
        int cx = (int) (px / 8.0f);
        int cy = (int) (py / 8.0f);
        for (int dy = -1; dy <= 1; ++dy) {
            for (int dx = -1; dx <= 1; ++dx) {
                IsoChunk chunk = IsoWorld.instance.currentCell.getChunk(cx + dx, cy + dy);
                if (chunk == null) continue;
                for (int i = 0; i < chunk.vehicles.size(); ++i) {
                    BaseVehicle v = chunk.vehicles.get(i);
                    if (v == self) continue;
                    if (Math.abs(v.getZ() - self.getZ()) > 1.0f) continue;
                    if (dist(px, py, v.getX(), v.getY()) < 3.0f) return false;
                }
            }
        }
        // cell 全量兜底 (生成车不一定在 chunk 登记 — 廿四实测)
        for (BaseVehicle v : IsoWorld.instance.currentCell.getVehicles()) {
            if (v == self) continue;
            if (Math.abs(v.getZ() - self.getZ()) > 1.0f) continue;
            if (dist(px, py, v.getX(), v.getY()) < 3.0f) return false;
        }
        return true;
    }

    /**
     * 绕行决策 (waypoint detour, 卅一): 前方走廊有挡路车 →
     * ① 算障碍两侧候选绕行目标点 = 障碍中心 + 侧向偏移 4 格 + 前向偏移 6 格
     *    (朝障碍侧后方空地直插, 静态物反正穿墙, 目标点只需避开车辆);
     * ② 首选障碍偏外侧 (障碍偏右 → 从左绕), 该侧被其他车占用 → 换另一侧;
     * ③ 两侧都占用 (整路被车堵死) → AVOID_BLOCKED (前后蠕动, 用户要求不刹停);
     * ④ 障碍过车尾 (AVOID_CLEAR_DIST) → 回路径。
     * 返回绕行限速 km/h (无绕行 = Float.MAX_VALUE)。
     */
    private float updateAvoidance(BaseVehicle vehicle, long now) {
        if (avoidMode == AVOID_RETURN) {
            // 并入段: 模式不受障碍影响, 由 cte 退出 (卌一); 但扫描期若重新锁到
            // 障碍, 保持 obsLon/obsLat 新鲜 — IDM gap 纵向保命网并入期也活着 (卌三)
            if (!Float.isNaN(avoidObstacleX)) {
                Vector3f fr = vehicle.getForwardVector(fwdVec);
                float orx = avoidObstacleX - vehicle.getX();
                float ory = avoidObstacleY - vehicle.getY();
                obsLon = orx * fr.x + ory * fr.z;
                obsLat = orx * fr.z - ory * fr.x;
            } else {
                obsLon = Float.NaN;
                obsLat = Float.NaN;
            }
            return AVOID_SPEED_CAP;
        }
        boolean hasObstacle = !Float.isNaN(avoidObstacleX);
        if (!hasObstacle) {
            if (avoidMode == AVOID_DETOUR) {
                // 绕行中锁丢 (不应发生, 保险): 也走并入段而非硬切 Stanley
                avoidMode = AVOID_RETURN;
                return AVOID_SPEED_CAP;
            }
            avoidMode = AVOID_NONE;
            return Float.MAX_VALUE;
        }
        // 障碍相对自车: 纵向 lon / 横向 lat (自车坐标系; 存字段供 IDM gap 纵向兜底)
        Vector3f fwd = vehicle.getForwardVector(fwdVec);
        float ox = avoidObstacleX - vehicle.getX();
        float oy = avoidObstacleY - vehicle.getY();
        float lon = ox * fwd.x + oy * fwd.z;
        float lat = ox * fwd.z - oy * fwd.x;
        obsLon = lon;
        obsLat = lat;
        // 障碍已在车尾后方 → 绕行完成, 回路径
        if (lon < -AVOID_CLEAR_DIST) {
            // 障碍已远在车后 → 并入段 (卌一: 旧硬切 NONE, e≈8 时低速大偏差 →
            // Stanley 转弯过猛 → 偏移 → 再回线 → 振荡循环, 实测)
            avoidMode = AVOID_RETURN;
            avoidObstacleX = Float.NaN;
            avoidObstacleY = Float.NaN;
            obsLon = Float.NaN;
            obsLat = Float.NaN;
            return AVOID_SPEED_CAP;
        }
        // ===== 固定参考系 waypoint pair (卌, 仿真 S1-S6 全绿) =====
        // 卅八胡萝卜的致命缺陷: 横向偏移垂直于**当前车头** — 车追点 → 车头转 →
        // 点跟着移 → 再追 = 追逐自己的尾巴, 绕过障碍后在障碍后方走出长方形闭环
        // (实测; 日志佐证: 障碍 lat=-14 早不挡路, DETOUR 仍维持)。
        // 修复: 进入 DETOUR 锁定 detourDir (当时的行进方向), 全程固定几何:
        //   wpA = 障碍 ± perp(dir)·LAT_OFFSET           (侧向通过点)
        //   wpB = wpA + dir·AVOID_CLEAR_DIST             (障碍后方, 保持偏移)
        // 车纵向未过障碍 → 追 wpA; 过了 → 追 wpB; wpB 到达 (<3 格) → NONE 回 Stanley。
        // 侧锁定 (卅七): 侧只在进入 DETOUR 时选一次 (最小偏航侧), 过障碍前不变。
        if (avoidMode == AVOID_NONE) {
            // 参考系只在全新绕行时锁一次 (卌二), 且取**路线切向**而非当前车头 (卌三):
            // 锁航向的缺陷 = 转向/纠偏中航向斜指时, 绕行几何整体斜置 → "过障碍"
            // 判定沿斜轴, 实际没过 → 回线后障碍仍在正前方 → 反向重绕 → 来回乱开
            // (实测: side=+ 绕后回线又 side=- 绕, target 随车头旋转)。路线切向 =
            // 道路方向, 几何永远与行驶方向对齐; 无路线 (直线兜底) 退回车头。
            float[] tan = pathProject(avoidObstacleX, avoidObstacleY);
            if (tan != null) {
                detourDirX = tan[0];
                detourDirY = tan[1];
            } else {
                detourDirX = fwd.x;
                detourDirY = fwd.z;
            }
            avoidSide = lat >= 0.0f ? 1.0f : -1.0f;
        }
        float lonD = (vehicle.getX() - avoidObstacleX) * detourDirX
                + (vehicle.getY() - avoidObstacleY) * detourDirY;
        // 卌四: 已绕过的障碍不再重复接敌 — 并入期转向中车头扫过障碍时会把它
        // 重新扫进走廊 (实测 f:6737 obs lon=-7 身后障碍触发重绕), lonD>0 = 已过 → 清锁。
        if (avoidMode == AVOID_NONE && lonD > 0.0f) {
            avoidObstacleX = Float.NaN;
            avoidObstacleY = Float.NaN;
            obsLon = Float.NaN;
            obsLat = Float.NaN;
            avoidMode = AVOID_NONE;
            return Float.MAX_VALUE;
        }
        float[] sides = {avoidSide, -avoidSide};
        int mode = AVOID_BLOCKED;
        for (int i = 0; i < 2; ++i) {
            float side = sides[i];
            float wpAx = avoidObstacleX + (-detourDirY) * side * AVOID_LAT_OFFSET;
            float wpAy = avoidObstacleY + (detourDirX) * side * AVOID_LAT_OFFSET;
            float wpBx = wpAx + detourDirX * AVOID_CLEAR_DIST;
            float wpBy = wpAy + detourDirY * AVOID_CLEAR_DIST;
            if (!pointClearOfVehicles(wpBx, wpBy, vehicle)
                    || !pointClearOfVehicles(wpAx, wpAy, vehicle)) continue;
            avoidSide = side;      // 锁定侧被占则切另一侧 (并更新锁定)
            avoidWpBX = wpBx;
            avoidWpBY = wpBy;
            if (lonD < 0.0f) {
                // 未过障碍 → 追 wpA (提前建立侧偏, 到障碍处已全偏移)
                avoidTargetX = wpAx;
                avoidTargetY = wpAy;
                mode = AVOID_DETOUR;
            } else {
                // 已过 → 追 wpB (保持偏移); 到达 → RETURN 并入段
                // (卌一根因: 此处比较反了 — 过障碍后追**身后** wpA → 掉头 U-turn
                //  → 回线振荡循环, 实测; 仿真轨迹追踪确诊)
                if (dist(vehicle.getX(), vehicle.getY(), wpBx, wpBy) < 3.0f) {
                    // 进入 RETURN 必须清障碍锁 (卌二): 不清 → RETURN 结束后重进场,
                    // mode≠DETOUR 触发参考系重锁 (用车头) → 目标点随车头旋转 →
                    // 车追目标 = 绕障碍公转转圈 (实测); 旧锁的 obsLon 还会让 IDM
                    // gap 在并入后段 (|lat|<1.5 且 lon>-8) 以 -8 深刹
                    mode = AVOID_RETURN;
                    avoidTargetX = Float.NaN;
                    avoidObstacleX = Float.NaN;
                    avoidObstacleY = Float.NaN;
                    obsLon = Float.NaN;
                    obsLat = Float.NaN;
                } else {
                    avoidTargetX = wpBx;
                    avoidTargetY = wpBy;
                    mode = AVOID_DETOUR;
                }
            }
            break;
        }
        // 两侧都占用 → 堵死 (蠕动) ; 否则绕行
        if (mode != avoidDiagMode) {
            avoidDiagMode = mode;
            if (mode == AVOID_DETOUR) {
                Logger.printLog("[AvoidDiag] mode: DETOUR side=" + (avoidSide > 0 ? "+" : "-")
                        + " target=" + (int) avoidTargetX + "," + (int) avoidTargetY
                        + " obs lon=" + (int) lon + " lat=" + (int) lat);
            } else {
                Logger.printLog("[AvoidDiag] mode: "
                        + (mode == AVOID_BLOCKED ? "BLOCKED" : (mode == AVOID_RETURN ? "RETURN" : "NONE")));
            }
        }
        avoidMode = mode;
        return mode == AVOID_DETOUR ? AVOID_SPEED_CAP : Float.MAX_VALUE;
    }

    /** 绕行状态复位 (start/resumeRoute)。 */
    private void resetAvoidance() {
        avoidMode = AVOID_NONE;
        avoidTargetX = 0.0f;
        avoidTargetY = 0.0f;
        avoidObstacleX = Float.NaN;
        avoidObstacleY = Float.NaN;
        obsLon = Float.NaN;
        obsLat = Float.NaN;
    }

    // ================================================================
    // 路径与规划
    // ================================================================

    private void advancePath(BaseVehicle vehicle) {
        if (path == null) return;
        // 进度推进 (2026-09-07 十三, 路径跟踪标准做法): 前进时 pathIdx 随**车在
        // 路径上的投影段**单调推进 — 投影落在哪段, 进度就在哪段。倒车 (掉头机动/
        // 脱困) 期间冻结 (车尾朝路径, 投影不代表进度)。这取代旧的"3 格近邻圈 +
        // 越过判定"双补丁: 投影进度天然覆盖弯道斜切/冲过路口 (进度贴着几何走),
        // 掉头出口后进度直接落在车前方路段, 预瞄不再"在正后方"。
        if (vehicle.getCurrentSpeedKmHour() <= 0.5f) {
            return;
        }
        float x = vehicle.getX();
        float y = vehicle.getY();
        int last = path.size() - 1;
        int segStart = Math.max(0, pathIdx - 2);
        int segEnd = Math.min(last - 1, segStart + 32);
        int bestSeg = -1;
        float bestD2 = Float.MAX_VALUE;
        for (int s = segStart; s <= segEnd; s++) {
            float[] a = path.get(s);
            float[] b = path.get(s + 1);
            float dx = b[0] - a[0], dy = b[1] - a[1];
            float l2 = dx * dx + dy * dy;
            float t = l2 > 0.0f ? ((x - a[0]) * dx + (y - a[1]) * dy) / l2 : 0.0f;
            t = t < 0.0f ? 0.0f : (t > 1.0f ? 1.0f : t);
            float px = a[0] + dx * t, py = a[1] + dy * t;
            float ex = x - px, ey = y - py;
            float d2 = ex * ex + ey * ey;
            // 平局偏后段 (<=): 直角弯处入/出段打平时选后段
            if (d2 <= bestD2) {
                bestD2 = d2;
                bestSeg = s;
            }
        }
        if (bestSeg >= pathIdx) {
            // 单调: 投影段只会向前推 (投影在后 = 车倒回, 冻结不动)
            pathIdx = bestSeg;
        }
    }

    /** 车到路径中心线的横向偏差 (投影 pathIdx 前后几段), 供偏航限速。 */
    /** 点在路线上的投影 (卌三): {切向x, 切向y, 离线横向距离}; 无路线/退化 → null。
     *  搜索窗 = pathIdx 附近 (路点间距 ~25 格, 窗 -2..+8 段 ≈ 250 格覆盖足够)。 */
    private float[] pathProject(float px, float py) {
        if (path == null || path.size() < 2) return null;
        int last = path.size() - 1;
        int s0 = Math.max(0, pathIdx - 2);
        int s1 = Math.min(last - 1, pathIdx + 8);
        int bestSeg = -1;
        float bestD2 = Float.MAX_VALUE;
        for (int s = s0; s <= s1; ++s) {
            float[] a = path.get(s);
            float[] b = path.get(s + 1);
            float dx = b[0] - a[0];
            float dy = b[1] - a[1];
            float l2 = dx * dx + dy * dy;
            float t = l2 > 1e-6f ? clamp(((px - a[0]) * dx + (py - a[1]) * dy) / l2, 0.0f, 1.0f) : 0.0f;
            float ex = px - (a[0] + dx * t);
            float ey = py - (a[1] + dy * t);
            float d2 = ex * ex + ey * ey;
            if (d2 < bestD2) {
                bestD2 = d2;
                bestSeg = s;
            }
        }
        if (bestSeg < 0) return null;
        float[] a = path.get(bestSeg);
        float[] b = path.get(bestSeg + 1);
        float dx = b[0] - a[0];
        float dy = b[1] - a[1];
        float l = (float) Math.sqrt(dx * dx + dy * dy);
        if (l < 1e-3f) return null;
        return new float[]{dx / l, dy / l, (float) Math.sqrt(bestD2)};
    }

    /** 路径投影 (RETURN 并入用): {px, py, tx, ty} = 投影点 + 单位切向; 无路径段 → null。 */
    private float[] projectOnPath(BaseVehicle vehicle) {
        if (path == null || pathIdx >= path.size()) return null;
        float x = vehicle.getX();
        float y = vehicle.getY();
        float[] a = pathIdx == 0 ? new float[]{x, y} : path.get(pathIdx - 1);
        float[] b = path.get(pathIdx);
        float dx = b[0] - a[0];
        float dy = b[1] - a[1];
        float l2 = dx * dx + dy * dy;
        if (l2 < 1e-6f) return null;
        float t = clamp(((x - a[0]) * dx + (y - a[1]) * dy) / l2, 0.0f, 1.0f);
        float il = (float) Math.sqrt(l2);
        return new float[]{a[0] + dx * t, a[1] + dy * t, dx / il, dy / il};
    }

    private float crossTrackError(BaseVehicle vehicle) {
        if (path == null || pathIdx >= path.size()) return 0.0f;
        float x = vehicle.getX();
        float y = vehicle.getY();
        float[] prev = pathIdx == 0 ? new float[]{x, y} : path.get(pathIdx - 1);
        float best = Float.MAX_VALUE;
        int end = Math.min(path.size() - 1, pathIdx + 4);
        for (int i = pathIdx; i <= end; i++) {
            float[] cur = path.get(i);
            best = Math.min(best, pointSegmentDist(x, y, prev[0], prev[1], cur[0], cur[1]));
            prev = cur;
        }
        return best;
    }

    /** 点到线段最短距离。 */
    private static float pointSegmentDist(float px, float py, float ax, float ay, float bx, float by) {
        float dx = bx - ax;
        float dy = by - ay;
        float len2 = dx * dx + dy * dy;
        float t = len2 > 0.0f ? ((px - ax) * dx + (py - ay) * dy) / len2 : 0.0f;
        t = clamp(t, 0.0f, 1.0f);
        return dist(px, py, ax + dx * t, ay + dy * t);
    }

    /**
     * 路线规划 (一次性路线语义, 2026-09-06 用户拍板): 锚定一次 → 规划一条路线,
     * 车沿这条线一直走到终点; 路线平时冻结, 只允许两个事件改动它 — 向前续段
     * (append=true: 路线尽头/边界等待续段)。周期性重规划已删除。
     *
     * 路线来源 = 大地图同源道路矢量 (RoadNetwork, worldmap.xml.bin 的 highway
     * 路网): 起终点各吸附最近路网节点, Dijkstra 中心折线, 末尾追加精确终点 —
     * 整图数据一次性可得, 无流式加载限制, 首段即全程 (不再有"止于已加载边缘"
     * 的续段需求)。路网不可用/断裂 → 直线直奔兜底 (routeKind="direct",
     * 穿墙保证可达)。
     *
     *                    新段替换 pathIdx 之后的部分
     * @return false = 规划失败 (冷却期内返回"路线仍有效"布尔)
     */
    private boolean planRoute(BaseVehicle vehicle, long now, boolean append, boolean replaceTail) {
        if (now < replanCooldownMs) return path != null && pathIdx < path.size();
        replanCooldownMs = now + REPLAN_COOLDOWN_MS;

        ArrayList<float[]> assembled = RoadNetwork.findRoute(
                vehicle.getX(), vehicle.getY(), targetX, targetY);
        if (assembled != null && !assembled.isEmpty()) {
            routeKind = "road";
        } else {
            assembled = new ArrayList<float[]>();
            assembled.add(new float[]{targetX, targetY});
            routeKind = "direct";
        }
        // 路线事件常开日志 (每次规划/续段/尾段替换一条, 供实机诊断)
        Logger.printLog("[AutoDrive] route=" + (replaceTail ? "tail-swap " : append ? "extend " : "plan ")
                + routeKind + " (" + assembled.size() + " wp, "
                + (int) dist(vehicle.getX(), vehicle.getY(), targetX, targetY) + " cells to target)");

        if (append && path != null) {
            // 续段/尾段替换: 跳过新段中距车辆当前位置 <1 格的头部点 (防起步回走)
            int skip = 0;
            while (skip < assembled.size()) {
                float[] wp = assembled.get(skip);
                float dx = wp[0] - vehicle.getX();
                float dy = wp[1] - vehicle.getY();
                if (dx * dx + dy * dy >= 1.0f) break;
                skip++;
            }
            if (replaceTail) {
                // 尾段替换: 保留 0..pathIdx 已画前缀, pathIdx 之后整体换新段
                ArrayList<float[]> merged = new ArrayList<float[]>(pathIdx + assembled.size());
                for (int i = 0; i < pathIdx; i++) merged.add(path.get(i));
                for (int i = skip; i < assembled.size(); i++) merged.add(assembled.get(i));
                path = merged;
                // pathIdx 不动 (前缀不变, 追踪无缝衔接)
            } else {
                // 向前续段: 新段追加到现有路线尾部 (已画部分永不改变), pathIdx 不动
                for (int i = skip; i < assembled.size(); i++) path.add(assembled.get(i));
            }
        } else {
            // 锚定/出发: 路线整体替换 (append 且无现有路线时同此)
            path = assembled;
            pathIdx = 0;
        }
        return true;
    }

    // ================================================================
    // 生命周期
    // ================================================================

    /** 锚定终点并出发 (AutoDriveAPI.autoDriveTarget)。返回 false = 未出发。 */
    public boolean start(BaseVehicle vehicle, float x, float y) {
        minDistToTarget = Float.MAX_VALUE;
        this.targetX = x;
        this.targetY = y;
        this.arrivalBrake = false;
        this.engineDeadMs = 0;
        this.lastTickMs = 0;
        this.lastScanMs = 0;
        this.replanCooldownMs = 0;
        this.throttleState = THROTTLE_COAST;
        this.throttleDwellMs = 0;
        this.pendingNoClipOffMs = 0;
        resetAvoidance();
        path = null;
        pathIdx = 0;
        routeKind = "";

        if (!planRoute(vehicle, System.currentTimeMillis(), false, false)) {
            this.state = STATE_IDLE;
            this.messageKey = "UI_DrivePanel_MsgNoPath";
            return false;
        }
        setState(STATE_DRIVING, "UI_DrivePanel_StatusDriving");
        refreshWorldNoClip();
        return true;
    }

    public void stop(String msgKey) {
        cancel(msgKey);
    }

    /**
     * 继续导航 (仅 IDLE 态且有保留路线): 手动接管后沿原路线/原终点恢复自动
     * 驾驶。路线/终点/画线沿用锚定时数据 (所见即所行), 手动驾驶期间驶过的
     * 路点由 advancePath 首帧自行跳过; 宽限期内恢复无缝 (豁免态本来就还开着)。
     * 返回 false = 无保留路线/状态不符。
     */
    public boolean resumeRoute(BaseVehicle vehicle) {
        if (state != STATE_IDLE || path == null || path.isEmpty()) {
            return false;
        }
        this.arrivalBrake = false;
        this.engineDeadMs = 0;
        this.lastTickMs = 0;
        this.lastScanMs = 0;
        this.replanCooldownMs = 0;
        this.throttleState = THROTTLE_COAST;
        this.throttleDwellMs = 0;
        this.lastHandleMs = 0;
        resetAvoidance();
        setState(STATE_DRIVING, "UI_DrivePanel_StatusDriving");
        refreshWorldNoClip();
        return true;
    }

    private void setState(int newState, String statusKey) {
        this.state = newState;
        this.messageKey = statusKey;
        this.stateSinceMs = System.currentTimeMillis();
        log("state -> " + newState);
    }

    /** 世界碰撞豁免开关跟随状态: 目标态与已应用态一致则跳过, 切换时重传已加载 chunk。 */
    private void refreshWorldNoClip() {
        // 手动穿墙开启时保持过滤版: 导航结束不回传原版 (否则误关手动穿墙)
        boolean active = state != STATE_IDLE || combatNoClipManual;
        if (active == worldNoClipApplied) {
            return;
        }
        worldNoClipApplied = active;
        // 运行期只准碰 Hook 类 (零 ASM 引用) — Patch 类是安装期专用, 运行期引用
        // 会因游戏目录无 ASM 库而 NoClassDefFoundError (实测)
        BulletNoClipHook.refreshChunks();
        log("world no-clip " + (active ? "ON (statics passable)" : "OFF (restored)"));
    }

    private void cancel(String msgKey) {
        setState(STATE_ARRIVED, msgKey);
        deactivate();
    }

    /**
     * 车辆重置的碰撞宽限续期 (「车辆重置」按钮调用): 嵌墙车需要时间开出,
     * 豁免再续 NOCLIP_GRACE_MS; 未应用态 (宽限已过/从未激活) 先重传 chunk
     * 物理让豁免立即生效。到期收尾走既有轮询 (handleControls/isActive)。
     */
    public void grantNoClipGrace() {
        if (!worldNoClipApplied) {
            worldNoClipApplied = true;
            BulletNoClipHook.refreshChunks();
        }
        this.pendingNoClipOffMs = System.currentTimeMillis() + NOCLIP_GRACE_MS;
    }

    /** 退出接管: 状态回 IDLE, 清零 clientControls (无状态残留, §五)。
     *  路线/终点保留 (地图导航线持续显示到下次锚定, 2026-09-07 用户需求);
     *  碰撞恢复进宽限期 (见 NOCLIP_GRACE_MS 注释), 由 handleControls/isActive 收尾。 */
    private void deactivate() {
        this.state = STATE_IDLE;
        this.arrivalBrake = false;
        this.ctlSteer = 0;
        this.ctlForward = false;
        this.ctlBackward = false;
        this.ctlBrake = false;
        this.lastTickMs = 0;
        this.engineDeadMs = 0;
        this.throttleState = THROTTLE_COAST;
        this.throttleDwellMs = 0;
        this.lastHandleMs = 0;
        if (worldNoClipApplied) {
            // 碰撞恢复宽限: 立即恢复会把嵌在墙里的车往地下挤 (实测黑屏+埋车)
            this.pendingNoClipOffMs = System.currentTimeMillis() + NOCLIP_GRACE_MS;
        } else {
            this.pendingNoClipOffMs = 0;
        }
        // 消息保留给 UI 状态行读 (ARRIVED 状态语义), 状态归零后由 autoDriveGetMessage 兜底
    }

    // ================================================================
    // 工具
    // ================================================================

    /** 直接写一条状态消息 (API 前置校验失败时用)。 */
    public void setMessage(String key) {
        this.messageKey = key == null ? "" : key;
    }

    /** 期望速上限 (UI 提示行): 用户显式设定 > 车辆物理极速 (服务端限速只作默认)。 */
    public float hardCap(BaseVehicle vehicle) {
        float vehicleMax = Math.max(vehicle.getMaxSpeed(), 20.0f);
        return cruiseSpeed > 0 ? Math.min(cruiseSpeed, vehicleMax) : vehicleMax;
    }

    /** 服务端限速原值 (UI 提示行)。 */
    public float speedLimit() {
        return (float) ServerOptions.instance.speedLimit.getValue();
    }

    /** 制动距离 (格): v(m/s)² / (2·a)。1 格 ≈ 1 m。 */
    private static float brakingDistance(float kmh) {
        float v = Math.max(0.0f, kmh) / 3.6f;
        return v * v / (2.0f * BRAKE_DECEL);
    }

    /** 允许距离 d → 安全速度 km/h (sqrt(2·a·d), 可为 0 = 必须停)。 */
    private static float safeSpeed(float d) {
        if (d <= 0) return 0;
        return (float) Math.sqrt(2.0 * BRAKE_DECEL * d) * 3.6f;
    }

    private static float heading(BaseVehicle vehicle) {
        Vector3f fwd = vehicle.getForwardVector(INSTANCE.fwdVec);
        return (float) Math.atan2(fwd.z, fwd.x);
    }

    /** 角差归一化到 [-π, π]。PZ y 轴朝南 (屏幕向下), 角度顺时针为正 = 右转 = steering 正。 */
    private static float wrapPi(float a) {
        while (a > (float) Math.PI) a -= (float) (Math.PI * 2);
        while (a < (float) -Math.PI) a += (float) (Math.PI * 2);
        return a;
    }

    private static float dist(float x0, float y0, float x1, float y1) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : Math.min(v, hi);
    }

    private void log(String msg) {
        if (verbose) {
            Logger.printLog("[AutoDrive] " + msg);
        }
    }

    // ================================================================
    // 状态读取 (AutoDriveAPI 转发)
    // ================================================================

    public int getStateId() {
        return state;
    }

    public String getMessageKey() {
        return messageKey == null ? "" : messageKey;
    }

    public float getTargetX() {
        return targetX;
    }

    public float getTargetY() {
        return targetY;
    }

    public float getCruiseSpeed() {
        return cruiseSpeed;
    }

    public void setCruiseSpeed(float v) {
        cruiseSpeed = clamp(v, 0.0f, 200.0f);
    }

    public boolean isActive() {
        // 看门狗: 状态挂在活跃档但补丁通道已停跳 (下车/换座后 updateControls 不再
        // 进 handleControls, 状态永不清) → 自愈归零。否则地图"自动驾驶终点"标记
        // 永不消失 (实测), 且下次上车会带旧目标幽灵续驶。
        if (state != STATE_IDLE && lastHandleMs > 0
                && System.currentTimeMillis() - lastHandleMs > 2000) {
            deactivate();
        }
        // on-foot 兜底收尾宽限期 (handleControls 只在开车时进)
        if (state == STATE_IDLE && worldNoClipApplied
                && System.currentTimeMillis() >= pendingNoClipOffMs) {
            refreshWorldNoClip();
        }
        return state == STATE_DRIVING || state == STATE_BRAKE_TO_BOUNDARY || state == STATE_WAIT_LOAD;
    }

    public boolean hasPath() {
        return path != null && !path.isEmpty();
    }

    /**
     * 清除导航线与终点标记 (仅 IDLE 态有效): 接管/到达后路线保留到下次锚定,
     * 玩家改道别处时可显式清掉, 不必被旧线一直挂在地图上。
     */
    public boolean clearRoute() {
        if (state != STATE_IDLE) {
            return false;
        }
        path = null;
        pathIdx = 0;
        routeKind = "";
        return true;
    }

    // ================================================================
    // 路线读数 (AutoDriveAPI 转发 → AutoDriveMap 画线)
    // ================================================================

    /** 路点总数 (IDLE/无路线 = 0)。 */
    public int getRouteCount() {
        return path == null ? 0 : path.size();
    }

    /** 第 i 个路点世界坐标 X (越界 0)。 */
    public float getRouteX(int i) {
        if (path == null || i < 0 || i >= path.size()) return 0.0f;
        return path.get(i)[0];
    }

    /** 第 i 个路点世界坐标 Y (越界 0)。 */
    public float getRouteY(int i) {
        if (path == null || i < 0 || i >= path.size()) return 0.0f;
        return path.get(i)[1];
    }

    /** 当前推进下标 (画线进度/防脏读)。 */
    public int getRouteIndex() {
        return pathIdx;
    }

    /** 路线类型: "road" / "fallback"。退出接管后保留 (地图导航线持续到下次锚定)。 */
    public String getRouteKind() {
        return routeKind == null ? "" : routeKind;
    }
}
