package com.fire.recommendation.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fire.recommendation.common.Result;
import com.fire.recommendation.exception.BusinessException;
import com.fire.recommendation.service.ForumService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "管理端-论坛审核", description = "帖子列表、审核通过/驳回")
@RestController
@RequestMapping("/admin/forum")
@RequiredArgsConstructor
public class AdminForumController {

    private final ForumService forumService;

    @Operation(summary = "帖子详情（管理端）", description = "含完整正文，不增加浏览量；任意审核状态，用于新窗口查看")
    @GetMapping("/post/detail/{id}")
    public Result<Map<String, Object>> postDetailAdmin(@Parameter(description = "帖子ID") @PathVariable Long id) {
        return Result.ok(forumService.adminPostDetail(id));
    }

    @Operation(summary = "论坛评论审核上下文", description = "完整帖子正文、父评论全文、本条评论，供管理端新窗口审核")
    @GetMapping("/comment/{id}/audit-detail")
    public Result<Map<String, Object>> commentAuditDetail(@Parameter(description = "评论ID") @PathVariable Long id) {
        return Result.ok(forumService.adminCommentAuditDetail(id));
    }

    @Operation(summary = "帖子分页（审核台）", description = "需管理员。可按 status 筛选：0 待审核 1 通过 -1 驳回")
    @GetMapping("/post/list")
    public Result<IPage<Map<String, Object>>> postList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        return Result.ok(forumService.adminPostList(pageNum, pageSize, status, keyword));
    }

    @Operation(summary = "帖子审核", description = "需登录/管理员。status：1 通过，-1 驳回；驳回时可填 rejectReason")
    @PutMapping("/post/audit/{id}")
    public Result<Void> auditPost(@Parameter(description = "帖子ID") @PathVariable Long id, @RequestBody Map<String, Object> body) {
        if (body == null || body.get("status") == null) {
            throw new BusinessException("status 不能为空");
        }
        Integer status;
        try {
            status = Integer.valueOf(body.get("status").toString().trim());
        } catch (NumberFormatException e) {
            throw new BusinessException("status 无效");
        }
        Object rr = body.get("rejectReason");
        String rejectReason = rr != null ? rr.toString().trim() : null;
        if (rejectReason != null && rejectReason.isEmpty()) {
            rejectReason = null;
        }
        forumService.auditPost(id, status, rejectReason);
        return Result.ok();
    }

    @Operation(summary = "删除帖子（软删）", description = "管理员")
    @DeleteMapping("/post/{id}")
    public Result<Void> deletePost(@Parameter(description = "帖子ID") @PathVariable Long id) {
        forumService.adminDeletePost(id);
        return Result.ok();
    }

    @Operation(summary = "删除评论（软删）", description = "管理员")
    @DeleteMapping("/comment/{id}")
    public Result<Void> deleteComment(@Parameter(description = "评论ID") @PathVariable Long id) {
        forumService.adminDeleteComment(id);
        return Result.ok();
    }

    @Operation(summary = "论坛评论分页（全站）", description = "可选 postId、正文关键词、status（1 显示 0 隐藏，空=全部）")
    @GetMapping("/comment/page")
    public Result<IPage<Map<String, Object>>> commentPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Long postId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        return Result.ok(forumService.adminCommentPage(pageNum, pageSize, postId, keyword, status));
    }

    @Operation(summary = "论坛评论显示/隐藏", description = "status：1 显示，0 隐藏")
    @PutMapping("/comment/{id}/status")
    public Result<Void> updateCommentStatus(
            @Parameter(description = "评论ID") @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        if (body == null || body.get("status") == null) {
            throw new BusinessException("status 不能为空");
        }
        Integer status;
        try {
            status = Integer.valueOf(body.get("status").toString().trim());
        } catch (Exception e) {
            throw new BusinessException("status 无效");
        }
        forumService.adminUpdateCommentStatus(id, status);
        return Result.ok();
    }
}
