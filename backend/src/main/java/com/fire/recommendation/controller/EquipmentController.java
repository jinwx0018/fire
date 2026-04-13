package com.fire.recommendation.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fire.recommendation.common.Result;
import com.fire.recommendation.service.EquipmentService;
import com.fire.recommendation.service.UserCollectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@Tag(name = "消防器材查询", description = "器材列表、详情、类型列表，无需登录")
@RestController
@RequestMapping("/equipment")
@RequiredArgsConstructor
public class EquipmentController {

    private final EquipmentService equipmentService;
    private final UserCollectionService userCollectionService;

    @Operation(summary = "器材分页列表", description = "支持名称模糊、类型筛选")
    @GetMapping("/list")
    public Result<IPage<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long typeId) {
        IPage<Map<String, Object>> page = equipmentService.list(pageNum, pageSize, keyword, typeId);
        return Result.ok(page);
    }

    @Operation(summary = "器材详情", description = "含使用步骤、检查要点、故障解决、多图等；带 Token 时返回 collected")
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@Parameter(description = "器材ID") @PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.ok(equipmentService.getDetail(id, userId));
    }

    @Operation(summary = "收藏器材", description = "需登录")
    @PostMapping("/{id}/collect")
    public Result<Map<String, Boolean>> collect(@Parameter(description = "器材ID") @PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        userCollectionService.collectEquipment(userId, id);
        return Result.ok(Map.of("collected", true));
    }

    @Operation(summary = "取消收藏器材", description = "需登录")
    @DeleteMapping("/{id}/collect")
    public Result<Void> uncollect(@Parameter(description = "器材ID") @PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        userCollectionService.uncollectEquipment(userId, id);
        return Result.ok();
    }

    @Operation(summary = "器材类型列表", description = "返回所有器材类型 id、name")
    @GetMapping("/type/list")
    public Result<List<Map<String, Object>>> typeList() {
        return Result.ok(equipmentService.typeList());
    }
}
