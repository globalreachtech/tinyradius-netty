package org.tinyradius.core.packet.request;

import io.netty.channel.socket.DatagramPacket;
import org.tinyradius.core.attribute.type.RadiusAttribute;
import org.tinyradius.core.dictionary.Dictionary;
import org.tinyradius.core.packet.PacketType;
import org.tinyradius.core.packet.RadiusPacket;
import org.tinyradius.core.RadiusPacketException;

import java.util.List;

public interface RadiusRequest extends RadiusPacket<RadiusRequest> {

    /**
     * Creates a RadiusPacket object. Depending on the passed type, an
     * appropriate packet is created. Also sets the type, and the
     * the packet identifier.
     *
     * @param dictionary    custom dictionary to use
     * @param type          packet type
     * @param identifier    packet identifier
     * @param authenticator authenticator for packet, nullable
     * @param attributes    list of attributes for packet
     * @return RadiusPacket object
     */
    static RadiusRequest create(Dictionary dictionary, byte type, byte identifier, byte[] authenticator, List<RadiusAttribute> attributes) {
        switch (type) {
            case PacketType.ACCESS_REQUEST:
                return AccessRequest.create(dictionary, identifier, authenticator, attributes);
            case PacketType.ACCOUNTING_REQUEST:
                return new AccountingRequest(dictionary, identifier, authenticator, attributes);
            default:
                return new GenericRequest(dictionary, type, identifier, authenticator, attributes);
        }
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
        return RadiusPacket.fromByteBuf(dictionary, datagram.content());
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
