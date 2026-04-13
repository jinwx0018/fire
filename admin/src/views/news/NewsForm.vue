<template>
  <div class="page">
    <h2>{{ isEdit ? '编辑新闻' : '新增新闻' }}</h2>
    <form class="form" @submit.prevent="submit">
      <div class="field">
        <label>标题 *</label>
        <input v-model="form.title" required maxlength="200" />
      </div>
      <div class="field">
        <label>摘要</label>
        <input v-model="form.summary" maxlength="500" />
        <span class="hint">留空则保存时由正文自动生成摘要</span>
      </div>
      <div class="field">
        <label>分类（字典）</label>
        <select v-model="form.categoryId">
          <option :value="emptyCat">— 不选 —</option>
          <option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</option>
        </select>
        <span class="hint">在「新闻分类」菜单维护字典项</span>
      </div>
      <div class="field">
        <label>封面图</label>
        <div class="cover-row">
          <input v-model="form.coverUrl" maxlength="512" placeholder="http(s) 或上传后自动填入" />
          <label class="file-btn">
            <input type="file" accept="image/*" @change="onCoverFile" :disabled="coverUploading" />
            {{ coverUploading ? '上传中…' : '上传' }}
          </label>
        </div>
        <span class="hint">与知识内容共用封面接口；也可粘贴外链（须 http(s) 或 / 开头的站内路径）</span>
      </div>
      <div class="field rich-field">
        <label>正文 *（富文本；保存时后端 OWASP 消毒）</label>
        <RichTextEditor v-model="form.content" />
      </div>
      <div class="field">
        <label>地区</label>
        <input v-model="form.region" maxlength="64" />
      </div>
      <div class="field">
        <label>紧急等级</label>
        <select v-model.number="form.urgencyLevel">
          <option :value="1">低</option>
          <option :value="2">中</option>
          <option :value="3">高</option>
        </select>
      </div>
      <div class="field">
        <label>上架状态</label>
        <select v-model.number="form.status">
          <option :value="1">上架（用户端可见）</option>
          <option :value="0">下架（仅管理端可见）</option>
        </select>
      </div>
      <div class="field">
        <label>发布时间</label>
        <input v-model="form.publishTime" type="datetime-local" />
        <span class="hint">留空则新建为当前时间。设为未来时间可实现定时上架：用户端仅当到达该时间且已上架时才可见。</span>
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
import { getNewsById, addNews, updateNews, getNewsCategoryOptions } from '@/api/news'
import { uploadCoverImage } from '@/api/content'
import RichTextEditor from '@/components/RichTextEditor.vue'

const route = useRoute()
const router = useRouter()
const id = computed(() => route.params.id)
const isEdit = computed(() => !!id.value)

const emptyCat = ''
const categories = ref([])

const form = reactive({
  title: '',
  summary: '',
  categoryId: emptyCat,
  coverUrl: '',
  content: '',
  region: '',
  urgencyLevel: 1,
  status: 1,
  publishTime: '',
})
const loading = ref(false)
const coverUploading = ref(false)
const err = ref('')

function isRichTextEmpty(html) {
  if (!html || !String(html).trim()) return true
  const t = String(html)
    .replace(/<br\s*\/?>/gi, '')
    .replace(/<p>\s*<\/p>/gi, '')
    .replace(/<[^>]+>/g, '')
    .replace(/&nbsp;/g, ' ')
    .trim()
  return !t
}

function toDatetimeLocal(v) {
  if (!v) return ''
  return String(v).replace(' ', 'T').slice(0, 16)
}

async function loadCategories() {
  try {
    categories.value = (await getNewsCategoryOptions()) || []
  } catch (_) {
    categories.value = []
  }
}

async function load() {
  await loadCategories()
  if (!isEdit.value) {
    form.categoryId = emptyCat
    return
  }
  try {
    const data = await getNewsById(id.value)
    Object.assign(form, {
      title: data.title ?? '',
      summary: data.summary ?? '',
      categoryId: data.categoryId != null ? String(data.categoryId) : emptyCat,
      coverUrl: data.coverUrl ?? '',
      content: data.content ?? '',
      region: data.region ?? '',
      urgencyLevel: [1, 2, 3].includes(Number(data.urgencyLevel)) ? Number(data.urgencyLevel) : 1,
      status: data.status === 0 ? 0 : 1,
      publishTime: toDatetimeLocal(data.publishTime),
    })
  } catch (e) {
    err.value = e.message || '加载失败'
  }
}

async function onCoverFile(e) {
  const file = e.target.files?.[0]
  if (!file) return
  coverUploading.value = true
  err.value = ''
  try {
    const data = await uploadCoverImage(file)
    const url = data?.url
    if (url) form.coverUrl = url
  } catch (e) {
    err.value = e.message || '封面上传失败'
  } finally {
    coverUploading.value = false
    e.target.value = ''
  }
}

async function submit() {
  err.value = ''
  if (isRichTextEmpty(form.content)) {
    err.value = '正文不能为空'
    return
  }
  loading.value = true
  try {
    const body = {
      title: form.title.trim(),
      summary: form.summary?.trim() || undefined,
      coverUrl: form.coverUrl?.trim() || undefined,
      content: form.content,
      region: form.region?.trim() || undefined,
      urgencyLevel: form.urgencyLevel,
      status: form.status,
    }
    if (form.publishTime?.trim()) {
      const raw = form.publishTime.trim()
      const pt = raw.length === 16 ? `${raw}:00` : raw
      body.publishTime = pt.includes('T') ? pt.replace('T', ' ') : pt
    } else if (isEdit.value) {
      body.publishTime = null
    }
    if (form.categoryId !== emptyCat && form.categoryId !== '') {
      body.categoryId = Number(form.categoryId)
    } else if (isEdit.value) {
      body.categoryId = null
    }
    if (isEdit.value) {
      body.coverUrl = form.coverUrl?.trim() ? form.coverUrl.trim() : ''
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
.page {
  max-width: 900px;
}
.form .field {
  margin-bottom: 12px;
}
.form .rich-field {
  min-height: 380px;
}
.form label {
  display: block;
  margin-bottom: 4px;
  color: var(--client-text);
  font-weight: 500;
  font-size: 0.875rem;
}
.form .field:not(.rich-field) input,
.form .field:not(.rich-field) select {
  width: 100%;
  padding: 8px 10px;
  border: 1px solid rgba(59, 130, 246, 0.22);
  border-radius: 10px;
  box-sizing: border-box;
  color: var(--client-text);
}
.hint {
  display: block;
  font-size: 12px;
  color: var(--client-muted);
  margin-top: 4px;
}
.err {
  color: #dc2626;
}
.actions {
  margin-top: 16px;
  display: flex;
  gap: 12px;
}
.btn {
  padding: 8px 16px;
  background: var(--client-surface);
  color: var(--client-text);
  text-decoration: none;
  border-radius: 10px;
  border: 1px solid rgba(59, 130, 246, 0.22);
}
.cover-row {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}
.cover-row input {
  flex: 1;
  min-width: 200px;
}
.file-btn {
  display: inline-block;
  padding: 8px 12px;
  background: var(--client-accent-soft);
  border: 1px solid rgba(59, 130, 246, 0.25);
  border-radius: 10px;
  cursor: pointer;
  font-size: 14px;
  color: var(--client-primary);
}
.file-btn input {
  display: none;
}
</style>
