<template>
  <KnowledgeModuleShell :crumbs="crumbs" narrow>
    <div class="fp-page">
      <header class="fp-hero">
        <div class="fp-hero-main">
          <h1 class="fp-title">发布新帖子</h1>
          <p class="fp-desc">分享你的消防见解，与社区成员共同探讨安全知识</p>
        </div>
        <div v-if="autoSaving" class="fp-saving-badge">
          <span class="fp-saving-spin" aria-hidden="true" />
          <span>正在保存草稿…</span>
        </div>
      </header>

      <div class="fp-alert" role="note">
        <div class="fp-alert-icon" aria-hidden="true">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10" />
            <line x1="12" y1="16" x2="12" y2="12" />
            <line x1="12" y1="8" x2="12.01" y2="8" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
        </div>
        <p class="fp-alert-text">
          提交后需管理员审核通过后方可在论坛列表展示。若之后编辑修改内容，将重新进入待审核，通过前仅自己可见。
        </p>
      </div>

      <form class="fp-form" @submit.prevent="submit">
        <div class="fp-field">
          <label class="fp-label fp-label--req">标题</label>
          <div class="fp-input-wrap">
            <input
              v-model.trim="form.title"
              type="text"
              maxlength="200"
              class="fp-input"
              placeholder="起一个吸引人的标题吧…"
              required
            />
            <span class="fp-count" :class="{ 'is-near': form.title.length > 180 }">
              {{ form.title.length }}/200
            </span>
          </div>
        </div>

        <div class="fp-field fp-field--rich">
          <label class="fp-label fp-label--req">正文</label>
          <div class="fp-rich-shell">
            <RichTextEditor :key="richEditorKey" ref="richRef" v-model="form.content" />
          </div>
        </div>

        <p v-if="err" class="fp-err">{{ err }}</p>

        <footer class="fp-footer">
          <p class="fp-autosave" :class="{ 'is-active': autoSaving }">
            {{ autoSaving ? '草稿已自动保存到本地' : '编辑中会自动保存草稿到本地' }}
          </p>
          <div class="fp-footer-actions">
            <button type="button" class="fp-btn fp-btn--ghost" @click="onCancel">返回论坛</button>
            <button type="submit" class="fp-btn fp-btn--primary" :disabled="loading">
              <span v-if="loading" class="fp-btn-spin" aria-hidden="true" />
              {{ loading ? '发布中…' : '发布帖子' }}
            </button>
          </div>
        </footer>
      </form>
    </div>
  </KnowledgeModuleShell>
</template>

<script setup>
import { ref, reactive, onMounted, watch, computed } from 'vue'
import { useRouter } from 'vue-router'
import { publishPost } from '@/api/forum'
import RichTextEditor from '@/components/RichTextEditor.vue'
import KnowledgeModuleShell from '@/components/KnowledgeModuleShell.vue'
import { isEmptyHtml } from '@/utils/htmlContent'

const router = useRouter()
const richRef = ref(null)
/** 恢复草稿后强制重挂富文本，避免先空挂载再 setHtml 与 editor-for-vue 内部 curValue 不同步导致 Slate 异常 */
const richEditorKey = ref(0)
const form = reactive({
  title: '',
  content: '',
})
const loading = ref(false)
const err = ref('')
const autoSaving = ref(false)

const crumbs = computed(() => [
  { label: '首页', to: '/home' },
  { label: '论坛', to: '/forum' },
  { label: '发布帖子', to: '' },
])

const DRAFT_KEY = 'forum_publish_draft'

function errMsg(e) {
  if (typeof e === 'string') return e
  return e?.message || '操作失败'
}

function saveDraftCache() {
  autoSaving.value = true
  const payload = { title: form.title, content: form.content, ts: Date.now() }
  try {
    localStorage.setItem(DRAFT_KEY, JSON.stringify(payload))
  } catch (_) {}
  setTimeout(() => {
    autoSaving.value = false
  }, 300)
}

function restoreDraftCache() {
  try {
    const raw = localStorage.getItem(DRAFT_KEY)
    if (!raw) return
    const draft = JSON.parse(raw)
    if (!draft) return
    if (!confirm('检测到未发布草稿，是否恢复？')) return
    form.title = draft.title ?? ''
    form.content = draft.content ?? ''
    richEditorKey.value += 1
  } catch (_) {}
}

function clearDraftCache() {
  try {
    localStorage.removeItem(DRAFT_KEY)
  } catch (_) {}
}

function onCancel() {
  if (confirm('确定要放弃编辑并返回论坛吗？本地草稿仍会保留，下次进入可恢复。')) {
    router.push('/forum')
  }
}

async function submit() {
  err.value = ''
  const html = richRef.value?.getHtml?.() ?? form.content
  if (!form.title || isEmptyHtml(html)) {
    err.value = '请填写标题与正文'
    return
  }
  loading.value = true
  try {
    const data = await publishPost({ title: form.title, content: html })
    clearDraftCache()
    const id = data?.id
    if (id != null) {
      router.replace(`/forum/${id}`)
    } else {
      router.push('/forum')
    }
  } catch (e) {
    err.value = errMsg(e) || '发布失败'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  restoreDraftCache()
})

watch(
  form,
  () => {
    saveDraftCache()
  },
  { deep: true },
)
</script>

<style scoped>
.fp-page {
  min-width: 0;
}

/* —— 头部 —— */
.fp-hero {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 14px;
  margin: -4px 0 20px;
  padding-bottom: 18px;
  border-bottom: 1px solid rgba(59, 130, 246, 0.1);
}

.fp-title {
  margin: 0 0 6px;
  font-size: 1.65rem;
  font-weight: 800;
  color: var(--client-text);
  letter-spacing: -0.03em;
  line-height: 1.2;
}

.fp-desc {
  margin: 0;
  font-size: 0.875rem;
  color: var(--client-muted);
  line-height: 1.55;
  max-width: 520px;
}

.fp-saving-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 14px;
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--client-primary);
  background: var(--client-accent-soft);
  border: 1px solid rgba(59, 130, 246, 0.2);
  border-radius: 999px;
}

.fp-saving-spin {
  width: 12px;
  height: 12px;
  border: 2px solid rgba(59, 130, 246, 0.25);
  border-top-color: var(--client-primary);
  border-radius: 50%;
  animation: fp-spin 0.8s linear infinite;
}

@keyframes fp-spin {
  to {
    transform: rotate(360deg);
  }
}

/* —— 提示条 —— */
.fp-alert {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  padding: 14px 16px;
  margin-bottom: 20px;
  border-radius: var(--client-radius);
  border: 1px solid rgba(59, 130, 246, 0.18);
  background: rgba(224, 242, 254, 0.55);
}

.fp-alert-icon {
  flex-shrink: 0;
  margin-top: 2px;
  color: var(--client-primary);
}

.fp-alert-text {
  margin: 0;
  font-size: 0.8125rem;
  line-height: 1.6;
  color: var(--client-text);
}

/* —— 表单（外层已由 KnowledgeModuleShell 提供白底卡片） —— */
.fp-form {
  margin: 0;
  padding: 0;
}

.fp-field {
  margin-bottom: 22px;
}

.fp-field--rich {
  max-width: 100%;
}

.fp-label {
  display: block;
  margin-bottom: 8px;
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--client-text);
}

.fp-label--req::after {
  content: '*';
  color: #dc2626;
  margin-left: 4px;
}

.fp-input-wrap {
  position: relative;
}

.fp-input {
  width: 100%;
  box-sizing: border-box;
  padding: 12px 88px 12px 14px;
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

.fp-input:focus {
  border-color: var(--client-primary);
  background: var(--client-surface);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.12);
}

.fp-input::placeholder {
  color: #94a3b8;
}

.fp-count {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 0.75rem;
  color: var(--client-muted);
  pointer-events: none;
}

.fp-count.is-near {
  color: #d97706;
  font-weight: 700;
}

.fp-rich-shell {
  border-radius: 10px;
  border: 1px solid rgba(59, 130, 246, 0.18);
  /* 勿用 overflow:hidden，易与 wangEditor 内部层级叠放冲突导致正文被裁切或看不见 */
  overflow: visible;
  background: var(--client-surface);
}

.fp-rich-shell :deep(.rich-wrap) {
  border: none;
}

.fp-rich-shell :deep(.rich-toolbar) {
  border-bottom: 1px solid rgba(59, 130, 246, 0.1);
}

.fp-rich-shell :deep(.rich-editor) {
  min-height: 360px;
}

.fp-err {
  margin: 0 0 12px;
  font-size: 0.8125rem;
  color: #dc2626;
  line-height: 1.45;
}

/* —— 底栏 —— */
.fp-footer {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 8px;
  padding-top: 20px;
  border-top: 1px solid rgba(59, 130, 246, 0.1);
}

.fp-autosave {
  margin: 0;
  font-size: 0.75rem;
  color: var(--client-muted);
}

.fp-autosave.is-active {
  color: var(--client-primary);
  font-weight: 600;
}

.fp-footer-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.fp-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px 22px;
  border-radius: 10px;
  font-size: 0.875rem;
  font-weight: 700;
  cursor: pointer;
  border: none;
  transition:
    background 0.2s,
    color 0.2s,
    box-shadow 0.2s,
    border-color 0.2s;
}

.fp-btn--primary {
  background: var(--client-primary);
  color: #fff;
  box-shadow: 0 4px 14px rgba(59, 130, 246, 0.35);
}

.fp-btn--primary:hover:not(:disabled) {
  background: var(--client-primary-hover);
}

.fp-btn--primary:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.fp-btn--ghost {
  background: var(--client-surface);
  color: var(--client-muted);
  border: 1px solid rgba(59, 130, 246, 0.22);
  font-weight: 600;
}

.fp-btn--ghost:hover {
  background: rgba(240, 247, 252, 0.85);
  border-color: rgba(59, 130, 246, 0.35);
  color: var(--client-text);
}

.fp-btn-spin {
  width: 14px;
  height: 14px;
  border: 2px solid rgba(255, 255, 255, 0.35);
  border-top-color: #fff;
  border-radius: 50%;
  animation: fp-spin 0.75s linear infinite;
}

@media (max-width: 640px) {
  .fp-hero {
    flex-direction: column;
    align-items: flex-start;
  }

  .fp-footer {
    flex-direction: column;
    align-items: stretch;
  }

  .fp-footer-actions {
    width: 100%;
  }

  .fp-btn {
    flex: 1;
    min-width: 0;
  }
}
</style>
