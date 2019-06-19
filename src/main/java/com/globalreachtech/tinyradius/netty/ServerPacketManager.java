package com.globalreachtech.tinyradius.netty;

import com.globalreachtech.tinyradius.RadiusServer;
import com.globalreachtech.tinyradius.packet.RadiusPacket;
import io.netty.util.Timer;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class ServerPacketManager implements RadiusServer.PacketManager {

    private final long ttlMs;
    private final Timer timer;

    private final Set<Packet> packets = ConcurrentHashMap.newKeySet();

    /**
     * @param timer used to set timeouts that clean up packets after predefined TTL
     * @param ttlMs time in ms to keep packets in cache and ignore duplicates
     */
    public ServerPacketManager(Timer timer, long ttlMs) {
        this.timer = timer;
        this.ttlMs = ttlMs;
    }

    /**
     * Checks whether the passed packet is a duplicate.
     * A packet is duplicate if another packet with the same identifier
     * has been sent from the same host in the last time.
     *
     * If duplicate is received, TTL of the packet will not rebased to
     * the most recent hit.
     *
     * @param packet  packet in question
     * @param address client address
     * @return true if it is duplicate
     */
    @Override
    public boolean isPacketDuplicate(RadiusPacket packet, InetSocketAddress address) {
        Packet p = new Packet(packet.getPacketIdentifier(), address, packet.getAuthenticator());

        if (packets.contains(p))
            return true;

        packets.add(p);
        timer.newTimeout(t -> packets.remove(p), ttlMs, MILLISECONDS);
        return false;
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
