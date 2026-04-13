<template>
  <div class="rich-wrap">
    <Toolbar class="rich-toolbar" :editor="editorRef" :default-config="toolbarConfig" mode="default" />
    <Editor
      class="rich-editor"
      v-model="innerHtml"
      :default-config="editorConfig"
      mode="default"
      @on-created="onCreated"
      @on-change="onEditorChange"
    />
  </div>
</template>

<script setup>
import '@wangeditor/editor/dist/css/style.css'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import { ref, shallowRef, watch, onBeforeUnmount } from 'vue'
import { uploadEditorImage } from '@/api/content'

const props = defineProps({
  modelValue: { type: String, default: '' },
})
const emit = defineEmits(['update:modelValue'])

const editorRef = shallowRef()
const innerHtml = ref(props.modelValue ?? '')

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
  const initial = props.modelValue ?? ''
  if (initial) editor.setHtml(initial)
}

/** wangEditor 部分场景下 v-model 与父级 reactive 同步不及时，onChange 显式回写 */
function onEditorChange(editor) {
  const html = editor.getHtml()
  innerHtml.value = html
  emit('update:modelValue', html)
}

watch(
  () => props.modelValue,
  (v) => {
    const next = v ?? ''
    const ed = editorRef.value
    if (ed) {
      if (next !== ed.getHtml()) {
        ed.setHtml(next)
        innerHtml.value = ed.getHtml()
      }
    } else {
      innerHtml.value = next
    }
  }
)

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
/* 防止父级/Element Plus 等全局样式覆盖 wangEditor 变量，导致输入文字不可见 */
.rich-wrap {
  --w-e-textarea-color: #333333;
  --w-e-textarea-bg-color: #ffffff;
  --w-e-toolbar-color: #595959;
  --w-e-toolbar-bg-color: #ffffff;
  border: 1px solid #ddd;
  border-radius: 4px;
  /* 勿用 overflow:hidden，会裁切工具栏下拉/插入面板，且与 hover 空隙叠加导致菜单秒关 */
  overflow: visible;
  background: #fff;
  color: #333;
  color-scheme: light;
  forced-color-adjust: none;
}
.rich-toolbar {
  border-bottom: 1px solid #eee;
  position: relative;
  z-index: 20;
  overflow: visible;
}
.rich-editor {
  min-height: 320px;
  overflow: hidden;
  border-radius: 0 0 4px 4px;
}
.rich-editor :deep(.w-e-text-container) {
  min-height: 280px !important;
  color: #333 !important;
  background-color: #fff !important;
}
.rich-editor :deep(.w-e-text-container [data-slate-editor]) {
  color: #333 !important;
  caret-color: #333;
}
</style>

<style>
/* 非 scoped：wangEditor 运行时插入 .w-e-*，仅用 scoped :deep 在部分环境下不稳定，会导致编辑区看不见字 */
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
  position: relative;
  z-index: auto;
}
.rich-wrap .w-e-text-container [data-slate-editor] p,
.rich-wrap .w-e-text-container [data-slate-editor] li {
  color: #333 !important;
  -webkit-text-fill-color: #333 !important;
}
.rich-wrap .w-e-text-container [data-slate-string='true'] {
  color: #333 !important;
  -webkit-text-fill-color: #333 !important;
  opacity: 1 !important;
}
.rich-wrap .w-e-text-container pre [data-slate-string='true'] {
  color: inherit !important;
  -webkit-text-fill-color: inherit !important;
}
.rich-wrap .w-e-text-placeholder {
  z-index: 0;
}

/*
 * 默认 top:0 + margin-top:40px 会在按钮与下拉之间留出「空白带」，鼠标移入菜单时易失 hover；
 * 改为 top:100% + margin-top:0，菜单紧贴工具栏底边，无需 padding 撑高（避免图标错行、换行错乱）。
 */
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
