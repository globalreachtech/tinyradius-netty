/**
 * This package contains classes for Radius Server and reference implementation of
 * ChannelHandlers for incoming packets.
 * <p>
 * ServerHandler is a base implementation of SimpleChannelInboundHandler that
 * should be used or extended in most cases. It converts between DatagramPackets and
 * RadiusPackets and handles incoming packets and sending responses.
 * <p>
 * ServerHandler depends on a Deduplicator to handle duplicate requests. A default
 * implementation of Deduplicator is provided that considers packets duplicate if
 * packetIdentifier and remote address matches.
 * <p>
 * Basic implementations of Accounting and Access Request handlers are also included
 * for the most trivial use cases.
 */
package org.tinyradius.server;