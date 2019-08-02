/**
 * Callback handlers that are invoked when a packet is received.
 * <p>
 * {@link org.tinyradius.server.handler.RequestHandler} is defined
 * so custom handlers do not have to be concerned with how netty handles packets
 * and serialization between Datagram and {@link org.tinyradius.packet.RadiusPacket}.
 * <p>
 * Basic implementations of Accounting and Access RequestHandlers are included
 * for the most trivial use cases. A simple DeduplicatorHandler that returns
 * null for duplicates is also included.
 */
package org.tinyradius.server.handler;