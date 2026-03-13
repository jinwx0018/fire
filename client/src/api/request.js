import axios from 'axios'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('client_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (res) => {
    const { code, message, data } = res.data ?? {}
    if (code !== 200) {
      return Promise.reject(new Error(message || '请求失败'))
    }
    return data
  },
  (err) => {
    if (err.response?.status === 401) {
      localStorage.removeItem('client_token')
      localStorage.removeItem('client_user')
      window.location.hash = '#/login'
    }
    return Promise.reject(err.response?.data?.message || err.message)
  }
)

export default request
