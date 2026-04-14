package org.tinyradius.core.packet.request;

import io.netty.buffer.ByteBuf;
import org.jspecify.annotations.NonNull;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.attribute.type.RadiusAttribute;
import org.tinyradius.core.dictionary.Dictionary;

import java.util.List;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toList;
import static org.tinyradius.core.attribute.AttributeTypes.USER_PASSWORD;

/**
 * PAP AccessRequest RFC2865
 */
public class AccessRequestPap extends AccessRequest {

    /**
     * Constructs an AccessRequestPap.
     *
     * @param dictionary the dictionary to use for attribute lookups
     * @param header     the 20-octet packet header
     * @param attributes the list of attributes for this packet
     * @throws RadiusPacketException if the packet length or header is invalid
     */
    public AccessRequestPap(@NonNull Dictionary dictionary, @NonNull ByteBuf header, @NonNull List<RadiusAttribute> attributes) throws RadiusPacketException {
        super(dictionary, header, attributes);
    }

    static @NonNull AccessRequest withPassword(@NonNull AccessRequest request, @NonNull String password) throws RadiusPacketException {
        var attributes = withPasswordAttribute(request.getDictionary(), request.getAttributes(), password);
        return (AccessRequest) request.withAttributes(attributes);
    }

    private static @NonNull List<RadiusAttribute> withPasswordAttribute(@NonNull Dictionary dictionary, @NonNull List<RadiusAttribute> attributes, @NonNull String password) {
        var newAttributes = attributes.stream()
                .filter(a -> a.getVendorId() != -1 || a.getType() != USER_PASSWORD)
                .collect(toList());

        newAttributes.add(dictionary.createAttribute(-1, USER_PASSWORD, password.getBytes(UTF_8)));
        return newAttributes;
    }

    /**
     * Retrieves the plain-text user password.
     *
     * @return user password in plaintext if decoded
     */
    public @NonNull Optional<String> getPassword() {
        return getAttribute(-1, USER_PASSWORD)
                .map(RadiusAttribute::getValueString);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateAttributes() throws RadiusPacketException {
        int count = getAttributes(USER_PASSWORD).size();
        if (count != 1)
            throw new RadiusPacketException("AccessRequest (PAP) should have exactly one User-Password attribute, has " + count);
    }
}

