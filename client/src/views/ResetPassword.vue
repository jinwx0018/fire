<template>
  <div class="auth-page">
    <div class="card">
      <h1>设置新密码</h1>
      <p v-if="!token" class="err">链接无效或已过期，请重新<a href="#/forgot-password">申请重置</a>。</p>
      <form v-else @submit.prevent="submit">
        <div class="field">
          <label>新密码</label>
          <input v-model="form.newPassword" type="password" required placeholder="请输入新密码" />
        </div>
        <div class="field">
          <label>确认新密码</label>
          <input v-model="form.confirm" type="password" required placeholder="再次输入新密码" />
        </div>
        <p v-if="err" class="err">{{ err }}</p>
        <p v-if="success" class="success">{{ success }}</p>
        <button type="submit" :disabled="loading">{{ loading ? '提交中...' : '确认重置' }}</button>
      </form>
      <p class="tip">
        <router-link to="/login">返回登录</router-link>
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { resetByToken } from '@/api/user'

const route = useRoute()
const router = useRouter()
const token = ref('')
const form = reactive({ newPassword: '', confirm: '' })
const loading = ref(false)
const err = ref('')
const success = ref('')

onMounted(() => {
  token.value = (route.query.token || '').trim()
})

async function submit() {
  err.value = ''
  success.value = ''
  if (form.newPassword !== form.confirm) {
    err.value = '两次输入的密码不一致'
    return
  }
  if (form.newPassword.length < 6) {
    err.value = '密码至少 6 位'
    return
  }
  loading.value = true
  try {
    await resetByToken(token.value, form.newPassword)
    success.value = '密码已重置，即将跳转登录…'
    setTimeout(() => router.replace('/login'), 1500)
  } catch (e) {
    err.value = e.message || '重置失败'
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
.card h1 { font-size: 18px; margin-bottom: 20px; text-align: center; }
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
.err a { color: #1890ff; }
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
