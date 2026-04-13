package com.fire.recommendation.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class CorsConfig {

    @Value("${app.cors.allow-all:true}")
    private boolean corsAllowAll;

    @Value("${app.cors.allowed-origin-patterns:http://localhost:5173,http://localhost:5177}")
    private String allowedOriginPatternsRaw;

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        if (corsAllowAll) {
            config.addAllowedOriginPattern("*");
        } else {
            List<String> patterns = new ArrayList<>();
            for (String p : allowedOriginPatternsRaw.split(",")) {
                if (StringUtils.hasText(p)) {
                    patterns.add(p.trim());
                }
            }
            if (patterns.isEmpty()) {
                patterns.add("http://localhost:5173");
                patterns.add("http://localhost:5177");
            }
            for (String p : patterns) {
                config.addAllowedOriginPattern(p);
            }
        }
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
