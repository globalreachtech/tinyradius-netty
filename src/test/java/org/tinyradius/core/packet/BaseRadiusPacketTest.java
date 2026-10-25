package org.tinyradius.core.packet;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.attribute.type.RadiusAttribute;
import org.tinyradius.core.attribute.type.VendorSpecificAttribute;
import org.tinyradius.core.dictionary.Dictionary;
import org.tinyradius.core.dictionary.parser.DictionaryParser;
import org.tinyradius.core.packet.request.RadiusRequest;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("OptionalGetWithoutIsPresent")
class BaseRadiusPacketTest {

    private final SecureRandom random = new SecureRandom();
    private static Dictionary dictionary;

    @BeforeAll
    static void setup() throws IOException {
        dictionary = DictionaryParser.newClasspathParser().parseDictionary("org/tinyradius/core/dictionary/test_dictionary");
    }

    @Test
    void doesNotMutateOriginalAttributeList() throws RadiusPacketException {
        List<RadiusAttribute> attributes = Collections.emptyList(); // immutable
        StubPacket rp = new StubPacket(attributes)
                .addAttribute("WISPr-Location-ID", "myLocationId");

        assertEquals(1, rp.getAttributes().size());
    }

    @Test
    void addAttribute() throws RadiusPacketException {
        StubPacket packet = new StubPacket()
                .addAttribute("WISPr-Location-ID", "myLocationId")
                .addAttribute(8, "1234567")
                .addAttribute(168, "fe80::")
                .addAttribute(97, "fe80::/64")
                .addAttribute(97, "fe80::/128");

        List<VendorSpecificAttribute> vendorAttributes = packet.getVendorAttributes(14122);
        assertEquals(1, vendorAttributes.size());

        List<RadiusAttribute> wisprLocations = vendorAttributes.get(0).getAttributes();
        assertEquals(1, wisprLocations.size());
        assertEquals("myLocationId", wisprLocations.get(0).getValueString());

        assertEquals("myLocationId", packet.getAttribute(14122, 1).get().getValueString());
        List<RadiusAttribute> wisprLocations2 = packet.getAttributes(14122, 1);
        assertEquals(1, wisprLocations2.size());
        assertEquals("myLocationId", wisprLocations2.get(0).getValueString());

        assertEquals("0.18.214.135", packet.getAttribute(8).get().getValueString());
        assertEquals("0.18.214.135", packet.getAttribute("Framed-IP-Address").get().getValueString());
        assertEquals("fe80:0:0:0:0:0:0:0", packet.getAttribute(168).get().getValueString());
        assertEquals("fe80:0:0:0:0:0:0:0", packet.getAttribute("Framed-IPv6-Address").get().getValueString());

        List<RadiusAttribute> ipV6Attributes = packet.getAttributes(97);
        assertArrayEquals(new String[]{"fe80:0:0:0:0:0:0:0/64", "fe80:0:0:0:0:0:0:0/128"},
                ipV6Attributes.stream().map(RadiusAttribute::getValueString).toArray());

        assertEquals("""
                Access-Request, ID 1, len 96, attributes={
                Vendor-Specific: Vendor ID 14122 (WISPr)
                  WISPr-Location-ID=myLocationId
                Framed-IP-Address=0.18.214.135
                Framed-IPv6-Address=fe80:0:0:0:0:0:0:0
                Framed-IPv6-Prefix=fe80:0:0:0:0:0:0:0/64
                Framed-IPv6-Prefix=fe80:0:0:0:0:0:0:0/128
                }""", packet.toString());
    }

    @Test
    void removeSpecificAttribute() throws RadiusPacketException {
        RadiusAttribute ra = dictionary.createAttribute(-1, 8, new byte[4]);
        StubPacket rp = new StubPacket()
                .addAttribute(ra);
        assertEquals(1, rp.getAttributes().size());

        StubPacket removed = rp.removeAttribute(ra);
        assertEquals(0, removed.getAttributes().size());
    }

    @Test
    void removeSpecificVendorAttributes() throws RadiusPacketException {
        StubPacket rp1 = new StubPacket()
                .addAttribute("WISPr-Location-ID", "myLocationId");
        assertEquals(1, rp1.getAttributes().size());
        assertInstanceOf(VendorSpecificAttribute.class, rp1.getAttributes().get(0));

        StubPacket rp2 = rp1.removeAttributes(14122, 1);
        assertTrue(rp2.getAttributes().isEmpty());

        StubPacket rp3 = rp2.addAttribute("WISPr-Location-ID", "myLocationId");
        RadiusAttribute ra = rp3.getAttribute(14122, 1).get();

        StubPacket rp4 = rp3.removeAttribute(ra);
        assertEquals(0, rp4.getAttributes().size());
    }

    @Test
    void removeAttributesByType() throws RadiusPacketException {
        StubPacket rp = new StubPacket()
                .addAttribute("Service-Type", "1")
                .addAttribute("Service-Type", "2")
                .addAttribute("User-Name", "user");
        assertEquals(3, rp.getAttributes().size());

        rp = rp.removeAttributes(6);
        assertFalse(rp.getAttributes().isEmpty());
        assertEquals(1, rp.getAttributes().size());
    }

    @Test
    void removeLastAttributeForType() throws RadiusPacketException {
        StubPacket rp = new StubPacket()
                .addAttribute("Service-Type", "1")
                .addAttribute("Service-Type", "2")
                .addAttribute("User-Name", "user");

        // remove once
        StubPacket rp2 = rp.removeLastAttribute(6);

        List<RadiusAttribute> rp2Attributes = rp2.getAttributes(6);
        assertEquals(1, rp2Attributes.size());
        assertEquals("Login-User", rp2Attributes.get(0).getValueString());

        // remove again
        StubPacket rp3 = rp2.removeLastAttribute(6);

        List<RadiusAttribute> rp3Attributes = rp3.getAttributes(6);
        assertEquals(0, rp3Attributes.size());

        // last remove should do nothing
        StubPacket rp4 = rp3.removeLastAttribute(6);

        List<RadiusAttribute> rp4Attributes = rp4.getAttributes(6);
        assertEquals(0, rp4Attributes.size());

        assertEquals(3, rp.getAttributes().size());
        assertEquals(2, rp2.getAttributes().size());
        assertEquals(1, rp3.getAttributes().size());
        assertEquals(1, rp4.getAttributes().size());
    }

    @Test
    void testFlattenAttributes() throws RadiusPacketException {
        VendorSpecificAttribute vsa = new VendorSpecificAttribute(dictionary, 14122, Arrays.asList(
                dictionary.createAttribute("WISPr-Logoff-URL", "111"),
                dictionary.createAttribute("WISPr-Logoff-URL", "222")
        ));

        StubPacket radiusPacket = new StubPacket()
                .addAttribute("Service-Type", "999")
                .addAttribute("Filter-Id", "abc")
                .addAttribute("Reply-Message", "foobar")
                .addAttribute(vsa);

        List<RadiusAttribute> attributes = radiusPacket.getFlattenedAttributes();

        assertEquals("Service-Type=999", attributes.get(0).toString());
        assertEquals("Filter-Id=abc", attributes.get(1).toString());
        assertEquals("Reply-Message=foobar", attributes.get(2).toString());
        assertEquals("WISPr-Logoff-URL=111", attributes.get(3).toString());
        assertEquals("WISPr-Logoff-URL=222", attributes.get(4).toString());
        // getAttributes only gets the last subAttribute of VendorSpecificAttribute

        assertEquals(5, attributes.size());
    }

    @Test
    void withAttributesUpdatesHeaderLength() throws RadiusPacketException {
        StubPacket stubPacket = new StubPacket();
        assertEquals(20, stubPacket.toByteBuf().readableBytes());
        assertEquals(20, stubPacket.toByteBuf().getShort(2));

        StubPacket packet2 = stubPacket.withAttributes(Collections.singletonList(dictionary.createAttribute("User-Name", "a")));
        assertEquals(23, packet2.toByteBuf().readableBytes());
        assertEquals(23, packet2.toByteBuf().getShort(2));
    }

    @Test
    void withAuthAttributesUpdatesHeaderLength() throws RadiusPacketException {
        StubPacket stubPacket = new StubPacket();
        assertEquals(20, stubPacket.toByteBuf().readableBytes());
        assertEquals(20, stubPacket.toByteBuf().getShort(2));

        StubPacket packet2 = stubPacket.withAuthAttributes(
                random.generateSeed(16),
                Collections.singletonList(dictionary.createAttribute("User-Name", "a")));
        assertEquals(23, packet2.toByteBuf().readableBytes());
        assertEquals(23, packet2.toByteBuf().getShort(2));
    }

    @Test
    void vsaAutoWrap() throws RadiusPacketException {
        String vendorAttrName = "WISPr-Location-ID";
        RadiusAttribute vsa = dictionary.createAttribute(vendorAttrName, "anything");

        // Create request with empty attribute list and add vsa later
        RadiusRequest request1 = RadiusRequest.create(dictionary, (byte) 1, (byte) 1, null, List.of())
                .addAttribute(vsa);
        assertEquals(vendorAttrName, request1.getAttribute(vendorAttrName).get().getAttributeName());

        // Create request with vsa in attribute list at creation time
        RadiusRequest request2 = RadiusRequest.create(dictionary, (byte) 1, (byte) 1, null, List.of(vsa));
        assertEquals(vendorAttrName, request2.getAttribute(vendorAttrName).get().getAttributeName());
    }

    @Test
    void headerValidationVerifyAuthAndEquals() throws RadiusPacketException {
        var shortHeader = Unpooled.buffer(10).writeZero(10);
        assertThrows(IllegalArgumentException.class, () -> new StubPacket(shortHeader, Collections.emptyList()));

        var mismatchHeader = RadiusPacket.buildHeader((byte) 1, (byte) 1, new byte[16], Collections.emptyList());
        mismatchHeader.setShort(2, 99);
        assertThrows(RadiusPacketException.class, () -> new StubPacket(mismatchHeader, Collections.emptyList()));

        var p1 = new StubPacket();
        var p1Same = new StubPacket();
        var pDiffAttr = p1.addAttribute("User-Name", "alice");
        var diffHeader = RadiusPacket.buildHeader((byte) 2, (byte) 1, new byte[16], Collections.emptyList());
        var pDiffHeader = new StubPacket(diffHeader, Collections.emptyList());

        assertEquals(p1, p1Same);
        assertEquals(p1.hashCode(), p1Same.hashCode());
        assertThat(p1)
                .isNotEqualTo("not a packet")
                .isNotEqualTo(null);
        assertNotEquals(p1, pDiffAttr);
        assertNotEquals(p1, pDiffHeader);

        // verifyPacketAuth with short authenticator override
        var shortAuthPacket = new StubPacket() {
            @Override
            public byte @Nullable [] getAuthenticator() {
                return new byte[15];
            }
        };
        assertThrows(RadiusPacketException.class, () -> shortAuthPacket.verifyPacketAuth("secret", new byte[16]));

        // genHashedAuth with empty sharedSecret
        assertThrows(IllegalArgumentException.class, () -> p1.genHashedAuth("", new byte[16]));
    }

    @Test
    void nestedAttributeHolderEdgeCases() throws RadiusPacketException {
        var vsa1 = new VendorSpecificAttribute(dictionary, 14122, List.of(
                dictionary.createAttribute(14122, 1, "loc1"),
                dictionary.createAttribute(14122, 2, "name1")
        ));
        var vsaOtherVendor = new VendorSpecificAttribute(dictionary, 4846, List.of(
                dictionary.createAttribute(4846, 2, new byte[4])
        ));
        var topAttr = dictionary.createAttribute("User-Name", "bob");

        var packet = new StubPacket(List.of(topAttr, vsa1, vsaOtherVendor));

        // getAttributes(vendorId, type) with multiple VSAs and sub-attributes
        assertEquals(1, packet.getAttributes(14122, 1).size());
        assertEquals(0, packet.getAttributes(14122, 99).size());
        assertEquals(0, packet.getAttributes(99999, 1).size());

        // removeAttribute(subAttr) where VSA still has remaining sub-attributes and non-VSA attributes exist
        var loc1Attr = vsa1.getAttributes().get(0);
        var afterRemoveOneSub = packet.removeAttribute(loc1Attr);
        assertEquals(3, afterRemoveOneSub.getAttributes().size());
        assertEquals(1, afterRemoveOneSub.getVendorAttributes(14122).get(0).getAttributes().size());

        // removeAttributes(vendorId, type) where VSA still has remaining sub-attributes and non-VSA attributes exist
        var afterRemoveTypeSub = packet.removeAttributes(14122, 1);
        assertEquals(3, afterRemoveTypeSub.getAttributes().size());
        assertEquals(1, afterRemoveTypeSub.getVendorAttributes(14122).get(0).getAttributes().size());
    }

    private static class StubPacket extends BaseRadiusPacket<StubPacket> {

        private StubPacket() throws RadiusPacketException {
            this(Collections.emptyList());
        }

        private StubPacket(@NonNull List<RadiusAttribute> attributes) throws RadiusPacketException {
            this(RadiusPacket.buildHeader((byte) 1, (byte) 1, new byte[16], attributes), attributes);
        }

        private StubPacket(@NonNull ByteBuf header, @NonNull List<RadiusAttribute> attributes) throws RadiusPacketException {
            super(dictionary, header, attributes);
        }

        @Override
        public @NonNull StubPacket withAuthAttributes(byte @Nullable [] auth, @NonNull List<RadiusAttribute> attributes) throws RadiusPacketException {
            return new StubPacket(attributes);
        }
    }
}