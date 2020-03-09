package org.tinyradius.packet;

import net.jradius.util.RadiusUtils;
import org.junit.jupiter.api.Test;
import org.tinyradius.attribute.IntegerAttribute;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.util.RadiusPacketException;

import java.util.ArrayList;
import java.util.Collections;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.tinyradius.attribute.util.Attributes.create;
import static org.tinyradius.packet.AccessRequest.USER_NAME;
import static org.tinyradius.packet.RadiusPacket.HEADER_LENGTH;
import static org.tinyradius.packet.util.PacketType.ACCOUNTING_REQUEST;

class AccountingRequestTest {

    private static final byte ACCT_STATUS_TYPE = 40;
    private static final Dictionary dictionary = DefaultDictionary.INSTANCE;

    @Test
    void encodeAccountingRequest() throws RadiusPacketException {

        String sharedSecret = "sharedSecret";
        String user = "myUser1";
        AccountingRequest request = new AccountingRequest(dictionary, (byte) 1, null, Collections.emptyList());
        request.addAttribute(create(dictionary, -1, (byte) 1, user.getBytes(UTF_8)));

        final byte[] attributeBytes = request.getAttributeBytes();
        final int length = attributeBytes.length + HEADER_LENGTH;
        final byte[] expectedAuthenticator = RadiusUtils.makeRFC2866RequestAuthenticator(
                sharedSecret, ACCOUNTING_REQUEST, (byte) 1, length, attributeBytes, 0, attributeBytes.length);

        RadiusRequest encoded = request.encodeRequest(sharedSecret);

        assertEquals(request.getType(), encoded.getType());
        assertEquals(request.getId(), encoded.getId());
        assertEquals(request.getAttributes(), encoded.getAttributes());
        assertArrayEquals(expectedAuthenticator, encoded.getAuthenticator());
    }

    @Test
    void encodeNewAccountingRequestWithUsernameAndAcctStatus() throws RadiusPacketException {
        String sharedSecret = "sharedSecret";
        String user = "myUser1";
        AccountingRequest request = new AccountingRequest(dictionary, (byte) 1, null, new ArrayList<>());
        request.addAttribute("User-Name", user);
        request.addAttribute("Acct-Status-Type", "7");

        AccountingRequest encoded = (AccountingRequest) request.encodeRequest(sharedSecret);
        assertEquals(request.getAttributeString(USER_NAME), encoded.getAttributeString(USER_NAME));
        assertEquals(7, ((IntegerAttribute) encoded.getAttribute(ACCT_STATUS_TYPE)).getValueInt());
        assertEquals(request.getAttribute(ACCT_STATUS_TYPE), encoded.getAttribute(ACCT_STATUS_TYPE));
    }
}