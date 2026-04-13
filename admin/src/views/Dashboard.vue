<template>
  <div class="data-screen">
    <header class="screen-header">
      <div class="header-bar" />
      <div class="header-row">
        <div class="header-center">
          <h1 class="screen-title">消防科普管理平台 · 首页</h1>
          <p class="screen-sub">数据大屏与统计报表 · 可选日期区间；未选为全时段累计（用户活跃见下方说明）</p>
        </div>
        <div class="header-right">
          <span class="clock">{{ clockText }}</span>
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始"
            end-placeholder="结束"
            value-format="YYYY-MM-DD"
            unlink-panels
            clearable
            class="range-dp"
          />
          <el-button type="primary" class="refresh-btn" :loading="loading" @click="loadAll">
            刷新数据
          </el-button>
        </div>
      </div>
      <div class="toolbar-row">
        <div class="export-btns">
          <span class="export-label">导出 Excel：</span>
          <el-button size="small" plain class="export-btn" @click="doExport('')">全部</el-button>
          <el-button size="small" plain class="export-btn" @click="doExport('content')">内容维度</el-button>
          <el-button size="small" plain class="export-btn" @click="doExport('user')">用户维度</el-button>
          <el-button size="small" plain class="export-btn" @click="doExport('interaction')">互动维度</el-button>
        </div>
      </div>
    </header>

    <div v-if="err" class="err-banner">{{ err }}</div>

    <section class="kpi-section">
      <div
        v-for="item in kpiItems"
        :key="item.key"
        class="kpi-panel"
      >
        <div class="kpi-corner tl" /><div class="kpi-corner tr" />
        <div class="kpi-corner bl" /><div class="kpi-corner br" />
        <div class="kpi-label">{{ item.label }}</div>
        <div class="kpi-value">
          <span class="num">{{ formatNum(item.value) }}</span>
        </div>
      </div>
    </section>

    <section class="report-section">
      <h2 class="section-heading">
        <span class="title-bar" /> 统计报表 · 维度摘要
      </h2>
      <p class="section-sub">与上方日期区间一致；图表与 KPI 同步按区间刷新。</p>
      <div class="report-grid">
        <div class="report-panel">
          <h3 class="report-title">内容维度</h3>
          <p v-if="contentStats" class="report-text">
            已发布知识按分类见下方饼图；浏览 Top10 共 {{ (contentStats.viewTop10 || []).length }} 条（受日期区间影响）
          </p>
          <p v-else class="report-muted">暂无或加载失败</p>
        </div>
        <div class="report-panel">
          <h3 class="report-title">用户维度</h3>
          <template v-if="userStats">
            <p class="report-text">
              <template v-if="dateRange?.length === 2">区间内活跃用户数（行为去重）：</template>
              <template v-else>近 7 天活跃用户数：</template>
              <strong class="report-strong">{{ userStats.activeUserCount ?? 0 }}</strong>
            </p>
            <ul v-if="userStats.roleCount?.length" class="role-list">
              <li v-for="r in userStats.roleCount" :key="r.roleId">
                {{ r.roleCode }}：{{ r.count }}
              </li>
            </ul>
            <p v-else class="report-muted">无角色分布数据</p>
          </template>
          <p v-else class="report-muted">暂无或加载失败</p>
        </div>
        <div class="report-panel">
          <h3 class="report-title">互动维度</h3>
          <template v-if="interactionStats">
            <p class="report-text">帖子：{{ interactionStats.postCount ?? 0 }}</p>
            <p class="report-text">评论：{{ interactionStats.commentCount ?? 0 }}</p>
            <p class="report-text">点赞（已通过帖子的 like_count 合计）：{{ interactionStats.likeCount ?? 0 }}</p>
            <p class="report-hint">论坛帖子数、评论数、点赞均按「已通过审核」帖子口径统计。</p>
          </template>
          <p v-else class="report-muted">暂无或加载失败</p>
        </div>
      </div>
    </section>

    <section class="charts-section">
      <div class="chart-panel">
        <div class="panel-title">
          <span class="title-bar" /> 分类知识发布量
        </div>
        <div ref="pieRef" class="chart-inner" />
      </div>
      <div class="chart-panel">
        <div class="panel-title">
          <span class="title-bar" /> 月度趋势（区间内按月；未选日期为当年）
        </div>
        <p class="panel-hint">主色：知识 view 按创建月汇总 · 辅色：行为 VIEW 按发生月</p>
        <div ref="lineRef" class="chart-inner chart-inner-line" />
      </div>
    </section>

    <el-collapse v-model="debugOpen" class="debug-collapse">
      <el-collapse-item title="原始 JSON（调试用）" name="1">
        <pre v-if="contentStats" class="raw-pre">{{ JSON.stringify({ dashboard, contentStats, userStats, interactionStats }, null, 2) }}</pre>
      </el-collapse-item>
    </el-collapse>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import {
  getDashboard,
  getCategoryPie,
  getViewTrendRange,
  getContentStats,
  getUserStats,
  getInteractionStats,
  exportExcel,
} from '@/api/statistics'

const dashboard = ref(null)
const contentStats = ref(null)
const userStats = ref(null)
const interactionStats = ref(null)
const dateRange = ref(null)
const debugOpen = ref([])
const loading = ref(false)
const err = ref('')
const clockText = ref('')
const pieRef = ref(null)
const lineRef = ref(null)

let pieChart
let lineChart
let clockTimer
let resizeObserver

function rangeParams() {
  if (!dateRange.value || dateRange.value.length !== 2) return {}
  return { startDate: dateRange.value[0], endDate: dateRange.value[1] }
}

const kpiItems = computed(() => {
  const d = dashboard.value
  if (!d) {
    return [
      { key: 'kp', label: '已发布知识（条）', value: 0 },
      { key: 'kv', label: '知识浏览合计', value: 0 },
      { key: 'np', label: '已发布新闻（条）', value: 0 },
      { key: 'nv', label: '新闻浏览合计', value: 0 },
      { key: 'eq', label: '上架器材（条）', value: 0 },
      { key: 'bv', label: '行为 VIEW 次数', value: 0 },
      { key: 'fp', label: '论坛帖子', value: 0 },
      { key: 'fc', label: '论坛评论', value: 0 },
      { key: 'fl', label: '论坛点赞合计', value: 0 },
    ]
  }
  return [
    { key: 'kp', label: '已发布知识（条）', value: d.knowledgePublishCount ?? 0 },
    { key: 'kv', label: '知识浏览合计', value: d.knowledgeViewSum ?? 0 },
    { key: 'np', label: '已发布新闻（条）', value: d.newsPublishCount ?? 0 },
    { key: 'nv', label: '新闻浏览合计', value: d.newsViewSum ?? 0 },
    { key: 'eq', label: '上架器材（条）', value: d.equipmentOnShelfCount ?? 0 },
    { key: 'bv', label: '行为 VIEW 次数', value: d.behaviorViewCount ?? 0 },
    { key: 'fp', label: '论坛帖子', value: d.forumPostCount ?? 0 },
    { key: 'fc', label: '论坛评论', value: d.forumCommentCount ?? 0 },
    { key: 'fl', label: '论坛点赞合计', value: d.forumLikeSum ?? 0 },
  ]
})

function formatNum(n) {
  const v = Number(n) || 0
  return v.toLocaleString('zh-CN')
}

function tickClock() {
  const now = new Date()
  clockText.value = now.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
  })
}

function resizeCharts() {
  pieChart?.resize()
  lineChart?.resize()
}

/** 与用户端主色一致，保证浅色背景下可读 */
const chartText = '#64748b'
const chartAxis = 'rgba(100, 116, 139, 0.35)'
const chartSplit = 'rgba(59, 130, 246, 0.12)'
const chartTooltipBg = 'rgba(255, 255, 255, 0.97)'
const chartTooltipBorder = 'rgba(59, 130, 246, 0.22)'
const chartTooltipText = '#1e3a5f'
const linePrimary = '#3b82f6'
const lineSecondary = '#0ea5e9'
const piePalette = ['#3b82f6', '#0ea5e9', '#2563eb', '#6366f1', '#8b5cf6', '#38bdf8', '#60a5fa', '#93c5fd']

function setPieChart(data) {
  if (!pieRef.value) return
  if (!pieChart) pieChart = echarts.init(pieRef.value)
  pieChart.setOption({
    backgroundColor: 'transparent',
    color: piePalette,
    textStyle: { color: chartText },
    tooltip: {
      trigger: 'item',
      backgroundColor: chartTooltipBg,
      borderColor: chartTooltipBorder,
      textStyle: { color: chartTooltipText },
    },
    legend: {
      bottom: 4,
      type: 'scroll',
      textStyle: { color: chartText, fontSize: 11 },
      pageTextStyle: { color: chartText },
    },
    series: [
      {
        type: 'pie',
        radius: ['38%', '64%'],
        center: ['50%', '46%'],
        itemStyle: {
          borderColor: '#ffffff',
          borderWidth: 2,
        },
        label: { color: chartText, fontSize: 11 },
        data: (data || []).map((d) => ({
          name: d.name,
          value: Number(d.value) || 0,
        })),
        emphasis: {
          itemStyle: {
            shadowBlur: 24,
            shadowColor: 'rgba(59, 130, 246, 0.35)',
          },
        },
      },
    ],
  })
}

function setLineChart(data) {
  if (!lineRef.value) return
  if (!lineChart) lineChart = echarts.init(lineRef.value)
  const months = (data || []).map((d) => d.month)
  const knowledge = (data || []).map((d) => Number(d.knowledgeViewSum) || 0)
  const behavior = (data || []).map((d) => Number(d.behaviorViewCount) || 0)
  lineChart.setOption({
    backgroundColor: 'transparent',
    textStyle: { color: chartText },
    tooltip: {
      trigger: 'axis',
      backgroundColor: chartTooltipBg,
      borderColor: chartTooltipBorder,
      textStyle: { color: chartTooltipText },
    },
    legend: {
      data: ['知识 view（创建月）', '行为 VIEW（发生月）'],
      bottom: 0,
      textStyle: { color: chartText, fontSize: 11 },
    },
    grid: { left: 52, right: 20, top: 28, bottom: 52 },
    xAxis: {
      type: 'category',
      data: months,
      boundaryGap: false,
      axisLine: { lineStyle: { color: chartAxis } },
      axisLabel: { color: chartText, fontSize: 10 },
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: chartSplit } },
      axisLine: { show: true, lineStyle: { color: chartAxis } },
      axisLabel: { color: chartText, fontSize: 10 },
    },
    series: [
      {
        name: '知识 view（创建月）',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: { width: 2, color: linePrimary },
        itemStyle: { color: linePrimary },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(59, 130, 246, 0.28)' },
            { offset: 1, color: 'rgba(59, 130, 246, 0)' },
          ]),
        },
        data: knowledge,
      },
      {
        name: '行为 VIEW（发生月）',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: { width: 2, color: lineSecondary },
        itemStyle: { color: lineSecondary },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(14, 165, 233, 0.22)' },
            { offset: 1, color: 'rgba(14, 165, 233, 0)' },
          ]),
        },
        data: behavior,
      },
    ],
  })
}

async function loadAll() {
  loading.value = true
  err.value = ''
  const p = rangeParams()
  try {
    const [dash, pieData, lineData, cStats, uStats, iStats] = await Promise.all([
      getDashboard(p),
      getCategoryPie(p),
      getViewTrendRange(p),
      getContentStats(p),
      getUserStats(p),
      getInteractionStats(p),
    ])
    dashboard.value = dash
    contentStats.value = cStats
    userStats.value = uStats
    interactionStats.value = iStats
    await nextTick()
    setPieChart(pieData)
    setLineChart(lineData)
  } catch (e) {
    err.value = e?.message || String(e) || '加载失败'
    dashboard.value = null
    contentStats.value = null
    userStats.value = null
    interactionStats.value = null
  } finally {
    loading.value = false
    await nextTick()
    resizeCharts()
  }
}

async function doExport(type) {
  try {
    const blob = await exportExcel({ type: type || undefined, ...rangeParams() })
    if (!(blob instanceof Blob)) {
      ElMessage.error('导出失败：响应格式异常')
      return
    }
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `统计导出_${type || 'all'}_${Date.now()}.xlsx`
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('已开始下载')
  } catch (e) {
    ElMessage.error(e?.message || '导出失败')
  }
}

onMounted(() => {
  tickClock()
  clockTimer = window.setInterval(tickClock, 1000)
  loadAll()
  window.addEventListener('resize', resizeCharts)
  if (typeof ResizeObserver !== 'undefined') {
    resizeObserver = new ResizeObserver(() => resizeCharts())
    nextTick(() => {
      if (pieRef.value) resizeObserver.observe(pieRef.value)
      if (lineRef.value) resizeObserver.observe(lineRef.value)
    })
  }
})

onUnmounted(() => {
  if (clockTimer) clearInterval(clockTimer)
  window.removeEventListener('resize', resizeCharts)
  resizeObserver?.disconnect()
  pieChart?.dispose()
  lineChart?.dispose()
  pieChart = null
  lineChart = null
})
</script>

<style scoped>
.data-screen {
  margin: -24px;
  min-height: calc(100vh - 48px);
  padding: 20px 24px 28px;
  background: var(--client-bg);
  color: var(--client-text);
  box-sizing: border-box;
}

.screen-header {
  margin-bottom: 16px;
  position: relative;
}
.header-bar {
  height: 2px;
  margin-bottom: 14px;
  background: linear-gradient(
    90deg,
    transparent,
    rgba(59, 130, 246, 0.35),
    rgba(14, 165, 233, 0.25),
    transparent
  );
  border-radius: 2px;
}
.header-row {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}
.toolbar-row {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid rgba(59, 130, 246, 0.12);
}
.export-btns {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}
.export-label {
  font-size: 12px;
  color: var(--client-muted);
  margin-right: 4px;
}
.export-btn {
  --el-button-bg-color: var(--client-surface);
  --el-button-border-color: rgba(59, 130, 246, 0.22);
  --el-button-text-color: var(--client-text);
  --el-button-hover-bg-color: var(--client-accent-soft);
  --el-button-hover-border-color: rgba(59, 130, 246, 0.4);
  --el-button-hover-text-color: var(--client-primary);
}

.range-dp {
  width: 260px;
  max-width: 100%;
}
.range-dp :deep(.el-input__wrapper) {
  background: var(--client-surface);
  box-shadow: 0 0 0 1px rgba(59, 130, 246, 0.18) inset;
  border-radius: 10px;
}
.range-dp :deep(.el-range-input) {
  color: var(--client-text);
}
.range-dp :deep(.el-range-separator) {
  color: var(--client-muted);
}

.header-center {
  flex: 1;
  min-width: 200px;
  text-align: center;
}
.screen-title {
  margin: 0 0 6px;
  font-size: clamp(1.15rem, 2.2vw, 1.65rem);
  font-weight: 700;
  letter-spacing: 0.06em;
  color: var(--client-text);
}
.screen-sub {
  margin: 0;
  font-size: 12px;
  color: var(--client-muted);
  letter-spacing: 0.02em;
  line-height: 1.5;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: flex-end;
}
.clock {
  font-family: ui-monospace, 'Consolas', monospace;
  font-size: 13px;
  color: var(--client-primary);
  letter-spacing: 0.04em;
}
.refresh-btn {
  border-radius: 10px;
}

.err-banner {
  margin-bottom: 16px;
  padding: 10px 14px;
  border-radius: 10px;
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.28);
  color: #b91c1c;
  font-size: 13px;
}

.kpi-section {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(168px, 1fr));
  gap: 14px;
  margin-bottom: 18px;
}

@media (max-width: 768px) {
  .kpi-section {
    grid-template-columns: repeat(2, 1fr);
  }
  .header-center {
    text-align: left;
    width: 100%;
  }
  .header-right {
    width: 100%;
    justify-content: flex-start;
  }
}

.kpi-panel {
  position: relative;
  padding: 16px 14px 14px;
  background: var(--client-surface);
  border: 1px solid rgba(59, 130, 246, 0.14);
  border-radius: var(--client-radius);
  box-shadow: 0 2px 16px rgba(59, 130, 246, 0.06);
  overflow: hidden;
}

.kpi-corner {
  position: absolute;
  width: 10px;
  height: 10px;
  border-color: rgba(59, 130, 246, 0.45);
  border-style: solid;
  opacity: 0.85;
  pointer-events: none;
}
.kpi-corner.tl {
  top: 0;
  left: 0;
  border-width: 2px 0 0 2px;
}
.kpi-corner.tr {
  top: 0;
  right: 0;
  border-width: 2px 2px 0 0;
}
.kpi-corner.bl {
  bottom: 0;
  left: 0;
  border-width: 0 0 2px 2px;
}
.kpi-corner.br {
  bottom: 0;
  right: 0;
  border-width: 0 2px 2px 0;
}

.kpi-label {
  font-size: 12px;
  color: var(--client-muted);
  margin-bottom: 8px;
  letter-spacing: 0.02em;
}
.kpi-value .num {
  font-size: clamp(1.25rem, 2.4vw, 1.75rem);
  font-weight: 700;
  font-family: 'DIN Alternate', ui-monospace, 'Segoe UI', sans-serif;
  color: var(--client-primary);
}

.section-heading {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0 0 6px;
  font-size: 15px;
  font-weight: 600;
  color: var(--client-text);
  letter-spacing: 0.02em;
}
.section-sub {
  margin: 0 0 14px;
  font-size: 11px;
  color: var(--client-muted);
  line-height: 1.4;
}
.report-section {
  margin-bottom: 18px;
}
.report-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
}
@media (max-width: 992px) {
  .report-grid {
    grid-template-columns: 1fr;
  }
}
.report-panel {
  padding: 14px 16px;
  background: var(--client-surface);
  border: 1px solid rgba(59, 130, 246, 0.12);
  border-radius: var(--client-radius);
  min-height: 120px;
  box-shadow: 0 2px 12px rgba(59, 130, 246, 0.05);
}
.report-title {
  margin: 0 0 10px;
  font-size: 13px;
  font-weight: 600;
  color: var(--client-primary);
}
.report-text {
  margin: 4px 0;
  font-size: 13px;
  color: var(--client-text);
  line-height: 1.45;
}
.report-strong {
  color: var(--client-primary);
  font-size: 1.05em;
}
.report-muted {
  margin: 0;
  font-size: 13px;
  color: var(--client-muted);
}
.report-hint {
  margin: 10px 0 0;
  font-size: 11px;
  color: var(--client-muted);
  line-height: 1.4;
}
.role-list {
  margin: 8px 0 0;
  padding-left: 18px;
  font-size: 12px;
  color: var(--client-text);
}

.charts-section {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

@media (max-width: 992px) {
  .charts-section {
    grid-template-columns: 1fr;
  }
}

.chart-panel {
  position: relative;
  padding: 14px 16px 12px;
  min-height: 320px;
  background: var(--client-surface);
  border: 1px solid rgba(59, 130, 246, 0.12);
  border-radius: var(--client-radius);
  box-shadow: 0 2px 20px rgba(59, 130, 246, 0.06);
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  font-weight: 600;
  color: var(--client-text);
  margin-bottom: 6px;
  letter-spacing: 0.02em;
}
.title-bar {
  width: 4px;
  height: 16px;
  background: linear-gradient(180deg, var(--client-primary), var(--client-accent));
  border-radius: 2px;
  flex-shrink: 0;
}
.panel-hint {
  margin: 0 0 8px;
  font-size: 11px;
  color: var(--client-muted);
  line-height: 1.4;
}
.chart-inner {
  width: 100%;
  height: 280px;
}
.chart-inner-line {
  height: 260px;
}

.debug-collapse {
  margin-top: 16px;
  --el-collapse-border-color: rgba(59, 130, 246, 0.12);
  --el-collapse-header-bg-color: var(--client-surface);
  --el-collapse-content-bg-color: rgba(240, 247, 252, 0.5);
  border-radius: var(--client-radius);
  overflow: hidden;
  border: 1px solid rgba(59, 130, 246, 0.1);
}
.debug-collapse :deep(.el-collapse-item__header) {
  color: var(--client-text);
  font-size: 13px;
}
.debug-collapse :deep(.el-collapse-item__wrap) {
  border-bottom-color: rgba(59, 130, 246, 0.08);
}
.raw-pre {
  font-size: 11px;
  overflow: auto;
  max-height: 220px;
  margin: 0;
  color: var(--client-text);
  background: rgba(224, 242, 254, 0.35);
  padding: 10px;
  border-radius: 8px;
  border: 1px solid rgba(59, 130, 246, 0.1);
}
</style>
