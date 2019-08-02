/**
 * Classes for Radius Server, and reference implementation of
 * ChannelHandlers for incoming packets.
 * <p>
 * ChannelInboundHandler is a base implementation of SimpleChannelInboundHandler that
 * should be used in most cases. It converts between DatagramPackets and
 * RadiusPackets and handles incoming packets and sending responses.
 * <p>
 * ChannelInboundHandler uses underlying RequestHandler for logic to handle RadiusPackets.
 */
package org.tinyradius.server;