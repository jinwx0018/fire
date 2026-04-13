package com.fire.recommendation.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 判断请求是否来自本机或常见私网段（用于限制 Knife4j 等仅内网访问，非严谨防火墙替代）。
 */
public final class InternalNetworkClient {

    private InternalNetworkClient() {
    }

    public static boolean isInternalOrLocalhost(HttpServletRequest request) {
        String ip = HttpClientIp.resolve(request);
        return isInternalOrLocalhost(ip);
    }

    public static boolean isInternalOrLocalhost(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        if ("127.0.0.1".equals(ip) || "::1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) {
            return true;
        }
        if (ip.startsWith("192.168.")) {
            return true;
        }
        if (ip.startsWith("10.")) {
            return true;
        }
        if (ip.startsWith("172.")) {
            int dot = ip.indexOf('.', 4);
            if (dot > 4) {
                try {
                    int second = Integer.parseInt(ip.substring(4, dot));
                    return second >= 16 && second <= 31;
                } catch (NumberFormatException ignored) {
                    return false;
                }
            }
        }
        return false;
    }
}
