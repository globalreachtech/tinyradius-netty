package org.tinyradius.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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