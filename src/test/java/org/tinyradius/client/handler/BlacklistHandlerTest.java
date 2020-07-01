package org.tinyradius.client.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.ImmediateEventExecutor;
import io.netty.util.concurrent.Promise;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tinyradius.client.PendingRequestCtx;
import org.tinyradius.packet.request.RadiusRequest;
import org.tinyradius.packet.response.RadiusResponse;
import org.tinyradius.util.RadiusEndpoint;

import java.net.InetSocketAddress;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BlacklistHandlerTest {

    private final EventExecutor eventExecutor = ImmediateEventExecutor.INSTANCE;

    private final BlacklistHandler handler = new BlacklistHandler(5000, 2);

    @Mock
    private ChannelHandlerContext handlerContext;

    @Mock
    private ChannelPromise channelPromise;

    private PendingRequestCtx genRequest(int port) {
        final RadiusEndpoint endpoint = new RadiusEndpoint(new InetSocketAddress(port), "mySecret");
        final Promise<RadiusResponse> promise = eventExecutor.newPromise();
        return new PendingRequestCtx(mock(RadiusRequest.class), endpoint, promise);
    }

    @Test
    void noBlacklist() {
        final PendingRequestCtx request = genRequest(0);
        handler.write(handlerContext, request, channelPromise);

        verify(handlerContext).write(request, channelPromise);
    }

    @Test
    void testBlacklist() {
        final PendingRequestCtx request1 = genRequest(0);
        handler.write(handlerContext, request1, channelPromise);
        verify(handlerContext).write(request1, channelPromise);

        final PendingRequestCtx request2 = genRequest(0);
        handler.write(handlerContext, request2, channelPromise);
        verify(handlerContext).write(request2, channelPromise);

        // two failures to trigger blacklist
        request1.getResponse().tryFailure(new Exception());
        request2.getResponse().tryFailure(new Exception());

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
    void blacklistEndByExpire() {
        final PendingRequestCtx request1 = genRequest(0);
        handler.write(handlerContext, request1, channelPromise);

        final PendingRequestCtx request2 = genRequest(0);
        handler.write(handlerContext, request2, channelPromise);

        // two failures to trigger blacklist
        request1.getResponse().tryFailure(new Exception());
        request2.getResponse().tryFailure(new Exception());

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
    void blacklistEndBySuccessfulResponse() {
        final PendingRequestCtx request1 = genRequest(0);
        handler.write(handlerContext, request1, channelPromise);

        final PendingRequestCtx request2 = genRequest(0);
        handler.write(handlerContext, request2, channelPromise);

        final PendingRequestCtx request3 = genRequest(0);
        handler.write(handlerContext, request3, channelPromise);

        // two failures to trigger blacklist
        request1.getResponse().tryFailure(new Exception());
        request2.getResponse().tryFailure(new Exception());
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
    void repeatFailsDontExtendBlacklist() throws InterruptedException {
        final PendingRequestCtx request1 = genRequest(0);
        handler.write(handlerContext, request1, channelPromise);

        final PendingRequestCtx request2 = genRequest(0);
        handler.write(handlerContext, request2, channelPromise);

        final PendingRequestCtx request3 = genRequest(0);
        handler.write(handlerContext, request3, channelPromise);

        // two failures to trigger blacklist
        request1.getResponse().tryFailure(new Exception());
        request2.getResponse().tryFailure(new Exception());

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
}