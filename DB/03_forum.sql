-- ============================================================
-- 模块三：消防知识论坛 - 帖子、评论（兼容 MySQL 5.6，点赞存 Redis）
-- 依赖：01_sys_user.sql
-- ============================================================

USE fire_recommendation;

-- 帖子表（InnoDB 全文索引需 MySQL 5.6.4+）
CREATE TABLE IF NOT EXISTS forum_post (
  id             BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '帖子ID',
  user_id        BIGINT       NOT NULL COMMENT '发帖用户ID',
  title          VARCHAR(256) NOT NULL COMMENT '标题',
  content        TEXT         NOT NULL COMMENT '内容',
  status         TINYINT      NOT NULL DEFAULT 0 COMMENT '1 正常 0 待审核 -1 违规',
  reject_reason  VARCHAR(512) DEFAULT NULL COMMENT '驳回理由（status=-1 时）',
  view_count     INT          NOT NULL DEFAULT 0 COMMENT '浏览量',
  like_count     INT          NOT NULL DEFAULT 0 COMMENT '点赞数（可与 Redis 同步）',
  comment_count  INT          NOT NULL DEFAULT 0 COMMENT '评论数',
  deleted        TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  create_time    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  update_time    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_user_id (user_id),
  KEY idx_status (status),
  KEY idx_deleted (deleted),
  KEY idx_create_time (create_time),
  FULLTEXT KEY ft_title_content (title, content)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='论坛帖子表';

-- 评论表
CREATE TABLE IF NOT EXISTS forum_comment (
  id          BIGINT   PRIMARY KEY AUTO_INCREMENT,
  post_id     BIGINT   NOT NULL COMMENT '帖子ID',
  user_id     BIGINT   NOT NULL COMMENT '评论者ID',
  parent_id   BIGINT   DEFAULT NULL COMMENT '父评论ID，为空则为一级评论',
  content     TEXT     NOT NULL COMMENT '评论内容',
  deleted     TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_post_id (post_id),
  KEY idx_user_id (user_id),
  KEY idx_parent_id (parent_id),
  KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖子评论表';
