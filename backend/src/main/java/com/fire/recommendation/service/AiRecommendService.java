package com.fire.recommendation.service;

import com.fire.recommendation.dto.AiRecommendContext;

import java.util.List;
import java.util.Map;

/**
 * 智能推荐 - AI 模型增强服务。
 * AI 参与选品+排序：对召回候选进行打分/重排；并可对最终列表补充推荐理由（recommendReason）。
 * 未配置或调用失败时不改变原有列表。
 */
public interface AiRecommendService {

    /**
     * AI 参与排序：对候选列表按“对当前用户的推荐优先级”重排（打分或返回顺序）。
     * 用于实现智能推荐核心——选品与排序。未启用 AI 或调用异常时返回原列表。
     *
     * @param userId    当前用户 ID，可为 null（冷启动）
     * @param candidates 候选列表，每项至少含 id、title、summary、categoryName
     * @return 重排后的列表（顺序可能改变），若未启用或失败则返回原 candidates
     */
    default List<Map<String, Object>> reorderByAi(Long userId, List<Map<String, Object>> candidates) {
        return reorderByAi(userId, candidates, AiRecommendContext.empty());
    }

    /**
     * 同 {@link #reorderByAi(Long, List)}，并传入检索词/偏好分类/热点词等供 Prompt 或通用 API 使用。
     */
    List<Map<String, Object>> reorderByAi(Long userId, List<Map<String, Object>> candidates, AiRecommendContext ctx);

    /**
     * 对推荐列表进行 AI 增强：为每条结果补充推荐理由（recommendReason）。
     * 若未启用 AI 或调用异常，不修改 items。
     */
    default void enrichRecommendReasons(Long userId, List<Map<String, Object>> items) {
        enrichRecommendReasons(userId, items, AiRecommendContext.empty());
    }

    void enrichRecommendReasons(Long userId, List<Map<String, Object>> items, AiRecommendContext ctx);

    /** 是否启用 AI 推荐（配置开关） */
    boolean isAiRecommendEnabled();

    /**
     * 是否已具备调用大模型/外部网关的条件（开关开启且 URL、密钥、模型等已配齐）。
     * 豆包模式需同时配置 api-url、api-key、model（推理接入点 ID）。
     */
    boolean isAiLlmReady();
}
