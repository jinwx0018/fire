<template>
  <div class="page">
    <h2>{{ isEdit ? '编辑内容' : '新增内容' }}</h2>
    <form class="form" @submit.prevent="submit">
      <div class="field">
        <label>标题 *</label>
        <input v-model="form.title" required />
      </div>
      <div class="field">
        <label>分类 *</label>
        <select v-model="form.categoryId" required>
          <option value="">请选择</option>
          <option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</option>
        </select>
      </div>
      <div class="field">
        <label>摘要</label>
        <input v-model="form.summary" />
      </div>
      <div class="field">
        <label>正文 *</label>
        <textarea v-model="form.content" rows="6" required></textarea>
      </div>
      <div class="field">
        <label>封面 URL</label>
        <input v-model="form.cover" />
      </div>
      <div class="field">
        <label>状态</label>
        <select v-model="form.status">
          <option :value="1">发布</option>
          <option :value="0">草稿</option>
        </select>
      </div>
      <p v-if="err" class="err">{{ err }}</p>
      <div class="actions">
        <button type="submit" :disabled="loading">保存</button>
        <router-link to="/content" class="btn">返回</router-link>
      </div>
    </form>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getContentPage, getContentById, addContent, updateContent, getCategoryList } from '@/api/content'

const route = useRoute()
const router = useRouter()
const id = computed(() => route.params.id)
const isEdit = computed(() => !!id.value)

const categories = ref([])
const form = reactive({
  title: '',
  categoryId: '',
  summary: '',
  content: '',
  cover: '',
  status: 1,
})
const loading = ref(false)
const err = ref('')

async function loadCategories() {
  try {
    const data = await getCategoryList()
    categories.value = Array.isArray(data) ? data : (data.list ?? [])
  } catch (e) {
    console.error(e)
  }
}

async function load() {
  if (!isEdit.value) return
  try {
    const data = await getContentById(id.value)
    Object.assign(form, {
      title: data.title,
      categoryId: data.categoryId ?? data.categoryId,
      summary: data.summary ?? '',
      content: data.content ?? '',
      cover: data.cover ?? '',
      status: data.status ?? 1,
    })
  } catch (e) {
    err.value = e.message
  }
}

async function submit() {
  err.value = ''
  loading.value = true
  try {
    const body = {
      title: form.title,
      categoryId: Number(form.categoryId),
      summary: form.summary,
      content: form.content,
      cover: form.cover,
      status: form.status,
    }
    if (isEdit.value) {
      await updateContent(id.value, body)
    } else {
      await addContent(body)
    }
    router.push('/content')
  } catch (e) {
    err.value = e.message || '保存失败'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadCategories()
  load()
})
</script>

<style scoped>
.page { background: #fff; padding: 16px; border-radius: 8px; max-width: 640px; }
.form .field { margin-bottom: 12px; }
.form label { display: block; margin-bottom: 4px; }
.form input, .form select, .form textarea { width: 100%; padding: 8px; border: 1px solid #ddd; border-radius: 4px; }
.err { color: #c00; }
.actions { margin-top: 16px; display: flex; gap: 12px; }
.btn { padding: 8px 16px; background: #eee; color: #333; text-decoration: none; border-radius: 4px; }
</style>
