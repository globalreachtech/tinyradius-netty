package org.tinyradius.attribute;

import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;
import org.tinyradius.attribute.util.Attributes;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.tinyradius.attribute.VendorSpecificAttribute.VENDOR_SPECIFIC;
import static org.tinyradius.attribute.util.Attributes.create;

class VendorSpecificAttributeTest {

    private static final Dictionary dictionary = DefaultDictionary.INSTANCE;

    @Test
    void parseChildVendorIdZero() {
        // childVendorId 4 bytes, smallest subattribute 2 bytes (type + length)
        final byte[] bytes = new byte[6];
        bytes[5] = 2; // subattribute length

        VendorSpecificAttribute vsa =
                new VendorSpecificAttribute(dictionary, -1, VENDOR_SPECIFIC, bytes);

        assertEquals(26, vsa.getType());
        assertEquals(-1, vsa.getVendorId());
        assertEquals(0, vsa.getChildVendorId());
    }

    @Test
    void parseChildVendorIdUnsignedIntMax() {
        final byte[] bytes = {
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, // childVendorId
                (byte) 0x00, (byte) 0x02 // subattribute
        };
        VendorSpecificAttribute vsa =
                new VendorSpecificAttribute(dictionary, -1, VENDOR_SPECIFIC, bytes);

        assertEquals(26, vsa.getType());
        assertEquals(-1, vsa.getVendorId());
        assertEquals(-1, vsa.getChildVendorId());
    }

    @Test
    void getVsaSubAttributeValueStringByName() {
        final VendorSpecificAttribute.Builder builder = new VendorSpecificAttribute.Builder()
                .setDictionary(dictionary)
                .setChildVendorId(14122);
        builder.addAttribute("WISPr-Location-ID", "myLocationId");
        VendorSpecificAttribute vsa = builder.build();

        assertFalse(vsa.getAttributes().isEmpty());
        assertEquals("myLocationId", vsa.getAttribute("WISPr-Location-ID").getValueString());
    }

    @Test
    void addSubAttributeOk() {
        String data = "myLocationId";
        final VendorSpecificAttribute.Builder builder = new VendorSpecificAttribute.Builder()
                .setDictionary(dictionary)
                .setChildVendorId(14122);
        builder.addAttribute(Attributes.create(dictionary, 14122, (byte) 2, data));
        final VendorSpecificAttribute build = builder.build();

        assertEquals(1, build.getAttributes().size());
        assertEquals(data, build.getAttribute((byte) 2).getValueString());
    }

    @Test
    void addNonVsaSubAttribute() {
        final VendorSpecificAttribute.Builder builder = new VendorSpecificAttribute.Builder()
                .setDictionary(dictionary)
                .setChildVendorId(14122);
        Exception exception = assertThrows(RuntimeException.class, () -> builder.addAttribute("User-Name", "test1"));
        assertTrue(exception.getMessage().toLowerCase().contains("vendor id doesn't match"));
    }

    @Test
    void addEmptySubAttribute() {
        final VendorSpecificAttribute.Builder builder = new VendorSpecificAttribute.Builder()
                .setDictionary(dictionary)
                .setChildVendorId(14122);
        Exception exception = assertThrows(RuntimeException.class, () -> builder.addAttribute("", "myLocationId"));
        assertTrue(exception.getMessage().toLowerCase().contains("type name is null/empty"));
    }

    @Test
    void vsaToFromByteArray() {
        final VendorSpecificAttribute.Builder builder = new VendorSpecificAttribute.Builder()
                .setDictionary(dictionary)
                .setChildVendorId(14122);
        builder.addAttribute(Attributes.create(dictionary, 14122, (byte) 2, "hiii")); //new byte[8]
        builder.addAttribute("WISPr-Location-ID", "myLocationId");

        final VendorSpecificAttribute vsa = builder.build();
        assertEquals(2, vsa.getAttributes().size());

        // convert to bytes
        final byte[] bytes = vsa.toByteArray();
        final ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);

        assertEquals(VENDOR_SPECIFIC, byteBuffer.get());
        assertEquals(bytes.length, byteBuffer.get());
        assertEquals(14122, byteBuffer.getInt());

        // deserialize
        final byte[] vsaData = Unpooled.wrappedBuffer(bytes)
                .skipBytes(2) // skip type and length
                .copy().array();

        final VendorSpecificAttribute newVsa = new VendorSpecificAttribute(dictionary, -1, VENDOR_SPECIFIC, vsaData);
        assertEquals(2, newVsa.getAttributes().size());

        // convert to bytes again
        final byte[] newBytes = newVsa.toByteArray();
        assertArrayEquals(bytes, newBytes);
    }

    @Test
    void vsaToByteArrayLargestUnsignedVendorId() {
        RadiusAttribute radiusAttribute = create(dictionary, Integer.parseUnsignedInt("4294967295"), (byte) 1, new byte[4]);
        VendorSpecificAttribute vsa = new VendorSpecificAttribute(
                dictionary, Collections.singletonList(radiusAttribute), Integer.parseUnsignedInt("4294967295"));
        assertEquals(1, vsa.getAttributes().size());

        byte[] bytes = vsa.toByteArray();
        assertEquals(12, bytes.length);
        assertEquals(-1, ByteBuffer.wrap(bytes).getInt(2));
        // int unsigned max == -1 signed
    }

    @Test
    void vsaToByteArrayTooLong() {
        final VendorSpecificAttribute.Builder builder = new VendorSpecificAttribute.Builder()
                .setDictionary(dictionary)
                .setChildVendorId(14122);
        builder.addAttribute(new RadiusAttribute(dictionary, 14122, (byte) 26, new byte[253]));
        builder.addAttribute("WISPr-Location-ID", "myLocationId");

        assertEquals(2, builder.getAttributes().size());
        Exception exception = assertThrows(IllegalArgumentException.class, builder::build);
        assertTrue(exception.getMessage().toLowerCase().contains("attribute data too long, max 253 octets"));
    }

    @Test
    void vsaToByteArrayWithNoSubAttributes() {
        Exception exception = assertThrows(RuntimeException.class,
                () -> new VendorSpecificAttribute(dictionary, new ArrayList<>(), 14122));
        assertTrue(exception.getMessage().toLowerCase().contains("should be greater than 6 octets"));
    }

    @Test
    void testFlatten() {
        final VendorSpecificAttribute.Builder builder = new VendorSpecificAttribute.Builder()
                .setDictionary(dictionary)
                .setChildVendorId(14122);
        builder.addAttribute("WISPr-Location-ID", "myLocationId");
        builder.addAttribute("WISPr-Location-Name", "myLocationName");

        assertEquals("[WISPr-Location-ID: myLocationId, WISPr-Location-Name: myLocationName]",
                builder.build().flatten().toString());
    }

    @Test
    void testToString() {
        final VendorSpecificAttribute.Builder builder = new VendorSpecificAttribute.Builder()
                .setDictionary(dictionary)
                .setChildVendorId(14122);
        builder.addAttribute("WISPr-Location-ID", "myLocationId");
        builder.addAttribute("WISPr-Location-Name", "myLocationName");

        assertEquals("Vendor-Specific: WISPr (14122)\n" +
                "  WISPr-Location-ID: myLocationId\n" +
                "  WISPr-Location-Name: myLocationName", builder.build().toString());
    }
}