package org.tinyradius.core.packet.request;

import net.jradius.util.RadiusUtils;
import org.junit.jupiter.api.Test;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.attribute.type.IntegerAttribute;
import org.tinyradius.core.dictionary.DefaultDictionary;
import org.tinyradius.core.dictionary.Dictionary;

import java.util.Collections;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;
import static org.tinyradius.core.packet.PacketType.ACCOUNTING_RESPONSE;

class GenericRequestTest {

    private static final Dictionary dictionary = DefaultDictionary.INSTANCE;

    private static final int HEADER_LENGTH = 20;
    private static final byte USER_NAME = 1;
    private static final byte ACCT_STATUS_TYPE = 40;

    @Test
    void encodeDecode() throws RadiusPacketException {
        final String sharedSecret = "sharedSecret1";
        final String username = "myUsername";

        final GenericRequest request = (GenericRequest) RadiusRequest.create(dictionary, ACCOUNTING_RESPONSE, (byte) 1, null, Collections.emptyList())
                .addAttribute(dictionary.createAttribute("User-Name", username));

        final RadiusPacketException e = assertThrows(RadiusPacketException.class, () -> request.decodeRequest(sharedSecret));
        assertTrue(e.getMessage().contains("authenticator missing"));

        final RadiusRequest encoded = request.encodeRequest(sharedSecret);
        assertNotNull(encoded.getAuthenticator());
        assertEquals(username, encoded.getAttribute("User-Name").get().getValueString());

        // idempotence check
        final RadiusRequest encoded2 = encoded.encodeRequest(sharedSecret);
        assertArrayEquals(encoded.toBytes(), encoded2.toBytes());

        final RadiusRequest decoded = encoded2.decodeRequest(sharedSecret);
        assertEquals(username, decoded.getAttribute("User-Name").get().getValueString());

        // idempotence check
        final RadiusRequest decoded2 = decoded.decodeRequest(sharedSecret);
        assertArrayEquals(decoded.toBytes(), decoded2.toBytes());
        assertEquals(username, decoded2.getAttribute("User-Name").get().getValueString());

        // sanity check
        assertArrayEquals(decoded.toBytes(), decoded.toByteBuffer().array());
    }


    @Test
    void encodeGenericRequest() throws RadiusPacketException {
        String sharedSecret = "sharedSecret";
        String user = "myUser1";
        GenericRequest request = (GenericRequest) RadiusRequest.create(dictionary, ACCOUNTING_RESPONSE, (byte) 1, null, Collections.emptyList())
                .addAttribute(dictionary.createAttribute(-1, 1, user.getBytes(UTF_8)))
                .addAttribute("Acct-Status-Type", "7");

        final byte[] attributeBytes = request.getAttributeByteBuf().copy().array();
        final int length = attributeBytes.length + HEADER_LENGTH;
        final byte[] expectedAuthenticator = RadiusUtils.makeRFC2866RequestAuthenticator(
                sharedSecret, ACCOUNTING_RESPONSE, (byte) 1, length, attributeBytes, 0, attributeBytes.length);

        final RadiusRequest encoded = request.encodeRequest(sharedSecret);

        assertEquals(request.getType(), encoded.getType());
        assertEquals(request.getId(), encoded.getId());
        assertEquals(request.getAttributes(), encoded.getAttributes());
        assertArrayEquals(expectedAuthenticator, encoded.getAuthenticator());
        assertEquals(user, encoded.getAttribute(USER_NAME).get().getValueString());
        assertEquals(7, ((IntegerAttribute) encoded.getAttribute(ACCT_STATUS_TYPE).get()).getValueInt());
        assertEquals(request.getAttribute(ACCT_STATUS_TYPE), encoded.getAttribute(ACCT_STATUS_TYPE));
    }
}
