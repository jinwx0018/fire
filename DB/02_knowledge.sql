-- ============================================================
-- 模块二：消防知识内容管理 - 分类、内容、标签、用户收藏（兼容 MySQL 5.6）
-- 依赖：01_sys_user.sql
-- ============================================================

USE fire_recommendation;

-- 知识分类表
CREATE TABLE IF NOT EXISTS knowledge_category (
  id          BIGINT   PRIMARY KEY AUTO_INCREMENT COMMENT '分类ID',
  name        VARCHAR(64) NOT NULL COMMENT '分类名称',
  sort        INT      NOT NULL DEFAULT 0 COMMENT '排序，数值越小越靠前',
  status      TINYINT  NOT NULL DEFAULT 1 COMMENT '0 禁用 1 正常',
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_sort (sort),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识分类表';

-- 知识内容表（InnoDB 全文索引需 MySQL 5.6.4+）
CREATE TABLE IF NOT EXISTS knowledge_content (
  id          BIGINT   PRIMARY KEY AUTO_INCREMENT COMMENT '内容ID',
  title       VARCHAR(256) NOT NULL COMMENT '标题',
  category_id BIGINT   NOT NULL COMMENT '分类ID',
  content     LONGTEXT NOT NULL COMMENT '正文',
  cover       VARCHAR(512) DEFAULT NULL COMMENT '封面图URL',
  summary     VARCHAR(512) DEFAULT NULL COMMENT '摘要',
  author_id   BIGINT   NOT NULL COMMENT '作者用户ID',
  view_count  INT      NOT NULL DEFAULT 0 COMMENT '浏览量',
  like_count  INT      NOT NULL DEFAULT 0 COMMENT '点赞数（若单独有点赞表可做冗余）',
  status      TINYINT  NOT NULL DEFAULT 1 COMMENT '0 草稿 1 已发布 2 下架',
  deleted     TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 否 1 是',
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_category_id (category_id),
  KEY idx_author_id (author_id),
  KEY idx_status (status),
  KEY idx_deleted (deleted),
  KEY idx_view_count (view_count),
  KEY idx_create_time (create_time),
  FULLTEXT KEY ft_title (title)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识内容表';

-- 标签表
CREATE TABLE IF NOT EXISTS knowledge_tag (
  id          BIGINT   PRIMARY KEY AUTO_INCREMENT,
  name        VARCHAR(64) NOT NULL COMMENT '标签名',
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识标签表';

-- 内容与标签多对多
CREATE TABLE IF NOT EXISTS knowledge_content_tag (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  content_id  BIGINT NOT NULL,
  tag_id      BIGINT NOT NULL,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_content_tag (content_id, tag_id),
  KEY idx_content_id (content_id),
  KEY idx_tag_id (tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='内容-标签关联表';

-- 用户收藏表
CREATE TABLE IF NOT EXISTS user_collection (
  id          BIGINT   PRIMARY KEY AUTO_INCREMENT,
  user_id     BIGINT   NOT NULL COMMENT '用户ID',
  content_id  BIGINT   NOT NULL COMMENT '知识内容ID',
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_content (user_id, content_id),
  KEY idx_user_id (user_id),
  KEY idx_content_id (content_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户收藏表';
