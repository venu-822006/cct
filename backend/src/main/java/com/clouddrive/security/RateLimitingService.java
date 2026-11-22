package com.clouddrive.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Service
public class RateLimitingService {

    @Value("${app.security.rate-limit.enabled:true}")
    private boolean enabled = true;

    @Value("${app.security.rate-limit.login-limit:10}")
    private int loginLimit = 10;

    @Value("${app.security.rate-limit.register-limit:5}")
    private int registerLimit = 5;

    @Value("${app.security.rate-limit.window-seconds:60}")
    private int windowSeconds = 60;

    private final ConcurrentHashMap<String, ConcurrentLinkedDeque<Long>> loginAttempts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ConcurrentLinkedDeque<Long>> registerAttempts = new ConcurrentHashMap<>();

    public boolean allowLogin(String clientIp) {
        if (!enabled) return true;
        return checkLimit(loginAttempts, clientIp, loginLimit);
    }

    public boolean allowRegister(String clientIp) {
        if (!enabled) return true;
        return checkLimit(registerAttempts, clientIp, registerLimit);
    }

    private boolean checkLimit(ConcurrentHashMap<String, ConcurrentLinkedDeque<Long>> map, String clientIp, int maxAllowed) {
        if (clientIp == null || clientIp.isBlank()) {
            clientIp = "unknown";
        }

        long now = System.currentTimeMillis();
        long windowStart = now - (windowSeconds * 1000L);

        ConcurrentLinkedDeque<Long> timestamps = map.computeIfAbsent(clientIp, k -> new ConcurrentLinkedDeque<>());

        // Evict timestamps older than the sliding window
        while (!timestamps.isEmpty() && timestamps.peekFirst() < windowStart) {
            timestamps.pollFirst();
        }

        if (timestamps.size() >= maxAllowed) {
            return false;
        }

        timestamps.addLast(now);
        return true;
    }

    public void reset(String clientIp) {
        loginAttempts.remove(clientIp);
        registerAttempts.remove(clientIp);
    }

    public void resetAll() {
        loginAttempts.clear();
        registerAttempts.clear();
    }
}
