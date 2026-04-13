package com.fire.recommendation.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fire.recommendation.common.Result;
import com.fire.recommendation.service.NewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "管理端-消防新闻", description = "新闻分页、详情、增删改")
@RestController
@RequestMapping("/admin/news")
@RequiredArgsConstructor
public class AdminNewsController {

    private final NewsService newsService;

    @Operation(summary = "新闻地区去重列表（管理端）", description = "含下架等全部未删稿；需在 /{id} 之前声明")
    @GetMapping("/regions")
    public Result<List<String>> adminRegions() {
        return Result.ok(newsService.listDistinctRegionsForAdmin());
    }

    @Operation(summary = "新闻分页（管理端）", description = "含下架稿；筛选项与用户端列表一致，另加 status")
    @GetMapping("/page")
    public Result<IPage<Map<String, Object>>> adminPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) List<String> regions,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String orderBy,
            @RequestParam(required = false) String order,
            @RequestParam(required = false) Integer status) {
        return Result.ok(newsService.adminPage(pageNum, pageSize, regions, region, categoryId, category, title, keyword,
                orderBy, order, status));
    }

    @Operation(summary = "新闻详情（管理端）", description = "含下架，供编辑")
    @GetMapping("/{id}")
    public Result<Map<String, Object>> adminDetail(@Parameter(description = "新闻ID") @PathVariable Long id) {
        return Result.ok(newsService.adminGetDetail(id));
    }

    @Operation(summary = "新增新闻", description = "需登录。title、content 必填；urgencyLevel 1-3；status 0/1；publishTime 可选")
    @PostMapping
    public Result<Map<String, Long>> save(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Long userId = (Long) request.getAttribute("userId");
        Long id = newsService.adminSave(userId, body);
        return Result.ok(Map.of("id", id));
    }

    @Operation(summary = "修改新闻", description = "需登录")
    @PutMapping("/{id}")
    public Result<Void> update(HttpServletRequest request, @Parameter(description = "新闻ID") @PathVariable Long id,
                               @RequestBody Map<String, Object> body) {
        Long op = (Long) request.getAttribute("userId");
        newsService.adminUpdate(op, id, body);
        return Result.ok();
    }

    @Operation(summary = "删除新闻", description = "需登录，逻辑删除")
    @DeleteMapping("/{id}")
    public Result<Void> delete(HttpServletRequest request, @Parameter(description = "新闻ID") @PathVariable Long id) {
        Long op = (Long) request.getAttribute("userId");
        newsService.adminDelete(op, id);
        return Result.ok();
    }
}
