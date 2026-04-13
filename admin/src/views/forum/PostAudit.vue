<template>
  <div class="page">
    <h2>帖子审核</h2>
    <p class="hint">默认展示全部状态帖子；筛选「待审核」仅看未审。已通过帖子可在此删帖、管理评论；「查看全文」进入管理端帖子全文。</p>
    <div class="filters">
      <select v-model="query.status">
        <option value="">全部</option>
        <option value="0">待审核</option>
        <option value="1">已通过</option>
        <option value="-1">已驳回</option>
      </select>
      <input v-model.trim="query.keyword" type="search" placeholder="标题/正文关键词" class="keyword" />
      <button type="button" class="btn-query" @click="search">查询</button>
    </div>
    <table class="table">
      <thead>
        <tr>
          <th>序号</th>
          <th>标题</th>
          <th>状态</th>
          <th>发布时间</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(row, index) in list" :key="row.id">
          <td>{{ (pageNum - 1) * pageSize + index + 1 }}</td>
          <td>{{ row.title }}</td>
          <td>{{ statusText(row.status) }}</td>
          <td>{{ formatDateTime(row.createTime) }}</td>
          <td class="ops">
            <button type="button" class="link" @click="goPostFullView(row.id)">查看全文</button>
            <template v-if="row.status === 0">
              <button type="button" class="link" @click="audit(row.id, 1)">通过</button>
              <button type="button" class="link danger" @click="auditReject(row.id)">驳回</button>
            </template>
            <button type="button" class="link" @click="openComments(row)">评论管理</button>
            <button type="button" class="link danger" @click="removePost(row.id)">删帖</button>
          </td>
        </tr>
      </tbody>
    </table>

    <PaginationBar v-model="pageNum" class="pager-bar" :total="total" :page-size="pageSize" @current-change="() => load()" />

    <div v-if="commentDialog.open" class="dialog-mask" @click.self="closeComments">
      <div class="dialog dialog-wide">
        <h3>帖子 #{{ commentDialog.postId }} · 评论管理（含隐藏项）</h3>
        <p v-if="commentDialog.loading">加载中…</p>
        <ul v-else-if="commentDialog.list.length" class="comment-ul">
          <li v-for="c in commentDialog.list" :key="c.id">
            <div v-if="c.parentContentPreview" class="parent-quote">
              <span class="pq-label">回复 @{{ c.parentUserName || '用户' }}</span>
              <div class="pq-text">{{ c.parentContentPreview }}</div>
            </div>
            <div class="c-line">
              <span class="c-user">{{ c.userName || c.userId }}</span>
              <span class="c-time">{{ formatDateTime(c.createTime) }}</span>
              <span class="c-status" :class="c.status === 0 ? 'hidden' : ''">{{ c.status === 0 ? '隐藏' : '显示' }}</span>
              <button type="button" class="link" @click="goCommentAuditDetail(c.id)">详情</button>
              <button v-if="c.status !== 0" type="button" class="link warn" @click="setCommentVisible(c, 0)">隐藏</button>
              <button v-if="c.status === 0" type="button" class="link" @click="setCommentVisible(c, 1)">显示</button>
              <button type="button" class="link danger" @click="removeComment(c.id)">删除</button>
            </div>
            <div class="c-content comment-rich-html" v-html="commentRowHtml(c.content)"></div>
          </li>
        </ul>
        <p v-else class="empty">暂无评论</p>
        <button type="button" class="btn-close" @click="closeComments">关闭</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import {
  getPostPage,
  auditPost,
  deletePost,
  deleteComment,
  getForumCommentAdminPage,
  updateForumCommentStatus,
} from '@/api/forum'
import { formatDateTime } from '@/utils/formatDateTime'
import { getPostDisplayHtml } from '@/utils/htmlContent'
import PaginationBar from '@/components/PaginationBar.vue'

function commentRowHtml(raw) {
  return getPostDisplayHtml(raw || '')
}

const router = useRouter()

const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 10
/** 默认「全部」，避免仅有已通过帖子时列表为空 */
const query = reactive({ status: '', keyword: '' })

const commentDialog = reactive({
  open: false,
  postId: null,
  loading: false,
  list: [],
})

function statusText(s) {
  const m = { 0: '待审核', 1: '已通过', '-1': '已驳回' }
  return m[s] ?? '-'
}

function goPostFullView(postId) {
  router.push({ name: 'ForumPostAuditView', query: { id: String(postId) } })
}

function goCommentAuditDetail(commentId) {
  router.push({
    name: 'CommentAuditDetail',
    query: { type: 'forum', id: String(commentId) },
  })
}

function search() {
  pageNum.value = 1
  load()
}

const POLL_MS = 10000
let listPollTimer = null

async function load(silent) {
  try {
    const data = await getPostPage({
      pageNum: pageNum.value,
      pageSize,
      status: query.status === '' ? undefined : query.status,
      keyword: query.keyword || undefined,
    })
    list.value = data.records ?? data.list ?? []
    total.value = Number(data.total ?? 0)
  } catch (e) {
    if (!silent) console.error(e)
  }
}

function onWinFocus() {
  load(true)
}

async function audit(postId, status) {
  try {
    await auditPost(postId, { status })
    load()
  } catch (e) {
    alert(e.message || '操作失败')
  }
}

async function auditReject(postId) {
  const reason = window.prompt('请输入驳回理由（必填）', '')
  if (reason == null) return
  const trimmed = reason.trim()
  if (!trimmed) {
    alert('驳回理由不能为空')
    return
  }
  try {
    await auditPost(postId, { status: -1, rejectReason: trimmed })
    load()
  } catch (e) {
    alert(e.message || '操作失败')
  }
}

async function removePost(postId) {
  if (!window.confirm('确定删除该帖子？（软删）')) return
  try {
    await deletePost(postId)
    load()
    if (commentDialog.open && commentDialog.postId === postId) closeComments()
  } catch (e) {
    alert(e.message || '删除失败')
  }
}

async function reloadCommentDialog() {
  if (commentDialog.postId == null) return
  commentDialog.loading = true
  try {
    const data = await getForumCommentAdminPage({
      postId: commentDialog.postId,
      pageNum: 1,
      pageSize: 100,
    })
    commentDialog.list = data.records ?? data.list ?? []
  } catch (e) {
    alert(e.message || '加载评论失败')
    commentDialog.list = []
  } finally {
    commentDialog.loading = false
  }
}

async function openComments(row) {
  commentDialog.open = true
  commentDialog.postId = row.id
  commentDialog.list = []
  await reloadCommentDialog()
}

function closeComments() {
  commentDialog.open = false
  commentDialog.postId = null
  commentDialog.list = []
}

async function setCommentVisible(c, status) {
  try {
    await updateForumCommentStatus(c.id, { status })
    await reloadCommentDialog()
    load()
  } catch (e) {
    alert(e.message || '操作失败')
  }
}

async function removeComment(commentId) {
  if (!window.confirm('确定删除该评论？')) return
  try {
    await deleteComment(commentId)
    await reloadCommentDialog()
    load()
  } catch (e) {
    alert(e.message || '删除失败')
  }
}

function onForumRefresh() {
  load()
  if (commentDialog.open) reloadCommentDialog()
}

onMounted(() => {
  load()
  listPollTimer = window.setInterval(() => load(true), POLL_MS)
  window.addEventListener('focus', onWinFocus)
  window.addEventListener('forum-post-audit-refresh', onForumRefresh)
})

onBeforeUnmount(() => {
  if (listPollTimer) {
    window.clearInterval(listPollTimer)
    listPollTimer = null
  }
  window.removeEventListener('focus', onWinFocus)
  window.removeEventListener('forum-post-audit-refresh', onForumRefresh)
})
</script>

<style scoped>
.hint {
  font-size: 13px;
  color: var(--client-muted);
  margin: 0 0 12px;
  line-height: 1.5;
}
.filters {
  margin-bottom: 16px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}
.keyword {
  padding: 6px 10px;
  border: 1px solid rgba(59, 130, 246, 0.22);
  border-radius: 10px;
  min-width: 180px;
}
.btn-query {
  padding: 6px 14px;
  border: 1px solid rgba(59, 130, 246, 0.25);
  border-radius: 10px;
  background: var(--client-surface);
  color: var(--client-text);
  cursor: pointer;
}
.table {
  width: 100%;
  border-collapse: collapse;
}
.table th,
.table td {
  padding: 10px;
  border: 1px solid rgba(59, 130, 246, 0.12);
  vertical-align: top;
}
.ops {
  white-space: normal;
}
.table .link {
  background: none;
  border: none;
  color: var(--client-primary);
  cursor: pointer;
  margin-right: 8px;
}
.table .link.danger {
  color: #dc2626;
}
.table .link.warn {
  color: #f59e0b;
}
.pager-bar {
  margin-top: 16px;
}
.dialog-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}
.dialog {
  background: var(--client-surface);
  padding: 20px;
  border-radius: 12px;
  border: 1px solid rgba(59, 130, 246, 0.14);
  max-width: 560px;
  width: 90%;
  max-height: 80vh;
  overflow: auto;
}
.dialog-wide {
  max-width: 720px;
}
.dialog h3 {
  margin-top: 0;
}
.comment-ul {
  list-style: none;
  padding: 0;
  margin: 0 0 16px;
}
.comment-ul li {
  border-bottom: 1px solid rgba(59, 130, 246, 0.1);
  padding: 10px 0;
}
.c-line {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  font-size: 13px;
}
.c-user {
  font-weight: 600;
}
.c-time {
  color: var(--client-muted);
}
.c-status {
  font-size: 12px;
  color: #16a34a;
}
.c-status.hidden {
  color: var(--client-muted);
}
.c-content {
  margin-top: 6px;
  white-space: normal;
  word-break: break-word;
  line-height: 1.55;
  font-size: 14px;
}
.parent-quote {
  background: rgba(224, 242, 254, 0.5);
  border-left: 3px solid rgba(59, 130, 246, 0.45);
  padding: 8px 10px;
  border-radius: 6px;
  margin-bottom: 8px;
  font-size: 13px;
}
.pq-label {
  color: var(--client-primary);
  font-weight: 600;
  display: block;
  margin-bottom: 4px;
}
.pq-text {
  color: var(--client-muted);
  line-height: 1.45;
  white-space: pre-wrap;
}
.empty {
  color: var(--client-muted);
}
.btn-close {
  padding: 8px 16px;
  border: 1px solid rgba(59, 130, 246, 0.22);
  border-radius: 10px;
  background: var(--client-surface);
  color: var(--client-text);
  cursor: pointer;
}
</style>
