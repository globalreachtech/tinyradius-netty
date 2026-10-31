package org.tinyradius.core.attribute.type;

import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;
import org.tinyradius.core.dictionary.DefaultDictionary;
import org.tinyradius.core.dictionary.Dictionary;

import static org.junit.jupiter.api.Assertions.*;

class AnonSubAttributeTest {

    private static final Dictionary dictionary = DefaultDictionary.INSTANCE;

    @Test
    void testAnonSubAttribute() {
        byte[] bytes = new byte[]{1, 2, 3, 4, 5};
        var buf = Unpooled.wrappedBuffer(bytes);
        var attr = new AnonSubAttribute(dictionary, 99999, buf);

        assertSame(dictionary, attr.getDictionary());
        assertSame(buf, attr.getData());
        assertEquals(99999, attr.getVendorId());
        assertEquals(0, attr.getType());
        assertTrue(attr.getTag().isEmpty());
        assertArrayEquals(bytes, attr.getValue());
        assertEquals("[Unparsable sub-attribute (vendorId 99999, length 5)]", attr.getValueString());
        assertEquals("[Unparsable sub-attribute (vendorId 99999, length 5)]", attr.toString());
    }

    @Test
    void testInvalidVendorId() {
        var buf = Unpooled.wrappedBuffer(new byte[]{1, 2});
        var ex = assertThrows(IllegalArgumentException.class, () -> new AnonSubAttribute(dictionary, -1, buf));
        assertTrue(ex.getMessage().contains("should not be -1"));
    }
}
