package org.tinyradius;

import io.netty.channel.ReflectiveChannelFactory;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinyradius.client.RadiusClient;
import org.tinyradius.client.handler.PromiseAdapter;
import org.tinyradius.client.retry.BasicTimeoutHandler;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.packet.AccountingRequest;
import org.tinyradius.packet.PacketEncoder;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.server.HandlerAdapter;
import org.tinyradius.server.RadiusServer;
import org.tinyradius.server.SecretProvider;
import org.tinyradius.server.handler.DeduplicatingHandler;
import org.tinyradius.server.handler.ProxyHandler;
import org.tinyradius.server.handler.ProxyRequestHandler;
import org.tinyradius.server.handler.ResponseContext;
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

    private static final Logger logger = LoggerFactory.getLogger(TestProxy.class);

    public static void main(String[] args) throws Exception {

        final NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup(4);
        final Dictionary dictionary = DefaultDictionary.INSTANCE;
        final PacketEncoder packetEncoder = new PacketEncoder(dictionary);

        final ReflectiveChannelFactory<NioDatagramChannel> channelFactory = new ReflectiveChannelFactory<>(NioDatagramChannel.class);
        final Timer timer = new HashedWheelTimer();

        final SecretProvider secretProvider = remote -> {
            if (remote.getPort() == 1812 || remote.getPort() == 1813)
                return "testing123";

            return remote.getAddress().getHostAddress().equals("127.0.0.1") ?
                    "proxytest" : null;
        };
        final PromiseAdapter clientHandler = new PromiseAdapter(packetEncoder);
        final BasicTimeoutHandler retryStrategy = new BasicTimeoutHandler(timer, 3, 1000);
        RadiusClient radiusClient = new RadiusClient(
                eventLoopGroup, timer, timeoutHandler, channelFactory, clientHandler, retryStrategy, new InetSocketAddress(11814));

        final ProxyHandler proxyRequestHandler = new ProxyHandler(radiusClient) {
            @Override
            public Optional<RadiusEndpoint> getProxyServer(RadiusPacket packet, RadiusEndpoint client) {
                try {
                    InetAddress address = InetAddress.getByAddress(new byte[]{127, 0, 0, 1});
                    int port = packet instanceof AccountingRequest ? 1813 : 1812;
                    return Optional.of(new RadiusEndpoint(new InetSocketAddress(address, port), "testing123"));
                } catch (UnknownHostException e) {
                    return Optional.empty();
                }
            }
        };


        final RadiusServer proxy = new RadiusServer(
                eventLoopGroup,
                channelFactory,
                new HandlerAdapter<>(secretProvider, RadiusPacket.class),
                new HandlerAdapter<>(secretProvider, RadiusPacket.class),
                new InetSocketAddress(11812), new InetSocketAddress(11813));

        proxy.start().addListener(future1 -> {
            if (future1.isSuccess()) {
                logger.info("Server started.");
            } else {
                logger.info("Failed to start server");
                future1.cause().printStackTrace();
            }
        });

        System.in.read();

        proxy.close();

        eventLoopGroup.shutdownGracefully().awaitUninterruptibly();
    }
}
