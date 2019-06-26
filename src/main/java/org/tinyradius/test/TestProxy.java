package org.tinyradius.test;

import io.netty.channel.ReflectiveChannelFactory;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.HashedWheelTimer;
import io.netty.util.concurrent.Future;
import org.tinyradius.client.ProxyStateClientHandler;
import org.tinyradius.client.RadiusClient;
import org.tinyradius.dictionary.DictionaryParser;
import org.tinyradius.dictionary.MemoryDictionary;
import org.tinyradius.dictionary.WritableDictionary;
import org.tinyradius.packet.AccountingRequest;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.proxy.ProxyChannelInboundHandler;
import org.tinyradius.proxy.ProxyRequestHandler;
import org.tinyradius.proxy.RadiusProxy;
import org.tinyradius.util.RadiusEndpoint;
import org.tinyradius.util.SecretProvider;

import java.io.FileInputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

/**
 * TestProxy shows how to implement a proxy radius server. You can use
 * this class together with TestClient and TestServer.
 * <p>
 * Listens on localhost:1812 and localhost:1813. Proxies every access clientRequest
 * to localhost:10000 and every accounting clientRequest to localhost:10001.
 * You can use TestClient to ask this TestProxy and TestServer
 * with the parameters 10000 and 10001 as the target server.
 * Uses "testing123" as the shared secret for the communication with the
 * target server (localhost:10000/localhost:10001) and "proxytest" as the
 * shared secret for the communication with connecting clients.
 */
public class TestProxy {

    public static void main(String[] args) throws Exception {

        final NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup(4);

        WritableDictionary dictionary = new MemoryDictionary();
        DictionaryParser.parseDictionary(new FileInputStream("dictionary/dictionary"), dictionary);
        ReflectiveChannelFactory<NioDatagramChannel> channelFactory = new ReflectiveChannelFactory<>(NioDatagramChannel.class);

        HashedWheelTimer timer = new HashedWheelTimer();

        final SecretProvider secretProvider = remote -> {
            if (remote.getPort() == 1812 || remote.getPort() == 1813)
                return "testing123";

            return remote.getAddress().getHostAddress().equals("127.0.0.1") ?
                    "proxytest" : null;
        };
        final ProxyStateClientHandler clientHandler = new ProxyStateClientHandler(dictionary, timer, 3000, secretProvider);
        RadiusClient<NioDatagramChannel> radiusClient = new RadiusClient<>(eventLoopGroup, channelFactory, clientHandler, null, 11814);

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

        final RadiusProxy<NioDatagramChannel> proxy = new RadiusProxy<>(
                eventLoopGroup,
                channelFactory,
                null,
                new ProxyChannelInboundHandler(dictionary, proxyRequestHandler, timer, secretProvider),
                11812, 11813);


        Future<Void> future = proxy.start();
        future.addListener(future1 -> {
            if (future1.isSuccess()) {
                System.out.println("Server started.");
            } else {
                System.out.println("Failed to start server");
                future1.cause().printStackTrace();
            }
        });

        System.in.read();

        proxy.stop();

        eventLoopGroup.shutdownGracefully()
                .awaitUninterruptibly();
    }
}
