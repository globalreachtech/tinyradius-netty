package org.tinyradius.packet.request;

import net.jradius.util.RadiusUtils;
import org.junit.jupiter.api.Test;
import org.tinyradius.attribute.type.IntegerAttribute;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.util.RadiusPacketException;

import java.util.Collections;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.tinyradius.packet.util.PacketType.ACCOUNTING_REQUEST;

class AccountingRequestTest {

    private static final byte USER_NAME = 1;
    private static final int HEADER_LENGTH = 20;
    private static final byte ACCT_STATUS_TYPE = 40;
    private static final Dictionary dictionary = DefaultDictionary.INSTANCE;

    @Test
    void encodeAccountingRequest() throws RadiusPacketException {
        String sharedSecret = "sharedSecret";
        String user = "myUser1";
        RadiusRequest request = new AccountingRequest(dictionary, (byte) 1, null, Collections.emptyList())
                .addAttribute(dictionary.createAttribute(-1, (byte) 1, user.getBytes(UTF_8)))
                .addAttribute("Acct-Status-Type", "7");

        final byte[] attributeBytes = request.getAttributeBytes();
        final int length = attributeBytes.length + HEADER_LENGTH;
        final byte[] expectedAuthenticator = RadiusUtils.makeRFC2866RequestAuthenticator(
                sharedSecret, ACCOUNTING_REQUEST, (byte) 1, length, attributeBytes, 0, attributeBytes.length);

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