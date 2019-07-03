package org.tinyradius.server;

import io.netty.channel.Channel;
import io.netty.util.concurrent.Promise;
import org.tinyradius.dictionary.Dictionary;
import org.tinyradius.packet.AccountingRequest;
import org.tinyradius.packet.RadiusPacket;

import java.net.InetSocketAddress;

import static org.tinyradius.packet.PacketType.ACCOUNTING_RESPONSE;

/**
 * A reference implementation of AccountingRequest handler that responds to all Accounting-Request
 * with standard Accounting-Response.
 */
public class AcctHandler implements RequestHandler<AccountingRequest> {

    /**
     * Constructs an answer for an Accounting-Request packet. This method
     * should be overridden.
     *
     * @param dictionary
     * @param accountingRequest Radius request packet
     * @return response packet or null if no packet shall be sent
     */
    @Override
    public Promise<RadiusPacket> handlePacket(Dictionary dictionary, Channel channel, AccountingRequest accountingRequest, InetSocketAddress remoteAddress, String sharedSecret) {
        RadiusPacket answer = new RadiusPacket(dictionary, ACCOUNTING_RESPONSE, accountingRequest.getPacketIdentifier());
        accountingRequest.getAttributes(33)
                .forEach(answer::addAttribute);

        Promise<RadiusPacket> promise = channel.eventLoop().newPromise();
        promise.trySuccess(answer);
        return promise;
    }
}
