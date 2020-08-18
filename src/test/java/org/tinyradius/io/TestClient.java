package org.tinyradius.io;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.dictionary.DefaultDictionary;
import org.tinyradius.core.dictionary.Dictionary;
import org.tinyradius.core.packet.request.AccessRequest;
import org.tinyradius.core.packet.request.AccessRequestPap;
import org.tinyradius.core.packet.request.AccountingRequest;
import org.tinyradius.core.packet.request.RadiusRequest;
import org.tinyradius.core.packet.response.RadiusResponse;
import org.tinyradius.io.client.RadiusClient;
import org.tinyradius.io.client.handler.BlacklistHandler;
import org.tinyradius.io.client.handler.ClientDatagramCodec;
import org.tinyradius.io.client.handler.PromiseAdapter;
import org.tinyradius.io.client.timeout.FixedTimeoutHandler;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;

import static org.tinyradius.core.packet.PacketType.ACCESS_REQUEST;
import static org.tinyradius.core.packet.PacketType.ACCOUNTING_REQUEST;


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
    public static void main(String[] args) throws RadiusPacketException {
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
                bootstrap, new InetSocketAddress(0), new FixedTimeoutHandler(timer), new ChannelInitializer<DatagramChannel>() {
            @Override
            protected void initChannel(DatagramChannel ch) {
                ch.pipeline().addLast(
                        new ClientDatagramCodec(dictionary),
                        new PromiseAdapter(),
                        new BlacklistHandler(60_000, 3));
            }
        });

        final RadiusEndpoint authEndpoint = new RadiusEndpoint(new InetSocketAddress(host, 1812), shared);
        final RadiusEndpoint acctEndpoint = new RadiusEndpoint(new InetSocketAddress(host, 1813), shared);

        // 1. Send Access-Request
        final AccessRequestPap ar = (AccessRequestPap)
                ((AccessRequest) RadiusRequest.create(dictionary, ACCESS_REQUEST, (byte) 1, null, Collections.emptyList()))
                .withPapPassword(pass)
                .addAttribute("User-Name", user)
                .addAttribute("NAS-Identifier", "this.is.my.nas-identifier.de")
                .addAttribute("NAS-IP-Address", "192.168.0.100")
                .addAttribute("Service-Type", "Login-User")
                .addAttribute("WISPr-Redirection-URL", "http://www.sourceforge.net/")
                .addAttribute("WISPr-Location-ID", "net.sourceforge.ap1");

        logger.info("Packet before it is sent\n" + ar + "\n");
        RadiusResponse response = rc.communicate(ar, authEndpoint).syncUninterruptibly().getNow();
        logger.info("Packet after it was sent\n" + ar + "\n");
        logger.info("Response\n" + response + "\n");

        // 2. Send Accounting-Request
        final AccountingRequest acc = (AccountingRequest) RadiusRequest.create(dictionary, ACCOUNTING_REQUEST, (byte) 2, null, new ArrayList<>())
                .addAttribute("User-Name", "username")
                .addAttribute("Acct-Status-Type", "1")
                .addAttribute("Acct-Session-Id", "1234567890")
                .addAttribute("NAS-Identifier", "this.is.my.nas-identifier.de")
                .addAttribute("NAS-Port", "0");

        logger.info(acc + "\n");
        response = rc.communicate(acc, acctEndpoint).syncUninterruptibly().getNow();
        logger.info("Response: " + response);

        rc.close();
    }
}
