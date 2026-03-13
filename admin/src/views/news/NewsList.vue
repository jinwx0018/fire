<template>
  <div class="page">
    <div class="toolbar">
      <h2>新闻管理</h2>
      <router-link to="/news/add" class="btn primary">新增新闻</router-link>
    </div>
    <table class="table">
      <thead>
        <tr>
          <th>ID</th>
          <th>标题</th>
          <th>地区</th>
          <th>紧急等级</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="row in list" :key="row.id">
          <td>{{ row.id }}</td>
          <td>{{ row.title }}</td>
          <td>{{ row.region ?? '-' }}</td>
          <td>{{ row.urgencyLevel ?? '-' }}</td>
          <td>
            <router-link :to="`/news/edit/${row.id}`">编辑</router-link>
            <button type="button" class="link danger" @click="del(row)">删除</button>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getNewsPage, deleteNews } from '@/api/news'

const list = ref([])

async function load() {
  try {
    const data = await getNewsPage({ pageNum: 1, pageSize: 20 })
    list.value = data.records ?? data.list ?? []
  } catch (e) {
    console.error(e)
  }
}

async function del(row) {
  if (!confirm(`确定删除新闻《${row.title}》？`)) return
  try {
    await deleteNews(row.id)
    load()
  } catch (e) {
    alert(e.message || '删除失败')
  }
}

onMounted(load)
</script>

<style scoped>
.page { background: #fff; padding: 16px; border-radius: 8px; }
.toolbar { display: flex; justify-content: space-between; margin-bottom: 16px; }
.table { width: 100%; border-collapse: collapse; }
.table th, .table td { padding: 10px; border: 1px solid #eee; }
.table .link { background: none; border: none; color: #c00; cursor: pointer; margin-left: 8px; }
.btn.primary { background: #1890ff; color: #fff; padding: 8px 16px; border-radius: 4px; text-decoration: none; }
</style>
