<template>
  <div class="page">
    <h2>消防新闻</h2>
    <ul class="list">
      <li v-for="row in list" :key="row.id">
        <router-link :to="`/news/${row.id}`">{{ row.title }}</router-link>
        <span class="meta">{{ row.region }} {{ row.publishTime }}</span>
      </li>
    </ul>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getNewsList } from '@/api/news'

const list = ref([])

onMounted(async () => {
  try {
    const data = await getNewsList({ pageNum: 1, pageSize: 20 })
    list.value = data.records ?? data.list ?? []
  } catch (e) {
    console.error(e)
  }
})
</script>

<style scoped>
.page { background: #fff; padding: 16px; border-radius: 8px; }
.list { list-style: none; }
.list li { padding: 12px 0; border-bottom: 1px solid #eee; }
.list a { text-decoration: none; color: #1890ff; }
.list .meta { margin-left: 12px; font-size: 12px; color: #999; }
</style>
