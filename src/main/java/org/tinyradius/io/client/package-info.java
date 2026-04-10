/**
 * RADIUS client implementation using Netty.
 * <p>
 * This package provides a non-blocking client for sending RADIUS requests
 * and receiving responses.
 * <h2>Key Features</h2>
 * <ul>
 *   <li><b>Asynchronous IO</b>: Implemented using Netty {@code ChannelHandlers}.</li>
 *   <li><b>Promises</b>: Blocking calls are handled using promises/futures to
 *   maintain the asynchronous nature of the library.</li>
 *   <li><b>Retries</b>: Automatically handles retries based on configured timeouts.</li>
 *   <li><b>Blacklisting</b>: Support for blacklisting unresponsive endpoints.</li>
 * </ul>
 */
package org.tinyradius.io.client;
