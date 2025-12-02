package org.tinyradius.core.dictionary;

import org.jspecify.annotations.NonNull;

import java.nio.ByteBuffer;

/**
 * Vendor definition
 *
 * @param id         Vendor ID
 * @param name       Vendor Name
 * @param typeSize   number of octets for vendor 'type' field
 * @param lengthSize number of octets for vendor 'length' field
 */
public record Vendor(int id, @NonNull String name, int typeSize, int lengthSize) {

    public Vendor {
        if (id < 0)
            throw new IllegalArgumentException("Vendor ID must be positive: " + id + " (" + name + ")");
        if (name.isEmpty())
            throw new IllegalArgumentException("Vendor name empty: " + name + " (vendorId" + id + ")");
        if (typeSize != 1 && typeSize != 2 && typeSize != 4)
            throw new IllegalArgumentException("Vendor typeSize must be 1, 2, or 4");
        if (lengthSize != 0 && lengthSize != 1 && lengthSize != 2)
            throw new IllegalArgumentException("Vendor lengthSize must be 0, 1, or 2");
    }

    public int getHeaderSize() {
        return typeSize + lengthSize;
    }

    public byte[] toTypeBytes(int type) {
        return switch (typeSize) {
            case 2 -> ByteBuffer.allocate(Short.BYTES).putShort((short) type).array();
            case 4 -> ByteBuffer.allocate(Integer.BYTES).putInt(type).array();
            default -> new byte[]{(byte) type};
        };
    }

    public byte[] toLengthBytes(int len) {
        return switch (lengthSize) {
            case 0 -> new byte[0];
            case 2 -> ByteBuffer.allocate(Short.BYTES).putShort((short) len).array();
            default -> new byte[]{(byte) len};
        };
    }
}
