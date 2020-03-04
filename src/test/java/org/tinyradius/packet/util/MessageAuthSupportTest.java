package org.tinyradius.packet.util;

import org.junit.jupiter.api.Test;
import org.tinyradius.attribute.RadiusAttribute;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.packet.BaseRadiusPacket;
import org.tinyradius.util.RadiusPacketException;

import java.util.Collections;
import java.util.List;

class MessageAuthSupportTest {

    private static final Dictionary dictionary = DefaultDictionary.INSTANCE;

    @Test
    void decodeNoMessageAuth() throws RadiusPacketException {
        final String secret = "mySecret";
        final TestPacket testPacket = new TestPacket(dictionary, (byte) 1, (byte) 1, new byte[16], Collections.emptyList());

        testPacket.verifyMessageAuth(secret, null);
    }

    @Test
    void encodeVerify() throws RadiusPacketException {
        final String secret = "mySecret";
        final TestPacket testPacket = new TestPacket(dictionary, (byte) 1, (byte) 1, new byte[16], Collections.emptyList());

        final TestPacket encodedPacket = testPacket.encodeMessageAuth(secret, testPacket.getAuthenticator());
        encodedPacket.verifyMessageAuth(secret, testPacket.getAuthenticator());
    }

    static class TestPacket extends BaseRadiusPacket implements MessageAuthSupport<TestPacket> {

        public TestPacket(Dictionary dictionary, byte type, byte identifier, byte[] authenticator, List<RadiusAttribute> attributes) {
            super(dictionary, type, identifier, authenticator, attributes);
        }

        @Override
        public TestPacket copy() {
            return new TestPacket(getDictionary(), getType(), getId(), getAuthenticator(), getAttributes());
        }
    }
}