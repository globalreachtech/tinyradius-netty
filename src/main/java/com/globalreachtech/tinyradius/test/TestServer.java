package com.globalreachtech.tinyradius.test;

import com.globalreachtech.tinyradius.server.RadiusServer;
import com.globalreachtech.tinyradius.dictionary.DictionaryParser;
import com.globalreachtech.tinyradius.dictionary.MemoryDictionary;
import com.globalreachtech.tinyradius.dictionary.WritableDictionary;
import com.globalreachtech.tinyradius.server.DefaultServerPacketManager;
import com.globalreachtech.tinyradius.packet.AccessRequest;
import com.globalreachtech.tinyradius.packet.RadiusPacket;
import com.globalreachtech.tinyradius.util.RadiusException;
import io.netty.channel.ReflectiveChannelFactory;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.HashedWheelTimer;
import io.netty.util.concurrent.Future;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.FileInputStream;
import java.net.InetSocketAddress;

/**
 * Test server which terminates after 30 s.
 * Knows only the client "localhost" with secret "testing123" and
 * the user "mw" with the password "test".
 */
public class TestServer {

    public static void main(String[] args) throws Exception {

        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.INFO);

        WritableDictionary dictionary = new MemoryDictionary(); // DefaultDictionary.INSTANCE
        DictionaryParser.parseDictionary(new FileInputStream("dictionary/dictionary"), dictionary);

        final NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup(4);

        final RadiusServer<NioDatagramChannel> server = new RadiusServer<NioDatagramChannel>(
                dictionary,
                eventLoopGroup,
                new ReflectiveChannelFactory<>(NioDatagramChannel.class),
                new DefaultServerPacketManager(new HashedWheelTimer(), 30000),
                null,
                11812, 11813) {

            // Authorize localhost/testing123
            public String getSharedSecret(InetSocketAddress client) {
                if (client.getAddress().getHostAddress().equals("127.0.0.1"))
                    return "testing123";
                else
                    return null;
            }

            // Authenticate mw
            public String getUserPassword(String userName) {
                if (userName.equals("test"))
                    return "password";
                else
                    return null;
            }

            // Adds an attribute to the Access-Accept packet
            public RadiusPacket accessRequestReceived(AccessRequest accessRequest, InetSocketAddress client)
                    throws RadiusException {
                System.out.println("Received Access-Request:\n" + accessRequest);
                RadiusPacket packet = super.accessRequestReceived(accessRequest, client);
                if (packet.getPacketType() == RadiusPacket.ACCESS_ACCEPT)
                    packet.addAttribute("Reply-Message", "Welcome " + accessRequest.getUserName() + "!");
                if (packet == null)
                    System.out.println("Ignore packet.");
                else
                    System.out.println("Answer:\n" + packet);
                return packet;
            }
        };

        final Future<Void> future = server.start();
        future.addListener(future1 -> {
            if (future1.isSuccess()) {
                System.out.println("Server started");
            } else {
                System.out.println("Failed to start server: " + future1.cause());
                server.stop();
                eventLoopGroup.shutdownGracefully();
            }
        });

        System.in.read();

        server.stop();

        eventLoopGroup.shutdownGracefully()
                .awaitUninterruptibly();
    }

}
