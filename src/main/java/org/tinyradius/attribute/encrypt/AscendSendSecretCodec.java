package org.tinyradius.attribute.encrypt;

import org.tinyradius.util.RadiusPacketException;

public class AscendSendSecretCodec implements AttributeCodec {

    @Override
    public byte[] encode(byte[] data, String sharedSecret, byte[] authenticator) {
        return data; // todo implement
    }

    @Override
    public byte[] decode(byte[] data, String sharedSecret, byte[] authenticator) throws RadiusPacketException {
        return data; // todo implement
    }
}
