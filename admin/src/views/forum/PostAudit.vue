<template>
  <div class="page">
    <h2>帖子审核</h2>
    <div class="filters">
      <select v-model="query.status">
        <option value="">全部</option>
        <option value="0">待审核</option>
        <option value="1">已通过</option>
        <option value="-1">已驳回</option>
      </select>
      <button @click="load">查询</button>
    </div>
    <table class="table">
      <thead>
        <tr>
          <th>ID</th>
          <th>标题</th>
          <th>状态</th>
          <th>发布时间</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="row in list" :key="row.id">
          <td>{{ row.id }}</td>
          <td>{{ row.title }}</td>
          <td>{{ statusText(row.status) }}</td>
          <td>{{ row.createTime }}</td>
          <td>
            <template v-if="row.status === 0">
              <button type="button" class="link" @click="audit(row.id, 1)">通过</button>
              <button type="button" class="link danger" @click="audit(row.id, -1)">驳回</button>
            </template>
            <span v-else>-</span>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getPostPage, auditPost } from '@/api/forum'

const list = ref([])
const query = reactive({ status: '0' })

function statusText(s) {
  const m = { 0: '待审核', 1: '已通过', '-1': '已驳回' }
  return m[s] ?? '-'
}

async function load() {
  try {
    const data = await getPostPage({
      pageNum: 1,
      pageSize: 20,
      status: query.status || undefined,
    })
    list.value = data.records ?? data.list ?? []
  } catch (e) {
    console.error(e)
  }
}

async function audit(postId, status) {
  const body = status === -1 ? { status, rejectReason: '不符合规范' } : { status }
  try {
    await auditPost(postId, body)
    load()
  } catch (e) {
    alert(e.message || '操作失败')
  }
}

onMounted(load)
</script>

<style scoped>
.page { background: #fff; padding: 16px; border-radius: 8px; }
.filters { margin-bottom: 16px; display: flex; gap: 8px; }
.table { width: 100%; border-collapse: collapse; }
.table th, .table td { padding: 10px; border: 1px solid #eee; }
.table .link { background: none; border: none; color: #1890ff; cursor: pointer; margin-right: 8px; }
.table .link.danger { color: #c00; }
</style>
