<template>
  <div class="page" v-loading="loading">
    <el-page-header @back="goBack">
      <template #content>
        <span class="ph-title">{{ titleText }}</span>
      </template>
    </el-page-header>

    <el-alert v-if="errorText" type="error" :title="errorText" show-icon class="err" />

    <template v-else-if="loaded">
      <div class="audit-flow">
        <section v-if="news" class="audit-panel client-news-preview-card">
          <h3 class="section-title">所属新闻 · 用户端样式预览</h3>
          <div class="client-news-preview">
          <p class="cn-admin-strip">
            <span>{{ newsStatusLabel(news.status) }}</span>
            <span v-if="news.publishTime">{{ formatDateTime(news.publishTime) }}</span>
          </p>
          <h2 class="cn-title">{{ news.title }}</h2>
          <p v-if="news.summary" class="cn-summary">{{ news.summary }}</p>
          <div class="cn-body" v-html="newsBodyHtml" />
          </div>
        </section>

        <section v-if="post" class="audit-panel client-forum-preview-card">
          <h3 class="section-title">所属帖子 · 用户端样式预览</h3>
          <div class="client-forum-preview">
          <p class="cf-admin-strip">
            <span>帖子 ID {{ post.id }}</span>
            <span>{{ postStatusLabel(post.status) }}</span>
          </p>
          <h2 class="cf-title">{{ post.title }}</h2>
          <div class="cf-author-bar">
            <div class="cf-avatar-wrap">
              <img v-if="postAuthorAvatarUrl" :src="postAuthorAvatarUrl" alt="" class="cf-avatar-img" />
              <span v-else class="cf-avatar-fallback">{{ postAuthorInitial }}</span>
            </div>
            <div class="cf-author-meta">
              <span class="cf-author-name">{{ post.userName || '用户' }}</span>
              <span class="cf-author-id">用户ID {{ post.userId ?? '—' }}</span>
            </div>
          </div>
          <div class="cf-meta">
            <span v-if="post.createTime">{{ formatDateTime(post.createTime) }}</span>
            <span>浏览 {{ post.viewCount ?? 0 }}</span>
            <span>点赞 {{ post.likeCount ?? 0 }}</span>
            <span>评论 {{ post.commentCount ?? 0 }}</span>
          </div>
          <div class="cf-body" v-html="auditPostBodyHtml" />
          </div>
        </section>

        <section v-if="knowledge" class="audit-panel knowledge-target-card">
          <h3 class="section-title">所属知识 · 用户端样式预览</h3>
          <div class="client-knowledge-preview">
          <h2 class="kp-title">{{ knowledge.title }}</h2>
          <div class="kp-meta">
            <span>{{ knowledgeStatusLabel(knowledge.status) }}</span>
            <span v-if="knowledge.createTime">{{ formatDateTime(knowledge.createTime) }}</span>
          </div>
          <p v-if="knowledge.summary" class="kp-summary">{{ knowledge.summary }}</p>
          <div class="kp-body" v-html="knowledgeBodyHtml" />
          </div>
        </section>

        <section v-if="parentComment" class="audit-panel">
          <h3 class="section-title">被回复的评论</h3>
          <p class="meta">
            <span>ID {{ parentComment.id }}</span>
            <span>{{ parentComment.username || parentComment.userName || '用户' }}</span>
            <span>状态 {{ parentComment.status === 0 ? '隐藏' : '显示' }}</span>
            <span v-if="parentComment.createTime">{{ formatDateTime(parentComment.createTime) }}</span>
          </p>
          <div
            class="parent-bubble comment-rich-html"
            v-html="richCommentHtml(parentComment.content)"
          />
        </section>

        <section class="audit-panel">
          <h3 class="section-title">当前审核评论</h3>
          <p class="meta">
            <span>ID {{ comment?.id }}</span>
            <span>用户 {{ comment?.username || comment?.userName || comment?.userId || '—' }}</span>
            <span>状态 {{ comment?.status === 0 ? '隐藏' : '显示' }}</span>
            <span>{{ comment?.createTime ? formatDateTime(comment.createTime) : '—' }}</span>
          </p>
          <div class="comment-bubble">
            <div class="text-box comment-rich comment-rich-html" v-html="richCommentHtml(comment?.content)" />
          </div>
          <div class="audit-actions">
            <el-button v-if="comment?.status !== 0" type="warning" plain @click="setStatus(0)">隐藏</el-button>
            <el-button v-if="comment?.status === 0" type="success" plain @click="setStatus(1)">显示</el-button>
            <el-button type="danger" plain @click="removeComment">删除</el-button>
          </div>
        </section>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { formatDateTime } from '@/utils/formatDateTime'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getNewsCommentAuditDetail, deleteNewsComment, updateNewsCommentStatus } from '@/api/news'
import { getForumCommentAuditDetail, deleteComment, updateForumCommentStatus } from '@/api/forum'
import {
  getContentCommentAuditDetail,
  deleteContentComment,
  updateContentCommentStatus,
} from '@/api/content'
import { getPostDisplayHtml, sanitizeHtml } from '@/utils/htmlContent'
import { resolveMediaUrl } from '@/utils/resolveMediaUrl'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const errorText = ref('')
const loaded = ref(false)
const kind = ref('') // 'news' | 'forum' | 'knowledge'
const comment = ref(null)
const parentComment = ref(null)
const news = ref(null)
const post = ref(null)
const knowledge = ref(null)

const titleText = computed(() => {
  if (kind.value === 'forum') return '论坛评论 · 审核详情'
  if (kind.value === 'knowledge') return '知识评论 · 审核详情'
  return '新闻评论 · 审核详情'
})

/** 与用户端知识详情一致：正文经 sanitizeHtml 后再 v-html */
const knowledgeBodyHtml = computed(() => sanitizeHtml(knowledge.value?.content || ''))

const newsBodyHtml = computed(() => sanitizeHtml(news.value?.content || ''))

const auditPostBodyHtml = computed(() => getPostDisplayHtml(post.value?.content || ''))

const postAuthorAvatarUrl = computed(() => {
  const a = post.value?.avatar
  return a ? resolveMediaUrl(a) : ''
})

const postAuthorInitial = computed(() => {
  const n = post.value?.userName || '用'
  return String(n).charAt(0).toUpperCase()
})

function postStatusLabel(s) {
  const m = { 0: '待审核', 1: '已通过', '-1': '已驳回' }
  return m[s] ?? String(s ?? '—')
}

function knowledgeStatusLabel(s) {
  const m = { 0: '草稿', 1: '已发布', 2: '已下架', 3: '待审核' }
  return m[s] ?? String(s ?? '—')
}

function newsStatusLabel(s) {
  const m = { 0: '草稿', 1: '已发布', 2: '已下架' }
  return m[s] ?? String(s ?? '—')
}

/** 论坛/知识评论正文：与客户端一致的 HTML 展示与安全过滤 */
function richCommentHtml(raw) {
  return getPostDisplayHtml(raw)
}

function listPathForKind() {
  const k = kind.value || (route.query.type || '').toString().toLowerCase()
  if (k === 'forum') return '/forum/comments'
  if (k === 'knowledge') return '/knowledge/comments'
  return '/news/comments'
}

function goBack() {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push(listPathForKind())
  }
}

let loadSeq = 0

async function load() {
  const seq = ++loadSeq
  loading.value = true
  errorText.value = ''
  loaded.value = false
  const type = (route.query.type || '').toString().toLowerCase()
  const id = Number(route.query.id)
  if ((type !== 'news' && type !== 'forum' && type !== 'knowledge') || !Number.isFinite(id) || id < 1) {
    errorText.value = '参数错误：需要 type=news|forum|knowledge 与 id=评论ID'
    if (seq === loadSeq) loading.value = false
    return
  }
  kind.value = type
  try {
    if (type === 'news') {
      const data = await getNewsCommentAuditDetail(id)
      if (seq !== loadSeq) return
      comment.value = data.comment ?? null
      parentComment.value = data.parentComment ?? null
      news.value = data.news ?? null
      post.value = null
      knowledge.value = null
    } else if (type === 'forum') {
      const data = await getForumCommentAuditDetail(id)
      if (seq !== loadSeq) return
      comment.value = data.comment ?? null
      parentComment.value = data.parentComment ?? null
      post.value = data.post ?? null
      news.value = null
      knowledge.value = null
    } else {
      const data = await getContentCommentAuditDetail(id)
      if (seq !== loadSeq) return
      comment.value = data.comment ?? null
      parentComment.value = data.parentComment ?? null
      knowledge.value = data.knowledge ?? null
      news.value = null
      post.value = null
    }
    loaded.value = true
  } catch (e) {
    if (seq !== loadSeq) return
    errorText.value = e?.message || '加载失败'
  } finally {
    if (seq === loadSeq) loading.value = false
  }
}

async function setStatus(status) {
  const id = comment.value?.id
  if (id == null) return
  try {
    if (kind.value === 'news') {
      await updateNewsCommentStatus(id, { status })
    } else if (kind.value === 'knowledge') {
      await updateContentCommentStatus(id, { status })
    } else {
      await updateForumCommentStatus(id, { status })
    }
    ElMessage.success(status === 1 ? '已显示' : '已隐藏')
    await load()
  } catch (e) {
    ElMessage.error(e?.message || '操作失败')
  }
}

async function removeComment() {
  const id = comment.value?.id
  if (id == null) return
  try {
    await ElMessageBox.confirm('确定删除该评论？', '确认', { type: 'warning' })
    if (kind.value === 'news') {
      await deleteNewsComment(id)
    } else if (kind.value === 'knowledge') {
      await deleteContentComment(id)
    } else {
      await deleteComment(id)
    }
    ElMessage.success('已删除')
    window.dispatchEvent(new Event('comment-audit-refresh'))
    router.push(listPathForKind())
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e?.message || '删除失败')
  }
}

watch(
  () => `${(route.query.type || '').toString().toLowerCase()}:${route.query.id || ''}`,
  () => load(),
  { immediate: true },
)
</script>

<style scoped>
.page {
  padding: 16px;
  max-width: 960px;
}
.audit-flow {
  margin-top: 16px;
  background: var(--client-surface);
  border: 1px solid rgba(59, 130, 246, 0.12);
  border-radius: var(--client-radius);
  padding: 14px 16px;
  box-shadow: 0 2px 16px rgba(59, 130, 246, 0.06);
}
.audit-panel + .audit-panel {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px dashed rgba(59, 130, 246, 0.18);
}
.section-title {
  margin: 0 0 10px;
  font-size: 15px;
  font-weight: 600;
  color: var(--client-text);
}
.ph-title {
  font-weight: 600;
}
.err {
  margin-top: 16px;
}
.meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 13px;
  color: var(--client-muted);
  margin: 0 0 12px;
}
.text-box {
  white-space: pre-wrap;
  line-height: 1.6;
  font-size: 14px;
  color: var(--client-text);
  margin-bottom: 12px;
}
.comment-bubble,
.parent-bubble {
  background: rgba(240, 247, 252, 0.55);
  border: 1px solid rgba(59, 130, 246, 0.12);
  border-radius: 10px;
  padding: 10px 12px;
}
.parent-bubble {
  color: var(--client-text);
  white-space: normal;
  word-break: break-word;
  line-height: 1.6;
  font-size: 14px;
}
.comment-rich {
  white-space: normal;
}
.comment-rich :deep(p) {
  margin: 0.35em 0;
}
.post-content {
  max-height: none;
}
.audit-actions {
  margin-top: 8px;
}
.target-title {
  margin: 0 0 8px;
  font-size: 18px;
}
.summary {
  color: var(--client-muted);
  margin: 0 0 12px;
  line-height: 1.5;
}
.html-body {
  border: 1px solid rgba(59, 130, 246, 0.12);
  border-radius: 10px;
  padding: 12px;
  margin-bottom: 12px;
  min-height: 80px;
  overflow-x: auto;
  line-height: 1.6;
  background: rgba(240, 247, 252, 0.35);
}
.html-body :deep(img) {
  max-width: 100%;
  height: auto;
}

/* 与用户端 client/src/views/knowledge/Detail.vue 阅读区对齐 */
.client-knowledge-preview {
  background: var(--client-surface);
  padding: 16px;
  border-radius: var(--client-radius);
  border: 1px solid rgba(59, 130, 246, 0.12);
}
.kp-title {
  margin: 0 0 12px;
  font-size: 1.35rem;
  font-weight: 600;
  color: var(--client-text);
  line-height: 1.35;
}
.kp-meta {
  font-size: 13px;
  color: var(--client-muted);
  margin-bottom: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
}
.kp-summary {
  color: var(--client-muted);
  margin: 0 0 12px;
  line-height: 1.5;
  font-size: 14px;
}
.kp-body {
  line-height: 1.6;
  color: var(--client-text);
  word-break: break-word;
}
.kp-body :deep(img) {
  max-width: 100%;
  height: auto;
  vertical-align: middle;
  border-radius: 6px;
  margin: 8px 0;
}
.kp-body :deep(p) {
  margin: 0.5em 0;
}
.kp-body :deep(p:first-child) {
  margin-top: 0;
}
.kp-body :deep(p:last-child) {
  margin-bottom: 0;
}
.kp-body :deep(table) {
  max-width: 100%;
  border-collapse: collapse;
}
.kp-body :deep(th),
.kp-body :deep(td) {
  border: 1px solid rgba(59, 130, 246, 0.15);
  padding: 6px 8px;
}

/* 与用户端新闻详情阅读区接近 */
.cn-admin-strip,
.cf-admin-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 12px;
  color: var(--client-muted);
  margin: 0 0 12px;
}
.client-news-preview,
.client-forum-preview {
  background: var(--client-surface);
  padding: 12px 16px 16px;
  border-radius: var(--client-radius);
  border: 1px solid rgba(59, 130, 246, 0.12);
}
.cn-title,
.cf-title {
  margin: 0 0 12px;
  font-size: 1.35rem;
  font-weight: 600;
  color: var(--client-text);
  line-height: 1.35;
}
.cn-summary {
  color: var(--client-muted);
  margin: 0 0 12px;
  line-height: 1.5;
  font-size: 14px;
}
.cn-body {
  line-height: 1.65;
  color: var(--client-text);
  word-break: break-word;
}
.cn-body :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: 6px;
}
.cf-author-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}
.cf-avatar-wrap {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  overflow: hidden;
  background: rgba(224, 242, 254, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
}
.cf-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.cf-avatar-fallback {
  font-size: 18px;
  font-weight: 600;
  color: var(--client-primary);
}
.cf-author-meta {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.cf-author-name {
  font-weight: 600;
  font-size: 15px;
}
.cf-author-id {
  font-size: 13px;
  color: var(--client-muted);
}
.cf-meta {
  color: var(--client-muted);
  font-size: 13px;
  margin-bottom: 12px;
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}
.cf-body {
  line-height: 1.65;
  word-break: break-word;
}
.cf-body :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: 6px;
}
</style>
