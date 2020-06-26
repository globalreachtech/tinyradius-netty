package org.tinyradius.attribute.encrypt;

import org.tinyradius.util.RadiusPacketException;

/**
 * Attribute is encrypted as per Ascend's definitions for the Ascend-Send-Secret attribute
 * TODO
 */
public class AscendSendSecretCodec implements AttributeCodec {

    @Override
    public byte[] encode(byte[] data, String sharedSecret, byte[] authenticator) {
        return data;
    }

    @Override
    public byte[] decode(byte[] data, String sharedSecret, byte[] authenticator) throws RadiusPacketException {
        return data;
    }
}
