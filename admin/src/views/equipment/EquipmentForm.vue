<template>
  <div class="page">
    <h2>{{ isEdit ? '编辑器材' : '新增器材' }}</h2>
    <form class="form" @submit.prevent="submit">
      <div class="field">
        <label>名称 *</label>
        <input v-model="form.name" required />
      </div>
      <div class="field">
        <label>类型 *</label>
        <select v-model="form.typeId" required>
          <option value="">请选择</option>
          <option v-for="t in types" :key="t.id" :value="t.id">{{ t.name }}</option>
        </select>
      </div>
      <div class="field">
        <label>上架状态</label>
        <select v-model.number="form.status">
          <option :value="1">上架（用户端可见）</option>
          <option :value="0">下架（仅管理端列表可见）</option>
        </select>
      </div>
      <div class="field">
        <label>封面</label>
        <div class="cover-row">
          <div class="cover-preview">
            <img v-if="coverPreview" :src="coverPreview" alt="封面预览" />
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
            <p class="hint">支持 jpg/png/gif/webp，上传后即时预览</p>
          </div>
        </div>
      </div>
      <div class="field">
        <label>多图</label>
        <div class="multi-actions">
          <input
            ref="imagesFileRef"
            type="file"
            accept="image/jpeg,image/png,image/gif,image/webp"
            class="file-input"
            multiple
            @change="onImagesFile"
          />
          <button type="button" class="btn-secondary" :disabled="imagesUploading" @click="imagesFileRef?.click()">
            {{ imagesUploading ? '上传中…' : '添加图片' }}
          </button>
          <span class="hint">可多选上传，支持拖动排序</span>
        </div>
        <div v-if="imageList.length" class="gallery">
          <div
            v-for="(url, i) in imageList"
            :key="`${url}-${i}`"
            class="thumb-item"
            :class="{ dragging: dragIndex === i, over: dragOverIndex === i }"
            draggable="true"
            @dragstart="onDragStart(i, $event)"
            @dragenter.prevent="onDragEnter(i)"
            @dragover.prevent
            @drop.prevent="onDrop(i)"
            @dragend="onDragEnd"
          >
            <img :src="mediaPreviewUrl(url)" alt="" />
            <span class="thumb-index">#{{ i + 1 }}</span>
            <button type="button" class="thumb-del" @click="removeImage(i)">删除</button>
          </div>
        </div>
        <p v-else class="hint">未添加图片</p>
      </div>
      <div class="field">
        <label>使用步骤</label>
        <textarea v-model="form.usageSteps" rows="3" placeholder="请输入纯文本内容"></textarea>
      </div>
      <div class="field">
        <label>检查要点</label>
        <textarea v-model="form.checkPoints" rows="2" placeholder="请输入纯文本内容"></textarea>
      </div>
      <div class="field">
        <label>常见故障解决</label>
        <textarea v-model="form.faultSolution" rows="2" placeholder="请输入纯文本内容"></textarea>
      </div>
      <div class="field">
        <label>简介</label>
        <input v-model="form.summary" />
      </div>
      <p v-if="err" class="err">{{ err }}</p>
      <div class="actions">
        <button type="submit" :disabled="loading">保存</button>
        <router-link to="/equipment/list" class="btn">返回</router-link>
      </div>
    </form>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  getEquipmentTypeList,
  getEquipmentById,
  addEquipment,
  updateEquipment,
  uploadEquipmentImage,
} from '@/api/equipment'
import { resolveMediaUrl } from '@/utils/resolveMediaUrl'

const route = useRoute()
const router = useRouter()
const id = computed(() => route.params.id)
const isEdit = computed(() => !!id.value)

const types = ref([])
const form = reactive({
  name: '',
  typeId: '',
  status: 1,
  cover: '',
  images: '',
  usageSteps: '',
  checkPoints: '',
  faultSolution: '',
  summary: '',
})
const loading = ref(false)
const coverUploading = ref(false)
const imagesUploading = ref(false)
const err = ref('')
const coverFileRef = ref(null)
const imagesFileRef = ref(null)
const imageList = ref([])
const dragIndex = ref(-1)
const dragOverIndex = ref(-1)

const coverPreview = computed(() => mediaPreviewUrl(form.cover))

function stripHtmlTags(v) {
  if (v == null || v === '') return ''
  return String(v)
    .replace(/<br\s*\/?>/gi, '\n')
    .replace(/<\/p>\s*<p>/gi, '\n')
    .replace(/<[^>]+>/g, '')
    .replace(/&nbsp;/gi, ' ')
    .trim()
}

function parseImageUrls(images) {
  if (images == null || !String(images).trim()) return []
  const s = String(images).trim()
  try {
    const arr = JSON.parse(s)
    if (Array.isArray(arr)) return arr.map((x) => String(x).trim()).filter(Boolean)
  } catch (_) {
    /* ignore */
  }
  return s.split(/[\n,，;；]/).map((x) => x.trim()).filter(Boolean)
}

function isPrivateOrLocalHost(hostname) {
  const h = String(hostname || '').toLowerCase()
  if (!h) return false
  if (h === 'localhost' || h.endsWith('.localhost') || h === '0.0.0.0' || h === '::1') return true
  if (h.startsWith('127.') || h.startsWith('10.') || h.startsWith('192.168.') || h.startsWith('169.254.')) {
    return true
  }
  if (h.startsWith('172.')) {
    const parts = h.split('.')
    const second = Number(parts[1])
    if (Number.isInteger(second) && second >= 16 && second <= 31) return true
  }
  return false
}

/**
 * 后端禁止保存 localhost/内网绝对地址；
 * 对上传返回的本机/内网 URL 转为相对路径后再保存。
 */
function normalizeMediaPath(rawUrl) {
  const s = String(rawUrl || '').trim()
  if (!s) return ''
  if (s.startsWith('/api/') && !s.startsWith('//')) return s.slice(4) || '/'
  if (s.startsWith('/') && !s.startsWith('//')) return s
  try {
    const u = new URL(s, window.location.origin)
    if (isPrivateOrLocalHost(u.hostname)) {
      const p = `${u.pathname || ''}${u.search || ''}${u.hash || ''}` || ''
      return p.startsWith('/api/') ? (p.slice(4) || '/') : p
    }
  } catch (_) {
    /* ignore parse error */
  }
  return s
}

function mediaPreviewUrl(rawUrl) {
  const s = String(rawUrl || '').trim()
  if (!s) return ''
  if (/^https?:\/\//i.test(s)) return s
  if (s.startsWith('/api/')) return s
  return resolveMediaUrl(s)
}

function removeImage(index) {
  imageList.value.splice(index, 1)
}

function onDragStart(index, event) {
  dragIndex.value = index
  dragOverIndex.value = index
  if (event?.dataTransfer) {
    event.dataTransfer.effectAllowed = 'move'
    event.dataTransfer.setData('text/plain', String(index))
  }
}

function onDragEnter(index) {
  dragOverIndex.value = index
}

function onDrop(targetIndex) {
  const from = dragIndex.value
  if (from < 0 || from === targetIndex) return
  const list = [...imageList.value]
  const [moved] = list.splice(from, 1)
  list.splice(targetIndex, 0, moved)
  imageList.value = list
  dragIndex.value = -1
  dragOverIndex.value = -1
}

function onDragEnd() {
  dragIndex.value = -1
  dragOverIndex.value = -1
}

async function loadTypes() {
  try {
    const data = await getEquipmentTypeList()
    types.value = Array.isArray(data) ? data : (data.list ?? [])
  } catch (e) {
    console.error(e)
  }
}

async function load() {
  if (!isEdit.value) return
  try {
    const data = await getEquipmentById(id.value)
    Object.assign(form, {
      name: data.name,
      typeId: data.typeId != null ? String(data.typeId) : '',
      status: data.status === 0 ? 0 : 1,
      cover: data.cover ?? '',
      images: '',
      usageSteps: stripHtmlTags(data.usageSteps),
      checkPoints: stripHtmlTags(data.checkPoints),
      faultSolution: stripHtmlTags(data.faultSolution),
      summary: data.summary ?? '',
    })
    imageList.value = parseImageUrls(data.images).map((x) => normalizeMediaPath(x))
  } catch (e) {
    err.value = e.message
  }
}

async function onCoverFile(e) {
  const file = e.target.files?.[0]
  if (!file) return
  coverUploading.value = true
  err.value = ''
  try {
    const data = await uploadEquipmentImage(file)
    const url = data?.url
    if (url) form.cover = normalizeMediaPath(url)
  } catch (ex) {
    err.value = ex?.message || '封面上传失败'
  } finally {
    coverUploading.value = false
    e.target.value = ''
  }
}

async function onImagesFile(e) {
  const files = Array.from(e.target.files || [])
  if (!files.length) return
  imagesUploading.value = true
  err.value = ''
  try {
    for (const file of files) {
      const data = await uploadEquipmentImage(file)
      const url = data?.url
      if (url) imageList.value.push(normalizeMediaPath(url))
    }
  } catch (ex) {
    err.value = ex?.message || '多图上传失败'
  } finally {
    imagesUploading.value = false
    e.target.value = ''
  }
}

async function submit() {
  err.value = ''
  loading.value = true
  try {
    const body = {
      name: form.name,
      typeId: Number(form.typeId),
      status: form.status,
      cover: normalizeMediaPath(form.cover),
      images: imageList.value.length ? JSON.stringify(imageList.value.map((x) => normalizeMediaPath(x))) : '',
      usageSteps: stripHtmlTags(form.usageSteps),
      checkPoints: stripHtmlTags(form.checkPoints),
      faultSolution: stripHtmlTags(form.faultSolution),
      summary: form.summary,
    }
    if (isEdit.value) {
      await updateEquipment(id.value, body)
    } else {
      await addEquipment(body)
    }
    router.push('/equipment/list')
  } catch (e) {
    err.value = e.message || '保存失败'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadTypes()
  load()
})
</script>

<style scoped>
.page {
  max-width: 560px;
}
.form .field {
  margin-bottom: 12px;
}
.form label {
  display: block;
  margin-bottom: 4px;
  color: var(--client-text);
  font-weight: 500;
  font-size: 0.875rem;
}
.form input,
.form select,
.form textarea {
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
.hint {
  margin: 0;
  font-size: 12px;
  color: var(--client-muted);
}
.cover-row {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  flex-wrap: wrap;
}
.cover-preview {
  width: 140px;
  height: 84px;
  border: 1px solid rgba(59, 130, 246, 0.15);
  border-radius: 10px;
  overflow: hidden;
  background: rgba(240, 247, 252, 0.65);
  display: flex;
  align-items: center;
  justify-content: center;
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
  gap: 8px;
  align-items: flex-start;
}
.file-input {
  display: none;
}
.btn-secondary {
  padding: 6px 12px;
  border: 1px solid rgba(59, 130, 246, 0.35);
  border-radius: 10px;
  background: var(--client-accent-soft);
  color: var(--client-primary);
  cursor: pointer;
}
.btn-secondary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.btn-text {
  border: none;
  background: none;
  color: #dc2626;
  padding: 0;
  cursor: pointer;
}
.multi-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}
.gallery {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(110px, 1fr));
  gap: 10px;
}
.thumb-item {
  border: 1px solid rgba(59, 130, 246, 0.12);
  border-radius: 10px;
  padding: 6px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  background: rgba(240, 247, 252, 0.4);
  cursor: move;
  transition:
    border-color 0.15s ease,
    background-color 0.15s ease,
    opacity 0.15s ease;
}
.thumb-item.dragging {
  opacity: 0.55;
}
.thumb-item.over {
  border-color: var(--client-primary);
  background: var(--client-accent-soft);
}
.thumb-item img {
  width: 100%;
  height: 84px;
  object-fit: cover;
  border-radius: 8px;
}
.thumb-index {
  font-size: 12px;
  color: var(--client-muted);
}
.thumb-del {
  border: none;
  background: rgba(239, 68, 68, 0.12);
  color: #dc2626;
  border-radius: 8px;
  padding: 4px 6px;
  cursor: pointer;
}
</style>
