<template>
  <div class="page">
    <div class="toolbar-head">
      <h2>分类管理</h2>
      <button
        type="button"
        class="btn-primary"
        :disabled="submitting || editingId != null"
        @click="openAddDialog"
      >
        新增分类
      </button>
    </div>
    <div class="toolbar-filters">
      <input
        v-model="filterInput"
        placeholder="按分类名称查询"
        @keyup.enter="applyFilter"
      />
      <button type="button" class="btn-query" @click="applyFilter">查询</button>
      <button
        v-if="appliedKeyword || filterInput.trim()"
        type="button"
        class="btn-text"
        @click="clearFilter"
      >
        清空
      </button>
    </div>
    <p v-if="appliedKeyword" class="filter-hint">
      当前条件：名称包含「{{ appliedKeyword }}」，共 {{ filteredTotal }} 条
    </p>
    <table class="table">
      <thead>
        <tr><th>序号</th><th>名称</th><th>排序</th><th>操作</th></tr>
      </thead>
      <tbody>
        <tr v-for="(row, index) in pagedList" :key="row.id">
          <td>{{ (pageNum - 1) * pageSize + index + 1 }}</td>
          <td>
            <template v-if="editingId === row.id">
              <input v-model="editName" class="cell-input" placeholder="名称" />
            </template>
            <template v-else>
              <button type="button" class="name-link" title="查看该分类下的知识" @click="goContents(row)">
                {{ row.name }}
              </button>
            </template>
          </td>
          <td>
            <template v-if="editingId === row.id">
              <input v-model.number="editSort" type="number" class="cell-input cell-input-sort" placeholder="排序" />
            </template>
            <template v-else>{{ row.sort ?? 0 }}</template>
          </td>
          <td class="col-actions">
            <template v-if="editingId === row.id">
              <button type="button" class="link primary" :disabled="submitting" @click="saveEdit(row.id)">保存</button>
              <button type="button" class="link" :disabled="submitting" @click="cancelEdit">取消</button>
            </template>
            <template v-else>
              <button type="button" class="link primary" :disabled="submitting || editingId != null" @click="startEdit(row)">编辑</button>
              <button type="button" class="link danger" :disabled="submitting || editingId != null" @click="del(row)">删除</button>
            </template>
          </td>
        </tr>
      </tbody>
    </table>
    <PaginationBar v-model="pageNum" :total="filteredTotal" :page-size="pageSize" class="pagination-wrap" />
    <p v-if="!filteredList.length && list.length" class="muted">无匹配分类，请调整查询条件</p>
    <p v-if="!list.length" class="muted">暂无分类数据</p>

    <!-- 新增分类弹窗 -->
    <div v-if="showAddDialog" class="dialog-mask" @click.self="closeAddDialog">
      <div class="dialog" role="dialog" aria-modal="true" aria-labelledby="add-dialog-title">
        <h3 id="add-dialog-title">新增分类</h3>
        <div class="dialog-body">
          <label class="field">
            <span>分类名称</span>
            <input v-model="dialogName" placeholder="必填" />
          </label>
          <label class="field">
            <span>排序</span>
            <input v-model.number="dialogSort" type="number" placeholder="数字越小越靠前" />
          </label>
        </div>
        <div class="dialog-actions">
          <button type="button" class="btn-text" @click="closeAddDialog">取消</button>
          <button type="button" class="btn-primary" :disabled="submitting" @click="submitAdd">确定</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getCategoryList, addCategory, updateCategory, deleteCategory } from '@/api/content'
import PaginationBar from '@/components/PaginationBar.vue'

const router = useRouter()

const list = ref([])
const filterInput = ref('')
const appliedKeyword = ref('')
const submitting = ref(false)
const editingId = ref(null)
const editName = ref('')
const editSort = ref(0)

const showAddDialog = ref(false)
const dialogName = ref('')
const dialogSort = ref(0)

const pageNum = ref(1)
const pageSize = 10

const filteredList = computed(() => {
  const k = appliedKeyword.value.trim().toLowerCase()
  if (!k) return list.value
  return list.value.filter((row) => String(row.name ?? '').toLowerCase().includes(k))
})

const filteredTotal = computed(() => filteredList.value.length)

const pagedList = computed(() => {
  const rows = filteredList.value
  const start = (pageNum.value - 1) * pageSize
  return rows.slice(start, start + pageSize)
})

watch(filteredTotal, (len) => {
  const maxP = Math.max(1, Math.ceil(len / pageSize))
  if (pageNum.value > maxP) pageNum.value = maxP
})

function errText(e) {
  if (typeof e === 'string') return e
  return e?.message || '操作失败'
}

function applyFilter() {
  appliedKeyword.value = filterInput.value.trim()
  pageNum.value = 1
}

function clearFilter() {
  filterInput.value = ''
  appliedKeyword.value = ''
  pageNum.value = 1
}

function goContents(row) {
  router.push({ path: '/knowledge/list', query: { categoryId: String(row.id) } })
}

function openAddDialog() {
  dialogName.value = ''
  dialogSort.value = 0
  showAddDialog.value = true
}

function closeAddDialog() {
  showAddDialog.value = false
}

async function load() {
  try {
    const data = await getCategoryList()
    list.value = Array.isArray(data) ? data : (data.list ?? [])
  } catch (e) {
    console.error(e)
    alert(errText(e))
  }
}

async function submitAdd() {
  const name = String(dialogName.value ?? '').trim()
  if (!name) {
    alert('请填写分类名称')
    return
  }
  const sortNum = Number(dialogSort.value)
  const sort = Number.isFinite(sortNum) ? sortNum : 0
  submitting.value = true
  try {
    await addCategory({ name, sort })
    closeAddDialog()
    alert('新增成功')
    await load()
  } catch (e) {
    alert(errText(e))
  } finally {
    submitting.value = false
  }
}

function startEdit(row) {
  editingId.value = row.id
  editName.value = row.name ?? ''
  const s = row.sort
  editSort.value = s != null && Number.isFinite(Number(s)) ? Number(s) : 0
}

function cancelEdit() {
  editingId.value = null
  editName.value = ''
  editSort.value = 0
}

async function saveEdit(id) {
  const name = String(editName.value ?? '').trim()
  if (!name) {
    alert('分类名称不能为空')
    return
  }
  const sortNum = Number(editSort.value)
  const sort = Number.isFinite(sortNum) ? sortNum : 0
  submitting.value = true
  try {
    await updateCategory(id, { name, sort })
    cancelEdit()
    alert('保存成功')
    await load()
  } catch (e) {
    alert(errText(e))
  } finally {
    submitting.value = false
  }
}

async function del(row) {
  if (!confirm(`确定删除分类「${row.name}」？若仍有知识使用该分类，删除可能失败。`)) return
  submitting.value = true
  try {
    await deleteCategory(row.id)
    await load()
  } catch (e) {
    alert(errText(e))
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.toolbar-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  flex-wrap: wrap;
  gap: 12px;
}
.toolbar-head h2 { margin: 0; font-size: 18px; }
.toolbar-filters {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
  margin-bottom: 12px;
}
.toolbar-filters input {
  min-width: 200px;
  padding: 8px 10px;
  border: 1px solid rgba(59, 130, 246, 0.22);
  border-radius: 10px;
}
.btn-primary {
  padding: 8px 16px;
  border: none;
  border-radius: 10px;
  background: var(--client-primary);
  color: #fff;
  cursor: pointer;
  font-size: 14px;
}
.btn-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.btn-query {
  padding: 8px 16px;
  border: 1px solid rgba(59, 130, 246, 0.22);
  border-radius: 10px;
  background: var(--client-surface);
  color: var(--client-text);
  cursor: pointer;
}
.btn-text {
  padding: 8px 12px;
  border: none;
  background: none;
  color: var(--client-primary);
  cursor: pointer;
  font-size: 14px;
}
.filter-hint { margin: 0 0 10px; font-size: 13px; color: var(--client-muted); }
.muted { color: var(--client-muted); font-size: 14px; margin-top: 12px; }

.table { width: 100%; border-collapse: collapse; }
.table th, .table td { padding: 10px; border: 1px solid rgba(59, 130, 246, 0.12); }
.table td.col-actions { white-space: nowrap; }
.table td.col-actions .link { margin-right: 12px; }
.table td.col-actions .link:last-child { margin-right: 0; }
.table .link {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 14px;
  padding: 0;
}
.table .link.primary { color: var(--client-primary); }
.table .link.danger { color: #dc2626; }
.table .link:disabled { opacity: 0.45; cursor: not-allowed; }

.name-link {
  background: none;
  border: none;
  padding: 0;
  color: var(--client-primary);
  cursor: pointer;
  text-align: left;
  font-size: inherit;
  text-decoration: underline;
  text-underline-offset: 2px;
}
.name-link:hover { color: var(--client-primary-hover); }

.cell-input {
  width: 100%;
  max-width: 220px;
  padding: 6px 8px;
  border: 1px solid rgba(59, 130, 246, 0.22);
  border-radius: 8px;
  box-sizing: border-box;
}
.cell-input-sort { max-width: 100px; }

.dialog-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}
.dialog {
  background: var(--client-surface);
  border-radius: 12px;
  padding: 20px 24px;
  min-width: 360px;
  max-width: 90vw;
  box-shadow: 0 10px 30px rgba(59, 130, 246, 0.16);
  border: 1px solid rgba(59, 130, 246, 0.14);
}
.dialog h3 { margin: 0 0 16px; font-size: 16px; }
.dialog-body { display: flex; flex-direction: column; gap: 14px; margin-bottom: 20px; }
.field { display: flex; flex-direction: column; gap: 6px; font-size: 14px; }
.field span { color: var(--client-muted); }
.field input {
  padding: 8px 10px;
  border: 1px solid rgba(59, 130, 246, 0.22);
  border-radius: 8px;
}
.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.pagination-wrap {
  margin-top: 16px;
}
</style>
