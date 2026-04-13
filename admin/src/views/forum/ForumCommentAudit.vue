<template>
  <div class="page">
    <el-card>
      <template #header>
        <span>论坛评论</span>
      </template>
      <el-form :inline="true" class="filters" @submit.prevent="loadForum">
        <el-form-item label="帖子ID">
          <el-input v-model="forumQuery.postId" placeholder="可选" clearable style="width: 140px" @keyup.enter="searchForum" />
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="forumQuery.keyword" placeholder="评论正文" clearable style="width: 180px" @keyup.enter="searchForum" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="forumQuery.status" clearable placeholder="全部" style="width: 120px">
            <el-option label="显示" :value="1" />
            <el-option label="隐藏" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="searchForum">查询</el-button>
        </el-form-item>
      </el-form>
      <el-table v-loading="forumLoading" :data="forumList" stripe border style="width: 100%">
        <el-table-column
          type="index"
          label="序号"
          width="72"
          :index="(i) => (forumPageNum - 1) * pageSize + i + 1"
        />
        <el-table-column prop="postId" label="帖子ID" width="88" />
        <el-table-column prop="postTitle" label="帖子标题" min-width="140" show-overflow-tooltip />
        <el-table-column prop="userName" label="用户" width="100" show-overflow-tooltip />
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
        <el-table-column label="操作" width="380" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="goCommentAuditDetail('forum', row.id)">详情</el-button>
            <el-button link type="primary" @click="goForumPostFullView(row.postId)">帖子全文</el-button>
            <el-button link type="primary" @click="goForumPostAudit">帖子审核</el-button>
            <el-button v-if="row.status !== 0" link type="warning" @click="setForumStatus(row, 0)">隐藏</el-button>
            <el-button v-if="row.status === 0" link type="success" @click="setForumStatus(row, 1)">显示</el-button>
            <el-button link type="danger" @click="removeForum(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap">
        <PaginationBar
          v-model="forumPageNum"
          :total="forumTotal"
          :page-size="pageSize"
          @current-change="() => loadForum()"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getForumCommentAdminPage, deleteComment, updateForumCommentStatus } from '@/api/forum'
import PaginationBar from '@/components/PaginationBar.vue'
import { formatDateTime } from '@/utils/formatDateTime'

const route = useRoute()
const router = useRouter()
const pageSize = 20

const forumList = ref([])
const forumTotal = ref(0)
const forumLoading = ref(false)
const forumPageNum = ref(1)
const forumQuery = reactive({ postId: '', keyword: '', status: '' })

function parseId(s) {
  const t = s?.trim()
  if (!t) return undefined
  const n = Number(t)
  return Number.isFinite(n) && n > 0 ? n : null
}

function searchForum() {
  forumPageNum.value = 1
  loadForum()
}

async function loadForum(silent) {
  forumLoading.value = true
  try {
    const pid = parseId(forumQuery.postId)
    if (forumQuery.postId?.trim() && pid == null) {
      if (!silent) ElMessage.warning('帖子ID须为正整数')
      forumLoading.value = false
      return
    }
    const st = forumQuery.status === '' || forumQuery.status === null ? undefined : forumQuery.status
    const data = await getForumCommentAdminPage({
      pageNum: forumPageNum.value,
      pageSize,
      postId: pid,
      keyword: forumQuery.keyword?.trim() || undefined,
      status: st,
    })
    forumList.value = data.records ?? data.list ?? []
    forumTotal.value = data.total ?? 0
  } catch (e) {
    if (!silent) ElMessage.error(e?.message || '加载失败')
    forumList.value = []
    forumTotal.value = 0
  } finally {
    forumLoading.value = false
  }
}

function goCommentAuditDetail(type, commentId) {
  if (commentId == null) return
  router.push({
    name: 'CommentAuditDetail',
    query: { type, id: String(commentId) },
  })
}

function goForumPostFullView(postId) {
  if (postId == null) return
  router.push({
    name: 'ForumPostAuditView',
    query: { id: String(postId) },
  })
}

function goForumPostAudit() {
  router.push({ name: 'ForumAudit' })
}

function refreshList(opts = {}) {
  const silent = opts.silent === true
  loadForum(silent)
}

const AUDIT_POLL_MS = 12000
let auditPollTimer = null

function onWinFocusAudit() {
  refreshList({ silent: true })
}

function onCommentAuditExternalRefresh() {
  refreshList({ silent: false })
}

async function setForumStatus(row, status) {
  try {
    await updateForumCommentStatus(row.id, { status })
    ElMessage.success(status === 1 ? '已显示' : '已隐藏')
    loadForum()
  } catch (e) {
    ElMessage.error(e?.message || '操作失败')
  }
}

async function removeForum(row) {
  try {
    await ElMessageBox.confirm(`确定删除论坛评论 #${row.id}？`, '确认', { type: 'warning' })
    await deleteComment(row.id)
    ElMessage.success('已删除')
    loadForum()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e?.message || '删除失败')
  }
}

onMounted(() => {
  const q = route.query
  if (q.postId != null && String(q.postId).trim() !== '') {
    forumQuery.postId = String(q.postId)
  }
  loadForum()
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
