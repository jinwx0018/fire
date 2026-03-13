<template>
  <div class="layout">
    <header class="header">
      <router-link to="/home" class="logo">消防科普</router-link>
      <nav>
        <router-link to="/knowledge">知识</router-link>
        <router-link to="/forum">论坛</router-link>
        <router-link to="/recommend">推荐</router-link>
        <router-link to="/equipment">器材</router-link>
        <router-link to="/news">新闻</router-link>
        <template v-if="userStore.isLoggedIn">
          <router-link to="/profile" class="user-wrap">
            <img v-if="userStore.user?.avatar" :src="userStore.user.avatar" alt="" class="header-avatar" />
            <span class="header-username">{{ userStore.user?.username }}</span>
          </router-link>
          <button type="button" @click="logout">退出</button>
        </template>
        <template v-else>
          <router-link to="/login">登录</router-link>
          <router-link to="/register">注册</router-link>
        </template>
      </nav>
    </header>
    <main class="main">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

function logout() {
  userStore.logout()
  router.push('/home')
}
</script>

<style scoped>
.layout { display: flex; flex-direction: column; height: 100%; }
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  height: 56px;
  background: #001529;
  color: #fff;
}
.logo { font-weight: bold; color: #fff; text-decoration: none; }
.header nav { display: flex; align-items: center; gap: 16px; }
.header nav a { color: rgba(255,255,255,0.85); text-decoration: none; font-size: 14px; }
.header nav a:hover { color: #fff; }
.user-wrap { display: inline-flex; align-items: center; gap: 8px; }
.header-avatar { width: 28px; height: 28px; border-radius: 50%; object-fit: cover; vertical-align: middle; }
.header-username { font-size: 14px; }
.header nav button { padding: 4px 12px; background: transparent; color: #fff; border: 1px solid rgba(255,255,255,0.5); border-radius: 4px; cursor: pointer; font-size: 13px; }
.main { flex: 1; overflow: auto; padding: 24px; background: #f5f5f5; }
</style>
