package com.fire.recommendation.service;

import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.Map;

public interface NewsCommentService {

    /**
     * viewerUserId 非空时返回每条评论 liked。
     *
     * @param sortBy time=按发布时间倒序（默认）；hot=按点赞数倒序，同分再按时间
     */
    IPage<Map<String, Object>> pageForNews(Long newsId, Integer pageNum, Integer pageSize, Long viewerUserId, String sortBy);

    /** 评论点赞切换；需登录 */
    Map<String, Object> toggleCommentLike(Long commentId, Long userId);

    /** 管理端：全站评论分页，可选按新闻 ID、正文关键词、显示状态筛选（status 空=全部） */
    IPage<Map<String, Object>> adminPage(Integer pageNum, Integer pageSize, Long newsId, String contentKeyword, Integer status);

    void post(Long newsId, Long userId, String content, Long parentId);

    /** 用户端：删除自己的评论 */
    void deleteOwn(Long commentId, Long userId);

    void adminDelete(Long commentId, Long operatorId);

    /** 管理端：显示/隐藏（status：1 显示，0 隐藏） */
    void adminSetStatus(Long commentId, Integer status, Long operatorId);

    /**
     * 管理端：单条评论审核上下文（完整新闻正文、父评论全文、本条评论）。
     */
    Map<String, Object> adminAuditDetail(Long commentId);
}
