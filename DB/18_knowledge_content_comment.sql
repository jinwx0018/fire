-- 知识内容评论（用户端）；执行一次即可
USE fire_recommendation;

CREATE TABLE IF NOT EXISTS knowledge_content_comment (
  id           BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  content_id   BIGINT NOT NULL COMMENT '知识内容 ID',
  user_id      BIGINT NOT NULL,
  parent_id    BIGINT DEFAULT NULL COMMENT '预留：回复父评论',
  content      TEXT         NOT NULL COMMENT '评论正文（富文本：表情与受控 img）',
  like_count   INT NOT NULL DEFAULT 0 COMMENT '点赞数',
  status       TINYINT NOT NULL DEFAULT 1 COMMENT '1 显示 0 隐藏',
  deleted      TINYINT NOT NULL DEFAULT 0,
  create_time  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  KEY idx_content_id (content_id),
  KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识评论';
