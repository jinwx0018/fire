package com.fire.recommendation.component;

import com.fire.recommendation.exception.BusinessException;
import com.fire.recommendation.util.HttpClientIp;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 启用 AI 推荐时，对 GET /recommend/list 限流（可能触发两次外部调用），减轻成本与滥用（单机内存窗口）。
 */
@Component
public class RecommendAiListRateLimiter {

    private static final long WINDOW_MS = 60_000;

    @Value("${app.rate-limit.recommend-list-ai-per-ip-per-minute:30}")
    private int maxPerIpPerMinute;

    @Value("${app.rate-limit.recommend-list-ai-per-user-per-minute:60}")
    private int maxPerUserPerMinute;

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object> locks = new ConcurrentHashMap<>();

    private static final class Window {
        long startMs;
        int count;
    }

    public void beforeList(HttpServletRequest request, Long userId) {
        if (maxPerIpPerMinute > 0) {
            checkByKey(ipKey(request), maxPerIpPerMinute, "推荐列表访问过于频繁，请稍后再试");
        }
        if (userId != null && maxPerUserPerMinute > 0) {
            checkByKey("RLAI:U:" + userId, maxPerUserPerMinute, "推荐列表访问过于频繁，请稍后再试");
        }
    }

    private static String ipKey(HttpServletRequest request) {
        String ip = HttpClientIp.resolve(request);
        if (!StringUtils.hasText(ip)) {
            ip = "unknown";
        }
        return "RLAI:IP:" + ip;
    }

    private void checkByKey(String key, int maxPerWindow, String message) {
        Object lock = locks.computeIfAbsent(key, k -> new Object());
        synchronized (lock) {
            long now = System.currentTimeMillis();
            Window w = windows.computeIfAbsent(key, k -> {
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
                throw new BusinessException(message);
            }
        }
    }
}
