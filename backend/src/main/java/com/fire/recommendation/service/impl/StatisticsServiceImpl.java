package com.fire.recommendation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fire.recommendation.entity.*;
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
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatisticsServiceImpl implements StatisticsService {

    private final KnowledgeContentMapper contentMapper;
    private final KnowledgeCategoryMapper categoryMapper;
    private final UserBehaviorMapper behaviorMapper;
    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final ForumPostMapper postMapper;
    private final ForumCommentMapper commentMapper;

    @Override
    public Map<String, Object> contentStats() {
        Map<String, Object> m = new HashMap<>();
        List<Map<String, Object>> categoryPublishCount = new ArrayList<>();
        for (Map<String, Object> row : contentMapper.selectCategoryPublishCount()) {
            Object cid = row.get("categoryId");
            if (cid == null) continue;
            Long categoryId = cid instanceof Number ? ((Number) cid).longValue() : Long.parseLong(cid.toString());
            KnowledgeCategory cat = categoryMapper.selectById(categoryId);
            Map<String, Object> item = new HashMap<>();
            item.put("categoryId", categoryId);
            item.put("categoryName", cat != null ? cat.getName() : String.valueOf(categoryId));
            item.put("count", row.get("cnt"));
            categoryPublishCount.add(item);
        }
        m.put("categoryPublishCount", categoryPublishCount);

        List<KnowledgeContent> top10 = contentMapper.selectList(
                new LambdaQueryWrapper<KnowledgeContent>()
                        .eq(KnowledgeContent::getDeleted, 0)
                        .eq(KnowledgeContent::getStatus, 1)
                        .select(KnowledgeContent::getId, KnowledgeContent::getTitle, KnowledgeContent::getViewCount)
                        .orderByDesc(KnowledgeContent::getViewCount)
                        .last("LIMIT 10"));
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
    public Map<String, Object> userStats() {
        Map<String, Object> m = new HashMap<>();
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        long activeUserCount = behaviorMapper.selectList(
                        new LambdaQueryWrapper<UserBehavior>().ge(UserBehavior::getCreateTime, since))
                .stream().map(UserBehavior::getUserId).distinct().count();
        m.put("activeUserCount", activeUserCount);

        List<SysUser> users = userMapper.selectList(null);
        Map<Long, Long> roleIdCount = users.stream()
                .filter(u -> u.getRoleId() != null)
                .collect(Collectors.groupingBy(SysUser::getRoleId, Collectors.counting()));
        List<Map<String, Object>> roleCount = new ArrayList<>();
        for (Map.Entry<Long, Long> e : roleIdCount.entrySet()) {
            SysRole role = roleMapper.selectById(e.getKey());
            Map<String, Object> item = new HashMap<>();
            item.put("roleId", e.getKey());
            item.put("roleCode", role != null ? role.getRoleCode() : String.valueOf(e.getKey()));
            item.put("count", e.getValue());
            roleCount.add(item);
        }
        m.put("roleCount", roleCount);
        return m;
    }

    @Override
    public Map<String, Object> interactionStats() {
        Map<String, Object> m = new HashMap<>();
        long postCount = postMapper.selectCount(new LambdaQueryWrapper<ForumPost>().eq(ForumPost::getDeleted, 0));
        long commentCount = commentMapper.selectCount(new LambdaQueryWrapper<ForumComment>().eq(ForumComment::getDeleted, 0));
        List<ForumPost> posts = postMapper.selectList(
                new LambdaQueryWrapper<ForumPost>().eq(ForumPost::getDeleted, 0).select(ForumPost::getLikeCount));
        int likeCount = posts.stream().mapToInt(p -> p.getLikeCount() != null ? p.getLikeCount() : 0).sum();
        m.put("postCount", postCount);
        m.put("commentCount", commentCount);
        m.put("likeCount", likeCount);
        return m;
    }

    @Override
    public List<Map<String, Object>> categoryPie() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map<String, Object> row : contentMapper.selectCategoryPublishCount()) {
            Object cid = row.get("categoryId");
            if (cid == null) continue;
            Long categoryId = cid instanceof Number ? ((Number) cid).longValue() : Long.parseLong(cid.toString());
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
        String y = StringUtils.hasText(year) ? year : String.valueOf(LocalDateTime.now().getYear());
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
    public void exportExcel(String type, String startDate, String endDate, HttpServletResponse response) {
        String filename = "统计导出_" + (StringUtils.hasText(type) ? type : "all") + "_" + System.currentTimeMillis() + ".xlsx";
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + URLEncoder.encode(filename, StandardCharsets.UTF_8));

            List<Map<String, String>> rows = new ArrayList<>();
            if (!StringUtils.hasText(type) || "content".equalsIgnoreCase(type)) {
                Map<String, Object> content = contentStats();
                rows.add(Map.of("维度", "内容", "项", "分类发布量/浏览量Top10", "说明", "见下方列表"));
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> catList = (List<Map<String, Object>>) content.get("categoryPublishCount");
                if (catList != null) for (Map<String, Object> r : catList) {
                    Map<String, String> row = new HashMap<>();
                    row.put("分类", String.valueOf(r.get("categoryName")));
                    row.put("发布量", String.valueOf(r.get("count")));
                    rows.add(row);
                }
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> top10 = (List<Map<String, Object>>) content.get("viewTop10");
                if (top10 != null) for (Map<String, Object> r : top10) {
                    Map<String, String> row = new HashMap<>();
                    row.put("标题", String.valueOf(r.get("title")));
                    row.put("浏览量", String.valueOf(r.get("viewCount")));
                    rows.add(row);
                }
            }
            if (!StringUtils.hasText(type) || "user".equalsIgnoreCase(type)) {
                Map<String, Object> user = userStats();
                rows.add(Map.of("维度", "用户", "活跃用户数(近7天)", String.valueOf(user.get("activeUserCount")), "说明", ""));
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> roleList = (List<Map<String, Object>>) user.get("roleCount");
                if (roleList != null) for (Map<String, Object> r : roleList) {
                    Map<String, String> row = new HashMap<>();
                    row.put("角色", String.valueOf(r.get("roleCode")));
                    row.put("用户数", String.valueOf(r.get("count")));
                    rows.add(row);
                }
            }
            if (!StringUtils.hasText(type) || "interaction".equalsIgnoreCase(type)) {
                Map<String, Object> inter = interactionStats();
                rows.add(Map.of("维度", "互动", "帖子数", String.valueOf(inter.get("postCount")),
                        "评论数", String.valueOf(inter.get("commentCount")), "点赞数", String.valueOf(inter.get("likeCount"))));
            }

            com.alibaba.excel.EasyExcel.write(response.getOutputStream())
                    .sheet("统计数据")
                    .doWrite(rows);
        } catch (IOException e) {
            log.error("导出Excel失败", e);
            throw new RuntimeException("导出失败");
        }
    }
}
