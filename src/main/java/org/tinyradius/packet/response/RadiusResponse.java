package org.tinyradius.packet.response;

import io.netty.channel.socket.DatagramPacket;
import org.tinyradius.attribute.type.RadiusAttribute;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.packet.request.RadiusRequest;
import org.tinyradius.util.RadiusPacketException;

import java.util.List;

import static org.tinyradius.packet.PacketType.*;

public interface RadiusResponse extends RadiusPacket<RadiusResponse> {

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
    static RadiusResponse create(Dictionary dictionary, byte type, byte identifier, byte[] authenticator, List<RadiusAttribute> attributes) {
        switch (type) {
            case ACCESS_ACCEPT:
                return new AccessResponse.Accept(dictionary, identifier, authenticator, attributes);
            case ACCESS_REJECT:
                return new AccessResponse.Reject(dictionary, identifier, authenticator, attributes);
            case ACCESS_CHALLENGE:
                return new AccessResponse.Challenge(dictionary, identifier, authenticator, attributes);
            default:
                return new GenericResponse(dictionary, type, identifier, authenticator, attributes);
        }
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
        final RadiusRequest rr = RadiusPacket.fromByteBuf(dictionary, datagram.content());
        return create(rr.getDictionary(), rr.getType(), rr.getId(), rr.getAuthenticator(), rr.getAttributes());
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
