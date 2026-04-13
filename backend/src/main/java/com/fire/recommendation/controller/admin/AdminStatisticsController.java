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

    @Operation(summary = "内容维度统计", description = "已发布知识(status=1)。可选 startDate、endDate(yyyy-MM-dd) 限定知识 create_time")
    @GetMapping("/content")
    public Result<Map<String, Object>> contentStats(
            @Parameter(description = "开始日期 yyyy-MM-dd") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期 yyyy-MM-dd") @RequestParam(required = false) String endDate) {
        return Result.ok(statisticsService.contentStats(startDate, endDate));
    }

    @Operation(summary = "用户维度统计", description = "角色数为未删除用户。均未传日期时活跃用户=近7天有行为去重；传了则按区间内行为去重")
    @GetMapping("/user")
    public Result<Map<String, Object>> userStats(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return Result.ok(statisticsService.userStats(startDate, endDate));
    }

    @Operation(summary = "互动维度统计", description = "可选日期筛选帖子/评论创建时间；点赞为区间内帖子的 like_count 之和")
    @GetMapping("/interaction")
    public Result<Map<String, Object>> interactionStats(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return Result.ok(statisticsService.interactionStats(startDate, endDate));
    }

    @Operation(summary = "分类发布量饼图数据", description = "与内容维度分类统计一致，可选日期")
    @GetMapping("/chart/categoryPie")
    public Result<List<Map<String, Object>>> categoryPie(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return Result.ok(statisticsService.categoryPie(startDate, endDate));
    }

    @Operation(summary = "月度浏览量折线图数据", description = "按年按月；已发布知识按创建月对 view_count 求和")
    @GetMapping("/chart/viewTrend")
    public Result<List<Map<String, Object>>> viewTrend(@Parameter(description = "年份，默认当前年") @RequestParam(required = false) String year) {
        return Result.ok(statisticsService.viewTrend(year));
    }

    @Operation(summary = "区间内按月趋势", description = "与日期区间一致（未选区间为当年）；返回 knowledgeViewSum（知识创建月 view_count 求和）、behaviorViewCount（行为 VIEW 按月次数）")
    @GetMapping("/chart/viewTrendRange")
    public Result<List<Map<String, Object>>> viewTrendRange(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return Result.ok(statisticsService.viewTrendRange(startDate, endDate));
    }

    @Operation(summary = "全站运营大盘", description = "知识/新闻/器材/论坛/行为浏览汇总；create_time 左闭右开，与内容统计一致")
    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return Result.ok(statisticsService.dashboard(startDate, endDate));
    }

    @Operation(summary = "导出统计 Excel", description = "type 为空或 all 导出全部（含运营大盘与月度趋势）；content/user/interaction 为单维度。startDate 与 endDate 须同时传入或同时不传；最长区间 3650 天")
    @GetMapping("/export")
    public void export(
            @Parameter(description = "导出类型：空/all/content/user/interaction") @RequestParam(required = false) String type,
            @Parameter(description = "开始日期 yyyy-MM-dd") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期 yyyy-MM-dd") @RequestParam(required = false) String endDate,
            HttpServletResponse response) {
        statisticsService.exportExcel(type, startDate, endDate, response);
    }
}
