package modcore.core;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;
import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import se.krka.kahlua.integration.annotations.LuaMethod;
import se.krka.kahlua.vm.KahluaTable;
import zombie.Lua.LuaManager;
import zombie.iso.FishSchoolManager;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.SpriteDetails.IsoFlagType;
import modcore.utils.Logger;

/**
 * 「探鱼雷达」取数桥 (2026-10-18, 方案 analysis/探鱼雷达-研判与实施方案(已实施-最新版未实测).md)。
 *
 * 数据面: FishSchoolManager (客户端单例) 本地持有全部鱼群数据 (鱼点=私有, 丰度=公开面);
 * 私有成员经 MethodHandles.privateLookupIn 读取 (TimedActionReceipt 先例), 只读不改。
 *
 * 两阶段跨帧扫描 (性能设计, 用户要求):
 *   - fishRadarScanBegin (≈2-4ms): 预扫半径+6 方形窗内的全部鱼点/打窝点 (逐格调用
 *     game 私有 isFishPoint/coordsToHash), 把点存进静态暂存数组;
 *   - fishRadarScanFinish (≈1-3ms): 复刻 game getFishAbundance 的 13×13 逐格语义
 *     (对每个候选格按 (dx 升, dy 升, 鱼点先于打窝) 顺序精确重放; 含 (int)(result+n*0.5)
 *     截断与打窝爬坡 int 除法), 结果构建成 Lua 表。
 * 两阶段各自单帧完成, 由 Lua 侧跨帧驱动 (Begin 一帧 / Finish 下一帧), 避免单帧卡顿。
 *
 * 精确性约定 (与游戏咬钩判定逐格一致, 档位 10/25 界不漂):
 *   - 贡献点收集范围 Chebyshev ≤4 = 影响距离 ≤4.5 (鱼点半量界 r+3.5≤4.5 / 打窝界 4.5),
 *     被收集的格子内再做与 game 完全相同的距离判定, 超界自然被拒;
 *   - 惊鱼 (noiseFishPointDisabler) 在 Begin 暂存时折进 scared 标记 (坐标哈希与 game 同源);
 *   - 水系判定 = sq.getProperties().has(IsoFlagType.water) (FishingUtils.lua:4-10
 *     isWaterCoords 与 FishSchoolManager.generateSplashes:309 同款)。
 *
 * 返回码 (Begin): -1 = 句柄/环境不可用 (Lua 侧探测即自禁用); 0 = seed 未就绪 (MP 进服
 * 尚未收到 fishingData, 重试); 1 = 已暂存, 等 Finish。Finish 返回 {points, chums, heat}
 * 或 nil (未暂存/异常)。heat 为平铺三元组 (x,y,ab), 已按 (ab 降, 距离升) 截前 HEAT_CAP。
 */
public final class FishRadarAPI {

    private static final int HEAT_CAP = 400;
    private static final int WINDOW_PAD = 6;      // 丰度窗 Chebyshev ≤6 外溢
    private static final int CONTRIB_RADIUS = 4;  // 贡献收集范围 (d ≤ 4.5)

    private static MethodHandle hIsFishPoint, hGetRadius, hGetCount, hCoordsToHash, hMinutes, hSeed, hChums, hNoise;
    private static boolean handlesTried, handlesOk, loggedDown, loggedError;

    // ---- Begin 暂存 (静态, 单线程: Lua OnPlayerUpdate / 主线程) ----
    private static boolean staged;
    private static double sCx, sCy, sRadius;
    private static int[] ptX = new int[512], ptY = new int[512], ptN = new int[512];
    private static float[] ptR = new float[512];
    private static boolean[] ptScared = new boolean[512];
    private static int ptCount;
    private static int[] chX = new int[64], chY = new int[64], chMf = new int[64];
    private static boolean[] chScared = new boolean[64];
    private static int chCount;

    // Finish 重放用的贡献缓冲 (大小 = 暂存点总数上界, 不可能溢出)
    private static int[] contribBuf = new int[8];

    private FishRadarAPI() {
    }

    private static boolean ensureHandles() {
        if (handlesTried) {
            return handlesOk;
        }
        handlesTried = true;
        try {
            MethodHandles.Lookup lk = MethodHandles.privateLookupIn(FishSchoolManager.class, MethodHandles.lookup());
            hIsFishPoint = lk.findVirtual(FishSchoolManager.class, "isFishPoint", MethodType.methodType(boolean.class, int.class, int.class));
            hGetRadius = lk.findVirtual(FishSchoolManager.class, "getFishPointRadius", MethodType.methodType(float.class, int.class, int.class));
            hGetCount = lk.findVirtual(FishSchoolManager.class, "getNumberOfFishInPoint", MethodType.methodType(int.class, int.class, int.class));
            hCoordsToHash = lk.findVirtual(FishSchoolManager.class, "coordsToHash", MethodType.methodType(long.class, int.class, int.class));
            hMinutes = lk.findVirtual(FishSchoolManager.class, "getCurrentGameTimeInMinutes", MethodType.methodType(int.class));
            hSeed = lk.findGetter(FishSchoolManager.class, "seed", int.class);
            hChums = lk.findStaticGetter(FishSchoolManager.class, "chumPoints", TLongObjectHashMap.class);
            hNoise = lk.findStaticGetter(FishSchoolManager.class, "noiseFishPointDisabler", TLongIntHashMap.class);
            handlesOk = true;
        }
        catch (Throwable t) {
            handlesOk = false;
            if (!loggedDown) {
                loggedDown = true;
                Logger.printLog("[FishRadar] scan access unavailable: " + t);
            }
        }
        return handlesOk;
    }

    private static void ensurePtCapacity(int need) {
        if (ptX.length >= need) {
            return;
        }
        int cap = ptX.length;
        while (cap < need) {
            cap *= 2;
        }
        ptX = Arrays.copyOf(ptX, cap);
        ptY = Arrays.copyOf(ptY, cap);
        ptN = Arrays.copyOf(ptN, cap);
        ptR = Arrays.copyOf(ptR, cap);
        ptScared = Arrays.copyOf(ptScared, cap);
    }

    private static void ensureChCapacity(int need) {
        if (chX.length >= need) {
            return;
        }
        int cap = chX.length;
        while (cap < need) {
            cap *= 2;
        }
        chX = Arrays.copyOf(chX, cap);
        chY = Arrays.copyOf(chY, cap);
        chMf = Arrays.copyOf(chMf, cap);
        chScared = Arrays.copyOf(chScared, cap);
    }

    /**
     * 阶段 1: 预扫暂存。返回 -1=不可用 / 0=seed 未就绪 / 1=已暂存。
     */
    @LuaMethod(name = "fishRadarScanBegin", global = true)
    public static int fishRadarScanBegin(double cx, double cy, double radius) {
        try {
            if (!ensureHandles()) {
                return -1;
            }
            FishSchoolManager inst = FishSchoolManager.getInstance();
            if (inst == null) {
                return -1;
            }
            int seed = (Integer) hSeed.invoke(inst);
            if (seed == -1) {
                return 0;
            }
            int r = (int) radius;
            if (r < 8) {
                r = 8;
            }
            if (r > 150) {
                r = 150;
            }
            sCx = cx;
            sCy = cy;
            sRadius = r;
            ptCount = 0;
            chCount = 0;

            TLongIntHashMap noise = (TLongIntHashMap) hNoise.invoke();
            TLongObjectHashMap<?> chums = (TLongObjectHashMap<?>) hChums.invoke();
            int ccx = (int) Math.floor(cx);
            int ccy = (int) Math.floor(cy);
            int minX = ccx - r - WINDOW_PAD;
            int maxX = ccx + r + WINDOW_PAD;
            int minY = ccy - r - WINDOW_PAD;
            int maxY = ccy + r + WINDOW_PAD;
            for (int x = minX; x <= maxX; ++x) {
                for (int y = minY; y <= maxY; ++y) {
                    long h = (Long) hCoordsToHash.invoke(inst, x, y);
                    boolean scared = noise.containsKey(h);
                    if ((Boolean) hIsFishPoint.invoke(inst, x, y)) {
                        ensurePtCapacity(ptCount + 1);
                        ptX[ptCount] = x;
                        ptY[ptCount] = y;
                        ptR[ptCount] = (Float) hGetRadius.invoke(inst, x, y);
                        ptN[ptCount] = (Integer) hGetCount.invoke(inst, x, y);
                        ptScared[ptCount] = scared;
                        ++ptCount;
                    }
                    FishSchoolManager.ChumData cd = (FishSchoolManager.ChumData) chums.get(h);
                    if (cd != null) {
                        ensureChCapacity(chCount + 1);
                        chX[chCount] = x;
                        chY[chCount] = y;
                        chMf[chCount] = cd.maxForceTime;
                        chScared[chCount] = scared;
                        ++chCount;
                    }
                }
            }
            staged = true;
            return 1;
        }
        catch (Throwable t) {
            staged = false;
            logError(t);
            return -1;
        }
    }

    /**
     * 阶段 2: 重放 + 建表。返回 {points=..., chums=..., heat=...} 或 nil。
     */
    @LuaMethod(name = "fishRadarScanFinish", global = true)
    public static KahluaTable fishRadarScanFinish() {
        if (!staged) {
            return null;
        }
        staged = false;
        try {
            if (!ensureHandles()) {
                return null;
            }
            IsoCell cell = IsoCell.getInstance();
            if (cell == null) {
                return null;
            }
            FishSchoolManager inst = FishSchoolManager.getInstance();
            int nowMin = (Integer) hMinutes.invoke(inst);
            contribBuf = new int[ptCount + chCount + 8];
            double r2 = sRadius * sRadius;

            KahluaTable res = LuaManager.platform.newTable();

            // ---- 鱼点 (半径内 + 9x9 有水; 含 r/n/ab/scared) ----
            KahluaTable ptsTab = LuaManager.platform.newTable();
            int pi = 0;
            for (int i = 0; i < ptCount; ++i) {
                double ddx = ptX[i] - sCx;
                double ddy = ptY[i] - sCy;
                if (ddx * ddx + ddy * ddy > r2) {
                    continue;
                }
                if (!anyWaterAround(cell, ptX[i], ptY[i])) {
                    continue;
                }
                KahluaTable e = LuaManager.platform.newTable();
                e.rawset("x", (double) ptX[i]);
                e.rawset("y", (double) ptY[i]);
                e.rawset("r", (double) ptR[i]);
                e.rawset("n", (double) ptN[i]);
                e.rawset("ab", (double) replayAb(ptX[i], ptY[i], nowMin));
                e.rawset("scared", ptScared[i] ? Boolean.TRUE : Boolean.FALSE);
                ptsTab.rawset(++pi, e);
            }
            res.rawset("points", ptsTab);

            // ---- 打窝 (半径内; 爬坡期数据 maxForceTime 已含) ----
            KahluaTable chTab = LuaManager.platform.newTable();
            int ci = 0;
            for (int i = 0; i < chCount; ++i) {
                double ddx = chX[i] - sCx;
                double ddy = chY[i] - sCy;
                if (ddx * ddx + ddy * ddy > r2) {
                    continue;
                }
                KahluaTable e = LuaManager.platform.newTable();
                e.rawset("x", (double) chX[i]);
                e.rawset("y", (double) chY[i]);
                e.rawset("scared", chScared[i] ? Boolean.TRUE : Boolean.FALSE);
                chTab.rawset(++ci, e);
            }
            res.rawset("chums", chTab);

            // ---- 热力 (半径内水格 ab>0; 平铺三数组 x,y,ab; (ab 降, 距离升) 截前 HEAT_CAP) ----
            KahluaTable heat = LuaManager.platform.newTable();
            if (ptCount + chCount > 0) {
                int[] hX = new int[256], hY = new int[256], hAb = new int[256];
                long[] hKey = new long[256];
                int hCount = 0;
                int minX = (int) Math.floor(sCx) - (int) sRadius;
                int maxX = (int) Math.floor(sCx) + (int) sRadius;
                int minY = (int) Math.floor(sCy) - (int) sRadius;
                int maxY = (int) Math.floor(sCy) + (int) sRadius;
                for (int x = minX; x <= maxX; ++x) {
                    for (int y = minY; y <= maxY; ++y) {
                        double ddx = x + 0.5 - sCx;
                        double ddy = y + 0.5 - sCy;
                        double d2 = ddx * ddx + ddy * ddy;
                        if (d2 > r2) {
                            continue;
                        }
                        int ab = replayAb(x, y, nowMin);
                        if (ab <= 0) {
                            continue;
                        }
                        if (!isWater(cell, x, y)) {
                            continue;
                        }
                        if (hCount == hX.length) {
                            int cap = hCount * 2;
                            hX = Arrays.copyOf(hX, cap);
                            hY = Arrays.copyOf(hY, cap);
                            hAb = Arrays.copyOf(hAb, cap);
                            hKey = Arrays.copyOf(hKey, cap);
                        }
                        hX[hCount] = x;
                        hY[hCount] = y;
                        hAb[hCount] = ab;
                        long ac = ab > 0xFFFFF ? 0xFFFFF : ab;
                        long dc = (long) d2;
                        if (dc > 0xFFFFF) {
                            dc = 0xFFFFF;
                        }
                        hKey[hCount] = ((0xFFFFFL - ac) << 40) | (dc << 20) | hCount;
                        ++hCount;
                    }
                }
                Arrays.sort(hKey, 0, hCount);
                int cap = Math.min(hCount, HEAT_CAP);
                int idx = 0;
                for (int k = 0; k < cap; ++k) {
                    int hi = (int) (hKey[k] & 0xFFFFFL);
                    // Double.valueOf: 显式装箱让 rawset(int,Object) 在严格阶段唯一匹配
                    heat.rawset(idx + 1, Double.valueOf(hX[hi]));
                    heat.rawset(idx + 2, Double.valueOf(hY[hi]));
                    heat.rawset(idx + 3, Double.valueOf(hAb[hi]));
                    idx += 3;
                }
            }
            res.rawset("heat", heat);
            return res;
        }
        catch (Throwable t) {
            logError(t);
            return null;
        }
    }

    /**
     * 精确重放 getFishAbundance(x,y) 的 13×13 语义 (见类头): 只遍历暂存点/打窝中
     * Chebyshev ≤4 的贡献者, 按 (dx 升, dy 升, 鱼点先于打窝) 排序后逐条累积 ——
     * 与 game 同偏移顺序, (int) 截断/负数/爬坡除法逐位一致。
     */
    private static int replayAb(int x, int y, int nowMin) {
        int cnt = 0;
        for (int i = 0; i < ptCount; ++i) {
            int dx = ptX[i] - x;
            if (dx < -CONTRIB_RADIUS || dx > CONTRIB_RADIUS) {
                continue;
            }
            int dy = ptY[i] - y;
            if (dy < -CONTRIB_RADIUS || dy > CONTRIB_RADIUS) {
                continue;
            }
            contribBuf[cnt++] = ((dx + 8) << 24) | ((dy + 8) << 20) | i;
        }
        for (int i = 0; i < chCount; ++i) {
            int dx = chX[i] - x;
            if (dx < -CONTRIB_RADIUS || dx > CONTRIB_RADIUS) {
                continue;
            }
            int dy = chY[i] - y;
            if (dy < -CONTRIB_RADIUS || dy > CONTRIB_RADIUS) {
                continue;
            }
            contribBuf[cnt++] = (1 << 19) | ((dx + 8) << 24) | ((dy + 8) << 20) | i;
        }
        if (cnt == 0) {
            return 0;
        }
        Arrays.sort(contribBuf, 0, cnt);
        int result = 0;
        for (int k = 0; k < cnt; ++k) {
            int enc = contribBuf[k];
            int dx = (enc >>> 24) - 8;
            int dy = ((enc >>> 20) & 15) - 8;
            int ref = enc & 0x7FFFF;
            double d = Math.sqrt((double) (dx * dx + dy * dy));
            if ((enc & (1 << 19)) == 0) {
                if (ptScared[ref]) {
                    continue;
                }
                double r = (double) (ptR[ref] + 2.0f);
                if (d <= r) {
                    result += ptN[ref];
                } else if (d <= r + 1.5) {
                    result = (int) ((double) result + (double) ptN[ref] * 0.5);
                }
            } else {
                if (chScared[ref]) {
                    continue;
                }
                int mf = chMf[ref];
                if (nowMin > mf) {
                    if (d <= 3.0) {
                        result += 15;
                    } else if (d <= 4.5) {
                        result += 7;
                    }
                } else {
                    int f = 100 - (mf - nowMin);
                    if (d <= 3.0) {
                        result += 15 * f / 100;
                    } else if (d <= 4.5) {
                        result += 7 * f / 100;
                    }
                }
            }
        }
        if (result < 0) {
            result = 0;
        }
        return result;
    }

    private static boolean isWater(IsoCell cell, int x, int y) {
        IsoGridSquare sq = cell.getGridSquare((double) x, (double) y, 0.0);
        return sq != null && sq.getProperties().has(IsoFlagType.water);
    }

    /** 9×9 (Chebyshev ≤4) 邻域内有任意水格 —— 点的影响界 (d≤4.5) 全在水上才算可用点位。 */
    private static boolean anyWaterAround(IsoCell cell, int x, int y) {
        for (int dx = -CONTRIB_RADIUS; dx <= CONTRIB_RADIUS; ++dx) {
            for (int dy = -CONTRIB_RADIUS; dy <= CONTRIB_RADIUS; ++dy) {
                if (isWater(cell, x + dx, y + dy)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void logError(Throwable t) {
        if (loggedError) {
            return;
        }
        loggedError = true;
        Logger.printLog("[FishRadar] scan error: " + t);
    }
}
