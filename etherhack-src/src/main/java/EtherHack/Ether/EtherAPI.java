/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  se.krka.kahlua.converter.KahluaConverterManager
 *  se.krka.kahlua.integration.annotations.LuaMethod
 *  se.krka.kahlua.j2se.J2SEPlatform
 *  se.krka.kahlua.vm.KahluaTable
 *  se.krka.kahlua.vm.Platform
 *  zombie.Lua.LuaManager
 *  zombie.SandboxOptions
 *  zombie.characterTextures.BloodBodyPartType
 *  zombie.characters.CharacterStat
 *  zombie.characters.IsoPlayer
 *  zombie.characters.IsoZombie
 *  zombie.core.Color
 *  zombie.core.Core
 *  zombie.core.textures.Texture
 *  zombie.inventory.InventoryItem
 *  zombie.inventory.types.HandWeapon
 *  zombie.iso.IsoWorld
 *  zombie.network.GameClient
 *  zombie.network.GameServer
 *  zombie.network.ServerOptions
 *  zombie.network.ZomboidNetData
 *  zombie.vehicles.BaseVehicle
 */
package EtherHack.Ether;

import EtherHack.Ether.EtherLuaMethods;
import EtherHack.Ether.EtherMain;
import EtherHack.Ether.EventProtector;
import EtherHack.Ether.ProtectionManagerX;
import EtherHack.Ether.SafeAPI;
import EtherHack.Ether.ServerSyncBlocker;
import EtherHack.GameClientWrapper;
import EtherHack.annotations.LuaEvents;
import EtherHack.annotations.SubscribeLuaEvent;
import EtherHack.utils.ColorUtils;
import EtherHack.utils.ConfigUtils;
import EtherHack.utils.EventSubscriber;
import EtherHack.utils.Exposer;
import EtherHack.utils.FieldCache;
import EtherHack.utils.Logger;
import EtherHack.utils.PlayerUtils;
import EtherHack.utils.Rendering;
import EtherHack.utils.VehicleUtils;
import EtherHack.utils.ZombieUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import se.krka.kahlua.converter.KahluaConverterManager;
import se.krka.kahlua.integration.annotations.LuaMethod;
import se.krka.kahlua.j2se.J2SEPlatform;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.Platform;
import zombie.Lua.LuaManager;
import zombie.SandboxOptions;
import zombie.characterTextures.BloodBodyPartType;
import zombie.characters.BodyDamage.BodyPart;
import zombie.characters.CharacterStat;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.skinnedmodel.visual.ItemVisual;
import zombie.core.textures.Texture;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.Clothing;
import zombie.inventory.types.HandWeapon;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.LightingJNI;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;
import zombie.network.ZomboidNetData;
import zombie.vehicles.BaseVehicle;

public class EtherAPI {
    private final ProtectionManagerX protectionManager;
    private SafeExposer exposer;
    final ConcurrentHashMap<String, Texture> textureCache = new ConcurrentHashMap();
    private final SafeAPI safeAPI = SafeAPI.getInstance();
    private final ConcurrentHashMap<String, float[]> originalWeaponStats = new ConcurrentHashMap();
    private final ConcurrentHashMap<String, Boolean> critMaxAlwaysKnockdown = new ConcurrentHashMap();
    public Color mainUIAccentColor;
    public Color vehiclesUIColor;
    public Color zombiesUIColor;
    public Color playersUIColor;
    public boolean isPlayerInSafeTeleported;
    public boolean isMultiHitZombies = true;
    public boolean isExtraDamage;
    public boolean isTimedActionCheat;
    public boolean isEnableGodMode;
    public boolean isEnableNoclip;
    public boolean isEnableInvisible;
    public boolean isEnableNightVision;
    public boolean isZombieDontAttack;
    public boolean isNoRecoil;
    private HandWeapon jamStompedWeapon;
    private float jamStompedOriginalChance;
    private HandWeapon recoilStompedWeapon;
    private long lastPlayerDamageSendMs;
    public boolean isHeadshotOnly;
    public boolean isBypassDebugMode;
    public boolean initialCoreDebugCaptured;
    public boolean initialCoreDebug;
    public boolean isUnlimitedCarry;
    public boolean isUnlimitedCondition;
    public boolean isUnlimitedEndurance;
    public boolean isUnlimitedAmmo;
    public int ammoFarmCount = 30;
    public boolean isCritMax;
    public float combatSpeedMultiplier = 1.0f;
    public boolean isAutoRepairItems;
    public boolean isRepairClothing;
    public boolean isDisableFatigue;
    public boolean isDisableHunger;
    public boolean isDisableThirst;
    public boolean isDisableDrunkenness;
    public boolean isDisableAnger;
    public boolean isDisableFear;
    public boolean isDisablePain;
    public boolean isDisablePanic;
    public boolean isDisableMorale;
    public boolean isDisableStress;
    public boolean isDisableSickness;
    public boolean isDisableStressFromCigarettes;
    public boolean isDisableSanity;
    public boolean isDisableBoredomLevel;
    public boolean isDisableUnhappynessLevel;
    public boolean isDisableWetness;
    public boolean isDisableInfectionLevel;
    public boolean isDisableFakeInfectionLevel;
    public boolean isOptimalCalories;
    public boolean isOptimalWeight;
    public boolean isVisualsEnable = true;
    public boolean isVisualsVehiclesEnable;
    public boolean isVisualsZombiesEnable;
    public boolean isVisualDrawLineToZombies;
    public boolean isVisualDrawPlayerNickname;
    public boolean isVisualDrawPlayerInfo;
    public boolean isVisualDrawLineToVehicle;
    public boolean isVisualDrawLineToPlayers;
    public boolean isVisualEnable360Vision = true;
    public boolean isMapDrawLocalPlayer = true;
    public boolean isMapDrawAllPlayers;
    public boolean isMapDrawVehicles;
    public boolean isMapDrawZombies;
    public boolean isMapDrawItems;
    public boolean isMinimapOpen;
    public boolean isNoJam;
    public boolean isNoMuscleStrain;
    public boolean isFullBodyRestore;
    public boolean isCharCreateAllTraits;
    public boolean isCharCreateMaxSkills;
    public boolean isCharCreateAllClothes;
    public final java.util.ArrayList<String> charCreateCustomTraits = new java.util.ArrayList<String>();
    public final java.util.HashMap<String, Integer> charCreateCustomSkillLevels = new java.util.HashMap<String, Integer>();

    public void clearCharCreateCustom() {
        this.charCreateCustomTraits.clear();
        this.charCreateCustomSkillLevels.clear();
    }
    public boolean isVehicleInstantStart;
    public boolean isFullbright;
    private boolean fullbrightApplied;
    private int fullbrightSavedViewConeOpacity = 3;

    public void saveConfig(String var1) {
        String var2 = "EtherHack/config/" + var1 + ".properties";
        Properties var3 = new Properties();
        var3.setProperty("mainUIAccentColor", ColorUtils.colorToString(this.mainUIAccentColor));
        var3.setProperty("vehiclesUIColor", ColorUtils.colorToString(this.vehiclesUIColor));
        var3.setProperty("zombiesUIColor", ColorUtils.colorToString(this.zombiesUIColor));
        var3.setProperty("playersUIColor", ColorUtils.colorToString(this.playersUIColor));
        var3.setProperty("isPlayerInSafeTeleported", Boolean.toString(this.isPlayerInSafeTeleported));
        var3.setProperty("isMultiHitZombies", Boolean.toString(this.isMultiHitZombies));
        var3.setProperty("isPlayerInSafeTeleported", Boolean.toString(this.isPlayerInSafeTeleported));
        var3.setProperty("isMultiHitZombies", Boolean.toString(this.isMultiHitZombies));
        var3.setProperty("isExtraDamage", Boolean.toString(this.isExtraDamage));
        var3.setProperty("isTimedActionCheat", Boolean.toString(this.isTimedActionCheat));
        var3.setProperty("isEnableGodMode", Boolean.toString(this.isEnableGodMode));
        var3.setProperty("isEnableNoclip", Boolean.toString(this.isEnableNoclip));
        var3.setProperty("isEnableInvisible", Boolean.toString(this.isEnableInvisible));
        var3.setProperty("isEnableNightVision", Boolean.toString(this.isEnableNightVision));
        var3.setProperty("isZombieDontAttack", Boolean.toString(this.isZombieDontAttack));
        var3.setProperty("isNoRecoil", Boolean.toString(this.isNoRecoil));
        var3.setProperty("isHeadshotOnly", Boolean.toString(this.isHeadshotOnly));
        var3.setProperty("isBypassDebugMode", Boolean.toString(this.isBypassDebugMode));
        var3.setProperty("isUnlimitedCarry", Boolean.toString(this.isUnlimitedCarry));
        var3.setProperty("isUnlimitedCondition", Boolean.toString(this.isUnlimitedCondition));
        var3.setProperty("isUnlimitedEndurance", Boolean.toString(this.isUnlimitedEndurance));
        var3.setProperty("isUnlimitedAmmo", Boolean.toString(this.isUnlimitedAmmo));
        var3.setProperty("ammoFarmCount", Integer.toString(this.ammoFarmCount));
        var3.setProperty("isCritMax", Boolean.toString(this.isCritMax));
        var3.setProperty("combatSpeedMultiplier", Float.toString(this.combatSpeedMultiplier));
        var3.setProperty("isAutoRepairItems", Boolean.toString(this.isAutoRepairItems));
        var3.setProperty("isRepairClothing", Boolean.toString(this.isRepairClothing));
        var3.setProperty("isDisableFatigue", Boolean.toString(this.isDisableFatigue));
        var3.setProperty("isDisableHunger", Boolean.toString(this.isDisableHunger));
        var3.setProperty("isDisableThirst", Boolean.toString(this.isDisableThirst));
        var3.setProperty("isDisableDrunkenness", Boolean.toString(this.isDisableDrunkenness));
        var3.setProperty("isDisableAnger", Boolean.toString(this.isDisableAnger));
        var3.setProperty("isDisableFear", Boolean.toString(this.isDisableFear));
        var3.setProperty("isDisablePain", Boolean.toString(this.isDisablePain));
        var3.setProperty("isDisablePanic", Boolean.toString(this.isDisablePanic));
        var3.setProperty("isDisableMorale", Boolean.toString(this.isDisableMorale));
        var3.setProperty("isDisableStress", Boolean.toString(this.isDisableStress));
        var3.setProperty("isDisableSickness", Boolean.toString(this.isDisableSickness));
        var3.setProperty("isDisableStressFromCigarettes", Boolean.toString(this.isDisableStressFromCigarettes));
        var3.setProperty("isDisableSanity", Boolean.toString(this.isDisableSanity));
        var3.setProperty("isDisableBoredomLevel", Boolean.toString(this.isDisableBoredomLevel));
        var3.setProperty("isDisableUnhappynessLevel", Boolean.toString(this.isDisableUnhappynessLevel));
        var3.setProperty("isDisableWetness", Boolean.toString(this.isDisableWetness));
        var3.setProperty("isDisableInfectionLevel", Boolean.toString(this.isDisableInfectionLevel));
        var3.setProperty("isDisableFakeInfectionLevel", Boolean.toString(this.isDisableFakeInfectionLevel));
        var3.setProperty("isOptimalCalories", Boolean.toString(this.isOptimalCalories));
        var3.setProperty("isOptimalWeight", Boolean.toString(this.isOptimalWeight));
        var3.setProperty("isVisualsEnable", Boolean.toString(this.isVisualsEnable));
        var3.setProperty("isVisualsVehiclesEnable", Boolean.toString(this.isVisualsVehiclesEnable));
        var3.setProperty("isVisualsZombiesEnable", Boolean.toString(this.isVisualsZombiesEnable));
        var3.setProperty("isVisualDrawLineToZombies", Boolean.toString(this.isVisualDrawLineToZombies));
        var3.setProperty("isVisualDrawPlayerNickname", Boolean.toString(this.isVisualDrawPlayerNickname));
        var3.setProperty("isVisualDrawPlayerInfo", Boolean.toString(this.isVisualDrawPlayerInfo));
        var3.setProperty("isVisualDrawLineToVehicle", Boolean.toString(this.isVisualDrawLineToVehicle));
        var3.setProperty("isVisualDrawLineToPlayers", Boolean.toString(this.isVisualDrawLineToPlayers));
        var3.setProperty("isVisualEnable360Vision", Boolean.toString(this.isVisualEnable360Vision));
        var3.setProperty("isMapDrawLocalPlayer", Boolean.toString(this.isMapDrawLocalPlayer));
        var3.setProperty("isMapDrawAllPlayers", Boolean.toString(this.isMapDrawAllPlayers));
        var3.setProperty("isMapDrawVehicles", Boolean.toString(this.isMapDrawVehicles));
        var3.setProperty("isMapDrawZombies", Boolean.toString(this.isMapDrawZombies));
        var3.setProperty("isMapDrawItems", Boolean.toString(this.isMapDrawItems));
        var3.setProperty("isMinimapOpen", Boolean.toString(this.isMinimapOpen));
        var3.setProperty("isNoJam", Boolean.toString(this.isNoJam));
        var3.setProperty("isNoMuscleStrain", Boolean.toString(this.isNoMuscleStrain));
        var3.setProperty("isFullBodyRestore", Boolean.toString(this.isFullBodyRestore));
        var3.setProperty("isCharCreateAllTraits", Boolean.toString(this.isCharCreateAllTraits));
        var3.setProperty("isCharCreateMaxSkills", Boolean.toString(this.isCharCreateMaxSkills));
        var3.setProperty("isCharCreateAllClothes", Boolean.toString(this.isCharCreateAllClothes));
        StringBuilder ctSb = new StringBuilder();
        for (String t : this.charCreateCustomTraits) {
            if (ctSb.length() > 0) ctSb.append(',');
            ctSb.append(t);
        }
        var3.setProperty("charCreateCustomTraits", ctSb.toString());
        StringBuilder csSb = new StringBuilder();
        for (java.util.Map.Entry<String, Integer> e : this.charCreateCustomSkillLevels.entrySet()) {
            if (csSb.length() > 0) csSb.append(',');
            csSb.append(e.getKey()).append('=').append(e.getValue());
        }
        var3.setProperty("charCreateCustomSkillLevels", csSb.toString());
        var3.setProperty("isVehicleInstantStart", Boolean.toString(this.isVehicleInstantStart));
        var3.setProperty("isFullbright", Boolean.toString(this.isFullbright));
        new File("EtherHack/config").mkdirs();
        try (FileOutputStream var4 = new FileOutputStream(var2);){
            var3.store(var4, (String)null);
        }
        catch (IOException var9) {
            Logger.printLog("Error while saving config: " + String.valueOf(var9));
        }
    }

    public void loadConfig(String var1) {
        String var2 = "EtherHack/config/" + var1 + ".properties";
        Properties var3 = new Properties();
        try (FileInputStream var4 = new FileInputStream(var2);){
            var3.load(var4);
        }
        catch (IOException var9) {
            Logger.printLog("The config file was not found. Loading canceled.");
            return;
        }
        this.mainUIAccentColor = ConfigUtils.getColorFromConfig(var3, "mainUIAccentColor", new Color(72, 216, 168));
        this.vehiclesUIColor = ConfigUtils.getColorFromConfig(var3, "vehiclesUIColor", new Color(150, 150, 200));
        this.zombiesUIColor = ConfigUtils.getColorFromConfig(var3, "zombiesUIColor", new Color(255, 150, 100));
        this.playersUIColor = ConfigUtils.getColorFromConfig(var3, "playersUIColor", new Color(255, 50, 100));
        this.isPlayerInSafeTeleported = ConfigUtils.getBooleanFromConfig(var3, "isPlayerInSafeTeleported", false);
        this.isMultiHitZombies = ConfigUtils.getBooleanFromConfig(var3, "isMultiHitZombies", true);
        this.isExtraDamage = ConfigUtils.getBooleanFromConfig(var3, "isExtraDamage", false);
        this.isTimedActionCheat = ConfigUtils.getBooleanFromConfig(var3, "isTimedActionCheat", false);
        this.isEnableGodMode = ConfigUtils.getBooleanFromConfig(var3, "isEnableGodMode", false);
        this.isEnableNoclip = ConfigUtils.getBooleanFromConfig(var3, "isEnableNoclip", false);
        this.isEnableInvisible = ConfigUtils.getBooleanFromConfig(var3, "isEnableInvisible", false);
        this.isEnableNightVision = ConfigUtils.getBooleanFromConfig(var3, "isEnableNightVision", false);
        this.isZombieDontAttack = ConfigUtils.getBooleanFromConfig(var3, "isZombieDontAttack", false);
        this.isNoRecoil = ConfigUtils.getBooleanFromConfig(var3, "isNoRecoil", false);
        this.isHeadshotOnly = ConfigUtils.getBooleanFromConfig(var3, "isHeadshotOnly", false);
        this.isBypassDebugMode = ConfigUtils.getBooleanFromConfig(var3, "isBypassDebugMode", false);
        this.isUnlimitedCarry = ConfigUtils.getBooleanFromConfig(var3, "isUnlimitedCarry", false);
        this.isUnlimitedCondition = ConfigUtils.getBooleanFromConfig(var3, "isUnlimitedCondition", false);
        this.isUnlimitedEndurance = ConfigUtils.getBooleanFromConfig(var3, "isUnlimitedEndurance", false);
        this.isUnlimitedAmmo = ConfigUtils.getBooleanFromConfig(var3, "isUnlimitedAmmo", false);
        this.ammoFarmCount = ConfigUtils.getIntFromConfig(var3, "ammoFarmCount", 30);
        this.isCritMax = ConfigUtils.getBooleanFromConfig(var3, "isCritMax", false);
        this.combatSpeedMultiplier = ConfigUtils.getFloatFromConfig(var3, "combatSpeedMultiplier", 1.0f);
        this.isAutoRepairItems = ConfigUtils.getBooleanFromConfig(var3, "isAutoRepairItems", false);
        this.isRepairClothing = ConfigUtils.getBooleanFromConfig(var3, "isRepairClothing", false);
        this.isDisableFatigue = ConfigUtils.getBooleanFromConfig(var3, "isDisableFatigue", false);
        this.isDisableHunger = ConfigUtils.getBooleanFromConfig(var3, "isDisableHunger", false);
        this.isDisableThirst = ConfigUtils.getBooleanFromConfig(var3, "isDisableThirst", false);
        this.isDisableDrunkenness = ConfigUtils.getBooleanFromConfig(var3, "isDisableDrunkenness", false);
        this.isDisableAnger = ConfigUtils.getBooleanFromConfig(var3, "isDisableAnger", false);
        this.isDisableFear = ConfigUtils.getBooleanFromConfig(var3, "isDisableFear", false);
        this.isDisablePain = ConfigUtils.getBooleanFromConfig(var3, "isDisablePain", false);
        this.isDisablePanic = ConfigUtils.getBooleanFromConfig(var3, "isDisablePanic", false);
        this.isDisableMorale = ConfigUtils.getBooleanFromConfig(var3, "isDisableMorale", false);
        this.isDisableStress = ConfigUtils.getBooleanFromConfig(var3, "isDisableStress", false);
        this.isDisableSickness = ConfigUtils.getBooleanFromConfig(var3, "isDisableSickness", false);
        this.isDisableStressFromCigarettes = ConfigUtils.getBooleanFromConfig(var3, "isDisableStressFromCigarettes", false);
        this.isDisableSanity = ConfigUtils.getBooleanFromConfig(var3, "isDisableSanity", false);
        this.isDisableBoredomLevel = ConfigUtils.getBooleanFromConfig(var3, "isDisableBoredomLevel", false);
        this.isDisableUnhappynessLevel = ConfigUtils.getBooleanFromConfig(var3, "isDisableUnhappynessLevel", false);
        this.isDisableWetness = ConfigUtils.getBooleanFromConfig(var3, "isDisableWetness", false);
        this.isDisableInfectionLevel = ConfigUtils.getBooleanFromConfig(var3, "isDisableInfectionLevel", false);
        this.isDisableFakeInfectionLevel = ConfigUtils.getBooleanFromConfig(var3, "isDisableFakeInfectionLevel", false);
        this.isOptimalCalories = ConfigUtils.getBooleanFromConfig(var3, "isOptimalCalories", false);
        this.isOptimalWeight = ConfigUtils.getBooleanFromConfig(var3, "isOptimalWeight", false);
        this.isVisualsEnable = ConfigUtils.getBooleanFromConfig(var3, "isVisualsEnable", true);
        this.isVisualsVehiclesEnable = ConfigUtils.getBooleanFromConfig(var3, "isVisualsVehiclesEnable", false);
        this.isVisualsZombiesEnable = ConfigUtils.getBooleanFromConfig(var3, "isVisualsZombiesEnable", false);
        this.isVisualDrawLineToZombies = ConfigUtils.getBooleanFromConfig(var3, "isVisualDrawLineToZombies", false);
        this.isVisualDrawPlayerNickname = ConfigUtils.getBooleanFromConfig(var3, "isVisualDrawPlayerNickname", false);
        this.isVisualDrawPlayerInfo = ConfigUtils.getBooleanFromConfig(var3, "isVisualDrawPlayerInfo", false);
        this.isVisualDrawLineToVehicle = ConfigUtils.getBooleanFromConfig(var3, "isVisualDrawLineToVehicle", false);
        this.isVisualDrawLineToPlayers = ConfigUtils.getBooleanFromConfig(var3, "isVisualDrawLineToPlayers", false);
        this.isVisualEnable360Vision = ConfigUtils.getBooleanFromConfig(var3, "isVisualEnable360Vision", true);
        this.isMapDrawLocalPlayer = ConfigUtils.getBooleanFromConfig(var3, "isMapDrawLocalPlayer", true);
        this.isMapDrawAllPlayers = ConfigUtils.getBooleanFromConfig(var3, "isMapDrawAllPlayers", false);
        this.isMapDrawVehicles = ConfigUtils.getBooleanFromConfig(var3, "isMapDrawVehicles", false);
        this.isMapDrawZombies = ConfigUtils.getBooleanFromConfig(var3, "isMapDrawZombies", false);
        this.isMapDrawItems = ConfigUtils.getBooleanFromConfig(var3, "isMapDrawItems", false);
        this.isMinimapOpen = ConfigUtils.getBooleanFromConfig(var3, "isMinimapOpen", false);
        this.isNoJam = ConfigUtils.getBooleanFromConfig(var3, "isNoJam", false);
        this.isNoMuscleStrain = ConfigUtils.getBooleanFromConfig(var3, "isNoMuscleStrain", false);
        this.isFullBodyRestore = ConfigUtils.getBooleanFromConfig(var3, "isFullBodyRestore", false);
        this.isCharCreateAllTraits = ConfigUtils.getBooleanFromConfig(var3, "isCharCreateAllTraits", false);
        this.isCharCreateMaxSkills = ConfigUtils.getBooleanFromConfig(var3, "isCharCreateMaxSkills", false);
        this.isCharCreateAllClothes = ConfigUtils.getBooleanFromConfig(var3, "isCharCreateAllClothes", false);
        this.charCreateCustomTraits.clear();
        String ctStr = ConfigUtils.getStringFromConfig(var3, "charCreateCustomTraits", "");
        if (ctStr != null && !ctStr.isEmpty()) {
            for (String t : ctStr.split(",")) {
                if (!t.isEmpty() && !this.charCreateCustomTraits.contains(t)) this.charCreateCustomTraits.add(t);
            }
        }
        this.charCreateCustomSkillLevels.clear();
        String csStr = ConfigUtils.getStringFromConfig(var3, "charCreateCustomSkillLevels", "");
        if (csStr != null && !csStr.isEmpty()) {
            for (String pair : csStr.split(",")) {
                int eq = pair.indexOf('=');
                if (eq > 0) {
                    try {
                        this.charCreateCustomSkillLevels.put(pair.substring(0, eq),
                            Integer.valueOf(pair.substring(eq + 1)));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        this.isVehicleInstantStart = ConfigUtils.getBooleanFromConfig(var3, "isVehicleInstantStart", false);
        this.isFullbright = ConfigUtils.getBooleanFromConfig(var3, "isFullbright", false);
    }

    private void initStartupConfig() {
        if (!new File("EtherHack/config/startup.properties").exists()) {
            this.mainUIAccentColor = new Color(72, 216, 168);
            this.vehiclesUIColor = new Color(150, 150, 200);
            this.zombiesUIColor = new Color(255, 150, 100);
            this.playersUIColor = new Color(255, 50, 100);
            this.saveConfig("startup");
        }
        Properties var1 = new Properties();
        try (FileInputStream var2 = new FileInputStream("EtherHack/config/startup.properties");){
            var1.load(var2);
        }
        catch (IOException var7) {
            Logger.printLog("Startup file not found. Loading default settings.");
        }
        this.mainUIAccentColor = ConfigUtils.getColorFromConfig(var1, "mainUIAccentColor", new Color(72, 216, 168));
        this.vehiclesUIColor = ConfigUtils.getColorFromConfig(var1, "vehiclesUIColor", new Color(150, 150, 200));
        this.zombiesUIColor = ConfigUtils.getColorFromConfig(var1, "zombiesUIColor", new Color(255, 150, 100));
        this.playersUIColor = ConfigUtils.getColorFromConfig(var1, "playersUIColor", new Color(255, 50, 100));
        this.isPlayerInSafeTeleported = ConfigUtils.getBooleanFromConfig(var1, "isPlayerInSafeTeleported", false);
        this.isMultiHitZombies = ConfigUtils.getBooleanFromConfig(var1, "isMultiHitZombies", true);
        this.isExtraDamage = ConfigUtils.getBooleanFromConfig(var1, "isExtraDamage", false);
        this.isTimedActionCheat = ConfigUtils.getBooleanFromConfig(var1, "isTimedActionCheat", false);
        this.isEnableGodMode = ConfigUtils.getBooleanFromConfig(var1, "isEnableGodMode", false);
        this.isEnableNoclip = ConfigUtils.getBooleanFromConfig(var1, "isEnableNoclip", false);
        this.isEnableInvisible = ConfigUtils.getBooleanFromConfig(var1, "isEnableInvisible", false);
        this.isEnableNightVision = ConfigUtils.getBooleanFromConfig(var1, "isEnableNightVision", false);
        this.isZombieDontAttack = ConfigUtils.getBooleanFromConfig(var1, "isZombieDontAttack", false);
        this.isNoRecoil = ConfigUtils.getBooleanFromConfig(var1, "isNoRecoil", false);
        this.isHeadshotOnly = ConfigUtils.getBooleanFromConfig(var1, "isHeadshotOnly", false);
        this.isBypassDebugMode = ConfigUtils.getBooleanFromConfig(var1, "isBypassDebugMode", false);
        this.isUnlimitedCarry = ConfigUtils.getBooleanFromConfig(var1, "isUnlimitedCarry", false);
        this.isUnlimitedCondition = ConfigUtils.getBooleanFromConfig(var1, "isUnlimitedCondition", false);
        this.isUnlimitedEndurance = ConfigUtils.getBooleanFromConfig(var1, "isUnlimitedEndurance", false);
        this.isUnlimitedAmmo = ConfigUtils.getBooleanFromConfig(var1, "isUnlimitedAmmo", false);
        this.isCritMax = ConfigUtils.getBooleanFromConfig(var1, "isCritMax", false);
        this.combatSpeedMultiplier = ConfigUtils.getFloatFromConfig(var1, "combatSpeedMultiplier", 1.0f);
        this.isAutoRepairItems = ConfigUtils.getBooleanFromConfig(var1, "isAutoRepairItems", false);
        this.isRepairClothing = ConfigUtils.getBooleanFromConfig(var1, "isRepairClothing", false);
        this.isDisableFatigue = ConfigUtils.getBooleanFromConfig(var1, "isDisableFatigue", false);
        this.isDisableHunger = ConfigUtils.getBooleanFromConfig(var1, "isDisableHunger", false);
        this.isDisableThirst = ConfigUtils.getBooleanFromConfig(var1, "isDisableThirst", false);
        this.isDisableDrunkenness = ConfigUtils.getBooleanFromConfig(var1, "isDisableDrunkenness", false);
        this.isDisableAnger = ConfigUtils.getBooleanFromConfig(var1, "isDisableAnger", false);
        this.isDisableFear = ConfigUtils.getBooleanFromConfig(var1, "isDisableFear", false);
        this.isDisablePain = ConfigUtils.getBooleanFromConfig(var1, "isDisablePain", false);
        this.isDisablePanic = ConfigUtils.getBooleanFromConfig(var1, "isDisablePanic", false);
        this.isDisableMorale = ConfigUtils.getBooleanFromConfig(var1, "isDisableMorale", false);
        this.isDisableStress = ConfigUtils.getBooleanFromConfig(var1, "isDisableStress", false);
        this.isDisableSickness = ConfigUtils.getBooleanFromConfig(var1, "isDisableSickness", false);
        this.isDisableStressFromCigarettes = ConfigUtils.getBooleanFromConfig(var1, "isDisableStressFromCigarettes", false);
        this.isDisableSanity = ConfigUtils.getBooleanFromConfig(var1, "isDisableSanity", false);
        this.isDisableBoredomLevel = ConfigUtils.getBooleanFromConfig(var1, "isDisableBoredomLevel", false);
        this.isDisableUnhappynessLevel = ConfigUtils.getBooleanFromConfig(var1, "isDisableUnhappynessLevel", false);
        this.isDisableWetness = ConfigUtils.getBooleanFromConfig(var1, "isDisableWetness", false);
        this.isDisableInfectionLevel = ConfigUtils.getBooleanFromConfig(var1, "isDisableInfectionLevel", false);
        this.isDisableFakeInfectionLevel = ConfigUtils.getBooleanFromConfig(var1, "isDisableFakeInfectionLevel", false);
        this.isOptimalCalories = ConfigUtils.getBooleanFromConfig(var1, "isOptimalCalories", false);
        this.isOptimalWeight = ConfigUtils.getBooleanFromConfig(var1, "isOptimalWeight", false);
        this.isVisualsEnable = ConfigUtils.getBooleanFromConfig(var1, "isVisualsEnable", true);
        this.isVisualsVehiclesEnable = ConfigUtils.getBooleanFromConfig(var1, "isVisualsVehiclesEnable", false);
        this.isVisualsZombiesEnable = ConfigUtils.getBooleanFromConfig(var1, "isVisualsZombiesEnable", false);
        this.isVisualDrawLineToZombies = ConfigUtils.getBooleanFromConfig(var1, "isVisualDrawLineToZombies", false);
        this.isVisualDrawPlayerNickname = ConfigUtils.getBooleanFromConfig(var1, "isVisualDrawPlayerNickname", false);
        this.isVisualDrawPlayerInfo = ConfigUtils.getBooleanFromConfig(var1, "isVisualDrawPlayerInfo", false);
        this.isVisualDrawLineToVehicle = ConfigUtils.getBooleanFromConfig(var1, "isVisualDrawLineToVehicle", false);
        this.isVisualDrawLineToPlayers = ConfigUtils.getBooleanFromConfig(var1, "isVisualDrawLineToPlayers", false);
        this.isVisualEnable360Vision = ConfigUtils.getBooleanFromConfig(var1, "isVisualEnable360Vision", true);
        this.isMapDrawLocalPlayer = ConfigUtils.getBooleanFromConfig(var1, "isMapDrawLocalPlayer", true);
        this.isMapDrawAllPlayers = ConfigUtils.getBooleanFromConfig(var1, "isMapDrawAllPlayers", false);
        this.isMapDrawVehicles = ConfigUtils.getBooleanFromConfig(var1, "isMapDrawVehicles", false);
        this.isMapDrawZombies = ConfigUtils.getBooleanFromConfig(var1, "isMapDrawZombies", false);
        this.isMapDrawItems = ConfigUtils.getBooleanFromConfig(var1, "isMapDrawItems", false);
        this.isMinimapOpen = ConfigUtils.getBooleanFromConfig(var1, "isMinimapOpen", false);
        this.isNoJam = ConfigUtils.getBooleanFromConfig(var1, "isNoJam", false);
        this.isNoMuscleStrain = ConfigUtils.getBooleanFromConfig(var1, "isNoMuscleStrain", false);
        this.isFullBodyRestore = ConfigUtils.getBooleanFromConfig(var1, "isFullBodyRestore", false);
        this.isCharCreateAllTraits = ConfigUtils.getBooleanFromConfig(var1, "isCharCreateAllTraits", false);
        this.isCharCreateMaxSkills = ConfigUtils.getBooleanFromConfig(var1, "isCharCreateMaxSkills", false);
        this.isCharCreateAllClothes = ConfigUtils.getBooleanFromConfig(var1, "isCharCreateAllClothes", false);
        this.charCreateCustomTraits.clear();
        String ctStr = ConfigUtils.getStringFromConfig(var1, "charCreateCustomTraits", "");
        if (ctStr != null && !ctStr.isEmpty()) {
            for (String t : ctStr.split(",")) {
                if (!t.isEmpty() && !this.charCreateCustomTraits.contains(t)) this.charCreateCustomTraits.add(t);
            }
        }
        this.charCreateCustomSkillLevels.clear();
        String csStr = ConfigUtils.getStringFromConfig(var1, "charCreateCustomSkillLevels", "");
        if (csStr != null && !csStr.isEmpty()) {
            for (String pair : csStr.split(",")) {
                int eq = pair.indexOf('=');
                if (eq > 0) {
                    try {
                        this.charCreateCustomSkillLevels.put(pair.substring(0, eq),
                            Integer.valueOf(pair.substring(eq + 1)));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        this.isVehicleInstantStart = ConfigUtils.getBooleanFromConfig(var1, "isVehicleInstantStart", false);
        this.isFullbright = ConfigUtils.getBooleanFromConfig(var1, "isFullbright", false);
    }

    public EtherAPI() {
        this.initStartupConfig();
        EventSubscriber.register(this);
        this.protectionManager = ProtectionManagerX.getInstance();
    }

    @LuaEvents(value={@SubscribeLuaEvent(eventName="OnResetLua"), @SubscribeLuaEvent(eventName="OnMainMenuEnter")})
    public void loadAPI() {
        Logger.printLog("Loading protected EtherAPI...");
        this.protectionManager.initializeProtection();
        this.protectionManager.initializeProtection();
        EventProtector.getInstance().installProtection();
        if (this.exposer != null) {
            this.exposer.destroy();
        }
        this.exposer = new SafeExposer(this, LuaManager.converterManager, (Platform)LuaManager.platform, LuaManager.env);
        SafeEtherLuaMethods protectedMethods = this.createProtectedMethods();
        this.exposer.exposeAPI(protectedMethods);
        this.exposer.exposeServerSyncBlocker();
        this.initializeProtectedState();
    }

    private SafeEtherLuaMethods createProtectedMethods() {
        return new SafeEtherLuaMethods(this){
            public Object invokeMethod(String name, Object ... args) {
                return this.this$0.protectionManager.invokeFunction(name, args);
            }
        };
    }

    public void handleNetworkPacket(String command, Map<String, Object> data) {
        this.protectionManager.handlePacket(command, data);
    }

    private void initializeProtectedState() {
        try {
            Object connection = GameClientWrapper.getConnection();
            if (connection != null) {
                EtherAPI.setFieldValue(connection);
                GameClientWrapper wrapper = GameClientWrapper.get();
                wrapper.clearIncomingNetData();
            }
        }
        catch (Exception e) {
            Logger.printLog("Error initializing protected state: " + e.getMessage());
        }
    }

    private void clearPendingHandshakes() {
        try {
            GameClientWrapper wrapper = GameClientWrapper.get();
            ArrayList<ZomboidNetData> netData = wrapper.getIncomingNetData();
            if (netData != null) {
                netData.clear();
            }
        }
        catch (Exception e) {
            Logger.printLog("Error clearing handshakes: " + e.getMessage());
        }
    }

    private static void setFieldValue(Object obj) {
        try {
            Field field = FieldCache.getField(obj.getClass(), "validated");
            FieldCache.setFieldValue(obj, field, true);
        }
        catch (Exception e) {
            Logger.printLog("Error setting field value: " + e.getMessage());
        }
    }

    public void resetWeaponsStats() {
        ArrayList var2;
        IsoPlayer var1 = IsoPlayer.getInstance();
        if (var1 != null && (var2 = var1.getInventory().getItems()) != null && !var2.isEmpty()) {
            Iterator var3 = var2.iterator();
            while (true) {
                String var6;
                if (!var3.hasNext()) {
                    return;
                }
                InventoryItem var4 = (InventoryItem)var3.next();
                if (!(var4 instanceof HandWeapon)) continue;
                HandWeapon var5 = (HandWeapon)var4;
                if (!var4.getStringItemType().equals("RangedWeapon") && !var4.getStringItemType().equals("MeleeWeapon") || !this.originalWeaponStats.containsKey(var6 = var5.getFullType())) continue;
                float[] var7 = this.originalWeaponStats.get(var6);
                var5.setExtraDamage(var7[0]);
                var5.setMaxDamage(var7[1]);
                var5.setMinDamage(var7[2]);
                var5.setMaxRange(var7[3]);
                var5.setMinRange(var7[4]);
                var5.setHitChance((int)var7[5]);
                var5.setCriticalDamageMultiplier(var7[6]);
            }
        }
    }

    public void resetCritMax() {
        IsoPlayer player = IsoPlayer.getInstance();
        if (player != null) {
            ArrayList<InventoryItem> items = player.getInventory().getItems();
            if (items != null) {
                for (InventoryItem item : items) {
                    if (!(item instanceof HandWeapon)) {
                        continue;
                    }
                    HandWeapon weapon = (HandWeapon)item;
                    String weaponType = weapon.getFullType();
                    if (this.critMaxAlwaysKnockdown.containsKey(weaponType)) {
                        weapon.setAlwaysKnockdown(this.critMaxAlwaysKnockdown.get(weaponType));
                    }
                }
            }
        }
        this.critMaxAlwaysKnockdown.clear();
    }

    private void applyUnlimitedAmmo(IsoPlayer player, InventoryItem item) {
        if (item == null || !item.getStringItemType().equals("RangedWeapon") || item.getContainer() == null || item.getMaxAmmo() <= 0) {
            return;
        }
        if (item.getCurrentAmmoCount() != item.getMaxAmmo()) {
            item.setCurrentAmmoCount(item.getMaxAmmo());
            INetworkPacket.send(PacketTypes.PacketType.SyncItemFields, player, item);
        }
    }

    // 无限弹药(背包弹匣扩展): 弹匣 = 非武器且 getMaxAmmo()>0 的物品(InventoryItem:836
    // 官方 tooltip 同款判定)。背包内弹匣打空自动回满 + SyncItemFields 上行
    // (服务端 processServer 零校验采纳 currentAmmoCount), 免去手动装填;
    // 与手持锁同款"变化才发", 静止时零包。
    private void applyUnlimitedAmmoToInventory(IsoPlayer player) {
        ArrayList<InventoryItem> items = player.getInventory().getItems();
        if (items == null || items.isEmpty()) {
            return;
        }
        for (InventoryItem item : items) {
            if (item == null || item instanceof HandWeapon || item.getContainer() == null || item.getMaxAmmo() <= 0) {
                continue;
            }
            if (item.getCurrentAmmoCount() != item.getMaxAmmo()) {
                item.setCurrentAmmoCount(item.getMaxAmmo());
                INetworkPacket.send(PacketTypes.PacketType.SyncItemFields, player, item);
            }
        }
    }

    public void farmSetWeaponAmmo() {
        IsoPlayer player = IsoPlayer.getInstance();
        if (player == null) {
            return;
        }
        this.farmSetHandWeapon(player, player.getPrimaryHandItem());
        this.farmSetHandWeapon(player, player.getSecondaryHandItem());
    }

    private void farmSetHandWeapon(IsoPlayer player, InventoryItem item) {
        if (!(item instanceof HandWeapon)) {
            return;
        }
        HandWeapon weapon = (HandWeapon)item;
        if (!weapon.getStringItemType().equals("RangedWeapon") || weapon.getContainer() == null) {
            return;
        }
        weapon.setCurrentAmmoCount(this.ammoFarmCount);
        String magazineType = weapon.getMagazineType();
        if (magazineType != null && !magazineType.isEmpty()) {
            weapon.setContainsClip(true);
        }
        INetworkPacket.send(PacketTypes.PacketType.SyncHandWeaponFields, player, weapon);
    }

    private void updateLocalPlayerFeatures() {
        ArrayList<InventoryItem> var7;
        HandWeapon var3;
        IsoPlayer var1 = IsoPlayer.getInstance();
        if (var1 == null) {
            return;
        }
        InventoryItem var2 = var1.getPrimaryHandItem();
        if (this.isExtraDamage && var2 != null && (var2.getStringItemType().equals("RangedWeapon") || var2.getStringItemType().equals("MeleeWeapon")) && var2 instanceof HandWeapon) {
            var3 = (HandWeapon)var2;
            String var4 = var3.getFullType();
            if (!this.originalWeaponStats.containsKey(var4)) {
                this.originalWeaponStats.put(var4, new float[]{var3.getExtraDamage(), var3.getMaxDamage(), var3.getMinDamage(), var3.getMaxRange(), var3.getMinRange(), var3.getHitChance(), var3.getCriticalDamageMultiplier()});
            }
            var3.setExtraDamage(100000.0f);
            var3.setMaxDamage(1000000.0f);
            var3.setMinDamage(1000000.0f);
            var3.setMaxRange(10000.0f);
            var3.setMinRange(0.0f);
            var3.setHitChance(100);
            var3.setCriticalDamageMultiplier(100000.0f);
        }
        if (this.isCritMax && var2 != null && (var2.getStringItemType().equals("RangedWeapon") || var2.getStringItemType().equals("MeleeWeapon")) && var2 instanceof HandWeapon) {
            var3 = (HandWeapon)var2;
            String critWeaponType = var3.getFullType();
            if (!this.critMaxAlwaysKnockdown.containsKey(critWeaponType)) {
                this.critMaxAlwaysKnockdown.put(critWeaponType, var3.isAlwaysKnockdown());
            }
            var3.setAlwaysKnockdown(true);
            var3.setCriticalChance(100.0f);
        }
        if ((Boolean)SandboxOptions.instance.getOptionByName("MultiHitZombies").asConfigOption().getValueAsObject() != this.isMultiHitZombies) {
            SandboxOptions.instance.set("MultiHitZombies", (Object)this.isMultiHitZombies);
        }
        if (var1.isTimedActionInstantCheat() != this.isTimedActionCheat) {
            var1.setTimedActionInstantCheat(this.isTimedActionCheat);
        }
        if (var1.isWearingNightVisionGoggles() != this.isEnableNightVision) {
            var1.setWearingNightVisionGoggles(this.isEnableNightVision);
        }
        // Fullbright (功能9): 渲染取值改写由 GamePatcher.patchFullbright 三处注入完成
        // (getVertLight 返白 + cacheLightInfo 白化 + updateRenderSettings 夜色清零)。
        // 这里只处理切换边沿的配套动作:
        //  · 视野锥出锥黑幕是独立 overlay (PerformanceSettings.viewConeOpacity, 官方
        //    选项 0-5 默认 3), 开启置 0、关闭还原进入时的值;
        //  · chunk FBO 纹理有缓存 (fboRenderChunk 默认开), 仅在 native 光照值变化时才
        //    重画 —— 调 LightingJNI.buildingsChanged() 强制全量失效, 否则开关切换后已
        //    缓存的暗块/亮块不会刷新。
        if (this.isFullbright != this.fullbrightApplied) {
            this.fullbrightApplied = this.isFullbright;
            if (this.isFullbright) {
                this.fullbrightSavedViewConeOpacity = PerformanceSettings.viewConeOpacity;
                PerformanceSettings.viewConeOpacity = 0;
            }
            else {
                PerformanceSettings.viewConeOpacity = this.fullbrightSavedViewConeOpacity;
            }
            LightingJNI.buildingsChanged();
        }
        if (var1.isGodMod() != this.isEnableGodMode) {
            var1.setGodMod(this.isEnableGodMode);
        }
        if (this.isEnableNoclip) {
            var1.setNoClip(true);
        }
        if (this.isEnableInvisible) {
            var1.setInvisible(true);
        }
        // 僵尸不攻击玩家: 不再走 vanilla setZombiesDontAttack —— 该 setter 被 Role.hasCapability
        // 门禁 (单人需 Core.debug 才放行, 否则强制 false), 是"单人下必须先开解锁调试权限才生效"的
        // 根因, 且关调试会连带触发 Core.debug 运行时切换导致重启。改由 GamePatcher 注入
        // IsoZombie.getShouldAttack (攻击唯一裁决门) 拦截本机玩家, SP/MP 通用、无需调试、无重启。
        // 参见 GamePatcher.patchZombieShouldAttack。
        // 提高枪械射速 (原"神枪手模式"): 射速门只看玩家侧 recoilDelay (AttemptAttack 前置
        // getRecoilDelay()<=0 才可击发)。分两档处理:
        //  · 半自动/单发: 只每帧清玩家侧 recoilDelay, 武器字段不动 => recoilVarX/singleShootSpeed
        //    全 vanilla (无模型扭曲/动画卡死), 射速由射击动画自然限速, 远高于单发 150ms AC 阈值。
        //  · 全自动: 把武器侧 recoilDelay 压到 6 (比旧值 8 更激进, 保留安全冗余) —— 玩家侧每帧衰减
        //    0.625, 每发后取按瞄准/力量缩放的 getRecoilDelay(owner) (双满级最低 ×0.5625), 故实弹间隔
        //    ≈ ceil(6×0.5625/0.625)=6 帧: <400fps 一律 >15ms 全自动阈值 (60fps=100ms / 240fps=25ms
        //    冗余); 值非零 => recoilVarX 未满, 姿势/动画仅轻微加速不扭曲。切非全自动/关功能/换枪时一律
        //    还原脚本 recoilDelay, 防加速值残留把单发打穿 150ms 阈值。
        if (this.isNoRecoil && var2 != null && var2.getStringItemType().equals("RangedWeapon") && var2 instanceof HandWeapon) {
            var3 = (HandWeapon)var2;
            var3.setCriticalChance(100.0f);
            var3.setAlwaysKnockdown(true);
            var3.setAimingTime(0);
            if ("Auto".equals(var3.getFireMode())) {
                if (this.recoilStompedWeapon != null && this.recoilStompedWeapon != var3 && this.recoilStompedWeapon.getScriptItem() != null) {
                    this.recoilStompedWeapon.setRecoilDelay(this.recoilStompedWeapon.getScriptItem().recoilDelay);
                }
                var3.setRecoilDelay(6);
                this.recoilStompedWeapon = var3;
            } else {
                if (this.recoilStompedWeapon != null && this.recoilStompedWeapon.getScriptItem() != null) {
                    this.recoilStompedWeapon.setRecoilDelay(this.recoilStompedWeapon.getScriptItem().recoilDelay);
                }
                this.recoilStompedWeapon = null;
                var1.setRecoilDelay(0.0f);
            }
        } else {
            if (this.recoilStompedWeapon != null && this.recoilStompedWeapon.getScriptItem() != null) {
                this.recoilStompedWeapon.setRecoilDelay(this.recoilStompedWeapon.getScriptItem().recoilDelay);
            }
            this.recoilStompedWeapon = null;
            if (this.isNoRecoil) {
                var1.setRecoilDelay(0.0f);
            }
        }
        if (this.isUnlimitedAmmo) {
            this.applyUnlimitedAmmo(var1, var1.getPrimaryHandItem());
            this.applyUnlimitedAmmo(var1, var1.getSecondaryHandItem());
            this.applyUnlimitedAmmoToInventory(var1);
        }
        if (this.isUnlimitedCondition && var2 != null) {
            if (var2.getHaveBeenRepaired() > 1) {
                var2.setHaveBeenRepaired(1);
            }
            if (var2.getCondition() != var2.getConditionMax() || var2.getHaveBeenRepaired() > 1) {
                var2.setCondition(var2.getConditionMax());
                var2.syncItemFields();
            }
        }
        // 无卡壳: 卡壳 roll 在持有者客户端的装弹/拉栓动作里(ISReloadWeaponAction/
        // ISRackFirearm 调 checkJam), 服务端不重判 —— 每帧清卡壳概率与已卡壳状态;
        // 关闭/换枪时还原进入时的原始概率
        if (this.isNoJam && var2 != null && var2.getStringItemType().equals("RangedWeapon") && var2 instanceof HandWeapon) {
            var3 = (HandWeapon)var2;
            if (this.jamStompedWeapon != var3) {
                if (this.jamStompedWeapon != null) {
                    this.jamStompedWeapon.setJamGunChance(this.jamStompedOriginalChance);
                }
                this.jamStompedWeapon = var3;
                this.jamStompedOriginalChance = var3.getJamGunChance();
            }
            var3.setJamGunChance(0.0f);
            var3.setJammed(false);
        } else if (this.jamStompedWeapon != null) {
            this.jamStompedWeapon.setJamGunChance(this.jamStompedOriginalChance);
            this.jamStompedWeapon = null;
        }
        // 一包三用 (负重/拉伤/回血): PlayerDamagePacket 服务端 parse 零校验直采
        // 客户端自报的 maxWeight/BodyDamage, 但服务端每帧 UpdateStrength 会重算
        // 覆盖 —— 本地踩值 + 每 50ms 重发一次 (20/s 压制 last-writer-wins; 该包
        // anticheats=None, 限流默认 300/s 且超限仅告警)。
        // 注: 原「无尸病」已移除 —— corpseSicknessRate 仅驱动 NOXIOUS_SMELL moodle
        // 档位 (Moodle:458), UpdateIllness 每帧按尸体数重算 rate 并直加
        // CharacterStat.FOOD_SICKNESS, 从不读该字段, 清零只遮指示器不挡病情。
        if (this.isUnlimitedCarry || this.isNoMuscleStrain || this.isFullBodyRestore) {
            if (this.isUnlimitedCarry && var1.getMaxWeight() < 10000) {
                var1.setMaxWeight(10000);
            }
            if (this.isNoMuscleStrain || this.isFullBodyRestore) {
                ArrayList<BodyPart> bodyParts = var1.getBodyDamage().getBodyParts();
                for (int bodyIndex = 0; bodyIndex < bodyParts.size(); ++bodyIndex) {
                    BodyPart bodyPart = bodyParts.get(bodyIndex);
                    if (this.isNoMuscleStrain && bodyPart.getStiffness() > 0.0f) {
                        bodyPart.setStiffness(0.0f);
                    }
                    if (this.isFullBodyRestore && bodyPart.getHealth() < 100.0f) {
                        bodyPart.SetHealth(100.0f);
                    }
                }
            }
            long nowMs = System.currentTimeMillis();
            if (GameClient.client && nowMs - this.lastPlayerDamageSendMs >= 50L) {
                this.lastPlayerDamageSendMs = nowMs;
                GameClient.sendPlayerDamage(var1);
            }
        }
        // 自动修理背包物品 / 修复身上衣物: 本地修复 + 变化时 SyncItemFields 上行(多人化, 2026-08-28)。
        // 服务端 processMaintenanceCheck 每次近战命中 roll 扣耐久(服务端专属), 下行推回;
        // 此处检测 condition 变化即拉满并上行覆盖(last-writer-wins, 与手持无限耐久同款)。
        // 包为裸网络包不走 ClientCommand 通道 → 零 cmd 日志; 背包物品不广播他人
        // (SyncItemFieldsPacket:520 仅地面容器转发) —— 详见 analysis/物品字段锁定链-分析(已实施完成).md。
        // 锋利度: 上行锁不了(processServer 不应用 sharpness)也无须锁 —— 伤害在客户端
        // 计算后随命中包上传, AC 只校验 damage<=100; 本地 applyMaxSharpness 拉满即可
        // (上限=condition/conditionMax, 先修耐久再拉锋利正好吃到满上限)。
        // 修复身上衣物(isRepairClothing)独立开关: 仅对 Clothing —— 清血污/污渍/破洞
        // (含补丁)+修补耐久; 视觉清理结果随同一 SyncItemFields 包的 itemVisual/patches
        // 字段同步(服务端 processClothing 应用)。自动修理不碰衣物视觉(语义分离)。
        if ((this.isAutoRepairItems || this.isRepairClothing) && (var7 = var1.getInventory().getItems()) != null && !var7.isEmpty()) {
            for (InventoryItem var5 : var7) {                if (var5 == null) continue;
                boolean isClothingTarget = this.isRepairClothing && var5 instanceof Clothing && var5.getVisual() instanceof ItemVisual;
                boolean conditionChanged = (this.isAutoRepairItems || isClothingTarget) && var5.getCondition() != var5.getConditionMax();
                if (this.isAutoRepairItems) {
                    if (var5.isBroken()) {
                        var5.setBroken(false);
                    }
                    var5.setHaveBeenRepaired(1);
                    var5.setWet(false);
                    var5.setInfected(false);
                    if (var5.hasSharpness() && var5.getSharpness() < var5.getMaxSharpness()) {
                        var5.applyMaxSharpness();
                    }
                }
                boolean visualChanged = false;
                if (isClothingTarget) {
                    ItemVisual visual = (ItemVisual)var5.getVisual();
                    boolean dirty = visual.getTotalBlood() > 0.0f || visual.getHolesNumber() > 0;
                    if (!dirty) {
                        for (int var6 = 0; var6 < BloodBodyPartType.MAX.index(); ++var6) {
                            if (visual.getDirt(BloodBodyPartType.FromIndex(var6)) > 0.0f) {
                                dirty = true;
                                break;
                            }
                        }
                    }
                    if (dirty) {
                        visualChanged = true;
                        for (int var6 = 0; var6 < BloodBodyPartType.MAX.index(); ++var6) {
                            visual.removeHole(var6);
                            visual.removePatch(var6);
                        }
                        visual.removeBlood();
                        visual.removeDirt();
                    }
                }
                if (conditionChanged) {
                    var5.setCondition(var5.getConditionMax());
                }
                if ((conditionChanged || visualChanged) && GameClient.client) {
                    var5.syncItemFields();
                }
            }
        }
        if (this.isUnlimitedEndurance) {
            var1.getStats().set(CharacterStat.ENDURANCE, 1.0f);
        }
        if (this.isDisableFatigue) {
            var1.getStats().set(CharacterStat.FATIGUE, 0.0f);
        }
        if (this.isDisableHunger) {
            var1.getStats().set(CharacterStat.HUNGER, 0.0f);
        }
        if (this.isDisableThirst) {
            var1.getStats().set(CharacterStat.THIRST, 0.0f);
        }
        if (this.isDisableDrunkenness) {
            var1.getStats().set(CharacterStat.INTOXICATION, 0.0f);
        }
        if (this.isDisableAnger) {
            var1.getStats().set(CharacterStat.ANGER, 0.0f);
        }
        if (this.isDisableFear) {
            var1.getStats().set(CharacterStat.PANIC, 0.0f);
        }
        if (this.isDisablePain) {
            var1.getStats().set(CharacterStat.PAIN, 0.0f);
        }
        if (this.isDisablePanic) {
            var1.getStats().set(CharacterStat.PANIC, 0.0f);
        }
        if (this.isDisableMorale) {
            var1.getStats().set(CharacterStat.MORALE, 1.0f);
        }
        if (this.isDisableStress) {
            var1.getStats().set(CharacterStat.STRESS, 0.0f);
        }
        if (this.isDisableSickness) {
            var1.getStats().set(CharacterStat.SICKNESS, 0.0f);
        }
        if (this.isDisableStressFromCigarettes) {
            var1.getStats().set(CharacterStat.NICOTINE_WITHDRAWAL, 0.0f);
        }
        if (this.isDisableSanity) {
            var1.getStats().set(CharacterStat.SANITY, 1.0f);
        }
        if (this.isDisableBoredomLevel) {
            var1.getStats().set(CharacterStat.BOREDOM, 0.0f);
        }
        if (this.isDisableUnhappynessLevel) {
            var1.getStats().set(CharacterStat.UNHAPPINESS, 0.0f);
        }
        if (this.isDisableWetness) {
            var1.getStats().set(CharacterStat.WETNESS, 0.0f);
        }
        if (this.isDisableInfectionLevel) {
            var1.getStats().set(CharacterStat.ZOMBIE_INFECTION, 0.0f);
        }
        if (this.isDisableFakeInfectionLevel) {
            var1.getStats().set(CharacterStat.FOOD_SICKNESS, 0.0f);
        }
        if (this.isOptimalCalories) {
            var1.getNutrition().setCalories(1200.0f);
        }
        if (this.isOptimalWeight) {
            var1.getNutrition().setWeight(80.0);
        }
    }

    private void bypassDebugMode() {
        if (!this.initialCoreDebugCaptured) {
            this.initialCoreDebugCaptured = true;
            this.initialCoreDebug = Core.debug;
        }
        // 只锁存"开启", 绝不在运行时把 Core.debug 由 true 翻回 false: B42 不支持运行时关闭
        // 调试模式, 关闭会连带把开启时初始化的调试子系统不一致拆除 -> 崩溃/游戏重启 (即用户反馈
        // "同时开启后再关掉解锁调试权限就游戏重启"的根因)。取消勾选改为下次进游戏才生效。
        boolean singlePlayer = GameClient.ingame && !GameClient.client && !GameServer.server;
        if (singlePlayer && this.isBypassDebugMode && !Core.debug) {
            Core.debug = true;
        }
    }

    @SubscribeLuaEvent(eventName="OnPostUIDraw")
    public void updateVisuals() {
        try {
            this.updatePlayersVisuals();
            this.updateVehiclesVisuals();
            this.updateZombiesVisuals();
            this.updateUltraPlayerVision();
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    public void updateUltraPlayerVision() {
        if (this.isVisualEnable360Vision) {
            GameClient instance;
            ArrayList<IsoPlayer> var8;
            ArrayList<IsoZombie> var6;
            Set<BaseVehicle> var1 = IsoWorld.instance.getCell().getVehicles();
            if (var1 != null && !var1.isEmpty()) {
                for (BaseVehicle var3 : var1) {
                    var3.setAlpha(100.0f);
                }
            }
            if ((var6 = IsoWorld.instance.getCell().getZombieList()) != null && !var6.isEmpty()) {
                for (IsoZombie var4 : var6) {
                    var4.setAlpha(100.0f);
                }
            }
            ArrayList arrayList = var8 = (instance = GameClientWrapper.getInstance()) != null ? instance.getPlayers() : null;
            if (var8 != null && !var8.isEmpty()) {
                for (IsoPlayer var5 : var8) {
                    if (var5.isLocalPlayer()) continue;
                    var5.setAlpha(100.0f);
                }
            }
        }
    }

    private void updateVehiclesVisuals() {
        IsoPlayer var1;
        if (this.isVisualsEnable && (this.isVisualsVehiclesEnable || this.isVisualDrawLineToVehicle) && (var1 = IsoPlayer.getInstance()) != null) {
            Set<BaseVehicle> var2 = IsoWorld.instance.getCell().getVehicles();
            float var3 = PlayerUtils.getScreenPositionX(var1);
            float var4 = PlayerUtils.getScreenPositionY(var1);
            float var5 = this.vehiclesUIColor.a;
            float var6 = this.vehiclesUIColor.r;
            float var7 = this.vehiclesUIColor.g;
            float var8 = this.vehiclesUIColor.b;
            if (var2 != null && !var2.isEmpty()) {
                float scale = Rendering.espTextScale();
                int lineH = Rendering.getEspLineH();
                for (BaseVehicle var10 : var2) {
                    float var11 = VehicleUtils.getScreenPositionX(var10);
                    float var12 = VehicleUtils.getScreenPositionY(var10);
                    if (this.isVisualsVehiclesEnable) {
                        Rendering.drawEspTextCenter(EtherMain.getInstance().etherTranslator.getTranslate("UI_VisualsDraws_VehiclePower") + var10.getEnginePower() / 10.0f, scale, var11, var12, var6, var7, var8, var5);
                        Rendering.drawEspTextCenter(EtherMain.getInstance().etherTranslator.getTranslate("UI_VisualsDraws_VehicleSpeed") + var10.getMaxSpeed(), scale, var11, var12 + (float)lineH, var6, var7, var8, var5);
                    }
                    if (!this.isVisualDrawLineToVehicle) continue;
                    int var13 = (int)PlayerUtils.getDistanceBetweenPlayerAndVehicle(var1, var10);
                    int var14 = Math.max(30, Math.min(150, var13));
                    float var15 = (float)Math.sqrt(Math.pow(var11 - var3, 2.0) + Math.pow(var12 - var4, 2.0));
                    float var16 = (float)var14 / var15;
                    float var17 = var3 + var16 * (var11 - var3);
                    float var18 = var4 + 60.0f + var16 * (var12 - var4);
                    Rendering.drawThinLine(var11, var12, var3, var4 + 60.0f, var6, var7, var8, 0.8f);
                    Rendering.drawEspTextCenter(String.valueOf(var13), scale, var17, var18, var6, var7, var8, var5);
                }
            }
        }
    }

    private void updateZombiesVisuals() {
        IsoPlayer var1;
        if (this.isVisualsEnable && (this.isVisualsZombiesEnable || this.isVisualDrawLineToZombies) && (var1 = IsoPlayer.getInstance()) != null) {
            ArrayList<IsoZombie> var2 = IsoWorld.instance.getCell().getZombieList();
            float var3 = PlayerUtils.getScreenPositionX(var1);
            float var4 = PlayerUtils.getScreenPositionY(var1);
            float var5 = this.zombiesUIColor.a;
            float var6 = this.zombiesUIColor.r;
            float var7 = this.zombiesUIColor.g;
            float var8 = this.zombiesUIColor.b;
            if (var2 != null && !var2.isEmpty()) {
                float scale = Rendering.espTextScale();
                for (IsoZombie var9 : var2) {
                    float var10 = ZombieUtils.getScreenPositionX(var9);
                    float var11 = ZombieUtils.getScreenPositionY(var9);
                    if (this.isVisualsZombiesEnable) {
                        Rendering.drawEspTextCenter("HP: " + (int)(var9.getHealth() * 100.0f), scale, var10, var11 - 10.0f, var6, var7, var8, var5);
                    }
                    if (!this.isVisualDrawLineToZombies || !(PlayerUtils.getDistanceBetweenPlayerAndZombie(var1, var9) < 150.0f)) continue;
                    int var12 = (int)PlayerUtils.getDistanceBetweenPlayerAndZombie(var1, var9);
                    int var13 = Math.max(30, Math.min(150, var12));
                    float var14 = (float)Math.sqrt(Math.pow(var10 - var3, 2.0) + Math.pow(var11 - var4, 2.0));
                    float var15 = (float)var13 / var14;
                    float var16 = var3 + var15 * (var10 - var3);
                    float var17 = var4 + 60.0f + var15 * (var11 - var4);
                    Rendering.drawThinLine(var10, var11, var3, var4 + 60.0f, var6, var7, var8, 0.8f);
                    Rendering.drawEspTextCenter(String.valueOf(var12), scale, var16, var17, var6, var7, var8, var5);
                }
            }
        }
    }

    private void updatePlayersVisuals() {
        IsoPlayer var1;
        if (this.isVisualsEnable && (this.isVisualDrawPlayerNickname || this.isVisualDrawPlayerInfo || this.isVisualDrawLineToPlayers) && (var1 = IsoPlayer.getInstance()) != null) {
            GameClient instance = GameClientWrapper.getInstance();
            ArrayList<IsoPlayer> var2 = instance != null ? instance.getPlayers() : null;
            float var3 = PlayerUtils.getScreenPositionX(var1);
            float var4 = PlayerUtils.getScreenPositionY(var1);
            float var5 = this.playersUIColor.a;
            float var6 = this.playersUIColor.r;
            float var7 = this.playersUIColor.g;
            float var8 = this.playersUIColor.b;
            if (var2 != null && !var2.isEmpty()) {
                float scale = Rendering.espTextScale();
                int lineH = Rendering.getEspLineH();
                Iterator var9 = var2.iterator();
                while (true) {
                    if (!var9.hasNext()) {
                        return;
                    }
                    IsoPlayer var10 = (IsoPlayer)var9.next();
                    float var11 = PlayerUtils.getScreenPositionX(var10);
                    float var12 = PlayerUtils.getScreenPositionY(var10);
                    if (var10.isLocalPlayer()) continue;
                    if (this.isVisualDrawPlayerNickname) {
                        Rendering.drawEspTextCenter(var10.getUsername(), scale, var11, var12 - 30.0f, var6, var7, var8, var5);
                    }
                    if (this.isVisualDrawPlayerInfo) {
                        String var13 = var10.getPrimaryHandItem() != null ? var10.getPrimaryHandItem().getDisplayName() : "None";
                        String var14 = var10.getSecondaryHandItem() != null ? var10.getSecondaryHandItem().getDisplayName() : "None";
                        Rendering.drawEspTextCenter(EtherMain.getInstance().etherTranslator.getTranslate("UI_VisualsDraws_PrimaryHand") + var13, scale, var11, var12 + 70.0f, var6, var7, var8, var5);
                        Rendering.drawEspTextCenter(EtherMain.getInstance().etherTranslator.getTranslate("UI_VisualsDraws_SecondaryHand") + var14, scale, var11, var12 + 70.0f + (float)lineH, var6, var7, var8, var5);
                    }
                    if (var10.isLocalPlayer() || !this.isVisualDrawLineToPlayers || !(PlayerUtils.getDistanceBetweenPlayers(var1, var10) < 150.0f)) continue;
                    int var19 = (int)PlayerUtils.getDistanceBetweenPlayers(var10, var1);
                    int var20 = Math.max(30, Math.min(150, var19));
                    float var15 = (float)Math.sqrt(Math.pow(var11 - var3, 2.0) + Math.pow(var12 - var4, 2.0));
                    float var16 = (float)var20 / var15;
                    float var17 = var3 + var16 * (var11 - var3);
                    float var18 = var4 + 60.0f + var16 * (var12 - var4);
                    Rendering.drawThinLine(var11, var12, var3, var4 + 60.0f, var6, var7, var8, 0.8f);
                    Rendering.drawEspTextCenter(String.valueOf(var19), scale, var17, var18, var6, var7, var8, var5);
                }
            }
        }
    }

    @SubscribeLuaEvent(eventName="OnRenderTick")
    public synchronized void updateAPI() {
        try {
            this.updateLocalPlayerFeatures();
            this.bypassDebugMode();
            if (ServerSyncBlocker.isProtectionActive()) {
                ServerSyncBlocker.filterIncomingSyncPackets();
            }
        }
        catch (Exception e) {
            Logger.printLog("Error in updateAPI: " + e.getMessage());
        }
    }

    @SubscribeLuaEvent(eventName="OnTick")
    public void onTickUpdate() {
        try {
            IsoPlayer player = IsoPlayer.getInstance();
            if (player == null || player.isDead()) {
                return;
            }
            if (this.isUnlimitedEndurance) {
                player.getStats().set(CharacterStat.ENDURANCE, 1.0f);
            }
            if (this.isDisableFatigue) {
                player.getStats().set(CharacterStat.FATIGUE, 0.0f);
            }
            if (this.isDisableHunger) {
                player.getStats().set(CharacterStat.HUNGER, 0.0f);
            }
            if (this.isDisableThirst) {
                player.getStats().set(CharacterStat.THIRST, 0.0f);
            }
            if (this.isDisableDrunkenness) {
                player.getStats().set(CharacterStat.INTOXICATION, 0.0f);
            }
            if (this.isDisableAnger) {
                player.getStats().set(CharacterStat.ANGER, 0.0f);
            }
            if (this.isDisableFear) {
                player.getStats().set(CharacterStat.PANIC, 0.0f);
            }
            if (this.isDisablePain) {
                player.getStats().set(CharacterStat.PAIN, 0.0f);
            }
            if (this.isDisablePanic) {
                player.getStats().set(CharacterStat.PANIC, 0.0f);
            }
            if (this.isDisableMorale) {
                player.getStats().set(CharacterStat.MORALE, 1.0f);
            }
            if (this.isDisableStress) {
                player.getStats().set(CharacterStat.STRESS, 0.0f);
            }
            if (this.isDisableSickness) {
                player.getStats().set(CharacterStat.SICKNESS, 0.0f);
            }
            if (this.isDisableStressFromCigarettes) {
                player.getStats().set(CharacterStat.NICOTINE_WITHDRAWAL, 0.0f);
            }
            if (this.isDisableSanity) {
                player.getStats().set(CharacterStat.SANITY, 1.0f);
            }
            if (this.isDisableBoredomLevel) {
                player.getStats().set(CharacterStat.BOREDOM, 0.0f);
            }
            if (this.isDisableUnhappynessLevel) {
                player.getStats().set(CharacterStat.UNHAPPINESS, 0.0f);
            }
            if (this.isDisableWetness) {
                player.getStats().set(CharacterStat.WETNESS, 0.0f);
            }
            if (this.isDisableInfectionLevel) {
                player.getStats().set(CharacterStat.ZOMBIE_INFECTION, 0.0f);
            }
            if (this.isDisableFakeInfectionLevel) {
                player.getStats().set(CharacterStat.FOOD_SICKNESS, 0.0f);
            }
            if (this.isOptimalCalories) {
                player.getNutrition().setCalories(1200.0f);
            }
            if (this.isOptimalWeight) {
                player.getNutrition().setWeight(80.0);
            }
            if (ServerSyncBlocker.isProtectionActive()) {
                ServerSyncBlocker.reapplyProtectedValues();
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    private class SafeExposer
    extends Exposer {
        final /* synthetic */ EtherAPI this$0;

        public SafeExposer(EtherAPI etherAPI, KahluaConverterManager m, Platform p, KahluaTable e) {
            EtherAPI etherAPI2 = etherAPI;
            Objects.requireNonNull(etherAPI2);
            this.this$0 = etherAPI2;
            super(m, (J2SEPlatform)p, e);
        }

        public void exposeAPI(EtherLuaMethods methods) {
            for (Method method : methods.getClass().getMethods()) {
                if (!method.isAnnotationPresent(LuaMethod.class)) continue;
                String originalName = method.getName();
                String safeName = this.this$0.safeAPI.getSafeName(originalName);
                this.exposeGlobalFunction(method, safeName);
            }
        }

        public void exposeServerSyncBlocker() {
            for (Method method : ServerSyncBlocker.class.getMethods()) {
                if (!method.isAnnotationPresent(LuaMethod.class)) continue;
                LuaMethod annotation = method.getAnnotation(LuaMethod.class);
                String name = annotation.name();
                if (name == null || name.isEmpty()) {
                    name = method.getName();
                }
                // static method -> global function (was exposeMethod, which only
                // attaches to the class metatable and never became a Lua global)
                this.exposeGlobalClassFunction(LuaManager.env, ServerSyncBlocker.class, method, name);
                Logger.printLog("Exposed ServerSyncBlocker method: " + name);
            }
        }

        private void exposeGlobalFunction(Method method, String name) {
            this.exposeMethod(method.getDeclaringClass(), method, name, LuaManager.env);
        }
    }

    public class SafeEtherLuaMethods
    extends EtherLuaMethods {
        final /* synthetic */ EtherAPI this$0;

        public SafeEtherLuaMethods(EtherAPI this$0) {
            EtherAPI etherAPI = this$0;
            Objects.requireNonNull(etherAPI);
            this.this$0 = etherAPI;
        }

        public Object callMethod(String name, Object ... args) {
            String originalName = this.this$0.safeAPI.getOriginalName(name);
            if (originalName != null) {
                try {
                    Method method = this.getClass().getMethod(originalName, this.getParameterTypes(args));
                    return method.invoke(this, args);
                }
                catch (Exception e) {
                    Logger.printLog("Error calling method " + originalName + ": " + e.getMessage());
                }
            }
            return null;
        }

        private Class<?>[] getParameterTypes(Object[] args) {
            Class[] types = new Class[args.length];
            for (int i = 0; i < args.length; ++i) {
                types[i] = args[i].getClass();
            }
            return types;
        }
    }
}
