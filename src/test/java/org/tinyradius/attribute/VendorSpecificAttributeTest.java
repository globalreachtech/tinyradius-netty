package org.tinyradius.attribute;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VendorSpecificAttributeTest {

    @Test
    void toByteArray() {

        // normal Attributes, we can check in constructor we aren't passing in too big a byte array
        // VSA we manipulate sub-attributes, and only get array when we call toByteArray()
        // todo test size isn't too big
    }
}