<template>
  <KnowledgeModuleShell :crumbs="crumbs">
    <div v-if="detail" class="news-detail">
      <div class="news-top-actions">
        <router-link to="/news" class="back-btn">
          <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
            <line x1="19" y1="12" x2="5" y2="12" />
            <polyline points="12 19 5 12 12 5" />
          </svg>
          返回列表
        </router-link>
      </div>

      <article class="news-article">
        <img
          v-if="detail.coverUrl"
          class="news-cover"
          :src="resolveMediaUrl(detail.coverUrl)"
          alt=""
        />
        <header class="news-article-head">
          <h1 class="news-article-title">{{ detail.title }}</h1>
          <div class="news-meta-row">
            <span v-if="displayCategory" class="meta-pill meta-pill--cat">{{ displayCategory }}</span>
            <span class="meta-pill">{{ detail.region || '—' }}</span>
            <span class="meta-pill">{{ formatDateTime(detail.publishTime) }}</span>
            <span class="meta-pill urgency" :class="'u' + (detail.urgencyLevel || 1)">{{ urgencyLabel(detail.urgencyLevel) }}</span>
            <span v-if="detail.publisherName" class="meta-pill">发布人 {{ detail.publisherName }}</span>
            <span v-if="detail.viewCount != null" class="meta-pill">浏览 {{ detail.viewCount }}</span>
            <span v-if="detail.likeCount != null" class="meta-pill">点赞 {{ detail.likeCount }}</span>
          </div>
        </header>

        <div class="news-interactions">
          <button
            type="button"
            class="int-btn int-btn--like"
            :class="{ 'is-on': detail.liked }"
            :disabled="!userStore.isLoggedIn || likeLoading"
            @click="onToggleLike"
          >
            <svg
              class="int-icon"
              viewBox="0 0 24 24"
              :fill="detail.liked ? 'currentColor' : 'none'"
              stroke="currentColor"
              stroke-width="2"
              aria-hidden="true"
            >
              <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
            </svg>
            {{ detail.liked ? '已赞' : '点赞' }}
          </button>
          <button
            v-if="userStore.isLoggedIn"
            type="button"
            class="int-btn int-btn--collect"
            :class="{ 'is-on': detail.collected }"
            :disabled="collectLoading"
            @click="onToggleCollect"
          >
            <svg
              class="int-icon"
              viewBox="0 0 24 24"
              :fill="detail.collected ? 'currentColor' : 'none'"
              stroke="currentColor"
              stroke-width="2"
              aria-hidden="true"
            >
              <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z" />
            </svg>
            {{ detail.collected ? '已收藏' : '收藏' }}
          </button>
          <button type="button" class="int-btn int-btn--share" @click="onShareNews">
            <svg class="int-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
              <circle cx="18" cy="5" r="3" />
              <circle cx="6" cy="12" r="3" />
              <circle cx="18" cy="19" r="3" />
              <line x1="8.59" y1="13.51" x2="15.42" y2="17.49" />
              <line x1="15.41" y1="6.51" x2="8.59" y2="10.49" />
            </svg>
            分享
          </button>
          <span v-if="!userStore.isLoggedIn" class="int-hint">登录后可点赞、收藏、评论</span>
        </div>

        <p v-if="detail.summary" class="news-lead">{{ detail.summary }}</p>
        <div class="news-body rich-html" v-html="safeContent"></div>
      </article>

      <section v-if="relatedList.length" class="related-section">
        <h2 class="section-title">相关新闻</h2>
        <ul class="related-list">
          <li v-for="r in relatedList" :key="r.id" class="related-item">
            <router-link :to="`/news/${r.id}`" class="related-link">{{ r.title }}</router-link>
            <span class="related-sub">{{ r.publishTime ? formatDateTime(r.publishTime) : '' }}</span>
          </li>
        </ul>
      </section>

      <section class="comments-section">
        <div class="comments-header">
          <h2 class="section-title">评论</h2>
          <CommentSortBar :active-sort="commentSort" @select="setCommentSort" />
        </div>

        <div v-if="userStore.isLoggedIn" class="comment-input-card">
          <ForumCommentEditor v-model="rootCommentDraft" placeholder="写下你的看法…" />
          <button type="button" class="submit-comment-btn" :disabled="commentSubmitting" @click="submitRootComment">
            {{ commentSubmitting ? '发送中…' : '发表评论' }}
          </button>
        </div>
        <p v-else class="muted">请先登录后参与评论。</p>

        <CommentThreadNodes
          v-if="displayCommentTreeRoots.length"
          :nodes="displayCommentTreeRoots"
          :item-class="'comment-li'"
          root-list-class="comment-list"
          :threshold="COMMENT_REPLY_BRANCH_THRESHOLD"
        >
          <template #row="{ node: c }">
            <div v-if="c.parentId" class="reply-line">
              <span class="reply-line-at">@{{ replyAtUsername(c, commentList) }}</span>
              <span class="reply-line-snippet">{{ replyQuoteText(c, commentList) }}</span>
            </div>
            <div class="c-head">
              <strong>{{ c.username || c.userName || '用户' }}</strong>
              <span class="t">{{ formatDateTime(c.createTime) }}</span>
              <button
                v-if="userStore.isLoggedIn"
                type="button"
                class="linkish"
                :disabled="commentLikeBusyId === c.id"
                @click="toggleNewsCommentLike(c)"
              >
                {{ c.liked ? '已赞' : '赞' }} {{ c.likeCount ?? 0 }}
              </button>
              <button v-if="userStore.isLoggedIn" type="button" class="linkish" @click="toggleReply(c.id)">回复</button>
              <button
                v-if="userStore.isLoggedIn && isCommentOwner(c)"
                type="button"
                class="linkish danger"
                @click="removeNewsComment(c)"
              >
                删除
              </button>
            </div>
            <div class="c-body comment-rich-html" v-html="commentDisplayHtml(c.content)" />
            <div v-if="replyTo === c.id" class="reply-box">
              <ForumCommentEditor v-model="replyText" placeholder="回复…" />
              <div class="reply-actions">
                <button type="button" :disabled="commentSubmitting" @click="sendReply(c.id)">发送</button>
                <button type="button" class="secondary" @click="cancelReply">取消</button>
              </div>
            </div>
          </template>
        </CommentThreadNodes>

        <div v-if="commentRows.length && showCommentListFoldToggle" class="comment-list-fold-bar">
          <button
            v-if="!commentListExpanded"
            type="button"
            class="comment-list-fold-btn"
            @click="commentListExpanded = true"
          >
            展开全部 {{ commentRows.length }} 条评论
          </button>
          <button v-else type="button" class="comment-list-fold-btn" @click="commentListExpanded = false">收起评论列表</button>
        </div>

        <p v-if="!commentRows.length && !commentsLoading" class="empty-c">暂无评论</p>
        <p v-if="commentsLoading" class="loading-c">评论加载中…</p>

        <PaginationBar
          v-model="commentPage"
          class="comment-pager"
          :total="commentTotal"
          :page-size="commentPageSize"
          @current-change="() => loadComments(false)"
        />
      </section>
    </div>

    <div v-else-if="loadError" class="news-state news-state--error">
      <p>{{ loadError }}</p>
      <router-link to="/news" class="state-link">返回新闻列表</router-link>
    </div>

    <div v-else class="news-state">加载中…</div>
  </KnowledgeModuleShell>
</template>

<script setup>
import { ref, computed, watch, onMounted, nextTick, provide, reactive } from 'vue'
import { useRoute } from 'vue-router'
import { storeToRefs } from 'pinia'
import {
  getNewsDetail,
  getNewsComments,
  postNewsComment,
  toggleNewsLike,
  likeNewsComment,
  deleteNewsComment,
  collectNews,
  uncollectNews,
} from '@/api/news'
import { useUserStore } from '@/stores/user'
import ForumCommentEditor from '@/components/ForumCommentEditor.vue'
import { sanitizeNewsHtml } from '@/utils/sanitizeNewsHtml'
import { getPostDisplayHtml, isEmptyHtml } from '@/utils/htmlContent'
import { resolveMediaUrl } from '@/utils/resolveMediaUrl'
import { formatDateTime } from '@/utils/formatDateTime'
import PaginationBar from '@/components/PaginationBar.vue'
import CommentSortBar from '@/components/CommentSortBar.vue'
import CommentThreadNodes from '@/components/CommentThreadNodes.vue'
import KnowledgeModuleShell from '@/components/KnowledgeModuleShell.vue'
import { replyAtUsername, replyQuoteText } from '@/utils/replyContext'
import {
  buildCommentTree,
  flattenCommentTree,
  COMMENT_LIST_COLLAPSE_THRESHOLD,
  COMMENT_REPLY_BRANCH_KEY,
  COMMENT_REPLY_BRANCH_THRESHOLD,
  filterTreeByIdSet,
} from '@/utils/commentTree'

const route = useRoute()
const userStore = useUserStore()
const { isLoggedIn } = storeToRefs(userStore)

const detail = ref(null)
const loadError = ref('')
const likeLoading = ref(false)
const collectLoading = ref(false)
const rootCommentDraft = ref('')
const replyTo = ref(null)
const replyText = ref('')
const commentSubmitting = ref(false)
const commentLikeBusyId = ref(null)
const commentList = ref([])
const commentSort = ref('time')
const commentPage = ref(1)
const commentPageSize = 10
const commentTotal = ref(0)
const commentsLoading = ref(false)

const commentRows = computed(() =>
  flattenCommentTree(buildCommentTree(commentList.value, commentSort.value)),
)

const commentTreeRoots = computed(() => buildCommentTree(commentList.value, commentSort.value))

const commentListExpanded = ref(false)

const visibleCommentRows = computed(() => {
  const rows = commentRows.value
  const th = COMMENT_LIST_COLLAPSE_THRESHOLD
  if (commentListExpanded.value || rows.length <= th) {
    return rows
  }
  return rows.slice(0, th)
})

const replyBranchExpanded = reactive({})
provide(COMMENT_REPLY_BRANCH_KEY, replyBranchExpanded)

const displayCommentTreeRoots = computed(() => {
  const roots = commentTreeRoots.value
  const rows = commentRows.value
  const th = COMMENT_LIST_COLLAPSE_THRESHOLD
  if (commentListExpanded.value || rows.length <= th) return roots
  const idSet = new Set(visibleCommentRows.value.map((c) => c.id))
  return filterTreeByIdSet(roots, idSet)
})

const showCommentListFoldToggle = computed(
  () => commentRows.value.length > COMMENT_LIST_COLLAPSE_THRESHOLD,
)

watch([commentPage, commentSort], () => {
  commentListExpanded.value = false
  for (const k of Object.keys(replyBranchExpanded)) delete replyBranchExpanded[k]
})

function commentDisplayHtml(raw) {
  return getPostDisplayHtml(raw || '')
}

const id = computed(() => route.params.id)

const safeContent = computed(() => sanitizeNewsHtml(detail.value?.content))

const relatedList = computed(() => {
  const r = detail.value?.related
  return Array.isArray(r) ? r : []
})

const displayCategory = computed(() => {
  const d = detail.value
  if (!d) return ''
  return d.categoryName || d.category || ''
})

const crumbs = computed(() => {
  const base = [
    { label: '首页', to: '/home' },
    { label: '新闻', to: '/news' },
  ]
  if (!detail.value) return base
  const title = detail.value.title || '正文'
  const short = title.length > 22 ? `${title.slice(0, 22)}…` : title
  return [...base, { label: short, titleAttr: title }]
})

function urgencyLabel(level) {
  const m = { 1: '低', 2: '中', 3: '高' }
  return m[level] || ''
}

async function loadDetail() {
  detail.value = null
  loadError.value = ''
  try {
    detail.value = await getNewsDetail(id.value)
    if (detail.value == null) {
      loadError.value = '新闻不存在或已下架'
    }
  } catch (e) {
    loadError.value = e?.message || '加载失败'
  }
}

function setCommentSort(mode) {
  if (commentSort.value === mode) return
  commentSort.value = mode
  loadComments(true)
}

async function loadComments(reset = true) {
  if (!id.value) return
  if (reset) {
    commentPage.value = 1
  }
  commentsLoading.value = true
  try {
    const data = await getNewsComments(id.value, {
      pageNum: commentPage.value,
      pageSize: commentPageSize,
      sortBy: commentSort.value === 'hot' ? 'hot' : 'time',
    })
    const records = data.records ?? data.list ?? []
    commentTotal.value = Number(data.total ?? records.length)
    commentList.value = records
  } catch (_) {
    if (reset) {
      commentList.value = []
      commentTotal.value = 0
    }
  } finally {
    commentsLoading.value = false
  }
  await nextTick()
  expandCommentListIfNeededForQuery()
  scrollToCommentFromQuery()
}

function expandCommentListIfNeededForQuery() {
  const cid = route.query?.commentId
  if (!cid) return
  const rows = commentRows.value
  const th = COMMENT_LIST_COLLAPSE_THRESHOLD
  const idx = rows.findIndex((x) => String(x.id) === String(cid))
  if (idx >= th) {
    commentListExpanded.value = true
  }
}

function scrollToCommentFromQuery() {
  const cid = route.query?.commentId
  if (!cid) return
  const el = document.getElementById(`comment-${cid}`)
  if (!el) return
  el.scrollIntoView({ behavior: 'smooth', block: 'center' })
  el.classList.remove('comment-locate-flash')
  void el.offsetWidth
  el.classList.add('comment-locate-flash')
  setTimeout(() => el.classList.remove('comment-locate-flash'), 2600)
}

function isCommentOwner(c) {
  const uid = userStore.user?.userId ?? userStore.user?.id
  return uid != null && String(c?.userId) === String(uid)
}

async function onToggleLike() {
  if (!isLoggedIn.value || likeLoading.value) return
  likeLoading.value = true
  try {
    const res = await toggleNewsLike(id.value)
    if (detail.value && res) {
      detail.value = {
        ...detail.value,
        liked: !!res.liked,
        likeCount: res.likeCount != null ? Number(res.likeCount) : detail.value.likeCount,
      }
    }
  } catch (e) {
    window.alert(e?.message || '操作失败')
  } finally {
    likeLoading.value = false
  }
}

async function onToggleCollect() {
  if (!isLoggedIn.value || collectLoading.value) return
  collectLoading.value = true
  try {
    if (detail.value?.collected) {
      await uncollectNews(id.value)
      detail.value = { ...detail.value, collected: false }
    } else {
      await collectNews(id.value)
      detail.value = { ...detail.value, collected: true }
    }
  } catch (e) {
    window.alert(e?.message || '收藏操作失败')
  } finally {
    collectLoading.value = false
  }
}

function onShareNews() {
  const url = `${window.location.origin}${window.location.pathname}#/news/${id.value}`
  if (navigator.clipboard?.writeText) {
    navigator.clipboard.writeText(url).then(() => window.alert('分享链接已复制')).catch(() => window.prompt('请复制分享链接：', url))
  } else {
    window.prompt('请复制分享链接：', url)
  }
}

async function toggleNewsCommentLike(c) {
  if (!isLoggedIn.value) return
  commentLikeBusyId.value = c.id
  try {
    const res = await likeNewsComment(c.id)
    const row = commentList.value.find((x) => String(x.id) === String(c.id))
    if (row) {
      row.liked = !!res?.liked
      row.likeCount = res?.likeCount != null ? Number(res.likeCount) : row.likeCount ?? 0
    }
  } catch (e) {
    window.alert(e?.message || '操作失败')
  } finally {
    commentLikeBusyId.value = null
  }
}

async function removeNewsComment(c) {
  if (!c?.id) return
  if (!window.confirm('确定删除该评论？')) return
  try {
    await deleteNewsComment(c.id)
    await loadComments(true)
  } catch (e) {
    window.alert(e?.message || '删除失败')
  }
}

function toggleReply(cid) {
  replyTo.value = replyTo.value === cid ? null : cid
  replyText.value = ''
}

function cancelReply() {
  replyTo.value = null
  replyText.value = ''
}

async function submitRootComment() {
  if (!isLoggedIn.value || commentSubmitting.value) return
  const html = rootCommentDraft.value ?? ''
  if (isEmptyHtml(html)) {
    window.alert('请输入评论内容')
    return
  }
  commentSubmitting.value = true
  try {
    await postNewsComment(id.value, { content: html })
    rootCommentDraft.value = ''
    await loadComments(true)
  } catch (e) {
    window.alert(e?.message || '发表失败')
  } finally {
    commentSubmitting.value = false
  }
}

async function sendReply(parentId) {
  if (!isLoggedIn.value || commentSubmitting.value) return
  const html = replyText.value ?? ''
  if (isEmptyHtml(html)) {
    window.alert('请输入评论内容')
    return
  }
  commentSubmitting.value = true
  try {
    await postNewsComment(id.value, { content: html, parentId })
    replyText.value = ''
    replyTo.value = null
    await loadComments(true)
  } catch (e) {
    window.alert(e?.message || '发表失败')
  } finally {
    commentSubmitting.value = false
  }
}

watch(id, () => {
  loadDetail()
  loadComments(true)
})

onMounted(() => {
  loadDetail()
  loadComments(true)
})
</script>

<style scoped>
.news-detail {
  max-width: 880px;
  margin: 0 auto;
}

.news-top-actions {
  margin-bottom: 14px;
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  border: 1px solid rgba(59, 130, 246, 0.22);
  border-radius: 10px;
  background: var(--client-surface);
  color: var(--client-muted);
  font-size: 0.875rem;
  font-weight: 500;
  text-decoration: none;
  transition: border-color 0.2s, color 0.2s;
}

.back-btn:hover {
  border-color: var(--client-primary);
  color: var(--client-primary);
}

.icon {
  width: 16px;
  height: 16px;
}

.news-article {
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(59, 130, 246, 0.12);
  border-radius: var(--client-radius);
  padding: clamp(18px, 2vw, 26px);
  box-shadow: 0 1px 0 rgba(59, 130, 246, 0.06);
}

.news-cover {
  width: 100%;
  max-height: 360px;
  object-fit: cover;
  border-radius: calc(var(--client-radius) - 4px);
  margin-bottom: 18px;
}

.news-article-head {
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(59, 130, 246, 0.1);
}

.news-article-title {
  margin: 0 0 14px;
  font-size: clamp(1.25rem, 2.2vw, 1.65rem);
  font-weight: 800;
  color: var(--client-text);
  line-height: 1.35;
  letter-spacing: -0.02em;
}

.news-meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.meta-pill {
  padding: 5px 10px;
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--client-muted);
  background: rgba(59, 130, 246, 0.08);
  border: 1px solid rgba(59, 130, 246, 0.1);
}

.meta-pill--cat {
  color: var(--client-primary-hover);
  background: var(--client-accent-soft);
  border-color: rgba(59, 130, 246, 0.2);
}

.meta-pill.urgency.u1 {
  background: rgba(241, 245, 249, 0.95);
  color: var(--client-muted);
}

.meta-pill.urgency.u2 {
  background: rgba(254, 243, 199, 0.65);
  color: #b45309;
}

.meta-pill.urgency.u3 {
  background: rgba(254, 226, 226, 0.7);
  color: #b91c1c;
}

.news-interactions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  margin-bottom: 18px;
}

.int-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  border-radius: 10px;
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  border: 1px solid rgba(59, 130, 246, 0.25);
  background: var(--client-surface);
  color: var(--client-muted);
  transition: border-color 0.2s, color 0.2s, background 0.2s;
}

.int-btn:hover:not(:disabled) {
  border-color: var(--client-primary);
  color: var(--client-primary);
  background: var(--client-accent-soft);
}

.int-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.int-btn.is-on {
  border-color: var(--client-primary);
  color: var(--client-primary);
  background: rgba(59, 130, 246, 0.1);
}

.int-icon {
  width: 18px;
  height: 18px;
}

.int-hint {
  font-size: 0.8125rem;
  color: var(--client-muted);
}

.news-lead {
  margin: 0 0 18px;
  padding: 12px 14px;
  border-radius: 10px;
  background: rgba(59, 130, 246, 0.06);
  border-left: 3px solid var(--client-primary);
  color: var(--client-text);
  font-size: 0.9375rem;
  line-height: 1.6;
}

.news-body.rich-html {
  line-height: 1.75;
  font-size: 0.9375rem;
  color: var(--client-text);
  word-break: break-word;
}

.news-body.rich-html :deep(p) {
  margin: 0 0 12px;
}

.news-body.rich-html :deep(img) {
  max-width: 100%;
  height: auto;
  vertical-align: middle;
  border-radius: 8px;
}

.news-body.rich-html :deep(table) {
  border-collapse: collapse;
  max-width: 100%;
  margin: 12px 0;
}

.news-body.rich-html :deep(th),
.news-body.rich-html :deep(td) {
  border: 1px solid rgba(59, 130, 246, 0.15);
  padding: 6px 10px;
}

.news-body.rich-html :deep(a) {
  color: var(--client-primary);
}

.news-body.rich-html :deep(pre),
.news-body.rich-html :deep(code) {
  background: rgba(59, 130, 246, 0.06);
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 0.875rem;
}

.news-body.rich-html :deep(pre) {
  padding: 12px;
  overflow-x: auto;
}

.related-section {
  margin-top: 24px;
  padding: 20px;
  border-radius: var(--client-radius);
  border: 1px solid rgba(59, 130, 246, 0.1);
  background: rgba(255, 255, 255, 0.85);
}

.section-title {
  margin: 0 0 14px;
  font-size: 1.0625rem;
  font-weight: 800;
  color: var(--client-text);
}

.related-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.related-item {
  margin-bottom: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: baseline;
}

.related-link {
  color: var(--client-primary);
  text-decoration: none;
  font-weight: 600;
  font-size: 0.875rem;
}

.related-link:hover {
  text-decoration: underline;
  color: var(--client-primary-hover);
}

.related-sub {
  font-size: 0.75rem;
  color: var(--client-muted);
}

.comments-section {
  margin-top: 22px;
  padding: clamp(16px, 2vw, 22px);
  border-radius: var(--client-radius);
  border: 1px solid rgba(59, 130, 246, 0.12);
  background: rgba(255, 255, 255, 0.92);
}

.comments-header {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(59, 130, 246, 0.1);
}

.comments-header :deep(.comment-sort-label) {
  color: var(--client-muted);
}

.comments-header :deep(.comment-sort-tabs) {
  border-color: rgba(59, 130, 246, 0.2);
  border-radius: 8px;
}

.comments-header :deep(.comment-sort-btn) {
  background: var(--client-surface);
  color: var(--client-muted);
}

.comments-header :deep(.comment-sort-btn.active) {
  background: var(--client-primary);
  color: #fff;
  font-weight: 700;
}

.comment-input-card {
  margin-bottom: 16px;
  padding: 14px;
  border-radius: 10px;
  border: 1px solid rgba(59, 130, 246, 0.15);
  background: rgba(59, 130, 246, 0.04);
}

.comment-input-card:focus-within {
  border-color: var(--client-primary);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.submit-comment-btn {
  margin-top: 12px;
  padding: 10px 22px;
  background: var(--client-primary);
  color: #fff;
  border: none;
  border-radius: 10px;
  cursor: pointer;
  font-size: 0.875rem;
  font-weight: 700;
}

.submit-comment-btn:hover:not(:disabled) {
  background: var(--client-primary-hover);
}

.submit-comment-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.muted {
  color: var(--client-muted);
  font-size: 0.875rem;
  margin-bottom: 12px;
}

.reply-line {
  font-size: 0.75rem;
  color: var(--client-muted);
  margin-bottom: 8px;
  padding: 6px 10px;
  border-radius: 8px;
  background: rgba(59, 130, 246, 0.06);
  border-left: 3px solid rgba(59, 130, 246, 0.25);
}

.reply-line-at {
  font-weight: 700;
  color: var(--client-primary);
  margin-right: 6px;
}

.comment-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.comment-list .comment-li {
  padding: 14px 12px;
  margin-bottom: 10px;
  border-radius: 10px;
  border: 1px solid rgba(59, 130, 246, 0.08);
  border-left: 3px solid transparent;
  background: rgba(255, 255, 255, 0.65);
}

.comment-list .comment-li.comment-locate-flash {
  animation: commentLocateFlash 2.4s ease;
}

@keyframes commentLocateFlash {
  0% {
    background: rgba(59, 130, 246, 0.2);
  }
  40% {
    background: rgba(59, 130, 246, 0.1);
  }
  100% {
    background: transparent;
  }
}

.comment-list .comment-li.depth-1,
.comment-list .comment-li.depth-2,
.comment-list .comment-li.depth-3,
.comment-list .comment-li.depth-4,
.comment-list .comment-li.depth-5,
.comment-list .comment-li.depth-6 {
  border-left-color: var(--client-accent-soft);
}

.c-head {
  font-size: 0.8125rem;
  color: var(--client-text);
  margin-bottom: 6px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.c-head .t {
  color: var(--client-muted);
}

.linkish {
  background: none;
  border: none;
  color: var(--client-primary);
  cursor: pointer;
  padding: 0 4px;
  font-size: 0.8125rem;
  font-weight: 600;
}

.linkish.danger {
  color: #dc2626;
}

.comments-section :deep(.c-body.comment-rich-html) {
  margin: 0;
  white-space: normal;
  word-break: break-word;
  font-size: 0.875rem;
  line-height: 1.55;
}

.comments-section :deep(.c-body.comment-rich-html p) {
  margin: 0.35em 0;
}

.comments-section :deep(.c-body.comment-rich-html p:first-child) {
  margin-top: 0;
}

.comments-section :deep(.c-body.comment-rich-html p:last-child) {
  margin-bottom: 0;
}

.reply-box {
  margin-top: 10px;
  padding: 12px;
  background: rgba(59, 130, 246, 0.04);
  border: 1px solid rgba(59, 130, 246, 0.12);
  border-radius: 10px;
}

.reply-actions {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.reply-actions button {
  padding: 6px 14px;
  border-radius: 8px;
  border: none;
  cursor: pointer;
  background: var(--client-primary);
  color: #fff;
  font-size: 0.875rem;
  font-weight: 600;
}

.reply-actions button.secondary {
  background: rgba(148, 163, 184, 0.25);
  color: var(--client-text);
}

.comment-list-fold-bar {
  margin: 12px 0 8px;
  text-align: center;
}

.comment-list-fold-btn {
  padding: 8px 16px;
  border: 1px solid rgba(59, 130, 246, 0.25);
  border-radius: 8px;
  background: rgba(59, 130, 246, 0.06);
  color: var(--client-primary);
  font-size: 0.8125rem;
  font-weight: 600;
  cursor: pointer;
}

.comment-list-fold-btn:hover {
  border-color: var(--client-primary);
  background: var(--client-accent-soft);
}

.empty-c,
.loading-c {
  color: var(--client-muted);
  font-size: 0.875rem;
}

.comment-pager {
  margin-top: 14px;
}

.news-state {
  padding: 48px 16px;
  text-align: center;
  color: var(--client-muted);
  font-size: 0.9375rem;
}

.news-state--error {
  max-width: 400px;
  margin: 0 auto;
  color: #b91c1c;
}

.state-link {
  display: inline-block;
  margin-top: 12px;
  color: var(--client-primary);
  font-weight: 600;
}
</style>
