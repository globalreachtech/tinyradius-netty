package org.tinyradius.attribute;

import org.tinyradius.dictionary.Dictionary;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * This class represents a Radius attribute which only contains a string.
 */
public class StringAttribute extends RadiusAttribute {

    // todo make public and add docs to recommend using builder
    StringAttribute(Dictionary dictionary, int vendorId, int type, byte[] data) {
        super(dictionary, vendorId, type, data);
        if (data.length < 1)
            throw new IllegalArgumentException("String attribute value should be min 1 octet, actual: " + data.length);
    }

    StringAttribute(Dictionary dictionary, int vendorId, int type, String value) {
        this(dictionary, vendorId, type, value.getBytes(UTF_8));
    }

    @Override
    public String getDataString() {
        return new String(getData(), UTF_8);
    }
}
