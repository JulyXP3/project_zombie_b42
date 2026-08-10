/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.joml.Vector2i
 *  se.krka.kahlua.integration.annotations.LuaMethod
 *  se.krka.kahlua.vm.JavaFunction
 *  se.krka.kahlua.vm.KahluaTable
 *  se.krka.kahlua.vm.LuaClosure
 *  zombie.GameProfiler
 *  zombie.GameProfiler$ProfileArea
 *  zombie.GameTime
 *  zombie.GameWindow
 *  zombie.IndieGL
 *  zombie.Lua.LuaEventManager
 *  zombie.Lua.LuaHookManager
 *  zombie.Lua.LuaManager
 *  zombie.Lua.MapObjects
 *  zombie.MainThread
 *  zombie.MovingObjectUpdateScheduler
 *  zombie.ReanimatedPlayers
 *  zombie.SandboxOptions
 *  zombie.UsedFromLua
 *  zombie.VirtualZombieManager
 *  zombie.ZomboidFileSystem
 *  zombie.ai.astar.Mover
 *  zombie.characters.IsoGameCharacter
 *  zombie.characters.IsoPlayer
 *  zombie.characters.IsoSurvivor
 *  zombie.characters.IsoZombie
 *  zombie.characters.animals.IsoAnimal
 *  zombie.core.BoxedStaticValues
 *  zombie.core.Core
 *  zombie.core.PerformanceSettings
 *  zombie.core.SpriteRenderer
 *  zombie.core.Translator
 *  zombie.core.logger.ExceptionLogger
 *  zombie.core.math.PZMath
 *  zombie.core.opengl.RenderThread
 *  zombie.core.opengl.Shader
 *  zombie.core.physics.RagdollControllerDebugRenderer
 *  zombie.core.physics.WorldSimulation
 *  zombie.core.profiling.AbstractPerformanceProfileProbe
 *  zombie.core.random.Rand
 *  zombie.core.textures.ColorInfo
 *  zombie.core.textures.Texture
 *  zombie.core.textures.TextureDraw$GenericDrawer
 *  zombie.core.utils.IntGrid
 *  zombie.core.utils.OnceEvery
 *  zombie.debug.DebugLog
 *  zombie.debug.DebugOptions
 *  zombie.debug.DebugType
 *  zombie.debug.LineDrawer
 *  zombie.entity.GameEntity
 *  zombie.erosion.utils.Noise2D
 *  zombie.gameStates.GameLoadingState
 *  zombie.input.AimingReticle
 *  zombie.input.GameKeyboard
 *  zombie.input.JoypadManager
 *  zombie.iso.BuildingDef
 *  zombie.iso.CellLoader
 *  zombie.iso.ChunkSaveWorker
 *  zombie.iso.DiamondMatrixIterator
 *  zombie.iso.FishSchoolManager
 *  zombie.iso.IsoCamera
 *  zombie.iso.IsoCell$BuildingSearchCriteria
 *  zombie.iso.IsoCell$PerPlayerRender
 *  zombie.iso.IsoCell$SnowGrid
 *  zombie.iso.IsoCell$SnowGridTiles
 *  zombie.iso.IsoCell$StencilArea
 *  zombie.iso.IsoCell$s_performance
 *  zombie.iso.IsoCell$s_performance$renderTiles
 *  zombie.iso.IsoCell$s_performance$renderTiles$PerformRenderTilesLayer
 *  zombie.iso.IsoDirections
 *  zombie.iso.IsoGridOcclusionData
 *  zombie.iso.IsoGridOcclusionData$OcclusionFilter
 *  zombie.iso.IsoGridSquare$CircleStencilShader
 *  zombie.iso.IsoGridSquare$CutawayNoDepthShader
 *  zombie.iso.IsoGridStack
 *  zombie.iso.IsoHeatSource
 *  zombie.iso.IsoLightSource
 *  zombie.iso.IsoLot
 *  zombie.iso.IsoMarkers
 *  zombie.iso.IsoMetaGrid
 *  zombie.iso.IsoPuddles
 *  zombie.iso.IsoPushableObject
 *  zombie.iso.IsoRoomLight
 *  zombie.iso.IsoUtils
 *  zombie.iso.IsoWater
 *  zombie.iso.IsoWorld
 *  zombie.iso.LosUtil
 *  zombie.iso.LosUtil$TestResults
 *  zombie.iso.LotHeader
 *  zombie.iso.RoomDef
 *  zombie.iso.RoomDef$RoomRect
 *  zombie.iso.SliceY
 *  zombie.iso.SpriteDetails.IsoFlagType
 *  zombie.iso.SpriteDetails.IsoObjectType
 *  zombie.iso.Vector2
 *  zombie.iso.WorldMarkers
 *  zombie.iso.WorldReuserThread
 *  zombie.iso.areas.BuildingScore
 *  zombie.iso.areas.IsoBuilding
 *  zombie.iso.areas.IsoRoom
 *  zombie.iso.areas.IsoRoomExit
 *  zombie.iso.fboRenderChunk.FBORenderCell
 *  zombie.iso.fboRenderChunk.FBORenderChunkManager
 *  zombie.iso.fboRenderChunk.FBORenderCutaways
 *  zombie.iso.fboRenderChunk.FBORenderSnow
 *  zombie.iso.objects.IsoDeadBody
 *  zombie.iso.objects.IsoTree
 *  zombie.iso.objects.IsoWindow
 *  zombie.iso.sprite.CorpseFlies
 *  zombie.iso.sprite.IsoSprite
 *  zombie.iso.sprite.IsoSpriteManager
 *  zombie.iso.sprite.shapers.FloorShaperAttachedSprites
 *  zombie.iso.sprite.shapers.FloorShaperDiamond
 *  zombie.iso.weather.ClimateManager
 *  zombie.iso.weather.fog.ImprovedFog
 *  zombie.iso.weather.fx.IsoWeatherFX
 *  zombie.iso.weather.fx.WeatherFxMask
 *  zombie.network.GameClient
 *  zombie.network.GameServer
 *  zombie.network.ServerMap
 *  zombie.popman.NetworkZombieSimulator
 *  zombie.savefile.ClientPlayerDB
 *  zombie.savefile.PlayerDB
 *  zombie.scripting.objects.VehicleScript$Position
 *  zombie.ui.UIManager
 *  zombie.vehicles.BaseVehicle
 */
package zombie.iso;

import java.awt.Rectangle;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.joml.Vector2i;
import se.krka.kahlua.integration.annotations.LuaMethod;
import se.krka.kahlua.vm.JavaFunction;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.LuaClosure;
import zombie.GameProfiler;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.IndieGL;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaHookManager;
import zombie.Lua.LuaManager;
import zombie.Lua.MapObjects;
import zombie.MainThread;
import zombie.MovingObjectUpdateScheduler;
import zombie.ReanimatedPlayers;
import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.VirtualZombieManager;
import zombie.ZomboidFileSystem;
import zombie.ai.astar.Mover;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoSurvivor;
import zombie.characters.IsoZombie;
import zombie.characters.animals.IsoAnimal;
import zombie.core.BoxedStaticValues;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.SpriteRenderer;
import zombie.core.Translator;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.core.opengl.RenderThread;
import zombie.core.opengl.Shader;
import zombie.core.physics.RagdollControllerDebugRenderer;
import zombie.core.physics.WorldSimulation;
import zombie.core.profiling.AbstractPerformanceProfileProbe;
import zombie.core.random.Rand;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.core.utils.IntGrid;
import zombie.core.utils.OnceEvery;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.debug.DebugType;
import zombie.debug.LineDrawer;
import zombie.entity.GameEntity;
import zombie.erosion.utils.Noise2D;
import zombie.gameStates.GameLoadingState;
import zombie.input.AimingReticle;
import zombie.input.GameKeyboard;
import zombie.input.JoypadManager;
import zombie.inventory.InventoryItem;
import zombie.iso.BuildingDef;
import zombie.iso.CellLoader;
import zombie.iso.ChunkSaveWorker;
import zombie.iso.DiamondMatrixIterator;
import zombie.iso.FishSchoolManager;
import zombie.iso.IsoCamera;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridOcclusionData;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoGridStack;
import zombie.iso.IsoHeatSource;
import zombie.iso.IsoLightSource;
import zombie.iso.IsoLot;
import zombie.iso.IsoMarkers;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoPuddles;
import zombie.iso.IsoPushableObject;
import zombie.iso.IsoRoomLight;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWater;
import zombie.iso.IsoWorld;
import zombie.iso.LosUtil;
import zombie.iso.LotHeader;
import zombie.iso.RoomDef;
import zombie.iso.SliceY;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.Vector2;
import zombie.iso.WorldMarkers;
import zombie.iso.WorldReuserThread;
import zombie.iso.areas.BuildingScore;
import zombie.iso.areas.IsoBuilding;
import zombie.iso.areas.IsoRoom;
import zombie.iso.areas.IsoRoomExit;
import zombie.iso.fboRenderChunk.FBORenderCell;
import zombie.iso.fboRenderChunk.FBORenderChunkManager;
import zombie.iso.fboRenderChunk.FBORenderCutaways;
import zombie.iso.fboRenderChunk.FBORenderSnow;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoTree;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.iso.sprite.CorpseFlies;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.iso.sprite.shapers.FloorShaperAttachedSprites;
import zombie.iso.sprite.shapers.FloorShaperDiamond;
import zombie.iso.weather.ClimateManager;
import zombie.iso.weather.fog.ImprovedFog;
import zombie.iso.weather.fx.IsoWeatherFX;
import zombie.iso.weather.fx.WeatherFxMask;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.ServerMap;
import zombie.popman.NetworkZombieSimulator;
import zombie.savefile.ClientPlayerDB;
import zombie.savefile.PlayerDB;
import zombie.scripting.objects.VehicleScript;
import zombie.ui.UIManager;
import zombie.vehicles.BaseVehicle;

@UsedFromLua
public final class IsoCell {
    public static final int CELL_SIZE_IN_CHUNKS = 32;
    public static final int CELL_SIZE_IN_SQUARES = 256;
    public static int maxHeight = 32;
    public static Shader floorRenderShader;
    public static Shader wallRenderShader;
    public ArrayList<IsoGridSquare> trees = new ArrayList();
    public int minHeight;
    static final ArrayList<IsoGridSquare> stchoices;
    public final IsoChunkMap[] chunkMap = new IsoChunkMap[4];
    public final ArrayList<IsoBuilding> buildingList = new ArrayList();
    private final ArrayList<IsoWindow> windowList = new ArrayList();
    private final Set<IsoMovingObject> objectList = new HashSet<IsoMovingObject>();
    private final ArrayList<IsoPushableObject> pushableObjectList = new ArrayList();
    private final HashMap<Integer, BuildingScore> buildingScores = new HashMap();
    private final ArrayList<IsoRoom> roomList = new ArrayList();
    private final ArrayList<IsoObject> staticUpdaterObjectList = new ArrayList();
    private final Set<IsoObject> staticUpdaterObjectSet = new HashSet<IsoObject>();
    private final ArrayList<IsoZombie> zombieList = new ArrayList();
    private final ArrayList<IsoGameCharacter> remoteSurvivorList = new ArrayList();
    private final Set<IsoMovingObject> removeList = new HashSet<IsoMovingObject>();
    private final Set<IsoMovingObject> addList = new HashSet<IsoMovingObject>();
    private final ArrayList<IsoObject> processIsoObject = new ArrayList();
    private final Set<IsoObject> processIsoObjectSet = new HashSet<IsoObject>();
    private final Set<IsoObject> processIsoObjectRemove = new HashSet<IsoObject>();
    private final ArrayList<InventoryItem> processItems = new ArrayList();
    private final Set<InventoryItem> processItemsRemove = new HashSet<InventoryItem>();
    private final ArrayList<IsoWorldInventoryObject> processWorldItems = new ArrayList();
    public final Set<IsoWorldInventoryObject> processWorldItemsRemove = new HashSet<IsoWorldInventoryObject>();
    private final IsoGridSquare[][] gridSquares = GameServer.server ? null : new IsoGridSquare[4][IsoChunkMap.chunkWidthInTiles * IsoChunkMap.chunkWidthInTiles * 64];
    public static final boolean ENABLE_SQUARE_CACHE = false;
    private int height;
    private int width;
    private int worldX;
    private int worldY;
    public IntGrid dangerScore;
    private boolean safeToAdd = true;
    private final Stack<IsoLightSource> lamppostPositions = new Stack();
    public final ArrayList<IsoRoomLight> roomLights = new ArrayList();
    private final ArrayList<IsoHeatSource> heatSources = new ArrayList();
    public final Set<BaseVehicle> addVehicles = new HashSet<BaseVehicle>();
    public final Set<BaseVehicle> vehicles = new HashSet<BaseVehicle>();
    public static final int ISOANGLEFACTOR = 3;
    public static final int ZOMBIESCANBUDGET = 10;
    public static final float NEARESTZOMBIEDISTSQRMAX = 150.0f;
    public int zombieScanCursor;
    public final IsoZombie[] nearestVisibleZombie = new IsoZombie[4];
    public final float[] nearestVisibleZombieDistSqr = new float[4];
    private static Stack<BuildingScore> buildingscores;
    public static ArrayList<IsoGridSquare> gridStack;
    public static final int RTF_SolidFloor = 1;
    public static final int RTF_VegetationCorpses = 2;
    public static final int RTF_MinusFloorCharacters = 4;
    public static final int RTF_ShadedFloor = 8;
    public static final int RTF_Shadows = 16;
    public static final ArrayList<IsoGridSquare> ShadowSquares;
    public static final ArrayList<IsoGridSquare> MinusFloorCharacters;
    public static final ArrayList<IsoGridSquare> SolidFloor;
    public static final ArrayList<IsoGridSquare> ShadedFloor;
    public static final ArrayList<IsoGridSquare> VegetationCorpses;
    public static final PerPlayerRender[] perPlayerRender;
    private final Map<String, Texture> stencilTexture = new HashMap<String, Texture>();
    private final List<StencilArea> stencilAreas = new ArrayList<StencilArea>();
    private final DiamondMatrixIterator diamondMatrixIterator = new DiamondMatrixIterator(123);
    private final Vector2i diamondMatrixPos = new Vector2i();
    public int deferredCharacterTick;
    private boolean hasSetupSnowGrid;
    private SnowGridTiles snowGridTilesSquare;
    private SnowGridTiles[] snowGridTilesStrip;
    private SnowGridTiles[] snowGridTilesEdge;
    private SnowGridTiles[] snowGridTilesCove;
    private SnowGridTiles snowGridTilesEnclosed;
    private int snowFirstNonSquare = -1;
    private Noise2D snowNoise2d = new Noise2D();
    private SnowGrid snowGridCur;
    private SnowGrid snowGridPrev;
    private int snowFracTarget;
    private long snowFadeTime;
    private final float snowTransitionTime = 5000.0f;
    private final int raport = 0;
    private static final int SNOWSHORE_NONE = 0;
    private static final int SNOWSHORE_N = 1;
    private static final int SNOWSHORE_E = 2;
    private static final int SNOWSHORE_S = 4;
    private static final int SNOWSHORE_W = 8;
    public boolean recalcFloors;
    static int wx;
    static int wy;
    final KahluaTable[] drag = new KahluaTable[4];
    final ArrayList<IsoSurvivor> survivorList = new ArrayList();
    private static Texture texWhite;
    private static IsoCell instance;
    private int currentLx;
    private int currentLy;
    private int currentLz;
    public int recalcShading = 30;
    public int lastMinX = -1234567;
    public int lastMinY = -1234567;
    private float rainScroll;
    private final int[] rainX = new int[4];
    private final int[] rainY = new int[4];
    private final Texture[] rainTextures = new Texture[5];
    private final long[] rainFileTime = new long[5];
    private float rainAlphaMax = 0.6f;
    private final float[] rainAlpha = new float[4];
    protected int rainIntensity;
    protected int rainSpeed = 6;
    public int lightUpdateCount = 11;
    public boolean rendering;
    public final boolean[] hideFloors = new boolean[4];
    public final int[] unhideFloorsCounter = new int[4];
    public boolean occludedByOrphanStructureFlag;
    public long playerPeekedRoomId = -1L;
    public final ArrayList<ArrayList<IsoBuilding>> playerOccluderBuildings = new ArrayList(4);
    public final IsoBuilding[][] playerOccluderBuildingsArr = new IsoBuilding[4][];
    public final long[] playerWindowPeekingRoomId = new long[4];
    public final boolean[] playerHidesOrphanStructures = new boolean[4];
    public final boolean[] playerCutawaysDirty = new boolean[4];
    final Vector2 tempCutawaySqrVector = new Vector2();
    ArrayList<ArrayList<Long>> tempPrevPlayerCutawayRoomIds = new ArrayList(4);
    public ArrayList<ArrayList<Long>> tempPlayerCutawayRoomIds = new ArrayList(4);
    public final IsoGridSquare[] lastPlayerSquare = new IsoGridSquare[4];
    public final boolean[] lastPlayerSquareHalf = new boolean[4];
    public final IsoDirections[] lastPlayerDir = new IsoDirections[4];
    public final Vector2[] lastPlayerAngle = new Vector2[4];
    public int hidesOrphanStructuresAbove = maxHeight;
    final Rectangle buildingRectTemp = new Rectangle();
    public final ArrayList<ArrayList<IsoBuilding>> zombieOccluderBuildings = new ArrayList(4);
    public final IsoBuilding[][] zombieOccluderBuildingsArr = new IsoBuilding[4][];
    public final IsoGridSquare[] lastZombieSquare = new IsoGridSquare[4];
    public final boolean[] lastZombieSquareHalf = new boolean[4];
    public final ArrayList<ArrayList<IsoBuilding>> otherOccluderBuildings = new ArrayList(4);
    public final IsoBuilding[][] otherOccluderBuildingsArr = new IsoBuilding[4][];
    final int mustSeeSquaresRadius = 4;
    final int mustSeeSquaresGridSize = 10;
    public final ArrayList<IsoGridSquare> gridSquaresTempLeft = new ArrayList(100);
    public final ArrayList<IsoGridSquare> gridSquaresTempRight = new ArrayList(100);
    private IsoWeatherFX weatherFx;
    public int minX;
    public int maxX;
    public int minY;
    public int maxY;
    public int minZ;
    public int maxZ;
    private OnceEvery dangerUpdate = new OnceEvery(0.4f, false);
    private Thread lightInfoUpdate;
    long lastServerItemsUpdate = System.currentTimeMillis();
    private final Stack<IsoRoom> spottedRooms = new Stack();
    private IsoZombie fakeZombieForHit;

    public static int getMaxHeight() {
        return maxHeight;
    }

    public static int getCellSizeInChunks() {
        return 32;
    }

    public static int getCellSizeInSquares() {
        return 256;
    }

    public LotHeader getCurrentLotHeader() {
        IsoGameCharacter isoGameCharacter = IsoCamera.getCameraCharacter();
        IsoChunk ch = this.getChunkForGridSquare(PZMath.fastfloor((float)isoGameCharacter.getX()), PZMath.fastfloor((float)isoGameCharacter.getY()), PZMath.fastfloor((float)isoGameCharacter.getZ()));
        return ch.lotheader;
    }

    public IsoChunkMap getChunkMap(int pl) {
        return this.chunkMap[pl];
    }

    public IsoGridSquare getFreeTile(RoomDef def) {
        stchoices.clear();
        for (int n = 0; n < def.rects.size(); ++n) {
            RoomDef.RoomRect rect = (RoomDef.RoomRect)def.rects.get(n);
            for (int x = rect.x; x < rect.x + rect.w; ++x) {
                for (int y = rect.y; y < rect.y + rect.h; ++y) {
                    IsoGridSquare g = this.getGridSquare(x, y, def.level);
                    if (g == null) continue;
                    g.setCachedIsFree(false);
                    g.setCacheIsFree(false);
                    if (!g.isFree(false)) continue;
                    stchoices.add(g);
                }
            }
        }
        if (stchoices.isEmpty()) {
            return null;
        }
        IsoGridSquare sq = stchoices.get(Rand.Next((int)stchoices.size()));
        stchoices.clear();
        return sq;
    }

    public static Stack<BuildingScore> getBuildings() {
        return buildingscores;
    }

    public static void setBuildings(Stack<BuildingScore> scores) {
        buildingscores = scores;
    }

    public IsoZombie getNearestVisibleZombie(int playerIndex) {
        return this.nearestVisibleZombie[playerIndex];
    }

    public IsoChunk getChunkForGridSquare(int x, int y, int z) {
        if (GameServer.server) {
            return ServerMap.instance.getChunk(x / 8, y / 8);
        }
        int ox = x;
        int oy = y;
        for (int n = 0; n < IsoPlayer.numPlayers; ++n) {
            IsoChunk c;
            if (this.chunkMap[n].ignore) continue;
            x = ox;
            y = oy;
            y -= this.chunkMap[n].getWorldYMinTiles();
            if ((x -= this.chunkMap[n].getWorldXMinTiles()) < 0 || y < 0 || (c = this.chunkMap[n].getChunk(x /= 8, y /= 8)) == null) continue;
            return c;
        }
        return null;
    }

    public IsoChunk getChunk(int wx, int wy) {
        for (int n = 0; n < IsoPlayer.numPlayers; ++n) {
            IsoChunk chunk;
            IsoChunkMap chunkMap = this.chunkMap[n];
            if (chunkMap.ignore || (chunk = chunkMap.getChunk(wx - chunkMap.getWorldXMin(), wy - chunkMap.getWorldYMin())) == null) continue;
            return chunk;
        }
        return null;
    }

    public IsoCell(int width, int height) {
        IsoWorld.instance.currentCell = this;
        instance = this;
        this.width = width;
        this.height = height;
        for (int n = 0; n < 4; ++n) {
            this.chunkMap[n] = new IsoChunkMap(this);
            this.chunkMap[n].playerId = n;
            this.chunkMap[n].ignore = n > 0;
            this.tempPlayerCutawayRoomIds.add(new ArrayList());
            this.tempPrevPlayerCutawayRoomIds.add(new ArrayList());
            this.playerOccluderBuildings.add(new ArrayList(5));
            this.zombieOccluderBuildings.add(new ArrayList(5));
            this.otherOccluderBuildings.add(new ArrayList(5));
        }
        WorldReuserThread.instance.run();
    }

    public void CalculateVertColoursForTile(IsoGridSquare sqThis, int x, int y, int zz, int playerIndex) {
        IsoGridSquare sqUL = !IsoGridSquare.getMatrixBit(sqThis.visionMatrix, 0, 0, 1) ? sqThis.getAdjacentSquare(IsoDirections.NW) : null;
        IsoGridSquare sqU = !IsoGridSquare.getMatrixBit(sqThis.visionMatrix, 1, 0, 1) ? sqThis.getAdjacentSquare(IsoDirections.N) : null;
        IsoGridSquare sqUR = !IsoGridSquare.getMatrixBit(sqThis.visionMatrix, 2, 0, 1) ? sqThis.getAdjacentSquare(IsoDirections.NE) : null;
        IsoGridSquare sqR = !IsoGridSquare.getMatrixBit(sqThis.visionMatrix, 2, 1, 1) ? sqThis.getAdjacentSquare(IsoDirections.E) : null;
        IsoGridSquare sqDR = !IsoGridSquare.getMatrixBit(sqThis.visionMatrix, 2, 2, 1) ? sqThis.getAdjacentSquare(IsoDirections.SE) : null;
        IsoGridSquare sqD = !IsoGridSquare.getMatrixBit(sqThis.visionMatrix, 1, 2, 1) ? sqThis.getAdjacentSquare(IsoDirections.S) : null;
        IsoGridSquare sqDL = !IsoGridSquare.getMatrixBit(sqThis.visionMatrix, 0, 2, 1) ? sqThis.getAdjacentSquare(IsoDirections.SW) : null;
        IsoGridSquare sqL = !IsoGridSquare.getMatrixBit(sqThis.visionMatrix, 0, 1, 1) ? sqThis.getAdjacentSquare(IsoDirections.W) : null;
        this.CalculateColor(sqUL, sqU, sqL, sqThis, 0, playerIndex);
        this.CalculateColor(sqU, sqUR, sqR, sqThis, 1, playerIndex);
        this.CalculateColor(sqDR, sqD, sqR, sqThis, 2, playerIndex);
        this.CalculateColor(sqDL, sqD, sqL, sqThis, 3, playerIndex);
    }

    private Texture getStencilTexture(String filename) {
        if (this.stencilTexture.get(filename) == null) {
            this.stencilTexture.put(filename, Texture.getSharedTexture((String)filename));
        }
        return this.stencilTexture.get(filename);
    }

    public void drawStencilMask() {
        IndieGL.glClear((int)1280);
        this.stencilAreas.clear();
        this.drawStencilMask("media/mask_transparency_player.png", IsoCamera.getOffscreenWidth((int)IsoPlayer.getPlayerIndex()) / 2, IsoCamera.getOffscreenHeight((int)IsoPlayer.getPlayerIndex()) / 2, PZMath.fastfloor((float)IsoCamera.getRightClickOffX()), PZMath.fastfloor((float)IsoCamera.getRightClickOffY()));
        if (IsoPlayer.getPlayer((int)IsoCamera.frameState.playerIndex).isAnyAimKeyDown()) {
            this.drawStencilMask("media/mask_transparency_cursor.png", AimingReticle.getX((int)IsoPlayer.getPlayerIndex()), AimingReticle.getY((int)IsoPlayer.getPlayerIndex()), 0, 0);
        }
    }

    private void drawStencilMask(String filename, int x, int y, int offX, int offY) {
        Texture tex2 = this.getStencilTexture(filename);
        if (tex2 == null) {
            return;
        }
        IndieGL.glStencilMask((int)255);
        IndieGL.glDepthMask((boolean)false);
        int px = x - tex2.getWidth() / (2 / Core.tileScale);
        int py = y - tex2.getHeight() / (2 / Core.tileScale);
        IndieGL.enableStencilTest();
        IndieGL.enableAlphaTest();
        IndieGL.glAlphaFunc((int)516, (float)0.1f);
        IndieGL.glStencilFunc((int)519, (int)128, (int)255);
        IndieGL.glStencilOp((int)7680, (int)7680, (int)7681);
        IndieGL.glColorMask((boolean)false, (boolean)false, (boolean)false, (boolean)false);
        tex2.renderstrip(px - offX, py - offY, tex2.getWidth() * Core.tileScale, tex2.getHeight() * Core.tileScale, 1.0f, 1.0f, 1.0f, 1.0f, null);
        IndieGL.glColorMask((boolean)true, (boolean)true, (boolean)true, (boolean)true);
        IndieGL.glStencilFunc((int)519, (int)0, (int)255);
        IndieGL.glStencilOp((int)7680, (int)7680, (int)7680);
        IndieGL.glStencilMask((int)127);
        IndieGL.glAlphaFunc((int)519, (float)0.0f);
        int stencilX1 = px - offX;
        int stencilY1 = py - offY;
        int stencilX2 = stencilX1 + tex2.getWidth() * Core.tileScale;
        int stencilY2 = stencilY1 + tex2.getHeight() * Core.tileScale;
        this.stencilAreas.add(new StencilArea(stencilX1, stencilY1, stencilX2, stencilY2, x, y, tex2.getWidth(), tex2.getHeight(), offX, offY));
    }

    public boolean isInStencil(float sx, float sy) {
        for (StencilArea stencilArea : this.stencilAreas) {
            if (!(sx + (float)(32 * Core.tileScale) > (float)stencilArea.stencilX1) || !(sx - (float)(32 * Core.tileScale) < (float)stencilArea.stencilX2) || !(sy + (float)(32 * Core.tileScale) > (float)stencilArea.stencilY1) || !(sy - (float)(96 * Core.tileScale) < (float)stencilArea.stencilY2)) continue;
            return true;
        }
        return false;
    }

    public List<StencilArea> getStencilAreas() {
        return this.stencilAreas;
    }

    public void RenderTiles(int maxHeight) {
        try (AbstractPerformanceProfileProbe abstractPerformanceProfileProbe = s_performance.isoCellRenderTiles.profile();){
            this.renderTilesInternal(maxHeight);
        }
    }

    private void renderTilesInternal(int maxHeight) {
        if (!DebugOptions.instance.terrain.renderTiles.enable.getValue()) {
            return;
        }
        if (floorRenderShader == null) {
            RenderThread.invokeOnRenderContext(this::initTileShaders);
        }
        int playerIndex = IsoCamera.frameState.playerIndex;
        IsoPlayer player = IsoPlayer.players[playerIndex];
        player.dirtyRecalcGridStackTime -= GameTime.getInstance().getMultiplier() / 4.0f;
        PerPlayerRender perPlayerRender = this.getPerPlayerRenderAt(playerIndex);
        perPlayerRender.setSize(this.maxX - this.minX + 1, this.maxY - this.minY + 1);
        long currentTimeMillis = System.currentTimeMillis();
        if (this.minX != perPlayerRender.minX || this.minY != perPlayerRender.minY || this.maxX != perPlayerRender.maxX || this.maxY != perPlayerRender.maxY) {
            perPlayerRender.minX = this.minX;
            perPlayerRender.minY = this.minY;
            perPlayerRender.maxX = this.maxX;
            perPlayerRender.maxY = this.maxY;
            player.dirtyRecalcGridStack = true;
            WeatherFxMask.forceMaskUpdate((int)playerIndex);
        }
        boolean recalculatedGridStack = player.dirtyRecalcGridStack;
        try (AbstractPerformanceProfileProbe abstractPerformanceProfileProbe = s_performance.renderTiles.recalculateAnyGridStacks.profile();){
            this.recalculateAnyGridStacks(perPlayerRender, maxHeight, playerIndex, currentTimeMillis);
        }
        ++this.deferredCharacterTick;
        abstractPerformanceProfileProbe = s_performance.renderTiles.flattenAnyFoliage.profile();
        try {
            this.flattenAnyFoliage(perPlayerRender, playerIndex);
        }
        finally {
            if (abstractPerformanceProfileProbe != null) {
                abstractPerformanceProfileProbe.close();
            }
        }
        if (this.SetCutawayRoomsForPlayer() || recalculatedGridStack) {
            IsoGridStack gridStacks = perPlayerRender.gridStacks;
            for (int zza = 0; zza < maxHeight + 1; ++zza) {
                gridStack = (ArrayList)gridStacks.squares.get(zza);
                for (int i = 0; i < gridStack.size(); ++i) {
                    IsoGridSquare square;
                    square.setPlayerCutawayFlag(playerIndex, this.IsCutawaySquare(square = gridStack.get(i), currentTimeMillis) ? 3 : 0, currentTimeMillis);
                }
            }
        }
        abstractPerformanceProfileProbe = s_performance.renderTiles.performRenderTiles.profile();
        try {
            this.performRenderTiles(perPlayerRender, maxHeight, playerIndex, currentTimeMillis);
        }
        finally {
            if (abstractPerformanceProfileProbe != null) {
                abstractPerformanceProfileProbe.close();
            }
        }
        this.playerCutawaysDirty[playerIndex] = false;
        ShadowSquares.clear();
        MinusFloorCharacters.clear();
        ShadedFloor.clear();
        SolidFloor.clear();
        VegetationCorpses.clear();
        abstractPerformanceProfileProbe = s_performance.renderTiles.renderDebugPhysics.profile();
        try {
            this.renderDebugPhysics(playerIndex);
        }
        finally {
            if (abstractPerformanceProfileProbe != null) {
                abstractPerformanceProfileProbe.close();
            }
        }
        abstractPerformanceProfileProbe = s_performance.renderTiles.renderDebugLighting.profile();
        try {
            this.renderDebugLighting(perPlayerRender, maxHeight);
        }
        finally {
            if (abstractPerformanceProfileProbe != null) {
                abstractPerformanceProfileProbe.close();
            }
        }
    }

    public void initTileShaders() {
        if (DebugLog.isEnabled((DebugType)DebugType.Shader)) {
            DebugType.Shader.debugln((Object)"Loading shader: \"floorTile\"");
        }
        floorRenderShader = new Shader("floorTile");
        if (DebugLog.isEnabled((DebugType)DebugType.Shader)) {
            DebugType.Shader.debugln((Object)"Loading shader: \"wallTile\"");
        }
        wallRenderShader = new Shader("wallTile");
        IsoGridSquare.CircleStencilShader inst = IsoGridSquare.CircleStencilShader.instance;
        IsoGridSquare.CutawayNoDepthShader.getInstance();
    }

    public PerPlayerRender getPerPlayerRenderAt(int playerIndex) {
        if (perPlayerRender[playerIndex] == null) {
            IsoCell.perPlayerRender[playerIndex] = new PerPlayerRender();
        }
        return perPlayerRender[playerIndex];
    }

    private void recalculateAnyGridStacks(PerPlayerRender perPlayerRender, int maxHeight, int playerIndex, long currentTimeMillis) {
        int zza;
        IsoPlayer player = IsoPlayer.players[playerIndex];
        if (!player.dirtyRecalcGridStack) {
            return;
        }
        player.dirtyRecalcGridStack = false;
        IsoGridStack gridStacks = perPlayerRender.gridStacks;
        boolean[][][] visiOccludedFlags = perPlayerRender.visiOccludedFlags;
        boolean[][] visiCulledFlags = perPlayerRender.visiCulledFlags;
        int lastChunkX = -1;
        int lastChunkY = -1;
        int currentChunkMaxHeight = -1;
        WeatherFxMask.setDiamondIterDone((int)playerIndex);
        int max = this.chunkMap[playerIndex].maxHeight;
        int min = this.chunkMap[playerIndex].minHeight;
        if (IsoPlayer.getInstance().getZ() < 0.0f) {
            max = Math.min(-1, PZMath.fastfloor((float)IsoPlayer.getInstance().getZ()));
            min = Math.max(min, PZMath.fastfloor((float)IsoPlayer.getInstance().getZ()));
        }
        for (zza = 0; zza < 64; ++zza) {
            gridStack = (ArrayList)gridStacks.squares.get(zza);
            gridStack.clear();
        }
        for (zza = max; zza >= min; --zza) {
            IsoGridSquare square;
            gridStack = (ArrayList)gridStacks.squares.get(zza + 32);
            gridStack.clear();
            if (zza >= this.maxZ) continue;
            if (DebugOptions.instance.terrain.renderTiles.newRender.getValue()) {
                DiamondMatrixIterator iter = this.diamondMatrixIterator.reset(this.maxX - this.minX);
                Vector2i v = this.diamondMatrixPos;
                while (iter.next(v)) {
                    if (v.y >= this.maxY - this.minY + 1) continue;
                    square = this.chunkMap[playerIndex].getGridSquare(v.x + this.minX, v.y + this.minY, zza);
                    if (zza == 0) {
                        visiOccludedFlags[v.x][v.y][0] = false;
                        visiOccludedFlags[v.x][v.y][1] = false;
                        visiCulledFlags[v.x][v.y] = false;
                    }
                    if (square == null) {
                        WeatherFxMask.addMaskLocation(null, (int)(v.x + this.minX), (int)(v.y + this.minY), (int)zza);
                        continue;
                    }
                    IsoChunk c = square.getChunk();
                    if (c == null || !square.IsOnScreen(true)) continue;
                    WeatherFxMask.addMaskLocation((IsoGridSquare)square, (int)(v.x + this.minX), (int)(v.y + this.minY), (int)zza);
                    boolean bDissolved = this.IsDissolvedSquare(square, playerIndex);
                    square.setIsDissolved(playerIndex, bDissolved, currentTimeMillis);
                    if (square.getIsDissolved(playerIndex, currentTimeMillis)) continue;
                    square.cacheLightInfo();
                    gridStack.add(square);
                }
                continue;
            }
            for (int n = this.minY; n < this.maxY; ++n) {
                int x = this.minX;
                square = this.chunkMap[playerIndex].getGridSquare(x, n, zza);
                IsoDirections navStepDirIdx = IsoDirections.E;
                while (x < this.maxX) {
                    IsoChunk test;
                    if (zza == 0) {
                        visiOccludedFlags[x - this.minX][n - this.minY][0] = false;
                        visiOccludedFlags[x - this.minX][n - this.minY][1] = false;
                        visiCulledFlags[x - this.minX][n - this.minY] = false;
                    }
                    if (square != null && square.getY() != n) {
                        square = null;
                    }
                    int lx = x;
                    int ly = n;
                    lx -= (this.chunkMap[playerIndex].worldX - IsoChunkMap.chunkGridWidth / 2) * 8;
                    ly -= (this.chunkMap[playerIndex].worldY - IsoChunkMap.chunkGridWidth / 2) * 8;
                    int chunkX = lx /= 8;
                    int chunkY = ly /= 8;
                    if ((chunkX != lastChunkX || chunkY != lastChunkY) && (test = this.chunkMap[playerIndex].getChunkForGridSquare(x, n)) != null) {
                        currentChunkMaxHeight = test.maxLevel;
                    }
                    lastChunkX = chunkX;
                    lastChunkY = chunkY;
                    if (currentChunkMaxHeight < zza) {
                        ++x;
                        continue;
                    }
                    if (square == null && (square = this.getGridSquare(x, n, zza)) == null && (square = this.chunkMap[playerIndex].getGridSquare(x, n, zza)) == null) {
                        ++x;
                        continue;
                    }
                    IsoChunk c = square.getChunk();
                    if (c != null && square.IsOnScreen(true)) {
                        WeatherFxMask.addMaskLocation((IsoGridSquare)square, (int)square.x, (int)square.y, (int)zza);
                        boolean bDissolved = this.IsDissolvedSquare(square, playerIndex);
                        square.setIsDissolved(playerIndex, bDissolved, currentTimeMillis);
                        if (!square.getIsDissolved(playerIndex, currentTimeMillis)) {
                            square.cacheLightInfo();
                            gridStack.add(square);
                        }
                    }
                    square = square.getAdjacentSquare(navStepDirIdx);
                    ++x;
                }
            }
        }
        this.CullFullyOccludedSquares(gridStacks, visiOccludedFlags, visiCulledFlags);
    }

    public void flattenAnyFoliage(PerPlayerRender perPlayerRender, int playerIndex) {
        boolean[][] flattenGrassEtc = perPlayerRender.flattenGrassEtc;
        for (int y = this.minY; y <= this.maxY; ++y) {
            for (int x = this.minX; x <= this.maxX; ++x) {
                flattenGrassEtc[x - this.minX][y - this.minY] = false;
            }
        }
        for (BaseVehicle vehicle : this.vehicles) {
            if (vehicle.getAlpha(playerIndex) <= 0.0f) continue;
            for (int dy = -2; dy < 5; ++dy) {
                for (int dx = -2; dx < 5; ++dx) {
                    int vx = PZMath.fastfloor((float)vehicle.getX()) + dx;
                    int vy = PZMath.fastfloor((float)vehicle.getY()) + dy;
                    if (vx < this.minX || vx > this.maxX || vy < this.minY || vy > this.maxY) continue;
                    flattenGrassEtc[vx - this.minX][vy - this.minY] = true;
                }
            }
        }
    }

    private void performRenderTiles(PerPlayerRender perPlayerRender, int maxHeight, int playerIndex, long currentTimeMillis) {
        Shader wallRenderShader;
        Shader floorRenderShader;
        IsoGridStack gridStacks = perPlayerRender.gridStacks;
        boolean[][] flattenGrassEtc = perPlayerRender.flattenGrassEtc;
        if (!Core.debug || DebugOptions.instance.terrain.renderTiles.useShaders.getValue()) {
            floorRenderShader = IsoCell.floorRenderShader;
            wallRenderShader = IsoCell.wallRenderShader;
        } else {
            floorRenderShader = null;
            wallRenderShader = null;
        }
        IsoPuddles.getInstance().clearThreadData();
        IsoWater.getInstance().clearThreadData();
        for (int zza = -32; zza < maxHeight + 1; ++zza) {
            IsoGridSquare square;
            int i;
            AbstractPerformanceProfileProbe abstractPerformanceProfileProbe;
            s_performance.renderTiles.PerformRenderTilesLayer performanceLayerProbe = (s_performance.renderTiles.PerformRenderTilesLayer)s_performance.renderTiles.performRenderTilesLayers.start(zza + 32);
            gridStack = (ArrayList)gridStacks.squares.get(zza + 32);
            ShadowSquares.clear();
            SolidFloor.clear();
            ShadedFloor.clear();
            VegetationCorpses.clear();
            MinusFloorCharacters.clear();
            IndieGL.glClear((int)256);
            if (DebugOptions.instance.terrain.renderTiles.water.getValue() && DebugOptions.instance.terrain.renderTiles.waterBody.getValue()) {
                abstractPerformanceProfileProbe = performanceLayerProbe.renderIsoWater.profile();
                try {
                    IsoWater.getInstance().render(gridStack, zza);
                }
                finally {
                    if (abstractPerformanceProfileProbe != null) {
                        abstractPerformanceProfileProbe.close();
                    }
                }
            }
            abstractPerformanceProfileProbe = performanceLayerProbe.renderFloor.profile();
            try {
                for (i = 0; i < gridStack.size(); ++i) {
                    square = gridStack.get(i);
                    if (square.chunk != null && square.chunk.lightingNeverDone[playerIndex]) continue;
                    square.flattenGrassEtc = zza == 0 && flattenGrassEtc[square.x - this.minX][square.y - this.minY];
                    int flags = square.renderFloor(floorRenderShader);
                    if (!square.getStaticMovingObjects().isEmpty()) {
                        flags |= 2;
                        flags |= 0x10;
                        if (square.HasStairs()) {
                            flags |= 4;
                        }
                    }
                    if (!square.getWorldObjects().isEmpty()) {
                        flags |= 2;
                    }
                    for (int m = 0; m < square.getMovingObjects().size(); ++m) {
                        IsoMovingObject mov = square.getMovingObjects().get(m);
                        boolean bOnFloor = mov.onFloor;
                        if (bOnFloor && mov instanceof IsoZombie) {
                            IsoZombie zombie = (IsoZombie)mov;
                            bOnFloor = zombie.isProne();
                            if (!BaseVehicle.renderToTexture) {
                                bOnFloor = false;
                            }
                        }
                        flags = bOnFloor ? (flags |= 2) : (flags |= 4);
                        flags |= 0x10;
                    }
                    if (!square.getDeferedCharacters().isEmpty()) {
                        flags |= 4;
                    }
                    if (square.hasFlies()) {
                        flags |= 4;
                    }
                    if ((flags & 1) != 0) {
                        SolidFloor.add(square);
                    }
                    if ((flags & 8) != 0) {
                        ShadedFloor.add(square);
                    }
                    if ((flags & 2) != 0) {
                        VegetationCorpses.add(square);
                    }
                    if ((flags & 4) != 0) {
                        MinusFloorCharacters.add(square);
                    }
                    if ((flags & 0x10) == 0) continue;
                    ShadowSquares.add(square);
                }
            }
            finally {
                if (abstractPerformanceProfileProbe != null) {
                    abstractPerformanceProfileProbe.close();
                }
            }
            abstractPerformanceProfileProbe = performanceLayerProbe.renderPuddles.profile();
            try {
                IsoPuddles.getInstance().render(SolidFloor, zza);
            }
            finally {
                if (abstractPerformanceProfileProbe != null) {
                    abstractPerformanceProfileProbe.close();
                }
            }
            if (DebugOptions.instance.terrain.renderTiles.water.getValue() && DebugOptions.instance.terrain.renderTiles.waterShore.getValue()) {
                abstractPerformanceProfileProbe = performanceLayerProbe.renderShore.profile();
                try {
                    IsoWater.getInstance().renderShore(gridStack, zza);
                }
                finally {
                    if (abstractPerformanceProfileProbe != null) {
                        abstractPerformanceProfileProbe.close();
                    }
                }
            }
            if (!SolidFloor.isEmpty()) {
                abstractPerformanceProfileProbe = performanceLayerProbe.renderSnow.profile();
                try {
                    this.RenderSnow(zza);
                }
                finally {
                    if (abstractPerformanceProfileProbe != null) {
                        abstractPerformanceProfileProbe.close();
                    }
                }
            }
            if (!gridStack.isEmpty()) {
                abstractPerformanceProfileProbe = performanceLayerProbe.renderBlood.profile();
                try {
                    this.chunkMap[playerIndex].renderBloodForChunks(zza);
                }
                finally {
                    if (abstractPerformanceProfileProbe != null) {
                        abstractPerformanceProfileProbe.close();
                    }
                }
            }
            if (!ShadedFloor.isEmpty()) {
                abstractPerformanceProfileProbe = performanceLayerProbe.renderFloorShading.profile();
                try {
                    this.RenderFloorShading(zza);
                }
                finally {
                    if (abstractPerformanceProfileProbe != null) {
                        abstractPerformanceProfileProbe.close();
                    }
                }
            }
            WorldMarkers.instance.renderGridSquareMarkers(perPlayerRender, zza, playerIndex);
            if (DebugOptions.instance.terrain.renderTiles.shadows.getValue()) {
                abstractPerformanceProfileProbe = performanceLayerProbe.renderShadows.profile();
                try {
                    this.renderShadows();
                }
                finally {
                    if (abstractPerformanceProfileProbe != null) {
                        abstractPerformanceProfileProbe.close();
                    }
                }
            }
            if (DebugOptions.instance.terrain.renderTiles.lua.getValue()) {
                abstractPerformanceProfileProbe = performanceLayerProbe.luaOnPostFloorLayerDraw.profile();
                try {
                    LuaEventManager.triggerEvent((String)"OnPostFloorLayerDraw", (Object)zza);
                }
                finally {
                    if (abstractPerformanceProfileProbe != null) {
                        abstractPerformanceProfileProbe.close();
                    }
                }
            }
            IsoMarkers.instance.renderIsoMarkers(perPlayerRender, zza, playerIndex);
            if (DebugOptions.instance.terrain.renderTiles.vegetationCorpses.getValue()) {
                abstractPerformanceProfileProbe = performanceLayerProbe.vegetationCorpses.profile();
                try {
                    for (i = 0; i < VegetationCorpses.size(); ++i) {
                        square = VegetationCorpses.get(i);
                        boolean cutawaySelf = false;
                        boolean cutawayN = false;
                        boolean cutawayS = false;
                        boolean cutawayW = false;
                        boolean cutawayE = false;
                        square.renderMinusFloor(this.maxZ, false, true, 0, 0, 0, 0, 0, wallRenderShader);
                        square.renderCharacters(this.maxZ, true, true);
                    }
                }
                finally {
                    if (abstractPerformanceProfileProbe != null) {
                        abstractPerformanceProfileProbe.close();
                    }
                }
            }
            ImprovedFog.startRender((int)playerIndex, (int)zza);
            if (DebugOptions.instance.terrain.renderTiles.minusFloorCharacters.getValue()) {
                abstractPerformanceProfileProbe = performanceLayerProbe.minusFloorCharacters.profile();
                try {
                    for (i = 0; i < MinusFloorCharacters.size(); ++i) {
                        square = MinusFloorCharacters.get(i);
                        IsoGridSquare squareN = square.getAdjacentSquare(IsoDirections.N);
                        IsoGridSquare squareS = square.getAdjacentSquare(IsoDirections.S);
                        IsoGridSquare squareW = square.getAdjacentSquare(IsoDirections.W);
                        IsoGridSquare squareE = square.getAdjacentSquare(IsoDirections.E);
                        int cutawaySelf = square.getPlayerCutawayFlag(playerIndex, currentTimeMillis);
                        int cutawayN = squareN == null ? 0 : squareN.getPlayerCutawayFlag(playerIndex, currentTimeMillis);
                        int cutawayS = squareS == null ? 0 : squareS.getPlayerCutawayFlag(playerIndex, currentTimeMillis);
                        int cutawayW = squareW == null ? 0 : squareW.getPlayerCutawayFlag(playerIndex, currentTimeMillis);
                        int cutawayE = squareE == null ? 0 : squareE.getPlayerCutawayFlag(playerIndex, currentTimeMillis);
                        this.currentLy = square.getY() - this.minY;
                        this.currentLz = zza;
                        ImprovedFog.renderRowsBehind((IsoGridSquare)square);
                        boolean hasSE = square.renderMinusFloor(this.maxZ, false, false, cutawaySelf, cutawayN, cutawayS, cutawayW, cutawayE, wallRenderShader);
                        square.renderDeferredCharacters(this.maxZ);
                        square.renderCharacters(this.maxZ, false, true);
                        if (square.hasFlies()) {
                            CorpseFlies.render((int)square.x, (int)square.y, (int)square.z);
                        }
                        if (!hasSE) continue;
                        square.renderMinusFloor(this.maxZ, true, false, cutawaySelf, cutawayN, cutawayS, cutawayW, cutawayE, wallRenderShader);
                    }
                }
                finally {
                    if (abstractPerformanceProfileProbe != null) {
                        abstractPerformanceProfileProbe.close();
                    }
                }
            }
            ImprovedFog.endRender();
            performanceLayerProbe.end();
        }
    }

    public void renderShadows() {
        boolean corpseShadows = Core.getInstance().getOptionCorpseShadows();
        for (int i = 0; i < ShadowSquares.size(); ++i) {
            IsoMovingObject mov;
            int j;
            IsoGridSquare square = ShadowSquares.get(i);
            for (j = 0; j < square.getMovingObjects().size(); ++j) {
                mov = square.getMovingObjects().get(j);
                if (mov instanceof IsoGameCharacter) {
                    IsoGameCharacter chr = (IsoGameCharacter)mov;
                    chr.renderShadow(chr.getX(), chr.getY(), chr.getZ());
                    continue;
                }
                if (!(mov instanceof BaseVehicle)) continue;
                BaseVehicle vehicle = (BaseVehicle)mov;
                vehicle.renderShadow();
            }
            if (!corpseShadows) continue;
            for (j = 0; j < square.getStaticMovingObjects().size(); ++j) {
                mov = square.getStaticMovingObjects().get(j);
                if (!(mov instanceof IsoDeadBody)) continue;
                IsoDeadBody deadBody = (IsoDeadBody)mov;
                deadBody.renderShadow();
            }
        }
    }

    public void renderDebugPhysics(int playerIndex) {
        if (Core.debug && (DebugOptions.instance.physicsRender.getValue() || RagdollControllerDebugRenderer.renderDebugPhysics() || DebugOptions.instance.physicsRenderBallisticsTargets.getValue() || DebugOptions.instance.physicsRenderBallisticsControllers.getValue())) {
            TextureDraw.GenericDrawer drawer = WorldSimulation.getDrawer((int)playerIndex);
            SpriteRenderer.instance.drawGeneric(drawer);
        }
    }

    public void renderDebugLighting(PerPlayerRender perPlayerRender, int maxHeight) {
        if (Core.debug && DebugOptions.instance.lightingRender.getValue()) {
            IsoGridStack gridStacks = perPlayerRender.gridStacks;
            boolean tz = true;
            for (int zza = this.chunkMap[IsoPlayer.getPlayerIndex()].minHeight; zza < maxHeight + 1; ++zza) {
                gridStack = (ArrayList)gridStacks.squares.get(zza + 32);
                for (int i = 0; i < gridStack.size(); ++i) {
                    IsoGridSquare square = gridStack.get(i);
                    float x1 = IsoUtils.XToScreenExact((float)((float)square.x + 0.3f), (float)square.y, (float)0.0f, (int)0);
                    float y1 = IsoUtils.YToScreenExact((float)((float)square.x + 0.3f), (float)square.y, (float)0.0f, (int)0);
                    float x2 = IsoUtils.XToScreenExact((float)((float)square.x + 0.6f), (float)square.y, (float)0.0f, (int)0);
                    float y2 = IsoUtils.YToScreenExact((float)((float)square.x + 0.6f), (float)square.y, (float)0.0f, (int)0);
                    float x3 = IsoUtils.XToScreenExact((float)(square.x + 1), (float)((float)square.y + 0.3f), (float)0.0f, (int)0);
                    float y3 = IsoUtils.YToScreenExact((float)(square.x + 1), (float)((float)square.y + 0.3f), (float)0.0f, (int)0);
                    float x4 = IsoUtils.XToScreenExact((float)(square.x + 1), (float)((float)square.y + 0.6f), (float)0.0f, (int)0);
                    float y4 = IsoUtils.YToScreenExact((float)(square.x + 1), (float)((float)square.y + 0.6f), (float)0.0f, (int)0);
                    float x5 = IsoUtils.XToScreenExact((float)((float)square.x + 0.6f), (float)(square.y + 1), (float)0.0f, (int)0);
                    float y5 = IsoUtils.YToScreenExact((float)((float)square.x + 0.6f), (float)(square.y + 1), (float)0.0f, (int)0);
                    float x6 = IsoUtils.XToScreenExact((float)((float)square.x + 0.3f), (float)(square.y + 1), (float)0.0f, (int)0);
                    float y6 = IsoUtils.YToScreenExact((float)((float)square.x + 0.3f), (float)(square.y + 1), (float)0.0f, (int)0);
                    float x7 = IsoUtils.XToScreenExact((float)square.x, (float)((float)square.y + 0.6f), (float)0.0f, (int)0);
                    float y7 = IsoUtils.YToScreenExact((float)square.x, (float)((float)square.y + 0.6f), (float)0.0f, (int)0);
                    float x8 = IsoUtils.XToScreenExact((float)square.x, (float)((float)square.y + 0.3f), (float)0.0f, (int)0);
                    float y8 = IsoUtils.YToScreenExact((float)square.x, (float)((float)square.y + 0.3f), (float)0.0f, (int)0);
                    if (IsoGridSquare.getMatrixBit(square.visionMatrix, 0, 0, 1)) {
                        LineDrawer.drawLine((float)x1, (float)y1, (float)x2, (float)y2, (float)1.0f, (float)0.0f, (float)0.0f, (float)1.0f, (int)0);
                    }
                    if (IsoGridSquare.getMatrixBit(square.visionMatrix, 0, 1, 1)) {
                        LineDrawer.drawLine((float)x2, (float)y2, (float)x3, (float)y3, (float)1.0f, (float)0.0f, (float)0.0f, (float)1.0f, (int)0);
                    }
                    if (IsoGridSquare.getMatrixBit(square.visionMatrix, 0, 2, 1)) {
                        LineDrawer.drawLine((float)x3, (float)y3, (float)x4, (float)y4, (float)1.0f, (float)0.0f, (float)0.0f, (float)1.0f, (int)0);
                    }
                    if (IsoGridSquare.getMatrixBit(square.visionMatrix, 1, 2, 1)) {
                        LineDrawer.drawLine((float)x4, (float)y4, (float)x5, (float)y5, (float)1.0f, (float)0.0f, (float)0.0f, (float)1.0f, (int)0);
                    }
                    if (IsoGridSquare.getMatrixBit(square.visionMatrix, 2, 2, 1)) {
                        LineDrawer.drawLine((float)x5, (float)y5, (float)x6, (float)y6, (float)1.0f, (float)0.0f, (float)0.0f, (float)1.0f, (int)0);
                    }
                    if (IsoGridSquare.getMatrixBit(square.visionMatrix, 2, 1, 1)) {
                        LineDrawer.drawLine((float)x6, (float)y6, (float)x7, (float)y7, (float)1.0f, (float)0.0f, (float)0.0f, (float)1.0f, (int)0);
                    }
                    if (IsoGridSquare.getMatrixBit(square.visionMatrix, 2, 0, 1)) {
                        LineDrawer.drawLine((float)x7, (float)y7, (float)x8, (float)y8, (float)1.0f, (float)0.0f, (float)0.0f, (float)1.0f, (int)0);
                    }
                    if (!IsoGridSquare.getMatrixBit(square.visionMatrix, 1, 0, 1)) continue;
                    LineDrawer.drawLine((float)x8, (float)y8, (float)x1, (float)y1, (float)1.0f, (float)0.0f, (float)0.0f, (float)1.0f, (int)0);
                }
            }
        }
    }

    private void CullFullyOccludedSquares(IsoGridStack gridStacks, boolean[][][] vf, boolean[][] cf) {
        int aboveGroundSquares = 0;
        for (int earlyOutCheckZ = 1; earlyOutCheckZ < maxHeight + 1; ++earlyOutCheckZ) {
            aboveGroundSquares += ((ArrayList)gridStacks.squares.get(earlyOutCheckZ + 32)).size();
        }
        if (aboveGroundSquares < 500) {
            return;
        }
        int drawnSquares = 0;
        for (int zza = this.chunkMap[IsoPlayer.getPlayerIndex()].maxHeight; zza >= this.chunkMap[IsoPlayer.getPlayerIndex()].minHeight; --zza) {
            gridStack = (ArrayList)gridStacks.squares.get(zza + 32);
            for (int i = gridStack.size() - 1; i >= 0; --i) {
                IsoGridSquare square = gridStack.get(i);
                int projX = square.getX() - zza * 3 - this.minX;
                int projY = square.getY() - zza * 3 - this.minY;
                if (projX < 0 || projX >= cf.length) {
                    gridStack.remove(i);
                    continue;
                }
                if (projY < 0 || projY >= cf[0].length) {
                    gridStack.remove(i);
                    continue;
                }
                if (zza < maxHeight) {
                    boolean bVisible;
                    boolean bl = bVisible = !cf[projX][projY];
                    if (bVisible) {
                        if (projX > 2) {
                            bVisible = projY > 2 ? !vf[projX - 3][projY - 3][0] || !vf[projX - 3][projY - 3][1] || !vf[projX - 3][projY - 2][0] || !vf[projX - 2][projY - 3][1] || !vf[projX - 2][projY - 2][0] || !vf[projX - 2][projY - 2][1] || !vf[projX - 2][projY - 1][0] || !vf[projX - 1][projY - 2][0] || !vf[projX - 1][projY - 1][1] || !vf[projX - 1][projY - 1][0] || !vf[projX - 1][projY][0] || !vf[projX][projY - 1][1] || !vf[projX][projY][0] || !vf[projX][projY][1] : (projY > 1 ? !vf[projX - 3][projY - 2][0] || !vf[projX - 2][projY - 2][0] || !vf[projX - 2][projY - 2][1] || !vf[projX - 2][projY - 1][0] || !vf[projX - 1][projY - 2][0] || !vf[projX - 1][projY - 1][1] || !vf[projX - 1][projY - 1][0] || !vf[projX - 1][projY][0] || !vf[projX][projY - 1][1] || !vf[projX][projY][0] || !vf[projX][projY][1] : (projY > 0 ? !vf[projX - 2][projY - 1][0] || !vf[projX - 1][projY - 1][1] || !vf[projX - 1][projY - 1][0] || !vf[projX - 1][projY][0] || !vf[projX][projY - 1][1] || !vf[projX][projY][0] || !vf[projX][projY][1] : !vf[projX - 1][projY][0] || !vf[projX][projY][0] || !vf[projX][projY][1]));
                        } else if (projX > 1) {
                            bVisible = projY > 2 ? !vf[projX - 2][projY - 3][1] || !vf[projX - 2][projY - 2][0] || !vf[projX - 2][projY - 2][1] || !vf[projX - 2][projY - 1][0] || !vf[projX - 1][projY - 2][0] || !vf[projX - 1][projY - 1][1] || !vf[projX - 1][projY - 1][0] || !vf[projX - 1][projY][0] || !vf[projX][projY - 1][1] || !vf[projX][projY][0] || !vf[projX][projY][1] : (projY > 1 ? !vf[projX - 2][projY - 2][0] || !vf[projX - 2][projY - 2][1] || !vf[projX - 2][projY - 1][0] || !vf[projX - 1][projY - 2][0] || !vf[projX - 1][projY - 1][1] || !vf[projX - 1][projY - 1][0] || !vf[projX - 1][projY][0] || !vf[projX][projY - 1][1] || !vf[projX][projY][0] || !vf[projX][projY][1] : (projY > 0 ? !vf[projX - 2][projY - 1][0] || !vf[projX - 1][projY - 1][1] || !vf[projX - 1][projY - 1][0] || !vf[projX - 1][projY][0] || !vf[projX][projY - 1][1] || !vf[projX][projY][0] || !vf[projX][projY][1] : !vf[projX - 1][projY][0] || !vf[projX][projY][0] || !vf[projX][projY][1]));
                        } else if (projX > 0) {
                            bVisible = projY > 2 ? !vf[projX - 1][projY - 2][0] || !vf[projX - 1][projY - 1][1] || !vf[projX - 1][projY - 1][0] || !vf[projX - 1][projY][0] || !vf[projX][projY - 1][1] || !vf[projX][projY][0] || !vf[projX][projY][1] : (projY > 1 ? !vf[projX - 1][projY - 2][0] || !vf[projX - 1][projY - 1][1] || !vf[projX - 1][projY - 1][0] || !vf[projX - 1][projY][0] || !vf[projX][projY - 1][1] || !vf[projX][projY][0] || !vf[projX][projY][1] : (projY > 0 ? !vf[projX - 1][projY - 1][1] || !vf[projX - 1][projY - 1][0] || !vf[projX - 1][projY][0] || !vf[projX][projY - 1][1] || !vf[projX][projY][0] || !vf[projX][projY][1] : !vf[projX - 1][projY][0] || !vf[projX][projY][0] || !vf[projX][projY][1]));
                        } else if (projY > 2) {
                            bVisible = !vf[projX][projY - 1][1] || !vf[projX][projY][0] || !vf[projX][projY][1];
                        } else if (projY > 1) {
                            bVisible = !vf[projX][projY - 1][1] || !vf[projX][projY][0] || !vf[projX][projY][1];
                        } else if (projY > 0) {
                            bVisible = !vf[projX][projY - 1][1] || !vf[projX][projY][0] || !vf[projX][projY][1];
                        } else {
                            boolean bl2 = bVisible = !vf[projX][projY][0] || !vf[projX][projY][1];
                        }
                    }
                    if (!bVisible) {
                        gridStack.remove(i);
                        cf[projX][projY] = true;
                        continue;
                    }
                }
                ++drawnSquares;
                boolean cutW = IsoGridSquare.getMatrixBit(square.visionMatrix, 0, 1, 1) && square.getProperties().has(IsoFlagType.cutW);
                boolean cutN = IsoGridSquare.getMatrixBit(square.visionMatrix, 1, 0, 1) && square.getProperties().has(IsoFlagType.cutN);
                boolean stenciled = false;
                if (cutW || cutN) {
                    boolean bl = stenciled = ((float)square.x > IsoCamera.frameState.camCharacterX || (float)square.y > IsoCamera.frameState.camCharacterY) && square.z >= PZMath.fastfloor((float)IsoCamera.frameState.camCharacterZ);
                    if (stenciled) {
                        int sx = (int)(square.cachedScreenX - IsoCamera.frameState.offX);
                        int sy = (int)(square.cachedScreenY - IsoCamera.frameState.offY);
                        stenciled = this.isInStencil(sx, sy);
                    }
                }
                int blockedCount = 0;
                if (cutW && !stenciled) {
                    ++blockedCount;
                    if (projX > 0) {
                        vf[projX - 1][projY][0] = true;
                        if (projY > 0) {
                            vf[projX - 1][projY - 1][1] = true;
                        }
                    }
                    if (projX > 1 && projY > 0) {
                        vf[projX - 2][projY - 1][0] = true;
                        if (projY > 1) {
                            vf[projX - 2][projY - 2][1] = true;
                        }
                    }
                    if (projX > 2 && projY > 1) {
                        vf[projX - 3][projY - 2][0] = true;
                        if (projY > 2) {
                            vf[projX - 3][projY - 3][1] = true;
                        }
                    }
                }
                if (cutN && !stenciled) {
                    ++blockedCount;
                    if (projY > 0) {
                        vf[projX][projY - 1][1] = true;
                        if (projX > 0) {
                            vf[projX - 1][projY - 1][0] = true;
                        }
                    }
                    if (projY > 1 && projX > 0) {
                        vf[projX - 1][projY - 2][1] = true;
                        if (projX > 1) {
                            vf[projX - 2][projY - 2][0] = true;
                        }
                    }
                    if (projY > 2 && projX > 1) {
                        vf[projX - 2][projY - 3][1] = true;
                        if (projX > 2) {
                            vf[projX - 3][projY - 3][0] = true;
                        }
                    }
                }
                if (IsoGridSquare.getMatrixBit(square.visionMatrix, 1, 1, 0)) {
                    ++blockedCount;
                    vf[projX][projY][0] = true;
                    vf[projX][projY][1] = true;
                }
                if (blockedCount != 3) continue;
                cf[projX][projY] = true;
            }
        }
    }

    public void RenderFloorShading(int zza) {
        Texture tex;
        if (!DebugOptions.instance.terrain.renderTiles.isoGridSquare.floor.lightingOld.getValue() || DebugOptions.instance.terrain.renderTiles.isoGridSquare.floor.lighting.getValue()) {
            return;
        }
        if (zza >= this.maxZ) {
            return;
        }
        if (Core.debug && DebugOptions.instance.debugDrawSkipWorldShading.getValue()) {
            return;
        }
        if (texWhite == null) {
            texWhite = Texture.getWhite();
        }
        if ((tex = texWhite) == null) {
            return;
        }
        int playerIndex = IsoCamera.frameState.playerIndex;
        int camOffX = (int)IsoCamera.frameState.offX;
        int camOffY = (int)IsoCamera.frameState.offY;
        for (int i = 0; i < ShadedFloor.size(); ++i) {
            IsoGridSquare square = ShadedFloor.get(i);
            if (!square.getProperties().has(IsoFlagType.solidfloor)) continue;
            float offX = 0.0f;
            float offY = 0.0f;
            float offZ = 0.0f;
            if (square.getProperties().has(IsoFlagType.FloorHeightOneThird)) {
                offY = -1.0f;
                offX = -1.0f;
            } else if (square.getProperties().has(IsoFlagType.FloorHeightTwoThirds)) {
                offY = -2.0f;
                offX = -2.0f;
            }
            float sx = IsoUtils.XToScreen((float)((float)square.getX() + offX), (float)((float)square.getY() + offY), (float)((float)zza + 0.0f), (int)0);
            float sy = IsoUtils.YToScreen((float)((float)square.getX() + offX), (float)((float)square.getY() + offY), (float)((float)zza + 0.0f), (int)0);
            sx -= (float)camOffX;
            sy -= (float)camOffY;
            int col0 = square.getVertLight(0, playerIndex);
            int col1 = square.getVertLight(1, playerIndex);
            int col2 = square.getVertLight(2, playerIndex);
            int col3 = square.getVertLight(3, playerIndex);
            if (DebugOptions.instance.terrain.renderTiles.isoGridSquare.floor.lightingDebug.getValue()) {
                col0 = -65536;
                col1 = -65536;
                col2 = -16776961;
                col3 = -16776961;
            }
            tex.renderdiamond(sx - (float)(32 * Core.tileScale), sy + (float)(16 * Core.tileScale), (float)(64 * Core.tileScale), (float)(32 * Core.tileScale), col3, col0, col1, col2);
        }
    }

    public boolean IsPlayerWindowPeeking(int playerIndex) {
        return this.playerWindowPeekingRoomId[playerIndex] != -1L;
    }

    public boolean CanBuildingSquareOccludePlayer(IsoGridSquare square, int playerIndex) {
        ArrayList<IsoBuilding> oc = this.playerOccluderBuildings.get(playerIndex);
        for (int i = 0; i < oc.size(); ++i) {
            IsoBuilding building = oc.get(i);
            int boundsX = building.getDef().getX();
            int boundsY = building.getDef().getY();
            int boundsWidth = building.getDef().getX2() - boundsX;
            int boundsHeight = building.getDef().getY2() - boundsY;
            this.buildingRectTemp.setBounds(boundsX - 1, boundsY - 1, boundsWidth + 2, boundsHeight + 2);
            if (!this.buildingRectTemp.contains(square.getX(), square.getY())) continue;
            return true;
        }
        return false;
    }

    public long GetEffectivePlayerRoomId() {
        int playerIndex = IsoCamera.frameState.playerIndex;
        long playerRoomID = this.playerWindowPeekingRoomId[playerIndex];
        if (IsoPlayer.players[playerIndex] != null && IsoPlayer.players[playerIndex].isClimbing()) {
            playerRoomID = -1L;
        }
        if (playerRoomID != -1L) {
            return playerRoomID;
        }
        IsoGridSquare playerSquare = IsoPlayer.players[playerIndex].current;
        if (playerSquare != null) {
            return playerSquare.getRoomID();
        }
        return -1L;
    }

    public boolean SetCutawayRoomsForPlayer() {
        int playerIndex = IsoCamera.frameState.playerIndex;
        IsoPlayer player = IsoPlayer.players[playerIndex];
        ArrayList<Long> tempList = this.tempPrevPlayerCutawayRoomIds.get(playerIndex);
        this.tempPrevPlayerCutawayRoomIds.set(playerIndex, this.tempPlayerCutawayRoomIds.get(playerIndex));
        this.tempPlayerCutawayRoomIds.set(playerIndex, tempList);
        this.tempPlayerCutawayRoomIds.get(playerIndex).clear();
        IsoGridSquare playerSquare = player.getSquare();
        if (playerSquare == null) {
            return false;
        }
        IsoBuilding playerBuilding = playerSquare.getBuilding();
        long playerRoomId = playerSquare.getRoomID();
        if (playerRoomId == -1L && FBORenderCutaways.getInstance().isRoofRoomSquare(playerSquare)) {
            playerRoomId = playerSquare.associatedBuilding.getRoofRoomID(playerSquare.z);
        }
        boolean bForceCutawayUpdate = false;
        if (playerRoomId == -1L) {
            if (this.playerWindowPeekingRoomId[playerIndex] != -1L) {
                this.tempPlayerCutawayRoomIds.get(playerIndex).add(this.playerWindowPeekingRoomId[playerIndex]);
            } else {
                bForceCutawayUpdate = this.playerCutawaysDirty[playerIndex];
            }
        } else {
            float cutawaySeeRange = 1.5f;
            int minX = (int)(player.getX() - 1.5f);
            int minY = (int)(player.getY() - 1.5f);
            int maxX = (int)(player.getX() + 1.5f);
            int maxY = (int)(player.getY() + 1.5f);
            for (int x = minX; x <= maxX; ++x) {
                for (int y = minY; y <= maxY; ++y) {
                    IsoGridSquare sqr = this.getGridSquare(x, y, playerSquare.getZ());
                    if (sqr == null) continue;
                    long roomID = sqr.getRoomID();
                    if (roomID == -1L && FBORenderCutaways.getInstance().isRoofRoomSquare(sqr)) {
                        roomID = sqr.associatedBuilding.getRoofRoomID(sqr.z);
                    }
                    if (!sqr.getCanSee(playerIndex) && playerSquare != sqr || roomID == -1L || this.tempPlayerCutawayRoomIds.get(playerIndex).contains(roomID)) continue;
                    this.tempCutawaySqrVector.set((float)sqr.getX() + 0.5f - player.getX(), (float)sqr.getY() + 0.5f - player.getY());
                    if (playerSquare != sqr && !(player.getForwardDirection().dot(this.tempCutawaySqrVector) > 0.0f)) continue;
                    this.tempPlayerCutawayRoomIds.get(playerIndex).add(roomID);
                }
            }
            Collections.sort((List)this.tempPlayerCutawayRoomIds.get(playerIndex));
        }
        return bForceCutawayUpdate || !this.tempPlayerCutawayRoomIds.get(playerIndex).equals(this.tempPrevPlayerCutawayRoomIds.get(playerIndex));
    }

    public boolean IsCutawaySquare(IsoGridSquare square, long currentTimeMillis) {
        int playerIndex = IsoCamera.frameState.playerIndex;
        IsoPlayer player = IsoPlayer.players[playerIndex];
        if (player.current == null) {
            return false;
        }
        if (square == null) {
            return false;
        }
        IsoGridSquare playerSquare = player.current;
        if (playerSquare.getZ() != square.getZ()) {
            return false;
        }
        if (this.tempPlayerCutawayRoomIds.get(playerIndex).isEmpty()) {
            IsoGridSquare nSquare = square.getAdjacentSquare(IsoDirections.N);
            IsoGridSquare wSquare = square.getAdjacentSquare(IsoDirections.W);
            if (this.IsCollapsibleBuildingSquare(square)) {
                if (player.getZ() == 0.0f) {
                    return true;
                }
                if (square.getBuilding() != null && (playerSquare.getX() < square.getBuilding().def.x || playerSquare.getY() < square.getBuilding().def.y)) {
                    return true;
                }
                IsoGridSquare checkSquare = square;
                for (int i = 0; i < 3 && (checkSquare = checkSquare.getAdjacentSquare(IsoDirections.NW)) != null; ++i) {
                    if (!checkSquare.isCanSee(playerIndex)) continue;
                    return true;
                }
            }
            if (nSquare != null && nSquare.getRoomID() == -1L && wSquare != null && wSquare.getRoomID() == -1L) {
                return this.DoesSquareHaveValidCutaways(playerSquare, square, playerIndex, currentTimeMillis);
            }
        } else {
            IsoGridSquare nSquare = square.getAdjacentSquare(IsoDirections.N);
            IsoGridSquare eSquare = square.getAdjacentSquare(IsoDirections.E);
            IsoGridSquare sSquare = square.getAdjacentSquare(IsoDirections.S);
            IsoGridSquare wSquare = square.getAdjacentSquare(IsoDirections.W);
            IsoGridSquare nPlayerSquare = playerSquare.getAdjacentSquare(IsoDirections.N);
            IsoGridSquare ePlayerSquare = playerSquare.getAdjacentSquare(IsoDirections.E);
            IsoGridSquare sPlayerSquare = playerSquare.getAdjacentSquare(IsoDirections.S);
            IsoGridSquare wPlayerSquare = playerSquare.getAdjacentSquare(IsoDirections.W);
            boolean isSquareOnEdgeOfRoom = false;
            boolean isSquareNotInPlayerCutawayRooms = false;
            for (int i = 0; i < 8; ++i) {
                if (square.getSurroundingSquares()[i] == null || square.getSurroundingSquares()[i].getRoomID() == square.getRoomID()) continue;
                isSquareOnEdgeOfRoom = true;
                break;
            }
            if (!this.tempPlayerCutawayRoomIds.get(playerIndex).contains(square.getRoomID())) {
                isSquareNotInPlayerCutawayRooms = true;
            }
            if (isSquareOnEdgeOfRoom || isSquareNotInPlayerCutawayRooms || square.getWall() != null) {
                IsoGridSquare checkSquare = square;
                for (int i = 0; i < 3 && (checkSquare = checkSquare.getAdjacentSquare(IsoDirections.NW)) != null; ++i) {
                    if (checkSquare.getRoomID() == -1L || !this.tempPlayerCutawayRoomIds.get(playerIndex).contains(checkSquare.getRoomID())) continue;
                    if ((isSquareOnEdgeOfRoom || isSquareNotInPlayerCutawayRooms) && checkSquare.getCanSee(playerIndex)) {
                        return true;
                    }
                    if (square.getWall() == null || !checkSquare.isCouldSee(playerIndex)) continue;
                    return true;
                }
            }
            if (nSquare != null && wSquare != null && (nSquare.getThumpableWallOrHoppable(false) != null || wSquare.getThumpableWallOrHoppable(true) != null || square.getThumpableWallOrHoppable(true) != null || square.getThumpableWallOrHoppable(false) != null)) {
                return this.DoesSquareHaveValidCutaways(playerSquare, square, playerIndex, currentTimeMillis);
            }
            if (playerSquare.getRoomID() == -1L && (nPlayerSquare != null && nPlayerSquare.getRoomID() != -1L || ePlayerSquare != null && ePlayerSquare.getRoomID() != -1L || sPlayerSquare != null && sPlayerSquare.getRoomID() != -1L || wPlayerSquare != null && wPlayerSquare.getRoomID() != -1L)) {
                int diffX = playerSquare.x - square.x;
                int diffY = playerSquare.y - square.y;
                if (diffX < 0 && diffY < 0) {
                    if (diffX >= -3) {
                        if (diffY >= -3) {
                            return true;
                        }
                        if (nSquare != null && sSquare != null && square.getWall(false) != null && nSquare.getWall(false) != null && sSquare.getWall(false) != null && sSquare.getPlayerCutawayFlag(playerIndex, currentTimeMillis) != 0) {
                            return true;
                        }
                    } else if (eSquare != null && wSquare != null) {
                        if (square.getWall(true) != null && wSquare.getWall(true) != null && eSquare.getWall(true) != null && eSquare.getPlayerCutawayFlag(playerIndex, currentTimeMillis) != 0) {
                            return true;
                        }
                        if (square.getWall(true) != null && wSquare.getWall(true) != null && eSquare.getWall(true) != null && eSquare.getPlayerCutawayFlag(playerIndex, currentTimeMillis) != 0) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public boolean DoesSquareHaveValidCutaways(IsoGridSquare playerSquare, IsoGridSquare square, int playerIndex, long currentTimeMillis) {
        IsoGridSquare nSquare = square.getAdjacentSquare(IsoDirections.N);
        IsoGridSquare eSquare = square.getAdjacentSquare(IsoDirections.E);
        IsoGridSquare sSquare = square.getAdjacentSquare(IsoDirections.S);
        IsoGridSquare wSquare = square.getAdjacentSquare(IsoDirections.W);
        int seeSquareRevealDist = 6;
        IsoObject northWall = square.getWall(true);
        IsoObject westWall = square.getWall(false);
        IsoObject wNorthWall = null;
        IsoObject nWestWall = null;
        if (nSquare != null && nSquare.getAdjacentSquare(IsoDirections.W) != null && nSquare.getAdjacentSquare(IsoDirections.W).getRoomID() == nSquare.getRoomID()) {
            nWestWall = nSquare.getWall(false);
        }
        if (wSquare != null && wSquare.getAdjacentSquare(IsoDirections.N) != null && wSquare.getAdjacentSquare(IsoDirections.N).getRoomID() == wSquare.getRoomID()) {
            wNorthWall = wSquare.getWall(true);
        }
        if (westWall != null || northWall != null || nWestWall != null || wNorthWall != null) {
            IsoGridSquare checkSquare = square.getAdjacentSquare(IsoDirections.NW);
            for (int i = 0; i < 2 && checkSquare != null && checkSquare.getRoomID() == playerSquare.getRoomID(); ++i) {
                IsoGridSquare sCheckSquare = checkSquare.getAdjacentSquare(IsoDirections.S);
                IsoGridSquare eCheckSquare = checkSquare.getAdjacentSquare(IsoDirections.E);
                if (sCheckSquare != null && sCheckSquare.getBuilding() != null || eCheckSquare != null && eCheckSquare.getBuilding() != null) break;
                if (checkSquare.isCanSee(playerIndex) && checkSquare.isCouldSee(playerIndex) && checkSquare.DistTo(playerSquare) <= (float)(6 - (i + 1))) {
                    return true;
                }
                if (checkSquare.getBuilding() != null) continue;
                checkSquare = checkSquare.getAdjacentSquare(IsoDirections.NW);
            }
        }
        int diffX = playerSquare.x - square.x;
        int diffY = playerSquare.y - square.y;
        if (northWall != null && northWall.sprite.name.contains("fencing") || westWall != null && westWall.sprite.name.contains("fencing")) {
            if (northWall != null && wNorthWall != null && diffY >= -6 && diffY < 0) {
                return true;
            }
            if (westWall != null && nWestWall != null && diffX >= -6 && diffX < 0) {
                return true;
            }
        } else if (!(!(square.DistTo(playerSquare) <= 6.0f) || square.getAdjacentSquare(IsoDirections.NW) == null || square.getAdjacentSquare(IsoDirections.NW).getRoomID() != square.getRoomID() || square.getWall(true) != null && square.getWall(true) != northWall || square.getWall(false) != null && square.getWall(false) != westWall)) {
            if (sSquare != null && nSquare != null && diffY != 0) {
                if (diffY > 0 && westWall != null && sSquare.getWall(false) != null && nSquare.getWall(false) != null && sSquare.getPlayerCutawayFlag(playerIndex, currentTimeMillis) != 0) {
                    return true;
                }
                if (diffY < 0 && westWall != null && nSquare.getWall(false) != null && nSquare.getPlayerCutawayFlag(playerIndex, currentTimeMillis) != 0) {
                    return true;
                }
            }
            if (eSquare != null && wSquare != null && diffX != 0) {
                if (diffX > 0 && northWall != null && eSquare.getWall(true) != null && wSquare.getWall(true) != null && eSquare.getPlayerCutawayFlag(playerIndex, currentTimeMillis) != 0) {
                    return true;
                }
                if (diffX < 0 && northWall != null && wSquare.getWall(true) != null && wSquare.getPlayerCutawayFlag(playerIndex, currentTimeMillis) != 0) {
                    return true;
                }
            }
        }
        if (square == playerSquare && square.getAdjacentSquare(IsoDirections.NW) != null && square.getAdjacentSquare(IsoDirections.NW).getRoomID() == square.getRoomID()) {
            if (northWall != null && nSquare != null && nSquare.getWall(false) == null && nSquare.isCanSee(playerIndex) && nSquare.isCouldSee(playerIndex)) {
                return true;
            }
            if (westWall != null && wSquare != null && wSquare.getWall(true) != null && wSquare.isCanSee(playerIndex) && wSquare.isCouldSee(playerIndex)) {
                return true;
            }
        }
        if (nSquare != null && wSquare != null && diffX != 0 && diffY != 0 && nWestWall != null && wNorthWall != null && nSquare.getPlayerCutawayFlag(playerIndex, currentTimeMillis) != 0 && wSquare.getPlayerCutawayFlag(playerIndex, currentTimeMillis) != 0) {
            return true;
        }
        return diffX < 0 && diffX >= -6 && diffY < 0 && diffY >= -6 && (westWall != null && square.getWall(true) == null || northWall != null && square.getWall(false) == null);
    }

    public boolean IsCollapsibleBuildingSquare(IsoGridSquare square) {
        BuildingDef def;
        IsoBuilding building;
        int i;
        if (square.getProperties().has(IsoFlagType.forceRender)) {
            return false;
        }
        for (int playerIter = 0; playerIter < 4; ++playerIter) {
            int size = 500;
            for (i = 0; i < 500 && this.playerOccluderBuildingsArr[playerIter] != null && (building = this.playerOccluderBuildingsArr[playerIter][i]) != null; ++i) {
                def = building.getDef();
                if (this.collapsibleBuildingSquareAlgorithm(def, square, IsoPlayer.players[playerIter].getSquare())) {
                    return true;
                }
                if (square.getY() - def.getY2() == 1 && square.getWall(true) != null) {
                    return true;
                }
                if (square.getX() - def.getX2() != 1 || square.getWall(false) == null) continue;
                return true;
            }
        }
        int playerIndex = IsoCamera.frameState.playerIndex;
        IsoPlayer player = IsoPlayer.players[playerIndex];
        if (player.getVehicle() != null) {
            return false;
        }
        for (i = 0; i < 500 && this.zombieOccluderBuildingsArr[playerIndex] != null && (building = this.zombieOccluderBuildingsArr[playerIndex][i]) != null; ++i) {
            def = building.getDef();
            if (!this.collapsibleBuildingSquareAlgorithm(def, square, player.getSquare())) continue;
            return true;
        }
        for (i = 0; i < 500 && this.otherOccluderBuildingsArr[playerIndex] != null && (building = this.otherOccluderBuildingsArr[playerIndex][i]) != null; ++i) {
            def = building.getDef();
            if (!this.collapsibleBuildingSquareAlgorithm(def, square, player.getSquare())) continue;
            return true;
        }
        return false;
    }

    public boolean collapsibleBuildingSquareAlgorithm(BuildingDef def, IsoGridSquare sq, IsoGridSquare pl) {
        int boundsX = def.getX();
        int boundsY = def.getY();
        int boundsWidth = def.getW();
        int boundsHeight = def.getH();
        this.buildingRectTemp.setBounds(boundsX, boundsY, boundsWidth, boundsHeight);
        if (pl.getRoomID() == -1L && this.buildingRectTemp.contains(pl.getX(), pl.getY())) {
            this.buildingRectTemp.setBounds(boundsX - 1, boundsY - 1, boundsWidth + 2, boundsHeight + 2);
            IsoGridSquare sqN = sq.getAdjacentSquare(IsoDirections.N);
            IsoGridSquare sqW = sq.getAdjacentSquare(IsoDirections.W);
            IsoGridSquare sqNW = sq.getAdjacentSquare(IsoDirections.NW);
            if (sqNW == null || sqN == null || sqW == null) {
                return false;
            }
            boolean sqOut = sq.getRoomID() == -1L;
            boolean sqnOut = sqN.getRoomID() == -1L;
            boolean sqwOut = sqW.getRoomID() == -1L;
            boolean sqnwOut = sqNW.getRoomID() == -1L;
            boolean plN = pl.getY() < sq.getY();
            boolean plW = pl.getX() < sq.getX();
            return this.buildingRectTemp.contains(sq.getX(), sq.getY()) && (pl.getZ() < sq.getZ() || sqOut && (!sqnOut && plN || !sqwOut && plW) || sqOut && sqnOut && sqwOut && !sqnwOut || !sqOut && (sqnwOut || sqnOut == sqwOut || sqnOut && plW || sqwOut && plN));
        }
        this.buildingRectTemp.setBounds(boundsX - 1, boundsY - 1, boundsWidth + 2, boundsHeight + 2);
        return this.buildingRectTemp.contains(sq.getX(), sq.getY());
    }

    private boolean IsDissolvedSquare(IsoGridSquare square, int playerIndex) {
        IsoPlayer player = IsoPlayer.players[playerIndex];
        if (player.current == null) {
            return false;
        }
        IsoGridSquare playerSquare = player.current;
        if (playerSquare.getZ() >= square.getZ()) {
            return false;
        }
        if (!PerformanceSettings.newRoofHiding) {
            return this.hideFloors[playerIndex] && square.getZ() >= this.maxZ;
        }
        if (square.getZ() > this.hidesOrphanStructuresAbove) {
            IsoBuilding squareBuilding = square.getBuilding();
            if (squareBuilding == null) {
                squareBuilding = square.roofHideBuilding;
            }
            for (int dropZ = square.getZ() - 1; dropZ >= 0 && squareBuilding == null; --dropZ) {
                IsoGridSquare testDropSquare = this.getGridSquare(square.x, square.y, dropZ);
                if (testDropSquare == null || (squareBuilding = testDropSquare.getBuilding()) != null) continue;
                squareBuilding = testDropSquare.roofHideBuilding;
            }
            if (squareBuilding == null) {
                IsoGridSquare squareToNorthWest;
                IsoGridSquare squareToWest;
                if (square.isSolidFloor()) {
                    return true;
                }
                IsoGridSquare squareToNorth = square.getAdjacentSquare(IsoDirections.N);
                if (squareToNorth != null && squareToNorth.getBuilding() == null) {
                    if (squareToNorth.getPlayerBuiltFloor() != null) {
                        return true;
                    }
                    if (squareToNorth.HasStairsBelow()) {
                        return true;
                    }
                }
                if ((squareToWest = square.getAdjacentSquare(IsoDirections.W)) != null && squareToWest.getBuilding() == null) {
                    if (squareToWest.getPlayerBuiltFloor() != null) {
                        return true;
                    }
                    if (squareToWest.HasStairsBelow()) {
                        return true;
                    }
                }
                if (square.has(IsoFlagType.WallSE) && (squareToNorthWest = square.getAdjacentSquare(IsoDirections.NW)) != null && squareToNorthWest.getBuilding() == null) {
                    if (squareToNorthWest.getPlayerBuiltFloor() != null) {
                        return true;
                    }
                    if (squareToNorthWest.HasStairsBelow()) {
                        return true;
                    }
                }
            }
        }
        return this.IsCollapsibleBuildingSquare(square);
    }

    private int GetBuildingHeightAt(IsoBuilding building, int inX, int inY, int inZ) {
        for (int z = maxHeight; z > inZ; --z) {
            IsoGridSquare square = this.getGridSquare(inX, inY, z);
            if (square == null || square.getBuilding() != building) continue;
            return z;
        }
        return inZ;
    }

    private void updateSnow(int fracTarget) {
        if (this.snowGridCur == null) {
            this.snowGridCur = new SnowGrid(this, fracTarget);
            this.snowGridPrev = new SnowGrid(this, 0);
            return;
        }
        if (fracTarget != this.snowGridCur.frac) {
            this.snowGridPrev.init(this.snowGridCur.frac);
            this.snowGridCur.init(fracTarget);
            this.snowFadeTime = System.currentTimeMillis();
            DebugLog.log((String)("snow from " + this.snowGridPrev.frac + " to " + this.snowGridCur.frac));
            if (PerformanceSettings.fboRenderChunk) {
                for (int i = 0; i < IsoPlayer.numPlayers; ++i) {
                    IsoPlayer player = IsoPlayer.players[i];
                    if (player == null) continue;
                    player.dirtyRecalcGridStack = true;
                }
            }
        }
    }

    public void setSnowTarget(int target) {
        if (!SandboxOptions.instance.enableSnowOnGround.getValue()) {
            target = 0;
        }
        this.snowFracTarget = target;
    }

    public int getSnowTarget() {
        return this.snowFracTarget;
    }

    public boolean gridSquareIsSnow(int x, int y, int z) {
        if (PerformanceSettings.fboRenderChunk) {
            return FBORenderSnow.getInstance().gridSquareIsSnow(x, y, z);
        }
        IsoGridSquare square = this.getGridSquare(x, y, z);
        if (square != null && this.snowGridCur != null) {
            if (!square.getProperties().has(IsoFlagType.solidfloor)) {
                return false;
            }
            if (square.getProperties().has(IsoFlagType.water) || square.getWater() != null && square.getWater().isValid()) {
                return false;
            }
            if (!square.getProperties().has(IsoFlagType.exterior) || square.room != null || square.isInARoom()) {
                return false;
            }
            int snowX = square.getX() % this.snowGridCur.w;
            int snowY = square.getY() % this.snowGridCur.h;
            return this.snowGridCur.check(snowX, snowY);
        }
        return false;
    }

    public void RenderSnow(int zza) {
        if (!DebugOptions.instance.weather.snow.getValue()) {
            return;
        }
        this.updateSnow(this.snowFracTarget);
        SnowGrid snowGridCur = this.snowGridCur;
        if (snowGridCur == null) {
            return;
        }
        SnowGrid snowGridPrev = this.snowGridPrev;
        if (snowGridCur.frac <= 0 && snowGridPrev.frac <= 0) {
            return;
        }
        float alphaCur = 1.0f;
        float alphaPrev = 0.0f;
        long ms = System.currentTimeMillis();
        long fadeDt = ms - this.snowFadeTime;
        if ((float)fadeDt < 5000.0f) {
            alphaCur = (float)fadeDt / 5000.0f;
            alphaPrev = 1.0f - alphaCur;
        }
        if (PerformanceSettings.fboRenderChunk && FBORenderChunkManager.instance.isCaching()) {
            alphaCur = 1.0f;
            alphaPrev = 0.0f;
        }
        Shader floorRenderShader = null;
        if (DebugOptions.instance.terrain.renderTiles.useShaders.getValue()) {
            floorRenderShader = IsoCell.floorRenderShader;
        }
        FloorShaperAttachedSprites.instance.setShore(false);
        FloorShaperDiamond.instance.setShore(false);
        IndieGL.StartShader((Shader)floorRenderShader, (int)IsoCamera.frameState.playerIndex);
        int camOffX = (int)IsoCamera.frameState.offX;
        int camOffY = (int)IsoCamera.frameState.offY;
        for (int i = 0; i < SolidFloor.size(); ++i) {
            int shore;
            IsoGridSquare square = SolidFloor.get(i);
            if (square.room != null || !square.getProperties().has(IsoFlagType.exterior) || !square.getProperties().has(IsoFlagType.solidfloor)) continue;
            if (square.getProperties().has(IsoFlagType.water) || square.getWater() != null && square.getWater().isValid()) {
                shore = IsoCell.getShoreInt(square);
                if (shore == 0) {
                    continue;
                }
            } else {
                shore = 0;
            }
            int snowX = square.getX() % snowGridCur.w;
            int snowY = square.getY() % snowGridCur.h;
            float sx = IsoUtils.XToScreen((float)square.getX(), (float)square.getY(), (float)zza, (int)0);
            float sy = IsoUtils.YToScreen((float)square.getX(), (float)square.getY(), (float)zza, (int)0);
            sx -= (float)camOffX;
            sy -= (float)camOffY;
            if (PerformanceSettings.fboRenderChunk && FBORenderChunkManager.instance.isCaching()) {
                sx = IsoUtils.XToScreen((float)(square.getX() % 8), (float)(square.getY() % 8), (float)zza, (int)0);
                sy = IsoUtils.YToScreen((float)(square.getX() % 8), (float)(square.getY() % 8), (float)zza, (int)0);
                sx += FBORenderChunkManager.instance.getXOffset();
                sy += FBORenderChunkManager.instance.getYOffset();
            }
            float offsetX = 32 * Core.tileScale;
            float offsetY = 96 * Core.tileScale;
            sx -= offsetX;
            sy -= offsetY;
            int playerIndex = IsoCamera.frameState.playerIndex;
            int col0 = square.getVertLight(0, playerIndex);
            int col1 = square.getVertLight(1, playerIndex);
            int col2 = square.getVertLight(2, playerIndex);
            int col3 = square.getVertLight(3, playerIndex);
            if (DebugOptions.instance.terrain.renderTiles.isoGridSquare.floor.lightingDebug.getValue()) {
                col0 = -65536;
                col1 = -65536;
                col2 = -16776961;
                col3 = -16776961;
            }
            FloorShaperAttachedSprites.instance.setVertColors(col0, col1, col2, col3);
            FloorShaperDiamond.instance.setVertColors(col0, col1, col2, col3);
            for (int s = 0; s < 2; ++s) {
                if (alphaPrev > alphaCur) {
                    this.renderSnowTileGeneral(snowGridCur, alphaCur, square, shore, snowX, snowY, (int)sx, (int)sy, s);
                    this.renderSnowTileGeneral(snowGridPrev, alphaPrev, square, shore, snowX, snowY, (int)sx, (int)sy, s);
                    continue;
                }
                this.renderSnowTileGeneral(snowGridPrev, alphaPrev, square, shore, snowX, snowY, (int)sx, (int)sy, s);
                this.renderSnowTileGeneral(snowGridCur, alphaCur, square, shore, snowX, snowY, (int)sx, (int)sy, s);
            }
        }
        IndieGL.StartShader(null);
    }

    private void renderSnowTileGeneral(SnowGrid snowGrid, float alpha, IsoGridSquare square, int shore, int snowX, int snowY, int sx, int sy, int s) {
        if (alpha <= 0.0f) {
            return;
        }
        Texture tex = snowGrid.grid[snowX][snowY][s];
        if (tex == null) {
            return;
        }
        if (s == 0) {
            this.renderSnowTile(snowGrid, snowX, snowY, s, square, shore, tex, sx, sy, alpha);
        } else if (shore == 0) {
            byte id = snowGrid.gridType[snowX][snowY][s];
            this.renderSnowTileBase(tex, sx, sy, alpha, id < this.snowFirstNonSquare);
        }
    }

    private void renderSnowTileBase(Texture tex, int sx, int sy, float alpha, boolean square) {
        FloorShaperDiamond shaper = square ? FloorShaperDiamond.instance : FloorShaperAttachedSprites.instance;
        shaper.setAlpha4(alpha);
        tex.render((float)sx, (float)sy, (float)tex.getWidth(), (float)tex.getHeight(), 1.0f, 1.0f, 1.0f, alpha, (Consumer)shaper);
    }

    private void renderSnowTile(SnowGrid sgrid, int gx, int gy, int s, IsoGridSquare sq, int shore, Texture tex, int sx, int sy, float alpha) {
        boolean bW;
        if (shore == 0) {
            byte id = sgrid.gridType[gx][gy][s];
            this.renderSnowTileBase(tex, sx, sy, alpha, id < this.snowFirstNonSquare);
            return;
        }
        int count = 0;
        boolean selfIsSnowFull = sgrid.check(gx, gy);
        boolean bN = (shore & 1) == 1 && (selfIsSnowFull || sgrid.check(gx, gy - 1));
        boolean bE = (shore & 2) == 2 && (selfIsSnowFull || sgrid.check(gx + 1, gy));
        boolean bS = (shore & 4) == 4 && (selfIsSnowFull || sgrid.check(gx, gy + 1));
        boolean bl = bW = (shore & 8) == 8 && (selfIsSnowFull || sgrid.check(gx - 1, gy));
        if (bN) {
            ++count;
        }
        if (bS) {
            ++count;
        }
        if (bE) {
            ++count;
        }
        if (bW) {
            ++count;
        }
        SnowGridTiles sTiles = null;
        SnowGridTiles sTiles2 = null;
        boolean square = false;
        if (count == 0) {
            return;
        }
        if (count == 1) {
            if (bN) {
                sTiles = this.snowGridTilesStrip[0];
            } else if (bS) {
                sTiles = this.snowGridTilesStrip[1];
            } else if (bE) {
                sTiles = this.snowGridTilesStrip[3];
            } else if (bW) {
                sTiles = this.snowGridTilesStrip[2];
            }
        } else if (count == 2) {
            if (bN && bS) {
                sTiles = this.snowGridTilesStrip[0];
                sTiles2 = this.snowGridTilesStrip[1];
            } else if (bE && bW) {
                sTiles = this.snowGridTilesStrip[2];
                sTiles2 = this.snowGridTilesStrip[3];
            } else if (bN) {
                sTiles = this.snowGridTilesEdge[bW ? 0 : 3];
            } else if (bS) {
                sTiles = this.snowGridTilesEdge[bW ? 2 : 1];
            } else if (bW) {
                sTiles = this.snowGridTilesEdge[bN ? 0 : 2];
            } else if (bE) {
                sTiles = this.snowGridTilesEdge[bN ? 3 : 1];
            }
        } else if (count == 3) {
            if (!bN) {
                sTiles = this.snowGridTilesCove[1];
            } else if (!bS) {
                sTiles = this.snowGridTilesCove[0];
            } else if (!bE) {
                sTiles = this.snowGridTilesCove[2];
            } else if (!bW) {
                sTiles = this.snowGridTilesCove[3];
            }
            square = true;
        } else if (count == 4) {
            sTiles = this.snowGridTilesEnclosed;
            square = true;
        }
        if (sTiles != null) {
            int var = (sq.getX() + sq.getY()) % sTiles.size();
            tex = sTiles.get(var);
            if (tex != null) {
                this.renderSnowTileBase(tex, sx, sy, alpha, square);
            }
            if (sTiles2 != null && (tex = sTiles2.get(var)) != null) {
                this.renderSnowTileBase(tex, sx, sy, alpha, false);
            }
        }
    }

    private static int getShoreInt(IsoGridSquare sq) {
        int shore = 0;
        if (IsoCell.isSnowShore(sq, 0, -1)) {
            shore |= 1;
        }
        if (IsoCell.isSnowShore(sq, 1, 0)) {
            shore |= 2;
        }
        if (IsoCell.isSnowShore(sq, 0, 1)) {
            shore |= 4;
        }
        if (IsoCell.isSnowShore(sq, -1, 0)) {
            shore |= 8;
        }
        return shore;
    }

    private static boolean isSnowShore(IsoGridSquare sq, int ox, int oy) {
        IsoGridSquare square = IsoWorld.instance.getCell().getGridSquare(sq.getX() + ox, sq.getY() + oy, 0);
        return square != null && !square.getProperties().has(IsoFlagType.water);
    }

    public IsoBuilding getClosestBuildingExcept(IsoGameCharacter chr, IsoRoom except) {
        IsoBuilding building = null;
        float lowest = 1000000.0f;
        for (int n = 0; n < this.buildingList.size(); ++n) {
            IsoBuilding b = this.buildingList.get(n);
            for (int m = 0; m < b.exits.size(); ++m) {
                float dist = chr.DistTo(((IsoRoomExit)b.exits.get((int)m)).x, ((IsoRoomExit)b.exits.get((int)m)).y);
                if (!(dist < lowest) || except != null && except.building == b) continue;
                building = b;
                lowest = dist;
            }
        }
        return building;
    }

    public int getDangerScore(int x, int y) {
        if (x < 0 || y < 0 || x >= this.width || y >= this.height) {
            return 1000000;
        }
        return this.dangerScore.getValue(x, y);
    }

    private void ObjectDeletionAddition() {
        for (IsoMovingObject r : this.removeList) {
            IsoPlayer isoPlayer;
            if (r instanceof IsoZombie) {
                IsoZombie isoZombie = (IsoZombie)r;
                VirtualZombieManager.instance.RemoveZombie(isoZombie);
            }
            if (r instanceof IsoPlayer && !(isoPlayer = (IsoPlayer)r).isDead()) continue;
            MovingObjectUpdateScheduler.instance.removeObject(r);
            this.objectList.remove(r);
            if (r.getCurrentSquare() != null) {
                r.getCurrentSquare().getMovingObjects().remove(r);
            }
            if (r.getLastSquare() == null) continue;
            r.getLastSquare().getMovingObjects().remove(r);
        }
        this.removeList.clear();
        this.objectList.addAll(this.addList);
        this.addList.clear();
        for (BaseVehicle vehicle : this.addVehicles) {
            this.objectList.add((IsoMovingObject)vehicle);
            this.vehicles.add(vehicle);
        }
        this.addVehicles.clear();
    }

    private void ProcessItems(Iterator<InventoryItem> it2) {
        GameEntity i;
        int n;
        int size = this.processItems.size();
        for (n = 0; n < size; ++n) {
            i = this.processItems.get(n);
            i.update();
            if (!i.finishupdate()) continue;
            this.processItemsRemove.add((InventoryItem)i);
        }
        size = this.processWorldItems.size();
        for (n = 0; n < size; ++n) {
            i = this.processWorldItems.get(n);
            i.update();
            if (!i.finishupdate()) continue;
            this.processWorldItemsRemove.add((IsoWorldInventoryObject)i);
        }
    }

    private void ProcessIsoObject() {
        if (!this.processIsoObjectRemove.isEmpty()) {
            this.processIsoObject.removeAll(this.processIsoObjectRemove);
            this.processIsoObjectSet.removeAll(this.processIsoObjectRemove);
            this.processIsoObjectRemove.clear();
        }
        int size = this.processIsoObject.size();
        for (int n = 0; n < size; ++n) {
            IsoObject i = this.processIsoObject.get(n);
            if (i == null) continue;
            i.update();
            if (size <= this.processIsoObject.size()) continue;
            --n;
            --size;
        }
    }

    private void ProcessObjects(Iterator<IsoMovingObject> it) {
        MovingObjectUpdateScheduler.instance.update();
        for (IsoMovingObject obj : this.objectList) {
            IsoAnimal animal;
            if (!(obj instanceof IsoAnimal) || (animal = (IsoAnimal)obj).isOnHook()) continue;
            animal.updateVocalProperties();
            animal.updateLoopingSounds();
        }
        try (GameProfiler.ProfileArea profileArea = GameProfiler.getInstance().profile("Zombie Vocals");){
            this.updateZombieVocals();
        }
    }

    private void updateZombieVocals() {
        for (int n = 0; n < this.zombieList.size(); ++n) {
            IsoZombie zombie = this.zombieList.get(n);
            zombie.updateVocalProperties();
        }
    }

    private void ProcessRemoveItems(Iterator<InventoryItem> it2) {
        this.processItems.removeAll(this.processItemsRemove);
        this.processWorldItems.removeAll(this.processWorldItemsRemove);
        this.processItemsRemove.clear();
        this.processWorldItemsRemove.clear();
    }

    private void ProcessStaticUpdaters() {
        int size = this.staticUpdaterObjectList.size();
        for (int n = 0; n < size; ++n) {
            try {
                this.staticUpdaterObjectList.get(n).update();
            }
            catch (Exception ex) {
                Logger.getLogger(GameWindow.class.getName()).log(Level.SEVERE, null, ex);
            }
            if (size <= this.staticUpdaterObjectList.size()) continue;
            --n;
            --size;
        }
    }

    public void addToProcessIsoObject(IsoObject object) {
        if (object == null) {
            return;
        }
        this.processIsoObjectRemove.remove(object);
        if (this.processIsoObjectSet.add(object)) {
            this.processIsoObject.add(object);
        }
    }

    public void addToProcessIsoObjectRemove(IsoObject object) {
        if (object == null) {
            return;
        }
        if (this.processIsoObjectSet.contains(object)) {
            this.processIsoObjectRemove.add(object);
        }
    }

    public void addToStaticUpdaterObjectList(IsoObject object) {
        if (object == null) {
            return;
        }
        if (this.staticUpdaterObjectSet.add(object)) {
            this.staticUpdaterObjectList.add(object);
        }
    }

    public void removeFromStaticUpdaterObjectList(IsoObject object) {
        if (object == null) {
            return;
        }
        if (this.staticUpdaterObjectSet.remove(object)) {
            this.staticUpdaterObjectList.remove(object);
        }
    }

    public void addToProcessItems(InventoryItem item) {
        if (item == null || GameClient.client) {
            return;
        }
        this.processItemsRemove.remove((Object)item);
        if (!this.processItems.contains((Object)item)) {
            this.processItems.add(item);
        }
    }

    public void addToProcessItems(ArrayList<InventoryItem> items) {
        if (items == null || GameClient.client) {
            return;
        }
        for (int i = 0; i < items.size(); ++i) {
            InventoryItem item = items.get(i);
            if (item == null) continue;
            this.processItemsRemove.remove((Object)item);
            if (this.processItems.contains((Object)item)) continue;
            this.processItems.add(item);
        }
    }

    public void addToProcessItemsRemove(InventoryItem item) {
        if (item == null) {
            return;
        }
        if (!this.processItemsRemove.contains((Object)item)) {
            this.processItemsRemove.add(item);
        }
    }

    public void addToProcessItemsRemove(ArrayList<InventoryItem> items) {
        if (items == null) {
            return;
        }
        for (int i = 0; i < items.size(); ++i) {
            InventoryItem item = items.get(i);
            if (item == null || this.processItemsRemove.contains((Object)item)) continue;
            this.processItemsRemove.add(item);
        }
    }

    public void addToProcessWorldItems(IsoWorldInventoryObject worldItem) {
        if (worldItem == null) {
            return;
        }
        this.processWorldItemsRemove.remove(worldItem);
        if (!this.processWorldItems.contains(worldItem)) {
            this.processWorldItems.add(worldItem);
        }
    }

    public void addToProcessWorldItemsRemove(IsoWorldInventoryObject worldItem) {
        if (worldItem == null) {
            return;
        }
        if (!this.processWorldItemsRemove.contains(worldItem)) {
            this.processWorldItemsRemove.add(worldItem);
        }
    }

    public IsoSurvivor getNetworkPlayer(int remoteId) {
        int size = this.remoteSurvivorList.size();
        for (int n = 0; n < size; ++n) {
            if (this.remoteSurvivorList.get(n).getRemoteID() != remoteId) continue;
            return (IsoSurvivor)this.remoteSurvivorList.get(n);
        }
        return null;
    }

    IsoGridSquare ConnectNewSquare(IsoGridSquare newSquare, boolean bDoSurrounds, boolean specialSquare) {
        int x = newSquare.getX();
        int y = newSquare.getY();
        int z = newSquare.getZ();
        this.setCacheGridSquare(x, y, z, newSquare);
        return newSquare.doGridNav(IsoGridSquare.cellGetSquare);
    }

    public IsoGridSquare ConnectNewSquare(IsoGridSquare newSquare, boolean bDoSurrounds) {
        for (int n = 0; n < IsoPlayer.numPlayers; ++n) {
            if (this.chunkMap[n].ignore) continue;
            this.chunkMap[n].setGridSquare(newSquare, newSquare.getX(), newSquare.getY(), newSquare.getZ());
        }
        return this.ConnectNewSquare(newSquare, bDoSurrounds, false);
    }

    public void PlaceLot(String filename, int sx, int sy, int sz, boolean bClearExisting) {
    }

    public void PlaceLot(IsoLot lot, int sx, int sy, int sz, boolean bClearExisting) {
        int maxLevel = Math.min(sz + lot.info.maxLevel - lot.info.minLevel + 1, 32);
        for (int x = sx; x < sx + lot.info.width; ++x) {
            for (int y = sy; y < sy + lot.info.height; ++y) {
                for (int z = sz; z < maxLevel; ++z) {
                    int numInts;
                    int squareXYZ;
                    int offsetInData;
                    int lx = x - sx;
                    int ly = y - sy;
                    int lz = z - sz;
                    if (x >= this.width || y >= this.height || x < 0 || y < 0 || z < 0 || (offsetInData = lot.offsetInData[squareXYZ = lx + ly * 8 + lz * 8 * 8]) == -1 || (numInts = lot.data.getQuick(offsetInData)) <= 0) continue;
                    boolean bClearObjects = false;
                    for (int n = 0; n < numInts; ++n) {
                        String tile = (String)lot.info.tilesUsed.get(lot.data.getQuick(offsetInData + 1 + n));
                        IsoSprite spr = (IsoSprite)IsoSpriteManager.instance.namedMap.get(tile);
                        if (spr == null) {
                            Logger.getLogger(GameWindow.class.getName()).log(Level.SEVERE, "Missing tile definition: " + tile);
                            continue;
                        }
                        IsoGridSquare square = this.getGridSquare(x, y, z);
                        if (square == null) {
                            square = IsoGridSquare.loadGridSquareCache != null ? IsoGridSquare.getNew(IsoGridSquare.loadGridSquareCache, this, null, x, y, z) : IsoGridSquare.getNew(this, null, x, y, z);
                            this.chunkMap[IsoPlayer.getPlayerIndex()].setGridSquare(square, x, y, z);
                        } else {
                            if (bClearExisting && n == 0 && spr.getProperties().has(IsoFlagType.solidfloor) && (!spr.properties.has(IsoFlagType.hidewalls) || numInts > 1)) {
                                bClearObjects = true;
                            }
                            if (bClearObjects && n == 0) {
                                square.getObjects().clear();
                            }
                        }
                        CellLoader.DoTileObjectCreation((IsoSprite)spr, (IsoObjectType)spr.getTileType(), (IsoGridSquare)square, (IsoCell)this, (int)x, (int)y, (int)z, (String)tile);
                    }
                }
            }
        }
    }

    public int PlaceLot(IsoLot lot, int sx, int sy, int sz, IsoChunk ch, int wx, int wy, boolean[] bDoneSquares) {
        wx *= 8;
        wy *= 8;
        IsoMetaGrid metaGrid = IsoWorld.instance.getMetaGrid();
        int minLevel = Math.max(sz, -32);
        int maxLevel = Math.min(sz + lot.maxLevel - lot.minLevel - 1, 31);
        int nonEmptySquareCount = 0;
        try {
            for (int x = wx + sx; x < wx + sx + 8; ++x) {
                for (int y = wy + sy; y < wy + sy + 8; ++y) {
                    if (x < wx || y < wy || x >= wx + 8 || y >= wy + 8) continue;
                    if (bDoneSquares[x - wx + (y - wy) * 8]) {
                        ++nonEmptySquareCount;
                        continue;
                    }
                    for (int z = minLevel; z <= maxLevel; ++z) {
                        IsoGridSquare square;
                        int numInts;
                        int lx = x - wx - sx;
                        int ly = y - wy - sy;
                        int lz = z - lot.info.minLevel;
                        int squareXYZ = lx + ly * 8 + lz * 8 * 8;
                        int offsetInData = lot.offsetInData[squareXYZ];
                        if (offsetInData == -1 || (numInts = lot.data.getQuick(offsetInData)) <= 0) continue;
                        if (!bDoneSquares[x - wx + (y - wy) * 8]) {
                            bDoneSquares[x - wx + (y - wy) * 8] = true;
                            ++nonEmptySquareCount;
                        }
                        if ((square = ch.getGridSquare(x - wx, y - wy, z)) == null) {
                            square = IsoGridSquare.loadGridSquareCache != null ? IsoGridSquare.getNew(IsoGridSquare.loadGridSquareCache, this, null, x, y, z) : IsoGridSquare.getNew(this, null, x, y, z);
                            square.setX(x);
                            square.setY(y);
                            square.setZ(z);
                            ch.setSquare(x - wx, y - wy, z, square);
                        }
                        for (int xx = -1; xx <= 1; ++xx) {
                            for (int yy = -1; yy <= 1; ++yy) {
                                IsoGridSquare square2;
                                if (xx == 0 && yy == 0 || xx + x - wx < 0 || xx + x - wx >= 8 || yy + y - wy < 0 || yy + y - wy >= 8 || (square2 = ch.getGridSquare(x + xx - wx, y + yy - wy, z)) != null) continue;
                                square2 = IsoGridSquare.getNew(this, null, x + xx, y + yy, z);
                                ch.setSquare(x + xx - wx, y + yy - wy, z, square2);
                            }
                        }
                        RoomDef roomDef = metaGrid.getRoomAt(x, y, z);
                        long roomID = roomDef != null ? roomDef.id : -1L;
                        square.setRoomID(roomID);
                        square.ResetIsoWorldRegion();
                        roomDef = metaGrid.getEmptyOutsideAt(x, y, z);
                        if (roomDef != null) {
                            IsoRoom room = ch.getRoom(roomDef.id);
                            square.roofHideBuilding = room == null ? null : room.building;
                        }
                        boolean bClearObjects = true;
                        for (int n = 0; n < numInts; ++n) {
                            IsoSprite spr;
                            String tile = (String)lot.info.tilesUsed.get(lot.data.get(offsetInData + 1 + n));
                            if (!lot.info.fixed2x) {
                                tile = IsoChunk.Fix2x(tile);
                            }
                            if ((spr = (IsoSprite)IsoSpriteManager.instance.namedMap.get(tile)) == null) {
                                Logger.getLogger(GameWindow.class.getName()).log(Level.SEVERE, "Missing tile definition: " + tile);
                                continue;
                            }
                            if (n == 0 && spr.getProperties().has(IsoFlagType.solidfloor) && (!spr.properties.has(IsoFlagType.hidewalls) || numInts > 1)) {
                                bClearObjects = true;
                            }
                            if (bClearObjects && n == 0) {
                                square.getObjects().clear();
                            }
                            CellLoader.DoTileObjectCreation((IsoSprite)spr, (IsoObjectType)spr.getTileType(), (IsoGridSquare)square, (IsoCell)this, (int)x, (int)y, (int)z, (String)tile);
                        }
                        square.FixStackableObjects();
                    }
                }
            }
        }
        catch (Exception ex) {
            DebugLog.log((String)"Failed to load chunk, blocking out area");
            ExceptionLogger.logException((Throwable)ex);
            for (int x = wx + sx; x < wx + sx + 8; ++x) {
                for (int y = wy + sy; y < wy + sy + 8; ++y) {
                    for (int z = minLevel; z <= maxLevel; ++z) {
                        ch.setSquare(x - wx - sx, y - wy - sy, z - sz, null);
                        this.setCacheGridSquare(x, y, z, null);
                    }
                }
            }
        }
        return nonEmptySquareCount;
    }

    public void setDrag(KahluaTable draggingItem, int player) {
        Object functionObj;
        LuaEventManager.triggerEvent((String)"SetDragItem", (Object)draggingItem, (Object)player);
        if (player < 0 || player >= 4) {
            return;
        }
        if (this.drag[player] == draggingItem) {
            return;
        }
        KahluaTable previouslyDraggedItem = this.drag[player];
        if (previouslyDraggedItem != null && ((functionObj = previouslyDraggedItem.rawget((Object)"deactivate")) instanceof JavaFunction || functionObj instanceof LuaClosure)) {
            LuaManager.caller.pcallvoid(LuaManager.thread, functionObj, (Object)previouslyDraggedItem);
        }
        this.drag[player] = draggingItem;
        if (draggingItem != null) {
            draggingItem.rawset((Object)"player", (Object)BoxedStaticValues.toDouble((double)player));
        }
    }

    public KahluaTable getDrag(int player) {
        if (player < 0 || player >= 4) {
            return null;
        }
        return this.drag[player];
    }

    public boolean DoBuilding(int player, boolean bRender) {
        try (AbstractPerformanceProfileProbe abstractPerformanceProfileProbe = s_performance.isoCellDoBuilding.profile();){
            boolean bl = this.doBuildingInternal(player, bRender);
            return bl;
        }
    }

    private boolean doBuildingInternal(int player, boolean bRender) {
        if (UIManager.getPickedTile() != null && this.drag[player] != null && JoypadManager.instance.getFromPlayer(player) == null) {
            int buildZ;
            int buildY;
            int buildX = PZMath.fastfloor((float)UIManager.getPickedTile().x);
            if (!IsoWorld.instance.isValidSquare(buildX, buildY = PZMath.fastfloor((float)UIManager.getPickedTile().y), buildZ = PZMath.fastfloor((float)IsoCamera.getCameraCharacterZ()))) {
                return false;
            }
            IsoGridSquare square = this.getGridSquare(buildX, buildY, buildZ);
            if (!bRender) {
                if (square == null && (square = this.createNewGridSquare(buildX, buildY, buildZ, true)) == null) {
                    return false;
                }
                square.EnsureSurroundNotNull();
            }
            LuaEventManager.triggerEvent((String)"OnDoTileBuilding2", (Object)this.drag[player], (Object)bRender, (Object)buildX, (Object)buildY, (Object)buildZ, (Object)square);
        }
        if (this.drag[player] != null && JoypadManager.instance.getFromPlayer(player) != null) {
            LuaEventManager.triggerEvent((String)"OnDoTileBuilding3", (Object)this.drag[player], (Object)bRender, (Object)PZMath.fastfloor((float)IsoPlayer.players[player].getX()), (Object)PZMath.fastfloor((float)IsoPlayer.players[player].getY()), (Object)PZMath.fastfloor((float)IsoCamera.getCameraCharacterZ()));
        }
        if (bRender) {
            if (PerformanceSettings.fboRenderChunk) {
                IndieGL.glBlendFuncSeparate((int)770, (int)771, (int)770, (int)1);
            } else {
                IndieGL.glBlendFunc((int)770, (int)771);
            }
        }
        return false;
    }

    public float DistanceFromSupport(int x, int y, int z) {
        return 0.0f;
    }

    public ArrayList<IsoBuilding> getBuildingList() {
        return this.buildingList;
    }

    public ArrayList<IsoWindow> getWindowList() {
        return this.windowList;
    }

    public void addToWindowList(IsoWindow window) {
        if (GameServer.server) {
            return;
        }
        if (window == null) {
            return;
        }
        if (!this.windowList.contains(window)) {
            this.windowList.add(window);
        }
    }

    public void removeFromWindowList(IsoWindow window) {
        this.windowList.remove(window);
    }

    public Set<IsoMovingObject> getObjectList() {
        return this.objectList;
    }

    @UsedFromLua
    public List<IsoMovingObject> getObjectListForLua() {
        return this.objectList.stream().toList();
    }

    public IsoRoom getRoom(int id) {
        return this.chunkMap[IsoPlayer.getPlayerIndex()].getRoom(id);
    }

    public ArrayList<IsoPushableObject> getPushableObjectList() {
        return this.pushableObjectList;
    }

    public HashMap<Integer, BuildingScore> getBuildingScores() {
        return this.buildingScores;
    }

    public ArrayList<IsoRoom> getRoomList() {
        return this.roomList;
    }

    public ArrayList<IsoObject> getStaticUpdaterObjectList() {
        return this.staticUpdaterObjectList;
    }

    public ArrayList<IsoZombie> getZombieList() {
        return this.zombieList;
    }

    public ArrayList<IsoGameCharacter> getRemoteSurvivorList() {
        return this.remoteSurvivorList;
    }

    public Set<IsoMovingObject> getRemoveList() {
        return this.removeList;
    }

    public Set<IsoMovingObject> getAddList() {
        return this.addList;
    }

    public void addMovingObject(IsoMovingObject o) {
        if (this.isSafeToAdd()) {
            this.objectList.add(o);
        } else {
            this.addList.add(o);
        }
    }

    public ArrayList<InventoryItem> getProcessItems() {
        return this.processItems;
    }

    public ArrayList<IsoWorldInventoryObject> getProcessWorldItems() {
        return this.processWorldItems;
    }

    public ArrayList<IsoObject> getProcessIsoObjects() {
        return this.processIsoObject;
    }

    public Set<InventoryItem> getProcessItemsRemove() {
        return this.processItemsRemove;
    }

    public Set<BaseVehicle> getVehicles() {
        return this.vehicles;
    }

    public int getHeight() {
        return this.height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getWidth() {
        return this.width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getWorldX() {
        return this.worldX;
    }

    public void setWorldX(int worldX) {
        this.worldX = worldX;
    }

    public int getWorldY() {
        return this.worldY;
    }

    public void setWorldY(int worldY) {
        this.worldY = worldY;
    }

    public boolean isSafeToAdd() {
        return this.safeToAdd;
    }

    public void setSafeToAdd(boolean safeToAdd) {
        this.safeToAdd = safeToAdd;
    }

    public Stack<IsoLightSource> getLamppostPositions() {
        return this.lamppostPositions;
    }

    public IsoLightSource getLightSourceAt(int x, int y, int z) {
        for (int i = 0; i < this.lamppostPositions.size(); ++i) {
            IsoLightSource light = (IsoLightSource)this.lamppostPositions.get(i);
            if (light.getX() != x || light.getY() != y || light.getZ() != z) continue;
            return light;
        }
        return null;
    }

    public void addLamppost(IsoLightSource light) {
        if (light == null || this.lamppostPositions.contains(light)) {
            return;
        }
        this.lamppostPositions.add(light);
        IsoGridSquare.recalcLightTime = -1.0f;
        if (PerformanceSettings.fboRenderChunk) {
            ++Core.dirtyGlobalLightsCount;
        }
        GameTime.instance.lightSourceUpdate = 100.0f;
    }

    public IsoLightSource addLamppost(int x, int y, int z, float r, float g, float b, int rad) {
        IsoLightSource light = new IsoLightSource(x, y, z, r, g, b, rad);
        this.lamppostPositions.add(light);
        IsoGridSquare.recalcLightTime = -1.0f;
        if (PerformanceSettings.fboRenderChunk) {
            ++Core.dirtyGlobalLightsCount;
        }
        GameTime.instance.lightSourceUpdate = 100.0f;
        return light;
    }

    public void removeLamppost(int x, int y, int z) {
        for (int i = 0; i < this.lamppostPositions.size(); ++i) {
            IsoLightSource light = (IsoLightSource)this.lamppostPositions.get(i);
            if (light.getX() != x || light.getY() != y || light.getZ() != z) continue;
            light.clearInfluence();
            this.lamppostPositions.remove(light);
            IsoGridSquare.recalcLightTime = -1.0f;
            if (PerformanceSettings.fboRenderChunk) {
                ++Core.dirtyGlobalLightsCount;
            }
            GameTime.instance.lightSourceUpdate = 100.0f;
            return;
        }
    }

    public void removeLamppost(IsoLightSource light) {
        light.life = 0;
        IsoGridSquare.recalcLightTime = -1.0f;
        if (PerformanceSettings.fboRenderChunk) {
            ++Core.dirtyGlobalLightsCount;
        }
        GameTime.instance.lightSourceUpdate = 100.0f;
    }

    public int getCurrentLightX() {
        return this.currentLx;
    }

    public void setCurrentLightX(int currentLX) {
        this.currentLx = currentLX;
    }

    public int getCurrentLightY() {
        return this.currentLy;
    }

    public void setCurrentLightY(int currentLY) {
        this.currentLy = currentLY;
    }

    public int getCurrentLightZ() {
        return this.currentLz;
    }

    public void setCurrentLightZ(int currentLZ) {
        this.currentLz = currentLZ;
    }

    public int getMinX() {
        return this.minX;
    }

    public void setMinX(int minX) {
        this.minX = minX;
    }

    public int getMaxX() {
        return this.maxX;
    }

    public void setMaxX(int maxX) {
        this.maxX = maxX;
    }

    public int getMinY() {
        return this.minY;
    }

    public void setMinY(int minY) {
        this.minY = minY;
    }

    public int getMaxY() {
        return this.maxY;
    }

    public void setMaxY(int maxY) {
        this.maxY = maxY;
    }

    public int getMinZ() {
        return this.minZ;
    }

    public void setMinZ(int minZ) {
        this.minZ = minZ;
    }

    public int getMaxZ() {
        return this.maxZ;
    }

    public void setMaxZ(int maxZ) {
        this.maxZ = maxZ;
    }

    public OnceEvery getDangerUpdate() {
        return this.dangerUpdate;
    }

    public void setDangerUpdate(OnceEvery dangerUpdate) {
        this.dangerUpdate = dangerUpdate;
    }

    public Thread getLightInfoUpdate() {
        return this.lightInfoUpdate;
    }

    public void setLightInfoUpdate(Thread lightInfoUpdate) {
        this.lightInfoUpdate = lightInfoUpdate;
    }

    public ArrayList<IsoSurvivor> getSurvivorList() {
        return this.survivorList;
    }

    public static int getRComponent(int col) {
        return col & 0xFF;
    }

    public static int getGComponent(int col) {
        return (col & 0xFF00) >> 8;
    }

    public static int getBComponent(int col) {
        return (col & 0xFF0000) >> 16;
    }

    public static int toIntColor(float r, float g, float b, float a) {
        return (int)(r * 255.0f) << 0 | (int)(g * 255.0f) << 8 | (int)(b * 255.0f) << 16 | (int)(a * 255.0f) << 24;
    }

    public IsoGridSquare getRandomOutdoorTile() {
        IsoGridSquare sq;
        do {
            if ((sq = this.getGridSquare(this.chunkMap[IsoPlayer.getPlayerIndex()].getWorldXMin() * 8 + Rand.Next((int)this.width), this.chunkMap[IsoPlayer.getPlayerIndex()].getWorldYMin() * 8 + Rand.Next((int)this.height), 0)) == null) continue;
            sq.setCachedIsFree(false);
        } while (sq == null || !sq.isFree(false) || sq.getRoom() != null);
        return sq;
    }

    private static void InsertAt(int a, BuildingScore score, BuildingScore[] array) {
        for (int n = array.length - 1; n > a; --n) {
            array[n] = array[n - 1];
        }
        array[a] = score;
    }

    static void Place(BuildingScore score, BuildingScore[] array, BuildingSearchCriteria criteria) {
        for (int a = 0; a < array.length; ++a) {
            if (array[a] == null) continue;
            boolean bHigher = false;
            if (array[a] == null) {
                bHigher = true;
            } else {
                switch (criteria.ordinal()) {
                    case 4: {
                        if (!(array[a].food + array[a].defense + (float)array[a].size + array[a].weapons < score.food + score.defense + (float)score.size + score.weapons)) break;
                        bHigher = true;
                        break;
                    }
                    case 0: {
                        if (!(array[a].food < score.food)) break;
                        bHigher = true;
                        break;
                    }
                    case 2: {
                        if (!(array[a].wood < score.wood)) break;
                        bHigher = true;
                        break;
                    }
                    case 3: {
                        if (!(array[a].weapons < score.weapons)) break;
                        bHigher = true;
                        break;
                    }
                    case 1: {
                        if (!(array[a].defense < score.defense)) break;
                        bHigher = true;
                    }
                }
            }
            if (!bHigher) continue;
            IsoCell.InsertAt(a, score, array);
            return;
        }
    }

    public Stack<BuildingScore> getBestBuildings(BuildingSearchCriteria criteria, int count) {
        int n;
        int size;
        BuildingScore[] b = new BuildingScore[count];
        if (this.buildingScores.isEmpty()) {
            size = this.buildingList.size();
            for (n = 0; n < size; ++n) {
                this.buildingList.get(n).update();
            }
        }
        size = this.buildingScores.size();
        for (n = 0; n < size; ++n) {
            BuildingScore score = this.buildingScores.get(n);
            IsoCell.Place(score, b, criteria);
        }
        buildingscores.clear();
        buildingscores.addAll(Arrays.asList(b));
        return buildingscores;
    }

    public boolean blocked(Mover mover, int x, int y, int z, int lx, int ly, int lz) {
        IsoMovingObject isoMovingObject;
        IsoGridSquare spfrom = this.getGridSquare(lx, ly, lz);
        if (spfrom == null) {
            return true;
        }
        return mover instanceof IsoMovingObject ? spfrom.testPathFindAdjacent(isoMovingObject = (IsoMovingObject)mover, x - lx, y - ly, z - lz) : spfrom.testPathFindAdjacent(null, x - lx, y - ly, z - lz);
    }

    public void Dispose() {
        int i;
        int n;
        for (IsoMovingObject o : this.objectList) {
            if (!(o instanceof IsoZombie)) continue;
            IsoZombie isoZombie = (IsoZombie)o;
            o.setCurrent(null);
            o.setLast(null);
            VirtualZombieManager.instance.addToReusable(isoZombie);
        }
        for (n = 0; n < this.roomList.size(); ++n) {
            this.roomList.get((int)n).tileList.clear();
            this.roomList.get((int)n).exits.clear();
            this.roomList.get((int)n).waterSources.clear();
            this.roomList.get((int)n).lightSwitches.clear();
            this.roomList.get((int)n).beds.clear();
        }
        for (n = 0; n < this.buildingList.size(); ++n) {
            this.buildingList.get((int)n).exits.clear();
            this.buildingList.get((int)n).rooms.clear();
            this.buildingList.get((int)n).container.clear();
            this.buildingList.get((int)n).windows.clear();
        }
        LuaEventManager.clear();
        LuaHookManager.clear();
        this.lamppostPositions.clear();
        this.processItems.clear();
        this.processItemsRemove.clear();
        this.processWorldItems.clear();
        this.processWorldItemsRemove.clear();
        this.buildingScores.clear();
        this.buildingList.clear();
        this.windowList.clear();
        this.pushableObjectList.clear();
        this.roomList.clear();
        this.survivorList.clear();
        this.objectList.clear();
        this.zombieList.clear();
        WorldReuserThread.instance.stop();
        for (i = 0; i < this.chunkMap.length; ++i) {
            this.chunkMap[i].Dispose();
            this.chunkMap[i] = null;
        }
        if (!GameServer.server) {
            for (i = 0; i < this.gridSquares.length; ++i) {
                if (this.gridSquares[i] == null) continue;
                Arrays.fill(this.gridSquares[i], null);
                this.gridSquares[i] = null;
            }
        }
        if (this.weatherFx != null) {
            this.weatherFx.Reset();
            this.weatherFx = null;
        }
    }

    @LuaMethod(name="getGridSquare")
    public IsoGridSquare getGridSquare(double x, double y, double z) {
        int x1 = PZMath.fastfloor((double)x);
        int y1 = PZMath.fastfloor((double)y);
        int z1 = PZMath.fastfloor((double)z);
        if (GameServer.server) {
            return ServerMap.instance.getGridSquare(x1, y1, z1);
        }
        return this.getGridSquare(x1, y1, z1);
    }

    @LuaMethod(name="getOrCreateGridSquare")
    public IsoGridSquare getOrCreateGridSquare(double x, double y, double z) {
        int x1 = PZMath.fastfloor((double)x);
        int y1 = PZMath.fastfloor((double)y);
        int z1 = PZMath.fastfloor((double)z);
        if (GameServer.server) {
            IsoGridSquare sq = ServerMap.instance.getGridSquare(x1, y1, z1);
            if (sq == null) {
                sq = IsoGridSquare.getNew(this, null, x1, y1, z1);
                ServerMap.instance.setGridSquare(x1, y1, z1, sq);
                this.ConnectNewSquare(sq, true);
            }
            return sq;
        }
        IsoGridSquare sq = this.getGridSquare(x1, y1, z1);
        if (sq == null) {
            sq = IsoGridSquare.getNew(this, null, x1, y1, z1);
            this.ConnectNewSquare(sq, true);
        }
        return sq;
    }

    public void setCacheGridSquare(int x, int y, int z, IsoGridSquare square) {
        assert (square == null || x == square.getX() && y == square.getY() && z == square.getZ());
    }

    public void setCacheChunk(IsoChunk chunk) {
        if (GameServer.server) {
            return;
        }
    }

    public void setCacheChunk(IsoChunk chunk, int playerIndex) {
        if (GameServer.server) {
            return;
        }
    }

    public void clearCacheGridSquare(int playerIndex) {
    }

    public void setCacheGridSquareLocal(int x, int y, int z, IsoGridSquare square, int playerIndex) {
    }

    public IsoGridSquare getGridSquare(Double x, Double y, Double z) {
        return this.getGridSquare(PZMath.fastfloor((double)x), PZMath.fastfloor((double)y), PZMath.fastfloor((double)z));
    }

    public IsoGridSquare getGridSquare(int x, int y, int z) {
        if (GameServer.server) {
            return ServerMap.instance.getGridSquare(x, y, z);
        }
        int stride = IsoChunkMap.chunkWidthInTiles;
        for (int n = 0; n < IsoPlayer.numPlayers; ++n) {
            IsoGridSquare sq;
            IsoChunkMap chunkMap = this.chunkMap[n];
            if (chunkMap.ignore) continue;
            if (z == 0) {
                boolean bl = false;
            }
            int chunkMapSquareX = x - chunkMap.getWorldXMinTiles();
            int chunkMapSquareY = y - chunkMap.getWorldYMinTiles();
            int zz = z + 32;
            if (z > 31 || z < -32 || chunkMapSquareX < 0 || chunkMapSquareX >= stride || chunkMapSquareY < 0 || chunkMapSquareY >= stride || (sq = chunkMap.getGridSquareDirect(chunkMapSquareX, chunkMapSquareY, z)) == null) continue;
            return sq;
        }
        return null;
    }

    public void EnsureSurroundNotNull(int xx, int yy, int zz) {
        for (int x = -1; x <= 1; ++x) {
            for (int y = -1; y <= 1; ++y) {
                this.createNewGridSquare(xx + x, yy + y, zz, false);
            }
        }
    }

    public void DeleteAllMovingObjects() {
        this.objectList.clear();
    }

    @LuaMethod(name="getMaxFloors")
    public int getMaxFloors() {
        return 32;
    }

    public KahluaTable getLuaObjectList() {
        KahluaTable table = LuaManager.platform.newTable();
        LuaManager.env.rawset((Object)"Objects", (Object)table);
        int n = 1;
        for (IsoMovingObject obj : this.objectList) {
            table.rawset(n++, (Object)obj);
        }
        return table;
    }

    public int getHeightInTiles() {
        return this.chunkMap[IsoPlayer.getPlayerIndex()].getWidthInTiles();
    }

    public int getWidthInTiles() {
        return this.chunkMap[IsoPlayer.getPlayerIndex()].getWidthInTiles();
    }

    public boolean isNull(int x, int y, int z) {
        IsoGridSquare sq = this.getGridSquare(x, y, z);
        return sq == null || !sq.isFree(false);
    }

    public void Remove(IsoMovingObject obj) {
        IsoPlayer isoPlayer;
        if (obj instanceof IsoPlayer && !(isoPlayer = (IsoPlayer)obj).isDead()) {
            return;
        }
        this.removeList.add(obj);
    }

    boolean isBlocked(IsoGridSquare from, IsoGridSquare to) {
        return from.room != to.room;
    }

    private int CalculateColor(IsoGridSquare sqUL, IsoGridSquare sqU, IsoGridSquare sqL, IsoGridSquare sqThis, int col, int playerIndex) {
        ColorInfo inf;
        float r = 0.0f;
        float g = 0.0f;
        float b = 0.0f;
        float a = 1.0f;
        if (sqThis == null) {
            return 0;
        }
        float div = 0.0f;
        boolean bTest = true;
        if (sqUL != null && sqThis.room == sqUL.room && sqUL.getChunk() != null) {
            div += 1.0f;
            inf = sqUL.lighting[playerIndex].lightInfo();
            r += inf.r;
            g += inf.g;
            b += inf.b;
        }
        if (sqU != null && sqThis.room == sqU.room && sqU.getChunk() != null) {
            div += 1.0f;
            inf = sqU.lighting[playerIndex].lightInfo();
            r += inf.r;
            g += inf.g;
            b += inf.b;
        }
        if (sqL != null && sqThis.room == sqL.room && sqL.getChunk() != null) {
            div += 1.0f;
            inf = sqL.lighting[playerIndex].lightInfo();
            r += inf.r;
            g += inf.g;
            b += inf.b;
        }
        if (sqThis != null) {
            div += 1.0f;
            inf = sqThis.lighting[playerIndex].lightInfo();
            r += inf.r;
            g += inf.g;
            b += inf.b;
        }
        if (div != 0.0f) {
            r /= div;
            g /= div;
            b /= div;
        }
        if (r > 1.0f) {
            r = 1.0f;
        }
        if (g > 1.0f) {
            g = 1.0f;
        }
        if (b > 1.0f) {
            b = 1.0f;
        }
        if (r < 0.0f) {
            r = 0.0f;
        }
        if (g < 0.0f) {
            g = 0.0f;
        }
        if (b < 0.0f) {
            b = 0.0f;
        }
        if (sqThis != null) {
            sqThis.setVertLight(col, (int)(r * 255.0f) << 0 | (int)(g * 255.0f) << 8 | (int)(b * 255.0f) << 16 | 0xFF000000, playerIndex);
            sqThis.setVertLight(col + 4, (int)(r * 255.0f) << 0 | (int)(g * 255.0f) << 8 | (int)(b * 255.0f) << 16 | 0xFF000000, playerIndex);
        }
        return col;
    }

    public static IsoCell getInstance() {
        return instance;
    }

    public void render() {
        if (Core.debug && IsoCamera.frameState.playerIndex == 0 && GameKeyboard.isKeyPressed((int)199)) {
            PerformanceSettings.fboRenderChunk = !PerformanceSettings.fboRenderChunk;
        }
        try (AbstractPerformanceProfileProbe abstractPerformanceProfileProbe = s_performance.isoCellRender.profile();){
            if (PerformanceSettings.fboRenderChunk) {
                FBORenderCell.instance.cell = this;
                FBORenderCutaways.getInstance().cell = this;
                FBORenderCell.instance.renderInternal();
            } else {
                this.renderInternal();
            }
        }
    }

    private void renderInternal() {
        IsoGridSquare sq;
        int maxHeight;
        int playerIndex = IsoCamera.frameState.playerIndex;
        IsoPlayer player = IsoPlayer.players[playerIndex];
        player.dirtyRecalcGridStack = player.dirtyRecalcGridStackTime > 0.0f;
        if (!PerformanceSettings.newRoofHiding) {
            if (this.hideFloors[playerIndex] && this.unhideFloorsCounter[playerIndex] > 0) {
                int n = playerIndex;
                this.unhideFloorsCounter[n] = this.unhideFloorsCounter[n] - 1;
            }
            if (this.unhideFloorsCounter[playerIndex] <= 0) {
                this.hideFloors[playerIndex] = false;
                this.unhideFloorsCounter[playerIndex] = 60;
            }
        }
        if ((maxHeight = 32) < 32) {
            ++maxHeight;
        }
        --this.recalcShading;
        boolean x1 = false;
        boolean y1 = false;
        int x2 = 0 + IsoCamera.getOffscreenWidth((int)playerIndex);
        int y2 = 0 + IsoCamera.getOffscreenHeight((int)playerIndex);
        float topLeftX = IsoUtils.XToIso((float)0.0f, (float)0.0f, (float)0.0f);
        float topRightY = IsoUtils.YToIso((float)x2, (float)0.0f, (float)0.0f);
        float bottomRightX = IsoUtils.XToIso((float)x2, (float)y2, (float)6.0f);
        float bottomLeftY = IsoUtils.YToIso((float)0.0f, (float)y2, (float)6.0f);
        this.minY = (int)topRightY;
        this.maxY = (int)bottomLeftY;
        this.minX = (int)topLeftX;
        this.maxX = (int)bottomRightX;
        this.minX -= 2;
        this.minY -= 2;
        this.maxZ = IsoCell.maxHeight;
        IsoGameCharacter isoGameCharacter = IsoCamera.getCameraCharacter();
        if (isoGameCharacter == null) {
            this.maxZ = 1;
        }
        if (this.minX != this.lastMinX || this.minY != this.lastMinY) {
            this.lightUpdateCount = 10;
        }
        if (!PerformanceSettings.newRoofHiding) {
            IsoGridSquare currentSq;
            IsoGridSquare isoGridSquare = currentSq = isoGameCharacter == null ? null : isoGameCharacter.getCurrentSquare();
            if (currentSq != null) {
                sq = this.getGridSquare((double)Math.round(isoGameCharacter.getX()), (double)Math.round(isoGameCharacter.getY()), isoGameCharacter.getZ());
                if (sq != null && this.IsBehindStuff(sq)) {
                    this.hideFloors[playerIndex] = true;
                }
                if (!this.hideFloors[playerIndex] && currentSq.getProperties().has(IsoFlagType.hidewalls) || !currentSq.getProperties().has(IsoFlagType.exterior)) {
                    this.hideFloors[playerIndex] = true;
                }
            }
            if (this.hideFloors[playerIndex]) {
                this.maxZ = isoGameCharacter.getZi() + 1;
            }
        }
        this.drawStencilMask();
        GameProfiler profiler = GameProfiler.getInstance();
        if (PerformanceSettings.newRoofHiding && player.dirtyRecalcGridStack) {
            this.hidesOrphanStructuresAbove = maxHeight;
            this.otherOccluderBuildings.get(playerIndex).clear();
            if (this.otherOccluderBuildingsArr[playerIndex] != null) {
                this.otherOccluderBuildingsArr[playerIndex][0] = null;
            } else {
                this.otherOccluderBuildingsArr[playerIndex] = new IsoBuilding[500];
            }
            if (isoGameCharacter != null && isoGameCharacter.getCurrentSquare() != null) {
                sq = profiler.profile("updateZombies");
                try {
                    this.updateZombiesForRender(isoGameCharacter, playerIndex);
                }
                finally {
                    if (sq != null) {
                        sq.close();
                    }
                }
            } else {
                for (int playerIter = 0; playerIter < 4; ++playerIter) {
                    this.playerOccluderBuildings.get(playerIter).clear();
                    if (this.playerOccluderBuildingsArr[playerIter] != null) {
                        this.playerOccluderBuildingsArr[playerIter][0] = null;
                    } else {
                        this.playerOccluderBuildingsArr[playerIter] = new IsoBuilding[500];
                    }
                    this.lastPlayerSquare[playerIter] = null;
                    this.playerCutawaysDirty[playerIter] = true;
                }
                this.playerWindowPeekingRoomId[playerIndex] = -1L;
                this.zombieOccluderBuildings.get(playerIndex).clear();
                if (this.zombieOccluderBuildingsArr[playerIndex] != null) {
                    this.zombieOccluderBuildingsArr[playerIndex][0] = null;
                } else {
                    this.zombieOccluderBuildingsArr[playerIndex] = new IsoBuilding[500];
                }
                this.lastZombieSquare[playerIndex] = null;
            }
        }
        if (!PerformanceSettings.newRoofHiding) {
            for (int i = 0; i < IsoPlayer.numPlayers; ++i) {
                IsoBuilding currentBuilding;
                this.playerWindowPeekingRoomId[i] = -1L;
                IsoPlayer player2 = IsoPlayer.players[i];
                if (player2 == null || (currentBuilding = player2.getCurrentBuilding()) != null) continue;
                IsoDirections playerDir = IsoDirections.fromAngle((Vector2)player2.getForwardDirection());
                currentBuilding = this.GetPeekedInBuilding(player2.getCurrentSquare(), playerDir);
                if (currentBuilding == null) continue;
                this.playerWindowPeekingRoomId[i] = this.playerPeekedRoomId;
            }
        }
        if (isoGameCharacter != null && isoGameCharacter.getCurrentSquare() != null && isoGameCharacter.getCurrentSquare().getProperties().has(IsoFlagType.hidewalls)) {
            this.maxZ = isoGameCharacter.getZi() + 1;
        }
        this.rendering = true;
        try {
            this.RenderTiles(maxHeight);
        }
        catch (Exception ex) {
            this.rendering = false;
            Logger.getLogger(GameWindow.class.getName()).log(Level.SEVERE, null, ex);
        }
        this.rendering = false;
        if (IsoGridSquare.getRecalcLightTime() < 0.0f) {
            IsoGridSquare.setRecalcLightTime(60.0f);
        }
        if (IsoGridSquare.getLightcache() <= 0) {
            IsoGridSquare.setLightcache(90);
        }
        try (GameProfiler.ProfileArea profileArea = profiler.profile("renderLast");){
            this.renderLast();
        }
        IsoTree.renderChopTreeIndicators();
        this.lastMinX = this.minX;
        this.lastMinY = this.minY;
        profileArea = profiler.profile("DoBuilding");
        try {
            this.DoBuilding(IsoPlayer.getPlayerIndex(), true);
        }
        finally {
            if (profileArea != null) {
                profileArea.close();
            }
        }
        profileArea = profiler.profile("renderRain");
        try {
            this.renderRain();
        }
        finally {
            if (profileArea != null) {
                profileArea.close();
            }
        }
    }

    private void renderLast() {
        for (IsoMovingObject isoMovingObject : this.objectList) {
            isoMovingObject.renderlast();
        }
        for (IsoObject isoObject : this.staticUpdaterObjectList) {
            isoObject.renderlast();
        }
    }

    private void updateZombiesForRender(IsoGameCharacter isoGameCharacter, int playerIndex) {
        IsoGridSquare zombieSquare;
        IsoGridSquare playerSquare = isoGameCharacter.getCurrentSquare();
        IsoGridSquare currentPlayerSquare = null;
        int scanCount = 10;
        if (this.zombieList.size() < 10) {
            scanCount = this.zombieList.size();
        }
        if (this.nearestVisibleZombie[playerIndex] != null) {
            if (this.nearestVisibleZombie[playerIndex].isDead()) {
                this.nearestVisibleZombie[playerIndex] = null;
            } else {
                float diffX = this.nearestVisibleZombie[playerIndex].getX() - isoGameCharacter.getX();
                float diffY = this.nearestVisibleZombie[playerIndex].getY() - isoGameCharacter.getY();
                this.nearestVisibleZombieDistSqr[playerIndex] = diffX * diffX + diffY * diffY;
            }
        }
        int scan = 0;
        while (scan < scanCount) {
            IsoGridSquare candidateZombieSquare;
            IsoZombie candidateZombie;
            if (this.zombieScanCursor >= this.zombieList.size()) {
                this.zombieScanCursor = 0;
            }
            if ((candidateZombie = this.zombieList.get(this.zombieScanCursor)) != null && (candidateZombieSquare = candidateZombie.getCurrentSquare()) != null && playerSquare.z == candidateZombieSquare.z && candidateZombieSquare.getCanSee(playerIndex)) {
                if (this.nearestVisibleZombie[playerIndex] == null) {
                    this.nearestVisibleZombie[playerIndex] = candidateZombie;
                    diffX = this.nearestVisibleZombie[playerIndex].getX() - isoGameCharacter.getX();
                    diffY = this.nearestVisibleZombie[playerIndex].getY() - isoGameCharacter.getY();
                    this.nearestVisibleZombieDistSqr[playerIndex] = diffX * diffX + diffY * diffY;
                } else {
                    diffX = candidateZombie.getX() - isoGameCharacter.getX();
                    float candidateZombieDistSqr = diffX * diffX + (diffY = candidateZombie.getY() - isoGameCharacter.getY()) * diffY;
                    if (candidateZombieDistSqr < this.nearestVisibleZombieDistSqr[playerIndex]) {
                        this.nearestVisibleZombie[playerIndex] = candidateZombie;
                        this.nearestVisibleZombieDistSqr[playerIndex] = candidateZombieDistSqr;
                    }
                }
            }
            ++scan;
            ++this.zombieScanCursor;
        }
        for (int playerIter = 0; playerIter < 4; ++playerIter) {
            double yInSquare;
            double xInSquare;
            boolean bRightOfSquare;
            IsoPlayer player2 = IsoPlayer.players[playerIter];
            if (player2 == null || player2.getCurrentSquare() == null) continue;
            IsoGridSquare playerIterSquare = player2.getCurrentSquare();
            if (playerIter == playerIndex) {
                currentPlayerSquare = playerIterSquare;
            }
            boolean bl = bRightOfSquare = (xInSquare = (double)(player2.getX() - (float)PZMath.fastfloor((float)player2.getX()))) > (yInSquare = (double)(player2.getY() - (float)PZMath.fastfloor((float)player2.getY())));
            if (this.lastPlayerAngle[playerIter] == null) {
                this.lastPlayerAngle[playerIter] = new Vector2(player2.getForwardDirection());
                this.playerCutawaysDirty[playerIter] = true;
            } else if (player2.getForwardDirection().dot(this.lastPlayerAngle[playerIter]) < 0.98f) {
                this.lastPlayerAngle[playerIter].set(player2.getForwardDirection());
                this.playerCutawaysDirty[playerIter] = true;
            }
            IsoDirections playerDir = IsoDirections.fromAngle((Vector2)player2.getForwardDirection());
            if (this.lastPlayerSquare[playerIter] != playerIterSquare || this.lastPlayerSquareHalf[playerIter] != bRightOfSquare || this.lastPlayerDir[playerIter] != playerDir) {
                this.playerCutawaysDirty[playerIter] = true;
                this.lastPlayerSquare[playerIter] = playerIterSquare;
                this.lastPlayerSquareHalf[playerIter] = bRightOfSquare;
                this.lastPlayerDir[playerIter] = playerDir;
                IsoBuilding currentBuilding = playerIterSquare.getBuilding();
                this.playerWindowPeekingRoomId[playerIter] = -1L;
                this.GetBuildingsInFrontOfCharacter(this.playerOccluderBuildings.get(playerIter), playerIterSquare, bRightOfSquare);
                if (this.playerOccluderBuildingsArr[playerIndex] == null) {
                    this.playerOccluderBuildingsArr[playerIndex] = new IsoBuilding[500];
                }
                this.playerHidesOrphanStructures[playerIter] = this.occludedByOrphanStructureFlag;
                if (currentBuilding == null && !player2.remote && (currentBuilding = this.GetPeekedInBuilding(playerIterSquare, playerDir)) != null) {
                    this.playerWindowPeekingRoomId[playerIter] = this.playerPeekedRoomId;
                }
                if (currentBuilding != null) {
                    this.AddUniqueToBuildingList(this.playerOccluderBuildings.get(playerIter), currentBuilding);
                }
                ArrayList<IsoBuilding> arr = this.playerOccluderBuildings.get(playerIter);
                for (int i = 0; i < arr.size(); ++i) {
                    IsoBuilding isoBuilding;
                    this.playerOccluderBuildingsArr[playerIndex][i] = isoBuilding = arr.get(i);
                }
                this.playerOccluderBuildingsArr[playerIndex][arr.size()] = null;
            }
            if (playerIter == playerIndex && currentPlayerSquare != null) {
                IsoGridSquare square;
                int i;
                this.gridSquaresTempLeft.clear();
                this.gridSquaresTempRight.clear();
                this.GetSquaresAroundPlayerSquare(player2, currentPlayerSquare, this.gridSquaresTempLeft, this.gridSquaresTempRight);
                for (i = 0; i < this.gridSquaresTempLeft.size(); ++i) {
                    square = this.gridSquaresTempLeft.get(i);
                    if (!square.getCanSee(playerIndex) || square.getBuilding() != null && square.getBuilding() != currentPlayerSquare.getBuilding()) continue;
                    ArrayList<IsoBuilding> buildings = this.GetBuildingsInFrontOfMustSeeSquare(square, IsoGridOcclusionData.OcclusionFilter.Right);
                    for (int j = 0; j < buildings.size(); ++j) {
                        this.AddUniqueToBuildingList(this.otherOccluderBuildings.get(playerIndex), buildings.get(j));
                    }
                    int n = playerIndex;
                    this.playerHidesOrphanStructures[n] = this.playerHidesOrphanStructures[n] | this.occludedByOrphanStructureFlag;
                }
                for (i = 0; i < this.gridSquaresTempRight.size(); ++i) {
                    square = this.gridSquaresTempRight.get(i);
                    if (!square.getCanSee(playerIndex) || square.getBuilding() != null && square.getBuilding() != currentPlayerSquare.getBuilding()) continue;
                    ArrayList<IsoBuilding> buildings = this.GetBuildingsInFrontOfMustSeeSquare(square, IsoGridOcclusionData.OcclusionFilter.Left);
                    for (int j = 0; j < buildings.size(); ++j) {
                        this.AddUniqueToBuildingList(this.otherOccluderBuildings.get(playerIndex), buildings.get(j));
                    }
                    int n = playerIndex;
                    this.playerHidesOrphanStructures[n] = this.playerHidesOrphanStructures[n] | this.occludedByOrphanStructureFlag;
                }
                ArrayList<IsoBuilding> oocc = this.otherOccluderBuildings.get(playerIndex);
                if (this.otherOccluderBuildingsArr[playerIndex] == null) {
                    this.otherOccluderBuildingsArr[playerIndex] = new IsoBuilding[500];
                }
                for (int i2 = 0; i2 < oocc.size(); ++i2) {
                    IsoBuilding isoBuilding;
                    this.otherOccluderBuildingsArr[playerIndex][i2] = isoBuilding = oocc.get(i2);
                }
                this.otherOccluderBuildingsArr[playerIndex][oocc.size()] = null;
            }
            if (!this.playerHidesOrphanStructures[playerIter] || this.hidesOrphanStructuresAbove <= playerIterSquare.getZ()) continue;
            this.hidesOrphanStructuresAbove = playerIterSquare.getZ();
        }
        if (currentPlayerSquare != null && this.hidesOrphanStructuresAbove < currentPlayerSquare.getZ()) {
            this.hidesOrphanStructuresAbove = currentPlayerSquare.getZ();
        }
        boolean bSetZombieBuildings = false;
        if (this.nearestVisibleZombie[playerIndex] != null && this.nearestVisibleZombieDistSqr[playerIndex] < 150.0f && (zombieSquare = this.nearestVisibleZombie[playerIndex].getCurrentSquare()) != null && zombieSquare.getCanSee(playerIndex)) {
            double yInSquare;
            double xInSquare = this.nearestVisibleZombie[playerIndex].getX() - (float)PZMath.fastfloor((float)this.nearestVisibleZombie[playerIndex].getX());
            boolean bRightOfSquare = xInSquare > (yInSquare = (double)(this.nearestVisibleZombie[playerIndex].getY() - (float)PZMath.fastfloor((float)this.nearestVisibleZombie[playerIndex].getY())));
            bSetZombieBuildings = true;
            if (this.lastZombieSquare[playerIndex] != zombieSquare || this.lastZombieSquareHalf[playerIndex] != bRightOfSquare) {
                this.lastZombieSquare[playerIndex] = zombieSquare;
                this.lastZombieSquareHalf[playerIndex] = bRightOfSquare;
                this.GetBuildingsInFrontOfCharacter(this.zombieOccluderBuildings.get(playerIndex), zombieSquare, bRightOfSquare);
                ArrayList<IsoBuilding> zarr = this.zombieOccluderBuildings.get(playerIndex);
                if (this.zombieOccluderBuildingsArr[playerIndex] == null) {
                    this.zombieOccluderBuildingsArr[playerIndex] = new IsoBuilding[500];
                }
                for (int i = 0; i < zarr.size(); ++i) {
                    IsoBuilding isoBuilding;
                    this.zombieOccluderBuildingsArr[playerIndex][i] = isoBuilding = zarr.get(i);
                }
                this.zombieOccluderBuildingsArr[playerIndex][zarr.size()] = null;
            }
        }
        if (!bSetZombieBuildings) {
            this.zombieOccluderBuildings.get(playerIndex).clear();
            if (this.zombieOccluderBuildingsArr[playerIndex] != null) {
                this.zombieOccluderBuildingsArr[playerIndex][0] = null;
            } else {
                this.zombieOccluderBuildingsArr[playerIndex] = new IsoBuilding[500];
            }
        }
    }

    public void invalidatePeekedRoom(int playerIndex) {
        this.lastPlayerDir[playerIndex] = null;
    }

    protected boolean initWeatherFx() {
        if (GameServer.server) {
            return false;
        }
        if (this.weatherFx == null) {
            this.weatherFx = new IsoWeatherFX();
            this.weatherFx.init();
        }
        return true;
    }

    private void updateWeatherFx() {
        if (this.initWeatherFx()) {
            this.weatherFx.update();
        }
    }

    private void renderWeatherFx() {
        if (this.initWeatherFx()) {
            this.weatherFx.render();
        }
    }

    public IsoWeatherFX getWeatherFX() {
        return this.weatherFx;
    }

    public void renderRain() {
    }

    public void setRainAlpha(int alpha) {
        this.rainAlphaMax = (float)alpha / 100.0f;
    }

    public void setRainIntensity(int intensity) {
        this.rainIntensity = intensity;
    }

    public int getRainIntensity() {
        return this.rainIntensity;
    }

    public void setRainSpeed(int speed) {
        this.rainSpeed = speed;
    }

    public void reloadRainTextures() {
    }

    public void GetBuildingsInFrontOfCharacter(ArrayList<IsoBuilding> buildings, IsoGridSquare square, boolean bRightOfSquare) {
        buildings.clear();
        this.occludedByOrphanStructureFlag = false;
        if (square == null) {
            return;
        }
        int x = square.getX();
        int y = square.getY();
        int z = square.getZ();
        this.GetBuildingsInFrontOfCharacterSquare(x, y, z, bRightOfSquare, buildings);
        if (z < maxHeight) {
            this.GetBuildingsInFrontOfCharacterSquare(x - 1 + 3, y - 1 + 3, z + 1, bRightOfSquare, buildings);
            this.GetBuildingsInFrontOfCharacterSquare(x - 2 + 3, y - 2 + 3, z + 1, bRightOfSquare, buildings);
            if (bRightOfSquare) {
                this.GetBuildingsInFrontOfCharacterSquare(x + 3, y - 1 + 3, z + 1, !bRightOfSquare, buildings);
                this.GetBuildingsInFrontOfCharacterSquare(x - 1 + 3, y - 2 + 3, z + 1, !bRightOfSquare, buildings);
            } else {
                this.GetBuildingsInFrontOfCharacterSquare(x - 1 + 3, y + 3, z + 1, !bRightOfSquare, buildings);
                this.GetBuildingsInFrontOfCharacterSquare(x - 2 + 3, y - 1 + 3, z + 1, !bRightOfSquare, buildings);
            }
        }
    }

    private void GetBuildingsInFrontOfCharacterSquare(int inX, int inY, int inZ, boolean bRightOfSquare, ArrayList<IsoBuilding> outBuildings) {
        IsoGridSquare square = this.getGridSquare(inX, inY, inZ);
        if (square == null) {
            if (inZ < maxHeight) {
                this.GetBuildingsInFrontOfCharacterSquare(inX + 3, inY + 3, inZ + 1, bRightOfSquare, outBuildings);
            }
            return;
        }
        IsoGridOcclusionData occlusion = square.getOrCreateOcclusionData();
        IsoGridOcclusionData.OcclusionFilter occlusionFilter = bRightOfSquare ? IsoGridOcclusionData.OcclusionFilter.Right : IsoGridOcclusionData.OcclusionFilter.Left;
        this.occludedByOrphanStructureFlag |= occlusion.getCouldBeOccludedByOrphanStructures(occlusionFilter);
        ArrayList occluders = occlusion.getBuildingsCouldBeOccluders(occlusionFilter);
        for (int i = 0; i < occluders.size(); ++i) {
            this.AddUniqueToBuildingList(outBuildings, (IsoBuilding)occluders.get(i));
        }
    }

    public ArrayList<IsoBuilding> GetBuildingsInFrontOfMustSeeSquare(IsoGridSquare square, IsoGridOcclusionData.OcclusionFilter filter) {
        IsoGridOcclusionData occlusion = square.getOrCreateOcclusionData();
        this.occludedByOrphanStructureFlag = occlusion.getCouldBeOccludedByOrphanStructures(IsoGridOcclusionData.OcclusionFilter.All);
        return occlusion.getBuildingsCouldBeOccluders(filter);
    }

    public IsoBuilding GetPeekedInBuilding(IsoGridSquare square, IsoDirections lookDir) {
        IsoBuilding peekedBuilding;
        IsoGridSquare targetSquare;
        this.playerPeekedRoomId = -1L;
        if (square == null) {
            return null;
        }
        if ((lookDir == IsoDirections.NW || lookDir == IsoDirections.N || lookDir == IsoDirections.NE) && LosUtil.lineClear((IsoCell)this, (int)square.x, (int)square.y, (int)square.z, (int)square.x, (int)(square.y - 1), (int)square.z, (boolean)false) != LosUtil.TestResults.Blocked && (targetSquare = square.getAdjacentSquare(IsoDirections.N)) != null && (peekedBuilding = targetSquare.getBuilding()) != null) {
            this.playerPeekedRoomId = targetSquare.getRoomID();
            return peekedBuilding;
        }
        if ((lookDir == IsoDirections.SW || lookDir == IsoDirections.W || lookDir == IsoDirections.NW) && LosUtil.lineClear((IsoCell)this, (int)square.x, (int)square.y, (int)square.z, (int)(square.x - 1), (int)square.y, (int)square.z, (boolean)false) != LosUtil.TestResults.Blocked && (targetSquare = square.getAdjacentSquare(IsoDirections.W)) != null && (peekedBuilding = targetSquare.getBuilding()) != null) {
            this.playerPeekedRoomId = targetSquare.getRoomID();
            return peekedBuilding;
        }
        if ((lookDir == IsoDirections.SE || lookDir == IsoDirections.S || lookDir == IsoDirections.SW) && LosUtil.lineClear((IsoCell)this, (int)square.x, (int)square.y, (int)square.z, (int)square.x, (int)(square.y + 1), (int)square.z, (boolean)false) != LosUtil.TestResults.Blocked && (targetSquare = square.getAdjacentSquare(IsoDirections.S)) != null && (peekedBuilding = targetSquare.getBuilding()) != null) {
            this.playerPeekedRoomId = targetSquare.getRoomID();
            return peekedBuilding;
        }
        if ((lookDir == IsoDirections.NE || lookDir == IsoDirections.E || lookDir == IsoDirections.SE) && LosUtil.lineClear((IsoCell)this, (int)square.x, (int)square.y, (int)square.z, (int)(square.x + 1), (int)square.y, (int)square.z, (boolean)false) != LosUtil.TestResults.Blocked && (targetSquare = square.getAdjacentSquare(IsoDirections.E)) != null && (peekedBuilding = targetSquare.getBuilding()) != null) {
            this.playerPeekedRoomId = targetSquare.getRoomID();
            return peekedBuilding;
        }
        return null;
    }

    public void GetSquaresAroundPlayerSquare(IsoPlayer player, IsoGridSquare square, ArrayList<IsoGridSquare> outGridSquaresToLeft, ArrayList<IsoGridSquare> outGridSquaresToRight) {
        float fMinX = player.getX() - 4.0f;
        float fMinY = player.getY() - 4.0f;
        int startX = PZMath.fastfloor((float)fMinX);
        int startY = PZMath.fastfloor((float)fMinY);
        int z = square.getZ();
        for (int y = startY; y < startY + 10; ++y) {
            for (int x = startX; x < startX + 10; ++x) {
                IsoGridSquare iterSquare;
                if (x < PZMath.fastfloor((float)player.getX()) && y < PZMath.fastfloor((float)player.getY()) || x == PZMath.fastfloor((float)player.getX()) && y == PZMath.fastfloor((float)player.getY())) continue;
                float deltaX = (float)x - player.getX();
                float deltaY = (float)y - player.getY();
                if (!((double)deltaY < (double)deltaX + 4.5) || !((double)deltaY > (double)deltaX - 4.5) || (iterSquare = this.getGridSquare(x, y, z)) == null) continue;
                if (deltaY >= deltaX) {
                    outGridSquaresToLeft.add(iterSquare);
                }
                if (!(deltaY <= deltaX)) continue;
                outGridSquaresToRight.add(iterSquare);
            }
        }
    }

    public boolean IsBehindStuff(IsoGridSquare sq) {
        if (!sq.getProperties().has(IsoFlagType.exterior)) {
            return true;
        }
        for (int z = 1; z < 64 && sq.getZ() + z < maxHeight; ++z) {
            for (int y = -5; y <= 6; ++y) {
                for (int x = -5; x <= 6; ++x) {
                    IsoGridSquare sq2;
                    if (x < y - 5 || x > y + 5 || (sq2 = this.getGridSquare(sq.getX() + x + z * 3, sq.getY() + y + z * 3, sq.getZ() + z)) == null || sq2.getObjects().isEmpty()) continue;
                    if (z == 1 && sq2.getObjects().size() == 1) {
                        IsoObject obj = (IsoObject)sq2.getObjects().get(0);
                        if (obj.sprite != null && obj.sprite.name != null && obj.sprite.name.startsWith("lighting_outdoor")) continue;
                    }
                    return true;
                }
            }
        }
        return false;
    }

    public static IsoDirections FromMouseTile() {
        IsoDirections dir = IsoDirections.N;
        float x = UIManager.getPickedTileLocal().x;
        float y = UIManager.getPickedTileLocal().y;
        float centrenessY = 0.5f - Math.abs(0.5f - y);
        float centrenessX = 0.5f - Math.abs(0.5f - x);
        if (x > 0.5f && centrenessX < centrenessY) {
            dir = IsoDirections.E;
        } else if (y > 0.5f && centrenessX > centrenessY) {
            dir = IsoDirections.S;
        } else if (x < 0.5f && centrenessX < centrenessY) {
            dir = IsoDirections.W;
        } else if (y < 0.5f && centrenessX > centrenessY) {
            dir = IsoDirections.N;
        }
        return dir;
    }

    public void update() {
        try (AbstractPerformanceProfileProbe abstractPerformanceProfileProbe = s_performance.isoCellUpdate.profile();){
            this.updateInternal();
        }
    }

    private void updateInternal() {
        GameProfiler.ProfileArea profileArea;
        MovingObjectUpdateScheduler.instance.startFrame();
        IsoSprite.alphaStep = 0.075f * GameTime.getInstance().getThirtyFPSMultiplier();
        ++IsoGridSquare.gridSquareCacheEmptyTimer;
        GameProfiler profiler = GameProfiler.getInstance();
        try (GameProfiler.ProfileArea profileArea2 = profiler.profile("SpottedRooms");){
            this.ProcessSpottedRooms();
        }
        if (!GameServer.server) {
            for (int n = 0; n < IsoPlayer.numPlayers; ++n) {
                if (IsoPlayer.players[n] == null || IsoPlayer.players[n].isDead() && IsoPlayer.players[n].reanimatedCorpse == null) continue;
                IsoPlayer.setInstance((IsoPlayer)IsoPlayer.players[n]);
                IsoCamera.setCameraCharacter((IsoGameCharacter)IsoPlayer.players[n]);
                this.chunkMap[n].update();
            }
        }
        CompletableFuture itemsFuture = null;
        this.ProcessRemoveItems(null);
        if (!GameClient.client && !GameServer.server || GameServer.server && System.currentTimeMillis() - this.lastServerItemsUpdate > 5000L) {
            this.lastServerItemsUpdate = System.currentTimeMillis();
            profileArea = profiler.profile("Items");
            try {
                this.ProcessItems(null);
            }
            finally {
                if (profileArea != null) {
                    profileArea.close();
                }
            }
        }
        this.ProcessRemoveItems(null);
        profileArea = profiler.profile("IsoObject");
        try {
            this.ProcessIsoObject();
        }
        finally {
            if (profileArea != null) {
                profileArea.close();
            }
        }
        this.safeToAdd = false;
        profileArea = profiler.profile("Objects");
        try {
            this.ProcessObjects(null);
        }
        finally {
            if (profileArea != null) {
                profileArea.close();
            }
        }
        if (GameClient.client && (NetworkZombieSimulator.getInstance().anyUnknownZombies() && GameClient.instance.sendZombieRequestsTimer.Check() || GameClient.instance.sendZombieTimer.Check())) {
            NetworkZombieSimulator.getInstance().send();
            GameClient.instance.sendZombieTimer.Reset();
            GameClient.instance.sendZombieRequestsTimer.Reset();
        }
        if (itemsFuture != null) {
            profileArea = profiler.profile("Items");
            try {
                itemsFuture.join();
            }
            finally {
                if (profileArea != null) {
                    profileArea.close();
                }
            }
        }
        this.safeToAdd = true;
        profileArea = profiler.profile("Static Updaters");
        try {
            this.ProcessStaticUpdaters();
        }
        finally {
            if (profileArea != null) {
                profileArea.close();
            }
        }
        this.ObjectDeletionAddition();
        profileArea = profiler.profile("Update Dead Bodies");
        try {
            IsoDeadBody.updateBodies();
        }
        finally {
            if (profileArea != null) {
                profileArea.close();
            }
        }
        profileArea = profiler.profile("Update Fish");
        try {
            FishSchoolManager.getInstance().update();
        }
        finally {
            if (profileArea != null) {
                profileArea.close();
            }
        }
        IsoGridSquare.setLightcache(IsoGridSquare.getLightcache() - 1);
        IsoGridSquare.setRecalcLightTime(IsoGridSquare.getRecalcLightTime() - GameTime.getInstance().getThirtyFPSMultiplier());
        if (GameServer.server) {
            this.lamppostPositions.clear();
            this.roomLights.clear();
        }
        if (!GameTime.isGamePaused()) {
            this.rainScroll += (float)this.rainSpeed / 10.0f * 0.075f * (30.0f / (float)PerformanceSettings.getLockFPS());
            if (this.rainScroll > 1.0f) {
                this.rainScroll = 0.0f;
            }
        }
        if (!GameServer.server) {
            profileArea = profiler.profile("Update Weather");
            try {
                this.updateWeatherFx();
            }
            finally {
                if (profileArea != null) {
                    profileArea.close();
                }
            }
        }
    }

    IsoGridSquare getRandomFreeTile() {
        IsoGridSquare tile;
        boolean bDone;
        do {
            bDone = true;
            tile = this.getGridSquare(Rand.Next((int)this.width), Rand.Next((int)this.height), 0);
            if (tile == null) {
                bDone = false;
                continue;
            }
            if (!tile.isFree(false)) {
                bDone = false;
                continue;
            }
            if (tile.getProperties().has(IsoFlagType.solid) || tile.getProperties().has(IsoFlagType.solidtrans)) {
                bDone = false;
                continue;
            }
            if (!tile.getMovingObjects().isEmpty()) {
                bDone = false;
                continue;
            }
            if (tile.has(IsoObjectType.stairsBN) || tile.has(IsoObjectType.stairsMN) || tile.has(IsoObjectType.stairsTN)) {
                bDone = false;
                continue;
            }
            if (!tile.has(IsoObjectType.stairsBW) && !tile.has(IsoObjectType.stairsMW) && !tile.has(IsoObjectType.stairsTW)) continue;
            bDone = false;
        } while (!bDone);
        return tile;
    }

    IsoGridSquare getRandomOutdoorFreeTile() {
        IsoGridSquare tile;
        boolean bDone;
        do {
            bDone = true;
            tile = this.getGridSquare(Rand.Next((int)this.width), Rand.Next((int)this.height), 0);
            if (tile == null) {
                bDone = false;
                continue;
            }
            if (!tile.isFree(false)) {
                bDone = false;
                continue;
            }
            if (tile.getRoom() != null) {
                bDone = false;
                continue;
            }
            if (tile.getProperties().has(IsoFlagType.solid) || tile.getProperties().has(IsoFlagType.solidtrans)) {
                bDone = false;
                continue;
            }
            if (!tile.getMovingObjects().isEmpty()) {
                bDone = false;
                continue;
            }
            if (tile.has(IsoObjectType.stairsBN) || tile.has(IsoObjectType.stairsMN) || tile.has(IsoObjectType.stairsTN)) {
                bDone = false;
                continue;
            }
            if (!tile.has(IsoObjectType.stairsBW) && !tile.has(IsoObjectType.stairsMW) && !tile.has(IsoObjectType.stairsTW)) continue;
            bDone = false;
        } while (!bDone);
        return tile;
    }

    public IsoGridSquare getRandomFreeTileInRoom() {
        Stack<IsoRoom> rooms = new Stack<IsoRoom>();
        for (int n = 0; n < this.roomList.size(); ++n) {
            if (this.roomList.get((int)n).tileList.size() <= 9 || this.roomList.get((int)n).exits.isEmpty() || !((IsoGridSquare)this.roomList.get((int)n).tileList.get(0)).getProperties().has(IsoFlagType.solidfloor)) continue;
            rooms.add(this.roomList.get(n));
        }
        if (rooms.isEmpty()) {
            return null;
        }
        IsoRoom room = (IsoRoom)rooms.get(Rand.Next((int)rooms.size()));
        return room.getFreeTile();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void roomSpotted(IsoRoom room) {
        Stack<IsoRoom> stack = this.spottedRooms;
        synchronized (stack) {
            if (!this.spottedRooms.contains(room)) {
                this.spottedRooms.push(room);
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void ProcessSpottedRooms() {
        Stack<IsoRoom> stack = this.spottedRooms;
        synchronized (stack) {
            for (int i = 0; i < this.spottedRooms.size(); ++i) {
                IsoGridSquare square;
                IsoRoom room = (IsoRoom)this.spottedRooms.get(i);
                if (room.def.doneSpawn) continue;
                room.def.doneSpawn = true;
                LuaEventManager.triggerEvent((String)"OnSeeNewRoom", (Object)room);
                VirtualZombieManager.instance.roomSpotted(room);
                if (GameClient.client || Core.lastStand || !"shed".equals(room.def.name) && !"garage".equals(room.def.name) && !"garagestorage".equals(room.def.name) && !"storageunit".equals(room.def.name) && !"farmstorage".equals(room.def.name)) continue;
                int chance = 7;
                if ("shed".equals(room.def.name) || "garagestorage".equals(room.def.name) || "farmstorage".equals(room.def.name)) {
                    chance = 4;
                }
                switch (SandboxOptions.instance.generatorSpawning.getValue()) {
                    case 1: {
                        chance = 0;
                        break;
                    }
                    case 2: {
                        chance += 20;
                        break;
                    }
                    case 3: {
                        chance += 3;
                        break;
                    }
                    case 4: {
                        chance += 2;
                        break;
                    }
                    case 6: {
                        chance -= 2;
                        break;
                    }
                    case 7: {
                        chance -= 3;
                    }
                }
                if (room.isRural() && chance != 0 && Rand.Next((int)chance) == 0) {
                    room.spawnRandomWorkstation();
                }
                if (chance == 0 || Rand.Next((int)chance) != 0 || (square = room.getRandomDoorFreeSquare()) == null) continue;
                square.spawnRandomGenerator();
            }
            this.spottedRooms.clear();
        }
    }

    public IsoObject addTileObject(IsoGridSquare sq, String spriteName) {
        if (sq == null) {
            return null;
        }
        IsoObject obj = IsoObject.getNew(sq, spriteName, null, false);
        sq.AddTileObject(obj);
        MapObjects.newGridSquare((IsoGridSquare)sq);
        MapObjects.loadGridSquare((IsoGridSquare)sq);
        return obj;
    }

    public void save(DataOutputStream output, boolean bDoChars) throws IOException {
        while (ChunkSaveWorker.instance.saving) {
            try {
                MainThread.busyWait();
                Thread.sleep(30L);
            }
            catch (InterruptedException e) {
                ExceptionLogger.logException((Throwable)e);
            }
        }
        for (int n = 0; n < IsoPlayer.numPlayers; ++n) {
            this.chunkMap[n].Save();
        }
        output.writeInt(this.width);
        output.writeInt(this.height);
        output.writeInt(maxHeight);
        File outFile = ZomboidFileSystem.instance.getFileInCurrentSave("map_t.bin");
        FileOutputStream outStream = new FileOutputStream(outFile);
        output = new DataOutputStream(new BufferedOutputStream(outStream));
        GameTime.instance.save(output);
        output.flush();
        output.close();
        if (!GameClient.client) {
            IsoWorld.instance.metaGrid.save();
        }
        if (PlayerDB.isAllow()) {
            PlayerDB.getInstance().savePlayers();
        }
        ReanimatedPlayers.instance.saveReanimatedPlayers();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean LoadPlayer(int worldVersion) throws FileNotFoundException, IOException {
        if (GameClient.client) {
            return ClientPlayerDB.getInstance().loadNetworkPlayer();
        }
        File inFile = ZomboidFileSystem.instance.getFileInCurrentSave("map_p.bin");
        if (!inFile.exists()) {
            PlayerDB.getInstance().importPlayersFromVehiclesDB();
            return PlayerDB.getInstance().loadLocalPlayer(1);
        }
        FileInputStream inStream = new FileInputStream(inFile);
        BufferedInputStream input = new BufferedInputStream(inStream);
        Object object = SliceY.SliceBufferLock;
        synchronized (object) {
            SliceY.SliceBuffer.clear();
            int numBytes = input.read(SliceY.SliceBuffer.array());
            SliceY.SliceBuffer.limit(numBytes);
            byte b1 = SliceY.SliceBuffer.get();
            byte b2 = SliceY.SliceBuffer.get();
            byte b3 = SliceY.SliceBuffer.get();
            byte b4 = SliceY.SliceBuffer.get();
            if (b1 == 80 && b2 == 76 && b3 == 89 && b4 == 82) {
                worldVersion = SliceY.SliceBuffer.getInt();
            } else {
                SliceY.SliceBuffer.rewind();
            }
            String serverPlayerID = GameWindow.ReadString((ByteBuffer)SliceY.SliceBuffer);
            if (GameClient.client && !IsoPlayer.isServerPlayerIDValid((String)serverPlayerID)) {
                GameLoadingState.gameLoadingString = Translator.getText((String)"IGUI_MP_ServerPlayerIDMismatch", (Object[])new Object[0]);
                GameLoadingState.playerWrongIP = true;
                return false;
            }
            IsoCell.instance.chunkMap[IsoPlayer.getPlayerIndex()].worldX = SliceY.SliceBuffer.getInt() + IsoWorld.saveoffsetx * 32;
            IsoCell.instance.chunkMap[IsoPlayer.getPlayerIndex()].worldY = SliceY.SliceBuffer.getInt() + IsoWorld.saveoffsety * 32;
            SliceY.SliceBuffer.getInt();
            SliceY.SliceBuffer.getInt();
            SliceY.SliceBuffer.getInt();
            if (IsoPlayer.getInstance() == null) {
                IsoPlayer.setInstance((IsoPlayer)new IsoPlayer(instance));
                IsoPlayer.players[0] = IsoPlayer.getInstance();
            }
            IsoPlayer.getInstance().load(SliceY.SliceBuffer, worldVersion);
            inStream.close();
        }
        PlayerDB.getInstance().saveLocalPlayersForce();
        inFile.delete();
        PlayerDB.getInstance().uploadLocalPlayers2DB();
        return true;
    }

    public IsoGridSquare getRelativeGridSquare(int x, int y, int z) {
        int wx = this.chunkMap[0].getWorldXMin() * 8;
        int wy = this.chunkMap[0].getWorldYMin() * 8;
        return this.getGridSquare(x += wx, y += wy, z);
    }

    public IsoGridSquare createNewGridSquare(int x, int y, int z, boolean recalcAll) {
        if (!IsoWorld.instance.isValidSquare(x, y, z)) {
            return null;
        }
        IsoGridSquare sq = this.getGridSquare(x, y, z);
        if (sq != null) {
            return sq;
        }
        if (GameServer.server) {
            int wy;
            int wx = PZMath.coorddivision((int)x, (int)8);
            if (ServerMap.instance.getChunk(wx, wy = PZMath.coorddivision((int)y, (int)8)) != null) {
                sq = IsoGridSquare.getNew(this, null, x, y, z);
                ServerMap.instance.setGridSquare(x, y, z, sq);
            }
        } else if (this.getChunkForGridSquare(x, y, z) != null) {
            sq = IsoGridSquare.getNew(this, null, x, y, z);
            this.ConnectNewSquare(sq, true);
        }
        if (sq != null && recalcAll) {
            sq.RecalcAllWithNeighbours(true);
        }
        return sq;
    }

    public IsoGridSquare getGridSquareDirect(int x, int y, int z, int playerIndex) {
        if (GameServer.server) {
            return null;
        }
        int length = IsoChunkMap.chunkWidthInTiles;
        return this.chunkMap[playerIndex].getGridSquareDirect(x, y, z);
    }

    public boolean isInChunkMap(int x, int y) {
        for (int pn = 0; pn < IsoPlayer.numPlayers; ++pn) {
            int minX = this.chunkMap[pn].getWorldXMinTiles();
            int maxX = this.chunkMap[pn].getWorldXMaxTiles();
            int minY = this.chunkMap[pn].getWorldYMinTiles();
            int maxY = this.chunkMap[pn].getWorldYMaxTiles();
            if (x < minX || x >= maxX || y < minY || y >= maxY) continue;
            return true;
        }
        return false;
    }

    public Set<IsoObject> getProcessIsoObjectRemove() {
        return this.processIsoObjectRemove;
    }

    public void checkHaveRoof(int x, int y) {
        boolean haveRoof = false;
        for (int z = 31; z >= 0; --z) {
            IsoGridSquare sq = this.getGridSquare(x, y, z);
            if (sq == null) continue;
            if (haveRoof != sq.haveRoof) {
                sq.haveRoof = haveRoof;
                sq.RecalcAllWithNeighbours(true);
            }
            if (!sq.hasRainBlockingTile()) continue;
            haveRoof = true;
        }
    }

    public IsoZombie getFakeZombieForHit() {
        if (this.fakeZombieForHit == null) {
            this.fakeZombieForHit = new IsoZombie(this);
        }
        return this.fakeZombieForHit;
    }

    public void addHeatSource(IsoHeatSource heatSource) {
        if (this.heatSources.contains(heatSource)) {
            DebugLog.log((String)"ERROR addHeatSource called again with the same HeatSource");
            return;
        }
        this.heatSources.add(heatSource);
    }

    public void removeHeatSource(IsoHeatSource heatSource) {
        this.heatSources.remove(heatSource);
    }

    public void updateHeatSources() {
        if (GameServer.server) {
            return;
        }
        for (int i = this.heatSources.size() - 1; i >= 0; --i) {
            IsoHeatSource heatSource = this.heatSources.get(i);
            if (heatSource.isInBounds()) continue;
            this.heatSources.remove(i);
        }
    }

    public int getHeatSourceTemperature(int x, int y, int z) {
        int temperature = 0;
        for (int i = 0; i < this.heatSources.size(); ++i) {
            LosUtil.TestResults los;
            float distSq;
            IsoHeatSource heatSource = this.heatSources.get(i);
            if (heatSource.getZ() != z || !((distSq = IsoUtils.DistanceToSquared((float)x, (float)y, (float)heatSource.getX(), (float)heatSource.getY())) < (float)(heatSource.getRadius() * heatSource.getRadius())) || (los = LosUtil.lineClear((IsoCell)this, (int)heatSource.getX(), (int)heatSource.getY(), (int)heatSource.getZ(), (int)x, (int)y, (int)z, (boolean)false)) != LosUtil.TestResults.Clear && los != LosUtil.TestResults.ClearThroughOpenDoor) continue;
            temperature = (int)((double)temperature + (double)heatSource.getTemperature() * (1.0 - Math.sqrt(distSq) / (double)heatSource.getRadius()));
        }
        return temperature;
    }

    public float getHeatSourceHighestTemperature(float surroundingAirTemperature, int x, int y, int z) {
        float temperature = surroundingAirTemperature;
        for (int i = 0; i < this.heatSources.size(); ++i) {
            LosUtil.TestResults los;
            float testTemp;
            IsoHeatSource heatSource = this.heatSources.get(i);
            if (heatSource.getZ() != z) continue;
            float distSq = IsoUtils.DistanceToSquared((float)x, (float)y, (float)heatSource.getX(), (float)heatSource.getY());
            IsoGridSquare gs = this.getGridSquare(heatSource.getX(), heatSource.getY(), heatSource.getZ());
            float mod = 0.0f;
            if (gs != null) {
                if (!gs.isInARoom()) {
                    mod = surroundingAirTemperature - 30.0f;
                    if (mod < -15.0f) {
                        mod = -15.0f;
                    } else if (mod > 5.0f) {
                        mod = 5.0f;
                    }
                } else {
                    mod = surroundingAirTemperature - 30.0f;
                    if (mod < -7.0f) {
                        mod = -7.0f;
                    } else if (mod > 7.0f) {
                        mod = 7.0f;
                    }
                }
            }
            if ((testTemp = ClimateManager.lerp((float)((float)(1.0 - Math.sqrt(distSq) / (double)heatSource.getRadius())), (float)surroundingAirTemperature, (float)((float)heatSource.getTemperature() + mod))) <= temperature || !(distSq < (float)(heatSource.getRadius() * heatSource.getRadius())) || (los = LosUtil.lineClear((IsoCell)this, (int)heatSource.getX(), (int)heatSource.getY(), (int)heatSource.getZ(), (int)x, (int)y, (int)z, (boolean)false)) != LosUtil.TestResults.Clear && los != LosUtil.TestResults.ClearThroughOpenDoor) continue;
            temperature = testTemp;
        }
        return temperature;
    }

    public void putInVehicle(IsoGameCharacter chr) {
        if (chr == null || chr.savedVehicleSeat == -1) {
            return;
        }
        int chunkMinX = (PZMath.fastfloor((float)chr.getX()) - 4) / 8;
        int chunkMinY = (PZMath.fastfloor((float)chr.getY()) - 4) / 8;
        int chunkMaxX = (PZMath.fastfloor((float)chr.getX()) + 4) / 8;
        int chunkMaxY = (PZMath.fastfloor((float)chr.getY()) + 4) / 8;
        for (int wy = chunkMinY; wy <= chunkMaxY; ++wy) {
            for (int wx = chunkMinX; wx <= chunkMaxX; ++wx) {
                IsoChunk chunk = this.getChunkForGridSquare(wx * 8, wy * 8, PZMath.fastfloor((float)chr.getZ()));
                if (chunk == null) continue;
                for (int i = 0; i < chunk.vehicles.size(); ++i) {
                    BaseVehicle vehicle = chunk.vehicles.get(i);
                    if (PZMath.fastfloor((float)vehicle.getZ()) != PZMath.fastfloor((float)chr.getZ()) || !(IsoUtils.DistanceToSquared((float)vehicle.getX(), (float)vehicle.getY(), (float)chr.savedVehicleX, (float)chr.savedVehicleY) < 0.010000001f)) continue;
                    if (vehicle.vehicleId == -1) {
                        return;
                    }
                    VehicleScript.Position position = vehicle.getPassengerPosition((int)chr.savedVehicleSeat, "inside");
                    if (position != null && !vehicle.isSeatOccupied((int)chr.savedVehicleSeat)) {
                        vehicle.enter((int)chr.savedVehicleSeat, chr, position.offset);
                        LuaEventManager.triggerEvent((String)"OnEnterVehicle", (Object)chr);
                        if (vehicle.getCharacter((int)chr.savedVehicleSeat) == chr && chr.savedVehicleRunning) {
                            vehicle.resumeRunningAfterLoad();
                        }
                    }
                    return;
                }
            }
        }
    }

    @Deprecated
    public void resumeVehicleSounds(IsoGameCharacter chr) {
        if (chr == null || chr.savedVehicleSeat == -1) {
            return;
        }
        int chunkMinX = (PZMath.fastfloor((float)chr.getX()) - 4) / 8;
        int chunkMinY = (PZMath.fastfloor((float)chr.getY()) - 4) / 8;
        int chunkMaxX = (PZMath.fastfloor((float)chr.getX()) + 4) / 8;
        int chunkMaxY = (PZMath.fastfloor((float)chr.getY()) + 4) / 8;
        for (int wy = chunkMinY; wy <= chunkMaxY; ++wy) {
            for (int wx = chunkMinX; wx <= chunkMaxX; ++wx) {
                IsoChunk chunk = this.getChunkForGridSquare(wx * 8, wy * 8, PZMath.fastfloor((float)chr.getZ()));
                if (chunk == null) continue;
                for (int i = 0; i < chunk.vehicles.size(); ++i) {
                    BaseVehicle vehicle = chunk.vehicles.get(i);
                    if (!vehicle.lightbarSirenMode.isEnable()) continue;
                    vehicle.setLightbarSirenMode(vehicle.lightbarSirenMode.get());
                }
            }
        }
    }

    public void AddUniqueToBuildingList(ArrayList<IsoBuilding> buildings, IsoBuilding inBuilding) {
        for (int i = 0; i < buildings.size(); ++i) {
            if (buildings.get(i) != inBuilding) continue;
            return;
        }
        buildings.add(inBuilding);
    }

    public IsoSpriteManager getSpriteManager() {
        return IsoSpriteManager.instance;
    }

    public List<IsoAnimal> getAnimals() {
        LinkedList<IsoAnimal> animalList = new LinkedList<IsoAnimal>();
        for (IsoMovingObject obj : this.objectList) {
            if (!(obj instanceof IsoAnimal)) continue;
            IsoAnimal animal = (IsoAnimal)obj;
            animalList.add(animal);
        }
        return animalList;
    }

    @UsedFromLua
    public static boolean isBasementWallAdjacentToTheVoid_North(IsoObject object) {
        return IsoCell.isBasementWallAdjacentToTheVoid(object, IsoFlagType.WallN, IsoDirections.N);
    }

    @UsedFromLua
    public static boolean isBasementWallAdjacentToTheVoid_West(IsoObject object) {
        return IsoCell.isBasementWallAdjacentToTheVoid(object, IsoFlagType.WallW, IsoDirections.W);
    }

    private static boolean isBasementWallAdjacentToTheVoid(IsoObject object, IsoFlagType wallType, IsoDirections adjacentDir) {
        if (object == null) {
            return false;
        }
        if (!object.hasProperty(wallType) && !object.hasProperty(IsoFlagType.WallNW)) {
            return false;
        }
        IsoGridSquare square = object.getSquare();
        if (square == null) {
            return false;
        }
        if (square.getZ() >= 0) {
            return false;
        }
        if (square.getFloor() == null) {
            return true;
        }
        IsoGridSquare adjacent = square.getAdjacentSquare(adjacentDir);
        return adjacent == null || adjacent.getFloor() == null;
    }

    static {
        stchoices = new ArrayList();
        buildingscores = new Stack();
        ShadowSquares = new ArrayList(1000);
        MinusFloorCharacters = new ArrayList(1000);
        SolidFloor = new ArrayList(5000);
        ShadedFloor = new ArrayList(5000);
        VegetationCorpses = new ArrayList(5000);
        perPlayerRender = new PerPlayerRender[4];
    }
}
