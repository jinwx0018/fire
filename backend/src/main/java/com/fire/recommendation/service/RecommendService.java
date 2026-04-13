package com.fire.recommendation.service;

import com.fire.recommendation.common.PageResult;

import java.util.Map;

public interface RecommendService {

    void recordBehavior(Long userId, String behaviorType, String targetType, Long targetId);

    /**
     * 推荐列表分页。
     *
     * @param keyword      可选；非空时标题/摘要模糊匹配，并作为给 AI 的检索关键词（见 AiRecommendContext）
     * @param useAiEnhance 为 true 且服务端已配置大模型时，才对前 N 条做 AI 重排并生成推荐理由；默认 false 以节省 Token
     */
    PageResult<Map<String, Object>> recommendList(Long userId, Integer pageNum, Integer pageSize, String keyword,
                                                  boolean useAiEnhance);
}
