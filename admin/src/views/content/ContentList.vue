<template>
  <div class="page">
    <div class="toolbar">
      <h2>知识内容</h2>
      <div class="toolbar-right">
        <button type="button" class="btn batch" :disabled="selectedIds.length === 0" @click="batchOffline">批量下架</button>
        <button type="button" class="btn batch" :disabled="selectedIds.length === 0" @click="batchPublish">批量恢复发布</button>
        <button type="button" class="btn batch danger" :disabled="selectedIds.length === 0" @click="batchDelete">批量删除</button>
        <router-link to="/content/add" class="btn primary">新增内容</router-link>
      </div>
    </div>
    <p v-if="routeCategoryHint" class="route-hint">{{ routeCategoryHint }}</p>
    <div class="filters">
      <select v-model="statusFilter" @change="onStatusChange">
        <option value="">全部状态</option>
        <option value="1">已发布</option>
        <option value="2">已下架</option>
        <option value="3">待审核</option>
        <option value="0">草稿</option>
      </select>
      <select v-model="query.categoryId" @change="load">
        <option value="">全部分类</option>
        <option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</option>
      </select>
      <input v-model="query.title" placeholder="标题关键词" @keyup.enter="load" />
      <button @click="load">查询</button>
    </div>
    <table class="table">
      <thead>
        <tr>
          <th class="col-check">
            <input type="checkbox" :checked="isAllPageSelected" @change="onToggleAll" />
          </th>
          <th>序号</th>
          <th class="col-cover">封面</th>
          <th>标题</th>
          <th>分类</th>
          <th class="col-center">浏览量</th>
          <th class="col-center">状态</th>
          <th class="col-center">操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(row, index) in list" :key="row.id">
          <td class="col-check">
            <input type="checkbox" v-model="selectedIds" :value="row.id" />
          </td>
          <td>{{ (pageNum - 1) * pageSize + index + 1 }}</td>
          <td class="col-cover">
            <img
              v-if="coverSrc(row)"
              :src="coverSrc(row)"
              alt=""
              class="cover-thumb"
            />
          </td>
          <td>{{ row.title }}</td>
          <td>{{ row.categoryName ?? '-' }}</td>
          <td class="col-center">{{ row.viewCount ?? 0 }}</td>
          <td class="col-center">{{ statusText(row.status) }}</td>
          <td class="col-actions">
            <span class="actions-wrap">
              <template v-if="Number(row.status) === 3">
                <button type="button" class="btn-ok" @click="audit(row.id, 1)">通过</button>
                <button type="button" class="btn-danger" @click="audit(row.id, 0)">驳回</button>
              </template>
              <router-link :to="`/content/edit/${row.id}`" class="action-link">编辑</router-link>
              <button v-if="Number(row.status) === 1" type="button" class="link action-link" @click="setStatus(row, 2)">下架</button>
              <button v-if="Number(row.status) === 2" type="button" class="link action-link" @click="setStatus(row, 1)">恢复发布</button>
              <button type="button" class="link danger action-link" @click="del(row)">删除</button>
            </span>
          </td>
        </tr>
      </tbody>
    </table>
    <PaginationBar v-model="pageNum" class="pager-bar" :total="total" :page-size="pageSize" @current-change="() => load()" />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import {
  getContentPage,
  deleteContent,
  getCategoryList,
  auditContent,
  changeContentStatus,
  batchDeleteContent,
  batchOfflineContent,
  batchPublishContent,
} from '@/api/content'
import PaginationBar from '@/components/PaginationBar.vue'
import { resolveMediaUrl } from '@/utils/resolveMediaUrl'

const route = useRoute()

const list = ref([])
const selectedIds = ref([])
const categories = ref([])
const pageNum = ref(1)
const pageSize = 10
const query = reactive({ categoryId: '', title: '' })
const statusFilter = ref('')
const total = ref(0)

/** 列表自动刷新（新待审核、他处审核后同步），静默失败避免打断操作 */
const POLL_MS = 10000
let pollTimer = null

/** 从「分类管理」带 query 进入时展示提示 */
const routeCategoryHint = computed(() => {
  const cid = route.query.categoryId
  if (cid == null || cid === '') return ''
  const idStr = String(cid)
  const cat = categories.value.find((c) => String(c.id) === idStr)
  const name = cat?.name ? `「${cat.name}」` : `ID ${idStr}`
  return `当前按分类筛选：${name}（含全部状态）；可改下方筛选项或分类。`
})

function applyRouteCategoryQuery() {
  const cid = route.query.categoryId
  if (cid != null && cid !== '') {
    query.categoryId = String(cid)
    statusFilter.value = ''
    pageNum.value = 1
    return true
  }
  return false
}

async function load(_silent) {
  try {
    const params = {
      pageNum: pageNum.value,
      pageSize,
      categoryId: query.categoryId || undefined,
      title: query.title || undefined,
    }
    
    // 如果选择了特定状态，则传递 status 参数
    if (statusFilter.value) {
      params.status = parseInt(statusFilter.value)
    }
    
    const data = await getContentPage(params)
    list.value = data.records ?? data.list ?? []
    total.value = Number(data.total ?? 0)
    const pageIdSet = new Set(list.value.map((r) => r.id))
    selectedIds.value = selectedIds.value.filter((id) => pageIdSet.has(id))
  } catch (e) {
    if (!_silent) console.error(e)
  }
}

function onWinFocus() {
  load(true)
}

const isAllPageSelected = computed(() => {
  if (!list.value.length) return false
  return list.value.every((r) => selectedIds.value.includes(r.id))
})

function onToggleAll(e) {
  const pageIds = list.value.map((r) => r.id)
  if (e.target.checked) {
    selectedIds.value = [...new Set([...selectedIds.value, ...pageIds])]
  } else {
    const drop = new Set(pageIds)
    selectedIds.value = selectedIds.value.filter((id) => !drop.has(id))
  }
}

async function batchDelete() {
  if (!selectedIds.value.length) return
  if (!confirm(`确定将选中的 ${selectedIds.value.length} 条内容移入回收站（逻辑删除）？`)) return
  try {
    await batchDeleteContent([...selectedIds.value])
    selectedIds.value = []
    alert('已删除')
    load()
  } catch (e) {
    alert(e.message || '批量删除失败')
  }
}

async function batchOffline() {
  if (!selectedIds.value.length) return
  if (!confirm(`确定下架选中的内容？仅「已发布」的会被下架，作者将收到站内通知。`)) return
  try {
    await batchOfflineContent([...selectedIds.value])
    selectedIds.value = []
    alert('已处理')
    load()
  } catch (e) {
    alert(e.message || '批量下架失败')
  }
}

async function batchPublish() {
  if (!selectedIds.value.length) return
  if (!confirm('确定将选中内容恢复为「已发布」？仅当前为「已下架」的会变更。')) return
  try {
    await batchPublishContent([...selectedIds.value])
    selectedIds.value = []
    alert('已处理')
    load()
  } catch (e) {
    alert(e.message || '批量恢复发布失败')
  }
}

async function loadCategories() {
  try {
    const data = await getCategoryList()
    categories.value = Array.isArray(data) ? data : (data.list ?? [])
  } catch (_) {}
}

/** 无封面或仅空白时不展示图片 */
function coverSrc(row) {
  const raw = row?.cover
  if (raw == null || !String(raw).trim()) return ''
  return resolveMediaUrl(String(raw).trim())
}

function statusText(status) {
  const s = Number(status)
  if (s === 0) return '草稿'
  if (s === 1) return '发布'
  if (s === 2) return '下架'
  if (s === 3) return '待审核'
  return '发布'
}

async function audit(id, status) {
  const isApprove = status === 1
  if (!confirm(isApprove ? '确定通过该内容？' : '确定驳回该内容？驳回后将变为草稿，作者将看到您填写的驳回原因。')) return
  let reason = null
  if (!isApprove) {
    reason = window.prompt('驳回原因（必填，将展示给作者）：')
    if (reason === null) return
    if (!String(reason).trim()) {
      alert('请填写驳回原因，作者将据此修改后再次提交。')
      return
    }
    reason = String(reason).trim()
  }
  try {
    await auditContent(id, status, reason)
    alert(isApprove ? '已通过' : '已驳回')
    load()
  } catch (e) {
    alert(e.message || '操作失败')
  }
}

async function del(row) {
  if (!confirm(`确定删除《${row.title}》？`)) return
  try {
    await deleteContent(row.id)
    load()
  } catch (e) {
    alert(e.message || '删除失败')
  }
}

function onStatusChange() {
  pageNum.value = 1
  load()
}

async function setStatus(row, status) {
  const text = status === 2 ? '下架' : '恢复发布'
  if (!confirm(`确定${text}《${row.title}》？`)) return
  try {
    await changeContentStatus(row.id, status)
    await load()
  } catch (e) {
    alert(e.message || `${text}失败`)
  }
}

/** 有待审核时默认看「待审核」，否则默认「已发布」 */
async function checkPendingContent() {
  try {
    const data = await getContentPage({ pageNum: 1, pageSize: 1, status: 3 })
    const pendingTotal = Number(data.total ?? 0)
    statusFilter.value = pendingTotal > 0 ? '3' : '1'
  } catch (e) {
    statusFilter.value = '1'
  }
}

onMounted(async () => {
  await loadCategories()
  if (!applyRouteCategoryQuery()) {
    await checkPendingContent()
  }
  await load()
  pollTimer = window.setInterval(() => load(true), POLL_MS)
  window.addEventListener('focus', onWinFocus)
})

onUnmounted(() => {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
  window.removeEventListener('focus', onWinFocus)
})

watch(
  () => route.query.categoryId,
  async () => {
    applyRouteCategoryQuery()
    await load()
  }
)
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 8px;
}
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.btn.batch {
  padding: 6px 12px;
  border-radius: 10px;
  border: 1px solid rgba(59, 130, 246, 0.22);
  background: var(--client-surface);
  cursor: pointer;
  font-size: 13px;
  color: var(--client-text);
}
.btn.batch.danger {
  border-color: rgba(239, 68, 68, 0.35);
  color: #dc2626;
}
.btn.batch:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.table th.col-check,
.table td.col-check {
  width: 36px;
  text-align: center;
}
.table th.col-cover,
.table td.col-cover {
  width: 100px;
  text-align: center;
  vertical-align: middle;
}
.cover-thumb {
  width: 72px;
  height: 44px;
  object-fit: cover;
  border-radius: 8px;
  border: 1px solid rgba(59, 130, 246, 0.15);
  display: inline-block;
  vertical-align: middle;
}
.route-hint {
  margin: 0 0 10px;
  padding: 8px 12px;
  background: var(--client-accent-soft);
  border: 1px solid rgba(59, 130, 246, 0.22);
  border-radius: 10px;
  font-size: 13px;
  color: var(--client-primary);
}
.filters {
  margin-bottom: 16px;
  display: flex;
  gap: 8px;
}
.recycle-toggle {
  font-size: 13px;
  color: var(--client-muted);
  display: inline-flex;
  align-items: center;
  gap: 4px;
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
.table th.col-center,
.table td.col-center {
  text-align: center;
}
.table td.col-actions {
  text-align: center;
}
.table td.col-actions .actions-wrap {
  display: inline-flex;
  align-items: center;
  gap: 16px;
}
.table td.col-actions .action-link {
  font-size: 14px;
}
.table td.col-actions a.action-link {
  text-decoration: none;
  color: var(--client-primary);
}
.table td.col-actions .link {
  background: none;
  border: none;
  color: #dc2626;
  cursor: pointer;
  font-size: 14px;
  padding: 0;
}
.table td.col-actions .btn-ok {
  margin-right: 4px;
  padding: 2px 8px;
  font-size: 12px;
  background: var(--client-primary);
  color: #fff;
  border: none;
  border-radius: 8px;
  cursor: pointer;
}
.table td.col-actions .btn-danger {
  margin-right: 4px;
  padding: 2px 8px;
  font-size: 12px;
  background: #ef4444;
  color: #fff;
  border: none;
  border-radius: 8px;
  cursor: pointer;
}
.pager-bar {
  margin-top: 16px;
}
.btn.primary {
  background: var(--client-primary);
  color: #fff;
  padding: 8px 16px;
  border-radius: 10px;
  text-decoration: none;
}
</style>
