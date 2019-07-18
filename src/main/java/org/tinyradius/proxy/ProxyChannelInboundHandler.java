package org.tinyradius.proxy;

import io.netty.util.Timer;
import io.netty.util.concurrent.Future;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.server.ChannelInboundHandler;
import org.tinyradius.util.Lifecycle;
import org.tinyradius.util.SecretProvider;

/**
 * ChannelInboundHandler that allows any RadiusPacket type and implements Lifecycle.
 */
public class ProxyChannelInboundHandler extends ChannelInboundHandler<RadiusPacket> implements Lifecycle {

    private final Lifecycle requestHandler;

    /**
     * @param dictionary     for encoding/decoding RadiusPackets
     * @param requestHandler ProxyRequestHandler to handle requests. Should also implement {@link Lifecycle}
     *                       as the handler is expected to manage the socket for proxying.
     * @param timer          handle timeouts if requests take too long to be processed
     * @param secretProvider lookup sharedSecret given remote address
     */
    public ProxyChannelInboundHandler(
            Dictionary dictionary,
            LifecycleRequestHandler<RadiusPacket> requestHandler,
            Timer timer,
            SecretProvider secretProvider) {
        super(dictionary, requestHandler, timer, secretProvider, RadiusPacket.class);
        this.requestHandler = requestHandler;
    }

    @Override
    public Future<Void> start() {
        return requestHandler.start();
    }

    @Override
    public void stop() {
        requestHandler.stop();
    }
}
