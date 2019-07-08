package org.tinyradius.packet;

import org.junit.jupiter.api.Test;
import org.tinyradius.dictionary.DefaultDictionary;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.tinyradius.packet.PacketType.ACCOUNTING_REQUEST;

class AccountingRequestTest {

    @Test
    void encodeNewAccountingRequest() throws NoSuchAlgorithmException {
        //hashed authenticator
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.update((byte) ACCOUNTING_REQUEST); // code
        md5.update((byte) 1); // identifier
        md5.update((byte) 20); // length
        md5.update(new byte[16]); // 16 zero octets
        md5.update(new byte[0]); // attributes
        String sharedSecret = "sharedSecret";
        byte[] authenticator = md5.digest(sharedSecret.getBytes(UTF_8)); // shared secret

        AccountingRequest original = new AccountingRequest(DefaultDictionary.INSTANCE, 1, null);
        AccountingRequest encoded = original.encodeRequest(sharedSecret);
        assertEquals(original.getPacketType(), encoded.getPacketType());
        assertEquals(original.getPacketIdentifier(), encoded.getPacketIdentifier());
        assertEquals(original.getAttributes().size(), encoded.getAttributes().size());
        assertEquals(authenticator, encoded.getAuthenticator());
    }
}