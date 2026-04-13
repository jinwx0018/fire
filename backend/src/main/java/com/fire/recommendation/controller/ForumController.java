package com.fire.recommendation.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fire.recommendation.common.Result;
import com.fire.recommendation.exception.BusinessException;
import com.fire.recommendation.service.ForumService;
import com.fire.recommendation.service.UserCollectionService;
import com.fire.recommendation.util.HttpClientIp;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "消防知识论坛", description = "帖子发布、列表、详情、点赞、评论")
@RestController
@RequestMapping("/forum")
@RequiredArgsConstructor
public class ForumController {

    private final ForumService forumService;
    private final UserCollectionService userCollectionService;

    @Operation(summary = "帖子分页列表", description = "仅展示审核通过帖子；keyword 标题/正文模糊，无需登录")
    @GetMapping("/post/list")
    public Result<IPage<Map<String, Object>>> postList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword) {
        IPage<Map<String, Object>> page = forumService.postList(pageNum, pageSize, keyword);
        return Result.ok(page);
    }

    @Operation(summary = "我的帖子", description = "需登录；含待审核/已通过/已驳回；keyword 可选")
    @GetMapping("/post/mine")
    public Result<IPage<Map<String, Object>>> myPostList(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.ok(forumService.myPostList(userId, pageNum, pageSize, keyword));
    }

    @Operation(summary = "帖子详情", description = "含当前用户是否已点赞 liked；recordView=false 时不增加浏览量（用于前端刷新元数据）")
    @GetMapping("/post/{id}")
    public Result<Map<String, Object>> postDetail(
            @Parameter(description = "帖子ID") @PathVariable Long id,
            @RequestParam(defaultValue = "true") boolean recordView,
            HttpServletRequest request) {
        Object attr = request.getAttribute("userId");
        Long userId = attr != null ? (Long) attr : null;
        Map<String, Object> data = forumService.postDetail(id, userId, recordView, HttpClientIp.resolve(request));
        return Result.ok(data);
    }

    @Operation(summary = "发布帖子", description = "需登录，请求体 title、content")
    @PostMapping("/post")
    public Result<Map<String, Long>> publishPost(HttpServletRequest request, @RequestBody Map<String, String> body) {
        Long userId = (Long) request.getAttribute("userId");
        Long id = forumService.publishPost(userId, body.get("title"), body.get("content"));
        return Result.ok(Map.of("id", id));
    }

    @Operation(summary = "修改帖子", description = "需登录，仅作者可修改")
    @PutMapping("/post/{id}")
    public Result<Void> updatePost(@Parameter(description = "帖子ID") @PathVariable Long id, HttpServletRequest request, @RequestBody Map<String, String> body) {
        Long userId = (Long) request.getAttribute("userId");
        forumService.updatePost(id, userId, body.get("title"), body.get("content"));
        return Result.ok();
    }

    @Operation(summary = "删除帖子", description = "需登录，仅作者可删")
    @DeleteMapping("/post/{id}")
    public Result<Void> deletePost(@Parameter(description = "帖子ID") @PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        forumService.deletePost(id, userId);
        return Result.ok();
    }

    @Operation(summary = "点赞/取消点赞", description = "需登录，第一次点赞、第二次取消（切换）")
    @PostMapping("/post/like/{postId}")
    public Result<Map<String, Object>> likePost(@Parameter(description = "帖子ID") @PathVariable Long postId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.ok(forumService.likePost(postId, userId));
    }

    @Operation(summary = "收藏帖子", description = "需登录；仅审核通过的帖子可收藏")
    @PostMapping("/post/collect/{postId}")
    public Result<Map<String, Boolean>> collectPost(
            @Parameter(description = "帖子ID") @PathVariable Long postId,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        userCollectionService.collectForumPost(userId, postId);
        return Result.ok(Map.of("collected", true));
    }

    @Operation(summary = "取消收藏帖子", description = "需登录")
    @DeleteMapping("/post/collect/{postId}")
    public Result<Void> uncollectPost(
            @Parameter(description = "帖子ID") @PathVariable Long postId,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        userCollectionService.uncollectForumPost(userId, postId);
        return Result.ok();
    }

    @Operation(summary = "评论列表", description = "与帖子可见性一致：未通过审核的帖子仅作者/管理员带 Token 时可查看评论。sortBy=time 按时间倒序（默认）；sortBy=hot 按点赞倒序")
    @GetMapping("/comment/list")
    public Result<IPage<Map<String, Object>>> commentList(
            @RequestParam Long postId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false, defaultValue = "time") String sortBy,
            HttpServletRequest request) {
        Object attr = request.getAttribute("userId");
        Long viewerId = attr != null ? (Long) attr : null;
        IPage<Map<String, Object>> page = forumService.commentList(postId, pageNum, pageSize, viewerId, sortBy);
        return Result.ok(page);
    }

    @Operation(summary = "评论点赞/取消", description = "需登录；Redis key forum:comment:like:{commentId}，无 Redis 时写 user_comment_like")
    @PostMapping("/comment/like/{commentId}")
    public Result<Map<String, Object>> likeComment(
            @Parameter(description = "评论ID") @PathVariable Long commentId,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.ok(forumService.likeComment(commentId, userId));
    }

    @Operation(summary = "发表评论", description = "需登录，请求体 postId、content、parentId（可选，回复时传）")
    @PostMapping("/comment")
    public Result<Map<String, Long>> addComment(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Long userId = (Long) request.getAttribute("userId");
        if (body == null) {
            throw new BusinessException("请求体不能为空");
        }
        Object postIdObj = body.get("postId");
        if (postIdObj == null) {
            throw new BusinessException("postId 不能为空");
        }
        Long postId;
        try {
            postId = Long.valueOf(postIdObj.toString().trim());
        } catch (NumberFormatException e) {
            throw new BusinessException("postId 无效");
        }
        Object contentObj = body.get("content");
        String content = contentObj != null ? contentObj.toString() : null;
        Long parentId = null;
        Object parentObj = body.get("parentId");
        if (parentObj != null) {
            try {
                parentId = Long.valueOf(parentObj.toString().trim());
            } catch (NumberFormatException e) {
                throw new BusinessException("parentId 无效");
            }
        }
        Long id = forumService.addComment(userId, postId, content, parentId);
        return Result.ok(Map.of("id", id));
    }

    @Operation(summary = "删除评论", description = "需登录，仅评论者可删")
    @DeleteMapping("/comment/{id}")
    public Result<Void> deleteComment(@Parameter(description = "评论ID") @PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        forumService.deleteComment(id, userId);
        return Result.ok();
    }
}
