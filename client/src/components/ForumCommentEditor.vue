<template>

  <div class="comment-editor">

    <div class="editor-toolbar">

      <div ref="emojiWrapRef" class="emoji-wrap">

        <button

          type="button"

          class="btn-emoji-toggle"

          :class="{ active: emojiOpen }"

          :aria-expanded="emojiOpen"

          aria-haspopup="true"

          aria-label="选择表情"

          title="表情"

          @click.stop="emojiOpen = !emojiOpen"

        >

          <span class="sr-only">选择表情</span>

          <svg class="ico-smile" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true">

            <circle cx="12" cy="12" r="10" />

            <path d="M8 14s1.5 2 4 2 4-2 4-2" stroke-linecap="round" />

            <circle cx="9" cy="9" r="1.2" fill="currentColor" stroke="none" />

            <circle cx="15" cy="9" r="1.2" fill="currentColor" stroke="none" />

          </svg>

        </button>

        <div

          v-show="emojiOpen"

          class="emoji-panel"

          role="dialog"

          aria-label="表情列表"

          @mousedown.prevent

        >

          <div class="emoji-grid">

            <button

              v-for="(ch, i) in EMOJI_LIST"

              :key="i"

              type="button"

              class="emoji-btn"

              :title="'插入 ' + ch"

              @mousedown.prevent

              @click="onPickEmoji(ch)"

            >

              {{ ch }}

            </button>

          </div>

        </div>

      </div>

    </div>

    <div class="editor-shell">

      <div

        ref="editorRef"

        class="ta ce comment-rich-html"

        contenteditable="true"

        role="textbox"

        aria-multiline="true"

        tabindex="0"

        :data-placeholder="placeholder"

        :class="{ 'is-empty': showPlaceholder }"

        @input="onEditorInput"

        @paste="onPaste"

      />

    </div>

    <div class="bottom-bar">

      <span class="hint">支持表情与图片（jpg/png/gif/webp）；插入图片自动换行且显示不超过 250×250；粘贴为纯文本。</span>

      <div class="bar-actions">

        <input

          ref="fileRef"

          type="file"

          class="file-input"

          accept="image/jpeg,image/png,image/gif,image/webp"

          @change="onPickFile"

        />

        <button type="button" class="btn-img" :disabled="uploading" @click="fileRef?.click()">

          {{ uploading ? '上传中…' : '插入图片' }}

        </button>

      </div>

    </div>

  </div>

</template>



<script setup>

import { ref, computed, watch, nextTick, onMounted, onUnmounted } from 'vue'

import { uploadEditorImage } from '@/api/content'

import { sanitizeHtml, plainToEditorHtml, isEmptyHtml, normalizeRichCommentHtml } from '@/utils/htmlContent'



const EMOJI_LIST = [

  '😀', '😃', '😄', '😁', '😆', '😅', '🤣', '😂', '🙂', '🙃', '😉', '😊', '😍', '🥰', '😘', '😋', '😛', '😜', '🤪', '😝', '🤔', '😏', '😒', '🙄', '😬', '😢', '😭', '😤', '😡', '🤬', '👍', '👎', '👏', '🙌', '🤝', '🙏', '💪', '🔥', '❤️', '💯',

]



const props = defineProps({

  modelValue: { type: String, default: '' },

  placeholder: { type: String, default: '写下你的看法…' },

})



const emit = defineEmits(['update:modelValue'])



const editorRef = ref(null)

const fileRef = ref(null)

const emojiWrapRef = ref(null)

const uploading = ref(false)

const emojiOpen = ref(false)



/** 与父组件 v-model 同步用的「上次发出值」，避免循环更新 */

let lastEmitted = ''



function toEditableHtml(s) {

  const raw = String(s ?? '')

  if (!raw.trim()) return ''

  const t = raw.trim()

  if (/<[a-z][\s\S]*>/i.test(t)) return sanitizeHtml(raw)

  return plainToEditorHtml(raw)

}



function readSanitizedFromEditor() {

  const el = editorRef.value

  if (!el) return ''

  let h = normalizeRichCommentHtml(el.innerHTML)

  h = sanitizeHtml(h)

  if (isEmptyHtml(h)) return ''

  return h

}



const showPlaceholder = computed(() => isEmptyHtml(props.modelValue))



function onEditorInput() {

  const el = editorRef.value

  if (!el) return

  let sanitized = normalizeRichCommentHtml(el.innerHTML)

  sanitized = sanitizeHtml(sanitized)

  if (isEmptyHtml(sanitized)) {

    sanitized = ''

    if (el.innerHTML !== '') el.innerHTML = ''

  }

  if (sanitized === lastEmitted) return

  lastEmitted = sanitized

  emit('update:modelValue', sanitized)

}



function syncFromProp(html) {

  const el = editorRef.value

  if (!el) return

  el.innerHTML = toEditableHtml(html)

}



watch(

  () => props.modelValue,

  (v) => {

    const s = v ?? ''

    if (s === lastEmitted) return

    lastEmitted = s

    nextTick(() => syncFromProp(s))

  },

)



function onDocClick(e) {

  const el = emojiWrapRef.value

  if (el && !el.contains(e.target)) {

    emojiOpen.value = false

  }

}



onMounted(() => {

  lastEmitted = props.modelValue ?? ''

  nextTick(() => syncFromProp(lastEmitted))

  document.addEventListener('click', onDocClick)

})

onUnmounted(() => {

  document.removeEventListener('click', onDocClick)

})



function getActiveRange(root) {

  const sel = window.getSelection()

  if (sel?.rangeCount && root.contains(sel.anchorNode)) {

    return sel.getRangeAt(0)

  }

  const range = document.createRange()

  range.selectNodeContents(root)

  range.collapse(false)

  sel?.removeAllRanges()

  sel?.addRange(range)

  return range

}



function insertAtCursor(nodeOrText) {

  const root = editorRef.value

  if (!root) return

  root.focus()

  const range = getActiveRange(root)

  range.deleteContents()

  if (typeof nodeOrText === 'string') {

    const tn = document.createTextNode(nodeOrText)

    range.insertNode(tn)

    range.setStartAfter(tn)

    range.collapse(true)

  } else {

    range.insertNode(nodeOrText)

    range.setStartAfter(nodeOrText)

    range.collapse(true)

  }

  const sel = window.getSelection()

  sel?.removeAllRanges()

  sel?.addRange(range)

  onEditorInput()

}



function onPickEmoji(ch) {

  insertAtCursor(ch)

}



function onPaste(e) {

  e.preventDefault()

  const text = e.clipboardData?.getData('text/plain') ?? ''

  insertAtCursor(text)

}



function applyCommentImageLayout(img) {

  img.alt = ''

  img.style.display = 'block'

  img.style.maxWidth = '250px'

  img.style.maxHeight = '250px'

  img.style.width = 'auto'

  img.style.height = 'auto'

  img.style.objectFit = 'contain'

  img.style.borderRadius = '6px'

  img.style.margin = '0'

}

/** 图片前换行、独占一行，避免与文字挤在同一行 */

function insertCommentImageAtCursor(img) {

  const root = editorRef.value

  if (!root) return

  root.focus()

  const range = getActiveRange(root)

  range.deleteContents()

  applyCommentImageLayout(img)

  const br = document.createElement('br')

  const wrap = document.createElement('span')

  wrap.className = 'comment-img-line'

  wrap.appendChild(img)

  const frag = document.createDocumentFragment()

  frag.appendChild(br)

  frag.appendChild(wrap)

  range.insertNode(frag)

  range.setStartAfter(wrap)

  range.collapse(true)

  const sel = window.getSelection()

  sel?.removeAllRanges()

  sel?.addRange(range)

  onEditorInput()

}

async function onPickFile(e) {

  const file = e.target.files?.[0]

  e.target.value = ''

  if (!file) return

  uploading.value = true

  try {

    const data = await uploadEditorImage(file)

    const url = data?.url

    if (url) {

      const img = document.createElement('img')

      img.src = url

      insertCommentImageAtCursor(img)

    }

  } catch (_) {

    alert('图片上传失败')

  } finally {

    uploading.value = false

  }

}



defineExpose({

  getHtml: () => readSanitizedFromEditor(),

})

</script>



<style scoped>

.comment-editor {

  width: 100%;

  max-width: 100%;

}

.editor-toolbar {

  margin-bottom: 6px;

  min-height: 32px;

}

.emoji-wrap {

  position: relative;

  display: inline-block;

}

.btn-emoji-toggle {

  display: inline-flex;

  align-items: center;

  justify-content: center;

  width: 36px;

  height: 36px;

  padding: 0;

  border: 1px solid #d9d9d9;

  border-radius: 8px;

  background: #fff;

  color: #595959;

  cursor: pointer;

  transition: border-color 0.15s, color 0.15s, background 0.15s;

}

.btn-emoji-toggle:hover {

  border-color: #1890ff;

  color: #1890ff;

  background: #f0f9ff;

}

.btn-emoji-toggle.active {

  border-color: #1890ff;

  color: #1890ff;

  background: #e6f7ff;

}

.ico-smile {

  width: 22px;

  height: 22px;

}

.sr-only {

  position: absolute;

  width: 1px;

  height: 1px;

  padding: 0;

  margin: -1px;

  overflow: hidden;

  clip: rect(0, 0, 0, 0);

  white-space: nowrap;

  border: 0;

}

.emoji-panel {

  position: absolute;

  top: calc(100% + 6px);

  left: 0;

  z-index: 20;

  min-width: 240px;

  max-width: min(320px, 92vw);

  padding: 10px;

  background: #fff;

  border: 1px solid #e8e8e8;

  border-radius: 8px;

  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);

}

.emoji-grid {

  display: flex;

  flex-wrap: wrap;

  gap: 4px;

  max-height: 200px;

  overflow-y: auto;

}

.emoji-btn {

  padding: 4px 6px;

  font-size: 20px;

  line-height: 1.2;

  border: none;

  background: transparent;

  border-radius: 4px;

  cursor: pointer;

  transition: background 0.15s;

}

.emoji-btn:hover {

  background: #e6f4ff;

}

.editor-shell {

  border: 1px solid #ddd;

  border-radius: 8px;

  overflow: hidden;

  background: #fff;

  transition: border-color 0.15s, box-shadow 0.15s;

}

.editor-shell:focus-within {

  border-color: #1890ff;

  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.12);

}

.ta.ce {

  display: block;

  position: relative;

  width: 100%;

  box-sizing: border-box;

  min-height: 100px;

  max-height: 320px;

  padding: 10px 12px;

  border: none;

  font-size: 14px;

  line-height: 1.55;

  color: #333;

  resize: vertical;

  overflow: auto;

  font-family: inherit;

  word-break: break-word;

  outline: none;

}

.ta.ce.is-empty::before {

  content: attr(data-placeholder);

  color: #bfbfbf;

  pointer-events: none;

}

/* 尺寸与换行由全局 commentRichHtml.css + 插入时的 comment-img-line 负责 */

.ta.ce :deep(img) {

  vertical-align: middle;

}

.ta.ce :deep(p) {

  margin: 0.35em 0;

}

.ta.ce :deep(p:first-child) {

  margin-top: 0;

}

.ta.ce :deep(p:last-child) {

  margin-bottom: 0;

}

.bottom-bar {

  display: flex;

  flex-wrap: wrap;

  align-items: center;

  justify-content: space-between;

  gap: 8px;

  margin-top: 8px;

}

.hint {

  font-size: 12px;

  color: #999;

  flex: 1;

  min-width: 160px;

}

.bar-actions {

  display: flex;

  align-items: center;

  gap: 8px;

}

.file-input {

  display: none;

}

.btn-img {

  padding: 6px 14px;

  font-size: 13px;

  color: #1890ff;

  background: #fff;

  border: 1px solid #1890ff;

  border-radius: 6px;

  cursor: pointer;

  white-space: nowrap;

}

.btn-img:hover:not(:disabled) {

  background: #e6f7ff;

}

.btn-img:disabled {

  opacity: 0.6;

  cursor: not-allowed;

}

</style>

