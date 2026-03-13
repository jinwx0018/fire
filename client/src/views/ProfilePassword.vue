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
.page { max-width: 400px; }
.card {
  background: #fff;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}
.field { margin-bottom: 14px; }
.field label { display: block; margin-bottom: 4px; font-size: 14px; color: #555; }
.field input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}
.err { color: #c00; font-size: 13px; margin-bottom: 10px; }
.ok { color: #52c41a; font-size: 13px; margin-bottom: 10px; }
.actions { margin-top: 16px; display: flex; align-items: center; gap: 12px; }
.actions button {
  padding: 10px 20px;
  background: #1890ff;
  color: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}
.actions button:disabled { opacity: 0.7; cursor: not-allowed; }
.actions .link { color: #1890ff; text-decoration: none; font-size: 14px; }
.actions .link:hover { text-decoration: underline; }
</style>
