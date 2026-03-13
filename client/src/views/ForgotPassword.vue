<template>
  <div class="auth-page">
    <div class="card">
      <h1>找回密码</h1>
      <p class="desc">请输入注册时使用的邮箱，我们将发送重置链接到您的邮箱。</p>
      <form @submit.prevent="submit">
        <div class="field">
          <label>邮箱</label>
          <input v-model="form.email" type="email" required placeholder="请输入注册邮箱" />
        </div>
        <p v-if="err" class="err">{{ err }}</p>
        <p v-if="success" class="success">{{ success }}</p>
        <button type="submit" :disabled="loading">{{ loading ? '发送中...' : '发送重置链接' }}</button>
      </form>
      <p class="tip">
        <router-link to="/login">返回登录</router-link>
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { sendResetEmail } from '@/api/user'

const form = reactive({ email: '' })
const loading = ref(false)
const err = ref('')
const success = ref('')

async function submit() {
  err.value = ''
  success.value = ''
  loading.value = true
  try {
    await sendResetEmail(form.email)
    success.value = '重置链接已发送，请查收邮件（若未配置邮件服务，链接会在服务端日志中输出）。'
  } catch (e) {
    err.value = e.message || '发送失败'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0f2f5;
}
.card {
  width: 400px;
  padding: 32px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.08);
}
.card h1 { font-size: 18px; margin-bottom: 8px; text-align: center; }
.desc { font-size: 13px; color: #666; margin-bottom: 20px; text-align: center; }
.field { margin-bottom: 16px; }
.field label { display: block; margin-bottom: 6px; font-size: 14px; }
.field input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}
.err { color: #c00; font-size: 13px; margin-bottom: 12px; }
.success { color: #52c41a; font-size: 13px; margin-bottom: 12px; }
button {
  width: 100%;
  padding: 10px;
  background: #1890ff;
  color: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}
button:disabled { opacity: 0.7; cursor: not-allowed; }
.tip { margin-top: 16px; font-size: 13px; text-align: center; }
.tip a { color: #1890ff; text-decoration: none; }
.tip a:hover { text-decoration: underline; }
</style>
