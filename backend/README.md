# 消防科普推荐系统 - 后端

基于 Spring Boot 3 + MyBatis-Plus + MySQL + Redis + JWT 的后端项目。

## 技术栈

- Java 17
- Spring Boot 3.2
- MyBatis-Plus（分页、逻辑删除）
- MySQL 5.6（建表脚本见项目根目录 DB 文件夹，兼容 MySQL 5.6.x）
- Redis（帖子点赞 Set、可选缓存）
- JWT（登录态）
- Lombok、Validation、EasyExcel、JavaMail

## 目录结构

```
src/main/java/com/fire/recommendation/
├── FireRecommendationApplication.java   # 启动类
├── common/          # 统一响应 Result、PageResult、ResultCode
├── config/          # Cors、MyBatis-Plus、JWT、Security(PasswordEncoder)、WebMvc
├── controller/      # 用户端接口：user、content、forum、recommend、equipment、news
├── controller/admin/# 管理端接口：user、content/category、forum/audit、equipment、news、statistics
├── dto/             # 请求/响应 DTO、VO
├── entity/          # 实体（对应 DB 表）
├── exception/       # BusinessException、GlobalExceptionHandler
├── interceptor/     # JwtInterceptor（认证与认证可选路径）
├── mapper/          # MyBatis-Plus Mapper
├── service/        # 业务接口与实现
└── util/            # JwtUtil
```

## 运行前准备

1. **JDK 17**：本工程需 **Java 17** 及以上（Spring Boot 3 要求）。在 cmd 中执行 `java -version` 确认版本；若为 8/11，请安装 [JDK 17](https://adoptium.net/) 并将该 JDK 设为 `JAVA_HOME` 后再执行 Maven。
2. **数据库**：执行 `DB/` 目录下 SQL（先 `00_init_database.sql`，再 `01`～`06`）。
3. **Redis**：本地启动 Redis（默认 6379）。
4. **配置**：修改 `application.yml` / `application-dev.yml` 中的数据库、Redis、邮箱等。

## 启动

```bash
mvn spring-boot:run
```

默认端口 8088，上下文路径 `/api`，接口基础地址：`http://localhost:8088/api`。

## 启动失败排查（BUILD FAILURE / exit code 1）

Maven 只显示「Process terminated with exit code: 1」，真实原因在 Spring Boot 的启动日志里。请按下面做：

1. **先看完整错误**（在项目目录下执行）：
   ```bash
   mvn spring-boot:run -e
   ```
   在控制台**往上滚动**，找到第一次出现的 **红色 Caused by:** 或 **Error/Exception**，根据提示处理。

2. **常见原因与处理**：
   - **MySQL 连接失败**（如 `Communications link failure`、`Access denied`）  
     → 确认 MySQL 已启动；数据库 `fire_recommendation` 已建好；`application.yml` 里 `url`、`username`、`password` 正确（当前为 root / 123456）。
   - **Redis 连接失败**（如 `Connection refused`、`NOAUTH`）  
     → 确认 Redis 已启动；若 Redis **未设密码**，把 `application.yml` 里 `spring.data.redis.password` 删掉或改为空；若**有密码**，则填成与 Redis 一致（当前配置为 123456）。
   - **端口 8088 被占用**（如 `Port 8088 was already in use`）  
     → 关闭占用 8088 的程序，或把 `server.port` 改成其他端口。
   - **其他报错**  
     → 把 `mvn spring-boot:run -e` 输出里**从第一个 Caused by 到 BUILD FAILURE 之间**的几行贴出来，便于进一步排查。

## 接口说明

详见项目根目录下 `DOCX/后端接口文档.md`。除登录、注册、密码找回及文档中标注的公开接口外，其余需在请求头携带：`Authorization: Bearer <token>`。

## 未实现/待扩展

- 邮箱重置密码（sendResetEmail、resetByToken）：需配置 Spring Mail（JavaMailSender）才能真实发邮件，否则重置链接仅在服务端日志中输出。
- 知识内容详情中的「作者名」需关联用户表查询（当前 toDetailMap 可扩展）。
- 推荐逻辑：当前为简化版（有行为按浏览量排序；无行为全站热门），可扩展为按用户兴趣分类 Top1 + 标签。**智能推荐 - AI 模型接入**：可配置 ai.recommend.enabled、api-url、api-key，在推荐列表返回前调用外部 AI/推荐服务补充 recommendReason 或按模型重排序，异常时降级为规则推荐。
- 统计与导出：StatisticsServiceImpl 中 contentStats、userStats、interactionStats、categoryPie、viewTrend、exportExcel 需按表统计与 EasyExcel 导出。
- 器材批量导入：EquipmentServiceImpl.importExcel 需解析 Excel 并批量插入。
