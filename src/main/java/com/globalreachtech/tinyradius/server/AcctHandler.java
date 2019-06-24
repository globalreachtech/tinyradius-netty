package com.globalreachtech.tinyradius.server;

import com.globalreachtech.tinyradius.dictionary.Dictionary;
import com.globalreachtech.tinyradius.packet.AccountingRequest;
import com.globalreachtech.tinyradius.packet.RadiusPacket;
import com.globalreachtech.tinyradius.util.RadiusException;
import com.globalreachtech.tinyradius.util.SecretProvider;
import io.netty.channel.Channel;
import io.netty.util.Timer;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Promise;

import java.net.InetSocketAddress;

import static com.globalreachtech.tinyradius.packet.RadiusPacket.ACCOUNTING_RESPONSE;

public class AcctHandler extends ServerHandler {

    public AcctHandler(Dictionary dictionary, Deduplicator deduplicator, Timer timer, SecretProvider secretProvider) {
        super(dictionary, deduplicator, timer, secretProvider);
    }

    /**
     * Constructs an answer for an Accounting-Request packet. This method
     * should be overridden.
     *
     * @param accountingRequest Radius clientRequest packet
     * @return clientResponse packet or null if no packet shall be sent
     */
    public Promise<RadiusPacket> accountingRequestReceived(EventExecutor eventExecutor, AccountingRequest accountingRequest) {
        RadiusPacket answer = new RadiusPacket(ACCOUNTING_RESPONSE, accountingRequest.getPacketIdentifier());
        accountingRequest.getAttributes(33)
                .forEach(answer::addAttribute);

        Promise<RadiusPacket> promise = eventExecutor.newPromise();
        promise.trySuccess(answer);
        return promise;
    }

    /**
     * Handles the received Radius packet and constructs a clientResponse.
     *
     * @param channel
     * @param request       the packet
     * @param remoteAddress remote address the packet was sent by
     * @param sharedSecret
     * @return clientResponse packet or null for no clientResponse
     */
    @Override
    protected Promise<RadiusPacket> handlePacket(Channel channel, RadiusPacket request, InetSocketAddress remoteAddress, String sharedSecret) {
        if (request instanceof AccountingRequest)
            return accountingRequestReceived(channel.eventLoop(), (AccountingRequest) request);

        Promise<RadiusPacket> promise = channel.eventLoop().newPromise();
        promise.tryFailure(new RadiusException("unknown Radius packet type: " + request.getPacketType()));
        return promise;

    }
}
