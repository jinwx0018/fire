-- ============================================================
-- 论坛模块测试数据：帖子、评论
-- 依赖：01_sys_user.sql、03_forum.sql、seed_user_sample.sql 已执行（存在用户 1～5）
-- 执行：mysql -u root -p fire_recommendation < seed_forum_sample.sql
-- ============================================================

USE fire_recommendation;

-- 论坛帖子（status：1 正常 0 待审核 -1 违规）
INSERT INTO forum_post (id, user_id, title, content, status, reject_reason, view_count, like_count, comment_count, deleted, create_time, update_time) VALUES
(1, 1, '小区楼道堆放杂物引发火灾，大家注意', '我们小区上周有户在楼道堆纸箱，烟头引燃了，幸好发现及时。提醒大家楼道不要堆杂物，安全通道保持畅通。', 1, NULL, 128, 15, 6, 0, NOW(), NOW()),
(2, 2, '干粉灭火器使用心得分享', '家里备了灭火器，上个月单位培训实操了一次，总结：站在上风、对准根部、左右扫射。建议每年检查压力表。', 1, NULL, 89, 12, 4, 0, NOW(), NOW()),
(3, 3, '高层火灾逃生有没有好的办法？', '住 20 楼，一直担心火灾时怎么跑。除了低姿捂鼻、走楼梯，还有没有其他要注意的？求经验。', 1, NULL, 256, 8, 12, 0, NOW(), NOW()),
(4, 2, '厨房油锅起火千万别用水', '亲眼见过邻居用水泼油锅，火苗窜到天花板。正确做法是关火、盖锅盖或灭火毯。大家转发提醒家人。', 1, NULL, 312, 45, 9, 0, NOW(), NOW()),
(5, 4, '消防演练参加后的几点体会', '公司组织了一次疏散演练，发现很多人不知道最近的安全出口在哪。建议进商场、办公楼先看疏散图。', 1, NULL, 67, 6, 2, 0, NOW(), NOW());
-- 若表已有自增冲突可去掉 id 让自增生成

-- 帖子评论（post_id, user_id, parent_id 为空为一级评论）
INSERT INTO forum_comment (post_id, user_id, parent_id, content, deleted, create_time, update_time) VALUES
(1, 2, NULL, '我们楼道也有堆鞋柜的，物业贴了通知但没人清。', 0, NOW(), NOW()),
(1, 3, NULL, '物业应该定期清理，出了事谁都担不起。', 0, NOW(), NOW()),
(1, 4, 1, '我们这物业会直接清走，提前公告。', 0, NOW(), NOW()),
(2, 1, NULL, '单位每年组织一次演练很有必要。', 0, NOW(), NOW()),
(2, 3, NULL, '压力表在绿区就没事吗？', 0, NOW(), NOW()),
(2, 2, 5, '绿区正常，红区要送修或换新的。', 0, NOW(), NOW()),
(3, 1, NULL, '家里备逃生面罩、手电，门边摸墙找出口。', 0, NOW(), NOW()),
(3, 2, NULL, '防烟楼梯间比普通楼梯安全，先搞清楚本楼布局。', 0, NOW(), NOW()),
(4, 1, NULL, '灭火毯家里备了两条，厨房一条门口一条。', 0, NOW(), NOW()),
(4, 5, NULL, '感谢分享，已转发家庭群。', 0, NOW(), NOW()),
(5, 3, NULL, '疏散图很多地方在电梯口，平时真没注意过。', 0, NOW(), NOW());
-- 更新帖子评论数（与上面插入条数对应）
UPDATE forum_post SET comment_count = 3 WHERE id = 1;
UPDATE forum_post SET comment_count = 3 WHERE id = 2;
UPDATE forum_post SET comment_count = 2 WHERE id = 3;
UPDATE forum_post SET comment_count = 2 WHERE id = 4;
UPDATE forum_post SET comment_count = 1 WHERE id = 5;
