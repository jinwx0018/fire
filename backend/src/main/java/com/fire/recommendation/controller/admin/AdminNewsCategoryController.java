package com.fire.recommendation.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fire.recommendation.common.Result;
import com.fire.recommendation.entity.NewsCategory;
import com.fire.recommendation.service.NewsCategoryDictService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "管理端-新闻分类字典", description = "增删改查")
@RestController
@RequestMapping("/admin/news/category")
@RequiredArgsConstructor
public class AdminNewsCategoryController {

    private final NewsCategoryDictService newsCategoryDictService;

    @Operation(summary = "全部分类（含逻辑删除过滤后）")
    @GetMapping("/list")
    public Result<List<NewsCategory>> list() {
        return Result.ok(newsCategoryDictService.adminListAll());
    }

    @Operation(summary = "分类分页（管理端）", description = "按 sortOrder、id 升序")
    @GetMapping("/page")
    public Result<IPage<NewsCategory>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.ok(newsCategoryDictService.adminPage(pageNum, pageSize));
    }

    @Operation(summary = "新增分类")
    @PostMapping
    public Result<Map<String, Long>> create(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Long op = (Long) request.getAttribute("userId");
        String name = body.get("name") != null ? body.get("name").toString() : null;
        Integer sort = null;
        if (body.get("sortOrder") != null) {
            sort = Integer.parseInt(body.get("sortOrder").toString());
        }
        Long id = newsCategoryDictService.adminCreate(op, name, sort);
        return Result.ok(Map.of("id", id));
    }

    @Operation(summary = "修改分类")
    @PutMapping("/{id}")
    public Result<Void> update(HttpServletRequest request, @Parameter(description = "分类ID") @PathVariable Long id,
                               @RequestBody Map<String, Object> body) {
        Long op = (Long) request.getAttribute("userId");
        String name = body.containsKey("name") && body.get("name") != null ? body.get("name").toString() : null;
        Integer sort = null;
        if (body.containsKey("sortOrder") && body.get("sortOrder") != null) {
            sort = Integer.parseInt(body.get("sortOrder").toString());
        }
        newsCategoryDictService.adminUpdate(op, id, name, sort);
        return Result.ok();
    }

    @Operation(summary = "删除分类", description = "仍有新闻引用时失败")
    @DeleteMapping("/{id}")
    public Result<Void> delete(HttpServletRequest request, @Parameter(description = "分类ID") @PathVariable Long id) {
        Long op = (Long) request.getAttribute("userId");
        newsCategoryDictService.adminDelete(op, id);
        return Result.ok();
    }
}
