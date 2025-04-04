package org.tinyradius.io.server;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.InetSocketAddress;
import java.util.List;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class RadiusServerTest {

    private final EventLoopGroup eventLoopGroup = new MultiThreadIoEventLoopGroup(2, NioIoHandler.newFactory());

    private final Bootstrap bootstrap = new Bootstrap().group(eventLoopGroup).channel(NioDatagramChannel.class);

    @Mock
    private ChannelHandler accessHandler;

    @Mock
    private ChannelHandler acctHandler;

    @Test
    void serverStartStop() {
        final RadiusServer server = new RadiusServer(bootstrap, accessHandler, acctHandler, new InetSocketAddress(0), new InetSocketAddress(0));

        // registering event loop and adding handlers is almost instant
        // socket binding is variable, possible race condition, so we sync
        server.isReady().syncUninterruptibly();

        final Channel accessChannel = server.getChannels().get(0);
        final Channel acctChannel = server.getChannels().get(1);

        // registered with eventLoop
        assertTrue(accessChannel.isRegistered());
        assertTrue(acctChannel.isRegistered());

        // bound to socket
        assertNotNull(accessChannel.localAddress());
        assertNotNull(acctChannel.localAddress());

        // handlers registered
        String TAIL_CONTEXT = "DefaultChannelPipeline$TailContext#0";
        final List<String> accessPipeline = accessChannel.pipeline().names();
        assertEquals(TAIL_CONTEXT, accessPipeline.get(1));
        assertTrue(accessPipeline.get(0).contains("ChannelHandler$MockitoMock$"));
        final List<String> accountingPipeline = acctChannel.pipeline().names();
        assertEquals(TAIL_CONTEXT, accountingPipeline.get(1));
        assertTrue(accountingPipeline.get(0).contains("ChannelHandler$MockitoMock$"));

        server.close();

        // not registered with eventLoop
        await().until(() -> !accessChannel.isRegistered());
        await().until(() -> !acctChannel.isRegistered());

        // no handlers registered
        assertEquals(List.of(TAIL_CONTEXT), accessChannel.pipeline().names());
        assertEquals(List.of(TAIL_CONTEXT), acctChannel.pipeline().names());
    }
}