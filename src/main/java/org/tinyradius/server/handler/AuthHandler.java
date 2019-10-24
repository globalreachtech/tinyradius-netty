package org.tinyradius.server.handler;

import io.netty.channel.Channel;
import io.netty.util.concurrent.Promise;
import org.tinyradius.packet.AccessRequest;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.server.SecretProvider;

import java.net.InetSocketAddress;

import static org.tinyradius.packet.PacketType.ACCESS_ACCEPT;
import static org.tinyradius.packet.PacketType.ACCESS_REJECT;

/**
 * Reference implementation of AccessRequest handler that returns Access-Accept/Reject
 * depending on whether {@link #getUserPassword(String)} matches password in Access-Request.
 */
public abstract class AuthHandler implements RequestHandler<AccessRequest, SecretProvider> {

    /**
     * Returns the password of the passed user.
     *
     * @param userName user name
     * @return plain-text password or null if user unknown
     */
    public abstract String getUserPassword(String userName);

    @Override
    public Promise<RadiusPacket> handlePacket(Channel channel, AccessRequest request, InetSocketAddress remoteAddress, SecretProvider secretProvider) {
        Promise<RadiusPacket> promise = channel.eventLoop().newPromise();
        try {
            String password = getUserPassword(request.getUserName());
            int type = password != null && request.verifyPassword(password) ?
                    ACCESS_ACCEPT : ACCESS_REJECT;

            RadiusPacket answer = new RadiusPacket(request.getDictionary(), type, request.getIdentifier());
            request.getAttributes(33)
                    .forEach(answer::addAttribute);

            promise.trySuccess(answer);
        } catch (Exception e) {
            promise.tryFailure(e);
        }
        return promise;
    }
}
