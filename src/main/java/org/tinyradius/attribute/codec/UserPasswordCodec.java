package org.tinyradius.attribute.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.tinyradius.util.RadiusPacketException;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Attribute is encrypted with the method as defined in RFC2865 for the User-Password attribute
 */
class UserPasswordCodec extends BaseCodec {

    @Override
    protected byte[] encodeData(byte[] data, byte[] auth, byte[] secret) {
        final byte[] str = pad16x(data);
        final ByteBuffer buffer = ByteBuffer.allocate(str.length);

        byte[] c = auth;

        for (int i = 0; i < str.length; i += 16) {
            c = xor16(str, i, md5(secret, c));
            buffer.put(c);
        }

        return buffer.array();
    }

    @Override
    protected byte[] decodeData(byte[] encodedData, byte[] auth, byte[] secret) throws RadiusPacketException {
        if (encodedData.length < 16)
            throw new RadiusPacketException("Malformed attribute while decoding with RFC2865 User-Password method - " +
                    "data must be at least 16 octets, actual: " + encodedData.length);

        if (encodedData.length % 16 != 0)
            throw new RadiusPacketException("Malformed attribute while decoding with RFC2865 User-Password method - " +
                    "data octets must be multiple of 16, actual: " + encodedData.length);

        final ByteBuf buf = Unpooled.buffer(encodedData.length, encodedData.length);
        byte[] c = auth;

        for (int i = 0; i < encodedData.length; i += 16) {
            buf.writeBytes(xor16(encodedData, i, md5(secret, c)));
            c = Arrays.copyOfRange(encodedData, i, 16);
        }

        final int nullIndex = buf.indexOf(0, encodedData.length - 1, (byte) 0);

        return nullIndex == -1 ?
                buf.copy().array() :
                buf.writerIndex(nullIndex).copy().array();
    }
}
