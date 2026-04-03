package org.tinyradius.core.packet.request;

import io.netty.buffer.ByteBuf;
import org.junit.jupiter.api.Test;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.attribute.type.RadiusAttribute;
import org.tinyradius.core.dictionary.DefaultDictionary;
import org.tinyradius.core.dictionary.Dictionary;
import org.tinyradius.core.packet.RadiusPacket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.tinyradius.core.attribute.AttributeTypes.CHAP_PASSWORD;
import static org.tinyradius.core.attribute.AttributeTypes.USER_PASSWORD;
import static org.tinyradius.core.packet.PacketType.ACCESS_REQUEST;

class AccessRequestEdgeCaseTest {

    private final Dictionary dictionary = DefaultDictionary.INSTANCE;

    @Test
    void testLookupAuthTypeMultiple() throws RadiusPacketException {
        List<RadiusAttribute> attributes = new ArrayList<>();
        attributes.add(dictionary.createAttribute(-1, USER_PASSWORD, new byte[16]));
        attributes.add(dictionary.createAttribute(-1, CHAP_PASSWORD, new byte[16]));

        ByteBuf header = RadiusPacket.buildHeader(ACCESS_REQUEST, (byte) 1, null, attributes);

        AccessRequest request = AccessRequest.create(dictionary, header, attributes);
        assertTrue(request instanceof AccessRequestNoAuth);
    }

    @Test
    void testGenAuthWithNullAuthenticator() throws RadiusPacketException {
        ByteBuf header = RadiusPacket.buildHeader(ACCESS_REQUEST, (byte) 1, null, Collections.emptyList());

        AccessRequest request = new AccessRequestPap(dictionary, header, Collections.emptyList());
        byte[] auth = request.genAuth(null);
        assertNotNull(auth);
        assertEquals(16, auth.length);
        // Should be randomized since it was null
    }

    @Test
    void testDecodeRequestInvalidAuthLength() throws RadiusPacketException {
        List<RadiusAttribute> attributes = List.of(dictionary.createAttribute(-1, USER_PASSWORD, new byte[16]));
        ByteBuf header = RadiusPacket.buildHeader(ACCESS_REQUEST, (byte) 1, null, attributes);

        AccessRequest request = new AccessRequestPap(dictionary, header, attributes);
        RadiusPacketException exception = assertThrows(RadiusPacketException.class, () -> request.decodeRequest("secret"));
        assertTrue(exception.getMessage().contains("authenticator missing"), "Actual message: " + exception.getMessage());
    }

    @Test
    void testDecodeRequestWrongAuthLength() throws RadiusPacketException {
        List<RadiusAttribute> attributes = List.of(dictionary.createAttribute(-1, USER_PASSWORD, new byte[16]));
        ByteBuf header = RadiusPacket.buildHeader(ACCESS_REQUEST, (byte) 1, new byte[16], attributes);
        // overwrite some byte to make it non-zero so getAuthenticator returns it
        header.setByte(4, (byte) 1);

        AccessRequest request = new AccessRequest(dictionary, header, attributes) {
            @Override
            public byte[] getAuthenticator() {
                return new byte[10];
            }

            @Override
            public AccessRequest withAuthAttributes(byte[] auth, List<RadiusAttribute> attributes) {
                return null;
            }
        };

        RadiusPacketException exception = assertThrows(RadiusPacketException.class, () -> request.decodeRequest("secret"));
        assertTrue(exception.getMessage().contains("authenticator must be 16 octets"), "Actual message: " + exception.getMessage());
    }
}
