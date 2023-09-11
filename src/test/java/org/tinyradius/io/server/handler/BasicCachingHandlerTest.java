package org.tinyradius.io.server.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.HashedWheelTimer;
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

    @Mock
    private ChannelHandlerContext ctx;

    @Test
    void cacheHitAndTimeout() throws RadiusPacketException {
        final BasicCachingHandler basicCachingHandler =
                new BasicCachingHandler(new HashedWheelTimer(), 500);

        final RadiusRequest request = RadiusRequest.create(dictionary, ACCOUNTING_REQUEST, (byte) 100, null, Collections.emptyList()).encodeRequest("test");
        final RequestCtx requestCtx = new RequestCtx(request, new RadiusEndpoint(new InetSocketAddress(0), "foo"));
        final ResponseCtx responseContext = requestCtx.withResponse(RadiusResponse.create(dictionary, ACCESS_ACCEPT, (byte) 100, null, Collections.emptyList()));

        // cache miss
        final ArrayList<Object> in1 = new ArrayList<>();
        basicCachingHandler.decode(ctx, requestCtx, in1);
        assertEquals(1, in1.size());
        assertTrue(in1.contains(requestCtx));

        // cache miss again if no response
        final ArrayList<Object> in2 = new ArrayList<>();
        basicCachingHandler.decode(ctx, requestCtx, in2);
        assertEquals(1, in2.size());
        assertTrue(in2.contains(requestCtx));

        // response
        final ArrayList<Object> in3 = new ArrayList<>();
        basicCachingHandler.encode(ctx, responseContext, in3);

        // ctx only used if cache hits (to flush response)
        verifyNoInteractions(ctx);

        // cache hit
        final ArrayList<Object> in4 = new ArrayList<>();
        basicCachingHandler.decode(ctx, requestCtx, in4);
        assertEquals(0, in4.size());

        // cache hit again
        final ArrayList<Object> in5 = new ArrayList<>();
        basicCachingHandler.decode(ctx, requestCtx, in5);
        assertEquals(0, in5.size());

        // check 2 cache hits
        verify(ctx, times(2)).writeAndFlush(responseContext);

        // assert cache miss, but only after 500ms (cache timeout)
        await().atLeast(500, MILLISECONDS).untilAsserted(() -> {
            final ArrayList<Object> in6 = new ArrayList<>();
            basicCachingHandler.decode(ctx, requestCtx, in6);
            assertEquals(1, in6.size());
            assertTrue(in6.contains(requestCtx));
        });

        // cache miss again if no response
        final ArrayList<Object> in7 = new ArrayList<>();
        basicCachingHandler.decode(ctx, requestCtx, in7);
        assertEquals(1, in7.size());
        assertTrue(in7.contains(requestCtx));
    }
}