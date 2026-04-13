<template>
  <KnowledgeModuleShell v-if="detail" :crumbs="crumbs">
    <article class="eq-detail">
      <div class="eq-detail-toolbar">
        <router-link to="/equipment" class="eq-back">
          <svg class="eq-back-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
            <line x1="19" y1="12" x2="5" y2="12" />
            <polyline points="12 19 5 12 12 5" />
          </svg>
          返回列表
        </router-link>
      </div>

      <div class="eq-layout">
        <div class="eq-main">
          <!-- 图示轮播 -->
          <section v-if="gallerySlides.length" class="eq-card eq-carousel-card">
            <div class="eq-main-image-wrap">
              <img
                :src="gallerySlides[currentSlide]"
                :alt="detail.name"
                class="eq-main-image"
                loading="lazy"
                @error="onMainImgErr"
                @click="openZoom"
              />
              <button type="button" class="eq-zoom-btn" @click="openZoom">
                <svg class="eq-ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                  <circle cx="11" cy="11" r="7" />
                  <path d="M21 21l-4.35-4.35M11 8v6M8 11h6" stroke-linecap="round" />
                </svg>
                放大
              </button>
            </div>
            <div class="eq-thumbs" role="tablist" aria-label="图示缩略图">
              <button
                v-for="(url, i) in gallerySlides"
                :key="i"
                type="button"
                role="tab"
                class="eq-thumb"
                :class="{ 'is-active': currentSlide === i }"
                :aria-selected="currentSlide === i"
                @click="currentSlide = i"
              >
                <img :src="url" alt="" loading="lazy" @error="hideThumb($event)" />
              </button>
            </div>
          </section>

          <Teleport to="body">
            <Transition name="eq-fade">
              <div v-if="zoomOpen" class="eq-zoom-overlay" role="dialog" aria-modal="true" aria-label="图片预览" @click="zoomOpen = false">
                <div class="eq-zoom-panel" @click.stop>
                  <button type="button" class="eq-zoom-close" aria-label="关闭" @click="zoomOpen = false">×</button>
                  <div class="eq-zoom-img-wrap">
                    <img
                      :src="gallerySlides[currentSlide]"
                      alt=""
                      class="eq-zoom-img"
                      :style="{ transform: `scale(${zoomLevel})` }"
                      @error="onMainImgErr"
                    />
                  </div>
                  <div class="eq-zoom-bar">
                    <button type="button" class="eq-zoom-ctl" @click="zoomLevel = Math.max(1, +(zoomLevel - 0.2).toFixed(2))">−</button>
                    <span class="eq-zoom-pct">{{ Math.round(zoomLevel * 100) }}%</span>
                    <button type="button" class="eq-zoom-ctl" @click="zoomLevel = Math.min(3, +(zoomLevel + 0.2).toFixed(2))">+</button>
                  </div>
                </div>
              </div>
            </Transition>
          </Teleport>

          <!-- 简介 -->
          <section class="eq-card eq-hero-card">
            <h1 class="eq-title">{{ detail.name }}</h1>
            <div class="eq-meta">
              <span v-if="detail.typeName" class="eq-type-pill">{{ detail.typeName }}</span>
            </div>
            <p v-if="detail.summary" class="eq-lead">{{ detail.summary }}</p>
          </section>

          <!-- 使用步骤 -->
          <section v-if="parsedSteps && parsedSteps.length" class="eq-card">
            <h2 class="eq-section-title">使用步骤</h2>
            <div class="eq-steps-grid">
              <div v-for="(step, index) in parsedSteps" :key="index" class="eq-step-card">
                <div class="eq-step-num">{{ index + 1 }}</div>
                <div class="eq-step-body">
                  <h3 class="eq-step-title">{{ step.title }}</h3>
                  <p v-if="step.description" class="eq-step-desc">{{ step.description }}</p>
                </div>
              </div>
            </div>
          </section>
          <section v-else-if="detail.usageSteps" class="eq-card">
            <h2 class="eq-section-title">使用步骤</h2>
            <div class="eq-prose-block">
              <pre class="eq-pre">{{ detail.usageSteps }}</pre>
            </div>
          </section>

          <!-- 检查要点 -->
          <section v-if="checklistPoints.length" class="eq-card">
            <h2 class="eq-section-title">检查要点</h2>
            <div class="eq-check-grid">
              <label v-for="(point, index) in checklistPoints" :key="index" class="eq-check-item">
                <input v-model="checkState[index]" type="checkbox" class="eq-check-input" />
                <span class="eq-check-label">{{ point }}</span>
              </label>
            </div>
          </section>
          <section v-else-if="detail.checkPoints" class="eq-card">
            <h2 class="eq-section-title">检查要点</h2>
            <div class="eq-prose-block">
              <pre class="eq-pre">{{ detail.checkPoints }}</pre>
            </div>
          </section>

          <!-- 常见故障 -->
          <section v-if="parsedFaults && parsedFaults.length" class="eq-card">
            <h2 class="eq-section-title">常见故障解决</h2>
            <div class="eq-faq-list">
              <div v-for="(item, index) in parsedFaults" :key="index" class="eq-faq-item">
                <button type="button" class="eq-faq-head" @click="toggleFaq(index)">
                  <span class="eq-faq-q">Q：{{ item.question }}</span>
                  <svg
                    class="eq-faq-chevron"
                    :class="{ 'is-open': expandedFaq[index] }"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="2"
                    aria-hidden="true"
                  >
                    <path d="M6 9l6 6 6-6" stroke-linecap="round" stroke-linejoin="round" />
                  </svg>
                </button>
                <Transition name="eq-slide">
                  <div v-show="expandedFaq[index]" class="eq-faq-body">
                    <p>A：{{ item.answer }}</p>
                  </div>
                </Transition>
              </div>
            </div>
          </section>
          <section v-else-if="detail.faultSolution" class="eq-card">
            <h2 class="eq-section-title">常见故障解决</h2>
            <div class="eq-prose-block">
              <pre class="eq-pre">{{ detail.faultSolution }}</pre>
            </div>
          </section>
        </div>

        <aside class="eq-sidebar">
          <div class="eq-card eq-side-card">
            <h2 class="eq-side-title">器材信息</h2>
            <dl class="eq-spec-list">
              <div v-if="detail.typeName" class="eq-spec-row">
                <dt>类型</dt>
                <dd>{{ detail.typeName }}</dd>
              </div>
              <div v-if="formattedCreateTime" class="eq-spec-row">
                <dt>收录时间</dt>
                <dd>{{ formattedCreateTime }}</dd>
              </div>
            </dl>
          </div>

          <div class="eq-card eq-side-actions">
            <button type="button" class="eq-btn eq-btn--primary" :class="{ 'is-on': isCollected }" @click="toggleCollect">
              <svg class="eq-ico eq-heart" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                <path
                  d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"
                />
              </svg>
              {{ isCollected ? '已收藏' : '收藏' }}
            </button>
            <div class="eq-share-wrap">
              <button type="button" class="eq-btn eq-btn--outline" @click="showShareMenu = !showShareMenu">
                <svg class="eq-ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                  <circle cx="18" cy="5" r="3" />
                  <circle cx="6" cy="12" r="3" />
                  <circle cx="18" cy="19" r="3" />
                  <path d="M8.59 13.51l6.83 3.98M15.41 6.51l-6.82 4.01" />
                </svg>
                分享
              </button>
              <Transition name="eq-fade">
                <div v-if="showShareMenu" class="eq-share-menu">
                  <button type="button" class="eq-share-item" @click="copyPageLink">复制页面链接</button>
                </div>
              </Transition>
            </div>
          </div>

          <div v-if="relatedSidebar.length" class="eq-card eq-side-card">
            <h2 class="eq-side-title">相关器材</h2>
            <ul class="eq-related-list">
              <li v-for="item in relatedSidebar" :key="item.id">
                <router-link :to="`/equipment/${item.id}`" class="eq-related-row">
                  <div class="eq-related-thumb">
                    <img v-if="item.cover" :src="resolveMediaUrl(item.cover)" :alt="item.name" loading="lazy" @error="hideImg" />
                    <span v-else class="eq-related-ph" aria-hidden="true" />
                  </div>
                  <div class="eq-related-text">
                    <span class="eq-related-name">{{ item.name }}</span>
                    <span v-if="item.typeName" class="eq-related-sub">{{ item.typeName }}</span>
                  </div>
                  <svg class="eq-related-arrow" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                    <path d="M9 18l6-6-6-6" stroke-linecap="round" stroke-linejoin="round" />
                  </svg>
                </router-link>
              </li>
            </ul>
          </div>
        </aside>
      </div>

      <!-- 底部相关推荐 -->
      <section v-if="relatedGrid.length" class="eq-card eq-related-section">
        <h2 class="eq-section-title">相关推荐</h2>
        <div class="eq-related-grid">
          <article v-for="item in relatedGrid" :key="'g-' + item.id" class="eq-related-card">
            <router-link :to="`/equipment/${item.id}`" class="eq-related-card-link">
              <div class="eq-related-card-img">
                <img v-if="item.cover" :src="resolveMediaUrl(item.cover)" :alt="item.name" loading="lazy" @error="hideImg" />
                <span v-else class="eq-related-card-ph" />
                <span v-if="item.typeName" class="eq-related-card-badge">{{ item.typeName }}</span>
              </div>
              <div class="eq-related-card-body">
                <h3 class="eq-related-card-title">{{ item.name }}</h3>
                <p v-if="item.summary" class="eq-related-card-desc">{{ item.summary }}</p>
                <span class="eq-related-card-cta">
                  查看详情
                  <svg class="eq-ico-sm" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                    <path d="M9 18l6-6-6-6" stroke-linecap="round" stroke-linejoin="round" />
                  </svg>
                </span>
              </div>
            </router-link>
          </article>
        </div>
      </section>

      <p v-if="toast" class="eq-toast" role="status">{{ toast }}</p>
    </article>
  </KnowledgeModuleShell>

  <KnowledgeModuleShell v-else-if="loadError" :crumbs="crumbsError" narrow>
    <div class="eq-state eq-state--error">
      <p>{{ loadError }}</p>
      <router-link to="/equipment" class="eq-state-link">返回器材列表</router-link>
    </div>
  </KnowledgeModuleShell>

  <KnowledgeModuleShell v-else :crumbs="crumbsLoading" narrow>
    <div class="eq-state">
      <div class="eq-spinner" />
      <p>加载中…</p>
    </div>
  </KnowledgeModuleShell>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { collectEquipment, getEquipmentDetail, getEquipmentList, uncollectEquipment } from '@/api/equipment'
import { resolveMediaUrl } from '@/utils/resolveMediaUrl'
import KnowledgeModuleShell from '@/components/KnowledgeModuleShell.vue'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const userStore = useUserStore()
const detail = ref(null)
const loadError = ref('')
const currentSlide = ref(0)
const zoomOpen = ref(false)
const zoomLevel = ref(1)
const showShareMenu = ref(false)
const expandedFaq = ref({})
const relatedList = ref([])
const toast = ref('')
const checkState = ref({})

const id = computed(() => route.params.id)

const isCollected = computed(() => !!detail.value?.collected)

const coverUrl = computed(() => resolveMediaUrl(detail.value?.cover || ''))

const imageUrls = computed(() => parseImageUrls(detail.value?.images).map((u) => resolveMediaUrl(u)))

const gallerySlides = computed(() => {
  const urls = imageUrls.value.filter(Boolean)
  const cu = coverUrl.value
  if (cu && !urls.some((u) => u === cu)) return [cu, ...urls]
  if (urls.length) return urls
  if (cu) return [cu]
  return []
})

const parsedSteps = computed(() => parseUsageSteps(detail.value?.usageSteps))

const checklistPoints = computed(() => parseCheckPoints(detail.value?.checkPoints))

const parsedFaults = computed(() => parseFaultSolution(detail.value?.faultSolution))

const formattedCreateTime = computed(() => {
  const t = detail.value?.createTime
  if (t == null || t === '') return ''
  const s = String(t).replace('T', ' ')
  return s.length > 16 ? s.slice(0, 16) : s
})

const relatedSidebar = computed(() => relatedList.value.slice(0, 4))
const relatedGrid = computed(() => relatedList.value.slice(0, 8))

const crumbs = computed(() => {
  const base = [
    { label: '首页', to: '/home' },
    { label: '消防器材', to: '/equipment' },
  ]
  if (!detail.value) return base
  const title = detail.value.name || '详情'
  const short = title.length > 22 ? `${title.slice(0, 22)}…` : title
  return [...base, { label: short, titleAttr: title }]
})

const crumbsError = computed(() => [
  { label: '首页', to: '/home' },
  { label: '消防器材', to: '/equipment' },
  { label: '加载失败', to: '' },
])

const crumbsLoading = computed(() => [
  { label: '首页', to: '/home' },
  { label: '消防器材', to: '/equipment' },
  { label: '加载中…', to: '' },
])

watch(checklistPoints, (pts) => {
  const next = {}
  pts.forEach((_, i) => {
    next[i] = checkState.value[i] ?? false
  })
  checkState.value = next
})

watch(gallerySlides, (slides) => {
  if (currentSlide.value >= slides.length) currentSlide.value = 0
})

watch(detail, () => {
  expandedFaq.value = {}
})

watch(zoomOpen, (open) => {
  if (open) zoomLevel.value = 1
})

function parseImageUrls(images) {
  if (images == null || !String(images).trim()) return []
  const s = String(images).trim()
  try {
    const arr = JSON.parse(s)
    if (Array.isArray(arr)) {
      return arr.map((x) => String(x).trim()).filter(Boolean)
    }
  } catch {
    /* 非 JSON 则按分隔符切分 */
  }
  return s.split(/[\n,，;；]/).map((x) => x.trim()).filter(Boolean)
}

function parseUsageSteps(raw) {
  if (raw == null || !String(raw).trim()) return null
  const s = String(raw).trim()
  try {
    const j = JSON.parse(s)
    if (Array.isArray(j) && j.length) {
      return j.map((item, i) => {
        if (typeof item === 'string') return { title: item, description: '' }
        return {
          title: String(item.title || item.name || `步骤 ${i + 1}`),
          description: String(item.description || item.desc || '').trim(),
        }
      })
    }
  } catch {
    /* 文本解析 */
  }
  const lines = s.split(/\r?\n/).map((l) => l.trim()).filter(Boolean)
  const numRe = /^(\d+)[\.、．]\s*(.*)$/
  const steps = []
  let cur = null
  for (const line of lines) {
    const m = line.match(numRe)
    if (m) {
      if (cur) steps.push(cur)
      cur = { title: (m[2] || m[1]).trim(), description: '' }
    } else if (cur) {
      cur.description += (cur.description ? '\n' : '') + line
    } else {
      steps.push({ title: line, description: '' })
    }
  }
  if (cur) steps.push(cur)
  return steps.length ? steps : null
}

function parseCheckPoints(raw) {
  if (raw == null || !String(raw).trim()) return []
  const s = String(raw).trim()
  try {
    const j = JSON.parse(s)
    if (Array.isArray(j)) {
      return j.map((x) => (typeof x === 'string' ? x : x.text || x.label || '')).filter(Boolean)
    }
  } catch {
    /* 文本行 */
  }
  return s.split(/\r?\n/).map((l) => l.replace(/^[-*•]\s*/, '').trim()).filter(Boolean)
}

function parseFaultSolution(raw) {
  if (raw == null || !String(raw).trim()) return null
  const s = String(raw).trim()
  try {
    const j = JSON.parse(s)
    if (Array.isArray(j) && j.length) {
      return j
        .map((item) => ({
          question: String(item.question || item.q || '').trim(),
          answer: String(item.answer || item.a || '').trim(),
        }))
        .filter((x) => x.question || x.answer)
    }
  } catch {
    /* 文本 Q&A */
  }
  const parts = s.split(/\n(?=[Q问]\s*[:：])/)
  const out = []
  const qaRe = /^(?:Q|问)\s*[:：]\s*([\s\S]+?)(?:\n+(?:A|答)\s*[:：]\s*([\s\S]*))?$/im
  for (const block of parts.map((b) => b.trim()).filter(Boolean)) {
    const m = block.match(qaRe)
    if (m) {
      out.push({ question: m[1].trim(), answer: (m[2] || '').trim() })
    }
  }
  return out.length ? out : null
}

function toggleFaq(index) {
  expandedFaq.value = { ...expandedFaq.value, [index]: !expandedFaq.value[index] }
}

function openZoom() {
  if (!gallerySlides.value.length) return
  zoomOpen.value = true
}

function hideThumb(e) {
  const btn = e.target?.closest?.('.eq-thumb')
  if (btn instanceof HTMLElement) btn.style.display = 'none'
}

function onMainImgErr(e) {
  const t = e.target
  if (t instanceof HTMLImageElement) {
    t.style.opacity = '0.35'
    t.alt = '图片加载失败'
  }
}

function hideImg(e) {
  const t = e.target
  if (t instanceof HTMLImageElement) t.style.display = 'none'
}

async function toggleCollect() {
  if (!detail.value?.id) return
  if (!userStore.isLoggedIn) {
    alert('请先登录后再收藏')
    return
  }
  try {
    if (detail.value.collected) {
      await uncollectEquipment(detail.value.id)
      detail.value = { ...detail.value, collected: false }
    } else {
      await collectEquipment(detail.value.id)
      detail.value = { ...detail.value, collected: true }
    }
  } catch (e) {
    console.error(e)
    alert(e?.message || '收藏操作失败')
  }
}

let toastTimer
function showToast(msg) {
  toast.value = msg
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => {
    toast.value = ''
  }, 2200)
}

async function copyPageLink() {
  showShareMenu.value = false
  const url = window.location.href
  try {
    await navigator.clipboard.writeText(url)
    showToast('链接已复制到剪贴板')
  } catch {
    showToast('复制失败，请手动复制地址栏链接')
  }
}

async function loadRelated() {
  relatedList.value = []
  if (!detail.value?.typeId) return
  try {
    const data = await getEquipmentList({
      pageNum: 1,
      pageSize: 16,
      typeId: detail.value.typeId,
    })
    const rows = data.records ?? data.list ?? []
    const cur = String(detail.value.id)
    relatedList.value = rows.filter((r) => String(r.id) !== cur)
  } catch {
    relatedList.value = []
  }
}

async function load() {
  detail.value = null
  loadError.value = ''
  currentSlide.value = 0
  try {
    detail.value = await getEquipmentDetail(id.value)
    if (detail.value == null) {
      loadError.value = '器材不存在或已下架'
    } else {
      await loadRelated()
    }
  } catch (e) {
    loadError.value = e?.message || '加载失败，器材不存在或已下架'
  }
}

watch(id, () => load())
onMounted(load)
</script>

<style scoped>
.eq-detail {
  min-width: 0;
}

.eq-detail-toolbar {
  margin-bottom: 16px;
}

.eq-back {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--client-primary);
  text-decoration: none;
}

.eq-back:hover {
  color: var(--client-primary-hover);
  text-decoration: underline;
}

.eq-back-icon {
  width: 18px;
  height: 18px;
}

/* 两栏布局（主题色来自 theme.css 变量） */
.eq-layout {
  display: grid;
  grid-template-columns: 1fr min(320px, 34%);
  gap: clamp(20px, 3vw, 28px);
  margin-bottom: 28px;
  align-items: start;
}

.eq-main {
  display: flex;
  flex-direction: column;
  gap: clamp(18px, 2.5vw, 24px);
  min-width: 0;
}

.eq-sidebar {
  display: flex;
  flex-direction: column;
  gap: 16px;
  position: sticky;
  top: 16px;
}

.eq-card {
  background: var(--client-surface);
  border-radius: var(--client-radius);
  box-shadow: var(--client-shadow);
  border: 1px solid color-mix(in srgb, var(--client-primary) 12%, transparent);
  padding: clamp(18px, 2.2vw, 22px);
}

/* 主图区居中；缩略图行单独拉满宽度 */
.eq-carousel-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-bottom: 14px;
}

/* 容器随图片比例收缩，不再强制 4:3，避免方图左右大留白 */
.eq-main-image-wrap {
  position: relative;
  display: block;
  width: fit-content;
  max-width: 100%;
  margin-inline: auto;
  margin-bottom: 12px;
  border-radius: calc(var(--client-radius) - 4px);
  overflow: hidden;
  background: linear-gradient(145deg, var(--client-accent-soft) 0%, #f8fafc 100%);
  line-height: 0;
}

.eq-main-image {
  display: block;
  width: auto;
  height: auto;
  max-width: min(560px, 100%);
  max-height: min(520px, 72vh);
  object-fit: contain;
  cursor: zoom-in;
  transition: transform 0.25s ease;
}

.eq-main-image:hover {
  transform: scale(1.02);
}

.eq-zoom-btn {
  position: absolute;
  top: 12px;
  right: 12px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border: none;
  border-radius: 8px;
  font-size: 0.8125rem;
  font-weight: 600;
  cursor: pointer;
  background: var(--client-primary);
  color: #fff;
  transition: background 0.2s;
}

.eq-zoom-btn:hover {
  background: var(--client-primary-hover);
}

.eq-thumbs {
  align-self: stretch;
  display: flex;
  gap: 10px;
  overflow-x: auto;
  padding-bottom: 4px;
  scrollbar-width: thin;
}

.eq-thumb {
  flex: 0 0 72px;
  width: 72px;
  height: 72px;
  padding: 0;
  border: 2px solid color-mix(in srgb, var(--client-primary) 10%, #e2e8f0);
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  background: #f8fafc;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.eq-thumb.is-active {
  border-color: var(--client-primary);
  box-shadow: 0 0 0 2px color-mix(in srgb, var(--client-primary) 25%, transparent);
}

.eq-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.eq-zoom-overlay {
  position: fixed;
  inset: 0;
  z-index: 2000;
  background: rgba(15, 23, 42, 0.72);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 10px;
}

/* 放大层：尽量占满视口，图应明显大于页内轮播 */
.eq-zoom-panel {
  position: relative;
  width: min(100vw - 20px, 1600px);
  max-width: calc(100vw - 20px);
  /* 占满视口高度（留边距），不再用 920px 窄框限制 */
  height: calc(100vh - 20px);
  max-height: calc(100vh - 20px);
  background: var(--client-surface);
  border-radius: var(--client-radius);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-shadow: 0 24px 48px rgba(0, 0, 0, 0.2);
}

.eq-zoom-close {
  position: absolute;
  top: 10px;
  right: 10px;
  z-index: 2;
  width: 40px;
  height: 40px;
  border: none;
  border-radius: 50%;
  font-size: 1.5rem;
  line-height: 1;
  cursor: pointer;
  background: rgba(30, 58, 95, 0.45);
  color: #fff;
  transition: background 0.2s;
}

.eq-zoom-close:hover {
  background: rgba(30, 58, 95, 0.65);
}

.eq-zoom-img-wrap {
  flex: 1;
  min-height: min(50vh, 400px);
  overflow: auto;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 8px;
  box-sizing: border-box;
  background: #0f172a;
}

.eq-zoom-img {
  /* 在可视区内尽量放大：上限接近视口，通常大于页内 480px 预览 */
  max-width: min(calc(100vw - 48px), 1400px);
  max-height: min(calc(100vh - 120px), 820px);
  width: auto;
  height: auto;
  display: block;
  transition: transform 0.2s ease;
  transform-origin: center center;
  object-fit: contain;
}

.eq-zoom-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 12px 16px;
  background: var(--client-accent-soft);
  border-top: 1px solid color-mix(in srgb, var(--client-primary) 14%, transparent);
}

.eq-zoom-ctl {
  width: 40px;
  height: 40px;
  border: none;
  border-radius: 8px;
  font-size: 1.25rem;
  cursor: pointer;
  background: var(--client-primary);
  color: #fff;
  transition: background 0.2s;
}

.eq-zoom-ctl:hover {
  background: var(--client-primary-hover);
}

.eq-zoom-pct {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--client-text);
  min-width: 3.5rem;
  text-align: center;
}

.eq-hero-card {
  box-shadow: var(--client-shadow);
}

.eq-title {
  margin: 0 0 10px;
  font-size: clamp(1.35rem, 2.5vw, 1.75rem);
  font-weight: 800;
  color: var(--client-text);
  line-height: 1.25;
  letter-spacing: -0.02em;
}

.eq-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 10px;
}

.eq-type-pill {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 999px;
  font-size: 0.8125rem;
  font-weight: 700;
  background: var(--client-accent-soft);
  color: var(--client-primary);
  border: 1px solid color-mix(in srgb, var(--client-primary) 22%, transparent);
}

.eq-lead {
  margin: 0;
  font-size: 0.9375rem;
  line-height: 1.65;
  color: var(--client-muted);
}

.eq-section-title {
  margin: 0 0 16px;
  font-size: 1.0625rem;
  font-weight: 800;
  color: var(--client-text);
  padding-bottom: 10px;
  border-bottom: 2px solid var(--client-primary);
}

.eq-steps-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 14px;
}

.eq-step-card {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  padding: 14px;
  border-radius: calc(var(--client-radius) - 4px);
  background: color-mix(in srgb, var(--client-accent-soft) 65%, #fff);
  border-left: 4px solid var(--client-primary);
  transition: transform 0.2s, box-shadow 0.2s;
}

.eq-step-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px color-mix(in srgb, var(--client-primary) 12%, transparent);
}

.eq-step-num {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 800;
  font-size: 0.9375rem;
  background: var(--client-primary);
  color: #fff;
}

.eq-step-body {
  min-width: 0;
}

.eq-step-title {
  margin: 0 0 4px;
  font-size: 0.9375rem;
  font-weight: 700;
  color: var(--client-text);
}

.eq-step-desc {
  margin: 0;
  font-size: 0.8125rem;
  line-height: 1.55;
  color: var(--client-muted);
  white-space: pre-wrap;
}

.eq-check-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 10px;
}

.eq-check-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 14px;
  border-radius: calc(var(--client-radius) - 4px);
  background: color-mix(in srgb, var(--client-accent-soft) 50%, #fff);
  cursor: pointer;
  transition: background 0.2s;
}

.eq-check-item:hover {
  background: var(--client-accent-soft);
}

.eq-check-input {
  width: 18px;
  height: 18px;
  margin-top: 2px;
  flex-shrink: 0;
  accent-color: var(--client-primary);
  cursor: pointer;
}

.eq-check-label {
  font-size: 0.875rem;
  line-height: 1.5;
  color: var(--client-text);
}

.eq-faq-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.eq-faq-item {
  border: 1px solid color-mix(in srgb, var(--client-primary) 12%, #e2e8f0);
  border-radius: calc(var(--client-radius) - 4px);
  overflow: hidden;
}

.eq-faq-head {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
  border: none;
  background: color-mix(in srgb, var(--client-accent-soft) 40%, #fff);
  cursor: pointer;
  text-align: left;
  font: inherit;
  transition: background 0.2s;
}

.eq-faq-head:hover {
  background: var(--client-accent-soft);
}

.eq-faq-q {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--client-text);
}

.eq-faq-chevron {
  width: 22px;
  height: 22px;
  flex-shrink: 0;
  color: var(--client-primary);
  transition: transform 0.25s;
}

.eq-faq-chevron.is-open {
  transform: rotate(180deg);
}

.eq-faq-body {
  padding: 14px 16px;
  font-size: 0.875rem;
  line-height: 1.65;
  color: var(--client-muted);
  background: var(--client-surface);
  border-top: 1px solid color-mix(in srgb, var(--client-primary) 10%, #e2e8f0);
}

.eq-faq-body p {
  margin: 0;
}

.eq-prose-block {
  border-radius: calc(var(--client-radius) - 4px);
  border: 1px solid color-mix(in srgb, var(--client-primary) 10%, transparent);
  background: color-mix(in srgb, var(--client-accent-soft) 35%, transparent);
  overflow: hidden;
}

.eq-pre {
  margin: 0;
  padding: 16px 18px;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 0.875rem;
  line-height: 1.65;
  color: var(--client-text);
  font-family: inherit;
}

.eq-side-title {
  margin: 0 0 12px;
  font-size: 0.9375rem;
  font-weight: 800;
  color: var(--client-text);
  padding-bottom: 8px;
  border-bottom: 2px solid var(--client-primary);
}

.eq-spec-list {
  margin: 0;
}

.eq-spec-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid #f1f5f9;
  font-size: 0.8125rem;
}

.eq-spec-row:last-child {
  border-bottom: none;
}

.eq-spec-row dt {
  margin: 0;
  color: var(--client-muted);
  font-weight: 600;
}

.eq-spec-row dd {
  margin: 0;
  text-align: right;
  color: var(--client-text);
  font-weight: 700;
}

.eq-side-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.eq-share-wrap {
  position: relative;
}

.eq-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  padding: 11px 14px;
  border-radius: 10px;
  font-size: 0.875rem;
  font-weight: 700;
  cursor: pointer;
  transition: background 0.2s, color 0.2s, border-color 0.2s;
}

.eq-btn--primary {
  border: 2px solid var(--client-primary);
  background: #fff;
  color: var(--client-primary);
}

.eq-btn--primary:hover {
  background: var(--client-primary);
  color: #fff;
}

.eq-btn--primary.is-on {
  background: var(--client-primary);
  color: #fff;
}

.eq-btn--outline {
  border: 2px solid var(--client-primary);
  background: #fff;
  color: var(--client-primary);
}

.eq-btn--outline:hover {
  background: var(--client-accent-soft);
}

.eq-ico {
  width: 18px;
  height: 18px;
}

.eq-heart {
  fill: none;
}

.eq-btn--primary.is-on .eq-heart {
  fill: currentColor;
}

.eq-ico-sm {
  width: 16px;
  height: 16px;
}

.eq-share-menu {
  margin-top: 8px;
  padding: 8px;
  border-radius: 10px;
  background: var(--client-accent-soft);
  border: 1px solid color-mix(in srgb, var(--client-primary) 14%, transparent);
}

.eq-share-item {
  display: block;
  width: 100%;
  padding: 10px 12px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: var(--client-primary);
  font-size: 0.8125rem;
  font-weight: 600;
  text-align: left;
  cursor: pointer;
  transition: background 0.2s;
}

.eq-share-item:hover {
  background: #fff;
}

.eq-related-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.eq-related-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px;
  border-radius: 10px;
  background: color-mix(in srgb, var(--client-accent-soft) 45%, #fff);
  text-decoration: none;
  color: inherit;
  transition: background 0.2s, transform 0.2s;
}

.eq-related-row:hover {
  background: var(--client-accent-soft);
  transform: translateX(4px);
}

.eq-related-thumb {
  width: 52px;
  height: 52px;
  border-radius: 8px;
  overflow: hidden;
  flex-shrink: 0;
  background: #f1f5f9;
}

.eq-related-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.eq-related-ph {
  display: block;
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, var(--client-accent-soft), #e2e8f0);
}

.eq-related-text {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.eq-related-name {
  font-size: 0.8125rem;
  font-weight: 700;
  color: var(--client-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.eq-related-sub {
  font-size: 0.75rem;
  color: var(--client-muted);
}

.eq-related-arrow {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
  color: var(--client-primary);
}

.eq-related-section {
  margin-top: 8px;
}

.eq-related-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: clamp(14px, 2vw, 20px);
}

.eq-related-card {
  border-radius: calc(var(--client-radius) - 2px);
  overflow: hidden;
  border: 1px solid color-mix(in srgb, var(--client-primary) 12%, #e2e8f0);
  transition: border-color 0.2s, box-shadow 0.2s, transform 0.2s;
  background: var(--client-surface);
}

.eq-related-card:hover {
  border-color: var(--client-primary);
  box-shadow: 0 12px 28px color-mix(in srgb, var(--client-primary) 18%, transparent);
  transform: translateY(-3px);
}

.eq-related-card-link {
  text-decoration: none;
  color: inherit;
  display: block;
  height: 100%;
}

.eq-related-card-img {
  position: relative;
  aspect-ratio: 4 / 3;
  background: linear-gradient(145deg, var(--client-accent-soft), #f8fafc);
  overflow: hidden;
}

.eq-related-card-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.35s ease;
}

.eq-related-card:hover .eq-related-card-img img {
  transform: scale(1.06);
}

.eq-related-card-ph {
  display: block;
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, var(--client-accent-soft), #e2e8f0);
}

.eq-related-card-badge {
  position: absolute;
  top: 10px;
  right: 10px;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 0.6875rem;
  font-weight: 700;
  background: var(--client-primary);
  color: #fff;
}

.eq-related-card-body {
  padding: 14px 16px 16px;
}

.eq-related-card-title {
  margin: 0 0 6px;
  font-size: 0.9375rem;
  font-weight: 800;
  color: var(--client-text);
}

.eq-related-card-desc {
  margin: 0 0 12px;
  font-size: 0.8125rem;
  line-height: 1.5;
  color: var(--client-muted);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.eq-related-card-cta {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 0.8125rem;
  font-weight: 700;
  color: var(--client-primary);
}

.eq-toast {
  position: fixed;
  bottom: 24px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 2100;
  margin: 0;
  padding: 10px 18px;
  border-radius: 999px;
  font-size: 0.8125rem;
  font-weight: 600;
  background: var(--client-text);
  color: #fff;
  box-shadow: var(--client-shadow);
}

.eq-state {
  text-align: center;
  padding: 40px 16px;
  color: var(--client-muted);
}

.eq-state--error {
  color: var(--client-text);
}

.eq-state--error p {
  margin: 0 0 16px;
}

.eq-state-link {
  color: var(--client-primary);
  font-weight: 600;
  text-decoration: none;
}

.eq-state-link:hover {
  text-decoration: underline;
}

.eq-spinner {
  width: 40px;
  height: 40px;
  margin: 0 auto 12px;
  border: 3px solid color-mix(in srgb, var(--client-primary) 25%, transparent);
  border-top-color: var(--client-primary);
  border-radius: 50%;
  animation: eqd-spin 0.75s linear infinite;
}

@keyframes eqd-spin {
  to {
    transform: rotate(360deg);
  }
}

.eq-fade-enter-active,
.eq-fade-leave-active {
  transition: opacity 0.25s ease;
}

.eq-fade-enter-from,
.eq-fade-leave-to {
  opacity: 0;
}

.eq-slide-enter-active,
.eq-slide-leave-active {
  transition: opacity 0.2s ease, max-height 0.28s ease;
}

.eq-slide-enter-from,
.eq-slide-leave-to {
  opacity: 0;
  max-height: 0;
}

@media (max-width: 1024px) {
  .eq-layout {
    grid-template-columns: 1fr;
  }

  .eq-sidebar {
    position: static;
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 16px;
  }

  .eq-side-actions {
    grid-column: span 2;
  }
}

@media (max-width: 640px) {
  .eq-sidebar {
    grid-template-columns: 1fr;
  }

  .eq-side-actions {
    grid-column: span 1;
  }

  .eq-steps-grid,
  .eq-check-grid {
    grid-template-columns: 1fr;
  }

  .eq-related-grid {
    grid-template-columns: 1fr;
  }
}
</style>
