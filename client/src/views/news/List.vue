<template>
  <KnowledgeModuleShell :crumbs="crumbs">
    <div class="news-page">
      <header class="news-hero">
        <div class="news-hero-text">
          <h1 class="news-hero-title">消防新闻</h1>
          <p class="news-hero-sub">权威资讯 · 事件通报 · 政策法规与培训动态</p>
        </div>
      </header>

      <div class="news-toolbar">
        <div class="search-row">
          <div class="search-wrap">
            <input
              v-model="query.title"
              type="search"
              class="search-input"
              placeholder="标题关键词"
              @keyup.enter="search"
            />
          </div>
          <div class="search-wrap">
            <input
              v-model="query.keyword"
              type="search"
              class="search-input"
              placeholder="搜标题或摘要"
              @keyup.enter="search"
            />
            <button type="button" class="search-btn" aria-label="搜索" @click="search">
              <svg class="search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                <circle cx="11" cy="11" r="8" />
                <path d="m21 21-4.35-4.35" />
              </svg>
            </button>
          </div>
        </div>

        <div class="filter-row">
          <select v-model="query.categoryId" class="filter-select">
            <option value="">全部分类</option>
            <option v-for="c in categories" :key="c.id" :value="String(c.id)">{{ c.name }}</option>
          </select>

          <div class="region-dropdown">
            <span class="region-label">地区</span>
            <div ref="regionTriggerWrapRef" class="region-trigger-wrap">
              <button type="button" class="region-trigger" @click.stop="toggleRegionPanel">
                <span class="region-trigger-text">{{ regionSummary }}</span>
                <span class="region-caret" aria-hidden="true">{{ regionOpen ? '▲' : '▼' }}</span>
              </button>
            </div>
          </div>

          <select v-model="query.orderBy" class="filter-select">
            <option value="publishTime">按发布时间</option>
            <option value="urgency">按紧急等级</option>
          </select>

          <select v-model="query.order" class="filter-select filter-select--narrow">
            <option value="desc">降序</option>
            <option value="asc">升序</option>
          </select>

          <button type="button" class="btn-query" @click="search">查询</button>
        </div>
      </div>

      <Teleport to="body">
        <div
          v-show="regionOpen"
          ref="regionPanelRef"
          class="region-panel region-panel-fixed"
          :style="regionPanelStyle"
          @click.stop
        >
          <button type="button" class="region-all-btn" @click="pickAllRegions">全部地区（不限）</button>
          <div class="region-check-list">
            <label v-for="r in regionOptions" :key="r" class="region-option">
              <input v-model="selectedRegions" type="checkbox" :value="r" @change="onRegionCheckChange" />
              <span>{{ r }}</span>
            </label>
          </div>
        </div>
      </Teleport>

      <div v-if="loading" class="state-loading">加载中…</div>
      <template v-else>
        <div v-if="list.length" class="news-grid">
          <router-link v-for="row in list" :key="row.id" :to="`/news/${row.id}`" class="news-card">
            <div class="news-card-media">
              <img
                v-if="row.coverUrl"
                class="news-card-img"
                :src="resolveMediaUrl(row.coverUrl)"
                alt=""
                loading="lazy"
              />
              <div v-else class="news-card-placeholder" aria-hidden="true">
                <span class="news-card-placeholder-icon">📰</span>
              </div>
              <span v-if="displayCategory(row)" class="news-card-cat">{{ displayCategory(row) }}</span>
              <span class="news-card-urgency" :class="'u' + (row.urgencyLevel || 1)">{{ urgencyLabel(row.urgencyLevel) }}</span>
            </div>
            <div class="news-card-body">
              <h2 class="news-card-title">{{ row.title }}</h2>
              <p v-if="row.summary" class="news-card-excerpt">{{ row.summary }}</p>
              <div class="news-card-meta">
                <span class="meta-item">
                  <svg class="meta-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                    <rect x="3" y="4" width="18" height="18" rx="2" ry="2" />
                    <line x1="16" y1="2" x2="16" y2="6" />
                    <line x1="8" y1="2" x2="8" y2="6" />
                    <line x1="3" y1="10" x2="21" y2="10" />
                  </svg>
                  {{ row.publishTime ? formatDateTime(row.publishTime) : '—' }}
                </span>
                <span class="meta-item">
                  <svg class="meta-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                    <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z" />
                    <circle cx="12" cy="10" r="3" />
                  </svg>
                  {{ row.region || '—' }}
                </span>
                <span v-if="row.likeCount != null" class="meta-item">
                  <svg class="meta-icon" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
                    <path
                      d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"
                    />
                  </svg>
                  {{ row.likeCount }}
                </span>
              </div>
              <span v-if="row.publisherName" class="news-card-publisher">发布：{{ row.publisherName }}</span>
            </div>
          </router-link>
        </div>
        <div v-else class="state-empty">
          <svg class="empty-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
            <path d="M9 12h6m-6 4h6m2-5a9 9 0 1 1-18 0 9 9 0 0 1 18 0z" />
          </svg>
          <p>暂无符合条件的新闻</p>
        </div>
        <PaginationBar
          v-model="query.pageNum"
          class="pager-bar"
          :total="total"
          :page-size="query.pageSize"
          @current-change="load"
        />
      </template>
    </div>
  </KnowledgeModuleShell>
</template>

<script setup>
import { ref, reactive, computed, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { getNewsList, getNewsCategories, getNewsRegions } from '@/api/news'
import { resolveMediaUrl } from '@/utils/resolveMediaUrl'
import { formatDateTime } from '@/utils/formatDateTime'
import PaginationBar from '@/components/PaginationBar.vue'
import KnowledgeModuleShell from '@/components/KnowledgeModuleShell.vue'

const crumbs = [
  { label: '首页', to: '/home' },
  { label: '新闻' },
]

const list = ref([])
const categories = ref([])
const regionOptions = ref([])
const selectedRegions = ref([])
const regionOpen = ref(false)
const regionTriggerWrapRef = ref(null)
const regionPanelRef = ref(null)
const regionPanelStyle = ref({})
const total = ref(0)
const loading = ref(false)

const query = reactive({
  title: '',
  keyword: '',
  categoryId: '',
  orderBy: 'publishTime',
  order: 'desc',
  pageNum: 1,
  pageSize: 12,
})

const regionSummary = computed(() => {
  const arr = selectedRegions.value
  if (!arr.length) return '全部地区'
  if (arr.length <= 2) return arr.join('、')
  return `已选 ${arr.length} 个地区`
})

function syncRegionPanelPosition() {
  const wrap = regionTriggerWrapRef.value
  if (!wrap) return
  const r = wrap.getBoundingClientRect()
  const vw = typeof window !== 'undefined' ? window.innerWidth : 400
  const maxW = Math.min(320, Math.round(vw * 0.92))
  regionPanelStyle.value = {
    position: 'fixed',
    top: `${Math.round(r.bottom + 4)}px`,
    left: `${Math.round(r.left)}px`,
    minWidth: `${Math.max(Math.round(r.width), 200)}px`,
    maxWidth: `${maxW}px`,
    zIndex: 4000,
  }
}

function toggleRegionPanel() {
  regionOpen.value = !regionOpen.value
  if (regionOpen.value) {
    nextTick(() => syncRegionPanelPosition())
  }
}

function pickAllRegions() {
  selectedRegions.value = []
  regionOpen.value = false
  search()
}

function onRegionCheckChange() {
  search()
}

function onDocumentClick(e) {
  if (!regionOpen.value) return
  const wrap = regionTriggerWrapRef.value
  const panel = regionPanelRef.value
  if (wrap?.contains(e.target) || panel?.contains(e.target)) return
  regionOpen.value = false
}

function displayCategory(row) {
  return row.categoryName || row.category || ''
}

function urgencyLabel(level) {
  const m = { 1: '低', 2: '中', 3: '高' }
  return m[level] || ''
}

function search() {
  query.pageNum = 1
  load()
}

async function loadCategories() {
  try {
    categories.value = (await getNewsCategories()) || []
  } catch (_) {
    categories.value = []
  }
}

async function loadRegionOptions() {
  try {
    regionOptions.value = (await getNewsRegions()) || []
  } catch (_) {
    regionOptions.value = []
  }
}

async function load() {
  loading.value = true
  try {
    const cid = query.categoryId === '' || query.categoryId == null ? undefined : Number(query.categoryId)
    const regions = selectedRegions.value.length ? [...selectedRegions.value] : undefined
    const data = await getNewsList({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      title: query.title?.trim() || undefined,
      keyword: query.keyword?.trim() || undefined,
      categoryId: cid,
      regions,
      orderBy: query.orderBy,
      order: query.order,
    })
    list.value = data.records ?? data.list ?? []
    total.value = data.total ?? list.value.length
  } catch (e) {
    console.error(e)
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

watch(regionOpen, (open) => {
  if (open) {
    nextTick(() => syncRegionPanelPosition())
    window.addEventListener('scroll', syncRegionPanelPosition, true)
    window.addEventListener('resize', syncRegionPanelPosition)
  } else {
    window.removeEventListener('scroll', syncRegionPanelPosition, true)
    window.removeEventListener('resize', syncRegionPanelPosition)
  }
})

onMounted(async () => {
  document.addEventListener('click', onDocumentClick)
  await loadCategories()
  await loadRegionOptions()
  load()
})

onUnmounted(() => {
  document.removeEventListener('click', onDocumentClick)
  window.removeEventListener('scroll', syncRegionPanelPosition, true)
  window.removeEventListener('resize', syncRegionPanelPosition)
})
</script>

<style scoped>
.news-page {
  min-width: 0;
}

.news-hero {
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(59, 130, 246, 0.12);
}

.news-hero-title {
  margin: 0 0 6px;
  font-size: 1.5rem;
  font-weight: 800;
  color: var(--client-text);
  letter-spacing: -0.02em;
}

.news-hero-sub {
  margin: 0;
  font-size: 0.875rem;
  color: var(--client-muted);
  line-height: 1.5;
}

.news-toolbar {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 22px;
}

.search-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.search-wrap {
  display: flex;
  align-items: stretch;
  flex: 1;
  min-width: min(100%, 200px);
  max-width: 360px;
  border: 1.5px solid rgba(59, 130, 246, 0.2);
  border-radius: 12px;
  overflow: hidden;
  background: var(--client-surface);
  transition: border-color 0.2s, box-shadow 0.2s;
}

.search-wrap:focus-within {
  border-color: var(--client-primary);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.12);
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

.filter-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.filter-select {
  padding: 9px 12px;
  border: 1px solid rgba(59, 130, 246, 0.2);
  border-radius: 10px;
  font-size: 0.875rem;
  color: var(--client-text);
  background: var(--client-surface);
  cursor: pointer;
  min-width: 140px;
  font-family: var(--client-font);
}

.filter-select:focus {
  outline: none;
  border-color: var(--client-primary);
}

.filter-select--narrow {
  min-width: 88px;
}

.btn-query {
  padding: 9px 18px;
  border: none;
  border-radius: 10px;
  font-size: 0.875rem;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, var(--client-primary), var(--client-primary-hover));
  cursor: pointer;
  box-shadow: 0 4px 14px rgba(59, 130, 246, 0.3);
  transition: transform 0.15s, box-shadow 0.2s;
}

.btn-query:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 18px rgba(59, 130, 246, 0.38);
}

.region-dropdown {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.region-label {
  font-size: 0.8125rem;
  color: var(--client-muted);
  flex-shrink: 0;
}

.region-trigger-wrap {
  display: inline-block;
}

.region-trigger {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 140px;
  max-width: 240px;
  padding: 9px 12px;
  border: 1px solid rgba(59, 130, 246, 0.2);
  border-radius: 10px;
  font-size: 0.875rem;
  background: var(--client-surface);
  color: var(--client-text);
  cursor: pointer;
  text-align: left;
  font-family: var(--client-font);
}

.region-trigger:hover {
  border-color: var(--client-primary);
}

.region-trigger-text {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.region-caret {
  flex-shrink: 0;
  font-size: 10px;
  color: var(--client-muted);
}

.region-panel {
  max-height: 280px;
  overflow-y: auto;
  padding: 8px 0;
  background: var(--client-surface);
  border: 1px solid rgba(59, 130, 246, 0.15);
  border-radius: 12px;
  box-shadow: var(--client-shadow);
}

.region-panel-fixed {
  box-sizing: border-box;
}

.region-all-btn {
  display: block;
  width: 100%;
  padding: 10px 14px;
  margin: 0;
  border: none;
  border-bottom: 1px solid rgba(59, 130, 246, 0.1);
  background: rgba(59, 130, 246, 0.06);
  font-size: 0.875rem;
  color: var(--client-primary);
  cursor: pointer;
  text-align: left;
  font-weight: 600;
}

.region-all-btn:hover {
  background: var(--client-accent-soft);
}

.region-check-list {
  padding: 6px 0;
}

.region-option {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  margin: 0;
  cursor: pointer;
  font-size: 0.875rem;
  color: var(--client-text);
}

.region-option:hover {
  background: rgba(59, 130, 246, 0.06);
}

.state-loading {
  padding: 48px 16px;
  text-align: center;
  color: var(--client-muted);
  font-size: 0.9375rem;
}

.state-empty {
  text-align: center;
  padding: 48px 20px;
  color: var(--client-muted);
}

.empty-icon {
  width: 48px;
  height: 48px;
  margin: 0 auto 12px;
  opacity: 0.45;
  color: var(--client-primary);
}

.news-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(100%, 300px), 1fr));
  gap: 18px;
}

.news-card {
  display: flex;
  flex-direction: column;
  text-decoration: none;
  color: inherit;
  border-radius: calc(var(--client-radius) - 2px);
  overflow: hidden;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(59, 130, 246, 0.1);
  box-shadow: 0 1px 0 rgba(59, 130, 246, 0.04);
  transition: border-color 0.25s, box-shadow 0.25s, transform 0.2s;
}

.news-card:hover {
  border-color: rgba(59, 130, 246, 0.35);
  box-shadow: 0 14px 36px rgba(59, 130, 246, 0.12);
  transform: translateY(-3px);
}

.news-card-media {
  position: relative;
  aspect-ratio: 16 / 9;
  background: rgba(59, 130, 246, 0.08);
  overflow: hidden;
}

.news-card-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.35s ease;
}

.news-card:hover .news-card-img {
  transform: scale(1.04);
}

.news-card-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(145deg, var(--client-accent-soft), rgba(59, 130, 246, 0.12));
}

.news-card-placeholder-icon {
  font-size: 2.5rem;
  opacity: 0.35;
}

.news-card-cat {
  position: absolute;
  top: 10px;
  right: 10px;
  padding: 4px 10px;
  border-radius: 8px;
  font-size: 0.6875rem;
  font-weight: 700;
  color: var(--client-primary-hover);
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(59, 130, 246, 0.2);
  max-width: calc(100% - 100px);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.news-card-urgency {
  position: absolute;
  top: 10px;
  left: 10px;
  padding: 4px 8px;
  border-radius: 6px;
  font-size: 0.6875rem;
  font-weight: 700;
}

.news-card-urgency.u1 {
  background: rgba(241, 245, 249, 0.95);
  color: var(--client-muted);
}

.news-card-urgency.u2 {
  background: rgba(254, 243, 199, 0.95);
  color: #b45309;
}

.news-card-urgency.u3 {
  background: rgba(254, 226, 226, 0.95);
  color: #b91c1c;
}

.news-card-body {
  padding: 14px 16px 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
}

.news-card-title {
  margin: 0;
  font-size: 1.0625rem;
  font-weight: 700;
  color: var(--client-text);
  line-height: 1.35;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  transition: color 0.2s;
}

.news-card:hover .news-card-title {
  color: var(--client-primary);
}

.news-card-excerpt {
  margin: 0;
  font-size: 0.8125rem;
  color: var(--client-muted);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  flex: 1;
}

.news-card-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 14px;
  margin-top: auto;
  padding-top: 4px;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 0.75rem;
  color: var(--client-muted);
  font-weight: 500;
}

.meta-icon {
  width: 14px;
  height: 14px;
  flex-shrink: 0;
  opacity: 0.85;
  color: var(--client-primary);
}

.news-card-publisher {
  font-size: 0.6875rem;
  color: var(--client-muted);
  opacity: 0.9;
}

.pager-bar {
  margin-top: 24px;
}

@media (max-width: 520px) {
  .filter-row {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-select {
    width: 100%;
  }

  .btn-query {
    width: 100%;
  }
}
</style>
