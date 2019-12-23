package org.tinyradius.server.handler;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import io.netty.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tinyradius.attribute.RadiusAttribute;
import org.tinyradius.client.RadiusClient;
import org.tinyradius.client.retry.BasicTimeoutHandler;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.packet.AccessRequest;
import org.tinyradius.packet.AccountingRequest;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.server.RequestCtx;
import org.tinyradius.server.ServerResponseCtx;
import org.tinyradius.util.RadiusEndpoint;

import java.net.InetSocketAddress;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.tinyradius.attribute.Attributes.createAttribute;
import static org.tinyradius.packet.PacketType.ACCOUNTING_RESPONSE;

@ExtendWith(MockitoExtension.class)
class ProxyHandlerTest {

    private static final Dictionary dictionary = DefaultDictionary.INSTANCE;
    private final SecureRandom random = new SecureRandom();

    private final NioEventLoopGroup eventExecutors = new NioEventLoopGroup(4);
    private final Timer timer = new HashedWheelTimer();

    private final Bootstrap bootstrap = new Bootstrap().group(eventExecutors).channel(NioDatagramChannel.class);

    private final MockClient radiusClient = new MockClient(bootstrap, timer);

    @Mock
    private ChannelHandlerContext ctx;

    @Captor
    private ArgumentCaptor<ServerResponseCtx> responseCaptor;

    @Test
    void handleSuccessfulPacket() {
        final int id = random.nextInt(256);

        ProxyHandler proxyHandler = new ProxyHandler(radiusClient) {
            @Override
            public Optional<RadiusEndpoint> getProxyServer(RadiusPacket packet, RadiusEndpoint client) {
                return Optional.of(new RadiusEndpoint(new InetSocketAddress(0), "shared"));
            }
        };

        final AccountingRequest packet = new AccountingRequest(dictionary, id, null, Arrays.asList(
                createAttribute(dictionary, -1, 33, "state1".getBytes(UTF_8)),
                createAttribute(dictionary, -1, 33, "state2".getBytes(UTF_8))));
        final RequestCtx requestCtx = new RequestCtx(packet, new RadiusEndpoint(new InetSocketAddress(0), "shared"));

        proxyHandler.channelRead0(ctx, requestCtx);

        verify(ctx).writeAndFlush(responseCaptor.capture());

        final ServerResponseCtx value = responseCaptor.getValue();
        final RadiusPacket response = value.getResponse();
        assertEquals(id, response.getIdentifier());
        assertEquals(ACCOUNTING_RESPONSE, response.getType());
        assertEquals(Arrays.asList("state1", "state2"), response.getAttributes().stream()
                .map(RadiusAttribute::getValue)
                .map(String::new)
                .collect(Collectors.toList()));
    }

    @Test
    void handlePacketWithTimeout() {
        ProxyHandler proxyHandler = new ProxyHandler(radiusClient) {
            @Override
            public Optional<RadiusEndpoint> getProxyServer(RadiusPacket packet, RadiusEndpoint client) {
                return Optional.of(new RadiusEndpoint(new InetSocketAddress(0), "shared"));
            }
        };

        final AccessRequest packet = new AccessRequest(dictionary, 123, null, "user", "user-pw");
        final RequestCtx requestCtx = new RequestCtx(packet, new RadiusEndpoint(new InetSocketAddress(0), "shared"));

        proxyHandler.channelRead0(ctx, requestCtx);
        verifyNoInteractions(ctx);
    }

    @Test
    void handlePacketNullServerEndPoint() {

        ProxyHandler proxyHandler = new ProxyHandler(radiusClient) {
            @Override
            public Optional<RadiusEndpoint> getProxyServer(RadiusPacket packet, RadiusEndpoint client) {
                return Optional.empty();
            }
        };

        final AccountingRequest packet = new AccountingRequest(dictionary, 456, null, Collections.emptyList());
        final RequestCtx requestCtx = new RequestCtx(packet, new RadiusEndpoint(new InetSocketAddress(0), "shared"));

        proxyHandler.channelRead0(ctx, requestCtx);
        verifyNoInteractions(ctx);
    }

    private static class MockClient extends RadiusClient {

        private final EventLoopGroup eventExecutors;

        public MockClient(Bootstrap bootstrap, Timer timer) {
            super(bootstrap, new InetSocketAddress(0), new BasicTimeoutHandler(timer), new ChannelInboundHandlerAdapter());
            this.eventExecutors = bootstrap.config().group();
        }

        public Future<RadiusPacket> communicate(RadiusPacket packet, RadiusEndpoint endpoint) {
            RadiusPacket radiusPacket = new RadiusPacket(
                    packet.getDictionary(), ACCOUNTING_RESPONSE, packet.getIdentifier(), packet.getAttributes());
            return eventExecutors.next().newSucceededFuture(radiusPacket);
        }
    }

}
