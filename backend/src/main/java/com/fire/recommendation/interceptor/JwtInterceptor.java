package com.fire.recommendation.interceptor;

import com.fire.recommendation.common.ResultCode;
import com.fire.recommendation.entity.SysUser;
import com.fire.recommendation.exception.BusinessException;
import com.fire.recommendation.mapper.SysUserMapper;
import com.fire.recommendation.util.InternalNetworkClient;
import com.fire.recommendation.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final SysUserMapper userMapper;

    /** false 时 /doc.html、OpenAPI 文档路径直接 404（与 springdoc/knife4j 开关配合使用） */
    @Value("${app.api-docs.enabled:true}")
    private boolean apiDocsEnabled;

    /** true 时仅本机或常见私网 IP 可访问文档（反向代理后请确认 X-Forwarded-For 可信） */
    @Value("${app.api-docs.restrict-to-internal-network:false}")
    private boolean apiDocsRestrictInternal;

    /** 认证可选的路径：无 Token 时放行并设置 userId=null */
    private static boolean isOptionalAuthPath(String path, String method) {
        if (path == null) return false;
        // /content/1, /forum/post/1, /equipment/1, /news/1, /recommend/list
        if (path.equals("/recommend/list")) return true;
        if (path.startsWith("/content/") && path.length() > 9 && path.substring(9).matches("\\d+")) return true;
        if (path.startsWith("/forum/post/") && path.length() > 12 && path.substring(12).matches("\\d+")) return true;
        if ("/forum/comment/list".equals(path)) return true;
        if (path.startsWith("/equipment/") && path.length() > 11 && path.substring(11).matches("\\d+")) return true;
        if (path.startsWith("/news/") && path.length() > 6 && path.substring(6).matches("\\d+")) return true;
        // 知识评论列表可匿名；发表评论需登录（POST 不走此处）
        if (path.matches("^/content/\\d+/comments$")) {
            return "GET".equalsIgnoreCase(method);
        }
        return false;
    }

    private static boolean isOpenApiDocsPath(String path) {
        if (path == null) {
            return false;
        }
        return path.startsWith("/v3/api-docs") || path.equals("/doc.html") || path.startsWith("/webjars/");
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getServletPath();
        if (isOpenApiDocsPath(path)) {
            if (!apiDocsEnabled) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return false;
            }
            if (apiDocsRestrictInternal && !InternalNetworkClient.isInternalOrLocalhost(request)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return false;
            }
            return true;
        }
        String auth = request.getHeader("Authorization");
        boolean hasValidToken = StringUtils.hasText(auth) && auth.startsWith("Bearer ");
        if (hasValidToken) {
            String token = auth.substring(7).trim();
            if (jwtUtil.validate(token)) {
                if (!"access".equals(jwtUtil.getTokenType(token))) {
                    throw new BusinessException(ResultCode.UNAUTHORIZED, "token 类型错误");
                }
                Long userId = jwtUtil.getUserId(token);
                SysUser user = userMapper.selectById(userId);
                if (user == null || Integer.valueOf(1).equals(user.getDeleted())) {
                    throw new BusinessException(ResultCode.UNAUTHORIZED, "账号不存在或已注销");
                }
                if (user.getStatus() != 1) {
                    throw new BusinessException(ResultCode.UNAUTHORIZED, "您的账号已冻结，请联系管理员处理");
                }
                Integer tokenVersion = jwtUtil.getTokenVersion(token);
                Integer currentVersion = user.getTokenVersion() == null ? 0 : user.getTokenVersion();
                if (!currentVersion.equals(tokenVersion)) {
                    throw new BusinessException(ResultCode.UNAUTHORIZED, "您的账号已被强制下线");
                }
                request.setAttribute("userId", userId);
                // 管理端接口：仅 ADMIN 角色可访问（/admin/login 已在下方排除）
                if (path.startsWith("/admin/") && !path.equals("/admin/login")) {
                    String role = jwtUtil.getRole(token);
                    if (!"ADMIN".equals(role)) {
                        throw new BusinessException(ResultCode.FORBIDDEN, "无权限访问管理端");
                    }
                }
                return true;
            }
        }
        if ("GET".equalsIgnoreCase(request.getMethod()) && path != null && path.matches("^/news/\\d+/comments$")) {
            request.setAttribute("userId", null);
            return true;
        }
        if (isOptionalAuthPath(path, request.getMethod())) {
            request.setAttribute("userId", null);
            return true;
        }
        throw new BusinessException(ResultCode.UNAUTHORIZED);
    }
}
