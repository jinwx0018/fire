package com.fire.recommendation.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fire.recommendation.entity.AuditLog;

public interface AuditLogService {
    void log(Long operatorId, String action, String targetType, Long targetId, String detail);

    /** 管理端：审计日志分页 */
    IPage<AuditLog> pageForAdmin(Integer pageNum, Integer pageSize, String action, Long operatorId);
}
