package org.tinyradius.e2e;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.dictionary.DefaultDictionary;
import org.tinyradius.core.dictionary.Dictionary;
import org.tinyradius.core.packet.request.AccessRequest;
import org.tinyradius.core.packet.request.RadiusRequest;
import org.tinyradius.core.packet.response.RadiusResponse;
import org.tinyradius.io.server.RadiusServer;

import java.util.List;
import java.util.Map;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.tinyradius.core.packet.PacketType.*;

@Execution(ExecutionMode.SAME_THREAD)
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
    void testAll() throws RadiusPacketException {
        var username = "user1";
        var pw = "pw1";
        try (var origin = startOrigin(Map.of(username, pw));
             var proxy = startProxy()) {
            await().until(() -> proxy.isReady().isSuccess() && origin.isReady().isSuccess());

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

            assertEquals(ACCESS_ACCEPT, responses.get(0).getType());
            assertEquals(ACCOUNTING_RESPONSE, responses.get(1).getType());
            assertEquals(ACCESS_REJECT, responses.get(2).getType());
        }
    }

    @Test
    void testProxyStartup() {
        try (var proxy = startProxy()) {
            await().until(() -> proxy.isReady().isSuccess());
        }
    }

    @Test
    void testOriginStartup() {
        try (var origin = startOrigin(Map.of())) {
            await().until(() -> origin.isReady().isSuccess());
        }
    }

    private RadiusServer startProxy() {
        return harness.startProxy(PROXY_ACCESS_PORT, PROXY_ACCT_PORT, PROXY_SECRET, ORIGIN_ACCESS_PORT, ORIGIN_ACCT_PORT, ORIGIN_SECRET);
    }

    private RadiusServer startOrigin(Map<String, String> credentials) {
        return harness.startOrigin(ORIGIN_ACCESS_PORT, ORIGIN_ACCT_PORT, ORIGIN_SECRET, credentials);
    }
}
