package modcore.core;

import se.krka.kahlua.integration.LuaReturn;
import se.krka.kahlua.integration.annotations.LuaMethod;
import se.krka.kahlua.vm.KahluaTable;
import zombie.Lua.LuaManager;
import zombie.characters.IsoPlayer;
import zombie.core.ActionManager;
import zombie.core.NetTimedAction;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.network.GameClient;
import zombie.network.PacketTypes;
import zombie.network.packets.SyncItemFieldsPacket;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehiclePart;
import modcore.utils.Logger;

/** 单零件、单在途动作; 原版回执和物品归属分别确认, 未知状态不重发。 */
public final class VehicleRepairAPI {
    private static final int TARGET_CONDITION = 95;
    private static final VehicleRepairState state = new VehicleRepairState();
    private static IsoPlayer cyclePlayer;
    private static UdpConnection cycleConnection;
    private static InventoryItem preparedItem;

    private VehicleRepairAPI() {
    }

    private static boolean refreshSession() {
        IsoPlayer player = IsoPlayer.getInstance();
        UdpConnection connection = GameClient.connection;
        if (!GameClient.client || player == null || connection == null || !connection.isReady()) {
            player = null;
            connection = null;
        }
        if (cyclePlayer != player || cycleConnection != connection) {
            cyclePlayer = player;
            cycleConnection = connection;
            preparedItem = null;
        }
        state.connect(player, connection);
        return player != null;
    }

    /** 背包维护与手持维护共用此门, 不切换用户开关; 重新进服才丢弃旧会话隔离。 */
    public static boolean isItemReserved(InventoryItem item) {
        return refreshSession() && item != null && state.blocks(item.getID(), item.getFullType());
    }

    @LuaMethod(name = "vehicleRepairSessionState", global = true)
    public static String vehicleRepairSessionState() {
        if (!refreshSession()) return "offline";
        return state.isQuarantined() ? "quarantined" : state.isClaimed() ? "busy" : "ready";
    }

    @LuaMethod(name = "vehicleRepairClaim", global = true)
    public static String vehicleRepairClaim(VehiclePart part) {
        if (!refreshSession()) return "not-mp";
        if (part == null || part.getInventoryItem() == null || !isCurrentVehicle(part)) return "bad-args";
        if ("GasTank".equals(part.getId()) || "lightbar".equals(part.getId()) || part.getTable("install") == null) {
            return "unsupported";
        }
        InventoryItem item = part.getInventoryItem();
        if (part.getCondition() >= TARGET_CONDITION || item.getConditionMax() < TARGET_CONDITION) return "unsupported";
        if (!state.claim(part, item.getID(), item.getFullType())) return "busy";
        preparedItem = null;
        return "ready";
    }

    @LuaMethod(name = "vehicleRepairRelease", global = true)
    public static String vehicleRepairRelease() {
        if (!refreshSession()) return "released";
        if (!state.release()) return "pending";
        preparedItem = null;
        return "released";
    }

    @LuaMethod(name = "vehicleRepairQuarantine", global = true)
    public static void vehicleRepairQuarantine() {
        if (refreshSession()) state.quarantine();
    }

    @LuaMethod(name = "vehicleRepairActionStatus", global = true)
    public static String vehicleRepairActionStatus() {
        if (!refreshSession()) return "offline";
        if (state.status() == VehicleRepairState.Status.IDLE) return "idle";
        try {
            NetTimedAction action = ActionManager.getAction(state.actionId(), cyclePlayer);
            boolean matches = action != null && action.action == state.action();
            return state.observeAction(matches ? action.action : null,
                    matches && ActionManager.isDone(cyclePlayer, state.actionId()),
                    matches && ActionManager.isRejected(cyclePlayer, state.actionId())).code;
        } catch (Throwable t) {
            Logger.error("[VehRepair] action receipt unavailable", t);
            state.quarantine();
            return "missing";
        }
    }

    /** 仅在 Lua 已核对物品归属后移除本次动作; 不触碰其它功能或玩家的动作。 */
    @LuaMethod(name = "vehicleRepairFinishAction", global = true)
    public static String vehicleRepairFinishAction() {
        if (!refreshSession()) return "offline";
        String status = vehicleRepairActionStatus();
        if (!"done".equals(status) && !"rejected".equals(status)) return "pending";
        if (state.isQuarantined()) return "pending";
        KahluaTable action = (KahluaTable) state.action();
        TimedActionReceipt.remove(action);
        return state.completeAction() ? "ready" : "pending";
    }

    @LuaMethod(name = "vehicleRepairSendUninstall", global = true)
    public static String vehicleRepairSendUninstall(VehiclePart part, float workTime) {
        if (!refreshSession()) return "not-mp";
        InventoryItem item = part == null ? null : part.getInventoryItem();
        if (!owns(part, item) || !isCurrentVehicle(part)) return "bad-args";
        return sendAction("ISUninstallVehiclePart", part, null, workTime);
    }

    @LuaMethod(name = "vehicleRepairSendInstall", global = true)
    public static String vehicleRepairSendInstall(VehiclePart part, InventoryItem item, float maxTime) {
        if (!refreshSession()) return "not-mp";
        if (!owns(part, item) || !isCurrentVehicle(part) || part.getInventoryItem() != null) return "bad-args";
        if (!inRootInventory(item) || item != preparedItem || item.getCondition() != TARGET_CONDITION) return "bad-item";
        preparedItem = null;
        return sendAction("ISInstallVehiclePart", part, item, maxTime);
    }

    private static boolean isCurrentVehicle(VehiclePart part) {
        BaseVehicle vehicle = part == null ? null : part.getVehicle();
        return vehicle != null && cyclePlayer != null && cyclePlayer.getVehicle() == vehicle;
    }

    private static boolean owns(VehiclePart part, InventoryItem item) {
        return item != null && state.owns(part, item.getID(), item.getFullType());
    }

    private static boolean inRootInventory(InventoryItem item) {
        ItemContainer inventory = cyclePlayer == null ? null : cyclePlayer.getInventory();
        return item != null && inventory != null && item.getContainer() == inventory
                && inventory.getItemWithID(item.getID()) == item;
    }

    private static String sendAction(String type, VehiclePart part, InventoryItem item, float time) {
        if (state.status() != VehicleRepairState.Status.IDLE || state.isQuarantined()) return "busy";
        if (!Float.isFinite(time) || time <= 0.0f) return "bad-args";
        try {
            Object actionClass = LuaManager.get(type);
            Object constructor = LuaManager.getFunctionObject(type + ".new");
            Object[] args = item == null
                    ? new Object[]{actionClass, cyclePlayer, part, Float.valueOf(time)}
                    : new Object[]{actionClass, cyclePlayer, part, item, Float.valueOf(time)};
            LuaReturn result = LuaManager.caller.protectedCall(LuaManager.thread, constructor, args);
            if (!result.isSuccess() || !(result.getFirst() instanceof KahluaTable action)
                    || action.rawget("character") != cyclePlayer || action.rawget("part") != part
                    || (item != null && action.rawget("item") != item)) return "bad-action";
            byte id = ActionManager.getInstance().createNetTimedAction(cyclePlayer, action);
            if (id == 0 || !state.startAction(id, action)) {
                state.quarantine();
                return "error";
            }
            return "sent";
        } catch (Throwable t) {
            state.quarantine();
            Logger.error("[VehRepair] action send failed", t);
            return "error";
        }
    }

    /** 只处理当前保留的根背包零件; 返回失败时调用方不得继续安装。 */
    @LuaMethod(name = "vehicleRepairSyncItemCondition", global = true)
    public static String vehicleRepairSyncItemCondition(InventoryItem item, int condition) {
        if (!refreshSession()) return "not-mp";
        if (state.status() != VehicleRepairState.Status.IDLE || state.isQuarantined()) return "busy";
        if (item == null || !state.blocks(item.getID(), item.getFullType())
                || condition != TARGET_CONDITION || item.getConditionMax() < condition) return "bad-args";
        if (!inRootInventory(item)) return "no-container";
        boolean packetOpen = false;
        try {
            item.setCondition(condition);
            SyncItemFieldsPacket packet = new SyncItemFieldsPacket();
            packet.setData(cyclePlayer, item);
            if (!packet.isConsistent(cycleConnection)) return "bad-item";
            ByteBufferWriter writer = cycleConnection.startPacket();
            packetOpen = true;
            PacketTypes.PacketType.SyncItemFields.doPacket(writer);
            packet.write(writer);
            PacketTypes.PacketType.SyncItemFields.send(cycleConnection);
            packetOpen = false;
            preparedItem = item;
            return "sent";
        } catch (Throwable t) {
            if (packetOpen) cycleConnection.cancelPacket();
            state.quarantine();
            Logger.error("[VehRepair] item condition sync failed", t);
            return "error";
        }
    }
}
