package org.tinyradius.core.packet.request;

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
import static org.tinyradius.core.packet.PacketType.ACCESS_REQUEST;
import static org.tinyradius.core.packet.PacketType.ACCOUNTING_REQUEST;

public interface RadiusRequest extends RadiusPacket<RadiusRequest> {

    static RadiusRequest create(Dictionary dictionary, ByteBuf header, List<RadiusAttribute> attributes) throws RadiusPacketException {
        switch (header.getByte(0)) {
            case ACCESS_REQUEST:
                return AccessRequest.create(dictionary, header, attributes);
            case ACCOUNTING_REQUEST:
                return new AccountingRequest(dictionary, header, attributes);
            default:
                return new GenericRequest(dictionary, header, attributes);
        }
    }

    /**
     * Creates a RadiusPacket object. Depending on the passed type, an
     * appropriate packet is created. Also sets the type, and the packet id.
     *
     * @param dictionary    custom dictionary to use
     * @param type          packet type
     * @param id            packet id
     * @param authenticator authenticator for packet, nullable
     * @param attributes    list of attributes for packet
     * @return RadiusPacket object
     * @throws RadiusPacketException packet validation exceptions
     */
    static RadiusRequest create(Dictionary dictionary, byte type, byte id, byte[] authenticator, List<RadiusAttribute> attributes) throws RadiusPacketException {
        List<RadiusAttribute> wrappedAttributes = attributes.stream()
                .map(NestedAttributeHolder::vsaAutowrap)
                .collect(toList());
        final ByteBuf header = RadiusPacket.buildHeader(type, id, authenticator, wrappedAttributes);
        return create(dictionary, header, wrappedAttributes);
    }

    /**
     * Reads a request from the given input stream and
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
    static RadiusRequest fromDatagram(Dictionary dictionary, DatagramPacket datagram) throws RadiusPacketException {
        // copy into Unpooled heap buffer so let JVM manage GC
        return fromByteBuf(dictionary, Unpooled.unreleasableBuffer(
                Unpooled.copiedBuffer(datagram.content())));
    }

    /**
     * Reads a request from the given input stream and
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
    static RadiusRequest fromByteBuf(Dictionary dictionary, ByteBuf byteBuf) throws RadiusPacketException {
        return RadiusRequest.create(dictionary, RadiusPacket.readHeader(byteBuf),
                AttributeHolder.readAttributes(dictionary, -1, byteBuf));
    }

    /**
     * Encode request and generate authenticator.
     * <p>
     * Must be idempotent.
     *
     * @param sharedSecret shared secret that secures the communication
     *                     with the other Radius server/client
     * @return RadiusRequest with new authenticator and/or encoded attributes
     * @throws RadiusPacketException if invalid or missing attributes
     */
    RadiusRequest encodeRequest(String sharedSecret) throws RadiusPacketException;

    /**
     * Decodes the request against the supplied shared secret.
     * <p>
     * Must be idempotent.
     *
     * @param sharedSecret shared secret
     * @return verified RadiusRequest with decoded attributes if appropriate
     * @throws RadiusPacketException if authenticator check fails
     */
    RadiusRequest decodeRequest(String sharedSecret) throws RadiusPacketException;
}
