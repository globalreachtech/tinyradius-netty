package org.tinyradius.core.packet.util;

import io.netty.buffer.ByteBuf;
import net.jradius.packet.RadiusFormat;
import net.jradius.packet.RadiusPacket;
import net.jradius.packet.attribute.Attr_UnknownAttribute;
import net.jradius.util.MD5;
import net.jradius.util.MessageAuthenticator;
import org.junit.jupiter.api.Test;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.attribute.type.RadiusAttribute;
import org.tinyradius.core.dictionary.DefaultDictionary;
import org.tinyradius.core.dictionary.Dictionary;
import org.tinyradius.core.packet.BaseRadiusPacket;
import org.tinyradius.core.packet.request.AccessRequestNoAuth;
import org.tinyradius.core.packet.request.RadiusRequest;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.tinyradius.core.attribute.AttributeTypes.MESSAGE_AUTHENTICATOR;
import static org.tinyradius.core.packet.PacketType.ACCESS_REQUEST;
import static org.tinyradius.core.packet.RadiusPacket.buildHeader;

class MessageAuthSupportTest {

    private static final Dictionary dictionary = DefaultDictionary.INSTANCE;
    private static final String secret = "mySecret";

    @Test
    void decodeNoMessageAuth() throws RadiusPacketException {
        final TestPacket testPacket = new TestPacket((byte) 1, (byte) 1, new byte[16], Collections.emptyList());

        // should always pass if nothing to verify
        testPacket.verifyMessageAuth(secret, null);
    }

    @Test
    void selfEncodeVerify() throws RadiusPacketException {
        final byte[] auth = new byte[16];
        auth[0] = 1; // set auth to non-zeros
        final TestPacket testPacket = new TestPacket((byte) 1, (byte) 1, auth, Collections.emptyList());

        final TestPacket encodedPacket = testPacket.encodeMessageAuth(secret, testPacket.getAuthenticator());
        encodedPacket.verifyMessageAuth(secret, testPacket.getAuthenticator());
    }

    @Test
    void testEncode() throws Exception {
        // impl under test
        final AccessRequestNoAuth encodedRequest = (AccessRequestNoAuth) RadiusRequest.create(dictionary, ACCESS_REQUEST, (byte) 1, null, Collections.emptyList())
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
        final byte[] hash = new byte[16];
        final ByteBuffer buffer = ByteBuffer.allocate(4096);

        final Attr_UnknownAttribute attribute = new Attr_UnknownAttribute(MESSAGE_AUTHENTICATOR);
        attribute.setValue(hash);

        request.overwriteAttribute(attribute);

        RadiusFormat.getInstance().packPacket(request, secret, buffer, true);
        System.arraycopy(MD5.hmac_md5(buffer.array(), 0, buffer.position(), secret.getBytes()), 0, hash, 0, 16);
    }

    private static class TestPacket extends BaseRadiusPacket<TestPacket> implements MessageAuthSupport<TestPacket> {

        private TestPacket(byte type, byte id, byte[] authenticator, List<RadiusAttribute> attributes) throws RadiusPacketException {
            super(MessageAuthSupportTest.dictionary, buildHeader(type, id, authenticator, attributes), attributes);
        }

        @Override
        protected TestPacket with(ByteBuf header, List<RadiusAttribute> attributes) throws RadiusPacketException {
            return new TestPacket(header.getByte(0), header.getByte(1), header.slice(4, 16).copy().array(), attributes);
        }
    }
}