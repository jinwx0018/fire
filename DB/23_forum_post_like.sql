-- 论坛帖子点赞：无 Redis 或 Redis 不可用时使用本表；有 Redis 时仍以 Redis 为准（与 forum_post.like_count 同步）
CREATE TABLE IF NOT EXISTS forum_post_like (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    post_id     BIGINT       NOT NULL COMMENT '帖子 ID',
    user_id     BIGINT       NOT NULL COMMENT '点赞用户',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_forum_post_like (post_id, user_id),
    KEY idx_forum_post_like_post (post_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='论坛帖子点赞（Redis 降级）';
