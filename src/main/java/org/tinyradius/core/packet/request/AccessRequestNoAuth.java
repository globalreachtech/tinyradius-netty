package org.tinyradius.core.packet.request;

import static org.tinyradius.core.attribute.AttributeTypes.MESSAGE_AUTHENTICATOR;

import io.netty.buffer.ByteBuf;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.attribute.type.RadiusAttribute;
import org.tinyradius.core.dictionary.Dictionary;

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
