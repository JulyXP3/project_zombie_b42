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
    /** Stanley 输出增益 (A 轮 2026-09-11: 原 2.0 固定, 仿真标定 temp/sim_avoid/pp_compare.py
     *  — 摆头极限环主因之一是输出增益过高 + 无阻尼)。 */
    private static final float STEER_GAIN = 1.6f;
    /** 偏航角速度阻尼 (工业 Stanley 实践标配): 抑制方向角闭环极限环 (实测直线摆头)。 */
    private static final float K_DAMP = 0.35f;
    /** psiDot 一阶低通时间常数 (s): 差分噪声抑制。 */
    private static final float PSI_LP_TAU = 0.2f;

    // ===== 纵向层 (速度剖面 + gap 安全速) 参数 — 单位制: 内部 m/s, 对外 km/h =====
    /** 弯道横向加速度上限 (m/s²): v = √(a·R) 的 a。 */
    private static final float A_LAT_MAX = 1.8f;   // 旧 2.5 高估 PZ 轮胎抓地 → 弯中甩出 (实测); 1.8 = 保守弯速
    /** 剖面规划舒适减速度 (m/s², 后向传递用; 急刹由 BRAKE_DECEL 负责)。 */
    private static final float A_PLAN_BRAKE = 4.0f;
    /** ACC 期望车头时距 (s): vSafe = (gap−净距−S0)/T (A 轮 2026-09-11 替代 IDM 加速度式)。 */
    private static final float IDM_T = 1.2f;
    /** ACC 最小停车间距 (m, 格)。 */
    private static final float IDM_S0 = 3.0f;
    // gap 走廊必须 << 检测走廊 (3.5): 绕行中障碍 lat 常在 0.5~3.5 徘徊, 走廊重叠
    // 则 gap 深刹与绕行转向互相打架 → 开一下刹一下 (实测卅九)。1.5 = 死线正前
    // (车半宽 1.5), 只有真的正对障碍才触发保命刹; 横向绕开即平滑退出。
    private static final float IDM_GAP_CORRIDOR = 1.5f;
    private static final float IDM_GAP_CLEAR = 3.5f;      // 间隙 = lon - (自车半长1.5 + 障碍包围2.0)
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
    private String routeKind = "";          // 当前路线类型 ("road"/"roadwm"/"direct"/"")
    /** 当前路线是否裁尾 (终点无道路可达, 路线止于最近可达道路点 — A 规则 2026-09-12):
     *  为真时到达判定锚在路线末点 (真终点可能还在几百格外, 否则永不判到达)。 */
    private boolean routeEndsAtRoad;

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
    private BaseVehicle lastVehicle;       // 最近受控车辆 (deactivate 时关 regulator 用)

    // ===== 纵向执行器状态 (A 轮 2026-09-11: 原版 regulator 单一执行器) =====
    private boolean brakeLatched;          // 刹车迟滞闩 (超目标+5 刹, ≤+1 松)
    private float lastHeading;             // psiDot 差分用
    private long lastHeadingMs;
    private float psiDotLp;                // 偏航角速度低通 (rad/s)

    // ===== 动态避障层 (多障碍车道化, B 轮 2026-09-11) =====
    // MP 下车-车碰撞服务端权威 (ghost 下沉治不了, 廿六/廿七实测) — 唯一真防线 =
    // 物理上别碰: 局部规划窗内全部车辆 → 车道包络选缝 (B1) → 车朝车道内的
    // wpA/wpB 直插绕过 → 全过窗尾回线。无缝 (整路堵死) → BLOCKED 前后蠕动
    // (B2, 相对静止防僵尸砸窗, 绝不完全停车 — 用户拍板)。
    private static final float AVOID_LOOKAHEAD = 18.0f;   // 障碍前瞻 (格, ≈1.1s@55km/h)
    private static final float AVOID_CLEAR_DIST = 8.0f;   // 绕行序列尾部出口余量 (格)
    private static final float AVOID_WINDOW = 40.0f;      // 局部规划窗 (格): 窗内全部障碍参与决策
    private static final float AVOID_CLEAR = 3.5f;        // 侧向禁入半宽 (自车1.5 + 障碍2.0)
    private static final float AVOID_LANE_MAX = 10.0f;    // 横向偏移搜索范围 ± (格)
    private static final float AVOID_SPEED_CAP = 20.0f;   // 绕行/并入恒速 km/h (2026-09-11 用户裁定 10→20)
    /** 挤缝模式净空 (格, C 修复 2026-09-11): 硬净空 AVOID_CLEAR=3.5 = 自车半宽 1.5 +
     *  障碍**包围圆** 2.0 — 圆模型对平行/斜列停放车辆明显偏大 (车横向半宽只有 ~1.0),
     *  密集错位车流恒判无解 → 原地静止 (实测)。硬解无解时降到 2.5 格
     *  (= 自车半宽 1.5 + **平行停放车辆**横向半宽 ~0.9, 留 0.2 余量; 两车中心距 ≥ 4.8 格
     *  才放行, 小于此值车体会重叠) 再规划一次, 限速 8 km/h 挤过去 ——
     *  比"不动"安全 (不动会被僵尸围, 用户红线)。
     *  边界: 该值按"障碍车与路线同向/近平行"标定; 垂直停放的车辆横向半长 ~2.2,
     *  2.5 格不足以完全避免轻擦 —— 低速下可接受, 实机若出现擦碰则回调 2.5→3.0 或
     *  关闭挤缝 (回落到蠕动接近)。 */
    private static final float SQUEEZE_CLEAR = 2.4f;
    private static final float SQUEEZE_SPEED_CAP = 8.0f;  // 挤缝限速 km/h
    /** BLOCKED 蠕动接近律 (C 修复): ACC 跟车律的平衡点是 净距3.5+停车距3.0 = 6.5 格,
     *  比蠕动门控 (IDM_GAP_CLEAR+1.5 = 5.0 格) 更远 → 车永远停在 6.5 格外、蠕动永不
     *  触发 = 完全静止 (实测 csv obsLon=6.56 / v0=0.19 与公式精确吻合)。堵死场景改用
     *  固定低速抵近到 BLOCKED_GAP, 交给"贴住蠕动"分支 (绝不完全停车)。 */
    private static final float BLOCKED_CREEP_KMH = 6.0f;
    private static final float BLOCKED_GAP = IDM_GAP_CLEAR + 1.5f;
    /** 绕行/并入态贴住护栏 (F 修复 2026-09-12, 用户裁定"绕行恒速 20"): 时距跟车律
     *  (IDM_T = 1.2s) 在障碍于正前 10 格时给 (10 − 6.5)/1.2 × 3.6 = 10.5 km/h ——
     *  但绕行层本来就是"横向让开"的解法, 时距项与横向剖面互相打架 (实测避障期掉到
     *  10km/h 的根因: 障碍一进 ±1.5 正前走廊就压速, 横向让开后 obsLon=NaN 又回 20,
     *  表现为"有时候变 10")。绕行/并入期间豁免时距项, 只留**纯距离贴住护栏**:
     *  ≥ 6.5 格 (= ACC 停车平衡点 净距3.5 + 停车距3.0) 完全不干预, 5.0 格刹停,
     *  之间线性降速 —— 横向剖面失效 (车没让开) 时才起作用。 */
    private static final float AVOID_HOLD_GAP = IDM_GAP_CLEAR + 1.5f;   // 5.0 格刹停线
    private static final float AVOID_HOLD_T = 0.25f;                    // 护栏坡度 (s)
    /** 卡死脱困 (C 二轮 2026-09-11): 绕行中目标速 >3 却连续 STUCK_MS 车速 ≈0 =
     *  楔住 (实测 20km/h 目标下静止 12s+, 只靠 regulator 永远出不来) → 倒车
     *  UNSTICK_MS 脱困 + 复位绕行参考系重选缝; 冷即 UNSTICK_COOLDOWN_MS 防死循环。 */
    private static final float STUCK_SPEED = 1.0f;        // km/h
    private static final long STUCK_MS = 1200;
    private static final long UNSTICK_MS = 1500;
    private static final long UNSTICK_COOLDOWN_MS = 2500;
    private long stuckSinceMs;
    private long unstickUntilMs;
    private long unstickReadyMs;
    /** 绕行决策切入距离 (格): 最近障碍比这远时不进绕行, 沿路线正常 gap 跟车 —
     *  旧实现 40 格外就锁参考系整窗选缝, 远距离误判 BLOCKED 原地蠕动 (实测)。 */
    private static final float AVOID_ENGAGE = 30.0f;
    /** 横向剖面 DP: 候选偏移步长 (格) / 偏心代价权重 (偏向中线)。 */
    /** 横向候选偏移步长 (格): 1.0 时整数栅格在窄缝处放不下 —— 5.2 格缝的中点 2.6
     *  取不到整数候选, 净空 2.5 也无解 (C 修复 2026-09-11 实测: 该场景本可通过却被判
     *  无解)。降到 0.5 让剖面能用缝的真实中点; 候选数 21→41, DP 代价 O(n·K²) 仍微秒级。 */
    private static final float LATTICE_STEP = 0.5f;
    private static final float LATTICE_CENTER_W = 0.05f;
    private static final int AVOID_NONE = 0;              // 无绕行
    private static final int AVOID_DETOUR = 1;            // 缺口序列绕行中
    private static final int AVOID_BLOCKED = 2;           // 无可行横向剖面 (贴近后蠕动)
    private static final int AVOID_RETURN = 3;            // 并入段: Stanley 回线 (卌一)
    // RETURN 并入参数: |cte| < CTE_EXIT 交回 Stanley (近线后 Stanley 良态)。
    private static final float AVOID_RETURN_CTE_EXIT = 1.5f;
    private int avoidMode = AVOID_NONE;
    private float avoidTargetX;                           // 绕行目标点 (世界坐标)
    private float avoidTargetY;
    private float avoidOriginX, avoidOriginY;             // 锁定参考系原点 (自车位置)
    private boolean avoidDiagHit;          // AvoidDiag 节流 (状态变化才打)
    private int avoidDiagMode = -1;        // AvoidDiag 模式节流 (模式变化才打)
    private float detourDirX, detourDirY;  // 进入绕行时锁定的行进方向 (固定参考系, 卌)
    private float obsLon = Float.NaN;      // 走廊最近障碍纵向投影 (IDM gap 用, NaN = 无)
    private float obsLat = Float.NaN;
    // 窗内全部车辆位置缓存 (scanObstacle 150ms 刷新; 车道决策数据源)
    private final ArrayList<float[]> avoidObsList = new ArrayList<float[]>();
    // item 收集缓冲 (updateAvoidance 每帧用, 免分配; 上限外忽略 — 现实中不会超)
    private final float[] itemLon = new float[128];
    private final float[] itemLat = new float[128];
    private final float[] itemX = new float[128];
    private final float[] itemY = new float[128];
    // 横向剖面 DP 缓冲 (免分配): 候选偏移数 = 2*AVOID_LANE_MAX/step+1
    private static final int LATTICE_N = (int) (2 * AVOID_LANE_MAX / LATTICE_STEP) + 1;
    private final float[] dpPrev = new float[LATTICE_N];
    private final float[] dpCur = new float[LATTICE_N];
    private final int[] bkRow = new int[LATTICE_N * 128];   // 回溯表 (障碍数 ≤128)
    // 绕行路径点 (锁定系 lon/lat; DP 输出, 每次 updateAvoidance 重算)
    private final float[] wpLon = new float[130];
    private final float[] wpLat = new float[130];
    private int wpCount;
    private float minDistToTarget = Float.MAX_VALUE;   // 终点最近点追踪 (绕行残留偏移>到达半径时防冲过, 仿真 S5)
    // 碰撞恢复宽限: 退出接管时车可能正在墙里/树下, 立即恢复静态碰撞会被 Bullet
    // 把嵌着的车往地下挤 (实测: 概率黑屏 + 下车发现车埋地下)。宽限期内保持可穿,
    // 玩家驶离后再恢复。伤害/冲量豁免不受宽限 (isWorldNoClip 仍按状态严格门控)。
    private long pendingNoClipOffMs;
    private static final long NOCLIP_GRACE_MS = 15000;

    // 诊断采样 stash (DriveDiag.sample 用; 每帧由 stanleySteer/driveTick 写入):
    private float diagPhi;      // 最近一次 Stanley 航向误差 (rad)
    private float diagCTE;      // 最近一次 Stanley 横向偏差 e (格)
    private int diagSeg = -1;   // 最近一次投影段号
    private float diagCorner;   // 当帧弯道参考速 km/h
    private float diagV0;       // 当帧最终目标速 km/h

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

    /** 蠕动速度上限 (km/h): 超过即踩刹车收速 (导航 BLOCKED 与蠕动秒杀共用)。 */
    private static final float WIGGLE_SPEED_KMH = 6.0f;

    /**
     * 蠕动动作 (导航 BLOCKED 与「蠕动秒杀」**共用同一套**, 2026-09-11 用户要求统一):
     * 前进/后退互补 + 锚点 ±WIGGLE_RANGE 限位 + 范围内 1s 节拍 + 方向回正 +
     * 超 WIGGLE_SPEED_KMH 踩刹车 (蠕动是"原地前后蹭", 不是开走)。
     * 结果写入 ctlForward/ctlBackward/ctlBrake/ctlSteer (导航路径由 writeControls 下发;
     * 手动路径由调用方写 clientControls)。
     */
    private void applyWiggleMotion(BaseVehicle vehicle, long now, float absSpeed) {
        if (vehicle.isRegulator()) vehicle.setRegulator(false);
        boolean fwd = wiggleForward(vehicle, now);
        ctlForward = fwd;
        ctlBackward = !fwd;
        ctlBrake = absSpeed > WIGGLE_SPEED_KMH;
        ctlSteer = 0.0f;
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
            lastVehicle = vehicle;   // deactivate 时关 regulator (还玩家干净车况)
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
        // 与导航 BLOCKED 蠕动同一套动作 (用户 2026-09-11: 「蠕动秒杀」做成 C 里的那个蠕动)
        applyWiggleMotion(vehicle, System.currentTimeMillis(),
                Math.abs(vehicle.getCurrentSpeedKmHour()));
        CarController.ClientControls c = cc.clientControls;
        c.steering = ctlSteer;
        c.forward = ctlForward;
        c.backward = ctlBackward;
        c.brake = ctlBrake;
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

        // 诊断采样 (5Hz + 自动异常标记; 关闭时零开销), 全状态覆盖 (含等待/刹边界)
        if (DriveDiag.isEnabled()) {
            DriveDiag.sample(vehicle, now, diagV0, diagCorner, diagPhi, diagCTE, pathIdx, diagSeg,
                    avoidMode, obsLon, obsLat, throttleState, vehicle.isRegulator(),
                    state, arrivalBrake, ctlSteer);
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
        // 踏板复位 (B 修复 2026-09-11): 各分支只显式置自己需要的位, 上一帧蠕行/
        // 倒车的 ctlBackward 会残留 — 与后续 ctlForward 同真 = 原版双键刹车,
        // 车停死 (路口实测); 或 forward=false 时车自然后退。根因收口在这一处。
        ctlForward = false;
        ctlBackward = false;
        ctlBrake = false;

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
        // A 规则 (F 修复 2026-09-12): 路线裁尾时 (终点无道路可达) 到达锚点 = 路线末点
        // (最近可达道路点) —— 真终点还在几百格外, 锚在真终点则到达条件永不满足, 车会
        // 停在道路尽头原地空转。
        float ax = targetX;
        float ay = targetY;
        if (routeEndsAtRoad && path != null && !path.isEmpty()) {
            float[] end = path.get(path.size() - 1);
            ax = end[0];
            ay = end[1];
        }
        float distToTarget = dist(vehicle.getX(), vehicle.getY(), ax, ay);
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
                // 裁尾路线: 到达即"已停在最近道路", 用 A 规则提示保留"剩余自行驾驶"语义
                setState(STATE_ARRIVED, routeEndsAtRoad
                        ? "UI_DrivePanel_MsgRoadEnd" : "UI_DrivePanel_MsgArrived");
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

        // ===== 动态避障 (车辆/残骸 缺口序列绕行, A 轮 2026-09-11) =====
        // MP 下车-车碰撞服务端权威 → 唯一真防线 = 别碰上: 感知在 scanObstacle,
        // 决策在 updateAvoidance — 窗内全部障碍 → 横向剖面 DP (缺口序列) 串缝绕过。
        updatePsiDot(vehicle, now);
        float avoidCap = updateAvoidance(vehicle, now);

        // ===== 卡死脱困 (C 二轮修复 2026-09-11) =====
        // 绕行/挤缝中目标速有值却连续 1.2s 车速 ≈0 → 楔在车阵/墙里; 倒车一段 + 复位
        // 绕行参考系 (下次扫描按新位置重新规划)。蠕动态 (前后蹭) 也走这条路: 卡住不动
        // 时倒车比原地蹭更有效。
        if (avoidMode != AVOID_NONE && absSpeed < STUCK_SPEED) {
            if (stuckSinceMs == 0) {
                stuckSinceMs = now;
            }
        } else {
            stuckSinceMs = 0;
        }
        if (now < unstickUntilMs) {
            // 倒车窗口: 不给油不给刹, 方向回正 (纯后退 3~4 格)
            if (vehicle.isRegulator()) vehicle.setRegulator(false);
            ctlForward = false;
            ctlBackward = true;
            ctlBrake = false;
            ctlSteer = 0.0f;
            return;
        }
        if (stuckSinceMs != 0 && now - stuckSinceMs > STUCK_MS && now >= unstickReadyMs) {
            unstickUntilMs = now + UNSTICK_MS;
            unstickReadyMs = unstickUntilMs + UNSTICK_COOLDOWN_MS;
            stuckSinceMs = 0;
            resetAvoidance();
            DriveDiag.event("UNSTICK", "reverse " + (int) UNSTICK_MS + "ms");
            if (vehicle.isRegulator()) vehicle.setRegulator(false);
            ctlForward = false;
            ctlBackward = true;
            ctlBrake = false;
            ctlSteer = 0.0f;
            return;
        }
        if (avoidMode == AVOID_BLOCKED && absSpeed < 2.0f
                && !Float.isNaN(obsLon) && obsLon < IDM_GAP_CLEAR + 1.5f) {
            // 无可行横向剖面且已贴住堵点: 原地蠕动 (相对静止防僵尸砸窗, 用户规则;
            // 锚定 ±2 格 + 1s 节拍)。远距离不再蠕动 (旧实现 39 格外就原地蹭, 实测
            // DETOUR↔BLOCKED 对翻不前) — 沿路线以 gap 安全速接近到贴住为止。
            applyWiggleMotion(vehicle, now, absSpeed);
            return;
        }
        wiggleAnchored = false;     // 非蠕动态: 锚点失效

        // ===== 纵向目标: 巡航/弯道剖面 + 偏航安全网 + 绕行限速 =====
        // 静态物穿墙, 僵尸碾杀 (战损钩子), 车辆/残骸缺口序列绕行 (本层)。
        float vehicleMax = Math.max(vehicle.getMaxSpeed(), 20.0f);
        float cruise = cruiseSpeed > 0 ? Math.min(cruiseSpeed, vehicleMax)
                : Math.min(Math.min((float) ServerOptions.instance.speedLimit.getValue()
                        * SPEED_LIMIT_MARGIN, vehicleMax), CRUISE_ADAPTIVE);
        float cornerLimit = cornerRefSpeed(vehicle, cruise);
        diagCorner = cornerLimit;
        float v0 = Math.min(cruise, cornerLimit);
        v0 = Math.min(v0, avoidCap);   // 绕行限速 (转向物理余量)

        // 偏航安全网 (Stanley 收敛失败时兜底, 阈值放宽) — **只在正常跟线时生效**
        // (E 修复 2026-09-11): 绕行层本身要求横向偏移 ±10 格, 用"相对原路线的偏差"
        // 衡量会把有效绕行全程压到 10 km/h (实测反向绕行 cte=5.27 → 22 格爬 8.2 秒,
        // 正反向速度不一致的根因)。绕行/并入/堵死期间速度由绕行层自己的限速负责
        // (绕行 20 / 挤缝 8 / 蠕动 6)。
        if (avoidMode == AVOID_NONE) {
            float cte = crossTrackError(vehicle);
            if (cte > 4.0f) {
                v0 = Math.min(v0, 10.0f);
            } else if (cte > 2.5f) {
                v0 = Math.min(v0, 18.0f);
            }
        }

        // 未加载边界刹停线: 制动距离 + 4 格富余 (停于 3×3 物理守卫带内)
        float brakeDist = brakingDistance(absSpeed);
        if (boundaryDist <= brakeDist + 4.0f) {
            setState(STATE_BRAKE_TO_BOUNDARY, "UI_DrivePanel_StatusBrakeToBoundary");
            return;
        }

        // 跟车安全速: 正常跟车 = ACC 时间间隙律 (gap 越小目标越低); 无可行剖面的
        // 堵死态 = 蠕动接近律 (固定低速抵近到 BLOCKED_GAP, 交给蠕动分支); 绕行/并入
        // = 恒速 20 (F 修复 2026-09-12: 豁免时距项, 只留贴住护栏 —— 见 AVOID_HOLD_GAP)。
        // 三者不可混用 —— ACC 在堵死态会把车停在 6.5 格外, 而蠕动门控要求 5.0 格,
        // 互锁成完全静止 (C 修复 2026-09-11, 实测证据见常量区注释)。
        float vSafe = gapSafeSpeed(avoidMode, obsLon, obsLat);
        float target = Math.min(v0, vSafe);
        diagV0 = target;   // 诊断采样: 当帧最终目标速 (边界检查之后 = 实际执行值)

        // ===== 执行器: 原版 regulator 拉锁 + 刹车迟滞 (见常量区原版语义注释) =====
        int targetKmh = Math.max(0, Math.round(target));
        if (vehicle.isRegulator()) {
            vehicle.setRegulatorSpeed(targetKmh);
        } else if (absSpeed <= targetKmh + BRAKE_RELEASE_KMH) {
            // 单次重挂 (刹车帧游戏自动取消 regulator :401; 回落到目标附近才挂回)
            vehicle.setRegulator(true);
            vehicle.setRegulatorSpeed(targetKmh);
        }
        if (absSpeed > targetKmh + BRAKE_OVER_KMH) {
            brakeLatched = true;
        } else if (absSpeed <= targetKmh + BRAKE_RELEASE_KMH) {
            brakeLatched = false;
        }
        ctlBrake = brakeLatched;
        ctlForward = false;
        ctlBackward = false;
        throttleState = brakeLatched ? THROTTLE_BRAKE
                : (vehicle.isRegulator() ? THROTTLE_COAST : THROTTLE_FWD);

        // ===== 横向: 绕行模式 = 朝缺口序列当前点直插 / 正常 = Stanley 跟线 =====
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
     * gap 安全速纯函数 (纵向目标速的下限来源; 抽成 static = 离线自检可调, 见
     * temp/drivetest/AvoidSpeedTest): 返回 km/h 上限, MAX_VALUE = 不干预。
     *  · 正常跟车: ACC 时间间隙律 (净距/时距),
     *  · 绕行/并入: 豁免时距项, 只留贴住护栏 (恒速 20 由 avoidCap 负责),
     *  · 堵死态: 不在此处 (由蠕动接近律接管)。
     */
    static float gapSafeSpeed(int mode, float obsLon, float obsLat) {
        if (mode == AVOID_BLOCKED) {
            return (!Float.isNaN(obsLon) && obsLon <= BLOCKED_GAP) ? 0.0f : BLOCKED_CREEP_KMH;
        }
        if (mode == AVOID_DETOUR || mode == AVOID_RETURN) {
            // 绕行/并入: 让开由横向剖面负责, 纵向恒速 20 (用户裁定); 只在贴住区
            // (正前 |obsLat| < 1.5) 降速。检测走廊是 ±3.5 — 侧前 3.4 格的障碍
            // 只是"在走廊里", 不挡道, 误刹会把车整段停死 (2026-09-12 05:22 版
            // 回归实测: 一辆侧向残骸 → UNSTICK×6 → 接管)。
            if (Float.isNaN(obsLon) || Math.abs(obsLat) >= IDM_GAP_CORRIDOR) {
                return Float.MAX_VALUE;
            }
            return Math.max(0.0f, (obsLon - AVOID_HOLD_GAP) / AVOID_HOLD_T) * 3.6f;
        }
        if (!Float.isNaN(obsLon) && Math.abs(obsLat) < IDM_GAP_CORRIDOR) {
            return Math.max(0.0f, (obsLon - IDM_GAP_CLEAR - IDM_S0) / IDM_T) * 3.6f;
        }
        return Float.MAX_VALUE;
    }

    /**
     * 弯道参考速 (速度剖面, velocity profiling 标准法): 沿路径自 pathIdx 前向
     * 采样 (步长 PROFILE_DS), 每采样点用三点外接圆估曲率 → v = √(A_LAT_MAX·R);
     * 后向传递 v_j = min(v_j, √(v_{j+1}² + 2·A_PLAN_BRAKE·Δs)) 保证"到弯前能刹
     * 到弯速"。返回当前位置的参考速 (km/h)。目标速由前方几何决定: 距离连续、
     * 平滑、有预见 — 这是替代"看当前角误差限速"的根治。
     */
    private float cornerRefSpeed(BaseVehicle vehicle, float capKmh) {
        if (path == null || pathIdx >= path.size() - 1) return capKmh;
        // 采样起点 = **车在当前段上的投影点** (B 修复 2026-09-11): 旧实现从段起点
        // path.get(pathIdx) 起算, 车常行驶在长段中间 (实测段距车 70+ 格), 剖面的
        // 减速距离从段起点算 → 车到了弯前 v0 还没降 (实测 88km/h 冲到弯点才收到
        // 限速, "减速不及时冲出去"根因)。投影点在路径线上, 不引入"车→路点"对角
        // 弦的假曲率 (旧注释担心的尖峰不成立 — 投影 ≠ 车辆实际位置)。
        float[] a0 = path.get(pathIdx);
        float[] b0 = path.get(pathIdx + 1);
        float dx0 = b0[0] - a0[0];
        float dy0 = b0[1] - a0[1];
        float l20 = dx0 * dx0 + dy0 * dy0;
        float tProj = l20 > 1e-6f
                ? clamp(((vehicle.getX() - a0[0]) * dx0 + (vehicle.getY() - a0[1]) * dy0) / l20, 0.0f, 1.0f)
                : 0.0f;
        ArrayList<float[]> pts = new ArrayList<float[]>(24);
        float acc = 0;
        float[] prev = new float[]{a0[0] + dx0 * tProj, a0[1] + dy0 * tProj};
        outer:
        for (int i = pathIdx + 1; i < path.size(); i++) {
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

    // ===== 执行器 (A 轮 2026-09-11: 原版 regulator 单一执行器, bang-bang 已删) =====
    //
    // 依据原版语义 (CarController 亲读, analysis/pz_sync_analysis):
    //   • regulator 开 = 低于目标速自动给油 / 高于滑行, control_NoControl **不脱档**
    //     (:441 isRegulator 门控); 关 = 无油门无刹车即强制 N 档 (:441), 刹车也 N (:455)。
    //   • 原版刹车间 regulator 会被游戏自己取消 (:401 isBreak 分支) — 我们不逐帧抢挂,
    //     只在速度回落到目标附近后**单次**重挂 (迟滞), 否则就是实测的"疯狂开关巡航"。
    //   • regulatorSpeed 原版只 ±5 整数改速 (:385); 仪表盘直接画浮点
    //     (ISVehicleDashboard.lua:406) → 目标速必须取整 (实测巡航小数点来源)。
    // 旧 FWD/COAST bang-bang 每次松油门都置 N (低速 1↔N 对翻), 已整体删除;
    // 跟车安全速改为目标速合成: vSafe = (gap-净距-停车距)/时距 (ACC 时间间隙律)。
    private static final int THROTTLE_FWD = 0;
    private static final int THROTTLE_COAST = 1;
    private static final int THROTTLE_BRAKE = 2;
    /** 死区半宽 (m/s): 剖面种子裕度 (cornerRefSpeed 用)。 */
    private static final float THROTTLE_BAND_MS = 2.0f / 3.6f;
    /** 刹车迟滞 (km/h): 超目标速此值才刹车 / 回落到目标+1 松刹。 */
    private static final float BRAKE_OVER_KMH = 5.0f;
    private static final float BRAKE_RELEASE_KMH = 1.0f;

    private int throttleState = THROTTLE_COAST;   // 诊断列语义保留 (0 FWD/1 巡航/2 刹车)

    /**
     * Stanley 横向控制 (Hoffmann et al.,斯坦福 DARPA 挑战赛):
     * δ = φ + atan2(k·e, v) − kd·ψ̇, 输出归一化舵量 [-1,1]。
     * A 轮 2026-09-11: 加偏航角速度阻尼 (kd=0.35, 低通 0.2s) + 输出增益 2.0→1.6
     * — 旧实现无阻尼 + PZ 舵机一阶滞后, 直线上满舵极限环 (实测摆头全程;
     * 仿真 pp_compare.py: 方向穿越 20→3)。
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
        float delta = phi + eTerm - K_DAMP * psiDotLp;
        // 诊断采样 stash (DriveDiag.sample 读: 摆头/段跳变/饱和的事件证据)
        diagPhi = phi;
        diagCTE = e;
        diagSeg = bestSeg;
        return clamp(delta * STEER_GAIN, -1.0f, 1.0f);
    }

    /** 偏航角速度低通 (每 tick 一次, driveTick 头部调用)。 */
    private void updatePsiDot(BaseVehicle vehicle, long now) {
        float h = heading(vehicle);
        if (lastHeadingMs > 0) {
            float dt = (now - lastHeadingMs) / 1000.0f;
            if (dt > 0.001f && dt < 0.5f) {
                float rate = wrapPi(h - lastHeading) / dt;
                psiDotLp += (rate - psiDotLp) * Math.min(1.0f, dt / PSI_LP_TAU);
            }
        } else {
            psiDotLp = 0;
        }
        lastHeading = h;
        lastHeadingMs = now;
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
     * 障碍感知 (每 150ms 感知节拍随 scan 调用, B 轮多障碍版):
     * 收集**全部**窗内车辆位置到 avoidObsList (车道包络决策数据源, 不做单障碍筛选);
     * 同时维护 avoidDiagHit 状态变化日志。静态物不判 (照穿), 僵尸不判 (照碾) —
     * 只处理车辆/残骸 (MP 碰撞服务端权威, 客户端唯一真防线 = 别碰上)。
     */
    private void scanObstacle(BaseVehicle vehicle, float absSpeed) {
        float vx = vehicle.getX();
        float vy = vehicle.getY();
        float selfZ = vehicle.getZ();
        // 收集半径 = max(前瞻, 窗+裕量) — 窗内全部障碍都要入列
        float look = Math.max(AVOID_LOOKAHEAD + brakingDistance(absSpeed), AVOID_WINDOW + 8.0f);
        avoidObsList.clear();
        int cx = (int) (vx / 8.0f);
        int cy = (int) (vy / 8.0f);
        for (int dy = -5; dy <= 5; ++dy) {
            for (int dx = -5; dx <= 5; ++dx) {
                IsoChunk chunk = IsoWorld.instance.currentCell.getChunk(cx + dx, cy + dy);
                if (chunk == null) continue;
                collectObstacles(chunk.vehicles, vehicle, vx, vy, selfZ, look);
            }
        }
        // cell 全量兜底 (生成车不一定在 chunk 登记 — 廿四实测)
        for (BaseVehicle v : IsoWorld.instance.currentCell.getVehicles()) {
            collectObstacle(v, vehicle, vx, vy, selfZ, look);
        }
        if (!avoidObsList.isEmpty()) {
            if (!avoidDiagHit) {
                Logger.printLog("[AvoidDiag] obstacles: " + avoidObsList.size()
                        + " in window (nearest check next frame)");
                DriveDiag.event("OBS_HIT", "count=" + avoidObsList.size());
                avoidDiagHit = true;
            }
        } else if (avoidDiagHit) {
            Logger.printLog("[AvoidDiag] obstacles lost");
            DriveDiag.event("OBS_LOST", "");
            avoidDiagHit = false;
        }
    }

    /** 单障碍入列 (去重: chunk 列表与 cell 兜底可能重复)。 */
    private void collectObstacle(BaseVehicle v, BaseVehicle self,
            float vx, float vy, float selfZ, float look) {
        if (v == self) return;
        if (Math.abs(v.getZ() - selfZ) > 1.0f) return;
        if (dist(vx, vy, v.getX(), v.getY()) > look) return;
        for (int i = 0; i < avoidObsList.size(); ++i) {
            float[] o = avoidObsList.get(i);
            if (o[0] == v.getX() && o[1] == v.getY()) return;
        }
        avoidObsList.add(new float[]{v.getX(), v.getY()});
    }

    /** chunk 车辆列表内收集 (scanObstacle 用)。 */
    private void collectObstacles(java.util.ArrayList<BaseVehicle> list, BaseVehicle self,
            float vx, float vy, float selfZ, float look) {
        for (int i = 0; i < list.size(); ++i) {
            collectObstacle(list.get(i), self, vx, vy, selfZ, look);
        }
    }

    /**
     * 绕行决策 (A 轮 2026-09-11: 缺口序列 = 局部栅格 DP, 业界 local lattice):
     * ① 决策距离门控: 最近障碍 > AVOID_ENGAGE 不进绕行, 沿路线正常 gap 跟车 —
     *    旧实现 40 格外就锁窗整缝选道, 远距离误判 BLOCKED 原地蹭 (实测 39 格外蹭 20s);
     * ② 锁参考系 (路线切向 + 原点, 卌三);
     * ③ 横向剖面 DP (planLattice): 障碍按 lon 排序逐个选横向偏移, 净空可行 +
     *    Δlat² 平滑代价 + 偏中线代价 → (lon,lat) 序列 + 窗尾出口点 → S 形串缝
     *    (错位车流单条直线车道切不进, 旧 chooseLane 因此恒 NaN → BLOCKED 对翻);
     * ④ 无可行剖面 → BLOCKED: gap 安全速沿路线接近, 贴住后蠕动 (driveTick);
     * ⑤ 全窗障碍过尾 → RETURN 并入段 (Stanley 回线)。
     * 返回绕行限速 km/h (无绕行 = Float.MAX_VALUE)。
     */
    private float updateAvoidance(BaseVehicle vehicle, long now) {
        // IDM gap 数据源: 自车系走廊 (|lat|<AVOID_CLEAR) 内最近障碍 (保命层用)
        refreshGapFields(vehicle);

        if (avoidMode == AVOID_RETURN) {
            return AVOID_SPEED_CAP;   // 并入段: 不受障碍影响, 由 cte 退出
        }
        boolean tracking = (avoidMode == AVOID_DETOUR || avoidMode == AVOID_BLOCKED);
        float fx, fy, ox0, oy0;
        if (tracking) {
            fx = detourDirX;
            fy = detourDirY;
            ox0 = avoidOriginX;
            oy0 = avoidOriginY;
        } else {
            Vector3f fwd = vehicle.getForwardVector(fwdVec);
            fx = fwd.x;
            fy = fwd.z;
            ox0 = vehicle.getX();
            oy0 = vehicle.getY();
        }
        float eLon = (vehicle.getX() - ox0) * fx + (vehicle.getY() - oy0) * fy;
        float eLat = (vehicle.getX() - ox0) * fy - (vehicle.getY() - oy0) * fx;
        float back = tracking ? AVOID_CLEAR : 2.0f;
        int n = collectItems(fx, fy, ox0, oy0, eLon, back);
        if (n == 0) {
            if (tracking) {
                // 全窗障碍过尾 → 并入段 (卅一); 新绕行全新规划
                avoidMode = AVOID_RETURN;
                avoidTargetX = Float.NaN;
                avoidTargetY = Float.NaN;
                logAvoidMode(AVOID_RETURN, "");
                return AVOID_SPEED_CAP;
            }
            avoidMode = AVOID_NONE;
            return Float.MAX_VALUE;
        }
        if (avoidMode == AVOID_NONE) {
            // 决策距离门控: 没到切入距离不锁参考系 (避免远距误判)
            float minLonAll = Float.MAX_VALUE;
            for (int i = 0; i < n; ++i) minLonAll = Math.min(minLonAll, itemLon[i]);
            if (minLonAll > AVOID_ENGAGE) {
                avoidMode = AVOID_NONE;
                return Float.MAX_VALUE;
            }
            // 全新绕行: 锁参考系 (路线切向优先, 卌三); 原点 = 自车位置
            int ni = 0;
            for (int i = 1; i < n; ++i) if (itemLon[i] < itemLon[ni]) ni = i;
            float[] tan = pathProject(itemX[ni], itemY[ni]);
            if (tan != null) {
                detourDirX = tan[0];
                detourDirY = tan[1];
            } else {
                Vector3f fwd = vehicle.getForwardVector(fwdVec);
                detourDirX = fwd.x;
                detourDirY = fwd.z;
            }
            avoidOriginX = vehicle.getX();
            avoidOriginY = vehicle.getY();
            fx = detourDirX;
            fy = detourDirY;
            ox0 = avoidOriginX;
            oy0 = avoidOriginY;
            eLon = 0.0f;
            eLat = 0.0f;
            n = collectItems(fx, fy, ox0, oy0, eLon, 2.0f);
            if (n == 0) {
                avoidMode = AVOID_NONE;
                return Float.MAX_VALUE;
            }
        }
        // 硬净空无解 → 挤缝净空再规划一次 (C 修复 2026-09-11): 密集/错位车流下
        // 3.5 格舒适净空恒无解, 旧实现直接 BLOCKED 原地蹭 (实测"有缝也不动")。
        // 挤缝剖面可行 = 仍走 DETOUR (诊断列语义不变) 但限速降到 8 km/h。
        boolean squeeze = false;
        if (!planLattice(n, eLat, AVOID_CLEAR)) {
            if (planLattice(n, eLat, SQUEEZE_CLEAR)) {
                squeeze = true;
            } else {
                avoidMode = AVOID_BLOCKED;
                avoidTargetX = Float.NaN;
                avoidTargetY = Float.NaN;
                logAvoidMode(AVOID_BLOCKED, "");
                return AVOID_SPEED_CAP;   // 蠕动接近律 (driveTick); 贴住后原地蠕动
            }
        }
        // 当前目标点 = 序列中第一个在车前方的点 (DP 每感知重算, 无状态重选:
        // 车前进则目标点自然切到下一个 — 旧 wpA/wpB 双点特判删除)
        float px = fy;
        float py = -fx;
        float tx = Float.NaN;
        float ty = Float.NaN;
        for (int i = 0; i < wpCount; ++i) {
            if (wpLon[i] > eLon + 0.5f) {
                tx = ox0 + fx * wpLon[i] + px * wpLat[i];
                ty = oy0 + fy * wpLon[i] + py * wpLat[i];
                break;
            }
        }
        if (Float.isNaN(tx)) {
            // 全部序列点在车后 (越过了窗尾) → 并入段
            avoidMode = AVOID_RETURN;
            avoidTargetX = Float.NaN;
            avoidTargetY = Float.NaN;
            logAvoidMode(AVOID_RETURN, "");
            return AVOID_SPEED_CAP;
        }
        avoidTargetX = tx;
        avoidTargetY = ty;
        avoidMode = AVOID_DETOUR;
        logAvoidMode(AVOID_DETOUR, " wp=" + wpCount + (squeeze ? " squeeze" : ""));
        return squeeze ? SQUEEZE_SPEED_CAP : AVOID_SPEED_CAP;
    }

    /** 横向剖面 DP: 约束行合并跨距 (格) — 纵向距 < 此值的障碍车是同时面对的,
     *  净空约束取并集 (S10b 实证: 不合并则横排车被当"先后通过"直接穿车)。 */
    private static final float LATTICE_ROW_SPAN = 6.0f;

    /** 候选 lat 对障碍 j 可行 = 对纵向距 < LATTICE_ROW_SPAN 的全部相关障碍
     *  (前后双向) 都满足 |lat−obsLat| ≥ clear。itemLon 已按升序, n 为总数。
     *  clear 参数化 (C 修复): AVOID_CLEAR = 舒适净空, SQUEEZE_CLEAR = 挤缝降级净空。 */
    private boolean latticeFeasible(int n, int j, float lat, float clear) {
        int lo = j;
        while (lo > 0 && itemLon[j] - itemLon[lo - 1] < LATTICE_ROW_SPAN) --lo;
        int hi = j;
        while (hi < n - 1 && itemLon[hi + 1] - itemLon[j] < LATTICE_ROW_SPAN) ++hi;
        for (int q = lo; q <= hi; ++q) {
            if (Math.abs(lat - itemLat[q]) < clear) return false;
        }
        return true;
    }

    /**
     * 横向剖面 DP (local lattice): 障碍按 lon 升序, 候选偏移
     * {-AVOID_LANE_MAX..+AVOID_LANE_MAX} 步长 LATTICE_STEP。
     * dp[j][k] = 障碍 j 处取候选 k 的最小累计代价; 可行 = latticeFeasible (约束行并集);
     * 转移代价 = Δlat² (平滑, 隐式限斜率) + 偏心 lat²·w (偏中线); 起步项 = 自车 eLat。
     * 输出 (lon,lat) 序列 + 窗尾出口点到 wpLon/wpLat。无可行剖面返回 false。
     */
    private boolean planLattice(int n, float eLat, float clear) {
        // 障碍按 lon 升序 (选择排序, n ≤128)
        for (int i = 0; i < n; ++i) {
            int mi = i;
            for (int j = i + 1; j < n; ++j) if (itemLon[j] < itemLon[mi]) mi = j;
            if (mi != i) {
                float tl = itemLon[i]; itemLon[i] = itemLon[mi]; itemLon[mi] = tl;
                float ta = itemLat[i]; itemLat[i] = itemLat[mi]; itemLat[mi] = ta;
            }
        }
        int K = LATTICE_N;
        float[] cand = new float[K];
        for (int k = 0; k < K; ++k) cand[k] = -AVOID_LANE_MAX + k * LATTICE_STEP;
        // 起步: 自车 (lon=0, eLat) → 障碍 0
        for (int k = 0; k < K; ++k) {
            if (!latticeFeasible(n, 0, cand[k], clear)) {
                dpPrev[k] = Float.POSITIVE_INFINITY;
                continue;
            }
            float d = cand[k] - eLat;
            dpPrev[k] = d * d + LATTICE_CENTER_W * cand[k] * cand[k];
        }
        // 逐障碍转移 (bkRow = 回溯表, 行 = 障碍序号)
        for (int j = 1; j < n; ++j) {
            int bkBase = j * K;
            for (int k = 0; k < K; ++k) {
                dpCur[k] = Float.POSITIVE_INFINITY;
                bkRow[bkBase + k] = -1;
                if (!latticeFeasible(n, j, cand[k], clear)) continue;
                float best = Float.POSITIVE_INFINITY;
                int arg = -1;
                for (int p = 0; p < K; ++p) {
                    if (dpPrev[p] == Float.POSITIVE_INFINITY) continue;
                    float d = cand[k] - cand[p];
                    float c = dpPrev[p] + d * d;
                    if (c < best) {
                        best = c;
                        arg = p;
                    }
                }
                if (arg >= 0) {
                    dpCur[k] = best + LATTICE_CENTER_W * cand[k] * cand[k];
                    bkRow[bkBase + k] = arg;
                }
            }
            System.arraycopy(dpCur, 0, dpPrev, 0, K);
        }
        // 终态取最优; 全 INF = 无可行剖面
        int bestK = -1;
        float bestC = Float.POSITIVE_INFINITY;
        for (int k = 0; k < K; ++k) {
            if (dpPrev[k] < bestC) {
                bestC = dpPrev[k];
                bestK = k;
            }
        }
        if (bestK < 0) return false;
        // 回溯 → (lon,lat) 序列 (正向)
        int[] sel = new int[n];
        sel[n - 1] = bestK;
        for (int j = n - 1; j > 0; --j) sel[j - 1] = bkRow[j * K + sel[j]];
        wpCount = 0;
        for (int j = 0; j < n; ++j) {
            wpLon[wpCount] = itemLon[j];
            wpLat[wpCount] = cand[sel[j]];
            wpCount++;
        }
        // 窗尾出口点: 沿末偏移再走 AVOID_CLEAR_DIST, 给回线预留直线段
        wpLon[wpCount] = itemLon[n - 1] + AVOID_CLEAR_DIST;
        wpLat[wpCount] = cand[sel[n - 1]];
        wpCount++;
        return true;
    }

    /** IDM gap 保命层数据源: 自车系走廊 (|lat|<AVOID_CLEAR) 内最近前向障碍。 */
    private void refreshGapFields(BaseVehicle vehicle) {
        Vector3f fwd = vehicle.getForwardVector(fwdVec);
        float vx = vehicle.getX();
        float vy = vehicle.getY();
        float bestLon = Float.MAX_VALUE;
        float bestLat = Float.NaN;
        for (int i = 0; i < avoidObsList.size(); ++i) {
            float[] o = avoidObsList.get(i);
            float ox = o[0] - vx;
            float oy = o[1] - vy;
            float lon = ox * fwd.x + oy * fwd.z;
            float lat = ox * fwd.z - oy * fwd.x;
            if (lon < -2.0f) continue;
            if (Math.abs(lat) >= AVOID_CLEAR) continue;
            if (lon < bestLon) {
                bestLon = lon;
                bestLat = lat;
            }
        }
        if (bestLon == Float.MAX_VALUE) {
            obsLon = Float.NaN;
            obsLat = Float.NaN;
        } else {
            obsLon = bestLon;
            obsLat = bestLat;
        }
    }

    /** 窗内障碍收集到 item 数组 (锁定系或车头系; 返回条数)。 */
    private int collectItems(float fx, float fy, float ox0, float oy0, float eLon, float back) {
        int n = 0;
        for (int i = 0; i < avoidObsList.size() && n < itemLon.length; ++i) {
            float[] o = avoidObsList.get(i);
            float rx = o[0] - ox0;
            float ry = o[1] - oy0;
            float flon = rx * fx + ry * fy;
            float flat = rx * fy - ry * fx;
            float rel = flon - eLon;
            if (rel < -back || rel > AVOID_WINDOW) continue;
            if (Math.abs(flat) > AVOID_LANE_MAX + AVOID_CLEAR) continue;
            itemLon[n] = flon;
            itemLat[n] = flat;
            itemX[n] = o[0];
            itemY[n] = o[1];
            ++n;
        }
        return n;
    }

    /** 模式变化日志 (AvoidDiag 节流)。 */
    private void logAvoidMode(int mode, String detail) {
        if (mode == avoidDiagMode) return;
        avoidDiagMode = mode;
        String name = mode == AVOID_DETOUR ? "DETOUR"
                : mode == AVOID_BLOCKED ? "BLOCKED"
                : mode == AVOID_RETURN ? "RETURN" : "NONE";
        Logger.printLog("[AvoidDiag] mode: " + name + detail);
        DriveDiag.event("AVOID", name + detail);
    }

    /** 绕行状态复位 (start/resumeRoute)。 */
    private void resetAvoidance() {
        avoidMode = AVOID_NONE;
        avoidTargetX = 0.0f;
        avoidTargetY = 0.0f;
        avoidOriginX = 0.0f;
        avoidOriginY = 0.0f;
        obsLon = Float.NaN;
        obsLat = Float.NaN;
        avoidObsList.clear();
        wpCount = 0;
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

    /** 命名街道吸附合格线 (格): 超过则优先用 worldmap 数据源 (C2)。 */
    private static final float STREET_SNAP_OK = 40.0f;

    /**
     * 路线源选择 (C2 双源): streets 优先 (起终点都贴近命名街道时行为与旧版完全一致),
     * 否则 worldmap (更完整的道路图层); worldmap 失败回退 streets 宽松; 全失败 null
     * (调用方 direct)。成功时写 routeKind ("road" / "roadwm")。
     */
    private ArrayList<float[]> computeRoute(float sx, float sy, float tx, float ty) {
        RoadNetwork.resetRouteFlags();   // 每次规划复位: 只看本次的位移/裁尾标志
        float snapS = RoadNetwork.snapDistance(sx, sy);
        float snapT = RoadNetwork.snapDistance(tx, ty);
        if (snapS <= STREET_SNAP_OK && snapT <= STREET_SNAP_OK) {
            ArrayList<float[]> r = RoadNetwork.findRoute(sx, sy, tx, ty);
            if (r != null && !r.isEmpty()) {
                routeKind = "road";
                return r;
            }
        }
        ArrayList<float[]> wm = RoadNetwork.findRouteWorldMap(sx, sy, tx, ty);
        if (wm != null && !wm.isEmpty()) {
            routeKind = "roadwm";
            return wm;
        }
        ArrayList<float[]> r = RoadNetwork.findRoute(sx, sy, tx, ty);
        if (r != null && !r.isEmpty()) {
            routeKind = "road";
            return r;
        }
        return null;
    }

    /**
     * 路线规划 (一次性路线语义, 2026-09-06 用户拍板): 锚定一次 → 规划一条路线,
     * 车沿这条线一直走到终点; 路线平时冻结, 只允许两个事件改动它 — 向前续段
     * (append=true: 路线尽头/边界等待续段)。周期性重规划已删除。
     *
     * 路线来源 (C2 双源): ① 起终点都贴近命名街道 (snap ≤ STREET_SNAP_OK) →
     * streets.xml 路网 (现役行为, 城镇内优先); ② 否则 worldmap 道路图层路网
     * (更完整: 无名路/小区路/林区路) — routeKind="roadwm"; ③ worldmap 不可用
     * 回退 streets 宽松; ④ 都失败 → 直线直奔 (direct, 穿墙保证可达)。
     * 整图数据一次性可得, 无流式加载限制。
     *
     *                    新段替换 pathIdx 之后的部分
     * @return false = 规划失败 (冷却期内返回"路线仍有效"布尔)
     */
    private boolean planRoute(BaseVehicle vehicle, long now, boolean append, boolean replaceTail) {
        if (now < replanCooldownMs) return path != null && pathIdx < path.size();
        replanCooldownMs = now + REPLAN_COOLDOWN_MS;

        ArrayList<float[]> assembled = computeRoute(
                vehicle.getX(), vehicle.getY(), targetX, targetY);
        if (assembled != null && !assembled.isEmpty()) {
            // routeKind / routeEndsAtRoad 已由 computeRoute 写入
            routeEndsAtRoad = RoadNetwork.lastRouteTrimmed;
        } else {
            assembled = new ArrayList<float[]>();
            assembled.add(new float[]{targetX, targetY});
            routeKind = "direct";
            routeEndsAtRoad = false;
        }
        // 路线事件常开日志 (每次规划/续段/尾段替换一条, 供实机诊断)
        Logger.printLog("[AutoDrive] route=" + (replaceTail ? "tail-swap " : append ? "extend " : "plan ")
                + routeKind + " (" + assembled.size() + " wp, "
                + (int) dist(vehicle.getX(), vehicle.getY(), targetX, targetY) + " cells to target)");
        // 诊断: 路线全量转储 (坐标 + 类型; 复现"奇怪路线"用)
        DriveDiag.route(replaceTail ? "tail-swap" : append ? "extend" : "plan",
                routeKind, assembled, vehicle.getX(), vehicle.getY(), targetX, targetY);

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
        this.routeEndsAtRoad = false;   // 由本次 planRoute 按结果重设
        this.engineDeadMs = 0;
        this.lastTickMs = 0;
        this.lastScanMs = 0;
        this.replanCooldownMs = 0;
        this.throttleState = THROTTLE_COAST;
        this.pendingNoClipOffMs = 0;
        resetAvoidance();
        resetDiagFields();
        path = null;
        pathIdx = 0;
        routeKind = "";

        if (!planRoute(vehicle, System.currentTimeMillis(), false, false)) {
            this.state = STATE_IDLE;
            this.messageKey = "UI_DrivePanel_MsgNoPath";
            DriveDiag.closeSession();   // 规划失败: 关掉可能已被路线转储打开的会话
            return false;
        }
        // A 规则 (2026-09-12 用户拍板): 目标无道路可达时, 路线终点 = 最近可达道路点,
        // 明确提示用户"到道路尽头, 剩余一段自行驾驶", 而不是画一条穿野直线。
        // F 修复 (2026-09-12): 判定改用"路线是否真的裁了尾"(lastRouteTrimmed) —— 旧实现
        // 按吸附位移判定, 位移大但路线仍带精确终点短驳时 (同分量的远距目标) 会谎报
        // "已停在最近道路"; 现在提示 / 画线 / 到达判定三者同源。
        if (routeEndsAtRoad) {
            setState(STATE_DRIVING, "UI_DrivePanel_MsgRoadEnd");
            log("target beyond road network: snapped " + (int) RoadNetwork.lastSnapDisplacement
                    + " cells to nearest reachable road point");
        } else {
            setState(STATE_DRIVING, "UI_DrivePanel_StatusDriving");
        }
        refreshWorldNoClip();
        DriveDiag.event("SESSION", "start @" + (int) vehicle.getX() + "," + (int) vehicle.getY()
                + " -> " + (int) x + "," + (int) y + " kind=" + routeKind);
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
        this.lastHandleMs = 0;
        resetAvoidance();
        resetDiagFields();
        setState(STATE_DRIVING, "UI_DrivePanel_StatusDriving");
        refreshWorldNoClip();
        DriveDiag.event("SESSION", "resume @" + (int) vehicle.getX() + "," + (int) vehicle.getY());
        return true;
    }

    /** 诊断字段复位 (start/resumeRoute): 防上一会话残留 (上局 seg 值 → 新会话首采
     *  发假 SEG_JUMP, 实测 csv 开局 seg 8->0 即此)。 */
    private void resetDiagFields() {
        diagPhi = 0.0f;
        diagCTE = 0.0f;
        diagSeg = -1;
        diagCorner = 0.0f;
        diagV0 = 0.0f;
    }

    private void setState(int newState, String statusKey) {
        this.state = newState;
        this.messageKey = statusKey;
        this.stateSinceMs = System.currentTimeMillis();
        log("state -> " + newState);
        DriveDiag.event("STATE", newState + " " + statusKey);
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
        this.brakeLatched = false;
        this.psiDotLp = 0;
        this.lastHeadingMs = 0;
        this.lastHandleMs = 0;
        // 原版巡航还玩家干净车况: 不关的话玩家松开油门后车会自己保持旧目标速
        if (lastVehicle != null && lastVehicle.isRegulator()) {
            lastVehicle.setRegulator(false);
        }
        lastVehicle = null;
        DriveDiag.closeSession();   // 诊断会话收尾 (接管/到达/取消) 
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

    /** 路线类型: "road" (命名街道) / "roadwm" (worldmap 道路图层, C2) / "direct"。退出接管后保留 (地图导航线持续到下次锚定)。 */
    public String getRouteKind() {
        return routeKind == null ? "" : routeKind;
    }
}
