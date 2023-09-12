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
    void addSubAttribute() throws RadiusPacketException {
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
        final List<RadiusAttribute> attributes = Collections.singletonList(dictionary.createAttribute("User-Name", "myName"));
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                new VendorSpecificAttribute(dictionary, 14122, attributes));

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

    @CsvSource({
            "4846,2,20119,3", // Lucent format=2,1
            "8164,2,256,4" // Starent format=2,2
    })
    @ParameterizedTest
    void customTypeLengthToFromByteArray(int vendorId, int attribute1, int attribute2, int subAttrHeaderSize) {
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

        final VendorSpecificAttribute parsed = new VendorSpecificAttribute(dictionary, -1, Unpooled.wrappedBuffer(vsaByteBuf));
        assertArrayEquals(vsa.toByteArray(), parsed.toByteArray());

        final RadiusAttribute parsedSub1 = parsed.getAttributes().get(0);
        assertFalse(parsedSub1.getAttributeName().contains("Unknown"));
        assertEquals(attribute1, parsedSub1.getType());
        assertArrayEquals(sub1.toByteArray(), parsedSub1.toByteArray());

        final RadiusAttribute parsedSub2 = parsed.getAttributes().get(1);
        assertFalse(parsedSub2.getAttributeName().contains("Unknown"));
        assertEquals(attribute2, parsedSub2.getType());
        assertArrayEquals(sub2.toByteArray(), parsedSub2.toByteArray());

        final RadiusAttribute parsedSub3 = parsed.getAttributes().get(2);
        assertTrue(parsedSub3.getAttributeName().contains("Unknown"));
        assertEquals(attribute3, parsedSub3.getType());
        assertArrayEquals(sub3.toByteArray(), parsedSub3.toByteArray());
    }

    /**
     * USR format=4,0
     */
    @Test
    void customTypeNilLengthToFromByteArray() {
        final int vendorId = 429;
        final int attribute1 = 102;
        final int attribute2 = 232;
        final int subAttrHeaderSize = 4; // USR
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
        assertEquals("USR-Last-Number-Dialed-Out", sub1.getAttributeName());
        assertEquals(attribute1, sub1.getType());

        final RadiusAttribute sub2 = vsa.getAttributes().get(1);
        assertEquals("USR-Last-Number-Dialed-In-DNIS", sub2.getAttributeName());
        assertEquals(attribute2, sub2.getType());

        final RadiusAttribute sub3 = vsa.getAttributes().get(2);
        assertEquals("Unknown-Sub-Attribute-999", sub3.getAttributeName());
        assertEquals(attribute3, sub3.getType());

        final VendorSpecificAttribute parsed = new VendorSpecificAttribute(dictionary, -1, Unpooled.wrappedBuffer(vsaByteBuf));

        assertEquals(1, parsed.getAttributes().size());

        final RadiusAttribute parsedSub1 = parsed.getAttributes().get(0);
        assertEquals("USR-Last-Number-Dialed-Out", parsedSub1.getAttributeName());
        assertEquals(attribute1, parsedSub1.getType());

        final int offset = 6 + subAttrHeaderSize;
        assertArrayEquals(vsa.toByteBuf().slice(offset, vsaByteBuf.length - offset).copy().array(), parsedSub1.getValue());
        // VSA doesn't have length field, so everything inside VSA is treated as single sub-attribute
        // (we parse sub-attribute to the end of the outer VSA)
    }

    /**
     * See <a href="https://tools.ietf.org/html/rfc2865#page-47">...</a>
     * <p>
     * "The String field is one or more octets.  The actual format of the
     * information is site or application specific, and a robust
     * implementation SHOULD support the field as undistinguished octets."
     * <p>
     * Testing when Vendor cannot be found.
     * <p>
     * See also {@link AnonSubAttribute}
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
        final ByteBuf byteBuf = Unpooled.buffer().writeByte(26).writeByte(8).writeInt(123456).writeShort(2);
        final IllegalArgumentException e1 = assertThrows(IllegalArgumentException.class, () -> VSA.create(dictionary, 4567, byteBuf));
        assertEquals("Vendor-Specific attribute should be top level attribute, vendorId should be -1, actual: 4567",
                e1.getCause().getMessage());

        final IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class, () ->
                VSA.create(dictionary, 4567, 26, (byte) 0, new byte[6]));
        assertEquals("Vendor-Specific attribute should be top level attribute, vendorId should be -1, actual: 4567",
                e2.getCause().getMessage());
    }

    @Test
    void badAttributeType() {
        final ByteBuf byteBuf = Unpooled.buffer().writeByte(10).writeByte(8).writeInt(123456).writeShort(2);
        final IllegalArgumentException e1 = assertThrows(IllegalArgumentException.class, () ->
                VSA.create(dictionary, -1, byteBuf));
        assertEquals("Vendor-Specific attribute attributeId should always be 26, actual: 10",
                e1.getCause().getMessage());

        final IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class, () ->
                VSA.create(dictionary, -1, 10, (byte) 0, new byte[6]));
        assertEquals("Vendor-Specific attribute attributeId should always be 26, actual: 10",
                e2.getCause().getMessage());
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
    void createTooShort() {
        final ByteBuf byteBuf = Unpooled.buffer().writeByte(26).writeByte(6);
        final IllegalArgumentException e1 = assertThrows(IllegalArgumentException.class, () -> VSA.create(dictionary, 14122, byteBuf));
        assertEquals("Vendor-Specific attribute should be greater than 6 octets, actual: 2", e1.getCause().getMessage());

        final IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class, () ->
                VSA.create(dictionary, 14122, 26, (byte) 0, new byte[0]));
        assertEquals("Vendor-Specific attribute should be greater than 6 octets, actual: 2", e2.getCause().getMessage());

        final IllegalArgumentException e3 = assertThrows(IllegalArgumentException.class, () ->
                VSA.create(dictionary, 14122, 26, (byte) 0, ""));
        assertEquals("Vendor-Specific attribute should be greater than 6 octets, actual: 2", e3.getCause().getMessage());
    }

    @Test
    void noSubAttribute() {
        final ByteBuf byteBuf = Unpooled.buffer().writeByte(26).writeByte(6).writeInt(123456);
        final IllegalArgumentException e1 = assertThrows(IllegalArgumentException.class, () ->
                VSA.create(dictionary, 14122, byteBuf));
        assertEquals("Vendor-Specific attribute should be greater than 6 octets, actual: 6", e1.getCause().getMessage());

        final IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class, () ->
                VSA.create(dictionary, 14122, 26, (byte) 0, new byte[4]));
        assertEquals("Vendor-Specific attribute should be greater than 6 octets, actual: 6", e2.getCause().getMessage());

        final IllegalArgumentException e3 = assertThrows(IllegalArgumentException.class, () ->
                VSA.create(dictionary, 14122, 26, (byte) 0, "11111111"));
        assertEquals("Vendor-Specific attribute should be greater than 6 octets, actual: 6", e3.getCause().getMessage());

        final List<RadiusAttribute> emptyList = Collections.emptyList();
        final IllegalArgumentException e4 = assertThrows(IllegalArgumentException.class,
                () -> new VendorSpecificAttribute(dictionary, 14122, emptyList));
        assertEquals("Vendor-Specific attribute should be greater than 6 octets, actual: 6", e4.getMessage());
    }

    @Test
    void testFlatten() {
        final VendorSpecificAttribute vsa = new VendorSpecificAttribute(dictionary, 14122, Arrays.asList(
                dictionary.createAttribute("WISPr-Location-ID", "myLocationId"),
                dictionary.createAttribute("WISPr-Location-Name", "myLocationName")
        ));

        assertEquals("[WISPr-Location-ID=myLocationId, WISPr-Location-Name=myLocationName]",
                vsa.flatten().toString());
    }

    @Test
    void testToString() {
        final VendorSpecificAttribute vsa = new VendorSpecificAttribute(dictionary, 14122, Arrays.asList(
                dictionary.createAttribute("WISPr-Location-ID", "myLocationId"),
                dictionary.createAttribute("WISPr-Location-Name", "myLocationName")
        ));

        assertEquals("Vendor-Specific: Vendor ID 14122 (WISPr)\n" +
                "  WISPr-Location-ID=myLocationId\n" +
                "  WISPr-Location-Name=myLocationName", vsa.toString());
    }

    @Test
    void encodeDecode() throws RadiusPacketException {
        final int vendorId = 14122;
        final String secret = "mySecret";
        final byte tag = 123;
        final byte[] requestAuth = random.generateSeed(16);

        final VendorSpecificAttribute vsa = new VendorSpecificAttribute(dictionary, vendorId, Arrays.asList(
                dictionary.createAttribute(vendorId, 5, tag, "12345"), // has_tag
                dictionary.createAttribute(vendorId, 6, tag, "12345"), // encrypt=1
                dictionary.createAttribute(vendorId, 7, tag, "12345")  // has_tag,encrypt=2
        ));
        assertEquals(-1, vsa.getVendorId());
        assertEquals(vendorId, vsa.getChildVendorId());

        final IntegerAttribute minUp = (IntegerAttribute) vsa.getAttribute("WISPr-Bandwidth-Min-Up").get();
        assertEquals(12345, minUp.getValueInt());
        assertEquals(tag, minUp.getTag().get());

        final IntegerAttribute minDown = (IntegerAttribute) vsa.getAttribute("WISPr-Bandwidth-Min-Down").get();
        assertEquals(12345, minDown.getValueInt());
        assertFalse(minDown.getTag().isPresent());

        final IntegerAttribute maxUp = (IntegerAttribute) vsa.getAttribute("WISPr-Bandwidth-Max-Up").get();
        assertEquals(12345, maxUp.getValueInt());
        assertEquals(tag, maxUp.getTag().get());

        // encode
        final VendorSpecificAttribute encoded = vsa.encode(requestAuth, secret);
        assertEquals(-1, encoded.getVendorId());
        assertEquals(vendorId, encoded.getChildVendorId());

        final IntegerAttribute encodeMinUp = (IntegerAttribute) encoded.getAttribute("WISPr-Bandwidth-Min-Up").get();
        assertEquals(12345, encodeMinUp.getValueInt());
        assertEquals(tag, encodeMinUp.getTag().get());

        final EncodedAttribute encodeMinDown = (EncodedAttribute) encoded.getAttribute("WISPr-Bandwidth-Min-Down").get();
        assertNotEquals(12345, ByteBuffer.wrap(encodeMinDown.getValue()).getInt());
        assertFalse(encodeMinDown.getTag().isPresent());

        final EncodedAttribute encodeMaxUp = (EncodedAttribute) encoded.getAttribute("WISPr-Bandwidth-Max-Up").get();
        assertNotEquals(12345, ByteBuffer.wrap(encodeMaxUp.getValue()).getInt());
        assertEquals(tag, encodeMaxUp.getTag().get());

        // use different encoding
        assertNotEquals(ByteBuffer.wrap(encodeMinDown.getValue()).getInt(), ByteBuffer.wrap(encodeMaxUp.getValue()).getInt());

        // encode again
        final VendorSpecificAttribute encode1 = encoded.encode(requestAuth, secret);
        assertEquals(encoded, encode1);

        // decode
        final VendorSpecificAttribute decoded = encoded.decode(requestAuth, secret);
        assertEquals(-1, decoded.getVendorId());
        assertEquals(vendorId, decoded.getChildVendorId());

        final IntegerAttribute decodeMinUp = (IntegerAttribute) decoded.getAttribute("WISPr-Bandwidth-Min-Up").get();
        assertEquals(12345, decodeMinUp.getValueInt());
        assertEquals(tag, decodeMinUp.getTag().get());

        final IntegerAttribute decodeMinDown = (IntegerAttribute) decoded.getAttribute("WISPr-Bandwidth-Min-Down").get();
        assertEquals(12345, decodeMinDown.getValueInt());
        assertFalse(decodeMinDown.getTag().isPresent());

        final IntegerAttribute decodeMaxUp = (IntegerAttribute) decoded.getAttribute("WISPr-Bandwidth-Max-Up").get();
        assertEquals(12345, decodeMaxUp.getValueInt());
        assertEquals(tag, decodeMaxUp.getTag().get());

        // decode again
        final VendorSpecificAttribute decode1 = decoded.decode(requestAuth, secret);
        assertEquals(decode1, decoded);
    }
}