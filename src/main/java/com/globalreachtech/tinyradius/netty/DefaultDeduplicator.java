package com.globalreachtech.tinyradius.netty;

import com.globalreachtech.tinyradius.packet.RadiusPacket;
import io.netty.util.Timer;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class DefaultDeduplicator implements RadiusServer.PacketDeduplicator {

    private static long TTL = 30000;
    private final Timer timer;

    private final Set<Packet> packets = ConcurrentHashMap.newKeySet();

    DefaultDeduplicator(Timer timer) {
        this.timer = timer;
    }

    @Override
    public boolean isPacketDuplicate(RadiusPacket packet, InetSocketAddress address) {
        Packet p = new Packet(packet.getPacketIdentifier(), address, packet.getAuthenticator());
        final boolean packetAdded = packets.add(p);
        timer.newTimeout(t -> packets.remove(p), TTL, MILLISECONDS);
        return !packetAdded;
    }

    private class Packet {

        private final int packetIdentifier;
        private final InetSocketAddress address;
        private final byte[] authenticator;

        Packet(int packetIdentifier, InetSocketAddress address, byte[] authenticator) {
            this.packetIdentifier = packetIdentifier;
            this.address = address;
            this.authenticator = authenticator;
        }

        /**
         * If authenticator is null (should not happen), ignores and only compares other properties.
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Packet packet = (Packet) o;
            return packetIdentifier == packet.packetIdentifier &&
                    address.equals(packet.address) &&
                    (authenticator == null || packet.authenticator == null || Arrays.equals(packet.authenticator, authenticator));
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(packetIdentifier, address);
            result = 31 * result + Arrays.hashCode(authenticator);
            return result;
        }
    }
}
