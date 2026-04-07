package org.tinyradius.io.client.handler;

import org.jspecify.annotations.NonNull;

import java.net.SocketAddress;

public interface BlacklistManager {

    boolean isBlacklisted(@NonNull SocketAddress address);

    void logFailure(@NonNull SocketAddress address, @NonNull Throwable cause);

    void reset(@NonNull SocketAddress address);
}
