package org.tinyradius.proxy.handler;

import io.netty.channel.ChannelFactory;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ReflectiveChannelFactory;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.HashedWheelTimer;
import io.netty.util.concurrent.Promise;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.tinyradius.attribute.RadiusAttribute;
import org.tinyradius.client.RadiusClient;
import org.tinyradius.client.handler.ClientHandler;
import org.tinyradius.client.handler.SimpleClientHandler;
import org.tinyradius.client.retry.RetryStrategy;
import org.tinyradius.client.retry.SimpleRetryStrategy;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.packet.AccessRequest;
import org.tinyradius.packet.AccountingRequest;
import org.tinyradius.packet.PacketEncoder;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.util.RadiusEndpoint;
import org.tinyradius.util.RadiusException;

import java.net.InetSocketAddress;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;
import static org.tinyradius.attribute.Attributes.createAttribute;
import static org.tinyradius.packet.PacketType.*;

class ProxyRequestHandlerTest {
    private static final Dictionary dictionary = DefaultDictionary.INSTANCE;
    private static final PacketEncoder packetEncoder = new PacketEncoder(dictionary);
    private final SecureRandom random = new SecureRandom();

    private static final NioEventLoopGroup eventExecutors = new NioEventLoopGroup(4);
    private static final HashedWheelTimer timer = new HashedWheelTimer();
    private static final NioDatagramChannel datagramChannel = new NioDatagramChannel();

    private static final RadiusClient client = new RadiusClient(
            eventExecutors,
            new ReflectiveChannelFactory<>(NioDatagramChannel.class),
            new SimpleClientHandler(packetEncoder),
            new SimpleRetryStrategy(timer, 3, 1000),
            new InetSocketAddress(0));

    @BeforeAll
    static void beforeAll() {
        eventExecutors.register(datagramChannel).syncUninterruptibly();
    }

    @AfterAll
    static void afterAll() {
        timer.stop();
        eventExecutors.shutdownGracefully().syncUninterruptibly();
    }

    @Test
    void handleSuccessfulPacket() {
        final int id = random.nextInt(256);
        MockClient radiusClient = new MockClient(eventExecutors,
                new ReflectiveChannelFactory<>(NioDatagramChannel.class),
                new SimpleClientHandler(packetEncoder),
                new SimpleRetryStrategy(timer, 3, 1000));

        ProxyRequestHandler proxyRequestHandler = new ProxyRequestHandler(radiusClient) {
            @Override
            public RadiusEndpoint getProxyServer(RadiusPacket packet, RadiusEndpoint client) {
                return new RadiusEndpoint(new InetSocketAddress(0), "shared");
            }
        };

        final AccountingRequest packet = new AccountingRequest(dictionary, id, null, Arrays.asList(
                createAttribute(dictionary, -1, 33, "state1".getBytes(UTF_8)),
                createAttribute(dictionary, -1, 33, "state2".getBytes(UTF_8))));

        assertEquals(ACCOUNTING_REQUEST, packet.getType());
        assertEquals(Arrays.asList("state1", "state2"), packet.getAttributes().stream()
                .map(RadiusAttribute::getValue)
                .map(String::new)
                .collect(Collectors.toList()));

        RadiusPacket response = proxyRequestHandler.handlePacket(datagramChannel, packet, new InetSocketAddress(0), "shared").syncUninterruptibly().getNow();

        assertEquals(id, response.getIdentifier());
        assertEquals(ACCOUNTING_RESPONSE, response.getType());
        assertEquals(Arrays.asList("state1", "state2"), response.getAttributes().stream()
                .map(RadiusAttribute::getValue)
                .map(String::new)
                .collect(Collectors.toList()));
    }

    @Test
    void handlePacketWithTimeout() {
        final int id = random.nextInt(256);

        ProxyRequestHandler proxyRequestHandler = new ProxyRequestHandler(client) {
            @Override
            public RadiusEndpoint getProxyServer(RadiusPacket packet, RadiusEndpoint client) {
                return new RadiusEndpoint(new InetSocketAddress(0), "shared");
            }
        };

        final AccessRequest packet = new AccessRequest(dictionary, id, null, "user", "user-pw");
        assertEquals(ACCESS_REQUEST, packet.getType());
        assertEquals("user", packet.getUserName());

        final RadiusException radiusException = assertThrows(RadiusException.class,
                () -> proxyRequestHandler.handlePacket(datagramChannel, packet, new InetSocketAddress(0), "shared").syncUninterruptibly().getNow());

        assertTrue(radiusException.getMessage().toLowerCase().contains("max retries"));
    }

    @Test
    void handlePacketNullServerEndPoint() {
        final int id = random.nextInt(256);

        ProxyRequestHandler proxyRequestHandler = new ProxyRequestHandler(client) {
            @Override
            public RadiusEndpoint getProxyServer(RadiusPacket packet, RadiusEndpoint client) {
                return null;
            }
        };

        final AccountingRequest packet = new AccountingRequest(dictionary, id, null, Collections.emptyList());

        final RadiusException radiusException = assertThrows(RadiusException.class,
                () -> proxyRequestHandler.handlePacket(datagramChannel, packet, new InetSocketAddress(0), "shared").syncUninterruptibly());

        assertTrue(radiusException.getMessage().toLowerCase().contains("server not found"));
    }

    private static class MockClient extends RadiusClient {

        MockClient(EventLoopGroup eventLoopGroup, ChannelFactory<DatagramChannel> factory, ClientHandler clientHandler, RetryStrategy retryStrategy) {
            super(eventLoopGroup, factory, clientHandler, retryStrategy, new InetSocketAddress(0));
        }

        public Promise<RadiusPacket> communicate(RadiusPacket originalPacket, RadiusEndpoint endpoint) {
            Promise<RadiusPacket> promise = eventExecutors.next().newPromise();
            RadiusPacket radiusPacket = new RadiusPacket(originalPacket.getDictionary(), ACCOUNTING_RESPONSE, originalPacket.getIdentifier(), originalPacket.getAttributes());
            promise.trySuccess(radiusPacket);
            return promise;
        }
    }

}
