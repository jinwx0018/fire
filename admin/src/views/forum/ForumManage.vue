<template>
  <div class="forum-manage">
    <el-tabs v-model="tab" class="fm-tabs" @tab-change="onTabChange">
      <el-tab-pane label="帖子审核" name="posts" />
      <el-tab-pane label="论坛评论" name="comments" />
    </el-tabs>
    <router-view />
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const tab = ref('posts')

function syncTab() {
  tab.value = route.name === 'ForumCommentAudit' ? 'comments' : 'posts'
}

watch(() => route.name, syncTab, { immediate: true })

function onTabChange(name) {
  if (name === 'comments') {
    router.push({ name: 'ForumCommentAudit' })
  } else {
    router.push({ name: 'ForumAudit' })
  }
}
</script>

<style scoped>
.forum-manage {
  max-width: 1200px;
}
.fm-tabs :deep(.el-tabs__header) {
  margin-bottom: 16px;
}
</style>
