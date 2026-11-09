package modcore;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.tree.FieldNode;
import zombie.core.physics.Transform;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Runs the real teleport primitives; only native physics and world bookkeeping are replaced. */
public class VehicleTeleportPoseTest {
    @Test
    void nativeWheelEncodingAndSerializedIntegerDescribeTheSamePose() throws Throwable {
        Class<?> api = runtime();
        var lookup = MethodHandles.privateLookupIn(api, MethodHandles.lookup());
        var matches = lookup.findStatic(api, "poseMatches", MethodType.methodType(boolean.class, float[].class, float[].class));
        float[] nativePose = pose(10, 20);
        assertTrue((boolean) matches.invoke(nativePose, nativePose.clone()), "native four-wheel reads contain 4.1, not 4.0");
        float[] serialized = nativePose.clone();
        serialized[10] = 4.0f;
        assertTrue((boolean) matches.invoke(nativePose, serialized), "vanilla serialization truncates the wheel count");
        for (float invalid : new float[]{Float.NaN, Float.POSITIVE_INFINITY, -0.1f, 5.1f, 3.1f}) {
            float[] other = nativePose.clone();
            other[10] = invalid;
            assertFalse((boolean) matches.invoke(nativePose, other), "invalid or different wheel count: " + invalid);
        }
    }

    @Test
    void successfulNativeReturnFiveRestoresAllSourcesAndAllowsAnotherHop() throws Exception {
        Class<?> api = runtime();
        assertEquals("ok", hop(api, 5));
        assertEquals("ok", call(api, "nativeRollback"));
        assertEquals(10, Physics.pose[0], 0.001f);
        assertEquals(10, Vehicle.instance.x, 0.001f);
        assertEquals(10.4f, Player.instance.x, 0.001f);
        assertFalse(saved(api));
        assertEquals("ok", hop(api, 8));
        assertEquals(18, Physics.pose[0], 0.001f);
    }

    @Test
    void delayedRollbackIsConfirmedWithoutRewritingTheOldPosition() throws Exception {
        Class<?> api = runtime();
        assertEquals("ok", hop(api, 5));
        Physics.delayRestore = true;
        assertNotEquals("ok", call(api, "nativeRollback"));
        assertTrue(saved(api));
        int writes = Physics.writes;
        Physics.completeRestore();
        step(api, 100);
        assertFalse(saved(api), "a fresh aligned rollback must finish the saved transaction");
        assertEquals(writes, Physics.writes, "confirmation must not write another native pose");
        assertEquals("ok", hop(api, 8));
    }

    @Test
    void confirmedNativeRestoreFollowedByNormalDrivingNeverPullsTheCarBack() throws Exception {
        Class<?> api = runtime();
        assertEquals("ok", hop(api, 5));
        Manager.delayBookkeeping = true;
        assertNotEquals("ok", call(api, "nativeRollback"));
        int writes = Physics.writes;
        Manager.delayBookkeeping = false;
        Physics.pose = pose(30, 40);
        align();
        step(api, 100);
        assertFalse(saved(api), "the old native pose was observed, then normal simulation resumed in sync");
        assertEquals(30, Physics.pose[0], 0.001f);
        assertEquals(writes, Physics.writes, "normal driving must not trigger a write to the old start");
        assertEquals("ok", hop(api, 5));
        assertEquals(35, Physics.pose[0], 0.001f);
    }

    @Test
    void unconfirmedRestoreTimesOutWithoutDiscardingTheSaveOrStartingANewWrite() throws Exception {
        Class<?> api = runtime();
        assertEquals("ok", hop(api, 5));
        Physics.delayRestore = true;
        assertNotEquals("ok", call(api, "nativeRollback"));
        int writes = Physics.writes;
        Clock.now += 1100;
        WorldPhysics.instance.frame++;
        api.getMethod("discardRemovedVehicleState").invoke(null);
        assertTrue(saved(api));
        assertEquals("blocked-restore-timeout", hop(api, 5));
        assertEquals(writes, Physics.writes);
        Physics.completeRestore();
        assertEquals("ok", hop(api, 5), "a later click may confirm the original pose, never force it again");
    }

    @Test
    void alignedAtAnUnrelatedPositionDoesNotProveTheRequestedRestoreHappened() throws Exception {
        Class<?> api = runtime();
        assertEquals("ok", hop(api, 5));
        Physics.delayRestore = true;
        assertNotEquals("ok", call(api, "nativeRollback"));
        Physics.pose = pose(30, 40);
        align();
        int writes = Physics.writes;
        step(api, 100);
        assertTrue(saved(api));
        assertEquals("blocked-pending-restore", hop(api, 5));
        assertEquals(writes, Physics.writes);
    }

    @Test
    void negativeNativeReturnIsReportedInsteadOfClaimingAHopSucceeded() throws Exception {
        Class<?> api = runtime();
        Physics.writeResult = -5;
        String result = (String) hop(api, 5);
        assertTrue(result.startsWith("physics-rejected(rc=-5"), result);
        assertEquals(10, Physics.pose[0], 0.001f);
        assertEquals(10, Vehicle.instance.x, 0.001f);
    }

    @Test
    void aRejectedUnchangedHopCanBeConfirmedBeforeTheNextAttempt() throws Exception {
        Class<?> api = runtime();
        Physics.writeResult = -5;
        assertTrue(((String) hop(api, 5)).startsWith("physics-rejected(rc=-5"));
        Physics.writeResult = 5;
        assertEquals("ok", hop(api, 5), "the saved starting pose is still present, so no rollback write is needed");
        assertEquals(15, Physics.pose[0], 0.001f);
    }

    @Test
    void theSamePhysicsFrameIsNotReadRepeatedlyWhileRecoveryIsPending() throws Exception {
        Class<?> api = runtime();
        assertEquals("ok", hop(api, 5));
        Physics.delayRestore = true;
        assertNotEquals("ok", call(api, "nativeRollback"));
        step(api, 100);
        int reads = Physics.reads;
        Clock.now += 100;
        api.getMethod("discardRemovedVehicleState").invoke(null);
        assertEquals(reads, Physics.reads, "no new physical frame means no new recovery evidence");
        assertTrue(saved(api));
    }

    @Test
    void aStoppedEngineWithoutLocalSimulationAsksToStartTheVehicle() throws Exception {
        Class<?> api = runtime();
        Vehicle.localSim = false;
        Vehicle.engineRunning = false;
        assertEquals("engine-off", api.getMethod("teleportSimulationState", Vehicle.class)
                .invoke(null, Vehicle.instance));
        Vehicle.engineRunning = true;
        assertEquals("not-local", api.getMethod("teleportSimulationState", Vehicle.class)
                .invoke(null, Vehicle.instance));
        Vehicle.localSim = true;
        Vehicle.engineRunning = false;
        assertEquals("ok", api.getMethod("teleportSimulationState", Vehicle.class)
                .invoke(null, Vehicle.instance));
    }

    private static Object hop(Class<?> api, float dx) throws Exception {
        return api.getMethod("nativeHop", Vehicle.class, float.class, float.class, float.class)
                .invoke(null, Vehicle.instance, dx, 0.0f, 3.7f);
    }

    private static Object call(Class<?> api, String name) throws Exception {
        return api.getMethod(name, Vehicle.class).invoke(null, Vehicle.instance);
    }

    private static boolean saved(Class<?> api) throws Exception {
        return (boolean) call(api, "hasSavedPose");
    }

    private static void step(Class<?> api, long elapsed) throws Exception {
        Clock.now += elapsed;
        WorldPhysics.instance.frame++;
        api.getMethod("discardRemovedVehicleState").invoke(null);
    }

    private static float[] pose(float x, float y) {
        float[] pose = new float[27];
        pose[0] = x;
        pose[1] = y;
        pose[2] = 0.2f;
        pose[6] = 1.0f;
        pose[10] = 4.1f;
        for (int i = 0; i < 4; i++) pose[14 + i * 4] = 0.2f;
        return pose;
    }

    private static void align() {
        Vehicle vehicle = Vehicle.instance;
        vehicle.jniTransform.origin.set(Physics.pose[0], Physics.pose[2], Physics.pose[1]);
        vehicle.jniTransform.setRotation(new Quaternionf(Physics.pose[3], Physics.pose[4], Physics.pose[5], Physics.pose[6]));
        vehicle.x = Physics.pose[0];
        vehicle.y = Physics.pose[1];
        vehicle.square = World.instance.currentCell.getGridSquare(vehicle.x, vehicle.y, 0);
        vehicle.chunk = vehicle.square.chunk;
        vehicle.chunk.vehicles.add(vehicle);
        Player.instance.x = vehicle.x + 0.4f;
        Player.instance.y = vehicle.y - 0.2f;
        Player.instance.z = 0;
    }

    private static Class<?> runtime() throws Exception {
        Clock.now = 1000;
        World.instance = new World();
        WorldPhysics.instance = new WorldPhysics();
        Vehicle.instance = new Vehicle();
        Player.instance = new Player();
        Physics.pose = pose(10, 20);
        Physics.pending = null;
        Physics.writes = 0;
        Physics.reads = 0;
        Physics.writeResult = 5;
        Physics.delayRestore = false;
        Manager.instance = new Manager();
        Manager.delayBookkeeping = false;
        Vehicle.localSim = true;
        Vehicle.engineRunning = true;
        align();
        Map<String, String> names = Map.ofEntries(
                Map.entry("modcore/core/VehicleTeleportAPI", "modcore/fixture/VehicleTeleportPose"),
                Map.entry("modcore/utils/Logger", BytecodeFixture.owner(Log.class)),
                Map.entry("java/lang/System", BytecodeFixture.owner(Clock.class)),
                Map.entry("zombie/characters/IsoGameCharacter", BytecodeFixture.owner(Character.class)),
                Map.entry("zombie/characters/IsoPlayer", BytecodeFixture.owner(Player.class)),
                Map.entry("zombie/core/physics/Bullet", BytecodeFixture.owner(Physics.class)),
                Map.entry("zombie/core/physics/WorldSimulation", BytecodeFixture.owner(WorldPhysics.class)),
                Map.entry("zombie/iso/IsoChunk", BytecodeFixture.owner(Chunk.class)),
                Map.entry("zombie/iso/IsoChunkMap", BytecodeFixture.owner(ChunkMap.class)),
                Map.entry("zombie/iso/IsoGridSquare", BytecodeFixture.owner(Square.class)),
                Map.entry("zombie/iso/IsoWorld", BytecodeFixture.owner(World.class)),
                Map.entry("zombie/iso/IsoCell", BytecodeFixture.owner(Cell.class)),
                Map.entry("zombie/iso/IsoMetaGrid", BytecodeFixture.owner(MetaGrid.class)),
                Map.entry("zombie/vehicles/BaseVehicle", BytecodeFixture.owner(Vehicle.class)),
                Map.entry("zombie/vehicles/BaseVehicle$WheelInfo", BytecodeFixture.owner(Wheel.class)),
                Map.entry("zombie/vehicles/VehicleManager", BytecodeFixture.owner(Manager.class)),
                Map.entry("zombie/scripting/objects/VehicleScript", BytecodeFixture.owner(Script.class)));
        var source = BytecodeFixture.read("modcore/core/VehicleTeleportAPI");
        var target = BytecodeFixture.type("modcore/fixture/VehicleTeleportPose");
        target.methods.clear();
        for (FieldNode field : source.fields) {
            String descriptor = field.desc;
            for (var entry : names.entrySet()) descriptor = descriptor.replace("L" + entry.getKey() + ";", "L" + entry.getValue() + ";");
            target.fields.add(new FieldNode(field.access, field.name, descriptor, null, field.value));
        }
        for (var method : source.methods) target.methods.add(BytecodeFixture.copy(method, names));
        return BytecodeFixture.define(target);
    }

    public static class Clock {
        public static long now;
        public static long nanoTime() { return now * 1_000_000; }
        public static void arraycopy(Object source, int from, Object target, int to, int count) {
            System.arraycopy(source, from, target, to, count);
        }
    }
    public static class Log {
        public static void printLog(String message) {}
        public static void error(String message, Throwable error) { throw new AssertionError(message, error); }
    }
    public static class Physics {
        public static float[] pose, pending;
        public static boolean delayRestore;
        public static int writes, reads, writeResult;
        public static int getOwnVehiclePhysics(int id, float[] target) { reads++; System.arraycopy(pose, 0, target, 0, 27); return 0; }
        public static int setOwnVehiclePhysics(int id, float[] target, boolean hard) {
            writes++;
            if (writeResult < 0) return writeResult;
            if (delayRestore) pending = target.clone(); else pose = target.clone();
            return writeResult;
        }
        public static void completeRestore() { pose = pending; pending = null; delayRestore = false; align(); }
    }
    public static class WorldPhysics {
        public static WorldPhysics instance;
        public float offsetX, offsetY;
        public int frame;
        public int getBulletFrameNo() { return frame; }
    }
    public static class Character {
        public float x, y, z;
        public float getX() { return x; } public float getY() { return y; } public float getZ() { return z; }
        public float setX(float value) { return x = value; } public float setY(float value) { return y = value; }
        public float setZ(float value) { return z = value; }
        public Vehicle getVehicle() { return Vehicle.instance; }
    }
    public static class Player extends Character {
        public static Player instance;
        public static Player getInstance() { return instance; }
        public boolean isDead() { return false; } public boolean isLocalPlayer() { return true; }
        public void setCurrentSquareFromPosition() {}
    }
    public static class Wheel { public float steering, rotation, skidInfo, suspensionLength; }
    public static class Script { public float getSuspensionRestLength() { return 0.3f; } }
    public static class Vehicle {
        public static Vehicle instance;
        public static final float MINIMUM_DOT_UPRIGHT = 0.8f;
        public static boolean localSim = true, engineRunning = true;
        public short vehicleId = 1;
        public float x, y, z;
        public final Transform jniTransform = new Transform();
        public final Quaternionf savedRot = new Quaternionf();
        public final Vector3f jniLinearVelocity = new Vector3f();
        public boolean isActive = true, isStatic, jniIsCollide, polyDirty;
        public Wheel[] wheelInfo = {new Wheel(), new Wheel(), new Wheel(), new Wheel()};
        public Chunk chunk;
        public Square square;
        public boolean isRemovedFromWorld() { return false; }
        public Cell getCell() { return World.instance.currentCell; }
        public boolean isDriver(Character player) { return player == Player.instance; }
        public boolean isLocalPhysicSim() { return localSim; }
        public boolean isEngineRunning() { return engineRunning; }
        public Vehicle getVehicleTowing() { return null; } public Vehicle getVehicleTowedBy() { return null; }
        public boolean isWaitingToReattachTowingPartner() { return false; }
        public float getX() { return x; } public float getY() { return y; } public float getZ() { return z; }
        public float setZ(float value) { return z = value; }
        public boolean isPhysicsActive() { return isActive; }
        public void setPhysicsActive(boolean value) { isActive = value; isStatic = !value; }
        public void checkSurroundingChunks() {}
        public Script getScript() { return new Script(); }
        public float getUpVectorDot() { return 1; }
        public Square getSquare() { return square; } public Square getCurrentSquare() { return square; }
        public int getMaxPassengers() { return 1; }
        public Character getCharacter(int seat) { return Player.instance; }
        public Vector3f getPassengerWorldPos(int seat, Vector3f target) { return target.set(x + 0.4f, y - 0.2f, 0); }
    }
    public static class Chunk { public boolean loaded = true; public ArrayList<Vehicle> vehicles = new ArrayList<>(); }
    public static class Square {
        public final Chunk chunk; public int z;
        public Square(Chunk chunk) { this.chunk = chunk; }
        public boolean hasFloor() { return true; }
    }
    public static class ChunkMap {
        public static int chunkGridWidth = 9;
        public int worldX, worldY;
        public Chunk getChunkForGridSquare(int x, int y) { return World.instance.currentCell.chunk; }
    }
    public static class Cell {
        public final Chunk chunk = new Chunk();
        public Chunk getChunk(int x, int y) { return chunk; }
        public ChunkMap getChunkMap(int player) { return new ChunkMap(); }
        public Square getGridSquare(double x, double y, double z) { return new Square(chunk); }
    }
    public static class MetaGrid { public boolean isValidChunk(int x, int y) { return true; } }
    public static class World {
        public static World instance;
        public Cell currentCell = new Cell();
        public MetaGrid getMetaGrid() { return new MetaGrid(); }
    }
    public static class Manager {
        public static Manager instance;
        public static boolean delayBookkeeping;
        public void clientUpdateVehiclePos(Vehicle vehicle, float x, float y, float z, Square square) {
            if (delayBookkeeping) return;
            vehicle.x = x; vehicle.y = y; vehicle.square = square; vehicle.chunk = square.chunk;
            square.chunk.vehicles.add(vehicle);
        }
    }
}
