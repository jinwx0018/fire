package com.fire.recommendation.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fire.recommendation.common.PageResult;
import com.fire.recommendation.common.Result;
import com.fire.recommendation.service.ContentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "消防知识内容管理", description = "知识列表、详情、发布、收藏、分享、分类")
@RestController
@RequestMapping("/content")
@RequiredArgsConstructor
public class ContentController {

    private final ContentService contentService;

    @Operation(summary = "内容分页列表", description = "按分类、标题模糊搜索，支持 status 筛选，无需登录")
    @GetMapping("/list")
    public Result<IPage<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Integer status) {
        IPage<Map<String, Object>> page = contentService.list(pageNum, pageSize, categoryId, title, status);
        return Result.ok(page);
    }

    @Operation(summary = "内容详情", description = "获取单条知识详情，含标签与是否已收藏；会递增浏览量")
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(
            @Parameter(description = "内容ID") @PathVariable Long id, HttpServletRequest request) {
        Object attr = request.getAttribute("userId");
        Long userId = attr != null ? (Long) attr : null;
        Map<String, Object> data = contentService.getDetail(id, userId);
        return Result.ok(data);
    }

    @Operation(summary = "新增内容", description = "需登录，作者/管理员可发布，请求体含 title、categoryId、content、cover、summary、tags、status")
    @PostMapping
    public Result<Map<String, Long>> save(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Long userId = (Long) request.getAttribute("userId");
        Long id = contentService.save(userId, body);
        return Result.ok(Map.of("id", id));
    }

    @Operation(summary = "修改内容", description = "需登录")
    @PutMapping("/{id}")
    public Result<Void> update(@Parameter(description = "内容ID") @PathVariable Long id, HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Long userId = (Long) request.getAttribute("userId");
        contentService.update(id, userId, body);
        return Result.ok();
    }

    @Operation(summary = "删除内容", description = "需登录，逻辑删除")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "内容ID") @PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        contentService.delete(id, userId);
        return Result.ok();
    }

    @Operation(summary = "收藏内容", description = "需登录，同一用户同一内容仅一条收藏")
    @PostMapping("/collect/{contentId}")
    public Result<Map<String, Boolean>> collect(@Parameter(description = "内容ID") @PathVariable Long contentId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        contentService.collect(userId, contentId);
        return Result.ok(Map.of("collected", true));
    }

    @Operation(summary = "取消收藏", description = "需登录")
    @DeleteMapping("/collect/{contentId}")
    public Result<Void> uncollect(@Parameter(description = "内容ID") @PathVariable Long contentId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        contentService.uncollect(userId, contentId);
        return Result.ok();
    }

    @Operation(summary = "我的收藏列表", description = "需登录，分页返回已收藏的知识摘要")
    @GetMapping("/collect/list")
    public Result<PageResult<Map<String, Object>>> collectList(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = (Long) request.getAttribute("userId");
        PageResult<Map<String, Object>> page = contentService.collectList(userId, pageNum, pageSize);
        return Result.ok(page);
    }

    @Operation(summary = "生成分享链接", description = "返回前端配置的分享 URL，无需登录")
    @GetMapping("/share/{contentId}")
    public Result<Map<String, Object>> share(@Parameter(description = "内容ID") @PathVariable Long contentId) {
        return Result.ok(contentService.shareUrl(contentId));
    }

    @Operation(summary = "分类列表", description = "知识分类列表（id、name、sort），无需登录")
    @GetMapping("/category/list")
    public Result<List<Map<String, Object>>> categoryList() {
        return Result.ok(contentService.categoryList());
    }
}
