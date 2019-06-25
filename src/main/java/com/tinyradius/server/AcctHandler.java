package com.tinyradius.server;

import com.tinyradius.dictionary.Dictionary;
import com.tinyradius.packet.AccountingRequest;
import com.tinyradius.packet.RadiusPacket;
import com.tinyradius.util.SecretProvider;
import io.netty.channel.Channel;
import io.netty.util.Timer;
import io.netty.util.concurrent.Promise;

import java.net.InetSocketAddress;

import static com.tinyradius.packet.RadiusPacket.ACCOUNTING_RESPONSE;

/**
 * A reference implementation of AccountingRequest handler that responds to all Accounting-Request
 * with standard Accounting-Response.
 */
public class AcctHandler extends ServerHandler<AccountingRequest> {

    public AcctHandler(Dictionary dictionary, Deduplicator deduplicator, Timer timer, SecretProvider secretProvider) {
        super(dictionary, deduplicator, timer, secretProvider, AccountingRequest.class);
    }

    /**
     * Constructs an answer for an Accounting-Request packet. This method
     * should be overridden.
     *
     * @param accountingRequest Radius clientRequest packet
     * @return clientResponse packet or null if no packet shall be sent
     */
    @Override
    protected Promise<RadiusPacket> handlePacket(Channel channel, AccountingRequest accountingRequest, InetSocketAddress remoteAddress, String sharedSecret) {
        RadiusPacket answer = new RadiusPacket(ACCOUNTING_RESPONSE, accountingRequest.getPacketIdentifier());
        accountingRequest.getAttributes(33)
                .forEach(answer::addAttribute);

        Promise<RadiusPacket> promise = channel.eventLoop().newPromise();
        promise.trySuccess(answer);
        return promise;
    }
}
