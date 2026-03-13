import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as apiLogin } from '@/api/user'

const TOKEN_KEY = 'client_token'
const USER_KEY = 'client_user'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem(TOKEN_KEY) || '')
  const user = ref(JSON.parse(localStorage.getItem(USER_KEY) || 'null'))

  const isLoggedIn = computed(() => !!token.value)

  function setLogin(data) {
    token.value = data.token || ''
    user.value = {
      userId: data.userId,
      username: data.username,
      role: data.role,
      avatar: data.avatar,
    }
    localStorage.setItem(TOKEN_KEY, token.value)
    localStorage.setItem(USER_KEY, JSON.stringify(user.value))
  }

  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }

  /** 更新当前用户信息（如头像），与 setLogin 中的结构一致 */
  function updateUser(partial) {
    if (!user.value) return
    user.value = { ...user.value, ...partial }
    localStorage.setItem(USER_KEY, JSON.stringify(user.value))
  }

  async function login(form) {
    const data = await apiLogin(form)
    setLogin(data)
    return data
  }

  return { token, user, isLoggedIn, setLogin, logout, login, updateUser }
})
