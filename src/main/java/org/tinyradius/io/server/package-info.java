/**
 * RADIUS server implementation using Netty.
 * <p>
 * This package provides a foundational framework for building RADIUS services.
 * <h2>Key Features</h2>
 * <ul>
 *   <li><b>Extensibility</b>: Designed with interfaces and factories to allow
 *   for easy custom implementations.</li>
 *   <li><b>Proxy Support</b>: The proxy is a promise-based adapter that sits
 *   between the client and server classes, simplifying the implementation of
 *   forwarding logic.</li>
 *   <li><b>Caching</b>: Support for caching responses to handle retransmissions.</li>
 * </ul>
 */
package org.tinyradius.io.server;
