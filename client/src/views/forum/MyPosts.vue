<template>
  <KnowledgeModuleShell :crumbs="crumbs">
    <div class="mine-page">
      <header class="mine-hero">
        <div class="mine-hero-text">
          <h1 class="mine-title">我的帖子</h1>
          <p class="mine-sub">查看审核状态、修改被驳回的帖子</p>
        </div>
        <div class="mine-toolbar">
          <div class="search-wrap">
            <input
              v-model.trim="keyword"
              type="search"
              class="search-input"
              placeholder="搜索标题或正文"
              @keyup.enter="search"
            />
            <button type="button" class="search-btn" aria-label="搜索" @click="search">
              <svg class="search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                <circle cx="11" cy="11" r="8" />
                <path d="m21 21-4.35-4.35" />
              </svg>
            </button>
          </div>
          <div class="mine-actions">
            <router-link to="/forum/publish" class="btn-primary">发帖</router-link>
            <router-link to="/forum" class="btn-ghost">论坛首页</router-link>
          </div>
        </div>
      </header>

      <p class="tip-banner">新帖与修改后的帖子均需管理员审核通过后，才会出现在论坛公开列表。</p>

      <div v-if="loading" class="state state--muted">加载中…</div>
      <div v-else-if="errorMsg" class="state state--error">{{ errorMsg }}</div>
      <template v-else>
        <ul v-if="list.length" class="post-list">
          <li v-for="row in list" :key="row.id">
            <router-link :to="`/forum/${row.id}`" class="post-card">
              <div class="post-card-head">
                <div class="avatar-wrap">
                  <img v-if="rowAvatar(row)" :src="rowAvatar(row)" alt="" class="avatar-img" />
                  <span v-else class="avatar-fallback">{{ rowInitial(row) }}</span>
                </div>
                <div class="post-card-author">
                  <span class="badge" :class="badgeClass(row.status)">{{ statusText(row.status) }}</span>
                  <div class="post-card-meta">
                    <span class="meta-id">ID {{ row.userId ?? '—' }}</span>
                    <span v-if="row.createTime" class="meta-time">{{ formatDateTime(row.createTime) }}</span>
                  </div>
                </div>
              </div>
              <h2 class="post-card-title">{{ row.title }}</h2>
              <p v-if="Number(row.status) === -1 && row.rejectReason" class="reject-line">驳回原因：{{ row.rejectReason }}</p>
            </router-link>
          </li>
        </ul>
        <div v-else class="state state--empty">暂无帖子，去论坛发一条吧。</div>
        <PaginationBar v-model="pageNum" class="pager-bar" :total="total" :page-size="pageSize" @current-change="load" />
      </template>
    </div>
  </KnowledgeModuleShell>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getMyPostList } from '@/api/forum'
import { resolveMediaUrl } from '@/utils/resolveMediaUrl'
import { formatDateTime } from '@/utils/formatDateTime'
import PaginationBar from '@/components/PaginationBar.vue'
import KnowledgeModuleShell from '@/components/KnowledgeModuleShell.vue'

const crumbs = [
  { label: '首页', to: '/home' },
  { label: '论坛', to: '/forum' },
  { label: '我的帖子' },
]

const list = ref([])
const pageNum = ref(1)
const pageSize = 10
const total = ref(0)
const keyword = ref('')
const loading = ref(true)
const errorMsg = ref('')

function rowAvatar(row) {
  const a = row?.avatar
  return a ? resolveMediaUrl(a) : ''
}

function rowInitial(row) {
  const n = row?.userName || '?'
  return String(n).charAt(0).toUpperCase()
}

function statusText(s) {
  const n = Number(s)
  if (n === 0) return '待审核'
  if (n === 1) return '已通过'
  if (n === -1) return '已驳回'
  return '—'
}

function badgeClass(s) {
  const n = Number(s)
  if (n === 0) return 'pending'
  if (n === 1) return 'ok'
  if (n === -1) return 'reject'
  return ''
}

async function load() {
  loading.value = true
  errorMsg.value = ''
  try {
    const data = await getMyPostList({
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
.mine-page {
  min-width: 0;
}

.mine-hero {
  margin-bottom: 14px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(59, 130, 246, 0.12);
}

.mine-title {
  margin: 0 0 6px;
  font-size: 1.5rem;
  font-weight: 800;
  color: var(--client-text);
  letter-spacing: -0.02em;
}

.mine-sub {
  margin: 0 0 16px;
  font-size: 0.875rem;
  color: var(--client-muted);
}

.mine-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.search-wrap {
  display: flex;
  align-items: stretch;
  flex: 1;
  min-width: min(100%, 220px);
  max-width: 400px;
  border: 1.5px solid rgba(59, 130, 246, 0.2);
  border-radius: 12px;
  overflow: hidden;
  background: var(--client-surface);
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
}

.search-btn:hover {
  background: var(--client-accent-soft);
}

.search-icon {
  width: 18px;
  height: 18px;
}

.mine-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.btn-primary {
  padding: 9px 16px;
  border-radius: 10px;
  font-size: 0.875rem;
  font-weight: 600;
  text-decoration: none;
  color: #fff;
  background: linear-gradient(135deg, var(--client-primary), var(--client-primary-hover));
  box-shadow: 0 4px 14px rgba(59, 130, 246, 0.3);
  transition: transform 0.2s, box-shadow 0.2s;
}

.btn-primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 8px 20px rgba(59, 130, 246, 0.38);
}

.btn-ghost {
  padding: 9px 16px;
  border-radius: 10px;
  font-size: 0.875rem;
  font-weight: 600;
  text-decoration: none;
  color: var(--client-text);
  border: 1px solid rgba(59, 130, 246, 0.2);
  background: rgba(59, 130, 246, 0.06);
  transition: border-color 0.2s, background 0.2s;
}

.btn-ghost:hover {
  border-color: var(--client-primary);
  background: var(--client-accent-soft);
  color: var(--client-primary-hover);
}

.tip-banner {
  font-size: 0.8125rem;
  color: var(--client-muted);
  margin: 0 0 18px;
  line-height: 1.55;
  padding: 10px 12px;
  border-radius: 8px;
  background: rgba(59, 130, 246, 0.06);
  border: 1px solid rgba(59, 130, 246, 0.1);
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
  padding: 40px 16px;
}

.state--error {
  color: #b91c1c;
  text-align: left;
  max-width: 480px;
  margin: 0 auto;
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
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.badge {
  display: inline-block;
  width: fit-content;
  padding: 3px 10px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 700;
}

.badge.pending {
  background: rgba(254, 243, 199, 0.65);
  color: #b45309;
}

.badge.ok {
  background: rgba(187, 247, 208, 0.55);
  color: #15803d;
}

.badge.reject {
  background: rgba(254, 226, 226, 0.7);
  color: #b91c1c;
}

.post-card-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 0.75rem;
  color: var(--client-muted);
}

.post-card-title {
  margin: 0;
  font-size: 1.0625rem;
  font-weight: 700;
  color: var(--client-text);
  line-height: 1.35;
  transition: color 0.2s;
}

.post-card:hover .post-card-title {
  color: var(--client-primary);
}

.reject-line {
  margin: 10px 0 0;
  font-size: 0.75rem;
  color: #b91c1c;
  line-height: 1.45;
}

.pager-bar {
  margin-top: 22px;
}

@media (max-width: 640px) {
  .mine-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .search-wrap {
    max-width: none;
  }

  .mine-actions {
    justify-content: stretch;
  }

  .btn-primary,
  .btn-ghost {
    flex: 1;
    text-align: center;
  }
}
</style>
