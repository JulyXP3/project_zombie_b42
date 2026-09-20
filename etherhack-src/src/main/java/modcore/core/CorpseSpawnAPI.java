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

    /*
     * Forge a corpse at the player's feet carrying `count` copies of `itemType`
     * and upload it to the server. The corpse appears immediately and can be
     * searched like any other body. Throws RuntimeException on failure so the
     * Lua pcall gets the real reason (never silently return false).
     */
    @LuaMethod(name = "corpseSpawnPrepare", global = true)
    public static boolean corpseSpawnPrepare(String itemType, int count) {
        IsoPlayer p = IsoPlayer.getInstance();
        if (p == null) throw new RuntimeException("no player");
        IsoGridSquare sq = p.getCurrentSquare();
        if (sq == null) throw new RuntimeException("no current square");
        if (itemType == null || itemType.isEmpty() || count < 1 || count > 100) {
            throw new RuntimeException("bad args: item=" + itemType + " count=" + count);
        }

        List<InventoryItem> items = new ArrayList<InventoryItem>(count);
        for (int i = 0; i < count; i++) {
            InventoryItem item = InventoryItemFactory.CreateItem(itemType);
            if (item == null) {
                throw new RuntimeException("CreateItem failed for " + itemType);
            }
            items.add(item);
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
        Logger.printLog("[CorpseSpawn] forged corpse uploaded with "
                + count + "x " + itemType);
        return true;
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
