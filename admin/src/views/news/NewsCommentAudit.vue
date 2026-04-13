<template>
  <div class="page">
    <el-card>
      <template #header>
        <span>新闻评论审核</span>
      </template>
      <el-form :inline="true" class="filters" @submit.prevent="loadNews">
        <el-form-item label="关键词">
          <el-input v-model="newsQuery.keyword" placeholder="评论正文" clearable style="width: 180px" @keyup.enter="searchNews" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="newsQuery.status" clearable placeholder="全部" style="width: 120px">
            <el-option label="显示" :value="1" />
            <el-option label="隐藏" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="searchNews">查询</el-button>
        </el-form-item>
      </el-form>
      <el-table v-loading="newsLoading" :data="newsList" stripe border style="width: 100%">
        <el-table-column
          type="index"
          label="序号"
          width="72"
          :index="(i) => (newsPageNum - 1) * pageSize + i + 1"
        />
        <el-table-column prop="newsTitle" label="新闻标题" min-width="140" show-overflow-tooltip />
        <el-table-column prop="username" label="用户" width="100" show-overflow-tooltip />
        <el-table-column label="父评论引用" min-width="180">
          <template #default="{ row }">
            <span v-if="row.parentContentPreview" class="parent-preview">{{ row.parentUserName || '用户' }}：{{ row.parentContentPreview }}</span>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="内容" min-width="200" show-overflow-tooltip />
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
            <el-button link type="primary" @click="goCommentAuditDetail('news', row.id)">详情</el-button>
            <el-button link type="primary" @click="goEditNews(row.newsId)">编辑新闻</el-button>
            <el-button v-if="row.status !== 0" link type="warning" @click="setNewsStatus(row, 0)">隐藏</el-button>
            <el-button v-if="row.status === 0" link type="success" @click="setNewsStatus(row, 1)">显示</el-button>
            <el-button link type="danger" @click="removeNews(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap">
        <PaginationBar
          v-model="newsPageNum"
          :total="newsTotal"
          :page-size="pageSize"
          @current-change="() => loadNews()"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getNewsCommentAdminPage, deleteNewsComment, updateNewsCommentStatus } from '@/api/news'
import PaginationBar from '@/components/PaginationBar.vue'
import { formatDateTime } from '@/utils/formatDateTime'

const route = useRoute()
const router = useRouter()
const pageSize = 20

const newsList = ref([])
const newsTotal = ref(0)
const newsLoading = ref(false)
const newsPageNum = ref(1)
const newsQuery = reactive({ newsId: '', keyword: '', status: '' })

function parseId(s) {
  const t = s?.trim()
  if (!t) return undefined
  const n = Number(t)
  return Number.isFinite(n) && n > 0 ? n : null
}

function searchNews() {
  newsPageNum.value = 1
  loadNews()
}

async function loadNews(silent) {
  const rq = route.query
  if (rq.newsId != null && String(rq.newsId).trim() !== '') {
    newsQuery.newsId = String(rq.newsId)
  } else {
    newsQuery.newsId = ''
  }
  newsLoading.value = true
  try {
    const nid = parseId(newsQuery.newsId)
    if (newsQuery.newsId?.trim() && nid == null) {
      if (!silent) ElMessage.warning('链接中的新闻标识无效')
      newsLoading.value = false
      return
    }
    const st = newsQuery.status === '' || newsQuery.status === null ? undefined : newsQuery.status
    const data = await getNewsCommentAdminPage({
      pageNum: newsPageNum.value,
      pageSize,
      newsId: nid,
      keyword: newsQuery.keyword?.trim() || undefined,
      status: st,
    })
    newsList.value = data.records ?? data.list ?? []
    newsTotal.value = data.total ?? 0
  } catch (e) {
    if (!silent) ElMessage.error(e?.message || '加载失败')
    newsList.value = []
    newsTotal.value = 0
  } finally {
    newsLoading.value = false
  }
}

function goEditNews(newsId) {
  if (newsId == null) return
  router.push(`/news/edit/${newsId}`)
}

function goCommentAuditDetail(type, commentId) {
  if (commentId == null) return
  router.push({
    name: 'CommentAuditDetail',
    query: { type, id: String(commentId) },
  })
}

function refreshList(opts = {}) {
  const silent = opts.silent === true
  loadNews(silent)
}

const AUDIT_POLL_MS = 12000
let auditPollTimer = null

function onWinFocusAudit() {
  refreshList({ silent: true })
}

function onCommentAuditExternalRefresh() {
  refreshList({ silent: false })
}

async function setNewsStatus(row, status) {
  try {
    await updateNewsCommentStatus(row.id, { status })
    ElMessage.success(status === 1 ? '已显示' : '已隐藏')
    loadNews()
  } catch (e) {
    ElMessage.error(e?.message || '操作失败')
  }
}

async function removeNews(row) {
  try {
    await ElMessageBox.confirm(`确定删除评论 #${row.id}？`, '确认', { type: 'warning' })
    await deleteNewsComment(row.id)
    ElMessage.success('已删除')
    loadNews()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e?.message || '删除失败')
  }
}

onMounted(() => {
  loadNews()
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
