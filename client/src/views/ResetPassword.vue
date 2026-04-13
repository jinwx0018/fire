<template>
  <div class="auth-page">
    <div class="auth-card">
      <h1>设置新密码</h1>
      <p class="auth-sub">请设置符合强度要求的新密码</p>
      <p v-if="!token" class="err">链接无效或已过期，请重新<a href="#/forgot-password">申请重置</a>。</p>
      <form v-else @submit.prevent="submit">
        <div class="field">
          <label>新密码</label>
          <input v-model="form.newPassword" type="password" required placeholder="请输入新密码" autocomplete="new-password" />
        </div>
        <div class="field">
          <label>确认新密码</label>
          <input v-model="form.confirm" type="password" required placeholder="再次输入新密码" autocomplete="new-password" />
        </div>
        <p v-if="err" class="err">{{ err }}</p>
        <p v-if="success" class="success">{{ success }}</p>
        <button type="submit" :disabled="loading">{{ loading ? '提交中…' : '确认重置' }}</button>
      </form>
      <p class="tip">
        <router-link to="/login">返回登录</router-link>
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
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
.success {
  color: #16a34a;
  font-size: 0.8125rem;
  margin-bottom: 12px;
}
</style>
