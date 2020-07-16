package org.tinyradius.core.packet.response;

import io.netty.buffer.ByteBuf;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.attribute.type.RadiusAttribute;
import org.tinyradius.core.dictionary.Dictionary;
import org.tinyradius.core.packet.BaseRadiusPacket;
import org.tinyradius.core.packet.RadiusPacket;

import java.util.List;

public class GenericResponse extends BaseRadiusPacket<RadiusResponse> implements RadiusResponse {

    public GenericResponse(Dictionary dictionary, ByteBuf header, List<RadiusAttribute> attributes) throws RadiusPacketException {
        super(dictionary, header, attributes);
    }

    @Override
    public RadiusResponse encodeResponse(String sharedSecret, byte[] requestAuth) throws RadiusPacketException {
        final RadiusResponse response = withAttributes(encodeAttributes(requestAuth, sharedSecret));

        final byte[] auth = response.genHashedAuth(sharedSecret, requestAuth);
        return new GenericResponse(getDictionary(), headerWithAuth( auth), response.getAttributes());
    }

    @Override
    public RadiusResponse decodeResponse(String sharedSecret, byte[] requestAuth) throws RadiusPacketException {
        verifyPacketAuth(sharedSecret, requestAuth);
        return withAttributes(decodeAttributes(requestAuth, sharedSecret));
    }

    @Override
    public GenericResponse withAttributes(List<RadiusAttribute> attributes) throws RadiusPacketException {
        return new GenericResponse(getDictionary(), getHeader(), attributes);
    }
}
