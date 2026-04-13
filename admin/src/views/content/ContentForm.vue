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
      <div class="field field-rich">
        <label>正文 *</label>
        <RichTextEditor ref="richRef" v-model="form.content" />
      </div>
      <div class="field">
        <label>封面</label>
        <div class="cover-row">
          <div class="cover-preview">
            <img v-if="form.cover" :src="form.cover" alt="封面预览" />
            <span v-else class="cover-placeholder">未设置</span>
          </div>
          <div class="cover-actions">
            <input
              ref="coverFileRef"
              type="file"
              accept="image/jpeg,image/png,image/gif,image/webp"
              class="file-input"
              @change="onCoverFile"
            />
            <button type="button" class="btn-secondary" :disabled="coverUploading" @click="coverFileRef?.click()">
              {{ coverUploading ? '上传中…' : '选择图片' }}
            </button>
            <button v-if="form.cover" type="button" class="btn-text" @click="form.cover = ''">清除</button>
            <p class="hint">与头像相同：jpg / png / gif / webp，不超过 2MB</p>
          </div>
        </div>
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
        <router-link to="/knowledge/list" class="btn">返回</router-link>
      </div>
    </form>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getContentById, addContent, updateContent, getCategoryList, uploadCoverImage } from '@/api/content'
import RichTextEditor from '@/components/RichTextEditor.vue'

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
const richRef = ref(null)
const coverFileRef = ref(null)
const coverUploading = ref(false)

function errMsg(e) {
  if (typeof e === 'string') return e
  return e?.message || '操作失败'
}

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
    err.value = errMsg(e)
  }
}

function isContentEmpty(html) {
  if (html == null || !String(html).trim()) return true
  const stripped = String(html)
    .replace(/<p><br\s*\/?><\/p>/gi, '')
    .replace(/<p>\s*<\/p>/gi, '')
    .replace(/<br\s*\/?>/gi, '')
    .replace(/&nbsp;/gi, ' ')
    .replace(/<[^>]+>/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
  return !stripped
}

async function onCoverFile(e) {
  const file = e.target.files?.[0]
  if (!file) return
  coverUploading.value = true
  err.value = ''
  try {
    const data = await uploadCoverImage(file)
    const url = data?.url
    if (url) form.cover = url
  } catch (e) {
    err.value = errMsg(e)
  } finally {
    coverUploading.value = false
    e.target.value = ''
  }
}

async function submit() {
  err.value = ''
  const html = richRef.value?.getHtml?.() ?? form.content
  if (isContentEmpty(html)) {
    err.value = '请填写正文'
    return
  }
  loading.value = true
  try {
    const body = {
      title: form.title,
      categoryId: Number(form.categoryId),
      summary: form.summary,
      content: html,
      cover: form.cover,
      status: form.status,
    }
    if (isEdit.value) {
      await updateContent(id.value, body)
    } else {
      await addContent(body)
    }
    router.push('/knowledge/list')
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
.page {
  max-width: 900px;
}
.form .field {
  margin-bottom: 12px;
}
.form .field-rich {
  max-width: 100%;
}
.form label {
  display: block;
  margin-bottom: 4px;
  color: var(--client-text);
  font-weight: 500;
  font-size: 0.875rem;
}
.form .field:not(.field-rich) input,
.form .field:not(.field-rich) select,
.form .field:not(.field-rich) textarea {
  width: 100%;
  padding: 8px 10px;
  border: 1px solid rgba(59, 130, 246, 0.22);
  border-radius: 10px;
  color: var(--client-text);
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
  align-items: flex-start;
  gap: 16px;
  flex-wrap: wrap;
}
.cover-preview {
  width: 120px;
  height: 68px;
  border-radius: 10px;
  overflow: hidden;
  background: rgba(240, 247, 252, 0.65);
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(59, 130, 246, 0.15);
}
.cover-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.cover-placeholder {
  font-size: 12px;
  color: var(--client-muted);
}
.cover-actions {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
}
.file-input {
  display: none;
}
.btn-secondary {
  padding: 6px 14px;
  font-size: 13px;
  cursor: pointer;
  background: var(--client-accent-soft);
  color: var(--client-primary);
  border: 1px solid rgba(59, 130, 246, 0.35);
  border-radius: 10px;
}
.btn-secondary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.btn-text {
  padding: 0;
  border: none;
  background: none;
  color: #dc2626;
  cursor: pointer;
  font-size: 13px;
}
.hint {
  margin: 0;
  font-size: 12px;
  color: var(--client-muted);
}
</style>
