package org.tinyradius.io.server.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.dictionary.DefaultDictionary;
import org.tinyradius.core.dictionary.Dictionary;
import org.tinyradius.core.packet.request.RadiusRequest;
import org.tinyradius.core.packet.response.RadiusResponse;
import org.tinyradius.io.RadiusEndpoint;
import org.tinyradius.io.server.RequestCtx;
import org.tinyradius.io.server.ResponseCtx;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static org.tinyradius.core.packet.PacketType.ACCESS_ACCEPT;
import static org.tinyradius.core.packet.PacketType.ACCOUNTING_REQUEST;

@ExtendWith(MockitoExtension.class)
class BasicCachingHandlerTest {

    private final Dictionary dictionary = DefaultDictionary.INSTANCE;

    @AutoClose("stop")
    private final Timer timer = new HashedWheelTimer();

    @Mock
    private ChannelHandlerContext ctx;

    @Test
    void cacheHitAndTimeout() throws RadiusPacketException {
        BasicCachingHandler basicCachingHandler =
                new BasicCachingHandler(timer, 500);

        RadiusRequest request = RadiusRequest.create(dictionary, ACCOUNTING_REQUEST, (byte) 100, null, Collections.emptyList()).encodeRequest("test");
        RequestCtx requestCtx = new RequestCtx(request, new RadiusEndpoint(new InetSocketAddress(0), "foo"));
        ResponseCtx responseContext = requestCtx.withResponse(RadiusResponse.create(dictionary, ACCESS_ACCEPT, (byte) 100, null, Collections.emptyList()));

        // cache miss
        ArrayList<Object> in1 = new ArrayList<>();
        basicCachingHandler.decode(ctx, requestCtx, in1);
        assertEquals(1, in1.size());
        assertTrue(in1.contains(requestCtx));

        // cache miss again if no response
        ArrayList<Object> in2 = new ArrayList<>();
        basicCachingHandler.decode(ctx, requestCtx, in2);
        assertEquals(1, in2.size());
        assertTrue(in2.contains(requestCtx));

        // response
        ArrayList<Object> in3 = new ArrayList<>();
        basicCachingHandler.encode(ctx, responseContext, in3);

        // ctx only used if cache hits (to flush response)
        verifyNoInteractions(ctx);

        // cache hit
        ArrayList<Object> in4 = new ArrayList<>();
        basicCachingHandler.decode(ctx, requestCtx, in4);
        assertEquals(0, in4.size());

        // cache hit again
        ArrayList<Object> in5 = new ArrayList<>();
        basicCachingHandler.decode(ctx, requestCtx, in5);
        assertEquals(0, in5.size());

        // check 2 cache hits
        verify(ctx, times(2)).writeAndFlush(responseContext);

        // assert cache miss, but only after 500ms (cache timeout)
        await().atLeast(500, MILLISECONDS).untilAsserted(() -> {
            ArrayList<Object> in6 = new ArrayList<>();
            basicCachingHandler.decode(ctx, requestCtx, in6);
            assertEquals(1, in6.size());
            assertTrue(in6.contains(requestCtx));
        });

        // cache miss again if no response
        var in7 = new ArrayList<>();
        basicCachingHandler.decode(ctx, requestCtx, in7);
        assertEquals(1, in7.size());
        assertTrue(in7.contains(requestCtx));
    }

    @Test
    void packetEqualsAndDiffFields() throws RadiusPacketException {
        var basicCachingHandler = new BasicCachingHandler(timer, 5000);

        var req1 = RadiusRequest.create(dictionary, ACCOUNTING_REQUEST, (byte) 100, null, Collections.emptyList()).encodeRequest("test");
        var reqDiffId = RadiusRequest.create(dictionary, ACCOUNTING_REQUEST, (byte) 101, null, Collections.emptyList()).encodeRequest("test");
        var reqDiffAuth = RadiusRequest.create(dictionary, ACCOUNTING_REQUEST, (byte) 100, null, Collections.emptyList()).encodeRequest("test2");

        var ep1 = new RadiusEndpoint(new InetSocketAddress(1000), "foo");
        var ep2 = new RadiusEndpoint(new InetSocketAddress(2000), "foo");

        var ctx1 = new RequestCtx(req1, ep1);
        var resp1 = ctx1.withResponse(RadiusResponse.create(dictionary, ACCESS_ACCEPT, (byte) 100, null, Collections.emptyList()));
        basicCachingHandler.encode(ctx, resp1, new ArrayList<>());

        // diff id -> miss
        var outDiffId = new ArrayList<>();
        basicCachingHandler.decode(ctx, new RequestCtx(reqDiffId, ep1), outDiffId);
        assertEquals(1, outDiffId.size());

        // diff auth -> miss
        var outDiffAuth = new ArrayList<>();
        basicCachingHandler.decode(ctx, new RequestCtx(reqDiffAuth, ep1), outDiffAuth);
        assertEquals(1, outDiffAuth.size());

        // diff remoteAddress -> miss
        var outDiffAddr = new ArrayList<>();
        basicCachingHandler.decode(ctx, new RequestCtx(req1, ep2), outDiffAddr);
        assertEquals(1, outDiffAddr.size());

        // hash collision (Arrays.hashCode(new byte[]{-31}) == 0 == Arrays.hashCode(null)) -> triggers Packet.equals branches
        var mockReqA = mock(RadiusRequest.class);
        when(mockReqA.getId()).thenReturn((byte) 55);
        when(mockReqA.getAuthenticator()).thenReturn(new byte[]{-31});

        var mockReqB = mock(RadiusRequest.class);
        when(mockReqB.getId()).thenReturn((byte) 55);
        when(mockReqB.getAuthenticator()).thenReturn(null);

        var ctxA = new RequestCtx(mockReqA, ep1);
        basicCachingHandler.encode(ctx, ctxA.withResponse(resp1.getResponse()), new ArrayList<>());

        var outCollision = new ArrayList<>();
        basicCachingHandler.decode(ctx, new RequestCtx(mockReqB, ep1), outCollision);
        assertEquals(1, outCollision.size());
    }
}