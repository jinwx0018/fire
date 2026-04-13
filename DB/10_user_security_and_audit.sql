-- ============================================================
-- 用户会话治理、作者申请增强、审计日志、站内通知、账号软删除（兼容 MySQL 5.6）
-- 依赖：01_sys_user.sql、07_author_application.sql
-- ============================================================

USE fire_recommendation;

-- 1) sys_user 增强字段（老库升级）
ALTER TABLE sys_user
  ADD COLUMN token_version INT NOT NULL DEFAULT 0 COMMENT 'JWT版本号，改密/强制下线后+1' AFTER status;
ALTER TABLE sys_user
  ADD COLUMN deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除' AFTER token_version;
ALTER TABLE sys_user
  ADD COLUMN deleted_time DATETIME DEFAULT NULL COMMENT '注销时间' AFTER deleted;

-- 2) refresh token 会话表
CREATE TABLE IF NOT EXISTS user_session (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id       BIGINT NOT NULL COMMENT '用户ID',
  refresh_token VARCHAR(512) NOT NULL COMMENT 'refresh token',
  token_version INT NOT NULL DEFAULT 0 COMMENT '签发时用户token版本',
  revoked       TINYINT NOT NULL DEFAULT 0 COMMENT '0 有效 1 已撤销',
  expire_time   DATETIME NOT NULL COMMENT '过期时间',
  create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_user_id (user_id),
  KEY idx_expire_time (expire_time),
  KEY idx_revoked (revoked),
  UNIQUE KEY uk_refresh_token (refresh_token(191))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户会话表（refresh token）';

-- 3) 作者申请增强字段（老库升级）
ALTER TABLE author_application
  ADD COLUMN apply_reason VARCHAR(1024) DEFAULT NULL COMMENT '申请原因' AFTER user_id;
ALTER TABLE author_application
  ADD COLUMN attachments VARCHAR(2048) DEFAULT NULL COMMENT '附件URL列表，逗号分隔' AFTER apply_reason;
ALTER TABLE author_application
  ADD COLUMN review_remark VARCHAR(512) DEFAULT NULL COMMENT '审核备注' AFTER status;

-- 4) 站内通知
CREATE TABLE IF NOT EXISTS user_notification (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id     BIGINT NOT NULL COMMENT '用户ID',
  title       VARCHAR(128) NOT NULL COMMENT '标题',
  content     VARCHAR(1024) NOT NULL COMMENT '通知内容',
  is_read     TINYINT NOT NULL DEFAULT 0 COMMENT '0 未读 1 已读',
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  KEY idx_user_id (user_id),
  KEY idx_is_read (is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户通知表';

-- 5) 审计日志
CREATE TABLE IF NOT EXISTS audit_log (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  operator_id BIGINT DEFAULT NULL COMMENT '操作人用户ID',
  action      VARCHAR(64) NOT NULL COMMENT '行为编码',
  target_type VARCHAR(64) DEFAULT NULL COMMENT '目标类型',
  target_id   BIGINT DEFAULT NULL COMMENT '目标ID',
  detail      VARCHAR(1024) DEFAULT NULL COMMENT '行为详情',
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  KEY idx_operator_id (operator_id),
  KEY idx_action (action),
  KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审计日志表';
