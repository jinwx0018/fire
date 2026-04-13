package com.fire.recommendation.config;

import com.fire.recommendation.interceptor.JwtInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;

    /** 逗号分隔，追加到下方内置匿名访问路径（如第三方回调） */
    @Value("${jwt.extra-exclude-paths:}")
    private String jwtExtraExcludePaths;

    @Value("${app.upload-dir:./uploads}")
    private String uploadDir;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        List<String> excludes = new ArrayList<>(Arrays.asList(
                "/user/register", "/user/login",
                "/user/refresh-token",
                "/admin/login",
                "/user/password/sendEmail", "/user/password/resetByToken",
                "/uploads/**",
                "/content/list", "/content/category/list", "/content/share/*",
                "/forum/post/list",
                "/equipment/list", "/equipment/type/list",
                "/news/list",
                "/news/regions",
                "/news/rss",
                "/news/category/list",
                "/recommend/feedback",
                "/error",
                "/doc.html", "/webjars/**", "/v3/api-docs", "/v3/api-docs/**",
                "/ws/**"));
        if (jwtExtraExcludePaths != null && !jwtExtraExcludePaths.isBlank()) {
            for (String p : jwtExtraExcludePaths.split(",")) {
                if (p != null && !p.isBlank()) {
                    excludes.add(p.trim());
                }
            }
        }
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(excludes.toArray(new String[0]));
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + (uploadDir.endsWith("/") ? uploadDir : uploadDir + "/"));
    }
}
