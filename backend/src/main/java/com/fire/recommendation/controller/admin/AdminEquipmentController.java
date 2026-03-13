package com.fire.recommendation.controller.admin;

import com.fire.recommendation.common.Result;
import com.fire.recommendation.service.EquipmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Tag(name = "管理端-消防器材", description = "器材增删改、Excel 批量导入")
@RestController
@RequestMapping("/admin/equipment")
@RequiredArgsConstructor
public class AdminEquipmentController {

    private final EquipmentService equipmentService;

    @Operation(summary = "新增器材", description = "需登录。请求体 name、typeId、cover、usageSteps、checkPoints、faultSolution、summary 等")
    @PostMapping
    public Result<Map<String, Long>> save(@RequestBody Map<String, Object> body) {
        Long id = equipmentService.adminSave(body);
        return Result.ok(Map.of("id", id));
    }

    @Operation(summary = "修改器材", description = "需登录")
    @PutMapping("/{id}")
    public Result<Void> update(@Parameter(description = "器材ID") @PathVariable Long id, @RequestBody Map<String, Object> body) {
        equipmentService.adminUpdate(id, body);
        return Result.ok();
    }

    @Operation(summary = "删除器材", description = "需登录")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "器材ID") @PathVariable Long id) {
        equipmentService.adminDelete(id);
        return Result.ok();
    }

    @Operation(summary = "批量导入器材", description = "需登录。上传 Excel，约定列：名称、类型、使用步骤、检查要点、故障解决等；返回 successCount、failCount")
    @PostMapping("/import")
    public Result<Map<String, Object>> importExcel(@Parameter(description = "Excel 文件") @RequestParam("file") MultipartFile file) {
        return Result.ok(equipmentService.importExcel(file));
    }
}
