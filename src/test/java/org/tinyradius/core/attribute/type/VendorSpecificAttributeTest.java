package org.tinyradius.core.attribute.type;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.dictionary.Dictionary;
import org.tinyradius.core.dictionary.parser.DictionaryParser;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.tinyradius.core.attribute.type.AttributeType.VSA;
import static org.tinyradius.core.attribute.type.VendorSpecificAttribute.VENDOR_SPECIFIC;

class VendorSpecificAttributeTest {

    private final SecureRandom random = new SecureRandom();
    private static Dictionary dictionary;

    @BeforeAll
    static void setup() throws IOException {
        dictionary = DictionaryParser.newClasspathParser().parseDictionary("org/tinyradius/core/dictionary/test_dictionary");
    }

    @Test
    void parseChildVendorIdZero() {
        // childVendorId 4 bytes + smallest subattribute 2 bytes (type + length)
        final byte[] value = new byte[6];
        value[5] = 2; // subattribute length

        final VendorSpecificAttribute vsa = (VendorSpecificAttribute) VSA.create(dictionary, -1, VENDOR_SPECIFIC, (byte) 0, value);

        assertEquals(26, vsa.getType());
        assertEquals(-1, vsa.getVendorId());
        assertEquals(0, vsa.getChildVendorId());
    }

    @Test
    void parseChildVendorIdUnsignedIntMax() {
        final byte[] value = {
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, // childVendorId
                (byte) 0x00, (byte) 0x02 // subattribute
        };
        final VendorSpecificAttribute vsa = (VendorSpecificAttribute) VSA.create(dictionary, -1, VENDOR_SPECIFIC, (byte) 0, value);

        assertEquals(26, vsa.getType());
        assertEquals(-1, vsa.getVendorId());
        assertEquals(-1, vsa.getChildVendorId());
    }

    @Test
    void getVsaSubAttributeValueStringByName() {
        final VendorSpecificAttribute vsa = new VendorSpecificAttribute(dictionary, 14122, Collections.singletonList(
                dictionary.createAttribute("WISPr-Location-ID", "myLocationId")
        ));

        assertFalse(vsa.getAttributes().isEmpty());
        assertEquals("myLocationId", vsa.getAttribute("WISPr-Location-ID").get().getValueString());
    }

    @Test
    void addSubAttributeOk() throws RadiusPacketException {
        final String data = "myLocationId";
        final VendorSpecificAttribute vsa = new VendorSpecificAttribute(
                dictionary, 14122, Collections.singletonList(dictionary.createAttribute("WISPr-Location-ID", "myLocationId")))
                .addAttribute(dictionary.createAttribute(14122, 2, data));

        assertEquals(2, vsa.getAttributes().size());
        assertEquals(data, vsa.getAttribute(2).get().getValueString());
    }

    @Test
    void addDiffVendorSubAttribute() {
        final VendorSpecificAttribute vsa = new VendorSpecificAttribute(dictionary, 14122, Collections.singletonList(
                dictionary.createAttribute("WISPr-Location-ID", "myLocationId")
        ));

        final Exception e1 = assertThrows(RuntimeException.class, () -> vsa.addAttribute("User-Name", "test1"));
        assertTrue(e1.getMessage().toLowerCase().contains("vendor id doesn't match"));

        final Exception e2 = assertThrows(RuntimeException.class, () -> vsa.addAttribute("Ascend-UU-Info", "test1"));
        assertTrue(e2.getMessage().toLowerCase().contains("vendor id doesn't match"));
    }

    @Test
    void createWithDiffVendorSubAttribute() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                new VendorSpecificAttribute(dictionary, 14122, Collections.singletonList(
                        dictionary.createAttribute("User-Name", "myName"))));

        assertTrue(e.getMessage().contains("Vendor-Specific attribute sub-attributes must have same vendorId as VSA childVendorId"));
    }

    @Test
    void addEmptySubAttribute() {
        final VendorSpecificAttribute vsa = new VendorSpecificAttribute(dictionary, 14122, Collections.singletonList(
                dictionary.createAttribute("WISPr-Location-ID", "myLocationId")
        ));

        final Exception exception = assertThrows(RuntimeException.class, () -> vsa.addAttribute("", "myLocationId"));
        assertEquals("Unknown attribute type name: ''", exception.getMessage());
    }

    @Test
    void toFromByteArray() {
        final VendorSpecificAttribute vsa = new VendorSpecificAttribute(dictionary, 14122, Arrays.asList(
                dictionary.createAttribute(14122, 2, "hiii"),
                dictionary.createAttribute("WISPr-Location-ID", "myLocationId")
        ));
        assertEquals(2, vsa.getAttributes().size());

        // convert to bytes
        final byte[] bytes = vsa.toByteArray();
        final ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);

        assertEquals(VENDOR_SPECIFIC, byteBuffer.get());
        assertEquals(bytes.length, byteBuffer.get());
        assertEquals(14122, byteBuffer.getInt());

        // create from bytebuf
        final VendorSpecificAttribute parsedAttribute = (VendorSpecificAttribute) dictionary.createAttribute(-1, VENDOR_SPECIFIC, Unpooled.wrappedBuffer(bytes));
        assertArrayEquals(bytes, parsedAttribute.toByteArray());
        assertEquals(2, parsedAttribute.getAttributes().size());

        // convert to bytes again
        assertArrayEquals(bytes, parsedAttribute.toByteArray());

        // remove headers and create
        final byte[] vsaData = Unpooled.wrappedBuffer(bytes)
                .skipBytes(2) // skip type and length
                .copy().array();

        // create from byte array
        final VendorSpecificAttribute createdAttribute = (VendorSpecificAttribute) dictionary.createAttribute(-1, VENDOR_SPECIFIC, vsaData);
        assertArrayEquals(bytes, createdAttribute.toByteArray());
        assertEquals(2, createdAttribute.getAttributes().size());

        // convert to bytes again
        assertArrayEquals(bytes, createdAttribute.toByteArray());
    }

    @CsvSource({ // vendorId, 2x known attributes, subAttrHeaderSize
            "4846,2,20119,3", // Lucent format=2,1
            "8164,2,256,4", // Starent format=2,2
            "429,102,232,4" // USR format=4,0
    })
    @ParameterizedTest
    void customTypeSizeToFromByteArray(int vendorId, int attribute1, int attribute2, int subAttrHeaderSize) {
        final int attribute3 = 999;
        final byte[] bytes1 = random.generateSeed(4);
        final byte[] bytes2 = random.generateSeed(4);
        final byte[] bytes3 = random.generateSeed(4);

        final VendorSpecificAttribute vsa = new VendorSpecificAttribute(dictionary, vendorId, Arrays.asList(
                dictionary.createAttribute(vendorId, attribute1, bytes1),
                dictionary.createAttribute(vendorId, attribute2, bytes2),
                // vendor defined, but attribute undefined
                dictionary.createAttribute(vendorId, attribute3, bytes3)
        ));

        int length = 6 + 3 * (4 + subAttrHeaderSize);

        final byte[] vsaByteBuf = vsa.toByteArray();
        assertEquals(length, vsaByteBuf.length);
        assertEquals(3, vsa.getAttributes().size());


        final RadiusAttribute sub1 = vsa.getAttributes().get(0);
        assertFalse(sub1.getAttributeName().contains("Unknown"));
        assertEquals(attribute1, sub1.getType());

        final RadiusAttribute sub2 = vsa.getAttributes().get(1);
        assertFalse(sub2.getAttributeName().contains("Unknown"));
        assertEquals(attribute2, sub2.getType());

        final RadiusAttribute sub3 = vsa.getAttributes().get(2);
        assertTrue(sub3.getAttributeName().contains("Unknown"));
        assertEquals(attribute3, sub3.getType());

        // does USR still parse properly?

        System.out.println(vsa);
        // todo test customTypeSize / customLengthSize
    }

    /**
     * The String field is one or more octets.  The actual format of the
     * information is site or application specific, and a robust
     * implementation SHOULD support the field as undistinguished octets.
     */
    @Test
    void undistinguishedOctets() {
        final byte[] bytes = random.generateSeed(10);
        final ByteBuf vsaByteBuf = Unpooled.buffer().writeByte(26).writeByte(16).writeInt(123456).writeBytes(bytes);

        // create from bytebuf
        final VendorSpecificAttribute attribute1 = (VendorSpecificAttribute) VSA.create(dictionary, -1, vsaByteBuf);

        assertEquals(1, attribute1.getAttributes().size());
        final AnonSubAttribute sub1 = (AnonSubAttribute) attribute1.getAttributes().get(0);
        assertArrayEquals(bytes, sub1.toByteArray());
        assertEquals("[Unparsable sub-attribute (vendorId 123456, length 10)]", sub1.getValueString());
        assertArrayEquals(vsaByteBuf.copy().array(), attribute1.toByteArray());

        // create from byte[]
        final VendorSpecificAttribute attribute2 = (VendorSpecificAttribute) VSA.create(dictionary, -1, 26, (byte) 0,
                vsaByteBuf.slice(2, vsaByteBuf.readableBytes() - 2).copy().array());

        assertEquals(1, attribute2.getAttributes().size());
        final AnonSubAttribute sub2 = (AnonSubAttribute) attribute1.getAttributes().get(0);
        assertArrayEquals(bytes, sub2.toByteArray());
        assertEquals("[Unparsable sub-attribute (vendorId 123456, length 10)]", sub2.getValueString());
        assertArrayEquals(vsaByteBuf.copy().array(), attribute2.toByteArray());
    }

    @Test
    void badVendorId() {
        final IllegalArgumentException e1 = assertThrows(IllegalArgumentException.class, () ->
                VSA.create(dictionary, 4567, Unpooled.buffer().writeByte(26).writeByte(8).writeInt(123456).writeShort(2)));
        assertEquals("Vendor-Specific attribute should be top level attribute, vendorId should be -1, actual: 4567", e1.getMessage());

        final IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class, () ->
                VSA.create(dictionary, 4567, 26, (byte) 0, new byte[6]));
        assertEquals("Vendor-Specific attribute should be top level attribute, vendorId should be -1, actual: 4567", e2.getMessage());
    }

    @Test
    void badAttributeType() {
        final IllegalArgumentException e1 = assertThrows(IllegalArgumentException.class, () ->
                VSA.create(dictionary, -1, Unpooled.buffer().writeByte(10).writeByte(8).writeInt(123456).writeShort(2)));
        assertEquals("Vendor-Specific attribute attributeId should always be 26, actual: 10", e1.getMessage());

        final IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class, () ->
                VSA.create(dictionary, -1, 10, (byte) 0, new byte[6]));
        assertEquals("Vendor-Specific attribute attributeId should always be 26, actual: 10", e2.getMessage());
    }

    @Test
    void noSubAttribute() {
        final IllegalArgumentException e1 = assertThrows(IllegalArgumentException.class, () ->
                VSA.create(dictionary, 14122, Unpooled.buffer().writeByte(26).writeByte(6).writeInt(123456)));
        assertTrue(e1.getMessage().contains("should be greater than 6 octets, actual: 6"));

        final IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class, () ->
                VSA.create(dictionary, 14122, 26, (byte) 0, new byte[4]));
        assertTrue(e2.getMessage().contains("should be greater than 6 octets, actual: 6"));

        final IllegalArgumentException e3 = assertThrows(IllegalArgumentException.class,
                () -> new VendorSpecificAttribute(dictionary, 14122, new ArrayList<>()));
        assertTrue(e3.getMessage().contains("should be greater than 6 octets, actual: 6"));
    }

    @Test
    void toByteArrayLargestUnsignedVendorId() {
        final RadiusAttribute radiusAttribute = dictionary.createAttribute(Integer.parseUnsignedInt("4294967295"), 1, new byte[4]);
        final VendorSpecificAttribute vsa = new VendorSpecificAttribute(
                dictionary, Integer.parseUnsignedInt("4294967295"), Collections.singletonList(radiusAttribute));
        assertEquals(1, vsa.getAttributes().size());

        final byte[] bytes = vsa.toByteArray();
        assertEquals(12, bytes.length);
        assertEquals(-1, ByteBuffer.wrap(bytes).getInt(2));
        // int unsigned max == -1 signed
    }

    @Test
    void createTooLong() {
        final List<RadiusAttribute> attributes = Collections.singletonList(
                dictionary.createAttribute(14122, 26, new byte[253]));
        assertTrue(attributes.get(0) instanceof OctetsAttribute);
        final Exception exception = assertThrows(IllegalArgumentException.class,
                () -> new VendorSpecificAttribute(dictionary, 14122, attributes));
        assertTrue(exception.getMessage().contains("Attribute too long"));
    }

    @Test
    void testFlatten() {
        final VendorSpecificAttribute vsa = new VendorSpecificAttribute(dictionary, 14122, Arrays.asList(
                dictionary.createAttribute("WISPr-Location-ID", "myLocationId"),
                dictionary.createAttribute("WISPr-Location-Name", "myLocationName")
        ));

        assertEquals("[WISPr-Location-ID: myLocationId, WISPr-Location-Name: myLocationName]",
                vsa.flatten().toString());
    }

    @Test
    void testToString() {
        final VendorSpecificAttribute vsa = new VendorSpecificAttribute(dictionary, 14122, Arrays.asList(
                dictionary.createAttribute("WISPr-Location-ID", "myLocationId"),
                dictionary.createAttribute("WISPr-Location-Name", "myLocationName")
        ));

        assertEquals("Vendor-Specific: Vendor ID 14122 (WISPr)\n" +
                "  WISPr-Location-ID: myLocationId\n" +
                "  WISPr-Location-Name: myLocationName", vsa.toString());
    }

    @Test
    void encodeDecode() throws RadiusPacketException {
        final int vendorId = 14122;
        final String secret = "mySecret";
        final byte tag = 123;
        final byte[] requestAuth = random.generateSeed(16);

        final VendorSpecificAttribute vsa = new VendorSpecificAttribute(dictionary, vendorId, Arrays.asList(
                dictionary.createAttribute(vendorId, 5, tag, "12345"),
                dictionary.createAttribute(vendorId, 6, tag, "12345"),
                dictionary.createAttribute(vendorId, 7, tag, "12345")
        ));
        assertEquals(-1, vsa.getVendorId());
        assertEquals(vendorId, vsa.getChildVendorId());

        final IntegerAttribute minUp = (IntegerAttribute) vsa.getAttribute("WISPr-Bandwidth-Min-Up").get();
        assertEquals(12345, ByteBuffer.wrap(minUp.getValue()).getInt());
        assertEquals(tag, minUp.getTag().get());

        final IntegerAttribute minDown = (IntegerAttribute) vsa.getAttribute("WISPr-Bandwidth-Min-Down").get();
        assertEquals(12345, ByteBuffer.wrap(minDown.getValue()).getInt());
        assertFalse(minDown.getTag().isPresent());

        final IntegerAttribute maxUp = (IntegerAttribute) vsa.getAttribute("WISPr-Bandwidth-Max-Up").get();
        assertEquals(12345, ByteBuffer.wrap(maxUp.getValue()).getInt());
        assertEquals(tag, maxUp.getTag().get());

        // encode
        final VendorSpecificAttribute encode = (VendorSpecificAttribute) vsa.encode(requestAuth, secret);
        assertEquals(-1, encode.getVendorId());
        assertEquals(vendorId, encode.getChildVendorId());

        final OctetsAttribute encodeMinUp = (OctetsAttribute) encode.getAttribute("WISPr-Bandwidth-Min-Up").get();
        assertEquals(12345, ByteBuffer.wrap(encodeMinUp.getValue()).getInt());
        assertEquals(tag, encodeMinUp.getTag().get());

        final EncodedAttribute encodeMinDown = (EncodedAttribute) encode.getAttribute("WISPr-Bandwidth-Min-Down").get();
        assertNotEquals(12345, ByteBuffer.wrap(encodeMinDown.getValue()).getInt());
        assertFalse(encodeMinDown.getTag().isPresent());

        final EncodedAttribute encodeMaxUp = (EncodedAttribute) encode.getAttribute("WISPr-Bandwidth-Max-Up").get();
        assertNotEquals(12345, ByteBuffer.wrap(encodeMaxUp.getValue()).getInt());
        assertEquals(tag, encodeMaxUp.getTag().get());

        // use different encoding
        assertNotEquals(ByteBuffer.wrap(encodeMinDown.getValue()).getInt(), ByteBuffer.wrap(encodeMaxUp.getValue()).getInt());

        // encode again
        final VendorSpecificAttribute encode1 = (VendorSpecificAttribute) encode.encode(requestAuth, secret);
        assertEquals(encode, encode1);

        // decode
        final VendorSpecificAttribute decode = (VendorSpecificAttribute) encode.decode(requestAuth, secret);
        assertEquals(-1, decode.getVendorId());
        assertEquals(vendorId, decode.getChildVendorId());

        final IntegerAttribute decodeMinUp = (IntegerAttribute) decode.getAttribute("WISPr-Bandwidth-Min-Up").get();
        assertEquals(12345, ByteBuffer.wrap(decodeMinUp.getValue()).getInt());
        assertEquals(tag, decodeMinUp.getTag().get());

        final IntegerAttribute decodeMinDown = (IntegerAttribute) decode.getAttribute("WISPr-Bandwidth-Min-Down").get();
        assertEquals(12345, ByteBuffer.wrap(decodeMinUp.getValue()).getInt());
        assertFalse(decodeMinDown.getTag().isPresent());

        final IntegerAttribute decodeMaxUp = (IntegerAttribute) decode.getAttribute("WISPr-Bandwidth-Max-Up").get();
        assertEquals(12345, ByteBuffer.wrap(decodeMinUp.getValue()).getInt());
        assertEquals(tag, decodeMaxUp.getTag().get());

        // decode again
        final VendorSpecificAttribute decode1 = (VendorSpecificAttribute) decode.decode(requestAuth, secret);
        assertEquals(decode1, decode);
    }
}