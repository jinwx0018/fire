package com.fire.recommendation.service;

import java.util.List;
import java.util.Map;

public interface StatisticsService {

    /**
     * @param startDate 可选，yyyy-MM-dd，按知识 create_time 下限（含）
     * @param endDate   可选，yyyy-MM-dd，按知识 create_time 上限（含当日）
     */
    Map<String, Object> contentStats(String startDate, String endDate);

    /**
     * @param startDate 可选；与 endDate 同时有效时，活跃用户 = 该时间段内有行为的去重用户数；均未传时仍为近 7 天
     * @param endDate   可选
     */
    Map<String, Object> userStats(String startDate, String endDate);

    /**
     * @param startDate 可选，论坛帖子/评论 create_time 筛选下限
     * @param endDate   可选
     */
    Map<String, Object> interactionStats(String startDate, String endDate);

    List<Map<String, Object>> categoryPie(String startDate, String endDate);

    List<Map<String, Object>> viewTrend(String year);

    /**
     * 区间内按月趋势：knowledgeViewSum（知识创建月口径）、behaviorViewCount（行为 VIEW 发生月口径）。
     * 均未传日期时按当年自然年。
     */
    List<Map<String, Object>> viewTrendRange(String startDate, String endDate);

    /**
     * 全站运营大盘：知识/新闻/器材/论坛/行为浏览等汇总；日期与内容统计 create_time 左闭右开语义一致，未传为全时段。
     */
    Map<String, Object> dashboard(String startDate, String endDate);

    void exportExcel(String type, String startDate, String endDate, jakarta.servlet.http.HttpServletResponse response);
}
