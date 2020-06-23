package org.tinyradius;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tinyradius.client.RadiusClient;
import org.tinyradius.client.handler.ClientPacketCodec;
import org.tinyradius.client.handler.PromiseAdapter;
import org.tinyradius.client.timeout.BasicTimeoutHandler;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.packet.request.AccountingRequest;
import org.tinyradius.packet.request.RadiusRequest;
import org.tinyradius.server.RadiusServer;
import org.tinyradius.server.SecretProvider;
import org.tinyradius.server.handler.ProxyHandler;
import org.tinyradius.server.handler.ServerPacketCodec;
import org.tinyradius.util.RadiusEndpoint;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Optional;

/**
 * TestProxy shows how to implement a proxy radius server. You can use
 * this class together with TestClient and TestServer.
 * <p>
 * Listens on localhost:1812 and localhost:1813. Proxies every access request
 * to localhost:10000 and every accounting request to localhost:10001.
 * You can use TestClient to ask this TestProxy and TestServer
 * with the parameters 10000 and 10001 as the target server.
 * Uses "testing123" as the shared secret for the communication with the
 * target server (localhost:10000/localhost:10001) and "proxytest" as the
 * shared secret for the communication with connecting clients.
 */
public class TestProxy {

    private static final Logger logger = LogManager.getLogger();

    public static void main(String[] args) throws Exception {

        final NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup(4);
        final Dictionary dictionary = DefaultDictionary.INSTANCE;

        final Timer timer = new HashedWheelTimer();
        final Bootstrap bootstrap = new Bootstrap().channel(NioDatagramChannel.class).group(eventLoopGroup);

        final SecretProvider secretProvider = remote -> {
            if (remote.getPort() == 1812 || remote.getPort() == 1813)
                return "testing123";

            return remote.getAddress().getHostAddress().equals("127.0.0.1") ?
                    "proxytest" : null;
        };

        final BasicTimeoutHandler retryStrategy = new BasicTimeoutHandler(timer, 3, 1000);

        final RadiusClient radiusClient = new RadiusClient(
                bootstrap, new InetSocketAddress(0), retryStrategy, new ChannelInitializer<DatagramChannel>() {
            @Override
            protected void initChannel(DatagramChannel ch) {
                ch.pipeline().addLast(new ClientPacketCodec(dictionary), new PromiseAdapter());
            }
        });

        final ChannelInitializer<DatagramChannel> channelInitializer = new ChannelInitializer<DatagramChannel>() {
            @Override
            protected void initChannel(DatagramChannel ch) {
                ch.pipeline().addLast(new ServerPacketCodec(dictionary, secretProvider), new ProxyHandler(radiusClient) {
                    @Override
                    public Optional<RadiusEndpoint> getProxyServer(RadiusRequest request, RadiusEndpoint client) {
                        try {
                            InetAddress address = InetAddress.getByAddress(new byte[]{127, 0, 0, 1});
                            int port = request instanceof AccountingRequest ? 1813 : 1812;
                            return Optional.of(new RadiusEndpoint(new InetSocketAddress(address, port), "testing123"));
                        } catch (UnknownHostException e) {
                            return Optional.empty();
                        }
                    }
                });
            }
        };

        try (RadiusServer proxy = new RadiusServer(bootstrap,
                channelInitializer, channelInitializer,
                new InetSocketAddress(11812), new InetSocketAddress(11813))) {

            proxy.isReady().addListener(future1 -> {
                if (future1.isSuccess()) {
                    logger.info("Server started");
                } else {
                    logger.info("Failed to start server", future1.cause());
                    proxy.close();
                    eventLoopGroup.shutdownGracefully();
                }
            });

            System.in.read();
        }

        eventLoopGroup.shutdownGracefully();
    }
}
