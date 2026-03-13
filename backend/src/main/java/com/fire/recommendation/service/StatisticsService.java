package com.fire.recommendation.service;

import java.util.List;
import java.util.Map;

public interface StatisticsService {

    Map<String, Object> contentStats();

    Map<String, Object> userStats();

    Map<String, Object> interactionStats();

    List<Map<String, Object>> categoryPie();

    List<Map<String, Object>> viewTrend(String year);

    void exportExcel(String type, String startDate, String endDate, jakarta.servlet.http.HttpServletResponse response);
}
