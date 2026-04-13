package com.fire.recommendation.util;

import org.owasp.html.AttributePolicy;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 标题、摘要等字段：去掉 HTML 标签与危险片段，仅保留纯文本，防止将来 v-html 误用时存储型 XSS。
 */
public final class PlainTextSanitizer {

    private static final PolicyFactory STRIP_ALL = new HtmlPolicyBuilder().toFactory();

    /**
     * 知识/新闻等富文本评论：保留换行结构（含 contenteditable 常见的 div）、受控 span（仅包裹插入图）、
     * 以及 img（仅 http/https src）。论坛评论入库未走本策略，但展示端同样安全。
     */
    private static final AttributePolicy CLASS_COMMENT_IMG_LINE_ONLY = (elementName, attributeName, value) -> {
        if (value == null) {
            return null;
        }
        String t = value.trim();
        return "comment-img-line".equals(t) ? t : null;
    };

    private static final PolicyFactory RICH_COMMENT = new HtmlPolicyBuilder()
            .allowElements("img", "br", "p", "div", "span")
            .allowUrlProtocols("http", "https")
            .allowAttributes("src", "alt").onElements("img")
            .allowAttributes("class").matching(CLASS_COMMENT_IMG_LINE_ONLY).onElements("span")
            .allowWithoutAttributes("br", "p", "div")
            .toFactory();

    private static final int RICH_COMMENT_MAX_LEN = 12000;

    /** OWASP sanitize 会把补充平面字符（如 emoji）写成 &#x1f604;，需还原为真实 Unicode 供纯文本展示 */
    private static final Pattern ENTITY_HEX = Pattern.compile("&#x([0-9a-fA-F]+);");
    private static final Pattern ENTITY_DEC = Pattern.compile("&#([0-9]{1,7});");

    private PlainTextSanitizer() {
    }

    /** 单行标题，最大 200 字符 */
    public static String sanitizeTitle(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "";
        }
        String t = STRIP_ALL.sanitize(raw).replace('\r', ' ').replace('\n', ' ').replaceAll("\\s+", " ").trim();
        // 标题里若包含 emoji/特殊符号，OWASP 可能转成 &#x...;，这里还原为可读字符。
        t = decodeHtmlCharacterReferences(t);
        return truncate(t, 200);
    }

    /** 摘要，可含换行，最大 500 字符 */
    public static String sanitizeSummary(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String t = STRIP_ALL.sanitize(raw).trim();
        t = decodeHtmlCharacterReferences(t);
        t = truncate(t, 500);
        return t.isEmpty() ? null : t;
    }

    /** API 输出时再次收敛（兼容历史脏数据） */
    public static String sanitizeTitleOutput(String stored) {
        return sanitizeTitle(stored == null ? "" : stored);
    }

    public static String sanitizeSummaryOutput(String stored) {
        if (!StringUtils.hasText(stored)) {
            return null;
        }
        return sanitizeSummary(stored);
    }

    /** 新闻分类等短标签，最大 64 字符 */
    public static String sanitizeCategory(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String t = STRIP_ALL.sanitize(raw).replace('\r', ' ').replace('\n', ' ').replaceAll("\\s+", " ").trim();
        t = truncate(t, 64);
        return t.isEmpty() ? null : t;
    }

    public static String sanitizeCategoryOutput(String stored) {
        return sanitizeCategory(stored == null ? "" : stored);
    }

    /** 评论正文，纯文本，最大 2000 字 */
    public static String sanitizeCommentBody(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "";
        }
        String t = STRIP_ALL.sanitize(raw).trim();
        t = decodeHtmlCharacterReferences(t);
        return truncate(t, 2000);
    }

    /**
     * 知识评论等富文本存储：消毒后保留 img/br/p 与文本；最大 12000 字符。
     */
    public static String sanitizeRichCommentBody(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "";
        }
        String t = RICH_COMMENT.sanitize(raw).trim();
        t = decodeHtmlCharacterReferences(t);
        return truncate(t, RICH_COMMENT_MAX_LEN);
    }

    /**
     * 富文本评论是否视为空：无可见文字且无 {@code <img} 则为空（仅图片也算有内容）。
     */
    public static boolean isBlankRichComment(String sanitized) {
        if (!StringUtils.hasText(sanitized)) {
            return true;
        }
        if (Pattern.compile("<\\s*img\\b", Pattern.CASE_INSENSITIVE).matcher(sanitized).find()) {
            return false;
        }
        String plain = STRIP_ALL.sanitize(sanitized).trim();
        plain = decodeHtmlCharacterReferences(plain).replaceAll("\\s+", " ").trim();
        return !StringUtils.hasText(plain);
    }

    /** 站内通知等：去掉标签后的纯文本摘要；仅有图无字时返回「[图片]」。 */
    public static String plainPreviewForNotify(String raw, int maxLen) {
        if (!StringUtils.hasText(raw)) {
            return "";
        }
        boolean hasImg = Pattern.compile("<\\s*img\\b", Pattern.CASE_INSENSITIVE).matcher(raw).find();
        String plain = STRIP_ALL.sanitize(raw).trim();
        plain = decodeHtmlCharacterReferences(plain);
        plain = plain.replaceAll("\\s+", " ").trim();
        if (StringUtils.hasText(plain)) {
            return truncate(plain, Math.max(1, maxLen));
        }
        if (hasImg) {
            return "[图片]";
        }
        return "";
    }

    /**
     * 评论内容输出给前端（或写入通知文案）：还原 HTML 数字实体与常见命名实体，兼容历史库中已存的 &#x1f604; 形式。
     */
    public static String formatCommentForApi(String stored) {
        if (stored == null) {
            return null;
        }
        if (!StringUtils.hasText(stored)) {
            return "";
        }
        return decodeHtmlCharacterReferences(stored);
    }

    static String decodeHtmlCharacterReferences(String html) {
        if (html == null || html.isEmpty()) {
            return html;
        }
        String s = replaceNumericEntities(html, ENTITY_HEX, 16);
        s = replaceNumericEntities(s, ENTITY_DEC, 10);
        s = s.replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&apos;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&nbsp;", "\u00a0");
        s = s.replace("&amp;", "&");
        return s;
    }

    private static String replaceNumericEntities(String input, Pattern p, int radix) {
        Matcher m = p.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            try {
                int cp = Integer.parseInt(m.group(1), radix);
                if (Character.isValidCodePoint(cp)) {
                    String ch = new String(Character.toChars(cp));
                    m.appendReplacement(sb, Matcher.quoteReplacement(ch));
                } else {
                    m.appendReplacement(sb, Matcher.quoteReplacement(m.group(0)));
                }
            } catch (NumberFormatException e) {
                m.appendReplacement(sb, Matcher.quoteReplacement(m.group(0)));
            }
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String truncate(String t, int max) {
        if (t.length() <= max) {
            return t;
        }
        return t.substring(0, max);
    }
}
