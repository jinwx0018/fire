-- ============================================================
-- 模块六：消防新闻 - 新闻表（兼容 MySQL 5.6）
-- 依赖：01_sys_user.sql（发布人可选关联用户）
-- ============================================================

USE fire_recommendation;

-- 新闻表（InnoDB 全文索引需 MySQL 5.6.4+）
CREATE TABLE IF NOT EXISTS news (
  id              BIGINT   PRIMARY KEY AUTO_INCREMENT,
  title           VARCHAR(256) NOT NULL COMMENT '标题',
  content         LONGTEXT NOT NULL COMMENT '内容',
  region          VARCHAR(64)  DEFAULT NULL COMMENT '地区',
  urgency_level   TINYINT  NOT NULL DEFAULT 1 COMMENT '紧急等级：1 低 2 中 3 高',
  publisher_id    BIGINT   DEFAULT NULL COMMENT '发布人用户ID',
  summary         VARCHAR(512) DEFAULT NULL COMMENT '摘要',
  view_count      INT      NOT NULL DEFAULT 0,
  status          TINYINT  NOT NULL DEFAULT 1 COMMENT '0 下架 1 正常',
  deleted         TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  publish_time    DATETIME DEFAULT NULL COMMENT '发布时间',
  create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_region (region),
  KEY idx_urgency_level (urgency_level),
  KEY idx_publisher_id (publisher_id),
  KEY idx_status (status),
  KEY idx_deleted (deleted),
  KEY idx_publish_time (publish_time),
  KEY idx_create_time (create_time),
  FULLTEXT KEY ft_title (title)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消防新闻表';
