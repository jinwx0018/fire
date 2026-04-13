<template>
  <div class="page">
    <h2>修改密码</h2>
    <div class="card">
      <form @submit.prevent="submit">
        <div class="field">
          <label>原密码</label>
          <input v-model="form.oldPassword" type="password" required placeholder="请输入当前密码" />
        </div>
        <div class="field">
          <label>新密码</label>
          <input v-model="form.newPassword" type="password" required placeholder="请输入新密码" />
        </div>
        <div class="field">
          <label>确认新密码</label>
          <input v-model="form.confirm" type="password" required placeholder="再次输入新密码" />
        </div>
        <p v-if="err" class="err">{{ err }}</p>
        <p v-if="ok" class="ok">{{ ok }}</p>
        <div class="actions">
          <button type="submit" :disabled="loading">确认修改</button>
          <router-link to="/profile" class="link">返回个人中心</router-link>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { updatePassword } from '@/api/user'

const router = useRouter()
const form = reactive({ oldPassword: '', newPassword: '', confirm: '' })
const loading = ref(false)
const err = ref('')
const ok = ref('')

async function submit() {
  err.value = ''
  ok.value = ''
  if (form.newPassword !== form.confirm) {
    err.value = '两次输入的新密码不一致'
    return
  }
  if (form.newPassword.length < 6) {
    err.value = '新密码至少 6 位'
    return
  }
  loading.value = true
  try {
    await updatePassword({
      oldPassword: form.oldPassword,
      newPassword: form.newPassword,
    })
    ok.value = '密码已修改，请重新登录'
    form.oldPassword = ''
    form.newPassword = ''
    form.confirm = ''
    setTimeout(() => {
      router.push('/login')
    }, 1500)
  } catch (e) {
    err.value = e.message || '修改失败'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.page {
  max-width: 440px;
}
.page h2 {
  font-size: 1.25rem;
  font-weight: 700;
  margin-bottom: 20px;
  color: var(--client-text);
}
.card {
  background: var(--client-surface);
  padding: 28px;
  border-radius: var(--client-radius);
  box-shadow: var(--client-shadow);
  border: 1px solid #f1f5f9;
}
.field {
  margin-bottom: 16px;
}
.field label {
  display: block;
  margin-bottom: 8px;
  font-size: 0.8125rem;
  font-weight: 500;
  color: #334155;
}
.field input {
  width: 100%;
  padding: 12px 14px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  font-size: 0.9375rem;
}
.field input:focus {
  outline: none;
  border-color: var(--client-primary);
  box-shadow: 0 0 0 3px rgba(29, 78, 216, 0.15);
}
.err {
  color: #dc2626;
  font-size: 0.8125rem;
  margin-bottom: 10px;
}
.ok {
  color: #16a34a;
  font-size: 0.8125rem;
  margin-bottom: 10px;
}
.actions {
  margin-top: 20px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 16px;
}
.actions button {
  padding: 12px 22px;
  background: linear-gradient(135deg, var(--client-primary), #2563eb);
  color: #fff;
  border: none;
  border-radius: 10px;
  cursor: pointer;
  font-size: 0.9375rem;
  font-weight: 600;
}
.actions button:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}
.actions .link {
  color: var(--client-primary);
  text-decoration: none;
  font-size: 0.875rem;
  font-weight: 500;
}
.actions .link:hover {
  text-decoration: underline;
}
</style>
