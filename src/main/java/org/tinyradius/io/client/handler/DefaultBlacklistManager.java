package org.tinyradius.io.client.handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.NonNull;

import java.net.SocketAddress;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Default implementation of {@link BlacklistManager}.
 * <p>
 * Tracks failed requests per endpoint and blacklists endpoints that exceed
 * a configurable failure threshold. Endpoints are automatically removed
 * from the blacklist after a configurable TTL expires.
 * <p>
 * This implementation uses an in-memory ConcurrentHashMap to store
 * failure counts and timestamps. For production deployments with
 * multiple client instances, consider using a shared store (e.g., Redis).
 */
public class DefaultBlacklistManager implements BlacklistManager {

    private static final Logger log = LogManager.getLogger(DefaultBlacklistManager.class);
    private final long blacklistTtlMs;
    private final int failCountThreshold;
    private final Clock clock;

    private final Map<SocketAddress, AtomicInteger> failCounts = new ConcurrentHashMap<>();
    private final Map<SocketAddress, Long> blacklist = new ConcurrentHashMap<>();

    /**
     * @param blacklistTtlMs     time-to-live for blacklist entries in milliseconds
     * @param failCountThreshold number of failures before blacklisting an endpoint
     * @param clock              clock for timestamp operations
     */
    public DefaultBlacklistManager(long blacklistTtlMs, int failCountThreshold, Clock clock) {
        this.blacklistTtlMs = blacklistTtlMs;
        this.failCountThreshold = failCountThreshold;
        this.clock = clock;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isBlacklisted(@NonNull SocketAddress socketAddress) {
        Long blacklistExpiry = blacklist.get(socketAddress);

        // not blacklisted
        if (blacklistExpiry == null)
            return false;

        // blacklist active
        if (clock.millis() < blacklistExpiry) {
            return true;
        }

        // blacklist expired
        reset(socketAddress);
        log.info("Endpoint {} removed from blacklist (expired)", socketAddress);
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void logFailure(@NonNull SocketAddress address, @NonNull Throwable cause) {
        if (cause instanceof TimeoutException) {
            int failCount = failCounts.computeIfAbsent(address, k -> new AtomicInteger()).incrementAndGet();

            if (failCount >= failCountThreshold && blacklist.get(address) == null) {

                // only set if isn't already blacklisted to avoid delayed responses extending ttl
                blacklist.put(address, clock.millis() + blacklistTtlMs);
                log.debug("Endpoint {} added to blacklist", address);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset(@NonNull SocketAddress socketAddress) {
        blacklist.remove(socketAddress);
        failCounts.remove(socketAddress);
    }
}
