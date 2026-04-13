package com.fire.recommendation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fire.recommendation.entity.Equipment;
import com.fire.recommendation.entity.ForumComment;
import com.fire.recommendation.entity.ForumPost;
import com.fire.recommendation.entity.KnowledgeCategory;
import com.fire.recommendation.entity.KnowledgeContent;
import com.fire.recommendation.entity.News;
import com.fire.recommendation.entity.SysRole;
import com.fire.recommendation.entity.UserBehavior;
import com.fire.recommendation.exception.BusinessException;
import com.fire.recommendation.mapper.*;
import com.fire.recommendation.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatisticsServiceImpl implements StatisticsService {

    private static final int FORUM_POST_APPROVED = 1;
    /** 防止误选超大区间拖垮数据库 */
    private static final int MAX_STAT_RANGE_DAYS = 3650;

    private final KnowledgeContentMapper contentMapper;
    private final KnowledgeCategoryMapper categoryMapper;
    private final UserBehaviorMapper behaviorMapper;
    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final ForumPostMapper postMapper;
    private final ForumCommentMapper commentMapper;
    private final NewsMapper newsMapper;
    private final EquipmentMapper equipmentMapper;

    /** 左闭右开：[startInclusive, endExclusive)，null 表示不按时间过滤 */
    private record TimeRange(LocalDateTime startInclusive, LocalDateTime endExclusive) {}

    /**
     * 解析日期区间；均空返回 null。须同时传入 startDate 与 endDate（避免单侧默认导致全表扫描或语义歧义）。
     */
    private static TimeRange parseTimeRange(String startDate, String endDate) {
        boolean hasS = StringUtils.hasText(startDate);
        boolean hasE = StringUtils.hasText(endDate);
        if (!hasS && !hasE) {
            return null;
        }
        if (hasS != hasE) {
            throw new BusinessException("startDate 与 endDate 须同时传入或同时不传");
        }
        try {
            LocalDate s = LocalDate.parse(startDate.trim());
            LocalDate e = LocalDate.parse(endDate.trim());
            if (s.isAfter(e)) {
                throw new BusinessException("开始日期不能晚于结束日期");
            }
            long spanDays = ChronoUnit.DAYS.between(s, e) + 1;
            if (spanDays > MAX_STAT_RANGE_DAYS) {
                throw new BusinessException("统计区间不能超过 " + MAX_STAT_RANGE_DAYS + " 天（约 10 年）");
            }
            return new TimeRange(s.atStartOfDay(), e.plusDays(1).atStartOfDay());
        } catch (DateTimeParseException ex) {
            throw new BusinessException("日期格式须为 yyyy-MM-dd");
        }
    }

    private static Long parseLongQuiet(Object o) {
        if (o == null) {
            return null;
        }
        try {
            if (o instanceof Number) {
                return ((Number) o).longValue();
            }
            String t = o.toString().trim();
            if (t.isEmpty()) {
                return null;
            }
            return Long.parseLong(t);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String resolveTrendYearParam(String year) {
        if (!StringUtils.hasText(year)) {
            return String.valueOf(LocalDate.now().getYear());
        }
        String t = year.trim();
        if (!t.matches("\\d{4}")) {
            throw new BusinessException("year 须为四位数字，例如 2025");
        }
        int y = Integer.parseInt(t);
        int cur = LocalDate.now().getYear();
        if (y < 1970 || y > cur + 1) {
            throw new BusinessException("year 须在 1970～" + (cur + 1) + " 之间");
        }
        return t;
    }

    private static long toLong(Object o) {
        if (o == null) {
            return 0L;
        }
        if (o instanceof Number) {
            return ((Number) o).longValue();
        }
        try {
            return Long.parseLong(o.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /**
     * 合并两个按月序列，缺月补 0。
     */
    private static List<Map<String, Object>> mergeMonthlyTrend(List<Map<String, Object>> knowledgeRows,
                                                               List<Map<String, Object>> behaviorRows) {
        TreeMap<String, long[]> merged = new TreeMap<>();
        if (knowledgeRows != null) {
            for (Map<String, Object> row : knowledgeRows) {
                if (row.get("month") == null) {
                    continue;
                }
                String m = row.get("month").toString();
                merged.computeIfAbsent(m, k -> new long[2])[0] = toLong(row.get("viewCount"));
            }
        }
        if (behaviorRows != null) {
            for (Map<String, Object> row : behaviorRows) {
                if (row.get("month") == null) {
                    continue;
                }
                String m = row.get("month").toString();
                merged.computeIfAbsent(m, k -> new long[2])[1] = toLong(row.get("cnt"));
            }
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map.Entry<String, long[]> e : merged.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("month", e.getKey());
            item.put("knowledgeViewSum", e.getValue()[0]);
            item.put("behaviorViewCount", e.getValue()[1]);
            out.add(item);
        }
        return out;
    }

    @Override
    public Map<String, Object> contentStats(String startDate, String endDate) {
        TimeRange range = parseTimeRange(startDate, endDate);
        Map<String, Object> m = new HashMap<>();
        List<Map<String, Object>> rawRows = range == null
                ? contentMapper.selectCategoryPublishCount()
                : contentMapper.selectCategoryPublishCountRange(range.startInclusive(), range.endExclusive());
        List<Map<String, Object>> categoryPublishCount = new ArrayList<>();
        for (Map<String, Object> row : rawRows) {
            Object cid = row.get("categoryId");
            if (cid == null) {
                continue;
            }
            Long categoryId = parseLongQuiet(cid);
            if (categoryId == null) {
                continue;
            }
            KnowledgeCategory cat = categoryMapper.selectById(categoryId);
            Map<String, Object> item = new HashMap<>();
            item.put("categoryId", categoryId);
            item.put("categoryName", cat != null ? cat.getName() : String.valueOf(categoryId));
            item.put("count", row.get("cnt"));
            categoryPublishCount.add(item);
        }
        m.put("categoryPublishCount", categoryPublishCount);

        LambdaQueryWrapper<KnowledgeContent> w = new LambdaQueryWrapper<KnowledgeContent>()
                .eq(KnowledgeContent::getDeleted, 0)
                .eq(KnowledgeContent::getStatus, 1)
                .select(KnowledgeContent::getId, KnowledgeContent::getTitle, KnowledgeContent::getViewCount)
                .orderByDesc(KnowledgeContent::getViewCount)
                .orderByDesc(KnowledgeContent::getId)
                .last("LIMIT 10");
        if (range != null) {
            w.ge(KnowledgeContent::getCreateTime, range.startInclusive())
                    .lt(KnowledgeContent::getCreateTime, range.endExclusive());
        }
        List<KnowledgeContent> top10 = contentMapper.selectList(w);
        List<Map<String, Object>> viewTop10 = top10.stream().map(c -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", c.getId());
            item.put("title", c.getTitle());
            item.put("viewCount", c.getViewCount());
            return item;
        }).collect(Collectors.toList());
        m.put("viewTop10", viewTop10);
        return m;
    }

    @Override
    public Map<String, Object> userStats(String startDate, String endDate) {
        TimeRange range = parseTimeRange(startDate, endDate);
        Map<String, Object> m = new HashMap<>();
        long activeUserCount;
        if (range == null) {
            LocalDateTime since = LocalDateTime.now().minusDays(7);
            activeUserCount = Optional.ofNullable(behaviorMapper.countDistinctUsersSince(since)).orElse(0L);
        } else {
            activeUserCount = Optional.ofNullable(behaviorMapper.countDistinctUsersInRange(range.startInclusive(), range.endExclusive())).orElse(0L);
        }
        m.put("activeUserCount", activeUserCount);

        List<Map<String, Object>> roleRows = userMapper.selectRoleUserCounts();
        List<Map<String, Object>> roleCount = new ArrayList<>();
        for (Map<String, Object> row : roleRows) {
            Object rid = row.get("roleId");
            if (rid == null) {
                continue;
            }
            Long roleId = parseLongQuiet(rid);
            if (roleId == null) {
                continue;
            }
            Long cntObj = parseLongQuiet(row.get("cnt"));
            long cnt = cntObj != null ? cntObj : 0L;
            SysRole role = roleMapper.selectById(roleId);
            Map<String, Object> item = new HashMap<>();
            item.put("roleId", roleId);
            item.put("roleCode", role != null ? role.getRoleCode() : String.valueOf(roleId));
            item.put("count", cnt);
            roleCount.add(item);
        }
        m.put("roleCount", roleCount);
        return m;
    }

    @Override
    public Map<String, Object> interactionStats(String startDate, String endDate) {
        TimeRange range = parseTimeRange(startDate, endDate);
        LambdaQueryWrapper<ForumPost> pq = new LambdaQueryWrapper<ForumPost>()
                .eq(ForumPost::getDeleted, 0)
                .eq(ForumPost::getStatus, FORUM_POST_APPROVED);
        if (range != null) {
            pq.ge(ForumPost::getCreateTime, range.startInclusive()).lt(ForumPost::getCreateTime, range.endExclusive());
        }
        long postCount = postMapper.selectCount(pq);
        long commentCount = range == null
                ? Optional.ofNullable(commentMapper.countOnApprovedPostsTotal()).orElse(0L)
                : Optional.ofNullable(commentMapper.countOnApprovedPostsInRange(range.startInclusive(), range.endExclusive())).orElse(0L);
        long likeCount;
        if (range == null) {
            likeCount = Optional.ofNullable(postMapper.sumLikeCountTotal()).orElse(0L);
        } else {
            likeCount = Optional.ofNullable(postMapper.sumLikeCountInRange(range.startInclusive(), range.endExclusive())).orElse(0L);
        }
        Map<String, Object> m = new HashMap<>();
        m.put("postCount", postCount);
        m.put("commentCount", commentCount);
        m.put("likeCount", likeCount);
        return m;
    }

    @Override
    public List<Map<String, Object>> categoryPie(String startDate, String endDate) {
        TimeRange range = parseTimeRange(startDate, endDate);
        List<Map<String, Object>> rawRows = range == null
                ? contentMapper.selectCategoryPublishCount()
                : contentMapper.selectCategoryPublishCountRange(range.startInclusive(), range.endExclusive());
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map<String, Object> row : rawRows) {
            Object cid = row.get("categoryId");
            if (cid == null) {
                continue;
            }
            Long categoryId = parseLongQuiet(cid);
            if (categoryId == null) {
                continue;
            }
            KnowledgeCategory cat = categoryMapper.selectById(categoryId);
            Map<String, Object> item = new HashMap<>();
            item.put("name", cat != null ? cat.getName() : String.valueOf(categoryId));
            item.put("value", row.get("cnt"));
            list.add(item);
        }
        return list;
    }

    @Override
    public List<Map<String, Object>> viewTrend(String year) {
        String y = resolveTrendYearParam(year);
        List<Map<String, Object>> rows = contentMapper.selectViewTrendByYear(y);
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new HashMap<>();
            item.put("month", row.get("month"));
            item.put("viewCount", row.get("viewCount"));
            list.add(item);
        }
        return list;
    }

    @Override
    public List<Map<String, Object>> viewTrendRange(String startDate, String endDate) {
        TimeRange range = parseTimeRange(startDate, endDate);
        if (range == null) {
            int y = LocalDate.now().getYear();
            range = new TimeRange(LocalDate.of(y, 1, 1).atStartOfDay(), LocalDate.of(y + 1, 1, 1).atStartOfDay());
        }
        List<Map<String, Object>> k = contentMapper.selectViewTrendInRange(range.startInclusive(), range.endExclusive());
        List<Map<String, Object>> b = behaviorMapper.selectViewBehaviorCountByMonth(range.startInclusive(), range.endExclusive());
        return mergeMonthlyTrend(k, b);
    }

    @Override
    public Map<String, Object> dashboard(String startDate, String endDate) {
        TimeRange range = parseTimeRange(startDate, endDate);
        Map<String, Object> m = new HashMap<>();

        LambdaQueryWrapper<KnowledgeContent> kq = new LambdaQueryWrapper<KnowledgeContent>()
                .eq(KnowledgeContent::getDeleted, 0)
                .eq(KnowledgeContent::getStatus, 1);
        if (range != null) {
            kq.ge(KnowledgeContent::getCreateTime, range.startInclusive()).lt(KnowledgeContent::getCreateTime, range.endExclusive());
        }
        m.put("knowledgePublishCount", contentMapper.selectCount(kq));

        LambdaQueryWrapper<News> nq = new LambdaQueryWrapper<News>()
                .eq(News::getDeleted, 0)
                .eq(News::getStatus, 1);
        if (range != null) {
            nq.ge(News::getCreateTime, range.startInclusive()).lt(News::getCreateTime, range.endExclusive());
        }
        m.put("newsPublishCount", newsMapper.selectCount(nq));

        QueryWrapper<News> nSum = new QueryWrapper<News>().eq("deleted", 0).eq("status", 1);
        if (range != null) {
            nSum.ge("create_time", range.startInclusive()).lt("create_time", range.endExclusive());
        }
        nSum.select("COALESCE(SUM(view_count),0) AS v");
        List<Map<String, Object>> nMaps = newsMapper.selectMaps(nSum);
        long newsViewSum = (nMaps != null && !nMaps.isEmpty()) ? toLong(nMaps.get(0).get("v")) : 0L;
        m.put("newsViewSum", newsViewSum);

        LambdaQueryWrapper<Equipment> eq = new LambdaQueryWrapper<Equipment>()
                .eq(Equipment::getDeleted, 0)
                .eq(Equipment::getStatus, 1);
        if (range != null) {
            eq.ge(Equipment::getCreateTime, range.startInclusive()).lt(Equipment::getCreateTime, range.endExclusive());
        }
        m.put("equipmentOnShelfCount", equipmentMapper.selectCount(eq));

        Map<String, Object> inter = interactionStats(startDate, endDate);
        m.put("forumPostCount", inter.get("postCount"));
        m.put("forumCommentCount", inter.get("commentCount"));
        m.put("forumLikeSum", inter.get("likeCount"));

        LambdaQueryWrapper<UserBehavior> bq = new LambdaQueryWrapper<UserBehavior>()
                .eq(UserBehavior::getBehaviorType, "VIEW");
        if (range != null) {
            bq.ge(UserBehavior::getCreateTime, range.startInclusive()).lt(UserBehavior::getCreateTime, range.endExclusive());
        }
        m.put("behaviorViewCount", behaviorMapper.selectCount(bq));

        QueryWrapper<KnowledgeContent> kSum = new QueryWrapper<KnowledgeContent>().eq("deleted", 0).eq("status", 1);
        if (range != null) {
            kSum.ge("create_time", range.startInclusive()).lt("create_time", range.endExclusive());
        }
        kSum.select("COALESCE(SUM(view_count),0) AS v");
        List<Map<String, Object>> kMaps = contentMapper.selectMaps(kSum);
        long knowledgeViewSum = (kMaps != null && !kMaps.isEmpty()) ? toLong(kMaps.get(0).get("v")) : 0L;
        m.put("knowledgeViewSum", knowledgeViewSum);

        return m;
    }

    @Override
    public void exportExcel(String type, String startDate, String endDate, HttpServletResponse response) {
        TimeRange range = parseTimeRange(startDate, endDate);
        String typeNorm = type != null ? type.trim() : "";
        boolean exportAll = !StringUtils.hasText(typeNorm) || "all".equalsIgnoreCase(typeNorm);
        if (!exportAll && !"content".equalsIgnoreCase(typeNorm) && !"user".equalsIgnoreCase(typeNorm)
                && !"interaction".equalsIgnoreCase(typeNorm)) {
            throw new BusinessException("type 须为空、all、content、user 或 interaction");
        }
        String filename = "统计导出_" + (StringUtils.hasText(typeNorm) ? typeNorm : "all") + "_" + System.currentTimeMillis() + ".xlsx";
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + URLEncoder.encode(filename, StandardCharsets.UTF_8));

            String sd = StringUtils.hasText(startDate) ? startDate.trim() : null;
            String ed = StringUtils.hasText(endDate) ? endDate.trim() : null;

            List<ExportLine> rows = new ArrayList<>();

            if (exportAll || "content".equalsIgnoreCase(typeNorm)) {
                Map<String, Object> content = contentStats(sd, ed);
                addExportRow(rows, "【内容维度】", "");
                addExportRow(rows, "说明",
                        range == null ? "全时段已发布知识" : "限定创建时间在所选区间");
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> catList = (List<Map<String, Object>>) content.get("categoryPublishCount");
                if (catList != null) {
                    for (Map<String, Object> r : catList) {
                        addExportRow(rows,
                                "分类发布量 · " + nullToEmpty(r.get("categoryName")),
                                nullToEmpty(r.get("count")));
                    }
                }
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> top10 = (List<Map<String, Object>>) content.get("viewTop10");
                if (top10 != null) {
                    int rank = 1;
                    for (Map<String, Object> r : top10) {
                        addExportRow(rows,
                                "浏览Top" + rank + " · " + nullToEmpty(r.get("title")),
                                nullToEmpty(r.get("viewCount")));
                        rank++;
                    }
                }
            }
            if (exportAll || "user".equalsIgnoreCase(typeNorm)) {
                Map<String, Object> user = userStats(sd, ed);
                String activeLabel = range == null ? "活跃用户数(近7天有行为)" : "活跃用户数(区间内去重)";
                addExportRow(rows, "【用户维度】", "");
                addExportRow(rows, activeLabel, nullToEmpty(user.get("activeUserCount")));
                addExportRow(rows, "说明", "角色数为未删除用户");
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> roleList = (List<Map<String, Object>>) user.get("roleCount");
                if (roleList != null) {
                    for (Map<String, Object> r : roleList) {
                        addExportRow(rows,
                                "角色 · " + nullToEmpty(r.get("roleCode")),
                                nullToEmpty(r.get("count")));
                    }
                }
            }
            if (exportAll || "interaction".equalsIgnoreCase(typeNorm)) {
                Map<String, Object> inter = interactionStats(sd, ed);
                addExportRow(rows, "【互动维度】", "");
                addExportRow(rows, "论坛帖子数", nullToEmpty(inter.get("postCount")));
                addExportRow(rows, "论坛评论数", nullToEmpty(inter.get("commentCount")));
                addExportRow(rows, "论坛点赞合计(已通过帖)", nullToEmpty(inter.get("likeCount")));
            }

            if (exportAll) {
                addExportRow(rows, "【运营大盘】", "与统计页一致；论坛为已通过帖子及下属评论");
                Map<String, Object> dash = dashboard(sd, ed);
                addExportRow(rows, "知识发布条数", nullToEmpty(dash.get("knowledgePublishCount")));
                addExportRow(rows, "知识浏览合计", nullToEmpty(dash.get("knowledgeViewSum")));
                addExportRow(rows, "新闻发布条数", nullToEmpty(dash.get("newsPublishCount")));
                addExportRow(rows, "新闻浏览合计", nullToEmpty(dash.get("newsViewSum")));
                addExportRow(rows, "上架器材条数", nullToEmpty(dash.get("equipmentOnShelfCount")));
                addExportRow(rows, "行为VIEW次数", nullToEmpty(dash.get("behaviorViewCount")));
                addExportRow(rows, "论坛帖子(已通过)", nullToEmpty(dash.get("forumPostCount")));
                addExportRow(rows, "论坛评论(帖已通过)", nullToEmpty(dash.get("forumCommentCount")));
                addExportRow(rows, "论坛点赞合计", nullToEmpty(dash.get("forumLikeSum")));
                addExportRow(rows, "【月度趋势】", "知识view按月汇总 / 行为VIEW按月");
                for (Map<String, Object> tr : viewTrendRange(sd, ed)) {
                    addExportRow(rows,
                            "月份 · " + nullToEmpty(tr.get("month")),
                            "知识view:" + nullToEmpty(tr.get("knowledgeViewSum"))
                                    + "；行为VIEW:" + nullToEmpty(tr.get("behaviorViewCount")));
                }
            }

            com.alibaba.excel.EasyExcel.write(response.getOutputStream())
                    .head(ExportLine.class)
                    .sheet("统计数据")
                    .doWrite(rows);
        } catch (IOException e) {
            log.error("导出Excel失败", e);
            throw new BusinessException("导出失败，请稍后重试");
        }
    }

    private static void addExportRow(List<ExportLine> rows, String col1, String col2) {
        rows.add(new ExportLine(col1 == null ? "" : col1, col2 == null ? "" : col2));
    }

    private static String nullToEmpty(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    private static class ExportLine {
        @com.alibaba.excel.annotation.ExcelProperty("项目")
        private String item;
        @com.alibaba.excel.annotation.ExcelProperty("内容")
        private String content;
    }
}
