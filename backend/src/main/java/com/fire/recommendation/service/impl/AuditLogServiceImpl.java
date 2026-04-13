package com.fire.recommendation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fire.recommendation.entity.AuditLog;
import com.fire.recommendation.mapper.AuditLogMapper;
import com.fire.recommendation.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogMapper auditLogMapper;

    @Override
    public void log(Long operatorId, String action, String targetType, Long targetId, String detail) {
        if (!StringUtils.hasText(action)) {
            return;
        }
        AuditLog log = new AuditLog();
        log.setOperatorId(operatorId);
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetail(detail);
        auditLogMapper.insert(log);
    }

    @Override
    public IPage<AuditLog> pageForAdmin(Integer pageNum, Integer pageSize, String action, Long operatorId) {
        Page<AuditLog> page = new Page<>(pageNum != null ? pageNum : 1, pageSize != null ? pageSize : 10);
        LambdaQueryWrapper<AuditLog> q = new LambdaQueryWrapper<AuditLog>().orderByDesc(AuditLog::getCreateTime);
        if (StringUtils.hasText(action)) {
            q.eq(AuditLog::getAction, action);
        }
        if (operatorId != null) {
            q.eq(AuditLog::getOperatorId, operatorId);
        }
        return auditLogMapper.selectPage(page, q);
    }
}
