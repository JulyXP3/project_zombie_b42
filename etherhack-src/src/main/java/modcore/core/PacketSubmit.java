package modcore.core;

import modcore.utils.Logger;
import zombie.core.network.ByteBufferWriter;
import zombie.network.IConnection;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;

/** Explicit local submission result; no server acknowledgement is implied. */
public final class PacketSubmit {
    private PacketSubmit() {
    }

    public static boolean send(IConnection connection, PacketTypes.PacketType type, INetworkPacket packet) {
        if (type == null) return false;
        return send(connection, type, packet, type.packetPriority, type.packetReliability, type.orderingChannel);
    }

    public static boolean send(IConnection connection, PacketTypes.PacketType type, INetworkPacket packet,
                               int priority, int reliability, byte ordering) {
        if (connection == null || type == null || packet == null) return false;
        boolean started = false;
        try {
            ByteBufferWriter writer = connection.startPacket();
            started = true;
            type.doPacket(writer);
            packet.write(writer);
            if (connection.isLimitExceeded(type)) {
                started = false;
                connection.cancelPacket();
                return false;
            }
            connection.endPacket(priority, reliability, ordering);
            started = false;
            return true;
        } catch (Throwable t) {
            if (started) {
                try { connection.cancelPacket(); } catch (Throwable ignored) { }
            }
            Logger.printLog("Packet " + type.name() + " submission failed: " + t);
            return false;
        }
    }
}
