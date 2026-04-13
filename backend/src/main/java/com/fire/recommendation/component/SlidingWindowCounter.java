package com.fire.recommendation.component;

import com.fire.recommendation.exception.BusinessException;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 按 key 的固定时间窗口计数，超过上限抛出业务异常（单机内存，集群需 Redis 等）。
 */
public final class SlidingWindowCounter {

    private final long windowMs;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object> locks = new ConcurrentHashMap<>();

    private static final class Window {
        long startMs;
        int count;
    }

    public SlidingWindowCounter(long windowMs) {
        this.windowMs = windowMs;
    }

    public void incrementAndCheck(String key, int maxAllowed) {
        if (key == null || maxAllowed <= 0) {
            return;
        }
        Object lock = locks.computeIfAbsent(key, k -> new Object());
        synchronized (lock) {
            long now = System.currentTimeMillis();
            Window w = windows.computeIfAbsent(key, k -> {
                Window x = new Window();
                x.startMs = now;
                x.count = 0;
                return x;
            });
            if (now - w.startMs > windowMs) {
                w.startMs = now;
                w.count = 0;
            }
            w.count++;
            if (w.count > maxAllowed) {
                throw new BusinessException("请求过于频繁，请稍后再试");
            }
        }
    }
}
