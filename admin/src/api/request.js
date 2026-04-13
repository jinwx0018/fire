import axios from 'axios'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
})

let refreshPromise = null
const storage = window.sessionStorage
const TOKEN_KEY = 'admin_token'
const REFRESH_TOKEN_KEY = 'admin_refresh_token'
const USER_KEY = 'admin_user'

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

/**
 * 认证失效：清空本地与 Pinia，并强制跳转登录页。
 * 后端统一为 HTTP 200 + body code=401，不能仅依赖 router.replace（守卫与异步时序会导致仍显示 admin）。
 */
function handleUnauthorized() {
  storage.removeItem(TOKEN_KEY)
  storage.removeItem(REFRESH_TOKEN_KEY)
  storage.removeItem(USER_KEY)
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
  import('@/stores/user')
    .then(({ useUserStore }) => {
      try {
        useUserStore().logout()
      } catch (_) {}
    })
    .finally(() => {
      window.location.replace(window.location.origin + '/#/login')
    })
}

request.interceptors.response.use(
  (res) => {
    const rt = res.config?.responseType
    if (rt === 'blob' || rt === 'arraybuffer') {
      const blob = res.data
      const ct = String(res.headers['content-type'] || '').toLowerCase()
      if (blob && ct.includes('application/json')) {
        return blob.text().then((text) => {
          try {
            const j = JSON.parse(text)
            if (j.code === 401) {
              handleUnauthorized()
              return Promise.reject(new Error(j.message || '登录已失效，请重新登录'))
            }
            if (j.code !== 200) {
              return Promise.reject(new Error(j.message || '请求失败'))
            }
            return j.data
          } catch (_) {
            return Promise.reject(new Error(text || '请求失败'))
          }
        })
      }
      return blob
    }
    const { code, message, data } = res.data ?? {}
    if (code === 401) {
      handleUnauthorized()
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
      handleUnauthorized()
      return Promise.reject(apiMsg)
    }
    const currentRefreshToken = getAuthValue(REFRESH_TOKEN_KEY)
    if (!currentRefreshToken) {
      handleUnauthorized()
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
      handleUnauthorized()
      return Promise.reject('登录已失效，请重新登录')
    }
  }
)

export default request
