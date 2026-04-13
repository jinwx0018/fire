<template>
  <div class="page user-list-page">
    <el-card class="user-list-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span class="card-title">用户管理</span>
          <el-button type="primary" class="header-btn" @click="$router.push('/user/add')">新增用户</el-button>
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
        <el-table-column
          type="index"
          label="序号"
          width="80"
          :index="(i) => (pageNum - 1) * pageSize + i + 1"
        />
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
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="$router.push(`/user/edit/${row.userId ?? row.id}`)">编辑</el-button>
            <el-button :type="row.status === 1 ? 'warning' : 'success'" link @click="toggleFreeze(row)">
              {{ row.status === 1 ? '冻结' : '解冻' }}
            </el-button>
            <el-button type="info" link @click="kickout(row)">强制下线</el-button>
            <el-button type="danger" link @click="del(row)">删除</el-button>
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserList, deleteUser, freezeUser, unfreezeUser, forceLogoutUser } from '@/api/user'
import PaginationBar from '@/components/PaginationBar.vue'

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
    const msg = typeof e === 'string' ? e : e?.message || '加载失败'
    ElMessage.error(msg)
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

async function toggleFreeze(row) {
  const id = row.userId ?? row.id
  const action = row.status === 1 ? '冻结' : '解冻'
  try {
    await ElMessageBox.confirm(`确定${action}用户 ${row.username}？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    if (row.status === 1) {
      await freezeUser(id)
    } else {
      await unfreezeUser(id)
    }
    ElMessage.success(`${action}成功`)
    load()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || `${action}失败`)
  }
}

async function kickout(row) {
  const id = row.userId ?? row.id
  try {
    await ElMessageBox.confirm(`确定强制下线用户 ${row.username}？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await forceLogoutUser(id)
    ElMessage.success('强制下线成功')
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '强制下线失败')
  }
}

onMounted(load)
</script>

<style scoped>
.user-list-card {
  border-radius: var(--client-radius);
  border: 1px solid rgba(59, 130, 246, 0.12);
  box-shadow: 0 2px 20px rgba(59, 130, 246, 0.06);
}
.user-list-card :deep(.el-card__header) {
  padding: 16px 20px;
  border-bottom: 1px solid rgba(59, 130, 246, 0.1);
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.card-title {
  font-size: 1.0625rem;
  font-weight: 700;
  color: var(--client-text);
  letter-spacing: -0.02em;
}
.header-btn {
  border-radius: 10px;
}
.user-list-card :deep(.el-card__body) {
  padding: 20px 20px 24px;
}
.filters {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
  align-items: center;
}
.filters :deep(.el-button--primary) {
  border-radius: 10px;
}
.pagination-wrap {
  margin-top: 16px;
}
</style>
