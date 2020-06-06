package org.tinyradius.packet.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.tinyradius.attribute.RadiusAttribute;
import org.tinyradius.attribute.util.Attributes;
import org.tinyradius.attribute.util.NestedAttributeHolder;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.util.RadiusPacketException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Partial implementation for encoding/verifying Message-Authenticator (RFC 2869)
 *
 * @param <T> same type as implementation
 */
public interface MessageAuthSupport<T extends MessageAuthSupport<T>> extends RadiusPacket, NestedAttributeHolder.Writable<T> {

    byte MESSAGE_AUTHENTICATOR = 80;

    default void verifyMessageAuth(String sharedSecret, byte[] requestAuth) throws RadiusPacketException {
        final List<RadiusAttribute> msgAuthAttr = getAttributes(MESSAGE_AUTHENTICATOR);

        if (msgAuthAttr.isEmpty())
            return;

        if (msgAuthAttr.size() > 1)
            throw new RadiusPacketException("Packet should have at most one Message-Authenticator attribute, has " + msgAuthAttr.size());

        final byte[] messageAuth = msgAuthAttr.get(0).getValue();

        if (!Arrays.equals(messageAuth, computeMessageAuth(sharedSecret, requestAuth)))
            throw new RadiusPacketException("Message-Authenticator attribute check failed");
    }

    default byte[] computeMessageAuth(String sharedSecret, byte[] requestAuth) {
        Objects.requireNonNull(requestAuth, "Request Authenticator cannot be null for Message-Authenticator hashing");
        final Mac mac = getHmacMd5(sharedSecret);
        return mac.doFinal(calcMessageAuthInput(requestAuth));
    }

    default byte[] calcMessageAuthInput(byte[] requestAuth) {
        final ByteBuf buf = Unpooled.buffer()
                .writeByte(getType())
                .writeByte(getId())
                .writeShort(0) // placeholder
                .writeBytes(requestAuth);

        for (RadiusAttribute attribute : getAttributes()) {
            if (attribute.getVendorId() == -1 && attribute.getType() == MESSAGE_AUTHENTICATOR)
                buf.writeByte(MESSAGE_AUTHENTICATOR)
                        .writeByte(18)
                        .writeBytes(new byte[16]);
            else
                buf.writeBytes(attribute.toByteArray());
        }

        return buf.setShort(2, buf.readableBytes()).copy().array();
    }

    static Mac getHmacMd5(String key) {
        try {
            final String HMAC_MD5 = "HmacMD5";
            final SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(), HMAC_MD5);
            final Mac mac = Mac.getInstance(HMAC_MD5);
            mac.init(secretKeySpec);
            return mac;
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalArgumentException(e); // never happens
        }
    }

    /**
     * @return packet of same type as self, including intermediate/transient fields
     */
    T copy();

    /**
     * @param sharedSecret share secret
     * @param requestAuth  current packet auth if encoding request, otherwise auth
     *                     for corresponding request
     * @return shallow copy of packet
     */
    default T encodeMessageAuth(String sharedSecret, byte[] requestAuth) {
        // When the message integrity check is calculated the signature
        // string should be considered to be sixteen octets of zero.
        final ByteBuffer buffer = ByteBuffer.allocate(16);

        final T newPacket = this
                .removeAttributes(MESSAGE_AUTHENTICATOR)
                .addAttribute(Attributes.create(getDictionary(), -1, MESSAGE_AUTHENTICATOR, buffer.array()));

        buffer.put(newPacket.computeMessageAuth(sharedSecret, requestAuth));

        return newPacket;
    }
}
