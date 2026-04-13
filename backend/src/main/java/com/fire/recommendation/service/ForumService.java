package com.fire.recommendation.service;

import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.Map;

public interface ForumService {

    /** 用户端公开列表：仅已通过审核的帖子 */
    IPage<Map<String, Object>> postList(Integer pageNum, Integer pageSize, String keyword);

    /** 当前用户自己发布的帖子（含待审核/驳回/通过），需登录 */
    IPage<Map<String, Object>> myPostList(Long userId, Integer pageNum, Integer pageSize, String keyword);

    /** 管理端审核列表：可按 status 筛选 */
    IPage<Map<String, Object>> adminPostList(Integer pageNum, Integer pageSize, Integer status, String keyword);

    /**
     * @param recordView true 且非作者/非管理员、并通过去重窗口时增加浏览量；false 用于前端仅刷新元数据
     * @param clientIp     匿名访客去重，{@link com.fire.recommendation.util.HttpClientIp#resolve}
     */
    Map<String, Object> postDetail(Long postId, Long userId, boolean recordView, String clientIp);

    Long publishPost(Long userId, String title, String content);

    void updatePost(Long postId, Long userId, String title, String content);

    void deletePost(Long postId, Long userId);

    Map<String, Object> likePost(Long postId, Long userId);

    /** sortBy：time=按发布时间倒序（默认）；hot=按点赞数倒序，同分再按时间 */
    IPage<Map<String, Object>> commentList(Long postId, Integer pageNum, Integer pageSize, Long viewerUserId, String sortBy);

    Long addComment(Long userId, Long postId, String content, Long parentId);

    /** 评论点赞切换（Redis Set 或 user_comment_like）；需登录 */
    Map<String, Object> likeComment(Long commentId, Long userId);

    void deleteComment(Long commentId, Long userId);

    void auditPost(Long postId, Integer status, String rejectReason);

    /** 管理端：软删帖子 */
    void adminDeletePost(Long postId);

    /** 管理端：软删评论并扣减帖子 comment_count */
    void adminDeleteComment(Long commentId);

    /** 管理端：全站论坛评论分页，可选帖子 ID、正文关键词、是否显示 */
    IPage<Map<String, Object>> adminCommentPage(Integer pageNum, Integer pageSize, Long postId, String keyword, Integer status);

    /** 管理端：显示/隐藏评论（status：1 显示，0 隐藏） */
    void adminUpdateCommentStatus(Long commentId, Integer status);

    /** 管理端：帖子详情（含正文，不增加浏览量；任意审核状态） */
    Map<String, Object> adminPostDetail(Long postId);

    /**
     * 管理端：单条论坛评论审核上下文（完整帖子正文、父评论全文、本条评论）。
     */
    Map<String, Object> adminCommentAuditDetail(Long commentId);
}
