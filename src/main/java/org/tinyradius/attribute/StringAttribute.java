package org.tinyradius.attribute;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

/**
 * This class represents a Radius attribute which only contains a string.
 */
public class StringAttribute extends RadiusAttribute {

    /**
     * Constructs an empty string attribute.
     */
    public StringAttribute(int attributeType, int vendorId) {
        super(attributeType, vendorId);
    }

    public StringAttribute(int type, int vendorId, String value) {
        this(type, vendorId);
        setAttributeValue(value);
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

    /**
     * Sets the string value of this attribute.
     *
     * @param value string, not null
     */
    @Override
    public void setAttributeValue(String value) {
        requireNonNull(value, "string value not set");
        setAttributeData(value.getBytes(UTF_8));
    }

}
