package com.fire.recommendation.controller.admin;

import com.fire.recommendation.common.Result;
import com.fire.recommendation.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "管理端-数据统计与分析", description = "内容/用户/互动维度统计、图表数据、Excel 导出")
@RestController
@RequestMapping("/admin/statistics")
@RequiredArgsConstructor
public class AdminStatisticsController {

    private final StatisticsService statisticsService;

    @Operation(summary = "内容维度统计", description = "需登录。分类发布量、浏览量 Top10")
    @GetMapping("/content")
    public Result<Map<String, Object>> contentStats() {
        return Result.ok(statisticsService.contentStats());
    }

    @Operation(summary = "用户维度统计", description = "需登录。近 7 天活跃用户数、各角色用户数")
    @GetMapping("/user")
    public Result<Map<String, Object>> userStats() {
        return Result.ok(statisticsService.userStats());
    }

    @Operation(summary = "互动维度统计", description = "需登录。帖子数、评论数、点赞数")
    @GetMapping("/interaction")
    public Result<Map<String, Object>> interactionStats() {
        return Result.ok(statisticsService.interactionStats());
    }

    @Operation(summary = "分类发布量饼图数据", description = "需登录。返回 [{name, value}] 供 ECharts 饼图")
    @GetMapping("/chart/categoryPie")
    public Result<List<Map<String, Object>>> categoryPie() {
        return Result.ok(statisticsService.categoryPie());
    }

    @Operation(summary = "月度浏览量折线图数据", description = "需登录。按年按月汇总，year 默认当前年；返回 [{month, viewCount}]")
    @GetMapping("/chart/viewTrend")
    public Result<List<Map<String, Object>>> viewTrend(@Parameter(description = "年份，默认当前年") @RequestParam(required = false) String year) {
        return Result.ok(statisticsService.viewTrend(year));
    }

    @Operation(summary = "导出统计 Excel", description = "需登录。type=content/user/interaction 可选；startDate、endDate 可选；响应为文件流")
    @GetMapping("/export")
    public void export(
            @Parameter(description = "导出类型：content/user/interaction") @RequestParam(required = false) String type,
            @Parameter(description = "开始日期") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) String endDate,
            HttpServletResponse response) {
        statisticsService.exportExcel(type, startDate, endDate, response);
    }
}
