package org.tinyradius.client;

import io.netty.channel.ChannelFactory;
import io.netty.channel.ReflectiveChannelFactory;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.HashedWheelTimer;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.Timer;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.tinyradius.client.handler.ClientHandler;
import org.tinyradius.client.retry.SimpleRetryStrategy;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.packet.AccessRequest;
import org.tinyradius.packet.PacketEncoder;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.util.RadiusEndpoint;
import org.tinyradius.util.RadiusException;

import java.net.InetSocketAddress;
import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicInteger;

import static io.netty.util.ResourceLeakDetector.Level.PARANOID;
import static io.netty.util.ResourceLeakDetector.Level.SIMPLE;
import static org.junit.jupiter.api.Assertions.*;

class RadiusClientTest {

    private final SecureRandom random = new SecureRandom();
    private final static Dictionary dictionary = DefaultDictionary.INSTANCE;
    private final static PacketEncoder packetEncoder = new PacketEncoder(dictionary);

    private final ChannelFactory<NioDatagramChannel> channelFactory = new ReflectiveChannelFactory<>(NioDatagramChannel.class);

    private static final HashedWheelTimer timer = new HashedWheelTimer();
    private static final NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup(4);

    @BeforeAll
    static void beforeAll() {
        ResourceLeakDetector.setLevel(PARANOID);
    }

    @AfterAll
    static void afterAll() {
        timer.stop();
        eventLoopGroup.shutdownGracefully().syncUninterruptibly();
        ResourceLeakDetector.setLevel(SIMPLE);
    }

    @Test()
    void communicateWithTimeout() {
        final SimpleRetryStrategyHelper retryStrategy = new SimpleRetryStrategyHelper(timer, 3, 100);
        RadiusClient radiusClient = new RadiusClient(
                eventLoopGroup, channelFactory, new MockClientHandler(null), retryStrategy, new InetSocketAddress(0));

        final RadiusPacket request = new AccessRequest(dictionary, random.nextInt(256), null).encodeRequest("test");
        final RadiusEndpoint endpoint = new RadiusEndpoint(new InetSocketAddress(0), "test");

        final RadiusException radiusException = assertThrows(RadiusException.class,
                () -> radiusClient.communicate(request, endpoint).syncUninterruptibly());

        assertTrue(radiusException.getMessage().toLowerCase().contains("max retries"));
        assertEquals(3, retryStrategy.count.get());
        radiusClient.stop().syncUninterruptibly();
    }

    @Test
    void communicateSuccess() {
        final int id = random.nextInt(256);
        final RadiusPacket response = new RadiusPacket(DefaultDictionary.INSTANCE, 2, id);
        final MockClientHandler mockClientHandler = new MockClientHandler(response);
        final SimpleRetryStrategyHelper simpleRetryStrategyHelper = new SimpleRetryStrategyHelper(timer, 3, 1000);

        final RadiusClient radiusClient = new RadiusClient(
                eventLoopGroup, channelFactory, mockClientHandler, simpleRetryStrategyHelper, new InetSocketAddress(0));

        final RadiusPacket request = new AccessRequest(dictionary, id, null).encodeRequest("test");

        final Future<RadiusPacket> future = radiusClient.communicate(request, new RadiusEndpoint(new InetSocketAddress(0), "mySecret"));
        assertFalse(future.isDone());

        mockClientHandler.handleResponse(null);
        assertTrue(future.isSuccess());

        assertSame(response, future.getNow());

        radiusClient.stop().syncUninterruptibly();
    }

    @Test
    void badDatagramEncode() {
        final int id = random.nextInt(256);
        final MockClientHandler mockClientHandler = new MockClientHandler(null);

        final RadiusClient radiusClient = new RadiusClient(
                eventLoopGroup, channelFactory, mockClientHandler, null, new InetSocketAddress(0));

        final RadiusPacket request = new RadiusPacket(dictionary, 1, id);

        final Future<RadiusPacket> future = radiusClient.communicate(request, new RadiusEndpoint(new InetSocketAddress(0), ""));

        assertTrue(future.isDone());
        assertTrue(future.cause().getMessage().toLowerCase().contains("missing authenticator"));

        radiusClient.stop().syncUninterruptibly();
    }

    private static class SimpleRetryStrategyHelper extends SimpleRetryStrategy {

        private final AtomicInteger count = new AtomicInteger();

        SimpleRetryStrategyHelper(Timer timer, int maxAttempts, int retryWait) {
            super(timer, maxAttempts, retryWait);
        }

        @Override
        public void scheduleRetry(Runnable retry, int totalAttempts, Promise<RadiusPacket> promise) {
            super.scheduleRetry(retry, totalAttempts, promise);
            count.getAndIncrement();
        }
    }

    private static class MockClientHandler extends ClientHandler {

        private RadiusPacket response;
        private Promise<RadiusPacket> promise;

        private MockClientHandler(RadiusPacket response) {
            this.response = response;
        }

        @Override
        public DatagramPacket prepareDatagram(RadiusPacket packet, RadiusEndpoint endpoint, InetSocketAddress sender, Promise<RadiusPacket> promise) throws RadiusException {
            this.promise = promise;
            return packetEncoder.toDatagram(packet, endpoint.getAddress(), sender);
        }

        @Override
        protected void handleResponse(DatagramPacket datagramPacket) {
            promise.trySuccess(response);
        }
    }
}