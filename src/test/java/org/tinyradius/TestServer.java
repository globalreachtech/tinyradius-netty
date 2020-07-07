package org.tinyradius;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tinyradius.core.dictionary.DefaultDictionary;
import org.tinyradius.core.dictionary.Dictionary;
import org.tinyradius.core.packet.request.AccessRequest;
import org.tinyradius.core.packet.request.AccessRequestPap;
import org.tinyradius.core.packet.request.RadiusRequest;
import org.tinyradius.core.packet.response.RadiusResponse;
import org.tinyradius.io.server.RadiusServer;
import org.tinyradius.io.server.RequestCtx;
import org.tinyradius.io.server.SecretProvider;
import org.tinyradius.io.server.handler.BasicCachingHandler;
import org.tinyradius.io.server.handler.RequestHandler;
import org.tinyradius.io.server.handler.ServerPacketCodec;

import java.net.InetSocketAddress;

import static org.tinyradius.core.packet.PacketType.*;

/**
 * TestServer can answer both to Access-Request and Access-Response
 * packets with Access-Accept/Reject or Accounting-Response, respectively.
 */
public class TestServer {

    private static final byte USER_NAME = 1;

    private static final Logger logger = LogManager.getLogger();

    public static void main(String[] args) throws Exception {

        final Dictionary dictionary = DefaultDictionary.INSTANCE;

        final EventLoopGroup eventLoopGroup = new NioEventLoopGroup(4);
        final Bootstrap bootstrap = new Bootstrap().channel(NioDatagramChannel.class).group(eventLoopGroup);

        final SecretProvider secretProvider = remote ->
                remote.getAddress().getHostAddress().equals("127.0.0.1") ? "testing123" : null;

        final ServerPacketCodec serverPacketCodec = new ServerPacketCodec(dictionary, secretProvider);

        final Timer timer = new HashedWheelTimer();
        final BasicCachingHandler cachingHandlerAuth = new BasicCachingHandler(timer, 5000);
        final BasicCachingHandler cachingHandlerAcct = new BasicCachingHandler(timer, 5000);

        final SimpleAccessHandler simpleAccessHandler = new SimpleAccessHandler();
        final SimpleAccountingHandler simpleAccountingHandler = new SimpleAccountingHandler();

        try (RadiusServer server = new RadiusServer(bootstrap,
                new ChannelInitializer<DatagramChannel>() {
                    @Override
                    protected void initChannel(DatagramChannel ch) {
                        ch.pipeline().addLast(serverPacketCodec, cachingHandlerAuth, simpleAccessHandler);
                    }
                },
                new ChannelInitializer<DatagramChannel>() {
                    @Override
                    protected void initChannel(DatagramChannel ch) {
                        ch.pipeline().addLast(serverPacketCodec, cachingHandlerAcct, simpleAccountingHandler);
                    }
                },
                new InetSocketAddress(11812), new InetSocketAddress(11813))) {

            server.isReady().addListener(future1 -> {
                if (future1.isSuccess()) {
                    logger.info("Server started");
                } else {
                    logger.info("Failed to start server", future1.cause());
                    server.close();
                    eventLoopGroup.shutdownGracefully();
                }
            });

            System.in.read();
        }

        eventLoopGroup.shutdownGracefully();
    }

    public static class SimpleAccessHandler extends RequestHandler {

        @Override
        protected Class<? extends RadiusRequest> acceptedPacketType() {
            return AccessRequest.class;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, RequestCtx msg) {

            final AccessRequestPap request = (AccessRequestPap) msg.getRequest();

            final String password = request.getAttribute(USER_NAME).get().getValueString().equals("test") ? "password" : null;
            final byte type = request.getPassword()
                    .filter(p -> p.equals(password))
                    .map(x -> ACCESS_ACCEPT)
                    .orElse(ACCESS_REJECT);

            RadiusResponse answer = RadiusResponse.create(request.getDictionary(), type, request.getId(), null, request.filterAttributes((byte) 33));

            ctx.writeAndFlush(msg.withResponse(answer));
        }
    }

    public static class SimpleAccountingHandler extends RequestHandler {

        @Override
        protected Class<? extends RadiusRequest> acceptedPacketType() {
            return AccessRequest.class;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, RequestCtx msg) {
            final RadiusRequest request = msg.getRequest();

            RadiusResponse answer = RadiusResponse.create(request.getDictionary(), ACCOUNTING_RESPONSE, request.getId(), null, request.filterAttributes((byte) 33));

            ctx.writeAndFlush(msg.withResponse(answer));
        }
    }
}
