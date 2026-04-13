package com.fire.recommendation.service;

public interface RecommendFeedbackService {

    /** 推荐列表曝光或点击埋点（用于离线 CTR 等分析） */
    void record(Long userId, Long contentId, Integer rankPos, String actionType);
}
