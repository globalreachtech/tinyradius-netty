package org.tinyradius.attribute;

import org.junit.jupiter.api.Test;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.tinyradius.attribute.Attributes.create;

class VendorSpecificAttributeTest {

    private static final Dictionary dictionary = DefaultDictionary.INSTANCE;

    @Test
    void addSubAttributeOk() {
        String data = "myLocationId";
        VendorSpecificAttribute vendorSpecificAttribute = new VendorSpecificAttribute(dictionary, 14122, 2, data);
        vendorSpecificAttribute.addAttribute(Attributes.create(dictionary, 14122, 2, data));

        assertEquals(1, vendorSpecificAttribute.getAttributes().size());
        assertEquals(data, vendorSpecificAttribute.getAttribute(2).getValueString());
    }

    @Test
    void parseVendorIdZero() {
        VendorSpecificAttribute vendorSpecificAttribute =
                new VendorSpecificAttribute(dictionary, 1, 1, new byte[4]);

        assertEquals(26, vendorSpecificAttribute.getType());
        assertEquals(0, vendorSpecificAttribute.getVendorId());
    }

    @Test
    void parseVendorIdUnsignedIntMax() {
        final byte[] bytes = {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff};
        VendorSpecificAttribute vendorSpecificAttribute =
                new VendorSpecificAttribute(dictionary, 1, 1, bytes);

        assertEquals(26, vendorSpecificAttribute.getType());
        assertEquals(-1, vendorSpecificAttribute.getVendorId());
    }

    @Test
    void getVsaSubAttributeValueStringByName() {
        VendorSpecificAttribute vendorSpecificAttribute = new VendorSpecificAttribute(dictionary, 14122, new ArrayList<>());
        vendorSpecificAttribute.addAttribute("WISPr-Location-ID", "myLocationId");

        assertFalse(vendorSpecificAttribute.getAttributes().isEmpty());
        assertEquals("myLocationId", vendorSpecificAttribute.getAttribute("WISPr-Location-ID").getValueString());
    }

    @Test
    void addNonVsaSubAttribute() {
        VendorSpecificAttribute vendorSpecificAttribute = new VendorSpecificAttribute(dictionary, 14122, new ArrayList<>());
        Exception exception = assertThrows(RuntimeException.class, () -> vendorSpecificAttribute.addAttribute("User-Name", "test1"));
        assertTrue(exception.getMessage().toLowerCase().contains("vendor id doesn't match"));
    }

    @Test
    void addEmptySubAttribute() {
        VendorSpecificAttribute vendorSpecificAttribute = new VendorSpecificAttribute(dictionary, 14122, new ArrayList<>());
        Exception exception = assertThrows(RuntimeException.class, () -> vendorSpecificAttribute.addAttribute("", "myLocationId"));
        assertTrue(exception.getMessage().toLowerCase().contains("type name is empty"));
    }

    @Test
    void vsaToByteArray() {
        VendorSpecificAttribute vendorSpecificAttribute = new VendorSpecificAttribute(dictionary, 14122, new ArrayList<>());
        RadiusAttribute radiusAttribute = new RadiusAttribute(dictionary, 14122, 26, new byte[8]);
        vendorSpecificAttribute.addAttribute(radiusAttribute);
        vendorSpecificAttribute.addAttribute("WISPr-Location-ID", "myLocationId");
        assertEquals(2, vendorSpecificAttribute.getAttributes().size());

        byte[] bytes = vendorSpecificAttribute.toByteArray();
        assertEquals(bytes.length, bytes[1]);
    }

    @Test
    void vsaToByteArrayLargestUnsignedVendorId() {
        RadiusAttribute radiusAttribute = create(dictionary, Integer.parseUnsignedInt("4294967295"), 1, new byte[4]);
        VendorSpecificAttribute vendorSpecificAttribute = new VendorSpecificAttribute(dictionary, Integer.parseUnsignedInt("4294967295"), new ArrayList<>());
        vendorSpecificAttribute.addAttribute(radiusAttribute);
        assertEquals(1, vendorSpecificAttribute.getAttributes().size());

        byte[] bytes = vendorSpecificAttribute.toByteArray();
        assertEquals(12, bytes.length);
        assertEquals(-1, ByteBuffer.wrap(bytes).getInt(2));
        // int unsigned max == -1 signed
    }

    @Test
    void vsaToByteArrayTooLong() {
        VendorSpecificAttribute vendorSpecificAttribute = new VendorSpecificAttribute(dictionary, 14122, new ArrayList<>());
        vendorSpecificAttribute.addAttribute(new RadiusAttribute(dictionary, 14122, 26, new byte[253]));
        vendorSpecificAttribute.addAttribute("WISPr-Location-ID", "myLocationId");
        assertEquals(2, vendorSpecificAttribute.getAttributes().size());

        Exception exception = assertThrows(RuntimeException.class, vendorSpecificAttribute::toByteArray);
        assertTrue(exception.getMessage().toLowerCase().contains("should be less than 256 octets"));
    }

    @Test
    void vsaToByteArrayWithNoSubAttributes() {
        VendorSpecificAttribute vendorSpecificAttribute = new VendorSpecificAttribute(dictionary, 14122, new ArrayList<>());
        Exception exception = assertThrows(RuntimeException.class, vendorSpecificAttribute::toByteArray);
        assertTrue(exception.getMessage().toLowerCase().contains("should be greater than 6 octets"));
    }

    @Test
    void testToMap() {
        VendorSpecificAttribute vendorSpecificAttribute = new VendorSpecificAttribute(dictionary, 14122, new ArrayList<>());
        vendorSpecificAttribute.addAttribute("WISPr-Location-ID", "myLocationId");
        vendorSpecificAttribute.addAttribute("WISPr-Location-Name", "myLocationName");

        assertEquals("{WISPr-Location-Name=myLocationName, WISPr-Location-ID=myLocationId}", vendorSpecificAttribute.getAttributeMap().toString());
    }

    @Test
    void testToString() {
        VendorSpecificAttribute vendorSpecificAttribute = new VendorSpecificAttribute(dictionary, 14122, new ArrayList<>());
        vendorSpecificAttribute.addAttribute("WISPr-Location-ID", "myLocationId");
        vendorSpecificAttribute.addAttribute("WISPr-Location-Name", "myLocationName");

        assertEquals("Vendor-Specific: WISPr (14122)\n" +
                "  WISPr-Location-ID: myLocationId\n" +
                "  WISPr-Location-Name: myLocationName", vendorSpecificAttribute.toString());
    }
}