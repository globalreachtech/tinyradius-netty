package com.globalreachtech.tinyradius.test.netty;

import com.globalreachtech.tinyradius.dictionary.Dictionary;
import com.globalreachtech.tinyradius.dictionary.DictionaryParser;
import com.globalreachtech.tinyradius.dictionary.MemoryDictionary;
import com.globalreachtech.tinyradius.dictionary.WritableDictionary;
import com.globalreachtech.tinyradius.netty.RadiusProxy;
import com.globalreachtech.tinyradius.netty.RadiusServer;
import com.globalreachtech.tinyradius.packet.AccountingRequest;
import com.globalreachtech.tinyradius.packet.RadiusPacket;
import com.globalreachtech.tinyradius.util.RadiusEndpoint;
import io.netty.channel.ChannelFactory;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ReflectiveChannelFactory;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.concurrent.Future;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.FileInputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

/**
 * Test proxy server.
 * Listens on localhost:1812 and localhost:1813. Proxies every access clientRequest
 * to localhost:10000 and every accounting clientRequest to localhost:10001.
 * You can use TestClient to ask this TestProxy and TestServer
 * with the parameters 10000 and 10001 as the target server.
 * Uses "testing123" as the shared secret for the communication with the
 * target server (localhost:10000/localhost:10001) and "proxytest" as the
 * shared secret for the communication with connecting clients.
 */
public class TestProxy<T extends DatagramChannel> extends RadiusProxy<T> {

    public TestProxy(EventLoopGroup eventGroup, ChannelFactory<T> factory) {
        super(eventGroup, factory);
    }

    public TestProxy(Dictionary dictionary, EventLoopGroup eventGroup, ChannelFactory<T> factory) {
        super(dictionary, eventGroup, factory);
    }

    public TestProxy(Dictionary dictionary, EventLoopGroup eventGroup, ChannelFactory<T> factory, int authPort, int acctPort, int proxyPort) {
        super(dictionary, eventGroup, factory, authPort, acctPort, proxyPort);
    }

    public RadiusEndpoint getProxyServer(RadiusPacket packet,
                                         RadiusEndpoint client) {
        // always proxy
        try {
            InetAddress address = InetAddress.getByAddress(new byte[]{127, 0, 0, 1});
            int port = packet instanceof AccountingRequest ? 1813 : 1812;
            return new RadiusEndpoint(new InetSocketAddress(address, port), "testing123");
        } catch (UnknownHostException uhe) {
            uhe.printStackTrace();
            return null;
        }
    }

    public String getSharedSecret(InetSocketAddress client) {
        if (client.getPort() == 1812 || client.getPort() == 1813)
            return "testing123";

        if (client.getAddress().getHostAddress().equals("127.0.0.1"))
            return "proxytest";

        return null;
    }

    public String getUserPassword(String userName) {
        // not used because every clientRequest is proxied
        return null;
    }

    public static void main(String[] args) throws Exception {

        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.INFO);

        final NioEventLoopGroup eventGroup = new NioEventLoopGroup(4);

        WritableDictionary dictionary = new MemoryDictionary();
        DictionaryParser.parseDictionary(new FileInputStream("dictionary/dictionary"), dictionary);

        final TestProxy<NioDatagramChannel> proxy = new TestProxy<>(
                dictionary,
                eventGroup,
                new ReflectiveChannelFactory<>(NioDatagramChannel.class), 11812, 11813, 11814);

        //                new DefaultEventExecutorGroup(4),

        Future<RadiusServer<NioDatagramChannel>> future = proxy.start();
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

        eventGroup.shutdownGracefully()
                .awaitUninterruptibly();
    }
}
