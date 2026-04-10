/**
 * Reference implementations of Netty ChannelHandlers for RadiusServer.
 * <p>
 * This package provides pluggable handlers for the Netty channel pipeline
 * that handle various aspects of RADIUS server functionality:
 * <ul>
 *   <li><b>ServerPacketCodec</b>: Decodes incoming RADIUS packets and encodes outgoing responses</li>
 *   <li><b>RequestHandler</b>: User-defined handler for processing requests and generating responses</li>
 *   <li><b>BasicCachingHandler</b>: Caches responses for handling retransmissions</li>
 *   <li><b>ProxyHandler</b>: Forwards requests to upstream RADIUS servers</li>
 * </ul>
 * <p>
 * These handlers can be combined and customized for different deployment
 * scenarios. The server is typically configured with a pipeline containing
 * these handlers plus any custom handlers for application logic.
 */
package org.tinyradius.io.server.handler;