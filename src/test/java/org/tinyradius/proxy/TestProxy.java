package org.tinyradius.proxy;

import io.netty.channel.ReflectiveChannelFactory;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.HashedWheelTimer;
import io.netty.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinyradius.client.RadiusClient;
import org.tinyradius.client.handler.ProxyStateClientHandler;
import org.tinyradius.client.retry.SimpleRetryStrategy;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.packet.AccountingRequest;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.proxy.handler.ProxyDeduplicatorHandler;
import org.tinyradius.proxy.handler.ProxyRequestHandler;
import org.tinyradius.util.RadiusEndpoint;
import org.tinyradius.util.SecretProvider;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

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
        final DefaultDictionary dictionary = DefaultDictionary.INSTANCE;
        ReflectiveChannelFactory<NioDatagramChannel> channelFactory = new ReflectiveChannelFactory<>(NioDatagramChannel.class);

        HashedWheelTimer timer = new HashedWheelTimer();

        final SecretProvider secretProvider = remote -> {
            if (remote.getPort() == 1812 || remote.getPort() == 1813)
                return "testing123";

            return remote.getAddress().getHostAddress().equals("127.0.0.1") ?
                    "proxytest" : null;
        };
        final ProxyStateClientHandler clientHandler = new ProxyStateClientHandler(dictionary, secretProvider);
        final SimpleRetryStrategy retryStrategy = new SimpleRetryStrategy(timer, 3, 1000);
        RadiusClient<NioDatagramChannel> radiusClient = new RadiusClient<>(eventLoopGroup, channelFactory, clientHandler, retryStrategy, null, 11814);

        final ProxyRequestHandler proxyRequestHandler = new ProxyRequestHandler(radiusClient) {
            @Override
            public RadiusEndpoint getProxyServer(RadiusPacket packet, RadiusEndpoint client) {
                try {
                    InetAddress address = InetAddress.getByAddress(new byte[]{127, 0, 0, 1});
                    int port = packet instanceof AccountingRequest ? 1813 : 1812;
                    return new RadiusEndpoint(new InetSocketAddress(address, port), "testing123");
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        };

        final ProxyDeduplicatorHandler proxyDeduplicatorHandler = new ProxyDeduplicatorHandler(proxyRequestHandler, timer, 30000);

        final RadiusProxy proxy = new RadiusProxy(
                eventLoopGroup,
                channelFactory,
                null,
                new ProxyHandlerAdapter(dictionary, proxyDeduplicatorHandler, timer, secretProvider),
                11812, 11813);


        Future<Void> future = proxy.start();
        future.addListener(future1 -> {
            if (future1.isSuccess()) {
                logger.info("Server started.");
            } else {
                logger.info("Failed to start server");
                future1.cause().printStackTrace();
            }
        });

        System.in.read();

        proxy.stop();

        eventLoopGroup.shutdownGracefully().awaitUninterruptibly();
    }
}
