package org.tinyradius.io.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import lombok.extern.log4j.Log4j2;
import org.tinyradius.core.packet.request.RadiusRequest;
import org.tinyradius.core.packet.response.RadiusResponse;
import org.tinyradius.io.RadiusEndpoint;
import org.tinyradius.io.RadiusLifecycle;
import org.tinyradius.io.client.timeout.TimeoutHandler;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.List;

import static org.tinyradius.io.client.ClientEventListener.EventType.POST_RECEIVE;
import static org.tinyradius.io.client.ClientEventListener.EventType.PRE_SEND;
import static org.tinyradius.io.client.ClientEventListener.NO_OP_LISTENER;

/**
 * A simple Radius client which binds to a specific socket, and can
 * then be used to send any number of packets to any endpoints through
 * that socket.
 * <p>
 * External functions to be invoke for each send try, timeout and response
 * received may be optionally specified.
 */
@Log4j2
public class RadiusClient implements RadiusLifecycle {

    private final TimeoutHandler defaultTimeoutHandler;
    private final ClientEventListener defaultEventListener;
    private final EventLoopGroup eventLoopGroup;
    private final ChannelFuture channelFuture;

    /**
     * @param bootstrap      bootstrap with channel class and eventLoopGroup set up
     * @param listenAddress  local address to bind to
     * @param timeoutHandler retry strategy for scheduling retries and timeouts
     * @param handler        ChannelHandler to handle outbound requests
     */
    public RadiusClient(Bootstrap bootstrap, SocketAddress listenAddress, TimeoutHandler timeoutHandler, ChannelHandler handler) {
        this(bootstrap, listenAddress, timeoutHandler, handler, NO_OP_LISTENER);
    }

    /**
     * @param bootstrap      bootstrap with channel class and eventLoopGroup set up
     * @param listenAddress  local address to bind to
     * @param timeoutHandler retry strategy for scheduling retries and timeouts
     * @param handler        ChannelHandler to handle outbound requests
     * @param eventListener  instrumentation hooks for client events
     */
    public RadiusClient(Bootstrap bootstrap, SocketAddress listenAddress, TimeoutHandler timeoutHandler, ChannelHandler handler, ClientEventListener eventListener) {
        this.defaultTimeoutHandler = timeoutHandler;
        this.defaultEventListener = eventListener;
        eventLoopGroup = bootstrap.config().group();
        channelFuture = bootstrap.clone().handler(handler).bind(listenAddress);
    }

    /**
     * Sends packet to specified endpoints in turn until an endpoint succeeds or all fail.
     *
     * @param packet         packet to send
     * @param endpoints      endpoints to send packet to
     * @param timeoutHandler the timeoutHandler to use for this request
     * @param listener       instrumentation hooks
     * @return deferred response containing response packet or exception
     */
    public Future<RadiusResponse> communicate(RadiusRequest packet, List<RadiusEndpoint> endpoints, TimeoutHandler timeoutHandler, ClientEventListener listener) {
        if (endpoints.isEmpty())
            return eventLoopGroup.next().newFailedFuture(new IOException("Client send failed - no valid endpoints"));

        final Promise<RadiusResponse> promise = eventLoopGroup.next().newPromise();
        communicateNextEndpoint(packet, endpoints, 0, promise, null, timeoutHandler, listener);

        return promise;
    }

    /**
     * Sends packet to specified endpoints in turn until an endpoint succeeds or all fail.
     *
     * @param packet    packet to send
     * @param endpoints endpoints to send packet to
     * @return deferred response containing response packet or exception
     */
    public Future<RadiusResponse> communicate(RadiusRequest packet, List<RadiusEndpoint> endpoints) {
        return communicate(packet, endpoints, defaultTimeoutHandler, defaultEventListener);
    }

    private void communicateNextEndpoint(RadiusRequest packet, List<RadiusEndpoint> endpoints, int endpointIndex,
                                         Promise<RadiusResponse> promise, Throwable lastException, TimeoutHandler timeoutHandler,
                                         ClientEventListener listener) {

        if (endpointIndex >= endpoints.size()) {
            promise.tryFailure(new IOException("Client send failed - all endpoints failed", lastException));
            return;
        }

        communicate(packet, endpoints.get(endpointIndex), timeoutHandler, listener).addListener((Future<RadiusResponse> f) -> {
            if (f.isSuccess())
                promise.trySuccess(f.getNow());
            else
                communicateNextEndpoint(packet, endpoints, endpointIndex + 1, promise, f.cause(), timeoutHandler, listener);
        });
    }

    /**
     * Sends packet to specified endpoint with the default timeoutHandler and default event listener.
     *
     * @param packet   packet to send
     * @param endpoint endpoint to send packet to
     * @return deferred response containing response packet or exception
     */
    public Future<RadiusResponse> communicate(RadiusRequest packet, RadiusEndpoint endpoint) {
        return communicate(packet, endpoint, defaultTimeoutHandler, defaultEventListener);
    }

    /**
     * Sends packet to specified endpoint.
     *
     * @param packet         packet to send
     * @param endpoint       endpoint to send packet to
     * @param timeoutHandler TimeoutHandler to use for this request.
     * @param listener       instrumentation event listeners
     * @return deferred response containing response packet or exception
     */
    public Future<RadiusResponse> communicate(RadiusRequest packet, RadiusEndpoint endpoint, TimeoutHandler timeoutHandler, ClientEventListener listener) {
        var promise = eventLoopGroup.next().<RadiusResponse>newPromise();

        channelFuture.addListener(s -> {
            if (s.isSuccess()) {
                var ctx = new PendingRequestCtx(packet, endpoint, promise);
                send(ctx, 1, timeoutHandler, listener);
                promise.addListener(f -> {
                    if (f.isSuccess()) {
                        log.debug("Response received, packet: {}", f.getNow());
                        listener.onEvent(POST_RECEIVE, ctx);
                    } else {
                        log.warn(f.cause().getMessage());
                    }
                });
            } else
                promise.tryFailure(s.cause());
        });

        return promise;
    }

    private void send(PendingRequestCtx ctx, int attempt, TimeoutHandler timeoutHandler, ClientEventListener listener) {
        // More appropriate to use debug than info
        log.debug("Attempt {}, sending packet to {}", attempt, ctx.getEndpoint().getAddress());

        listener.onEvent(PRE_SEND, ctx);
        channelFuture.channel().writeAndFlush(ctx);

        timeoutHandler.scheduleTimeout(() -> send(ctx, attempt + 1, timeoutHandler, listener), attempt, ctx, listener);
    }

    @Override
    public ChannelFuture isReady() {
        return channelFuture;
    }

    @Override
    public ChannelFuture closeAsync() {
        return channelFuture.channel().close();
    }
}