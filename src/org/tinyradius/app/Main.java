package org.tinyradius.app;

import io.netty.channel.ChannelFactory;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.tinyradius.dictionary.*;
import org.tinyradius.netty.RadiusClient;
import org.tinyradius.netty.RadiusRequestContext;
import org.tinyradius.util.RadiusEndpoint;

import java.io.FileInputStream;
import java.net.InetSocketAddress;

public class Main {

    public static void main(String[] args) throws Exception {

        NioEventLoopGroup eventGroup = new NioEventLoopGroup(1);

        Dictionary dictionary = new MemoryDictionary();

        DictionaryParser.parseDictionary(new FileInputStream("dictionary/dictionary"),
                (WritableDictionary) dictionary);

        RadiusClient<NioDatagramChannel> client = new RadiusClient<NioDatagramChannel>(dictionary,
                eventGroup, new NioDatagramChannelFactory());

        RadiusEndpoint endpoint = new RadiusEndpoint(
                new InetSocketAddress("127.0.0.1", 1812), "testing123");

        while (!eventGroup.isTerminated()) {

            try {
                RadiusRequestContext context = client.authenticate("test", "password", endpoint);

                System.out.println("Queued request context " + context.toString());

            } catch (Exception e) {
                e.printStackTrace();
            }

            Thread.sleep(1000);
        }
    }

    private static class NioDatagramChannelFactory implements ChannelFactory<NioDatagramChannel> {
        public NioDatagramChannel newChannel() {
             return new NioDatagramChannel();
        }
    }
}
