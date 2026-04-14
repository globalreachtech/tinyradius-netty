package org.tinyradius.io.server.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.util.Timer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.NonNull;
import org.tinyradius.io.server.RequestCtx;
import org.tinyradius.io.server.ResponseCtx;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * Simple caching handler backed by ConcurrentHashMap, invalidates using {@link Timer}.
 */
public class BasicCachingHandler extends MessageToMessageCodec<RequestCtx, ResponseCtx> {

    private static final Logger log = LogManager.getLogger(BasicCachingHandler.class);
    private final Timer timer;
    private final int ttlMs;

    private final Map<Packet, ResponseCtx> requests = new ConcurrentHashMap<>();

    /**
     * Constructs a {@code BasicCachingHandler} with the specified {@link Timer} and TTL.
     *
     * @param timer the timer used for cache eviction
     * @param ttlMs the time to live for cached items in milliseconds
     */
    public BasicCachingHandler(Timer timer, int ttlMs) {
        this.timer = timer;
        this.ttlMs = ttlMs;
    }

    /**
     * Called when a request is not found in the cache.
     *
     * @param ctx        ChannelHandlerContext
     * @param requestCtx inbound request context
     * @param out        list to which decoded messages should be added
     */
    protected void onMiss(ChannelHandlerContext ctx, RequestCtx requestCtx, List<Object> out) {
        out.add(requestCtx);
    }

    /**
     * Called when a request is found in the cache.
     *
     * @param ctx         ChannelHandlerContext
     * @param requestCtx  inbound request context
     * @param responseCtx outbound response context
     * @param out         list to which decoded messages should be added
     */
    protected void onHit(ChannelHandlerContext ctx, RequestCtx requestCtx, ResponseCtx responseCtx, List<Object> out) {
        ctx.writeAndFlush(responseCtx);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void decode(ChannelHandlerContext ctx, RequestCtx requestCtx, List<Object> out) {
        var packet = Packet.from(requestCtx);
        var responseContext = requests.get(packet);

        if (responseContext != null) {
            log.debug("Cache hit, resending response, id: {}, remote address: {}", packet.id, packet.remoteAddress);
            onHit(ctx, requestCtx, responseContext, out);
        } else {
            log.debug("Cache miss, handling request, id: {}, remote address: {}", packet.id, packet.remoteAddress);
            onMiss(ctx, requestCtx, out);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void encode(ChannelHandlerContext ctx, ResponseCtx msg, List<Object> out) {
        var packet = Packet.from(msg);
        requests.put(packet, msg);
        timer.newTimeout(t -> requests.remove(packet), ttlMs, MILLISECONDS);
        out.add(msg);
    }

    /**
     * Represents a RADIUS packet for caching purposes.
     *
     * @param id            RADIUS packet identifier
     * @param remoteAddress remote address from which the packet was received
     * @param authenticator packet authenticator to differentiate between retransmissions and new requests
     */
    private record Packet(int id, InetSocketAddress remoteAddress, byte[] authenticator) {

        /**
         * Creates a Packet record from a RequestCtx.
         *
         * @param ctx the request context
         * @return a new Packet record
         */
        private static Packet from(RequestCtx ctx) {
            return new Packet(ctx.getRequest().getId(), ctx.getEndpoint().address(), ctx.getRequest().getAuthenticator());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Packet packet)) return false;
            return id == packet.id && Objects.deepEquals(authenticator, packet.authenticator) && Objects.equals(remoteAddress, packet.remoteAddress);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return Objects.hash(id, remoteAddress, Arrays.hashCode(authenticator));
        }

        /**
         * {@inheritDoc}
         */
        @NonNull
        @Override
        public String toString() {
            return "Packet{" +
                    "id=" + id +
                    ", remoteAddress=" + remoteAddress +
                    ", authenticator=" + Arrays.toString(authenticator) +
                    '}';
        }
    }
}
