package org.tinyradius.server;

import io.netty.channel.Channel;
import io.netty.util.concurrent.Promise;
import org.tinyradius.packet.AccountingRequest;
import org.tinyradius.packet.RadiusPacket;

import java.net.InetSocketAddress;

import static org.tinyradius.packet.PacketType.ACCOUNTING_RESPONSE;

/**
 * A reference implementation of AccountingRequest handler that responds to all Accounting-Request
 * with standard Accounting-Response.
 */
public class AcctHandler implements RequestHandler<AccountingRequest> {

    @Override
    public Promise<RadiusPacket> handlePacket(Channel channel, AccountingRequest packet, InetSocketAddress remoteAddress, String sharedSecret) {
        RadiusPacket answer = new RadiusPacket(packet.getDictionary(), ACCOUNTING_RESPONSE, packet.getPacketIdentifier());
        packet.getAttributes(33)
                .forEach(answer::addAttribute);

        Promise<RadiusPacket> promise = channel.eventLoop().newPromise();
        promise.trySuccess(answer);
        return promise;
    }
}
