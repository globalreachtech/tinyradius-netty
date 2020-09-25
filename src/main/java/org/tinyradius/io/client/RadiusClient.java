package org.tinyradius.io.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tinyradius.core.packet.request.RadiusRequest;
import org.tinyradius.core.packet.response.RadiusResponse;
import org.tinyradius.io.RadiusEndpoint;
import org.tinyradius.io.client.timeout.TimeoutHandler;

import java.io.Closeable;
import java.io.IOException;
import java.net.SocketAddress;
import java.util.List;

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

    /**
     * Sends packet to specified endpoints in turn until an endpoint succeeds or all fail.
     *
     * @param packet    packet to send
     * @param endpoints endpoints to send packet to
     * @return deferred response containing response packet or exception
     */
    public Future<RadiusResponse> communicate(RadiusRequest packet, List<RadiusEndpoint> endpoints) {
        if (endpoints.isEmpty())
            return eventLoopGroup.next().newFailedFuture(new IOException("Client send failed - no valid endpoints"));

        final Promise<RadiusResponse> promise = eventLoopGroup.next().newPromise();
        communicateRecursive(packet, endpoints, 0, promise, null);

        return promise;
    }

    private void communicateRecursive(RadiusRequest packet, List<RadiusEndpoint> endpoints, int endpointIndex,
                                      Promise<RadiusResponse> promise, Throwable lastException) {
        if (endpointIndex >= endpoints.size()) {
            promise.tryFailure(new IOException("Client send failed - all endpoints failed", lastException));
            return;
        }

        communicate(packet, endpoints.get(endpointIndex)).addListener((Future<RadiusResponse> f) -> {
            if (f.isSuccess())
                promise.trySuccess(f.getNow());
            else
                communicateRecursive(packet, endpoints, endpointIndex + 1, promise, f.cause());
        });
    }

    /**
     * Sends packet to specified endpoint.
     *
     * @param packet   packet to send
     * @param endpoint endpoint to send packet to
     * @return deferred response containing response packet or exception
     */
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