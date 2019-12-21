package org.tinyradius.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
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
import org.tinyradius.client.retry.BasicTimeoutHandler;
import org.tinyradius.client.retry.TimeoutHandler;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.packet.AccessRequest;
import org.tinyradius.packet.PacketEncoder;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.util.RadiusEndpoint;
import org.tinyradius.util.RadiusException;

import java.net.InetSocketAddress;
import java.security.SecureRandom;

import static io.netty.util.ResourceLeakDetector.Level.PARANOID;
import static io.netty.util.ResourceLeakDetector.Level.SIMPLE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RadiusClientTest {

    private final SecureRandom random = new SecureRandom();
    private static final Dictionary dictionary = DefaultDictionary.INSTANCE;
    private static final PacketEncoder packetEncoder = new PacketEncoder(dictionary);

    private final ChannelFactory<NioDatagramChannel> channelFactory = new ReflectiveChannelFactory<>(NioDatagramChannel.class);

    private final HashedWheelTimer timer = new HashedWheelTimer();
    private final NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup(2);
    private final InetSocketAddress address = new InetSocketAddress(0);

    private final Bootstrap bootstrap = new Bootstrap().group(eventLoopGroup).channel(NioDatagramChannel.class);

    @Spy
    private TimeoutHandler timeoutHandler = new BasicTimeoutHandler(timer, 3, 100);

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
        RadiusClient radiusClient = new RadiusClient(bootstrap, address, timeoutHandler, new ChannelInitializer<DatagramChannel>() {
            @Override
            protected void initChannel(DatagramChannel ch) throws Exception {

            }
        });

        final RadiusPacket request = new AccessRequest(dictionary, random.nextInt(256), null).encodeRequest("test");
        final RadiusEndpoint endpoint = new RadiusEndpoint(new InetSocketAddress(0), "test");

        final RadiusException radiusException = assertThrows(RadiusException.class,
                () -> radiusClient.communicate(request, endpoint).syncUninterruptibly());

        assertTrue(radiusException.getMessage().toLowerCase().contains("max retries"));
        verify(timeoutHandler, times(3)).onTimeout(any(), anyInt(), any());

        radiusClient.close();
    }

    @Test
    void communicateSuccess() {
        final int id = random.nextInt(256);
        final RadiusPacket response = new RadiusPacket(DefaultDictionary.INSTANCE, 2, id);
        final MockClientHandler mockClientHandler = new MockClientHandler(response);
        final BasicTimeoutHandler simpleRetryStrategyHelper = new BasicTimeoutHandler(timer, 3, 1000);

        final RadiusClient radiusClient = new RadiusClient(bootstrap, new InetSocketAddress(0), timeoutHandler, mockClientHandler);

        final RadiusPacket request = new AccessRequest(dictionary, id, null).encodeRequest("test");

        final Future<RadiusPacket> future = radiusClient.communicate(request, new RadiusEndpoint(new InetSocketAddress(0), "mySecret"));
        assertFalse(future.isDone());

        mockClientHandler.handleResponse(null);
        assertTrue(future.isSuccess());

        assertSame(response, future.getNow());

        radiusClient.close();
    }

    @Test
    void badDatagramEncode() {
        final int id = random.nextInt(256);
        final MockClientHandler mockClientHandler = new MockClientHandler(null);

        final RadiusClient radiusClient = new RadiusClient(bootstrap, new InetSocketAddress(0), timeoutHandler, mockClientHandler);

        final RadiusPacket request = new RadiusPacket(dictionary, 1, id);

        final Future<RadiusPacket> future = radiusClient.communicate(request, new RadiusEndpoint(new InetSocketAddress(0), ""));

        assertTrue(future.isDone());
        assertTrue(future.cause().getMessage().toLowerCase().contains("missing authenticator"));

        radiusClient.close();
    }

    private static class MockClientHandler extends SimpleChannelInboundHandler<DatagramPacket> {

        private final RadiusPacket response;
        private Promise<RadiusPacket> promise;

        private MockClientHandler(RadiusPacket response) {
            this.response = response;
        }

        public DatagramPacket prepareDatagram(RadiusPacket original, RadiusEndpoint endpoint, InetSocketAddress sender, Promise<RadiusPacket> promise) throws RadiusException {
            this.promise = promise;
            return packetEncoder.toDatagram(original, endpoint.getAddress(), sender);
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket datagramPacket) {
            promise.trySuccess(response);
        }
    }
}