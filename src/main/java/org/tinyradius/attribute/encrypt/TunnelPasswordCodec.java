package org.tinyradius.attribute.encrypt;

import org.tinyradius.util.RadiusPacketException;

import java.nio.charset.StandardCharsets;

/**
 * Attribute is encrypted with the method as defined in RFC2868 for the Tunnel-Password attribute
 */
public class TunnelPasswordCodec extends AbstractCodec {

    @Override
    public byte[] encode(byte[] data, String sharedSecret, byte[] requestAuth) {

        return encryptData(data, requestAuth, new byte[]{}, sharedSecret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public byte[] decode(byte[] data, String sharedSecret, byte[] requestAuth) throws RadiusPacketException {
        return decodeData(data, requestAuth, sharedSecret.getBytes(StandardCharsets.UTF_8));
    }

    private byte[] encryptData(byte[] value, byte[] auth, byte[] salt, byte[] secret) {

        int length = ((value.length + 1 + 16) / 16) * 16;

        byte[] P = new byte[length];
        byte[] C = new byte[auth.length + salt.length];

        P[0] = (byte) value.length;

        System.arraycopy(value, 0, P, 1, value.length);
        System.arraycopy(auth, 0, C, 0, auth.length);
        System.arraycopy(salt, 0, C, auth.length, salt.length);

        byte[] result = new byte[P.length + salt.length];

        for (int i = 0; i < P.length; i += 16) {
            C = md5(secret, C);
            C = xor16(P, i, C);
            System.arraycopy(C, 0, result, i + salt.length, 16);
        }

        System.arraycopy(salt, 0, result, 0, salt.length);

        return result;
    }

    private byte[] decodeData(byte[] value, byte[] auth, byte[] secret) {

        byte[] P = new byte[value.length - 2];
        byte[] C = new byte[auth.length + 2];

        System.arraycopy(value, 2, P, 0, P.length);
        System.arraycopy(auth, 0, C, 0, auth.length);
        System.arraycopy(value, 0, C, auth.length, 2);

        byte[] tmp = new byte[P.length];

        for (int i = 0; i < P.length; i += 16) {
            C = md5(secret, C);
            C = xor16(P, i, C);
            System.arraycopy(C, 0, tmp, i, 16);
            System.arraycopy(P, i, C, 0, 16);
        }

        byte[] result = new byte[tmp[0]];

        System.arraycopy(tmp, 1, result, 0, result.length);

        return result;
    }
}
