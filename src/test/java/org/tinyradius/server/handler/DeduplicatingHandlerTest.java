package org.tinyradius.server.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.HashedWheelTimer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.packet.AccessRequest;
import org.tinyradius.packet.RadiusPacket;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class DeduplicatingHandlerTest {

    private final Dictionary dictionary = DefaultDictionary.INSTANCE;

    @Mock
    private ChannelHandlerContext ctx;

    @Test
    void timeoutTest() throws InterruptedException {
        final DeduplicatingHandler deduplicatingHandler =
                new DeduplicatingHandler(new HashedWheelTimer(), 500);

        final RadiusPacket request = new AccessRequest(dictionary, 100, null).encodeRequest("test");
        final RequestContext requestContext = new RequestContext(request, null, null, null);

        // response id 0
        deduplicatingHandler.channelRead0(ctx, requestContext);
        verify(ctx).fireChannelRead(requestContext);

        // duplicate - return null
        deduplicatingHandler.channelRead0(ctx, requestContext);
        verifyNoInteractions(ctx);

        // wait for cache to timeout
        Thread.sleep(1000);

        // response id 1
        deduplicatingHandler.channelRead0(ctx, requestContext);
        verify(ctx).fireChannelRead(requestContext);

        // duplicate - return null
        deduplicatingHandler.channelRead0(ctx, requestContext);
        verifyNoInteractions(ctx);
    }
}