package com.mooar.market.common;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Component
public class SessionStore {

    private final Cache<String, Long> sessions = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofHours(24))
            .maximumSize(100_000)
            .build();

    public String create(Long userId) {
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        sessions.put(sessionId, userId);
        return sessionId;
    }

    public Long getUserId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return null;
        }
        return sessions.getIfPresent(sessionId);
    }

    public void remove(String sessionId) {
        if (sessionId != null && !sessionId.isBlank()) {
            sessions.invalidate(sessionId);
        }
    }
}
