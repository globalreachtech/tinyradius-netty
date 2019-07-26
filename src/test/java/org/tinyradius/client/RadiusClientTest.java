package org.tinyradius.client;

import io.netty.channel.ChannelFactory;
import io.netty.channel.ReflectiveChannelFactory;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.HashedWheelTimer;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.Timer;
import io.netty.util.concurrent.Promise;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.tinyradius.client.handler.SimpleClientHandler;
import org.tinyradius.client.retry.SimpleRetryStrategy;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.packet.AccessRequest;
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
    private final static DefaultDictionary dictionary = DefaultDictionary.INSTANCE;

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
        eventLoopGroup.shutdownGracefully();
        ResourceLeakDetector.setLevel(SIMPLE);
    }

    @Test()
    void communicateWithTimeout() {
        SimpleClientHandler handler = new SimpleClientHandler(dictionary);
        final SimpleRetryStrategyHelper retryStrategy = new SimpleRetryStrategyHelper(timer, 3, 100);
        RadiusClient<NioDatagramChannel> radiusClient = new RadiusClient<>(eventLoopGroup, channelFactory, handler, retryStrategy, null, 0);

        final RadiusPacket request = new AccessRequest(dictionary, random.nextInt(256), null).encodeRequest("test");
        final RadiusEndpoint endpoint = new RadiusEndpoint(new InetSocketAddress(0), "test");

        final RadiusException radiusException = assertThrows(RadiusException.class,
                () -> radiusClient.communicate(request, endpoint).syncUninterruptibly());

        assertTrue(radiusException.getMessage().toLowerCase().contains("max retries"));
        assertEquals(3, retryStrategy.count.get());
        radiusClient.stop();
    }

    @Test
    void communicateSuccess() {
        // todo
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
}