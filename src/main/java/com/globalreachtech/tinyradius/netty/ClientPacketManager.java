package com.globalreachtech.tinyradius.netty;

import com.globalreachtech.tinyradius.grt.RequestContext;
import com.globalreachtech.tinyradius.packet.RadiusPacket;
import com.globalreachtech.tinyradius.util.RadiusException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.Timer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.requireNonNull;

public class ClientPacketManager implements RadiusClient.PacketManager {

    private final Timer timer;
    private final Set<PacketContext> packets = ConcurrentHashMap.newKeySet();

    public ClientPacketManager(Timer timer) {

        this.timer = timer;
    }

    @Override
    public void process(DatagramPacket packet) {

        PacketContext p = new PacketContext(packet.getPacketIdentifier(), address, packet.getAuthenticator());

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

            dequeue(context);
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


    private class PacketContext {

        private final int packetIdentifier;
        private final InetSocketAddress address;
        private final String sharedSecret;

    }
}
