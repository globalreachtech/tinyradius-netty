package org.tinyradius;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tinyradius.client.RadiusClient;
import org.tinyradius.client.handler.ClientPacketCodec;
import org.tinyradius.client.handler.PromiseAdapter;
import org.tinyradius.client.timeout.BasicTimeoutHandler;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.packet.request.AccessRequestPap;
import org.tinyradius.packet.request.AccountingRequest;
import org.tinyradius.packet.response.RadiusResponse;
import org.tinyradius.util.RadiusEndpoint;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;


/**
 * TestClient shows how to send Radius Access-Request and Accounting-Request packets.
 */
public class TestClient {

    private static final Logger logger = LogManager.getLogger();

    /**
     * Radius command line client.
     *
     * @param args [host, sharedSecret, username, password]
     */
    public static void main(String[] args) {
        if (args.length != 4) {
            logger.info("Usage: TestClient hostName sharedSecret userName password");
            System.exit(1);
        }

        String host = args[0];
        String shared = args[1];
        String user = args[2];
        String pass = args[3];

        final NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup(4);

        final Dictionary dictionary = DefaultDictionary.INSTANCE;
        final Timer timer = new HashedWheelTimer();

        final Bootstrap bootstrap = new Bootstrap().group(eventLoopGroup).channel(NioDatagramChannel.class);

        RadiusClient rc = new RadiusClient(
                bootstrap, new InetSocketAddress(0), new BasicTimeoutHandler(timer), new ChannelInitializer<DatagramChannel>() {
            @Override
            protected void initChannel(DatagramChannel ch) {
                ch.pipeline().addLast(new ClientPacketCodec(dictionary), new PromiseAdapter());
            }
        });

        final RadiusEndpoint authEndpoint = new RadiusEndpoint(new InetSocketAddress(host, 1812), shared);
        final RadiusEndpoint acctEndpoint = new RadiusEndpoint(new InetSocketAddress(host, 1813), shared);

        // 1. Send Access-Request
        AccessRequestPap ar = new AccessRequestPap(dictionary, (byte) 1, null, Collections.emptyList())
                .withPassword(pass);
        ar.addAttribute("User-Name", user);
        ar.addAttribute("NAS-Identifier", "this.is.my.nas-identifier.de");
        ar.addAttribute("NAS-IP-Address", "192.168.0.100");
        ar.addAttribute("Service-Type", "Login-User");
        ar.addAttribute("WISPr-Redirection-URL", "http://www.sourceforge.net/");
        ar.addAttribute("WISPr-Location-ID", "net.sourceforge.ap1");

        logger.info("Packet before it is sent\n" + ar + "\n");
        RadiusResponse response = rc.communicate(ar, authEndpoint).syncUninterruptibly().getNow();
        logger.info("Packet after it was sent\n" + ar + "\n");
        logger.info("Response\n" + response + "\n");

        // 2. Send Accounting-Request
        AccountingRequest acc = new AccountingRequest(dictionary, (byte) 2, null, new ArrayList<>());
        acc.addAttribute("User-Name", "username");
        acc.addAttribute("Acct-Status-Type", "1");
        acc.addAttribute("Acct-Session-Id", "1234567890");
        acc.addAttribute("NAS-Identifier", "this.is.my.nas-identifier.de");
        acc.addAttribute("NAS-Port", "0");

        logger.info(acc + "\n");
        response = rc.communicate(acc, acctEndpoint).syncUninterruptibly().getNow();
        logger.info("Response: " + response);

        rc.close();
    }
}
