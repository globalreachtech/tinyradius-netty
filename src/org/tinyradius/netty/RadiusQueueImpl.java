package org.tinyradius.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.socket.DatagramPacket;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.util.RadiusEndpoint;
import org.tinyradius.util.RadiusException;

import java.io.IOException;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * RadiusQueueImpl class
 */
public class RadiusQueueImpl implements RadiusQueue {

    private Dictionary dictionary;

    private AtomicInteger identifier =
            new AtomicInteger();

    private PriorityQueue<RadiusRequestContextImpl> queue =
            new PriorityQueue<RadiusRequestContextImpl>(10, new Comparator<RadiusRequestContextImpl>() {
                public int compare(RadiusRequestContextImpl o1, RadiusRequestContextImpl o2) {
                    if (o2 == null)
                        return 1;
                    if (o1 == null)
                        return -1;
                    if (o1.request.getPacketIdentifier() > o2.request.getPacketIdentifier())
                        return 1;
                    if (o2.request.getPacketIdentifier() > o1.request.getPacketIdentifier())
                        return -1;
                    return 0;
                }
            });

    private LinkedHashSet<RadiusRequestContext> pending =
            new LinkedHashSet<RadiusRequestContext>();

    public RadiusQueueImpl(Dictionary dictionary) {
        if (dictionary == null)
            throw new NullPointerException("dictionary cannot be null");
        this.dictionary = dictionary;
    }

    /**
     *
     * @param request
     * @param endpoint
     * @return
     */
    public RadiusRequestContext queue(RadiusPacket request, RadiusEndpoint endpoint) {
        if (request == null)
            throw new NullPointerException("request cannot be null");
        if (endpoint == null)
            throw new NullPointerException("request cannot be null");

        try {
            RadiusPacket queued = (RadiusPacket) request.clone();
            RadiusRequestContextImpl context =
                    new RadiusRequestContextImpl(identifier.getAndIncrement(), queued, endpoint);

            /* override the identifier based on the current seed */
            queued.setDictionary(dictionary);
            queued.setPacketIdentifier(context.identifier() & 0xf);

            System.out.println("Queued request " + context.identifier() +
                    " identifier => " + queued.getPacketIdentifier());

            for (RadiusRequestContextImpl ctx : queue) {
                System.out.println("Context(" + ctx.identifier() + ") => " +
                        ctx.request().getPacketIdentifier());
            }

            /* TODO: Check request hasn't already been queued */
            queue.add(context);

            return context;

        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    public void dequeue(RadiusRequestContext context) {
        if (context == null)
            throw new NullPointerException("context cannot be null");
        queue.remove(context);
    }

    /**
     *
     * @param response
     * @return
     */
    public RadiusRequestContext lookup(DatagramPacket response) {
        if (response == null)
            throw new NullPointerException("response cannot be null");

        ByteBuf buf = response.content()
                .duplicate().skipBytes(1);

        int identifier = buf.readByte() & 0xff;
        buf.skipBytes(2); /* length */
        ByteBuf authenticator = buf.readBytes(16);

        for (RadiusRequestContextImpl context : queue) {
            if (identifier != context.request().getPacketIdentifier())
                continue;
            if (!(response.sender().equals(context.endpoint().getEndpointAddress())))
                continue;
            try {
                /* Avoid uncessary lookups for already processed responses */
                if (context.response != null &&
                        authenticator.equals(Unpooled.wrappedBuffer(
                                context.response.getAuthenticator()))) {

                    System.out.println("Found processed context " + context.identifier() + " for response " +
                            "identifier => " + context.response.getPacketIdentifier());

                    return context;
                }

                context.setResponse(RadiusPacket.decodeResponsePacket(dictionary,
                    new ByteBufInputStream(buf.resetReaderIndex()), context.endpoint.getSharedSecret(), context.request()));

                System.out.println("Found context " + context.identifier() + " for response " +
                        "identifier => " + context.response.getPacketIdentifier());

                return context;

            } catch (IOException ioe) {
                ioe.printStackTrace();
            } catch (RadiusException e) {
            }
        }

        return null;
    }

    private class RadiusRequestContextImpl implements RadiusRequestContext {

        private int identifier;
        private RadiusPacket request;
        private RadiusPacket response;
        private RadiusEndpoint endpoint;

        public RadiusRequestContextImpl(int identifier, RadiusPacket request, RadiusEndpoint endpoint) {
            if (request == null)
                throw new NullPointerException("request cannot be null");
            if (endpoint == null)
                throw new NullPointerException("endpoint cannot be null");
            this.identifier = identifier;
            this.request = request;
            this.endpoint = endpoint;
        }

        public RadiusPacket request() {
           return request;
        }

        public RadiusEndpoint endpoint() {
            return endpoint;
        }

        void setResponse(RadiusPacket response) {
            this.response = response;
        }

        public RadiusPacket response() {
            return response;
        }

        public int identifier() {
            return identifier;
        }

        public String toString() {
            return Integer.valueOf(identifier).toString();
        }
    }
}