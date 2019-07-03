package org.tinyradius.attribute;

import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.util.RadiusException;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * This class represents a Radius attribute which only contains a string.
 */
public class StringAttribute extends RadiusAttribute {

    public static StringAttribute parse(Dictionary dictionary, int vendorId, byte[] data, int offset) throws RadiusException {
        return new StringAttribute(dictionary, readType(data, offset), vendorId, readData(data, offset));
    }

    public StringAttribute(Dictionary dictionary, int attributeType, int vendorId, byte[] data) {
        super(dictionary, attributeType, vendorId, data);
    }

    public StringAttribute(Dictionary dictionary, int type, int vendorId, String value) {
        this(dictionary, type, vendorId, value.getBytes(UTF_8));
    }

    /**
     * Returns the string value of this attribute.
     *
     * @return a string
     */
    @Override
    public String getAttributeValue() {
        return new String(getAttributeData(), UTF_8);
    }
}
