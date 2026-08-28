/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  se.krka.kahlua.integration.annotations.LuaMethod
 *  se.krka.kahlua.vm.KahluaTable
 *  zombie.Lua.LuaManager
 *  zombie.characters.IsoPlayer
 *  zombie.core.Color
 *  zombie.core.textures.Texture
 *  zombie.inventory.InventoryItem
 *  zombie.inventory.InventoryItemFactory
 *  zombie.inventory.types.HandWeapon
 *  zombie.iso.IsoGridSquare
 *  zombie.network.GameClient
 *  zombie.network.ServerOptions
 *  zombie.network.ServerOptions$EnumServerOption
 *  zombie.scripting.ScriptManager
 *  zombie.scripting.objects.Recipe
 */
package EtherHack.Ether;

import EtherHack.Ether.EtherLuaCompiler;
import EtherHack.Ether.EtherMain;
import EtherHack.Ether.SafeAPI;
import EtherHack.Ether.ServerAntiCheatBypass;
import EtherHack.GameClientWrapper;
import EtherHack.utils.FieldCache;
import EtherHack.utils.Logger;
import EtherHack.utils.PlayerUtils;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.Properties;
import se.krka.kahlua.integration.annotations.LuaMethod;
import se.krka.kahlua.vm.KahluaTable;
import zombie.Lua.LuaManager;
import zombie.characters.IsoPlayer;
import zombie.core.Color;
import zombie.core.textures.Texture;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.types.HandWeapon;
import zombie.inventory.types.Food;
import zombie.iso.IsoGridSquare;
import zombie.network.GameClient;
import zombie.network.PacketTypes;
import zombie.network.ServerOptions;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.Recipe;

public class EtherLuaMethods {
    private static EtherLuaMethods instance = null;
    private final SafeAPI safeAPI = SafeAPI.getInstance();
    private static final Map<String, Object> methodCache = new HashMap<String, Object>();
    private static Field playerLastUpdateField = null;
    private static Field connectionValidatedField = null;
    private static Field connectionTrustedField = null;
    private static final Map<String, Float> originalXpValues = new ConcurrentHashMap<String, Float>();
    private static final Map<String, Float> targetXpValues = new ConcurrentHashMap<String, Float>();
    private static final Map<String, Long> lastSyncTimes = new ConcurrentHashMap<String, Long>();
    private static volatile boolean bypassActive = false;
    private static volatile boolean stealthMode = false;
    private static final float MAX_XP_CHANGE_PER_SYNC = 2.5f;
    private static final long MIN_SYNC_INTERVAL_MS = 500L;
    private static final float TRAIT_POINT_THRESHOLD = 5.0f;

    private static void setPlayerCheat(IsoPlayer player, String cheatTypeName, boolean enable) {
        try {
            Field cheatsField = FieldCache.getField(player.getClass(), "cheats");
            if (cheatsField == null) {
                Logger.printLog("Failed to find 'cheats' field in player");
                return;
            }
            Object playerCheats = FieldCache.getFieldValue(player, cheatsField);
            if (playerCheats == null) {
                Logger.printLog("Failed to get PlayerCheats object");
                return;
            }
            Class<?> playerCheatsClass = Class.forName("zombie.characters.PlayerCheats");
            Field enumSetField = FieldCache.getField(playerCheatsClass, "cheats");
            if (enumSetField == null) {
                Logger.printLog("Failed to find 'cheats' EnumSet field in PlayerCheats");
                return;
            }
            Object enumSetObj = FieldCache.getFieldValue(playerCheats, enumSetField);
            if (enumSetObj == null) {
                Logger.printLog("Failed to get EnumSet object");
                return;
            }
            Class<?> cheatTypeClass = Class.forName("zombie.characters.CheatType");
            Method valueOfMethod = cheatTypeClass.getMethod("valueOf", String.class);
            Object cheatTypeEnum = valueOfMethod.invoke(null, cheatTypeName);
            if (cheatTypeEnum == null) {
                Logger.printLog("Failed to get CheatType enum value for: " + cheatTypeName);
                return;
            }
            Set enumSet = (Set)enumSetObj;
            if (enable) {
                boolean added = enumSet.add(cheatTypeEnum);
                Logger.printLog("[+] " + cheatTypeName + (added ? " enabled" : " already enabled"));
            } else {
                boolean removed = enumSet.remove(cheatTypeEnum);
                Logger.printLog("[-] " + cheatTypeName + (removed ? " disabled" : " already disabled"));
            }
        }
        catch (Exception e) {
            Logger.printLog("Error setting player cheat: " + e.getMessage());
            Logger.logException(e);
        }
    }

    @LuaMethod(name="getZombieUIColor", global=true)
    public static Color getZombieUIColor() {
        return EtherMain.getInstance().etherAPI.zombiesUIColor;
    }

    @LuaMethod(name="setZombieUIColor", global=true)
    public static void setZombieUIColor(float var0, float var1, float var2) {
        Color var3;
        EtherMain.getInstance().etherAPI.zombiesUIColor = var3 = new Color(var0, var1, var2);
    }

    @LuaMethod(name="getVehicleUIColor", global=true)
    public static Color getVehicleUIColor() {
        return EtherMain.getInstance().etherAPI.vehiclesUIColor;
    }

    @LuaMethod(name="setVehicleUIColor", global=true)
    public static void setVehicleUIColor(float var0, float var1, float var2) {
        Color var3;
        EtherMain.getInstance().etherAPI.vehiclesUIColor = var3 = new Color(var0, var1, var2);
    }

    @LuaMethod(name="getPlayersUIColor", global=true)
    public static Color getPlayersUIColor() {
        return EtherMain.getInstance().etherAPI.playersUIColor;
    }

    @LuaMethod(name="setPlayersUIColor", global=true)
    public static void setPlayersUIColor(float var0, float var1, float var2) {
        Color var3;
        EtherMain.getInstance().etherAPI.playersUIColor = var3 = new Color(var0, var1, var2);
    }

    @LuaMethod(name="setAccentUIColor", global=true)
    public static void setAccentUIColor(float var0, float var1, float var2) {
        Color var3;
        EtherMain.getInstance().etherAPI.mainUIAccentColor = var3 = new Color(var0, var1, var2);
    }

    @LuaMethod(name="deleteConfig", global=true)
    public static void deleteConfig(String var0) {
        Path var1 = Paths.get("EtherHack/config/" + var0 + ".properties", new String[0]);
        try {
            Files.deleteIfExists(var1);
        }
        catch (IOException var3) {
            Logger.printLog("The file '" + var0 + "' does not exist. Deletion canceled. Exception: " + var3.getMessage());
        }
    }

    @LuaMethod(name="getConfigList", global=true)
    public static ArrayList<String> getConfigList() {
        ArrayList<String> configFiles = new ArrayList<String>();
        try {
            Path configFolderPath = Paths.get("EtherHack/config", new String[0]);
            if (!Files.exists(configFolderPath, new LinkOption[0])) {
                Files.createDirectories(configFolderPath, new FileAttribute[0]);
                return configFiles;
            }
            List<Path> fileList = Files.list(configFolderPath).filter(file -> file.toString().endsWith(".properties")).toList();
            for (Path filePath : fileList) {
                String fileName = filePath.getFileName().toString().replace(".properties", "");
                configFiles.add(fileName);
            }
            return configFiles;
        }
        catch (IOException e) {
            Logger.printLog("An error occurred while getting the list of config files: " + String.valueOf(e));
            return null;
        }
    }

    @LuaMethod(name="loadConfig", global=true)
    public static void loadConfig(String var0) {
        EtherMain.getInstance().etherAPI.loadConfig(var0);
    }

    @LuaMethod(name="saveConfig", global=true)
    public static void saveConfig(String var0) {
        EtherMain.getInstance().etherAPI.saveConfig(var0);
    }

    @LuaMethod(name="safePlayerTeleport", global=true)
    public static void safePlayerTeleport(int x, int y) {
        String key = SafeAPI.getInstance().generateVerificationKey();
        try {
            EtherMain.getInstance().etherAPI.isPlayerInSafeTeleported = true;
            IsoPlayer player = IsoPlayer.getInstance();
            float z = player.getZ();
            float dx = (float)x - player.getX();
            float dy = (float)y - player.getY();
            float dz = z - player.getZ();
            float absDx = Math.abs(dx);
            float absDy = Math.abs(dy);
            float absDz = Math.abs(dz);
            while (absDx > 0.0f || absDy > 0.0f || absDz > 0.0f) {
                float step = 1.0f;
                float stepX = Math.min(Math.min(absDx, step), 1.0f);
                float stepY = Math.min(Math.min(absDy, step), 1.0f);
                float stepZ = Math.min(Math.min(absDz, step), 1.0f);
                absDx -= stepX;
                absDy -= stepY;
                absDz -= stepZ;
                if (dx < 0.0f) {
                    stepX = -stepX;
                }
                if (dy < 0.0f) {
                    stepY = -stepY;
                }
                if (dz < 0.0f) {
                    stepZ = -stepZ;
                }
                player.setX(player.getX() + stepX);
                player.setY(player.getY() + stepY);
                player.setZ(player.getZ() + stepZ);
                player.setLastX(player.getX());
                player.setLastY(player.getY());
                player.setLastZ(player.getZ());
                GameClient instance = GameClientWrapper.getInstance();
                if (instance == null) continue;
                instance.sendPlayer(player);
            }
            EtherMain.getInstance().etherAPI.isPlayerInSafeTeleported = false;
        }
        catch (Exception e) {
            Logger.printLog("Error in safePlayerTeleport: " + e.getMessage());
            EtherMain.getInstance().etherAPI.isPlayerInSafeTeleported = false;
        }
    }

    @LuaMethod(name="isPlayerInSafeTeleported", global=true)
    public static boolean isPlayerInSafeTeleported() {
        return EtherMain.getInstance().etherAPI.isPlayerInSafeTeleported;
    }

    @LuaMethod(name="learnAllRecipes", global=true)
    public static void learnAllRecipes() {
        String key = SafeAPI.getInstance().generateVerificationKey();
        try {
            ArrayList<Recipe> recipes;
            IsoPlayer player = IsoPlayer.getInstance();
            if (player != null && (recipes = ScriptManager.instance.getAllRecipes()) != null) {
                for (Recipe recipe : recipes) {
                    if (recipe.getOriginalname() == null) continue;
                    player.learnRecipe(recipe.getOriginalname());
                }
            }
        }
        catch (Exception e) {
            Logger.printLog("Error in learnAllRecipes: " + e.getMessage());
        }
    }

    @LuaMethod(name="giveItem", global=true)
    public static void giveItem(InventoryItem item, int count) {
        String key = SafeAPI.getInstance().generateVerificationKey();
        try {
            IsoPlayer player = IsoPlayer.getInstance();
            if (player != null) {
                for (int i = 0; i < count; ++i) {
                    player.getInventory().AddItem(item);
                }
            }
        }
        catch (Exception e) {
            Logger.printLog("Error in giveItem: " + e.getMessage());
        }
    }

    @LuaMethod(name="giveItem", global=true)
    public static void giveItem(String var0, int var1) {
        IsoPlayer var2 = IsoPlayer.getInstance();
        if (var2 != null) {
            for (int var3 = 0; var3 < var1; ++var3) {
                var2.getInventory().AddItem(var0);
            }
        }
    }

    @LuaMethod(name="giveItemAndEquip", global=true)
    public static InventoryItem giveItemAndEquip(String itemType) {
        IsoPlayer player = IsoPlayer.getInstance();
        if (player != null) {
            try {
                InventoryItem item = player.getInventory().AddItem(itemType);
                if (item != null) {
                    if (item instanceof HandWeapon) {
                        player.setPrimaryHandItem(item);
                    } else if (item.getCategory() != null && item.getCategory().equals("Clothing")) {
                        player.dressInClothingItem(itemType);
                    }
                }
                return item;
            }
            catch (Exception e) {
                Logger.printLog("Error in giveItemAndEquip: " + e.getMessage());
            }
        }
        return null;
    }

    @LuaMethod(name="spawnItem", global=true)
    public static void spawnItem(String itemType, int count) {
        try {
            IsoGridSquare square;
            IsoPlayer player = IsoPlayer.getInstance();
            if (player != null && (square = player.getCurrentSquare()) != null) {
                float baseX = 0.5f;
                float baseY = 0.5f;
                for (int i = 0; i < count; ++i) {
                    InventoryItem item = InventoryItemFactory.CreateItem((String)itemType);
                    if (item == null) continue;
                    float offsetX = baseX + (float)i * 0.1f % 0.4f - 0.2f;
                    float offsetY = baseY + (float)(i / 2) * 0.1f % 0.4f - 0.2f;
                    square.AddWorldInventoryItem(item, offsetX, offsetY, 0.0f);
                }
                Logger.printLog("Spawned " + count + "x " + itemType + " on ground");
            }
        }
        catch (Exception e) {
            Logger.printLog("Error in spawnItem: " + e.getMessage());
        }
    }

    @LuaMethod(name="itemBaseHunger", global=true)
    public static float itemBaseHunger(InventoryItem item) {
        try {
            if (item != null && item instanceof Food) {
                return ((Food)item).getBaseHunger();
            }
            return 0.0f;
        }
        catch (Exception e) {
            return 0.0f;
        }
    }

    @LuaMethod(name="spawnItem", global=true)
    public static void spawnItem(String itemType) {
        EtherLuaMethods.spawnItem(itemType, 1);
    }

    @LuaMethod(name="spawnItem", global=true)
    public static void spawnItem(InventoryItem item, int count) {
        try {
            IsoGridSquare square;
            IsoPlayer player = IsoPlayer.getInstance();
            if (player != null && item != null && (square = player.getCurrentSquare()) != null) {
                float baseX = 0.5f;
                float baseY = 0.5f;
                for (int i = 0; i < count; ++i) {
                    float offsetX = baseX + (float)i * 0.1f % 0.4f - 0.2f;
                    float offsetY = baseY + (float)(i / 2) * 0.1f % 0.4f - 0.2f;
                    square.AddWorldInventoryItem(item, offsetX, offsetY, 0.0f);
                }
                Logger.printLog("Spawned " + count + "x " + item.getType() + " on ground");
            }
        }
        catch (Exception e) {
            Logger.printLog("Error in spawnItem: " + e.getMessage());
        }
    }

    @LuaMethod(name="equipPrimaryHand", global=true)
    public static void equipPrimaryHand(InventoryItem item) {
        IsoPlayer player = IsoPlayer.getInstance();
        if (player != null && item != null) {
            try {
                if (!player.getInventory().contains(item)) {
                    player.getInventory().AddItem(item);
                }
                player.setPrimaryHandItem(item);
            }
            catch (Exception e) {
                Logger.printLog("Error in equipPrimaryHand: " + e.getMessage());
            }
        }
    }

    @LuaMethod(name="equipSecondaryHand", global=true)
    public static void equipSecondaryHand(InventoryItem item) {
        IsoPlayer player = IsoPlayer.getInstance();
        if (player != null && item != null) {
            try {
                if (!player.getInventory().contains(item)) {
                    player.getInventory().AddItem(item);
                }
                player.setSecondaryHandItem(item);
            }
            catch (Exception e) {
                Logger.printLog("Error in equipSecondaryHand: " + e.getMessage());
            }
        }
    }

    @LuaMethod(name="getDistanceBetweenPlayers", global=true)
    public static float getDistanceBetweenPlayers(IsoPlayer var0, IsoPlayer var1) {
        return PlayerUtils.getDistanceBetweenPlayers(var0, var1);
    }

    @LuaMethod(name="isBlockCompileLuaWithBadWords", global=true)
    public static boolean isBlockCompileLuaWithBadWords() {
        return EtherLuaCompiler.getInstance().isBlockCompileLuaWithBadWords;
    }

    @LuaMethod(name="toggleBlockCompileLuaWithBadWords", global=true)
    public static void toggleBlockCompileLuaWithBadWords(boolean var0) {
        EtherLuaCompiler.getInstance().isBlockCompileLuaWithBadWords = var0;
    }

    @LuaMethod(name="isBlockCompileLuaAboutEtherHack", global=true)
    public static boolean isBlockCompileLuaAboutEtherHack() {
        return EtherLuaCompiler.getInstance().isBlockCompileLuaAboutEtherHack;
    }

    @LuaMethod(name="toggleBlockCompileLuaAboutEtherHack", global=true)
    public static void toggleBlockCompileLuaAboutEtherHack(boolean var0) {
        EtherLuaCompiler.getInstance().isBlockCompileLuaAboutEtherHack = var0;
    }

    @LuaMethod(name="isBlockCompileDefaultLua", global=true)
    public static boolean isBlockCompileDefaultLua() {
        return EtherLuaCompiler.getInstance().isBlockCompileDefaultLua;
    }

    @LuaMethod(name="toggleBlockCompileDefaultLua", global=true)
    public static void toggleBlockCompileDefaultLua(boolean var0) {
        EtherLuaCompiler.getInstance().isBlockCompileDefaultLua = var0;
    }

    @LuaMethod(name="isEnableInvisible", global=true)
    public static boolean isEnableInvisible() {
        return EtherMain.getInstance().etherAPI.isEnableInvisible;
    }

    @LuaMethod(name="toggleInvisible", global=true)
    public static void toggleInvisible(boolean var0) {
        EtherMain.getInstance().etherAPI.isEnableInvisible = var0;
        saveConfig("startup");
        IsoPlayer player = IsoPlayer.getInstance();
        if (player != null) {
            EtherLuaMethods.setPlayerCheat(player, "INVISIBLE", var0);
        }
    }

    @LuaMethod(name="isZombieDontAttack", global=true)
    public static boolean isZombieDontAttack() {
        return EtherMain.getInstance().etherAPI.isZombieDontAttack;
    }

    @LuaMethod(name="toggleZombieDontAttack", global=true)
    public static void toggleZombieDontAttack(boolean var0) {
        EtherMain.getInstance().etherAPI.isZombieDontAttack = var0;
        saveConfig("startup");
        IsoPlayer player = IsoPlayer.getInstance();
        if (player != null) {
            EtherLuaMethods.setPlayerCheat(player, "ZOMBIES_DONT_ATTACK", var0);
        }
    }

    @LuaMethod(name="isEnableNoclip", global=true)
    public static boolean isEnableNoclip() {
        return EtherMain.getInstance().etherAPI.isEnableNoclip;
    }

    @LuaMethod(name="toggleNoclip", global=true)
    public static void toggleNoclip(boolean var0) {
        EtherMain.getInstance().etherAPI.isEnableNoclip = var0;
        saveConfig("startup");
        IsoPlayer player = IsoPlayer.getInstance();
        if (player != null) {
            EtherLuaMethods.setPlayerCheat(player, "NO_CLIP", var0);
        }
    }

    @LuaMethod(name="isEnableGodMode", global=true)
    public static boolean isEnableGodMode() {
        return EtherMain.getInstance().etherAPI.isEnableGodMode;
    }

    @LuaMethod(name="toggleGodMode", global=true)
    public static void toggleGodMode(boolean var0) {
        EtherMain.getInstance().etherAPI.isEnableGodMode = var0;
        saveConfig("startup");
        IsoPlayer player = IsoPlayer.getInstance();
        if (player != null) {
            EtherLuaMethods.setPlayerCheat(player, "GOD_MODE", var0);
        }
    }

    @LuaMethod(name="isEnableNightVision", global=true)
    public static boolean isEnableNightVision() {
        return EtherMain.getInstance().etherAPI.isEnableNightVision;
    }

    @LuaMethod(name="toggleNightVision", global=true)
    public static void toggleNightVision(boolean var0) {
        EtherMain.getInstance().etherAPI.isEnableNightVision = var0;
        saveConfig("startup");
    }

    @LuaMethod(name="isNoRecoil", global=true)
    public static boolean isNoRecoil() {
        return EtherMain.getInstance().etherAPI.isNoRecoil;
    }

    @LuaMethod(name="toggleNoRecoil", global=true)
    public static void toggleNoRecoil(boolean var0) {
        EtherMain.getInstance().etherAPI.isNoRecoil = var0;
        saveConfig("startup");
    }

    @LuaMethod(name="isNoJam", global=true)
    public static boolean isNoJam() {
        return EtherMain.getInstance().etherAPI.isNoJam;
    }

    @LuaMethod(name="toggleNoJam", global=true)
    public static void toggleNoJam(boolean var0) {
        EtherMain.getInstance().etherAPI.isNoJam = var0;
        saveConfig("startup");
    }

    @LuaMethod(name="isNoMuscleStrain", global=true)
    public static boolean isNoMuscleStrain() {
        return EtherMain.getInstance().etherAPI.isNoMuscleStrain;
    }

    @LuaMethod(name="toggleNoMuscleStrain", global=true)
    public static void toggleNoMuscleStrain(boolean var0) {
        EtherMain.getInstance().etherAPI.isNoMuscleStrain = var0;
        saveConfig("startup");
    }

    @LuaMethod(name="isFullBodyRestore", global=true)
    public static boolean isFullBodyRestore() {
        return EtherMain.getInstance().etherAPI.isFullBodyRestore;
    }

    @LuaMethod(name="toggleFullBodyRestore", global=true)
    public static void toggleFullBodyRestore(boolean var0) {
        EtherMain.getInstance().etherAPI.isFullBodyRestore = var0;
        saveConfig("startup");
    }

    @LuaMethod(name="isCharCreateAllTraits", global=true)
    public static boolean isCharCreateAllTraits() {
        return EtherMain.getInstance().etherAPI.isCharCreateAllTraits;
    }

    @LuaMethod(name="toggleCharCreateAllTraits", global=true)
    public static void toggleCharCreateAllTraits(boolean var0) {
        EtherMain.getInstance().etherAPI.isCharCreateAllTraits = var0;
        saveConfig("startup");
    }

    @LuaMethod(name="isCharCreateMaxSkills", global=true)
    public static boolean isCharCreateMaxSkills() {
        return EtherMain.getInstance().etherAPI.isCharCreateMaxSkills;
    }

    @LuaMethod(name="toggleCharCreateMaxSkills", global=true)
    public static void toggleCharCreateMaxSkills(boolean var0) {
        EtherMain.getInstance().etherAPI.isCharCreateMaxSkills = var0;
        saveConfig("startup");
    }

    @LuaMethod(name="isCharCreateAllClothes", global=true) public static boolean isCharCreateAllClothes() { return EtherMain.getInstance().etherAPI.isCharCreateAllClothes; }

    @LuaMethod(name="toggleCharCreateAllClothes", global=true) public static void toggleCharCreateAllClothes(boolean var0) { EtherMain.getInstance().etherAPI.isCharCreateAllClothes = var0; saveConfig("startup"); }

      @LuaMethod(name="getCharacterBoostCustomTraits", global=true)
     public static se.krka.kahlua.vm.KahluaTable getCharacterBoostCustomTraits() {
         se.krka.kahlua.vm.KahluaTable table = LuaManager.platform.newTable();
         ArrayList<String> list = EtherMain.getInstance().etherAPI.charCreateCustomTraits;
         for (int i = 0; i < list.size(); ++i) {
             table.rawset((Object)(i + 1), (Object)list.get(i));
         }
         return table;
     }

      @LuaMethod(name="addCharacterBoostCustomTrait", global=true)
     public static void addCharacterBoostCustomTrait(String traitType) {
         if (traitType == null || traitType.isEmpty()) return;
         ArrayList<String> list = EtherMain.getInstance().etherAPI.charCreateCustomTraits;
         if (!list.contains(traitType)) {
             list.add(traitType);
             saveConfig("startup");
         }
     }

      @LuaMethod(name="removeCharacterBoostCustomTrait", global=true)
     public static void removeCharacterBoostCustomTrait(String traitType) {
         if (traitType == null) return;
         EtherMain.getInstance().etherAPI.charCreateCustomTraits.remove(traitType);
         saveConfig("startup");
     }

      @LuaMethod(name="getCharacterBoostCustomSkillLevels", global=true)
     public static se.krka.kahlua.vm.KahluaTable getCharacterBoostCustomSkillLevels() {
         se.krka.kahlua.vm.KahluaTable table = LuaManager.platform.newTable();
         for (java.util.Map.Entry<String, Integer> e : EtherMain.getInstance().etherAPI.charCreateCustomSkillLevels.entrySet()) {
             table.rawset((Object)e.getKey(), (Object)e.getValue());
         }
         return table;
     }

      @LuaMethod(name="setCharacterBoostCustomSkillLevel", global=true)
     public static void setCharacterBoostCustomSkillLevel(String perkName, int level) {
         if (perkName == null || perkName.isEmpty()) return;
         if (level <= 0) {
             EtherMain.getInstance().etherAPI.charCreateCustomSkillLevels.remove(perkName);
         } else {
             if (level > 10) level = 10;
             EtherMain.getInstance().etherAPI.charCreateCustomSkillLevels.put(perkName, level);
         }
         saveConfig("startup");
     }
      @LuaMethod(name="clearCharacterBoostCustom", global=true)
     public static void clearCharacterBoostCustom() {
         EtherMain.getInstance().etherAPI.clearCharCreateCustom();
     }

    @LuaMethod(name="isVehicleInstantStart", global=true)
    public static boolean isVehicleInstantStart() {
        return EtherMain.getInstance().etherAPI.isVehicleInstantStart;
    }

    @LuaMethod(name="toggleVehicleInstantStart", global=true)
    public static void toggleVehicleInstantStart(boolean var0) {
        EtherMain.getInstance().etherAPI.isVehicleInstantStart = var0;
        saveConfig("startup");
    }

    @LuaMethod(name="isHeadshotOnly", global=true)
    public static boolean isHeadshotOnly() {
        return EtherMain.getInstance().etherAPI.isHeadshotOnly;
    }

    @LuaMethod(name="toggleHeadshotOnly", global=true)
    public static void toggleHeadshotOnly(boolean var0) {
        EtherMain.getInstance().etherAPI.isHeadshotOnly = var0;
        saveConfig("startup");
    }

    @LuaMethod(name="isAutoRepairItems", global=true)
    public static boolean isAutoRepairItems() {
        return EtherMain.getInstance().etherAPI.isAutoRepairItems;
    }

    @LuaMethod(name="toggleAutoRepairItems", global=true)
    public static void toggleAutoRepairItems(boolean var0) {
        EtherMain.getInstance().etherAPI.isAutoRepairItems = var0;
        saveConfig("startup");
    }

    @LuaMethod(name="isRepairClothing", global=true)
    public static boolean isRepairClothing() {
        return EtherMain.getInstance().etherAPI.isRepairClothing;
    }

    @LuaMethod(name="toggleRepairClothing", global=true)
    public static void toggleRepairClothing(boolean var0) {
        EtherMain.getInstance().etherAPI.isRepairClothing = var0;
        saveConfig("startup");
    }

    @LuaMethod(name="resetWeaponsStats", global=true)
    public static void resetWeaponsStats() {
        EtherMain.getInstance().etherAPI.resetWeaponsStats();
    }

    @LuaMethod(name="isExtraDamage", global=true)
    public static boolean isExtraDamage() {
        return EtherMain.getInstance().etherAPI.isExtraDamage;
    }

    @LuaMethod(name="toggleExtraDamage", global=true)
    public static void toggleExtraDamage(boolean var0) {
        EtherMain.getInstance().etherAPI.isExtraDamage = var0;
        saveConfig("startup");
    }

    @LuaMethod(name="isTimedActionCheat", global=true)
    public static boolean isTimedActionCheat() {
        return EtherMain.getInstance().etherAPI.isTimedActionCheat;
    }

    @LuaMethod(name="toggleTimedActionCheat", global=true)
    public static void toggleTimedActionCheat(boolean var0) {
        EtherMain.getInstance().etherAPI.isTimedActionCheat = var0;
        saveConfig("startup");
        IsoPlayer player = IsoPlayer.getInstance();
        if (player != null) {
            EtherLuaMethods.setPlayerCheat(player, "TIMED_ACTION_INSTANT", var0);
        }
    }

    @LuaMethod(name="isMultiHitZombies", global=true)
    public static boolean isMultiHitZombies() {
        return EtherMain.getInstance().etherAPI.isMultiHitZombies;
    }

    @LuaMethod(name="toggleMultiHitZombies", global=true)
    public static void toggleMultiHitZombies(boolean var0) {
        EtherMain.getInstance().etherAPI.isMultiHitZombies = var0;
        saveConfig("startup");
    }

    @LuaMethod(name="toggleCritMax", global=true)
    public static void toggleCritMax(boolean var0) {
        EtherMain.getInstance().etherAPI.isCritMax = var0;
        saveConfig("startup");
        if (!var0) {
            EtherMain.getInstance().etherAPI.resetCritMax();
        }
    }

    @LuaMethod(name="isCritMax", global=true)
    public static boolean isCritMax() {
        return EtherMain.getInstance().etherAPI.isCritMax;
    }

    @LuaMethod(name="getCombatSpeedMultiplier", global=true)
    public static float getCombatSpeedMultiplier() {
        return EtherMain.getInstance().etherAPI.combatSpeedMultiplier;
    }

    @LuaMethod(name="setCombatSpeedMultiplier", global=true)
    public static void setCombatSpeedMultiplier(float var0) {
        EtherMain.getInstance().etherAPI.combatSpeedMultiplier = var0;
        saveConfig("startup");
    }

    @LuaMethod(name="isUnlimitedCondition", global=true)
    public static boolean isUnlimitedCondition() {
        return EtherMain.getInstance().etherAPI.isUnlimitedCondition;
    }

    @LuaMethod(name="toggleUnlimitedCondition", global=true)
    public static void toggleUnlimitedCondition(boolean var0) {
        EtherMain.getInstance().etherAPI.isUnlimitedCondition = var0;
        saveConfig("startup");
        IsoPlayer player = IsoPlayer.getInstance();
        if (player != null) {
            EtherLuaMethods.setPlayerCheat(player, "UNLIMITED_ENDURANCE", var0);
        }
    }

    @LuaMethod(name="isVisualEnable360Vision", global=true)
    public static boolean isVisualEnable360Vision() {
        return EtherMain.getInstance().etherAPI.isVisualEnable360Vision;
    }

    @LuaMethod(name="toggleVisualEnable360Vision", global=true)
    public static void toggleVisualEnable360Vision(boolean var0) {
        EtherMain.getInstance().etherAPI.isVisualEnable360Vision = var0;
        saveConfig("startup");
    }

    @LuaMethod(name="isFullbright", global=true)
    public static boolean isFullbright() {
        return EtherMain.getInstance().etherAPI.isFullbright;
    }

    @LuaMethod(name="toggleFullbright", global=true)
    public static void toggleFullbright(boolean var0) {
        EtherMain.getInstance().etherAPI.isFullbright = var0;
        saveConfig("startup");
    }

    @LuaMethod(name="isVisualDrawLineToPlayers", global=true)
    public static boolean isVisualDrawLineToPlayers() {
        return EtherMain.getInstance().etherAPI.isVisualDrawLineToPlayers;
    }

    @LuaMethod(name="toggleVisualDrawLineToPlayers", global=true)
    public static void toggleVisualDrawLineToPlayers(boolean var0) {
        EtherMain.getInstance().etherAPI.isVisualDrawLineToPlayers = var0;
        saveConfig("startup");
    }

    @LuaMethod(name="isVisualDrawLineToVehicle", global=true)
    public static boolean isVisualDrawLineToVehicle() {
        return EtherMain.getInstance().etherAPI.isVisualDrawLineToVehicle;
    }

    @LuaMethod(name="toggleVisualDrawLineToVehicle", global=true)
    public static void toggleVisualDrawLineToVehicle(boolean var0) {
        EtherMain.getInstance().etherAPI.isVisualDrawLineToVehicle = var0;
        saveConfig("startup");
    }

    @LuaMethod(name="isMapDrawZombies", global=true)
    public static boolean isMapDrawZombies() {
        return EtherMain.getInstance().etherAPI.isMapDrawZombies;
    }

    @LuaMethod(name="toggleMapDrawZombies", global=true)
    public static void toggleMapDrawZombies(boolean var0) {
        EtherMain.getInstance().etherAPI.isMapDrawZombies = var0;
        saveConfig("startup");
    }

    @LuaMethod(name="isMapDrawVehicles", global=true)
    public static boolean isMapDrawVehicles() {
        return EtherMain.getInstance().etherAPI.isMapDrawVehicles;
    }

    @LuaMethod(name="toggleMapDrawVehicles", global=true)
    public static void toggleMapDrawVehicles(boolean var0) {
        EtherMain.getInstance().etherAPI.isMapDrawVehicles = var0;
        saveConfig("startup");
    }

    @LuaMethod(name="isMapDrawAllPlayers", global=true)
    public static boolean isMapDrawAllPlayers() {
        return EtherMain.getInstance().etherAPI.isMapDrawAllPlayers;
    }

    @LuaMethod(name="toggleMapDrawAllPlayers", global=true)
    public static void toggleMapDrawAllPlayers(boolean var0) {
        EtherMain.getInstance().etherAPI.isMapDrawAllPlayers = var0;
        saveConfig("startup");
    }

    @LuaMethod(name="isMapDrawLocalPlayer", global=true)
    public static boolean isMapDrawLocalPlayer() {
        return EtherMain.getInstance().etherAPI.isMapDrawLocalPlayer;
    }

    @LuaMethod(name="toggleMapDrawLocalPlayer", global=true)
    public static void toggleMapDrawLocalPlayer(boolean var0) {
        EtherMain.getInstance().etherAPI.isMapDrawLocalPlayer = var0;
        saveConfig("startup");
    }

    @LuaMethod(name="isMapDrawItems", global=true)
    public static boolean isMapDrawItems() {
        return EtherMain.getInstance().etherAPI.isMapDrawItems;
    }

    @LuaMethod(name="setMapDrawItems", global=true)
    public static void setMapDrawItems(boolean var0) {
        EtherMain.getInstance().etherAPI.isMapDrawItems = var0;
        saveConfig("startup");
    }

    @LuaMethod(name="isMinimapOpen", global=true)
    public static boolean isMinimapOpen() {
        return EtherMain.getInstance().etherAPI.isMinimapOpen;
    }

    @LuaMethod(name="setMinimapOpen", global=true)
    public static void setMinimapOpen(boolean var0) {
        EtherMain.getInstance().etherAPI.isMinimapOpen = var0;
        saveConfig("startup");
    }

    @LuaMethod(name="isVisualDrawPlayerInfo", global=true)
    public static boolean isVisualDrawPlayerInfo() {
        return EtherMain.getInstance().etherAPI.isVisualDrawPlayerInfo;
    }

    @LuaMethod(name="toggleVisualDrawPlayerInfo", global=true)
    public static void toggleVisualDrawPlayerInfo(boolean var0) {
        EtherMain.getInstance().etherAPI.isVisualDrawPlayerInfo = var0;
        saveConfig("startup");
    }

    @LuaMethod(name="isVisualsZombiesEnable", global=true)
    public static boolean isVisualsZombiesEnable() {
        return EtherMain.getInstance().etherAPI.isVisualsZombiesEnable;
    }

    @LuaMethod(name="toggleVisualsZombiesEnable", global=true)
    public static void toggleVisualsZombiesEnable(boolean var0) {
        EtherMain.getInstance().etherAPI.isVisualsZombiesEnable = var0;
        saveConfig("startup");
    }

    @LuaMethod(name="isVisualsVehiclesEnable", global=true)
    public static boolean isVisualsVehiclesEnable() {
        return EtherMain.getInstance().etherAPI.isVisualsVehiclesEnable;
    }

    @LuaMethod(name="toggleVisualsVehiclesEnable", global=true)
    public static void toggleVisualsVehiclesEnable(boolean var0) {
        EtherMain.getInstance().etherAPI.isVisualsVehiclesEnable = var0;
        saveConfig("startup");
    }

    @LuaMethod(name="isVisualDrawPlayerNickname", global=true)
    public static boolean isVisualDrawPlayerNickname() {
        return EtherMain.getInstance().etherAPI.isVisualDrawPlayerNickname;
    }

    @LuaMethod(name="toggleVisualDrawPlayerNickname", global=true)
    public static void toggleVisualDrawPlayerNickname(boolean var0) {
        EtherMain.getInstance().etherAPI.isVisualDrawPlayerNickname = var0;
        saveConfig("startup");
    }

    @LuaMethod(name="isVisualDrawLineToZombies", global=true)
    public static boolean isVisualDrawLineToZombies() {
        return EtherMain.getInstance().etherAPI.isVisualDrawLineToZombies;
    }

    @LuaMethod(name="toggleVisualDrawLineToZombies", global=true)
    public static void toggleVisualDrawLineToZombies(boolean var0) {
        EtherMain.getInstance().etherAPI.isVisualDrawLineToZombies = var0;
        saveConfig("startup");
    }

    @LuaMethod(name="isVisualsEnable", global=true)
    public static boolean isVisualsEnable() {
        return EtherMain.getInstance().etherAPI.isVisualsEnable;
    }

    @LuaMethod(name="toggleVisualsEnable", global=true)
    public static void toggleVisualsEnable(boolean var0) {
        EtherMain.getInstance().etherAPI.isVisualsEnable = var0;
        saveConfig("startup");
    }

    @LuaMethod(name="isBypassDebugMode", global=true)
    public static boolean isBypassDebugMode() {
        return EtherMain.getInstance().etherAPI.isBypassDebugMode;
    }

    @LuaMethod(name="toggleBypassDebugMode", global=true)
    public static void toggleBypassDebugMode(boolean var0) {
        EtherMain.getInstance().etherAPI.isBypassDebugMode = var0;
        saveConfig("startup");
    }

    @LuaMethod(name="toggleUnlimitedEndurance", global=true)
    public static void toggleUnlimitedEndurance(boolean var0) {
        EtherMain.getInstance().etherAPI.isUnlimitedEndurance = var0;
        saveConfig("startup");
    }

    @LuaMethod(name="isUnlimitedEndurance", global=true)
    public static boolean isUnlimitedEndurance() {
        return EtherMain.getInstance().etherAPI.isUnlimitedEndurance;
    }

    @LuaMethod(name="toggleUnlimitedAmmo", global=true)
    public static void toggleUnlimitedAmmo(boolean var0) {
        EtherMain.getInstance().etherAPI.isUnlimitedAmmo = var0;
        saveConfig("startup");
    }

    @LuaMethod(name="isUnlimitedAmmo", global=true)
    public static boolean isUnlimitedAmmo() {
        return EtherMain.getInstance().etherAPI.isUnlimitedAmmo;
    }

    @LuaMethod(name="getAmmoFarmCount", global=true)
    public static int getAmmoFarmCount() {
        return EtherMain.getInstance().etherAPI.ammoFarmCount;
    }

    @LuaMethod(name="setAmmoFarmCount", global=true)
    public static void setAmmoFarmCount(int var0) {
        if (var0 > 0) {
            EtherMain.getInstance().etherAPI.ammoFarmCount = var0;
            saveConfig("startup");
        }
    }

    @LuaMethod(name="farmSetAmmo", global=true)
    public static void farmSetAmmo() {
        EtherMain.getInstance().etherAPI.farmSetWeaponAmmo();
    }

    @LuaMethod(name="toggleDisableFatigue", global=true)
    public static void toggleDisableFatigue(boolean var0) {
        EtherMain.getInstance().etherAPI.isDisableFatigue = var0;
        saveConfig("startup");
    }

    @LuaMethod(name="isDisableFatigue", global=true)
    public static boolean isDisableFatigue() {
        return EtherMain.getInstance().etherAPI.isDisableFatigue;
    }

    @LuaMethod(name="toggleDisableHunger", global=true)
    public static void toggleDisableHunger(boolean var0) {
        EtherMain.getInstance().etherAPI.isDisableHunger = var0;
        saveConfig("startup");
    }

    @LuaMethod(name="isDisableHunger", global=true)
    public static boolean isDisableHunger() {
        return EtherMain.getInstance().etherAPI.isDisableHunger;
    }

    @LuaMethod(name="toggleDisableThirst", global=true)
    public static void toggleDisableThirst(boolean var0) {
        EtherMain.getInstance().etherAPI.isDisableThirst = var0;
        saveConfig("startup");
    }

    @LuaMethod(name="isDisableThirst", global=true)
    public static boolean isDisableThirst() {
        return EtherMain.getInstance().etherAPI.isDisableThirst;
    }

    @LuaMethod(name="toggleDisableDrunkenness", global=true)
    public static void toggleDisableDrunkenness(boolean var0) {
        EtherMain.getInstance().etherAPI.isDisableDrunkenness = var0;
        saveConfig("startup");
    }

    @LuaMethod(name="isDisableDrunkenness", global=true)
    public static boolean isDisableDrunkenness() {
        return EtherMain.getInstance().etherAPI.isDisableDrunkenness;
    }

    @LuaMethod(name="toggleDisableAnger", global=true)
    public static void toggleDisableAnger(boolean var0) {
        EtherMain.getInstance().etherAPI.isDisableAnger = var0;
        saveConfig("startup");
    }

    @LuaMethod(name="isDisableAnger", global=true)
    public static boolean isDisableAnger() {
        return EtherMain.getInstance().etherAPI.isDisableAnger;
    }

    @LuaMethod(name="toggleDisableFear", global=true)
    public static void toggleDisableFear(boolean var0) {
        EtherMain.getInstance().etherAPI.isDisableFear = var0;
        saveConfig("startup");
    }

    @LuaMethod(name="isDisableFear", global=true)
    public static boolean isDisableFear() {
        return EtherMain.getInstance().etherAPI.isDisableFear;
    }

    @LuaMethod(name="toggleDisablePain", global=true)
    public static void toggleDisablePain(boolean var0) {
        EtherMain.getInstance().etherAPI.isDisablePain = var0;
        saveConfig("startup");
    }

    @LuaMethod(name="isDisablePain", global=true)
    public static boolean isDisablePain() {
        return EtherMain.getInstance().etherAPI.isDisablePain;
    }

    @LuaMethod(name="toggleDisablePanic", global=true)
    public static void toggleDisablePanic(boolean var0) {
        EtherMain.getInstance().etherAPI.isDisablePanic = var0;
        saveConfig("startup");
    }

    @LuaMethod(name="isDisablePanic", global=true)
    public static boolean isDisablePanic() {
        return EtherMain.getInstance().etherAPI.isDisablePanic;
    }

    @LuaMethod(name="toggleDisableMorale", global=true)
    public static void toggleDisableMorale(boolean var0) {
        EtherMain.getInstance().etherAPI.isDisableMorale = var0;
        saveConfig("startup");
    }

    @LuaMethod(name="isDisableMorale", global=true)
    public static boolean isDisableMorale() {
        return EtherMain.getInstance().etherAPI.isDisableMorale;
    }

    @LuaMethod(name="toggleDisableStress", global=true)
    public static void toggleDisableStress(boolean var0) {
        EtherMain.getInstance().etherAPI.isDisableStress = var0;
        saveConfig("startup");
    }

    @LuaMethod(name="isDisableStress", global=true)
    public static boolean isDisableStress() {
        return EtherMain.getInstance().etherAPI.isDisableStress;
    }

    @LuaMethod(name="toggleDisableSickness", global=true)
    public static void toggleDisableSickness(boolean var0) {
        EtherMain.getInstance().etherAPI.isDisableSickness = var0;
        saveConfig("startup");
    }

    @LuaMethod(name="isDisableSickness", global=true)
    public static boolean isDisableSickness() {
        return EtherMain.getInstance().etherAPI.isDisableSickness;
    }

    @LuaMethod(name="toggleDisableStressFromCigarettes", global=true)
    public static void toggleDisableStressFromCigarettes(boolean var0) {
        EtherMain.getInstance().etherAPI.isDisableStressFromCigarettes = var0;
        saveConfig("startup");
    }

    @LuaMethod(name="isDisableStressFromCigarettes", global=true)
    public static boolean isDisableStressFromCigarettes() {
        return EtherMain.getInstance().etherAPI.isDisableStressFromCigarettes;
    }

    @LuaMethod(name="toggleDisableSanity", global=true)
    public static void toggleDisableSanity(boolean var0) {
        EtherMain.getInstance().etherAPI.isDisableSanity = var0;
        saveConfig("startup");
    }

    @LuaMethod(name="isDisableSanity", global=true)
    public static boolean isDisableSanity() {
        return EtherMain.getInstance().etherAPI.isDisableSanity;
    }

    @LuaMethod(name="toggleDisableBoredomLevel", global=true)
    public static void toggleDisableBoredomLevel(boolean var0) {
        EtherMain.getInstance().etherAPI.isDisableBoredomLevel = var0;
        saveConfig("startup");
    }

    @LuaMethod(name="isDisableBoredomLevel", global=true)
    public static boolean isDisableBoredomLevel() {
        return EtherMain.getInstance().etherAPI.isDisableBoredomLevel;
    }

    @LuaMethod(name="toggleDisableUnhappynessLevel", global=true)
    public static void toggleDisableUnhappynessLevel(boolean var0) {
        EtherMain.getInstance().etherAPI.isDisableUnhappynessLevel = var0;
        saveConfig("startup");
    }

    @LuaMethod(name="isDisableUnhappynessLevel", global=true)
    public static boolean isDisableUnhappynessLevel() {
        return EtherMain.getInstance().etherAPI.isDisableUnhappynessLevel;
    }

    @LuaMethod(name="toggleDisableWetness", global=true)
    public static void toggleDisableWetness(boolean value) {
        EtherMain.getInstance().etherAPI.isDisableWetness = value;
    }

    @LuaMethod(name="isDisableWetness", global=true)
    public static boolean isDisableWetness() {
        return EtherMain.getInstance().etherAPI.isDisableWetness;
    }

    @LuaMethod(name="toggleDisableInfectionLevel", global=true)
    public static void toggleDisableInfectionLevel(boolean value) {
        EtherMain.getInstance().etherAPI.isDisableInfectionLevel = value;
    }

    @LuaMethod(name="isDisableInfectionLevel", global=true)
    public static boolean isDisableInfectionLevel() {
        return EtherMain.getInstance().etherAPI.isDisableInfectionLevel;
    }

    @LuaMethod(name="toggleDisableFakeInfectionLevel", global=true)
    public static void toggleDisableFakeInfectionLevel(boolean var0) {
        EtherMain.getInstance().etherAPI.isDisableFakeInfectionLevel = var0;
        saveConfig("startup");
    }

    @LuaMethod(name="isDisableFakeInfectionLevel", global=true)
    public static boolean isDisableFakeInfectionLevel() {
        return EtherMain.getInstance().etherAPI.isDisableFakeInfectionLevel;
    }

    @LuaMethod(name="toggleOptimalCalories", global=true)
    public static void toggleOptimalCalories(boolean value) {
        EtherMain.getInstance().etherAPI.isOptimalCalories = value;
    }

    @LuaMethod(name="isOptimalCalories", global=true)
    public static boolean isOptimalCalories() {
        return EtherMain.getInstance().etherAPI.isOptimalCalories;
    }

    @LuaMethod(name="toggleOptimalWeight", global=true)
    public static void toggleOptimalWeight(boolean value) {
        EtherMain.getInstance().etherAPI.isOptimalWeight = value;
    }

    @LuaMethod(name="isOptimalWeight", global=true)
    public static boolean isOptimalWeight() {
        return EtherMain.getInstance().etherAPI.isOptimalWeight;
    }

    @LuaMethod(name="toggleEnableUnlimitedCarry", global=true)
    public static void toggleEnableUnlimitedCarry(boolean value) {
        EtherMain.getInstance().etherAPI.isUnlimitedCarry = value;
    }

    @LuaMethod(name="isEnableUnlimitedCarry", global=true)
    public static boolean isEnableUnlimitedCarry() {
        return EtherMain.getInstance().etherAPI.isUnlimitedCarry;
    }

    @LuaMethod(name="getAntiCheat12Status", global=true)
    public static boolean getAntiCheat12Status() {
        try {
            if (ServerOptions.instance == null || ServerOptions.instance.antiCheatPermission == null) {
                return false;
            }
            return ServerOptions.instance.antiCheatPermission.getValue() > 0;
        }
        catch (Exception e) {
            return false;
        }
    }

    @LuaMethod(name="getAntiCheat8Status", global=true)
    public static boolean getAntiCheat8Status() {
        return EtherLuaMethods.getAntiCheatMovementEnabled();
    }

    @LuaMethod(name="getAntiCheatMovementEnabled", global=true)
    public static boolean getAntiCheatMovementEnabled() {
        try {
            if (ServerOptions.instance == null || ServerOptions.instance.antiCheatSpeed == null) {
                return false;
            }
            return ServerOptions.instance.antiCheatSpeed.getValue() > 0;
        }
        catch (Exception e) {
            return false;
        }
    }

    @LuaMethod(name="getAntiCheatStatus", global=true)
    public static KahluaTable getAntiCheatStatus() {
        KahluaTable table = LuaManager.platform.newTable();
        try {
            if (ServerOptions.instance == null) {
                return table;
            }
            Function<ServerOptions.EnumServerOption, Integer> getValue = opt -> {
                try {
                    return opt != null ? opt.getValue() : 0;
                }
                catch (Exception e) {
                    return 0;
                }
            };
            table.rawset((Object)"movement", (Object)getValue.apply(ServerOptions.instance.antiCheatSpeed));
            table.rawset((Object)"safety", (Object)getValue.apply(ServerOptions.instance.antiCheatSafety));
            table.rawset((Object)"hit", (Object)getValue.apply(ServerOptions.instance.antiCheatHit));
            table.rawset((Object)"packet", (Object)getValue.apply(ServerOptions.instance.antiCheatPacketException));
            table.rawset((Object)"permission", (Object)getValue.apply(ServerOptions.instance.antiCheatPermission));
            table.rawset((Object)"xp", (Object)getValue.apply(ServerOptions.instance.antiCheatXp));
            table.rawset((Object)"fire", (Object)getValue.apply(ServerOptions.instance.antiCheatHit));
            table.rawset((Object)"safehouse", (Object)getValue.apply(ServerOptions.instance.antiCheatSafeHouse));
            table.rawset((Object)"recipe", (Object)getValue.apply(ServerOptions.instance.antiCheatXp));
            table.rawset((Object)"player", (Object)getValue.apply(ServerOptions.instance.antiCheatPlayer));
            table.rawset((Object)"checksum", (Object)getValue.apply(ServerOptions.instance.antiCheatChecksum));
            table.rawset((Object)"item", (Object)getValue.apply(ServerOptions.instance.antiCheatPlayer));
            table.rawset((Object)"serverCustomization", (Object)getValue.apply(ServerOptions.instance.antiCheatChecksum));
            table.rawset((Object)"POLICY_DISABLED", (Object)0);
            table.rawset((Object)"POLICY_BAN", (Object)1);
            table.rawset((Object)"POLICY_KICK", (Object)2);
            table.rawset((Object)"POLICY_LOG", (Object)3);
        }
        catch (Exception e) {
            Logger.printLog("Error getting anti-cheat status: " + e.getMessage());
        }
        return table;
    }

    @LuaMethod(name="getAntiCheatXpEnabled", global=true)
    public static boolean getAntiCheatXpEnabled() {
        try {
            if (ServerOptions.instance == null || ServerOptions.instance.antiCheatXp == null) {
                return false;
            }
            return ServerOptions.instance.antiCheatXp.getValue() > 0;
        }
        catch (Exception e) {
            return false;
        }
    }

    @LuaMethod(name="getAntiCheatPlayerEnabled", global=true)
    public static boolean getAntiCheatPlayerEnabled() {
        try {
            if (ServerOptions.instance == null || ServerOptions.instance.antiCheatPlayer == null) {
                return false;
            }
            return ServerOptions.instance.antiCheatPlayer.getValue() > 0;
        }
        catch (Exception e) {
            return false;
        }
    }

    @LuaMethod(name="getAntiCheatSafetyEnabled", global=true)
    public static boolean getAntiCheatSafetyEnabled() {
        try {
            if (ServerOptions.instance == null || ServerOptions.instance.antiCheatSafety == null) {
                return false;
            }
            return ServerOptions.instance.antiCheatSafety.getValue() > 0;
        }
        catch (Exception e) {
            return false;
        }
    }

    @LuaMethod(name="getAntiCheatHitEnabled", global=true)
    public static boolean getAntiCheatHitEnabled() {
        try {
            if (ServerOptions.instance == null || ServerOptions.instance.antiCheatHit == null) {
                return false;
            }
            return ServerOptions.instance.antiCheatHit.getValue() > 0;
        }
        catch (Exception e) {
            return false;
        }
    }

    @LuaMethod(name="getAntiCheatPacketEnabled", global=true)
    public static boolean getAntiCheatPacketEnabled() {
        try {
            if (ServerOptions.instance == null || ServerOptions.instance.antiCheatPacketException == null) {
                return false;
            }
            return ServerOptions.instance.antiCheatPacketException.getValue() > 0;
        }
        catch (Exception e) {
            return false;
        }
    }

    @LuaMethod(name="getAntiCheatPermissionEnabled", global=true)
    public static boolean getAntiCheatPermissionEnabled() {
        try {
            if (ServerOptions.instance == null || ServerOptions.instance.antiCheatPermission == null) {
                return false;
            }
            return ServerOptions.instance.antiCheatPermission.getValue() > 0;
        }
        catch (Exception e) {
            return false;
        }
    }

    @LuaMethod(name="getAntiCheatFireEnabled", global=true)
    public static boolean getAntiCheatFireEnabled() {
        try {
            if (ServerOptions.instance == null || ServerOptions.instance.antiCheatHit == null) {
                return false;
            }
            return ServerOptions.instance.antiCheatHit.getValue() > 0;
        }
        catch (Exception e) {
            return false;
        }
    }

    @LuaMethod(name="getAntiCheatSafeHouseEnabled", global=true)
    public static boolean getAntiCheatSafeHouseEnabled() {
        try {
            if (ServerOptions.instance == null || ServerOptions.instance.antiCheatSafeHouse == null) {
                return false;
            }
            return ServerOptions.instance.antiCheatSafeHouse.getValue() > 0;
        }
        catch (Exception e) {
            return false;
        }
    }

    @LuaMethod(name="getAntiCheatRecipeEnabled", global=true)
    public static boolean getAntiCheatRecipeEnabled() {
        try {
            if (ServerOptions.instance == null || ServerOptions.instance.antiCheatXp == null) {
                return false;
            }
            return ServerOptions.instance.antiCheatXp.getValue() > 0;
        }
        catch (Exception e) {
            return false;
        }
    }

    @LuaMethod(name="getAntiCheatChecksumEnabled", global=true)
    public static boolean getAntiCheatChecksumEnabled() {
        try {
            if (ServerOptions.instance == null || ServerOptions.instance.antiCheatChecksum == null) {
                return false;
            }
            return ServerOptions.instance.antiCheatChecksum.getValue() > 0;
        }
        catch (Exception e) {
            return false;
        }
    }

    @LuaMethod(name="getAntiCheatItemEnabled", global=true)
    public static boolean getAntiCheatItemEnabled() {
        try {
            if (ServerOptions.instance == null || ServerOptions.instance.antiCheatPlayer == null) {
                return false;
            }
            return ServerOptions.instance.antiCheatPlayer.getValue() > 0;
        }
        catch (Exception e) {
            return false;
        }
    }

    @LuaMethod(name="getAntiCheatServerCustomizationEnabled", global=true)
    public static boolean getAntiCheatServerCustomizationEnabled() {
        try {
            if (ServerOptions.instance == null || ServerOptions.instance.antiCheatChecksum == null) {
                return false;
            }
            return ServerOptions.instance.antiCheatChecksum.getValue() > 0;
        }
        catch (Exception e) {
            return false;
        }
    }

    @LuaMethod(name="getAntiCheatSummary", global=true)
    public static String getAntiCheatSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Movement: ").append(EtherLuaMethods.getAntiCheatMovementEnabled() ? "ON" : "OFF");
        sb.append(" | Safety: ").append(EtherLuaMethods.getAntiCheatSafetyEnabled() ? "ON" : "OFF");
        sb.append(" | Hit: ").append(EtherLuaMethods.getAntiCheatHitEnabled() ? "ON" : "OFF");
        sb.append(" | Packet: ").append(EtherLuaMethods.getAntiCheatPacketEnabled() ? "ON" : "OFF");
        sb.append(" | Permission: ").append(EtherLuaMethods.getAntiCheatPermissionEnabled() ? "ON" : "OFF");
        sb.append(" | XP: ").append(EtherLuaMethods.getAntiCheatXpEnabled() ? "ON" : "OFF");
        sb.append(" | Fire: ").append(EtherLuaMethods.getAntiCheatFireEnabled() ? "ON" : "OFF");
        sb.append(" | SafeHouse: ").append(EtherLuaMethods.getAntiCheatSafeHouseEnabled() ? "ON" : "OFF");
        sb.append(" | Recipe: ").append(EtherLuaMethods.getAntiCheatRecipeEnabled() ? "ON" : "OFF");
        sb.append(" | Player: ").append(EtherLuaMethods.getAntiCheatPlayerEnabled() ? "ON" : "OFF");
        sb.append(" | Checksum: ").append(EtherLuaMethods.getAntiCheatChecksumEnabled() ? "ON" : "OFF");
        sb.append(" | Item: ").append(EtherLuaMethods.getAntiCheatItemEnabled() ? "ON" : "OFF");
        sb.append(" | ServerConfig: ").append(EtherLuaMethods.getAntiCheatServerCustomizationEnabled() ? "ON" : "OFF");
        return sb.toString();
    }

    @LuaMethod(name="disableAntiCheatLocally", global=true)
    public static void disableAntiCheatLocally(String type) {
        try {
            if (ServerOptions.instance == null) {
                return;
            }
            switch (type.toLowerCase()) {
                case "xp": {
                    if (ServerOptions.instance.antiCheatXp == null) break;
                    ServerOptions.instance.antiCheatXp.setValue(4);
                    break;
                }
                case "player": {
                    if (ServerOptions.instance.antiCheatPlayer == null) break;
                    ServerOptions.instance.antiCheatPlayer.setValue(4);
                    break;
                }
                case "movement": {
                    if (ServerOptions.instance.antiCheatSpeed == null) break;
                    ServerOptions.instance.antiCheatSpeed.setValue(4);
                    break;
                }
                case "item": {
                    if (ServerOptions.instance.antiCheatPlayer == null) break;
                    ServerOptions.instance.antiCheatPlayer.setValue(4);
                    break;
                }
                case "hit": {
                    if (ServerOptions.instance.antiCheatHit == null) break;
                    ServerOptions.instance.antiCheatHit.setValue(4);
                    break;
                }
                case "safety": {
                    if (ServerOptions.instance.antiCheatSafety == null) break;
                    ServerOptions.instance.antiCheatSafety.setValue(4);
                    break;
                }
                case "packet": {
                    if (ServerOptions.instance.antiCheatPacketException == null) break;
                    ServerOptions.instance.antiCheatPacketException.setValue(4);
                    break;
                }
                case "permission": {
                    if (ServerOptions.instance.antiCheatPermission == null) break;
                    ServerOptions.instance.antiCheatPermission.setValue(4);
                    break;
                }
                case "fire": {
                    if (ServerOptions.instance.antiCheatHit == null) break;
                    ServerOptions.instance.antiCheatHit.setValue(4);
                    break;
                }
                case "safehouse": {
                    if (ServerOptions.instance.antiCheatSafeHouse == null) break;
                    ServerOptions.instance.antiCheatSafeHouse.setValue(4);
                    break;
                }
                case "recipe": {
                    if (ServerOptions.instance.antiCheatXp == null) break;
                    ServerOptions.instance.antiCheatXp.setValue(4);
                    break;
                }
                case "checksum": {
                    if (ServerOptions.instance.antiCheatChecksum == null) break;
                    ServerOptions.instance.antiCheatChecksum.setValue(4);
                    break;
                }
                case "serverconfig": {
                    if (ServerOptions.instance.antiCheatChecksum == null) break;
                    ServerOptions.instance.antiCheatChecksum.setValue(4);
                    break;
                }
                case "all": {
                    if (ServerOptions.instance.antiCheatSpeed != null) {
                        ServerOptions.instance.antiCheatSpeed.setValue(4);
                    }
                    if (ServerOptions.instance.antiCheatSafety != null) {
                        ServerOptions.instance.antiCheatSafety.setValue(4);
                    }
                    if (ServerOptions.instance.antiCheatHit != null) {
                        ServerOptions.instance.antiCheatHit.setValue(4);
                    }
                    if (ServerOptions.instance.antiCheatPacketException != null) {
                        ServerOptions.instance.antiCheatPacketException.setValue(4);
                    }
                    if (ServerOptions.instance.antiCheatPermission != null) {
                        ServerOptions.instance.antiCheatPermission.setValue(4);
                    }
                    if (ServerOptions.instance.antiCheatXp != null) {
                        ServerOptions.instance.antiCheatXp.setValue(4);
                    }
                    if (ServerOptions.instance.antiCheatHit != null) {
                        ServerOptions.instance.antiCheatHit.setValue(4);
                    }
                    if (ServerOptions.instance.antiCheatSafeHouse != null) {
                        ServerOptions.instance.antiCheatSafeHouse.setValue(4);
                    }
                    if (ServerOptions.instance.antiCheatXp != null) {
                        ServerOptions.instance.antiCheatXp.setValue(4);
                    }
                    if (ServerOptions.instance.antiCheatPlayer != null) {
                        ServerOptions.instance.antiCheatPlayer.setValue(4);
                    }
                    if (ServerOptions.instance.antiCheatChecksum != null) {
                        ServerOptions.instance.antiCheatChecksum.setValue(4);
                    }
                    if (ServerOptions.instance.antiCheatPlayer != null) {
                        ServerOptions.instance.antiCheatPlayer.setValue(4);
                    }
                    if (ServerOptions.instance.antiCheatChecksum == null) break;
                    ServerOptions.instance.antiCheatChecksum.setValue(4);
                }
            }
        }
        catch (Exception e) {
            Logger.printLog("Error disabling anti-cheat: " + e.getMessage());
        }
    }

    @LuaMethod(name="isSinglePlayer", global=true)
    public static boolean isSinglePlayer() {
        try {
            return GameClientWrapper.getInstance() == null;
        }
        catch (Exception e) {
            return true;
        }
    }

    @LuaMethod(name="safeSyncXp", global=true)
    public static boolean safeSyncXp(IsoPlayer player) {
        try {
            if (player == null) {
                return false;
            }
            if (GameClientWrapper.getInstance() == null) {
                return true;
            }
            if (ServerOptions.instance != null) {
                int playerPolicy;
                int xpPolicy = ServerOptions.instance.antiCheatXp != null ? ServerOptions.instance.antiCheatXp.getValue() : 0;
                int n = playerPolicy = ServerOptions.instance.antiCheatPlayer != null ? ServerOptions.instance.antiCheatPlayer.getValue() : 0;
                if (xpPolicy == 1 || xpPolicy == 2 || playerPolicy == 1 || playerPolicy == 2) {
                    Logger.printLog("Skipping sync - anti-cheat would ban/kick");
                    return false;
                }
            }
            return true;
        }
        catch (Exception e) {
            return false;
        }
    }

    @LuaMethod(name="enableStealthMode", global=true)
    public static void enableStealthMode() {
        stealthMode = true;
        bypassActive = true;
        Logger.printLog("Stealth mode enabled - changes will be applied gradually");
    }

    @LuaMethod(name="disableStealthMode", global=true)
    public static void disableStealthMode() {
        stealthMode = false;
        Logger.printLog("Stealth mode disabled - instant changes enabled");
    }

    @LuaMethod(name="isStealthModeEnabled", global=true)
    public static boolean isStealthModeEnabled() {
        return stealthMode;
    }

    @LuaMethod(name="getPendingChangesCount", global=true)
    public static int getPendingChangesCount() {
        return targetXpValues.size();
    }

    @LuaMethod(name="clearQueuedChanges", global=true)
    public static void clearQueuedChanges() {
        targetXpValues.clear();
        originalXpValues.clear();
        lastSyncTimes.clear();
    }

    @LuaMethod(name="spoofSyncTimestamp", global=true)
    public static boolean spoofSyncTimestamp(IsoPlayer player) {
        if (player == null) {
            return false;
        }
        try {
            if (playerLastUpdateField == null) {
                String[] fieldNames;
                for (String fieldName : fieldNames = new String[]{"lastUpdateTime", "lastSyncTime", "lastPacketTime", "m_lastUpdateTime"}) {
                    playerLastUpdateField = FieldCache.getField(player.getClass(), fieldName);
                    if (playerLastUpdateField != null) break;
                }
                if (playerLastUpdateField == null) {
                    Logger.printLog("Could not find player timestamp field");
                    return false;
                }
            }
            if (playerLastUpdateField != null) {
                FieldCache.setFieldValue(player, playerLastUpdateField, System.currentTimeMillis() - 60000L);
                Logger.printLog("Spoofed sync timestamp: " + playerLastUpdateField.getName());
                return true;
            }
        }
        catch (Exception e) {
            Logger.printLog("Error spoofing timestamp: " + e.getMessage());
        }
        return false;
    }

    @LuaMethod(name="bypassServerValidation", global=true)
    public static boolean bypassServerValidation() {
        try {
            Object connection = GameClientWrapper.getConnection();
            if (connection == null) {
                return false;
            }
            if (connectionValidatedField == null || connectionTrustedField == null) {
                try {
                    String fieldName;
                    String[] trustedFieldNames;
                    String fieldName2;
                    int n;
                    String[] validatedFieldNames;
                    Class<?> connectionClass = connection.getClass();
                    String[] stringArray = validatedFieldNames = new String[]{"validated", "isValidated", "m_validated", "bValidated"};
                    int n2 = stringArray.length;
                    for (n = 0; n < n2 && (connectionValidatedField = FieldCache.getField(connectionClass, fieldName2 = stringArray[n])) == null; ++n) {
                    }
                    String[] stringArray2 = trustedFieldNames = new String[]{"trusted", "isTrusted", "m_trusted", "bTrusted"};
                    n = stringArray2.length;
                    for (int i = 0; i < n && (connectionTrustedField = FieldCache.getField(connectionClass, fieldName = stringArray2[i])) == null; ++i) {
                    }
                }
                catch (Exception e) {
                    Logger.printLog("Could not initialize validation field cache");
                }
            }
            boolean success = false;
            Object connection2 = GameClientWrapper.getConnection();
            if (connectionValidatedField != null && connection2 != null) {
                try {
                    FieldCache.setFieldValue(connection2, connectionValidatedField, true);
                    Logger.printLog("Set validated field: " + connectionValidatedField.getName());
                    success = true;
                }
                catch (Exception exception) {
                    // empty catch block
                }
            }
            if (connectionTrustedField != null && connection2 != null) {
                try {
                    FieldCache.setFieldValue(connection2, connectionTrustedField, true);
                    Logger.printLog("Set trusted field: " + connectionTrustedField.getName());
                    success = true;
                }
                catch (Exception exception) {
                    // empty catch block
                }
            }
            if (success) {
                bypassActive = true;
            }
            return success;
        }
        catch (Exception e) {
            Logger.printLog("Error bypassing server validation: " + e.getMessage());
            return false;
        }
    }

    @LuaMethod(name="sendValidationPacket", global=true)
    public static boolean sendValidationPacket(IsoPlayer player) {
        if (player == null) {
            return false;
        }
        try {
            EtherLuaMethods.spoofSyncTimestamp(player);
            GameClient instance = GameClientWrapper.getInstance();
            if (instance != null) {
                instance.sendPlayer(player);
            }
            return true;
        }
        catch (Exception e) {
            Logger.printLog("Error sending validation packet: " + e.getMessage());
            return false;
        }
    }

    @LuaMethod(name="prepareBypass", global=true)
    public static void prepareBypass(IsoPlayer player) {
        EtherLuaMethods.disableAntiCheatLocally("all");
        EtherLuaMethods.bypassServerValidation();
        if (player != null) {
            EtherLuaMethods.spoofSyncTimestamp(player);
        }
        bypassActive = true;
    }

    @LuaMethod(name="safeSyncWithBypass", global=true)
    public static void safeSyncWithBypass(IsoPlayer player) {
        if (player == null) {
            return;
        }
        EtherLuaMethods.prepareBypass(player);
        EtherLuaMethods.sendValidationPacket(player);
    }

    @LuaMethod(name="enableGlobalBypass", global=true)
    public static void enableGlobalBypass() {
        ServerAntiCheatBypass.getInstance().enableGlobalBypass();
    }

    @LuaMethod(name="disableGlobalBypass", global=true)
    public static void disableGlobalBypass() {
        ServerAntiCheatBypass.getInstance().disableGlobalBypass();
    }

    @LuaMethod(name="enableBypassFor", global=true)
    public static void enableBypassFor(String type) {
        ServerAntiCheatBypass.getInstance().enableBypassForType(type);
    }

    @LuaMethod(name="disableBypassFor", global=true)
    public static void disableBypassFor(String type) {
        ServerAntiCheatBypass.getInstance().disableBypassForType(type);
    }

    @LuaMethod(name="enableAllBypasses", global=true)
    public static void enableAllBypasses() {
        ServerAntiCheatBypass.getInstance().enableAllTypeBypasses();
    }

    @LuaMethod(name="disableAllBypasses", global=true)
    public static void disableAllBypasses() {
        ServerAntiCheatBypass.getInstance().disableAllTypeBypasses();
    }

    @LuaMethod(name="whitelistPlayer", global=true)
    public static void whitelistPlayer(String username) {
        ServerAntiCheatBypass.getInstance().whitelistPlayer(username);
    }

    @LuaMethod(name="unwhitelistPlayer", global=true)
    public static void unwhitelistPlayer(String username) {
        ServerAntiCheatBypass.getInstance().unwhitelistPlayer(username);
    }

    @LuaMethod(name="clearWhitelist", global=true)
    public static void clearWhitelist() {
        ServerAntiCheatBypass.getInstance().clearWhitelist();
    }

    @LuaMethod(name="isGlobalBypassEnabled", global=true)
    public static boolean isGlobalBypassEnabled() {
        return ServerAntiCheatBypass.getInstance().isGlobalBypassEnabled();
    }

    @LuaMethod(name="isBypassEnabledFor", global=true)
    public static boolean isBypassEnabledFor(String type) {
        return ServerAntiCheatBypass.getInstance().isBypassEnabledForType(type);
    }

    @LuaMethod(name="isPlayerWhitelisted", global=true)
    public static boolean isPlayerWhitelisted(String username) {
        return ServerAntiCheatBypass.getInstance().isPlayerWhitelisted(username);
    }

    @LuaMethod(name="resetBypassStats", global=true)
    public static void resetBypassStats() {
        ServerAntiCheatBypass.getInstance().resetStatistics();
    }

    @LuaMethod(name="printBypassStatus", global=true)
    public static void printBypassStatus() {
        ServerAntiCheatBypass.getInstance().printStatus();
    }

    @LuaMethod(name="safeAddTrait", global=true)
    public static void safeAddTrait(IsoPlayer player, String traitType) {
        if (player == null || traitType == null) {
            return;
        }
        try {
            EtherLuaMethods.prepareBypass(player);
            try {
                Method getTraitsMethod = FieldCache.getMethod(player.getClass(), "getCharacterTraits", new Class[0]);
                Object traitsObj = FieldCache.invokeMethod(player, getTraitsMethod, new Object[0]);
                if (traitsObj != null) {
                    try {
                        Method addMethod = FieldCache.getMethod(traitsObj.getClass(), "add", String.class);
                        FieldCache.invokeMethod(traitsObj, addMethod, traitType);
                    }
                    catch (Exception e) {
                        Method addMethod = FieldCache.getMethod(traitsObj.getClass(), "add", Object.class);
                        FieldCache.invokeMethod(traitsObj, addMethod, traitType);
                    }
                }
            }
            catch (Exception e) {
                try {
                    Method getTraitsMethod = FieldCache.getMethod(player.getClass(), "getTraits", new Class[0]);
                    Object traitsObj = FieldCache.invokeMethod(player, getTraitsMethod, new Object[0]);
                    if (traitsObj != null) {
                        Method addMethod = FieldCache.getMethod(traitsObj.getClass(), "add", Object.class);
                        FieldCache.invokeMethod(traitsObj, addMethod, traitType);
                    }
                }
                catch (Exception e2) {
                    Logger.printLog("Error adding trait (fallback): " + e2.getMessage());
                }
            }
            EtherLuaMethods.sendValidationPacket(player);
        }
        catch (Exception e) {
            Logger.printLog("Error adding trait: " + e.getMessage());
        }
    }

    @LuaMethod(name="safeRemoveTrait", global=true)
    public static void safeRemoveTrait(IsoPlayer player, String traitType) {
        if (player == null || traitType == null) {
            return;
        }
        try {
            EtherLuaMethods.prepareBypass(player);
            try {
                Method getTraitsMethod = FieldCache.getMethod(player.getClass(), "getCharacterTraits", new Class[0]);
                Object traitsObj = FieldCache.invokeMethod(player, getTraitsMethod, new Object[0]);
                if (traitsObj != null) {
                    Method removeMethod = FieldCache.getMethod(traitsObj.getClass(), "remove", Object.class);
                    FieldCache.invokeMethod(traitsObj, removeMethod, traitType);
                }
            }
            catch (Exception e) {
                try {
                    Method getTraitsMethod = FieldCache.getMethod(player.getClass(), "getTraits", new Class[0]);
                    Object traitsObj = FieldCache.invokeMethod(player, getTraitsMethod, new Object[0]);
                    if (traitsObj != null) {
                        Method removeMethod = FieldCache.getMethod(traitsObj.getClass(), "remove", Object.class);
                        FieldCache.invokeMethod(traitsObj, removeMethod, traitType);
                    }
                }
                catch (Exception e2) {
                    Logger.printLog("Error removing trait (fallback): " + e2.getMessage());
                }
            }
            EtherLuaMethods.sendValidationPacket(player);
        }
        catch (Exception e) {
            Logger.printLog("Error removing trait: " + e.getMessage());
        }
    }

    @LuaMethod(name="isBypassActive", global=true)
    public static boolean isBypassActive() {
        return bypassActive;
    }

    @LuaMethod(name="getBypassStatus", global=true)
    public static String getBypassStatus() {
        StringBuilder status = new StringBuilder();
        status.append("Bypass: ").append(bypassActive ? "ACTIVE" : "INACTIVE");
        status.append(" | Stealth: ").append(stealthMode ? "ON" : "OFF");
        return status.toString();
    }

    @LuaMethod(name="requireExtra", global=true)
    public static void requireExtra(String file) {
        String key = SafeAPI.getInstance().generateVerificationKey();
        try {
            Object luaFile;
            Object object = luaFile = file.endsWith(".lua") ? file : file + ".lua";
            if (!EtherMain.getInstance().etherLuaManager.luaFilesList.contains(luaFile)) {
                EtherMain.getInstance().etherLuaManager.luaFilesList.add((String)luaFile);
            }
            EtherLuaCompiler.getInstance().addWordToBlacklistLuaCompiler(((String)luaFile).substring(0, ((String)luaFile).lastIndexOf(".")));
            EtherLuaCompiler.getInstance().addPathToWhiteListLuaCompiler((String)luaFile);
            LuaManager.RunLua((String)luaFile);
        }
        catch (Exception e) {
            Logger.error("Error in requireExtra", e);
        }
    }

    /*
     * Enabled aggressive exception aggregation
     */
    @LuaMethod(name="getExtraTexture", global=true)
    public static Texture getExtraTexture(String path) {
        String key = SafeAPI.getInstance().generateVerificationKey();
        try {
            if (!path.endsWith(".png")) {
                Logger.printLog("Incorrect path to the image file. Required .png");
                return null;
            }
            ConcurrentHashMap<String, Texture> textureCache = EtherMain.getInstance().etherAPI.textureCache;
            if (textureCache.containsKey(path)) {
                return textureCache.get(path);
            }
            try (FileInputStream fis = new FileInputStream(Paths.get(path, new String[0]).toFile());){
                Texture texture;
                try (BufferedInputStream bis = new BufferedInputStream(fis);){
                    Texture texture2 = new Texture(path, bis, false);
                    textureCache.put(path, texture2);
                    texture = texture2;
                }
                return texture;
            }
        }
        catch (Exception e) {
            Logger.printLog("Error reading image: " + e.getMessage());
            return null;
        }
    }

    @LuaMethod(name="getTranslate", global=true)
    public static String getTranslate(String var0, KahluaTable var1) {
        return EtherMain.getInstance().etherTranslator.getTranslate(var0, var1);
    }

    @LuaMethod(name="getTranslate", global=true)
    public static String getTranslate(String var0) {
        return EtherMain.getInstance().etherTranslator.getTranslate(var0);
    }

    @LuaMethod(name="setLanguage", global=true)
    public static void setLanguage(String language) {
        EtherMain.getInstance().etherTranslator.setLanguage(language);
    }

    @LuaMethod(name="getLanguage", global=true)
    public static String getLanguage() {
        return EtherMain.getInstance().etherTranslator.getCurrentLanguage();
    }

    @LuaMethod(name="setPanelSize", global=true)
    public static void setPanelSize(int width, int height) {
        try {
            Path configFolderPath = Paths.get("EtherHack/config", new String[0]);
            if (!Files.exists(configFolderPath, new LinkOption[0])) {
                Files.createDirectories(configFolderPath, new FileAttribute[0]);
            }
            Path panelConfigPath = configFolderPath.resolve("panel.properties");
            Properties props = new Properties();
            props.setProperty("width", Integer.toString(width));
            props.setProperty("height", Integer.toString(height));
            try (FileOutputStream out = new FileOutputStream(panelConfigPath.toFile())) {
                props.store(out, null);
            }
            Logger.printLog("Panel size saved: " + width + "x" + height);
        }
        catch (IOException e) {
            Logger.printLog("Error while saving panel size: " + String.valueOf(e));
        }
    }

    @LuaMethod(name="getPanelWidth", global=true)
    public static int getPanelWidth() {
        return getPanelSize("width", 1000);
    }

    @LuaMethod(name="getPanelHeight", global=true)
    public static int getPanelHeight() {
        return getPanelSize("height", 1100);
    }

    private static int getPanelSize(String key, int defaultValue) {
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream("EtherHack/config/panel.properties")) {
            props.load(in);
            return Integer.parseInt(props.getProperty(key, Integer.toString(defaultValue)));
        }
        catch (Exception e) {
            return defaultValue;
        }
    }

    @LuaMethod(name="hackAdminAccess", global=true)
    public static void hackAdminAccess() {
        String key = SafeAPI.getInstance().generateVerificationKey();
        try {
            GameClient instance = GameClientWrapper.getInstance();
            if (instance != null) {
                for (IsoPlayer player : instance.getPlayers()) {
                    if (!player.isLocalPlayer()) continue;
                    player.accessLevel = "admin";
                }
            }
        }
        catch (Exception e) {
            Logger.printLog("Error in hackAdminAccess: " + e.getMessage());
        }
    }

    @LuaMethod(name="setZombieKills", global=true)
    public static void setZombieKills(int kills) {
        IsoPlayer player = IsoPlayer.getInstance();
        if (player != null) {
            player.setZombieKills(kills);
        }
    }

    @LuaMethod(name="setHoursAlive", global=true)
    public static void setHoursAlive(int hours) {
        IsoPlayer player = IsoPlayer.getInstance();
        if (player != null) {
            player.setHoursSurvived((double)hours);
        }
    }

    @LuaMethod(name="getZombieKills", global=true)
    public static int getZombieKills() {
        IsoPlayer player = IsoPlayer.getInstance();
        if (player != null) {
            return player.getZombieKills();
        }
        return 0;
    }

    @LuaMethod(name="getHoursAlive", global=true)
    public static int getHoursAlive() {
        IsoPlayer player = IsoPlayer.getInstance();
        if (player != null) {
            return (int)player.getHoursSurvived();
        }
        return 0;
    }

    @LuaMethod(name="getAccentUIColor", global=true)
    public static Color getAccentUIColor() {
        return EtherMain.getInstance().etherAPI.mainUIAccentColor;
    }

    protected void cleanMethodCache() {
        methodCache.clear();
    }

    public static EtherLuaMethods getInstance() {
        if (instance == null) {
            instance = new EtherLuaMethods();
        }
        return instance;
    }
}
