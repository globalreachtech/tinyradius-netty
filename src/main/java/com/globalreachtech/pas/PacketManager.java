package com.globalreachtech.pas;

import com.globalreachtech.tinyradius.client.ClientPacketManager;
import com.globalreachtech.tinyradius.dictionary.Dictionary;
import com.globalreachtech.tinyradius.packet.RadiusPacket;
import com.globalreachtech.tinyradius.proxy.ProxyPacketManager;
import com.globalreachtech.tinyradius.util.RadiusEndpoint;
import com.globalreachtech.tinyradius.util.RadiusException;
import com.globalreachtech.tinyradius.util.RadiusProxyConnection;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.Timer;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Promise;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.net.InetSocketAddress;

import static java.util.Objects.requireNonNull;

public class PacketManager implements ClientPacketManager, ProxyPacketManager {

    private static Log logger = LogFactory.getLog(PacketManager.class);

    private RadiusQueue<RequestContext> queue = new RadiusQueue<>();

    private Dictionary dictionary;

    public PacketManager(Dictionary dictionary, Timer timer) {
        this.dictionary = dictionary;
    }

    @Override
    public Promise<RadiusPacket> logOutbound(RadiusPacket packet, RadiusEndpoint endpoint, EventExecutor eventExecutor) {
        return null;
    }

    @Override
    public void logInbound(DatagramPacket packet) {
        RequestContext context = lookup(packet);
        if (context == null) {
            logger.info("Request context not found for received packet, ignoring...");
        } else {
            context.calculateResponseTime();

            if (logger.isInfoEnabled()) {
                logger.info(String.format("Received clientResponse in %d.%dms: %s\nFor clientRequest: %s",
                        context.responseTime() / 1000000,
                        context.responseTime() % 1000000 / 10000,
                        context.clientResponse().toString(),
                        context.clientRequest().toString()));
            }

        }
    }


    private RequestContext lookup(DatagramPacket response) {
        requireNonNull(response, "clientResponse cannot be null");

        ByteBuf buf = response.content().duplicate().skipBytes(1);

        int identifier = buf.readByte() & 0xff;

        for (RequestContext context : queue.get(identifier)) {
            if (identifier != context.clientRequest().getPacketIdentifier())
                continue;
            if (!(response.sender().equals(
                    context.endpoint().getEndpointAddress())))
                continue;
            try {
                RadiusPacket resp = RadiusPacket.decodeResponsePacket(dictionary,
                        new ByteBufInputStream(response.content().duplicate()),
                        context.endpoint().getSharedSecret(), context.clientRequest());

                if (logger.isInfoEnabled())
                    logger.info(String.format("Found context %d for clientResponse identifier => %d",
                            context.identifier(), resp.getPacketIdentifier()));

                context.setClientResponse(resp);

                return context;

            } catch (IOException | RadiusException e) {
            }
        }

        return null;
    }

    @Override
    public void putProxyConnection(String proxyIndex, RadiusProxyConnection proxyConnection) {

    }

    @Override
    public RadiusProxyConnection removeProxyConnection(String proxyIndex) {
        return null;
    }

    @Override
    public boolean isClientPacketDuplicate(RadiusPacket packet, InetSocketAddress address) {
        return false;
    }
}
