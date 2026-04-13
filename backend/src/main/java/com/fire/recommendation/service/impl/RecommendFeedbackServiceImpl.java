package com.fire.recommendation.service.impl;

import com.fire.recommendation.entity.RecommendFeedback;
import com.fire.recommendation.mapper.RecommendFeedbackMapper;
import com.fire.recommendation.service.RecommendFeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RecommendFeedbackServiceImpl implements RecommendFeedbackService {

    private final RecommendFeedbackMapper recommendFeedbackMapper;

    @Value("${recommend.feedback.enabled:true}")
    private boolean enabled;

    @Override
    public void record(Long userId, Long contentId, Integer rankPos, String actionType) {
        if (!enabled || contentId == null || contentId <= 0) {
            return;
        }
        RecommendFeedback f = new RecommendFeedback();
        f.setUserId(userId);
        f.setContentId(contentId);
        f.setRankPos(rankPos);
        f.setActionType(actionType != null ? actionType.trim().toUpperCase() : "CLICK");
        f.setCreateTime(LocalDateTime.now());
        recommendFeedbackMapper.insert(f);
    }
}
