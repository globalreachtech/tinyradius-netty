/**
 * Classes for Radius Proxy and reference implementation of
 * ProxyHandler for incoming packets.
 * <p>
 * RadiusProxy sets up listeners for client requests and ProxyHandler. ProxyHandler handles
 * client requests, and uses an instance of {@link org.tinyradius.client.RadiusClient} to
 * manage all communications with upstream server.
 */
package org.tinyradius.proxy;