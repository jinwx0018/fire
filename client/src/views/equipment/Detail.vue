<template>
  <div class="page" v-if="detail">
    <h2>{{ detail.name }}</h2>
    <p v-if="detail.summary">{{ detail.summary }}</p>
    <section v-if="detail.usageSteps">
      <h3>使用步骤</h3>
      <pre>{{ detail.usageSteps }}</pre>
    </section>
    <section v-if="detail.checkPoints">
      <h3>检查要点</h3>
      <pre>{{ detail.checkPoints }}</pre>
    </section>
  </div>
  <div v-else class="loading">加载中...</div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { getEquipmentDetail } from '@/api/equipment'

const route = useRoute()
const id = computed(() => route.params.id)
const detail = ref(null)

onMounted(async () => {
  try {
    detail.value = await getEquipmentDetail(id.value)
  } catch (e) {
    console.error(e)
  }
})
</script>

<style scoped>
.page { background: #fff; padding: 16px; border-radius: 8px; }
.page section { margin-top: 16px; }
.page pre { white-space: pre-wrap; font-size: 14px; }
.loading { padding: 24px; text-align: center; }
</style>
