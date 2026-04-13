-- 可选：推荐曝光/点击埋点表（需前端或网关上报接口配合，当前工程未强制使用）
-- 用于后续计算 CTR、位置偏差等离线指标

USE fire_recommendation;

CREATE TABLE IF NOT EXISTS recommend_feedback (
  id              BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  user_id         BIGINT       DEFAULT NULL COMMENT '可为空（未登录）',
  content_id      BIGINT       NOT NULL COMMENT '知识内容 ID',
  rank_pos        INT          DEFAULT NULL COMMENT '列表中的名次（从 1 起）',
  action_type     VARCHAR(16)  NOT NULL DEFAULT 'CLICK' COMMENT 'EXPOSE=曝光 CLICK=点击',
  create_time     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
  KEY idx_user_time (user_id, create_time),
  KEY idx_content (content_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='推荐反馈（可选）';
