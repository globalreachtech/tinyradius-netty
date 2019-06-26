package org.tinyradius.proxy;

import io.netty.util.Timer;
import io.netty.util.concurrent.Future;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.server.ServerChannelInboundHandler;
import org.tinyradius.util.SecretProvider;

/**
 * ServerChannelInboundHandler that allows any RadiusPacket type and adds lifecycle methods.
 */
public class ProxyChannelInboundHandler extends ServerChannelInboundHandler<RadiusPacket> {

    private final ProxyRequestHandler requestHandler;

    /**
     * @param dictionary     for encoding/decoding RadiusPackets
     * @param requestHandler handle requests
     * @param timer          handle timeouts if requests take too long to be processed
     * @param secretProvider lookup sharedSecret given remote address
     */
    public ProxyChannelInboundHandler(Dictionary dictionary, ProxyRequestHandler requestHandler, Timer timer, SecretProvider secretProvider) {
        super(dictionary, requestHandler, timer, secretProvider, RadiusPacket.class);
        this.requestHandler = requestHandler;
    }

    /***
     * @return future completes when underlying ProxyRequestHandler initializes
     */
    public Future<Void> start() {
        return requestHandler.start();
    }

    /**
     * Close sockets used by underlying RequestHandler
     */
    public void stop() {
        requestHandler.stop();
    }
}
