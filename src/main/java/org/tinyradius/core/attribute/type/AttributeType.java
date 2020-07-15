package org.tinyradius.core.attribute.type;

import org.tinyradius.core.dictionary.Dictionary;

public enum AttributeType {
    VSA((dictionary, vendorId, attributeId, tag, data) -> {
        return new VendorSpecificAttribute(dictionary, vendorId, attributeId, data);
    }, (dictionary, vendorId, attributeId, tag, data) -> {
        return new VendorSpecificAttribute(dictionary, vendorId, attributeId, data);
    }),
    OCTETS(OctetsAttribute::new, OctetsAttribute::new),
    STRING(StringAttribute::new, StringAttribute::new),
    INTEGER(IntegerAttribute::new, IntegerAttribute::new),
    IPV4(IpAttribute.V4::new, IpAttribute.V4::new),
    IPV6(IpAttribute.V6::new, IpAttribute.V6::new),
    IPV6_PREFIX(Ipv6PrefixAttribute::new, Ipv6PrefixAttribute::new);

    private final ByteArrayConstructor byteArrayConstructor;
    private final StringConstructor stringConstructor;

    AttributeType(ByteArrayConstructor byteArrayConstructor, StringConstructor stringConstructor) {
        this.byteArrayConstructor = byteArrayConstructor;
        this.stringConstructor = stringConstructor;
    }

    public OctetsAttribute create(Dictionary dictionary, int vendorId, int type, byte tag, byte[] value) {
        return byteArrayConstructor.newInstance(dictionary, vendorId, type, tag, value);
    }

    public OctetsAttribute create(Dictionary dictionary, int vendorId, int type, byte tag, String value) {
        return stringConstructor.newInstance(dictionary, vendorId, type, tag, value);
    }

    public static AttributeType fromDataType(String dataType) {
        switch (dataType) {
            case "vsa":
                return AttributeType.VSA;
            case "string":
                return AttributeType.STRING;
            case "integer":
            case "date":
                return AttributeType.INTEGER;
            case "ipaddr":
                return AttributeType.IPV4;
            case "ipv6addr":
                return AttributeType.IPV6;
            case "ipv6prefix":
                return AttributeType.IPV6_PREFIX;
            case "octets":
            case "ifid":
            case "integer64":
            case "ether":
            case "abinary":
            case "byte":
            case "short":
            case "signed":
            case "tlv":
            case "ipv4prefix":
            default:
                return AttributeType.OCTETS;
        }
    }

    private interface ByteArrayConstructor {
        OctetsAttribute newInstance(Dictionary dictionary, int vendorId, int type, byte tag, byte[] data);
    }

    private interface StringConstructor {
        OctetsAttribute newInstance(Dictionary dictionary, int vendorId, int type, byte tag, String data);
    }
}
