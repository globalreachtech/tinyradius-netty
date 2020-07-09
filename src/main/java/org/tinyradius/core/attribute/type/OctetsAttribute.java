package org.tinyradius.core.attribute.type;

import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.attribute.AttributeTemplate;
import org.tinyradius.core.dictionary.Dictionary;

import javax.xml.bind.DatatypeConverter;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * The basic generic Radius attribute. All type-specific implementations extend this class
 * by adding additional type conversion methods and validations.
 */
public class OctetsAttribute implements RadiusAttribute {

    private final Dictionary dictionary;
    private final int type;
    private final byte[] value;

    private final int vendorId; // for Vendor-Specific sub-attributes, otherwise -1

    /**
     * @param dictionary dictionary to use
     * @param vendorId   vendor ID or -1
     * @param type       attribute type code
     * @param value      value of attribute as byte array, excluding type and length bytes
     */
    public OctetsAttribute(Dictionary dictionary, int vendorId, int type, byte[] value) {
        this.dictionary = requireNonNull(dictionary, "Dictionary not set");
        this.vendorId = vendorId;
        this.type = type;
        this.value = requireNonNull(value, "Attribute data not set");
        if (value.length > 253)
            throw new IllegalArgumentException("Attribute data too long, max 253 octets, actual: " + value.length);
    }

    /**
     * @param dictionary dictionary to use
     * @param vendorId   vendor ID or -1
     * @param type       attribute type code
     * @param value      value of attribute as hex string
     */
    public OctetsAttribute(Dictionary dictionary, int vendorId, int type, String value) {
        this(dictionary, vendorId, type, DatatypeConverter.parseHexBinary(value));
    }

    @Override
    public int getVendorId() {
        return vendorId;
    }

    @Override
    public int getType() {
        return type;
    }

    @Override
    public byte getTag() {
        return 0;
    }

    @Override
    public byte[] getValue() {
        return value;
    }

    @Override
    public String getValueString() {
        return DatatypeConverter.printHexBinary(value);
    }

    @Override
    public Dictionary getDictionary() {
        return dictionary;
    }

    @Override
    public String toString() {
        return getAttributeName() + ": " + getValueString();
    }

    @Override
    public RadiusAttribute encode(byte[] requestAuth, String secret) throws RadiusPacketException {
        final Optional<AttributeTemplate> template = getAttributeTemplate();
        return template.isPresent() ?
                template.get().encode(this, requestAuth, secret) :
                this;
    }

    // do not remove - for removing from list of attributes
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OctetsAttribute that = (OctetsAttribute) o;
        return type == that.type &&
                vendorId == that.vendorId &&
                Arrays.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(type, vendorId);
        result = 31 * result + Arrays.hashCode(value);
        return result;
    }
}
