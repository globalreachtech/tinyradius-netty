package org.tinyradius.packet;

import org.junit.jupiter.api.Test;
import org.tinyradius.dictionary.DefaultDictionary;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AccountingRequestPacketTest {

    @Test
    void encodeNewAccountingRequest() throws NoSuchAlgorithmException {
        //hashed authenticator
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.update((byte) 4); // code
        md5.update((byte) 1); // identifier
        md5.update((byte) 20); // length
        md5.update(new byte[16]); // 16 zero octets
        md5.update(new byte[0]); // attributes
        String sharedSecret = "sharedSecret";
        byte[] authenticator = md5.digest(sharedSecret.getBytes(UTF_8)); // shared secret

        AccountingRequest accountingRequest = new AccountingRequest(DefaultDictionary.INSTANCE, 1, authenticator);
        AccountingRequest accountingRequest1 = accountingRequest.encodeRequest(sharedSecret);
        assertEquals(accountingRequest.getPacketType(), accountingRequest1.getPacketType());
        assertEquals(accountingRequest.getPacketIdentifier(), accountingRequest1.getPacketIdentifier());
        assertEquals(accountingRequest.getAttributes().size(), accountingRequest1.getAttributes().size());
    }
}