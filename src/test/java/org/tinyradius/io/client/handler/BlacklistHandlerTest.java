package org.tinyradius.io.client.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.ImmediateEventExecutor;
import io.netty.util.concurrent.Promise;
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
import org.tinyradius.io.client.PendingRequestCtx;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.tinyradius.core.packet.PacketType.ACCOUNTING_REQUEST;

@ExtendWith(MockitoExtension.class)
class BlacklistHandlerTest {

    private static final Dictionary dictionary = DefaultDictionary.INSTANCE;

    private final EventExecutor eventExecutor = ImmediateEventExecutor.INSTANCE;

    private final BlacklistHandler handler = new BlacklistHandler(5000, 2);

    @Mock
    private ChannelHandlerContext handlerContext;

    @Mock
    private ChannelPromise channelPromise;

    private PendingRequestCtx genRequest(int port) throws RadiusPacketException {
        final RadiusEndpoint endpoint = new RadiusEndpoint(new InetSocketAddress(port), "mySecret");
        final Promise<RadiusResponse> promise = eventExecutor.newPromise();
        final RadiusRequest request = RadiusRequest.create(dictionary, ACCOUNTING_REQUEST, (byte) 1, null, Collections.emptyList());
        return new PendingRequestCtx(request, endpoint, promise);
    }

    @Test
    void noBlacklist() throws RadiusPacketException {
        final PendingRequestCtx request = genRequest(0);
        handler.write(handlerContext, request, channelPromise);

        verify(handlerContext).write(request, channelPromise);
    }

    @Test
    void testBlacklist() throws RadiusPacketException {
        final PendingRequestCtx request1 = genRequest(0);
        handler.write(handlerContext, request1, channelPromise);
        verify(handlerContext).write(request1, channelPromise);

        final PendingRequestCtx request2 = genRequest(0);
        handler.write(handlerContext, request2, channelPromise);
        verify(handlerContext).write(request2, channelPromise);

        // two failures to trigger blacklist
        request1.getResponse().tryFailure(new TimeoutException());
        request2.getResponse().tryFailure(new TimeoutException());

        final PendingRequestCtx request3 = genRequest(0);
        handler.write(handlerContext, request3, channelPromise);

        // next request blacklisted
        verify(handlerContext, never()).write(request3, channelPromise);
        assertTrue(request3.getResponse().isDone());
        assertFalse(request3.getResponse().isSuccess());

        // different endpoint still works
        final PendingRequestCtx diffEndpoint = genRequest(1);
        handler.write(handlerContext, diffEndpoint, channelPromise);
        verify(handlerContext).write(diffEndpoint, channelPromise);
    }

    @Test
    void blacklistEndByExpire() throws RadiusPacketException {
        final PendingRequestCtx request1 = genRequest(0);
        handler.write(handlerContext, request1, channelPromise);

        final PendingRequestCtx request2 = genRequest(0);
        handler.write(handlerContext, request2, channelPromise);

        // two failures to trigger blacklist
        request1.getResponse().tryFailure(new TimeoutException());
        request2.getResponse().tryFailure(new TimeoutException());

        // next request blacklisted
        final PendingRequestCtx blacklisted1 = genRequest(0);
        handler.write(handlerContext, blacklisted1, channelPromise);

        verify(handlerContext, never()).write(blacklisted1, channelPromise);
        assertTrue(blacklisted1.getResponse().isDone());
        assertFalse(blacklisted1.getResponse().isSuccess());

        // expires after at least 4sec (actual 5)
        await().atLeast(4, SECONDS).untilAsserted(() -> {
            final PendingRequestCtx laterRequest = genRequest(0);
            handler.write(handlerContext, laterRequest, channelPromise);
            verify(handlerContext).write(laterRequest, channelPromise);
        });
    }

    @Test
    void blacklistEndBySuccessfulResponse() throws RadiusPacketException {
        final PendingRequestCtx request1 = genRequest(0);
        handler.write(handlerContext, request1, channelPromise);

        final PendingRequestCtx request2 = genRequest(0);
        handler.write(handlerContext, request2, channelPromise);

        final PendingRequestCtx request3 = genRequest(0);
        handler.write(handlerContext, request3, channelPromise);

        // two failures to trigger blacklist
        request1.getResponse().tryFailure(new TimeoutException());
        request2.getResponse().tryFailure(new TimeoutException());
        // request3 no response yet

        // next request blacklisted
        final PendingRequestCtx blacklisted1 = genRequest(0);
        handler.write(handlerContext, blacklisted1, channelPromise);

        verify(handlerContext, never()).write(blacklisted1, channelPromise);
        assertTrue(blacklisted1.getResponse().isDone());
        assertFalse(blacklisted1.getResponse().isSuccess());

        // successful response
        request3.getResponse().trySuccess(null);

        // success should expire blacklist
        final PendingRequestCtx laterRequest = genRequest(0);
        handler.write(handlerContext, laterRequest, channelPromise);
        verify(handlerContext).write(laterRequest, channelPromise);
    }

    @Test
    void repeatFailsDontExtendBlacklist() throws InterruptedException, RadiusPacketException {
        final PendingRequestCtx request1 = genRequest(0);
        handler.write(handlerContext, request1, channelPromise);

        final PendingRequestCtx request2 = genRequest(0);
        handler.write(handlerContext, request2, channelPromise);

        final PendingRequestCtx request3 = genRequest(0);
        handler.write(handlerContext, request3, channelPromise);

        // two failures to trigger blacklist
        request1.getResponse().tryFailure(new TimeoutException());
        request2.getResponse().tryFailure(new TimeoutException());

        // next request blacklisted
        final PendingRequestCtx blacklisted1 = genRequest(0);
        handler.write(handlerContext, blacklisted1, channelPromise);

        verify(handlerContext, never()).write(blacklisted1, channelPromise);
        assertTrue(blacklisted1.getResponse().isDone());
        assertFalse(blacklisted1.getResponse().isSuccess());

        Thread.sleep(4000);

        // fail again
        request3.getResponse().tryFailure(new Exception());

        // still expires at most 6sec (actual 5) after first failure
        await().atMost(2, SECONDS).untilAsserted(() -> {
            final PendingRequestCtx laterRequest = genRequest(0);
            handler.write(handlerContext, laterRequest, channelPromise);
            verify(handlerContext).write(laterRequest, channelPromise);
        });
    }

    @Test
    void onlyTimeoutsTriggerBlacklist() throws RadiusPacketException {
        final PendingRequestCtx request1 = genRequest(0);
        handler.write(handlerContext, request1, channelPromise);
        verify(handlerContext).write(request1, channelPromise);

        final PendingRequestCtx request2 = genRequest(0);
        handler.write(handlerContext, request2, channelPromise);
        verify(handlerContext).write(request2, channelPromise);

        // only one TimeoutException should not trigger blacklist
        request1.getResponse().tryFailure(new TimeoutException());
        request2.getResponse().tryFailure(new IOException());

        final PendingRequestCtx request3 = genRequest(0);
        handler.write(handlerContext, request3, channelPromise);
        verify(handlerContext).write(request3, channelPromise);

        // second TimeoutException should trigger blacklist
        request3.getResponse().tryFailure(new TimeoutException());

        final PendingRequestCtx request4 = genRequest(0);
        handler.write(handlerContext, request4, channelPromise);
        verify(handlerContext, never()).write(request4, channelPromise);
    }
}