/*
 * 驾驶诊断记录 (排障用, 默认关, 载具页面板开关).
 *
 * 背景 (2026-09-11 用户实测反馈): 摆头/路口绕行/奇怪路线三类问题无报错日志,
 * 口头描述难以复现 — 加驾驶诊断采样: 复现问题后把 CSV 交给开发者, 用时间轴
 * 定位前因后果 (联动 temp/sim_avoid 离线重放, 见研判附篇仿真纪律)。
 *
 * 输出: %USERPROFILE%\Zomboid\modcore\logs\drive_<yyyyMMdd_HHmmss>.csv
 *   文件格式 (行前缀判别):
 *     # 注释 (含 #route 路线转储: 锚定/续段时的完整路点坐标)
 *     S,<ms>,x,y,speed,v0,corner,phi,cte,steer,pathIdx,seg,avoid,obsLon,obsLat,throttle,regulator,state  — 5Hz 采样
 *     E,<ms>,<tag>,<detail>                                                                    — 事件
 * 自动事件 (L3/L4):
 *   STATE     状态机切换 (setState)
 *   AVOID     绕行模式切换 (DETOUR/BLOCKED/RETURN/NONE + 侧/距离)
 *   OBS_HIT / OBS_LOST   障碍锁定/丢失
 *   SESSION   出发/续驶
 *   STEER_SAT 舵量饱和 >0.6s (摆头候选)
 *   SEG_JUMP  路径投影段跳变 ≥2 (弯道切向阶跃候选)
 *   SPEED_DROP 非刹车指令下 0.4s 内掉速 ≥15km/h (碰撞指纹)
 *   OSC       3.2s 窗口内舵量过零 ≥5 次 (振荡自动标记)
 *
 * 开销: 写文件在游戏线程, 5Hz 小行 + 事件即时写; flush 每写一次 (崩溃不丢数据),
 * 实测量级微秒级。enable=false 时所有入口第一行早退 (零开销)。
 * 生命周期: 出发/续驶自动开会话, 接管/到达/取消时 closeSession 收尾。
 *
 * 仅依赖共享设施 (Logger + CoreAPI.configDir 路径约定), 禁止 import 其他功能域。
 */
package modcore.drive;

import modcore.utils.Logger;
import zombie.vehicles.BaseVehicle;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public final class DriveDiag {

    private static final long SAMPLE_PERIOD_MS = 200;      // 5Hz
    private static final float SAT_STEER = 0.85f;          // 舵量饱和阈值
    private static final long SAT_MIN_MS = 600;            // 饱和持续 ≥0.6s 才记
    private static final float DROP_KMH = 15.0f;           // 掉速事件阈值
    private static final long DROP_WINDOW_MS = 400;        // 掉速事件窗口
    // 振荡检测 (2026-09-11 实测标定: 摆头 csv 显著翻转最大 4 次/5s, 旧 3.2s/5 次
    // 永不触发; 现 5s 窗 / 任一端 ≥0.4 / ≥3 次, 该样本可被标出)
    private static final int OSC_SAMPLES = 25;             // 5s @5Hz
    private static final float OSC_MIN_AMP = 0.4f;         // 过零计入的最小幅度
    private static final int OSC_MIN_CROSS = 3;            // 过零 ≥3 次 = 振荡
    private static final long OSC_REPEAT_MS = 5000;        // 振荡事件节流

    private static boolean enabled;
    private static BufferedWriter writer;
    private static boolean sessionFailed;

    private static long lastSampleMs;
    private static long lastOscMs;
    private static long satSinceMs;
    private static boolean satFired;
    private static float lastSpeed = Float.NaN;
    private static long lastSpeedMs;
    private static int lastSeg = -1;
    private static final float[] steerHist = new float[OSC_SAMPLES];
    private static int steerHistIdx;
    private static int steerHistCount;

    private DriveDiag() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    /** 开关 (AutoDriveAPI.autoDriveSetDiagnostics / loadConfig 调用)。关 = 立即收尾会话。 */
    public static void setEnabled(boolean v) {
        enabled = v;
        if (!v) {
            closeSession();
        }
    }

    /** 开诊断会话 (幂等; 失败置 sessionFailed 防每帧重试)。 */
    public static synchronized void ensureSession() {
        if (writer != null || sessionFailed) {
            return;
        }
        try {
            File dir = logsDir();
            if (!dir.exists() && !dir.mkdirs()) {
                throw new java.io.IOException("cannot create " + dir);
            }
            String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT).format(new Date());
            File f = new File(dir, "drive_" + stamp + ".csv");
            writer = new BufferedWriter(new FileWriter(f, true));
            writer.write("# modcore drive diagnostics v1\n");
            writer.write("# file " + f.getAbsolutePath() + "\n");
            writer.write("# S,ms,x,y,speed,v0,corner,phi,cte,steer,pathIdx,seg,avoid,obsLon,obsLat,throttle,regulator,state\n");
            writer.write("# E,ms,tag,detail\n");
            writer.flush();
            Logger.printLog("[DriveDiag] session started: " + f.getName());
        } catch (Throwable t) {
            sessionFailed = true;
            Logger.printLog("[DriveDiag] session start failed: " + t.getMessage());
        }
    }

    public static synchronized void closeSession() {
        if (writer == null) {
            return;
        }
        try {
            writer.flush();
            writer.close();
            Logger.printLog("[DriveDiag] session closed");
        } catch (Throwable t) {
            Logger.printLog("[DriveDiag] session close failed: " + t.getMessage());
        }
        writer = null;
        sessionFailed = false;
        lastSampleMs = 0;
        lastOscMs = 0;
        satSinceMs = 0;
        satFired = false;
        lastSpeed = Float.NaN;
        lastSeg = -1;
        steerHistIdx = 0;
        steerHistCount = 0;
    }

    /** 事件记录 (任何一层都可调; 未开会话自动开)。 */
    public static synchronized void event(String tag, String detail) {
        if (!enabled) {
            return;
        }
        ensureSession();
        write("E," + System.currentTimeMillis() + "," + tag + "," + esc(detail));
    }

    /**
     * 路线转储 (planRoute 调用): 锚定/续段时的完整路点坐标逐行记录
     * ("奇怪路线"类问题的直接证据)。
     */
    public static synchronized void route(String label, String kind, ArrayList<float[]> pts,
            float sx, float sy, float tx, float ty) {
        if (!enabled) {
            return;
        }
        ensureSession();
        int n = pts == null ? 0 : pts.size();
        write("#route " + label + " " + kind + " wp=" + n
                + " start=" + (int) sx + "," + (int) sy + " target=" + (int) tx + "," + (int) ty);
        for (int i = 0; i < n; i++) {
            float[] p = pts.get(i);
            write("#wp " + i + " " + f2(p[0]) + " " + f2(p[1]));
        }
    }

    /**
     * 5Hz 状态采样 + 自动异常标记 (tick 顶层调用, 全状态覆盖; 参数由控制器
     * 取当帧值, phi/cte/seg 为最近一次 Stanley 投影结果)。
     */
    public static synchronized void sample(BaseVehicle vehicle, long now,
            float v0, float corner, float phi, float cte, int pathIdx, int seg, int avoid,
            float obsLon, float obsLat, int throttle, boolean regulator,
            int state, boolean arrivalBrake, float steer) {        if (!enabled) {
            return;
        }
        if (now - lastSampleMs < SAMPLE_PERIOD_MS) {
            return;
        }
        lastSampleMs = now;
        ensureSession();
        float speed = Math.abs(vehicle.getCurrentSpeedKmHour());
        float x = vehicle.getX();
        float y = vehicle.getY();

        // --- 自动异常标记 ---
        if (lastSeg >= 0 && seg >= 0 && Math.abs(seg - lastSeg) >= 2) {
            event("SEG_JUMP", "seg " + lastSeg + "->" + seg + " phi=" + f2(phi));
        }
        lastSeg = seg;

        if (Math.abs(steer) >= SAT_STEER) {
            if (satSinceMs == 0) {
                satSinceMs = now;
            } else if (!satFired && now - satSinceMs >= SAT_MIN_MS) {
                event("STEER_SAT", "steer=" + f2(steer) + " cte=" + f2(cte) + " speed=" + f2(speed));
                satFired = true;
            }
        } else {
            satSinceMs = 0;
            satFired = false;
        }

        // throttle: 0=FWD 1=COAST 2=BRAKE (非指令刹车下的骤降 = 碰撞指纹)
        if (!Float.isNaN(lastSpeed) && now - lastSpeedMs <= DROP_WINDOW_MS
                && lastSpeed - speed >= DROP_KMH && throttle != 2 && !arrivalBrake) {
            event("SPEED_DROP", "speed " + f2(lastSpeed) + "->" + f2(speed)
                    + " steer=" + f2(steer) + " cte=" + f2(cte));
        }
        lastSpeed = speed;
        lastSpeedMs = now;

        // 振荡检测 (L4): 3.2s 窗口过零计数
        steerHist[steerHistIdx] = steer;
        steerHistIdx = (steerHistIdx + 1) % OSC_SAMPLES;
        if (steerHistCount < OSC_SAMPLES) {
            steerHistCount++;
        }
        float[] seq = new float[steerHistCount];
        for (int i = 0; i < steerHistCount; i++) {
            seq[i] = steerHist[(steerHistIdx - steerHistCount + i + OSC_SAMPLES) % OSC_SAMPLES];
        }
        int crossings = countCrossings(seq);
        if (crossings >= OSC_MIN_CROSS && now - lastOscMs >= OSC_REPEAT_MS) {
            event("OSC", "crossings=" + crossings + " steer=" + f2(steer)
                    + " cte=" + f2(cte) + " speed=" + f2(speed));
            lastOscMs = now;
        }

        write("S," + now + "," + f2(x) + "," + f2(y) + "," + f2(speed)
                + "," + f2(v0) + "," + f2(corner) + "," + f2(phi) + "," + f2(cte)
                + "," + f2(steer) + "," + pathIdx
                + "," + seg + "," + avoid
                + "," + f2(obsLon) + "," + f2(obsLat)
                + "," + throttle + "," + (regulator ? 1 : 0) + "," + state);
    }

    /** 过零计数 (符号翻转且**任一端** |幅度|≥OSC_MIN_AMP); 供振荡检测与自检。 */
    public static int countCrossings(float[] seq) {
        int c = 0;
        for (int i = 1; i < seq.length; i++) {
            float a = seq[i - 1];
            float b = seq[i];
            if ((Math.abs(a) >= OSC_MIN_AMP || Math.abs(b) >= OSC_MIN_AMP)
                    && ((a >= 0.0f) != (b >= 0.0f))) {
                c++;
            }
        }
        return c;
    }

    /** 日志目录 = <Zomboid>/modcore/logs (与 Logger 同源)。 */
    private static File logsDir() {
        return modcore.utils.Logger.logDir();
    }

    private static void write(String line) {
        if (writer == null) {
            return;
        }
        try {
            writer.write(line);
            writer.write("\n");
            writer.flush();
        } catch (Throwable t) {
            Logger.printLog("[DriveDiag] write failed: " + t.getMessage());
            closeSession();
        }
    }

    private static String esc(String s) {
        if (s == null) {
            return "";
        }
        return s.replace(',', ';').replace('\n', ' ').replace('\r', ' ');
    }

    private static String f2(float v) {
        if (Float.isNaN(v)) {
            return "nan";
        }
        if (Float.isInfinite(v)) {
            return v > 0 ? "inf" : "-inf";
        }
        return String.format(Locale.ROOT, "%.2f", v);
    }
}
