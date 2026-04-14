/**
 * Core classes for the RADIUS protocol implementation.
 * <p>
 * This package handles the fundamental logic of the RADIUS protocol, including:
 * <ul>
 *   <li><b>Parsing &amp; Building</b>: Logic for constructing and deconstructing RADIUS packets.</li>
 *   <li><b>Validation</b>: Ensuring packets meet RFC specifications and vendor-specific requirements.</li>
 *   <li><b>Immutability</b>: Packets and Attributes are designed to be fully immutable for thread safety and predictability.</li>
 *   <li><b>Security</b>: Signing and verifying Request Authenticators for Access and Accounting requests/responses.
 *   Supports PAP, CHAP, and EAP (Message-Authenticator) verification and encoding.</li>
 * </ul>
 * <h2>Design Philosophy</h2>
 * The library is designed to be open and extensible, making liberal use of
 * interfaces, parsers, and factories instead of direct constructors to allow
 * for custom implementations and vendor-specific extensions.
 */
package org.tinyradius.core;
