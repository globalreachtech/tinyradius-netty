package org.tinyradius.server.handler;

import org.junit.jupiter.api.Test;

class AuthHandlerTest {


    @Test
    void handlePacket() {


        final AuthHandler authHandler = new AuthHandler() {
            @Override
            public String getUserPassword(String userName) {
                return userName + "123";
            }
        };

        // todo

    }
}