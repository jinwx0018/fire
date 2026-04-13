package com.fire.recommendation.service;

import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.Map;

public interface UserNotificationService {

    IPage<Map<String, Object>> pageForUser(Long userId, Integer pageNum, Integer pageSize, Integer isRead);

    /** 单条详情（仅本人），不存在则抛 NOT_FOUND */
    Map<String, Object> getDetail(Long userId, Long notificationId);

    void markRead(Long userId, Long notificationId);

    void markAllRead(Long userId);

    long unreadCount(Long userId);

    /** 站内通知：写入一条未读消息（content 过长会截断） */
    void sendSimple(Long userId, String title, String content);

    /** 同 {@link #sendSimple}，写入失败时仅记录日志，不向外抛异常（避免连带回滚主业务事务） */
    void sendSimpleIgnoreFailure(Long userId, String title, String content);

    /** 站内通知（含点击跳转） */
    void sendWithAction(Long userId, String title, String content, String actionUrl, String actionText);

    /** 同 {@link #sendWithAction}，写入失败时仅记录日志，不向外抛异常 */
    void sendWithActionIgnoreFailure(Long userId, String title, String content, String actionUrl, String actionText);

    /**
     * 向每位启用中的管理员各发一条站内通知（单条失败不影响其余）。
     * 用于作者申请、知识/论坛待审核等需管理端处理的事项。
     */
    void notifyAllAdminsWithActionIgnoreFailure(String title, String content, String actionUrl, String actionText);

    /**
     * 每条新评论向管理员发一条摘要：评论默认对用户端可见，提醒管理员审阅；违规时可在管理端隐藏或删除。
     */
    void notifyAdminsNewCommentReview(String sceneLabel, String targetTitle, String commenterName, String contentPreview, String adminManagePath);
}
