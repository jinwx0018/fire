# 消防科普推荐系统 - 管理端前端

Vue3 + Vite + Vue Router + Pinia + Axios，与后端联调、和 Apifox 并行测试用。

## 技术栈

- Vue 3、Vite 5
- Vue Router 4、Pinia、Axios

## 环境要求

- Node.js 18+
- 后端已启动：在项目根目录 `backend` 下执行 `mvn spring-boot:run`，接口基地址 `http://localhost:8088/api`

## 安装与运行

```bash
cd admin
npm install
npm run dev
```

浏览器访问：http://localhost:5173（Vite 代理将 `/api` 转发到 8088）

## 登录

- 若没有管理员账号：先在库中插入（见项目根目录 `DOCX/接口测试步骤.md`），或先注册用户再在库中把 `role_id` 改为 3。
- 默认示例：用户名 `admin`，密码 `123456`（需先在库中存在且为管理员角色）。

## 与 Apifox 联调说明

- 接口基地址与前端一致：`http://localhost:8088/api`。
- 需要登录的接口在 Apifox 中设置请求头：`Authorization: Bearer <token>`，token 可从登录接口响应中获取，或登录管理端后浏览器控制台执行 `localStorage.getItem('admin_token')` 复制。
- 前端请求均通过 Vite 代理发到同一后端，便于一边在 Apifox 测接口、一边在页面上操作验证。

## 目录结构

```
src/
├── api/          # 接口封装（request.js + 各模块）
├── router/       # 路由
├── stores/       # Pinia 状态（用户）
└── views/        # 页面（登录、布局、用户/内容/分类/论坛/器材/新闻/统计）
```

## 构建

```bash
npm run build
```

产出在 `dist/`，可部署到任意静态服务器；若后端与前端同域且带 `/api` 代理，需相应配置 Nginx 等。
