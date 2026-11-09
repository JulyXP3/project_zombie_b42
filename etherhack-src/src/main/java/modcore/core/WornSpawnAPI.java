/*
 * Red-team POC: worn-item ghost extraction (multiplayer, own test server only).
 *
 * Chain (verified against the B42.22 decompile / bytecode):
 *   SyncClothingPacket (handlingType=3) -> process() :186-205 creates any
 *   itemType via InventoryItemFactory.CreateItem and puts it in the worn slot
 *   (no container, no type check).
 *   Then IsoGameCharacter.setWornItem(loc, item, forceDropTooHeavy) :8031-8151
 *   (b42.22) drops the previous worn item to the ground when
 *   forceDropTooHeavy && itemCur != null && IsoPlayer && !inventory.hasRoomFor
 *   && !GameClient.client  -> performDropItemOnSquare -> AddWorldInventoryItem.
 *   ISWearClothing:complete :101-131 calls setWornItem(bodyLocation, item)
 *   (2-arg wrapper = forceDropTooHeavy true) and runs on the server
 *   (LuaTimedActionNew:165-166 skips complete on clients).
 *
 * Weight gate: the inventory must be over capacity at the moment the wear
 * action completes. Instead of stuffing junk, temporarily inflate ONE food
 * item's weight via ItemStatsPacket (handlingType=3; processServer ->
 * applyItemStats -> food.setHungChange / setActualWeight, no validation).
 * Food.getActualWeight() computes from hungChange / script values, so the
 * helper below picks the right field and amplifies until the computed weight
 * reaches TARGET_WEIGHT (60), then restores afterwards.
 *
 * Only for the user's own server / self-built test environment.
 */
package modcore.core;

import java.util.Random;
import se.krka.kahlua.integration.LuaReturn;
import se.krka.kahlua.integration.annotations.LuaMethod;
import se.krka.kahlua.vm.KahluaTable;
import zombie.Lua.LuaManager;
import zombie.characters.IsoPlayer;
import zombie.core.ActionManager;
import zombie.core.raknet.UdpConnection;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.types.Food;
import zombie.iso.IsoGridSquare;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.network.GameClient;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.ItemBodyLocation;
import modcore.utils.Logger;

public class WornSpawnAPI {

    /** 目标临时重量: 60 足以压过常规背包容量, 又不至于把食物数值改得离谱。 */
    static final float TARGET_WEIGHT = 60.0f;
    /** 穿衣动作 getDuration=50 tick ≈ 1s; 留足余量。 */
    static final long WEAR_TIMEOUT_MS = 6000L;

    private static boolean active = false;
    private static long startMs = 0L;
    private static int ghostId = -1;
    private static ItemBodyLocation ghostLoc = null;
    private static String ghostType = null;
    private static int foodId = -1;
    private static IsoPlayer cyclePlayer = null;
    private static UdpConnection cycleConnection = null;
    private static Food cycleFood = null;
    private static int triggerId = -1;
    private static IsoGridSquare dropSquare = null;
    private static KahluaTable wearAction = null;
    private static byte actionId = 0;
    private static boolean weightSent = false;
    private static boolean clothingSent = false;
    private static boolean weightRestored = false;
    private static int lastReceipt = -1;
    private static boolean lastGhostStillWorn = true;
    private static float savedHunger = 0.0f;
    private static float savedThirst = 0.0f;
    private static float savedActualWeight = 0.0f;

    /*
     * Start one extraction cycle:
     *   1. pick a clean wearable in the backpack (its body slot must be empty)
     *   2. pick any food and inflate it to ~60 (sent upstream)
     *   3. forge a worn ghost of `itemType` in that slot (SyncClothing upstream)
     *   4. send an ISWearClothing net action for the real wearable; its
     *      server-side complete() replaces the ghost, which then drops.
     * Returns a status code (see WornSpawnAPI.statusKey in Lua).
     */
    @LuaMethod(name = "wornSpawnStart", global = true)
    public static String wornSpawnStart(String itemType) {
        try {
            // 客户端连接的就绪标志是 isReady()（IsoPlayer.setReady(true)，原版 syncClothing 同款）；
            // isFullyConnected() 只在服务端置位，客户端恒为 false，用它判断会一直 not-mp。
            if (!GameClient.client || GameClient.connection == null || !GameClient.connection.isReady()) {
                return "not-mp";
            }
            IsoPlayer player = IsoPlayer.getInstance();
            if (player == null) {
                return "no-player";
            }
            if (active) {
                return "waiting";
            }
            String fullType = itemType == null ? "" : itemType.trim();
            if (fullType.isEmpty()) {
                return "bad-item";
            }
            InventoryItem probe = InventoryItemFactory.CreateItem(fullType);
            if (probe == null) {
                return "bad-item";
            }

            InventoryItem trigger = findCleanWearable(player);
            if (trigger == null) {
                return "no-cloth";
            }
            Food food = findFood(player, trigger);
            if (food == null) {
                return "no-food";
            }

            // 构造函数需要 character 和 item；事务 owner 不会自动传给 Lua。
            Object actionClass = LuaManager.get("ISWearClothing");
            Object constructor = LuaManager.getFunctionObject("ISWearClothing.new");
            LuaReturn result = LuaManager.caller.protectedCall(LuaManager.thread, constructor,
                    actionClass, player, trigger);
            if (!result.isSuccess() || !(result.getFirst() instanceof KahluaTable action)
                    || action.rawget("character") != player || action.rawget("item") != trigger) {
                Logger.printLog("[WornSpawn] invalid wear action: " + result);
                return "send-failed";
            }
            IsoGridSquare square = player.getCurrentSquare();
            if (square == null) {
                return "no-player";
            }

            savedHunger = food.getHungChange();
            savedThirst = food.getThirstChange();
            savedActualWeight = food.getActualWeightUnmodded();
            cyclePlayer = player;
            cycleConnection = GameClient.connection;
            cycleFood = food;
            foodId = food.getID();
            triggerId = trigger.getID();
            dropSquare = square;
            wearAction = action;
            ghostType = fullType;
            if (!inflateFood(food)) {
                finish("food-weight", true);
                return "food-weight";
            }
            weightSent = true;
            INetworkPacket.send(PacketTypes.PacketType.ItemStats, player.getInventory(), food);

            // 保留全部真穿戴项，只在空部位挂本次幽灵。
            int newGhostId = randomGhostId(player);
            InventoryItem ghost = InventoryItemFactory.CreateItem(fullType);
            if (ghost == null) {
                finish("bad-item", true);
                return "bad-item";
            }
            ghost.setID(newGhostId);
            ghostId = newGhostId;
            ghostLoc = trigger.getBodyLocation();
            player.getWornItems().setItem(ghostLoc, ghost);
            clothingSent = true;
            INetworkPacket.send(PacketTypes.PacketType.SyncClothing, player);

            // 与原版网络动作相同的发送入口；保留动作表供回执与取消使用。
            actionId = ActionManager.getInstance().createNetTimedAction(player, wearAction);
            if (actionId == 0) {
                finish("send-failed", true);
                return "send-failed";
            }
            active = true;
            startMs = System.currentTimeMillis();
            Logger.printLog("[WornSpawn] started " + fullType + " id=" + ghostId + " at " + ghostLoc
                    + " action=" + actionId + " trigger=" + trigger.getFullType()
                    + " food=" + food.getFullType() + " weight=" + food.getActualWeight());
            return "started";
        } catch (Throwable t) {
            Logger.printLog("[WornSpawn] start error: " + t);
            finish("error", true);
            return "error";
        }
    }

    /*
     * Server completion, clothing sync and ground sync can arrive separately.
     * A vanished ghost alone does not prove a drop.
     */
    @LuaMethod(name = "wornSpawnPoll", global = true)
    public static String wornSpawnPoll() {
        try {
            if (!active) {
                return "idle";
            }
            if (!sameSession()) {
                return finish("no-player", true) ? "no-player" : "error";
            }
            InventoryItem worn = cyclePlayer.getWornItem(ghostLoc);
            boolean ghostStillWorn = worn != null && worn.getID() == ghostId;
            boolean triggerWorn = worn != null && worn.getID() == triggerId;
            int receipt = TimedActionReceipt.state(wearAction);
            boolean dropped = hasDroppedItem();
            if (receipt != lastReceipt || ghostStillWorn != lastGhostStillWorn) {
                Logger.printLog("[WornSpawn] action=" + actionId + " receipt=" + receipt
                        + " ghost-worn=" + ghostStillWorn + " trigger-worn=" + triggerWorn
                        + " dropped=" + dropped);
                lastReceipt = receipt;
                lastGhostStillWorn = ghostStillWorn;
            }
            if (receipt == 2) {
                return finish("rejected", true) ? "rejected" : "error";
            }
            if (receipt == 1 || triggerWorn || dropped) {
                restoreFood();
            }
            if (triggerWorn && dropped) {
                return finish("done", false) ? "done" : "error";
            }
            if (System.currentTimeMillis() - startMs > WEAR_TIMEOUT_MS) {
                return finish("timeout", true) ? "timeout" : "error";
            }
            return receipt == 1 || !ghostStillWorn || dropped ? "waiting-drop" : "waiting";
        } catch (Throwable t) {
            Logger.printLog("[WornSpawn] poll error: " + t);
            finish("error", true);
            return "error";
        }
    }

    /** Manual, timeout and disconnect cleanup all use the same owned-cycle path. */
    @LuaMethod(name = "wornSpawnRestore", global = true)
    public static String wornSpawnRestore() {
        return finish("restored", true) ? "restored" : "error";
    }

    @LuaMethod(name = "wornSpawnStatus", global = true)
    public static String wornSpawnStatus() {
        if (active && !sameSession()) {
            return finish("no-player", true) ? "no-player" : "error";
        }
        if (!active) {
            return "idle";
        }
        return "waiting";
    }

    /** 纯函数: 为达到目标重量, 饥饿/口渴字段应取的初始变化值 (供测试)。 */
    public static float initialChangeForTarget(float scriptChange, float scriptWeight, float emptyWeight, float target) {
        if (scriptChange == 0.0f || scriptWeight <= 0.0001f) {
            return -10.0f;
        }
        float usedDelta;
        if (emptyWeight >= 0.0f) {
            float denom = scriptWeight - emptyWeight;
            if (denom <= 0.0001f) {
                return -10.0f;
            }
            usedDelta = (target - emptyWeight) / denom;
        } else {
            usedDelta = target / scriptWeight;
        }
        float change = usedDelta * scriptChange / 100.0f;
        return change == 0.0f ? -10.0f : change;
    }

    private static InventoryItem findCleanWearable(IsoPlayer player) {
        for (InventoryItem item : player.getInventory().getItems()) {
            ItemBodyLocation loc = item.getBodyLocation();
            if (loc == null) {
                continue;
            }
            if (player.getWornItem(loc) != null) {
                continue;   // 该部位已有真衣服, 不抢 (否则真衣服会被顶掉)
            }
            boolean exclusive = false;
            var wornItems = player.getWornItems();
            var group = wornItems.getBodyLocationGroup();
            if (group.getLocation(loc) != null) {
                for (int i = 0; i < wornItems.size(); i++) {
                    InventoryItem worn = wornItems.getItemByIndex(i);
                    ItemBodyLocation wornLoc = wornItems.getLocation(worn);
                    if (wornLoc == null || group.getLocation(wornLoc) == null) {
                        continue;
                    }
                    if (group.isExclusive(loc, wornLoc)) {
                        exclusive = true;
                        break;
                    }
                }
            }
            if (exclusive) {
                continue;
            }
            if (!(item instanceof zombie.inventory.types.Clothing)) {
                continue;
            }
            return item;
        }
        return null;
    }

    private static Food findFood(IsoPlayer player, InventoryItem trigger) {
        for (InventoryItem item : player.getInventory().getItems()) {
            if (item == trigger) {
                continue;
            }
            if (item instanceof Food) {
                return (Food)item;
            }
        }
        return null;
    }

    /*
     * Food.getActualWeight() 的分支 (b42.22 Food.java:777-820):
     *   - haveExtraItems: (实际重量 + 额外重量 - 空容器重量) * usedDelta + 空容器重量
     *   - ReplaceOnUse 且非 CustomWeight: (脚本重量 - 空容器重量) * usedDelta + 空容器重量
     *   - 非 CustomWeight: 脚本重量 * usedDelta
     *   - 其它: actualWeight 字段
     * usedDelta = 饥饿变化 * 100 / 脚本饥饿变化 (饥饿不为负时改用口渴)。
     * 先按公式给初值, 再按实测重量放大, 兼容各分支; 字段权重食物直接写字段。
     */
    private static boolean inflateFood(Food food) {
        Item script = food.getScriptItem();
        if (script == null) {
            return false;
        }
        float scriptHunger = script.getHungerChange();
        float scriptThirst = script.getThirstChange();
        boolean useHunger = scriptHunger < 0.0f;
        boolean useThirst = !useHunger && scriptThirst < 0.0f;
        if (!useHunger && !useThirst) {
            return false;
        }
        float scriptChange = useHunger ? scriptHunger : scriptThirst;
        float scriptWeight = script.getActualWeight();
        float emptyWeight = -1.0f;
        String replace = food.getReplaceOnUseFullType();
        if (replace != null && !replace.isEmpty()) {
            Item empty = ScriptManager.instance.FindItem(replace);
            if (empty != null) {
                emptyWeight = empty.getActualWeight();
            }
        }
        float value = initialChangeForTarget(scriptChange, scriptWeight, emptyWeight, TARGET_WEIGHT);
        if (useHunger) {
            food.setHungChange(value);
        } else {
            food.setThirstChange(value);
        }
        float weight = food.getActualWeight();
        for (int i = 0; i < 8 && weight < TARGET_WEIGHT; ++i) {
            float current = useHunger ? food.getHungChange() : food.getThirstChange();
            float next = current * 10.0f;
            if (useHunger) {
                food.setHungChange(next);
            } else {
                food.setThirstChange(next);
            }
            float after = food.getActualWeight();
            if (Math.abs(after - weight) < 0.01f) {
                break;
            }
            weight = after;
        }
        if (weight < TARGET_WEIGHT) {
            // CustomWeight 食物走 actualWeight 字段
            food.setActualWeight(TARGET_WEIGHT);
            weight = food.getActualWeight();
        }
        return weight >= TARGET_WEIGHT;
    }

    private static void restoreLocalFood(Food food) {
        if (food == null) {
            return;
        }
        food.setHungChange(savedHunger);
        food.setThirstChange(savedThirst);
        food.setActualWeight(savedActualWeight);
    }

    private static boolean sameSession() {
        return GameClient.client && cycleConnection != null && GameClient.connection == cycleConnection
                && cycleConnection.isReady()
                && cyclePlayer != null && IsoPlayer.getInstance() == cyclePlayer;
    }

    private static boolean hasDroppedItem() {
        if (dropSquare == null || ghostType == null) {
            return false;
        }
        IsoGridSquare current = cyclePlayer.getCurrentSquare();
        if (hasDroppedItem(current)) {
            dropSquare = current;
            return true;
        }
        return current != dropSquare && hasDroppedItem(dropSquare);
    }

    private static boolean hasDroppedItem(IsoGridSquare origin) {
        if (origin == null) {
            return false;
        }
        for (int z = origin.getZ(); z >= 0; z--) {
            IsoGridSquare square = origin.getCell().getGridSquare(origin.getX(), origin.getY(), z);
            if (square == null) {
                continue;
            }
            for (IsoWorldInventoryObject world : square.getWorldObjects()) {
                InventoryItem item = world.getItem();
                if (item != null && item.getID() == ghostId && ghostType.equals(item.getFullType())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void restoreFood() {
        if (weightRestored || cycleFood == null) {
            return;
        }
        Food food = cycleFood;
        if (cyclePlayer != null) {
            InventoryItem item = cyclePlayer.getInventory().getItemWithIDRecursiv(foodId);
            if (item instanceof Food current) {
                food = current;
            }
        }
        restoreLocalFood(cycleFood);
        if (food != cycleFood) {
            restoreLocalFood(food);
        }
        if (weightSent && sameSession() && food.getContainer() != null) {
            INetworkPacket.send(PacketTypes.PacketType.ItemStats, food.getContainer(), food);
        }
        weightRestored = true;
    }

    private static boolean finish(String status, boolean cancel) {
        if (cyclePlayer == null) {
            active = false;
            return true;
        }
        boolean clean = true;
        if (cancel && actionId != 0 && sameSession()) {
            try {
                INetworkPacket.send(PacketTypes.PacketType.GeneralAction, actionId, cyclePlayer);
            } catch (Throwable failure) {
                Logger.printLog("[WornSpawn] action cleanup error: " + failure);
                clean = false;
            }
        }
        try {
            TimedActionReceipt.remove(wearAction);
        } catch (Throwable failure) {
            Logger.printLog("[WornSpawn] receipt cleanup error: " + failure);
            clean = false;
        }
        try {
            if (ghostLoc != null) {
                InventoryItem worn = cyclePlayer.getWornItem(ghostLoc);
                if (worn != null && worn.getID() == ghostId && ghostType.equals(worn.getFullType())) {
                    cyclePlayer.getWornItems().remove(worn);
                    if (clothingSent && sameSession()) {
                        INetworkPacket.send(PacketTypes.PacketType.SyncClothing, cyclePlayer);
                    }
                }
            }
        } catch (Throwable failure) {
            Logger.printLog("[WornSpawn] clothing cleanup error: " + failure);
            clean = false;
        }
        try {
            restoreFood();
        } catch (Throwable failure) {
            Logger.printLog("[WornSpawn] weight cleanup error: " + failure);
            clean = false;
        }
        Logger.printLog("[WornSpawn] finished " + status + " action=" + actionId
                + " receipt=" + lastReceipt + " cleanup=" + clean);
        active = false;
        startMs = 0;
        ghostId = -1;
        ghostLoc = null;
        ghostType = null;
        foodId = -1;
        cyclePlayer = null;
        cycleConnection = null;
        cycleFood = null;
        triggerId = -1;
        dropSquare = null;
        wearAction = null;
        actionId = 0;
        weightSent = false;
        clothingSent = false;
        weightRestored = false;
        lastReceipt = -1;
        lastGhostStillWorn = true;
        return clean;
    }

    private static int randomGhostId(IsoPlayer player) {
        Random random = new Random();
        for (int i = 0; i < 64; ++i) {
            int id = 0x20000000 + random.nextInt(0x10000000);
            if (id == 0) {
                continue;
            }
            if (player.getInventory().getItemWithID(id) != null) {
                continue;
            }
            if (player.getWornItems().getItemById(id) != null) {
                continue;
            }
            return id;
        }
        return 0x2FFFFFFF - (int)(System.nanoTime() & 0xFFFF);
    }
}
