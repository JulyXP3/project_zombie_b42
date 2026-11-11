/*
 * 研判探针 (2026-11-11): 僵尸挂载单发探针 — 只用于自建测试服核实结论。
 *
 * 静态链 (analysis/pz_sync_analysis):
 *   - GameCharacterAttachedItemPacket (handlingType=3, LoginOnServer, 无反作弊):
 *     客户端 write 分支只发 movingObject + location + containerId + itemId;
 *     服务端 processServer 按 containerId 解析容器、按 itemId 取物,
 *     只校验 movingObject.isConsistent + containerId.isConsistent, 不校验物品归属。
 *   - IsoZombie 构造 initAttachedItems("Human"); setAttachedItem -> AttachedItems.setItem
 *     只做 group.checkValid(location), 不从源容器移除物品。
 *   - IsoZombie.onKilled -> DoZombieInventory: 把 attachedItems 逐件并进僵尸库存
 *     (setContainer + getItems().add), 源容器 (我们的背包) 不变。
 *   => "挂载后我背包里还有没有 / 尸体里是不是同一件" 由实机核验按钮判定。
 *
 * 发送方式与 FishingSpawnAPI 同款: 手动 startPacket -> doPacket -> write -> send,
 * 不走 setData 自动发送 (客户端只对本地玩家自动上行, 僵尸目标必须手动发)。
 */
package modcore.core;

import se.krka.kahlua.integration.annotations.LuaMethod;
import zombie.characters.IsoGameCharacter;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.inventory.InventoryItem;
import zombie.network.GameClient;
import zombie.network.PacketTypes;
import zombie.network.packets.GameCharacterAttachedItemPacket;
import modcore.utils.Logger;

public final class ZombieAttachAPI {

    private ZombieAttachAPI() {
    }

    /**
     * 把一件真实物品挂到指定角色 (本探针只用于僵尸)。
     * 物品必须仍在可解析容器里 (主背包即可); 槽位名必须在目标角色的附着组内
     * (僵尸为 "Human" 组, 由调用方传入合法名, 如 "Belt Right")。
     * 返回 sent / not-mp / bad-args / no-connection / error。
     */
    @LuaMethod(name = "zombieAttachSend", global = true)
    public static String zombieAttachSend(IsoGameCharacter target, InventoryItem item, String location) {
        try {
            if (!GameClient.client || GameClient.instance == null || GameClient.connection == null) {
                return "not-mp";
            }
            if (target == null || item == null || location == null || location.isEmpty()) {
                return "bad-args";
            }
            if (item.getContainer() == null) {
                return "bad-args";
            }
            GameCharacterAttachedItemPacket packet = new GameCharacterAttachedItemPacket();
            packet.setData(target, location, item);
            ByteBufferWriter b = GameClient.connection.startPacket();
            PacketTypes.PacketType.GameCharacterAttachedItem.doPacket(b);
            packet.write(b);
            PacketTypes.PacketType.GameCharacterAttachedItem.send(GameClient.connection);
            return "sent";
        }
        catch (Throwable t) {
            Logger.error("[ResearchProbe] attach send failed", t);
            return "error";
        }
    }
}
