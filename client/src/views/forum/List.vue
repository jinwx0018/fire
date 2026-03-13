<template>
  <div class="page">
    <h2>论坛</h2>
    <ul class="list">
      <li v-for="row in list" :key="row.id">
        <router-link :to="`/forum/${row.id}`">{{ row.title }}</router-link>
        <span class="meta">点赞 {{ row.likeCount ?? 0 }}</span>
      </li>
    </ul>
    <div class="pagination">
      <button :disabled="pageNum <= 1" @click="pageNum--; load()">上一页</button>
      <button @click="pageNum++; load()">下一页</button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getPostList } from '@/api/forum'

const list = ref([])
const pageNum = ref(1)

async function load() {
  try {
    const data = await getPostList({ pageNum: pageNum.value, pageSize: 10 })
    list.value = data.records ?? data.list ?? []
  } catch (e) {
    console.error(e)
  }
}

onMounted(load)
</script>

<style scoped>
.page { background: #fff; padding: 16px; border-radius: 8px; }
.list { list-style: none; }
.list li { padding: 12px 0; border-bottom: 1px solid #eee; }
.list a { text-decoration: none; color: #1890ff; }
.list .meta { margin-left: 12px; font-size: 12px; color: #999; }
.pagination { margin-top: 16px; display: flex; gap: 12px; }
</style>
