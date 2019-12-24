package org.tinyradius.server.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.util.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinyradius.server.RequestCtx;
import org.tinyradius.server.ServerResponseCtx;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class CachingHandler<INBOUND extends RequestCtx, OUTBOUND extends ServerResponseCtx> extends MessageToMessageCodec<INBOUND, OUTBOUND> {

    private static final Logger logger = LoggerFactory.getLogger(CachingHandler.class);

    private final Timer timer;
    private final int ttlMs;

    private final Map<Packet, OUTBOUND> requests = new ConcurrentHashMap<>();

    public CachingHandler(Timer timer, int ttlMs) {
        this.timer = timer;
        this.ttlMs = ttlMs;
    }

    /**
     *
     * @param ctx            ChannelHandlerContext
     * @param requestContext Inbound request context
     * @return request context to forward
     */
    protected RequestCtx onMiss(ChannelHandlerContext ctx, INBOUND requestContext) {
        return requestContext;
    }

    /**
     * @param ctx             ChannelHandlerContext
     * @param responseContext Outbound response context
     * @return response context to return
     */
    protected RequestCtx onHit(ChannelHandlerContext ctx, OUTBOUND responseContext) {
        return responseContext;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, INBOUND msg, List<Object> out) {
        final Packet packet = Packet.from(msg);
        final OUTBOUND responseContext = requests.get(packet);

        if (responseContext != null) {
            logger.debug("Cache hit, resending response, id: {}, remote address: {}", packet.identifier, packet.remoteAddress);
            ctx.writeAndFlush(onHit(ctx, responseContext));
        } else {
            logger.debug("Cache miss, proxying request, id: {}, remote address: {}", packet.identifier, packet.remoteAddress);
            out.add(onMiss(ctx, msg));
        }
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, OUTBOUND msg, List<Object> out) {
        final Packet packet = Packet.from(msg);
        requests.put(packet, msg);
        timer.newTimeout(t -> requests.remove(packet), ttlMs, MILLISECONDS);
        out.add(msg);
    }

    private static class Packet {

        private final int identifier;
        private final InetSocketAddress remoteAddress;
        private final byte[] authenticator;

        private static Packet from(RequestCtx ctx) {
            return new Packet(ctx.getRequest().getIdentifier(), ctx.getEndpoint().getAddress(), ctx.getRequest().getAuthenticator());
        }

        private Packet(int identifier, InetSocketAddress remoteAddress, byte[] authenticator) {
            this.identifier = identifier;
            this.remoteAddress = remoteAddress;
            this.authenticator = authenticator;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Packet packet = (Packet) o;
            return identifier == packet.identifier &&
                    Objects.equals(remoteAddress, packet.remoteAddress) &&
                    Arrays.equals(authenticator, packet.authenticator);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(identifier, remoteAddress);
            result = 31 * result + Arrays.hashCode(authenticator);
            return result;
        }
    }
}
