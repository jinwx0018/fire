<template>
  <div class="auth-page login-root">
    <div class="login-deco" aria-hidden="true">
      <div class="login-deco-circle login-deco-circle--1" />
      <div class="login-deco-circle login-deco-circle--2" />
    </div>

    <div class="login-inner">
      <header class="login-head">
        <h1 class="login-title">用户登录</h1>
        <p class="login-sub">欢迎回到消防科普推荐系统</p>
      </header>

      <div class="login-card">
        <form class="login-form" @submit.prevent="onSubmit">
          <div class="form-group">
            <label class="form-label" for="login-username">用户名</label>
            <input
              id="login-username"
              v-model="form.username"
              type="text"
              class="form-input"
              placeholder="请输入用户名"
              required
              autocomplete="username"
            />
          </div>

          <div class="form-group">
            <label class="form-label" for="login-password">密码</label>
            <div class="password-wrap">
              <input
                id="login-password"
                v-model="form.password"
                :type="showPassword ? 'text' : 'password'"
                class="form-input form-input--password"
                placeholder="请输入密码"
                required
                autocomplete="current-password"
              />
              <button
                type="button"
                class="password-toggle"
                :aria-pressed="showPassword"
                :aria-label="showPassword ? '隐藏密码' : '显示密码'"
                @click="showPassword = !showPassword"
              >
                <svg
                  v-if="!showPassword"
                  class="password-toggle-icon"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                  aria-hidden="true"
                >
                  <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                  <circle cx="12" cy="12" r="3" />
                </svg>
                <svg
                  v-else
                  class="password-toggle-icon"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                  aria-hidden="true"
                >
                  <path
                    d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"
                  />
                  <line x1="1" y1="1" x2="23" y2="23" />
                </svg>
              </button>
            </div>
          </div>

          <p v-if="err" class="form-err">{{ err }}</p>

          <button type="submit" class="login-submit" :disabled="loading" :class="{ 'is-loading': loading }">
            <template v-if="!loading">登 录</template>
            <span v-else class="login-submit-loading">
              <svg class="login-spinner" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                <circle class="login-spinner-track" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
                <path
                  class="login-spinner-head"
                  fill="currentColor"
                  d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"
                />
              </svg>
              登录中…
            </span>
          </button>
        </form>

        <div class="login-divider">
          <span>或</span>
        </div>

        <div class="login-links">
          <span class="login-links-text">还没有账号？</span>
          <router-link to="/register" class="login-link login-link--strong">立即注册</router-link>
          <span class="login-links-sep" aria-hidden="true">·</span>
          <router-link to="/forgot-password" class="login-link login-link--muted">忘记密码</router-link>
        </div>
      </div>

      <p class="login-copy">© 2026 消防科普推荐系统 · 安全第一，预防为主</p>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const store = useUserStore()
const form = reactive({ username: '', password: '' })
const loading = ref(false)
const err = ref('')
const showPassword = ref(false)

async function onSubmit() {
  err.value = ''
  loading.value = true
  try {
    await store.login(form)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''
    router.replace(redirect || '/')
  } catch (e) {
    err.value = e.message || '登录失败'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-root {
  position: relative;
}

.login-deco {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
  z-index: 0;
}

.login-deco-circle {
  position: absolute;
  border-radius: 50%;
  opacity: 0.07;
}

.login-deco-circle--1 {
  width: 384px;
  height: 384px;
  top: 0;
  right: 0;
  background: radial-gradient(circle, var(--client-primary) 0%, transparent 70%);
  transform: translate(32%, -32%);
}

.login-deco-circle--2 {
  width: 320px;
  height: 320px;
  bottom: 0;
  left: 0;
  background: radial-gradient(circle, var(--client-accent) 0%, transparent 70%);
  transform: translate(-32%, 32%);
}

.login-inner {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 448px;
  animation: login-fade-in 0.5s ease forwards;
}

@keyframes login-fade-in {
  from {
    opacity: 0;
    transform: translateY(-12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.login-head {
  text-align: center;
  margin-bottom: 1.5rem;
}

.login-title {
  margin: 0 0 0.5rem;
  font-size: 1.75rem;
  font-weight: 700;
  color: var(--client-text);
  letter-spacing: -0.02em;
  font-family: var(--client-font);
}

.login-sub {
  margin: 0;
  font-size: 0.875rem;
  color: var(--client-muted);
  line-height: 1.5;
  font-family: var(--client-font);
}

.login-card {
  background: var(--client-surface);
  border: 1px solid rgba(59, 130, 246, 0.14);
  border-radius: var(--client-radius);
  padding: 1.75rem 1.625rem 1.5rem;
  box-shadow: var(--client-shadow), 0 1px 3px rgba(15, 23, 42, 0.06);
  animation: login-card-in 0.4s ease forwards;
}

@keyframes login-card-in {
  from {
    opacity: 0;
    transform: scale(0.98);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 1.125rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.form-label {
  font-size: 0.8125rem;
  font-weight: 500;
  color: var(--client-text);
  font-family: var(--client-font);
}

.form-input {
  width: 100%;
  box-sizing: border-box;
  height: 2.75rem;
  padding: 0 1rem;
  border: 1.5px solid rgba(59, 130, 246, 0.2);
  border-radius: 10px;
  font-size: 0.875rem;
  color: var(--client-text);
  background: var(--client-surface);
  font-family: var(--client-font);
  transition: border-color 0.25s ease, box-shadow 0.25s ease;
}

.form-input::placeholder {
  color: var(--client-muted);
  opacity: 0.85;
}

.form-input:focus {
  outline: none;
  border-color: var(--client-primary);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.18);
}

.form-input--password {
  padding-right: 2.75rem;
}

.password-wrap {
  position: relative;
  display: flex;
  align-items: center;
}

.password-toggle {
  position: absolute;
  right: 0.75rem;
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  align-items: center;
  justify-content: center;
  width: 2rem;
  height: 2rem;
  padding: 0;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: var(--client-muted);
  cursor: pointer;
  transition: color 0.2s ease, background 0.2s ease;
}

.password-toggle:hover {
  color: var(--client-primary);
  background: rgba(59, 130, 246, 0.08);
}

.password-toggle:focus-visible {
  outline: 2px solid var(--client-primary);
  outline-offset: 2px;
}

.password-toggle-icon {
  width: 1.125rem;
  height: 1.125rem;
}

.form-err {
  margin: -0.25rem 0 0;
  font-size: 0.8125rem;
  color: #dc2626;
}

.login-submit {
  position: relative;
  overflow: hidden;
  width: 100%;
  margin-top: 0.5rem;
  height: 2.75rem;
  border: none;
  border-radius: 10px;
  font-size: 0.875rem;
  font-weight: 600;
  letter-spacing: 0.06em;
  color: #fff;
  cursor: pointer;
  font-family: var(--client-font);
  background: linear-gradient(135deg, var(--client-primary) 0%, var(--client-primary-hover) 100%);
  transition: transform 0.2s ease, box-shadow 0.2s ease, opacity 0.2s ease;
}

.login-submit::before {
  content: '';
  position: absolute;
  inset: 0;
  left: -100%;
  width: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.22), transparent);
  transition: left 0.45s ease;
  pointer-events: none;
}

.login-submit:hover:not(:disabled):not(.is-loading)::before {
  left: 100%;
}

.login-submit:hover:not(:disabled):not(.is-loading) {
  transform: translateY(-1px) scale(1.02);
  box-shadow: 0 10px 28px rgba(59, 130, 246, 0.38);
}

.login-submit:active:not(:disabled):not(.is-loading) {
  transform: translateY(0) scale(0.98);
}

.login-submit:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.login-submit-loading {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
}

.login-spinner {
  width: 1.125rem;
  height: 1.125rem;
  animation: login-spin 0.9s linear infinite;
}

.login-spinner-track {
  opacity: 0.25;
}

.login-spinner-head {
  opacity: 0.85;
}

@keyframes login-spin {
  to {
    transform: rotate(360deg);
  }
}

.login-divider {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin: 1.375rem 0 1.125rem;
}

.login-divider::before,
.login-divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: rgba(59, 130, 246, 0.15);
}

.login-divider span {
  font-size: 0.75rem;
  color: var(--client-muted);
  font-family: var(--client-font);
}

.login-links {
  text-align: center;
  font-size: 0.875rem;
  font-family: var(--client-font);
  line-height: 1.6;
}

.login-links-text {
  color: var(--client-muted);
}

.login-link {
  display: inline;
  text-decoration: none;
  transition: color 0.2s ease, transform 0.2s ease;
}

.login-link--strong {
  margin-left: 0.25rem;
  font-weight: 600;
  color: var(--client-primary);
}

.login-link--strong:hover {
  color: var(--client-primary-hover);
  text-decoration: underline;
}

.login-link--muted {
  color: var(--client-muted);
}

.login-link--muted:hover {
  color: var(--client-primary);
  text-decoration: underline;
}

.login-links-sep {
  margin: 0 0.5rem;
  color: rgba(59, 130, 246, 0.25);
  user-select: none;
}

.login-copy {
  margin: 1.75rem 0 0;
  text-align: center;
  font-size: 0.75rem;
  color: var(--client-muted);
  font-family: var(--client-font);
  opacity: 0.9;
}

@media (max-width: 480px) {
  .login-card {
    padding: 1.5rem 1.25rem;
  }

  .login-title {
    font-size: 1.5rem;
  }

  .login-deco-circle--1 {
    width: 280px;
    height: 280px;
  }

  .login-deco-circle--2 {
    width: 240px;
    height: 240px;
  }
}
</style>
