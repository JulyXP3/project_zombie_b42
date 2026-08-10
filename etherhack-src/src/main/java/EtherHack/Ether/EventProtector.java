/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.characters.IsoPlayer
 *  zombie.network.GameClient
 *  zombie.network.PacketTypes$PacketType
 *  zombie.network.ZomboidNetData
 */
package EtherHack.Ether;

import EtherHack.Ether.SafeAPI;
import EtherHack.GameClientWrapper;
import EtherHack.utils.FieldCache;
import EtherHack.utils.Logger;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import zombie.characters.IsoPlayer;
import zombie.network.GameClient;
import zombie.network.PacketTypes;
import zombie.network.ZomboidNetData;

public class EventProtector {
    private static EventProtector instance;
    private final GameClientWrapper wrapper;
    private final Map<String, Long> lastChecks = new HashMap<String, Long>();
    private final Set<String> protectedEvents = new HashSet<String>();
    private static final long CHECK_COOLDOWN = 2000L;
    private final SafeAPI safeAPI;
    private static Field validatedField;
    private static Field connectedField;

    private EventProtector() {
        this.wrapper = GameClientWrapper.get();
        this.safeAPI = SafeAPI.getInstance();
    }

    public static EventProtector getInstance() {
        if (instance == null) {
            instance = new EventProtector();
        }
        return instance;
    }

    public void handlePacket(String command, Map<String, Object> data) {
        try {
            String username = IsoPlayer.getInstance().getUsername();
            if (!this.shouldProcessPacket(username)) {
                return;
            }
            switch (command) {
                case "join_request": {
                    this.handleJoinRequest(data);
                    break;
                }
                case "heartbeat_request": {
                    this.handleHeartbeatRequest(data);
                }
            }
        }
        catch (Exception e) {
            Logger.printLog("Error handling packet: " + e.getMessage());
        }
    }

    private boolean shouldProcessPacket(String username) {
        long now = System.currentTimeMillis();
        Long lastCheck = this.lastChecks.get(username);
        if (lastCheck == null || now - lastCheck > 2000L) {
            this.lastChecks.put(username, now);
            return true;
        }
        return false;
    }

    private void handleJoinRequest(Map<String, Object> data) {
        String serverFragment = (String)data.get("message");
        String responseFragment = this.safeAPI.generateResponseKey(serverFragment);
    }

    private void handleHeartbeatRequest(Map<String, Object> data) {
        String currentKey = (String)data.get("message");
        if (!this.safeAPI.verifyHeartbeat(currentKey)) {
            Logger.printLog("Invalid heartbeat key detected");
        }
    }

    private void initializeProtectedEvents() {
        this.protectedEvents.add("OnPlayerConnect");
        this.protectedEvents.add("OnPlayerDisconnect");
        this.protectedEvents.add("OnCreatePlayer");
        this.protectedEvents.add("OnLogin");
        this.protectedEvents.add("OnLoginState");
        this.protectedEvents.add("OnLoginStateSuccess");
        this.protectedEvents.add("OnCharacterConnect");
        this.protectedEvents.add("OnCoopClientConnect");
        this.protectedEvents.add("OnGameStart");
        this.protectedEvents.add("OnCreateLivingCharacter");
        this.protectedEvents.add("OnClientCommand");
        this.protectedEvents.add("OnServerCommand");
    }

    public void installProtection() {
        try {
            this.wrapper.clearIncomingNetData();
            IsoPlayer player = IsoPlayer.getInstance();
            if (player != null) {
                player.setOnlineID((short)new Random().nextInt(10000));
                EventProtector.setFieldValue(player, "connected");
            }
            this.initializeProtectedState();
        }
        catch (Exception e) {
            Logger.printLog("Failed to install protection: " + e.getMessage());
        }
    }

    private void initializeProtectedState() {
        try {
            Object connection = GameClientWrapper.getConnection();
            if (connection != null) {
                EventProtector.setFieldValue(connection, "validated");
                this.wrapper.clearIncomingNetData();
            }
        }
        catch (Exception e) {
            Logger.printLog("Error initializing protected state: " + e.getMessage());
        }
    }

    private void sendFakePlayerUpdate(IsoPlayer player) {
        try {
            GameClient instance = GameClientWrapper.getInstance();
            if (instance != null && player != null) {
                instance.sendPlayer(player);
            }
        }
        catch (Exception e) {
            Logger.printLog("Error sending player update: " + e.getMessage());
        }
    }

    private short generateSafeID() {
        return (short)(Math.abs(new Random().nextInt()) % 10000 + 1);
    }

    public boolean shouldBlockEvent(String eventName) {
        if (!this.protectedEvents.contains(eventName)) {
            return false;
        }
        long now = System.currentTimeMillis();
        Long lastCheck = this.lastChecks.get(eventName);
        if (lastCheck == null || now - lastCheck > 2000L) {
            this.lastChecks.put(eventName, now);
            return true;
        }
        return false;
    }

    private static void setFieldValue(Object obj, String fieldName) {
        try {
            Field field = FieldCache.getField(obj.getClass(), fieldName);
            FieldCache.setFieldValue(obj, field, true);
        }
        catch (Exception e) {
            Logger.printLog("Error setting field value: " + e.getMessage());
        }
    }

    public static void filterIncomingPackets() {
        try {
            GameClientWrapper wrapper = GameClientWrapper.get();
            ArrayList<ZomboidNetData> netData = wrapper.getIncomingNetData();
            if (netData == null) {
                return;
            }
            ArrayList<ZomboidNetData> filteredData = new ArrayList<ZomboidNetData>();
            for (ZomboidNetData packet : netData) {
                short packetId;
                if (packet == null || (packetId = packet.type.getId()) == PacketTypes.PacketType.PlayerConnect.getId() || packetId == PacketTypes.PacketType.PlayerConnect.getId() || packetId == PacketTypes.PacketType.Login.getId() || packetId == PacketTypes.PacketType.PlayerUpdateReliable.getId()) continue;
                filteredData.add(packet);
            }
            netData.clear();
            netData.addAll(filteredData);
        }
        catch (Exception e) {
            Logger.printLog("Error filtering packets: " + e.getMessage());
        }
    }

    public void maintain() {
        EventProtector.filterIncomingPackets();
        IsoPlayer player = IsoPlayer.getInstance();
        if (player != null) {
            try {
                boolean connected;
                if (connectedField == null) {
                    connectedField = FieldCache.getField(player.getClass(), "connected");
                }
                if (connected = ((Boolean)FieldCache.getFieldValue(player, connectedField)).booleanValue()) {
                    this.sendFakePlayerUpdate(player);
                }
            }
            catch (Exception e) {
                Logger.printLog("Error maintaining connection: " + e.getMessage());
            }
        }
    }

    static {
        validatedField = null;
        connectedField = null;
    }
}
