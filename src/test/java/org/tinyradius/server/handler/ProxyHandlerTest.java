package org.tinyradius.server.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tinyradius.attribute.Attributes;
import org.tinyradius.client.RadiusClient;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.packet.AccountingRequest;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.packet.RadiusResponse;
import org.tinyradius.packet.util.RadiusPackets;
import org.tinyradius.server.RequestCtx;
import org.tinyradius.server.ResponseCtx;
import org.tinyradius.util.RadiusEndpoint;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.tinyradius.packet.util.PacketType.ACCOUNTING_RESPONSE;

@ExtendWith(MockitoExtension.class)
class ProxyHandlerTest {

    private static final Dictionary dictionary = DefaultDictionary.INSTANCE;

    private final RadiusEndpoint stubEndpoint = new RadiusEndpoint(new InetSocketAddress(0), "shared");

    @Mock
    private RadiusClient client;

    @Mock
    private ChannelHandlerContext ctx;

    @Captor
    private ArgumentCaptor<ResponseCtx> responseCaptor;

    @Test
    void handleSuccess() throws InterruptedException {
        ProxyHandler proxyHandler = new ProxyHandler(client) {
            @Override
            public Optional<RadiusEndpoint> getProxyServer(RadiusPacket packet, RadiusEndpoint client) {
                return Optional.of(stubEndpoint);
            }
        };

        final AccountingRequest request = new AccountingRequest(dictionary, (byte) 1, null, Collections.emptyList());
        final RadiusResponse mockResponse = RadiusPackets.createResponse(dictionary, ACCOUNTING_RESPONSE, (byte) 123, null,
                Collections.singletonList(Attributes.create(dictionary, -1, (byte) 33, "state1".getBytes(UTF_8))));

        when(client.communicate(any(), any())).thenReturn(GlobalEventExecutor.INSTANCE.newSucceededFuture(mockResponse));

        proxyHandler.channelRead0(ctx, new RequestCtx(request, stubEndpoint));

        Thread.sleep(200);

        verify(ctx).writeAndFlush(responseCaptor.capture());
        assertEquals(mockResponse, responseCaptor.getValue().getResponse());
    }

    @Test
    void handleRadiusClientError() throws InterruptedException {
        ProxyHandler proxyHandler = new ProxyHandler(client) {
            @Override
            public Optional<RadiusEndpoint> getProxyServer(RadiusPacket packet, RadiusEndpoint client) {
                return Optional.of(stubEndpoint);
            }
        };

        when(client.communicate(any(), any())).thenReturn(GlobalEventExecutor.INSTANCE.newFailedFuture(new Exception("test")));

        final AccountingRequest packet = new AccountingRequest(dictionary, (byte) 123, null, Collections.emptyList());

        proxyHandler.channelRead0(ctx, new RequestCtx(packet, stubEndpoint));

        Thread.sleep(200);

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

        final AccountingRequest packet = new AccountingRequest(dictionary, (byte) 123, null, Collections.emptyList());

        proxyHandler.channelRead0(ctx, new RequestCtx(packet, stubEndpoint));

        verifyNoInteractions(ctx);
    }
}
