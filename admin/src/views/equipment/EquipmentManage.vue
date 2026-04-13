<template>
  <div class="equipment-manage">
    <el-tabs v-model="tab" class="em-tabs" @tab-change="onTabChange">
      <el-tab-pane label="器材管理" name="list" />
      <el-tab-pane label="器材类型" name="types" />
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
  tab.value = route.name === 'EquipmentTypeList' ? 'types' : 'list'
}

watch(() => route.name, syncTab, { immediate: true })

function onTabChange(name) {
  if (name === 'types') {
    router.push({ name: 'EquipmentTypeList' })
  } else {
    router.push({ name: 'EquipmentList' })
  }
}
</script>

<style scoped>
.equipment-manage {
  max-width: 1200px;
}
.em-tabs :deep(.el-tabs__header) {
  margin-bottom: 16px;
}
</style>
