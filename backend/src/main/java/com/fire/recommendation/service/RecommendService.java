package com.fire.recommendation.service;

import com.fire.recommendation.common.PageResult;

import java.util.Map;

public interface RecommendService {

    void recordBehavior(Long userId, String behaviorType, String targetType, Long targetId);

    PageResult<Map<String, Object>> recommendList(Long userId, Integer pageNum, Integer pageSize);
}
