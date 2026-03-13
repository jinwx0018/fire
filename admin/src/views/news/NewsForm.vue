<template>
  <div class="page">
    <h2>{{ isEdit ? '编辑新闻' : '新增新闻' }}</h2>
    <form class="form" @submit.prevent="submit">
      <div class="field">
        <label>标题 *</label>
        <input v-model="form.title" required />
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
        <label>地区</label>
        <input v-model="form.region" />
      </div>
      <div class="field">
        <label>紧急等级</label>
        <input v-model="form.urgencyLevel" placeholder="如 高/中/低" />
      </div>
      <p v-if="err" class="err">{{ err }}</p>
      <div class="actions">
        <button type="submit" :disabled="loading">保存</button>
        <router-link to="/news" class="btn">返回</router-link>
      </div>
    </form>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getNewsById, addNews, updateNews } from '@/api/news'

const route = useRoute()
const router = useRouter()
const id = computed(() => route.params.id)
const isEdit = computed(() => !!id.value)

const form = reactive({
  title: '',
  summary: '',
  content: '',
  region: '',
  urgencyLevel: '',
})
const loading = ref(false)
const err = ref('')

async function load() {
  if (!isEdit.value) return
  try {
    const data = await getNewsById(id.value)
    Object.assign(form, {
      title: data.title,
      summary: data.summary ?? '',
      content: data.content ?? '',
      region: data.region ?? '',
      urgencyLevel: data.urgencyLevel ?? '',
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
      summary: form.summary,
      content: form.content,
      region: form.region,
      urgencyLevel: form.urgencyLevel,
    }
    if (isEdit.value) {
      await updateNews(id.value, body)
    } else {
      await addNews(body)
    }
    router.push('/news')
  } catch (e) {
    err.value = e.message || '保存失败'
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.page { background: #fff; padding: 16px; border-radius: 8px; max-width: 640px; }
.form .field { margin-bottom: 12px; }
.form label { display: block; margin-bottom: 4px; }
.form input, .form textarea { width: 100%; padding: 8px; border: 1px solid #ddd; border-radius: 4px; }
.err { color: #c00; }
.actions { margin-top: 16px; display: flex; gap: 12px; }
.btn { padding: 8px 16px; background: #eee; color: #333; text-decoration: none; border-radius: 4px; }
</style>
