package org.tinyradius.attribute;

import org.junit.jupiter.api.Test;
import org.tinyradius.dictionary.DefaultDictionary;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.tinyradius.attribute.Attributes.createAttribute;

class VendorSpecificAttributeTest {

    private static DefaultDictionary dictionary = DefaultDictionary.INSTANCE;

    @Test
    void addSubAttributeOk() {
        String data = "myLocationId";
        VendorSpecificAttribute vendorSpecificAttribute = new VendorSpecificAttribute(dictionary, 14122, 2, data);
        vendorSpecificAttribute.addSubAttribute(createAttribute(dictionary, 14122, 2, data));

        assertEquals(1, vendorSpecificAttribute.getSubAttributes().size());
        assertEquals(data, vendorSpecificAttribute.getSubAttribute(2).getValueString());
    }

    @Test
    void parseVendorIdZero() {
        VendorSpecificAttribute vendorSpecificAttribute =
                new VendorSpecificAttribute(DefaultDictionary.INSTANCE, 1, 1, new byte[4]);

        assertEquals(26, vendorSpecificAttribute.getType());
        assertEquals(0, vendorSpecificAttribute.getVendorId());
    }

    @Test
    void parseVendorIdUnsignedIntMax() {
        final byte[] bytes = {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff};
        VendorSpecificAttribute vendorSpecificAttribute =
                new VendorSpecificAttribute(DefaultDictionary.INSTANCE, 1, 1, bytes);

        assertEquals(26, vendorSpecificAttribute.getType());
        assertEquals(-1, vendorSpecificAttribute.getVendorId());
    }

    @Test
    void getVsaSubAttributeValueStringByName() {
        VendorSpecificAttribute vendorSpecificAttribute = new VendorSpecificAttribute(dictionary, 14122, new ArrayList<>());
        vendorSpecificAttribute.addSubAttribute("WISPr-Location-ID", "myLocationId");

        assertFalse(vendorSpecificAttribute.getSubAttributes().isEmpty());
        assertEquals("myLocationId", vendorSpecificAttribute.getSubAttribute("WISPr-Location-ID").getValueString());
    }

    @Test
    void addNonVsaSubAttribute() {
        VendorSpecificAttribute vendorSpecificAttribute = new VendorSpecificAttribute(dictionary, 14122, new ArrayList<>());
        Exception exception = assertThrows(RuntimeException.class, () -> vendorSpecificAttribute.addSubAttribute("User-Password", "password"));
        assertTrue(exception.getMessage().toLowerCase().contains("is not a vendor-specific sub-attribute"));
    }

    @Test
    void addEmptySubAttribute() {
        VendorSpecificAttribute vendorSpecificAttribute = new VendorSpecificAttribute(dictionary, 14122, new ArrayList<>());
        Exception exception = assertThrows(RuntimeException.class, () -> vendorSpecificAttribute.addSubAttribute("", "myLocationId"));
        assertTrue(exception.getMessage().toLowerCase().contains("type name is empty"));
    }

    @Test
    void vsaToByteArray() {
        VendorSpecificAttribute vendorSpecificAttribute = new VendorSpecificAttribute(dictionary, 14122, new ArrayList<>());
        RadiusAttribute radiusAttribute = new RadiusAttribute(dictionary, 14122, 26, new byte[8]);
        vendorSpecificAttribute.addSubAttribute(radiusAttribute);
        vendorSpecificAttribute.addSubAttribute("WISPr-Location-ID", "myLocationId");
        assertEquals(2, vendorSpecificAttribute.getSubAttributes().size());

        byte[] bytes = vendorSpecificAttribute.toByteArray();
        assertEquals(bytes.length, bytes[1]);
    }

    @Test
    void vsaToByteArrayLargestUnsignedVendorId() {
        RadiusAttribute radiusAttribute = createAttribute(DefaultDictionary.INSTANCE, Integer.parseUnsignedInt("4294967295"), 1, new byte[4]);
        VendorSpecificAttribute vendorSpecificAttribute = new VendorSpecificAttribute(dictionary, Integer.parseUnsignedInt("4294967295"), new ArrayList<>());
        vendorSpecificAttribute.addSubAttribute(radiusAttribute);
        assertEquals(1, vendorSpecificAttribute.getSubAttributes().size());

        byte[] bytes = vendorSpecificAttribute.toByteArray();
        assertEquals(12, bytes.length);
        assertEquals(-1, ByteBuffer.wrap(bytes).getInt(2));
        // int unsigned max == -1 signed
    }

    @Test
    void vsaToByteArrayTooLong() {
        VendorSpecificAttribute vendorSpecificAttribute = new VendorSpecificAttribute(dictionary, 14122, new ArrayList<>());
        vendorSpecificAttribute.addSubAttribute(new RadiusAttribute(dictionary, 14122, 26, new byte[253]));
        vendorSpecificAttribute.addSubAttribute("WISPr-Location-ID", "myLocationId");
        assertEquals(2, vendorSpecificAttribute.getSubAttributes().size());

        Exception exception = assertThrows(RuntimeException.class, vendorSpecificAttribute::toByteArray);
        exception.printStackTrace();
        assertTrue(exception.getMessage().toLowerCase().contains("should be less than 256 octets"));
    }

    @Test
    void vsaToByteArrayWithNoSubAttributes() {
        VendorSpecificAttribute vendorSpecificAttribute = new VendorSpecificAttribute(dictionary, 14122, new ArrayList<>());
        Exception exception = assertThrows(RuntimeException.class, vendorSpecificAttribute::toByteArray);
        assertTrue(exception.getMessage().toLowerCase().contains("should be greater than 6 octets"));
    }
}