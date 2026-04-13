<template>
  <KnowledgeModuleShell :crumbs="crumbs">
    <div v-if="detail" class="detail-page">
      <div class="detail-layout">
        <div class="detail-main">
          <div class="post-actions-top">
            <router-link to="/forum" class="back-btn">
              <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                <line x1="19" y1="12" x2="5" y2="12" />
                <polyline points="12 19 5 12 12 5" />
              </svg>
              返回列表
            </router-link>
            <div class="top-actions">
              <router-link v-if="userStore.isLoggedIn" to="/forum/mine" class="top-action">
                <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                  <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z" />
                </svg>
                我的帖子
              </router-link>
              <router-link v-if="userStore.isLoggedIn" to="/forum/publish" class="top-action top-action--primary">
                <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                  <line x1="12" y1="5" x2="12" y2="19" />
                  <line x1="5" y1="12" x2="19" y2="12" />
                </svg>
                发帖
              </router-link>
            </div>
          </div>

          <article class="post-card">
            <header class="post-card-header">
              <h1 class="post-title">{{ detail.title }}</h1>
              <div class="post-meta-row">
                <div class="post-author-block">
                  <div class="avatar-wrap lg">
                    <img v-if="authorAvatarUrl" :src="authorAvatarUrl" alt="" class="avatar-img" />
                    <span v-else class="avatar-fallback">{{ authorInitial }}</span>
                  </div>
                  <div class="author-text">
                    <span class="author-name">{{ detail.userName || '用户' }}</span>
                    <div class="author-details">
                      <span class="user-id">用户 ID {{ detail.userId ?? '—' }}</span>
                      <span v-if="detail.createTime" class="post-time">{{ formatDateTime(detail.createTime) }}</span>
                    </div>
                  </div>
                </div>
                <div class="post-stats">
                  <span class="stat-pill">浏览 {{ detail.viewCount ?? 0 }}</span>
                  <span class="stat-pill">点赞 {{ detail.likeCount ?? 0 }}</span>
                  <span class="stat-pill">评论 {{ detail.commentCount ?? 0 }}</span>
                </div>
              </div>
            </header>

            <div v-if="statusHint" class="status-banner">{{ statusHint }}</div>

            <div v-if="!editing" class="post-body post-html" v-html="safePostContent"></div>

            <div v-else class="edit-box">
              <div class="field">
                <label>标题</label>
                <input v-model.trim="editTitle" class="edit-input" type="text" maxlength="200" />
              </div>
              <div class="field field-rich">
                <label>正文</label>
                <RichTextEditor ref="editRichRef" v-model="editContent" />
              </div>
              <div class="edit-actions">
                <button type="button" @click="saveEdit">保存（将重新提交审核）</button>
                <button type="button" class="secondary" @click="cancelEdit">取消</button>
              </div>
            </div>

            <footer v-if="!editing" class="post-card-footer">
              <div class="post-interactions">
                <button
                  v-if="userStore.isLoggedIn && canInteract && !isAuthor"
                  type="button"
                  class="interaction-btn"
                  :class="{ 'is-active': detail.liked }"
                  @click="like"
                >
                  <svg
                    class="interaction-icon"
                    viewBox="0 0 24 24"
                    :fill="detail.liked ? 'currentColor' : 'none'"
                    stroke="currentColor"
                    stroke-width="2"
                    aria-hidden="true"
                  >
                    <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
                  </svg>
                  <span>{{ detail.liked ? '已赞' : '点赞' }} {{ detail.likeCount ?? 0 }}</span>
                </button>
                <button
                  v-if="!isAuthor && userStore.isLoggedIn && canInteract"
                  type="button"
                  class="interaction-btn"
                  :class="{ 'is-active': detail.collected }"
                  :disabled="collectBusy"
                  @click="onToggleCollect"
                >
                  <svg
                    class="interaction-icon"
                    viewBox="0 0 24 24"
                    :fill="detail.collected ? 'currentColor' : 'none'"
                    stroke="currentColor"
                    stroke-width="2"
                    aria-hidden="true"
                  >
                    <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z" />
                  </svg>
                  <span>{{ detail.collected ? '已收藏' : '收藏' }}</span>
                </button>
                <button v-if="!isAuthor" type="button" class="interaction-btn" @click="onSharePost">
                  <svg class="interaction-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                    <circle cx="18" cy="5" r="3" />
                    <circle cx="6" cy="12" r="3" />
                    <circle cx="18" cy="19" r="3" />
                    <line x1="8.59" y1="13.51" x2="15.42" y2="17.49" />
                    <line x1="15.41" y1="6.51" x2="8.59" y2="10.49" />
                  </svg>
                  <span>分享</span>
                </button>
              </div>
              <div v-if="userStore.isLoggedIn && isAuthor" class="post-author-tools">
                <button
                  v-if="!editing && !authorUnpublishedPreview"
                  type="button"
                  class="tool-btn tool-btn--ghost"
                  @click="startEdit"
                >
                  编辑
                </button>
                <button type="button" class="tool-btn tool-btn--danger" @click="removePost">删除帖子</button>
              </div>
            </footer>
          </article>

          <section class="comments-section">
            <div class="comments-header">
              <h2 class="comments-title">评论</h2>
              <CommentSortBar :active-sort="commentSort" @select="setCommentSort" />
            </div>

            <div v-if="userStore.isLoggedIn && canInteract && !isAuthor" class="comment-input-card">
              <div class="comment-input-head">
                <div class="avatar-wrap sm">
                  <img v-if="viewerAvatarUrl" :src="viewerAvatarUrl" alt="" class="avatar-img" />
                  <span v-else class="avatar-fallback">{{ viewerInitial }}</span>
                </div>
                <span class="comment-input-name">{{ userStore.user?.username || '我' }}</span>
              </div>
              <ForumCommentEditor v-model="newComment" placeholder="写下你的看法…" />
              <button type="button" class="submit-comment-btn" @click="sendComment(null)">发表评论</button>
            </div>
            <p v-else-if="!userStore.isLoggedIn" class="muted">请先登录后参与评论。</p>
            <p v-else-if="!canInteract" class="muted">该帖待审核或已驳回，审核通过后方可评论。</p>
            <CommentThreadNodes
              v-if="displayCommentTreeRoots.length"
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
                <div class="c-head">
                  <div class="avatar-wrap sm">
                    <img v-if="commentAvatarUrl(c)" :src="commentAvatarUrl(c)" alt="" class="avatar-img" />
                    <span v-else class="avatar-fallback">{{ commentInitial(c) }}</span>
                  </div>
                  <div class="c-head-main">
                    <div class="c-head-line">
                      <strong>{{ c.userName || '用户' }}</strong>
                      <span class="c-uid">ID {{ c.userId ?? '—' }}</span>
                      <span class="time">{{ formatDateTime(c.createTime) }}</span>
                    </div>
                  </div>
                </div>
                <div class="c-body comment-body comment-rich-html" v-html="commentDisplayHtml(c.content)" />
                <div class="c-foot">
                  <button
                    v-if="userStore.isLoggedIn && canInteract"
                    type="button"
                    class="link"
                    :disabled="commentLikeBusyId === c.id"
                    @click="toggleCommentLike(c)"
                  >
                    {{ c.liked ? '已赞' : '赞' }} {{ c.likeCount ?? 0 }}
                  </button>
                  <button v-if="userStore.isLoggedIn && canInteract" type="button" class="link" @click="toggleReply(c.id)">回复</button>
                  <button
                    v-if="canDeleteComment(c)"
                    type="button"
                    class="link danger"
                    @click="removeComment(c.id)"
                  >
                    删除
                  </button>
                </div>
                <div v-if="replyTo === c.id" class="reply-box">
                  <ForumCommentEditor v-model="replyText" placeholder="回复…" />
                  <div class="reply-actions">
                    <button type="button" @click="sendReply(c.id)">发送</button>
                    <button type="button" class="secondary" @click="cancelReply">取消</button>
                  </div>
                </div>
              </template>
            </CommentThreadNodes>
            <div
              v-if="commentRows.length && showCommentListFoldToggle"
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
            <p v-if="!commentRows.length && !commentsLoading" class="muted">暂无评论</p>
            <p v-if="commentsLoading" class="muted">评论加载中…</p>
            <PaginationBar
              v-model="commentPage"
              class="comment-pager"
              :total="commentTotal"
              :page-size="commentPageSize"
              @current-change="() => loadComments(false)"
            />
          </section>
        </div>

        <aside class="detail-aside" aria-label="侧栏">
          <div class="aside-card">
            <h3 class="aside-title">作者</h3>
            <div class="author-aside">
              <div class="avatar-wrap aside-avatar">
                <img v-if="authorAvatarUrl" :src="authorAvatarUrl" alt="" class="avatar-img" />
                <span v-else class="avatar-fallback">{{ authorInitial }}</span>
              </div>
              <div class="author-aside-text">
                <div class="author-aside-name">{{ detail.userName || '用户' }}</div>
                <div class="author-aside-id">用户 ID {{ detail.userId ?? '—' }}</div>
              </div>
            </div>
            <p class="aside-hint">以上内容由该用户发布，观点不代表本站立场。</p>
          </div>
          <div class="aside-card">
            <h3 class="aside-title">论坛导航</h3>
            <nav class="aside-nav">
              <router-link to="/forum" class="aside-link">返回论坛列表</router-link>
              <router-link v-if="userStore.isLoggedIn" to="/forum/mine" class="aside-link">我的帖子</router-link>
              <router-link v-if="userStore.isLoggedIn" to="/forum/publish" class="aside-link aside-link--accent">发布新帖</router-link>
            </nav>
          </div>
        </aside>
      </div>
    </div>
    <div v-else class="detail-loading">加载中…</div>
  </KnowledgeModuleShell>
</template>

<script setup>
import { ref, computed, onMounted, watch, nextTick, provide, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  getPostDetail,
  likePost,
  getCommentList,
  submitComment,
  deleteComment,
  likeForumComment,
  updatePost,
  deletePost,
  collectForumPost,
  uncollectForumPost,
} from '@/api/forum'
import { useUserStore } from '@/stores/user'
import { resolveMediaUrl } from '@/utils/resolveMediaUrl'
import { formatDateTime } from '@/utils/formatDateTime'
import { getPostDisplayHtml, plainToEditorHtml, isEmptyHtml } from '@/utils/htmlContent'
import RichTextEditor from '@/components/RichTextEditor.vue'
import ForumCommentEditor from '@/components/ForumCommentEditor.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import KnowledgeModuleShell from '@/components/KnowledgeModuleShell.vue'
import CommentSortBar from '@/components/CommentSortBar.vue'
import CommentThreadNodes from '@/components/CommentThreadNodes.vue'
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

const crumbs = computed(() => {
  const base = [
    { label: '首页', to: '/home' },
    { label: '论坛', to: '/forum' },
  ]
  if (!detail.value) return base
  const title = detail.value.title || '帖子'
  const short = title.length > 22 ? `${title.slice(0, 22)}…` : title
  return [...base, { label: short, titleAttr: title }]
})
const comments = ref([])
const commentLikeBusyId = ref(null)
const commentPage = ref(1)
const commentPageSize = 10
const commentTotal = ref(0)
const commentsLoading = ref(false)
const commentSort = ref('time')
const commentRows = computed(() => flattenCommentTree(buildCommentTree(comments.value, commentSort.value)))

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

watch([commentPage, commentSort], () => {
  commentListExpanded.value = false
  for (const k of Object.keys(replyBranchExpanded)) delete replyBranchExpanded[k]
})

const newComment = ref('')
const replyTo = ref(null)
const replyText = ref('')
const collectBusy = ref(false)
const editing = ref(false)
const editTitle = ref('')
const editContent = ref('')
const editRichRef = ref(null)

const safePostContent = computed(() => getPostDisplayHtml(detail.value?.content || ''))

const uid = computed(() => userStore.user?.userId ?? userStore.user?.id)
const isAuthor = computed(() => detail.value && uid.value != null && String(detail.value.userId) === String(uid.value))
const canInteract = computed(() => detail.value && Number(detail.value.status) === 1)
/** 待审核/驳回：不显示编辑，仅删除 */
const authorUnpublishedPreview = computed(() => isAuthor.value && !canInteract.value)

const statusHint = computed(() => {
  const d = detail.value
  if (!d || !isAuthor.value) return ''
  const s = Number(d.status)
  if (s === 0) return '待审核：仅你自己可见，通过后将公开展示。'
  if (s === -1) return `已驳回${d.rejectReason ? '：' + d.rejectReason : ''}。修改保存后将重新进入待审核。`
  return ''
})

const authorAvatarUrl = computed(() => {
  const a = detail.value?.avatar
  return a ? resolveMediaUrl(a) : ''
})

const authorInitial = computed(() => {
  const n = detail.value?.userName || '用'
  return String(n).charAt(0).toUpperCase()
})

const viewerAvatarUrl = computed(() => {
  const a = userStore.user?.avatar
  return a ? resolveMediaUrl(a) : ''
})

const viewerInitial = computed(() => {
  const n = userStore.user?.username || '?'
  return String(n).charAt(0).toUpperCase()
})

function commentAvatarUrl(c) {
  const a = c?.avatar
  return a ? resolveMediaUrl(a) : ''
}

function commentInitial(c) {
  const n = c?.userName || '?'
  return String(n).charAt(0).toUpperCase()
}

function commentDisplayHtml(raw) {
  return getPostDisplayHtml(raw || '')
}

async function loadDetail(recordView = true) {
  try {
    detail.value = await getPostDetail(id.value, { recordView })
  } catch (e) {
    detail.value = null
    alert(e?.message || '帖子不存在或无权查看')
    router.push('/forum')
  }
}

function setCommentSort(mode) {
  if (commentSort.value === mode) return
  commentSort.value = mode
  loadComments(true)
}

async function loadComments(reset = true) {
  if (!id.value || !detail.value) return
  if (reset) {
    commentPage.value = 1
  }
  commentsLoading.value = true
  try {
    const data = await getCommentList(id.value, {
      pageNum: commentPage.value,
      pageSize: commentPageSize,
      sortBy: commentSort.value === 'hot' ? 'hot' : 'time',
    })
    const records = data.records ?? data.list ?? []
    comments.value = records
    commentTotal.value = Number(data.total ?? records.length)
  } catch {
    if (reset) {
      comments.value = []
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

async function load() {
  await loadDetail(true)
  await loadComments(true)
  await nextTick()
  scrollToCommentFromQuery()
}

/** 点赞/评论等之后刷新帖子与评论，不计浏览量 */
async function refreshDetailAndComments() {
  await loadDetail(false)
  await loadComments(true)
  await nextTick()
  scrollToCommentFromQuery()
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


async function like() {
  try {
    const res = await likePost(id.value)
    if (detail.value) {
      detail.value = {
        ...detail.value,
        liked: !!res?.liked,
        likeCount: res?.likeCount != null ? Number(res.likeCount) : detail.value.likeCount ?? 0,
      }
    }
  } catch (e) {
    alert(e?.message || '操作失败')
  }
}

async function onToggleCollect() {
  if (!userStore.isLoggedIn || !canInteract.value) return
  collectBusy.value = true
  try {
    if (detail.value?.collected) {
      await uncollectForumPost(id.value)
      detail.value = { ...detail.value, collected: false }
    } else {
      await collectForumPost(id.value)
      detail.value = { ...detail.value, collected: true }
    }
  } catch (e) {
    alert(e?.message || '收藏操作失败')
  } finally {
    collectBusy.value = false
  }
}

function onSharePost() {
  const url = `${window.location.origin}${window.location.pathname}#/forum/${id.value}`
  if (navigator.clipboard?.writeText) {
    navigator.clipboard.writeText(url).then(() => alert('分享链接已复制')).catch(() => prompt('请复制分享链接：', url))
  } else {
    prompt('请复制分享链接：', url)
  }
}

async function sendComment(parentId) {
  const text = parentId == null ? newComment.value : replyText.value
  if (isEmptyHtml(text)) return
  try {
    await submitComment({ postId: Number(id.value), content: text, parentId: parentId ?? undefined })
    newComment.value = ''
    replyText.value = ''
    replyTo.value = null
    refreshDetailAndComments()
  } catch (e) {
    alert(e?.message || '评论失败')
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

function sendReply(parentId) {
  sendComment(parentId)
}

function isCommentAuthor(c) {
  return uid.value != null && String(c.userId) === String(uid.value)
}

/** 评论作者可删；已通过帖子的楼主也可删任意评论 */
function canDeleteComment(c) {
  if (!userStore.isLoggedIn) return false
  if (isCommentAuthor(c)) return true
  return isAuthor.value && canInteract.value
}

async function removeComment(cid) {
  if (!confirm('确定删除该评论？')) return
  try {
    await deleteComment(cid)
    refreshDetailAndComments()
  } catch (e) {
    alert(e?.message || '删除失败')
  }
}

async function toggleCommentLike(c) {
  if (!userStore.isLoggedIn) {
    alert('请先登录')
    return
  }
  commentLikeBusyId.value = c.id
  try {
    const res = await likeForumComment(c.id)
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

function startEdit() {
  editTitle.value = detail.value.title || ''
  editContent.value = plainToEditorHtml(detail.value.content || '')
  editing.value = true
}

function cancelEdit() {
  editing.value = false
}

async function saveEdit() {
  const html = editRichRef.value?.getHtml?.() ?? editContent.value
  if (!editTitle.value || isEmptyHtml(html)) {
    alert('请填写标题与正文')
    return
  }
  try {
    await updatePost(id.value, { title: editTitle.value, content: html })
    editing.value = false
    refreshDetailAndComments()
  } catch (e) {
    alert(e?.message || '保存失败')
  }
}

async function removePost() {
  if (!confirm('确定删除该帖子？')) return
  try {
    await deletePost(id.value)
    router.push('/forum')
  } catch (e) {
    alert(e?.message || '删除失败')
  }
}

watch(id, () => load())
onMounted(load)
</script>

<style scoped>
.detail-page {
  min-width: 0;
}

.detail-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(220px, 280px);
  gap: clamp(16px, 2.5vw, 28px);
  align-items: start;
}

.detail-main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.post-actions-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 10px;
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
  transition: border-color 0.2s, color 0.2s, box-shadow 0.2s;
}

.back-btn:hover {
  border-color: var(--client-primary);
  color: var(--client-primary);
  box-shadow: 0 2px 10px rgba(59, 130, 246, 0.12);
}

.top-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.top-action {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border: 1px solid rgba(59, 130, 246, 0.22);
  border-radius: 10px;
  background: var(--client-surface);
  color: var(--client-muted);
  font-size: 0.8125rem;
  font-weight: 600;
  text-decoration: none;
  transition: border-color 0.2s, color 0.2s, background 0.2s;
}

.top-action:hover {
  border-color: var(--client-primary);
  color: var(--client-primary);
  background: var(--client-accent-soft);
}

.top-action--primary {
  border-color: transparent;
  color: #fff;
  background: linear-gradient(135deg, var(--client-primary), var(--client-primary-hover));
  box-shadow: 0 4px 14px rgba(59, 130, 246, 0.35);
}

.top-action--primary:hover {
  color: #fff;
  filter: brightness(1.05);
  box-shadow: 0 6px 18px rgba(59, 130, 246, 0.42);
}

.icon {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
}

.post-card {
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(59, 130, 246, 0.12);
  border-radius: var(--client-radius);
  padding: clamp(18px, 2.5vw, 28px);
  box-shadow: 0 1px 0 rgba(59, 130, 246, 0.06);
}

.post-card-header {
  margin-bottom: 1.25rem;
  padding-bottom: 1.25rem;
  border-bottom: 1px solid rgba(59, 130, 246, 0.12);
}

.post-title {
  margin: 0 0 1rem;
  font-size: clamp(1.25rem, 2.5vw, 1.75rem);
  font-weight: 800;
  color: var(--client-text);
  line-height: 1.35;
  letter-spacing: -0.02em;
}

.post-meta-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 14px;
}

.post-author-block {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.avatar-wrap {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
  background: rgba(59, 130, 246, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-wrap.lg {
  width: 52px;
  height: 52px;
}

.avatar-wrap.sm {
  width: 36px;
  height: 36px;
}

.avatar-wrap.aside-avatar {
  width: 56px;
  height: 56px;
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-fallback {
  font-size: 18px;
  font-weight: 700;
  color: var(--client-primary);
}

.avatar-wrap.sm .avatar-fallback {
  font-size: 14px;
}

.author-text {
  min-width: 0;
}

.post-author-block .author-name {
  display: block;
  font-size: 0.9375rem;
  font-weight: 700;
  color: var(--client-text);
}

.author-details {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 4px;
  font-size: 0.75rem;
  color: var(--client-muted);
}

.post-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.stat-pill {
  padding: 6px 10px;
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--client-muted);
  background: rgba(59, 130, 246, 0.08);
  border: 1px solid rgba(59, 130, 246, 0.1);
}

.status-banner {
  background: rgba(254, 243, 199, 0.45);
  border: 1px solid rgba(251, 191, 36, 0.45);
  color: #92400e;
  padding: 10px 14px;
  border-radius: 10px;
  margin-bottom: 16px;
  font-size: 0.875rem;
  line-height: 1.5;
}

.post-body.post-html {
  margin: 0 0 8px;
  line-height: 1.75;
  word-break: break-word;
  color: var(--client-text);
  font-size: 0.9375rem;
}

.post-body.post-html :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: 8px;
}

.post-body.post-html :deep(p) {
  margin: 0.5em 0;
}

.post-body.post-html :deep(p:first-child) {
  margin-top: 0;
}

.post-body.post-html :deep(p:last-child) {
  margin-bottom: 0;
}

.post-card-footer {
  margin-top: 1.25rem;
  padding-top: 1.25rem;
  border-top: 1px solid rgba(59, 130, 246, 0.12);
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.post-interactions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 20px;
}

.interaction-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 0;
  border: none;
  background: none;
  color: var(--client-muted);
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  transition: color 0.2s, transform 0.15s;
}

.interaction-btn:hover:not(:disabled) {
  color: var(--client-primary);
  transform: scale(1.03);
}

.interaction-btn.is-active {
  color: var(--client-primary);
}

.interaction-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.interaction-icon {
  width: 20px;
  height: 20px;
}

.post-author-tools {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.tool-btn {
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  border: none;
  transition: background 0.2s, transform 0.15s;
}

.tool-btn--ghost {
  background: rgba(59, 130, 246, 0.1);
  color: var(--client-text);
}

.tool-btn--ghost:hover {
  background: rgba(59, 130, 246, 0.16);
}

.tool-btn--danger {
  background: #ef4444;
  color: #fff;
}

.tool-btn--danger:hover {
  background: #dc2626;
}

.edit-box {
  margin: 0;
  padding: 16px;
  background: rgba(59, 130, 246, 0.04);
  border: 1px solid rgba(59, 130, 246, 0.12);
  border-radius: calc(var(--client-radius) - 2px);
}

.edit-box .field {
  margin-bottom: 14px;
}

.edit-box .field-rich {
  max-width: 100%;
}

.edit-box label {
  display: block;
  margin-bottom: 6px;
  font-weight: 600;
  font-size: 0.8125rem;
  color: var(--client-text);
}

.edit-input {
  width: 100%;
  box-sizing: border-box;
  padding: 10px 12px;
  border: 1px solid rgba(59, 130, 246, 0.22);
  border-radius: 8px;
  font-size: 0.875rem;
}

.edit-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 8px;
}

.edit-actions button {
  padding: 8px 16px;
  background: var(--client-primary);
  color: #fff;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 0.875rem;
  font-weight: 600;
}

.edit-actions button.secondary {
  background: rgba(148, 163, 184, 0.25);
  color: var(--client-text);
}

.comments-section {
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(59, 130, 246, 0.12);
  border-radius: var(--client-radius);
  padding: clamp(16px, 2vw, 24px);
  box-shadow: 0 1px 0 rgba(59, 130, 246, 0.06);
}

.comments-header {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 1.25rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid rgba(59, 130, 246, 0.1);
}

.comments-title {
  margin: 0;
  font-size: 1.125rem;
  font-weight: 800;
  color: var(--client-text);
}

.comments-header :deep(.comment-sort-label) {
  color: var(--client-muted);
}

.comments-header :deep(.comment-sort-tabs) {
  border-color: rgba(59, 130, 246, 0.2);
  border-radius: 8px;
  overflow: hidden;
}

.comments-header :deep(.comment-sort-btn) {
  background: var(--client-surface);
  color: var(--client-muted);
}

.comments-header :deep(.comment-sort-btn + .comment-sort-btn) {
  border-left-color: rgba(59, 130, 246, 0.15);
}

.comments-header :deep(.comment-sort-btn:hover) {
  color: var(--client-primary);
}

.comments-header :deep(.comment-sort-btn.active) {
  background: var(--client-primary);
  color: #fff;
  font-weight: 700;
}

.comment-input-card {
  margin-bottom: 1.25rem;
  padding: 14px;
  border-radius: calc(var(--client-radius) - 2px);
  border: 1px solid rgba(59, 130, 246, 0.15);
  background: rgba(59, 130, 246, 0.04);
  transition: border-color 0.2s, box-shadow 0.2s;
}

.comment-input-card:focus-within {
  border-color: var(--client-primary);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.12);
}

.comment-input-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.comment-input-name {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--client-text);
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
  transition: background 0.2s, transform 0.15s;
}

.submit-comment-btn:hover {
  background: var(--client-primary-hover);
  transform: translateY(-1px);
}

.muted {
  color: var(--client-muted);
  font-size: 0.875rem;
  margin: 0 0 12px;
  line-height: 1.5;
}

.comment-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.comment-item {
  padding: 14px 12px;
  margin-bottom: 10px;
  border-radius: 10px;
  border: 1px solid rgba(59, 130, 246, 0.08);
  border-left: 3px solid transparent;
  background: rgba(255, 255, 255, 0.6);
  transition: border-color 0.2s, box-shadow 0.2s;
}

.comment-item:hover {
  border-color: rgba(59, 130, 246, 0.18);
  box-shadow: 0 4px 14px rgba(59, 130, 246, 0.06);
}

.comment-item.comment-locate-flash {
  animation: commentLocateFlash 2.4s ease;
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

.comment-item.depth-1,
.comment-item.depth-2,
.comment-item.depth-3,
.comment-item.depth-4,
.comment-item.depth-5,
.comment-item.depth-6 {
  border-left-color: var(--client-accent-soft);
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

.reply-line-snippet {
  opacity: 0.9;
}

.c-head {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  font-size: 14px;
}

.c-head-main {
  flex: 1;
  min-width: 0;
}

.c-head-line {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 8px;
}

.c-head-line strong {
  color: var(--client-text);
}

.c-uid {
  font-size: 12px;
  color: var(--client-muted);
}

.time {
  color: var(--client-muted);
  font-size: 12px;
}

.comments-section :deep(.c-body.comment-rich-html) {
  margin: 8px 0;
  line-height: 1.55;
  word-break: break-word;
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

.c-foot {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.link {
  background: none;
  border: none;
  color: var(--client-primary);
  cursor: pointer;
  padding: 0;
  font-size: 13px;
  font-weight: 600;
}

.link:hover {
  color: var(--client-primary-hover);
  text-decoration: underline;
}

.link.danger {
  color: #dc2626;
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
  font-size: 14px;
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
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: border-color 0.2s, background 0.2s;
}

.comment-list-fold-btn:hover {
  border-color: var(--client-primary);
  background: var(--client-accent-soft);
}

.comment-pager {
  margin-top: 16px;
}

.detail-aside {
  position: sticky;
  top: 12px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.aside-card {
  padding: 16px 14px;
  border-radius: calc(var(--client-radius) - 2px);
  background: rgba(255, 255, 255, 0.85);
  border: 1px solid rgba(59, 130, 246, 0.12);
}

.aside-title {
  margin: 0 0 12px;
  font-size: 0.9375rem;
  font-weight: 800;
  color: var(--client-text);
}

.author-aside {
  display: flex;
  align-items: center;
  gap: 12px;
}

.author-aside-text {
  min-width: 0;
}

.author-aside-name {
  font-size: 0.9375rem;
  font-weight: 700;
  color: var(--client-text);
}

.author-aside-id {
  font-size: 0.75rem;
  color: var(--client-muted);
  margin-top: 4px;
}

.aside-hint {
  margin: 12px 0 0;
  font-size: 0.75rem;
  color: var(--client-muted);
  line-height: 1.45;
}

.aside-nav {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.aside-link {
  display: block;
  padding: 8px 10px;
  border-radius: 8px;
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--client-text);
  text-decoration: none;
  transition: background 0.2s, color 0.2s;
}

.aside-link:hover {
  background: var(--client-accent-soft);
  color: var(--client-primary-hover);
}

.aside-link--accent {
  text-align: center;
  margin-top: 4px;
  background: linear-gradient(135deg, var(--client-primary), var(--client-primary-hover));
  color: #fff !important;
}

.aside-link--accent:hover {
  filter: brightness(1.05);
  box-shadow: 0 4px 14px rgba(59, 130, 246, 0.35);
}

.detail-loading {
  padding: 48px 16px;
  text-align: center;
  color: var(--client-muted);
  font-size: 0.9375rem;
}

@media (max-width: 1024px) {
  .detail-layout {
    grid-template-columns: 1fr;
  }

  .detail-aside {
    position: static;
  }
}
</style>
