<template>
  <el-container class="layout">
    <el-aside width="200px" class="sidebar">
      <div class="logo">消防管理端</div>
      <el-menu
        :default-active="activeMenu"
        background-color="#001529"
        text-color="rgba(255,255,255,0.8)"
        active-text-color="#fff"
        router
      >
        <el-menu-item index="/dashboard">首页概览</el-menu-item>
        <el-menu-item index="/user">用户管理</el-menu-item>
        <el-menu-item index="/content">知识内容</el-menu-item>
        <el-menu-item index="/category">分类管理</el-menu-item>
        <el-menu-item index="/forum">帖子审核</el-menu-item>
        <el-menu-item index="/equipment">器材管理</el-menu-item>
        <el-menu-item index="/news">新闻管理</el-menu-item>
        <el-menu-item index="/statistics">数据统计</el-menu-item>
      </el-menu>
      <div class="user-bar">
        <span class="user-name">{{ userStore.user?.username }}</span>
        <el-button type="primary" link @click="logout">退出</el-button>
      </div>
    </el-aside>
    <el-main class="main">
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)

function logout() {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.layout {
  height: 100%;
}
.sidebar {
  background: #001529;
  display: flex;
  flex-direction: column;
}
.sidebar :deep(.el-menu) {
  border-right: none;
}
.sidebar :deep(.el-menu-item) {
  height: 48px;
  line-height: 48px;
}
.logo {
  padding: 16px;
  font-weight: bold;
  color: #fff;
  border-bottom: 1px solid rgba(255,255,255,0.1);
}
.user-bar {
  padding: 12px 16px;
  border-top: 1px solid rgba(255,255,255,0.1);
  font-size: 13px;
  color: rgba(255,255,255,0.9);
}
.user-name {
  margin-right: 8px;
}
.user-bar :deep(.el-button) {
  color: rgba(255,255,255,0.9);
}
.main {
  overflow: auto;
  padding: 24px;
  background: #f0f2f5;
}
</style>
