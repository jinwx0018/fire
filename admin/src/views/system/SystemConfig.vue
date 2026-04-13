<template>
  <div class="system-config">
    <el-tabs v-model="tab" class="sc-tabs" @tab-change="onTabChange">
      <el-tab-pane label="回收站" name="recycle" />
      <el-tab-pane label="审计日志" name="audit" />
      <el-tab-pane label="系统监控" name="monitor" />
    </el-tabs>
    <router-view />
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const tab = ref('recycle')

function syncTab() {
  const n = route.name
  if (n === 'AuditLogList') tab.value = 'audit'
  else if (n === 'Monitor') tab.value = 'monitor'
  else tab.value = 'recycle'
}

watch(() => route.name, syncTab, { immediate: true })

function onTabChange(name) {
  if (name === 'audit') {
    router.push({ name: 'AuditLogList' })
  } else if (name === 'monitor') {
    router.push({ name: 'Monitor' })
  } else {
    router.push({ name: 'RecycleList' })
  }
}
</script>

<style scoped>
.system-config {
  max-width: var(--client-content-max, 1680px);
}
.sc-tabs :deep(.el-tabs__header) {
  margin-bottom: 16px;
}
</style>
