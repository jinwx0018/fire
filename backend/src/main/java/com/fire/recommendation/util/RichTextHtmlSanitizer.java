package com.fire.recommendation.util;

import org.jsoup.Jsoup;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;

/**
 * 富文本 HTML 消毒：基于 OWASP Java HTML Sanitizer，允许常见排版、链接、表格与图片（src 经库内策略约束）。
 * 保存入库前调用，避免 script、事件属性、危险 URL 等。
 */
public final class RichTextHtmlSanitizer {

    private static final PolicyFactory POLICY = Sanitizers.FORMATTING
            .and(Sanitizers.BLOCKS)
            .and(Sanitizers.LINKS)
            .and(Sanitizers.TABLES)
            .and(Sanitizers.IMAGES);

    private RichTextHtmlSanitizer() {
    }

    public static String sanitize(String untrusted) {
        if (untrusted == null) {
            return "";
        }
        return POLICY.sanitize(untrusted);
    }

    /**
     * 先 OWASP 消毒，再用 Jsoup 取纯文本判断是否为空（与入库内容一致，避免正则与嵌套 HTML 边界偏差）。
     */
    public static boolean isEffectivelyBlank(String html) {
        if (html == null || html.isBlank()) {
            return true;
        }
        String safe = sanitize(html);
        String text = Jsoup.parseBodyFragment(safe).text();
        return text == null || text.trim().isEmpty();
    }
}
