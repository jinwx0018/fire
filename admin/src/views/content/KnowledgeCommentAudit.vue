<template>
  <div class="page">
    <el-card>
      <template #header>
        <span>知识评论审核</span>
      </template>
      <el-form :inline="true" class="filters" @submit.prevent="loadList">
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="评论正文" clearable style="width: 180px" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部" style="width: 120px">
            <el-option label="显示" :value="1" />
            <el-option label="隐藏" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
        </el-form-item>
      </el-form>
      <el-table v-loading="loading" :data="list" stripe border style="width: 100%">
        <el-table-column
          type="index"
          label="序号"
          width="72"
          :index="(i) => (pageNum - 1) * pageSize + i + 1"
        />
        <el-table-column prop="contentTitle" label="知识标题" min-width="140" show-overflow-tooltip />
        <el-table-column prop="username" label="用户" width="100" show-overflow-tooltip />
        <el-table-column label="父评论引用" min-width="180">
          <template #default="{ row }">
            <span v-if="row.parentContentPreview" class="parent-preview">{{ row.parentUserName || '用户' }}：{{ row.parentContentPreview }}</span>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column label="内容" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            {{ commentListPreview(row.content) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            {{ row.status === 0 ? '隐藏' : '显示' }}
          </template>
        </el-table-column>
        <el-table-column label="时间" width="156">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="340" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="goCommentAuditDetail(row.id)">详情</el-button>
            <el-button link type="primary" @click="goEditContent(row.contentId)">编辑知识</el-button>
            <el-button v-if="row.status !== 0" link type="warning" @click="setRowStatus(row, 0)">隐藏</el-button>
            <el-button v-if="row.status === 0" link type="success" @click="setRowStatus(row, 1)">显示</el-button>
            <el-button link type="danger" @click="removeRow(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap">
        <PaginationBar
          v-model="pageNum"
          :total="total"
          :page-size="pageSize"
          @current-change="() => loadList()"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getContentCommentAdminPage,
  deleteContentComment,
  updateContentCommentStatus,
} from '@/api/content'
import PaginationBar from '@/components/PaginationBar.vue'
import { formatDateTime } from '@/utils/formatDateTime'

function commentListPreview(html) {
  if (html == null || html === '') return ''
  const s = String(html)
  if (!/<[a-z]/i.test(s)) return s.length > 100 ? `${s.slice(0, 100)}…` : s
  const t = s.replace(/<[^>]+>/g, ' ').replace(/\s+/g, ' ').trim()
  if (!t && /<img/i.test(s)) return '[图片]'
  return t.length > 100 ? `${t.slice(0, 100)}…` : t
}

const route = useRoute()
const router = useRouter()
const pageSize = 20

const list = ref([])
const total = ref(0)
const loading = ref(false)
const pageNum = ref(1)
const query = reactive({ contentId: '', keyword: '', status: '' })

function parseId(s) {
  const t = s?.trim()
  if (!t) return undefined
  const n = Number(t)
  return Number.isFinite(n) && n > 0 ? n : null
}

function search() {
  pageNum.value = 1
  loadList()
}

async function loadList(silent) {
  const rq = route.query
  if (rq.contentId != null && String(rq.contentId).trim() !== '') {
    query.contentId = String(rq.contentId)
  } else {
    query.contentId = ''
  }
  loading.value = true
  try {
    const cid = parseId(query.contentId)
    if (query.contentId?.trim() && cid == null) {
      if (!silent) ElMessage.warning('链接中的知识标识无效')
      loading.value = false
      return
    }
    const st = query.status === '' || query.status === null ? undefined : query.status
    const data = await getContentCommentAdminPage({
      pageNum: pageNum.value,
      pageSize,
      contentId: cid,
      keyword: query.keyword?.trim() || undefined,
      status: st,
    })
    list.value = data.records ?? data.list ?? []
    total.value = data.total ?? 0
  } catch (e) {
    if (!silent) ElMessage.error(e?.message || '加载失败')
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function goEditContent(contentId) {
  if (contentId == null) return
  router.push(`/content/edit/${contentId}`)
}

function goCommentAuditDetail(commentId) {
  if (commentId == null) return
  router.push({
    name: 'CommentAuditDetail',
    query: { type: 'knowledge', id: String(commentId) },
  })
}

function refreshList(opts = {}) {
  const silent = opts.silent === true
  loadList(silent)
}

const AUDIT_POLL_MS = 12000
let auditPollTimer = null

function onWinFocusAudit() {
  refreshList({ silent: true })
}

function onCommentAuditExternalRefresh() {
  refreshList({ silent: false })
}

async function setRowStatus(row, status) {
  try {
    await updateContentCommentStatus(row.id, { status })
    ElMessage.success(status === 1 ? '已显示' : '已隐藏')
    loadList()
  } catch (e) {
    ElMessage.error(e?.message || '操作失败')
  }
}

async function removeRow(row) {
  try {
    await ElMessageBox.confirm(`确定删除评论 #${row.id}？`, '确认', { type: 'warning' })
    await deleteContentComment(row.id)
    ElMessage.success('已删除')
    loadList()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e?.message || '删除失败')
  }
}

onMounted(() => {
  loadList()
  window.addEventListener('comment-audit-refresh', onCommentAuditExternalRefresh)
  auditPollTimer = window.setInterval(() => {
    refreshList({ silent: true })
  }, AUDIT_POLL_MS)
  window.addEventListener('focus', onWinFocusAudit)
})

onBeforeUnmount(() => {
  window.removeEventListener('comment-audit-refresh', onCommentAuditExternalRefresh)
  if (auditPollTimer) {
    clearInterval(auditPollTimer)
    auditPollTimer = null
  }
  window.removeEventListener('focus', onWinFocusAudit)
})
</script>

<style scoped>
.filters {
  margin-bottom: 16px;
}
.pagination-wrap {
  margin-top: 16px;
}
.parent-preview {
  font-size: 12px;
  color: var(--client-muted);
}
</style>
