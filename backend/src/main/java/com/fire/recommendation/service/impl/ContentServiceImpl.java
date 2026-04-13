package com.fire.recommendation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fire.recommendation.common.PageResult;
import com.fire.recommendation.entity.KnowledgeCategory;
import com.fire.recommendation.entity.KnowledgeContent;
import com.fire.recommendation.entity.KnowledgeContentComment;
import com.fire.recommendation.entity.UserContentLike;
import com.fire.recommendation.entity.UserNotification;
import com.fire.recommendation.entity.SysRole;
import com.fire.recommendation.entity.SysUser;
import com.fire.recommendation.exception.BusinessException;
import com.fire.recommendation.common.ResultCode;
import com.fire.recommendation.mapper.AdminRecycleMapper;
import com.fire.recommendation.mapper.ForumCommentMapper;
import com.fire.recommendation.mapper.ForumPostMapper;
import com.fire.recommendation.mapper.KnowledgeCategoryMapper;
import com.fire.recommendation.mapper.KnowledgeContentCommentMapper;
import com.fire.recommendation.mapper.KnowledgeContentMapper;
import com.fire.recommendation.mapper.NewsCommentMapper;
import com.fire.recommendation.mapper.NewsLikeMapper;
import com.fire.recommendation.mapper.NewsMapper;
import com.fire.recommendation.mapper.UserContentLikeMapper;
import com.fire.recommendation.mapper.UserNotificationMapper;
import com.fire.recommendation.mapper.SysRoleMapper;
import com.fire.recommendation.mapper.SysUserMapper;
import com.fire.recommendation.component.CommentLikeService;
import com.fire.recommendation.component.DetailViewDedupHelper;
import com.fire.recommendation.component.NotificationRealtimePublisher;
import com.fire.recommendation.constant.CollectionTargetType;
import com.fire.recommendation.service.ContentService;
import com.fire.recommendation.service.RecommendService;
import com.fire.recommendation.service.UserCollectionService;
import com.fire.recommendation.service.AuditLogService;
import com.fire.recommendation.service.UserNotificationService;
import com.fire.recommendation.util.PlainTextSanitizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentServiceImpl implements ContentService {

    /** 与 DB user_notification.title VARCHAR(128)、content VARCHAR(1024) 对齐 */
    private static final int USER_NOTIFY_TITLE_MAX = 128;
    private static final int USER_NOTIFY_CONTENT_MAX = 1024;

    private final KnowledgeContentMapper contentMapper;
    private final AdminRecycleMapper adminRecycleMapper;
    private final NewsMapper newsMapper;
    private final NewsLikeMapper newsLikeMapper;
    private final NewsCommentMapper newsCommentMapper;
    private final ForumPostMapper forumPostMapper;
    private final ForumCommentMapper forumCommentMapper;
    private final KnowledgeCategoryMapper categoryMapper;
    private final UserCollectionService userCollectionService;
    private final UserContentLikeMapper userContentLikeMapper;
    private final UserNotificationMapper userNotificationMapper;
    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final RecommendService recommendService;
    private final AuditLogService auditLogService;
    private final KnowledgeContentCommentMapper knowledgeContentCommentMapper;
    private final CommentLikeService commentLikeService;
    private final DetailViewDedupHelper detailViewDedupHelper;
    private final UserNotificationService userNotificationService;
    private final NotificationRealtimePublisher notificationRealtimePublisher;

    private static final int STATUS_DRAFT = 0;
    private static final int STATUS_PUBLISHED = 1;
    private static final int STATUS_OFFLINE = 2;
    private static final int STATUS_PENDING = 3;
    private static final String USER_CONTENT_LIKE_TABLE = "user_content_like";

    private static final int ADMIN_K_COMMENT_MAX_PAGE_SIZE = 100;
    private static final int ADMIN_K_COMMENT_MAX_PAGE_NUM = 10_000;
    private static final int ADMIN_K_COMMENT_KEYWORD_MAX = 200;

    @Value("${app.client-base-url:http://localhost:5173}")
    private String clientBaseUrl;

    @Override
    public IPage<Map<String, Object>> list(Integer pageNum, Integer pageSize, Long categoryId, String title, String sortBy, String timeOrder) {
        Page<KnowledgeContent> page = new Page<>(pageNum != null ? pageNum : 1, pageSize != null ? pageSize : 10);
        LambdaQueryWrapper<KnowledgeContent> q = new LambdaQueryWrapper<>();
        q.eq(KnowledgeContent::getDeleted, 0);
        if (categoryId != null) q.eq(KnowledgeContent::getCategoryId, categoryId);
        if (StringUtils.hasText(title)) q.like(KnowledgeContent::getTitle, title);
        // 公开列表：仅已发布；忽略 status 参数，防止传入 0/3 浏览草稿或待审核
        q.eq(KnowledgeContent::getStatus, STATUS_PUBLISHED);
        if ("hot".equalsIgnoreCase(sortBy)) {
            q.orderByDesc(KnowledgeContent::getViewCount)
                    .orderByDesc(KnowledgeContent::getCreateTime)
                    .orderByDesc(KnowledgeContent::getId);
        } else {
            boolean asc = "asc".equalsIgnoreCase(timeOrder);
            if (asc) {
                q.orderByAsc(KnowledgeContent::getCreateTime).orderByAsc(KnowledgeContent::getId);
            } else {
                q.orderByDesc(KnowledgeContent::getCreateTime).orderByDesc(KnowledgeContent::getId);
            }
        }
        IPage<KnowledgeContent> result = contentMapper.selectPage(page, q);
        return result.convert(this::toListMap);
    }

    @Override
    public IPage<Map<String, Object>> adminContentPage(Integer pageNum, Integer pageSize, Long categoryId, String title, Integer status) {
        Page<KnowledgeContent> page = new Page<>(pageNum != null ? pageNum : 1, pageSize != null ? pageSize : 10);
        LambdaQueryWrapper<KnowledgeContent> q = new LambdaQueryWrapper<>();
        q.eq(KnowledgeContent::getDeleted, 0);
        if (categoryId != null) q.eq(KnowledgeContent::getCategoryId, categoryId);
        if (StringUtils.hasText(title)) q.like(KnowledgeContent::getTitle, title);
        if (status != null) q.eq(KnowledgeContent::getStatus, status);
        q.orderByDesc(KnowledgeContent::getCreateTime).orderByDesc(KnowledgeContent::getId);
        IPage<KnowledgeContent> result = contentMapper.selectPage(page, q);
        return result.convert(this::toListMap);
    }

    @Override
    public PageResult<Map<String, Object>> adminRecyclePage(Integer pageNum, Integer pageSize) {
        long current = pageNum != null ? pageNum : 1;
        long size = pageSize != null ? pageSize : 10;
        long offset = (current - 1) * size;
        List<Map<String, Object>> list = adminRecycleMapper.selectUnifiedRecyclePage(offset, size);
        long total = adminRecycleMapper.countUnifiedRecycle();
        return PageResult.of(list, total, (int) current, (int) size);
    }

    @Override
    public IPage<Map<String, Object>> myDrafts(Long authorId, Integer pageNum, Integer pageSize, Long categoryId, String title) {
        Page<KnowledgeContent> page = new Page<>(pageNum != null ? pageNum : 1, pageSize != null ? pageSize : 10);
        LambdaQueryWrapper<KnowledgeContent> q = new LambdaQueryWrapper<>();
        q.eq(KnowledgeContent::getDeleted, 0);
        q.eq(KnowledgeContent::getAuthorId, authorId);
        // 作者「我的知识」返回全部状态，避免已发布/下架内容在作者侧丢失
        if (categoryId != null) q.eq(KnowledgeContent::getCategoryId, categoryId);
        if (StringUtils.hasText(title)) q.like(KnowledgeContent::getTitle, title);
        q.orderByDesc(KnowledgeContent::getCreateTime).orderByDesc(KnowledgeContent::getId);
        IPage<KnowledgeContent> result = contentMapper.selectPage(page, q);
        return result.convert(this::toListMap);
    }

    /** 作者可管理自己的稿件；管理员可管理全部 */
    private void assertCanManageContent(Long operatorId, KnowledgeContent c) {
        if (c == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        SysUser operator = userMapper.selectById(operatorId);
        if (operator == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        SysRole role = roleMapper.selectById(operator.getRoleId());
        if (role != null && "ADMIN".equals(role.getRoleCode())) {
            return;
        }
        if (c.getAuthorId() != null && c.getAuthorId().equals(operatorId)) {
            return;
        }
        throw new BusinessException(ResultCode.FORBIDDEN, "无权操作该内容");
    }

    @Override
    public Map<String, Object> getDetail(Long id, Long userId, boolean recordView, String clientIp) {
        KnowledgeContent c = contentMapper.selectById(id);
        if (c == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        boolean isAuthor = userId != null && userId.equals(c.getAuthorId());
        boolean isAdmin = false;
        if (userId != null && !isAuthor) {
            SysUser u = userMapper.selectById(userId);
            if (u != null) {
                SysRole r = roleMapper.selectById(u.getRoleId());
                isAdmin = r != null && "ADMIN".equals(r.getRoleCode());
            }
        }
        // 已发布(1)所有人可见；下架/草稿/待审核等仅作者或管理员可查看
        int st = c.getStatus() != null ? c.getStatus() : STATUS_PUBLISHED;
        if (st != STATUS_PUBLISHED && !isAuthor && !isAdmin) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        // 浏览量：仅用户端真实阅读；作者/管理员预览、发布人自己看不计；同访客窗口内重复打开只计 1 次
        boolean bumpView = recordView
                && st == STATUS_PUBLISHED
                && !isAdmin
                && !isAuthor
                && detailViewDedupHelper.shouldIncrement("knowledge", id, userId, clientIp);
        if (bumpView) {
            c.setViewCount((c.getViewCount() != null ? c.getViewCount() : 0) + 1);
            contentMapper.updateById(c);
        }
        if (userId != null) {
            recommendService.recordBehavior(userId, "VIEW", "CONTENT", id);
        }
        Map<String, Object> map = toDetailMap(c);
        try {
            Long cc = knowledgeContentCommentMapper.countVisibleByContentId(id);
            map.put("commentCount", cc != null ? cc : 0L);
        } catch (BadSqlGrammarException e) {
            map.put("commentCount", 0L);
        }
        if (userId != null) {
            /* 未上架/未通过审核：不展示已点赞/已收藏（作者预览时避免误导） */
            if (st != STATUS_PUBLISHED) {
                map.put("collected", false);
                map.put("liked", false);
            } else {
                map.put("collected", userCollectionService.isCollected(userId, CollectionTargetType.KNOWLEDGE, id));
                try {
                    long likeCnt = userContentLikeMapper.selectCount(new LambdaQueryWrapper<UserContentLike>()
                            .eq(UserContentLike::getUserId, userId).eq(UserContentLike::getContentId, id));
                    map.put("liked", likeCnt > 0);
                } catch (BadSqlGrammarException e) {
                    if (isMissingUserContentLikeTable(e)) {
                        map.put("liked", false);
                    } else {
                        throw e;
                    }
                }
            }
        } else {
            map.put("collected", false);
            map.put("liked", false);
        }
        return map;
    }

    @Override
    public Long save(Long authorId, Map<String, Object> body) {
        SysUser user = userMapper.selectById(authorId);
        if (user == null) throw new BusinessException(ResultCode.NOT_FOUND);
        SysRole role = roleMapper.selectById(user.getRoleId());
        String roleCode = role != null ? role.getRoleCode() : "";
        if (!"AUTHOR".equals(roleCode) && !"ADMIN".equals(roleCode)) {
            throw new BusinessException("仅作者或管理员可发布知识，请先申请成为作者");
        }
        int requestedStatus = body.get("status") != null ? Integer.valueOf(body.get("status").toString()) : STATUS_DRAFT;
        if (!"ADMIN".equals(roleCode) && requestedStatus == STATUS_PUBLISHED) {
            throw new BusinessException("作者不能直接新建为已发布，请保存草稿后提交审核");
        }
        KnowledgeContent c = new KnowledgeContent();
        c.setTitle((String) body.get("title"));
        c.setCategoryId(Long.valueOf(body.get("categoryId").toString()));
        c.setContent((String) body.get("content"));
        c.setCover((String) body.get("cover"));
        c.setSummary((String) body.get("summary"));
        c.setAuthorId(authorId);
        c.setStatus(requestedStatus);
        c.setRejectReason(null);
        contentMapper.insert(c);
        return c.getId();
    }

    @Override
    public void update(Long id, Long operatorId, Map<String, Object> body) {
        KnowledgeContent c = contentMapper.selectById(id);
        assertCanManageContent(operatorId, c);
        if (body.containsKey("title")) c.setTitle((String) body.get("title"));
        if (body.containsKey("categoryId")) c.setCategoryId(Long.valueOf(body.get("categoryId").toString()));
        if (body.containsKey("content")) c.setContent((String) body.get("content"));
        if (body.containsKey("cover")) c.setCover((String) body.get("cover"));
        if (body.containsKey("summary")) c.setSummary((String) body.get("summary"));
        if (body.containsKey("status")) {
            Integer st = Integer.valueOf(body.get("status").toString());
            SysUser op = userMapper.selectById(operatorId);
            SysRole opRole = op != null ? roleMapper.selectById(op.getRoleId()) : null;
            boolean isAdmin = opRole != null && "ADMIN".equals(opRole.getRoleCode());
            if (!isAdmin && st != null && st == STATUS_PUBLISHED) {
                throw new BusinessException("作者不能直接改为已发布，请使用「提交审核」或等待管理员审核");
            }
            c.setStatus(st);
            // 作者重新编辑或再次提交审核时，清空历史驳回原因，避免误导
            if (st != null && (st == STATUS_DRAFT || st == STATUS_PENDING)) {
                c.setRejectReason(null);
            }
        }
        if (body.containsKey("updateTime")) {
            String reqUpdateTime = String.valueOf(body.get("updateTime"));
            String dbUpdateTime = String.valueOf(c.getUpdateTime());
            if (StringUtils.hasText(reqUpdateTime) && !Objects.equals(reqUpdateTime, dbUpdateTime)) {
                throw new BusinessException("内容已被他人修改，请刷新后重试");
            }
        }
        contentMapper.updateById(c);
    }

    @Override
    public void delete(Long id, Long operatorId) {
        KnowledgeContent c = contentMapper.selectById(id);
        assertCanManageContent(operatorId, c);
        contentMapper.deleteById(id);
        auditLogService.log(operatorId, "CONTENT_DELETE", "CONTENT", id, "删除知识内容");
    }

    @Override
    public void restore(Long id, Long operatorId, String module) {
        SysUser operator = userMapper.selectById(operatorId);
        SysRole role = operator != null ? roleMapper.selectById(operator.getRoleId()) : null;
        if (role == null || !"ADMIN".equals(role.getRoleCode())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅管理员可恢复删除内容");
        }
        String m = module == null ? "KNOWLEDGE" : module.trim().toUpperCase(Locale.ROOT);
        int n;
        switch (m) {
            case "KNOWLEDGE" -> {
                n = contentMapper.restoreById(id);
                if (n > 0) {
                    auditLogService.log(operatorId, "CONTENT_RESTORE", "CONTENT", id, "回收站恢复知识内容");
                }
            }
            case "NEWS" -> {
                n = newsMapper.restoreByIdFromRecycle(id);
                if (n > 0) {
                    auditLogService.log(operatorId, "NEWS_RESTORE", "NEWS", id, "回收站恢复新闻");
                }
            }
            case "FORUM" -> {
                n = forumPostMapper.restoreByIdFromRecycle(id);
                if (n > 0) {
                    auditLogService.log(operatorId, "FORUM_POST_RESTORE", "FORUM_POST", id, "回收站恢复论坛帖子");
                }
            }
            default -> throw new BusinessException(400, "module 须为 KNOWLEDGE、NEWS 或 FORUM");
        }
        if (n <= 0) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
    }

    private void assertAdmin(Long operatorId) {
        SysUser operator = userMapper.selectById(operatorId);
        SysRole role = operator != null ? roleMapper.selectById(operator.getRoleId()) : null;
        if (role == null || !"ADMIN".equals(role.getRoleCode())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "仅管理员可操作");
        }
    }

    private void cleanupJunctionsForContent(Long contentId) {
        contentMapper.deleteContentTagsByContentId(contentId);
        userCollectionService.deleteAllForKnowledgeContent(contentId);
        try {
            userContentLikeMapper.delete(new LambdaQueryWrapper<UserContentLike>().eq(UserContentLike::getContentId, contentId));
        } catch (BadSqlGrammarException e) {
            if (!isMissingUserContentLikeTable(e)) throw e;
        }
    }

    @Override
    public void permanentDelete(Long id, Long operatorId, String module) {
        assertAdmin(operatorId);
        String m = module == null ? "KNOWLEDGE" : module.trim().toUpperCase(Locale.ROOT);
        switch (m) {
            case "KNOWLEDGE" -> {
                if (contentMapper.countInRecycle(id) <= 0) {
                    throw new BusinessException(ResultCode.NOT_FOUND);
                }
                cleanupJunctionsForContent(id);
                int n = contentMapper.physicalDeleteById(id);
                if (n <= 0) {
                    throw new BusinessException(ResultCode.NOT_FOUND);
                }
                auditLogService.log(operatorId, "CONTENT_PURGE", "CONTENT", id, "回收站彻底删除知识内容");
            }
            case "NEWS" -> {
                if (newsMapper.countInRecycle(id) <= 0) {
                    throw new BusinessException(ResultCode.NOT_FOUND);
                }
                newsLikeMapper.physicalDeleteByNewsId(id);
                newsCommentMapper.physicalDeleteByNewsId(id);
                int n = newsMapper.physicalDeleteFromRecycle(id);
                if (n <= 0) {
                    throw new BusinessException(ResultCode.NOT_FOUND);
                }
                auditLogService.log(operatorId, "NEWS_PURGE", "NEWS", id, "回收站彻底删除新闻");
            }
            case "FORUM" -> {
                if (forumPostMapper.countInRecycle(id) <= 0) {
                    throw new BusinessException(ResultCode.NOT_FOUND);
                }
                forumCommentMapper.physicalDeleteByPostId(id);
                int n = forumPostMapper.physicalDeleteFromRecycle(id);
                if (n <= 0) {
                    throw new BusinessException(ResultCode.NOT_FOUND);
                }
                auditLogService.log(operatorId, "FORUM_POST_PURGE", "FORUM_POST", id, "回收站彻底删除论坛帖子");
            }
            default -> throw new BusinessException(400, "module 须为 KNOWLEDGE、NEWS 或 FORUM");
        }
    }

    @Override
    public void purgeRecycle(Long operatorId) {
        assertAdmin(operatorId);
        int total = 0;
        List<Long> kIds = contentMapper.selectRecycleIds();
        for (Long id : kIds) {
            cleanupJunctionsForContent(id);
            contentMapper.physicalDeleteById(id);
            total++;
        }
        List<Long> nIds = newsMapper.selectRecycleIds();
        for (Long id : nIds) {
            newsLikeMapper.physicalDeleteByNewsId(id);
            newsCommentMapper.physicalDeleteByNewsId(id);
            newsMapper.physicalDeleteFromRecycle(id);
            total++;
        }
        List<Long> fIds = forumPostMapper.selectRecycleIds();
        for (Long id : fIds) {
            forumCommentMapper.physicalDeleteByPostId(id);
            forumPostMapper.physicalDeleteFromRecycle(id);
            total++;
        }
        auditLogService.log(operatorId, "RECYCLE_PURGE_ALL", "RECYCLE", null, "回收站清空，共 " + total + " 条（含知识/新闻/帖子）");
    }

    @Override
    public void batchDelete(List<Long> ids, Long operatorId) {
        assertAdmin(operatorId);
        if (ids == null || ids.isEmpty()) return;
        for (Long id : ids) {
            if (id == null) continue;
            delete(id, operatorId);
        }
    }

    @Override
    public void batchOffline(List<Long> ids, Long operatorId) {
        assertAdmin(operatorId);
        if (ids == null || ids.isEmpty()) return;
        for (Long id : ids) {
            if (id == null) continue;
            KnowledgeContent c = contentMapper.selectById(id);
            if (c == null) continue;
            if (c.getStatus() == null || c.getStatus() != STATUS_PUBLISHED) continue;
            changeStatus(id, operatorId, STATUS_OFFLINE);
        }
    }

    @Override
    public void batchPublish(List<Long> ids, Long operatorId) {
        assertAdmin(operatorId);
        if (ids == null || ids.isEmpty()) return;
        for (Long id : ids) {
            if (id == null) continue;
            KnowledgeContent c = contentMapper.selectById(id);
            if (c == null) continue;
            if (c.getStatus() == null || c.getStatus() != STATUS_OFFLINE) continue;
            changeStatus(id, operatorId, STATUS_PUBLISHED);
        }
    }

    @Override
    public void changeStatus(Long id, Long operatorId, Integer status) {
        if (status == null || (status != STATUS_PUBLISHED && status != STATUS_OFFLINE)) {
            throw new BusinessException("状态仅支持 1(发布) 或 2(下架)");
        }
        KnowledgeContent c = contentMapper.selectById(id);
        assertCanManageContent(operatorId, c);
        Integer oldStatus = c.getStatus();
        c.setStatus(status);
        contentMapper.updateById(c);
        auditLogService.log(operatorId, status == STATUS_OFFLINE ? "CONTENT_OFFLINE" : "CONTENT_ONLINE", "CONTENT", id,
                status == STATUS_OFFLINE ? "下架知识内容" : "恢复发布知识内容");
        // 管理员将已发布内容下架时，通知作者（非本人操作）
        if (status == STATUS_OFFLINE && oldStatus != null && oldStatus == STATUS_PUBLISHED
                && c.getAuthorId() != null && !c.getAuthorId().equals(operatorId)) {
            SysUser admin = userMapper.selectById(operatorId);
            String adminName = admin != null ? admin.getUsername() : "管理员";
            UserNotification n = new UserNotification();
            n.setUserId(c.getAuthorId());
            n.setTitle(truncateForVarchar("知识内容已下架", USER_NOTIFY_TITLE_MAX));
            n.setContent(truncateForVarchar(
                    adminName + " 已将您的知识内容《" + (c.getTitle() != null ? c.getTitle() : "") + "》下架，用户端将不再展示。您可在「我的知识」中查看与修改。",
                    USER_NOTIFY_CONTENT_MAX));
            n.setIsRead(0);
            insertUserNotificationSafe(n);
        }
    }

    @Override
    public void publish(Long id, Long operatorId) {
        KnowledgeContent c = contentMapper.selectById(id);
        if (c == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (!Objects.equals(c.getAuthorId(), operatorId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        if (c.getStatus() != null && c.getStatus() == STATUS_PUBLISHED) {
            return;
        }
        SysUser operator = userMapper.selectById(operatorId);
        SysRole role = operator != null ? roleMapper.selectById(operator.getRoleId()) : null;
        String roleCode = role != null ? role.getRoleCode() : "";
        // 管理员直接发布(1)，作者提交审核(3)，提交审核时清空历史驳回原因
        if ("ADMIN".equals(roleCode)) {
            c.setStatus(STATUS_PUBLISHED);
        } else {
            c.setStatus(STATUS_PENDING);
            c.setRejectReason(null);
        }
        contentMapper.updateById(c);
        if (!"ADMIN".equals(roleCode)) {
            String titleShort = c.getTitle() != null ? c.getTitle() : "";
            if (titleShort.length() > 80) {
                titleShort = titleShort.substring(0, 80) + "…";
            }
            SysUser author = userMapper.selectById(operatorId);
            String authorName = author != null && StringUtils.hasText(author.getUsername()) ? author.getUsername() : "作者";
            userNotificationService.notifyAllAdminsWithActionIgnoreFailure(
                    "新的知识内容待审核",
                    "作者 " + authorName + " 提交了知识《" + titleShort + "》，请在知识管理中审核。",
                    "/knowledge/list",
                    "去审核");
        }
    }

    @Override
    public void auditContent(Long id, Integer status, String rejectReason, Long operatorId) {
        KnowledgeContent c = contentMapper.selectById(id);
        if (c == null) throw new BusinessException(ResultCode.NOT_FOUND);
        if (c.getStatus() == null || c.getStatus() != STATUS_PENDING) {
            throw new BusinessException("仅能审核待审核状态的内容");
        }
        boolean approve = status != null && status == 1;
        int newStatus = approve ? STATUS_PUBLISHED : STATUS_DRAFT;
        String newRejectReason = approve ? null : rejectReason;
        // 使用 UpdateWrapper 显式更新 status 和 reject_reason，确保驳回原因写入数据库
        LambdaUpdateWrapper<KnowledgeContent> uw = new LambdaUpdateWrapper<KnowledgeContent>()
                .eq(KnowledgeContent::getId, id)
                .set(KnowledgeContent::getStatus, newStatus)
                .set(KnowledgeContent::getRejectReason, newRejectReason);
        contentMapper.update(null, uw);

        Long authorId = c.getAuthorId();
        if (authorId == null || authorId.equals(operatorId)) {
            return;
        }
        String title = c.getTitle() != null ? c.getTitle() : "";
        SysUser admin = operatorId != null ? userMapper.selectById(operatorId) : null;
        String adminName = admin != null ? admin.getUsername() : "管理员";
        if (approve) {
            UserNotification n = new UserNotification();
            n.setUserId(authorId);
            n.setTitle(truncateForVarchar("知识审核通过", USER_NOTIFY_TITLE_MAX));
            n.setContent(truncateForVarchar(
                    adminName + " 已审核通过你的知识《" + title + "》，用户端已可浏览。",
                    USER_NOTIFY_CONTENT_MAX));
            n.setActionUrl("/knowledge/" + id);
            n.setActionText("去查看");
            n.setIsRead(0);
            insertUserNotificationSafe(n);
        } else {
            String reasonPart = StringUtils.hasText(newRejectReason) ? "。原因：" + newRejectReason : "。请修改后重新提交审核。";
            UserNotification n = new UserNotification();
            n.setUserId(authorId);
            n.setTitle(truncateForVarchar("知识审核未通过", USER_NOTIFY_TITLE_MAX));
            n.setContent(truncateForVarchar(
                    adminName + " 驳回了你的知识《" + title + "》" + reasonPart,
                    USER_NOTIFY_CONTENT_MAX));
            n.setActionUrl("/knowledge/edit/" + id);
            n.setActionText("去修改");
            n.setIsRead(0);
            insertUserNotificationSafe(n);
        }
    }

    @Override
    public void collect(Long userId, Long contentId) {
        userCollectionService.collectKnowledge(userId, contentId);
    }

    @Override
    public void uncollect(Long userId, Long contentId) {
        userCollectionService.uncollectKnowledge(userId, contentId);
    }

    @Override
    public PageResult<Map<String, Object>> collectList(Long userId, Integer pageNum, Integer pageSize, String module) {
        return userCollectionService.myCollectPage(userId, module, pageNum, pageSize);
    }

    @Override
    public Map<String, Object> toggleLike(Long userId, Long contentId) {
        KnowledgeContent c = contentMapper.selectById(contentId);
        if (c == null || Integer.valueOf(1).equals(c.getDeleted())) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        int cst = c.getStatus() != null ? c.getStatus() : STATUS_DRAFT;
        if (cst != STATUS_PUBLISHED) {
            throw new BusinessException("内容未通过审核或未上架，暂不可点赞");
        }
        LambdaQueryWrapper<UserContentLike> q = new LambdaQueryWrapper<UserContentLike>()
                .eq(UserContentLike::getUserId, userId)
                .eq(UserContentLike::getContentId, contentId);
        UserContentLike old;
        try {
            old = userContentLikeMapper.selectOne(q);
        } catch (BadSqlGrammarException e) {
            if (isMissingUserContentLikeTable(e)) {
                throw new BusinessException("点赞功能尚未初始化，请先执行 DB/11_user_content_like.sql");
            }
            throw e;
        }
        boolean liked;
        int currentLike = c.getLikeCount() != null ? c.getLikeCount() : 0;
        if (old != null) {
            userContentLikeMapper.deleteById(old.getId());
            liked = false;
            currentLike = Math.max(0, currentLike - 1);
        } else {
            UserContentLike row = new UserContentLike();
            row.setUserId(userId);
            row.setContentId(contentId);
            userContentLikeMapper.insert(row);
            liked = true;
            currentLike += 1;
            // 给作者发送点赞通知（自己给自己点赞不通知）
            if (c.getAuthorId() != null && !c.getAuthorId().equals(userId)) {
                SysUser liker = userMapper.selectById(userId);
                String likerName = liker != null ? liker.getUsername() : "有用户";
                UserNotification n = new UserNotification();
                n.setUserId(c.getAuthorId());
                n.setTitle(truncateForVarchar("内容获赞通知", USER_NOTIFY_TITLE_MAX));
                n.setContent(truncateForVarchar(
                        likerName + " 点赞了你发布的内容《" + (c.getTitle() != null ? c.getTitle() : "") + "》",
                        USER_NOTIFY_CONTENT_MAX));
                n.setIsRead(0);
                insertUserNotificationSafe(n);
            }
        }
        c.setLikeCount(currentLike);
        contentMapper.updateById(c);
        if (liked) {
            recommendService.recordBehavior(userId, "LIKE", "CONTENT", contentId);
        }
        Map<String, Object> m = new HashMap<>();
        m.put("liked", liked);
        m.put("likeCount", currentLike);
        return m;
    }

    private boolean isMissingUserContentLikeTable(Throwable e) {
        String msg = e != null ? e.getMessage() : null;
        return msg != null && msg.toLowerCase().contains(USER_CONTENT_LIKE_TABLE);
    }

    private static final String USER_NOTIFICATION_TABLE = "user_notification";

    /** 通知摘要用：富文本评论去标签 */
    private static String plainPreviewRich(String html) {
        if (html == null) {
            return "";
        }
        return html.replaceAll("<[^>]+>", " ").replace("&nbsp;", " ").replaceAll("\\s+", " ").trim();
    }

    private static String truncateForVarchar(String s, int maxLen) {
        if (s == null) {
            return "";
        }
        if (s.length() <= maxLen) {
            return s;
        }
        if (maxLen <= 1) {
            return "…";
        }
        return s.substring(0, maxLen - 1) + "…";
    }

    private boolean isMissingUserNotificationTable(Throwable e) {
        String msg = e != null ? e.getMessage() : null;
        return msg != null && msg.toLowerCase().contains(USER_NOTIFICATION_TABLE);
    }

    /** 下架/获赞等主流程已成功时，通知写入失败不应导致整单 500 */
    private void insertUserNotificationSafe(UserNotification n) {
        try {
            userNotificationMapper.insert(n);
            if (n.getUserId() != null) {
                notificationRealtimePublisher.notifyNewNotification(n.getUserId());
            }
        } catch (BadSqlGrammarException e) {
            if (isMissingUserNotificationTable(e)) {
                log.warn("user_notification 表不可用，已跳过站内通知: {}", e.getMessage());
                return;
            }
            throw e;
        } catch (DataIntegrityViolationException e) {
            log.warn("站内通知写入失败，已忽略: {}", e.getMessage());
        }
    }

    @Override
    public Map<String, Object> shareUrl(Long contentId) {
        Map<String, Object> m = new HashMap<>();
        m.put("contentId", contentId);
        // 用户端为 hash 路由 #/knowledge/:id
        String base = clientBaseUrl.endsWith("/") ? clientBaseUrl.substring(0, clientBaseUrl.length() - 1) : clientBaseUrl;
        m.put("shareUrl", base + "/#/knowledge/" + contentId);
        return m;
    }

    @Override
    public List<Map<String, Object>> categoryList() {
        List<KnowledgeCategory> list = categoryMapper.selectList(
                new LambdaQueryWrapper<KnowledgeCategory>().eq(KnowledgeCategory::getStatus, 1).orderByAsc(KnowledgeCategory::getSort));
        return list.stream().map(c -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", c.getId());
            m.put("name", c.getName());
            m.put("sort", c.getSort());
            return m;
        }).collect(Collectors.toList());
    }

    @Override
    public Long adminSaveCategory(String name, Integer sort) {
        KnowledgeCategory c = new KnowledgeCategory();
        c.setName(name);
        c.setSort(sort != null ? sort : 0);
        c.setStatus(1);
        categoryMapper.insert(c);
        return c.getId();
    }

    @Override
    public void adminUpdateCategory(Long id, String name, Integer sort) {
        KnowledgeCategory c = categoryMapper.selectById(id);
        if (c != null) {
            if (name != null) c.setName(name);
            if (sort != null) c.setSort(sort);
            categoryMapper.updateById(c);
        }
    }

    @Override
    public void adminDeleteCategory(Long id) {
        categoryMapper.deleteById(id);
    }

    private Map<String, Object> toListMap(KnowledgeContent c) {
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
        m.put("status", c.getStatus());
        String reason = c.getRejectReason();
        m.put("rejectReason", reason);
        m.put("reject_reason", reason);
        return m;
    }

    private Map<String, Object> toDetailMap(KnowledgeContent c) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", c.getId());
        m.put("title", c.getTitle());
        m.put("categoryId", c.getCategoryId());
        KnowledgeCategory cat = categoryMapper.selectById(c.getCategoryId());
        m.put("categoryName", cat != null ? cat.getName() : null);
        m.put("content", c.getContent());
        m.put("cover", c.getCover());
        m.put("summary", c.getSummary());
        m.put("viewCount", c.getViewCount());
        m.put("likeCount", c.getLikeCount());
        m.put("authorId", c.getAuthorId());
        SysUser author = userMapper.selectById(c.getAuthorId());
        m.put("authorName", author != null ? author.getUsername() : null);
        m.put("createTime", c.getCreateTime());
        m.put("status", c.getStatus());
        String detailReason = c.getRejectReason();
        m.put("rejectReason", detailReason);
        m.put("reject_reason", detailReason);
        return m;
    }

    @Override
    public IPage<Map<String, Object>> listContentComments(Long contentId, Integer pageNum, Integer pageSize, Long viewerUserId, String sortBy) {
        KnowledgeContent c = contentMapper.selectById(contentId);
        if (c == null || Integer.valueOf(1).equals(c.getDeleted())) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        int st = c.getStatus() != null ? c.getStatus() : STATUS_PUBLISHED;
        /* 已发布：任何人可拉评论；下架：仅作者或管理员可查看历史评论（与详情页可见范围一致）；草稿/待审等不可 */
        if (st != STATUS_PUBLISHED) {
            if (st != STATUS_OFFLINE) {
                throw new BusinessException(ResultCode.NOT_FOUND);
            }
            if (viewerUserId == null) {
                throw new BusinessException(ResultCode.NOT_FOUND);
            }
            boolean viewerAdmin = false;
            SysUser vu = userMapper.selectById(viewerUserId);
            if (vu != null) {
                SysRole vr = roleMapper.selectById(vu.getRoleId());
                viewerAdmin = vr != null && "ADMIN".equals(vr.getRoleCode());
            }
            boolean author = c.getAuthorId() != null && c.getAuthorId().equals(viewerUserId);
            if (!viewerAdmin && !author) {
                throw new BusinessException(ResultCode.NOT_FOUND);
            }
        }
        int pn = pageNum != null ? pageNum : 1;
        int ps = pageSize != null ? pageSize : 10;
        Page<KnowledgeContentComment> page = new Page<>(pn, ps);
        LambdaQueryWrapper<KnowledgeContentComment> q = new LambdaQueryWrapper<KnowledgeContentComment>()
                .eq(KnowledgeContentComment::getContentId, contentId)
                .eq(KnowledgeContentComment::getStatus, 1);
        applyKnowledgeCommentSort(q, sortBy);
        try {
            IPage<KnowledgeContentComment> r = knowledgeContentCommentMapper.selectPage(page, q);
            Map<Long, KnowledgeContentComment> parentMap = loadKnowledgeCommentParents(r.getRecords());
            Map<Long, String> names = loadKnowledgeCommentUsernames(r.getRecords(), parentMap);
            return r.convert(row -> commentToMap(row, viewerUserId, parentMap, names));
        } catch (BadSqlGrammarException e) {
            throw new BusinessException("知识评论功能尚未初始化，请先执行 DB/18_knowledge_content_comment.sql");
        }
    }

    private static void applyKnowledgeCommentSort(LambdaQueryWrapper<KnowledgeContentComment> q, String sortBy) {
        if (sortBy != null && "hot".equalsIgnoreCase(sortBy.trim())) {
            q.orderByDesc(KnowledgeContentComment::getLikeCount)
                    .orderByDesc(KnowledgeContentComment::getCreateTime)
                    .orderByDesc(KnowledgeContentComment::getId);
        } else {
            q.orderByDesc(KnowledgeContentComment::getCreateTime)
                    .orderByDesc(KnowledgeContentComment::getId);
        }
    }

    private Map<String, Object> commentToMap(KnowledgeContentComment row, Long viewerUserId,
                                             Map<Long, KnowledgeContentComment> parentMap,
                                             Map<Long, String> names) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", row.getId());
        m.put("parentId", row.getParentId());
        m.put("userId", row.getUserId());
        SysUser u = userMapper.selectById(row.getUserId());
        m.put("username", u != null ? u.getUsername() : "用户");
        m.put("content", PlainTextSanitizer.formatCommentForApi(row.getContent()));
        m.put("createTime", row.getCreateTime());
        m.put("likeCount", commentLikeService.resolveLikeCount(
                CommentLikeService.REDIS_KNOWLEDGE_COMMENT, CommentLikeService.CHAN_KNOWLEDGE, row.getId(), row.getLikeCount()));
        if (viewerUserId != null) {
            m.put("liked", commentLikeService.isLiked(CommentLikeService.REDIS_KNOWLEDGE_COMMENT, CommentLikeService.CHAN_KNOWLEDGE, row.getId(), viewerUserId));
        } else {
            m.put("liked", false);
        }
        if (row.getParentId() != null && parentMap != null) {
            KnowledgeContentComment p = parentMap.get(row.getParentId());
            if (p != null) {
                String pu = names != null ? names.get(p.getUserId()) : null;
                if (pu == null && p.getUserId() != null) {
                    SysUser puUser = userMapper.selectById(p.getUserId());
                    pu = puUser != null ? puUser.getUsername() : null;
                }
                m.put("parentUserName", pu);
                m.put("parentContentPreview", previewKnowledgeCommentText(p.getContent()));
            }
        }
        return m;
    }

    @Override
    public Map<String, Object> postContentComment(Long userId, Long contentId, String text, Long parentId) {
        KnowledgeContent c = contentMapper.selectById(contentId);
        if (c == null || Integer.valueOf(1).equals(c.getDeleted())) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (c.getStatus() == null || c.getStatus() != STATUS_PUBLISHED) {
            throw new BusinessException("仅已发布内容可评论");
        }
        String body = PlainTextSanitizer.sanitizeRichCommentBody(text);
        if (PlainTextSanitizer.isBlankRichComment(body)) {
            throw new BusinessException("评论内容不能为空");
        }
        Long parentUserId = null;
        String parentContent = null;
        if (parentId != null) {
            KnowledgeContentComment p = knowledgeContentCommentMapper.selectById(parentId);
            if (p == null) {
                throw new BusinessException(ResultCode.NOT_FOUND, "父评论不存在");
            }
            if (!contentId.equals(p.getContentId())) {
                throw new BusinessException("父评论不属于该内容");
            }
            if (p.getStatus() != null && !Integer.valueOf(1).equals(p.getStatus())) {
                throw new BusinessException("父评论已隐藏，无法回复");
            }
            parentUserId = p.getUserId();
            parentContent = p.getContent();
        }
        KnowledgeContentComment cm = new KnowledgeContentComment();
        cm.setContentId(contentId);
        cm.setUserId(userId);
        cm.setParentId(parentId);
        cm.setContent(body);
        cm.setStatus(1);
        cm.setLikeCount(0);
        cm.setCreateTime(LocalDateTime.now());
        try {
            knowledgeContentCommentMapper.insert(cm);
        } catch (BadSqlGrammarException e) {
            throw new BusinessException("知识评论功能尚未初始化，请先执行 DB/18_knowledge_content_comment.sql");
        }
        SysUser commenter = userMapper.selectById(userId);
        String commenterName = commenter != null && StringUtils.hasText(commenter.getUsername()) ? commenter.getUsername() : "用户";
        String previewPlain = truncateForVarchar(plainPreviewRich(body), 220);
        if (parentId == null) {
            Long authorId = c.getAuthorId();
            if (authorId != null && !authorId.equals(userId)) {
                userNotificationService.sendWithActionIgnoreFailure(
                        authorId,
                        "知识有新评论",
                        commenterName + " 在您的知识《" + (c.getTitle() != null ? c.getTitle() : "") + "》下评论：「"
                                + truncateForVarchar(plainPreviewRich(body), 120) + "」",
                        "/knowledge/" + contentId + "?commentId=" + cm.getId(),
                        "查看评论");
            }
        } else if (parentUserId != null && !parentUserId.equals(userId)) {
            userNotificationService.sendWithActionIgnoreFailure(
                    parentUserId,
                    "评论有新回复",
                    "知识《" + (c.getTitle() != null ? c.getTitle() : "") + "》下，您的评论「"
                            + truncateForVarchar(plainPreviewRich(parentContent), 50) + "」收到新回复：「"
                            + truncateForVarchar(plainPreviewRich(body), 80) + "」。",
                    "/knowledge/" + contentId + "?commentId=" + cm.getId(),
                    "查看回复");
        }
        userNotificationService.notifyAdminsNewCommentReview(
                "知识",
                c.getTitle(),
                commenterName,
                previewPlain,
                "/knowledge/comments");
        recommendService.recordBehavior(userId, "COMMENT", "CONTENT", contentId);
        return Map.of("id", cm.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> toggleKnowledgeCommentLike(Long userId, Long commentId) {
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        KnowledgeContentComment c;
        try {
            c = knowledgeContentCommentMapper.selectById(commentId);
        } catch (BadSqlGrammarException e) {
            throw new BusinessException("知识评论功能尚未初始化，请先执行 DB/18_knowledge_content_comment.sql");
        }
        if (c == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (c.getStatus() == null || !Integer.valueOf(1).equals(c.getStatus())) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        KnowledgeContent k = contentMapper.selectById(c.getContentId());
        if (k == null || Integer.valueOf(1).equals(k.getDeleted())) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (k.getStatus() == null || k.getStatus() != STATUS_PUBLISHED) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        Map<String, Object> r = commentLikeService.toggle(
                CommentLikeService.REDIS_KNOWLEDGE_COMMENT,
                CommentLikeService.CHAN_KNOWLEDGE,
                commentId,
                userId,
                () -> {
                    KnowledgeContentComment fresh = knowledgeContentCommentMapper.selectById(commentId);
                    return fresh != null && fresh.getLikeCount() != null ? fresh.getLikeCount() : 0;
                },
                cnt -> knowledgeContentCommentMapper.update(null, new LambdaUpdateWrapper<KnowledgeContentComment>()
                        .eq(KnowledgeContentComment::getId, commentId)
                        .set(KnowledgeContentComment::getLikeCount, cnt)));
        if (Boolean.TRUE.equals(r.get("liked"))) {
            if (c.getUserId() != null && !Objects.equals(c.getUserId(), userId)) {
                UserNotification n = new UserNotification();
                n.setUserId(c.getUserId());
                n.setTitle(truncateForVarchar("评论被点赞", USER_NOTIFY_TITLE_MAX));
                n.setContent(truncateForVarchar(
                        "知识《" + (k.getTitle() != null ? k.getTitle() : "") + "》下，您的评论「"
                                + PlainTextSanitizer.plainPreviewForNotify(c.getContent(), 60) + "」收到了一个新点赞。",
                        USER_NOTIFY_CONTENT_MAX));
                n.setActionUrl("/knowledge/" + k.getId() + "?commentId=" + c.getId());
                n.setActionText("查看评论");
                n.setIsRead(0);
                insertUserNotificationSafe(n);
            }
            try {
                recommendService.recordBehavior(userId, "LIKE", "KNOWLEDGE_COMMENT", commentId);
            } catch (Exception e) {
                log.warn("推荐行为记录失败(已忽略): {}", e.getMessage());
            }
        }
        return r;
    }

    @Override
    public void deleteKnowledgeComment(Long userId, Long commentId) {
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        KnowledgeContentComment c = knowledgeContentCommentMapper.selectById(commentId);
        if (c == null || Integer.valueOf(1).equals(c.getDeleted())) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (!Objects.equals(c.getUserId(), userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        knowledgeContentCommentMapper.deleteById(commentId);
    }

    @Override
    public IPage<Map<String, Object>> adminKnowledgeCommentPage(Integer pageNum, Integer pageSize, Long contentId, String keyword, Integer status) {
        int pn = pageNum == null || pageNum < 1 ? 1 : Math.min(pageNum, ADMIN_K_COMMENT_MAX_PAGE_NUM);
        int ps = pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, ADMIN_K_COMMENT_MAX_PAGE_SIZE);
        Page<KnowledgeContentComment> page = new Page<>(pn, ps);
        LambdaQueryWrapper<KnowledgeContentComment> q = new LambdaQueryWrapper<KnowledgeContentComment>()
                .orderByDesc(KnowledgeContentComment::getCreateTime);
        if (status != null) {
            q.eq(KnowledgeContentComment::getStatus, status);
        }
        if (contentId != null) {
            q.eq(KnowledgeContentComment::getContentId, contentId);
        }
        if (StringUtils.hasText(keyword)) {
            String k = keyword.trim();
            if (k.length() > ADMIN_K_COMMENT_KEYWORD_MAX) {
                k = k.substring(0, ADMIN_K_COMMENT_KEYWORD_MAX);
            }
            q.like(KnowledgeContentComment::getContent, k);
        }
        try {
            IPage<KnowledgeContentComment> result = knowledgeContentCommentMapper.selectPage(page, q);
            Set<Long> cids = result.getRecords().stream()
                    .map(KnowledgeContentComment::getContentId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            Map<Long, String> contentTitles = new HashMap<>();
            if (!cids.isEmpty()) {
                for (KnowledgeContent kc : contentMapper.selectBatchIds(new ArrayList<>(cids))) {
                    if (kc != null && kc.getId() != null) {
                        contentTitles.put(kc.getId(), PlainTextSanitizer.sanitizeTitleOutput(kc.getTitle()));
                    }
                }
            }
            Map<Long, KnowledgeContentComment> parentMap = loadKnowledgeCommentParents(result.getRecords());
            Map<Long, String> names = loadKnowledgeCommentUsernames(result.getRecords(), parentMap);
            return result.convert(c -> {
                Map<String, Object> m = toAdminKnowledgeCommentRow(c, parentMap, names);
                m.put("contentId", c.getContentId());
                m.put("contentTitle", contentTitles.get(c.getContentId()));
                m.put("status", c.getStatus());
                return m;
            });
        } catch (BadSqlGrammarException e) {
            throw new BusinessException("知识评论功能尚未初始化，请先执行 DB/18_knowledge_content_comment.sql");
        }
    }

    @Override
    public void adminKnowledgeCommentDelete(Long commentId, Long operatorId) {
        try {
            KnowledgeContentComment c = knowledgeContentCommentMapper.selectById(commentId);
            if (c == null) {
                throw new BusinessException(ResultCode.NOT_FOUND);
            }
            knowledgeContentCommentMapper.deleteById(commentId);
            auditLogService.log(operatorId, "KNOWLEDGE_COMMENT_DELETE", "KNOWLEDGE_COMMENT", commentId,
                    "管理端删除知识评论 contentId=" + c.getContentId());
        } catch (BadSqlGrammarException e) {
            throw new BusinessException("知识评论功能尚未初始化，请先执行 DB/18_knowledge_content_comment.sql");
        }
    }

    @Override
    public void adminKnowledgeCommentSetStatus(Long commentId, Integer status, Long operatorId) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("status 只能为 0（隐藏）或 1（显示）");
        }
        try {
            KnowledgeContentComment c = knowledgeContentCommentMapper.selectById(commentId);
            if (c == null) {
                throw new BusinessException(ResultCode.NOT_FOUND);
            }
            c.setStatus(status);
            knowledgeContentCommentMapper.updateById(c);
            auditLogService.log(operatorId, "KNOWLEDGE_COMMENT_STATUS", "KNOWLEDGE_COMMENT", commentId,
                    "管理端设置知识评论状态=" + status);
        } catch (BadSqlGrammarException e) {
            throw new BusinessException("知识评论功能尚未初始化，请先执行 DB/18_knowledge_content_comment.sql");
        }
    }

    @Override
    public Map<String, Object> adminKnowledgeCommentAuditDetail(Long commentId) {
        if (commentId == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        try {
            KnowledgeContentComment c = knowledgeContentCommentMapper.selectById(commentId);
            if (c == null) {
                throw new BusinessException(ResultCode.NOT_FOUND);
            }
            KnowledgeContent kc = contentMapper.selectById(c.getContentId());
            if (kc == null) {
                throw new BusinessException(ResultCode.NOT_FOUND);
            }

            Map<String, Object> comment = new HashMap<>();
            comment.put("id", c.getId());
            comment.put("contentId", c.getContentId());
            comment.put("userId", c.getUserId());
            comment.put("parentId", c.getParentId());
            comment.put("content", PlainTextSanitizer.formatCommentForApi(c.getContent()));
            comment.put("status", c.getStatus());
            comment.put("createTime", c.getCreateTime());
            SysUser cu = userMapper.selectById(c.getUserId());
            comment.put("username", cu != null ? cu.getUsername() : null);

            Map<String, Object> parentComment = null;
            if (c.getParentId() != null) {
                KnowledgeContentComment p = knowledgeContentCommentMapper.selectById(c.getParentId());
                if (p != null) {
                    parentComment = new HashMap<>();
                    parentComment.put("id", p.getId());
                    parentComment.put("userId", p.getUserId());
                    parentComment.put("content", PlainTextSanitizer.formatCommentForApi(p.getContent()));
                    parentComment.put("status", p.getStatus());
                    parentComment.put("createTime", p.getCreateTime());
                    SysUser pu = userMapper.selectById(p.getUserId());
                    parentComment.put("username", pu != null ? pu.getUsername() : null);
                }
            }

            Map<String, Object> knowledgeMap = new HashMap<>();
            knowledgeMap.put("id", kc.getId());
            knowledgeMap.put("title", PlainTextSanitizer.sanitizeTitleOutput(kc.getTitle()));
            knowledgeMap.put("summary", kc.getSummary());
            knowledgeMap.put("content", kc.getContent());
            knowledgeMap.put("status", kc.getStatus());
            knowledgeMap.put("createTime", kc.getCreateTime());

            Map<String, Object> out = new HashMap<>();
            out.put("comment", comment);
            out.put("parentComment", parentComment);
            out.put("knowledge", knowledgeMap);
            return out;
        } catch (BadSqlGrammarException e) {
            throw new BusinessException("知识评论功能尚未初始化，请先执行 DB/18_knowledge_content_comment.sql");
        }
    }

    private Map<String, Object> toAdminKnowledgeCommentRow(KnowledgeContentComment c,
                                                           Map<Long, KnowledgeContentComment> parentMap,
                                                           Map<Long, String> names) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", c.getId());
        m.put("userId", c.getUserId());
        m.put("username", names.get(c.getUserId()));
        m.put("parentId", c.getParentId());
        m.put("content", PlainTextSanitizer.formatCommentForApi(c.getContent()));
        m.put("createTime", c.getCreateTime());
        m.put("likeCount", c.getLikeCount() != null ? c.getLikeCount() : 0);
        if (c.getParentId() != null && parentMap != null) {
            KnowledgeContentComment p = parentMap.get(c.getParentId());
            if (p != null) {
                String pu = names.get(p.getUserId());
                if (pu == null) {
                    SysUser puUser = userMapper.selectById(p.getUserId());
                    pu = puUser != null ? puUser.getUsername() : null;
                }
                m.put("parentUserName", pu);
                m.put("parentContentPreview", previewKnowledgeCommentText(p.getContent()));
            }
        }
        return m;
    }

    private static String previewKnowledgeCommentText(String s) {
        if (s == null) {
            return "";
        }
        String more = PlainTextSanitizer.plainPreviewForNotify(s, 2000);
        if (more.length() <= 280) {
            return more;
        }
        return more.substring(0, 280) + "…";
    }

    private Map<Long, KnowledgeContentComment> loadKnowledgeCommentParents(List<KnowledgeContentComment> records) {
        Set<Long> parentIds = records.stream()
                .map(KnowledgeContentComment::getParentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, KnowledgeContentComment> map = new HashMap<>();
        if (parentIds.isEmpty()) {
            return map;
        }
        for (KnowledgeContentComment row : records) {
            if (row.getId() != null && parentIds.contains(row.getId())) {
                map.put(row.getId(), row);
            }
        }
        List<Long> missing = parentIds.stream().filter(pid -> !map.containsKey(pid)).collect(Collectors.toList());
        if (!missing.isEmpty()) {
            for (KnowledgeContentComment pc : knowledgeContentCommentMapper.selectBatchIds(missing)) {
                if (pc != null && pc.getId() != null) {
                    map.put(pc.getId(), pc);
                }
            }
        }
        for (Long pid : parentIds) {
            if (!map.containsKey(pid)) {
                KnowledgeContentComment one = knowledgeContentCommentMapper.selectById(pid);
                if (one != null && one.getId() != null) {
                    map.put(one.getId(), one);
                }
            }
        }
        return map;
    }

    private Map<Long, String> loadKnowledgeCommentUsernames(List<KnowledgeContentComment> records,
                                                            Map<Long, KnowledgeContentComment> parentMap) {
        Set<Long> uids = new HashSet<>();
        for (KnowledgeContentComment c : records) {
            if (c.getUserId() != null) {
                uids.add(c.getUserId());
            }
        }
        for (KnowledgeContentComment p : parentMap.values()) {
            if (p.getUserId() != null) {
                uids.add(p.getUserId());
            }
        }
        Map<Long, String> names = new HashMap<>();
        if (uids.isEmpty()) {
            return names;
        }
        List<SysUser> users = userMapper.selectList(new LambdaQueryWrapper<SysUser>().in(SysUser::getId, uids));
        for (SysUser u : users) {
            if (u != null && StringUtils.hasText(u.getUsername())) {
                names.put(u.getId(), u.getUsername());
            }
        }
        return names;
    }
}
