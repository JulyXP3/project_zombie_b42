package modcore.core;

import se.krka.kahlua.integration.annotations.LuaMethod;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehiclePart;

/**
 * C1/C2 (2026-09-14, 见 analysis/DLL分析/C-传送与载具-设计方案(C1-C3已实施).md):
 * 载具座位包通道 —— 客户端直接发原版 VehicleEnterPacket, 服务端权威执行
 * `vehicle.enter(seat, 包内玩家)` 并广播 (requiredCapability=LoginOnServer,
 * 目标不绑定连接)。C1 = 自己远程上车; C2 = 把指定在线玩家塞进指定座位。
 *
 * 仅自建测试环境使用; 前置预检 (座位空/已安装/无物品) 在 Lua 侧完成,
 * 服务端 isConsistent 也会兜底拒绝 (座位被占时整包丢弃)。
 */
public final class VehicleSeatAPI {

    private VehicleSeatAPI() {
    }

    /**
     * 发 VehicleEnterPacket: 把 player 放进 vehicle 的 seat 座位。
     *
     * @return true = 包已发出 (服务器是否接受以其 isConsistent 为准)
     */
    @LuaMethod(name = "vehicleEnterSeat", global = true)
    public static boolean enterSeat(BaseVehicle vehicle, IsoPlayer player, int seat) {
        if (vehicle == null || player == null || seat < 0) {
            return false;
        }
        INetworkPacket.send(PacketTypes.PacketType.VehicleEnter, vehicle, player, seat);
        return true;
    }

    /**
     * 座位状态预检 (C1 的 9 条状态文案的机器版)。
     *
     * @return "ok" | "no-seat" (座位未安装) | "occupied" (有人) | "items" (座位上有物品)
     */
    @LuaMethod(name = "vehicleSeatInfo", global = true)
    public static String seatInfo(BaseVehicle vehicle, int seat) {
        if (vehicle == null || seat < 0) {
            return "no-seat";
        }
        VehiclePart part = vehicle.getPartForSeatContainer(seat);
        if (part == null || part.getInventoryItem() == null) {
            return "no-seat";
        }
        IsoGameCharacter occupant = vehicle.getCharacter(seat);
        if (occupant != null) {
            return "occupied";
        }
        if (vehicle.isSeatHoldingItems(part)) {
            return "items";
        }
        return "ok";
    }
}
