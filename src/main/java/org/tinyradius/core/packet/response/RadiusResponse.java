package org.tinyradius.core.packet.response;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.socket.DatagramPacket;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.attribute.AttributeHolder;
import org.tinyradius.core.attribute.NestedAttributeHolder;
import org.tinyradius.core.attribute.type.RadiusAttribute;
import org.tinyradius.core.dictionary.Dictionary;
import org.tinyradius.core.packet.RadiusPacket;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.tinyradius.core.packet.PacketType.*;

public interface RadiusResponse extends RadiusPacket<RadiusResponse> {

    static RadiusResponse create(Dictionary dictionary, ByteBuf header, List<RadiusAttribute> attributes) throws RadiusPacketException {
        switch (header.getByte(0)) {
            case ACCESS_ACCEPT:
                return new AccessResponse.Accept(dictionary, header, attributes);
            case ACCESS_REJECT:
                return new AccessResponse.Reject(dictionary, header, attributes);
            case ACCESS_CHALLENGE:
                return new AccessResponse.Challenge(dictionary, header, attributes);
            default:
                return new GenericResponse(dictionary, header, attributes);
        }
    }

    /**
     * Creates a RadiusPacket object. Depending on the passed type, an
     * appropriate packet is created. Also sets the type and the packet id.
     *
     * @param dictionary    custom dictionary to use
     * @param type          packet type
     * @param id            packet id
     * @param authenticator authenticator for packet, nullable
     * @param attributes    list of attributes for packet
     * @return RadiusPacket object
     * @throws RadiusPacketException packet validation exceptions
     */
    static RadiusResponse create(Dictionary dictionary, byte type, byte id, byte[] authenticator, List<RadiusAttribute> attributes) throws RadiusPacketException {
        List<RadiusAttribute> wrappedAttributes = attributes.stream()
                .map(NestedAttributeHolder::vsaAutowrap)
                .collect(toList());
        final ByteBuf header = RadiusPacket.buildHeader(type, id, authenticator, wrappedAttributes);
        return create(dictionary, header, wrappedAttributes);
    }

    /**
     * Reads a response from the given input stream and
     * creates an appropriate RadiusPacket/subclass.
     * <p>
     * Decodes the encrypted fields and attributes of the packet, and checks
     * authenticator if appropriate.
     *
     * @param dictionary dictionary to use for attributes
     * @param datagram   DatagramPacket to read packet from
     * @return new RadiusPacket object
     * @throws RadiusPacketException malformed packet
     */
    static RadiusResponse fromDatagram(Dictionary dictionary, DatagramPacket datagram) throws RadiusPacketException {
        // copy into Unpooled heap buffer so let JVM manage GC
        return fromByteBuf(dictionary, Unpooled.unreleasableBuffer(
                Unpooled.copiedBuffer(datagram.content())));
    }

    /**
     * Reads a response from the given input stream and
     * creates an appropriate RadiusPacket/subclass.
     * <p>
     * Decodes the encrypted fields and attributes of the packet, and checks
     * authenticator if appropriate.
     *
     * @param dictionary dictionary to use for attributes
     * @param byteBuf    byteBuf to read packet from
     * @return new RadiusPacket object
     * @throws RadiusPacketException malformed packet
     */
    static RadiusResponse fromByteBuf(Dictionary dictionary, ByteBuf byteBuf) throws RadiusPacketException {
        return RadiusResponse.create(dictionary, RadiusPacket.readHeader(byteBuf),
                AttributeHolder.readAttributes(dictionary, -1, byteBuf));
    }

    /**
     * Encode and generate authenticator.
     * <p>
     * Requires request authenticator to generate response authenticator.
     * <p>
     * Must be idempotent.
     *
     * @param sharedSecret shared secret to be used to encode this packet
     * @param requestAuth  request packet authenticator
     * @return new RadiusPacket instance with same properties and valid authenticator
     * @throws RadiusPacketException errors encoding packet
     */
    RadiusResponse encodeResponse(String sharedSecret, byte[] requestAuth) throws RadiusPacketException;

    /**
     * Decodes the response against the supplied shared secret and request authenticator.
     * <p>
     * Must be idempotent.
     *
     * @param sharedSecret shared secret
     * @param requestAuth  authenticator for corresponding request
     * @return verified RadiusResponse with decoded attributes if appropriate
     * @throws RadiusPacketException errors verifying or decoding packet
     */
    RadiusResponse decodeResponse(String sharedSecret, byte[] requestAuth) throws RadiusPacketException;
}
