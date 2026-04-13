<template>
  <div class="knowledge-manage">
    <el-tabs v-model="tab" class="km-tabs" @tab-change="onTabChange">
      <el-tab-pane label="知识内容" name="content" />
      <el-tab-pane label="分类管理" name="category" />
      <el-tab-pane label="知识评论审核" name="comments" />
    </el-tabs>
    <router-view />
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const tab = ref('content')

function syncTab() {
  if (route.name === 'CategoryList') tab.value = 'category'
  else if (route.name === 'KnowledgeCommentAudit') tab.value = 'comments'
  else tab.value = 'content'
}

watch(() => route.name, syncTab, { immediate: true })

function onTabChange(name) {
  if (name === 'category') {
    router.push({ name: 'CategoryList' })
  } else if (name === 'comments') {
    router.push({ name: 'KnowledgeCommentAudit' })
  } else {
    router.push({ name: 'ContentList' })
  }
}
</script>

<style scoped>
.knowledge-manage {
  max-width: 1200px;
}
.km-tabs :deep(.el-tabs__header) {
  margin-bottom: 16px;
}
</style>
