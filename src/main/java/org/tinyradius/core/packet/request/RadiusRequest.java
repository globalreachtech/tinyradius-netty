package org.tinyradius.core.packet.request;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.socket.DatagramPacket;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.attribute.AttributeHolder;
import org.tinyradius.core.attribute.NestedAttributeHolder;
import org.tinyradius.core.attribute.type.RadiusAttribute;
import org.tinyradius.core.dictionary.Dictionary;
import org.tinyradius.core.packet.RadiusPacket;

import java.util.List;

import static org.tinyradius.core.packet.PacketType.ACCESS_REQUEST;
import static org.tinyradius.core.packet.PacketType.ACCOUNTING_REQUEST;

public interface RadiusRequest extends RadiusPacket<RadiusRequest> {

    /**
     * Creates a RadiusRequest object.
     *
     * @param dictionary the dictionary to use
     * @param header     the packet header
     * @param attributes the packet attributes
     * @return the new RadiusRequest
     * @throws RadiusPacketException if there is an error creating the request
     */
    @NonNull
    static RadiusRequest create(@NonNull Dictionary dictionary, @NonNull ByteBuf header, @NonNull List<RadiusAttribute> attributes) throws RadiusPacketException {
        return switch (header.getByte(0)) {
            case ACCESS_REQUEST -> AccessRequest.create(dictionary, header, attributes);
            case ACCOUNTING_REQUEST -> new AccountingRequest(dictionary, header, attributes);
            default -> new GenericRequest(dictionary, header, attributes);
        };
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
    @NonNull
    static RadiusRequest create(@NonNull Dictionary dictionary, byte type, byte id, byte @Nullable [] authenticator, @NonNull List<RadiusAttribute> attributes) throws RadiusPacketException {
        var wrappedAttributes = attributes.stream()
                .map(NestedAttributeHolder::vsaAutowrap)
                .toList();
        var header = RadiusPacket.buildHeader(type, id, authenticator, wrappedAttributes);
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
    @NonNull
    static RadiusRequest fromDatagram(@NonNull Dictionary dictionary, @NonNull DatagramPacket datagram) throws RadiusPacketException {
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
    @NonNull
    static RadiusRequest fromByteBuf(@NonNull Dictionary dictionary, @NonNull ByteBuf byteBuf) throws RadiusPacketException {
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
    @NonNull
    RadiusRequest encodeRequest(@NonNull String sharedSecret) throws RadiusPacketException;

    /**
     * Decodes the request against the supplied shared secret.
     * <p>
     * Must be idempotent.
     *
     * @param sharedSecret shared secret
     * @return verified RadiusRequest with decoded attributes if appropriate
     * @throws RadiusPacketException if authenticator check fails
     */
    @NonNull
    RadiusRequest decodeRequest(@NonNull String sharedSecret) throws RadiusPacketException;
}
