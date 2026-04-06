package org.tinyradius.core.packet.response;

import static org.tinyradius.core.packet.PacketType.ACCESS_ACCEPT;
import static org.tinyradius.core.packet.PacketType.ACCESS_CHALLENGE;
import static org.tinyradius.core.packet.PacketType.ACCESS_REJECT;

import io.netty.buffer.ByteBuf;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.attribute.type.RadiusAttribute;
import org.tinyradius.core.dictionary.Dictionary;
import org.tinyradius.core.packet.util.MessageAuthSupport;

public class AccessResponse extends GenericResponse implements MessageAuthSupport<RadiusResponse> {

    private AccessResponse(Dictionary dictionary, ByteBuf header, List<RadiusAttribute> attributes) throws RadiusPacketException {
        super(dictionary, header, attributes);
    }

    @Override
    public @NonNull RadiusResponse encodeResponse(@NonNull String sharedSecret, byte @NonNull [] requestAuth) throws RadiusPacketException {
        var response = ((AccessResponse) withAttributes(encodeAttributes(requestAuth, sharedSecret)))
                .encodeMessageAuth(sharedSecret, requestAuth);

        var auth = response.genHashedAuth(sharedSecret, requestAuth);
        return withAuthAttributes(auth, response.getAttributes());
    }

    @Override
    public @NonNull RadiusResponse decodeResponse(@NonNull String sharedSecret, byte @NonNull [] requestAuth) throws RadiusPacketException {
        verifyMessageAuth(sharedSecret, requestAuth);
        return super.decodeResponse(sharedSecret, requestAuth);
    }

    private static void checkType(byte allowed, ByteBuf header) {
        byte type = header.getByte(0);
        if (type != allowed)
            throw new IllegalArgumentException("First octet must be " + allowed + ", actual: " + type);
    }

    public static class Accept extends AccessResponse {
        public Accept(Dictionary dictionary, ByteBuf header, List<RadiusAttribute> attributes) throws RadiusPacketException {
            super(dictionary, header, attributes);
            checkType(ACCESS_ACCEPT, header);
        }
    }

    public static class Reject extends AccessResponse {
        public Reject(Dictionary dictionary, ByteBuf header, List<RadiusAttribute> attributes) throws RadiusPacketException {
            super(dictionary, header, attributes);
            checkType(ACCESS_REJECT, header);
        }
    }

    public static class Challenge extends AccessResponse {
        public Challenge(Dictionary dictionary, ByteBuf header, List<RadiusAttribute> attributes) throws RadiusPacketException {
            super(dictionary, header, attributes);
            checkType(ACCESS_CHALLENGE, header);
        }
    }
}
