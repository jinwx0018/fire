package com.fire.recommendation.component;

import com.fire.recommendation.exception.BusinessException;
import com.fire.recommendation.util.HttpClientIp;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户端新闻：列表/详情/RSS 按 IP 限流；发表评论按 IP + 登录用户限流（单机内存窗口；集群需 Redis 等）。
 * 配置为 0 时关闭对应限流。
 */
@Component
public class NewsReadRateLimiter {

    private static final long WINDOW_MS = 60_000;

    @Value("${app.rate-limit.news-list-per-ip-per-minute:0}")
    private int listMaxPerMinute;

    @Value("${app.rate-limit.news-detail-per-ip-per-minute:0}")
    private int detailMaxPerMinute;

    @Value("${app.rate-limit.news-rss-per-ip-per-minute:0}")
    private int rssMaxPerMinute;

    @Value("${app.rate-limit.news-comment-post-per-ip-per-minute:0}")
    private int commentPostIpMaxPerMinute;

    @Value("${app.rate-limit.news-comment-post-per-user-per-minute:0}")
    private int commentPostUserMaxPerMinute;

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object> locks = new ConcurrentHashMap<>();

    private static final class Window {
        long startMs;
        int count;
    }

    public void beforeList(HttpServletRequest request) {
        check(request, "L", listMaxPerMinute, "新闻列表访问过于频繁，请稍后再试");
    }

    public void beforeDetail(HttpServletRequest request) {
        check(request, "D", detailMaxPerMinute, "新闻详情访问过于频繁，请稍后再试");
    }

    public void beforeRss(HttpServletRequest request) {
        check(request, "R", rssMaxPerMinute, "RSS 访问过于频繁，请稍后再试");
    }

    /** 发表评论：按 IP + 可选按登录用户限流 */
    public void beforeNewsCommentPost(HttpServletRequest request, Long userId) {
        check(request, "NCIP", commentPostIpMaxPerMinute, "发表评论过于频繁，请稍后再试");
        if (userId != null && commentPostUserMaxPerMinute > 0) {
            checkByKey("NCU:" + userId, commentPostUserMaxPerMinute, "发表评论过于频繁，请稍后再试");
        }
    }

    private void checkByKey(String key, int maxPerWindow, String message) {
        if (maxPerWindow <= 0) {
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

    private void check(HttpServletRequest request, String prefix, int maxPerWindow, String message) {
        if (maxPerWindow <= 0) {
            return;
        }
        String ip = HttpClientIp.resolve(request);
        if (!org.springframework.util.StringUtils.hasText(ip)) {
            ip = "unknown";
        }
        String key = prefix + ":" + ip;
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
