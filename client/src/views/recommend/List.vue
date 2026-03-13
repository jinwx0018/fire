<template>
  <div class="page">
    <h2>为你推荐</h2>
    <ul class="list">
      <li v-for="row in list" :key="row.id">
        <router-link :to="`/knowledge/${row.id}`">{{ row.title }}</router-link>
      </li>
    </ul>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getRecommendList } from '@/api/recommend'

const list = ref([])

onMounted(async () => {
  try {
    const data = await getRecommendList({ pageNum: 1, pageSize: 10 })
    list.value = data.list ?? data.records ?? []
  } catch (e) {
    console.error(e)
  }
})
</script>

<style scoped>
.page { background: #fff; padding: 16px; border-radius: 8px; }
.list { list-style: none; }
.list li { padding: 10px 0; border-bottom: 1px solid #eee; }
.list a { text-decoration: none; color: #1890ff; }
</style>
