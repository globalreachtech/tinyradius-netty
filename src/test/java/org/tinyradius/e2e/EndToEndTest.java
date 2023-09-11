package org.tinyradius.e2e;

import org.junit.jupiter.api.Test;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.dictionary.DefaultDictionary;
import org.tinyradius.core.dictionary.Dictionary;
import org.tinyradius.core.packet.request.AccessRequest;
import org.tinyradius.core.packet.request.AccessRequestPap;
import org.tinyradius.core.packet.request.AccountingRequest;
import org.tinyradius.core.packet.request.RadiusRequest;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.tinyradius.core.packet.PacketType.ACCESS_REQUEST;
import static org.tinyradius.core.packet.PacketType.ACCOUNTING_REQUEST;

class EndToEndTest {

    private static final Dictionary dictionary = DefaultDictionary.INSTANCE;

    private static final int PROXY_ACCESS_PORT = 1812;
    private static final int PROXY_ACCT_PORT = 1813;

    private static final String PROXY_SECRET = "myProxySecret";
    private static final int ORIGIN_ACCESS_PORT = 11812;
    private static final int ORIGIN_ACCT_PORT = 11813;
    private static final String ORIGIN_SECRET = "myOriginSecret";

    private final Harness harness = new Harness();

    @Test
    void testAll() throws RadiusPacketException, IOException, InterruptedException {
        final Closeable origin = startOrigin();
        final Closeable proxy = startProxy();

        final AccessRequestPap ar = (AccessRequestPap)
                ((AccessRequest) RadiusRequest.create(dictionary, ACCESS_REQUEST, (byte) 1, null, Collections.emptyList()))
                        .withPapPassword("myPassword")
                        .addAttribute("User-Name", "myUser")
                        .addAttribute("NAS-Identifier", "this.is.my.nas-identifier.de")
                        .addAttribute("NAS-IP-Address", "192.168.0.100")
                        .addAttribute("Service-Type", "Login-User");


        final AccountingRequest acc = (AccountingRequest) RadiusRequest.create(dictionary, ACCOUNTING_REQUEST, (byte) 2, null, new ArrayList<>())
                .addAttribute("User-Name", "username")
                .addAttribute("Acct-Status-Type", "1")
                .addAttribute("Acct-Session-Id", "1234567890")
                .addAttribute("NAS-Identifier", "this.is.my.nas-identifier.de")
                .addAttribute("NAS-Port", "0");

        harness.testClient("localhost", PROXY_ACCESS_PORT, PROXY_ACCT_PORT, PROXY_SECRET, List.of(ar, acc));

        // TODO assert responses

        Thread.sleep(1000);
        origin.close();
        proxy.close();
    }

    @Test
    void testProxyStartup() throws IOException, InterruptedException {
        final Closeable proxy = startProxy();
        Thread.sleep(1000);
        proxy.close();
    }

    @Test
    void testOriginStartup() throws IOException, InterruptedException {
        final Closeable origin = startOrigin();
        Thread.sleep(1000);
        origin.close();
    }

    private Closeable startProxy() {
        return harness.startProxy(PROXY_ACCESS_PORT, PROXY_ACCT_PORT, PROXY_SECRET, ORIGIN_ACCESS_PORT, ORIGIN_ACCT_PORT, ORIGIN_SECRET);
    }

    private Closeable startOrigin() {
        return harness.startOrigin(ORIGIN_ACCESS_PORT, ORIGIN_ACCT_PORT, ORIGIN_SECRET);
    }
}
