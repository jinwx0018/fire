package com.fire.recommendation.interceptor;

import com.fire.recommendation.common.ResultCode;
import com.fire.recommendation.exception.BusinessException;
import com.fire.recommendation.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    /** 认证可选的路径：无 Token 时放行并设置 userId=null */
    private static boolean isOptionalAuthPath(String path) {
        if (path == null) return false;
        // /content/1, /forum/post/1, /equipment/1, /news/1, /recommend/list
        if (path.equals("/recommend/list")) return true;
        if (path.startsWith("/content/") && path.length() > 9 && path.substring(9).matches("\\d+")) return true;
        if (path.startsWith("/forum/post/") && path.length() > 12 && path.substring(12).matches("\\d+")) return true;
        if (path.startsWith("/equipment/") && path.length() > 11 && path.substring(11).matches("\\d+")) return true;
        if (path.startsWith("/news/") && path.length() > 6 && path.substring(6).matches("\\d+")) return true;
        return false;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getServletPath();
        // Knife4j 接口文档与静态资源放行
        if (path.startsWith("/v3/api-docs") || path.equals("/doc.html") || path.startsWith("/webjars/")) {
            return true;
        }
        String auth = request.getHeader("Authorization");
        boolean hasValidToken = StringUtils.hasText(auth) && auth.startsWith("Bearer ");
        if (hasValidToken) {
            String token = auth.substring(7).trim();
            if (jwtUtil.validate(token)) {
                request.setAttribute("userId", jwtUtil.getUserId(token));
                return true;
            }
        }
        if (isOptionalAuthPath(path)) {
            request.setAttribute("userId", null);
            return true;
        }
        throw new BusinessException(ResultCode.UNAUTHORIZED);
    }
}
