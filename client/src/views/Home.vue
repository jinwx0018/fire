<template>
  <div class="home">
    <!-- 轮播（参考设计：16:9 + 图片 + 遮罩 + 双按钮） -->
    <section class="carousel-section" aria-label="首页推荐">
      <div class="carousel-container">
        <div class="carousel-wrapper">
          <transition name="carousel-fade" mode="out-in">
            <div :key="carouselIndex" class="carousel-slide">
              <img
                :src="carouselSlides[carouselIndex].image"
                :alt="carouselSlides[carouselIndex].title"
                class="carousel-image-bg"
                aria-hidden="true"
              />
              <img
                :src="carouselSlides[carouselIndex].image"
                :alt="carouselSlides[carouselIndex].title"
                class="carousel-image-fg"
                loading="eager"
              />
              <div class="carousel-overlay" />
              <div class="carousel-content">
                <p class="carousel-kicker">{{ carouselSlides[carouselIndex].kicker }}</p>
                <h2 class="carousel-title">{{ carouselSlides[carouselIndex].title }}</h2>
                <p class="carousel-sub">{{ carouselSlides[carouselIndex].subtitle }}</p>
                <div class="carousel-buttons">
                  <router-link class="btn-carousel primary" :to="carouselSlides[carouselIndex].link">
                    进入
                  </router-link>
                  <router-link class="btn-carousel secondary" :to="carouselSlides[carouselIndex].secondaryLink">
                    了解更多
                  </router-link>
                </div>
              </div>
            </div>
          </transition>
          <button type="button" class="carousel-nav prev" aria-label="上一张" @click.prevent="prevSlide">
            ‹
          </button>
          <button type="button" class="carousel-nav next" aria-label="下一张" @click.prevent="nextSlide">
            ›
          </button>
          <div class="carousel-dots" role="tablist">
            <button
              v-for="(_, i) in carouselSlides"
              :key="'dot-' + i"
              type="button"
              class="carousel-dot"
              :class="{ active: i === carouselIndex }"
              :aria-label="'第 ' + (i + 1) + ' 张'"
              @click="goSlide(i)"
            />
          </div>
        </div>
      </div>
    </section>

    <!-- 简介 -->
    <section class="intro-section">
      <p class="intro-text">
        聚焦消防知识、技能经验与行业资讯；登录后可收藏、发帖并体验个性化推荐。
      </p>
      <div class="intro-buttons">
        <router-link :to="{ name: 'KnowledgeList', query: { view: 'smart' } }" class="btn-large primary">
          消防知识（智能推荐）
        </router-link>
        <router-link :to="{ name: 'KnowledgeList', query: { view: 'hot' } }" class="btn-large ghost">热门知识</router-link>
      </div>
    </section>

    <!-- 核心能力（配图来自 assets/home） -->
    <section class="core-section">
      <h2 class="section-heading">核心能力</h2>
      <p class="section-sub">品质消防知识与工具，点击图片进入对应页面</p>
      <div class="abilities-grid">
        <router-link v-for="c in capabilityCards" :key="c.title" :to="c.to" class="ability-card">
          <div class="ability-img-wrap">
            <img :src="c.img" :alt="c.title" class="ability-image-bg" aria-hidden="true" />
            <img :src="c.img" :alt="c.title" class="ability-image-fg" loading="lazy" />
            <div class="ability-shade" />
          </div>
          <div class="ability-body">
            <h3>{{ c.title }}</h3>
            <p>{{ c.desc }}</p>
          </div>
        </router-link>
      </div>
    </section>

    <!-- 热门新闻（卡片栅格） -->
    <section class="news-section">
      <div class="news-header">
        <h2 class="section-heading">热门新闻</h2>
        <router-link to="/news" class="view-more">查看更多 →</router-link>
      </div>
      <p v-if="hotNewsLoading" class="muted">加载中…</p>
      <div v-else-if="hotNewsList.length" class="news-grid">
        <router-link v-for="n in hotNewsList" :key="n.id" :to="`/news/${n.id}`" class="news-card">
          <span class="news-tag">热门</span>
          <h3 class="news-card-title">{{ n.title }}</h3>
          <div class="news-meta">
            <span>{{ formatNewsTime(n.publishTime) }}</span>
            <span v-if="n.likeCount != null">👍 {{ n.likeCount }}</span>
          </div>
        </router-link>
      </div>
      <p v-else class="muted">暂无新闻，请稍后再试</p>
    </section>

    <section class="dev-hint">
      <p>
        <strong>联调提示：</strong>后端 <code>mvn spring-boot:run</code>，接口基地址
        <code>http://localhost:8088/api</code>。
      </p>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { getNewsList } from '@/api/news'
import { formatDateTime } from '@/utils/formatDateTime'
import { HOME_ASSET_FILES, homeAssetUrl } from '@/constants/homeAssets'

const carouselSlides = [
  {
    kicker: '消防科普 · 智能推荐',
    title: '智能推荐消防知识',
    subtitle: '基于浏览与兴趣，为你匹配科普内容',
    image: homeAssetUrl(HOME_ASSET_FILES.banner),
    link: { name: 'KnowledgeList', query: { view: 'smart' } },
    secondaryLink: { name: 'KnowledgeList', query: { view: 'hot' } },
  },
  {
    kicker: '平台全景',
    title: '一站式消防科普体验',
    subtitle: '知识、论坛、资讯与器材查询聚合呈现',
    image: homeAssetUrl(HOME_ASSET_FILES.banner1),
    link: { path: '/forum' },
    secondaryLink: { path: '/news' },
  },
  {
    kicker: '核心模块',
    title: '核心能力 · 快速直达',
    subtitle: '消防知识、论坛交流与热门资讯',
    image: homeAssetUrl(HOME_ASSET_FILES.banner2),
    link: { name: 'KnowledgeList', query: { view: 'smart' } },
    secondaryLink: { path: '/equipment' },
  },
  {
    kicker: '移动友好',
    title: '随时随地学习消防知识',
    subtitle: '响应式布局，手机与电脑均可流畅浏览',
    image: homeAssetUrl(HOME_ASSET_FILES.banner4),
    link: { path: '/news' },
    secondaryLink: { path: '/equipment' },
  },
]

const capabilityCards = [
  {
    title: '消防知识',
    desc: '智能推荐、热门排行，收藏与分享',
    img: homeAssetUrl(HOME_ASSET_FILES.knowledge),
    to: { name: 'KnowledgeList', query: { view: 'smart' } },
  },
  {
    title: '交流论坛',
    desc: '发帖讨论，审核通过后公开可见',
    img: homeAssetUrl(HOME_ASSET_FILES.community),
    to: { path: '/forum' },
  },
  {
    title: '器材查询',
    desc: '按类型与名称查询，图文详情',
    img: homeAssetUrl(HOME_ASSET_FILES.equipment),
    to: { path: '/equipment' },
  },
  {
    title: '消防新闻',
    desc: '行业动态与紧急资讯',
    img: homeAssetUrl(HOME_ASSET_FILES.news),
    to: { path: '/news' },
  },
]

const carouselIndex = ref(0)
let carouselTimer = null
const CAROUSEL_INTERVAL = 5000

function nextSlide() {
  carouselIndex.value = (carouselIndex.value + 1) % carouselSlides.length
  resetCarouselTimer()
}

function prevSlide() {
  carouselIndex.value = (carouselIndex.value - 1 + carouselSlides.length) % carouselSlides.length
  resetCarouselTimer()
}

function goSlide(i) {
  carouselIndex.value = i
  resetCarouselTimer()
}

function resetCarouselTimer() {
  if (carouselTimer) clearInterval(carouselTimer)
  carouselTimer = window.setInterval(nextSlide, CAROUSEL_INTERVAL)
}

const hotNewsLoading = ref(false)
const hotNewsList = ref([])

function formatNewsTime(t) {
  if (!t) return '—'
  return formatDateTime(t)
}

async function loadHotNews() {
  hotNewsLoading.value = true
  try {
    const data = await getNewsList({
      pageNum: 1,
      pageSize: 48,
      orderBy: 'publishTime',
      order: 'desc',
    })
    const records = data?.records ?? data?.list ?? []
    const sorted = [...records].sort((a, b) => {
      const la = Number(a.likeCount ?? 0)
      const lb = Number(b.likeCount ?? 0)
      if (lb !== la) return lb - la
      const ta = a.publishTime ? new Date(a.publishTime).getTime() : 0
      const tb = b.publishTime ? new Date(b.publishTime).getTime() : 0
      return tb - ta
    })
    hotNewsList.value = sorted.slice(0, 6)
  } catch {
    hotNewsList.value = []
  } finally {
    hotNewsLoading.value = false
  }
}

onMounted(() => {
  loadHotNews()
  resetCarouselTimer()
})

onUnmounted(() => {
  if (carouselTimer) clearInterval(carouselTimer)
})
</script>

<style scoped>
.home {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 16px 48px;
}

/* 轮播 */
.carousel-section {
  padding: 20px 0 8px;
}

.carousel-container {
  max-width: 1200px;
  margin: 0 auto;
}

.carousel-wrapper {
  position: relative;
  width: 100%;
  aspect-ratio: 16 / 9;
  min-height: 200px;
  max-height: min(56vh, 440px);
  border-radius: var(--client-radius);
  overflow: hidden;
  box-shadow: 0 12px 40px rgba(59, 130, 246, 0.15);
  background: linear-gradient(135deg, #dbeafe, #eff6ff);
}

.carousel-slide {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  text-decoration: none;
  color: #fff;
}

.carousel-image-bg {
  position: absolute;
  inset: -8px;
  width: calc(100% + 16px);
  height: calc(100% + 16px);
  object-fit: cover;
  object-position: center;
  filter: blur(8px) saturate(1.06);
  transform: scale(1.04);
  opacity: 0.55;
}

.carousel-image-fg {
  position: relative;
  z-index: 1;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center center;
}

.carousel-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(120deg, rgba(30, 58, 95, 0.78) 0%, rgba(59, 130, 246, 0.35) 55%, rgba(255, 255, 255, 0.08) 100%);
  pointer-events: none;
}

.carousel-content {
  position: absolute;
  inset: 0;
  z-index: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 28px 24px 52px 72px;
  max-width: 620px;
}

@media (min-width: 768px) {
  .carousel-content {
    padding: 40px 48px 60px 112px;
  }
}

.carousel-kicker {
  font-size: 0.72rem;
  font-weight: 600;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: rgba(255, 255, 255, 0.9);
  margin: 0 0 10px;
}

.carousel-title {
  font-size: clamp(1.4rem, 3.6vw, 2rem);
  font-weight: 800;
  line-height: 1.2;
  margin: 0 0 10px;
  text-shadow: 0 2px 16px rgba(0, 0, 0, 0.35);
}

.carousel-sub {
  font-size: 0.94rem;
  line-height: 1.55;
  color: rgba(255, 255, 255, 0.92);
  margin: 0 0 18px;
  text-shadow: 0 1px 8px rgba(0, 0, 0, 0.25);
}

.carousel-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
}

.btn-carousel {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 10px 22px;
  border-radius: 8px;
  font-size: 0.875rem;
  font-weight: 600;
  text-decoration: none;
  transition: transform 0.15s, box-shadow 0.2s, background 0.2s;
}

.btn-carousel.primary {
  background: linear-gradient(135deg, #38bdf8, #3b82f6);
  color: #fff;
  border: none;
  box-shadow: 0 6px 20px rgba(59, 130, 246, 0.45);
  cursor: pointer;
}

.btn-carousel.secondary {
  background: rgba(255, 255, 255, 0.22);
  color: #fff;
  border: 1px solid rgba(255, 255, 255, 0.45);
  backdrop-filter: blur(6px);
}

.btn-carousel.secondary:hover {
  background: rgba(255, 255, 255, 0.32);
}

.carousel-nav {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  z-index: 2;
  width: 44px;
  height: 44px;
  border: none;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.28);
  color: #fff;
  font-size: 1.5rem;
  line-height: 1;
  cursor: pointer;
  transition: background 0.2s;
}

.carousel-nav:hover {
  background: rgba(255, 255, 255, 0.42);
}

.carousel-nav.prev {
  left: 12px;
}
.carousel-nav.next {
  right: 12px;
}

.carousel-dots {
  position: absolute;
  bottom: 14px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 2;
  display: flex;
  gap: 8px;
}

.carousel-dot {
  width: 8px;
  height: 8px;
  padding: 0;
  border: none;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.4);
  cursor: pointer;
  transition: all 0.25s;
}

.carousel-dot.active {
  background: #fff;
  width: 22px;
  border-radius: 6px;
}

.carousel-fade-enter-active,
.carousel-fade-leave-active {
  transition: opacity 0.45s ease;
}
.carousel-fade-enter-from,
.carousel-fade-leave-to {
  opacity: 0;
}

/* 简介 */
.intro-section {
  margin-top: 28px;
  padding: 28px 22px;
  text-align: center;
  background: var(--client-surface);
  border-radius: var(--client-radius);
  border: 1px solid rgba(59, 130, 246, 0.12);
  box-shadow: var(--client-shadow);
}

.intro-text {
  margin: 0 0 20px;
  font-size: 0.95rem;
  color: var(--client-muted);
  line-height: 1.65;
  max-width: 640px;
  margin-left: auto;
  margin-right: auto;
}

.intro-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  justify-content: center;
}

.btn-large {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 12px 26px;
  border-radius: 10px;
  font-size: 0.94rem;
  font-weight: 600;
  text-decoration: none;
  transition: transform 0.15s, box-shadow 0.2s;
}

.btn-large.primary {
  background: linear-gradient(135deg, #60a5fa, #3b82f6);
  color: #fff;
  box-shadow: 0 8px 22px rgba(59, 130, 246, 0.35);
}

.btn-large.primary:hover {
  transform: translateY(-2px);
}

.btn-large.ghost {
  background: var(--client-surface);
  color: var(--client-primary);
  border: 1px solid rgba(59, 130, 246, 0.35);
}

.btn-large.ghost:hover {
  background: var(--client-accent-soft);
}

/* 核心能力 */
.core-section {
  padding: 44px 0 16px;
}

.section-heading {
  font-size: 1.35rem;
  font-weight: 800;
  color: var(--client-text);
  text-align: center;
  margin: 0 0 8px;
}

.section-sub {
  font-size: 0.875rem;
  color: var(--client-muted);
  text-align: center;
  margin: 0 0 28px;
}

.abilities-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 20px;
}

.ability-card {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border-radius: var(--client-radius);
  background: var(--client-surface);
  border: 1px solid rgba(59, 130, 246, 0.1);
  box-shadow: var(--client-shadow);
  text-decoration: none;
  color: inherit;
  transition: transform 0.22s, box-shadow 0.22s;
}

.ability-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 16px 36px rgba(59, 130, 246, 0.14);
}

.ability-img-wrap {
  position: relative;
  aspect-ratio: 16 / 10;
  overflow: hidden;
  background: #e0f2fe;
}

.ability-image-bg {
  position: absolute;
  inset: -6px;
  width: calc(100% + 12px);
  height: calc(100% + 12px);
  object-fit: cover;
  object-position: center;
  filter: blur(6px) saturate(1.06);
  opacity: 0.42;
  transform: scale(1.04);
}

.ability-image-fg {
  position: relative;
  z-index: 1;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center center;
  transition: transform 0.35s ease;
}

.ability-card:hover .ability-image-fg {
  transform: scale(1.02);
}

.ability-shade {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, transparent 50%, rgba(30, 58, 95, 0.2) 100%);
  pointer-events: none;
}

.ability-body {
  padding: 18px 18px 22px;
}

.ability-body h3 {
  font-size: 1.05rem;
  font-weight: 700;
  margin: 0 0 8px;
  color: var(--client-text);
}

.ability-body p {
  font-size: 0.8125rem;
  color: var(--client-muted);
  line-height: 1.55;
  margin: 0;
}

/* 热门新闻 */
.news-section {
  padding: 36px 0 8px;
}

.news-header {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 20px;
}

.news-header .section-heading {
  margin: 0;
  text-align: left;
}

.view-more {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--client-primary);
  text-decoration: none;
}

.view-more:hover {
  text-decoration: underline;
}

.muted {
  color: var(--client-muted);
  font-size: 0.875rem;
}

.news-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 16px;
}

.news-card {
  display: block;
  padding: 20px 20px 18px;
  background: #f8fbff;
  border-radius: 10px;
  border: 1px solid rgba(59, 130, 246, 0.12);
  border-left: 4px solid var(--client-primary);
  text-decoration: none;
  color: inherit;
  transition: background 0.2s, box-shadow 0.2s, transform 0.2s;
}

.news-card:hover {
  background: #fff;
  box-shadow: 0 8px 24px rgba(59, 130, 246, 0.12);
  transform: translateY(-2px);
}

.news-tag {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 700;
  background: var(--client-accent-soft);
  color: var(--client-primary);
  margin-bottom: 10px;
}

.news-card-title {
  font-size: 0.98rem;
  font-weight: 700;
  color: var(--client-text);
  line-height: 1.45;
  margin: 0 0 10px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.news-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 12px;
  color: var(--client-muted);
}

.dev-hint {
  margin-top: 32px;
  padding: 14px 18px;
  background: var(--client-accent-soft);
  border: 1px solid rgba(59, 130, 246, 0.2);
  border-radius: var(--client-radius);
  font-size: 0.8125rem;
  color: var(--client-text);
  line-height: 1.55;
}

.dev-hint code {
  background: #fff;
  padding: 2px 8px;
  border-radius: 6px;
  font-size: 0.75rem;
}

@media (max-width: 640px) {
  .carousel-wrapper {
    max-height: 42vh;
  }
  .carousel-content {
    padding-bottom: 48px;
  }
  .carousel-buttons {
    flex-direction: column;
    align-items: stretch;
  }
  .btn-carousel {
    justify-content: center;
  }
}
</style>
