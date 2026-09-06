/*
 * 车宽净空 BFS 寻路 (自动驾驶专用)。
 *
 * 与 UIMap 步行 BFS 的判定规则本质不同 (车宽净空 vs 步行可走), 按研判文档 §八
 * 不抽公共类: 判定 = 候选格为中心的 3×3 核内无墙/水/树/窗/关门, 无其他载具
 * AABB 重叠 (僵尸不计入 — 硬闯设计, 2026-09-06)。BFS 只认已加载格
 * (getGridSquare 空 = 阻挡), 长途由 AutoDriveController 滚动规划: 先开到已知
 * 边界, 边开边随流式刷新分段延长。
 *
 * 两种模式 (2026-09-06 用户拍板 "锚定之后直接按道路往终点走"):
 * - roadOnly=true 道路网模式: 只许走道路格 (原版车辆事故系统同口径的沥青/砾石
 *   路面), 路线贴道路 — 主路由;
 * - roadOnly=false 全地形模式 (兜底): 路网断连/接驳失败时直奔终点 (穿墙保证可达)。
 * 核半径统一 3×3: 穿墙时代 5×5 大核反而制造"找不到路线" (窄巷小区域必失败, 实测)。
 *
 * 目标落在未加载区/净空不可达时返回"离目标欧氏距离最近的可达格"路径
 * (reachedTarget=false), 供滚动续驶; 完全无可达格才返回 null。
 */
package EtherHack.drive;

import zombie.core.properties.PropertyContainer;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoWindow;
import zombie.vehicles.BaseVehicle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public final class AutoDrivePathfinder {

    /** 规划结果: 简化后的世界坐标路点序列 {x, y}(格中心), 以及是否精确到达目标。 */
    public static final class Path {
        public final ArrayList<float[]> points = new ArrayList<float[]>();
        public boolean reachedTarget = false;
    }

    /** BFS 节点上限: 车宽净空核判定较重, 防御性封顶 (约 275x275 格范围)。 */
    private static final int MAX_NODES = 120000;
    /** 路径直线化时单段可视检查的前瞻窗口 (格), 防 O(n²) 退化。 */
    private static final int SMOOTH_WINDOW = 48;

    private AutoDrivePathfinder() {
    }

    /**
     * 车宽净空 BFS。
     * 僵尸不计入阻挡 (硬闯设计: 路径只由静态/载具决定, 不随尸群晃动)。
     *
     * @param sx/sy       起点格坐标 (车辆当前所在)
     * @param z           层 (同层规划, 跨层目标直接不可达)
     * @param tx/ty       目标世界坐标
     * @param self        本车 (从阻挡清单排除), 可 null
     * @param roadOnly    true = 道路网模式 (只走道路格); false = 全地形 (兜底)
     */
    public static Path findPath(int sx, int sy, int z, float tx, float ty,
                                BaseVehicle self, boolean roadOnly) {
        IsoCell cell = IsoWorld.instance.currentCell;
        if (cell == null) {
            return null;
        }
        // 净空核统一 3×3: 穿墙时代 5×5 大核反而制造"找不到路线" (窄巷/小区域必失败,
        // 实测), 3×3 (路径中心 ±1.5 格) 对车半宽 ~1.1 足够; 道路模式亦适配窄路
        int r = 1;

        // 其他载具 → 膨胀 AABB (多边形包围盒再膨胀净空半径), 一次性收集
        ArrayList<float[]> vehicleBoxes = new ArrayList<float[]>();
        for (BaseVehicle v : cell.getVehicles()) {
            if (v == self) continue;
            float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
            float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
            for (float[] corner : new float[][]{
                    {v.getPoly().x1, v.getPoly().y1}, {v.getPoly().x2, v.getPoly().y2},
                    {v.getPoly().x3, v.getPoly().y3}, {v.getPoly().x4, v.getPoly().y4}}) {
                minX = Math.min(minX, corner[0]);
                minY = Math.min(minY, corner[1]);
                maxX = Math.max(maxX, corner[0]);
                maxY = Math.max(maxY, corner[1]);
            }
            vehicleBoxes.add(new float[]{minX - r - 0.5f, minY - r - 0.5f, maxX + r + 0.5f, maxY + r + 0.5f});
        }

        HashMap<Long, Boolean> passCache = new HashMap<Long, Boolean>();
        Kernel kernel = new Kernel(cell, z, r, roadOnly, vehicleBoxes, passCache);

        int txi = (int) Math.floor(tx);
        int tyi = (int) Math.floor(ty);
        float arriveR2 = 4.0f * 4.0f; // 到达半径 4 格 (研判 §五)

        HashMap<Long, Long> parent = new HashMap<Long, Long>();
        HashSet<Long> visited = new HashSet<Long>();
        ArrayList<Long> queue = new ArrayList<Long>();
        long startKey = key(sx, sy);
        visited.add(startKey);
        queue.add(startKey);
        // 最近的可达格 (到目标欧氏距离最小) — 目标不可达时的滚动规划中间点
        long bestKey = -1;
        float bestD2 = Float.MAX_VALUE;

        int expanded = 0;
        int head = 0;
        boolean reached = false;
        long reachedKey = -1;
        while (head < queue.size() && expanded < MAX_NODES) {
            long cur = queue.get(head++);
            expanded++;
            int cx = (int) (cur >> 32);
            int cy = (int) (cur & 0xFFFFFFFFL);
            float ccx = cx + 0.5f;
            float ccy = cy + 0.5f;
            float d2 = (ccx - tx) * (ccx - tx) + (ccy - ty) * (ccy - ty);
            if (d2 < bestD2) {
                bestD2 = d2;
                bestKey = cur;
            }
            if (d2 <= arriveR2) {
                reached = true;
                reachedKey = cur;
                break;
            }
            for (int[] dir : DIRS4) {
                int nx = cx + dir[0];
                int ny = cy + dir[1];
                long nk = key(nx, ny);
                if (visited.contains(nk)) continue;
                visited.add(nk);
                if (kernel.blocked(nx, ny)) continue;
                parent.put(nk, cur);
                queue.add(nk);
            }
        }

        long endKey = reached ? reachedKey : bestKey;
        if (endKey < 0) {
            return null;
        }

        // 回溯 → 格序列
        ArrayList<int[]> cells = new ArrayList<int[]>();
        for (long k = endKey; k != startKey; k = parent.get(k)) {
            cells.add(new int[]{(int) (k >> 32), (int) (k & 0xFFFFFFFFL)});
        }
        Collections.reverse(cells);

        // 直线化: 贪心取窗口内最远可视格 (可视检查同样走净空核, 防切内角)
        Path path = new Path();
        path.reachedTarget = reached;
        int i = -1; // 相对 cells 下标, -1 = 起点
        int sx2 = sx, sy2 = sy;
        while (i < cells.size() - 1) {
            int j = Math.min(cells.size() - 1, i + SMOOTH_WINDOW);
            int chosen = i;
            for (; j > i; j--) {
                int[] c = cells.get(j);
                if (kernel.lineOfSight(sx2, sy2, c[0], c[1])) {
                    chosen = j;
                    break;
                }
            }
            if (chosen == i) {
                // 窗口内无可视 (极窄巷): 退化为逐格输出
                chosen = i + 1;
            }
            int[] c = cells.get(chosen);
            path.points.add(new float[]{c[0] + 0.5f, c[1] + 0.5f});
            sx2 = c[0];
            sy2 = c[1];
            i = chosen;
        }
        return path;
    }

    private static final int[][] DIRS4 = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    /**
     * 道路格判定 (原版车辆事故系统同口径, RandomizedVehicleStoryBase:222):
     * 沥青族 floor sprite (blends_street*, 剔除 _01_16/21/48/53/54/55 六个
     * 砾石/泥土变体) 或 FootstepMaterial = Gravel/Sand 的砾石/沙路。
     * 草地/泥地/树林/田野/牧场都不算道路 (blends_natural 等自然地面族)。
     */
    public static boolean isRoadSquare(IsoGridSquare sq) {
        if (sq == null) return false;
        IsoObject floor = sq.getFloor();
        if (floor == null || floor.getSprite() == null) return false;
        String name = floor.getSprite().getName();
        if (name == null) return false;
        if (name.startsWith("blends_street")) {
            return !(name.startsWith("blends_street_01_16") || name.startsWith("blends_street_01_21")
                    || name.startsWith("blends_street_01_48") || name.startsWith("blends_street_01_53")
                    || name.startsWith("blends_street_01_54") || name.startsWith("blends_street_01_55"));
        }
        PropertyContainer props = floor.getProperties();
        if (props != null && props.has("FootstepMaterial")) {
            Object mat = props.get("FootstepMaterial");
            return "Gravel".equals(mat) || "Sand".equals(mat);
        }
        return false;
    }

    /** 最近道路格 (环形外扩扫描, 接驳段用): 返回 {x, y} 或 null (半径内无路)。 */
    public static int[] nearestRoadCell(int cx, int cy, int z, int radius) {
        IsoCell cell = IsoWorld.instance.currentCell;
        if (cell == null) return null;
        if (isRoadSquare(cell.getGridSquare(cx, cy, z))) return new int[]{cx, cy};
        for (int r = 1; r <= radius; r++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dx = -r; dx <= r; dx++) {
                    if (Math.max(Math.abs(dx), Math.abs(dy)) != r) continue; // 只扫环边
                    IsoGridSquare sq = cell.getGridSquare(cx + dx, cy + dy, z);
                    if (isRoadSquare(sq)) return new int[]{cx + dx, cy + dy};
                }
            }
        }
        return null;
    }

    /**
     * 单格静态阻挡判定 (寻路核 / 感知扫描 / 绕行净空共用同一真相源):
     * 除墙/水/树/窗/关门外, 补齐 collideN/collideW 碰撞面族 —— 围栏/路灯/护栏/
     * 柜台等"非 solid 但物理上撞得上"的整族。判定口径与原版移动阻挡数据同源
     * (MapCollisionData.isBlockedN/W、BorderFinder 读的都是这两面标记);
     * 车不能翻栏, HoppableN/W 不豁免 (那是给会爬的步行者用的)。
     *
     * 载具物理真值补全 (IsoChunk.addPhysicsShapes 是车辆碰撞体的唯一装配点,
     * 差集逐条对照补齐, 否则车会开进"扫描看不见但物理撞得上"的方块后顶死):
     * - blocksight 方块按 Solid 加碰撞体 (IsoChunk:1946) — 高灌木/树篱一族,
     *   行人碰撞标志查不到它们, 车辆却会撞上 (实测"怼进小灌木"根因);
     * - IsoThumpable.isBlockAllTheSquare → Solid (IsoChunk:1920, 玩家造墙/栅栏);
     * - 带 PhysicsShape 属性的物件 / 灯柱族 sprite (lighting_outdoor_*, IsoChunk:1906)
     *   → Tree 柱体碰撞。
     */
    public static boolean staticBlocked(IsoGridSquare sq) {
        if (sq == null) return true;             // 未加载 = 阻挡 (滚动规划的边界)
        if (sq.isSolid() || sq.isSolidTrans() || sq.isWaterSquare()) return true;
        if (sq.getTree() != null) return true;
        if (sq.has(IsoFlagType.collideN) || sq.has(IsoFlagType.collideW)) return true;
        if (sq.has(IsoFlagType.blocksight)) return true;
        if (sq.has(IsoFlagType.windowN) || sq.has(IsoFlagType.windowW)
                || sq.has(IsoFlagType.WindowN) || sq.has(IsoFlagType.WindowW)) return true;
        // getObjects() 返回 PZArrayList, 其 iterator() 被原版实现为直接抛
        // UnsupportedOperationException (逼热点走下标循环), 只能 get(i) 遍历
        java.util.List<?> squareObjects = sq.getObjects();
        for (int oi = 0; oi < squareObjects.size(); oi++) {
            if (vehicleSolidObject(squareObjects.get(oi))) return true;
        }
        // 特殊物件列表 (玩家造墙/栅栏等 IsoThumpable 常走这里, IsoChunk:1915 同源)
        java.util.List<?> specialObjects = sq.getSpecialObjects();
        for (int oi = 0; oi < specialObjects.size(); oi++) {
            if (vehicleSolidObject(specialObjects.get(oi))) return true;
        }
        return false;
    }

    /** 物件级载具阻挡 (与 IsoChunk 物件分支同源): 见 staticBlocked 注释。 */
    private static boolean vehicleSolidObject(Object o) {
        if (o instanceof IsoWindow) return true;   // 窗 = 墙体开口, 保守不穿
        if (o instanceof IsoDoor && !((IsoDoor) o).isOpen()) return true;
        if (o instanceof IsoThumpable && ((IsoThumpable) o).isBlockAllTheSquare()) return true;
        if (!(o instanceof IsoObject)) return false;
        IsoObject obj = (IsoObject) o;
        if (obj.getProperties().has(IsoFlagType.collideN)
                || obj.getProperties().has(IsoFlagType.collideW)) return true;
        if (obj.hasProperty("PhysicsShape")) return true;
        // 灯柱族: sprite 名命中即柱体碰撞 (IsoChunk:1906 白名单同源)
        if (obj.getSprite() != null && obj.getSprite().getName() != null) {
            String name = obj.getSprite().getName();
            if (name.contains("lighting_outdoor_")) return true;
        }
        return false;
    }

    private static long key(int x, int y) {
        return ((long) x << 32) | (y & 0xFFFFFFFFL);
    }

    /** 净空判定器: 单格可行走缓存 + 3×3 核判定 + 直线可视。 */
    private static final class Kernel {
        private final IsoCell cell;
        private final int z;
        private final int r;
        private final boolean roadOnly;
        private final ArrayList<float[]> vehicleBoxes;
        private final HashMap<Long, Boolean> passCache;

        Kernel(IsoCell cell, int z, int r, boolean roadOnly, ArrayList<float[]> vehicleBoxes,
               HashMap<Long, Boolean> passCache) {
            this.cell = cell;
            this.z = z;
            this.r = r;
            this.roadOnly = roadOnly;
            this.vehicleBoxes = vehicleBoxes;
            this.passCache = passCache;
        }

        /** 核判定: 以 (x,y) 为中心, (2r+1)² 范围内任一格不可走则净空失败。 */
        boolean blocked(int x, int y) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dx = -r; dx <= r; dx++) {
                    if (!passable(x + dx, y + dy)) return true;
                }
            }
            return false;
        }

        /** 单格可行走 (带缓存): 已加载 + 非墙/水/树/窗/关门 + 不在载具 AABB 内。 */
        private boolean passable(int x, int y) {
            long k = key(x, y);
            Boolean cached = passCache.get(k);
            if (cached != null) return cached;
            boolean ok = testSquare(x, y);
            passCache.put(k, ok);
            return ok;
        }

        private boolean testSquare(int x, int y) {
            IsoGridSquare sq = cell.getGridSquare(x, y, z);
            if (sq == null) return false;            // 未加载 = 阻挡 (滚动规划的边界)
            if (staticBlocked(sq)) return false;     // 静态阻挡 (含碰撞面族, 同一真相源)
            if (roadOnly && !isRoadSquare(sq)) return false; // 道路网模式: 只走道路格
            // 其他载具 AABB (已膨胀)
            float fx = x + 0.5f;
            float fy = y + 0.5f;
            for (float[] box : vehicleBoxes) {
                if (fx >= box[0] && fx <= box[2] && fy >= box[1] && fy <= box[3]) return false;
            }
            return true;
        }

        /** 直线可视: 步进 1 格采样, 全程核判定通过。 */
        boolean lineOfSight(int x0, int y0, int x1, int y1) {
            int dx = x1 - x0;
            int dy = y1 - y0;
            int steps = Math.max(Math.abs(dx), Math.abs(dy));
            for (int i = 1; i <= steps; i++) {
                int x = x0 + (dx * i) / steps;
                int y = y0 + (dy * i) / steps;
                if (blocked(x, y)) return false;
            }
            return true;
        }
    }
}
