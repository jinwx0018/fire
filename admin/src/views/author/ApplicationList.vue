<template>
  <div class="page">
    <div class="tabs">
      <button type="button" :class="{ active: tab === 'pending' }" @click="setTab('pending')">待审核</button>
      <button type="button" :class="{ active: tab === 'processed' }" @click="setTab('processed')">已处理记录</button>
    </div>

    <template v-if="tab === 'pending'">
      <p class="tip">
        用户提交的作者申请，通过后其角色将变为「作者」，可发布需审核的知识内容。列表已按用户去重：同一账号若存在多条待审核记录（异常数据），仅展示该用户<strong>最新提交</strong>的一条。处理后的记录可在「已处理记录」中查看。
      </p>
      <table class="table">
        <thead>
          <tr>
            <th>用户名</th>
            <th>手机号</th>
            <th>邮箱</th>
            <th>申请原因</th>
            <th>附件</th>
            <th>申请时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in list" :key="row.id">
            <td>{{ row.username }}</td>
            <td>{{ row.phone ?? '-' }}</td>
            <td>{{ row.email ?? '-' }}</td>
            <td class="reason-cell">{{ row.applyReason || '-' }}</td>
            <td>
              <a
                v-if="row.attachments"
                :href="row.attachments.split(',')[0]"
                target="_blank"
                rel="noopener noreferrer"
              >
                查看附件
              </a>
              <span v-else>-</span>
            </td>
            <td>{{ formatDateTime(row.createTime) }}</td>
            <td>
              <button type="button" class="btn-ok" @click="onApprove(row)">通过</button>
              <button type="button" class="btn-danger" @click="onReject(row)">驳回</button>
            </td>
          </tr>
        </tbody>
      </table>
      <p v-if="list.length === 0" class="empty">暂无待审核申请</p>
    </template>

    <template v-else>
      <p class="tip">已通过或已驳回的申请按审核时间倒序排列，便于核对刚处理的结果。</p>
      <table class="table">
        <thead>
          <tr>
            <th>用户名</th>
            <th>状态</th>
            <th>申请原因</th>
            <th>附件</th>
            <th>申请时间</th>
            <th>审核时间</th>
            <th>审核人</th>
            <th>备注 / 驳回原因</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in processedList" :key="row.id">
            <td>{{ row.username }}</td>
            <td>{{ statusLabel(row.status) }}</td>
            <td class="reason-cell">{{ row.applyReason || '-' }}</td>
            <td>
              <a
                v-if="row.attachments"
                :href="String(row.attachments).split(',')[0]"
                target="_blank"
                rel="noopener noreferrer"
              >
                查看附件
              </a>
              <span v-else>-</span>
            </td>
            <td>{{ formatDateTime(row.createTime) }}</td>
            <td>{{ row.reviewTime ? formatDateTime(row.reviewTime) : '—' }}</td>
            <td>{{ row.reviewerUsername || row.reviewBy || '—' }}</td>
            <td class="reason-cell">{{ remarkCell(row) }}</td>
          </tr>
        </tbody>
      </table>
      <p v-if="processedList.length === 0" class="empty">暂无已处理记录</p>
      <PaginationBar
        v-model="processedPageNum"
        class="pager"
        :total="processedTotal"
        :page-size="processedPageSize"
        @current-change="() => loadProcessed()"
      />
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { listPending, listProcessedPage, approve, reject } from '@/api/authorApplication'
import { formatDateTime } from '@/utils/formatDateTime'
import PaginationBar from '@/components/PaginationBar.vue'

const tab = ref('pending')
const list = ref([])
const processedList = ref([])
const processedTotal = ref(0)
const processedPageNum = ref(1)
const processedPageSize = 10

const POLL_MS = 8000
let pollTimer = null

function statusLabel(s) {
  if (s === 'APPROVED') return '已通过'
  if (s === 'REJECTED') return '已驳回'
  return s || '—'
}

function remarkCell(row) {
  if (row.status === 'REJECTED') return row.rejectReason || '—'
  if (row.status === 'APPROVED') return row.reviewRemark || '—'
  return row.rejectReason || row.reviewRemark || '—'
}

async function loadPending(silent) {
  try {
    list.value = await listPending()
  } catch (e) {
    console.error(e)
    if (!silent) alert(e.message || '加载失败')
  }
}

async function loadProcessed(silent) {
  try {
    const data = await listProcessedPage({
      pageNum: processedPageNum.value,
      pageSize: processedPageSize,
    })
    processedList.value = data.records ?? data.list ?? []
    processedTotal.value = Number(data.total ?? 0)
  } catch (e) {
    console.error(e)
    if (!silent) alert(e.message || '加载失败')
  }
}

async function showProcessedAfterAction() {
  tab.value = 'processed'
  processedPageNum.value = 1
  await nextTick()
  await loadProcessed()
}

function setTab(next) {
  tab.value = next
  if (next === 'pending') {
    loadPending()
  } else {
    processedPageNum.value = 1
    loadProcessed()
  }
}

function poll() {
  if (tab.value === 'pending') {
    loadPending(true)
  } else {
    loadProcessed(true)
  }
}

async function onApprove(row) {
  if (!confirm(`确定通过用户「${row.username}」的作者申请？`)) return
  const reviewRemark = window.prompt('审核备注（可选）：')
  if (reviewRemark === null) return
  try {
    await approve(row.id, reviewRemark || undefined)
    await loadPending()
    await showProcessedAfterAction()
  } catch (e) {
    alert(e.message || '操作失败')
  }
}

async function onReject(row) {
  const reason = window.prompt('驳回原因（可选）：')
  if (reason === null) return
  try {
    await reject(row.id, reason || undefined)
    await loadPending()
    await showProcessedAfterAction()
  } catch (e) {
    alert(e.message || '操作失败')
  }
}

function onWinFocus() {
  poll()
}

onMounted(() => {
  loadPending()
  pollTimer = window.setInterval(poll, POLL_MS)
  window.addEventListener('focus', onWinFocus)
})

onUnmounted(() => {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
  window.removeEventListener('focus', onWinFocus)
})
</script>

<style scoped>
.tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}
.tabs button {
  padding: 8px 16px;
  border: 1px solid rgba(59, 130, 246, 0.22);
  background: var(--client-surface);
  border-radius: 10px;
  cursor: pointer;
  font-size: 14px;
  color: var(--client-text);
}
.tabs button.active {
  background: var(--client-primary);
  color: #fff;
  border-color: var(--client-primary);
}
.tip {
  margin-bottom: 16px;
  font-size: 13px;
  color: var(--client-muted);
  line-height: 1.5;
}
.table {
  width: 100%;
  border-collapse: collapse;
}
.table th,
.table td {
  padding: 10px;
  border: 1px solid rgba(59, 130, 246, 0.12);
}
.reason-cell {
  max-width: 220px;
  white-space: pre-wrap;
  word-break: break-all;
}
.btn-ok {
  margin-right: 8px;
  padding: 4px 12px;
  background: var(--client-primary);
  color: #fff;
  border: none;
  border-radius: 8px;
  cursor: pointer;
}
.btn-danger {
  padding: 4px 12px;
  background: #ef4444;
  color: #fff;
  border: none;
  border-radius: 8px;
  cursor: pointer;
}
.empty {
  color: var(--client-muted);
  padding: 24px;
}
.pager {
  margin-top: 16px;
}

a {
  color: var(--client-primary);
}

a:hover {
  color: var(--client-primary-hover);
}
</style>
