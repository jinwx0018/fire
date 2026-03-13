package com.fire.recommendation.controller.admin;

import com.fire.recommendation.common.Result;
import com.fire.recommendation.service.ContentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "管理端-知识分类", description = "知识分类的增删改")
@RestController
@RequestMapping("/admin/content")
@RequiredArgsConstructor
public class AdminContentCategoryController {

    private final ContentService contentService;

    @Operation(summary = "新增分类", description = "需登录。请求体 name、sort")
    @PostMapping("/category")
    public Result<Map<String, Long>> saveCategory(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        Integer sort = body.get("sort") != null ? Integer.valueOf(body.get("sort").toString()) : 0;
        Long id = contentService.adminSaveCategory(name, sort);
        return Result.ok(Map.of("id", id));
    }

    @Operation(summary = "修改分类", description = "需登录")
    @PutMapping("/category/{id}")
    public Result<Void> updateCategory(@Parameter(description = "分类ID") @PathVariable Long id, @RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        Integer sort = body.get("sort") != null ? Integer.valueOf(body.get("sort").toString()) : null;
        contentService.adminUpdateCategory(id, name, sort);
        return Result.ok();
    }

    @Operation(summary = "删除分类", description = "需登录")
    @DeleteMapping("/category/{id}")
    public Result<Void> deleteCategory(@Parameter(description = "分类ID") @PathVariable Long id) {
        contentService.adminDeleteCategory(id);
        return Result.ok();
    }
}
