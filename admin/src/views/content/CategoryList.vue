<template>
  <div class="page">
    <h2>分类管理</h2>
    <div class="toolbar">
      <input v-model="newName" placeholder="分类名称" />
      <input v-model.number="newSort" type="number" placeholder="排序" style="width:80px" />
      <button @click="add">新增分类</button>
    </div>
    <table class="table">
      <thead>
        <tr><th>ID</th><th>名称</th><th>排序</th><th>操作</th></tr>
      </thead>
      <tbody>
        <tr v-for="row in list" :key="row.id">
          <td>{{ row.id }}</td>
          <td>{{ row.name }}</td>
          <td>{{ row.sort }}</td>
          <td>
            <button type="button" class="link danger" @click="del(row)">删除</button>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getCategoryList, addCategory, deleteCategory } from '@/api/content'

const list = ref([])
const newName = ref('')
const newSort = ref(0)

async function load() {
  try {
    const data = await getCategoryList()
    list.value = Array.isArray(data) ? data : (data.list ?? [])
  } catch (e) {
    console.error(e)
  }
}

async function add() {
  if (!newName.value.trim()) return
  try {
    await addCategory({ name: newName.value.trim(), sort: newSort.value || 0 })
    newName.value = ''
    newSort.value = 0
    load()
  } catch (e) {
    alert(e.message || '新增失败')
  }
}

async function del(row) {
  if (!confirm(`确定删除分类 ${row.name}？`)) return
  try {
    await deleteCategory(row.id)
    load()
  } catch (e) {
    alert(e.message || '删除失败')
  }
}

onMounted(load)
</script>

<style scoped>
.page { background: #fff; padding: 16px; border-radius: 8px; }
.toolbar { margin-bottom: 16px; display: flex; gap: 8px; }
.table { width: 100%; border-collapse: collapse; }
.table th, .table td { padding: 10px; border: 1px solid #eee; }
.table .link.danger { background: none; border: none; color: #c00; cursor: pointer; }
</style>
