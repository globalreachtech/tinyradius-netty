package org.tinyradius.server.handler;

import io.netty.channel.Channel;
import io.netty.util.concurrent.Promise;
import org.tinyradius.packet.AccountingRequest;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.server.SecretProvider;

import java.net.InetSocketAddress;

import static org.tinyradius.packet.PacketType.ACCOUNTING_RESPONSE;

/**
 * A reference implementation of AccountingRequest handler that responds to all Accounting-Request
 * with standard Accounting-Response.
 */
public class AcctHandler implements RequestHandler<AccountingRequest, SecretProvider> {

    @Override
    public Promise<RadiusPacket> handlePacket(Channel channel, AccountingRequest request, InetSocketAddress remoteAddress, SecretProvider secretProvider) {
        RadiusPacket answer = new RadiusPacket(request.getDictionary(), ACCOUNTING_RESPONSE, request.getIdentifier());
        request.getAttributes(33)
                .forEach(answer::addAttribute);

        Promise<RadiusPacket> promise = channel.eventLoop().newPromise();
        promise.trySuccess(answer);
        return promise;
    }
}
