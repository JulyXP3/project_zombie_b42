/*
 * Red-team POC: arbitrary item spawn via fishing-rod exploit (multiplayer).
 *
 * Chain (verified against the running game jar):
 *   1. Client holds a fishing rod in primary hand, sends FishingAction
 *      (cast) via ActionManager.createFishingAction -> server starts a
 *      FishingAction and, ~85 ticks later, creates a server-side bobber.
 *   2. Client forges a FishingAction update packet with
 *      contentFlag = flagUpdateFish(4) | flagUpdateBobberParameters(8),
 *      carrying an arbitrary InventoryItem serialized by the client.
 *      The server parses it with NO validation.
 *   3. processServer() triggers OnFishingActionMPUpdate (because flag 8),
 *      and getLuaTable() attaches the client's fish/fishItem to the event.
 *   4. shared Bobber.onFishingActionMPUpdate overwrites the server bobber's
 *      fish.fishItem with the client item; flag 8 also sets
 *      catchFishStarted=true so the nibble timer cannot clear it.
 *   5. The server's FishingAction.update() then stores the item into
 *      fishForPickUp; ISPickupFishAction (server side) adds
 *      getPickedUpFish() to the player's inventory - no item-type check.
 *
 * Only for the user's own server / self-built test environment.
 */
package EtherHack.Ether;

import EtherHack.utils.FieldCache;
import EtherHack.utils.Logger;
import se.krka.kahlua.integration.annotations.LuaMethod;
import se.krka.kahlua.vm.KahluaTable;
import zombie.Lua.LuaManager;
import zombie.characters.IsoPlayer;
import zombie.core.ActionManager;
import zombie.core.FishingAction;
import zombie.core.network.ByteBufferWriter;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.iso.IsoGridSquare;
import zombie.network.GameClient;
import zombie.network.PacketTypes;
import zombie.network.packets.NetTimedActionPacket;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentLinkedQueue;

public class FishingSpawnAPI {
    private static byte actionId = 0;

    private static Field actionsField = null;
    private static Field idField = null;
    private static Field stateField = null;
    private static Field currentFishField = null;
    private static Field currentFishItemField = null;
    private static Field catchFishStartedField = null;

    private static void ensureFields() {
        Class<?> actionClass = FishingAction.class.getSuperclass();
        if (actionsField == null) actionsField = FieldCache.getField(ActionManager.class, "actions");
        if (idField == null) idField = FieldCache.getField(actionClass, "id");
        if (stateField == null) stateField = FieldCache.getField(actionClass, "state");
        if (currentFishField == null) currentFishField = FieldCache.getField(FishingAction.class, "currentFish");
        if (currentFishItemField == null) currentFishItemField = FieldCache.getField(FishingAction.class, "currentFishItem");
        if (catchFishStartedField == null) catchFishStartedField = FieldCache.getField(FishingAction.class, "catchFishStarted");
    }

    private static FishingAction findAction() {
        ensureFields();
        if (actionsField == null || idField == null) return null;
        try {
            Object queue = actionsField.get(null);
            if (!(queue instanceof ConcurrentLinkedQueue)) return null;
            for (Object o : (ConcurrentLinkedQueue<?>) queue) {
                if (!(o instanceof FishingAction)) continue;
                Object idValue = idField.get(o);
                if (idValue instanceof Byte && ((Byte) idValue).byteValue() == actionId) {
                    return (FishingAction) o;
                }
            }
        } catch (Throwable t) {
            Logger.printLog("[FishSpawn] findAction error: " + t.getMessage());
        }
        return null;
    }

    private static boolean isAccepted(FishingAction act) {
        if (stateField == null) return false;
        try {
            Object state = stateField.get(act);
            return state != null && state.toString().equals("Accept");
        } catch (Throwable t) {
            return false;
        }
    }

    private static void forceStateAccept(FishingAction act) {
        if (stateField == null) return;
        try {
            for (Object constant : stateField.getType().getEnumConstants()) {
                if (constant != null && constant.toString().equals("Accept")) {
                    stateField.set(act, constant);
                    return;
                }
            }
        } catch (Throwable t) {
            Logger.printLog("[FishSpawn] forceStateAccept error: " + t.getMessage());
        }
    }

    @LuaMethod(name = "fishSpawnCast", global = true)
    public static boolean fishSpawnCast() {
        try {
            IsoPlayer p = IsoPlayer.getInstance();
            if (p == null) return false;
            InventoryItem rod = p.getPrimaryHandItem();
            if (rod == null || rod.getScriptItem() == null || rod.getScriptItem().getWeaponSprite() == null) {
                Logger.printLog("[FishSpawn] cast: no fishing rod in primary hand");
                return false;
            }
            IsoGridSquare sq = p.getCurrentSquare();
            if (sq == null) return false;

            KahluaTable bobber = LuaManager.platform.newTable();
            bobber.rawset("catchFishStarted", Boolean.FALSE);

            byte id = ActionManager.getInstance().createFishingAction(p, rod, sq, bobber);
            if (id == 0) {
                Logger.printLog("[FishSpawn] cast: createFishingAction failed");
                return false;
            }
            actionId = id;
            Logger.printLog("[FishSpawn] cast sent, action id=" + id);
            return true;
        } catch (Throwable t) {
            Logger.printLog("[FishSpawn] cast error: " + t.getMessage());
            return false;
        }
    }

    @LuaMethod(name = "fishSpawnIsAccepted", global = true)
    public static boolean fishSpawnIsAccepted() {
        FishingAction act = findAction();
        return act != null && isAccepted(act);
    }

    @LuaMethod(name = "fishSpawnSpoof", global = true)
    public static boolean fishSpawnSpoof(String itemType) {
        try {
            FishingAction act = findAction();
            if (act == null) {
                Logger.printLog("[FishSpawn] spoof: action not found");
                return false;
            }
            // Client-side state may not reflect the server's Accept (setStateFromPacket
            // skips Accept echoes because their playerId is not serialized); force it so
            // write() emits an Accept update instead of a fresh Request.
            forceStateAccept(act);
            InventoryItem item = InventoryItemFactory.CreateItem(itemType);
            if (item == null) {
                Logger.printLog("[FishSpawn] spoof: CreateItem failed for " + itemType);
                return false;
            }
            ensureFields();
            if (currentFishField == null || currentFishItemField == null || catchFishStartedField == null) {
                Logger.printLog("[FishSpawn] spoof: reflection fields missing");
                return false;
            }

            KahluaTable fish = LuaManager.platform.newTable();
            fish.rawset("timer", 1.0);
            fish.rawset("dx", 0.0);
            fish.rawset("dy", 0.0);
            fish.rawset("isTrash", Boolean.FALSE);
            fish.rawset("fishSize", 1.0);
            fish.rawset("fishSizeLen", 1.0);
            fish.rawset("splashTimer", 0.0);

            currentFishField.set(act, fish);
            currentFishItemField.set(act, item);
            catchFishStartedField.set(act, true);
            act.contentFlag = (byte) (FishingAction.flagUpdateFish | FishingAction.flagUpdateBobberParameters);

            ByteBufferWriter bb = GameClient.connection.startPacket();
            PacketTypes.PacketType.FishingAction.doPacket(bb);
            act.write(bb);
            PacketTypes.PacketType.FishingAction.send(GameClient.connection);

            // reset so the client's own update loop does not re-send the payload
            act.contentFlag = 0;
            catchFishStartedField.set(act, false);

            Logger.printLog("[FishSpawn] spoofed item: " + itemType);
            return true;
        } catch (Throwable t) {
            Logger.printLog("[FishSpawn] spoof error: " + t.getMessage());
            return false;
        }
    }

    @LuaMethod(name = "fishSpawnPickup", global = true)
    public static boolean fishSpawnPickup(String itemType) {
        try {
            IsoPlayer p = IsoPlayer.getInstance();
            if (p == null) return false;
            InventoryItem rod = p.getPrimaryHandItem();
            InventoryItem item = InventoryItemFactory.CreateItem(itemType);
            if (rod == null || item == null) {
                Logger.printLog("[FishSpawn] pickup: need rod in hand and a valid item");
                return false;
            }
            // createNewAndSend(actionName, owner, ...values): owner is only used for
            // createNetTimedAction(playerId); the Lua constructor receives `values` as
            // (character, rod, fish). So the player must be repeated as the first value.
            NetTimedActionPacket.createNewAndSend("ISPickupFishAction", p, p, rod, item);
            Logger.printLog("[FishSpawn] pickup action sent for " + itemType);
            return true;
        } catch (Throwable t) {
            Logger.printLog("[FishSpawn] pickup error: " + t.getMessage());
            return false;
        }
    }

    @LuaMethod(name = "fishSpawnReset", global = true)
    public static void fishSpawnReset() {
        actionId = 0;
    }
}
