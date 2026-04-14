package org.tinyradius.io.server;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.tinyradius.core.packet.request.RadiusRequest;

import java.net.InetSocketAddress;

/**
 * Interface for providing RADIUS shared secrets based on the remote address or request.
 */
public interface SecretProvider {

    /**
     * Returns the shared secret used to communicate with the client/host with the
     * passed IP address or null if the client is not allowed at this server.
     *
     * @param address IP address and port number of remote host/client
     * @return shared secret or null
     */
    @Nullable
    String getSharedSecret(@NonNull InetSocketAddress address);

    /**
     * An alternative method of returning shared secret but with the
     * current radius request so that alternative secrets can be returned based on
     * some sort of context.
     * <p>
     * By default, this method calls the original getSharedSecret.
     *
     * @param address IP address and port number of remote host/client
     * @param request the RadiusRequest relating to this request
     * @return shared secret or null
     */
    @SuppressWarnings("unused")
    @Nullable
    default String getSharedSecret(@NonNull InetSocketAddress address, @NonNull RadiusRequest request) {
        return getSharedSecret(address);
    }

}
