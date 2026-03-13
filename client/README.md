# 消防科普推荐系统 - 用户端前端

Vue3 + Vite + Vue Router + Pinia + Axios，与后端联调、和 Apifox 并行测试用。

## 技术栈

- Vue 3、Vite 5
- Vue Router 4、Pinia、Axios

## 环境要求

- Node.js 18+
- 后端已启动：在项目根目录 `backend` 下执行 `mvn spring-boot:run`，接口基地址 `http://localhost:8088/api`

## 安装与运行

```bash
cd client
npm install
npm run dev
```

浏览器访问：http://localhost:5174（Vite 代理将 `/api` 转发到 8088）

## 与 Apifox 联调说明

- 接口基地址：`http://localhost:8088/api`。
- 需要登录的接口在请求头加：`Authorization: Bearer <token>`，token 来自 `POST /user/login` 或前端登录后 `localStorage.getItem('client_token')`。
- 前端与 Apifox 共用同一后端，可一边在 Apifox 测接口、一边在页面操作验证。

## 目录结构

```
src/
├── api/          # 接口封装（user、content、forum、recommend、equipment、news）
├── router/       # 路由
├── stores/       # Pinia（用户登录态）
└── views/        # 页面（登录/注册、首页、知识/论坛/推荐/器材/新闻、个人中心）
```

## 构建

```bash
npm run build
```

产出在 `dist/`。
