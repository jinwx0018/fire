import request from './request'

/** 内容分页（管理端也用此接口，可传 status 筛选） */
export function getContentPage(params) {
  return request.get('/content/list', { params })
}

/** 新增内容（作者/管理员） */
export function addContent(data) {
  return request.post('/content', data)
}

/** 内容详情 */
export function getContentById(id) {
  return request.get(`/content/${id}`)
}

/** 修改内容 */
export function updateContent(id, data) {
  return request.put(`/content/${id}`, data)
}

/** 删除内容（逻辑删除） */
export function deleteContent(id) {
  return request.delete(`/content/${id}`)
}

/** 分类列表（树形或平铺） */
export function getCategoryList() {
  return request.get('/content/category/list')
}

/** 管理端：新增分类 */
export function addCategory(data) {
  return request.post('/admin/content/category', data)
}

/** 管理端：修改分类 */
export function updateCategory(id, data) {
  return request.put(`/admin/content/category/${id}`, data)
}

/** 管理端：删除分类 */
export function deleteCategory(id) {
  return request.delete(`/admin/content/category/${id}`)
}
