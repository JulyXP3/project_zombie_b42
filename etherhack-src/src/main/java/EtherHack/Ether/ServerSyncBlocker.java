/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  se.krka.kahlua.integration.annotations.LuaMethod
 *  zombie.characters.CharacterStat
 *  zombie.characters.IsoGameCharacter$XP
 *  zombie.characters.IsoPlayer
 *  zombie.network.PacketTypes$PacketType
 *  zombie.network.ZomboidNetData
 */
package EtherHack.Ether;

import EtherHack.GameClientWrapper;
import EtherHack.utils.FieldCache;
import EtherHack.utils.Logger;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import se.krka.kahlua.integration.annotations.LuaMethod;
import zombie.characters.CharacterStat;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.network.PacketTypes;
import zombie.network.ZomboidNetData;

public class ServerSyncBlocker {
    private static ServerSyncBlocker instance;
    private final GameClientWrapper wrapper;
    private static final Map<String, CharacterStat> CHARACTER_STAT_CACHE;
    private volatile boolean blockStatsSync = false;
    private volatile boolean blockSkillsSync = false;
    private volatile boolean blockInventorySync = false;
    private volatile boolean blockTraitsSync = false;
    private final Map<CharacterStat, Float> protectedStats = new ConcurrentHashMap<CharacterStat, Float>();
    private final Map<String, Integer> protectedSkillLevels = new ConcurrentHashMap<String, Integer>();
    private final Map<String, Float> protectedSkillXP = new ConcurrentHashMap<String, Float>();
    private final Set<String> protectedTraits = ConcurrentHashMap.newKeySet();
    private static final Set<Short> SYNC_PACKET_TYPES;

    private ServerSyncBlocker() {
        this.wrapper = GameClientWrapper.get();
    }

    public static ServerSyncBlocker getInstance() {
        if (instance == null) {
            instance = new ServerSyncBlocker();
        }
        return instance;
    }

    @LuaMethod(name="enableStatsProtection", global=true)
    public static void enableStatsProtection() {
        ServerSyncBlocker.getInstance().blockStatsSync = true;
        Logger.printLog("Stats protection enabled - server sync will be blocked");
    }

    @LuaMethod(name="disableStatsProtection", global=true)
    public static void disableStatsProtection() {
        ServerSyncBlocker.getInstance().blockStatsSync = false;
        ServerSyncBlocker.getInstance().protectedStats.clear();
        Logger.printLog("Stats protection disabled");
    }

    @LuaMethod(name="enableSkillsProtection", global=true)
    public static void enableSkillsProtection() {
        ServerSyncBlocker.getInstance().blockSkillsSync = true;
        Logger.printLog("Skills protection enabled - server sync will be blocked");
    }

    @LuaMethod(name="disableSkillsProtection", global=true)
    public static void disableSkillsProtection() {
        ServerSyncBlocker.getInstance().blockSkillsSync = false;
        ServerSyncBlocker.getInstance().protectedSkillLevels.clear();
        ServerSyncBlocker.getInstance().protectedSkillXP.clear();
        Logger.printLog("Skills protection disabled");
    }

    @LuaMethod(name="enableFullProtection", global=true)
    public static void enableFullProtection() {
        ServerSyncBlocker blocker = ServerSyncBlocker.getInstance();
        blocker.blockStatsSync = true;
        blocker.blockSkillsSync = true;
        blocker.blockInventorySync = true;
        blocker.blockTraitsSync = true;
        Logger.printLog("Full protection enabled - all server sync will be blocked");
    }

    @LuaMethod(name="disableFullProtection", global=true)
    public static void disableFullProtection() {
        ServerSyncBlocker blocker = ServerSyncBlocker.getInstance();
        blocker.blockStatsSync = false;
        blocker.blockSkillsSync = false;
        blocker.blockInventorySync = false;
        blocker.blockTraitsSync = false;
        blocker.protectedStats.clear();
        blocker.protectedSkillLevels.clear();
        blocker.protectedSkillXP.clear();
        blocker.protectedTraits.clear();
        Logger.printLog("All protection disabled");
    }

    @LuaMethod(name="protectStat", global=true)
    public static void protectStat(String statName, float value) {
        try {
            CharacterStat stat = ServerSyncBlocker.getCharacterStatByName(statName);
            if (stat != null) {
                ServerSyncBlocker.getInstance().protectedStats.put(stat, Float.valueOf(value));
                Logger.printLog("Protected stat: " + statName + " = " + value);
            } else {
                Logger.printLog("Invalid stat name: " + statName);
            }
        }
        catch (Exception e) {
            Logger.printLog("Error protecting stat: " + statName + " - " + e.getMessage());
        }
    }

    private static CharacterStat getCharacterStatByName(String name) {
        return CHARACTER_STAT_CACHE.get(name.toUpperCase());
    }

    @LuaMethod(name="unprotectStat", global=true)
    public static void unprotectStat(String statName) {
        try {
            CharacterStat stat = ServerSyncBlocker.getCharacterStatByName(statName);
            if (stat != null) {
                ServerSyncBlocker.getInstance().protectedStats.remove(stat);
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    @LuaMethod(name="protectSkill", global=true)
    public static void protectSkill(String perkName, int level, float xp) {
        ServerSyncBlocker.getInstance().protectedSkillLevels.put(perkName, level);
        ServerSyncBlocker.getInstance().protectedSkillXP.put(perkName, Float.valueOf(xp));
        Logger.printLog("Protected skill: " + perkName + " level=" + level + " xp=" + xp);
    }

    @LuaMethod(name="reapplyProtectedValues", global=true)
    public static void reapplyProtectedValues() {
        ServerSyncBlocker.getInstance().reapplyAllProtectedValues();
    }

    private void reapplyAllProtectedValues() {
        try {
            IsoPlayer player = IsoPlayer.getInstance();
            if (player == null) {
                return;
            }
            if (this.blockStatsSync && !this.protectedStats.isEmpty()) {
                for (Map.Entry<CharacterStat, Float> entry : this.protectedStats.entrySet()) {
                    player.getStats().set(entry.getKey(), entry.getValue().floatValue());
                }
            }
            if (this.blockSkillsSync && !this.protectedSkillLevels.isEmpty()) {
                for (Map.Entry<String, Integer> entry : this.protectedSkillLevels.entrySet()) {
                    try {
                        this.setSkillLevelDirect(player, entry.getKey(), entry.getValue());
                        Float xp = this.protectedSkillXP.get(entry.getKey());
                        if (xp == null) continue;
                        this.setSkillXPDirect(player, entry.getKey(), xp.floatValue());
                    }
                    catch (Exception exception) {}
                }
            }
        }
        catch (Exception e) {
            Logger.printLog("Error reapplying protected values: " + e.getMessage());
        }
    }

    private void setSkillLevelDirect(IsoPlayer player, String perkName, int level) {
        try {
            Class<?> perkClass = Class.forName("zombie.characters.skills.PerkFactory$Perks");
            Object perk = null;
            for (Object enumConstant : perkClass.getEnumConstants()) {
                if (!enumConstant.toString().equalsIgnoreCase(perkName)) continue;
                perk = enumConstant;
                break;
            }
            if (perk == null) {
                Method fromString = FieldCache.getMethod(perkClass, "fromString", String.class);
                perk = FieldCache.invokeMethod(null, fromString, perkName);
            }
            if (perk != null) {
                Method setMethod = FieldCache.getMethod(player.getClass(), "setPerkLevelDebug", perkClass, Integer.TYPE);
                FieldCache.invokeMethod(player, setMethod, perk, level);
            }
        }
        catch (Exception e) {
            Logger.printLog("Could not set skill level directly: " + perkName);
        }
    }

    private void setSkillXPDirect(IsoPlayer player, String perkName, float xpValue) {
        try {
            IsoGameCharacter.XP xpObj;
            Class<?> perkClass = Class.forName("zombie.characters.skills.PerkFactory$Perks");
            Object perk = null;
            for (Object enumConstant : perkClass.getEnumConstants()) {
                if (!enumConstant.toString().equalsIgnoreCase(perkName)) continue;
                perk = enumConstant;
                break;
            }
            if (perk != null && (xpObj = player.getXp()) != null) {
                try {
                    Method addXp = FieldCache.getMethod(xpObj.getClass(), "AddXP", perkClass, Float.TYPE);
                    FieldCache.invokeMethod(xpObj, addXp, perk, Float.valueOf(xpValue));
                }
                catch (Exception exception) {}
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    @LuaMethod(name="filterIncomingSyncPackets", global=true)
    public static void filterIncomingSyncPackets() {
        ServerSyncBlocker.getInstance().filterPackets();
    }

    private void filterPackets() {
        if (!(this.blockStatsSync || this.blockSkillsSync || this.blockInventorySync)) {
            return;
        }
        try {
            if (GameClientWrapper.getInstance() == null) {
                return;
            }
            ArrayList<ZomboidNetData> netData = this.wrapper.getIncomingNetData();
            if (netData == null || netData.isEmpty()) {
                return;
            }
            ArrayList<ZomboidNetData> toRemove = new ArrayList<ZomboidNetData>();
            for (ZomboidNetData packet : netData) {
                if (packet == null || !this.shouldFilterPacket(packet)) continue;
                toRemove.add(packet);
            }
            if (!toRemove.isEmpty()) {
                netData.removeAll(toRemove);
                this.reapplyAllProtectedValues();
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    private boolean shouldFilterPacket(ZomboidNetData packet) {
        try {
            short packetId = packet.type.getId();
            if (packetId == PacketTypes.PacketType.PlayerUpdateReliable.getId()) {
                return this.blockStatsSync || this.blockSkillsSync;
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        return false;
    }

    @LuaMethod(name="isProtectionActive", global=true)
    public static boolean isProtectionActive() {
        ServerSyncBlocker blocker = ServerSyncBlocker.getInstance();
        return blocker.blockStatsSync || blocker.blockSkillsSync || blocker.blockInventorySync || blocker.blockTraitsSync;
    }

    @LuaMethod(name="getProtectionStatus", global=true)
    public static String getProtectionStatus() {
        ServerSyncBlocker blocker = ServerSyncBlocker.getInstance();
        return String.format("Stats:%b Skills:%b Inventory:%b Traits:%b", blocker.blockStatsSync, blocker.blockSkillsSync, blocker.blockInventorySync, blocker.blockTraitsSync);
    }

    static {
        CHARACTER_STAT_CACHE = new HashMap<String, CharacterStat>();
        try {
            for (Field field : CharacterStat.class.getDeclaredFields()) {
                if (!field.getType().equals(CharacterStat.class)) continue;
                field.setAccessible(true);
                CharacterStat stat = (CharacterStat)field.get(null);
                if (stat == null) continue;
                CHARACTER_STAT_CACHE.put(field.getName().toUpperCase(), stat);
                CHARACTER_STAT_CACHE.put(field.getName().toLowerCase(), stat);
            }
            Logger.print("[ServerSyncBlocker] Cached " + CHARACTER_STAT_CACHE.size() + " CharacterStat values");
        }
        catch (Exception e) {
            Logger.printLog("[ServerSyncBlocker] Failed to cache CharacterStat enum: " + e.getMessage());
        }
        SYNC_PACKET_TYPES = new HashSet<Short>();
        try {
            SYNC_PACKET_TYPES.add(PacketTypes.PacketType.PlayerUpdateReliable.getId());
        }
        catch (Exception e) {
            Logger.printLog("Error initializing packet types: " + e.getMessage());
        }
    }
}
