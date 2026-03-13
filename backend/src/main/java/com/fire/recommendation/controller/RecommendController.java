package com.fire.recommendation.controller;

import com.fire.recommendation.common.PageResult;
import com.fire.recommendation.common.Result;
import com.fire.recommendation.common.ResultCode;
import com.fire.recommendation.exception.BusinessException;
import com.fire.recommendation.service.RecommendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "个性化知识推荐", description = "行为上报、推荐列表（有行为按兴趣分类，无行为全站热门）")
@RestController
@RequestMapping("/recommend")
@RequiredArgsConstructor
public class RecommendController {

    private final RecommendService recommendService;

    @Operation(summary = "行为上报", description = "需登录。记录浏览/点赞/评论/收藏等，请求体 behaviorType(VIEW/LIKE/COMMENT/COLLECT)、targetType(CONTENT/POST)、targetId")
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
        Long targetId = Long.valueOf(targetIdObj.toString());
        recommendService.recordBehavior(userId, behaviorType, targetType, targetId);
        return Result.ok();
    }

    @Operation(summary = "推荐列表", description = "可选登录。未登录返回全站热门；已登录有行为则按兴趣分类推荐")
    @GetMapping("/list")
    public Result<PageResult<Map<String, Object>>> list(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Object attr = request.getAttribute("userId");
        Long userId = attr != null ? (Long) attr : null;
        PageResult<Map<String, Object>> page = recommendService.recommendList(userId, pageNum, pageSize);
        return Result.ok(page);
    }
}
