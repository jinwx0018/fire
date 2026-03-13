package com.fire.recommendation.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fire.recommendation.common.Result;
import com.fire.recommendation.service.ForumService;
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

    @Operation(summary = "帖子分页列表", description = "支持 status、keyword 筛选，无需登录")
    @GetMapping("/post/list")
    public Result<IPage<Map<String, Object>>> postList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        IPage<Map<String, Object>> page = forumService.postList(pageNum, pageSize, status, keyword);
        return Result.ok(page);
    }

    @Operation(summary = "帖子详情", description = "含当前用户是否已点赞 liked")
    @GetMapping("/post/{id}")
    public Result<Map<String, Object>> postDetail(@Parameter(description = "帖子ID") @PathVariable Long id, HttpServletRequest request) {
        Object attr = request.getAttribute("userId");
        Long userId = attr != null ? (Long) attr : null;
        Map<String, Object> data = forumService.postDetail(id, userId);
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

    @Operation(summary = "评论列表", description = "某帖子的评论分页，无需登录")
    @GetMapping("/comment/list")
    public Result<IPage<Map<String, Object>>> commentList(
            @RequestParam Long postId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        IPage<Map<String, Object>> page = forumService.commentList(postId, pageNum, pageSize);
        return Result.ok(page);
    }

    @Operation(summary = "发表评论", description = "需登录，请求体 postId、content、parentId（可选，回复时传）")
    @PostMapping("/comment")
    public Result<Map<String, Long>> addComment(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Long userId = (Long) request.getAttribute("userId");
        Long postId = Long.valueOf(body.get("postId").toString());
        String content = (String) body.get("content");
        Long parentId = body.get("parentId") != null ? Long.valueOf(body.get("parentId").toString()) : null;
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
