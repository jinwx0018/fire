package com.fire.recommendation.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fire.recommendation.common.Result;
import com.fire.recommendation.exception.BusinessException;
import com.fire.recommendation.service.NewsCommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Objects;

@Tag(name = "管理端-新闻评论", description = "分页查询、删除违规评论")
@RestController
@RequestMapping("/admin/news/comment")
@RequiredArgsConstructor
public class AdminNewsCommentController {

    private final NewsCommentService newsCommentService;

    @Operation(summary = "评论审核上下文", description = "完整新闻正文、父评论全文、本条评论，供管理端新窗口审核")
    @GetMapping("/{id}/audit-detail")
    public Result<Map<String, Object>> auditDetail(@Parameter(description = "评论ID") @PathVariable Long id) {
        return Result.ok(newsCommentService.adminAuditDetail(id));
    }

    @Operation(summary = "评论分页", description = "含新闻标题、用户名、父评论摘要；可选 newsId、正文关键词、status（1 显示 0 隐藏，空=全部）")
    @GetMapping("/page")
    public Result<IPage<Map<String, Object>>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Long newsId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        return Result.ok(newsCommentService.adminPage(pageNum, pageSize, newsId, keyword, status));
    }

    @Operation(summary = "显示/隐藏评论")
    @PutMapping("/{id}/status")
    public Result<Void> setStatus(HttpServletRequest request, @PathVariable Long id, @RequestBody Map<String, Object> body) {
        if (body == null || body.get("status") == null) {
            throw new BusinessException("status 不能为空");
        }
        int status;
        try {
            status = Integer.parseInt(Objects.requireNonNull(body.get("status")).toString().trim());
        } catch (Exception e) {
            throw new BusinessException("status 无效");
        }
        Long op = (Long) request.getAttribute("userId");
        newsCommentService.adminSetStatus(id, status, op);
        return Result.ok();
    }

    @Operation(summary = "删除评论（逻辑删除）")
    @DeleteMapping("/{id}")
    public Result<Void> delete(HttpServletRequest request, @Parameter(description = "评论ID") @PathVariable Long id) {
        Long op = (Long) request.getAttribute("userId");
        newsCommentService.adminDelete(id, op);
        return Result.ok();
    }
}
