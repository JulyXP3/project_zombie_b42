/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 *  se.krka.kahlua.vm.KahluaTable
 *  zombie.GameTime
 *  zombie.GameWindow
 *  zombie.IndieGL
 *  zombie.Lua.LuaEventManager
 *  zombie.Lua.LuaManager
 *  zombie.Lua.LuaManager$GlobalObject
 *  zombie.Lua.MapObjects
 *  zombie.MapCollisionData
 *  zombie.SandboxOptions
 *  zombie.SoundManager
 *  zombie.UsedFromLua
 *  zombie.VirtualZombieManager
 *  zombie.ZombieSpawnRecorder
 *  zombie.audio.BaseSoundEmitter
 *  zombie.characters.IsoGameCharacter
 *  zombie.characters.IsoGameCharacter$LightInfo
 *  zombie.characters.IsoGameCharacter$TorchInfo
 *  zombie.characters.IsoPlayer
 *  zombie.characters.IsoSurvivor
 *  zombie.characters.IsoZombie
 *  zombie.characters.VisibilityData
 *  zombie.characters.animals.AnimalSoundState
 *  zombie.characters.animals.IsoAnimal
 *  zombie.characters.animals.datas.AnimalBreed$Sound
 *  zombie.core.Color
 *  zombie.core.Core
 *  zombie.core.PerformanceSettings
 *  zombie.core.SceneShaderStore
 *  zombie.core.SpriteRenderer
 *  zombie.core.SpriteRenderer$WallShaderTexRender
 *  zombie.core.logger.ExceptionLogger
 *  zombie.core.math.PZMath
 *  zombie.core.network.ByteBufferWriter
 *  zombie.core.opengl.Shader
 *  zombie.core.opengl.ShaderUniformSetter
 *  zombie.core.profiling.AbstractPerformanceProfileProbe
 *  zombie.core.properties.IsoObjectChange
 *  zombie.core.properties.IsoPropertyType
 *  zombie.core.properties.PropertyContainer
 *  zombie.core.random.Rand
 *  zombie.core.textures.ColorInfo
 *  zombie.core.textures.Texture
 *  zombie.core.textures.TextureDraw
 *  zombie.debug.DebugLog
 *  zombie.debug.DebugOptions
 *  zombie.debug.DebugType
 *  zombie.debug.LogSeverity
 *  zombie.entity.ComponentType
 *  zombie.entity.GameEntityFactory
 *  zombie.entity.GameEntityManager
 *  zombie.erosion.ErosionData$Square
 *  zombie.erosion.categories.ErosionCategory$Data
 *  zombie.globalObjects.CGlobalObjectSystem
 *  zombie.globalObjects.CGlobalObjects
 *  zombie.globalObjects.GlobalObject
 *  zombie.globalObjects.SGlobalObjectSystem
 *  zombie.globalObjects.SGlobalObjects
 *  zombie.inventory.InventoryItemFactory
 *  zombie.inventory.types.AnimalInventoryItem
 *  zombie.inventory.types.Food
 *  zombie.inventory.types.HandWeapon
 *  zombie.iso.BentFences
 *  zombie.iso.BuildingDef
 *  zombie.iso.CellLoader
 *  zombie.iso.FishSplashSoundManager
 *  zombie.iso.IsoButcherHook
 *  zombie.iso.IsoCamera
 *  zombie.iso.IsoDepthHelper
 *  zombie.iso.IsoDepthHelper$Results
 *  zombie.iso.IsoDirections
 *  zombie.iso.IsoFloorBloodSplat
 *  zombie.iso.IsoGridOcclusionData
 *  zombie.iso.IsoGridSquare$1
 *  zombie.iso.IsoGridSquare$CircleStencilShader
 *  zombie.iso.IsoGridSquare$CutawayNoDepthShader
 *  zombie.iso.IsoGridSquare$GetSquare
 *  zombie.iso.IsoGridSquare$ILighting
 *  zombie.iso.IsoGridSquare$Lighting
 *  zombie.iso.IsoGridSquare$WaterSplashData
 *  zombie.iso.IsoGridSquare$s_performance
 *  zombie.iso.IsoGridSquareCollisionData
 *  zombie.iso.IsoObject$VisionResult
 *  zombie.iso.IsoObjectUtils
 *  zombie.iso.IsoPuddles
 *  zombie.iso.IsoPuddlesCompute
 *  zombie.iso.IsoPuddlesGeometry
 *  zombie.iso.IsoPushableObject
 *  zombie.iso.IsoUtils
 *  zombie.iso.IsoWallBloodSplat
 *  zombie.iso.IsoWater
 *  zombie.iso.IsoWaterGeometry
 *  zombie.iso.IsoWorld
 *  zombie.iso.LightingJNI
 *  zombie.iso.LightingJNI$JNILighting
 *  zombie.iso.LosUtil
 *  zombie.iso.LosUtil$TestResults
 *  zombie.iso.RoomDef
 *  zombie.iso.SliceY
 *  zombie.iso.SpriteDetails.IsoFlagType
 *  zombie.iso.SpriteDetails.IsoObjectType
 *  zombie.iso.Vector2
 *  zombie.iso.areas.DesignationZone
 *  zombie.iso.areas.DesignationZoneAnimal
 *  zombie.iso.areas.IsoBuilding
 *  zombie.iso.areas.IsoRoom
 *  zombie.iso.areas.NonPvpZone
 *  zombie.iso.areas.SafeHouse
 *  zombie.iso.areas.isoregion.IsoRegions
 *  zombie.iso.areas.isoregion.regions.IWorldRegion
 *  zombie.iso.areas.isoregion.regions.IsoWorldRegion
 *  zombie.iso.fboRenderChunk.FBORenderCell
 *  zombie.iso.fboRenderChunk.FBORenderChunk
 *  zombie.iso.fboRenderChunk.FBORenderChunkManager
 *  zombie.iso.fboRenderChunk.ObjectRenderLayer
 *  zombie.iso.objects.IsoAnimalTrack
 *  zombie.iso.objects.IsoBarbecue
 *  zombie.iso.objects.IsoBarricade
 *  zombie.iso.objects.IsoBrokenGlass
 *  zombie.iso.objects.IsoCarBatteryCharger
 *  zombie.iso.objects.IsoCompost
 *  zombie.iso.objects.IsoCurtain
 *  zombie.iso.objects.IsoDeadBody
 *  zombie.iso.objects.IsoDoor
 *  zombie.iso.objects.IsoFire
 *  zombie.iso.objects.IsoFireManager
 *  zombie.iso.objects.IsoFireplace
 *  zombie.iso.objects.IsoGenerator
 *  zombie.iso.objects.IsoHutch
 *  zombie.iso.objects.IsoLightSwitch
 *  zombie.iso.objects.IsoMannequin
 *  zombie.iso.objects.IsoRainSplash
 *  zombie.iso.objects.IsoRaindrop
 *  zombie.iso.objects.IsoThumpable
 *  zombie.iso.objects.IsoTrap
 *  zombie.iso.objects.IsoTree
 *  zombie.iso.objects.IsoWaveSignal
 *  zombie.iso.objects.IsoWindow
 *  zombie.iso.objects.IsoWindowFrame
 *  zombie.iso.objects.RainManager
 *  zombie.iso.objects.interfaces.BarricadeAble
 *  zombie.iso.sprite.IsoSprite
 *  zombie.iso.sprite.IsoSpriteInstance
 *  zombie.iso.sprite.IsoSpriteManager
 *  zombie.iso.sprite.shapers.FloorShaperAttachedSprites
 *  zombie.iso.sprite.shapers.FloorShaperDeDiamond
 *  zombie.iso.sprite.shapers.FloorShaperDiamond
 *  zombie.iso.sprite.shapers.WallShaper
 *  zombie.iso.sprite.shapers.WallShaperN
 *  zombie.iso.sprite.shapers.WallShaperW
 *  zombie.iso.sprite.shapers.WallShaperWhole
 *  zombie.iso.weather.fx.WeatherFxMask
 *  zombie.iso.worldgen.biomes.IBiome
 *  zombie.iso.worldgen.utils.SquareCoord
 *  zombie.iso.zones.Zone
 *  zombie.meta.Meta
 *  zombie.network.GameClient
 *  zombie.network.GameServer
 *  zombie.network.IConnection
 *  zombie.network.PacketTypes$PacketType
 *  zombie.network.ServerLOS$ServerLighting
 *  zombie.network.ServerMap
 *  zombie.network.ServerOptions
 *  zombie.network.packets.AddExplosiveTrapPacket
 *  zombie.network.packets.INetworkPacket
 *  zombie.network.packets.RemoveItemFromSquarePacket
 *  zombie.network.packets.actions.AddCorpseToMapPacket
 *  zombie.network.packets.character.AnimalCommandPacket$Type
 *  zombie.network.packets.service.ReceiveModDataPacket
 *  zombie.pathfind.PolygonalMap2
 *  zombie.popman.ObjectPool
 *  zombie.popman.animal.AnimalInstanceManager
 *  zombie.radio.devices.DeviceData
 *  zombie.randomizedWorld.RandomizedWorldBase
 *  zombie.randomizedWorld.randomizedZoneStory.RandomizedZoneStoryBase
 *  zombie.scripting.ScriptManager
 *  zombie.scripting.entity.GameEntityScript
 *  zombie.scripting.objects.CharacterTrait
 *  zombie.scripting.objects.ItemKey
 *  zombie.scripting.objects.ItemTag
 *  zombie.tileDepth.CutawayAttachedModifier
 *  zombie.tileDepth.TileDepthMapManager
 *  zombie.tileDepth.TileDepthMapManager$TileDepthPreset
 *  zombie.tileDepth.TileSeamModifier
 *  zombie.util.StringUtils
 *  zombie.util.Type
 *  zombie.util.io.BitHeader
 *  zombie.util.io.BitHeader$HeaderSize
 *  zombie.util.io.BitHeaderRead
 *  zombie.util.io.BitHeaderWrite
 *  zombie.util.lambda.Invokers$Params2$Boolean$ICallback
 *  zombie.util.list.PZArrayList
 *  zombie.util.list.PZArrayUtil
 *  zombie.vehicles.BaseVehicle
 */
package zombie.iso;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.invoke.LambdaMetafactory;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Stack;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.jspecify.annotations.Nullable;
import se.krka.kahlua.vm.KahluaTable;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.IndieGL;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.Lua.MapObjects;
import zombie.MapCollisionData;
import zombie.SandboxOptions;
import zombie.SoundManager;
import zombie.UsedFromLua;
import zombie.VirtualZombieManager;
import zombie.ZombieSpawnRecorder;
import zombie.audio.BaseSoundEmitter;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoSurvivor;
import zombie.characters.IsoZombie;
import zombie.characters.VisibilityData;
import zombie.characters.animals.AnimalSoundState;
import zombie.characters.animals.IsoAnimal;
import zombie.characters.animals.datas.AnimalBreed;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.SceneShaderStore;
import zombie.core.SpriteRenderer;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferWriter;
import zombie.core.opengl.Shader;
import zombie.core.opengl.ShaderUniformSetter;
import zombie.core.profiling.AbstractPerformanceProfileProbe;
import zombie.core.properties.IsoObjectChange;
import zombie.core.properties.IsoPropertyType;
import zombie.core.properties.PropertyContainer;
import zombie.core.random.Rand;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;
import zombie.entity.ComponentType;
import zombie.entity.GameEntityFactory;
import zombie.entity.GameEntityManager;
import zombie.erosion.ErosionData;
import zombie.erosion.categories.ErosionCategory;
import zombie.globalObjects.CGlobalObjectSystem;
import zombie.globalObjects.CGlobalObjects;
import zombie.globalObjects.GlobalObject;
import zombie.globalObjects.SGlobalObjectSystem;
import zombie.globalObjects.SGlobalObjects;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemContainer;
import zombie.inventory.types.AnimalInventoryItem;
import zombie.inventory.types.Food;
import zombie.inventory.types.HandWeapon;
import zombie.iso.BentFences;
import zombie.iso.BuildingDef;
import zombie.iso.CellLoader;
import zombie.iso.FishSplashSoundManager;
import zombie.iso.IsoButcherHook;
import zombie.iso.IsoCamera;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoDepthHelper;
import zombie.iso.IsoDirections;
import zombie.iso.IsoFloorBloodSplat;
import zombie.iso.IsoGridOcclusionData;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoGridSquareCollisionData;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoObjectUtils;
import zombie.iso.IsoPuddles;
import zombie.iso.IsoPuddlesCompute;
import zombie.iso.IsoPuddlesGeometry;
import zombie.iso.IsoPushableObject;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWallBloodSplat;
import zombie.iso.IsoWater;
import zombie.iso.IsoWaterGeometry;
import zombie.iso.IsoWorld;
import zombie.iso.LightingJNI;
import zombie.iso.LosUtil;
import zombie.iso.RoomDef;
import zombie.iso.SliceY;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.Vector2;
import zombie.iso.areas.DesignationZone;
import zombie.iso.areas.DesignationZoneAnimal;
import zombie.iso.areas.IsoBuilding;
import zombie.iso.areas.IsoRoom;
import zombie.iso.areas.NonPvpZone;
import zombie.iso.areas.SafeHouse;
import zombie.iso.areas.isoregion.IsoRegions;
import zombie.iso.areas.isoregion.regions.IWorldRegion;
import zombie.iso.areas.isoregion.regions.IsoWorldRegion;
import zombie.iso.fboRenderChunk.FBORenderCell;
import zombie.iso.fboRenderChunk.FBORenderChunk;
import zombie.iso.fboRenderChunk.FBORenderChunkManager;
import zombie.iso.fboRenderChunk.ObjectRenderLayer;
import zombie.iso.objects.IsoAnimalTrack;
import zombie.iso.objects.IsoBarbecue;
import zombie.iso.objects.IsoBarricade;
import zombie.iso.objects.IsoBrokenGlass;
import zombie.iso.objects.IsoCarBatteryCharger;
import zombie.iso.objects.IsoCompost;
import zombie.iso.objects.IsoCurtain;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoFire;
import zombie.iso.objects.IsoFireManager;
import zombie.iso.objects.IsoFireplace;
import zombie.iso.objects.IsoGenerator;
import zombie.iso.objects.IsoHutch;
import zombie.iso.objects.IsoLightSwitch;
import zombie.iso.objects.IsoMannequin;
import zombie.iso.objects.IsoRainSplash;
import zombie.iso.objects.IsoRaindrop;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoTrap;
import zombie.iso.objects.IsoTree;
import zombie.iso.objects.IsoWaveSignal;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.IsoWindowFrame;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.iso.objects.RainManager;
import zombie.iso.objects.interfaces.BarricadeAble;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteInstance;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.iso.sprite.shapers.FloorShaperAttachedSprites;
import zombie.iso.sprite.shapers.FloorShaperDeDiamond;
import zombie.iso.sprite.shapers.FloorShaperDiamond;
import zombie.iso.sprite.shapers.WallShaper;
import zombie.iso.sprite.shapers.WallShaperN;
import zombie.iso.sprite.shapers.WallShaperW;
import zombie.iso.sprite.shapers.WallShaperWhole;
import zombie.iso.weather.fx.WeatherFxMask;
import zombie.iso.worldgen.biomes.IBiome;
import zombie.iso.worldgen.utils.SquareCoord;
import zombie.iso.zones.Zone;
import zombie.meta.Meta;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.PacketTypes;
import zombie.network.ServerLOS;
import zombie.network.ServerMap;
import zombie.network.ServerOptions;
import zombie.network.packets.AddExplosiveTrapPacket;
import zombie.network.packets.INetworkPacket;
import zombie.network.packets.RemoveItemFromSquarePacket;
import zombie.network.packets.actions.AddCorpseToMapPacket;
import zombie.network.packets.character.AnimalCommandPacket;
import zombie.network.packets.service.ReceiveModDataPacket;
import zombie.pathfind.PolygonalMap2;
import zombie.popman.ObjectPool;
import zombie.popman.animal.AnimalInstanceManager;
import zombie.radio.devices.DeviceData;
import zombie.randomizedWorld.RandomizedWorldBase;
import zombie.randomizedWorld.randomizedZoneStory.RandomizedZoneStoryBase;
import zombie.scripting.ScriptManager;
import zombie.scripting.entity.GameEntityScript;
import zombie.scripting.objects.CharacterTrait;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.ItemKey;
import zombie.scripting.objects.ItemTag;
import zombie.tileDepth.CutawayAttachedModifier;
import zombie.tileDepth.TileDepthMapManager;
import zombie.tileDepth.TileSeamModifier;
import zombie.util.StringUtils;
import zombie.util.Type;
import zombie.util.io.BitHeader;
import zombie.util.io.BitHeaderRead;
import zombie.util.io.BitHeaderWrite;
import zombie.util.lambda.Invokers;
import zombie.util.list.PZArrayList;
import zombie.util.list.PZArrayUtil;
import zombie.vehicles.BaseVehicle;

/*
 * Exception performing whole class analysis ignored.
 */
@UsedFromLua
public final class IsoGridSquare {
    public static final boolean USE_WALL_SHADER = true;
    public static final boolean ADD_UNDERGROUND_BLOCKS = false;
    private static final float CUTAWAY_OUTLINE_ALPHA = 0.35f;
    private static final int cutawayY = 0;
    private static final int cutawayNWWidth = 66;
    private static final int cutawayNWHeight = 226;
    private static final int cutawaySEXCut = 1084;
    private static final int cutawaySEXUncut = 1212;
    private static final int cutawaySEWidth = 6;
    private static final int cutawaySEHeight = 196;
    private static final int cutawayNXFullyCut = 700;
    private static final int cutawayNXCutW = 444;
    private static final int cutawayNXUncut = 828;
    private static final int cutawayNXCutE = 956;
    private static final int cutawayWXFullyCut = 512;
    private static final int cutawayWXCutS = 768;
    private static final int cutawayWXUncut = 896;
    private static final int cutawayWXCutN = 256;
    private static final int cutawayFenceXOffset = 1;
    private static final int cutawayLogWallXOffset = 1;
    private static final int cutawayMedicalCurtainWXOffset = -3;
    private static final int cutawayTentWallXOffset = -3;
    private static final int cutawaySpiffoWindowXOffset = -24;
    private static final int cutawayRoof4XOffset = -60;
    private static final int cutawayRoof17XOffset = -46;
    private static final int cutawayRoof28XOffset = -60;
    private static final int cutawayRoof41XOffset = -46;
    public static final int WALL_TYPE_N = 1;
    public static final int WALL_TYPE_S = 2;
    public static final int WALL_TYPE_W = 4;
    public static final int WALL_TYPE_E = 8;
    private static final int[] SURFACE_OFFSETS = new int[8];
    private static final long VisiFlagTimerPeriod_ms = 750L;
    public static final byte PCF_NONE = 0;
    public static final byte PCF_NORTH = 1;
    public static final byte PCF_WEST = 2;
    private static final ThreadLocal<ArrayList<Zone>> threadLocalZones = ThreadLocal.withInitial(ArrayList::new);
    private static final IsoDirections[] DIRECTIONS = IsoDirections.values();
    public final ILighting[] lighting = new ILighting[4];
    private static final Vector2 tempo = new Vector2();
    private static final Vector2 tempo2 = new Vector2();
    public static float rmod;
    public static float gmod;
    public static float bmod;
    public static int idMax;
    private static int col;
    private static int path;
    private static int pathdoor;
    private static int vision;
    private static final String[] rainsplashCache;
    public static boolean useSlowCollision;
    public BuildingDef associatedBuilding;
    private boolean hasTree;
    private ArrayList<Float> lightInfluenceB;
    private ArrayList<Float> lightInfluenceG;
    private ArrayList<Float> lightInfluenceR;
    private final IsoGridSquare[] nav = new IsoGridSquare[8];
    public int lightLevel;
    public int collideMatrix;
    public int pathMatrix;
    public int visionMatrix;
    public IsoRoom room;
    public IsoGridSquare w;
    public IsoGridSquare nw;
    public IsoGridSquare sw;
    public IsoGridSquare s;
    public IsoGridSquare n;
    public IsoGridSquare ne;
    public IsoGridSquare se;
    public IsoGridSquare e;
    public IsoGridSquare u;
    public IsoGridSquare d;
    public boolean haveSheetRope;
    private IWorldRegion isoWorldRegion;
    private boolean hasSetIsoWorldRegion;
    public int objectsSyncCount;
    public IsoBuilding roofHideBuilding;
    public boolean flattenGrassEtc;
    private final byte[] playerCutawayFlags = new byte[4];
    private final long[] playerCutawayFlagLockUntilTimes = new long[4];
    private final byte[] targetPlayerCutawayFlags = new byte[4];
    private final boolean[] playerIsDissolvedFlags = new boolean[4];
    private final long[] playerIsDissolvedFlagLockUntilTimes = new long[4];
    private final boolean[] targetPlayerIsDissolvedFlags = new boolean[4];
    private IsoWaterGeometry water;
    private IsoPuddlesGeometry puddles;
    private float puddlesCacheSize = -1.0f;
    private float puddlesCacheLevel = -1.0f;
    private final WaterSplashData waterSplashData = new WaterSplashData();
    private final ColorInfo[] lightInfo = new ColorInfo[4];
    private IsoRaindrop rainDrop;
    private IsoRainSplash rainSplash;
    private float splashX;
    private float splashY;
    private float splashFrame = -1.0f;
    private int splashFrameNum;
    private static final Texture[] waterSplashCache;
    private static boolean isWaterSplashCacheInitialised;
    public static int gridSquareCacheEmptyTimer;
    private static float darkStep;
    public static float recalcLightTime;
    private static int lightcache;
    public boolean propertiesDirty = true;
    private static final ColorInfo defColorInfo;
    private static final ColorInfo blackColorInfo;
    private static int colu;
    private static int coll;
    private static int colr;
    private static int colu2;
    private static int coll2;
    private static int colr2;
    private static boolean doSlowPathfinding;
    public static boolean circleStencil;
    public long hashCodeObjects;
    public int FIRE_IMMUNE_THRESHOLD = 800000;
    public static final String FLOORS_BURNT_SPRITE_PREFIX = "floors_burnt_01_";
    public static final GetSquare cellGetSquare;
    public int x;
    public int y;
    public int z;
    private int cachedScreenValue;
    public float cachedScreenX;
    public float cachedScreenY;
    private static long torchTimer;
    public boolean solidFloorCached;
    public boolean solidFloor;
    private boolean cacheIsFree;
    private boolean cachedIsFree;
    public IsoChunk chunk;
    public long roomId = -1L;
    public Integer id;
    public Zone zone;
    private final ArrayList<IsoGameCharacter> deferedCharacters = new ArrayList();
    private int deferredCharacterTick = -1;
    private final ArrayList<IsoMovingObject> staticMovingObjects = new ArrayList(0);
    private final ArrayList<IsoMovingObject> movingObjects = new ArrayList(0);
    protected final PZArrayList<IsoObject> objects = new PZArrayList(IsoObject.class, 2);
    private final ArrayList<IsoWorldInventoryObject> worldObjects = new ArrayList();
    public long hasTypes;
    private final PropertyContainer properties = new PropertyContainer();
    private final ArrayList<IsoObject> specialObjects = new ArrayList(0);
    public boolean haveRoof;
    private boolean burntOut;
    private boolean hasFlies;
    private IBiome biome;
    private IsoGridOcclusionData occlusionDataCache;
    private static final PZArrayList<IsoWorldInventoryObject> tempWorldInventoryObjects;
    public static final ConcurrentLinkedQueue<IsoGridSquare> isoGridSquareCache;
    public static ArrayDeque<IsoGridSquare> loadGridSquareCache;
    private boolean overlayDone;
    private KahluaTable table;
    private int trapPositionX = -1;
    private int trapPositionY = -1;
    private int trapPositionZ = -1;
    public static final ArrayList<String> ignoreBlockingSprites;
    public static final ArrayList<IsoGridSquare> choices;
    private static final ColorInfo lightInfoTemp;
    private static final float doorWindowCutawayLightMin = 0.3f;
    private static boolean wallCutawayW;
    private static boolean wallCutawayN;
    public boolean isSolidFloorCache;
    public boolean isExteriorCache;
    public boolean isVegitationCache;
    public int hourLastSeen = Integer.MIN_VALUE;
    private static IsoGridSquare lastLoaded;
    private static final Color tr;
    private static final Color tl;
    private static final Color br;
    private static final Color bl;
    private static final Color interp1;
    private static final Color interp2;
    private static final Color finalCol;
    private static final Comparator<IsoMovingObject> comp;
    public static boolean isOnScreenLast;
    private ErosionData.Square erosion;

    public SquareCoord getCoords() {
        return new SquareCoord(this.x, this.y, this.z);
    }

    public static boolean getMatrixBit(int matrix, int x, int y, int z) {
        return IsoGridSquare.getMatrixBit(matrix, (byte)x, (byte)y, (byte)z);
    }

    public static boolean getMatrixBit(int matrix, byte x, byte y, byte z) {
        return (matrix >> x + y * 3 + z * 9 & 1) != 0;
    }

    public static int setMatrixBit(int matrix, int x, int y, int z, boolean val) {
        return IsoGridSquare.setMatrixBit(matrix, (byte)x, (byte)y, (byte)z, val);
    }

    public static int setMatrixBit(int matrix, byte x, byte y, byte z, boolean val) {
        if (val) {
            return matrix | 1 << x + y * 3 + z * 9;
        }
        return matrix & ~(1 << x + y * 3 + z * 9);
    }

    public int GetRLightLevel() {
        return (this.lightLevel & 0xFF0000) >> 16;
    }

    public int GetGLightLevel() {
        return (this.lightLevel & 0xFF00) >> 8;
    }

    public int GetBLightLevel() {
        return this.lightLevel & 0xFF;
    }

    public void SetRLightLevel(int val) {
        this.lightLevel = this.lightLevel & 0xFF00FFFF | val << 16;
    }

    public void SetGLightLevel(int val) {
        this.lightLevel = this.lightLevel & 0xFFFF00FF | val << 8;
    }

    public void SetBLightLevel(int val) {
        this.lightLevel = this.lightLevel & 0xFFFFFF00 | val;
    }

    public void setPlayerCutawayFlag(int playerIndex, int flags, long currentTimeMillis) {
        this.targetPlayerCutawayFlags[playerIndex] = (byte)(flags & 3);
        if (currentTimeMillis > this.playerCutawayFlagLockUntilTimes[playerIndex] && this.playerCutawayFlags[playerIndex] != this.targetPlayerCutawayFlags[playerIndex]) {
            this.playerCutawayFlags[playerIndex] = this.targetPlayerCutawayFlags[playerIndex];
            this.playerCutawayFlagLockUntilTimes[playerIndex] = currentTimeMillis + 750L;
        }
    }

    public void addPlayerCutawayFlag(int playerIndex, int flag, long currentTimeMillis) {
        int flags = this.targetPlayerCutawayFlags[playerIndex] | flag;
        this.setPlayerCutawayFlag(playerIndex, flags, currentTimeMillis);
    }

    public void clearPlayerCutawayFlag(int playerIndex, int flag, long currentTimeMillis) {
        int flags = this.targetPlayerCutawayFlags[playerIndex] & ~flag;
        this.setPlayerCutawayFlag(playerIndex, flags, currentTimeMillis);
    }

    public int getPlayerCutawayFlag(int playerIndex, long currentTimeMillis) {
        if (PerformanceSettings.fboRenderChunk) {
            return this.targetPlayerCutawayFlags[playerIndex];
        }
        if (currentTimeMillis > this.playerCutawayFlagLockUntilTimes[playerIndex]) {
            return this.targetPlayerCutawayFlags[playerIndex];
        }
        return this.playerCutawayFlags[playerIndex];
    }

    public void setIsDissolved(int playerIndex, boolean bDissolved, long currentTimeMillis) {
        this.targetPlayerIsDissolvedFlags[playerIndex] = bDissolved;
        if (currentTimeMillis > this.playerIsDissolvedFlagLockUntilTimes[playerIndex] && this.playerIsDissolvedFlags[playerIndex] != this.targetPlayerIsDissolvedFlags[playerIndex]) {
            this.playerIsDissolvedFlags[playerIndex] = this.targetPlayerIsDissolvedFlags[playerIndex];
            this.playerIsDissolvedFlagLockUntilTimes[playerIndex] = currentTimeMillis + 750L;
        }
    }

    public boolean getIsDissolved(int playerIndex, long currentTimeMillis) {
        if (currentTimeMillis > this.playerIsDissolvedFlagLockUntilTimes[playerIndex]) {
            return this.targetPlayerIsDissolvedFlags[playerIndex];
        }
        return this.playerIsDissolvedFlags[playerIndex];
    }

    public boolean hasWater() {
        for (int i = 0; i < this.objects.size(); ++i) {
            if (!((IsoObject)this.objects.get(i)).hasWater()) continue;
            return true;
        }
        return false;
    }

    public IsoWaterGeometry getWater() {
        if (this.water != null && this.water.adjacentChunkLoadedCounter != this.chunk.adjacentChunkLoadedCounter) {
            this.water.adjacentChunkLoadedCounter = this.chunk.adjacentChunkLoadedCounter;
            if (this.water.hasWater || this.water.shore) {
                this.clearWater();
            }
        }
        if (this.water == null) {
            try {
                this.water = (IsoWaterGeometry)IsoWaterGeometry.pool.alloc();
                this.water.adjacentChunkLoadedCounter = this.chunk.adjacentChunkLoadedCounter;
                if (this.water.init(this) == null) {
                    this.clearWater();
                }
            }
            catch (Exception e) {
                this.clearWater();
            }
        }
        return this.water;
    }

    public void clearWater() {
        if (this.water != null) {
            IsoWaterGeometry.pool.release((Object)this.water);
            this.water = null;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public IsoPuddlesGeometry getPuddles() {
        if (this.puddles == null) {
            try {
                ObjectPool objectPool = IsoPuddlesGeometry.pool;
                synchronized (objectPool) {
                    this.puddles = (IsoPuddlesGeometry)IsoPuddlesGeometry.pool.alloc();
                }
                this.puddles.square = this;
                this.puddles.recalc = true;
            }
            catch (Exception e) {
                this.clearPuddles();
            }
        }
        return this.puddles;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void clearPuddles() {
        if (this.puddles != null) {
            this.puddles.square = null;
            ObjectPool objectPool = IsoPuddlesGeometry.pool;
            synchronized (objectPool) {
                IsoPuddlesGeometry.pool.release((Object)this.puddles);
            }
            this.puddles = null;
        }
    }

    public float getPuddlesInGround() {
        if (this.isInARoom()) {
            return -1.0f;
        }
        if (GameServer.server || (double)Math.abs(IsoPuddles.getInstance().getPuddlesSize() + (float)Core.getInstance().getPerfPuddles() + (float)IsoCamera.frameState.offscreenWidth - this.puddlesCacheSize) >= 0.01) {
            this.puddlesCacheSize = IsoPuddles.getInstance().getPuddlesSize() + (float)Core.getInstance().getPerfPuddles() + (float)IsoCamera.frameState.offscreenWidth;
            this.puddlesCacheLevel = IsoPuddlesCompute.computePuddle((IsoGridSquare)this);
        }
        return this.puddlesCacheLevel;
    }

    public void removeUnderground() {
        IsoObject[] elements = (IsoObject[])this.objects.getElements();
        for (int i = 0; i < elements.length; ++i) {
            IsoObject element = elements[i];
            if (element == null || element.getTile() == null || !element.getTile().startsWith("underground")) continue;
            this.getObjects().remove((Object)element);
            return;
        }
    }

    public boolean isInsideRectangle(int x, int y, int w, int h) {
        return this.x >= x && this.y >= y && this.x < x + w && this.y < y + h;
    }

    public IsoGridSquare doGridNav(GetSquare getter) {
        for (int i = 0; i < DIRECTIONS.length; ++i) {
            IsoDirections dir = DIRECTIONS[i];
            IsoGridSquare square = getter.getGridSquare(this.x + dir.dx(), this.y + dir.dy(), this.z);
            this.setAdjacentSquare(dir, square);
            if (square == null) continue;
            square.setAdjacentSquare(dir.Rot180(), this);
        }
        return this;
    }

    public IsoGridOcclusionData getOcclusionData() {
        return this.occlusionDataCache;
    }

    public IsoGridOcclusionData getOrCreateOcclusionData() {
        assert (!GameServer.server);
        if (this.occlusionDataCache == null) {
            this.occlusionDataCache = new IsoGridOcclusionData(this);
        }
        return this.occlusionDataCache;
    }

    public void softClear() {
        this.zone = null;
        this.room = null;
        this.w = null;
        this.nw = null;
        this.sw = null;
        this.s = null;
        this.n = null;
        this.ne = null;
        this.se = null;
        this.e = null;
        this.u = null;
        this.d = null;
        this.isoWorldRegion = null;
        this.hasSetIsoWorldRegion = false;
        this.biome = null;
        for (int n = 0; n < 8; ++n) {
            this.nav[n] = null;
        }
    }

    public float getGridSneakModifier(boolean onlySolidTrans) {
        if (!onlySolidTrans) {
            if (this.properties.has("CloseSneakBonus")) {
                return (float)Integer.parseInt(this.properties.get("CloseSneakBonus")) / 100.0f;
            }
            if (this.properties.has(IsoFlagType.collideN) || this.properties.has(IsoFlagType.collideW) || this.properties.has(IsoFlagType.WindowN) || this.properties.has(IsoFlagType.WindowW) || this.properties.has(IsoFlagType.doorN) || this.properties.has(IsoFlagType.doorW)) {
                return 8.0f;
            }
        } else if (this.properties.has(IsoFlagType.solidtrans)) {
            return 4.0f;
        }
        return 1.0f;
    }

    public boolean isSomethingTo(IsoGridSquare other) {
        return this.isWallTo(other) || this.isWindowTo(other) || this.isDoorTo(other);
    }

    public IsoObject getTransparentWallTo(IsoGridSquare other) {
        if (other == null || other == this || !this.isWallTo(other)) {
            return null;
        }
        if (other.x > this.x && other.properties.has(IsoFlagType.SpearOnlyAttackThrough) && !other.properties.has(IsoFlagType.WindowW)) {
            return other.getWall();
        }
        if (this.x > other.x && this.properties.has(IsoFlagType.SpearOnlyAttackThrough) && !this.properties.has(IsoFlagType.WindowW)) {
            return this.getWall();
        }
        if (other.y > this.y && other.properties.has(IsoFlagType.SpearOnlyAttackThrough) && !other.properties.has(IsoFlagType.WindowN)) {
            return other.getWall();
        }
        if (this.y > other.y && this.properties.has(IsoFlagType.SpearOnlyAttackThrough) && !this.properties.has(IsoFlagType.WindowN)) {
            return this.getWall();
        }
        if (other.x != this.x && other.y != this.y) {
            IsoObject wall1 = this.getTransparentWallTo(IsoWorld.instance.currentCell.getGridSquare(other.x, this.y, this.z));
            IsoObject wall2 = this.getTransparentWallTo(IsoWorld.instance.currentCell.getGridSquare(this.x, other.y, this.z));
            if (wall1 != null) {
                return wall1;
            }
            if (wall2 != null) {
                return wall2;
            }
            wall1 = other.getTransparentWallTo(IsoWorld.instance.currentCell.getGridSquare(other.x, this.y, this.z));
            wall2 = other.getTransparentWallTo(IsoWorld.instance.currentCell.getGridSquare(this.x, other.y, this.z));
            if (wall1 != null) {
                return wall1;
            }
            if (wall2 != null) {
                return wall2;
            }
        }
        return null;
    }

    public boolean isWallTo(IsoGridSquare other) {
        if (other == null || other == this) {
            return false;
        }
        if (other.x > this.x && other.properties.has(IsoFlagType.collideW) && !other.properties.has(IsoFlagType.WindowW)) {
            return true;
        }
        if (this.x > other.x && this.properties.has(IsoFlagType.collideW) && !this.properties.has(IsoFlagType.WindowW)) {
            return true;
        }
        if (other.y > this.y && other.properties.has(IsoFlagType.collideN) && !other.properties.has(IsoFlagType.WindowN)) {
            return true;
        }
        if (this.y > other.y && this.properties.has(IsoFlagType.collideN) && !this.properties.has(IsoFlagType.WindowN)) {
            return true;
        }
        if (other.x != this.x && other.y != this.y) {
            if (this.isWallTo(IsoWorld.instance.currentCell.getGridSquare(other.x, this.y, this.z), 1) || this.isWallTo(IsoWorld.instance.currentCell.getGridSquare(this.x, other.y, this.z), 1)) {
                return true;
            }
            if (other.isWallTo(IsoWorld.instance.currentCell.getGridSquare(other.x, this.y, this.z), 1) || other.isWallTo(IsoWorld.instance.currentCell.getGridSquare(this.x, other.y, this.z), 1)) {
                return true;
            }
        }
        return false;
    }

    public boolean isWallTo(IsoGridSquare other, int depth) {
        if (depth > 100) {
            boolean bl = false;
        }
        if (other == null || other == this) {
            return false;
        }
        if (other.x > this.x && other.properties.has(IsoFlagType.collideW) && !other.properties.has(IsoFlagType.WindowW)) {
            return true;
        }
        if (this.x > other.x && this.properties.has(IsoFlagType.collideW) && !this.properties.has(IsoFlagType.WindowW)) {
            return true;
        }
        if (other.y > this.y && other.properties.has(IsoFlagType.collideN) && !other.properties.has(IsoFlagType.WindowN)) {
            return true;
        }
        if (this.y > other.y && this.properties.has(IsoFlagType.collideN) && !this.properties.has(IsoFlagType.WindowN)) {
            return true;
        }
        if (other.x != this.x && other.y != this.y) {
            if (this.isWallTo(IsoWorld.instance.currentCell.getGridSquare(other.x, this.y, this.z), depth + 1) || this.isWallTo(IsoWorld.instance.currentCell.getGridSquare(this.x, other.y, this.z), depth + 1)) {
                return true;
            }
            if (other.isWallTo(IsoWorld.instance.currentCell.getGridSquare(other.x, this.y, this.z), depth + 1) || other.isWallTo(IsoWorld.instance.currentCell.getGridSquare(this.x, other.y, this.z), depth + 1)) {
                return true;
            }
        }
        return false;
    }

    public boolean isWindowTo(IsoGridSquare other) {
        if (other == null || other == this) {
            return false;
        }
        if (other.x > this.x && other.properties.has(IsoFlagType.windowW)) {
            return true;
        }
        if (this.x > other.x && this.properties.has(IsoFlagType.windowW)) {
            return true;
        }
        if (other.y > this.y && other.properties.has(IsoFlagType.windowN)) {
            return true;
        }
        if (this.y > other.y && this.properties.has(IsoFlagType.windowN)) {
            return true;
        }
        if (other.x != this.x && other.y != this.y) {
            if (this.isWindowTo(IsoWorld.instance.currentCell.getGridSquare(other.x, this.y, this.z)) || this.isWindowTo(IsoWorld.instance.currentCell.getGridSquare(this.x, other.y, this.z))) {
                return true;
            }
            if (other.isWindowTo(IsoWorld.instance.currentCell.getGridSquare(other.x, this.y, this.z)) || other.isWindowTo(IsoWorld.instance.currentCell.getGridSquare(this.x, other.y, this.z))) {
                return true;
            }
        }
        return false;
    }

    public boolean haveDoor() {
        for (int i = 0; i < this.objects.size(); ++i) {
            if (!(this.objects.get(i) instanceof IsoDoor)) continue;
            return true;
        }
        return false;
    }

    public boolean hasDoorOnEdge(IsoDirections edge, boolean ignoreOpen) {
        for (int i = 0; i < this.specialObjects.size(); ++i) {
            IsoDoor door = (IsoDoor)Type.tryCastTo((Object)this.specialObjects.get(i), IsoDoor.class);
            if (door != null && door.getSpriteEdge(ignoreOpen) == edge) {
                return true;
            }
            IsoThumpable thump = (IsoThumpable)Type.tryCastTo((Object)this.specialObjects.get(i), IsoThumpable.class);
            if (thump == null || thump.getSpriteEdge(ignoreOpen) != edge) continue;
            return true;
        }
        return false;
    }

    public boolean hasClosedDoorOnEdge(IsoDirections edge) {
        boolean ignoreOpen = false;
        for (int i = 0; i < this.specialObjects.size(); ++i) {
            IsoDoor door = (IsoDoor)Type.tryCastTo((Object)this.specialObjects.get(i), IsoDoor.class);
            if (door != null && !door.IsOpen() && door.getSpriteEdge(false) == edge) {
                return true;
            }
            IsoThumpable thump = (IsoThumpable)Type.tryCastTo((Object)this.specialObjects.get(i), IsoThumpable.class);
            if (thump == null || thump.IsOpen() || thump.getSpriteEdge(false) != edge) continue;
            return true;
        }
        return false;
    }

    public boolean hasOpenDoorOnEdge(IsoDirections edge) {
        boolean ignoreOpen = false;
        for (int i = 0; i < this.specialObjects.size(); ++i) {
            IsoDoor door = (IsoDoor)Type.tryCastTo((Object)this.specialObjects.get(i), IsoDoor.class);
            if (door != null && door.IsOpen() && door.getSpriteEdge(false) == edge) {
                return true;
            }
            IsoThumpable thump = (IsoThumpable)Type.tryCastTo((Object)this.specialObjects.get(i), IsoThumpable.class);
            if (thump == null || !thump.IsOpen() || thump.getSpriteEdge(false) != edge) continue;
            return true;
        }
        return false;
    }

    public boolean isDoorTo(IsoGridSquare other) {
        if (other == null || other == this) {
            return false;
        }
        if (other.x > this.x && other.properties.has(IsoFlagType.doorW)) {
            return true;
        }
        if (this.x > other.x && this.properties.has(IsoFlagType.doorW)) {
            return true;
        }
        if (other.y > this.y && other.properties.has(IsoFlagType.doorN)) {
            return true;
        }
        if (this.y > other.y && this.properties.has(IsoFlagType.doorN)) {
            return true;
        }
        if (other.x != this.x && other.y != this.y) {
            if (this.isDoorTo(IsoWorld.instance.currentCell.getGridSquare(other.x, this.y, this.z)) || this.isDoorTo(IsoWorld.instance.currentCell.getGridSquare(this.x, other.y, this.z))) {
                return true;
            }
            if (other.isDoorTo(IsoWorld.instance.currentCell.getGridSquare(other.x, this.y, this.z)) || other.isDoorTo(IsoWorld.instance.currentCell.getGridSquare(this.x, other.y, this.z))) {
                return true;
            }
        }
        return false;
    }

    public boolean isBlockedTo(IsoGridSquare other) {
        return this.isWallTo(other) || this.isWindowBlockedTo(other) || this.isDoorBlockedTo(other) || this.isStairBlockedTo(other);
    }

    public boolean canReachTo(IsoGridSquare other) {
        if (other == this) {
            return true;
        }
        if (Math.abs(other.x - this.x) > 1 || Math.abs(other.y - this.y) > 1 || other.z != this.z || this.isWindowBlockedTo(other) || this.isDoorBlockedTo(other)) {
            return false;
        }
        if (other.y < this.y && this.getWallExcludingList(true, ignoreBlockingSprites) != null) {
            return false;
        }
        if (this.y < other.y && other.getWallExcludingList(true, ignoreBlockingSprites) != null) {
            return false;
        }
        if (other.x < this.x && this.getWallExcludingList(false, ignoreBlockingSprites) != null) {
            return false;
        }
        if (this.x < other.x && other.getWallExcludingList(false, ignoreBlockingSprites) != null) {
            return false;
        }
        if (this.x > other.x && this.HasStairTopWest()) {
            return false;
        }
        if (this.y > other.y && this.HasStairTopNorth()) {
            return false;
        }
        return !this.isWallTo(other);
    }

    public boolean isWindowBlockedTo(IsoGridSquare other) {
        if (other == null) {
            return false;
        }
        if (other.x > this.x && other.hasBlockedWindow(false)) {
            return true;
        }
        if (this.x > other.x && this.hasBlockedWindow(false)) {
            return true;
        }
        if (other.y > this.y && other.hasBlockedWindow(true)) {
            return true;
        }
        if (this.y > other.y && this.hasBlockedWindow(true)) {
            return true;
        }
        if (other.x != this.x && other.y != this.y) {
            if (this.isWindowBlockedTo(IsoWorld.instance.currentCell.getGridSquare(other.x, this.y, this.z)) || this.isWindowBlockedTo(IsoWorld.instance.currentCell.getGridSquare(this.x, other.y, this.z))) {
                return true;
            }
            if (other.isWindowBlockedTo(IsoWorld.instance.currentCell.getGridSquare(other.x, this.y, this.z)) || other.isWindowBlockedTo(IsoWorld.instance.currentCell.getGridSquare(this.x, other.y, this.z))) {
                return true;
            }
        }
        return false;
    }

    public boolean hasBlockedWindow(boolean north) {
        for (int i = 0; i < this.objects.size(); ++i) {
            IsoWindow w;
            IsoObject o = (IsoObject)this.objects.get(i);
            if (!(o instanceof IsoWindow) || (w = (IsoWindow)o).getNorth() != north) continue;
            return !w.isDestroyed() && !w.IsOpen() || w.isBarricaded();
        }
        return false;
    }

    public boolean isDoorBlockedTo(IsoGridSquare other) {
        if (other == null) {
            return false;
        }
        if (other.x > this.x && other.hasBlockedDoor(false)) {
            return true;
        }
        if (this.x > other.x && this.hasBlockedDoor(false)) {
            return true;
        }
        if (other.y > this.y && other.hasBlockedDoor(true)) {
            return true;
        }
        if (this.y > other.y && this.hasBlockedDoor(true)) {
            return true;
        }
        if (other.x != this.x && other.y != this.y) {
            if (this.isDoorBlockedTo(IsoWorld.instance.currentCell.getGridSquare(other.x, this.y, this.z)) || this.isDoorBlockedTo(IsoWorld.instance.currentCell.getGridSquare(this.x, other.y, this.z))) {
                return true;
            }
            if (other.isDoorBlockedTo(IsoWorld.instance.currentCell.getGridSquare(other.x, this.y, this.z)) || other.isDoorBlockedTo(IsoWorld.instance.currentCell.getGridSquare(this.x, other.y, this.z))) {
                return true;
            }
        }
        return false;
    }

    public boolean hasBlockedDoor(boolean north) {
        for (int i = 0; i < this.objects.size(); ++i) {
            IsoDoor d;
            IsoObject o = (IsoObject)this.objects.get(i);
            if (o instanceof IsoDoor && (d = (IsoDoor)o).getNorth() == north) {
                return !d.isOpen() || d.isBarricaded();
            }
            if (!(o instanceof IsoThumpable) || !(d = (IsoThumpable)o).isDoor() || d.getNorth() != north) continue;
            return !d.open || d.isBarricaded();
        }
        return false;
    }

    public IsoCurtain getCurtain(IsoObjectType curtainType) {
        for (int i = 0; i < this.getSpecialObjects().size(); ++i) {
            IsoCurtain curtain = (IsoCurtain)Type.tryCastTo((Object)this.getSpecialObjects().get(i), IsoCurtain.class);
            if (curtain == null || curtain.getType() != curtainType) continue;
            return curtain;
        }
        return null;
    }

    public IsoObject getHoppable(boolean north) {
        for (int n = 0; n < this.objects.size(); ++n) {
            IsoObject obj = (IsoObject)this.objects.get(n);
            PropertyContainer props = obj.getProperties();
            if (props != null && props.has(north ? IsoFlagType.HoppableN : IsoFlagType.HoppableW)) {
                return obj;
            }
            if (props == null || !props.has(north ? IsoFlagType.WindowN : IsoFlagType.WindowW)) continue;
            return obj;
        }
        return null;
    }

    public IsoObject getHoppableTo(IsoGridSquare next) {
        IsoObject obj;
        if (next == null || next == this) {
            return null;
        }
        if (next.x < this.x && next.y == this.y && (obj = this.getHoppable(false)) != null) {
            return obj;
        }
        if (next.x == this.x && next.y < this.y && (obj = this.getHoppable(true)) != null) {
            return obj;
        }
        if (next.x > this.x && next.y == this.y && (obj = next.getHoppable(false)) != null) {
            return obj;
        }
        if (next.x == this.x && next.y > this.y && (obj = next.getHoppable(true)) != null) {
            return obj;
        }
        if (next.x != this.x && next.y != this.y) {
            IsoGridSquare betweenA = this.getCell().getGridSquare(this.x, next.y, this.z);
            IsoGridSquare betweenB = this.getCell().getGridSquare(next.x, this.y, this.z);
            obj = this.getHoppableTo(betweenA);
            if (obj != null) {
                return obj;
            }
            obj = this.getHoppableTo(betweenB);
            if (obj != null) {
                return obj;
            }
            obj = next.getHoppableTo(betweenA);
            if (obj != null) {
                return obj;
            }
            obj = next.getHoppableTo(betweenB);
            if (obj != null) {
                return obj;
            }
        }
        return null;
    }

    public boolean isHoppableTo(IsoGridSquare other) {
        if (other == null) {
            return false;
        }
        if (other.x != this.x && other.y != this.y) {
            return false;
        }
        if (other.x > this.x && other.properties.has(IsoFlagType.HoppableW)) {
            return true;
        }
        if (this.x > other.x && this.properties.has(IsoFlagType.HoppableW)) {
            return true;
        }
        if (other.y > this.y && other.properties.has(IsoFlagType.HoppableN)) {
            return true;
        }
        return this.y > other.y && this.properties.has(IsoFlagType.HoppableN);
    }

    public IsoObject getBendable(boolean north) {
        for (int n = 0; n < this.objects.size(); ++n) {
            IsoObject obj = (IsoObject)this.objects.get(n);
            if (!BentFences.getInstance().isUnbentObject(obj, north ? IsoDirections.N : IsoDirections.W)) continue;
            return obj;
        }
        return null;
    }

    public IsoObject getBendableTo(IsoGridSquare next) {
        IsoObject obj;
        if (next == null || next == this) {
            return null;
        }
        if (next.x < this.x && next.y == this.y && (obj = this.getBendable(false)) != null) {
            return obj;
        }
        if (next.x == this.x && next.y < this.y && (obj = this.getBendable(true)) != null) {
            return obj;
        }
        if (next.x > this.x && next.y == this.y && (obj = next.getBendable(false)) != null) {
            return obj;
        }
        if (next.x == this.x && next.y > this.y && (obj = next.getBendable(true)) != null) {
            return obj;
        }
        if (next.x != this.x && next.y != this.y) {
            IsoGridSquare betweenA = this.getCell().getGridSquare(this.x, next.y, this.z);
            IsoGridSquare betweenB = this.getCell().getGridSquare(next.x, this.y, this.z);
            obj = this.getBendableTo(betweenA);
            if (obj != null) {
                return obj;
            }
            obj = this.getBendableTo(betweenB);
            if (obj != null) {
                return obj;
            }
            obj = next.getBendableTo(betweenA);
            if (obj != null) {
                return obj;
            }
            obj = next.getBendableTo(betweenB);
            if (obj != null) {
                return obj;
            }
        }
        return null;
    }

    public void discard() {
        this.hourLastSeen = Short.MIN_VALUE;
        this.chunk = null;
        this.zone = null;
        this.lightInfluenceB = null;
        this.lightInfluenceG = null;
        this.lightInfluenceR = null;
        this.room = null;
        this.w = null;
        this.nw = null;
        this.sw = null;
        this.s = null;
        this.n = null;
        this.ne = null;
        this.se = null;
        this.e = null;
        this.u = null;
        this.d = null;
        this.isoWorldRegion = null;
        this.hasSetIsoWorldRegion = false;
        this.nav[0] = null;
        this.nav[1] = null;
        this.nav[2] = null;
        this.nav[3] = null;
        this.nav[4] = null;
        this.nav[5] = null;
        this.nav[6] = null;
        this.nav[7] = null;
        for (int n = 0; n < 4; ++n) {
            if (this.lighting[n] != null) {
                this.lighting[n].reset();
            }
            this.lightInfo[n] = null;
        }
        this.solidFloorCached = false;
        this.solidFloor = false;
        this.cacheIsFree = false;
        this.cachedIsFree = false;
        this.chunk = null;
        this.roomId = -1L;
        this.deferedCharacters.clear();
        this.deferredCharacterTick = -1;
        this.staticMovingObjects.clear();
        this.movingObjects.clear();
        this.objects.clear();
        this.worldObjects.clear();
        this.hasTypes = 0L;
        this.table = null;
        this.properties.Clear();
        this.specialObjects.clear();
        this.rainDrop = null;
        this.rainSplash = null;
        this.overlayDone = false;
        this.haveRoof = false;
        this.burntOut = false;
        this.trapPositionZ = -1;
        this.trapPositionY = -1;
        this.trapPositionX = -1;
        this.haveSheetRope = false;
        if (this.erosion != null) {
            this.erosion.reset();
        }
        if (this.occlusionDataCache != null) {
            this.occlusionDataCache.Reset();
        }
        this.roofHideBuilding = null;
        this.hasFlies = false;
        Arrays.fill(this.playerCutawayFlags, (byte)0);
        Arrays.fill(this.playerCutawayFlagLockUntilTimes, 0L);
        Arrays.fill(this.targetPlayerCutawayFlags, (byte)0);
        isoGridSquareCache.add(this);
    }

    public float DistTo(int x, int y) {
        return IsoUtils.DistanceManhatten((float)((float)x + 0.5f), (float)((float)y + 0.5f), (float)this.x, (float)this.y);
    }

    public float DistTo(IsoGridSquare sq) {
        return IsoUtils.DistanceManhatten((float)((float)this.x + 0.5f), (float)((float)this.y + 0.5f), (float)((float)sq.x + 0.5f), (float)((float)sq.y + 0.5f));
    }

    public float DistToProper(int x, int y) {
        return IsoUtils.DistanceManhatten((float)((float)x + 0.5f), (float)((float)y + 0.5f), (float)((float)this.x + 0.5f), (float)((float)this.y + 0.5f));
    }

    public float DistToProper(IsoGridSquare sq) {
        return IsoUtils.DistanceTo((float)((float)this.x + 0.5f), (float)((float)this.y + 0.5f), (float)((float)sq.x + 0.5f), (float)((float)sq.y + 0.5f));
    }

    public float DistTo(IsoMovingObject other) {
        return IsoUtils.DistanceManhatten((float)((float)this.x + 0.5f), (float)((float)this.y + 0.5f), (float)other.getX(), (float)other.getY());
    }

    public float DistToProper(IsoMovingObject other) {
        return IsoUtils.DistanceTo((float)((float)this.x + 0.5f), (float)((float)this.y + 0.5f), (float)other.getX(), (float)other.getY());
    }

    public boolean isSafeToSpawn() {
        choices.clear();
        this.isSafeToSpawn(this, 0);
        if (choices.size() > 7) {
            choices.clear();
            return true;
        }
        choices.clear();
        return false;
    }

    public void isSafeToSpawn(IsoGridSquare sq, int depth) {
        if (depth > 5) {
            return;
        }
        choices.add(sq);
        if (sq.n != null && !choices.contains(sq.n)) {
            this.isSafeToSpawn(sq.n, depth + 1);
        }
        if (sq.s != null && !choices.contains(sq.s)) {
            this.isSafeToSpawn(sq.s, depth + 1);
        }
        if (sq.e != null && !choices.contains(sq.e)) {
            this.isSafeToSpawn(sq.e, depth + 1);
        }
        if (sq.w != null && !choices.contains(sq.w)) {
            this.isSafeToSpawn(sq.w, depth + 1);
        }
    }

    private void renderAttachedSpritesWithNoWallLighting(IsoObject obj, ColorInfo lightInfo, Consumer<TextureDraw> texdModifier) {
        if (obj.attachedAnimSprite == null || obj.attachedAnimSprite.isEmpty()) {
            return;
        }
        boolean needed = false;
        for (int i = 0; i < obj.attachedAnimSprite.size(); ++i) {
            IsoSpriteInstance s = obj.attachedAnimSprite.get(i);
            if (s.parentSprite == null || !s.parentSprite.properties.has(IsoFlagType.NoWallLighting)) continue;
            needed = true;
            break;
        }
        if (!needed) {
            return;
        }
        IsoGridSquare.defColorInfo.r = lightInfo.r;
        IsoGridSquare.defColorInfo.g = lightInfo.g;
        IsoGridSquare.defColorInfo.b = lightInfo.b;
        float fa = IsoGridSquare.defColorInfo.a;
        if (FBORenderCell.instance.isBlackedOutBuildingSquare(this)) {
            float fade = 1.0f - FBORenderCell.instance.getBlackedOutRoomFadeRatio(this);
            defColorInfo.set(IsoGridSquare.defColorInfo.r * fade, IsoGridSquare.defColorInfo.g * fade, IsoGridSquare.defColorInfo.b * fade, IsoGridSquare.defColorInfo.a);
        }
        if (DebugOptions.instance.fboRenderChunk.nolighting.getValue()) {
            defColorInfo.set(1.0f, 1.0f, 1.0f, IsoGridSquare.defColorInfo.a);
        }
        if (circleStencil) {
            IndieGL.enableStencilTest();
            IndieGL.enableAlphaTest();
            IndieGL.glAlphaFunc((int)516, (float)0.02f);
            IndieGL.glStencilFunc((int)517, (int)128, (int)128);
            for (int i = 0; i < obj.attachedAnimSprite.size(); ++i) {
                IsoSpriteInstance s = obj.attachedAnimSprite.get(i);
                if (s.parentSprite == null || !s.parentSprite.properties.has(IsoFlagType.NoWallLighting)) continue;
                IsoGridSquare.defColorInfo.a = s.alpha;
                s.render(obj, (float)this.x, (float)this.y, (float)this.z, obj.getForwardIsoDirection(), obj.offsetX, obj.offsetY + obj.getRenderYOffset() * (float)Core.tileScale, defColorInfo, true, texdModifier);
            }
            IndieGL.glStencilFunc((int)519, (int)255, (int)255);
        } else {
            for (int i = 0; i < obj.attachedAnimSprite.size(); ++i) {
                IsoSpriteInstance s = obj.attachedAnimSprite.get(i);
                if (s.parentSprite == null || !s.parentSprite.properties.has(IsoFlagType.NoWallLighting)) continue;
                IsoGridSquare.defColorInfo.a = s.alpha;
                s.render(obj, (float)this.x, (float)this.y, (float)this.z, obj.getForwardIsoDirection(), obj.offsetX, obj.offsetY + obj.getRenderYOffset() * (float)Core.tileScale, defColorInfo);
                s.update();
            }
        }
        IsoGridSquare.defColorInfo.r = 1.0f;
        IsoGridSquare.defColorInfo.g = 1.0f;
        IsoGridSquare.defColorInfo.b = 1.0f;
        IsoGridSquare.defColorInfo.a = fa;
    }

    private float calculateCutawayOutlineAlpha(int playerIndex, boolean north) {
        float outlineAlpha = 0.35f;
        IsoWindow window = this.getWindow(north);
        if (window != null) {
            float windowAlpha = PZMath.clamp_01((float)(window.getRenderInfo((int)playerIndex).targetAlpha * 2.0f));
            outlineAlpha = PZMath.clamp((float)(1.0f - windowAlpha), (float)0.0f, (float)0.35f);
        }
        return outlineAlpha;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void DoCutawayShader(IsoObject obj, IsoDirections dir, int cutawaySelf, int cutawayN, int cutawayS, int cutawayW, int cutawayE, boolean bHasDoorN, boolean bHasDoorW, boolean bHasWindowN, boolean bHasWindowW, WallShaper texdModifier) {
        Texture tex_1 = Texture.getSharedTexture((String)"media/wallcutaways.png", (int)3);
        Texture tex_2 = Texture.getSharedTexture((String)"media/windowframe_cutaways.png", (int)3);
        Texture tex_3 = Texture.getSharedTexture((String)"media/windowframe_cutaways_2.png", (int)3);
        Texture tex_4 = Texture.getSharedTexture((String)"media/windowframe_cutaways_3.png", (int)3);
        if (tex_1 == null || tex_1.getID() == -1 || tex_2 == null || tex_2.getID() == -1 || tex_3 == null || tex_3.getID() == -1 || tex_4 == null || tex_4.getID() == -1) {
            return;
        }
        Texture tex2 = tex_1;
        boolean noWallLighting = obj.sprite.getProperties().has(IsoFlagType.NoWallLighting);
        int playerIndex = IsoCamera.frameState.playerIndex;
        obj.getRenderInfo((int)playerIndex).cutaway = true;
        IsoGridSquare squareN = this.getAdjacentSquare(IsoDirections.N);
        IsoGridSquare squareS = this.getAdjacentSquare(IsoDirections.S);
        IsoGridSquare squareW = this.getAdjacentSquare(IsoDirections.W);
        IsoGridSquare squareE = this.getAdjacentSquare(IsoDirections.E);
        ColorInfo lightInfo = this.lightInfo[playerIndex];
        if (obj.getProperties().has(IsoPropertyType.GARAGE_DOOR)) {
            obj.renderWallTileOnly(dir, this.x, this.y, this.z, noWallLighting ? lightInfo : defColorInfo, null, (Consumer<TextureDraw>)texdModifier);
        }
        String cutawayHint = obj.getProperties().get("CutawayHint");
        String windowShape = obj.getProperties().get("WindowShape");
        try {
            SpriteRenderer.WallShaderTexRender wallShaderTexRender;
            int row;
            int windowShapeNum;
            int cutawayYAdjusted;
            CircleStencilShader shader = CircleStencilShader.instance;
            if (dir == IsoDirections.N || dir == IsoDirections.NW) {
                int cutawayNX = 700;
                if ((cutawaySelf & 1) != 0) {
                    if ((cutawayE & 1) == 0 && IsoGridSquare.hasCutawayCapableWallNorth(squareE)) {
                        cutawayNX = 444;
                    }
                    if ((cutawayW & 1) == 0 && IsoGridSquare.hasCutawayCapableWallNorth(squareW)) {
                        cutawayNX = 956;
                    } else if ((cutawayN & 2) == 0 && IsoGridSquare.hasCutawayCapableWallWest(squareN)) {
                        cutawayNX = 956;
                    }
                } else {
                    cutawayNX = (cutawayE & 1) == 0 ? 828 : 956;
                }
                cutawayYAdjusted = 0;
                if (!PerformanceSettings.fboRenderChunk || FBORenderCell.lowestCutawayObjectN == obj) {
                    if (bHasDoorN) {
                        obj.getRenderInfo((int)playerIndex).cutawayOutline = true;
                        cutawayYAdjusted = 904;
                        if (cutawayHint != null) {
                            if ("DoubleDoorLeft".equals(cutawayHint)) {
                                cutawayYAdjusted = 1130;
                            } else if ("DoubleDoorRight".equals(cutawayHint)) {
                                cutawayYAdjusted = 1356;
                            } else if ("GarageDoorLeft".equals(cutawayHint)) {
                                cutawayNX = 444;
                                cutawayYAdjusted = 1808;
                            } else if ("GarageDoorMiddle".equals(cutawayHint)) {
                                cutawayNX = 572;
                                cutawayYAdjusted = 1808;
                            } else if ("GarageDoorRight".equals(cutawayHint)) {
                                cutawayNX = 700;
                                cutawayYAdjusted = 1808;
                            }
                        }
                    } else if (bHasWindowN) {
                        obj.getRenderInfo((int)playerIndex).cutawayOutline = true;
                        cutawayYAdjusted = 226;
                        if (cutawayHint != null) {
                            if ("DoubleWindowLeft".equals(cutawayHint)) {
                                cutawayYAdjusted = 678;
                            } else if ("DoubleWindowRight".equals(cutawayHint)) {
                                cutawayYAdjusted = 452;
                            }
                        } else if (windowShape != null) {
                            windowShapeNum = PZMath.tryParseInt((String)windowShape, (int)-1);
                            if (windowShapeNum >= 1 && windowShapeNum <= 9) {
                                tex2 = tex_2;
                                row = windowShapeNum - 1;
                                cutawayYAdjusted = 226 * row;
                            } else if (windowShapeNum >= 10 && windowShapeNum <= 18) {
                                tex2 = tex_3;
                                row = windowShapeNum - 10;
                                cutawayYAdjusted = 226 * row;
                            } else if (windowShapeNum >= 19 && windowShapeNum <= 21) {
                                tex2 = tex_4;
                                row = windowShapeNum - 19;
                                cutawayYAdjusted = 226 * row;
                            }
                        }
                    }
                }
                colu = this.getVertLight(0, playerIndex);
                coll = this.getVertLight(1, playerIndex);
                colu2 = this.getVertLight(4, playerIndex);
                coll2 = this.getVertLight(5, playerIndex);
                if (FBORenderCell.instance.isBlackedOutBuildingSquare(this)) {
                    float fade = FBORenderCell.instance.getBlackedOutRoomFadeRatio(this);
                    colu = Color.lerpABGR((int)colu, (int)-16777216, (float)fade);
                    coll = Color.lerpABGR((int)coll, (int)-16777216, (float)fade);
                    colu2 = Color.lerpABGR((int)colu2, (int)-16777216, (float)fade);
                    coll2 = Color.lerpABGR((int)coll2, (int)-16777216, (float)fade);
                }
                if (Core.debug && DebugOptions.instance.debugDrawSkipWorldShading.getValue()) {
                    coll2 = -1;
                    colu2 = -1;
                    coll = -1;
                    colu = -1;
                    lightInfo = defColorInfo;
                }
                SpriteRenderer.instance.setCutawayTexture(tex2, cutawayNX, cutawayYAdjusted, 66, 226);
                if (dir == IsoDirections.N) {
                    Texture depthTexture = TileDepthMapManager.instance.getTextureForPreset(TileDepthMapManager.TileDepthPreset.NWall);
                    SpriteRenderer.instance.setCutawayTexture2(depthTexture, 0, 0, 0, 0);
                }
                if (dir == IsoDirections.NW) {
                    Texture depthTexture = TileDepthMapManager.instance.getTextureForPreset(TileDepthMapManager.TileDepthPreset.NWWall);
                    SpriteRenderer.instance.setCutawayTexture2(depthTexture, 0, 0, 0, 0);
                }
                wallShaderTexRender = dir == IsoDirections.N ? SpriteRenderer.WallShaderTexRender.All : SpriteRenderer.WallShaderTexRender.RightOnly;
                SpriteRenderer.instance.setExtraWallShaderParams(wallShaderTexRender);
                if (PerformanceSettings.fboRenderChunk && FBORenderCell.instance.renderWindowFrameOutline) {
                    obj.setAlpha(playerIndex, this.calculateCutawayOutlineAlpha(playerIndex, true));
                }
                texdModifier.col[0] = colu2;
                texdModifier.col[1] = coll2;
                texdModifier.col[2] = coll;
                texdModifier.col[3] = colu;
                if (!tex2.getTextureId().hasMipMaps()) {
                    IndieGL.glBlendFunc((int)770, (int)771);
                }
                obj.renderWallTileOnly(null, this.x, this.y, this.z, noWallLighting ? lightInfo : defColorInfo, (Shader)shader, (Consumer<TextureDraw>)texdModifier);
                if (PerformanceSettings.fboRenderChunk && !FBORenderCell.instance.renderWindowFrameOutline) {
                    CutawayAttachedModifier.instance.setDir(IsoDirections.N);
                    this.DoCutawayShaderAttached(obj, dir, tex2, noWallLighting, lightInfo, cutawayNX, cutawayYAdjusted, 66, 226, colu2, coll2, coll, colu, wallShaderTexRender);
                    CutawayAttachedModifier.instance.setDir(null);
                }
                if (!tex2.getTextureId().hasMipMaps()) {
                    IsoGridSquare.setBlendFunc();
                }
            }
            if (dir == IsoDirections.W || dir == IsoDirections.NW) {
                int cutawayWX = 512;
                if ((cutawayS & 2) != 0) {
                    if ((cutawayN & 2) == 0 && IsoGridSquare.hasCutawayCapableWallWest(squareN)) {
                        cutawayWX = 768;
                    } else if ((cutawayW & 1) == 0 && IsoGridSquare.hasCutawayCapableWallNorth(squareW)) {
                        cutawayWX = 768;
                    } else if ((cutawaySelf & 1) == 0 && IsoGridSquare.hasCutawayCapableWallNorth(this)) {
                        cutawayWX = 768;
                    }
                } else if ((cutawaySelf & 2) == 0) {
                    cutawayWX = 896;
                } else if ((cutawayN & 2) == 0 && IsoGridSquare.hasCutawayCapableWallWest(squareN)) {
                    cutawayWX = 768;
                } else if (IsoGridSquare.hasCutawayCapableWallWest(squareS)) {
                    cutawayWX = 256;
                } else if ((cutawayW & 1) == 0 && IsoGridSquare.hasCutawayCapableWallNorth(squareW)) {
                    cutawayWX = 768;
                }
                cutawayYAdjusted = 0;
                if (!PerformanceSettings.fboRenderChunk || FBORenderCell.lowestCutawayObjectW == obj) {
                    if (bHasDoorW) {
                        obj.getRenderInfo((int)playerIndex).cutawayOutline = true;
                        cutawayYAdjusted = 904;
                        if (cutawayHint != null) {
                            if ("GarageDoorLeft".equals(cutawayHint)) {
                                cutawayWX = 0;
                                cutawayYAdjusted = 1808;
                            } else if ("GarageDoorMiddle".equals(cutawayHint)) {
                                cutawayWX = 128;
                                cutawayYAdjusted = 1808;
                            } else if ("GarageDoorRight".equals(cutawayHint)) {
                                cutawayWX = 256;
                                cutawayYAdjusted = 1808;
                            } else if ("DoubleDoorLeft".equals(cutawayHint)) {
                                cutawayYAdjusted = 1356;
                            } else if ("DoubleDoorRight".equals(cutawayHint)) {
                                cutawayYAdjusted = 1130;
                            }
                        }
                    } else if (bHasWindowW) {
                        obj.getRenderInfo((int)playerIndex).cutawayOutline = true;
                        cutawayYAdjusted = 226;
                        if (cutawayHint != null) {
                            if ("DoubleWindowLeft".equals(cutawayHint)) {
                                cutawayYAdjusted = 452;
                            } else if ("DoubleWindowRight".equals(cutawayHint)) {
                                cutawayYAdjusted = 678;
                            }
                        } else if (windowShape != null) {
                            windowShapeNum = PZMath.tryParseInt((String)windowShape, (int)-1);
                            if (windowShapeNum >= 1 && windowShapeNum <= 9) {
                                tex2 = tex_2;
                                row = windowShapeNum - 1;
                                cutawayYAdjusted = 226 * row;
                            } else if (windowShapeNum >= 10 && windowShapeNum <= 18) {
                                tex2 = tex_3;
                                row = windowShapeNum - 10;
                                cutawayYAdjusted = 226 * row;
                            } else if (windowShapeNum >= 19 && windowShapeNum <= 21) {
                                tex2 = tex_4;
                                row = windowShapeNum - 19;
                                cutawayYAdjusted = 226 * row;
                            }
                        }
                    }
                }
                colu = this.getVertLight(0, playerIndex);
                coll = this.getVertLight(3, playerIndex);
                colu2 = this.getVertLight(4, playerIndex);
                coll2 = this.getVertLight(7, playerIndex);
                if (FBORenderCell.instance.isBlackedOutBuildingSquare(this)) {
                    float fade = FBORenderCell.instance.getBlackedOutRoomFadeRatio(this);
                    colu = Color.lerpABGR((int)colu, (int)-16777216, (float)fade);
                    coll = Color.lerpABGR((int)coll, (int)-16777216, (float)fade);
                    colu2 = Color.lerpABGR((int)colu2, (int)-16777216, (float)fade);
                    coll2 = Color.lerpABGR((int)coll2, (int)-16777216, (float)fade);
                }
                if (Core.debug && DebugOptions.instance.debugDrawSkipWorldShading.getValue()) {
                    coll2 = -1;
                    colu2 = -1;
                    coll = -1;
                    colu = -1;
                    lightInfo = defColorInfo;
                }
                SpriteRenderer.instance.setCutawayTexture(tex2, cutawayWX, cutawayYAdjusted, 66, 226);
                if (dir == IsoDirections.W) {
                    Texture depthTexture = TileDepthMapManager.instance.getTextureForPreset(TileDepthMapManager.TileDepthPreset.WWall);
                    SpriteRenderer.instance.setCutawayTexture2(depthTexture, 0, 0, 0, 0);
                }
                if (dir == IsoDirections.NW) {
                    Texture depthTexture = TileDepthMapManager.instance.getTextureForPreset(TileDepthMapManager.TileDepthPreset.NWWall);
                    SpriteRenderer.instance.setCutawayTexture2(depthTexture, 0, 0, 0, 0);
                }
                wallShaderTexRender = dir == IsoDirections.W ? SpriteRenderer.WallShaderTexRender.All : SpriteRenderer.WallShaderTexRender.LeftOnly;
                SpriteRenderer.instance.setExtraWallShaderParams(wallShaderTexRender);
                if (PerformanceSettings.fboRenderChunk && FBORenderCell.instance.renderWindowFrameOutline) {
                    obj.setAlpha(playerIndex, this.calculateCutawayOutlineAlpha(playerIndex, false));
                }
                texdModifier.col[0] = coll2;
                texdModifier.col[1] = colu2;
                texdModifier.col[2] = colu;
                texdModifier.col[3] = coll;
                if (!tex2.getTextureId().hasMipMaps()) {
                    IndieGL.glBlendFunc((int)770, (int)771);
                }
                obj.renderWallTileOnly(null, this.x, this.y, this.z, noWallLighting ? lightInfo : defColorInfo, (Shader)shader, (Consumer<TextureDraw>)texdModifier);
                if (PerformanceSettings.fboRenderChunk && !FBORenderCell.instance.renderWindowFrameOutline) {
                    CutawayAttachedModifier.instance.setDir(IsoDirections.W);
                    this.DoCutawayShaderAttached(obj, dir, tex2, noWallLighting, lightInfo, cutawayWX, cutawayYAdjusted, 66, 226, coll2, colu2, colu, coll, wallShaderTexRender);
                    CutawayAttachedModifier.instance.setDir(null);
                }
                if (!tex2.getTextureId().hasMipMaps()) {
                    IsoGridSquare.setBlendFunc();
                }
            }
            if (dir == IsoDirections.SE) {
                int cutawaySEX = 1084;
                if ((cutawayW & 1) == 0 && IsoGridSquare.hasCutawayCapableWallNorth(squareW)) {
                    cutawaySEX = 1212;
                } else if ((cutawayN & 2) == 0 && IsoGridSquare.hasCutawayCapableWallWest(squareN)) {
                    cutawaySEX = 1212;
                }
                cutawayYAdjusted = 0;
                SpriteRenderer.instance.setCutawayTexture(tex2, cutawaySEX, 0, 6, 196);
                Texture depthTexture = TileDepthMapManager.instance.getTextureForPreset(TileDepthMapManager.TileDepthPreset.SEWall);
                SpriteRenderer.instance.setCutawayTexture2(depthTexture, 0, 0, 0, 0);
                SpriteRenderer.instance.setExtraWallShaderParams(SpriteRenderer.WallShaderTexRender.All);
                colu = this.getVertLight(0, playerIndex);
                coll = this.getVertLight(3, playerIndex);
                colu2 = this.getVertLight(4, playerIndex);
                coll2 = this.getVertLight(7, playerIndex);
                if (FBORenderCell.instance.isBlackedOutBuildingSquare(this)) {
                    float fade = FBORenderCell.instance.getBlackedOutRoomFadeRatio(this);
                    colu = Color.lerpABGR((int)colu, (int)-16777216, (float)fade);
                    coll = Color.lerpABGR((int)coll, (int)-16777216, (float)fade);
                    colu2 = Color.lerpABGR((int)colu2, (int)-16777216, (float)fade);
                    coll2 = Color.lerpABGR((int)coll2, (int)-16777216, (float)fade);
                }
                if (Core.debug && DebugOptions.instance.debugDrawSkipWorldShading.getValue()) {
                    coll2 = -1;
                    colu2 = -1;
                    coll = -1;
                    colu = -1;
                    lightInfo = defColorInfo;
                }
                texdModifier.col[0] = coll2;
                texdModifier.col[1] = colu2;
                texdModifier.col[2] = colu;
                texdModifier.col[3] = coll;
                if (!tex2.getTextureId().hasMipMaps()) {
                    IndieGL.glBlendFunc((int)770, (int)771);
                }
                obj.renderWallTileOnly(null, this.x, this.y, this.z, noWallLighting ? lightInfo : defColorInfo, (Shader)shader, (Consumer<TextureDraw>)texdModifier);
                if (PerformanceSettings.fboRenderChunk) {
                    CutawayAttachedModifier.instance.setDir(IsoDirections.SE);
                    this.DoCutawayShaderAttached(obj, dir, tex2, noWallLighting, lightInfo, cutawaySEX, 0, 66, 226, coll2, colu2, colu, coll, SpriteRenderer.WallShaderTexRender.All);
                    CutawayAttachedModifier.instance.setDir(null);
                }
                if (!tex2.getTextureId().hasMipMaps()) {
                    IsoGridSquare.setBlendFunc();
                }
            }
        }
        finally {
            SpriteRenderer.instance.setExtraWallShaderParams(null);
            SpriteRenderer.instance.clearCutawayTexture();
            SpriteRenderer.instance.clearUseVertColorsArray();
        }
        if (!PerformanceSettings.fboRenderChunk) {
            obj.renderAttachedAndOverlaySprites(obj.getForwardIsoDirection(), this.x, this.y, this.z, noWallLighting ? lightInfo : defColorInfo, false, !noWallLighting, null, (Consumer<TextureDraw>)texdModifier);
        }
    }

    private void DoCutawayShaderAttached(IsoObject obj, IsoDirections dir, Texture tex2, boolean noWallLighting, ColorInfo lightInfo, int cutawayX, int cutawayY, int cutawayW, int cutawayH, int col0, int col1, int col2, int col3, SpriteRenderer.WallShaderTexRender wallShaderTexRender) {
        if (!DebugOptions.instance.terrain.renderTiles.isoGridSquare.walls.attachedSprites.getValue()) {
            return;
        }
        if (SceneShaderStore.cutawayAttachedShader == null) {
            return;
        }
        SpriteRenderer.instance.setExtraWallShaderParams(null);
        SpriteRenderer.instance.clearCutawayTexture();
        SpriteRenderer.instance.clearUseVertColorsArray();
        float farDepthZ = IsoDepthHelper.getSquareDepthData((int)PZMath.fastfloor((float)IsoCamera.frameState.camCharacterX), (int)PZMath.fastfloor((float)IsoCamera.frameState.camCharacterY), (float)((float)this.x), (float)((float)this.y), (float)((float)this.z)).depthStart;
        float zPlusOne = (float)this.z + 1.0f;
        float frontDepthZ = IsoDepthHelper.getSquareDepthData((int)PZMath.fastfloor((float)IsoCamera.frameState.camCharacterX), (int)PZMath.fastfloor((float)IsoCamera.frameState.camCharacterY), (float)((float)(this.x + 1)), (float)((float)(this.y + 1)), (float)zPlusOne).depthStart;
        if (!FBORenderCell.instance.renderTranslucentOnly) {
            int chunksPerWidth = 8;
            IsoDepthHelper.Results result = IsoDepthHelper.getChunkDepthData((int)PZMath.fastfloor((float)(IsoCamera.frameState.camCharacterX / 8.0f)), (int)PZMath.fastfloor((float)(IsoCamera.frameState.camCharacterY / 8.0f)), (int)PZMath.fastfloor((float)((float)this.x / 8.0f)), (int)PZMath.fastfloor((float)((float)this.y / 8.0f)), (int)PZMath.fastfloor((float)this.z));
            float chunkDepth = result.depthStart;
            farDepthZ -= chunkDepth;
            frontDepthZ -= chunkDepth;
        }
        ShaderUniformSetter uniforms = ShaderUniformSetter.uniform1f((Shader)SceneShaderStore.cutawayAttachedShader, (String)"zDepth", (float)frontDepthZ);
        uniforms.setNext(ShaderUniformSetter.uniform1f((Shader)SceneShaderStore.cutawayAttachedShader, (String)"zDepthBlendZ", (float)frontDepthZ)).setNext(ShaderUniformSetter.uniform1f((Shader)SceneShaderStore.cutawayAttachedShader, (String)"zDepthBlendToZ", (float)farDepthZ));
        IndieGL.pushShader((Shader)SceneShaderStore.cutawayAttachedShader, (ShaderUniformSetter)uniforms);
        CutawayAttachedModifier.instance.setVertColors(col0, col1, col2, col3);
        CutawayAttachedModifier.instance.setupWallDepth(obj.getSprite(), dir, tex2, cutawayX, cutawayY, cutawayW, cutawayH, wallShaderTexRender);
        IndieGL.glDefaultBlendFunc();
        obj.renderAttachedAndOverlaySprites(null, this.x, this.y, this.z, noWallLighting ? lightInfo : defColorInfo, true, !noWallLighting, null, (Consumer<TextureDraw>)CutawayAttachedModifier.instance);
        this.renderAttachedSpritesWithNoWallLighting(obj, lightInfo, (Consumer<TextureDraw>)CutawayAttachedModifier.instance);
        IndieGL.popShader((Shader)SceneShaderStore.cutawayAttachedShader);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void DoCutawayShaderSprite(IsoSprite sprite, IsoDirections dir, int cutawaySelf, int cutawayN, int cutawayS, int cutawayW, int cutawayE) {
        CutawayNoDepthShader shader = CutawayNoDepthShader.getInstance();
        WallShaperWhole texdModifier = WallShaperWhole.instance;
        int playerIndex = IsoCamera.frameState.playerIndex;
        Texture tex2 = Texture.getSharedTexture((String)"media/wallcutaways.png", (int)3);
        if (tex2 == null || tex2.getID() == -1) {
            return;
        }
        IsoGridSquare squareN = this.getAdjacentSquare(IsoDirections.N);
        IsoGridSquare squareS = this.getAdjacentSquare(IsoDirections.S);
        IsoGridSquare squareW = this.getAdjacentSquare(IsoDirections.W);
        IsoGridSquare squareE = this.getAdjacentSquare(IsoDirections.E);
        int tileScale = 2 / Core.tileScale;
        try {
            int cutawaySEX;
            Texture texture = sprite.getTextureForCurrentFrame(dir);
            if (texture == null) {
                return;
            }
            float xOffset = 0.0f;
            float offsetY = texture.getOffsetY();
            int wOffset = 0;
            int hOffset = 226 - texture.getHeight() * tileScale;
            if (dir != IsoDirections.NW) {
                wOffset = 66 - texture.getWidth() * tileScale;
            }
            if (sprite.isWallSE()) {
                wOffset = 6 - texture.getWidth() * tileScale;
                hOffset = 196 - texture.getHeight() * tileScale;
            }
            if (sprite.name.contains("fencing_01_11")) {
                xOffset = 1.0f;
            } else if (sprite.name.contains("carpentry_02_80")) {
                xOffset = 1.0f;
            } else if (sprite.name.contains("spiffos_01_71")) {
                xOffset = -24.0f;
            } else if (sprite.name.contains("location_community_medical")) {
                spriteName = sprite.name.replaceAll("(.*)_", "");
                spriteID = Integer.parseInt(spriteName);
                switch (spriteID) {
                    case 45: 
                    case 46: 
                    case 47: 
                    case 147: 
                    case 148: 
                    case 149: {
                        xOffset = -3.0f;
                    }
                }
            } else if (sprite.name.contains("walls_exterior_roofs")) {
                spriteName = sprite.name.replaceAll("(.*)_", "");
                spriteID = Integer.parseInt(spriteName);
                if (spriteID == 4) {
                    xOffset = -60.0f;
                } else if (spriteID == 17) {
                    xOffset = -46.0f;
                } else if (spriteID == 28 && !sprite.name.contains("03")) {
                    xOffset = -60.0f;
                } else if (spriteID == 41) {
                    xOffset = -46.0f;
                }
            }
            if (dir == IsoDirections.N || dir == IsoDirections.NW) {
                int cutawayNX = 700;
                cutawaySEX = 1084;
                if ((cutawaySelf & 1) == 0) {
                    cutawayNX = 828;
                    cutawaySEX = 1212;
                } else if ((cutawaySelf & 1) != 0) {
                    cutawaySEX = 1212;
                    if ((cutawayE & 1) == 0 && IsoGridSquare.hasCutawayCapableWallNorth(squareE)) {
                        cutawayNX = 444;
                    }
                    if ((cutawayW & 1) == 0 && IsoGridSquare.hasCutawayCapableWallNorth(squareW)) {
                        cutawayNX = 956;
                    } else if ((cutawayN & 2) == 0 && IsoGridSquare.hasCutawayCapableWallWest(squareN)) {
                        cutawayNX = 956;
                    }
                } else {
                    cutawayNX = (cutawayE & 1) == 0 ? 828 : 956;
                }
                colu = this.getVertLight(0, playerIndex);
                coll = this.getVertLight(1, playerIndex);
                colu2 = this.getVertLight(4, playerIndex);
                coll2 = this.getVertLight(5, playerIndex);
                if (sprite.isWallSE()) {
                    SpriteRenderer.instance.setCutawayTexture(tex2, cutawaySEX + (int)xOffset, 0 + (int)offsetY, 6 - wOffset, 196 - hOffset);
                } else {
                    SpriteRenderer.instance.setCutawayTexture(tex2, cutawayNX + (int)xOffset, 0 + (int)offsetY, 66 - wOffset, 226 - hOffset);
                }
                if (dir == IsoDirections.N) {
                    SpriteRenderer.instance.setExtraWallShaderParams(SpriteRenderer.WallShaderTexRender.All);
                } else {
                    SpriteRenderer.instance.setExtraWallShaderParams(SpriteRenderer.WallShaderTexRender.RightOnly);
                }
                texdModifier.col[0] = colu2;
                texdModifier.col[1] = coll2;
                texdModifier.col[2] = coll;
                texdModifier.col[3] = colu;
                IndieGL.bindShader((Shader)shader, (Object)sprite, (Object)dir, (Object)texdModifier, (lSprite, lDir, lTexdModifier) -> lSprite.render(null, (float)this.x, (float)this.y, (float)this.z, lDir, WeatherFxMask.offsetX, WeatherFxMask.offsetY, defColorInfo, false, (Consumer)lTexdModifier));
            }
            if (dir == IsoDirections.W || dir == IsoDirections.NW) {
                Texture depthTexture;
                int cutawayWX = 512;
                cutawaySEX = 1084;
                if ((cutawaySelf & 2) == 0) {
                    cutawayWX = 896;
                    cutawaySEX = 1212;
                } else if ((cutawayS & 2) != 0) {
                    if ((cutawayN & 2) == 0 && IsoGridSquare.hasCutawayCapableWallWest(squareN)) {
                        cutawayWX = 768;
                        cutawaySEX = 1212;
                    } else if ((cutawayW & 1) == 0 && IsoGridSquare.hasCutawayCapableWallNorth(squareW)) {
                        cutawayWX = 768;
                        cutawaySEX = 1212;
                    }
                } else if ((cutawaySelf & 2) == 0) {
                    cutawayWX = 896;
                    cutawaySEX = 1212;
                } else if ((cutawayN & 2) == 0 && IsoGridSquare.hasCutawayCapableWallWest(squareN)) {
                    cutawayWX = 768;
                } else if (IsoGridSquare.hasCutawayCapableWallWest(squareS)) {
                    cutawayWX = 256;
                }
                colu = this.getVertLight(0, playerIndex);
                coll = this.getVertLight(3, playerIndex);
                colu2 = this.getVertLight(4, playerIndex);
                coll2 = this.getVertLight(7, playerIndex);
                if (sprite.isWallSE()) {
                    SpriteRenderer.instance.setCutawayTexture(tex2, cutawaySEX + (int)xOffset, 0 + (int)offsetY, 6 - wOffset, 196 - hOffset);
                } else {
                    SpriteRenderer.instance.setCutawayTexture(tex2, cutawayWX + (int)xOffset, 0 + (int)offsetY, 66 - wOffset, 226 - hOffset);
                }
                if (dir == IsoDirections.W) {
                    depthTexture = TileDepthMapManager.instance.getTextureForPreset(TileDepthMapManager.TileDepthPreset.WWall);
                    SpriteRenderer.instance.setCutawayTexture2(depthTexture, 0, 0, 0, 0);
                }
                if (dir == IsoDirections.NW) {
                    depthTexture = TileDepthMapManager.instance.getTextureForPreset(TileDepthMapManager.TileDepthPreset.NWWall);
                    SpriteRenderer.instance.setCutawayTexture2(depthTexture, 0, 0, 0, 0);
                }
                if (sprite.isWallSE()) {
                    depthTexture = TileDepthMapManager.instance.getTextureForPreset(TileDepthMapManager.TileDepthPreset.SEWall);
                    SpriteRenderer.instance.setCutawayTexture2(depthTexture, 0, 0, 0, 0);
                }
                if (dir == IsoDirections.W) {
                    SpriteRenderer.instance.setExtraWallShaderParams(SpriteRenderer.WallShaderTexRender.All);
                } else {
                    SpriteRenderer.instance.setExtraWallShaderParams(SpriteRenderer.WallShaderTexRender.LeftOnly);
                }
                texdModifier.col[0] = coll2;
                texdModifier.col[1] = colu2;
                texdModifier.col[2] = colu;
                texdModifier.col[3] = coll;
                IndieGL.bindShader((Shader)shader, (Object)sprite, (Object)dir, (Object)texdModifier, (lSprite, lDir, lTexdModifier) -> lSprite.render(null, (float)this.x, (float)this.y, (float)this.z, lDir, WeatherFxMask.offsetX, WeatherFxMask.offsetY, defColorInfo, false, (Consumer)lTexdModifier));
            }
            if (dir == IsoDirections.SE) {
                int cutawaySEX2 = 1084;
                if ((cutawayW & 1) == 0 && IsoGridSquare.hasCutawayCapableWallNorth(squareW)) {
                    cutawaySEX2 = 1212;
                } else if ((cutawayN & 2) == 0 && IsoGridSquare.hasCutawayCapableWallWest(squareN)) {
                    cutawaySEX2 = 1212;
                }
                SpriteRenderer.instance.setCutawayTexture(tex2, cutawaySEX2 + (int)xOffset, 0 + (int)offsetY, 6 - wOffset, 196 - hOffset);
                Texture depthTexture = TileDepthMapManager.instance.getTextureForPreset(TileDepthMapManager.TileDepthPreset.SEWall);
                SpriteRenderer.instance.setCutawayTexture2(depthTexture, 0, 0, 0, 0);
                SpriteRenderer.instance.setExtraWallShaderParams(SpriteRenderer.WallShaderTexRender.All);
                colu = this.getVertLight(0, playerIndex);
                coll = this.getVertLight(3, playerIndex);
                colu2 = this.getVertLight(4, playerIndex);
                texdModifier.col[0] = coll2 = this.getVertLight(7, playerIndex);
                texdModifier.col[1] = colu2;
                texdModifier.col[2] = colu;
                texdModifier.col[3] = coll;
                IndieGL.bindShader((Shader)shader, (Object)sprite, (Object)dir, (Object)texdModifier, (lSprite, lDir, lTexdModifier) -> lSprite.render(null, (float)this.x, (float)this.y, (float)this.z, lDir, WeatherFxMask.offsetX, WeatherFxMask.offsetY, defColorInfo, false, (Consumer)lTexdModifier));
            }
        }
        finally {
            SpriteRenderer.instance.setExtraWallShaderParams(null);
            SpriteRenderer.instance.clearCutawayTexture();
            SpriteRenderer.instance.clearUseVertColorsArray();
        }
    }

    public int DoWallLightingNW(IsoObject obj, int stenciled, int cutawaySelf, int cutawayN, int cutawayS, int cutawayW, int cutawayE, boolean bHasDoorN, boolean bHasDoorW, boolean bHasWindowN, boolean bHasWindowW, Shader wallRenderShader) {
        if (!DebugOptions.instance.terrain.renderTiles.isoGridSquare.walls.nw.getValue()) {
            return stenciled;
        }
        boolean isCutaway = cutawaySelf != 0 && DebugOptions.instance.terrain.renderTiles.cutaway.getValue();
        IsoDirections cutawayDirection = IsoDirections.NW;
        int playerIndex = IsoCamera.frameState.playerIndex;
        colu = this.getVertLight(0, playerIndex);
        coll = this.getVertLight(3, playerIndex);
        colr = this.getVertLight(1, playerIndex);
        colu2 = this.getVertLight(4, playerIndex);
        coll2 = this.getVertLight(7, playerIndex);
        colr2 = this.getVertLight(5, playerIndex);
        if (FBORenderCell.instance.isBlackedOutBuildingSquare(this)) {
            float fade = FBORenderCell.instance.getBlackedOutRoomFadeRatio(this);
            colu = Color.lerpABGR((int)colu, (int)-16777216, (float)fade);
            coll = Color.lerpABGR((int)coll, (int)-16777216, (float)fade);
            colr = Color.lerpABGR((int)colr, (int)-16777216, (float)fade);
            colu2 = Color.lerpABGR((int)colu2, (int)-16777216, (float)fade);
            coll2 = Color.lerpABGR((int)coll2, (int)-16777216, (float)fade);
            colr2 = Color.lerpABGR((int)colr2, (int)-16777216, (float)fade);
        }
        if (DebugOptions.instance.terrain.renderTiles.isoGridSquare.walls.lightingDebug.getValue()) {
            colu = -65536;
            coll = -16711936;
            colr = -16711681;
            colu2 = -16776961;
            coll2 = -65281;
            colr2 = -256;
        }
        boolean circleStencil = IsoGridSquare.circleStencil;
        if (this.z != PZMath.fastfloor((float)IsoCamera.getCameraCharacterZ())) {
            circleStencil = false;
        }
        boolean isDoorN = obj.sprite.getTileType() == IsoObjectType.doorFrN || obj.sprite.getTileType() == IsoObjectType.doorN;
        boolean isDoorW = obj.sprite.getTileType() == IsoObjectType.doorFrW || obj.sprite.getTileType() == IsoObjectType.doorW;
        boolean isWindowN = false;
        boolean isWindowW = false;
        boolean noWallLighting = (isDoorN || isDoorW) && isCutaway || obj.sprite.getProperties().has(IsoFlagType.NoWallLighting);
        if ((circleStencil = this.calculateWallAlphaAndCircleStencilCorner(obj, cutawaySelf, bHasDoorN, bHasDoorW, bHasWindowN, bHasWindowW, circleStencil, playerIndex, isDoorN, isDoorW, false, false)) && isCutaway) {
            this.DoCutawayShader(obj, cutawayDirection, cutawaySelf, cutawayN, cutawayS, cutawayW, cutawayE, bHasDoorN, bHasDoorW, bHasWindowN, bHasWindowW, (WallShaper)WallShaperWhole.instance);
            wallCutawayN = true;
            wallCutawayW = true;
            return stenciled;
        }
        WallShaperWhole.instance.col[0] = colu2;
        WallShaperWhole.instance.col[1] = colr2;
        WallShaperWhole.instance.col[2] = colr;
        WallShaperWhole.instance.col[3] = colu;
        WallShaperN wallShaperN = WallShaperN.instance;
        wallShaperN.col[0] = colu2;
        wallShaperN.col[1] = colr2;
        wallShaperN.col[2] = colr;
        wallShaperN.col[3] = colu;
        TileSeamModifier.instance.setVertColors(colu2, colr2, colr, colu);
        stenciled = this.performDrawWall(obj, cutawayDirection, stenciled, playerIndex, noWallLighting, (Consumer<TextureDraw>)wallShaperN, wallRenderShader);
        WallShaperWhole.instance.col[0] = coll2;
        WallShaperWhole.instance.col[1] = colu2;
        WallShaperWhole.instance.col[2] = colu;
        WallShaperWhole.instance.col[3] = coll;
        WallShaperW wallShaperW = WallShaperW.instance;
        wallShaperW.col[0] = coll2;
        wallShaperW.col[1] = colu2;
        wallShaperW.col[2] = colu;
        wallShaperW.col[3] = coll;
        TileSeamModifier.instance.setVertColors(coll2, colu2, colu, coll);
        stenciled = this.performDrawWall(obj, cutawayDirection, stenciled, playerIndex, noWallLighting, (Consumer<TextureDraw>)wallShaperW, wallRenderShader);
        return stenciled;
    }

    public int DoWallLightingN(IsoObject obj, int stenciled, int cutawaySelf, int cutawayN, int cutawayS, int cutawayW, int cutawayE, boolean bHasDoorN, boolean bHasWindowN, Shader wallRenderShader) {
        if (!DebugOptions.instance.terrain.renderTiles.isoGridSquare.walls.n.getValue()) {
            return stenciled;
        }
        boolean bHasDoorW = false;
        boolean bHasWindowW = false;
        boolean hasNoDoor = !bHasDoorN;
        boolean hasNoWindow = !bHasWindowN;
        IsoObjectType doorFrType = IsoObjectType.doorFrN;
        IsoObjectType doorType = IsoObjectType.doorN;
        boolean isCutaway = (cutawaySelf & 1) != 0 && DebugOptions.instance.terrain.renderTiles.cutaway.getValue();
        IsoFlagType transparentFlag = IsoFlagType.transparentN;
        IsoFlagType transparentWindowFlag = IsoFlagType.WindowN;
        IsoFlagType hoppableType = IsoFlagType.HoppableN;
        IsoDirections cutawayDirection = IsoDirections.N;
        boolean circleStencil = IsoGridSquare.circleStencil;
        int playerIndex = IsoCamera.frameState.playerIndex;
        colu = this.getVertLight(0, playerIndex);
        coll = this.getVertLight(1, playerIndex);
        colu2 = this.getVertLight(4, playerIndex);
        coll2 = this.getVertLight(5, playerIndex);
        float r1 = Color.getRedChannelFromABGR((int)colu2);
        float g1 = Color.getGreenChannelFromABGR((int)colu2);
        float b1 = Color.getBlueChannelFromABGR((int)colu2);
        float r2 = Color.getRedChannelFromABGR((int)colu);
        float g2 = Color.getGreenChannelFromABGR((int)colu);
        float b2 = Color.getBlueChannelFromABGR((int)colu);
        float f = 0.045f;
        float r = PZMath.clamp((float)(r2 * (r2 >= r1 ? 1.045f : 0.955f)), (float)0.0f, (float)1.0f);
        float g = PZMath.clamp((float)(g2 * (g2 >= g1 ? 1.045f : 0.955f)), (float)0.0f, (float)1.0f);
        float b = PZMath.clamp((float)(b2 * (b2 >= b1 ? 1.045f : 0.955f)), (float)0.0f, (float)1.0f);
        colu = Color.colorToABGR((float)r, (float)g, (float)b, (float)1.0f);
        r1 = Color.getRedChannelFromABGR((int)coll);
        g1 = Color.getGreenChannelFromABGR((int)coll);
        b1 = Color.getBlueChannelFromABGR((int)coll);
        r2 = Color.getRedChannelFromABGR((int)coll2);
        g2 = Color.getGreenChannelFromABGR((int)coll2);
        b2 = Color.getBlueChannelFromABGR((int)coll2);
        r = PZMath.clamp((float)(r2 * (r2 >= r1 ? 1.045f : 0.955f)), (float)0.0f, (float)1.0f);
        g = PZMath.clamp((float)(g2 * (g2 >= g1 ? 1.045f : 0.955f)), (float)0.0f, (float)1.0f);
        b = PZMath.clamp((float)(b2 * (b2 >= b1 ? 1.045f : 0.955f)), (float)0.0f, (float)1.0f);
        coll2 = Color.colorToABGR((float)r, (float)g, (float)b, (float)1.0f);
        if (FBORenderCell.instance.isBlackedOutBuildingSquare(this)) {
            float fade = FBORenderCell.instance.getBlackedOutRoomFadeRatio(this);
            colu = Color.lerpABGR((int)colu, (int)-16777216, (float)fade);
            coll = Color.lerpABGR((int)coll, (int)-16777216, (float)fade);
            colu2 = Color.lerpABGR((int)colu2, (int)-16777216, (float)fade);
            coll2 = Color.lerpABGR((int)coll2, (int)-16777216, (float)fade);
        }
        if (DebugOptions.instance.terrain.renderTiles.isoGridSquare.walls.lightingDebug.getValue()) {
            colu = -65536;
            coll = -16711936;
            colu2 = -16776961;
            coll2 = -65281;
        }
        WallShaperWhole wallShaperWhole = WallShaperWhole.instance;
        wallShaperWhole.col[0] = colu2;
        wallShaperWhole.col[1] = coll2;
        wallShaperWhole.col[2] = coll;
        wallShaperWhole.col[3] = colu;
        TileSeamModifier.instance.setVertColors(coll2, colu2, colu, coll);
        return this.performDrawWallSegmentSingle(obj, stenciled, cutawaySelf, cutawayN, cutawayS, cutawayW, cutawayE, false, false, bHasDoorN, bHasWindowN, hasNoDoor, hasNoWindow, doorFrType, doorType, isCutaway, transparentFlag, transparentWindowFlag, hoppableType, cutawayDirection, circleStencil, wallShaperWhole, wallRenderShader);
    }

    public int DoWallLightingW(IsoObject obj, int stenciled, int cutawaySelf, int cutawayN, int cutawayS, int cutawayW, int cutawayE, boolean bHasDoorW, boolean bHasWindowW, Shader wallRenderShader) {
        if (!DebugOptions.instance.terrain.renderTiles.isoGridSquare.walls.w.getValue()) {
            return stenciled;
        }
        boolean bHasDoorN = false;
        boolean bHasWindowN = false;
        boolean hasNoDoor = !bHasDoorW;
        boolean hasNoWindow = !bHasWindowW;
        IsoObjectType doorFrType = IsoObjectType.doorFrW;
        IsoObjectType doorType = IsoObjectType.doorW;
        boolean isWallSE = obj.isWallSE();
        boolean isCutawaySE = false;
        if (isWallSE) {
            if ((cutawayW & 1) != 0) {
                isCutawaySE = true;
            }
            if ((cutawayN & 2) != 0) {
                isCutawaySE = true;
            }
        }
        boolean isCutaway = ((cutawaySelf & 2) != 0 || isCutawaySE) && DebugOptions.instance.terrain.renderTiles.cutaway.getValue();
        IsoFlagType transparentFlag = IsoFlagType.transparentW;
        IsoFlagType transparentWindowFlag = IsoFlagType.WindowW;
        IsoFlagType hoppableType = IsoFlagType.HoppableW;
        IsoDirections cutawayDirection = isWallSE ? IsoDirections.SE : IsoDirections.W;
        boolean circleStencil = IsoGridSquare.circleStencil;
        int playerIndex = IsoCamera.frameState.playerIndex;
        colu = this.getVertLight(0, playerIndex);
        coll = this.getVertLight(3, playerIndex);
        colu2 = this.getVertLight(4, playerIndex);
        coll2 = this.getVertLight(7, playerIndex);
        float r1 = Color.getRedChannelFromABGR((int)colu2);
        float g1 = Color.getGreenChannelFromABGR((int)colu2);
        float b1 = Color.getBlueChannelFromABGR((int)colu2);
        float r2 = Color.getRedChannelFromABGR((int)colu);
        float g2 = Color.getGreenChannelFromABGR((int)colu);
        float b2 = Color.getBlueChannelFromABGR((int)colu);
        float f = 0.045f;
        float r = PZMath.clamp((float)(r2 * (r2 >= r1 ? 1.045f : 0.955f)), (float)0.0f, (float)1.0f);
        float g = PZMath.clamp((float)(g2 * (g2 >= g1 ? 1.045f : 0.955f)), (float)0.0f, (float)1.0f);
        float b = PZMath.clamp((float)(b2 * (b2 >= b1 ? 1.045f : 0.955f)), (float)0.0f, (float)1.0f);
        colu = Color.colorToABGR((float)r, (float)g, (float)b, (float)1.0f);
        r1 = Color.getRedChannelFromABGR((int)coll);
        g1 = Color.getGreenChannelFromABGR((int)coll);
        b1 = Color.getBlueChannelFromABGR((int)coll);
        r2 = Color.getRedChannelFromABGR((int)coll2);
        g2 = Color.getGreenChannelFromABGR((int)coll2);
        b2 = Color.getBlueChannelFromABGR((int)coll2);
        r = PZMath.clamp((float)(r2 * (r2 >= r1 ? 1.045f : 0.955f)), (float)0.0f, (float)1.0f);
        g = PZMath.clamp((float)(g2 * (g2 >= g1 ? 1.045f : 0.955f)), (float)0.0f, (float)1.0f);
        b = PZMath.clamp((float)(b2 * (b2 >= b1 ? 1.045f : 0.955f)), (float)0.0f, (float)1.0f);
        coll2 = Color.colorToABGR((float)r, (float)g, (float)b, (float)1.0f);
        if (FBORenderCell.instance.isBlackedOutBuildingSquare(this)) {
            float fade = FBORenderCell.instance.getBlackedOutRoomFadeRatio(this);
            colu = Color.lerpABGR((int)colu, (int)-16777216, (float)fade);
            coll = Color.lerpABGR((int)coll, (int)-16777216, (float)fade);
            colu2 = Color.lerpABGR((int)colu2, (int)-16777216, (float)fade);
            coll2 = Color.lerpABGR((int)coll2, (int)-16777216, (float)fade);
        }
        if (DebugOptions.instance.terrain.renderTiles.isoGridSquare.walls.lightingDebug.getValue()) {
            colu = -65536;
            coll = -16711936;
            colu2 = -16776961;
            coll2 = -65281;
        }
        WallShaperWhole wallShaperWhole = WallShaperWhole.instance;
        wallShaperWhole.col[0] = coll2;
        wallShaperWhole.col[1] = colu2;
        wallShaperWhole.col[2] = colu;
        wallShaperWhole.col[3] = coll;
        TileSeamModifier.instance.setVertColors(coll2, colu2, colu, coll);
        return this.performDrawWallSegmentSingle(obj, stenciled, cutawaySelf, cutawayN, cutawayS, cutawayW, cutawayE, bHasDoorW, bHasWindowW, false, false, hasNoDoor, hasNoWindow, doorFrType, doorType, isCutaway, transparentFlag, transparentWindowFlag, hoppableType, cutawayDirection, circleStencil, wallShaperWhole, wallRenderShader);
    }

    private int performDrawWallSegmentSingle(IsoObject obj, int stenciled, int cutawaySelf, int cutawayN, int cutawayS, int cutawayW, int cutawayE, boolean bHasDoorW, boolean bHasWindowW, boolean bHasDoorN, boolean bHasWindowN, boolean hasNoDoor, boolean hasNoWindow, IsoObjectType doorFrType, IsoObjectType doorType, boolean isCutaway, IsoFlagType transparentFlag, IsoFlagType transparentWindowFlag, IsoFlagType hoppableType, IsoDirections cutawayDirection, boolean circleStencil, WallShaperWhole texdModifier, Shader wallRenderShader) {
        int playerIndex = IsoCamera.frameState.playerIndex;
        if (this.z != PZMath.fastfloor((float)IsoCamera.getCameraCharacterZ())) {
            circleStencil = false;
        }
        boolean isDoor = obj.sprite.getTileType() == doorFrType || obj.sprite.getTileType() == doorType;
        boolean isWindow = obj instanceof IsoWindow;
        boolean noWallLighting = (isDoor || isWindow) && isCutaway || obj.sprite.getProperties().has(IsoFlagType.NoWallLighting);
        if ((circleStencil = this.calculateWallAlphaAndCircleStencilEdge(obj, hasNoDoor, hasNoWindow, isCutaway, transparentFlag, transparentWindowFlag, hoppableType, circleStencil, playerIndex, isDoor, isWindow)) && isCutaway) {
            this.DoCutawayShader(obj, cutawayDirection, cutawaySelf, cutawayN, cutawayS, cutawayW, cutawayE, bHasDoorN, bHasDoorW, bHasWindowN, bHasWindowW, (WallShaper)texdModifier);
            wallCutawayN |= cutawayDirection == IsoDirections.N;
            wallCutawayW |= cutawayDirection == IsoDirections.W;
            return stenciled;
        }
        return this.performDrawWall(obj, cutawayDirection, stenciled, playerIndex, noWallLighting, (Consumer<TextureDraw>)texdModifier, wallRenderShader);
    }

    private int performDrawWallOnly(IsoObject obj, IsoDirections dir, int stenciled, int playerIndex, boolean noWallLighting, Consumer<TextureDraw> texdModifier, Shader wallRenderShader) {
        IndieGL.enableAlphaTest();
        if (!noWallLighting) {
            // empty if block
        }
        if (DebugOptions.instance.terrain.renderTiles.isoGridSquare.walls.render.getValue()) {
            obj.renderWallTile(dir, this.x, this.y, this.z, noWallLighting ? lightInfoTemp : defColorInfo, true, !noWallLighting, wallRenderShader, texdModifier);
        }
        if (PerformanceSettings.fboRenderChunk && obj instanceof IsoWindow) {
            if (!obj.alphaForced && obj.isUpdateAlphaDuringRender()) {
                obj.updateAlpha(playerIndex);
            }
            return stenciled;
        }
        obj.setAlpha(playerIndex, 1.0f);
        if (noWallLighting) {
            return stenciled;
        }
        return stenciled + 1;
    }

    private int performDrawWall(IsoObject obj, IsoDirections dir, int stenciled, int playerIndex, boolean noWallLighting, Consumer<TextureDraw> texdModifier, Shader wallRenderShader) {
        lightInfoTemp.set(this.lightInfo[playerIndex]);
        if (Core.debug && DebugOptions.instance.debugDrawSkipWorldShading.getValue()) {
            obj.render(this.x, this.y, this.z, defColorInfo, true, !noWallLighting, null);
            return stenciled;
        }
        int stenciledResult = this.performDrawWallOnly(obj, dir, stenciled, playerIndex, noWallLighting, texdModifier, wallRenderShader);
        if (DebugOptions.instance.terrain.renderTiles.isoGridSquare.walls.attachedSprites.getValue()) {
            this.renderAttachedSpritesWithNoWallLighting(obj, lightInfoTemp, null);
        }
        return stenciledResult;
    }

    private void calculateWallAlphaCommon(IsoObject obj, boolean isCutaway, boolean bHasDoor, boolean bHasWindow, int playerIndex, boolean isDoor, boolean isWindow) {
        if (!isDoor && !isWindow) {
            return;
        }
        if (isWindow && PerformanceSettings.fboRenderChunk) {
            return;
        }
        if (isCutaway) {
            obj.setAlpha(playerIndex, 0.4f);
            obj.setTargetAlpha(playerIndex, 0.4f);
            IsoGridSquare.lightInfoTemp.r = Math.max(0.3f, IsoGridSquare.lightInfoTemp.r);
            IsoGridSquare.lightInfoTemp.g = Math.max(0.3f, IsoGridSquare.lightInfoTemp.g);
            IsoGridSquare.lightInfoTemp.b = Math.max(0.3f, IsoGridSquare.lightInfoTemp.b);
            if (isDoor && !bHasDoor) {
                obj.setAlpha(playerIndex, 0.0f);
                obj.setTargetAlpha(playerIndex, 0.0f);
            }
            if (isWindow && !bHasWindow) {
                obj.setAlpha(playerIndex, 0.0f);
                obj.setTargetAlpha(playerIndex, 0.0f);
            }
        }
    }

    private boolean calculateWallAlphaAndCircleStencilEdge(IsoObject obj, boolean hasNoDoor, boolean hasNoWindow, boolean isCutaway, IsoFlagType transparentFlag, IsoFlagType transparentWindowFlag, IsoFlagType hoppableType, boolean circleStencil, int playerIndex, boolean isDoor, boolean isWindow) {
        if (isDoor || isWindow) {
            if (!obj.sprite.getProperties().has(IsoPropertyType.GARAGE_DOOR)) {
                circleStencil = false;
            }
            this.calculateWallAlphaCommon(obj, isCutaway, !hasNoDoor, !hasNoWindow, playerIndex, isDoor, isWindow);
        }
        if (!(!circleStencil || obj.sprite.getTileType() == IsoObjectType.wall && obj.getProperties().has(transparentFlag) && "walls_burnt_01".equals(obj.sprite.tilesetName) || obj.sprite.getTileType() != IsoObjectType.wall || !obj.sprite.getProperties().has(transparentFlag) || obj.getSprite().getProperties().has(IsoFlagType.exterior) || obj.sprite.getProperties().has(transparentWindowFlag))) {
            circleStencil = false;
        }
        return circleStencil;
    }

    private boolean calculateWallAlphaAndCircleStencilCorner(IsoObject obj, int cutawaySelf, boolean bHasDoorN, boolean bHasDoorW, boolean bHasWindowN, boolean bHasWindowW, boolean circleStencil, int playerIndex, boolean isDoorN, boolean isDoorW, boolean isWindowN, boolean isWindowW) {
        this.calculateWallAlphaCommon(obj, (cutawaySelf & 1) != 0, bHasDoorN, bHasWindowN, playerIndex, isDoorN, isWindowN);
        this.calculateWallAlphaCommon(obj, (cutawaySelf & 2) != 0, bHasDoorW, bHasWindowW, playerIndex, isDoorW, isWindowW);
        boolean bl = circleStencil = circleStencil && !isDoorN && !isWindowN;
        if (circleStencil && obj.sprite.getTileType() == IsoObjectType.wall && (obj.sprite.getProperties().has(IsoFlagType.transparentN) || obj.sprite.getProperties().has(IsoFlagType.transparentW)) && !obj.getSprite().getProperties().has(IsoFlagType.exterior) && !obj.sprite.getProperties().has(IsoFlagType.WindowN) && !obj.sprite.getProperties().has(IsoFlagType.WindowW)) {
            circleStencil = false;
        }
        return circleStencil;
    }

    public KahluaTable getLuaMovingObjectList() {
        KahluaTable table = LuaManager.platform.newTable();
        LuaManager.env.rawset((Object)"Objects", (Object)table);
        for (int n = 0; n < this.movingObjects.size(); ++n) {
            table.rawset(n + 1, (Object)this.movingObjects.get(n));
        }
        return table;
    }

    public boolean has(IsoFlagType flag) {
        return this.properties.has(flag);
    }

    public boolean has(IsoPropertyType flag) {
        return this.has(flag.getName());
    }

    public boolean has(IsoPropertyType ... flag) {
        return this.properties.has(flag);
    }

    public boolean has(String flag) {
        return this.properties.has(flag);
    }

    public boolean has(IsoObjectType type) {
        return this.has(type.index());
    }

    public boolean has(int type) {
        return (this.hasTypes & 1L << type) != 0L;
    }

    public void set(String tilePropertyKey) {
        this.properties.set(tilePropertyKey);
    }

    public void unset(String tilePropertyKey) {
        this.properties.unset(tilePropertyKey);
    }

    public void DeleteTileObject(IsoObject obj) {
        int index = this.objects.indexOf((Object)obj);
        if (index == -1) {
            return;
        }
        this.objects.remove(index);
        if (obj instanceof IsoTree) {
            IsoTree tree = (IsoTree)obj;
            obj.reset();
            CellLoader.isoTreeCache.push((Object)tree);
        } else if (obj.getObjectName().equals("IsoObject")) {
            obj.reset();
            CellLoader.isoObjectCache.push((Object)obj);
        }
    }

    public KahluaTable getLuaTileObjectList() {
        KahluaTable table = LuaManager.platform.newTable();
        LuaManager.env.rawset((Object)"Objects", (Object)table);
        for (int n = 0; n < this.objects.size(); ++n) {
            table.rawset(n + 1, this.objects.get(n));
        }
        return table;
    }

    boolean HasDoor(boolean north) {
        for (int n = 0; n < this.specialObjects.size(); ++n) {
            if (this.specialObjects.get(n) instanceof IsoDoor && ((IsoDoor)this.specialObjects.get((int)n)).north == north) {
                return true;
            }
            if (!(this.specialObjects.get(n) instanceof IsoThumpable) || !((IsoThumpable)this.specialObjects.get(n)).isDoor() || ((IsoThumpable)this.specialObjects.get((int)n)).north != north) continue;
            return true;
        }
        return false;
    }

    public boolean HasStairs() {
        return this.HasStairsNorth() || this.HasStairsWest();
    }

    public boolean HasStairsNorth() {
        return this.has(IsoObjectType.stairsTN) || this.has(IsoObjectType.stairsMN) || this.has(IsoObjectType.stairsBN);
    }

    public boolean HasStairsWest() {
        return this.has(IsoObjectType.stairsTW) || this.has(IsoObjectType.stairsMW) || this.has(IsoObjectType.stairsBW);
    }

    public boolean isStairBlockedTo(IsoGridSquare other) {
        if (other == null) {
            return false;
        }
        if (this.x > other.x && this.HasStairTopWest()) {
            return true;
        }
        return this.y > other.y && this.HasStairTopNorth();
    }

    public boolean HasStairTop() {
        return this.HasStairTopNorth() || this.HasStairTopWest();
    }

    public boolean HasStairTopNorth() {
        return this.has(IsoObjectType.stairsTN);
    }

    public boolean HasStairTopWest() {
        return this.has(IsoObjectType.stairsTW);
    }

    public boolean HasStairsBelow() {
        IsoGridSquare below = this.getCell().getGridSquare(this.x, this.y, this.z - 1);
        return below != null && below.HasStairs();
    }

    public IsoObject getStairPillar() {
        IsoObject obj = this.getObjectWithSprite("carpentry_02_94");
        if (obj == null) {
            obj = this.getObjectWithSprite("carpentry_02_95");
        }
        return obj;
    }

    public IsoGridSquare getFloorSquareBelow() {
        if (this.getChunk() == null) {
            return null;
        }
        for (int z = this.getZ() - 1; z >= this.getChunk().getMinLevel(); --z) {
            IsoGridSquare testSq = this.getCell().getGridSquare(this.getX(), this.getY(), z);
            if (testSq == null || !testSq.TreatAsSolidFloor()) continue;
            return testSq;
        }
        return null;
    }

    public boolean hasFloorBelow() {
        return this.getFloorSquareBelow() != null;
    }

    public IsoObject getObjectWithSprite(String spriteName) {
        for (int i = 0; i < this.objects.size(); ++i) {
            IsoObject obj = (IsoObject)this.objects.get(i);
            if (obj == null || obj.getSprite() == null || !spriteName.equals(obj.getSprite().getName())) continue;
            return obj;
        }
        return null;
    }

    public boolean hasFloorAtTopOfStairs() {
        if (this.getFloor() == null) {
            return false;
        }
        IsoGridSquare s = this.getAdjacentSquare(IsoDirections.S);
        if (s != null && s.HasStairsBelow()) {
            return true;
        }
        IsoGridSquare e = this.getAdjacentSquare(IsoDirections.E);
        return e != null && e.HasStairsBelow();
    }

    public IsoObjectType getStairs() {
        if (this.has(IsoObjectType.stairsTW)) {
            return IsoObjectType.stairsTW;
        }
        if (this.has(IsoObjectType.stairsTN)) {
            return IsoObjectType.stairsTN;
        }
        if (this.has(IsoObjectType.stairsMW)) {
            return IsoObjectType.stairsMW;
        }
        if (this.has(IsoObjectType.stairsMN)) {
            return IsoObjectType.stairsMN;
        }
        if (this.has(IsoObjectType.stairsBW)) {
            return IsoObjectType.stairsBW;
        }
        if (this.has(IsoObjectType.stairsBN)) {
            return IsoObjectType.stairsBN;
        }
        return IsoObjectType.MAX;
    }

    public boolean HasElevatedFloor() {
        return this.has(IsoObjectType.stairsTN) || this.has(IsoObjectType.stairsMN) || this.has(IsoObjectType.stairsTW) || this.has(IsoObjectType.stairsMW);
    }

    public boolean isSameStaircase(int x, int y, int z) {
        if (z != this.getZ()) {
            return false;
        }
        int minX = this.getX();
        int minY = this.getY();
        int maxX = minX--;
        int maxY = minY--;
        if (this.has(IsoObjectType.stairsTN)) {
            maxY += 2;
        } else if (this.has(IsoObjectType.stairsMN)) {
            ++maxY;
        } else if (this.has(IsoObjectType.stairsBN)) {
            minY -= 2;
        } else if (this.has(IsoObjectType.stairsTW)) {
            maxX += 2;
        } else if (this.has(IsoObjectType.stairsMW)) {
            ++maxX;
        } else if (this.has(IsoObjectType.stairsBW)) {
            minX -= 2;
        } else {
            return false;
        }
        if (x < minX || y < minY || x > maxX || y > maxY) {
            return false;
        }
        IsoGridSquare square = this.getCell().getGridSquare(x, y, z);
        return square != null && square.HasStairs();
    }

    public boolean hasRainBlockingTile() {
        return this.has(IsoFlagType.solidfloor) || this.has(IsoFlagType.BlockRain);
    }

    public boolean haveRoofFull() {
        if (this.haveRoof) {
            return true;
        }
        if (this.chunk == null) {
            return false;
        }
        int zTest = 1;
        int chunksPerWidth = 8;
        IsoGridSquare sq = this.chunk.getGridSquare(this.x % 8, this.y % 8, zTest);
        while (zTest <= this.chunk.getMaxLevel()) {
            if (sq != null && sq.haveRoof) {
                return true;
            }
            sq = this.chunk.getGridSquare(this.x % 8, this.y % 8, ++zTest);
        }
        return false;
    }

    public boolean HasSlopedRoof() {
        return this.HasSlopedRoofWest() || this.HasSlopedRoofNorth();
    }

    public boolean HasSlopedRoofWest() {
        return this.has(IsoObjectType.WestRoofB) || this.has(IsoObjectType.WestRoofM) || this.has(IsoObjectType.WestRoofT);
    }

    public boolean HasSlopedRoofNorth() {
        return this.has(IsoObjectType.WestRoofB) || this.has(IsoObjectType.WestRoofM) || this.has(IsoObjectType.WestRoofT);
    }

    public boolean HasEave() {
        return this.getProperties().has(IsoFlagType.isEave);
    }

    public boolean HasTree() {
        return this.hasTree;
    }

    public IsoTree getTree() {
        for (int i = 0; i < this.objects.size(); ++i) {
            Object object = this.objects.get(i);
            if (!(object instanceof IsoTree)) continue;
            IsoTree tree = (IsoTree)object;
            return tree;
        }
        return null;
    }

    public IsoObject getStump() {
        for (int i = 0; i < this.objects.size(); ++i) {
            if (this.objects.get(i) == null || !((IsoObject)this.objects.get(i)).isStump()) continue;
            return (IsoObject)this.objects.get(i);
        }
        return null;
    }

    public IsoObject getOre() {
        for (int i = 0; i < this.objects.size(); ++i) {
            if (this.objects.get(i) == null || !((IsoObject)this.objects.get(i)).isOre()) continue;
            return (IsoObject)this.objects.get(i);
        }
        return null;
    }

    public boolean hasBush() {
        return this.getBush() != null;
    }

    public IsoObject getBush() {
        for (int i = 0; i < this.objects.size(); ++i) {
            if (this.objects.get(i) == null || !((IsoObject)this.objects.get(i)).isBush()) continue;
            return (IsoObject)this.objects.get(i);
        }
        return null;
    }

    public List<IsoObject> getBushes() {
        ArrayList<IsoObject> objects = new ArrayList<IsoObject>();
        for (int i = 0; i < this.objects.size(); ++i) {
            IsoObject object = (IsoObject)this.objects.get(i);
            if (!object.isBush()) continue;
            objects.add(object);
        }
        return objects;
    }

    public IsoObject getGrass() {
        for (int i = 0; i < this.objects.size(); ++i) {
            if (this.objects.get(i) == null || !((IsoObject)this.objects.get(i)).isGrass()) continue;
            return (IsoObject)this.objects.get(i);
        }
        return null;
    }

    public boolean hasGrassLike() {
        return !this.getGrassLike().isEmpty();
    }

    public List<IsoObject> getGrassLike() {
        ArrayList<IsoObject> objects = new ArrayList<IsoObject>();
        for (int i = 0; i < this.objects.size(); ++i) {
            if (!((IsoObject)this.objects.get(i)).isGrassLike()) continue;
            objects.add((IsoObject)this.objects.get(i));
        }
        return objects;
    }

    public List<IsoObject> getOres() {
        ArrayList<IsoObject> objects = new ArrayList<IsoObject>();
        for (int i = 0; i < this.objects.size(); ++i) {
            if (!((IsoObject)this.objects.get(i)).isOres()) continue;
            objects.add((IsoObject)this.objects.get(i));
        }
        return objects;
    }

    public IsoObject getCountertopObject() {
        for (int i = this.getObjects().size() - 1; i >= 0; --i) {
            IsoObject obj = (IsoObject)this.getObjects().get(i);
            if (!obj.hasProperty(IsoPropertyType.COUNTERTOP)) continue;
            return obj;
        }
        return null;
    }

    public IsoObject getCountertopAttachObject() {
        for (int i = this.getObjects().size() - 1; i >= 0; --i) {
            IsoObject obj = (IsoObject)this.getObjects().get(i);
            if (!obj.hasProperty(IsoPropertyType.COUNTERTOP_ATTACH)) continue;
            return obj;
        }
        return null;
    }

    public boolean shouldSave() {
        return !this.objects.isEmpty() || this.z == 0;
    }

    public void save(ByteBuffer output, ObjectOutputStream outputObj) throws IOException {
        this.save(output, outputObj, false);
    }

    public void save(ByteBuffer output, ObjectOutputStream outputObj, boolean isDebugSave) throws IOException {
        int n;
        this.getErosionData().save(output);
        BitHeaderWrite header = BitHeader.allocWrite((BitHeader.HeaderSize)BitHeader.HeaderSize.Byte, (ByteBuffer)output);
        int objSize = this.objects.size();
        if (!this.objects.isEmpty()) {
            header.addFlags(1);
            if (objSize == 2) {
                header.addFlags(2);
            } else if (objSize == 3) {
                header.addFlags(4);
            } else if (objSize >= 4) {
                header.addFlags(8);
            }
            if (isDebugSave) {
                GameWindow.WriteString((ByteBuffer)output, (String)("Number of objects (" + objSize + ")"));
            }
            if (objSize >= 4) {
                output.putShort((short)this.objects.size());
            }
            for (int n2 = 0; n2 < this.objects.size(); ++n2) {
                int position1 = output.position();
                if (isDebugSave) {
                    output.putInt(0);
                }
                byte flagsObj = 0;
                if (this.specialObjects.contains(this.objects.get(n2))) {
                    flagsObj = (byte)(flagsObj | 2);
                }
                if (this.worldObjects.contains(this.objects.get(n2))) {
                    flagsObj = (byte)(flagsObj | 4);
                }
                output.put(flagsObj);
                if (isDebugSave) {
                    GameWindow.WriteString((ByteBuffer)output, (String)((IsoObject)this.objects.get(n2)).getClass().getName());
                }
                ((IsoObject)this.objects.get(n2)).save(output, isDebugSave);
                if (isDebugSave) {
                    int position2 = output.position();
                    output.position(position1);
                    output.putInt(position2 - position1);
                    output.position(position2);
                }
                if (!GameServer.server || GameEntityManager.needSave || !((IsoObject)this.objects.get(n2)).hasComponent(ComponentType.MetaTag)) continue;
                GameEntityManager.needSave = true;
            }
            if (isDebugSave) {
                output.put((byte)67);
                output.put((byte)82);
                output.put((byte)80);
                output.put((byte)83);
            }
        }
        if (this.isOverlayDone()) {
            header.addFlags(16);
        }
        if (this.haveRoof) {
            header.addFlags(32);
        }
        BitHeaderWrite bits = BitHeader.allocWrite((BitHeader.HeaderSize)BitHeader.HeaderSize.Byte, (ByteBuffer)output);
        int bodyCount = 0;
        for (n = 0; n < this.staticMovingObjects.size(); ++n) {
            if (!(this.staticMovingObjects.get(n) instanceof IsoDeadBody)) continue;
            ++bodyCount;
        }
        if (bodyCount > 0) {
            bits.addFlags(1);
            if (isDebugSave) {
                GameWindow.WriteString((ByteBuffer)output, (String)"Number of bodies");
            }
            output.putShort((short)bodyCount);
            for (n = 0; n < this.staticMovingObjects.size(); ++n) {
                IsoMovingObject body = this.staticMovingObjects.get(n);
                if (!(body instanceof IsoDeadBody)) continue;
                if (isDebugSave) {
                    GameWindow.WriteString((ByteBuffer)output, (String)body.getClass().getName());
                }
                body.save(output, isDebugSave);
            }
        }
        if (this.table != null && !this.table.isEmpty()) {
            bits.addFlags(2);
            this.table.save(output);
        }
        if (this.burntOut) {
            bits.addFlags(4);
        }
        if (this.getTrapPositionX() > 0) {
            bits.addFlags(8);
            output.putInt(this.getTrapPositionX());
            output.putInt(this.getTrapPositionY());
            output.putInt(this.getTrapPositionZ());
        }
        if (this.haveSheetRope) {
            bits.addFlags(16);
        }
        if (!bits.equals(0)) {
            header.addFlags(64);
            bits.write();
        } else {
            output.position(bits.getStartPosition());
        }
        int vis = 0;
        if (!GameClient.client && !GameServer.server) {
            for (int i = 0; i < 4; ++i) {
                if (!this.isSeen(i)) continue;
                vis |= 1 << i;
            }
        }
        output.put((byte)vis);
        header.write();
        header.release();
        bits.release();
    }

    static void loadmatrix(boolean[][][] matrix, DataInputStream input) throws IOException {
    }

    static void savematrix(boolean[][][] matrix, DataOutputStream output) throws IOException {
        for (int x = 0; x < 3; ++x) {
            for (int y = 0; y < 3; ++y) {
                for (int z = 0; z < 3; ++z) {
                    output.writeBoolean(matrix[x][y][z]);
                }
            }
        }
    }

    public boolean isCommonGrass() {
        if (this.objects.isEmpty()) {
            return false;
        }
        IsoObject o = (IsoObject)this.objects.get(0);
        return o.sprite.getProperties().has(IsoFlagType.solidfloor) && ("TileFloorExt_3".equals(o.getTile()) || "TileFloorExt_4".equals(o.getTile()));
    }

    public static boolean toBoolean(byte[] data) {
        return data == null || data.length == 0 ? false : data[0] != 0;
    }

    public void removeCorpse(IsoDeadBody body, boolean bRemote) {
        if (GameClient.client && !bRemote) {
            try {
                GameClient.instance.checkAddedRemovedItems((IsoObject)body);
            }
            catch (Exception ex) {
                GameClient.connection.cancelPacket();
                ExceptionLogger.logException((Throwable)ex);
            }
            INetworkPacket.send((PacketTypes.PacketType)PacketTypes.PacketType.RemoveCorpseFromMap, (Object[])new Object[]{body});
        }
        if (GameServer.server && !bRemote) {
            INetworkPacket.sendToRelative((PacketTypes.PacketType)PacketTypes.PacketType.RemoveCorpseFromMap, (float)body.getX(), (float)body.getY(), (Object[])new Object[]{body});
        }
        body.invalidateRenderChunkLevel(130L);
        body.removeFromWorld();
        body.removeFromSquare();
        if (!GameServer.server) {
            LuaEventManager.triggerEvent((String)"OnContainerUpdate", (Object)this);
        }
    }

    public IsoDeadBody getDeadBody() {
        for (int i = 0; i < this.staticMovingObjects.size(); ++i) {
            if (!(this.staticMovingObjects.get(i) instanceof IsoDeadBody)) continue;
            return (IsoDeadBody)this.staticMovingObjects.get(i);
        }
        return null;
    }

    public List<IsoDeadBody> getDeadBodys() {
        ArrayList<IsoDeadBody> result = new ArrayList<IsoDeadBody>();
        for (int i = 0; i < this.staticMovingObjects.size(); ++i) {
            if (!(this.staticMovingObjects.get(i) instanceof IsoDeadBody)) continue;
            result.add((IsoDeadBody)this.staticMovingObjects.get(i));
        }
        return result;
    }

    public void addCorpse(IsoDeadBody body, boolean bRemote) {
        if (GameClient.client && !bRemote) {
            AddCorpseToMapPacket packet = new AddCorpseToMapPacket();
            packet.set(this, body);
            ByteBufferWriter b = GameClient.connection.startPacket();
            PacketTypes.PacketType.AddCorpseToMap.doPacket(b);
            packet.write(b);
            PacketTypes.PacketType.AddCorpseToMap.send((IConnection)GameClient.connection);
        }
        if (!this.staticMovingObjects.contains(body)) {
            this.staticMovingObjects.add((IsoMovingObject)body);
        }
        body.addToWorld();
        this.burntOut = false;
        this.properties.unset(IsoFlagType.burntOut);
        body.invalidateRenderChunkLevel(66L);
    }

    public IsoBrokenGlass getBrokenGlass() {
        for (int i = 0; i < this.specialObjects.size(); ++i) {
            IsoObject obj = this.specialObjects.get(i);
            if (!(obj instanceof IsoBrokenGlass)) continue;
            IsoBrokenGlass isoBrokenGlass = (IsoBrokenGlass)obj;
            return isoBrokenGlass;
        }
        return null;
    }

    public IsoBrokenGlass addBrokenGlass() {
        if (!this.isFree(false)) {
            return this.getBrokenGlass();
        }
        IsoBrokenGlass brokenGlass = this.getBrokenGlass();
        if (brokenGlass == null) {
            brokenGlass = new IsoBrokenGlass(this.getCell());
            brokenGlass.setSquare(this);
            this.AddSpecialObject((IsoObject)brokenGlass);
            if (GameServer.server) {
                GameServer.transmitBrokenGlass((IsoGridSquare)this);
            }
            if (GameClient.client) {
                GameClient.sendBrokenGlass((IsoGridSquare)this);
            }
        }
        return brokenGlass;
    }

    public IsoFire getFire() {
        for (int i = 0; i < this.objects.size(); ++i) {
            Object object = this.objects.get(i);
            if (!(object instanceof IsoFire)) continue;
            IsoFire fire = (IsoFire)object;
            return fire;
        }
        return null;
    }

    public IsoObject getHiddenStash() {
        for (int i = 0; i < this.specialObjects.size(); ++i) {
            IsoObject obj = this.specialObjects.get(i);
            IsoSprite sprite = obj.getSprite();
            if (sprite == null || !StringUtils.equalsIgnoreCase((String)"floors_interior_tilesandwood_01_62", (String)sprite.getName())) continue;
            return obj;
        }
        return null;
    }

    public void load(ByteBuffer b, int worldVersion) throws IOException {
        this.load(b, worldVersion, false);
    }

    public void load(ByteBuffer b, int worldVersion, boolean isDebugSave) throws IOException {
        this.getErosionData().load(b, worldVersion);
        BitHeaderRead header = BitHeader.allocRead((BitHeader.HeaderSize)BitHeader.HeaderSize.Byte, (ByteBuffer)b);
        if (!header.equals(0)) {
            if (header.hasFlags(1)) {
                if (isDebugSave) {
                    String str = GameWindow.ReadString((ByteBuffer)b);
                    DebugLog.log((String)str);
                }
                int objs = 1;
                if (header.hasFlags(2)) {
                    objs = 2;
                } else if (header.hasFlags(4)) {
                    objs = 3;
                } else if (header.hasFlags(8)) {
                    objs = b.getShort();
                }
                for (int n = 0; n < objs; ++n) {
                    int position2;
                    IsoObject obj;
                    boolean bWorld;
                    byte flagsObj;
                    int position1 = b.position();
                    int size = 0;
                    if (isDebugSave) {
                        size = b.getInt();
                    }
                    boolean bSpecial = ((flagsObj = b.get()) & 2) != 0;
                    boolean bl = bWorld = (flagsObj & 4) != 0;
                    if (isDebugSave) {
                        String str = GameWindow.ReadString((ByteBuffer)b);
                        DebugLog.log((String)str);
                    }
                    if ((obj = IsoObject.factoryFromFileInput(this.getCell(), b)) == null) {
                        if (!isDebugSave || (position2 = b.position()) - position1 == size) continue;
                        DebugLog.log((String)("***** Object loaded size " + (position2 - position1) + " != saved size " + size + ", reading obj size: " + objs + ", Object == null"));
                        if (obj.getSprite() == null || obj.getSprite().getName() == null) continue;
                        DebugLog.log((String)("Obj sprite = " + obj.getSprite().getName()));
                        continue;
                    }
                    obj.square = this;
                    try {
                        obj.load(b, worldVersion, isDebugSave);
                    }
                    catch (Exception ex) {
                        this.debugPrintGridSquare();
                        if (lastLoaded != null) {
                            lastLoaded.debugPrintGridSquare();
                        }
                        throw new RuntimeException(ex);
                    }
                    if (isDebugSave && (position2 = b.position()) - position1 != size) {
                        DebugLog.log((String)("***** Object loaded size " + (position2 - position1) + " != saved size " + size + ", reading obj size: " + objs));
                        if (obj.getSprite() != null && obj.getSprite().getName() != null) {
                            DebugLog.log((String)("Obj sprite = " + obj.getSprite().getName()));
                        }
                    }
                    if (obj instanceof IsoWorldInventoryObject) {
                        String type;
                        Item scriptItem;
                        IsoWorldInventoryObject worldItem = (IsoWorldInventoryObject)obj;
                        if (worldItem.getItem() == null || (scriptItem = ScriptManager.instance.FindItem(type = worldItem.getItem().getFullType())) != null && scriptItem.getObsolete()) continue;
                        String[] splitString = type.split("_");
                        if ((worldItem.dropTime > -1.0 && SandboxOptions.instance.hoursForWorldItemRemoval.getValue() > 0.0 && (!SandboxOptions.instance.itemRemovalListBlacklistToggle.getValue() && SandboxOptions.instance.worldItemRemovalListContains(type) || SandboxOptions.instance.itemRemovalListBlacklistToggle.getValue() && !SandboxOptions.instance.worldItemRemovalListContains(type)) || !SandboxOptions.instance.itemRemovalListBlacklistToggle.getValue() && SandboxOptions.instance.worldItemRemovalListContains(splitString[0]) || SandboxOptions.instance.itemRemovalListBlacklistToggle.getValue() && !SandboxOptions.instance.worldItemRemovalListContains(splitString[0])) && !worldItem.isIgnoreRemoveSandbox() && GameTime.instance.getWorldAgeHours() > worldItem.dropTime + SandboxOptions.instance.hoursForWorldItemRemoval.getValue()) continue;
                    }
                    if (obj instanceof IsoWindow && obj.getSprite() != null && ("walls_special_01_8".equals(obj.getSprite().getName()) || "walls_special_01_9".equals(obj.getSprite().getName()))) continue;
                    this.objects.add((Object)obj);
                    if (bSpecial) {
                        this.specialObjects.add(obj);
                    }
                    if (!bWorld) continue;
                    if (Core.debug && !(obj instanceof IsoWorldInventoryObject)) {
                        DebugLog.log((String)("Bitflags = " + flagsObj + ", obj name = " + obj.getObjectName() + ", sprite = " + (obj.getSprite() != null ? obj.getSprite().getName() : "unknown")));
                    }
                    this.worldObjects.add((IsoWorldInventoryObject)obj);
                }
                if (isDebugSave) {
                    byte b1 = b.get();
                    byte b2 = b.get();
                    byte b3 = b.get();
                    byte b4 = b.get();
                    if (b1 != 67 || b2 != 82 || b3 != 80 || b4 != 83) {
                        DebugLog.log((String)"***** Expected CRPS here");
                    }
                }
            }
            this.setOverlayDone(header.hasFlags(16));
            this.haveRoof = header.hasFlags(32);
            if (header.hasFlags(64)) {
                BitHeaderRead bits = BitHeader.allocRead((BitHeader.HeaderSize)BitHeader.HeaderSize.Byte, (ByteBuffer)b);
                if (bits.hasFlags(1)) {
                    if (isDebugSave) {
                        String str = GameWindow.ReadString((ByteBuffer)b);
                        DebugLog.log((String)str);
                    }
                    int objs = b.getShort();
                    for (int n = 0; n < objs; ++n) {
                        IsoMovingObject obj;
                        if (isDebugSave) {
                            String str = GameWindow.ReadString((ByteBuffer)b);
                            DebugLog.log((String)str);
                        }
                        try {
                            obj = (IsoMovingObject)IsoObject.factoryFromFileInput(this.getCell(), b);
                        }
                        catch (Exception ex) {
                            this.debugPrintGridSquare();
                            if (lastLoaded != null) {
                                lastLoaded.debugPrintGridSquare();
                            }
                            throw new RuntimeException(ex);
                        }
                        if (obj == null) continue;
                        obj.square = this;
                        obj.current = this;
                        try {
                            obj.load(b, worldVersion, isDebugSave);
                        }
                        catch (Exception ex) {
                            this.debugPrintGridSquare();
                            if (lastLoaded != null) {
                                lastLoaded.debugPrintGridSquare();
                            }
                            throw new RuntimeException(ex);
                        }
                        this.staticMovingObjects.add(obj);
                        this.recalcHashCodeObjects();
                    }
                }
                if (bits.hasFlags(2)) {
                    if (this.table == null) {
                        this.table = LuaManager.platform.newTable();
                    }
                    this.table.load(b, worldVersion);
                }
                this.burntOut = bits.hasFlags(4);
                if (bits.hasFlags(8)) {
                    int trapPositionX = b.getInt();
                    int trapPositionY = b.getInt();
                    int n = b.getInt();
                }
                this.haveSheetRope = bits.hasFlags(16);
                bits.release();
            }
        }
        header.release();
        byte vis = b.get();
        if (!GameClient.client && !GameServer.server) {
            for (int i = 0; i < 4; ++i) {
                this.setIsSeen(i, (vis & 1 << i) != 0);
            }
        }
        lastLoaded = this;
    }

    private void debugPrintGridSquare() {
        int n;
        System.out.println("x=" + this.x + " y=" + this.y + " z=" + this.z);
        System.out.println("objects");
        for (n = 0; n < this.objects.size(); ++n) {
            ((IsoObject)this.objects.get(n)).debugPrintout();
        }
        System.out.println("staticmovingobjects");
        for (n = 0; n < this.staticMovingObjects.size(); ++n) {
            ((IsoObject)this.objects.get(n)).debugPrintout();
        }
    }

    public float scoreAsWaypoint(int x, int y) {
        float score = 2.0f;
        return score -= IsoUtils.DistanceManhatten((float)x, (float)y, (float)this.getX(), (float)this.getY()) * 5.0f;
    }

    public void InvalidateSpecialObjectPaths() {
    }

    public boolean isSolid() {
        return this.properties.has(IsoFlagType.solid);
    }

    public boolean isSolidTrans() {
        return this.properties.has(IsoFlagType.solidtrans);
    }

    public boolean isFree(boolean bCountOtherCharacters) {
        if (bCountOtherCharacters && !this.movingObjects.isEmpty()) {
            return false;
        }
        if (this.cachedIsFree) {
            return this.cacheIsFree;
        }
        this.cachedIsFree = true;
        this.cacheIsFree = true;
        if (this.properties.has(IsoFlagType.solid) || this.properties.has(IsoFlagType.solidtrans) || this.has(IsoObjectType.tree)) {
            this.cacheIsFree = false;
        }
        if (!this.properties.has(IsoFlagType.solidfloor)) {
            this.cacheIsFree = false;
        }
        if (this.has(IsoObjectType.stairsBN) || this.has(IsoObjectType.stairsMN) || this.has(IsoObjectType.stairsTN)) {
            this.cacheIsFree = true;
        } else if (this.has(IsoObjectType.stairsBW) || this.has(IsoObjectType.stairsMW) || this.has(IsoObjectType.stairsTW)) {
            this.cacheIsFree = true;
        }
        return this.cacheIsFree;
    }

    public boolean isFreeOrMidair(boolean bCountOtherCharacters) {
        if (bCountOtherCharacters && !this.movingObjects.isEmpty()) {
            return false;
        }
        boolean cacheIsFree = true;
        if (this.properties.has(IsoFlagType.solid) || this.properties.has(IsoFlagType.solidtrans) || this.has(IsoObjectType.tree)) {
            cacheIsFree = false;
        }
        if (this.has(IsoObjectType.stairsBN) || this.has(IsoObjectType.stairsMN) || this.has(IsoObjectType.stairsTN)) {
            cacheIsFree = true;
        } else if (this.has(IsoObjectType.stairsBW) || this.has(IsoObjectType.stairsMW) || this.has(IsoObjectType.stairsTW)) {
            cacheIsFree = true;
        }
        return cacheIsFree;
    }

    public boolean isFreeOrMidair(boolean bCountOtherCharacters, boolean bDoZombie) {
        if (bCountOtherCharacters && !this.movingObjects.isEmpty()) {
            if (bDoZombie) {
                for (int i = 0; i < this.movingObjects.size(); ++i) {
                    IsoMovingObject object = this.movingObjects.get(i);
                    if (object instanceof IsoDeadBody) continue;
                    return false;
                }
            } else {
                return false;
            }
        }
        boolean cacheIsFree = true;
        if (this.properties.has(IsoFlagType.solid) || this.properties.has(IsoFlagType.solidtrans) || this.has(IsoObjectType.tree)) {
            cacheIsFree = false;
        }
        if (this.has(IsoObjectType.stairsBN) || this.has(IsoObjectType.stairsMN) || this.has(IsoObjectType.stairsTN)) {
            cacheIsFree = true;
        } else if (this.has(IsoObjectType.stairsBW) || this.has(IsoObjectType.stairsMW) || this.has(IsoObjectType.stairsTW)) {
            cacheIsFree = true;
        }
        return cacheIsFree;
    }

    public boolean connectedWithFloor() {
        if (this.getZ() == 0) {
            return true;
        }
        IsoGridSquare sq = this.getCell().getGridSquare(this.getX() - 1, this.getY(), this.getZ());
        if (sq != null && sq.properties.has(IsoFlagType.solidfloor)) {
            return true;
        }
        sq = this.getCell().getGridSquare(this.getX() + 1, this.getY(), this.getZ());
        if (sq != null && sq.properties.has(IsoFlagType.solidfloor)) {
            return true;
        }
        sq = this.getCell().getGridSquare(this.getX(), this.getY() - 1, this.getZ());
        if (sq != null && sq.properties.has(IsoFlagType.solidfloor)) {
            return true;
        }
        sq = this.getCell().getGridSquare(this.getX(), this.getY() + 1, this.getZ());
        if (sq != null && sq.properties.has(IsoFlagType.solidfloor)) {
            return true;
        }
        sq = this.getCell().getGridSquare(this.getX(), this.getY() - 1, this.getZ() - 1);
        if (sq != null && sq.getSlopedSurfaceHeight(IsoDirections.S) == 1.0f) {
            return true;
        }
        sq = this.getCell().getGridSquare(this.getX(), this.getY() + 1, this.getZ() - 1);
        if (sq != null && sq.getSlopedSurfaceHeight(IsoDirections.N) == 1.0f) {
            return true;
        }
        sq = this.getCell().getGridSquare(this.getX() - 1, this.getY(), this.getZ() - 1);
        if (sq != null && sq.getSlopedSurfaceHeight(IsoDirections.E) == 1.0f) {
            return true;
        }
        sq = this.getCell().getGridSquare(this.getX() + 1, this.getY(), this.getZ() - 1);
        return sq != null && sq.getSlopedSurfaceHeight(IsoDirections.W) == 1.0f;
    }

    public boolean hasFloor(boolean north) {
        if (this.properties.has(IsoFlagType.solidfloor)) {
            return true;
        }
        IsoGridSquare sq = north ? this.getCell().getGridSquare(this.getX(), this.getY() - 1, this.getZ()) : this.getCell().getGridSquare(this.getX() - 1, this.getY(), this.getZ());
        return sq != null && sq.properties.has(IsoFlagType.solidfloor);
    }

    public boolean hasFloor() {
        return this.properties.has(IsoFlagType.solidfloor);
    }

    public boolean isNotBlocked(boolean bCountOtherCharacters) {
        if (!this.cachedIsFree) {
            this.cacheIsFree = true;
            this.cachedIsFree = true;
            if (this.properties.has(IsoFlagType.solid) || this.properties.has(IsoFlagType.solidtrans)) {
                this.cacheIsFree = false;
            }
            if (!this.properties.has(IsoFlagType.solidfloor)) {
                this.cacheIsFree = false;
            }
        } else if (!this.cacheIsFree) {
            return false;
        }
        return !bCountOtherCharacters || this.movingObjects.isEmpty();
    }

    public IsoObject getDoor(boolean north) {
        for (int n = 0; n < this.specialObjects.size(); ++n) {
            IsoThumpable thump;
            IsoObject special = this.specialObjects.get(n);
            if (special instanceof IsoThumpable && (thump = (IsoThumpable)special).isDoor() && north == thump.north) {
                return thump;
            }
            if (!(special instanceof IsoDoor)) continue;
            IsoDoor door = (IsoDoor)special;
            if (north != door.north) continue;
            return door;
        }
        return null;
    }

    public IsoDoor getIsoDoor() {
        for (int n = 0; n < this.specialObjects.size(); ++n) {
            IsoObject special = this.specialObjects.get(n);
            if (!(special instanceof IsoDoor)) continue;
            IsoDoor isoDoor = (IsoDoor)special;
            return isoDoor;
        }
        return null;
    }

    public IsoObject getDoorTo(IsoGridSquare next) {
        IsoObject o;
        if (next == null || next == this) {
            return null;
        }
        if (next.x < this.x && (o = this.getDoor(false)) != null) {
            return o;
        }
        if (next.y < this.y && (o = this.getDoor(true)) != null) {
            return o;
        }
        if (next.x > this.x && (o = next.getDoor(false)) != null) {
            return o;
        }
        if (next.y > this.y && (o = next.getDoor(true)) != null) {
            return o;
        }
        if (next.x != this.x && next.y != this.y) {
            IsoGridSquare betweenA = this.getCell().getGridSquare(this.x, next.y, this.z);
            IsoGridSquare betweenB = this.getCell().getGridSquare(next.x, this.y, this.z);
            o = this.getDoorTo(betweenA);
            if (o != null) {
                return o;
            }
            o = this.getDoorTo(betweenB);
            if (o != null) {
                return o;
            }
            o = next.getDoorTo(betweenA);
            if (o != null) {
                return o;
            }
            o = next.getDoorTo(betweenB);
            if (o != null) {
                return o;
            }
        }
        return null;
    }

    public IsoWindow getWindow(boolean north) {
        for (int n = 0; n < this.specialObjects.size(); ++n) {
            IsoWindow window;
            IsoObject special = this.specialObjects.get(n);
            if (!(special instanceof IsoWindow) || north != (window = (IsoWindow)special).isNorth()) continue;
            return window;
        }
        return null;
    }

    public IsoWindow getWindow() {
        for (int n = 0; n < this.specialObjects.size(); ++n) {
            IsoObject special = this.specialObjects.get(n);
            if (!(special instanceof IsoWindow)) continue;
            IsoWindow isoWindow = (IsoWindow)special;
            return isoWindow;
        }
        return null;
    }

    public IsoWindow getWindowTo(IsoGridSquare next) {
        IsoWindow o;
        if (next == null || next == this) {
            return null;
        }
        if (next.x < this.x && (o = this.getWindow(false)) != null) {
            return o;
        }
        if (next.y < this.y && (o = this.getWindow(true)) != null) {
            return o;
        }
        if (next.x > this.x && (o = next.getWindow(false)) != null) {
            return o;
        }
        if (next.y > this.y && (o = next.getWindow(true)) != null) {
            return o;
        }
        if (next.x != this.x && next.y != this.y) {
            IsoGridSquare betweenA = this.getCell().getGridSquare(this.x, next.y, this.z);
            IsoGridSquare betweenB = this.getCell().getGridSquare(next.x, this.y, this.z);
            o = this.getWindowTo(betweenA);
            if (o != null) {
                return o;
            }
            o = this.getWindowTo(betweenB);
            if (o != null) {
                return o;
            }
            o = next.getWindowTo(betweenA);
            if (o != null) {
                return o;
            }
            o = next.getWindowTo(betweenB);
            if (o != null) {
                return o;
            }
        }
        return null;
    }

    public boolean isAdjacentToWindow() {
        if (this.getWindow() != null) {
            return true;
        }
        if (this.hasWindowFrame()) {
            return true;
        }
        if (this.getThumpableWindow(false) != null || this.getThumpableWindow(true) != null) {
            return true;
        }
        IsoGridSquare s = this.nav[IsoDirections.S.ordinal()];
        if (s != null && (s.getWindow(true) != null || s.getWindowFrame(true) != null || s.getThumpableWindow(true) != null)) {
            return true;
        }
        IsoGridSquare e = this.nav[IsoDirections.E.ordinal()];
        return e != null && (e.getWindow(false) != null || e.getWindowFrame(false) != null || e.getThumpableWindow(false) != null);
    }

    public boolean isAdjacentToHoppable() {
        if (this.getHoppable(true) != null || this.getHoppable(false) != null) {
            return true;
        }
        if (this.getHoppableThumpable(true) != null || this.getHoppableThumpable(false) != null) {
            return true;
        }
        IsoGridSquare s = this.nav[IsoDirections.S.ordinal()];
        if (s != null && (s.getHoppable(true) != null || s.getHoppableThumpable(true) != null)) {
            return true;
        }
        IsoGridSquare e = this.nav[IsoDirections.E.ordinal()];
        return e != null && (e.getHoppable(false) != null || e.getHoppableThumpable(false) != null);
    }

    public IsoThumpable getThumpableWindow(boolean north) {
        for (int n = 0; n < this.specialObjects.size(); ++n) {
            IsoThumpable thump;
            IsoObject special = this.specialObjects.get(n);
            if (!(special instanceof IsoThumpable) || !(thump = (IsoThumpable)special).isWindow() || north != thump.north) continue;
            return thump;
        }
        return null;
    }

    public IsoThumpable getWindowThumpableTo(IsoGridSquare next) {
        IsoThumpable o;
        if (next == null || next == this) {
            return null;
        }
        if (next.x < this.x && (o = this.getThumpableWindow(false)) != null) {
            return o;
        }
        if (next.y < this.y && (o = this.getThumpableWindow(true)) != null) {
            return o;
        }
        if (next.x > this.x && (o = next.getThumpableWindow(false)) != null) {
            return o;
        }
        if (next.y > this.y && (o = next.getThumpableWindow(true)) != null) {
            return o;
        }
        if (next.x != this.x && next.y != this.y) {
            IsoGridSquare betweenA = this.getCell().getGridSquare(this.x, next.y, this.z);
            IsoGridSquare betweenB = this.getCell().getGridSquare(next.x, this.y, this.z);
            o = this.getWindowThumpableTo(betweenA);
            if (o != null) {
                return o;
            }
            o = this.getWindowThumpableTo(betweenB);
            if (o != null) {
                return o;
            }
            o = next.getWindowThumpableTo(betweenA);
            if (o != null) {
                return o;
            }
            o = next.getWindowThumpableTo(betweenB);
            if (o != null) {
                return o;
            }
        }
        return null;
    }

    public IsoThumpable getThumpable(boolean north) {
        for (int n = 0; n < this.specialObjects.size(); ++n) {
            IsoObject special = this.specialObjects.get(n);
            if (!(special instanceof IsoThumpable)) continue;
            IsoThumpable thump = (IsoThumpable)special;
            if (north != thump.north) continue;
            return thump;
        }
        return null;
    }

    public IsoThumpable getHoppableThumpable(boolean north) {
        for (int n = 0; n < this.specialObjects.size(); ++n) {
            IsoThumpable thump;
            IsoObject special = this.specialObjects.get(n);
            if (!(special instanceof IsoThumpable) || !(thump = (IsoThumpable)special).isHoppable() || north != thump.north) continue;
            return thump;
        }
        return null;
    }

    public IsoThumpable getHoppableThumpableTo(IsoGridSquare next) {
        IsoThumpable o;
        if (next == null || next == this) {
            return null;
        }
        if (next.x < this.x && (o = this.getHoppableThumpable(false)) != null) {
            return o;
        }
        if (next.y < this.y && (o = this.getHoppableThumpable(true)) != null) {
            return o;
        }
        if (next.x > this.x && (o = next.getHoppableThumpable(false)) != null) {
            return o;
        }
        if (next.y > this.y && (o = next.getHoppableThumpable(true)) != null) {
            return o;
        }
        if (next.x != this.x && next.y != this.y) {
            IsoGridSquare betweenA = this.getCell().getGridSquare(this.x, next.y, this.z);
            IsoGridSquare betweenB = this.getCell().getGridSquare(next.x, this.y, this.z);
            o = this.getHoppableThumpableTo(betweenA);
            if (o != null) {
                return o;
            }
            o = this.getHoppableThumpableTo(betweenB);
            if (o != null) {
                return o;
            }
            o = next.getHoppableThumpableTo(betweenA);
            if (o != null) {
                return o;
            }
            o = next.getHoppableThumpableTo(betweenB);
            if (o != null) {
                return o;
            }
        }
        return null;
    }

    public IsoObject getWallHoppable(boolean north) {
        for (int i = 0; i < this.objects.size(); ++i) {
            if (!((IsoObject)this.objects.get(i)).isHoppable() || north != ((IsoObject)this.objects.get(i)).isNorthHoppable()) continue;
            return (IsoObject)this.objects.get(i);
        }
        return null;
    }

    public IsoObject getWallHoppableTo(IsoGridSquare next) {
        IsoObject o;
        if (next == null || next == this) {
            return null;
        }
        if (next.x < this.x && (o = this.getWallHoppable(false)) != null) {
            return o;
        }
        if (next.y < this.y && (o = this.getWallHoppable(true)) != null) {
            return o;
        }
        if (next.x > this.x && (o = next.getWallHoppable(false)) != null) {
            return o;
        }
        if (next.y > this.y && (o = next.getWallHoppable(true)) != null) {
            return o;
        }
        if (next.x != this.x && next.y != this.y) {
            IsoGridSquare betweenA = this.getCell().getGridSquare(this.x, next.y, this.z);
            IsoGridSquare betweenB = this.getCell().getGridSquare(next.x, this.y, this.z);
            o = this.getWallHoppableTo(betweenA);
            if (o != null) {
                return o;
            }
            o = this.getWallHoppableTo(betweenB);
            if (o != null) {
                return o;
            }
            o = next.getWallHoppableTo(betweenA);
            if (o != null) {
                return o;
            }
            o = next.getWallHoppableTo(betweenB);
            if (o != null) {
                return o;
            }
        }
        return null;
    }

    public IsoObject getBedTo(IsoGridSquare next) {
        ArrayList<IsoObject> special = next.y < this.y || next.x < this.x ? this.specialObjects : next.specialObjects;
        for (int n = 0; n < special.size(); ++n) {
            IsoObject bed = special.get(n);
            if (!bed.getProperties().has(IsoFlagType.bed)) continue;
            return bed;
        }
        return null;
    }

    public IsoWindowFrame getWindowFrame(boolean north) {
        for (int n = 0; n < this.objects.size(); ++n) {
            IsoWindowFrame windowFrame;
            IsoObject obj = (IsoObject)this.objects.get(n);
            if (!(obj instanceof IsoWindowFrame) || (windowFrame = (IsoWindowFrame)obj).getNorth() != north) continue;
            return windowFrame;
        }
        return null;
    }

    public IsoWindowFrame getWindowFrameTo(IsoGridSquare next) {
        IsoWindowFrame o;
        if (next == null || next == this) {
            return null;
        }
        if (next.x < this.x && (o = this.getWindowFrame(false)) != null) {
            return o;
        }
        if (next.y < this.y && (o = this.getWindowFrame(true)) != null) {
            return o;
        }
        if (next.x > this.x && (o = next.getWindowFrame(false)) != null) {
            return o;
        }
        if (next.y > this.y && (o = next.getWindowFrame(true)) != null) {
            return o;
        }
        if (next.x != this.x && next.y != this.y) {
            IsoGridSquare betweenA = this.getCell().getGridSquare(this.x, next.y, this.z);
            IsoGridSquare betweenB = this.getCell().getGridSquare(next.x, this.y, this.z);
            o = this.getWindowFrameTo(betweenA);
            if (o != null) {
                return o;
            }
            o = this.getWindowFrameTo(betweenB);
            if (o != null) {
                return o;
            }
            o = next.getWindowFrameTo(betweenA);
            if (o != null) {
                return o;
            }
            o = next.getWindowFrameTo(betweenB);
            if (o != null) {
                return o;
            }
        }
        return null;
    }

    public boolean hasWindowFrame() {
        for (int n = 0; n < this.objects.size(); ++n) {
            IsoObject obj = (IsoObject)this.objects.get(n);
            if (!(obj instanceof IsoWindowFrame)) continue;
            return true;
        }
        return false;
    }

    public boolean hasWindowOrWindowFrame() {
        for (int n = 0; n < this.objects.size(); ++n) {
            IsoObject obj = (IsoObject)this.objects.get(n);
            if (obj instanceof IsoWorldInventoryObject || !this.isWindowOrWindowFrame(obj, true) && !this.isWindowOrWindowFrame(obj, false)) continue;
            return true;
        }
        return false;
    }

    private IsoObject getSpecialWall(boolean north) {
        for (int n = this.specialObjects.size() - 1; n >= 0; --n) {
            IsoWindow window;
            IsoObject special = this.specialObjects.get(n);
            if (special instanceof IsoThumpable) {
                IsoThumpable thump = (IsoThumpable)special;
                if (thump.isStairs() || !thump.isThumpable() && !thump.isWindow() && !thump.isDoor() || thump.isDoor() && thump.open || thump.isBlockAllTheSquare() || thump.canPassThrough && thump.getProperties() != null && thump.getContainer() != null && thump.getProperties().has(IsoPropertyType.IS_HIGH)) continue;
                if (north == thump.north && !thump.isCorner()) {
                    return thump;
                }
            }
            if (special instanceof IsoWindow && north == (window = (IsoWindow)special).isNorth()) {
                return window;
            }
            if (!(special instanceof IsoDoor)) continue;
            IsoDoor door = (IsoDoor)special;
            if (north != door.north || door.isOpen()) continue;
            return door;
        }
        if (north && !this.has(IsoFlagType.WindowN) || !north && !this.has(IsoFlagType.WindowW)) {
            return null;
        }
        IsoWindowFrame obj = this.getWindowFrame(north);
        if (obj != null) {
            return obj;
        }
        return null;
    }

    public IsoObject getSheetRope() {
        for (int i = 0; i < this.getObjects().size(); ++i) {
            IsoObject obj = (IsoObject)this.getObjects().get(i);
            if (!obj.sheetRope) continue;
            return obj;
        }
        return null;
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    public boolean damageSpriteSheetRopeFromBottom(IsoPlayer player, boolean north) {
        IsoFlagType type2;
        IsoGridSquare sq = this;
        if (north) {
            if (this.has(IsoFlagType.climbSheetN)) {
                type2 = IsoFlagType.climbSheetN;
            } else {
                if (!this.has(IsoFlagType.climbSheetS)) return false;
                type2 = IsoFlagType.climbSheetS;
            }
        } else if (this.has(IsoFlagType.climbSheetW)) {
            type2 = IsoFlagType.climbSheetW;
        } else {
            if (!this.has(IsoFlagType.climbSheetE)) return false;
            type2 = IsoFlagType.climbSheetE;
        }
        while (sq != null) {
            for (int i = 0; i < sq.getObjects().size(); ++i) {
                IsoObject o = (IsoObject)sq.getObjects().get(i);
                if (o.getProperties() == null || !o.getProperties().has(type2)) continue;
                int index = Integer.parseInt(o.getSprite().getName().split("_")[2]);
                if (index > 14) {
                    return false;
                }
                String spriteName = o.getSprite().getName().split("_")[0] + "_" + o.getSprite().getName().split("_")[1];
                o.setSprite(IsoSpriteManager.instance.getSprite(spriteName + "_" + (index += 40)));
                o.transmitUpdatedSprite();
                break;
            }
            if (sq.getZ() == 7) return true;
            sq = sq.getCell().getGridSquare(sq.getX(), sq.getY(), sq.getZ() + 1);
        }
        return true;
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    public boolean removeSheetRopeFromBottom(IsoPlayer player, boolean north) {
        IsoGridSquare topSq;
        IsoFlagType type2;
        IsoFlagType type1;
        IsoGridSquare sq = this;
        if (north) {
            if (this.has(IsoFlagType.climbSheetN)) {
                type1 = IsoFlagType.climbSheetTopN;
                type2 = IsoFlagType.climbSheetN;
            } else {
                if (!this.has(IsoFlagType.climbSheetS)) return false;
                type1 = IsoFlagType.climbSheetTopS;
                type2 = IsoFlagType.climbSheetS;
                tile = "crafted_01_4";
                for (i = 0; i < sq.getObjects().size(); ++i) {
                    o = (IsoObject)sq.getObjects().get(i);
                    if (o.sprite == null || o.sprite.getName() == null || !o.sprite.getName().equals(tile)) continue;
                    sq.transmitRemoveItemFromSquare(o);
                    break;
                }
            }
        } else if (this.has(IsoFlagType.climbSheetW)) {
            type1 = IsoFlagType.climbSheetTopW;
            type2 = IsoFlagType.climbSheetW;
        } else {
            if (!this.has(IsoFlagType.climbSheetE)) return false;
            type1 = IsoFlagType.climbSheetTopE;
            type2 = IsoFlagType.climbSheetE;
            tile = "crafted_01_3";
            for (i = 0; i < sq.getObjects().size(); ++i) {
                o = (IsoObject)sq.getObjects().get(i);
                if (o.sprite == null || o.sprite.getName() == null || !o.sprite.getName().equals(tile)) continue;
                sq.transmitRemoveItemFromSquare(o);
                break;
            }
        }
        boolean find = false;
        IsoGridSquare previousSq = null;
        while (sq != null) {
            for (int i = 0; i < sq.getObjects().size(); ++i) {
                IsoObject o = (IsoObject)sq.getObjects().get(i);
                if (o.getProperties() == null || !o.getProperties().has(type1) && !o.getProperties().has(type2)) continue;
                previousSq = sq;
                find = true;
                sq.transmitRemoveItemFromSquare(o);
                if (GameServer.server) {
                    if (player == null) break;
                    player.sendObjectChange(IsoObjectChange.ADD_ITEM_OF_TYPE, new Object[]{"type", o.getName()});
                    break;
                }
                if (player == null) break;
                player.getInventory().AddItem(o.getName());
                break;
            }
            if (sq.getZ() == 7) break;
            sq = sq.getCell().getGridSquare(sq.getX(), sq.getY(), sq.getZ() + 1);
            find = false;
        }
        if (find) return true;
        sq = previousSq.getCell().getGridSquare(previousSq.getX(), previousSq.getY(), previousSq.getZ());
        IsoGridSquare isoGridSquare = topSq = north ? sq.nav[IsoDirections.S.ordinal()] : sq.nav[IsoDirections.E.ordinal()];
        if (topSq == null) {
            return true;
        }
        for (int i = 0; i < topSq.getObjects().size(); ++i) {
            IsoObject o = (IsoObject)topSq.getObjects().get(i);
            if (o.getProperties() == null || !o.getProperties().has(type1) && !o.getProperties().has(type2)) continue;
            topSq.transmitRemoveItemFromSquare(o);
            return true;
        }
        return true;
    }

    private IsoObject getSpecialSolid() {
        for (int n = 0; n < this.specialObjects.size(); ++n) {
            IsoThumpable thump;
            IsoObject special = this.specialObjects.get(n);
            if (!(special instanceof IsoThumpable) || (thump = (IsoThumpable)special).isStairs() || !thump.isThumpable() || !thump.isBlockAllTheSquare()) continue;
            if (thump.getProperties().has(IsoFlagType.solidtrans) && (this.isAdjacentToWindow() || this.isAdjacentToHoppable())) {
                return null;
            }
            return thump;
        }
        for (int i = 0; i < this.objects.size(); ++i) {
            IsoObject obj = (IsoObject)this.objects.get(i);
            if (!obj.isMovedThumpable()) continue;
            if (this.isAdjacentToWindow() || this.isAdjacentToHoppable()) {
                return null;
            }
            return obj;
        }
        return null;
    }

    public IsoObject testCollideSpecialObjects(IsoGridSquare next) {
        if (next == null || next == this) {
            return null;
        }
        if (next.x < this.x && next.y == this.y) {
            if (next.z == this.z && this.has(IsoObjectType.stairsTW)) {
                return null;
            }
            if (next.z == this.z && this.hasSlopedSurfaceToLevelAbove(IsoDirections.W)) {
                return null;
            }
            IsoObject o = this.getSpecialWall(false);
            if (o != null) {
                return o;
            }
            if (this.isBlockedTo(next)) {
                return null;
            }
            o = next.getSpecialSolid();
            if (o != null) {
                return o;
            }
            return null;
        }
        if (next.x == this.x && next.y < this.y) {
            if (next.z == this.z && this.has(IsoObjectType.stairsTN)) {
                return null;
            }
            if (next.z == this.z && this.hasSlopedSurfaceToLevelAbove(IsoDirections.N)) {
                return null;
            }
            IsoObject o = this.getSpecialWall(true);
            if (o != null) {
                return o;
            }
            if (this.isBlockedTo(next)) {
                return null;
            }
            o = next.getSpecialSolid();
            if (o != null) {
                return o;
            }
            return null;
        }
        if (next.x > this.x && next.y == this.y) {
            IsoObject o = next.getSpecialWall(false);
            if (o != null) {
                return o;
            }
            if (this.isBlockedTo(next)) {
                return null;
            }
            o = next.getSpecialSolid();
            if (o != null) {
                return o;
            }
            return null;
        }
        if (next.x == this.x && next.y > this.y) {
            IsoObject o = next.getSpecialWall(true);
            if (o != null) {
                return o;
            }
            if (this.isBlockedTo(next)) {
                return null;
            }
            o = next.getSpecialSolid();
            if (o != null) {
                return o;
            }
            return null;
        }
        if (next.x < this.x && next.y < this.y) {
            IsoGridSquare betweenB;
            IsoObject o = this.getSpecialWall(true);
            if (o != null) {
                return o;
            }
            o = this.getSpecialWall(false);
            if (o != null) {
                return o;
            }
            IsoGridSquare betweenA = this.getCell().getGridSquare(this.x, this.y - 1, this.z);
            if (betweenA != null && !this.isBlockedTo(betweenA)) {
                o = betweenA.getSpecialSolid();
                if (o != null) {
                    return o;
                }
                o = betweenA.getSpecialWall(false);
                if (o != null) {
                    return o;
                }
            }
            if ((betweenB = this.getCell().getGridSquare(this.x - 1, this.y, this.z)) != null && !this.isBlockedTo(betweenB)) {
                o = betweenB.getSpecialSolid();
                if (o != null) {
                    return o;
                }
                o = betweenB.getSpecialWall(true);
                if (o != null) {
                    return o;
                }
            }
            if (betweenA == null || this.isBlockedTo(betweenA) || betweenB == null || this.isBlockedTo(betweenB)) {
                return null;
            }
            if (betweenA.isBlockedTo(next) || betweenB.isBlockedTo(next)) {
                return null;
            }
            o = next.getSpecialSolid();
            if (o != null) {
                return o;
            }
            return null;
        }
        if (next.x > this.x && next.y < this.y) {
            IsoObject o = this.getSpecialWall(true);
            if (o != null) {
                return o;
            }
            IsoGridSquare betweenA = this.getCell().getGridSquare(this.x, this.y - 1, this.z);
            if (betweenA != null && !this.isBlockedTo(betweenA) && (o = betweenA.getSpecialSolid()) != null) {
                return o;
            }
            IsoGridSquare betweenB = this.getCell().getGridSquare(this.x + 1, this.y, this.z);
            if (betweenB != null) {
                o = betweenB.getSpecialWall(false);
                if (o != null) {
                    return o;
                }
                if (!this.isBlockedTo(betweenB)) {
                    o = betweenB.getSpecialSolid();
                    if (o != null) {
                        return o;
                    }
                    o = betweenB.getSpecialWall(true);
                    if (o != null) {
                        return o;
                    }
                }
            }
            if (betweenA == null || this.isBlockedTo(betweenA) || betweenB == null || this.isBlockedTo(betweenB)) {
                return null;
            }
            o = next.getSpecialWall(false);
            if (o != null) {
                return o;
            }
            if (betweenA.isBlockedTo(next) || betweenB.isBlockedTo(next)) {
                return null;
            }
            o = next.getSpecialSolid();
            if (o != null) {
                return o;
            }
            return null;
        }
        if (next.x > this.x && next.y > this.y) {
            IsoGridSquare betweenB;
            IsoObject o;
            IsoGridSquare betweenA = this.getCell().getGridSquare(this.x, this.y + 1, this.z);
            if (betweenA != null) {
                o = betweenA.getSpecialWall(true);
                if (o != null) {
                    return o;
                }
                if (!this.isBlockedTo(betweenA) && (o = betweenA.getSpecialSolid()) != null) {
                    return o;
                }
            }
            if ((betweenB = this.getCell().getGridSquare(this.x + 1, this.y, this.z)) != null) {
                o = betweenB.getSpecialWall(false);
                if (o != null) {
                    return o;
                }
                if (!this.isBlockedTo(betweenB) && (o = betweenB.getSpecialSolid()) != null) {
                    return o;
                }
            }
            if (betweenA == null || this.isBlockedTo(betweenA) || betweenB == null || this.isBlockedTo(betweenB)) {
                return null;
            }
            o = next.getSpecialWall(false);
            if (o != null) {
                return o;
            }
            o = next.getSpecialWall(true);
            if (o != null) {
                return o;
            }
            if (betweenA.isBlockedTo(next) || betweenB.isBlockedTo(next)) {
                return null;
            }
            o = next.getSpecialSolid();
            if (o != null) {
                return o;
            }
            return null;
        }
        if (next.x < this.x && next.y > this.y) {
            IsoGridSquare betweenB;
            IsoObject o = this.getSpecialWall(false);
            if (o != null) {
                return o;
            }
            IsoGridSquare betweenA = this.getCell().getGridSquare(this.x, this.y + 1, this.z);
            if (betweenA != null) {
                o = betweenA.getSpecialWall(true);
                if (o != null) {
                    return o;
                }
                if (!this.isBlockedTo(betweenA) && (o = betweenA.getSpecialSolid()) != null) {
                    return o;
                }
            }
            if ((betweenB = this.getCell().getGridSquare(this.x - 1, this.y, this.z)) != null && !this.isBlockedTo(betweenB) && (o = betweenB.getSpecialSolid()) != null) {
                return o;
            }
            if (betweenA == null || this.isBlockedTo(betweenA) || betweenB == null || this.isBlockedTo(betweenB)) {
                return null;
            }
            o = next.getSpecialWall(true);
            if (o != null) {
                return o;
            }
            if (betweenA.isBlockedTo(next) || betweenB.isBlockedTo(next)) {
                return null;
            }
            o = next.getSpecialSolid();
            if (o != null) {
                return o;
            }
            return null;
        }
        return null;
    }

    public IsoObject getDoorFrameTo(IsoGridSquare next) {
        ArrayList<IsoObject> special = next.y < this.y || next.x < this.x ? this.specialObjects : next.specialObjects;
        for (int n = 0; n < special.size(); ++n) {
            IsoObject isoObject = special.get(n);
            if (isoObject instanceof IsoDoor) {
                IsoDoor door = (IsoDoor)isoObject;
                boolean no = door.north;
                if (no && next.y != this.y) {
                    return door;
                }
                if (no || next.x == this.x) continue;
                return door;
            }
            IsoObject no = special.get(n);
            if (!(no instanceof IsoThumpable)) continue;
            IsoThumpable door = (IsoThumpable)no;
            if (!((IsoThumpable)special.get(n)).isDoor()) continue;
            boolean no2 = door.north;
            if (no2 && next.y != this.y) {
                return door;
            }
            if (no2 || next.x == this.x) continue;
            return door;
        }
        return null;
    }

    public static void getSquaresForThread(ArrayDeque<IsoGridSquare> isoGridSquareCacheDest, int count) {
        for (int xx = 0; xx < count; ++xx) {
            IsoGridSquare sq = isoGridSquareCache.poll();
            if (sq == null) {
                isoGridSquareCacheDest.add(new IsoGridSquare(null, null, 0, 0, 0));
                continue;
            }
            isoGridSquareCacheDest.add(sq);
        }
    }

    public static IsoGridSquare getNew(IsoCell cell, SliceY slice, int x, int y, int z) {
        IsoGridSquare sq = isoGridSquareCache.poll();
        if (sq == null) {
            return new IsoGridSquare(cell, slice, x, y, z);
        }
        sq.x = x;
        sq.y = y;
        sq.z = z;
        sq.cachedScreenValue = -1;
        col = 0;
        path = 0;
        pathdoor = 0;
        vision = 0;
        sq.collideMatrix = 0x7FFFFFF;
        sq.pathMatrix = 0x7FFFFFF;
        sq.visionMatrix = 0;
        return sq;
    }

    public static IsoGridSquare getNew(ArrayDeque<IsoGridSquare> isoGridSquareCache, IsoCell cell, SliceY slice, int x, int y, int z) {
        if (isoGridSquareCache.isEmpty()) {
            return new IsoGridSquare(cell, slice, x, y, z);
        }
        IsoGridSquare sq = isoGridSquareCache.pop();
        sq.x = x;
        sq.y = y;
        sq.z = z;
        sq.cachedScreenValue = -1;
        col = 0;
        path = 0;
        pathdoor = 0;
        vision = 0;
        sq.collideMatrix = 0x7FFFFFF;
        sq.pathMatrix = 0x7FFFFFF;
        sq.visionMatrix = 0;
        return sq;
    }

    @Deprecated
    public long getHashCodeObjects() {
        this.recalcHashCodeObjects();
        return this.hashCodeObjects;
    }

    @Deprecated
    public int getHashCodeObjectsInt() {
        this.recalcHashCodeObjects();
        return (int)this.hashCodeObjects;
    }

    @Deprecated
    public void recalcHashCodeObjects() {
        long h;
        this.hashCodeObjects = h = 0L;
    }

    @Deprecated
    public int hashCodeNoOverride() {
        int n;
        int h = 0;
        this.recalcHashCodeObjects();
        h = h * 2 + this.objects.size();
        h = (int)((long)h + this.getHashCodeObjects());
        for (int n2 = 0; n2 < this.objects.size(); ++n2) {
            h = h * 2 + ((IsoObject)this.objects.get(n2)).hashCode();
        }
        int bodyCount = 0;
        for (n = 0; n < this.staticMovingObjects.size(); ++n) {
            if (!(this.staticMovingObjects.get(n) instanceof IsoDeadBody)) continue;
            ++bodyCount;
        }
        h = h * 2 + bodyCount;
        for (n = 0; n < this.staticMovingObjects.size(); ++n) {
            IsoMovingObject body = this.staticMovingObjects.get(n);
            if (!(body instanceof IsoDeadBody)) continue;
            h = h * 2 + body.hashCode();
        }
        if (this.table != null && !this.table.isEmpty()) {
            h = h * 2 + this.table.hashCode();
        }
        byte flags = 0;
        if (this.isOverlayDone()) {
            flags = (byte)(flags | 1);
        }
        if (this.haveRoof) {
            flags = (byte)(flags | 2);
        }
        if (this.burntOut) {
            flags = (byte)(flags | 4);
        }
        h = h * 2 + flags;
        h = h * 2 + this.getErosionData().hashCode();
        if (this.getTrapPositionX() > 0) {
            h = h * 2 + this.getTrapPositionX();
            h = h * 2 + this.getTrapPositionY();
            h = h * 2 + this.getTrapPositionZ();
        }
        h = h * 2 + (this.haveElectricity() ? 1 : 0);
        h = h * 2 + (this.haveSheetRope ? 1 : 0);
        return h;
    }

    public IsoGridSquare(IsoCell cell, SliceY slice, int x, int y, int z) {
        this.id = ++idMax;
        this.x = x;
        this.y = y;
        this.z = z;
        this.cachedScreenValue = -1;
        col = 0;
        path = 0;
        pathdoor = 0;
        vision = 0;
        this.collideMatrix = 0x7FFFFFF;
        this.pathMatrix = 0x7FFFFFF;
        this.visionMatrix = 0;
        for (int i = 0; i < 4; ++i) {
            if (GameServer.server) {
                if (i != 0) continue;
                this.lighting[i] = new ServerLOS.ServerLighting();
                continue;
            }
            this.lighting[i] = LightingJNI.init ? new LightingJNI.JNILighting(i, this) : new Lighting();
        }
    }

    public IsoGridSquare getTileInDirection(IsoDirections directions) {
        if (directions == IsoDirections.N) {
            return this.getCell().getGridSquare(this.x, this.y - 1, this.z);
        }
        if (directions == IsoDirections.NE) {
            return this.getCell().getGridSquare(this.x + 1, this.y - 1, this.z);
        }
        if (directions == IsoDirections.NW) {
            return this.getCell().getGridSquare(this.x - 1, this.y - 1, this.z);
        }
        if (directions == IsoDirections.E) {
            return this.getCell().getGridSquare(this.x + 1, this.y, this.z);
        }
        if (directions == IsoDirections.W) {
            return this.getCell().getGridSquare(this.x - 1, this.y, this.z);
        }
        if (directions == IsoDirections.SE) {
            return this.getCell().getGridSquare(this.x + 1, this.y + 1, this.z);
        }
        if (directions == IsoDirections.SW) {
            return this.getCell().getGridSquare(this.x - 1, this.y + 1, this.z);
        }
        if (directions == IsoDirections.S) {
            return this.getCell().getGridSquare(this.x, this.y + 1, this.z);
        }
        return null;
    }

    public IsoObject getWall() {
        for (int n = 0; n < this.objects.size(); ++n) {
            IsoObject obj = (IsoObject)this.objects.get(n);
            if (obj == null || obj.sprite == null || !obj.sprite.cutW && !obj.sprite.cutN) continue;
            return obj;
        }
        return null;
    }

    public IsoObject getThumpableWall(boolean bNorth) {
        IsoObject obj = this.getWall(bNorth);
        if (obj != null && obj instanceof IsoThumpable) {
            return obj;
        }
        return null;
    }

    public IsoObject getHoppableWall(boolean bNorth) {
        for (int n = 0; n < this.objects.size(); ++n) {
            boolean bTallHoppableN;
            IsoObject obj = (IsoObject)this.objects.get(n);
            if (obj == null || obj.sprite == null) continue;
            PropertyContainer properties = obj.getProperties();
            boolean bTallHoppableW = properties.has(IsoFlagType.TallHoppableW) && !properties.has(IsoFlagType.WallWTrans);
            boolean bl = bTallHoppableN = properties.has(IsoFlagType.TallHoppableN) && !properties.has(IsoFlagType.WallNTrans);
            if ((!bTallHoppableW || bNorth) && (!bTallHoppableN || !bNorth)) continue;
            return obj;
        }
        return null;
    }

    public IsoObject getThumpableWallOrHoppable(boolean bNorth) {
        IsoObject thumpableWall = this.getThumpableWall(bNorth);
        IsoObject hoppable = this.getHoppableWall(bNorth);
        if (thumpableWall != null && hoppable != null && thumpableWall == hoppable) {
            return thumpableWall;
        }
        if (thumpableWall == null && hoppable != null) {
            return hoppable;
        }
        if (thumpableWall != null && hoppable == null) {
            return thumpableWall;
        }
        return null;
    }

    public Boolean getWallFull() {
        for (int n = 0; n < this.objects.size(); ++n) {
            IsoObject obj = (IsoObject)this.objects.get(n);
            if (obj == null || obj.sprite == null || !obj.sprite.cutN && !obj.sprite.cutW && !obj.sprite.getProperties().has(IsoFlagType.WallN) && !obj.sprite.getProperties().has(IsoFlagType.WallW)) continue;
            return true;
        }
        return false;
    }

    public boolean hasNonHoppableWall(boolean isNorth) {
        for (int n = 0; n < this.objects.size(); ++n) {
            IsoWindow window;
            IsoObject obj = (IsoObject)this.objects.get(n);
            if (obj == null || obj.sprite == null || obj.isWallSE()) continue;
            if (obj instanceof IsoWindow && (window = (IsoWindow)obj).getNorth() == isNorth) {
                return true;
            }
            if (!(isNorth ? (obj.sprite.cutN || obj.sprite.getProperties().has(IsoFlagType.WallN) || obj.sprite.getProperties().has(IsoFlagType.WallNTrans)) && !obj.sprite.getProperties().has(IsoFlagType.HoppableN) && !obj.sprite.getProperties().has(IsoFlagType.TallHoppableN) : (obj.sprite.cutW || obj.sprite.getProperties().has(IsoFlagType.WallW) || obj.sprite.getProperties().has(IsoFlagType.WallWTrans)) && !obj.sprite.getProperties().has(IsoFlagType.HoppableW) && !obj.sprite.getProperties().has(IsoFlagType.TallHoppableW))) continue;
            return true;
        }
        return false;
    }

    public boolean isPlayerAbleToHopWallTo(IsoDirections dir, IsoGridSquare oppositeSq) {
        if (!this.isAdjacentTo(oppositeSq)) {
            return false;
        }
        if (this.HasStairs() && this.has(IsoObjectType.stairsTN) || this.has(IsoObjectType.stairsTW)) {
            if (this.getSquareAbove() != null && oppositeSq.getSquareAbove() != null) {
                switch (1.$SwitchMap$zombie$iso$IsoDirections[dir.ordinal()]) {
                    case 1: {
                        if (!this.getSquareAbove().hasNonHoppableWall(true)) break;
                        return false;
                    }
                    case 2: {
                        if (!oppositeSq.getSquareAbove().hasNonHoppableWall(true)) break;
                        return false;
                    }
                    case 3: {
                        if (!this.getSquareAbove().hasNonHoppableWall(false)) break;
                        return false;
                    }
                    case 4: {
                        if (!oppositeSq.getSquareAbove().hasNonHoppableWall(false)) break;
                        return false;
                    }
                }
            }
        } else {
            switch (1.$SwitchMap$zombie$iso$IsoDirections[dir.ordinal()]) {
                case 1: {
                    if (!this.hasNonHoppableWall(true)) break;
                    return false;
                }
                case 2: {
                    if (!oppositeSq.hasNonHoppableWall(true)) break;
                    return false;
                }
                case 3: {
                    if (!this.hasNonHoppableWall(false)) break;
                    return false;
                }
                case 4: {
                    if (!oppositeSq.hasNonHoppableWall(false)) break;
                    return false;
                }
            }
        }
        return true;
    }

    IsoObject getWallExcludingList(boolean bNorth, ArrayList<String> excluded) {
        for (int n = 0; n < this.objects.size(); ++n) {
            IsoObject obj = (IsoObject)this.objects.get(n);
            if (obj == null || obj.sprite == null || excluded.contains(obj.sprite.name) || obj.isWallSE() || bNorth && obj.hasProperty(IsoFlagType.DoorWallN) || !bNorth && obj.hasProperty(IsoFlagType.DoorWallW) || (!bNorth || !obj.sprite.cutN && !obj.hasProperty(IsoFlagType.WallN)) && (bNorth || !obj.sprite.cutW && !obj.hasProperty(IsoFlagType.WallW))) continue;
            return obj;
        }
        return null;
    }

    public IsoObject getWallExcludingObject(boolean bNorth, IsoObject exclude) {
        for (int n = 0; n < this.objects.size(); ++n) {
            IsoObject obj = (IsoObject)this.objects.get(n);
            if (obj == null || obj.sprite == null || obj == exclude || obj.isWallSE() || (!bNorth || !obj.sprite.cutN && !obj.sprite.getProperties().has(IsoFlagType.WallN)) && (bNorth || !obj.sprite.cutW && !obj.sprite.getProperties().has(IsoFlagType.WallW))) continue;
            return obj;
        }
        return null;
    }

    public IsoObject getWall(boolean bNorth) {
        for (int n = 0; n < this.objects.size(); ++n) {
            IsoObject obj = (IsoObject)this.objects.get(n);
            if (obj == null || obj.sprite == null || obj.isWallSE() || (!obj.sprite.cutN || !bNorth) && (!obj.sprite.cutW || bNorth)) continue;
            return obj;
        }
        return null;
    }

    public IsoObject getWallSE() {
        for (int n = 0; n < this.objects.size(); ++n) {
            IsoObject obj = (IsoObject)this.objects.get(n);
            if (obj == null || obj.sprite == null || !obj.isWallSE()) continue;
            return obj;
        }
        return null;
    }

    public IsoObject getWallNW() {
        for (int n = 0; n < this.objects.size(); ++n) {
            IsoObject obj = (IsoObject)this.objects.get(n);
            if (obj == null || obj.sprite == null || !obj.sprite.getProperties().has(IsoFlagType.WallNW)) continue;
            return obj;
        }
        return null;
    }

    public IsoObject getGarageDoor(boolean bNorth) {
        for (int n = 0; n < this.objects.size(); ++n) {
            boolean bNorth2;
            IsoObject obj = (IsoObject)this.objects.get(n);
            if (IsoDoor.getGarageDoorIndex((IsoObject)obj) == -1) continue;
            if (obj instanceof IsoDoor) {
                IsoDoor isoDoor = (IsoDoor)obj;
                v0 = isoDoor.getNorth();
            } else {
                v0 = bNorth2 = ((IsoThumpable)obj).getNorth();
            }
            if (bNorth != bNorth2) continue;
            return obj;
        }
        return null;
    }

    public IsoObject getFloor() {
        for (int n = 0; n < this.objects.size(); ++n) {
            IsoObject obj = (IsoObject)this.objects.get(n);
            if (obj.sprite == null || !obj.sprite.getProperties().has(IsoFlagType.solidfloor)) continue;
            return obj;
        }
        return null;
    }

    public IsoObject getPlayerBuiltFloor() {
        if (this.getBuilding() != null || this.roofHideBuilding != null && !this.roofHideBuilding.isEntirelyEmptyOutside()) {
            return null;
        }
        return this.getFloor();
    }

    public IsoObject getWaterObject() {
        for (int n = 0; n < this.objects.size(); ++n) {
            IsoObject obj = (IsoObject)this.objects.get(n);
            if (obj.sprite == null || !obj.sprite.getProperties().has(IsoFlagType.water)) continue;
            return obj;
        }
        return null;
    }

    public void interpolateLight(ColorInfo inf, float x, float y) {
        IsoCell cell = this.getCell();
        if (x < 0.0f) {
            x = 0.0f;
        }
        if (x > 1.0f) {
            x = 1.0f;
        }
        if (y < 0.0f) {
            y = 0.0f;
        }
        if (y > 1.0f) {
            y = 1.0f;
        }
        int playerIndex = IsoCamera.frameState.playerIndex;
        int coltl = this.getVertLight(0, playerIndex);
        int coltr = this.getVertLight(1, playerIndex);
        int colbr = this.getVertLight(2, playerIndex);
        int colbl = this.getVertLight(3, playerIndex);
        Color.abgrToColor((int)coltl, (Color)tl);
        Color.abgrToColor((int)colbl, (Color)bl);
        Color.abgrToColor((int)coltr, (Color)tr);
        Color.abgrToColor((int)colbr, (Color)br);
        tl.interp(tr, x, interp1);
        bl.interp(br, x, interp2);
        interp1.interp(interp2, y, finalCol);
        inf.r = IsoGridSquare.finalCol.r;
        inf.g = IsoGridSquare.finalCol.g;
        inf.b = IsoGridSquare.finalCol.b;
        inf.a = IsoGridSquare.finalCol.a;
    }

    public void EnsureSurroundNotNull() {
        assert (!GameServer.server);
        for (int x = -1; x <= 1; ++x) {
            for (int y = -1; y <= 1; ++y) {
                if (x == 0 && y == 0 || !IsoWorld.instance.isValidSquare(this.x + x, this.y + y, this.z) || this.getCell().getChunkForGridSquare(this.x + x, this.y + y, this.z) == null) continue;
                boolean created = false;
                IsoGridSquare sq = this.getCell().getGridSquare(this.x + x, this.y + y, this.z);
                if (sq != null) continue;
                sq = IsoGridSquare.getNew(this.getCell(), null, this.x + x, this.y + y, this.z);
                IsoGridSquare newSq = this.getCell().ConnectNewSquare(sq, false);
                boolean bl = true;
            }
        }
    }

    public void setSquareChanged() {
        this.setCachedIsFree(false);
        PolygonalMap2.instance.squareChanged(this);
        IsoGridOcclusionData.SquareChanged();
        IsoRegions.squareChanged((IsoGridSquare)this);
    }

    public IsoObject addFloor(String sprite) {
        IsoRegions.setPreviousFlags((IsoGridSquare)this);
        IsoObject obj = new IsoObject(this.getCell(), this, sprite);
        boolean hasRug = false;
        for (int nn = 0; nn < this.getObjects().size(); ++nn) {
            IsoObject o = (IsoObject)this.getObjects().get(nn);
            IsoSprite ss = o.sprite;
            if (ss == null || !ss.getProperties().has(IsoFlagType.solidfloor) && !ss.getProperties().has(IsoFlagType.noStart) && (!ss.getProperties().has(IsoFlagType.vegitation) || o.getType() == IsoObjectType.tree) && !ss.getProperties().has(IsoFlagType.taintedWater) && (ss.getName() == null || !ss.getName().startsWith("blends_grassoverlays"))) continue;
            if (ss.getName() != null && ss.getName().startsWith("floors_rugs")) {
                hasRug = true;
                continue;
            }
            this.transmitRemoveItemFromSquare(o);
            --nn;
        }
        obj.sprite.getProperties().set(IsoFlagType.solidfloor);
        if (hasRug) {
            this.getObjects().add(0, (Object)obj);
        } else {
            this.getObjects().add((Object)obj);
        }
        this.EnsureSurroundNotNull();
        this.RecalcProperties();
        DesignationZoneAnimal.addNewRoof((int)this.x, (int)this.y, (int)this.z);
        this.getCell().checkHaveRoof(this.x, this.y);
        for (int pn = 0; pn < IsoPlayer.numPlayers; ++pn) {
            LosUtil.cachecleared[pn] = true;
        }
        IsoGridSquare.setRecalcLightTime(-1.0f);
        if (PerformanceSettings.fboRenderChunk) {
            ++Core.dirtyGlobalLightsCount;
        }
        GameTime.getInstance().lightSourceUpdate = 100.0f;
        obj.transmitCompleteItemToClients();
        this.RecalcAllWithNeighbours(true);
        for (int z1 = this.z - 1; z1 > 0; --z1) {
            IsoGridSquare below = this.getCell().getGridSquare(this.x, this.y, z1);
            if (below == null) {
                below = IsoGridSquare.getNew(this.getCell(), null, this.x, this.y, z1);
                this.getCell().ConnectNewSquare(below, false);
            }
            below.EnsureSurroundNotNull();
            below.RecalcAllWithNeighbours(true);
        }
        this.setCachedIsFree(false);
        PolygonalMap2.instance.squareChanged(this);
        IsoGridOcclusionData.SquareChanged();
        IsoRegions.squareChanged((IsoGridSquare)this);
        this.clearWater();
        obj.invalidateRenderChunkLevel(64L);
        return obj;
    }

    public IsoObject addUndergroundBlock(String sprite) {
        IsoRegions.setPreviousFlags((IsoGridSquare)this);
        IsoObject obj = new IsoObject(this.getCell(), this, sprite);
        obj.sprite.getProperties().set(IsoFlagType.solid);
        this.getObjects().add((Object)obj);
        this.RecalcProperties();
        for (int pn = 0; pn < IsoPlayer.numPlayers; ++pn) {
            LosUtil.cachecleared[pn] = true;
        }
        ++Core.dirtyGlobalLightsCount;
        IsoGridSquare.setRecalcLightTime(-1.0f);
        GameTime.getInstance().lightSourceUpdate = 100.0f;
        this.RecalcAllWithNeighbours(true);
        this.setCachedIsFree(false);
        PolygonalMap2.instance.squareChanged(this);
        IsoGridOcclusionData.SquareChanged();
        IsoRegions.squareChanged((IsoGridSquare)this);
        this.clearWater();
        return obj;
    }

    public boolean isUndergroundBlock() {
        if (this.getObjects().size() != 1) {
            return false;
        }
        IsoObject object = (IsoObject)this.getObjects().get(0);
        return object != null && object.getSprite() != null && object.getSprite().getName() != null && object.getSprite().getName().startsWith("underground_01");
    }

    public IsoThumpable AddStairs(boolean north, int level, String sprite, String pillarSprite, KahluaTable table) {
        IsoRegions.setPreviousFlags((IsoGridSquare)this);
        this.EnsureSurroundNotNull();
        boolean floating = !this.TreatAsSolidFloor() && !this.HasStairsBelow();
        this.cachedIsFree = false;
        IsoThumpable obj = new IsoThumpable(this.getCell(), this, sprite, north, table);
        if (north) {
            if (level == 0) {
                obj.setType(IsoObjectType.stairsBN);
            }
            if (level == 1) {
                obj.setType(IsoObjectType.stairsMN);
            }
            if (level == 2) {
                obj.setType(IsoObjectType.stairsTN);
                obj.sprite.getProperties().set(north ? IsoFlagType.cutN : IsoFlagType.cutW);
            }
        }
        if (!north) {
            if (level == 0) {
                obj.setType(IsoObjectType.stairsBW);
            }
            if (level == 1) {
                obj.setType(IsoObjectType.stairsMW);
            }
            if (level == 2) {
                obj.setType(IsoObjectType.stairsTW);
                obj.sprite.getProperties().set(north ? IsoFlagType.cutN : IsoFlagType.cutW);
            }
        }
        this.AddSpecialObject((IsoObject)obj);
        if (floating && level == 2) {
            int zI = this.z - 1;
            IsoGridSquare sq = this.getCell().getGridSquare(this.x, this.y, zI);
            if (sq == null) {
                sq = new IsoGridSquare(this.getCell(), null, this.x, this.y, zI);
                this.getCell().ConnectNewSquare(sq, true);
            }
            while (zI >= 0) {
                IsoThumpable obj2 = new IsoThumpable(this.getCell(), sq, pillarSprite, north, table);
                sq.AddSpecialObject((IsoObject)obj2);
                if (sq.TreatAsSolidFloor()) break;
                if (this.getCell().getGridSquare(sq.x, sq.y, --zI) == null) {
                    sq = new IsoGridSquare(this.getCell(), null, sq.x, sq.y, zI);
                    this.getCell().ConnectNewSquare(sq, true);
                    continue;
                }
                sq = this.getCell().getGridSquare(sq.x, sq.y, zI);
            }
        }
        if (level == 2) {
            IsoGridSquare above = null;
            if (north) {
                if (IsoWorld.instance.isValidSquare(this.x, this.y - 1, this.z + 1)) {
                    above = this.getCell().getGridSquare(this.x, this.y - 1, this.z + 1);
                    if (above == null) {
                        above = new IsoGridSquare(this.getCell(), null, this.x, this.y - 1, this.z + 1);
                        this.getCell().ConnectNewSquare(above, false);
                    }
                    if (!above.properties.has(IsoFlagType.solidfloor)) {
                        above.addFloor("carpentry_02_57");
                    }
                }
            } else if (IsoWorld.instance.isValidSquare(this.x - 1, this.y, this.z + 1)) {
                above = this.getCell().getGridSquare(this.x - 1, this.y, this.z + 1);
                if (above == null) {
                    above = new IsoGridSquare(this.getCell(), null, this.x - 1, this.y, this.z + 1);
                    this.getCell().ConnectNewSquare(above, false);
                }
                if (!above.properties.has(IsoFlagType.solidfloor)) {
                    above.addFloor("carpentry_02_57");
                }
            }
            above.getModData().rawset((Object)("ConnectedToStairs" + north), (Object)true);
            above = this.getCell().getGridSquare(this.x, this.y, this.z + 1);
            if (above == null) {
                above = new IsoGridSquare(this.getCell(), null, this.x, this.y, this.z + 1);
                this.getCell().ConnectNewSquare(above, false);
            }
        }
        for (int x = this.getX() - 1; x <= this.getX() + 1; ++x) {
            for (int y = this.getY() - 1; y <= this.getY() + 1; ++y) {
                for (int z = this.getZ() - 1; z <= this.getZ() + 1; ++z) {
                    IsoGridSquare sq;
                    if (!IsoWorld.instance.isValidSquare(x, y, z) || (sq = this.getCell().getGridSquare(x, y, z)) == this) continue;
                    if (sq == null) {
                        sq = new IsoGridSquare(this.getCell(), null, x, y, z);
                        this.getCell().ConnectNewSquare(sq, false);
                    }
                    sq.RecalcAllWithNeighbours(true);
                }
            }
        }
        return obj;
    }

    void ReCalculateAll(IsoGridSquare a) {
        this.ReCalculateAll(a, cellGetSquare);
    }

    void ReCalculateAll(IsoGridSquare a, GetSquare getter) {
        if (a == null || a == this) {
            return;
        }
        this.solidFloorCached = false;
        a.solidFloorCached = false;
        this.RecalcPropertiesIfNeeded();
        a.RecalcPropertiesIfNeeded();
        this.ReCalculateCollide(a, getter);
        a.ReCalculateCollide(this, getter);
        this.ReCalculatePathFind(a, getter);
        a.ReCalculatePathFind(this, getter);
        this.ReCalculateVisionBlocked(a, getter);
        a.ReCalculateVisionBlocked(this, getter);
        this.setBlockedGridPointers(getter);
        a.setBlockedGridPointers(getter);
    }

    void ReCalculateAll(boolean bDoReverse, IsoGridSquare a, GetSquare getter) {
        if (a == null || a == this) {
            return;
        }
        this.solidFloorCached = false;
        a.solidFloorCached = false;
        this.RecalcPropertiesIfNeeded();
        if (bDoReverse) {
            a.RecalcPropertiesIfNeeded();
        }
        this.ReCalculateCollide(a, getter);
        if (bDoReverse) {
            a.ReCalculateCollide(this, getter);
        }
        this.ReCalculatePathFind(a, getter);
        if (bDoReverse) {
            a.ReCalculatePathFind(this, getter);
        }
        this.ReCalculateVisionBlocked(a, getter);
        if (bDoReverse) {
            a.ReCalculateVisionBlocked(this, getter);
        }
        this.setBlockedGridPointers(getter);
        if (bDoReverse) {
            a.setBlockedGridPointers(getter);
        }
    }

    void ReCalculateMineOnly(IsoGridSquare a) {
        this.solidFloorCached = false;
        this.RecalcProperties();
        this.ReCalculateCollide(a);
        this.ReCalculatePathFind(a);
        this.ReCalculateVisionBlocked(a);
        this.setBlockedGridPointers(cellGetSquare);
    }

    public boolean getOpenAir() {
        if (!this.getProperties().has(IsoFlagType.exterior)) {
            return false;
        }
        IsoGridSquare u = this.u;
        int zzz = this.z;
        if (u != null) {
            return u.getOpenAir();
        }
        return IsoCell.getInstance().getGridSquare(this.x, this.y, this.z + 1) == null && this.z >= 0;
    }

    public void RecalcAllWithNeighbours(boolean bDoReverse) {
        this.RecalcAllWithNeighbours(bDoReverse, cellGetSquare);
    }

    public void RecalcAllWithNeighbours(boolean bDoReverse, GetSquare getter) {
        this.solidFloorCached = false;
        this.RecalcPropertiesIfNeeded();
        for (int x = this.getX() - 1; x <= this.getX() + 1; ++x) {
            for (int y = this.getY() - 1; y <= this.getY() + 1; ++y) {
                for (int z = this.getZ() - 1; z <= this.getZ() + 1; ++z) {
                    IsoGridSquare sq;
                    if (!IsoWorld.instance.isValidSquare(x, y, z)) continue;
                    int lx = x - this.getX();
                    int ly = y - this.getY();
                    int lz = z - this.getZ();
                    if (lx == 0 && ly == 0 && lz == 0 || (sq = getter.getGridSquare(x, y, z)) == null) continue;
                    sq.DirtySlice();
                    this.ReCalculateAll(bDoReverse, sq, getter);
                }
            }
        }
        this.doGridNav(getter);
        IsoGridSquare n = this.nav[IsoDirections.N.ordinal()];
        IsoGridSquare s = this.nav[IsoDirections.S.ordinal()];
        IsoGridSquare w = this.nav[IsoDirections.W.ordinal()];
        IsoGridSquare e = this.nav[IsoDirections.E.ordinal()];
        if (n != null && w != null) {
            n.ReCalculateAll(w, getter);
        }
        if (n != null && e != null) {
            n.ReCalculateAll(e, getter);
        }
        if (s != null && w != null) {
            s.ReCalculateAll(w, getter);
        }
        if (s != null && e != null) {
            s.ReCalculateAll(e, getter);
        }
    }

    public void RecalcAllWithNeighboursMineOnly() {
        this.solidFloorCached = false;
        this.RecalcProperties();
        for (int x = this.getX() - 1; x <= this.getX() + 1; ++x) {
            for (int y = this.getY() - 1; y <= this.getY() + 1; ++y) {
                for (int z = this.getZ() - 1; z <= this.getZ() + 1; ++z) {
                    IsoGridSquare sq;
                    if (z < 0) continue;
                    int lx = x - this.getX();
                    int ly = y - this.getY();
                    int lz = z - this.getZ();
                    if (lx == 0 && ly == 0 && lz == 0 || (sq = this.getCell().getGridSquare(x, y, z)) == null) continue;
                    sq.DirtySlice();
                    this.ReCalculateMineOnly(sq);
                }
            }
        }
    }

    boolean IsWindow(int sx, int sy, int sz) {
        IsoGridSquare sq = this.getCell().getGridSquare(this.x + sx, this.y + sy, this.z + sz);
        return this.getWindowTo(sq) != null || this.getWindowThumpableTo(sq) != null;
    }

    void RemoveAllWith(IsoFlagType propertyType) {
        for (int n = 0; n < this.objects.size(); ++n) {
            IsoObject o = (IsoObject)this.objects.get(n);
            if (o.sprite == null || !o.sprite.getProperties().has(propertyType)) continue;
            this.objects.remove((Object)o);
            this.specialObjects.remove(o);
            --n;
        }
        this.RecalcAllWithNeighbours(true);
    }

    public boolean hasSupport() {
        IsoGridSquare s = this.getCell().getGridSquare(this.x, this.y + 1, this.z);
        IsoGridSquare e = this.getCell().getGridSquare(this.x + 1, this.y, this.z);
        for (int n = 0; n < this.objects.size(); ++n) {
            IsoObject o = (IsoObject)this.objects.get(n);
            if (o.sprite == null || !o.sprite.getProperties().has(IsoFlagType.solid) && (!o.sprite.getProperties().has(IsoFlagType.cutW) && !o.sprite.getProperties().has(IsoFlagType.cutN) || o.sprite.properties.has(IsoFlagType.halfheight))) continue;
            return true;
        }
        if (s != null && s.properties.has(IsoFlagType.cutN) && !s.properties.has(IsoFlagType.halfheight)) {
            return true;
        }
        return e != null && e.properties.has(IsoFlagType.cutW) && !s.properties.has(IsoFlagType.halfheight);
    }

    public Integer getID() {
        return this.id;
    }

    public void setID(int id) {
        this.id = id;
    }

    private int savematrix(boolean[][][] pathMatrix, byte[] databytes, int index) {
        for (int x = 0; x <= 2; ++x) {
            for (int y = 0; y <= 2; ++y) {
                for (int z = 0; z <= 2; ++z) {
                    databytes[index] = pathMatrix[x][y][z] ? (byte)1 : 0;
                    ++index;
                }
            }
        }
        return index;
    }

    private int loadmatrix(boolean[][][] pathMatrix, byte[] databytes, int index) {
        for (int x = 0; x <= 2; ++x) {
            for (int y = 0; y <= 2; ++y) {
                for (int z = 0; z <= 2; ++z) {
                    pathMatrix[x][y][z] = databytes[index] != 0;
                    ++index;
                }
            }
        }
        return index;
    }

    private void savematrix(boolean[][][] pathMatrix, ByteBuffer databytes) {
        for (int x = 0; x <= 2; ++x) {
            for (int y = 0; y <= 2; ++y) {
                for (int z = 0; z <= 2; ++z) {
                    databytes.put(pathMatrix[x][y][z] ? (byte)1 : 0);
                }
            }
        }
    }

    private void loadmatrix(boolean[][][] pathMatrix, ByteBuffer databytes) {
        for (int x = 0; x <= 2; ++x) {
            for (int y = 0; y <= 2; ++y) {
                for (int z = 0; z <= 2; ++z) {
                    pathMatrix[x][y][z] = databytes.get() != 0;
                }
            }
        }
    }

    public void DirtySlice() {
    }

    public void setHourSeenToCurrent() {
        this.hourLastSeen = (int)GameTime.instance.getWorldAgeHours();
    }

    public void splatBlood(int dist, float alpha) {
        alpha *= 2.0f;
        if ((alpha *= 3.0f) > 1.0f) {
            alpha = 1.0f;
        }
        IsoGridSquare n = this;
        IsoGridSquare w = this;
        for (int dUse = 0; dUse < dist; ++dUse) {
            int id;
            IsoGridSquare b;
            IsoGridSquare a;
            float offZ;
            int endUse;
            int startUse;
            boolean bDoTwo;
            int range;
            int max;
            int min;
            boolean bRight;
            boolean bLeft;
            if (n != null) {
                n = this.getCell().getGridSquare(PZMath.fastfloor((float)this.getX()), PZMath.fastfloor((float)this.getY()) - dUse, PZMath.fastfloor((float)this.getZ()));
            }
            if (w != null) {
                w = this.getCell().getGridSquare(PZMath.fastfloor((float)this.getX()) - dUse, PZMath.fastfloor((float)this.getY()), PZMath.fastfloor((float)this.getZ()));
            }
            float offX = 0.0f;
            if (w != null && w.testCollideAdjacent(null, -1, 0, 0)) {
                bLeft = false;
                bRight = false;
                min = 0;
                max = 0;
                if (w.getS() != null && w.getS().testCollideAdjacent(null, -1, 0, 0)) {
                    bLeft = true;
                }
                if (w.getN() != null && w.getN().testCollideAdjacent(null, -1, 0, 0)) {
                    bRight = true;
                }
                if (bLeft) {
                    min = -1;
                }
                if (bRight) {
                    max = 1;
                }
                range = max - min;
                bDoTwo = false;
                startUse = 0;
                endUse = 0;
                if (range > 0 && Rand.Next((int)2) == 0) {
                    bDoTwo = true;
                    if (range > 1) {
                        if (Rand.Next((int)2) == 0) {
                            startUse = -1;
                            endUse = 0;
                        } else {
                            startUse = 0;
                            endUse = 1;
                        }
                    } else {
                        startUse = min;
                        endUse = max;
                    }
                }
                offZ = (float)Rand.Next((int)100) / 300.0f;
                a = this.getCell().getGridSquare(w.getX(), w.getY() + startUse, w.getZ());
                b = this.getCell().getGridSquare(w.getX(), w.getY() + endUse, w.getZ());
                if (a == null || b == null || !a.has(IsoFlagType.cutW) || !b.has(IsoFlagType.cutW) || a.getProperties().has(IsoFlagType.WallSE) || b.getProperties().has(IsoFlagType.WallSE) || a.has(IsoFlagType.HoppableW) || b.has(IsoFlagType.HoppableW)) {
                    bDoTwo = false;
                }
                if (bDoTwo) {
                    id = 24 + Rand.Next((int)2) * 2;
                    if (Rand.Next((int)2) == 0) {
                        id += 8;
                    }
                    a.DoSplat("overlay_blood_wall_01_" + (id + 1), false, IsoFlagType.cutW, 0.0f, offZ, alpha);
                    b.DoSplat("overlay_blood_wall_01_" + (id + 0), false, IsoFlagType.cutW, 0.0f, offZ, alpha);
                } else {
                    id = 0;
                    switch (Rand.Next((int)3)) {
                        case 0: {
                            id = 0 + Rand.Next((int)4);
                            break;
                        }
                        case 1: {
                            id = 8 + Rand.Next((int)4);
                            break;
                        }
                        case 2: {
                            id = 16 + Rand.Next((int)4);
                        }
                    }
                    if (id == 17 || id == 19) {
                        offZ = 0.0f;
                    }
                    if (w.has(IsoFlagType.HoppableW)) {
                        w.DoSplat("overlay_blood_fence_01_" + id, false, IsoFlagType.HoppableW, 0.0f, 0.0f, alpha);
                    } else {
                        w.DoSplat("overlay_blood_wall_01_" + id, false, IsoFlagType.cutW, 0.0f, offZ, alpha);
                    }
                }
                w = null;
            }
            if (n == null || !n.testCollideAdjacent(null, 0, -1, 0)) continue;
            bLeft = false;
            bRight = false;
            min = 0;
            max = 0;
            if (n.getW() != null && n.getW().testCollideAdjacent(null, 0, -1, 0)) {
                bLeft = true;
            }
            if (n.getE() != null && n.getE().testCollideAdjacent(null, 0, -1, 0)) {
                bRight = true;
            }
            if (bLeft) {
                min = -1;
            }
            if (bRight) {
                max = 1;
            }
            range = max - min;
            bDoTwo = false;
            startUse = 0;
            endUse = 0;
            if (range > 0 && Rand.Next((int)2) == 0) {
                bDoTwo = true;
                if (range > 1) {
                    if (Rand.Next((int)2) == 0) {
                        startUse = -1;
                        endUse = 0;
                    } else {
                        startUse = 0;
                        endUse = 1;
                    }
                } else {
                    startUse = min;
                    endUse = max;
                }
            }
            offZ = (float)Rand.Next((int)100) / 300.0f;
            a = this.getCell().getGridSquare(n.getX() + startUse, n.getY(), n.getZ());
            b = this.getCell().getGridSquare(n.getX() + endUse, n.getY(), n.getZ());
            if (a == null || b == null || !a.has(IsoFlagType.cutN) || !b.has(IsoFlagType.cutN) || a.getProperties().has(IsoFlagType.WallSE) || b.getProperties().has(IsoFlagType.WallSE) || a.has(IsoFlagType.HoppableN) || b.has(IsoFlagType.HoppableN)) {
                bDoTwo = false;
            }
            if (bDoTwo) {
                id = 28 + Rand.Next((int)2) * 2;
                if (Rand.Next((int)2) == 0) {
                    id += 8;
                }
                a.DoSplat("overlay_blood_wall_01_" + (id + 0), false, IsoFlagType.cutN, 0.0f, offZ, alpha);
                b.DoSplat("overlay_blood_wall_01_" + (id + 1), false, IsoFlagType.cutN, 0.0f, offZ, alpha);
            } else {
                id = 0;
                switch (Rand.Next((int)3)) {
                    case 0: {
                        id = 4 + Rand.Next((int)4);
                        break;
                    }
                    case 1: {
                        id = 12 + Rand.Next((int)4);
                        break;
                    }
                    case 2: {
                        id = 20 + Rand.Next((int)4);
                    }
                }
                if (id == 20 || id == 22) {
                    offZ = 0.0f;
                }
                if (n.has(IsoFlagType.HoppableN)) {
                    n.DoSplat("overlay_blood_fence_01_" + id, false, IsoFlagType.HoppableN, 0.0f, offZ, alpha);
                } else {
                    n.DoSplat("overlay_blood_wall_01_" + id, false, IsoFlagType.cutN, 0.0f, offZ, alpha);
                }
            }
            n = null;
        }
    }

    public boolean haveBlood() {
        int i;
        if (Core.getInstance().getOptionBloodDecals() == 0) {
            return false;
        }
        for (i = 0; i < this.getObjects().size(); ++i) {
            IsoObject obj = (IsoObject)this.getObjects().get(i);
            if (obj.wallBloodSplats == null || obj.wallBloodSplats.isEmpty()) continue;
            return true;
        }
        for (i = 0; i < this.getChunk().floorBloodSplats.size(); ++i) {
            IsoFloorBloodSplat splat = (IsoFloorBloodSplat)this.getChunk().floorBloodSplats.get(i);
            float splatX = splat.x + (float)(this.getChunk().wx * 8);
            float splatY = splat.y + (float)(this.getChunk().wy * 8);
            if (PZMath.fastfloor((float)splatX) - 1 > this.x || PZMath.fastfloor((float)splatX) + 1 < this.x || PZMath.fastfloor((float)splatY) - 1 > this.y || PZMath.fastfloor((float)splatY) + 1 < this.y) continue;
            return true;
        }
        return false;
    }

    public boolean haveBloodWall() {
        if (Core.getInstance().getOptionBloodDecals() == 0) {
            return false;
        }
        for (int i = 0; i < this.getObjects().size(); ++i) {
            IsoObject obj = (IsoObject)this.getObjects().get(i);
            if (obj.wallBloodSplats == null || obj.wallBloodSplats.isEmpty()) continue;
            return true;
        }
        return false;
    }

    public boolean haveBloodFloor() {
        if (Core.getInstance().getOptionBloodDecals() == 0) {
            return false;
        }
        for (int i = 0; i < this.getChunk().floorBloodSplats.size(); ++i) {
            IsoFloorBloodSplat splat = (IsoFloorBloodSplat)this.getChunk().floorBloodSplats.get(i);
            float splatX = splat.x + (float)(this.getChunk().wx * 8);
            float splatY = splat.y + (float)(this.getChunk().wy * 8);
            if ((int)splatX - 1 > this.x || (int)splatX + 1 < this.x || (int)splatY - 1 > this.y || (int)splatY + 1 < this.y) continue;
            return true;
        }
        return false;
    }

    public boolean haveGrime() {
        for (int i = 0; i < this.getObjects().size(); ++i) {
            IsoObject obj = (IsoObject)this.getObjects().get(i);
            if (obj == null || obj.getAttachedAnimSprite() == null) continue;
            ArrayList<IsoSpriteInstance> sprites = obj.getAttachedAnimSprite();
            for (int j = 0; j < sprites.size(); ++j) {
                IsoSpriteInstance sprite = sprites.get(j);
                if (sprite == null || sprite.getParentSprite() == null || sprite.getParentSprite().getName() == null || !sprite.getParentSprite().getName().contains("overlay_grime")) continue;
                return true;
            }
        }
        return false;
    }

    public boolean haveGrimeWall() {
        for (int i = 0; i < this.getObjects().size(); ++i) {
            IsoObject obj = (IsoObject)this.getObjects().get(i);
            if (obj == null || obj.isFloor() || obj.getAttachedAnimSprite() == null) continue;
            ArrayList<IsoSpriteInstance> sprites = obj.getAttachedAnimSprite();
            for (int j = 0; j < sprites.size(); ++j) {
                IsoSpriteInstance sprite = sprites.get(j);
                if (sprite == null || sprite.getParentSprite() == null || sprite.getParentSprite().getName() == null || !sprite.getParentSprite().getName().contains("overlay_grime")) continue;
                return true;
            }
        }
        return false;
    }

    public boolean haveGrimeFloor() {
        for (int i = 0; i < this.getObjects().size(); ++i) {
            IsoObject obj = (IsoObject)this.getObjects().get(i);
            if (obj == null || !obj.isFloor() || obj.getAttachedAnimSprite() == null) continue;
            ArrayList<IsoSpriteInstance> sprites = obj.getAttachedAnimSprite();
            for (int j = 0; j < sprites.size(); ++j) {
                IsoSpriteInstance sprite = sprites.get(j);
                if (sprite == null || sprite.getParentSprite() == null || sprite.getParentSprite().getName() == null || !sprite.getParentSprite().getName().contains("overlay_grime")) continue;
                return true;
            }
        }
        return false;
    }

    public boolean haveGraffiti() {
        for (int i = 0; i < this.getObjects().size(); ++i) {
            IsoObject obj = (IsoObject)this.getObjects().get(i);
            if (obj == null || obj.getAttachedAnimSprite() == null) continue;
            ArrayList<IsoSpriteInstance> sprites = obj.getAttachedAnimSprite();
            for (int j = 0; j < sprites.size(); ++j) {
                IsoSpriteInstance sprite = sprites.get(j);
                if (sprite == null || sprite.getParentSprite() == null || sprite.getParentSprite().getName() == null || !sprite.getParentSprite().getName().contains("overlay_graffiti") && !sprite.getParentSprite().getName().contains("overlay_messages") && !sprite.getParentSprite().getName().contains("constructedobjects_signs_01_4")) continue;
                return true;
            }
        }
        return false;
    }

    public IsoObject getGraffitiObject() {
        IsoObject graff = null;
        for (int i = 0; i < this.getObjects().size(); ++i) {
            IsoObject obj = (IsoObject)this.getObjects().get(i);
            if (obj == null || obj.getAttachedAnimSprite() == null) continue;
            ArrayList<IsoSpriteInstance> sprites = obj.getAttachedAnimSprite();
            for (int j = 0; j < sprites.size(); ++j) {
                IsoSpriteInstance sprite = sprites.get(j);
                if (sprite == null || sprite.getParentSprite() == null || sprite.getParentSprite().getName() == null || !sprite.getParentSprite().getName().contains("overlay_graffiti") && !sprite.getParentSprite().getName().contains("overlay_messages") && !sprite.getParentSprite().getName().contains("constructedobjects_signs_01_4")) continue;
                return obj;
            }
        }
        return graff;
    }

    public boolean haveStains() {
        return this.haveBlood() || this.haveGrime();
    }

    public void removeGrime() {
        while (this.haveGrime()) {
            for (int i = 0; i < this.getObjects().size(); ++i) {
                IsoObject obj = (IsoObject)this.getObjects().get(i);
                if (obj == null || obj.getAttachedAnimSprite() == null) continue;
                boolean clean = false;
                ArrayList<IsoSpriteInstance> sprites = obj.getAttachedAnimSprite();
                for (int j = 0; j < sprites.size(); ++j) {
                    IsoSpriteInstance sprite = sprites.get(j);
                    if (sprite != null && sprite.getParentSprite() != null && sprite.getParentSprite().getName() != null && sprite.getParentSprite().getName().contains("overlay_grime")) {
                        obj.RemoveAttachedAnim(j);
                    }
                    clean = true;
                }
                if (!clean) continue;
                obj.transmitUpdatedSpriteToClients();
            }
        }
    }

    public void removeGraffiti() {
        while (this.haveGraffiti()) {
            for (int i = 0; i < this.getObjects().size(); ++i) {
                IsoObject obj = (IsoObject)this.getObjects().get(i);
                if (obj == null || obj.getAttachedAnimSprite() == null) continue;
                boolean clean = false;
                ArrayList<IsoSpriteInstance> sprites = obj.getAttachedAnimSprite();
                for (int j = 0; j < sprites.size(); ++j) {
                    IsoSpriteInstance sprite = sprites.get(j);
                    if (sprite != null && sprite.getParentSprite() != null && sprite.getParentSprite().getName() != null && (sprite.getParentSprite().getName().contains("overlay_graffiti") || sprite.getParentSprite().getName().contains("overlay_messages") || sprite.getParentSprite().getName().contains("constructedobjects_signs_01_4"))) {
                        obj.RemoveAttachedAnim(j);
                    }
                    clean = true;
                }
                if (!clean) continue;
                obj.transmitUpdatedSpriteToClients();
            }
        }
    }

    public void removeBlood(boolean remote, boolean onlyWall) {
        int i;
        for (i = 0; i < this.getObjects().size(); ++i) {
            IsoObject obj = (IsoObject)this.getObjects().get(i);
            if (obj.wallBloodSplats == null) continue;
            obj.wallBloodSplats.clear();
        }
        if (!onlyWall) {
            for (i = 0; i < this.getChunk().floorBloodSplats.size(); ++i) {
                IsoFloorBloodSplat splat = (IsoFloorBloodSplat)this.getChunk().floorBloodSplats.get(i);
                int splatX = (int)((float)(this.getChunk().wx * 8) + splat.x);
                int splatY = (int)((float)(this.getChunk().wy * 8) + splat.y);
                if (splatX < this.getX() - 1 || splatX > this.getX() + 1 || splatY < this.getY() - 1 || splatY > this.getY() + 1) continue;
                this.getChunk().floorBloodSplats.remove(i);
                --i;
            }
        }
        if (GameServer.server && !remote) {
            INetworkPacket.sendToRelative((PacketTypes.PacketType)PacketTypes.PacketType.RemoveBlood, (float)this.x, (float)this.y, (Object[])new Object[]{this, onlyWall});
        }
        if (PerformanceSettings.fboRenderChunk && Thread.currentThread() == GameWindow.gameThread) {
            this.invalidateRenderChunkLevel(1L);
        }
    }

    public void DoSplat(String id, boolean bFlip, IsoFlagType prop, float offX, float offZ, float alpha) {
        for (int i = 0; i < this.getObjects().size(); ++i) {
            IsoObject obj = (IsoObject)this.getObjects().get(i);
            if (obj.sprite == null || !obj.sprite.getProperties().has(prop) || obj instanceof IsoWindow && obj.isDestroyed()) continue;
            IsoSprite spr = IsoSprite.getSprite((IsoSpriteManager)IsoSpriteManager.instance, (String)id, (int)0);
            if (spr == null) {
                return;
            }
            if (obj.wallBloodSplats == null) {
                obj.wallBloodSplats = new ArrayList();
            }
            IsoWallBloodSplat splat = new IsoWallBloodSplat((float)GameTime.getInstance().getWorldAgeHours(), spr);
            obj.wallBloodSplats.add(splat);
            if (!PerformanceSettings.fboRenderChunk || Thread.currentThread() != GameWindow.gameThread) break;
            this.invalidateRenderChunkLevel(1L);
            break;
        }
    }

    public void ClearTileObjects() {
        this.objects.clear();
        this.RecalcProperties();
    }

    public void ClearTileObjectsExceptFloor() {
        for (int n = 0; n < this.objects.size(); ++n) {
            IsoObject o = (IsoObject)this.objects.get(n);
            if (o.sprite != null && o.sprite.getProperties().has(IsoFlagType.solidfloor)) continue;
            this.objects.remove((Object)o);
            --n;
        }
        this.RecalcProperties();
    }

    public int RemoveTileObject(IsoObject obj) {
        boolean chunkIsLoading = obj.getSquare() == null || !obj.getSquare().getChunk().loaded || obj.getSquare().getChunk().preventHotSave;
        return this.RemoveTileObject(obj, !chunkIsLoading);
    }

    public int RemoveTileObject(IsoObject obj, boolean safelyRemove) {
        if (safelyRemove) {
            return IsoObjectUtils.safelyRemoveTileObjectFromSquare((IsoObject)obj);
        }
        IsoRegions.setPreviousFlags((IsoGridSquare)this);
        int index = this.objects.indexOf((Object)obj);
        if (!this.objects.contains((Object)obj)) {
            index = this.specialObjects.indexOf(obj);
        }
        if (obj != null && this.objects.contains((Object)obj)) {
            IsoObject playerBuiltFloor;
            if (obj.isTableSurface()) {
                for (int i = this.objects.indexOf((Object)obj) + 1; i < this.objects.size(); ++i) {
                    IsoObject object = (IsoObject)this.objects.get(i);
                    if (!object.isTableTopObject() && (!object.isTableSurface() || object.hasProperty(IsoPropertyType.COUNTERTOP))) continue;
                    object.setRenderYOffset(object.getRenderYOffset() - obj.getSurfaceOffset());
                    object.sx = 0.0f;
                    object.sy = 0.0f;
                }
            }
            if (obj == (playerBuiltFloor = this.getPlayerBuiltFloor())) {
                IsoGridOcclusionData.SquareChanged();
            }
            LuaEventManager.triggerEvent((String)"OnObjectAboutToBeRemoved", (Object)obj);
            if (!this.objects.contains((Object)obj)) {
                throw new IllegalArgumentException("OnObjectAboutToBeRemoved not allowed to remove the object");
            }
            index = this.objects.indexOf((Object)obj);
            if (obj instanceof IsoWorldInventoryObject) {
                obj.invalidateRenderChunkLevel(136L);
            } else {
                obj.invalidateRenderChunkLevel(128L);
            }
            obj.removeFromWorld();
            obj.removeFromSquare();
            assert (!this.objects.contains((Object)obj));
            assert (!this.specialObjects.contains(obj));
            if (!(obj instanceof IsoWorldInventoryObject)) {
                this.RecalcAllWithNeighbours(true);
                this.getCell().checkHaveRoof(this.getX(), this.getY());
                for (int pn = 0; pn < IsoPlayer.numPlayers; ++pn) {
                    LosUtil.cachecleared[pn] = true;
                }
                IsoGridSquare.setRecalcLightTime(-1.0f);
                if (PerformanceSettings.fboRenderChunk) {
                    ++Core.dirtyGlobalLightsCount;
                }
                GameTime.instance.lightSourceUpdate = 100.0f;
                this.fixPlacedItemRenderOffsets();
            }
        }
        MapCollisionData.instance.squareChanged(this);
        LuaEventManager.triggerEvent((String)"OnTileRemoved", (Object)obj);
        PolygonalMap2.instance.squareChanged(this);
        IsoRegions.squareChanged((IsoGridSquare)this, (boolean)true);
        return index;
    }

    public int RemoveTileObjectErosionNoRecalc(IsoObject obj) {
        int index = this.objects.indexOf((Object)obj);
        IsoGridSquare sq = obj.square;
        obj.removeFromWorld();
        obj.removeFromSquare();
        sq.RecalcPropertiesIfNeeded();
        assert (!this.objects.contains((Object)obj));
        assert (!this.specialObjects.contains(obj));
        return index;
    }

    public void AddSpecialObject(IsoObject obj) {
        this.AddSpecialObject(obj, -1);
    }

    public void AddSpecialObject(IsoObject obj, int index) {
        if (obj == null) {
            return;
        }
        IsoRegions.setPreviousFlags((IsoGridSquare)this);
        index = this.placeWallAndDoorCheck(obj, index);
        if (index != -1 && index >= 0 && index <= this.objects.size()) {
            this.objects.add(index, (Object)obj);
        } else {
            this.objects.add((Object)obj);
        }
        this.specialObjects.add(obj);
        this.burntOut = false;
        obj.addToWorld();
        if (!GameServer.server && !GameClient.client) {
            this.restackSheetRope();
        }
        this.RecalcAllWithNeighbours(true);
        if (!(obj instanceof IsoWorldInventoryObject)) {
            this.fixPlacedItemRenderOffsets();
            for (int pn = 0; pn < IsoPlayer.numPlayers; ++pn) {
                LosUtil.cachecleared[pn] = true;
            }
            IsoGridSquare.setRecalcLightTime(-1.0f);
            if (PerformanceSettings.fboRenderChunk) {
                ++Core.dirtyGlobalLightsCount;
            }
            GameTime.instance.lightSourceUpdate = 100.0f;
            if (obj == this.getPlayerBuiltFloor()) {
                IsoGridOcclusionData.SquareChanged();
            }
        }
        MapCollisionData.instance.squareChanged(this);
        PolygonalMap2.instance.squareChanged(this);
        IsoRegions.squareChanged((IsoGridSquare)this);
        this.invalidateRenderChunkLevel(64L);
    }

    public void AddTileObject(IsoObject obj) {
        this.AddTileObject(obj, -1);
    }

    public void AddTileObject(IsoObject obj, int index) {
        if (obj == null) {
            return;
        }
        IsoRegions.setPreviousFlags((IsoGridSquare)this);
        index = this.placeWallAndDoorCheck(obj, index);
        if (index != -1 && index >= 0 && index <= this.objects.size()) {
            this.objects.add(index, (Object)obj);
        } else {
            this.objects.add((Object)obj);
        }
        this.burntOut = false;
        obj.addToWorld();
        this.RecalcAllWithNeighbours(true);
        if (!(obj instanceof IsoWorldInventoryObject)) {
            this.fixPlacedItemRenderOffsets();
            for (int pn = 0; pn < IsoPlayer.numPlayers; ++pn) {
                LosUtil.cachecleared[pn] = true;
            }
            IsoGridSquare.setRecalcLightTime(-1.0f);
            if (PerformanceSettings.fboRenderChunk) {
                ++Core.dirtyGlobalLightsCount;
            }
            GameTime.instance.lightSourceUpdate = 100.0f;
            if (obj == this.getPlayerBuiltFloor()) {
                IsoGridOcclusionData.SquareChanged();
            }
        }
        MapCollisionData.instance.squareChanged(this);
        PolygonalMap2.instance.squareChanged(this);
        IsoRegions.squareChanged((IsoGridSquare)this);
        this.invalidateRenderChunkLevel(64L);
    }

    public int placeWallAndDoorCheck(IsoObject obj, int index) {
        int needleIndex = -1;
        if (obj.sprite != null) {
            boolean findDoors;
            IsoObjectType t = obj.sprite.getTileType();
            boolean findWalls = t == IsoObjectType.doorN || t == IsoObjectType.doorW;
            boolean bl = findDoors = !findWalls && (obj.sprite.cutW || obj.sprite.cutN || t == IsoObjectType.doorFrN || t == IsoObjectType.doorFrW || obj.sprite.treatAsWallOrder);
            if (findDoors || findWalls) {
                for (int i = 0; i < this.objects.size(); ++i) {
                    IsoObject other = (IsoObject)this.objects.get(i);
                    if (other.sprite == null) continue;
                    t = other.sprite.getTileType();
                    if (findDoors && (t == IsoObjectType.doorN || t == IsoObjectType.doorW)) {
                        needleIndex = i;
                    }
                    if (!findWalls || t != IsoObjectType.doorFrN && t != IsoObjectType.doorFrW && !other.sprite.cutW && !other.sprite.cutN && !other.sprite.treatAsWallOrder) continue;
                    needleIndex = i;
                }
                if (findWalls && needleIndex > index) {
                    index = needleIndex + 1;
                    return index;
                }
                if (findDoors && needleIndex >= 0 && (needleIndex < index || index < 0)) {
                    index = needleIndex;
                    return index;
                }
            }
        }
        return index;
    }

    public void transmitAddObjectToSquare(IsoObject obj, int index) {
        if (obj == null || this.objects.contains((Object)obj)) {
            return;
        }
        this.AddTileObject(obj, index);
        if (GameServer.server) {
            obj.transmitCompleteItemToClients();
        }
    }

    public int transmitRemoveItemFromSquare(IsoObject obj) {
        return this.transmitRemoveItemFromSquare(obj, true);
    }

    public int transmitRemoveItemFromSquare(IsoObject obj, boolean safelyRemove) {
        if (obj == null || !this.objects.contains((Object)obj)) {
            return -1;
        }
        if (GameClient.client) {
            try {
                GameClient.instance.checkAddedRemovedItems(obj);
            }
            catch (Exception ex) {
                GameClient.connection.cancelPacket();
                ExceptionLogger.logException((Throwable)ex);
            }
            RemoveItemFromSquarePacket packet = new RemoveItemFromSquarePacket();
            packet.set(obj);
            ByteBufferWriter b = GameClient.connection.startPacket();
            PacketTypes.PacketType.RemoveItemFromSquare.doPacket(b);
            packet.write(b);
            PacketTypes.PacketType.RemoveItemFromSquare.send((IConnection)GameClient.connection);
        }
        if (GameServer.server) {
            if (safelyRemove && IsoObjectUtils.isObjectMultiSquare((IsoObject)obj)) {
                ArrayList objects = new ArrayList();
                if (IsoObjectUtils.getAllMultiTileObjects((IsoObject)obj, objects)) {
                    int objectIndex = -1;
                    for (IsoObject object2 : objects) {
                        IsoGridSquare sq = object2.square;
                        if (sq == null) continue;
                        int idx = GameServer.RemoveItemFromMap((IsoObject)object2);
                        if (obj != object2) continue;
                        objectIndex = idx;
                    }
                    return objectIndex;
                }
                return -1;
            }
            return GameServer.RemoveItemFromMap((IsoObject)obj);
        }
        return this.RemoveTileObject(obj, safelyRemove);
    }

    public void transmitRemoveItemFromSquareOnClients(IsoObject obj) {
        if (obj == null || !this.objects.contains((Object)obj)) {
            return;
        }
        if (GameServer.server) {
            GameServer.RemoveItemFromMap((IsoObject)obj);
        }
    }

    public void transmitModdata() {
        if (GameClient.client) {
            ReceiveModDataPacket packet = new ReceiveModDataPacket();
            packet.set(this);
            ByteBufferWriter b = GameClient.connection.startPacket();
            PacketTypes.PacketType.ReceiveModData.doPacket(b);
            packet.write(b);
            PacketTypes.PacketType.ReceiveModData.send((IConnection)GameClient.connection);
        } else if (GameServer.server) {
            GameServer.loadModData((IsoGridSquare)this);
        }
    }

    public void SpawnWorldInventoryItem(String itemType, float x, float y, float height, int nbr) {
        for (int i = 0; i < nbr; ++i) {
            InventoryItem inventoryItem = this.SpawnWorldInventoryItem(itemType, x, y, height);
        }
    }

    public InventoryItem SpawnWorldInventoryItem(String itemType, float x, float y, float height) {
        return this.SpawnWorldInventoryItem(itemType, x, y, height, true);
    }

    public InventoryItem SpawnWorldInventoryItem(String itemType, float x, float y, float height, boolean autoAge) {
        return this.AddWorldInventoryItem(itemType, x, y, height, autoAge, true);
    }

    public void AddWorldInventoryItem(String itemType, float x, float y, float height, int nbr) {
        for (int i = 0; i < nbr; ++i) {
            this.AddWorldInventoryItem(itemType, x, y, height);
        }
    }

    public InventoryItem AddWorldInventoryItem(ItemKey itemKey, float x, float y, float height) {
        return this.AddWorldInventoryItem(itemKey.toString(), x, y, height);
    }

    public InventoryItem AddWorldInventoryItem(String itemType, float x, float y, float height) {
        return this.AddWorldInventoryItem(itemType, x, y, height, true);
    }

    public InventoryItem AddWorldInventoryItem(ItemKey itemKey, float x, float y, float height, boolean autoAge) {
        return this.AddWorldInventoryItem(itemKey.toString(), x, y, height, autoAge);
    }

    public InventoryItem AddWorldInventoryItem(String itemType, float x, float y, float height, boolean autoAge) {
        return this.AddWorldInventoryItem(itemType, x, y, height, autoAge, false);
    }

    public InventoryItem AddWorldInventoryItem(String itemType, float x, float y, float height, boolean autoAge, boolean synchSpawn) {
        InventoryItem item = InventoryItemFactory.CreateItem((String)itemType);
        if (item == null) {
            return null;
        }
        IsoWorldInventoryObject obj = new IsoWorldInventoryObject(item, this, x, y, height);
        if (autoAge) {
            item.setAutoAge();
        }
        item.setWorldItem(obj);
        obj.setKeyId(item.getKeyId());
        obj.setName(item.getName());
        this.objects.add((Object)obj);
        this.worldObjects.add(obj);
        if (obj.getRenderSquare() != null) {
            obj.getRenderSquare().invalidateRenderChunkLevel(68L);
        }
        if (GameServer.server && !synchSpawn) {
            obj.transmitCompleteItemToClients();
        }
        if (synchSpawn) {
            item.SynchSpawn();
        }
        return item;
    }

    public InventoryItem AddWorldInventoryItem(InventoryItem item, float x, float y, float height) {
        return this.AddWorldInventoryItem(item, x, y, height, true);
    }

    public IsoDeadBody createAnimalCorpseFromItem(InventoryItem item) {
        if (item.getFullType().equals("Base.CorpseAnimal")) {
            return item.loadCorpseFromByteData(null);
        }
        return null;
    }

    public InventoryItem SpawnWorldInventoryItem(InventoryItem item, float x, float y, float height, boolean transmit) {
        return this.AddWorldInventoryItem(item, x, y, height, transmit, true);
    }

    public InventoryItem AddWorldInventoryItem(InventoryItem item, float x, float y, float height, boolean transmit) {
        return this.AddWorldInventoryItem(item, x, y, height, transmit, false);
    }

    public InventoryItem AddWorldInventoryItem(InventoryItem item, float x, float y, float height, boolean transmit, boolean synchSpawn) {
        IsoDeadBody corpse = this.tryAddCorpseToWorld(item, x, y);
        if (corpse != null) {
            return item;
        }
        this.invalidateRenderChunkLevel(68L);
        if (item.getFullType().contains(".Generator") || item.hasTag(ItemTag.GENERATOR) && item.getWorldObjectSprite() != null) {
            new IsoGenerator(item, IsoWorld.instance.currentCell, this);
            IsoWorld.instance.currentCell.addToProcessItemsRemove(item);
            return item;
        }
        if (item instanceof AnimalInventoryItem) {
            IsoAnimal prevHolder;
            AnimalInventoryItem animalItem = (AnimalInventoryItem)item;
            IsoAnimal animal = new IsoAnimal(IsoWorld.instance.getCell(), this.x, this.y, this.z, animalItem.getAnimal().getAnimalType(), animalItem.getAnimal().getBreed());
            animal.copyFrom(animalItem.getAnimal());
            short reuseId = animalItem.getAnimal().getOnlineID();
            if (LuaManager.GlobalObject.isMultiplayer() && (prevHolder = AnimalInstanceManager.getInstance().get(reuseId)) != null && prevHolder != animal) {
                prevHolder.removeFromWorld();
                prevHolder.removeFromSquare();
                AnimalInstanceManager.getInstance().remove(prevHolder);
            }
            AnimalInstanceManager.getInstance().add(animal, reuseId);
            animal.addToWorld();
            animal.attachBackToMotherTimer = 10000.0f;
            animal.setSquare(this);
            animal.playBreedSound("put_down");
            AnimalSoundState ass = animal.getAnimalSoundState("voice");
            if (ass != null && animal.getBreed() != null) {
                AnimalBreed.Sound abs = animal.getBreed().getSound("idle");
                if (abs != null) {
                    ass.setIntervalExpireTime(abs.soundName, System.currentTimeMillis() + (long)Rand.Next((int)abs.intervalMin, (int)abs.intervalMax) * 1000L);
                }
                if ((abs = animal.getBreed().getSound("stressed")) != null) {
                    ass.setIntervalExpireTime(abs.soundName, System.currentTimeMillis() + (long)Rand.Next((int)abs.intervalMin, (int)abs.intervalMax) * 1000L);
                }
            }
            if (transmit && GameServer.server) {
                INetworkPacket.sendToRelative((PacketTypes.PacketType)PacketTypes.PacketType.AnimalCommand, (float)this.getX(), (float)this.getY(), (Object[])new Object[]{AnimalCommandPacket.Type.DropAnimal, animal, this});
            }
            if (synchSpawn) {
                item.SynchSpawn();
            }
            this.getCell().addToProcessItemsRemove((InventoryItem)animalItem);
            return animalItem;
        }
        IsoWorldInventoryObject obj = new IsoWorldInventoryObject(item, this, x, y, height);
        obj.setName(item.getName());
        obj.setKeyId(item.getKeyId());
        this.objects.add((Object)obj);
        this.worldObjects.add(obj);
        item.setWorldItem(obj);
        obj.addToWorld();
        DesignationZoneAnimal.addItemOnGround((IsoWorldInventoryObject)obj, (IsoGridSquare)this);
        if (obj.getRenderSquare() != null) {
            obj.getRenderSquare().invalidateRenderChunkLevel(68L);
        }
        if (transmit && GameServer.server) {
            obj.transmitCompleteItemToClients();
        }
        return item;
    }

    public IsoDeadBody tryAddCorpseToWorld(InventoryItem item, float x, float y) {
        return this.tryAddCorpseToWorld(item, x, y, true);
    }

    public @Nullable IsoDeadBody tryAddCorpseToWorld(InventoryItem item, float x, float y, boolean isVisible) {
        if (!item.isHumanCorpse() && !item.isAnimalCorpse()) {
            return null;
        }
        IsoDeadBody dead = item.loadCorpseFromByteData(null);
        dead.setX((float)this.x + x);
        dead.setY((float)this.y + y);
        dead.setZ(this.getApparentZ(x, y));
        dead.setSquare(this);
        dead.setCurrent(this);
        dead.setDoRender(isVisible);
        this.addCorpse(dead, false);
        if (GameServer.server) {
            GameServer.sendCorpse((IsoDeadBody)dead);
        }
        IsoWorld.instance.currentCell.addToProcessItemsRemove(item);
        return dead;
    }

    public void restackSheetRope() {
        if (!(this.has(IsoFlagType.climbSheetW) || this.has(IsoFlagType.climbSheetN) || this.has(IsoFlagType.climbSheetE) || this.has(IsoFlagType.climbSheetS))) {
            return;
        }
        for (int i = 0; i < this.getObjects().size() - 1; ++i) {
            IsoObject sheetRope = (IsoObject)this.getObjects().get(i);
            if (sheetRope.getProperties() == null || !sheetRope.getProperties().has(IsoFlagType.climbSheetW) && !sheetRope.getProperties().has(IsoFlagType.climbSheetN) && !sheetRope.getProperties().has(IsoFlagType.climbSheetE) && !sheetRope.getProperties().has(IsoFlagType.climbSheetS)) continue;
            if (GameServer.server) {
                this.transmitRemoveItemFromSquare(sheetRope);
                this.objects.add((Object)sheetRope);
                sheetRope.transmitCompleteItemToClients();
                break;
            }
            if (GameClient.client) break;
            this.objects.remove((Object)sheetRope);
            this.objects.add((Object)sheetRope);
            break;
        }
    }

    public void Burn() {
        if ((GameServer.server || GameClient.client) && ServerOptions.instance.noFire.getValue()) {
            return;
        }
        if (this.getCell() == null) {
            return;
        }
        this.BurnWalls(true, true);
        LuaEventManager.triggerEvent((String)"OnGridBurnt", (Object)this);
    }

    public void Burn(boolean explode) {
        if ((GameServer.server || GameClient.client) && ServerOptions.instance.noFire.getValue()) {
            return;
        }
        if (this.getCell() == null) {
            return;
        }
        this.BurnWalls(explode, true);
    }

    public void BurnWalls(boolean explode, boolean recursive) {
        if (GameClient.client) {
            return;
        }
        if (GameServer.server && SafeHouse.isSafeHouse((IsoGridSquare)this, null, (boolean)false) != null) {
            if (ServerOptions.instance.noFire.getValue()) {
                return;
            }
            if (!ServerOptions.instance.safehouseAllowFire.getValue()) {
                return;
            }
        }
        for (int i = 0; i < this.specialObjects.size(); ++i) {
            IsoWindowFrame windowFrame;
            IsoObject obj = this.specialObjects.get(i);
            if (obj instanceof IsoThumpable && obj.haveSheetRope()) {
                obj.removeSheetRope(null);
            }
            if (obj instanceof IsoWindow) {
                IsoWindow isoWindow = (IsoWindow)obj;
                if (obj.haveSheetRope()) {
                    obj.removeSheetRope(null);
                }
                isoWindow.removeSheet(null);
            }
            if (obj instanceof IsoWindowFrame && (windowFrame = (IsoWindowFrame)obj).haveSheetRope()) {
                windowFrame.removeSheetRope(null);
            }
            if (!(obj instanceof BarricadeAble)) continue;
            BarricadeAble barricadeAble = (BarricadeAble)obj;
            IsoBarricade barricade1 = barricadeAble.getBarricadeOnSameSquare();
            IsoBarricade barricade2 = barricadeAble.getBarricadeOnOppositeSquare();
            if (barricade1 != null) {
                if (GameServer.server) {
                    GameServer.RemoveItemFromMap((IsoObject)barricade1);
                } else {
                    this.RemoveTileObject((IsoObject)barricade1);
                }
            }
            if (barricade2 == null) continue;
            if (GameServer.server) {
                GameServer.RemoveItemFromMap((IsoObject)barricade2);
                continue;
            }
            barricade2.getSquare().RemoveTileObject((IsoObject)barricade2);
        }
        boolean removedTileObject = false;
        if (!this.getProperties().has(IsoFlagType.burntOut)) {
            if (recursive) {
                for (int n = 0; n < this.objects.size(); ++n) {
                    ((IsoObject)this.objects.get(n)).handleBurning();
                }
            }
            int power = 0;
            for (int n = 0; n < this.objects.size(); ++n) {
                IsoObject obj = (IsoObject)this.objects.get(n);
                boolean replaceIt = false;
                if (recursive) {
                    IsoObjectUtils.safelyBurnTileObjectFromSquare((IsoObject)obj);
                }
                if (obj.getSprite() == null || obj.getSprite().getName() == null || obj.getSprite().getProperties().has(IsoFlagType.water) || obj.getSprite().getName().contains("_burnt_") || obj.getSprite().firerequirement >= this.FIRE_IMMUNE_THRESHOLD || IsoCell.isBasementWallAdjacentToTheVoid_North(obj) || IsoCell.isBasementWallAdjacentToTheVoid_West(obj)) continue;
                if (obj instanceof IsoThumpable && obj.getSprite().burntTile != null) {
                    IsoObject replace = IsoObject.getNew();
                    replace.setSprite(IsoSpriteManager.instance.getSprite(obj.getSprite().burntTile));
                    replace.setSquare(this);
                    if (GameServer.server) {
                        obj.sendObjectChange(IsoObjectChange.REPLACE_WITH, "object", replace);
                    }
                    obj.removeFromWorld();
                    this.objects.set(n, (Object)replace);
                    continue;
                }
                if (obj.getSprite().burntTile != null || obj.getType() == IsoObjectType.tree) {
                    obj.sprite = obj.getSprite().burntTile != null ? IsoSpriteManager.instance.getSprite(obj.getSprite().burntTile) : IsoSpriteManager.instance.getSprite("fencing_burnt_01_" + (Rand.Next((int)15, (int)19) + 1));
                    obj.RemoveAttachedAnims();
                    if (obj.children != null) {
                        obj.children.clear();
                    }
                    obj.transmitUpdatedSpriteToClients();
                    obj.setOverlaySprite(null);
                    continue;
                }
                if (obj instanceof IsoTrap) continue;
                if (obj instanceof IsoBarricade || obj instanceof IsoMannequin) {
                    if (GameServer.server) {
                        GameServer.RemoveItemFromMap((IsoObject)obj);
                    } else {
                        this.objects.remove((Object)obj);
                    }
                    --n;
                    continue;
                }
                if (obj instanceof IsoGenerator) {
                    IsoGenerator generator = (IsoGenerator)obj;
                    if (generator.getFuel() > 0.0f) {
                        power += 20;
                    }
                    if (generator.isActivated()) {
                        generator.activated = false;
                        generator.setSurroundingElectricity();
                        if (GameServer.server) {
                            generator.sync();
                        }
                    }
                    if (GameServer.server) {
                        GameServer.RemoveItemFromMap((IsoObject)obj);
                    } else {
                        this.RemoveTileObject(obj);
                    }
                    --n;
                    continue;
                }
                if ("Campfire".equalsIgnoreCase(obj.getName())) continue;
                if (!(obj.getType() != IsoObjectType.wall || obj.getProperties().has(IsoFlagType.DoorWallW) || obj.getProperties().has(IsoFlagType.DoorWallN) || obj.getProperties().has("WindowN") || obj.getProperties().has(IsoFlagType.WindowW) || obj.getSprite().getName().startsWith("walls_exterior_roofs_") || obj.getSprite().getName().startsWith("fencing_") || obj.getSprite().getName().startsWith("fixtures_railings_"))) {
                    if (obj.getSprite().getProperties().has(IsoFlagType.collideW) && !obj.getSprite().getProperties().has(IsoFlagType.collideN)) {
                        obj.sprite = IsoSpriteManager.instance.getSprite("walls_burnt_01_" + (Rand.Next((int)2) == 0 ? "0" : "4"));
                    } else if (obj.getSprite().getProperties().has(IsoFlagType.collideN) && !obj.getSprite().getProperties().has(IsoFlagType.collideW)) {
                        obj.sprite = IsoSpriteManager.instance.getSprite("walls_burnt_01_" + (Rand.Next((int)2) == 0 ? "1" : "5"));
                    } else if (obj.getSprite().getProperties().has(IsoFlagType.collideW) && obj.getSprite().getProperties().has(IsoFlagType.collideN)) {
                        obj.sprite = IsoSpriteManager.instance.getSprite("walls_burnt_01_" + (Rand.Next((int)2) == 0 ? "2" : "6"));
                    } else if (obj.isWallSE()) {
                        obj.sprite = IsoSpriteManager.instance.getSprite("walls_burnt_01_" + (Rand.Next((int)2) == 0 ? "3" : "7"));
                    }
                } else {
                    if (obj instanceof IsoDoor || obj instanceof IsoWindow || obj instanceof IsoCurtain) {
                        if (GameServer.server) {
                            GameServer.RemoveItemFromMap((IsoObject)obj);
                        } else {
                            this.RemoveTileObject(obj);
                            removedTileObject = true;
                        }
                        --n;
                        continue;
                    }
                    if (obj.getProperties().has(IsoFlagType.WindowW)) {
                        obj.sprite = IsoSpriteManager.instance.getSprite("walls_burnt_01_" + (Rand.Next((int)2) == 0 ? "8" : "12"));
                    } else if (obj.getProperties().has("WindowN")) {
                        obj.sprite = IsoSpriteManager.instance.getSprite("walls_burnt_01_" + (Rand.Next((int)2) == 0 ? "9" : "13"));
                    } else if (obj.getProperties().has(IsoFlagType.DoorWallW)) {
                        obj.sprite = IsoSpriteManager.instance.getSprite("walls_burnt_01_" + (Rand.Next((int)2) == 0 ? "10" : "14"));
                    } else if (obj.getProperties().has(IsoFlagType.DoorWallN)) {
                        obj.sprite = IsoSpriteManager.instance.getSprite("walls_burnt_01_" + (Rand.Next((int)2) == 0 ? "11" : "15"));
                    } else if (obj.getSprite().getProperties().has(IsoFlagType.solidfloor) && !obj.getSprite().getProperties().has(IsoFlagType.exterior)) {
                        obj.sprite = IsoSpriteManager.instance.getSprite("floors_burnt_01_0");
                    } else {
                        if (obj instanceof IsoWaveSignal) {
                            if (GameServer.server) {
                                GameServer.RemoveItemFromMap((IsoObject)obj);
                            } else {
                                this.RemoveTileObject(obj);
                                removedTileObject = true;
                            }
                            --n;
                            continue;
                        }
                        if (obj.getContainer() != null && obj.getContainer().getItems() != null) {
                            int i;
                            for (i = 0; i < obj.getContainer().getItems().size(); ++i) {
                                InventoryItem item = obj.getContainer().getItems().get(i);
                                if ((!(item instanceof Food) || !item.isAlcoholic()) && !item.getType().equals("PetrolCan") && !item.getType().equals("Bleach") || (power += 20) <= 100) continue;
                                power = 100;
                                break;
                            }
                            obj.sprite = IsoSpriteManager.instance.getSprite("floors_burnt_01_" + Rand.Next((int)1, (int)2));
                            for (i = 0; i < obj.getContainerCount(); ++i) {
                                ItemContainer container = obj.getContainerByIndex(i);
                                container.removeItemsFromProcessItems();
                                container.removeAllItems();
                            }
                            obj.removeAllContainers();
                            if (obj.getOverlaySprite() != null) {
                                obj.setOverlaySprite(null);
                            }
                            replaceIt = true;
                        } else if (obj.getSprite().getProperties().has(IsoFlagType.solidtrans) || obj.getSprite().getProperties().has(IsoFlagType.bed) || obj.getSprite().getProperties().has(IsoFlagType.waterPiped)) {
                            obj.sprite = IsoSpriteManager.instance.getSprite("floors_burnt_01_" + Rand.Next((int)1, (int)2));
                            if (obj.getOverlaySprite() != null) {
                                obj.setOverlaySprite(null);
                            }
                        } else if (obj.getSprite().getName().startsWith("walls_exterior_roofs_")) {
                            obj.sprite = IsoSpriteManager.instance.getSprite("walls_burnt_roofs_01_" + obj.getSprite().getName().substring(obj.getSprite().getName().lastIndexOf("_") + 1));
                        } else if (!obj.getSprite().getName().startsWith("roofs_accents")) {
                            if (obj.getSprite().getName().startsWith("roofs_")) {
                                obj.sprite = IsoSpriteManager.instance.getSprite("roofs_burnt_01_" + obj.getSprite().getName().substring(obj.getSprite().getName().lastIndexOf("_") + 1));
                            } else if ((obj.getSprite().getName().startsWith("fencing_") || obj.getSprite().getName().startsWith("fixtures_railings_")) && (obj.getSprite().getProperties().has(IsoFlagType.HoppableN) || obj.getSprite().getProperties().has(IsoFlagType.HoppableW))) {
                                obj.sprite = obj.getSprite().getProperties().has(IsoFlagType.transparentW) && !obj.getSprite().getProperties().has(IsoFlagType.transparentN) ? IsoSpriteManager.instance.getSprite("fencing_burnt_01_0") : (obj.getSprite().getProperties().has(IsoFlagType.transparentN) && !obj.getSprite().getProperties().has(IsoFlagType.transparentW) ? IsoSpriteManager.instance.getSprite("fencing_burnt_01_1") : IsoSpriteManager.instance.getSprite("fencing_burnt_01_2"));
                            } else {
                                if (obj.getSprite().getProperties().has(IsoFlagType.attachedN) || obj.getSprite().getProperties().has(IsoFlagType.attachedW) || obj.getSprite().getProperties().has(IsoFlagType.attachedS) || obj.getSprite().getProperties().has(IsoFlagType.attachedE)) {
                                    if (GameServer.server) {
                                        GameServer.RemoveItemFromMap((IsoObject)obj);
                                    } else {
                                        this.RemoveTileObject(obj);
                                        removedTileObject = true;
                                    }
                                    --n;
                                    continue;
                                }
                                if (obj instanceof IsoThumpable || IsoObjectUtils.isObjectMultiSquare((IsoObject)obj)) {
                                    obj.sprite = IsoSpriteManager.instance.getSprite("floors_burnt_01_" + Rand.Next((int)1, (int)2));
                                    if (obj.getOverlaySprite() != null) {
                                        obj.setOverlaySprite(null);
                                    }
                                }
                            }
                        }
                    }
                }
                if (replaceIt || obj instanceof IsoThumpable) {
                    IsoObject replace = IsoObject.getNew();
                    replace.setSprite(obj.getSprite());
                    replace.setSquare(this);
                    if (GameServer.server) {
                        obj.sendObjectChange(IsoObjectChange.REPLACE_WITH, "object", replace);
                    }
                    obj.removeFromWorld();
                    this.objects.set(n, (Object)replace);
                } else {
                    obj.RemoveAttachedAnims();
                    obj.transmitUpdatedSpriteToClients();
                    obj.setOverlaySprite(null);
                }
                if (obj.emitter == null) continue;
                obj.emitter.stopAll();
                obj.emitter = null;
            }
            if (power > 0 && explode) {
                if (GameServer.server) {
                    GameServer.PlayWorldSoundServer((String)"BurnedObjectExploded", (boolean)false, (IsoGridSquare)this, (float)0.0f, (float)50.0f, (float)1.0f, (boolean)false);
                } else {
                    SoundManager.instance.PlayWorldSound("BurnedObjectExploded", this, 0.0f, 50.0f, 1.0f, false);
                }
                IsoFireManager.explode((IsoCell)this.getCell(), (IsoGridSquare)this, (int)power);
            }
        }
        for (int i = 0; i < this.specialObjects.size(); ++i) {
            IsoObject object = this.specialObjects.get(i);
            if (this.objects.contains((Object)object)) continue;
            this.specialObjects.remove(i);
            --i;
        }
        if (!removedTileObject) {
            this.RecalcProperties();
        }
        this.getProperties().set(IsoFlagType.burntOut);
        this.burntOut = true;
        MapCollisionData.instance.squareChanged(this);
        PolygonalMap2.instance.squareChanged(this);
        this.invalidateRenderChunkLevel(384L);
    }

    public void BurnWallsTCOnly() {
        for (int n = 0; n < this.objects.size(); ++n) {
            IsoObject obj = (IsoObject)this.objects.get(n);
            if (obj.sprite != null) continue;
        }
    }

    public void BurnTick() {
        if (GameClient.client) {
            return;
        }
        for (int i = 0; i < this.staticMovingObjects.size(); ++i) {
            IsoMovingObject mov = this.staticMovingObjects.get(i);
            if (!(mov instanceof IsoDeadBody)) continue;
            IsoDeadBody isoDeadBody = (IsoDeadBody)mov;
            isoDeadBody.Burn();
            if (this.staticMovingObjects.contains(mov)) continue;
            --i;
        }
    }

    public boolean CalculateCollide(IsoGridSquare gridSquare, boolean bVision, boolean bPathfind, boolean bIgnoreSolidTrans) {
        return this.CalculateCollide(gridSquare, bVision, bPathfind, bIgnoreSolidTrans, false);
    }

    public boolean CalculateCollide(IsoGridSquare gridSquare, boolean bVision, boolean bPathfind, boolean bIgnoreSolidTrans, boolean bIgnoreSolid) {
        return this.CalculateCollide(gridSquare, bVision, bPathfind, bIgnoreSolidTrans, bIgnoreSolid, cellGetSquare);
    }

    public boolean CalculateCollide(IsoGridSquare gridSquare, boolean bVision, boolean bPathfind, boolean bIgnoreSolidTrans, boolean bIgnoreSolid, GetSquare getter) {
        boolean diag;
        boolean colE;
        boolean isTopStairFloorTransition;
        if (gridSquare == null && bPathfind) {
            return true;
        }
        if (gridSquare == null) {
            return false;
        }
        if (this.properties.has(IsoFlagType.water) && !this.hasFloorOverWater() || gridSquare.properties.has(IsoFlagType.water) && !gridSquare.hasFloorOverWater()) {
            return true;
        }
        if (!bVision || gridSquare.properties.has(IsoFlagType.trans)) {
            // empty if block
        }
        boolean testW = false;
        boolean testE = false;
        boolean testN = false;
        boolean testS = false;
        if (gridSquare.x < this.x) {
            testW = true;
        }
        if (gridSquare.y < this.y) {
            testN = true;
        }
        if (gridSquare.x > this.x) {
            testE = true;
        }
        if (gridSquare.y > this.y) {
            testS = true;
        }
        if (!bIgnoreSolid && gridSquare.properties.has(IsoFlagType.solid)) {
            if (this.has(IsoObjectType.stairsTW) && !bPathfind && gridSquare.x < this.x && gridSquare.y == this.y && gridSquare.z == this.z) {
                return false;
            }
            return !this.has(IsoObjectType.stairsTN) || bPathfind || gridSquare.x != this.x || gridSquare.y >= this.y || gridSquare.z != this.z;
        }
        if (!bIgnoreSolidTrans && gridSquare.properties.has(IsoFlagType.solidtrans)) {
            IsoGridSquare e;
            IsoGridSquare s;
            IsoGridSquare e2;
            IsoGridSquare s2;
            if (this.has(IsoObjectType.stairsTW) && !bPathfind && gridSquare.x < this.x && gridSquare.y == this.y && gridSquare.z == this.z) {
                return false;
            }
            if (this.has(IsoObjectType.stairsTN) && !bPathfind && gridSquare.x == this.x && gridSquare.y < this.y && gridSquare.z == this.z) {
                return false;
            }
            boolean hasHoppable = false;
            if (gridSquare.properties.has(IsoFlagType.HoppableN) || gridSquare.properties.has(IsoFlagType.HoppableW)) {
                hasHoppable = true;
            }
            if (!hasHoppable && (s2 = getter.getGridSquare(gridSquare.x, gridSquare.y + 1, this.z)) != null && (s2.has(IsoFlagType.HoppableN) || s2.has(IsoFlagType.HoppableW))) {
                hasHoppable = true;
            }
            if (!hasHoppable && (e2 = getter.getGridSquare(gridSquare.x + 1, gridSquare.y, this.z)) != null && (e2.has(IsoFlagType.HoppableN) || e2.has(IsoFlagType.HoppableW))) {
                hasHoppable = true;
            }
            boolean hasWindow = false;
            if (gridSquare.properties.has(IsoFlagType.windowW) || gridSquare.properties.has(IsoFlagType.windowN)) {
                hasWindow = true;
            }
            if (!hasWindow && (gridSquare.properties.has(IsoFlagType.WindowW) || gridSquare.properties.has(IsoFlagType.WindowN))) {
                hasWindow = true;
            }
            if (!hasWindow && (s = getter.getGridSquare(gridSquare.x, gridSquare.y + 1, this.z)) != null && (s.has(IsoFlagType.windowN) || s.has(IsoFlagType.WindowN))) {
                hasWindow = true;
            }
            if (!hasWindow && (e = getter.getGridSquare(gridSquare.x + 1, gridSquare.y, this.z)) != null && (e.has(IsoFlagType.windowW) || e.has(IsoFlagType.WindowW))) {
                hasWindow = true;
            }
            if (!hasWindow && !hasHoppable) {
                return true;
            }
        }
        if (gridSquare.x != this.x && gridSquare.y != this.y && this.z != gridSquare.z && bPathfind) {
            return true;
        }
        if (bPathfind && gridSquare.z < this.z && !(!this.solidFloorCached ? this.TreatAsSolidFloor() : this.solidFloor)) {
            return gridSquare.has(IsoObjectType.stairsTN) || gridSquare.has(IsoObjectType.stairsTW);
        }
        if (bPathfind && gridSquare.z == this.z) {
            if (gridSquare.x > this.x && gridSquare.y == this.y && gridSquare.properties.has(IsoFlagType.windowW)) {
                return false;
            }
            if (gridSquare.y > this.y && gridSquare.x == this.x && gridSquare.properties.has(IsoFlagType.windowN)) {
                return false;
            }
            if (gridSquare.x < this.x && gridSquare.y == this.y && this.properties.has(IsoFlagType.windowW)) {
                return false;
            }
            if (gridSquare.y < this.y && gridSquare.x == this.x && this.properties.has(IsoFlagType.windowN)) {
                return false;
            }
        }
        if (gridSquare.x > this.x && gridSquare.z < this.z && gridSquare.has(IsoObjectType.stairsTW)) {
            return false;
        }
        if (gridSquare.y > this.y && gridSquare.z < this.z && gridSquare.has(IsoObjectType.stairsTN)) {
            return false;
        }
        IsoGridSquare belowTarg = getter.getGridSquare(gridSquare.x, gridSquare.y, gridSquare.z - 1);
        IsoObjectType currentStairs = this.getStairs();
        IsoObjectType targetStairs = gridSquare.getStairs();
        IsoObjectType leftStairs = this.x < gridSquare.x ? currentStairs : targetStairs;
        IsoObjectType rightStairs = this.x > gridSquare.x ? currentStairs : targetStairs;
        IsoObjectType upStairs = this.y < gridSquare.y ? currentStairs : targetStairs;
        IsoObjectType downStairs = this.y > gridSquare.y ? currentStairs : targetStairs;
        IsoGridSquare leftSquare = this.x < gridSquare.x ? this : gridSquare;
        IsoGridSquare rightSquare = this.x > gridSquare.x ? this : gridSquare;
        IsoGridSquare upSquare = this.y < gridSquare.y ? this : gridSquare;
        IsoGridSquare downSquare = this.y > gridSquare.y ? this : gridSquare;
        boolean hasCurrentStairs = currentStairs != IsoObjectType.MAX;
        boolean hasTargetStairs = targetStairs != IsoObjectType.MAX;
        boolean isSameLevel = gridSquare.z == this.z;
        boolean isDiagonal = gridSquare.x != this.x && gridSquare.y != this.y;
        boolean isAtTopStair = currentStairs == IsoObjectType.stairsTN || currentStairs == IsoObjectType.stairsTW;
        boolean isTargetingTopStair = targetStairs == IsoObjectType.stairsTN || targetStairs == IsoObjectType.stairsTW;
        boolean bl = isTopStairFloorTransition = isAtTopStair && !hasTargetStairs || !hasCurrentStairs && isTargetingTopStair;
        if (isSameLevel && this.isWallTo(gridSquare) && !isTopStairFloorTransition) {
            return true;
        }
        if (hasCurrentStairs && hasTargetStairs && isSameLevel && targetStairs == currentStairs) {
            boolean isAdjacent;
            switch (1.$SwitchMap$zombie$iso$SpriteDetails$IsoObjectType[currentStairs.ordinal()]) {
                case 1: 
                case 2: 
                case 3: {
                    boolean bl2 = this.isHorizontalNeighbor(this.x, this.y, gridSquare.x, gridSquare.y);
                    break;
                }
                case 4: 
                case 5: 
                case 6: {
                    boolean bl2 = this.isVerticalNeighbor(this.x, this.y, gridSquare.x, gridSquare.y);
                    break;
                }
                default: {
                    boolean bl2 = isAdjacent = false;
                }
            }
            if (isAdjacent) {
                return false;
            }
        }
        if (isSameLevel && isDiagonal && hasCurrentStairs && hasTargetStairs) {
            if (this.isNorthFacingSequence(upStairs, downStairs)) {
                return false;
            }
            if (this.isWestFacingSequence(leftStairs, rightStairs)) {
                return false;
            }
        }
        if (isDiagonal && (hasCurrentStairs || hasTargetStairs)) {
            if (isSameLevel && this.hasConnectingBNStair(upStairs, leftStairs, upSquare)) {
                return false;
            }
            if (isSameLevel && this.hasConnectingBWStair(rightStairs, leftStairs, rightSquare)) {
                return false;
            }
            if (this.hasConnectingTNStair(downStairs, leftStairs, downSquare, upSquare)) {
                return false;
            }
            if (this.hasConnectingTWStair(leftStairs, upStairs, leftSquare, upSquare)) {
                return false;
            }
        }
        if (gridSquare.x != this.x && gridSquare.z == this.z && gridSquare.has(IsoObjectType.stairsTN) && (belowTarg == null || !belowTarg.has(IsoObjectType.stairsTN) || bPathfind)) {
            return true;
        }
        if (gridSquare.y > this.y && gridSquare.x == this.x && gridSquare.z == this.z && gridSquare.has(IsoObjectType.stairsTN) && (belowTarg == null || !belowTarg.has(IsoObjectType.stairsTN) || bPathfind)) {
            return true;
        }
        if (gridSquare.x > this.x && gridSquare.y == this.y && gridSquare.z == this.z && gridSquare.has(IsoObjectType.stairsTW) && (belowTarg == null || !belowTarg.has(IsoObjectType.stairsTW) || bPathfind)) {
            return true;
        }
        if (gridSquare.y != this.y && gridSquare.z == this.z && gridSquare.has(IsoObjectType.stairsTW) && (belowTarg == null || !belowTarg.has(IsoObjectType.stairsTW) || bPathfind)) {
            return true;
        }
        if (gridSquare.x != this.x && gridSquare.z == this.z && gridSquare.has(IsoObjectType.stairsMN)) {
            return true;
        }
        if (gridSquare.y != this.y && gridSquare.z == this.z && gridSquare.has(IsoObjectType.stairsMW)) {
            return true;
        }
        if (gridSquare.x != this.x && gridSquare.z == this.z && gridSquare.has(IsoObjectType.stairsBN)) {
            return true;
        }
        if (gridSquare.y != this.y && gridSquare.z == this.z && gridSquare.has(IsoObjectType.stairsBW)) {
            return true;
        }
        if (gridSquare.x != this.x && gridSquare.z == this.z && this.has(IsoObjectType.stairsTN)) {
            return true;
        }
        if (gridSquare.y != this.y && gridSquare.z == this.z && this.has(IsoObjectType.stairsTW)) {
            return true;
        }
        if (gridSquare.x != this.x && gridSquare.z == this.z && this.has(IsoObjectType.stairsMN)) {
            return true;
        }
        if (gridSquare.y != this.y && gridSquare.z == this.z && this.has(IsoObjectType.stairsMW)) {
            return true;
        }
        if (gridSquare.x != this.x && gridSquare.z == this.z && this.has(IsoObjectType.stairsBN)) {
            return true;
        }
        if (gridSquare.y != this.y && gridSquare.z == this.z && this.has(IsoObjectType.stairsBW)) {
            return true;
        }
        if (gridSquare.y < this.y && gridSquare.x == this.x && gridSquare.z > this.z && this.has(IsoObjectType.stairsTN)) {
            return false;
        }
        if (gridSquare.x < this.x && gridSquare.y == this.y && gridSquare.z > this.z && this.has(IsoObjectType.stairsTW)) {
            return false;
        }
        if (gridSquare.y > this.y && gridSquare.x == this.x && gridSquare.z < this.z && gridSquare.has(IsoObjectType.stairsTN)) {
            return false;
        }
        if (gridSquare.x > this.x && gridSquare.y == this.y && gridSquare.z < this.z && gridSquare.has(IsoObjectType.stairsTW)) {
            return false;
        }
        if (gridSquare.z == this.z) {
            IsoGridSquare square1 = this;
            IsoGridSquare square2 = gridSquare;
            if (square2.x == square1.x && square2.y == square1.y - 1 && !square1.hasSlopedSurfaceToLevelAbove(IsoDirections.N) && (square1.isSlopedSurfaceEdgeBlocked(IsoDirections.N) || square2.isSlopedSurfaceEdgeBlocked(IsoDirections.S))) {
                return true;
            }
            if (square2.x == square1.x && square2.y == square1.y + 1 && !square1.hasSlopedSurfaceToLevelAbove(IsoDirections.S) && (square1.isSlopedSurfaceEdgeBlocked(IsoDirections.S) || square2.isSlopedSurfaceEdgeBlocked(IsoDirections.N))) {
                return true;
            }
            if (square2.x == square1.x - 1 && square2.y == square1.y && !square1.hasSlopedSurfaceToLevelAbove(IsoDirections.W) && (square1.isSlopedSurfaceEdgeBlocked(IsoDirections.W) || square2.isSlopedSurfaceEdgeBlocked(IsoDirections.E))) {
                return true;
            }
            if (square2.x == square1.x + 1 && square2.y == square1.y && !square1.hasSlopedSurfaceToLevelAbove(IsoDirections.E) && (square1.isSlopedSurfaceEdgeBlocked(IsoDirections.E) || square2.isSlopedSurfaceEdgeBlocked(IsoDirections.W))) {
                return true;
            }
        }
        if (gridSquare.z > this.z) {
            if (gridSquare.y < this.y && gridSquare.x == this.x && this.hasSlopedSurfaceToLevelAbove(IsoDirections.N)) {
                return false;
            }
            if (gridSquare.y > this.y && gridSquare.x == this.x && this.hasSlopedSurfaceToLevelAbove(IsoDirections.S)) {
                return false;
            }
            if (gridSquare.x < this.x && gridSquare.y == this.y && this.hasSlopedSurfaceToLevelAbove(IsoDirections.W)) {
                return false;
            }
            if (gridSquare.x > this.x && gridSquare.y == this.y && this.hasSlopedSurfaceToLevelAbove(IsoDirections.E)) {
                return false;
            }
        }
        if (gridSquare.z < this.z) {
            if (gridSquare.y > this.y && gridSquare.x == this.x && gridSquare.hasSlopedSurfaceToLevelAbove(IsoDirections.N)) {
                return false;
            }
            if (gridSquare.y < this.y && gridSquare.x == this.x && gridSquare.hasSlopedSurfaceToLevelAbove(IsoDirections.S)) {
                return false;
            }
            if (gridSquare.x > this.x && gridSquare.y == this.y && gridSquare.hasSlopedSurfaceToLevelAbove(IsoDirections.W)) {
                return false;
            }
            if (gridSquare.x < this.x && gridSquare.y == this.y && gridSquare.hasSlopedSurfaceToLevelAbove(IsoDirections.E)) {
                return false;
            }
        }
        if (gridSquare.z == this.z && !(!gridSquare.solidFloorCached ? gridSquare.TreatAsSolidFloor() : gridSquare.solidFloor) && bPathfind) {
            return true;
        }
        if (gridSquare.z == this.z && !(!gridSquare.solidFloorCached ? gridSquare.TreatAsSolidFloor() : gridSquare.solidFloor)) {
            if (gridSquare.z > 0 && (belowTarg = getter.getGridSquare(gridSquare.x, gridSquare.y, gridSquare.z - 1)) == null) {
                return true;
            }
        }
        if (this.z != gridSquare.z) {
            return gridSquare.z >= this.z || gridSquare.x != this.x || gridSquare.y != this.y || (!this.solidFloorCached ? this.TreatAsSolidFloor() : this.solidFloor);
        }
        boolean colN = testN && this.properties.has(IsoFlagType.collideN);
        boolean colW = testW && this.properties.has(IsoFlagType.collideW);
        boolean colS = testS && gridSquare.properties.has(IsoFlagType.collideN);
        boolean bl3 = colE = testE && gridSquare.properties.has(IsoFlagType.collideW);
        if (colN && bPathfind && this.properties.has(IsoFlagType.canPathN)) {
            colN = false;
        }
        if (colW && bPathfind && this.properties.has(IsoFlagType.canPathW)) {
            colW = false;
        }
        if (colS && bPathfind && gridSquare.properties.has(IsoFlagType.canPathN)) {
            colS = false;
        }
        if (colE && bPathfind && gridSquare.properties.has(IsoFlagType.canPathW)) {
            colE = false;
        }
        if (colW && this.has(IsoObjectType.stairsTW) && !bPathfind) {
            colW = false;
        }
        if (colN && this.has(IsoObjectType.stairsTN) && !bPathfind) {
            colN = false;
        }
        if (colN || colW || colS || colE) {
            return true;
        }
        boolean bl4 = diag = gridSquare.x != this.x && gridSquare.y != this.y;
        if (diag) {
            IsoGridSquare betweenA = getter.getGridSquare(this.x, gridSquare.y, this.z);
            IsoGridSquare betweenB = getter.getGridSquare(gridSquare.x, this.y, this.z);
            if (betweenA != null && betweenA != this && betweenA != gridSquare) {
                betweenA.RecalcPropertiesIfNeeded();
            }
            if (betweenB != null && betweenB != this && betweenB != gridSquare) {
                betweenB.RecalcPropertiesIfNeeded();
            }
            if (gridSquare == this || betweenA == betweenB || betweenA == this || betweenB == this || betweenA == gridSquare || betweenB == gridSquare) {
                return true;
            }
            if (gridSquare.x == this.x + 1 && gridSquare.y == this.y + 1 && betweenA != null && betweenB != null && betweenA.has(IsoFlagType.windowN) && betweenB.has(IsoFlagType.windowW)) {
                return true;
            }
            if (gridSquare.x == this.x - 1 && gridSquare.y == this.y - 1 && betweenA != null && betweenB != null && betweenA.has(IsoFlagType.windowW) && betweenB.has(IsoFlagType.windowN)) {
                return true;
            }
            if (this.CalculateCollide(betweenA, bVision, bPathfind, bIgnoreSolidTrans, false, getter)) {
                return true;
            }
            if (this.CalculateCollide(betweenB, bVision, bPathfind, bIgnoreSolidTrans, false, getter)) {
                return true;
            }
            if (gridSquare.CalculateCollide(betweenA, bVision, bPathfind, bIgnoreSolidTrans, false, getter)) {
                return true;
            }
            if (gridSquare.CalculateCollide(betweenB, bVision, bPathfind, bIgnoreSolidTrans, false, getter)) {
                return true;
            }
        }
        return false;
    }

    public boolean CalculateVisionBlocked(IsoGridSquare gridSquare) {
        return this.CalculateVisionBlocked(gridSquare, cellGetSquare);
    }

    public boolean CalculateVisionBlocked(IsoGridSquare gridSquare, GetSquare getter) {
        boolean diag;
        boolean colE;
        if (gridSquare == null) {
            return false;
        }
        if (Math.abs(gridSquare.getX() - this.getX()) > 1 || Math.abs(gridSquare.getY() - this.getY()) > 1) {
            return true;
        }
        boolean testW = false;
        boolean testE = false;
        boolean testN = false;
        boolean testS = false;
        if (gridSquare.x < this.x) {
            testW = true;
        }
        if (gridSquare.y < this.y) {
            testN = true;
        }
        if (gridSquare.x > this.x) {
            testE = true;
        }
        if (gridSquare.y > this.y) {
            testS = true;
        }
        if (gridSquare.properties.has(IsoFlagType.trans) || this.properties.has(IsoFlagType.trans)) {
            return false;
        }
        if (this.z != gridSquare.z) {
            if (gridSquare.z > this.z) {
                if (gridSquare.properties.has(IsoFlagType.solidfloor) && !gridSquare.getProperties().has(IsoFlagType.transparentFloor)) {
                    return true;
                }
                if (this.properties.has(IsoFlagType.noStart)) {
                    return true;
                }
                sq = getter.getGridSquare(this.x, this.y, gridSquare.z);
                if (sq == null) {
                    return false;
                }
                if (sq.properties.has(IsoFlagType.solidfloor) && !sq.getProperties().has(IsoFlagType.transparentFloor)) {
                    return true;
                }
            } else {
                if (this.properties.has(IsoFlagType.solidfloor) && !this.getProperties().has(IsoFlagType.transparentFloor)) {
                    return true;
                }
                if (this.properties.has(IsoFlagType.noStart)) {
                    return true;
                }
                sq = getter.getGridSquare(gridSquare.x, gridSquare.y, this.z);
                if (sq == null) {
                    return false;
                }
                if (sq.properties.has(IsoFlagType.solidfloor) && !sq.getProperties().has(IsoFlagType.transparentFloor)) {
                    return true;
                }
            }
        }
        boolean colN = testN && this.properties.has(IsoFlagType.collideN) && !this.properties.has(IsoFlagType.transparentN) && !this.properties.has(IsoFlagType.doorN);
        boolean colW = testW && this.properties.has(IsoFlagType.collideW) && !this.properties.has(IsoFlagType.transparentW) && !this.properties.has(IsoFlagType.doorW);
        boolean colS = testS && gridSquare.properties.has(IsoFlagType.collideN) && !gridSquare.properties.has(IsoFlagType.transparentN) && !gridSquare.properties.has(IsoFlagType.doorN);
        boolean bl = colE = testE && gridSquare.properties.has(IsoFlagType.collideW) && !gridSquare.properties.has(IsoFlagType.transparentW) && !gridSquare.properties.has(IsoFlagType.doorW);
        if (colN || colW || colS || colE) {
            return true;
        }
        boolean bl2 = diag = gridSquare.x != this.x && gridSquare.y != this.y;
        if (gridSquare.properties.has(IsoFlagType.solid) || gridSquare.properties.has(IsoFlagType.blocksight)) {
            return true;
        }
        if (diag) {
            IsoGridSquare betweenA = getter.getGridSquare(this.x, gridSquare.y, this.z);
            IsoGridSquare betweenB = getter.getGridSquare(gridSquare.x, this.y, this.z);
            if (betweenA != null && betweenA != this && betweenA != gridSquare) {
                betweenA.RecalcPropertiesIfNeeded();
            }
            if (betweenB != null && betweenB != this && betweenB != gridSquare) {
                betweenB.RecalcPropertiesIfNeeded();
            }
            if (this.CalculateVisionBlocked(betweenA, getter)) {
                return true;
            }
            if (this.CalculateVisionBlocked(betweenB, getter)) {
                return true;
            }
            if (gridSquare.CalculateVisionBlocked(betweenA, getter)) {
                return true;
            }
            if (gridSquare.CalculateVisionBlocked(betweenB, getter)) {
                return true;
            }
        }
        return false;
    }

    public IsoGameCharacter FindFriend(IsoGameCharacter g, int range, Stack<IsoGameCharacter> enemyList) {
        Stack<IsoGameCharacter> zombieList = new Stack<IsoGameCharacter>();
        for (int n = 0; n < g.getLocalList().size(); ++n) {
            IsoMovingObject obj = (IsoMovingObject)g.getLocalList().get(n);
            if (obj == g || obj == g.getFollowingTarget() || !(obj instanceof IsoGameCharacter)) continue;
            IsoGameCharacter isoGameCharacter = (IsoGameCharacter)obj;
            if (obj instanceof IsoZombie || enemyList.contains(obj)) continue;
            zombieList.add(isoGameCharacter);
        }
        float lowestDist = 1000000.0f;
        IsoGameCharacter lowest = null;
        for (IsoGameCharacter z : zombieList) {
            float dist = 0.0f;
            dist += Math.abs((float)this.getX() - z.getX());
            dist += Math.abs((float)this.getY() - z.getY());
            if ((dist += Math.abs((float)this.getZ() - z.getZ())) < lowestDist) {
                lowest = z;
                lowestDist = dist;
            }
            if (z != IsoPlayer.getInstance()) continue;
            lowest = z;
        }
        if (lowestDist > (float)range) {
            return null;
        }
        return lowest;
    }

    public IsoGameCharacter FindEnemy(IsoGameCharacter g, int range, ArrayList<IsoMovingObject> enemyList, IsoGameCharacter rangeTest, int testRangeMax) {
        float lowestDist = 1000000.0f;
        IsoGameCharacter lowest = null;
        for (int n = 0; n < enemyList.size(); ++n) {
            IsoGameCharacter z = (IsoGameCharacter)enemyList.get(n);
            float dist = 0.0f;
            dist += Math.abs((float)this.getX() - z.getX());
            dist += Math.abs((float)this.getY() - z.getY());
            if (!((dist += Math.abs((float)this.getZ() - z.getZ())) < (float)range) || !(dist < lowestDist) || !(z.DistTo((IsoMovingObject)rangeTest) < (float)testRangeMax)) continue;
            lowest = z;
            lowestDist = dist;
        }
        if (lowestDist > (float)range) {
            return null;
        }
        return lowest;
    }

    public IsoGameCharacter FindEnemy(IsoGameCharacter g, int range, ArrayList<IsoMovingObject> enemyList) {
        float lowestDist = 1000000.0f;
        IsoGameCharacter lowest = null;
        for (int n = 0; n < enemyList.size(); ++n) {
            IsoGameCharacter z = (IsoGameCharacter)enemyList.get(n);
            float dist = 0.0f;
            dist += Math.abs((float)this.getX() - z.getX());
            dist += Math.abs((float)this.getY() - z.getY());
            if (!((dist += Math.abs((float)this.getZ() - z.getZ())) < lowestDist)) continue;
            lowest = z;
            lowestDist = dist;
        }
        if (lowestDist > (float)range) {
            return null;
        }
        return lowest;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public int getZ() {
        return this.z;
    }

    public float getCenterX() {
        return (float)this.getX() + 0.5f;
    }

    public float getCenterY() {
        return (float)this.getY() + 0.5f;
    }

    public void RecalcProperties() {
        this.cachedIsFree = false;
        String fuelAmount = null;
        if (this.properties.has(IsoPropertyType.FUEL_AMOUNT)) {
            fuelAmount = this.properties.get(IsoPropertyType.FUEL_AMOUNT);
        }
        if (this.zone == null) {
            this.zone = IsoWorld.instance.metaGrid.getZoneAt(this.x, this.y, this.z);
        }
        this.properties.Clear();
        this.hasTypes = 0L;
        this.hasTree = false;
        boolean nonWaterSolidTrans = false;
        boolean nonWaterSolidFloor = false;
        boolean nonTransparentSolidFloor = false;
        boolean nonHoppableCollideN = false;
        boolean nonHoppableCollideW = false;
        boolean nonTransparentCutN = false;
        boolean nonTransparentCutW = false;
        boolean forceRender = false;
        int numObjects = this.objects.size();
        IsoObject[] objectArray = (IsoObject[])this.objects.getElements();
        for (int n = 0; n < numObjects; ++n) {
            PropertyContainer spriteProps;
            IsoObject obj = objectArray[n];
            if (obj == null || (spriteProps = obj.getProperties()) == null || spriteProps.has(IsoFlagType.blueprint)) continue;
            if (obj.sprite.forceRender) {
                forceRender = true;
            }
            if (obj.getType() == IsoObjectType.tree) {
                this.hasTree = true;
            }
            this.hasTypes |= 1L << obj.getType().index();
            this.properties.AddProperties(spriteProps);
            if (spriteProps.has(IsoFlagType.water)) {
                nonWaterSolidFloor = false;
            } else {
                if (!nonWaterSolidFloor && spriteProps.has(IsoFlagType.solidfloor)) {
                    nonWaterSolidFloor = true;
                }
                if (!nonWaterSolidTrans && spriteProps.has(IsoFlagType.solidtrans)) {
                    nonWaterSolidTrans = true;
                }
                if (!nonTransparentSolidFloor && spriteProps.has(IsoFlagType.solidfloor) && !spriteProps.has(IsoFlagType.transparentFloor)) {
                    nonTransparentSolidFloor = true;
                }
            }
            if (!nonHoppableCollideN && spriteProps.has(IsoFlagType.collideN) && !spriteProps.has(IsoFlagType.HoppableN)) {
                nonHoppableCollideN = true;
            }
            if (!nonHoppableCollideW && spriteProps.has(IsoFlagType.collideW) && !spriteProps.has(IsoFlagType.HoppableW)) {
                nonHoppableCollideW = true;
            }
            if (!nonTransparentCutN && spriteProps.has(IsoFlagType.cutN) && !spriteProps.has(IsoFlagType.transparentN) && !spriteProps.has(IsoFlagType.WallSE)) {
                nonTransparentCutN = true;
            }
            if (nonTransparentCutW || !spriteProps.has(IsoFlagType.cutW) || spriteProps.has(IsoFlagType.transparentW) || spriteProps.has(IsoFlagType.WallSE)) continue;
            nonTransparentCutW = true;
        }
        if (this.roomId != -1L || this.haveRoof) {
            this.getProperties().unset(IsoFlagType.exterior);
            try {
                if (!GameServer.server) {
                    this.getPuddles().recalc = true;
                }
            }
            catch (Exception e1) {
                DebugType.General.printException((Throwable)e1, LogSeverity.Error);
            }
        } else {
            this.getProperties().set(IsoFlagType.exterior);
            try {
                if (!GameServer.server) {
                    this.getPuddles().recalc = true;
                }
            }
            catch (Exception e1) {
                DebugType.General.printException((Throwable)e1, LogSeverity.Error);
            }
        }
        if (fuelAmount != null) {
            this.getProperties().set(IsoPropertyType.FUEL_AMOUNT, fuelAmount, false);
        }
        if (this.rainDrop != null) {
            this.properties.set(IsoFlagType.HasRaindrop);
        }
        if (forceRender) {
            this.properties.set(IsoFlagType.forceRender);
        }
        if (this.rainSplash != null) {
            this.properties.set(IsoFlagType.HasRainSplashes);
        }
        if (this.burntOut) {
            this.properties.set(IsoFlagType.burntOut);
        }
        if (!nonWaterSolidTrans && nonWaterSolidFloor && this.properties.has(IsoFlagType.water)) {
            this.properties.unset(IsoFlagType.solidtrans);
        }
        if (nonTransparentSolidFloor && this.properties.has(IsoFlagType.transparentFloor)) {
            this.properties.unset(IsoFlagType.transparentFloor);
        }
        if (nonHoppableCollideN && this.properties.has(IsoFlagType.HoppableN)) {
            this.properties.unset(IsoFlagType.canPathN);
            this.properties.unset(IsoFlagType.HoppableN);
        }
        if (nonHoppableCollideW && this.properties.has(IsoFlagType.HoppableW)) {
            this.properties.unset(IsoFlagType.canPathW);
            this.properties.unset(IsoFlagType.HoppableW);
        }
        if (nonTransparentCutN && this.properties.has(IsoFlagType.transparentN)) {
            this.properties.unset(IsoFlagType.transparentN);
        }
        if (nonTransparentCutW && this.properties.has(IsoFlagType.transparentW)) {
            this.properties.unset(IsoFlagType.transparentW);
        }
        boolean bl = this.propertiesDirty = this.chunk == null || this.chunk.loaded;
        if (this.chunk != null) {
            this.chunk.checkLightingLater_AllPlayers_OneLevel(this.z);
        }
        if (this.chunk != null) {
            this.chunk.checkPhysicsLater(this.z);
            this.chunk.collision.clear();
        }
        this.isExteriorCache = this.has(IsoFlagType.exterior);
        this.isSolidFloorCache = this.has(IsoFlagType.solidfloor);
        this.isVegitationCache = this.has(IsoFlagType.vegitation);
        if (this.water != null && this.water.isValid() && !this.properties.has(IsoFlagType.water)) {
            this.clearWater();
        }
    }

    public void RecalcPropertiesIfNeeded() {
        if (this.propertiesDirty) {
            this.RecalcProperties();
        }
    }

    public void ReCalculateCollide(IsoGridSquare square) {
        this.ReCalculateCollide(square, cellGetSquare);
    }

    public void ReCalculateCollide(IsoGridSquare square, GetSquare getter) {
        if (1 + square.x - this.x < 0 || 1 + square.y - this.y < 0 || 1 + square.z - this.z < 0) {
            DebugLog.log((String)"ERROR");
        }
        boolean b = this.CalculateCollide(square, false, false, false, false, getter);
        this.collideMatrix = IsoGridSquare.setMatrixBit(this.collideMatrix, 1 + square.x - this.x, 1 + square.y - this.y, 1 + square.z - this.z, b);
    }

    public void ReCalculatePathFind(IsoGridSquare square) {
        this.ReCalculatePathFind(square, cellGetSquare);
    }

    public void ReCalculatePathFind(IsoGridSquare square, GetSquare getter) {
        boolean b = this.CalculateCollide(square, false, true, false, false, getter);
        this.pathMatrix = IsoGridSquare.setMatrixBit(this.pathMatrix, 1 + square.x - this.x, 1 + square.y - this.y, 1 + square.z - this.z, b);
    }

    public void ReCalculateVisionBlocked(IsoGridSquare square) {
        this.ReCalculateVisionBlocked(square, cellGetSquare);
    }

    public void ReCalculateVisionBlocked(IsoGridSquare square, GetSquare getter) {
        boolean b = this.CalculateVisionBlocked(square, getter);
        this.visionMatrix = IsoGridSquare.setMatrixBit(this.visionMatrix, 1 + square.x - this.x, 1 + square.y - this.y, 1 + square.z - this.z, b);
    }

    private static boolean testCollideSpecialObjects(IsoMovingObject collideObject, IsoGridSquare sqFrom, IsoGridSquare sqTo) {
        for (int n = 0; n < sqTo.specialObjects.size(); ++n) {
            IsoThumpable isoThumpable;
            IsoObject obj = sqTo.specialObjects.get(n);
            if (!obj.TestCollide(collideObject, sqFrom, sqTo)) continue;
            if (obj instanceof IsoDoor) {
                collideObject.setCollidedWithDoor(true);
            } else if (obj instanceof IsoThumpable && (isoThumpable = (IsoThumpable)obj).isDoor()) {
                collideObject.setCollidedWithDoor(true);
            }
            collideObject.setCollidedObject(obj);
            return true;
        }
        return false;
    }

    public boolean testCollideAdjacent(IsoMovingObject collideObject, int x, int y, int z) {
        IsoPlayer player;
        IsoObject obj;
        IsoPlayer isoPlayer;
        if (collideObject instanceof IsoPlayer && (isoPlayer = (IsoPlayer)collideObject).isNoClip()) {
            return false;
        }
        if (this.collideMatrix == -1) {
            return true;
        }
        if (x < -1 || x > 1 || y < -1 || y > 1 || z < -1 || z > 1) {
            return true;
        }
        if (!IsoWorld.instance.metaGrid.isValidChunk((this.x + x) / 8, (this.y + y) / 8)) {
            return true;
        }
        IsoGridSquare sq = this.getCell().getGridSquare(this.x + x, this.y + y, this.z + z);
        if (collideObject != null && collideObject.shouldIgnoreCollisionWithSquare(sq)) {
            return false;
        }
        if ((GameServer.server || GameClient.client) && collideObject instanceof IsoPlayer) {
            IsoGridSquare sqFloor;
            boolean allowTrepass;
            IsoPlayer isoPlayer2 = (IsoPlayer)collideObject;
            if (!(collideObject instanceof IsoAnimal) && !(allowTrepass = SafeHouse.isSafehouseAllowTrepass((IsoGridSquare)(sqFloor = this.getCell().getGridSquare(this.x + x, this.y + y, 0)), (IsoPlayer)isoPlayer2)) && (GameServer.server || isoPlayer2.isLocalPlayer())) {
                return true;
            }
        }
        if (sq != null && collideObject != null && (obj = this.testCollideSpecialObjects(sq)) != null) {
            IsoThumpable isoThumpable;
            collideObject.collideWith(obj);
            if (obj instanceof IsoDoor) {
                collideObject.setCollidedWithDoor(true);
            } else if (obj instanceof IsoThumpable && (isoThumpable = (IsoThumpable)obj).isDoor()) {
                collideObject.setCollidedWithDoor(true);
            }
            collideObject.setCollidedObject(obj);
            return true;
        }
        if (useSlowCollision) {
            return this.CalculateCollide(sq, false, false, false);
        }
        if (collideObject instanceof IsoPlayer && !(player = (IsoPlayer)collideObject).isAnimal() && IsoGridSquare.getMatrixBit(this.collideMatrix, x + 1, y + 1, z + 1)) {
            this.RecalcAllWithNeighbours(true);
        }
        return IsoGridSquare.getMatrixBit(this.collideMatrix, x + 1, y + 1, z + 1);
    }

    public boolean testCollideAdjacentAdvanced(int x, int y, int z, boolean ignoreDoors) {
        if (this.collideMatrix == -1) {
            return true;
        }
        if (x < -1 || x > 1 || y < -1 || y > 1 || z < -1 || z > 1) {
            return true;
        }
        IsoGridSquare sq = this.getCell().getGridSquare(this.x + x, this.y + y, this.z + z);
        if (sq != null) {
            IsoObject obj;
            int n;
            if (!sq.specialObjects.isEmpty()) {
                for (n = 0; n < sq.specialObjects.size(); ++n) {
                    obj = sq.specialObjects.get(n);
                    if (!obj.TestCollide(null, this, sq)) continue;
                    return true;
                }
            }
            if (!this.specialObjects.isEmpty()) {
                for (n = 0; n < this.specialObjects.size(); ++n) {
                    obj = this.specialObjects.get(n);
                    if (!obj.TestCollide(null, this, sq)) continue;
                    return true;
                }
            }
        }
        if (useSlowCollision) {
            return this.CalculateCollide(sq, false, false, false);
        }
        return IsoGridSquare.getMatrixBit(this.collideMatrix, x + 1, y + 1, z + 1);
    }

    public static void setCollisionMode() {
        useSlowCollision = !useSlowCollision;
    }

    public boolean testPathFindAdjacent(IsoMovingObject mover, int x, int y, int z) {
        return this.testPathFindAdjacent(mover, x, y, z, cellGetSquare);
    }

    public boolean testPathFindAdjacent(IsoMovingObject mover, int x, int y, int z, GetSquare getter) {
        IsoGridSquare gridSquare;
        if (x < -1 || x > 1 || y < -1 || y > 1 || z < -1 || z > 1) {
            return true;
        }
        if (this.has(IsoObjectType.stairsTN) || this.has(IsoObjectType.stairsTW)) {
            gridSquare = getter.getGridSquare(x + this.x, y + this.y, z + this.z);
            if (gridSquare == null) {
                return true;
            }
            if (this.has(IsoObjectType.stairsTN) && gridSquare.y < this.y && gridSquare.z == this.z) {
                return true;
            }
            if (this.has(IsoObjectType.stairsTW) && gridSquare.x < this.x && gridSquare.z == this.z) {
                return true;
            }
        }
        if (doSlowPathfinding) {
            gridSquare = getter.getGridSquare(x + this.x, y + this.y, z + this.z);
            return this.CalculateCollide(gridSquare, false, true, false, false, getter);
        }
        return IsoGridSquare.getMatrixBit(this.pathMatrix, x + 1, y + 1, z + 1);
    }

    public LosUtil.TestResults testVisionAdjacent(int x, int y, int z, boolean specialDiag, boolean bIgnoreDoors) {
        LosUtil.TestResults test;
        IsoGridSquare sq;
        if (x < -1 || x > 1 || y < -1 || y > 1 || z < -1 || z > 1) {
            return LosUtil.TestResults.Blocked;
        }
        if (z == 1 && (x != 0 || y != 0) && this.HasElevatedFloor() && (sq = this.getCell().getGridSquare(this.x, this.y, this.z + z)) != null) {
            return sq.testVisionAdjacent(x, y, 0, specialDiag, bIgnoreDoors);
        }
        if (z == -1 && (x != 0 || y != 0) && (sq = this.getCell().getGridSquare(this.x + x, this.y + y, this.z + z)) != null && sq.HasElevatedFloor()) {
            return this.testVisionAdjacent(x, y, 0, specialDiag, bIgnoreDoors);
        }
        if (x != 0 && y != 0 && specialDiag) {
            IsoGridSquare sq2;
            test = this.DoDiagnalCheck(x, y, z, bIgnoreDoors);
            if (!(GameServer.server || test != LosUtil.TestResults.Clear && test != LosUtil.TestResults.ClearThroughWindow && test != LosUtil.TestResults.ClearThroughOpenDoor && test != LosUtil.TestResults.ClearThroughClosedDoor || (sq2 = this.getCell().getGridSquare(this.x + x, this.y + y, this.z + z)) == null)) {
                test = sq2.DoDiagnalCheck(-x, -y, -z, bIgnoreDoors);
            }
        } else {
            IsoGridSquare sq3 = this.getCell().getGridSquare(this.x + x, this.y + y, this.z + z);
            LosUtil.TestResults ret = LosUtil.TestResults.Clear;
            if (sq3 != null && sq3.z == this.z) {
                IsoThumpable isoThumpable;
                IsoThumpable isoThumpable2;
                IsoThumpable isoThumpable3;
                IsoDoor isoDoor;
                IsoObject.VisionResult vis;
                IsoObject obj;
                int n;
                if (!this.specialObjects.isEmpty()) {
                    for (n = 0; n < this.specialObjects.size(); ++n) {
                        obj = this.specialObjects.get(n);
                        if (obj == null) {
                            return LosUtil.TestResults.Clear;
                        }
                        vis = obj.TestVision(this, sq3);
                        if (vis == IsoObject.VisionResult.NoEffect) continue;
                        if (vis == IsoObject.VisionResult.Unblocked && obj instanceof IsoDoor) {
                            isoDoor = (IsoDoor)obj;
                            ret = isoDoor.IsOpen() ? LosUtil.TestResults.ClearThroughOpenDoor : LosUtil.TestResults.ClearThroughClosedDoor;
                            continue;
                        }
                        if (vis == IsoObject.VisionResult.Unblocked && obj instanceof IsoThumpable && (isoThumpable3 = (IsoThumpable)obj).isDoor()) {
                            ret = LosUtil.TestResults.ClearThroughOpenDoor;
                            continue;
                        }
                        if (vis == IsoObject.VisionResult.Unblocked && obj instanceof IsoWindow) {
                            ret = LosUtil.TestResults.ClearThroughWindow;
                            continue;
                        }
                        if (vis == IsoObject.VisionResult.Blocked && obj instanceof IsoDoor && !bIgnoreDoors) {
                            return LosUtil.TestResults.Blocked;
                        }
                        if (vis == IsoObject.VisionResult.Blocked && obj instanceof IsoThumpable && (isoThumpable2 = (IsoThumpable)obj).isDoor() && !bIgnoreDoors) {
                            return LosUtil.TestResults.Blocked;
                        }
                        if (vis == IsoObject.VisionResult.Blocked && obj instanceof IsoThumpable && (isoThumpable = (IsoThumpable)obj).isWindow()) {
                            return LosUtil.TestResults.Blocked;
                        }
                        if (vis == IsoObject.VisionResult.Blocked && obj instanceof IsoCurtain) {
                            return LosUtil.TestResults.Blocked;
                        }
                        if (vis == IsoObject.VisionResult.Blocked && obj instanceof IsoWindow) {
                            return LosUtil.TestResults.Blocked;
                        }
                        if (vis != IsoObject.VisionResult.Blocked || !(obj instanceof IsoBarricade)) continue;
                        return LosUtil.TestResults.Blocked;
                    }
                }
                if (!sq3.specialObjects.isEmpty()) {
                    for (n = 0; n < sq3.specialObjects.size(); ++n) {
                        obj = sq3.specialObjects.get(n);
                        if (obj == null) {
                            return LosUtil.TestResults.Clear;
                        }
                        vis = obj.TestVision(this, sq3);
                        if (vis == IsoObject.VisionResult.NoEffect) continue;
                        if (vis == IsoObject.VisionResult.Unblocked && obj instanceof IsoDoor) {
                            isoDoor = (IsoDoor)obj;
                            ret = isoDoor.IsOpen() ? LosUtil.TestResults.ClearThroughOpenDoor : LosUtil.TestResults.ClearThroughClosedDoor;
                            continue;
                        }
                        if (vis == IsoObject.VisionResult.Unblocked && obj instanceof IsoThumpable && (isoThumpable3 = (IsoThumpable)obj).isDoor()) {
                            ret = LosUtil.TestResults.ClearThroughOpenDoor;
                            continue;
                        }
                        if (vis == IsoObject.VisionResult.Unblocked && obj instanceof IsoWindow) {
                            ret = LosUtil.TestResults.ClearThroughWindow;
                            continue;
                        }
                        if (vis == IsoObject.VisionResult.Blocked && obj instanceof IsoDoor && !bIgnoreDoors) {
                            return LosUtil.TestResults.Blocked;
                        }
                        if (vis == IsoObject.VisionResult.Blocked && obj instanceof IsoThumpable && (isoThumpable2 = (IsoThumpable)obj).isDoor() && !bIgnoreDoors) {
                            return LosUtil.TestResults.Blocked;
                        }
                        if (vis == IsoObject.VisionResult.Blocked && obj instanceof IsoThumpable && (isoThumpable = (IsoThumpable)obj).isWindow()) {
                            return LosUtil.TestResults.Blocked;
                        }
                        if (vis == IsoObject.VisionResult.Blocked && obj instanceof IsoCurtain) {
                            return LosUtil.TestResults.Blocked;
                        }
                        if (vis == IsoObject.VisionResult.Blocked && obj instanceof IsoWindow) {
                            return LosUtil.TestResults.Blocked;
                        }
                        if (vis != IsoObject.VisionResult.Blocked || !(obj instanceof IsoBarricade)) continue;
                        return LosUtil.TestResults.Blocked;
                    }
                }
            } else if (z > 0 && sq3 != null && (this.z != -1 || sq3.z != 0) && sq3.getProperties().has(IsoFlagType.exterior) && !this.getProperties().has(IsoFlagType.exterior)) {
                ret = LosUtil.TestResults.Blocked;
            }
            test = ret;
            return !IsoGridSquare.getMatrixBit(this.visionMatrix, x + 1, y + 1, z + 1) ? test : LosUtil.TestResults.Blocked;
        }
        return test;
    }

    public boolean TreatAsSolidFloor() {
        if (this.solidFloorCached) {
            return this.solidFloor;
        }
        this.solidFloor = this.properties.has(IsoFlagType.solidfloor) || this.HasStairs() || this.hasSlopedSurface();
        this.solidFloorCached = true;
        return this.solidFloor;
    }

    public void AddSpecialTileObject(IsoObject obj) {
        this.AddSpecialObject(obj);
    }

    public void renderCharacters(int maxZ, boolean deadRender, boolean doBlendFunc) {
        IsoMovingObject mov;
        int n;
        if (this.z >= maxZ) {
            return;
        }
        if (!isOnScreenLast) {
            // empty if block
        }
        if (doBlendFunc) {
            IsoGridSquare.setBlendFunc();
        }
        if (this.movingObjects.size() > 1) {
            Collections.sort(this.movingObjects, comp);
        }
        int playerIndex = IsoCamera.frameState.playerIndex;
        ColorInfo lightInfo = this.lightInfo[playerIndex];
        int size = this.staticMovingObjects.size();
        for (n = 0; n < size; ++n) {
            mov = this.staticMovingObjects.get(n);
            if (mov.sprite == null && !(mov instanceof IsoDeadBody) || deadRender && (!(mov instanceof IsoDeadBody) || this.HasStairs()) || !deadRender && mov instanceof IsoDeadBody && !this.HasStairs()) continue;
            mov.render(mov.getX(), mov.getY(), mov.getZ(), lightInfo, true, false, null);
        }
        size = this.movingObjects.size();
        for (n = 0; n < size; ++n) {
            mov = this.movingObjects.get(n);
            if (mov == null || mov.sprite == null) continue;
            boolean bOnFloor = mov.onFloor;
            if (bOnFloor && mov instanceof IsoZombie) {
                IsoZombie zombie = (IsoZombie)mov;
                bOnFloor = zombie.isProne();
                if (!BaseVehicle.renderToTexture) {
                    bOnFloor = false;
                }
            }
            if (deadRender && !bOnFloor || !deadRender && bOnFloor) continue;
            mov.render(mov.getX(), mov.getY(), mov.getZ(), lightInfo, true, false, null);
        }
    }

    public void renderDeferredCharacters(int maxZ) {
        if (this.deferedCharacters.isEmpty()) {
            return;
        }
        if (this.deferredCharacterTick != this.getCell().deferredCharacterTick) {
            this.deferedCharacters.clear();
            return;
        }
        if (this.z >= maxZ) {
            this.deferedCharacters.clear();
            return;
        }
        IndieGL.enableAlphaTest();
        IndieGL.glAlphaFunc((int)516, (float)0.0f);
        float sx = IsoUtils.XToScreen((float)this.x, (float)this.y, (float)this.z, (int)0);
        float sy = IsoUtils.YToScreen((float)this.x, (float)this.y, (float)this.z, (int)0);
        IndieGL.glColorMask((boolean)false, (boolean)false, (boolean)false, (boolean)false);
        Texture.getWhite().renderwallnw(sx -= IsoCamera.frameState.offX, sy -= IsoCamera.frameState.offY, (float)(64 * Core.tileScale), (float)(32 * Core.tileScale), -1, -1, -1, -1, -1, -1);
        IndieGL.glColorMask((boolean)true, (boolean)true, (boolean)true, (boolean)true);
        IndieGL.enableAlphaTest();
        IndieGL.glAlphaFunc((int)516, (float)0.0f);
        ColorInfo lightInfo = this.lightInfo[IsoCamera.frameState.playerIndex];
        Collections.sort(this.deferedCharacters, comp);
        for (int n = 0; n < this.deferedCharacters.size(); ++n) {
            IsoGameCharacter chr = this.deferedCharacters.get(n);
            if (chr.sprite == null) continue;
            chr.setbDoDefer(false);
            chr.render(chr.getX(), chr.getY(), chr.getZ(), lightInfo, true, false, null);
            chr.renderObjectPicker(chr.getX(), chr.getY(), chr.getZ(), lightInfo);
            chr.setbDoDefer(true);
        }
        this.deferedCharacters.clear();
        IndieGL.glAlphaFunc((int)516, (float)0.0f);
    }

    public void switchLight(boolean active) {
        for (int n = 0; n < this.objects.size(); ++n) {
            IsoObject o = (IsoObject)this.objects.get(n);
            if (!(o instanceof IsoLightSwitch)) continue;
            IsoLightSwitch isoLightSwitch = (IsoLightSwitch)o;
            isoLightSwitch.setActive(active);
        }
    }

    public void removeGlassAttachments(IsoWindow window) {
        IsoDirections[] isoDirectionsArray;
        if (window.getNorth()) {
            IsoDirections[] isoDirectionsArray2 = new IsoDirections[2];
            isoDirectionsArray2[0] = IsoDirections.N;
            isoDirectionsArray = isoDirectionsArray2;
            isoDirectionsArray2[1] = IsoDirections.S;
        } else {
            IsoDirections[] isoDirectionsArray3 = new IsoDirections[2];
            isoDirectionsArray3[0] = IsoDirections.W;
            isoDirectionsArray = isoDirectionsArray3;
            isoDirectionsArray3[1] = IsoDirections.E;
        }
        IsoDirections[] windowSides = isoDirectionsArray;
        for (int n = 0; n < this.objects.size(); ++n) {
            boolean isAttachedToWindowWall;
            IsoObject o = (IsoObject)this.objects.get(n);
            if (o.sprite == null) continue;
            boolean isAttachedToGlass = o.getProperties().has(IsoPropertyType.ATTACHED_TO_GLASS);
            boolean isWallObject = o.getProperties().has(IsoPropertyType.IS_MOVE_ABLE) && (o.getProperties().has(IsoPropertyType.IS_HIGH) || "WallObject".equals(o.getProperties().get(IsoPropertyType.MOVE_TYPE)));
            boolean bl = isAttachedToWindowWall = isWallObject && Arrays.stream(windowSides).anyMatch(x -> x == o.getFacing());
            if (!isAttachedToGlass && (!(o instanceof IsoLightSwitch) || !isAttachedToWindowWall)) continue;
            this.RemoveTileObject(o);
            --n;
        }
    }

    public boolean IsOnScreen() {
        return this.IsOnScreen(false);
    }

    public boolean IsOnScreen(boolean halfTileBorder) {
        int border;
        if (this.cachedScreenValue != Core.tileScale) {
            this.cachedScreenX = IsoUtils.XToScreen((float)this.x, (float)this.y, (float)this.z, (int)0);
            this.cachedScreenY = IsoUtils.YToScreen((float)this.x, (float)this.y, (float)this.z, (int)0);
            this.cachedScreenValue = Core.tileScale;
        }
        float sx = this.cachedScreenX;
        float sy = this.cachedScreenY;
        sx -= IsoCamera.frameState.offX;
        sy -= IsoCamera.frameState.offY;
        int n = border = halfTileBorder ? 32 * Core.tileScale : 0;
        if (this.hasTree) {
            int offsetX = (FBORenderChunk.JUMBO_XXL_WIDTH - FBORenderChunk.FLOOR_WIDTH) / 2;
            int offsetY = FBORenderChunk.JUMBO_XXL_HEIGHT - FBORenderChunk.FLOOR_HEIGHT;
            if (sx + (float)offsetX <= (float)(0 - border)) {
                return false;
            }
            if (sy + (float)FBORenderChunk.FLOOR_HEIGHT <= (float)(0 - border)) {
                return false;
            }
            if (sx - (float)offsetX >= (float)(IsoCamera.frameState.offscreenWidth + border)) {
                return false;
            }
            return !(sy - (float)offsetY >= (float)(IsoCamera.frameState.offscreenHeight + border));
        }
        if (sx + (float)FBORenderChunk.FLOOR_WIDTH / 2.0f <= (float)(0 - border)) {
            return false;
        }
        if (sy + (float)FBORenderChunk.FLOOR_HEIGHT <= (float)(0 - border)) {
            return false;
        }
        if (sx - (float)FBORenderChunk.FLOOR_WIDTH / 2.0f >= (float)(IsoCamera.frameState.offscreenWidth + border)) {
            return false;
        }
        return !(sy - (float)FBORenderChunk.PIXELS_PER_LEVEL >= (float)(IsoCamera.frameState.offscreenHeight + border));
    }

    private static void initWaterSplashCache() {
        int i;
        for (i = 0; i < 16; ++i) {
            IsoGridSquare.waterSplashCache[i] = Texture.getSharedTexture((String)("media/textures/waterSplashes/WaterSplashSmall0_" + i + ".png"));
        }
        for (i = 16; i < 48; ++i) {
            IsoGridSquare.waterSplashCache[i] = Texture.getSharedTexture((String)("media/textures/waterSplashes/WaterSplashBig0_" + i + ".png"));
        }
        for (i = 48; i < 80; ++i) {
            IsoGridSquare.waterSplashCache[i] = Texture.getSharedTexture((String)("media/textures/waterSplashes/WaterSplashBig1_" + i + ".png"));
        }
        isWaterSplashCacheInitialised = true;
    }

    public void startWaterSplash(boolean isBigSplash, float dx, float dy) {
        if (this.isSeen(IsoCamera.frameState.playerIndex) && !this.waterSplashData.isSplashNow()) {
            if (isBigSplash) {
                this.waterSplashData.initBigSplash(dx, dy);
            } else {
                this.waterSplashData.initSmallSplash(dx, dy);
            }
            FishSplashSoundManager.instance.addSquare(this);
        }
    }

    public void startWaterSplash(boolean isBigSplash) {
        this.startWaterSplash(isBigSplash, Rand.Next((float)0.0f, (float)0.5f) - 0.25f, Rand.Next((float)0.0f, (float)0.5f) - 0.25f);
    }

    public boolean shouldRenderFishSplash(int playerIndex) {
        if (this.objects.size() != 1) {
            return false;
        }
        IsoObject object = (IsoObject)this.objects.get(0);
        if (object.attachedAnimSprite != null && !object.attachedAnimSprite.isEmpty()) {
            return false;
        }
        return this.chunk != null && this.isCouldSee(playerIndex) && this.waterSplashData.isSplashNow();
    }

    public ColorInfo getLightInfo(int playerNumber) {
        return this.lightInfo[playerNumber];
    }

    public void cacheLightInfo() {
        int playerIndex = IsoCamera.frameState.playerIndex;
        this.lightInfo[playerIndex] = this.lighting[playerIndex].lightInfo();
    }

    public void setLightInfoServerGUIOnly(ColorInfo c) {
        this.lightInfo[0] = c;
    }

    public int renderFloor(Shader floorShader) {
        try (AbstractPerformanceProfileProbe abstractPerformanceProfileProbe = s_performance.renderFloor.profile();){
            int n = this.renderFloorInternal(floorShader);
            return n;
        }
    }

    private int renderFloorInternal(Shader floorShader) {
        long roomID;
        int playerIndex = IsoCamera.frameState.playerIndex;
        ColorInfo lightInfo = this.lightInfo[playerIndex];
        IsoGridSquare camCharacterSquare = IsoCamera.frameState.camCharacterSquare;
        boolean bCouldSee = this.lighting[playerIndex].bCouldSee();
        float darkMulti = this.lighting[playerIndex].darkMulti();
        boolean showHighlightColors = GameClient.client && IsoPlayer.players[playerIndex] != null && IsoPlayer.players[playerIndex].isSeeNonPvpZone();
        boolean showSafehouseHighlighting = Core.debug && GameClient.client && SafeHouse.isSafeHouse((IsoGridSquare)this, null, (boolean)true) != null;
        boolean showMetaAnimalZoneHighlightColors = IsoPlayer.players[playerIndex] != null && IsoPlayer.players[playerIndex].isSeeDesignationZone();
        Double selectedMetaAnimalZone = IsoPlayer.players[playerIndex] != null ? IsoPlayer.players[playerIndex].getSelectedZoneForHighlight() : 0.0;
        boolean objSetAlpha = true;
        float objAlpha = 1.0f;
        float objTargetAlpha = 1.0f;
        if (camCharacterSquare != null && (roomID = this.getRoomID()) != -1L) {
            long playerRoomID = IsoWorld.instance.currentCell.GetEffectivePlayerRoomId();
            if (playerRoomID == -1L && IsoWorld.instance.currentCell.CanBuildingSquareOccludePlayer(this, playerIndex)) {
                objSetAlpha = false;
                objAlpha = 1.0f;
                objTargetAlpha = 1.0f;
            } else if (!bCouldSee && roomID != playerRoomID && darkMulti < 0.5f) {
                objSetAlpha = false;
                objAlpha = 0.0f;
                objTargetAlpha = darkMulti * 2.0f;
            }
        }
        IsoWaterGeometry water = this.z == 0 ? this.getWater() : null;
        boolean isShore = water != null && water.shore;
        float depth0 = water == null ? 0.0f : water.depth[0];
        float depth1 = water == null ? 0.0f : water.depth[3];
        float depth2 = water == null ? 0.0f : water.depth[2];
        float depth3 = water == null ? 0.0f : water.depth[1];
        IsoGridSquare.setBlendFunc();
        int flags = 0;
        int size = this.objects.size();
        IsoObject[] objectArray = (IsoObject[])this.objects.getElements();
        for (int n = 0; n < size; ++n) {
            DesignationZone zone;
            IsoObject obj = objectArray[n];
            if (showHighlightColors && !obj.isHighlighted(playerIndex)) {
                obj.setHighlighted(playerIndex, true);
                if (NonPvpZone.getNonPvpZone((int)this.x, (int)this.y) != null) {
                    obj.setHighlightColor(0.6f, 0.6f, 1.0f, 0.5f);
                } else {
                    obj.setHighlightColor(1.0f, 0.6f, 0.6f, 0.5f);
                }
            }
            if (showMetaAnimalZoneHighlightColors && (zone = DesignationZone.getZone((int)this.x, (int)this.y, (int)this.z)) != null) {
                obj.setHighlighted(true);
                if (selectedMetaAnimalZone > 0.0 && zone.getId().intValue() == selectedMetaAnimalZone.intValue()) {
                    obj.setHighlightColor(0.2f, 0.8f, 0.9f, 0.8f);
                } else {
                    obj.setHighlightColor(0.2f, 0.2f, 0.9f, 0.8f);
                }
            }
            if (showSafehouseHighlighting) {
                obj.setHighlighted(true);
                obj.setHighlightColor(1.0f, 0.0f, 0.0f, 1.0f);
            }
            boolean bDoIt = true;
            if (obj.sprite != null && !obj.sprite.solidfloor && obj.sprite.renderLayer != 1) {
                bDoIt = false;
                flags |= 4;
            }
            if (obj instanceof IsoFire || obj instanceof IsoCarBatteryCharger) {
                bDoIt = false;
                flags |= 4;
            }
            if (PerformanceSettings.fboRenderChunk && IsoWater.getInstance().getShaderEnable() && water != null && water.isValid() && obj.sprite != null && obj.sprite.properties.has(IsoFlagType.water)) {
                bDoIt = false;
            }
            if (!bDoIt) {
                boolean bGrassEtc;
                boolean bl = bGrassEtc = obj.sprite != null && (obj.sprite.isBush || obj.sprite.canBeRemoved || obj.sprite.attachedFloor);
                if (!this.flattenGrassEtc || !bGrassEtc) continue;
                flags |= 2;
                continue;
            }
            IndieGL.glAlphaFunc((int)516, (float)0.0f);
            obj.setTargetAlpha(playerIndex, objTargetAlpha);
            if (objSetAlpha) {
                obj.setAlpha(playerIndex, objAlpha);
            }
            if (DebugOptions.instance.terrain.renderTiles.renderGridSquares.getValue() && obj.sprite != null) {
                IndieGL.StartShader((Shader)floorShader, (int)playerIndex);
                FloorShaperAttachedSprites attachedFloorShaper = FloorShaperAttachedSprites.instance;
                Object floorShaper = obj.getProperties().has(IsoFlagType.diamondFloor) || obj.getProperties().has(IsoFlagType.water) ? FloorShaperDiamond.instance : FloorShaperDeDiamond.instance;
                int col0 = this.getVertLight(0, playerIndex);
                int col1 = this.getVertLight(1, playerIndex);
                int col2 = this.getVertLight(2, playerIndex);
                int col3 = this.getVertLight(3, playerIndex);
                if (DebugOptions.instance.terrain.renderTiles.isoGridSquare.floor.lightingDebug.getValue()) {
                    col0 = -65536;
                    col1 = -65536;
                    col2 = -16776961;
                    col3 = -16776961;
                }
                attachedFloorShaper.setShore(isShore);
                attachedFloorShaper.setWaterDepth(depth0, depth1, depth2, depth3);
                attachedFloorShaper.setVertColors(col0, col1, col2, col3);
                floorShaper.setShore(isShore);
                floorShaper.setWaterDepth(depth0, depth1, depth2, depth3);
                floorShaper.setVertColors(col0, col1, col2, col3);
                obj.renderFloorTile(this.x, this.y, this.z, defColorInfo, true, false, floorShader, (Consumer<TextureDraw>)floorShaper, (Consumer<TextureDraw>)attachedFloorShaper);
                IndieGL.StartShader(null);
            }
            flags |= 1;
            if (!obj.isHighlighted(playerIndex)) {
                flags |= 8;
            }
            if (PerformanceSettings.fboRenderChunk || !obj.isHighlightRenderOnce(playerIndex)) continue;
            obj.setHighlighted(playerIndex, false, false);
        }
        if (!FBORenderChunkManager.instance.isCaching() && this.IsOnScreen(true)) {
            IndieGL.glBlendFunc((int)770, (int)771);
            this.renderRainSplash(playerIndex, lightInfo);
            this.renderFishSplash(playerIndex, lightInfo);
        }
        return flags;
    }

    public void renderRainSplash(int playerIndex, ColorInfo lightInfo) {
        if ((this.getCell().rainIntensity > 0 || RainManager.isRaining().booleanValue() && RainManager.rainIntensity > 0.0f) && this.isExteriorCache && !this.isVegitationCache && this.isSolidFloorCache && this.isCouldSee(playerIndex)) {
            if (!IsoCamera.frameState.paused) {
                int intensity;
                int n = intensity = this.getCell().rainIntensity == 0 ? Math.min(PZMath.fastfloor((float)(RainManager.rainIntensity / 0.2f)) + 1, 5) : this.getCell().rainIntensity;
                if (this.splashFrame < 0.0f && Rand.Next((int)Rand.AdjustForFramerate((int)((int)(5.0f / (float)intensity) * 100))) == 0) {
                    this.splashFrame = 0.0f;
                }
            }
            if (this.splashFrame >= 0.0f) {
                Texture tex;
                int frame = (int)(this.splashFrame * 4.0f);
                if (rainsplashCache[frame] == null) {
                    IsoGridSquare.rainsplashCache[frame] = "RainSplash_00_" + frame;
                }
                if ((tex = Texture.getSharedTexture((String)rainsplashCache[frame])) != null) {
                    float sx = IsoUtils.XToScreen((float)((float)this.x + this.splashX), (float)((float)this.y + this.splashY), (float)this.z, (int)0) - IsoCamera.frameState.offX;
                    float sy = IsoUtils.YToScreen((float)((float)this.x + this.splashX), (float)((float)this.y + this.splashY), (float)this.z, (int)0) - IsoCamera.frameState.offY;
                    float alpha = 0.6f * (this.getCell().rainIntensity > 0 ? 1.0f : RainManager.rainIntensity);
                    float shaderMod = SceneShaderStore.weatherShader != null ? 0.6f : 1.0f;
                    SpriteRenderer.instance.render(tex, sx -= (float)(tex.getWidth() / 2 * Core.tileScale), sy -= (float)(tex.getHeight() / 2 * Core.tileScale), (float)(tex.getWidth() * Core.tileScale), (float)(tex.getHeight() * Core.tileScale), 0.8f * lightInfo.r, 0.9f * lightInfo.g, 1.0f * lightInfo.b, alpha * shaderMod, null);
                }
                if (!IsoCamera.frameState.paused && this.splashFrameNum != IsoCamera.frameState.frameCount) {
                    this.splashFrame += 0.08f * (30.0f / (float)PerformanceSettings.getLockFPS());
                    if (this.splashFrame >= 1.0f) {
                        this.splashX = Rand.Next((float)0.1f, (float)0.9f);
                        this.splashY = Rand.Next((float)0.1f, (float)0.9f);
                        this.splashFrame = -1.0f;
                    }
                    this.splashFrameNum = IsoCamera.frameState.frameCount;
                }
            }
        } else {
            this.splashFrame = -1.0f;
        }
    }

    public void renderRainSplash(int playerIndex, ColorInfo lightInfo, float splashFrame, boolean bRandomXY) {
        Texture tex;
        if (splashFrame < 0.0f || (int)(splashFrame * 4.0f) >= rainsplashCache.length) {
            return;
        }
        if (!this.isCouldSee(playerIndex)) {
            return;
        }
        if (!this.isExteriorCache || this.isVegitationCache || !this.isSolidFloorCache) {
            return;
        }
        int frame = (int)(splashFrame * 4.0f);
        if (rainsplashCache[frame] == null) {
            IsoGridSquare.rainsplashCache[frame] = "RainSplash_00_" + frame;
        }
        if ((tex = Texture.getSharedTexture((String)rainsplashCache[frame])) == null) {
            return;
        }
        if (bRandomXY) {
            this.splashX = Rand.Next((float)0.1f, (float)0.9f);
            this.splashY = Rand.Next((float)0.1f, (float)0.9f);
        }
        float sx = IsoUtils.XToScreen((float)((float)this.x + this.splashX), (float)((float)this.y + this.splashY), (float)this.z, (int)0) - IsoCamera.frameState.offX;
        float sy = IsoUtils.YToScreen((float)((float)this.x + this.splashX), (float)((float)this.y + this.splashY), (float)this.z, (int)0) - IsoCamera.frameState.offY;
        sx -= (float)(tex.getWidth() / 2 * Core.tileScale);
        sy -= (float)(tex.getHeight() / 2 * Core.tileScale);
        if (PerformanceSettings.fboRenderChunk) {
            TextureDraw.nextZ = IsoDepthHelper.getSquareDepthData((int)PZMath.fastfloor((float)IsoCamera.frameState.camCharacterX), (int)PZMath.fastfloor((float)IsoCamera.frameState.camCharacterY), (float)((float)this.x + this.splashX + 0.1f), (float)((float)this.y + this.splashY + 0.1f), (float)((float)this.z)).depthStart * 2.0f - 1.0f;
            SpriteRenderer.instance.StartShader(0, playerIndex);
            IndieGL.enableDepthTest();
            IndieGL.glDepthFunc((int)515);
            IndieGL.glDepthMask((boolean)false);
        }
        IndieGL.glBlendFunc((int)770, (int)771);
        float alpha = 0.6f * (this.getCell().rainIntensity > 0 ? 1.0f : RainManager.rainIntensity);
        float shaderMod = 1.0f;
        SpriteRenderer.instance.render(tex, sx, sy, (float)(tex.getWidth() * Core.tileScale), (float)(tex.getHeight() * Core.tileScale), 0.8f * lightInfo.r, 0.9f * lightInfo.g, 1.0f * lightInfo.b, alpha * 1.0f, null);
    }

    public void renderFishSplash(int playerIndex, ColorInfo lightInfo) {
        Texture tex;
        if (this.isCouldSee(playerIndex) && this.waterSplashData.isSplashNow() && (tex = this.waterSplashData.getTexture()) != null) {
            float texWidth = (float)tex.getWidth() * this.waterSplashData.size;
            float texHeight = (float)tex.getHeight() * this.waterSplashData.size;
            float sx = IsoUtils.XToScreen((float)((float)this.x + this.waterSplashData.dx), (float)((float)this.y + this.waterSplashData.dy), (float)0.0f, (int)0) - IsoCamera.frameState.offX;
            float sy = IsoUtils.YToScreen((float)((float)this.x + this.waterSplashData.dx), (float)((float)this.y + this.waterSplashData.dy), (float)0.0f, (int)0) - IsoCamera.frameState.offY;
            if (PerformanceSettings.fboRenderChunk) {
                sx = IsoUtils.XToScreen((float)((float)this.x + 0.5f), (float)((float)this.y + 0.5f), (float)this.z, (int)0) - IsoCamera.frameState.offX - texWidth / 2.0f;
                sy = IsoUtils.YToScreen((float)((float)this.x + 0.5f), (float)((float)this.y + 0.5f), (float)this.z, (int)0) - IsoCamera.frameState.offY - texHeight / 2.0f;
                TextureDraw.nextZ = (IsoDepthHelper.getSquareDepthData((int)PZMath.fastfloor((float)IsoCamera.frameState.camCharacterX), (int)PZMath.fastfloor((float)IsoCamera.frameState.camCharacterY), (float)((float)this.x + 0.99f), (float)((float)this.y + 0.99f), (float)((float)this.z)).depthStart + 0.001f) * 2.0f - 1.0f;
                SpriteRenderer.instance.StartShader(0, playerIndex);
                IndieGL.enableDepthTest();
                IndieGL.glDepthFunc((int)515);
                IndieGL.glDepthMask((boolean)false);
            }
            float alpha = 1.0f;
            float shaderMod = 1.0f;
            SpriteRenderer.instance.render(tex, sx += IsoCamera.cameras[playerIndex].fixJigglyModelsX * IsoCamera.frameState.zoom, sy += IsoCamera.cameras[playerIndex].fixJigglyModelsY * IsoCamera.frameState.zoom, texWidth, texHeight, 0.8f * lightInfo.r, 0.9f * lightInfo.g, lightInfo.b, 1.0f, null);
            this.waterSplashData.update();
        }
    }

    public boolean isSpriteOnSouthOrEastWall(IsoObject obj) {
        if (obj instanceof IsoBarricade) {
            return obj.getDir() == IsoDirections.S || obj.getDir() == IsoDirections.E;
        }
        if (obj instanceof IsoCurtain) {
            IsoCurtain curtain = (IsoCurtain)obj;
            return curtain.getType() == IsoObjectType.curtainS || curtain.getType() == IsoObjectType.curtainE;
        }
        PropertyContainer properties = obj.getProperties();
        return properties != null && (properties.has(IsoFlagType.attachedE) || properties.has(IsoFlagType.attachedS));
    }

    public void RenderOpenDoorOnly() {
        int numObjects = this.objects.size();
        IsoObject[] objectArray = (IsoObject[])this.objects.getElements();
        try {
            boolean start = false;
            int end = numObjects - 1;
            for (int n = 0; n <= end; ++n) {
                IsoObject obj = objectArray[n];
                if (obj.sprite == null || !obj.sprite.getProperties().has(IsoFlagType.attachedN) && !obj.sprite.getProperties().has(IsoFlagType.attachedW)) continue;
                obj.renderFxMask(this.x, this.y, this.z, false);
            }
        }
        catch (Exception ex) {
            ExceptionLogger.logException((Throwable)ex);
        }
    }

    public boolean RenderMinusFloorFxMask(int maxZ, boolean doSE, boolean vegitationRender) {
        boolean hasSE = false;
        int numObjects = this.objects.size();
        IsoObject[] objectArray = (IsoObject[])this.objects.getElements();
        long currentTimeMillis = System.currentTimeMillis();
        try {
            int start = doSE ? numObjects - 1 : 0;
            int end = doSE ? 0 : numObjects - 1;
            int n = start;
            while (doSE ? n >= end : n <= end) {
                IsoObject obj = objectArray[n];
                if (obj.sprite != null) {
                    boolean bGrassEtc;
                    boolean bDoIt = true;
                    IsoObjectType t = obj.sprite.getTileType();
                    if (obj.sprite.solidfloor || obj.sprite.renderLayer == 1) {
                        bDoIt = false;
                    }
                    if (this.z >= maxZ && !obj.sprite.alwaysDraw) {
                        bDoIt = false;
                    }
                    boolean bl = bGrassEtc = obj.sprite.isBush || obj.sprite.canBeRemoved || obj.sprite.attachedFloor;
                    if ((!vegitationRender || bGrassEtc && this.flattenGrassEtc) && (vegitationRender || !bGrassEtc || !this.flattenGrassEtc)) {
                        if ((t == IsoObjectType.WestRoofB || t == IsoObjectType.WestRoofM || t == IsoObjectType.WestRoofT) && this.z == maxZ - 1 && this.z == PZMath.fastfloor((float)IsoCamera.getCameraCharacterZ())) {
                            bDoIt = false;
                        }
                        if (this.isSpriteOnSouthOrEastWall(obj)) {
                            if (!doSE) {
                                bDoIt = false;
                            }
                            hasSE = true;
                        } else if (doSE) {
                            bDoIt = false;
                        }
                        if (bDoIt) {
                            if (obj.sprite.cutW || obj.sprite.cutN) {
                                int cutawayE;
                                int playerIndex = IsoCamera.frameState.playerIndex;
                                boolean cutN = obj.sprite.cutN;
                                boolean cutW = obj.sprite.cutW;
                                IsoGridSquare squareN = this.nav[IsoDirections.N.ordinal()];
                                IsoGridSquare squareS = this.nav[IsoDirections.S.ordinal()];
                                IsoGridSquare squareW = this.nav[IsoDirections.W.ordinal()];
                                IsoGridSquare squareE = this.nav[IsoDirections.E.ordinal()];
                                int cutawaySelf = this.getPlayerCutawayFlag(playerIndex, currentTimeMillis);
                                int cutawayN = squareN == null ? 0 : squareN.getPlayerCutawayFlag(playerIndex, currentTimeMillis);
                                int cutawayS = squareS == null ? 0 : squareS.getPlayerCutawayFlag(playerIndex, currentTimeMillis);
                                int cutawayW = squareW == null ? 0 : squareW.getPlayerCutawayFlag(playerIndex, currentTimeMillis);
                                int n2 = cutawayE = squareE == null ? 0 : squareE.getPlayerCutawayFlag(playerIndex, currentTimeMillis);
                                IsoDirections dir = cutN && cutW ? IsoDirections.NW : (cutN ? IsoDirections.N : (cutW ? IsoDirections.W : IsoDirections.W));
                                this.DoCutawayShaderSprite(obj.sprite, dir, cutawaySelf, cutawayN, cutawayS, cutawayW, cutawayE);
                            } else {
                                obj.renderFxMask(this.x, this.y, this.z, false);
                            }
                        }
                    }
                }
                n += doSE ? -1 : 1;
            }
        }
        catch (Exception ex) {
            ExceptionLogger.logException((Throwable)ex);
        }
        return hasSE;
    }

    public boolean isWindowOrWindowFrame(IsoObject obj, boolean north) {
        if (obj == null || obj.sprite == null) {
            return false;
        }
        if (north && obj.sprite.getProperties().has(IsoFlagType.windowN)) {
            return true;
        }
        if (!north && obj.sprite.getProperties().has(IsoFlagType.windowW)) {
            return true;
        }
        IsoThumpable thumpable = (IsoThumpable)Type.tryCastTo((Object)obj, IsoThumpable.class);
        if (thumpable != null && thumpable.isWindow()) {
            return north == thumpable.getNorth();
        }
        if (obj instanceof IsoWindowFrame) {
            IsoWindowFrame windowFrame = (IsoWindowFrame)obj;
            return windowFrame.getNorth() == north;
        }
        return false;
    }

    /*
     * Unable to fully structure code
     */
    public boolean renderMinusFloor(int maxZ, boolean doSE, boolean vegitationRender, int cutawaySelf, int cutawayN, int cutawayS, int cutawayW, int cutawayE, Shader wallRenderShader) {
        if (!DebugOptions.instance.terrain.renderTiles.isoGridSquare.renderMinusFloor.getValue()) {
            return false;
        }
        IsoGridSquare.setBlendFunc();
        stenciled = 0;
        IsoGridSquare.isOnScreenLast = this.IsOnScreen();
        playerIndex = IsoCamera.frameState.playerIndex;
        camCharacterSquare = IsoCamera.frameState.camCharacterSquare;
        lightInfo = this.lightInfo[playerIndex];
        bCouldSee = this.lighting[playerIndex].bCouldSee();
        darkMulti = this.lighting[playerIndex].darkMulti();
        playerOccluderSqr = IsoWorld.instance.currentCell.CanBuildingSquareOccludePlayer(this, playerIndex);
        lightInfo.a = 1.0f;
        IsoGridSquare.defColorInfo.r = 1.0f;
        IsoGridSquare.defColorInfo.g = 1.0f;
        IsoGridSquare.defColorInfo.b = 1.0f;
        IsoGridSquare.defColorInfo.a = 1.0f;
        if (Core.debug && DebugOptions.instance.debugDrawSkipWorldShading.getValue()) {
            lightInfo = IsoGridSquare.defColorInfo;
        }
        sx = this.cachedScreenX - IsoCamera.frameState.offX;
        sy = this.cachedScreenY - IsoCamera.frameState.offY;
        cell = this.getCell();
        bInStencilRect = cell.isInStencil(sx, sy);
        hasSE = false;
        numObjects = this.objects.size();
        objectArray = (IsoObject[])this.objects.getElements();
        IsoGridSquare.tempWorldInventoryObjects.clear();
        start = doSE != false ? numObjects - 1 : 0;
        end = doSE != false ? 0 : numObjects - 1;
        bHasSeenDoorN = false;
        bHasSeenDoorW = false;
        bHasSeenWindowN = false;
        bHasSeenWindowW = false;
        if (!doSE) {
            for (n = start; n <= end; ++n) {
                obj = objectArray[n];
                if (this.isWindowOrWindowFrame(obj, true) && (cutawaySelf & 1) != 0) {
                    toNorth = this.nav[IsoDirections.N.ordinal()];
                    v0 = bHasSeenWindowN = bCouldSee != false || toNorth != null && toNorth.isCouldSee(playerIndex) != false;
                }
                if (this.isWindowOrWindowFrame(obj, false) && (cutawaySelf & 2) != 0) {
                    toWest = this.nav[IsoDirections.W.ordinal()];
                    v1 = bHasSeenWindowW = bCouldSee != false || toWest != null && toWest.isCouldSee(playerIndex) != false;
                }
                if (obj.sprite != null && (obj.sprite.getTileType() == IsoObjectType.doorFrN || obj.sprite.getTileType() == IsoObjectType.doorN) && (cutawaySelf & 1) != 0) {
                    toNorth = this.nav[IsoDirections.N.ordinal()];
                    v2 = bHasSeenDoorN = bCouldSee != false || toNorth != null && toNorth.isCouldSee(playerIndex) != false;
                }
                if (obj.sprite == null || obj.sprite.getTileType() != IsoObjectType.doorFrW && obj.sprite.getTileType() != IsoObjectType.doorW || (cutawaySelf & 2) == 0) continue;
                toWest = this.nav[IsoDirections.W.ordinal()];
                bHasSeenDoorW = bCouldSee != false || toWest != null && toWest.isCouldSee(playerIndex) != false;
            }
        }
        playerRoomID = IsoWorld.instance.currentCell.GetEffectivePlayerRoomId();
        IsoGridSquare.wallCutawayN = false;
        IsoGridSquare.wallCutawayW = false;
        n = start;
        while (doSE != false ? n >= end : n <= end) {
            block75: {
                obj = objectArray[n];
                bDoIt = true;
                t = IsoObjectType.MAX;
                if (obj.sprite != null) {
                    t = obj.sprite.getTileType();
                }
                IsoGridSquare.circleStencil = false;
                if (obj.sprite != null && (obj.sprite.solidfloor || obj.sprite.renderLayer == 1)) {
                    bDoIt = false;
                }
                if (obj instanceof IsoFire) {
                    v3 = bDoIt = vegitationRender == false;
                }
                if (!(this.z < maxZ || obj.sprite != null && obj.sprite.alwaysDraw)) {
                    bDoIt = false;
                }
                v4 = bGrassEtc = obj.sprite != null && (obj.sprite.isBush != false || obj.sprite.canBeRemoved != false || obj.sprite.attachedFloor != false);
                if (vegitationRender && (!bGrassEtc || !this.flattenGrassEtc) || !vegitationRender && bGrassEtc && this.flattenGrassEtc) break block75;
                if (obj.sprite != null && (t == IsoObjectType.WestRoofB || t == IsoObjectType.WestRoofM || t == IsoObjectType.WestRoofT) && this.z == maxZ - 1 && this.z == PZMath.fastfloor((float)IsoCamera.getCameraCharacterZ())) {
                    bDoIt = false;
                }
                isWestDoorOrWall = t == IsoObjectType.doorFrW || t == IsoObjectType.doorW || obj.sprite != null && obj.sprite.cutW != false;
                v5 = isNorthDoorOrWall = t == IsoObjectType.doorFrN || t == IsoObjectType.doorN || obj.sprite != null && obj.sprite.cutN != false;
                if (obj instanceof IsoDoor && (isoDoor = (IsoDoor)obj).isOpen()) ** GOTO lbl-1000
                if (obj instanceof IsoThumpable) {
                    isoThumpable = (IsoThumpable)obj;
                    ** if (!isoThumpable.open) goto lbl-1000
                }
                ** GOTO lbl-1000
lbl-1000:
                // 2 sources

                {
                    v6 = true;
                    ** GOTO lbl80
                }
lbl-1000:
                // 2 sources

                {
                    v6 = false;
                }
lbl80:
                // 2 sources

                isOpenDoor = v6;
                isContainer = obj.container != null;
                v7 = isPlumbed = obj.sprite != null && obj.sprite.getProperties().has(IsoFlagType.waterPiped) != false;
                if (!(obj.sprite == null || t != IsoObjectType.MAX || obj instanceof IsoDoor || obj instanceof IsoWindow || isContainer || isPlumbed)) {
                    if (!isWestDoorOrWall && obj.sprite.getProperties().has(IsoFlagType.attachedW) && (playerOccluderSqr || (cutawaySelf & 2) != 0)) {
                        bDoIt = IsoGridSquare.wallCutawayW == false;
                    } else if (!isNorthDoorOrWall && obj.sprite.getProperties().has(IsoFlagType.attachedN) && (playerOccluderSqr || (cutawaySelf & 1) != 0)) {
                        v8 = bDoIt = IsoGridSquare.wallCutawayN == false;
                    }
                }
                if (obj.sprite != null && !obj.sprite.solidfloor && IsoPlayer.getInstance().isClimbing()) {
                    bDoIt = true;
                }
                if (this.isSpriteOnSouthOrEastWall(obj)) {
                    if (!doSE) {
                        bDoIt = false;
                    }
                    hasSE = true;
                } else if (doSE) {
                    bDoIt = false;
                }
                if (PerformanceSettings.fboRenderChunk) {
                    renderLayer = obj.getRenderInfo((int)playerIndex).layer;
                    v9 = bTranslucent = renderLayer == ObjectRenderLayer.Translucent || renderLayer == ObjectRenderLayer.TranslucentSE;
                    if (FBORenderCell.instance.renderTranslucentOnly != bTranslucent) {
                        bDoIt = false;
                    }
                }
                if (bDoIt) {
                    IndieGL.glAlphaFunc((int)516, (float)0.0f);
                    obj.alphaForced = false;
                    if (isOpenDoor) {
                        obj.setTargetAlpha(playerIndex, 0.6f);
                        obj.setAlpha(playerIndex, 0.6f);
                    }
                    if (obj.sprite != null && (isWestDoorOrWall || isNorthDoorOrWall)) {
                        if (DebugOptions.instance.terrain.renderTiles.isoGridSquare.doorsAndWalls.getValue()) {
                            IsoGridSquare.circleStencil = true;
                            if (camCharacterSquare != null && this.getRoomID() != -1L && playerRoomID == -1L && playerOccluderSqr) {
                                obj.setTargetAlpha(playerIndex, 0.5f);
                                obj.setAlpha(playerIndex, 0.5f);
                            } else if (this.getRoomID() != playerRoomID && !bCouldSee && (obj.getProperties().has(IsoFlagType.transparentN) || obj.getProperties().has(IsoFlagType.transparentW)) && obj.getSpriteName() != null && obj.getSpriteName().contains("police")) {
                                obj.setTargetAlpha(playerIndex, 0.0f);
                                obj.setAlpha(playerIndex, 0.0f);
                            } else if (!isOpenDoor) {
                                obj.setTargetAlpha(playerIndex, 1.0f);
                                obj.setAlpha(playerIndex, 1.0f);
                            }
                            obj.alphaForced = true;
                            if (obj.sprite.cutW && obj.sprite.cutN) {
                                stenciled = this.DoWallLightingNW(obj, stenciled, cutawaySelf, cutawayN, cutawayS, cutawayW, cutawayE, bHasSeenDoorN, bHasSeenDoorW, bHasSeenWindowN, bHasSeenWindowW, wallRenderShader);
                            } else if (obj.sprite.getTileType() == IsoObjectType.doorFrW || t == IsoObjectType.doorW || obj.sprite.cutW) {
                                stenciled = this.DoWallLightingW(obj, stenciled, cutawaySelf, cutawayN, cutawayS, cutawayW, cutawayE, bHasSeenDoorW, bHasSeenWindowW, wallRenderShader);
                            } else if (t == IsoObjectType.doorFrN || t == IsoObjectType.doorN || obj.sprite.cutN) {
                                stenciled = this.DoWallLightingN(obj, stenciled, cutawaySelf, cutawayN, cutawayS, cutawayW, cutawayE, bHasSeenDoorN, bHasSeenWindowN, wallRenderShader);
                            }
                            if (obj instanceof IsoWindow && obj.getTargetAlpha(playerIndex) < 1.0f) {
                                IsoGridSquare.wallCutawayN |= obj.sprite.cutN;
                                IsoGridSquare.wallCutawayW |= obj.sprite.cutW;
                            }
                        }
                    } else if (DebugOptions.instance.terrain.renderTiles.isoGridSquare.objects.getValue()) {
                        if (this.getRoomID() != -1L && this.getRoomID() != playerRoomID && IsoPlayer.players[playerIndex].isSeatedInVehicle() && IsoPlayer.players[playerIndex].getVehicle().getCurrentSpeedKmHour() >= 50.0f) break;
                        pl = IsoPlayer.players[playerIndex];
                        withinRoofFadeDist = IsoUtils.DistanceToSquared((float)pl.getX(), (float)pl.getY(), (float)((float)this.x - 1.5f - ((float)this.z - pl.getZ() * 3.0f)), (float)((float)this.y - 1.5f - ((float)this.z - pl.getZ() * 3.0f))) <= 30.0f;
                        sqAtZero = IsoWorld.instance.currentCell.getGridSquare(this.x, this.y, 0);
                        sqZeroN = null;
                        sqZeroW = null;
                        sqZeroHasWallN = false;
                        sqZeroHasWallW = false;
                        if (sqAtZero != null) {
                            sqZeroN = sqAtZero.nav[IsoDirections.N.ordinal()];
                            sqZeroW = sqAtZero.nav[IsoDirections.W.ordinal()];
                            sqZeroHasWallN = sqAtZero.getWall(true) != null || sqAtZero.getDoor(true) != null || sqAtZero.getWindow(true) != null;
                            sqZeroHasWallW = sqAtZero.getWall(false) != null || sqAtZero.getDoor(false) != null || sqAtZero.getWindow(false) != null;
                        }
                        v10 = roofFadeDist = IsoUtils.DistanceToSquared((float)pl.getX(), (float)pl.getY(), (float)((float)this.x - 1.5f - ((float)this.z - pl.getZ() * 3.0f)), (float)((float)this.y - 1.5f - ((float)this.z - pl.getZ() * 3.0f))) <= 30.0f;
                        if ((t == IsoObjectType.WestRoofB || t == IsoObjectType.WestRoofM || t == IsoObjectType.WestRoofT) && (this.getRoomID() == -1L && ((cutawayE != 0 || cutawayS != 0) && (pl.getX() < (float)this.x && pl.getY() < (float)this.y || pl.getZ() < (float)this.z) || sqAtZero != null && sqAtZero.getRoomID() == -1L && roofFadeDist != false && pl.getZ() < (float)(this.z + 1)) || (this.getRoomID() != -1L || playerRoomID != -1L) && roofFadeDist && pl.getZ() < (float)(this.z + 1))) {
                            sqZeroN = sqAtZero.nav[IsoDirections.N.ordinal()];
                            sqZeroW = sqAtZero.nav[IsoDirections.W.ordinal()];
                            sqZeroHasWallN = sqAtZero.getWall(true) != null || sqAtZero.getDoor(true) != null || sqAtZero.getWindow(true) != null;
                            v11 = sqZeroHasWallW = sqAtZero.getWall(false) != null || sqAtZero.getDoor(false) != null || sqAtZero.getWindow(false) != null;
                        }
                        if ((t == IsoObjectType.WestRoofB || t == IsoObjectType.WestRoofM || t == IsoObjectType.WestRoofT) && (this.getRoomID() == -1L && ((cutawayE != 0 || cutawayS != 0) && (pl.getX() < (float)this.x && pl.getY() < (float)this.y || pl.getZ() < (float)this.z) || sqAtZero != null && sqAtZero.getRoomID() == -1L && (withinRoofFadeDist != false || playerRoomID != -1L) && pl.getZ() < (float)(this.z + 1) && (sqZeroHasWallN != false && sqZeroN != null && sqZeroN.getRoomID() == -1L || sqZeroHasWallW != false && sqZeroW != null && sqZeroW.getRoomID() == -1L || sqZeroHasWallN == false && sqZeroHasWallW == false)) || this.getRoomID() != -1L && playerRoomID != -1L && withinRoofFadeDist && pl.getZ() < (float)(this.z + 1))) {
                            obj.setTargetAlpha(playerIndex, 0.0f);
                        } else if (camCharacterSquare != null && !bCouldSee && this.getRoomID() != playerRoomID && darkMulti < 0.5f) {
                            if (obj.getProperties() != null && obj.getProperties().has("forceFade")) {
                                obj.setTargetAlpha(playerIndex, 0.0f);
                            } else {
                                obj.setTargetAlpha(playerIndex, darkMulti * 2.0f);
                            }
                        } else {
                            if (!isOpenDoor) {
                                obj.setTargetAlpha(playerIndex, 1.0f);
                            }
                            if (IsoPlayer.getInstance() != null && obj.getProperties() != null && (obj.getProperties().has(IsoFlagType.solid) || obj.getProperties().has(IsoFlagType.solidtrans) || obj.getProperties().has(IsoFlagType.attachedCeiling) || obj.getSprite().getProperties().has(IsoFlagType.attachedE) || obj.getSprite().getProperties().has(IsoFlagType.attachedS)) || t.index() > 2 && t.index() < 9 && IsoCamera.frameState.camCharacterZ <= obj.getZ()) {
                                transRange = 3;
                                transAlpha = 0.75f;
                                if (t.index() > 2 && t.index() < 9 || obj.getSprite().getProperties().has(IsoFlagType.attachedE) || obj.getSprite().getProperties().has(IsoFlagType.attachedS) || obj.getProperties().has(IsoFlagType.attachedCeiling)) {
                                    transRange = 4;
                                    if (t.index() > 2 && t.index() < 9) {
                                        transAlpha = 0.5f;
                                    }
                                }
                                if (obj.sprite.solid || obj.sprite.solidTrans) {
                                    transRange = 5;
                                    transAlpha = 0.25f;
                                }
                                dx = this.getX() - PZMath.fastfloor((float)IsoPlayer.getInstance().getX());
                                dy = this.getY() - PZMath.fastfloor((float)IsoPlayer.getInstance().getY());
                                if (dx >= 0 && dx < transRange && dy >= 0 && dy < transRange || dy >= 0 && dy < transRange && dx >= 0 && dx < transRange) {
                                    obj.setTargetAlpha(playerIndex, transAlpha);
                                }
                                if ((nearestVisibleZombie = IsoCell.getInstance().getNearestVisibleZombie(playerIndex)) != null && nearestVisibleZombie.getCurrentSquare() != null && nearestVisibleZombie.getCurrentSquare().isCanSee(playerIndex)) {
                                    zx = this.getX() - PZMath.fastfloor((float)nearestVisibleZombie.getX());
                                    zy = this.getY() - PZMath.fastfloor((float)nearestVisibleZombie.getY());
                                    if (zx > 0 && zx < transRange && zy >= 0 && zy < transRange || zy > 0 && zy < transRange && zx >= 0 && zx < transRange) {
                                        obj.setTargetAlpha(playerIndex, transAlpha);
                                    }
                                }
                            }
                        }
                        if (obj instanceof IsoWindow) {
                            w = (IsoWindow)obj;
                            if (obj.getTargetAlpha(playerIndex) < 1.0E-4f && (oppositeSq = w.getOppositeSquare()) != null && oppositeSq != this && oppositeSq.lighting[playerIndex].bSeen()) {
                                obj.setTargetAlpha(playerIndex, oppositeSq.lighting[playerIndex].darkMulti() * 2.0f);
                            }
                            if (obj.getTargetAlpha(playerIndex) > 0.4f && cutawaySelf != 0 && (cutawayE != 0 && obj.sprite.getProperties().has(IsoFlagType.windowN) || cutawayS != 0 && obj.sprite.getProperties().has(IsoFlagType.windowW))) {
                                maxOpacity = 0.4f;
                                minOpacity = 0.1f;
                                player = IsoPlayer.players[playerIndex];
                                if (player != null) {
                                    maxFadeDistance = 5.0f;
                                    distanceSquared = Math.abs(player.getX() - (float)this.x) * Math.abs(player.getX() - (float)this.x) + Math.abs(player.getY() - (float)this.y) * Math.abs(player.getY() - (float)this.y);
                                    fadeAmount = 0.4f * (float)(1.0 - Math.sqrt(distanceSquared / 5.0f));
                                    obj.setTargetAlpha(playerIndex, Math.max(fadeAmount, 0.1f));
                                } else {
                                    obj.setTargetAlpha(playerIndex, 0.1f);
                                }
                                if (cutawayE != 0) {
                                    IsoGridSquare.wallCutawayN = true;
                                } else {
                                    IsoGridSquare.wallCutawayW = true;
                                }
                            }
                        }
                        if (obj instanceof IsoTree) {
                            isoTree = (IsoTree)obj;
                            if (bInStencilRect && this.x >= PZMath.fastfloor((float)IsoCamera.frameState.camCharacterX) && this.y >= PZMath.fastfloor((float)IsoCamera.frameState.camCharacterY) && camCharacterSquare != null && camCharacterSquare.has(IsoFlagType.exterior)) {
                                isoTree.renderFlag = true;
                                obj.setTargetAlpha(playerIndex, Math.min(0.99f, obj.getTargetAlpha(playerIndex)));
                            } else {
                                isoTree.renderFlag = false;
                            }
                        }
                        if (obj instanceof IsoWorldInventoryObject) {
                            worldObj = (IsoWorldInventoryObject)obj;
                            IsoGridSquare.tempWorldInventoryObjects.add((Object)worldObj);
                        } else {
                            if (!PerformanceSettings.fboRenderChunk && obj.getAlpha(playerIndex) < 1.0f) {
                                IndieGL.glBlendFunc((int)770, (int)771);
                            }
                            obj.render(this.x, this.y, this.z, lightInfo, true, false, null);
                        }
                    }
                    if (!PerformanceSettings.fboRenderChunk && obj.isHighlightRenderOnce(playerIndex)) {
                        obj.setHighlighted(playerIndex, false, false);
                    }
                }
            }
            n += doSE != false ? -1 : 1;
        }
        Arrays.sort((IsoWorldInventoryObject[])IsoGridSquare.tempWorldInventoryObjects.getElements(), 0, IsoGridSquare.tempWorldInventoryObjects.size(), (Comparator)LambdaMetafactory.metafactory(null, null, null, (Ljava/lang/Object;Ljava/lang/Object;)I, lambda$renderMinusFloor$0(zombie.iso.objects.IsoWorldInventoryObject zombie.iso.objects.IsoWorldInventoryObject ), (Lzombie/iso/objects/IsoWorldInventoryObject;Lzombie/iso/objects/IsoWorldInventoryObject;)I)());
        for (i = 0; i < IsoGridSquare.tempWorldInventoryObjects.size(); ++i) {
            worldObj = (IsoWorldInventoryObject)IsoGridSquare.tempWorldInventoryObjects.get(i);
            worldObj.render(this.x, this.y, this.z, lightInfo, true, false, null);
        }
        return hasSE;
    }

    void RereouteWallMaskTo(IsoObject obj) {
        for (int n = 0; n < this.objects.size(); ++n) {
            IsoObject objTest = (IsoObject)this.objects.get(n);
            if (!objTest.sprite.getProperties().has(IsoFlagType.collideW) && !objTest.sprite.getProperties().has(IsoFlagType.collideN)) continue;
            objTest.rerouteMask = obj;
        }
    }

    void setBlockedGridPointers(GetSquare getter) {
        this.w = getter.getGridSquare(this.x - 1, this.y, this.z);
        this.e = getter.getGridSquare(this.x + 1, this.y, this.z);
        this.s = getter.getGridSquare(this.x, this.y + 1, this.z);
        this.n = getter.getGridSquare(this.x, this.y - 1, this.z);
        this.ne = getter.getGridSquare(this.x + 1, this.y - 1, this.z);
        this.nw = getter.getGridSquare(this.x - 1, this.y - 1, this.z);
        this.se = getter.getGridSquare(this.x + 1, this.y + 1, this.z);
        this.sw = getter.getGridSquare(this.x - 1, this.y + 1, this.z);
        this.u = getter.getGridSquare(this.x, this.y, this.z + 1);
        this.d = getter.getGridSquare(this.x, this.y, this.z - 1);
        if (this.u != null && (this.u.properties.has(IsoFlagType.solidfloor) || this.u.properties.has(IsoFlagType.solid))) {
            this.u = null;
        }
        if (this.d != null && (this.properties.has(IsoFlagType.solidfloor) || this.properties.has(IsoFlagType.solid))) {
            this.d = null;
        }
        if (this.s != null && this.testPathFindAdjacent(null, this.s.x - this.x, this.s.y - this.y, this.s.z - this.z, getter)) {
            this.s = null;
        }
        if (this.w != null && this.testPathFindAdjacent(null, this.w.x - this.x, this.w.y - this.y, this.w.z - this.z, getter)) {
            this.w = null;
        }
        if (this.n != null && this.testPathFindAdjacent(null, this.n.x - this.x, this.n.y - this.y, this.n.z - this.z, getter)) {
            this.n = null;
        }
        if (this.e != null && this.testPathFindAdjacent(null, this.e.x - this.x, this.e.y - this.y, this.e.z - this.z, getter)) {
            this.e = null;
        }
        if (this.sw != null && this.testPathFindAdjacent(null, this.sw.x - this.x, this.sw.y - this.y, this.sw.z - this.z, getter)) {
            this.sw = null;
        }
        if (this.se != null && this.testPathFindAdjacent(null, this.se.x - this.x, this.se.y - this.y, this.se.z - this.z, getter)) {
            this.se = null;
        }
        if (this.nw != null && this.testPathFindAdjacent(null, this.nw.x - this.x, this.nw.y - this.y, this.nw.z - this.z, getter)) {
            this.nw = null;
        }
        if (this.ne != null && this.testPathFindAdjacent(null, this.ne.x - this.x, this.ne.y - this.y, this.ne.z - this.z, getter)) {
            this.ne = null;
        }
    }

    public IsoObject getContainerItem(String type) {
        int numObjects = this.getObjects().size();
        IsoObject[] objectArray = (IsoObject[])this.getObjects().getElements();
        for (int i = 0; i < numObjects; ++i) {
            IsoObject o = objectArray[i];
            if (o.getContainer() == null || !type.equals(o.getContainer().getType())) continue;
            return o;
        }
        return null;
    }

    @Deprecated
    public void StartFire() {
    }

    public int getHourLastSeen() {
        return this.hourLastSeen;
    }

    public float getHoursSinceLastSeen() {
        return (float)GameTime.instance.getWorldAgeHours() - (float)this.hourLastSeen;
    }

    public void CalcVisibility(int playerIndex, IsoGameCharacter isoGameCharacter, VisibilityData visibilityData) {
        ILighting lighting = this.lighting[playerIndex];
        lighting.bCanSee(false);
        lighting.bCouldSee(false);
        if (!GameServer.server && isoGameCharacter.isDead() && isoGameCharacter.reanimatedCorpse == null) {
            lighting.bSeen(true);
            lighting.bCanSee(true);
            lighting.bCouldSee(true);
            return;
        }
        IsoGameCharacter.LightInfo lightInfo = isoGameCharacter.getLightInfo2();
        IsoGridSquare currentSquare = lightInfo.square;
        if (currentSquare == null) {
            return;
        }
        IsoChunk chk = this.getChunk();
        if (chk == null) {
            return;
        }
        IsoGridSquare.tempo.x = (float)this.x + 0.5f;
        IsoGridSquare.tempo.y = (float)this.y + 0.5f;
        IsoGridSquare.tempo2.x = lightInfo.x;
        IsoGridSquare.tempo2.y = lightInfo.y;
        IsoGridSquare.tempo2.x -= IsoGridSquare.tempo.x;
        IsoGridSquare.tempo2.y -= IsoGridSquare.tempo.y;
        Vector2 dir = tempo;
        float dist = tempo2.getLength();
        tempo2.normalize();
        if (isoGameCharacter instanceof IsoSurvivor) {
            isoGameCharacter.setForwardDirection(dir);
            lightInfo.angleX = dir.x;
            lightInfo.angleY = dir.y;
        }
        dir.x = lightInfo.angleX;
        dir.y = lightInfo.angleY;
        dir.normalize();
        float dot = tempo2.dot(dir);
        if (currentSquare == this) {
            dot = -1.0f;
        }
        if (!GameServer.server) {
            float fatigue = visibilityData.getFatigue();
            float noiseDistance = visibilityData.getNoiseDistance();
            if (dist < noiseDistance * (1.0f - fatigue) && !isoGameCharacter.hasTrait(CharacterTrait.DEAF)) {
                dot = -1.0f;
            }
        }
        LosUtil.TestResults test = LosUtil.lineClearCached((IsoCell)this.getCell(), (int)this.x, (int)this.y, (int)this.z, (int)PZMath.fastfloor((float)lightInfo.x), (int)PZMath.fastfloor((float)lightInfo.y), (int)PZMath.fastfloor((float)lightInfo.z), (boolean)false, (int)playerIndex);
        float cone = visibilityData.getCone();
        if (dot > cone || test == LosUtil.TestResults.Blocked) {
            lighting.bCouldSee(test != LosUtil.TestResults.Blocked);
            if (!GameServer.server) {
                if (lighting.bSeen()) {
                    float amb = visibilityData.getBaseAmbient();
                    amb = !lighting.bCouldSee() ? (amb *= 0.5f) : (amb *= 0.94f);
                    if (this.room == null && currentSquare.getRoom() == null) {
                        lighting.targetDarkMulti(amb);
                    } else if (this.room != null && currentSquare.getRoom() != null && this.room.building == currentSquare.getRoom().building) {
                        if (this.room != currentSquare.getRoom() && !lighting.bCouldSee()) {
                            lighting.targetDarkMulti(0.0f);
                        } else {
                            lighting.targetDarkMulti(amb);
                        }
                    } else if (this.room == null) {
                        lighting.targetDarkMulti(amb / 2.0f);
                    } else if (lighting.lampostTotalR() + lighting.lampostTotalG() + lighting.lampostTotalB() == 0.0f) {
                        lighting.targetDarkMulti(0.0f);
                    }
                    if (this.room != null) {
                        lighting.targetDarkMulti(lighting.targetDarkMulti() * 0.7f);
                    }
                } else {
                    lighting.targetDarkMulti(0.0f);
                    lighting.darkMulti(0.0f);
                }
            }
        } else {
            lighting.bCouldSee(true);
            if (this.room != null && this.room.def != null) {
                if (!this.room.def.explored) {
                    IsoPlayer isoPlayer;
                    int dist1 = 10;
                    if (lightInfo.square != null && lightInfo.square.getBuilding() == this.room.building) {
                        dist1 = 50;
                    }
                    if (!(GameServer.server && isoGameCharacter instanceof IsoPlayer && (isoPlayer = (IsoPlayer)isoGameCharacter).isGhostMode() || !(IsoUtils.DistanceManhatten((float)lightInfo.x, (float)lightInfo.y, (float)this.x, (float)this.y) < (float)dist1) || this.z != PZMath.fastfloor((float)lightInfo.z))) {
                        if (GameServer.server) {
                            DebugLog.log((DebugType)DebugType.Zombie, (String)("bExplored room=" + this.room.def.id));
                        }
                        this.room.def.explored = true;
                        this.room.onSee();
                        this.room.seen = 0;
                    }
                }
                if (!GameClient.client) {
                    Meta.instance.dealWithSquareSeen(this);
                }
                lighting.bCanSee(true);
                lighting.bSeen(true);
                lighting.targetDarkMulti(1.0f);
            }
        }
        if (dot > cone) {
            lighting.targetDarkMulti(lighting.targetDarkMulti() * 0.85f);
        }
        if (GameServer.server) {
            return;
        }
        for (int i = 0; i < lightInfo.torches.size(); ++i) {
            boolean isTorchDot;
            IsoGameCharacter.TorchInfo torch = (IsoGameCharacter.TorchInfo)lightInfo.torches.get(i);
            IsoGridSquare.tempo2.x = torch.x;
            IsoGridSquare.tempo2.y = torch.y;
            IsoGridSquare.tempo2.x -= (float)this.x + 0.5f;
            IsoGridSquare.tempo2.y -= (float)this.y + 0.5f;
            dist = tempo2.getLength();
            tempo2.normalize();
            dir.x = torch.angleX;
            dir.y = torch.angleY;
            dir.normalize();
            dot = tempo2.dot(dir);
            if (PZMath.fastfloor((float)torch.x) == this.getX() && PZMath.fastfloor((float)torch.y) == this.getY() && PZMath.fastfloor((float)torch.z) == this.getZ()) {
                dot = -1.0f;
            }
            boolean bl = isTorchDot = IsoUtils.DistanceManhatten((float)this.getX(), (float)this.getY(), (float)torch.x, (float)torch.y) < torch.dist && (torch.cone && dot < -torch.dot || dot == -1.0f || !torch.cone && dot < 0.8f);
            if (!(torch.cone && dist < torch.dist) && (torch.cone || !(dist < torch.dist)) || !lighting.bCanSee() || !isTorchDot || this.z != PZMath.fastfloor((float)isoGameCharacter.getZ())) continue;
            float del = dist / torch.dist;
            if (del > 1.0f) {
                del = 1.0f;
            }
            if (del < 0.0f) {
                del = 0.0f;
            }
            lighting.targetDarkMulti(lighting.targetDarkMulti() + torch.strength * (1.0f - del) * 3.0f);
            if (lighting.targetDarkMulti() > 2.5f) {
                lighting.targetDarkMulti(2.5f);
            }
            torchTimer = lightInfo.time;
        }
    }

    private LosUtil.TestResults DoDiagnalCheck(int x, int y, int z, boolean bIgnoreDoors) {
        LosUtil.TestResults res = this.testVisionAdjacent(x, 0, z, false, bIgnoreDoors);
        if (res == LosUtil.TestResults.Blocked) {
            return LosUtil.TestResults.Blocked;
        }
        LosUtil.TestResults res2 = this.testVisionAdjacent(0, y, z, false, bIgnoreDoors);
        if (res2 == LosUtil.TestResults.Blocked) {
            return LosUtil.TestResults.Blocked;
        }
        if (res == LosUtil.TestResults.ClearThroughWindow || res2 == LosUtil.TestResults.ClearThroughWindow) {
            return LosUtil.TestResults.ClearThroughWindow;
        }
        return this.testVisionAdjacent(x, y, z, false, bIgnoreDoors);
    }

    boolean HasNoCharacters() {
        int n;
        for (n = 0; n < this.movingObjects.size(); ++n) {
            if (!(this.movingObjects.get(n) instanceof IsoGameCharacter)) continue;
            return false;
        }
        for (n = 0; n < this.specialObjects.size(); ++n) {
            if (!(this.specialObjects.get(n) instanceof IsoBarricade)) continue;
            return false;
        }
        return true;
    }

    public IsoZombie getZombie() {
        for (int n = 0; n < this.movingObjects.size(); ++n) {
            if (!(this.movingObjects.get(n) instanceof IsoZombie)) continue;
            return (IsoZombie)this.movingObjects.get(n);
        }
        return null;
    }

    public IsoPlayer getPlayer() {
        for (int n = 0; n < this.movingObjects.size(); ++n) {
            if (!(this.movingObjects.get(n) instanceof IsoPlayer)) continue;
            return (IsoPlayer)this.movingObjects.get(n);
        }
        return null;
    }

    public static float getDarkStep() {
        return darkStep;
    }

    public static void setDarkStep(float aDarkStep) {
        darkStep = aDarkStep;
    }

    public static float getRecalcLightTime() {
        return recalcLightTime;
    }

    public static void setRecalcLightTime(float aRecalcLightTime) {
        recalcLightTime = aRecalcLightTime;
        if (PerformanceSettings.fboRenderChunk && aRecalcLightTime < 0.0f) {
            ++Core.dirtyGlobalLightsCount;
        }
    }

    public static int getLightcache() {
        return lightcache;
    }

    public static void setLightcache(int aLightcache) {
        lightcache = aLightcache;
    }

    public boolean isCouldSee(int playerIndex) {
        return this.lighting[playerIndex].bCouldSee();
    }

    public void setCouldSee(int playerIndex, boolean bCouldSee) {
        this.lighting[playerIndex].bCouldSee(bCouldSee);
    }

    public boolean isCanSee(int playerIndex) {
        return this.lighting[playerIndex].bCanSee();
    }

    public void setCanSee(int playerIndex, boolean canSee) {
        this.lighting[playerIndex].bCanSee(canSee);
    }

    public IsoCell getCell() {
        return IsoWorld.instance.currentCell;
    }

    public IsoGridSquare getE() {
        return this.e;
    }

    public void setE(IsoGridSquare e) {
        this.e = e;
    }

    public ArrayList<Float> getLightInfluenceB() {
        return this.lightInfluenceB;
    }

    public void setLightInfluenceB(ArrayList<Float> lightInfluenceB) {
        this.lightInfluenceB = lightInfluenceB;
    }

    public ArrayList<Float> getLightInfluenceG() {
        return this.lightInfluenceG;
    }

    public void setLightInfluenceG(ArrayList<Float> lightInfluenceG) {
        this.lightInfluenceG = lightInfluenceG;
    }

    public ArrayList<Float> getLightInfluenceR() {
        return this.lightInfluenceR;
    }

    public void setLightInfluenceR(ArrayList<Float> lightInfluenceR) {
        this.lightInfluenceR = lightInfluenceR;
    }

    public ArrayList<IsoMovingObject> getStaticMovingObjects() {
        return this.staticMovingObjects;
    }

    public <ObjectType extends IsoMovingObject> List<ObjectType> getStaticMovingObjects(Class<? extends ObjectType> objectType, Predicate<ObjectType> objectFilter) {
        return this.visitStaticMovingObjects(objectType, new ArrayList(), objectFilter, List::add);
    }

    public <ObjectType extends IsoMovingObject> List<ObjectType> getStaticMovingObjectsInNearbySquares(Class<? extends ObjectType> objectType, BiPredicate<IsoGridSquare, IsoGridSquare> squareFilter, Predicate<ObjectType> objectFilter) {
        return this.visitStaticMovingObjectsInNearbySquares(objectType, new ArrayList(), squareFilter, objectFilter, List::add);
    }

    public <Param, ObjectType extends IsoMovingObject> Param visitStaticMovingObjects(Class<? extends ObjectType> objectType, Param param, Predicate<ObjectType> objectFilter, BiConsumer<Param, ObjectType> objectVisitor) {
        for (IsoMovingObject object : this.getStaticMovingObjects()) {
            IsoMovingObject objectOfType = (IsoMovingObject)Type.tryCastTo((Object)object, objectType);
            if (objectOfType == null || !objectFilter.test(objectOfType)) continue;
            objectVisitor.accept(param, objectOfType);
        }
        return param;
    }

    public <Param, ObjectType extends IsoMovingObject> Param visitStaticMovingObjectsInNearbySquares(Class<? extends ObjectType> objectType, Param param, BiPredicate<IsoGridSquare, IsoGridSquare> squareFilter, Predicate<ObjectType> objectFilter, BiConsumer<Param, ObjectType> objectVisitor) {
        return (Param)this.visitNearbySquares(param, squareFilter, (_param, toSquare) -> toSquare.visitStaticMovingObjects(objectType, _param, objectFilter, objectVisitor));
    }

    public ArrayList<IsoMovingObject> getMovingObjects() {
        return this.movingObjects;
    }

    public <Param> Param visitNearbySquares(Param param, BiPredicate<IsoGridSquare, IsoGridSquare> squareFilter, BiConsumer<Param, IsoGridSquare> squareVisitor) {
        IsoCell cell = this.getCell();
        int originX = this.getX();
        int originY = this.getY();
        int originZ = this.getZ();
        for (int x = originX - 1; x <= originX + 1; ++x) {
            for (int y = originY - 1; y <= originY + 1; ++y) {
                IsoGridSquare square = cell.getGridSquare(x, y, originZ);
                if (square == null || !squareFilter.test(this, square)) continue;
                squareVisitor.accept(param, square);
            }
        }
        return param;
    }

    public IsoGridSquare getN() {
        return this.n;
    }

    public void setN(IsoGridSquare n) {
        this.n = n;
    }

    public PZArrayList<IsoObject> getObjects() {
        return this.objects;
    }

    public PropertyContainer getProperties() {
        return this.properties;
    }

    public IsoRoom getRoom() {
        if (this.roomId == -1L) {
            return null;
        }
        return this.room;
    }

    public void setRoom(IsoRoom room) {
        this.room = room;
    }

    public RoomDef getRoomDef() {
        IsoRoom room = this.getRoom();
        return room == null ? null : room.getRoomDef();
    }

    public IsoBuilding getBuilding() {
        IsoRoom room = this.getRoom();
        if (room != null) {
            return room.getBuilding();
        }
        return null;
    }

    public BuildingDef getBuildingDef() {
        IsoBuilding building = this.getBuilding();
        return building == null ? null : building.getDef();
    }

    public IsoGridSquare getS() {
        return this.s;
    }

    public void setS(IsoGridSquare s) {
        this.s = s;
    }

    public ArrayList<IsoObject> getSpecialObjects() {
        return this.specialObjects;
    }

    public IsoGridSquare getW() {
        return this.w;
    }

    public void setW(IsoGridSquare w) {
        this.w = w;
    }

    public float getLampostTotalR() {
        return this.lighting[0].lampostTotalR();
    }

    public void setLampostTotalR(float lampostTotalR) {
        this.lighting[0].lampostTotalR(lampostTotalR);
    }

    public float getLampostTotalG() {
        return this.lighting[0].lampostTotalG();
    }

    public void setLampostTotalG(float lampostTotalG) {
        this.lighting[0].lampostTotalG(lampostTotalG);
    }

    public float getLampostTotalB() {
        return this.lighting[0].lampostTotalB();
    }

    public void setLampostTotalB(float lampostTotalB) {
        this.lighting[0].lampostTotalB(lampostTotalB);
    }

    public boolean isSeen(int playerIndex) {
        return this.lighting[playerIndex].bSeen();
    }

    public void setIsSeen(int playerIndex, boolean bSeen) {
        this.lighting[playerIndex].bSeen(bSeen);
    }

    public float getDarkMulti(int playerIndex) {
        return this.lighting[playerIndex].darkMulti();
    }

    public void setDarkMulti(int playerIndex, float darkMulti) {
        this.lighting[playerIndex].darkMulti(darkMulti);
    }

    public float getTargetDarkMulti(int playerIndex) {
        return this.lighting[playerIndex].targetDarkMulti();
    }

    public void setTargetDarkMulti(int playerIndex, float targetDarkMulti) {
        this.lighting[playerIndex].targetDarkMulti(targetDarkMulti);
    }

    public void setX(int x) {
        this.x = x;
        this.cachedScreenValue = -1;
    }

    public void setY(int y) {
        this.y = y;
        this.cachedScreenValue = -1;
    }

    public void setZ(int z) {
        z = Math.max(-32, z);
        this.z = z = Math.min(31, z);
        this.cachedScreenValue = -1;
    }

    public ArrayList<IsoGameCharacter> getDeferedCharacters() {
        return this.deferedCharacters;
    }

    public void addDeferredCharacter(IsoGameCharacter chr) {
        if (this.deferredCharacterTick != this.getCell().deferredCharacterTick) {
            if (!this.deferedCharacters.isEmpty()) {
                this.deferedCharacters.clear();
            }
            this.deferredCharacterTick = this.getCell().deferredCharacterTick;
        }
        this.deferedCharacters.add(chr);
    }

    public boolean isCacheIsFree() {
        return this.cacheIsFree;
    }

    public void setCacheIsFree(boolean cacheIsFree) {
        this.cacheIsFree = cacheIsFree;
    }

    public boolean isCachedIsFree() {
        return this.cachedIsFree;
    }

    public void setCachedIsFree(boolean cachedIsFree) {
        this.cachedIsFree = cachedIsFree;
    }

    public static boolean isbDoSlowPathfinding() {
        return doSlowPathfinding;
    }

    public static void setbDoSlowPathfinding(boolean abDoSlowPathfinding) {
        doSlowPathfinding = abDoSlowPathfinding;
    }

    public boolean isSolidFloorCached() {
        return this.solidFloorCached;
    }

    public void setSolidFloorCached(boolean solidFloorCached) {
        this.solidFloorCached = solidFloorCached;
    }

    public boolean isSolidFloor() {
        return this.solidFloor;
    }

    public void setSolidFloor(boolean solidFloor) {
        this.solidFloor = solidFloor;
    }

    public static ColorInfo getDefColorInfo() {
        return defColorInfo;
    }

    public boolean isOutside() {
        return this.properties.has(IsoFlagType.exterior);
    }

    public boolean HasPushable() {
        int size = this.movingObjects.size();
        for (int n = 0; n < size; ++n) {
            if (!(this.movingObjects.get(n) instanceof IsoPushableObject)) continue;
            return true;
        }
        return false;
    }

    public void setRoomID(long roomId) {
        this.roomId = roomId;
        if (roomId != -1L) {
            this.getProperties().unset(IsoFlagType.exterior);
            this.room = this.chunk.getRoom(roomId);
        }
    }

    public long getRoomID() {
        return this.roomId;
    }

    public String getRoomIDString() {
        return String.valueOf(this.getRoomID());
    }

    public boolean getCanSee(int playerIndex) {
        return this.lighting[playerIndex].bCanSee();
    }

    public boolean getSeen(int playerIndex) {
        return this.lighting[playerIndex].bSeen();
    }

    public IsoChunk getChunk() {
        return this.chunk;
    }

    public IsoObject getDoorOrWindow(boolean north) {
        for (int n = this.specialObjects.size() - 1; n >= 0; --n) {
            IsoWindow isoWindow;
            IsoObject s = this.specialObjects.get(n);
            if (s instanceof IsoDoor) {
                IsoDoor isoDoor = (IsoDoor)s;
                if (isoDoor.north == north) {
                    return s;
                }
            }
            if (s instanceof IsoThumpable) {
                IsoThumpable isoThumpable = (IsoThumpable)s;
                if (isoThumpable.north == north && (isoThumpable.isDoor() || isoThumpable.isWindow())) {
                    return s;
                }
            }
            if (!(s instanceof IsoWindow) || (isoWindow = (IsoWindow)s).isNorth() != north) continue;
            return s;
        }
        return null;
    }

    public IsoObject getDoorOrWindowOrWindowFrame(IsoDirections dir, boolean ignoreOpen) {
        for (int i = this.objects.size() - 1; i >= 0; --i) {
            IsoObject obj = (IsoObject)this.objects.get(i);
            IsoDoor door = (IsoDoor)Type.tryCastTo((Object)obj, IsoDoor.class);
            IsoThumpable thumpable = (IsoThumpable)Type.tryCastTo((Object)obj, IsoThumpable.class);
            IsoWindow window = (IsoWindow)Type.tryCastTo((Object)obj, IsoWindow.class);
            if (door != null && door.getSpriteEdge(ignoreOpen) == dir) {
                return obj;
            }
            if (thumpable != null && thumpable.getSpriteEdge(ignoreOpen) == dir) {
                return obj;
            }
            if (window != null) {
                if (window.isNorth() && dir == IsoDirections.N) {
                    return obj;
                }
                if (!window.isNorth() && dir == IsoDirections.W) {
                    return obj;
                }
            }
            if (!(obj instanceof IsoWindowFrame)) continue;
            IsoWindowFrame windowFrame = (IsoWindowFrame)obj;
            if (windowFrame.getNorth() && dir == IsoDirections.N) {
                return obj;
            }
            if (windowFrame.getNorth() || dir != IsoDirections.W) continue;
            return obj;
        }
        return null;
    }

    public IsoObject getOpenDoor(IsoDirections dir) {
        for (int i = 0; i < this.specialObjects.size(); ++i) {
            IsoObject obj = this.specialObjects.get(i);
            IsoDoor door = (IsoDoor)Type.tryCastTo((Object)obj, IsoDoor.class);
            IsoThumpable thumpable = (IsoThumpable)Type.tryCastTo((Object)obj, IsoThumpable.class);
            if (door != null && door.isOpen() && door.getSpriteEdge(false) == dir) {
                return door;
            }
            if (thumpable == null || !thumpable.open || thumpable.getSpriteEdge(false) != dir) continue;
            return thumpable;
        }
        return null;
    }

    public void removeWorldObject(IsoWorldInventoryObject object) {
        if (object == null) {
            return;
        }
        object.invalidateRenderChunkLevel(136L);
        object.removeFromWorld();
        object.removeFromSquare();
    }

    public void removeAllWorldObjects() {
        for (int i = 0; i < this.getWorldObjects().size(); ++i) {
            IsoObject object = this.getWorldObjects().get(i);
            object.invalidateRenderChunkLevel(136L);
            object.removeFromWorld();
            object.removeFromSquare();
            --i;
        }
    }

    public ArrayList<IsoWorldInventoryObject> getWorldObjects() {
        return this.worldObjects;
    }

    public int getNextNonItemObjectIndex(int index) {
        for (int i = index; i < this.getObjects().size(); ++i) {
            IsoObject object = (IsoObject)this.getObjects().get(i);
            if (object instanceof IsoWorldInventoryObject) continue;
            return i;
        }
        return -1;
    }

    public KahluaTable getModData() {
        if (this.table == null) {
            this.table = LuaManager.platform.newTable();
        }
        return this.table;
    }

    public boolean hasModData() {
        return this.table != null && !this.table.isEmpty();
    }

    public void setVertLight(int i, int col, int playerIndex) {
        this.lighting[playerIndex].lightverts(i, col);
    }

    public int getVertLight(int i, int playerIndex) {
        return this.lighting[playerIndex].lightverts(i);
    }

    public void setRainDrop(IsoRaindrop drop) {
        this.rainDrop = drop;
    }

    public IsoRaindrop getRainDrop() {
        return this.rainDrop;
    }

    public void setRainSplash(IsoRainSplash splash) {
        this.rainSplash = splash;
    }

    public IsoRainSplash getRainSplash() {
        return this.rainSplash;
    }

    public Zone getZone() {
        return this.zone;
    }

    public String getZoneType() {
        if (this.zone != null) {
            return this.zone.getType();
        }
        return null;
    }

    public boolean isOverlayDone() {
        return this.overlayDone;
    }

    public void setOverlayDone(boolean overlayDone) {
        this.overlayDone = overlayDone;
    }

    public ErosionData.Square getErosionData() {
        if (this.erosion == null) {
            this.erosion = new ErosionData.Square();
        }
        return this.erosion;
    }

    public void disableErosion() {
        ErosionData.Square erosionModData = this.getErosionData();
        if (erosionModData != null && !erosionModData.doNothing) {
            erosionModData.doNothing = true;
        }
    }

    public void removeErosionObject(String type) {
        if (this.erosion == null) {
            return;
        }
        if ("WallVines".equals(type)) {
            for (int i = 0; i < this.erosion.regions.size(); ++i) {
                ErosionCategory.Data sqCategoryData = (ErosionCategory.Data)this.erosion.regions.get(i);
                if (sqCategoryData.regionId != 2 || sqCategoryData.categoryId != 0) continue;
                this.erosion.regions.remove(i);
                break;
            }
        }
    }

    public void syncIsoTrap(HandWeapon weapon, IsoPlayer attacker) {
        AddExplosiveTrapPacket packet = new AddExplosiveTrapPacket();
        packet.set(weapon, attacker, this);
        ByteBufferWriter b = GameClient.connection.startPacket();
        PacketTypes.PacketType.AddExplosiveTrap.doPacket(b);
        packet.write(b);
        PacketTypes.PacketType.AddExplosiveTrap.send((IConnection)GameClient.connection);
    }

    public int getTrapPositionX() {
        return this.trapPositionX;
    }

    public void setTrapPositionX(int trapPositionX) {
        this.trapPositionX = trapPositionX;
    }

    public int getTrapPositionY() {
        return this.trapPositionY;
    }

    public void setTrapPositionY(int trapPositionY) {
        this.trapPositionY = trapPositionY;
    }

    public int getTrapPositionZ() {
        return this.trapPositionZ;
    }

    public void setTrapPositionZ(int trapPositionZ) {
        this.trapPositionZ = trapPositionZ;
    }

    public boolean haveElectricity() {
        if (!SandboxOptions.getInstance().allowExteriorGenerator.getValue() && this.has(IsoFlagType.exterior)) {
            return false;
        }
        return this.chunk != null && this.chunk.isGeneratorPoweringSquare(this.x, this.y, this.z);
    }

    @Deprecated
    public void setHaveElectricity(boolean haveElectricity) {
        if (this.getObjects() != null) {
            for (int i = 0; i < this.getObjects().size(); ++i) {
                if (!(this.getObjects().get(i) instanceof IsoLightSwitch)) continue;
                ((IsoObject)this.getObjects().get(i)).update();
            }
        }
    }

    public IsoGenerator getGenerator() {
        if (this.getSpecialObjects() != null) {
            for (int i = 0; i < this.getSpecialObjects().size(); ++i) {
                if (!(this.getSpecialObjects().get(i) instanceof IsoGenerator)) continue;
                return (IsoGenerator)this.getSpecialObjects().get(i);
            }
        }
        return null;
    }

    public void stopFire() {
        IsoFireManager.RemoveAllOn((IsoGridSquare)this);
        this.getProperties().set(IsoFlagType.burntOut);
        this.getProperties().unset(IsoFlagType.burning);
        this.burntOut = true;
    }

    public void transmitStopFire() {
        if (GameClient.client) {
            GameClient.sendStopFire((IsoGridSquare)this);
        }
    }

    public long playSound(String file) {
        BaseSoundEmitter emitter = IsoWorld.instance.getFreeEmitter((float)this.x + 0.5f, (float)this.y + 0.5f, (float)this.z);
        return emitter.playSound(file);
    }

    public long playSoundLocal(String file) {
        BaseSoundEmitter emitter = IsoWorld.instance.getFreeEmitter((float)this.x + 0.5f, (float)this.y + 0.5f, (float)this.z);
        return emitter.playSoundImpl(file, this);
    }

    @Deprecated
    public long playSound(String file, boolean doWorldSound) {
        BaseSoundEmitter emitter = IsoWorld.instance.getFreeEmitter((float)this.x + 0.5f, (float)this.y + 0.5f, (float)this.z);
        return emitter.playSound(file, doWorldSound);
    }

    public void FixStackableObjects() {
        IsoObject table = null;
        for (int i = 0; i < this.objects.size(); ++i) {
            IsoObject obj = (IsoObject)this.objects.get(i);
            if (obj instanceof IsoWorldInventoryObject || obj.sprite == null) continue;
            PropertyContainer props = obj.getProperties();
            if (props.getStackReplaceTileOffset() != 0) {
                obj.sprite = IsoSprite.getSprite((IsoSpriteManager)IsoSpriteManager.instance, (int)(obj.sprite.id + props.getStackReplaceTileOffset()));
                if (obj.sprite == null) continue;
                props = obj.sprite.getProperties();
            }
            if (props.isTable() && !props.has(IsoPropertyType.COUNTERTOP) || props.isTableTop()) {
                float offset;
                float f = offset = props.isSurfaceOffset() ? (float)props.getSurface() : 0.0f;
                if (table != null) {
                    obj.setRenderYOffset(table.getRenderYOffset() + table.getSurfaceOffset() - offset);
                } else {
                    obj.setRenderYOffset(0.0f - offset);
                }
            }
            if (!props.isTable()) continue;
            table = obj;
        }
    }

    private void setTableTopObjectDirection(IsoObject table, IsoObject obj, PropertyContainer props) {
        int nOffset = PZMath.tryParseInt((String)props.get(IsoPropertyType.N_OFFSET), (int)0);
        int sOffset = PZMath.tryParseInt((String)props.get(IsoPropertyType.S_OFFSET), (int)0);
        int wOffset = PZMath.tryParseInt((String)props.get(IsoPropertyType.W_OFFSET), (int)0);
        int eOffset = PZMath.tryParseInt((String)props.get(IsoPropertyType.E_OFFSET), (int)0);
        String ownFacing = props.get(IsoPropertyType.FACING);
        PropertyContainer tableProps = table.getProperties();
        String tableFacing = tableProps.get(IsoPropertyType.FACING);
        if (StringUtils.isNullOrWhitespace((String)tableFacing)) {
            return;
        }
        if (tableFacing.equals(ownFacing)) {
            return;
        }
        int offset = 0;
        if ("N".equals(tableFacing)) {
            if (nOffset != 0) {
                offset = nOffset;
            } else if (sOffset != 0) {
                offset = sOffset;
            }
        } else if ("S".equals(tableFacing)) {
            if (sOffset != 0) {
                offset = sOffset;
            } else if (nOffset != 0) {
                offset = nOffset;
            }
        } else if ("W".equals(tableFacing)) {
            if (wOffset != 0) {
                offset = wOffset;
            } else if (eOffset != 0) {
                offset = eOffset;
            }
        } else if ("E".equals(tableFacing)) {
            if (eOffset != 0) {
                offset = eOffset;
            } else if (wOffset != 0) {
                offset = wOffset;
            }
        }
        if (offset == 0) {
            return;
        }
        IsoSprite newSprite = IsoSpriteManager.instance.getSprite(obj.sprite.id + offset);
        if (newSprite != null) {
            obj.setSprite(newSprite);
        }
    }

    public void fixPlacedItemRenderOffsets() {
        IsoObject obj;
        int i;
        IsoObject[] objects = (IsoObject[])this.objects.getElements();
        int nObjects = this.objects.size();
        int nOffsets = 0;
        for (i = 0; i < nObjects; ++i) {
            obj = objects[i];
            int surfaceOffset = PZMath.roundToInt((float)obj.getSurfaceOffsetNoTable());
            if ((float)surfaceOffset <= 0.0f || PZArrayUtil.contains((int[])SURFACE_OFFSETS, (int)nOffsets, (int)surfaceOffset)) continue;
            IsoGridSquare.SURFACE_OFFSETS[nOffsets++] = surfaceOffset;
        }
        if (nOffsets == 0) {
            IsoGridSquare.SURFACE_OFFSETS[nOffsets++] = 0;
        }
        for (i = 0; i < nObjects; ++i) {
            IsoWorldInventoryObject worldObj;
            obj = objects[i];
            if (!(obj instanceof IsoWorldInventoryObject) || (worldObj = (IsoWorldInventoryObject)obj).isExtendedPlacement()) continue;
            int renderOffset = PZMath.roundToInt((float)(worldObj.zoff * 96.0f));
            int newOffset = 0;
            for (int j = 0; j < nOffsets; ++j) {
                if (renderOffset <= SURFACE_OFFSETS[j]) {
                    newOffset = SURFACE_OFFSETS[j];
                    break;
                }
                newOffset = SURFACE_OFFSETS[j];
                if (j < nOffsets - 1 && renderOffset < SURFACE_OFFSETS[j + 1]) break;
            }
            worldObj.zoff = (float)newOffset / 96.0f;
        }
    }

    public BaseVehicle getVehicleContainer() {
        int chunkMinX = PZMath.fastfloor((float)(((float)this.x - 4.0f) / 8.0f));
        int chunkMinY = PZMath.fastfloor((float)(((float)this.y - 4.0f) / 8.0f));
        int chunkMaxX = (int)Math.ceil(((float)this.x + 4.0f) / 8.0f);
        int chunkMaxY = (int)Math.ceil(((float)this.y + 4.0f) / 8.0f);
        for (int cy = chunkMinY; cy < chunkMaxY; ++cy) {
            for (int cx = chunkMinX; cx < chunkMaxX; ++cx) {
                IsoChunk chunk;
                IsoChunk isoChunk = chunk = GameServer.server ? ServerMap.instance.getChunk(cx, cy) : IsoWorld.instance.currentCell.getChunk(cx, cy);
                if (chunk == null) continue;
                for (int i = 0; i < chunk.vehicles.size(); ++i) {
                    BaseVehicle vehicle = chunk.vehicles.get(i);
                    if (!vehicle.isIntersectingSquare(this.x, this.y, this.z)) continue;
                    return vehicle;
                }
            }
        }
        return null;
    }

    public boolean isVehicleIntersecting() {
        int chunkMinX = PZMath.fastfloor((float)(((float)this.x - 4.0f) / 8.0f));
        int chunkMinY = PZMath.fastfloor((float)(((float)this.y - 4.0f) / 8.0f));
        int chunkMaxX = (int)Math.ceil(((float)this.x + 4.0f) / 8.0f);
        int chunkMaxY = (int)Math.ceil(((float)this.y + 4.0f) / 8.0f);
        for (int cy = chunkMinY; cy < chunkMaxY; ++cy) {
            for (int cx = chunkMinX; cx < chunkMaxX; ++cx) {
                IsoChunk chunk;
                IsoChunk isoChunk = chunk = GameServer.server ? ServerMap.instance.getChunk(cx, cy) : IsoWorld.instance.currentCell.getChunk(cx, cy);
                if (chunk == null) continue;
                for (int i = 0; i < chunk.vehicles.size(); ++i) {
                    BaseVehicle vehicle = chunk.vehicles.get(i);
                    if (!vehicle.isIntersectingSquare(this.x, this.y, this.z)) continue;
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isVehicleIntersectingCrops() {
        if (!this.hasFarmingPlant()) {
            return false;
        }
        int chunkMinX = PZMath.fastfloor((float)(((float)this.x - 4.0f) / 8.0f));
        int chunkMinY = PZMath.fastfloor((float)(((float)this.y - 4.0f) / 8.0f));
        int chunkMaxX = (int)Math.ceil(((float)this.x + 4.0f) / 8.0f);
        int chunkMaxY = (int)Math.ceil(((float)this.y + 4.0f) / 8.0f);
        for (int cy = chunkMinY; cy < chunkMaxY; ++cy) {
            for (int cx = chunkMinX; cx < chunkMaxX; ++cx) {
                IsoChunk chunk;
                IsoChunk isoChunk = chunk = GameServer.server ? ServerMap.instance.getChunk(cx, cy) : IsoWorld.instance.currentCell.getChunk(cx, cy);
                if (chunk == null) continue;
                for (int i = 0; i < chunk.vehicles.size(); ++i) {
                    BaseVehicle vehicle = chunk.vehicles.get(i);
                    if (!vehicle.isIntersectingSquare(this) || vehicle.notKillCrops()) continue;
                    return true;
                }
            }
        }
        return false;
    }

    public DeviceData getDeviceData() {
        BaseVehicle vehicle = this.getVehicleContainer();
        if (vehicle != null) {
            for (int i = 0; i < vehicle.getPartCount(); ++i) {
                DeviceData deviceData = vehicle.getPartByIndex(i).getDeviceData();
                if (deviceData == null) continue;
                return deviceData;
            }
        } else {
            for (int i = 0; i < this.objects.size(); ++i) {
                Object object = this.objects.get(i);
                if (!(object instanceof IsoWaveSignal)) continue;
                IsoWaveSignal isoWaveSignal = (IsoWaveSignal)object;
                return isoWaveSignal.getDeviceData();
            }
        }
        return null;
    }

    public void checkForIntersectingCrops(BaseVehicle vehicle) {
        if (this.hasFarmingPlant() && !vehicle.notKillCrops() && vehicle.isIntersectingSquare(this)) {
            this.destroyFarmingPlant();
        }
    }

    public IsoCompost getCompost() {
        if (this.getSpecialObjects() != null) {
            for (int i = 0; i < this.getSpecialObjects().size(); ++i) {
                if (!(this.getSpecialObjects().get(i) instanceof IsoCompost)) continue;
                return (IsoCompost)this.getSpecialObjects().get(i);
            }
        }
        return null;
    }

    public <T> PZArrayList<ItemContainer> getAllContainers(T paramToCompare, Invokers.Params2.Boolean.ICallback<T, ItemContainer> isValidPredicate, PZArrayList<ItemContainer> containerList) {
        this.getObjectContainers(paramToCompare, isValidPredicate, containerList);
        this.getVehicleItemContainers(paramToCompare, isValidPredicate, containerList);
        return containerList;
    }

    public <T> PZArrayList<ItemContainer> getAllContainersFromAdjacentSquare(IsoDirections dir, T paramToCompare, Invokers.Params2.Boolean.ICallback<T, ItemContainer> isValidPredicate, PZArrayList<ItemContainer> containerList) {
        IsoGridSquare adjSquare = this.getAdjacentSquare(dir);
        return adjSquare.getAllContainers(paramToCompare, isValidPredicate, containerList);
    }

    public <T> PZArrayList<ItemContainer> getObjectContainers(T paramToCompare, Invokers.Params2.Boolean.ICallback<T, ItemContainer> isValidPredicate, PZArrayList<ItemContainer> containerList) {
        PZArrayList<IsoObject> adjObj = this.getObjects();
        for (int i = 0; i < adjObj.size(); ++i) {
            IsoObject obj = (IsoObject)adjObj.get(i);
            obj.getContainers(paramToCompare, isValidPredicate, containerList);
        }
        return containerList;
    }

    public <T> PZArrayList<ItemContainer> getVehicleItemContainers(T paramToCompare, Invokers.Params2.Boolean.ICallback<T, ItemContainer> isValidPredicate) {
        PZArrayList containerList = new PZArrayList(ItemContainer.class, 10);
        return this.getVehicleItemContainers(paramToCompare, isValidPredicate, (PZArrayList<ItemContainer>)containerList);
    }

    public <T> PZArrayList<ItemContainer> getVehicleItemContainers(T paramToCompare, Invokers.Params2.Boolean.ICallback<T, ItemContainer> isValidPredicate, PZArrayList<ItemContainer> containerList) {
        boolean hasVehicle;
        BaseVehicle vehicle = this.getVehicleContainer();
        boolean bl = hasVehicle = vehicle != null;
        if (!hasVehicle) {
            return containerList;
        }
        return vehicle.getVehicleItemContainers(paramToCompare, isValidPredicate, containerList);
    }

    public void setIsoWorldRegion(IsoWorldRegion mr) {
        this.hasSetIsoWorldRegion = mr != null;
        this.isoWorldRegion = mr;
    }

    public IWorldRegion getIsoWorldRegion() {
        if (this.z < 0) {
            return null;
        }
        if (GameServer.server) {
            return IsoRegions.getIsoWorldRegion((int)this.x, (int)this.y, (int)this.z);
        }
        if (!this.hasSetIsoWorldRegion) {
            this.isoWorldRegion = IsoRegions.getIsoWorldRegion((int)this.x, (int)this.y, (int)this.z);
            this.hasSetIsoWorldRegion = true;
        }
        return this.isoWorldRegion;
    }

    public void ResetIsoWorldRegion() {
        this.isoWorldRegion = null;
        this.hasSetIsoWorldRegion = false;
    }

    public boolean isInARoom() {
        return this.getRoom() != null || this.getIsoWorldRegion() != null && this.getIsoWorldRegion().isPlayerRoom();
    }

    public int getRoomSize() {
        if (this.getRoom() != null) {
            return this.getRoom().getSquares().size();
        }
        if (this.getIsoWorldRegion() != null && this.getIsoWorldRegion().isPlayerRoom()) {
            return this.getIsoWorldRegion().getSquareSize();
        }
        return -1;
    }

    public int getWallType() {
        IsoGridSquare sqS;
        IsoGridSquare sqE;
        int type = 0;
        if (this.getProperties().has(IsoFlagType.WallN)) {
            type |= 1;
        }
        if (this.getProperties().has(IsoFlagType.WallW)) {
            type |= 4;
        }
        if (this.getProperties().has(IsoFlagType.WallNW)) {
            type |= 5;
        }
        if ((sqE = this.nav[IsoDirections.E.ordinal()]) != null && (sqE.getProperties().has(IsoFlagType.WallW) || sqE.getProperties().has(IsoFlagType.WallNW))) {
            type |= 8;
        }
        if ((sqS = this.nav[IsoDirections.S.ordinal()]) != null && (sqS.getProperties().has(IsoFlagType.WallN) || sqS.getProperties().has(IsoFlagType.WallNW))) {
            type |= 2;
        }
        return type;
    }

    public int getPuddlesDir() {
        int dir = 8;
        if (this.isInARoom()) {
            return 1;
        }
        for (int i = 0; i < this.getObjects().size(); ++i) {
            IsoObject obj = (IsoObject)this.getObjects().get(i);
            if (obj.attachedAnimSprite == null) continue;
            for (int j = 0; j < obj.attachedAnimSprite.size(); ++j) {
                IsoSprite attached = obj.attachedAnimSprite.get((int)j).parentSprite;
                if (attached.name == null) continue;
                if (attached.name.equals("street_trafficlines_01_2") || attached.name.equals("street_trafficlines_01_6") || attached.name.equals("street_trafficlines_01_22") || attached.name.equals("street_trafficlines_01_32")) {
                    dir = 4;
                }
                if (!attached.name.equals("street_trafficlines_01_4") && !attached.name.equals("street_trafficlines_01_0") && !attached.name.equals("street_trafficlines_01_16")) continue;
                dir = 2;
            }
        }
        return dir;
    }

    public boolean haveFire() {
        int size = this.objects.size();
        IsoObject[] objectArray = (IsoObject[])this.objects.getElements();
        for (int n = 0; n < size; ++n) {
            IsoObject obj = objectArray[n];
            if (!(obj instanceof IsoFire)) continue;
            return true;
        }
        return false;
    }

    public IsoBuilding getRoofHideBuilding() {
        return this.roofHideBuilding;
    }

    public IsoGridSquare getAdjacentSquare(IsoDirections dir) {
        return this.nav[dir.ordinal()];
    }

    public void setAdjacentSquare(IsoDirections dir, IsoGridSquare square) {
        this.nav[dir.ordinal()] = square;
    }

    public IsoGridSquare[] getSurroundingSquares() {
        return this.nav;
    }

    public IsoGridSquare getSquareAbove() {
        return cellGetSquare.getGridSquare(this.x, this.y, this.z + 1);
    }

    public IsoGridSquare getAdjacentPathSquare(IsoDirections dir) {
        switch (1.$SwitchMap$zombie$iso$IsoDirections[dir.ordinal()]) {
            case 5: {
                return this.nw;
            }
            case 1: {
                return this.n;
            }
            case 6: {
                return this.ne;
            }
            case 3: {
                return this.w;
            }
            case 4: {
                return this.e;
            }
            case 7: {
                return this.sw;
            }
            case 2: {
                return this.s;
            }
            case 8: {
                return this.se;
            }
        }
        return null;
    }

    public float getApparentZ(float dx, float dy) {
        float fudge;
        dx = PZMath.clamp((float)dx, (float)0.0f, (float)1.0f);
        dy = PZMath.clamp((float)dy, (float)0.0f, (float)1.0f);
        float f = fudge = PerformanceSettings.fboRenderChunk ? 0.1f : 0.0f;
        if (this.has(IsoObjectType.stairsTN)) {
            return (float)this.getZ() + PZMath.lerp((float)(0.6666f + fudge), (float)1.0f, (float)(1.0f - dy));
        }
        if (this.has(IsoObjectType.stairsTW)) {
            return (float)this.getZ() + PZMath.lerp((float)(0.6666f + fudge), (float)1.0f, (float)(1.0f - dx));
        }
        if (this.has(IsoObjectType.stairsMN)) {
            return (float)this.getZ() + PZMath.lerp((float)(0.3333f + fudge), (float)(0.6666f + fudge), (float)(1.0f - dy));
        }
        if (this.has(IsoObjectType.stairsMW)) {
            return (float)this.getZ() + PZMath.lerp((float)(0.3333f + fudge), (float)(0.6666f + fudge), (float)(1.0f - dx));
        }
        if (this.has(IsoObjectType.stairsBN)) {
            return (float)this.getZ() + PZMath.lerp((float)0.01f, (float)(0.3333f + fudge), (float)(1.0f - dy));
        }
        if (this.has(IsoObjectType.stairsBW)) {
            return (float)this.getZ() + PZMath.lerp((float)0.01f, (float)(0.3333f + fudge), (float)(1.0f - dx));
        }
        return (float)this.getZ() + this.getSlopedSurfaceHeight(dx, dy);
    }

    public IsoDirections getStairsDirection() {
        if (this.HasStairsNorth()) {
            return IsoDirections.N;
        }
        if (this.HasStairsWest()) {
            return IsoDirections.W;
        }
        return null;
    }

    public float getStairsHeightMax() {
        if (this.has(IsoObjectType.stairsTN) || this.has(IsoObjectType.stairsTW)) {
            return 1.0f;
        }
        if (this.has(IsoObjectType.stairsMN) || this.has(IsoObjectType.stairsMW)) {
            return 0.66f;
        }
        if (this.has(IsoObjectType.stairsMN) || this.has(IsoObjectType.stairsMW)) {
            return 0.33f;
        }
        return 0.0f;
    }

    public float getStairsHeightMin() {
        if (this.has(IsoObjectType.stairsTN) || this.has(IsoObjectType.stairsTW)) {
            return 0.66f;
        }
        if (this.has(IsoObjectType.stairsMN) || this.has(IsoObjectType.stairsMW)) {
            return 0.33f;
        }
        return 0.0f;
    }

    public float getStairsHeight(IsoDirections edge) {
        IsoDirections slopeDir = this.getStairsDirection();
        if (slopeDir == null) {
            return 0.0f;
        }
        if (slopeDir == edge) {
            return this.getStairsHeightMax();
        }
        if (slopeDir.Rot180() == edge) {
            return this.getStairsHeightMin();
        }
        return -1.0f;
    }

    public boolean isStairsEdgeBlocked(IsoDirections edge) {
        IsoDirections dir = this.getStairsDirection();
        if (dir == null) {
            return false;
        }
        IsoGridSquare square2 = this.getAdjacentSquare(edge);
        if (square2 == null) {
            return true;
        }
        return this.getStairsHeight(edge) != square2.getStairsHeight(edge.Rot180());
    }

    public boolean hasSlopedSurface() {
        return this.getSlopedSurfaceDirection() != null;
    }

    public IsoDirections getSlopedSurfaceDirection() {
        return this.getProperties().getSlopedSurfaceDirection();
    }

    public boolean hasIdenticalSlopedSurface(IsoGridSquare other) {
        return this.getSlopedSurfaceDirection() == other.getSlopedSurfaceDirection() && this.getSlopedSurfaceHeightMin() == other.getSlopedSurfaceHeightMin() && this.getSlopedSurfaceHeightMax() == other.getSlopedSurfaceHeightMax();
    }

    public float getSlopedSurfaceHeightMin() {
        return (float)this.getProperties().getSlopedSurfaceHeightMin() / 100.0f;
    }

    public float getSlopedSurfaceHeightMax() {
        return (float)this.getProperties().getSlopedSurfaceHeightMax() / 100.0f;
    }

    public float getSlopedSurfaceHeight(float dx, float dy) {
        float z;
        IsoDirections dir = this.getSlopedSurfaceDirection();
        if (dir == null) {
            return 0.0f;
        }
        dx = PZMath.clamp((float)dx, (float)0.0f, (float)1.0f);
        dy = PZMath.clamp((float)dy, (float)0.0f, (float)1.0f);
        int slopeHeightMin = this.getProperties().getSlopedSurfaceHeightMin();
        int slopeHeightMax = this.getProperties().getSlopedSurfaceHeightMax();
        switch (1.$SwitchMap$zombie$iso$IsoDirections[dir.ordinal()]) {
            case 1: {
                float f = PZMath.lerp((float)slopeHeightMin, (float)slopeHeightMax, (float)(1.0f - dy));
                break;
            }
            case 2: {
                float f = PZMath.lerp((float)slopeHeightMin, (float)slopeHeightMax, (float)dy);
                break;
            }
            case 3: {
                float f = PZMath.lerp((float)slopeHeightMin, (float)slopeHeightMax, (float)(1.0f - dx));
                break;
            }
            case 4: {
                float f = PZMath.lerp((float)slopeHeightMin, (float)slopeHeightMax, (float)dx);
                break;
            }
            default: {
                float f = z = -1.0f;
            }
        }
        if (z < 0.0f) {
            return 0.0f;
        }
        return z / 100.0f;
    }

    public float getSlopedSurfaceHeight(IsoDirections edge) {
        IsoDirections slopeDir = this.getSlopedSurfaceDirection();
        if (slopeDir == null) {
            return 0.0f;
        }
        if (slopeDir == edge) {
            return this.getSlopedSurfaceHeightMax();
        }
        if (slopeDir.Rot180() == edge) {
            return this.getSlopedSurfaceHeightMin();
        }
        return -1.0f;
    }

    public boolean isSlopedSurfaceEdgeBlocked(IsoDirections edge) {
        IsoDirections dir = this.getSlopedSurfaceDirection();
        if (dir == null) {
            return false;
        }
        IsoGridSquare square2 = this.getAdjacentSquare(edge);
        if (square2 == null) {
            return true;
        }
        return this.getSlopedSurfaceHeight(edge) != square2.getSlopedSurfaceHeight(edge.Rot180());
    }

    public boolean hasSlopedSurfaceToLevelAbove(IsoDirections dir) {
        IsoDirections slopeDir = this.getSlopedSurfaceDirection();
        if (slopeDir == null) {
            return false;
        }
        return this.getSlopedSurfaceHeight(dir) == 1.0f;
    }

    public float getTotalWeightOfItemsOnFloor() {
        float total = 0.0f;
        for (int i = 0; i < this.worldObjects.size(); ++i) {
            InventoryItem item = this.worldObjects.get(i).getItem();
            if (item == null) continue;
            total += item.getUnequippedWeight();
        }
        return total;
    }

    public boolean getCollideMatrix(int dx, int dy, int dz) {
        return IsoGridSquare.getMatrixBit(this.collideMatrix, dx + 1, dy + 1, dz + 1);
    }

    public boolean getPathMatrix(int dx, int dy, int dz) {
        return IsoGridSquare.getMatrixBit(this.pathMatrix, dx + 1, dy + 1, dz + 1);
    }

    public boolean getVisionMatrix(int dx, int dy, int dz) {
        return IsoGridSquare.getMatrixBit(this.visionMatrix, dx + 1, dy + 1, dz + 1);
    }

    public void checkRoomSeen(int playerIndex) {
        IsoRoom room = this.getRoom();
        if (room == null || room.def == null || room.def.explored) {
            return;
        }
        IsoPlayer player = IsoPlayer.players[playerIndex];
        if (player == null) {
            return;
        }
        if (this.z != PZMath.fastfloor((float)player.getZ())) {
            return;
        }
        int dist = 10;
        if (player.getBuilding() == room.building) {
            dist = 50;
        }
        if (IsoUtils.DistanceToSquared((float)player.getX(), (float)player.getY(), (float)((float)this.x + 0.5f), (float)((float)this.y + 0.5f)) < (float)(dist * dist)) {
            room.def.explored = true;
            room.onSee();
            room.seen = 0;
            if (player.isLocalPlayer()) {
                player.triggerMusicIntensityEvent("SeeUnexploredRoom");
            }
        }
    }

    public boolean hasFlies() {
        return this.hasFlies;
    }

    public void setHasFlies(boolean hasFlies) {
        if (hasFlies != this.hasFlies) {
            this.invalidateRenderChunkLevel(hasFlies ? 64L : 128L);
        }
        this.hasFlies = hasFlies;
    }

    public float getLightLevel(int playerIndex) {
        if (playerIndex == -1) {
            return this.getLightLevel2();
        }
        ColorInfo lightInfo = this.lighting[playerIndex].lightInfo();
        return PZMath.max((float)lightInfo.r, (float)lightInfo.g, (float)lightInfo.b);
    }

    public float getLightLevel2() {
        LightingJNI.JNILighting lightingTemp = new LightingJNI.JNILighting(-1, this);
        ColorInfo lightInfo = lightingTemp.lightInfo();
        return PZMath.max((float)lightInfo.r, (float)lightInfo.g, (float)lightInfo.b);
    }

    public ArrayList<IsoAnimal> getAnimals(ArrayList<IsoAnimal> result) {
        result.clear();
        for (int i = 0; i < this.getMovingObjects().size(); ++i) {
            IsoAnimal animal;
            IsoMovingObject movingObject = this.getMovingObjects().get(i);
            if (!(movingObject instanceof IsoAnimal) || (animal = (IsoAnimal)movingObject).isOnHook()) continue;
            result.add(animal);
        }
        return result;
    }

    public ArrayList<IsoAnimal> getAnimals() {
        return this.getAnimals(new ArrayList<IsoAnimal>());
    }

    public boolean checkHaveGrass() {
        IsoObject floor = this.getFloor();
        if (floor == null) {
            return false;
        }
        if (floor.getSprite() != null && !floor.getProperties().has(IsoPropertyType.GRASS_FLOOR)) {
            return false;
        }
        if (floor.getAttachedAnimSprite() != null) {
            for (int i = 0; i < floor.getAttachedAnimSprite().size(); ++i) {
                IsoSprite sprite = floor.getAttachedAnimSprite().get((int)i).parentSprite;
                if (!"blends_natural_01_87".equals(sprite.getName())) continue;
                return false;
            }
        }
        return true;
    }

    public boolean checkHaveDung() {
        for (int i = 0; i < this.getWorldObjects().size(); ++i) {
            InventoryItem item = this.getWorldObjects().get(i).getItem();
            if (!item.getScriptItem().isDung) continue;
            return true;
        }
        return false;
    }

    public ArrayList<InventoryItem> removeAllDung() {
        ArrayList<InventoryItem> result = new ArrayList<InventoryItem>();
        for (int i = 0; i < this.getWorldObjects().size(); ++i) {
            InventoryItem item = this.getWorldObjects().get(i).getItem();
            if (!item.getScriptItem().isDung) continue;
            result.add(item);
            this.removeWorldObject(this.getWorldObjects().get(i));
            --i;
        }
        return result;
    }

    public boolean removeGrass() {
        boolean removed = false;
        if (this.getFloor() != null && this.getFloor().getSprite().getProperties().has("grassFloor") && this.checkHaveGrass()) {
            this.getFloor().addAttachedAnimSpriteByName("blends_natural_01_87");
            if (GameServer.server) {
                this.getFloor().transmitUpdatedSpriteToClients();
            }
            removed = true;
            for (int i = 0; i < this.getObjects().size(); ++i) {
                IsoObject obj = (IsoObject)this.getObjects().get(i);
                if (!obj.getSprite().getProperties().has(IsoFlagType.canBeRemoved)) continue;
                if (GameServer.server) {
                    this.transmitRemoveItemFromSquare(obj);
                }
                this.getObjects().remove((Object)obj);
                --i;
            }
            this.RecalcProperties();
            this.RecalcAllWithNeighbours(true);
            if (!this.isOutside()) {
                return false;
            }
            Zone zone = this.getGrassRegrowthZone();
            if (zone == null) {
                zone = IsoWorld.instance.getMetaGrid().registerZone("", "GrassRegrowth", this.x - 20, this.y - 20, this.z, 40, 40);
                zone.setLastActionTimestamp(Long.valueOf(GameTime.instance.getCalender().getTimeInMillis() / 1000L).intValue());
                INetworkPacket.sendToRelative((PacketTypes.PacketType)PacketTypes.PacketType.RegisterZone, (float)this.x, (float)this.y, (Object[])new Object[]{zone});
            } else {
                zone.setLastActionTimestamp(Long.valueOf(GameTime.instance.getCalender().getTimeInMillis() / 1000L).intValue());
                INetworkPacket.sendToRelative((PacketTypes.PacketType)PacketTypes.PacketType.SyncZone, (float)this.x, (float)this.y, (Object[])new Object[]{zone});
            }
        }
        return removed;
    }

    private Zone getGrassRegrowthZone() {
        ArrayList<Zone> zones = threadLocalZones.get();
        zones.clear();
        IsoWorld.instance.metaGrid.getZonesAt(this.x, this.y, this.z, zones);
        for (int i = 0; i < zones.size(); ++i) {
            Zone zone = zones.get(i);
            if (!StringUtils.equals((String)zone.getType(), (String)"GrassRegrowth")) continue;
            return zone;
        }
        return null;
    }

    public int getZombieCount() {
        int count = 0;
        for (int n = 0; n < this.movingObjects.size(); ++n) {
            IsoMovingObject mov = this.movingObjects.get(n);
            if (mov == null || !(mov instanceof IsoZombie)) continue;
            ++count;
        }
        return count;
    }

    public String getSquareRegion() {
        ArrayList<Zone> zones = threadLocalZones.get();
        zones.clear();
        IsoWorld.instance.metaGrid.getZonesAt(this.x, this.y, 0, zones);
        for (int i = 0; i < zones.size(); ++i) {
            Zone zone = zones.get(i);
            if (!StringUtils.equals((String)zone.type, (String)"Region")) continue;
            return zone.name;
        }
        return "General";
    }

    public boolean containsVegetation() {
        for (int i = 0; i < this.getObjects().size(); ++i) {
            IsoObject obj = (IsoObject)this.getObjects().get(i);
            if (obj == null || obj.getSprite() == null || obj.getSprite().getName() == null || !obj.getSprite().getName().contains("vegetation")) continue;
            return true;
        }
        return false;
    }

    public IsoAnimalTrack getAnimalTrack() {
        for (int i = 0; i < this.getObjects().size(); ++i) {
            IsoObject obj = (IsoObject)this.getObjects().get(i);
            if (!(obj instanceof IsoAnimalTrack)) continue;
            IsoAnimalTrack isoAnimalTrack = (IsoAnimalTrack)obj;
            return isoAnimalTrack;
        }
        return null;
    }

    public boolean hasTrashReceptacle() {
        for (int i = 0; i < this.getObjects().size(); ++i) {
            IsoObject obj = (IsoObject)this.getObjects().get(i);
            if (obj == null || obj.getSprite() == null || obj.getSprite().getProperties() == null || !obj.getSprite().getProperties().has("IsTrashCan")) continue;
            return true;
        }
        return false;
    }

    public boolean hasTrash() {
        for (int i = 0; i < this.getObjects().size(); ++i) {
            IsoObject obj = (IsoObject)this.getObjects().get(i);
            if (obj == null || obj.getSprite() == null || (obj.getSprite().getName() == null || !obj.getSprite().getName().contains("trash")) && (obj.getSprite().getProperties() == null || !obj.getSprite().getProperties().has("IsTrashCan"))) continue;
            return true;
        }
        return false;
    }

    public IsoObject getTrashReceptacle() {
        for (int i = 0; i < this.getObjects().size(); ++i) {
            IsoObject obj = (IsoObject)this.getObjects().get(i);
            if (obj == null || obj.getSprite() == null || obj.getSprite().getProperties() == null || !obj.getSprite().getProperties().has("IsTrashCan") || obj.getContainer() == null) continue;
            return obj;
        }
        return null;
    }

    public boolean isExtraFreeSquare() {
        return this.isFree(false) && this.getObjects().size() < 2 && !this.HasStairs() && this.hasFloor();
    }

    public IsoGridSquare getRandomAdjacentFreeSameRoom() {
        if (this.getRoom() == null) {
            return null;
        }
        ArrayList<IsoGridSquare> squares = new ArrayList<IsoGridSquare>();
        for (int i = 0; i < DIRECTIONS.length; ++i) {
            IsoGridSquare testSq = this.getAdjacentSquare(DIRECTIONS[i]);
            if (testSq == null || !testSq.isExtraFreeSquare() || testSq.getRoom() == null || testSq.getRoom() != this.getRoom()) continue;
            squares.add(testSq);
        }
        if (squares.isEmpty()) {
            return null;
        }
        IsoGridSquare sq2 = (IsoGridSquare)squares.get(Rand.Next((int)squares.size()));
        if (sq2 != null && sq2.isExtraFreeSquare() && sq2.getRoom() != null && sq2.getRoom() == this.getRoom()) {
            return sq2;
        }
        return sq2;
    }

    public String getZombiesType() {
        ArrayList<Zone> zones = threadLocalZones.get();
        zones.clear();
        IsoWorld.instance.metaGrid.getZonesAt(this.x, this.y, 0, zones);
        for (int i = 0; i < zones.size(); ++i) {
            Zone zone = zones.get(i);
            if (!StringUtils.equalsIgnoreCase((String)zone.type, (String)"ZombiesType")) continue;
            return zone.name;
        }
        return null;
    }

    public String getLootZone() {
        ArrayList<Zone> zones = threadLocalZones.get();
        zones.clear();
        IsoWorld.instance.metaGrid.getZonesAt(this.x, this.y, 0, zones);
        for (int i = 0; i < zones.size(); ++i) {
            Zone zone = zones.get(i);
            if (!StringUtils.equals((String)zone.type, (String)"LootZone")) continue;
            return zone.name;
        }
        return null;
    }

    public IsoObject addTileObject(String spriteName) {
        IsoObject obj = IsoObject.getNew(this, spriteName, null, false);
        this.AddTileObject(obj);
        MapObjects.newGridSquare((IsoGridSquare)this);
        MapObjects.loadGridSquare((IsoGridSquare)this);
        return obj;
    }

    public boolean hasSand() {
        if (this.getFloor() == null || this.getFloor().getSprite() == null || this.getFloor().getSprite().getName() == null) {
            return false;
        }
        String name = this.getFloor().getSprite().getName();
        if (!name.contains("blends_natural_01") && !name.contains("floors_exterior_natural_01")) {
            return false;
        }
        if (name.equals("blends_natural_01_0") || name.equals("blends_natural_01_5") || name.equals("blends_natural_01_6") || name.equals("blends_natural_01_7")) {
            return true;
        }
        return name.contains("floors_exterior_natural_24");
    }

    public boolean hasDirt() {
        if (this.getFloor() == null || this.getFloor().getSprite() == null || this.getFloor().getSprite().getName() == null) {
            return false;
        }
        String name = this.getFloor().getSprite().getName();
        if (!name.contains("blends_natural_01") && !name.contains("floors_exterior_natural_01")) {
            return false;
        }
        if (name.equals("blends_natural_01_64") || name.equals("blends_natural_01_69") || name.equals("blends_natural_01_70") || name.equals("blends_natural_01_71")) {
            return true;
        }
        if (name.equals("blends_natural_01_80") || name.equals("blends_natural_01_85") || name.equals("blends_natural_01_86") || name.equals("blends_natural_01_87")) {
            return true;
        }
        return name.equals("floors_exterior_natural_16") || name.equals("floors_exterior_natural_17") || name.equals("floors_exterior_natural_18") || name.equals("floors_exterior_natural_19");
    }

    public boolean hasNaturalFloor() {
        if (this.getFloor() == null || this.getFloor().getSprite() == null || this.getFloor().getSprite().getName() == null) {
            return false;
        }
        String name = this.getFloor().getSprite().getName();
        return name.startsWith("blends_natural_01") || name.startsWith("floors_exterior_natural");
    }

    public void dirtStamp() {
        IsoObject obj2;
        if (this.hasSand() || this.hasDirt() || this.getFloor() == null) {
            return;
        }
        this.getFloor().setAttachedAnimSprite(null);
        this.getFloor().setOverlaySprite(null);
        Object tile = "blends_natural_01_64";
        if (!Rand.NextBool((int)4)) {
            tile = "blends_natural_01_" + (69 + Rand.Next((int)3));
        }
        this.getFloor().setSpriteFromName((String)tile);
        IsoGridSquare sq = this.getAdjacentSquare(IsoDirections.N);
        if (sq != null && !sq.hasSand() && !sq.hasDirt() && sq.getFloor() != null) {
            RandomizedZoneStoryBase.cleanSquareForStory((IsoGridSquare)sq);
            tile = "blends_natural_01_75";
            if (!Rand.NextBool((int)4)) {
                tile = "blends_natural_01_79";
            }
            sq.getFloor().setOverlaySprite((String)tile);
            sq.RecalcAllWithNeighbours(true);
            obj2 = sq.getFloor();
            if (obj2 != null) {
                if (obj2.attachedAnimSprite == null) {
                    obj2.attachedAnimSprite = new ArrayList(4);
                }
                obj2.attachedAnimSprite.add(IsoSpriteInstance.get((IsoSprite)sq.getFloor().getOverlaySprite()));
            }
        }
        if ((sq = this.getAdjacentSquare(IsoDirections.S)) != null && !sq.hasSand() && !sq.hasDirt() && sq.getFloor() != null) {
            RandomizedZoneStoryBase.cleanSquareForStory((IsoGridSquare)sq);
            tile = "blends_natural_01_72";
            if (!Rand.NextBool((int)4)) {
                tile = "blends_natural_01_76";
            }
            sq.getFloor().setOverlaySprite((String)tile);
            sq.RecalcAllWithNeighbours(true);
            obj2 = sq.getFloor();
            if (obj2 != null) {
                if (obj2.attachedAnimSprite == null) {
                    obj2.attachedAnimSprite = new ArrayList(4);
                }
                obj2.attachedAnimSprite.add(IsoSpriteInstance.get((IsoSprite)sq.getFloor().getOverlaySprite()));
            }
        }
        if ((sq = this.getAdjacentSquare(IsoDirections.E)) != null && !sq.hasSand() && !sq.hasDirt() && sq.getFloor() != null) {
            RandomizedZoneStoryBase.cleanSquareForStory((IsoGridSquare)sq);
            tile = "blends_natural_01_73";
            if (!Rand.NextBool((int)4)) {
                tile = "blends_natural_01_77";
            }
            sq.getFloor().setOverlaySprite((String)tile);
            sq.RecalcAllWithNeighbours(true);
            obj2 = sq.getFloor();
            if (obj2 != null) {
                if (obj2.attachedAnimSprite == null) {
                    obj2.attachedAnimSprite = new ArrayList(4);
                }
                obj2.attachedAnimSprite.add(IsoSpriteInstance.get((IsoSprite)sq.getFloor().getOverlaySprite()));
            }
        }
        if ((sq = this.getAdjacentSquare(IsoDirections.W)) != null && !sq.hasSand() && !sq.hasDirt() && sq.getFloor() != null) {
            RandomizedZoneStoryBase.cleanSquareForStory((IsoGridSquare)sq);
            tile = "blends_natural_01_74";
            if (!Rand.NextBool((int)4)) {
                tile = "blends_natural_01_78";
            }
            sq.getFloor().setOverlaySprite((String)tile);
            sq.RecalcAllWithNeighbours(true);
            obj2 = sq.getFloor();
            if (obj2 != null) {
                if (obj2.attachedAnimSprite == null) {
                    obj2.attachedAnimSprite = new ArrayList(4);
                }
                obj2.attachedAnimSprite.add(IsoSpriteInstance.get((IsoSprite)sq.getFloor().getOverlaySprite()));
            }
        }
    }

    public IsoGridSquare getRandomAdjacent() {
        ArrayList<IsoGridSquare> squares = new ArrayList<IsoGridSquare>();
        for (int i = 0; i < DIRECTIONS.length; ++i) {
            IsoGridSquare testSq = this.getAdjacentSquare(DIRECTIONS[i]);
            if (testSq == null || !testSq.isExtraFreeSquare()) continue;
            squares.add(testSq);
        }
        if (squares.isEmpty()) {
            return null;
        }
        return (IsoGridSquare)squares.get(Rand.Next((int)squares.size()));
    }

    public boolean isAdjacentTo(IsoGridSquare sq) {
        if (this == sq) {
            return true;
        }
        for (int i = 0; i < DIRECTIONS.length; ++i) {
            IsoGridSquare testSq = this.getAdjacentSquare(DIRECTIONS[i]);
            if (testSq == null || testSq != sq) continue;
            return true;
        }
        return false;
    }

    public boolean hasFireObject() {
        for (int i = 0; i < this.getObjects().size(); ++i) {
            IsoObject obj = (IsoObject)this.getObjects().get(i);
            if (obj == null) continue;
            if (obj instanceof IsoFireplace && obj.isLit()) {
                return true;
            }
            if (!(obj instanceof IsoBarbecue) || !obj.isLit()) continue;
            return true;
        }
        return false;
    }

    public boolean hasAdjacentFireObject() {
        for (int i = 0; i < DIRECTIONS.length; ++i) {
            IsoGridSquare testSq = this.getAdjacentSquare(DIRECTIONS[i]);
            if (testSq == null || this.isBlockedTo(testSq) || !testSq.hasFireObject()) continue;
            return true;
        }
        return false;
    }

    public void addGrindstone() {
        GameEntityScript script = ScriptManager.instance.getGameEntityScript("Base.Grindstone");
        if (script == null) {
            return;
        }
        Object sprite = "crafted_01_" + (120 + Rand.Next((int)4));
        if (this.getProperties().has(IsoFlagType.WallNW)) {
            sprite = "crafted_01_" + (120 + Rand.Next((int)2));
        } else if (this.getProperties().has(IsoFlagType.WallN)) {
            sprite = "crafted_01_120";
        } else if (this.getProperties().has(IsoFlagType.WallW)) {
            sprite = "crafted_01_121";
        } else if (this.getAdjacentSquare(IsoDirections.S) != null && this.getAdjacentSquare(IsoDirections.S).getProperties().has(IsoFlagType.WallN)) {
            sprite = "crafted_01_122";
        } else if (this.getAdjacentSquare(IsoDirections.E) != null && this.getAdjacentSquare(IsoDirections.E).getProperties().has(IsoFlagType.WallW)) {
            sprite = "crafted_01_123";
        }
        this.addWorkstationEntity(script, (String)sprite);
    }

    public void addFreezer() {
        Object sprite = "appliances_refrigeration_01_" + (48 + Rand.Next((int)4));
        if (this.getProperties().has(IsoFlagType.WallNW)) {
            sprite = "appliances_refrigeration_01_" + (48 + Rand.Next((int)2));
        } else if (this.getProperties().has(IsoFlagType.WallN)) {
            sprite = "appliances_refrigeration_01_48";
        } else if (this.getProperties().has(IsoFlagType.WallW)) {
            sprite = "appliances_refrigeration_01_49";
        } else if (this.getAdjacentSquare(IsoDirections.S) != null && this.getAdjacentSquare(IsoDirections.S).getProperties().has(IsoFlagType.WallN)) {
            sprite = "appliances_refrigeration_01_50";
        } else if (this.getAdjacentSquare(IsoDirections.E) != null && this.getAdjacentSquare(IsoDirections.E).getProperties().has(IsoFlagType.WallW)) {
            sprite = "appliances_refrigeration_01_51";
        }
        this.addTileObject((String)sprite);
    }

    public void addFloodLights() {
        Object sprite = "lighting_outdoor_01_" + (48 + Rand.Next((int)4));
        if (this.getProperties().has(IsoFlagType.WallNW)) {
            sprite = "lighting_outdoor_01_" + (48 + Rand.Next((int)2));
        } else if (this.getProperties().has(IsoFlagType.WallN)) {
            sprite = "lighting_outdoor_01_01_48";
        } else if (this.getProperties().has(IsoFlagType.WallW)) {
            sprite = "lighting_outdoor_01_01_49";
        } else if (this.getAdjacentSquare(IsoDirections.S) != null && this.getAdjacentSquare(IsoDirections.S).getProperties().has(IsoFlagType.WallN)) {
            sprite = "lighting_outdoor_01_01_51";
        } else if (this.getAdjacentSquare(IsoDirections.E) != null && this.getAdjacentSquare(IsoDirections.E).getProperties().has(IsoFlagType.WallW)) {
            sprite = "lighting_outdoor_01_01_50";
        }
        this.addTileObject((String)sprite);
    }

    public void addSpinningWheel() {
        GameEntityScript script = ScriptManager.instance.getGameEntityScript("Base.Spinning_Wheel");
        if (script == null) {
            return;
        }
        Object sprite = "crafted_04_" + (36 + Rand.Next((int)4));
        if (this.getProperties().has(IsoFlagType.WallNW)) {
            sprite = "crafted_04_" + (36 + Rand.Next((int)2));
        } else if (this.getProperties().has(IsoFlagType.WallN)) {
            sprite = "crafted_04_37";
        } else if (this.getProperties().has(IsoFlagType.WallW)) {
            sprite = "crafted_04_36";
        } else if (this.getAdjacentSquare(IsoDirections.S) != null && this.getAdjacentSquare(IsoDirections.S).getProperties().has(IsoFlagType.WallN)) {
            sprite = "crafted_04_38";
        } else if (this.getAdjacentSquare(IsoDirections.E) != null && this.getAdjacentSquare(IsoDirections.E).getProperties().has(IsoFlagType.WallW)) {
            sprite = "crafted_04_39";
        }
        this.addWorkstationEntity(script, (String)sprite);
    }

    public void addLoom() {
        GameEntityScript script = ScriptManager.instance.getGameEntityScript("Base.Loom");
        if (script == null) {
            return;
        }
        Object sprite = "crafted_04_" + (72 + Rand.Next((int)4));
        if (this.getProperties().has(IsoFlagType.WallNW)) {
            sprite = "crafted_04_" + (72 + Rand.Next((int)2));
        } else if (this.getProperties().has(IsoFlagType.WallN)) {
            sprite = "crafted_04_72";
        } else if (this.getProperties().has(IsoFlagType.WallW)) {
            sprite = "crafted_04_73";
        } else if (this.getAdjacentSquare(IsoDirections.S) != null && this.getAdjacentSquare(IsoDirections.S).getProperties().has(IsoFlagType.WallN)) {
            sprite = "crafted_04_74";
        } else if (this.getAdjacentSquare(IsoDirections.E) != null && this.getAdjacentSquare(IsoDirections.E).getProperties().has(IsoFlagType.WallW)) {
            sprite = "crafted_04_75";
        }
        this.addWorkstationEntity(script, (String)sprite);
    }

    public void addHandPress() {
        GameEntityScript script = ScriptManager.instance.getGameEntityScript("Base.Hand_Press");
        if (script == null) {
            return;
        }
        Object sprite = "crafted_01_" + (72 + Rand.Next((int)2));
        if (this.getProperties().has(IsoFlagType.WallN)) {
            sprite = "crafted_01_72";
        } else if (this.getProperties().has(IsoFlagType.WallW)) {
            sprite = "crafted_01_73";
        }
        this.addWorkstationEntity(script, (String)sprite);
    }

    public IsoThumpable addWorkstationEntity(String scriptString, String sprite) {
        GameEntityScript script = ScriptManager.instance.getGameEntityScript(scriptString);
        return this.addWorkstationEntity(script, sprite);
    }

    public IsoThumpable addWorkstationEntity(GameEntityScript script, String sprite) {
        if (script == null) {
            return null;
        }
        IsoThumpable thumpable = new IsoThumpable(IsoWorld.instance.getCell(), this, sprite, false, null);
        this.addWorkstationEntity(thumpable, script);
        return thumpable;
    }

    public void addWorkstationEntity(IsoThumpable thumpable, GameEntityScript script) {
        thumpable.setHealth(thumpable.getMaxHealth());
        thumpable.setBreakSound(IsoThumpable.DEFAULT_BREAK_SOUND.getSoundName());
        GameEntityFactory.CreateIsoObjectEntity((IsoObject)thumpable, (GameEntityScript)script, (boolean)true);
        this.AddSpecialObject((IsoObject)thumpable);
        thumpable.transmitCompleteItemToClients();
    }

    public boolean isDoorSquare() {
        if (this.getProperties().has(IsoFlagType.DoorWallN) || this.getProperties().has(IsoFlagType.DoorWallW) || this.has(IsoObjectType.doorN) || this.has(IsoObjectType.doorW)) {
            return true;
        }
        if (this.getAdjacentSquare(IsoDirections.S) != null && (this.getAdjacentSquare(IsoDirections.S).getProperties().has(IsoFlagType.DoorWallN) || this.getAdjacentSquare(IsoDirections.S).has(IsoObjectType.doorN))) {
            return true;
        }
        return this.getAdjacentSquare(IsoDirections.E) != null && (this.getAdjacentSquare(IsoDirections.E).getProperties().has(IsoFlagType.DoorWallW) || this.getAdjacentSquare(IsoDirections.E).has(IsoObjectType.doorW));
    }

    public boolean isWallSquare() {
        if (this.getProperties().has(IsoFlagType.WallN) || this.getProperties().has(IsoFlagType.WallW) || this.getProperties().has(IsoFlagType.WallNW)) {
            return true;
        }
        if (this.getAdjacentSquare(IsoDirections.S) != null && this.getAdjacentSquare(IsoDirections.S).getProperties().has(IsoFlagType.WallN)) {
            return true;
        }
        return this.getAdjacentSquare(IsoDirections.E) != null && this.getAdjacentSquare(IsoDirections.E).getProperties().has(IsoFlagType.WallW);
    }

    public boolean isWallSquareNW() {
        return this.getProperties().has(IsoFlagType.WallN) || this.getProperties().has(IsoFlagType.WallW) || this.getProperties().has(IsoFlagType.WallNW);
    }

    public boolean isFreeWallSquare() {
        if ((this.getProperties().has(IsoFlagType.WallN) || this.getProperties().has(IsoFlagType.WallW) || this.getProperties().has(IsoFlagType.WallNW)) && this.getObjects().size() < 3) {
            return true;
        }
        if (this.getAdjacentSquare(IsoDirections.S) != null && this.getAdjacentSquare(IsoDirections.S).getProperties().has(IsoFlagType.WallN) && this.getObjects().size() < 2) {
            return true;
        }
        return this.getAdjacentSquare(IsoDirections.E) != null && this.getAdjacentSquare(IsoDirections.E).getProperties().has(IsoFlagType.WallW) && this.getObjects().size() < 2;
    }

    public boolean isDoorOrWallSquare() {
        return this.isDoorSquare() || this.isWallSquare();
    }

    public void spawnRandomRuralWorkstation() {
        int tool = Rand.Next((int)7);
        switch (tool) {
            case 0: {
                this.addFreezer();
                break;
            }
            case 1: {
                this.addFloodLights();
                break;
            }
            case 2: {
                IsoBarbecue bbq = new IsoBarbecue(IsoWorld.instance.getCell(), this, (IsoSprite)IsoSpriteManager.instance.namedMap.get("crafted_02_52"));
                this.getObjects().add((Object)bbq);
                this.addTileObject("crafted_02_52");
                break;
            }
            case 3: {
                this.addHandPress();
                break;
            }
            case 4: {
                this.addGrindstone();
                break;
            }
            case 5: {
                this.addSpinningWheel();
                break;
            }
            case 6: {
                this.addLoom();
            }
        }
    }

    public void spawnRandomWorkstation() {
        if (this.isRural()) {
            this.spawnRandomRuralWorkstation();
        }
    }

    public boolean isRural() {
        return Objects.equals(this.getSquareRegion(), "General") || this.getSquareRegion() == null;
    }

    public boolean isRuralExtraFussy() {
        Zone zone = this.getZone();
        if (zone != null && ("TownZone".equals(zone.getType()) || "TownZones".equals(zone.getType()) || "TrailerPark".equals(zone.getType()))) {
            return false;
        }
        return Objects.equals(this.getSquareRegion(), "General") || this.getSquareRegion() == null;
    }

    public boolean isFreeWallPair(IsoDirections dir, boolean both) {
        IsoGridSquare sq = this.getAdjacentSquare(dir);
        if (!this.isAdjacentTo(sq) || this.getRoom() == null || sq == null || sq.getRoom() == null || sq.getRoom() != this.getRoom() || !this.canReachTo(sq) || this.isDoorSquare() || this.getObjects().size() > 2 || sq.isDoorSquare() || sq.getObjects().size() > 2) {
            return false;
        }
        boolean good = false;
        int dir2 = dir.ordinal();
        switch (dir2) {
            case 0: {
                if (!this.getProperties().has(IsoFlagType.WallN) && !this.getProperties().has(IsoFlagType.WallNW)) break;
                return false;
            }
            case 2: {
                if (!this.getProperties().has(IsoFlagType.WallW) && !this.getProperties().has(IsoFlagType.WallNW)) break;
                return false;
            }
            case 4: {
                if (!sq.getProperties().has(IsoFlagType.WallN) && !sq.getProperties().has(IsoFlagType.WallNW)) break;
                return false;
            }
            case 6: {
                if (!sq.getProperties().has(IsoFlagType.WallW) && !sq.getProperties().has(IsoFlagType.WallNW)) break;
                return false;
            }
        }
        if (dir2 > 4) {
            dir2 -= 4;
        }
        switch (dir2) {
            case 0: {
                IsoGridSquare sq2;
                if (this.getProperties().has(IsoFlagType.WallW) && sq.getProperties().has(IsoFlagType.WallW)) {
                    return true;
                }
                if (!both || !(sq2 = this.getAdjacentSquare(IsoDirections.E)).getProperties().has(IsoFlagType.WallW) || this.getObjects().size() >= 2 || !sq2.getProperties().has(IsoFlagType.WallW) || sq2.getObjects().size() >= 2) break;
                return true;
            }
            case 2: {
                IsoGridSquare sq2;
                if (this.getProperties().has(IsoFlagType.WallN) && sq.getProperties().has(IsoFlagType.WallN)) {
                    return true;
                }
                if (!both || !(sq2 = this.getAdjacentSquare(IsoDirections.S)).getProperties().has(IsoFlagType.WallN) || this.getObjects().size() >= 2 || !sq2.getProperties().has(IsoFlagType.WallN) || sq2.getObjects().size() >= 2) break;
                return true;
            }
        }
        return false;
    }

    public boolean isGoodSquare() {
        if (this.getFloor() == null) {
            return false;
        }
        if (this.isWaterSquare()) {
            return false;
        }
        if (this.isDoorSquare() || this.HasStairs() || this.getObjects().size() > 2) {
            return false;
        }
        return this.isWallSquareNW() || this.getObjects().size() <= 1;
    }

    public boolean isWaterSquare() {
        if (this.getFloor() == null) {
            return false;
        }
        PropertyContainer props = this.getFloor().getProperties();
        return Objects.equals(props.get("FloorMaterial"), "Water");
    }

    public boolean isGoodOutsideSquare() {
        return this.isOutside() && this.isGoodSquare();
    }

    public void addStump() {
    }

    public static void setBlendFunc() {
        if (PerformanceSettings.fboRenderChunk) {
            IndieGL.glBlendFuncSeparate((int)1, (int)771, (int)773, (int)1);
        } else {
            IndieGL.glDefaultBlendFunc();
        }
    }

    public void invalidateRenderChunkLevel(long dirtyFlags) {
        if (this.chunk == null) {
            return;
        }
        this.chunk.invalidateRenderChunkLevel(this.z, dirtyFlags);
    }

    public void invalidateVispolyChunkLevel() {
        if (this.chunk == null) {
            return;
        }
        this.chunk.invalidateVispolyChunkLevel(this.z);
    }

    public ArrayList<IsoHutch> getHutchTiles(IsoHutch sourceHutch) {
        ArrayList<IsoHutch> result = new ArrayList<IsoHutch>();
        for (int i = 0; i < this.getSpecialObjects().size(); ++i) {
            IsoObject isoObject = this.getSpecialObjects().get(i);
            if (!(isoObject instanceof IsoHutch)) continue;
            IsoHutch hutch = (IsoHutch)isoObject;
            if (sourceHutch != null && hutch.getHutch() != sourceHutch.getHutch()) continue;
            result.add(hutch);
        }
        return result;
    }

    public IsoHutch getHutch() {
        for (int i = 0; i < this.getSpecialObjects().size(); ++i) {
            IsoObject isoObject = this.getSpecialObjects().get(i);
            if (!(isoObject instanceof IsoHutch)) continue;
            IsoHutch hutch = (IsoHutch)isoObject;
            return hutch;
        }
        return null;
    }

    public String getSquareZombiesType() {
        ArrayList<Zone> zones = threadLocalZones.get();
        zones.clear();
        IsoWorld.instance.metaGrid.getZonesAt(this.x, this.y, 0, zones);
        for (int i = 0; i < zones.size(); ++i) {
            Zone zone = zones.get(i);
            if (!StringUtils.equals((String)zone.type, (String)"ZombiesType")) continue;
            return zone.name;
        }
        return null;
    }

    public boolean hasRoomDef() {
        return this.getRoom() != null && this.getRoom().getRoomDef() != null;
    }

    public void spawnRandomGenerator() {
        if (!this.isRural()) {
            this.spawnRandomNewGenerator();
            return;
        }
        String thing = "Base.Generator";
        int tool = Rand.Next((int)4);
        switch (tool) {
            case 0: {
                thing = "Base.Generator";
                break;
            }
            case 1: {
                thing = "Base.Generator_Blue";
                break;
            }
            case 2: {
                thing = "Base.Generator_Yellow";
                break;
            }
            case 3: {
                thing = "Base.Generator_Old";
            }
        }
        IsoGenerator generator = new IsoGenerator(InventoryItemFactory.CreateItem((String)thing), this.getCell(), this);
        DebugLog.log((String)("Special resource tile spawns: " + thing + ", at " + this.x + ", " + this.y));
    }

    public void spawnRandomNewGenerator() {
        String thing = "Base.Generator";
        int tool = Rand.Next((int)3);
        switch (tool) {
            case 0: {
                thing = "Base.Generator";
                break;
            }
            case 1: {
                thing = "Base.Generator_Blue";
                break;
            }
            case 2: {
                thing = "Base.Generator_Yellow";
            }
        }
        IsoGenerator generator = new IsoGenerator(InventoryItemFactory.CreateItem((String)thing), this.getCell(), this);
        DebugLog.log((String)("Special resource tile spawns: " + thing + ", at " + this.x + ", " + this.y));
    }

    public boolean hasGrave() {
        for (int i = 0; i < this.getObjects().size(); ++i) {
            IsoObject obj = (IsoObject)this.getObjects().get(i);
            if (obj == null || !obj.isGrave()) continue;
            return true;
        }
        return false;
    }

    public boolean hasFarmingPlant() {
        return this.getFarmingPlant() != null;
    }

    public GlobalObject getFarmingPlant() {
        if (GameClient.client) {
            CGlobalObjectSystem plantSystem = CGlobalObjects.getSystemByName((String)"farming");
            if (plantSystem == null) {
                return null;
            }
            return plantSystem.getObjectAt(this);
        }
        SGlobalObjectSystem plantSystem = SGlobalObjects.getSystemByName((String)"farming");
        if (plantSystem == null) {
            return null;
        }
        return plantSystem.getObjectAt(this);
    }

    public void destroyFarmingPlant() {
        GlobalObject farmingPlant = this.getFarmingPlant();
        if (farmingPlant == null) {
            return;
        }
        farmingPlant.destroyThisObject();
    }

    public boolean hasLitCampfire() {
        SGlobalObjectSystem campfireSystem = SGlobalObjects.getSystemByName((String)"campfire");
        if (campfireSystem == null) {
            return false;
        }
        GlobalObject campfire = campfireSystem.getObjectAt(this);
        if (campfire == null) {
            return false;
        }
        Object isLit = campfire.getModData().rawget((Object)"isLit");
        if (isLit instanceof Boolean) {
            return (Boolean)isLit;
        }
        if (isLit instanceof String) {
            return Objects.equals(isLit, "true");
        }
        return false;
    }

    public GlobalObject getCampfire() {
        SGlobalObjectSystem campfireSystem = SGlobalObjects.getSystemByName((String)"campfire");
        if (campfireSystem == null) {
            return null;
        }
        return campfireSystem.getObjectAt(this);
    }

    public void putOutCampfire() {
        SGlobalObjectSystem campfireSystem = SGlobalObjects.getSystemByName((String)"campfire");
        if (campfireSystem == null) {
            return;
        }
        GlobalObject campfire = campfireSystem.getObjectAt(this);
        if (campfire == null) {
            return;
        }
        campfire.destroyThisObject();
    }

    private IsoGridSquareCollisionData DoDiagnalCheck(IsoGridSquareCollisionData isoGridSquareCollisionData, int x, int y, int z, boolean bIgnoreDoors) {
        LosUtil.TestResults res = this.testVisionAdjacent(x, 0, z, false, bIgnoreDoors);
        if (res == LosUtil.TestResults.Blocked) {
            isoGridSquareCollisionData.testResults = LosUtil.TestResults.Blocked;
            return isoGridSquareCollisionData;
        }
        LosUtil.TestResults res2 = this.testVisionAdjacent(0, y, z, false, bIgnoreDoors);
        if (res2 == LosUtil.TestResults.Blocked) {
            isoGridSquareCollisionData.testResults = LosUtil.TestResults.Blocked;
            return isoGridSquareCollisionData;
        }
        if (res == LosUtil.TestResults.ClearThroughWindow || res2 == LosUtil.TestResults.ClearThroughWindow) {
            isoGridSquareCollisionData.testResults = LosUtil.TestResults.ClearThroughWindow;
            return isoGridSquareCollisionData;
        }
        return this.getFirstBlocking(isoGridSquareCollisionData, x, y, z, false, bIgnoreDoors);
    }

    public IsoGridSquareCollisionData getFirstBlocking(IsoGridSquareCollisionData isoGridSquareCollisionData, int x, int y, int z, boolean specialDiag, boolean bIgnoreDoors) {
        LosUtil.TestResults test;
        IsoGridSquare sq;
        if (x < -1 || x > 1 || y < -1 || y > 1 || z < -1 || z > 1) {
            isoGridSquareCollisionData.testResults = LosUtil.TestResults.Blocked;
            return isoGridSquareCollisionData;
        }
        if (z == 1 && (x != 0 || y != 0) && this.HasElevatedFloor() && (sq = this.getCell().getGridSquare(this.x, this.y, this.z + z)) != null) {
            return sq.getFirstBlocking(isoGridSquareCollisionData, x, y, 0, specialDiag, bIgnoreDoors);
        }
        if (z == -1 && (x != 0 || y != 0) && (sq = this.getCell().getGridSquare(this.x + x, this.y + y, this.z + z)) != null && sq.HasElevatedFloor()) {
            return this.getFirstBlocking(isoGridSquareCollisionData, x, y, 0, specialDiag, bIgnoreDoors);
        }
        if (x != 0 && y != 0 && specialDiag) {
            IsoGridSquare sq2;
            test = this.DoDiagnalCheck(x, y, z, bIgnoreDoors);
            if ((test == LosUtil.TestResults.Clear || test == LosUtil.TestResults.ClearThroughWindow || test == LosUtil.TestResults.ClearThroughOpenDoor || test == LosUtil.TestResults.ClearThroughClosedDoor) && (sq2 = this.getCell().getGridSquare(this.x + x, this.y + y, this.z + z)) != null) {
                isoGridSquareCollisionData = sq2.DoDiagnalCheck(isoGridSquareCollisionData, -x, -y, -z, bIgnoreDoors);
            }
        } else {
            IsoGridSquare sq3 = this.getCell().getGridSquare(this.x + x, this.y + y, this.z + z);
            LosUtil.TestResults ret = LosUtil.TestResults.Clear;
            if (sq3 != null && sq3.z == this.z) {
                IsoThumpable isoThumpable;
                IsoThumpable isoThumpable2;
                IsoThumpable isoThumpable3;
                IsoDoor isoDoor;
                IsoObject.VisionResult vis;
                IsoObject obj;
                int n;
                if (!this.specialObjects.isEmpty()) {
                    for (n = 0; n < this.specialObjects.size(); ++n) {
                        obj = this.specialObjects.get(n);
                        if (obj == null) {
                            isoGridSquareCollisionData.testResults = LosUtil.TestResults.Clear;
                            return isoGridSquareCollisionData;
                        }
                        vis = obj.TestVision(this, sq3);
                        if (vis == IsoObject.VisionResult.NoEffect) continue;
                        if (vis == IsoObject.VisionResult.Unblocked && obj instanceof IsoDoor) {
                            isoDoor = (IsoDoor)obj;
                            ret = isoDoor.IsOpen() ? LosUtil.TestResults.ClearThroughOpenDoor : LosUtil.TestResults.ClearThroughClosedDoor;
                            continue;
                        }
                        if (vis == IsoObject.VisionResult.Unblocked && obj instanceof IsoThumpable && (isoThumpable3 = (IsoThumpable)obj).isDoor()) {
                            ret = LosUtil.TestResults.ClearThroughOpenDoor;
                            continue;
                        }
                        if (vis == IsoObject.VisionResult.Unblocked && obj instanceof IsoWindow) {
                            ret = LosUtil.TestResults.ClearThroughWindow;
                            continue;
                        }
                        if (vis == IsoObject.VisionResult.Blocked && obj instanceof IsoDoor && !bIgnoreDoors) {
                            isoGridSquareCollisionData.testResults = LosUtil.TestResults.Blocked;
                            return isoGridSquareCollisionData;
                        }
                        if (vis == IsoObject.VisionResult.Blocked && obj instanceof IsoThumpable && (isoThumpable2 = (IsoThumpable)obj).isDoor() && !bIgnoreDoors) {
                            isoGridSquareCollisionData.testResults = LosUtil.TestResults.Blocked;
                            return isoGridSquareCollisionData;
                        }
                        if (vis == IsoObject.VisionResult.Blocked && obj instanceof IsoThumpable && (isoThumpable = (IsoThumpable)obj).isWindow()) {
                            isoGridSquareCollisionData.testResults = LosUtil.TestResults.Blocked;
                            return isoGridSquareCollisionData;
                        }
                        if (vis == IsoObject.VisionResult.Blocked && obj instanceof IsoCurtain) {
                            isoGridSquareCollisionData.testResults = LosUtil.TestResults.Blocked;
                            return isoGridSquareCollisionData;
                        }
                        if (vis == IsoObject.VisionResult.Blocked && obj instanceof IsoWindow) {
                            isoGridSquareCollisionData.testResults = LosUtil.TestResults.Blocked;
                            return isoGridSquareCollisionData;
                        }
                        if (vis != IsoObject.VisionResult.Blocked || !(obj instanceof IsoBarricade)) continue;
                        isoGridSquareCollisionData.testResults = LosUtil.TestResults.Blocked;
                        return isoGridSquareCollisionData;
                    }
                }
                if (!sq3.specialObjects.isEmpty()) {
                    for (n = 0; n < sq3.specialObjects.size(); ++n) {
                        obj = sq3.specialObjects.get(n);
                        if (obj == null) {
                            isoGridSquareCollisionData.testResults = LosUtil.TestResults.Clear;
                            return isoGridSquareCollisionData;
                        }
                        vis = obj.TestVision(this, sq3);
                        if (vis == IsoObject.VisionResult.NoEffect) continue;
                        if (vis == IsoObject.VisionResult.Unblocked && obj instanceof IsoDoor) {
                            isoDoor = (IsoDoor)obj;
                            ret = isoDoor.IsOpen() ? LosUtil.TestResults.ClearThroughOpenDoor : LosUtil.TestResults.ClearThroughClosedDoor;
                            continue;
                        }
                        if (vis == IsoObject.VisionResult.Unblocked && obj instanceof IsoThumpable && (isoThumpable3 = (IsoThumpable)obj).isDoor()) {
                            ret = LosUtil.TestResults.ClearThroughOpenDoor;
                            continue;
                        }
                        if (vis == IsoObject.VisionResult.Unblocked && obj instanceof IsoWindow) {
                            ret = LosUtil.TestResults.ClearThroughWindow;
                            continue;
                        }
                        if (vis == IsoObject.VisionResult.Blocked && obj instanceof IsoDoor && !bIgnoreDoors) {
                            isoGridSquareCollisionData.testResults = LosUtil.TestResults.Blocked;
                            return isoGridSquareCollisionData;
                        }
                        if (vis == IsoObject.VisionResult.Blocked && obj instanceof IsoThumpable && (isoThumpable2 = (IsoThumpable)obj).isDoor() && !bIgnoreDoors) {
                            isoGridSquareCollisionData.testResults = LosUtil.TestResults.Blocked;
                            return isoGridSquareCollisionData;
                        }
                        if (vis == IsoObject.VisionResult.Blocked && obj instanceof IsoThumpable && (isoThumpable = (IsoThumpable)obj).isWindow()) {
                            isoGridSquareCollisionData.testResults = LosUtil.TestResults.Blocked;
                            return isoGridSquareCollisionData;
                        }
                        if (vis == IsoObject.VisionResult.Blocked && obj instanceof IsoCurtain) {
                            isoGridSquareCollisionData.testResults = LosUtil.TestResults.Blocked;
                            return isoGridSquareCollisionData;
                        }
                        if (vis == IsoObject.VisionResult.Blocked && obj instanceof IsoWindow) {
                            isoGridSquareCollisionData.testResults = LosUtil.TestResults.Blocked;
                            return isoGridSquareCollisionData;
                        }
                        if (vis != IsoObject.VisionResult.Blocked || !(obj instanceof IsoBarricade)) continue;
                        isoGridSquareCollisionData.testResults = LosUtil.TestResults.Blocked;
                        return isoGridSquareCollisionData;
                    }
                }
            } else if (z > 0 && sq3 != null && (this.z != -1 || sq3.z != 0) && sq3.getProperties().has(IsoFlagType.exterior) && !this.getProperties().has(IsoFlagType.exterior)) {
                ret = LosUtil.TestResults.Blocked;
            }
            test = ret;
            isoGridSquareCollisionData.testResults = test = !IsoGridSquare.getMatrixBit(this.visionMatrix, x + 1, y + 1, z + 1) ? test : LosUtil.TestResults.Blocked;
            return isoGridSquareCollisionData;
        }
        isoGridSquareCollisionData.testResults = test;
        return isoGridSquareCollisionData;
    }

    private static boolean hasCutawayCapableWallNorth(IsoGridSquare square) {
        boolean bWallLike;
        if (square == null) {
            return false;
        }
        if (square.has(IsoFlagType.WallSE)) {
            return false;
        }
        boolean bl = bWallLike = !(square.getWall(true) == null && !square.has(IsoFlagType.WindowN) || !square.has(IsoFlagType.WallN) && !square.has(IsoFlagType.WallNW) && !square.has(IsoFlagType.DoorWallN) && !square.has(IsoFlagType.WindowN));
        if (!bWallLike) {
            bWallLike = square.getGarageDoor(true) != null;
        }
        return bWallLike;
    }

    private static boolean hasCutawayCapableWallWest(IsoGridSquare square) {
        boolean bWallLike;
        if (square == null) {
            return false;
        }
        if (square.has(IsoFlagType.WallSE)) {
            return false;
        }
        boolean bl = bWallLike = !(square.getWall(false) == null && !square.has(IsoFlagType.WindowW) || !square.has(IsoFlagType.WallW) && !square.has(IsoFlagType.WallNW) && !square.has(IsoFlagType.DoorWallW) && !square.has(IsoFlagType.WindowW));
        if (!bWallLike) {
            bWallLike = square.getGarageDoor(false) != null;
        }
        return bWallLike;
    }

    public boolean canSpawnVermin() {
        if (SandboxOptions.instance.getCurrentRatIndex() <= 0) {
            return false;
        }
        if (this.isVehicleIntersecting() || this.isWaterSquare() || !this.isSolidFloor()) {
            return false;
        }
        if (this.isOutside() && this.z != 0) {
            return false;
        }
        if (this.z < 0) {
            return true;
        }
        if (this.zone != null && ("TownZone".equals(this.zone.getType()) || "TownZones".equals(this.zone.getType()) || "TrailerPark".equals(this.zone.getType()) || "Farm".equals(this.zone.getType()))) {
            return true;
        }
        return this.getSquareRegion() != null || Objects.equals(this.getSquareZombiesType(), "StreetPoor") || Objects.equals(this.getSquareZombiesType(), "TrailerPark") || Objects.equals(this.getLootZone(), "Poor");
    }

    public boolean isNoGas() {
        ArrayList<Zone> zones = threadLocalZones.get();
        zones.clear();
        IsoWorld.instance.metaGrid.getZonesAt(this.x, this.y, 0, zones);
        for (int i = 0; i < zones.size(); ++i) {
            String zoneType = zones.get((int)i).type;
            if (!StringUtils.equals((String)zoneType, (String)"NoGas")) continue;
            return true;
        }
        return false;
    }

    public boolean isNoPower() {
        if (this.isDerelict() || this.isUserDefinedRoom()) {
            return true;
        }
        ArrayList<Zone> zones = threadLocalZones.get();
        zones.clear();
        IsoWorld.instance.metaGrid.getZonesAt(this.x, this.y, 0, zones);
        for (int i = 0; i < zones.size(); ++i) {
            String zoneType = zones.get((int)i).type;
            if (!StringUtils.equals((String)zoneType, (String)"NoPower") && !StringUtils.equals((String)zoneType, (String)"NoPowerOrWater")) continue;
            return true;
        }
        return false;
    }

    public boolean isNoWater() {
        if (this.isDerelict()) {
            return true;
        }
        ArrayList<Zone> zones = threadLocalZones.get();
        zones.clear();
        IsoWorld.instance.metaGrid.getZonesAt(this.x, this.y, 0, zones);
        for (int i = 0; i < zones.size(); ++i) {
            String zoneType = zones.get((int)i).type;
            if (!StringUtils.equals((String)zoneType, (String)"NoWater") && !StringUtils.equals((String)zoneType, (String)"NoPowerOrWater")) continue;
            return true;
        }
        return false;
    }

    public IsoButcherHook getButcherHook() {
        for (int i = 0; i < this.getObjects().size(); ++i) {
            IsoObject obj = (IsoObject)this.getObjects().get(i);
            if (!(obj instanceof IsoButcherHook)) continue;
            IsoButcherHook isoButcherHook = (IsoButcherHook)obj;
            return isoButcherHook;
        }
        return null;
    }

    public boolean isShop() {
        if (this.getRoom() != null) {
            return this.getRoom().isShop();
        }
        return false;
    }

    public boolean hasFireplace() {
        for (int i = 0; i < this.getObjects().size(); ++i) {
            IsoObject obj = (IsoObject)this.getObjects().get(i);
            if (obj.getContainer() == null || obj.getContainer().getType() == null || !obj.getContainer().getType().equals("fireplace")) continue;
            return true;
        }
        return false;
    }

    public IsoDeadBody addCorpse() {
        boolean isSkeleton = Rand.Next((int)(15 - SandboxOptions.instance.timeSinceApo.getValue())) == 0;
        return this.addCorpse(isSkeleton);
    }

    public IsoDeadBody addCorpse(boolean isSkeleton) {
        IsoDeadBody body = null;
        int amount = 14;
        if (Rand.Next((int)10) == 0) {
            amount = 50;
        }
        if (Rand.Next((int)40) == 0) {
            amount = 100;
        }
        for (int m = 0; m < amount; ++m) {
            float rx = (float)Rand.Next((int)3000) / 1000.0f;
            float ry = (float)Rand.Next((int)3000) / 1000.0f;
            this.getChunk().addBloodSplat((float)this.getX() + (rx -= 1.5f), (float)this.getY() + (ry -= 1.5f), this.getZ(), Rand.Next((int)20));
        }
        VirtualZombieManager.instance.choices.clear();
        VirtualZombieManager.instance.choices.add(this);
        IsoZombie zombie = VirtualZombieManager.instance.createRealZombieAlways(IsoDirections.getRandom(), false);
        if (zombie != null) {
            zombie.setX((float)this.x);
            zombie.setY((float)this.y);
            zombie.setFakeDead(false);
            zombie.setHealth(0.0f);
            if (!isSkeleton) {
                zombie.dressInRandomOutfit();
                for (int i = 0; i < 10; ++i) {
                    zombie.addHole(null);
                    zombie.addBlood(null, false, true, false);
                    zombie.addDirt(null, null, false);
                }
                zombie.DoCorpseInventory();
            }
            zombie.setSkeleton(isSkeleton);
            if (isSkeleton) {
                zombie.getHumanVisual().setSkinTextureIndex(1);
            }
            body = new IsoDeadBody((IsoGameCharacter)zombie, true);
            if (!isSkeleton && Rand.Next((int)10) == 0) {
                body.setFakeDead(true);
                if (Rand.Next((int)5) == 0) {
                    body.setCrawling(true);
                }
            }
        }
        return body;
    }

    public IsoDeadBody createCorpse(boolean skeleton) {
        IsoZombie zombie = VirtualZombieManager.instance.createRealZombieAlways(IsoDirections.getRandom(), false);
        return this.createCorpse(zombie, skeleton);
    }

    public IsoDeadBody createCorpse(IsoZombie zombie) {
        return this.createCorpse(zombie, false);
    }

    public IsoDeadBody createCorpse(IsoZombie zombie, boolean skeleton) {
        if (zombie == null) {
            return null;
        }
        VirtualZombieManager.instance.choices.clear();
        VirtualZombieManager.instance.choices.add(this);
        ZombieSpawnRecorder.instance.record(zombie, this.getClass().getSimpleName());
        RandomizedWorldBase.alignCorpseToSquare((IsoGameCharacter)zombie, (IsoGridSquare)this);
        zombie.setFakeDead(false);
        zombie.setHealth(0.0f);
        if (skeleton) {
            zombie.setSkeleton(true);
            zombie.getHumanVisual().setSkinTextureIndex(Rand.Next((int)1, (int)3));
        }
        return new IsoDeadBody((IsoGameCharacter)zombie, true);
    }

    public IsoObject getBed() {
        for (int i = 0; i < this.getObjects().size(); ++i) {
            IsoObject test = (IsoObject)this.getObjects().get(i);
            if (test.getSprite() == null || test.getSprite().getProperties() == null || !test.getSprite().getProperties().has(IsoFlagType.bed)) continue;
            return test;
        }
        return null;
    }

    public IsoObject getPuddleFloor() {
        for (int i = 0; i < this.getObjects().size(); ++i) {
            IsoObject obj = (IsoObject)this.getObjects().get(i);
            if (!obj.hasWater() || obj.getSprite() == null || obj.getSprite().getProperties() == null || !obj.getSprite().getProperties().has(IsoFlagType.solidfloor)) continue;
            return obj;
        }
        return null;
    }

    public void flagForHotSave() {
        if (this.getChunk() != null) {
            this.getChunk().flagForHotSave();
        }
    }

    public boolean hasGridPower() {
        return !this.isNoPower() && SandboxOptions.instance.doesPowerGridExist();
    }

    public boolean hasGridPower(int offset) {
        return !this.isNoPower() && SandboxOptions.instance.doesPowerGridExist(offset);
    }

    public boolean isDerelict() {
        return this.getRoom() != null && this.getRoom().isDerelict();
    }

    public boolean isUserDefinedRoom() {
        return this.getRoomDef() != null && this.getRoomDef().isUserDefined();
    }

    public boolean isUserDefinedBuilding() {
        return this.getBuildingDef() != null && this.getBuildingDef().isUserDefined();
    }

    public boolean shouldNotSpawnActivatedRadiosOrTvs() {
        return this.getRoom() != null && this.getRoom().getName() != null && this.getRoom().getName().toLowerCase().contains("radiofactory");
    }

    public boolean hasFence() {
        for (int i = 0; i < this.getObjects().size(); ++i) {
            IsoObject obj = (IsoObject)this.getObjects().get(i);
            if (obj.isHoppable()) {
                return true;
            }
            if (obj.getSpriteName() != null && obj.getSpriteName().toLowerCase().contains("fence")) {
                return true;
            }
            if (obj.getSpriteName() == null || !obj.getSpriteName().toLowerCase().contains("fencing")) continue;
            return true;
        }
        return false;
    }

    public boolean hasFenceInVicinity() {
        for (int x = -1; x < 1; ++x) {
            for (int y = -1; y < 1; ++y) {
                IsoChunk chunk = IsoWorld.instance.currentCell.getChunkForGridSquare(this.getX() + x * 8, this.getY() + y * 8, this.getZ());
                if (chunk == null || !chunk.hasFence()) continue;
                return true;
            }
        }
        return false;
    }

    public boolean hasFloorOverWater() {
        return this.getProperties().has(IsoFlagType.water) && !this.getProperties().has(IsoFlagType.solidtrans);
    }

    public List<IsoGridSquare> getRadius(int radius) {
        ArrayList<IsoGridSquare> result = new ArrayList<IsoGridSquare>();
        for (int x = this.getX() - (radius /= 2); x <= this.getX() + radius; ++x) {
            for (int y = this.getY() - radius; y <= this.getY() + radius; ++y) {
                IsoGridSquare sq = this.getCell().getGridSquare(x, y, this.getZ());
                if (sq == null) continue;
                result.add(sq);
            }
        }
        return result;
    }

    public IsoGridSquare getSquareBelow() {
        return this.getCell().getGridSquare(this.x, this.y, this.z - 1);
    }

    public boolean canStand() {
        if (this.has(IsoFlagType.solid)) {
            return false;
        }
        if (this.has(IsoFlagType.solidtrans)) {
            return this.isAdjacentToWindow() || this.isAdjacentToHoppable();
        }
        return this.TreatAsSolidFloor();
    }

    public boolean hasAdjacentCanStandSquare() {
        return this.getAdjacentSquare(IsoDirections.NW) != null && this.getAdjacentSquare(IsoDirections.NW).canStand() || this.getAdjacentSquare(IsoDirections.W) != null && this.getAdjacentSquare(IsoDirections.W).canStand() || this.getAdjacentSquare(IsoDirections.SW) != null && this.getAdjacentSquare(IsoDirections.SW).canStand() || this.getAdjacentSquare(IsoDirections.S) != null && this.getAdjacentSquare(IsoDirections.S).canStand() || this.getAdjacentSquare(IsoDirections.SE) != null && this.getAdjacentSquare(IsoDirections.SE).canStand() || this.getAdjacentSquare(IsoDirections.E) != null && this.getAdjacentSquare(IsoDirections.E).canStand() || this.getAdjacentSquare(IsoDirections.NE) != null && this.getAdjacentSquare(IsoDirections.NE).canStand() || this.getAdjacentSquare(IsoDirections.N) != null && this.getAdjacentSquare(IsoDirections.N).canStand();
    }

    public void addAshes() {
        String spriteName = "floors_burnt_01_" + Rand.Next((int)1, (int)2);
        IsoObject burntRemains = new IsoObject(this, spriteName);
        burntRemains.spriteName = spriteName;
        this.getObjects().add((Object)burntRemains);
        burntRemains.transmitCompleteItemToClients();
    }

    private boolean isHorizontalNeighbor(int x1, int y1, int x2, int y2) {
        return x1 != x2 && y1 == y2;
    }

    private boolean isVerticalNeighbor(int x1, int y1, int x2, int y2) {
        return x1 == x2 && y1 != y2;
    }

    private boolean isNorthFacingSequence(IsoObjectType up, IsoObjectType down) {
        return up == IsoObjectType.stairsTN && down == IsoObjectType.stairsMN || up == IsoObjectType.stairsMN && down == IsoObjectType.stairsBN;
    }

    private boolean isWestFacingSequence(IsoObjectType left, IsoObjectType right) {
        return left == IsoObjectType.stairsMW && right == IsoObjectType.stairsTW || left == IsoObjectType.stairsBW && right == IsoObjectType.stairsMW;
    }

    private boolean hasAdjacentStairs(IsoGridSquare square, IsoDirections dir, IsoObjectType type) {
        IsoGridSquare adjacentSquare = square.getAdjacentSquare(dir);
        return adjacentSquare != null && adjacentSquare.has(type);
    }

    private boolean hasConnectingBNStair(IsoObjectType upStairs, IsoObjectType leftStairs, IsoGridSquare upSquare) {
        if (upStairs != IsoObjectType.stairsBN) {
            return false;
        }
        IsoDirections connectionDirection = leftStairs == upStairs ? IsoDirections.E : IsoDirections.W;
        return this.hasAdjacentStairs(upSquare, connectionDirection, IsoObjectType.stairsBN);
    }

    private boolean hasConnectingBWStair(IsoObjectType rightStairs, IsoObjectType leftStairs, IsoGridSquare rightSquare) {
        if (rightStairs != IsoObjectType.stairsBW) {
            return false;
        }
        IsoDirections connectionDirection = leftStairs == rightStairs ? IsoDirections.S : IsoDirections.N;
        return this.hasAdjacentStairs(rightSquare, connectionDirection, IsoObjectType.stairsBW);
    }

    private boolean hasConnectingTNStair(IsoObjectType downStairs, IsoObjectType leftStairs, IsoGridSquare downSquare, IsoGridSquare upSquare) {
        if (downStairs != IsoObjectType.stairsTN || downSquare.z != upSquare.z - 1) {
            return false;
        }
        IsoDirections connectionDirection = leftStairs == downStairs ? IsoDirections.E : IsoDirections.W;
        return this.hasAdjacentStairs(downSquare, connectionDirection, IsoObjectType.stairsTN);
    }

    private boolean hasConnectingTWStair(IsoObjectType leftStairs, IsoObjectType upStairs, IsoGridSquare leftSquare, IsoGridSquare upSquare) {
        if (leftStairs != IsoObjectType.stairsTW || leftSquare.z != upSquare.z - 1) {
            return false;
        }
        IsoDirections connectionDirection = leftStairs == upStairs ? IsoDirections.S : IsoDirections.N;
        return this.hasAdjacentStairs(leftSquare, connectionDirection, IsoObjectType.stairsTW);
    }

    private static /* synthetic */ int lambda$renderMinusFloor$0(IsoWorldInventoryObject o1, IsoWorldInventoryObject o2) {
        float d1 = o1.xoff * o1.xoff + o1.yoff * o1.yoff;
        float d2 = o2.xoff * o2.xoff + o2.yoff * o2.yoff;
        if (d1 == d2) {
            return 0;
        }
        return d1 > d2 ? 1 : -1;
    }

    static {
        idMax = -1;
        col = -1;
        path = -1;
        pathdoor = -1;
        vision = -1;
        rainsplashCache = new String[50];
        waterSplashCache = new Texture[80];
        darkStep = 0.06f;
        defColorInfo = new ColorInfo();
        blackColorInfo = new ColorInfo();
        cellGetSquare = (x, y, z) -> IsoWorld.instance.currentCell.getGridSquare(x, y, z);
        tempWorldInventoryObjects = new PZArrayList(IsoWorldInventoryObject.class, 16);
        isoGridSquareCache = new ConcurrentLinkedQueue();
        ignoreBlockingSprites = new ArrayList();
        choices = new ArrayList();
        lightInfoTemp = new ColorInfo();
        tr = new Color(1, 1, 1, 1);
        tl = new Color(1, 1, 1, 1);
        br = new Color(1, 1, 1, 1);
        bl = new Color(1, 1, 1, 1);
        interp1 = new Color(1, 1, 1, 1);
        interp2 = new Color(1, 1, 1, 1);
        finalCol = new Color(1, 1, 1, 1);
        comp = (a, b) -> a.compareToY((IsoMovingObject)b);
    }
}
