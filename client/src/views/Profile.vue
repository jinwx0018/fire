<template>
  <KnowledgeModuleShell :crumbs="crumbs">
    <div v-if="loading" class="state-loading">加载中…</div>

    <template v-else-if="info">
      <header class="page-head">
        <div class="page-head-text">
          <h1 class="page-title">个人中心</h1>
          <p class="page-sub">管理资料、推荐偏好与账号安全</p>
        </div>
      </header>

      <div class="profile-grid">
        <aside class="profile-aside">
          <div class="user-card">
            <div class="avatar-ring">
              <div class="avatar-wrap">
                <img v-if="form.avatar" :src="form.avatar" alt="头像" />
                <span v-else class="avatar-placeholder">{{ usernameInitial }}</span>
              </div>
            </div>
            <h2 class="user-name">{{ info.username }}</h2>
            <span class="role-pill">{{ roleLabel }}</span>
            <div class="user-meta">
              <p v-if="displayPhone" class="meta-line">{{ displayPhone }}</p>
              <p v-if="showEmailInSidebar && form.email" class="meta-line meta-email">{{ form.email }}</p>
            </div>
          </div>

          <nav class="side-nav" aria-label="个人中心分区">
            <button
              v-for="item in navItems"
              :key="item.key"
              type="button"
              class="side-nav-btn"
              :class="{ active: activeSection === item.key }"
              @click="activeSection = item.key"
            >
              {{ item.label }}
            </button>
          </nav>
        </aside>

        <main class="profile-main">
          <section class="stats-row" aria-label="数据概览">
            <router-link
              v-for="card in statCards"
              :key="card.key"
              :to="card.to"
              class="stat-card"
            >
              <span class="stat-value">{{ card.display }}</span>
              <span class="stat-label">{{ card.label }}</span>
            </router-link>
          </section>

          <!-- 总览 -->
          <div v-show="activeSection === 'overview'" class="section-stack">
            <div class="sub-card">
              <h3 class="sub-card-title">显示偏好</h3>
              <p class="sub-card-desc">以下开关仅影响本页侧栏展示，并保存在本机浏览器。</p>
              <div class="pref-row">
                <span class="pref-label">侧栏显示完整手机号</span>
                <ToggleSwitch v-model="showFullPhone" />
              </div>
              <div class="pref-row">
                <span class="pref-label">侧栏展示邮箱地址</span>
                <ToggleSwitch v-model="showEmailInSidebar" />
              </div>
            </div>

            <div class="sub-card">
              <h3 class="sub-card-title">快捷入口</h3>
              <div class="quick-grid">
                <router-link v-for="q in quickLinks" :key="q.to" :to="q.to" class="quick-tile">
                  <span class="quick-title">{{ q.label }}</span>
                  <span class="quick-sub">{{ q.sub }}</span>
                </router-link>
              </div>
            </div>
          </div>

          <!-- 资料 -->
          <div v-show="activeSection === 'profile'" class="section-stack">
            <div class="sub-card">
              <h3 class="sub-card-title">个人资料</h3>
              <form class="profile-form" @submit.prevent="saveInfo">
                <div class="field">
                  <label>用户名</label>
                  <input :value="info.username" disabled class="input disabled" />
                  <span class="hint">用户名不可修改</span>
                </div>
                <div class="field">
                  <label>头像</label>
                  <div class="avatar-row">
                    <div class="avatar-preview sm">
                      <img v-if="form.avatar" :src="form.avatar" alt="头像" />
                      <span v-else class="no-avatar">未设置</span>
                    </div>
                    <div class="avatar-upload">
                      <input
                        ref="fileInput"
                        type="file"
                        accept="image/jpeg,image/png,image/gif,image/webp"
                        class="file-input"
                        @change="onFileChange"
                      />
                      <button type="button" class="btn-outline" :disabled="uploadIng" @click="fileInput?.click()">
                        {{ uploadIng ? '上传中…' : '选择图片' }}
                      </button>
                      <span class="hint">支持 jpg、png、gif、webp，不超过 2MB</span>
                    </div>
                  </div>
                </div>
                <div class="field">
                  <label>手机号</label>
                  <input v-model="form.phone" type="text" class="input" placeholder="手机号" required />
                </div>
                <div class="field">
                  <label>邮箱</label>
                  <input v-model="form.email" type="email" class="input" placeholder="选填" />
                </div>
                <p v-if="infoErr" class="err">{{ infoErr }}</p>
                <button type="submit" class="btn-primary" :disabled="saving">保存资料</button>
              </form>
            </div>
          </div>

          <!-- 推荐偏好 -->
          <div v-show="activeSection === 'recommend'" class="section-stack">
            <div class="sub-card">
              <h3 class="sub-card-title">智能推荐 · 不感兴趣分类</h3>
              <p class="field-hint">
                勾选后，这些分类下的知识在「知识 → 智能推荐」中<strong>不会出现</strong>（筛选阶段过滤）；在「按时间」「热门」仍可正常浏览。
              </p>
              <p v-if="catLoading" class="hint">加载分类中…</p>
              <ul v-else class="cat-checks">
                <li v-for="c in categories" :key="c.id">
                  <label class="cat-label">
                    <input v-model="blockedCategoryIds" type="checkbox" :value="c.id" />
                    {{ c.name }}
                  </label>
                </li>
              </ul>
              <p v-if="!catLoading && categories.length === 0" class="hint">暂无分类数据</p>
              <p v-if="blockedErr" class="err">{{ blockedErr }}</p>
              <button type="button" class="btn-outline" :disabled="blockedSaving || catLoading" @click="saveBlocked">
                {{ blockedSaving ? '保存中…' : '保存屏蔽设置' }}
              </button>
            </div>
          </div>

          <!-- 账号与安全 -->
          <div v-show="activeSection === 'security'" class="section-stack">
            <div class="sub-card">
              <h3 class="sub-card-title">登录安全</h3>
              <p class="sub-card-desc">定期修改密码有助于保护账号。</p>
              <router-link to="/profile/password" class="btn-primary link-as-btn">修改登录密码</router-link>
            </div>
            <div v-if="info.role !== 'ADMIN'" class="sub-card danger-zone">
              <h3 class="sub-card-title danger-title">注销账号</h3>
              <p class="danger-tip">注销后账号将无法登录，数据按系统策略处理。需输入登录密码确认。</p>
              <div class="field">
                <label>确认密码</label>
                <input
                  v-model="deletePassword"
                  type="password"
                  class="input"
                  placeholder="请输入当前登录密码"
                  autocomplete="current-password"
                />
              </div>
              <button type="button" class="btn-danger" :disabled="deleting" @click="onSelfDelete">确认注销账号</button>
            </div>
          </div>
        </main>
      </div>
    </template>

    <div v-else class="empty-state">
      <p v-if="infoErr" class="err">{{ infoErr }}</p>
      <p v-else>暂无数据</p>
      <router-link to="/login" class="empty-link">去登录</router-link>
    </div>
  </KnowledgeModuleShell>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import {
  getUserInfo,
  updateUserInfo,
  uploadAvatar,
  deleteAccount,
  getBlockedKnowledgeCategories,
  putBlockedKnowledgeCategories,
  getUnreadNotificationCount,
} from '@/api/user'
import { getCategoryList, getCollectList, getMyDrafts } from '@/api/content'
import { getMyPostList } from '@/api/forum'
import KnowledgeModuleShell from '@/components/KnowledgeModuleShell.vue'
import ToggleSwitch from '@/components/ToggleSwitch.vue'
import { SITE_NOTIFICATIONS_EVENT } from '@/constants/siteNotifications'

const LS_SHOW_PHONE = 'client_profile_show_full_phone'
const LS_SHOW_EMAIL = 'client_profile_show_email_sidebar'

const crumbs = [
  { label: '首页', to: '/home' },
  { label: '个人中心' },
]

const navItems = [
  { key: 'overview', label: '总览' },
  { key: 'profile', label: '资料与头像' },
  { key: 'recommend', label: '推荐偏好' },
  { key: 'security', label: '账号与安全' },
]

const ROLE_LABELS = {
  READER: '读者',
  AUTHOR: '作者',
  ADMIN: '管理员',
}

const userStore = useUserStore()
const router = useRouter()
const activeSection = ref('overview')
const deletePassword = ref('')
const deleting = ref(false)
const fileInput = ref(null)
const info = ref(null)
const loading = ref(true)
const saving = ref(false)
const uploadIng = ref(false)
const infoErr = ref('')
const form = reactive({ avatar: '', phone: '', email: '' })

const showFullPhone = ref(false)
const showEmailInSidebar = ref(false)

const categories = ref([])
const catLoading = ref(true)
const blockedCategoryIds = ref([])
const blockedSaving = ref(false)
const blockedErr = ref('')

const statsLoading = ref(true)
const unreadCount = ref(0)
const statKnowledgeCollect = ref(0)
const statForumCollect = ref(0)
const statEquipmentCollect = ref(0)
const statMyPosts = ref(0)
const statDrafts = ref(0)

const isAuthor = computed(() => {
  const r = info.value?.role
  return r === 'AUTHOR' || r === 'ADMIN'
})

const roleLabel = computed(() => ROLE_LABELS[info.value?.role] || '用户')

const usernameInitial = computed(() => {
  const u = info.value?.username
  if (!u) return '?'
  return u.slice(0, 1).toUpperCase()
})

function maskPhone(p) {
  if (!p || String(p).length < 7) return p || ''
  const s = String(p)
  return `${s.slice(0, 3)}****${s.slice(-4)}`
}

const displayPhone = computed(() => {
  const p = form.phone
  if (!p) return ''
  return showFullPhone.value ? p : maskPhone(p)
})

const statCards = computed(() => {
  const n = (v) => (statsLoading.value ? '…' : String(v))
  const cards = [
    { key: 'unread', label: '未读消息', display: n(unreadCount.value), to: '/notifications' },
    { key: 'kcol', label: '知识收藏', display: n(statKnowledgeCollect.value), to: '/knowledge/collect' },
    { key: 'fcol', label: '论坛收藏', display: n(statForumCollect.value), to: '/knowledge/collect' },
    { key: 'ecol', label: '器材收藏', display: n(statEquipmentCollect.value), to: '/knowledge/collect?module=equipment' },
    { key: 'posts', label: '我的帖子', display: n(statMyPosts.value), to: '/forum/mine' },
  ]
  if (isAuthor.value) {
    cards.push({
      key: 'drafts',
      label: '我的知识',
      display: statsLoading.value ? '…' : String(statDrafts.value),
      to: '/knowledge/drafts',
    })
  }
  return cards
})

const quickLinks = computed(() => {
  const links = [
    { to: '/notifications', label: '消息通知', sub: '站内信与提醒' },
    { to: '/profile/password', label: '修改密码', sub: '保护账号安全' },
    { to: '/knowledge/collect', label: '我的收藏', sub: '知识 · 论坛 · 新闻 · 器材' },
    { to: '/forum/mine', label: '我的帖子', sub: '论坛发布记录' },
  ]
  if (isAuthor.value) {
    links.push({ to: '/knowledge/drafts', label: '我的知识', sub: '草稿与已发布' })
  } else if (info.value?.role !== 'ADMIN') {
    links.push({ to: '/apply-author', label: '作者认证', sub: '申请成为作者' })
  }
  return links
})

watch(showFullPhone, (v) => localStorage.setItem(LS_SHOW_PHONE, v ? '1' : '0'))
watch(showEmailInSidebar, (v) => localStorage.setItem(LS_SHOW_EMAIL, v ? '1' : '0'))

async function refreshUnreadBadgeFromEvent() {
  try {
    const unread = await getUnreadNotificationCount()
    unreadCount.value = Number(unread?.count ?? 0)
  } catch {
    unreadCount.value = 0
  }
}

async function loadStats() {
  if (!info.value) return
  statsLoading.value = true
  try {
    const [unread, kn, fo, eq, po] = await Promise.all([
      getUnreadNotificationCount(),
      getCollectList({ pageNum: 1, pageSize: 1, module: 'knowledge' }),
      getCollectList({ pageNum: 1, pageSize: 1, module: 'forum' }),
      getCollectList({ pageNum: 1, pageSize: 1, module: 'equipment' }),
      getMyPostList({ pageNum: 1, pageSize: 1 }),
    ])
    unreadCount.value = Number(unread?.count ?? 0)
    statKnowledgeCollect.value = Number(kn?.total ?? 0)
    statForumCollect.value = Number(fo?.total ?? 0)
    statEquipmentCollect.value = Number(eq?.total ?? 0)
    statMyPosts.value = Number(po?.total ?? po?.totalCount ?? 0)
    if (isAuthor.value) {
      const dr = await getMyDrafts({ pageNum: 1, pageSize: 1 })
      statDrafts.value = Number(dr?.total ?? dr?.totalCount ?? 0)
    } else {
      statDrafts.value = 0
    }
  } catch {
    unreadCount.value = 0
    statKnowledgeCollect.value = 0
    statForumCollect.value = 0
    statEquipmentCollect.value = 0
    statMyPosts.value = 0
    statDrafts.value = 0
  } finally {
    statsLoading.value = false
  }
}

async function onFileChange(e) {
  const file = e.target.files?.[0]
  if (!file) return
  uploadIng.value = true
  infoErr.value = ''
  try {
    const data = await uploadAvatar(file)
    const url = data?.url || data
    if (url) {
      form.avatar = url
    }
  } catch (err) {
    infoErr.value = err.message || '上传失败'
  } finally {
    uploadIng.value = false
    e.target.value = ''
  }
}

async function loadBlockedCategories() {
  catLoading.value = true
  blockedErr.value = ''
  try {
    const [cats, blockedRes] = await Promise.all([getCategoryList(), getBlockedKnowledgeCategories()])
    const list = Array.isArray(cats) ? cats : cats?.list ?? []
    categories.value = list
    const ids = blockedRes?.categoryIds ?? []
    blockedCategoryIds.value = Array.isArray(ids) ? ids.map((x) => Number(x)).filter((n) => Number.isFinite(n)) : []
  } catch (e) {
    blockedErr.value = e.message || '加载屏蔽分类失败'
    categories.value = []
    blockedCategoryIds.value = []
  } finally {
    catLoading.value = false
  }
}

async function saveBlocked() {
  blockedErr.value = ''
  blockedSaving.value = true
  try {
    await putBlockedKnowledgeCategories(blockedCategoryIds.value)
  } catch (e) {
    blockedErr.value = e.message || '保存失败'
  } finally {
    blockedSaving.value = false
  }
}

async function load() {
  loading.value = true
  infoErr.value = ''
  try {
    const data = await getUserInfo()
    info.value = data || null
    if (data) {
      form.avatar = data.avatar ?? ''
      form.phone = data.phone ?? ''
      form.email = data.email ?? ''
      if (data.role != null) userStore.updateUser({ role: data.role })
      await loadBlockedCategories()
      await loadStats()
    }
  } catch (e) {
    infoErr.value = e.message || '加载失败，请重新登录'
  } finally {
    loading.value = false
  }
}

async function saveInfo() {
  infoErr.value = ''
  saving.value = true
  try {
    await updateUserInfo({
      avatar: form.avatar || undefined,
      phone: form.phone,
      email: form.email || undefined,
    })
    if (form.avatar) userStore.updateUser({ avatar: form.avatar })
    load()
  } catch (e) {
    infoErr.value = e.message || '保存失败'
  } finally {
    saving.value = false
  }
}

async function onSelfDelete() {
  if (!deletePassword.value) {
    alert('请输入密码')
    return
  }
  if (!confirm('确定删除当前账号？此操作不可恢复。')) return
  deleting.value = true
  infoErr.value = ''
  try {
    await deleteAccount(deletePassword.value)
    alert('账号已注销')
    userStore.logout()
    router.push('/home')
  } catch (e) {
    infoErr.value = e.message || '注销失败'
  } finally {
    deleting.value = false
  }
}

onMounted(() => {
  showFullPhone.value = localStorage.getItem(LS_SHOW_PHONE) === '1'
  showEmailInSidebar.value = localStorage.getItem(LS_SHOW_EMAIL) === '1'
  load()
  window.addEventListener(SITE_NOTIFICATIONS_EVENT, refreshUnreadBadgeFromEvent)
})

onUnmounted(() => {
  window.removeEventListener(SITE_NOTIFICATIONS_EVENT, refreshUnreadBadgeFromEvent)
})
</script>

<style scoped>
.page-head {
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(59, 130, 246, 0.12);
}

.page-title {
  margin: 0 0 6px;
  font-size: 1.35rem;
  font-weight: 700;
  color: var(--client-text);
  letter-spacing: -0.02em;
}

.page-sub {
  margin: 0;
  font-size: 0.875rem;
  color: var(--client-muted);
  line-height: 1.5;
}

.state-loading {
  padding: 48px 16px;
  text-align: center;
  color: var(--client-muted);
  font-size: 0.9375rem;
}

.profile-grid {
  display: grid;
  grid-template-columns: minmax(240px, 280px) minmax(0, 1fr);
  gap: clamp(20px, 3vw, 32px);
  align-items: start;
}

.profile-aside {
  position: sticky;
  top: 16px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.user-card {
  padding: 20px 18px;
  border-radius: calc(var(--client-radius) - 2px);
  background: linear-gradient(165deg, var(--client-accent-soft) 0%, var(--client-surface) 55%);
  border: 1px solid rgba(59, 130, 246, 0.14);
  text-align: center;
}

.avatar-ring {
  display: flex;
  justify-content: center;
  margin-bottom: 12px;
}

.avatar-wrap {
  width: 88px;
  height: 88px;
  border-radius: 50%;
  overflow: hidden;
  background: rgba(59, 130, 246, 0.12);
  border: 3px solid var(--client-surface);
  box-shadow: 0 4px 16px rgba(59, 130, 246, 0.15);
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-wrap img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-placeholder {
  font-size: 2rem;
  font-weight: 700;
  color: var(--client-primary);
}

.user-name {
  margin: 0 0 8px;
  font-size: 1.125rem;
  font-weight: 700;
  color: var(--client-text);
  word-break: break-all;
}

.role-pill {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--client-primary-hover);
  background: rgba(59, 130, 246, 0.12);
  border: 1px solid rgba(59, 130, 246, 0.2);
}

.user-meta {
  margin-top: 14px;
  text-align: left;
  font-size: 0.8125rem;
  color: var(--client-muted);
  line-height: 1.45;
}

.meta-line {
  margin: 0 0 6px;
  word-break: break-all;
}

.meta-email {
  font-size: 0.75rem;
}

.side-nav {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.side-nav-btn {
  width: 100%;
  text-align: left;
  padding: 10px 14px;
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--client-text);
  background: rgba(59, 130, 246, 0.06);
  border: 1px solid transparent;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.2s, border-color 0.2s, color 0.2s;
}

.side-nav-btn:hover {
  background: rgba(59, 130, 246, 0.1);
  border-color: rgba(59, 130, 246, 0.12);
}

.side-nav-btn.active {
  color: var(--client-primary-hover);
  background: var(--client-accent-soft);
  border-color: rgba(59, 130, 246, 0.22);
}

.profile-main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 12px;
}

.stat-card {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 14px 16px;
  border-radius: calc(var(--client-radius) - 2px);
  background: var(--client-surface);
  border: 1px solid rgba(59, 130, 246, 0.12);
  text-decoration: none;
  color: inherit;
  transition: border-color 0.2s, box-shadow 0.2s, transform 0.15s;
  box-shadow: 0 1px 0 rgba(59, 130, 246, 0.04);
}

.stat-card:hover {
  border-color: rgba(59, 130, 246, 0.35);
  box-shadow: 0 6px 20px rgba(59, 130, 246, 0.08);
  transform: translateY(-1px);
}

.stat-value {
  font-size: 1.375rem;
  font-weight: 700;
  color: var(--client-primary);
  font-variant-numeric: tabular-nums;
}

.stat-label {
  font-size: 0.75rem;
  color: var(--client-muted);
  font-weight: 500;
}

.section-stack {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.sub-card {
  padding: 20px 20px 22px;
  border-radius: calc(var(--client-radius) - 2px);
  border: 1px solid rgba(59, 130, 246, 0.1);
  background: rgba(255, 255, 255, 0.65);
}

.sub-card-title {
  margin: 0 0 8px;
  font-size: 1rem;
  font-weight: 700;
  color: var(--client-text);
}

.sub-card-desc {
  margin: 0 0 16px;
  font-size: 0.8125rem;
  color: var(--client-muted);
  line-height: 1.5;
}

.field-hint {
  font-size: 0.8125rem;
  color: var(--client-muted);
  line-height: 1.55;
  margin: 0 0 12px;
}

.pref-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 10px 0;
  border-bottom: 1px solid rgba(59, 130, 246, 0.08);
}

.pref-row:last-of-type {
  border-bottom: none;
  padding-bottom: 0;
}

.pref-label {
  font-size: 0.875rem;
  color: var(--client-text);
}

.pref-hint {
  margin: 12px 0 0;
  font-size: 0.75rem;
  color: var(--client-muted);
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 12px;
}

.quick-tile {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 14px 14px;
  border-radius: 10px;
  text-decoration: none;
  color: inherit;
  border: 1px solid rgba(59, 130, 246, 0.12);
  background: var(--client-surface);
  transition: border-color 0.2s, background 0.2s;
}

.quick-tile:hover {
  border-color: var(--client-primary);
  background: var(--client-accent-soft);
}

.quick-title {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--client-text);
}

.quick-sub {
  font-size: 0.75rem;
  color: var(--client-muted);
  line-height: 1.35;
}

.profile-form .field {
  margin-bottom: 16px;
}

.profile-form label {
  display: block;
  margin-bottom: 6px;
  font-size: 0.8125rem;
  font-weight: 500;
  color: var(--client-text);
}

.input {
  width: 100%;
  box-sizing: border-box;
  padding: 10px 12px;
  border: 1px solid rgba(59, 130, 246, 0.2);
  border-radius: 10px;
  font-size: 0.875rem;
  color: var(--client-text);
  background: var(--client-surface);
  transition: border-color 0.2s, box-shadow 0.2s;
}

.input:focus {
  outline: none;
  border-color: var(--client-primary);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.15);
}

.input.disabled {
  background: rgba(241, 245, 249, 0.9);
  color: var(--client-muted);
}

.hint {
  font-size: 0.75rem;
  color: var(--client-muted);
  margin-top: 6px;
  display: block;
}

.avatar-row {
  display: flex;
  align-items: center;
  gap: 16px;
}

.avatar-preview.sm {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  overflow: hidden;
  background: rgba(59, 130, 246, 0.08);
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-preview.sm img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.no-avatar {
  font-size: 0.75rem;
  color: var(--client-muted);
}

.avatar-upload {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.file-input {
  display: none;
}

.btn-primary {
  padding: 10px 20px;
  font-size: 0.875rem;
  font-weight: 600;
  color: #fff;
  background: var(--client-primary);
  border: none;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-primary:hover:not(:disabled) {
  background: var(--client-primary-hover);
}

.btn-primary:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.link-as-btn {
  display: inline-block;
  text-decoration: none;
  text-align: center;
}

.btn-outline {
  padding: 8px 16px;
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--client-primary);
  background: var(--client-surface);
  border: 1px solid rgba(59, 130, 246, 0.35);
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.2s, border-color 0.2s;
}

.btn-outline:hover:not(:disabled) {
  background: var(--client-accent-soft);
  border-color: var(--client-primary);
}

.btn-outline:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.cat-checks {
  list-style: none;
  padding: 10px 12px;
  margin: 0 0 14px;
  max-height: 240px;
  overflow-y: auto;
  border: 1px solid rgba(59, 130, 246, 0.12);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.8);
}

.cat-checks li {
  margin-bottom: 8px;
}

.cat-label {
  font-size: 0.875rem;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--client-text);
}

.err {
  color: #dc2626;
  font-size: 0.8125rem;
  margin: 0 0 10px;
}

.danger-zone {
  border-color: rgba(220, 38, 38, 0.25) !important;
  background: rgba(254, 242, 242, 0.35) !important;
}

.danger-title {
  color: #b91c1c;
}

.danger-tip {
  font-size: 0.8125rem;
  color: var(--client-muted);
  margin: 0 0 14px;
  line-height: 1.5;
}

.btn-danger {
  margin-top: 8px;
  padding: 10px 18px;
  font-size: 0.875rem;
  font-weight: 600;
  color: #fff;
  background: #ef4444;
  border: none;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-danger:hover:not(:disabled) {
  background: #dc2626;
}

.btn-danger:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.empty-state {
  text-align: center;
  padding: 40px 16px;
  color: var(--client-muted);
}

.empty-link {
  display: inline-block;
  margin-top: 12px;
  color: var(--client-primary);
  text-decoration: none;
  font-weight: 500;
}

.empty-link:hover {
  text-decoration: underline;
  color: var(--client-primary-hover);
}

@media (max-width: 900px) {
  .profile-grid {
    grid-template-columns: 1fr;
  }

  .profile-aside {
    position: static;
  }
}
</style>
