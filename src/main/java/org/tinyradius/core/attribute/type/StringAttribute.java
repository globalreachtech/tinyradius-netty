package org.tinyradius.core.attribute.type;

import org.tinyradius.core.dictionary.Dictionary;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * This class represents a Radius attribute which only contains a string.
 */
public class StringAttribute extends OctetsAttribute {

    public StringAttribute(Dictionary dictionary, int vendorId, int type, byte tag, byte[] value) {
        super(dictionary, vendorId, type, tag,value);
        if (value.length == 0)
            throw new IllegalArgumentException("String attribute value should be min 1 octet, actual: " + value.length);
    }

    public StringAttribute(Dictionary dictionary, int vendorId, int type, byte tag, String value) {
        this(dictionary, vendorId, type,tag, value.getBytes(UTF_8));
    }

    @Override
    public String getValueString() {
        return new String(getValue(), UTF_8);
    }
}
