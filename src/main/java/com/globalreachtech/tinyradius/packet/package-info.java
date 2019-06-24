/**
 * This package contains classes for encoding and decoding Radius packets.
 * <p>
 * The base class RadiusPacket can read and write every Radius packet, the
 * subclasses AccessRequest and AccountingRequest provide convenience
 * methods to ease the access of special packet attributes.
 * <p>
 * The class AccessRequest manages the encryption of
 * passwords in the attributes User-Password (PAP) or CHAP-Password and
 * CHAP-Challenge (CHAP).
 * <p>
 * Each Radius packet has an associated dictionary of attribute
 * types that allows the convenient access to packet attributes
 * and sub-attributes.
 */
package com.globalreachtech.tinyradius.packet;