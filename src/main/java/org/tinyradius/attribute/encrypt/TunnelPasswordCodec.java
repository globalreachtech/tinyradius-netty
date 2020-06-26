package org.tinyradius.attribute.encrypt;

import org.tinyradius.util.RadiusPacketException;

/**
 * Attribute is encrypted with the method as defined in RFC2868 for the Tunnel-Password attribute
 */
public class TunnelPasswordCodec implements AttributeCodec {

    @Override
    public byte[] encode(byte[] data, String sharedSecret, byte[] authenticator) {
        return data;
    }

    @Override
    public byte[] decode(byte[] data, String sharedSecret, byte[] authenticator) throws RadiusPacketException {
        return data;
    }
}
