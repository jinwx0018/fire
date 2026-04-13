import request from './request'

/** 管理端登录（仅 ADMIN 角色成功，普通用户/作者会返回 403） */
export function login(data) {
  return request.post('/admin/login', data)
}

export function refreshToken(refreshToken) {
  return request.post('/user/refresh-token', { refreshToken }, { _skipAuthRefresh: true })
}

/** 管理端：用户分页列表 */
export function getUserList(params) {
  return request.get('/admin/user/list', { params })
}

/** 管理端：新增用户 */
export function addUser(data) {
  return request.post('/admin/user', data)
}

/** 管理端：用户详情 */
export function getUserById(id) {
  return request.get(`/admin/user/${id}`)
}

/** 管理端：修改用户 */
export function updateUser(id, data) {
  return request.put(`/admin/user/${id}`, data)
}

/** 管理端：删除用户 */
export function deleteUser(id) {
  return request.delete(`/admin/user/${id}`)
}

export function freezeUser(id) {
  return request.put(`/admin/user/${id}/freeze`)
}

export function unfreezeUser(id) {
  return request.put(`/admin/user/${id}/unfreeze`)
}

export function forceLogoutUser(id) {
  return request.put(`/admin/user/${id}/force-logout`)
}
