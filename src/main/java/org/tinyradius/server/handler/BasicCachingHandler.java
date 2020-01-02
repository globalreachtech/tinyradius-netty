package org.tinyradius.server.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.util.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinyradius.server.RequestCtx;
import org.tinyradius.server.ResponseCtx;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class BasicCachingHandler<IN extends RequestCtx, OUT extends ResponseCtx> extends MessageToMessageCodec<IN, OUT> {

    private static final Logger logger = LoggerFactory.getLogger(BasicCachingHandler.class);

    private final Timer timer;
    private final int ttlMs;

    private final Map<Packet, OUT> requests = new ConcurrentHashMap<>();

    /**
     * @param timer         for cache eviction
     * @param ttlMs         time for items to stay cached after being returned, in milliseconds
     * @param inboundClass  explicit class due to type erasure
     * @param outboundClass explicit class due to type erasure
     */
    public BasicCachingHandler(Timer timer, int ttlMs, Class<IN> inboundClass, Class<OUT> outboundClass) {
        super(inboundClass, outboundClass);
        this.timer = timer;
        this.ttlMs = ttlMs;
    }

    /**
     * @param ctx            ChannelHandlerContext
     * @param requestContext Inbound request context
     */
    protected void onMiss(ChannelHandlerContext ctx, IN requestContext) {
    }

    /**
     * @param ctx             ChannelHandlerContext
     * @param responseContext Outbound response context
     */
    protected void onHit(ChannelHandlerContext ctx, OUT responseContext) {
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, IN msg, List<Object> out) {
        final Packet packet = Packet.from(msg);
        final OUT responseContext = requests.get(packet);

        if (responseContext != null) {
            logger.debug("Cache hit, resending response, id: {}, remote address: {}", packet.identifier, packet.remoteAddress);
            onHit(ctx, responseContext);
            ctx.writeAndFlush(responseContext);
        } else {
            logger.debug("Cache miss, proxying request, id: {}, remote address: {}", packet.identifier, packet.remoteAddress);
            onMiss(ctx, msg);
            out.add(msg);
        }
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, OUT msg, List<Object> out) {
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
