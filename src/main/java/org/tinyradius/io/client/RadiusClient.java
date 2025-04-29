package org.tinyradius.io.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.EventLoopGroup;
import io.netty.util.HashedWheelTimer;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import lombok.extern.log4j.Log4j2;
import org.tinyradius.core.packet.request.RadiusRequest;
import org.tinyradius.core.packet.response.RadiusResponse;
import org.tinyradius.io.RadiusEndpoint;
import org.tinyradius.io.RadiusLifecycle;
import org.tinyradius.io.client.timeout.FixedTimeoutHandler;
import org.tinyradius.io.client.timeout.TimeoutHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.function.BiConsumer;

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

    // Will be used to create FixedTimeoutHandlers when not using the default
    private final HashedWheelTimer timer;

    // Used as default, if maxAttempts and timeoutMillis are not specified
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
        this.timer = new HashedWheelTimer();
        channelFuture = bootstrap.clone().handler(handler).bind(listenAddress);
    }

    private Future<RadiusResponse> communicate(RadiusRequest packet, List<RadiusEndpoint> endpoints, TimeoutHandler perRequestTimeoutHandler,
            BiConsumer<Byte, InetSocketAddress> preSendHook, BiConsumer<Byte, InetSocketAddress> timeoutHook, 
            BiConsumer<Byte, InetSocketAddress> postReceiveHook) {
        if (endpoints.isEmpty())
            return eventLoopGroup.next().newFailedFuture(new IOException("Client send failed - no valid endpoints"));

        final Promise<RadiusResponse> promise = eventLoopGroup.next().newPromise();
        communicateRecursive(packet, endpoints, 0, promise, null, perRequestTimeoutHandler, preSendHook, timeoutHook, postReceiveHook);

        return promise;
    }

    /**
     * Sends packet to specified endpoints in turn until an endpoint succeeds or all fail.
     * @param packet    packet to send
     * @param endpoints endpoints to send packet to
     * @param maxAttempts max attemtps per endpoint
     * @param timeoutMillis timeout in milliseconds per request
     * @param preSendHook function to invoke before sending the radius request. 
     * Parameters are the type of request and InetSocket address of the remote
     * @param timeoutHook function to invoke when a timeout expires. Parameters
     * are the type of the request and the InetSocketAddress of the remote.
     * @param postReceiveHook function to invoke when a responsne is received.
     * Parameters are the type of the response and the InetSocketAddress of the remote.
     * @return
     */
    public Future<RadiusResponse> communicate(RadiusRequest packet, List<RadiusEndpoint> endpoints, int maxAttempts, int timeoutMillis,
                                              BiConsumer<Byte, InetSocketAddress> preSendHook, BiConsumer<Byte, InetSocketAddress> timeoutHook, 
                                              BiConsumer<Byte, InetSocketAddress> postReceiveHook){
        return communicate(packet, endpoints, new FixedTimeoutHandler(timer, maxAttempts, timeoutMillis), preSendHook, timeoutHook, postReceiveHook);
    }

    /**
     * Sends packet to specified endpoints in turn until an endpoint succeeds or all fail.
     * @param packet    packet to send
     * @param endpoints endpoints to send packet to
     * @return
     */
    public Future<RadiusResponse> communicate(RadiusRequest packet, List<RadiusEndpoint> endpoints){
        return communicate(packet, endpoints, timeoutHandler, null, null, null);
    }

    private void communicateRecursive(RadiusRequest packet, List<RadiusEndpoint> endpoints, int endpointIndex,
                                      Promise<RadiusResponse> promise, Throwable lastException,  TimeoutHandler perRequestTimeoutHandler,
                                      BiConsumer<Byte, InetSocketAddress> preSendHook, BiConsumer<Byte, InetSocketAddress> timeoutHook, 
                                      BiConsumer<Byte, InetSocketAddress> postReceiveHook) {

        if (endpointIndex >= endpoints.size()) {
            promise.tryFailure(new IOException("Client send failed - all endpoints failed", lastException));
            return;
        }

        communicate(packet, endpoints.get(endpointIndex), perRequestTimeoutHandler, preSendHook, timeoutHook, postReceiveHook).addListener((Future<RadiusResponse> f) -> {
            if (f.isSuccess())
                promise.trySuccess(f.getNow());
            else
                communicateRecursive(packet, endpoints, endpointIndex + 1, promise, f.cause(), perRequestTimeoutHandler, preSendHook, timeoutHook, postReceiveHook);
        });
    }


    private Future<RadiusResponse> communicate(RadiusRequest packet, RadiusEndpoint endpoint, TimeoutHandler perRequestTimeoutHandler, 
                BiConsumer<Byte, InetSocketAddress> preSendHook, BiConsumer<Byte, InetSocketAddress> timeoutHook, 
                BiConsumer<Byte, InetSocketAddress> postReceiveHook) {

        final Promise<RadiusResponse> promise = eventLoopGroup.next().<RadiusResponse>newPromise().addListener(f -> {
            if (f.isSuccess()){
                log.debug("Response received, packet: {}", f.getNow());
                // Report response received to hook
                if(postReceiveHook != null) postReceiveHook.accept(((RadiusResponse)f.getNow()).getType(), endpoint.getAddress());
            }
            else{
                log.warn(f.cause().getMessage());
                // Report final timeout to hook
                if(timeoutHook != null) timeoutHook.accept(packet.getType(), endpoint.getAddress());
            }
        });

        channelFuture.addListener(s -> {
            if (s.isSuccess())
                send(new PendingRequestCtx(packet, endpoint, promise), 1, perRequestTimeoutHandler, preSendHook, timeoutHook);
            else
                promise.tryFailure(s.cause());
        });

        return promise;
    }

    /**
     * Sends packet to specified endpoint.
     *
     * @param packet   packet to send
     * @param endpoint endpoint to send packet to
     * @param maxAttempts max attemtps per endpoint
     * @param timeoutMillis timeout in milliseconds per request
     * @param preSendHook function to invoke before sending the radius request. 
     * Parameters are the type of request and InetSocket address of the remote
     * @param timeoutHook function to invoke when a timeout expires. Parameters
     * are the type of the request and the InetSocketAddress of the remote.
     * @param postReceiveHook function to invoke when a responsne is received.ctx
     * Parameters are the type of the response and the InetSocketAddress of the remote.
     * @return deferred response containing response packet or exception
     */
    public Future<RadiusResponse> communicate(RadiusRequest packet, RadiusEndpoint endpoint, int maxAttempts, int timeoutMillis, 
            BiConsumer<Byte, InetSocketAddress> preSendHook, BiConsumer<Byte, InetSocketAddress> timeoutHook, 
            BiConsumer<Byte, InetSocketAddress> postReceiveHook){
        return communicate(packet, endpoint, new FixedTimeoutHandler(timer, maxAttempts, timeoutMillis), preSendHook, timeoutHook, postReceiveHook);
    }

    /**
     * Sends packet to specified endpoint with the default timeoutHandler and no hooks.
     * @param packet   packet to send
     * @param endpoint endpoint to send packet to
     * @return
     */
    public Future<RadiusResponse> communicate(RadiusRequest packet, RadiusEndpoint endpoint){
        return communicate(packet, endpoint, timeoutHandler, null, null, null);
    }

    private void send(PendingRequestCtx ctx, int attempt, TimeoutHandler timeoutHandler, BiConsumer<Byte, InetSocketAddress> preSendHook, BiConsumer<Byte, InetSocketAddress> timeoutHook){
        // More appropriate to use debug than info
        log.debug("Attempt {}, sending packet to {}", attempt, ctx.getEndpoint().getAddress());
        
        // Report send packet to hook
        if(preSendHook != null) preSendHook.accept(ctx.getRequest().getType(), ctx.getEndpoint().getAddress());
        channelFuture.channel().writeAndFlush(ctx);

        timeoutHandler.onTimeout(() -> {
                // Report timeout with retry (not final) to hook
                if(timeoutHook != null) timeoutHook.accept(ctx.getRequest().getType(), ctx.getEndpoint().getAddress());
                send(ctx, attempt + 1, timeoutHandler, preSendHook, timeoutHook);
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