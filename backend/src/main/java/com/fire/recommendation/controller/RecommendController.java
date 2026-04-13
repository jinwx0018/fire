package com.fire.recommendation.controller;

import com.fire.recommendation.common.PageResult;
import com.fire.recommendation.common.Result;
import com.fire.recommendation.common.ResultCode;
import com.fire.recommendation.component.BehaviorRateLimiter;
import com.fire.recommendation.component.RecommendAiListRateLimiter;
import com.fire.recommendation.exception.BusinessException;
import com.fire.recommendation.service.AiRecommendService;
import com.fire.recommendation.service.RecommendFeedbackService;
import com.fire.recommendation.service.RecommendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "个性化知识推荐", description = "行为上报、智能推荐列表（先筛合法内容，再综合排序；按时间/热门见内容列表接口）")
@RestController
@RequestMapping("/recommend")
@RequiredArgsConstructor
public class RecommendController {

    private final RecommendService recommendService;
    private final AiRecommendService aiRecommendService;
    private final BehaviorRateLimiter behaviorRateLimiter;
    private final RecommendAiListRateLimiter recommendAiListRateLimiter;
    private final RecommendFeedbackService recommendFeedbackService;

    @Operation(summary = "推荐反馈埋点", description = "无需登录。contentId、actionType(EXPOSE|CLICK)，rankPos 可选；用于离线 CTR 分析（需先执行 DB/17_recommend_feedback_optional.sql）")
    @PostMapping("/feedback")
    public Result<Void> feedback(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Long userId = (Long) request.getAttribute("userId");
        Object cidObj = body != null ? body.get("contentId") : null;
        Long contentId = parsePositiveLongTargetId(cidObj);
        if (contentId == null) {
            throw new BusinessException("contentId 无效");
        }
        String action = body != null ? (String) body.get("actionType") : null;
        if (!StringUtils.hasText(action)) {
            action = "CLICK";
        }
        String a = action.trim().toUpperCase();
        if (!"EXPOSE".equals(a) && !"CLICK".equals(a)) {
            throw new BusinessException("actionType 须为 EXPOSE 或 CLICK");
        }
        Integer rankPos = null;
        Object rp = body != null ? body.get("rankPos") : null;
        if (rp instanceof Number) {
            rankPos = ((Number) rp).intValue();
        } else if (rp != null && StringUtils.hasText(rp.toString())) {
            try {
                rankPos = Integer.parseInt(rp.toString().trim());
            } catch (NumberFormatException ignored) {
                /* skip */
            }
        }
        recommendFeedbackService.record(userId, contentId, rankPos, a);
        return Result.ok();
    }

    @Operation(summary = "行为上报", description = "需登录。behaviorType：VIEW/LIKE/COMMENT/COLLECT；targetType：CONTENT、FORUM_POST（POST 同义）、FORUM_COMMENT、NEWS_COMMENT、KNOWLEDGE_COMMENT；targetId 正整数")
    @PostMapping("/behavior")
    public Result<Void> behavior(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        String behaviorType = body != null ? (String) body.get("behaviorType") : null;
        String targetType = body != null ? (String) body.get("targetType") : null;
        Object targetIdObj = body != null ? body.get("targetId") : null;
        if (!StringUtils.hasText(behaviorType) || !StringUtils.hasText(targetType) || targetIdObj == null) {
            throw new BusinessException("behaviorType、targetType、targetId 不能为空");
        }
        behaviorRateLimiter.check(userId);
        Long targetId = parsePositiveLongTargetId(targetIdObj);
        if (targetId == null) {
            throw new BusinessException("targetId 无效，须为正整数");
        }
        recommendService.recordBehavior(userId, behaviorType, targetType, targetId);
        return Result.ok();
    }

    @Operation(summary = "推荐列表", description = "可选登录。智能推荐：仅已上架知识；按偏好分类/标签、Item-CF、热度与时效综合排序，全量分页（越贴合越靠前）。可选 keyword 标题/摘要筛选。参数 useAi=true 且服务端已配置大模型时，仅对排序靠前的前 N 条做模型微调（recommend.ai.rerank-pool-size）；默认不调用大模型以节省 Token。")
    @GetMapping("/list")
    public Result<PageResult<Map<String, Object>>> list(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean useAi) {
        Object attr = request.getAttribute("userId");
        Long userId = attr != null ? (Long) attr : null;
        boolean useAiEnhance = Boolean.TRUE.equals(useAi);
        if (useAiEnhance && aiRecommendService.isAiLlmReady()) {
            recommendAiListRateLimiter.beforeList(request, userId);
        }
        PageResult<Map<String, Object>> page = recommendService.recommendList(userId, pageNum, pageSize, keyword, useAiEnhance);
        return Result.ok(page);
    }

    private static Long parsePositiveLongTargetId(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number) {
            long v = ((Number) o).longValue();
            return v > 0 ? v : null;
        }
        String s = o.toString().trim();
        if (!StringUtils.hasText(s)) {
            return null;
        }
        try {
            long v = Long.parseLong(s);
            return v > 0 ? v : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
