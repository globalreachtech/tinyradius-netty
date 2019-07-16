package org.tinyradius.attribute;

import org.tinyradius.dictionary.Dictionary;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.Byte.toUnsignedInt;

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
    public static RadiusAttribute createAttribute(Dictionary dictionary, int vendorId, int type, byte[] data) {
        final ByteArrayConstructor byteArrayConstructor = dictionary.getAttributeTypeByCode(vendorId, type).getByteArrayConstructor();
        return byteArrayConstructor.newInstance(dictionary, vendorId, type, data);
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
    public static RadiusAttribute createAttribute(Dictionary dictionary, int vendorId, int type, String data) {
        final StringConstructor stringConstructor = dictionary.getAttributeTypeByCode(vendorId, type).getStringConstructor();
        return stringConstructor.newInstance(dictionary, vendorId, type, data);
    }

    public static List<RadiusAttribute> extractAttributes(Dictionary dictionary, int vendorId, byte[] data, int pos) {
        final ArrayList<RadiusAttribute> attributes = new ArrayList<>();

        // at least 2 octets left
        while (data.length - pos >= 2) {
            final int type = toUnsignedInt(data[pos]);
            final int length = toUnsignedInt(data[pos + 1]); // max 255
            final int expectedLen = length - 2;
            if (expectedLen < 0)
                throw new IllegalArgumentException("invalid attribute length " + length + ", must be >=2");
            if (expectedLen > data.length - pos)
                throw new IllegalArgumentException("invalid attribute length " + length + ", remaining bytes " + (data.length - pos));
            attributes.add(createAttribute(dictionary, vendorId, type, Arrays.copyOfRange(data, pos + 2, pos + length)));
            pos += length;
        }

        if (pos != data.length)
            throw new IllegalArgumentException("attribute malformed");
        return attributes;
    }

    public interface ByteArrayConstructor<T extends RadiusAttribute> {
        T newInstance(Dictionary dictionary, int vendorId, int type, byte[] data);
    }

    public interface StringConstructor<T extends RadiusAttribute> {
        T newInstance(Dictionary dictionary, int vendorId, int type, String data);
    }
}
