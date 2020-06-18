package org.tinyradius.client.handler;

import org.junit.jupiter.api.Test;

class BlacklistHandlerTest {

    private final BlacklistHandler handler = new BlacklistHandler(5000, 2);

    @Test
    void noBlacklist() {
    }

    @Test
    void blacklistEndByTimeout() {

    }

    @Test
    void blacklistEndBySuccessfulResponse() {
    }

}