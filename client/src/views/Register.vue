<template>
  <div class="auth-page">
    <div class="auth-card">
      <h1>用户注册</h1>
      <p class="auth-sub">创建账号，开始学习消防知识</p>
      <form @submit.prevent="onSubmit">
        <div class="field">
          <label>用户名 *</label>
          <input v-model="form.username" type="text" required autocomplete="username" />
        </div>
        <div class="field">
          <label>密码 *</label>
          <input v-model="form.password" type="password" required autocomplete="new-password" />
        </div>
        <div class="field">
          <label>手机号 *</label>
          <input v-model="form.phone" type="tel" required autocomplete="tel" />
        </div>
        <div class="field">
          <label>邮箱</label>
          <input v-model="form.email" type="email" autocomplete="email" />
        </div>
        <p v-if="err" class="err">{{ err }}</p>
        <button type="submit" :disabled="loading">{{ loading ? '提交中…' : '注册' }}</button>
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
