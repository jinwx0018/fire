<template>
  <div class="page">
    <el-card>
      <template #header>
        <span>审计日志</span>
      </template>
      <div class="filters">
        <el-input v-model="query.action" placeholder="行为编码 action" clearable style="width: 200px" @keyup.enter="load" />
        <el-input v-model="query.operatorId" placeholder="操作人用户ID" clearable style="width: 160px" @keyup.enter="load" />
        <el-button type="primary" @click="load">查询</el-button>
      </div>
      <el-table :data="list" stripe border style="width: 100%">
        <el-table-column
          type="index"
          label="序号"
          width="80"
          :index="(i) => (pageNum - 1) * pageSize + i + 1"
        />
        <el-table-column prop="operatorId" label="操作人ID" width="100" />
        <el-table-column prop="action" label="行为" min-width="160" />
        <el-table-column prop="targetType" label="目标类型" width="120" />
        <el-table-column prop="targetId" label="目标ID" width="100" />
        <el-table-column prop="detail" label="详情" min-width="200" show-overflow-tooltip />
        <el-table-column label="时间" width="156">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap">
        <PaginationBar v-model="pageNum" :total="total" :page-size="pageSize" @current-change="load" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getAuditLogPage } from '@/api/auditLog'
import PaginationBar from '@/components/PaginationBar.vue'
import { formatDateTime } from '@/utils/formatDateTime'

const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 20
const query = reactive({ action: '', operatorId: '' })

async function load() {
  try {
    const data = await getAuditLogPage({
      pageNum: pageNum.value,
      pageSize,
      action: query.action || undefined,
      operatorId: query.operatorId ? Number(query.operatorId) : undefined,
    })
    list.value = data.records ?? data.list ?? []
    total.value = data.total ?? 0
  } catch (e) {
    ElMessage.error(e.message || '加载失败')
  }
}

onMounted(load)
</script>

<style scoped>
.filters {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.pagination-wrap {
  margin-top: 16px;
}
</style>
