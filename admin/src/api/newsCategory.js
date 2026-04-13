import request from './request'

export function getNewsCategoryAdminList() {
  return request.get('/admin/news/category/list')
}

/** 管理端新闻分类分页 */
export function getNewsCategoryAdminPage(params) {
  return request.get('/admin/news/category/page', { params })
}

export function createNewsCategory(data) {
  return request.post('/admin/news/category', data)
}

export function updateNewsCategory(id, data) {
  return request.put(`/admin/news/category/${id}`, data)
}

export function deleteNewsCategory(id) {
  return request.delete(`/admin/news/category/${id}`)
}
