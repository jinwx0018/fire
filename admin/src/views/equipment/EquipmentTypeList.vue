<template>
  <div class="page">
    <div class="toolbar">
      <h2>器材类型</h2>
      <el-button type="primary" @click="openEdit()">新增类型</el-button>
    </div>
    <el-table :data="list" border stripe v-loading="loading" style="width: 100%">
      <el-table-column
        type="index"
        label="序号"
        width="80"
        :index="(i) => (pageNum - 1) * pageSize + i + 1"
      />
      <el-table-column prop="name" label="名称" />
      <el-table-column prop="sort" label="排序" width="100" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑类型' : '新增类型'" width="400px" @closed="resetForm">
      <el-form label-width="72px">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" placeholder="类型名称" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>

    <PaginationBar
      v-model="pageNum"
      class="pager-bar"
      :total="total"
      :page-size="pageSize"
      @current-change="load"
    />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getEquipmentTypeAdminPage,
  addEquipmentType,
  updateEquipmentType,
  deleteEquipmentType,
} from '@/api/equipment'
import PaginationBar from '@/components/PaginationBar.vue'

const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 10
const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const form = reactive({ id: null, name: '', sort: 0 })

async function load() {
  loading.value = true
  try {
    const data = await getEquipmentTypeAdminPage({ pageNum: pageNum.value, pageSize })
    list.value = data.records ?? data.list ?? []
    total.value = Number(data.total ?? 0)
    const maxPage = Math.max(1, Math.ceil(total.value / pageSize) || 1)
    if (pageNum.value > maxPage) {
      pageNum.value = maxPage
      await load()
      return
    }
  } catch (e) {
    ElMessage.error(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function openEdit(row) {
  if (row) {
    form.id = row.id
    form.name = row.name || ''
    form.sort = row.sort ?? 0
  } else {
    resetForm()
  }
  dialogVisible.value = true
}

function resetForm() {
  form.id = null
  form.name = ''
  form.sort = 0
}

async function submit() {
  if (!form.name?.trim()) {
    ElMessage.warning('请填写名称')
    return
  }
  saving.value = true
  try {
    if (form.id) {
      await updateEquipmentType(form.id, { name: form.name.trim(), sort: form.sort })
      ElMessage.success('已保存')
    } else {
      await addEquipmentType({ name: form.name.trim(), sort: form.sort })
      ElMessage.success('已新增')
    }
    dialogVisible.value = false
    load()
  } catch (e) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(`确定删除类型「${row.name}」？`, '确认', { type: 'warning' })
    await deleteEquipmentType(row.id)
    ElMessage.success('已删除')
    load()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e?.message || '删除失败')
  }
}

onMounted(load)
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.toolbar h2 {
  margin: 0;
  font-size: 1.125rem;
  color: var(--client-text);
}
.pager-bar {
  margin-top: 16px;
}
</style>
