import request from './request'

export function register(data) {
  return request.post('/user/register', data)
}

export function login(data) {
  return request.post('/user/login', data)
}

export function getUserInfo() {
  return request.get('/user/info')
}

export function updateUserInfo(data) {
  return request.put('/user/info', data)
}

/** 修改密码（需登录，传原密码与新密码） */
export function updatePassword(data) {
  return request.put('/user/password', data)
}

/** 发送邮箱重置链接（无需登录） */
export function sendResetEmail(email) {
  return request.post('/user/password/sendEmail', { email })
}

/** 凭 Token 重置密码（邮箱链接跳转后调用，无需登录） */
export function resetByToken(token, newPassword) {
  return request.post('/user/password/resetByToken', { token, newPassword })
}

/** 上传头像（需登录），返回 { url } */
export function uploadAvatar(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/user/avatar/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}
