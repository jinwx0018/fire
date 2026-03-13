package com.fire.recommendation.config;

import com.fire.recommendation.interceptor.JwtInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;

    @Value("${jwt.exclude-paths:/api/user/register,/api/user/login,/api/user/password/sendEmail,/api/user/password/resetByToken}")
    private List<String> excludePaths;

    @Value("${app.upload-dir:./uploads}")
    private String uploadDir;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/user/register", "/user/login",
                        "/user/password/sendEmail", "/user/password/resetByToken",
                        "/uploads/**",
                        "/content/list", "/content/category/list", "/content/share/*",
                        "/forum/post/list", "/forum/comment/list",
                        "/equipment/list", "/equipment/type/list",
                        "/news/list",
                        "/error",
                        "/doc.html", "/webjars/**", "/v3/api-docs", "/v3/api-docs/**"
                );
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + (uploadDir.endsWith("/") ? uploadDir : uploadDir + "/"))
                .setOrder(Ordered.LOWEST_PRECEDENCE);
    }
}
