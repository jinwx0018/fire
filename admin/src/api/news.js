import request from './request'

/** 新闻分页（管理端列表也用此接口） */
export function getNewsPage(params) {
  return request.get('/news/list', { params })
}

/** 管理端：新增新闻 */
export function addNews(data) {
  return request.post('/admin/news', data)
}

/** 新闻详情 */
export function getNewsById(id) {
  return request.get(`/news/${id}`)
}

/** 管理端：修改新闻 */
export function updateNews(id, data) {
  return request.put(`/admin/news/${id}`, data)
}

/** 管理端：删除新闻 */
export function deleteNews(id) {
  return request.delete(`/admin/news/${id}`)
}
