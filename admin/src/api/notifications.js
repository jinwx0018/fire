import request from './request'

export function getNotificationPage(params) {
  return request.get('/user/notifications', { params })
}

export function getNotificationDetail(id) {
  return request.get(`/user/notifications/detail/${id}`)
}

export function getUnreadNotificationCount() {
  return request.get('/user/notifications/unread-count')
}

export function markNotificationRead(id) {
  return request.put(`/user/notifications/${id}/read`, {})
}

export function markAllNotificationsRead() {
  return request.put('/user/notifications/read-all', {})
}
