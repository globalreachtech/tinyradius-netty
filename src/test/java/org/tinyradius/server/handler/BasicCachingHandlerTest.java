package org.tinyradius.server.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.HashedWheelTimer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.packet.AccountingRequest;
import org.tinyradius.packet.GenericRadiusPacket;
import org.tinyradius.packet.RadiusPackets;
import org.tinyradius.server.RequestCtx;
import org.tinyradius.server.ResponseCtx;
import org.tinyradius.util.RadiusEndpoint;
import org.tinyradius.util.RadiusPacketException;

import java.net.InetSocketAddress;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static org.tinyradius.packet.PacketType.ACCESS_ACCEPT;

@ExtendWith(MockitoExtension.class)
class BasicCachingHandlerTest {

    private final Dictionary dictionary = DefaultDictionary.INSTANCE;

    @Mock
    private ChannelHandlerContext ctx;

    @Test
    void cacheHitAndTimeout() throws InterruptedException, RadiusPacketException {
        final BasicCachingHandler<RequestCtx, ResponseCtx> basicCachingHandler =
                new BasicCachingHandler<>(new HashedWheelTimer(), 500, RequestCtx.class, ResponseCtx.class);

        final GenericRadiusPacket request = new AccountingRequest(dictionary, 100, null).encodeRequest("test");
        final RequestCtx requestCtx = new RequestCtx(request, new RadiusEndpoint(new InetSocketAddress(0), "foo"));
        final ResponseCtx responseContext = requestCtx.withResponse(RadiusPackets.create(dictionary, ACCESS_ACCEPT, 100));

        // cache miss
        final ArrayList<Object> out1 = new ArrayList<>();
        basicCachingHandler.decode(ctx, requestCtx, out1);
        assertEquals(1, out1.size());
        assertTrue(out1.contains(requestCtx));

        // cache miss again if no response
        final ArrayList<Object> out2 = new ArrayList<>();
        basicCachingHandler.decode(ctx, requestCtx, out2);
        assertEquals(1, out2.size());
        assertTrue(out2.contains(requestCtx));

        // response
        final ArrayList<Object> out3 = new ArrayList<>();
        basicCachingHandler.encode(ctx, responseContext, out3);

        verifyNoInteractions(ctx);

        // cache hit
        final ArrayList<Object> out4 = new ArrayList<>();
        basicCachingHandler.decode(ctx, requestCtx, out4);
        assertEquals(0, out4.size());

        // cache hit again
        final ArrayList<Object> out5 = new ArrayList<>();
        basicCachingHandler.decode(ctx, requestCtx, out5);
        assertEquals(0, out5.size());
        verify(ctx, times(2)).writeAndFlush(responseContext);

        // cache timeout
        Thread.sleep(1000);

        // cache miss
        final ArrayList<Object> out6 = new ArrayList<>();
        basicCachingHandler.decode(ctx, requestCtx, out6);
        assertEquals(1, out6.size());
        assertTrue(out6.contains(requestCtx));

        // cache miss again if no response
        final ArrayList<Object> out7 = new ArrayList<>();
        basicCachingHandler.decode(ctx, requestCtx, out7);
        assertEquals(1, out7.size());
        assertTrue(out7.contains(requestCtx));
    }
}