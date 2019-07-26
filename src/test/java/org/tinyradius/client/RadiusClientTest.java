package org.tinyradius.client;

import io.netty.channel.ChannelFactory;
import io.netty.channel.ReflectiveChannelFactory;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.HashedWheelTimer;
import io.netty.util.ResourceLeakDetector;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.tinyradius.client.retry.SimpleRetryStrategy;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.packet.AccessRequest;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.util.RadiusEndpoint;
import org.tinyradius.util.RadiusException;

import java.net.InetSocketAddress;
import java.security.SecureRandom;

import static io.netty.util.ResourceLeakDetector.Level.PARANOID;
import static io.netty.util.ResourceLeakDetector.Level.SIMPLE;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RadiusClientTest {

    private final SecureRandom random = new SecureRandom();
    private final static DefaultDictionary dictionary = DefaultDictionary.INSTANCE;

    private final ChannelFactory<NioDatagramChannel> channelFactory = new ReflectiveChannelFactory<>(NioDatagramChannel.class);

    private static final HashedWheelTimer timer = new HashedWheelTimer();
    private static final NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup(4);

    @BeforeAll
    static void beforeAll(){
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
        final SimpleRetryStrategy retryStrategy = new SimpleRetryStrategy(timer, 3, 1000);
        RadiusClient<NioDatagramChannel> radiusClient = new RadiusClient<>(eventLoopGroup, channelFactory, handler, retryStrategy, null, 0);

        final RadiusPacket request = new AccessRequest(dictionary, random.nextInt(256), null).encodeRequest("test");
        final RadiusEndpoint endpoint = new RadiusEndpoint(new InetSocketAddress(0), "test");

        final RadiusException radiusException = assertThrows(RadiusException.class,
                () -> radiusClient.communicate(request, endpoint).syncUninterruptibly());

        assertTrue(radiusException.getMessage().toLowerCase().contains("max retries"));
        radiusClient.stop();
    }
}
