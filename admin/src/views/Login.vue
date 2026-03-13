<template>
  <div class="login-page">
    <el-card class="login-card" shadow="hover">
      <template #header>
        <span>消防科普推荐系统 - 管理端</span>
      </template>
      <el-form :model="form" @submit.prevent="onSubmit" label-position="top">
        <el-form-item label="用户名" required>
          <el-input v-model="form.username" placeholder="请输入用户名" clearable />
        </el-form-item>
        <el-form-item label="密码" required>
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password clearable />
        </el-form-item>
        <el-alert v-if="err" type="error" :title="err" show-icon :closable="false" class="err-alert" />
        <el-form-item>
          <el-button type="primary" native-type="submit" :loading="loading" style="width: 100%">
            登录
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const store = useUserStore()

const form = reactive({ username: '', password: '' })
const loading = ref(false)
const err = ref('')

async function onSubmit() {
  err.value = ''
  loading.value = true
  try {
    await store.login(form)
    router.replace('/')
  } catch (e) {
    err.value = e.message || '登录失败'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0f2f5;
}
.login-card {
  width: 380px;
}
.login-card :deep(.el-card__header) {
  font-size: 18px;
  text-align: center;
  color: #333;
}
.err-alert {
  margin-bottom: 12px;
}
</style>
