package com.fire.recommendation;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.fire.recommendation.mapper")
public class FireRecommendationApplication {

    public static void main(String[] args) {
        SpringApplication.run(FireRecommendationApplication.class, args);
    }
}
