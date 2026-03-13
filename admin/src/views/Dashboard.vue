<template>
  <el-card>
    <template #header>
      <span>首页概览</span>
    </template>
    <p>管理端已接入 Element Plus。请确保后端已启动（<code>backend</code> 目录下 <code>mvn spring-boot:run</code>），接口基地址：<code>http://localhost:8088/api</code>。</p>
    <p>左侧菜单可进入各模块进行列表、新增、编辑、删除等操作与接口联调。</p>
  </el-card>
</template>

<style scoped>
p { margin-bottom: 12px; color: #555; font-size: 14px; }
code { background: #f5f5f5; padding: 2px 6px; border-radius: 4px; font-size: 13px; }
</style>
