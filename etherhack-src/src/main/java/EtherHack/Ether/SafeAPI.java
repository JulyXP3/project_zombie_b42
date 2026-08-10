/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  zombie.characters.IsoPlayer
 */
package EtherHack.Ether;

import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import zombie.characters.IsoPlayer;

public class SafeAPI {
    private static SafeAPI instance;
    private final Map<String, String> obfuscatedNames = new ConcurrentHashMap<String, String>();
    private final Map<String, Long> methodTimestamps = new ConcurrentHashMap<String, Long>();
    private final Random random = new Random();
    private final Map<String, String> currentKeys = new ConcurrentHashMap<String, String>();
    private static final long METHOD_LIFETIME = 10000L;

    private SafeAPI() {
        this.initializeObfuscation();
    }

    public static SafeAPI getInstance() {
        if (instance == null) {
            instance = new SafeAPI();
        }
        return instance;
    }

    private void initializeObfuscation() {
        String[] protectedMethods;
        for (String method : protectedMethods = new String[]{"getAntiCheat8Status", "getAntiCheat12Status", "getExtraTexture", "hackAdminAccess", "isDisableFakeInfectionLevel", "isDisableInfectionLevel", "isDisableWetness", "isEnableUnlimitedCarry", "isOptimalWeight", "isOptimalCalories", "isPlayerInSafeTeleported", "learnAllRecipes", "requireExtra", "safePlayerTeleport", "toggleEnableUnlimitedCarry", "toggleOptimalWeight", "toggleOptimalCalories", "vehicle", "toggleDisableFakeInfectionLevel", "toggleDisableInfectionLevel", "toggleDisableWetness", "repairPart", "setPartCondition", "repair", "setContainerContentAmount", "instanceof"}) {
            this.obfuscatedNames.put(method, this.generateSafeName());
            this.methodTimestamps.put(method, System.currentTimeMillis());
        }
    }

    private String generateSafeName() {
        String[] prefixes = new String[]{"_fn_", "lua_", "game_", "core_"};
        String prefix = prefixes[this.random.nextInt(prefixes.length)];
        return prefix + UUID.randomUUID().toString().substring(0, 8);
    }

    public String generateResponseKey(String serverFragment) {
        String username = IsoPlayer.getInstance().getUsername();
        String baseKey = "EtherHammerX_" + username;
        String clientFragment = UUID.randomUUID().toString();
        String fullKey = baseKey + serverFragment + clientFragment;
        this.currentKeys.put(username, fullKey);
        return clientFragment;
    }

    public boolean verifyHeartbeat(String key) {
        String username = IsoPlayer.getInstance().getUsername();
        String currentKey = this.currentKeys.get(username);
        return currentKey != null && currentKey.equals(key);
    }

    public String getSafeName(String originalName) {
        Long timestamp = this.methodTimestamps.get(originalName);
        if (timestamp == null || System.currentTimeMillis() - timestamp > 10000L) {
            String newName = this.generateSafeName();
            this.obfuscatedNames.put(originalName, newName);
            this.methodTimestamps.put(originalName, System.currentTimeMillis());
            return newName;
        }
        return this.obfuscatedNames.get(originalName);
    }

    public String getOriginalName(String safeName) {
        for (Map.Entry<String, String> entry : this.obfuscatedNames.entrySet()) {
            if (!entry.getValue().equals(safeName)) continue;
            return entry.getKey();
        }
        return null;
    }

    public List<String> cleanGlobalTable(List<String> globals) {
        HashSet<String> protected_names = new HashSet<String>(this.obfuscatedNames.values());
        return globals.stream().filter(name -> !protected_names.contains(name)).collect(Collectors.toList());
    }

    public String generateVerificationKey() {
        byte[] keyBytes = new byte[16];
        this.random.nextBytes(keyBytes);
        return Base64.getEncoder().encodeToString(keyBytes);
    }
}
