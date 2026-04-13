package com.fire.recommendation.service;

import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;
import java.util.Map;

public interface AuthorApplicationService {

    /** 用户提交作者申请 */
    void apply(Long userId, String applyReason, String attachments);

    /** 当前用户最新的申请记录（用于展示状态；与 {@link #getMyApplicationsOverview} 中首条一致） */
    Map<String, Object> getMyApplication(Long userId);

    /** 当前用户全部申请历史，按创建时间倒序（最新在前）；含 authorRoleActive */
    Map<String, Object> getMyApplicationsOverview(Long userId);

    /** 管理端：待审核列表；同一用户仅保留最新一条待审核记录 */
    List<Map<String, Object>> listPending();

    /** 管理端：已通过/已驳回记录分页（按审核时间倒序，便于回看刚处理的结果） */
    IPage<Map<String, Object>> pageProcessedApplications(Integer pageNum, Integer pageSize);

    /** 管理端：通过申请（将用户角色改为 AUTHOR） */
    void approve(Long applicationId, Long adminUserId, String reviewRemark);

    /** 管理端：驳回申请 */
    void reject(Long applicationId, Long adminUserId, String rejectReason);
}
