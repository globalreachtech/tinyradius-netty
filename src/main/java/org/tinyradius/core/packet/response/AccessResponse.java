package org.tinyradius.core.packet.response;

import io.netty.buffer.ByteBuf;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.attribute.type.RadiusAttribute;
import org.tinyradius.core.dictionary.Dictionary;
import org.tinyradius.core.packet.util.MessageAuthSupport;

import java.util.List;

import static org.tinyradius.core.packet.PacketType.*;

public class AccessResponse extends GenericResponse implements MessageAuthSupport<RadiusResponse> {

    private AccessResponse(Dictionary dictionary, ByteBuf header, List<RadiusAttribute> attributes) throws RadiusPacketException {
        super(dictionary, header, attributes);
    }

    @Override
    public RadiusResponse encodeResponse(String sharedSecret, byte[] requestAuth) throws RadiusPacketException {
        final RadiusResponse response = ((AccessResponse) withAttributes(encodeAttributes(requestAuth, sharedSecret)))
                .encodeMessageAuth(sharedSecret, requestAuth);

        final byte[] auth = response.genHashedAuth(sharedSecret, requestAuth);
        return withAuthAttributes(auth, response.getAttributes());
    }

    @Override
    public RadiusResponse decodeResponse(String sharedSecret, byte[] requestAuth) throws RadiusPacketException {
        verifyMessageAuth(sharedSecret, requestAuth);
        return super.decodeResponse(sharedSecret, requestAuth);
    }

    private static void checkType(byte allowed, ByteBuf header) {
        final byte type = header.getByte(0);
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
