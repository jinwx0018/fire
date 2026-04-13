<template>
  <KnowledgeModuleShell v-if="detail" :crumbs="detailCrumbs">
    <div class="detail-page-layout" :class="{ 'layout-no-toc': !tocItems.length }">
      <aside v-if="tocItems.length" class="detail-toc-sidebar" aria-label="文章目录">
        <div class="toc-card">
          <div class="toc-head">
            <h3 class="toc-title">目录</h3>
            <button type="button" class="toc-toggle" :aria-expanded="tocExpanded" @click="tocExpanded = !tocExpanded">
              {{ tocExpanded ? '收起' : '展开' }}
            </button>
          </div>
          <nav v-show="tocExpanded" class="toc-nav">
            <a
              v-for="item in tocItems"
              :key="item.id"
              href="#"
              class="toc-link"
              :class="['toc-level-' + item.level, { active: activeTocId === item.id }]"
              @click.prevent="scrollToHeading(item.id)"
            >
              {{ item.text }}
            </a>
          </nav>
        </div>
      </aside>

      <div class="detail-main-wrap">
        <article class="detail-article">
          <button type="button" class="back-btn" @click="goBack">← 返回</button>

          <header class="article-hero">
            <h1 class="article-title">{{ detail.title }}</h1>
            <div class="article-meta-bar">
              <span v-if="categoryDisplay" class="meta-pill">{{ categoryDisplay }}</span>
              <span v-if="detail.createTime" class="meta-item">{{ formatDateTime(detail.createTime) }}</span>
              <span class="meta-item">浏览 {{ detail.viewCount ?? 0 }}</span>
              <span class="meta-item">赞 {{ detail.likeCount ?? 0 }}</span>
              <span v-if="showShareAndComments && commentCountDisplay != null" class="meta-item">
                评论 {{ commentCountDisplay }}
              </span>
            </div>
            <div class="article-actions">
              <button
                v-if="userStore.isLoggedIn && isPublished"
                type="button"
                class="action-pill action-like"
                :class="{ 'is-on': detail.liked }"
                @click="onLike"
              >
                {{ detail.liked ? '已点赞' : '点赞' }}
              </button>
              <button
                v-if="userStore.isLoggedIn && isPublished"
                type="button"
                class="action-pill action-collect"
                :class="{ 'is-on': detail.collected }"
                @click="onCollect"
              >
                {{ detail.collected ? '已收藏' : '收藏' }}
              </button>
              <button
                v-if="showShareAndComments && !authorPreviewReadOnly"
                type="button"
                class="action-pill action-share"
                @click="onShare"
              >
                分享
              </button>
            </div>
          </header>

          <p v-if="detailRejectReason" class="reject-reason">驳回原因：{{ detailRejectReason }}</p>
          <div ref="proseRef" class="body knowledge-prose" v-html="displayContent"></div>

          <nav class="article-chapter-nav" aria-label="上一篇下一篇">
            <router-link
              v-if="prevArticle"
              :to="'/knowledge/' + encodeURIComponent(String(prevArticle.id))"
              class="chapter-card chapter-prev"
            >
              <span class="chapter-label">← 上一篇</span>
              <span class="chapter-title">{{ prevArticle.title }}</span>
            </router-link>
            <div v-else class="chapter-card chapter-empty">
              <span class="chapter-label">← 上一篇</span>
              <span class="chapter-title">没有了</span>
            </div>
            <router-link
              v-if="nextArticle"
              :to="'/knowledge/' + encodeURIComponent(String(nextArticle.id))"
              class="chapter-card chapter-next"
            >
              <span class="chapter-label">下一篇 →</span>
              <span class="chapter-title">{{ nextArticle.title }}</span>
            </router-link>
            <div v-else class="chapter-card chapter-empty">
              <span class="chapter-label">下一篇 →</span>
              <span class="chapter-title">没有了</span>
            </div>
          </nav>

          <div v-if="authorPreviewReadOnly" class="author-preview-actions">
            <button type="button" class="del-knowledge" @click="removeKnowledgeContent">删除</button>
          </div>

          <section v-if="showShareAndComments && !authorPreviewReadOnly" class="comments-section">
            <div class="comments-head">
              <h3 class="comments-title">评论</h3>
              <CommentSortBar :active-sort="commentSort" @select="setCommentSort" />
            </div>
            <div v-if="userStore.isLoggedIn && isPublished" class="comment-form">
              <ForumCommentEditor v-model="rootCommentDraft" placeholder="写下你的看法…" />
              <button type="button" class="comment-submit" :disabled="commentSubmitting" @click="submitRootComment">
                {{ commentSubmitting ? '发送中…' : '发表评论' }}
              </button>
            </div>
            <p v-if="commentLoadError" class="comment-error">{{ commentLoadError }}</p>
            <CommentThreadNodes
              v-else-if="!commentLoadError && displayCommentTreeRoots.length"
              :nodes="displayCommentTreeRoots"
              :item-class="'comment-item'"
              root-list-class="comment-list"
              :threshold="COMMENT_REPLY_BRANCH_THRESHOLD"
            >
              <template #row="{ node: c }">
                <div v-if="c.parentId" class="reply-line">
                  <span class="reply-line-at">@{{ replyAtUsername(c, comments) }}</span>
                  <span class="reply-line-snippet">{{ replyQuoteText(c, comments) }}</span>
                </div>
                <div class="comment-head-row">
                  <span class="comment-user">{{ c.username || '用户' }}</span>
                  <span class="comment-time">{{ formatDateTime(c.createTime) }}</span>
                  <button
                    v-if="userStore.isLoggedIn && isPublished"
                    type="button"
                    class="comment-like-btn"
                    :disabled="commentLikeBusyId === c.id"
                    @click="toggleKnowledgeCommentLike(c)"
                  >
                    {{ c.liked ? '已赞' : '赞' }} {{ c.likeCount ?? 0 }}
                  </button>
                  <button
                    v-if="userStore.isLoggedIn && isPublished"
                    type="button"
                    class="comment-reply-btn"
                    @click="toggleReply(c.id)"
                  >
                    回复
                  </button>
                  <button
                    v-if="userStore.isLoggedIn && isCommentOwner(c)"
                    type="button"
                    class="comment-del-btn"
                    @click="removeKnowledgeComment(c)"
                  >
                    删除
                  </button>
                </div>
                <div class="comment-body comment-rich-html" v-html="commentDisplayHtml(c.content)" />
                <div v-if="replyTo === c.id" class="reply-box">
                  <ForumCommentEditor v-model="replyText" placeholder="回复…" />
                  <div class="reply-actions">
                    <button type="button" :disabled="commentSubmitting" @click="sendReply(c.id)">发送</button>
                    <button type="button" class="secondary" @click="cancelReply">取消</button>
                  </div>
                </div>
              </template>
            </CommentThreadNodes>
            <div
              v-if="!commentLoadError && commentRows.length && showCommentListFoldToggle"
              class="comment-list-fold-bar"
            >
              <button
                v-if="!commentListExpanded"
                type="button"
                class="comment-list-fold-btn"
                @click="commentListExpanded = true"
              >
                展开全部 {{ commentRows.length }} 条评论
              </button>
              <button
                v-else
                type="button"
                class="comment-list-fold-btn"
                @click="commentListExpanded = false"
              >
                收起评论列表
              </button>
            </div>
            <p v-if="!commentLoadError && !commentRows.length && !commentLoading" class="comment-empty">暂无评论</p>
            <p v-if="commentLoading" class="comment-loading">评论加载中…</p>

            <PaginationBar
              v-model="commentPage"
              class="comment-pager"
              :total="commentTotal"
              :page-size="commentPageSize"
              @current-change="() => loadComments(false)"
            />

            <p v-if="!userStore.isLoggedIn && showShareAndComments" class="comment-login-hint">登录后可发表评论</p>
            <p v-else-if="showShareAndComments && !isPublished" class="comment-login-hint">当前为未上架内容，仅可浏览历史评论</p>
          </section>
        </article>
      </div>

      <aside class="detail-related-sidebar" aria-label="相关推荐">
        <div class="related-card-wrap">
          <h3 class="related-title">相关推荐</h3>
          <ul v-if="relatedList.length" class="related-list">
            <li v-for="row in relatedList" :key="row.id">
              <router-link :to="'/knowledge/' + encodeURIComponent(String(row.id))" class="related-item">
                <div v-if="row.cover" class="related-thumb">
                  <img :src="resolveMediaUrl(row.cover)" alt="" loading="lazy" />
                </div>
                <div v-else class="related-thumb related-thumb-placeholder">科普</div>
                <div class="related-text">
                  <span class="related-item-title">{{ row.title }}</span>
                  <span class="related-meta">浏览 {{ row.viewCount ?? 0 }}</span>
                </div>
              </router-link>
            </li>
          </ul>
          <p v-else class="related-empty">暂无同分类推荐</p>
        </div>
      </aside>
    </div>
  </KnowledgeModuleShell>
  <KnowledgeModuleShell v-else :crumbs="loadingCrumbs" narrow>
    <div class="detail-loading">加载中…</div>
  </KnowledgeModuleShell>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed, watch, nextTick, provide, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  getContentDetail,
  getContentList,
  getCategoryList,
  toggleContentLike,
  collect,
  cancelCollect,
  getShareUrl,
  getContentComments,
  postContentComment,
  likeKnowledgeComment,
  deleteKnowledgeComment,
  deleteContent,
} from '@/api/content'
import { postRecommendFeedback } from '@/api/recommend'
import { useUserStore } from '@/stores/user'
import { sanitizeHtml, getPostDisplayHtml, isEmptyHtml } from '@/utils/htmlContent'
import { injectHeadingIdsAndToc } from '@/utils/knowledgeBody'
import { highlightKnowledgeProse } from '@/utils/knowledgePrism'
import { resolveMediaUrl } from '@/utils/resolveMediaUrl'
import ForumCommentEditor from '@/components/ForumCommentEditor.vue'
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
const router = useRouter()
const userStore = useUserStore()
const id = computed(() => route.params.id)
const detail = ref(null)
const comments = ref([])
const commentLoading = ref(false)
const commentLoadError = ref('')
const rootCommentDraft = ref('')
const replyTo = ref(null)
const replyText = ref('')
const commentSubmitting = ref(false)
const commentLikeBusyId = ref(null)
const commentPage = ref(1)
const commentPageSize = 10
const commentTotal = ref(0)
const commentSort = ref('time')

const commentRows = computed(() =>
  flattenCommentTree(buildCommentTree(comments.value, commentSort.value)),
)

const commentTreeRoots = computed(() => buildCommentTree(comments.value, commentSort.value))

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

const tocExpanded = ref(true)
const activeTocId = ref('')
const relatedList = ref([])
const prevArticle = ref(null)
const nextArticle = ref(null)
let tocObserver = null

const safeContent = computed(() => sanitizeHtml(detail.value?.content || ''))
const enrichedContent = computed(() => injectHeadingIdsAndToc(safeContent.value))
const displayContent = computed(() => enrichedContent.value.html)
const tocItems = computed(() => enrichedContent.value.toc)

const proseRef = ref(null)

watch(displayContent, async () => {
  await nextTick()
  highlightKnowledgeProse(proseRef.value)
})

watch([commentPage, commentSort], () => {
  commentListExpanded.value = false
  for (const k of Object.keys(replyBranchExpanded)) delete replyBranchExpanded[k]
})

const detailRejectReason = computed(() => {
  const d = detail.value
  if (!d) return ''
  const t = d.rejectReason ?? d.reject_reason
  return t && String(t).trim() ? String(t).trim() : ''
})

/** 仅已发布（status=1）可点赞/收藏/评赞；作者预览待审核/草稿等不显示 */
const isPublished = computed(() => {
  const d = detail.value
  if (!d) return false
  const s = Number(d.status)
  return s === 1
})

const uid = computed(() => userStore.user?.userId ?? userStore.user?.id)
const isAuthor = computed(() => {
  const d = detail.value
  if (!d || uid.value == null) return false
  const aid = d.authorId ?? d.author_id
  return aid != null && String(aid) === String(uid.value)
})

/** 作者查看未正式发布内容：无分享/评论区，仅可删除 */
const authorPreviewReadOnly = computed(() => isAuthor.value && !isPublished.value)

/**
 * 分享与评论区：已发布对所有人展示；草稿/待审不展示；
 * 例外：已下架且当前用户为作者（自己下架）仍展示，便于保留历史评论与分享。
 */
const showShareAndComments = computed(() => {
  const d = detail.value
  if (!d) return false
  const s = Number(d.status)
  if (s === 1) return true
  if (s === 2 && isAuthor.value) return true
  return false
})
const commentCountDisplay = computed(() => {
  const d = detail.value
  if (!d) return null
  if (d.commentCount != null) return d.commentCount
  if (d.comment_count != null) return d.comment_count
  return null
})

/** 详情接口已带 categoryName；兼容 snake_case，缺省时用分类列表按 categoryId 补全 */
const categoryDisplay = computed(() => {
  const d = detail.value
  if (!d) return ''
  const n = d.categoryName ?? d.category_name
  if (n != null && String(n).trim() !== '') return String(n).trim()
  return ''
})

const loadingCrumbs = [
  { label: '首页', to: '/' },
  { label: '消防知识', to: '/knowledge' },
  { label: '加载中' },
]

const detailCrumbs = computed(() => {
  const d = detail.value
  const title = d?.title && String(d.title).trim() ? String(d.title).trim() : '正文'
  return [
    { label: '首页', to: '/' },
    { label: '消防知识', to: '/knowledge' },
    { label: title, titleAttr: d?.title ? String(d.title) : undefined },
  ]
})

/** 与论坛/新闻评论一致：富文本 img 经 XSS 粗过滤后展示 */
function commentDisplayHtml(raw) {
  return sanitizeHtml(getPostDisplayHtml(raw || ''))
}

function scrollToHeading(hid) {
  const el = document.getElementById(hid)
  if (!el) return
  el.scrollIntoView({ behavior: 'smooth', block: 'start' })
  activeTocId.value = hid
}

function clearTocObserver() {
  if (tocObserver) {
    tocObserver.disconnect()
    tocObserver = null
  }
}

function setupTocObserver() {
  clearTocObserver()
  if (!tocItems.value.length) return
  const root = document.querySelector('.knowledge-prose')
  if (!root) return
  const headings = root.querySelectorAll('h2[id], h3[id]')
  if (!headings.length) return
  tocObserver = new IntersectionObserver(
    (entries) => {
      const vis = entries.filter((e) => e.isIntersecting)
      if (!vis.length) return
      vis.sort((a, b) => (b.intersectionRatio || 0) - (a.intersectionRatio || 0))
      const id = vis[0].target.id
      if (id) activeTocId.value = id
    },
    { root: null, rootMargin: '-12% 0px -60% 0px', threshold: [0, 0.1, 0.25, 0.5, 1] },
  )
  headings.forEach((h) => tocObserver.observe(h))
}

async function loadRelatedAndNeighbors() {
  relatedList.value = []
  prevArticle.value = null
  nextArticle.value = null
  const d = detail.value
  if (!d) return
  const cid = d.categoryId ?? d.category_id
  const curId = Number(id.value)
  if (cid == null || cid === '' || !Number.isFinite(curId)) return
  try {
    const [hotRes, timeRes] = await Promise.all([
      getContentList({ categoryId: cid, sortBy: 'hot', pageNum: 1, pageSize: 16 }),
      getContentList({
        categoryId: cid,
        sortBy: 'latest',
        timeOrder: 'desc',
        pageNum: 1,
        pageSize: 100,
      }),
    ])
    const hotRows = hotRes?.records ?? hotRes?.list ?? []
    relatedList.value = hotRows.filter((r) => Number(r.id) !== curId).slice(0, 6)
    const timeRows = timeRes?.records ?? timeRes?.list ?? []
    const ix = timeRows.findIndex((r) => Number(r.id) === curId)
    if (ix >= 0) {
      const newer = timeRows[ix - 1]
      const older = timeRows[ix + 1]
      nextArticle.value = newer ? { id: newer.id, title: newer.title } : null
      prevArticle.value = older ? { id: older.id, title: older.title } : null
    }
  } catch (_) {
    /* 侧栏与导航为增强能力，失败不阻断阅读 */
  }
}

function goBack() {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/knowledge')
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
  commentLoading.value = true
  commentLoadError.value = ''
  try {
    const page = await getContentComments(id.value, {
      pageNum: commentPage.value,
      pageSize: commentPageSize,
      sortBy: commentSort.value === 'hot' ? 'hot' : 'time',
    })
    const records = page?.records ?? page?.list ?? []
    comments.value = Array.isArray(records) ? records : []
    commentTotal.value = Number(page?.total ?? records.length)
  } catch (e) {
    commentLoadError.value = e?.message || '评论加载失败'
    if (reset) {
      comments.value = []
      commentTotal.value = 0
    }
  } finally {
    commentLoading.value = false
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

/** 从智能推荐列表进入时上报 CLICK（离线 CTR），同会话去重 */
async function reportRecClickIfNeeded() {
  if (route.query.ref !== 'rec') return
  const rankRaw = route.query.rank
  const rankPos = rankRaw != null && String(rankRaw).trim() !== '' ? Number(rankRaw) : undefined
  const nid = Number(id.value)
  if (!Number.isFinite(nid)) return
  const dedupeKey = `rec_fb_click_${nid}_${rankRaw ?? ''}`
  try {
    if (sessionStorage.getItem(dedupeKey)) return
    await postRecommendFeedback({
      contentId: nid,
      actionType: 'CLICK',
      rankPos: Number.isFinite(rankPos) && rankPos > 0 ? rankPos : undefined,
    })
    sessionStorage.setItem(dedupeKey, '1')
  } catch (_) {
    /* 埋点失败不影响阅读 */
  }
}

async function enrichCategoryNameIfNeeded() {
  const d = detail.value
  if (!d) return
  const raw = d.categoryName ?? d.category_name
  if (raw != null && String(raw).trim() !== '') return
  const cid = d.categoryId ?? d.category_id
  if (cid == null || cid === '') return
  try {
    const list = await getCategoryList()
    const arr = Array.isArray(list) ? list : []
    const row = arr.find((c) => String(c.id) === String(cid))
    if (row?.name) {
      detail.value = { ...d, categoryName: row.name }
    }
  } catch (_) {
    /* 分类列表失败时不阻断详情 */
  }
}

async function loadDetailThenComments() {
  try {
    detail.value = await getContentDetail(id.value)
    await enrichCategoryNameIfNeeded()
    await loadRelatedAndNeighbors()
  } catch (e) {
    console.error(e)
  }
  await reportRecClickIfNeeded()
  if (showShareAndComments.value && !authorPreviewReadOnly.value) {
    await loadComments(true)
  } else {
    comments.value = []
    commentLoadError.value = ''
    commentTotal.value = 0
    commentPage.value = 1
  }
  await nextTick()
  setupTocObserver()
}

onMounted(async () => {
  await loadDetailThenComments()
})

watch(id, async () => {
  detail.value = null
  comments.value = []
  rootCommentDraft.value = ''
  replyTo.value = null
  replyText.value = ''
  activeTocId.value = ''
  clearTocObserver()
  await loadDetailThenComments()
})

onUnmounted(() => {
  clearTocObserver()
})

function toggleReply(cid) {
  replyTo.value = replyTo.value === cid ? null : cid
  replyText.value = ''
}

function cancelReply() {
  replyTo.value = null
  replyText.value = ''
}

async function onLike() {
  try {
    const res = await toggleContentLike(id.value)
    detail.value = {
      ...detail.value,
      liked: !!res?.liked,
      likeCount: Number(res?.likeCount ?? detail.value?.likeCount ?? 0),
    }
  } catch (e) {
    console.error(e)
    alert(e.message || '点赞失败')
  }
}

async function onCollect() {
  if (!userStore.isLoggedIn) {
    alert('请先登录')
    return
  }
  try {
    if (detail.value?.collected) {
      await cancelCollect(id.value)
      detail.value = { ...detail.value, collected: false }
    } else {
      await collect(id.value)
      detail.value = { ...detail.value, collected: true }
    }
  } catch (e) {
    console.error(e)
    alert(e.message || '收藏操作失败')
  }
}

async function onShare() {
  try {
    const data = await getShareUrl(id.value)
    const url = data?.shareUrl || `${window.location.origin}/#/knowledge/${id.value}`
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(url)
      alert('分享链接已复制')
      return
    }
    prompt('请复制分享链接：', url)
  } catch (e) {
    console.error(e)
    alert(e.message || '生成分享链接失败')
  }
}

async function bumpCommentCount() {
  if (detail.value) {
    const n = Number(detail.value.commentCount ?? detail.value.comment_count ?? 0) + 1
    detail.value = { ...detail.value, commentCount: n, comment_count: n }
  }
}

async function submitRootComment() {
  const text = rootCommentDraft.value
  if (isEmptyHtml(text)) {
    alert('请输入评论内容')
    return
  }
  commentSubmitting.value = true
  try {
    await postContentComment(id.value, { content: text })
    rootCommentDraft.value = ''
    await loadComments(true)
    await bumpCommentCount()
  } catch (e) {
    console.error(e)
    alert(e.message || '发表评论失败')
  } finally {
    commentSubmitting.value = false
  }
}

async function sendReply(parentId) {
  const text = replyText.value
  if (isEmptyHtml(text)) {
    alert('请输入评论内容')
    return
  }
  commentSubmitting.value = true
  try {
    await postContentComment(id.value, { content: text, parentId })
    replyText.value = ''
    replyTo.value = null
    await loadComments(true)
    await bumpCommentCount()
  } catch (e) {
    console.error(e)
    alert(e.message || '回复失败')
  } finally {
    commentSubmitting.value = false
  }
}

async function toggleKnowledgeCommentLike(c) {
  if (!userStore.isLoggedIn) {
    alert('请先登录')
    return
  }
  commentLikeBusyId.value = c.id
  try {
    const res = await likeKnowledgeComment(c.id)
    const row = comments.value.find((x) => String(x.id) === String(c.id))
    if (row) {
      row.liked = !!res?.liked
      row.likeCount = res?.likeCount != null ? Number(res.likeCount) : row.likeCount ?? 0
    }
  } catch (e) {
    alert(e?.message || '操作失败')
  } finally {
    commentLikeBusyId.value = null
  }
}

async function removeKnowledgeComment(c) {
  if (!c?.id) return
  if (!window.confirm('确定删除该评论？')) return
  try {
    await deleteKnowledgeComment(c.id)
    await loadComments(true)
  } catch (e) {
    alert(e?.message || '删除失败')
  }
}

async function removeKnowledgeContent() {
  if (!window.confirm('确定删除该知识？')) return
  try {
    await deleteContent(id.value)
    router.push('/knowledge/drafts')
  } catch (e) {
    alert(e?.message || '删除失败')
  }
}
</script>

<style scoped>
.detail-loading {
  text-align: center;
  padding: 48px 16px;
  color: var(--client-muted);
  font-size: 0.9375rem;
}

.detail-page-layout {
  display: grid;
  gap: 20px;
  grid-template-columns: 1fr;
  align-items: start;
}

@media (min-width: 1100px) {
  .detail-page-layout {
    grid-template-columns: minmax(0, 200px) minmax(0, 1fr) minmax(0, 260px);
    gap: 24px;
  }

  .detail-page-layout.layout-no-toc {
    grid-template-columns: minmax(0, 1fr) minmax(0, 280px);
  }

  .detail-toc-sidebar,
  .detail-related-sidebar {
    position: sticky;
    top: 16px;
    max-height: calc(100vh - 32px);
    overflow-y: auto;
  }
}

.detail-main-wrap {
  min-width: 0;
}

.detail-article {
  min-width: 0;
}

.back-btn {
  margin-bottom: 16px;
  padding: 8px 16px;
  background: rgba(59, 130, 246, 0.08);
  border: 1px solid rgba(59, 130, 246, 0.22);
  border-radius: 10px;
  cursor: pointer;
  color: var(--client-primary-hover);
  font-size: 0.875rem;
}

.back-btn:hover {
  background: var(--client-accent-soft);
}

.article-hero {
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid rgba(59, 130, 246, 0.12);
}

.article-title {
  margin: 0 0 14px;
  font-size: clamp(1.35rem, 2.5vw, 1.75rem);
  font-weight: 800;
  color: var(--client-text);
  line-height: 1.3;
  letter-spacing: -0.03em;
}

.article-meta-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 12px;
  align-items: center;
  margin-bottom: 16px;
  font-size: 0.8125rem;
  color: var(--client-muted);
}

.meta-pill {
  padding: 4px 10px;
  border-radius: 999px;
  font-weight: 600;
  color: var(--client-primary-hover);
  background: rgba(59, 130, 246, 0.12);
  border: 1px solid rgba(59, 130, 246, 0.2);
}

.meta-item {
  white-space: nowrap;
}

.article-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.action-pill {
  padding: 8px 16px;
  border-radius: 999px;
  font-size: 0.8125rem;
  font-weight: 600;
  cursor: pointer;
  border: 1px solid rgba(59, 130, 246, 0.35);
  background: var(--client-surface);
  color: var(--client-primary-hover);
  transition: background 0.15s, border-color 0.15s, color 0.15s, box-shadow 0.15s;
}

.action-pill:hover {
  background: var(--client-accent-soft);
  border-color: var(--client-primary);
}

.action-pill.action-like.is-on {
  background: var(--client-primary);
  border-color: var(--client-primary);
  color: #fff;
  box-shadow: 0 4px 14px rgba(59, 130, 246, 0.35);
}

.action-pill.action-collect.is-on {
  background: #16a34a;
  border-color: #16a34a;
  color: #fff;
}

.action-pill.action-share {
  border-color: rgba(124, 58, 237, 0.45);
  color: #6d28d9;
}

.action-pill.action-share:hover {
  background: rgba(237, 233, 254, 0.8);
}

.toc-card,
.related-card-wrap {
  background: rgba(59, 130, 246, 0.04);
  border: 1px solid rgba(59, 130, 246, 0.14);
  border-radius: var(--client-radius);
  padding: 14px 16px;
}

.toc-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 10px;
}

.toc-title {
  margin: 0;
  font-size: 0.9375rem;
  font-weight: 700;
  color: var(--client-text);
}

.toc-toggle {
  padding: 4px 10px;
  font-size: 0.75rem;
  border-radius: 8px;
  border: 1px solid rgba(59, 130, 246, 0.25);
  background: var(--client-surface);
  color: var(--client-muted);
  cursor: pointer;
}

.toc-nav {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.toc-link {
  display: block;
  padding: 6px 8px;
  border-radius: 8px;
  font-size: 0.8125rem;
  color: var(--client-muted);
  text-decoration: none;
  line-height: 1.4;
  border-left: 3px solid transparent;
  transition: background 0.15s, color 0.15s, border-color 0.15s;
}

.toc-link:hover {
  background: rgba(59, 130, 246, 0.08);
  color: var(--client-text);
}

.toc-link.active {
  color: var(--client-primary-hover);
  font-weight: 600;
  background: var(--client-accent-soft);
  border-left-color: var(--client-primary);
}

.toc-level-3 {
  padding-left: 14px;
  font-size: 0.78rem;
}

.related-title {
  margin: 0 0 12px;
  font-size: 0.9375rem;
  font-weight: 700;
  color: var(--client-text);
}

.related-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.related-item {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  padding: 8px;
  border-radius: 10px;
  text-decoration: none;
  color: inherit;
  border: 1px solid transparent;
  transition: border-color 0.15s, background 0.15s, box-shadow 0.15s;
}

.related-item:hover {
  background: var(--client-surface);
  border-color: rgba(59, 130, 246, 0.2);
  box-shadow: 0 4px 14px rgba(59, 130, 246, 0.08);
}

.related-thumb {
  width: 72px;
  height: 48px;
  flex-shrink: 0;
  border-radius: 8px;
  overflow: hidden;
  background: rgba(59, 130, 246, 0.08);
}

.related-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.related-thumb-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.6875rem;
  font-weight: 700;
  color: var(--client-primary);
  opacity: 0.7;
}

.related-text {
  min-width: 0;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.related-item-title {
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--client-text);
  line-height: 1.35;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.related-item:hover .related-item-title {
  color: var(--client-primary);
}

.related-meta {
  font-size: 0.72rem;
  color: var(--client-muted);
}

.related-empty {
  margin: 0;
  font-size: 0.8125rem;
  color: var(--client-muted);
  line-height: 1.5;
}

.article-chapter-nav {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin: 28px 0 8px;
}

@media (max-width: 640px) {
  .article-chapter-nav {
    grid-template-columns: 1fr;
  }
}

.chapter-card {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 14px 16px;
  border-radius: var(--client-radius);
  border: 1px solid rgba(59, 130, 246, 0.15);
  background: rgba(59, 130, 246, 0.04);
  text-decoration: none;
  color: inherit;
  min-height: 72px;
  transition: transform 0.2s, box-shadow 0.2s, border-color 0.2s;
}

.chapter-card:hover:not(.chapter-empty) {
  border-color: rgba(59, 130, 246, 0.3);
  box-shadow: 0 8px 24px rgba(59, 130, 246, 0.12);
  transform: translateY(-2px);
}

.chapter-next {
  text-align: right;
}

.chapter-empty {
  opacity: 0.65;
  cursor: default;
}

.chapter-label {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--client-primary);
}

.chapter-title {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--client-text);
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.reject-reason {
  font-size: 0.875rem;
  color: #dc2626;
  margin-bottom: 12px;
  padding: 10px 12px;
  background: rgba(254, 226, 226, 0.6);
  border-radius: 8px;
  border: 1px solid rgba(248, 113, 113, 0.35);
}

.detail-article .body.knowledge-prose {
  line-height: 1.75;
  color: var(--client-text);
  font-size: 1rem;
}

.detail-article .knowledge-prose :deep(h2),
.detail-article .knowledge-prose :deep(h3),
.detail-article .knowledge-prose :deep(h4) {
  color: var(--client-text);
  font-weight: 700;
  margin: 1.35em 0 0.55em;
  scroll-margin-top: 88px;
}

.detail-article .knowledge-prose :deep(h2) {
  font-size: 1.25rem;
  padding-bottom: 0.35em;
  border-bottom: 1px solid rgba(59, 130, 246, 0.15);
}

.detail-article .knowledge-prose :deep(h3) {
  font-size: 1.08rem;
}

.detail-article .knowledge-prose :deep(p) {
  margin: 0.65em 0;
}

.detail-article .knowledge-prose :deep(ul),
.detail-article .knowledge-prose :deep(ol) {
  margin: 0.65em 0;
  padding-left: 1.35rem;
}

.detail-article .knowledge-prose :deep(blockquote) {
  margin: 1em 0;
  padding: 10px 14px;
  border-left: 4px solid var(--client-primary);
  background: rgba(59, 130, 246, 0.06);
  color: var(--client-muted);
}

.detail-article .knowledge-prose :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: 10px;
  margin: 1em 0;
}

/* 代码块：配色由 Prism theme（prism-tomorrow）负责 */
.detail-article .knowledge-prose :deep(pre) {
  margin: 1em 0;
  border-radius: 10px;
  overflow-x: auto;
  font-size: 0.875rem;
  line-height: 1.55;
}

.detail-article .knowledge-prose :deep(code) {
  font-family: ui-monospace, 'Cascadia Code', monospace;
  font-size: 0.9em;
}

.detail-article .knowledge-prose :deep(p code),
.detail-article .knowledge-prose :deep(li code) {
  padding: 2px 6px;
  border-radius: 6px;
  background: rgba(59, 130, 246, 0.1);
  color: var(--client-primary-hover);
}

.detail-article .knowledge-prose :deep(pre code) {
  padding: 1em 1.15em;
  display: block;
  background: transparent;
}

.detail-article .knowledge-prose :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 1em 0;
  font-size: 0.9rem;
}

.detail-article .knowledge-prose :deep(th),
.detail-article .knowledge-prose :deep(td) {
  border: 1px solid rgba(59, 130, 246, 0.2);
  padding: 8px 10px;
}

.detail-article .knowledge-prose :deep(th) {
  background: rgba(59, 130, 246, 0.08);
}

.comments-section {
  margin-top: 28px;
  padding-top: 20px;
  border-top: 1px solid rgba(59, 130, 246, 0.12);
}
.comments-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 12px;
}
.comments-title {
  margin: 0;
  font-size: 1.05rem;
  font-weight: 700;
  color: var(--client-text);
}
.comment-list-fold-bar {
  margin: 10px 0 6px;
  text-align: center;
}
.comment-list-fold-btn {
  padding: 8px 16px;
  border: 1px solid rgba(59, 130, 246, 0.25);
  border-radius: 10px;
  background: rgba(59, 130, 246, 0.06);
  color: var(--client-primary-hover);
  font-size: 0.8125rem;
  cursor: pointer;
}
.comment-list-fold-btn:hover {
  border-color: var(--client-primary);
  background: var(--client-accent-soft);
}
.comment-error {
  color: #dc2626;
  font-size: 0.875rem;
}
.comment-list {
  list-style: none;
  margin: 0;
  padding: 0;
}
.comment-item {
  padding: 12px 0;
  border-bottom: 1px solid rgba(59, 130, 246, 0.08);
  border-left: 3px solid transparent;
}
.comment-item.depth-1,
.comment-item.depth-2,
.comment-item.depth-3,
.comment-item.depth-4,
.comment-item.depth-5,
.comment-item.depth-6 {
  border-left-color: var(--client-accent-soft);
}
.comment-item.comment-locate-flash {
  animation: commentLocateFlash 2.4s ease;
  border-radius: 8px;
}
@keyframes commentLocateFlash {
  0% {
    background: rgba(59, 130, 246, 0.22);
  }
  40% {
    background: rgba(59, 130, 246, 0.12);
  }
  100% {
    background: transparent;
  }
}
.comment-head-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 12px;
  margin-bottom: 6px;
}
.comment-user {
  font-weight: 600;
  color: var(--client-text);
}
.comment-time {
  font-size: 0.75rem;
  color: var(--client-muted);
}
.comment-like-btn {
  margin-left: auto;
  padding: 4px 10px;
  font-size: 0.75rem;
  border: 1px solid rgba(59, 130, 246, 0.2);
  border-radius: 8px;
  background: rgba(59, 130, 246, 0.04);
  color: var(--client-text);
  cursor: pointer;
}
.comment-reply-btn {
  padding: 4px 10px;
  font-size: 0.75rem;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: var(--client-primary);
  cursor: pointer;
}
.comment-reply-btn:hover {
  color: var(--client-primary-hover);
}
.comment-del-btn {
  padding: 4px 10px;
  font-size: 0.75rem;
  border: 1px solid rgba(248, 113, 113, 0.45);
  border-radius: 8px;
  background: rgba(254, 226, 226, 0.45);
  color: #b91c1c;
  cursor: pointer;
}
.comment-like-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.comments-section :deep(.comment-body.comment-rich-html) {
  margin: 6px 0 0;
  word-break: break-word;
  color: var(--client-text);
  line-height: 1.55;
}
.comments-section :deep(.comment-body.comment-rich-html p) {
  margin: 0.35em 0;
}
.comments-section :deep(.comment-body.comment-rich-html p:first-child) {
  margin-top: 0;
}
.comments-section :deep(.comment-body.comment-rich-html p:last-child) {
  margin-bottom: 0;
}
.comment-empty,
.comment-loading {
  color: var(--client-muted);
  font-size: 0.875rem;
}
.comment-pager {
  margin-top: 12px;
}
.comment-form {
  margin-bottom: 12px;
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
  padding: 8px 16px;
  border-radius: 8px;
  border: none;
  cursor: pointer;
  background: var(--client-primary);
  color: #fff;
  font-size: 0.875rem;
}
.reply-actions button:hover {
  background: var(--client-primary-hover);
}
.reply-actions button.secondary {
  background: rgba(100, 116, 139, 0.12);
  color: var(--client-text);
}
.reply-actions button.secondary:hover {
  background: rgba(100, 116, 139, 0.18);
}
.comment-submit {
  margin-top: 12px;
  padding: 10px 20px;
  border: none;
  border-radius: 10px;
  background: var(--client-primary);
  color: #fff;
  cursor: pointer;
  font-size: 0.875rem;
  font-weight: 600;
}
.comment-submit:hover:not(:disabled) {
  background: var(--client-primary-hover);
}
.comment-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.comment-login-hint {
  color: var(--client-muted);
  font-size: 0.875rem;
  margin-top: 12px;
}
.author-preview-actions {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid rgba(59, 130, 246, 0.12);
}
.del-knowledge {
  padding: 10px 20px;
  border: none;
  border-radius: 10px;
  background: #dc2626;
  color: #fff;
  cursor: pointer;
  font-size: 0.875rem;
  font-weight: 600;
}
.del-knowledge:hover {
  background: #b91c1c;
}
</style>
