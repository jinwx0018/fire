# 消防科普推荐系统 - 数据库脚本说明

## 适用版本

- **MySQL 5.6.x**（建议 5.6.5 及以上；含 FULLTEXT 的表需 **5.6.4** 及以上，以支持 InnoDB 全文索引）

## 数据库

- 数据库名：`fire_recommendation`
- 字符集：`utf8mb4`
- 排序规则：`utf8mb4_unicode_ci`

## 执行顺序

在 MySQL 5.6 中按以下顺序执行（若已创建库且 `USE fire_recommendation`，可只执行 01～06）：

| 序号 | 文件 | 说明 |
|------|------|------|
| 0 | `00_init_database.sql` | 创建数据库并 USE |
| 1 | `01_sys_user.sql` | 用户、角色、密码重置 Token（邮箱找回） |
| 2 | `02_knowledge.sql` | 知识分类、内容、标签、用户收藏 |
| 3 | `03_forum.sql` | 论坛帖子、评论 |
| 4 | `04_user_behavior.sql` | 用户行为（推荐用） |
| 5 | `05_equipment.sql` | 器材类型、器材 |
| 6 | `06_news.sql` | 新闻 |
| 7 | `07_author_application.sql` | 作者申请表（用户申请成为作者，管理员审核） |
| 8 | `08_knowledge_reject_reason.sql` | 知识内容审核驳回原因字段 |
| 9 | `09_sys_role_three_roles.sql` | 对齐三角色 id：1=USER 2=AUTHOR 3=ADMIN（修复仅有两条角色或错乱） |
| 10 | `10_user_security_and_audit.sql` | 用户会话治理、作者申请增强、审计日志、通知、账号软删除 |
| 12 | `12_news_cover_category.sql` | 新闻表增加 `cover_url`、`category`（**若已用新版 `06_news.sql` 建表含两列则不必执行**） |
| 13 | `13_news_category_comment_like_rss.sql` | 新闻分类字典表、评论表、点赞表；**新版 `06_news.sql` 已含 `category_id`、`like_count` 时勿再 ALTER news** |
| 13b | `13b_news_alter_legacy_columns.sql` | **仅旧库**：为 `news` 补 `category_id`、`like_count`（已用新版 06+13 可跳过） |
| 14 | `14_news_ngram_fulltext_optional.sql` | **可选**：`news` 上 **title+summary** 的 FULLTEXT（脚本默认**无 ngram 解析器**，兼容 MariaDB）；**`WITH PARSER ngram` 仅 Oracle MySQL 5.7.6+**。若执行 ngram 报 **1128**，用脚本内方案乙即可。配合 `app.news.ngram-fulltext-enabled: true` 与 `searchMode=ngram` |
| 19 | `19_comment_like.sql` | 三类评论表增加 `like_count`；建 `user_comment_like`（**无 Redis 时**评论点赞持久化）。若列已存在 ALTER 报错可忽略对应行 |

## 表与模块对应关系

| 模块 | 主要表 |
|------|--------|
| 用户管理 | sys_role, sys_user, sys_password_reset_token |
| 知识内容 | knowledge_category, knowledge_content, knowledge_tag, knowledge_content_tag, user_collection |
| 论坛 | forum_post, forum_comment（帖子点赞 Redis `forum:post:like:{id}`；**评论点赞** Redis `forum:comment:like:{id}` 或表 `user_comment_like`） |
| 推荐 | user_behavior |
| 器材 | equipment_type, equipment |
| 新闻 | news、news_category、news_comment、news_like；评论点赞 Redis `news:comment:like:{id}` 或 `user_comment_like` |

## MySQL 5.6 兼容说明

- 使用 **TIMESTAMP** 作为 `create_time` / `update_time`，以兼容 5.6.0 的 `DEFAULT CURRENT_TIMESTAMP` 与 `ON UPDATE CURRENT_TIMESTAMP`。
- 不含默认时间的字段（如 `expire_time`、`publish_time`）仍使用 **DATETIME**。
- 含 **FULLTEXT** 索引的表（knowledge_content、forum_post、equipment、news）需 **MySQL 5.6.4+**（InnoDB 全文索引）。

## 一键执行（命令行示例）

```bash
mysql -u root -p < 00_init_database.sql
mysql -u root -p fire_recommendation < 01_sys_user.sql
mysql -u root -p fire_recommendation < 02_knowledge.sql
mysql -u root -p fire_recommendation < 03_forum.sql
mysql -u root -p fire_recommendation < 04_user_behavior.sql
mysql -u root -p fire_recommendation < 05_equipment.sql
mysql -u root -p fire_recommendation < 06_news.sql
mysql -u root -p fire_recommendation < 07_author_application.sql
mysql -u root -p fire_recommendation < 08_knowledge_reject_reason.sql
mysql -u root -p fire_recommendation < 09_sys_role_three_roles.sql
mysql -u root -p fire_recommendation < 10_user_security_and_audit.sql
```

或在 MySQL 5.6 客户端中依次执行各文件内容即可。

## 测试/种子数据（可选）

在 **01～06 建表脚本** 执行完成后，可按需执行以下种子数据，便于联调与测试。

| 序号 | 文件 | 说明 | 依赖 |
|------|------|------|------|
| - | `seed_user_sample.sql` | 测试用户 5 个（admin/author1/user1～3），密码均为 **password** | 01 |
| - | `seed_knowledge_sample.sql` | 知识分类 5 个 + 知识内容约 10 条（author_id=1） | 01、02 |
| - | `seed_knowledge_extra.sql` | 知识标签、内容-标签关联、用户收藏 | 02 + 上述用户与知识 |
| - | `seed_forum_sample.sql` | 论坛帖子 5 条、评论若干 | 01、03 + 用户 |
| - | `seed_behavior_sample.sql` | 用户行为（VIEW/LIKE/COMMENT/COLLECT） | 01、02、03 + 用户与内容、帖子 |
| - | `seed_equipment_sample.sql` | 器材类型 5 个 + 器材约 10 条 | 05 |
| - | `seed_news_sample.sql` | 新闻约 8 条 | 01、06 + 用户 |

**推荐执行顺序**（在 `USE fire_recommendation` 或已选库后）：

```bash
# 建表
mysql -u root -p fire_recommendation < 01_sys_user.sql
mysql -u root -p fire_recommendation < 02_knowledge.sql
mysql -u root -p fire_recommendation < 03_forum.sql
mysql -u root -p fire_recommendation < 04_user_behavior.sql
mysql -u root -p fire_recommendation < 05_equipment.sql
mysql -u root -p fire_recommendation < 06_news.sql

# 种子数据（按顺序）
mysql -u root -p fire_recommendation < seed_user_sample.sql
mysql -u root -p fire_recommendation < seed_knowledge_sample.sql
mysql -u root -p fire_recommendation < seed_knowledge_extra.sql
mysql -u root -p fire_recommendation < seed_forum_sample.sql
mysql -u root -p fire_recommendation < seed_behavior_sample.sql
mysql -u root -p fire_recommendation < seed_equipment_sample.sql
mysql -u root -p fire_recommendation < seed_news_sample.sql
```

- **测试账号**：`admin` / `author1` / `user1`～`user3`，密码均为 **password**（BCrypt 与 Spring Security 兼容）。
- 若本机已存在管理员且 id 非 1，可将各 seed 中的 `author_id`、`user_id`、`publisher_id` 等改为对应用户 id 后执行。
