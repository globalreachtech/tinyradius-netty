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

    boolean isBlacklisted(@NonNull SocketAddress address);

    void logFailure(@NonNull SocketAddress address, @NonNull Throwable cause);

    void reset(@NonNull SocketAddress address);
}
