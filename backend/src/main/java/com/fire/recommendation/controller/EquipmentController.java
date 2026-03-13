package com.fire.recommendation.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fire.recommendation.common.Result;
import com.fire.recommendation.service.EquipmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "消防器材查询", description = "器材列表、详情、类型列表，无需登录")
@RestController
@RequestMapping("/equipment")
@RequiredArgsConstructor
public class EquipmentController {

    private final EquipmentService equipmentService;

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

    @Operation(summary = "器材详情", description = "含使用步骤、检查要点、故障解决、多图等")
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@Parameter(description = "器材ID") @PathVariable Long id) {
        return Result.ok(equipmentService.getDetail(id));
    }

    @Operation(summary = "器材类型列表", description = "返回所有器材类型 id、name")
    @GetMapping("/type/list")
    public Result<List<Map<String, Object>>> typeList() {
        return Result.ok(equipmentService.typeList());
    }
}
