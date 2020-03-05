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
        VendorSpecificAttribute vsa = new VendorSpecificAttribute(dictionary, 1234, 1234,
                ByteBuffer.allocate(4).putInt(14122).array());
        vsa.addAttribute(Attributes.create(dictionary, 14122, (byte) 2, data));

        assertEquals(1, vsa.getAttributes().size());
        assertEquals(data, vsa.getAttribute((byte) 2).getValueString());
    }

    @Test
    void parseChildVendorIdZero() {
        VendorSpecificAttribute vsa =
                new VendorSpecificAttribute(dictionary, 1, 1, new byte[4]);

        assertEquals(26, vsa.getType());
        assertEquals(-1, vsa.getVendorId());
        assertEquals(0, vsa.getChildVendorId());
    }

    @Test
    void parseVendorIdUnsignedIntMax() {
        final byte[] bytes = {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff};
        VendorSpecificAttribute vsa =
                new VendorSpecificAttribute(dictionary, 0, 0, bytes);

        assertEquals(26, vsa.getType());
        assertEquals(-1, vsa.getVendorId());
        assertEquals(-1, vsa.getChildVendorId());
    }

    @Test
    void getVsaSubAttributeValueStringByName() {
        VendorSpecificAttribute vsa = new VendorSpecificAttribute(dictionary, new ArrayList<>(), 14122);
        vsa.addAttribute("WISPr-Location-ID", "myLocationId");

        assertFalse(vsa.getAttributes().isEmpty());
        assertEquals("myLocationId", vsa.getAttribute("WISPr-Location-ID").getValueString());
    }

    @Test
    void addNonVsaSubAttribute() {
        VendorSpecificAttribute vsa = new VendorSpecificAttribute(dictionary, new ArrayList<>(), 14122);
        Exception exception = assertThrows(RuntimeException.class, () -> vsa.addAttribute("User-Name", "test1"));
        assertTrue(exception.getMessage().toLowerCase().contains("vendor id doesn't match"));
    }

    @Test
    void addEmptySubAttribute() {
        VendorSpecificAttribute vsa = new VendorSpecificAttribute(dictionary, new ArrayList<>(), 14122);
        Exception exception = assertThrows(RuntimeException.class, () -> vsa.addAttribute("", "myLocationId"));
        assertTrue(exception.getMessage().toLowerCase().contains("type name is null/empty"));
    }

    @Test
    void vsaToByteArray() {
        VendorSpecificAttribute vsa = new VendorSpecificAttribute(dictionary, new ArrayList<>(), 14122);
        RadiusAttribute radiusAttribute = new RadiusAttribute(dictionary, 14122, (byte) 26, new byte[8]);
        vsa.addAttribute(radiusAttribute);
        vsa.addAttribute("WISPr-Location-ID", "myLocationId");
        assertEquals(2, vsa.getAttributes().size());

        byte[] bytes = vsa.toByteArray();
        assertEquals(bytes.length, bytes[1]);
    }

    @Test
    void vsaToByteArrayLargestUnsignedVendorId() {
        RadiusAttribute radiusAttribute = create(dictionary, Integer.parseUnsignedInt("4294967295"), (byte) 1, new byte[4]);
        VendorSpecificAttribute vsa = new VendorSpecificAttribute(dictionary, new ArrayList<>(), Integer.parseUnsignedInt("4294967295"));
        vsa.addAttribute(radiusAttribute);
        assertEquals(1, vsa.getAttributes().size());

        byte[] bytes = vsa.toByteArray();
        assertEquals(12, bytes.length);
        assertEquals(-1, ByteBuffer.wrap(bytes).getInt(2));
        // int unsigned max == -1 signed
    }

    @Test
    void vsaToByteArrayTooLong() {
        VendorSpecificAttribute vendorSpecificAttribute = new VendorSpecificAttribute(dictionary, new ArrayList<>(), 14122);
        vendorSpecificAttribute.addAttribute(new RadiusAttribute(dictionary, 14122, (byte) 26, new byte[253]));
        vendorSpecificAttribute.addAttribute("WISPr-Location-ID", "myLocationId");
        assertEquals(2, vendorSpecificAttribute.getAttributes().size());

        Exception exception = assertThrows(RuntimeException.class, vendorSpecificAttribute::toByteArray);
        assertTrue(exception.getMessage().toLowerCase().contains("should be less than 256 octets"));
    }

    @Test
    void vsaToByteArrayWithNoSubAttributes() {
        VendorSpecificAttribute vsa = new VendorSpecificAttribute(dictionary, new ArrayList<>(), 14122);
        Exception exception = assertThrows(RuntimeException.class, vsa::toByteArray);
        assertTrue(exception.getMessage().toLowerCase().contains("should be greater than 6 octets"));
    }

    @Test
    void testToMap() {
        VendorSpecificAttribute vsa = new VendorSpecificAttribute(dictionary, new ArrayList<>(), 14122);
        vsa.addAttribute("WISPr-Location-ID", "myLocationId");
        vsa.addAttribute("WISPr-Location-Name", "myLocationName");

        assertEquals("{WISPr-Location-Name=myLocationName, WISPr-Location-ID=myLocationId}", vsa.getAttributeMap().toString());
    }

    @Test
    void testToString() {
        VendorSpecificAttribute vsa = new VendorSpecificAttribute(dictionary, new ArrayList<>(), 14122);
        vsa.addAttribute("WISPr-Location-ID", "myLocationId");
        vsa.addAttribute("WISPr-Location-Name", "myLocationName");

        assertEquals("Vendor-Specific: WISPr (14122)\n" +
                "  WISPr-Location-ID: myLocationId\n" +
                "  WISPr-Location-Name: myLocationName", vsa.toString());
    }
}