package org.tinyradius.io.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import io.netty.util.concurrent.Promise;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.dictionary.DefaultDictionary;
import org.tinyradius.core.dictionary.Dictionary;
import org.tinyradius.core.packet.request.RadiusRequest;
import org.tinyradius.core.packet.response.RadiusResponse;
import org.tinyradius.io.RadiusEndpoint;
import org.tinyradius.io.client.timeout.FixedTimeoutHandler;
import org.tinyradius.io.client.timeout.TimeoutHandler;

import java.net.InetSocketAddress;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.tinyradius.core.packet.PacketType.ACCESS_REQUEST;

@ExtendWith(MockitoExtension.class)
class RadiusClientTest {

    private static final Dictionary dictionary = DefaultDictionary.INSTANCE;

    private final SecureRandom random = new SecureRandom();

    private final EventLoopGroup eventLoopGroup = new MultiThreadIoEventLoopGroup(2, NioIoHandler.newFactory());
    private final InetSocketAddress address = new InetSocketAddress(0);
    private final RadiusEndpoint stubEndpoint = new RadiusEndpoint(address, "secret");

    private final Bootstrap bootstrap = new Bootstrap().group(eventLoopGroup).channel(NioDatagramChannel.class);

    @AutoClose("stop")
    private final Timer timer = new HashedWheelTimer();

    // Convenience class for encapsulating the three hook types
    static class CommunicateHooks implements RadiusClientHooks{
        public void preSendHook(int code, InetSocketAddress address){ /* To be mocked */}
        public void timeoutHook(int code, InetSocketAddress address){ /* To be mocked */}
        public void postReceiveHook(int code, InetSocketAddress address){/* To be mocked */}
    }

    @Spy
    private final TimeoutHandler timeoutHandler = new FixedTimeoutHandler(timer); // no retries

    // A per-request timeout handler
    int perRequestAttempts = 2;
    @Spy
    private final TimeoutHandler perRequestTimeoutHandler = new FixedTimeoutHandler(timer, perRequestAttempts, 500); // no retries

    @Test
    void communicateWithTimeout() throws RadiusPacketException {
        var request = RadiusRequest.create(dictionary, ACCESS_REQUEST, (byte) 1, null, List.of());

        try (var radiusClient = new RadiusClient(bootstrap, address, timeoutHandler, CapturingOutboundHandler.NOOP)) {
            final TimeoutException e = assertThrows(TimeoutException.class,
                    () -> radiusClient.communicate(request, stubEndpoint).syncUninterruptibly());

            assertTrue(e.getMessage().toLowerCase().contains("max attempts reached"));
            verify(timeoutHandler).onTimeout(any(), anyInt(), any());
        }
    }

    @Test
    void communicateSuccess() throws RadiusPacketException, InterruptedException {
        var id = (byte) random.nextInt(256);
        var request = RadiusRequest.create(dictionary, ACCESS_REQUEST, (byte) 1, null, List.of());
        var response = RadiusResponse.create(DefaultDictionary.INSTANCE, (byte) 2, id, null, List.of());

        try (var radiusClient = new RadiusClient(bootstrap, address, timeoutHandler, CapturingOutboundHandler.of(response))) {
            var future = radiusClient.communicate(request, stubEndpoint).await();

            assertTrue(future.isSuccess());
            assertSame(response, future.getNow());
        }
    }

    @Test
    void outboundError() throws RadiusPacketException, InterruptedException {
        var exception = new Exception("test 123");
        var request = RadiusRequest.create(dictionary, ACCESS_REQUEST, (byte) 1, null, List.of());

        try (var radiusClient = new RadiusClient(bootstrap, address, timeoutHandler, CapturingOutboundHandler.of(exception))) {
            var future = radiusClient.communicate(request, stubEndpoint).await();

            assertFalse(future.isSuccess());
            assertSame(exception, future.cause());
        }
    }

    @Test
    void communicateEndpointListFirstSuccess() throws RadiusPacketException, InterruptedException {
        byte id = (byte) random.nextInt(256);
        var request = RadiusRequest.create(dictionary, ACCESS_REQUEST, (byte) 1, null, List.of());
        var response = RadiusResponse.create(DefaultDictionary.INSTANCE, (byte) 2, id, null, List.of());
        var capturingOutboundHandler = CapturingOutboundHandler.of(response);

        var stubEndpoint2 = new RadiusEndpoint(new InetSocketAddress(1), "secret2"); // never used
        var stubEndpoint3 = new RadiusEndpoint(new InetSocketAddress(2), "secret3"); // never used
        var endpoints = Arrays.asList(stubEndpoint, stubEndpoint2, stubEndpoint3);

        try (var radiusClient = new RadiusClient(bootstrap, address, timeoutHandler, capturingOutboundHandler)) {
            var future = radiusClient.communicate(request, endpoints).await();

            assertTrue(future.isSuccess());
            assertEquals(response, future.getNow());

            assertEquals(1, capturingOutboundHandler.requests.size());
            assertEquals("secret", capturingOutboundHandler.requests.get(0).getEndpoint().getSecret());
        }
    }

    @Test
    void communicateEndpointListEmpty() throws RadiusPacketException, InterruptedException {
        var request = RadiusRequest.create(dictionary, ACCESS_REQUEST, (byte) 1, null, List.of());
        var exception = new Exception("test 123");

        try (var radiusClient = new RadiusClient(bootstrap, address, timeoutHandler, CapturingOutboundHandler.of(exception))) {
            var future = radiusClient.communicate(request, List.of()).await();

            assertFalse(future.isSuccess());
            assertTrue(future.cause().getMessage().contains("no valid endpoints"));
        }
    }

    @Test
    void communicateEndpointListAllFail() throws RadiusPacketException, InterruptedException {
        var exception = new Exception("test 123");
        var stubEndpoint2 = new RadiusEndpoint(new InetSocketAddress(1), "secret2");
        var stubEndpoint3 = new RadiusEndpoint(new InetSocketAddress(2), "secret3");
        var endpoints = List.of(stubEndpoint, stubEndpoint2, stubEndpoint3);

        var capturingOutboundHandler = CapturingOutboundHandler.of(exception);

        try (var radiusClient = new RadiusClient(bootstrap, address, timeoutHandler, capturingOutboundHandler)) {
            var request = RadiusRequest.create(dictionary, ACCESS_REQUEST, (byte) 1, null, List.of());
            var future = radiusClient.communicate(request, endpoints).await();

            assertFalse(future.isSuccess());
            assertTrue(future.cause().getMessage().contains("all endpoints failed"));
            assertSame(exception, future.cause().getCause());

            assertEquals(3, capturingOutboundHandler.requests.size());
            assertEquals("secret", capturingOutboundHandler.requests.get(0).getEndpoint().getSecret());
            assertEquals("secret2", capturingOutboundHandler.requests.get(1).getEndpoint().getSecret());
            assertEquals("secret3", capturingOutboundHandler.requests.get(2).getEndpoint().getSecret());

            assertEquals(1, request.toByteBuf().refCnt()); // unpooled, let GC handle it
        }
    }

    @Test
    void retainPacketsWithRetries() throws RadiusPacketException, InterruptedException {
        try (var radiusClient = new RadiusClient(
                bootstrap, address, new FixedTimeoutHandler(timer, 2, 0), CapturingOutboundHandler.NOOP)) {
            var request = RadiusRequest.create(dictionary, ACCESS_REQUEST, (byte) 1, null, List.of());
            var future = radiusClient.communicate(request, stubEndpoint).await();

            assertFalse(future.isSuccess());
            assertEquals("Client send timeout - max attempts reached: 2", future.cause().getMessage());

            assertEquals(1, request.toByteBuf().refCnt()); // unpooled, let GC handle it
        }
    }

    // "communicate" with per-request TimeoutHandler, timeout and hooks: request with success
    @Test 
    void communicateWithHooksSuccess() throws RadiusPacketException, InterruptedException{
        var id = (byte) random.nextInt(256);
        var request = RadiusRequest.create(dictionary, ACCESS_REQUEST, (byte) 1, null, List.of());
        var response = RadiusResponse.create(DefaultDictionary.INSTANCE, (byte) 2, id, null, List.of());

        CommunicateHooks communicateHooks  = Mockito.mock(CommunicateHooks.class);

        try (var radiusClient = new RadiusClient(bootstrap, address, timeoutHandler, CapturingOutboundHandler.of(response))) {
            var future = radiusClient.communicate(request, Arrays.asList(stubEndpoint), timeoutHandler,
                communicateHooks).await();

            assertTrue(future.isSuccess());
            assertSame(response, future.getNow());
            verify(communicateHooks, times(1)).preSendHook(eq(1), any(InetSocketAddress.class));
            verify(communicateHooks, times(1)).postReceiveHook(eq(2), any(InetSocketAddress.class));
        }
    }

    // "communicate" with per-request TimeoutHandler and hooks: requests timed out in all endpoints
    @Test 
    void communicateWithHooksTimeout() throws RadiusPacketException, InterruptedException{
        var request = RadiusRequest.create(dictionary, ACCESS_REQUEST, (byte) 1, null, List.of());

        var stubEndpoint2 = new RadiusEndpoint(new InetSocketAddress(1), "secret2"); // never used
        var stubEndpoint3 = new RadiusEndpoint(new InetSocketAddress(2), "secret3"); // never used
        var endpoints = Arrays.asList(stubEndpoint, stubEndpoint2,  stubEndpoint3);

        CommunicateHooks communicateHooks  = Mockito.mock(CommunicateHooks.class);

        try (var radiusClient = new RadiusClient(bootstrap, address, timeoutHandler, CapturingOutboundHandler.NOOP)) {
            var future = radiusClient.communicate(request, endpoints, perRequestTimeoutHandler, communicateHooks).await();

            assertFalse(future.isSuccess());
            verify(communicateHooks, times(3 * perRequestAttempts)).preSendHook(eq(1), any(InetSocketAddress.class));
            verify(communicateHooks, times(3 * perRequestAttempts)).timeoutHook(eq(1), any(InetSocketAddress.class));
        }
    }

    @ChannelHandler.Sharable
    @RequiredArgsConstructor
    private static class CapturingOutboundHandler extends ChannelOutboundHandlerAdapter {

        private static final CapturingOutboundHandler NOOP = new CapturingOutboundHandler(p -> {
        });

        private static CapturingOutboundHandler of(Exception exception) {
            return new CapturingOutboundHandler(p -> p.tryFailure(exception));
        }

        private static CapturingOutboundHandler of(RadiusResponse response) {
            return new CapturingOutboundHandler(p -> p.trySuccess(response));
        }

        private final Consumer<Promise<RadiusResponse>> failFast;
        private final List<PendingRequestCtx> requests = new ArrayList<>();

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
            final PendingRequestCtx reqCtx = (PendingRequestCtx) msg;
            requests.add(reqCtx);
            failFast.accept((reqCtx).getResponse());

            /*
              https://netty.io/wiki/reference-counted-objects.html
              Unlike inbound messages, outbound messages are created by your application, and it is
              the responsibility of Netty to release these after writing them out to the wire. However,
              the handlers that intercept your write requests should make sure to release any
              intermediary objects properly. (e.g. encoders)
             */
            reqCtx.getRequest().toByteBuf().release();
        }
    }
}