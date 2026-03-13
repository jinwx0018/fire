<template>
  <div class="page">
    <div class="toolbar">
      <h2>器材管理</h2>
      <router-link to="/equipment/add" class="btn primary">新增器材</router-link>
    </div>
    <div class="filters">
      <input v-model="query.keyword" placeholder="名称" @keyup.enter="load" />
      <button @click="load">查询</button>
    </div>
    <table class="table">
      <thead>
        <tr>
          <th>ID</th>
          <th>名称</th>
          <th>类型</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="row in list" :key="row.id">
          <td>{{ row.id }}</td>
          <td>{{ row.name }}</td>
          <td>{{ row.typeName ?? '-' }}</td>
          <td>
            <router-link :to="`/equipment/edit/${row.id}`">编辑</router-link>
            <button type="button" class="link danger" @click="del(row)">删除</button>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getEquipmentPage, deleteEquipment } from '@/api/equipment'

const list = ref([])
const query = reactive({ keyword: '' })

async function load() {
  try {
    const data = await getEquipmentPage({
      pageNum: 1,
      pageSize: 20,
      keyword: query.name || undefined,
    })
    list.value = data.records ?? data.list ?? []
  } catch (e) {
    console.error(e)
  }
}

async function del(row) {
  if (!confirm(`确定删除器材 ${row.name}？`)) return
  try {
    await deleteEquipment(row.id)
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
.filters { margin-bottom: 16px; display: flex; gap: 8px; }
.table { width: 100%; border-collapse: collapse; }
.table th, .table td { padding: 10px; border: 1px solid #eee; }
.table .link { background: none; border: none; color: #c00; cursor: pointer; margin-left: 8px; }
.btn.primary { background: #1890ff; color: #fff; padding: 8px 16px; border-radius: 4px; text-decoration: none; }
</style>
