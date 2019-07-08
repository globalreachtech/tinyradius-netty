package org.tinyradius.packet;

import net.jradius.util.RadiusUtils;
import org.junit.jupiter.api.Test;
import org.tinyradius.attribute.RadiusAttribute;
import org.tinyradius.dictionary.DefaultDictionary;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.tinyradius.packet.PacketType.ACCOUNTING_REQUEST;
import static org.tinyradius.packet.RadiusPacket.HEADER_LENGTH;

class AccountingRequestTest {

    @Test
    void encodeAccountingRequest() {

        String sharedSecret = "sharedSecret";

        AccountingRequest original = new AccountingRequest(DefaultDictionary.INSTANCE, 1, null);
        original.addAttribute(new RadiusAttribute(DefaultDictionary.INSTANCE, -1, 1, "asdf".getBytes(UTF_8)));

        final byte[] attributeBytes = original.getAttributeBytes();
        final int length = attributeBytes.length + HEADER_LENGTH;
        final byte[] expectedAuthenticator = RadiusUtils.makeRFC2866RequestAuthenticator(sharedSecret, (byte) ACCOUNTING_REQUEST, (byte) 1, length, attributeBytes, 0, attributeBytes.length);

        AccountingRequest encoded = original.encodeRequest(sharedSecret);
        assertEquals(original.getPacketType(), encoded.getPacketType());
        assertEquals(original.getPacketIdentifier(), encoded.getPacketIdentifier());
        assertEquals(original.getAttributes().size(), encoded.getAttributes().size());
        assertArrayEquals(expectedAuthenticator, encoded.getAuthenticator());
    }
}