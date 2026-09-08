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
 * 精确终点 (末段短驳, 穿墙保证可达)。路网不可用/断裂返回 null, 调用方
 * (AutoDriveController) 退化直线直奔 (routeKind="direct")。
 *
 * 目录优先级: MapFiles.getCurrentMapFiles() (mods 在前) 逐目录合并; 自制地图
 * 自带 streets.xml 同样生效; 缺失目录跳过, 全部缺失 → direct。
 */
package EtherHack.drive;

import EtherHack.utils.Logger;
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

    private float[] nodeX;
    private float[] nodeY;
    private int[] adjStart;
    private int[] adjNode;
    private float[] adjWeight;

    private static RoadNetwork instance;

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
            if (!net.load()) return false;
            instance = net;
            return true;
        } catch (Throwable t) {
            Logger.error("[AutoDrive] street network load failed", t);
            return false;
        }
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
     * West Point 接口 96 格), 缺口由首末段直线短驳覆盖。返回 null = 路网不可用。
     */
    public static ArrayList<float[]> findRoute(float sx, float sy, float tx, float ty) {
        if (!ensureLoaded()) return null;
        RoadNetwork net = instance;
        float[] se = net.nearestEdgeProj(sx, sy, -1);
        float[] te = net.nearestEdgeProj(tx, ty, -1);
        if (se == null || te == null) return null;
        ArrayList<float[]> route = net.buildRoute(se, te, tx, ty);
        if (route != null) return route;
        // 不可达 → 数据洞兜底: 大分量内重吸附边 (大分量连通, 必可达)
        int big = net.biggestComponent();
        se = net.nearestEdgeProj(sx, sy, big);
        te = net.nearestEdgeProj(tx, ty, big);
        if (se == null || te == null) return null;
        return net.buildRoute(se, te, tx, ty);
    }

    /** 边吸附 → 多源 Dijkstra → 路线。不可达返回 null。 */
    private ArrayList<float[]> buildRoute(float[] se, float[] te, float tx, float ty) {
        int sa = (int) se[0], sb = (int) se[1];
        int tc = (int) te[0], td = (int) te[1];
        // 同一条路段: 直接投影点连线 (无需绕到端点节点, 消除端点折角)
        if (sa == tc && sb == td) {
            ArrayList<float[]> direct = new ArrayList<float[]>(3);
            direct.add(new float[]{se[2], se[3]});
            direct.add(new float[]{te[2], te[3]});
            direct.add(new float[]{tx, ty});
            return direct;
        }
        int[] starts = {sa, sb};
        float[] init = {dist(se[2], se[3], nodeX[sa], nodeY[sa]),
                dist(se[2], se[3], nodeX[sb], nodeY[sb])};
        Object[] res = dijkstraMulti(starts, init);
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
        route.add(new float[]{tx, ty});
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
    private Object[] dijkstraMulti(int[] starts, float[] initCost) {
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

    private boolean load() throws Exception {
        long t0 = System.currentTimeMillis();
        ArrayList<MapFiles> maps = MapFiles.getCurrentMapFiles();
        if (maps == null || maps.isEmpty()) {
            Logger.printLog("[AutoDrive] street network: no map directories");
            return false;
        }
        // 折线收集: 每条街道一条 float[] (x0,y0,x1,y1,...)
        ArrayList<float[]> polylines = new ArrayList<float[]>();
        for (MapFiles mf : maps) {
            java.io.File xml = new java.io.File(mf.mapDirectoryZfsPath, "streets.xml");
            if (!xml.isFile()) xml = new java.io.File(mf.mapDirectoryAbsolutePath, "streets.xml");
            if (!xml.isFile()) continue;
            int before = polylines.size();
            parseStreets(xml.getAbsolutePath(), polylines);
            Logger.printLog("[AutoDrive] street network: " + mf.mapDirectoryName
                    + " (" + (polylines.size() - before) + " streets)");
        }
        if (polylines.isEmpty()) {
            Logger.printLog("[AutoDrive] street network: no streets parsed (dirs="
                    + maps.size() + ")");
            return false;
        }
        buildGraph(polylines);
        Logger.printLog("[AutoDrive] street network loaded: " + polylines.size()
                + " streets, " + nodeX.length + " nodes, " + adjNode.length / 2
                + " links, " + (System.currentTimeMillis() - t0) + "ms");
        return true;
    }

    /** 解析一个目录的 streets.xml → 折线列表。 */
    private void parseStreets(String path, ArrayList<float[]> polylines) throws Exception {
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
        }
    }

    // ---------------- 平面化 + 建图 ----------------

    /** 网格桶: 段 bbox 覆盖的桶 → 段 id 列表。 */
    private final HashMap<Long, ArrayList<Integer>> segBuckets = new HashMap<Long, ArrayList<Integer>>();
    /** 端点吸附焊桥半径 (格): 实测 Riverside T 型路口悬空 4~6 格。 */
    private static final float WELD_R = 6.0f;
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
        for (float[] pl : polylines) {
            if (seenKeys.add(polylineKey(pl))) uniq.add(pl);
        }
        polylines = uniq;
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
        int welds = 0;
        for (int a = 0; a < segCount; a++) {
            float[][] ends = {{segX1[a], segY1[a]}, {segX2[a], segY2[a]}};
            for (float[] ep : ends) {
                for (int b : candidateSegments(a)) {
                    if (segStreet[a] == segStreet[b]) continue;
                    float[] pr = projectPoint(ep[0], ep[1], b);
                    if (pr == null) continue;
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
        idx = 0;
        for (int s = 0; s < polylines.size(); s++) {
            float[] pl = polylines.get(s);
            int prevEnd = -1;   // 上一段终点 (段间衔接边)
            for (int p = 0; p < pl.length / 2 - 1; p++, idx++) {
                int n0 = node(segX1[idx], segY1[idx], coords, nodeIds);
                if (prevEnd >= 0 && prevEnd != n0) { edgeFrom.add(prevEnd); edgeTo.add(n0); }
                ArrayList<float[]> splits = segSplits.get(idx);
                if (!splits.isEmpty()) {
                    splits.sort((u, v) -> Float.compare(u[0], v[0]));
                    for (float[] sp : splits) {
                        int n1 = node(sp[1], sp[2], coords, nodeIds);
                        if (n1 != n0) { edgeFrom.add(n0); edgeTo.add(n1); n0 = n1; }
                    }
                }
                int nEnd = node(segX2[idx], segY2[idx], coords, nodeIds);
                if (nEnd != n0) { edgeFrom.add(n0); edgeTo.add(nEnd); }
                prevEnd = nEnd;
            }
        }
        // 焊桥边追加 (端点悬空衔接)
        for (float[] br : bridges) {
            int na = node(br[0], br[1], coords, nodeIds);
            int nb = node(br[2], br[3], coords, nodeIds);
            if (na != nb) { edgeFrom.add(na); edgeTo.add(nb); }
        }
        finishAdjacency(coords, edgeFrom, edgeTo, polylines.size());
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
        if (ex * ex + ey * ey > WELD_R * WELD_R) return null;
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
                                 ArrayList<Integer> edgeTo, int streetCount) {
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
            float w = (float) Math.sqrt(dx * dx + dy * dy);
            int at = cursor[a]++;
            adjNode[at] = b;
            adjWeight[at] = w;
            at = cursor[b]++;
            adjNode[at] = a;
            adjWeight[at] = w;
        }
    }
}
