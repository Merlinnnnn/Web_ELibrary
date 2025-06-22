package com.spkt.libraSys.service.webSocket;

import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class WebSocketUserTracker {
    private final SimpUserRegistry simpUserRegistry;

    public WebSocketUserTracker(SimpUserRegistry simpUserRegistry) {
        this.simpUserRegistry = simpUserRegistry;
    }

    public Set<String> getConnectedUsers() {
        return simpUserRegistry.getUsers().stream()
                .map(SimpUser::getName)
                .collect(Collectors.toSet());
    }
}
