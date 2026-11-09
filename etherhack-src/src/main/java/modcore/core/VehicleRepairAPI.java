/*
 * 载具修理链路: 直接发送原版网络计时动作 + 物品耐久同步。
 *
 * 为什么不用客户端 Lua 动作:
 *   ISInstallVehiclePart:isValid 要求物品在玩家背包, start() 还会把物品重绑到背包;
 *   ISUninstallVehiclePart:isValid 要求部件已装且 canUninstallPart 通过。
 *   服务端对网络计时动作只做 isConsistent + character 身份校验, 不调用 isValid
 *   (ActionManager.start 不调, NetTimedAction.isValid 恒 false), 所以直接构造并发送
 *   动作表即可, 不排本地动作队列。
 *
 * 物品耐久同步:
 *   SyncItemFieldsPacket 是客户端可发的包(LoginOnServer, 无 anticheat), 服务端
 *   processServer 直接 item.setCondition(包里的值), 只校验容器能解析、物品能找到。
 *   拆下的零件在玩家背包里(可解析容器), 因此可以把耐久拉满再装回。
 */
package modcore.core;

import se.krka.kahlua.integration.annotations.LuaMethod;
import zombie.characters.IsoPlayer;
import zombie.inventory.InventoryItem;
import zombie.network.GameClient;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;
import zombie.network.packets.NetTimedActionPacket;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehiclePart;
import modcore.utils.Logger;

public final class VehicleRepairAPI {

    private VehicleRepairAPI() {
    }

    /** 发送原版拆卸动作; 返回 sent / not-mp / bad-args / no-vehicle / error。 */
    @LuaMethod(name = "vehicleRepairSendUninstall", global = true)
    public static String vehicleRepairSendUninstall(VehiclePart part, float workTime) {
        try {
            if (!GameClient.client || GameClient.instance == null) {
                return "not-mp";
            }
            IsoPlayer player = IsoPlayer.getInstance();
            if (player == null || part == null) {
                return "bad-args";
            }
            BaseVehicle vehicle = part.getVehicle();
            if (vehicle == null) {
                return "no-vehicle";
            }
            NetTimedActionPacket.createNewAndSend("ISUninstallVehiclePart", player,
                player, part, Float.valueOf(workTime));
            return "sent";
        }
        catch (Throwable t) {
            Logger.error("[VehRepair] uninstall send failed", t);
            return "error";
        }
    }

    /** 发送原版安装动作(part, item 都由服务端按 id 解析); 返回同上。 */
    @LuaMethod(name = "vehicleRepairSendInstall", global = true)
    public static String vehicleRepairSendInstall(VehiclePart part, InventoryItem item, float maxTime) {
        try {
            if (!GameClient.client || GameClient.instance == null) {
                return "not-mp";
            }
            IsoPlayer player = IsoPlayer.getInstance();
            if (player == null || part == null || item == null) {
                return "bad-args";
            }
            BaseVehicle vehicle = part.getVehicle();
            if (vehicle == null) {
                return "no-vehicle";
            }
            NetTimedActionPacket.createNewAndSend("ISInstallVehiclePart", player,
                player, part, item, Float.valueOf(maxTime));
            return "sent";
        }
        catch (Throwable t) {
            Logger.error("[VehRepair] install send failed", t);
            return "error";
        }
    }

    /** 灯光条: 原版 ISRepairLightbar(服务端只接受 lightbar 部件, 不看技能); item 只当过账件, 不消耗。 */
    @LuaMethod(name = "vehicleRepairSendLightbar", global = true)
    public static String vehicleRepairSendLightbar(VehiclePart part, InventoryItem item, float maxTime) {
        try {
            if (!GameClient.client || GameClient.instance == null) {
                return "not-mp";
            }
            IsoPlayer player = IsoPlayer.getInstance();
            if (player == null || part == null || item == null) {
                return "bad-args";
            }
            BaseVehicle vehicle = part.getVehicle();
            if (vehicle == null) {
                return "no-vehicle";
            }
            NetTimedActionPacket.createNewAndSend("ISRepairLightbar", player,
                player, part, item, Float.valueOf(maxTime));
            return "sent";
        }
        catch (Throwable t) {
            Logger.error("[VehRepair] lightbar send failed", t);
            return "error";
        }
    }

    /**
     * 把物品耐久写成目标值并发 SyncItemFields; 服务端照抄。
     * 物品必须在一个可解析容器里(玩家背包/载具容器/地上), 否则服务端解析不到容器会丢弃。
     * 返回 sent / bad-args / no-container / error。
     */
    @LuaMethod(name = "vehicleRepairSyncItemCondition", global = true)
    public static String vehicleRepairSyncItemCondition(InventoryItem item, int condition) {
        try {
            if (item == null) {
                return "bad-args";
            }
            if (item.getContainer() == null) {
                return "no-container";
            }
            item.setCondition(condition);
            if (GameClient.client) {
                IsoPlayer player = IsoPlayer.getInstance();
                if (player == null) {
                    return "bad-args";
                }
                INetworkPacket.send(PacketTypes.PacketType.SyncItemFields, player, item);
            }
            return "sent";
        }
        catch (Throwable t) {
            Logger.error("[VehRepair] item condition sync failed", t);
            return "error";
        }
    }
}
