# Issue Ipv6PrefixAttribute validation failure

## Summary

The validation for an Ipv6PrefixAttribute incorrectly rejects valid values. It works properly only when prefix sizes are multiple of 8.

For example, `2001:0:0:1650:0:0:0:0/60` will not be validated, although it is correct (the bits after the 60 are zero). Instead, and exception is thrown with the following message:

```
java.lang.IllegalArgumentException: Prefix-Length is 60, actual address has prefix length 63, bits outside of the Prefix-Length must be zero
```

## Analysis

The issue is due to the fact that the number of non-zero bits of the IPv6 prefix are calculated using a `java.util.BitSet`, which is initialized by passing the 16 bytes of the IPv6 address with `BitSet.valueOf()`. *But the created bits are using a little-endian representation (https://docs.oracle.com/javase/8/docs/api/java/util/BitSet.html#valueOf-byte:A-), instead of a big-endian as required to do the comparation properly.*

For example, the prefix above is represented as shown below by BitSet

```
# Bitset representation (first line) vs Correct representation (sedond line)
00000100-10000000-00000000-00000000-00000000-00000000-01101000-00001010-00000000-00000000-00000000-00000000-00000000-00000000-00000000-00000000
00100000-00000001-00000000-00000000-00000000-00000000-00010110-01010000-00000000-00000000-00000000-00000000-00000000-00000000-00000000-00000000
```

This has been produced using the following code snippet

```java
InetAddress address = InetAddress.getByName("2001:0:0:1650::0");
byte[] addrBytes = address.getAddress();

final BitSet bitSet = BitSet.valueOf(addrBytes);
final int bitSetLength = bitSet.length();
System.out.println("Bitset representation");
for(int i = 0; i < 128; i++){
    if(bitSet.get(i)) System.out.print("1"); else System.out.print("0");
    // To separate the bytes for better reading
    if(i % 8 == 7) System.out.print("-");
}
System.out.println();
System.out.println("Correct IPv6 representation");
for(int i = 0; i < addrBytes.length; i++){
    System.out.print(String.format("%8s-", Integer.toBinaryString((int)addrBytes[i] & 0xff)).replace(" ","0"));
}
System.out.println();
System.out.println(bitSetLength);
```

## Fix

The class `Ipv6PrefixAttribute.java` must be updated.

Since probably you don't want to introduce external dependencies to libraries such as `https://seancfoley.github.io/IPAddress/` or `https://mvnrepository.com/artifact/com.github.jgonian/commons-ip-math`, the checking can be done using pure java. The code is not trivial to read but only has two lines

```java
// Preparation with test data
InetAddress ipv6Address = InetAddress.getByName("ffff:ffff:ffff:fff0::0");
byte[] addrBytes = ipv6Address.getAddress();
int prefix = 60;

// Check the first byte that must have some zeroed bits is conformant. This one is special because the
// comparison requires masking some bits, instead of checking the full byte as zero
boolean passed = (addrBytes[p/8] & &0xff (0xff >> p - 8*(p/8))) == 0;
// Check the rest of the bytes
for(int i = p/8 + 1; i < 16; i++) passed = passed & (addrBytes[i] == 0);

// passed is true if the address bytes do not have bits to 1 after the prefix
```

Since BitSet is not used, the building of the attributes must be slightly changed. I'd like better just to have all the bytes in the attribute sent, even if zero, so using...

```java
    return ByteBuffer.allocate(18)
            .put((byte) 0)
            .put((byte) prefixLength)
            .put(addressBytes)
            .array();
```

...but this breaks the `BaseRadiusPacketTest.addAttribute()` test, because it is checking a string output that includes the expected length, and I'd rather minimize the proposed changes in this pull request.

## Testing

A new test is added for an IPv6 prefix with a prefix which is not a multipe of 8

``` java
@Test
void string60bitsOk() {
    final Ipv6PrefixAttribute attribute = (Ipv6PrefixAttribute)
            IPV6_PREFIX.create(dictionary, -1, 97, (byte) 0, "2001:0:0:1650:0:0:0:0/60"); // Framed-IPv6-Prefix
    assertEquals("2001:0:0:1650:0:0:0:0/60", attribute.getValueString());
}
```

This test will fail in the current implementation and will pass with the fix.