package org.tinyradius;

import io.netty.channel.ChannelHandler;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.packet.PacketEncoder;
import org.tinyradius.server.RadiusServer;
import org.tinyradius.server.SecretProvider;
import org.tinyradius.server.handler.AccessHandler;
import org.tinyradius.server.handler.AccountingHandler;
import org.tinyradius.server.handler.ServerPacketCodec;

import java.net.InetSocketAddress;

/**
 * Test server which terminates after 30 s.
 * Knows only the client "localhost" with secret "testing123" and
 * the user "mw" with the password "test".
 * <p>
 * TestServer can answer both to Access-Request and Access-Response
 * packets with Access-Accept/Reject or Accounting-Response, respectively.
 */
public class TestServer {

    private static final Logger logger = LoggerFactory.getLogger(TestServer.class);

    public static void main(String[] args) throws Exception {

        final PacketEncoder packetEncoder = new PacketEncoder(DefaultDictionary.INSTANCE);
        final EventLoopGroup eventLoopGroup = new NioEventLoopGroup(4);
        final Timer timer = new HashedWheelTimer();

        final SecretProvider secretProvider = remote ->
                remote.getAddress().getHostAddress().equals("127.0.0.1") ? "testing123" : null;

        final ServerPacketCodec serverPacketCodec = new ServerPacketCodec(packetEncoder, secretProvider);

        final AccessHandler accessHandler = new AccessHandler() {
            @Override
            public String getUserPassword(String userName) {
                return userName.equals("test") ? "password" : null;
            }
        };
        final ChannelHandler authHandler = new AccessHandler() {
            @Override
            public String getUserPassword(String userName) {
                return null;
            }
        };

        final AccountingHandler accountingHandler = new AccountingHandler();

        final RadiusServer server = new RadiusServer(
                authHandler,
                accountingHandler,
                new InetSocketAddress(11812), new InetSocketAddress(11813));

        server.start().addListener(future1 -> {
            if (future1.isSuccess()) {
                logger.info("Server started");
            } else {
                logger.info("Failed to start server: " + future1.cause());
                server.close();
                eventLoopGroup.shutdownGracefully();
            }
        });

        System.in.read();

        server.close();

        eventLoopGroup.shutdownGracefully().awaitUninterruptibly();
    }
}
