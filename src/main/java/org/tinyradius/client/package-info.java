/**
 * This package contains classes for Radius Client and reference implementations
 * of ChannelHandlers to handle incoming packets.
 * <p>
 * RadiusClient sets up the socket, while the handler contains actual logic to
 * process incoming packets. ClientHandler also requires implementing a method to
 * log/preprocess outgoing packets so in/outbound packets can be matched.
 */
package org.tinyradius.client;