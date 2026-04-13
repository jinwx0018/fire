package com.fire.recommendation.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fire.recommendation.common.Result;
import com.fire.recommendation.component.NewsReadRateLimiter;
import com.fire.recommendation.exception.BusinessException;
import com.fire.recommendation.service.NewsCommentService;
import com.fire.recommendation.service.NewsRssService;
import com.fire.recommendation.service.NewsService;
import com.fire.recommendation.util.HttpClientIp;
import com.fire.recommendation.service.UserCollectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "消防新闻", description = "列表、详情、RSS、评论、点赞")
@RestController
@RequestMapping("/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsService newsService;
    private final NewsCommentService newsCommentService;
    private final NewsRssService newsRssService;
    private final NewsReadRateLimiter newsReadRateLimiter;
    private final UserCollectionService userCollectionService;

    @Operation(summary = "RSS 2.0 订阅", description = "最近已上架新闻，UTF-8")
    @GetMapping(value = "/rss", produces = "application/rss+xml;charset=UTF-8")
    public ResponseEntity<String> rss(HttpServletRequest request) {
        newsReadRateLimiter.beforeRss(request);
        String xml = newsRssService.buildRssXml(30);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/rss+xml;charset=UTF-8")).body(xml);
    }

    @Operation(summary = "已上架新闻中的地区去重列表", description = "供筛选多选")
    @GetMapping("/regions")
    public Result<List<String>> regions() {
        return Result.ok(newsService.listDistinctRegions());
    }

    @Operation(summary = "新闻分页列表", description = "标题/摘要 LIKE 模糊；地区支持 regions 多选或 region 单值")
    @GetMapping("/list")
    public Result<IPage<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "12") Integer pageSize,
            @RequestParam(required = false) List<String> regions,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String orderBy,
            @RequestParam(required = false) String order,
            HttpServletRequest request) {
        newsReadRateLimiter.beforeList(request);
        IPage<Map<String, Object>> page = newsService.list(pageNum, pageSize, regions, region, categoryId, category, title, keyword,
                orderBy, order);
        return Result.ok(page);
    }

    @Operation(summary = "新闻评论分页", description = "带 Token 时返回每条评论 liked。sortBy=time 按时间倒序（默认）；sortBy=hot 按点赞倒序")
    @GetMapping("/{id}/comments")
    public Result<IPage<Map<String, Object>>> comments(
            @Parameter(description = "新闻ID") @PathVariable Long id,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false, defaultValue = "time") String sortBy,
            HttpServletRequest request) {
        newsReadRateLimiter.beforeDetail(request);
        Object attr = request.getAttribute("userId");
        Long viewerId = attr instanceof Long ? (Long) attr : null;
        return Result.ok(newsCommentService.pageForNews(id, pageNum, pageSize, viewerId, sortBy));
    }

    @Operation(summary = "新闻评论点赞/取消", description = "需登录；Redis key news:comment:like:{commentId}")
    @PostMapping("/comment/like/{commentId}")
    public Result<Map<String, Object>> likeNewsComment(
            @Parameter(description = "评论ID") @PathVariable Long commentId,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.ok(newsCommentService.toggleCommentLike(commentId, userId));
    }

    @Operation(summary = "发表评论", description = "需登录")
    @PostMapping("/{id}/comments")
    public Result<Void> postComment(
            @Parameter(description = "新闻ID") @PathVariable Long id,
            HttpServletRequest request,
            @RequestBody Map<String, Object> body) {
        Long userId = (Long) request.getAttribute("userId");
        newsReadRateLimiter.beforeNewsCommentPost(request, userId);
        String content = body.get("content") != null ? body.get("content").toString() : "";
        Long parentId = parseOptionalPositiveLong(body.get("parentId"), "parentId");
        newsCommentService.post(id, userId, content, parentId);
        return Result.ok();
    }

    @Operation(summary = "删除评论", description = "需登录，仅本人可删除自己的评论")
    @DeleteMapping("/comment/{commentId}")
    public Result<Void> deleteOwnComment(
            @Parameter(description = "评论ID") @PathVariable Long commentId,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        newsCommentService.deleteOwn(commentId, userId);
        return Result.ok();
    }

    @Operation(summary = "点赞/取消点赞", description = "需登录，幂等切换；返回 liked、likeCount，避免前端再拉详情导致浏览量重复 +1")
    @PostMapping("/{id}/like")
    public Result<Map<String, Object>> like(
            @Parameter(description = "新闻ID") @PathVariable Long id,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.ok(newsService.toggleLike(id, userId));
    }

    @Operation(summary = "收藏新闻", description = "需登录；仅已上架且已到发布时间的可收藏")
    @PostMapping("/{id}/collect")
    public Result<Map<String, Boolean>> collectNews(
            @Parameter(description = "新闻ID") @PathVariable Long id,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        userCollectionService.collectNews(userId, id);
        return Result.ok(Map.of("collected", true));
    }

    @Operation(summary = "取消收藏新闻", description = "需登录")
    @DeleteMapping("/{id}/collect")
    public Result<Void> uncollectNews(
            @Parameter(description = "新闻ID") @PathVariable Long id,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        userCollectionService.uncollectNews(userId, id);
        return Result.ok();
    }

    @Operation(summary = "新闻详情", description = "含 related、likeCount、liked（登录时）；recordView=false 时不增加浏览量")
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(
            @Parameter(description = "新闻ID") @PathVariable Long id,
            @RequestParam(defaultValue = "true") boolean recordView,
            HttpServletRequest request) {
        newsReadRateLimiter.beforeDetail(request);
        Long viewer = null;
        Object u = request.getAttribute("userId");
        if (u instanceof Long) {
            viewer = (Long) u;
        }
        return Result.ok(newsService.getDetail(id, viewer, recordView, HttpClientIp.resolve(request)));
    }

    /** null 表示未传或空串；非法格式抛出业务异常（避免 500） */
    private static Long parseOptionalPositiveLong(Object raw, String field) {
        if (raw == null) {
            return null;
        }
        String s = raw.toString().trim();
        if (s.isEmpty()) {
            return null;
        }
        try {
            long v = Long.parseLong(s);
            if (v <= 0) {
                throw new BusinessException(field + " 须为正整数");
            }
            return v;
        } catch (NumberFormatException e) {
            throw new BusinessException(field + " 须为数字");
        }
    }
}
