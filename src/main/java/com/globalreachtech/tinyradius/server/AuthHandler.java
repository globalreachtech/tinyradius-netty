package com.globalreachtech.tinyradius.server;

import com.globalreachtech.tinyradius.dictionary.Dictionary;
import com.globalreachtech.tinyradius.packet.AccessRequest;
import com.globalreachtech.tinyradius.packet.RadiusPacket;
import com.globalreachtech.tinyradius.util.RadiusException;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.net.InetSocketAddress;

import static com.globalreachtech.tinyradius.packet.RadiusPacket.ACCESS_ACCEPT;
import static com.globalreachtech.tinyradius.packet.RadiusPacket.ACCESS_REJECT;

public abstract class AuthHandler extends BaseHandler {
    private static Log logger = LogFactory.getLog(RadiusServer.class);

    public AuthHandler(Dictionary dictionary, ServerPacketManager packetManager) {
        super(dictionary, packetManager);
    }

    /**
     * Returns the password of the passed user. Either this
     * method or accessRequestReceived() should be overriden.
     *
     * @param userName user name
     * @return plain-text password or null if user unknown
     */
    public abstract String getUserPassword(String userName);

    /**
     * Constructs an answer for an Access-Request packet. This
     * method should be overridden.
     *
     * @param accessRequest Radius clientRequest packet
     * @param client        address of Radius client
     * @return clientResponse packet or null if no packet shall be sent
     * @throws RadiusException malformed clientRequest packet; if this
     *                         exception is thrown, no answer will be sent
     */
    public RadiusPacket accessRequestReceived(AccessRequest accessRequest, InetSocketAddress client) throws RadiusException {
        String plaintext = getUserPassword(accessRequest.getUserName());
        int type = plaintext != null && accessRequest.verifyPassword(plaintext) ?
                ACCESS_ACCEPT : ACCESS_REJECT;

        RadiusPacket answer = new RadiusPacket(type, accessRequest.getPacketIdentifier());
        answer.setDictionary(dictionary);
        copyProxyState(accessRequest, answer);
        return answer;
    }


    /**
     * Handles the received Radius packet and constructs a clientResponse.
     *
     * @param remoteAddress remote address the packet was sent by
     * @param request       the packet
     * @return clientResponse packet or null for no clientResponse
     * @throws RadiusException
     */
    protected RadiusPacket handlePacket(InetSocketAddress remoteAddress, RadiusPacket request) throws RadiusException {
        // check for duplicates
        if (!packetManager.isClientPacketDuplicate(request, remoteAddress)) {
            if (request instanceof AccessRequest)
                return accessRequestReceived((AccessRequest) request, remoteAddress);
            else
                logger.error("unknown Radius packet type: " + request.getPacketType());
        } else
            logger.info("ignore duplicate packet");

        return null;
    }


    public void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) {
        try {
            InetSocketAddress localAddress = packet.recipient();
            InetSocketAddress remoteAddress = packet.sender();

            String secret = getSharedSecret(remoteAddress);
            if (secret == null) {
                if (logger.isInfoEnabled())
                    logger.info("ignoring packet from unknown client " + remoteAddress + " received on local address " + localAddress);
                return;
            }

            // parse packet
            RadiusPacket request = makeRadiusPacket(packet, secret);
            if (logger.isInfoEnabled())
                logger.info("received packet from " + remoteAddress + " on local address " + localAddress + ": " + request);

            // handle packet
            logger.trace("about to call RadiusServer.handlePacket()");
            RadiusPacket response = handlePacket(remoteAddress, request);
            // send clientResponse
            if (response != null) {
                response.setDictionary(dictionary);
                if (logger.isInfoEnabled())
                    logger.info("send clientResponse: " + response);
                DatagramPacket packetOut = makeDatagramPacket(response, secret, remoteAddress, request);
                ctx.writeAndFlush(packetOut);
            } else {
                logger.info("no clientResponse sent");
            }

        } catch (IOException ioe) {
            // error while reading/writing socket
            logger.error("communication error", ioe);
        } catch (RadiusException re) {
            // malformed packet
            logger.error("malformed Radius packet", re);
        }
    }
}
