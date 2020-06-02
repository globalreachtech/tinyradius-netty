/**
 * Classes for encoding and decoding Radius packets.
 * <p>
 * The interface {@link org.tinyradius.packet.RadiusPacket} supports
 * getters for the core elements of a packet. {@link org.tinyradius.packet.BaseRadiusPacket}
 * provides the reference implementation with support for managing attributes.
 * <p>
 * {@link org.tinyradius.packet.request.RadiusRequest} and {@link org.tinyradius.packet.response.RadiusResponse}
 * support encoding and verifying authenticators for requests and responses
 * respectively. Other subclasses inherit from these to support their specific
 * authentication mechanisms and data validation.
 * <p>
 * Each Radius packet has an associated dictionary of attribute
 * types that allows the convenient access to packet attributes
 * and sub-attributes.
 */
package org.tinyradius.packet;