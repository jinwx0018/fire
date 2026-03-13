package com.fire.recommendation.controller.admin;

import com.fire.recommendation.common.Result;
import com.fire.recommendation.service.NewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "管理端-消防新闻", description = "新闻的增删改")
@RestController
@RequestMapping("/admin/news")
@RequiredArgsConstructor
public class AdminNewsController {

    private final NewsService newsService;

    @Operation(summary = "新增新闻", description = "需登录。请求体 title、content、region、urgencyLevel、summary、publishTime 等")
    @PostMapping
    public Result<Map<String, Long>> save(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Long userId = (Long) request.getAttribute("userId");
        Long id = newsService.adminSave(userId, body);
        return Result.ok(Map.of("id", id));
    }

    @Operation(summary = "修改新闻", description = "需登录")
    @PutMapping("/{id}")
    public Result<Void> update(@Parameter(description = "新闻ID") @PathVariable Long id, @RequestBody Map<String, Object> body) {
        newsService.adminUpdate(id, body);
        return Result.ok();
    }

    @Operation(summary = "删除新闻", description = "需登录，逻辑删除")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "新闻ID") @PathVariable Long id) {
        newsService.adminDelete(id);
        return Result.ok();
    }
}
