package com.globalreachtech.tinyradius.proxy;

import com.globalreachtech.tinyradius.attribute.RadiusAttribute;
import com.globalreachtech.tinyradius.client.ClientHandler;
import com.globalreachtech.tinyradius.dictionary.Dictionary;
import com.globalreachtech.tinyradius.packet.RadiusPacket;
import com.globalreachtech.tinyradius.util.RadiusEndpoint;
import com.globalreachtech.tinyradius.util.RadiusException;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Promise;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.globalreachtech.tinyradius.packet.RadiusPacket.decodeRequestPacket;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public abstract class ProxyStateClientHandler extends ClientHandler {

    private static Log logger = LogFactory.getLog(ProxyStateClientHandler.class);

    private final AtomicInteger proxyIndex = new AtomicInteger(1);

    private final Timer timer;
    private final Dictionary dictionary;
    private final int timeoutMs;

    private Map<String, Promise<RadiusPacket>> proxyConnections = new ConcurrentHashMap<>();

    public ProxyStateClientHandler(Timer timer, Dictionary dictionary, int timeoutMs) {
        this.timer = timer;
        this.dictionary = dictionary;
        this.timeoutMs = timeoutMs;
    }

    // todo composition over inheritance
    public abstract String getSharedSecret(InetSocketAddress client);

    private String genProxyState() {
        return Integer.toString(proxyIndex.getAndIncrement());
    }

    @Override
    public Promise<RadiusPacket> logOutbound(RadiusPacket packet, RadiusEndpoint endpoint, EventExecutor eventExecutor) {
        // add Proxy-State attribute
        String proxyState = genProxyState();
        packet.addAttribute(new RadiusAttribute(33, proxyState.getBytes()));

        Promise<RadiusPacket> response = eventExecutor.newPromise();
        proxyConnections.put(proxyState, response);

        final Timeout timeout = timer.newTimeout(
                t -> response.tryFailure(new RadiusException("Timeout occurred")), timeoutMs, MILLISECONDS);

        response.addListener(f -> {
            proxyConnections.remove(proxyState);
            timeout.cancel();
        });

        return response;
    }

    /**
     * Creates a RadiusPacket for a Radius clientRequest from a received
     * datagram packet.
     *
     * @param packet received datagram
     * @return RadiusPacket object
     * @throws RadiusException malformed packet
     * @throws IOException     communication error (after getRetryCount()
     *                         retries)
     */
    protected RadiusPacket makeRadiusPacket(DatagramPacket packet, String sharedSecret) throws IOException, RadiusException {
        ByteBufInputStream in = new ByteBufInputStream(packet.content());
        return decodeRequestPacket(dictionary, in, sharedSecret);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket datagramPacket) {
        try {
            String secret = getSharedSecret(datagramPacket.sender());
            if (secret == null) {
                if (logger.isInfoEnabled())
                    logger.info("ignoring packet from unknown server " + datagramPacket.sender() + " received on local address " + datagramPacket.recipient());
                return;
            }

            RadiusPacket packet = makeRadiusPacket(datagramPacket, secret);

            // retrieve my Proxy-State attribute (the last)
            List<RadiusAttribute> proxyStates = packet.getAttributes(33);
            if (proxyStates == null || proxyStates.size() == 0)
                throw new RadiusException("proxy packet without Proxy-State attribute");
            RadiusAttribute proxyState = proxyStates.get(proxyStates.size() - 1);

            String connectionId = new String(proxyState.getAttributeData());

            final Promise<RadiusPacket> response = proxyConnections.get(connectionId);

            if (response == null) {
                logger.info("Request context not found for received packet, ignoring...");
                return;
            }

            if (logger.isInfoEnabled())
                logger.info(String.format("Found context %s for response identifier => %d",
                        connectionId, packet.getPacketIdentifier()));

            response.trySuccess(packet);
        } catch (IOException ioe) {
            // error while reading/writing socket
            logger.error("communication error", ioe);
        } catch (RadiusException re) {
            logger.error("malformed Radius packet", re);
        }
    }
}
