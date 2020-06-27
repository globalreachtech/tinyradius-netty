package org.tinyradius.attribute.encrypt;

import org.tinyradius.util.RadiusPacketException;

/**
 * Attribute is encrypted as per Ascend's definitions for the Ascend-Send-Secret attribute
 * TODO
 */
public class AscendSendSecretCodec extends AbstractCodec {

    @Override
    public byte[] encode(byte[] data, String sharedSecret, byte[] requestAuth) {
        return data;
    }

    @Override
    public byte[] decode(byte[] data, String sharedSecret, byte[] requestAuth) {
        return data;
    }
}
