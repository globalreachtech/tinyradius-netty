package org.tinyradius.core.packet.request;

import io.netty.buffer.ByteBuf;
import org.jspecify.annotations.NonNull;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.attribute.type.RadiusAttribute;
import org.tinyradius.core.dictionary.Dictionary;

import java.util.List;

import static org.tinyradius.core.attribute.AttributeTypes.EAP_MESSAGE;
import static org.tinyradius.core.attribute.AttributeTypes.MESSAGE_AUTHENTICATOR;

/**
 * EAP AccessRequest RFC3579
 */
public class AccessRequestEap extends AccessRequest {

    /**
     * Constructs an AccessRequestEap.
     *
     * @param dictionary the dictionary to use
     * @param header     the packet header
     * @param attributes the packet attributes
     * @throws RadiusPacketException if there is an error creating the request
     */
    public AccessRequestEap(@NonNull Dictionary dictionary, @NonNull ByteBuf header, @NonNull List<RadiusAttribute> attributes) throws RadiusPacketException {
        super(dictionary, header, attributes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NonNull RadiusRequest decodeRequest(@NonNull String sharedSecret) throws RadiusPacketException {
        var messageAuthAttr = getAttributes(MESSAGE_AUTHENTICATOR);
        if (messageAuthAttr.size() != 1)
            throw new RadiusPacketException("AccessRequest (EAP) should have exactly one Message-Authenticator attribute, has " + messageAuthAttr.size());

        return super.decodeRequest(sharedSecret);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateAttributes() throws RadiusPacketException {
        var eapMessageAttr = getAttributes(EAP_MESSAGE);
        if (eapMessageAttr.isEmpty())
            throw new RadiusPacketException("AccessRequest (EAP) must have at least one EAP-Message attribute");
    }
}
