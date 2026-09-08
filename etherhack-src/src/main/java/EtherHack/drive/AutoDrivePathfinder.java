/*
 * 单格静态阻挡判定真相源 (感知扫描共用)。
 *
 * 历史注记: 本类曾是自动驾驶的 BFS 车宽净空寻路器 (roadOnly 道路网模式 + 全地形
 * 兜底); 2026-09-06 路线来源切换为 RoadNetwork (大地图同源道路矢量, 整图一次可得)
 * 后, BFS/Kernel/道路格判定整体退役删除, 仅保留感知扫描依赖的 staticBlocked 判定。
 * 穿墙豁免 (BulletNoClipHook) 激活期车辆对静态物可穿, 路线不再依赖地块可达性。
 */
package EtherHack.drive;

import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoWindow;

public final class AutoDrivePathfinder {

    private AutoDrivePathfinder() {
    }

    /**
     * 单格静态阻挡判定 (感知扫描 / 静态障碍距离共用同一真相源):
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
        if (sq == null) return true;             // 未加载 = 阻挡 (扫描边界)
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
}
