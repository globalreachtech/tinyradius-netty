package org.tinyradius.e2e;

import org.junit.jupiter.api.Test;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.dictionary.DefaultDictionary;
import org.tinyradius.core.dictionary.Dictionary;
import org.tinyradius.core.packet.request.AccessRequest;
import org.tinyradius.core.packet.request.RadiusRequest;
import org.tinyradius.core.packet.response.RadiusResponse;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.tinyradius.core.packet.PacketType.*;

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
        String username = "user1";
        String pw = "pw1";
        Closeable origin = startOrigin(Map.of(username, pw));
        Closeable proxy = startProxy();

        RadiusRequest r1 = ((AccessRequest) RadiusRequest.create(dictionary, ACCESS_REQUEST, (byte) 1, null, List.of()))
                .withPapPassword(pw)
                .addAttribute("User-Name", username);

        RadiusRequest r2 = RadiusRequest.create(dictionary, ACCOUNTING_REQUEST, (byte) 2, null, List.of())
                .addAttribute("Acct-Status-Type", "1");

        RadiusRequest r3 = ((AccessRequest) RadiusRequest.create(dictionary, ACCESS_REQUEST, (byte) 1, null, List.of()))
                .withPapPassword("badPw")
                .addAttribute("User-Name", username);


        List<RadiusResponse> responses = harness.testClient("localhost", PROXY_ACCESS_PORT, PROXY_ACCT_PORT, PROXY_SECRET,
                List.of(r1, r2, r3));

        assertEquals(responses.get(0).getType(), ACCESS_ACCEPT);
        assertEquals(responses.get(1).getType(), ACCOUNTING_RESPONSE);
        assertEquals(responses.get(2).getType(), ACCESS_REJECT);

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
        final Closeable origin = startOrigin(Map.of());
        Thread.sleep(1000);
        origin.close();
    }

    private Closeable startProxy() {
        return harness.startProxy(PROXY_ACCESS_PORT, PROXY_ACCT_PORT, PROXY_SECRET, ORIGIN_ACCESS_PORT, ORIGIN_ACCT_PORT, ORIGIN_SECRET);
    }

    private Closeable startOrigin(Map<String, String> credentials) {
        return harness.startOrigin(ORIGIN_ACCESS_PORT, ORIGIN_ACCT_PORT, ORIGIN_SECRET, credentials);
    }
}
