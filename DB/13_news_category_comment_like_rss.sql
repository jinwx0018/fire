-- ============================================================
-- 新闻扩展：分类字典、评论、点赞、RSS 所需字段
-- 依赖：已有 news 表。MySQL 5.6 兼容（无 ngram 索引，见 14 脚本）
-- ============================================================

USE fire_recommendation;

-- 新闻分类字典（管理端维护；新闻关联 category_id）
CREATE TABLE IF NOT EXISTS news_category (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  name            VARCHAR(64)  NOT NULL COMMENT '分类名称',
  sort_order      INT          NOT NULL DEFAULT 0 COMMENT '排序，小在前',
  deleted         TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  create_time     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  update_time     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_nc_deleted (deleted),
  KEY idx_nc_sort (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='新闻分类字典';

ALTER TABLE news_category ADD UNIQUE KEY uk_nc_name (name);

INSERT IGNORE INTO news_category (name, sort_order, deleted) VALUES
  ('政策法规', 10, 0),
  ('演练培训', 20, 0),
  ('案例警示', 30, 0),
  ('装备动态', 40, 0);

-- 新闻评论
CREATE TABLE IF NOT EXISTS news_comment (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  news_id         BIGINT       NOT NULL,
  user_id         BIGINT       NOT NULL,
  parent_id       BIGINT       DEFAULT NULL COMMENT '回复的评论 id',
  content         TEXT         NOT NULL COMMENT '富文本评论，见 DB/23 说明',
  status          TINYINT      NOT NULL DEFAULT 1 COMMENT '1 显示 0 隐藏',
  deleted         TINYINT      NOT NULL DEFAULT 0,
  create_time     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  KEY idx_ncm_news (news_id),
  KEY idx_ncm_user (user_id),
  KEY idx_ncm_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='新闻评论';

-- 新闻点赞（用户-新闻唯一）
CREATE TABLE IF NOT EXISTS news_like (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  news_id         BIGINT       NOT NULL,
  user_id         BIGINT       NOT NULL,
  create_time     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_news_like (news_id, user_id),
  KEY idx_nl_news (news_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='新闻点赞';

-- news 表若尚无 category_id、like_count（旧库升级），请执行 DB/13b_news_alter_legacy_columns.sql
-- 新版 06_news.sql 已含上述列时可跳过
