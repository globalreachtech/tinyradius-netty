package com.globalreachtech.tinyradius.grt;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Set;

public enum RequestState {

    QUEUED,
    AWAITING_SERVER_RESPONSE,
    RESOLVED,
    CACHED,
    EXPIRED;

    public static final EnumMap<RequestState, Set<RequestState>> transitions = new EnumMap<>(RequestState.class);

    static {
        transitions.put(QUEUED, new HashSet<>(Arrays.asList(AWAITING_SERVER_RESPONSE, RESOLVED)));
        transitions.put(AWAITING_SERVER_RESPONSE, new HashSet<>(Arrays.asList(QUEUED, RESOLVED)));
        transitions.put(RESOLVED, new HashSet<>(Arrays.asList(CACHED)));
        transitions.put(CACHED, new HashSet<>(Arrays.asList(EXPIRED)));
    }
}
