import axios from 'axios'
import { useUserStore } from '@/stores/user'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
})

let refreshPromise = null
let unauthorizedHandling = false
const storage = window.sessionStorage
const TOKEN_KEY = 'client_token'
const REFRESH_TOKEN_KEY = 'client_refresh_token'
const USER_KEY = 'client_user'

function getAuthValue(key) {
  return storage.getItem(key) || localStorage.getItem(key) || ''
}

request.interceptors.request.use((config) => {
  const token = getAuthValue(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

/** 认证失效：先提示，再清理登录态，并统一跳转登录页 */
function handleUnauthorized(message) {
  if (unauthorizedHandling) return
  unauthorizedHandling = true
  const tip = message || '登录已失效，请重新登录'
  try {
    window.alert(tip)
  } catch (_) {}
  try {
    const userStore = useUserStore()
    userStore.logout()
  } catch (_) {}
  storage.removeItem(TOKEN_KEY)
  storage.removeItem(REFRESH_TOKEN_KEY)
  storage.removeItem(USER_KEY)
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
  const hash = window.location.hash || '#/home'
  const routePath = hash.startsWith('#') ? hash.slice(1) : hash
  const authRoutes = ['/login', '/register', '/forgot-password', '/reset-password']
  const alreadyOnAuthPage = authRoutes.some((p) => routePath === p || routePath.startsWith(p + '?'))
  if (!alreadyOnAuthPage) {
    const redirect = encodeURIComponent(routePath || '/home')
    setTimeout(() => {
      window.location.replace(`${window.location.origin}/#/login?redirect=${redirect}`)
    }, 0)
  }
  setTimeout(() => {
    unauthorizedHandling = false
  }, 300)
}

request.interceptors.response.use(
  (res) => {
    const { code, message, data } = res.data ?? {}
    if (code === 401) {
      handleUnauthorized(message)
      return Promise.reject(new Error(message || '登录已失效，请重新登录'))
    }
    if (code !== 200) {
      return Promise.reject(new Error(message || '请求失败'))
    }
    return data
  },
  async (err) => {
    const originalConfig = err.config || err?.config || err?.response?.config
    const apiMsg = err.response?.data?.message || err.message || '登录已失效，请重新登录'
    const isUnauthorized = err?.__unauthorized || err.response?.status === 401
    if (!isUnauthorized) {
      return Promise.reject(apiMsg)
    }
    if (originalConfig?._skipAuthRefresh || originalConfig?._retriedByRefresh) {
      handleUnauthorized(apiMsg)
      return Promise.reject(apiMsg)
    }
    const currentRefreshToken = getAuthValue(REFRESH_TOKEN_KEY)
    if (!currentRefreshToken) {
      handleUnauthorized(apiMsg)
      return Promise.reject(apiMsg)
    }
    if (!refreshPromise) {
      refreshPromise = request
        .post('/user/refresh-token', { refreshToken: currentRefreshToken }, { _skipAuthRefresh: true })
        .then((data) => {
          storage.setItem(TOKEN_KEY, data.token || '')
          storage.setItem(REFRESH_TOKEN_KEY, data.refreshToken || '')
          localStorage.removeItem(TOKEN_KEY)
          localStorage.removeItem(REFRESH_TOKEN_KEY)
          const rawUser = storage.getItem(USER_KEY) || localStorage.getItem(USER_KEY)
          const user = rawUser ? JSON.parse(rawUser) : {}
          user.userId = data.userId
          user.username = data.username
          user.role = data.role
          user.avatar = data.avatar
          storage.setItem(USER_KEY, JSON.stringify(user))
          localStorage.removeItem(USER_KEY)
          return data.token
        })
        .finally(() => {
          refreshPromise = null
        })
    }
    try {
      const newToken = await refreshPromise
      originalConfig._retriedByRefresh = true
      originalConfig.headers = originalConfig.headers || {}
      originalConfig.headers.Authorization = `Bearer ${newToken}`
      return request(originalConfig)
    } catch (_e) {
      handleUnauthorized(apiMsg)
      return Promise.reject('登录已失效，请重新登录')
    }
  }
)

export default request
