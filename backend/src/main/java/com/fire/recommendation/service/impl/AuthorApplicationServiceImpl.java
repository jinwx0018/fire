package com.fire.recommendation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fire.recommendation.entity.AuthorApplication;
import com.fire.recommendation.entity.SysRole;
import com.fire.recommendation.entity.SysUser;
import com.fire.recommendation.entity.UserNotification;
import com.fire.recommendation.exception.BusinessException;
import com.fire.recommendation.common.ResultCode;
import com.fire.recommendation.mapper.AuthorApplicationMapper;
import com.fire.recommendation.mapper.SysRoleMapper;
import com.fire.recommendation.mapper.SysUserMapper;
import com.fire.recommendation.mapper.UserNotificationMapper;
import com.fire.recommendation.component.NotificationRealtimePublisher;
import com.fire.recommendation.service.AuditLogService;
import com.fire.recommendation.service.AuthorApplicationService;
import com.fire.recommendation.service.UserNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthorApplicationServiceImpl implements AuthorApplicationService {

    private static final String PENDING = "PENDING";
    private static final String APPROVED = "APPROVED";
    private static final String REJECTED = "REJECTED";

    private final AuthorApplicationMapper applicationMapper;
    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final UserNotificationMapper userNotificationMapper;
    private final AuditLogService auditLogService;
    private final UserNotificationService userNotificationService;
    private final NotificationRealtimePublisher notificationRealtimePublisher;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String mailFrom;

    @Override
    public void apply(Long userId, String applyReason, String attachments) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(ResultCode.NOT_FOUND);
        SysRole authorRole = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, "AUTHOR"));
        if (authorRole != null && authorRole.getId().equals(user.getRoleId())) {
            throw new BusinessException("您已是作者，无需重复申请");
        }
        AuthorApplication latest = applicationMapper.selectOne(
                new LambdaQueryWrapper<AuthorApplication>()
                        .eq(AuthorApplication::getUserId, userId)
                        .orderByDesc(AuthorApplication::getCreateTime)
                        .last("LIMIT 1"));
        if (latest != null && PENDING.equals(latest.getStatus())) {
            throw new BusinessException("您已提交过申请，请等待审核");
        }
        AuthorApplication app = new AuthorApplication();
        app.setUserId(userId);
        app.setApplyReason(StringUtils.hasText(applyReason) ? applyReason.trim() : null);
        app.setAttachments(StringUtils.hasText(attachments) ? attachments.trim() : null);
        app.setStatus(PENDING);
        applicationMapper.insert(app);
        auditLogService.log(userId, "AUTHOR_APPLY", "AUTHOR_APPLICATION", app.getId(), "提交作者申请");
        String applicantName = user != null && StringUtils.hasText(user.getUsername()) ? user.getUsername() : "用户";
        String reason = StringUtils.hasText(applyReason) ? applyReason.trim() : "";
        if (reason.length() > 200) {
            reason = reason.substring(0, 200) + "…";
        }
        String body = "用户 " + applicantName + " 提交了作者申请，请及时审核。";
        if (StringUtils.hasText(reason)) {
            body += " 申请说明：" + reason;
        }
        userNotificationService.notifyAllAdminsWithActionIgnoreFailure(
                "新的作者申请待审核", body, "/user/author-applications", "去审核");
    }

    @Override
    public Map<String, Object> getMyApplicationsOverview(Long userId) {
        SysUser user = userMapper.selectById(userId);
        boolean authorRoleActive = false;
        if (user != null) {
            SysRole role = roleMapper.selectById(user.getRoleId());
            authorRoleActive = role != null && ("AUTHOR".equals(role.getRoleCode()) || "ADMIN".equals(role.getRoleCode()));
        }
        List<AuthorApplication> apps = applicationMapper.selectList(
                new LambdaQueryWrapper<AuthorApplication>()
                        .eq(AuthorApplication::getUserId, userId)
                        .orderByDesc(AuthorApplication::getCreateTime));
        List<Map<String, Object>> records = new ArrayList<>();
        for (AuthorApplication app : apps) {
            records.add(toUserHistoryRow(app));
        }
        Map<String, Object> m = new HashMap<>();
        m.put("authorRoleActive", authorRoleActive);
        m.put("records", records);
        return m;
    }

    private Map<String, Object> toUserHistoryRow(AuthorApplication app) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", app.getId());
        row.put("status", app.getStatus());
        row.put("applyReason", app.getApplyReason());
        row.put("attachments", app.getAttachments());
        row.put("reviewRemark", app.getReviewRemark());
        row.put("rejectReason", app.getRejectReason());
        row.put("createTime", app.getCreateTime());
        row.put("reviewTime", app.getReviewTime());
        return row;
    }

    @Override
    public Map<String, Object> getMyApplication(Long userId) {
        Map<String, Object> ov = getMyApplicationsOverview(userId);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> records = (List<Map<String, Object>>) ov.get("records");
        Map<String, Object> m = new HashMap<>();
        m.put("authorRoleActive", ov.get("authorRoleActive"));
        if (records == null || records.isEmpty()) {
            m.put("status", null);
            m.put("applied", false);
            return m;
        }
        m.put("applied", true);
        Map<String, Object> latest = records.get(0);
        m.put("status", latest.get("status"));
        m.put("applyReason", latest.get("applyReason"));
        m.put("attachments", latest.get("attachments"));
        m.put("reviewRemark", latest.get("reviewRemark"));
        m.put("rejectReason", latest.get("rejectReason"));
        m.put("createTime", latest.get("createTime"));
        m.put("reviewTime", latest.get("reviewTime"));
        return m;
    }

    @Override
    public List<Map<String, Object>> listPending() {
        List<AuthorApplication> list = applicationMapper.selectList(
                new LambdaQueryWrapper<AuthorApplication>()
                        .eq(AuthorApplication::getStatus, PENDING)
                        .orderByDesc(AuthorApplication::getCreateTime));
        Map<Long, Map<String, Object>> byUser = new LinkedHashMap<>();
        for (AuthorApplication app : list) {
            byUser.putIfAbsent(app.getUserId(), toPendingAdminRow(app));
        }
        return new ArrayList<>(byUser.values());
    }

    private Map<String, Object> toPendingAdminRow(AuthorApplication app) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", app.getId());
        m.put("userId", app.getUserId());
        m.put("status", app.getStatus());
        m.put("applyReason", app.getApplyReason());
        m.put("attachments", app.getAttachments());
        m.put("createTime", app.getCreateTime());
        SysUser u = userMapper.selectById(app.getUserId());
        m.put("username", u != null ? u.getUsername() : null);
        m.put("phone", u != null ? u.getPhone() : null);
        m.put("email", u != null ? u.getEmail() : null);
        return m;
    }

    @Override
    public IPage<Map<String, Object>> pageProcessedApplications(Integer pageNum, Integer pageSize) {
        int pn = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int ps = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 50);
        Page<AuthorApplication> page = new Page<>(pn, ps);
        LambdaQueryWrapper<AuthorApplication> q = new LambdaQueryWrapper<AuthorApplication>()
                .in(AuthorApplication::getStatus, APPROVED, REJECTED)
                .orderByDesc(AuthorApplication::getReviewTime)
                .orderByDesc(AuthorApplication::getCreateTime);
        IPage<AuthorApplication> raw = applicationMapper.selectPage(page, q);
        return raw.convert(this::toProcessedAdminRow);
    }

    private Map<String, Object> toProcessedAdminRow(AuthorApplication app) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", app.getId());
        m.put("userId", app.getUserId());
        m.put("status", app.getStatus());
        m.put("applyReason", app.getApplyReason());
        m.put("attachments", app.getAttachments());
        m.put("reviewRemark", app.getReviewRemark());
        m.put("rejectReason", app.getRejectReason());
        m.put("createTime", app.getCreateTime());
        m.put("reviewTime", app.getReviewTime());
        m.put("reviewBy", app.getReviewBy());
        SysUser u = userMapper.selectById(app.getUserId());
        m.put("username", u != null ? u.getUsername() : null);
        m.put("phone", u != null ? u.getPhone() : null);
        m.put("email", u != null ? u.getEmail() : null);
        if (app.getReviewBy() != null) {
            SysUser rv = userMapper.selectById(app.getReviewBy());
            m.put("reviewerUsername", rv != null ? rv.getUsername() : null);
        } else {
            m.put("reviewerUsername", null);
        }
        return m;
    }

    @Override
    public void approve(Long applicationId, Long adminUserId, String reviewRemark) {
        AuthorApplication app = applicationMapper.selectById(applicationId);
        if (app == null) throw new BusinessException(ResultCode.NOT_FOUND);
        if (!PENDING.equals(app.getStatus())) throw new BusinessException("该申请已处理");
        SysRole authorRole = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, "AUTHOR"));
        if (authorRole == null) throw new BusinessException("系统未配置作者角色");
        SysUser user = userMapper.selectById(app.getUserId());
        if (user == null) throw new BusinessException(ResultCode.NOT_FOUND);
        user.setRoleId(authorRole.getId());
        userMapper.updateById(user);
        app.setStatus(APPROVED);
        app.setReviewRemark(StringUtils.hasText(reviewRemark) ? reviewRemark.trim() : null);
        app.setReviewBy(adminUserId);
        app.setReviewTime(LocalDateTime.now());
        applicationMapper.updateById(app);
        notifyUser(app.getUserId(), "作者申请已通过", "恭喜，您的作者申请已通过审核，现在可以发布待审核的消防知识内容。");
        sendReviewEmail(user, true, null);
        auditLogService.log(adminUserId, "AUTHOR_APPLICATION_APPROVE", "AUTHOR_APPLICATION", applicationId, "通过作者申请");
    }

    @Override
    public void reject(Long applicationId, Long adminUserId, String rejectReason) {
        AuthorApplication app = applicationMapper.selectById(applicationId);
        if (app == null) throw new BusinessException(ResultCode.NOT_FOUND);
        if (!PENDING.equals(app.getStatus())) throw new BusinessException("该申请已处理");
        app.setStatus(REJECTED);
        app.setReviewBy(adminUserId);
        app.setReviewTime(LocalDateTime.now());
        if (StringUtils.hasText(rejectReason)) app.setRejectReason(rejectReason);
        applicationMapper.updateById(app);
        SysUser user = userMapper.selectById(app.getUserId());
        notifyUser(app.getUserId(), "作者申请未通过", "您的作者申请未通过。"
                + (StringUtils.hasText(rejectReason) ? "原因：" + rejectReason : ""));
        sendReviewEmail(user, false, rejectReason);
        auditLogService.log(adminUserId, "AUTHOR_APPLICATION_REJECT", "AUTHOR_APPLICATION", applicationId, "驳回作者申请");
    }

    private void notifyUser(Long userId, String title, String content) {
        UserNotification n = new UserNotification();
        n.setUserId(userId);
        n.setTitle(title);
        n.setContent(content);
        n.setIsRead(0);
        userNotificationMapper.insert(n);
        notificationRealtimePublisher.notifyNewNotification(userId);
    }

    private void sendReviewEmail(SysUser user, boolean approved, String rejectReason) {
        if (user == null || !StringUtils.hasText(user.getEmail()) || mailSender == null) {
            return;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            if (StringUtils.hasText(mailFrom)) {
                msg.setFrom(mailFrom);
            }
            msg.setTo(user.getEmail());
            msg.setSubject("消防科普推荐系统 - 作者申请审核结果");
            msg.setText(approved
                    ? "您的作者申请已通过审核。"
                    : "您的作者申请未通过审核。" + (StringUtils.hasText(rejectReason) ? "\n原因：" + rejectReason : ""));
            mailSender.send(msg);
        } catch (Exception ignore) {
        }
    }
}
