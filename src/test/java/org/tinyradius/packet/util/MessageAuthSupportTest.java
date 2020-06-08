package org.tinyradius.packet.util;

import net.jradius.packet.RadiusFormat;
import net.jradius.packet.RadiusPacket;
import net.jradius.packet.attribute.Attr_UnknownAttribute;
import net.jradius.util.MD5;
import net.jradius.util.MessageAuthenticator;
import org.junit.jupiter.api.Test;
import org.tinyradius.attribute.RadiusAttribute;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.packet.BaseRadiusPacket;
import org.tinyradius.packet.request.AccessRequestEap;
import org.tinyradius.packet.request.RadiusRequest;
import org.tinyradius.util.RadiusPacketException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.tinyradius.packet.util.MessageAuthSupport.MESSAGE_AUTHENTICATOR;

class MessageAuthSupportTest {

    private static final Dictionary dictionary = DefaultDictionary.INSTANCE;
    private static final String secret = "mySecret";

    @Test
    void decodeNoMessageAuth() throws RadiusPacketException {
        final TestPacket testPacket = new TestPacket(dictionary, (byte) 1, (byte) 1, new byte[16], Collections.emptyList());

        // should always pass if nothing to verify
        testPacket.verifyMessageAuth(secret, null);
    }

    @Test
    void selfEncodeVerify() throws RadiusPacketException {
        final TestPacket testPacket = new TestPacket(dictionary, (byte) 1, (byte) 1, new byte[16], Collections.emptyList());

        final TestPacket encodedPacket = testPacket.encodeMessageAuth(secret, testPacket.getAuthenticator());
        encodedPacket.verifyMessageAuth(secret, testPacket.getAuthenticator());
    }

    @Test
    void testEncode() throws Exception {
        // impl under test
        final RadiusRequest encodedRequest = new AccessRequestEap(dictionary, (byte) 1, null, Collections.emptyList())
                .encodeRequest(secret);
        final byte[] actualMsgAuth = encodedRequest.getAttributes().get(0).getValue();

        // jradius impl
        final net.jradius.packet.AccessRequest jradiusRequest = new net.jradius.packet.AccessRequest();
        jradiusRequest.setIdentifier(1);
        jradiusRequest.setAuthenticator(encodedRequest.getAuthenticator());
        jRadius_generateRequestMessageAuthenticator(jradiusRequest);

        final byte[] jradiusMsgAuth = jradiusRequest.getAttributes().get(MESSAGE_AUTHENTICATOR).getValue().getBytes();

        assertArrayEquals(jradiusMsgAuth, actualMsgAuth);
    }

    /**
     * Adapted from {@link MessageAuthenticator#generateRequestMessageAuthenticator}
     */
    private static void jRadius_generateRequestMessageAuthenticator(RadiusPacket request) throws IOException, InvalidKeyException, NoSuchAlgorithmException {
        byte[] hash = new byte[16];
        ByteBuffer buffer = ByteBuffer.allocate(4096);

        final Attr_UnknownAttribute attribute = new Attr_UnknownAttribute(MESSAGE_AUTHENTICATOR);
        attribute.setValue(hash);

        request.overwriteAttribute(attribute);

        RadiusFormat.getInstance().packPacket(request, secret, buffer, true);
        System.arraycopy(MD5.hmac_md5(buffer.array(), 0, buffer.position(), secret.getBytes()), 0, hash, 0, 16);
    }

    private static class TestPacket extends BaseRadiusPacket<TestPacket> implements MessageAuthSupport<TestPacket> {

        public TestPacket(Dictionary dictionary, byte type, byte identifier, byte[] authenticator, List<RadiusAttribute> attributes) {
            super(dictionary, type, identifier, authenticator, attributes);
        }

        @Override
        public TestPacket withAttributes(List<RadiusAttribute> attributes) {
            return new TestPacket(getDictionary(), getType(), getId(), getAuthenticator(), attributes);
        }
    }
}