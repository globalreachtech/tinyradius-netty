package org.tinyradius.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tinyradius.client.timeout.TimeoutHandler;
import org.tinyradius.packet.request.RadiusRequest;
import org.tinyradius.packet.response.RadiusResponse;
import org.tinyradius.util.RadiusEndpoint;

import java.io.Closeable;
import java.net.SocketAddress;

/**
 * A simple Radius client which binds to a specific socket, and can
 * then be used to send any number of packets to any endpoints through
 * that socket.
 */
public class RadiusClient implements Closeable {

    private static final Logger logger = LogManager.getLogger();

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

    public Future<RadiusResponse> communicate(RadiusRequest packet, RadiusEndpoint endpoint) {
        final Promise<RadiusResponse> promise = eventLoopGroup.next().<RadiusResponse>newPromise().addListener(f -> {
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