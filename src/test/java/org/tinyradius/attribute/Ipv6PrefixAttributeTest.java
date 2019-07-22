package org.tinyradius.attribute;

import org.junit.jupiter.api.Test;
import org.tinyradius.dictionary.DefaultDictionary;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.packet.AccessRequest;
import org.tinyradius.packet.AccountingRequest;
import org.tinyradius.packet.RadiusPacket;

import static org.junit.jupiter.api.Assertions.*;
import static org.tinyradius.packet.PacketType.ACCESS_REJECT;

class Ipv6PrefixAttributeTest {

    private static final Dictionary dictionary = DefaultDictionary.INSTANCE;
    private Ipv6PrefixAttribute attribute;

    @Test
    void minAttributeLength() {
        final Ipv6PrefixAttribute prefixAttribute = new Ipv6PrefixAttribute(dictionary, -1, 97, new byte[2]);
        assertEquals(2, prefixAttribute.getValue().length);
    }

    @Test
    void maxAttributeLength() {
        final Ipv6PrefixAttribute prefixAttribute = new Ipv6PrefixAttribute(dictionary, -1, 97, new byte[18]);
        assertEquals(18, prefixAttribute.getValue().length);
    }

    @Test
    void LessThanMinAttributeLength() {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new Ipv6PrefixAttribute(dictionary, -1, 97, new byte[1]));
        assertTrue(exception.getMessage().toLowerCase().contains("expected length min 4, max 20"));
    }

    @Test
    void MoreThanMaxAttributeLength() {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new Ipv6PrefixAttribute(dictionary, -1, 97, new byte[20]));
        assertTrue(exception.getMessage().toLowerCase().contains("expected length min 4, max 20"));
    }

    @Test
    void addFramedIpv6PrefixToAccessRequestPacket() {
        String user = "user";
        String plaintextPw = "myPassword";
        RadiusPacket packet = new AccessRequest(dictionary, 1, null, user, plaintextPw);
        attribute = new Ipv6PrefixAttribute(packet.getDictionary(), -1, 97, new byte[4]);
        packet.addAttribute(attribute);

        assertEquals(attribute, packet.getAttribute("Framed-IPv6-Prefix"));
    }

    @Test
    void addDelegatedIpv6PrefixToAcceptRejectPacket() {
        RadiusPacket packet = new RadiusPacket(dictionary, ACCESS_REJECT, 3);
        attribute = new Ipv6PrefixAttribute(packet.getDictionary(), -1, 123, new byte[4]);

        final RuntimeException exception = assertThrows(RuntimeException.class,
                () -> packet.addAttribute(attribute));

        assertTrue(exception.getMessage().toLowerCase().contains("ipv6 prefix not allowed in packet"));
    }

    @Test
    void addDelegatedIpv6PrefixToAccountingRequestPacket() {
        String user = "user";
        RadiusPacket packet = new AccountingRequest(DefaultDictionary.INSTANCE, 12, null, user, AccountingRequest.ACCT_STATUS_TYPE_ACCOUNTING_ON);
        attribute = new Ipv6PrefixAttribute(packet.getDictionary(), -1, 123, new byte[4]);
        packet.addAttribute(attribute);

        assertEquals(attribute, packet.getAttribute("Delegated-Ipv6-Prefix"));
    }

    @Test
    void getDataString() {
    }
}