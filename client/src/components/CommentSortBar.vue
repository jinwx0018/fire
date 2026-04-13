<template>
  <div class="comment-sort-bar" role="group" aria-label="评论排序：最新或最热">
    <span class="comment-sort-label">排序</span>
    <div class="comment-sort-tabs">
      <button
        type="button"
        class="comment-sort-btn"
        :class="{ active: activeSort === 'time' }"
        title="按发布时间从新到旧"
        @click="pick('time')"
      >
        最新
      </button>
      <button
        type="button"
        class="comment-sort-btn"
        :class="{ active: activeSort === 'hot' }"
        title="按点赞数从高到低"
        @click="pick('hot')"
      >
        最热
      </button>
    </div>
  </div>
</template>

<script setup>
defineProps({
  /** time=最新优先；hot=热度（点赞）优先 */
  activeSort: {
    type: String,
    default: 'time',
    validator: (v) => v === 'time' || v === 'hot',
  },
})

const emit = defineEmits(['select'])

function pick(mode) {
  emit('select', mode)
}
</script>

<style scoped>
.comment-sort-bar {
  display: inline-flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}
.comment-sort-label {
  font-size: 13px;
  color: #8c8c8c;
  user-select: none;
}
.comment-sort-tabs {
  display: inline-flex;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  overflow: hidden;
  font-size: 13px;
}
.comment-sort-btn {
  min-height: 32px;
  padding: 6px 14px;
  border: none;
  background: #fff;
  color: #595959;
  cursor: pointer;
  line-height: 1.2;
}
.comment-sort-btn + .comment-sort-btn {
  border-left: 1px solid #d9d9d9;
}
.comment-sort-btn:hover {
  color: #1677ff;
}
.comment-sort-btn.active {
  background: #e6f4ff;
  color: #1677ff;
  font-weight: 600;
}
</style>
