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
        <label>封面 URL</label>
        <input v-model="form.cover" />
      </div>
      <div class="field">
        <label>使用步骤</label>
        <textarea v-model="form.usageSteps" rows="3"></textarea>
      </div>
      <div class="field">
        <label>检查要点</label>
        <textarea v-model="form.checkPoints" rows="2"></textarea>
      </div>
      <div class="field">
        <label>常见故障解决</label>
        <textarea v-model="form.faultSolution" rows="2"></textarea>
      </div>
      <div class="field">
        <label>简介</label>
        <input v-model="form.summary" />
      </div>
      <p v-if="err" class="err">{{ err }}</p>
      <div class="actions">
        <button type="submit" :disabled="loading">保存</button>
        <router-link to="/equipment" class="btn">返回</router-link>
      </div>
    </form>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getEquipmentTypeList, getEquipmentById, addEquipment, updateEquipment } from '@/api/equipment'

const route = useRoute()
const router = useRouter()
const id = computed(() => route.params.id)
const isEdit = computed(() => !!id.value)

const types = ref([])
const form = reactive({
  name: '',
  typeId: '',
  cover: '',
  usageSteps: '',
  checkPoints: '',
  faultSolution: '',
  summary: '',
})
const loading = ref(false)
const err = ref('')

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
      typeId: data.typeId ?? data.typeId,
      cover: data.cover ?? '',
      usageSteps: data.usageSteps ?? '',
      checkPoints: data.checkPoints ?? '',
      faultSolution: data.faultSolution ?? '',
      summary: data.summary ?? '',
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
      name: form.name,
      typeId: Number(form.typeId),
      cover: form.cover,
      usageSteps: form.usageSteps,
      checkPoints: form.checkPoints,
      faultSolution: form.faultSolution,
      summary: form.summary,
    }
    if (isEdit.value) {
      await updateEquipment(id.value, body)
    } else {
      await addEquipment(body)
    }
    router.push('/equipment')
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
.page { background: #fff; padding: 16px; border-radius: 8px; max-width: 560px; }
.form .field { margin-bottom: 12px; }
.form label { display: block; margin-bottom: 4px; }
.form input, .form select, .form textarea { width: 100%; padding: 8px; border: 1px solid #ddd; border-radius: 4px; }
.err { color: #c00; }
.actions { margin-top: 16px; display: flex; gap: 12px; }
.btn { padding: 8px 16px; background: #eee; color: #333; text-decoration: none; border-radius: 4px; }
</style>
