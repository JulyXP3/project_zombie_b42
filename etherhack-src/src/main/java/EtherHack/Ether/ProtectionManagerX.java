/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.GameWindow
 *  zombie.core.network.ByteBufferWriter
 *  zombie.network.GameClient
 *  zombie.network.PacketTypes$PacketType
 */
package EtherHack.Ether;

import EtherHack.GameClientWrapper;
import EtherHack.utils.Logger;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import zombie.GameWindow;
import zombie.core.network.ByteBufferWriter;
import zombie.network.GameClient;
import zombie.network.PacketTypes;

public class ProtectionManagerX {
    private static ProtectionManagerX instance;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, String> virtualGlobals = new ConcurrentHashMap<String, String>();
    private final Map<String, Object> realFunctions = new ConcurrentHashMap<String, Object>();
    private final Set<String> protectedNames = new ConcurrentSkipListSet<String>();
    private static final String MODULE_ID = "EtherHammerX";
    private final Map<String, Long> keyTimestamps = new ConcurrentHashMap<String, Long>();
    private final Map<String, String> responseCache = new ConcurrentHashMap<String, String>();
    private String currentKey;
    private String pendingKey;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public ProtectionManagerX() {
        this.initializeProtection();
    }

    void initializeProtection() {
        this.storeFunctions();
        this.createVirtualGlobals();
        this.startKeyRotation();
    }

    private void storeFunctions() {
        for (String funcName : Arrays.asList("getAntiCheat8Status", "getAntiCheat12Status", "getExtraTexture", "hackAdminAccess", "isDisableFakeInfectionLevel", "isDisableInfectionLevel", "isDisableWetness", "isEnableUnlimitedCarry", "isOptimalWeight", "isOptimalCalories", "isPlayerInSafeTeleported", "learnAllRecipes", "requireExtra", "safePlayerTeleport", "toggleEnableUnlimitedCarry", "toggleOptimalWeight", "toggleOptimalCalories", "vehicle", "toggleDisableFakeInfectionLevel", "toggleDisableInfectionLevel", "toggleDisableWetness", "repairPart", "setPartCondition", "repair", "setContainerContentAmount", "instanceof")) {
            String obfuscatedName = this.generateObfuscatedName();
            this.protectedNames.add(obfuscatedName);
            this.realFunctions.put(obfuscatedName, this.createProxyFunction(funcName));
        }
    }

    private String generateObfuscatedName() {
        byte[] randomBytes = new byte[16];
        this.random.nextBytes(randomBytes);
        return "_" + Base64.getEncoder().encodeToString(randomBytes).substring(0, 8);
    }

    private void createVirtualGlobals() {
        for (String name : this.protectedNames) {
            this.virtualGlobals.put(this.generateObfuscatedName(), "function() end");
        }
    }

    private void startKeyRotation() {
        this.scheduler.scheduleAtFixedRate(() -> {
            this.currentKey = this.generateNewKey();
            this.pendingKey = this.generateNewKey();
        }, 0L, 10L, TimeUnit.SECONDS);
    }

    private String generateNewKey() {
        byte[] keyBytes = new byte[32];
        this.random.nextBytes(keyBytes);
        return Base64.getEncoder().encodeToString(keyBytes);
    }

    public Object invokeFunction(String name, Object ... args) {
        ProxyFunction func;
        String realName = this.protectedNames.stream().filter(n -> n.endsWith(name)).findFirst().orElse(null);
        if (realName != null && (func = (ProxyFunction)this.realFunctions.get(realName)) != null) {
            return func.invoke(args);
        }
        return null;
    }

    private ProxyFunction createProxyFunction(String name) {
        return args -> {
            String validationKey = this.generateValidationKey();
            return this.executeSecurely(name, validationKey, args);
        };
    }

    private String generateValidationKey() {
        return UUID.randomUUID().toString();
    }

    private Object executeSecurely(String name, String validationKey, Object ... args) {
        return null;
    }

    public void handlePacket(String command, Map<String, Object> data) {
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

    private String generateFakeResponse(String serverFragment) {
        return UUID.randomUUID().toString();
    }

    public static synchronized ProtectionManagerX getInstance() {
        if (instance == null) {
            instance = new ProtectionManagerX();
        }
        return instance;
    }

    public void shutdown() {
        this.scheduler.shutdown();
    }

    private String generateHeartbeatResponse() {
        String username = GameClient.username;
        if (username == null) {
            return null;
        }
        long timestamp = System.currentTimeMillis();
        String fragment = String.format("%d_%s_%s", timestamp, this.generateSafeHash(username), this.generateSafeHash(this.currentKey));
        this.responseCache.put(this.currentKey, fragment);
        this.keyTimestamps.put(this.currentKey, timestamp);
        this.cleanResponseCache();
        return fragment;
    }

    private String generateSafeHash(String input) {
        long hash = 0L;
        for (byte b : input.getBytes()) {
            hash = (hash << 5) - hash + (long)b;
        }
        return Long.toHexString(hash);
    }

    private void cleanResponseCache() {
        long now = System.currentTimeMillis();
        this.keyTimestamps.entrySet().removeIf(entry -> now - (Long)entry.getValue() > TimeUnit.MINUTES.toMillis(5L));
        this.responseCache.keySet().removeIf(key -> !this.keyTimestamps.containsKey(key));
    }

    public void handleNetworkPacket(String command, Map<String, Object> data) {
        try {
            switch (command) {
                case "join_request": {
                    this.handleJoinRequest(data);
                    break;
                }
                case "heartbeat_request": {
                    this.handleHeartbeatRequest(data);
                    break;
                }
                case "key_rotation": {
                    this.handleKeyRotation(data);
                    break;
                }
                default: {
                    Logger.printLog("Unknown command received: " + command);
                    break;
                }
            }
        }
        catch (Exception e) {
            Logger.printLog("Error handling network packet: " + e.getMessage());
        }
    }

    private void handleJoinRequest(Map<String, Object> data) {
        final String serverFragment = (String)data.get("message");
        String username = GameClient.username;
        if (username == null) {
            return;
        }
        final String fragment2 = this.generateSafeHash(username + System.currentTimeMillis());
        HashMap<String, Object> packetData = new HashMap<String, Object>();
        packetData.put("message", fragment2);
        packetData.put("fragment", serverFragment);
        packetData.put("timestamp", System.currentTimeMillis());
        this.sendPacket("join_response", packetData);
    }

    private void handleHeartbeatRequest(Map<String, Object> data) {
        final String response = this.generateHeartbeatResponse();
        if (response == null) {
            return;
        }
        HashMap<String, Object> packetData = new HashMap<String, Object>();
        packetData.put("message", response);
        packetData.put("key", this.currentKey);
        packetData.put("timestamp", System.currentTimeMillis());
        this.sendPacket("heartbeat_response", packetData);
    }

    private void handleKeyRotation(Map<String, Object> data) {
        String newKey = (String)data.get("key");
        if (this.validKey(newKey)) {
            this.pendingKey = newKey;
        }
    }

    private boolean validKey(String key) {
        return key != null && key.length() >= 32;
    }

    private void sendPacket(String command, Map<String, Object> data) {
        try {
            Object connection = GameClientWrapper.getConnection();
            if (connection == null) {
                return;
            }
            Method startPacketMethod = connection.getClass().getMethod("startPacket", new Class[0]);
            ByteBufferWriter writer = (ByteBufferWriter)startPacketMethod.invoke(connection, new Object[0]);
            PacketTypes.PacketType.PlayerUpdateReliable.doPacket(writer);
            writer.putUTF(MODULE_ID);
            writer.putUTF(command);
            writer.putInt(data.size());
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                writer.putUTF(entry.getKey());
                Object value = entry.getValue();
                if (value instanceof String) {
                    writer.putByte((byte)0);
                    writer.putUTF((String)value);
                    continue;
                }
                if (value instanceof Long) {
                    writer.putByte((byte)1);
                    writer.putLong(((Long)value).longValue());
                    continue;
                }
                if (value instanceof Integer) {
                    writer.putByte((byte)2);
                    writer.putInt(((Integer)value).intValue());
                    continue;
                }
                if (value instanceof Float) {
                    writer.putByte((byte)3);
                    writer.putFloat(((Float)value).floatValue());
                    continue;
                }
                if (!(value instanceof Boolean)) continue;
                writer.putByte((byte)4);
                writer.putBoolean(((Boolean)value).booleanValue());
            }
            Method sendMethod = PacketTypes.PacketType.PlayerUpdateReliable.getClass().getMethod("send", Object.class);
            sendMethod.invoke(PacketTypes.PacketType.PlayerUpdateReliable, connection);
        }
        catch (Exception e) {
            Logger.printLog("Error sending packet: " + e.getMessage());
        }
    }

    private Map<String, Object> readPacketData(ByteBuffer buffer) {
        HashMap<String, Object> data = new HashMap<String, Object>();
        try {
            int entries = buffer.getInt();
            block9: for (int i = 0; i < entries; ++i) {
                Object value;
                String key = GameWindow.ReadString((ByteBuffer)buffer);
                byte type = buffer.get();
                switch (type) {
                    case 0: {
                        value = GameWindow.ReadString((ByteBuffer)buffer);
                        break;
                    }
                    case 1: {
                        value = buffer.getLong();
                        break;
                    }
                    case 2: {
                        value = buffer.getInt();
                        break;
                    }
                    case 3: {
                        value = Float.valueOf(buffer.getFloat());
                        break;
                    }
                    case 4: {
                        value = buffer.get() != 0;
                        break;
                    }
                    default: {
                        continue block9;
                    }
                }
                data.put(key, value);
            }
        }
        catch (Exception e) {
            Logger.printLog("Error reading packet data: " + e.getMessage());
        }
        return data;
    }

    public void processIncomingPacket(String module, String command, ByteBuffer data) {
        if (!module.equals(MODULE_ID)) {
            return;
        }
        try {
            Map<String, Object> packetData = this.readPacketData(data);
            this.handleNetworkPacket(command, packetData);
        }
        catch (Exception e) {
            Logger.printLog("Error processing incoming packet: " + e.getMessage());
        }
    }

    private ByteBuffer mapToBuffer(Map<String, Object> data) {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(4096);
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                buffer.putInt(entry.getKey().length());
                buffer.put(entry.getKey().getBytes());
                Object value = entry.getValue();
                if (value instanceof String) {
                    buffer.put((byte)0);
                    buffer.putInt(((String)value).length());
                    buffer.put(((String)value).getBytes());
                    continue;
                }
                if (!(value instanceof Long)) continue;
                buffer.put((byte)1);
                buffer.putLong((Long)value);
            }
            buffer.flip();
            return buffer;
        }
        catch (Exception e) {
            Logger.printLog("Error converting map to buffer: " + e.getMessage());
            return ByteBuffer.allocate(0);
        }
    }

    static interface ProxyFunction {
        public Object invoke(Object ... var1);
    }
}
