<template>
  <div class="page">
    <div class="toolbar">
      <h2>回收站</h2>
      <div class="toolbar-actions">
        <button @click="load" class="btn secondary">刷新</button>
        <button @click="clearAll" class="btn danger" v-if="list.length > 0">
          清空回收站
        </button>
      </div>
    </div>

    <div class="tips">
      共 <strong>{{ total }}</strong> 条已删除内容
    </div>

    <table class="table">
      <thead>
        <tr>
          <th width="60">序号</th>
          <th width="88">模块</th>
          <th>标题</th>
          <th width="120">分类</th>
          <th width="80" class="col-center">浏览量</th>
          <th width="160" class="col-center">删除时间</th>
          <th width="160" class="col-center">操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(row, index) in list" :key="`${row.moduleType || 'KNOWLEDGE'}-${row.id}`">
          <td>{{ (pageNum - 1) * pageSize + index + 1 }}</td>
          <td><span class="module-tag" :class="'mod-' + (row.moduleType || 'KNOWLEDGE').toLowerCase()">{{ moduleLabel(row.moduleType) }}</span></td>
          <td class="title-cell">{{ row.title }}</td>
          <td>{{ row.categoryName ?? '-' }}</td>
          <td class="col-center">{{ row.viewCount ?? 0 }}</td>
          <td class="col-center">{{ formatTime(row.updateTime || row.createTime) }}</td>
          <td class="col-actions">
            <button class="btn-restore" @click="restore(row)">恢复</button>
            <button class="btn-delete" @click="permanentDelete(row)">彻底删除</button>
          </td>
        </tr>
        <tr v-if="list.length === 0">
          <td colspan="7" class="empty">回收站为空</td>
        </tr>
      </tbody>
    </table>

    <PaginationBar v-model="pageNum" class="pager-bar" :total="total" :page-size="pageSize" @current-change="load" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getRecyclePage, restoreContent, permanentDeleteRecycle, purgeRecycleAll } from '@/api/content'
import { formatDateTime } from '@/utils/formatDateTime'
import PaginationBar from '@/components/PaginationBar.vue'

const list = ref([])
const pageNum = ref(1)
const pageSize = 10
const total = ref(0)

async function load() {
  try {
    const data = await getRecyclePage({
      pageNum: pageNum.value,
      pageSize
    })
    list.value = data.records ?? data.list ?? []
    total.value = Number(data.total ?? 0)
  } catch (e) {
    console.error('加载回收站失败', e)
    alert('加载回收站失败')
  }
}

function moduleLabel(moduleType) {
  const m = (moduleType || 'KNOWLEDGE').toString().toUpperCase()
  if (m === 'NEWS') return '新闻'
  if (m === 'FORUM') return '帖子'
  return '知识'
}

function rowModule(row) {
  return (row.moduleType || 'KNOWLEDGE').toString().toUpperCase()
}

async function restore(row) {
  if (!confirm(`确定恢复内容《${row.title}》吗？`)) return
  try {
    await restoreContent(row.id, rowModule(row))
    alert('恢复成功')
    load()
  } catch (e) {
    alert(e.message || '恢复失败')
  }
}

async function permanentDelete(row) {
  if (!confirm(`确定彻底删除《${row.title}》？此操作不可恢复！`)) return
  try {
    await permanentDeleteRecycle(row.id, rowModule(row))
    alert('已彻底删除')
    load()
  } catch (e) {
    alert(e.message || '删除失败')
  }
}

function formatTime(time) {
  if (!time) return '-'
  return formatDateTime(time) || '-'
}

async function clearAll() {
  if (!confirm('确定清空整个回收站吗？此操作不可恢复！')) return
  try {
    await purgeRecycleAll()
    alert('回收站已清空')
    pageNum.value = 1
    load()
  } catch (e) {
    alert(e.message || '清空失败')
  }
}

onMounted(() => {
  load()
})
</script>

<style scoped>
.toolbar { 
  display: flex; 
  justify-content: space-between; 
  align-items: center; 
  margin-bottom: 16px; 
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(59, 130, 246, 0.1);
}
.toolbar-actions { display: flex; gap: 8px; }
.tips { color: var(--client-muted); margin-bottom: 16px; font-size: 14px; }
.table { 
  width: 100%; 
  border-collapse: collapse; 
  margin-bottom: 16px;
}
.table th, .table td { 
  padding: 12px 8px; 
  border: 1px solid rgba(59, 130, 246, 0.12); 
  text-align: left;
}
.table th { background: rgba(224, 242, 254, 0.42); font-weight: 600; color: var(--client-text); }
.col-center { text-align: center !important; }
.title-cell { max-width: 360px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.col-actions { white-space: nowrap; }
.btn-restore { 
  padding: 4px 12px; 
  background: #22c55e; 
  color: white; 
  border: none; 
  border-radius: 8px; 
  cursor: pointer;
  margin-right: 6px;
}
.btn-delete { 
  padding: 4px 12px; 
  background: #ef4444; 
  color: white; 
  border: none; 
  border-radius: 8px; 
  cursor: pointer;
}
.btn.secondary {
  padding: 6px 14px;
  background: var(--client-surface);
  border: 1px solid rgba(59, 130, 246, 0.22);
  border-radius: 10px;
  color: var(--client-text);
  cursor: pointer;
}
.pager-bar { margin-top: 20px; }
.empty {
  text-align: center;
  padding: 60px 20px;
  color: var(--client-muted);
  font-size: 15px;
}
.module-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 500;
}
.module-tag.mod-knowledge { background: var(--client-accent-soft); color: var(--client-primary); }
.module-tag.mod-news { background: rgba(16, 185, 129, 0.14); color: #059669; }
.module-tag.mod-forum { background: rgba(14, 165, 233, 0.14); color: #0284c7; }
</style>
