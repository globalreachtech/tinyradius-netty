package org.tinyradius.io.server.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.util.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
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
@Log4j2
@RequiredArgsConstructor
public class BasicCachingHandler extends MessageToMessageCodec<RequestCtx, ResponseCtx> {

    /**
     * for cache eviction
     */
    private final Timer timer;

    /**
     * time for items to stay cached after being returned, in milliseconds
     */
    private final int ttlMs;

    private final Map<Packet, ResponseCtx> requests = new ConcurrentHashMap<>();

    /**
     * @param ctx        ChannelHandlerContext
     * @param requestCtx inbound request context
     * @param out        list to which decoded messages should be added
     */
    protected void onMiss(ChannelHandlerContext ctx, RequestCtx requestCtx, List<Object> out) {
        out.add(requestCtx);
    }

    /**
     * @param ctx         ChannelHandlerContext
     * @param requestCtx  inbound request context
     * @param responseCtx outbound response context
     * @param out         list to which decoded messages should be added
     */
    protected void onHit(ChannelHandlerContext ctx, RequestCtx requestCtx, ResponseCtx responseCtx, List<Object> out) {
        ctx.writeAndFlush(responseCtx);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, RequestCtx requestCtx, List<Object> out) {
        final Packet packet = Packet.from(requestCtx);
        final ResponseCtx responseContext = requests.get(packet);

        if (responseContext != null) {
            log.debug("Cache hit, resending response, id: {}, remote address: {}", packet.id, packet.remoteAddress);
            onHit(ctx, requestCtx, responseContext, out);
        } else {
            log.debug("Cache miss, handling request, id: {}, remote address: {}", packet.id, packet.remoteAddress);
            onMiss(ctx, requestCtx, out);
        }
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ResponseCtx msg, List<Object> out) {
        final Packet packet = Packet.from(msg);
        requests.put(packet, msg);
        timer.newTimeout(t -> requests.remove(packet), ttlMs, MILLISECONDS);
        out.add(msg);
    }

    private record Packet(int id, InetSocketAddress remoteAddress, byte[] authenticator) {

        private static Packet from(RequestCtx ctx) {
            return new Packet(ctx.getRequest().getId(), ctx.getEndpoint().address(), ctx.getRequest().getAuthenticator());
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Packet packet)) return false;
            return id == packet.id && Objects.deepEquals(authenticator, packet.authenticator) && Objects.equals(remoteAddress, packet.remoteAddress);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, remoteAddress, Arrays.hashCode(authenticator));
        }

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
