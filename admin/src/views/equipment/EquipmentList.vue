<template>
  <div class="page">
    <div class="toolbar">
      <h2>器材管理</h2>
      <div class="toolbar-actions">
        <el-upload
          :show-file-list="false"
          accept=".xlsx,.xls"
          :before-upload="onImportFile"
        >
          <el-button>Excel 导入</el-button>
        </el-upload>
        <router-link to="/equipment/types">
          <el-button>器材类型</el-button>
        </router-link>
        <router-link to="/equipment/add">
          <el-button type="primary">新增器材</el-button>
        </router-link>
      </div>
    </div>

    <el-form :inline="true" class="filters" @submit.prevent="load">
      <el-form-item label="名称">
        <el-input v-model="query.keyword" placeholder="模糊搜索" clearable @keyup.enter="load" />
      </el-form-item>
      <el-form-item label="类型">
        <el-select v-model="query.typeId" placeholder="全部" clearable style="width: 180px">
          <el-option v-for="t in typeOptions" :key="t.id" :label="t.name" :value="t.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="上架">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 140px">
          <el-option label="已上架" :value="1" />
          <el-option label="已下架" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="load">查询</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="list" border stripe v-loading="loading">
      <el-table-column
        type="index"
        label="序号"
        width="72"
        :index="(i) => (query.pageNum - 1) * query.pageSize + i + 1"
      />
      <el-table-column label="封面" width="110">
        <template #default="{ row }">
          <img v-if="row.cover" :src="resolveMediaUrl(row.cover)" alt="" class="cover-thumb" />
          <span v-else class="empty-cover">无</span>
        </template>
      </el-table-column>
      <el-table-column prop="name" label="名称" min-width="140" />
      <el-table-column prop="typeName" label="类型" width="120" />
      <el-table-column label="状态" width="88">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '上架' : '下架' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <router-link :to="`/equipment/edit/${row.id}`">
            <el-button link type="primary">编辑</el-button>
          </router-link>
          <el-button link type="danger" @click="del(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <PaginationBar
      v-model="query.pageNum"
      class="pager-bar"
      :total="total"
      :page-size="query.pageSize"
      @current-change="load"
    />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getEquipmentAdminPage,
  deleteEquipment,
  getEquipmentTypeList,
  importEquipmentExcel,
} from '@/api/equipment'
import { resolveMediaUrl } from '@/utils/resolveMediaUrl'
import PaginationBar from '@/components/PaginationBar.vue'

const list = ref([])
const typeOptions = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({
  keyword: '',
  typeId: undefined,
  status: undefined,
  pageNum: 1,
  pageSize: 20,
})

async function loadTypes() {
  try {
    const data = await getEquipmentTypeList()
    typeOptions.value = Array.isArray(data) ? data : []
  } catch (_) {
    typeOptions.value = []
  }
}

async function load() {
  loading.value = true
  try {
    const data = await getEquipmentAdminPage({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      keyword: query.keyword?.trim() || undefined,
      typeId: query.typeId,
      status: query.status,
    })
    list.value = data.records ?? data.list ?? []
    total.value = data.total ?? list.value.length
  } catch (e) {
    ElMessage.error(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

async function del(row) {
  try {
    await ElMessageBox.confirm(`确定删除器材「${row.name}」？`, '确认', { type: 'warning' })
    await deleteEquipment(row.id)
    ElMessage.success('已删除')
    load()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e?.message || '删除失败')
  }
}

async function onImportFile(file) {
  try {
    const res = await importEquipmentExcel(file)
    const ok = res?.successCount ?? 0
    const fail = res?.failCount ?? 0
    const errs = res?.errors
    let msg = `导入完成：成功 ${ok} 条，失败 ${fail} 条`
    if (Array.isArray(errs) && errs.length) {
      msg += '\n' + errs.slice(0, 8).join('\n')
      if (errs.length > 8) msg += '\n…'
    }
    ElMessageBox.alert(msg, '导入结果', { confirmButtonText: '确定' })
    load()
  } catch (e) {
    ElMessage.error(e?.message || '导入失败')
  }
  return false
}

onMounted(async () => {
  await loadTypes()
  load()
})
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}
.toolbar h2 {
  margin: 0;
  font-size: 1.125rem;
  color: var(--client-text);
}
.toolbar-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}
.toolbar-actions a {
  text-decoration: none;
}
.filters {
  margin-bottom: 16px;
}
.pager-bar {
  margin-top: 16px;
}
.cover-thumb {
  width: 72px;
  height: 44px;
  object-fit: cover;
  border-radius: 8px;
  border: 1px solid rgba(59, 130, 246, 0.15);
  display: block;
}
.empty-cover {
  color: var(--client-muted);
  font-size: 12px;
}
</style>
