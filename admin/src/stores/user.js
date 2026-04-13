import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as apiLogin } from '@/api/user'

const TOKEN_KEY = 'admin_token'
const REFRESH_TOKEN_KEY = 'admin_refresh_token'
const USER_KEY = 'admin_user'
const storage = window.sessionStorage

function readAuthValue(key) {
  return storage.getItem(key) || localStorage.getItem(key) || ''
}

function readUserValue() {
  const raw = storage.getItem(USER_KEY) || localStorage.getItem(USER_KEY)
  return raw ? JSON.parse(raw) : null
}

export const useUserStore = defineStore('user', () => {
  const token = ref(readAuthValue(TOKEN_KEY))
  const refreshToken = ref(readAuthValue(REFRESH_TOKEN_KEY))
  const user = ref(readUserValue())

  const isLoggedIn = computed(() => !!token.value)

  function setLogin(data) {
    token.value = data.token || ''
    refreshToken.value = data.refreshToken || ''
    user.value = {
      userId: data.userId,
      username: data.username,
      role: data.role,
      avatar: data.avatar,
    }
    storage.setItem(TOKEN_KEY, token.value)
    storage.setItem(REFRESH_TOKEN_KEY, refreshToken.value)
    storage.setItem(USER_KEY, JSON.stringify(user.value))
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(REFRESH_TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }

  function logout() {
    token.value = ''
    refreshToken.value = ''
    user.value = null
    storage.removeItem(TOKEN_KEY)
    storage.removeItem(REFRESH_TOKEN_KEY)
    storage.removeItem(USER_KEY)
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(REFRESH_TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }

  async function login(form) {
    const data = await apiLogin(form)
    setLogin(data)
    return data
  }

  return { token, refreshToken, user, isLoggedIn, setLogin, logout, login }
})
