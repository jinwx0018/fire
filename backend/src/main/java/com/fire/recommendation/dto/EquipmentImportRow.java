package com.fire.recommendation.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 器材批量导入 Excel 行（表头需与 @ExcelProperty 一致）。
 */
@Data
public class EquipmentImportRow {

    @ExcelProperty("名称")
    private String name;

    /** 器材类型名称，须与系统中已维护的类型一致 */
    @ExcelProperty("类型")
    private String typeName;

    @ExcelProperty("摘要")
    private String summary;

    @ExcelProperty("使用步骤")
    private String usageSteps;

    @ExcelProperty("检查要点")
    private String checkPoints;

    @ExcelProperty("故障解决")
    private String faultSolution;

    @ExcelProperty("封面URL")
    private String cover;
}
