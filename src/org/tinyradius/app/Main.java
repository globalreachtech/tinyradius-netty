package org.tinyradius.app;

import io.netty.channel.ChannelException;
import io.netty.channel.ChannelFactory;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.dictionary.DictionaryParser;
import org.tinyradius.dictionary.WritableDictionary;
import org.tinyradius.netty.RadiusClient;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.util.RadiusEndpoint;

import java.io.FileInputStream;
import java.net.InetSocketAddress;

public class Main {


    public static void main(String[] args) throws Exception {

        NioEventLoopGroup eventGroup = new NioEventLoopGroup(1);

        Dictionary dictionary = DefaultDictionary
                .getDefaultDictionary();

        DictionaryParser.parseDictionary(new FileInputStream("dictionary/dictionary"),
                (WritableDictionary) dictionary);

        RadiusClient<NioDatagramChannel> client = new RadiusClient<NioDatagramChannel>(
                eventGroup, new NioDatagramChannelFactory());

        RadiusEndpoint endpoint = new RadiusEndpoint(
                new InetSocketAddress("127.0.0.1", 1812), "testing123");

        while (!eventGroup.isTerminated()) {

            try {
                client.authenticate("test", "password", endpoint, new RadiusClient.CallbackHandler() {
                    public void response(RadiusPacket response) {
                        System.out.println(response.toString());
                    }
                    public void error(Exception exception) {
                        exception.printStackTrace();
                    }
                });
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
