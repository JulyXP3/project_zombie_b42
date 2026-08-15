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
 *  zombie.ui.UIFont
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
import zombie.characters.CharacterStat;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.textures.Texture;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.HandWeapon;
import zombie.iso.IsoWorld;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.ServerOptions;
import zombie.network.packets.INetworkPacket;
import zombie.network.ZomboidNetData;
import zombie.ui.UIFont;
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
    public boolean isBypassDebugMode;
    public boolean isUnlimitedCarry;
    public boolean isUnlimitedCondition;
    public boolean isUnlimitedEndurance;
    public boolean isUnlimitedAmmo;
    public int ammoFarmCount = 30;
    public boolean isCritMax;
    public float combatSpeedMultiplier = 1.0f;
    public boolean isAutoRepairItems;
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
    public boolean isVisualsEnable;
    public boolean isVisualsPlayersEnable;
    public boolean isVisualsVehiclesEnable;
    public boolean isVisualsZombiesEnable;
    public boolean isVisualDrawToLocalPlayer;
    public boolean isVisualDrawPlayerNickname;
    public boolean isVisualDrawCredits;
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
        var3.setProperty("isBypassDebugMode", Boolean.toString(this.isBypassDebugMode));
        var3.setProperty("isUnlimitedCarry", Boolean.toString(this.isUnlimitedCarry));
        var3.setProperty("isUnlimitedCondition", Boolean.toString(this.isUnlimitedCondition));
        var3.setProperty("isUnlimitedEndurance", Boolean.toString(this.isUnlimitedEndurance));
        var3.setProperty("isUnlimitedAmmo", Boolean.toString(this.isUnlimitedAmmo));
        var3.setProperty("ammoFarmCount", Integer.toString(this.ammoFarmCount));
        var3.setProperty("isCritMax", Boolean.toString(this.isCritMax));
        var3.setProperty("combatSpeedMultiplier", Float.toString(this.combatSpeedMultiplier));
        var3.setProperty("isAutoRepairItems", Boolean.toString(this.isAutoRepairItems));
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
        var3.setProperty("isVisualsPlayersEnable", Boolean.toString(this.isVisualsPlayersEnable));
        var3.setProperty("isVisualsVehiclesEnable", Boolean.toString(this.isVisualsVehiclesEnable));
        var3.setProperty("isVisualsZombiesEnable", Boolean.toString(this.isVisualsZombiesEnable));
        var3.setProperty("isVisualDrawToLocalPlayer", Boolean.toString(this.isVisualDrawToLocalPlayer));
        var3.setProperty("isVisualDrawPlayerNickname", Boolean.toString(this.isVisualDrawPlayerNickname));
        var3.setProperty("isVisualDrawCredits", Boolean.toString(this.isVisualDrawCredits));
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
        this.mainUIAccentColor = ConfigUtils.getColorFromConfig(var3, "mainUIAccentColor", new Color(56, 239, 125));
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
        this.isBypassDebugMode = ConfigUtils.getBooleanFromConfig(var3, "isBypassDebugMode", false);
        this.isUnlimitedCarry = ConfigUtils.getBooleanFromConfig(var3, "isUnlimitedCarry", false);
        this.isUnlimitedCondition = ConfigUtils.getBooleanFromConfig(var3, "isUnlimitedCondition", false);
        this.isUnlimitedEndurance = ConfigUtils.getBooleanFromConfig(var3, "isUnlimitedEndurance", false);
        this.isUnlimitedAmmo = ConfigUtils.getBooleanFromConfig(var3, "isUnlimitedAmmo", false);
        this.ammoFarmCount = ConfigUtils.getIntFromConfig(var3, "ammoFarmCount", 30);
        this.isCritMax = ConfigUtils.getBooleanFromConfig(var3, "isCritMax", false);
        this.combatSpeedMultiplier = ConfigUtils.getFloatFromConfig(var3, "combatSpeedMultiplier", 1.0f);
        this.isAutoRepairItems = ConfigUtils.getBooleanFromConfig(var3, "isAutoRepairItems", false);
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
        this.isVisualsEnable = ConfigUtils.getBooleanFromConfig(var3, "isVisualsEnable", false);
        this.isVisualsPlayersEnable = ConfigUtils.getBooleanFromConfig(var3, "isVisualsPlayersEnable", false);
        this.isVisualsVehiclesEnable = ConfigUtils.getBooleanFromConfig(var3, "isVisualsVehiclesEnable", false);
        this.isVisualsZombiesEnable = ConfigUtils.getBooleanFromConfig(var3, "isVisualsZombiesEnable", false);
        this.isVisualDrawToLocalPlayer = ConfigUtils.getBooleanFromConfig(var3, "isVisualDrawToLocalPlayer", false);
        this.isVisualDrawPlayerNickname = ConfigUtils.getBooleanFromConfig(var3, "isVisualDrawPlayerNickname", false);
        this.isVisualDrawCredits = ConfigUtils.getBooleanFromConfig(var3, "isVisualDrawCredits", true);
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
    }

    private void initStartupConfig() {
        if (!new File("EtherHack/config/startup.properties").exists()) {
            this.mainUIAccentColor = new Color(56, 239, 125);
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
        this.mainUIAccentColor = ConfigUtils.getColorFromConfig(var1, "mainUIAccentColor", new Color(56, 239, 125));
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
        this.isBypassDebugMode = ConfigUtils.getBooleanFromConfig(var1, "isBypassDebugMode", false);
        this.isUnlimitedCarry = ConfigUtils.getBooleanFromConfig(var1, "isUnlimitedCarry", false);
        this.isUnlimitedCondition = ConfigUtils.getBooleanFromConfig(var1, "isUnlimitedCondition", false);
        this.isUnlimitedEndurance = ConfigUtils.getBooleanFromConfig(var1, "isUnlimitedEndurance", false);
        this.isUnlimitedAmmo = ConfigUtils.getBooleanFromConfig(var1, "isUnlimitedAmmo", false);
        this.isCritMax = ConfigUtils.getBooleanFromConfig(var1, "isCritMax", false);
        this.combatSpeedMultiplier = ConfigUtils.getFloatFromConfig(var1, "combatSpeedMultiplier", 1.0f);
        this.isAutoRepairItems = ConfigUtils.getBooleanFromConfig(var1, "isAutoRepairItems", false);
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
        this.isVisualsEnable = ConfigUtils.getBooleanFromConfig(var1, "isVisualsEnable", false);
        this.isVisualsPlayersEnable = ConfigUtils.getBooleanFromConfig(var1, "isVisualsPlayersEnable", false);
        this.isVisualsVehiclesEnable = ConfigUtils.getBooleanFromConfig(var1, "isVisualsVehiclesEnable", false);
        this.isVisualsZombiesEnable = ConfigUtils.getBooleanFromConfig(var1, "isVisualsZombiesEnable", false);
        this.isVisualDrawToLocalPlayer = ConfigUtils.getBooleanFromConfig(var1, "isVisualDrawToLocalPlayer", false);
        this.isVisualDrawPlayerNickname = ConfigUtils.getBooleanFromConfig(var1, "isVisualDrawPlayerNickname", false);
        this.isVisualDrawCredits = ConfigUtils.getBooleanFromConfig(var1, "isVisualDrawCredits", true);
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
        if (var1.isGodMod() != this.isEnableGodMode) {
            var1.setGodMod(this.isEnableGodMode);
        }
        if (this.isEnableNoclip) {
            var1.setNoClip(true);
        }
        if (this.isEnableInvisible) {
            var1.setInvisible(true);
        }
        if (this.isZombieDontAttack) {
            var1.setZombiesDontAttack(true);
        }
        if (this.isNoRecoil && var2 != null && var2.getStringItemType().equals("RangedWeapon") && var2 instanceof HandWeapon) {
            var3 = (HandWeapon)var2;
            var3.setRecoilDelay(0);
            var3.setCriticalChance(100.0f);
            var3.setAlwaysKnockdown(true);
            var3.setAimingTime(0);
        }
        if (this.isUnlimitedAmmo) {
            this.applyUnlimitedAmmo(var1, var1.getPrimaryHandItem());
            this.applyUnlimitedAmmo(var1, var1.getSecondaryHandItem());
        }
        if (this.isUnlimitedCondition && var2 != null) {
            if (var2.getHaveBeenRepaired() > 1) {
                var2.setHaveBeenRepaired(1);
            }
            var2.setCondition(var2.getConditionMax());
        }
        if (this.isAutoRepairItems && (var7 = var1.getInventory().getItems()) != null && !var7.isEmpty()) {
            for (InventoryItem var5 : var7) {                if (var5 == null) continue;
                if (var5.isBroken()) {
                    var5.setBroken(false);
                }
                var5.setHaveBeenRepaired(1);
                if (var5.getVisual() != null) {
                    for (int var6 = 0; var6 < BloodBodyPartType.MAX.index(); ++var6) {
                        var5.getVisual().removeHole(var6);
                        var5.getVisual().removeDirt();
                        var5.getVisual().removeBlood();
                    }
                }
                var5.setWet(false);
                var5.setInfected(false);
                var5.setCondition(var5.getConditionMax());
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
        boolean var1 = GameClient.ingame;
        Boolean antiCheatOption = ServerOptions.instance.getBoolean("AntiCheatProtectionType12");
        boolean var2 = antiCheatOption != null && antiCheatOption != false;
        boolean var3 = GameServer.server;
        boolean var4 = GameServer.coop;
        Core.debug = var1 && this.isBypassDebugMode && (!var2 && var3 || var4 || !var3);
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
        if (this.isVisualsEnable && this.isVisualsVehiclesEnable && (var1 = IsoPlayer.getInstance()) != null) {
            Set<BaseVehicle> var2 = IsoWorld.instance.getCell().getVehicles();
            float var3 = PlayerUtils.getScreenPositionX(var1);
            float var4 = PlayerUtils.getScreenPositionY(var1);
            float var5 = this.vehiclesUIColor.a;
            float var6 = this.vehiclesUIColor.r;
            float var7 = this.vehiclesUIColor.g;
            float var8 = this.vehiclesUIColor.b;
            if (var2 != null && !var2.isEmpty()) {
                for (BaseVehicle var10 : var2) {
                    float var11 = VehicleUtils.getScreenPositionX(var10);
                    float var12 = VehicleUtils.getScreenPositionY(var10);
                    Rendering.drawTextCenterWithShadow("ID:" + var10.getScriptName(), UIFont.Small, var11, var12, var6, var7, var8, var5);
                    Rendering.drawTextCenterWithShadow(EtherMain.getInstance().etherTranslator.getTranslate("UI_VisualsDraws_VehicleSpeed") + var10.getMaxSpeed(), UIFont.Small, var11, var12 + 10.0f, var6, var7, var8, var5);
                    if (!this.isVisualDrawLineToVehicle) continue;
                    int var13 = (int)PlayerUtils.getDistanceBetweenPlayerAndVehicle(var1, var10);
                    int var14 = Math.max(30, Math.min(150, var13));
                    float var15 = (float)Math.sqrt(Math.pow(var11 - var3, 2.0) + Math.pow(var12 - var4, 2.0));
                    float var16 = (float)var14 / var15;
                    float var17 = var3 + var16 * (var11 - var3);
                    float var18 = var4 + 60.0f + var16 * (var12 - var4);
                    Rendering.drawLine((int)var11, (int)var12, (int)var3, (int)var4 + 60, var6, var7, var8, 0.8f, 1);
                    Rendering.drawTextCenterWithShadow(String.valueOf(var13), UIFont.Small, var17, var18, var6, var7, var8, var5);
                }
            }
        }
    }

    private void updateZombiesVisuals() {
        IsoPlayer var1;
        if (this.isVisualsEnable && this.isVisualsZombiesEnable && (var1 = IsoPlayer.getInstance()) != null) {
            ArrayList<IsoZombie> var2 = IsoWorld.instance.getCell().getZombieList();
            float var3 = this.zombiesUIColor.a;
            float var4 = this.zombiesUIColor.r;
            float var5 = this.zombiesUIColor.g;
            float var6 = this.zombiesUIColor.b;
            if (var2 != null && !var2.isEmpty()) {
                for (IsoZombie var8 : var2) {
                    float var9 = ZombieUtils.getScreenPositionX(var8);
                    float var10 = ZombieUtils.getScreenPositionY(var8);
                    int var11 = (int)(var8.getHealth() * 100.0f);
                    Rendering.drawTextCenterWithShadow(EtherMain.getInstance().etherTranslator.getTranslate("UI_VisualsDraws_ZombieTitle"), UIFont.Small, var9, var10, var4, var5, var6, var3);
                    Rendering.drawTextCenterWithShadow(EtherMain.getInstance().etherTranslator.getTranslate("UI_VisualsDraws_ZombieHealth") + var11, UIFont.Small, var9, var10 + 10.0f, var4, var5, var6, var3);
                }
            }
        }
    }

    private void updatePlayersVisuals() {
        IsoPlayer var1;
        if (this.isVisualsEnable && this.isVisualsPlayersEnable && (var1 = IsoPlayer.getInstance()) != null) {
            GameClient instance = GameClientWrapper.getInstance();
            ArrayList<IsoPlayer> var2 = instance != null ? instance.getPlayers() : null;
            float var3 = PlayerUtils.getScreenPositionX(var1);
            float var4 = PlayerUtils.getScreenPositionY(var1);
            float var5 = this.playersUIColor.a;
            float var6 = this.playersUIColor.r;
            float var7 = this.playersUIColor.g;
            float var8 = this.playersUIColor.b;
            if (var2 != null && !var2.isEmpty()) {
                Iterator var9 = var2.iterator();
                while (true) {
                    if (!var9.hasNext()) {
                        return;
                    }
                    IsoPlayer var10 = (IsoPlayer)var9.next();
                    float var11 = PlayerUtils.getScreenPositionX(var10);
                    float var12 = PlayerUtils.getScreenPositionY(var10);
                    if (var10.isLocalPlayer() && !this.isVisualDrawToLocalPlayer) continue;
                    if (this.isVisualDrawPlayerNickname) {
                        Rendering.drawTextCenterWithShadow(var10.getUsername(), UIFont.Small, var11, var12 - 30.0f, var6, var7, var8, var5);
                    }
                    if (this.isVisualDrawPlayerInfo) {
                        String var13 = var10.getPrimaryHandItem() != null ? var10.getPrimaryHandItem().getDisplayName() : "None";
                        String var14 = var10.getSecondaryHandItem() != null ? var10.getSecondaryHandItem().getDisplayName() : "None";
                        Rendering.drawTextCenterWithShadow(EtherMain.getInstance().etherTranslator.getTranslate("UI_VisualsDraws_PrimaryHand") + var13, UIFont.Small, var11, var12 + 70.0f, var6, var7, var8, var5);
                        Rendering.drawTextCenterWithShadow(EtherMain.getInstance().etherTranslator.getTranslate("UI_VisualsDraws_SecondaryHand") + var14, UIFont.Small, var11, var12 + 80.0f, var6, var7, var8, var5);
                    }
                    if (var10.isLocalPlayer() || !this.isVisualDrawLineToPlayers || !(PlayerUtils.getDistanceBetweenPlayers(var1, var10) < 150.0f)) continue;
                    int var19 = (int)PlayerUtils.getDistanceBetweenPlayers(var10, var1);
                    int var20 = Math.max(30, Math.min(150, var19));
                    float var15 = (float)Math.sqrt(Math.pow(var11 - var3, 2.0) + Math.pow(var12 - var4, 2.0));
                    float var16 = (float)var20 / var15;
                    float var17 = var3 + var16 * (var11 - var3);
                    float var18 = var4 + 60.0f + var16 * (var12 - var4);
                    Rendering.drawLine((int)var11, (int)var12, (int)var3, (int)var4 + 60, var6, var7, var8, 0.8f, 1);
                    Rendering.drawTextCenterWithShadow(String.valueOf(var19), UIFont.Small, var17, var18, var6, var7, var8, var5);
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
