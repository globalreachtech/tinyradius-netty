package org.tinyradius.attribute.type;

import org.tinyradius.attribute.AttributeTemplate;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.util.RadiusPacketException;

import javax.xml.bind.DatatypeConverter;
import java.nio.ByteBuffer;
import java.util.*;

import static java.util.Objects.requireNonNull;

/**
 * The basic generic Radius attribute. All type-specific implementations extend this class
 * by adding additional type conversion methods and validations.
 */
public class OctetsAttribute implements RadiusAttribute {

    private final Dictionary dictionary;
    private final byte type;
    private final byte[] value;

    private final int vendorId; // for Vendor-Specific sub-attributes, otherwise -1

    /**
     * @param dictionary dictionary to use
     * @param vendorId   vendor ID or -1
     * @param type       attribute type code
     * @param value      value of attribute as byte array, excluding type and length bytes
     */
    public OctetsAttribute(Dictionary dictionary, int vendorId, byte type, byte[] value) {
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
    public OctetsAttribute(Dictionary dictionary, int vendorId, byte type, String value) {
        this(dictionary, vendorId, type, DatatypeConverter.parseHexBinary(value));
    }

    @Override
    public int getVendorId() {
        return vendorId;
    }

    @Override
    public byte getType() {
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
    public byte[] toByteArray() {
        final int len = getValue().length + 2;
        return ByteBuffer.allocate(len)
                .put(getType())
                .put((byte) len)
                .put(getValue())
                .array();
    }

    @Override
    public String toString() {
        return getAttributeName() + ": " + getValueString();
    }

    @Override
    public String getAttributeName() {
        return getAttributeTemplate()
                .map(AttributeTemplate::getName)
                .orElse(getVendorId() != -1 ?
                        "Unknown-Sub-Attribute-" + getType() :
                        "Unknown-Attribute-" + getType());
    }

    @Override
    public List<RadiusAttribute> flatten() {
        return Collections.singletonList(this);
    }

    @Override
    public Optional<AttributeTemplate> getAttributeTemplate() {
        return dictionary.getAttributeTemplate(getVendorId(), getType());
    }

    @Override
    public RadiusAttribute encode(byte[] requestAuth, String secret) throws RadiusPacketException {
        final Optional<AttributeTemplate> template = getAttributeTemplate();
        return template.isPresent() ?
                template.get().encode(this, requestAuth, secret) :
                this;
    }

    @Override
    public RadiusAttribute decode(byte[] requestAuth, String secret) {
        return this;
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
