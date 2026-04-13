package com.fire.recommendation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 传给 AI 排序/理由的扩展上下文：检索词、兴趣分类 Top1、运营热点词等（见功能文档-04 §5.0）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiRecommendContext {

    private String keyword;
    private String topCategoryName;
    @Builder.Default
    private List<String> hotTopicKeywords = new ArrayList<>();
    @Builder.Default
    private Map<String, Object> extra = new HashMap<>();

    public static AiRecommendContext empty() {
        return AiRecommendContext.builder()
                .hotTopicKeywords(Collections.emptyList())
                .extra(Collections.emptyMap())
                .build();
    }
}
