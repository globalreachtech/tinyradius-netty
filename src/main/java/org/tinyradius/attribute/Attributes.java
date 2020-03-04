package org.tinyradius.attribute;

import org.tinyradius.dictionary.Dictionary;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.Byte.toUnsignedInt;

/**
 * Helper class to create and extract attributes.
 */
public class Attributes {

    /**
     * Creates a RadiusAttribute object of the appropriate type by looking up type and vendorId.
     *
     * @param dictionary Dictionary to use
     * @param vendorId   vendor ID or -1
     * @param type       attribute type
     * @param data       attribute data as byte array
     * @return RadiusAttribute object
     */
    public static RadiusAttribute create(Dictionary dictionary, int vendorId, byte type, byte[] data) {
        final AttributeType attributeType = dictionary.getAttributeTypeByCode(vendorId, type);
        if (attributeType != null)
            return attributeType.create(dictionary, data);

        return new RadiusAttribute(dictionary, vendorId, type, data);
    }

    /**
     * Creates a RadiusAttribute object of the appropriate type by looking up type and vendorId.
     *
     * @param dictionary Dictionary to use
     * @param vendorId   vendor ID or -1
     * @param type       attribute type
     * @param data       attribute data as String
     * @return RadiusAttribute object
     */
    public static RadiusAttribute create(Dictionary dictionary, int vendorId, byte type, String data) {
        final AttributeType attributeType = dictionary.getAttributeTypeByCode(vendorId, type);
        if (attributeType != null)
            return attributeType.create(dictionary, data);

        return new RadiusAttribute(dictionary, vendorId, type, data);
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
            throw new IllegalArgumentException("Attribute malformed, lengths do not match");
        return attributes;
    }
}
