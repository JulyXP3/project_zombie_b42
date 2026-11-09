package modcore.core;

import zombie.CombatManager;
import zombie.characters.IsoZombie;
import zombie.iso.IsoObject;
import zombie.popman.NetworkZombieSimulator;

import java.util.Iterator;
import java.util.List;

/**
 * 超级群攻 · 属主僵尸命中包抑制。
 *
 * <p>b42.22 删除了 {@code GameClient.sendPlayerHit}，改由 {@code attackCollisionCheck} 末尾
 * {@code INetworkPacket.send(PlayerHit, Object[])} 上报，载荷是 {@code List<CombatManager.TargetHitList>}。
 * 这里按条过滤：属主（客户端模拟）僵尸的命中不回报，其余目标照常上报——对真人玩家的命中语义不变。
 *
 * <p>本类必须保持 ASM-free：注入体只引用这里的方法签名。
 */
public final class HitPacketFilter {
    private HitPacketFilter() {}

    /**
     * 就地移除属主模拟僵尸的命中条目。
     *
     * @return true = 仍有需要上报的目标；false = 全部被抑制，整包不必发送
     */
    public static boolean filter(List<?> hits) {
        try {
            if (hits == null || hits.isEmpty()) return true;
            Iterator<?> iterator = hits.iterator();
            while (iterator.hasNext()) {
                Object entry = iterator.next();
                if (!(entry instanceof CombatManager.TargetHitList target)) continue;
                IsoObject object = target.target();
                if (!(object instanceof IsoZombie zombie)) continue;
                if (NetworkZombieSimulator.getInstance().isZombieSimulated(Short.valueOf(zombie.getOnlineID()))) {
                    iterator.remove();
                }
            }
            return !hits.isEmpty();
        } catch (Throwable failure) {
            // 判定失败时保持原版上报, 不因过滤逻辑本身出错而吞掉命中
            return true;
        }
    }
}
