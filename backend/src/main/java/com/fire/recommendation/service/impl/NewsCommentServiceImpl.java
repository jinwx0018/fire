package com.fire.recommendation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fire.recommendation.entity.News;
import com.fire.recommendation.entity.NewsComment;
import com.fire.recommendation.entity.SysUser;
import com.fire.recommendation.exception.BusinessException;
import com.fire.recommendation.common.ResultCode;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fire.recommendation.component.CommentLikeService;
import com.fire.recommendation.mapper.NewsCommentMapper;
import com.fire.recommendation.mapper.NewsMapper;
import com.fire.recommendation.mapper.SysUserMapper;
import com.fire.recommendation.service.AuditLogService;
import com.fire.recommendation.service.NewsCommentService;
import com.fire.recommendation.service.RecommendService;
import com.fire.recommendation.service.UserNotificationService;
import com.fire.recommendation.util.PlainTextSanitizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsCommentServiceImpl implements NewsCommentService {

    private static final int ADMIN_COMMENT_MAX_PAGE_SIZE = 100;
    private static final int ADMIN_COMMENT_MAX_PAGE_NUM = 10_000;
    private static final int ADMIN_COMMENT_KEYWORD_MAX = 200;

    private final NewsCommentMapper newsCommentMapper;
    private final NewsMapper newsMapper;
    private final SysUserMapper sysUserMapper;
    private final UserNotificationService userNotificationService;
    private final AuditLogService auditLogService;
    private final CommentLikeService commentLikeService;
    private final RecommendService recommendService;

    /** time=最新；hot=点赞数优先 */
    private static void applyNewsCommentSort(LambdaQueryWrapper<NewsComment> q, String sortBy) {
        if (sortBy != null && "hot".equalsIgnoreCase(sortBy.trim())) {
            q.orderByDesc(NewsComment::getLikeCount)
                    .orderByDesc(NewsComment::getCreateTime)
                    .orderByDesc(NewsComment::getId);
        } else {
            q.orderByDesc(NewsComment::getCreateTime)
                    .orderByDesc(NewsComment::getId);
        }
    }

    private News assertNewsCommentable(Long newsId) {
        News n = newsMapper.selectById(newsId);
        if (n == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (n.getStatus() == null || !Integer.valueOf(1).equals(n.getStatus())) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (n.getPublishTime() != null && n.getPublishTime().isAfter(LocalDateTime.now())) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return n;
    }

    @Override
    public IPage<Map<String, Object>> pageForNews(Long newsId, Integer pageNum, Integer pageSize, Long viewerUserId, String sortBy) {
        assertNewsCommentable(newsId);
        Page<NewsComment> page = new Page<>(pageNum != null ? pageNum : 1, pageSize != null ? pageSize : 10);
        LambdaQueryWrapper<NewsComment> q = new LambdaQueryWrapper<NewsComment>()
                .eq(NewsComment::getNewsId, newsId)
                .eq(NewsComment::getStatus, 1);
        applyNewsCommentSort(q, sortBy);
        IPage<NewsComment> result = newsCommentMapper.selectPage(page, q);
        Map<Long, NewsComment> parentMap = loadNewsParents(result.getRecords());
        Map<Long, String> names = loadNewsUsernames(result.getRecords(), parentMap);
        return result.convert(c -> toNewsCommentMap(c, parentMap, names, viewerUserId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> toggleCommentLike(Long commentId, Long userId) {
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        NewsComment c = newsCommentMapper.selectById(commentId);
        if (c == null || Integer.valueOf(1).equals(c.getDeleted())) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (c.getStatus() == null || !Integer.valueOf(1).equals(c.getStatus())) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        News news = assertNewsCommentable(c.getNewsId());
        Map<String, Object> r = commentLikeService.toggle(
                CommentLikeService.REDIS_NEWS_COMMENT,
                CommentLikeService.CHAN_NEWS,
                commentId,
                userId,
                () -> {
                    NewsComment fresh = newsCommentMapper.selectById(commentId);
                    return fresh != null && fresh.getLikeCount() != null ? fresh.getLikeCount() : 0;
                },
                cnt -> newsCommentMapper.update(null, new LambdaUpdateWrapper<NewsComment>()
                        .eq(NewsComment::getId, commentId)
                        .set(NewsComment::getLikeCount, cnt)));
        if (Boolean.TRUE.equals(r.get("liked"))) {
            if (c.getUserId() != null && !Objects.equals(c.getUserId(), userId)) {
                userNotificationService.sendWithActionIgnoreFailure(
                        c.getUserId(),
                        "评论被点赞",
                        "新闻《" + truncate(news.getTitle(), 40) + "》下，您的评论「"
                                + truncate(previewNewsCommentText(c.getContent()), 60) + "」收到了一个新点赞。",
                        "/news/" + news.getId() + "?commentId=" + c.getId(),
                        "查看评论");
            }
            try {
                recommendService.recordBehavior(userId, "LIKE", "NEWS_COMMENT", commentId);
            } catch (Exception e) {
                log.warn("推荐行为记录失败(已忽略): {}", e.getMessage());
            }
        }
        return r;
    }

    @Override
    public IPage<Map<String, Object>> adminPage(Integer pageNum, Integer pageSize, Long newsId, String contentKeyword, Integer status) {
        int pn = pageNum == null || pageNum < 1 ? 1 : Math.min(pageNum, ADMIN_COMMENT_MAX_PAGE_NUM);
        int ps = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, ADMIN_COMMENT_MAX_PAGE_SIZE);
        Page<NewsComment> page = new Page<>(pn, ps);
        LambdaQueryWrapper<NewsComment> q = new LambdaQueryWrapper<NewsComment>()
                .orderByDesc(NewsComment::getCreateTime);
        if (status != null) {
            q.eq(NewsComment::getStatus, status);
        }
        if (newsId != null) {
            q.eq(NewsComment::getNewsId, newsId);
        }
        if (StringUtils.hasText(contentKeyword)) {
            String k = contentKeyword.trim();
            if (k.length() > ADMIN_COMMENT_KEYWORD_MAX) {
                k = k.substring(0, ADMIN_COMMENT_KEYWORD_MAX);
            }
            q.like(NewsComment::getContent, k);
        }
        IPage<NewsComment> result = newsCommentMapper.selectPage(page, q);
        Set<Long> nids = result.getRecords().stream().map(NewsComment::getNewsId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> newsTitles = new HashMap<>();
        if (!nids.isEmpty()) {
            List<News> newsRows = newsMapper.selectBatchIds(new ArrayList<>(nids));
            for (News n : newsRows) {
                if (n != null && n.getId() != null) {
                    newsTitles.put(n.getId(), PlainTextSanitizer.sanitizeTitleOutput(n.getTitle()));
                }
            }
        }
        Map<Long, NewsComment> parentMap = loadNewsParents(result.getRecords());
        Map<Long, String> names = loadNewsUsernames(result.getRecords(), parentMap);
        return result.convert(c -> {
            Map<String, Object> m = toNewsCommentMap(c, parentMap, names, null);
            m.put("newsId", c.getNewsId());
            m.put("newsTitle", newsTitles.get(c.getNewsId()));
            m.put("status", c.getStatus());
            return m;
        });
    }

    @Override
    public void post(Long newsId, Long userId, String content, Long parentId) {
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        News news = assertNewsCommentable(newsId);
        // 与知识/论坛评论一致：保留受控富文本（img/br/p），避免纯文本消毒把图片整段去掉
        String text = PlainTextSanitizer.sanitizeRichCommentBody(content);
        if (PlainTextSanitizer.isBlankRichComment(text)) {
            throw new BusinessException("评论内容不能为空");
        }
        if (parentId != null) {
            NewsComment parent = newsCommentMapper.selectById(parentId);
            if (parent == null || !newsId.equals(parent.getNewsId()) || Integer.valueOf(1).equals(parent.getDeleted())) {
                throw new BusinessException("回复的评论不存在");
            }
            if (parent.getStatus() != null && !Integer.valueOf(1).equals(parent.getStatus())) {
                throw new BusinessException("父评论已隐藏，无法回复");
            }
        }
        NewsComment c = new NewsComment();
        c.setNewsId(newsId);
        c.setUserId(userId);
        c.setParentId(parentId);
        c.setContent(text);
        c.setStatus(1);
        c.setLikeCount(0);
        newsCommentMapper.insert(c);

        SysUser commenterUser = sysUserMapper.selectById(userId);
        String commenterName = commenterUser != null && StringUtils.hasText(commenterUser.getUsername())
                ? commenterUser.getUsername() : "用户";

        if (parentId == null) {
            Long pub = news.getPublisherId();
            if (pub != null && !pub.equals(userId)) {
                userNotificationService.sendWithActionIgnoreFailure(
                        pub,
                        "新闻有新评论",
                        commenterName + " 对您发布的新闻《" + truncate(news.getTitle(), 40) + "》发表评论：「"
                                + previewNewsCommentText(c.getContent()) + "」",
                        "/news/" + news.getId() + "?commentId=" + c.getId(),
                        "查看评论");
            }
        } else {
            NewsComment parent = newsCommentMapper.selectById(parentId);
            if (parent != null && parent.getUserId() != null && !parent.getUserId().equals(userId)) {
                userNotificationService.sendWithActionIgnoreFailure(
                        parent.getUserId(),
                        "评论有新回复",
                        "新闻《" + truncate(news.getTitle(), 40) + "》下，您的评论「"
                                + truncate(previewNewsCommentText(parent.getContent()), 50)
                                + "」收到新回复：「" + truncate(previewNewsCommentText(c.getContent()), 80) + "」。",
                        "/news/" + news.getId() + "?commentId=" + c.getId(),
                        "查看回复");
            }
        }

        userNotificationService.notifyAdminsNewCommentReview(
                "新闻",
                news.getTitle(),
                commenterName,
                previewNewsCommentText(c.getContent()),
                "/news/comments");
    }

    @Override
    public void deleteOwn(Long commentId, Long userId) {
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        NewsComment c = newsCommentMapper.selectById(commentId);
        if (c == null || Integer.valueOf(1).equals(c.getDeleted())) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (!Objects.equals(c.getUserId(), userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        newsCommentMapper.deleteById(commentId);
    }

    @Override
    public void adminDelete(Long commentId, Long operatorId) {
        NewsComment c = newsCommentMapper.selectById(commentId);
        if (c == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        newsCommentMapper.deleteById(commentId);
        auditLogService.log(operatorId, "NEWS_COMMENT_DELETE", "NEWS_COMMENT", commentId, "管理端删除新闻评论 newsId=" + c.getNewsId());
    }

    @Override
    public void adminSetStatus(Long commentId, Integer status, Long operatorId) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("status 只能为 0（隐藏）或 1（显示）");
        }
        NewsComment c = newsCommentMapper.selectById(commentId);
        if (c == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        c.setStatus(status);
        newsCommentMapper.updateById(c);
        auditLogService.log(operatorId, "NEWS_COMMENT_STATUS", "NEWS_COMMENT", commentId, "管理端设置新闻评论状态=" + status);
    }

    @Override
    public Map<String, Object> adminAuditDetail(Long commentId) {
        if (commentId == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        NewsComment c = newsCommentMapper.selectById(commentId);
        if (c == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        News news = newsMapper.selectById(c.getNewsId());
        if (news == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        Map<String, Object> comment = new HashMap<>();
        comment.put("id", c.getId());
        comment.put("newsId", c.getNewsId());
        comment.put("userId", c.getUserId());
        comment.put("parentId", c.getParentId());
        comment.put("content", PlainTextSanitizer.formatCommentForApi(c.getContent()));
        comment.put("status", c.getStatus());
        comment.put("createTime", c.getCreateTime());
        SysUser cu = sysUserMapper.selectById(c.getUserId());
        comment.put("username", cu != null ? cu.getUsername() : null);

        Map<String, Object> parentComment = null;
        if (c.getParentId() != null) {
            NewsComment p = newsCommentMapper.selectById(c.getParentId());
            if (p != null) {
                parentComment = new HashMap<>();
                parentComment.put("id", p.getId());
                parentComment.put("userId", p.getUserId());
                parentComment.put("content", PlainTextSanitizer.formatCommentForApi(p.getContent()));
                parentComment.put("status", p.getStatus());
                parentComment.put("createTime", p.getCreateTime());
                SysUser pu = sysUserMapper.selectById(p.getUserId());
                parentComment.put("username", pu != null ? pu.getUsername() : null);
            }
        }

        Map<String, Object> newsMap = new HashMap<>();
        newsMap.put("id", news.getId());
        newsMap.put("title", PlainTextSanitizer.sanitizeTitleOutput(news.getTitle()));
        newsMap.put("summary", news.getSummary());
        newsMap.put("content", news.getContent());
        newsMap.put("status", news.getStatus());
        newsMap.put("publishTime", news.getPublishTime());

        Map<String, Object> out = new HashMap<>();
        out.put("comment", comment);
        out.put("parentComment", parentComment);
        out.put("news", newsMap);
        return out;
    }

    private Map<String, Object> toNewsCommentMap(NewsComment c, Map<Long, NewsComment> parentMap, Map<Long, String> names, Long viewerUserId) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", c.getId());
        m.put("userId", c.getUserId());
        m.put("username", names.get(c.getUserId()));
        m.put("parentId", c.getParentId());
        m.put("content", PlainTextSanitizer.formatCommentForApi(c.getContent()));
        m.put("createTime", c.getCreateTime());
        m.put("likeCount", commentLikeService.resolveLikeCount(
                CommentLikeService.REDIS_NEWS_COMMENT, CommentLikeService.CHAN_NEWS, c.getId(), c.getLikeCount()));
        if (viewerUserId != null) {
            m.put("liked", commentLikeService.isLiked(CommentLikeService.REDIS_NEWS_COMMENT, CommentLikeService.CHAN_NEWS, c.getId(), viewerUserId));
        } else {
            m.put("liked", false);
        }
        if (c.getParentId() != null && parentMap != null) {
            NewsComment p = parentMap.get(c.getParentId());
            if (p != null) {
                m.put("parentUserName", names.get(p.getUserId()));
                m.put("parentContentPreview", previewNewsCommentText(p.getContent()));
            }
        }
        return m;
    }

    /** 父评论预览与通知文案：去标签纯文本，仅图无字时为「[图片]」，避免前端文本插值出现 HTML 乱码。 */
    private static String previewNewsCommentText(String s) {
        if (s == null) {
            return "";
        }
        String more = PlainTextSanitizer.plainPreviewForNotify(s, 2000);
        if (more.length() <= 280) {
            return more;
        }
        return more.substring(0, 280) + "…";
    }

    /**
     * 父评论可能不在本页（按时间倒序分页时常见），必须按 id 再查库；
     * 同时先把本页中出现的行并入 map，避免个别环境下 batch 未命中导致引用区空白。
     */
    private Map<Long, NewsComment> loadNewsParents(List<NewsComment> records) {
        Set<Long> parentIds = records.stream()
                .map(NewsComment::getParentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, NewsComment> map = new HashMap<>();
        if (parentIds.isEmpty()) {
            return map;
        }
        for (NewsComment row : records) {
            if (row.getId() != null && parentIds.contains(row.getId())) {
                map.put(row.getId(), row);
            }
        }
        List<Long> missing = parentIds.stream().filter(pid -> !map.containsKey(pid)).collect(Collectors.toList());
        if (!missing.isEmpty()) {
            for (NewsComment pc : newsCommentMapper.selectBatchIds(missing)) {
                if (pc != null && pc.getId() != null) {
                    map.put(pc.getId(), pc);
                }
            }
        }
        for (Long pid : parentIds) {
            if (!map.containsKey(pid)) {
                NewsComment one = newsCommentMapper.selectById(pid);
                if (one != null && one.getId() != null) {
                    map.put(one.getId(), one);
                }
            }
        }
        return map;
    }

    private Map<Long, String> loadNewsUsernames(List<NewsComment> records, Map<Long, NewsComment> parentMap) {
        Set<Long> uids = new HashSet<>();
        for (NewsComment c : records) {
            if (c.getUserId() != null) {
                uids.add(c.getUserId());
            }
        }
        for (NewsComment p : parentMap.values()) {
            if (p.getUserId() != null) {
                uids.add(p.getUserId());
            }
        }
        Map<Long, String> names = new HashMap<>();
        if (uids.isEmpty()) {
            return names;
        }
        List<SysUser> users = sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>().in(SysUser::getId, uids));
        for (SysUser u : users) {
            if (u != null && StringUtils.hasText(u.getUsername())) {
                names.put(u.getId(), u.getUsername());
            }
        }
        return names;
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}
