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

/**
 * A simple Radius client which binds to a specific socket, and can
 * then be used to send any number of packets to any endpoints through
 * that socket.
 * 
 * External functions to be invoke for each send try, timeout and response
 * received may be optionally specified.
 */
@Log4j2
public class RadiusClient implements RadiusLifecycle {

    // Used as default, if maxAttempts and timeoutMillis are not specified
    private final TimeoutHandler defaultTimeoutHandler;
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
        this.defaultTimeoutHandler = timeoutHandler;
        channelFuture = bootstrap.clone().handler(handler).bind(listenAddress);
    }

    /**
     * Sends packet to specified endpoints in turn until an endpoint succeeds or all fail.
     * @param packet    packet to send
     * @param endpoints endpoints to send packet to
     * @param timeoutHandler the timeoutHandler to use for this request
     * @param hooks instrumentation hooks
     * @return
     */
    public Future<RadiusResponse> communicate(RadiusRequest packet, List<RadiusEndpoint> endpoints, TimeoutHandler timeoutHandler,
            RadiusClientHooks hooks) {
        if (endpoints.isEmpty())
            return eventLoopGroup.next().newFailedFuture(new IOException("Client send failed - no valid endpoints"));

        final Promise<RadiusResponse> promise = eventLoopGroup.next().newPromise();
        communicateRecursive(packet, endpoints, 0, promise, null, timeoutHandler, hooks);

        return promise;
    }

    /**
     * Sends packet to specified endpoints in turn until an endpoint succeeds or all fail.
     * @param packet    packet to send
     * @param endpoints endpoints to send packet to
     * @return
     */
    public Future<RadiusResponse> communicate(RadiusRequest packet, List<RadiusEndpoint> endpoints){
        return communicate(packet, endpoints, defaultTimeoutHandler, null);
    }

    private void communicateRecursive(RadiusRequest packet, List<RadiusEndpoint> endpoints, int endpointIndex,
                                      Promise<RadiusResponse> promise, Throwable lastException, TimeoutHandler timeoutHandler,
                                      RadiusClientHooks hooks) {

        if (endpointIndex >= endpoints.size()) {
            promise.tryFailure(new IOException("Client send failed - all endpoints failed", lastException));
            return;
        }

        communicate(packet, endpoints.get(endpointIndex), timeoutHandler, hooks).addListener((Future<RadiusResponse> f) -> {
            if (f.isSuccess())
                promise.trySuccess(f.getNow());
            else
                communicateRecursive(packet, endpoints, endpointIndex + 1, promise, f.cause(), timeoutHandler, hooks);
        });
    }

    /**
     * Sends packet to specified endpoint.
     *
     * @param packet   packet to send
     * @param endpoint endpoint to send packet to
     * @param timeoutHandler TimeoutHandler to use for this request.
     * @param hooks instrumentation hooks
     * @return deferred response containing response packet or exception
     */
    public Future<RadiusResponse> communicate(RadiusRequest packet, RadiusEndpoint endpoint, TimeoutHandler timeoutHandler, 
                RadiusClientHooks hooks) {

        final Promise<RadiusResponse> promise = eventLoopGroup.next().<RadiusResponse>newPromise().addListener(f -> {
            if (f.isSuccess()){
                log.debug("Response received, packet: {}", f.getNow());
                // Report response received to hook
                if(hooks != null) hooks.postReceiveHook(((RadiusResponse)f.getNow()).getType(), endpoint.getAddress());
            }
            else{
                log.warn(f.cause().getMessage());
                // Report final timeout to hook
                if(hooks != null) hooks.timeoutHook(packet.getType(), endpoint.getAddress());
            }
        });

        channelFuture.addListener(s -> {
            if (s.isSuccess())
                send(new PendingRequestCtx(packet, endpoint, promise), 1, timeoutHandler, hooks);
            else
                promise.tryFailure(s.cause());
        });

        return promise;
    }

    /**
     * Sends packet to specified endpoint with the default timeoutHandler and no hooks.
     * @param packet   packet to send
     * @param endpoint endpoint to send packet to
     * @return
     */
    public Future<RadiusResponse> communicate(RadiusRequest packet, RadiusEndpoint endpoint){
        return communicate(packet, endpoint, defaultTimeoutHandler, null);
    }

    private void send(PendingRequestCtx ctx, int attempt, TimeoutHandler timeoutHandler, RadiusClientHooks hooks){
        // More appropriate to use debug than info
        log.debug("Attempt {}, sending packet to {}", attempt, ctx.getEndpoint().getAddress());
        
        // Report send packet to hook
        if(hooks != null) hooks.preSendHook(ctx.getRequest().getType(), ctx.getEndpoint().getAddress());
        channelFuture.channel().writeAndFlush(ctx);

        timeoutHandler.onTimeout(() -> {
                // Report timeout with retry (not final) to hook
                if(hooks != null) hooks.timeoutHook(ctx.getRequest().getType(), ctx.getEndpoint().getAddress());
                send(ctx, attempt + 1, timeoutHandler, hooks);
            }, attempt, ctx.getResponse());
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