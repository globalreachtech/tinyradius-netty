/**
 * Attribute encryption and decryption codecs for RADIUS attributes.
 * <p>
 * Some RADIUS attributes contain sensitive data (e.g., passwords) that require
 * encryption using the shared secret. This package provides codecs for various
 * encryption methods:
 * <ul>
 *   <li><b>User-Password</b> (RFC2865): CBC-MD5 encryption for User-Password attribute</li>
 *   <li><b>Tunnel-Password</b> (RFC2868): For Tunnel-Password attributes</li>
 *   <li><b>Ascend-Secret</b> (Vendor-specific): Used by Ascend/Nortel equipment</li>
 *   <li><b>No-Op</b>: No encryption, passthrough</li>
 * </ul>
 * <p>
 * Each codec implements encryption using the Request Authenticator and shared secret
 * as defined in the relevant RFC specifications.
 */
package org.tinyradius.core.attribute.codec;