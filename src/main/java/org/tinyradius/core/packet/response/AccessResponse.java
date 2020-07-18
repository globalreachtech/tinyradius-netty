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
        final byte type = header.getByte(0);
        if (type != ACCESS_ACCEPT && type != ACCESS_REJECT && type != ACCESS_CHALLENGE)
            throw new IllegalArgumentException("First octet must be " + ACCESS_ACCEPT + "/" + ACCESS_REJECT + "/" + ACCESS_CHALLENGE + ", actual: " + type);
    }

    @Override
    public RadiusResponse encodeResponse(String sharedSecret, byte[] requestAuth) throws RadiusPacketException {
        final RadiusResponse response = ((AccessResponse) withAttributes(encodeAttributes(requestAuth, sharedSecret)))
                .encodeMessageAuth(sharedSecret, requestAuth);

        final byte[] auth = response.genHashedAuth(sharedSecret, requestAuth);
        return new AccessResponse(getDictionary(), headerWithAuth(auth), response.getAttributes());
    }

    @Override
    public RadiusResponse decodeResponse(String sharedSecret, byte[] requestAuth) throws RadiusPacketException {
        verifyMessageAuth(sharedSecret, requestAuth);
        return super.decodeResponse(sharedSecret, requestAuth);
    }

    public static class Accept extends AccessResponse {
        public Accept(Dictionary dictionary, ByteBuf header, List<RadiusAttribute> attributes) throws RadiusPacketException {
            super(dictionary, header, attributes);
            final byte type = header.getByte(0);
            if (type != ACCESS_ACCEPT)
                throw new IllegalArgumentException("First octet must be " + ACCESS_ACCEPT + ", actual: " + type);
        }
    }

    public static class Reject extends AccessResponse {
        public Reject(Dictionary dictionary, ByteBuf header, List<RadiusAttribute> attributes) throws RadiusPacketException {
            super(dictionary, header, attributes);
            final byte type = header.getByte(0);
            if (type != ACCESS_REJECT)
                throw new IllegalArgumentException("First octet must be " + ACCESS_REJECT + ", actual: " + type);
        }
    }

    public static class Challenge extends AccessResponse {
        public Challenge(Dictionary dictionary, ByteBuf header, List<RadiusAttribute> attributes) throws RadiusPacketException {
            super(dictionary, header, attributes);
            final byte type = header.getByte(0);
            if (type != ACCESS_CHALLENGE)
                throw new IllegalArgumentException("First octet must be " + ACCESS_CHALLENGE + ", actual: " + type);
        }
    }
}
