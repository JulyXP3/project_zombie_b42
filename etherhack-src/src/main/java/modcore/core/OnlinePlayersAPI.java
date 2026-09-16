package modcore.core;

import se.krka.kahlua.integration.annotations.LuaMethod;
import zombie.characters.IsoPlayer;
import zombie.characters.Role;
import zombie.network.GameClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * E3 (2026-09-14, 见 analysis/DLL分析/E-信息层-设计方案.md): 在线玩家快照。
 *
 * 数据源 = GameClient.instance.getPlayers() (客户端远端玩家表)。修订 (2026-09-14):
 * 旧实现走 WorldMapRemotePlayers —— 该表被服务端 MapRemotePlayerVisibility (默认 1=NONE)
 * 门控, 普通观察者收不到任何远端数据 → 名单恒空; getPlayers() 配合
 * ServerAntiCheatBypass.hookTimeoutRemotePlayers (隐身玩家剪枝豁免, GamePatcher.patchInvisibleKeep
 * 整方法替换 GameClient.timeoutRemotePlayers) 后**包含隐身管理员**。
 * 本机玩家恒为第一条; 权限值 = 角色能力数 (越大权限越高)。
 *
 * P1 性能修订 (2026-09-14 八十五, 用户提问"在线玩家会不会很吃性能"):
 * 旧实现被 UI 的 render() **每帧**调用一次 —— 每帧重建 ArrayList + 每人一条字符串 +
 * 每人一次角色能力数查询, 再由 Kahlua 侧逐条 tostring + table.concat, 60fps 下全是白做功
 * (名单一秒内几乎不变)。现在:
 *   ① {@link #changed()} —— UI 每帧只读一个 **boolean** (零分配, Boolean 有缓存),
 *      内部按 {@link #SCAN_INTERVAL_MS} 节流扫描, 内容真变了才置脏;
 *   ② {@link #snapshot()} 返回**上次扫描的缓存行** (同一份数据只构建一次, 不再每帧重建);
 *   ③ 角色能力数按角色名缓存 (角色能力一次会话内不变)。
 * 注意: {@link #changed()} 是"读后即清"的脏标记 —— 只允许一个 UI 消费者轮询 (信息页)。
 */
public final class OnlinePlayersAPI {

    /** 扫描节流: 名单位变化远慢于帧率, 250ms (4Hz) 对 UI 完全无感 */
    private static final long SCAN_INTERVAL_MS = 250L;

    private static final Map<String, Integer> rolePowerCache = new HashMap<String, Integer>();

    private static long lastScanMs = 0L;
    private static String lastSerial = null;
    private static ArrayList<String> cachedRows = new ArrayList<String>();
    private static boolean dirty = false;

    private OnlinePlayersAPI() {
    }

    /**
     * P1: 名单自上次询问以来**是否变化** —— 供 UI 每帧做变更检测 (boolean, 零分配)。
     * 内部 250ms 节流重扫; 人数/ID/隐身位/权限值任一变化即返回 true (读后即清)。
     *
     * @return true = 需要重建 UI 行
     */
    @LuaMethod(name = "onlinePlayersChanged", global = true)
    public static boolean changed() {
        scanIfDue();
        boolean result = dirty;
        dirty = false;
        return result;
    }

    /**
     * @return 每人一条 "用户名|onlineID|隐身(1/0)|权限值"; 第一条恒为本机玩家,
     *         其后为其他在线玩家 (含隐身者 —— 剪枝豁免补丁生效后保留在表内)。
     *         P1: 返回缓存 (构建只发生在节流扫描里, 本方法不重复扫描)。
     */
    @LuaMethod(name = "onlinePlayersInfo", global = true)
    public static ArrayList<String> snapshot() {
        scanIfDue();
        dirty = false;   // 调用方正在重建 → 消费掉脏标记
        return cachedRows;
    }

    /** 250ms 节流扫描; 内容变化才更新缓存并置脏 */
    private static void scanIfDue() {
        long now = System.currentTimeMillis();
        if (now - lastScanMs < SCAN_INTERVAL_MS) {
            return;
        }
        lastScanMs = now;
        try {
            ArrayList<String> rows = buildRows();
            String serial = String.join(";;", rows);
            if (!serial.equals(lastSerial)) {
                lastSerial = serial;
                cachedRows = rows;
                dirty = true;
            }
        }
        catch (Throwable ignored) {
            // 扫描失败不影响 UI (保留上一版数据)
        }
    }

    /** 真正构建名单行 (仅由节流扫描调用) */
    private static ArrayList<String> buildRows() {
        ArrayList<String> out = new ArrayList<String>();
        // 本机玩家 (单机也有 —— 名单常显)
        try {
            IsoPlayer self = IsoPlayer.getInstance();
            if (self != null) {
                out.add(self.getUsername() + "|" + self.getOnlineID() + "|"
                        + (self.isInvisible() ? "1" : "0") + "|" + rolePowerOf(self));
            }
        }
        catch (Exception ignored) {
        }
        // 其他在线玩家 (客户端远端玩家表; 隐身者经剪枝豁免补丁保留)
        try {
            ArrayList<IsoPlayer> players = GameClient.instance.getPlayers();
            if (players == null) {
                return out;
            }
            for (int i = 0; i < players.size(); ++i) {
                IsoPlayer player = players.get(i);
                if (player == null || player.isLocalPlayer()) {
                    continue;
                }
                try {
                    out.add(player.getUsername() + "|" + player.getOnlineID() + "|"
                            + (player.isInvisible() ? "1" : "0") + "|" + rolePowerOf(player));
                }
                catch (Exception ignored) {
                    // 单个玩家字段读取失败不影响名单其余行
                }
            }
        }
        catch (Exception ignored) {
        }
        return out;
    }

    /** 角色权限值 = 角色能力数 (Role.getCapabilities().size()); 无角色/异常 → 0。按角色名缓存。 */
    private static int rolePowerOf(IsoPlayer player) {
        try {
            Role role = player.getRole();
            if (role == null) {
                return 0;
            }
            String key = role.getName();
            if (key == null) {
                return role.getCapabilities().size();
            }
            Integer cached = rolePowerCache.get(key);
            if (cached != null) {
                return cached;
            }
            int power = role.getCapabilities().size();
            rolePowerCache.put(key, Integer.valueOf(power));
            return power;
        }
        catch (Exception ignored) {
            return 0;
        }
    }
}
