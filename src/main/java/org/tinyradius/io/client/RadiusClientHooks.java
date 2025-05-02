package org.tinyradius.io.client;

import java.net.InetSocketAddress;

/**
 * Instrumentation hooks for RadiusClient. 
 * <code>preSendHook</code> function to invoke before sending the radius request. 
 * Parameters are the type of request and InetSocket address of the remote
 * <code>timeoutHook</code> function to invoke when a timeout expires. Parameters
 * are the type of the request and the InetSocketAddress of the remote.
 * <code>postReceiveHook</code> function to invoke when a responsne is received.
 * Parameters are the type of the response and the InetSocketAddress of the remote.
 */
public interface RadiusClientHooks {
    void preSendHook(int code, InetSocketAddress address);
    void timeoutHook(int code, InetSocketAddress address);
    void postReceiveHook(int code, InetSocketAddress address);
}
