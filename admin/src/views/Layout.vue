<template>
  <el-container class="layout">
    <!-- 桌面侧栏 -->
    <el-aside v-show="!isMobile" width="220px" class="sidebar">
      <div class="logo">
        <span class="logo-mark" />
        <span class="logo-text">消防管理端</span>
      </div>
      <el-menu
        class="admin-sidebar-menu"
        :default-active="activeMenu"
        background-color="transparent"
        text-color="rgba(255,255,255,0.78)"
        active-text-color="#ffffff"
        router
      >
        <el-menu-item v-for="item in menuItems" :key="item.index" :index="item.index">
          <template v-if="item.notifBadge">
            <el-badge :value="notifUnread" :hidden="notifUnread <= 0" :max="99" class="sidebar-badge">
              <span>{{ item.label }}</span>
            </el-badge>
          </template>
          <span v-else>{{ item.label }}</span>
        </el-menu-item>
      </el-menu>
      <div class="user-bar">
        <span class="user-name">{{ userStore.user?.username }}</span>
        <el-button type="primary" link @click="logout">退出</el-button>
      </div>
    </el-aside>

    <!-- 移动端顶栏 + 抽屉菜单 -->
    <el-container v-if="isMobile" direction="vertical" class="mobile-wrap">
      <header class="mobile-header">
        <el-button class="nav-btn" text @click="drawerOpen = true" aria-label="打开菜单">
          <span class="nav-icon" />
        </el-button>
        <span class="mobile-title">消防管理端</span>
        <span class="mobile-user">{{ userStore.user?.username }}</span>
      </header>
      <el-drawer v-model="drawerOpen" direction="ltr" size="240px" :with-header="false" class="nav-drawer">
        <div class="logo drawer-logo">消防管理端</div>
        <el-menu
          class="admin-sidebar-menu"
          :default-active="activeMenu"
          background-color="transparent"
          text-color="rgba(255,255,255,0.78)"
          active-text-color="#ffffff"
          router
          @select="drawerOpen = false"
        >
          <el-menu-item v-for="item in menuItems" :key="'d-' + item.index" :index="item.index">
            <template v-if="item.notifBadge">
              <el-badge :value="notifUnread" :hidden="notifUnread <= 0" :max="99" class="sidebar-badge">
                <span>{{ item.label }}</span>
              </el-badge>
            </template>
            <span v-else>{{ item.label }}</span>
          </el-menu-item>
        </el-menu>
        <div class="user-bar drawer-user">
          <el-button type="primary" link @click="logout">退出登录</el-button>
        </div>
      </el-drawer>
      <el-main class="main mobile-main">
        <router-view />
      </el-main>
    </el-container>

    <el-main v-show="!isMobile" class="main">
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup>
import { computed, onMounted, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElNotification } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getUnreadNotificationCount } from '@/api/notifications'
import { SITE_NOTIFICATIONS_EVENT } from '@/constants/siteNotifications'
import { BADGE_POLL_VISIBLE_MS, BADGE_POLL_HIDDEN_MS } from '@/constants/notificationPoll'
import { createNotificationWebSocket } from '@/utils/notificationWebSocket'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const notifWs = createNotificationWebSocket('admin_token')

/** 合并模块侧栏统一高亮到父级菜单 index */
const activeMenu = computed(() => {
  const p = route.path
  if (p.startsWith('/user')) {
    return '/user/list'
  }
  if (p.startsWith('/notifications')) {
    return '/notifications'
  }
  if (p.startsWith('/system')) {
    return '/system/recycle'
  }
  if (p.startsWith('/knowledge') || p.startsWith('/content')) {
    return '/knowledge/list'
  }
  if (p.startsWith('/forum')) {
    return '/forum/posts'
  }
  if (p.startsWith('/equipment')) {
    return '/equipment/list'
  }
  if (p.startsWith('/news')) {
    return '/news/list'
  }
  if (p.startsWith('/comments')) {
    const t = (route.query.type || '').toString().toLowerCase()
    if (t === 'forum') return '/forum/posts'
    if (t === 'knowledge') return '/knowledge/list'
    return '/news/list'
  }
  return p
})
const isMobile = ref(false)
const drawerOpen = ref(false)
const MOBILE_MAX = 768

const menuItems = [
  { index: '/dashboard', label: '首页' },
  { index: '/notifications', label: '消息通知', notifBadge: true },
  { index: '/user/list', label: '用户管理' },
  { index: '/knowledge/list', label: '知识管理' },
  { index: '/forum/posts', label: '论坛管理' },
  { index: '/equipment/list', label: '器材管理' },
  { index: '/news/list', label: '新闻管理' },
  { index: '/system/recycle', label: '系统配置' },
]

function logout() {
  notifWs.close()
  drawerOpen.value = false
  if (notifPollTimer) {
    window.clearInterval(notifPollTimer)
    notifPollTimer = null
  }
  lastNotifUnreadSnap = -1
  notifUnread.value = 0
  userStore.logout()
  router.push('/login')
}

function updateMobile() {
  isMobile.value = window.innerWidth <= MOBILE_MAX
}

const notifUnread = ref(0)
let lastNotifUnreadSnap = -1
let notifPollTimer = null
/** WS 已送达但轮询可能抢先更新了未读数，用节流补一次弹窗 */
let lastWsFallbackToastAt = 0
const WS_FALLBACK_TOAST_MS = 8000

function restartNotifPoll() {
  if (notifPollTimer) {
    window.clearInterval(notifPollTimer)
    notifPollTimer = null
  }
  if (!userStore.isLoggedIn) {
    return
  }
  const ms = document.visibilityState === 'visible' ? BADGE_POLL_VISIBLE_MS : BADGE_POLL_HIDDEN_MS
  notifPollTimer = window.setInterval(fetchNotifUnread, ms)
}

function showNewMessageNotification() {
  ElNotification({
    title: '新消息',
    message: '您有新的站内消息（含论坛帖子待审核、评论待审阅等，正文中会说明详情），请点击「消息通知」查看。',
    type: 'info',
    duration: 8000,
    showClose: true,
    onClick: () => {
      router.push('/notifications')
    },
  })
}

async function fetchNotifUnread(fromWebSocket = false) {
  if (!userStore.isLoggedIn) {
    notifUnread.value = 0
    lastNotifUnreadSnap = -1
    return
  }
  try {
    const data = await getUnreadNotificationCount()
    const n = Number(data?.count ?? 0)
    const prev = lastNotifUnreadSnap
    const increased = prev >= 0 && n > prev
    if (increased) {
      showNewMessageNotification()
    } else if (fromWebSocket && n > 0 && prev >= 0) {
      const now = Date.now()
      // 轮询可能抢先更新了 lastSnap（n===prev），仍要给管理员实时弹窗提示
      if (now - lastWsFallbackToastAt >= WS_FALLBACK_TOAST_MS) {
        lastWsFallbackToastAt = now
        showNewMessageNotification()
      }
    }
    lastNotifUnreadSnap = n
    notifUnread.value = n
    if (prev >= 0 && n !== prev) {
      window.dispatchEvent(new CustomEvent(SITE_NOTIFICATIONS_EVENT))
    }
  } catch (_) {
    notifUnread.value = 0
  }
}

function onSiteNotificationsUpdated(ev) {
  fetchNotifUnread(ev?.detail?.fromWebSocket === true)
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
  fetchNotifUnread()
  restartNotifPoll()
  window.addEventListener('focus', fetchNotifUnread)
  document.addEventListener('visibilitychange', onDocVisibilityForNotif)
  window.addEventListener(SITE_NOTIFICATIONS_EVENT, onSiteNotificationsUpdated)
})

function onDocVisibilityForNotif() {
  restartNotifPoll()
  if (document.visibilityState === 'visible') {
    fetchNotifUnread()
  }
}

onBeforeUnmount(() => {
  notifWs.close()
  window.removeEventListener('resize', updateMobile)
  if (notifPollTimer) {
    window.clearInterval(notifPollTimer)
    notifPollTimer = null
  }
  window.removeEventListener('focus', fetchNotifUnread)
  document.removeEventListener('visibilitychange', onDocVisibilityForNotif)
  window.removeEventListener(SITE_NOTIFICATIONS_EVENT, onSiteNotificationsUpdated)
})
</script>

<style scoped>
.layout {
  height: 100%;
}
.sidebar {
  background: var(--admin-sidebar-bg, #0f172a);
  display: flex;
  flex-direction: column;
  border-right: 1px solid var(--admin-sidebar-border);
}
.sidebar :deep(.el-menu) {
  border-right: none;
  flex: 1;
}
.sidebar :deep(.el-menu-item) {
  height: 46px;
  line-height: 46px;
  margin: 2px 8px;
  border-radius: 8px;
}
.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 18px 16px;
  font-weight: 700;
  font-size: 15px;
  color: #fff;
  letter-spacing: -0.02em;
  border-bottom: 1px solid var(--admin-sidebar-border);
}
.logo-mark {
  width: 10px;
  height: 28px;
  border-radius: 4px;
  background: linear-gradient(180deg, var(--client-primary), var(--client-accent));
  flex-shrink: 0;
}
.logo-text {
  flex: 1;
  min-width: 0;
}
.user-bar {
  padding: 12px 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  font-size: 13px;
  color: rgba(255, 255, 255, 0.9);
}
.sidebar-badge :deep(.el-badge__content) {
  border: none;
}
.user-name {
  margin-right: 8px;
}
.user-bar :deep(.el-button) {
  color: rgba(255, 255, 255, 0.9);
}
.main {
  overflow: auto;
  padding: 24px;
  background: var(--admin-main-bg, #f0f7fc);
}

.mobile-wrap {
  flex: 1;
  min-width: 0;
  width: 100%;
  min-height: 100%;
  background: var(--admin-main-bg, #f0f7fc);
}
.mobile-header {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 52px;
  padding: 0 14px;
  background: var(--admin-sidebar-bg, #0f172a);
  color: #fff;
  flex-shrink: 0;
  border-bottom: 1px solid var(--admin-sidebar-border);
}
.nav-btn {
  padding: 8px;
  color: #fff;
}
.nav-icon {
  display: block;
  width: 18px;
  height: 2px;
  background: #fff;
  box-shadow: 0 -6px 0 #fff, 0 6px 0 #fff;
  border-radius: 1px;
}
.mobile-title {
  flex: 1;
  font-weight: 600;
  font-size: 15px;
}
.mobile-user {
  font-size: 12px;
  opacity: 0.85;
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.mobile-main {
  padding: 16px;
}
.drawer-logo {
  margin: -8px 0 8px;
}
.drawer-user {
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  margin-top: 16px;
  padding-top: 12px;
}
.nav-drawer :deep(.el-drawer__body) {
  padding: 16px;
  background: var(--admin-sidebar-bg, #0f172a);
}
</style>
