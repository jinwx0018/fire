<template>
  <div class="user-manage">
    <el-tabs v-model="tab" class="um-tabs" @tab-change="onTabChange">
      <el-tab-pane label="用户列表" name="list" />
      <el-tab-pane label="作者申请" name="author" />
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
  tab.value = route.path.includes('author-applications') ? 'author' : 'list'
}

watch(() => route.path, syncTab, { immediate: true })

function onTabChange(name) {
  if (name === 'author') {
    router.push('/user/author-applications')
  } else {
    router.push('/user/list')
  }
}
</script>

<style scoped>
.user-manage {
  max-width: 1200px;
}
.um-tabs :deep(.el-tabs__header) {
  margin-bottom: 16px;
}
</style>
