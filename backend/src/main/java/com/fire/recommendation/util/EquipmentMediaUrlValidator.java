package com.fire.recommendation.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fire.recommendation.exception.BusinessException;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * 器材封面与多图 URL 校验：仅允许公网 http(s)，降低 javascript:/data: 与内网 SSRF 风险。
 */
public final class EquipmentMediaUrlValidator {

    private static final int MAX_URL_LENGTH = 2048;
    private static final ObjectMapper OM = new ObjectMapper();

    private EquipmentMediaUrlValidator() {
    }

    public static void validateCoverAndImages(String cover, String images) {
        validateOptionalUrl("封面 URL", cover);
        validateImagesField(images);
    }

    public static void validateOptionalUrl(String label, String url) {
        if (!StringUtils.hasText(url)) {
            return;
        }
        String u = url.trim();
        if (isSafeRelativePath(u)) {
            return;
        }
        assertSafePublicHttpUrl(label, u);
    }

    /** 允许本站相对路径（如 /uploads/xxx），禁止 .. 与协议相对 // */
    private static boolean isSafeRelativePath(String u) {
        return u.startsWith("/") && !u.startsWith("//") && u.length() <= 512 && !u.contains("..");
    }

    public static void validateImagesField(String images) {
        if (!StringUtils.hasText(images)) {
            return;
        }
        List<String> urls = splitImageUrls(images.trim());
        for (int i = 0; i < urls.size(); i++) {
            validateOptionalUrl("多图 URL 第 " + (i + 1) + " 个", urls.get(i));
        }
    }

    private static List<String> splitImageUrls(String s) {
        List<String> out = new ArrayList<>();
        if (s.startsWith("[") && s.endsWith("]")) {
            try {
                List<String> arr = OM.readValue(s, new TypeReference<List<String>>() {
                });
                if (arr != null) {
                    for (String item : arr) {
                        if (StringUtils.hasText(item)) {
                            out.add(item.trim());
                        }
                    }
                }
                return out;
            } catch (Exception ignored) {
                /* 按分隔符解析 */
            }
        }
        for (String part : s.split("[,，\\n\\r;；]")) {
            String t = part.trim();
            if (StringUtils.hasText(t)) {
                out.add(t);
            }
        }
        return out;
    }

    private static void assertSafePublicHttpUrl(String label, String url) {
        if (url.length() > MAX_URL_LENGTH) {
            throw new BusinessException(label + " 过长（最多 " + MAX_URL_LENGTH + " 字符）");
        }
        final URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(label + " 格式无效");
        }
        String scheme = uri.getScheme();
        if (scheme == null || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
            throw new BusinessException(label + " 仅支持 http 或 https");
        }
        String host = uri.getHost();
        if (host == null || host.isEmpty()) {
            throw new BusinessException(label + " 缺少合法主机名");
        }
        String h = host.toLowerCase();
        if ("localhost".equals(h) || h.endsWith(".localhost")) {
            throw new BusinessException(label + " 不允许指向本机或内网地址");
        }
        if (h.startsWith("127.") || "0.0.0.0".equals(h) || "::1".equals(h) || "[::1]".equalsIgnoreCase(h)) {
            throw new BusinessException(label + " 不允许指向本机或内网地址");
        }
        if (h.startsWith("192.168.") || h.startsWith("10.") || h.startsWith("169.254.")) {
            throw new BusinessException(label + " 不允许指向内网地址");
        }
        if (h.startsWith("172.")) {
            String[] p = h.split("\\.");
            if (p.length >= 2) {
                try {
                    int second = Integer.parseInt(p[1]);
                    if (second >= 16 && second <= 31) {
                        throw new BusinessException(label + " 不允许指向内网地址");
                    }
                } catch (NumberFormatException ignored) {
                    /* fallthrough */
                }
            }
        }
    }
}
