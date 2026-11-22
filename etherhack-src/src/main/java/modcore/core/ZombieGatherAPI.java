/*
 * Red-team POC: "gather nearby zombies" (troll feature, see
 * analysis/僵尸吸引与尸群聚集(已实施).md).
 *
 * In MP the server assigns zombie ownership to the connection of the player the
 * zombie targets (NetworkZombieManager.updateAuth, target-first). A local
 * (owned) zombie's position streams back to the server every 200 ms via
 * ZombieSimulation packets, and NetworkZombiePacker.parseZombie only checks
 * ownership - no distance/speed validation, no anti-cheat. So teleporting
 * locally-owned zombies on the client IS authoritative: the regular simulation
 * stream carries the new position up, other clients see the >3-unit teleport
 * snap via ZombieSynchronizationPacket.
 *
 * PRIMITIVE ONLY (one design rule): this API moves owned zombies toward the
 * CALLING player and sets their target to that same player. It never aims
 * zombies at third parties - that would be the deferred "horde command"
 * attack surface (analysis/僵尸控制与尸群指挥-研判(暂不实现).md).
 *
 * Landing rule (2026-11-20 user decision): every gathered zombie is put on the
 * SAME tile four tiles ahead of the player, along the facing direction - not a
 * ring around the player. Vanilla body collision then spreads the pile out;
 * that is the expected outcome and this API deliberately does no spacing.
 * See analysis/僵尸吸引与尸群聚集-前方定点落位(已实施-待实测).md.
 *
 * Known interaction: with「zombies don't attack me」enabled our own ASM patch
 * gates IsoZombie.setTarget, so the target refresh is suppressed and only the
 * position gather takes effect. Accepted by design.
 *
 * Only for the user's own server / self-built test environment.
 */
package modcore.core;

import modcore.utils.Logger;
import se.krka.kahlua.integration.annotations.LuaMethod;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.iso.IsoCell;

import java.util.ArrayList;
import java.util.List;

public class ZombieGatherAPI {

    /** 落点距离 (格): 玩家面朝方向正前方的这一格, 所有僵尸共用。 */
    private static final float GATHER_DISTANCE = 4.0f;
    /** 默认聚集半径 (格): Lua 层未传 radius 时的兜底。 */
    private static final float DEFAULT_RADIUS = 40.0f;

    /*
     * Teleport every locally-owned zombie within `radius` tiles onto one tile
     * four tiles ahead of the calling player, and point them at that player. Returns the
     * number gathered (0 = none owned - zombies must target you first; run the
     * sound lure to take ownership). Throws RuntimeException so the Lua pcall
     * surfaces the real reason.
     */
    @LuaMethod(name = "zombieGather", global = true)
    public static int zombieGather(Double radiusArg) {
        IsoPlayer p = IsoPlayer.getInstance();
        if (p == null) throw new RuntimeException("no player");
        float radius = (radiusArg == null || radiusArg <= 0 || radiusArg.isNaN()) ? DEFAULT_RADIUS : (float) (double) radiusArg;

        IsoCell cell = p.getCell();
        if (cell == null) throw new RuntimeException("no cell");
        ArrayList<IsoZombie> all = cell.getZombieList();
        if (all == null) throw new RuntimeException("no zombie list");

        float px = p.getX();
        float py = p.getY();
        int pz = (int) p.getZ();
        List<IsoZombie> owned = new ArrayList<IsoZombie>();
        for (int i = 0; i < all.size(); ++i) {
            IsoZombie z = all.get(i);
            if (z == null || z.isDead()) continue;
            // isLocal: SP 恒 true (无网络组件); MP = 模拟权在本连接
            if (!z.isLocal()) continue;
            float dx = z.getX() - px;
            float dy = z.getY() - py;
            if (dx * dx + dy * dy > radius * radius) continue;
            owned.add(z);
        }

        int count = owned.size();
        if (count == 0) return 0;

        // 定点落位: 所有僵尸去玩家面朝方向前方同一格, 由游戏体积碰撞挤开
        int[] target = ZombieGatherTarget.gatherTile(px, py,
                p.getForwardDirectionX(), p.getForwardDirectionY(), GATHER_DISTANCE);
        int gathered = 0;
        for (int i = 0; i < count; ++i) {
            IsoZombie z = owned.get(i);
            try {
                z.teleportTo(target[0], target[1], pz);
                // 目标刷新为玩家自己 (聚集后围上来); 「僵尸不会攻击玩家」
                // 开启时被自家补丁拦掉, 只剩位置生效 - 如实接受
                z.setTarget(p);
                ++gathered;
            } catch (Throwable t) {
                Logger.printLog("[ZombieGather] move failed: " + t);
            }
        }
        Logger.printLog("[ZombieGather] gathered " + gathered + "/" + count
                + " local zombies within " + radius + " tiles to tile "
                + target[0] + "," + target[1] + "," + pz + " (player " + px + "," + py + ")");
        return gathered;
    }

    /** 诊断: 当前可支配 (isLocal 且在半径内) 的僵尸数, 供面板状态行预判。 */
    @LuaMethod(name = "zombieGatherCount", global = true)
    public static int zombieGatherCount(Double radiusArg) {
        IsoPlayer p = IsoPlayer.getInstance();
        if (p == null) return -1;
        float radius = (radiusArg == null || radiusArg <= 0 || radiusArg.isNaN()) ? DEFAULT_RADIUS : (float) (double) radiusArg;
        IsoCell cell = p.getCell();
        if (cell == null) return -1;
        ArrayList<IsoZombie> all = cell.getZombieList();
        if (all == null) return -1;
        float px = p.getX();
        float py = p.getY();
        int n = 0;
        for (int i = 0; i < all.size(); ++i) {
            IsoZombie z = all.get(i);
            if (z == null || z.isDead() || !z.isLocal()) continue;
            float dx = z.getX() - px;
            float dy = z.getY() - py;
            if (dx * dx + dy * dy <= radius * radius) ++n;
        }
        return n;
    }
}
