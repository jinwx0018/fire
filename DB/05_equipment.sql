-- ============================================================
-- 模块五：消防器材查询 - 器材类型、器材表（兼容 MySQL 5.6）
-- 依赖：00_init_database.sql
-- ============================================================

USE fire_recommendation;

-- 器材类型表
CREATE TABLE IF NOT EXISTS equipment_type (
  id          BIGINT   PRIMARY KEY AUTO_INCREMENT,
  name        VARCHAR(64) NOT NULL COMMENT '类型名称',
  sort        INT      NOT NULL DEFAULT 0,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_sort (sort)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='器材类型表';

-- 器材表（InnoDB 全文索引需 MySQL 5.6.4+）
CREATE TABLE IF NOT EXISTS equipment (
  id              BIGINT   PRIMARY KEY AUTO_INCREMENT,
  name            VARCHAR(128) NOT NULL COMMENT '器材名称',
  type_id         BIGINT   NOT NULL COMMENT '类型ID',
  cover           VARCHAR(512) DEFAULT NULL COMMENT '封面图',
  usage_steps     TEXT     DEFAULT NULL COMMENT '使用步骤（富文本或结构化）',
  check_points    TEXT     DEFAULT NULL COMMENT '检查要点',
  fault_solution  TEXT     DEFAULT NULL COMMENT '常见故障与解决',
  images          VARCHAR(1024) DEFAULT NULL COMMENT '多图，JSON 或逗号分隔',
  summary         VARCHAR(512) DEFAULT NULL COMMENT '简介',
  sort            INT      NOT NULL DEFAULT 0,
  status          TINYINT  NOT NULL DEFAULT 1 COMMENT '0 下架 1 正常',
  deleted         TINYINT  NOT NULL DEFAULT 0,
  create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_type_id (type_id),
  KEY idx_status (status),
  KEY idx_deleted (deleted),
  KEY idx_sort (sort),
  FULLTEXT KEY ft_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消防器材表';
