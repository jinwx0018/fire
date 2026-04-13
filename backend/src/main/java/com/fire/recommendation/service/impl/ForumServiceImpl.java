package com.fire.recommendation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fire.recommendation.entity.ForumComment;
import com.fire.recommendation.entity.ForumPost;
import com.fire.recommendation.entity.SysRole;
import com.fire.recommendation.entity.SysUser;
import com.fire.recommendation.exception.BusinessException;
import com.fire.recommendation.common.ResultCode;
import com.fire.recommendation.mapper.ForumCommentMapper;
import com.fire.recommendation.mapper.ForumPostMapper;
import com.fire.recommendation.mapper.SysRoleMapper;
import com.fire.recommendation.mapper.SysUserMapper;
import com.fire.recommendation.component.CommentLikeService;
import com.fire.recommendation.component.DetailViewDedupHelper;
import com.fire.recommendation.component.ForumPostLikeService;
import com.fire.recommendation.constant.CollectionTargetType;
import com.fire.recommendation.service.ForumService;
import com.fire.recommendation.service.RecommendService;
import com.fire.recommendation.service.UserCollectionService;
import com.fire.recommendation.service.UserNotificationService;
import com.fire.recommendation.util.PlainTextSanitizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
public class ForumServiceImpl implements ForumService {

    /** 审核通过，对用户公开展示 */
    private static final int STATUS_APPROVED = 1;
    private static final int COMMENT_VISIBLE = 1;
    private static final int ADMIN_COMMENT_MAX_PAGE_SIZE = 100;

    private final ForumPostMapper postMapper;
    private final ForumCommentMapper commentMapper;
    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final RecommendService recommendService;
    private final CommentLikeService commentLikeService;
    private final UserNotificationService userNotificationService;
    private final UserCollectionService userCollectionService;
    private final DetailViewDedupHelper detailViewDedupHelper;
    private final ForumPostLikeService forumPostLikeService;

    @Override
    public IPage<Map<String, Object>> postList(Integer pageNum, Integer pageSize, String keyword) {
        Page<ForumPost> page = new Page<>(pageNum != null ? pageNum : 1, pageSize != null ? pageSize : 10);
        LambdaQueryWrapper<ForumPost> q = new LambdaQueryWrapper<>();
        q.eq(ForumPost::getDeleted, 0).eq(ForumPost::getStatus, STATUS_APPROVED);
        if (StringUtils.hasText(keyword)) {
            q.and(w -> w.like(ForumPost::getTitle, keyword).or().like(ForumPost::getContent, keyword));
        }
        q.orderByDesc(ForumPost::getCreateTime).orderByDesc(ForumPost::getId);
        return postMapper.selectPage(page, q).convert(this::postToMap);
    }

    @Override
    public IPage<Map<String, Object>> myPostList(Long userId, Integer pageNum, Integer pageSize, String keyword) {
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        Page<ForumPost> page = new Page<>(pageNum != null ? pageNum : 1, pageSize != null ? pageSize : 10);
        LambdaQueryWrapper<ForumPost> q = new LambdaQueryWrapper<>();
        q.eq(ForumPost::getDeleted, 0).eq(ForumPost::getUserId, userId);
        if (StringUtils.hasText(keyword)) {
            q.and(w -> w.like(ForumPost::getTitle, keyword).or().like(ForumPost::getContent, keyword));
        }
        q.orderByDesc(ForumPost::getCreateTime).orderByDesc(ForumPost::getId);
        return postMapper.selectPage(page, q).convert(this::postToMap);
    }

    @Override
    public IPage<Map<String, Object>> adminPostList(Integer pageNum, Integer pageSize, Integer status, String keyword) {
        Page<ForumPost> page = new Page<>(pageNum != null ? pageNum : 1, pageSize != null ? pageSize : 10);
        LambdaQueryWrapper<ForumPost> q = new LambdaQueryWrapper<>();
        q.eq(ForumPost::getDeleted, 0);
        if (status != null) {
            q.eq(ForumPost::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            q.and(w -> w.like(ForumPost::getTitle, keyword).or().like(ForumPost::getContent, keyword));
        }
        q.orderByDesc(ForumPost::getCreateTime).orderByDesc(ForumPost::getId);
        return postMapper.selectPage(page, q).convert(this::postToMap);
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

    /** 详情可见：已通过；或作者本人；或管理员 */
    private void assertCanViewPost(ForumPost p, Long viewerUserId) {
        int st = p.getStatus() != null ? p.getStatus() : 0;
        if (st == STATUS_APPROVED) {
            return;
        }
        if (viewerUserId != null && p.getUserId() != null && p.getUserId().equals(viewerUserId)) {
            return;
        }
        if (isAdmin(viewerUserId)) {
            return;
        }
        throw new BusinessException(ResultCode.NOT_FOUND);
    }

    /** 点赞/评论等互动：仅已通过审核的帖子 */
    private void assertApprovedForInteraction(ForumPost p) {
        int st = p.getStatus() != null ? p.getStatus() : 0;
        if (st != STATUS_APPROVED) {
            throw new BusinessException("帖子未通过审核，暂不可进行该操作");
        }
    }

    @Override
    public Map<String, Object> postDetail(Long postId, Long userId, boolean recordView, String clientIp) {
        ForumPost p = postMapper.selectById(postId);
        if (p == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        assertCanViewPost(p, userId);
        boolean bumpView = recordView
                && !isAdmin(userId)
                && !(userId != null && p.getUserId() != null && userId.equals(p.getUserId()))
                && detailViewDedupHelper.shouldIncrement("forum", postId, userId, clientIp);
        if (bumpView) {
            postMapper.update(null, new LambdaUpdateWrapper<ForumPost>()
                    .eq(ForumPost::getId, postId)
                    .setSql("view_count = IFNULL(view_count, 0) + 1"));
        }
        p = postMapper.selectById(postId);
        if (p == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        Map<String, Object> m = postToMap(p);
        int pst = p.getStatus() != null ? p.getStatus() : 0;
        boolean liked = pst == STATUS_APPROVED && userId != null && forumPostLikeService.isLiked(postId, userId);
        m.put("liked", liked);
        if (pst == STATUS_APPROVED && userId != null) {
            m.put("collected", userCollectionService.isCollected(userId, CollectionTargetType.FORUM_POST, postId));
        } else {
            m.put("collected", false);
        }
        return m;
    }

    @Override
    public Long publishPost(Long userId, String title, String content) {
        if (!StringUtils.hasText(title)) {
            throw new BusinessException("标题不能为空");
        }
        if (content == null) {
            content = "";
        }
        ForumPost p = new ForumPost();
        p.setUserId(userId);
        p.setTitle(title.trim());
        p.setContent(content);
        p.setStatus(0);
        p.setViewCount(0);
        p.setLikeCount(0);
        p.setCommentCount(0);
        postMapper.insert(p);
        SysUser author = userMapper.selectById(userId);
        String authorName = author != null && StringUtils.hasText(author.getUsername()) ? author.getUsername() : "用户";
        String titleShort = title.trim();
        if (titleShort.length() > 80) {
            titleShort = titleShort.substring(0, 80) + "…";
        }
        userNotificationService.notifyAllAdminsWithActionIgnoreFailure(
                "新的论坛帖子待审核",
                authorName + " 发布了帖子《" + titleShort + "》，请在论坛管理中审核。",
                "/forum/posts",
                "去审核");
        return p.getId();
    }

    @Override
    public void updatePost(Long postId, Long userId, String title, String content) {
        ForumPost p = postMapper.selectById(postId);
        if (p == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (!p.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        Integer oldStatus = p.getStatus();
        LambdaUpdateWrapper<ForumPost> uw = new LambdaUpdateWrapper<ForumPost>()
                .eq(ForumPost::getId, postId)
                .set(ForumPost::getStatus, 0)
                .set(ForumPost::getRejectReason, null);
        if (StringUtils.hasText(title)) {
            uw.set(ForumPost::getTitle, title.trim());
        }
        if (content != null) {
            uw.set(ForumPost::getContent, content);
        }
        postMapper.update(null, uw);
        if (oldStatus != null && oldStatus == -1) {
            SysUser author = userMapper.selectById(userId);
            String authorName = author != null && StringUtils.hasText(author.getUsername()) ? author.getUsername() : "用户";
            String t = p.getTitle() != null ? p.getTitle() : "";
            if (t.length() > 80) {
                t = t.substring(0, 80) + "…";
            }
            userNotificationService.notifyAllAdminsWithActionIgnoreFailure(
                    "论坛帖子重新提交审核",
                    authorName + " 在驳回后修改并重新提交了帖子《" + t + "》，请及时审核。",
                    "/forum/posts",
                    "去审核");
        }
    }

    @Override
    public void deletePost(Long postId, Long userId) {
        ForumPost p = postMapper.selectById(postId);
        if (p == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (!p.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        postMapper.deleteById(postId);
    }

    @Override
    public Map<String, Object> likePost(Long postId, Long userId) {
        ForumPost p = postMapper.selectById(postId);
        if (p == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        assertApprovedForInteraction(p);
        Map<String, Object> m = forumPostLikeService.toggle(postId, userId);
        if (Boolean.TRUE.equals(m.get("liked"))) {
            recommendService.recordBehavior(userId, "LIKE", "FORUM_POST", postId);
            Long authorId = p.getUserId();
            if (authorId != null && !authorId.equals(userId)) {
                SysUser liker = userMapper.selectById(userId);
                String likerName = liker != null ? liker.getUsername() : "有用户";
                userNotificationService.sendWithActionIgnoreFailure(
                        authorId,
                        "帖子获赞通知",
                        likerName + " 点赞了你的帖子《" + truncate(p.getTitle(), 40) + "》。",
                        "/forum/" + postId,
                        "去查看");
            }
        }
        return m;
    }

    private static void applyForumCommentSort(LambdaQueryWrapper<ForumComment> q, String sortBy) {
        if (sortBy != null && "hot".equalsIgnoreCase(sortBy.trim())) {
            q.orderByDesc(ForumComment::getLikeCount)
                    .orderByDesc(ForumComment::getCreateTime)
                    .orderByDesc(ForumComment::getId);
        } else {
            q.orderByDesc(ForumComment::getCreateTime)
                    .orderByDesc(ForumComment::getId);
        }
    }

    @Override
    public IPage<Map<String, Object>> commentList(Long postId, Integer pageNum, Integer pageSize, Long viewerUserId, String sortBy) {
        ForumPost p = postMapper.selectById(postId);
        if (p == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        assertCanViewPost(p, viewerUserId);
        Page<ForumComment> page = new Page<>(pageNum != null ? pageNum : 1, pageSize != null ? pageSize : 10);
        LambdaQueryWrapper<ForumComment> q = new LambdaQueryWrapper<>();
        q.eq(ForumComment::getPostId, postId)
                .eq(ForumComment::getDeleted, 0)
                .eq(ForumComment::getStatus, COMMENT_VISIBLE);
        applyForumCommentSort(q, sortBy);
        IPage<ForumComment> raw = commentMapper.selectPage(page, q);
        Map<Long, ForumComment> parentMap = loadParentComments(raw.getRecords());
        Map<Long, String> names = loadUsernamesForComments(raw.getRecords(), parentMap);
        return raw.convert(c -> commentToMap(c, parentMap, names, viewerUserId));
    }

    @Override
    public Long addComment(Long userId, Long postId, String content, Long parentId) {
        if (postId == null) {
            throw new BusinessException("postId 不能为空");
        }
        if (!StringUtils.hasText(content)) {
            throw new BusinessException("评论内容不能为空");
        }
        ForumPost post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        assertApprovedForInteraction(post);
        Long parentUserId = null;
        String parentContent = null;
        if (parentId != null) {
            ForumComment parent = commentMapper.selectById(parentId);
            if (parent == null) {
                throw new BusinessException(ResultCode.NOT_FOUND, "父评论不存在");
            }
            if (!postId.equals(parent.getPostId())) {
                throw new BusinessException("父评论不属于该帖子");
            }
            if (parent.getStatus() != null && !Integer.valueOf(COMMENT_VISIBLE).equals(parent.getStatus())) {
                throw new BusinessException("父评论已隐藏，无法回复");
            }
            parentUserId = parent.getUserId();
            parentContent = parent.getContent();
        }
        ForumComment c = new ForumComment();
        c.setUserId(userId);
        c.setPostId(postId);
        c.setContent(content.trim());
        c.setParentId(parentId);
        c.setStatus(COMMENT_VISIBLE);
        c.setLikeCount(0);
        commentMapper.insert(c);
        postMapper.update(null, new LambdaUpdateWrapper<ForumPost>().eq(ForumPost::getId, postId)
                .setSql("comment_count = comment_count + 1"));
        recommendService.recordBehavior(userId, "COMMENT", "FORUM_POST", postId);
        SysUser commenter = userMapper.selectById(userId);
        String commenterName = commenter != null && StringUtils.hasText(commenter.getUsername()) ? commenter.getUsername() : "用户";
        if (parentId == null) {
            Long ownerId = post.getUserId();
            if (ownerId != null && !ownerId.equals(userId)) {
                userNotificationService.sendWithActionIgnoreFailure(
                        ownerId,
                        "帖子有新评论",
                        commenterName + " 在您的帖子《" + truncate(post.getTitle(), 40) + "》下评论：「" + truncatePlain(c.getContent(), 100) + "」",
                        "/forum/" + postId + "?commentId=" + c.getId(),
                        "查看评论");
            }
        } else if (parentUserId != null && !parentUserId.equals(userId)) {
            userNotificationService.sendWithActionIgnoreFailure(
                    parentUserId,
                    "评论有新回复",
                    "帖子《" + truncate(post.getTitle(), 40) + "》下，您的评论「" + truncatePlain(parentContent, 50)
                            + "」收到新回复：「" + truncatePlain(c.getContent(), 80) + "」。",
                    "/forum/" + postId + "?commentId=" + c.getId(),
                    "查看回复");
        }
        userNotificationService.notifyAdminsNewCommentReview(
                "论坛",
                post.getTitle(),
                commenterName,
                truncatePlain(c.getContent(), 200),
                "/forum/comments");
        return c.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> likeComment(Long commentId, Long userId) {
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        ForumComment c = commentMapper.selectById(commentId);
        if (c == null || Integer.valueOf(1).equals(c.getDeleted())) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (c.getStatus() == null || !Integer.valueOf(COMMENT_VISIBLE).equals(c.getStatus())) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        ForumPost p = postMapper.selectById(c.getPostId());
        if (p == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        assertCanViewPost(p, userId);
        assertApprovedForInteraction(p);
        Map<String, Object> r = commentLikeService.toggle(
                CommentLikeService.REDIS_FORUM_COMMENT,
                CommentLikeService.CHAN_FORUM,
                commentId,
                userId,
                () -> {
                    ForumComment fresh = commentMapper.selectById(commentId);
                    return fresh != null && fresh.getLikeCount() != null ? fresh.getLikeCount() : 0;
                },
                cnt -> commentMapper.update(null, new LambdaUpdateWrapper<ForumComment>()
                        .eq(ForumComment::getId, commentId)
                        .set(ForumComment::getLikeCount, cnt)));
        if (Boolean.TRUE.equals(r.get("liked"))) {
            if (c.getUserId() != null && !Objects.equals(c.getUserId(), userId)) {
                userNotificationService.sendWithActionIgnoreFailure(
                        c.getUserId(),
                        "评论被点赞",
                        "帖子《" + truncate(p.getTitle(), 40) + "》下，您的评论「" + truncatePlain(c.getContent(), 60) + "」收到了一个新点赞。",
                        "/forum/" + p.getId() + "?commentId=" + c.getId(),
                        "查看评论");
            }
            try {
                recommendService.recordBehavior(userId, "LIKE", "FORUM_COMMENT", commentId);
            } catch (Exception e) {
                log.warn("推荐行为记录失败(已忽略): {}", e.getMessage());
            }
        }
        return r;
    }

    @Override
    public void deleteComment(Long commentId, Long userId) {
        ForumComment c = commentMapper.selectById(commentId);
        if (c == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        boolean isCommentOwner = userId != null && userId.equals(c.getUserId());
        if (!isCommentOwner) {
            ForumPost p = postMapper.selectById(c.getPostId());
            boolean isPostAuthor = p != null && userId != null && userId.equals(p.getUserId());
            if (!isPostAuthor) {
                throw new BusinessException(ResultCode.FORBIDDEN);
            }
        }
        Long pid = c.getPostId();
        commentMapper.deleteById(commentId);
        decrementPostCommentCount(pid);
    }

    @Override
    public void auditPost(Long postId, Integer status, String rejectReason) {
        if (status == null || (status != 1 && status != -1)) {
            throw new BusinessException("status 只能为 1（通过）或 -1（驳回）");
        }
        if (status == -1 && !StringUtils.hasText(rejectReason)) {
            throw new BusinessException("驳回时必须填写驳回理由");
        }
        ForumPost p = postMapper.selectById(postId);
        if (p == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        int oldStatus = p.getStatus() != null ? p.getStatus() : 0;
        p.setStatus(status);
        p.setRejectReason(status == 1 ? null : rejectReason.trim());
        postMapper.updateById(p);

        Long authorId = p.getUserId();
        if (authorId != null) {
            String titleShort = truncate(p.getTitle(), 80);
            if (status == 1 && oldStatus != 1) {
                userNotificationService.sendWithActionIgnoreFailure(
                        authorId,
                        "论坛帖子已通过审核",
                        "您的帖子《" + titleShort + "》已通过管理员审核，现已在论坛列表中展示。",
                        "/forum/" + postId,
                        "查看帖子");
            } else if (status == -1 && oldStatus != -1) {
                String reason = rejectReason != null ? rejectReason.trim() : "";
                if (reason.length() > 200) {
                    reason = reason.substring(0, 200) + "…";
                }
                userNotificationService.sendWithActionIgnoreFailure(
                        authorId,
                        "论坛帖子未通过审核",
                        "您的帖子《" + titleShort + "》未通过审核。驳回理由：" + (reason.isEmpty() ? "（未填写）" : reason),
                        "/forum/mine",
                        "查看我的帖子");
            }
        }
    }

    @Override
    public void adminDeletePost(Long postId) {
        ForumPost p = postMapper.selectById(postId);
        if (p == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        postMapper.deleteById(postId);
    }

    @Override
    public void adminDeleteComment(Long commentId) {
        ForumComment c = commentMapper.selectById(commentId);
        if (c == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        Long pid = c.getPostId();
        commentMapper.deleteById(commentId);
        decrementPostCommentCount(pid);
    }

    @Override
    public IPage<Map<String, Object>> adminCommentPage(Integer pageNum, Integer pageSize, Long postId, String keyword, Integer status) {
        int pn = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int ps = pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, ADMIN_COMMENT_MAX_PAGE_SIZE);
        Page<ForumComment> page = new Page<>(pn, ps);
        LambdaQueryWrapper<ForumComment> q = new LambdaQueryWrapper<>();
        q.eq(ForumComment::getDeleted, 0).orderByDesc(ForumComment::getCreateTime);
        if (postId != null) {
            q.eq(ForumComment::getPostId, postId);
        }
        if (StringUtils.hasText(keyword)) {
            q.like(ForumComment::getContent, keyword.trim());
        }
        if (status != null) {
            q.eq(ForumComment::getStatus, status);
        }
        IPage<ForumComment> result = commentMapper.selectPage(page, q);
        Set<Long> postIds = result.getRecords().stream().map(ForumComment::getPostId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> postTitles = new HashMap<>();
        if (!postIds.isEmpty()) {
            for (ForumPost fp : postMapper.selectBatchIds(new ArrayList<>(postIds))) {
                if (fp != null && fp.getId() != null) {
                    postTitles.put(fp.getId(), fp.getTitle());
                }
            }
        }
        Map<Long, ForumComment> parentMap = loadParentComments(result.getRecords());
        Map<Long, String> names = loadUsernamesForComments(result.getRecords(), parentMap);
        return result.convert(c -> {
            Map<String, Object> m = commentToMap(c, parentMap, names, null);
            m.put("postTitle", postTitles.get(c.getPostId()));
            m.put("status", c.getStatus());
            return m;
        });
    }

    @Override
    public void adminUpdateCommentStatus(Long commentId, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("status 只能为 0（隐藏）或 1（显示）");
        }
        ForumComment c = commentMapper.selectById(commentId);
        if (c == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        c.setStatus(status);
        commentMapper.updateById(c);
    }

    @Override
    public Map<String, Object> adminPostDetail(Long postId) {
        if (postId == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        ForumPost p = postMapper.selectById(postId);
        if (p == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return postToMap(p);
    }

    @Override
    public Map<String, Object> adminCommentAuditDetail(Long commentId) {
        if (commentId == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        ForumComment c = commentMapper.selectById(commentId);
        if (c == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        ForumPost post = postMapper.selectById(c.getPostId());
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        Map<Long, ForumComment> parentMap = loadParentComments(List.of(c));
        Map<Long, String> names = loadUsernamesForComments(List.of(c), parentMap);
        Map<String, Object> comment = commentToMap(c, parentMap, names, null);
        comment.put("status", c.getStatus());

        Map<String, Object> parentComment = null;
        if (c.getParentId() != null) {
            ForumComment p = commentMapper.selectById(c.getParentId());
            if (p != null) {
                parentComment = new HashMap<>();
                parentComment.put("id", p.getId());
                parentComment.put("userId", p.getUserId());
                parentComment.put("content", p.getContent());
                parentComment.put("status", p.getStatus());
                parentComment.put("createTime", p.getCreateTime());
                SysUser pu = userMapper.selectById(p.getUserId());
                parentComment.put("userName", pu != null ? pu.getUsername() : null);
            }
        }

        Map<String, Object> out = new HashMap<>();
        out.put("comment", comment);
        out.put("parentComment", parentComment);
        out.put("post", postToMap(post));
        return out;
    }

    private void decrementPostCommentCount(Long postId) {
        if (postId == null) {
            return;
        }
        postMapper.update(null, new LambdaUpdateWrapper<ForumPost>()
                .eq(ForumPost::getId, postId)
                .setSql("comment_count = GREATEST(IFNULL(comment_count, 0) - 1, 0)"));
    }

    private Map<String, Object> postToMap(ForumPost p) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", p.getId());
        m.put("userId", p.getUserId());
        SysUser u = userMapper.selectById(p.getUserId());
        m.put("userName", u != null ? u.getUsername() : null);
        m.put("avatar", u != null ? u.getAvatar() : null);
        m.put("title", p.getTitle());
        m.put("content", p.getContent());
        m.put("status", p.getStatus());
        m.put("rejectReason", p.getRejectReason());
        m.put("viewCount", p.getViewCount());
        m.put("likeCount", p.getLikeCount());
        m.put("commentCount", p.getCommentCount());
        m.put("createTime", p.getCreateTime());
        return m;
    }

    private Map<String, Object> commentToMap(ForumComment c, Map<Long, ForumComment> parentMap, Map<Long, String> usernameById, Long viewerUserId) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", c.getId());
        m.put("userId", c.getUserId());
        m.put("postId", c.getPostId());
        m.put("content", c.getContent());
        m.put("parentId", c.getParentId());
        m.put("createTime", c.getCreateTime());
        m.put("likeCount", commentLikeService.resolveLikeCount(
                CommentLikeService.REDIS_FORUM_COMMENT, CommentLikeService.CHAN_FORUM, c.getId(), c.getLikeCount()));
        if (viewerUserId != null) {
            m.put("liked", commentLikeService.isLiked(CommentLikeService.REDIS_FORUM_COMMENT, CommentLikeService.CHAN_FORUM, c.getId(), viewerUserId));
        } else {
            m.put("liked", false);
        }
        SysUser self = userMapper.selectById(c.getUserId());
        if (usernameById != null) {
            m.put("userName", usernameById.get(c.getUserId()));
        } else {
            m.put("userName", self != null ? self.getUsername() : null);
        }
        m.put("avatar", self != null ? self.getAvatar() : null);
        if (c.getParentId() != null && parentMap != null) {
            ForumComment p = parentMap.get(c.getParentId());
            if (p != null) {
                String pu = usernameById != null ? usernameById.get(p.getUserId()) : null;
                if (pu == null) {
                    SysUser puUser = userMapper.selectById(p.getUserId());
                    pu = puUser != null ? puUser.getUsername() : null;
                }
                m.put("parentUserName", pu);
                m.put("parentContentPreview", previewCommentText(p.getContent()));
            }
        }
        return m;
    }

    /** 与新闻/知识评论一致：纯文本 + 仅图时「[图片]」，避免仅图评论预览为空 */
    private static String previewCommentText(String s) {
        if (s == null) {
            return "";
        }
        String more = PlainTextSanitizer.plainPreviewForNotify(s, 4000);
        if (more.length() <= 280) {
            return more;
        }
        return more.substring(0, 280) + "…";
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    private static String truncatePlain(String s, int max) {
        if (s == null) {
            return "";
        }
        String t = s.replaceAll("<[^>]+>", " ")
                .replace("&nbsp;", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (t.length() <= max) {
            return t;
        }
        return t.substring(0, max) + "…";
    }

    private Map<Long, ForumComment> loadParentComments(List<ForumComment> records) {
        Set<Long> parentIds = records.stream()
                .map(ForumComment::getParentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, ForumComment> map = new HashMap<>();
        if (parentIds.isEmpty()) {
            return map;
        }
        for (ForumComment row : records) {
            if (row.getId() != null && parentIds.contains(row.getId())) {
                map.put(row.getId(), row);
            }
        }
        List<Long> missing = parentIds.stream().filter(pid -> !map.containsKey(pid)).collect(Collectors.toList());
        if (!missing.isEmpty()) {
            for (ForumComment pc : commentMapper.selectBatchIds(missing)) {
                if (pc != null && pc.getId() != null) {
                    map.put(pc.getId(), pc);
                }
            }
        }
        for (Long pid : parentIds) {
            if (!map.containsKey(pid)) {
                ForumComment one = commentMapper.selectById(pid);
                if (one != null && one.getId() != null) {
                    map.put(one.getId(), one);
                }
            }
        }
        return map;
    }

    private Map<Long, String> loadUsernamesForComments(List<ForumComment> records, Map<Long, ForumComment> parentMap) {
        Set<Long> uids = new HashSet<>();
        for (ForumComment c : records) {
            if (c.getUserId() != null) {
                uids.add(c.getUserId());
            }
        }
        for (ForumComment p : parentMap.values()) {
            if (p.getUserId() != null) {
                uids.add(p.getUserId());
            }
        }
        Map<Long, String> names = new HashMap<>();
        for (Long uid : uids) {
            SysUser u = userMapper.selectById(uid);
            if (u != null && StringUtils.hasText(u.getUsername())) {
                names.put(uid, u.getUsername());
            }
        }
        return names;
    }
}
