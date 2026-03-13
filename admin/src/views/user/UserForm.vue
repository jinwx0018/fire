<template>
  <div class="page">
    <el-card>
      <template #header>
        <span>{{ isEdit ? '编辑用户' : '新增用户' }}</span>
      </template>
      <el-form :model="form" label-width="80px" style="max-width: 480px">
        <el-form-item label="用户名" required>
          <el-input v-model="form.username" placeholder="用户名" />
        </el-form-item>
        <el-form-item v-if="!isEdit" label="密码" required>
          <el-input v-model="form.password" type="password" placeholder="密码" show-password />
        </el-form-item>
        <el-form-item label="手机号" required>
          <el-input v-model="form.phone" placeholder="手机号" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" type="email" placeholder="邮箱" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.role" placeholder="角色" style="width: 100%">
            <el-option label="USER" value="USER" />
            <el-option label="AUTHOR" value="AUTHOR" />
            <el-option label="ADMIN" value="ADMIN" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="isEdit" label="状态">
          <el-select v-model="form.status" placeholder="状态" style="width: 100%">
            <el-option label="正常" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-alert v-if="err" type="error" :title="err" show-icon :closable="false" class="err-alert" />
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="submit">保存</el-button>
          <el-button @click="$router.push('/user')">返回列表</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { addUser, getUserById, updateUser } from '@/api/user'

const route = useRoute()
const router = useRouter()
const id = computed(() => route.params.id)
const isEdit = computed(() => !!id.value)

const form = reactive({
  username: '',
  password: '',
  phone: '',
  email: '',
  role: 'USER',
  status: 1,
})
const loading = ref(false)
const err = ref('')

async function load() {
  if (!isEdit.value) return
  try {
    const data = await getUserById(id.value)
    Object.assign(form, {
      username: data.username,
      phone: data.phone,
      email: data.email ?? '',
      role: data.role ?? 'USER',
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
    if (isEdit.value) {
      await updateUser(id.value, {
        username: form.username,
        phone: form.phone,
        email: form.email,
        role: form.role,
        status: form.status,
      })
    } else {
      await addUser({
        username: form.username,
        password: form.password,
        phone: form.phone,
        email: form.email,
        role: form.role,
        status: 1,
      })
    }
    router.push('/user')
  } catch (e) {
    err.value = e.message || '保存失败'
  } finally {
    loading.value = false
  }
}

onMounted(load)
watch(() => route.params.id, (newId) => {
  if (newId && isEdit.value) load()
})
</script>

<style scoped>
.err-alert {
  margin-bottom: 12px;
}
</style>
