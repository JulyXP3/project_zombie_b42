package modcore;

import modcore.core.LuaMethods;
import modcore.core.TeleportClimbRuntime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import zombie.ai.State;
import zombie.characters.IsoPlayer;
import zombie.characters.NetworkCharacterAI;
import zombie.characters.NetworkPlayerAI;
import zombie.characters.NetworkState;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.core.random.RandAbstract;
import zombie.core.random.RandStandard;
import zombie.network.PacketTypes;
import zombie.network.GameClient;
import zombie.network.packets.actions.StatePacket;
import zombie.network.packets.character.PlayerPacket;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class TeleportClimbRuntimeTest {
    private boolean previousClient;
    private UdpConnection previousConnection;
    private Random previousRandom;
    private Player player;
    private Connection connection;

    @BeforeEach
    void setUp() throws Exception {
        VarHandle random = MethodHandles.privateLookupIn(RandAbstract.class, MethodHandles.lookup())
                .findVarHandle(RandAbstract.class, "rand", Random.class);
        previousRandom = (Random) random.get(RandStandard.INSTANCE);
        random.set(RandStandard.INSTANCE, new Random(1));
        previousClient = GameClient.client;
        previousConnection = GameClient.connection;
        GameClient.client = true;
        connection = BytecodeFixture.allocate(Connection.class);
        connection.ready = true;
        connection.packets = new ArrayList<>();
        connection.writer = new ByteBufferWriter(ByteBuffer.allocate(16384));
        GameClient.connection = connection;
        player = newPlayer();
        LuaMethods.teleportClimbState(null, false);
        if (TeleportClimbRuntime.pending(null)) completeExit();
    }

    @AfterEach
    void tearDown() throws Exception {
        LuaMethods.teleportClimbState(null, false);
        if (TeleportClimbRuntime.pending(null)) completeExit();
        GameClient.client = previousClient;
        GameClient.connection = previousConnection;
        MethodHandles.privateLookupIn(RandAbstract.class, MethodHandles.lookup())
                .findVarHandle(RandAbstract.class, "rand", Random.class).set(RandStandard.INSTANCE, previousRandom);
    }

    @Test
    void successfulEnterSuppressesOnlyItsPlayerAndNeverRemoteTimeouts() throws Exception {
        observeHooks();
        assertTrue(LuaMethods.teleportClimbState(player, true));
        assertTrue(suppressed(player.ai));
        assertFalse(suppressed(newPlayer().ai));
        assertFalse(suppressed(BytecodeFixture.allocate(AI.class)));
    }

    @Test
    void nilExitClearsTheSessionUsingItsStoredPlayer() throws Exception {
        observeHooks();
        assertTrue(LuaMethods.teleportClimbState(player, true));
        assertTrue(LuaMethods.teleportClimbState(null, false));
        completeExit();
        assertFalse(suppressed(player.ai));
        assertEquals("exit", connection.packets.get(connection.packets.size() - 1).kind);
    }

    @Test
    void enterDoesNotClearLiveWindowStateParameters() throws Exception {
        observeHooks();
        assertTrue(LuaMethods.teleportClimbState(player, true));
        assertEquals(0, player.clearCalls);
        assertEquals("enter", connection.packets.get(0).kind);
        StatePacket packet = new StatePacket();
        packet.parse(new zombie.core.network.ByteBufferReader(payload(connection.packets.get(0))), connection);
        assertEquals("ClimbThroughWindowState", packet.getState().getName());
        assertEquals(State.Stage.Enter, packet.getStage());
    }

    @Test
    void priorMovementFinalMovementAndExitUseOneReliableOrderedStream() throws Exception {
        observeHooks();
        route(player);
        assertTrue(LuaMethods.teleportClimbState(player, true));
        player.x = 1;
        route(player);
        player.x = 2;
        assertTrue(LuaMethods.teleportClimbState(player, false));
        completeExit();
        route(player);
        assertEquals(List.of("move", "enter", "move", "move", "fence", "fence", "exit", "move"),
                connection.packets.stream().map(p -> p.kind).toList());
        byte channel = PacketTypes.PacketType.State.orderingChannel;
        for (Sent packet : connection.packets) {
            assertEquals(channel, packet.channel);
            assertEquals(3, packet.reliability);
        }
        assertEquals(5, PacketTypes.PacketType.PlayerUpdateReliable.orderingChannel,
                "PacketType's global metadata must not change");
        assertEquals(2f, connection.packets.get(3).x);
    }

    @Test
    void failedEnterNeverEnablesSuppression() throws Exception {
        observeHooks();
        connection.limited = true;
        assertFalse(LuaMethods.teleportClimbState(player, true));
        assertFalse(suppressed(player.ai));
        assertTrue(connection.packets.isEmpty());
    }

    @Test
    void disconnectedOrNewConnectionClearsWithoutSendingToTheOldSession() throws Exception {
        observeHooks();
        assertTrue(LuaMethods.teleportClimbState(player, true));
        int before = connection.packets.size();
        GameClient.connection = null;
        assertFalse(suppressed(player.ai));
        assertFalse(LuaMethods.teleportClimbState(null, false));
        assertEquals(before, connection.packets.size());
    }

    @Test
    void failedMoveStopsReadinessUntilTheSessionIsClosed() throws Exception {
        observeHooks();
        assertTrue(LuaMethods.teleportClimbState(player, true));
        connection.limited = true;
        route(player);
        assertFalse(ready(player));
        connection.limited = false;
        LuaMethods.teleportClimbState(null, false);
        completeExit();
        assertFalse(suppressed(player.ai));
    }

    @Test
    void repeatedShortSessionsKeepACompleteOrderedEnterMoveExitPair() throws Exception {
        observeHooks();
        for (int i = 0; i < 3; i++) {
            assertTrue(LuaMethods.teleportClimbState(player, true));
            player.x += 1;
            assertTrue(LuaMethods.teleportClimbState(player, false));
            completeExit();
        }
        assertEquals(List.of("enter", "move", "fence", "fence", "exit", "enter", "move", "fence", "fence", "exit",
                        "enter", "move", "fence", "fence", "exit"),
                connection.packets.stream().map(p -> p.kind).toList());
    }

    @Test
    void exitWaitsForTwoServerCyclesBeforeRemovingTheState() throws Exception {
        observeHooks();
        assertTrue(LuaMethods.teleportClimbState(player, true));
        player.x = 2;
        assertTrue(LuaMethods.teleportClimbState(player, false));
        assertEquals(List.of("enter", "move", "fence"), connection.packets.stream().map(p -> p.kind).toList());
        long first = latestFence();
        acknowledge(first);
        assertEquals(List.of("enter", "move", "fence", "fence"), connection.packets.stream().map(p -> p.kind).toList());
        long second = latestFence();
        assertNotEquals(first, second);
        acknowledge(first);
        assertTrue(suppressed(player.ai));
        acknowledge(second);
        assertFalse(suppressed(player.ai));
        assertEquals("exit", connection.packets.get(connection.packets.size() - 1).kind);
    }

    @Test
    void serverPriorityQueuesCannotProcessExitBeforeTheFinalMoveWithTwoFences() throws Exception {
        observeHooks();
        assertTrue(LuaMethods.teleportClimbState(player, true));
        player.x = 2;
        assertTrue(LuaMethods.teleportClimbState(player, false));
        java.util.ArrayDeque<Sent> high = new java.util.ArrayDeque<>();
        java.util.ArrayDeque<Sent> moves = new java.util.ArrayDeque<>();
        List<String> processed = new ArrayList<>();
        int accepted = 0;
        for (int cycle = 0; cycle < 3; cycle++) {
            while (accepted < connection.packets.size()) {
                Sent packet = connection.packets.get(accepted++);
                (packet.kind.equals("move") ? moves : high).add(packet);
            }
            List<Sent> snapshot = new ArrayList<>(high);
            high.clear();
            for (Sent packet : snapshot) {
                processed.add(packet.kind);
                if (packet.kind.equals("fence")) acknowledge(ByteBuffer.wrap(packet.bytes).getLong(3));
            }
            while (!moves.isEmpty()) processed.add(moves.remove().kind);
        }
        assertEquals(List.of("enter", "fence", "move", "fence", "exit"), processed);
        assertTrue(processed.indexOf("move") < processed.indexOf("exit"));
    }

    @Test
    void movementGateKeepsVanillaPacketCadenceRatherThanForcingEveryFrame() throws Exception {
        observeHooks();
        assertTrue(LuaMethods.teleportClimbState(player, true));
        player.ai.needUpdateCalls = 0;
        player.ai.skipSelection = true;
        route(player);
        assertEquals(0, player.ai.needUpdateCalls);
        assertEquals(List.of("enter"), connection.packets.stream().map(p -> p.kind).toList());
        assertTrue(ready(player));
    }

    @Test
    void exitStillForcesOneFinalMovementWhenVanillaTimerWouldSkipIt() throws Exception {
        observeHooks();
        assertTrue(LuaMethods.teleportClimbState(player, true));
        player.ai.skipSelection = true;
        player.x = 3;
        assertTrue(LuaMethods.teleportClimbState(player, false));
        assertEquals(3f, connection.packets.get(1).x);
        completeExit();
    }

    @Test
    void cancellingDuringFenceWaitKeepsOrdinaryWalkingUpdatesOrdered() throws Exception {
        observeHooks();
        assertTrue(LuaMethods.teleportClimbState(player, true));
        assertTrue(LuaMethods.teleportClimbState(player, false));
        int count = connection.packets.size();
        player.x = 3;
        route(player);
        assertEquals(count + 1, connection.packets.size());
        assertEquals("move", connection.packets.get(connection.packets.size() - 1).kind);
        completeExit();
        route(player);
        assertEquals("move", connection.packets.get(connection.packets.size() - 1).kind);
    }

    @Test
    void echoFromAnotherConnectionCannotCloseTheSession() throws Exception {
        observeHooks();
        assertTrue(LuaMethods.teleportClimbState(player, true));
        assertTrue(LuaMethods.teleportClimbState(player, false));
        Connection other = BytecodeFixture.allocate(Connection.class);
        TeleportClimbRuntime.acknowledgeFence(latestFence(), other);
        assertTrue(TeleportClimbRuntime.pending(player));
        completeExit();
    }

    @Test
    void freshTimeSyncInstancesEchoExactlyTheBarrierTokenOnTheServer() throws Exception {
        observeHooks();
        boolean previousServer = zombie.network.GameServer.server;
        boolean previousClient = GameClient.client;
        try {
            GameClient.client = false;
            zombie.network.GameServer.server = true;
            long token = 99887766L;
            ByteBuffer data = ByteBuffer.allocate(16).putLong(token).putLong(0L).flip();
            zombie.network.packets.service.TimeSyncPacket incoming = new zombie.network.packets.service.TimeSyncPacket();
            incoming.parse(new zombie.core.network.ByteBufferReader(data), connection);
            connection.echoPacket = incoming;
            incoming.processServer(PacketTypes.PacketType.TimeSync, connection);
            assertEquals(token, ByteBuffer.wrap(connection.packets.get(0).bytes).getLong(3));
        } finally {
            connection.echoPacket = null;
            zombie.network.GameServer.server = previousServer;
            GameClient.client = previousClient;
        }
    }

    @Test
    void exitConstructionFailureNeverLeaksSuppressionOrEscapesTheCallback() throws Exception {
        observeHooks();
        assertTrue(LuaMethods.teleportClimbState(player, true));
        assertTrue(LuaMethods.teleportClimbState(null, false));
        acknowledge(latestFence());
        player.failCoordinates = true;
        assertDoesNotThrow(() -> acknowledge(latestFence()));
        assertFalse(suppressed(player.ai));
        assertTrue(closeFailed(player));
    }

    @Test
    void abortedFinalMovementClearsEvenWhenItsExitPacketCannotBeConstructed() throws Exception {
        observeHooks();
        assertTrue(LuaMethods.teleportClimbState(player, true));
        player.failCoordinates = true;
        assertFalse(assertDoesNotThrow(() -> LuaMethods.teleportClimbState(null, false)));
        assertFalse(suppressed(player.ai));
        assertTrue(closeFailed(player));
    }

    @Test
    void rejectedFinalExitIsReportedInsteadOfBeingACompletedClose() throws Exception {
        observeHooks();
        assertTrue(LuaMethods.teleportClimbState(player, true));
        assertTrue(LuaMethods.teleportClimbState(null, false));
        acknowledge(latestFence());
        connection.limited = true;
        acknowledge(latestFence());
        assertFalse(suppressed(player.ai));
        assertTrue(closeFailed(player));
        assertNotEquals("exit", connection.packets.get(connection.packets.size() - 1).kind);
    }

    @Test
    void closeTimeoutReportsFailureWithoutKeepingALocalStateGate() throws Exception {
        observeHooks();
        assertTrue(LuaMethods.teleportClimbState(player, true));
        assertTrue(LuaMethods.teleportClimbState(null, false));
        Class<?> sessionType = Class.forName("modcore.core.TeleportClimbRuntime$Session");
        Object active = MethodHandles.privateLookupIn(TeleportClimbRuntime.class, MethodHandles.lookup())
                .findStaticVarHandle(TeleportClimbRuntime.class, "session", sessionType).get();
        MethodHandles.privateLookupIn(sessionType, MethodHandles.lookup())
                .findVarHandle(sessionType, "closeDeadline", long.class).set(active, 0L);
        assertFalse(TeleportClimbRuntime.pending(player));
        assertFalse(suppressed(player.ai));
        assertTrue(closeFailed(player));
        GameClient.connection = null;
        assertFalse(closeFailed(player), "a failure must not follow the player into another connection");
    }

    private static boolean closeFailed(IsoPlayer player) throws Exception {
        return (boolean) TeleportClimbRuntime.class.getMethod("closeFailed", IsoPlayer.class).invoke(null, player);
    }

    private void completeExit() throws Exception {
        long first = latestFence();
        acknowledge(first);
        acknowledge(latestFence());
    }

    private long latestFence() {
        Sent packet = connection.packets.get(connection.packets.size() - 1);
        return ByteBuffer.wrap(packet.bytes).getLong(3);
    }

    private void acknowledge(long token) throws Exception {
        TeleportClimbRuntime.class.getMethod("acknowledgeFence", long.class, UdpConnection.class)
                .invoke(null, token, connection);
    }

    private void observeHooks() throws Exception {
        TeleportClimbRuntime.class.getMethod("observePostUpdate", NetworkCharacterAI.class).invoke(null, player.ai);
        TeleportClimbRuntime.class.getMethod("observePlayerSender").invoke(null);
        TeleportClimbRuntime.class.getMethod("observeFenceReceiver").invoke(null);
        suppressed(player.ai);
        route(player);
        connection.packets.clear();
    }

    private static boolean suppressed(NetworkCharacterAI ai) throws Exception {
        return (boolean) TeleportClimbRuntime.class.getMethod("suppressActive", NetworkCharacterAI.class).invoke(null, ai);
    }

    private static boolean route(IsoPlayer player) throws Exception {
        return (boolean) TeleportClimbRuntime.class.getMethod("routePlayerUpdate", IsoPlayer.class).invoke(null, player);
    }

    private static boolean ready(IsoPlayer player) throws Exception {
        return (boolean) LuaMethods.class.getMethod("teleportClimbReady", IsoPlayer.class).invoke(null, player);
    }

    private static Player newPlayer() throws Exception {
        Player p = BytecodeFixture.allocate(Player.class);
        p.ai = BytecodeFixture.allocate(AI.class);
        p.ai.player = p;
        p.ai.packet = new PlayerPacket();
        return p;
    }

    private static ByteBuffer payload(Sent packet) {
        ByteBuffer b = ByteBuffer.wrap(packet.bytes);
        b.position(3);
        return b;
    }

    public static final class Player extends IsoPlayer {
        AI ai;
        float x;
        int clearCalls;
        boolean failCoordinates;
        private Player() { super(null); }
        @Override public boolean isLocalPlayer() { return true; }
        @Override public boolean isDead() { return false; }
        @Override public float getX() {
            if (failCoordinates) throw new IllegalStateException("test player position unavailable");
            return x;
        }
        @Override public float getY() { return 0; }
        @Override public float getZ() { return 0; }
        @Override public NetworkPlayerAI getNetworkCharacterAI() { return ai; }
        @Override public String getActionStateName() { return "idle"; }
        @Override public float getVariableFloat(String name, float defaultValue) { return defaultValue; }
        @Override public void clear(State state) { clearCalls++; }
    }

    public static final class AI extends NetworkPlayerAI {
        Player player;
        PlayerPacket packet;
        boolean skipSelection;
        int needUpdateCalls;
        boolean requested;
        private AI() { super(null); }
        @Override public PlayerPacket getPlayerPacket() { return packet; }
        @Override public void needToUpdate() { needUpdateCalls++; requested = true; }
        @Override public boolean isReliable() { return false; }
        @Override public PacketTypes.PacketType set(PlayerPacket p) {
            if (skipSelection && !requested) return null;
            requested = false;
            p.prediction.x = player.x;
            p.prediction.y = 0;
            p.prediction.z = 0;
            p.prediction.position.set(player.x, 0, 0);
            return PacketTypes.PacketType.PlayerUpdateReliable;
        }
    }

    record Sent(String kind, float x, int reliability, byte channel, byte[] bytes) { }

    public static final class Connection extends UdpConnection {
        ByteBufferWriter writer;
        List<Sent> packets;
        boolean ready, limited;
        zombie.network.packets.INetworkPacket echoPacket;
        private Connection() { super(null, 0, 0); }
        @Override public zombie.network.packets.INetworkPacket getPacket(PacketTypes.PacketType type) { return echoPacket; }
        @Override public boolean isReady() { return ready; }
        @Override public boolean isLimitExceeded(PacketTypes.PacketType type) { return limited; }
        @Override public long getConnectedGUID() { return 1; }
        @Override public IsoPlayer getPlayerAt(int index) { return null; }
        @Override public ByteBufferWriter startPacket() { writer.bb.clear(); return writer; }
        @Override public void cancelPacket() { }
        @Override public void endPacket(int priority, int reliability, byte channel) {
            ByteBuffer b = writer.bb.duplicate().flip();
            byte[] bytes = new byte[b.remaining()];
            b.get(bytes);
            b = ByteBuffer.wrap(bytes);
            b.get();
            short id = b.getShort();
            String kind = "move";
            float x = 0;
            if (id == PacketTypes.PacketType.TimeSync.getId()) {
                kind = "fence";
            } else if (id == PacketTypes.PacketType.State.getId()) {
                int stage = ByteBuffer.wrap(bytes).getInt(bytes.length - 4);
                kind = stage == State.Stage.Enter.ordinal() ? "enter" : "exit";
            } else {
                b.position(3);
                zombie.network.fields.character.ConnectionPlayer identity = new zombie.network.fields.character.ConnectionPlayer();
                zombie.core.network.ByteBufferReader reader = new zombie.core.network.ByteBufferReader(b);
                identity.parse(reader, this);
                zombie.network.fields.character.Prediction prediction = new zombie.network.fields.character.Prediction();
                prediction.parse(reader, this);
                x = prediction.x;
            }
            packets.add(new Sent(kind, x, reliability, channel, bytes));
        }
    }
}
