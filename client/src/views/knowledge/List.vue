<template>
  <div class="page">
    <h2>消防知识</h2>
    <div class="filters">
      <select v-model="query.categoryId">
        <option value="">全部分类</option>
        <option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</option>
      </select>
      <input v-model="query.title" placeholder="标题搜索" @keyup.enter="load" />
      <button @click="load">查询</button>
    </div>
    <ul class="list">
      <li v-for="row in list" :key="row.id">
        <router-link :to="`/knowledge/${row.id}`">{{ row.title }}</router-link>
        <span class="meta">浏览 {{ row.viewCount ?? 0 }}</span>
      </li>
    </ul>
    <div class="pagination">
      <button :disabled="pageNum <= 1" @click="pageNum--; load()">上一页</button>
      <span>第 {{ pageNum }} 页</span>
      <button @click="pageNum++; load()">下一页</button>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getContentList, getCategoryList } from '@/api/content'

const list = ref([])
const categories = ref([])
const pageNum = ref(1)
const pageSize = 10
const query = reactive({ categoryId: '', title: '' })

async function load() {
  try {
    const data = await getContentList({
      pageNum: pageNum.value,
      pageSize,
      categoryId: query.categoryId || undefined,
      title: query.title || undefined,
    })
    list.value = data.records ?? data.list ?? []
  } catch (e) {
    console.error(e)
  }
}

async function loadCategories() {
  try {
    const data = await getCategoryList()
    categories.value = Array.isArray(data) ? data : (data.list ?? [])
  } catch (_) {}
}

onMounted(() => {
  loadCategories()
  load()
})
</script>

<style scoped>
.page { background: #fff; padding: 16px; border-radius: 8px; }
.filters { margin-bottom: 16px; display: flex; gap: 8px; }
.list { list-style: none; }
.list li { padding: 12px 0; border-bottom: 1px solid #eee; }
.list a { text-decoration: none; color: #1890ff; }
.list .meta { margin-left: 12px; font-size: 12px; color: #999; }
.pagination { margin-top: 16px; display: flex; gap: 12px; align-items: center; }
</style>
