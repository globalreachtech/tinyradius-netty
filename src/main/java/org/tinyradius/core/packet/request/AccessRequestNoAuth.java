package org.tinyradius.core.packet.request;

import io.netty.buffer.ByteBuf;
import org.jspecify.annotations.NonNull;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.attribute.type.RadiusAttribute;
import org.tinyradius.core.dictionary.Dictionary;

import java.util.List;

import static org.tinyradius.core.attribute.AttributeTypes.MESSAGE_AUTHENTICATOR;

/**
 * Basic Access-Request without Message-Authenticator attribute or any encoding.
 * <p>
 * Use this class when the authentication method does not require MD5 challenge-response (e.g., PAP with no EAP).
 * <p>
 * This is also used internally during construction of other Access-Request types.
 */
public class AccessRequestNoAuth extends AccessRequest {

    /**
     * Constructs an AccessRequestNoAuth.
     *
     * @param dictionary the dictionary to use for attribute lookups
     * @param header     the 20-octet packet header
     * @param attributes the list of attributes for this packet
     * @throws RadiusPacketException if the packet length or header is invalid
     */
    public AccessRequestNoAuth(@NonNull Dictionary dictionary, @NonNull ByteBuf header, @NonNull List<RadiusAttribute> attributes) throws RadiusPacketException {
        super(dictionary, header, attributes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NonNull RadiusRequest decodeRequest(@NonNull String sharedSecret) throws RadiusPacketException {
        var messageAuthAttr = getAttributes(MESSAGE_AUTHENTICATOR);
        if (messageAuthAttr.size() != 1)
            logger.warn("AccessRequest without one of User-Password/CHAP-Password/ARAP-Password/EAP-Message " +
                    "should contain a Message-Authenticator");
        return super.decodeRequest(sharedSecret);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateAttributes() {
        // nothing to validate
    }
}
