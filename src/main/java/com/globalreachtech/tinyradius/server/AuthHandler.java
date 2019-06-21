package com.globalreachtech.tinyradius.server;

import com.globalreachtech.tinyradius.dictionary.Dictionary;
import com.globalreachtech.tinyradius.packet.AccessRequest;
import com.globalreachtech.tinyradius.packet.RadiusPacket;
import com.globalreachtech.tinyradius.util.RadiusException;
import io.netty.channel.Channel;
import io.netty.util.Timer;
import io.netty.util.concurrent.Promise;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.InetSocketAddress;

import static com.globalreachtech.tinyradius.packet.RadiusPacket.ACCESS_ACCEPT;
import static com.globalreachtech.tinyradius.packet.RadiusPacket.ACCESS_REJECT;

public abstract class AuthHandler extends BaseHandler {
    private static Log logger = LogFactory.getLog(AuthHandler.class);

    public AuthHandler(Dictionary dictionary, RadiusServer.Deduplicator packetManager, Timer timer) {
        super(dictionary, packetManager, timer);
    }

    /**
     * Returns the password of the passed user. Either this
     * method or accessRequestReceived() should be overridden.
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
     * @return clientResponse packet or null if no packet shall be sent
     * @throws RadiusException malformed clientRequest packet; if this
     *                         exception is thrown, no answer will be sent
     */
    public RadiusPacket accessRequestReceived(AccessRequest accessRequest) throws RadiusException {
        String plaintext = getUserPassword(accessRequest.getUserName());
        int type = plaintext != null && accessRequest.verifyPassword(plaintext) ?
                ACCESS_ACCEPT : ACCESS_REJECT;

        RadiusPacket answer = new RadiusPacket(type, accessRequest.getPacketIdentifier());
        answer.setDictionary(dictionary);
        copyProxyState(accessRequest, answer);
        return answer;
    }

    @Override
    protected Promise<RadiusPacket> handlePacket(Channel channel, InetSocketAddress remoteAddress, RadiusPacket request) throws RadiusException {
        Promise<RadiusPacket> promise = channel.eventLoop().newPromise();
        if (request instanceof AccessRequest)
            promise.trySuccess(accessRequestReceived((AccessRequest) request));
        else
            logger.error("unknown Radius packet type: " + request.getPacketType());
        return promise;
    }
}
