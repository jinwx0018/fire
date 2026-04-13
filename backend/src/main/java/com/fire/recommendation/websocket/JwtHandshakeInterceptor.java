package com.fire.recommendation.websocket;

import com.fire.recommendation.entity.SysUser;
import com.fire.recommendation.mapper.SysUserMapper;
import com.fire.recommendation.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * WebSocket 握手：从查询参数 {@code token} 校验 JWT（浏览器无法为 WS 统一设置 Authorization 头）。
 */
@Component
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    public static final String ATTR_USER_ID = "wsUserId";

    private final JwtUtil jwtUtil;
    private final SysUserMapper userMapper;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return false;
        }
        HttpServletRequest req = servletRequest.getServletRequest();
        String token = extractTokenFromQuery(req.getQueryString());
        if (!StringUtils.hasText(token)) {
            return false;
        }
        if (!jwtUtil.validate(token) || !"access".equals(jwtUtil.getTokenType(token))) {
            return false;
        }
        Long userId = jwtUtil.getUserId(token);
        SysUser user = userMapper.selectById(userId);
        if (user == null || Integer.valueOf(1).equals(user.getDeleted())) {
            return false;
        }
        if (user.getStatus() != 1) {
            return false;
        }
        Integer tokenVersion = jwtUtil.getTokenVersion(token);
        Integer currentVersion = user.getTokenVersion() == null ? 0 : user.getTokenVersion();
        if (!currentVersion.equals(tokenVersion)) {
            return false;
        }
        attributes.put(ATTR_USER_ID, userId);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }

    private static String extractTokenFromQuery(String query) {
        if (query == null || query.isEmpty()) {
            return null;
        }
        for (String part : query.split("&")) {
            int eq = part.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String key = part.substring(0, eq);
            if (!"token".equals(key)) {
                continue;
            }
            String raw = part.substring(eq + 1);
            try {
                return URLDecoder.decode(raw, StandardCharsets.UTF_8);
            } catch (Exception e) {
                return raw;
            }
        }
        return null;
    }
}
