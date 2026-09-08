/*
 * 自动驾驶 Lua 暴露面 (研判文档 §八): Lua 只发命令、读状态 — 单向依赖,
 * 状态机自洽于 Java 侧 (AutoDriveController), UI 改版不动核心。
 *
 * 静态 @LuaMethod 方法由 SafeExposer.exposeAutoDrive() 暴露为 Lua 全局函数
 * (EtherAPI.loadAPI 挂钩; i18n 全局 getTranslate 由 EtherLuaMethods 提供)。
 *
 * 持久配置: EtherHack/config/drive.properties (cruiseSpeed), 同 EtherAPI 配置模式。
 *
 * 仅依赖共享设施, 禁止 import 任何其他功能域 (drive 域依赖纪律, §八)。
 */
package EtherHack.drive;

import EtherHack.utils.Logger;
import se.krka.kahlua.integration.annotations.LuaMethod;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.vehicles.BaseVehicle;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public final class EtherAutoDriveAPI {

    private static final String CONFIG_DIR = "EtherHack/config";
    private static final String CONFIG_PATH = "EtherHack/config/drive.properties";

    private EtherAutoDriveAPI() {
    }

    // ================================================================
    // 命令
    // ================================================================

    /**
     * 锚定终点 (世界坐标, z=0 车辆贴地) 并立即出发; 再锚定 = 换终点并重规划 (§五)。
     * 前置: 本地玩家在本车驾驶位。终点在未加载区 → 滚动规划先开到已知边界。
     */
    @LuaMethod(name = "autoDriveTarget", global = true)
    public static boolean autoDriveTarget(double x, double y) {
        BaseVehicle vehicle = driverVehicle();
        if (vehicle == null) {
            INSTANCE().setMessage("UI_DrivePanel_MsgNoVehicle");
            Logger.printLog("[AutoDrive] target rejected: not driving a vehicle");
            return false;
        }
        return INSTANCE().start(vehicle, (float) x, (float) y);
    }

    /** 停止自动驾驶 (UI 按钮 / 下车等由控制器自行取消)。 */
    @LuaMethod(name = "autoDriveStop", global = true)
    public static void autoDriveStop() {
        INSTANCE().stop("UI_DrivePanel_MsgStopped");
    }

    /**
     * 清除导航线与终点标记 (仅 IDLE 态: 接管/到达后路线保留显示至下次锚定,
     * 改主意不去原目的地时由此显式清除)。返回 false = 自动驾驶进行中, 忽略。
     */
    @LuaMethod(name = "autoDriveClearRoute", global = true)
    public static boolean autoDriveClearRoute() {
        return INSTANCE().clearRoute();
    }

    /**
     * 继续导航: 手动接管后恢复自动驾驶, 沿保留的原路线开往原终点 (所见即所行)。
     * 前置: 本地玩家在本车驾驶位 + 有保留路线。返回 false = 不满足。
     */
    @LuaMethod(name = "autoDriveResume", global = true)
    public static boolean autoDriveResume() {
        BaseVehicle vehicle = driverVehicle();
        if (vehicle == null) {
            INSTANCE().setMessage("UI_DrivePanel_MsgNoVehicle");
            Logger.printLog("[AutoDrive] resume rejected: not driving a vehicle");
            return false;
        }
        return INSTANCE().resumeRoute(vehicle);
    }

    /**
     * 车辆重置 (宽限期兜底): 嵌墙车 15s 内没开出会被挤进地里 — 此按钮
     * 续期碰撞豁免 15s + 把车抬回地面高度。返回 false = 不在驾驶位。
     */
    @LuaMethod(name = "autoDriveResetVehicle", global = true)
    public static boolean autoDriveResetVehicle() {
        BaseVehicle vehicle = driverVehicle();
        if (vehicle == null) {
            INSTANCE().setMessage("UI_DrivePanel_MsgNoVehicle");
            return false;
        }
        INSTANCE().grantNoClipGrace();
        boolean ok = BulletNoClipHook.resetVehiclePose(vehicle);
        Logger.printLog("[AutoDrive] vehicle reset: pose=" + ok);
        return ok;
    }

    /** 巡航目标速 km/h, 0 = 自适应 (~55, 仍被硬顶钳制) — 唯一速度旋钮 (§五)。 */
    @LuaMethod(name = "autoDriveSetCruiseSpeed", global = true)
    public static void autoDriveSetCruiseSpeed(double kmh) {
        INSTANCE().setCruiseSpeed((float) kmh);
        saveConfig();
    }

    @LuaMethod(name = "autoDriveGetCruiseSpeed", global = true)
    public static double autoDriveGetCruiseSpeed() {
        return INSTANCE().getCruiseSpeed();
    }

    // 遇阻策略已整体移除 (2026-09-06 用户拍板): 僵尸一律低速硬闯, 车辆/边界照常
    // 硬避让 — setPolicy/getPolicy 与 policy 配置键随之删除。

    // ================================================================
    // 状态读取 (UI 状态行)
    // ================================================================

    /** 状态机: 0=IDLE 1=DRIVING 2=BRAKE_TO_BOUNDARY 3=WAIT_LOAD 4=ARRIVED。 */
    // ================================================================
    // 战斗攻击三开关 (载具页「战斗攻击」模块; 导航期间恒开, 开关只控制手动驾驶)
    // ================================================================

    @LuaMethod(name = "autoDriveSetCombatWiggle", global = true)
    public static void autoDriveSetCombatWiggle(double v) {
        AutoDriveController.setCombatWiggle(v != 0.0 && !Double.isNaN(v));
        saveConfig();
    }

    @LuaMethod(name = "autoDriveGetCombatWiggle", global = true)
    public static double autoDriveGetCombatWiggle() {
        return AutoDriveController.isCombatWiggle() ? 1.0 : 0.0;
    }

    @LuaMethod(name = "autoDriveSetCombatZombieKill", global = true)
    public static void autoDriveSetCombatZombieKill(double v) {
        AutoDriveController.setCombatZombieKill(v != 0.0 && !Double.isNaN(v));
        saveConfig();
    }

    @LuaMethod(name = "autoDriveGetCombatZombieKill", global = true)
    public static double autoDriveGetCombatZombieKill() {
        return AutoDriveController.isCombatZombieKill() ? 1.0 : 0.0;
    }

    @LuaMethod(name = "autoDriveSetCombatNoClip", global = true)
    public static void autoDriveSetCombatNoClip(double v) {
        AutoDriveController.setCombatNoClip(v != 0.0 && !Double.isNaN(v));
        saveConfig();
    }

    @LuaMethod(name = "autoDriveGetCombatNoClip", global = true)
    public static double autoDriveGetCombatNoClip() {
        return AutoDriveController.isCombatNoClip() ? 1.0 : 0.0;
    }

    @LuaMethod(name = "autoDriveGetStateId", global = true)
    public static int autoDriveGetStateId() {
        return INSTANCE().getStateId();
    }

    /** 最近一次状态/结果消息的 i18n 键 (空串 = 无)。 */
    @LuaMethod(name = "autoDriveGetMessage", global = true)
    public static String autoDriveGetMessage() {
        return INSTANCE().getMessageKey();
    }

    @LuaMethod(name = "autoDriveGetTargetX", global = true)
    public static double autoDriveGetTargetX() {
        return INSTANCE().getTargetX();
    }

    @LuaMethod(name = "autoDriveGetTargetY", global = true)
    public static double autoDriveGetTargetY() {
        return INSTANCE().getTargetY();
    }

    @LuaMethod(name = "autoDriveIsActive", global = true)
    public static boolean autoDriveIsActive() {
        return INSTANCE().isActive();
    }

    /** 当前车速 km/h (状态行显示), 无车返回 0。 */
    @LuaMethod(name = "autoDriveGetSpeedKmh", global = true)
    public static double autoDriveGetSpeedKmh() {
        BaseVehicle v = currentVehicle();
        return v == null ? 0.0 : v.getCurrentSpeedKmHour();
    }

    /** 服务端限速原值 (登录同步可读, 研判 §四.3)。 */
    @LuaMethod(name = "autoDriveGetSpeedLimit", global = true)
    public static double autoDriveGetSpeedLimit() {
        return INSTANCE().speedLimit();
    }

    /** 上限 = 用户巡航设定 > 车辆极速 (服务端限速只作未设定时的自适应默认)。 */
    @LuaMethod(name = "autoDriveGetHardCap", global = true)
    public static double autoDriveGetHardCap() {
        BaseVehicle v = currentVehicle();
        if (v == null) {
            return INSTANCE().speedLimit() * 0.85;
        }
        return INSTANCE().hardCap(v);
    }

    // ================================================================
    // 路线读数 (AutoDriveMap.lua 地图画线)
    // ================================================================

    /** 当前路线路点数; IDLE/无路线一律 0 (防怠速期脏读)。 */
    @LuaMethod(name = "autoDriveGetRouteCount", global = true)
    public static int autoDriveGetRouteCount() {
        return INSTANCE().getRouteCount();
    }

    /** 第 i 个路点世界坐标 X; 越界返回 0。 */
    @LuaMethod(name = "autoDriveGetRouteX", global = true)
    public static double autoDriveGetRouteX(int i) {
        return INSTANCE().getRouteX(i);
    }

    /** 第 i 个路点世界坐标 Y; 越界返回 0。 */
    @LuaMethod(name = "autoDriveGetRouteY", global = true)
    public static double autoDriveGetRouteY(int i) {
        return INSTANCE().getRouteY(i);
    }

    /** 当前推进下标 (pathIdx)。 */
    @LuaMethod(name = "autoDriveGetRouteIndex", global = true)
    public static int autoDriveGetRouteIndex() {
        return INSTANCE().getRouteIndex();
    }

    /** 路线类型: "road" (大地图路网) / "direct" (直线兜底) / "" (IDLE 无路线)。 */
    @LuaMethod(name = "autoDriveGetRouteKind", global = true)
    public static String autoDriveGetRouteKind() {
        return INSTANCE().getRouteKind();
    }

    // ================================================================
    // 配置持久化 (EtherHack/config/drive.properties)
    // ================================================================

    /** 首次触碰本类 (任一 autoDrive* Lua 调用, 如 UI build) 时自动加载持久化配置
     * (修复存量缺陷: loadConfig 此前无调用点, 巡航速度只存不读)。 */
    static {
        loadConfig();
    }

    public static void loadConfig() {
        File f = new File(CONFIG_PATH);
        if (!f.exists()) {
            return;
        }
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(f)) {
            props.load(in);
            INSTANCE().setCruiseSpeed(parseFloat(props.getProperty("cruiseSpeed", "0")));
            AutoDriveController.setCombatWiggle("true".equals(props.getProperty("combatWiggle", "false")));
            AutoDriveController.setCombatZombieKill("true".equals(props.getProperty("combatZombieKill", "false")));
            AutoDriveController.setCombatNoClip("true".equals(props.getProperty("combatNoClip", "false")));
            Logger.printLog("[AutoDrive] config loaded (cruise="
                    + props.getProperty("cruiseSpeed") + ")");
        } catch (IOException e) {
            Logger.printLog("[AutoDrive] config load failed: " + e.getMessage());
        }
    }

    public static void saveConfig() {
        try {
            File dir = new File(CONFIG_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            Properties props = new Properties();
            props.setProperty("cruiseSpeed", String.valueOf(INSTANCE().getCruiseSpeed()));
            props.setProperty("combatWiggle", String.valueOf(AutoDriveController.isCombatWiggle()));
            props.setProperty("combatZombieKill", String.valueOf(AutoDriveController.isCombatZombieKill()));
            props.setProperty("combatNoClip", String.valueOf(AutoDriveController.isCombatNoClip()));
            props.remove("policy");   // 旧键清出 (停车等待时代)
            props.remove("policy2");  // 旧键清出 (两档策略时代)
            try (FileOutputStream out = new FileOutputStream(CONFIG_PATH)) {
                props.store(out, null);
            }
        } catch (IOException e) {
            Logger.printLog("[AutoDrive] config save failed: " + e.getMessage());
        }
    }

    // ================================================================
    // 内部
    // ================================================================

    private static AutoDriveController INSTANCE() {
        return AutoDriveController.getInstance();
    }

    /** 本地玩家当前所坐载具 (任意座位, 供状态行显示车速)。 */
    private static BaseVehicle currentVehicle() {
        IsoPlayer player = IsoPlayer.getInstance();
        if (player == null) {
            return null;
        }
        return player.getVehicle();
    }

    /** 本地玩家为司机的载具 (自动驾驶前置, 授权随驾驶位而来, 研判 §一)。 */
    private static BaseVehicle driverVehicle() {
        IsoPlayer player = IsoPlayer.getInstance();
        if (player == null) {
            return null;
        }
        BaseVehicle vehicle = player.getVehicle();
        if (vehicle == null) {
            return null;
        }
        IsoGameCharacter driver = vehicle.getDriver();
        if (driver != player) {
            return null;
        }
        return vehicle;
    }

    private static float parseFloat(String s) {
        try {
            return Float.parseFloat(s.trim());
        } catch (Exception e) {
            return 0.0f;
        }
    }
}
