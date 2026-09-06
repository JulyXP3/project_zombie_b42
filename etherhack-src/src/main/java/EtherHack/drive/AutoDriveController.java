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
 * ③ 路线 (tryReplan): 道路网 BFS 为主 (原版车辆事故系统道路判定, 起终点各接驳
 *    30 格内最近道路格, 终点下路直开), 路网断连退全地形兜底; 僵尸不进寻路不触发
 *    重规划, 绕行蛇形 (tryDetour) 已删 — 行驶中可见重规划为零, 仅剩流式加载的
 *    静默延长。
 * 感知节拍 150ms 自适应 (按时长非 tick 数, 卡顿降帧走廊随 dt 等比拉长), 扫描三层:
 * 动态层按列表过滤 O(目标数), 静态层沿前向中心线 + 两侧车道探未加载边界与实体
 * 障碍 (判定与寻路同源, 且对照 IsoChunk 车辆碰撞装配补齐 blocksight/柱体族真值;
 * 中心线进安全速, 侧车道仅转向朝它时压爬行), 量级 <0.1ms/次。重规划三通道:
 * 受阻 1.5s (仅车辆堵路) / 卡死 6s / 周期滚动 10s (共用 2s 冷却; 冷却跳过不计入
 * 受阻清零), 受阻 8s 收场取消, 卡死连续 2 次重规划无效升级倒车。脱困双通道:
 * 卡死倒车 (油门推不动) + 停驻死锁倒车 (带刹怼住无油门, 卡死检测够不着的那条路)。
 * 速度上限族: 弯道角误差线性 + 纯跟踪几何物理限速 (侧向加速度 ~3m/s²) + 偏航
 * (离路径中线越远越慢) + 避障/边界制动距离换算。纵向速度带: 停驶带 (≤2 或障碍
 * 压进爬行档以下 — 停车等绕行, 绝不顶着障碍碾; 绕行/大转向脱困例外) / 爬行带
 * (仅用户设定低速巡航, 油门常给) / 常速带阈值油门-滑行-点刹。
 *
 * 仅依赖共享设施, 禁止 import 任何其他功能域 (drive 域依赖纪律, §八)。
 */
package EtherHack.drive;

import EtherHack.utils.Logger;
import org.joml.Vector3f;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.core.physics.CarController;
import zombie.input.GameKeyboard;
import zombie.input.JoypadManager;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.network.ServerOptions;
import zombie.vehicles.BaseVehicle;

import java.util.ArrayList;

public final class AutoDriveController {

    // ===== 状态机 (EtherAutoDriveAPI.autoDriveGetStateId 对外) =====
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
    private static final float STEER_GAIN = 2.2f;        // 角误差 → 舵量比例
    private static final long STUCK_PUSH_MS = 6000;      // 卡死判定 (油门开而 <1km/h; 推尸属常态, 阈值放宽)
    private static final long REVERSE_MS = 1500;         // 卡死倒车脱困时长
    private static final long BLOCKED_REPLAN_MS = 1500;  // 持续受阻 → 重规划 (研判: >3s 收场, 重规划先行)
    private static final long DETOUR_GIVEUP_MS = 8000;   // 车辆堵路最终收场
    private static final long BOUNDARY_TIMEOUT_MS = 45000; // 边界等待超时
    private static final long REPLAN_COOLDOWN_MS = 2000; // 滚动规划重跑冷却
    private static final long ROLLING_REPLAN_MS = 10000; // 周期滚动重规划 (自愈陈旧路径, 与冷却共用闸门)
    private static final long ENGINE_DEAD_MS = 5000;     // 引擎持续非运行 → 取消
    private static final float STOP_HOLD_KMH = 2.0f;     // 停驻带: target 低于此 → 持续带刹 (N 稳定, 不再 N/1 摆)
    private static final float CRAWL_KMH = 8.0f;         // 爬行带: 油门常给 + 点刹调速 (1 档稳定)
    private static final float NOCLIP_STATIC_KMH = 30.0f; // 伪·自动驾驶: 静态物可穿, 近障兜底碾过档 (用户拍板提速: 12→30)
    private static final float HOLD_REVERSE_MS = 2500f;  // 停驻死锁倒车阈值 (非机动停驶持续过久 → 倒车脱困)
    private static final float HOLD_OBSTACLE_NEAR = 4.5f; // 停驻死锁倒车的障碍近距门槛 (格)
    private static final int STUCK_REPLAN_ESCALATE = 2;  // 连续 N 次卡死重规划仍未脱困 → 倒车 (感知盲区顶角障碍)
    private static final int ROAD_SEARCH_RADIUS = 30;    // 接驳段: 起/终点找最近道路格的搜索半径 (格)

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

    // 持久配置 (EtherAutoDriveAPI.loadConfig/saveConfig 落 EtherHack/config/drive.properties)
    private float cruiseSpeed = 0.0f;       // 用户巡航目标速, 0 = 自适应
    public boolean verbose = false;         // 置 true 输出状态机转换 verbose 日志 (排障用)

    // 控制 (每 tick 算出, writeControls 落盘)
    private float ctlSteer;
    private boolean ctlForward;
    private boolean ctlBackward;
    private boolean ctlBrake;

    // 感知缓存 (150ms)
    private float nearestZombieDist = Float.MAX_VALUE;
    private float nearestVehicleDist = Float.MAX_VALUE;
    private float staticAheadDist = Float.MAX_VALUE;     // 静态实体障碍 (前向中心线)
    private float staticSideDist = Float.MAX_VALUE;      // 静态实体障碍 (两侧车道最近)
    private float staticSideLateral;                     // 侧车道障碍的横向符号 (+左/-右)
    private float boundaryDist = Float.MAX_VALUE;
    private long lastScanMs;
    private final Vector3f fwdVec = new Vector3f();

    // 计时/脱困
    private long lastTickMs;
    private long stateSinceMs;
    private float stuckMs;
    private int stuckReplanCount;          // 连续"重规划成功但仍卡死"计数 (升级倒车用)
    private float blockedMs;
    private float holdStopMs;              // 非机动停驶持续时长 (停驻死锁倒车用)
    private float reverseTimer;
    private boolean recovering;
    private boolean arrivalBrake;
    private long engineDeadMs;
    private long replanCooldownMs;
    private long lastRollingReplanMs;
    private long lastHandleMs;             // 补丁通道最近一次进 handleControls 的时刻 (看门狗用)
    private boolean worldNoClipApplied;    // chunk 重传去重 (与目标态一致则跳过 refresh)

    private AutoDriveController() {
    }

    /**
     * 伪·自动驾驶世界碰撞豁免开关 (BulletNoClipHook 钩子读): 状态非 IDLE 即生效。
     * 期间 IsoChunk.calcPhysics 只保留 Floor 形状, 车辆对墙/栅栏/树/灯柱全部可穿;
     * 僵尸/其他车辆为动态碰撞体不受影响。纯客户端物理, 零新网络面。
     */
    public static boolean isWorldNoClip() {
        return INSTANCE.state != STATE_IDLE;
    }

    // ================================================================
    // 注入入口
    // ================================================================

    private boolean handleControls(BaseVehicle vehicle) {
        try {
            if (state == STATE_IDLE) {
                return false;
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

        // 到达: 距目标 ≤ ARRIVE_RADIUS 判到达, 刹停即结束 (§五)
        float distToTarget = dist(vehicle.getX(), vehicle.getY(), targetX, targetY);
        if (!arrivalBrake && distToTarget <= ARRIVE_RADIUS) {
            arrivalBrake = true;
            log("arrival brake engaged");
        }
        if (arrivalBrake) {
            ctlForward = false;
            ctlBackward = false;
            ctlBrake = absSpeed > 0.4f;
            ctlSteer = 0;
            if (absSpeed <= 0.4f) {
                setState(STATE_ARRIVED, "UI_DrivePanel_MsgArrived");
                deactivate();
            }
            return;
        }

        // 路点推进 + 路径耗尽 → 滚动规划 (长途分段延长, §六)
        advancePath(vehicle);
        if (path == null || pathIdx >= path.size()) {
            tryReplan(vehicle, now);
            if (path == null || pathIdx >= path.size()) {
                // 暂无可延长路径 (等加载/被围): 减速滑行等待下次冷却
                ctlForward = false;
                ctlBrake = absSpeed > 5f;
                ctlSteer = 0;
                return;
            }
        }

        // 周期滚动重规划: 自愈陈旧路径 (静态障碍判定/绕行路线随流式刷新更新),
        // 与受阻/卡死重规划共用冷却闸门 (cooldown 内自动跳过)
        if (now - lastRollingReplanMs >= ROLLING_REPLAN_MS) {
            lastRollingReplanMs = now;
            tryReplan(vehicle, now);
        }

        // ===== 三层速度体系 =====
        float hardCap = hardCap(vehicle);
        float cruise = cruiseSpeed > 0 ? Math.min(cruiseSpeed, hardCap) : Math.min(hardCap, CRUISE_ADAPTIVE);
        float target = cruise;

        // 场景上限 1 — 弯道: 前视方向误差越大上限越低 (转向 clamp 随速度收窄, 物理必需)
        float angleErr = steerError(vehicle);
        float cornerCap = Math.max(hardCap * (1.0f - Math.min(Math.abs(angleErr) / 0.8f, 1.0f) * 0.8f), 12.0f);
        target = Math.min(target, cornerCap);

        // 场景上限 1b — 弯道物理限速: 纯跟踪几何半径 R = L/(2·sin|err|), 侧向加速度
        // ≤ ~2.5 m/s²。穿墙移除了树木这道物理护栏后, 弯道欠速的代价 = 直接冲出路
        // 面进树林 (实测), 弯道收紧一档; 直线路段不受影响。
        float sinErr = Math.abs((float) Math.sin(angleErr));
        if (sinErr > 0.05f) {
            float turnRadius = Math.max(lookaheadDist(absSpeed) / (2.0f * sinErr), 3.0f);
            target = Math.min(target, (float) Math.sqrt(2.5f * turnRadius) * 3.6f);
        }

        // 场景上限 1c — 偏航限速: 车身偏离路径中心线越远越慢 (贴走廊边缘 = 随时冲
        // 出路面, 穿墙后无物理护栏, 收紧阈值让转向尽早收敛回中线)
        float cte = crossTrackError(vehicle);
        if (cte > 1.5f) {
            target = Math.min(target, 8.0f);
        } else if (cte > 1.0f) {
            target = Math.min(target, 15.0f);
        }

        // 场景上限 2 — 避障/边界: 按"允许的制动距离"换算安全速 (sqrt(2·a·d))。
        // 硬避障 = 车辆与未加载边界 (真实碰撞体, 穿不了); 静态物 (栅栏/路灯/灌木)
        // 物理已可穿 (BulletNoClipHook) → 兜底低速碾过。僵尸完全不限速 (2026-09-06
        // 用户拍板"由繁化简": 优先到达目的地, 沿途僵尸撞上即死且车零损伤)。
        target = Math.min(target, safeSpeed(nearestVehicleDist - 1.5f));
        target = Math.min(target, safeSpeed(boundaryDist - 6.0f));
        if (staticAheadDist < Float.MAX_VALUE) {
            target = Math.min(target,
                    Math.max(safeSpeed(staticAheadDist - 1.5f), NOCLIP_STATIC_KMH));
        }

        // 场景上限 2b — 转向内侧的静态物: 压到爬行档防车身内切蹭 (外侧/直线时不
        // 管 — 路径净空核已保证距离, 一律刹会停死, v2 回归教训)
        if (staticSideDist < Float.MAX_VALUE && Math.abs(angleErr) > 0.15f
                && staticSideLateral * angleErr < 0.0f) {
            target = Math.min(target, CRAWL_KMH);
        }

        // 边界刹停线: 制动距离 + 4 格富余 (停于 3×3 物理守卫带内, 物理从未停用, §六)
        float brakeDist = brakingDistance(absSpeed);
        if (boundaryDist <= brakeDist + 4.0f) {
            setState(STATE_BRAKE_TO_BOUNDARY, "UI_DrivePanel_StatusBrakeToBoundary");
            return;
        }

        // ===== 遇阻处置梯 (仅车辆: 僵尸碾过即死不触发, 静态物可穿同然) =====
        // 受阻判定 = 车辆距离 (贴侧通过不误报)。绕行蛇形机动已删 (由繁化简,
        // 2026-09-06): 车辆堵路 = 重规划绕路, 一次到位。
        float obstacleDist = nearestVehicleDist;
        boolean obstacleNear = obstacleDist < brakeDist + 2.0f;
        if (obstacleNear) {
            blockedMs += dtMs;
        } else {
            blockedMs = 0;
        }

        // 持续受阻 (车辆) → 重规划; 仍不可达 → 收场。
        // 注意必须先过冷却闸门再调 tryReplan: 冷却期内 tryReplan 会以"路径仍有效"
        // 返回 true, 若拿它清零 blockedMs, 受阻计时永远涨不满 → 真重规划/放弃
        // 收场双双永不触发, 车带刹怼在障碍前死犟 (实测主根因之一)
        if (blockedMs > BLOCKED_REPLAN_MS) {
            if (now >= replanCooldownMs && tryReplan(vehicle, now)) {
                blockedMs = 0;
            } else if (blockedMs > DETOUR_GIVEUP_MS) {
                cancel("UI_DrivePanel_MsgBlocked");
                return;
            }
        }

        // 卡死检测与倒车脱困
        if (handleStuck(vehicle, dtMs, absSpeed)) {
            return; // 脱困中, 控制已写
        }

        // ===== 纵向: 速度带控制 =====
        // 停驶带: target ≤ 2, 或障碍把目标压进爬行档以下 (尸群/静态贴近) —— 停车等
        // 绕行/重规划接管, 不再油门常给 (否则实测变成顶着尸群低速碾压)。
        // 脱困例外: 绕行路点已设或重规划路径要求大转向 → 低速爬行跟随转向。
        boolean userSlow = cruiseSpeed > 0.0f && cruiseSpeed <= CRAWL_KMH;
        boolean holdStop = target <= STOP_HOLD_KMH || (target < CRAWL_KMH && !userSlow);
        if (holdStop) {
            boolean maneuvering = Math.abs(angleErr) > 0.5f;
            ctlBackward = false;
            if (maneuvering) {
                holdStopMs = 0;
                ctlForward = absSpeed < 4.0f;
                ctlBrake = false;
            } else {
                // 停驻死锁: 非机动停驶 (刹车、朝向已对准路径) 持续过久且障碍仍在
                // 近前 → 倒车脱困。卡死检测依赖 ctlForward 而 _停驶带无油门_, 永不
                // 触发。倒完由 recovering 收尾强制重规划, 拉开距离后路径才可执行。
                holdStopMs += dtMs;
                if (holdStopMs > HOLD_REVERSE_MS && obstacleDist < HOLD_OBSTACLE_NEAR) {
                    holdStopMs = 0;
                    recovering = true;
                    reverseTimer = REVERSE_MS;
                    replanCooldownMs = 0;   // 脱困结束时的重规划立即生效
                    log("hold-stop deadlock -> reversing");
                }
                ctlForward = false;
                ctlBrake = true;
            }
        } else {
            holdStopMs = 0;
            if (target < CRAWL_KMH) {
                // 爬行带: 仅用户设定的低速巡航 — 油门常给 (1 档稳住), 超速点刹压回
                ctlForward = true;
                ctlBackward = false;
                ctlBrake = absSpeed > target + 0.5f;
            } else if (absSpeed < target - 1.0f) {
                ctlForward = true;
                ctlBackward = false;
                ctlBrake = false;
            } else if (absSpeed > target + 2.5f) {
                ctlForward = false;
                ctlBackward = false;
                ctlBrake = absSpeed > target + 5.0f; // 超速带内滑行, 超太多才点刹
            } else {
                ctlForward = false;
                ctlBackward = false;
                ctlBrake = false;
            }
        }

        // ===== 横向: 纯跟踪转向 (预瞄点方向误差 → 模拟舵量) =====
        float[] desired = lookaheadPoint(vehicle);
        float desiredAngle = (float) Math.atan2(desired[1] - vehicle.getY(), desired[0] - vehicle.getX());
        float err = wrapPi(desiredAngle - heading(vehicle));
        // 死后向抖动抑制: 路点恰在车正后方时 err 在 +π/−π 间逐帧翻转 → 左右满舵
        // 交替原地打转 (实测"出发前原地转几圈")。接近 ±π 一律强制右转, 确定性脱出。
        if (Math.abs(err) > 3.0f) {
            err = 3.0f;
        }
        ctlSteer = clamp(err * STEER_GAIN, -1.0f, 1.0f);
        if (Math.abs(err) > 0.6f && absSpeed > 35.0f) {
            ctlBrake = true; // 角误差大且车速高 → 弯前制动 (§二 横向控制)
        }
    }

    /** 卡死检测 (油门开而速度 <1km/h 6s — 推尸属常态故放宽): 先重规划, 再倒车脱困。返回 true = 本帧由脱困逻辑控制。 */
    private boolean handleStuck(BaseVehicle vehicle, float dtMs, float absSpeed) {
        boolean pushing = ctlForward && absSpeed < 1.0f;
        if (pushing) {
            stuckMs += dtMs;
        } else if (absSpeed > 2.0f) {
            stuckMs = 0;
            stuckReplanCount = 0;
        }

        if (recovering) {
            reverseTimer -= dtMs;
            ctlForward = false;
            ctlBackward = true;
            ctlBrake = false;
            ctlSteer = 0;
            if (reverseTimer <= 0) {
                recovering = false;
                stuckMs = 0;
                stuckReplanCount = 0;
                tryReplan(vehicle, System.currentTimeMillis());
            }
            return true;
        }

        if (stuckMs > STUCK_PUSH_MS) {
            // 冷却期内不动 stuckMs: 下帧继续尝试, 冷却一过立即真正重规划
            // (旧逻辑无条件清零, 重规划被冷却吞掉后计时重新涨, 空推 2s)
            if (System.currentTimeMillis() >= replanCooldownMs) {
                stuckMs = 0;
                if (tryReplan(vehicle, System.currentTimeMillis())) {
                    // 重规划"成功"不等于脱困: 顶角障碍/车缝在扫描与 BFS 判定外,
                    // 重规划永远返回同一条路。连续两次仍卡死 → 升级倒车
                    // (推尸时偶发倒车重顶属合理脱困, 不再按策略豁免)
                    if (++stuckReplanCount >= STUCK_REPLAN_ESCALATE) {
                        stuckReplanCount = 0;
                        recovering = true;
                        reverseTimer = REVERSE_MS;
                        log("stuck -> reverse after repeated replans");
                    } else {
                        log("stuck -> replanned");
                    }
                } else {
                    recovering = true;
                    reverseTimer = REVERSE_MS;
                    log("stuck -> reversing");
                }
            }
        }
        return false;
    }

    // ---------------- WAIT_LOAD (黑边等待 + 自动续驶, §六) ----------------

    private void waitTick(BaseVehicle vehicle, float dtMs, float absSpeed, long now) {
        // 停在边界前 (物理活跃, 普通停车); 扫描照跑 (tick 头部已做)
        ctlForward = false;
        ctlSteer = 0;

        // 等待期风险: 僵尸逼近可倒车拉开距离 (正常驾驶语义, 非传送, §六)
        if (nearestZombieDist < 6.0f && reverseTimer <= 0 && absSpeed < 2.0f) {
            reverseTimer = 1200f;
        }
        if (reverseTimer > 0) {
            reverseTimer -= dtMs;
            ctlBackward = true;
            ctlBrake = false;
        } else {
            ctlBackward = false;
            ctlBrake = absSpeed > 0.3f;
        }

        // 自动续驶: 边界前推 (新区块落地, 可见距离超过起步阈值) → 重规划延长路径 → 起步
        if (boundaryDist > brakingDistance(30.0f) + 10.0f && tryReplan(vehicle, now)) {
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

    private void scan(BaseVehicle vehicle, float absSpeed) {
        nearestZombieDist = Float.MAX_VALUE;
        nearestVehicleDist = Float.MAX_VALUE;
        boundaryDist = Float.MAX_VALUE;

        IsoCell cell = IsoWorld.instance.currentCell;
        if (cell == null) return;

        Vector3f fwd = vehicle.getForwardVector(fwdVec);
        float fx = fwd.x;
        float fy = fwd.z; // PZ: Vector3f.z 对应世界 y
        float vx = vehicle.getX();
        float vy = vehicle.getY();
        float vz = vehicle.getZ();
        float selfHalf = halfWidth(vehicle);
        float corridor = Math.max(brakingDistance(absSpeed) * 1.5f + 4.0f, state == STATE_WAIT_LOAD ? 40.0f : 12.0f);
        float halfSpan = selfHalf + 1.2f; // 宽 = 车宽 + 两侧各 ~1 格

        // 动态层: 僵尸列表过滤 (O(目标数), 非逐格) — 司机身边的僵尸恰是本机模拟的
        for (IsoZombie z : cell.getZombieList()) {
            if (Math.abs(z.getZ() - vz) > 1.0f) continue;
            float dx = z.getX() - vx;
            float dy = z.getY() - vy;
            if (Math.abs(dx) > corridor + 5.0f || Math.abs(dy) > corridor + 5.0f) continue;
            float s = dx * fx + dy * fy;
            if (s < -2.0f || s > corridor) continue;
            float l = Math.abs(-dx * fy + dy * fx);
            if (l > halfSpan) continue;
            float eff = Math.max(0.0f, s - selfHalf);
            if (eff < nearestZombieDist) nearestZombieDist = eff;
        }

        // 动态层: 其他载具 = 硬障碍 (不可碾)
        for (BaseVehicle v : cell.getVehicles()) {
            if (v == vehicle) continue;
            if (Math.abs(v.getZ() - vz) > 1.0f) continue;
            float dx = v.getX() - vx;
            float dy = v.getY() - vy;
            if (Math.abs(dx) > corridor + 5.0f || Math.abs(dy) > corridor + 5.0f) continue;
            float s = dx * fx + dy * fy;
            if (s < -2.0f || s > corridor) continue;
            float l = Math.abs(-dx * fy + dy * fx);
            if (l > halfSpan + halfWidth(v)) continue;
            float eff = Math.max(0.0f, s - selfHalf - halfWidth(v));
            if (eff < nearestVehicleDist) nearestVehicleDist = eff;
        }

        // 静态层: 边界距离 = 前向第一个未加载格 (getGridSquare 空洞, 客户端免费数据);
        // 实体障碍 (staticBlocked 与寻路同源) 分两路: 中心线距离 → 安全速 (可刹停);
        // 侧车道距离+内外侧符号 → 仅转向朝它时压爬行档 (车身内切防蹭)。只刹中心线:
        // 车侧静态物由路径净空核保证 — 一律刹会停死 (v2 回归), 只看中心线又会蹭上
        // 弯道内切的灯柱/围栏 (v2 残留), 两轮实测校准的折中。
        int zFloor = (int) Math.floor(vz);
        int steps = (int) corridor;
        staticAheadDist = Float.MAX_VALUE;
        staticSideDist = Float.MAX_VALUE;
        staticSideLateral = 0;
        for (int i = 0; i <= steps; i++) {
            float px = vx + fx * i;
            float py = vy + fy * i;
            IsoGridSquare sq = cell.getGridSquare((int) Math.floor(px), (int) Math.floor(py), zFloor);
            if (sq == null) {
                boundaryDist = i;
                break;
            }
            if (staticAheadDist == Float.MAX_VALUE && AutoDrivePathfinder.staticBlocked(sq)) {
                staticAheadDist = Math.max(0.0f, i - selfHalf);
            }
            if (staticSideDist == Float.MAX_VALUE) {
                for (int s = -1; s <= 1; s += 2) {
                    // 垂直向量取 (fy, -fx) = 车头**左**方 (PZ y 朝南, 旧写法 (-fy, fx)
                    // 实为右侧, 与 staticSideLateral"+左/-右"约定相反, 内侧判定
                    // `lateral·err<0` 因此反向 — 内切侧永不压速, 外侧白压, 实测蹭灯柱)
                    float sx = px + fy * (halfSpan * s);
                    float sy = py - fx * (halfSpan * s);
                    IsoGridSquare ssq = cell.getGridSquare((int) Math.floor(sx), (int) Math.floor(sy), zFloor);
                    if (ssq != null && AutoDrivePathfinder.staticBlocked(ssq)) {
                        staticSideDist = Math.max(0.0f, i - selfHalf);
                        staticSideLateral = halfSpan * s;   // +左 / -右 (相对车头)
                        break;
                    }
                }
            }
        }
    }

    /** 横向避让 (tryDetour) 已整体删除 (2026-09-06 由繁化简): 车辆堵路由重规划一次到位。 */

    // ================================================================
    // 路径与规划
    // ================================================================

    private void advancePath(BaseVehicle vehicle) {
        if (path == null) return;
        while (pathIdx < path.size()) {
            float[] wp = path.get(pathIdx);
            float dx = wp[0] - vehicle.getX();
            float dy = wp[1] - vehicle.getY();
            if (dx * dx + dy * dy < 9.0f) {
                pathIdx++;
            } else {
                break;
            }
        }
    }

    /** 预瞄距离 (随速度拉长), 弯道限速与跟踪点共用同一把尺。 */
    private static float lookaheadDist(float speedKmh) {
        return clamp(4.0f + Math.abs(speedKmh) * 0.35f, 6.0f, 16.0f);
    }

    /** 预瞄点: 自当前路点起累计弧长 ≥ 前视距离 (随速度拉长) 的第一个路点。 */
    private float[] lookaheadPoint(BaseVehicle vehicle) {
        float lookahead = lookaheadDist(vehicle.getCurrentSpeedKmHour());
        float acc = 0;
        float[] prev = {vehicle.getX(), vehicle.getY()};
        for (int i = pathIdx; i < path.size(); i++) {
            float[] wp = path.get(i);
            acc += dist(prev[0], prev[1], wp[0], wp[1]);
            if (acc >= lookahead) return wp;
            prev = wp;
        }
        return path.get(path.size() - 1);
    }

    /** 车到路径中心线的横向偏差 (投影 pathIdx 前后几段), 供偏航限速。 */
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
     * 从当前位置重跑路线 (rolling / 重规划) — 用户拍板"锚定之后直接按道路往终点走"
     * (2026-09-06 由繁化简): ① 主路由 = 道路网 BFS, 直接以终点坐标为目标 — 长途
     * (终点区域未加载) 时借寻路器"最近可达格"语义停在已加载路网边缘, 随流式加载
     * 由滚动重规划静默延长 (首版按"终点接驳格"定位路网, 终点未加载时 roadB 恒空
     * → 全程退化全地形直奔, 实测跨草地树林); ② 终点短驳仅在路网末端离终点
     * ≤ROAD_SEARCH_RADIUS 时附加 (路网直达或近旁); ③ 兜底 = 全地形 BFS (起点
     * 30 格内无路 / 道路 BFS 空时直奔, 穿墙保证可达)。
     */
    private boolean tryReplan(BaseVehicle vehicle, long now) {
        if (now < replanCooldownMs) return path != null && pathIdx < path.size();
        replanCooldownMs = now + REPLAN_COOLDOWN_MS;

        int z = (int) Math.floor(vehicle.getZ());
        int sx = (int) Math.floor(vehicle.getX());
        int sy = (int) Math.floor(vehicle.getY());
        ArrayList<float[]> assembled = null;
        String routeKind = "fallback";

        // ① 道路网路线 (主): 以终点为目标的道路 BFS — 未加载区天然止于路网边缘
        int[] roadA = AutoDrivePathfinder.nearestRoadCell(sx, sy, z, ROAD_SEARCH_RADIUS);
        if (roadA != null) {
            AutoDrivePathfinder.Path roadPath = AutoDrivePathfinder.findPath(roadA[0], roadA[1],
                    z, targetX, targetY, vehicle, true);
            if (roadPath != null && !roadPath.points.isEmpty()) {
                assembled = roadPath.points;
                float[] lastWp = assembled.get(assembled.size() - 1);
                // 路网末端贴近终点 → 下路短驳; 远离 (长途未加载) → 止于路缘静默延长
                if (dist(lastWp[0], lastWp[1], targetX, targetY) <= ROAD_SEARCH_RADIUS) {
                    assembled.add(new float[]{targetX, targetY});
                }
                routeKind = "road";
            }
        }

        // ② 兜底: 全地形直奔 (起点旁无路可接驳 / 道路 BFS 空)
        if (assembled == null) {
            AutoDrivePathfinder.Path p = AutoDrivePathfinder.findPath(sx, sy, z,
                    targetX, targetY, vehicle, false);
            if (p == null || p.points.isEmpty()) {
                Logger.printLog("[AutoDrive] route=fallback FAILED (no reachable cell)");
                return false;
            }
            assembled = p.points;
        }
        // 路线选择常开日志 (每次重规划一条, 供实机诊断道路/兜底切换)
        Logger.printLog("[AutoDrive] route=" + routeKind + " (" + assembled.size() + " wp, "
                + (int) dist(vehicle.getX(), vehicle.getY(), targetX, targetY) + " cells to target)");
        path = assembled;
        pathIdx = 0;
        return true;
    }

    // ================================================================
    // 生命周期
    // ================================================================

    /** 锚定终点并出发 (EtherAutoDriveAPI.autoDriveTarget)。返回 false = 未出发。 */
    public boolean start(BaseVehicle vehicle, float x, float y) {
        this.targetX = x;
        this.targetY = y;
        this.arrivalBrake = false;
        this.stuckMs = 0;
        this.stuckReplanCount = 0;
        this.blockedMs = 0;
        this.holdStopMs = 0;
        this.recovering = false;
        this.reverseTimer = 0;
        this.engineDeadMs = 0;
        this.lastTickMs = 0;
        this.lastScanMs = 0;
        this.replanCooldownMs = 0;
        this.lastRollingReplanMs = System.currentTimeMillis();
        path = null;
        pathIdx = 0;

        if (!tryReplan(vehicle, System.currentTimeMillis())) {
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

    private void setState(int newState, String statusKey) {
        this.state = newState;
        this.messageKey = statusKey;
        this.stateSinceMs = System.currentTimeMillis();
        log("state -> " + newState);
    }

    /** 世界碰撞豁免开关跟随状态: 目标态与已应用态一致则跳过, 切换时重传已加载 chunk。 */
    private void refreshWorldNoClip() {
        boolean active = state != STATE_IDLE;
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

    /** 退出接管: 状态回 IDLE, 清零 clientControls (无状态残留, §五)。 */
    private void deactivate() {
        this.state = STATE_IDLE;
        this.path = null;
        this.pathIdx = 0;
        this.stuckMs = 0;
        this.stuckReplanCount = 0;
        this.blockedMs = 0;
        this.holdStopMs = 0;
        this.recovering = false;
        this.reverseTimer = 0;
        this.arrivalBrake = false;
        this.ctlSteer = 0;
        this.ctlForward = false;
        this.ctlBackward = false;
        this.ctlBrake = false;
        this.lastTickMs = 0;
        this.engineDeadMs = 0;
        this.lastRollingReplanMs = 0;
        this.lastHandleMs = 0;
        refreshWorldNoClip();
        // 消息保留给 UI 状态行读 (ARRIVED 状态语义), 状态归零后由 autoDriveGetMessage 兜底
    }

    // ================================================================
    // 工具
    // ================================================================

    /** 直接写一条状态消息 (API 前置校验失败时用)。 */
    public void setMessage(String key) {
        this.messageKey = key == null ? "" : key;
    }

    /** 硬顶 (全自动, 用户不可越): min(服务端 speedLimit × 0.85, 车辆脚本极速), §五。 */
    public float hardCap(BaseVehicle vehicle) {
        float limit = (float) ServerOptions.instance.speedLimit.getValue();
        return Math.min(limit * SPEED_LIMIT_MARGIN, Math.max(vehicle.getMaxSpeed(), 20.0f));
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

    private float steerError(BaseVehicle vehicle) {
        if (path == null || pathIdx >= path.size()) return 0;
        float[] desired = lookaheadPoint(vehicle);
        float desiredAngle = (float) Math.atan2(desired[1] - vehicle.getY(), desired[0] - vehicle.getX());
        return wrapPi(desiredAngle - heading(vehicle));
    }

    /** 半宽 (格): 车辆脚本 extents.x / 2, 下限 0.9。 */
    private static float halfWidth(BaseVehicle vehicle) {
        try {
            float w = vehicle.getScript().getExtents().x() * 0.5f;
            return Math.max(0.9f, w);
        } catch (Throwable t) {
            return 1.0f;
        }
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
    // 状态读取 (EtherAutoDriveAPI 转发)
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
        return state == STATE_DRIVING || state == STATE_BRAKE_TO_BOUNDARY || state == STATE_WAIT_LOAD;
    }

    public boolean hasPath() {
        return path != null && !path.isEmpty();
    }
}
