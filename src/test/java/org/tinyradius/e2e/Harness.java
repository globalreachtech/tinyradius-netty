package org.tinyradius.e2e;

import io.netty.bootstrap.Bootstrap;
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
import org.tinyradius.core.packet.request.RadiusRequest;
import org.tinyradius.core.packet.response.RadiusResponse;
import org.tinyradius.e2e.handler.SimpleAccessHandler;
import org.tinyradius.e2e.handler.SimpleAccountingHandler;
import org.tinyradius.e2e.handler.SimpleProxyHandler;
import org.tinyradius.io.RadiusEndpoint;
import org.tinyradius.io.client.RadiusClient;
import org.tinyradius.io.client.handler.BlacklistHandler;
import org.tinyradius.io.client.handler.ClientDatagramCodec;
import org.tinyradius.io.client.handler.PromiseAdapter;
import org.tinyradius.io.client.timeout.FixedTimeoutHandler;
import org.tinyradius.io.server.RadiusServer;
import org.tinyradius.io.server.handler.BasicCachingHandler;
import org.tinyradius.io.server.handler.ServerPacketCodec;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.tinyradius.core.packet.PacketType.ACCESS_REQUEST;

public class Harness {

    private static final Logger logger = LogManager.getLogger();

    private final Dictionary dictionary = DefaultDictionary.INSTANCE;
    private final Timer timer = new HashedWheelTimer();
    private final FixedTimeoutHandler retryStrategy = new FixedTimeoutHandler(timer);

    public List<RadiusResponse> testClient(String host, int accessPort, int acctPort, String secret, List<RadiusRequest> requests) {
        NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup(4);
        Bootstrap bootstrap = new Bootstrap().group(eventLoopGroup).channel(NioDatagramChannel.class);

        try (RadiusClient rc = new RadiusClient(
                bootstrap, new InetSocketAddress(0), new FixedTimeoutHandler(timer), new ChannelInitializer<DatagramChannel>() {
            @Override
            protected void initChannel(DatagramChannel ch) {
                ch.pipeline().addLast(
                        new ClientDatagramCodec(dictionary),
                        new PromiseAdapter(),
                        new BlacklistHandler(60_000, 3));
            }
        })) {
            RadiusEndpoint accessEndpoint = new RadiusEndpoint(new InetSocketAddress(host, accessPort), secret);
            RadiusEndpoint acctEndpoint = new RadiusEndpoint(new InetSocketAddress(host, acctPort), secret);
            return requests.stream().map(r -> {
                RadiusEndpoint endpoint = (r.getType() == ACCESS_REQUEST) ? accessEndpoint : acctEndpoint;
                logger.info("Packet before it is sent\n{}\n", r);
                RadiusResponse response = rc.communicate(r, endpoint).syncUninterruptibly().getNow();
                logger.info("Packet after it was sent\n{}\n", r);
                logger.info("Response\n{}\n", response);
                return response;
            }).collect(Collectors.toList());
        }
    }

    /***
     *
     * @param originAccessPort  port to listen for access requests
     * @param originAcctPort  port to listen for accounting requests
     * @param originSecret shared secret used by origin server
     * @return Closeable handler to trigger origin server shutdown
     */
    public Closeable startOrigin(int originAccessPort, int originAcctPort, String originSecret, Map<String, String> credentials) {
        EventLoopGroup eventLoopGroup = new NioEventLoopGroup(4);
        Bootstrap bootstrap = new Bootstrap().channel(NioDatagramChannel.class).group(eventLoopGroup);

        ServerPacketCodec serverPacketCodec = new ServerPacketCodec(dictionary, x -> originSecret);

        BasicCachingHandler cachingHandlerAuth = new BasicCachingHandler(timer, 5000);
        BasicCachingHandler cachingHandlerAcct = new BasicCachingHandler(timer, 5000);

        SimpleAccessHandler simpleAccessHandler = new SimpleAccessHandler(credentials);
        SimpleAccountingHandler simpleAccountingHandler = new SimpleAccountingHandler();

        RadiusServer server = new RadiusServer(bootstrap,
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
                new InetSocketAddress(originAccessPort), new InetSocketAddress(originAcctPort));

        Runnable shutdown = () -> {
            server.close();
            eventLoopGroup.shutdownGracefully();
        };
        server.isReady().addListener(future1 -> {
            if (future1.isSuccess()) {
                logger.info("Origin server started");
            } else {
                logger.info("Failed to start origin server", future1.cause());
                server.close();
                eventLoopGroup.shutdownGracefully();
            }
        });


        return shutdown::run;
    }

    /**
     * Starts a proxy server, with a shared handler for access and accounting requests
     *
     * @param proxyAccessPort  port to listen for access requests
     * @param proxyAcctPort    port to listen for accounting requests
     * @param proxySecret      shared secret between client and proxy server
     * @param originAccessPort port to forward access requests
     * @param originAcctPort   port to forward accounting requests
     * @param originSecret     shared secret between proxy and origin server
     * @return Closeable handler to trigger proxy server shutdown
     */
    public Closeable startProxy(int proxyAccessPort, int proxyAcctPort, String proxySecret, int originAccessPort, int originAcctPort, String originSecret) {
        NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup(4);
        Bootstrap bootstrap = new Bootstrap().channel(NioDatagramChannel.class).group(eventLoopGroup);

        RadiusClient client = new RadiusClient(bootstrap, new InetSocketAddress(0), retryStrategy, new ChannelInitializer<DatagramChannel>() {
            @Override
            protected void initChannel(DatagramChannel ch) {
                ch.pipeline().addLast(new ClientDatagramCodec(dictionary), new PromiseAdapter());
            }
        });
        SimpleProxyHandler simpleProxyHandler = new SimpleProxyHandler(client, originAccessPort, originAcctPort, originSecret);
        ChannelInitializer<DatagramChannel> channelInitializer = new ChannelInitializer<>() {
            @Override
            protected void initChannel(DatagramChannel ch) {
                ch.pipeline().addLast(new ServerPacketCodec(dictionary, x -> proxySecret), simpleProxyHandler);
            }
        };

        RadiusServer server = new RadiusServer(bootstrap, channelInitializer, channelInitializer,
                new InetSocketAddress(proxyAccessPort), new InetSocketAddress(proxyAcctPort));

        Runnable shutdown = () -> {
            server.close();
            client.close();
            eventLoopGroup.shutdownGracefully();
        };

        server.isReady().addListener(future1 -> {
            if (future1.isSuccess()) {
                logger.info("Proxy server started");
            } else {
                logger.info("Failed to start proxy server", future1.cause());
                shutdown.run();
                throw new IOException("Failed to start proxy server: " + future1.cause().getMessage());
            }
        });

        return shutdown::run;
    }

}
