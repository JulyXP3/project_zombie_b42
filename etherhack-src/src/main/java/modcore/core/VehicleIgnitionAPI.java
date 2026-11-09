/*
 * 点火平替: 用身上任意一把车钥匙插进目标车点火口，再从服务端正常启动引擎。
 *
 * 原版 VehicleCommands.putKeyInIgnition 只检查 "物品是 Key" 且玩家在驾驶位，
 * 不校验钥匙是否属于该车；BaseVehicle.tryStartEngine 的放行条件只看点火开关
 * 是否非空。因此在服务端零改动的前提下，任意钥匙都能让目标车进入正常启动流程。
 *
 * 边界: 仍需一件 Key 物品；电池、燃油、引擎状态等原版条件照常生效。不是"无条件"。
 * 钥匙圈内的嵌套钥匙不取，避免服务端从主背包移除不到而在点火口复制一份。
 */
package modcore.core;

import se.krka.kahlua.integration.annotations.LuaMethod;
import zombie.characters.IsoPlayer;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.inventory.types.InventoryContainer;
import zombie.inventory.types.Key;
import zombie.network.GameClient;
import zombie.vehicles.BaseVehicle;
import modcore.utils.Logger;

public final class VehicleIgnitionAPI {

    private VehicleIgnitionAPI() {
    }

    /** 插入任意持有的钥匙并请求服务端启动；返回 sent / no-key / no-driver / not-mp / error。 */
    @LuaMethod(name = "vehicleIgnitionStart", global = true)
    public static String vehicleIgnitionStart() {
        try {
            if (!GameClient.client || GameClient.instance == null) {
                return "not-mp";
            }
            IsoPlayer player = IsoPlayer.getInstance();
            if (player == null) {
                return "no-player";
            }
            BaseVehicle vehicle = player.getVehicle();
            if (vehicle == null || !vehicle.isDriver(player)) {
                return "no-driver";
            }
            int[] containerId = {-1};
            InventoryItem key = firstHeldKey(player.getInventory(), containerId);
            if (key == null) {
                return "no-key";
            }
            GameClient.instance.sendClientCommandV(player, "vehicle", "putKeyInIgnition",
                "key", key, "container", containerId[0]);
            GameClient.instance.sendClientCommand(player, "vehicle", "startEngine", null);
            Logger.printLog("[VehIgnition] any-key ignition + start request sent");
            return "sent";
        }
        catch (Throwable t) {
            Logger.error("[VehIgnition] request failed", t);
            return "error";
        }
    }

    /** 主背包直持优先；钥匙圈内的钥匙连同圈 ID 一起传，服务端才会从圈里正确移除。 */
    private static InventoryItem firstHeldKey(ItemContainer inventory, int[] containerId) {
        if (inventory == null) {
            return null;
        }
        for (InventoryItem item : inventory.getItems()) {
            if (item instanceof Key) {
                containerId[0] = -1;
                return item;
            }
        }
        for (InventoryItem item : inventory.getItems()) {
            if (!(item instanceof InventoryContainer ring) || !"KeyRing".equals(ring.getType())) {
                continue;
            }
            for (InventoryItem nested : ring.getInventory().getItems()) {
                if (nested instanceof Key) {
                    containerId[0] = ring.getID();
                    return nested;
                }
            }
        }
        return null;
    }
}
