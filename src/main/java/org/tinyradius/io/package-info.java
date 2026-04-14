/**
 * I/O interfaces and classes for Netty-based RADIUS networking.
 * <p>
 * This package provides a modernized, non-blocking asynchronous architecture
 * for RADIUS, including:
 * <ul>
 *   <li><b>Asynchronous I/O</b>: Built on Netty to handle asynchronous sending
 *   and receiving of RADIUS packets.</li>
 *   <li><b>Pipeline Processing</b>: Uses Netty's {@code ChannelHandler} interceptor
 *   filter pattern to manage the packet lifecycle, including decoding, processing logic,
 *   and encoding.</li>
 *   <li><b>Timeouts &amp; Retries</b>: Support for scheduling retries and managing
 *   request timeouts efficiently.</li>
 *   <li><b>Thread Management</b>: Leverages Netty's event loop for efficient
 *   thread utilization.</li>
 * </ul>
 */
package org.tinyradius.io;
