package com.fire.recommendation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fire.recommendation.entity.SysRole;
import com.fire.recommendation.entity.SysUser;
import com.fire.recommendation.entity.UserNotification;
import com.fire.recommendation.exception.BusinessException;
import com.fire.recommendation.common.ResultCode;
import com.fire.recommendation.mapper.SysRoleMapper;
import com.fire.recommendation.mapper.SysUserMapper;
import com.fire.recommendation.component.NotificationRealtimePublisher;
import com.fire.recommendation.mapper.UserNotificationMapper;
import com.fire.recommendation.service.UserNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserNotificationServiceImpl implements UserNotificationService {

    private final UserNotificationMapper userNotificationMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserMapper userMapper;
    private final NotificationRealtimePublisher notificationRealtimePublisher;

    @Override
    public IPage<Map<String, Object>> pageForUser(Long userId, Integer pageNum, Integer pageSize, Integer isRead) {
        Page<UserNotification> page = new Page<>(pageNum != null ? pageNum : 1, pageSize != null ? pageSize : 10);
        LambdaQueryWrapper<UserNotification> q = new LambdaQueryWrapper<UserNotification>()
                .eq(UserNotification::getUserId, userId)
                .orderByDesc(UserNotification::getCreateTime);
        if (isRead != null) {
            q.eq(UserNotification::getIsRead, isRead);
        }
        IPage<UserNotification> result = userNotificationMapper.selectPage(page, q);
        return result.convert(n -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", n.getId());
            m.put("title", n.getTitle());
            m.put("content", n.getContent());
            m.put("actionUrl", n.getActionUrl());
            m.put("actionText", n.getActionText());
            m.put("isRead", n.getIsRead());
            m.put("createTime", n.getCreateTime());
            return m;
        });
    }

    @Override
    public Map<String, Object> getDetail(Long userId, Long notificationId) {
        UserNotification n = userNotificationMapper.selectById(notificationId);
        if (n == null || !userId.equals(n.getUserId())) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        Map<String, Object> m = new HashMap<>();
        m.put("id", n.getId());
        m.put("title", n.getTitle());
        m.put("content", n.getContent());
        m.put("actionUrl", n.getActionUrl());
        m.put("actionText", n.getActionText());
        m.put("isRead", n.getIsRead());
        m.put("createTime", n.getCreateTime());
        return m;
    }

    @Override
    public void markRead(Long userId, Long notificationId) {
        UserNotification n = userNotificationMapper.selectById(notificationId);
        if (n == null || !userId.equals(n.getUserId())) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        n.setIsRead(1);
        userNotificationMapper.updateById(n);
    }

    @Override
    public void markAllRead(Long userId) {
        userNotificationMapper.update(
                null,
                new LambdaUpdateWrapper<UserNotification>()
                        .eq(UserNotification::getUserId, userId)
                        .eq(UserNotification::getIsRead, 0)
                        .set(UserNotification::getIsRead, 1));
    }

    @Override
    public long unreadCount(Long userId) {
        return userNotificationMapper.selectCount(
                new LambdaQueryWrapper<UserNotification>()
                        .eq(UserNotification::getUserId, userId)
                        .eq(UserNotification::getIsRead, 0));
    }

    private static final int NOTIFY_TITLE_MAX = 128;
    private static final int NOTIFY_CONTENT_MAX = 1024;
    private static final int NOTIFY_ACTION_URL_MAX = 512;
    private static final int NOTIFY_ACTION_TEXT_MAX = 64;

    @Override
    public void sendSimple(Long userId, String title, String content) {
        sendWithAction(userId, title, content, null, null);
    }

    @Override
    public void sendWithAction(Long userId, String title, String content, String actionUrl, String actionText) {
        if (userId == null) {
            return;
        }
        UserNotification n = new UserNotification();
        n.setUserId(userId);
        String t = title != null ? title.trim() : "";
        if (t.length() > NOTIFY_TITLE_MAX) {
            t = t.substring(0, NOTIFY_TITLE_MAX);
        }
        n.setTitle(t.isEmpty() ? "通知" : t);
        String c = content != null ? content.trim() : "";
        if (c.length() > NOTIFY_CONTENT_MAX) {
            c = c.substring(0, NOTIFY_CONTENT_MAX);
        }
        n.setContent(c);
        String u = actionUrl != null ? actionUrl.trim() : "";
        if (u.length() > NOTIFY_ACTION_URL_MAX) {
            u = u.substring(0, NOTIFY_ACTION_URL_MAX);
        }
        n.setActionUrl(u.isEmpty() ? null : u);
        String at = actionText != null ? actionText.trim() : "";
        if (at.length() > NOTIFY_ACTION_TEXT_MAX) {
            at = at.substring(0, NOTIFY_ACTION_TEXT_MAX);
        }
        n.setActionText(at.isEmpty() ? null : at);
        n.setIsRead(0);
        userNotificationMapper.insert(n);
        notificationRealtimePublisher.notifyNewNotification(userId);
    }

    @Override
    public void sendSimpleIgnoreFailure(Long userId, String title, String content) {
        try {
            sendSimple(userId, title, content);
        } catch (Exception e) {
            log.warn("站内通知写入失败，已忽略: userId={} title={} err={}", userId, title, e.getMessage());
        }
    }

    @Override
    public void sendWithActionIgnoreFailure(Long userId, String title, String content, String actionUrl, String actionText) {
        try {
            sendWithAction(userId, title, content, actionUrl, actionText);
        } catch (Exception e) {
            log.warn("站内通知写入失败，已忽略: userId={} title={} err={}", userId, title, e.getMessage());
        }
    }

    @Override
    public void notifyAllAdminsWithActionIgnoreFailure(String title, String content, String actionUrl, String actionText) {
        SysRole adminRole = roleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, "ADMIN"));
        if (adminRole == null || adminRole.getId() == null) {
            return;
        }
        List<SysUser> admins = userMapper.selectList(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getRoleId, adminRole.getId())
                        .eq(SysUser::getStatus, 1)
                        .and(w -> w.isNull(SysUser::getDeleted).or().eq(SysUser::getDeleted, 0)));
        if (admins == null || admins.isEmpty()) {
            return;
        }
        for (SysUser u : admins) {
            if (u != null && u.getId() != null) {
                sendWithActionIgnoreFailure(u.getId(), title, content, actionUrl, actionText);
            }
        }
    }

    @Override
    public void notifyAdminsNewCommentReview(String sceneLabel, String targetTitle, String commenterName,
                                            String contentPreview, String adminManagePath) {
        String scene = sceneLabel != null ? sceneLabel.trim() : "站内";
        String tt = targetTitle != null ? targetTitle.trim() : "";
        if (tt.length() > 60) {
            tt = tt.substring(0, 60) + "…";
        }
        String cn = commenterName != null && !commenterName.isBlank() ? commenterName.trim() : "用户";
        String prev = contentPreview != null ? contentPreview.trim() : "";
        if (prev.length() > 280) {
            prev = prev.substring(0, 280) + "…";
        }
        String body = "[" + scene + "]《" + tt + "》 " + cn + "：「" + prev
                + "」。该评论已对用户端正常展示；请您及时审阅，若存在违规内容，请在管理端将评论隐藏或删除（屏蔽/删除后用户端将不再可见）。";
        notifyAllAdminsWithActionIgnoreFailure("新评论待审核提醒", body, adminManagePath, "去审核");
    }
}
