<template>
  <div class="layout">
    <button v-if="toastMsg" type="button" class="toast-msg clickable" role="status" @click="goNotifications">{{ toastMsg }}</button>

    <header class="navbar" :class="{ 'navbar-scrolled': isScrolled }">
      <div class="navbar-inner">
        <router-link to="/home" class="navbar-logo">
          <div class="logo-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M12 2L4.5 20.29L5.21 21L12 18L18.79 21L19.5 20.29L12 2Z" fill="currentColor" />
              <path d="M12 5.43L16.42 16.14L12 14.18L7.58 16.14L12 5.43Z" fill="white" fill-opacity="0.35" />
            </svg>
          </div>
          <span class="logo-text">消防科普</span>
        </router-link>

        <ul v-show="!isMobile" class="navbar-menu" aria-label="内容导航">
          <li v-for="item in mainMenu" :key="item.name">
            <router-link :to="item.to" class="menu-link" active-class="active">{{ item.name }}</router-link>
          </li>
        </ul>

        <div v-show="!isMobile" class="navbar-actions">
          <template v-if="userStore.isLoggedIn">
            <router-link to="/notifications" class="message-btn">
              <div class="icon-wrap">
                <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                  <path
                    d="M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9M13.73 21a2 2 0 01-3.46 0"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                  />
                </svg>
                <span v-if="unreadCount > 0" class="unread-badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
              </div>
              <span class="action-label">消息</span>
            </router-link>

            <div class="action-divider" aria-hidden="true" />

            <div
              class="user-profile"
              @mouseenter="dropdownOpen = true"
              @mouseleave="dropdownOpen = false"
            >
              <div class="avatar-wrap">
                <img v-if="avatarUrl" :src="avatarUrl" alt="" class="user-avatar" />
                <div v-else class="avatar-placeholder">{{ userInitial }}</div>
              </div>
              <span class="user-name">{{ userStore.user?.username || '用户' }}</span>
              <svg class="arrow-icon" :class="{ rotate: dropdownOpen }" viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.5" aria-hidden="true">
                <path d="M6 9l6 6 6-6" stroke-linecap="round" stroke-linejoin="round" />
              </svg>

              <transition name="dropdown">
                <div v-if="dropdownOpen" class="dropdown-menu">
                  <div class="dropdown-header">
                    <p class="dropdown-name">{{ userStore.user?.username }}</p>
                    <p class="dropdown-role">{{ roleLabel }}</p>
                  </div>
                  <div class="dropdown-divider" />
                  <router-link to="/forum/mine" class="dropdown-item" @click="dropdownOpen = false">
                    <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                      <path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7" stroke-linecap="round" stroke-linejoin="round" />
                      <path d="M18.5 2.5a2.121 2.121 0 113 3L12 15l-4 1 1-4 9.5-9.5z" stroke-linecap="round" stroke-linejoin="round" />
                    </svg>
                    我的帖子
                  </router-link>
                  <router-link to="/knowledge/collect" class="dropdown-item" @click="dropdownOpen = false">
                    <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                      <path d="M19 21l-7-5-7 5V5a2 2 0 012-2h10a2 2 0 012 2z" stroke-linecap="round" stroke-linejoin="round" />
                    </svg>
                    我的收藏
                  </router-link>
                  <router-link
                    v-if="userStore.user?.role === 'AUTHOR' || userStore.user?.role === 'ADMIN'"
                    to="/knowledge/drafts"
                    class="dropdown-item"
                    @click="dropdownOpen = false"
                  >
                    <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                      <path d="M4 19.5A2.5 2.5 0 016.5 17H20" stroke-linecap="round" stroke-linejoin="round" />
                      <path d="M6.5 2H20v20H6.5A2.5 2.5 0 014 19.5v-15A2.5 2.5 0 016.5 2z" stroke-linecap="round" stroke-linejoin="round" />
                    </svg>
                    我的知识
                  </router-link>
                  <router-link
                    v-if="userStore.user?.role !== 'AUTHOR' && userStore.user?.role !== 'ADMIN'"
                    to="/apply-author"
                    class="dropdown-item"
                    @click="dropdownOpen = false"
                  >
                    <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                      <path d="M16 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2" stroke-linecap="round" stroke-linejoin="round" />
                      <circle cx="8.5" cy="7" r="4" />
                      <path d="M20 8v6M23 11h-6" stroke-linecap="round" stroke-linejoin="round" />
                    </svg>
                    申请作者
                  </router-link>
                  <router-link to="/profile" class="dropdown-item" @click="dropdownOpen = false">
                    <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                      <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2" stroke-linecap="round" stroke-linejoin="round" />
                      <circle cx="12" cy="7" r="4" />
                    </svg>
                    个人中心
                  </router-link>
                  <div class="dropdown-divider" />
                  <button type="button" class="dropdown-item logout-btn" @click="onLogoutClick">
                    <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                      <path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4M16 17l5-5-5-5M21 12H9" stroke-linecap="round" stroke-linejoin="round" />
                    </svg>
                    退出登录
                  </button>
                </div>
              </transition>
            </div>
          </template>
          <template v-else>
            <router-link to="/login" class="auth-link">登录</router-link>
            <router-link to="/register" class="auth-link auth-link--solid">注册</router-link>
          </template>
        </div>

        <button v-if="isMobile" type="button" class="mobile-toggle" aria-label="打开菜单" @click="drawerOpen = true">
          <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
            <line x1="3" y1="12" x2="21" y2="12" />
            <line x1="3" y1="6" x2="21" y2="6" />
            <line x1="3" y1="18" x2="21" y2="18" />
          </svg>
        </button>
      </div>
    </header>

    <Teleport to="body">
      <div v-if="isMobile && drawerOpen" class="drawer-mask" @click="drawerOpen = false" />
      <aside v-if="isMobile && drawerOpen" class="drawer-panel">
        <div class="drawer-head">
          <span>导航</span>
          <button type="button" class="drawer-close" aria-label="关闭" @click="drawerOpen = false">×</button>
        </div>
        <nav class="nav-drawer" @click="onDrawerNavClick">
          <router-link to="/home">首页</router-link>
          <span class="drawer-section">内容浏览</span>
          <router-link :to="{ name: 'KnowledgeList', query: { view: 'smart' } }">知识</router-link>
          <router-link to="/forum">论坛</router-link>
          <router-link to="/equipment">器材</router-link>
          <router-link to="/news">新闻</router-link>
          <template v-if="userStore.isLoggedIn">
            <span class="drawer-section">我的</span>
            <router-link to="/forum/mine">我的帖子</router-link>
            <router-link to="/knowledge/collect">我的收藏</router-link>
            <router-link to="/notifications">消息 {{ unreadCount > 0 ? '(' + unreadCount + ')' : '' }}</router-link>
            <router-link v-if="userStore.user?.role === 'AUTHOR' || userStore.user?.role === 'ADMIN'" to="/knowledge/drafts">我的知识</router-link>
            <router-link v-if="userStore.user?.role !== 'AUTHOR' && userStore.user?.role !== 'ADMIN'" to="/apply-author">申请作者</router-link>
            <router-link to="/profile">个人中心</router-link>
            <button type="button" class="drawer-btn" @click="logout">退出登录</button>
          </template>
          <template v-else>
            <span class="drawer-section">账户</span>
            <router-link to="/login">登录</router-link>
            <router-link to="/register">注册</router-link>
          </template>
        </nav>
      </aside>
    </Teleport>

    <main class="main">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getUserInfo, getUnreadNotificationCount } from '@/api/user'
import { SITE_NOTIFICATIONS_EVENT } from '@/constants/siteNotifications'
import { BADGE_POLL_VISIBLE_MS, BADGE_POLL_HIDDEN_MS } from '@/constants/notificationPoll'
import { resolveMediaUrl } from '@/utils/resolveMediaUrl'
import { createNotificationWebSocket } from '@/utils/notificationWebSocket'

const router = useRouter()
const userStore = useUserStore()
const notifWs = createNotificationWebSocket('client_token')
const unreadCount = ref(0)
const toastMsg = ref('')
let lastUnreadSnapshot = -1
let toastClearTimer = null
const isMobile = ref(false)
const drawerOpen = ref(false)
const isScrolled = ref(false)
const dropdownOpen = ref(false)
const MOBILE_MAX = 768

const mainMenu = [
  { name: '知识', to: { name: 'KnowledgeList', query: { view: 'smart' } } },
  { name: '论坛', to: '/forum' },
  { name: '器材', to: '/equipment' },
  { name: '新闻', to: '/news' },
]

const avatarUrl = computed(() => resolveMediaUrl(userStore.user?.avatar || ''))
const userInitial = computed(() => {
  const n = userStore.user?.username || '用'
  return String(n).charAt(0).toUpperCase()
})

const roleLabel = computed(() => {
  const r = userStore.user?.role
  if (r === 'ADMIN') return '管理员'
  if (r === 'AUTHOR') return '作者'
  return '普通用户'
})

let heartbeatTimer = null
let checking = false
let unreadTimer = null
const ACTIVE_HEARTBEAT_MS = 3000
const HIDDEN_HEARTBEAT_MS = 15000

function restartUnreadPoll() {
  if (unreadTimer) {
    clearInterval(unreadTimer)
    unreadTimer = null
  }
  if (!userStore.isLoggedIn) {
    return
  }
  const ms = document.visibilityState === 'visible' ? BADGE_POLL_VISIBLE_MS : BADGE_POLL_HIDDEN_MS
  unreadTimer = setInterval(fetchUnread, ms)
}

function updateMobile() {
  isMobile.value = window.innerWidth <= MOBILE_MAX
  if (!isMobile.value) {
    drawerOpen.value = false
  }
}

function onScrollNav() {
  isScrolled.value = window.scrollY > 8
}

function onDrawerNavClick(e) {
  if (e.target.tagName === 'A') {
    drawerOpen.value = false
  }
}

function onLogoutClick() {
  dropdownOpen.value = false
  logout()
}

function logout() {
  notifWs.close()
  drawerOpen.value = false
  if (unreadTimer) {
    clearInterval(unreadTimer)
    unreadTimer = null
  }
  lastUnreadSnapshot = -1
  unreadCount.value = 0
  userStore.logout()
  router.push('/home')
}

function showToast(msg) {
  toastMsg.value = msg
  if (toastClearTimer) clearTimeout(toastClearTimer)
  toastClearTimer = setTimeout(() => {
    toastMsg.value = ''
    toastClearTimer = null
  }, 3200)
}

function goNotifications() {
  toastMsg.value = ''
  router.push('/notifications')
}

async function fetchUnread() {
  if (!userStore.isLoggedIn) {
    unreadCount.value = 0
    lastUnreadSnapshot = -1
    return
  }
  try {
    const data = await getUnreadNotificationCount()
    const n = Number(data?.count ?? 0)
    const prev = lastUnreadSnapshot
    if (prev >= 0 && n > prev) {
      showToast('您有新的消息，请点击「消息」查看')
    }
    lastUnreadSnapshot = n
    unreadCount.value = n
    if (prev >= 0 && n !== prev) {
      window.dispatchEvent(new CustomEvent(SITE_NOTIFICATIONS_EVENT))
    }
  } catch (_) {
    unreadCount.value = 0
  }
}

async function checkSession() {
  if (!userStore.isLoggedIn || checking) return
  checking = true
  try {
    const info = await getUserInfo()
    if (info?.role) {
      userStore.updateUser({ role: info.role })
    }
  } catch (_) {
    /* 401 由 request 处理 */
  } finally {
    checking = false
  }
}

function onNotificationsUpdated() {
  fetchUnread()
}

watch(
  () => userStore.isLoggedIn,
  (loggedIn) => {
    if (loggedIn) {
      notifWs.restart()
    } else {
      notifWs.close()
    }
  },
  { immediate: true },
)

onMounted(() => {
  updateMobile()
  window.addEventListener('resize', updateMobile)
  window.addEventListener('scroll', onScrollNav, { passive: true })
  checkSession()
  fetchUnread()
  heartbeatTimer = setInterval(checkSession, ACTIVE_HEARTBEAT_MS)
  restartUnreadPoll()
  document.addEventListener('visibilitychange', resetHeartbeatByVisibility)
  window.addEventListener('focus', fetchUnread)
  window.addEventListener(SITE_NOTIFICATIONS_EVENT, onNotificationsUpdated)
})

onUnmounted(() => {
  notifWs.close()
  window.removeEventListener('resize', updateMobile)
  window.removeEventListener('scroll', onScrollNav)
  if (heartbeatTimer) {
    clearInterval(heartbeatTimer)
    heartbeatTimer = null
  }
  document.removeEventListener('visibilitychange', resetHeartbeatByVisibility)
  window.removeEventListener('focus', fetchUnread)
  window.removeEventListener(SITE_NOTIFICATIONS_EVENT, onNotificationsUpdated)
  if (unreadTimer) {
    clearInterval(unreadTimer)
    unreadTimer = null
  }
})

function resetHeartbeatByVisibility() {
  if (heartbeatTimer) {
    clearInterval(heartbeatTimer)
  }
  const ms = document.visibilityState === 'visible' ? ACTIVE_HEARTBEAT_MS : HIDDEN_HEARTBEAT_MS
  heartbeatTimer = setInterval(checkSession, ms)
  restartUnreadPoll()
  if (document.visibilityState === 'visible') {
    fetchUnread()
  }
}
</script>

<style scoped>
.toast-msg {
  position: fixed;
  top: 72px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 9999;
  max-width: min(420px, 92vw);
  padding: 10px 16px;
  border-radius: 8px;
  background: rgba(15, 23, 42, 0.92);
  color: #fff;
  font-size: 14px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
  border: none;
}
.toast-msg.clickable {
  cursor: pointer;
  pointer-events: auto;
}

.layout {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  padding-top: 64px;
}

/* —— 顶栏 —— */
.navbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 64px;
  z-index: 1000;
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-bottom: 1px solid rgba(59, 130, 246, 0.14);
  transition:
    box-shadow 0.25s ease,
    background 0.25s ease,
    border-color 0.25s ease;
}

.navbar-scrolled {
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 4px 20px rgba(59, 130, 246, 0.1);
  border-bottom-color: rgba(59, 130, 246, 0.12);
}

.navbar-inner {
  max-width: var(--client-content-max, 1680px);
  margin: 0 auto;
  height: 100%;
  padding: 0 clamp(14px, 2vw, 28px);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.navbar-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
  color: inherit;
  flex-shrink: 0;
  transition: transform 0.2s;
}

.navbar-logo:hover {
  transform: scale(1.02);
}

.logo-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: linear-gradient(135deg, var(--client-primary) 0%, var(--client-accent) 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.35);
}

.logo-icon svg {
  width: 22px;
  height: 22px;
}

.logo-text {
  font-size: 1.125rem;
  font-weight: 800;
  color: var(--client-text);
  letter-spacing: -0.03em;
  white-space: nowrap;
}

.navbar-menu {
  display: flex;
  list-style: none;
  gap: 4px;
  margin: 0;
  padding: 0;
  flex: 1;
  justify-content: center;
  min-width: 0;
}

.navbar-menu li {
  margin: 0;
}

.menu-link {
  display: block;
  text-decoration: none;
  padding: 8px 14px;
  font-size: 0.9375rem;
  font-weight: 600;
  color: var(--client-muted);
  border-radius: 10px;
  transition:
    color 0.2s,
    background 0.2s;
  white-space: nowrap;
}

.menu-link:hover {
  color: var(--client-primary);
  background: rgba(59, 130, 246, 0.08);
}

.menu-link.active {
  color: var(--client-primary);
  background: rgba(59, 130, 246, 0.14);
  font-weight: 700;
}

.navbar-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.message-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 4px 8px;
  border-radius: 10px;
  text-decoration: none;
  color: var(--client-muted);
  transition:
    color 0.2s,
    background 0.2s;
}

.message-btn:hover {
  color: var(--client-primary);
  background: rgba(59, 130, 246, 0.08);
}

.icon-wrap {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}

.unread-badge {
  position: absolute;
  top: -5px;
  right: -8px;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  font-size: 10px;
  font-weight: 800;
  line-height: 16px;
  text-align: center;
  background: #ef4444;
  color: #fff;
  border-radius: 8px;
  border: 2px solid #fff;
}

.action-label {
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.02em;
}

.action-divider {
  width: 1px;
  height: 26px;
  background: rgba(59, 130, 246, 0.2);
}

.user-profile {
  position: relative;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 8px 4px 4px;
  border-radius: 12px;
  cursor: default;
  transition: background 0.2s;
}

.user-profile:hover {
  background: rgba(59, 130, 246, 0.08);
}

.avatar-wrap {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
  background: linear-gradient(135deg, var(--client-primary), var(--client-accent));
  border: 2px solid rgba(255, 255, 255, 0.95);
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-placeholder {
  color: #fff;
  font-weight: 800;
  font-size: 0.875rem;
}

.user-avatar {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.user-name {
  font-size: 0.8125rem;
  font-weight: 700;
  color: var(--client-text);
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.arrow-icon {
  color: var(--client-muted);
  flex-shrink: 0;
  transition: transform 0.2s;
}

.arrow-icon.rotate {
  transform: rotate(180deg);
}

.dropdown-menu {
  position: absolute;
  top: calc(100% + 10px);
  left: calc(4px + 34px / 2);
  right: auto;
  width: 228px;
  background: var(--client-surface);
  border-radius: 14px;
  box-shadow: 0 12px 40px rgba(59, 130, 246, 0.15);
  border: 1px solid rgba(59, 130, 246, 0.12);
  padding: 10px;
  transform: translateX(-50%);
  transform-origin: top center;
}

.dropdown-header {
  padding: 6px 10px 10px;
}

.dropdown-name {
  margin: 0;
  font-size: 0.875rem;
  font-weight: 800;
  color: var(--client-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dropdown-role {
  margin: 4px 0 0;
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--client-primary);
}

.dropdown-divider {
  height: 1px;
  background: rgba(59, 130, 246, 0.1);
  margin: 6px 0;
}

.dropdown-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 10px;
  text-decoration: none;
  color: var(--client-text);
  font-size: 0.8125rem;
  font-weight: 600;
  border-radius: 8px;
  transition:
    background 0.2s,
    color 0.2s;
  border: none;
  background: none;
  width: 100%;
  text-align: left;
  cursor: pointer;
  font-family: inherit;
}

.dropdown-item:hover {
  background: var(--client-accent-soft);
  color: var(--client-primary);
}

.logout-btn:hover {
  background: rgba(254, 226, 226, 0.65);
  color: #dc2626;
}

.dropdown-enter-active,
.dropdown-leave-active {
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.dropdown-enter-from,
.dropdown-leave-to {
  opacity: 0;
  transform: translateX(-50%) scale(0.96) translateY(-6px);
}

.auth-link {
  padding: 8px 16px;
  font-size: 0.8125rem;
  font-weight: 700;
  text-decoration: none;
  border-radius: 10px;
  color: var(--client-muted);
  transition:
    background 0.2s,
    color 0.2s;
}

.auth-link:hover {
  color: var(--client-primary);
  background: rgba(59, 130, 246, 0.08);
}

.auth-link--solid {
  background: var(--client-primary);
  color: #fff;
  box-shadow: 0 2px 10px rgba(59, 130, 246, 0.35);
}

.auth-link--solid:hover {
  background: var(--client-primary-hover);
  color: #fff;
}

.mobile-toggle {
  display: none;
  padding: 8px;
  background: transparent;
  border: none;
  color: var(--client-text);
  cursor: pointer;
  border-radius: 10px;
}

.mobile-toggle:hover {
  background: rgba(59, 130, 246, 0.1);
}

.main {
  flex: 1;
  overflow: auto;
  padding: 16px;
  background: var(--client-bg, #f0f7fc);
}

.drawer-mask {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  z-index: 1100;
}
.drawer-panel {
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  width: min(300px, 88vw);
  background: linear-gradient(180deg, #f8fbff 0%, #f0f7fc 100%);
  z-index: 1101;
  display: flex;
  flex-direction: column;
  box-shadow: -6px 0 32px rgba(59, 130, 246, 0.12);
}
.drawer-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 18px;
  color: var(--client-text);
  font-weight: 700;
  font-size: 1rem;
  border-bottom: 1px solid rgba(59, 130, 246, 0.12);
}
.drawer-close {
  background: none;
  border: none;
  color: var(--client-text);
  font-size: 26px;
  line-height: 1;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 8px;
}
.drawer-close:hover {
  background: rgba(59, 130, 246, 0.1);
}
.nav-drawer {
  display: flex;
  flex-direction: column;
  padding: 12px;
  gap: 2px;
  overflow-y: auto;
}
.drawer-section {
  display: block;
  margin: 14px 14px 6px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: rgba(30, 58, 95, 0.45);
  text-transform: uppercase;
}
.drawer-section:first-of-type {
  margin-top: 4px;
}
.nav-drawer a {
  color: var(--client-text);
  text-decoration: none;
  padding: 12px 14px;
  border-radius: 10px;
  font-weight: 600;
  font-size: 0.875rem;
}
.nav-drawer a:hover {
  background: rgba(59, 130, 246, 0.1);
}
.drawer-btn {
  margin-top: 10px;
  padding: 12px 14px;
  background: transparent;
  color: #b91c1c;
  border: 1px solid rgba(59, 130, 246, 0.22);
  border-radius: 10px;
  cursor: pointer;
  text-align: left;
  font-weight: 600;
  font-size: 0.875rem;
}

@media (max-width: 768px) {
  .navbar-menu,
  .navbar-actions {
    display: none !important;
  }
  .mobile-toggle {
    display: block;
  }
  .navbar-inner {
    padding: 0 12px;
  }
}

@media (min-width: 769px) {
  .main {
    padding: 20px clamp(16px, 2.5vw, 40px) 28px;
  }
}
</style>
