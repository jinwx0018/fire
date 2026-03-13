<template>
  <div class="page">
    <div class="toolbar">
      <h2>知识内容</h2>
      <router-link to="/content/add" class="btn primary">新增内容</router-link>
    </div>
    <div class="filters">
      <input v-model="query.title" placeholder="标题" @keyup.enter="load" />
      <button @click="load">查询</button>
    </div>
    <table class="table">
      <thead>
        <tr>
          <th>ID</th>
          <th>标题</th>
          <th>分类</th>
          <th>浏览量</th>
          <th>状态</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="row in list" :key="row.id">
          <td>{{ row.id }}</td>
          <td>{{ row.title }}</td>
          <td>{{ row.categoryName ?? '-' }}</td>
          <td>{{ row.viewCount ?? 0 }}</td>
          <td>{{ row.status === 1 ? '发布' : '草稿' }}</td>
          <td>
            <router-link :to="`/content/edit/${row.id}`">编辑</router-link>
            <button type="button" class="link danger" @click="del(row)">删除</button>
          </td>
        </tr>
      </tbody>
    </table>
    <div class="pagination">
      <button :disabled="pageNum <= 1" @click="pageNum--; load()">上一页</button>
      <span>第 {{ pageNum }} 页</span>
      <button :disabled="(list.length < pageSize && pageNum > 1) || list.length === 0" @click="pageNum++; load()">下一页</button>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getContentPage, deleteContent } from '@/api/content'

const list = ref([])
const pageNum = ref(1)
const pageSize = 10
const query = reactive({ title: '' })

async function load() {
  try {
    const data = await getContentPage({
      pageNum: pageNum.value,
      pageSize,
      title: query.title || undefined,
    })
    list.value = data.records ?? data.list ?? []
  } catch (e) {
    console.error(e)
  }
}

async function del(row) {
  if (!confirm(`确定删除《${row.title}》？`)) return
  try {
    await deleteContent(row.id)
    load()
  } catch (e) {
    alert(e.message || '删除失败')
  }
}

onMounted(load)
</script>

<style scoped>
.page { background: #fff; padding: 16px; border-radius: 8px; }
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.filters { margin-bottom: 16px; display: flex; gap: 8px; }
.table { width: 100%; border-collapse: collapse; }
.table th, .table td { padding: 10px; border: 1px solid #eee; }
.table .link { background: none; border: none; color: #c00; cursor: pointer; }
.pagination { margin-top: 16px; display: flex; gap: 12px; }
.btn.primary { background: #1890ff; color: #fff; padding: 8px 16px; border-radius: 4px; text-decoration: none; }
</style>
