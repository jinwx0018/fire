<template>
  <div class="knowledge-page">
    <div class="knowledge-layout">
      <aside class="knowledge-sidebar" aria-label="筛选与说明">
        <div v-if="currentView === 'time' || currentView === 'hot'" class="sidebar-card">
          <h3 class="sidebar-title">知识分类</h3>
          <nav class="cat-nav" aria-label="按分类筛选">
            <button
              type="button"
              class="cat-item"
              :class="{ active: isCategoryActive('') }"
              @click="selectCategory('')"
            >
              全部分类
            </button>
            <button
              v-for="c in categories"
              :key="c.id"
              type="button"
              class="cat-item"
              :class="{ active: isCategoryActive(c.id) }"
              @click="selectCategory(c.id)"
            >
              {{ c.name }}
            </button>
          </nav>
        </div>

        <details class="sidebar-card data-source">
          <summary class="data-source-summary">知识数据从哪来？</summary>
          <div class="data-source-body">
            <p>
              本站知识来自<strong>官方公开资料、开放数据、自编科普</strong>等合规来源；入库方式包括管理端录入、种子脚本（如
              <code>DB/seed_knowledge_sample.sql</code>）及调用接口批量写入。
            </p>
            <ul>
              <li>分类与正文存于 <code>knowledge_category</code>、<code>knowledge_content</code>。</li>
              <li>引用外链时请核对时效与版权；教学演示可用种子数据快速填充。</li>
            </ul>
          </div>
        </details>
      </aside>

      <main class="knowledge-main">
        <KnowledgeModuleShell variant="embed" :crumbs="listCrumbs">
        <header class="page-head">
          <div class="page-head-text">
            <h1 class="page-title">消防知识库</h1>
            <p class="page-sub">科普阅读 · 智能推荐 · 全库检索</p>
          </div>
        </header>

        <div class="view-tabs" role="tablist">
          <button
            type="button"
            role="tab"
            :aria-selected="currentView === 'smart'"
            :class="{ active: currentView === 'smart' }"
            @click="setView('smart')"
          >
            智能推荐
          </button>
          <button
            type="button"
            role="tab"
            :aria-selected="currentView === 'time'"
            :class="{ active: currentView === 'time' }"
            @click="setView('time')"
          >
            按时间
          </button>
          <button
            type="button"
            role="tab"
            :aria-selected="currentView === 'hot'"
            :class="{ active: currentView === 'hot' }"
            @click="setView('hot')"
          >
            热门
          </button>
        </div>

        <details class="hint-details">
          <summary>各浏览方式说明</summary>
          <div class="view-hint">
            <template v-if="currentView === 'smart'">
              本 Tab 使用推荐接口：在「已上架」范围内排序；你在个人中心设置的<strong>不感兴趣分类</strong>仅在这里生效。<strong>大模型默认不启用</strong>（即使服务端已配置豆包）：每次进入本页「AI
              增强」均为未勾选，需你<strong>手动勾选</strong>后本次浏览才会传参请求豆包（重排/推荐理由，消耗 Token）；刷新或重新进入页面后需再次勾选。要看<strong>全部</strong>知识或按标题<strong>专门搜索</strong>，请用「按时间」或「热门」。
            </template>
            <template v-else-if="currentView === 'time'">
              列出<strong>全部已上架</strong>知识（不受智能推荐里的屏蔽分类影响），按发布时间排序（可在下方选择从新到旧或从旧到新）。可用分类、标题筛选或搜索。
            </template>
            <template v-else>
              列出<strong>全部已上架</strong>知识（不受智能推荐屏蔽影响），按浏览量优先。可用分类、标题筛选或搜索。
            </template>
          </div>
        </details>

        <div v-if="currentView === 'smart' && pageHint && !loading" class="page-hint" role="note">
          <span class="ph-label">本页策略</span>
          <p class="ph-text">{{ pageHint }}</p>
        </div>
        <div v-if="currentView === 'smart'" class="demo-panel">
          <h3 class="demo-panel-title">课堂演示模式</h3>
          <p class="demo-tip">
            先点击“记录当前 Top5”，再选一条内容注入行为（浏览/点赞/收藏），最后点击“注入并刷新对比”观察排名变化。
          </p>
          <p class="demo-tip demo-tip--sub">
            说明：智能排序<strong>不会单独给「被注入的那一条」硬加分</strong>，而是用行为更新「偏好分类、协同过滤、与近期浏览文本的相似度」等信号。后端 Item-CF 规则是：<strong>已与你发生过浏览/点赞/收藏等互动的知识 ID 不再叠加协同过滤分</strong>（避免把「已看过的」再靠协同顶上去），因此被注入的那一条往往会<strong>失去</strong>这部分分数，而<strong>未互动过</strong>的同类目、共现邻居、文本相近条目反而可能被抬高，出现「注入后目标条排名下降」是正常现象。同分时按内容 ID 降序稳定排序，细微分差也会导致位次交换。下方列表会在注入后重新请求接口；若当前不在第 1 页，请先翻回首页对照 Top5。
          </p>
          <div class="demo-controls">
            <button type="button" @click="captureDemoBaseline" :disabled="loading || !list.length">记录当前 Top5</button>
            <select v-model.number="demoTargetId" :disabled="!list.length || loading">
              <option :value="0">选择要强化的内容</option>
              <option v-for="row in list" :key="row.id" :value="row.id">{{ row.title }}</option>
            </select>
            <button type="button" @click="runDemoBoost" :disabled="loading || !demoTargetId">注入并刷新对比</button>
          </div>
          <p v-if="demoMessage" class="demo-message">{{ demoMessage }}</p>
          <ul v-if="demoCompareRows.length" class="demo-result">
            <li v-for="r in demoCompareRows" :key="r.id">
              <strong>{{ r.title }}</strong>：{{ r.beforeText }} → {{ r.afterText }}
              <span class="delta" :class="{ up: r.delta > 0, down: r.delta < 0 }">{{ r.deltaText }}</span>
            </li>
          </ul>
        </div>

        <div class="toolbar">
          <template v-if="currentView !== 'smart'">
            <label v-if="currentView === 'time'" class="time-order">
              时间
              <select :value="timeOrder" @change="onTimeOrderChange">
                <option value="desc">从新到旧</option>
                <option value="asc">从旧到新</option>
              </select>
            </label>
            <input v-model="query.title" class="title-search" placeholder="标题搜索…" @keyup.enter="load" />
          </template>
          <template v-else>
            <input
              v-model="query.keyword"
              class="title-search kw"
              placeholder="关键词（匹配标题/摘要，可选）"
              @keyup.enter="search"
            />
            <label class="ai-toggle">
              <input v-model="useAiEnhance" type="checkbox" @change="onAiToggle" />
              AI 增强（豆包重排/理由，消耗 Token，默认关）
            </label>
          </template>
          <button type="button" class="btn-query" @click="search">查询</button>
          <label v-if="currentView === 'smart'" class="page-size">
            每页
            <select v-model.number="pageSize" @change="search">
              <option :value="9">9</option>
              <option :value="18">18</option>
            </select>
            条
          </label>
        </div>
        <p v-if="currentView === 'smart'" class="filter-note">
          需要浏览全库或按分类/标题查找时，请切换到「按时间」或「热门」——那里始终对接内容列表接口，展示全部已上架知识（仅受你勾选的分类、标题条件限制）。
        </p>

        <p v-if="!loading && total > 0" class="result-count">共 <strong>{{ total }}</strong> 条</p>

        <p v-if="loading" class="hint loading-line">加载中…</p>
        <template v-else>
          <div
            v-if="currentView === 'smart' && list.length === 0"
            class="cold-start"
          >
            <h3>暂无内容</h3>
            <p>
              可能原因：暂无已上架知识；或关键词过严；或您在个人中心屏蔽了相关分类；或运营侧排除了分类。要浏览<strong>全部</strong>知识请切换「按时间」或「热门」。
            </p>
            <div class="cold-actions">
              <button type="button" class="btn-outline" @click="setView('hot')">去看热门</button>
              <button type="button" class="btn-outline" @click="setView('time')">按时间浏览</button>
            </div>
          </div>
          <div v-else class="knowledge-grid">
            <router-link
              v-for="(row, idx) in list"
              :key="rowKey(row, idx)"
              :to="knowledgeDetailHref(row, idx)"
              class="k-card"
            >
              <div class="k-card-cover">
                <img
                  v-if="row.cover"
                  :src="resolveMediaUrl(row.cover)"
                  alt=""
                  loading="lazy"
                />
                <div v-else class="k-card-cover-placeholder" aria-hidden="true">消防科普</div>
              </div>
              <div class="k-card-body">
                <div class="k-card-tags">
                  <span v-if="row.recommendSourceLabel" class="k-tag k-tag-source">{{ row.recommendSourceLabel }}</span>
                  <span v-if="row.categoryName" class="k-tag">{{ row.categoryName }}</span>
                </div>
                <h3 class="k-card-title">{{ row.title }}</h3>
                <p v-if="rowSummary(row)" class="k-card-summary">{{ rowSummary(row) }}</p>
                <div class="k-card-meta">
                  <span v-if="row.createTime">{{ formatDateTime(row.createTime) }}</span>
                  <span>浏览 {{ row.viewCount ?? 0 }}</span>
                  <span>赞 {{ row.likeCount ?? 0 }}</span>
                </div>
                <p v-if="currentView === 'smart' && row.sortExplain" class="sort-explain">{{ row.sortExplain }}</p>
                <p v-if="currentView === 'smart' && row.recommendReason" class="reason">推荐理由：{{ row.recommendReason }}</p>
                <span class="k-card-more">阅读全文</span>
              </div>
            </router-link>
          </div>
        </template>

        <PaginationBar
          v-if="!loading && total > 0"
          v-model="pageNum"
          class="pager-bar"
          :total="total"
          :page-size="pageSize"
          :pages="pagesFromApi > 0 ? pagesFromApi : 0"
          @current-change="load"
        />
        </KnowledgeModuleShell>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getContentList, getCategoryList } from '@/api/content'
import { getRecommendList, reportBehavior } from '@/api/recommend'
import { resolveMediaUrl } from '@/utils/resolveMediaUrl'
import { formatDateTime } from '@/utils/formatDateTime'
import PaginationBar from '@/components/PaginationBar.vue'
import KnowledgeModuleShell from '@/components/KnowledgeModuleShell.vue'

const listCrumbs = [
  { label: '首页', to: '/' },
  { label: '消防知识' },
]

/** 是否在本次会话中启用豆包：始终默认 false，不因服务端已配置密钥而自动开启；不写入 localStorage/sessionStorage，避免「记住上次勾选」导致误触大模型。 */
const route = useRoute()
const router = useRouter()

const list = ref([])
const categories = ref([])
const pageNum = ref(1)
const pageSize = ref(9)
const total = ref(0)
const pagesFromApi = ref(0)
const loading = ref(false)
const pageHint = ref('')
const query = reactive({ categoryId: '', title: '', keyword: '' })
const demoBaseline = ref([])
const demoCompareRows = ref([])
const demoMessage = ref('')
const demoTargetId = ref(0)
const useAiEnhance = ref(false)

function rowSummary(row) {
  const s = row?.summary
  if (s == null || String(s).trim() === '') return ''
  const t = String(s).replace(/\s+/g, ' ').trim()
  return t.length > 120 ? `${t.slice(0, 120)}…` : t
}

function isCategoryActive(id) {
  if (id === '' || id == null) {
    return query.categoryId === '' || query.categoryId == null
  }
  return String(query.categoryId) === String(id)
}

function selectCategory(id) {
  query.categoryId = id === '' || id == null ? '' : String(id)
  onFilterChange()
}

function onAiToggle() {
  pageNum.value = 1
  load()
}

const currentView = computed(() => {
  const v = route.query.view
  if (v === 'time' || v === 'hot' || v === 'smart') return v
  return 'smart'
})

/** 按时间 Tab：发布时间排序方向，与地址栏 timeOrder 同步 */
const timeOrder = computed(() => (route.query.timeOrder === 'asc' ? 'asc' : 'desc'))

function setView(v) {
  const q = { ...route.query, view: v }
  if (v !== 'time') {
    delete q.timeOrder
  }
  router.push({ path: '/knowledge', query: q })
}

function onTimeOrderChange(e) {
  const v = e.target?.value
  if (v !== 'asc' && v !== 'desc') return
  router.replace({ path: '/knowledge', query: { ...route.query, view: 'time', timeOrder: v } })
}

function rowKey(row, idx) {
  const id = row?.id ?? row?.contentId ?? row?.content_id
  return id != null && id !== '' ? String(id) : `i-${idx}`
}

/**
 * 使用完整字符串路径（含查询串），避免嵌套路由下对象型 `to` 解析异常导致链接无效。
 * 智能推荐 Tab 附带 ref、rank 供详情页埋点。
 */
function knowledgeDetailHref(row, idx) {
  const rawId = row?.id ?? row?.contentId ?? row?.content_id
  if (rawId == null || rawId === '') {
    return '/knowledge'
  }
  const sid = String(rawId).trim()
  const pathSeg = encodeURIComponent(sid)
  if (currentView.value === 'smart') {
    const rank = Number(idx) + 1
    return `/knowledge/${pathSeg}?ref=rec&rank=${encodeURIComponent(String(rank))}`
  }
  return `/knowledge/${pathSeg}`
}

function onFilterChange() {
  pageNum.value = 1
  load()
}

function search() {
  pageNum.value = 1
  load()
}

async function load() {
  loading.value = true
  pageHint.value = ''
  try {
    const view = currentView.value
    if (view === 'smart') {
      const data = await getRecommendList({
        pageNum: pageNum.value,
        pageSize: pageSize.value,
        keyword: query.keyword?.trim() || undefined,
        ...(useAiEnhance.value ? { useAi: true } : {}),
      })
      list.value = data.list ?? data.records ?? []
      total.value = Number(data.total) || 0
      pagesFromApi.value = Number(data.pages) || 0
      pageHint.value = data.pageHint ?? ''
      if (data.pageNum != null) pageNum.value = data.pageNum
      if (!demoTargetId.value && list.value.length) {
        demoTargetId.value = Number(list.value[0].id) || 0
      }
    } else {
      const sortBy = view === 'hot' ? 'hot' : 'latest'
      const listParams = {
        pageNum: pageNum.value,
        pageSize: pageSize.value,
        categoryId: query.categoryId || undefined,
        title: query.title || undefined,
        sortBy,
      }
      if (view === 'time') {
        listParams.timeOrder = timeOrder.value
      }
      const data = await getContentList(listParams)
      const records = data.records ?? data.list ?? []
      const totalVal = Number(data.total ?? data.totalCount ?? 0)
      if (records.length === 0 && totalVal > 0 && pageNum.value > 1 && (pageNum.value - 1) * pageSize.value >= totalVal) {
        pageNum.value -= 1
        return load()
      }
      list.value = records
      total.value = totalVal
      pagesFromApi.value = 0
    }
  } catch (e) {
    console.error(e)
    const msg = typeof e === 'string' ? e : e?.message || '加载失败'
    alert(msg)
    list.value = []
    total.value = 0
    pagesFromApi.value = 0
  } finally {
    loading.value = false
    try {
      window.scrollTo({ top: 0, behavior: 'smooth' })
    } catch (_) {
      window.scrollTo(0, 0)
    }
  }
}

function captureDemoBaseline() {
  demoBaseline.value = (list.value || []).slice(0, 5).map((row, idx) => ({
    id: row.id,
    title: row.title,
    rank: idx + 1,
  }))
  demoCompareRows.value = []
  demoMessage.value = `已记录基线 Top${demoBaseline.value.length}`
}

async function runDemoBoost() {
  if (!demoTargetId.value) {
    demoMessage.value = '请先选择要强化的内容'
    return
  }
  if (!demoBaseline.value.length) {
    captureDemoBaseline()
  }
  try {
    loading.value = true
    demoMessage.value = '正在注入行为并刷新推荐...'
    pageNum.value = 1
    for (let i = 0; i < 3; i += 1) {
      await reportBehavior({ behaviorType: 'VIEW', targetType: 'CONTENT', targetId: demoTargetId.value })
    }
    await reportBehavior({ behaviorType: 'LIKE', targetType: 'CONTENT', targetId: demoTargetId.value })
    await reportBehavior({ behaviorType: 'COLLECT', targetType: 'CONTENT', targetId: demoTargetId.value })
    await load()
    buildDemoCompareRows()
    demoMessage.value = '对比完成：已展示注入行为前后 Top5 排名变化'
  } catch (e) {
    demoMessage.value = `演示失败：${String(e || '')}`
  } finally {
    loading.value = false
  }
}

function buildDemoCompareRows() {
  const afterTop = (list.value || []).slice(0, 5)
  const afterRankMap = new Map()
  afterTop.forEach((row, idx) => {
    afterRankMap.set(Number(row.id), idx + 1)
  })
  demoCompareRows.value = demoBaseline.value.map((b) => {
    const afterRank = afterRankMap.get(Number(b.id)) || null
    const delta = afterRank ? b.rank - afterRank : -99
    const beforeText = `第 ${b.rank} 位`
    const afterText = afterRank ? `第 ${afterRank} 位` : '跌出 Top5'
    let deltaText = '（排名无变化）'
    if (delta > 0) deltaText = `（上升 ${delta} 位）`
    if (delta < 0 && delta > -99) deltaText = `（下降 ${Math.abs(delta)} 位）`
    if (delta <= -99) deltaText = '（跌出 Top5）'
    return {
      id: b.id,
      title: b.title,
      beforeText,
      afterText,
      delta,
      deltaText,
    }
  })
}

async function loadCategories() {
  try {
    const data = await getCategoryList()
    categories.value = Array.isArray(data) ? data : (data.list ?? [])
  } catch (_) {}
}

watch(
  () => [route.name, route.query.view, route.query.timeOrder],
  () => {
    if (route.name !== 'KnowledgeList') return
    const v = route.query.view
    if (!v || (v !== 'time' && v !== 'hot' && v !== 'smart')) return
    pageNum.value = 1
    load()
  },
  { immediate: true, flush: 'post' },
)

onMounted(() => {
  loadCategories()
  if (!route.query.view) {
    router.replace({ path: '/knowledge', query: { ...route.query, view: 'smart' } })
  }
})
</script>

<style scoped>
.knowledge-page {
  width: 100%;
  max-width: var(--client-content-max);
  margin: 0 auto;
  padding: 0 clamp(16px, 2.5vw, 40px) 28px;
  box-sizing: border-box;
}

.knowledge-layout {
  display: grid;
  grid-template-columns: 1fr;
  gap: 20px;
  align-items: start;
}

@media (min-width: 900px) {
  .knowledge-layout {
    /* 侧栏固定约 260px，主栏吃掉剩余全部宽度 */
    grid-template-columns: minmax(220px, 260px) minmax(0, 1fr);
    gap: clamp(20px, 2vw, 36px);
  }
}

.knowledge-sidebar {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.sidebar-card {
  background: var(--client-surface);
  border-radius: var(--client-radius);
  box-shadow: var(--client-shadow);
  border: 1px solid rgba(59, 130, 246, 0.12);
  padding: 16px 18px;
}

.sidebar-title {
  margin: 0 0 12px;
  font-size: 0.9375rem;
  font-weight: 700;
  color: var(--client-text);
  letter-spacing: 0.02em;
}

.cat-nav {
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-height: 320px;
  overflow-y: auto;
}

.cat-item {
  display: block;
  width: 100%;
  text-align: left;
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px solid transparent;
  background: rgba(59, 130, 246, 0.06);
  color: var(--client-text);
  font-size: 0.875rem;
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s, color 0.15s;
}

.cat-item:hover {
  background: var(--client-accent-soft);
  border-color: rgba(59, 130, 246, 0.2);
}

.cat-item.active {
  background: var(--client-accent-soft);
  border-color: var(--client-primary);
  color: var(--client-primary-hover);
  font-weight: 600;
}

.data-source {
  padding: 0;
  overflow: hidden;
}

.data-source-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 14px 18px;
  font-weight: 700;
  font-size: 0.875rem;
  color: var(--client-text);
  cursor: pointer;
  list-style: none;
}

.data-source-summary::-webkit-details-marker {
  display: none;
}

.data-source-summary::after {
  content: '▾';
  flex-shrink: 0;
  color: var(--client-muted);
  font-size: 0.75rem;
  transition: transform 0.15s;
}

.data-source[open] .data-source-summary::after {
  transform: rotate(180deg);
}

.data-source-body {
  padding: 0 18px 16px;
  font-size: 0.8125rem;
  line-height: 1.6;
  color: var(--client-muted);
  border-top: 1px solid rgba(59, 130, 246, 0.1);
}

.data-source-body p {
  margin: 12px 0 8px;
}

.data-source-body ul {
  margin: 0;
  padding-left: 1.1rem;
}

.data-source-body li {
  margin: 6px 0;
}

.data-source-body code {
  font-size: 0.75rem;
  padding: 1px 5px;
  border-radius: 4px;
  background: rgba(59, 130, 246, 0.08);
  color: var(--client-primary-hover);
}

.knowledge-main {
  background: var(--client-surface);
  border-radius: var(--client-radius);
  box-shadow: var(--client-shadow);
  border: 1px solid rgba(59, 130, 246, 0.12);
  padding: 20px 22px 28px;
  min-width: 0;
}

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
  margin-bottom: 12px;
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

.hint-details {
  margin-bottom: 14px;
  border: 1px solid rgba(59, 130, 246, 0.15);
  border-radius: 10px;
  background: rgba(224, 242, 254, 0.35);
}

.hint-details summary {
  padding: 10px 14px;
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--client-text);
  cursor: pointer;
  list-style: none;
}

.hint-details summary::-webkit-details-marker {
  display: none;
}

.view-hint {
  font-size: 0.8125rem;
  color: var(--client-muted);
  margin: 0;
  padding: 0 14px 12px;
  line-height: 1.55;
}

.page-hint {
  margin-bottom: 14px;
  padding: 12px 14px;
  background: var(--client-accent-soft);
  border: 1px solid rgba(59, 130, 246, 0.25);
  border-radius: 10px;
  font-size: 0.8125rem;
  line-height: 1.5;
  color: var(--client-primary-hover);
}

.demo-panel {
  margin: 0 0 16px;
  padding: 14px 16px;
  border: 1px solid rgba(59, 130, 246, 0.15);
  border-radius: 10px;
  background: linear-gradient(180deg, rgba(224, 242, 254, 0.5) 0%, var(--client-surface) 100%);
}

.demo-panel-title {
  margin: 0 0 8px;
  font-size: 0.9375rem;
  font-weight: 700;
  color: var(--client-text);
}

.demo-tip {
  margin: 0 0 10px;
  color: var(--client-muted);
  font-size: 0.8125rem;
  line-height: 1.5;
}

.demo-tip--sub {
  margin-top: -4px;
  font-size: 0.75rem;
  line-height: 1.55;
}

.demo-controls {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
}

.demo-controls button {
  padding: 8px 14px;
  border-radius: 8px;
  border: 1px solid rgba(59, 130, 246, 0.35);
  background: var(--client-surface);
  color: var(--client-primary-hover);
  font-size: 0.8125rem;
  cursor: pointer;
}

.demo-controls button:hover:not(:disabled) {
  background: var(--client-accent-soft);
}

.demo-controls button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.demo-controls select {
  min-width: 200px;
  padding: 8px 10px;
  border: 1px solid rgba(59, 130, 246, 0.2);
  border-radius: 8px;
  font-size: 0.8125rem;
  background: var(--client-surface);
  color: var(--client-text);
}

.demo-message {
  margin: 0 0 8px;
  color: var(--client-text);
  font-size: 0.8125rem;
}

.demo-result {
  margin: 0;
  padding-left: 1.1rem;
  color: var(--client-text);
  font-size: 0.8125rem;
}

.demo-result li {
  margin: 4px 0;
}

.delta {
  margin-left: 6px;
}

.delta.up {
  color: #15803d;
}

.delta.down {
  color: #dc2626;
}

.ph-label {
  display: block;
  font-weight: 700;
  margin-bottom: 4px;
  font-size: 0.75rem;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  opacity: 0.85;
}

.ph-text {
  margin: 0;
}

.toolbar {
  margin-bottom: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.toolbar .title-search,
.toolbar select {
  padding: 10px 12px;
  border: 1px solid rgba(59, 130, 246, 0.2);
  border-radius: 10px;
  font-size: 0.875rem;
  background: var(--client-surface);
  color: var(--client-text);
}

.toolbar .title-search {
  min-width: 200px;
  flex: 1;
}

.toolbar .title-search.kw {
  min-width: 240px;
}

.btn-query {
  padding: 10px 20px;
  background: var(--client-primary);
  color: #fff;
  border: none;
  border-radius: 10px;
  cursor: pointer;
  font-size: 0.875rem;
  font-weight: 600;
  transition: background 0.15s;
}

.btn-query:hover {
  background: var(--client-primary-hover);
}

.ai-toggle {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 0.8125rem;
  color: var(--client-muted);
  cursor: pointer;
  user-select: none;
  white-space: nowrap;
}

.ai-toggle input {
  cursor: pointer;
}

.filter-note {
  font-size: 0.75rem;
  color: var(--client-muted);
  margin: 0 0 14px;
  line-height: 1.5;
}

.page-size {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 0.8125rem;
  color: var(--client-muted);
}

.page-size select {
  padding: 6px 8px;
  border: 1px solid rgba(59, 130, 246, 0.2);
  border-radius: 8px;
  background: var(--client-surface);
}

.time-order {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 0.8125rem;
  color: var(--client-muted);
  white-space: nowrap;
}

.time-order select {
  padding: 10px 12px;
  border: 1px solid rgba(59, 130, 246, 0.2);
  border-radius: 10px;
  background: var(--client-surface);
  font-size: 0.875rem;
}

.result-count {
  margin: 0 0 14px;
  font-size: 0.8125rem;
  color: var(--client-muted);
}

.result-count strong {
  color: var(--client-primary);
  font-weight: 700;
}

.hint {
  color: var(--client-muted);
  margin: 16px 0;
}

.loading-line {
  text-align: center;
  padding: 32px;
}

.cold-start {
  padding: 36px 20px;
  text-align: center;
  background: rgba(59, 130, 246, 0.04);
  border: 1px dashed rgba(59, 130, 246, 0.25);
  border-radius: var(--client-radius);
  margin-bottom: 16px;
}

.cold-start h3 {
  margin: 0 0 10px;
  font-size: 1.0625rem;
  color: var(--client-text);
}

.cold-start p {
  margin: 0 0 18px;
  font-size: 0.875rem;
  color: var(--client-muted);
  line-height: 1.6;
  text-align: left;
  max-width: 520px;
  margin-left: auto;
  margin-right: auto;
}

.cold-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
  flex-wrap: wrap;
}

.btn-outline {
  padding: 10px 18px;
  background: var(--client-surface);
  border: 1px solid var(--client-primary);
  color: var(--client-primary);
  border-radius: 10px;
  cursor: pointer;
  font-size: 0.875rem;
  font-weight: 500;
}

.btn-outline:hover {
  background: var(--client-accent-soft);
  color: var(--client-primary-hover);
}

.knowledge-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 18px;
}

@media (min-width: 640px) {
  .knowledge-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (min-width: 1200px) {
  .knowledge-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 20px;
  }
}

.k-card {
  display: flex;
  flex-direction: column;
  min-height: 100%;
  text-decoration: none;
  color: inherit;
  border-radius: var(--client-radius);
  overflow: hidden;
  border: 1px solid rgba(59, 130, 246, 0.12);
  background: var(--client-surface);
  box-shadow: 0 2px 12px rgba(59, 130, 246, 0.06);
  transition: transform 0.2s, box-shadow 0.2s, border-color 0.2s;
}

.k-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 12px 32px rgba(59, 130, 246, 0.15);
  border-color: rgba(59, 130, 246, 0.28);
}

.k-card-cover {
  position: relative;
  aspect-ratio: 16 / 9;
  background: linear-gradient(145deg, var(--client-accent-soft) 0%, rgba(59, 130, 246, 0.08) 100%);
  overflow: hidden;
}

.k-card-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.35s ease;
}

.k-card:hover .k-card-cover img {
  transform: scale(1.05);
}

.k-card-cover-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--client-primary);
  opacity: 0.65;
}

.k-card-body {
  padding: 14px 16px 16px;
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
}

.k-card-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 8px;
}

.k-tag {
  font-size: 0.6875rem;
  font-weight: 600;
  padding: 3px 8px;
  border-radius: 6px;
  background: rgba(59, 130, 246, 0.1);
  color: var(--client-primary-hover);
}

.k-tag-source {
  background: rgba(14, 165, 233, 0.15);
  color: #0369a1;
}

.k-card-title {
  margin: 0 0 8px;
  font-size: 1rem;
  font-weight: 700;
  line-height: 1.4;
  color: var(--client-text);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.k-card:hover .k-card-title {
  color: var(--client-primary);
}

.k-card-summary {
  margin: 0 0 10px;
  font-size: 0.8125rem;
  line-height: 1.55;
  color: var(--client-muted);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  flex: 1;
}

.k-card-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  font-size: 0.75rem;
  color: var(--client-muted);
  margin-bottom: 6px;
}

.sort-explain {
  margin: 4px 0 0;
  font-size: 0.75rem;
  color: var(--client-muted);
  line-height: 1.45;
}

.reason {
  margin: 6px 0 0;
  font-size: 0.8125rem;
  color: var(--client-text);
  line-height: 1.45;
  opacity: 0.92;
}

.k-card-more {
  margin-top: auto;
  padding-top: 10px;
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--client-primary);
}

.k-card:hover .k-card-more {
  color: var(--client-primary-hover);
}

.pager-bar {
  margin-top: 22px;
}
</style>
