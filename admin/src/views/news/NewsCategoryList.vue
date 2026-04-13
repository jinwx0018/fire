<template>
  <div class="page">
    <div class="toolbar">
      <h2>新闻分类字典</h2>
      <el-button type="primary" @click="openAdd">新增分类</el-button>
    </div>
    <el-table :data="list" border stripe v-loading="loading">
      <el-table-column
        type="index"
        label="序号"
        width="72"
        :index="(i) => (pageNum - 1) * pageSize + i + 1"
      />
      <el-table-column prop="name" label="名称" min-width="160" />
      <el-table-column prop="sortOrder" label="排序" width="88" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="del(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dlg" :title="dlgTitle" width="400px" @closed="resetForm">
      <el-form :model="form" label-width="80px">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" maxlength="64" show-word-limit />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" :max="9999" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
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
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getNewsCategoryAdminPage,
  createNewsCategory,
  updateNewsCategory,
  deleteNewsCategory,
} from '@/api/newsCategory'
import PaginationBar from '@/components/PaginationBar.vue'

const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 10
const loading = ref(false)
const dlg = ref(false)
const saving = ref(false)
const editId = ref(null)
const form = reactive({ name: '', sortOrder: 0 })

const dlgTitle = computed(() => (editId.value ? '编辑分类' : '新增分类'))

async function load() {
  loading.value = true
  try {
    const data = await getNewsCategoryAdminPage({ pageNum: pageNum.value, pageSize })
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

function resetForm() {
  editId.value = null
  form.name = ''
  form.sortOrder = 0
}

function openAdd() {
  resetForm()
  dlg.value = true
}

function openEdit(row) {
  editId.value = row.id
  form.name = row.name || ''
  form.sortOrder = row.sortOrder ?? 0
  dlg.value = true
}

async function save() {
  if (!form.name?.trim()) {
    ElMessage.warning('请填写名称')
    return
  }
  saving.value = true
  try {
    if (editId.value) {
      await updateNewsCategory(editId.value, { name: form.name.trim(), sortOrder: form.sortOrder })
    } else {
      await createNewsCategory({ name: form.name.trim(), sortOrder: form.sortOrder })
    }
    ElMessage.success('已保存')
    dlg.value = false
    load()
  } catch (e) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function del(row) {
  try {
    await ElMessageBox.confirm(`确定删除分类「${row.name}」？`, '确认', { type: 'warning' })
    await deleteNewsCategory(row.id)
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
