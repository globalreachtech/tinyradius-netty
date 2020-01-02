package org.tinyradius.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinyradius.client.timeout.TimeoutHandler;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.util.RadiusEndpoint;

import java.io.Closeable;
import java.net.SocketAddress;

/**
 * This object represents a simple Radius client which communicates with
 * a specified Radius server. You can use a single instance of this object
 * to authenticate or account different users with the same Radius server
 * as long as you authenticate/account one user after the other.
 */
public class RadiusClient implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(RadiusClient.class);

    private final TimeoutHandler timeoutHandler;
    private final EventLoopGroup eventLoopGroup;
    private final ChannelFuture channelFuture;

    /**
     * @param bootstrap      bootstrap with channel class and eventLoopGroup set up
     * @param listenAddress  local address to bind to
     * @param timeoutHandler retry strategy for scheduling retries and timeouts
     * @param handler        ChannelHandler to handle outbound requests
     */
    public RadiusClient(Bootstrap bootstrap, SocketAddress listenAddress, TimeoutHandler timeoutHandler, ChannelHandler handler) {
        this.eventLoopGroup = bootstrap.config().group();
        this.timeoutHandler = timeoutHandler;
        channelFuture = bootstrap.clone().handler(handler).bind(listenAddress);
    }

    public Future<RadiusPacket> communicate(RadiusPacket packet, RadiusEndpoint endpoint) {
        final Promise<RadiusPacket> promise = eventLoopGroup.next().<RadiusPacket>newPromise().addListener(f -> {
            if (f.isSuccess())
                logger.info("Response received, packet: {}", f.getNow());
            else
                logger.warn(f.cause().getMessage());
        });

        channelFuture.addListener(s -> {
            if (s.isSuccess())
                send(new PendingRequestCtx(packet, endpoint, promise), 1);
            else
                promise.tryFailure(s.cause());
        });

        return promise;
    }

    private void send(PendingRequestCtx ctx, int attempt) {
        logger.info("Attempt {}, sending packet to {}", attempt, ctx.getEndpoint().getAddress());
        channelFuture.channel().writeAndFlush(ctx);
        timeoutHandler.onTimeout(() -> send(ctx, attempt + 1), attempt, ctx.getResponse());
    }

    @Override
    public void close() {
        channelFuture.channel().close();
    }
}