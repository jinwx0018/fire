-- ============================================================
-- 作者申请表（用户申请成为作者，管理员审核通过后赋予 AUTHOR 角色）
-- 依赖：01_sys_user.sql
-- ============================================================

USE fire_recommendation;

CREATE TABLE IF NOT EXISTS author_application (
  id          BIGINT    PRIMARY KEY AUTO_INCREMENT,
  user_id     BIGINT    NOT NULL COMMENT '申请用户ID',
  apply_reason VARCHAR(1024) DEFAULT NULL COMMENT '申请原因',
  attachments VARCHAR(2048) DEFAULT NULL COMMENT '附件URL列表，逗号分隔',
  status      VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED',
  review_remark VARCHAR(512) DEFAULT NULL COMMENT '审核备注',
  reject_reason VARCHAR(512) DEFAULT NULL COMMENT '驳回原因',
  review_by   BIGINT    DEFAULT NULL COMMENT '审核人用户ID',
  review_time DATETIME  DEFAULT NULL,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_user_id (user_id),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作者申请表';
