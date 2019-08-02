/**
 * {@link org.tinyradius.client.retry.RetryStrategy} schedules a retry in the future
 * and handles timeouts.
 * <p>
 * {@link org.tinyradius.client.retry.RetryStrategy#scheduleRetry} is invoked after
 * every client request is sent to schedule a retry or timeout check in the future.
 * <p>
 * A naive version is implemented which waits fixed timeout and after a set
 * max attempts, fails the request promise. Custom implementations can be built
 * for exponential backoff.
 */
package org.tinyradius.client.retry;