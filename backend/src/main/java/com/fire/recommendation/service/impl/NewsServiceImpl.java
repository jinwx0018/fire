package com.fire.recommendation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fire.recommendation.component.DetailViewDedupHelper;
import com.fire.recommendation.entity.News;
import com.fire.recommendation.entity.NewsCategory;
import com.fire.recommendation.entity.NewsLike;
import com.fire.recommendation.entity.SysRole;
import com.fire.recommendation.entity.SysUser;
import com.fire.recommendation.exception.BusinessException;
import com.fire.recommendation.common.ResultCode;
import com.fire.recommendation.component.NewsMediaWhitelistFilter;
import com.fire.recommendation.constant.NewsRegionFallback;
import com.fire.recommendation.mapper.NewsCategoryMapper;
import com.fire.recommendation.mapper.NewsLikeMapper;
import com.fire.recommendation.mapper.NewsMapper;
import com.fire.recommendation.mapper.SysRoleMapper;
import com.fire.recommendation.mapper.SysUserMapper;
import com.fire.recommendation.constant.CollectionTargetType;
import com.fire.recommendation.service.AuditLogService;
import com.fire.recommendation.service.NewsService;
import com.fire.recommendation.service.UserCollectionService;
import com.fire.recommendation.util.NewsAutoSummaryUtil;
import com.fire.recommendation.util.NewsCoverUrlUtil;
import com.fire.recommendation.util.PlainTextSanitizer;
import com.fire.recommendation.util.RichTextHtmlSanitizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
public class NewsServiceImpl implements NewsService {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_PAGE_NUM = 10_000;
    private static final int MAX_KEYWORD_LEN = 100;
    private static final int RELATED_NEWS_LIMIT = 5;

    private static final DateTimeFormatter[] PUBLISH_TIME_FORMATTERS = new DateTimeFormatter[]{
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
    };

    private final NewsMapper newsMapper;
    private final NewsCategoryMapper newsCategoryMapper;
    private final NewsLikeMapper newsLikeMapper;
    private final NewsMediaWhitelistFilter newsMediaWhitelistFilter;
    private final SysUserMapper userMapper;
    private final AuditLogService auditLogService;
    private final UserCollectionService userCollectionService;
    private final SysRoleMapper roleMapper;
    private final DetailViewDedupHelper detailViewDedupHelper;

    private static int clampPageNum(Integer pageNum) {
        int p = pageNum == null ? 1 : pageNum;
        if (p < 1) {
            return 1;
        }
        return Math.min(p, MAX_PAGE_NUM);
    }

    private static int clampPageSize(Integer pageSize) {
        int s = pageSize == null ? DEFAULT_PAGE_SIZE : pageSize;
        if (s < 1) {
            return 1;
        }
        return Math.min(s, MAX_PAGE_SIZE);
    }

    @Override
    public IPage<Map<String, Object>> list(Integer pageNum, Integer pageSize, List<String> regions, String region, Long categoryId, String category,
                                           String title, String keyword, String orderBy, String order) {
        Page<News> page = new Page<>(clampPageNum(pageNum), clampPageSize(pageSize));
        LambdaQueryWrapper<News> q = buildUserListBase(regions, region, categoryId, category);
        applyFuzzyListSearch(q, title, keyword);
        applyOrder(q, orderBy, order);
        IPage<News> result = newsMapper.selectPage(page, q);
        Map<Long, String> publisherNames = publisherNamesForRecords(result.getRecords());
        Map<Long, String> categoryNames = categoryNamesForRecords(result.getRecords());
        return result.convert(n -> toUserListRow(n, publisherNames.get(n.getPublisherId()), categoryNames));
    }

    @Override
    public IPage<Map<String, Object>> adminPage(Integer pageNum, Integer pageSize, List<String> regions, String region, Long categoryId, String category,
                                                String title, String keyword, String orderBy, String order, Integer status) {
        Page<News> page = new Page<>(clampPageNum(pageNum), clampPageSize(pageSize));
        LambdaQueryWrapper<News> q = buildAdminListBase(regions, region, categoryId, category, status);
        applyFuzzyListSearch(q, title, keyword);
        applyOrder(q, orderBy, order);
        IPage<News> result = newsMapper.selectPage(page, q);
        Map<Long, String> publisherNames = publisherNamesForRecords(result.getRecords());
        Map<Long, String> categoryNames = categoryNamesForRecords(result.getRecords());
        return result.convert(n -> toAdminListRow(n, publisherNames.get(n.getPublisherId()), categoryNames));
    }

    @Override
    public List<String> listDistinctRegions() {
        return NewsRegionFallback.orProvincialFallback(newsMapper.selectDistinctRegionsForUser());
    }

    @Override
    public List<String> listDistinctRegionsForAdmin() {
        return NewsRegionFallback.orProvincialFallback(newsMapper.selectDistinctRegionsForAdmin());
    }

    private LambdaQueryWrapper<News> buildUserListBase(List<String> regions, String region, Long categoryId, String category) {
        LambdaQueryWrapper<News> q = new LambdaQueryWrapper<>();
        q.eq(News::getDeleted, 0).eq(News::getStatus, 1);
        LocalDateTime now = LocalDateTime.now();
        q.and(w -> w.isNull(News::getPublishTime).or().le(News::getPublishTime, now));
        applyRegionFilter(q, regions, region);
        applyCategoryFilter(q, categoryId, category);
        return q;
    }

    private LambdaQueryWrapper<News> buildAdminListBase(List<String> regions, String region, Long categoryId, String category, Integer status) {
        LambdaQueryWrapper<News> q = new LambdaQueryWrapper<>();
        q.eq(News::getDeleted, 0);
        if (status != null) {
            q.eq(News::getStatus, status);
        }
        applyRegionFilter(q, regions, region);
        applyCategoryFilter(q, categoryId, category);
        return q;
    }

    /** 多选：region IN (...)；否则单地区 eq */
    private void applyRegionFilter(LambdaQueryWrapper<News> q, List<String> regions, String region) {
        if (regions != null && !regions.isEmpty()) {
            List<String> cleaned = regions.stream()
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .distinct()
                    .collect(Collectors.toList());
            if (!cleaned.isEmpty()) {
                q.in(News::getRegion, cleaned);
            }
        } else if (StringUtils.hasText(region)) {
            q.eq(News::getRegion, region.trim());
        }
    }

    /** 标题、摘要均为 LIKE 模糊 */
    private void applyFuzzyListSearch(LambdaQueryWrapper<News> q, String title, String keyword) {
        if (StringUtils.hasText(title)) {
            q.like(News::getTitle, title.trim());
        }
        applyKeywordFilter(q, keyword);
    }

    private void applyCategoryFilter(LambdaQueryWrapper<News> q, Long categoryId, String category) {
        if (categoryId != null) {
            q.eq(News::getCategoryId, categoryId);
        } else if (StringUtils.hasText(category)) {
            // 旧版 category 文本列在部分数据库中不存在；为兼容只保留 category_id 的库，这里忽略该过滤条件。
        }
    }

    private void applyKeywordFilter(LambdaQueryWrapper<News> q, String keyword) {
        String k = normalizeKeyword(keyword);
        if (k == null) {
            return;
        }
        q.and(w -> w.like(News::getTitle, k).or().like(News::getSummary, k));
    }

    private static String normalizeKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        String k = keyword.trim();
        if (k.length() > MAX_KEYWORD_LEN) {
            k = k.substring(0, MAX_KEYWORD_LEN);
        }
        return k;
    }

    private void applyOrder(LambdaQueryWrapper<News> q, String orderBy, String order) {
        boolean asc = "asc".equalsIgnoreCase(order);
        if ("urgency".equalsIgnoreCase(orderBy)) {
            q.orderBy(true, asc, News::getUrgencyLevel)
                    .orderByDesc(News::getPublishTime)
                    .orderByDesc(News::getId);
        } else {
            q.orderBy(true, asc, News::getPublishTime).orderBy(true, asc, News::getId);
        }
    }

    private Map<String, Object> toUserListRow(News n, String publisherName, Map<Long, String> categoryNames) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", n.getId());
        m.put("title", PlainTextSanitizer.sanitizeTitleOutput(n.getTitle()));
        m.put("region", n.getRegion());
        m.put("urgencyLevel", n.getUrgencyLevel());
        m.put("publishTime", n.getPublishTime());
        m.put("summary", PlainTextSanitizer.sanitizeSummaryOutput(n.getSummary()));
        m.put("categoryId", n.getCategoryId());
        m.put("categoryName", resolveCategoryDisplayName(n, categoryNames));
        // 兼容：数据库可能未包含 news.category
        m.put("category", null);
        m.put("coverUrl", NewsCoverUrlUtil.sanitize(n.getCoverUrl()));
        m.put("likeCount", n.getLikeCount() != null ? n.getLikeCount() : 0);
        m.put("publisherName", StringUtils.hasText(publisherName) ? publisherName : null);
        return m;
    }

    private Map<String, Object> toAdminListRow(News n, String publisherName, Map<Long, String> categoryNames) {
        Map<String, Object> m = toUserListRow(n, publisherName, categoryNames);
        m.put("status", n.getStatus());
        return m;
    }

    private String resolveCategoryDisplayName(News n, Map<Long, String> categoryNames) {
        if (n.getCategoryId() != null) {
            String cn = categoryNames.get(n.getCategoryId());
            if (StringUtils.hasText(cn)) {
                return cn;
            }
            NewsCategory c = newsCategoryMapper.selectById(n.getCategoryId());
            if (c != null && !Integer.valueOf(1).equals(c.getDeleted())) {
                return c.getName();
            }
        }
        return null;
    }

    private Map<Long, String> categoryNamesForRecords(List<News> records) {
        if (records == null || records.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = records.stream()
                .map(News::getCategoryId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        List<NewsCategory> list = newsCategoryMapper.selectList(new LambdaQueryWrapper<NewsCategory>().in(NewsCategory::getId, ids));
        Map<Long, String> map = new HashMap<>();
        for (NewsCategory c : list) {
            if (c != null && !Integer.valueOf(1).equals(c.getDeleted()) && StringUtils.hasText(c.getName())) {
                map.put(c.getId(), c.getName());
            }
        }
        return map;
    }

    private Map<Long, String> publisherNamesForRecords(List<News> records) {
        if (records == null || records.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = records.stream()
                .map(News::getPublisherId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        List<SysUser> users = userMapper.selectList(new LambdaQueryWrapper<SysUser>().in(SysUser::getId, ids));
        Map<Long, String> map = new HashMap<>();
        for (SysUser u : users) {
            if (u != null && StringUtils.hasText(u.getUsername())) {
                map.put(u.getId(), u.getUsername());
            }
        }
        return map;
    }

    private boolean isAdmin(Long userId) {
        if (userId == null) {
            return false;
        }
        SysUser u = userMapper.selectById(userId);
        if (u == null || Integer.valueOf(1).equals(u.getDeleted())) {
            return false;
        }
        SysRole r = roleMapper.selectById(u.getRoleId());
        return r != null && "ADMIN".equals(r.getRoleCode());
    }

    @Override
    public Map<String, Object> getDetail(Long id, Long viewerUserId, boolean recordView, String clientIp) {
        News n = newsMapper.selectById(id);
        if (n == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (n.getStatus() == null || !Integer.valueOf(1).equals(n.getStatus())) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (n.getPublishTime() != null && n.getPublishTime().isAfter(LocalDateTime.now())) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        boolean publisherSelf = viewerUserId != null && n.getPublisherId() != null && viewerUserId.equals(n.getPublisherId());
        boolean bumpView = recordView
                && !isAdmin(viewerUserId)
                && !publisherSelf
                && detailViewDedupHelper.shouldIncrement("news", id, viewerUserId, clientIp);
        if (bumpView) {
            newsMapper.update(null, new LambdaUpdateWrapper<News>()
                    .eq(News::getId, id)
                    .setSql("view_count = IFNULL(view_count, 0) + 1"));
        }
        n = newsMapper.selectById(id);
        if (n == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        Map<String, Object> m = newsToMap(n);
        m.put("related", buildRelatedNews(n));
        m.put("likeCount", n.getLikeCount() != null ? n.getLikeCount() : 0);
        if (viewerUserId != null) {
            long lc = newsLikeMapper.selectCount(new LambdaQueryWrapper<NewsLike>()
                    .eq(NewsLike::getNewsId, id)
                    .eq(NewsLike::getUserId, viewerUserId));
            m.put("liked", lc > 0);
            m.put("collected", userCollectionService.isCollected(viewerUserId, CollectionTargetType.NEWS, id));
        } else {
            m.put("liked", false);
            m.put("collected", false);
        }
        return m;
    }

    @Override
    public Map<String, Object> adminGetDetail(Long id) {
        News n = newsMapper.selectById(id);
        if (n == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        Map<String, Object> m = newsToMap(n);
        m.put("status", n.getStatus());
        return m;
    }

    private Map<String, Object> newsToMap(News n) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", n.getId());
        m.put("title", PlainTextSanitizer.sanitizeTitleOutput(n.getTitle()));
        m.put("content", finalizeNewsHtml(n.getContent()));
        m.put("region", n.getRegion());
        m.put("urgencyLevel", n.getUrgencyLevel());
        m.put("publisherId", n.getPublisherId());
        m.put("publisherName", resolvePublisherName(n.getPublisherId()));
        m.put("summary", PlainTextSanitizer.sanitizeSummaryOutput(n.getSummary()));
        m.put("categoryId", n.getCategoryId());
        m.put("categoryName", categoryNameForSingleNews(n));
        // 兼容：数据库可能未包含 news.category
        m.put("category", null);
        m.put("coverUrl", NewsCoverUrlUtil.sanitize(n.getCoverUrl()));
        m.put("viewCount", n.getViewCount());
        m.put("likeCount", n.getLikeCount() != null ? n.getLikeCount() : 0);
        m.put("publishTime", n.getPublishTime());
        m.put("createTime", n.getCreateTime());
        return m;
    }

    private String categoryNameForSingleNews(News n) {
        if (n.getCategoryId() != null) {
            NewsCategory c = newsCategoryMapper.selectById(n.getCategoryId());
            if (c != null && !Integer.valueOf(1).equals(c.getDeleted())) {
                return c.getName();
            }
        }
        return null;
    }

    private List<Map<String, Object>> buildRelatedNews(News current) {
        LocalDateTime now = LocalDateTime.now();
        List<Map<String, Object>> out = new ArrayList<>();
        Set<Long> exclude = new HashSet<>();
        exclude.add(current.getId());
        int need = RELATED_NEWS_LIMIT;
        if (current.getCategoryId() != null) {
            need = appendRelatedBatch(out, exclude, now, need, w -> w.eq(News::getCategoryId, current.getCategoryId()));
        }
        if (need > 0 && StringUtils.hasText(current.getRegion())) {
            need = appendRelatedBatch(out, exclude, now, need, w -> w.eq(News::getRegion, current.getRegion()));
        }
        if (need > 0) {
            appendRelatedBatch(out, exclude, now, need, w -> {
            });
        }
        return out;
    }

    private int appendRelatedBatch(List<Map<String, Object>> out, Set<Long> exclude, LocalDateTime now, int limit,
                                   Consumer<LambdaQueryWrapper<News>> extra) {
        if (limit <= 0) {
            return 0;
        }
        LambdaQueryWrapper<News> q = baseRelatedQuery(exclude, now);
        extra.accept(q);
        q.orderByDesc(News::getPublishTime).orderByDesc(News::getId).last("LIMIT " + limit);
        List<News> rows = newsMapper.selectList(q);
        for (News n : rows) {
            exclude.add(n.getId());
            out.add(toRelatedRow(n));
            if (--limit <= 0) {
                return 0;
            }
        }
        return limit;
    }

    private LambdaQueryWrapper<News> baseRelatedQuery(Set<Long> exclude, LocalDateTime now) {
        LambdaQueryWrapper<News> q = new LambdaQueryWrapper<>();
        q.eq(News::getDeleted, 0).eq(News::getStatus, 1)
                .and(w -> w.isNull(News::getPublishTime).or().le(News::getPublishTime, now));
        if (!exclude.isEmpty()) {
            q.notIn(News::getId, exclude);
        }
        return q;
    }

    private Map<String, Object> toRelatedRow(News n) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", n.getId());
        m.put("title", PlainTextSanitizer.sanitizeTitleOutput(n.getTitle()));
        m.put("summary", PlainTextSanitizer.sanitizeSummaryOutput(n.getSummary()));
        m.put("categoryId", n.getCategoryId());
        m.put("categoryName", categoryNameForSingleNews(n));
        m.put("category", null);
        m.put("region", n.getRegion());
        m.put("coverUrl", NewsCoverUrlUtil.sanitize(n.getCoverUrl()));
        m.put("publishTime", n.getPublishTime());
        m.put("urgencyLevel", n.getUrgencyLevel());
        return m;
    }

    private String resolvePublisherName(Long publisherId) {
        if (publisherId == null) {
            return null;
        }
        SysUser u = userMapper.selectById(publisherId);
        if (u == null || !StringUtils.hasText(u.getUsername())) {
            return null;
        }
        return u.getUsername();
    }

    private String finalizeNewsHtml(String stored) {
        if (stored == null) {
            return "";
        }
        return newsMediaWhitelistFilter.filter(RichTextHtmlSanitizer.sanitize(stored));
    }

    @Override
    public Long adminSave(Long publisherId, Map<String, Object> body) {
        if (body == null) {
            throw new BusinessException("请求体不能为空");
        }
        String title = PlainTextSanitizer.sanitizeTitle(
                body.get("title") != null ? body.get("title").toString() : "");
        if (!StringUtils.hasText(title)) {
            throw new BusinessException("标题不能为空");
        }
        String rawContent = body.get("content") != null ? body.get("content").toString() : "";
        if (RichTextHtmlSanitizer.isEffectivelyBlank(rawContent)) {
            throw new BusinessException("正文不能为空");
        }
        String safeContent = finalizeNewsHtml(rawContent);
        News n = new News();
        n.setTitle(title);
        n.setContent(safeContent);
        n.setRegion(strOrNull(body.get("region")));
        applyCategoryFieldsToNews(n, body, false);
        String sum = PlainTextSanitizer.sanitizeSummary(strOrNull(body.get("summary")));
        if (!StringUtils.hasText(sum)) {
            sum = NewsAutoSummaryUtil.fromSanitizedHtml(safeContent);
        }
        n.setSummary(sum);
        if (body.containsKey("coverUrl")) {
            n.setCoverUrl(parseCoverUrlFromBody(body.get("coverUrl")));
        }
        n.setUrgencyLevel(parseUrgencyLevel(body.get("urgencyLevel"), 1));
        n.setPublisherId(publisherId);
        n.setStatus(parseNewsStatus(body.get("status"), 1));
        n.setViewCount(0);
        n.setLikeCount(0);
        LocalDateTime pub = parsePublishTime(body.get("publishTime"));
        n.setPublishTime(pub != null ? pub : LocalDateTime.now());
        newsMapper.insert(n);
        auditLogService.log(publisherId, "NEWS_CREATE", "NEWS", n.getId(), "新增新闻: " + title);
        return n.getId();
    }

    @Override
    public void adminUpdate(Long operatorId, Long id, Map<String, Object> body) {
        News n = newsMapper.selectById(id);
        if (n == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (body == null) {
            return;
        }
        boolean contentChanged = false;
        if (body.containsKey("title")) {
            String title = PlainTextSanitizer.sanitizeTitle(
                    body.get("title") != null ? body.get("title").toString() : "");
            if (!StringUtils.hasText(title)) {
                throw new BusinessException("标题不能为空");
            }
            n.setTitle(title);
        }
        if (body.containsKey("content")) {
            String raw = body.get("content") != null ? body.get("content").toString() : "";
            if (RichTextHtmlSanitizer.isEffectivelyBlank(raw)) {
                throw new BusinessException("正文不能为空");
            }
            n.setContent(finalizeNewsHtml(raw));
            contentChanged = true;
        }
        if (body.containsKey("region")) {
            n.setRegion(strOrNull(body.get("region")));
        }
        if (body.containsKey("categoryId") || body.containsKey("category")) {
            applyCategoryFieldsToNews(n, body, true);
        }
        if (body.containsKey("summary")) {
            n.setSummary(PlainTextSanitizer.sanitizeSummary(strOrNull(body.get("summary"))));
        }
        if (contentChanged && !StringUtils.hasText(n.getSummary())) {
            n.setSummary(NewsAutoSummaryUtil.fromSanitizedHtml(n.getContent()));
        }
        if (body.containsKey("coverUrl")) {
            n.setCoverUrl(parseCoverUrlFromBody(body.get("coverUrl")));
        }
        if (body.containsKey("urgencyLevel")) {
            n.setUrgencyLevel(parseUrgencyLevel(body.get("urgencyLevel"), n.getUrgencyLevel() != null ? n.getUrgencyLevel() : 1));
        }
        if (body.containsKey("status")) {
            n.setStatus(parseNewsStatus(body.get("status"), n.getStatus() != null ? n.getStatus() : 1));
        }
        if (body.containsKey("publishTime")) {
            n.setPublishTime(parsePublishTime(body.get("publishTime")));
        }
        newsMapper.updateById(n);
        auditLogService.log(operatorId, "NEWS_UPDATE", "NEWS", id, "修改新闻");
    }

    private void applyCategoryFieldsToNews(News n, Map<String, Object> body, boolean forUpdate) {
        if (body.containsKey("categoryId")) {
            Object raw = body.get("categoryId");
            if (raw == null || (raw instanceof String s && !StringUtils.hasText(s))) {
                n.setCategoryId(null);
            } else {
                Long cid = parseCategoryId(raw);
                if (cid == null) {
                    throw new BusinessException("无效的新闻分类 id");
                }
                n.setCategoryId(cid);
            }
        } else if (!forUpdate) {
            n.setCategoryId(null);
        }
        // 兼容：数据库可能未包含 news.category，自由分类不落库
        n.setCategory(null);
    }

    private Long parseCategoryId(Object raw) {
        try {
            long v = Long.parseLong(raw.toString().trim());
            if (v <= 0) {
                return null;
            }
            NewsCategory c = newsCategoryMapper.selectById(v);
            if (c == null || Integer.valueOf(1).equals(c.getDeleted())) {
                return null;
            }
            return v;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void adminDelete(Long operatorId, Long id) {
        News n = newsMapper.selectById(id);
        if (n == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        newsMapper.deleteById(id);
        auditLogService.log(operatorId, "NEWS_DELETE", "NEWS", id, "删除新闻: " + n.getTitle());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> toggleLike(Long newsId, Long userId) {
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        News news = newsMapper.selectOne(new LambdaQueryWrapper<News>()
                .eq(News::getId, newsId)
                .last("FOR UPDATE"));
        if (news == null || Integer.valueOf(1).equals(news.getDeleted())) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (news.getStatus() == null || !Integer.valueOf(1).equals(news.getStatus())) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (news.getPublishTime() != null && news.getPublishTime().isAfter(LocalDateTime.now())) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        LambdaQueryWrapper<NewsLike> q = new LambdaQueryWrapper<NewsLike>()
                .eq(NewsLike::getNewsId, newsId)
                .eq(NewsLike::getUserId, userId);
        NewsLike existing = newsLikeMapper.selectOne(q);
        if (existing != null) {
            newsLikeMapper.deleteById(existing.getId());
            newsMapper.update(null, new LambdaUpdateWrapper<News>()
                    .eq(News::getId, newsId)
                    .setSql("like_count = GREATEST(IFNULL(like_count, 0) - 1, 0)"));
        } else {
            NewsLike l = new NewsLike();
            l.setNewsId(newsId);
            l.setUserId(userId);
            newsLikeMapper.insert(l);
            newsMapper.update(null, new LambdaUpdateWrapper<News>()
                    .eq(News::getId, newsId)
                    .setSql("like_count = IFNULL(like_count, 0) + 1"));
        }
        News after = newsMapper.selectById(newsId);
        int likeCount = after != null && after.getLikeCount() != null ? after.getLikeCount() : 0;
        boolean liked = existing == null;
        Map<String, Object> out = new HashMap<>();
        out.put("liked", liked);
        out.put("likeCount", likeCount);
        return out;
    }

    private static String strOrNull(Object o) {
        if (o == null) {
            return null;
        }
        String s = o.toString().trim();
        return s.isEmpty() ? null : s;
    }

    private static String parseCoverUrlFromBody(Object raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.toString().trim();
        if (s.isEmpty()) {
            return null;
        }
        String c = NewsCoverUrlUtil.sanitize(s);
        if (c == null) {
            throw new BusinessException("封面图地址无效，请使用 http(s) 或本站以 / 开头的路径（禁止 ..）");
        }
        return c;
    }

    private static int parseUrgencyLevel(Object raw, int defaultLevel) {
        if (raw == null) {
            return defaultLevel;
        }
        try {
            int v = Integer.parseInt(raw.toString().trim());
            if (v >= 1 && v <= 3) {
                return v;
            }
        } catch (NumberFormatException ignored) {
            /* fallthrough */
        }
        throw new BusinessException("紧急等级只能为 1（低）、2（中）、3（高）");
    }

    private static int parseNewsStatus(Object raw, int defaultStatus) {
        if (raw == null) {
            return defaultStatus;
        }
        try {
            int v = Integer.parseInt(raw.toString().trim());
            if (v == 0 || v == 1) {
                return v;
            }
        } catch (NumberFormatException ignored) {
            /* fallthrough */
        }
        throw new BusinessException("status 只能为 0（下架）或 1（上架）");
    }

    private LocalDateTime parsePublishTime(Object pt) {
        if (pt == null) {
            return null;
        }
        if (pt instanceof String s) {
            if (!StringUtils.hasText(s)) {
                return null;
            }
            return parsePublishTimeString(s.trim());
        }
        if (pt instanceof Long l) {
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(l), ZoneId.systemDefault());
        }
        if (pt instanceof Integer i) {
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(i.longValue()), ZoneId.systemDefault());
        }
        return parsePublishTimeString(pt.toString().trim());
    }

    private LocalDateTime parsePublishTimeString(String s) {
        for (DateTimeFormatter fmt : PUBLISH_TIME_FORMATTERS) {
            try {
                return LocalDateTime.parse(s, fmt);
            } catch (DateTimeParseException ignored) {
                /* try next */
            }
        }
        throw new BusinessException("发布时间格式无效，请使用 yyyy-MM-dd HH:mm:ss 或 ISO 本地日期时间");
    }
}
