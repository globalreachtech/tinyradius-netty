/**
 * {@link org.tinyradius.client.timeout.TimeoutHandler} schedules a retry in the future
 * and handles timeouts.
 * <p>
 * {@link org.tinyradius.client.timeout.TimeoutHandler#onTimeout} is invoked after
 * every client request is sent to schedule a retry or timeout check in the future.
 */
package org.tinyradius.client.timeout;