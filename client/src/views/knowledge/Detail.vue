<template>
  <div class="page" v-if="detail">
    <h2>{{ detail.title }}</h2>
    <div class="meta">浏览量 {{ detail.viewCount ?? 0 }}</div>
    <div class="body" v-html="detail.content"></div>
  </div>
  <div v-else class="loading">加载中...</div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { getContentDetail } from '@/api/content'

const route = useRoute()
const id = computed(() => route.params.id)
const detail = ref(null)

onMounted(async () => {
  try {
    detail.value = await getContentDetail(id.value)
  } catch (e) {
    console.error(e)
  }
})
</script>

<style scoped>
.page { background: #fff; padding: 16px; border-radius: 8px; }
.page .meta { font-size: 13px; color: #999; margin-bottom: 12px; }
.page .body { line-height: 1.6; }
.loading { padding: 24px; text-align: center; }
</style>
