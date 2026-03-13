package com.fire.recommendation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fire.recommendation.common.PageResult;
import com.fire.recommendation.entity.KnowledgeContent;
import com.fire.recommendation.entity.KnowledgeCategory;
import com.fire.recommendation.entity.UserBehavior;
import com.fire.recommendation.mapper.KnowledgeCategoryMapper;
import com.fire.recommendation.mapper.KnowledgeContentMapper;
import com.fire.recommendation.mapper.UserBehaviorMapper;
import com.fire.recommendation.service.AiRecommendService;
import com.fire.recommendation.service.RecommendService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendServiceImpl implements RecommendService {

    private final UserBehaviorMapper behaviorMapper;
    private final KnowledgeContentMapper contentMapper;
    private final KnowledgeCategoryMapper categoryMapper;
    private final AiRecommendService aiRecommendService;

    /** 是否启用基于物品的协同过滤（Item-CF）；为 false 时沿用“兴趣分类 Top1 + 热度”规则 */
    @Value("${recommend.use-item-cf:true}")
    private boolean useItemCf;

    /** Item-CF：取用户最近行为过的内容数量，用于找相似物品 */
    @Value("${recommend.item-cf.user-history-limit:30}")
    private int userHistoryLimit;

    /** Item-CF：每个内容取相似内容的数量 */
    @Value("${recommend.item-cf.similar-per-item:20}")
    private int similarPerItem;

    /** 启用 AI 时，召回候选数量（供 AI 打分/重排后再分页） */
    @Value("${recommend.ai.recall-size:50}")
    private int aiRecallSize;

    @Override
    public void recordBehavior(Long userId, String behaviorType, String targetType, Long targetId) {
        if (userId == null) {
            return;
        }
        UserBehavior b = new UserBehavior();
        b.setUserId(userId);
        b.setBehaviorType(behaviorType);
        b.setTargetType(targetType);
        b.setTargetId(targetId);
        behaviorMapper.insert(b);
    }

    @Override
    public PageResult<Map<String, Object>> recommendList(Long userId, Integer pageNum, Integer pageSize) {
        int pn = pageNum != null ? pageNum : 1;
        int ps = pageSize != null ? pageSize : 10;
        boolean aiEnabled = aiRecommendService.isAiRecommendEnabled();
        boolean hasBehavior = userId != null && behaviorMapper.selectCount(
                new LambdaQueryWrapper<UserBehavior>().eq(UserBehavior::getUserId, userId)) > 0;

        // 启用 AI 时：先召回更多候选，AI 重排后再分页（选品+排序由 AI 参与）
        if (aiEnabled) {
            List<Map<String, Object>> candidates = getRecallCandidates(userId, hasBehavior, aiRecallSize);
            if (!candidates.isEmpty()) {
                List<Map<String, Object>> reordered = aiRecommendService.reorderByAi(userId, candidates);
                long total = reordered.size();
                int from = (pn - 1) * ps;
                if (from >= total) {
                    aiRecommendService.enrichRecommendReasons(userId, Collections.emptyList());
                    return PageResult.of(Collections.emptyList(), total, pn, ps);
                }
                int to = (int) Math.min(from + ps, total);
                List<Map<String, Object>> pageList = new ArrayList<>(reordered.subList(from, to));
                aiRecommendService.enrichRecommendReasons(userId, pageList);
                return PageResult.of(pageList, total, pn, ps);
            }
        }

        // 未启用 AI 或召回为空：沿用原逻辑（Item-CF / 规则 / 冷启动，按页查 DB）
        List<KnowledgeContent> list;
        long total;

        if (hasBehavior && useItemCf) {
            PageResult<Map<String, Object>> cfResult = recommendByItemCF(userId, pn, ps);
            if (cfResult != null) {
                return cfResult;
            }
        }

        if (hasBehavior) {
            Long topCategoryId = findUserTop1CategoryId(userId);
            LambdaQueryWrapper<KnowledgeContent> q = new LambdaQueryWrapper<KnowledgeContent>()
                    .eq(KnowledgeContent::getDeleted, 0)
                    .eq(KnowledgeContent::getStatus, 1)
                    .orderByDesc(KnowledgeContent::getViewCount)
                    .orderByDesc(KnowledgeContent::getLikeCount);
            if (topCategoryId != null) {
                q.eq(KnowledgeContent::getCategoryId, topCategoryId);
            }
            Page<KnowledgeContent> page = contentMapper.selectPage(new Page<>(pn, ps), q);
            list = page.getRecords();
            total = contentMapper.selectCount(q);
        } else {
            LambdaQueryWrapper<KnowledgeContent> q = new LambdaQueryWrapper<KnowledgeContent>()
                    .eq(KnowledgeContent::getDeleted, 0)
                    .eq(KnowledgeContent::getStatus, 1)
                    .orderByDesc(KnowledgeContent::getViewCount)
                    .orderByDesc(KnowledgeContent::getLikeCount);
            Page<KnowledgeContent> page = contentMapper.selectPage(new Page<>(pn, ps), q);
            list = page.getRecords();
            total = contentMapper.selectCount(q);
        }
        List<Map<String, Object>> result = list.stream().map(this::contentToMap).collect(Collectors.toList());
        aiRecommendService.enrichRecommendReasons(userId, result);
        return PageResult.of(result, total, pn, ps);
    }

    /**
     * 召回候选列表（供 AI 重排或直接分页）。有行为时优先 Item-CF，不足则兴趣分类+热度；无行为则全站热门。
     * @param maxSize 最多返回条数（如 AI 召回量）
     */
    private List<Map<String, Object>> getRecallCandidates(Long userId, boolean hasBehavior, int maxSize) {
        if (hasBehavior && useItemCf) {
            List<Long> sortedIds = getItemCFSortedIds(userId);
            if (sortedIds != null && !sortedIds.isEmpty()) {
                int take = Math.min(maxSize, sortedIds.size());
                List<Long> ids = sortedIds.subList(0, take);
                List<KnowledgeContent> contents = contentMapper.selectBatchIds(ids);
                Map<Long, KnowledgeContent> byId = contents.stream().collect(Collectors.toMap(KnowledgeContent::getId, c -> c));
                List<Map<String, Object>> out = new ArrayList<>();
                for (Long id : ids) {
                    KnowledgeContent c = byId.get(id);
                    if (c != null && c.getDeleted() != null && c.getDeleted() == 0 && c.getStatus() != null && c.getStatus() == 1) {
                        out.add(contentToMap(c));
                    }
                }
                return out;
            }
        }
        LambdaQueryWrapper<KnowledgeContent> q = new LambdaQueryWrapper<KnowledgeContent>()
                .eq(KnowledgeContent::getDeleted, 0)
                .eq(KnowledgeContent::getStatus, 1)
                .orderByDesc(KnowledgeContent::getViewCount)
                .orderByDesc(KnowledgeContent::getLikeCount);
        if (hasBehavior) {
            Long topCategoryId = findUserTop1CategoryId(userId);
            if (topCategoryId != null) {
                q.eq(KnowledgeContent::getCategoryId, topCategoryId);
            }
        }
        Page<KnowledgeContent> page = contentMapper.selectPage(new Page<>(1, maxSize), q);
        return page.getRecords().stream().map(this::contentToMap).collect(Collectors.toList());
    }

    /** Item-CF 仅计算排序后的 contentId 列表（不查库、不分页） */
    private List<Long> getItemCFSortedIds(Long userId) {
        List<UserBehavior> behaviors = behaviorMapper.selectList(
                new LambdaQueryWrapper<UserBehavior>()
                        .eq(UserBehavior::getUserId, userId)
                        .eq(UserBehavior::getTargetType, "CONTENT")
                        .orderByDesc(UserBehavior::getCreateTime)
                        .last("LIMIT " + userHistoryLimit));
        if (CollectionUtils.isEmpty(behaviors)) return null;
        Set<Long> userContentIds = behaviors.stream().map(UserBehavior::getTargetId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (userContentIds.isEmpty()) return null;
        Map<Long, Long> candidateScores = new HashMap<>();
        for (Long contentId : userContentIds) {
            List<Map<String, Object>> similar = behaviorMapper.findSimilarContentIds(contentId, similarPerItem);
            for (Map<String, Object> row : similar) {
                Object idObj = row.get("contentId");
                Object scoreObj = row.get("score");
                if (idObj == null || scoreObj == null) continue;
                Long cid = idObj instanceof Number ? ((Number) idObj).longValue() : Long.parseLong(idObj.toString());
                long score = ((Number) scoreObj).longValue();
                if (!userContentIds.contains(cid)) {
                    candidateScores.merge(cid, score, Long::sum);
                }
            }
        }
        if (candidateScores.isEmpty()) return null;
        return candidateScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * 基于物品的协同过滤（Item-CF）：与用户看过/收藏过的内容“相似”的内容（被同一批用户行为过）按共现得分排序推荐。
     * 相似度 = 共现用户数（多少用户同时行为过这两个内容）。
     */
    private PageResult<Map<String, Object>> recommendByItemCF(Long userId, int pageNum, int pageSize) {
        List<UserBehavior> behaviors = behaviorMapper.selectList(
                new LambdaQueryWrapper<UserBehavior>()
                        .eq(UserBehavior::getUserId, userId)
                        .eq(UserBehavior::getTargetType, "CONTENT")
                        .orderByDesc(UserBehavior::getCreateTime)
                        .last("LIMIT " + userHistoryLimit));
        if (CollectionUtils.isEmpty(behaviors)) {
            return null;
        }
        Set<Long> userContentIds = behaviors.stream().map(UserBehavior::getTargetId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (userContentIds.isEmpty()) {
            return null;
        }

        // 对用户行为过的每个内容，查相似内容（共现用户数作为得分），聚合去重并累加得分
        Map<Long, Long> candidateScores = new HashMap<>();
        for (Long contentId : userContentIds) {
            List<Map<String, Object>> similar = behaviorMapper.findSimilarContentIds(contentId, similarPerItem);
            for (Map<String, Object> row : similar) {
                Object idObj = row.get("contentId");
                Object scoreObj = row.get("score");
                if (idObj == null || scoreObj == null) continue;
                Long cid = idObj instanceof Number ? ((Number) idObj).longValue() : Long.parseLong(idObj.toString());
                long score = ((Number) scoreObj).longValue();
                if (!userContentIds.contains(cid)) {
                    candidateScores.merge(cid, score, Long::sum);
                }
            }
        }
        if (candidateScores.isEmpty()) {
            return null;
        }

        // 按得分降序，取 contentId 列表（保持顺序用于分页）
        List<Long> sortedIds = candidateScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        int total = sortedIds.size();
        int from = (pageNum - 1) * pageSize;
        if (from >= total) {
            return PageResult.of(Collections.emptyList(), (long) total, pageNum, pageSize);
        }
        int to = Math.min(from + pageSize, total);
        List<Long> pageIds = sortedIds.subList(from, to);

        List<KnowledgeContent> contents = contentMapper.selectBatchIds(pageIds);
        if (contents.isEmpty()) {
            return PageResult.of(Collections.emptyList(), (long) total, pageNum, pageSize);
        }
        // 按 pageIds 顺序排列（selectBatchIds 不保证顺序）
        Map<Long, KnowledgeContent> byId = contents.stream().collect(Collectors.toMap(KnowledgeContent::getId, c -> c));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Long id : pageIds) {
            KnowledgeContent c = byId.get(id);
            if (c != null && c.getDeleted() != null && c.getDeleted() == 0 && c.getStatus() != null && c.getStatus() == 1) {
                result.add(contentToMap(c));
            }
        }
        aiRecommendService.enrichRecommendReasons(userId, result);
        return PageResult.of(result, (long) total, pageNum, pageSize);
    }

    /** 统计用户行为中 CONTENT 类型涉及的知识所属分类，取出现次数最多的分类 ID（Top1） */
    private Long findUserTop1CategoryId(Long userId) {
        List<UserBehavior> behaviors = behaviorMapper.selectList(
                new LambdaQueryWrapper<UserBehavior>()
                        .eq(UserBehavior::getUserId, userId)
                        .eq(UserBehavior::getTargetType, "CONTENT")
                        .orderByDesc(UserBehavior::getCreateTime)
                        .last("LIMIT 200"));
        if (behaviors.isEmpty()) return null;
        List<Long> contentIds = behaviors.stream().map(UserBehavior::getTargetId).distinct().toList();
        if (contentIds.isEmpty()) return null;
        List<KnowledgeContent> contents = contentMapper.selectBatchIds(contentIds);
        Map<Long, Long> categoryCount = new HashMap<>();
        for (KnowledgeContent c : contents) {
            if (c.getCategoryId() != null) {
                categoryCount.merge(c.getCategoryId(), 1L, Long::sum);
            }
        }
        return categoryCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private Map<String, Object> contentToMap(KnowledgeContent c) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", c.getId());
        m.put("title", c.getTitle());
        m.put("categoryId", c.getCategoryId());
        KnowledgeCategory cat = categoryMapper.selectById(c.getCategoryId());
        m.put("categoryName", cat != null ? cat.getName() : null);
        m.put("cover", c.getCover());
        m.put("summary", c.getSummary());
        m.put("viewCount", c.getViewCount());
        m.put("likeCount", c.getLikeCount());
        m.put("createTime", c.getCreateTime());
        return m;
    }
}
