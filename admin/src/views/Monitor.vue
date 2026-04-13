<template>
  <div class="page">
    <h2 class="title">系统监控与日志</h2>
    <p class="hint">展示 JVM 与数据库状态；日志依赖后端 <code>logging.file.name</code> 配置。</p>

    <el-card shadow="never" class="card">
      <template #header>
        <span>运行概览</span>
        <el-button type="primary" link :loading="loadingOverview" @click="loadOverview">刷新</el-button>
      </template>
      <el-descriptions v-if="overview" :column="1" border>
        <el-descriptions-item label="数据库">{{ overview.database }}</el-descriptions-item>
        <el-descriptions-item label="运行时长">{{ formatUptime(overview.uptimeMs) }}</el-descriptions-item>
        <el-descriptions-item label="堆内存">{{ formatBytes(overview.heapUsedBytes) }} / {{ formatBytes(overview.heapMaxBytes) }}</el-descriptions-item>
        <el-descriptions-item label="线程数">{{ overview.threadCount }}</el-descriptions-item>
        <el-descriptions-item label="CPU 逻辑核心">{{ overview.processors }}</el-descriptions-item>
        <el-descriptions-item label="日志文件">
          {{ overview.logFileConfigured ? overview.logFilePath : '未配置（仅控制台输出）' }}
        </el-descriptions-item>
      </el-descriptions>
      <el-empty v-else description="暂无数据" />
    </el-card>

    <el-card shadow="never" class="card">
      <template #header>
        <span>日志尾部</span>
        <div class="log-actions">
          <el-input-number v-model="logLines" :min="50" :max="500" :step="50" size="small" />
          <el-button type="primary" size="small" :loading="loadingLogs" @click="loadLogs">读取</el-button>
        </div>
      </template>
      <el-input v-model="logText" type="textarea" :rows="18" readonly class="log-box" placeholder="点击「读取」加载日志尾部" />
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getMonitorOverview, getLogTail } from '@/api/monitor'

const overview = ref(null)
const loadingOverview = ref(false)
const loadingLogs = ref(false)
const logLines = ref(200)
const logText = ref('')

function formatBytes(n) {
  if (n == null || Number.isNaN(n)) return '-'
  const v = Number(n)
  if (v < 1024) return `${v} B`
  if (v < 1024 * 1024) return `${(v / 1024).toFixed(1)} KB`
  return `${(v / (1024 * 1024)).toFixed(1)} MB`
}

function formatUptime(ms) {
  if (ms == null) return '-'
  const s = Math.floor(ms / 1000)
  const d = Math.floor(s / 86400)
  const h = Math.floor((s % 86400) / 3600)
  const m = Math.floor((s % 3600) / 60)
  const sec = s % 60
  const parts = []
  if (d > 0) parts.push(`${d} 天`)
  if (h > 0) parts.push(`${h} 小时`)
  if (m > 0) parts.push(`${m} 分`)
  parts.push(`${sec} 秒`)
  return parts.join(' ')
}

async function loadOverview() {
  loadingOverview.value = true
  try {
    overview.value = await getMonitorOverview()
  } catch (e) {
    ElMessage.error(e?.message || '加载失败')
  } finally {
    loadingOverview.value = false
  }
}

async function loadLogs() {
  loadingLogs.value = true
  try {
    logText.value = await getLogTail(logLines.value)
  } catch (e) {
    ElMessage.error(e?.message || '读取失败')
    logText.value = ''
  } finally {
    loadingLogs.value = false
  }
}

onMounted(() => {
  loadOverview()
})
</script>

<style scoped>
.page {
  max-width: 960px;
}
.title {
  margin-bottom: 8px;
  font-size: 1.125rem;
  color: var(--client-text);
}
.hint {
  color: var(--client-muted);
  font-size: 13px;
  margin-bottom: 16px;
  line-height: 1.5;
}
.hint code {
  background: var(--client-accent-soft);
  color: var(--client-primary);
  padding: 2px 6px;
  border-radius: 6px;
  font-size: 12px;
}
.card {
  margin-bottom: 16px;
}
.card :deep(.el-card__header) {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
}
.log-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
.log-box :deep(textarea) {
  font-family: ui-monospace, Consolas, monospace;
  font-size: 12px;
  line-height: 1.4;
}
</style>
