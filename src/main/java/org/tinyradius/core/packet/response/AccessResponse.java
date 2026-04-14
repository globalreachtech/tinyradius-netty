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

/**
 * RADIUS Access-Response packet implementation.
 * <p>
 * Represents Access-Accept, Access-Reject, and Access-Challenge responses.
 * This class extends {@link GenericResponse} to provide Message-Authenticator
 * support for EAP methods.
 * <p>
 * Access-Challenge responses are sent when additional authentication
 * information is required from the user (e.g., OTP token or secondary password).
 */
public class AccessResponse extends GenericResponse implements MessageAuthSupport<RadiusResponse> {

    private AccessResponse(Dictionary dictionary, ByteBuf header, List<RadiusAttribute> attributes) throws RadiusPacketException {
        super(dictionary, header, attributes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NonNull RadiusResponse encodeResponse(@NonNull String sharedSecret, byte @NonNull [] requestAuth) throws RadiusPacketException {
        var response = ((AccessResponse) withAttributes(encodeAttributes(requestAuth, sharedSecret)))
                .encodeMessageAuth(sharedSecret, requestAuth); // always add messageAuth CVE-2024-3596

        var auth = response.genHashedAuth(sharedSecret, requestAuth);
        return withAuthAttributes(auth, response.getAttributes());
    }

    /**
     * {@inheritDoc}
     */
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

    /**
     * Represents an Access-Accept response.
     */
    public static class Accept extends AccessResponse {
        /**
         * Constructs an Access-Accept response.
         *
         * @param dictionary the dictionary to use
         * @param header     the packet header
         * @param attributes the packet attributes
         * @throws RadiusPacketException if there is an error creating the response
         */
        public Accept(Dictionary dictionary, ByteBuf header, List<RadiusAttribute> attributes) throws RadiusPacketException {
            super(dictionary, header, attributes);
            checkType(ACCESS_ACCEPT, header);
        }
    }

    /**
     * Represents an Access-Reject response.
     */
    public static class Reject extends AccessResponse {
        /**
         * Constructs an Access-Reject response.
         *
         * @param dictionary the dictionary to use
         * @param header     the packet header
         * @param attributes the packet attributes
         * @throws RadiusPacketException if there is an error creating the response
         */
        public Reject(Dictionary dictionary, ByteBuf header, List<RadiusAttribute> attributes) throws RadiusPacketException {
            super(dictionary, header, attributes);
            checkType(ACCESS_REJECT, header);
        }
    }

    /**
     * Represents an Access-Challenge response.
     */
    public static class Challenge extends AccessResponse {
        /**
         * Constructs an Access-Challenge response.
         *
         * @param dictionary the dictionary to use
         * @param header     the packet header
         * @param attributes the packet attributes
         * @throws RadiusPacketException if there is an error creating the response
         */
        public Challenge(Dictionary dictionary, ByteBuf header, List<RadiusAttribute> attributes) throws RadiusPacketException {
            super(dictionary, header, attributes);
            checkType(ACCESS_CHALLENGE, header);
        }
    }
}
