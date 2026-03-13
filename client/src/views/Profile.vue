<template>
  <div class="page">
    <h2>个人中心</h2>
    <div v-if="loading" class="loading">加载中...</div>
    <template v-else-if="info">
      <div class="card">
        <h3>个人资料</h3>
        <form @submit.prevent="saveInfo">
          <div class="field">
            <label>用户名</label>
            <input :value="info.username" disabled class="disabled" />
            <span class="hint">用户名不可修改</span>
          </div>
          <div class="field">
            <label>头像</label>
            <div class="avatar-row">
              <div class="avatar-preview">
                <img v-if="form.avatar" :src="form.avatar" alt="头像" />
                <span v-else class="no-avatar">未设置</span>
              </div>
              <div class="avatar-upload">
                <input ref="fileInput" type="file" accept="image/jpeg,image/png,image/gif,image/webp" class="file-input" @change="onFileChange" />
                <button type="button" class="btn-choose" :disabled="uploadIng" @click="fileInput?.click()">{{ uploadIng ? '上传中...' : '选择图片' }}</button>
                <span class="hint">支持 jpg、png、gif、webp，不超过 2MB</span>
              </div>
            </div>
          </div>
          <div class="field">
            <label>手机号</label>
            <input v-model="form.phone" type="text" placeholder="手机号" required />
          </div>
          <div class="field">
            <label>邮箱</label>
            <input v-model="form.email" type="email" placeholder="选填" />
          </div>
          <p v-if="infoErr" class="err">{{ infoErr }}</p>
          <button type="submit" :disabled="saving">保存资料</button>
        </form>
      </div>
      <div class="links">
        <router-link to="/profile/password">修改密码</router-link>
      </div>
    </template>
    <div v-else class="empty">
      <p v-if="infoErr" class="err">{{ infoErr }}</p>
      <p v-else>暂无数据</p>
      <router-link to="/login" class="link">去登录</router-link>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { getUserInfo, updateUserInfo, uploadAvatar } from '@/api/user'

const userStore = useUserStore()
const fileInput = ref(null)
const info = ref(null)
const loading = ref(true)
const saving = ref(false)
const uploadIng = ref(false)
const infoErr = ref('')
const form = reactive({ avatar: '', phone: '', email: '' })

async function onFileChange(e) {
  const file = e.target.files?.[0]
  if (!file) return
  uploadIng.value = true
  infoErr.value = ''
  try {
    const data = await uploadAvatar(file)
    const url = data?.url || data
    if (url) {
      form.avatar = url
    }
  } catch (err) {
    infoErr.value = err.message || '上传失败'
  } finally {
    uploadIng.value = false
    e.target.value = ''
  }
}

async function load() {
  loading.value = true
  infoErr.value = ''
  try {
    const data = await getUserInfo()
    info.value = data || null
    if (data) {
      form.avatar = data.avatar ?? ''
      form.phone = data.phone ?? ''
      form.email = data.email ?? ''
    }
  } catch (e) {
    infoErr.value = e.message || '加载失败，请重新登录'
  } finally {
    loading.value = false
  }
}

async function saveInfo() {
  infoErr.value = ''
  saving.value = true
  try {
    await updateUserInfo({
      avatar: form.avatar || undefined,
      phone: form.phone,
      email: form.email || undefined,
    })
    if (form.avatar) userStore.updateUser({ avatar: form.avatar })
    load()
  } catch (e) {
    infoErr.value = e.message || '保存失败'
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.page { max-width: 480px; }
.loading { padding: 24px; color: #666; }
.card {
  background: #fff;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 16px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}
.card h3 { font-size: 16px; margin-bottom: 16px; color: #333; }
.field { margin-bottom: 14px; }
.field label { display: block; margin-bottom: 4px; font-size: 14px; color: #555; }
.field input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}
.field input.disabled { background: #f5f5f5; color: #666; }
.field .hint { font-size: 12px; color: #999; margin-top: 4px; display: block; }
.avatar-row { display: flex; align-items: center; gap: 16px; }
.avatar-preview {
  width: 64px; height: 64px; border-radius: 50%;
  overflow: hidden; background: #f0f0f0; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
}
.avatar-preview img { width: 100%; height: 100%; object-fit: cover; }
.avatar-preview .no-avatar { font-size: 12px; color: #999; }
.avatar-upload { display: flex; flex-direction: column; gap: 4px; }
.file-input { display: none; }
.btn-choose {
  padding: 6px 14px; font-size: 13px; cursor: pointer;
  background: #fff; color: #1890ff; border: 1px solid #1890ff; border-radius: 4px;
}
.btn-choose:hover:not(:disabled) { background: #e6f7ff; }
.btn-choose:disabled { opacity: 0.7; cursor: not-allowed; }
.err { color: #c00; font-size: 13px; margin-bottom: 10px; }
.empty { background: #fff; padding: 24px; border-radius: 8px; }
.empty .link { color: #1890ff; text-decoration: none; margin-top: 12px; display: inline-block; }
.empty .link:hover { text-decoration: underline; }
button {
  padding: 10px 20px;
  background: #1890ff;
  color: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}
button:disabled { opacity: 0.7; cursor: not-allowed; }
.links { font-size: 14px; }
.links a { color: #1890ff; text-decoration: none; }
.links a:hover { text-decoration: underline; }
</style>
