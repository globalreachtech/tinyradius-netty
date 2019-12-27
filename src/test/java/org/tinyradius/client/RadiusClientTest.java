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
import org.tinyradius.packet.AccessRequest;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.util.RadiusEndpoint;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.SecureRandom;
import java.util.function.Consumer;

import static io.netty.util.ResourceLeakDetector.Level.PARANOID;
import static io.netty.util.ResourceLeakDetector.Level.SIMPLE;
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
    private TimeoutHandler timeoutHandler = new BasicTimeoutHandler(new HashedWheelTimer());

    @BeforeAll
    static void beforeAll() {
        ResourceLeakDetector.setLevel(PARANOID);
    }

    @AfterAll
    static void afterAll() {
        ResourceLeakDetector.setLevel(SIMPLE);
    }

    @Test
    void communicateWithTimeout() {
        RadiusClient radiusClient = new RadiusClient(bootstrap, address, timeoutHandler, new CustomOutboundHandler(a -> {
        }));

        final RadiusPacket request = new AccessRequest(dictionary, random.nextInt(256), null).encodeRequest("test");

        final IOException e = assertThrows(IOException.class,
                () -> radiusClient.communicate(request, stubEndpoint).syncUninterruptibly());

        assertTrue(e.getMessage().toLowerCase().contains("max retries"));
        verify(timeoutHandler).onTimeout(any(), anyInt(), any());
    }

    @Test
    void communicateSuccess() throws InterruptedException {
        final int id = random.nextInt(256);
        final RadiusPacket response = new RadiusPacket(DefaultDictionary.INSTANCE, 2, id);
        final CustomOutboundHandler customOutboundHandler = new CustomOutboundHandler(a -> a.trySuccess(response));

        final RadiusClient radiusClient = new RadiusClient(bootstrap, address, timeoutHandler, customOutboundHandler);

        final RadiusPacket request = new AccessRequest(dictionary, id, null).encodeRequest("test");

        final Future<RadiusPacket> future = radiusClient.communicate(request, stubEndpoint);

        assertFalse(future.isDone());

        Thread.sleep(300);

        assertTrue(future.isDone());
        assertTrue(future.isSuccess());
        assertSame(response, future.getNow());
    }

    @Test
    void outboundError() throws InterruptedException {
        final Exception expectedException = new Exception("test 123");
        final CustomOutboundHandler customOutboundHandler = new CustomOutboundHandler(p -> p.tryFailure(expectedException));

        final RadiusClient radiusClient = new RadiusClient(bootstrap, address, timeoutHandler, customOutboundHandler);
        final RadiusPacket request = new RadiusPacket(dictionary, 1, 1);

        final Future<RadiusPacket> future = radiusClient.communicate(request, stubEndpoint);
        assertFalse(future.isDone());

        Thread.sleep(300);

        assertTrue(future.isDone());
        assertFalse(future.isSuccess());
        assertSame(expectedException, future.cause());
    }

    private static class CustomOutboundHandler extends ChannelOutboundHandlerAdapter {

        private final Consumer<Promise<RadiusPacket>> failFast;

        private CustomOutboundHandler(Consumer<Promise<RadiusPacket>> failFast) {
            this.failFast = failFast;
        }

        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
            failFast.accept(((RequestCtxWrapper) msg).getResponse());
        }
    }
}