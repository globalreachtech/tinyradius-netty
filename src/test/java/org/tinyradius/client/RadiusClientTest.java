package org.tinyradius.client;

import io.netty.channel.ReflectiveChannelFactory;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.HashedWheelTimer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.packet.AccessRequest;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.util.RadiusEndpoint;
import org.tinyradius.util.RadiusException;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.security.SecureRandom;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class RadiusClientTest {

    private SecureRandom random = new SecureRandom();
    private static DefaultDictionary dictionary = DefaultDictionary.INSTANCE;

    private int id;
    private HashedWheelTimer timer = new HashedWheelTimer();
    private NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup(4);
    private ReflectiveChannelFactory<NioDatagramChannel> channelFactory = new ReflectiveChannelFactory<>(NioDatagramChannel.class);

    @BeforeEach
    void setup() {
        id = random.nextInt(256);
    }

    @Test()
    void communicateWithTimeout() throws UnknownHostException {
        SimpleClientHandler handler = new SimpleClientHandler(timer, dictionary, 1000);
        RadiusClient<NioDatagramChannel> radiusClient = new RadiusClient<>(eventLoopGroup, channelFactory, handler, InetAddress.getLocalHost(), 11814);

        final RadiusPacket request = new AccessRequest(dictionary, id, null).encodeRequest("test");
        final RadiusEndpoint endpoint = new RadiusEndpoint(new InetSocketAddress(0), "test");
        int maxAttempts = 3;

        assertThrows(RadiusException.class,
                () -> radiusClient.communicate(request, endpoint, maxAttempts).syncUninterruptibly());

    }
}
