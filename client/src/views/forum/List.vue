<template>
  <KnowledgeModuleShell :crumbs="crumbs">
    <div class="forum-page">
      <div class="forum-layout">
        <aside class="forum-aside forum-aside--left" aria-label="论坛侧栏">
          <div class="side-card">
            <h3 class="side-title">交流提示</h3>
            <ul class="side-list">
              <li>发帖请文明用语，遵守法律法规。</li>
              <li>新帖需审核通过后才会出现在本列表。</li>
              <li>可使用搜索查找历史讨论。</li>
            </ul>
          </div>
          <nav class="side-card side-nav" aria-label="论坛快捷入口">
            <h3 class="side-title">快捷入口</h3>
            <router-link to="/home" class="side-link">返回首页</router-link>
            <router-link v-if="userStore.isLoggedIn" to="/forum/mine" class="side-link">我的帖子</router-link>
            <router-link v-else to="/login" class="side-link">登录后管理帖子</router-link>
            <router-link v-if="userStore.isLoggedIn" to="/forum/publish" class="side-link side-link--accent">发布新帖</router-link>
            <router-link v-else to="/login" class="side-link side-link--accent">登录后发帖</router-link>
          </nav>
        </aside>

        <div class="forum-main">
          <header class="forum-hero">
            <div class="forum-hero-text">
              <h1 class="forum-hero-title">消防论坛</h1>
              <p class="forum-hero-sub">经验交流 · 疑问互助 · 案例讨论</p>
            </div>
          </header>

          <div class="forum-toolbar">
            <div class="search-wrap">
              <input
                v-model.trim="keyword"
                type="search"
                class="search-input"
                placeholder="搜索标题或正文"
                aria-label="搜索帖子"
                @keyup.enter="search"
              />
              <button type="button" class="search-btn" aria-label="搜索" @click="search">
                <svg class="search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                  <circle cx="11" cy="11" r="8" />
                  <path d="m21 21-4.35-4.35" />
                </svg>
              </button>
            </div>

            <div class="toolbar-actions">
              <div class="sort-group" role="group" aria-label="列表排序">
                <button
                  type="button"
                  class="sort-btn"
                  :class="{ active: sortMode === 'latest' }"
                  title="按发帖时间（与接口一致）"
                  @click="sortMode = 'latest'"
                >
                  最新
                </button>
                <button
                  type="button"
                  class="sort-btn"
                  :class="{ active: sortMode === 'hotPage' }"
                  title="仅在当前页内按点赞、浏览排序"
                  @click="sortMode = 'hotPage'"
                >
                  本页热帖
                </button>
              </div>
              <router-link v-if="userStore.isLoggedIn" to="/forum/publish" class="btn-publish">
                <svg class="btn-publish-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                  <line x1="12" y1="5" x2="12" y2="19" />
                  <line x1="5" y1="12" x2="19" y2="12" />
                </svg>
                发帖
              </router-link>
              <router-link v-else to="/login" class="btn-publish">登录发帖</router-link>
            </div>
          </div>

          <div v-if="loading" class="state state--muted">加载中…</div>
          <div v-else-if="errorMsg" class="state state--error">
            <p class="error-title">无法加载帖子列表</p>
            <p class="error-detail">{{ errorMsg }}</p>
            <p class="error-hint">
              请确认后端已启动（默认 <code>http://localhost:8088/api</code>），并检查本页网络请求是否成功。
            </p>
            <button type="button" class="btn-retry" @click="load">重试</button>
          </div>
          <template v-else>
            <ul v-if="displayedRows.length" class="post-list">
              <li v-for="row in displayedRows" :key="row.id">
                <router-link :to="`/forum/${row.id}`" class="post-card">
                  <div class="post-card-head">
                    <div class="avatar-wrap">
                      <img v-if="rowAvatar(row)" :src="rowAvatar(row)" alt="" class="avatar-img" />
                      <span v-else class="avatar-fallback">{{ rowInitial(row) }}</span>
                    </div>
                    <div class="post-card-author">
                      <span class="author-name">{{ row.userName || '用户' }}</span>
                      <div class="post-card-meta">
                        <span class="meta-id">ID {{ row.userId ?? '—' }}</span>
                        <span v-if="row.createTime" class="meta-time">{{ formatDateTime(row.createTime) }}</span>
                      </div>
                    </div>
                  </div>
                  <h2 class="post-card-title">{{ row.title }}</h2>
                  <p class="post-card-excerpt">{{ postExcerpt(row) }}</p>
                  <div class="post-card-foot">
                    <span class="stat">
                      <svg class="stat-icon" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
                        <path
                          d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"
                        />
                      </svg>
                      {{ row.likeCount ?? 0 }}
                    </span>
                    <span class="stat">
                      <svg class="stat-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                        <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
                      </svg>
                      {{ row.commentCount ?? 0 }}
                    </span>
                    <span class="stat">
                      <svg class="stat-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                        <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                        <circle cx="12" cy="12" r="3" />
                      </svg>
                      {{ row.viewCount ?? 0 }}
                    </span>
                  </div>
                </router-link>
              </li>
            </ul>
            <div v-else class="state state--empty">暂无已通过审核的帖子，欢迎发帖参与讨论。</div>
            <PaginationBar v-model="pageNum" class="pager-bar" :total="total" :page-size="pageSize" @current-change="load" />
          </template>
        </div>

        <aside class="forum-aside forum-aside--right" aria-label="本页热议">
          <div class="side-card">
            <h3 class="side-title">本页热议</h3>
            <p class="side-note">基于当前页帖子按互动估算，翻页后会变化。</p>
            <ol v-if="sidebarHot.length" class="hot-list">
              <li v-for="(row, index) in sidebarHot" :key="row.id" class="hot-item">
                <span class="hot-rank" :class="{ 'hot-rank--top': index < 3 }">{{ index + 1 }}</span>
                <router-link :to="`/forum/${row.id}`" class="hot-link">{{ row.title }}</router-link>
                <span class="hot-meta">{{ row.likeCount ?? 0 }} 赞</span>
              </li>
            </ol>
            <p v-else class="side-empty">暂无数据</p>
          </div>
        </aside>
      </div>
    </div>
  </KnowledgeModuleShell>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getPostList } from '@/api/forum'
import { useUserStore } from '@/stores/user'
import { resolveMediaUrl } from '@/utils/resolveMediaUrl'
import { formatDateTime } from '@/utils/formatDateTime'
import PaginationBar from '@/components/PaginationBar.vue'
import KnowledgeModuleShell from '@/components/KnowledgeModuleShell.vue'

const crumbs = [
  { label: '首页', to: '/home' },
  { label: '论坛' },
]

function rowAvatar(row) {
  const a = row?.avatar
  return a ? resolveMediaUrl(a) : ''
}

function rowInitial(row) {
  const n = row?.userName || '?'
  return String(n).charAt(0).toUpperCase()
}

function postExcerpt(row) {
  const raw = row?.content || ''
  const t = String(raw)
    .replace(/<[^>]+>/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
  if (!t) return '（暂无摘要）'
  return t.length > 160 ? `${t.slice(0, 160)}…` : t
}

const userStore = useUserStore()
const list = ref([])
const pageNum = ref(1)
const pageSize = 10
const total = ref(0)
const keyword = ref('')
const loading = ref(true)
const errorMsg = ref('')
const sortMode = ref('latest')

const displayedRows = computed(() => {
  const rows = [...list.value]
  if (sortMode.value === 'hotPage') {
    rows.sort((a, b) => {
      const d = Number(b.likeCount || 0) - Number(a.likeCount || 0)
      if (d !== 0) return d
      return Number(b.viewCount || 0) - Number(a.viewCount || 0)
    })
  }
  return rows
})

const sidebarHot = computed(() => {
  const rows = [...displayedRows.value]
  rows.sort((a, b) => {
    const sa = Number(a.likeCount || 0) * 2 + Number(a.viewCount || 0)
    const sb = Number(b.likeCount || 0) * 2 + Number(b.viewCount || 0)
    return sb - sa
  })
  return rows.slice(0, 5)
})

async function load() {
  loading.value = true
  errorMsg.value = ''
  try {
    const data = await getPostList({
      pageNum: pageNum.value,
      pageSize,
      keyword: keyword.value || undefined,
    })
    const records = data?.records ?? data?.list ?? []
    const totalVal = Number(data?.total ?? data?.totalCount ?? 0)
    list.value = records
    total.value = totalVal
    if (records.length === 0 && totalVal > 0 && pageNum.value > 1 && (pageNum.value - 1) * pageSize >= totalVal) {
      pageNum.value -= 1
      return load()
    }
  } catch (e) {
    const msg = typeof e === 'string' ? e : e?.message || '请求失败'
    errorMsg.value = msg
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function search() {
  pageNum.value = 1
  load()
}

onMounted(load)
</script>

<style scoped>
.forum-page {
  min-width: 0;
}

.forum-layout {
  display: grid;
  grid-template-columns: minmax(200px, 240px) minmax(0, 1fr) minmax(200px, 260px);
  gap: clamp(16px, 2vw, 24px);
  align-items: start;
}

.forum-aside {
  position: sticky;
  top: 12px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.side-card {
  padding: 16px 14px;
  border-radius: calc(var(--client-radius) - 2px);
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(59, 130, 246, 0.12);
  box-shadow: 0 1px 0 rgba(59, 130, 246, 0.04);
}

.side-title {
  margin: 0 0 10px;
  font-size: 0.9375rem;
  font-weight: 700;
  color: var(--client-text);
}

.side-list {
  margin: 0;
  padding-left: 1.1rem;
  font-size: 0.8125rem;
  color: var(--client-muted);
  line-height: 1.55;
}

.side-list li {
  margin-bottom: 6px;
}

.side-nav {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.side-link {
  display: block;
  padding: 8px 10px;
  border-radius: 8px;
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--client-text);
  text-decoration: none;
  transition: background 0.2s, color 0.2s;
}

.side-link:hover {
  background: var(--client-accent-soft);
  color: var(--client-primary-hover);
}

.side-link--accent {
  margin-top: 4px;
  text-align: center;
  background: linear-gradient(135deg, var(--client-primary), var(--client-primary-hover));
  color: #fff !important;
  font-weight: 600;
}

.side-link--accent:hover {
  filter: brightness(1.05);
  box-shadow: 0 6px 18px rgba(59, 130, 246, 0.35);
}

.side-note {
  margin: 0 0 12px;
  font-size: 0.75rem;
  color: var(--client-muted);
  line-height: 1.45;
}

.hot-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.hot-item {
  display: grid;
  grid-template-columns: 28px 1fr;
  grid-template-rows: auto auto;
  gap: 2px 8px;
  padding: 10px 0;
  border-bottom: 1px solid rgba(59, 130, 246, 0.08);
}

.hot-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.hot-rank {
  grid-row: span 2;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border-radius: 8px;
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--client-muted);
  background: rgba(59, 130, 246, 0.08);
}

.hot-rank--top {
  color: #fff;
  background: linear-gradient(135deg, var(--client-primary), var(--client-accent));
}

.hot-link {
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--client-text);
  text-decoration: none;
  line-height: 1.35;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.hot-link:hover {
  color: var(--client-primary);
}

.hot-meta {
  font-size: 0.6875rem;
  color: var(--client-muted);
}

.side-empty {
  margin: 0;
  font-size: 0.8125rem;
  color: var(--client-muted);
}

.forum-main {
  min-width: 0;
}

.forum-hero {
  margin-bottom: 18px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(59, 130, 246, 0.12);
}

.forum-hero-title {
  margin: 0 0 6px;
  font-size: 1.5rem;
  font-weight: 800;
  color: var(--client-text);
  letter-spacing: -0.02em;
}

.forum-hero-sub {
  margin: 0;
  font-size: 0.875rem;
  color: var(--client-muted);
}

.forum-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 18px;
}

.search-wrap {
  display: flex;
  align-items: stretch;
  flex: 1;
  min-width: min(100%, 240px);
  max-width: 400px;
  border: 1.5px solid rgba(59, 130, 246, 0.2);
  border-radius: 12px;
  overflow: hidden;
  background: var(--client-surface);
  transition: border-color 0.2s, box-shadow 0.2s;
}

.search-wrap:focus-within {
  border-color: var(--client-primary);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.15);
}

.search-input {
  flex: 1;
  min-width: 0;
  border: none;
  padding: 10px 12px;
  font-size: 0.875rem;
  color: var(--client-text);
  background: transparent;
  font-family: var(--client-font);
}

.search-input:focus {
  outline: none;
}

.search-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  border: none;
  background: rgba(59, 130, 246, 0.08);
  color: var(--client-primary);
  cursor: pointer;
  transition: background 0.2s, color 0.2s;
}

.search-btn:hover {
  background: var(--client-accent-soft);
  color: var(--client-primary-hover);
}

.search-icon {
  width: 18px;
  height: 18px;
}

.toolbar-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.sort-group {
  display: inline-flex;
  padding: 3px;
  border-radius: 10px;
  background: rgba(59, 130, 246, 0.08);
  border: 1px solid rgba(59, 130, 246, 0.12);
}

.sort-btn {
  padding: 7px 12px;
  border: none;
  border-radius: 8px;
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--client-muted);
  background: transparent;
  cursor: pointer;
  transition: background 0.2s, color 0.2s;
  font-family: var(--client-font);
}

.sort-btn:hover {
  color: var(--client-text);
}

.sort-btn.active {
  background: var(--client-surface);
  color: var(--client-primary);
  box-shadow: 0 1px 4px rgba(59, 130, 246, 0.15);
}

.btn-publish {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 9px 16px;
  border-radius: 10px;
  font-size: 0.875rem;
  font-weight: 600;
  text-decoration: none;
  color: #fff;
  background: linear-gradient(135deg, var(--client-primary), var(--client-primary-hover));
  box-shadow: 0 4px 14px rgba(59, 130, 246, 0.35);
  transition: transform 0.2s, box-shadow 0.2s;
}

.btn-publish:hover {
  transform: translateY(-1px) scale(1.02);
  box-shadow: 0 8px 22px rgba(59, 130, 246, 0.42);
}

.btn-publish-icon {
  width: 16px;
  height: 16px;
}

.state {
  padding: 28px 16px;
  text-align: center;
  font-size: 0.9375rem;
}

.state--muted {
  color: var(--client-muted);
}

.state--empty {
  color: var(--client-muted);
  padding: 48px 16px;
}

.state--error {
  text-align: left;
  max-width: 520px;
  margin: 0 auto;
  padding: 18px 16px;
  background: rgba(254, 242, 242, 0.85);
  border: 1px solid rgba(248, 113, 113, 0.45);
  border-radius: var(--client-radius);
  color: #991b1b;
}

.error-title {
  font-weight: 700;
  margin: 0 0 8px;
  font-size: 1rem;
}

.error-detail {
  margin: 0 0 8px;
  word-break: break-all;
}

.error-hint {
  margin: 0 0 14px;
  font-size: 0.8125rem;
  color: #b91c1c;
  line-height: 1.5;
}

.error-hint code {
  background: var(--client-surface);
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 0.75rem;
}

.btn-retry {
  padding: 8px 20px;
  background: var(--client-surface);
  border: 1px solid #dc2626;
  color: #dc2626;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
}

.btn-retry:hover {
  background: rgba(254, 242, 242, 0.9);
}

.post-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.post-card {
  display: block;
  padding: 18px 18px 16px;
  border-radius: calc(var(--client-radius) - 2px);
  text-decoration: none;
  color: inherit;
  background: rgba(255, 255, 255, 0.85);
  border: 1px solid rgba(59, 130, 246, 0.1);
  box-shadow: 0 1px 0 rgba(59, 130, 246, 0.04);
  transition: border-color 0.25s ease, box-shadow 0.25s ease, transform 0.2s ease;
}

.post-card:hover {
  border-color: rgba(59, 130, 246, 0.35);
  box-shadow: 0 12px 32px rgba(59, 130, 246, 0.12);
  transform: translateY(-2px);
}

.post-card-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.avatar-wrap {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
  background: rgba(59, 130, 246, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-fallback {
  font-size: 1rem;
  font-weight: 700;
  color: var(--client-primary);
}

.post-card-author {
  min-width: 0;
  flex: 1;
}

.author-name {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--client-text);
}

.post-card-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 2px;
  font-size: 0.75rem;
  color: var(--client-muted);
}

.meta-id {
  opacity: 0.9;
}

.post-card-title {
  margin: 0 0 8px;
  font-size: 1.0625rem;
  font-weight: 700;
  color: var(--client-text);
  line-height: 1.35;
  transition: color 0.2s;
}

.post-card:hover .post-card-title {
  color: var(--client-primary);
}

.post-card-excerpt {
  margin: 0 0 14px;
  font-size: 0.8125rem;
  color: var(--client-muted);
  line-height: 1.55;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.post-card-foot {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  font-size: 0.75rem;
  color: var(--client-muted);
}

.stat {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.stat-icon {
  width: 14px;
  height: 14px;
  opacity: 0.85;
  color: var(--client-primary);
}

.pager-bar {
  margin-top: 22px;
}

@media (max-width: 1100px) {
  .forum-layout {
    grid-template-columns: 1fr;
  }

  .forum-aside {
    position: static;
  }

  .forum-aside--left {
    order: 2;
  }

  .forum-main {
    order: 1;
  }

  .forum-aside--right {
    order: 3;
  }

  .forum-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .search-wrap {
    max-width: none;
  }

  .toolbar-actions {
    justify-content: space-between;
  }
}
</style>
