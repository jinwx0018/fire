<template>
  <div class="login-shell">
    <div class="login-brand">
      <div class="brand-mark" aria-hidden="true">
        <svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
          <rect width="48" height="48" rx="12" fill="url(#g)" />
          <path d="M24 12l8 14H16l8-14z" fill="#fff" opacity=".95" />
          <path d="M18 30h12l-6 6-6-6z" fill="#fff" opacity=".75" />
          <defs>
            <linearGradient id="g" x1="8" y1="4" x2="44" y2="44" gradientUnits="userSpaceOnUse">
              <stop stop-color="#3b82f6" />
              <stop offset="1" stop-color="#0ea5e9" />
            </linearGradient>
          </defs>
        </svg>
      </div>
      <h2 class="brand-title">消防科普推荐系统</h2>
      <p class="brand-desc">管理端控制台 · 内容审核与数据运营</p>
      <ul class="brand-bullets">
        <li>用户与作者权限</li>
        <li>知识 · 论坛 · 新闻 · 器材</li>
        <li>统计与审计</li>
      </ul>
    </div>

    <el-card class="login-card" shadow="never">
      <template #header>
        <div class="card-head">
          <span class="card-title">管理员登录</span>
          <span class="card-hint">仅限 ADMIN 账号</span>
        </div>
      </template>
      <el-form :model="form" @submit.prevent="onSubmit" label-position="top" size="large">
        <el-form-item label="用户名" required>
          <el-input v-model="form.username" placeholder="请输入用户名" clearable />
        </el-form-item>
        <el-form-item label="密码" required>
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password clearable />
        </el-form-item>
        <el-alert v-if="err" type="error" :title="err" show-icon :closable="false" class="err-alert" />
        <el-form-item>
          <el-button type="primary" native-type="submit" :loading="loading" class="submit-btn">
            安全登录
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
.login-shell {
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(280px, 1fr) minmax(320px, 440px);
  align-items: stretch;
}

@media (max-width: 900px) {
  .login-shell {
    grid-template-columns: 1fr;
  }
  .login-brand {
    min-height: auto;
    padding: 32px 24px 16px;
  }
  .brand-bullets {
    display: none;
  }
}

.login-brand {
  background: linear-gradient(160deg, #0f172a 0%, #1e3a5f 48%, #2563eb 92%, #0ea5e9 130%);
  color: #fff;
  padding: 48px 40px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.login-brand::after {
  content: '';
  position: absolute;
  inset: 0;
  background: radial-gradient(ellipse 70% 40% at 80% 0%, rgba(255, 255, 255, 0.12), transparent);
  pointer-events: none;
}

.brand-mark {
  width: 56px;
  height: 56px;
  margin-bottom: 24px;
}

.brand-mark svg {
  width: 100%;
  height: 100%;
  filter: drop-shadow(0 8px 24px rgba(0, 0, 0, 0.25));
}

.brand-title {
  font-size: 1.5rem;
  font-weight: 700;
  letter-spacing: -0.02em;
  margin-bottom: 10px;
}

.brand-desc {
  font-size: 0.9375rem;
  opacity: 0.88;
  margin-bottom: 28px;
  line-height: 1.5;
}

.brand-bullets {
  list-style: none;
  font-size: 0.875rem;
  opacity: 0.85;
  line-height: 2;
}

.brand-bullets li::before {
  content: '✓ ';
  color: #7dd3fc;
  font-weight: 700;
}

.login-card {
  margin: 0;
  border: none;
  border-radius: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.login-card :deep(.el-card__header) {
  border-bottom: 1px solid #e2e8f0;
  padding: 20px 28px 16px;
}

.login-card :deep(.el-card__body) {
  padding: 8px 28px 36px;
}

.card-head {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.card-title {
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--client-text, #1e3a5f);
}

.card-hint {
  font-size: 0.75rem;
  color: var(--client-muted, #64748b);
}

.err-alert {
  margin-bottom: 12px;
}

.submit-btn {
  width: 100%;
  height: 44px;
  font-weight: 600;
  border-radius: 10px;
}
</style>
