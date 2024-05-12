package org.tinyradius.io.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import io.netty.util.concurrent.Promise;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.tinyradius.core.packet.PacketType.ACCESS_REQUEST;

@ExtendWith(MockitoExtension.class)
class RadiusClientTest {

    private static final Dictionary dictionary = DefaultDictionary.INSTANCE;

    private final SecureRandom random = new SecureRandom();

    private final NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup(2);
    private final InetSocketAddress address = new InetSocketAddress(0);
    private final RadiusEndpoint stubEndpoint = new RadiusEndpoint(address, "secret");

    private final Bootstrap bootstrap = new Bootstrap().group(eventLoopGroup).channel(NioDatagramChannel.class);

    private final Timer timer = new HashedWheelTimer();

    @Spy
    private final TimeoutHandler timeoutHandler = new FixedTimeoutHandler(timer); // no retries

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
    void communicateSuccess() throws RadiusPacketException {
        var id = (byte) random.nextInt(256);
        var request = RadiusRequest.create(dictionary, ACCESS_REQUEST, (byte) 1, null, List.of());
        var response = RadiusResponse.create(DefaultDictionary.INSTANCE, (byte) 2, id, null, List.of());

        try (var radiusClient = new RadiusClient(bootstrap, address, timeoutHandler, CapturingOutboundHandler.of(response))) {
            var future = radiusClient.communicate(request, stubEndpoint);

            await().until(future::isDone);
            assertTrue(future.isSuccess());
            assertSame(response, future.getNow());
        }
    }

    @Test
    void outboundError() throws RadiusPacketException {
        var exception = new Exception("test 123");
        var request = RadiusRequest.create(dictionary, ACCESS_REQUEST, (byte) 1, null, List.of());

        try (var radiusClient = new RadiusClient(bootstrap, address, timeoutHandler, CapturingOutboundHandler.of(exception))) {
            var future = radiusClient.communicate(request, stubEndpoint);

            await().until(future::isDone);
            assertFalse(future.isSuccess());
            assertSame(exception, future.cause());
        }
    }

    @Test
    void communicateEndpointListFirstSuccess() throws RadiusPacketException {
        byte id = (byte) random.nextInt(256);
        var request = RadiusRequest.create(dictionary, ACCESS_REQUEST, (byte) 1, null, List.of());
        var response = RadiusResponse.create(DefaultDictionary.INSTANCE, (byte) 2, id, null, List.of());
        var capturingOutboundHandler = CapturingOutboundHandler.of(response);

        var stubEndpoint2 = new RadiusEndpoint(new InetSocketAddress(1), "secret2"); // never used
        var stubEndpoint3 = new RadiusEndpoint(new InetSocketAddress(2), "secret3"); // never used
        var endpoints = Arrays.asList(stubEndpoint, stubEndpoint2, stubEndpoint3);

        try (var radiusClient = new RadiusClient(bootstrap, address, timeoutHandler, capturingOutboundHandler)) {
            var future = radiusClient.communicate(request, endpoints);

            await().until(future::isDone);
            assertTrue(future.isSuccess());
            assertEquals(response, future.getNow());

            assertEquals(1, capturingOutboundHandler.requests.size());
            assertEquals("secret", capturingOutboundHandler.requests.get(0).getEndpoint().getSecret());
        }
    }

    @Test
    void communicateEndpointListEmpty() throws RadiusPacketException {
        var request = RadiusRequest.create(dictionary, ACCESS_REQUEST, (byte) 1, null, List.of());
        var exception = new Exception("test 123");

        try (var radiusClient = new RadiusClient(bootstrap, address, timeoutHandler, CapturingOutboundHandler.of(exception))) {
            var future = radiusClient.communicate(request, List.of());

            await().until(future::isDone);
            assertFalse(future.isSuccess());
            assertTrue(future.cause().getMessage().contains("no valid endpoints"));
        }
    }

    @Test
    void communicateEndpointListAllFail() throws RadiusPacketException {
        var exception = new Exception("test 123");
        var stubEndpoint2 = new RadiusEndpoint(new InetSocketAddress(1), "secret2");
        var stubEndpoint3 = new RadiusEndpoint(new InetSocketAddress(2), "secret3");
        var endpoints = List.of(stubEndpoint, stubEndpoint2, stubEndpoint3);

        var capturingOutboundHandler = CapturingOutboundHandler.of(exception);

        try (var radiusClient = new RadiusClient(bootstrap, address, timeoutHandler, capturingOutboundHandler)) {
            var request = RadiusRequest.create(dictionary, ACCESS_REQUEST, (byte) 1, null, List.of());
            var future = radiusClient.communicate(request, endpoints);

            await().until(future::isDone);
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
    void retainPacketsWithRetries() throws RadiusPacketException {
        try (var radiusClient = new RadiusClient(
                bootstrap, address, new FixedTimeoutHandler(timer, 2, 0), CapturingOutboundHandler.NOOP)) {
            var request = RadiusRequest.create(dictionary, ACCESS_REQUEST, (byte) 1, null, List.of());
            var future = radiusClient.communicate(request, stubEndpoint);

            await().until(future::isDone);
            assertFalse(future.isSuccess());
            assertEquals("Client send timeout - max attempts reached: 2", future.cause().getMessage());

            assertEquals(1, request.toByteBuf().refCnt()); // unpooled, let GC handle it
        }
    }

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