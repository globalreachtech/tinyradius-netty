package com.globalreachtech.tinyradius.test;

import com.globalreachtech.tinyradius.dictionary.DictionaryParser;
import com.globalreachtech.tinyradius.dictionary.MemoryDictionary;
import com.globalreachtech.tinyradius.dictionary.WritableDictionary;
import com.globalreachtech.tinyradius.netty.ClientPacketManager;
import com.globalreachtech.tinyradius.RadiusClient;
import com.globalreachtech.tinyradius.packet.AccessRequest;
import com.globalreachtech.tinyradius.packet.AccountingRequest;
import com.globalreachtech.tinyradius.packet.RadiusPacket;
import com.globalreachtech.tinyradius.util.RadiusEndpoint;
import io.netty.channel.ReflectiveChannelFactory;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.HashedWheelTimer;

import java.io.FileInputStream;
import java.net.InetSocketAddress;

/**
 * Simple Radius command-line client.
 */
public class TestClient {

    /**
     * Radius command line client.
     * <br/>Usage: TestClient <i>hostName sharedSecret userName password</i>
     *
     * @param args arguments
     * @throws Exception
     */
    public static void main(String[] args)
            throws Exception {
        if (args.length != 4) {
            System.out.println("Usage: TestClient hostName sharedSecret userName password");
            System.exit(1);
        }

        String host = args[0];
        String shared = args[1];
        String user = args[2];
        String pass = args[3];

        final NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup(4);

        WritableDictionary dictionary = new MemoryDictionary();
        DictionaryParser.parseDictionary(new FileInputStream("dictionary/dictionary"), dictionary);


        RadiusClient<NioDatagramChannel> rc = new RadiusClient<>(
                eventLoopGroup,
                new ReflectiveChannelFactory<>(NioDatagramChannel.class),
                new ClientPacketManager(new HashedWheelTimer(), dictionary, 30000)
        );

        final RadiusEndpoint authEndpoint = new RadiusEndpoint(new InetSocketAddress(host, 1812), shared);
        final RadiusEndpoint acctEndpoint = new RadiusEndpoint(new InetSocketAddress(host, 1813), shared);

        // 1. Send Access-Request
        AccessRequest ar = new AccessRequest(user, pass);
        ar.setAuthProtocol(AccessRequest.AUTH_PAP); // or AUTH_CHAP
        ar.addAttribute("NAS-Identifier", "this.is.my.nas-identifier.de");
        ar.addAttribute("NAS-IP-Address", "192.168.0.100");
        ar.addAttribute("Service-Type", "Login-User");
        ar.addAttribute("WISPr-Redirection-URL", "http://www.sourceforge.net/");
        ar.addAttribute("WISPr-Location-ID", "net.sourceforge.ap1");

        System.out.println("Packet before it is sent\n" + ar + "\n");
        RadiusPacket response = rc.communicate(ar, authEndpoint, 3).syncUninterruptibly().getNow();
        System.out.println("Packet after it was sent\n" + ar + "\n");
        System.out.println("Response\n" + response + "\n");

        // 2. Send Accounting-Request
        AccountingRequest acc = new AccountingRequest("mw", AccountingRequest.ACCT_STATUS_TYPE_START);
        acc.addAttribute("Acct-Session-Id", "1234567890");
        acc.addAttribute("NAS-Identifier", "this.is.my.nas-identifier.de");
        acc.addAttribute("NAS-Port", "0");

        System.out.println(acc + "\n");
        response = rc.communicate(acc, acctEndpoint, 3).syncUninterruptibly().getNow();
        System.out.println("Response: " + response);

        rc.close();
    }

}
