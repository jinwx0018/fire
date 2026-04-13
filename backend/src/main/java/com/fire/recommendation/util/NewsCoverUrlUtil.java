package com.fire.recommendation.util;

import org.springframework.util.StringUtils;

import java.net.URI;

/**
 * 新闻封面地址：仅允许 http(s) 或本站相对路径（禁止 ..、javascript 等）。
 */
public final class NewsCoverUrlUtil {

    private NewsCoverUrlUtil() {
    }

    public static String sanitize(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String u = raw.trim();
        if (u.length() > 512) {
            return null;
        }
        String lower = u.toLowerCase();
        if (lower.startsWith("javascript:") || lower.startsWith("data:")) {
            return null;
        }
        if (u.startsWith("/") && !u.startsWith("//")) {
            return u.contains("..") ? null : u;
        }
        try {
            URI uri = URI.create(u);
            String scheme = uri.getScheme();
            if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
                return u;
            }
        } catch (Exception ignored) {
            /* invalid */
        }
        return null;
    }
}
