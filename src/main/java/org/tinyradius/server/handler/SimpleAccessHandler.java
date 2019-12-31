package org.tinyradius.server.handler;

import io.netty.channel.ChannelHandlerContext;
import org.tinyradius.packet.AccessRequest;
import org.tinyradius.packet.RadiusPacket;
import org.tinyradius.packet.RadiusPackets;
import org.tinyradius.server.RequestCtx;

import java.util.function.Function;

import static org.tinyradius.packet.PacketType.ACCESS_ACCEPT;
import static org.tinyradius.packet.PacketType.ACCESS_REJECT;

/**
 * Reference implementation of AccessRequest handler that returns Access-Accept/Reject.
 */
public class SimpleAccessHandler extends RequestHandler {

    private final Function<String, String> passwordLookup;

    /**
     * @param passwordLookup Function returns the password of the passed user.
     */
    public SimpleAccessHandler(Function<String, String> passwordLookup) {
        this.passwordLookup = passwordLookup;
    }

    @Override
    protected Class<AccessRequest> acceptedPacketType() {
        return AccessRequest.class;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RequestCtx msg) {

        final AccessRequest request = (AccessRequest) msg.getRequest();

        String password = passwordLookup.apply(request.getUserName());
        int type = request.verifyPassword(password) ? ACCESS_ACCEPT : ACCESS_REJECT;

        RadiusPacket answer = RadiusPackets.create(request.getDictionary(), type, request.getIdentifier());
        request.getAttributes(33).forEach(answer::addAttribute);

        ctx.writeAndFlush(msg.withResponse(answer));
    }
}
