<template>
  <KnowledgeModuleShell :crumbs="crumbs">
    <header class="page-head">
      <div class="page-head-text">
        <h1 class="page-title">我的收藏</h1>
        <p class="page-sub">知识、论坛、新闻与消防器材收藏统一在此查看</p>
      </div>
    </header>

    <div class="view-tabs" role="tablist">
      <button
        v-for="t in tabs"
        :key="t.module"
        type="button"
        role="tab"
        :class="{ active: module === t.module }"
        :aria-selected="module === t.module"
        @click="setModule(t.module)"
      >
        {{ t.label }}
      </button>
    </div>

    <ul class="item-list">
      <li v-for="row in list" :key="`${row.module}-${row.id}`" class="item-card">
        <router-link :to="linkTo(row)" class="item-title">{{ row.title || '（无标题）' }}</router-link>
        <div class="item-meta">
          <span v-if="showCategoryTag(row) && row.categoryName" class="item-tag">{{ row.categoryName }}</span>
          <span class="item-time">收藏时间：{{ row.collectTime ? formatDateTime(row.collectTime) : '-' }}</span>
        </div>
      </li>
      <li v-if="list.length === 0" class="item-empty">暂无收藏</li>
    </ul>

    <PaginationBar v-model="pageNum" class="pager-bar" :total="total" :page-size="pageSize" @current-change="load" />
  </KnowledgeModuleShell>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getCollectList } from '@/api/content'
import { formatDateTime } from '@/utils/formatDateTime'
import PaginationBar from '@/components/PaginationBar.vue'
import KnowledgeModuleShell from '@/components/KnowledgeModuleShell.vue'

const route = useRoute()

const crumbs = [
  { label: '首页', to: '/' },
  { label: '消防知识', to: '/knowledge' },
  { label: '我的收藏' },
]

const tabs = [
  { module: 'knowledge', label: '知识' },
  { module: 'forum', label: '论坛' },
  { module: 'news', label: '新闻' },
  { module: 'equipment', label: '器材' },
]

const module = ref('knowledge')
const list = ref([])
const pageNum = ref(1)
const pageSize = 10
const total = ref(0)

function showCategoryTag(row) {
  const m = row.module || 'knowledge'
  return m === 'knowledge' || m === 'equipment'
}

function linkTo(row) {
  const m = row.module || 'knowledge'
  if (m === 'forum') return `/forum/${row.id}`
  if (m === 'news') return `/news/${row.id}`
  if (m === 'equipment') return `/equipment/${row.id}`
  return `/knowledge/${row.id}`
}

function setModule(m) {
  module.value = m
  pageNum.value = 1
  load()
}

async function load() {
  try {
    const data = await getCollectList({
      pageNum: pageNum.value,
      pageSize,
      module: module.value,
    })
    list.value = data?.list ?? data?.records ?? []
    total.value = Number(data?.total ?? 0)
  } catch (e) {
    console.error(e)
    alert(e.message || '加载收藏失败')
  }
}

function applyModuleFromRoute() {
  const q = route.query.module
  if (typeof q === 'string' && ['knowledge', 'forum', 'news', 'equipment'].includes(q)) {
    module.value = q
  }
}

onMounted(() => {
  applyModuleFromRoute()
  load()
})

watch(
  () => route.query.module,
  (q) => {
    if (typeof q === 'string' && ['knowledge', 'forum', 'news', 'equipment'].includes(q) && q !== module.value) {
      module.value = q
      pageNum.value = 1
      load()
    }
  },
)
</script>

<style scoped>
.page-head {
  margin-bottom: 18px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(59, 130, 246, 0.12);
}

.page-title {
  margin: 0 0 6px;
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--client-text);
  letter-spacing: -0.02em;
}

.page-sub {
  margin: 0;
  font-size: 0.875rem;
  color: var(--client-muted);
}

.view-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 20px;
}

.view-tabs button {
  padding: 10px 18px;
  border: 1px solid rgba(59, 130, 246, 0.25);
  border-radius: 999px;
  background: rgba(59, 130, 246, 0.06);
  cursor: pointer;
  font-size: 0.875rem;
  color: var(--client-text);
  transition: background 0.15s, border-color 0.15s, color 0.15s;
}

.view-tabs button:hover {
  border-color: var(--client-primary);
  background: var(--client-accent-soft);
}

.view-tabs button.active {
  background: linear-gradient(135deg, var(--client-primary) 0%, var(--client-accent) 100%);
  border-color: transparent;
  color: #fff;
  font-weight: 600;
  box-shadow: 0 4px 14px rgba(59, 130, 246, 0.35);
}

.item-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.item-card {
  padding: 14px 16px;
  border-radius: 10px;
  border: 1px solid rgba(59, 130, 246, 0.12);
  background: rgba(59, 130, 246, 0.03);
  transition: border-color 0.15s, box-shadow 0.15s;
}

.item-card:hover {
  border-color: rgba(59, 130, 246, 0.22);
  box-shadow: 0 4px 16px rgba(59, 130, 246, 0.08);
}

.item-title {
  display: block;
  font-size: 1rem;
  font-weight: 600;
  color: var(--client-text);
  text-decoration: none;
  line-height: 1.4;
}

.item-title:hover {
  color: var(--client-primary);
}

.item-meta {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px 12px;
  font-size: 0.8125rem;
  color: var(--client-muted);
}

.item-tag {
  padding: 2px 8px;
  border-radius: 6px;
  background: rgba(59, 130, 246, 0.1);
  color: var(--client-primary-hover);
  font-weight: 600;
  font-size: 0.75rem;
}

.item-empty {
  text-align: center;
  padding: 40px 16px;
  color: var(--client-muted);
  font-size: 0.875rem;
  border: 1px dashed rgba(59, 130, 246, 0.25);
  border-radius: var(--client-radius);
  background: rgba(59, 130, 246, 0.04);
}

.pager-bar {
  margin-top: 22px;
}
</style>
