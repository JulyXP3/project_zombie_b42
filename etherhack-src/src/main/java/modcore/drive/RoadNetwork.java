/*
 * 路网模块: 导航路线直接取自大地图街道中心线矢量 (media/maps/<地图目录>/streets.xml)。
 * 大地图渲染的街道名标签就是这份数据 (ISMapDefinitions.addStreetData), 每条街道 =
 * 命名折线 (绝对世界格坐标, 渲染端直接 worldToUI 同一空间) + 路宽 —— 导航路线
 * 天生"严格按地图大路走": 零地块扫描、零流式加载限制 (整图锚定瞬间全量可得),
 * 折线即道路真实中心线。
 *
 * 关键坑 (2026-09-07 实测发现): streets 折线在交叉口互相穿越而不分割 (无共享点,
 * 偏移可达 30+ 格; 原版 initConnectedStreets 的 2 格容差搜索只服务路名高亮不管
 * 连通) —— 直接建图是 ~1000 个孤立分量。必须先平面化: 对段-段相交对求交点, 把
 * 两条折线都在交点处切开, 交叉口才成为共享节点。
 *
 * 图模型: 节点 = 折线点 (0.5 格量化去重) + 交点, 边 = 折线内相邻节点 (边权 =
 * 欧氏距离)。锚定 → 起终点各吸附最近节点 → Dijkstra → 中心线路线 → 末尾追加
 * 精确终点 (末段短驳, 穿墙保证可达); 例外 = 最大分量兜底重吸附且位移超
 * ROAD_END_SNAP_MAX: 目标无道路可达, 路线止于最近可达道路点 (裁尾, A 规则)。
 * 路网不可用/断裂返回 null, 调用方 (AutoDriveController) 退化直线直奔
 * (routeKind="direct")。
 *
 * 目录优先级: MapFiles.getCurrentMapFiles() (mods 在前) 逐目录合并; 自制地图
 * 自带 streets.xml 同样生效; 缺失目录跳过, 全部缺失 → direct。
 */
package modcore.drive;

import modcore.utils.Logger;
import zombie.iso.MapFiles;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.PriorityQueue;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public final class RoadNetwork {

    /** 折线点量化 (0.5 格): 世界坐标有 .5 半格, 量化后共享端点/交点精确合并。 */
    private static final float SNAP = 0.5f;
    /** 平面化网格桶边长 (格): 段-段相交对的候选查询粒度。 */
    private static final int HASH_CELL = 32;
    /** worldmap 缺口 portal 半径 (格): 端点焊接只救 weldR 内的缝, mod 地图/数据洞
     *  会留更大的实洞 (实测 B 轮 112 格) — 分量间以带惩罚的虚拟边补洞。 */
    private static final float WM_GAP_MAX = 128.0f;
    /** 缺口 portal 代价倍率 (真实道路边 = 距离 ×1): 真实路可走通时 Dijkstra 绝不
     *  走 portal; 必须跨洞时才选本次路线最近的洞 (旧全局 MST 按全图总长选洞 —
     *  与本次起终点无关, 实测把园区向西跨野接出 1659 格绕行, 已删)。 */
    private static final float WM_GAP_PENALTY = 10.0f;
    /** worldmap 最大分量兜底重吸附距离上限 (格): 超过则放弃 (旧实现无界 →
     *  起终点被吸到 7400 格外同一分量, 画出 6400 格直线, 实测)。 */
    private static final float WM_FALLBACK_MAX = 250.0f;
    /** streets 最大分量兜底重吸附距离上限 (格, F 修复 2026-09-12): streets 兜底
     *  旧实现无界 —— 目标最近街道边落在孤立小分量时 (Riverside 实测终点 369.71 格)
     *  被吸到主网另一条街, 画出 370 格穿野直线 (用户实机截图 + RouteProbeX 复现)。
     *  上限内仍兜底 (A 规则: 终点 = 最近可达道路点 + 提示剩余自行驾驶); 超限返回
     *  null, 调用方回退 worldmap/direct。 */
    private static final float STREET_FALLBACK_MAX = 400.0f;
    /** 兜底重吸附位移 ≤ 此值 (格) 时仍保留"精确终点短驳" (目标就在道路旁 → 送到
     *  门口); 超过则裁掉短驳 —— 路线终点 = 最近可达道路点 (A 规则), 同时置
     *  lastRouteTrimmed 让控制器提示"剩余自行驾驶"并把到达判定锚在路线末点。 */
    public static final float ROAD_END_SNAP_MAX = 10.0f;
    /** 建图期开关: worldmap 图启用分量桥接 (streets 现役行为不动)。 */
    private boolean bridgeGaps;
    /** 大块路面外环 (worldmap 专用): 落面焊接用 (见 buildAreaWelds)。 */
    private ArrayList<float[]> areaRings;
    /** 道路多边形 (含洞) — 路面判定 (中线裁剪, 见 cutOffRoad)。 */
    private ArrayList<float[][]> surfaceFeats;
    private HashMap<Long, ArrayList<Integer>> surfIdx;
    /** 边标记: true = 缺口 portal 虚拟边 (两段式 Dijkstra 先禁后允)。 */
    private boolean[] adjIsPortal;

    private float[] nodeX;
    private float[] nodeY;
    private int[] adjStart;
    private int[] adjNode;
    private float[] adjWeight;
    /** 端点吸附焊桥半径 (格): streets 实测 Riverside T 型路口悬空 4~6 格; worldmap
     *  中线由矩形分解产出, 邻格端点有半格级偏差, 放宽到 8。 */
    private float weldR = 6.0f;
    /** 宽度感知焊桥外延上限 (格): 端点悬空 = 停在目标路**边缘**时缺口 ≈ 目标路
     *  半宽 (实测 Patton St 端点到 Fiddler's Trail 中线 7.6 ≈ 15/2), 固定半径永远
     *  够不着 → 按 street width 属性的半宽和外延; 上限防超宽属性误焊一大片。 */
    private static final float WELD_EXT_MAX = 12.0f;
    /** 每条折线的 street width 属性 (streets 网; worldmap 折线无此属性 → null)。
     *  buildGraph 内部按去重后折线重排对齐。 */
    private float[] polyWidth;

    private static RoadNetwork instance;
    /** C2 道路数据源升级: worldmap 路网第二实例 (懒加载, 独立图)。 */
    private static RoadNetwork instanceWorldMap;
    private static boolean worldMapLoadFailed;

    private RoadNetwork() {
    }

    public static synchronized RoadNetwork getInstance() {
        return instance;
    }

    /** 懒加载: 首次锚定时解析路网 (一次性), 失败返回 false (导航退化直线)。 */
    public static synchronized boolean ensureLoaded() {
        if (instance != null) return true;
        try {
            RoadNetwork net = new RoadNetwork();
            if (!net.load(false)) return false;
            instance = net;
            return true;
        } catch (Throwable t) {
            Logger.error("[AutoDrive] street network load failed", t);
            return false;
        }
    }

    /** C2: worldmap 路网懒加载 (独立第二图; 失败缓存防重试)。 */
    public static synchronized boolean ensureLoadedWorldMap() {
        if (instanceWorldMap != null) return true;
        if (worldMapLoadFailed) return false;
        try {
            RoadNetwork net = new RoadNetwork();
            if (!net.load(true)) {
                worldMapLoadFailed = true;
                return false;
            }
            instanceWorldMap = net;
            return true;
        } catch (Throwable t) {
            Logger.error("[AutoDrive] worldmap network load failed", t);
            worldMapLoadFailed = true;
            return false;
        }
    }

    /** C2: 起终点到最近街道路段的吸附距离 (格); 路网不可用返回 MAX。规划源选择用。 */
    public static float snapDistance(float x, float y) {
        if (!ensureLoaded()) return Float.MAX_VALUE;
        float[] se = instance.nearestEdgeProj(x, y, -1);
        if (se == null) return Float.MAX_VALUE;
        float dx = se[2] - x, dy = se[3] - y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    /** 最近一次规划里"目标被吸附到可达道路点"的位移 (格) — 控制器据此提示用户。 */
    public static float lastSnapDisplacement;

    /** 最近一次规划的路线是否**裁掉了精确终点短驳** (目标无道路可达, 路线止于最近
     *  可达道路点): 控制器据此把 UI 提示切到"剩余自行驾驶", 并把到达判定锚在路线
     *  末点 —— 提示 / 画线 / 到达三者同源, 不再出现"画一条穿野线开到真终点"。 */
    public static boolean lastRouteTrimmed;

    /** 规划前复位 (控制器每次路线计算前调用): 防上一次规划的残留值造成误判。 */
    public static void resetRouteFlags() {
        lastSnapDisplacement = 0f;
        lastRouteTrimmed = false;
    }

    /** C2: worldmap 路网规划 (格式与 findRoute 同; 失败返回 null)。 */
    public static ArrayList<float[]> findRouteWorldMap(float sx, float sy, float tx, float ty) {
        if (!ensureLoadedWorldMap()) return null;
        RoadNetwork net = instanceWorldMap;
        float[] se = net.nearestEdgeProj(sx, sy, -1);
        float[] te = net.nearestEdgeProj(tx, ty, -1);
        if (se == null || te == null) return null;
        ArrayList<float[]> route = net.buildRoute(se, te, tx, ty, true);
        if (route != null && !route.isEmpty()) {
            lastSnapDisplacement = Math.max(dist(sx, sy, se[2], se[3]), dist(tx, ty, te[2], te[3]));
            lastRouteTrimmed = false;
            return route;
        }
        // 不可达 → 数据洞兜底: 最大分量内重吸附 (位移上限 WM_FALLBACK_MAX)
        return fallbackRoute(net, sx, sy, tx, ty, WM_FALLBACK_MAX);
    }

    /**
     * 最大分量兜底 (A 规则 + F 修复 2026-09-12): 起终点重吸附到最大连通分量,
     * 位移超 cap 一律放弃 (无界重吸附会把起终点吸到几千格外, 见各 cap 注释)。
     * 位移超过 ROAD_END_SNAP_MAX 时裁掉精确终点短驳 —— 路线终点 = 最近可达道路点,
     * 并置 lastRouteTrimmed 让控制器提示"剩余自行驾驶"。不可达返回 null。
     */
    private static ArrayList<float[]> fallbackRoute(RoadNetwork net, float sx, float sy,
            float tx, float ty, float cap) {
        int big = net.biggestComponent();
        float[] seb = net.nearestEdgeProj(sx, sy, big);
        float[] teb = net.nearestEdgeProj(tx, ty, big);
        if (seb == null || teb == null) return null;
        float dS = dist(sx, sy, seb[2], seb[3]);
        float dT = dist(tx, ty, teb[2], teb[3]);
        if (dS > cap || dT > cap) return null;
        float disp = Math.max(dS, dT);
        boolean trim = disp > ROAD_END_SNAP_MAX;
        ArrayList<float[]> route = net.buildRoute(seb, teb, tx, ty, !trim);
        if (route == null || route.isEmpty()) return null;
        lastSnapDisplacement = disp;
        lastRouteTrimmed = trim;
        return route;
    }

    public int nodeCount() {
        return nodeX.length;
    }

    /**
     * 路线规划: 起终点各投影吸附到最近路段 (边吸附, 非节点吸附 — 车常在路段
     * 中间, 最近节点可能在身后或旁边街的焊点上, 节点吸附会让路线先倒回/绕圈,
     * 实测"同一条路正前方尽头绕一大圈"根因) → 多源 Dijkstra → 折线中心线路线
     * + 末尾精确终点。最近边可能落在小分量 (悬空边比主网边更近) → INF 时重吸附
     * 到最大连通分量内的最近边 (命名街道网在城镇接口处有无名路数据洞, 实测
     * West Point 接口 96 格), 缺口由首末段直线短驳覆盖; 重吸附位移 >
     * ROAD_END_SNAP_MAX 时裁掉末尾短驳 (路线止于道路点, F 修复 2026-09-12 ——
     * 旧实现无界重吸附画出 370 格穿野直线)。返回 null = 路网不可用。
     */
    public static ArrayList<float[]> findRoute(float sx, float sy, float tx, float ty) {
        if (!ensureLoaded()) return null;
        RoadNetwork net = instance;
        float[] se = net.nearestEdgeProj(sx, sy, -1);
        float[] te = net.nearestEdgeProj(tx, ty, -1);
        if (se == null || te == null) return null;
        ArrayList<float[]> route = net.buildRoute(se, te, tx, ty, true);
        if (route != null && !route.isEmpty()) {
            lastSnapDisplacement = Math.max(dist(sx, sy, se[2], se[3]), dist(tx, ty, te[2], te[3]));
            lastRouteTrimmed = false;
            return route;
        }
        // 不可达 → 数据洞兜底: 大分量内重吸附边 (大分量连通, 必可达); 位移上限
        // STREET_FALLBACK_MAX (F 修复: 旧实现无界, 见常量注释)
        return fallbackRoute(net, sx, sy, tx, ty, STREET_FALLBACK_MAX);
    }

    /**
     * 边吸附 → 多源 Dijkstra → 路线。**两段式** (A2 修复 2026-09-11): 先只走真实
     * 道路边 (禁 portal); 不可达才允许 portal 补数据洞。保证"存在纯道路通路时,
     * 路线 100% 落在道路上" —— 旧单段式里 111 格的跨野 portal 只要比绕行便宜就会
     * 被选中, 实测画出横穿绿地的直线。不可达返回 null。
     *
     * @param appendTarget false = 不追加精确终点短驳 (兜底裁尾: 路线止于道路点,
     *                     见 fallbackRoute 与 lastRouteTrimmed)。
     */
    private ArrayList<float[]> buildRoute(float[] se, float[] te, float tx, float ty,
            boolean appendTarget) {
        ArrayList<float[]> road = buildRoutePass(se, te, tx, ty, appendTarget, false);
        if (road != null) return road;
        return buildRoutePass(se, te, tx, ty, appendTarget, true);
    }

    /** 单段规划 (allowPortals=false 时跳过分量 portal 虚拟边)。不可达返回 null。 */
    private ArrayList<float[]> buildRoutePass(float[] se, float[] te, float tx, float ty,
            boolean appendTarget, boolean allowPortals) {
        int sa = (int) se[0], sb = (int) se[1];
        int tc = (int) te[0], td = (int) te[1];
        // 同一条路段: 直接投影点连线 (无需绕到端点节点, 消除端点折角)
        if (sa == tc && sb == td) {
            ArrayList<float[]> direct = new ArrayList<float[]>(3);
            direct.add(new float[]{se[2], se[3]});
            direct.add(new float[]{te[2], te[3]});
            if (appendTarget) direct.add(new float[]{tx, ty});
            return direct;
        }
        int[] starts = {sa, sb};
        float[] init = {dist(se[2], se[3], nodeX[sa], nodeY[sa]),
                dist(se[2], se[3], nodeX[sb], nodeY[sb])};
        Object[] res = dijkstraMulti(starts, init, allowPortals);
        float[] dist = (float[]) res[0];
        int[] prev = (int[]) res[1];
        float dc = dist[tc] + dist(nodeX[tc], nodeY[tc], te[2], te[3]);
        float dd = dist[td] + dist(nodeX[td], nodeY[td], te[2], te[3]);
        int end = dc <= dd ? tc : td;
        if (Float.isInfinite(dist[end])) return null;
        ArrayList<Integer> nodes = new ArrayList<Integer>();
        for (int cur = end; cur != -1; cur = prev[cur]) nodes.add(cur);
        java.util.Collections.reverse(nodes);
        ArrayList<float[]> route = new ArrayList<float[]>(nodes.size() + 3);
        route.add(new float[]{se[2], se[3]});
        for (int n : nodes) route.add(new float[]{nodeX[n], nodeY[n]});
        // 尾部折角消除: 终点投影点已越过末节点 → 丢末节点直连投影点
        if (nodes.size() >= 2) {
            int lastN = nodes.get(nodes.size() - 1);
            int prevN = nodes.get(nodes.size() - 2);
            float dLast = dist(nodeX[lastN], nodeY[lastN], te[2], te[3]);
            float dPrev = dist(nodeX[prevN], nodeY[prevN], te[2], te[3]);
            if (dPrev < dLast) route.remove(route.size() - 1);
        }
        route.add(new float[]{te[2], te[3]});
        if (appendTarget) route.add(new float[]{tx, ty});
        return route;
    }

    private static float dist(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1, dy = y2 - y1;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    // ================================================================
    // 图算法
    // ================================================================

    private int nearestNode(float x, float y) {
        int best = 0;
        float bestD2 = Float.MAX_VALUE;
        for (int i = 0; i < nodeX.length; i++) {
            float dx = nodeX[i] - x;
            float dy = nodeY[i] - y;
            float d2 = dx * dx + dy * dy;
            if (d2 < bestD2) {
                bestD2 = d2;
                best = i;
            }
        }
        return best;
    }

    /** 指定分量内最近节点 (数据洞兜底重吸附用)。 */
    private int nearestNodeInComponent(float x, float y, int comp) {
        int best = -1;
        float bestD2 = Float.MAX_VALUE;
        for (int i = 0; i < nodeX.length; i++) {
            if (compOf[i] != comp) continue;
            float dx = nodeX[i] - x;
            float dy = nodeY[i] - y;
            float d2 = dx * dx + dy * dy;
            if (d2 < bestD2) {
                bestD2 = d2;
                best = i;
            }
        }
        return best < 0 ? nearestNode(x, y) : best;
    }

    // ---------------- 连通分量 (懒建, 数据洞兜底用) ----------------

    private int[] compOf;
    private int bigCompId;

    // ---------------- 边列表 (懒建, 边投影吸附用) ----------------

    private int[] edgeA;
    private int[] edgeB;

    /**
     * 最近路段投影: 返回 {节点a, 节点b, 投影x, 投影y} (a<b 为邻接边) 或 null。
     * compId >= 0 时只在指定连通分量内选边 (数据洞兜底重吸附用); -1 = 不限。
     * 线性扫全边 (~10k 段, 每次锚定一次, 亚毫秒级)。
     */
    private float[] nearestEdgeProj(float x, float y, int compId) {
        if (edgeA == null) buildEdgeList();
        if (compId >= 0 && compOf == null) biggestComponent();
        int bestE = -1;
        float bestT = 0;
        float bestD2 = Float.MAX_VALUE;
        for (int e = 0; e < edgeA.length; e++) {
            int a = edgeA[e], b = edgeB[e];
            if (compId >= 0 && compOf[a] != compId) continue;
            float ax = nodeX[a], ay = nodeY[a];
            float bx = nodeX[b], by = nodeY[b];
            float dx = bx - ax, dy = by - ay;
            float l2 = dx * dx + dy * dy;
            float t = l2 > 0.0f ? ((x - ax) * dx + (y - ay) * dy) / l2 : 0.0f;
            t = t < 0.0f ? 0.0f : (t > 1.0f ? 1.0f : t);
            float px = ax + dx * t, py = ay + dy * t;
            float ex = x - px, ey = y - py;
            float d2 = ex * ex + ey * ey;
            if (d2 < bestD2) {
                bestD2 = d2;
                bestE = e;
                bestT = t;
            }
        }
        if (bestE < 0) return null;
        int a = edgeA[bestE], b = edgeB[bestE];
        float px = nodeX[a] + (nodeX[b] - nodeX[a]) * bestT;
        float py = nodeY[a] + (nodeY[b] - nodeY[a]) * bestT;
        return new float[]{a, b, px, py};
    }

    private void buildEdgeList() {
        int n = nodeX.length;
        int count = adjNode.length / 2;
        edgeA = new int[count];
        edgeB = new int[count];
        int at = 0;
        for (int u = 0; u < n; u++) {
            for (int e = adjStart[u]; e < adjStart[u + 1]; e++) {
                int v = adjNode[e];
                if (u < v) {
                    edgeA[at] = u;
                    edgeB[at] = v;
                    at++;
                }
            }
        }
    }

    /** 最大连通分量 id (首次调用时 BFS 建全图分量)。 */
    private int biggestComponent() {
        if (compOf != null) return bigCompId;
        int n = nodeX.length;
        compOf = new int[n];
        java.util.Arrays.fill(compOf, -1);
        int[] sizes = new int[n];
        int comps = 0;
        java.util.ArrayDeque<Integer> queue = new java.util.ArrayDeque<Integer>();
        for (int i = 0; i < n; i++) {
            if (compOf[i] >= 0) continue;
            compOf[i] = comps;
            queue.add(i);
            int sz = 0;
            while (!queue.isEmpty()) {
                int u = queue.poll();
                sz++;
                for (int e = adjStart[u]; e < adjStart[u + 1]; e++) {
                    int v = adjNode[e];
                    if (compOf[v] < 0) {
                        compOf[v] = comps;
                        queue.add(v);
                    }
                }
            }
            sizes[comps] = sz;
            comps++;
        }
        for (int c = 1; c < comps; c++) {
            if (sizes[c] > sizes[bigCompId]) bigCompId = c;
        }
        return bigCompId;
    }

    /** 多源 Dijkstra 最短路 (节点数 ~6k, 二叉堆足够)。返回 {dist[], prev[]};
     *  不可达节点 dist=+INF (必须 POSITIVE_INFINITY, 不能 MAX_VALUE —
     *  isInfinite 检查区分不了 MAX_VALUE, 不可达端会重建出垃圾路线)。
     *  prev[源]=-1。 */
    private Object[] dijkstraMulti(int[] starts, float[] initCost, boolean allowPortals) {
        int n = nodeX.length;
        float[] dist = new float[n];
        int[] prev = new int[n];
        boolean[] done = new boolean[n];
        for (int i = 0; i < n; i++) {
            dist[i] = Float.POSITIVE_INFINITY;
            prev[i] = -1;
        }
        PriorityQueue<Long> pq = new PriorityQueue<Long>();
        for (int s = 0; s < starts.length; s++) {
            if (initCost[s] < dist[starts[s]]) {
                dist[starts[s]] = initCost[s];
                pq.add(pack(starts[s], initCost[s]));
            }
        }
        while (!pq.isEmpty()) {
            long top = pq.poll();
            int u = (int) (long) top;
            if (done[u]) continue;
            done[u] = true;
            for (int e = adjStart[u]; e < adjStart[u + 1]; e++) {
                if (!allowPortals && adjIsPortal != null && adjIsPortal[e]) continue;
                int v = adjNode[e];
                float nd = dist[u] + adjWeight[e];
                if (nd < dist[v]) {
                    dist[v] = nd;
                    prev[v] = u;
                    pq.add(pack(v, nd));
                }
            }
        }
        return new Object[]{dist, prev};
    }

    private static long pack(int node, float weight) {
        return ((long) Float.floatToIntBits(weight) << 32) | (node & 0xFFFFFFFFL);
    }

    // ================================================================
    // 解析 / 平面化 / 建图
    // ================================================================

    /**
     * 建图入口: worldMap=false = streets.xml (现役); worldMap=true = C2 worldmap
     * 道路图层 (WorldMapRoads 提取的中线, 焊桥半径放宽到 8)。
     */
    private boolean load(boolean worldMap) throws Exception {
        long t0 = System.currentTimeMillis();
        ArrayList<float[]> polylines = new ArrayList<float[]>();
        if (worldMap) {
            this.weldR = 8.0f;
            this.bridgeGaps = true;
            this.polyWidth = null;
            polylines.addAll(WorldMapRoads.loadPolylines());
            if (polylines.isEmpty()) {
                Logger.printLog("[AutoDrive] worldmap network: no polylines");
                return false;
            }
            this.areaRings = WorldMapRoads.loadAreaRings();
            this.surfaceFeats = WorldMapRoads.loadSurfaceFeatures();
            // 脱路面裁剪 (B 轮三轮修复): 提取期"长轴串线"在大多边形上会产生横切
            // 凹区的弦 (实测最长 105 格, 其中 57% 不在任何路面多边形内) — 这类弦既
            // 是"导航线横穿野外"的直接来源, 又会让规划绕远 (实测 A 场景 1243 格中
            // 349 格在野地)。按"中线必须落在路面上"裁剪: 连续脱路面 ≥ 6 格的段切掉,
            // 保留两侧在路面上的部分 (RDP 抽稀回同容差)。
            int beforeCut = polylines.size();
            polylines = cutOffRoad(polylines);
            Logger.printLog("[AutoDrive] centerline surface cut: " + beforeCut + " -> "
                    + polylines.size() + " polylines");
            int welds = 0;
            try {
                ArrayList<float[]> weldSegs = buildAreaWelds(polylines);
                weldSegs.addAll(buildSurfaceWelds(polylines));
                welds = weldSegs.size();
                if (welds > 0) {
                    polylines = new ArrayList<float[]>(polylines);
                    polylines.addAll(weldSegs);
                }
            } catch (Throwable t) {
                // 落面焊接失败只损失连通性增强, 不能拖垮建图 (portal 仍是兜底)
                Logger.printLog("[AutoDrive] area weld failed: " + t);
            }
            buildGraph(polylines);
            Logger.printLog("[AutoDrive] worldmap network loaded: " + polylines.size()
                    + " polylines (" + welds + " area welds), " + nodeX.length + " nodes, "
                    + adjNode.length / 2 + " links, " + (System.currentTimeMillis() - t0) + "ms");
            return true;
        }
        ArrayList<MapFiles> maps = MapFiles.getCurrentMapFiles();
        if (maps == null || maps.isEmpty()) {
            Logger.printLog("[AutoDrive] street network: no map directories");
            return false;
        }
        // 折线收集: 每条街道一条 float[] (x0,y0,x1,y1,...), width 属性同步收集
        ArrayList<Float> widths = new ArrayList<Float>();
        for (MapFiles mf : maps) {
            java.io.File xml = new java.io.File(mf.mapDirectoryZfsPath, "streets.xml");
            if (!xml.isFile()) xml = new java.io.File(mf.mapDirectoryAbsolutePath, "streets.xml");
            if (!xml.isFile()) continue;
            int before = polylines.size();
            parseStreets(xml.getAbsolutePath(), polylines, widths);
            Logger.printLog("[AutoDrive] street network: " + mf.mapDirectoryName
                    + " (" + (polylines.size() - before) + " streets)");
        }
        if (polylines.isEmpty()) {
            Logger.printLog("[AutoDrive] street network: no streets parsed (dirs="
                    + maps.size() + ")");
            return false;
        }
        this.polyWidth = new float[widths.size()];
        for (int i = 0; i < widths.size(); i++) this.polyWidth[i] = widths.get(i);
        buildGraph(polylines);
        Logger.printLog("[AutoDrive] street network loaded: " + polylines.size()
                + " streets, " + nodeX.length + " nodes, " + adjNode.length / 2
                + " links, " + (System.currentTimeMillis() - t0) + "ms");
        return true;
    }

    /** 解析一个目录的 streets.xml → 折线列表 (width 属性同步收集, 缺失/坏值记 0
     *  = 该街不参与宽度外延)。 */
    private void parseStreets(String path, ArrayList<float[]> polylines,
            ArrayList<Float> widths) throws Exception {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        // 必须 parse(File): parse(String) 把路径当 URI 解析, Windows 路径里的
        // 空格/逗号 ("Muldraugh, KY") 会抛异常 → 路网加载失败 → 全部退化直线
        // (2026-09-07 实测回归根因; 离线校验工具用 File 所以没暴露)
        Document doc = f.newDocumentBuilder().parse(new java.io.File(path));
        NodeList streetNodes = doc.getElementsByTagName("street");
        for (int s = 0; s < streetNodes.getLength(); s++) {
            Element street = (Element) streetNodes.item(s);
            NodeList pts = street.getElementsByTagName("point");
            int count = pts.getLength();
            if (count < 2) continue;
            float[] line = new float[count * 2];
            for (int p = 0; p < count; p++) {
                Element pt = (Element) pts.item(p);
                line[p * 2] = Float.parseFloat(pt.getAttribute("x"));
                line[p * 2 + 1] = Float.parseFloat(pt.getAttribute("y"));
            }
            polylines.add(line);
            float w = 0f;
            try {
                w = Float.parseFloat(street.getAttribute("width"));
            } catch (NumberFormatException ignore) {
            }
            widths.add(Math.max(0f, w));
        }
    }

    // ---------------- 平面化 + 建图 ----------------

    /** 网格桶: 段 bbox 覆盖的桶 → 段 id 列表。 */
    private final HashMap<Long, ArrayList<Integer>> segBuckets = new HashMap<Long, ArrayList<Integer>>();
    /** 焊桥边收集: {端点x, 端点y, 投影点x, 投影点y}。 */
    private final ArrayList<float[]> bridges = new ArrayList<float[]>();
    // 展开段坐标缓存 (桶查询用)
    private float[] segX1;
    private float[] segY1;
    private float[] segX2;
    private float[] segY2;

    /**
     * 平面化 + 建图: ①段-段相交对 (同折线跳过) 求交点并沿交点切开两段;
     * ②节点 = 全部折线点/交点 (0.5 格量化去重); ③折线内相邻节点连边。
     */
    private void buildGraph(ArrayList<float[]> polylines) {
        // 重复折线去重 (几何键): mod 常带全州数据的翻译副本 (vanilla + mod 同几何
        // 两份)。不去重时焊桥端点会先命中自己的副本 (投影距离 0 的无效自焊) 并
        // break, 真正的路口焊接被吞 → 连通性随机崩碎 (2026-09-07 实测第三根因)。
        HashSet<String> seenKeys = new HashSet<String>();
        ArrayList<float[]> uniq = new ArrayList<float[]>(polylines.size());
        // width 数组与去重同步重排 (保持 polyWidth[i] ↔ 去重后折线 i 对齐)
        float[] widthSrc = (polyWidth != null && polyWidth.length == polylines.size())
                ? polyWidth : null;
        ArrayList<Float> uniqW = widthSrc != null ? new ArrayList<Float>(polylines.size()) : null;
        int srcIdx = 0;
        for (float[] pl : polylines) {
            if (seenKeys.add(polylineKey(pl))) {
                uniq.add(pl);
                if (uniqW != null) uniqW.add(widthSrc[srcIdx]);
            }
            srcIdx++;
        }
        polylines = uniq;
        if (uniqW != null) {
            polyWidth = new float[uniqW.size()];
            for (int i = 0; i < uniqW.size(); i++) polyWidth[i] = uniqW.get(i);
        } else {
            polyWidth = null;
        }
        int segCount = 0;
        for (float[] pl : polylines) segCount += pl.length / 2 - 1;
        segX1 = new float[segCount];
        segY1 = new float[segCount];
        segX2 = new float[segCount];
        segY2 = new float[segCount];
        int[] segStreet = new int[segCount];
        // 每段的交点收集: segId → {t, x, y} 列表
        ArrayList<ArrayList<float[]>> segSplits = new ArrayList<ArrayList<float[]>>(segCount);
        int idx = 0;
        for (int s = 0; s < polylines.size(); s++) {
            float[] pl = polylines.get(s);
            for (int p = 0; p < pl.length / 2 - 1; p++, idx++) {
                segX1[idx] = pl[p * 2];
                segY1[idx] = pl[p * 2 + 1];
                segX2[idx] = pl[p * 2 + 2];
                segY2[idx] = pl[p * 2 + 3];
                segStreet[idx] = s;
                segSplits.add(new ArrayList<float[]>(2));
                bucketSegment(idx);
            }
        }
        // 段-段相交 (网格桶候选, 同折线跳过, bbox 预剔)
        for (int a = 0; a < segCount; a++) {
            for (int b : candidateSegments(a)) {
                if (b <= a) continue;
                if (segStreet[a] == segStreet[b]) continue;
                if (Math.max(segX1[a], segX2[a]) < Math.min(segX1[b], segX2[b])) continue;
                if (Math.max(segY1[a], segY2[a]) < Math.min(segY1[b], segY2[b])) continue;
                float[] hit = segmentHit(a, b);
                if (hit == null) continue;
                segSplits.get(a).add(new float[]{hit[2], hit[0], hit[1]});
                segSplits.get(b).add(new float[]{hit[3], hit[0], hit[1]});
            }
        }
        // 端点吸附焊桥 (T 型路口救星): streets 折线在别的街段中间悬空 4~6 格
        // (T 型/斜接路口不做点共享; 高速公路接口悬空的常是中间折点, 不只是首尾),
        // 点对点聚类救不了中段悬空 — 把每个折点投影到别的折线的段上 (距离 ≤
        // WELD_R), 在投影点切开那条折线, 并焊一条短桥边 → 路网连通。一个折点
        // 可焊多个命中段 (路口常有多条街交汇), 不可只焊第一个。同街折线跳过。
        // 宽度感知焊桥 (2026-09-12 巴顿街修复): 端点悬空若停在目标路**边缘**,
        // 缺口 ≈ 目标路半宽 (Patton St 端点到 Fiddler's Trail 中线 7.6 ≈ 15/2),
        // 固定半径 6 焊不上 → 整条街集群成孤岛被误判"不可达"。外延半径 = 两街
        // width 半宽和 + 0.5 (上限 WELD_EXT_MAX); 外延档必须近垂直 (真 T 型路口),
        // 平行悬空 (相邻平行街/匝道并线) 不焊防误连。基础 weldR 内行为不变。
        float weldQ = weldR;
        boolean useW = polyWidth != null && polyWidth.length == polylines.size();
        if (useW) {
            float mw = 0f;
            for (float w : polyWidth) if (w > mw) mw = w;
            weldQ = Math.max(weldR, Math.min(WELD_EXT_MAX, mw + 0.5f));
        }
        int welds = 0;
        for (int a = 0; a < segCount; a++) {
            float[][] ends = {{segX1[a], segY1[a]}, {segX2[a], segY2[a]}};
            for (float[] ep : ends) {
                // 候选查询必须以**端点**为中心 (weldR 扩桶): 旧实现用段 a 自身
                // bbox 的桶, T 字路口横路恰在端点桶外时 (如 y=11197 端点对
                // y=11204.5 横路, 距离 7.5<8) 根本取不到 → 漏焊 → 图碎裂
                // (2026-09-11 C2 实测: 6400 格直线路线)。
                for (int b : candidateSegmentsAround(ep[0], ep[1], weldQ)) {
                    if (b == a) continue;
                    if (segStreet[a] == segStreet[b]) continue;
                    float r = weldR;
                    if (useW) {
                        float ext = (polyWidth[segStreet[a]] + polyWidth[segStreet[b]]) * 0.5f + 0.5f;
                        if (ext > r) r = Math.min(WELD_EXT_MAX, ext);
                    }
                    float[] pr = projectPoint(ep[0], ep[1], b, r);
                    if (pr == null) continue;
                    if (r > weldR) {
                        // 外延档角度闸: 端点→投影点连线 与 目标段方向近垂直才焊
                        // (T 型路口垂直撞上横路; 平行街顺路边悬空 cos≈1 拒绝)
                        float cdx = pr[1] - ep[0], cdy = pr[2] - ep[1];
                        float clen = (float) Math.sqrt(cdx * cdx + cdy * cdy);
                        if (clen >= 0.5f) {
                            float tdx = segX2[b] - segX1[b], tdy = segY2[b] - segY1[b];
                            float tl = (float) Math.sqrt(tdx * tdx + tdy * tdy);
                            float cos = tl < 1e-3f ? 0f
                                    : Math.abs((cdx * tdx + cdy * tdy) / (clen * tl));
                            if (cos >= 0.85f) continue;
                        }
                    }
                    segSplits.get(b).add(pr);
                    // 桥边: 折点节点 ↔ 投影点节点 (在下方节点收集阶段追加)
                    bridges.add(new float[]{ep[0], ep[1], pr[1], pr[2]});
                    welds++;
                }
            }
        }
        // 节点收集 + 折线内连边 (交点按 t 排序插入)
        ArrayList<float[]> coords = new ArrayList<float[]>();
        HashMap<Long, Integer> nodeIds = new HashMap<Long, Integer>();
        ArrayList<Integer> edgeFrom = new ArrayList<Integer>();
        ArrayList<Integer> edgeTo = new ArrayList<Integer>();
        // 边权倍率 (与 edgeFrom 平行): 真实边 1.0, 缺口 portal = WM_GAP_PENALTY
        ArrayList<Float> edgeScale = new ArrayList<Float>();
        idx = 0;
        for (int s = 0; s < polylines.size(); s++) {
            float[] pl = polylines.get(s);
            int prevEnd = -1;   // 上一段终点 (段间衔接边)
            for (int p = 0; p < pl.length / 2 - 1; p++, idx++) {
                int n0 = node(segX1[idx], segY1[idx], coords, nodeIds);
                if (prevEnd >= 0 && prevEnd != n0) {
                    edgeFrom.add(prevEnd); edgeTo.add(n0); edgeScale.add(1.0f);
                }
                ArrayList<float[]> splits = segSplits.get(idx);
                if (!splits.isEmpty()) {
                    splits.sort((u, v) -> Float.compare(u[0], v[0]));
                    for (float[] sp : splits) {
                        int n1 = node(sp[1], sp[2], coords, nodeIds);
                        if (n1 != n0) {
                            edgeFrom.add(n0); edgeTo.add(n1); edgeScale.add(1.0f); n0 = n1;
                        }
                    }
                }
                int nEnd = node(segX2[idx], segY2[idx], coords, nodeIds);
                if (nEnd != n0) { edgeFrom.add(n0); edgeTo.add(nEnd); edgeScale.add(1.0f); }
                prevEnd = nEnd;
            }
        }
        // 焊桥边追加 (端点悬空衔接)
        for (float[] br : bridges) {
            int na = node(br[0], br[1], coords, nodeIds);
            int nb = node(br[2], br[3], coords, nodeIds);
            if (na != nb) { edgeFrom.add(na); edgeTo.add(nb); edgeScale.add(1.0f); }
        }
        // worldmap: 分量缺口补带惩罚 portal 边 (见 WM_GAP_MAX); streets 不动
        if (bridgeGaps) {
            connectComponentPortals(coords, edgeFrom, edgeTo, edgeScale);
        }
        finishAdjacency(coords, edgeFrom, edgeTo, edgeScale, polylines.size());
    }

    // ================================================================
    // 路面判定 + 脱路面裁剪 (B 轮三轮修复 2026-09-11)
    // ================================================================
    /** 路面桶边长 (格): 多边形 bbox 索引粒度。 */
    private static final int SURF_BUCKET = 64;
    /** 裁剪阈值 (格): 连续脱路面短于此值的抖动视为量化噪声, 不裁。 */
    private static final float CUT_MIN_OFF = 6.0f;
    /** 裁剪采样步长 (格) 与抽稀容差 (与提取期 RDP_EPS 一致)。 */
    private static final float CUT_STEP = 2.0f;
    private static final float CUT_RDP_EPS = 2.0f;
    /** 保留片段的最短长度 (格)。 */
    private static final float CUT_MIN_PIECE = 6.0f;

    /** 多边形 bbox 桶索引 (懒建)。 */
    private void ensureSurfIndex() {
        if (surfIdx != null || surfaceFeats == null) return;
        surfIdx = new HashMap<Long, ArrayList<Integer>>();
        for (int f = 0; f < surfaceFeats.size(); f++) {
            float[][] fs = surfaceFeats.get(f);
            float minx = Float.MAX_VALUE, maxx = -Float.MAX_VALUE;
            float miny = Float.MAX_VALUE, maxy = -Float.MAX_VALUE;
            for (float[] r : fs) {
                for (int k = 0; k + 1 < r.length; k += 2) {
                    if (r[k] < minx) minx = r[k];
                    if (r[k] > maxx) maxx = r[k];
                    if (r[k + 1] < miny) miny = r[k + 1];
                    if (r[k + 1] > maxy) maxy = r[k + 1];
                }
            }
            int bx0 = (int) Math.floor(minx / SURF_BUCKET), bx1 = (int) Math.floor(maxx / SURF_BUCKET);
            int by0 = (int) Math.floor(miny / SURF_BUCKET), by1 = (int) Math.floor(maxy / SURF_BUCKET);
            for (int by = by0; by <= by1; by++) {
                for (int bx = bx0; bx <= bx1; bx++) {
                    long key = (((long) bx) << 32) | (by & 0xFFFFFFFFL);
                    ArrayList<Integer> l = surfIdx.get(key);
                    if (l == null) {
                        l = new ArrayList<Integer>(4);
                        surfIdx.put(key, l);
                    }
                    l.add(f);
                }
            }
        }
    }

    /** 点是否落在某道路多边形内部 (even-odd 全环: 洞不算路面)。 */
    private boolean onSurface(float x, float y) {
        if (surfaceFeats == null || surfaceFeats.isEmpty()) return true;   // 无数据不裁
        ensureSurfIndex();
        long key = (((long) (int) Math.floor(x / SURF_BUCKET)) << 32)
                | ((int) Math.floor(y / SURF_BUCKET) & 0xFFFFFFFFL);
        ArrayList<Integer> list = surfIdx.get(key);
        if (list == null) return false;
        for (int i = 0; i < list.size(); i++) {
            float[][] fs = surfaceFeats.get(list.get(i));
            int cross = 0;
            for (int r = 0; r < fs.length; r++) {
                if (insideRing(x, y, fs[r])) cross++;
            }
            if ((cross & 1) == 1) return true;
        }
        return false;
    }

    /** 连接线是否全程在路面上 (每 step 格采样)。 */
    private boolean onSurfaceLine(float ax, float ay, float bx, float by, float step) {
        float d = dist(ax, ay, bx, by);
        int n = Math.max(1, (int) (d / step));
        for (int k = 0; k <= n; k++) {
            float t = (float) k / n;
            if (!onSurface(ax + (bx - ax) * t, ay + (by - ay) * t)) return false;
        }
        return true;
    }

    /**
     * 脱路面裁剪: 逐折线按 CUT_STEP 采样, 连续脱路面 ≥ CUT_MIN_OFF 的段切开,
     * 保留片段 (长度 ≥ CUT_MIN_PIECE) RDP 抽稀后输出。路面数据缺失时原样返回。
     */
    private ArrayList<float[]> cutOffRoad(ArrayList<float[]> in) {
        if (surfaceFeats == null || surfaceFeats.isEmpty()) return in;
        ensureSurfIndex();
        ArrayList<float[]> out = new ArrayList<float[]>(in.size());
        ArrayList<float[]> pts = new ArrayList<float[]>(64);
        for (float[] pl : in) {
            pts.clear();
            int vcount = pl.length / 2;
            if (vcount < 2) continue;
            for (int i = 0; i + 3 < pl.length; i += 2) {
                float ax = pl[i], ay = pl[i + 1], bx = pl[i + 2], by = pl[i + 3];
                float d = dist(ax, ay, bx, by);
                int n = Math.max(1, (int) (d / CUT_STEP));
                for (int k = 0; k < n; k++) {
                    float t = (float) k / n;
                    float px = ax + (bx - ax) * t, py = ay + (by - ay) * t;
                    pts.add(new float[]{px, py, onSurface(px, py) ? 1f : 0f});
                }
            }
            float lx = pl[pl.length - 2], ly = pl[pl.length - 1];
            pts.add(new float[]{lx, ly, onSurface(lx, ly) ? 1f : 0f});
            int start = 0;
            int i = 0;
            int size = pts.size();
            while (i < size) {
                if (pts.get(i)[2] == 1f) {
                    i++;
                    continue;
                }
                int j = i;
                while (j < size && pts.get(j)[2] == 0f) j++;
                float runLen = 0;
                for (int k = i + 1; k < j; k++) {
                    runLen += dist(pts.get(k - 1)[0], pts.get(k - 1)[1], pts.get(k)[0], pts.get(k)[1]);
                }
                if (runLen >= CUT_MIN_OFF) {
                    emitPiece(out, pts, start, i - 1);
                    start = j;
                }
                i = j;
            }
            emitPiece(out, pts, start, size - 1);
        }
        return out;
    }

    /** 裁剪片段输出 (RDP 抽稀, 过短丢弃)。 */
    private void emitPiece(ArrayList<float[]> out, ArrayList<float[]> pts, int from, int to) {
        if (to - from < 2) return;
        float len = 0;
        for (int k = from + 1; k <= to; k++) {
            len += dist(pts.get(k - 1)[0], pts.get(k - 1)[1], pts.get(k)[0], pts.get(k)[1]);
        }
        if (len < CUT_MIN_PIECE) return;
        int cnt = to - from + 1;
        float[] line = new float[cnt * 2];
        for (int k = 0; k < cnt; k++) {
            line[k * 2] = pts.get(from + k)[0];
            line[k * 2 + 1] = pts.get(from + k)[1];
        }
        float[] simp = rdpLine(line, CUT_RDP_EPS);
        if (simp != null && simp.length >= 4) out.add(simp);
    }

    /** Ramer-Douglas-Peucker 抽稀 (递归, 与提取期同容差)。 */
    private static float[] rdpLine(float[] p, float eps) {
        int n = p.length / 2;
        if (n < 3) return p;
        boolean[] keep = new boolean[n];
        keep[0] = true;
        keep[n - 1] = true;
        rdpRec(p, 0, n - 1, eps, keep);
        int cnt = 0;
        for (int i = 0; i < n; i++) {
            if (keep[i]) cnt++;
        }
        float[] out = new float[cnt * 2];
        int at = 0;
        for (int i = 0; i < n; i++) {
            if (keep[i]) {
                out[at * 2] = p[i * 2];
                out[at * 2 + 1] = p[i * 2 + 1];
                at++;
            }
        }
        return out;
    }

    private static void rdpRec(float[] p, int a, int b, float eps, boolean[] keep) {
        if (b <= a + 1) return;
        float ax = p[a * 2], ay = p[a * 2 + 1];
        float bx = p[b * 2], by = p[b * 2 + 1];
        float dx = bx - ax, dy = by - ay;
        float l2 = dx * dx + dy * dy;
        float maxD = -1;
        int idx = -1;
        for (int i = a + 1; i < b; i++) {
            float t = l2 > 0 ? ((p[i * 2] - ax) * dx + (p[i * 2 + 1] - ay) * dy) / l2 : 0;
            t = t < 0 ? 0 : (t > 1 ? 1 : t);
            float ex = p[i * 2] - (ax + dx * t), ey = p[i * 2 + 1] - (ay + dy * t);
            float d = (float) Math.sqrt(ex * ex + ey * ey);
            if (d > maxD) {
                maxD = d;
                idx = i;
            }
        }
        if (maxD > eps && idx > a && idx < b) {
            keep[idx] = true;
            rdpRec(p, a, idx, eps, keep);
            rdpRec(p, idx, b, eps, keep);
        }
    }

    // ================================================================
    // 落面焊接 (A1 修复 2026-09-11): "大块路面"中线代表不了整块面
    // ================================================================
    /** 端点距大块路面环边的容差 (格): 端点落在面内或贴着面边都算"在路上"。 */
    private static final float AREA_WELD_PAD = 4.0f;
    /** 焊到面内中线的距离上限 (格): 超过说明该端点与这块面无关, 交给 portal。 */
    private static final float AREA_WELD_MAX = 60.0f;
    /** 环桶边长 (格): 端点查询候选环的粒度。 */
    private static final int AREA_BUCKET = 128;

    /**
     * 端点落面焊接: 悬挂端点 (度 ≤ 1) 若落在"大块路面"外环内、或距环边 ≤
     * AREA_WELD_PAD, 则焊到该环中线上最近点 (合成 2 点折线, 权 = 真实距离)。
     *
     * 背景 (B 轮实测): 目的地园区的接入路段端点 (12427,10766) 落在一块 293×265 格的
     * primary 整块路面内部, 但该面被抽出的**单条中线**离端点 15~32 格 > weldR(8) →
     * 端点焊不上 → 园区成孤岛分量 → 只能靠 111 格 portal 跨野直线接入 (用户实测
     * "导航线横穿野外")。落面焊接把"端点在路面上"这一事实直接变成一条边。
     *
     * 实现: 先以无 portal 方式建一张临时图拿度数 (悬挂判定), 再对每个悬挂端点查
     * 环桶 → 点内/贴边判定 → 懒计算该环骨架 → 最近点。返回待追加的 2 点折线。
     */
    private ArrayList<float[]> buildAreaWelds(ArrayList<float[]> polylines) {
        ArrayList<float[]> welds = new ArrayList<float[]>();
        if (areaRings == null || areaRings.isEmpty()) return welds;
        long t0 = System.currentTimeMillis();
        RoadNetwork probe = new RoadNetwork();
        probe.weldR = weldR;
        probe.bridgeGaps = false;
        probe.buildGraph(polylines);
        float[] nx = probe.nodeX;
        float[] ny = probe.nodeY;
        int[] deg = new int[nx.length];
        for (int i = 0; i < nx.length; i++) {
            deg[i] = probe.adjStart[i + 1] - probe.adjStart[i];
        }
        // 环桶: 每个环按其 bbox 覆盖的全部桶登记
        HashMap<Long, ArrayList<Integer>> buckets = new HashMap<Long, ArrayList<Integer>>();
        for (int r = 0; r < areaRings.size(); r++) {
            float[] ring = areaRings.get(r);
            float minx = Float.MAX_VALUE, maxx = -Float.MAX_VALUE;
            float miny = Float.MAX_VALUE, maxy = -Float.MAX_VALUE;
            for (int i = 0; i + 1 < ring.length; i += 2) {
                if (ring[i] < minx) minx = ring[i];
                if (ring[i] > maxx) maxx = ring[i];
                if (ring[i + 1] < miny) miny = ring[i + 1];
                if (ring[i + 1] > maxy) maxy = ring[i + 1];
            }
            int bx0 = (int) Math.floor(minx / AREA_BUCKET), bx1 = (int) Math.floor(maxx / AREA_BUCKET);
            int by0 = (int) Math.floor(miny / AREA_BUCKET), by1 = (int) Math.floor(maxy / AREA_BUCKET);
            for (int by = by0; by <= by1; by++) {
                for (int bx = bx0; bx <= bx1; bx++) {
                    long key = (((long) bx) << 32) | (by & 0xFFFFFFFFL);
                    ArrayList<Integer> list = buckets.get(key);
                    if (list == null) {
                        list = new ArrayList<Integer>(4);
                        buckets.put(key, list);
                    }
                    list.add(r);
                }
            }
        }
        HashMap<Integer, ArrayList<float[]>> spineCache = new HashMap<Integer, ArrayList<float[]>>();
        int dangling = 0;
        for (int i = 0; i < nx.length; i++) {
            if (deg[i] > 1) continue;
            dangling++;
            float px = nx[i], py = ny[i];
            // 3×3 桶邻域: 端点在环 bbox 外 AREA_WELD_PAD 内时可能落在相邻桶
            // (桶边长 128 >> 容差 4), 只查单桶会漏焊。
            int bx = (int) Math.floor(px / AREA_BUCKET), by = (int) Math.floor(py / AREA_BUCKET);
            ArrayList<Integer> cand = null;
            for (int oy = -1; oy <= 1; oy++) {
                for (int ox = -1; ox <= 1; ox++) {
                    long key = (((long) (bx + ox)) << 32) | ((by + oy) & 0xFFFFFFFFL);
                    ArrayList<Integer> list = buckets.get(key);
                    if (list == null) continue;
                    if (cand == null) {
                        cand = new ArrayList<Integer>(list.size() * 2);
                    }
                    cand.addAll(list);
                }
            }
            if (cand == null) continue;
            float bestD = Float.MAX_VALUE;
            float bestX = 0, bestY = 0;
            for (int ci = 0; ci < cand.size(); ci++) {
                int r = cand.get(ci);
                float[] ring = areaRings.get(r);
                if (!insideRing(px, py, ring)
                        && distToRing(px, py, ring) > AREA_WELD_PAD) continue;
                ArrayList<float[]> sp = spineCache.get(r);
                if (sp == null) {
                    sp = new ArrayList<float[]>();
                    WorldMapRoads.spinesOfRing(ring, sp);
                    spineCache.put(r, sp);
                }
                for (int s = 0; s < sp.size(); s++) {
                    float[] pl = sp.get(s);
                    for (int k = 0; k + 3 < pl.length; k += 2) {
                        float qx = pl[k], qy = pl[k + 1];
                        float ex = pl[k + 2], ey = pl[k + 3];
                        float dx = ex - qx, dy = ey - qy;
                        float l2 = dx * dx + dy * dy;
                        float t = l2 > 0 ? ((px - qx) * dx + (py - qy) * dy) / l2 : 0.0f;
                        t = t < 0 ? 0 : (t > 1 ? 1 : t);
                        float hx = qx + dx * t, hy = qy + dy * t;
                        float d = dist(px, py, hx, hy);
                        if (d < bestD) {
                            bestD = d;
                            bestX = hx;
                            bestY = hy;
                        }
                    }
                }
            }
            if (bestD <= AREA_WELD_MAX && bestD > 0.2f) {
                welds.add(new float[]{px, py, bestX, bestY});
            }
        }
        Logger.printLog("[AutoDrive] area weld: dangling=" + dangling + " welds=" + welds.size()
                + " areas=" + areaRings.size() + " (" + (System.currentTimeMillis() - t0) + "ms)");
        return welds;
    }

    /** 路面焊接半径 (格): 端点 → 最近路段的搜索半径 (连接线必须全程在路面上)。 */
    private static final float SURF_WELD_R = 40.0f;

    /**
     * 路面同源焊接 (2026-09-12): 悬挂端点若能在 SURF_WELD_R 内找到"连接线全程在路面上"的
     * 最近路段, 就直接连边 —— 替代"加大 weldR"的做法 (后者会跨街区误焊)。
     *
     * 背景: 端点焊接半径只有 8 格, 而数据里同一片路面被切成多个多边形, 相邻多边形的
     * 中线端点常常差十几格 —— 既不满足 weldR, 落面焊接也只管"端点落在大块路面内"的情形,
     * 于是只能靠 portal (跨野虚拟边) 兜底, 实测 portal 9635 条且个别目的地被迫绕行。
     * 本步只补**路面上**的连接, 不产生跨野捷径 (连接线逐 3 格采样做路面判定)。
     */
    private ArrayList<float[]> buildSurfaceWelds(ArrayList<float[]> polylines) {
        ArrayList<float[]> welds = new ArrayList<float[]>();
        if (surfaceFeats == null || surfaceFeats.isEmpty()) return welds;
        ensureSurfIndex();
        long t0 = System.currentTimeMillis();
        final int cell = 32;
        HashMap<Long, ArrayList<Integer>> grid = new HashMap<Long, ArrayList<Integer>>();
        for (int p = 0; p < polylines.size(); p++) {
            float[] pl = polylines.get(p);
            for (int i = 0; i + 3 < pl.length; i += 2) {
                int x0 = (int) Math.floor(Math.min(pl[i], pl[i + 2]) / cell);
                int x1 = (int) Math.floor(Math.max(pl[i], pl[i + 2]) / cell);
                int y0 = (int) Math.floor(Math.min(pl[i + 1], pl[i + 3]) / cell);
                int y1 = (int) Math.floor(Math.max(pl[i + 1], pl[i + 3]) / cell);
                for (int gy = y0; gy <= y1; gy++) {
                    for (int gx = x0; gx <= x1; gx++) {
                        long key = (((long) gx) << 32) | (gy & 0xFFFFFFFFL);
                        ArrayList<Integer> l = grid.get(key);
                        if (l == null) {
                            l = new ArrayList<Integer>(4);
                            grid.put(key, l);
                        }
                        l.add(p);
                    }
                }
            }
        }
        int ends = 0;
        for (int p = 0; p < polylines.size(); p++) {
            float[] pl = polylines.get(p);
            if (pl.length < 4) continue;
            for (int end = 0; end < 2; end++) {
                float ex = end == 0 ? pl[0] : pl[pl.length - 2];
                float ey = end == 0 ? pl[1] : pl[pl.length - 1];
                ends++;
                int gx = (int) Math.floor(ex / cell), gy = (int) Math.floor(ey / cell);
                float bestD = Float.MAX_VALUE, bx = 0, by = 0;
                for (int oy = -1; oy <= 1; oy++) {
                    for (int ox = -1; ox <= 1; ox++) {
                        long key = (((long) (gx + ox)) << 32) | ((gy + oy) & 0xFFFFFFFFL);
                        ArrayList<Integer> cand = grid.get(key);
                        if (cand == null) continue;
                        for (int ci = 0; ci < cand.size(); ci++) {
                            int q = cand.get(ci);
                            if (q == p) continue;
                            float[] other = polylines.get(q);
                            for (int k = 0; k + 3 < other.length; k += 2) {
                                float px = other[k], py = other[k + 1];
                                float qx = other[k + 2], qy = other[k + 3];
                                float dx = qx - px, dy = qy - py;
                                float l2 = dx * dx + dy * dy;
                                float t = l2 > 0 ? ((ex - px) * dx + (ey - py) * dy) / l2 : 0;
                                t = t < 0 ? 0 : (t > 1 ? 1 : t);
                                float hx = px + dx * t, hy = py + dy * t;
                                float d = dist(ex, ey, hx, hy);
                                if (d < bestD) {
                                    bestD = d;
                                    bx = hx;
                                    by = hy;
                                }
                            }
                        }
                    }
                }
                if (bestD > 0.5f && bestD <= SURF_WELD_R && onSurfaceLine(ex, ey, bx, by, 3.0f)) {
                    welds.add(new float[]{ex, ey, bx, by});
                }
            }
        }
        Logger.printLog("[AutoDrive] surface weld: ends=" + ends + " welds=" + welds.size()
                + " (" + (System.currentTimeMillis() - t0) + "ms)");
        return welds;
    }

    /** 点在环内 (射线法)。 */
    private static boolean insideRing(float px, float py, float[] ring) {
        int n = ring.length / 2;
        boolean in = false;
        for (int i = 0, j = n - 1; i < n; j = i++) {
            float xi = ring[i * 2], yi = ring[i * 2 + 1];
            float xj = ring[j * 2], yj = ring[j * 2 + 1];
            if (((yi > py) != (yj > py)) && (px < (xj - xi) * (py - yi) / (yj - yi) + xi)) {
                in = !in;
            }
        }
        return in;
    }

    /** 点到环边最短距离 (格)。 */
    private static float distToRing(float px, float py, float[] ring) {
        int n = ring.length / 2;
        float best = Float.MAX_VALUE;
        for (int i = 0, j = n - 1; i < n; j = i++) {
            float ax = ring[j * 2], ay = ring[j * 2 + 1];
            float bx = ring[i * 2], by = ring[i * 2 + 1];
            float dx = bx - ax, dy = by - ay;
            float l2 = dx * dx + dy * dy;
            float t = l2 > 0 ? ((px - ax) * dx + (py - ay) * dy) / l2 : 0.0f;
            t = t < 0 ? 0 : (t > 1 ? 1 : t);
            float d = dist(px, py, ax + dx * t, ay + dy * t);
            if (d < best) best = d;
        }
        return best;
    }

    /**
     * 分量缺口 portal (worldmap 专用, 2026-09-11 A2): 数据洞只可能出现在"一条路
     * 的尽头" — 只从**端点节点** (度 ≤1) 出发, 找 WM_GAP_MAX 内其它分量的节点,
     * 每个相邻分量只连最近的一个 (NearestPerComponent), 边权 = 距离 × 惩罚倍率。
     * 旧全局 MST (任意节点对 + 全局最短) 选出的桥与本次路线无关, 实测把园区向西
     * 跨野桥接造成 1659 格绕行; 而任意节点对枚举在 5 万节点规模耗时 1.4s (实测)。
     * 端点枚举规模小一个量级, 且逻辑上只补"路的尽头"的洞。
     */
    private void connectComponentPortals(ArrayList<float[]> coords, ArrayList<Integer> edgeFrom,
            ArrayList<Integer> edgeTo, ArrayList<Float> edgeScale) {
        int n = coords.size();
        if (n < 2) return;
        int[] parent = new int[n];
        for (int i = 0; i < n; i++) parent[i] = i;
        for (int e = 0; e < edgeFrom.size(); e++) {
            ufUnion(parent, edgeFrom.get(e), edgeTo.get(e));
        }
        // 端点节点 (度 ≤1)
        int[] deg = new int[n];
        for (int e = 0; e < edgeFrom.size(); e++) {
            deg[edgeFrom.get(e)]++;
            deg[edgeTo.get(e)]++;
        }
        // 空间桶 (半径 WM_GAP_MAX) 存全部节点
        HashMap<Long, ArrayList<Integer>> grid = new HashMap<Long, ArrayList<Integer>>();
        for (int i = 0; i < n; i++) {
            long key = (((long) (int) Math.floor(coords.get(i)[0] / WM_GAP_MAX)) << 32)
                    | ((int) Math.floor(coords.get(i)[1] / WM_GAP_MAX) & 0xFFFFFFFFL);
            ArrayList<Integer> list = grid.get(key);
            if (list == null) {
                list = new ArrayList<Integer>(4);
                grid.put(key, list);
            }
            list.add(i);
        }
        // 每个端点: 最近的其他分量各连一条 (同一分量只连最近的节点)
        int added = 0;
        int skipped = 0;
        for (int i = 0; i < n; i++) {
            if (deg[i] > 1) continue;
            float x = coords.get(i)[0];
            float y = coords.get(i)[1];
            int gx = (int) Math.floor(x / WM_GAP_MAX);
            int gy = (int) Math.floor(y / WM_GAP_MAX);
            int ci = ufFind(parent, i);
            HashMap<Integer, float[]> best = new HashMap<Integer, float[]>();
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    long key = (((long) (gx + dx)) << 32) | ((gy + dy) & 0xFFFFFFFFL);
                    ArrayList<Integer> list = grid.get(key);
                    if (list == null) continue;
                    for (int j : list) {
                        if (j == i) continue;
                        int cj = ufFind(parent, j);
                        if (cj == ci) continue;
                        float d = dist(x, y, coords.get(j)[0], coords.get(j)[1]);
                        if (d > WM_GAP_MAX) continue;
                        float[] cur = best.get(cj);
                        if (cur == null || d < cur[0]) best.put(cj, new float[]{d, j});
                    }
                }
            }
            for (float[] hit : best.values()) {
                int j = (int) hit[1];
                // A 规则 (2026-09-12 用户拍板): portal 也**必须落在路面上** —— 跨野直线一律
                // 不补。数据洞导致的"无路可达"改由规划侧退到最近可达道路点 + UI 提示
                // (findRouteWorldMap 最大分量兜底 + UI_DrivePanel_MsgRoadEnd): 宁可停在道路
                // 尽头, 也不画穿野线。实测目的地 (12757,11344) 前的 74/96 格跨野直线即此来源
                // (两段分别 24/25、30/33 采样落在野地)。
                if (!onSurfaceLine(x, y, coords.get(j)[0], coords.get(j)[1], 4.0f)) {
                    skipped++;
                    continue;
                }
                edgeFrom.add(i);
                edgeTo.add(j);
                edgeScale.add(WM_GAP_PENALTY);
                added++;
            }
        }
        if (added > 0 || skipped > 0) {
            Logger.printLog("[AutoDrive] worldmap gap portals: " + added
                    + " (脱离路面被拒: " + skipped + ")");
        }
    }

    private static int ufFind(int[] parent, int x) {
        while (parent[x] != x) {
            parent[x] = parent[parent[x]];
            x = parent[x];
        }
        return x;
    }

    private static void ufUnion(int[] parent, int a, int b) {
        int ra = ufFind(parent, a);
        int rb = ufFind(parent, b);
        if (ra != rb) parent[ra] = rb;
    }

    /** 折线几何键 (端点半格量化, 首尾无序): 相同几何 (含反向副本) 去重用。 */
    private static String polylineKey(float[] pl) {
        long a = (Math.round(pl[0] * 2f) << 32) | (Math.round(pl[1] * 2f) & 0xFFFFFFFFL);
        long b = (Math.round(pl[pl.length - 2] * 2f) << 32) | (Math.round(pl[pl.length - 1] * 2f) & 0xFFFFFFFFL);
        long lo = Math.min(a, b), hi = Math.max(a, b);
        return lo + "|" + hi + "|" + (pl.length / 2);
    }

    private void bucketSegment(int id) {
        int x0 = (int) Math.floor(Math.min(segX1[id], segX2[id]) / HASH_CELL);
        int xe = (int) Math.floor(Math.max(segX1[id], segX2[id]) / HASH_CELL);
        int y0 = (int) Math.floor(Math.min(segY1[id], segY2[id]) / HASH_CELL);
        int ye = (int) Math.floor(Math.max(segY1[id], segY2[id]) / HASH_CELL);
        for (int cy = y0; cy <= ye; cy++) {
            for (int cx = x0; cx <= xe; cx++) {
                Long key = (((long) cx) << 32) | (cy & 0xFFFFFFFFL);
                ArrayList<Integer> list = segBuckets.get(key);
                if (list == null) {
                    list = new ArrayList<Integer>(4);
                    segBuckets.put(key, list);
                }
                list.add(id);
            }
        }
    }

    /** 端点焊桥候选: (x,y) 周围 pad 格内桶的全部段 (weldR 扩桶)。 */
    private ArrayList<Integer> candidateSegmentsAround(float x, float y, float pad) {
        ArrayList<Integer> out = new ArrayList<Integer>();
        HashSet<Integer> seen = new HashSet<Integer>();
        int x0 = (int) Math.floor((x - pad) / HASH_CELL);
        int xe = (int) Math.floor((x + pad) / HASH_CELL);
        int y0 = (int) Math.floor((y - pad) / HASH_CELL);
        int ye = (int) Math.floor((y + pad) / HASH_CELL);
        for (int cy = y0; cy <= ye; cy++) {
            for (int cx = x0; cx <= xe; cx++) {
                Long key = (((long) cx) << 32) | (cy & 0xFFFFFFFFL);
                ArrayList<Integer> list = segBuckets.get(key);
                if (list == null) continue;
                for (int other : list) {
                    if (seen.add(other)) out.add(other);
                }
            }
        }
        return out;
    }

    private ArrayList<Integer> candidateSegments(int id) {
        ArrayList<Integer> out = new ArrayList<Integer>();
        HashSet<Integer> seen = new HashSet<Integer>();
        int x0 = (int) Math.floor(Math.min(segX1[id], segX2[id]) / HASH_CELL);
        int xe = (int) Math.floor(Math.max(segX1[id], segX2[id]) / HASH_CELL);
        int y0 = (int) Math.floor(Math.min(segY1[id], segY2[id]) / HASH_CELL);
        int ye = (int) Math.floor(Math.max(segY1[id], segY2[id]) / HASH_CELL);
        for (int cy = y0; cy <= ye; cy++) {
            for (int cx = x0; cx <= xe; cx++) {
                Long key = (((long) cx) << 32) | (cy & 0xFFFFFFFFL);
                ArrayList<Integer> list = segBuckets.get(key);
                if (list == null) continue;
                for (int other : list) {
                    if (seen.add(other)) out.add(other);
                }
            }
        }
        return out;
    }

    /**
     * 段-段交点 (含端点接触): 返回 {x, y, tA, tB} 或 null (平行/不相交)。
     * 端点重合也算 (量化后自然合并为同一节点, 不产生分裂)。
     */
    private float[] segmentHit(int a, int b) {
        float dxA = segX2[a] - segX1[a];
        float dyA = segY2[a] - segY1[a];
        float dxB = segX2[b] - segX1[b];
        float dyB = segY2[b] - segY1[b];
        float denom = dxA * dyB - dyA * dxB;
        if (Math.abs(denom) < 1e-6f) return null;   // 平行 (共线重叠罕见, 忽略)
        float t = ((segX1[b] - segX1[a]) * dyB - (segY1[b] - segY1[a]) * dxB) / denom;
        float u = ((segX1[b] - segX1[a]) * dyA - (segY1[b] - segY1[a]) * dxA) / denom;
        if (t < 0f || t > 1f || u < 0f || u > 1f) return null;
        return new float[]{segX1[a] + dxA * t, segY1[a] + dyA * t, t, u};
    }

    /**
     * 点到段 b 的投影 (WELD_R 容差): 返回 {t, x, y} (t 为段上参数) 或 null
     * (投影距离超限/段长 0)。
     */
    private float[] projectPoint(float px, float py, int b) {
        return projectPoint(px, py, b, weldR);
    }

    /** 点到段 b 的投影 (距离 > maxR 返回 null); 返回 {t, x, y}。 */
    private float[] projectPoint(float px, float py, int b, float maxR) {
        float dx = segX2[b] - segX1[b];
        float dy = segY2[b] - segY1[b];
        float l2 = dx * dx + dy * dy;
        if (l2 < 1e-6f) return null;
        float t = ((px - segX1[b]) * dx + (py - segY1[b]) * dy) / l2;
        t = t < 0f ? 0f : (t > 1f ? 1f : t);
        float qx = segX1[b] + dx * t;
        float qy = segY1[b] + dy * t;
        float ex = px - qx;
        float ey = py - qy;
        if (ex * ex + ey * ey > maxR * maxR) return null;
        return new float[]{t, qx, qy};
    }

    /** 节点获取/创建 (0.5 格量化去重; 交叉口共享点/交点自动合并)。 */
    private static int node(float x, float y, ArrayList<float[]> coords, HashMap<Long, Integer> nodeIds) {
        long kx = (long) Math.round(x / SNAP);
        long ky = (long) Math.round(y / SNAP);
        long key = (kx << 32) | (ky & 0xFFFFFFFFL);
        Integer existing = nodeIds.get(key);
        if (existing != null) return existing;
        int id = coords.size();
        coords.add(new float[]{kx * SNAP, ky * SNAP});
        nodeIds.put(key, id);
        return id;
    }

    private void finishAdjacency(ArrayList<float[]> coords, ArrayList<Integer> edgeFrom,
                                 ArrayList<Integer> edgeTo, ArrayList<Float> edgeScale,
                                 int streetCount) {
        int n = coords.size();
        nodeX = new float[n];
        nodeY = new float[n];
        for (int i = 0; i < n; i++) {
            nodeX[i] = coords.get(i)[0];
            nodeY[i] = coords.get(i)[1];
        }
        // 无向图: 每条边正反两个方向各存一条邻接 (有向存法会让逆向路线全部
        // 不可达 → 退化直线, 2026-09-07 实测第二根因)
        int m = edgeFrom.size();
        adjStart = new int[n + 1];
        adjNode = new int[m * 2];
        adjWeight = new float[m * 2];
        adjIsPortal = new boolean[m * 2];
        int[] cursor = new int[n + 1];
        for (int e = 0; e < m; e++) {
            adjStart[edgeFrom.get(e) + 1]++;
            adjStart[edgeTo.get(e) + 1]++;
        }
        for (int i = 0; i < n; i++) {
            adjStart[i + 1] += adjStart[i];
            cursor[i] = adjStart[i];
        }
        for (int e = 0; e < m; e++) {
            int a = edgeFrom.get(e);
            int b = edgeTo.get(e);
            float dx = nodeX[b] - nodeX[a];
            float dy = nodeY[b] - nodeY[a];
            float w = (float) Math.sqrt(dx * dx + dy * dy)
                    * (e < edgeScale.size() ? edgeScale.get(e) : 1.0f);
            // portal 边 = 缺口虚拟边 (edgeScale > 1); 真实道路边 scale = 1.0
            boolean portal = e < edgeScale.size() && edgeScale.get(e) > 1.0f;
            int at = cursor[a]++;
            adjNode[at] = b;
            adjWeight[at] = w;
            adjIsPortal[at] = portal;
            at = cursor[b]++;
            adjNode[at] = a;
            adjWeight[at] = w;
            adjIsPortal[at] = portal;
        }
    }
}
