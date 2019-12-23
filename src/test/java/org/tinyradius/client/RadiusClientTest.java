package org.tinyradius.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandler;
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
import org.mockito.junit.jupiter.MockitoExtension;
import org.tinyradius.client.retry.BasicTimeoutHandler;
import org.tinyradius.client.retry.TimeoutHandler;
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

    private final HashedWheelTimer timer = new HashedWheelTimer();
    private final NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup(2);
    private final InetSocketAddress address = new InetSocketAddress(0);

    private final Bootstrap bootstrap = new Bootstrap().group(eventLoopGroup).channel(NioDatagramChannel.class);

    private TimeoutHandler timeoutHandler = new BasicTimeoutHandler(timer);

    @BeforeAll
    static void beforeAll() {
        ResourceLeakDetector.setLevel(PARANOID);
    }

    @AfterAll
    static void afterAll() {
        ResourceLeakDetector.setLevel(SIMPLE);
    }

    @Test()
    void communicateWithTimeout() {
        RadiusClient radiusClient = new RadiusClient(bootstrap, address, timeoutHandler, mock(ChannelHandler.class));

        final RadiusPacket request = new AccessRequest(dictionary, random.nextInt(256), null).encodeRequest("test");
        final RadiusEndpoint endpoint = new RadiusEndpoint(new InetSocketAddress(0), "test");

        final IOException e = assertThrows(IOException.class,
                () -> radiusClient.communicate(request, endpoint).syncUninterruptibly());

        assertTrue(e.getMessage().toLowerCase().contains("max retries"));
        verify(timeoutHandler, times(3)).onTimeout(any(), anyInt(), any());

        radiusClient.close();
    }

    @Test
    void communicateSuccess() throws InterruptedException {
        final int id = random.nextInt(256);
        final RadiusPacket response = new RadiusPacket(DefaultDictionary.INSTANCE, 2, id);
        final MockOutboundHandler mockOutboundHandler = new MockOutboundHandler(a -> a.trySuccess(response));

        final RadiusClient radiusClient = new RadiusClient(bootstrap, new InetSocketAddress(0), timeoutHandler, mockOutboundHandler);

        final RadiusPacket request = new AccessRequest(dictionary, id, null).encodeRequest("test");

        final Future<RadiusPacket> future = radiusClient.communicate(request, new RadiusEndpoint(new InetSocketAddress(0), "mySecret"));

        assertFalse(future.isDone());

        Thread.sleep(500);

        assertTrue(future.isDone());
        assertTrue(future.isSuccess());
        assertSame(response, future.getNow());
    }

    @Test
    void badEncode() throws InterruptedException {
        final MockOutboundHandler mockOutboundHandler = new MockOutboundHandler(p -> p.tryFailure(new Exception("test 123")));

        final RadiusClient radiusClient = new RadiusClient(bootstrap, new InetSocketAddress(0), timeoutHandler, mockOutboundHandler);
        final RadiusPacket request = new RadiusPacket(dictionary, 1, 1);

        final Future<RadiusPacket> future = radiusClient.communicate(request, new RadiusEndpoint(new InetSocketAddress(0), ""));
        assertFalse(future.isDone());

        Thread.sleep(500);

        assertTrue(future.isDone());
        assertFalse(future.isSuccess());
        assertTrue(future.cause().getMessage().toLowerCase().contains("test 123"));
    }

    private static class MockOutboundHandler extends ChannelOutboundHandlerAdapter {

        private Consumer<Promise<RadiusPacket>> failFast;

        private MockOutboundHandler(Consumer<Promise<RadiusPacket>> failFast) {
            this.failFast = failFast;
        }

        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
            failFast.accept(((RequestCtxWrapper) msg).getResponse());
        }
    }
}