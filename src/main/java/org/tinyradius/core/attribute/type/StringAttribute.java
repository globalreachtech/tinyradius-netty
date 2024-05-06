package org.tinyradius.core.attribute.type;

import io.netty.buffer.ByteBuf;
import org.tinyradius.core.dictionary.Dictionary;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * This class represents a Radius attribute which only contains a string.
 */
public class StringAttribute extends OctetsAttribute {

    public static final RadiusAttributeFactory<StringAttribute> FACTORY = new Factory();

    public StringAttribute(Dictionary dictionary, int vendorId, ByteBuf data) {
        super(dictionary, vendorId, data);
        if (!data.isReadable(3))
            throw new IllegalArgumentException("String attribute value should be min 3 octets, actual: " + data.readableBytes());
    }

    @Override
    public String getValueString() {
        return new String(getValue(), UTF_8);
    }

    public static byte[] stringParser(String s) {
        return s.getBytes(UTF_8);
    }

    private static class Factory implements RadiusAttributeFactory<StringAttribute> {

        @Override
        public StringAttribute newInstance(Dictionary dictionary, int vendorId, ByteBuf value) {
            return new StringAttribute(dictionary, vendorId, value);
        }

        @Override
        public byte[] parse(Dictionary dictionary, int vendorId, int type, String value) {
            return stringParser(value);
        }
    }

}
