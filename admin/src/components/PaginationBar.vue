<template>
  <div v-if="visible" class="pagination-bar">
    <span class="bar-total">共 {{ displayTotal }} 条</span>
    <div class="bar-nav" role="navigation" aria-label="分页">
      <button
        type="button"
        class="nav-btn"
        :disabled="modelValue <= 1"
        aria-label="第一页"
        title="第一页"
        @click="go(1)"
      >
        «
      </button>
      <button
        type="button"
        class="nav-btn"
        :disabled="modelValue <= 1"
        aria-label="上一页"
        title="上一页"
        @click="go(modelValue - 1)"
      >
        ‹
      </button>
      <span class="nav-current">{{ modelValue }}</span>
      <button
        type="button"
        class="nav-btn"
        :disabled="modelValue >= totalPages"
        aria-label="下一页"
        title="下一页"
        @click="go(modelValue + 1)"
      >
        ›
      </button>
      <button
        type="button"
        class="nav-btn"
        :disabled="modelValue >= totalPages"
        aria-label="末页"
        title="末页"
        @click="go(totalPages)"
      >
        »
      </button>
    </div>
    <span class="bar-pages">共 {{ totalPages }} 页</span>
  </div>
</template>

<script setup>
import { computed, watch } from 'vue'

const props = defineProps({
  total: { type: Number, default: 0 },
  pageSize: { type: Number, default: 10 },
  modelValue: { type: Number, default: 1 },
  /** 无数据时仍占位展示（默认不渲染） */
  showWhenEmpty: { type: Boolean, default: false },
  /** 总页数（>0 时优先于 total/pageSize，用于接口直接返回 pages 的场景） */
  pages: { type: Number, default: 0 },
})

const emit = defineEmits(['update:modelValue', 'current-change'])

const displayTotal = computed(() => Math.max(0, Number(props.total) || 0))
const safePageSize = computed(() => Math.max(1, Number(props.pageSize) || 10))

const totalPages = computed(() => {
  const fromApi = Number(props.pages)
  if (fromApi > 0) return Math.max(1, Math.floor(fromApi))
  const t = displayTotal.value
  const ps = safePageSize.value
  if (t <= 0) return 1
  return Math.max(1, Math.ceil(t / ps))
})

const visible = computed(() => props.showWhenEmpty || displayTotal.value > 0)

function go(p) {
  const max = totalPages.value
  const next = Math.min(Math.max(1, p), max)
  if (next === props.modelValue) return
  emit('update:modelValue', next)
  emit('current-change', next)
}

watch(
  [() => displayTotal.value, () => totalPages.value, () => visible.value],
  () => {
    if (!visible.value && !props.showWhenEmpty) return
    if (props.modelValue > totalPages.value) {
      emit('update:modelValue', totalPages.value)
    }
    if (props.modelValue < 1) {
      emit('update:modelValue', 1)
    }
  },
  { flush: 'post' },
)
</script>

<style scoped>
.pagination-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  flex-wrap: nowrap;
  gap: 12px;
  box-sizing: border-box;
}
.bar-total,
.bar-pages {
  font-size: 14px;
  color: var(--el-text-color-regular, #606266);
  white-space: nowrap;
  flex-shrink: 0;
}
.bar-nav {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
}
.nav-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 32px;
  height: 32px;
  padding: 0 4px;
  box-sizing: border-box;
  border: none;
  border-radius: 4px;
  background: transparent;
  color: #909399;
  font-size: 22px;
  line-height: 1;
  cursor: pointer;
  transition: color 0.15s, opacity 0.15s;
}
.nav-btn:not(:disabled):hover {
  color: var(--el-color-primary, #3b82f6);
}
.nav-btn:disabled {
  color: #c0c4cc;
  cursor: not-allowed;
  opacity: 0.45;
}
.nav-current {
  min-width: 28px;
  text-align: center;
  font-size: 16px;
  font-weight: 700;
  color: var(--el-color-primary, #3b82f6);
}
.bar-pages {
  text-align: right;
}
</style>
