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

import java.lang.reflect.Constructor;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
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
        await().untilAsserted(() -> {
            ArrayList<Object> in6 = new ArrayList<>();
            basicCachingHandler.decode(ctx, requestCtx, in6);
            assertEquals(1, in6.size());
            assertTrue(in6.contains(requestCtx));
        });

        // cache miss again if no response
        ArrayList<Object> in7 = new ArrayList<>();
        basicCachingHandler.decode(ctx, requestCtx, in7);
        assertEquals(1, in7.size());
        assertTrue(in7.contains(requestCtx));
    }

    @Test
    void packetRecord() throws Exception {
        Class<?> packetClass = Class.forName("org.tinyradius.io.server.handler.BasicCachingHandler$Packet");
        Constructor<?> constructor = packetClass.getDeclaredConstructor(int.class, InetSocketAddress.class, byte[].class);
        constructor.setAccessible(true);

        InetSocketAddress address1 = new InetSocketAddress(0);
        InetSocketAddress address2 = new InetSocketAddress(1);
        byte[] auth1 = new byte[]{1, 2, 3};
        byte[] auth2 = new byte[]{1, 2, 3};
        byte[] auth3 = new byte[]{4, 5, 6};

        Object p1 = constructor.newInstance(1, address1, auth1);
        Object p2 = constructor.newInstance(1, address1, auth2);
        Object p3 = constructor.newInstance(2, address1, auth1);
        Object p4 = constructor.newInstance(1, address2, auth1);
        Object p5 = constructor.newInstance(1, address1, auth3);
        Object p6 = constructor.newInstance(1, address1, null);

        assertEquals(p1, p2);
        assertEquals(p1.hashCode(), p2.hashCode());
        assertNotEquals(p1, p3);
        assertNotEquals(p1, p4);
        assertNotEquals(p1, p5);
        assertNotEquals(p1, p6);
        assertNotEquals(p1, "not a packet");
        assertNotEquals(p1, null);

        // toString
        String toString = p1.toString();
        assertTrue(toString.contains("id=1"));
        assertTrue(toString.contains("remoteAddress=" + address1));
        assertTrue(toString.contains("authenticator=" + Arrays.toString(auth1)));
    }
}