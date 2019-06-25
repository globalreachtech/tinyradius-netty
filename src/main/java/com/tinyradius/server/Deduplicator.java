package com.tinyradius.server;

import com.tinyradius.packet.RadiusPacket;

import java.net.InetSocketAddress;

public interface Deduplicator {

    /**
     * Checks whether the passed packet is a duplicate.
     * A packet is duplicate if another packet with the same identifier
     * has been sent from the same host in the last time.
     *
     * @param packet  packet in question
     * @param address client address
     * @return true if it is duplicate
     */
    boolean isPacketDuplicate(RadiusPacket packet, InetSocketAddress address);
}
