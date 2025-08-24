package org.tinyradius.e2e;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import jakarta.annotation.Nonnull;
import lombok.extern.log4j.Log4j2;
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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;
import static org.tinyradius.core.packet.PacketType.ACCESS_REQUEST;

@Log4j2
public class Harness implements AutoCloseable {

    private final Dictionary dictionary = DefaultDictionary.INSTANCE;
    private final Timer timer = new HashedWheelTimer();
    private final FixedTimeoutHandler retryStrategy = new FixedTimeoutHandler(timer);

    public List<RadiusResponse> testClient(String host, int accessPort, int acctPort, String secret, List<RadiusRequest> requests) {
        var eventLoopGroup = new MultiThreadIoEventLoopGroup(4, NioIoHandler.newFactory());
        var bootstrap = new Bootstrap().group(eventLoopGroup).channel(NioDatagramChannel.class);

        try (var rc = new RadiusClient(
                bootstrap, new InetSocketAddress(0), new FixedTimeoutHandler(timer), new ChannelInitializer<DatagramChannel>() {
            @Override
            protected void initChannel(@Nonnull DatagramChannel ch) {
                ch.pipeline().addLast(
                        new ClientDatagramCodec(dictionary),
                        new PromiseAdapter(),
                        new BlacklistHandler(60_000, 3));
            }
        })) {
            var accessEndpoint = new RadiusEndpoint(new InetSocketAddress(host, accessPort), secret);
            var acctEndpoint = new RadiusEndpoint(new InetSocketAddress(host, acctPort), secret);
            return requests.stream().map(r -> {
                RadiusEndpoint endpoint = (r.getType() == ACCESS_REQUEST) ? accessEndpoint : acctEndpoint;
                log.info("Packet before it is sent\n{}\n", r);
                RadiusResponse response = rc.communicate(r, endpoint).syncUninterruptibly().getNow();
                log.info("Packet after it was sent\n{}\n", r);
                log.info("Response\n{}\n", response);
                return response;
            }).collect(toList());
        }
    }

    /***
     *
     * @param originAccessPort  port to listen for access requests
     * @param originAcctPort  port to listen for accounting requests
     * @param originSecret shared secret used by origin server
     * @return Closeable handler to trigger origin server shutdown
     */
    public RadiusServer startOrigin(int originAccessPort, int originAcctPort, String originSecret, Map<String, String> credentials) {
        var eventLoopGroup = new MultiThreadIoEventLoopGroup(4, NioIoHandler.newFactory());
        var bootstrap = new Bootstrap().channel(NioDatagramChannel.class).group(eventLoopGroup);

        var serverPacketCodec = new ServerPacketCodec(dictionary, x -> originSecret);

        var cachingHandlerAuth = new BasicCachingHandler(timer, 5000);
        var cachingHandlerAcct = new BasicCachingHandler(timer, 5000);

        var simpleAccessHandler = new SimpleAccessHandler(credentials);
        var simpleAccountingHandler = new SimpleAccountingHandler();

        var server = new RadiusServer(bootstrap,
                new ChannelInitializer<DatagramChannel>() {
                    @Override
                    protected void initChannel(@Nonnull DatagramChannel ch) {
                        ch.pipeline().addLast(serverPacketCodec, cachingHandlerAuth, simpleAccessHandler);
                    }
                },
                new ChannelInitializer<DatagramChannel>() {
                    @Override
                    protected void initChannel(@Nonnull DatagramChannel ch) {
                        ch.pipeline().addLast(serverPacketCodec, cachingHandlerAcct, simpleAccountingHandler);
                    }
                },
                new InetSocketAddress(originAccessPort), new InetSocketAddress(originAcctPort)) {

            @Override
            public void close() {
                super.close();
                eventLoopGroup.shutdownGracefully();
            }
        };

        server.isReady().addListener(future1 -> {
            if (future1.isSuccess()) {
                log.info("Origin server started");
            } else {
                log.info("Failed to start origin server", future1.cause());
                server.close();
                eventLoopGroup.shutdownGracefully();
            }
        });

        return server;
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
    public RadiusServer startProxy(int proxyAccessPort, int proxyAcctPort, String proxySecret, int originAccessPort, int originAcctPort, String originSecret) {
        var eventLoopGroup = new MultiThreadIoEventLoopGroup(4, NioIoHandler.newFactory());
        var bootstrap = new Bootstrap().channel(NioDatagramChannel.class).group(eventLoopGroup);

        var client = new RadiusClient(bootstrap, new InetSocketAddress(0), retryStrategy, new ChannelInitializer<DatagramChannel>() {
            @Override
            protected void initChannel(@Nonnull DatagramChannel ch) {
                ch.pipeline().addLast(new ClientDatagramCodec(dictionary), new PromiseAdapter());
            }
        });
        var simpleProxyHandler = new SimpleProxyHandler(client, originAccessPort, originAcctPort, originSecret);
        var channelInitializer = new ChannelInitializer<DatagramChannel>() {
            @Override
            protected void initChannel(DatagramChannel ch) {
                ch.pipeline().addLast(new ServerPacketCodec(dictionary, x -> proxySecret), simpleProxyHandler);
            }
        };

        var server = new RadiusServer(bootstrap, channelInitializer, channelInitializer,
                new InetSocketAddress(proxyAccessPort), new InetSocketAddress(proxyAcctPort)) {
            @Override
            public void close() {
                super.close();
                client.close();
                eventLoopGroup.shutdownGracefully();
            }
        };

        server.isReady().addListener(future1 -> {
            if (future1.isSuccess()) {
                log.info("Proxy server started");
            } else {
                log.info("Failed to start proxy server", future1.cause());
                server.close();
                throw new IOException("Failed to start proxy server: " + future1.cause().getMessage());
            }
        });

        return server;
    }

    @Override
    public void close() throws Exception {
        timer.stop();
    }
}
