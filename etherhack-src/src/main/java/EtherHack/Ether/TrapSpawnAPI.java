/*
 * Red-team POC: arbitrary HandWeapon spawn via the explosive-trap exploit (multiplayer).
 *
 * Chain (verified against the running game jar):
 *   1. Client forges AddExplosiveTrapPacket with an arbitrary, fully
 *      client-serialized HandWeapon; the server parses it with
 *      InventoryItem.loadItem() and performs NO validation
 *      (LoginOnServer, no anticheats, no isConsistent override).
 *   2. processServer() creates an IsoTrap holding the weapon at the given
 *      square and broadcasts it; the weapon enters the world.
 *   3. Vanilla ISTakeTrap (shared timed action) -> server-side complete()
 *      -> inventory:AddItem(trap:getItem()) — server-authoritative pickup.
 *
 * Fuse detail: HandWeapon.isInstantExplosion() returns true for any weapon
 * without explosive script fields (explosionTimer<=0 && sensorRange<=0 &&
 * no remote) — the server would detonate the trap on placement and destroy
 * the item. The forged item therefore carries a huge explosionTimer so the
 * server takes the persistent-trap branch; the field is inert on carried
 * items (only IsoTrap.update() counts it down).
 *
 * Only for the user's own server / self-built test environment.
 */
package EtherHack.Ether;

import se.krka.kahlua.integration.annotations.LuaMethod;
import zombie.characters.IsoPlayer;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.types.HandWeapon;
import zombie.iso.IsoGridSquare;
import zombie.network.GameClient;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;
import EtherHack.utils.Logger;

public class TrapSpawnAPI {
    /*
     * 引信步进发生在 IsoTrap.update() (服务端, beep.check() 每 tick),
     * 99999999 tick 在实际游玩时间内不会倒数到 0; 拾回背包后该字段无任何
     * 消费方 (仅 IsoTrap 构造器从武器读取), 纯惰性数据。
     */
    private static final int SAFE_FUSE_TICKS = 99999999;

    @LuaMethod(name = "trapSpawnPlace", global = true)
    public static boolean trapSpawnPlace(String itemType) {
        try {
            IsoPlayer player = IsoPlayer.getInstance();
            if (player == null) {
                Logger.printLog("[TrapSpawn] place: no local player");
                return false;
            }
            if (!GameClient.client || GameClient.connection == null) {
                Logger.printLog("[TrapSpawn] place: multiplayer client only");
                return false;
            }
            IsoGridSquare square = player.getCurrentSquare();
            if (square == null) {
                Logger.printLog("[TrapSpawn] place: no current square");
                return false;
            }
            InventoryItem item = InventoryItemFactory.CreateItem(itemType);
            if (!(item instanceof HandWeapon)) {
                Logger.printLog("[TrapSpawn] place: not a HandWeapon: " + itemType);
                return false;
            }
            HandWeapon weapon = (HandWeapon) item;
            weapon.setExplosionTimer(SAFE_FUSE_TICKS);
            // AddExplosiveTrapPacket.setData 只接受 (HandWeapon, IsoPlayer, IsoGridSquare) 三参,
            // 服务器按到达序处理, 引信>0 走静置分支 (transmitCompleteItemToClients)
            INetworkPacket.send(PacketTypes.PacketType.AddExplosiveTrap, weapon, player, square);
            Logger.printLog("[TrapSpawn] placed trap: " + itemType);
            return true;
        } catch (Throwable t) {
            Logger.printLog("[TrapSpawn] place error: " + t.getMessage());
            return false;
        }
    }
}
