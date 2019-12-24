package org.tinyradius.server.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tinyradius.client.RadiusClient;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.packet.AccessRequest;
import org.tinyradius.packet.AccountingRequest;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.server.RequestCtx;
import org.tinyradius.server.ServerResponseCtx;
import org.tinyradius.util.RadiusEndpoint;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.tinyradius.attribute.Attributes.createAttribute;
import static org.tinyradius.packet.PacketType.ACCOUNTING_RESPONSE;

@ExtendWith(MockitoExtension.class)
class ProxyHandlerTest {

    private static final Dictionary dictionary = DefaultDictionary.INSTANCE;

    private final RadiusEndpoint stubEndpoint = new RadiusEndpoint(new InetSocketAddress(0), "shared");

    @Mock
    private RadiusClient client;

    @Mock
    private ChannelHandlerContext ctx;

    @Captor
    private ArgumentCaptor<ServerResponseCtx> responseCaptor;

    @Test
    void handleSuccess() {
        ProxyHandler proxyHandler = new ProxyHandler(client) {
            @Override
            public Optional<RadiusEndpoint> getProxyServer(RadiusPacket packet, RadiusEndpoint client) {
                return Optional.of(stubEndpoint);
            }
        };

        final AccountingRequest request = new AccountingRequest(dictionary, 1, null);
        final RadiusPacket mockResponse = new RadiusPacket(dictionary, ACCOUNTING_RESPONSE, 123);
        mockResponse.addAttribute(createAttribute(dictionary, -1, 33, "state1".getBytes(UTF_8)));

        when(client.communicate(any(), any())).thenReturn(GlobalEventExecutor.INSTANCE.newSucceededFuture(mockResponse));

        proxyHandler.channelRead0(ctx, new RequestCtx(request, stubEndpoint));

        verify(ctx).writeAndFlush(responseCaptor.capture());
        assertEquals(mockResponse, responseCaptor.getValue().getResponse());
    }

    @Test
    void handleRadiusClientError() {
        ProxyHandler proxyHandler = new ProxyHandler(client) {
            @Override
            public Optional<RadiusEndpoint> getProxyServer(RadiusPacket packet, RadiusEndpoint client) {
                return Optional.of(stubEndpoint);
            }
        };

        when(client.communicate(any(), any())).thenReturn(GlobalEventExecutor.INSTANCE.newFailedFuture(new Exception("test")));

        final AccessRequest packet = new AccessRequest(dictionary, 123, null);

        proxyHandler.channelRead0(ctx, new RequestCtx(packet, stubEndpoint));

        verifyNoInteractions(ctx);
    }

    @Test
    void handleNullServerEndPoint() {

        ProxyHandler proxyHandler = new ProxyHandler(client) {
            @Override
            public Optional<RadiusEndpoint> getProxyServer(RadiusPacket packet, RadiusEndpoint client) {
                return Optional.empty();
            }
        };

        final AccountingRequest packet = new AccountingRequest(dictionary, 123, null, Collections.emptyList());

        proxyHandler.channelRead0(ctx, new RequestCtx(packet, stubEndpoint));

        verifyNoInteractions(ctx);
    }
}
