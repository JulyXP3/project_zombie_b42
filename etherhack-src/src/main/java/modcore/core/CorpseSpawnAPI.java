/*
 * Red-team POC: arbitrary item spawn via forged corpse upload (multiplayer).
 *
 * Chain (verified against the running game jar, see
 * analysis/VHS获取经验与尸体物品伪造(已实施完成).md §五 — 原名"电台效果注入与尸体物品伪造",
 * df5c8810 改名, 内容未变):
 *   1. The client can construct an IsoDeadBody out of thin air (the
 *      IsoDeadBody(IsoCell) constructor needs no character) and fill its
 *      container with arbitrary InventoryItems.
 *   2. IsoGridSquare.addCorpse(body, bRemote=false) on the CLIENT sends an
 *      AddCorpseToMapPacket (handlingType=3, no anti-cheat) whose payload is
 *      the FULL IsoDeadBody serialization - including container.save() with
 *      all items (IsoDeadBody.java:884-905).
 *   3. The server parse: body = WorldItemTypes.createFromBuffer(b) +
 *      loadFromRemoteBuffer, then sq.addCorpse(body, true) - the corpse lands
 *      verbatim with the forged inventory. Zero validation.
 *   4. Loot the corpse (normal right-click search).
 *
 * GOTCHA: IsoDeadBody.save() (IsoDeadBody.java:876-881) throws
 * IllegalStateException("unhandled baseVisual class") when baseVisual == null.
 * The IsoDeadBody(IsoCell) constructor does NOT set baseVisual (only the
 * IsoGameCharacter one does, :319). Fix: reflectively inject
 * new HumanVisual(player) into the private baseVisual field.
 *
 * Note: this deliberately does NOT go through the zombie death flow - in MP
 * the server builds zombie corpses itself (NetworkZombiePacker calls die()
 * server-side and DoZombieInventory re-rolls), so filling a live zombie's
 * local inventory never reaches the server.
 *
 * Only for the user's own server / self-built test environment.
 */
package modcore.core;

import modcore.utils.Logger;
import se.krka.kahlua.integration.annotations.LuaMethod;
import zombie.characters.IsoPlayer;
import zombie.core.skinnedmodel.visual.HumanVisual;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoDeadBody;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class CorpseSpawnAPI {

    /** 单件数量上限 (与旧接口一致)。 */
    private static final int MAX_COUNT_PER_ITEM = 100;
    /** 一具尸体的总件数上限: 防 AddCorpseToMapPacket 体积失控。 */
    private static final int MAX_TOTAL_ITEMS = 300;

    /*
     * Forge a corpse at the player's feet carrying `count` copies of `itemType`
     * and upload it to the server. The corpse appears immediately and can be
     * searched like any other body. Throws RuntimeException on failure so the
     * Lua pcall gets the real reason (never silently return false).
     */
    @LuaMethod(name = "corpseSpawnPrepare", global = true)
    public static boolean corpseSpawnPrepare(String itemType, int count) {
        return corpseSpawnPrepareBatch(itemType + "=" + count);
    }

    /*
     * 三百零三 多选: 一具尸体装多种物品 (而不是造 N 具), 仍是同一条
     * AddCorpseToMapPacket。pairs 形如 "Base.Axe=2,Base.Nails=50"。
     * 任一字段非法/总量超上限直接抛错, 由 Lua 侧 pcall 展示真实原因。
     */
    @LuaMethod(name = "corpseSpawnPrepareBatch", global = true)
    public static boolean corpseSpawnPrepareBatch(String pairs) {
        IsoPlayer p = IsoPlayer.getInstance();
        if (p == null) throw new RuntimeException("no player");
        IsoGridSquare sq = p.getCurrentSquare();
        if (sq == null) throw new RuntimeException("no current square");

        List<String> types = new ArrayList<String>();
        List<Integer> counts = new ArrayList<Integer>();
        CorpseSpawnAPI.parseItemPairs(pairs, types, counts);

        List<InventoryItem> items = new ArrayList<InventoryItem>();
        int total = 0;
        for (int i = 0; i < types.size(); i++) {
            int count = counts.get(i);
            for (int k = 0; k < count; k++) {
                InventoryItem item = InventoryItemFactory.CreateItem(types.get(i));
                if (item == null) {
                    throw new RuntimeException("CreateItem failed for " + types.get(i));
                }
                items.add(item);
            }
            total += count;
        }

        // client-side corpse container; the server will re-read this from
        // the AddCorpseToMapPacket payload (container.save serialization)
        zombie.inventory.ItemContainer container = new zombie.inventory.ItemContainer("floor", sq, null);
        for (InventoryItem item : items) {
            container.AddItem(item);
        }

        IsoDeadBody body = new IsoDeadBody(IsoWorld.instance.getCell());
        // save() requires baseVisual (throws on null) - inject the player's visual
        try {
            Field f = IsoDeadBody.class.getDeclaredField("baseVisual");
            f.setAccessible(true);
            f.set(body, new HumanVisual(p));
        } catch (Throwable t) {
            Logger.printLog("[CorpseSpawn] baseVisual inject failed: " + t);
            throw new RuntimeException("baseVisual inject failed: " + t);
        }
        body.setX(p.getX() + 0.5f);
        body.setY(p.getY() + 0.5f);
        body.setZ(p.getZ());
        body.setSquare(sq);
        body.setCurrent(sq);
        body.setContainer(container);

        // bRemote=false + GameClient.client -> sends AddCorpseToMapPacket
        // with the full body serialization (container included)
        sq.addCorpse(body, false);
        Logger.printLog("[CorpseSpawn] forged corpse uploaded with " + total
                + " item(s) in " + types.size() + " type(s)");
        return true;
    }

    /** 解析 "Name=count,..." — 去空段, 校验名称/数量/总量 (供批量入口与单元测试共用)。 */
    static void parseItemPairs(String pairs, List<String> types, List<Integer> counts) {
        if (pairs == null || pairs.trim().isEmpty()) {
            throw new RuntimeException("empty item list");
        }
        int total = 0;
        for (String part : pairs.split(",")) {
            String seg = part.trim();
            if (seg.isEmpty()) {
                continue;
            }
            int eq = seg.indexOf('=');
            if (eq <= 0 || eq == seg.length() - 1) {
                throw new RuntimeException("bad entry: " + seg + " (expected Name=count)");
            }
            String type = seg.substring(0, eq).trim();
            int count;
            try {
                count = Integer.parseInt(seg.substring(eq + 1).trim());
            } catch (NumberFormatException e) {
                throw new RuntimeException("bad count: " + seg);
            }
            if (type.isEmpty() || count < 1 || count > MAX_COUNT_PER_ITEM) {
                throw new RuntimeException("bad entry: " + seg + " (count 1-" + MAX_COUNT_PER_ITEM + ")");
            }
            types.add(type);
            counts.add(count);
            total += count;
        }
        if (types.isEmpty()) {
            throw new RuntimeException("empty item list");
        }
        if (total > MAX_TOTAL_ITEMS) {
            throw new RuntimeException("total items " + total + " exceeds " + MAX_TOTAL_ITEMS);
        }
    }

    @LuaMethod(name = "corpseSpawnIsReady", global = true)
    public static boolean corpseSpawnIsReady() {
        return true;
    }

    @LuaMethod(name = "corpseSpawnIsDone", global = true)
    public static boolean corpseSpawnIsDone() {
        return true;
    }

    @LuaMethod(name = "corpseSpawnReset", global = true)
    public static void corpseSpawnReset() {
    }
}
