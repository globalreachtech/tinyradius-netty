package org.tinyradius.core.packet.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.attribute.type.RadiusAttribute;
import org.tinyradius.core.packet.RadiusPacket;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

import static org.tinyradius.core.attribute.AttributeTypes.MESSAGE_AUTHENTICATOR;
import static org.tinyradius.core.attribute.codec.AttributeCodecType.NO_ENCRYPT;

/**
 * Partial implementation for encoding/verifying Message-Authenticator (RFC 2869)
 *
 * @param <T> same type as implementation
 */
public interface MessageAuthSupport<T extends RadiusPacket<T>> extends RadiusPacket<T> {

    Logger msgAuthLogger = LogManager.getLogger();

    private static byte[] calcMessageAuthInput(RadiusPacket<?> packet, byte[] requestAuth) {
        final ByteBuf buf = Unpooled.buffer()
                .writeByte(packet.getType())
                .writeByte(packet.getId())
                .writeShort(0) // placeholder
                .writeBytes(requestAuth);

        for (RadiusAttribute attribute : packet.getAttributes()) {
            if (attribute.getVendorId() == -1 && attribute.getType() == MESSAGE_AUTHENTICATOR)
                buf.writeByte(MESSAGE_AUTHENTICATOR)
                        .writeByte(18)
                        .writeBytes(new byte[16]);
            else
                buf.writeBytes(attribute.toByteArray());
        }

        return buf.setShort(2, buf.readableBytes()).copy().array();
    }

    private static Mac getHmacMd5(String key) {
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
     * Verifies the packet with an encoded Message-Authenticator attribute.
     *
     * @param sharedSecret shared secret
     * @param requestAuth  corresponding request auth, or 'this' packet auth if null
     * @throws RadiusPacketException packet validation exceptions
     */
    default void verifyMessageAuth(@NonNull String sharedSecret, byte @Nullable [] requestAuth) throws RadiusPacketException {
        if (sharedSecret.isEmpty())
            throw new IllegalArgumentException("Shared secret cannot be null/empty");

        final List<RadiusAttribute> msgAuthAttr = getAttributes(MESSAGE_AUTHENTICATOR);
        if (msgAuthAttr.isEmpty())
            return;

        if (msgAuthAttr.size() > 1)
            throw new RadiusPacketException("Message-Authenticator check failed - should have at most one count, has " + msgAuthAttr.size());

        final byte[] messageAuth = msgAuthAttr.get(0).getValue();

        if (messageAuth.length != 16)
            throw new RadiusPacketException("Message-Authenticator check failed - must be 16 octets, actual " + messageAuth.length);

        if (!Arrays.equals(messageAuth, computeMessageAuth(this, sharedSecret, requestAuth))) {
            // find attributes that can be encoded but aren't
            final boolean decodedAlready = getAttribute(a ->
                    a.codecType() != NO_ENCRYPT && a.isDecoded()).isPresent();

            if (decodedAlready)
                msgAuthLogger.info("Skipping Message-Authenticator check - attributes have been decrypted already");
            else
                throw new RadiusPacketException("Message-Authenticator check failed");
        }
    }

    /**
     * @param packet       to compute messageAuth for
     * @param sharedSecret shared secret
     * @param requestAuth  corresponding request auth, defaults to 'this' packet auth if null
     * @return Message Authenticator byte array
     */
    private static byte[] computeMessageAuth(RadiusPacket<?> packet, String sharedSecret, byte @Nullable [] requestAuth) {
        final byte[] messageAuthInput = calcMessageAuthInput(
                packet, requestAuth != null ? requestAuth : packet.getAuthenticator());
        return getHmacMd5(sharedSecret).doFinal(messageAuthInput);
    }

    /**
     * Creates a packet with an encoded Message-Authenticator attribute.
     *
     * @param sharedSecret shared secret
     * @param requestAuth  corresponding request auth, defaults to 'this' packet auth if null
     * @return encoded copy of this packet
     * @throws RadiusPacketException packet validation exceptions
     */
    default T encodeMessageAuth(@NonNull String sharedSecret, byte @Nullable [] requestAuth) throws RadiusPacketException {
        if (sharedSecret.isEmpty())
            throw new IllegalArgumentException("Shared secret cannot be null/empty");

        // When the message integrity check is calculated, the signature
        // string should be considered to be sixteen octets of zero.
        final ByteBuffer buffer = ByteBuffer.allocate(16);
        final RadiusAttribute attribute = getDictionary().createAttribute(-1, MESSAGE_AUTHENTICATOR, (byte) 0, buffer.array());

        final List<RadiusAttribute> attributes = getAttributes(a -> a.getType() != MESSAGE_AUTHENTICATOR);
        attributes.add(attribute);

        final T newPacket = withAttributes(attributes);
        buffer.put(computeMessageAuth(newPacket, sharedSecret, requestAuth));

        return newPacket;
    }
}
