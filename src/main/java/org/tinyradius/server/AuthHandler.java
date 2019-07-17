package org.tinyradius.server;

import io.netty.channel.Channel;
import io.netty.util.concurrent.Promise;
import org.tinyradius.packet.AccessRequest;
import org.tinyradius.packet.RadiusPacket;

import java.net.InetSocketAddress;

import static org.tinyradius.packet.PacketType.ACCESS_ACCEPT;
import static org.tinyradius.packet.PacketType.ACCESS_REJECT;

/**
 * Reference implementation of AccessRequest handler that returns Access-Accept/Reject
 * depending on whether {@link #getUserPassword(String)} matches password in Access-Request.
 */
public abstract class AuthHandler implements RequestHandler<AccessRequest> {

    /**
     * Returns the password of the passed user.
     *
     * @param userName user name
     * @return plain-text password or null if user unknown
     */
    public abstract String getUserPassword(String userName);

    @Override
    public Promise<RadiusPacket> handlePacket(Channel channel, AccessRequest packet, InetSocketAddress remoteAddress, String sharedSecret) {
        Promise<RadiusPacket> promise = channel.eventLoop().newPromise();
        try {
            String password = getUserPassword(packet.getUserName());
            int type = password != null && packet.verifyPassword(password) ?
                    ACCESS_ACCEPT : ACCESS_REJECT;

            RadiusPacket answer = new RadiusPacket(packet.getDictionary(), type, packet.getIdentifier());
            packet.getAttributes(33)
                    .forEach(answer::addAttribute);

            promise.trySuccess(answer);
        } catch (Exception e) {
            promise.tryFailure(e);
        }
        return promise;
    }
}
