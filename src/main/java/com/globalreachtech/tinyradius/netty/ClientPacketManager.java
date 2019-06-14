package com.globalreachtech.tinyradius.netty;

import com.globalreachtech.tinyradius.dictionary.Dictionary;
import com.globalreachtech.tinyradius.grt.RequestContext;
import com.globalreachtech.tinyradius.packet.RadiusPacket;
import com.globalreachtech.tinyradius.util.RadiusException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.Timer;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class ClientPacketManager implements RadiusClient.PacketManager {

    private static Log logger = LogFactory.getLog(ClientPacketManager.class);

    private final Timer timer;
    private final Dictionary dictionary;

    private final Map<PacketContextKey, PacketContext> contexts = new ConcurrentHashMap<>();

    public ClientPacketManager(Timer timer, Dictionary dictionary) {
        this.timer = timer;
        this.dictionary = dictionary;
    }

    public Future<RadiusPacket> queue(DatagramPacket request) {

    }

    @Override
    public void processInbound(DatagramPacket response) {

        PacketContext context = lookup(response.sender(), response.content());
        if (context == null) {
            logger.info("Request context not found for received packet, ignoring...");
        } else {

            contexts.remove(context);
        }
    }

    private PacketContext lookup(InetSocketAddress address, ByteBuf content) {
        ByteBuf buf = content.duplicate().skipBytes(1);
        int identifier = buf.readByte() & 0xff;

        final PacketContext pc = contexts.get(new PacketContextKey(identifier, address));
        if (pc == null)
            return null;

        try {
            RadiusPacket resp = RadiusPacket.decodeResponsePacket(dictionary,
                    new ByteBufInputStream(content.duplicate()),
                    pc.sharedSecret, pc.request);

            if (logger.isInfoEnabled())
                logger.info(String.format("Found context %d for clientResponse identifier => %d",
                        identifier, resp.getPacketIdentifier()));

            pc.response.setSuccess(resp);
            return pc;

        } catch (IOException | RadiusException ignored) {
            return null;
        }
    }


    private class PacketContextKey {
        // todo can all packets be uniquely identified by this?
        private final int packetIdentifier;
        private final InetSocketAddress address;

        PacketContextKey(int packetIdentifier, InetSocketAddress address) {
            this.packetIdentifier = packetIdentifier;
            this.address = address;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PacketContextKey that = (PacketContextKey) o;
            return packetIdentifier == that.packetIdentifier &&
                    address.equals(that.address);
        }

        @Override
        public int hashCode() {
            return Objects.hash(packetIdentifier, address);
        }
    }

    private class PacketContext {

        private final String sharedSecret;
        private final RadiusPacket request;
        private final Promise<RadiusPacket> response = new DefaultPromise<>();

        public PacketContext(String sharedSecret, RadiusPacket request) {
            this.sharedSecret = sharedSecret;
            this.request = request;
        }
    }
}
