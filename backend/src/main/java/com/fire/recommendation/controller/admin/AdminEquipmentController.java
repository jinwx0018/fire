package com.fire.recommendation.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
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

    @Operation(summary = "器材分页（管理端）", description = "含未上架记录；status 空=全部，1=已上架，0=未上架")
    @GetMapping("/page")
    public Result<IPage<Map<String, Object>>> adminPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long typeId,
            @RequestParam(required = false) Integer status) {
        return Result.ok(equipmentService.adminPage(pageNum, pageSize, keyword, typeId, status));
    }

    @Operation(summary = "器材详情（管理端）", description = "含未上架记录，供编辑表单使用")
    @GetMapping("/{id}")
    public Result<Map<String, Object>> adminDetail(@Parameter(description = "器材ID") @PathVariable Long id) {
        return Result.ok(equipmentService.adminGetDetail(id));
    }

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

    @Operation(summary = "批量导入器材", description = "需登录。上传 Excel，表头：名称、类型、摘要、使用步骤、检查要点、故障解决、封面URL（类型为系统中类型名称）")
    @PostMapping("/import")
    public Result<Map<String, Object>> importExcel(@Parameter(description = "Excel 文件") @RequestParam("file") MultipartFile file) {
        return Result.ok(equipmentService.importExcel(file));
    }

    @Operation(summary = "器材类型分页（管理端）", description = "按 sort、id 升序")
    @GetMapping("/type/page")
    public Result<IPage<Map<String, Object>>> typePage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.ok(equipmentService.adminTypePage(pageNum, pageSize));
    }

    @Operation(summary = "新增器材类型", description = "需登录。body: name、sort(可选)")
    @PostMapping("/type")
    public Result<Map<String, Long>> saveType(@RequestBody Map<String, Object> body) {
        Long id = equipmentService.adminSaveType(body);
        return Result.ok(Map.of("id", id));
    }

    @Operation(summary = "修改器材类型", description = "需登录")
    @PutMapping("/type/{id}")
    public Result<Void> updateType(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        equipmentService.adminUpdateType(id, body);
        return Result.ok();
    }

    @Operation(summary = "删除器材类型", description = "需登录。若仍有器材引用则失败")
    @DeleteMapping("/type/{id}")
    public Result<Void> deleteType(@PathVariable Long id) {
        equipmentService.adminDeleteType(id);
        return Result.ok();
    }
}
