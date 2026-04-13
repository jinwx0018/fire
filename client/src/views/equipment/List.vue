<template>
  <KnowledgeModuleShell :crumbs="crumbs">
    <div class="equipment-page">
      <header class="eq-hero">
        <div class="eq-hero-icon" aria-hidden="true">
          <svg viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg">
            <circle cx="32" cy="32" r="30" fill="rgba(255,255,255,0.18)" />
            <path
              d="M32 10C32 10 20 22 20 32C20 38.627 25.373 44 32 44C38.627 44 44 38.627 44 32C44 22 32 10 32 10Z"
              fill="white"
              fill-opacity="0.92"
            />
            <path
              d="M32 20C32 20 26 27 26 32C26 35.314 28.686 38 32 38C35.314 38 38 35.314 38 32C38 27 32 20 32 20Z"
              fill="var(--client-primary)"
            />
            <rect x="29" y="44" width="6" height="10" rx="3" fill="white" fill-opacity="0.92" />
          </svg>
        </div>
        <div class="eq-hero-text">
          <h1 class="eq-hero-title">消防器材</h1>
          <p class="eq-hero-sub">了解各类消防器材的使用方法，守护生命安全</p>
        </div>
        <div class="eq-hero-stats">
          <div class="eq-stat">
            <span class="eq-stat-num">{{ total }}</span>
            <span class="eq-stat-label">件器材</span>
          </div>
          <div class="eq-stat-divider" />
          <div class="eq-stat">
            <span class="eq-stat-num">{{ types.length }}</span>
            <span class="eq-stat-label">个分类</span>
          </div>
        </div>
      </header>

      <section class="eq-toolbar" aria-label="搜索与筛选">
        <div class="eq-search-row">
          <div class="eq-search-box">
            <svg class="eq-search-icon" viewBox="0 0 20 20" fill="none" aria-hidden="true">
              <circle cx="9" cy="9" r="6" stroke="currentColor" stroke-width="1.5" />
              <path d="M13.5 13.5L17 17" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
            </svg>
            <input
              v-model="query.keyword"
              type="search"
              class="eq-search-input"
              placeholder="搜索器材名称…"
              @keyup.enter="search"
            />
            <button v-if="query.keyword" type="button" class="eq-clear-btn" aria-label="清空" @click="clearSearch">
              <svg viewBox="0 0 16 16" fill="none" aria-hidden="true">
                <path d="M4 4L12 12M12 4L4 12" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
              </svg>
            </button>
          </div>
          <div class="eq-cat-tabs" role="tablist">
            <button
              v-for="tab in categoryTabs"
              :key="tab.value === '' ? 'all' : tab.value"
              type="button"
              role="tab"
              class="eq-cat-tab"
              :class="{ 'is-active': typeIdStr === tab.value }"
              @click="selectCategory(tab.value)"
            >
              {{ tab.label }}
            </button>
          </div>
          <button type="button" class="eq-query-btn" @click="search">
            <svg viewBox="0 0 20 20" fill="none" aria-hidden="true">
              <circle cx="9" cy="9" r="6" stroke="currentColor" stroke-width="1.5" />
              <path d="M13.5 13.5L17 17" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
            </svg>
            查询
          </button>
        </div>
      </section>

      <div class="eq-result-bar">
        <p class="eq-result-text">
          共找到 <strong>{{ total }}</strong> 件器材
          <span v-if="query.keyword?.trim() || typeIdStr !== ''" class="eq-filter-tag">
            <template v-if="query.keyword?.trim()">「{{ query.keyword.trim() }}」</template>
            <template v-if="typeIdStr !== ''"> · {{ getCategoryLabel(typeIdStr) }}</template>
          </span>
        </p>
        <div class="eq-view-toggle" role="group" aria-label="视图切换">
          <button
            type="button"
            class="eq-toggle-btn"
            :class="{ 'is-on': viewMode === 'grid' }"
            title="网格视图"
            @click="viewMode = 'grid'"
          >
            <svg viewBox="0 0 16 16" fill="currentColor" aria-hidden="true">
              <rect x="1" y="1" width="6" height="6" rx="1" />
              <rect x="9" y="1" width="6" height="6" rx="1" />
              <rect x="1" y="9" width="6" height="6" rx="1" />
              <rect x="9" y="9" width="6" height="6" rx="1" />
            </svg>
          </button>
          <button
            type="button"
            class="eq-toggle-btn"
            :class="{ 'is-on': viewMode === 'list' }"
            title="列表视图"
            @click="viewMode = 'list'"
          >
            <svg viewBox="0 0 16 16" fill="currentColor" aria-hidden="true">
              <rect x="1" y="2" width="14" height="2.5" rx="1" />
              <rect x="1" y="6.75" width="14" height="2.5" rx="1" />
              <rect x="1" y="11.5" width="14" height="2.5" rx="1" />
            </svg>
          </button>
        </div>
      </div>

      <div v-if="loading" class="eq-loading">
        <div class="eq-spinner" />
        <p>加载中…</p>
      </div>

      <div v-else-if="!list.length" class="eq-empty">
        <div class="eq-empty-icon" aria-hidden="true">
          <svg viewBox="0 0 80 80" fill="none">
            <circle cx="40" cy="40" r="36" fill="var(--client-accent-soft)" stroke="rgba(59,130,246,0.25)" stroke-width="2" />
            <path
              d="M40 25C40 25 30 35 30 42C30 47.523 34.477 52 40 52C45.523 52 50 47.523 50 42C50 35 40 25 40 25Z"
              fill="var(--client-primary)"
              opacity="0.25"
            />
            <path d="M36 56H44V60H36V56Z" fill="var(--client-primary)" opacity="0.25" />
            <path d="M28 68H52" stroke="var(--client-primary)" stroke-width="2" stroke-linecap="round" opacity="0.35" />
          </svg>
        </div>
        <h3 class="eq-empty-title">暂无相关器材</h3>
        <p class="eq-empty-hint">尝试调整搜索条件或浏览全部器材</p>
        <button type="button" class="eq-reset-btn" @click="resetFilters">查看全部器材</button>
      </div>

      <div v-else-if="viewMode === 'grid'" class="eq-grid">
        <router-link
          v-for="row in list"
          :key="row.id"
          :to="`/equipment/${row.id}`"
          class="eq-grid-card"
        >
          <div class="eq-card-image-wrap">
            <img
              v-if="row.cover"
              :src="resolveMediaUrl(row.cover)"
              :alt="row.name"
              class="eq-card-img"
              @error="(e) => onImgErr(e, row.id)"
            />
            <div v-show="!imageOk[row.id]" class="eq-card-placeholder">
              <svg viewBox="0 0 48 48" fill="none" aria-hidden="true">
                <path
                  d="M24 8C24 8 14 18 14 25C14 30.523 18.477 35 24 35C29.523 35 34 30.523 34 25C34 18 24 8 24 8Z"
                  fill="var(--client-primary)"
                  opacity="0.22"
                />
                <path d="M21 35H27V40H21V35Z" fill="var(--client-primary)" opacity="0.22" />
              </svg>
              <span>暂无图片</span>
            </div>
            <span v-if="row.typeName" class="eq-card-badge">{{ row.typeName }}</span>
          </div>
          <div class="eq-card-body">
            <h3 class="eq-card-title">{{ row.name }}</h3>
            <p class="eq-card-desc">{{ row.summary || '暂无描述' }}</p>
            <span class="eq-card-link">查看详情 →</span>
          </div>
        </router-link>
      </div>

      <ul v-else class="eq-list">
        <li v-for="row in list" :key="row.id" class="eq-list-item">
          <router-link :to="`/equipment/${row.id}`" class="eq-list-link">
            <div class="eq-list-img-wrap">
              <img
                v-if="row.cover"
                :src="resolveMediaUrl(row.cover)"
                :alt="row.name"
                class="eq-list-img"
                @error="(e) => onImgErr(e, row.id)"
              />
              <div v-show="!imageOk[row.id]" class="eq-list-placeholder">
                <svg viewBox="0 0 48 48" fill="none" aria-hidden="true">
                  <path
                    d="M24 8C24 8 14 18 14 25C14 30.523 18.477 35 24 35C29.523 35 34 30.523 34 25C34 18 24 8 24 8Z"
                    fill="var(--client-primary)"
                    opacity="0.25"
                  />
                </svg>
              </div>
            </div>
            <div class="eq-list-main">
              <div class="eq-list-head">
                <h3 class="eq-list-title">{{ row.name }}</h3>
                <span v-if="row.typeName" class="eq-list-badge">{{ row.typeName }}</span>
              </div>
              <p class="eq-list-desc">{{ row.summary || '暂无描述' }}</p>
            </div>
            <div class="eq-list-arrow" aria-hidden="true">
              <svg viewBox="0 0 20 20" fill="none">
                <path
                  d="M7 4L13 10L7 16"
                  stroke="var(--client-primary)"
                  stroke-width="1.5"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                />
              </svg>
            </div>
          </router-link>
        </li>
      </ul>

      <PaginationBar
        v-if="total > query.pageSize"
        v-model="query.pageNum"
        class="eq-pager"
        :total="total"
        :page-size="query.pageSize"
        @current-change="load"
      />
    </div>
  </KnowledgeModuleShell>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { getEquipmentList, getEquipmentTypeList } from '@/api/equipment'
import { resolveMediaUrl } from '@/utils/resolveMediaUrl'
import PaginationBar from '@/components/PaginationBar.vue'
import KnowledgeModuleShell from '@/components/KnowledgeModuleShell.vue'

const list = ref([])
const types = ref([])
const total = ref(0)
const loading = ref(false)
const viewMode = ref('grid')
const imageOk = ref({})

const query = reactive({
  keyword: '',
  typeId: undefined,
  pageNum: 1,
  pageSize: 12,
})

const typeIdStr = ref('')

const crumbs = computed(() => [
  { label: '首页', to: '/home' },
  { label: '消防器材', to: '' },
])

const categoryTabs = computed(() => {
  const tabs = [{ value: '', label: '全部类型' }]
  for (const t of types.value) {
    tabs.push({ value: String(t.id), label: t.name })
  }
  return tabs
})

watch(typeIdStr, (v) => {
  query.typeId = v === '' ? undefined : Number(v)
})

function getCategoryLabel(val) {
  const t = types.value.find((x) => String(x.id) === String(val))
  return t?.name || ''
}

function selectCategory(value) {
  typeIdStr.value = value
  query.pageNum = 1
  load()
}

function search() {
  query.pageNum = 1
  load()
}

function clearSearch() {
  query.keyword = ''
  search()
}

function resetFilters() {
  query.keyword = ''
  typeIdStr.value = ''
  query.pageNum = 1
  load()
}

function onImgErr(e, id) {
  const img = e.target
  if (img instanceof HTMLImageElement) img.style.display = 'none'
  imageOk.value = { ...imageOk.value, [id]: false }
}

watch(
  list,
  (rows) => {
    const next = {}
    for (const r of rows) {
      next[r.id] = !!r.cover
    }
    imageOk.value = next
  },
  { immediate: true },
)

async function loadTypes() {
  try {
    const data = await getEquipmentTypeList()
    types.value = Array.isArray(data) ? data : []
  } catch {
    types.value = []
  }
}

async function load() {
  loading.value = true
  try {
    const data = await getEquipmentList({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      keyword: query.keyword?.trim() || undefined,
      typeId: query.typeId,
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

onMounted(async () => {
  await loadTypes()
  load()
})
</script>

<style scoped>
.equipment-page {
  min-width: 0;
}

/* —— Hero —— */
.eq-hero {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 20px;
  margin: -4px 0 20px;
  padding: 22px 22px 24px;
  border-radius: var(--client-radius);
  background: linear-gradient(
    125deg,
    var(--client-primary) 0%,
    var(--client-accent) 48%,
    var(--client-primary-hover) 100%
  );
  color: #fff;
  position: relative;
  overflow: hidden;
  box-shadow: 0 12px 40px rgba(59, 130, 246, 0.22);
}

.eq-hero::before {
  content: '';
  position: absolute;
  top: -40px;
  right: -40px;
  width: 180px;
  height: 180px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
  pointer-events: none;
}

.eq-hero::after {
  content: '';
  position: absolute;
  bottom: -50px;
  left: 5%;
  width: 140px;
  height: 140px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.06);
  pointer-events: none;
}

.eq-hero-icon,
.eq-hero-text,
.eq-hero-stats {
  position: relative;
  z-index: 1;
}

.eq-hero-icon {
  width: 72px;
  height: 72px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.25);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  backdrop-filter: blur(8px);
}

.eq-hero-icon svg {
  width: 44px;
  height: 44px;
}

.eq-hero-text {
  flex: 1;
  min-width: 200px;
}

.eq-hero-title {
  margin: 0 0 6px;
  font-size: 1.75rem;
  font-weight: 800;
  letter-spacing: 0.02em;
}

.eq-hero-sub {
  margin: 0;
  font-size: 0.9375rem;
  opacity: 0.92;
  line-height: 1.45;
}

.eq-hero-stats {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 14px 22px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.22);
  backdrop-filter: blur(8px);
}

.eq-stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.eq-stat-num {
  font-size: 1.65rem;
  font-weight: 800;
  line-height: 1;
}

.eq-stat-label {
  font-size: 0.75rem;
  opacity: 0.85;
}

.eq-stat-divider {
  width: 1px;
  height: 32px;
  background: rgba(255, 255, 255, 0.35);
}

/* —— Toolbar —— */
.eq-toolbar {
  margin-bottom: 18px;
}

.eq-search-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}

.eq-search-box {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 200px;
  flex: 1;
  max-width: 320px;
  padding: 0 12px;
  border-radius: 10px;
  border: 1.5px solid rgba(59, 130, 246, 0.2);
  background: rgba(240, 247, 252, 0.65);
  transition:
    border-color 0.2s,
    box-shadow 0.2s;
}

.eq-search-box:focus-within {
  border-color: var(--client-primary);
  background: var(--client-surface);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.12);
}

.eq-search-icon {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
  color: var(--client-muted);
}

.eq-search-input {
  flex: 1;
  min-width: 0;
  border: none;
  background: transparent;
  padding: 10px 0;
  font-size: 0.875rem;
  color: var(--client-text);
  outline: none;
}

.eq-search-input::placeholder {
  color: #94a3b8;
}

.eq-clear-btn {
  display: flex;
  padding: 4px;
  border: none;
  background: none;
  cursor: pointer;
  color: var(--client-muted);
  border-radius: 6px;
}

.eq-clear-btn:hover {
  color: var(--client-text);
  background: rgba(59, 130, 246, 0.08);
}

.eq-clear-btn svg {
  width: 14px;
  height: 14px;
}

.eq-cat-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  flex: 1;
  min-width: 0;
}

.eq-cat-tab {
  padding: 7px 14px;
  border-radius: 8px;
  border: 1.5px solid rgba(59, 130, 246, 0.18);
  background: rgba(240, 247, 252, 0.5);
  font-size: 0.8125rem;
  font-weight: 500;
  color: var(--client-text);
  cursor: pointer;
  transition:
    background 0.2s,
    border-color 0.2s,
    color 0.2s;
  white-space: nowrap;
}

.eq-cat-tab:hover {
  border-color: rgba(59, 130, 246, 0.35);
  background: var(--client-accent-soft);
}

.eq-cat-tab.is-active {
  border-color: var(--client-primary);
  background: var(--client-primary);
  color: #fff;
  font-weight: 700;
}

.eq-query-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 18px;
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

.eq-query-btn:hover {
  background: var(--client-primary-hover);
}

.eq-query-btn svg {
  width: 18px;
  height: 18px;
}

/* —— Result bar —— */
.eq-result-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(59, 130, 246, 0.1);
}

.eq-result-text {
  margin: 0;
  font-size: 0.875rem;
  color: var(--client-muted);
}

.eq-result-text strong {
  color: var(--client-text);
  font-weight: 700;
}

.eq-filter-tag {
  color: var(--client-primary);
  font-weight: 600;
}

.eq-view-toggle {
  display: inline-flex;
  border-radius: 10px;
  border: 1px solid rgba(59, 130, 246, 0.2);
  overflow: hidden;
  background: rgba(240, 247, 252, 0.6);
}

.eq-toggle-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 36px;
  border: none;
  background: transparent;
  color: var(--client-muted);
  cursor: pointer;
  transition:
    background 0.2s,
    color 0.2s;
}

.eq-toggle-btn:hover {
  color: var(--client-primary);
  background: rgba(59, 130, 246, 0.08);
}

.eq-toggle-btn.is-on {
  background: var(--client-accent-soft);
  color: var(--client-primary);
}

.eq-toggle-btn svg {
  width: 16px;
  height: 16px;
}

/* —— Loading / empty —— */
.eq-loading {
  text-align: center;
  padding: 48px 16px;
  color: var(--client-muted);
}

.eq-spinner {
  width: 40px;
  height: 40px;
  margin: 0 auto 12px;
  border: 3px solid rgba(59, 130, 246, 0.2);
  border-top-color: var(--client-primary);
  border-radius: 50%;
  animation: eq-spin 0.75s linear infinite;
}

@keyframes eq-spin {
  to {
    transform: rotate(360deg);
  }
}

.eq-empty {
  text-align: center;
  padding: 40px 16px 48px;
}

.eq-empty-icon {
  margin-bottom: 12px;
}

.eq-empty-icon svg {
  width: 80px;
  height: 80px;
  margin: 0 auto;
  display: block;
}

.eq-empty-title {
  margin: 0 0 8px;
  font-size: 1.125rem;
  color: var(--client-text);
}

.eq-empty-hint {
  margin: 0 0 20px;
  font-size: 0.875rem;
  color: var(--client-muted);
}

.eq-reset-btn {
  padding: 10px 22px;
  border: none;
  border-radius: 10px;
  background: var(--client-primary);
  color: #fff;
  font-size: 0.875rem;
  font-weight: 700;
  cursor: pointer;
}

.eq-reset-btn:hover {
  background: var(--client-primary-hover);
}

/* —— Grid —— */
.eq-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 18px;
}

.eq-grid-card {
  display: flex;
  flex-direction: column;
  border-radius: var(--client-radius);
  border: 1px solid rgba(59, 130, 246, 0.12);
  background: var(--client-surface);
  overflow: hidden;
  text-decoration: none;
  color: inherit;
  box-shadow: 0 2px 12px rgba(59, 130, 246, 0.06);
  transition:
    transform 0.2s,
    box-shadow 0.2s;
}

.eq-grid-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--client-shadow);
  border-color: rgba(59, 130, 246, 0.22);
}

.eq-card-image-wrap {
  position: relative;
  aspect-ratio: 16 / 10;
  background: linear-gradient(145deg, #e0f2fe 0%, #f0f9ff 100%);
  overflow: hidden;
}

.eq-card-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.eq-card-placeholder {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-size: 0.75rem;
  color: var(--client-muted);
}

.eq-card-placeholder svg {
  width: 48px;
  height: 48px;
}

.eq-card-badge {
  position: absolute;
  top: 10px;
  left: 10px;
  padding: 4px 10px;
  border-radius: 8px;
  font-size: 0.6875rem;
  font-weight: 700;
  background: rgba(255, 255, 255, 0.92);
  color: var(--client-primary);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.eq-card-body {
  padding: 14px 16px 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
}

.eq-card-title {
  margin: 0;
  font-size: 1rem;
  font-weight: 700;
  color: var(--client-text);
  line-height: 1.35;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.eq-card-desc {
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

.eq-card-link {
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--client-primary);
  margin-top: auto;
}

.eq-grid-card:hover .eq-card-link {
  color: var(--client-primary-hover);
}

/* —— List —— */
.eq-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.eq-list-item {
  margin: 0;
}

.eq-list-link {
  display: flex;
  align-items: stretch;
  gap: 14px;
  padding: 14px 16px;
  border-radius: var(--client-radius);
  border: 1px solid rgba(59, 130, 246, 0.1);
  background: rgba(255, 255, 255, 0.85);
  text-decoration: none;
  color: inherit;
  transition:
    border-color 0.2s,
    box-shadow 0.2s;
}

.eq-list-link:hover {
  border-color: rgba(59, 130, 246, 0.25);
  box-shadow: 0 4px 16px rgba(59, 130, 246, 0.08);
}

.eq-list-img-wrap {
  width: 112px;
  height: 80px;
  flex-shrink: 0;
  border-radius: 10px;
  overflow: hidden;
  background: linear-gradient(145deg, #e0f2fe 0%, #f0f9ff 100%);
  border: 1px solid rgba(59, 130, 246, 0.1);
}

.eq-list-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.eq-list-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.eq-list-placeholder svg {
  width: 40px;
  height: 40px;
}

.eq-list-main {
  flex: 1;
  min-width: 0;
}

.eq-list-head {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 8px 12px;
  margin-bottom: 6px;
}

.eq-list-title {
  margin: 0;
  font-size: 1rem;
  font-weight: 700;
  color: var(--client-text);
}

.eq-list-badge {
  font-size: 0.75rem;
  padding: 2px 8px;
  border-radius: 6px;
  background: var(--client-accent-soft);
  color: var(--client-primary);
  font-weight: 600;
}

.eq-list-desc {
  margin: 0;
  font-size: 0.8125rem;
  color: var(--client-muted);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.eq-list-arrow {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  padding-left: 4px;
}

.eq-list-arrow svg {
  width: 20px;
  height: 20px;
}

.eq-pager {
  margin-top: 24px;
}

@media (max-width: 640px) {
  .eq-hero {
    padding: 18px 16px;
  }

  .eq-hero-stats {
    width: 100%;
    justify-content: center;
  }

  .eq-query-btn {
    width: 100%;
    justify-content: center;
  }
}
</style>
