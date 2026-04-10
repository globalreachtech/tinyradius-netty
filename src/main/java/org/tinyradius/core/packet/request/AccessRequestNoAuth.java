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

    public AccessRequestNoAuth(Dictionary dictionary, ByteBuf header, List<RadiusAttribute> attributes) throws RadiusPacketException {
        super(dictionary, header, attributes);
    }

    @Override
    public @NonNull RadiusRequest encodeRequest(@NonNull String sharedSecret) throws RadiusPacketException {
        var radiusRequest = super.encodeRequest(sharedSecret);
        checkMessageAuth(radiusRequest.getAttributes()); // MessageAuthSupport should append Message-Auth during encoding
        return radiusRequest;
    }

    @Override
    public @NonNull RadiusRequest decodeRequest(@NonNull String sharedSecret) throws RadiusPacketException {
        checkMessageAuth(getAttributes());
        return super.decodeRequest(sharedSecret);
    }

    private static void checkMessageAuth(List<RadiusAttribute> attributes) {
        long count = attributes.stream()
                .filter(a -> a.getType() == MESSAGE_AUTHENTICATOR)
                .count();
        if (count != 1)
            logger.warn("AccessRequest without one of User-Password/CHAP-Password/ARAP-Password/EAP-Message " +
                    "should contain a Message-Authenticator");
    }
}
