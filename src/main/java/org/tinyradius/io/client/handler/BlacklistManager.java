package org.tinyradius.io.client.handler;

import org.jspecify.annotations.NonNull;

import java.net.SocketAddress;

/**
 * Manages blacklisting of unresponsive or failed RADIUS endpoints.
 * <p>
 * Implementations track endpoints that have failed authentication
 * or timed out, and prevent further requests from being sent
 * to those endpoints until they recover.
 */
public interface BlacklistManager {

    /**
     * Checks if the given socket address is currently blacklisted.
     *
     * @param address the socket address to check
     * @return {@code true} if blacklisted, {@code false} otherwise
     */
    boolean isBlacklisted(@NonNull SocketAddress address);

    /**
     * Logs a communication or request failure for the given socket address.
     *
     * @param address the socket address where the failure occurred
     * @param cause   the cause of the failure
     */
    void logFailure(@NonNull SocketAddress address, @NonNull Throwable cause);

    /**
     * Resets any blacklist status and failure counts for the given socket address.
     *
     * @param address the socket address to reset
     */
    void reset(@NonNull SocketAddress address);
}
