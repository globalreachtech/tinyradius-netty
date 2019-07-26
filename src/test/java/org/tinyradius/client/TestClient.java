package org.tinyradius.client;

import io.netty.channel.ReflectiveChannelFactory;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.HashedWheelTimer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinyradius.client.handler.SimpleClientHandler;
import org.tinyradius.client.retry.SimpleRetryStrategy;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.packet.AccessRequest;
import org.tinyradius.packet.AccountingRequest;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.util.RadiusEndpoint;

import java.net.InetSocketAddress;

import static org.tinyradius.packet.RadiusPacketEncoder.nextPacketId;

/**
 * TestClient shows how to send Radius Access-Request and Accounting-Request packets.
 */
public class TestClient {

    private static final Logger logger = LoggerFactory.getLogger(TestClient.class);

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

        final DefaultDictionary dictionary = DefaultDictionary.INSTANCE;
        final HashedWheelTimer timer = new HashedWheelTimer();
        RadiusClient<NioDatagramChannel> rc = new RadiusClient<>(
                eventLoopGroup,
                new ReflectiveChannelFactory<>(NioDatagramChannel.class),
                new SimpleClientHandler(dictionary),
                new SimpleRetryStrategy(timer, 3, 1000),
                null, 0);
        rc.startChannel().syncUninterruptibly();

        final RadiusEndpoint authEndpoint = new RadiusEndpoint(new InetSocketAddress(host, 1812), shared);
        final RadiusEndpoint acctEndpoint = new RadiusEndpoint(new InetSocketAddress(host, 1813), shared);

        // 1. Send Access-Request
        AccessRequest ar = new AccessRequest(dictionary, nextPacketId(), null, user, pass);
        ar.setAuthProtocol(AccessRequest.AUTH_PAP); // or AUTH_CHAP
        ar.addAttribute("NAS-Identifier", "this.is.my.nas-identifier.de");
        ar.addAttribute("NAS-IP-Address", "192.168.0.100");
        ar.addAttribute("Service-Type", "Login-User");
        ar.addAttribute("WISPr-Redirection-URL", "http://www.sourceforge.net/");
        ar.addAttribute("WISPr-Location-ID", "net.sourceforge.ap1");

        logger.info("Packet before it is sent\n" + ar + "\n");
        RadiusPacket response = rc.communicate(ar, authEndpoint).syncUninterruptibly().getNow();
        logger.info("Packet after it was sent\n" + ar + "\n");
        logger.info("Response\n" + response + "\n");

        // 2. Send Accounting-Request
        AccountingRequest acc = new AccountingRequest(dictionary, nextPacketId(), null, "mw", AccountingRequest.ACCT_STATUS_TYPE_START);
        acc.addAttribute("Acct-Session-Id", "1234567890");
        acc.addAttribute("NAS-Identifier", "this.is.my.nas-identifier.de");
        acc.addAttribute("NAS-Port", "0");

        logger.info(acc + "\n");
        response = rc.communicate(acc, acctEndpoint).syncUninterruptibly().getNow();
        logger.info("Response: " + response);

        rc.stop();
    }

}
