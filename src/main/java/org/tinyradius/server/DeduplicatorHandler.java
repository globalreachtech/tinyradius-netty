package org.tinyradius.server;

import io.netty.channel.Channel;
import io.netty.util.Timer;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinyradius.packet.RadiusPacket;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * Handler that wraps around another handler and deduplicates requests, returning
 * null if duplicate. This considers packets duplicate
 * if packetIdentifier and remote address matches.
 */
public class DeduplicatorHandler<T extends RadiusPacket> implements RequestHandler<T> {

    private static final Logger logger = LoggerFactory.getLogger(DeduplicatorHandler.class);

    private final RequestHandler<T> requestHandler;
    private final Timer timer;
    private final long ttlMs;

    private final Set<Packet> packets = ConcurrentHashMap.newKeySet();

    /**
     * @param requestHandler underlying handler to process packet if not duplicate
     * @param timer          used to set timeouts that clean up packets after predefined TTL
     * @param ttlMs          time in ms to keep packets in cache and ignore duplicates
     */
    public DeduplicatorHandler(RequestHandler<T> requestHandler, Timer timer, long ttlMs) {
        this.requestHandler = requestHandler;
        this.timer = timer;
        this.ttlMs = ttlMs;
    }

    /**
     * @param channel       socket which received packet
     * @param packet        the packet
     * @param remoteAddress remote address the packet was sent by
     * @param sharedSecret  shared secret associated with remoteAddress
     * @return null if packet is considered duplicate, otherwise delegates to underlying handler.
     */
    @Override
    public Promise<RadiusPacket> handlePacket(Channel channel, T packet, InetSocketAddress remoteAddress, String sharedSecret) {
        if (!isPacketDuplicate(packet, remoteAddress))
            return requestHandler.handlePacket(channel, packet, remoteAddress, sharedSecret);

        logger.info("ignore duplicate packet, id: {}, remote address: {}", packet.getPacketIdentifier(), remoteAddress);
        Promise<RadiusPacket> promise = channel.eventLoop().newPromise();
        promise.trySuccess(null);
        return promise;
    }

    /**
     * Checks whether the passed packet is a duplicate.
     * A packet is duplicate if another packet with the same identifier
     * has been sent from the same host.
     * <p>
     * If duplicate is received, TTL of the packet will NOT rebased to
     * the most recent hit.
     *
     * @param packet  packet in question
     * @param address client address
     * @return true if it is duplicate
     */
    private boolean isPacketDuplicate(RadiusPacket packet, InetSocketAddress address) {
        Packet p = new Packet(packet.getPacketIdentifier(), address, packet.getAuthenticator());

        if (packets.contains(p))
            return true;

        packets.add(p);
        timer.newTimeout(t -> packets.remove(p), ttlMs, MILLISECONDS);
        return false;
    }

    private static class Packet {

        private final int packetIdentifier;
        private final InetSocketAddress address;
        private final byte[] authenticator;

        Packet(int packetIdentifier, InetSocketAddress address, byte[] authenticator) {
            this.packetIdentifier = packetIdentifier;
            this.address = address;
            this.authenticator = authenticator;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Packet packet = (Packet) o;
            return packetIdentifier == packet.packetIdentifier &&
                    Objects.equals(address, packet.address) &&
                    Arrays.equals(authenticator, packet.authenticator);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(packetIdentifier, address);
            result = 31 * result + Arrays.hashCode(authenticator);
            return result;
        }
    }
}
