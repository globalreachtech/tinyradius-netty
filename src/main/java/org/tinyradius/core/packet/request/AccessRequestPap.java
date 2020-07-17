package org.tinyradius.core.packet.request;

import io.netty.buffer.ByteBuf;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.attribute.type.RadiusAttribute;
import org.tinyradius.core.dictionary.Dictionary;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * PAP AccessRequest RFC2865
 */
public class AccessRequestPap extends AccessRequest {

    public AccessRequestPap(Dictionary dictionary, ByteBuf header, List<RadiusAttribute> attributes) throws RadiusPacketException {
        super(dictionary, header, attributes);
    }

    AccessRequestPap(Dictionary dictionary, ByteBuf header, List<RadiusAttribute> attributes, String password) throws RadiusPacketException {
        super(dictionary, header, appendPasswordAttributes(dictionary, attributes, password));
    }

    public static List<RadiusAttribute> appendPasswordAttributes(Dictionary dictionary, List<RadiusAttribute> attributes, String password) throws RadiusPacketException {
        final List<RadiusAttribute> newAttributes = attributes.stream()
                .filter(a -> a.getVendorId() != -1 || a.getType() != USER_PASSWORD)
                .collect(Collectors.toList());

        newAttributes.add(dictionary.createAttribute(-1, USER_PASSWORD, password.getBytes(UTF_8)));

        return newAttributes;
    }

    /**
     * Retrieves the plain-text user password.
     *
     * @return user password in plaintext if decoded
     */
    public Optional<String> getPassword() {
        return getAttribute(-1, USER_PASSWORD)
                .map(RadiusAttribute::getValue)
                .map(v -> new String(v, UTF_8));
    }

    @Override
    public RadiusRequest encodeRequest(String sharedSecret) throws RadiusPacketException {
        checkUserPassword();
        return super.encodeRequest(sharedSecret);
    }

    @Override
    public RadiusRequest decodeRequest(String sharedSecret) throws RadiusPacketException {
        checkUserPassword();
        return super.decodeRequest(sharedSecret);
    }

    private void checkUserPassword() throws RadiusPacketException {
        final int count = filterAttributes(USER_PASSWORD).size();
        if (count != 1)
            throw new RadiusPacketException("AccessRequest (PAP) should have exactly one User-Password attribute, has " + count);
    }
}
