package org.tinyradius.core.packet.response;

import io.netty.buffer.ByteBuf;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.attribute.type.RadiusAttribute;
import org.tinyradius.core.dictionary.Dictionary;
import org.tinyradius.core.packet.BaseRadiusPacket;

import java.util.List;

public class GenericResponse extends BaseRadiusPacket<RadiusResponse> implements RadiusResponse {

    public GenericResponse(Dictionary dictionary, ByteBuf header, List<RadiusAttribute> attributes) throws RadiusPacketException {
        super(dictionary, header, attributes);
    }

    @Override
    public RadiusResponse encodeResponse(String sharedSecret, byte[] requestAuth) throws RadiusPacketException {
        final RadiusResponse response = withAttributes(encodeAttributes(requestAuth, sharedSecret));

        final byte[] auth = response.genHashedAuth(sharedSecret, requestAuth);
        return with(getDictionary(), headerWithAuth(auth), response.getAttributes());
    }

    @Override
    public RadiusResponse decodeResponse(String sharedSecret, byte[] requestAuth) throws RadiusPacketException {
        verifyPacketAuth(sharedSecret, requestAuth);
        return withAttributes(decodeAttributes(requestAuth, sharedSecret));
    }

    @Override
    public RadiusResponse with(Dictionary dictionary, ByteBuf header, List<RadiusAttribute> attributes) throws RadiusPacketException {
        return RadiusResponse.create(dictionary, header, attributes);
    }
}
