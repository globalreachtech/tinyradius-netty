package org.tinyradius.core.packet;

import io.netty.buffer.ByteBuf;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.tinyradius.core.RadiusPacketException;
import org.tinyradius.core.attribute.type.RadiusAttribute;
import org.tinyradius.core.attribute.type.VendorSpecificAttribute;
import org.tinyradius.core.dictionary.Dictionary;
import org.tinyradius.core.dictionary.parser.DictionaryParser;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BaseRadiusPacketTest {

    private final SecureRandom random = new SecureRandom();
    private static Dictionary dictionary;

    @BeforeAll
    static void setup() throws IOException {
        dictionary = DictionaryParser.newClasspathParser().parseDictionary("org/tinyradius/core/dictionary/test_dictionary");
    }

    @Test
    void doesNotMutateOriginalAttributeList() throws RadiusPacketException {
        final List<RadiusAttribute> attributes = Collections.emptyList();
        final StubPacket rp = new StubPacket(attributes)
                .addAttribute("WISPr-Location-ID", "myLocationId");

        assertEquals(0, attributes.size());
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

        final List<VendorSpecificAttribute> vendorAttributes = packet.getVendorAttributes(14122);
        assertEquals(1, vendorAttributes.size());

        final List<RadiusAttribute> wisprLocations = vendorAttributes.get(0).getAttributes();
        assertEquals(1, wisprLocations.size());
        assertEquals("myLocationId", wisprLocations.get(0).getValueString());

        assertEquals("myLocationId", packet.getAttribute(14122, 1).get().getValueString());
        final List<RadiusAttribute> wisprLocations2 = packet.filterAttributes(14122, 1);
        assertEquals(1, wisprLocations2.size());
        assertEquals("myLocationId", wisprLocations2.get(0).getValueString());

        assertEquals("0.18.214.135", packet.getAttribute(8).get().getValueString());
        assertEquals("0.18.214.135", packet.getAttribute("Framed-IP-Address").get().getValueString());
        assertEquals("fe80:0:0:0:0:0:0:0", packet.getAttribute(168).get().getValueString());
        assertEquals("fe80:0:0:0:0:0:0:0", packet.getAttribute("Framed-IPv6-Address").get().getValueString());

        final List<RadiusAttribute> ipV6Attributes = packet.filterAttributes(97);
        assertArrayEquals(new String[]{"fe80:0:0:0:0:0:0:0/64", "fe80:0:0:0:0:0:0:0/128"},
                ipV6Attributes.stream().map(RadiusAttribute::getValueString).toArray());

        assertEquals("Access-Request, ID 1\n" +
                "Vendor-Specific: Vendor ID 14122 (WISPr)\n" +
                "  WISPr-Location-ID: myLocationId\n" +
                "Framed-IP-Address: 0.18.214.135\n" +
                "Framed-IPv6-Address: fe80:0:0:0:0:0:0:0\n" +
                "Framed-IPv6-Prefix: fe80:0:0:0:0:0:0:0/64\n" +
                "Framed-IPv6-Prefix: fe80:0:0:0:0:0:0:0/128", packet.toString());
    }

    @Test
    void removeSpecificAttribute() throws RadiusPacketException {
        final RadiusAttribute ra = dictionary.createAttribute(-1, 8, new byte[4]);
        final StubPacket rp = new StubPacket()
                .addAttribute(ra);
        assertEquals(1, rp.getAttributes().size());

        final StubPacket removed = rp.removeAttribute(ra);
        assertEquals(0, removed.getAttributes().size());
    }

    @Test
    void removeSpecificVendorAttributes() throws RadiusPacketException {
        final StubPacket rp1 = new StubPacket()
                .addAttribute("WISPr-Location-ID", "myLocationId");
        assertEquals(1, rp1.getAttributes().size());
        assertTrue(rp1.getAttributes().get(0) instanceof VendorSpecificAttribute);

        final StubPacket rp2 = rp1.removeAttributes(14122, 1);
        assertTrue(rp2.getAttributes().isEmpty());

        final StubPacket rp3 = rp2.addAttribute("WISPr-Location-ID", "myLocationId");
        final RadiusAttribute ra = rp3.getAttribute(14122, 1).get();

        final StubPacket rp4 = rp3.removeAttribute(ra);
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
        final StubPacket rp2 = rp.removeLastAttribute(6);

        List<RadiusAttribute> rp2Attributes = rp2.filterAttributes(6);
        assertEquals(1, rp2Attributes.size());
        assertEquals("Login-User", rp2Attributes.get(0).getValueString());

        // remove again
        final StubPacket rp3 = rp2.removeLastAttribute(6);

        List<RadiusAttribute> rp3Attributes = rp3.filterAttributes(6);
        assertEquals(0, rp3Attributes.size());

        // last remove should do nothing
        final StubPacket rp4 = rp3.removeLastAttribute(6);

        List<RadiusAttribute> rp4Attributes = rp4.filterAttributes(6);
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

        final List<RadiusAttribute> attributes = radiusPacket.getFlattenedAttributes();

        assertEquals("Service-Type: 999", attributes.get(0).toString());
        assertEquals("Filter-Id: abc", attributes.get(1).toString());
        assertEquals("Reply-Message: foobar", attributes.get(2).toString());
        assertEquals("WISPr-Logoff-URL: 111", attributes.get(3).toString());
        assertEquals("WISPr-Logoff-URL: 222", attributes.get(4).toString());
        // getAttributes only gets the last subAttribute of VendorSpecificAttribute

        assertEquals(5, attributes.size());
    }

    @Test
    void withAttributesUpdatesHeaderLength() throws RadiusPacketException {
        final StubPacket stubPacket = new StubPacket();
        assertEquals(20, stubPacket.toByteBuf().readableBytes());
        assertEquals(20, stubPacket.toByteBuf().getShort(2));

        final StubPacket packet2 = stubPacket.withAttributes(Collections.singletonList(dictionary.createAttribute("User-Name", "a")));
        assertEquals(23, packet2.toByteBuf().readableBytes());
        assertEquals(23, packet2.toByteBuf().getShort(2));
    }

    @Test
    void withAuthAttributesUpdatesHeaderLength() throws RadiusPacketException {
        final StubPacket stubPacket = new StubPacket();
        assertEquals(20, stubPacket.toByteBuf().readableBytes());
        assertEquals(20, stubPacket.toByteBuf().getShort(2));

        final StubPacket packet2 = stubPacket.withAuthAttributes(
                random.generateSeed(16),
                Collections.singletonList(dictionary.createAttribute("User-Name", "a")));
        assertEquals(23, packet2.toByteBuf().readableBytes());
        assertEquals(23, packet2.toByteBuf().getShort(2));
    }

    private static class StubPacket extends BaseRadiusPacket<StubPacket> {

        private StubPacket() throws RadiusPacketException {
            this(Collections.emptyList());
        }

        private StubPacket(List<RadiusAttribute> attributes) throws RadiusPacketException {
            super(dictionary, RadiusPacket.buildHeader((byte) 1, (byte) 1, new byte[16], attributes), attributes);
        }

        @Override
        protected StubPacket with(ByteBuf header, List<RadiusAttribute> attributes) throws RadiusPacketException {
            return new StubPacket(attributes);
        }
    }
}