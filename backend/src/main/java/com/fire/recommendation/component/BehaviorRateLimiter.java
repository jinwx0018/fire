package com.fire.recommendation.component;

import com.fire.recommendation.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 行为上报接口按用户限流，减轻刷库风险（单机内存窗口，集群需换 Redis 等）。
 */
@Component
public class BehaviorRateLimiter {

    private static final long WINDOW_MS = 60_000;

    @Value("${app.rate-limit.behavior-per-minute:120}")
    private int maxPerWindow;

    private final ConcurrentHashMap<Long, Window> windows = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Object> locks = new ConcurrentHashMap<>();

    private static final class Window {
        long startMs;
        int count;
    }

    public void check(Long userId) {
        if (userId == null) {
            return;
        }
        Object lock = locks.computeIfAbsent(userId, k -> new Object());
        synchronized (lock) {
            long now = System.currentTimeMillis();
            Window w = windows.computeIfAbsent(userId, k -> {
                Window x = new Window();
                x.startMs = now;
                x.count = 0;
                return x;
            });
            if (now - w.startMs > WINDOW_MS) {
                w.startMs = now;
                w.count = 0;
            }
            w.count++;
            if (w.count > maxPerWindow) {
                throw new BusinessException("操作过于频繁，请稍后再试");
            }
        }
    }
}
