<template>
  <KnowledgeModuleShell :crumbs="crumbs">
    <div class="notif-page">
      <div class="notif-layout">
        <aside class="notif-sidebar" aria-label="消息分类">
          <div class="notif-sidebar-head">
            <h2 class="notif-sidebar-title">消息中心</h2>
            <p class="notif-sidebar-hint">站内提醒与互动通知</p>
          </div>
          <nav class="notif-nav" role="tablist">
            <button
              type="button"
              role="tab"
              class="notif-nav-item"
              :class="{ 'is-active': currentTab === 'all' }"
              :aria-selected="currentTab === 'all'"
              @click="setTab('all')"
            >
              <span class="notif-nav-icon" aria-hidden="true">
                <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M4 6h16M4 12h16M4 18h16" stroke-linecap="round" stroke-linejoin="round" />
                </svg>
              </span>
              <span class="notif-nav-label">全部消息</span>
            </button>
            <button
              type="button"
              role="tab"
              class="notif-nav-item"
              :class="{ 'is-active': currentTab === 'unread' }"
              :aria-selected="currentTab === 'unread'"
              @click="setTab('unread')"
            >
              <span class="notif-nav-icon" aria-hidden="true">
                <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
                  <path
                    d="M21 11.5a8.38 8.38 0 01-.9 3.8 8.5 8.5 0 11-7.6-10.6 8.5 8.5 0 013.8.9L21 4l-1.5 5.5z"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                  />
                </svg>
              </span>
              <span class="notif-nav-label">未读</span>
              <span v-if="unreadCount > 0" class="notif-nav-badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
            </button>
            <button
              type="button"
              role="tab"
              class="notif-nav-item"
              :class="{ 'is-active': currentTab === 'read' }"
              :aria-selected="currentTab === 'read'"
              @click="setTab('read')"
            >
              <span class="notif-nav-icon" aria-hidden="true">
                <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M22 11.08V12a10 10 0 11-5.93-9.14" />
                  <path d="M22 4L12 14.01l-3-3" stroke-linecap="round" stroke-linejoin="round" />
                </svg>
              </span>
              <span class="notif-nav-label">已读</span>
            </button>
          </nav>
        </aside>

        <main class="notif-main">
          <header class="notif-header">
            <div class="notif-header-left">
              <h3 class="notif-header-title">{{ currentTabTitle }}</h3>
              <span class="notif-header-count">共 {{ total }} 条</span>
            </div>
            <div class="notif-header-actions">
              <button
                v-if="list.length"
                type="button"
                class="notif-btn notif-btn--ghost"
                @click="markAll"
              >
                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                  <path d="M20 6L9 17l-5-5" stroke-linecap="round" stroke-linejoin="round" />
                </svg>
                全部已读
              </button>
            </div>
          </header>

          <div v-if="loading" class="notif-loading">
            <div class="notif-spinner" />
            <p>加载中…</p>
          </div>

          <div v-else-if="list.length" class="notif-list">
            <div
              v-for="item in list"
              :key="item.id"
              class="notif-item"
              :class="{ 'is-unread': isUnread(item) }"
              role="button"
              tabindex="0"
              @click="onItemClick(item)"
              @keydown.enter.prevent="onItemClick(item)"
            >
              <div class="notif-item-avatar" aria-hidden="true">
                <div class="notif-avatar-ph" :style="{ background: avatarBg(item) }">
                  {{ avatarLetter(item) }}
                </div>
                <span v-if="isUnread(item)" class="notif-unread-dot" />
              </div>
              <div class="notif-item-body">
                <div class="notif-item-top">
                  <span class="notif-item-title">{{ item.title || '通知' }}</span>
                  <time class="notif-item-time" :datetime="item.createTime">{{ formatDateTime(item.createTime) }}</time>
                </div>
                <p class="notif-item-preview">{{ item.content || '—' }}</p>
                <p v-if="item.actionUrl" class="notif-item-jump">{{ item.actionText || '点击查看' }}</p>
                <div class="notif-item-foot">
                  <span class="notif-item-more" @click.stop="onItemClick(item)">
                    查看详情
                    <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                      <path d="M9 18l6-6-6-6" stroke-linecap="round" stroke-linejoin="round" />
                    </svg>
                  </span>
                </div>
              </div>
            </div>
          </div>

          <div v-else class="notif-empty">
            <div class="notif-empty-icon" aria-hidden="true">
              <svg viewBox="0 0 24 24" width="72" height="72" fill="none" stroke="rgba(59,130,246,0.35)" stroke-width="1.2">
                <path
                  d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                />
              </svg>
            </div>
            <p class="notif-empty-title">暂无消息</p>
            <span class="notif-empty-hint">休息一下，稍后再来查看吧</span>
          </div>

          <PaginationBar
            v-if="total > pageSize"
            v-model="pageNum"
            class="notif-pager"
            :total="total"
            :page-size="pageSize"
            @current-change="() => load()"
          />
        </main>
      </div>
    </div>
  </KnowledgeModuleShell>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  getNotificationPage,
  markAllNotificationsRead,
  markNotificationRead,
  getUnreadNotificationCount,
} from '@/api/user'
import { SITE_NOTIFICATIONS_EVENT } from '@/constants/siteNotifications'
import { LIST_POLL_VISIBLE_MS, LIST_POLL_HIDDEN_MS } from '@/constants/notificationPoll'
import { formatDateTime } from '@/utils/formatDateTime'
import PaginationBar from '@/components/PaginationBar.vue'
import KnowledgeModuleShell from '@/components/KnowledgeModuleShell.vue'

const AVATAR_COLORS = [
  '#3b82f6',
  '#0ea5e9',
  '#2563eb',
  '#6366f1',
  '#8b5cf6',
]

const router = useRouter()
const route = useRoute()

let listPollTimer = null
let reloadDebounce = null

const list = ref([])
const loading = ref(true)
const pageNum = ref(1)
const pageSize = 10
const total = ref(0)
const currentTab = ref('all')
const unreadCount = ref(0)

const crumbs = computed(() => [
  { label: '首页', to: '/home' },
  { label: '消息通知', to: '' },
])

const currentTabTitle = computed(() => {
  const m = { all: '全部消息', unread: '未读消息', read: '已读消息' }
  return m[currentTab.value] || '消息通知'
})

function isUnread(item) {
  const v = item?.isRead ?? item?.is_read
  if (v === 0 || v === '0') return true
  if (v === false) return true
  return false
}

function avatarLetter(item) {
  const t = (item?.title || '消').trim()
  return t.charAt(0).toUpperCase()
}

function avatarBg(item) {
  const id = item?.id ?? 0
  const idx = Number(id) % AVATAR_COLORS.length
  return AVATAR_COLORS[idx]
}

function emitUnreadChanged() {
  window.dispatchEvent(new CustomEvent(SITE_NOTIFICATIONS_EVENT))
}

function scheduleReload() {
  if (reloadDebounce) clearTimeout(reloadDebounce)
  reloadDebounce = setTimeout(() => {
    reloadDebounce = null
    load()
  }, 280)
}

function onGlobalNotificationsUpdated() {
  scheduleReload()
}

function patchItemRead(nid) {
  const idStr = String(nid)
  const i = list.value.findIndex((x) => String(x.id) === idStr)
  if (i < 0) return
  const row = list.value[i]
  list.value.splice(i, 1, { ...row, isRead: 1, is_read: 1 })
}

async function fetchUnreadBadge() {
  try {
    const data = await getUnreadNotificationCount()
    unreadCount.value = Number(data?.count ?? 0)
  } catch {
    unreadCount.value = 0
  }
}

function setTab(id) {
  if (currentTab.value === id) return
  currentTab.value = id
  pageNum.value = 1
  load()
}

async function load(silent) {
  if (!silent) loading.value = true
  try {
    const params = { pageNum: pageNum.value, pageSize }
    if (currentTab.value === 'unread') params.isRead = 0
    if (currentTab.value === 'read') params.isRead = 1
    const data = await getNotificationPage(params)
    const records = data.records ?? data.list ?? []
    list.value = records
    total.value = data.total ?? 0
    await fetchUnreadBadge()
  } catch (e) {
    if (!silent) alert(e.message || '加载失败')
  } finally {
    if (!silent) loading.value = false
  }
}

async function onItemClick(item) {
  const nid = item.id
  if (nid == null || nid === '') return
  try {
    if (isUnread(item)) {
      await markNotificationRead(nid)
      patchItemRead(nid)
      emitUnreadChanged()
      await fetchUnreadBadge()
    }
  } catch (_) {}
  if (item.actionUrl) {
    router.push(item.actionUrl)
    return
  }
  router.push({ name: 'NotificationDetail', params: { id: String(nid) } })
}

async function markAll() {
  if (!confirm('确定将所有消息标记为已读？')) return
  try {
    await markAllNotificationsRead()
    await load()
    await fetchUnreadBadge()
    emitUnreadChanged()
  } catch (e) {
    alert(typeof e === 'string' ? e : e?.message || '操作失败')
  }
}

watch(
  () => route.name,
  (name, oldName) => {
    if (name === 'Notifications' && oldName === 'NotificationDetail') {
      load()
    }
  },
)

function restartListPoll() {
  if (listPollTimer) {
    clearInterval(listPollTimer)
    listPollTimer = null
  }
  const ms = document.visibilityState === 'visible' ? LIST_POLL_VISIBLE_MS : LIST_POLL_HIDDEN_MS
  listPollTimer = window.setInterval(() => load(true), ms)
}

function onWindowFocus() {
  load(true)
}

function onDocVisibility() {
  if (document.visibilityState === 'visible') {
    load(true)
  }
  restartListPoll()
}

onMounted(() => {
  load()
  fetchUnreadBadge()
  window.addEventListener(SITE_NOTIFICATIONS_EVENT, onGlobalNotificationsUpdated)
  restartListPoll()
  window.addEventListener('focus', onWindowFocus)
  document.addEventListener('visibilitychange', onDocVisibility)
})

onUnmounted(() => {
  window.removeEventListener(SITE_NOTIFICATIONS_EVENT, onGlobalNotificationsUpdated)
  document.removeEventListener('visibilitychange', onDocVisibility)
  window.removeEventListener('focus', onWindowFocus)
  if (reloadDebounce) clearTimeout(reloadDebounce)
  if (listPollTimer) {
    clearInterval(listPollTimer)
    listPollTimer = null
  }
})
</script>

<style scoped>
.notif-page {
  min-width: 0;
}

.notif-layout {
  display: flex;
  gap: 22px;
  align-items: flex-start;
  margin: -4px 0 0;
}

/* —— 侧边栏 —— */
.notif-sidebar {
  width: 232px;
  flex-shrink: 0;
  padding: 18px 12px 20px;
  border-radius: var(--client-radius);
  background: var(--client-surface);
  border: 1px solid rgba(59, 130, 246, 0.12);
  box-shadow: 0 2px 16px rgba(59, 130, 246, 0.06);
  position: sticky;
  top: 16px;
}

.notif-sidebar-head {
  padding: 4px 10px 14px;
  border-bottom: 1px solid rgba(59, 130, 246, 0.1);
  margin-bottom: 10px;
}

.notif-sidebar-title {
  margin: 0 0 6px;
  font-size: 1.0625rem;
  font-weight: 800;
  color: var(--client-text);
  letter-spacing: -0.02em;
}

.notif-sidebar-hint {
  margin: 0;
  font-size: 0.75rem;
  color: var(--client-muted);
  line-height: 1.4;
}

.notif-nav {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.notif-nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 11px 14px;
  border: none;
  border-radius: 10px;
  background: transparent;
  color: var(--client-muted);
  font-size: 0.9375rem;
  font-weight: 500;
  cursor: pointer;
  transition:
    background 0.2s,
    color 0.2s;
  text-align: left;
  position: relative;
}

.notif-nav-item:hover {
  background: rgba(59, 130, 246, 0.06);
  color: var(--client-text);
}

.notif-nav-item.is-active {
  background: var(--client-accent-soft);
  color: var(--client-primary);
  font-weight: 700;
}

.notif-nav-icon {
  display: flex;
  align-items: center;
  flex-shrink: 0;
  opacity: 0.92;
}

.notif-nav-item.is-active .notif-nav-icon {
  color: var(--client-primary);
}

.notif-nav-label {
  flex: 1;
  min-width: 0;
}

.notif-nav-badge {
  flex-shrink: 0;
  min-width: 20px;
  padding: 2px 7px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
  background: var(--client-primary);
  color: #fff;
  text-align: center;
}

/* —— 主区域 —— */
.notif-main {
  flex: 1;
  min-width: 0;
  border-radius: var(--client-radius);
  border: 1px solid rgba(59, 130, 246, 0.12);
  background: var(--client-surface);
  box-shadow: 0 2px 20px rgba(59, 130, 246, 0.06);
  display: flex;
  flex-direction: column;
  min-height: 420px;
}

.notif-header {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 18px 22px;
  border-bottom: 1px solid rgba(59, 130, 246, 0.1);
}

.notif-header-left {
  min-width: 0;
}

.notif-header-title {
  margin: 0 0 4px;
  font-size: 1.125rem;
  font-weight: 800;
  color: var(--client-text);
}

.notif-header-count {
  font-size: 0.8125rem;
  color: var(--client-muted);
}

.notif-header-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.notif-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border-radius: 10px;
  font-size: 0.8125rem;
  font-weight: 600;
  cursor: pointer;
  border: 1px solid rgba(59, 130, 246, 0.22);
  background: var(--client-surface);
  color: var(--client-muted);
  transition:
    background 0.2s,
    border-color 0.2s,
    color 0.2s;
}

.notif-btn:hover {
  background: rgba(240, 247, 252, 0.9);
  border-color: rgba(59, 130, 246, 0.35);
  color: var(--client-text);
}

.notif-btn--ghost {
  border-color: rgba(59, 130, 246, 0.2);
}

/* —— 加载 —— */
.notif-loading {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 56px 16px;
  color: var(--client-muted);
}

.notif-spinner {
  width: 36px;
  height: 36px;
  margin-bottom: 10px;
  border: 3px solid rgba(59, 130, 246, 0.2);
  border-top-color: var(--client-primary);
  border-radius: 50%;
  animation: notif-spin 0.75s linear infinite;
}

@keyframes notif-spin {
  to {
    transform: rotate(360deg);
  }
}

/* —— 列表 —— */
.notif-list {
  flex: 1;
}

.notif-item {
  display: flex;
  gap: 14px;
  padding: 20px 22px;
  border-bottom: 1px solid rgba(59, 130, 246, 0.08);
  cursor: pointer;
  transition: background 0.2s;
}

.notif-item:last-child {
  border-bottom: none;
}

.notif-item:hover {
  background: rgba(240, 247, 252, 0.45);
}

.notif-item.is-unread {
  background: rgba(224, 242, 254, 0.55);
}

.notif-item:focus-visible {
  outline: 2px solid var(--client-primary);
  outline-offset: -2px;
}

.notif-item-avatar {
  position: relative;
  flex-shrink: 0;
}

.notif-avatar-ph {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 800;
  font-size: 1.05rem;
}

.notif-unread-dot {
  position: absolute;
  top: 0;
  right: 0;
  width: 11px;
  height: 11px;
  background: var(--client-primary);
  border: 2px solid var(--client-surface);
  border-radius: 50%;
}

.notif-item-body {
  flex: 1;
  min-width: 0;
}

.notif-item-top {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px 12px;
  margin-bottom: 8px;
}

.notif-item-title {
  font-weight: 700;
  font-size: 0.9375rem;
  color: var(--client-text);
  line-height: 1.35;
}

.notif-item-time {
  font-size: 0.75rem;
  color: var(--client-muted);
  flex-shrink: 0;
}

.notif-item-preview {
  margin: 0 0 8px;
  font-size: 0.875rem;
  color: var(--client-muted);
  line-height: 1.55;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  white-space: pre-wrap;
  word-break: break-word;
}

.notif-item-jump {
  margin: 0 0 10px;
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--client-primary);
}

.notif-item-foot {
  display: flex;
  justify-content: flex-end;
}

.notif-item-more {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--client-muted);
  transition: color 0.2s;
}

.notif-item:hover .notif-item-more {
  color: var(--client-primary);
}

/* —— 空状态 —— */
.notif-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 56px 20px 72px;
  text-align: center;
}

.notif-empty-icon {
  margin-bottom: 16px;
  opacity: 0.85;
}

.notif-empty-title {
  margin: 0 0 6px;
  font-size: 1.0625rem;
  font-weight: 700;
  color: var(--client-text);
}

.notif-empty-hint {
  font-size: 0.875rem;
  color: var(--client-muted);
}

.notif-pager {
  padding: 16px 18px 20px;
  border-top: 1px solid rgba(59, 130, 246, 0.08);
}

@media (max-width: 900px) {
  .notif-layout {
    flex-direction: column;
  }

  .notif-sidebar {
    width: 100%;
    position: static;
  }

  .notif-nav {
    flex-direction: row;
    flex-wrap: wrap;
    gap: 8px;
  }

  .notif-nav-item {
    flex: 1;
    min-width: 0;
    justify-content: center;
  }

  .notif-nav-label {
    flex: none;
  }
}

@media (max-width: 520px) {
  .notif-nav-item {
    flex-direction: column;
    padding: 10px 8px;
    font-size: 0.8125rem;
  }

  .notif-nav-icon {
    margin: 0;
  }
}
</style>
