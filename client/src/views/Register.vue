<template>
  <div class="auth-page">
    <div class="card">
      <h1>用户注册</h1>
      <form @submit.prevent="onSubmit">
        <div class="field">
          <label>用户名 *</label>
          <input v-model="form.username" type="text" required />
        </div>
        <div class="field">
          <label>密码 *</label>
          <input v-model="form.password" type="password" required />
        </div>
        <div class="field">
          <label>手机号 *</label>
          <input v-model="form.phone" type="text" required />
        </div>
        <div class="field">
          <label>邮箱</label>
          <input v-model="form.email" type="email" />
        </div>
        <p v-if="err" class="err">{{ err }}</p>
        <button type="submit" :disabled="loading">注册</button>
      </form>
      <p class="tip">已有账号？<router-link to="/login">登录</router-link></p>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { register } from '@/api/user'

const router = useRouter()
const form = reactive({ username: '', password: '', phone: '', email: '' })
const loading = ref(false)
const err = ref('')

async function onSubmit() {
  err.value = ''
  loading.value = true
  try {
    await register(form)
    alert('注册成功，请登录')
    router.push('/login')
  } catch (e) {
    err.value = e.message || '注册失败'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page { min-height: 100vh; display: flex; align-items: center; justify-content: center; background: #f0f2f5; }
.card { width: 360px; padding: 32px; background: #fff; border-radius: 8px; box-shadow: 0 2px 12px rgba(0,0,0,0.08); }
.card h1 { font-size: 18px; margin-bottom: 24px; text-align: center; }
.field { margin-bottom: 16px; }
.field label { display: block; margin-bottom: 6px; font-size: 14px; }
.field input { width: 100%; padding: 10px; border: 1px solid #ddd; border-radius: 4px; }
.err { color: #c00; font-size: 13px; margin-bottom: 12px; }
button { width: 100%; padding: 10px; background: #1890ff; color: #fff; border: none; border-radius: 4px; cursor: pointer; }
.tip { margin-top: 16px; font-size: 13px; text-align: center; }
.tip a { color: #1890ff; }
</style>
