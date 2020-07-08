package org.tinyradius.core.dictionary;

import java.util.Objects;

public class Vendor {

    private final int id;
    private final String name;
    private final int typeLength;
    private final int lengthLength;

    public Vendor(int id, String name, int typeLength, int lengthLength) {
        this.id = id;
        this.name = Objects.requireNonNull(name);
        this.typeLength = typeLength;
        this.lengthLength = lengthLength;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getTypeLength() {
        return typeLength;
    }

    public int getLengthLength() {
        return lengthLength;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Vendor)) return false;
        final Vendor vendor = (Vendor) o;
        return id == vendor.id &&
                typeLength == vendor.typeLength &&
                lengthLength == vendor.lengthLength &&
                name.equals(vendor.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, typeLength, lengthLength);
    }

    @Override
    public String toString() {
        return "Vendor{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", typeLength=" + typeLength +
                ", lengthLength=" + lengthLength +
                '}';
    }
}
