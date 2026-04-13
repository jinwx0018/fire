<template>
  <div class="page">
    <div class="toolbar">
      <h2>新闻管理</h2>
      <router-link to="/news/add">
        <el-button type="primary">新增新闻</el-button>
      </router-link>
    </div>

    <el-form :inline="true" class="filters" @submit.prevent="load">
      <el-form-item label="标题">
        <el-input v-model="query.title" placeholder="模糊搜索" clearable @keyup.enter="load" />
      </el-form-item>
      <el-form-item label="关键词">
        <el-input v-model="query.keyword" placeholder="搜标题或摘要" clearable @keyup.enter="load" />
      </el-form-item>
      <el-form-item label="分类">
        <el-select v-model="query.categoryId" placeholder="全部" clearable style="width: 140px">
          <el-option v-for="c in categoryOptions" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="旧版分类字">
        <el-input v-model="query.category" placeholder="精确(无字典时)" clearable @keyup.enter="load" />
      </el-form-item>
      <el-form-item label="地区">
        <el-select
          v-model="selectedRegions"
          multiple
          filterable
          clearable
          collapse-tags
          collapse-tags-tooltip
          placeholder="多选，不选为全部"
          style="width: 260px"
        >
          <el-option v-for="r in regionOptions" :key="r" :label="r" :value="r" />
        </el-select>
      </el-form-item>
      <el-form-item label="排序">
        <el-select v-model="query.orderBy" style="width: 140px">
          <el-option label="发布时间" value="publishTime" />
          <el-option label="紧急等级" value="urgency" />
        </el-select>
      </el-form-item>
      <el-form-item label="顺序">
        <el-select v-model="query.order" style="width: 100px">
          <el-option label="降序" value="desc" />
          <el-option label="升序" value="asc" />
        </el-select>
      </el-form-item>
      <el-form-item label="上架">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
          <el-option label="已上架" :value="1" />
          <el-option label="已下架" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="onSearch">查询</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="list" border stripe v-loading="loading">
      <el-table-column
        type="index"
        label="序号"
        width="72"
        :index="(i) => (query.pageNum - 1) * query.pageSize + i + 1"
      />
      <el-table-column label="封面" width="72">
        <template #default="{ row }">
          <el-image
            v-if="row.coverUrl"
            :src="coverSrc(row.coverUrl)"
            style="width: 48px; height: 48px"
            fit="cover"
            preview-teleported
            :preview-src-list="[coverSrc(row.coverUrl)]"
          />
          <span v-else class="no-cover">—</span>
        </template>
      </el-table-column>
      <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
      <el-table-column prop="categoryName" label="分类" width="120" show-overflow-tooltip />
      <el-table-column prop="region" label="地区" width="100" />
      <el-table-column prop="publisherName" label="发布人" width="120" show-overflow-tooltip />
      <el-table-column prop="urgencyLevel" label="紧急" width="72">
        <template #default="{ row }">{{ urgencyText(row.urgencyLevel) }}</template>
      </el-table-column>
      <el-table-column label="发布时间" width="156">
        <template #default="{ row }">
          {{ formatDateTime(row.publishTime) }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '上架' : '下架' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <router-link :to="`/news/edit/${row.id}`">
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
import { getNewsAdminPage, deleteNews, getNewsCategoryOptions, getNewsRegions } from '@/api/news'
import { formatDateTime } from '@/utils/formatDateTime'
import PaginationBar from '@/components/PaginationBar.vue'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const categoryOptions = ref([])
const regionOptions = ref([])
const selectedRegions = ref([])
const query = reactive({
  title: '',
  keyword: '',
  category: '',
  categoryId: undefined,
  orderBy: 'publishTime',
  order: 'desc',
  status: undefined,
  pageNum: 1,
  pageSize: 20,
})

function coverSrc(url) {
  if (!url) return ''
  if (/^https?:\/\//i.test(url)) return url
  const base = import.meta.env.VITE_API_BASE_URL || '/api'
  const p = url.startsWith('/') ? url : `/${url}`
  return base.startsWith('http') ? base.replace(/\/?$/, '') + p : base.replace(/\/?$/, '') + p
}

function urgencyText(level) {
  const m = { 1: '低', 2: '中', 3: '高' }
  return m[level] ?? level ?? '-'
}

function onSearch() {
  query.pageNum = 1
  load()
}

async function loadCategories() {
  try {
    categoryOptions.value = (await getNewsCategoryOptions()) || []
  } catch (_) {
    categoryOptions.value = []
  }
}

async function loadRegionOptions() {
  try {
    regionOptions.value = (await getNewsRegions()) || []
  } catch (_) {
    regionOptions.value = []
  }
}

async function load() {
  loading.value = true
  try {
    const regions = selectedRegions.value.length ? [...selectedRegions.value] : undefined
    const data = await getNewsAdminPage({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      title: query.title?.trim() || undefined,
      keyword: query.keyword?.trim() || undefined,
      category: query.category?.trim() || undefined,
      categoryId: query.categoryId,
      regions,
      orderBy: query.orderBy,
      order: query.order,
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
    await ElMessageBox.confirm(`确定删除新闻《${row.title}》？`, '确认', { type: 'warning' })
    await deleteNews(row.id)
    ElMessage.success('已删除')
    load()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e?.message || '删除失败')
  }
}

onMounted(async () => {
  await loadCategories()
  await loadRegionOptions()
  load()
})
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
.toolbar a {
  text-decoration: none;
}
.filters {
  margin-bottom: 16px;
}
.pager-bar {
  margin-top: 16px;
}
.no-cover {
  color: var(--client-muted);
  font-size: 12px;
  opacity: 0.75;
}
</style>
