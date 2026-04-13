-- 评论点赞：冗余 like_count + 无 Redis 时使用的 user_comment_like 表
-- 有 Redis 时点赞用户集 key：forum:comment:like:{id}、news:comment:like:{id}、knowledge:comment:like:{id}
USE fire_recommendation;

-- 若列已存在会报错，可忽略后一条
ALTER TABLE forum_comment
  ADD COLUMN like_count INT NOT NULL DEFAULT 0 COMMENT '点赞数' AFTER content;

ALTER TABLE news_comment
  ADD COLUMN like_count INT NOT NULL DEFAULT 0 COMMENT '点赞数' AFTER content;

ALTER TABLE knowledge_content_comment
  ADD COLUMN like_count INT NOT NULL DEFAULT 0 COMMENT '点赞数' AFTER content;

CREATE TABLE IF NOT EXISTS user_comment_like (
  id           BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  channel      VARCHAR(32) NOT NULL COMMENT 'FORUM_COMMENT/NEWS_COMMENT/KNOWLEDGE_COMMENT',
  comment_id   BIGINT NOT NULL,
  user_id      BIGINT NOT NULL,
  create_time  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_channel_cid_uid (channel, comment_id, user_id),
  KEY idx_channel_comment (channel, comment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论点赞（Redis 不可用时的持久化）';
