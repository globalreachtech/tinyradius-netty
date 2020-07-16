package org.tinyradius.core.attribute.type;

import io.netty.buffer.ByteBuf;
import org.tinyradius.core.dictionary.Dictionary;

import java.util.Optional;

public class AnonSubAttribute implements RadiusAttribute {

    private final Dictionary dictionary;

    private final ByteBuf data;
    private final int vendorId; // for Vendor-Specific sub-attributes, otherwise -1

    public AnonSubAttribute(Dictionary dictionary, int vendorId, ByteBuf data) {
        this.dictionary = dictionary;
        this.vendorId = vendorId;
        this.data = data;
    }

    @Override
    public int getVendorId() {
        return vendorId;
    }

    @Override
    public int getType() {
        return 0;
    }

    @Override
    public Optional<Byte> getTag() {
        return Optional.empty();
    }

    @Override
    public byte[] getValue() {
        return data.copy().array();
    }

    @Override
    public String getValueString() {
        return "Unparsable";
    }

    @Override
    public Dictionary getDictionary() {
        return dictionary;
    }

    @Override
    public ByteBuf getData() {
        return data;
    }

    @Override
    public String toString() {
        return "Unparsable";
    }
}
