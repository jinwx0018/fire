package com.fire.recommendation.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fire.recommendation.common.PageResult;

import java.util.List;
import java.util.Map;

public interface ContentService {

    /**
     * 用户端公开列表：仅已发布（忽略草稿/待审核/下架）。
     * {@code sortBy=hot} 时按热度；否则按发布时间。
     * {@code timeOrder} 仅在非 hot 时生效：{@code asc} 从旧到新，{@code desc} 从新到旧（默认）。
     */
    IPage<Map<String, Object>> list(Integer pageNum, Integer pageSize, Long categoryId, String title, String sortBy, String timeOrder);

    /** 管理端：分页列表，status 为空时不按状态过滤（可查草稿/待审核/已发布等） */
    IPage<Map<String, Object>> adminContentPage(Integer pageNum, Integer pageSize, Long categoryId, String title, Integer status);
    PageResult<Map<String, Object>> adminRecyclePage(Integer pageNum, Integer pageSize);

    /** 当前作者可管理内容列表（返回作者全部状态内容） */
    IPage<Map<String, Object>> myDrafts(Long authorId, Integer pageNum, Integer pageSize, Long categoryId, String title);

    /** 管理端：审核知识内容。status 1=通过(改为已发布)，0=驳回(改为草稿)；仅对当前 status=3 的内容有效 */
    void auditContent(Long id, Integer status, String rejectReason, Long operatorId);

    /**
     * @param recordView false 时不增加浏览量（前端刷新元数据）
     * @param clientIp   匿名访客去重，{@link com.fire.recommendation.util.HttpClientIp#resolve}
     */
    Map<String, Object> getDetail(Long id, Long userId, boolean recordView, String clientIp);

    Long save(Long authorId, Map<String, Object> body);

    void update(Long id, Long operatorId, Map<String, Object> body);

    void delete(Long id, Long operatorId);

    /** 回收站恢复；{@code module}：KNOWLEDGE（知识）/ NEWS（新闻）/ FORUM（帖子） */
    void restore(Long id, Long operatorId, String module);

    default void restore(Long id, Long operatorId) {
        restore(id, operatorId, "KNOWLEDGE");
    }

    void changeStatus(Long id, Long operatorId, Integer status);

    /** 作者提交审核(status→3)，管理员直接发布(status→1) */
    void publish(Long id, Long operatorId);

    void collect(Long userId, Long contentId);

    void uncollect(Long userId, Long contentId);

    /** module：knowledge | forum | news，默认知识 */
    PageResult<Map<String, Object>> collectList(Long userId, Integer pageNum, Integer pageSize, String module);

    /** 点赞/取消点赞（切换） */
    Map<String, Object> toggleLike(Long userId, Long contentId);

    /**
     * 知识评论分页（仅展示 status=1）；viewerUserId 非空时返回 liked。
     * sortBy：time=按发布时间倒序（默认）；hot=按点赞数倒序，同分再按时间
     */
    IPage<Map<String, Object>> listContentComments(Long contentId, Integer pageNum, Integer pageSize, Long viewerUserId, String sortBy);

    /** 发表评论；写入 user_behavior COMMENT+CONTENT 供推荐加权 */
    Map<String, Object> postContentComment(Long userId, Long contentId, String text, Long parentId);

    /** 知识评论点赞切换；需登录 */
    Map<String, Object> toggleKnowledgeCommentLike(Long userId, Long commentId);

    /** 知识评论删除；仅本人可删除自己的评论 */
    void deleteKnowledgeComment(Long userId, Long commentId);

    /** 管理端：全站知识评论分页；可选 contentId、正文关键词、status（1 显示 0 隐藏，空=全部） */
    IPage<Map<String, Object>> adminKnowledgeCommentPage(Integer pageNum, Integer pageSize, Long contentId, String keyword, Integer status);

    void adminKnowledgeCommentDelete(Long commentId, Long operatorId);

    void adminKnowledgeCommentSetStatus(Long commentId, Integer status, Long operatorId);

    /** 管理端：单条知识评论审核上下文（完整知识正文、父评论、本条） */
    Map<String, Object> adminKnowledgeCommentAuditDetail(Long commentId);

    Map<String, Object> shareUrl(Long contentId);

    List<Map<String, Object>> categoryList();

    Long adminSaveCategory(String name, Integer sort);

    void adminUpdateCategory(Long id, String name, Integer sort);

    void adminDeleteCategory(Long id);

    /** 回收站：物理删除单条（仅管理员，且须为逻辑删除状态） */
    void permanentDelete(Long id, Long operatorId, String module);

    default void permanentDelete(Long id, Long operatorId) {
        permanentDelete(id, operatorId, "KNOWLEDGE");
    }

    /** 回收站：清空全部逻辑删除记录（物理删除，仅管理员；含知识、新闻、论坛帖子） */
    void purgeRecycle(Long operatorId);

    /** 管理端：批量逻辑删除（仅管理员） */
    void batchDelete(List<Long> ids, Long operatorId);

    /** 管理端：批量下架（仅对已发布内容生效，仅管理员） */
    void batchOffline(List<Long> ids, Long operatorId);

    /** 管理端：批量恢复发布（仅对已下架内容生效，仅管理员） */
    void batchPublish(List<Long> ids, Long operatorId);
}
