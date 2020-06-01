package org.tinyradius.server;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class RadiusServerTest {

    private final NioEventLoopGroup eventExecutors = new NioEventLoopGroup(4);

    private final Bootstrap bootstrap = new Bootstrap().group(eventExecutors).channel(NioDatagramChannel.class);

    @Mock
    private ChannelHandler authHandler;

    @Mock
    private ChannelHandler acctHandler;

    @Test
    void serverStartStop() throws InterruptedException {
        final RadiusServer server = new RadiusServer(bootstrap, authHandler, acctHandler, new InetSocketAddress(51024), new InetSocketAddress(51025));

        // registering event loop and adding handlers is almost instant
        // socket binding is variable, possible race condition, so we sync
        server.isReady().syncUninterruptibly();

        // registered with eventLoop
        assertTrue(server.getAcctChannel().isRegistered());
        assertTrue(server.getAuthChannel().isRegistered());

        // bound to socket
        assertNotNull(server.getAcctChannel().localAddress());
        assertNotNull(server.getAuthChannel().localAddress());

        // handlers registered
        String TAIL_CONTEXT = "DefaultChannelPipeline$TailContext#0";
        final List<String> accountingPipeline = server.getAcctChannel().pipeline().names();
        assertEquals(TAIL_CONTEXT, accountingPipeline.get(1));
        assertTrue(accountingPipeline.get(0).contains("ChannelHandler$MockitoMock$"));

        final List<String> accessPipeline = server.getAuthChannel().pipeline().names();
        assertEquals(TAIL_CONTEXT, accessPipeline.get(1));
        assertTrue(accessPipeline.get(0).contains("ChannelHandler$MockitoMock$"));

        server.close();
        Thread.sleep(300);

        // not registered with eventLoop
        assertFalse(server.getAcctChannel().isRegistered());
        assertFalse(server.getAuthChannel().isRegistered());

        // no handlers registered
        assertEquals(Collections.singletonList(TAIL_CONTEXT), server.getAcctChannel().pipeline().names());
        assertEquals(Collections.singletonList(TAIL_CONTEXT), server.getAuthChannel().pipeline().names());
    }
}