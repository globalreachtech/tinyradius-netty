package org.tinyradius.packet;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.util.RadiusPacketException;

import java.util.Collections;

public class AccessRequestEapTest {

    private static final Dictionary dictionary = DefaultDictionary.INSTANCE;

    @Test
    @Disabled
    void encodeVerify() throws RadiusPacketException {
        final String plaintextPw = "myPassword";
        String sharedSecret = "sharedSecret1";
        final AccessRequestEap accessRequestEap = new AccessRequestEap(dictionary, (byte) 1, null, Collections.emptyList());

        final AccessRequest encoded = accessRequestEap.encodeRequest(sharedSecret);

        encoded.verifyRequest(sharedSecret);
        throw new RuntimeException();
    }
}
