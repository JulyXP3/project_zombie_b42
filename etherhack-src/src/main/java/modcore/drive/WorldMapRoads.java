/*
 * 道路数据源升级 (C2, 2026-09-11): 大地图道路图层 (media/maps/<目录>/worldmap.xml.bin)。
 *
 * 背景: 现役路网源 streets.xml 只有**命名街道** — 命名街道之外的道路 (小区路/连接路/
 * 林区路) 在数据里不存在, 规划只能直线短驳 ("穿树林", 实测目的地距最近命名街道
 * 274 格)。游戏 M 键大地图渲染的道路图层 = worldmap 数据, 覆盖全图; 二进制格式
 * IGMB v2 与 zombie.worldMap.WorldMapBinary 同构, 手写解析零游戏类依赖。
 *
 * 世界坐标换算 (2026-09-11 实测标定): world = cellX*256 + localX (bin 的 cellSize
 * 字段 = 256; 用 KY-60 条带 (cell(50,43) local y 189..204 → world y 11197..11212)
 * 与 streets.xml KY-60 完全吻合验证)。注意 streets.xml 文本自身用 300 格 cell —
 * 两个数据源各自的 cell 索引不同, 但换算后的世界坐标同一空间。
 *
 * 几何形态: highway = Polygon 条带 (4 点矩形为主, 多点为 L/T/阶梯/缓弯条带),
 * 极小部分 LineString (本就是折线)。提取: 顶点 y 扫描带分解 → 矩形集合 →
 * 相邻带 x 重叠邻接 → 矩形中心图直径路径 → 沿矩形长轴串折线 → RDP 抽稀。
 * 产物 = 道路中心线折线 (与 streets.xml 同形态), 喂给 RoadNetwork 的现有建图管线
 * (去重/平面化/焊桥/Dijkstra) — RoadNetwork 以 worldMap 模式二次实例化使用。
 *
 * 失败边界: 任一目录解析失败仅跳过该目录; 全部失败返回空表, 调用方回退 streets
 * (或 direct) — 导航永不因本数据源崩溃。
 */
package modcore.drive;

import modcore.utils.Logger;
import zombie.iso.MapFiles;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.TreeSet;

public final class WorldMapRoads {

    /** cell 边长 (bin cellSize 字段值, 实测校准)。 */
    private static final int CELL = 256;
    /** 矩形分解上限 (防病态多边形; 正常道路 < 30)。 */
    private static final int MAX_RECTS = 96;
    /** 面积下限 (格^2): 噪点/装饰小面跳过。 */
    private static final float MIN_AREA = 15.0f;
    /** 相邻带矩形 x 重叠下限 (格): 小于此视为不连通。 */
    private static final float MIN_OVERLAP = 1.5f;
    /** 分支链最小长度 (格): 更短的交叉口碎片不输出 (weldR 兜接)。 */
    private static final float MIN_BRANCH_LEN = 6.0f;
    /** 折线抽稀容差 (格)。 */
    private static final float RDP_EPS = 2.0f;
    /** "大块路面" 判定 (格): ring0 在 x/y 任一方向跨度超过它 = 整块路面 (广场/大型
     *  路口/停车场), 其单条中线代表不了整块面 —— 供 RoadNetwork 落面焊接用
     *  (2026-09-11 B 轮实测: 园区接入端点落在这类面内却离中线 15~32 格 > 焊接半径)。 */
    private static final float AREA_MIN_SPAN = 60.0f;

    private static ArrayList<float[]> cached;
    private static ArrayList<float[]> areaRings;
    /** 全部 highway 多边形的**全部环** (外环 + 洞) — 路面判定用 (RoadNetwork 中线裁剪)。
     *  洞必须保留: 只按外环判会把院子/街区内院也算成路面 (2026-09-11 A 三轮实测)。 */
    private static ArrayList<float[][]> surfaceFeats;
    private static boolean loadFailed;

    private WorldMapRoads() {
    }

    /** 懒加载: 解析全部地图目录的 worldmap.xml.bin, 缓存。失败/缺失返回空表。 */
    public static synchronized ArrayList<float[]> loadPolylines() {
        if (cached != null) return cached;
        if (loadFailed) return new ArrayList<float[]>();
        long t0 = System.currentTimeMillis();
        ArrayList<float[]> out = new ArrayList<float[]>();
        ArrayList<float[]> areas = new ArrayList<float[]>();
        ArrayList<float[][]> feats = new ArrayList<float[][]>();
        int dirs = 0;
        try {
            ArrayList<MapFiles> maps = MapFiles.getCurrentMapFiles();
            if (maps != null) {
                for (MapFiles mf : maps) {
                    File bin = new File(mf.mapDirectoryAbsolutePath, "worldmap.xml.bin");
                    if (!bin.isFile()) continue;
                    dirs++;
                    int before = out.size();
                    try {
                        parseBin(bin, out, areas, feats);
                        Logger.printLog("[AutoDrive] worldmap roads: " + mf.mapDirectoryName
                                + " (" + (out.size() - before) + " polylines)");
                    } catch (Throwable t) {
                        Logger.printLog("[AutoDrive] worldmap parse failed: "
                                + mf.mapDirectoryName + " - " + t.getMessage());
                    }
                }
            }
            Logger.printLog("[AutoDrive] worldmap roads loaded: " + out.size()
                    + " polylines, " + dirs + " dirs, " + (System.currentTimeMillis() - t0) + "ms");
        } catch (Throwable t) {
            Logger.error("[AutoDrive] worldmap roads load failed", t);
        }
        if (out.isEmpty()) loadFailed = true;
        cached = out;
        areaRings = dedupRings(areas);
        Logger.printLog("[AutoDrive] worldmap big road areas: " + areaRings.size()
                + " rings (span>" + (int) AREA_MIN_SPAN + ")");
        surfaceFeats = dedupFeats(feats);
        Logger.printLog("[AutoDrive] worldmap road surface features: " + surfaceFeats.size());
        return cached;
    }

    /**
     * 大块路面外环 (跨度 > AREA_MIN_SPAN 的 Polygon ring0) — RoadNetwork 落面焊接用。
     * 与 loadPolylines 同一次解析产出, 不做二次读盘; 返回去重后的副本 (跨 cell 重复
     * 引用的同一多边形只留一份)。
     */
    public static synchronized ArrayList<float[]> loadAreaRings() {
        if (cached == null) loadPolylines();
        return areaRings == null ? new ArrayList<float[]>() : areaRings;
    }

    /** 全部道路多边形 (含洞) — 路面判定用。与中线同一次解析产出。 */
    public static synchronized ArrayList<float[][]> loadSurfaceFeatures() {
        if (cached == null) loadPolylines();
        return surfaceFeats == null ? new ArrayList<float[][]>() : surfaceFeats;
    }

    /** 多边形去重 (几何键: 环数 + 外环长度 + 首尾点)。 */
    private static ArrayList<float[][]> dedupFeats(ArrayList<float[][]> in) {
        ArrayList<float[][]> out = new ArrayList<float[][]>(in.size());
        TreeSet<String> seen = new TreeSet<String>();
        for (float[][] fs : in) {
            if (fs.length == 0) continue;
            float[] r0 = fs[0];
            if (r0.length < 6) continue;
            String key = fs.length + ":" + r0.length + ":"
                    + Math.round(r0[0] * 4f) + "," + Math.round(r0[1] * 4f) + ":"
                    + Math.round(r0[r0.length - 2] * 4f) + "," + Math.round(r0[r0.length - 1] * 4f);
            if (seen.add(key)) out.add(fs);
        }
        return out;
    }

    /** 环去重 (几何键: 顶点数 + 首尾点 0.5 量化)。 */
    private static ArrayList<float[]> dedupRings(ArrayList<float[]> in) {
        ArrayList<float[]> out = new ArrayList<float[]>(in.size());
        TreeSet<String> seen = new TreeSet<String>();
        for (float[] r : in) {
            if (r.length < 6) continue;
            int n = r.length / 2;
            String key = n + ":" + Math.round(r[0] * 2f) + "," + Math.round(r[1] * 2f)
                    + ":" + Math.round(r[r.length - 2] * 2f) + "," + Math.round(r[r.length - 1] * 2f);
            if (seen.add(key)) out.add(r);
        }
        return out;
    }

    // ================================================================
    // 二进制解析 (IGMB v2)
    // ================================================================

    /** 解析单个 worldmap.xml.bin, highway 折线(中线)追加到 out (package 可见: 离线自检)。 */
    public static void parseBin(File f, ArrayList<float[]> out) throws Exception {
        parseBin(f, out, null);
    }

    /**
     * 解析单个 worldmap.xml.bin: 中线追加 out; areas != null 时同时收集"大块路面"
     * 外环 (跨度 > AREA_MIN_SPAN 的 Polygon ring0) 供落面焊接。
     */
    public static void parseBin(File f, ArrayList<float[]> out, ArrayList<float[]> areas) throws Exception {
        parseBin(f, out, areas, null);
    }

    /**
     * 解析单个 worldmap.xml.bin: 中线追加 out; areas != null 收集"大块路面"外环;
     * feats != null 收集全部 highway 多边形的全部环 (外环 + 洞, 路面判定用)。
     */
    public static void parseBin(File f, ArrayList<float[]> out, ArrayList<float[]> areas,
            ArrayList<float[][]> feats) throws Exception {
        byte[] b = Files.readAllBytes(f.toPath());
        if (b.length < 24 || b[0] != 'I' || b[1] != 'G' || b[2] != 'M' || b[3] != 'B') {
            throw new java.io.IOException("bad magic");
        }
        int off = 4;
        int ver = i32(b, off); off += 4;
        int cellSize = i32(b, off); off += 4;
        int w = i32(b, off); off += 4;
        int h = i32(b, off); off += 4;
        if (ver != 2 || cellSize != CELL) {
            throw new java.io.IOException("unsupported version/cellSize: " + ver + "/" + cellSize);
        }
        int numStr = i32(b, off); off += 4;
        if (numStr <= 0 || numStr > 65536) throw new java.io.IOException("bad string table: " + numStr);
        String[] strs = new String[numStr];
        for (int i = 0; i < numStr; i++) {
            int len = i16(b, off); off += 2;
            if (len < 0 || off + len > b.length) throw new java.io.IOException("bad string len");
            strs[i] = new String(b, off, len, StandardCharsets.UTF_8);
            off += len;
        }
        for (int cy = 0; cy < h; cy++) {
            for (int cx = 0; cx < w; cx++) {
                int x = i32(b, off); off += 4;
                if (x == -1) continue;             // 空格子
                int y = i32(b, off); off += 4;
                int nf = i32(b, off); off += 4;
                for (int fi = 0; fi < nf; fi++) {
                    int typeIdx = i16(b, off); off += 2;
                    String type = (typeIdx >= 0 && typeIdx < numStr) ? strs[typeIdx] : "";
                    int nCoord = b[off] & 0xFF; off += 1;
                    ArrayList<float[]> rings = new ArrayList<float[]>(nCoord);
                    for (int ci = 0; ci < nCoord; ci++) {
                        int nPts = i16(b, off); off += 2;
                        if (nPts < 0 || off + nPts * 4 > b.length) throw new java.io.IOException("bad point count");
                        float[] ring = new float[nPts * 2];
                        for (int pi = 0; pi < nPts; pi++) {
                            int px = i16(b, off); off += 2;
                            int py = i16(b, off); off += 2;
                            ring[pi * 2] = x * CELL + px;
                            ring[pi * 2 + 1] = y * CELL + py;
                        }
                        rings.add(ring);
                    }
                    int nProps = b[off] & 0xFF; off += 1;
                    String highway = null;
                    for (int pi = 0; pi < nProps; pi++) {
                        int ni = i16(b, off); off += 2;
                        int vi = i16(b, off); off += 2;
                        if (ni >= 0 && ni < numStr && "highway".equals(strs[ni])) {
                            highway = (vi >= 0 && vi < numStr) ? strs[vi] : null;
                        }
                    }
                    if (highway == null || highway.isEmpty()) continue;
                    // 只取外环 (ring 0): 后续环是洞/院子, 不是道路 (旧实现把洞也当路
                    // 提取 → 院子里长假路)。
                    if (rings.isEmpty()) continue;
                    if ("Polygon".equals(type)) {
                        spinesOfRing(rings.get(0), out);
                        if (areas != null && spanOf(rings.get(0)) > AREA_MIN_SPAN) {
                            areas.add(rings.get(0));
                        }
                        if (feats != null) {
                            feats.add(rings.toArray(new float[0][]));
                        }
                    } else if ("LineString".equals(type) && rings.get(0).length >= 4) {
                        out.add(rings.get(0));
                    }
                }
            }
        }
    }

    // ================================================================
    // 中线提取 (扫描带分解 → 矩形图 → 骨架树: 主干 + 每分支一条链 → 长轴串线 → RDP)
    //
    // 2026-09-11 A1 修复: 旧实现每多边形只取一条"直径路径" — 道路多边形的所有
    // 分支 (接入走廊/匝道/环岛/停车场) 被丢弃, 图里园区/匝道成孤岛, MST 跨野桥接
    // 造成荒谬路线 (实测: 园区 136 格接入走廊被丢)。现改为**骨架树链分解**:
    //   ① 每个连通分量取直径路径 = 主干 (旧逻辑);
    //   ② 从已覆盖集合多源 Dijkstra, 取最远未覆盖矩形, 回走至覆盖集合 → 一条分支;
    //      重复到全部分量覆盖 (Prim 式生长, 先长后短);
    //   ③ 每条主干/分支单独输出折线 (矩形长轴串线 + RDP)。
    //   ④ 分支链尾点取接入矩形的长轴端点 — 与主干在同矩形的点重合, 建图时天然共享节点。
    // 只处理外环 (调用方只传 ring 0); 洞/院子不是道路。
    // ================================================================

    /** 环的包围盒最长边 (格) — 大块路面判定用。 */
    private static float spanOf(float[] ring) {
        float minx = Float.MAX_VALUE, maxx = -Float.MAX_VALUE;
        float miny = Float.MAX_VALUE, maxy = -Float.MAX_VALUE;
        for (int i = 0; i + 1 < ring.length; i += 2) {
            if (ring[i] < minx) minx = ring[i];
            if (ring[i] > maxx) maxx = ring[i];
            if (ring[i + 1] < miny) miny = ring[i + 1];
            if (ring[i + 1] > maxy) maxy = ring[i + 1];
        }
        return Math.max(maxx - minx, maxy - miny);
    }

    /** 单多边形外环 → 全部骨架折线 (主干 + 每条分支各一条), 追加到 out (package 可见: 自检)。 */
    public static void spinesOfRing(float[] ring, ArrayList<float[]> out) {
        int n = ring.length / 2;
        if (n < 3) return;
        // 鞋带面积过滤
        float area2 = 0;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            area2 += ring[i * 2] * ring[j * 2 + 1] - ring[j * 2] * ring[i * 2 + 1];
        }
        if (Math.abs(area2) * 0.5f < MIN_AREA) return;
        // 顶点 y 扫描带 (0.5 量化防抖)
        TreeSet<Integer> ysSet = new TreeSet<Integer>();
        for (int i = 0; i < n; i++) ysSet.add(Math.round(ring[i * 2 + 1] * 2.0f));
        if (ysSet.size() < 2) return;
        Integer[] yq = ysSet.toArray(new Integer[0]);
        // 每带求内部 x 区间 → 矩形
        ArrayList<float[]> rects = new ArrayList<float[]>();
        outer:
        for (int bi = 0; bi + 1 < yq.length; bi++) {
            float y0 = yq[bi] * 0.5f;
            float y1 = yq[bi + 1] * 0.5f;
            if (y1 - y0 < 0.5f) continue;
            float mid = (y0 + y1) * 0.5f;
            ArrayList<Float> xs = new ArrayList<Float>();
            for (int i = 0; i < n; i++) {
                int j = (i + 1) % n;
                float ax = ring[i * 2], ay = ring[i * 2 + 1];
                float bx = ring[j * 2], by = ring[j * 2 + 1];
                boolean aAbove = ay > mid, bAbove = by > mid;
                if (aAbove != bAbove) {
                    xs.add(ax + (mid - ay) * (bx - ax) / (by - ay));
                }
            }
            Collections.sort(xs);
            for (int k = 0; k + 1 < xs.size(); k += 2) {
                float x0 = xs.get(k), x1 = xs.get(k + 1);
                if (x1 - x0 < 0.5f) continue;
                rects.add(new float[]{x0, x1, y0, y1});
                if (rects.size() >= MAX_RECTS) break outer;
            }
        }
        int m = rects.size();
        if (m == 0) return;
        if (m == 1) {
            float[] seg = rdp(axisSegment(rects.get(0)), RDP_EPS);
            if (seg != null && seg.length >= 4) out.add(seg);
            return;
        }
        // 邻接: 相邻带 + x 重叠
        boolean[][] adj = new boolean[m][m];
        for (int i = 0; i < m; i++) {
            for (int j = i + 1; j < m; j++) {
                float[] a = rects.get(i), c = rects.get(j);
                float ov = Math.min(a[1], c[1]) - Math.max(a[0], c[0]);
                if (ov < MIN_OVERLAP) continue;
                boolean bandAdj = Math.abs(a[3] - c[2]) < 0.75f || Math.abs(c[3] - a[2]) < 0.75f;
                if (bandAdj) {
                    adj[i][j] = true;
                    adj[j][i] = true;
                }
            }
        }
        boolean[] covered = new boolean[m];
        // 逐连通分量: 主干 + 分支
        for (int seed = 0; seed < m; seed++) {
            if (covered[seed]) continue;
            // 主干 = 分量内直径路径 (两轮最远, 边权 = 中心距; m 小, 朴素 Dijkstra)
            int endA = farthest(m, seed, adj, rects, null);
            int[] prev = new int[m];
            java.util.Arrays.fill(prev, -1);
            int endB = farthest(m, endA, adj, rects, prev);
            ArrayList<Integer> trunk = walkBack(prev, endB);
            if (trunk.isEmpty()) { covered[seed] = true; continue; }
            emitPath(rects, trunk, out);
            for (int r : trunk) covered[r] = true;
            // 分支: 从覆盖集合多源生长, 每次取最远未覆盖点回走到覆盖集合
            float[] bd = new float[m];
            int[] bp = new int[m];
            while (true) {
                java.util.Arrays.fill(bd, Float.POSITIVE_INFINITY);
                java.util.Arrays.fill(bp, -1);
                multiSourceDijkstra(covered, m, adj, rects, bd, bp);
                int far = -1;
                float fd = -1;
                for (int i = 0; i < m; i++) {
                    if (!covered[i] && bd[i] < Float.POSITIVE_INFINITY && bd[i] > fd) {
                        fd = bd[i];
                        far = i;
                    }
                }
                if (far < 0) break;    // 本分量全部覆盖
                if (fd < MIN_BRANCH_LEN) {
                    // 只剩短碎片 (交叉口矩形噪声): 不再输出, 整分量收尾 —
                    // 分支链过多会使折线/节点数暴涨 (实测 4.5x, 建图 244→1641ms),
                    // 短于 6 格的分支由 weldR=8 兜接, 无导航价值。
                    for (int i = 0; i < m; i++) {
                        if (!covered[i] && bd[i] < Float.POSITIVE_INFINITY) covered[i] = true;
                    }
                    continue;
                }
                ArrayList<Integer> branch = new ArrayList<Integer>();
                int cur = far;
                while (cur != -1 && !covered[cur]) {
                    branch.add(cur);
                    cur = bp[cur];
                }
                if (cur >= 0) branch.add(cur);   // 接入点 (已覆盖矩形, 与主干共享节点)
                Collections.reverse(branch);     // 覆盖端 → 叶端
                emitPath(rects, branch, out);
                for (int r : branch) covered[r] = true;
            }
        }
    }

    /** 兼容包装 (离线自检/旧调用): 返回最长的一条骨架折线; 无输出返回 null。 */
    public static float[] spineOfRing(float[] ring) {
        ArrayList<float[]> lines = new ArrayList<float[]>();
        spinesOfRing(ring, lines);
        float[] best = null;
        for (float[] l : lines) {
            if (best == null || l.length > best.length) best = l;
        }
        return best;
    }

    /** prev 链回溯 (end → -1) 并反转为起点序。 */
    private static ArrayList<Integer> walkBack(int[] prev, int end) {
        ArrayList<Integer> path = new ArrayList<Integer>();
        for (int cur = end; cur != -1; cur = prev[cur]) path.add(cur);
        Collections.reverse(path);
        return path;
    }

    /** 从 covered 集合出发的多源 Dijkstra (中心距), 填 dist/parent (朴素, m<=96)。 */
    private static void multiSourceDijkstra(boolean[] covered, int m, boolean[][] adj,
            ArrayList<float[]> rects, float[] dist, int[] parent) {
        boolean[] done = new boolean[m];
        for (int i = 0; i < m; i++) {
            if (covered[i]) {
                dist[i] = 0;
                parent[i] = -1;
            }
        }
        for (int it = 0; it < m; it++) {
            int u = -1;
            float best = Float.POSITIVE_INFINITY;
            for (int i = 0; i < m; i++) {
                if (!done[i] && dist[i] < best) {
                    best = dist[i];
                    u = i;
                }
            }
            if (u < 0) break;
            done[u] = true;
            float[] ru = rects.get(u);
            float ux = (ru[0] + ru[1]) * 0.5f, uy = (ru[2] + ru[3]) * 0.5f;
            for (int v = 0; v < m; v++) {
                if (!adj[u][v] || done[v]) continue;
                float[] rv = rects.get(v);
                float vx = (rv[0] + rv[1]) * 0.5f, vy = (rv[2] + rv[3]) * 0.5f;
                float nd = dist[u] + d2(ux, uy, vx, vy);
                if (nd < dist[v]) {
                    dist[v] = nd;
                    parent[v] = u;
                }
            }
        }
    }

    /** 矩形路径 → 长轴串线折线 (首端远端 → 中间进/出端 → 尾端远端), RDP 后追加。 */
    private static void emitPath(ArrayList<float[]> rects, ArrayList<Integer> path,
            ArrayList<float[]> out) {
        if (path.isEmpty()) return;
        ArrayList<float[]> pts = new ArrayList<float[]>();
        for (int pi = 0; pi < path.size(); pi++) {
            float[] seg = axisSegment(rects.get(path.get(pi)));
            if (path.size() == 1) {
                pts.add(new float[]{seg[0], seg[1]});
                pts.add(new float[]{seg[2], seg[3]});
                break;
            }
            if (pi == 0) {
                float[] nc = center(rects.get(path.get(1)));
                pts.add(fartherEnd(seg, nc));
            } else if (pi == path.size() - 1) {
                float[] last = pts.get(pts.size() - 1);
                float[] entry = nearerEnd(seg, last);
                float[] exit = otherEnd(seg, entry);
                pts.add(entry);
                pts.add(exit);
            } else {
                float[] last = pts.get(pts.size() - 1);
                float[] entry = nearerEnd(seg, last);
                float[] exit = nearerEnd(seg, center(rects.get(path.get(pi + 1))));
                pts.add(entry);
                if (entry[0] != exit[0] || entry[1] != exit[1]) {
                    pts.add(exit);
                } else {
                    // 进/出同一端: 该矩形的另一端是死胡同 (如园区接入走廊 —
                    // 走廊与园区同属一个宽矩形, 主干只走西侧) → 另一端单独输出,
                    // 否则整条支路消失 (2026-09-11 A1 实测)。
                    float[] far = otherEnd(seg, entry);
                    if (d2(far[0], far[1], entry[0], entry[1]) >= 2.0f) {
                        out.add(new float[]{entry[0], entry[1], far[0], far[1]});
                    }
                }
            }
        }
        if (pts.size() < 2) return;
        float[] flat = new float[pts.size() * 2];
        for (int i = 0; i < pts.size(); i++) {
            flat[i * 2] = pts.get(i)[0];
            flat[i * 2 + 1] = pts.get(i)[1];
        }
        float[] line = rdp(flat, RDP_EPS);
        if (line != null && line.length >= 4) out.add(line);
    }

    /** 矩形长轴段: 宽 >= 高 → 水平中线, 否则垂直中线。 */
    private static float[] axisSegment(float[] r) {
        float cx = (r[0] + r[1]) * 0.5f;
        float cy = (r[2] + r[3]) * 0.5f;
        if (r[1] - r[0] >= r[3] - r[2]) {
            return new float[]{r[0], cy, r[1], cy};
        }
        return new float[]{cx, r[2], cx, r[3]};
    }

    /** 矩形中心。 */
    private static float[] center(float[] r) {
        return new float[]{(r[0] + r[1]) * 0.5f, (r[2] + r[3]) * 0.5f};
    }

    /** 长轴段 {x1,y1,x2,y2} 中距 p 较远的一端。 */
    private static float[] fartherEnd(float[] seg, float[] p) {
        float d1 = d2(seg[0], seg[1], p[0], p[1]);
        float d2v = d2(seg[2], seg[3], p[0], p[1]);
        return d1 >= d2v ? new float[]{seg[0], seg[1]} : new float[]{seg[2], seg[3]};
    }

    /** 长轴段中距 p 较近的一端。 */
    private static float[] nearerEnd(float[] seg, float[] p) {
        float d1 = d2(seg[0], seg[1], p[0], p[1]);
        float d2v = d2(seg[2], seg[3], p[0], p[1]);
        return d1 <= d2v ? new float[]{seg[0], seg[1]} : new float[]{seg[2], seg[3]};
    }

    /** 长轴段中与 end 相对的端点。 */
    private static float[] otherEnd(float[] seg, float[] end) {
        if (end[0] == seg[0] && end[1] == seg[1]) {
            return new float[]{seg[2], seg[3]};
        }
        return new float[]{seg[0], seg[1]};
    }

    /** 单源最远点 (朴素 Dijkstra; m 小)。prev != null 时回填路径。 */
    private static int farthest(int m, int start, boolean[][] adj, ArrayList<float[]> rects, int[] prev) {
        float[] dist = new float[m];
        boolean[] done = new boolean[m];
        java.util.Arrays.fill(dist, Float.POSITIVE_INFINITY);
        dist[start] = 0;
        for (int it = 0; it < m; it++) {
            int u = -1;
            float best = Float.POSITIVE_INFINITY;
            for (int i = 0; i < m; i++) {
                if (!done[i] && dist[i] < best) {
                    best = dist[i];
                    u = i;
                }
            }
            if (u < 0) break;
            done[u] = true;
            float[] ru = rects.get(u);
            float ux = (ru[0] + ru[1]) * 0.5f, uy = (ru[2] + ru[3]) * 0.5f;
            for (int v = 0; v < m; v++) {
                if (!adj[u][v] || done[v]) continue;
                float[] rv = rects.get(v);
                float vx = (rv[0] + rv[1]) * 0.5f, vy = (rv[2] + rv[3]) * 0.5f;
                float nd = dist[u] + d2(ux, uy, vx, vy);
                if (nd < dist[v]) {
                    dist[v] = nd;
                    if (prev != null) prev[v] = u;
                }
            }
        }
        int bestI = start;
        float bestD = -1;
        for (int i = 0; i < m; i++) {
            if (dist[i] < Float.POSITIVE_INFINITY && dist[i] > bestD) {
                bestD = dist[i];
                bestI = i;
            }
        }
        return bestI;
    }

    /** Ramer-Douglas-Peucker 抽稀 (输入/输出 flat {x0,y0,x1,y1,...})。 */
    private static float[] rdp(float[] pts, float eps) {
        int n = pts.length / 2;
        if (n <= 2) return pts;
        boolean[] keep = new boolean[n];
        keep[0] = true;
        keep[n - 1] = true;
        rdpRec(pts, 0, n - 1, eps, keep);
        int cnt = 0;
        for (int i = 0; i < n; i++) if (keep[i]) cnt++;
        if (cnt < 2) return pts;
        float[] out = new float[cnt * 2];
        int at = 0;
        for (int i = 0; i < n; i++) {
            if (!keep[i]) continue;
            out[at++] = pts[i * 2];
            out[at++] = pts[i * 2 + 1];
        }
        return out;
    }

    private static void rdpRec(float[] pts, int a, int b, float eps, boolean[] keep) {
        if (b - a < 2) return;
        float ax = pts[a * 2], ay = pts[a * 2 + 1];
        float bx = pts[b * 2], by = pts[b * 2 + 1];
        float dx = bx - ax, dy = by - ay;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        float maxD = -1;
        int maxI = -1;
        for (int i = a + 1; i < b; i++) {
            float px = pts[i * 2], py = pts[i * 2 + 1];
            float d;
            if (len < 1e-6f) {
                d = (float) Math.sqrt((px - ax) * (px - ax) + (py - ay) * (py - ay));
            } else {
                d = Math.abs((px - ax) * dy - (py - ay) * dx) / len;
            }
            if (d > maxD) {
                maxD = d;
                maxI = i;
            }
        }
        if (maxD > eps && maxI > 0) {
            keep[maxI] = true;
            rdpRec(pts, a, maxI, eps, keep);
            rdpRec(pts, maxI, b, eps, keep);
        }
    }

    private static float d2(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1, dy = y2 - y1;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private static int i32(byte[] b, int off) {
        return (b[off] & 0xFF) | ((b[off + 1] & 0xFF) << 8)
                | ((b[off + 2] & 0xFF) << 16) | ((b[off + 3] & 0xFF) << 24);
    }

    private static int i16(byte[] b, int off) {
        return (short) ((b[off] & 0xFF) | ((b[off + 1] & 0xFF) << 8));
    }
}
