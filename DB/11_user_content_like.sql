-- 模块二补充：知识点赞记录（用于去重点赞、点赞通知）
CREATE TABLE IF NOT EXISTS user_content_like (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  user_id     BIGINT       NOT NULL COMMENT '点赞用户ID',
  content_id  BIGINT       NOT NULL COMMENT '知识ID',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_content_like (user_id, content_id),
  KEY idx_content_id (content_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-知识点赞记录';
