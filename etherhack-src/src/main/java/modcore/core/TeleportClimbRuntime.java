package modcore.core;

import modcore.utils.Logger;
import zombie.ai.State;
import zombie.ai.states.ClimbThroughWindowState;
import zombie.characters.IsoPlayer;
import zombie.characters.NetworkCharacterAI;
import zombie.characters.NetworkPlayerAI;
import zombie.core.raknet.UdpConnection;
import zombie.network.GameClient;
import zombie.network.PacketTypes;
import zombie.network.packets.actions.StatePacket;
import zombie.network.packets.character.PlayerPacket;

/** Player-scoped state gate and ordered movement stream; no ASM runtime dependency. */
public final class TeleportClimbRuntime {
    private static boolean stateGateSeen;
    private static boolean playerSenderSeen;
    private static boolean fenceReceiverSeen;
    private static Session session;
    private static Session closeFailure;
    private static long lastFence;
    private static final long CLOSE_TIMEOUT_NS = 5_000_000_000L;
    private static final long SESSION_TIMEOUT_NS = 121_000_000_000L;

    private TeleportClimbRuntime() {
    }

    public static synchronized void observePostUpdate(NetworkCharacterAI ai) {
        stateGateSeen = true;
    }

    public static synchronized void observePlayerSender() {
        playerSenderSeen = true;
    }

    public static synchronized void observeFenceReceiver() {
        fenceReceiverSeen = true;
    }

    public static synchronized void acknowledgeFence(long token, UdpConnection connection) {
        Session current = currentSession();
        if (current == null || !current.closing || current.connection != connection || current.fence != token) return;
        if (current.fenceStage == 1) {
            current.fenceStage = 2;
            if (!sendFence(current)) abortClose(current);
        } else {
            closeState(current, false);
        }
    }

    public static synchronized boolean closeFailed(IsoPlayer player) {
        currentSession();
        return closeFailure != null && (player == null || closeFailure.player == player);
    }

    public static synchronized boolean pending(IsoPlayer player) {
        Session current = currentSession();
        return current != null && (player == null || current.player == player) && current.closing;
    }

    public static synchronized boolean suppressActive(NetworkCharacterAI ai) {
        Session current = currentSession();
        return current != null && current.player.getNetworkCharacterAI() == ai;
    }

    public static synchronized boolean ready(IsoPlayer player) {
        Session current = currentSession();
        return current != null && current.player == player && !current.failed && !current.closing;
    }

    public static synchronized boolean enter(IsoPlayer player) {
        if (player == null || !stateGateSeen || !playerSenderSeen || !fenceReceiverSeen || !GameClient.client
                || !player.isLocalPlayer() || GameClient.connection == null || !GameClient.connection.isReady()) {
            return false;
        }
        Session existing = currentSession();
        if (existing != null) return existing.player == player && !existing.failed && !existing.closing;
        UdpConnection connection = GameClient.connection;
        try {
            if (!PacketSubmit.send(connection, PacketTypes.PacketType.State, new ClimbPacket(player, State.Stage.Enter))) {
                return false;
            }
            closeFailure = null;
            session = new Session(player, connection, connection.getConnectedGUID());
            return true;
        } catch (Throwable t) {
            Logger.printLog("teleportClimbState enter failed: " + t);
            return false;
        }
    }

    public static synchronized boolean exit() {
        Session current = currentSession();
        if (current == null) {
            session = null;
            return false;
        }
        if (current.closing) return true;
        try {
            current.closing = true;
            current.failed = false;
            current.closeDeadline = System.nanoTime() + CLOSE_TIMEOUT_NS;
            current.fenceStage = 1;
            if (!sendMovement(current.player, current.connection, true) || !sendFence(current)) {
                abortClose(current);
                return false;
            }
            return true;
        } catch (Throwable t) {
            Logger.printLog("teleportClimbState exit failed: " + t);
            abortClose(current);
            return false;
        }
    }

    private static boolean sendFence(Session current) {
        long token = Math.max(System.nanoTime(), lastFence + 1);
        lastFence = token;
        current.fence = token;
        return PacketSubmit.send(current.connection, PacketTypes.PacketType.TimeSync, new FencePacket(token),
                0, 3, PacketTypes.PacketType.State.orderingChannel);
    }

    private static void abortClose(Session current) {
        closeState(current, true);
    }

    private static void closeState(Session current, boolean failed) {
        boolean sent = false;
        try {
            sent = PacketSubmit.send(current.connection, PacketTypes.PacketType.State,
                    new ClimbPacket(current.player, State.Stage.Exit));
        } catch (Throwable t) {
            Logger.printLog("teleport state close failed: " + t);
        } finally {
            // Clear locally even if packet construction fails; do not report a failed close as completion.
            if (session == current) session = null;
            closeFailure = failed || !sent ? current : null;
        }
    }

    public static synchronized boolean routePlayerUpdate(IsoPlayer player) {
        Session current = currentSession();
        if (!stateGateSeen || !playerSenderSeen || player == null || !GameClient.client || !player.isLocalPlayer()
                || GameClient.connection == null || !GameClient.connection.isReady()) return false;
        if (current != null && current.player == player && current.failed) return true;
        try {
            boolean sent = sendMovement(player, GameClient.connection, false);
            if (!sent && current != null && current.player == player) current.failed = true;
        } catch (Throwable t) {
            if (current != null && current.player == player) current.failed = true;
            Logger.printLog("teleport movement submission failed: " + t);
        }
        // Consumed even on failure: do not silently send the same moved position on vanilla channel 5.
        return true;
    }

    private static boolean sendMovement(IsoPlayer player, UdpConnection connection, boolean force) {
        NetworkPlayerAI ai = player.getNetworkCharacterAI();
        if (force || ai.isReliable()) ai.needToUpdate();
        PlayerPacket packet = ai.getPlayerPacket();
        PacketTypes.PacketType type = packet.set(player);
        if (type == null) return !force;
        if (force || session != null && session.player == player && !session.closing) {
            packet.prediction.x = player.getX();
            packet.prediction.y = player.getY();
            packet.prediction.z = (byte) Math.floor(player.getZ());
            packet.prediction.position.set(player.getX(), player.getY(), player.getZ());
            packet.prediction.distance = 0;
        }
        // Keep the stream ordered before Enter and after Exit as well, without mutating PacketType metadata.
        return PacketSubmit.send(connection, type, packet, type.packetPriority, 3,
                PacketTypes.PacketType.State.orderingChannel);
    }

    private static boolean sameConnection(Session current) {
        return GameClient.client && GameClient.connection == current.connection
                && current.connection.isReady() && current.connection.getConnectedGUID() == current.guid
                && current.player.isLocalPlayer();
    }

    private static Session currentSession() {
        if (closeFailure != null && !sameConnection(closeFailure)) closeFailure = null;
        if (session != null && !sameConnection(session)) session = null;
        if (session != null && !session.closing && System.nanoTime() >= session.deadline) {
            Logger.printLog("teleport session timed out without Lua cleanup");
            abortClose(session);
        }
        if (session != null && session.closing && System.nanoTime() >= session.closeDeadline) {
            Logger.printLog("teleport state close timed out");
            abortClose(session);
        }
        return session;
    }

    private static final class Session {
        final IsoPlayer player;
        final UdpConnection connection;
        final long guid;
        final long deadline = System.nanoTime() + SESSION_TIMEOUT_NS;
        boolean failed;
        boolean closing;
        int fenceStage;
        long fence;
        long closeDeadline;

        Session(IsoPlayer player, UdpConnection connection, long guid) {
            this.player = player;
            this.connection = connection;
            this.guid = guid;
        }
    }

    private static final class FencePacket implements zombie.network.packets.INetworkPacket {
        private final long token;

        FencePacket(long token) { this.token = token; }

        @Override public void write(zombie.core.network.ByteBufferWriter writer) {
            writer.putLong(token);
            writer.putLong(0L);
        }

        @Override public void parse(zombie.core.network.ByteBufferReader reader, zombie.network.IConnection connection) {
            reader.getLong();
            reader.getLong();
        }
    }

    private static final class ClimbPacket extends StatePacket {
        ClimbPacket(IsoPlayer player, State.Stage stage) {
            set(player.getX(), player.getY(), player.getZ());
            characterId.set(player);
            state.set(ClimbThroughWindowState.instance());
            this.stage = stage;
            isSubState = false;
            params.rawset("scratched", Boolean.FALSE);
            // Do not clear live window parameters or forward unrelated action-context events.
            events.clear();
        }
    }
}
