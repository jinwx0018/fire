<template>
  <div class="news-manage">
    <el-tabs v-model="tab" class="nm-tabs" @tab-change="onTabChange">
      <el-tab-pane label="新闻管理" name="list" />
      <el-tab-pane label="新闻分类" name="categories" />
      <el-tab-pane label="新闻评论审核" name="comments" />
    </el-tabs>
    <router-view />
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const tab = ref('list')

function syncTab() {
  const n = route.name
  if (n === 'NewsCategoryList') tab.value = 'categories'
  else if (n === 'NewsCommentAudit') tab.value = 'comments'
  else tab.value = 'list'
}

watch(() => route.name, syncTab, { immediate: true })

function onTabChange(name) {
  if (name === 'categories') {
    router.push({ name: 'NewsCategoryList' })
  } else if (name === 'comments') {
    router.push({ name: 'NewsCommentAudit' })
  } else {
    router.push({ name: 'NewsList' })
  }
}
</script>

<style scoped>
.news-manage {
  max-width: 1200px;
}
.nm-tabs :deep(.el-tabs__header) {
  margin-bottom: 16px;
}
</style>
