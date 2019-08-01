package org.tinyradius.server;

import io.netty.channel.Channel;
import io.netty.channel.ReflectiveChannelFactory;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.packet.*;
import org.tinyradius.server.handler.AcctHandler;
import org.tinyradius.server.handler.AuthHandler;
import org.tinyradius.server.handler.DeduplicatorHandler;
import org.tinyradius.server.handler.RequestHandler;
import org.tinyradius.util.SecretProvider;

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
        final NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup(4);
        final Timer timer = new HashedWheelTimer();

        final SecretProvider secretProvider = remote ->
                remote.getAddress().getHostAddress().equals("127.0.0.1") ? "testing123" : null;

        final RequestHandler<AccessRequest> authHandler = new DeduplicatorHandler<>(new AuthHandler() {
            @Override
            public String getUserPassword(String userName) {
                return userName.equals("test") ? "password" : null;
            }

            @Override
            public Promise<RadiusPacket> handlePacket(Channel channel, AccessRequest packet, InetSocketAddress remoteAddress, String sharedSecret) {
                logger.info("Received Access-Request:\n" + packet);
                final Promise<RadiusPacket> promise = channel.eventLoop().newPromise();
                super.handlePacket(channel, packet, remoteAddress, sharedSecret).addListener((Future<RadiusPacket> f) -> {
                    final RadiusPacket response = f.getNow();
                    if (response == null) {
                        logger.info("Ignore packet.");
                        promise.tryFailure(f.cause());
                    } else {
                        if (response.getType() == PacketType.ACCESS_ACCEPT)
                            response.addAttribute("Reply-Message", "Welcome " + packet.getUserName() + "!");
                        logger.info("Answer:\n" + response);
                        promise.trySuccess(response);
                    }
                });

                return promise;
            }
        }, timer, 30000);

        RequestHandler<AccountingRequest> acctHandler = new DeduplicatorHandler<>(new AcctHandler(), timer, 30000);

        final RadiusServer server = new RadiusServer(
                eventLoopGroup,
                new ReflectiveChannelFactory<>(NioDatagramChannel.class),
                new HandlerAdapter<>(packetEncoder, authHandler, timer, secretProvider, AccessRequest.class),
                new HandlerAdapter<>(packetEncoder, acctHandler, timer, secretProvider, AccountingRequest.class),
                new InetSocketAddress(11812), new InetSocketAddress(11813));

        final Future<Void> future = server.start();
        future.addListener(future1 -> {
            if (future1.isSuccess()) {
                logger.info("Server started");
            } else {
                logger.info("Failed to start server: " + future1.cause());
                server.stop().syncUninterruptibly();
                eventLoopGroup.shutdownGracefully();
            }
        });

        System.in.read();

        server.stop().syncUninterruptibly();

        eventLoopGroup.shutdownGracefully().awaitUninterruptibly();
    }

}
