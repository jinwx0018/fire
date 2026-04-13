<template>
  <ul :class="ulClass">
    <li
      v-for="node in nodes"
      :key="node.id"
      :class="[itemClass, 'depth-' + Math.min(depth, 6)]"
      :id="'comment-' + node.id"
      :style="{ marginLeft: Math.min(depth, 6) * indentPx + 'px' }"
    >
      <slot name="row" :node="node" :depth="depth" />
      <template v-if="node.children?.length">
        <div class="comment-thread-children">
          <CommentThreadNodes
            v-if="visibleChildNodes(node).length"
            :nodes="visibleChildNodes(node)"
            :depth="depth + 1"
            :item-class="itemClass"
            :root-list-class="''"
            :indent-px="indentPx"
            :threshold="threshold"
          >
            <template #row="p">
              <slot name="row" v-bind="p" />
            </template>
          </CommentThreadNodes>
          <div v-if="showExpandBtn(node)" class="comment-reply-fold-bar">
            <button type="button" class="comment-reply-fold-btn" @click="setBranchExpanded(node.id, true)">
              展开其余 {{ remainderCount(node) }} 条回复
            </button>
          </div>
          <div v-else-if="showCollapseBtn(node)" class="comment-reply-fold-bar">
            <button type="button" class="comment-reply-fold-btn comment-reply-fold-btn--ghost" @click="setBranchExpanded(node.id, false)">
              收起回复
            </button>
          </div>
        </div>
      </template>
    </li>
  </ul>
</template>

<script setup>
import { computed, inject } from 'vue'
import { COMMENT_REPLY_BRANCH_KEY } from '@/utils/commentTree'

const props = defineProps({
  nodes: { type: Array, default: () => [] },
  depth: { type: Number, default: 0 },
  itemClass: { type: String, default: 'comment-thread-li' },
  /** 仅 depth===0 时作为根 ul 的 class，例如 comment-list */
  rootListClass: { type: String, default: '' },
  indentPx: { type: Number, default: 14 },
  threshold: { type: Number, default: 3 },
})

const branchExpandedMap = inject(COMMENT_REPLY_BRANCH_KEY, null)

const ulClass = computed(() =>
  props.depth === 0 && props.rootListClass ? props.rootListClass : 'comment-thread-nested-ul',
)

function isBranchExpanded(parentId) {
  if (!branchExpandedMap) return true
  return !!branchExpandedMap[parentId]
}

function visibleChildNodes(node) {
  const ch = node.children
  if (!ch?.length) return []
  if (ch.length <= props.threshold || isBranchExpanded(node.id)) return ch
  return ch.slice(0, props.threshold)
}

function showExpandBtn(node) {
  const ch = node.children
  return ch?.length > props.threshold && !isBranchExpanded(node.id)
}

function showCollapseBtn(node) {
  const ch = node.children
  return ch?.length > props.threshold && isBranchExpanded(node.id)
}

function remainderCount(node) {
  return node.children.length - props.threshold
}

function setBranchExpanded(parentId, expanded) {
  if (!branchExpandedMap) return
  if (expanded) branchExpandedMap[parentId] = true
  else delete branchExpandedMap[parentId]
}
</script>

<style scoped>
.comment-thread-nested-ul {
  list-style: none;
  padding: 0;
  margin: 8px 0 0;
}

.comment-thread-children {
  margin-top: 4px;
}

.comment-reply-fold-bar {
  margin-top: 6px;
  margin-bottom: 2px;
}

.comment-reply-fold-btn {
  padding: 0;
  border: none;
  background: none;
  color: var(--client-primary, #1677ff);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.comment-reply-fold-btn:hover {
  text-decoration: underline;
}

.comment-reply-fold-btn--ghost {
  color: var(--client-muted, #64748b);
  font-weight: 500;
}
</style>
