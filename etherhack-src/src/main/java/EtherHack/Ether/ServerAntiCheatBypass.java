/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.characters.IsoPlayer
 */
package EtherHack.Ether;

import EtherHack.utils.Logger;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import zombie.characters.IsoPlayer;

public class ServerAntiCheatBypass {
    private static ServerAntiCheatBypass instance;
    private final AtomicBoolean globalBypassEnabled = new AtomicBoolean(false);
    private final Map<String, AtomicBoolean> bypassFlags = new ConcurrentHashMap<String, AtomicBoolean>();
    private final Set<String> whitelistedPlayers = ConcurrentHashMap.newKeySet();
    private final Map<String, AtomicLong> validationHookCounts = new ConcurrentHashMap<String, AtomicLong>();
    private final Map<String, AtomicLong> reportBlockCounts = new ConcurrentHashMap<String, AtomicLong>();
    private final AtomicLong kickBlockCount = new AtomicLong(0L);
    private static final String[] ANTICHEAT_TYPES;

    private ServerAntiCheatBypass() {
        for (String type : ANTICHEAT_TYPES) {
            this.bypassFlags.put(type, new AtomicBoolean(false));
            this.validationHookCounts.put(type, new AtomicLong(0L));
            this.reportBlockCounts.put(type, new AtomicLong(0L));
        }
        Logger.print("[ServerAntiCheatBypass] Initialized with " + ANTICHEAT_TYPES.length + " anti-cheat types");
    }

    public static synchronized ServerAntiCheatBypass getInstance() {
        if (instance == null) {
            instance = new ServerAntiCheatBypass();
        }
        return instance;
    }

    public static boolean hookValidation(Object antiCheat, IsoPlayer player) {
        try {
            ServerAntiCheatBypass bypass = ServerAntiCheatBypass.getInstance();
            if (bypass.globalBypassEnabled.get()) {
                String type = ServerAntiCheatBypass.getAntiCheatType(antiCheat);
                bypass.validationHookCounts.get(type).incrementAndGet();
                Logger.print("[ServerAntiCheatBypass] Global bypass: Spoofing validation for " + type);
                return true;
            }
            if (player != null && bypass.whitelistedPlayers.contains(player.getUsername())) {
                String type = ServerAntiCheatBypass.getAntiCheatType(antiCheat);
                bypass.validationHookCounts.get(type).incrementAndGet();
                Logger.print("[ServerAntiCheatBypass] Whitelisted player: Spoofing validation for " + player.getUsername());
                return true;
            }
            String type = ServerAntiCheatBypass.getAntiCheatType(antiCheat);
            if (bypass.bypassFlags.containsKey(type) && bypass.bypassFlags.get(type).get()) {
                bypass.validationHookCounts.get(type).incrementAndGet();
                Logger.print("[ServerAntiCheatBypass] Type-specific bypass: Spoofing " + type + " validation");
                return true;
            }
            return false;
        }
        catch (Exception e) {
            Logger.print("[ServerAntiCheatBypass] Error in hookValidation");
            Logger.logException(e);
            return false;
        }
    }

    public static boolean hookSuspiciousActivity(Object activity, IsoPlayer player) {
        try {
            ServerAntiCheatBypass bypass = ServerAntiCheatBypass.getInstance();
            if (bypass.globalBypassEnabled.get()) {
                Logger.print("[ServerAntiCheatBypass] Global bypass: Blocking suspicious activity report");
                return true;
            }
            if (player != null && bypass.whitelistedPlayers.contains(player.getUsername())) {
                Logger.print("[ServerAntiCheatBypass] Whitelisted player: Blocking report for " + player.getUsername());
                return true;
            }
            for (AtomicBoolean flag : bypass.bypassFlags.values()) {
                if (!flag.get()) continue;
                Logger.print("[ServerAntiCheatBypass] Type-specific bypass active: Blocking suspicious activity report");
                return true;
            }
            return false;
        }
        catch (Exception e) {
            Logger.print("[ServerAntiCheatBypass] Error in hookSuspiciousActivity");
            Logger.logException(e);
            return false;
        }
    }

    public static boolean hookKickAction(String username, String reason) {
        try {
            ServerAntiCheatBypass bypass = ServerAntiCheatBypass.getInstance();
            if (bypass.globalBypassEnabled.get()) {
                bypass.kickBlockCount.incrementAndGet();
                Logger.print("[ServerAntiCheatBypass] Global bypass: Blocking kick for " + username + " (reason: " + reason + ")");
                return true;
            }
            if (bypass.whitelistedPlayers.contains(username)) {
                bypass.kickBlockCount.incrementAndGet();
                Logger.print("[ServerAntiCheatBypass] Whitelisted player: Blocking kick for " + username);
                return true;
            }
            if (reason != null && ServerAntiCheatBypass.isAntiCheatRelatedKick(reason)) {
                for (AtomicBoolean flag : bypass.bypassFlags.values()) {
                    if (!flag.get()) continue;
                    bypass.kickBlockCount.incrementAndGet();
                    Logger.print("[ServerAntiCheatBypass] Anti-cheat bypass active: Blocking kick for " + username);
                    return true;
                }
            }
            return false;
        }
        catch (Exception e) {
            Logger.print("[ServerAntiCheatBypass] Error in hookKickAction");
            Logger.logException(e);
            return false;
        }
    }

    public void enableGlobalBypass() {
        this.globalBypassEnabled.set(true);
        Logger.print("[ServerAntiCheatBypass] Global bypass ENABLED");
    }

    public void disableGlobalBypass() {
        this.globalBypassEnabled.set(false);
        Logger.print("[ServerAntiCheatBypass] Global bypass DISABLED");
    }

    public void enableBypassForType(String type) {
        if (this.bypassFlags.containsKey(type)) {
            this.bypassFlags.get(type).set(true);
            Logger.print("[ServerAntiCheatBypass] Enabled bypass for type: " + type);
        } else {
            Logger.print("[ServerAntiCheatBypass] Unknown anti-cheat type: " + type);
        }
    }

    public void disableBypassForType(String type) {
        if (this.bypassFlags.containsKey(type)) {
            this.bypassFlags.get(type).set(false);
            Logger.print("[ServerAntiCheatBypass] Disabled bypass for type: " + type);
        }
    }

    public void enableAllTypeBypasses() {
        for (String type : ANTICHEAT_TYPES) {
            this.bypassFlags.get(type).set(true);
        }
        Logger.print("[ServerAntiCheatBypass] Enabled bypass for all types");
    }

    public void disableAllTypeBypasses() {
        for (String type : ANTICHEAT_TYPES) {
            this.bypassFlags.get(type).set(false);
        }
        Logger.print("[ServerAntiCheatBypass] Disabled bypass for all types");
    }

    public void whitelistPlayer(String username) {
        this.whitelistedPlayers.add(username);
        Logger.print("[ServerAntiCheatBypass] Whitelisted player: " + username);
    }

    public void unwhitelistPlayer(String username) {
        this.whitelistedPlayers.remove(username);
        Logger.print("[ServerAntiCheatBypass] Removed player from whitelist: " + username);
    }

    public void clearWhitelist() {
        this.whitelistedPlayers.clear();
        Logger.print("[ServerAntiCheatBypass] Cleared player whitelist");
    }

    public boolean isGlobalBypassEnabled() {
        return this.globalBypassEnabled.get();
    }

    public boolean isBypassEnabledForType(String type) {
        return this.bypassFlags.containsKey(type) && this.bypassFlags.get(type).get();
    }

    public boolean isPlayerWhitelisted(String username) {
        return this.whitelistedPlayers.contains(username);
    }

    public Map<String, Long> getStatistics() {
        HashMap<String, Long> stats = new HashMap<String, Long>();
        for (Map.Entry<String, AtomicLong> entry : this.validationHookCounts.entrySet()) {
            stats.put("validation_" + entry.getKey(), entry.getValue().get());
        }
        for (Map.Entry<String, AtomicLong> entry : this.reportBlockCounts.entrySet()) {
            stats.put("report_" + entry.getKey(), entry.getValue().get());
        }
        stats.put("kicks_blocked", this.kickBlockCount.get());
        return stats;
    }

    public void resetStatistics() {
        for (AtomicLong count : this.validationHookCounts.values()) {
            count.set(0L);
        }
        for (AtomicLong count : this.reportBlockCounts.values()) {
            count.set(0L);
        }
        this.kickBlockCount.set(0L);
        Logger.print("[ServerAntiCheatBypass] Statistics reset");
    }

    private static String getAntiCheatType(Object antiCheat) {
        if (antiCheat == null) {
            return "unknown";
        }
        String className = antiCheat.getClass().getSimpleName();
        if (className.contains("Movement")) {
            return "movement";
        }
        if (className.contains("XP") || className.contains("Experience")) {
            return "xp";
        }
        if (className.contains("Hit") || className.contains("Combat")) {
            return "hit";
        }
        if (className.contains("Packet")) {
            return "packet";
        }
        if (className.contains("Permission")) {
            return "permission";
        }
        if (className.contains("Fire")) {
            return "fire";
        }
        if (className.contains("Safehouse")) {
            return "safehouse";
        }
        if (className.contains("Recipe")) {
            return "recipe";
        }
        if (className.contains("Player")) {
            return "player";
        }
        if (className.contains("Checksum")) {
            return "checksum";
        }
        if (className.contains("Item")) {
            return "item";
        }
        if (className.contains("ServerCustomization")) {
            return "serverCustomization";
        }
        if (className.contains("Safety")) {
            return "safety";
        }
        return "unknown";
    }

    private static boolean isAntiCheatRelatedKick(String reason) {
        if (reason == null) {
            return false;
        }
        String lowerReason = reason.toLowerCase();
        return lowerReason.contains("cheat") || lowerReason.contains("exploit") || lowerReason.contains("hack") || lowerReason.contains("suspicious") || lowerReason.contains("invalid") || lowerReason.contains("violation") || lowerReason.contains("unauthorized");
    }

    public String getStatusString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[ServerAntiCheatBypass Status]\n");
        sb.append("Global Bypass: ").append(this.globalBypassEnabled.get() ? "ENABLED" : "DISABLED").append("\n");
        sb.append("Whitelisted Players: ").append(this.whitelistedPlayers.size()).append("\n");
        sb.append("\nType-Specific Bypasses:\n");
        for (String type : ANTICHEAT_TYPES) {
            sb.append("  ").append(type).append(": ").append(this.bypassFlags.get(type).get() ? "ENABLED" : "DISABLED").append(" (hooks: ").append(this.validationHookCounts.get(type).get()).append(")\n");
        }
        sb.append("\nTotal Kicks Blocked: ").append(this.kickBlockCount.get());
        return sb.toString();
    }

    public void printStatus() {
        Logger.print(this.getStatusString());
    }

    static {
        ANTICHEAT_TYPES = new String[]{"movement", "xp", "hit", "packet", "permission", "fire", "safehouse", "recipe", "player", "checksum", "item", "serverCustomization", "safety"};
    }
}
