/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  se.krka.kahlua.vm.KahluaTable
 *  se.krka.kahlua.vm.KahluaTableIterator
 *  zombie.GameTime
 *  zombie.GameWindow
 *  zombie.Lua.LuaEventManager
 *  zombie.Lua.LuaManager
 *  zombie.Lua.LuaManager$GlobalObject
 *  zombie.SandboxOptions
 *  zombie.UsedFromLua
 *  zombie.ZomboidGlobals
 *  zombie.audio.BaseSoundEmitter
 *  zombie.audio.SoundInstanceLimiter
 *  zombie.audio.SoundLimiterParams
 *  zombie.characterTextures.BloodBodyPartType
 *  zombie.characterTextures.BloodClothingType
 *  zombie.characters.BaseCharacterSoundEmitter
 *  zombie.characters.IsoGameCharacter
 *  zombie.characters.IsoPlayer
 *  zombie.characters.IsoZombie
 *  zombie.characters.SurvivorDesc
 *  zombie.characters.UnderwearDefinition
 *  zombie.characters.ZombiesZoneDefinition
 *  zombie.characters.animals.AnimalDefinitions
 *  zombie.characters.animals.AnimalTracks
 *  zombie.characters.animals.IsoAnimal
 *  zombie.characters.animals.datas.AnimalBreed
 *  zombie.characters.animals.datas.AnimalBreed$Sound
 *  zombie.characters.skills.PerkFactory$Perks
 *  zombie.core.Color
 *  zombie.core.Colors
 *  zombie.core.Core
 *  zombie.core.ImmutableColor
 *  zombie.core.Translator
 *  zombie.core.logger.ExceptionLogger
 *  zombie.core.math.PZMath
 *  zombie.core.random.Rand
 *  zombie.core.skinnedmodel.model.WorldItemAtlas$ItemTexture
 *  zombie.core.skinnedmodel.population.ClothingItem
 *  zombie.core.skinnedmodel.population.Outfit
 *  zombie.core.skinnedmodel.visual.ItemVisual
 *  zombie.core.stash.StashSystem
 *  zombie.core.textures.ColorInfo
 *  zombie.core.textures.Texture
 *  zombie.core.utils.Bits
 *  zombie.debug.DebugLog
 *  zombie.debug.DebugOptions
 *  zombie.debug.DebugType
 *  zombie.debug.LogSeverity
 *  zombie.entity.Component
 *  zombie.entity.ComponentType
 *  zombie.entity.GameEntity
 *  zombie.entity.GameEntityFactory
 *  zombie.entity.GameEntityType
 *  zombie.entity.components.attributes.Attribute
 *  zombie.entity.components.attributes.AttributeType
 *  zombie.entity.components.fluids.Fluid
 *  zombie.entity.components.fluids.FluidContainer
 *  zombie.inventory.InventoryItemFactory
 *  zombie.inventory.ItemPickerJava
 *  zombie.inventory.ItemSoundManager
 *  zombie.inventory.ItemUser
 *  zombie.inventory.types.AnimalInventoryItem
 *  zombie.inventory.types.Clothing
 *  zombie.inventory.types.Drainable
 *  zombie.inventory.types.DrainableComboItem
 *  zombie.inventory.types.Food
 *  zombie.inventory.types.HandWeapon
 *  zombie.inventory.types.InventoryContainer
 *  zombie.inventory.types.Key
 *  zombie.inventory.types.WeaponPart
 *  zombie.inventory.types.WeaponType
 *  zombie.iso.IsoDirections
 *  zombie.iso.IsoWorld
 *  zombie.iso.objects.IsoDeadBody
 *  zombie.iso.objects.IsoFireManager
 *  zombie.iso.objects.IsoFireplace
 *  zombie.iso.objects.RainManager
 *  zombie.network.GameClient
 *  zombie.network.GameServer
 *  zombie.network.PacketTypes$PacketType
 *  zombie.network.packets.INetworkPacket
 *  zombie.radio.ZomboidRadio
 *  zombie.radio.media.MediaData
 *  zombie.scripting.ScriptManager
 *  zombie.scripting.objects.AmmoType
 *  zombie.scripting.objects.BookSubject
 *  zombie.scripting.objects.CoverType
 *  zombie.scripting.objects.Item
 *  zombie.scripting.objects.ItemBodyLocation
 *  zombie.scripting.objects.ItemKey
 *  zombie.scripting.objects.ItemKey$Normal
 *  zombie.scripting.objects.ItemReplacement
 *  zombie.scripting.objects.ItemTag
 *  zombie.scripting.objects.ItemType
 *  zombie.scripting.objects.MagazineSubject
 *  zombie.scripting.objects.ModelKey
 *  zombie.scripting.objects.SoundKey
 *  zombie.scripting.objects.SoundMapKey
 *  zombie.scripting.objects.WeaponCategory
 *  zombie.ui.ObjectTooltip
 *  zombie.ui.ObjectTooltip$Layout
 *  zombie.ui.ObjectTooltip$LayoutItem
 *  zombie.ui.TextManager
 *  zombie.ui.UIFont
 *  zombie.util.StringUtils
 *  zombie.util.Type
 *  zombie.util.io.BitHeader
 *  zombie.util.io.BitHeader$HeaderSize
 *  zombie.util.io.BitHeaderRead
 *  zombie.util.io.BitHeaderWrite
 *  zombie.util.list.PZArrayUtil
 *  zombie.vehicles.VehiclePart
 *  zombie.world.ItemInfo
 *  zombie.world.WorldDictionary
 */
package zombie.inventory;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.ZomboidGlobals;
import zombie.audio.BaseSoundEmitter;
import zombie.audio.SoundInstanceLimiter;
import zombie.audio.SoundLimiterParams;
import zombie.characterTextures.BloodBodyPartType;
import zombie.characterTextures.BloodClothingType;
import zombie.characters.BaseCharacterSoundEmitter;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.SurvivorDesc;
import zombie.characters.UnderwearDefinition;
import zombie.characters.ZombiesZoneDefinition;
import zombie.characters.animals.AnimalDefinitions;
import zombie.characters.animals.AnimalTracks;
import zombie.characters.animals.IsoAnimal;
import zombie.characters.animals.datas.AnimalBreed;
import zombie.characters.skills.PerkFactory;
import zombie.core.Color;
import zombie.core.Colors;
import zombie.core.Core;
import zombie.core.ImmutableColor;
import zombie.core.Translator;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.model.WorldItemAtlas;
import zombie.core.skinnedmodel.population.ClothingItem;
import zombie.core.skinnedmodel.population.Outfit;
import zombie.core.skinnedmodel.visual.ItemVisual;
import zombie.core.stash.StashSystem;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.core.utils.Bits;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;
import zombie.entity.Component;
import zombie.entity.ComponentType;
import zombie.entity.GameEntity;
import zombie.entity.GameEntityFactory;
import zombie.entity.GameEntityType;
import zombie.entity.components.attributes.Attribute;
import zombie.entity.components.attributes.AttributeType;
import zombie.entity.components.fluids.Fluid;
import zombie.entity.components.fluids.FluidContainer;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemContainer;
import zombie.inventory.ItemPickerJava;
import zombie.inventory.ItemSoundManager;
import zombie.inventory.ItemUser;
import zombie.inventory.types.AnimalInventoryItem;
import zombie.inventory.types.Clothing;
import zombie.inventory.types.Drainable;
import zombie.inventory.types.DrainableComboItem;
import zombie.inventory.types.Food;
import zombie.inventory.types.HandWeapon;
import zombie.inventory.types.InventoryContainer;
import zombie.inventory.types.Key;
import zombie.inventory.types.WeaponPart;
import zombie.inventory.types.WeaponType;
import zombie.iso.IsoCell;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoFireManager;
import zombie.iso.objects.IsoFireplace;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.iso.objects.RainManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;
import zombie.radio.ZomboidRadio;
import zombie.radio.media.MediaData;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.AmmoType;
import zombie.scripting.objects.BookSubject;
import zombie.scripting.objects.CoverType;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.ItemBodyLocation;
import zombie.scripting.objects.ItemKey;
import zombie.scripting.objects.ItemReplacement;
import zombie.scripting.objects.ItemTag;
import zombie.scripting.objects.ItemType;
import zombie.scripting.objects.MagazineSubject;
import zombie.scripting.objects.ModelKey;
import zombie.scripting.objects.SoundKey;
import zombie.scripting.objects.SoundMapKey;
import zombie.scripting.objects.WeaponCategory;
import zombie.ui.ObjectTooltip;
import zombie.ui.TextManager;
import zombie.ui.UIFont;
import zombie.util.StringUtils;
import zombie.util.Type;
import zombie.util.io.BitHeader;
import zombie.util.io.BitHeaderRead;
import zombie.util.io.BitHeaderWrite;
import zombie.util.list.PZArrayUtil;
import zombie.vehicles.VehiclePart;
import zombie.world.ItemInfo;
import zombie.world.WorldDictionary;

@UsedFromLua
public class InventoryItem
extends GameEntity {
    private static final ByteBuffer tempBuffer = ByteBuffer.allocate(20000);
    protected static final int DEFAULT_USES = 1;
    private static final long SERVER_MAX_UPDATE_DELTA_MS = 6000L;
    private static final EnumMap<IsoDirections, String> COMPASS_TOOLTIP = new EnumMap<IsoDirections, String>(Map.of(IsoDirections.N, "Tooltip_compass_9", IsoDirections.NW, "Tooltip_compass_8", IsoDirections.W, "Tooltip_compass_7", IsoDirections.SW, "Tooltip_compass_4", IsoDirections.S, "Tooltip_compass_1", IsoDirections.SE, "Tooltip_compass_2", IsoDirections.E, "Tooltip_compass_3", IsoDirections.NE, "Tooltip_compass_6"));
    protected IsoGameCharacter previousOwner;
    protected Item scriptItem;
    protected ItemType itemType;
    protected ItemContainer container;
    protected int containerX;
    protected int containerY;
    protected String name;
    protected String replaceOnUse;
    protected String replaceOnUseFullType;
    protected int conditionMax = 10;
    protected ItemContainer rightClickContainer;
    protected Texture texture;
    protected Texture texturerotten;
    protected Texture textureCooked;
    protected Texture textureBurnt;
    protected String type;
    protected String fullType;
    protected int uses = 1;
    protected float age;
    protected float lastAged = -1.0f;
    protected boolean isCookable;
    protected float cookingTime;
    protected float minutesToCook = 60.0f;
    protected float minutesToBurn = 120.0f;
    public boolean cooked;
    protected boolean burnt;
    protected int offAge = 1000000000;
    protected int offAgeMax = 1000000000;
    protected float weight = 1.0f;
    protected float actualWeight = 1.0f;
    protected String worldTexture;
    protected String description;
    protected int condition = 10;
    protected String offString = Translator.getText((String)"Tooltip_food_Rotten", (Object[])new Object[0]);
    protected String freshString = Translator.getText((String)"Tooltip_food_Fresh", (Object[])new Object[0]);
    protected String staleString = Translator.getText((String)"Tooltip_food_Stale", (Object[])new Object[0]);
    protected String cookedString = Translator.getText((String)"Tooltip_food_Cooked", (Object[])new Object[0]);
    protected String toastedString = Translator.getText((String)"Tooltip_food_Toasted", (Object[])new Object[0]);
    protected String grilledString = Translator.getText((String)"Tooltip_food_Grilled", (Object[])new Object[0]);
    protected String unCookedString = Translator.getText((String)"Tooltip_food_Uncooked", (Object[])new Object[0]);
    protected String frozenString = Translator.getText((String)"Tooltip_food_Frozen", (Object[])new Object[0]);
    protected String burntString = Translator.getText((String)"Tooltip_food_Burnt", (Object[])new Object[0]);
    protected String emptyString = Translator.getText((String)"ContextMenu_Empty", (Object[])new Object[0]);
    protected String module;
    protected float boredomChange;
    protected float unhappyChange;
    protected float stressChange;
    protected int foodSicknessChange;
    protected int inverseCoughProbability;
    protected int inverseCoughProbabilitySmoker;
    protected ArrayList<IsoObject> taken = new ArrayList();
    private KahluaTable table;
    public String replaceOnUseOn;
    public Color col = Color.white;
    public boolean canStack;
    private boolean activated;
    private boolean isTorchCone;
    private int lightDistance;
    private int count = 1;
    public float fatigueChange;
    public IsoWorldInventoryObject worldItem;
    public IsoDeadBody deadBodyObject;
    private String customMenuOption;
    private String tooltip;
    private String displayCategory;
    private int haveBeenRepaired;
    private boolean broken;
    private final String originalName;
    public int id;
    public boolean requiresEquippedBothHands;
    public ByteBuffer byteData;
    public ArrayList<String> extraItems = new ArrayList();
    private boolean customName;
    private String breakSound;
    protected boolean alcoholic;
    private float alcoholPower;
    private float bandagePower;
    private float reduceInfectionPower;
    private boolean customWeight;
    private boolean customColor;
    private int keyId = -1;
    private boolean remoteController;
    private boolean canBeRemote;
    private int remoteControlId = -1;
    private int remoteRange;
    private float colorRed = 1.0f;
    private float colorGreen = 1.0f;
    private float colorBlue = 1.0f;
    private String countDownSound;
    private String explosionSound;
    private IsoGameCharacter equipParent;
    private String evolvedRecipeName;
    private float metalValue;
    private float itemHeat = 1.0f;
    private float meltingTime;
    private String worker;
    private boolean isWet;
    private float wetCooldown = -1.0f;
    private String itemWhenDry;
    private boolean favorite;
    protected ArrayList<String> requireInHandOrInventory;
    private String stashMap;
    private boolean zombieInfected;
    private float itemCapacity = -1.0f;
    private int maxCapacity = -1;
    private float brakeForce;
    private float durability;
    private int chanceToSpawnDamaged;
    private float conditionLowerNormal;
    private float conditionLowerOffroad;
    private float wheelFriction;
    private float suspensionDamping;
    private float suspensionCompression;
    private float engineLoudness;
    protected ItemVisual visual;
    protected String staticModel;
    private ArrayList<String> iconsForTexture;
    private ArrayList<BloodClothingType> bloodClothingType = new ArrayList();
    private int stashChance = 80;
    private AmmoType ammoType;
    private int maxAmmo;
    private int currentAmmoCount;
    private ArrayList<String> gunType;
    private final List<String> gunTypeDisplayName = new ArrayList<String>();
    private String attachmentType;
    private ArrayList<String> attachmentsProvided;
    private int attachedSlot = -1;
    private String attachedSlotType;
    private String attachmentReplacement;
    private String attachedToModel;
    private final String alternateModelName;
    private short registryId = (short)-1;
    public float worldScale = 1.0f;
    public float worldXRotation;
    public float worldYRotation;
    public float worldZRotation = -1.0f;
    public float worldAlpha = 1.0f;
    private short recordedMediaIndex = (short)-1;
    private byte mediaType = (byte)-1;
    private boolean isInitialised;
    public WorldItemAtlas.ItemTexture atlasTexture;
    protected Texture textureColorMask;
    protected Texture textureFluidMask;
    private AnimalTracks animalTracks;
    private ArrayList<String> staticModelsByIndex;
    private ArrayList<String> worldStaticModelsByIndex;
    private boolean doingExtendedPlacement;
    private int modelIndex = -1;
    private final int maxTextLength = 256;
    private IsoPlayer equippedAndActivatedPlayer;
    private long equippedAndActivatedSound;
    private boolean isCraftingConsumed;
    public float jobDelta;
    public String jobType;
    public String mainCategory;
    private boolean canBeActivated;
    private float lightStrength;
    public String closeKillMove;
    private float useDelta = 0.03125f;
    private long lastUpdateMs = -1L;
    protected float timeMultiplier;
    private boolean beingFilled;

    public CoverType getCoverType() {
        boolean hasSoftcover;
        boolean hasHardcover = this.hasTag(ItemTag.HARDCOVER, ItemTag.HOLLOW_BOOK, ItemTag.FANCY_BOOK);
        if (hasHardcover != (hasSoftcover = this.hasTag(ItemTag.SOFTCOVER))) {
            return hasHardcover ? CoverType.HARDCOVER : CoverType.SOFTCOVER;
        }
        return CoverType.BOTH;
    }

    public List<BookSubject> getBookSubjects() {
        return this.getScriptItem().bookSubjects;
    }

    public List<MagazineSubject> getMagazineSubjects() {
        return this.getScriptItem().magazineSubjects;
    }

    public IsoWorldInventoryObject getWorldItem() {
        return this.worldItem;
    }

    public boolean hasWorldItem() {
        return this.getWorldItem() != null;
    }

    public boolean isOnGroundOnSquare(IsoGridSquare square) {
        return square != null && this.hasWorldItem() && this.getWorldItem().getSquare() == square;
    }

    public boolean isInsideBagOnSquare(IsoGridSquare square) {
        return square != null && this.getOutermostContainer() != null && this.getOutermostContainer().hasWorldItem() && this.getOutermostContainer().getWorldItem().getSquare() == square;
    }

    public boolean isOnGroundOrInsideBagOnSquare(IsoGridSquare square) {
        return this.isOnGroundOnSquare(square) || this.isInsideBagOnSquare(square);
    }

    public void setEquipParent(IsoGameCharacter parent) {
        this.setEquipParent(parent, true);
    }

    public void setEquipParent(IsoGameCharacter parent, boolean register) {
        this.equipParent = parent;
        if (this.equipParent == null) {
            this.onUnEquip();
        } else {
            this.onEquip(register);
        }
    }

    public IsoGameCharacter getEquipParent() {
        if (this.equipParent != null && (this.equipParent.getPrimaryHandItem() == this || this.equipParent.getSecondaryHandItem() == this)) {
            return this.equipParent;
        }
        return null;
    }

    public String getBringToBearSound() {
        return this.scriptItem.getBringToBearSound();
    }

    public String getAimReleaseSound() {
        return this.scriptItem.getAimReleaseSound();
    }

    public String getEquipSound() {
        return this.scriptItem.getEquipSound();
    }

    public String getUnequipSound() {
        return this.scriptItem.getUnequipSound();
    }

    public String getDropSound() {
        if (StringUtils.equalsIgnoreCase((String)this.getType(), (String)"CorpseAnimal")) {
            IsoDeadBody corpse = this.loadCorpseFromByteData(null);
            if (corpse == null || !corpse.isAnimal()) {
                return this.scriptItem.getDropSound();
            }
            AnimalDefinitions def = AnimalDefinitions.getDef((String)corpse.getAnimalType());
            if (def == null) {
                return this.scriptItem.getDropSound();
            }
            AnimalBreed breed = def.getBreedByName(corpse.getBreed());
            if (breed == null) {
                return this.scriptItem.getDropSound();
            }
            AnimalBreed.Sound sound = breed.getSound("put_down_corpse");
            if (sound == null) {
                return this.scriptItem.getDropSound();
            }
            return sound.soundName;
        }
        return this.scriptItem.getDropSound();
    }

    public void setWorldItem(IsoWorldInventoryObject w) {
        this.worldItem = w;
    }

    public void setJobDelta(float delta) {
        this.jobDelta = delta;
    }

    public float getJobDelta() {
        return this.jobDelta;
    }

    public void setJobType(String type) {
        this.jobType = type;
    }

    public String getJobType() {
        return this.jobType;
    }

    public boolean hasModData() {
        return this.table != null && !this.table.isEmpty();
    }

    public KahluaTable getModData() {
        if (this.table == null) {
            this.table = LuaManager.platform.newTable();
        }
        return this.table;
    }

    public void storeInByteData(IsoObject o) {
        tempBuffer.clear();
        try {
            o.save(tempBuffer, false);
        }
        catch (IOException e) {
            DebugType.General.printException((Throwable)e, LogSeverity.Error);
        }
        tempBuffer.flip();
        if (this.byteData == null || this.byteData.capacity() < tempBuffer.limit() - 2 + 8) {
            this.byteData = ByteBuffer.allocate(tempBuffer.limit() - 2 + 8);
        }
        tempBuffer.get();
        tempBuffer.get();
        this.byteData.clear();
        this.byteData.put((byte)87);
        this.byteData.put((byte)86);
        this.byteData.put((byte)69);
        this.byteData.put((byte)82);
        this.byteData.putInt(249);
        this.byteData.put(tempBuffer);
        this.byteData.flip();
    }

    public ByteBuffer getByteData() {
        return this.byteData;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public IsoDeadBody loadCorpseFromByteData(IsoGridSquare square) {
        if (this.getByteData() == null) {
            return this.createAndStoreDefaultDeadBody(square);
        }
        try {
            IsoDeadBody isoDeadBody = this.tryLoadCorpseFromByteData(square);
            return isoDeadBody;
        }
        catch (IOException ex) {
            ExceptionLogger.logException((Throwable)ex);
            try {
                IsoDeadBody isoDeadBody = this.createDefaultDeadBody(square);
                return isoDeadBody;
            }
            catch (Throwable t) {
                ExceptionLogger.logException((Throwable)t);
                IsoDeadBody isoDeadBody = null;
                return isoDeadBody;
            }
        }
        finally {
            this.getByteData().rewind();
        }
    }

    private IsoDeadBody tryLoadCorpseFromByteData(IsoGridSquare square) throws IOException {
        this.getByteData().rewind();
        byte b1 = this.getByteData().get();
        byte b2 = this.getByteData().get();
        byte b3 = this.getByteData().get();
        byte b4 = this.getByteData().get();
        if (b1 != 87 || b2 != 86 || b3 != 69 || b4 != 82) {
            throw new IOException("expected 'WVER' signature in byteData");
        }
        int worldVersion = this.getByteData().getInt();
        IsoDeadBody corpse = new IsoDeadBody(IsoWorld.instance.currentCell);
        corpse.load(this.getByteData(), worldVersion);
        if ("CorpseAnimal".equalsIgnoreCase(this.getType())) {
            Object skeleton;
            Object object = skeleton = this.hasModData() ? this.getModData().rawget((Object)"skeleton") : null;
            if (skeleton != null && "true".equalsIgnoreCase(skeleton.toString())) {
                corpse.getModData().rawset((Object)"skeleton", (Object)"true");
            }
            double ageHours = (double)this.getAge() * 24.0;
            Object object2 = corpse.getModData().rawget((Object)"deathAge");
            if (object2 instanceof Double) {
                Double deathAge = (Double)object2;
                ageHours += deathAge.doubleValue();
                ageHours -= (double)PZMath.min((float)((float)ageHours), (float)(corpse.getInitialItemAge(this) * 24.0f));
            }
            corpse.setDeathTime((float)(GameTime.getInstance().getWorldAgeHours() - ageHours));
            corpse.getModData().rawset((Object)"deathAge", (Object)ageHours);
        }
        if (square != null) {
            corpse.setSquare(square);
            corpse.setCurrent(square);
        }
        return corpse;
    }

    public boolean isForceDropHeavyItem() {
        return this.isHumanCorpse() || "Generator".equalsIgnoreCase(this.getType()) || this.hasTag(ItemTag.HEAVY_ITEM) || "Animal".equalsIgnoreCase(this.getType()) || "CorpseAnimal".equalsIgnoreCase(this.getType());
    }

    public boolean isHumanCorpse() {
        String itemType = this.getType();
        if ("CorpseFemale".equalsIgnoreCase(itemType)) {
            return true;
        }
        return "CorpseMale".equalsIgnoreCase(itemType);
    }

    public boolean isAnimalCorpse() {
        String itemType = this.getType();
        return "CorpseAnimal".equalsIgnoreCase(itemType);
    }

    private IsoDeadBody createDefaultDeadBody(IsoGridSquare square) throws Throwable {
        if (this.isHumanCorpse()) {
            IsoZombie zombie = new IsoZombie(IsoWorld.instance.currentCell);
            zombie.setDir(IsoDirections.getRandom());
            zombie.setForwardDirection(zombie.getForwardIsoDirection().ToVector());
            zombie.setFakeDead(false);
            zombie.setHealth(0.0f);
            if (square != null) {
                zombie.dressInRandomOutfit();
            } else if (!zombie.isSkeleton()) {
                String roomName = null;
                Outfit outfit = ZombiesZoneDefinition.getRandomDefaultOutfit((boolean)zombie.isFemale(), roomName);
                UnderwearDefinition.addRandomUnderwear((IsoZombie)zombie);
                zombie.dressInPersistentOutfit(outfit.name);
            }
            zombie.DoZombieInventory();
            if (square != null) {
                zombie.setSquare(square);
                zombie.setCurrent(square);
            }
            return new IsoDeadBody((IsoGameCharacter)zombie, true, square != null);
        }
        if (this.isAnimalCorpse()) {
            AnimalDefinitions def = (AnimalDefinitions)PZArrayUtil.pickRandom((List)AnimalDefinitions.getAnimalDefsArray());
            if (def == null) {
                return null;
            }
            AnimalBreed breed = def.getRandomBreed();
            if (breed == null) {
                return null;
            }
            IsoAnimal animal = new IsoAnimal(IsoWorld.instance.currentCell, 0, 0, 0, def.getAnimalType(), breed.getName());
            animal.setDir(IsoDirections.getRandom());
            animal.setForwardDirection(animal.getDir().ToVector());
            animal.setHealth(0.0f);
            if (square != null) {
                animal.setSquare(square);
                animal.setCurrent(square);
            }
            IsoDeadBody corpse = new IsoDeadBody((IsoGameCharacter)animal, true, square != null);
            this.copyModData(corpse.getModData());
            this.setIcon(Texture.getSharedTexture((String)corpse.invIcon));
            if (corpse.isAnimalSkeleton()) {
                this.setName(Translator.getText((String)"IGUI_Item_AnimalSkeleton", (Object[])new Object[]{corpse.customName}));
            } else {
                this.setName(Translator.getText((String)"IGUI_Item_AnimalCorpse", (Object[])new Object[]{corpse.customName}));
            }
            this.setCustomName(true);
            this.setActualWeight(corpse.weight);
            this.setWeight(corpse.weight);
            this.setCustomWeight(true);
            return corpse;
        }
        return null;
    }

    public IsoDeadBody createAndStoreDefaultDeadBody(IsoGridSquare square) {
        try {
            IsoDeadBody corpse = this.createDefaultDeadBody(square);
            if (corpse != null) {
                this.storeInByteData((IsoObject)corpse);
            }
            return corpse;
        }
        catch (Throwable t) {
            ExceptionLogger.logException((Throwable)t);
            return null;
        }
    }

    public boolean isRequiresEquippedBothHands() {
        return this.requiresEquippedBothHands;
    }

    public float getA() {
        return this.col.a;
    }

    public float getR() {
        return this.col.r;
    }

    public float getG() {
        return this.col.g;
    }

    public float getB() {
        return this.col.b;
    }

    public InventoryItem(String module, String name, String type, String tex) {
        this.alternateModelName = null;
        this.texture = Texture.trygetTexture((String)tex);
        if (this.texture == null) {
            this.texture = Texture.getSharedTexture((String)"media/inventory/Question_On.png");
        }
        this.module = module;
        this.name = name;
        this.originalName = name;
        this.type = type;
        this.fullType = module + "." + type;
        this.worldTexture = tex.replace("Item_", "media/inventory/world/WItem_");
        this.worldTexture = this.worldTexture + ".png";
    }

    public InventoryItem(String module, String name, String type, Item item) {
        this.alternateModelName = null;
        this.texture = item.normalTexture;
        this.module = module;
        this.name = name;
        this.originalName = name;
        this.type = type;
        this.fullType = module + "." + type;
        this.worldTexture = item.worldTextureName;
    }

    public String getType() {
        return this.type;
    }

    public Texture getTex() {
        return this.getTexture();
    }

    public String getCategory() {
        if (this.mainCategory != null) {
            return this.mainCategory;
        }
        return "Item";
    }

    public boolean UseForCrafting(int uses) {
        return false;
    }

    public boolean IsRotten() {
        return this.age > (float)this.offAge;
    }

    public float HowRotten() {
        if (this.offAgeMax - this.offAge == 0) {
            return this.age > (float)this.offAge ? 1.0f : 0.0f;
        }
        return (this.age - (float)this.offAge) / (float)(this.offAgeMax - this.offAge);
    }

    public boolean CanStack(InventoryItem item) {
        return false;
    }

    public boolean ModDataMatches(InventoryItem item) {
        KahluaTable t = item.getModData();
        KahluaTable t2 = item.getModData();
        if (t == null && t2 == null) {
            return true;
        }
        if (t == null) {
            return false;
        }
        if (t2 == null) {
            return false;
        }
        if (t.len() != t2.len()) {
            return false;
        }
        KahluaTableIterator it = t.iterator();
        while (it.advance()) {
            Object b;
            Object a = t2.rawget(it.getKey());
            if (a.equals(b = it.getValue())) continue;
            return false;
        }
        return true;
    }

    public void DoTooltip(ObjectTooltip tooltipUI) {
        this.DoTooltipEmbedded(tooltipUI, null, 0);
    }

    public void DoTooltipEmbedded(ObjectTooltip tooltipUI, ObjectTooltip.Layout layoutOverride, int offsetY) {
        ObjectTooltip.Layout layout;
        float dx;
        InventoryItem item;
        int i;
        int dy;
        int x;
        int lineSpacing;
        tooltipUI.render();
        UIFont font = tooltipUI.getFont();
        int iconSize = lineSpacing = tooltipUI.getLineSpacing();
        int y = tooltipUI.padTop + offsetY;
        IsoPlayer player = (IsoPlayer)Type.tryCastTo((Object)tooltipUI.getCharacter(), IsoPlayer.class);
        String s = player != null ? this.getName(player) : this.getName();
        tooltipUI.DrawText(font, s, (double)tooltipUI.padLeft, (double)y, 1.0, 1.0, (double)0.8f, 1.0);
        tooltipUI.adjustWidth(tooltipUI.padLeft, s);
        ColorInfo highlightGood = Core.getInstance().getGoodHighlitedColor();
        ColorInfo highlightBad = Core.getInstance().getBadHighlitedColor();
        float goodR = highlightGood.getR();
        float goodG = highlightGood.getG();
        float goodB = highlightGood.getB();
        float badR = highlightBad.getR();
        float badG = highlightBad.getG();
        float badB = highlightBad.getB();
        y += lineSpacing + 5;
        if (this.extraItems != null && !this.extraItems.isEmpty()) {
            tooltipUI.DrawText(font, Translator.getText((String)"Tooltip_item_Contains", (Object[])new Object[0]), (double)tooltipUI.padLeft, (double)y, 1.0, 1.0, (double)0.8f, 1.0);
            x = tooltipUI.padLeft + TextManager.instance.MeasureStringX(font, Translator.getText((String)"Tooltip_item_Contains", (Object[])new Object[0])) + 4;
            dy = (lineSpacing - iconSize) / 2;
            for (i = 0; i < this.extraItems.size(); ++i) {
                item = InventoryItemFactory.CreateItem((String)this.extraItems.get(i));
                if (!this.isCookable && item.isCookable) {
                    item.setCooked(true);
                }
                if (this.isCooked() && item.isCookable) {
                    item.setCooked(true);
                }
                dx = this.drawTooltipItemTexture(tooltipUI, item.getTex(), x, y + dy, iconSize, iconSize, 1.0f, 1.0f, 1.0f, 1.0f);
                x = x + (int)PZMath.ceil((float)dx) + 2;
            }
            y = y + lineSpacing + 5;
        }
        if (this instanceof Food && ((Food)this).spices != null) {
            tooltipUI.DrawText(font, Translator.getText((String)"Tooltip_item_Spices", (Object[])new Object[0]), (double)tooltipUI.padLeft, (double)y, 1.0, 1.0, (double)0.8f, 1.0);
            x = tooltipUI.padLeft + TextManager.instance.MeasureStringX(font, Translator.getText((String)"Tooltip_item_Spices", (Object[])new Object[0])) + 4;
            dy = (lineSpacing - iconSize) / 2;
            for (i = 0; i < ((Food)this).spices.size(); ++i) {
                item = InventoryItemFactory.CreateItem((String)((String)((Food)this).spices.get(i)));
                dx = this.drawTooltipItemTexture(tooltipUI, item.getTex(), x, y + dy, iconSize, iconSize, 1.0f, 1.0f, 1.0f, 1.0f);
                x = x + (int)PZMath.ceil((float)dx) + 2;
            }
            y = y + lineSpacing + 5;
        }
        if (layoutOverride != null) {
            layout = layoutOverride;
            layout.offsetY = y;
        } else {
            layout = tooltipUI.beginLayout();
            layout.setMinLabelWidth(80);
            layout.setMinValueWidth(80);
        }
        if (SandboxOptions.instance.isUnstableScriptNameSpam()) {
            ObjectTooltip.LayoutItem item2 = layout.addItem();
            item2.setLabel(Translator.getText((String)"Item Report", (Object[])new Object[0]) + ":", 1.0f, 0.4f, 0.7f, 1.0f);
            item2.setValue(this.getFullType(), 1.0f, 1.0f, 0.8f, 1.0f);
        }
        if (player != null && this.scriptItem != null && !this.scriptItem.getResearchableRecipes((IsoGameCharacter)player).isEmpty()) {
            ColorInfo color = Core.getInstance().getGoodHighlitedColor();
            ObjectTooltip.LayoutItem item3 = layout.addItem();
            if (this.scriptItem.getResearchableRecipes((IsoGameCharacter)player).size() == 1) {
                item3.setLabel(Translator.getText((String)"Tooltip_item_CanResearch", (Object[])new Object[0]), color.getR(), color.getG(), color.getB(), 1.0f);
            } else {
                item3.setLabel(Translator.getText((String)"Tooltip_item_CanResearchPlural", (Object[])new Object[0]), color.getR(), color.getG(), color.getB(), 1.0f);
            }
        }
        if (player != null && this.scriptItem != null && this.scriptItem.isFavouriteRecipeInput(player)) {
            ColorInfo color = Core.getInstance().getGoodHighlitedColor();
            ObjectTooltip.LayoutItem item4 = layout.addItem();
            item4.setLabel(Translator.getText((String)"Tooltip_item_IsFavouriteInput", (Object[])new Object[0]), color.getR(), color.getG(), color.getB(), 1.0f);
        }
        if (player != null && this.isNoRecipes(player)) {
            ColorInfo color = Core.getInstance().getBadHighlitedColor();
            ObjectTooltip.LayoutItem item5 = layout.addItem();
            item5.setLabel(Translator.getText((String)"Tooltip_NoRecipes_More", (Object[])new Object[0]), color.getR(), color.getG(), color.getB(), 1.0f);
        }
        if (player != null && this.isUnwanted(player)) {
            ColorInfo color = Core.getInstance().getBadHighlitedColor();
            ObjectTooltip.LayoutItem item6 = layout.addItem();
            item6.setLabel(Translator.getText((String)"Tooltip_Unwanted_More", (Object[])new Object[0]), color.getR(), color.getG(), color.getB(), 1.0f);
        }
        ObjectTooltip.LayoutItem item7 = layout.addItem();
        item7.setLabel(Translator.getText((String)"Tooltip_item_Weight", (Object[])new Object[0]) + ":", 1.0f, 1.0f, 0.8f, 1.0f);
        boolean isEquipped = this.isEquipped();
        if (this instanceof HandWeapon || this instanceof Clothing || this instanceof DrainableComboItem || this.getFullType().contains("Walkie") || this.isKeyRing()) {
            if (isEquipped || this.isFakeEquipped()) {
                item7.setValue(this.getCleanString(this.getEquippedWeight()) + "    (" + this.getCleanString(this.getUnequippedWeight()) + " " + Translator.getText((String)"Tooltip_item_Unequipped", (Object[])new Object[0]) + ")", 1.0f, 1.0f, 1.0f, 1.0f);
            } else if (this.getAttachedSlot() > -1) {
                item7.setValue(this.getCleanString(this.getHotbarEquippedWeight()) + "    (" + this.getCleanString(this.getUnequippedWeight()) + " " + Translator.getText((String)"Tooltip_item_Unattached", (Object[])new Object[0]) + ")", 1.0f, 1.0f, 1.0f, 1.0f);
            } else {
                item7.setValue(this.getCleanString(this.getUnequippedWeight()) + "    (" + this.getCleanString(this.getEquippedWeight()) + " " + Translator.getText((String)"Tooltip_item_Equipped", (Object[])new Object[0]) + ")", 1.0f, 1.0f, 1.0f, 1.0f);
            }
        } else if (this instanceof AnimalInventoryItem) {
            item7.setValueRightNoPlus(this.getWeight());
        } else {
            float weight = this.getUnequippedWeight();
            if (weight > 0.0f && weight < 0.01f) {
                weight = 0.01f;
            }
            if (this.getAttachedSlot() > -1) {
                item7.setValue(this.getCleanString(this.getHotbarEquippedWeight()) + "    (" + this.getCleanString(this.getUnequippedWeight()) + " " + Translator.getText((String)"Tooltip_item_Unattached", (Object[])new Object[0]) + ")", 1.0f, 1.0f, 1.0f, 1.0f);
            } else {
                item7.setValueRightNoPlus(weight);
            }
        }
        if (tooltipUI.getWeightOfStack() > 0.0f) {
            item7 = layout.addItem();
            item7.setLabel(Translator.getText((String)"Tooltip_item_StackWeight", (Object[])new Object[0]) + ":", 1.0f, 1.0f, 0.8f, 1.0f);
            float weight = tooltipUI.getWeightOfStack();
            if (weight > 0.0f && weight < 0.01f) {
                weight = 0.01f;
            }
            item7.setValueRightNoPlus(weight);
        }
        if (this.getMaxAmmo() > 0 && !(this instanceof HandWeapon)) {
            item7 = layout.addItem();
            item7.setLabel(Translator.getText((String)"Tooltip_weapon_AmmoCount", (Object[])new Object[0]) + ":", 1.0f, 1.0f, 0.8f, 1.0f);
            item7.setValue(this.getCurrentAmmoCount() + " / " + this.getMaxAmmo(), 1.0f, 1.0f, 1.0f, 1.0f);
        }
        if (!(this instanceof HandWeapon) && this.getAmmoType() != null) {
            item7 = layout.addItem();
            item7.setLabel(Translator.getText((String)"ContextMenu_AmmoType", (Object[])new Object[0]) + ":", 1.0f, 1.0f, 0.8f, 1.0f);
            InventoryItem inventoryItem = InventoryItemFactory.CreateItem((AmmoType)this.getAmmoType());
            String displayName = inventoryItem.getDisplayName();
            item7.setValue(Translator.getText((String)displayName, (Object[])new Object[0]), 1.0f, 1.0f, 1.0f, 1.0f);
        }
        if (this.gunType != null) {
            item7 = layout.addItem();
            item7.setLabel(Translator.getText((String)"ContextMenu_GunType", (Object[])new Object[0]) + ":", 1.0f, 1.0f, 0.8f, 1.0f);
            item7.setValue(this.getGunTypeString(), 1.0f, 1.0f, 1.0f, 1.0f);
        }
        if (Core.debug && DebugOptions.instance.tooltipInfo.getValue()) {
            item7 = layout.addItem();
            item7.setLabel("getActualWeight()", 1.0f, 1.0f, 0.8f, 1.0f);
            item7.setValueRightNoPlus(this.getActualWeight());
            item7 = layout.addItem();
            item7.setLabel("getWeight()", 1.0f, 1.0f, 0.8f, 1.0f);
            item7.setValueRightNoPlus(this.getWeight());
            item7 = layout.addItem();
            item7.setLabel("getEquippedWeight()", 1.0f, 1.0f, 0.8f, 1.0f);
            item7.setValueRightNoPlus(this.getEquippedWeight());
            item7 = layout.addItem();
            item7.setLabel("getUnequippedWeight()", 1.0f, 1.0f, 0.8f, 1.0f);
            item7.setValueRightNoPlus(this.getUnequippedWeight());
            item7 = layout.addItem();
            item7.setLabel("getContentsWeight()", 1.0f, 1.0f, 0.8f, 1.0f);
            item7.setValueRightNoPlus(this.getContentsWeight());
            if (this instanceof Key || "Doorknob".equals(this.type)) {
                item7 = layout.addItem();
                item7.setLabel("DBG: keyId", 1.0f, 1.0f, 0.8f, 1.0f);
                item7.setValueRightNoPlus(this.getKeyId());
            }
            item7 = layout.addItem();
            item7.setLabel("ID", 1.0f, 1.0f, 0.8f, 1.0f);
            item7.setValueRightNoPlus(this.id);
            item7 = layout.addItem();
            item7.setLabel("DictionaryID", 1.0f, 1.0f, 0.8f, 1.0f);
            item7.setValueRightNoPlus((int)this.getRegistry_id());
            ClothingItem clothingItem = this.getClothingItem();
            if (clothingItem != null) {
                item7 = layout.addItem();
                item7.setLabel("ClothingItem", 1.0f, 1.0f, 1.0f, 1.0f);
                item7.setValue(this.getClothingItem().mame, 1.0f, 1.0f, 1.0f, 1.0f);
            }
        }
        if (Core.debug && DebugOptions.instance.tooltipInfo.getValue() || LuaManager.GlobalObject.isAdmin()) {
            item7 = layout.addItem();
            String label = "Loot Category";
            String category = Translator.getText((String)("Sandbox_" + this.getLootType() + "LootNew"), (Object[])new Object[0]);
            item7.setLabel("Loot Category:", 1.0f, 1.0f, 0.8f, 1.0f);
            item7.setValue(category, 1.0f, 1.0f, 1.0f, 1.0f);
        }
        if (this instanceof DrainableComboItem && !this.hasTag(ItemTag.HIDE_REMAINING)) {
            item7 = layout.addItem();
            item7.setLabel(Translator.getText((String)"IGUI_invpanel_Remaining", (Object[])new Object[0]) + ": ", 1.0f, 1.0f, 0.8f, 1.0f);
            float f = this.getCurrentUsesFloat();
            ColorInfo g2Bgrad = new ColorInfo();
            Core.getInstance().getBadHighlitedColor().interp(Core.getInstance().getGoodHighlitedColor(), f, g2Bgrad);
            item7.setProgress(f, g2Bgrad.getR(), g2Bgrad.getG(), g2Bgrad.getB(), 1.0f);
        }
        if (this instanceof Food && ((Food)this).isTainted() && SandboxOptions.instance.enableTaintedWaterText.getValue()) {
            item7 = layout.addItem();
            if (!this.hasMetal()) {
                item7.setLabel(Translator.getText((String)"Tooltip_item_TaintedWater", (Object[])new Object[0]), 1.0f, 0.5f, 0.5f, 1.0f);
            } else {
                item7.setLabel(Translator.getText((String)"Tooltip_item_TaintedWater_Plastic", (Object[])new Object[0]), 1.0f, 0.5f, 0.5f, 1.0f);
            }
        }
        if (!this.scriptItem.getForageFocusCategories().isEmpty()) {
            for (String categoryName : this.scriptItem.getForageFocusCategories()) {
                item7 = layout.addItem();
                item7.setLabel(Translator.getText((String)"UI_search_mode_item_focus", (Object[])new Object[0]) + ":", 1.0f, 1.0f, 0.8f, 1.0f);
                item7.setValue(Translator.getText((String)("IGUI_SearchMode_Categories_" + categoryName), (Object[])new Object[0]), 1.0f, 1.0f, 1.0f, 1.0f);
            }
        }
        if (this.getFatigueChange() != 0.0f) {
            item7 = layout.addItem();
            item7.setLabel(Translator.getText((String)"Tooltip_item_Fatigue", (Object[])new Object[0]) + ": ", 1.0f, 1.0f, 0.8f, 1.0f);
            if (this.getFatigueChange() < 0.0f) {
                item7.setProgress(this.getFatigueChange() * -1.0f, goodR, goodG, goodB, 1.0f);
            } else {
                item7.setProgress(this.getFatigueChange(), badR, badG, badB, 1.0f);
            }
        }
        this.DoTooltip(tooltipUI, layout);
        if (this.getRemoteControlID() != -1) {
            item7 = layout.addItem();
            item7.setLabel(Translator.getText((String)"Tooltip_TrapControllerID", (Object[])new Object[0]), 1.0f, 1.0f, 0.8f, 1.0f);
            item7.setValue(Integer.toString(this.getRemoteControlID()), 1.0f, 1.0f, 0.8f, 1.0f);
        }
        if (this.getHaveBeenRepaired() > 0) {
            item7 = layout.addItem();
            item7.setLabel(Translator.getText((String)"Tooltip_weapon_Repaired", (Object[])new Object[0]) + ":", 1.0f, 1.0f, 0.8f, 1.0f);
            if (this.hasTimesHeadRepaired()) {
                item7.setLabel(Translator.getText((String)"Tooltip_handle_Repaired", (Object[])new Object[0]) + ":", 1.0f, 1.0f, 0.8f, 1.0f);
            }
            item7.setValue(this.getHaveBeenRepaired() + "x", 1.0f, 1.0f, 1.0f, 1.0f);
        }
        if (this.hasTimesHeadRepaired() && this.getTimesHeadRepaired() > 0) {
            item7 = layout.addItem();
            item7.setLabel(Translator.getText((String)"Tooltip_head_Repaired", (Object[])new Object[0]) + ":", 1.0f, 1.0f, 0.8f, 1.0f);
            item7.setValue(this.getTimesHeadRepaired() + "x", 1.0f, 1.0f, 1.0f, 1.0f);
        }
        if (this.isEquippedNoSprint()) {
            item7 = layout.addItem();
            item7.setLabel(Translator.getText((String)"Tooltip_CantSprintEquipped", (Object[])new Object[0]), 1.0f, 0.1f, 0.1f, 1.0f);
        }
        if (this.isWet()) {
            item7 = layout.addItem();
            item7.setLabel(Translator.getText((String)"Tooltip_Wetness", (Object[])new Object[0]) + ": ", 1.0f, 1.0f, 0.8f, 1.0f);
            float f = this.getWetCooldown() / 10000.0f;
            ColorInfo b2Ggrad = new ColorInfo();
            Core.getInstance().getGoodHighlitedColor().interp(Core.getInstance().getBadHighlitedColor(), f, b2Ggrad);
            item7.setProgress(f, b2Ggrad.getR(), b2Ggrad.getG(), b2Ggrad.getB(), 1.0f);
        }
        if (this.getMaxCapacity() > 0) {
            item7 = layout.addItem();
            item7.setLabel(Translator.getText((String)"Tooltip_container_Capacity", (Object[])new Object[0]) + ":", 1.0f, 1.0f, 0.8f, 1.0f);
            float capacity = this.getMaxCapacity();
            if (this.isConditionAffectsCapacity()) {
                capacity = VehiclePart.getNumberByCondition((float)this.getMaxCapacity(), (float)this.getCondition(), (float)5.0f);
            }
            if (this.getItemCapacity() > -1.0f) {
                item7.setValue(this.getItemCapacity() + " / " + capacity, 1.0f, 1.0f, 0.8f, 1.0f);
            } else {
                item7.setValue("0 / " + capacity, 1.0f, 1.0f, 0.8f, 1.0f);
            }
        }
        if (!(this instanceof HandWeapon) && this.hasSharpness()) {
            float tr = 1.0f;
            float tg = 1.0f;
            float tb = 0.8f;
            float ta = 1.0f;
            ColorInfo g2Bgrad = new ColorInfo();
            item7 = layout.addItem();
            item7.setLabel(Translator.getText((String)"Tooltip_weapon_Sharpness", (Object[])new Object[0]) + ":", 1.0f, 1.0f, 0.8f, 1.0f);
            float f = this.getSharpness();
            Core.getInstance().getBadHighlitedColor().interp(Core.getInstance().getGoodHighlitedColor(), f, g2Bgrad);
            item7.setProgress(f, g2Bgrad.getR(), g2Bgrad.getG(), g2Bgrad.getB(), 1.0f);
        }
        if (!(this instanceof HandWeapon || this instanceof Clothing || this.getConditionMax() <= 0 || this.getMechanicType() <= 0 && !this.hasTag(ItemTag.SHOW_CONDITION) && this.getConditionMax() <= this.getCondition())) {
            ColorInfo g2Bgrad = new ColorInfo();
            float tr = 1.0f;
            float tg = 1.0f;
            float tb = 0.8f;
            float ta = 1.0f;
            item7 = layout.addItem();
            String text = "Tooltip_weapon_Condition";
            if (this.hasHeadCondition()) {
                text = "Tooltip_weapon_HandleCondition";
            }
            item7.setLabel(Translator.getText((String)text, (Object[])new Object[0]) + ":", 1.0f, 1.0f, 0.8f, 1.0f);
            float f = (float)this.getCondition() / (float)this.getConditionMax();
            Core.getInstance().getBadHighlitedColor().interp(Core.getInstance().getGoodHighlitedColor(), f, g2Bgrad);
            item7.setProgress(f, g2Bgrad.getR(), g2Bgrad.getG(), g2Bgrad.getB(), 1.0f);
        }
        if (player != null && player.hasReadMap(this)) {
            item7 = layout.addItem();
            String label = Translator.getText((String)"Tooltip_literature_HasBeenRead", (Object[])new Object[0]);
            item7.setLabel(label, 1.0f, 1.0f, 0.8f, 1.0f);
        }
        if (this.isRecordedMedia()) {
            MediaData data = this.getMediaData();
            if (data != null) {
                if (data.getTranslatedTitle() != null) {
                    item7 = layout.addItem();
                    item7.setLabel(Translator.getText((String)"Tooltip_media_title", (Object[])new Object[0]) + ":", 1.0f, 1.0f, 0.8f, 1.0f);
                    item7.setValue(data.getTranslatedTitle(), 1.0f, 1.0f, 1.0f, 1.0f);
                    if (data.getTranslatedSubTitle() != null) {
                        item7 = layout.addItem();
                        item7.setLabel("", 1.0f, 1.0f, 0.8f, 1.0f);
                        item7.setValue(data.getTranslatedSubTitle(), 1.0f, 1.0f, 1.0f, 1.0f);
                    }
                }
                if (data.getTranslatedAuthor() != null) {
                    item7 = layout.addItem();
                    item7.setLabel(Translator.getText((String)"Tooltip_media_author", (Object[])new Object[0]) + ":", 1.0f, 1.0f, 0.8f, 1.0f);
                    item7.setValue(data.getTranslatedAuthor(), 1.0f, 1.0f, 1.0f, 1.0f);
                }
            }
            if (tooltipUI.getCharacter() instanceof IsoPlayer && this.hasBeenSeen((IsoPlayer)tooltipUI.getCharacter())) {
                item7 = layout.addItem();
                String label = Translator.getText((String)"ContextMenu_Watched", (Object[])new Object[0]);
                item7.setLabel(label, 1.0f, 1.0f, 0.8f, 1.0f);
            }
            if (tooltipUI.getCharacter() instanceof IsoPlayer && this.hasBeenHeard((IsoPlayer)tooltipUI.getCharacter())) {
                item7 = layout.addItem();
                String label = Translator.getText((String)"ContextMenu_Heard", (Object[])new Object[0]);
                item7.setLabel(label, 1.0f, 1.0f, 0.8f, 1.0f);
            }
        }
        if (this.hasTag(ItemTag.COMPASS) && this.isInPlayerInventory()) {
            IsoDirections dir = this.getOutermostContainer().getParent().getDir();
            item7 = layout.addItem();
            item7.setLabel(Translator.getText((String)COMPASS_TOOLTIP.get(dir), (Object[])new Object[0]), 1.0f, 1.0f, 0.8f, 1.0f);
        }
        if (this.isFishingLure()) {
            item7 = layout.addItem();
            item7.setLabel(Translator.getText((String)"Tooltip_IsFishingLure", (Object[])new Object[0]), 1.0f, 1.0f, 0.8f, 1.0f);
        }
        if (this.getAttributes() != null) {
            this.getAttributes().DoTooltip(tooltipUI, layout);
        }
        if (this.getFluidContainer() != null) {
            this.getFluidContainer().DoTooltip(tooltipUI, layout);
        }
        if (this.getWorldItem() != null && this.getWorldItem().getFluidContainer() != null) {
            this.getWorldItem().getFluidContainer().DoTooltip(tooltipUI, layout);
        }
        if (this.getDurabilityComponent() != null) {
            this.getDurabilityComponent().DoTooltip(tooltipUI, layout);
        }
        if (this.getVisionModifier() != 1.0f) {
            item7 = layout.addItem();
            item7.setLabel(Translator.getText((String)"Tooltip_item_VisionImpariment", (Object[])new Object[0]) + ": ", 1.0f, 1.0f, 0.8f, 1.0f);
            if (this.getVisionModifier() < 1.0f) {
                item7.setProgress(1.0f - this.getVisionModifier(), badR, badG, badB, 1.0f);
            } else {
                item7.setProgress(this.getVisionModifier() - 1.0f, goodR, goodG, goodB, 1.0f);
            }
        }
        if (this.getHearingModifier() != 1.0f) {
            item7 = layout.addItem();
            item7.setLabel(Translator.getText((String)"Tooltip_item_HearingImpariment", (Object[])new Object[0]) + ": ", 1.0f, 1.0f, 0.8f, 1.0f);
            if (this.getHearingModifier() < 1.0f) {
                item7.setProgress(1.0f - this.getHearingModifier(), badR, badG, badB, 1.0f);
            } else {
                item7.setProgress(this.getHearingModifier() - 1.0f, goodR, goodG, goodB, 1.0f);
            }
        }
        if (this.getDiscomfortModifier() != 0.0f) {
            item7 = layout.addItem();
            item7.setLabel(Translator.getText((String)"Tooltip_item_Discomfort", (Object[])new Object[0]) + ": ", 1.0f, 1.0f, 0.8f, 1.0f);
            if (this.getDiscomfortModifier() > 0.0f) {
                item7.setProgress(this.getDiscomfortModifier(), badR, badG, badB, 1.0f);
            } else {
                item7.setProgress(this.getDiscomfortModifier() * -1.0f, goodR, goodG, goodB, 1.0f);
            }
        }
        if (Core.getInstance().getOptionShowItemModInfo() && !this.isVanilla()) {
            item7 = layout.addItem();
            Color c = Colors.CornFlowerBlue;
            item7.setLabel("Mod: " + this.getModName(), c.r, c.g, c.b, 1.0f);
            ItemInfo info = WorldDictionary.getItemInfoFromID((short)this.getRegistry_id());
            if (info != null && info.getModOverrides() != null) {
                item7 = layout.addItem();
                float cl = 0.5f;
                if (info.getModOverrides().size() == 1) {
                    item7.setLabel("This item overrides: " + WorldDictionary.getModNameFromID((String)((String)info.getModOverrides().get(0))), 0.5f, 0.5f, 0.5f, 1.0f);
                } else {
                    item7.setLabel("This item overrides:", 0.5f, 0.5f, 0.5f, 1.0f);
                    for (int i2 = 0; i2 < info.getModOverrides().size(); ++i2) {
                        item7 = layout.addItem();
                        item7.setLabel(" - " + WorldDictionary.getModNameFromID((String)((String)info.getModOverrides().get(i2))), 0.5f, 0.5f, 0.5f, 1.0f);
                    }
                }
            }
        }
        if (this.getTooltip() != null) {
            item7 = layout.addItem();
            item7.setLabel(Translator.getText((String)this.getTooltip(), (Object[])new Object[0]), 1.0f, 1.0f, 0.8f, 1.0f);
        }
        if (layoutOverride == null) {
            y = layout.render(tooltipUI.padLeft, y, tooltipUI);
            tooltipUI.endLayout(layout);
            tooltipUI.setHeight((double)(y += tooltipUI.padBottom));
            if (tooltipUI.getWidth() < 150.0) {
                tooltipUI.setWidth(150.0);
            }
        }
    }

    private float drawTooltipItemTexture(ObjectTooltip tooltipUI, Texture tex, float x, float y, float width, float height, float r, float g, float b, float a) {
        tooltipUI.DrawTextureScaledAspect(tex, (double)x, (double)y, (double)width, (double)height, (double)r, (double)g, (double)b, (double)a);
        if (tex != null && tex.getWidth() > 0 && tex.getHeight() > 0) {
            float ratio = Math.min(width / (float)tex.getWidthOrig(), height / (float)tex.getHeightOrig());
            return PZMath.ceil((float)((float)tex.getWidth() * ratio));
        }
        return width;
    }

    public String getCleanString(float weight) {
        float value = (float)((int)(((double)weight + 0.005) * 100.0)) / 100.0f;
        return Float.toString(value);
    }

    public void DoTooltip(ObjectTooltip tooltipUI, ObjectTooltip.Layout layout) {
    }

    public void SetContainerPosition(int x, int y) {
        this.containerX = x;
        this.containerY = y;
    }

    public void Use() {
        this.Use(false);
    }

    public void UseAndSync() {
        this.Use(false, false, GameServer.server);
    }

    public void UseItem() {
        this.Use(false);
    }

    public void Use(boolean bCrafting) {
        this.Use(bCrafting, false, false);
    }

    public void Use(boolean bCrafting, boolean bInContainer, boolean bNeedSync) {
        if (!this.isDisappearOnUse() && !bCrafting) {
            return;
        }
        this.setCurrentUses(this.getCurrentUses() - 1);
        if (this.replaceOnUse != null && !bInContainer && !bCrafting && this.container != null) {
            InventoryItem item;
            Object s = this.replaceOnUse;
            if (!this.replaceOnUse.contains(".")) {
                s = this.module + "." + (String)s;
            }
            if ((item = this.container.AddItem((String)s)) != null) {
                item.setColorRed(this.colorRed);
                item.setColorGreen(this.colorGreen);
                item.setColorBlue(this.colorBlue);
                item.setColor(new Color(this.colorRed, this.colorGreen, this.colorBlue));
                item.setCustomColor(true);
                item.setModelIndex(this.modelIndex);
                this.container.setDrawDirty(true);
                this.container.setDirty(true);
                item.copyConditionStatesFrom(this);
                item.setFavorite(this.isFavorite());
                if (GameServer.server && bNeedSync) {
                    GameServer.sendAddItemToContainer((ItemContainer)this.container, (InventoryItem)item);
                }
            }
        }
        if (this.getCurrentUses() <= 0) {
            if (this.isKeepOnDeplete()) {
                return;
            }
            if (this.container != null) {
                IsoObject isoObject = this.container.parent;
                if (isoObject instanceof IsoGameCharacter) {
                    IsoGameCharacter chr = (IsoGameCharacter)isoObject;
                    if (!(this instanceof HandWeapon)) {
                        chr.removeFromHands(this);
                    }
                }
                this.container.items.remove((Object)this);
                this.container.setDirty(true);
                this.container.setDrawDirty(true);
                if (GameServer.server && bNeedSync) {
                    GameServer.sendRemoveItemFromContainer((ItemContainer)this.container, (InventoryItem)this);
                }
                this.container = null;
            }
        } else if (bNeedSync) {
            this.syncItemFields();
        }
    }

    private boolean shouldUpdateInRain() {
        Food food;
        if (GameClient.client) {
            return false;
        }
        IsoWorldInventoryObject worldItem = this.getWorldItem();
        if (worldItem == null || worldItem.getObjectIndex() == -1 || !worldItem.getSquare().isOutside()) {
            return false;
        }
        InventoryItem inventoryItem = this;
        boolean validFood = inventoryItem instanceof Food && !(food = (Food)inventoryItem).isTainted();
        boolean validWetItem = this.is(ItemKey.Normal.BATH_TOWEL_WET, ItemKey.Normal.DISH_CLOTH_WET);
        return validFood || validWetItem;
    }

    public boolean shouldUpdateInWorld() {
        if (!GameServer.server && this.itemHeat != 1.0f) {
            return true;
        }
        if (!GameClient.client && (this.hasComponent(ComponentType.FluidContainer) || this instanceof Food)) {
            IsoGridSquare sq = this.getWorldItem().getSquare();
            return sq != null && sq.isOutside();
        }
        return this.shouldUpdateInRain();
    }

    private void handleRainTaint(Food food) {
        if (LuaManager.GlobalObject.ZombRandFloat((float)0.0f, (float)1.0f) >= RainManager.getRainIntensity()) {
            return;
        }
        food.setTainted(true);
        if (GameServer.server) {
            GameServer.sendItemStats((InventoryItem)this);
        }
    }

    private void soakTowel() {
        this.setWetCooldown(this.getWetCooldown() + RainManager.getRainIntensity() * (4.0f * this.timeMultiplier));
        if (GameServer.server) {
            GameServer.sendItemStats((InventoryItem)this);
        }
    }

    private void handleWorldItemInRain() {
        if (GameClient.client || this.getWorldItem() == null || !RainManager.isRaining().booleanValue()) {
            return;
        }
        IsoGridSquare square = this.getWorldItem().getSquare();
        if (square == null || !square.isOutside()) {
            return;
        }
        InventoryItem inventoryItem = this;
        if (inventoryItem instanceof Food) {
            Food food = (Food)inventoryItem;
            this.handleRainTaint(food);
        }
        if (this.is(ItemKey.Normal.BATH_TOWEL_WET, ItemKey.Normal.DISH_CLOTH_WET)) {
            this.soakTowel();
        }
    }

    protected void calculateTimeMultiplier() {
        if (GameServer.server) {
            long nowMs = System.currentTimeMillis();
            long delta = this.lastUpdateMs == -1L ? 0L : Math.clamp(nowMs - this.lastUpdateMs, 0L, 6000L);
            this.timeMultiplier = GameTime.getInstance().getMultiplierFromTimeDelta((float)delta * 0.001f);
            this.lastUpdateMs = nowMs;
            return;
        }
        this.timeMultiplier = GameTime.getInstance().getMultiplier();
    }

    public void update() {
        this.calculateTimeMultiplier();
        if (this.isWet()) {
            this.wetCooldown -= 1.0f * this.timeMultiplier;
            if (this.wetCooldown <= 0.0f) {
                InventoryItem dryItem = InventoryItemFactory.CreateItem((String)this.itemWhenDry);
                if (this.isFavorite()) {
                    dryItem.setFavorite(true);
                }
                IsoWorldInventoryObject worldItem = this.getWorldItem();
                ItemContainer container = this.getContainer();
                if (worldItem != null) {
                    worldItem.swapItem(dryItem);
                } else if (container != null) {
                    container.addItem(dryItem);
                    container.Remove(this);
                    if (GameServer.server) {
                        GameServer.sendReplaceItemInContainer((ItemContainer)container, (InventoryItem)this, (InventoryItem)dryItem);
                    }
                }
                this.setWet(false);
                IsoWorld.instance.currentCell.addToProcessItemsRemove(this);
                LuaEventManager.triggerEvent((String)"OnContainerUpdate");
            }
            if (GameServer.server) {
                GameServer.sendItemStats((InventoryItem)this);
            }
        }
        if (this.hasComponent(ComponentType.FluidContainer)) {
            ItemContainer outermostContainer = this.getOutermostContainer();
            FluidContainer cont = this.getFluidContainer();
            if (outermostContainer != null) {
                float tempChange;
                float temp = outermostContainer.getTemprature();
                float f = tempChange = GameServer.server ? 0.06f : 0.001f;
                if (temp == 1.0f && this.itemHeat < 1.0f) {
                    this.itemHeat += tempChange * this.timeMultiplier;
                    if (this.itemHeat > temp) {
                        this.itemHeat = temp;
                    }
                }
                if (this.itemHeat > temp) {
                    this.itemHeat -= tempChange * this.timeMultiplier;
                    if (this.itemHeat < Math.max(0.2f, temp)) {
                        this.itemHeat = Math.max(0.2f, temp);
                    }
                }
                if (this.itemHeat < temp && (this.hasTag(ItemTag.COOKABLE) || this.hasTag(ItemTag.COOKABLE_MICROWAVE) && outermostContainer.getType().equals("microwave"))) {
                    this.itemHeat += temp / (float)(GameServer.server ? 16 : 1000) * this.timeMultiplier;
                    if (this.itemHeat > Math.min(3.0f, temp)) {
                        this.itemHeat = Math.min(3.0f, temp);
                    }
                }
                if (this.itemHeat > 1.6f && !cont.isEmpty()) {
                    if (cont.contains(Fluid.TaintedWater)) {
                        float oldAmount = cont.getSpecificFluidAmount(Fluid.TaintedWater);
                        float remove = PZMath.min((float)oldAmount, (float)((GameServer.server ? 0.6f : 0.01f) * this.timeMultiplier));
                        cont.adjustSpecificFluidAmount(Fluid.TaintedWater, oldAmount - remove);
                        cont.addFluid(Fluid.Water, remove);
                    }
                    if (cont.contains(Fluid.Petrol)) {
                        boolean isCampfire;
                        cont.removeFluid();
                        boolean bl = isCampfire = this.container != null && this.container.getParent() != null && this.container.getParent().getName() != null && this.container.getParent().getName().equals("Campfire");
                        if (!isCampfire && this.container != null && this.container.getParent() != null && this.container.getParent() instanceof IsoFireplace) {
                            isCampfire = true;
                        }
                        if (this.container != null && this.container.sourceGrid != null && !isCampfire) {
                            IsoFireManager.StartFire((IsoCell)this.container.sourceGrid.getCell(), (IsoGridSquare)this.container.sourceGrid, (boolean)true, (int)500000);
                        }
                    }
                    if (GameServer.server) {
                        GameServer.sendItemStats((InventoryItem)this);
                    }
                }
            }
        }
        if ((this.container == null || this.getWorldItem() != null) && this.itemHeat != 1.0f) {
            float temp2 = 1.0f;
            if (this.itemHeat > 1.0f) {
                this.itemHeat -= 0.001f * this.timeMultiplier;
                if (this.itemHeat < 1.0f) {
                    this.itemHeat = 1.0f;
                }
            }
            if (this.itemHeat < 1.0f) {
                this.itemHeat += 0.001f * this.timeMultiplier;
                if (this.itemHeat > 1.0f) {
                    this.itemHeat = 1.0f;
                }
            }
        }
        this.handleWorldItemInRain();
    }

    public boolean finishupdate() {
        if (this.shouldUpdateInRain()) {
            return false;
        }
        if (this.getWorldItem() != null && this.itemHeat != 1.0f) {
            return false;
        }
        if (this.hasComponent(ComponentType.FluidContainer)) {
            FluidContainer cont = this.getFluidContainer();
            if (this.getWorldItem() != null && this.getWorldItem().getObjectIndex() != -1 && cont.canPlayerEmpty()) {
                return false;
            }
            if (this.container != null && (this.itemHeat != 1.0f || this.itemHeat != this.container.getTemprature() || this.container.isTemperatureChanging())) {
                return false;
            }
        }
        return !this.isWet();
    }

    public String getSoundLimiterGroupID() {
        return null;
    }

    public void registerWithSoundLimiter(SoundInstanceLimiter limiter) {
    }

    public void updateSound(BaseSoundEmitter emitter) {
        this.updateEquippedAndActivatedSound(emitter);
    }

    public void updateSound(BaseSoundEmitter emitter, SoundLimiterParams params) {
    }

    public void stopSoundOnPlayer() {
    }

    public void updateEquippedAndActivatedSound(BaseSoundEmitter emitter) {
        String soundName = this.scriptItem.getSoundByID(SoundMapKey.EQUIPPED_AND_ACTIVATED);
        if (soundName == null) {
            return;
        }
        IsoPlayer player = this.getOwnerPlayer(this.getContainer());
        if (player == null) {
            this.stopEquippedAndActivatedSound();
            ItemSoundManager.removeItem((InventoryItem)this);
            return;
        }
        if (!this.isEquipped() || !this.isActivated()) {
            this.stopEquippedAndActivatedSound();
            ItemSoundManager.removeItem((InventoryItem)this);
            return;
        }
        BaseCharacterSoundEmitter currentEmitter = player.getEmitter();
        if (!currentEmitter.isPlaying(this.equippedAndActivatedSound)) {
            this.stopEquippedAndActivatedSound();
            this.equippedAndActivatedPlayer = player;
            this.equippedAndActivatedSound = currentEmitter.playSoundImpl(soundName, (IsoObject)player);
        }
    }

    public void updateEquippedAndActivatedSound() {
        String soundName = this.scriptItem.getSoundByID(SoundMapKey.EQUIPPED_AND_ACTIVATED);
        if (soundName == null) {
            return;
        }
        if (this.isActivated() && this instanceof DrainableComboItem && this.getCurrentUses() <= 0) {
            this.setActivated(false);
        }
        if (this.isEquipped() && this.isActivated()) {
            ItemSoundManager.addItem((InventoryItem)this);
        } else {
            this.stopEquippedAndActivatedSound();
            ItemSoundManager.removeItem((InventoryItem)this);
        }
    }

    protected void stopEquippedAndActivatedSound() {
        if (this.equippedAndActivatedPlayer != null && this.equippedAndActivatedSound != 0L) {
            this.equippedAndActivatedPlayer.getEmitter().stopOrTriggerSound(this.equippedAndActivatedSound);
            this.equippedAndActivatedPlayer = null;
            this.equippedAndActivatedSound = 0L;
        }
    }

    public void playActivateSound() {
        String soundName = this.scriptItem.getSoundByID(SoundMapKey.ACTIVATE);
        if (soundName == null) {
            return;
        }
        this.playSoundOnPlayer(soundName);
    }

    public void playDeactivateSound() {
        String soundName = this.scriptItem.getSoundByID(SoundMapKey.DEACTIVATE);
        if (soundName == null) {
            return;
        }
        this.playSoundOnPlayer(soundName);
    }

    public void playActivateDeactivateSound() {
        if (this.isActivated()) {
            this.playActivateSound();
        } else {
            this.playDeactivateSound();
        }
    }

    protected void playSoundOnPlayer(SoundKey soundKey) {
        if (soundKey == null) {
            return;
        }
        this.playSoundOnPlayer(soundKey.toString());
    }

    protected void playSoundOnPlayer(String soundName) {
        IsoPlayer player = this.getOwnerPlayer(this.getContainer());
        if (player == null || !player.isLocalPlayer()) {
            return;
        }
        player.getEmitter().playSound(soundName);
    }

    protected IsoPlayer getOwnerPlayer(ItemContainer container) {
        if (container == null) {
            return null;
        }
        IsoObject parent = container.getParent();
        if (parent instanceof IsoPlayer) {
            IsoPlayer isoPlayer = (IsoPlayer)parent;
            return isoPlayer;
        }
        return null;
    }

    public boolean is(ItemKey ... item) {
        String fullType = this.getFullType();
        for (int i = 0; i < item.length; ++i) {
            if (!item[i].toString().equals(fullType)) continue;
            return true;
        }
        return false;
    }

    public String getFullType() {
        assert (this.fullType != null && this.fullType.equals(this.module + "." + this.type));
        return this.fullType;
    }

    public void save(ByteBuffer output, boolean net) throws IOException {
        output.putShort(this.getRegistry_id());
        output.put((byte)-1);
        output.putInt(this.id);
        BitHeaderWrite header = BitHeader.allocWrite((BitHeader.HeaderSize)BitHeader.HeaderSize.Byte, (ByteBuffer)output);
        if (this.getCurrentUses() != 1) {
            header.addFlags(1);
            output.putInt(this.getCurrentUses());
        }
        if (this.condition != this.conditionMax) {
            header.addFlags(4);
            output.put((byte)this.getCondition());
        }
        if (this.visual != null) {
            header.addFlags(8);
            this.visual.save(output);
        }
        if (this.isCustomColor() && (this.col.r != 1.0f || this.col.g != 1.0f || this.col.b != 1.0f || this.col.a != 1.0f)) {
            header.addFlags(16);
            output.put(Bits.packFloatUnitToByte((float)this.getColor().r));
            output.put(Bits.packFloatUnitToByte((float)this.getColor().g));
            output.put(Bits.packFloatUnitToByte((float)this.getColor().b));
            output.put(Bits.packFloatUnitToByte((float)this.getColor().a));
        }
        if (this.itemCapacity != -1.0f) {
            header.addFlags(32);
            output.putFloat(this.itemCapacity);
        }
        BitHeaderWrite bits = BitHeader.allocWrite((BitHeader.HeaderSize)BitHeader.HeaderSize.Integer, (ByteBuffer)output);
        if (this.table != null && !this.table.isEmpty()) {
            bits.addFlags(1);
            this.table.save(output);
        }
        if (this.isActivated()) {
            bits.addFlags(2);
        }
        if (this.haveBeenRepaired != 0) {
            bits.addFlags(4);
            output.putShort((short)this.getHaveBeenRepaired());
        }
        if (this.name != null && !this.name.equals(this.originalName)) {
            bits.addFlags(8);
            GameWindow.WriteString((ByteBuffer)output, (String)this.name);
        }
        if (this.byteData != null) {
            bits.addFlags(16);
            this.byteData.rewind();
            output.putInt(this.byteData.limit());
            output.put(this.byteData);
            this.byteData.flip();
        }
        if (this.extraItems != null && !this.extraItems.isEmpty()) {
            bits.addFlags(32);
            output.putInt(this.extraItems.size());
            for (int i = 0; i < this.extraItems.size(); ++i) {
                output.putShort(WorldDictionary.getItemRegistryID((String)this.extraItems.get(i)));
            }
        }
        if (this.isCustomName()) {
            bits.addFlags(64);
        }
        if (this.isCustomWeight()) {
            bits.addFlags(128);
            output.putFloat(this.isCustomWeight() ? this.getActualWeight() : -1.0f);
        }
        if (this.keyId != -1) {
            bits.addFlags(256);
            output.putInt(this.getKeyId());
        }
        if (this.remoteControlId != -1 || this.remoteRange != 0) {
            bits.addFlags(1024);
            output.putInt(this.getRemoteControlID());
            output.putInt(this.getRemoteRange());
        }
        if (this.colorRed != 1.0f || this.colorGreen != 1.0f || this.colorBlue != 1.0f) {
            bits.addFlags(2048);
            output.put(Bits.packFloatUnitToByte((float)this.colorRed));
            output.put(Bits.packFloatUnitToByte((float)this.colorGreen));
            output.put(Bits.packFloatUnitToByte((float)this.colorBlue));
        }
        if (this.worker != null) {
            bits.addFlags(4096);
            GameWindow.WriteString((ByteBuffer)output, (String)this.getWorker());
        }
        if (this.wetCooldown != -1.0f) {
            bits.addFlags(8192);
            output.putFloat(this.wetCooldown);
        }
        if (this.isFavorite()) {
            bits.addFlags(16384);
        }
        if (this.stashMap != null) {
            bits.addFlags(32768);
            GameWindow.WriteString((ByteBuffer)output, (String)this.stashMap);
        }
        if (this.isInfected()) {
            bits.addFlags(65536);
        }
        if (this.currentAmmoCount != 0) {
            bits.addFlags(131072);
            output.putInt(this.currentAmmoCount);
        }
        if (this.attachedSlot != -1) {
            bits.addFlags(262144);
            output.putInt(this.attachedSlot);
        }
        if (this.attachedSlotType != null) {
            bits.addFlags(524288);
            GameWindow.WriteString((ByteBuffer)output, (String)this.attachedSlotType);
        }
        if (this.attachedToModel != null) {
            bits.addFlags(0x100000);
            GameWindow.WriteString((ByteBuffer)output, (String)this.attachedToModel);
        }
        if (this.maxCapacity != -1) {
            bits.addFlags(0x200000);
            output.putInt(this.maxCapacity);
        }
        if (this.isRecordedMedia()) {
            bits.addFlags(0x400000);
            output.putShort(this.recordedMediaIndex);
        }
        if (this.worldScale != 1.0f) {
            bits.addFlags(0x1000000);
            output.putFloat(this.worldScale);
        }
        if (this.isInitialised) {
            bits.addFlags(0x2000000);
        }
        if (this.requiresEntitySave()) {
            bits.addFlags(0x4000000);
            this.saveEntity(output);
        }
        if (this.animalTracks != null) {
            bits.addFlags(0x8000000);
            this.animalTracks.save(output);
        }
        if (this.texture != null && this.texture.getName() != null && this.texture != Texture.getSharedTexture((String)"media/inventory/Question_On.png") && this.scriptItem.getIcon() != null && !Objects.equals(this.scriptItem.getIcon(), "None") && !Objects.equals(this.scriptItem.getIcon(), "default")) {
            String icon2;
            String icon1 = this.texture.getName();
            int p = icon1.lastIndexOf(File.separator);
            if (p != -1) {
                icon1 = icon1.substring(p + 1).replace(".png", "");
            }
            if (!Objects.equals(icon1, icon2 = "Item_" + this.scriptItem.getIcon())) {
                bits.addFlags(0x10000000);
                GameWindow.WriteString((ByteBuffer)output, (String)this.texture.getName());
            }
        }
        if (this.modelIndex > -1) {
            bits.addFlags(0x20000000);
            output.putInt(this.modelIndex);
        }
        if (this.worldXRotation != 0.0f || this.worldYRotation != 0.0f || this.worldZRotation != -1.0f) {
            bits.addFlags(0x40000000);
            output.putFloat(this.worldXRotation);
            output.putFloat(this.worldYRotation);
            output.putFloat(this.worldZRotation);
        }
        if (!bits.equals(0)) {
            header.addFlags(64);
            bits.write();
        } else {
            output.position(bits.getStartPosition());
        }
        header.write();
        header.release();
        bits.release();
    }

    public static InventoryItem loadItem(ByteBuffer input, int worldVersion) throws IOException {
        return InventoryItem.loadItem(input, worldVersion, true);
    }

    public static InventoryItem loadItem(ByteBuffer input, int worldVersion, boolean doSaveTypeCheck) throws IOException {
        return InventoryItem.loadItem(input, worldVersion, doSaveTypeCheck, null);
    }

    public static InventoryItem loadItem(ByteBuffer input, int worldVersion, boolean doSaveTypeCheck, InventoryItem i) throws IOException {
        int dataLen = input.getInt();
        if (dataLen <= 0) {
            throw new IOException("InventoryItem.loadItem() invalid item data length: " + dataLen);
        }
        int position = input.position();
        short registryID = input.getShort();
        byte saveType = input.get();
        InventoryItem item = i;
        if (item == null) {
            item = InventoryItemFactory.CreateItem((short)registryID);
        }
        if (item != null) {
            try {
                item.load(input, worldVersion);
            }
            catch (Exception ex) {
                ExceptionLogger.logException((Throwable)ex);
                item = null;
            }
        }
        if (item == null) {
            if (input.position() < position + dataLen) {
                while (input.position() < position + dataLen) {
                    input.get();
                }
                DebugLog.log((String)("InventoryItem.loadItem() item == null, skipped bytes. itemtype: " + WorldDictionary.getItemTypeDebugString((short)registryID)));
            } else if (input.position() >= position + dataLen) {
                input.position(position + dataLen);
                DebugLog.log((String)("InventoryItem.loadItem() item == null, resetting buffer position to '" + (position + dataLen) + "'. itemtype: " + WorldDictionary.getItemTypeDebugString((short)registryID)));
            }
            return null;
        }
        if (dataLen != -1 && input.position() != position + dataLen) {
            input.position(position + dataLen);
            DebugLog.log((String)("InventoryItem.loadItem() data length not matching, resetting buffer position to '" + (position + dataLen) + "'. itemtype: " + WorldDictionary.getItemTypeDebugString((short)registryID)));
            if (Core.debug) {
                throw new IOException("InventoryItem.loadItem() read more data than save() wrote (" + WorldDictionary.getItemTypeDebugString((short)registryID) + ")");
            }
        }
        return item;
    }

    public void load(ByteBuffer input, int worldVersion) throws IOException {
        this.id = input.getInt();
        BitHeaderRead header = BitHeader.allocRead((BitHeader.HeaderSize)BitHeader.HeaderSize.Byte, (ByteBuffer)input);
        this.setCurrentUses(1);
        this.name = this.originalName;
        this.condition = this.conditionMax;
        this.customColor = false;
        this.col = Color.white;
        this.itemCapacity = -1.0f;
        this.table = null;
        this.activated = false;
        this.haveBeenRepaired = 0;
        this.customName = false;
        this.customWeight = false;
        this.keyId = -1;
        this.remoteControlId = -1;
        this.remoteRange = 0;
        this.colorBlue = 1.0f;
        this.colorGreen = 1.0f;
        this.colorRed = 1.0f;
        this.worker = null;
        this.wetCooldown = -1.0f;
        this.favorite = false;
        this.stashMap = null;
        this.zombieInfected = false;
        this.currentAmmoCount = 0;
        this.attachedSlot = -1;
        this.attachedSlotType = null;
        this.attachedToModel = null;
        this.maxCapacity = -1;
        this.recordedMediaIndex = (short)-1;
        this.worldZRotation = -1.0f;
        this.worldScale = 1.0f;
        this.isInitialised = false;
        if (!header.equals(0)) {
            if (worldVersion >= 220) {
                if (header.hasFlags(1)) {
                    this.setCurrentUses(input.getInt());
                }
                if (header.hasFlags(2)) {
                    // empty if block
                }
            } else {
                short usesvalue;
                if (header.hasFlags(1)) {
                    usesvalue = input.getShort();
                    this.setCurrentUses(usesvalue);
                }
                if (header.hasFlags(2)) {
                    usesvalue = input.get();
                }
            }
            if (header.hasFlags(4)) {
                this.setConditionWhileLoading(input.get());
            }
            if (header.hasFlags(8)) {
                this.visual = new ItemVisual();
                this.visual.load(input, worldVersion);
            }
            if (header.hasFlags(16)) {
                float r = Bits.unpackByteToFloatUnit((byte)input.get());
                float g = Bits.unpackByteToFloatUnit((byte)input.get());
                float b = Bits.unpackByteToFloatUnit((byte)input.get());
                float a = Bits.unpackByteToFloatUnit((byte)input.get());
                this.setColor(new Color(r, g, b, a));
                this.setCustomColor(true);
                if (this.getVisual() != null) {
                    this.getVisual().setTint(new ImmutableColor(this.colorRed, this.colorGreen, this.colorBlue));
                }
            }
            if (header.hasFlags(32)) {
                this.itemCapacity = input.getFloat();
            }
            if (header.hasFlags(64)) {
                float weight;
                int it;
                BitHeaderRead bits = BitHeader.allocRead((BitHeader.HeaderSize)BitHeader.HeaderSize.Integer, (ByteBuffer)input);
                if (bits.hasFlags(1)) {
                    if (this.table == null) {
                        this.table = LuaManager.platform.newTable();
                    }
                    this.table.load(input, worldVersion);
                }
                this.activated = bits.hasFlags(2);
                if (bits.hasFlags(4)) {
                    this.setHaveBeenRepaired(input.getShort());
                }
                if (bits.hasFlags(8)) {
                    this.name = GameWindow.ReadString((ByteBuffer)input);
                }
                if (bits.hasFlags(16)) {
                    int size = input.getInt();
                    this.byteData = ByteBuffer.allocate(size);
                    for (int n = 0; n < size; ++n) {
                        this.byteData.put(input.get());
                    }
                    this.byteData.flip();
                }
                if (bits.hasFlags(32) && (it = input.getInt()) > 0) {
                    this.extraItems = new ArrayList();
                    for (int i = 0; i < it; ++i) {
                        short registryItemId = input.getShort();
                        String itemfull = WorldDictionary.getItemTypeFromID((short)registryItemId);
                        this.extraItems.add(itemfull);
                    }
                }
                this.setCustomName(bits.hasFlags(64));
                if (bits.hasFlags(128) && (weight = input.getFloat()) >= 0.0f) {
                    this.setActualWeight(weight);
                    this.setWeight(weight);
                    this.setCustomWeight(true);
                }
                if (bits.hasFlags(256)) {
                    this.setKeyId(input.getInt());
                }
                if (bits.hasFlags(1024)) {
                    this.setRemoteControlID(input.getInt());
                    this.setRemoteRange(input.getInt());
                }
                if (bits.hasFlags(2048)) {
                    float r = Bits.unpackByteToFloatUnit((byte)input.get());
                    float g = Bits.unpackByteToFloatUnit((byte)input.get());
                    float b = Bits.unpackByteToFloatUnit((byte)input.get());
                    this.setColorRed(r);
                    this.setColorGreen(g);
                    this.setColorBlue(b);
                    this.setColor(new Color(this.colorRed, this.colorGreen, this.colorBlue));
                    this.setCustomColor(true);
                    if (this.getVisual() != null) {
                        this.getVisual().setTint(new ImmutableColor(this.colorRed, this.colorGreen, this.colorBlue));
                    }
                }
                if (bits.hasFlags(4096)) {
                    this.setWorker(GameWindow.ReadString((ByteBuffer)input));
                }
                if (bits.hasFlags(8192)) {
                    this.setWetCooldown(input.getFloat());
                }
                this.setFavorite(bits.hasFlags(16384));
                if (bits.hasFlags(32768)) {
                    this.stashMap = GameWindow.ReadString((ByteBuffer)input);
                }
                this.setInfected(bits.hasFlags(65536));
                if (bits.hasFlags(131072)) {
                    this.setCurrentAmmoCount(input.getInt());
                }
                if (bits.hasFlags(262144)) {
                    this.attachedSlot = input.getInt();
                }
                if (bits.hasFlags(524288)) {
                    this.attachedSlotType = GameWindow.ReadString((ByteBuffer)input);
                }
                if (bits.hasFlags(0x100000)) {
                    this.attachedToModel = GameWindow.ReadString((ByteBuffer)input);
                }
                if (bits.hasFlags(0x200000)) {
                    this.maxCapacity = input.getInt();
                }
                if (bits.hasFlags(0x400000)) {
                    this.setRecordedMediaIndex(input.getShort());
                }
                if (worldVersion < 232 && bits.hasFlags(0x800000)) {
                    this.setWorldZRotation(input.getInt());
                }
                if (bits.hasFlags(0x1000000)) {
                    this.worldScale = input.getFloat();
                }
                this.setInitialised(bits.hasFlags(0x2000000));
                if (bits.hasFlags(0x4000000)) {
                    ArrayList loadedComponents = new ArrayList();
                    this.loadEntity(input, worldVersion, loadedComponents);
                    for (int i = this.componentSize() - 1; i >= 0; --i) {
                        Component component = this.getComponentForIndex(i);
                        if (loadedComponents.contains(component)) continue;
                        GameEntityFactory.RemoveComponent((GameEntity)this, (Component)component);
                    }
                    loadedComponents.clear();
                }
                if (bits.hasFlags(0x8000000)) {
                    this.animalTracks = new AnimalTracks();
                    this.animalTracks.load(input, worldVersion);
                }
                if (bits.hasFlags(0x10000000)) {
                    this.setTexture(Texture.getSharedTexture((String)GameWindow.ReadString((ByteBuffer)input)));
                }
                if (bits.hasFlags(0x20000000)) {
                    this.modelIndex = input.getInt();
                }
                if (bits.hasFlags(0x40000000)) {
                    if (worldVersion >= 232) {
                        this.setWorldXRotation(input.getFloat());
                        this.setWorldYRotation(input.getFloat());
                        this.setWorldZRotation(input.getFloat());
                    } else {
                        this.setWorldYRotation(input.getInt());
                        this.setWorldXRotation(input.getInt());
                    }
                }
                bits.release();
            }
        }
        this.synchWithVisual();
        header.release();
    }

    public InventoryItem createCloneItem() {
        if (Core.getInstance().getDebug()) {
            InventoryItem newItem = InventoryItemFactory.CreateItem((String)this.getFullType());
            try {
                tempBuffer.clear();
                int originalID = newItem.id;
                this.save(tempBuffer, false);
                tempBuffer.rewind();
                tempBuffer.getShort();
                tempBuffer.get();
                newItem.load(tempBuffer, 249);
                newItem.id = originalID;
            }
            catch (IOException e) {
                DebugType.General.printException((Throwable)e, LogSeverity.Error);
            }
            return newItem;
        }
        return null;
    }

    public boolean IsFood() {
        return false;
    }

    public boolean IsWeapon() {
        return false;
    }

    public boolean IsDrainable() {
        return false;
    }

    public boolean IsLiterature() {
        return false;
    }

    public boolean IsClothing() {
        return false;
    }

    public boolean IsInventoryContainer() {
        return false;
    }

    public boolean IsMap() {
        return false;
    }

    static InventoryItem LoadFromFile(DataInputStream input) throws IOException {
        GameWindow.ReadString((DataInputStream)input);
        return null;
    }

    public ItemContainer getOutermostContainer() {
        if (this.container == null || "floor".equals(this.container.type)) {
            return null;
        }
        ItemContainer outer = this.container;
        while (outer.getContainingItem() != null && outer.getContainingItem().getContainer() != null && !"floor".equals(outer.getContainingItem().getContainer().type)) {
            outer = outer.getContainingItem().getContainer();
        }
        return outer;
    }

    public boolean isInLocalPlayerInventory() {
        if (!GameClient.client) {
            return false;
        }
        ItemContainer outer = this.getOutermostContainer();
        if (outer == null) {
            return false;
        }
        if (outer.getParent() instanceof IsoPlayer) {
            return ((IsoPlayer)outer.getParent()).isLocalPlayer();
        }
        return false;
    }

    public boolean isInPlayerInventory() {
        ItemContainer outer = this.getOutermostContainer();
        if (outer == null) {
            return false;
        }
        return outer.getParent() instanceof IsoPlayer;
    }

    public ItemReplacement getItemReplacementPrimaryHand() {
        return this.scriptItem.replacePrimaryHand;
    }

    public ItemReplacement getItemReplacementSecondHand() {
        return this.scriptItem.replaceSecondHand;
    }

    public ClothingItem getClothingItem() {
        Item withoutDrainableItem;
        String alternateModelName1 = this.getAlternateModelName();
        if ("RightHand".equalsIgnoreCase(alternateModelName1)) {
            return this.getItemReplacementPrimaryHand().clothingItem;
        }
        if ("LeftHand".equalsIgnoreCase(alternateModelName1)) {
            return this.getItemReplacementSecondHand().clothingItem;
        }
        if ("WithoutDrainable".equalsIgnoreCase(alternateModelName1) && (withoutDrainableItem = ScriptManager.instance.getItem(this.getWithoutDrainable())) != null && withoutDrainableItem.getClothingItemAsset() != null) {
            return withoutDrainableItem.getClothingItemAsset();
        }
        return this.scriptItem.getClothingItemAsset();
    }

    public String getAlternateModelName() {
        IsoObject isoObject;
        if (this.getContainer() != null && (isoObject = this.getContainer().getParent()) instanceof IsoGameCharacter) {
            IsoGameCharacter chr = (IsoGameCharacter)isoObject;
            if (chr.getPrimaryHandItem() == this && this.getItemReplacementPrimaryHand() != null) {
                return "RightHand";
            }
            if (chr.getSecondaryHandItem() == this && this.getItemReplacementSecondHand() != null) {
                return "LeftHand";
            }
        }
        return this.alternateModelName;
    }

    public ItemVisual getVisual() {
        ClothingItem clothingItem = this.getClothingItem();
        if (clothingItem == null || !clothingItem.isReady()) {
            this.visual = null;
            return null;
        }
        if (this.visual == null) {
            this.visual = new ItemVisual();
            this.visual.setItemType(this.getFullType());
            this.visual.pickUninitializedValues(clothingItem);
        }
        this.visual.setClothingItemName(clothingItem.mame);
        this.visual.setAlternateModelName(this.getAlternateModelName());
        return this.visual;
    }

    public boolean allowRandomTint() {
        ClothingItem clothingItem = this.getClothingItem();
        if (clothingItem != null) {
            return clothingItem.allowRandomTint;
        }
        return false;
    }

    public void synchWithVisual() {
        String icon;
        if (this instanceof HandWeapon && ((HandWeapon)this).getWeaponSpritesByIndex() != null && this.modelIndex == -1) {
            maxIndex = ((HandWeapon)this).getWeaponSpritesByIndex().size();
            this.modelIndex = Rand.Next((int)maxIndex);
        } else if ((this.getStaticModelsByIndex() != null || this.getWorldStaticModelsByIndex() != null) && this.modelIndex == -1) {
            maxIndex = this.getStaticModelsByIndex() != null && this.getWorldStaticModelsByIndex() != null ? Math.max(this.getStaticModelsByIndex().size(), this.getWorldStaticModelsByIndex().size()) : (this.getStaticModelsByIndex() != null && this.getWorldStaticModelsByIndex() == null ? this.getStaticModelsByIndex().size() : this.getWorldStaticModelsByIndex().size());
            this.modelIndex = Rand.Next((int)maxIndex);
        }
        if (this.modelIndex != -1 && this.getIconsForTexture() != null && this.getIconsForTexture().get(this.modelIndex) != null && !StringUtils.isNullOrWhitespace((String)(icon = this.getIconsForTexture().get(this.modelIndex)))) {
            this.texture = Texture.trygetTexture((String)("Item_" + icon));
            if (this.texture == null) {
                this.texture = Texture.getSharedTexture((String)"media/inventory/Question_On.png");
            }
        }
        if (!(this instanceof Clothing) && !(this instanceof InventoryContainer)) {
            return;
        }
        ItemVisual visu = this.getVisual();
        if (visu == null) {
            return;
        }
        if (this instanceof Clothing && this.getBloodClothingType() != null) {
            BloodClothingType.calcTotalBloodLevel((Clothing)((Clothing)this));
            BloodClothingType.calcTotalDirtLevel((Clothing)((Clothing)this));
        }
        ClothingItem clothingItem = this.getClothingItem();
        if (clothingItem.allowRandomTint && !this.customColor) {
            this.setColor(new Color(visu.tint.r, visu.tint.g, visu.tint.b));
        } else {
            this.setColor(new Color(this.getColorRed(), this.getColorGreen(), this.getColorBlue()));
        }
        if (clothingItem.baseTextures.size() <= 1 && visu.textureChoice <= -1 || this.getIconsForTexture() == null) {
            return;
        }
        String icon2 = null;
        if (visu.baseTexture > -1 && this.getIconsForTexture().size() > visu.baseTexture) {
            icon2 = this.getIconsForTexture().get(visu.baseTexture);
        } else if (visu.textureChoice > -1 && this.getIconsForTexture().size() > visu.textureChoice) {
            icon2 = this.getIconsForTexture().get(visu.textureChoice);
        }
        if (StringUtils.isNullOrWhitespace(icon2)) {
            return;
        }
        this.texture = Texture.trygetTexture((String)("Item_" + icon2));
        if (this.texture == null) {
            this.texture = Texture.getSharedTexture((String)"media/inventory/Question_On.png");
        }
    }

    public int getContainerX() {
        return this.containerX;
    }

    public void setContainerX(int containerX) {
        this.containerX = containerX;
    }

    public int getContainerY() {
        return this.containerY;
    }

    public void setContainerY(int containerY) {
        this.containerY = containerY;
    }

    public boolean isDisappearOnUse() {
        return this.scriptItem.isDisappearOnUse();
    }

    public boolean isKeepOnDeplete() {
        return !this.scriptItem.isDisappearOnUse();
    }

    public String getName() {
        return this.getName(null);
    }

    public String getName(IsoPlayer player) {
        if (this.getFluidContainer() != null) {
            return this.getFluidContainer().getUiName();
        }
        if (this.getWorldItem() != null && this.getWorldItem().getFluidContainer() != null) {
            return this.getWorldItem().getFluidUiName();
        }
        if (this.getRemoteControlID() != -1) {
            return Translator.getText((String)"IGUI_ItemNameControllerLinked", (Object[])new Object[]{this.name});
        }
        String fakeName = this.name;
        if (this.getMechanicType() > 0) {
            fakeName = Translator.getText((String)"IGUI_ItemNameMechanicalType", (Object[])new Object[]{fakeName, Translator.getText((String)("IGUI_VehicleType_" + this.getMechanicType()), (Object[])new Object[0])});
        }
        Object prefix = "";
        if (this.isBloody()) {
            prefix = (String)prefix + Translator.getText((String)"IGUI_ClothingName_Bloody", (Object[])new Object[0]) + ", ";
        }
        if (this.isBroken()) {
            prefix = (String)prefix + Translator.getText((String)"Tooltip_broken", (Object[])new Object[0]) + ", ";
        } else if ((float)this.getCondition() < (float)this.getConditionMax() / 3.0f) {
            prefix = (String)prefix + Translator.getText((String)"IGUI_Name_Worn", (Object[])new Object[0]) + ", ";
        }
        if (!this.isBroken() && this.hasSharpness() && this.getSharpness() < 0.33333334f) {
            prefix = this.getSharpness() <= 0.0f ? (String)prefix + Translator.getText((String)"Tooltip_blunt", (Object[])new Object[0]) + ", " : (String)prefix + Translator.getText((String)"Tooltip_dull", (Object[])new Object[0]) + ", ";
        }
        if (this instanceof DrainableComboItem && this.getCurrentUsesFloat() <= 0.0f || this instanceof WeaponPart && this.getCurrentUsesFloat() <= 0.0f) {
            prefix = (String)prefix + this.emptyString + ", ";
        }
        if (this.canBeActivated() && this.isActivated()) {
            prefix = (String)prefix + Translator.getText((String)"Tooltip_activated", (Object[])new Object[0]) + ", ";
        }
        if (((String)prefix).length() > 2) {
            prefix = ((String)prefix).substring(0, ((String)prefix).length() - 2);
        }
        if (((String)(prefix = ((String)prefix).trim())).isEmpty()) {
            return fakeName;
        }
        return Translator.getText((String)"IGUI_ClothingNaming", (Object[])new Object[]{prefix, fakeName});
    }

    public void setName(String name) {
        if (name.length() > 256) {
            name = name.substring(0, Math.min(name.length(), 256));
        }
        this.name = name;
    }

    public String getReplaceOnUse() {
        return this.replaceOnUse;
    }

    public void setReplaceOnUse(String replaceOnUse) {
        this.replaceOnUse = replaceOnUse;
        this.replaceOnUseFullType = StringUtils.moduleDotType((String)this.getModule(), (String)replaceOnUse);
    }

    public String getReplaceOnUseFullType() {
        return this.replaceOnUseFullType;
    }

    public int getConditionMax() {
        return this.conditionMax;
    }

    public void setConditionMax(int conditionMax) {
        this.conditionMax = conditionMax;
    }

    public ItemContainer getRightClickContainer() {
        return this.rightClickContainer;
    }

    public void setRightClickContainer(ItemContainer rightClickContainer) {
        this.rightClickContainer = rightClickContainer;
    }

    public String getSwingAnim() {
        return this.scriptItem.swingAnim;
    }

    public Texture getTexture() {
        if (!this.hasModData()) {
            return this.texture;
        }
        Object customIconName = this.getModData().rawget((Object)"customInventoryIcon");
        if (!(customIconName instanceof String)) {
            return this.texture;
        }
        Texture customTexture = Texture.getSharedTexture((String)((String)customIconName));
        if (customTexture != null) {
            return customTexture;
        }
        return this.texture;
    }

    public Texture getIcon() {
        return this.getTexture();
    }

    public void setTexture(Texture texture) {
        this.texture = texture;
    }

    public void setIcon(Texture texture) {
        this.setTexture(texture);
    }

    public Texture getTexturerotten() {
        return this.texturerotten;
    }

    public void setTexturerotten(Texture texturerotten) {
        this.texturerotten = texturerotten;
    }

    public Texture getTextureCooked() {
        return this.textureCooked;
    }

    public void setTextureCooked(Texture textureCooked) {
        this.textureCooked = textureCooked;
    }

    public Texture getTextureBurnt() {
        return this.textureBurnt;
    }

    public void setTextureBurnt(Texture textureBurnt) {
        this.textureBurnt = textureBurnt;
    }

    public void setType(String type) {
        this.type = type;
        this.fullType = this.module + "." + type;
    }

    public void setCurrentUses(int newuses) {
        this.uses = newuses;
    }

    public int getCurrentUses() {
        return this.uses;
    }

    public void setCurrentUsesFrom(InventoryItem other) {
        float delta = (float)other.getCurrentUses() / (float)other.getMaxUses();
        this.setCurrentUses((int)(delta * (float)this.getMaxUses()));
    }

    public int getMaxUses() {
        return 1;
    }

    public float getCurrentUsesFloat() {
        return (float)this.uses / (float)this.getMaxUses();
    }

    public void setCurrentUsesFloat(float newUses) {
        newUses = PZMath.clamp((float)newUses, (float)0.0f, (float)1.0f);
        this.uses = Math.round(newUses / this.useDelta);
    }

    public float getUseDelta() {
        return this.useDelta;
    }

    public void setUseDelta(float useDelta) {
        this.useDelta = useDelta;
    }

    @Deprecated
    public int getUses() {
        return this.uses;
    }

    @Deprecated
    public void setUses(int newuses) {
        this.uses = newuses;
    }

    public void setUsesFrom(InventoryItem other) {
        this.setUses(other.getUses());
    }

    public float getAge() {
        return this.age;
    }

    public void setAge(float age) {
        this.age = age;
    }

    public float getLastAged() {
        return this.lastAged;
    }

    public void setLastAged(float time) {
        this.lastAged = time;
    }

    public void updateAge() {
    }

    public void setAutoAge() {
    }

    public boolean isIsCookable() {
        return this.isCookable;
    }

    public boolean isCookable() {
        return this.isCookable;
    }

    public void setIsCookable(boolean isCookable) {
        this.isCookable = isCookable;
    }

    public float getCookingTime() {
        return this.cookingTime;
    }

    public void setCookingTime(float cookingTime) {
        this.cookingTime = cookingTime;
    }

    public float getMinutesToCook() {
        return this.minutesToCook;
    }

    public void setMinutesToCook(float minutesToCook) {
        this.minutesToCook = minutesToCook;
    }

    public float getMinutesToBurn() {
        return this.minutesToBurn;
    }

    public void setMinutesToBurn(float minutesToBurn) {
        this.minutesToBurn = minutesToBurn;
    }

    public boolean isCooked() {
        return this.cooked;
    }

    public void setCooked(boolean cooked) {
        this.cooked = cooked;
        if (cooked && this.getCookingTime() < this.getMinutesToCook()) {
            this.setCookingTime(this.getMinutesToCook());
        }
    }

    public boolean isBurnt() {
        return this.burnt;
    }

    public void setBurnt(boolean burnt) {
        this.burnt = burnt;
        if (burnt && this.getCookingTime() < this.getMinutesToBurn()) {
            this.setCookingTime(this.getMinutesToBurn());
        }
    }

    public int getOffAge() {
        return this.offAge;
    }

    public void setOffAge(int offAge) {
        this.offAge = offAge;
    }

    public int getOffAgeMax() {
        return this.offAgeMax;
    }

    public void setOffAgeMax(int offAgeMax) {
        this.offAgeMax = offAgeMax;
    }

    public float getWeight() {
        return Math.max(this.weight, 0.0f);
    }

    public void setWeight(float weight) {
        if (weight < 0.0f) {
            weight = 0.0f;
        }
        this.weight = weight;
    }

    public float getActualWeight() {
        return this.getActualWeightUnmodded();
    }

    public float getActualWeightUnmodded() {
        if (this.getDisplayName().equals(this.getFullType())) {
            return 0.0f;
        }
        return Math.max(this.actualWeight, 0.0f);
    }

    public void setActualWeight(float actualWeight) {
        if (actualWeight < 0.0f) {
            actualWeight = 0.0f;
        }
        this.actualWeight = actualWeight;
    }

    public String getWorldTexture() {
        return this.worldTexture;
    }

    public void setWorldTexture(String worldTexture) {
        this.worldTexture = worldTexture;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void incrementCondition(int increment) {
        this.condition += increment;
    }

    public int getCondition() {
        return this.condition;
    }

    public void setCondition(int condition, boolean doSound) {
        if (Core.debug && DebugOptions.instance.cheat.player.unlimitedCondition.getValue() && this.condition > condition) {
            return;
        }
        condition = Math.max(0, condition);
        condition = Math.min(this.getConditionMax(), condition);
        if (doSound && this.condition > 0 && condition <= 0) {
            this.doBreakSound();
        } else if (doSound && this.condition > 0 && condition < this.condition) {
            this.doDamagedSound();
        }
        this.condition = condition;
        this.setBroken(condition <= 0);
    }

    public void doBreakSound() {
        IsoPlayer player = this.getPlayer();
        if (player == null) {
            return;
        }
        if (!StringUtils.isNullOrWhitespace((String)this.getBreakSound())) {
            if (GameServer.server) {
                INetworkPacket.sendToRelative((PacketTypes.PacketType)PacketTypes.PacketType.PlaySound, (float)player.getXi(), (float)player.getYi(), (Object[])new Object[]{this.getBreakSound(), (byte)0, player});
                return;
            }
            player.playSound(this.getBreakSound());
        } else if (!StringUtils.isNullOrWhitespace((String)this.getDamagedSound())) {
            this.doDamagedSound();
        }
    }

    public void doDamagedSound() {
        IsoPlayer player = this.getPlayer();
        if (player == null) {
            return;
        }
        if (!StringUtils.isNullOrWhitespace((String)this.getDamagedSound())) {
            if (GameServer.server) {
                INetworkPacket.sendToRelative((PacketTypes.PacketType)PacketTypes.PacketType.PlaySound, (float)player.getXi(), (float)player.getYi(), (Object[])new Object[]{this.getDamagedSound(), (byte)0, player});
                return;
            }
            player.playSound(this.getDamagedSound());
        }
    }

    public void setCondition(int condition) {
        this.setCondition(condition, true);
    }

    public void setConditionNoSound(int condition) {
        this.setCondition(condition, false);
    }

    public void setConditionWhileLoading(int condition) {
        this.condition = PZMath.clamp((int)condition, (int)0, (int)this.getConditionMax());
        this.broken = this.condition <= 0;
    }

    public String getOffString() {
        return this.offString;
    }

    public void setOffString(String offString) {
        this.offString = offString;
    }

    public String getCookedString() {
        return this.cookedString;
    }

    public void setCookedString(String cookedString) {
        this.cookedString = cookedString;
    }

    public String getUnCookedString() {
        return this.unCookedString;
    }

    public void setUnCookedString(String unCookedString) {
        this.unCookedString = unCookedString;
    }

    public String getBurntString() {
        return this.burntString;
    }

    public void setBurntString(String burntString) {
        this.burntString = burntString;
    }

    public String getModule() {
        return this.module;
    }

    public void setModule(String module) {
        this.module = module;
        this.fullType = module + "." + this.type;
    }

    public boolean isAlwaysWelcomeGift() {
        return this.scriptItem.isAlwaysWelcomeGift();
    }

    public boolean isCanBandage() {
        return this.scriptItem.isCanBandage();
    }

    public float getBoredomChange() {
        return this.boredomChange;
    }

    public void setBoredomChange(float boredomChange) {
        this.boredomChange = boredomChange;
    }

    public float getUnhappyChange() {
        return this.unhappyChange;
    }

    public void setUnhappyChange(float unhappyChange) {
        this.unhappyChange = unhappyChange;
    }

    public float getStressChange() {
        return this.stressChange;
    }

    public void setStressChange(float stressChange) {
        this.stressChange = stressChange;
    }

    public int getFoodSicknessChange() {
        return this.foodSicknessChange;
    }

    public void setFoodSicknessChange(int foodSicknessChange) {
        this.foodSicknessChange = foodSicknessChange;
    }

    public int getInverseCoughProbability() {
        return this.inverseCoughProbability;
    }

    public void setInverseCoughProbability(int inverseCoughProbability) {
        this.inverseCoughProbability = inverseCoughProbability;
    }

    public int getInverseCoughProbabilitySmoker() {
        return this.inverseCoughProbabilitySmoker;
    }

    public void setInverseCoughProbabilitySmoker(int inverseCoughProbabilitySmoker) {
        this.inverseCoughProbabilitySmoker = inverseCoughProbabilitySmoker;
    }

    public Set<ItemTag> getTags() {
        return this.scriptItem.getTags();
    }

    public boolean hasTag(ItemTag ... tags) {
        return this.scriptItem.hasTag(tags);
    }

    public boolean hasTag(ItemTag itemTag) {
        return this.scriptItem.hasTag(itemTag);
    }

    public ArrayList<IsoObject> getTaken() {
        return this.taken;
    }

    public void setTaken(ArrayList<IsoObject> taken) {
        this.taken = taken;
    }

    public void setReplaceOnUseOn(String replaceOnUseOn) {
        this.replaceOnUseOn = replaceOnUseOn;
    }

    public String getReplaceOnUseOn() {
        return this.replaceOnUseOn;
    }

    public String getReplaceOnUseOnString() {
        Object replaceWith = this.getReplaceOnUseOn();
        if (((String)replaceWith).split("-")[0].trim().contains("WaterSource")) {
            replaceWith = ((String)replaceWith).split("-")[1];
            String s = replaceWith;
            if (!((String)replaceWith).contains(".")) {
                replaceWith = this.getModule() + "." + s;
            }
        }
        return replaceWith;
    }

    public String getReplaceTypes() {
        return this.scriptItem.getReplaceTypes();
    }

    public HashMap<String, String> getReplaceTypesMap() {
        return this.scriptItem.getReplaceTypesMap();
    }

    public String getReplaceType(String key) {
        return this.scriptItem.getReplaceType(key);
    }

    public boolean hasReplaceType(String key) {
        return this.scriptItem.hasReplaceType(key);
    }

    public boolean isWaterSource() {
        return this.hasComponent(ComponentType.FluidContainer) && this.getFluidContainer().isWaterSource() && !this.getFluidContainer().isMultiTileMoveable();
    }

    public boolean isWaterOnlySource() {
        return this.hasComponent(ComponentType.FluidContainer) && this.getFluidContainer().isWaterOnlySource() && !this.getFluidContainer().isMultiTileMoveable();
    }

    boolean CanStackNoTemp(InventoryItem item) {
        return false;
    }

    public void CopyModData(KahluaTable defaultModData) {
        this.copyModData(defaultModData);
    }

    public void copyModData(KahluaTable modData) {
        if (this.table != null) {
            this.table.wipe();
        }
        if (modData == null) {
            return;
        }
        LuaManager.copyTable((KahluaTable)this.getModData(), (KahluaTable)modData);
    }

    public int getCount() {
        return this.count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public boolean isActivated() {
        return this.activated;
    }

    public void setActivated(boolean activated) {
        this.activated = activated;
        if (this.canEmitLight() && GameClient.client && this.getEquipParent() != null) {
            if (this.getEquipParent().getPrimaryHandItem() == this) {
                this.getEquipParent().reportEvent("EventSetActivatedPrimary");
            } else if (this.getEquipParent().getSecondaryHandItem() == this) {
                this.getEquipParent().reportEvent("EventSetActivatedSecondary");
            }
        }
    }

    public void setActivatedRemote(boolean activated) {
        this.activated = activated;
    }

    public void setCanBeActivated(boolean activatedItem) {
        this.canBeActivated = activatedItem;
    }

    public boolean canBeActivated() {
        return this.canBeActivated;
    }

    public void setLightStrength(float lightStrength) {
        this.lightStrength = lightStrength;
    }

    public float getLightStrength() {
        return this.lightStrength;
    }

    public boolean isTorchCone() {
        return this.isTorchCone;
    }

    public void setTorchCone(boolean isTorchCone) {
        this.isTorchCone = isTorchCone;
    }

    public float getTorchDot() {
        return this.scriptItem.torchDot;
    }

    public int getLightDistance() {
        return this.lightDistance;
    }

    public void setLightDistance(int lightDistance) {
        this.lightDistance = lightDistance;
    }

    public boolean canEmitLight() {
        if (this.getLightStrength() <= 0.0f) {
            return false;
        }
        Drainable drainable = (Drainable)Type.tryCastTo((Object)((Object)this), Drainable.class);
        return drainable == null || this.getCurrentUses() > 0;
    }

    public boolean isEmittingLight() {
        if (!this.canEmitLight()) {
            return false;
        }
        return !this.canBeActivated() || this.isActivated();
    }

    public boolean canStoreWater() {
        return this.hasComponent(ComponentType.FluidContainer);
    }

    public float getFatigueChange() {
        return this.fatigueChange;
    }

    public void setFatigueChange(float fatigueChange) {
        this.fatigueChange = fatigueChange;
    }

    public float getCurrentCondition() {
        return (float)this.condition / (float)this.conditionMax * 100.0f;
    }

    public void setColor(Color color) {
        this.col = color;
    }

    public Color getColor() {
        return this.col;
    }

    public ColorInfo getColorInfo() {
        return new ColorInfo(this.col.getRedFloat(), this.col.getGreenFloat(), this.col.getBlueFloat(), this.col.getAlphaFloat());
    }

    public boolean isTwoHandWeapon() {
        return this.scriptItem.twoHandWeapon;
    }

    public String getCustomMenuOption() {
        return this.customMenuOption;
    }

    public void setCustomMenuOption(String customMenuOption) {
        this.customMenuOption = customMenuOption;
    }

    public void setTooltip(String tooltip) {
        this.getModData().rawset((Object)"Tooltip", (Object)tooltip);
        this.tooltip = tooltip;
    }

    public String getTooltip() {
        Object object = this.getModData().rawget((Object)"Tooltip");
        if (object instanceof String) {
            String str = (String)object;
            return str;
        }
        return this.tooltip;
    }

    public String getDisplayCategory() {
        return this.displayCategory;
    }

    public void setDisplayCategory(String displayCategory) {
        this.displayCategory = displayCategory;
    }

    public int getHaveBeenRepaired() {
        return this.haveBeenRepaired;
    }

    public void setHaveBeenRepaired(int haveBeenRepaired) {
        this.haveBeenRepaired = haveBeenRepaired;
    }

    public int getTimesRepaired() {
        return this.haveBeenRepaired;
    }

    public void setTimesRepaired(int haveBeenRepaired) {
        this.haveBeenRepaired = haveBeenRepaired;
    }

    public void copyTimesRepairedFrom(InventoryItem item) {
        this.setTimesRepaired(item.getTimesRepaired());
    }

    public void copyTimesRepairedTo(InventoryItem item) {
        item.setTimesRepaired(this.getTimesRepaired());
    }

    public int getTimesHeadRepaired() {
        if (this.attrib() != null && this.attrib().contains((AttributeType)Attribute.TimesHeadRepaired)) {
            return this.attrib().get(Attribute.TimesHeadRepaired);
        }
        return this.haveBeenRepaired;
    }

    public void setTimesHeadRepaired(int haveBeenRepaired) {
        if (this.attrib() != null && this.attrib().contains((AttributeType)Attribute.TimesHeadRepaired)) {
            this.attrib().set(Attribute.TimesHeadRepaired, haveBeenRepaired);
            return;
        }
        this.haveBeenRepaired = haveBeenRepaired;
    }

    public boolean hasTimesHeadRepaired() {
        return this.attrib() != null && this.attrib().contains((AttributeType)Attribute.TimesHeadRepaired);
    }

    public void copyTimesHeadRepairedFrom(InventoryItem item) {
        this.setTimesHeadRepaired(item.getTimesHeadRepaired());
    }

    public void copyTimesHeadRepairedTo(InventoryItem item) {
        item.setTimesHeadRepaired(this.getTimesHeadRepaired());
    }

    public boolean isBroken() {
        return this.broken;
    }

    public void setBroken(boolean broken) {
        this.broken = broken;
        if (!GameClient.client && broken) {
            this.onBreak();
        }
    }

    public String getDisplayName() {
        return this.name;
    }

    public boolean isTrap() {
        return this.scriptItem.trap;
    }

    public void addExtraItem(ItemKey key) {
        this.addExtraItem(key.toString());
    }

    public void addExtraItem(String type) {
        if (this.extraItems == null) {
            this.extraItems = new ArrayList();
        }
        this.extraItems.add(type);
    }

    public boolean haveExtraItems() {
        return this.extraItems != null && !this.extraItems.isEmpty();
    }

    public ArrayList<String> getExtraItems() {
        return this.extraItems;
    }

    public float getExtraItemsWeight() {
        if (!this.haveExtraItems()) {
            return 0.0f;
        }
        float extraWeight = 0.0f;
        for (int i = 0; i < this.extraItems.size(); ++i) {
            InventoryItem item = InventoryItemFactory.CreateItem((String)this.extraItems.get(i));
            if (item == null || !(item.getActualWeight() > 0.0f)) continue;
            extraWeight += item.getActualWeight();
        }
        return extraWeight *= 0.6f;
    }

    public boolean isCustomName() {
        return this.customName;
    }

    public void setCustomName(boolean customName) {
        this.customName = customName;
        this.getModData().rawset((Object)"customName", (Object)String.valueOf(this.name));
    }

    public boolean isFishingLure() {
        return this.scriptItem.fishingLure;
    }

    public void copyConditionModData(InventoryItem other) {
        if (other.hasModData()) {
            KahluaTableIterator it = other.getModData().iterator();
            while (it.advance()) {
                if (!(it.getKey() instanceof String) || !((String)it.getKey()).startsWith("condition:")) continue;
                this.getModData().rawset(it.getKey(), it.getValue());
            }
        }
    }

    public void setConditionFromModData(InventoryItem other) {
        if (other.hasModData()) {
            Object v = other.getModData().rawget((Object)("condition:" + this.getType()));
            if (v instanceof Double) {
                Double doubleValue = (Double)v;
                this.setConditionNoSound((int)Math.round(doubleValue * (double)this.getConditionMax()));
            }
        } else if (!this.hasTag(ItemTag.DONT_INHERIT_CONDITION)) {
            this.setConditionFrom(other);
        }
    }

    public String getBreakSound() {
        return this.breakSound;
    }

    public void setBreakSound(String breakSound) {
        this.breakSound = breakSound;
    }

    public String getPlaceOneSound() {
        return this.scriptItem.getPlaceOneSound();
    }

    public String getPlaceMultipleSound() {
        return this.scriptItem.getPlaceMultipleSound();
    }

    public String getSoundByID(String id) {
        return this.scriptItem.getSoundByID(id);
    }

    public void setBeingFilled(boolean v) {
        this.beingFilled = v;
    }

    public boolean isBeingFilled() {
        return this.beingFilled;
    }

    public String getFillFromDispenserSound() {
        return this.scriptItem.getFillFromDispenserSound();
    }

    public String getFillFromLakeSound() {
        return this.scriptItem.getFillFromLakeSound();
    }

    public String getFillFromTapSound() {
        return this.scriptItem.getFillFromTapSound();
    }

    public String getFillFromToiletSound() {
        return this.scriptItem.getFillFromToiletSound();
    }

    public String getPourLiquidOnGroundSound() {
        if (StringUtils.equalsIgnoreCase((String)this.getPourType(), (String)"Bucket") && this.hasTag(ItemTag.HAS_METAL)) {
            return "PourLiquidOnGroundMetal";
        }
        if (StringUtils.equalsIgnoreCase((String)this.getPourType(), (String)"Pot")) {
            return "PourLiquidOnGroundMetal";
        }
        return "PourLiquidOnGround";
    }

    public boolean isAlcoholic() {
        return this.alcoholic;
    }

    public void setAlcoholic(boolean alcoholic) {
        this.alcoholic = alcoholic;
    }

    public float getAlcoholPower() {
        return this.alcoholPower;
    }

    public void setAlcoholPower(float alcoholPower) {
        this.alcoholPower = alcoholPower;
    }

    public float getBandagePower() {
        return this.bandagePower;
    }

    public void setBandagePower(float bandagePower) {
        this.bandagePower = bandagePower;
    }

    public float getReduceInfectionPower() {
        if (this.burnt) {
            return (int)(this.reduceInfectionPower / 3.0f);
        }
        if (this.age >= (float)this.offAge && this.age < (float)this.offAgeMax) {
            return (int)(this.reduceInfectionPower / 1.3f);
        }
        if (this.age >= (float)this.offAgeMax) {
            return (int)(this.reduceInfectionPower / 2.2f);
        }
        if (this.isCooked()) {
            return this.reduceInfectionPower * 1.3f;
        }
        return this.reduceInfectionPower;
    }

    public void setReduceInfectionPower(float reduceInfectionPower) {
        this.reduceInfectionPower = reduceInfectionPower;
    }

    public final void saveWithSize(ByteBuffer output, boolean net) throws IOException {
        int position1 = output.position();
        output.putInt(0);
        int position2 = output.position();
        this.save(output, net);
        int position3 = output.position();
        output.position(position1);
        output.putInt(position3 - position2);
        output.position(position3);
    }

    public boolean isCustomWeight() {
        return this.customWeight;
    }

    public void setCustomWeight(boolean custom) {
        this.customWeight = custom;
    }

    public float getContentsWeight() {
        Item scriptAmmo;
        if (this.ammoType != null && (scriptAmmo = ScriptManager.instance.FindItem(this.ammoType.getItemKey())) != null) {
            return scriptAmmo.getActualWeight() * (float)this.getCurrentAmmoCount();
        }
        if (this.getFluidContainer() != null) {
            return this.getFluidContainer().getAmount();
        }
        if (this.getWorldItem() != null && this.getWorldItem().hasComponent(ComponentType.FluidContainer)) {
            return this.getWorldItem().getFluidContainer().getAmount();
        }
        return 0.0f;
    }

    public float getHotbarEquippedWeight() {
        if (this.hasTag(ItemTag.LIGHT_WHEN_ATTACHED)) {
            return (this.getActualWeight() + this.getContentsWeight()) * (float)ZomboidGlobals.equippedOrWornEncumbranceMultiplier;
        }
        return (this.getActualWeight() + this.getContentsWeight()) * 0.7f;
    }

    public float getEquippedWeight() {
        return (this.getActualWeight() + this.getContentsWeight()) * (float)ZomboidGlobals.equippedOrWornEncumbranceMultiplier;
    }

    public float getUnequippedWeight() {
        return this.getActualWeight() + this.getContentsWeight();
    }

    public boolean isEquipped() {
        if (this.getContainer() == null) {
            return false;
        }
        IsoObject isoObject = this.getContainer().getParent();
        if (isoObject instanceof IsoGameCharacter) {
            IsoGameCharacter chr = (IsoGameCharacter)isoObject;
            return chr.isEquipped(this);
        }
        isoObject = this.getContainer().getParent();
        if (isoObject instanceof IsoDeadBody) {
            IsoDeadBody deadBody = (IsoDeadBody)isoObject;
            return deadBody.isEquipped(this);
        }
        return false;
    }

    public IsoGameCharacter getUser() {
        if (this.getContainer() != null && this.getContainer().getParent() instanceof IsoGameCharacter && ((IsoGameCharacter)this.getContainer().getParent()).isEquipped(this)) {
            return (IsoGameCharacter)this.getContainer().getParent();
        }
        return null;
    }

    public IsoGameCharacter getOwner() {
        if (this.getContainer() != null && this.getContainer().getParent() instanceof IsoGameCharacter) {
            return (IsoGameCharacter)this.getContainer().getParent();
        }
        return null;
    }

    public int getKeyId() {
        return this.keyId;
    }

    public void setKeyId(int keyId) {
        this.keyId = keyId;
    }

    public boolean isRemoteController() {
        return this.remoteController;
    }

    public void setRemoteController(boolean remoteController) {
        this.remoteController = remoteController;
    }

    public boolean canBeRemote() {
        return this.canBeRemote;
    }

    public void setCanBeRemote(boolean canBeRemote) {
        this.canBeRemote = canBeRemote;
    }

    public int getRemoteControlID() {
        return this.remoteControlId;
    }

    public void setRemoteControlID(int remoteControlId) {
        this.remoteControlId = remoteControlId;
    }

    public int getRemoteRange() {
        return this.remoteRange;
    }

    public void setRemoteRange(int remoteRange) {
        this.remoteRange = remoteRange;
    }

    public String getExplosionSound() {
        return this.explosionSound;
    }

    public void setExplosionSound(String explosionSound) {
        this.explosionSound = explosionSound;
    }

    public String getCountDownSound() {
        return this.countDownSound;
    }

    public void setCountDownSound(String sound) {
        this.countDownSound = sound;
    }

    public float getColorRed() {
        return this.colorRed;
    }

    public void setColorRed(float colorRed) {
        this.colorRed = colorRed;
    }

    public float getColorGreen() {
        return this.colorGreen;
    }

    public void setColorGreen(float colorGreen) {
        this.colorGreen = colorGreen;
    }

    public float getColorBlue() {
        return this.colorBlue;
    }

    public void setColorBlue(float colorBlue) {
        this.colorBlue = colorBlue;
    }

    public String getEvolvedRecipeName() {
        return this.evolvedRecipeName;
    }

    public void setEvolvedRecipeName(String evolvedRecipeName) {
        this.evolvedRecipeName = evolvedRecipeName;
    }

    public float getMetalValue() {
        return this.metalValue;
    }

    public void setMetalValue(float metalValue) {
        this.metalValue = metalValue;
    }

    public float getItemHeat() {
        return this.itemHeat;
    }

    public void setItemHeat(float itemHeat) {
        if (itemHeat > 3.0f) {
            itemHeat = 3.0f;
        }
        if (itemHeat < 0.0f) {
            itemHeat = 0.0f;
        }
        this.itemHeat = itemHeat;
    }

    public float getInvHeat() {
        return 1.0f - this.itemHeat;
    }

    public float getMeltingTime() {
        return this.meltingTime;
    }

    public void setMeltingTime(float meltingTime) {
        if (meltingTime > 100.0f) {
            meltingTime = 100.0f;
        }
        if (meltingTime < 0.0f) {
            meltingTime = 0.0f;
        }
        this.meltingTime = meltingTime;
    }

    public String getWorker() {
        return this.worker;
    }

    public void setWorker(String worker) {
        this.worker = worker;
    }

    public int getID() {
        return this.id;
    }

    public void setID(int itemId) {
        this.id = itemId;
    }

    public boolean isWet() {
        return this.isWet;
    }

    public void setWet(boolean isWet) {
        this.isWet = isWet;
    }

    public float getWetCooldown() {
        return this.wetCooldown;
    }

    public void setWetCooldown(float wetCooldown) {
        this.wetCooldown = wetCooldown;
    }

    public String getItemWhenDry() {
        return this.itemWhenDry;
    }

    public void setItemWhenDry(String itemWhenDry) {
        this.itemWhenDry = itemWhenDry;
    }

    public boolean isFavorite() {
        return this.favorite;
    }

    public void setFavorite(boolean favorite) {
        this.setFavorite(favorite, false);
    }

    public void setFavorite(boolean favorite, boolean isSyncNeeded) {
        this.favorite = favorite;
        if (isSyncNeeded) {
            this.syncItemFields();
        }
    }

    public ArrayList<String> getRequireInHandOrInventory() {
        return this.requireInHandOrInventory;
    }

    public void setRequireInHandOrInventory(ArrayList<String> requireInHandOrInventory) {
        this.requireInHandOrInventory = requireInHandOrInventory;
    }

    public boolean isCustomColor() {
        return this.customColor;
    }

    public void setCustomColor(boolean customColor) {
        this.customColor = customColor;
    }

    public void doBuildingStash() {
        if (this.stashMap != null) {
            if (GameClient.client) {
                INetworkPacket.send((PacketTypes.PacketType)PacketTypes.PacketType.ReadAnnotedMap, (Object[])new Object[]{this.stashMap});
            } else {
                StashSystem.prepareBuildingStash((String)this.stashMap);
            }
        }
    }

    public void setStashMap(String stashMap) {
        this.stashMap = stashMap;
    }

    public String getStashMap() {
        return this.stashMap;
    }

    public int getMechanicType() {
        return this.scriptItem.vehicleType;
    }

    public float getItemCapacity() {
        return this.itemCapacity;
    }

    public void setItemCapacity(float capacity) {
        this.itemCapacity = capacity;
    }

    public int getMaxCapacity() {
        return this.maxCapacity;
    }

    public void setMaxCapacity(int maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    public boolean isConditionAffectsCapacity() {
        return this.scriptItem != null && this.scriptItem.isConditionAffectsCapacity();
    }

    public float getBrakeForce() {
        return this.brakeForce;
    }

    public void setBrakeForce(float brakeForce) {
        this.brakeForce = brakeForce;
    }

    public float getDurability() {
        return this.durability;
    }

    public void setDurability(float durability) {
        this.durability = durability;
    }

    public int getChanceToSpawnDamaged() {
        return this.chanceToSpawnDamaged;
    }

    public void setChanceToSpawnDamaged(int chanceToSpawnDamaged) {
        this.chanceToSpawnDamaged = chanceToSpawnDamaged;
    }

    public float getConditionLowerNormal() {
        return this.conditionLowerNormal;
    }

    public void setConditionLowerNormal(float conditionLowerNormal) {
        this.conditionLowerNormal = conditionLowerNormal;
    }

    public float getConditionLowerOffroad() {
        return this.conditionLowerOffroad;
    }

    public void setConditionLowerOffroad(float conditionLowerOffroad) {
        this.conditionLowerOffroad = conditionLowerOffroad;
    }

    public float getWheelFriction() {
        return this.wheelFriction;
    }

    public void setWheelFriction(float wheelFriction) {
        this.wheelFriction = wheelFriction;
    }

    public float getSuspensionDamping() {
        return this.suspensionDamping;
    }

    public void setSuspensionDamping(float suspensionDamping) {
        this.suspensionDamping = suspensionDamping;
    }

    public float getSuspensionCompression() {
        return this.suspensionCompression;
    }

    public void setSuspensionCompression(float suspensionCompression) {
        this.suspensionCompression = suspensionCompression;
    }

    public void setInfected(boolean infected) {
        this.zombieInfected = infected;
    }

    public boolean isInfected() {
        return this.zombieInfected;
    }

    public float getEngineLoudness() {
        return this.engineLoudness;
    }

    public void setEngineLoudness(float engineLoudness) {
        this.engineLoudness = engineLoudness;
    }

    public String getStaticModel() {
        if (this.getModData().rawget((Object)"staticModel") != null) {
            return (String)this.getModData().rawget((Object)"staticModel");
        }
        if (this.modelIndex != -1 && this.getStaticModelsByIndex() != null) {
            return this.getStaticModelsByIndex().get(this.modelIndex);
        }
        return this.scriptItem.getStaticModel();
    }

    public void setStaticModel(String model) {
        this.getModData().rawset((Object)"staticModel", (Object)model);
    }

    public void setStaticModel(ModelKey model) {
        this.setStaticModel(model.toString());
    }

    public String getStaticModelException() {
        if (this.hasTag(ItemTag.USE_WORLD_STATIC_MODEL)) {
            return this.getWorldStaticModel();
        }
        return this.getStaticModel();
    }

    public ArrayList<String> getIconsForTexture() {
        return this.iconsForTexture;
    }

    public void setIconsForTexture(ArrayList<String> iconsForTexture) {
        this.iconsForTexture = iconsForTexture;
    }

    public float getScore(SurvivorDesc desc) {
        return 0.0f;
    }

    public IsoGameCharacter getPreviousOwner() {
        return this.previousOwner;
    }

    public void setPreviousOwner(IsoGameCharacter previousOwner) {
        this.previousOwner = previousOwner;
    }

    public Item getScriptItem() {
        return this.scriptItem;
    }

    public void setScriptItem(Item scriptItem) {
        this.scriptItem = scriptItem;
    }

    private ItemType getItemType() {
        return this.itemType;
    }

    public void setItemType(ItemType itemType) {
        this.itemType = itemType;
    }

    public boolean isItemType(ItemType itemType) {
        return this.itemType == itemType;
    }

    public ItemContainer getContainer() {
        return this.container;
    }

    public void setContainer(ItemContainer container) {
        this.container = container;
    }

    public ArrayList<BloodClothingType> getBloodClothingType() {
        return this.bloodClothingType;
    }

    public void setBloodClothingType(ArrayList<BloodClothingType> bloodClothingType) {
        this.bloodClothingType = bloodClothingType;
    }

    public void setBlood(BloodBodyPartType bodyPartType, float amount) {
        ItemVisual itemVisual = this.getVisual();
        if (itemVisual != null) {
            itemVisual.setBlood(bodyPartType, amount);
        }
    }

    public float getBlood(BloodBodyPartType bodyPartType) {
        ItemVisual itemVisual = this.getVisual();
        if (itemVisual != null) {
            return itemVisual.getBlood(bodyPartType);
        }
        return 0.0f;
    }

    public void setDirt(BloodBodyPartType bodyPartType, float amount) {
        ItemVisual itemVisual = this.getVisual();
        if (itemVisual != null) {
            itemVisual.setDirt(bodyPartType, amount);
        }
    }

    public float getDirt(BloodBodyPartType bodyPartType) {
        ItemVisual itemVisual = this.getVisual();
        if (itemVisual != null) {
            return itemVisual.getDirt(bodyPartType);
        }
        return 0.0f;
    }

    public String getClothingItemName() {
        return this.scriptItem.clothingItem;
    }

    public int getStashChance() {
        return this.stashChance;
    }

    public void setStashChance(int stashChance) {
        this.stashChance = stashChance;
    }

    public String getEatType() {
        return this.scriptItem.eatType;
    }

    public String getPourType() {
        return this.scriptItem.pourType;
    }

    public boolean isUseWorldItem() {
        return this.scriptItem.useWorldItem;
    }

    public AmmoType getAmmoType() {
        return this.ammoType;
    }

    public void setAmmoType(AmmoType ammoType) {
        this.ammoType = ammoType;
    }

    public int getMaxAmmo() {
        return this.maxAmmo;
    }

    public void setMaxAmmo(int maxAmmoCount) {
        this.maxAmmo = maxAmmoCount;
    }

    public int getCurrentAmmoCount() {
        return this.currentAmmoCount;
    }

    public void setCurrentAmmoCount(int ammo) {
        this.currentAmmoCount = ammo;
    }

    public ArrayList<String> getGunType() {
        return this.gunType;
    }

    /*
     * WARNING - void declaration
     */
    public void setGunType(ArrayList<String> gunType) {
        this.gunType = gunType;
        this.gunTypeDisplayName.clear();
        if (gunType == null) {
            return;
        }
        for (String string : gunType) {
            void var3_3;
            Item scriptItem;
            if (!string.contains(".")) {
                String string2 = this.getModule() + "." + string;
            }
            if ((scriptItem = ScriptManager.instance.getItem((String)var3_3)) == null) continue;
            this.gunTypeDisplayName.add(scriptItem.getDisplayName());
        }
    }

    public boolean hasBlood() {
        if (this instanceof Clothing) {
            if (this.getBloodClothingType() == null || this.getBloodClothingType().isEmpty()) {
                return false;
            }
            ArrayList coveredParts = BloodClothingType.getCoveredParts(this.getBloodClothingType());
            if (coveredParts == null) {
                return false;
            }
            for (int i = 0; i < coveredParts.size(); ++i) {
                if (!(this.getBlood((BloodBodyPartType)coveredParts.get(i)) > 0.0f)) continue;
                return true;
            }
        } else {
            if (this instanceof HandWeapon) {
                return this.getBloodLevel() > 0.0f;
            }
            if (this instanceof InventoryContainer) {
                return this.getBloodLevel() > 0.0f;
            }
        }
        return false;
    }

    public boolean hasDirt() {
        if (this instanceof Clothing || this instanceof InventoryContainer) {
            if (this.getBloodClothingType() == null || this.getBloodClothingType().isEmpty()) {
                return false;
            }
            ArrayList coveredParts = BloodClothingType.getCoveredParts(this.getBloodClothingType());
            if (coveredParts == null) {
                return false;
            }
            for (int i = 0; i < coveredParts.size(); ++i) {
                if (!(this.getDirt((BloodBodyPartType)coveredParts.get(i)) > 0.0f)) continue;
                return true;
            }
        }
        return false;
    }

    public String getAttachmentType() {
        return this.attachmentType;
    }

    public void setAttachmentType(String attachmentType) {
        this.attachmentType = attachmentType;
    }

    public int getAttachedSlot() {
        return this.attachedSlot;
    }

    public void setAttachedSlot(int attachedSlot) {
        this.attachedSlot = attachedSlot;
    }

    public ArrayList<String> getAttachmentsProvided() {
        return this.attachmentsProvided;
    }

    public void setAttachmentsProvided(ArrayList<String> attachmentsProvided) {
        this.attachmentsProvided = attachmentsProvided;
    }

    public String getAttachedSlotType() {
        return this.attachedSlotType;
    }

    public void setAttachedSlotType(String attachedSlotType) {
        this.attachedSlotType = attachedSlotType;
    }

    public String getAttachmentReplacement() {
        return this.attachmentReplacement;
    }

    public void setAttachmentReplacement(String attachementReplacement) {
        this.attachmentReplacement = attachementReplacement;
    }

    public String getAttachedToModel() {
        return this.attachedToModel;
    }

    public void setAttachedToModel(String attachedToModel) {
        this.attachedToModel = attachedToModel;
    }

    public String getFabricType() {
        return this.scriptItem.fabricType;
    }

    public String getStringItemType() {
        Item item = ScriptManager.instance.FindItem(this.getFullType());
        if (item == null || item.isItemType(null)) {
            return "Other";
        }
        if (item.isItemType(ItemType.FOOD)) {
            if (item.cannedFood) {
                return "CannedFood";
            }
            return "Food";
        }
        if ("Ammo".equals(item.getDisplayCategory())) {
            return "Ammo";
        }
        if (item.isItemType(ItemType.WEAPON) && !item.isRanged()) {
            return "MeleeWeapon";
        }
        if (item.isItemType(ItemType.WEAPON_PART) || item.isItemType(ItemType.WEAPON) && item.isRanged() || item.isItemType(ItemType.NORMAL) && item.getAmmoType() != null) {
            return "RangedWeapon";
        }
        if (item.isItemType(ItemType.LITERATURE)) {
            return "Literature";
        }
        if (item.medical) {
            return "Medical";
        }
        if (item.survivalGear) {
            return "SurvivalGear";
        }
        if (item.mechanicsItem) {
            return "Mechanic";
        }
        return "Other";
    }

    public boolean isProtectFromRainWhileEquipped() {
        return this.scriptItem.protectFromRainWhenEquipped;
    }

    public boolean isEquippedNoSprint() {
        return this.scriptItem.equippedNoSprint;
    }

    public ItemBodyLocation getBodyLocation() {
        return this.scriptItem.getBodyLocation();
    }

    public boolean isBodyLocation(ItemBodyLocation itemBodyLocation) {
        return this.scriptItem.getBodyLocation() == itemBodyLocation;
    }

    public String getMakeUpType() {
        return this.scriptItem.makeUpType;
    }

    public boolean isHidden() {
        return this.scriptItem.isHidden();
    }

    public String getConsolidateOption() {
        return this.scriptItem.consolidateOption;
    }

    public ArrayList<String> getClothingItemExtra() {
        return this.scriptItem.clothingItemExtra;
    }

    public ArrayList<String> getClothingItemExtraOption() {
        return this.scriptItem.clothingItemExtraOption;
    }

    public String getWorldStaticItem() {
        if (this.getModData().rawget((Object)"Flatpack") == "true") {
            return "Flatpack";
        }
        if (this.getModData().rawget((Object)"worldStaticModel") != null) {
            return (String)this.getModData().rawget((Object)"worldStaticModel");
        }
        String model = this.tryGetWorldStaticModelByIndex(this.getModelIndex());
        if (model != null) {
            return model;
        }
        return this.scriptItem.worldStaticModel;
    }

    public String getWorldStaticModel() {
        return this.getWorldStaticItem();
    }

    public void setWorldStaticItem(String model) {
        this.getModData().rawset((Object)"worldStaticModel", (Object)model);
    }

    public void setWorldStaticModel(String model) {
        this.setWorldStaticItem(model);
    }

    public void setWorldStaticModel(ModelKey model) {
        this.setWorldStaticItem(model.toString());
    }

    public void setRegistry_id(Item itemscript) {
        if (itemscript.getFullName().equals(this.getFullType())) {
            this.registryId = itemscript.getRegistry_id();
        } else if (Core.debug) {
            WorldDictionary.DebugPrintItem((Item)itemscript);
            throw new RuntimeException("These types should always match");
        }
    }

    public short getRegistry_id() {
        return this.registryId;
    }

    public String getModID() {
        if (this.scriptItem != null && this.scriptItem.getModID() != null) {
            return this.scriptItem.getModID();
        }
        return WorldDictionary.getItemModID((short)this.registryId);
    }

    public String getModName() {
        return WorldDictionary.getModNameFromID((String)this.getModID());
    }

    public boolean isVanilla() {
        if (this.getModID() != null) {
            return this.getModID().equals("pz-vanilla");
        }
        if (Core.debug) {
            WorldDictionary.DebugPrintItem((InventoryItem)this);
            throw new RuntimeException("Item has no modID?");
        }
        return true;
    }

    public short getRecordedMediaIndex() {
        return this.recordedMediaIndex;
    }

    public void setRecordedMediaIndex(short id) {
        this.recordedMediaIndex = id;
        if (this.recordedMediaIndex >= 0) {
            MediaData mediaData = ZomboidRadio.getInstance().getRecordedMedia().getMediaDataFromIndex(this.recordedMediaIndex);
            this.mediaType = (byte)-1;
            if (mediaData != null) {
                this.name = mediaData.getTranslatedItemDisplayName();
                this.mediaType = mediaData.getMediaType();
            } else {
                this.recordedMediaIndex = (short)-1;
            }
        } else {
            this.mediaType = (byte)-1;
            this.name = this.scriptItem.getDisplayName();
        }
    }

    public void setRecordedMediaIndexInteger(int id) {
        this.setRecordedMediaIndex((short)id);
    }

    public boolean isRecordedMedia() {
        return this.recordedMediaIndex >= 0;
    }

    public MediaData getMediaData() {
        if (this.isRecordedMedia()) {
            return ZomboidRadio.getInstance().getRecordedMedia().getMediaDataFromIndex(this.recordedMediaIndex);
        }
        return null;
    }

    public byte getMediaType() {
        return this.mediaType;
    }

    public void setMediaType(byte b) {
        this.mediaType = b;
    }

    public void setRecordedMediaData(MediaData data) {
        if (data != null && data.getIndex() >= 0) {
            this.setRecordedMediaIndex(data.getIndex());
        }
    }

    public void setWorldZRotation(float rot) {
        this.worldZRotation = rot;
    }

    public float getWorldZRotation() {
        return this.worldZRotation;
    }

    public void setWorldYRotation(float rot) {
        this.worldYRotation = rot;
    }

    public float getWorldYRotation() {
        return this.worldYRotation;
    }

    public void setWorldXRotation(float rot) {
        this.worldXRotation = rot;
    }

    public float getWorldXRotation() {
        return this.worldXRotation;
    }

    public void randomizeWorldZRotation() {
        this.worldZRotation = Rand.Next((int)360);
    }

    public void setWorldScale(float scale) {
        this.worldScale = scale;
    }

    public String getLuaCreate() {
        return this.scriptItem.getLuaCreate();
    }

    public boolean isInitialised() {
        return this.isInitialised;
    }

    public void setInitialised(boolean initialised) {
        this.isInitialised = initialised;
    }

    public void initialiseItem() {
        Object functionObject;
        this.setInitialised(true);
        if (this.getLuaCreate() != null && (functionObject = LuaManager.getFunctionObject((String)this.getLuaCreate())) != null) {
            LuaManager.caller.protectedCallVoid(LuaManager.thread, functionObject, (Object)this);
        }
    }

    public String getMilkReplaceItem() {
        return this.scriptItem.milkReplaceItem;
    }

    public int getMaxMilk() {
        return this.scriptItem.maxMilk;
    }

    public boolean isAnimalFeed() {
        return !StringUtils.isNullOrEmpty((String)this.scriptItem.animalFeedType);
    }

    public String getAnimalFeedType() {
        return this.scriptItem.animalFeedType;
    }

    public String getDigType() {
        return this.scriptItem.digType;
    }

    public String getSoundParameter(String parameterName) {
        return this.scriptItem.getSoundParameter(parameterName);
    }

    public boolean isWorn() {
        return this.IsClothing() && this.isWorn();
    }

    public String toString() {
        return this.getFullType() + ":" + super.toString();
    }

    public Texture getTextureColorMask() {
        return this.textureColorMask;
    }

    public Texture getTextureFluidMask() {
        return this.textureFluidMask;
    }

    public void setTextureColorMask(String tex) {
        this.textureColorMask = Texture.trygetTexture((String)tex);
        if (this.textureColorMask == null) {
            this.textureColorMask = Texture.getSharedTexture((String)"media/inventory/Question_On.png");
        }
    }

    public void setTextureFluidMask(String tex) {
        this.textureFluidMask = Texture.trygetTexture((String)tex);
        if (this.textureFluidMask == null) {
            this.textureFluidMask = Texture.getSharedTexture((String)"media/inventory/Question_On.png");
        }
    }

    public IsoGridSquare getSquare() {
        if (this.equipParent != null) {
            return this.equipParent.getSquare();
        }
        return null;
    }

    public GameEntityType getGameEntityType() {
        return GameEntityType.InventoryItem;
    }

    public long getEntityNetID() {
        return this.id;
    }

    public float getX() {
        if (this.equipParent != null) {
            return this.equipParent.getX();
        }
        return Float.MAX_VALUE;
    }

    public float getY() {
        if (this.equipParent != null) {
            return this.equipParent.getY();
        }
        return Float.MAX_VALUE;
    }

    public float getZ() {
        if (this.equipParent != null) {
            return this.equipParent.getZ();
        }
        return Float.MAX_VALUE;
    }

    public boolean isEntityValid() {
        return this.getEquipParent() != null;
    }

    public static boolean RemoveFromContainer(InventoryItem item) {
        ItemContainer container = item.getContainer();
        if (container != null) {
            if (container.getType().equals("floor") && item.getWorldItem() != null && item.getWorldItem().getSquare() != null) {
                item.getWorldItem().getSquare().transmitRemoveItemFromSquare(item.getWorldItem());
                item.getWorldItem().getSquare().getWorldObjects().remove(item.getWorldItem());
                item.getWorldItem().getSquare().getObjects().remove((Object)item.getWorldItem());
                item.setWorldItem(null);
            }
            container.DoRemoveItem(item);
            return true;
        }
        return false;
    }

    public AnimalTracks getAnimalTracks() {
        return this.animalTracks;
    }

    public void setAnimalTracks(AnimalTracks animalTracks) {
        this.animalTracks = animalTracks;
    }

    public void syncItemFields() {
        ItemContainer outer = this.getOutermostContainer();
        if (outer != null && outer.getParent() instanceof IsoPlayer) {
            if (GameClient.client) {
                INetworkPacket.send((PacketTypes.PacketType)PacketTypes.PacketType.SyncItemFields, (Object[])new Object[]{outer.getParent(), this});
            } else if (GameServer.server) {
                INetworkPacket.send((IsoPlayer)((IsoPlayer)outer.getParent()), (PacketTypes.PacketType)PacketTypes.PacketType.SyncItemFields, (Object[])new Object[]{outer.getParent(), this});
            }
        }
        if (GameClient.client && this instanceof InventoryContainer) {
            INetworkPacket.send((PacketTypes.PacketType)PacketTypes.PacketType.SyncItemFields, (Object[])new Object[]{IsoPlayer.getInstance(), this});
        }
        if (GameServer.server && this.getWorldItem() != null) {
            ItemContainer container = new ItemContainer("floor", this.getWorldItem().square, null);
            INetworkPacket.sendToRelative((PacketTypes.PacketType)PacketTypes.PacketType.ItemStats, (float)this.getWorldItem().getSquare().x, (float)this.getWorldItem().getSquare().y, (Object[])new Object[]{container, this});
        }
    }

    public void checkSyncItemFields(boolean b) {
        if (b) {
            this.syncItemFields();
        }
    }

    public String getWithDrainable() {
        return this.scriptItem.getWithDrainable();
    }

    public String getWithoutDrainable() {
        return this.scriptItem.getWithoutDrainable();
    }

    public ArrayList<String> getStaticModelsByIndex() {
        return this.staticModelsByIndex;
    }

    public void setStaticModelsByIndex(ArrayList<String> staticModelsByIndex) {
        this.staticModelsByIndex = staticModelsByIndex;
    }

    public ArrayList<String> getWorldStaticModelsByIndex() {
        return this.worldStaticModelsByIndex;
    }

    public void setWorldStaticModelsByIndex(ArrayList<String> staticModelsByIndex) {
        this.worldStaticModelsByIndex = staticModelsByIndex;
    }

    public String tryGetWorldStaticModelByIndex(int index) {
        ArrayList<String> models = this.getWorldStaticModelsByIndex();
        if (models == null || index < 0 || index >= models.size()) {
            return null;
        }
        return models.get(index);
    }

    public int getModelIndex() {
        return this.modelIndex;
    }

    public void setModelIndex(int index) {
        this.modelIndex = index;
        this.synchWithVisual();
    }

    public float getVisionModifier() {
        return this.scriptItem.getVisionModifier();
    }

    public float getHearingModifier() {
        return this.scriptItem.getHearingModifier();
    }

    public String getWorldObjectSprite() {
        return this.scriptItem.getWorldObjectSprite();
    }

    public float getStrainModifier() {
        return this.scriptItem.getStrainModifier();
    }

    public int getConditionLowerChance() {
        return this.scriptItem.getConditionLowerChance();
    }

    public void setConditionFrom(InventoryItem item) {
        if (item == null) {
            return;
        }
        if (this.hasSharpness() && item.hasSharpness()) {
            this.setSharpness(item.getSharpness());
        }
        if (this.getConditionMax() == item.getConditionMax()) {
            this.setConditionNoSound(item.getCondition());
            return;
        }
        float perc = (float)item.getCondition() / (float)item.getConditionMax();
        this.setConditionNoSound((int)((float)this.getConditionMax() * perc));
        this.setTimesRepaired(item.getTimesRepaired());
    }

    public void setConditionTo(InventoryItem item) {
        if (item == null) {
            return;
        }
        item.setConditionFrom(this);
    }

    public void reduceCondition() {
        this.setCondition(this.getCondition() - 1);
        this.syncItemFields();
    }

    public boolean damageCheck() {
        return this.damageCheck(0, 1.0f);
    }

    public boolean damageCheck(int skill) {
        return this.damageCheck(skill, 1.0f);
    }

    public boolean damageCheck(int skill, float multiplier) {
        return this.damageCheck(skill, multiplier, true);
    }

    public boolean damageCheck(int skill, float multiplier, boolean maintenance) {
        return this.damageCheck(skill, multiplier, maintenance, true);
    }

    public boolean damageCheck(int skill, float multiplier, boolean maintenance, boolean isEquipped) {
        return this.damageCheck(skill, multiplier, maintenance, isEquipped, null);
    }

    public boolean damageCheck(int skill, float multiplier, boolean maintenance, boolean isEquipped, IsoGameCharacter character) {
        multiplier = Math.max(multiplier, 0.0f);
        if (maintenance) {
            skill += this.getMaintenanceMod(isEquipped, character);
        }
        boolean damage = this.sharpnessCheck(skill / 2, multiplier / 2.0f, false, isEquipped);
        if (this.headConditionCheck(skill, multiplier, false, isEquipped)) {
            damage = true;
        }
        if (Rand.NextBool((int)((int)((float)this.getConditionLowerChance() * multiplier + (float)skill)))) {
            this.reduceCondition();
            return true;
        }
        return damage;
    }

    public boolean sharpnessCheck() {
        return this.sharpnessCheck(0);
    }

    public boolean sharpnessCheck(int skill) {
        return this.sharpnessCheck(skill, 1.0f);
    }

    public boolean sharpnessCheck(int skill, float multiplier) {
        return this.sharpnessCheck(skill, multiplier, true);
    }

    public boolean sharpnessCheck(int skill, float multiplier, boolean maintenance) {
        return this.sharpnessCheck(skill, multiplier, maintenance, true);
    }

    public boolean sharpnessCheck(int skill, float multiplier, boolean maintenance, boolean isEquipped) {
        return this.sharpnessCheck(skill, multiplier, maintenance, isEquipped, null);
    }

    private boolean sharpnessCheck(int skill, float multiplier, boolean maintenance, boolean isEquipped, IsoGameCharacter character) {
        if (!this.hasSharpness()) {
            return false;
        }
        multiplier = Math.max(multiplier, 0.0f);
        int mod = 0;
        if (maintenance) {
            mod += this.getMaintenanceMod(isEquipped, character);
        }
        if (Rand.NextBool((int)(2 * (int)((float)this.getConditionLowerChance() * multiplier + (float)(skill + mod))))) {
            this.reduceSharpness();
            return true;
        }
        return false;
    }

    private void reduceSharpness() {
        if (!this.hasSharpness()) {
            return;
        }
        if (this.getSharpness() <= 0.0f) {
            if (this.hasHeadCondition()) {
                this.reduceHeadCondition();
            } else {
                this.reduceCondition();
            }
            return;
        }
        this.setSharpness(this.getSharpness() - this.getSharpnessIncrement());
    }

    public boolean hasSharpness() {
        return this.attrib() != null && this.attrib().getAttribute((AttributeType)Attribute.Sharpness) != null;
    }

    public float getSharpness() {
        if (!this.hasSharpness()) {
            return 0.0f;
        }
        if (this.attrib().getAttribute((AttributeType)Attribute.Sharpness).getFloatValue() > this.getMaxSharpness()) {
            this.applyMaxSharpness();
        }
        return this.attrib().getAttribute((AttributeType)Attribute.Sharpness).getFloatValue();
    }

    public float getMaxSharpness() {
        if (!this.hasSharpness()) {
            return 1.0f;
        }
        if (this.hasHeadCondition()) {
            return (float)this.getHeadCondition() / (float)this.getHeadConditionMax();
        }
        return (float)this.getCondition() / (float)this.getConditionMax();
    }

    public void applyMaxSharpness() {
        if (!this.hasSharpness()) {
            return;
        }
        this.setSharpness(this.getMaxSharpness());
    }

    public float getSharpnessMultiplier() {
        if (!this.hasSharpness()) {
            return 1.0f;
        }
        return (this.attrib().getAttribute((AttributeType)Attribute.Sharpness).getFloatValue() + 1.0f) / 2.0f;
    }

    public void setSharpness(float value) {
        if (!this.hasSharpness()) {
            return;
        }
        float max = this.getMaxSharpness();
        if (value > max) {
            value = max;
        }
        if (value < 0.0f) {
            value = 0.0f;
        }
        if (value > 1.0f) {
            value = 1.0f;
        }
        String val = String.valueOf(value);
        this.attrib().getAttribute((AttributeType)Attribute.Sharpness).setValueFromScriptString(val);
    }

    public void setSharpnessFrom(InventoryItem item) {
        if (!this.hasSharpness() || !item.hasSharpness()) {
            return;
        }
        this.setSharpness(item.getSharpness());
    }

    public float getSharpnessIncrement() {
        if (!this.hasSharpness()) {
            return 0.0f;
        }
        return 1.0f / (float)this.getConditionMax();
    }

    public boolean isDamaged() {
        return this.getCondition() < this.getConditionMax();
    }

    public boolean isDull() {
        return this.hasSharpness() && this.getSharpness() <= this.getMaxSharpness() / 3.0f;
    }

    public int getMaintenanceMod() {
        return this.getMaintenanceMod(true);
    }

    public int getMaintenanceMod(boolean isEquipped) {
        return this.getMaintenanceMod(isEquipped, null);
    }

    public int getMaintenanceMod(IsoGameCharacter character) {
        return this.getMaintenanceMod(false, character);
    }

    public int getMaintenanceMod(boolean isEquipped, IsoGameCharacter character) {
        if (isEquipped && !this.isEquipped()) {
            return 0;
        }
        if (isEquipped && character == null) {
            character = this.getUser();
        } else if (character == null) {
            character = this.getOwner();
        }
        if (character == null) {
            return 0;
        }
        int level = character.getPerkLevel(PerkFactory.Perks.Maintenance);
        if (this instanceof HandWeapon) {
            level += character.getWeaponLevel((HandWeapon)this) / 2;
        }
        return level;
    }

    public int getWeaponLevel() {
        InventoryItem inventoryItem;
        if (!this.isEquipped() || !((inventoryItem = this) instanceof HandWeapon)) {
            return 0;
        }
        HandWeapon weapon = (HandWeapon)inventoryItem;
        WeaponType weaponType = WeaponType.getWeaponType((HandWeapon)((HandWeapon)this));
        int level = -1;
        if (weaponType != null && weaponType != WeaponType.UNARMED) {
            if (weapon.isOfWeaponCategory(WeaponCategory.AXE)) {
                level = this.getUser().getPerkLevel(PerkFactory.Perks.Axe);
            }
            if (weapon.isOfWeaponCategory(WeaponCategory.SPEAR)) {
                level += this.getUser().getPerkLevel(PerkFactory.Perks.Spear);
            }
            if (weapon.isOfWeaponCategory(WeaponCategory.SMALL_BLADE)) {
                level += this.getUser().getPerkLevel(PerkFactory.Perks.SmallBlade);
            }
            if (weapon.isOfWeaponCategory(WeaponCategory.LONG_BLADE)) {
                level += this.getUser().getPerkLevel(PerkFactory.Perks.LongBlade);
            }
            if (weapon.isOfWeaponCategory(WeaponCategory.BLUNT)) {
                level += this.getUser().getPerkLevel(PerkFactory.Perks.Blunt);
            }
            if (weapon.isOfWeaponCategory(WeaponCategory.SMALL_BLUNT)) {
                level += this.getUser().getPerkLevel(PerkFactory.Perks.SmallBlunt);
            }
        }
        if (level > 10) {
            level = 10;
        }
        if (level == -1) {
            return 0;
        }
        return level;
    }

    public boolean headConditionCheck() {
        return this.headConditionCheck(0, 1.0f);
    }

    public boolean headConditionCheck(int skill) {
        return this.headConditionCheck(skill, 1.0f);
    }

    public boolean headConditionCheck(int skill, float multiplier) {
        return this.headConditionCheck(skill, multiplier, true);
    }

    public boolean headConditionCheck(int skill, float multiplier, boolean maintenance) {
        return this.headConditionCheck(skill, multiplier, maintenance, true);
    }

    public boolean headConditionCheck(int skill, float multiplier, boolean maintenance, boolean isEquipped) {
        return this.headConditionCheck(skill, multiplier, maintenance, isEquipped, null);
    }

    private boolean headConditionCheck(int skill, float multiplier, boolean maintenance, boolean isEquipped, IsoGameCharacter character) {
        if (!this.hasHeadCondition()) {
            return false;
        }
        multiplier = Math.max(multiplier, 0.0f);
        int mod = 0;
        if (maintenance) {
            mod += this.getMaintenanceMod(isEquipped, character);
        }
        if (Rand.NextBool((int)((int)((float)this.getHeadConditionLowerChance() * multiplier + (float)(skill + mod))))) {
            this.reduceHeadCondition();
            return true;
        }
        return false;
    }

    public int getHeadConditionLowerChance() {
        return (int)((float)this.getConditionLowerChance() * this.getHeadConditionLowerChanceMultiplier());
    }

    public float getHeadConditionLowerChanceMultiplier() {
        return this.scriptItem.getHeadConditionLowerChanceMultiplier();
    }

    public void reduceHeadCondition() {
        if (!this.hasHeadCondition()) {
            return;
        }
        this.setHeadCondition(this.getHeadCondition() - 1);
    }

    public boolean hasHeadCondition() {
        return this.attrib() != null && this.attrib().getAttribute((AttributeType)Attribute.HeadCondition) != null;
    }

    public int getHeadCondition() {
        if (!this.hasHeadCondition()) {
            return 0;
        }
        return this.attrib().getAttribute((AttributeType)Attribute.HeadCondition).getIntValue();
    }

    public int getHeadConditionMax() {
        if (!this.hasHeadCondition()) {
            return 0;
        }
        if (this.attrib() != null && this.attrib().getAttribute((AttributeType)Attribute.HeadConditionMax) != null) {
            return this.attrib().get(Attribute.HeadConditionMax);
        }
        return this.getConditionMax();
    }

    public void setHeadCondition(int value) {
        int max;
        if (!this.hasHeadCondition()) {
            return;
        }
        if (value < 0) {
            value = 0;
        }
        if (value > (max = this.getHeadConditionMax())) {
            value = max;
        }
        this.attrib().set(Attribute.HeadCondition, value);
        if (this.getHeadCondition() <= 0) {
            this.setCondition(0);
        }
    }

    public void setHeadConditionFromCondition(InventoryItem item) {
        if (item == null) {
            return;
        }
        if (!this.hasHeadCondition()) {
            return;
        }
        if (this.getHeadConditionMax() == item.getConditionMax()) {
            this.setHeadCondition(item.getCondition());
            if (this.hasSharpness() && item.hasSharpness()) {
                this.setSharpness(item.getSharpness());
            }
            return;
        }
        float perc = (float)item.getCondition() / (float)item.getConditionMax();
        this.setHeadCondition((int)((float)this.getHeadConditionMax() * perc));
        if (this.hasSharpness() && item.hasSharpness()) {
            this.setSharpness(item.getSharpness());
        }
    }

    public void setConditionFromHeadCondition(InventoryItem item) {
        if (item == null) {
            return;
        }
        if (!item.hasHeadCondition()) {
            return;
        }
        if (this.getConditionMax() == item.getHeadConditionMax()) {
            this.setConditionNoSound(item.getHeadCondition());
            if (this.hasSharpness() && item.hasSharpness()) {
                this.setSharpness(item.getSharpness());
            }
            return;
        }
        float perc = (float)item.getHeadCondition() / (float)item.getHeadConditionMax();
        this.setConditionNoSound((int)((float)this.getConditionMax() * perc));
        if (this.hasSharpness() && item.hasSharpness()) {
            this.setSharpness(item.getSharpness());
        }
    }

    public boolean hasQuality() {
        return this.attrib() != null && this.attrib().getAttribute((AttributeType)Attribute.Quality) != null;
    }

    public int getQuality() {
        if (!this.hasQuality()) {
            return 0;
        }
        return this.attrib().getAttribute((AttributeType)Attribute.Quality).getIntValue();
    }

    public void setQuality(int value) {
        if (!this.hasQuality()) {
            return;
        }
        if (value < -50) {
            value = -50;
        }
        if (value > 50) {
            value = 50;
        }
        String val = String.valueOf(value);
        this.attrib().getAttribute((AttributeType)Attribute.Quality).setValueFromScriptString(val);
    }

    public String getOnBreak() {
        return this.scriptItem.getOnBreak();
    }

    public void onBreak() {
        IsoObject isoObject;
        Object functionObj = LuaManager.getFunctionObject((String)this.getOnBreak());
        IsoGameCharacter user = null;
        if (this.container != null && (isoObject = this.container.parent) instanceof IsoGameCharacter) {
            IsoGameCharacter isoGameCharacter;
            user = isoGameCharacter = (IsoGameCharacter)isoObject;
        }
        if (functionObj != null) {
            LuaManager.caller.pcallvoid(LuaManager.thread, functionObj, (Object)this, user);
        }
    }

    public float getBloodLevelAdjustedLow() {
        if (this instanceof Clothing) {
            return this.getBloodLevel() / 100.0f;
        }
        return this.getBloodLevel();
    }

    public float getBloodLevelAdjustedHigh() {
        if (this instanceof Clothing) {
            return this.getBloodLevel();
        }
        return this.getBloodLevel() * 100.0f;
    }

    public float getBloodLevel() {
        return 0.0f;
    }

    public void setBloodLevel(float level) {
    }

    public void copyBloodLevelFrom(InventoryItem item) {
        this.setBloodLevel(item.getBloodLevel());
    }

    public boolean isBloody() {
        return this.getBloodLevel() > 0.25f;
    }

    public String getDamagedSound() {
        if (this.scriptItem == null) {
            return null;
        }
        return this.scriptItem.getDamagedSound();
    }

    public String getBulletHitArmourSound() {
        if (this.scriptItem == null) {
            return null;
        }
        return this.scriptItem.getBulletHitArmourSound();
    }

    public String getWeaponHitArmourSound() {
        if (this.scriptItem == null) {
            return null;
        }
        return this.scriptItem.getWeaponHitArmourSound();
    }

    public String getShoutType() {
        if (this.scriptItem == null) {
            return null;
        }
        return this.scriptItem.getShoutType();
    }

    public float getShoutMultiplier() {
        if (this.scriptItem == null) {
            return 1.0f;
        }
        return this.scriptItem.getShoutMultiplier();
    }

    public int getEatTime() {
        if (this.scriptItem == null) {
            return 0;
        }
        return this.scriptItem.getEatTime();
    }

    public boolean isVisualAid() {
        return this.scriptItem.isVisualAid();
    }

    public float getDiscomfortModifier() {
        return this.scriptItem.getDiscomfortModifier();
    }

    public boolean hasMetal() {
        return this.getMetalValue() > 0.0f || this.hasTag(ItemTag.HAS_METAL);
    }

    public float getFireFuelRatio() {
        return this.scriptItem.getFireFuelRatio();
    }

    public float getWetness() {
        return 0.0f;
    }

    public boolean isMemento() {
        return this.hasTag(ItemTag.IS_MEMENTO) || Objects.equals(this.getDisplayCategory(), "Memento");
    }

    public void nameAfterDescriptor(SurvivorDesc desc) {
        if (desc == null) {
            return;
        }
        String name = this.scriptItem.getDisplayName();
        name = Translator.getText((String)name, (Object[])new Object[0]);
        this.setName(name + ": " + desc.getForename() + " " + desc.getSurname());
    }

    public void monogramAfterDescriptor(SurvivorDesc desc) {
        if (desc == null) {
            return;
        }
        String name = this.scriptItem.getDisplayName();
        name = Translator.getText((String)name, (Object[])new Object[0]);
        this.setName(name + ": " + desc.getForename().charAt(0) + desc.getSurname().charAt(0));
    }

    public String getLootType() {
        return ItemPickerJava.getLootType((Item)this.scriptItem);
    }

    public boolean getIsCraftingConsumed() {
        return this.isCraftingConsumed;
    }

    public void setIsCraftingConsumed(boolean craftingConsumed) {
        this.isCraftingConsumed = craftingConsumed;
    }

    public void OnAddedToContainer(ItemContainer container) {
    }

    public void OnBeforeRemoveFromContainer(ItemContainer container) {
    }

    public IsoDeadBody getDeadBodyObject() {
        return this.deadBodyObject;
    }

    public boolean isPureWater(boolean includeTainted) {
        FluidContainer fluidContainer = this.getFluidContainerFromSelfOrWorldItem();
        if (fluidContainer != null && !fluidContainer.isEmpty()) {
            if (fluidContainer.isPureFluid(Fluid.Water)) {
                return true;
            }
            if (includeTainted && fluidContainer.isPureFluid(Fluid.TaintedWater)) {
                return true;
            }
        }
        return false;
    }

    public void copyClothing(InventoryItem otherItem) {
        if (this.getClothingItem() == null || otherItem.getClothingItem() == null) {
            return;
        }
        Object functionObj = LuaManager.getFunctionObject((String)"copyClothingItem");
        if (functionObj != null) {
            LuaManager.caller.pcallvoid(LuaManager.thread, functionObj, (Object)otherItem, (Object)this);
        }
    }

    public void inheritFoodAgeFrom(InventoryItem otherFood) {
    }

    public void inheritOlderFoodAge(InventoryItem otherFood) {
    }

    public boolean isFood() {
        return false;
    }

    public void unsealIfNotFull() {
        if (this.getFluidContainer() == null) {
            return;
        }
        this.getFluidContainer().unsealIfNotFull();
    }

    public void randomizeCondition() {
        if (this instanceof HandWeapon && ((HandWeapon)this).getPhysicsObject() == null) {
            return;
        }
        int con = Math.max(1, Rand.Next((int)(this.getConditionMax() + 1)));
        this.setCondition(con, false);
    }

    public void randomizeGeneralCondition() {
        this.randomizeCondition();
        this.randomizeHeadCondition();
        this.randomizeSharpness();
    }

    public void randomizeHeadCondition() {
        if (!this.hasHeadCondition()) {
            return;
        }
        int con = Math.max(1, Rand.Next((int)(this.getConditionMax() + 1)));
        this.setCondition(con, false);
    }

    public void randomizeSharpness() {
        if (!this.hasSharpness()) {
            return;
        }
        this.setSharpness(Rand.Next((float)0.0f, (float)this.getMaxSharpness()));
    }

    public FluidContainer getFluidContainerFromSelfOrWorldItem() {
        FluidContainer fluidContainer = this.getFluidContainer();
        if (fluidContainer == null && this.getWorldItem() != null) {
            fluidContainer = this.getWorldItem().getFluidContainer();
        }
        return fluidContainer;
    }

    public boolean isEmptyOfFluid() {
        if (this.getFluidContainer() == null) {
            return false;
        }
        return this.getFluidContainer().isEmpty();
    }

    public boolean isFullOfFluid() {
        if (this.getFluidContainer() == null) {
            return false;
        }
        return this.getFluidContainer().isFull();
    }

    public boolean isFluidContainer() {
        return this.getFluidContainer() != null;
    }

    public boolean isSpice() {
        return this.scriptItem.isSpice();
    }

    public boolean isKeyRing() {
        if (this.scriptItem.isItemType(ItemType.KEY_RING)) {
            return true;
        }
        return this.hasTag(ItemTag.KEY_RING) && this instanceof InventoryContainer;
    }

    public boolean isFakeEquipped(IsoGameCharacter character) {
        if (!this.isInPlayerInventory()) {
            return false;
        }
        if (character == null || character.getInventory() == null || !character.getInventory().contains(this)) {
            return false;
        }
        if (this.getOutermostContainer() != this.getContainer()) {
            return false;
        }
        return this.isKeyRing();
    }

    public boolean isFakeEquipped() {
        if (!this.isInPlayerInventory()) {
            return false;
        }
        if (this.getOutermostContainer() != this.getContainer()) {
            return false;
        }
        return this.isKeyRing();
    }

    public String getItemAfterCleaning() {
        return this.scriptItem.getItemAfterCleaning();
    }

    public ArrayList<String> getResearchableRecipes() {
        return this.scriptItem.getResearchableRecipes();
    }

    public ArrayList<String> getResearchableRecipes(IsoGameCharacter chr) {
        return this.scriptItem.getResearchableRecipes(chr, true);
    }

    public boolean hasResearchableRecipes() {
        return this.scriptItem.hasResearchableRecipes();
    }

    public void researchRecipes(IsoGameCharacter character) {
        if (this.scriptItem != null) {
            this.scriptItem.researchRecipes(character);
        }
    }

    public boolean hasOrigin() {
        return this.attrib() != null && this.attrib().getAttribute((AttributeType)Attribute.OriginX) != null && this.attrib().getAttribute((AttributeType)Attribute.OriginY) != null && (this.attrib().getAttribute((AttributeType)Attribute.OriginX).getIntValue() != 0 || this.attrib().getAttribute((AttributeType)Attribute.OriginY).getIntValue() != 0);
    }

    public boolean canHaveOrigin() {
        return this.attrib() != null && this.attrib().getAttribute((AttributeType)Attribute.OriginX) != null && this.attrib().getAttribute((AttributeType)Attribute.OriginY) != null;
    }

    public boolean setOrigin(IsoGridSquare sq) {
        if (sq == null || !this.canHaveOrigin()) {
            return false;
        }
        return this.setOrigin(sq.getX(), sq.getY(), sq.getZ());
    }

    public boolean setOrigin(int x, int y) {
        return this.setOrigin(x, y, 0);
    }

    public boolean setOrigin(int x, int y, int z) {
        if (!this.canHaveOrigin()) {
            return false;
        }
        this.setOriginX(x);
        this.setOriginY(y);
        this.setOriginZ(z);
        return true;
    }

    public void setOriginX(int value) {
        if (!this.canHaveOrigin()) {
            return;
        }
        String val = String.valueOf(value);
        this.attrib().getAttribute((AttributeType)Attribute.OriginX).setValueFromScriptString(val);
    }

    public void setOriginY(int value) {
        if (!this.canHaveOrigin()) {
            return;
        }
        String val = String.valueOf(value);
        this.attrib().getAttribute((AttributeType)Attribute.OriginY).setValueFromScriptString(val);
    }

    public void setOriginZ(int value) {
        if (!this.canHaveOrigin()) {
            return;
        }
        String val = String.valueOf(value);
        this.attrib().getAttribute((AttributeType)Attribute.OriginZ).setValueFromScriptString(val);
    }

    public int getOriginX() {
        if (!this.hasOrigin()) {
            return 0;
        }
        return this.attrib().getAttribute((AttributeType)Attribute.OriginX).getIntValue();
    }

    public int getOriginY() {
        if (!this.hasOrigin()) {
            return 0;
        }
        return this.attrib().getAttribute((AttributeType)Attribute.OriginY).getIntValue();
    }

    public int getOriginZ() {
        if (!this.hasOrigin()) {
            return 0;
        }
        return this.attrib().getAttribute((AttributeType)Attribute.OriginZ).getIntValue();
    }

    public ItemBodyLocation canBeEquipped() {
        return null;
    }

    public IsoPlayer getPlayer() {
        ItemContainer outer = this.getOutermostContainer();
        if (outer != null && outer.getParent() != null && outer.getParent() instanceof IsoPlayer) {
            return (IsoPlayer)outer.getParent();
        }
        if (this.getOwner() != null && this.getOwner() instanceof IsoPlayer) {
            return (IsoPlayer)this.getOwner();
        }
        return null;
    }

    public float getWorldAlpha() {
        return this.worldAlpha;
    }

    public void setWorldAlpha(float worldAlpha) {
        this.worldAlpha = worldAlpha;
    }

    public void Remove() {
        ItemUser.RemoveItem((InventoryItem)this);
    }

    public void SynchSpawn() {
        if (this.getContainer() != null) {
            LuaManager.GlobalObject.sendAddItemToContainer((ItemContainer)this.getContainer(), (InventoryItem)this);
        }
        if (this.getWorldItem() != null) {
            this.getWorldItem().transmitCompleteItemToClients();
        }
    }

    public boolean isFavouriteRecipeInput(IsoPlayer player) {
        if (this.scriptItem == null) {
            return false;
        }
        return this.scriptItem.isFavouriteRecipeInput(player);
    }

    public void copyConditionStatesFrom(InventoryItem otherItem) {
        if (otherItem == null) {
            return;
        }
        if (this.getClothingItem() != null && otherItem.getClothingItem() != null) {
            this.copyClothing(otherItem);
            return;
        }
        this.copyConditionModData(otherItem);
        this.setConditionFrom(otherItem);
        this.setHaveBeenRepaired(otherItem.getHaveBeenRepaired());
        if (this.hasHeadCondition()) {
            this.setTimesHeadRepaired(otherItem.getTimesHeadRepaired());
            this.setHeadConditionFromCondition(otherItem);
        }
        if (this.hasSharpness()) {
            this.setSharpnessFrom(otherItem);
        }
        this.setFavorite(otherItem.isFavorite());
        this.copyBloodLevelFrom(otherItem);
        InventoryItem inventoryItem = this;
        if (inventoryItem instanceof DrainableComboItem) {
            DrainableComboItem thisDrainable = (DrainableComboItem)inventoryItem;
            if (otherItem instanceof DrainableComboItem) {
                DrainableComboItem thatDrainable = (DrainableComboItem)otherItem;
                thisDrainable.setUsedDelta(thatDrainable.getCurrentUsesFloat());
            }
        }
    }

    public String getFileName() {
        if (this.scriptItem == null) {
            return null;
        }
        return this.scriptItem.getFileName();
    }

    public void setDoingExtendedPlacement(boolean enable) {
        this.doingExtendedPlacement = enable;
    }

    public boolean isDoingExtendedPlacement() {
        return this.doingExtendedPlacement;
    }

    public boolean isNoRecipes(IsoPlayer player) {
        if (player == null) {
            return false;
        }
        String pinnedString = InventoryItem.getNoRecipesModDataString();
        Object noRecipeString = this.getModData().rawget((Object)pinnedString);
        return noRecipeString != null && noRecipeString.equals(player.getFullName());
    }

    public void setNoRecipes(IsoPlayer player, Boolean noCrafting) {
        String noCraftingString = InventoryItem.getNoRecipesModDataString();
        if (noCrafting.booleanValue()) {
            this.getModData().rawset((Object)noCraftingString, (Object)player.getFullName());
        } else {
            this.getModData().rawset((Object)noCraftingString, (Object)false);
        }
        this.syncItemFields();
    }

    public static String getNoRecipesModDataString() {
        return "itemNoRecipes";
    }

    public boolean isUnwanted(IsoPlayer player) {
        if (this.isRecordedMedia()) {
            return player.isUnwanted(this.getMediaData().getId());
        }
        if (this.getModData().rawget((Object)"literatureTitle") != null) {
            return player.isUnwanted(this.getModData().rawget((Object)"literatureTitle").toString());
        }
        if (this.getModData().rawget((Object)"printMedia") != null) {
            return player.isUnwanted(this.getModData().rawget((Object)"printMedia").toString());
        }
        if (this.getModData().rawget((Object)"collectibleKey") != null) {
            return player.isUnwanted(this.getModData().rawget((Object)"collectibleKey").toString());
        }
        return this.scriptItem.isUnwanted(player);
    }

    public void setUnwanted(IsoPlayer player, boolean unwanted) {
        if (this.isRecordedMedia()) {
            player.setUnwanted(this.getMediaData().getId(), Boolean.valueOf(unwanted));
            return;
        }
        if (this.getModData().rawget((Object)"literatureTitle") != null) {
            player.setUnwanted(this.getModData().rawget((Object)"literatureTitle").toString(), Boolean.valueOf(unwanted));
            return;
        }
        if (this.getModData().rawget((Object)"printMedia") != null) {
            player.setUnwanted(this.getModData().rawget((Object)"printMedia").toString(), Boolean.valueOf(unwanted));
            return;
        }
        if (this.getModData().rawget((Object)"collectibleKey") != null) {
            player.setUnwanted(this.getModData().rawget((Object)"collectibleKey").toString(), Boolean.valueOf(unwanted));
            return;
        }
        if (this.scriptItem == null) {
            return;
        }
        this.scriptItem.setUnwanted(player, unwanted);
    }

    public InventoryItem emptyLiquid() {
        if (this.getFluidContainer() != null) {
            this.getFluidContainer().Empty();
        }
        return this;
    }

    public String getOpeningRecipe() {
        return this.scriptItem.getOpeningRecipe();
    }

    public String getDoubleClickRecipe() {
        return this.scriptItem.getDoubleClickRecipe();
    }

    public boolean isSealed() {
        return this.getName().contains(Translator.getFluidText((String)"Fluid_Sealed"));
    }

    public boolean hasBeenSeen(IsoPlayer player) {
        if (!this.isRecordedMedia()) {
            return false;
        }
        if (this.scriptItem.getRecordedMediaCat() == null) {
            return false;
        }
        if (!Objects.equals(this.scriptItem.getRecordedMediaCat(), "Retail-VHS") && !Objects.equals(this.scriptItem.getRecordedMediaCat(), "Home-VHS")) {
            return false;
        }
        return LuaManager.GlobalObject.getZomboidRadio().getRecordedMedia().hasListenedToAll(player, this.getMediaData());
    }

    public boolean hasBeenHeard(IsoPlayer player) {
        if (!this.isRecordedMedia()) {
            return false;
        }
        if (this.scriptItem.getRecordedMediaCat() == null) {
            return false;
        }
        if (Objects.equals(this.scriptItem.getRecordedMediaCat(), "Retail-VHS") || Objects.equals(this.scriptItem.getRecordedMediaCat(), "Home-VHS")) {
            return false;
        }
        return LuaManager.GlobalObject.getZomboidRadio().getRecordedMedia().hasListenedToAll(player, this.getMediaData());
    }

    public String getReplaceOnExtinguish() {
        if (this.scriptItem == null) {
            return null;
        }
        return this.scriptItem.getReplaceOnExtinguish();
    }

    public InventoryItem getExtinguishedItem() {
        if (this.getReplaceOnExtinguish() == null) {
            return null;
        }
        InventoryItem newItem = LuaManager.GlobalObject.instanceItem((String)this.getReplaceOnExtinguish());
        if (newItem == null) {
            return null;
        }
        newItem.copyConditionStatesFrom(this);
        return newItem;
    }

    public boolean isSharpenable() {
        return this.hasTag(ItemTag.SHARPENABLE) && this.hasSharpness() && this.getSharpness() < this.getMaxSharpness();
    }

    public String getGunTypeString() {
        return this.gunTypeDisplayName.toString().replaceAll("\\[", "").replaceAll("\\]", "");
    }
}
