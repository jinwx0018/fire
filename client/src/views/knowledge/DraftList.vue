<template>
  <KnowledgeModuleShell :crumbs="crumbs">
    <header class="page-head">
      <div class="page-head-row">
        <div class="page-head-text">
          <h1 class="page-title">我的知识</h1>
          <p class="page-sub">草稿、待审与已发布内容的管理入口</p>
        </div>
        <router-link to="/knowledge/add" class="btn-primary">新增知识</router-link>
      </div>
    </header>

    <p class="tip-block">
      草稿可编辑并提交审核；待审核内容需管理员处理；已发布内容作者本人也可继续编辑或删除。若曾被驳回，此处会显示驳回原因；若一直显示「暂无」，请确认数据库已执行 08
      脚本且管理员驳回时已填写原因。
    </p>

    <div class="toolbar">
      <select v-model="query.categoryId" class="field-control" @change="onFilterChange">
        <option value="">全部分类</option>
        <option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</option>
      </select>
      <input v-model="query.title" class="field-control flex-grow" placeholder="标题搜索…" @keyup.enter="load" />
      <button type="button" class="btn-query" @click="load">查询</button>
    </div>

    <ul class="draft-list">
      <li v-for="row in list" :key="row.id" class="draft-card">
        <div class="draft-head">
          <router-link :to="`/knowledge/${row.id}`" class="draft-title">{{ row.title }}</router-link>
          <span v-if="row.categoryName" class="draft-cat">{{ row.categoryName }}</span>
          <span class="status-tag" :class="statusClass(row)">{{ statusText(row) }}</span>
        </div>
        <div class="draft-meta">
          <span>创建时间：{{ formatDateTime(row.createTime) }}</span>
          <span>点赞：{{ row.likeCount ?? 0 }}</span>
        </div>
        <p v-if="isDraft(row)" class="reject-line">
          驳回原因：{{ rejectReasonText(row) || '（暂无）' }}
        </p>
        <div class="draft-actions">
          <router-link :to="`/knowledge/${row.id}`" class="btn-outline sm">预览</router-link>
          <router-link v-if="canEdit(row)" :to="`/knowledge/edit/${row.id}`" class="btn-outline sm">编辑</router-link>
          <template v-if="Number(row.status) === 0">
            <button type="button" class="btn-primary sm" @click="onPublish(row)" :disabled="publishingId === row.id">
              提交审核
            </button>
          </template>
          <span v-else-if="Number(row.status) === 3" class="pending-label">待审核</span>
          <button v-if="Number(row.status) === 1" type="button" class="btn-warn sm" @click="onOffline(row)">下架</button>
          <button type="button" class="btn-danger sm" @click="onDelete(row)">删除</button>
        </div>
      </li>
    </ul>

    <PaginationBar v-model="pageNum" class="pager-bar" :total="total" :page-size="pageSize" @current-change="load" />
  </KnowledgeModuleShell>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getMyDrafts, getCategoryList, publishContent, offlineContent, deleteContent } from '@/api/content'
import { formatDateTime } from '@/utils/formatDateTime'
import PaginationBar from '@/components/PaginationBar.vue'
import KnowledgeModuleShell from '@/components/KnowledgeModuleShell.vue'

const crumbs = [
  { label: '首页', to: '/' },
  { label: '消防知识', to: '/knowledge' },
  { label: '我的知识' },
]

const list = ref([])
const categories = ref([])
const pageNum = ref(1)
const pageSize = 10
const total = ref(0)
const query = reactive({ categoryId: '', title: '' })
const publishingId = ref(null)

function isDraft(row) {
  return Number(row.status) === 0
}
function canEdit(row) {
  return Number(row.status) !== 3
}
function statusText(row) {
  const status = Number(row.status)
  if (status === 3) return '待审核'
  if (status === 1) return '已发布'
  if (status === 2) return '已下架'
  return '草稿'
}
function statusClass(row) {
  const status = Number(row.status)
  if (status === 3) return 'pending'
  if (status === 1) return 'published'
  if (status === 2) return 'offline'
  return 'draft'
}
function rejectReasonText(row) {
  const t = row.rejectReason ?? row.reject_reason
  return t && String(t).trim() ? String(t).trim() : ''
}

function onFilterChange() {
  pageNum.value = 1
  load()
}

async function load() {
  try {
    const data = await getMyDrafts({
      pageNum: pageNum.value,
      pageSize,
      categoryId: query.categoryId || undefined,
      title: query.title || undefined,
    })
    const records = data.records ?? data.list ?? []
    const totalVal = Number(data.total ?? data.totalCount ?? 0)
    if (records.length === 0 && totalVal > 0 && pageNum.value > 1 && (pageNum.value - 1) * pageSize >= totalVal) {
      pageNum.value -= 1
      return load()
    }
    list.value = records
    total.value = totalVal
  } catch (e) {
    console.error(e)
    alert(e.message || '加载我的知识失败')
  }
}

async function loadCategories() {
  try {
    const data = await getCategoryList()
    categories.value = Array.isArray(data) ? data : (data.list ?? [])
  } catch (_) {}
}

async function onPublish(row) {
  if (!confirm(`确定将《${row.title}》提交审核？提交后需管理员通过才会公开展示。`)) return
  publishingId.value = row.id
  try {
    await publishContent(row.id)
    alert('已提交审核，请等待管理员通过')
    await load()
  } catch (e) {
    console.error(e)
    alert(e.message || '提交失败')
  } finally {
    publishingId.value = null
  }
}

async function onOffline(row) {
  if (!confirm(`确定下架《${row.title}》？下架后用户端将不再展示。`)) return
  try {
    await offlineContent(row.id)
    alert('已下架')
    await load()
  } catch (e) {
    console.error(e)
    alert(e.message || '下架失败')
  }
}

async function onDelete(row) {
  if (!confirm(`确定删除《${row.title}》？删除后不可恢复。`)) return
  try {
    await deleteContent(row.id)
    alert('删除成功')
    await load()
  } catch (e) {
    console.error(e)
    alert(e.message || '删除失败')
  }
}

onMounted(() => {
  loadCategories()
  load()
})
</script>

<style scoped>
.page-head {
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(59, 130, 246, 0.12);
}

.page-head-row {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
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

.btn-primary {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 10px 20px;
  background: var(--client-primary);
  color: #fff;
  border: none;
  border-radius: 10px;
  font-size: 0.875rem;
  font-weight: 600;
  text-decoration: none;
  cursor: pointer;
  transition: background 0.15s;
  white-space: nowrap;
}

.btn-primary:hover {
  background: var(--client-primary-hover);
  color: #fff;
}

.btn-primary.sm {
  padding: 6px 14px;
  font-size: 0.8125rem;
}

.btn-primary:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.tip-block {
  margin: 0 0 16px;
  padding: 12px 14px;
  font-size: 0.8125rem;
  line-height: 1.55;
  color: var(--client-muted);
  background: rgba(224, 242, 254, 0.45);
  border: 1px solid rgba(59, 130, 246, 0.15);
  border-radius: 10px;
}

.toolbar {
  margin-bottom: 18px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.field-control {
  padding: 10px 12px;
  border: 1px solid rgba(59, 130, 246, 0.2);
  border-radius: 10px;
  font-size: 0.875rem;
  background: var(--client-surface);
  color: var(--client-text);
}

.flex-grow {
  flex: 1;
  min-width: 160px;
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
}

.btn-query:hover {
  background: var(--client-primary-hover);
}

.draft-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.draft-card {
  padding: 16px 18px;
  border-radius: var(--client-radius);
  border: 1px solid rgba(59, 130, 246, 0.12);
  background: rgba(59, 130, 246, 0.03);
  transition: box-shadow 0.15s, border-color 0.15s;
}

.draft-card:hover {
  border-color: rgba(59, 130, 246, 0.2);
  box-shadow: 0 4px 18px rgba(59, 130, 246, 0.08);
}

.draft-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 10px;
}

.draft-title {
  font-size: 1rem;
  font-weight: 700;
  color: var(--client-text);
  text-decoration: none;
}

.draft-title:hover {
  color: var(--client-primary);
}

.draft-cat {
  font-size: 0.75rem;
  color: var(--client-muted);
  padding: 2px 8px;
  border-radius: 6px;
  background: rgba(59, 130, 246, 0.08);
}

.status-tag {
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 600;
}

.status-tag.draft {
  background: rgba(100, 116, 139, 0.15);
  color: var(--client-muted);
}

.status-tag.pending {
  background: rgba(245, 158, 11, 0.18);
  color: #b45309;
}

.status-tag.published {
  background: rgba(34, 197, 94, 0.15);
  color: #15803d;
}

.status-tag.offline {
  background: rgba(248, 113, 113, 0.15);
  color: #b91c1c;
}

.draft-meta {
  margin-top: 8px;
  font-size: 0.8125rem;
  color: var(--client-muted);
  display: flex;
  flex-wrap: wrap;
  gap: 12px 16px;
}

.reject-line {
  margin: 10px 0 0;
  font-size: 0.8125rem;
  color: #dc2626;
  line-height: 1.45;
}

.draft-actions {
  margin-top: 14px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px 10px;
  align-items: center;
}

.btn-outline {
  display: inline-flex;
  align-items: center;
  padding: 6px 14px;
  border-radius: 8px;
  border: 1px solid rgba(59, 130, 246, 0.35);
  background: var(--client-surface);
  color: var(--client-primary-hover);
  font-size: 0.8125rem;
  text-decoration: none;
  cursor: pointer;
}

.btn-outline:hover {
  background: var(--client-accent-soft);
}

.btn-outline.sm {
  padding: 6px 12px;
}

.btn-warn {
  padding: 6px 14px;
  border-radius: 8px;
  border: 1px solid rgba(245, 158, 11, 0.45);
  background: rgba(254, 243, 199, 0.6);
  color: #b45309;
  font-size: 0.8125rem;
  cursor: pointer;
}

.btn-warn:hover {
  background: rgba(254, 243, 199, 0.95);
}

.btn-danger {
  padding: 6px 14px;
  border-radius: 8px;
  border: 1px solid rgba(248, 113, 113, 0.45);
  background: rgba(254, 226, 226, 0.5);
  color: #b91c1c;
  font-size: 0.8125rem;
  cursor: pointer;
}

.btn-danger:hover {
  background: rgba(254, 226, 226, 0.85);
}

.pending-label {
  font-size: 0.8125rem;
  color: var(--client-muted);
}

.pager-bar {
  margin-top: 22px;
}
</style>
