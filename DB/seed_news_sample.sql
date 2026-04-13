-- ============================================================
-- 新闻模块测试数据
-- 依赖：01_sys_user.sql、06_news.sql、seed_user_sample.sql；
--       **须先执行 13_news_category_comment_like_rss.sql**（写入 news_category 字典，本脚本按名称关联 category_id）
-- 执行：mysql -u root -p fire_recommendation < seed_news_sample.sql
-- ============================================================

USE fire_recommendation;

-- urgency_level：1 低 2 中 3 高；status 1 正常；deleted 0
-- category_id：按字典名称解析，避免依赖固定自增 id（与 13 脚本中 INSERT IGNORE 顺序无关）
INSERT INTO news (title, content, region, category_id, urgency_level, publisher_id, summary, view_count, status, deleted, publish_time, create_time, update_time)
SELECT v.title, v.content, v.region, c.id, v.urgency_level, v.publisher_id, v.summary, v.view_count, v.status, v.deleted, NOW(), NOW(), NOW()
FROM (
  SELECT '本市开展春季消防安全大检查' AS title,
         '<p>为落实消防安全责任，本周起在全市范围内开展春季消防安全大检查，重点针对商场、学校、医院等人员密集场所的疏散通道、消防设施及用火用电管理。</p><p>市民如发现火灾隐患可拨打 12345 举报。</p>' AS content,
         '本市' AS region, '政策法规' AS cat_name, 1 AS urgency_level, 1 AS publisher_id,
         '春季消防安全大检查启动，重点检查人员密集场所。' AS summary, 320 AS view_count, 1 AS status, 0 AS deleted
  UNION ALL SELECT '某小区电动车充电引发火情，无人员伤亡',
         '<p>昨日夜间，某小区地下车库一电动车在充电过程中冒烟起火，物业微型站及时处置并报警，火情被扑灭，无人员伤亡。消防提醒：电动车请到集中充电点充电，勿进楼入户。</p>',
         '本市', '案例警示', 2, 1, '电动车充电引发火情，物业及时处置。', 856, 1, 0
  UNION ALL SELECT '高层住宅疏散演练进社区',
         '<p>为提高居民自救能力，某街道联合消防部门在多个高层小区开展疏散演练，内容包括低姿捂鼻、使用逃生面罩、识别疏散指示等。参与居民表示收获很大。</p>',
         '本市', '演练培训', 1, 1, '高层住宅疏散演练进社区，居民参与踊跃。', 156, 1, 0
  UNION ALL SELECT '灭火器使用与维保公益讲座本周六举行',
         '<p>市消防协会将于本周六上午在市民活动中心举办灭火器使用与维保公益讲座，现场可实操体验。名额有限，请提前报名。</p>',
         NULL, '演练培训', 1, 1, '灭火器使用与维保公益讲座，可现场实操。', 89, 1, 0
  UNION ALL SELECT '省消防总队发布家庭防火提示',
         '<p>省消防总队提醒：家庭防火请注意用电安全、厨房用火、电动车充电、楼道畅通等；建议家中配备灭火器、灭火毯、逃生面罩和独立式感烟探测器。</p>',
         '本省', '政策法规', 1, 1, '省消防总队发布家庭防火提示。', 442, 1, 0
  UNION ALL SELECT '某商场消防演练：3 分钟完成疏散',
         '<p>某大型商场昨日进行无预案消防演练，从报警到人员疏散完毕约 3 分钟。消防部门指出，日常演练和清晰的疏散指示是保障安全的关键。</p>',
         '本省', '演练培训', 1, 1, '商场消防演练 3 分钟完成疏散。', 278, 1, 0
  UNION ALL SELECT '老旧小区消防设施改造项目启动',
         '<p>今年将推进多个老旧小区消防设施改造，包括增设消火栓、更新灭火器、补装独立式感烟探测器等。街道将征求居民意见后实施。</p>',
         '本市', '装备动态', 1, 1, '老旧小区消防设施改造项目启动。', 198, 1, 0
  UNION ALL SELECT '森林防火期来临，严禁野外用火',
         '<p>即日起进入森林防火期。有关部门提醒：严禁在林区及周边吸烟、野炊、烧荒等；发现火情请立即报警并远离火场。</p>',
         '本省', '政策法规', 3, 1, '森林防火期来临，严禁野外用火。', 512, 1, 0
) v
INNER JOIN news_category c ON c.name = v.cat_name AND c.deleted = 0
WHERE NOT EXISTS (SELECT 1 FROM news x WHERE x.title = v.title AND x.deleted = 0);

-- 已为库内「无分类」的新闻补全 category_id（含手工插入的测试稿）；可按标题增删
UPDATE news n
INNER JOIN news_category c ON c.name = '演练培训' AND c.deleted = 0
SET n.category_id = c.id
WHERE n.category_id IS NULL AND n.deleted = 0 AND n.title = '测试新闻';

-- 若此前用旧版种子插入过样本、仅缺 category_id，执行下列 UPDATE 即可（不会插入重复行）
UPDATE news n INNER JOIN news_category c ON c.name = '政策法规' AND c.deleted = 0
SET n.category_id = c.id
WHERE n.category_id IS NULL AND n.deleted = 0 AND n.title IN (
  '本市开展春季消防安全大检查', '省消防总队发布家庭防火提示', '森林防火期来临，严禁野外用火');
UPDATE news n INNER JOIN news_category c ON c.name = '案例警示' AND c.deleted = 0
SET n.category_id = c.id
WHERE n.category_id IS NULL AND n.deleted = 0 AND n.title = '某小区电动车充电引发火情，无人员伤亡';
UPDATE news n INNER JOIN news_category c ON c.name = '演练培训' AND c.deleted = 0
SET n.category_id = c.id
WHERE n.category_id IS NULL AND n.deleted = 0 AND n.title IN (
  '高层住宅疏散演练进社区', '灭火器使用与维保公益讲座本周六举行', '某商场消防演练：3 分钟完成疏散');
UPDATE news n INNER JOIN news_category c ON c.name = '装备动态' AND c.deleted = 0
SET n.category_id = c.id
WHERE n.category_id IS NULL AND n.deleted = 0 AND n.title = '老旧小区消防设施改造项目启动';

-- 若自增 id 冲突或重复执行 INSERT：可先删对应标题再跑本脚本，或只执行上方 UPDATE
