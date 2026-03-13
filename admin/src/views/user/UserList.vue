<template>
  <div class="page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>用户管理</span>
          <el-button type="primary" @click="$router.push('/user/add')">新增用户</el-button>
        </div>
      </template>
      <div class="filters">
        <el-input v-model="query.username" placeholder="用户名" clearable style="width: 160px" @keyup.enter="load" />
        <el-select v-model="query.role" placeholder="全部角色" clearable style="width: 120px">
          <el-option label="全部角色" value="" />
          <el-option label="USER" value="USER" />
          <el-option label="AUTHOR" value="AUTHOR" />
          <el-option label="ADMIN" value="ADMIN" />
        </el-select>
        <el-button type="primary" @click="load">查询</el-button>
      </div>
      <el-table :data="list" stripe border style="width: 100%">
        <el-table-column prop="userId" label="ID" width="80" :formatter="(row) => row.userId ?? row.id" />
        <el-table-column prop="username" label="用户名" min-width="100" />
        <el-table-column prop="phone" label="手机号" min-width="120" />
        <el-table-column prop="role" label="角色" width="100" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="$router.push(`/user/edit/${row.userId ?? row.id}`)">编辑</el-button>
            <el-button type="danger" link @click="del(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap">
        <el-pagination
          :current-page="pageNum"
          :page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="(p) => { pageNum = p; load(); }"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserList, deleteUser } from '@/api/user'

const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 10
const query = reactive({ username: '', role: '' })

async function load() {
  try {
    const data = await getUserList({
      pageNum: pageNum.value,
      pageSize,
      username: query.username || undefined,
      role: query.role || undefined,
    })
    list.value = data.records ?? data.list ?? []
    total.value = data.total ?? 0
  } catch (e) {
    ElMessage.error(e.message || '加载失败')
  }
}

async function del(row) {
  try {
    await ElMessageBox.confirm(`确定删除用户 ${row.username}？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await deleteUser(row.userId ?? row.id)
    ElMessage.success('删除成功')
    load()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '删除失败')
  }
}

onMounted(load)
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.filters {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}
.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
