package org.tinyradius.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.socket.DatagramPacket;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.util.RadiusEndpoint;
import org.tinyradius.util.RadiusException;

import java.io.IOException;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * RadiusQueueImpl class
 */
public class RadiusQueueImpl implements RadiusQueue {

    private static Log logger = LogFactory.getLog(RadiusClient.class);

    private AtomicInteger identifier = new AtomicInteger();

    private PriorityBlockingQueue<RadiusQueueEntryImpl> queue =
            new PriorityBlockingQueue<RadiusQueueEntryImpl>(10, new Comparator<RadiusQueueEntryImpl>() {
                public int compare(RadiusQueueEntryImpl o1, RadiusQueueEntryImpl o2) {
                    if (o2 == null)
                        return 1;
                    if (o1 == null)
                        return -1;

                    RadiusRequestContext ctx1 = o1.context;
                    RadiusRequestContext ctx2 = o2.context;

                    if (ctx1.request().getPacketIdentifier() > ctx2.request().getPacketIdentifier())
                        return 1;
                    if (ctx2.request().getPacketIdentifier() > ctx1.request().getPacketIdentifier())
                        return -1;

                    return 0;
                }
            });

    private LinkedHashSet<RadiusQueueEntry> pending =
            new LinkedHashSet<RadiusQueueEntry>();

    /**
     *
     * @param context
     * @return
     */
    public RadiusQueueEntry queue(RadiusRequestContext context) {
        if (context == null)
            throw new NullPointerException("context cannot be null");

        RadiusPacket request = context.request();
        RadiusQueueEntryImpl entry =
                new RadiusQueueEntryImpl(identifier.getAndIncrement(), context);

        request.setPacketIdentifier(entry.identifier() & 0xf);

        if (logger.isInfoEnabled())
            logger.info(String.format("Queued request %d identifier => %d",
                entry.identifier(), request.getPacketIdentifier()));

        /* TODO: Check request hasn't already been queued */
        queue.add(entry);

        return entry;
    }

    public void dequeue(RadiusQueueEntry context) {
        if (context == null)
            throw new NullPointerException("context cannot be null");
        queue.remove(context);
    }

    public void doit() {

        RadiusQueueEntryImpl entry;

        while ((entry = queue.poll()) != null)
        {
            RadiusRequestContext ctx = entry.context();
            System.out.println("Context(" + entry + ") identifier => " + ctx.request().getPacketIdentifier());
        }
    }

    /**
     *
     * @param response
     * @return
     */
    public RadiusQueueEntry lookup(DatagramPacket response) {
        if (response == null)
            throw new NullPointerException("response cannot be null");

        ByteBuf buf = response.content()
                .duplicate().skipBytes(1);

        int identifier = buf.readByte() & 0xff;

        for (RadiusQueueEntryImpl entry : queue) {
            RadiusRequestContext context = entry.context();
            if (identifier != context.request().getPacketIdentifier())
                continue;
            if (!(response.sender().equals(context.endpoint().getEndpointAddress())))
                continue;
            try {
                /* XXX: Will find a better way of determining if the response authenticator
                   matches the request but for now attempt to parse the packet which will
                   also check the authenticator */

                RadiusPacket resp = RadiusPacket.decodeResponsePacket(
                        new ByteBufInputStream(buf.resetReaderIndex()),
                        context.endpoint().getSharedSecret(), context.request());

                logger.info(String.format("Found context %d for response identifier => %d",
                        entry.identifier(), resp.getPacketIdentifier()));

                return entry;

            } catch (IOException ioe) {
            } catch (RadiusException e) {
            }
        }

        return null;
    }

    private class RadiusQueueEntryImpl implements RadiusQueueEntry {

        private int identifier;
        private RadiusRequestContext context;

        public RadiusQueueEntryImpl(int identifier, RadiusRequestContext context) {
            if (context == null)
                throw new NullPointerException("context cannot be null");
            this.identifier = identifier;
            this.context = context;
        }

        public int identifier() {
            return identifier;
        }
        public RadiusRequestContext context() {
            return context;
        }
        public String toString() {
            return Integer.valueOf(identifier).toString();
        }
    }
}