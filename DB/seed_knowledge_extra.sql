-- ============================================================
-- 知识模块补充测试数据：用户收藏
-- 依赖：01_sys_user.sql、02_knowledge.sql、seed_user_sample.sql、seed_knowledge_sample.sql 已执行
-- 即：存在用户 1～5，存在知识分类 1～5，存在知识内容约 10 条（id 1～10）
-- 执行：mysql -u root -p fire_recommendation < seed_knowledge_extra.sql
-- ============================================================

USE fire_recommendation;

-- 用户收藏（user_id 1～5 收藏部分 content_id）
INSERT IGNORE INTO user_collection (user_id, content_id, create_time) VALUES
(1, 1, NOW()), (1, 3, NOW()), (1, 5, NOW()),
(2, 2, NOW()), (2, 4, NOW()),
(3, 1, NOW()), (3, 2, NOW()), (3, 6, NOW()),
(4, 3, NOW()), (4, 7, NOW()),
(5, 5, NOW()), (5, 10, NOW());
