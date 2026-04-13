-- ============================================================
-- 用户行为模块测试数据（用于推荐/兴趣分析）
-- 依赖：01_sys_user、02_knowledge、03_forum、seed_user_sample、seed_knowledge_sample、seed_forum_sample 已执行
-- 即：存在用户 1～5，知识内容 1～10，论坛帖子 1～5
-- 执行：mysql -u root -p fire_recommendation < seed_behavior_sample.sql
-- ============================================================

USE fire_recommendation;

-- behavior_type: VIEW / LIKE / COMMENT / COLLECT
-- target_type: CONTENT（知识）/ POST（帖子）
-- extra 可选：分类ID等便于统计

INSERT INTO user_behavior (user_id, behavior_type, target_type, target_id, extra, create_time) VALUES
-- 用户1：浏览、收藏知识，浏览帖子
(1, 'VIEW',   'CONTENT', 1, '1', NOW()),
(1, 'VIEW',   'CONTENT', 3, '3', NOW()),
(1, 'COLLECT', 'CONTENT', 1, NULL, NOW()),
(1, 'COLLECT', 'CONTENT', 5, NULL, NOW()),
(1, 'VIEW',   'POST', 1, NULL, NOW()),
(1, 'VIEW',   'POST', 4, NULL, NOW()),
-- 用户2：浏览、点赞知识，发帖
(2, 'VIEW',   'CONTENT', 2, '2', NOW()),
(2, 'VIEW',   'CONTENT', 4, '4', NOW()),
(2, 'LIKE',   'CONTENT', 3, NULL, NOW()),
(2, 'VIEW',   'POST', 2, NULL, NOW()),
(2, 'LIKE',   'POST', 4, NULL, NOW()),
-- 用户3：多类行为
(3, 'VIEW',   'CONTENT', 1, '1', NOW()),
(3, 'VIEW',   'CONTENT', 2, '2', NOW()),
(3, 'VIEW',   'CONTENT', 6, '1', NOW()),
(3, 'COLLECT', 'CONTENT', 1, NULL, NOW()),
(3, 'COLLECT', 'CONTENT', 2, NULL, NOW()),
(3, 'VIEW',   'POST', 3, NULL, NOW()),
(3, 'COMMENT', 'POST', 3, NULL, NOW()),
-- 用户4
(4, 'VIEW',   'CONTENT', 3, '3', NOW()),
(4, 'VIEW',   'CONTENT', 7, '2', NOW()),
(4, 'LIKE',   'CONTENT', 3, NULL, NOW()),
(4, 'VIEW',   'POST', 5, NULL, NOW()),
-- 用户5
(5, 'VIEW',   'CONTENT', 5, '5', NOW()),
(5, 'VIEW',   'CONTENT', 10, '5', NOW()),
(5, 'COLLECT', 'CONTENT', 5, NULL, NOW()),
(5, 'COLLECT', 'CONTENT', 10, NULL, NOW()),
(5, 'VIEW',   'POST', 4, NULL, NOW()),
(5, 'LIKE',   'POST', 4, NULL, NOW());
