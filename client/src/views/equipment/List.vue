<template>
  <div class="page">
    <h2>消防器材</h2>
    <div class="filters">
      <input v-model="query.keyword" placeholder="名称" @keyup.enter="load" />
      <button @click="load">查询</button>
    </div>
    <ul class="list">
      <li v-for="row in list" :key="row.id">
        <router-link :to="`/equipment/${row.id}`">{{ row.name }}</router-link>
        <span class="meta">{{ row.typeName }}</span>
      </li>
    </ul>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getEquipmentList } from '@/api/equipment'

const list = ref([])
const query = reactive({ keyword: '' })

async function load() {
  try {
    const data = await getEquipmentList({
      pageNum: 1,
      pageSize: 20,
      keyword: query.keyword || undefined,
    })
    list.value = data.records ?? data.list ?? []
  } catch (e) {
    console.error(e)
  }
}

onMounted(load)
</script>

<style scoped>
.page { background: #fff; padding: 16px; border-radius: 8px; }
.filters { margin-bottom: 16px; display: flex; gap: 8px; }
.list { list-style: none; }
.list li { padding: 12px 0; border-bottom: 1px solid #eee; }
.list a { text-decoration: none; color: #1890ff; }
.list .meta { margin-left: 12px; font-size: 12px; color: #999; }
</style>
