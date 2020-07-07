/**
 * {@link org.tinyradius.io.client.timeout.TimeoutHandler} schedules a retry
 * in the future and handles timeouts.
 * <p>
 * A basic implementation that times out based on fixed period and max attempt
 * count is included. Custom implementations such as exponential backoff or
 * using external service to determine timeouts/retries is possible in the same way.
 */
package org.tinyradius.io.client.timeout;