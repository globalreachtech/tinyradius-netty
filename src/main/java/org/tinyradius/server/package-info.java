/**
 * This package contains classes for Radius Server, Proxy, and reference implementation of
 * ChannelHandlers for incoming packets.
 * <p>
 * ServerChannelInboundHandler is a base implementation of SimpleChannelInboundHandler that
 * should be used in most cases. It converts between DatagramPackets and
 * RadiusPackets and handles incoming packets and sending responses.
 * <p>
 * ServerChannelInboundHandler uses underlying RequestHandler for logic to handle RadiusPackets.
 * <p>
 * Basic implementations of Accounting and Access RequestHandlers are included
 * for the most trivial use cases. A simple Deduplicator handler is also included.
 */
package org.tinyradius.server;