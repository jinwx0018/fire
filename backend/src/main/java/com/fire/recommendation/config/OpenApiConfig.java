package com.fire.recommendation.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Knife4j 配置，接口文档与在线测试页：GET /api/doc.html
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "消防科普推荐系统 API",
                version = "1.0.0",
                description = "消防科普推荐系统后端接口文档。包含：用户管理、消防知识内容、论坛、个性化推荐、消防器材、消防新闻、数据统计等模块。除登录/注册/密码找回等接口外，需在请求头携带 Authorization: Bearer {token}。",
                contact = @Contact(name = "消防科普推荐系统")
        ),
        security = {
                @SecurityRequirement(name = "bearerAuth")
        }
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "登录后获取的 JWT Token，在请求头填写：Bearer {token}"
)
public class OpenApiConfig {
}

