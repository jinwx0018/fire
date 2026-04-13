<template>
  <div class="rich-wrap">
    <Toolbar class="rich-toolbar" :editor="editorRef" :default-config="toolbarConfig" mode="default" />
    <!-- 仅 v-model：组件内部已 watch modelValue 并 setHtml；勿再外层 setHtml，否则与 Slate 冲突易「有字不显示」 -->
    <Editor
      class="rich-editor"
      v-model="innerHtml"
      :default-config="editorConfig"
      mode="default"
      @on-created="onCreated"
    />
  </div>
</template>

<script setup>
import '@wangeditor/editor/dist/css/style.css'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import { ref, shallowRef, watch, onBeforeUnmount, nextTick } from 'vue'
import { uploadEditorImage } from '@/api/content'

const props = defineProps({
  modelValue: { type: String, default: '' },
})
const emit = defineEmits(['update:modelValue'])

const editorRef = shallowRef()
const innerHtml = ref(props.modelValue ?? '')
/** 正由父组件 props 回灌 innerHtml，避免与 Editor 回写形成 emit 环路 */
let syncingFromProp = false

const toolbarConfig = {}

const editorConfig = {
  placeholder: '请输入正文，可插入图片与排版',
  MENU_CONF: {
    uploadImage: {
      async customUpload(file, insertFn) {
        const data = await uploadEditorImage(file)
        const url = data?.url
        if (url) insertFn(url, '图片', url)
      },
    },
  },
}

function onCreated(editor) {
  editorRef.value = editor
}

/** 父级变更（如恢复草稿、编辑页回填）：只改 innerHtml，由 @wangeditor/editor-for-vue 内部 watch 执行 setHtml */
watch(
  () => props.modelValue,
  (v) => {
    const next = v ?? ''
    if (next === innerHtml.value) return
    syncingFromProp = true
    innerHtml.value = next
    nextTick(() => {
      syncingFromProp = false
    })
  }
)

/** Editor v-model 更新 innerHtml 后同步给父组件（不在 @on-change 里再 emit，避免双通道） */
watch(innerHtml, (html) => {
  if (syncingFromProp) return
  const p = props.modelValue ?? ''
  if (html === p) return
  emit('update:modelValue', html)
})

onBeforeUnmount(() => {
  const ed = editorRef.value
  if (ed) ed.destroy()
})

defineExpose({
  getHtml: () => {
    const ed = editorRef.value
    if (ed) return ed.getHtml()
    return innerHtml.value ?? ''
  },
})
</script>

<style scoped>
.rich-wrap {
  --w-e-textarea-color: #333333;
  --w-e-textarea-bg-color: #ffffff;
  --w-e-toolbar-color: #595959;
  --w-e-toolbar-bg-color: #ffffff;
  display: flex;
  flex-direction: column;
  border: 1px solid #ddd;
  border-radius: 4px;
  overflow: visible;
  background: #fff;
  color: #333;
  color-scheme: light;
  forced-color-adjust: none;
}
/* 须高于正文区，否则同 z-index 时编辑区在后会盖住表情/颜色等下拉面板，无法点击 */
.rich-toolbar {
  border-bottom: 1px solid #eee;
  position: relative;
  z-index: 20;
  overflow: visible;
}
.rich-editor {
  flex: 1 1 auto;
  display: flex;
  flex-direction: column;
  min-height: 320px;
  overflow: visible;
  border-radius: 0 0 4px 4px;
}
/* Editor 根节点内联 height:100%，父级高度为 auto 时部分浏览器解析为 0，导致正文区不可见 */
.rich-editor :deep(> div) {
  flex: 1 1 auto;
  min-height: 280px !important;
  height: auto !important;
}
.rich-editor :deep(.w-e-text-container) {
  min-height: 280px !important;
  color: #333 !important;
  background-color: #fff !important;
}
.rich-editor :deep(.w-e-text-container [data-slate-editor]) {
  color: #333 !important;
  -webkit-text-fill-color: #333 !important;
  caret-color: #333;
  position: relative;
  z-index: auto;
}
</style>

<style>
/* wangEditor 运行时插入 .w-e-*，需非 scoped */
.rich-wrap .w-e-text-container {
  color: #333 !important;
  background-color: #fff !important;
  --w-e-textarea-color: #333333;
  --w-e-textarea-bg-color: #ffffff;
}
.rich-wrap .w-e-text-container [data-slate-editor] {
  color: #333 !important;
  -webkit-text-fill-color: #333 !important;
  caret-color: #333;
  font-size: 16px;
  line-height: 1.6 !important;
  position: relative;
  z-index: auto;
}
.rich-wrap .w-e-text-container [data-slate-editor] p,
.rich-wrap .w-e-text-container [data-slate-editor] li {
  color: #333 !important;
  -webkit-text-fill-color: #333 !important;
  line-height: 1.6 !important;
}
.rich-wrap .w-e-text-container [data-slate-string='true'],
.rich-wrap .w-e-text-container [data-slate-string=''],
.rich-wrap .w-e-text-container span[data-slate-string] {
  color: #333 !important;
  -webkit-text-fill-color: #333 !important;
  opacity: 1 !important;
}
.rich-wrap .w-e-text-container [data-slate-leaf='true'],
.rich-wrap .w-e-text-container [data-slate-leaf] {
  color: #333 !important;
  -webkit-text-fill-color: #333 !important;
  opacity: 1 !important;
}
/* 勿对 [data-slate-editor] span 统一 inherit，会压掉上面 leaf 的 !important 与 Slate 结构冲突 */
.rich-wrap .w-e-text-container pre [data-slate-string='true'],
.rich-wrap .w-e-text-container pre span[data-slate-string],
.rich-wrap .w-e-text-container pre [data-slate-leaf] {
  color: inherit !important;
  -webkit-text-fill-color: inherit !important;
}
.rich-wrap .w-e-text-placeholder {
  z-index: 0 !important;
  pointer-events: none !important;
  color: #94a3b8 !important;
}

.rich-wrap .w-e-toolbar:not(.w-e-bar-bottom) .w-e-bar-item-menus-container,
.rich-wrap .w-e-toolbar:not(.w-e-bar-bottom) .w-e-drop-panel {
  top: 100% !important;
  margin-top: 0 !important;
}
.rich-wrap .w-e-toolbar:not(.w-e-bar-bottom) .w-e-bar-item .w-e-select-list {
  top: 100% !important;
  margin-top: 0 !important;
}
.rich-wrap .w-e-toolbar .w-e-bar {
  align-items: center;
}
.rich-wrap .w-e-toolbar .w-e-bar-item-menus-container,
.rich-wrap .w-e-toolbar .w-e-drop-panel,
.rich-wrap .w-e-toolbar .w-e-select-list,
.rich-wrap .w-e-toolbar .w-e-modal {
  z-index: 200 !important;
}
</style>
