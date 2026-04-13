package com.fire.recommendation.util;

import org.jsoup.Jsoup;
import org.springframework.util.StringUtils;

/**
 * 从已消毒 HTML 提取纯文本生成摘要（用于未填摘要时自动填充）。
 */
public final class NewsAutoSummaryUtil {

    public static final int SUMMARY_MAX = 500;

    private NewsAutoSummaryUtil() {
    }

    public static String fromSanitizedHtml(String safeHtml) {
        if (!StringUtils.hasText(safeHtml)) {
            return null;
        }
        String t = Jsoup.parseBodyFragment(safeHtml).text();
        if (!StringUtils.hasText(t)) {
            return null;
        }
        t = t.replaceAll("\\s+", " ").trim();
        if (t.length() <= SUMMARY_MAX) {
            return t;
        }
        return t.substring(0, SUMMARY_MAX - 1) + "…";
    }
}
