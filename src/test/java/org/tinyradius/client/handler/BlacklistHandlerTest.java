package org.tinyradius.client.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.nio.NioEventLoopGroup;
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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BlacklistHandlerTest {

    private final NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup(4);

    private final BlacklistHandler handler = new BlacklistHandler(5000, 2);

    @Mock
    private ChannelHandlerContext handlerContext;

    @Mock
    private ChannelPromise channelPromise;

    private PendingRequestCtx genRequest(int port) {
        final RadiusEndpoint endpoint = new RadiusEndpoint(new InetSocketAddress(port), "mySecret");
        final Promise<RadiusResponse> promise = eventLoopGroup.next().newPromise();
        return new PendingRequestCtx(mock(RadiusRequest.class), endpoint, promise);
    }

    @Test
    void noBlacklist() {
        final PendingRequestCtx request = genRequest(0);

        handler.write(handlerContext, request, channelPromise);
        verify(handlerContext).write(request, channelPromise);
    }

    @Test
    void blacklistEndByTimeout() {

    }

    @Test
    void blacklistEndBySuccessfulResponse() {
    }

}