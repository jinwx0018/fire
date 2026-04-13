import request from './request'

export function register(data) {
  return request.post('/user/register', data)
}

export function login(data) {
  return request.post('/user/login', data)
}

export function refreshToken(refreshToken) {
  return request.post('/user/refresh-token', { refreshToken }, { _skipAuthRefresh: true })
}

export function getUserInfo() {
  return request.get('/user/info')
}

/** 智能推荐：当前用户屏蔽的知识分类 ID 列表 */
export function getBlockedKnowledgeCategories() {
  return request.get('/user/knowledge/blocked-categories')
}

/** 智能推荐：全量替换屏蔽分类，body: { categoryIds: number[] } */
export function putBlockedKnowledgeCategories(categoryIds) {
  return request.put('/user/knowledge/blocked-categories', { categoryIds: categoryIds ?? [] })
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

/** 申请成为作者（需登录） */
export function applyAuthor(data) {
  return request.post('/user/author/apply', data || {})
}

/** 我的作者申请状态（需登录），返回 { applied, status, rejectReason?, createTime?, reviewTime? } */
export function getMyAuthorApplication() {
  return request.get('/user/author/application')
}

/** 我的作者申请完整历史（需登录），返回 { authorRoleActive, records[] }，records 按提交时间倒序 */
export function getMyAuthorApplicationsOverview() {
  return request.get('/user/author/applications')
}

export function logoutAll() {
  return request.post('/user/logoutAll')
}

/** 上传头像（需登录），返回 { url } */
export function uploadAvatar(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/user/avatar/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

/** 站内通知分页 */
export function getNotificationPage(params) {
  return request.get('/user/notifications', { params })
}

/** 单条通知详情（完整正文） */
export function getNotificationDetail(id) {
  return request.get(`/user/notifications/detail/${id}`)
}

export function getUnreadNotificationCount() {
  return request.get('/user/notifications/unread-count')
}

export function markNotificationRead(id) {
  // 传空对象避免部分环境下 PUT 无 body 导致网关/代理异常
  return request.put(`/user/notifications/${id}/read`, {})
}

export function markAllNotificationsRead() {
  return request.put('/user/notifications/read-all', {})
}

/** 自助注销（需登录密码） */
export function deleteAccount(password) {
  return request.post('/user/account/delete', { password })
}

/** 作者申请附件上传，返回 { url } */
export function uploadAuthorAttachment(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/user/author/attachment/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}
