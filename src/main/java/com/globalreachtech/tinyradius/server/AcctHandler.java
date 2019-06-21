package com.globalreachtech.tinyradius.server;

import com.globalreachtech.tinyradius.dictionary.Dictionary;
import com.globalreachtech.tinyradius.packet.AccountingRequest;
import com.globalreachtech.tinyradius.packet.RadiusPacket;
import com.globalreachtech.tinyradius.util.RadiusException;
import io.netty.channel.Channel;
import io.netty.util.Timer;
import io.netty.util.concurrent.Promise;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.InetSocketAddress;

import static com.globalreachtech.tinyradius.packet.RadiusPacket.ACCOUNTING_RESPONSE;

public abstract class AcctHandler extends BaseHandler {

    private static Log logger = LogFactory.getLog(AcctHandler.class);

    public AcctHandler(Dictionary dictionary, RadiusServer.Deduplicator packetManager, Timer timer) {
        super(dictionary, packetManager, timer);
    }

    /**
     * Constructs an answer for an Accounting-Request packet. This method
     * should be overridden.
     *
     * @param accountingRequest Radius clientRequest packet
     * @return clientResponse packet or null if no packet shall be sent
     * @throws RadiusException malformed clientRequest packet; if this
     *                         exception is thrown, no answer will be sent
     */
    public RadiusPacket accountingRequestReceived(AccountingRequest accountingRequest) {
        RadiusPacket answer = new RadiusPacket(ACCOUNTING_RESPONSE, accountingRequest.getPacketIdentifier());
        copyProxyState(accountingRequest, answer);
        return answer;
    }

    /**
     * Handles the received Radius packet and constructs a clientResponse.
     *
     * @param channel
     * @param remoteAddress remote address the packet was sent by
     * @param request       the packet
     * @return clientResponse packet or null for no clientResponse
     */
    @Override
    protected Promise<RadiusPacket> handlePacket(Channel channel, InetSocketAddress remoteAddress, RadiusPacket request) {
        Promise<RadiusPacket> promise = channel.eventLoop().newPromise();
        if (request instanceof AccountingRequest)
            promise.trySuccess(accountingRequestReceived((AccountingRequest) request));
        else
            logger.error("unknown Radius packet type: " + request.getPacketType());
        return promise;
    }
}
