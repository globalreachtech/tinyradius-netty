package org.tinyradius.attribute;

import org.tinyradius.attribute.type.RadiusAttribute;
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
            attributes.add(dictionary.createAttribute(vendorId, type, Arrays.copyOfRange(data, pos + 2, pos + length)));
            pos += length;
        }

        if (pos != data.length)
            throw new IllegalArgumentException("Attribute malformed, lengths do not match, " +
                    "parse position " + pos + ", bytes length " + data.length);
        return attributes;
    }
}
