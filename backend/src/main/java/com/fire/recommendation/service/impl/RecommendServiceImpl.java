package com.fire.recommendation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fire.recommendation.common.PageResult;
import com.fire.recommendation.dto.AiRecommendContext;
import com.fire.recommendation.entity.KnowledgeCategory;
import com.fire.recommendation.entity.KnowledgeContent;
import com.fire.recommendation.entity.UserBehavior;
import com.fire.recommendation.exception.BusinessException;
import com.fire.recommendation.mapper.KnowledgeCategoryMapper;
import com.fire.recommendation.mapper.KnowledgeContentMapper;
import com.fire.recommendation.mapper.UserBehaviorMapper;
import com.fire.recommendation.service.AiRecommendService;
import com.fire.recommendation.service.RecommendService;
import com.fire.recommendation.service.UserKnowledgeCategoryBlockService;
import com.fire.recommendation.util.TextSimilarityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 智能推荐：先筛选合法上架内容（可配置排除分类），可选「多路召回」合并类目/CF/热度候选池，再「精排」按
 * 「偏好分类(行为加权) + Item-CF + 热度/时效 + 轻量语义(近期浏览) + 论坛跨域」综合打分排序；
 * 启用 AI 时仅对前若干条做模型微调排序。按时间/热门仍走内容列表接口。
 * <p>
 * <strong>未登录</strong>仅冷启动：无个人行为画像，不使用 Item-CF、语义与论坛加成，且<strong>不调用</strong> AI 重排与理由；
 * <strong>登录后</strong>才启用完整个性化与可选 AI。
 * <p>
 * 与「用户协同 + 皮尔逊相关」类教程代码的差异（本系统采用更适配隐式反馈的方案）：<br>
 * 1) 使用 <strong>基于物品的协同（Item-CF）</strong>：通过「与当前用户历史内容共同被其他用户行为过」的共现强度打分，而非找最相似用户再取其评分列表。<br>
 * 2) 行为是 VIEW/LIKE/COLLECT 等<strong>隐式反馈</strong>，用加权 + 时间衰减代替显式评分矩阵上的皮尔逊。<br>
 * 3) 最终排序是<strong>多因子线性加权</strong>（类目偏好 + CF + 热度/时效 + 语义等），不是纯 CF 列表。<br>
 * 将教程中的 User-CF+Pearson 原样搬入需构造显式评分、且 TreeMap&lt;相似度,用户&gt; 在分数重复时会覆盖键，不宜直接套用。
 */
@Service
@RequiredArgsConstructor
public class RecommendServiceImpl implements RecommendService {

    private static final Set<String> ALLOWED_BEHAVIOR = Set.of("VIEW", "LIKE", "COMMENT", "COLLECT");
    private static final Set<String> ALLOWED_TARGET = Set.of("CONTENT", "FORUM_POST", "POST",
            "FORUM_COMMENT", "NEWS_COMMENT", "KNOWLEDGE_COMMENT", "NEWS", "EQUIPMENT");

    private final UserBehaviorMapper behaviorMapper;
    private final KnowledgeContentMapper contentMapper;
    private final KnowledgeCategoryMapper categoryMapper;
    private final AiRecommendService aiRecommendService;
    private final UserKnowledgeCategoryBlockService userKnowledgeCategoryBlockService;

    @Value("${recommend.use-item-cf:true}")
    private boolean useItemCf;

    @Value("${recommend.item-cf.user-history-limit:30}")
    private int userHistoryLimit;

    @Value("${recommend.item-cf.similar-per-item:20}")
    private int similarPerItem;

    /** 启用 AI 时，仅将排序后前 N 条交给大模型微调，避免超长 prompt；其余条目仍参与列表，排在 N 条之后 */
    @Value("${recommend.ai.rerank-pool-size:40}")
    private int aiRerankPoolSize;

    /** 单次智能排序最多加载的知识条数；0 表示不限制（数据量大时请在生产环境设上限，如 3000） */
    @Value("${recommend.smart-max-pool:0}")
    private int smartMaxPool;

    /** 逗号分隔 categoryId，从智能推荐候选中排除（如运营暂不推的分类） */
    @Value("${recommend.exclude-category-ids:}")
    private String excludeCategoryIdsConfig;

    @Value("${ai.recommend.hot-topics:}")
    private String hotTopicsConfig;

    @Value("${recommend.keyword-max-length:64}")
    private int keywordMaxLength;

    @Value("${recommend.cold-start.default-category-ids:}")
    private String coldStartCategoryIdsConfig;

    @Value("${recommend.semantic-similarity.enabled:true}")
    private boolean semanticSimilarityEnabled;

    @Value("${recommend.semantic-similarity.recent-view-limit:8}")
    private int semanticRecentViewLimit;

    @Value("${recommend.semantic-similarity.max-boost:420}")
    private double semanticMaxBoost;

    @Value("${recommend.forum-cross-domain.enabled:true}")
    private boolean forumCrossDomainEnabled;

    @Value("${recommend.forum-cross-domain.title-limit:12}")
    private int forumCrossTitleLimit;

    @Value("${recommend.forum-cross-domain.max-boost:200}")
    private double forumCrossMaxBoost;

    /**
     * 两阶段之「召回」：合并偏好类目最新、Item-CF 高分候选、全站热度暖池，控制参与精排规模（0 或关闭则仍拉全量再截断，与旧版一致）。
     */
    @Value("${recommend.recall.enabled:true}")
    private boolean recallEnabled;

    @Value("${recommend.recall.max-candidates:400}")
    private int recallMaxCandidates;

    @Value("${recommend.recall.per-category-limit:90}")
    private int recallPerCategoryLimit;

    @Value("${recommend.recall.cf-id-limit:150}")
    private int recallCfIdLimit;

    @Value("${recommend.recall.hot-batch-limit:120}")
    private int recallHotBatchLimit;

    /** 精排：热度 log 权重、时效衰减、类目阶梯加分（与论文 Score≈W_cat·match+W_hot·log(view)+W_time·decay 对应，尺度保持与历史数据可比） */
    @Value("${recommend.ranking.log-view-weight:12.0}")
    private double rankingLogViewWeight;

    @Value("${recommend.ranking.log-like-weight:18.0}")
    private double rankingLogLikeWeight;

    @Value("${recommend.ranking.decay-bonus-max:80.0}")
    private double rankingDecayBonusMax;

    /** 发布日起算，约等于「半衰期」天数，越大衰减越慢 */
    @Value("${recommend.ranking.decay-half-life-days:120.0}")
    private double rankingDecayHalfLifeDays;

    @Value("${recommend.ranking.category-bonus-1:1200}")
    private double rankingCategoryBonus1;

    @Value("${recommend.ranking.category-bonus-2:700}")
    private double rankingCategoryBonus2;

    @Value("${recommend.ranking.category-bonus-3:400}")
    private double rankingCategoryBonus3;

    @Value("${recommend.ranking.cf-score-cap:800}")
    private double rankingCfScoreCap;

    @Value("${recommend.ranking.cf-score-multiplier:6.0}")
    private double rankingCfScoreMultiplier;

    private static final String SORT_TIE_DESC = "同分时按内容 ID 降序，保证顺序稳定。";

    @Override
    public void recordBehavior(Long userId, String behaviorType, String targetType, Long targetId) {
        if (userId == null) {
            return;
        }
        if (!StringUtils.hasText(behaviorType) || !ALLOWED_BEHAVIOR.contains(behaviorType.trim().toUpperCase(Locale.ROOT))) {
            throw new BusinessException("behaviorType 无效，允许：VIEW、LIKE、COMMENT、COLLECT");
        }
        if (!StringUtils.hasText(targetType) || !ALLOWED_TARGET.contains(targetType.trim().toUpperCase(Locale.ROOT))) {
            throw new BusinessException("targetType 无效，允许：CONTENT、FORUM_POST、POST、FORUM_COMMENT、NEWS_COMMENT、KNOWLEDGE_COMMENT、NEWS");
        }
        if (targetId == null || targetId <= 0) {
            throw new BusinessException("targetId 无效");
        }
        String normBehavior = behaviorType.trim().toUpperCase(Locale.ROOT);
        String normTarget = targetType.trim().toUpperCase(Locale.ROOT);
        if ("POST".equals(normTarget)) {
            normTarget = "FORUM_POST";
        }
        UserBehavior b = new UserBehavior();
        b.setUserId(userId);
        b.setBehaviorType(normBehavior);
        b.setTargetType(normTarget);
        b.setTargetId(targetId);
        behaviorMapper.insert(b);
    }

    @Override
    public PageResult<Map<String, Object>> recommendList(Long userId, Integer pageNum, Integer pageSize, String keyword,
                                                         boolean useAiEnhance) {
        int pn = pageNum != null ? pageNum : 1;
        int ps = pageSize != null ? pageSize : 10;
        String kw = normalizeRecommendKeyword(keyword);
        PrefSignals pref = loadPrefSignals(userId);
        AiRecommendContext aiCtx = buildAiContext(userId, kw, pref);

        Set<Long> excludeCats = mergeExcludeCategoryIds(userId);
        LambdaQueryWrapper<KnowledgeContent> baseQ = buildPublishedBaseQuery(kw, excludeCats);
        long totalAll = contentMapper.selectCount(baseQ);

        SemanticContext sem = loadSemanticContext(userId);
        Map<Long, Double> cfScores = (useItemCf && userId != null) ? buildCfScores(userId) : Collections.emptyMap();

        List<KnowledgeContent> pool;
        boolean poolTruncated = false;
        boolean recallApplied = false;
        if (recallEnabled && recallMaxCandidates > 0) {
            List<KnowledgeContent> recalled = buildMergedRecallPool(kw, excludeCats, pref, cfScores);
            if (!recalled.isEmpty()) {
                pool = recalled;
                recallApplied = true;
                poolTruncated = true;
            } else {
                pool = loadFullCandidatePool(kw, excludeCats);
                poolTruncated = false;
            }
        } else {
            pool = loadFullCandidatePool(kw, excludeCats);
            poolTruncated = false;
        }
        if (smartMaxPool > 0 && pool.size() > smartMaxPool) {
            pool.sort(Comparator
                    .comparing((KnowledgeContent c) -> c.getViewCount() != null ? c.getViewCount() : 0, Comparator.reverseOrder())
                    .thenComparing(c -> c.getLikeCount() != null ? c.getLikeCount() : 0, Comparator.reverseOrder())
                    .thenComparing(KnowledgeContent::getId, Comparator.reverseOrder()));
            pool = new ArrayList<>(pool.subList(0, smartMaxPool));
            poolTruncated = true;
        }

        long total = poolTruncated ? pool.size() : totalAll;

        if (pool.isEmpty()) {
            int ub = userBlockCount(userId);
            String hint = buildEmptyHint(userId, kw, excludeCats, poolTruncated, totalAll, ub);
            return PageResult.of(Collections.emptyList(), 0L, pn, ps, hint);
        }

        List<ScoredContent> scored = new ArrayList<>(pool.size());
        for (KnowledgeContent c : pool) {
            double s = scoreContent(c, pref, cfScores, sem);
            scored.add(new ScoredContent(c, s));
        }
        scored.sort(Comparator
                .comparingDouble((ScoredContent x) -> x.score).reversed()
                .thenComparing(x -> x.content.getId(), Comparator.reverseOrder()));

        List<Map<String, Object>> orderedMaps = scored.stream()
                .map(x -> contentToMap(x.content))
                .collect(Collectors.toList());

        int userBlocks = userBlockCount(userId);
        boolean serverAiReady = aiRecommendService.isAiLlmReady();
        boolean aiActive = userId != null && useAiEnhance && serverAiReady;
        String pageHint = buildSmartPageHint(userId, pref, cfScores, serverAiReady, useAiEnhance, poolTruncated, totalAll,
                excludeCats, userBlocks, recallApplied, pool.size());

        if (aiActive && !orderedMaps.isEmpty()) {
            int n = Math.min(Math.max(aiRerankPoolSize, 1), orderedMaps.size());
            List<Map<String, Object>> head = new ArrayList<>(orderedMaps.subList(0, n));
            List<Map<String, Object>> tail = orderedMaps.subList(n, orderedMaps.size());
            List<Map<String, Object>> rerankedHead = aiRecommendService.reorderByAi(userId, head, aiCtx);
            List<Map<String, Object>> merged = new ArrayList<>(rerankedHead.size() + tail.size());
            merged.addAll(rerankedHead);
            merged.addAll(tail);
            orderedMaps = merged;
            pageHint += " 已启用 AI：对当前排序前 " + n + " 条做模型微调，其后条目仍为规则综合序。";
        }

        String sortExplain = userId == null
                ? (recallApplied ? "多路召回(冷启动类目+热度)→" : "")
                + "精排：运营冷启动类目(若配置)、热度/时效；无个人画像/CF/语义/论坛；"
                + SORT_TIE_DESC
                : (recallApplied ? "多路召回(偏好类目+CF+热度)→" : "")
                + "精排：偏好分类、Item-CF、热度/时效、语义相似、论坛跨域；综合分降序，"
                + SORT_TIE_DESC;
        tagRecommendRows(orderedMaps, "SMART_RANK", "智能推荐", sortExplain);

        int from = (pn - 1) * ps;
        if (from >= orderedMaps.size()) {
            if (aiActive) {
                aiRecommendService.enrichRecommendReasons(userId, Collections.emptyList(), aiCtx);
            }
            return PageResult.of(Collections.emptyList(), total, pn, ps, pageHint);
        }
        int to = Math.min(from + ps, orderedMaps.size());
        List<Map<String, Object>> pageList = new ArrayList<>(orderedMaps.subList(from, to));
        if (aiActive) {
            aiRecommendService.enrichRecommendReasons(userId, pageList, aiCtx);
        }
        return PageResult.of(pageList, total, pn, ps, pageHint);
    }

    private Set<Long> mergeExcludeCategoryIds(Long userId) {
        Set<Long> out = new HashSet<>(parseExcludeCategoryIds());
        if (userId != null) {
            for (Long cid : userKnowledgeCategoryBlockService.listBlockedCategoryIds(userId)) {
                if (cid != null && cid > 0) {
                    out.add(cid);
                }
            }
        }
        return out;
    }

    private int userBlockCount(Long userId) {
        if (userId == null) {
            return 0;
        }
        return userKnowledgeCategoryBlockService.listBlockedCategoryIds(userId).size();
    }

    private LambdaQueryWrapper<KnowledgeContent> buildPublishedBaseQuery(String kw, Set<Long> excludeCats) {
        LambdaQueryWrapper<KnowledgeContent> q = new LambdaQueryWrapper<KnowledgeContent>()
                .eq(KnowledgeContent::getDeleted, 0)
                .eq(KnowledgeContent::getStatus, 1);
        // NOT IN 在 SQL 三值逻辑下会排除 category_id IS NULL 的行；无分类的知识仍应参与推荐
        if (!excludeCats.isEmpty()) {
            q.and(w -> w.isNull(KnowledgeContent::getCategoryId)
                    .or()
                    .notIn(KnowledgeContent::getCategoryId, excludeCats));
        }
        applyKeywordFilter(q, kw);
        return q;
    }

    /** 全量候选（按 id 升序），与旧版一致；再结合 smart-max-pool 截断 */
    private List<KnowledgeContent> loadFullCandidatePool(String kw, Set<Long> excludeCats) {
        LambdaQueryWrapper<KnowledgeContent> listQ = buildPublishedBaseQuery(kw, excludeCats);
        listQ.orderByAsc(KnowledgeContent::getId);
        return contentMapper.selectList(listQ);
    }

    /**
     * 多路召回：① 偏好 Top3 类目下较新知识；② Item-CF 高分内容 id；③ 全站热度补足至上限。
     * 关键词/排除分类与主流程一致。返回按「召回顺序」去重后的实体列表（随后仍走统一精排打分）。
     */
    private List<KnowledgeContent> buildMergedRecallPool(String kw, Set<Long> excludeCats, PrefSignals pref,
                                                         Map<Long, Double> cfScores) {
        if (recallMaxCandidates <= 0) {
            return Collections.emptyList();
        }
        LinkedHashSet<Long> idOrder = new LinkedHashSet<>();
        if (pref.topCategoryIds != null) {
            for (Long catId : pref.topCategoryIds) {
                if (catId == null || idOrder.size() >= recallMaxCandidates) {
                    break;
                }
                LambdaQueryWrapper<KnowledgeContent> q = buildPublishedBaseQuery(kw, excludeCats);
                q.eq(KnowledgeContent::getCategoryId, catId)
                        .orderByDesc(KnowledgeContent::getCreateTime)
                        .last("LIMIT " + recallPerCategoryLimit);
                for (KnowledgeContent c : contentMapper.selectList(q)) {
                    if (c.getId() != null && idOrder.size() < recallMaxCandidates) {
                        idOrder.add(c.getId());
                    }
                }
            }
        }
        if (cfScores != null && !cfScores.isEmpty()) {
            List<Map.Entry<Long, Double>> sortedCf = new ArrayList<>(cfScores.entrySet());
            sortedCf.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
            int cfAdded = 0;
            for (Map.Entry<Long, Double> e : sortedCf) {
                if (idOrder.size() >= recallMaxCandidates || cfAdded >= recallCfIdLimit) {
                    break;
                }
                Long cid = e.getKey();
                if (cid != null && !idOrder.contains(cid)) {
                    idOrder.add(cid);
                    cfAdded++;
                }
            }
        }
        while (idOrder.size() < recallMaxCandidates) {
            LambdaQueryWrapper<KnowledgeContent> q = buildPublishedBaseQuery(kw, excludeCats);
            if (!idOrder.isEmpty()) {
                q.notIn(KnowledgeContent::getId, idOrder);
            }
            int need = recallMaxCandidates - idOrder.size();
            q.orderByDesc(KnowledgeContent::getViewCount)
                    .orderByDesc(KnowledgeContent::getLikeCount)
                    .orderByDesc(KnowledgeContent::getId)
                    .last("LIMIT " + Math.min(Math.max(need, 1), recallHotBatchLimit));
            List<KnowledgeContent> hotBatch = contentMapper.selectList(q);
            if (hotBatch.isEmpty()) {
                break;
            }
            int before = idOrder.size();
            for (KnowledgeContent c : hotBatch) {
                if (c.getId() != null) {
                    idOrder.add(c.getId());
                    if (idOrder.size() >= recallMaxCandidates) {
                        break;
                    }
                }
            }
            if (idOrder.size() == before) {
                break;
            }
        }
        if (idOrder.isEmpty()) {
            return Collections.emptyList();
        }
        return orderContentsByIds(new ArrayList<>(idOrder));
    }

    private List<KnowledgeContent> orderContentsByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        List<KnowledgeContent> rows = contentMapper.selectList(
                new LambdaQueryWrapper<KnowledgeContent>().in(KnowledgeContent::getId, ids));
        Map<Long, KnowledgeContent> byId = rows.stream()
                .filter(c -> c.getId() != null)
                .collect(Collectors.toMap(KnowledgeContent::getId, c -> c, (a, b) -> a));
        List<KnowledgeContent> out = new ArrayList<>(ids.size());
        for (Long id : ids) {
            KnowledgeContent c = byId.get(id);
            if (c != null) {
                out.add(c);
            }
        }
        return out;
    }

    private Set<Long> parseExcludeCategoryIds() {
        if (!StringUtils.hasText(excludeCategoryIdsConfig)) {
            return Collections.emptySet();
        }
        Set<Long> out = new HashSet<>();
        for (String p : excludeCategoryIdsConfig.split(",")) {
            if (!StringUtils.hasText(p)) {
                continue;
            }
            try {
                long v = Long.parseLong(p.trim());
                if (v > 0) {
                    out.add(v);
                }
            } catch (NumberFormatException ignored) {
                /* skip */
            }
        }
        return out;
    }

    private String buildEmptyHint(Long userId, String kw, Set<Long> excludeCats, boolean truncated, long totalAll, int userBlocks) {
        StringBuilder sb = new StringBuilder();
        if (totalAll == 0) {
            sb.append("当前没有符合筛选条件的已上架知识（或未发布内容）。");
        } else {
            sb.append("本页无数据。");
        }
        if (StringUtils.hasText(kw)) {
            sb.append(" 已按关键词缩小范围。");
        }
        if (userBlocks > 0) {
            sb.append(" 您已在个人中心屏蔽 ").append(userBlocks).append(" 个知识分类。");
        }
        Set<Long> configOnly = parseExcludeCategoryIds();
        if (!configOnly.isEmpty()) {
            sb.append(" 运营侧另排除 ").append(configOnly.size()).append(" 个分类。");
        }
        if (truncated) {
            sb.append(" 候选池已按配置截断。");
        }
        return sb.toString();
    }

    private String buildSmartPageHint(Long userId, PrefSignals pref, Map<Long, Double> cf,
                                      boolean serverAiReady, boolean useAiEnhance, boolean poolTruncated, long totalAll,
                                      Set<Long> excludeCats, int userBlocks, boolean recallApplied, int poolSize) {
        StringBuilder sb = new StringBuilder();
        sb.append("智能推荐：仅展示已上架且未删除的知识；");
        if (userBlocks > 0) {
            sb.append("已按您在个人中心设置的分类屏蔽过滤；");
        }
        Set<Long> configOnly = parseExcludeCategoryIds();
        if (!configOnly.isEmpty()) {
            sb.append("运营配置另排除部分分类；");
        }
        boolean anonymous = userId == null;
        if (anonymous) {
            if (recallApplied) {
                sb.append("采用「召回+精排」：合并冷启动默认类目(若配置)较新内容与热度暖池（约 ")
                        .append(poolSize)
                        .append(" 条）再统一打分；全库符合条件的条数可能大于本页 total。");
            }
            sb.append("未登录：仅冷启动与全站热度/时效，不含个人行为画像、Item-CF、语义与论坛；登录后可使用完整个性化与 AI 增强。");
            if (pref != null && pref.topCategoryIds != null && !pref.topCategoryIds.isEmpty()) {
                sb.append(" 当前已按运营配置的默认兴趣类目参与召回与类目匹配加分。");
            }
        } else {
            if (recallApplied) {
                sb.append("采用「召回+精排」：先合并偏好类目较新内容、协同过滤候选与热度暖池（约 ")
                        .append(poolSize)
                        .append(" 条）再统一多因子打分，避免对全库逐条计算；全库符合条件的条数可能大于本页 total。");
            }
            sb.append("再按您的偏好分类(行为加权+时间衰减)、协同过滤、全站热度/时效与文本相似加分综合排序，越贴合您的越靠前。");
            if (pref.topCategoryIds.isEmpty()) {
                sb.append(" 冷启动：暂无足够行为画像，热门内容会更靠前。");
            }
            if (cf.isEmpty() || !useItemCf) {
                sb.append(" 当前未叠加协同过滤得分。");
            }
        }
        if (poolTruncated) {
            sb.append(" 注意：候选超过 recommend.smart-max-pool，已按热度截取后再排序，总数以本页接口 total 为准。");
        }
        if (!serverAiReady) {
            sb.append(" 大模型未配置或未就绪：仅使用规则排序。");
        } else if (!useAiEnhance) {
            sb.append(" 本次未开启 AI 增强：不调用大模型，不消耗 Token。");
        } else if (anonymous) {
            sb.append(" AI 增强需登录：本次未调用大模型。");
        }
        return sb.toString();
    }

    private PrefSignals loadPrefSignals(Long userId) {
        PrefSignals p = new PrefSignals();
        if (userId == null) {
            p.topCategoryIds = new ArrayList<>(parseColdStartCategoryIds());
            return p;
        }
        List<Long> cats = behaviorMapper.selectWeightedTop3CategoryIds(userId);
        List<Long> catList = cats == null ? new ArrayList<>() : cats.stream()
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        p.topCategoryIds = catList;

        if (p.topCategoryIds.isEmpty()) {
            p.topCategoryIds = new ArrayList<>(parseColdStartCategoryIds());
        }
        return p;
    }

    private List<Long> parseColdStartCategoryIds() {
        if (!StringUtils.hasText(coldStartCategoryIdsConfig)) {
            return Collections.emptyList();
        }
        List<Long> out = new ArrayList<>();
        for (String part : coldStartCategoryIdsConfig.split(",")) {
            if (!StringUtils.hasText(part)) {
                continue;
            }
            try {
                long v = Long.parseLong(part.trim());
                if (v > 0) {
                    out.add(v);
                }
            } catch (NumberFormatException ignored) {
                /* skip */
            }
        }
        return out;
    }

    private SemanticContext loadSemanticContext(Long userId) {
        if (userId == null) {
            return SemanticContext.EMPTY;
        }
        List<String> recent = new ArrayList<>();
        if (semanticSimilarityEnabled) {
            List<Map<String, Object>> rows = behaviorMapper.selectRecentViewedContentTexts(userId, semanticRecentViewLimit);
            if (rows != null) {
                for (Map<String, Object> row : rows) {
                    String t = row.get("contentTitle") != null ? row.get("contentTitle").toString().trim() : "";
                    String s = row.get("contentSummary") != null ? row.get("contentSummary").toString().trim() : "";
                    String merged = (t + " " + s).trim();
                    if (StringUtils.hasText(merged)) {
                        if (merged.length() > 3000) {
                            merged = merged.substring(0, 3000);
                        }
                        recent.add(merged);
                    }
                }
            }
        }
        List<String> forumTitles = new ArrayList<>();
        if (forumCrossDomainEnabled) {
            List<String> ft = behaviorMapper.selectForumCrossDomainTitles(userId, forumCrossTitleLimit);
            if (ft != null) {
                LinkedHashSet<String> seen = new LinkedHashSet<>();
                for (String x : ft) {
                    if (StringUtils.hasText(x)) {
                        seen.add(x.trim());
                    }
                }
                forumTitles.addAll(seen);
            }
        }
        return new SemanticContext(recent, forumTitles);
    }

    /**
     * Item-CF：对用户最近 {@link #userHistoryLimit} 条 CONTENT 行为涉及的去重内容 id，分别查共现 TopN 再汇总。
     * 已交互过的内容不再作为 CF 候选（与「推荐未发现项」一致）。注意：此处对每个种子内容一次 SQL，种子数多时可用单条聚合 SQL 优化吞吐（会改变「每种子 TopN」截断语义）。
     */
    private Map<Long, Double> buildCfScores(Long userId) {
        List<UserBehavior> behaviors = behaviorMapper.selectList(
                new LambdaQueryWrapper<UserBehavior>()
                        .eq(UserBehavior::getUserId, userId)
                        .eq(UserBehavior::getTargetType, "CONTENT")
                        .orderByDesc(UserBehavior::getCreateTime)
                        .last("LIMIT " + userHistoryLimit));
        if (CollectionUtils.isEmpty(behaviors)) {
            return Collections.emptyMap();
        }
        Set<Long> userContentIds = behaviors.stream().map(UserBehavior::getTargetId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (userContentIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Double> candidateScores = new HashMap<>();
        for (Long contentId : userContentIds) {
            List<Map<String, Object>> similar = behaviorMapper.findSimilarContentIds(contentId, similarPerItem);
            if (similar == null) {
                continue;
            }
            for (Map<String, Object> row : similar) {
                Object idObj = row.get("contentId");
                Object scoreObj = row.get("score");
                if (idObj == null || scoreObj == null) {
                    continue;
                }
                try {
                    Long cid = idObj instanceof Number ? ((Number) idObj).longValue() : Long.parseLong(idObj.toString());
                    double score = parseDoubleScore(scoreObj);
                    if (Double.isNaN(score) || Double.isInfinite(score) || score <= 0) {
                        continue;
                    }
                    if (!userContentIds.contains(cid)) {
                        candidateScores.merge(cid, score, Double::sum);
                    }
                } catch (NumberFormatException ignored) {
                    /* 跳过异常行，避免单行脏数据拖垮推荐 */
                }
            }
        }
        return candidateScores;
    }

    private static double parseDoubleScore(Object scoreObj) {
        if (scoreObj instanceof Number) {
            return ((Number) scoreObj).doubleValue();
        }
        return Double.parseDouble(scoreObj.toString());
    }

    private double scoreContent(KnowledgeContent c, PrefSignals pref, Map<Long, Double> cfScores, SemanticContext sem) {
        int views = c.getViewCount() != null ? c.getViewCount() : 0;
        int likes = c.getLikeCount() != null ? c.getLikeCount() : 0;
        double score = Math.log1p(views) * rankingLogViewWeight + Math.log1p(likes) * rankingLogLikeWeight;
        LocalDateTime ct = c.getCreateTime();
        if (ct != null) {
            long days = ChronoUnit.DAYS.between(ct.toLocalDate(), LocalDateTime.now().toLocalDate());
            double half = rankingDecayHalfLifeDays > 0 ? rankingDecayHalfLifeDays : 120.0;
            double decay = Math.exp(-Math.max(0, days) / half);
            score += decay * rankingDecayBonusMax;
        }
        Long catId = c.getCategoryId();
        if (catId != null && pref.topCategoryIds != null && !pref.topCategoryIds.isEmpty()) {
            int idx = pref.topCategoryIds.indexOf(catId);
            if (idx == 0) {
                score += rankingCategoryBonus1;
            } else if (idx == 1) {
                score += rankingCategoryBonus2;
            } else if (idx == 2) {
                score += rankingCategoryBonus3;
            }
        }
        Double cf = cfScores.get(c.getId());
        if (cf != null && cf > 0) {
            double cap = rankingCfScoreCap > 0 ? rankingCfScoreCap : 800;
            double mul = rankingCfScoreMultiplier > 0 ? rankingCfScoreMultiplier : 6.0;
            score += Math.min(cap, cf * mul);
        }
        if (sem != null && semanticSimilarityEnabled && sem.recentTexts != null && !sem.recentTexts.isEmpty()) {
            String cand = concatTitleSummary(c);
            double maxSim = 0;
            for (String ref : sem.recentTexts) {
                maxSim = Math.max(maxSim, TextSimilarityUtil.diceBigram(cand, ref));
            }
            score += Math.min(semanticMaxBoost, semanticMaxBoost * maxSim);
        }
        if (sem != null && forumCrossDomainEnabled && sem.forumTitles != null && !sem.forumTitles.isEmpty()) {
            String cand = concatTitleSummary(c);
            double maxF = 0;
            for (String ft : sem.forumTitles) {
                maxF = Math.max(maxF, TextSimilarityUtil.diceBigram(cand, ft));
            }
            score += Math.min(forumCrossMaxBoost, forumCrossMaxBoost * maxF);
        }
        return score;
    }

    private static String concatTitleSummary(KnowledgeContent c) {
        String t = c.getTitle() != null ? c.getTitle() : "";
        String s = c.getSummary() != null ? c.getSummary() : "";
        return (t + " " + s).trim();
    }

    private void applyKeywordFilter(LambdaQueryWrapper<KnowledgeContent> q, String kw) {
        if (!StringUtils.hasText(kw)) {
            return;
        }
        String k = kw.trim();
        q.and(w -> w.like(KnowledgeContent::getTitle, k).or().like(KnowledgeContent::getSummary, k));
    }

    private String normalizeRecommendKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        String t = keyword.trim();
        if (keywordMaxLength > 0 && t.length() > keywordMaxLength) {
            throw new BusinessException("检索词过长，最多 " + keywordMaxLength + " 个字符");
        }
        return t;
    }

    private AiRecommendContext buildAiContext(Long userId, String keywordTrimmed, PrefSignals pref) {
        String topName = null;
        if (pref != null && !pref.topCategoryIds.isEmpty()) {
            KnowledgeCategory c = categoryMapper.selectById(pref.topCategoryIds.get(0));
            topName = c != null ? c.getName() : null;
        }
        return AiRecommendContext.builder()
                .keyword(keywordTrimmed)
                .topCategoryName(topName)
                .hotTopicKeywords(parseHotTopics())
                .build();
    }

    private List<String> parseHotTopics() {
        if (!StringUtils.hasText(hotTopicsConfig)) {
            return Collections.emptyList();
        }
        return Arrays.stream(hotTopicsConfig.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
    }

    private void tagRecommendRows(List<Map<String, Object>> rows, String source, String sourceLabel, String sortExplain) {
        if (rows == null) {
            return;
        }
        for (Map<String, Object> m : rows) {
            m.put("recommendSource", source);
            m.put("recommendSourceLabel", sourceLabel);
            m.put("sortExplain", sortExplain);
        }
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
        m.put("updateTime", c.getUpdateTime());
        return m;
    }

    private static final class PrefSignals {
        List<Long> topCategoryIds = Collections.emptyList();
    }

    /** 近期浏览文本 + 论坛标题，用于字二元组相似加分 */
    private static final class SemanticContext {
        static final SemanticContext EMPTY = new SemanticContext(Collections.emptyList(), Collections.emptyList());
        final List<String> recentTexts;
        final List<String> forumTitles;

        SemanticContext(List<String> recentTexts, List<String> forumTitles) {
            this.recentTexts = recentTexts;
            this.forumTitles = forumTitles;
        }
    }

    private static final class ScoredContent {
        final KnowledgeContent content;
        final double score;

        ScoredContent(KnowledgeContent content, double score) {
            this.content = content;
            this.score = score;
        }
    }
}
