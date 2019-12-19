package org.tinyradius.server.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.Timer;
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
 * Handler that ignores duplicate requests. This considers packets duplicate
 * if packetIdentifier and remote address matches.
 */
public class DeduplicatingHandler extends SimpleChannelInboundHandler<RequestContext> {

    private static final Logger logger = LoggerFactory.getLogger(DeduplicatingHandler.class);

    private final Timer timer;
    private final long ttlMs;

    private final Set<Packet> packets = ConcurrentHashMap.newKeySet();

    /**
     * @param timer used to set timeouts that clean up packets after predefined TTL
     * @param ttlMs time in ms to keep packets in cache and ignore duplicates
     */
    public DeduplicatingHandler(Timer timer, long ttlMs) {
        this.timer = timer;
        this.ttlMs = ttlMs;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RequestContext msg) {
        final RadiusPacket request = msg.getRequest();
        final InetSocketAddress remoteAddress = msg.getRemoteAddress();

        if (!isPacketDuplicate(request, remoteAddress))
            ctx.fireChannelRead(msg);

        logger.info("Ignoring duplicate packet, id: {}, remote address: {}", request.getIdentifier(), remoteAddress);
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
        Packet p = new Packet(packet.getIdentifier(), address, packet.getAuthenticator());

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
