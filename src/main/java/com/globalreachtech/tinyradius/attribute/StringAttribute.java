package com.globalreachtech.tinyradius.attribute;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

/**
 * This class represents a Radius attribute which only
 * contains a string.
 */
public class StringAttribute extends RadiusAttribute {

	/**
	 * Constructs a string attribute with the given value.
	 * @param type attribute type
	 * @param value attribute value
	 */
	public StringAttribute(int type, String value) {
		setAttributeType(type);
		setAttributeValue(value);
	}
	
	/**
	 * Returns the string value of this attribute.
	 * @return a string
	 */
	public String getAttributeValue() {
		return new String(getAttributeData(), UTF_8);
	}
	
	/**
	 * Sets the string value of this attribute.
	 * @param value string, not null
	 */
	public void setAttributeValue(String value) {
		requireNonNull(value, "string value not set");
		setAttributeData(value.getBytes(UTF_8));
	}
	
}
