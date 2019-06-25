package com.globalreachtech.tinyradius.server;

import com.globalreachtech.tinyradius.dictionary.Dictionary;
import com.globalreachtech.tinyradius.packet.AccessRequest;
import com.globalreachtech.tinyradius.packet.RadiusPacket;
import com.globalreachtech.tinyradius.util.RadiusException;
import com.globalreachtech.tinyradius.util.SecretProvider;
import io.netty.channel.Channel;
import io.netty.util.Timer;
import io.netty.util.concurrent.Promise;

import java.net.InetSocketAddress;

import static com.globalreachtech.tinyradius.packet.RadiusPacket.ACCESS_ACCEPT;
import static com.globalreachtech.tinyradius.packet.RadiusPacket.ACCESS_REJECT;

public abstract class AuthHandler extends ServerHandler<AccessRequest> {

    public AuthHandler(Dictionary dictionary, Deduplicator deduplicator, Timer timer, SecretProvider secretProvider) {
        super(dictionary, deduplicator, timer, secretProvider, AccessRequest.class);
    }

    /**
     * Returns the password of the passed user. Either this
     * method or {@link #handlePacket(Channel, AccessRequest, InetSocketAddress, String)} (EventExecutor, AccessRequest)} should be overridden.
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
    @Override
    protected Promise<RadiusPacket> handlePacket(Channel channel, AccessRequest accessRequest, InetSocketAddress remoteAddress, String sharedSecret) throws RadiusException {
        String password = getUserPassword(accessRequest.getUserName());
        int type = password != null && accessRequest.verifyPassword(password) ?
                ACCESS_ACCEPT : ACCESS_REJECT;

        RadiusPacket answer = new RadiusPacket(type, accessRequest.getPacketIdentifier());
        answer.setDictionary(dictionary);
        accessRequest.getAttributes(33)
                .forEach(answer::addAttribute);

        Promise<RadiusPacket> promise = channel.eventLoop().newPromise();
        promise.trySuccess(answer);
        return promise;
    }
}
