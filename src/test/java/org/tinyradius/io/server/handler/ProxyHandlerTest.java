package org.tinyradius.io.server.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.dictionary.DefaultDictionary;
import org.tinyradius.core.dictionary.Dictionary;
import org.tinyradius.core.packet.request.AccountingRequest;
import org.tinyradius.core.packet.request.RadiusRequest;
import org.tinyradius.core.packet.response.RadiusResponse;
import org.tinyradius.io.RadiusEndpoint;
import org.tinyradius.io.client.RadiusClient;
import org.tinyradius.io.server.RequestCtx;
import org.tinyradius.io.server.ResponseCtx;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.tinyradius.core.packet.PacketType.ACCOUNTING_REQUEST;
import static org.tinyradius.core.packet.PacketType.ACCOUNTING_RESPONSE;

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
    void handleSuccess() throws RadiusPacketException {
        ProxyHandler proxyHandler = new ProxyHandler(client) {
            @Override
            public Optional<RadiusEndpoint> getOriginServer(RadiusRequest request, RadiusEndpoint clientEndpoint) {
                return Optional.of(stubEndpoint);
            }
        };

        final AccountingRequest request = (AccountingRequest) RadiusRequest.create(dictionary, ACCOUNTING_REQUEST, (byte) 1, null, Collections.emptyList());
        final RadiusResponse mockResponse = RadiusResponse.create(dictionary, ACCOUNTING_RESPONSE, (byte) 123, null,
                Collections.singletonList(dictionary.createAttribute(-1, (byte) 33, "state1".getBytes(UTF_8))));

        when(client.communicate(any(RadiusRequest.class), any(RadiusEndpoint.class))).thenReturn(GlobalEventExecutor.INSTANCE.newSucceededFuture(mockResponse));

        proxyHandler.channelRead0(ctx, new RequestCtx(request, stubEndpoint));

        verify(ctx, timeout(200)).writeAndFlush(responseCaptor.capture());
        assertEquals(mockResponse, responseCaptor.getValue().getResponse());
    }

    @Test
    void handleRadiusClientError() throws RadiusPacketException {
        ProxyHandler proxyHandler = new ProxyHandler(client) {
            @Override
            public Optional<RadiusEndpoint> getOriginServer(RadiusRequest request, RadiusEndpoint clientEndpoint) {
                return Optional.of(stubEndpoint);
            }
        };

        when(client.communicate(any(RadiusRequest.class), any(RadiusEndpoint.class))).thenReturn(GlobalEventExecutor.INSTANCE.newFailedFuture(new Exception("test")));

        final AccountingRequest packet = (AccountingRequest) RadiusRequest.create(dictionary, ACCOUNTING_REQUEST, (byte) 123, null, Collections.emptyList());

        proxyHandler.channelRead0(ctx, new RequestCtx(packet, stubEndpoint));

        await().during(500, MILLISECONDS).untilAsserted(
                () -> verifyNoInteractions(ctx));
    }

    @Test
    void handleNullServerEndPoint() throws RadiusPacketException {

        final ProxyHandler proxyHandler = new ProxyHandler(client) {
            @Override
            public Optional<RadiusEndpoint> getOriginServer(RadiusRequest request, RadiusEndpoint clientEndpoint) {
                return Optional.empty();
            }
        };

        final AccountingRequest packet = (AccountingRequest) RadiusRequest.create(dictionary, ACCOUNTING_REQUEST, (byte) 123, null, Collections.emptyList());
        proxyHandler.channelRead0(ctx, new RequestCtx(packet, stubEndpoint));

        verifyNoInteractions(ctx);
    }
}
