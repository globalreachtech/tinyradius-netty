package org.tinyradius.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.HashedWheelTimer;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tinyradius.client.timeout.FixedTimeoutHandler;
import org.tinyradius.client.timeout.TimeoutHandler;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.packet.request.RadiusRequest;
import org.tinyradius.packet.response.RadiusResponse;
import org.tinyradius.util.RadiusEndpoint;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RadiusClientTest {

    private final SecureRandom random = new SecureRandom();
    private static final Dictionary dictionary = DefaultDictionary.INSTANCE;

    private final NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup(2);
    private final InetSocketAddress address = new InetSocketAddress(0);
    private final RadiusEndpoint stubEndpoint = new RadiusEndpoint(address, "secret");

    private final Bootstrap bootstrap = new Bootstrap().group(eventLoopGroup).channel(NioDatagramChannel.class);

    @Spy
    private final TimeoutHandler timeoutHandler = new FixedTimeoutHandler(new HashedWheelTimer());

    @Test
    void communicateWithTimeout() {
        RadiusClient radiusClient = new RadiusClient(bootstrap, address, timeoutHandler, new CapturingOutboundHandler(a -> {
        }));

        final RadiusRequest request = RadiusRequest.create(dictionary, (byte) 1, (byte) 1, null, Collections.emptyList());
        final IOException e = assertThrows(IOException.class,
                () -> radiusClient.communicate(request, stubEndpoint).syncUninterruptibly());

        assertTrue(e.getMessage().toLowerCase().contains("max attempts reached"));
        verify(timeoutHandler).onTimeout(any(), anyInt(), any());
    }

    @Test
    void communicateSuccess() {
        final byte id = (byte) random.nextInt(256);
        final RadiusResponse response = RadiusResponse.create(DefaultDictionary.INSTANCE, (byte) 2, id, null, Collections.emptyList());
        final CapturingOutboundHandler capturingOutboundHandler = new CapturingOutboundHandler(a -> a.trySuccess(response));
        final RadiusClient radiusClient = new RadiusClient(bootstrap, address, timeoutHandler, capturingOutboundHandler);

        final RadiusRequest request = RadiusRequest.create(dictionary, (byte) 1, (byte) 1, null, Collections.emptyList());
        final Future<RadiusResponse> future = radiusClient.communicate(request, stubEndpoint);

        assertFalse(future.isDone());

        await().until(future::isDone);
        assertTrue(future.isSuccess());
        assertSame(response, future.getNow());
    }

    @Test
    void outboundError() {
        final Exception expectedException = new Exception("test 123");
        final CapturingOutboundHandler capturingOutboundHandler = new CapturingOutboundHandler(p -> p.tryFailure(expectedException));
        final RadiusClient radiusClient = new RadiusClient(bootstrap, address, timeoutHandler, capturingOutboundHandler);

        final RadiusRequest request = RadiusRequest.create(dictionary, (byte) 1, (byte) 1, null, Collections.emptyList());
        final Future<RadiusResponse> future = radiusClient.communicate(request, stubEndpoint);
        assertFalse(future.isDone());

        await().until(future::isDone);
        assertFalse(future.isSuccess());
        assertSame(expectedException, future.cause());
    }

    @Test
    void communicateEndpointList() {
        final Exception expectedException = new Exception("test 123");
        final CapturingOutboundHandler capturingOutboundHandler = spy(new CapturingOutboundHandler(p -> p.tryFailure(expectedException)));
        final RadiusClient radiusClient = new RadiusClient(bootstrap, address, timeoutHandler, capturingOutboundHandler);

        final InetSocketAddress address2 = new InetSocketAddress(1);
        final RadiusEndpoint stubEndpoint2 = new RadiusEndpoint(address2, "secret2");

        final InetSocketAddress address3 = new InetSocketAddress(2);
        final RadiusEndpoint stubEndpoint3 = new RadiusEndpoint(address3, "secret3");

        final List<RadiusEndpoint> endpoints = Arrays.asList(stubEndpoint, stubEndpoint2, stubEndpoint3);

        final RadiusRequest request = RadiusRequest.create(dictionary, (byte) 1, (byte) 1, null, Collections.emptyList());
        final Future<RadiusResponse> future = radiusClient.communicate(request, endpoints);
        assertFalse(future.isDone());

        await().until(future::isDone);
        assertFalse(future.isSuccess());

        assertTrue(future.cause().getMessage().contains("all endpoints failed"));
        assertSame(expectedException, future.cause().getCause());

        assertEquals(3, capturingOutboundHandler.requests.size());
        assertEquals("secret", capturingOutboundHandler.requests.get(0).getEndpoint().getSecret());
        assertEquals("secret2", capturingOutboundHandler.requests.get(1).getEndpoint().getSecret());
        assertEquals("secret3", capturingOutboundHandler.requests.get(2).getEndpoint().getSecret());
    }

    private static class CapturingOutboundHandler extends ChannelOutboundHandlerAdapter {

        private final Consumer<Promise<RadiusResponse>> failFast;
        private final List<PendingRequestCtx> requests = new ArrayList<>();

        private CapturingOutboundHandler(Consumer<Promise<RadiusResponse>> failFast) {
            this.failFast = failFast;
        }

        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
            requests.add((PendingRequestCtx) msg);
            failFast.accept(((PendingRequestCtx) msg).getResponse());
        }
    }
}