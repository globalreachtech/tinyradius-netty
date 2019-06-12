/**
 * $Id: TestServer.java,v 1.6 2006/02/17 18:14:54 wuttke Exp $
 * Created on 08.04.2005
 *
 * @author Matthias Wuttke
 * @version $Revision: 1.6 $
 */
package com.globalreachtech.tinyradius.test.netty;

import com.globalreachtech.tinyradius.dictionary.DictionaryParser;
import com.globalreachtech.tinyradius.dictionary.MemoryDictionary;
import com.globalreachtech.tinyradius.dictionary.WritableDictionary;
import com.globalreachtech.tinyradius.netty.RadiusServer;
import com.globalreachtech.tinyradius.packet.AccessRequest;
import com.globalreachtech.tinyradius.packet.RadiusPacket;
import com.globalreachtech.tinyradius.util.RadiusException;
import io.netty.channel.ChannelFactory;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.HashedWheelTimer;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
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

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {

        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.INFO);

        WritableDictionary dictionary = new MemoryDictionary();
        DictionaryParser.parseDictionary(new FileInputStream("dictionary/dictionary"), dictionary);

        final NioEventLoopGroup eventGroup = new NioEventLoopGroup(4);

        final RadiusServer server = new RadiusServer(dictionary,
                eventGroup,
                new DefaultEventExecutorGroup(4),
                new NioDatagramChannelFactory(),
                new HashedWheelTimer()) {

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

        server.setAuthPort(11812);
        server.setAcctPort(11813);

        Future<NioDatagramChannel> future = server.start();
        future.addListener(future1 -> {
            if (future1.isSuccess()) {
                System.out.println("Server started");
            } else {
                System.out.println("Failed to start server: " + future1.cause());
                server.stop();
                eventGroup.shutdownGracefully();
            }
        });

        System.in.read();

        server.stop();

        eventGroup.shutdownGracefully()
                .awaitUninterruptibly();
    }

    private static class NioDatagramChannelFactory implements ChannelFactory<NioDatagramChannel> {
        public NioDatagramChannel newChannel() {
            return new NioDatagramChannel();
        }
    }
}
