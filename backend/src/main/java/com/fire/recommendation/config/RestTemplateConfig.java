package com.fire.recommendation.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        return new RestTemplate(factory);
    }

    /** 与 ai.recommend.timeout-ms 一致，供智能推荐外部 HTTP 调用（豆包/通用网关） */
    @Bean("aiRecommendRestTemplate")
    public RestTemplate aiRecommendRestTemplate(@Value("${ai.recommend.timeout-ms:5000}") int timeoutMs) {
        int t = Math.max(1000, Math.min(timeoutMs, 120_000));
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Math.min(t, 15_000));
        factory.setReadTimeout(t);
        return new RestTemplate(factory);
    }
}
