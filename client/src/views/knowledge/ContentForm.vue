<template>
  <KnowledgeModuleShell :crumbs="crumbs">
    <form class="kc-page" @submit.prevent="submit">
      <div class="kc-layout">
        <div class="kc-main">
          <div class="kc-card">
            <header class="kc-card-head">
              <h1 class="kc-title">{{ isEdit ? '编辑知识' : '新增知识' }}</h1>
              <p class="kc-sub">
                填写标题、分类与正文；可先保存为草稿，或直接点击右侧「提交审核」进入待管理员审核
              </p>
            </header>

            <div class="kc-fields">
              <div class="kc-field">
                <label class="kc-label kc-label--req">标题</label>
                <div class="kc-input-wrap">
                  <input
                    v-model="form.title"
                    type="text"
                    maxlength="200"
                    class="kc-input"
                    placeholder="请输入知识标题"
                    required
                  />
                  <span class="kc-char-count">{{ form.title.length }}/200</span>
                </div>
              </div>

              <div class="kc-field">
                <label class="kc-label kc-label--req">分类</label>
                <div class="kc-select-wrap">
                  <select v-model="form.categoryId" class="kc-select" required>
                    <option value="" disabled>请选择分类</option>
                    <option v-for="c in categories" :key="c.id" :value="String(c.id)">{{ c.name }}</option>
                  </select>
                  <span class="kc-select-arrow" aria-hidden="true">
                    <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M6 9l6 6 6-6" stroke-linecap="round" stroke-linejoin="round" />
                    </svg>
                  </span>
                </div>
              </div>

              <div class="kc-field">
                <label class="kc-label">摘要</label>
                <textarea
                  v-model="form.summary"
                  class="kc-textarea kc-textarea--summary"
                  rows="4"
                  placeholder="选填，用于列表摘要展示"
                />
              </div>

              <div class="kc-field kc-field--rich">
                <label class="kc-label kc-label--req">正文</label>
                <div class="kc-rich-shell">
                  <RichTextEditor ref="richRef" v-model="form.content" />
                </div>
              </div>
            </div>
          </div>
        </div>

        <aside class="kc-sidebar">
          <div class="kc-card kc-sidebar-card">
            <h3 class="kc-sidebar-title">封面设置</h3>
            <div class="kc-cover-wrap">
              <input
                ref="coverFileRef"
                type="file"
                accept="image/jpeg,image/png,image/gif,image/webp"
                class="kc-file-input"
                @change="onCoverFile"
              />
              <div v-if="form.cover" class="kc-cover-preview">
                <img :src="coverDisplayUrl" alt="封面预览" />
                <div class="kc-cover-overlay">
                  <button type="button" class="kc-cover-btn" :disabled="coverUploading" @click="coverFileRef?.click()">
                    {{ coverUploading ? '上传中…' : '更换' }}
                  </button>
                  <button type="button" class="kc-cover-btn kc-cover-btn--danger" @click="form.cover = ''">移除</button>
                </div>
              </div>
              <button
                v-else
                type="button"
                class="kc-upload-box"
                :disabled="coverUploading"
                @click="coverFileRef?.click()"
              >
                <span class="kc-upload-icon" aria-hidden="true">
                  <svg viewBox="0 0 24 24" width="32" height="32" fill="none" stroke="currentColor" stroke-width="1.5">
                    <rect x="3" y="3" width="18" height="18" rx="2" ry="2" />
                    <circle cx="8.5" cy="8.5" r="1.5" />
                    <path d="M21 15l-5-5L5 21" />
                  </svg>
                </span>
                <p class="kc-upload-text">未设置封面</p>
                <span class="kc-upload-cta">{{ coverUploading ? '上传中…' : '选择图片' }}</span>
              </button>
            </div>
            <p class="kc-upload-tip">与头像相同：jpg / png / gif / webp，不超过 2MB</p>
          </div>

          <div class="kc-card kc-sidebar-card kc-publish-card">
            <div class="kc-status" :class="'kc-status--' + statusVariant">
              <span class="kc-status-dot" aria-hidden="true" />
              <span class="kc-status-text">
                当前状态：<strong>{{ statusLabelText }}</strong>
              </span>
            </div>
            <p v-if="err" class="kc-err">{{ err }}</p>
            <div class="kc-actions">
              <button type="submit" class="kc-btn kc-btn--primary" :disabled="loading || publishLoading">
                {{ isEdit ? '保存修改' : '保存为草稿' }}
              </button>
              <button
                v-if="showSubmitReview"
                type="button"
                class="kc-btn kc-btn--review"
                :disabled="loading || publishLoading"
                @click="submitForReview"
              >
                {{ publishLoading ? '提交中…' : '提交审核' }}
              </button>
              <router-link to="/knowledge/drafts" class="kc-btn kc-btn--ghost">返回我的知识</router-link>
            </div>
            <p class="kc-autosave" :class="{ 'is-active': autoSaving }">
              {{ autoSaving ? '草稿已自动保存到本地' : '编辑中会自动保存草稿到本地' }}
            </p>
          </div>
        </aside>
      </div>
    </form>
  </KnowledgeModuleShell>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  getContentDetail,
  getCategoryList,
  saveContent,
  updateContent,
  uploadCoverImage,
  publishContent,
} from '@/api/content'
import { resolveMediaUrl } from '@/utils/resolveMediaUrl'
import RichTextEditor from '@/components/RichTextEditor.vue'
import KnowledgeModuleShell from '@/components/KnowledgeModuleShell.vue'

const route = useRoute()
const router = useRouter()
const id = computed(() => route.params.id)
const isEdit = computed(() => !!id.value)

const crumbs = computed(() => {
  const tail = isEdit.value ? '编辑知识' : '新增知识'
  return [
    { label: '首页', to: '/home' },
    { label: '消防知识', to: '/knowledge' },
    { label: '我的知识', to: '/knowledge/drafts' },
    { label: tail },
  ]
})

const categories = ref([])
const form = reactive({
  title: '',
  categoryId: '',
  summary: '',
  content: '',
  cover: '',
})
const loading = ref(false)
const publishLoading = ref(false)
const err = ref('')
const currentUpdateTime = ref('')
const contentStatus = ref(0)
const autoSaving = ref(false)
const richRef = ref(null)
const coverFileRef = ref(null)
const coverUploading = ref(false)

const draftCacheKey = computed(() => `knowledge_draft_cache_${id.value || 'new'}`)

const coverDisplayUrl = computed(() => resolveMediaUrl(form.cover || ''))

const statusLabelText = computed(() => {
  const n = Number(contentStatus.value)
  if (n === 0) return '草稿'
  if (n === 1) return '已发布'
  if (n === 2) return '已下架'
  if (n === 3) return '待审核'
  return '草稿'
})

const statusVariant = computed(() => {
  const n = Number(contentStatus.value)
  if (n === 1) return 'on'
  if (n === 3) return 'pending'
  if (n === 2) return 'off'
  return 'draft'
})

/** 新建、草稿、已下架可提交审核；已发布、待审核不显示 */
const showSubmitReview = computed(() => {
  if (!isEdit.value) return true
  const s = Number(contentStatus.value)
  if (s === 1 || s === 3) return false
  return true
})

function errMsg(e) {
  if (typeof e === 'string') return e
  return e?.message || '操作失败'
}

async function loadCategories() {
  try {
    const data = await getCategoryList()
    categories.value = Array.isArray(data) ? data : (data.list ?? [])
  } catch (_) {}
}

async function load() {
  if (!isEdit.value) {
    contentStatus.value = 0
    return
  }
  try {
    const data = await getContentDetail(id.value)
    form.title = data.title ?? ''
    form.categoryId = data.categoryId != null ? String(data.categoryId) : ''
    form.summary = data.summary ?? ''
    form.content = data.content ?? ''
    form.cover = data.cover ?? ''
    currentUpdateTime.value = data.updateTime ?? ''
    contentStatus.value = data.status != null ? Number(data.status) : 0
  } catch (e) {
    err.value = errMsg(e) || '加载失败'
  }
}

function isContentEmpty(html) {
  if (html == null || !String(html).trim()) return true
  const stripped = String(html)
    .replace(/<p><br\s*\/?><\/p>/gi, '')
    .replace(/<p>\s*<\/p>/gi, '')
    .replace(/<br\s*\/?>/gi, '')
    .replace(/&nbsp;/gi, ' ')
    .replace(/<[^>]+>/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
  return !stripped
}

async function onCoverFile(e) {
  const file = e.target.files?.[0]
  if (!file) return
  coverUploading.value = true
  err.value = ''
  try {
    const data = await uploadCoverImage(file)
    const url = data?.url
    if (url) form.cover = url
  } catch (e) {
    err.value = errMsg(e)
  } finally {
    coverUploading.value = false
    e.target.value = ''
  }
}

function buildSaveBody() {
  const html = richRef.value?.getHtml?.() ?? form.content
  if (isContentEmpty(html)) {
    err.value = '请填写正文'
    return null
  }
  if (!form.title?.trim()) {
    err.value = '请填写标题'
    return null
  }
  if (!form.categoryId) {
    err.value = '请选择分类'
    return null
  }
  return {
    title: form.title.trim(),
    categoryId: Number(form.categoryId),
    summary: form.summary,
    content: html,
    cover: form.cover,
    status: 0,
    updateTime: currentUpdateTime.value || undefined,
  }
}

async function submit() {
  err.value = ''
  const body = buildSaveBody()
  if (!body) return
  loading.value = true
  try {
    if (isEdit.value) {
      await updateContent(id.value, body)
      alert('已保存')
      clearDraftCache()
    } else {
      await saveContent(body)
      alert('已保存为草稿，可在「我的知识」中提交审核')
      clearDraftCache()
      router.push('/knowledge/drafts')
      return
    }
    router.push('/knowledge/drafts')
  } catch (e) {
    err.value = errMsg(e) || '保存失败'
  } finally {
    loading.value = false
  }
}

async function submitForReview() {
  err.value = ''
  const body = buildSaveBody()
  if (!body) return
  publishLoading.value = true
  try {
    if (isEdit.value) {
      await updateContent(id.value, body)
      await publishContent(id.value)
    } else {
      const res = await saveContent(body)
      const newId = res?.id
      if (newId == null) {
        err.value = '保存成功但未返回内容 ID，请稍后重试'
        return
      }
      await publishContent(newId)
    }
    clearDraftCache()
    alert('已提交审核，请耐心等待管理员处理')
    router.push('/knowledge/drafts')
  } catch (e) {
    err.value = errMsg(e) || '提交审核失败'
  } finally {
    publishLoading.value = false
  }
}

function saveDraftCache() {
  autoSaving.value = true
  const payload = {
    title: form.title,
    categoryId: form.categoryId,
    summary: form.summary,
    content: form.content,
    cover: form.cover,
    updateTime: currentUpdateTime.value,
    ts: Date.now(),
  }
  localStorage.setItem(draftCacheKey.value, JSON.stringify(payload))
  setTimeout(() => {
    autoSaving.value = false
  }, 300)
}

function restoreDraftCache() {
  if (isEdit.value) return

  const raw = localStorage.getItem(draftCacheKey.value)
  if (!raw) return
  try {
    const draft = JSON.parse(raw)
    if (!draft) return
    if (!confirm('检测到未保存草稿，是否恢复？')) return
    form.title = draft.title ?? form.title
    form.categoryId = draft.categoryId ?? form.categoryId
    form.summary = draft.summary ?? form.summary
    form.content = draft.content ?? form.content
    form.cover = draft.cover ?? form.cover
    currentUpdateTime.value = draft.updateTime ?? currentUpdateTime.value
  } catch (_) {}
}

function clearDraftCache() {
  localStorage.removeItem(draftCacheKey.value)
}

onMounted(() => {
  loadCategories()
  if (isEdit.value) {
    load()
  } else {
    load().then(restoreDraftCache)
  }
})

watch(form, () => {
  saveDraftCache()
}, { deep: true })
</script>

<style scoped>
.kc-page {
  width: 100%;
  min-width: 0;
}

.kc-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(260px, 320px);
  gap: 22px;
  align-items: flex-start;
}

.kc-main {
  min-width: 0;
}

.kc-card {
  background: var(--client-surface);
  border-radius: var(--client-radius);
  border: 1px solid rgba(59, 130, 246, 0.12);
  box-shadow: 0 2px 16px rgba(59, 130, 246, 0.06);
  overflow: hidden;
}

.kc-main .kc-card {
  padding: 24px 26px 28px;
}

.kc-card-head {
  margin-bottom: 24px;
  padding-bottom: 18px;
  border-bottom: 1px solid rgba(59, 130, 246, 0.1);
}

.kc-title {
  margin: 0 0 8px;
  font-size: 1.5rem;
  font-weight: 800;
  color: var(--client-text);
  letter-spacing: -0.02em;
}

.kc-sub {
  margin: 0;
  font-size: 0.875rem;
  color: var(--client-muted);
  line-height: 1.55;
}

.kc-fields {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.kc-field {
  margin: 0;
}

.kc-label {
  display: block;
  margin-bottom: 8px;
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--client-text);
}

.kc-label--req::after {
  content: '*';
  color: #dc2626;
  margin-left: 4px;
}

.kc-input-wrap {
  position: relative;
}

.kc-input,
.kc-select,
.kc-textarea {
  width: 100%;
  box-sizing: border-box;
  padding: 12px 14px;
  font-size: 0.9375rem;
  color: var(--client-text);
  background: rgba(240, 247, 252, 0.55);
  border: 1px solid rgba(59, 130, 246, 0.2);
  border-radius: 10px;
  outline: none;
  transition:
    border-color 0.2s,
    box-shadow 0.2s,
    background 0.2s;
}

.kc-input:focus,
.kc-textarea:focus {
  border-color: var(--client-primary);
  background: var(--client-surface);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.12);
}

.kc-char-count {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 0.75rem;
  color: var(--client-muted);
  pointer-events: none;
}

.kc-input {
  padding-right: 88px;
}

.kc-select-wrap {
  position: relative;
}

.kc-select {
  appearance: none;
  padding-right: 40px;
  cursor: pointer;
}

.kc-select:focus {
  border-color: var(--client-primary);
  background: var(--client-surface);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.12);
}

.kc-select-arrow {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--client-muted);
  pointer-events: none;
  display: flex;
}

.kc-textarea {
  resize: vertical;
  min-height: 96px;
  line-height: 1.55;
  font-family: inherit;
}

.kc-textarea--summary {
  min-height: 100px;
}

.kc-field--rich {
  max-width: 100%;
}

.kc-rich-shell {
  border-radius: 10px;
  border: 1px solid rgba(59, 130, 246, 0.18);
  overflow: visible;
  background: var(--client-surface);
}

.kc-rich-shell :deep(.rich-wrap) {
  border: none;
}

.kc-rich-shell :deep(.rich-toolbar) {
  border-bottom: 1px solid rgba(59, 130, 246, 0.1);
}

.kc-rich-shell :deep(.rich-editor) {
  min-height: 380px;
}

/* —— 侧边栏 —— */
.kc-sidebar {
  position: sticky;
  top: 16px;
  display: flex;
  flex-direction: column;
  gap: 18px;
  min-width: 0;
}

.kc-sidebar-card {
  padding: 20px 18px 22px;
}

.kc-sidebar-title {
  margin: 0 0 14px;
  font-size: 1rem;
  font-weight: 800;
  color: var(--client-text);
}

.kc-file-input {
  display: none;
}

.kc-cover-wrap {
  margin-bottom: 4px;
}

.kc-upload-box {
  width: 100%;
  min-height: 168px;
  padding: 20px 14px;
  border: 2px dashed rgba(59, 130, 246, 0.2);
  border-radius: var(--client-radius);
  background: rgba(240, 247, 252, 0.65);
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  transition:
    border-color 0.2s,
    background 0.2s;
  font: inherit;
}

.kc-upload-box:hover:not(:disabled) {
  border-color: var(--client-primary);
  background: var(--client-accent-soft);
}

.kc-upload-box:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.kc-upload-icon {
  color: var(--client-muted);
}

.kc-upload-text {
  margin: 0;
  font-size: 0.8125rem;
  color: var(--client-muted);
}

.kc-upload-cta {
  margin-top: 4px;
  padding: 6px 14px;
  border-radius: 8px;
  border: 1px solid rgba(59, 130, 246, 0.25);
  background: var(--client-surface);
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--client-primary);
}

.kc-cover-preview {
  position: relative;
  height: 168px;
  border-radius: var(--client-radius);
  overflow: hidden;
  border: 1px solid rgba(59, 130, 246, 0.15);
  background: rgba(59, 130, 246, 0.06);
}

.kc-cover-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.kc-cover-overlay {
  position: absolute;
  inset: 0;
  background: rgba(30, 58, 95, 0.55);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  opacity: 0;
  transition: opacity 0.2s;
}

.kc-cover-preview:hover .kc-cover-overlay {
  opacity: 1;
}

.kc-cover-btn {
  padding: 8px 14px;
  border-radius: 8px;
  border: none;
  font-size: 0.75rem;
  font-weight: 700;
  cursor: pointer;
  background: var(--client-surface);
  color: var(--client-primary);
}

.kc-cover-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.kc-cover-btn--danger {
  background: #fff;
  color: #dc2626;
}

.kc-upload-tip {
  margin: 12px 0 0;
  font-size: 0.75rem;
  color: var(--client-muted);
  line-height: 1.45;
}

/* Publish */
.kc-publish-card {
  padding-bottom: 20px;
}

.kc-status {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  margin-bottom: 14px;
  border-radius: 10px;
  font-size: 0.875rem;
  color: var(--client-muted);
  background: rgba(240, 247, 252, 0.65);
  border: 1px solid rgba(59, 130, 246, 0.1);
}

.kc-status strong {
  color: var(--client-text);
  font-weight: 800;
}

.kc-status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
  background: radial-gradient(circle at 30% 30%, #fff, #f59e0b);
  box-shadow: 0 0 0 2px rgba(245, 158, 11, 0.35);
}

.kc-status--draft .kc-status-dot {
  background: radial-gradient(circle at 30% 30%, #fff, #f59e0b);
  box-shadow: 0 0 0 2px rgba(245, 158, 11, 0.35);
}

.kc-status--pending .kc-status-dot {
  background: radial-gradient(circle at 30% 30%, #fff, var(--client-primary));
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.35);
}

.kc-status--on .kc-status-dot {
  background: radial-gradient(circle at 30% 30%, #fff, #22c55e);
  box-shadow: 0 0 0 2px rgba(34, 197, 94, 0.35);
}

.kc-status--off .kc-status-dot {
  background: radial-gradient(circle at 30% 30%, #fff, #94a3b8);
  box-shadow: 0 0 0 2px rgba(148, 163, 184, 0.45);
}

.kc-err {
  margin: 0 0 12px;
  font-size: 0.8125rem;
  color: #dc2626;
  line-height: 1.45;
}

.kc-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.kc-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  padding: 12px 16px;
  border-radius: 10px;
  font-size: 0.875rem;
  font-weight: 700;
  text-decoration: none;
  box-sizing: border-box;
  cursor: pointer;
  transition:
    background 0.2s,
    color 0.2s,
    box-shadow 0.2s;
}

.kc-btn--primary {
  border: none;
  background: var(--client-primary);
  color: #fff;
  box-shadow: 0 4px 14px rgba(59, 130, 246, 0.35);
}

.kc-btn--primary:hover:not(:disabled) {
  background: var(--client-primary-hover);
}

.kc-btn--primary:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.kc-btn--review {
  border: 1.5px solid var(--client-primary);
  background: var(--client-accent-soft);
  color: var(--client-primary);
  box-shadow: none;
}

.kc-btn--review:hover:not(:disabled) {
  background: rgba(59, 130, 246, 0.14);
  border-color: var(--client-primary-hover);
  color: var(--client-primary-hover);
}

.kc-btn--review:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.kc-btn--ghost {
  border: 1px solid rgba(59, 130, 246, 0.28);
  background: var(--client-surface);
  color: var(--client-muted);
}

.kc-btn--ghost:hover {
  border-color: rgba(59, 130, 246, 0.45);
  background: rgba(240, 247, 252, 0.65);
  color: var(--client-text);
}

.kc-autosave {
  margin: 14px 0 0;
  font-size: 0.75rem;
  color: var(--client-muted);
  text-align: center;
  line-height: 1.45;
}

.kc-autosave.is-active {
  color: var(--client-primary);
  font-weight: 600;
}

@media (max-width: 1024px) {
  .kc-layout {
    grid-template-columns: 1fr;
  }

  .kc-sidebar {
    position: static;
    order: -1;
  }
}

@media (max-width: 520px) {
  .kc-main .kc-card {
    padding: 18px 16px 22px;
  }

  .kc-sidebar-card {
    padding: 16px 14px;
  }
}
</style>
