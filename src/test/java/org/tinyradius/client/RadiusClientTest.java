package org.tinyradius.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.HashedWheelTimer;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tinyradius.client.timeout.BasicTimeoutHandler;
import org.tinyradius.client.timeout.TimeoutHandler;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.packet.request.AccountingRequest;
import org.tinyradius.packet.request.RadiusRequest;
import org.tinyradius.packet.response.RadiusResponse;
import org.tinyradius.util.RadiusEndpoint;
import org.tinyradius.util.RadiusPacketException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.function.Consumer;

import static io.netty.util.ResourceLeakDetector.Level.PARANOID;
import static io.netty.util.ResourceLeakDetector.Level.SIMPLE;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RadiusClientTest {

    private final SecureRandom random = new SecureRandom();
    private static final Dictionary dictionary = DefaultDictionary.INSTANCE;

    private final NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup(4);
    private final InetSocketAddress address = new InetSocketAddress(0);
    private final RadiusEndpoint stubEndpoint = new RadiusEndpoint(address, "secret");

    private final Bootstrap bootstrap = new Bootstrap().group(eventLoopGroup).channel(NioDatagramChannel.class);

    @Spy
    private final TimeoutHandler timeoutHandler = new BasicTimeoutHandler(new HashedWheelTimer());

    @BeforeAll
    static void beforeAll() {
        ResourceLeakDetector.setLevel(PARANOID);
    }

    @AfterAll
    static void afterAll() {
        ResourceLeakDetector.setLevel(SIMPLE);
    }

    @Test
    void communicateWithTimeout() throws RadiusPacketException {
        RadiusClient radiusClient = new RadiusClient(bootstrap, address, timeoutHandler, new CustomOutboundHandler(a -> {
        }));

        final RadiusRequest request = new AccountingRequest(dictionary, (byte) random.nextInt(256), null, Collections.emptyList()).encodeRequest("test");

        final IOException e = assertThrows(IOException.class,
                () -> radiusClient.communicate(request, stubEndpoint).syncUninterruptibly());

        assertTrue(e.getMessage().toLowerCase().contains("max attempts reached"));
        verify(timeoutHandler).onTimeout(any(), anyInt(), any());
    }

    @Test
    void communicateSuccess() throws RadiusPacketException {
        final byte id = (byte) random.nextInt(256);
        final RadiusResponse response = RadiusResponse.create(DefaultDictionary.INSTANCE, (byte) 2, id, null, Collections.emptyList());
        final CustomOutboundHandler customOutboundHandler = new CustomOutboundHandler(a -> a.trySuccess(response));

        final RadiusClient radiusClient = new RadiusClient(bootstrap, address, timeoutHandler, customOutboundHandler);

        final RadiusRequest request = new AccountingRequest(dictionary, id, null, Collections.emptyList()).encodeRequest("test");

        final Future<RadiusResponse> future = radiusClient.communicate(request, stubEndpoint);

        assertFalse(future.isDone());

        await().until(future::isDone);
        assertTrue(future.isSuccess());
        assertSame(response, future.getNow());
    }

    @Test
    void outboundError() {
        final Exception expectedException = new Exception("test 123");
        final CustomOutboundHandler customOutboundHandler = new CustomOutboundHandler(p -> p.tryFailure(expectedException));

        final RadiusClient radiusClient = new RadiusClient(bootstrap, address, timeoutHandler, customOutboundHandler);
        final RadiusRequest request = RadiusRequest.create(dictionary, (byte) 1, (byte) 1, null, Collections.emptyList());

        final Future<RadiusResponse> future = radiusClient.communicate(request, stubEndpoint);
        assertFalse(future.isDone());

        await().until(future::isDone);
        assertFalse(future.isSuccess());
        assertSame(expectedException, future.cause());
    }

    private static class CustomOutboundHandler extends ChannelOutboundHandlerAdapter {

        private final Consumer<Promise<RadiusResponse>> failFast;

        private CustomOutboundHandler(Consumer<Promise<RadiusResponse>> failFast) {
            this.failFast = failFast;
        }

        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
            failFast.accept(((PendingRequestCtx) msg).getResponse());
        }
    }
}