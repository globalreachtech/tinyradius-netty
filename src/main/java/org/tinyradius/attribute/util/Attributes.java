package org.tinyradius.attribute.util;

import org.tinyradius.attribute.RadiusAttribute;
import org.tinyradius.dictionary.Dictionary;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.Byte.toUnsignedInt;

/**
 * Helper class to create and extract attributes.
 */
public class Attributes {

    private Attributes() {
    }

    /**
     * Creates a RadiusAttribute object of the appropriate type by looking up type and vendorId.
     *
     * @param dictionary Dictionary to use
     * @param vendorId   vendor ID or -1
     * @param type       attribute type
     * @param value      attribute data as byte array
     * @return RadiusAttribute object
     */
    public static RadiusAttribute create(Dictionary dictionary, int vendorId, byte type, byte[] value) {
        final AttributeType attributeType = dictionary.getAttributeTypeByCode(vendorId, type);
        if (attributeType != null)
            return attributeType.create(dictionary, value);

        return new RadiusAttribute(dictionary, vendorId, type, value);
    }

    /**
     * Creates a RadiusAttribute object of the appropriate type by looking up type and vendorId.
     *
     * @param dictionary Dictionary to use
     * @param vendorId   vendor ID or -1
     * @param type       attribute type
     * @param value      attribute data as String
     * @return RadiusAttribute object
     */
    public static RadiusAttribute create(Dictionary dictionary, int vendorId, byte type, String value) {
        final AttributeType attributeType = dictionary.getAttributeTypeByCode(vendorId, type);
        if (attributeType != null)
            return attributeType.create(dictionary, value);

        return new RadiusAttribute(dictionary, vendorId, type, value);
    }

    /**
     * Creates a Radius attribute.
     * Uses AttributeTypes to lookup the type code and converts the value.
     *
     * @param name  name of the attribute, for example "NAS-IP-Address", should NOT be 'Vendor-Specific'
     * @param value value of the attribute, for example "127.0.0.1"
     * @throws IllegalArgumentException if type name or value is invalid
     */
    public static RadiusAttribute create(Dictionary dictionary, String name, String value) {
        if (name == null || name.isEmpty())
            throw new IllegalArgumentException("Type name is null/empty");
        if (value == null || value.isEmpty())
            throw new IllegalArgumentException("Value is null/empty");

        final AttributeType type = dictionary.getAttributeTypeByName(name);
        if (type == null)
            throw new IllegalArgumentException("Unknown attribute type name'" + name + "'");

        return type.create(dictionary, value);
    }

    /**
     * @param dictionary dictionary to create attribute
     * @param vendorId   vendor Id to set attributes
     * @param data       byte array to parse
     * @param pos        position in byte array at which to parse
     * @return list of RadiusAttributes
     */
    public static List<RadiusAttribute> extractAttributes(Dictionary dictionary, int vendorId, byte[] data, int pos) {
        final ArrayList<RadiusAttribute> attributes = new ArrayList<>();

        // at least 2 octets left
        while (data.length - pos >= 2) {
            final byte type = data[pos];
            final int length = toUnsignedInt(data[pos + 1]); // max 255
            final int expectedLen = length - 2;
            if (expectedLen < 0)
                throw new IllegalArgumentException("Invalid attribute length " + length + ", must be >=2");
            if (expectedLen > data.length - pos)
                throw new IllegalArgumentException("Invalid attribute length " + length + ", remaining bytes " + (data.length - pos));
            attributes.add(create(dictionary, vendorId, type, Arrays.copyOfRange(data, pos + 2, pos + length)));
            pos += length;
        }

        if (pos != data.length)
            throw new IllegalArgumentException("Attribute malformed, lengths do not match, " +
                    "parse position " + pos + ", bytes length " + data.length);
        return attributes;
    }
}
