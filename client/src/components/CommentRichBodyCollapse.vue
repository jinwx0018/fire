<template>
  <div class="comment-rich-body-collapse">
    <div
      ref="innerRef"
      class="comment-rich-html collapse-body"
      :class="{ 'is-clipped': isClipped }"
      v-html="html"
    />
    <button
      v-if="needsToggle"
      type="button"
      class="collapse-toggle"
      :aria-expanded="expanded"
      @click="expanded = !expanded"
    >
      {{ expanded ? '收起' : '展开全文' }}
    </button>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted, nextTick, computed } from 'vue'

const props = defineProps({
  /** 已由各页 commentDisplayHtml / sanitize 处理过的 HTML */
  html: { type: String, default: '' },
  /** 折叠时最大高度（px） */
  maxCollapsedPx: { type: Number, default: 132 },
})

const innerRef = ref(null)
const expanded = ref(false)
const needsToggle = ref(false)

const clipPx = computed(() => `${Math.max(80, props.maxCollapsedPx)}px`)

const isClipped = computed(() => !expanded.value && needsToggle.value)

function measure() {
  const el = innerRef.value
  if (!el) {
    needsToggle.value = false
    return
  }
  const limit = Math.max(80, props.maxCollapsedPx)
  needsToggle.value = el.scrollHeight > limit + 2
  if (!needsToggle.value) {
    expanded.value = false
  }
}

async function remeasure() {
  await nextTick()
  measure()
}

let resizeObserver

onMounted(() => {
  const el = innerRef.value
  if (el && typeof ResizeObserver !== 'undefined') {
    resizeObserver = new ResizeObserver(() => {
      measure()
    })
    resizeObserver.observe(el)
  }
  remeasure()
})

onUnmounted(() => {
  resizeObserver?.disconnect()
})

watch(
  () => props.html,
  async () => {
    expanded.value = false
    await remeasure()
  },
)

watch(
  () => props.maxCollapsedPx,
  () => {
    remeasure()
  },
)
</script>

<style scoped>
.comment-rich-body-collapse {
  min-width: 0;
  white-space: normal;
  word-break: break-word;
}
.comment-rich-html :deep(img) {
  max-width: 100%;
  height: auto;
  vertical-align: middle;
}
.collapse-body.is-clipped {
  max-height: v-bind(clipPx);
  overflow: hidden;
}
.collapse-toggle {
  margin-top: 6px;
  padding: 0;
  border: none;
  background: none;
  color: #1677ff;
  font-size: 13px;
  cursor: pointer;
}
.collapse-toggle:hover {
  text-decoration: underline;
}
</style>
