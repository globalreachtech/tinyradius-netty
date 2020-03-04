package org.tinyradius;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.packet.*;
import org.tinyradius.packet.util.RadiusPackets;
import org.tinyradius.server.RadiusServer;
import org.tinyradius.server.RequestCtx;
import org.tinyradius.server.SecretProvider;
import org.tinyradius.server.handler.RequestHandler;
import org.tinyradius.server.handler.ServerPacketCodec;

import java.net.InetSocketAddress;

import static org.tinyradius.packet.AccessRequest.USER_NAME;
import static org.tinyradius.packet.util.PacketType.*;

/**
 * TestServer can answer both to Access-Request and Access-Response
 * packets with Access-Accept/Reject or Accounting-Response, respectively.
 */
public class TestServer {

    private static final Logger logger = LogManager.getLogger();

    public static void main(String[] args) throws Exception {

        final Dictionary dictionary = DefaultDictionary.INSTANCE;

        final EventLoopGroup eventLoopGroup = new NioEventLoopGroup(4);
        final Bootstrap bootstrap = new Bootstrap().channel(NioDatagramChannel.class).group(eventLoopGroup);

        final SecretProvider secretProvider = remote ->
                remote.getAddress().getHostAddress().equals("127.0.0.1") ? "testing123" : null;

        final ServerPacketCodec serverPacketCodec = new ServerPacketCodec(dictionary, secretProvider);

        final SimpleAccessHandler simpleAccessHandler = new SimpleAccessHandler();
        final SimpleAccountingHandler simpleAccountingHandler = new SimpleAccountingHandler();

        try (RadiusServer server = new RadiusServer(bootstrap,
                new ChannelInitializer<DatagramChannel>() {
                    @Override
                    protected void initChannel(DatagramChannel ch) {
                        ch.pipeline().addLast(serverPacketCodec, simpleAccessHandler);
                    }
                },
                new ChannelInitializer<DatagramChannel>() {
                    @Override
                    protected void initChannel(DatagramChannel ch) {
                        ch.pipeline().addLast(serverPacketCodec, simpleAccountingHandler);
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
        protected Class<AccessRequest> acceptedPacketType() {
            return AccessRequest.class;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, RequestCtx msg) {

            final AccessRequestPap request = (AccessRequestPap) msg.getRequest();

            String password = request.getAttributeString(USER_NAME).equals("test") ? "password" : null;
            byte type = request.verifyPassword(password) ? ACCESS_ACCEPT : ACCESS_REJECT;

            RadiusResponse answer = RadiusPackets.createResponse(request.getDictionary(), type, request.getId(), null, request.getAttributes((byte) 33));

            ctx.writeAndFlush(msg.withResponse(answer));
        }
    }

    public static class SimpleAccountingHandler extends RequestHandler {

        @Override
        protected Class<AccountingRequest> acceptedPacketType() {
            return AccountingRequest.class;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, RequestCtx msg) {
            final RadiusRequest request = msg.getRequest();

            RadiusResponse answer = RadiusPackets.createResponse(request.getDictionary(), ACCOUNTING_RESPONSE, request.getId(), null, request.getAttributes((byte) 33));

            ctx.writeAndFlush(msg.withResponse(answer));
        }
    }
}
