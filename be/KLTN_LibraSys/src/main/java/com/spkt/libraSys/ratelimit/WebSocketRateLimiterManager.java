package com.spkt.libraSys.ratelimit;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class WebSocketRateLimiterManager {

    private final RateLimiterRegistry rateLimiterRegistry;

    // Để tránh tạo lại nhiều lần nếu đã có
    private final ConcurrentHashMap<String, RateLimiter> limiterCache = new ConcurrentHashMap<>();

    public WebSocketRateLimiterManager(RateLimiterRegistry rateLimiterRegistry) {
        this.rateLimiterRegistry = rateLimiterRegistry;
    }

    /**
     * Tạo RateLimiter theo IP
     */
    public RateLimiter getLimiterByIp(String ip) {
        String key = "wsSocket-ip-" + ip;
        return getOrCreateLimiter(key);
    }

    /**
     * Tạo RateLimiter theo userId
     */
    public RateLimiter getLimiterByUserId(Long userId) {
        String key = "wsSocket-user-" + userId;
        return getOrCreateLimiter(key);
    }

    /**
     * Kiểm tra xem có còn quota không (trả về true nếu còn)
     */
    public boolean tryAcquire(RateLimiter limiter, String label) {
        boolean granted = limiter.acquirePermission();
        if (!granted) {
            log.warn("⛔ Rate limit exceeded for {}", label);
        }
        return granted;
    }

    /**
     * Tạo hoặc lấy RateLimiter từ cache
     */
    private RateLimiter getOrCreateLimiter(String key) {
        System.out.println("key" + key);
        return limiterCache.computeIfAbsent(key, k -> {
            log.debug("🔧 Creating new RateLimiter for key: {}", k);
            return rateLimiterRegistry.rateLimiter(k); // kế thừa config wsSocket nếu không có riêng
        });
    }
}
