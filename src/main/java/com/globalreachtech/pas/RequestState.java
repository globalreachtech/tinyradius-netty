package com.globalreachtech.pas;

import java.util.*;

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
        transitions.put(RESOLVED, new HashSet<>(Collections.singletonList(CACHED)));
        transitions.put(CACHED, new HashSet<>(Collections.singletonList(EXPIRED)));
    }
}
