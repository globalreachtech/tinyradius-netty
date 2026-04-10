/**
 * Reference implementations of Netty ChannelHandlers for RadiusClient.
 * <p>
 * This package provides pluggable handlers for the Netty channel pipeline
 * that handle various aspects of RADIUS client functionality:
 * <ul>
 *   <li><b>ClientDatagramCodec</b>: Decodes incoming RADIUS packets and encodes outgoing</li>
 *   <li><b>PromiseAdapter</b>: Converts async responses to blocking promises</li>
 *   <li><b>BlacklistHandler</b>: Filters requests to blacklisted endpoints</li>
 * </ul>
 * <p>
 * These handlers can be combined and customized for different deployment
 * scenarios. The client is typically configured with a pipeline containing
 * these handlers plus any custom handlers for application logic.
 */
package org.tinyradius.io.client.handler;