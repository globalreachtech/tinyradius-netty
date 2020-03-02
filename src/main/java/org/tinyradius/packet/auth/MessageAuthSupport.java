package org.tinyradius.packet.auth;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.tinyradius.attribute.RadiusAttribute;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.util.RadiusPacketException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

public interface MessageAuthSupport extends RadiusPacket {

    int MESSAGE_AUTHENTICATOR = 80;

    default byte[] computeMessageAuth(String sharedSecret, byte[] requestAuth){
        final Mac mac = getHmacMd5(sharedSecret);
        return mac.doFinal(calcMessageAuthInput(requestAuth));
    }

    default void verifyMessageAuth(String sharedSecret, byte[] requestAuth) throws RadiusPacketException {
        final List<RadiusAttribute> msgAuthAttr = getAttributes(MESSAGE_AUTHENTICATOR);
        if (msgAuthAttr.size() > 1)
            throw new RadiusPacketException("Packet should have at most one Message-Authenticator attribute, has " + msgAuthAttr.size());

        if (msgAuthAttr.size() == 1) {
            final byte[] messageAuth = msgAuthAttr.get(0).getValue();
            // todo tests

            if (!Arrays.equals(messageAuth, computeMessageAuth(sharedSecret, requestAuth)))
                throw new RadiusPacketException("Message-Authenticator attribute check failed");
        }
    }

    default byte[] calcMessageAuthInput(byte[] requestAuth) {
        final ByteBuf buf = Unpooled.buffer()
                .writeByte(getType())
                .writeByte(getIdentifier())
                .writeShort(0) // placeholder
                .writeBytes(requestAuth);

        for (RadiusAttribute attribute : getAttributes()) {
            if (attribute.getVendorId() == -1 && attribute.getType() == MESSAGE_AUTHENTICATOR)
                buf.writeBytes(new byte[18]);
            else
                buf.writeBytes(attribute.toByteArray());
        }

        return buf.setShort(2, buf.readableBytes()).array();
    }

    static Mac getHmacMd5(String key) {
        try {
            final String HMAC_MD5 = "HmacMD5";
            final SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(), HMAC_MD5);
            final Mac mac = Mac.getInstance(HMAC_MD5);
            mac.init(secretKeySpec);
            return mac;
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException(e); // never happens
        }
    }
}
