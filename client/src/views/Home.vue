<template>
  <div class="home">
    <h1>消防科普推荐系统 - 用户端</h1>
    <p>前端框架已就绪，可与 Apifox 并行联调。请先启动后端（<code>backend</code> 下 <code>mvn spring-boot:run</code>），接口基地址：<code>http://localhost:8088/api</code>。</p>
    <div class="links">
      <router-link to="/knowledge">知识列表</router-link>
      <router-link to="/forum">论坛</router-link>
      <router-link to="/recommend">个性化推荐</router-link>
      <router-link to="/equipment">器材查询</router-link>
      <router-link to="/news">新闻</router-link>
    </div>
  </div>
</template>

<style scoped>
.home { max-width: 720px; }
.home h1 { margin-bottom: 16px; font-size: 22px; }
.home p { margin-bottom: 16px; color: #555; }
.home code { background: #eee; padding: 2px 6px; border-radius: 4px; }
.links { display: flex; flex-wrap: wrap; gap: 12px; }
.links a { padding: 10px 16px; background: #1890ff; color: #fff; text-decoration: none; border-radius: 4px; }
</style>
