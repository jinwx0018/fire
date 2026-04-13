<template>
  <div class="auth-page">
    <div class="auth-card">
      <h1>找回密码</h1>
      <p class="auth-sub">我们将向您的注册邮箱发送重置链接</p>
      <p class="desc">请输入注册时使用的邮箱；若未配置邮件服务，链接可能在服务端日志中输出。</p>
      <form @submit.prevent="submit">
        <div class="field">
          <label>邮箱</label>
          <input v-model="form.email" type="email" required placeholder="请输入注册邮箱" autocomplete="email" />
        </div>
        <p v-if="err" class="err">{{ err }}</p>
        <p v-if="success" class="success">{{ success }}</p>
        <button type="submit" :disabled="loading">{{ loading ? '发送中…' : '发送重置链接' }}</button>
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
.desc {
  font-size: 0.875rem;
  color: var(--client-muted);
  margin-bottom: 20px;
  line-height: 1.5;
}

.success {
  color: #16a34a;
  font-size: 0.8125rem;
  margin-bottom: 12px;
}
</style>
