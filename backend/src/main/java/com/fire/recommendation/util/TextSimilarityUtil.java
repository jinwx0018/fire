package com.fire.recommendation.util;

import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * 轻量文本相似度（字二元组 Dice），用于无向量服务时的「语义近似」加分。
 */
public final class TextSimilarityUtil {

    private TextSimilarityUtil() {
    }

    /**
     * Dice 系数：2*|A∩B| / (|A|+|B|)，基于字符 bigram；适合中文短文本。
     */
    public static double diceBigram(String a, String b) {
        if (!StringUtils.hasText(a) || !StringUtils.hasText(b)) {
            return 0.0;
        }
        String na = normalize(a);
        String nb = normalize(b);
        if (na.length() < 2 || nb.length() < 2) {
            return na.equals(nb) ? 1.0 : 0.0;
        }
        Set<String> ba = bigrams(na);
        Set<String> bb = bigrams(nb);
        if (ba.isEmpty() || bb.isEmpty()) {
            return 0.0;
        }
        int inter = 0;
        for (String x : ba) {
            if (bb.contains(x)) {
                inter++;
            }
        }
        return 2.0 * inter / (ba.size() + bb.size());
    }

    private static String normalize(String s) {
        String t = s.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
        return t.length() > 2000 ? t.substring(0, 2000) : t;
    }

    private static Set<String> bigrams(String s) {
        Set<String> out = new HashSet<>();
        for (int i = 0; i < s.length() - 1; i++) {
            out.add(s.substring(i, i + 2));
        }
        return out;
    }
}
