<template>
  <div class="page" v-loading="loading">
    <el-page-header @back="goBack">
      <template #content>
        <span class="ph-title">帖子审核 · 用户端样式预览</span>
      </template>
    </el-page-header>

    <el-alert v-if="errorText" type="error" :title="errorText" show-icon class="err" />

    <div v-else-if="post" class="client-forum-wrap">
      <div class="toolbar">
        <button type="button" class="back" @click="goBack">← 返回列表</button>
      </div>
      <p class="admin-strip">
        <span>帖子 ID {{ post.id }}</span>
        <span>{{ statusText(post.status) }}</span>
      </p>
      <h2 class="title">{{ post.title }}</h2>
      <div class="author-bar">
        <div class="avatar-wrap">
          <img v-if="authorAvatarUrl" :src="authorAvatarUrl" alt="" class="avatar-img" />
          <span v-else class="avatar-fallback">{{ authorInitial }}</span>
        </div>
        <div class="author-meta">
          <span class="author-name">{{ post.userName || '用户' }}</span>
          <span class="author-id">用户ID {{ post.userId ?? '—' }}</span>
        </div>
      </div>
      <div class="meta">
        <span v-if="post.createTime">{{ formatDateTime(post.createTime) }}</span>
        <span>浏览 {{ post.viewCount ?? 0 }}</span>
        <span>点赞 {{ post.likeCount ?? 0 }}</span>
        <span>评论 {{ post.commentCount ?? 0 }}</span>
      </div>
      <p v-if="post.rejectReason" class="status-banner">驳回原因：{{ post.rejectReason }}</p>
      <div class="post-body" v-html="postBodyHtml" />

      <div class="audit-actions">
        <el-button type="primary" plain @click="openCommentAudit">论坛评论审核（此帖）</el-button>
        <el-button v-if="post.status === 0" type="success" @click="audit(1)">通过</el-button>
        <el-button v-if="post.status === 0" type="warning" @click="auditReject">驳回</el-button>
        <el-button type="danger" plain @click="removePost">删除帖子</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminForumPostDetail, auditPost, deletePost } from '@/api/forum'
import { formatDateTime } from '@/utils/formatDateTime'
import { getPostDisplayHtml } from '@/utils/htmlContent'
import { resolveMediaUrl } from '@/utils/resolveMediaUrl'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const errorText = ref('')
const post = ref(null)

const postBodyHtml = computed(() => getPostDisplayHtml(post.value?.content || ''))

const authorAvatarUrl = computed(() => {
  const a = post.value?.avatar
  return a ? resolveMediaUrl(a) : ''
})

const authorInitial = computed(() => {
  const n = post.value?.userName || '用'
  return String(n).charAt(0).toUpperCase()
})

function statusText(s) {
  const m = { 0: '待审核', 1: '已通过', '-1': '已驳回' }
  return m[s] ?? '-'
}

function goBack() {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/forum/posts')
  }
}

function openCommentAudit() {
  const id = post.value?.id
  if (id == null) return
  router.push({ name: 'ForumCommentAudit', query: { postId: String(id) } })
}

async function load() {
  loading.value = true
  errorText.value = ''
  post.value = null
  const id = Number(route.query.id)
  if (!Number.isFinite(id) || id < 1) {
    errorText.value = '缺少有效帖子 id'
    loading.value = false
    return
  }
  try {
    post.value = await getAdminForumPostDetail(id)
  } catch (e) {
    errorText.value = e?.message || '加载失败'
  } finally {
    loading.value = false
  }
}

async function audit(status) {
  try {
    await auditPost(post.value.id, { status })
    ElMessage.success('已更新')
    await load()
    window.dispatchEvent(new Event('forum-post-audit-refresh'))
  } catch (e) {
    ElMessage.error(e?.message || '操作失败')
  }
}

async function auditReject() {
  try {
    const { value } = await ElMessageBox.prompt('请输入驳回理由', '驳回', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputPattern: /.+/,
      inputErrorMessage: '理由不能为空',
    })
    const reason = value?.trim()
    if (!reason) return
    await auditPost(post.value.id, { status: -1, rejectReason: reason })
    ElMessage.success('已驳回')
    await load()
    window.dispatchEvent(new Event('forum-post-audit-refresh'))
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e?.message || '操作失败')
  }
}

async function removePost() {
  try {
    await ElMessageBox.confirm('确定删除该帖子（软删）？', '确认', { type: 'warning' })
    await deletePost(post.value.id)
    ElMessage.success('已删除')
    window.dispatchEvent(new Event('forum-post-audit-refresh'))
    router.push({ name: 'ForumAudit' })
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e?.message || '删除失败')
  }
}

onMounted(load)
watch(
  () => route.query.id,
  () => load(),
)
</script>

<style scoped>
.page {
  padding: 16px;
  max-width: 800px;
}
.ph-title {
  font-weight: 600;
}
.err {
  margin-top: 16px;
}
.client-forum-wrap {
  margin-top: 16px;
  background: var(--client-surface);
  padding: 16px;
  border-radius: var(--client-radius);
  border: 1px solid rgba(59, 130, 246, 0.12);
  box-shadow: 0 2px 16px rgba(59, 130, 246, 0.06);
}
.toolbar {
  margin-bottom: 12px;
}
.back {
  background: none;
  border: none;
  padding: 0;
  color: var(--client-primary);
  cursor: pointer;
  font-size: 14px;
}
.admin-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 12px;
  color: var(--client-muted);
  margin: 0 0 10px;
}
.title {
  margin: 0 0 12px;
  font-size: 1.35rem;
  font-weight: 600;
  line-height: 1.35;
  color: var(--client-text);
}
.author-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}
.avatar-wrap {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
  background: rgba(224, 242, 254, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
}
.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.avatar-fallback {
  font-size: 18px;
  font-weight: 600;
  color: var(--client-primary);
}
.author-meta {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.author-name {
  font-weight: 600;
  font-size: 15px;
}
.author-id {
  font-size: 13px;
  color: var(--client-muted);
}
.meta {
  color: var(--client-muted);
  font-size: 13px;
  margin-bottom: 12px;
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}
.status-banner {
  background: var(--client-accent-soft);
  border: 1px solid rgba(59, 130, 246, 0.28);
  color: var(--client-primary);
  padding: 10px 12px;
  border-radius: 10px;
  margin-bottom: 12px;
  font-size: 14px;
}
.post-body {
  line-height: 1.65;
  word-break: break-word;
  margin-bottom: 20px;
}
.post-body :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: 6px;
}
.post-body :deep(p) {
  margin: 0.5em 0;
}
.post-body :deep(p:first-child) {
  margin-top: 0;
}
.post-body :deep(p:last-child) {
  margin-bottom: 0;
}
.audit-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding-top: 16px;
  border-top: 1px solid rgba(59, 130, 246, 0.1);
}
</style>
