<template>
  <div class="notif-detail-page">
    <nav class="notif-bc" aria-label="面包屑">
      <router-link to="/dashboard" class="notif-bc-link">首页</router-link>
      <span class="notif-bc-sep" aria-hidden="true">/</span>
      <router-link to="/notifications" class="notif-bc-link">消息通知</router-link>
      <template v-if="detail">
        <span class="notif-bc-sep" aria-hidden="true">/</span>
        <span class="notif-bc-current" :title="detail.title">{{ crumbTitle }}</span>
      </template>
      <template v-else-if="loading">
        <span class="notif-bc-sep" aria-hidden="true">/</span>
        <span class="notif-bc-current">加载中…</span>
      </template>
      <template v-else-if="errMsg">
        <span class="notif-bc-sep" aria-hidden="true">/</span>
        <span class="notif-bc-current">加载失败</span>
      </template>
    </nav>

    <article v-if="detail" class="notif-detail">
      <div class="notif-detail-top">
        <router-link to="/notifications" class="notif-back">
          <svg class="notif-back-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
            <line x1="19" y1="12" x2="5" y2="12" />
            <polyline points="12 19 5 12 12 5" />
          </svg>
          返回消息列表
        </router-link>
      </div>

      <header class="notif-detail-hero">
        <h1 class="notif-detail-title">{{ detail.title || '通知' }}</h1>
        <time class="notif-detail-time" :datetime="detail.createTime">{{ formatDateTime(detail.createTime) }}</time>
      </header>

      <div class="notif-detail-body-wrap">
        <div class="notif-detail-body">{{ detail.content }}</div>
      </div>

      <div v-if="detail.actionUrl" class="notif-detail-actions">
        <button type="button" class="notif-detail-goto" @click="goAction(detail.actionUrl)">
          {{ detail.actionText || '去查看' }}
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
            <path d="M5 12h14M12 5l7 7-7 7" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
        </button>
      </div>
    </article>

    <div v-else-if="!loading && errMsg" class="notif-state notif-state--error">
      <p>{{ errMsg }}</p>
      <router-link to="/notifications" class="notif-state-link">返回消息列表</router-link>
    </div>

    <div v-else class="notif-state">
      <div class="notif-spinner" />
      <p>加载中…</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getNotificationDetail, markNotificationRead } from '@/api/notifications'
import { SITE_NOTIFICATIONS_EVENT } from '@/constants/siteNotifications'
import { formatDateTime } from '@/utils/formatDateTime'

const route = useRoute()
const router = useRouter()
const id = computed(() => route.params.id)
const detail = ref(null)
const loading = ref(true)
const errMsg = ref('')

const crumbTitle = computed(() => {
  const t = detail.value?.title || '详情'
  return t.length > 20 ? `${t.slice(0, 20)}…` : t
})

function isUnread(item) {
  const v = item?.isRead ?? item?.is_read
  if (v === 0 || v === '0') return true
  if (v === false) return true
  return false
}

function emitUnreadChanged() {
  window.dispatchEvent(new CustomEvent(SITE_NOTIFICATIONS_EVENT))
}

function goAction(url) {
  if (!url) return
  router.push(url)
}

async function load() {
  detail.value = null
  errMsg.value = ''
  loading.value = true
  try {
    const data = await getNotificationDetail(id.value)
    detail.value = data
    if (data && isUnread(data)) {
      try {
        await markNotificationRead(data.id)
        detail.value = { ...data, isRead: 1 }
        emitUnreadChanged()
      } catch (_) {}
    }
  } catch (e) {
    errMsg.value = typeof e === 'string' ? e : e?.message || '加载失败'
  } finally {
    loading.value = false
  }
}

watch(
  id,
  () => {
    if (id.value == null || id.value === '') return
    load()
  },
  { immediate: true }
)

function onVisibility() {
  if (document.visibilityState === 'visible' && id.value) {
    load()
  }
}

onMounted(() => {
  document.addEventListener('visibilitychange', onVisibility)
  window.addEventListener('focus', onVisibility)
})

onUnmounted(() => {
  document.removeEventListener('visibilitychange', onVisibility)
  window.removeEventListener('focus', onVisibility)
})
</script>

<style scoped>
.notif-detail-page {
  min-width: 0;
  max-width: 960px;
  margin: 0 auto;
}

.notif-bc {
  font-size: 0.8125rem;
  color: var(--client-muted);
  line-height: 1.5;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px 6px;
  margin-bottom: 14px;
}

.notif-bc-link {
  color: var(--client-primary);
  text-decoration: none;
}

.notif-bc-link:hover {
  text-decoration: underline;
  color: var(--client-primary-hover);
}

.notif-bc-sep {
  opacity: 0.55;
}

.notif-bc-current {
  color: var(--client-text);
  font-weight: 500;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notif-detail {
  min-width: 0;
}

.notif-detail-top {
  margin-bottom: 16px;
}

.notif-back {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--client-primary);
  text-decoration: none;
}

.notif-back:hover {
  color: var(--client-primary-hover);
  text-decoration: underline;
}

.notif-back-icon {
  width: 18px;
  height: 18px;
}

.notif-detail-hero {
  margin-bottom: 18px;
  padding-bottom: 18px;
  border-bottom: 1px solid rgba(59, 130, 246, 0.12);
}

.notif-detail-title {
  margin: 0 0 10px;
  font-size: 1.35rem;
  font-weight: 800;
  color: var(--client-text);
  line-height: 1.35;
  letter-spacing: -0.02em;
}

.notif-detail-time {
  font-size: 0.8125rem;
  color: var(--client-muted);
}

.notif-detail-body-wrap {
  border-radius: var(--client-radius);
  border: 1px solid rgba(59, 130, 246, 0.1);
  background: rgba(240, 247, 252, 0.45);
  overflow: hidden;
}

.notif-detail-body {
  margin: 0;
  padding: 18px 20px;
  font-size: 0.9375rem;
  line-height: 1.75;
  color: var(--client-text);
  white-space: pre-wrap;
  word-break: break-word;
}

.notif-detail-actions {
  margin-top: 22px;
}

.notif-detail-goto {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  border: none;
  border-radius: 10px;
  background: var(--client-primary);
  color: #fff;
  font-size: 0.875rem;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 4px 14px rgba(59, 130, 246, 0.35);
  transition: background 0.2s;
}

.notif-detail-goto:hover {
  background: var(--client-primary-hover);
}

.notif-state {
  text-align: center;
  padding: 40px 16px;
  color: var(--client-muted);
}

.notif-state--error {
  color: var(--client-text);
}

.notif-state--error p {
  margin: 0 0 16px;
}

.notif-state-link {
  color: var(--client-primary);
  font-weight: 600;
  text-decoration: none;
}

.notif-state-link:hover {
  text-decoration: underline;
}

.notif-spinner {
  width: 40px;
  height: 40px;
  margin: 0 auto 12px;
  border: 3px solid rgba(59, 130, 246, 0.2);
  border-top-color: var(--client-primary);
  border-radius: 50%;
  animation: notif-d-spin 0.75s linear infinite;
}

@keyframes notif-d-spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
